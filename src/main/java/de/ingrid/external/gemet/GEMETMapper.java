/*-
 * **************************************************-
 * ingrid-external-service-gemet
 * ==================================================
 * Copyright (C) 2014 - 2017 wemove digital solutions GmbH
 * ==================================================
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * **************************************************#
 */
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
import de.ingrid.external.om.TreeTerm;
import de.ingrid.external.om.impl.RelatedTermImpl;
import de.ingrid.external.om.impl.TermImpl;
import de.ingrid.external.om.impl.TreeTermImpl;

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
                    if (!resultList.contains( myTerm )) {
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

    /**
     * Create an InGrid API TreeTerm.
     * 
     * @param node
     *            the JSON concept mapped to a TreeTerm
     * @param parents
     *            parents of TreeTerm, pass null if no parents
     * @param children
     *            children of TreeTerm, pass null if no children
     * @return the TreeTerm
     */
    public TreeTerm mapToTreeTerm(JSONObject node, JSONArray parents, JSONArray children) {
        TreeTerm outTerm = new TreeTermImpl();
        mapToTerm( node, outTerm );

        if (parents != null) {
            addParentsToTreeTerm( outTerm, parents );
        }

        if (children != null) {
            addChildrenToTreeTerm( outTerm, children );
        }

        return outTerm;
    }

    public TreeTerm addParentToTreeTerm(TreeTerm node, JSONObject parent) {
        return addToTreeTerm( node, JSONUtils.toJSONArray( parent ), true );
    }

    public TreeTerm addParentsToTreeTerm(TreeTerm node, JSONArray parents) {
        return addToTreeTerm( node, parents, true );
    }

    public TreeTerm addChildrenToTreeTerm(TreeTerm node, JSONArray children) {
        return addToTreeTerm( node, children, false );
    }

    /**
     * Map concepts to TreeTerms and add to given TreeTerm as parents or
     * children.
     * 
     * @param node
     *            TreeTerm where to add other TreeTerms
     * @param concepts
     *            concepts added as TreeTerms
     * @param addAsParent
     *            true=add as parents, false= add as children
     * @return the given node for further processing
     */
    private TreeTerm addToTreeTerm(TreeTerm node, JSONArray concepts, boolean addAsParent) {
        TreeTerm termToAdd = null;
        Iterator<JSONObject> iterator = concepts.iterator();
        while (iterator.hasNext()) {
            termToAdd = new TreeTermImpl();
            mapToTerm( iterator.next(), termToAdd );
            if (addAsParent)
                addParentTerm( node, termToAdd );
            else
                addChildTerm( node, termToAdd );
        }

        return node;
    }

    private void addParentTerm(TreeTerm term, TreeTerm parentTerm) {
        term.addParent( parentTerm );
        parentTerm.addChild( term );
    }

    private void addChildTerm(TreeTerm term, TreeTerm childTerm) {
        term.addChild( childTerm );
        childTerm.addParent( term );
    }

    /**
     * Creates a Term from the given RDF model.
     * 
     * @param res
     *            RDF model
     * @param language
     *            language to use
     * @param alternateLanguage
     *            map name in different language to alternateName. Pass null, if
     *            no alternate language.
     * @return the API term
     */
    public Term mapToTerm(Resource res, String language, String alternateLanguage) {
        Term outTerm = new TermImpl();

        outTerm.setId( RDFUtils.getId( res ) );
        // also set ID as GEMET ID, so the term is classified as GEMET in
        // frontend
        outTerm.setAlternateId( outTerm.getId() );

        outTerm.setName( RDFUtils.getName( res, language ) );

        if (alternateLanguage != null) {
            outTerm.setAlternateName( RDFUtils.getName( res, alternateLanguage ) );
        }

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
     * Set alternate data in given term from different language.
     */
    public Term mapAlternateLanguage(JSONObject json, Term termToMapTo) {
        termToMapTo.setAlternateName( JSONUtils.getName( json ) );

        return termToMapTo;
    }

    /**
     * Map JSON object to given InGrid term.
     * 
     * @param json
     *            JSON from response
     * @param termToMapTo
     *            InGrid Term to map JSON data to, not null !
     * @return the termToMapTo for further processing
     */
    private Term mapToTerm(JSONObject json, Term termToMapTo) {
        termToMapTo.setId( JSONUtils.getId( json ) );
        // also set ID as GEMET ID, so the term is classified as GEMET in
        // frontend
        termToMapTo.setAlternateId( termToMapTo.getId() );
        termToMapTo.setName( JSONUtils.getName( json ) );
        termToMapTo.setType( getTermTypeFromJSON( JSONUtils.getType( json ) ) );

        return termToMapTo;
    }

    private RelationType getRelationTypeFromConceptRelation(ConceptRelation conceptRelation) {
        if (ConceptRelation.RELATED == conceptRelation)
            return RelationType.RELATIVE;
        else if (ConceptRelation.BROADER == conceptRelation)
            return RelationType.PARENT;
        else if (ConceptRelation.NARROWER == conceptRelation)
            return RelationType.CHILD;
        else if (ConceptRelation.GROUP == conceptRelation)
            return RelationType.PARENT;
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
