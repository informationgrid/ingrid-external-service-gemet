package de.ingrid.external.gemet;

import java.util.ResourceBundle;

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.web.HttpOp;
import org.apache.log4j.Logger;

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

    private static String wildcard = "*";
    private static String singleChar = "?";
    private static String escapeChar = "\\";

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

        String query = termId;

        if (log.isDebugEnabled()) {
            log.debug( "Fetching term from: " + query );
        }

        try {
            // read the RDF/XML file. We have to pass Accept header to get RDF
            // response.
            final TypedInputStream is = HttpOp.execHttpGet( query, "application/rdf+xml" );
            model.read( is, null );
        } catch (DoesNotExistException e) {
            log.error( "The term does not exist: " + query, e );
            return null;
        } catch (Exception e) {
            log.error( "The URI seems to have a problem: " + query, e );
            return null;
        }

        // write it to standard out
        // throws error!
        /*
         * if (log.isDebugEnabled()) { model.write(System.out); }
         */

        return model.getResource( termId );
    }
}
