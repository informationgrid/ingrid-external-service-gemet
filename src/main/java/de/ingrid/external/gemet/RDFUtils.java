/*-
 * **************************************************-
 * ingrid-external-service-gemet
 * ==================================================
 * Copyright (C) 2014 - 2024 wemove digital solutions GmbH
 * ==================================================
 * Licensed under the EUPL, Version 1.2 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * **************************************************#
 */
package de.ingrid.external.gemet;


import org.apache.jena.rdf.model.*;

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
            return node.asNode().getLiteralValue().toString().trim();
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
        SimpleSelector selector = new SimpleSelector(null, prop, (RDFNode)null);
        StmtIterator statements = res.getModel().listStatements(selector);
        if (statements.hasNext()) {
            Statement next = statements.next();
            return next.getObject();
        }
        return null;
    }

    private static RDFNode getObject(Resource res, String namespace, String name, String lang) {
        String nsURI = res.getModel().getNsPrefixURI( namespace );
        if (nsURI == null)
            nsURI = namespace;
        Property prop = res.getModel().createProperty( nsURI + name );

        SimpleSelector selector = new SimpleSelector(null, prop, (RDFNode)null);

        StmtIterator statements = res.getModel().listStatements(selector);
        while (statements.hasNext()) {
            Statement stmt = statements.next();
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
