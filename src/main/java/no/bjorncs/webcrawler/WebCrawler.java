package no.bjorncs.webcrawler;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author bjorncs
 */
public class WebCrawler {
    // Number of crawler threads
    private static final int NUM_THREADS = 20;
    // Max connections in connection pool
    private static final int MAX_CONNECTIONS = 20;
    // Max connections per route/host
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;
    private static final String START_URL = "http://telenor.com";

    public static void main(String[] args) {
        try {
            // Caches HTTP connections so we dont have to make a new connection for every GET to same host
            PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
            cm.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
            cm.setMaxTotal(MAX_CONNECTIONS);
            // Create a http client (Apache HTTP-Client library)
            DefaultHttpClient client = new DefaultHttpClient(cm);
            
            // Thread pool for executing crawler tasks
            ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);
            
            // Get instance of database where we store crawled URIs and content
            WebCrawlerDb db = WebCrawlerDb.getInstance();
            // Restores the crawler tasks queue from last session if exist
            // Task queue is stored as a set of URIs represented as strings
            Set<String> uriQueue = db.getUriQueue();
            
            
            if (uriQueue.isEmpty()) {
                // Start from scratch if queue was empty
                threadPool.submit(new WebCrawlerTask(new URI(START_URL), threadPool, client));
            } else {
                // Create crawler tasks from URIs 
                for (String strUri : uriQueue) {
                    URI uri = new URI(strUri);
                    threadPool.submit(new WebCrawlerTask(uri, threadPool, client));
                }
            }
        } catch (URISyntaxException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
