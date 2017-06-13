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

    /**
     * GEMET API concept types (known thesauri) used in "thesaurus_uri"
     * parameter, see
     * http://www.eionet.europa.eu/gemet/webservices#knownrelations
     */
    public enum ConceptType {
        CONCEPT("http://www.eionet.europa.eu/gemet/concept/"), GROUP("http://www.eionet.europa.eu/gemet/group/"), SOUPERGROUP("http://www.eionet.europa.eu/gemet/supergroup/");

        private final String value;

        ConceptType(final String newValue) {
            value = newValue;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * GEMET API concept relations used in "relation_uri" parameter, see
     * http://www.eionet.europa.eu/gemet/webservices#knownrelations
     */
    public enum ConceptRelation {
        NARROWER("http://www.w3.org/2004/02/skos/core#narrower"),
        BROADER("http://www.w3.org/2004/02/skos/core#broader"),
        RELATED("http://www.w3.org/2004/02/skos/core#related"),
        GROUP("http://www.eionet.europa.eu/gemet/2004/06/gemet-schema.rdf#group"),
        GROUP_MEMBER("http://www.eionet.europa.eu/gemet/2004/06/gemet-schema.rdf#groupMember");

        private final String value;

        ConceptRelation(final String newValue) {
            value = newValue;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /** The URL to the service to use ((set from gemet.properties) */
    private String serviceUrl;

    public GEMETClient(ResourceBundle gemetProps) {
        this.serviceUrl = gemetProps.getString( "service.url" );
    }

    public Resource getConceptAsRDF(String conceptUri) {
        if (conceptUri == null || conceptUri.trim().length() == 0) {
            throw new IllegalArgumentException( "No conceptUri passed!" );
        }
        // create an empty model
        Model model = ModelFactory.createDefaultModel();

        String req = conceptUri;

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

        return model.getResource( conceptUri );
    }

    public JSONObject getConceptAsJSON(String conceptUri, String language) {
        if (conceptUri == null || conceptUri.trim().length() == 0) {
            throw new IllegalArgumentException( "No conceptUri passed!" );
        }

        String req = HTMLUtils.prepareUrl( serviceUrl ) + "getConcept?concept_uri=" + conceptUri + "&language=" + language;

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
    public List<JSONArray> getConceptsMatchingKeywords(String[] keywords, String language, MatchingConceptsSearchMode searchMode) {
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
    public JSONArray getConceptsMatchingKeyword(String keyword, String language, MatchingConceptsSearchMode searchMode) {
        JSONArray result = new JSONArray();
        if (keyword == null || keyword.trim().length() == 0) {
            log.warn( "Empty keyword (" + keyword + ") passed, we return empty result !" );
            return result;
        }

        String req = HTMLUtils.prepareUrl( serviceUrl ) + "getConceptsMatchingKeyword?keyword=" + HTMLUtils.encodeForURL( keyword ) + "&search_mode=" + searchMode
                + "&thesaurus_uri=" + ConceptType.CONCEPT + "&language=" + language;

        if (log.isDebugEnabled()) {
            log.debug( "Fetching terms from: " + req );
        }

        try {
            result = (JSONArray) requestJsonUrl( req );
        } catch (Exception e) {
            log.error( "The URI seems to have a problem: " + req, e );
        }

        return result;
    }

    /**
     * Get children of the given concept. NOTICE: If the concept is a group we
     * return only children having no broader concept, meaning the only parent
     * is the group !
     * 
     * @param conceptUri
     *            parent
     * @param language
     *            children in which language
     * @return list of JSONArrays with every array containing JSONObjects with
     *         full data of every child
     */
    public List<JSONArray> getChildConcepts(String conceptUri, String language) {
        return getChildConceptsViaGetAllConceptRelatives( conceptUri, language );
    }

    protected List<JSONArray> getChildConceptsViaGetAllConceptRelatives(String conceptUri, String language) {
        // child relation types
        ConceptRelation[] relations = null;
        if (isGroup( conceptUri )) {
            // for groups the relation is "groupMember"
            relations = new ConceptRelation[] { ConceptRelation.GROUP_MEMBER };
        } else {
            // for concepts and soupergroups the relation is "narrower"
            relations = new ConceptRelation[] { ConceptRelation.NARROWER };
        }

        List<JSONArray> resultList = new ArrayList<JSONArray>();

        // get children
        for (ConceptRelation relation : relations) {
            // only relation as JSON Object { source, relation, target } and not
            // full data of target
            JSONArray conceptRelations = getAllConceptRelatives( conceptUri, relation, language );

            // parse relations and fetch full child concepts
            JSONArray childConcepts = new JSONArray();
            for (Object conceptRelation : conceptRelations) {
                String childId = JSONUtils.getTarget( (JSONObject) conceptRelation );

                // we have to filter GROUP_MEMBER ! We use only those children
                // having NO BROADER concept meaning their only parent is the
                // group !
                if (relation == ConceptRelation.GROUP_MEMBER) {
                    if (hasRelation( childId, ConceptRelation.BROADER, language )) {
                        continue;
                    }
                }

                // we have a child, fetch full data and add to child concepts

                // we check on null, cause some concepts are buggy in service !
                // (e.g. concept/15041)
                JSONObject child = getConceptAsJSON( childId, language );
                if (child == null) {
                    log.error( "Problems fetching child " + childId + " we skip this one !" );
                    continue;
                }

                childConcepts.add( child );
            }

            // add all children of this relation type to result list
            resultList.add( childConcepts );
        }

        return resultList;
    }

    // @formatter:off
    /**
     * This one is buggy e.g.<br>
     * works for this URL:<br>
     * http://www.eionet.europa.eu/gemet/getRelatedConcepts?concept_uri=http://www.eionet.europa.eu/gemet/group/10117&relation_uri=http://www.eionet.europa.eu/gemet/2004/06/gemet-schema.rdf%23groupMember&language=de
     * <br>
     * but NOT for this one:<br>
     * http://www.eionet.europa.eu/gemet/getRelatedConcepts?concept_uri=http://www.eionet.europa.eu/gemet/group/10114&relation_uri=http://www.eionet.europa.eu/gemet/2004/06/gemet-schema.rdf%23groupMember&language=de
     * @param conceptUri
     * @param language
     * @return
     */
    // @formatter:on
    protected List<JSONArray> getChildConceptsViaGetRelatedConcepts(String conceptUri, String language) {
        // child relations
        ConceptRelation[] relations = null;
        if (isGroup( conceptUri )) {
            // for groups the relation is "groupMember"
            relations = new ConceptRelation[] { ConceptRelation.GROUP_MEMBER };
        } else {
            // for concepts and soupergroups the relation is "narrower"
            relations = new ConceptRelation[] { ConceptRelation.NARROWER };
        }

        // get children
        List<JSONArray> conceptList = new ArrayList<JSONArray>();
        for (ConceptRelation relation : relations) {
            JSONArray concepts = getRelatedConcepts( conceptUri, relation, language );

            // we have to filter GROUP_MEMBER !
            // we use only those having NO BROADER concept meaning their only
            // parent is the group !
            if (relation == ConceptRelation.GROUP_MEMBER && concepts.size() > 0) {
                JSONArray groupConcepts = new JSONArray();
                for (Object concept : concepts) {
                    if (!hasRelation( JSONUtils.getId( (JSONObject) concept ), ConceptRelation.BROADER, language )) {
                        groupConcepts.add( concept );
                    }
                }
                concepts = groupConcepts;
            }

            conceptList.add( concepts );
        }

        return conceptList;
    }

    /**
     * Check whether the given concept has the given relation to any object.
     * 
     * @param conceptUri
     *            relation from
     * @param relation
     *            relation type
     * @return true or false
     */
    private boolean hasRelation(String conceptUri, ConceptRelation relation, String language) {
        JSONArray concepts = getAllConceptRelatives( conceptUri, relation, language );
        return (concepts.size() > 0);
    }

    public List<JSONArray> getParentConcepts(String conceptUri, String language) {
        // parent relations
        ConceptRelation[] relations = new ConceptRelation[] { ConceptRelation.BROADER, ConceptRelation.GROUP };

        // get parents
        List<JSONArray> conceptList = new ArrayList<JSONArray>();
        for (ConceptRelation relation : relations) {
            JSONArray concepts = getRelatedConcepts( conceptUri, relation, language );
            conceptList.add( concepts );

            // only fetch GROUP if no BROADER relations found !
            if (relation == ConceptRelation.BROADER && concepts.size() > 0) {
                break;
            }
        }

        return conceptList;
    }

    public JSONArray getTopmostConcepts(ConceptType thesaurusUri, String language) {
        JSONArray result = new JSONArray();

        String req = HTMLUtils.prepareUrl( serviceUrl ) + "getTopmostConcepts?thesaurus_uri=" + thesaurusUri + "&language=" + language;

        if (log.isDebugEnabled()) {
            log.debug( "Fetching terms from: " + req );
        }

        try {
            result = (JSONArray) requestJsonUrl( req );
        } catch (Exception e) {
            log.error( "The URI seems to have a problem: " + req, e );
        }

        return result;
    }

    public JSONArray getRelatedConcepts(String conceptUri, ConceptRelation relation, String language) {
        JSONArray result = new JSONArray();
        if (conceptUri == null || conceptUri.trim().length() == 0) {
            log.warn( "No conceptUri passed (" + conceptUri + "), we return empty result !" );
            return result;
        }

        String req = HTMLUtils.prepareUrl( serviceUrl ) + "getRelatedConcepts?concept_uri=" + conceptUri + "&relation_uri=" + HTMLUtils.encodeForURL( relation.toString() )
                + "&language=" + language;

        if (log.isDebugEnabled()) {
            log.debug( "Fetching terms from: " + req );
        }

        try {
            result = (JSONArray) requestJsonUrl( req );
        } catch (Exception e) {
            log.error( "The URI seems to have a problem: " + req, e );
        }

        return result;
    }

    public JSONArray getAllConceptRelatives(String conceptUri, ConceptRelation relation, String language) {
        JSONArray result = new JSONArray();
        if (conceptUri == null || conceptUri.trim().length() == 0) {
            log.warn( "No conceptUri passed (" + conceptUri + "), we return empty result !" );
            return result;
        }

        String req = HTMLUtils.prepareUrl( serviceUrl ) + "getAllConceptRelatives?concept_uri=" + conceptUri + "&relation_uri=" + HTMLUtils.encodeForURL( relation.toString() )
                + "&language=" + language;

        if (log.isDebugEnabled()) {
            log.debug( "Fetching terms from: " + req );
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

    public boolean isConcept(String conceptUri) {
        if (conceptUri.contains( "/concept/" ))
            return true;
        return false;

    }

    public boolean isGroup(String conceptUri) {
        if (conceptUri.contains( "/group/" ))
            return true;
        return false;

    }
}
