package de.ingrid.external.gemet;

import com.hp.hpl.jena.rdf.model.LiteralRequiredException;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class RDFUtils {

    public static String getId(Resource res) {
        return res.getURI();
    }

    public static String getName(Resource res, String lang) {
        RDFNode node = getObject( res, "skos", "prefLabel", lang );
        if (node == null)
            node = getObject( res, "skosxl", "prefLabel", lang );
        if (node == null)
            node = getObject( res, "skos", "officialName", lang );
        if (node == null)
            node = getObject( res, "http://www.geonames.org/ontology", "officialName", lang );
        if (node == null)
            node = getObject( res, "skos", "altLabel", lang );

        if (node != null) {
            return node.asNode().getLiteralValue().toString();
        }
        return null;
    }

    public static String getType(Resource res) {
        RDFNode node = getObject( res, "rdf", "type" );
        if (node != null) {
            return node.asNode().getURI();
        }
        return null;
    }

    private static RDFNode getObject(Resource res, String namespace, String name) {
        String nsURI = res.getModel().getNsPrefixURI( namespace );
        Property prop = res.getModel().createProperty( nsURI + name );
        Statement stmt = res.getProperty( prop );
        return stmt != null ? stmt.getObject() : null;
    }

    private static RDFNode getObject(Resource res, String namespace, String name, String lang) {
        String nsURI = res.getModel().getNsPrefixURI( namespace );
        if (nsURI == null)
            nsURI = namespace;
        Property prop = res.getModel().createProperty( nsURI + name );
        StmtIterator stmts = res.listProperties( prop );
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            try {
                if (stmt.getLanguage().equals( lang )) {
                    return stmt.getObject();
                }
            } catch (LiteralRequiredException e) {
                continue;
            }
        }
        return null;
    }
}
