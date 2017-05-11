package de.ingrid.external.gemet;

import java.util.ResourceBundle;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.web.HttpOp;
import org.apache.log4j.Logger;
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
        if (termId == null) {
            throw new IllegalArgumentException( "The ID must not be null!" );
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
        if (termId == null) {
            throw new IllegalArgumentException( "The ID must not be null!" );
        }

        String req = HtmlUtils.prepareUrl( serviceUrl ) + "getConcept?concept_uri=" + termId + "&language=" + language;

        if (log.isDebugEnabled()) {
            log.debug( "Fetching term from: " + req );
        }

        JSONObject result = null;

        try {
            result = requestJsonUrl( req );
        } catch (Exception e) {
            log.error( "The URI seems to have a problem: " + req, e );
        }

        return result;
    }

    private JSONObject requestJsonUrl(String url) throws Exception {
        HttpClient client = HttpClientBuilder.create().useSystemProperties().build();
        HttpGet getMethod = new HttpGet( url );
        getMethod.addHeader( "User-Agent", "Request-Promise" );
        HttpResponse response = client.execute( getMethod );
        String json = IOUtils.toString( response.getEntity().getContent(), "UTF-8" );
        if (log.isDebugEnabled()) {
            log.debug( "response: " + json );
        }
        return (JSONObject) new JSONParser().parse( json );
    }

}
