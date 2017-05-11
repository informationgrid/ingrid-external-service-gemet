package de.ingrid.external.gemet;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;

import de.ingrid.external.om.Term;
import de.ingrid.external.om.Term.TermType;
import de.ingrid.external.om.impl.TermImpl;

public class GEMETMapper {
    private final static Logger log = Logger.getLogger( GEMETMapper.class );

    private ResourceBundle bundle;

    public GEMETMapper(ResourceBundle wfsProps) {
        this.bundle = wfsProps;
    }

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

        outTerm.setType( getTermType( RDFUtils.getType( res ) ) );

        // outTerm.setInspireThemes( ? );

        return outTerm;
    }

    private TermType getTermType(String nodeType) {
        if (nodeType.indexOf( "#Concept" ) != -1)
            return TermType.DESCRIPTOR;
        else
            return TermType.NODE_LABEL;
    }

}
