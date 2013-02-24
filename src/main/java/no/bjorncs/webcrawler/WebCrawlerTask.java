/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.bjorncs.webcrawler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

/**
 *
 * @author bjorncs
 */
public class WebCrawlerTask implements Runnable {
    
    // All URIs in a web page are discovered by searching for href-tags
    private static final String HREF_ATTR_SIGNATURE = "href=\"";
    private static final int HREF_ATTR_SIG_LENGTH = HREF_ATTR_SIGNATURE.length();
    
    private final WebCrawlerDb db = WebCrawlerDb.getInstance();
    private final URI startUri;
    private final ExecutorService threadPool;
    private final HttpClient httpClient;

    public WebCrawlerTask(URI uri, ExecutorService threadPool, HttpClient httpClient) {
        this.startUri = uri;
        this.httpClient = httpClient;
        this.threadPool = threadPool;
    }

    @Override
    public void run() {
        db.removeFromQueue(startUri);
        if (db.hasVisited(startUri)) return;
        db.markAsVisited(startUri);
        
        try {
            // Remove the URI from the task queue stored in DB
            // Don't crawl same page twice
            // Send an HTTP GET to the server and get the input stream for the content
            HttpResponse response = httpClient.execute(new HttpGet(startUri));
            InputStream in = response.getEntity().getContent();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                StringBuilder builder = new StringBuilder(4096);
                
                // Read the content and create a single string of it
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                String content = builder.toString();
                db.storeUriContent(startUri, content);
                
                // The search position in content string
                int index = 0;
                // Search for 'href="' in the web page
                while ((index = content.indexOf(HREF_ATTR_SIGNATURE, index)) != -1) {
                    index += HREF_ATTR_SIG_LENGTH;
                    // Find the " that terminates the URI/href
                    int endOfHref = content.indexOf('\"', index);
                    // Extract the URI from the webpage
                    String subUri = content.substring(index, endOfHref);
                    // Try to parse/normalize it (tryNormalize returns null if it fails)
                    URI uri = UrlNormalizer.tryNormalize(startUri, subUri);
                    if (uri != null) {
                        if (!db.hasVisited(uri)) {
                            // Add URI to the queue stored in DB (for backup purpose)
                            db.addToQueue(uri);
                            // Add URI as new crawling task
                            threadPool.submit(new WebCrawlerTask(uri, threadPool, httpClient));
                        }
                    }
                    // Update the position to end of last href
                    index = endOfHref;
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
}
