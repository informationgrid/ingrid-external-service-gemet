package de.ingrid.external.gemet;

public class HtmlUtils {

    public static String prepareUrl(String url) {
        if (url.endsWith( "/" ))
            return url;
        else
            return url + "/";
    }
}
