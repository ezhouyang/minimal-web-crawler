/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.bjorncs.webcrawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.mapdb.DB;
import org.mapdb.DBMaker;

/**
 *
 * @author bjorncs
 */
public class WebCrawlerDb {

    private static final WebCrawlerDb instance = new WebCrawlerDb();

    private final DB db;
    // The crawler queue
    private final Set<String> uriQueue;
    // Set of all URIs visited
    private final Set<String> uriVisited;
    // Maps URI to filename where content is stored
    private final Map<String, String> uriFileLocations;
    // Folder where all content of all URIs are stored
    private final File storageFolder = new File("crawled-content");
    
    private WebCrawlerDb() {
        // Make a MapDB database
        this.db = DBMaker.newFileDB(new File("webcrawler.db"))
                .closeOnJvmShutdown()
                .make();
        
        this.uriQueue = db.getHashSet("uri_queue");
        this.uriFileLocations = db.getHashMap("uri_file_mapping");
        this.uriVisited = db.getHashSet("uri_visited");
        storageFolder.mkdir();
    }
    
    public static WebCrawlerDb getInstance() {
        return instance;
    }

    public Set<String> getUriQueue() {
        return uriQueue;
    }
    
    public synchronized void addToQueue(URI uri) {
        uriQueue.add(uri.toASCIIString());
        db.commit();
    }
    
    public synchronized void removeFromQueue(URI uri) {
        uriQueue.remove(uri.toASCIIString());
        db.commit();
    }
    
    public boolean hasVisited(URI uri) {
        return uriVisited.contains(uri.toASCIIString());
    }
    
    public void storeUriContent(URI uri, String content) throws IOException {
        String filename;
        // Synchronize only the database access so that other threads dont have to wait when writing the content
        synchronized (this) {
            filename = String.format("%d", uriFileLocations.size());
            uriFileLocations.put(uri.toASCIIString(), filename);
            db.commit();
        }
        File file = new File(storageFolder, filename);
        file.createNewFile();
        try (Writer writer = new FileWriter(file)) {
            writer.append(content);
        }
    }
    
    public synchronized void markAsVisited(URI uri) {
        System.out.println("Visited: " + uri);
        uriVisited.add(uri.toASCIIString());
        db.commit();
    }
}
