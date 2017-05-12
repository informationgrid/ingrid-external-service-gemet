package de.ingrid.external.gemet;

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

    /** The URL to the service to use */
    private String serviceUrl;
    /** The URL to the thesaurus to use, passed as parameter in request */
    private String thesaurusUrl;

    public GEMETClient(ResourceBundle gemetProps) {
        this.serviceUrl = gemetProps.getString( "service.url" );
        this.thesaurusUrl = gemetProps.getString( "service.thesaurus_uri" );
    }

    public synchronized Resource getTermAsRDF(String termId) {
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

    public synchronized JSONObject getTermAsJSON(String termId, String language) {
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

    public synchronized List<JSONArray> getSimilarTerms(String[] keywords, String language) {
        if (keywords == null || keywords.length == 0) {
            throw new IllegalArgumentException( "No keywords for similar terms passed!" );
        }

        List<JSONArray> resultList = new ArrayList<JSONArray>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.trim().length() == 0) {
                continue;
            }

            String req = HtmlUtils.prepareUrl( serviceUrl ) + "getConceptsMatchingKeyword?keyword=" + keyword
                    + "&search_mode=4&thesaurus_uri=http://www.eionet.europa.eu/gemet/concept/&language=" + language;

            if (log.isDebugEnabled()) {
                log.debug( "Fetching term from: " + req );
            }

            try {
                JSONArray result = (JSONArray) requestJsonUrl( req );
                resultList.add( result );
            } catch (Exception e) {
                log.error( "The URI seems to have a problem: " + req, e );
            }
        }

        return resultList;
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
