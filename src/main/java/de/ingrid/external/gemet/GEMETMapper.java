package de.ingrid.external.gemet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.hp.hpl.jena.rdf.model.Resource;

import de.ingrid.external.gemet.GEMETClient.ConceptRelation;
import de.ingrid.external.om.RelatedTerm;
import de.ingrid.external.om.RelatedTerm.RelationType;
import de.ingrid.external.om.Term;
import de.ingrid.external.om.Term.TermType;
import de.ingrid.external.om.impl.RelatedTermImpl;
import de.ingrid.external.om.impl.TermImpl;

public class GEMETMapper {

    private final static Logger log = Logger.getLogger( GEMETMapper.class );

    public List<Term> mapToTerms(List<JSONArray> jsonArrayList) {
        return mapToTermsWithKeywordsFilter( jsonArrayList, null, null );
    }

    /**
     * Map Concepts (JSON) to API Terms. Only use concepts containing keywords.
     * 
     * @param jsonArrayList
     *            JSON concepts from GEMET request
     * @param keywordsFilter
     *            filter concepts: only use concepts containing all keywords.
     *            Pass null if no filter.
     * @param locale
     *            only used when filtering
     * @return mapped API Terms
     */
    public List<Term> mapToTermsWithKeywordsFilter(List<JSONArray> jsonArrayList, String[] keywordsFilter, Locale locale) {
        List<Term> resultList = new ArrayList<Term>();

        List<String> resultNames = new ArrayList<String>();
        for (JSONArray jsonArray : jsonArrayList) {
            Iterator<JSONObject> iterator = jsonArray.iterator();
            while (iterator.hasNext()) {
                Term myTerm = mapToTerm( iterator.next() );

                boolean addTerm = true;

                // check whether term contains all keywords if keywords passed !
                if (keywordsFilter != null) {
                    for (String keywordFilter : keywordsFilter) {
                        if (!myTerm.getName().toLowerCase( locale ).contains( keywordFilter.trim().toLowerCase( locale ) )) {
                            addTerm = false;
                            break;
                        }
                    }
                }

                // add term if not already present
                if (addTerm) {
                    if (!resultNames.contains( myTerm.getName() )) {
                        resultList.add( myTerm );
                    }
                }
            }
        }

        return resultList;
    }

    public List<RelatedTerm> mapToRelatedTerms(JSONArray jsonArray, ConceptRelation conceptRelation) {
        List<RelatedTerm> resultList = new ArrayList<RelatedTerm>();

        Iterator<JSONObject> iterator = jsonArray.iterator();
        while (iterator.hasNext()) {
            resultList.add( mapToRelatedTerm( iterator.next(), conceptRelation ) );
        }

        return resultList;
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

        outTerm.setType( getTermTypeFromRDF( RDFUtils.getType( res ) ) );

        // outTerm.setInspireThemes( ? );

        return outTerm;
    }

    /**
     * Creates an InGrid Term from the given JSON object.
     * 
     * @param json
     *            JSON from response
     * @return the InGrid API term
     */
    public Term mapToTerm(JSONObject json) {
        Term outTerm = new TermImpl();
        mapToTerm( json, outTerm );

        return outTerm;
    }

    /**
     * Map JSON object to given InGrid term.
     * 
     * @param json
     *            JSON from response
     * @param termToMapTo
     *            InGrid Term to map JSON data to, not null !
     */
    private void mapToTerm(JSONObject json, Term termToMapTo) {
        termToMapTo.setId( JSONUtils.getId( json ) );

        String name = JSONUtils.getName( json );
        termToMapTo.setName( name );

        termToMapTo.setType( getTermTypeFromJSON( JSONUtils.getType( json ) ) );
    }

    /**
     * Creates an InGrid RelatedTerm from the given JSON object.
     * 
     * @param json
     *            JSON from response
     * @param conceptRelation
     *            the relation type from GEMET API
     * @return the InGrid API RelatedTerm
     */
    public RelatedTerm mapToRelatedTerm(JSONObject json, ConceptRelation conceptRelation) {
        RelatedTerm outTerm = new RelatedTermImpl();
        mapToTerm( json, outTerm );
        outTerm.setRelationType( getRelationTypeFromConceptRelation( conceptRelation ) );

        return outTerm;
    }

    private RelationType getRelationTypeFromConceptRelation(ConceptRelation conceptRelation) {
        if (ConceptRelation.RELATED == conceptRelation)
            return RelationType.RELATIVE;
        else if (ConceptRelation.BROADER == conceptRelation)
            return RelationType.PARENT;
        else if (ConceptRelation.NARROWER == conceptRelation)
            return RelationType.CHILD;
        else if (ConceptRelation.GROUP_MEMBER == conceptRelation)
            return RelationType.CHILD;

        log.error( "Could not map ConceptRelation (" + conceptRelation + "), we return null !" );

        return null;
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
