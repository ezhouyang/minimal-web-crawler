/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.bjorncs.webcrawler;

import java.net.URI;

/**
 *
 * @author bjorncs
 */
public class UrlNormalizer {


    private UrlNormalizer() {}
    
    // Try to parse/normalize URIs from href-attributes
    // Returns null if fails
    public static URI tryNormalize(URI base, String href) {
        try {
            String strippedHref = stripEndingAnchorIfExist(href).trim();
            if (hasInvalidScheme(strippedHref)) {
                return null;
            } else {
                // Try to resolve the href by using the URI of the current page
                return base.resolve(strippedHref).normalize();
            }
        } catch (Exception exp) {
            System.err.println(exp.getMessage());
            return null;
        }
    }
    // Anchor is not needed to locate the web page. We need to remove it since we are using URI.equals() to determine is a URI is visited or not
    private static String stripEndingAnchorIfExist(String url) {
        int index = url.indexOf('#');
        if (index == -1) {
            return url;
        } else {
            return url.substring(0, index);
        }
    } 

    private static boolean hasInvalidScheme(String strippedHref) {
        return strippedHref.startsWith("mailto:") || strippedHref.startsWith("javascript:") || strippedHref.startsWith("#") || strippedHref.startsWith("tel:");
    }

}
