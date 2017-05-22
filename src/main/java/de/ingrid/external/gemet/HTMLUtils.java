package de.ingrid.external.gemet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

public class HTMLUtils {

    private final static Logger log = Logger.getLogger( HTMLUtils.class );

    public static String prepareUrl(String url) {
        if (url.endsWith( "/" ))
            return url;
        else
            return url + "/";
    }

    public static String encodeForURL(String input) {
        String output = input;
        try {
            output = URLEncoder.encode( input, "UTF-8" );
        } catch (UnsupportedEncodingException e) {
            log.error( "Problems encoding input for URL, we keep input: " + input, e );
        }

        return output;
    }
}
