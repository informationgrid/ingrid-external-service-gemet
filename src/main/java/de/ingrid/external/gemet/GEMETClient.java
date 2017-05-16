package de.ingrid.external.gemet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.web.HttpOp;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.DoesNotExistException;

public class GEMETClient {

    private final static Logger log = Logger.getLogger( GEMETClient.class );

    /**
     * GEMET API "search_mode" in "getConceptsMatchingKeyword", see
     * http://www.eionet.europa.eu/gemet/webservices
     */
    public enum MatchingConceptsSearchMode {
        EXACT(0), BEGINS_WITH(1), ENDS_WITH(2), CONTAINS(3), CHECK_ALL(4);

        private final int value;

        MatchingConceptsSearchMode(final int newValue) {
            value = newValue;
        }

        @Override
        public String toString() {
            return String.valueOf( value );
        }
    }

    /** The URL to the service to use */
    private String serviceUrl;
    /** The URL to the thesaurus to use, passed as parameter in request */
    private String thesaurusUrl;

    public GEMETClient(ResourceBundle gemetProps) {
        this.serviceUrl = gemetProps.getString( "service.url" );
        this.thesaurusUrl = gemetProps.getString( "service.thesaurus_uri" );
    }

    public synchronized Resource getConceptAsRDF(String termId) {
        if (termId == null || termId.trim().length() == 0) {
            throw new IllegalArgumentException( "No ID passed!" );
        }
        // create an empty model
        Model model = ModelFactory.createDefaultModel();

        String req = termId;

        if (log.isDebugEnabled()) {
            log.debug( "Fetching term from: " + req );
        }

        try {
            // read the RDF/XML file. We have to pass Accept header to get RDF
            // response.
            final TypedInputStream is = HttpOp.execHttpGet( req, "application/rdf+xml" );
            model.read( is, null );
        } catch (DoesNotExistException e) {
            log.error( "The term does not exist: " + req, e );
            return null;
        } catch (Exception e) {
            log.error( "The URI seems to have a problem: " + req, e );
            return null;
        }

        return model.getResource( termId );
    }

    public synchronized JSONObject getConceptAsJSON(String termId, String language) {
        if (termId == null || termId.trim().length() == 0) {
            throw new IllegalArgumentException( "No ID passed!" );
        }

        String req = HtmlUtils.prepareUrl( serviceUrl ) + "getConcept?concept_uri=" + termId + "&language=" + language;

        if (log.isDebugEnabled()) {
            log.debug( "Fetching term from: " + req );
        }

        JSONObject result = null;

        try {
            result = (JSONObject) requestJsonUrl( req );
        } catch (Exception e) {
            log.error( "The URI seems to have a problem: " + req, e );
        }

        return result;
    }

    /**
     * Every keyword is queried separately and list of concepts are combined to
     * one result list. Use mapping to keep only concepts containing all
     * keywords !
     * 
     * @param keywords
     *            list of keywords, queried separately
     * @param language
     *            in which language
     * @param searchMode
     *            how to search
     * @return list of JSONArray(s) containing concepts or empty list or list of
     *         empty JSONArray(s)
     */
    public synchronized List<JSONArray> getConceptsMatchingKeywords(String[] keywords, String language, MatchingConceptsSearchMode searchMode) {
        List<JSONArray> resultList = new ArrayList<JSONArray>();
        if (keywords == null || keywords.length == 0) {
            log.warn( "No keywords passed, we return empty list !" );
            return resultList;
        }

        for (String keyword : keywords) {
            if (keyword == null || keyword.trim().length() == 0) {
                continue;
            }

            resultList.add( getConceptsMatchingKeyword( keyword, language, searchMode ) );
        }

        return resultList;
    }

    /**
     * Get array of concepts matching given keyword.
     * 
     * @param keyword
     *            arbitrary keyword
     * @param searchMode
     *            how to search, exact match etc. see API
     *            http://www.eionet.europa.eu/gemet/webservices
     * @param language
     *            which language
     * @return array of concepts in JSON or empty array.
     */
    public synchronized JSONArray getConceptsMatchingKeyword(String keyword, String language, MatchingConceptsSearchMode searchMode) {
        JSONArray result = new JSONArray();
        if (keyword == null || keyword.trim().length() == 0) {
            log.warn( "Empty keyword (" + keyword + ") passed, we return empty result !" );
            return result;
        }

        try {
            keyword = URLEncoder.encode( keyword, "UTF-8" );
        } catch (UnsupportedEncodingException e) {
            log.error( "Problems encoding parameter value: " + keyword, e );
        }

        String req = HtmlUtils.prepareUrl( serviceUrl ) + "getConceptsMatchingKeyword?keyword=" + keyword + "&search_mode=" + searchMode
                + "&thesaurus_uri=http://www.eionet.europa.eu/gemet/concept/&language=" + language;

        if (log.isDebugEnabled()) {
            log.debug( "Fetching term from: " + req );
        }

        try {
            result = (JSONArray) requestJsonUrl( req );
        } catch (Exception e) {
            log.error( "The URI seems to have a problem: " + req, e );
        }

        return result;
    }

    private Object requestJsonUrl(String url) throws Exception {
        HttpClient client = HttpClientBuilder.create().useSystemProperties().build();
        HttpGet getMethod = new HttpGet( url );
        getMethod.addHeader( "User-Agent", "Request-Promise" );
        HttpResponse response = client.execute( getMethod );
        String json = IOUtils.toString( response.getEntity().getContent(), "UTF-8" );
        if (log.isDebugEnabled()) {
            log.debug( "response: " + json );
        }
        return new JSONParser().parse( json );
    }
}
