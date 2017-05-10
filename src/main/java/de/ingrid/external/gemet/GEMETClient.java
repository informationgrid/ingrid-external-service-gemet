package de.ingrid.external.gemet;

public class GEMETClient {

    /** The URL to the service to use */
    private String serviceUrl;
    /** The URL to the thesaurus to use, passed as parameter in request */
    private String thesaurusUrl;

    private static String wildcard = "*";
    private static String singleChar = "?";
    private static String escapeChar = "\\";

    public GEMETClient(String serviceUrl, String thesaurusUrl) {
        this.serviceUrl = serviceUrl;
        this.thesaurusUrl = thesaurusUrl;
    }
}
