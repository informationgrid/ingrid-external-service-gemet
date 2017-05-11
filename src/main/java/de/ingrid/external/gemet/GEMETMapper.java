package de.ingrid.external.gemet;

import org.json.simple.JSONObject;

import com.hp.hpl.jena.rdf.model.Resource;

import de.ingrid.external.om.Term;
import de.ingrid.external.om.Term.TermType;
import de.ingrid.external.om.impl.TermImpl;

public class GEMETMapper {

    /**
     * Creates a Term from the given RDF model.
     * 
     * @param res
     *            RDF model
     * @param language
     *            language to use
     * @return the API term
     */
    public Term mapToTerm(Resource res, String language) {
        Term outTerm = new TermImpl();

        outTerm.setId( RDFUtils.getId( res ) );

        String name = RDFUtils.getName( res, language );
        outTerm.setName( name );

        outTerm.setType( getTermTypeFromRDF( RDFUtils.getType( res ) ) );

        // outTerm.setInspireThemes( ? );

        return outTerm;
    }

    /**
     * Creates a Term from the given JSON object.
     * 
     * @param json
     *            JSON from response
     * @return the API term
     */
    public Term mapToTerm(JSONObject json) {
        Term outTerm = new TermImpl();
        outTerm.setId( JSONUtils.getId( json ) );

        String name = JSONUtils.getName( json );
        outTerm.setName( name );

        outTerm.setType( getTermTypeFromJSON( JSONUtils.getType( json ) ) );

        // outTerm.setInspireThemes( ? );

        return outTerm;
    }

    private TermType getTermTypeFromRDF(String rdfType) {
        if (rdfType.indexOf( "#Concept" ) != -1)
            return TermType.DESCRIPTOR;
        else
            return TermType.NODE_LABEL;
    }

    private TermType getTermTypeFromJSON(String jsonType) {
        if (jsonType.indexOf( "/concept/" ) != -1)
            return TermType.DESCRIPTOR;
        else
            return TermType.NODE_LABEL;
    }
}
