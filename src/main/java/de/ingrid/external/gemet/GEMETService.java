/*-
 * **************************************************-
 * ingrid-external-service-gemet
 * ==================================================
 * Copyright (C) 2014 - 2022 wemove digital solutions GmbH
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
import java.util.ResourceBundle;
import java.util.Stack;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.ingrid.external.ThesaurusService;
import de.ingrid.external.gemet.GEMETClient.ConceptRelation;
import de.ingrid.external.gemet.GEMETClient.ConceptType;
import de.ingrid.external.gemet.GEMETClient.MatchingConceptsSearchMode;
import de.ingrid.external.om.RelatedTerm;
import de.ingrid.external.om.Term;
import de.ingrid.external.om.Term.TermType;
import de.ingrid.external.om.TreeTerm;
import de.ingrid.external.om.impl.TreeTermImpl;

public class GEMETService implements ThesaurusService {

    private final static Logger log = LogManager.getLogger( GEMETService.class );

    GEMETClient gemetClient;
    GEMETMapper gemetMapper;

    /**
     * request RDF format from service where possible (true) or JSON (false)
     * (set from gemet.properties)
     */
    protected boolean doRDF;

    /**
     * Maximum number of keywords to analyze from text (set from
     * gemet.properties)
     */
    protected int analyzeMaxWords;

    /**
     * When looking for terms from a given query ignore the matching type which
     * is passed to the service (CONTAINS, BEGINS_WITH, EXACT) and always use
     * CONTAINS to get maximum number of results (set from gemet.properties)
     */
    protected boolean ignorePassedMatchingType;

    /**
     * Also deliver terms in this language as alternateName. Set to null if no
     * alternateName !
     */
    protected String alternateLanguage = null;

    // Init Method is called by the Spring Framework on initialization
    public void init() throws Exception {
        ResourceBundle gemetProps = ResourceBundle.getBundle( "gemet" );

        this.doRDF = Boolean.parseBoolean( gemetProps.getString( "service.request.rdf" ) );
        this.analyzeMaxWords = Integer.parseInt( gemetProps.getString( "service.analyzeMaxWords" ) );
        this.ignorePassedMatchingType = Boolean.parseBoolean( gemetProps.getString( "service.ignorePassedMatchingType" ) );
        try {
            this.alternateLanguage = gemetProps.getString( "service.alternateLanguage" );
        } catch (Exception ex) {
            // catch missing property etc., we set to null if problems
            if (log.isDebugEnabled()) {
                log.debug( "Problems reading 'service.alternateLanguage' from gemet.properties, we set to null." );
            }
            this.alternateLanguage = null;
        } finally {
            // set to null if empty string to indicate no alternate language !
            if (this.alternateLanguage != null && this.alternateLanguage.length() == 0)
                this.alternateLanguage = null;
        }

        this.gemetClient = new GEMETClient( gemetProps );
        this.gemetMapper = new GEMETMapper();
    }

    /**
     * request RDF format from service where possible (true) or JSON (false)
     * (set from gemet.properties)
     */
    public boolean isDoRDF() {
        return doRDF;
    }

    /**
     * request RDF format from service where possible (true) or JSON (false)
     * (set from gemet.properties)
     */
    public void setDoRDF(boolean doRDF) {
        this.doRDF = doRDF;
    }

    /**
     * When looking for terms from a given query ignore the matching type which
     * is passed to the service (CONTAINS, BEGINS_WITH, EXACT) and always use
     * CONTAINS to get maximum number of results (set from gemet.properties)
     */
    public boolean isIgnorePassedMatchingType() {
        return ignorePassedMatchingType;
    }

    /**
     * When looking for terms from a given query ignore the matching type which
     * is passed to the service (CONTAINS, BEGINS_WITH, EXACT) and always use
     * CONTAINS to get maximum number of results (set from gemet.properties)
     */
    public void setIgnorePassedMatchingType(boolean ignorePassedMatchingType) {
        this.ignorePassedMatchingType = ignorePassedMatchingType;
    }

    /**
     * If not null name of term is also fetched in this language and set as
     * alternateName !
     */
    public String getAlternateLanguage() {
        return alternateLanguage;
    }

    /**
     * If not null name of term is also fetched in this language and set as
     * alternateName !
     */
    public void setAlternateLanguage(String alternateLanguage) {
        this.alternateLanguage = alternateLanguage;
    }

    /**
     * NOTICE: Parameter "addDescriptors" is irrelevant !
     * 
     * @see de.ingrid.external.ThesaurusService#findTermsFromQueryTerm(java.lang.String,
     *      de.ingrid.external.ThesaurusService.MatchingType, boolean,
     *      java.util.Locale)
     */
    @Override
    public Term[] findTermsFromQueryTerm(String queryTerm, MatchingType matching, boolean addDescriptors, Locale locale) {
        return findTermsFromQueryTerm( null, queryTerm, matching, addDescriptors, locale );
    }

    /**
     * NOTICE: Parameter "url" and "addDescriptors" are irrelevant ! queryTerm
     * is processed only up to analyzeMaxWords from gemet.properties
     * 
     * @see de.ingrid.external.ThesaurusService#findTermsFromQueryTerm(java.lang.String,
     *      java.lang.String, de.ingrid.external.ThesaurusService.MatchingType,
     *      boolean, java.util.Locale)
     */
    @Override
    public Term[] findTermsFromQueryTerm(String url, String queryTerm, MatchingType matching, boolean addDescriptors, Locale locale) {
        if (queryTerm == null || queryTerm.trim().length() == 0) {
            log.warn("Empty queryTerm ({}) passed, we return empty list !", queryTerm);
            return new Term[] {};
        }

        String language = getGEMETLanguageFilter( locale );
        MatchingConceptsSearchMode gemetSearchMode = getGEMETSearchMode( matching );
        if (this.ignorePassedMatchingType) {
            // we ignore passed search mode and always search concepts
            // containing the query
            gemetSearchMode = MatchingConceptsSearchMode.CONTAINS;
        }

        List<JSONArray> responseList = new ArrayList<JSONArray>();

        // first search exact query term
        responseList.add( gemetClient.getConceptsMatchingKeyword( queryTerm, language, gemetSearchMode ) );

        // then search single keywords
        String[] keywords = processKeywords( queryTerm.trim().split( " " ), this.analyzeMaxWords );
        if (keywords.length > 1) {
            responseList.addAll( gemetClient.getConceptsMatchingKeywords( keywords, language, gemetSearchMode ) );
        }

        List<Term> resultList = new ArrayList<Term>();
        if (responseList != null && responseList.size() > 0) {
            resultList = gemetMapper.mapToTermsWithKeywordsFilter( responseList, keywords, locale );
        }

        // NOTICE: result list does NOT contain additional localization of term!
        // We fetch additional localization if wanted (alternateLanguage set)
        // AND EXACT matching was requested, where we have only "one" term !
        // see https://dev.informationgrid.eu/redmine/issues/363
        if (alternateLanguage != null && MatchingConceptsSearchMode.EXACT.equals( gemetSearchMode )) {
            // fetch JSON
            for (Term result : resultList) {
                if (!alternateLanguage.equals( language )) {
                    gemetMapper.mapAlternateLanguage( gemetClient.getConceptAsJSON( result.getId(), alternateLanguage ), result );
                } else {
                    result.setAlternateName( result.getName() );
                }
            }
// @formatter:off
            // fetch RDF, we comment this one, is slower
/*
            for (int i = 0; i < resultList.size(); i++) {
                resultList.set( i, this.getTermFromRDF( resultList.get( i ).getId(), locale ) );
            }
*/
// @formatter:on
        }

        return resultList.toArray( new Term[resultList.size()] );
    }

    @Override
    public TreeTerm[] getHierarchyNextLevel(String termId, Locale locale) {
        return getHierarchyNextLevel( null, termId, locale );
    }

    // NOTICE: Parameter "url" is irrelevant !
    @Override
    public TreeTerm[] getHierarchyNextLevel(String url, String termId, Locale locale) {
        if (termId == null) {
            return getHierarchyTopLevel( locale );
        }

        if (termId.trim().length() == 0) {
            log.warn("No termId passed ({}), we return empty result !", termId);
            return new TreeTerm[] {};
        }

        String language = getGEMETLanguageFilter( locale );

        // get concept itself, this is the parent
        JSONObject parent = gemetClient.getConceptAsJSON( termId, language );
        // we check on null, cause some concepts are buggy in service !
        // (e.g. concept/15041)
        if (parent == null) {
            log.error("Problems fetching {} we return empty children list !", termId);
            return new TreeTerm[] {};
        }

        JSONArray parentArray = JSONUtils.toJSONArray( parent );

        // get direct children
        List<JSONArray> childrenList = gemetClient.getChildConcepts( termId, language );

        // get children of children (next hierarchy level) and create TreeTerms
        List<TreeTerm> resultList = new ArrayList<TreeTerm>();
        for (JSONArray children : childrenList) {
            Iterator<JSONObject> childrenIterator = children.iterator();
            while (childrenIterator.hasNext()) {

                // map basic TreeTerm
                TreeTerm resultTreeTerm = gemetMapper.mapToTreeTerm( childrenIterator.next(), null, null );

                // add parent to TreeTerm
                gemetMapper.addParentsToTreeTerm( resultTreeTerm, parentArray );

                // get next hierarchy level (subchildren) and add to TreeTerm !
                // This is time consuming, we only do this for terms where we do
                // not know whether there are children !
                // For GROUPS OR SOUPERGROUPS we just add DUMMY CHILD to
                // indicate children, so we reduce requests !
                if (TermType.NODE_LABEL.equals( resultTreeTerm.getType() )) {
                    // set DUMMY CHILD to indicate children
                    resultTreeTerm.addChild( new TreeTermImpl() );

                } else {
                    List<JSONArray> subChildrenList = gemetClient.getChildConcepts( resultTreeTerm.getId(), language );
                    for (JSONArray subChildren : subChildrenList) {
                        gemetMapper.addChildrenToTreeTerm( resultTreeTerm, subChildren );
                    }
                }

                resultList.add( resultTreeTerm );
            }
        }

        return resultList.toArray( new TreeTerm[resultList.size()] );
    }

    private TreeTerm[] getHierarchyTopLevel(Locale locale) {
        String language = getGEMETLanguageFilter( locale );

        // get top supergroups
        JSONArray children = gemetClient.getTopmostConcepts( ConceptType.SOUPERGROUP, language );

        // get children (next hierarchy level) and create TreeTerms
        List<TreeTerm> resultList = new ArrayList<TreeTerm>();
        Iterator<JSONObject> childrenIterator = children.iterator();
        while (childrenIterator.hasNext()) {

            // map basic TreeTerm
            TreeTerm resultTreeTerm = gemetMapper.mapToTreeTerm( childrenIterator.next(), null, null );

            // NOTICE: Do not set parents in TreeTerm, stays null cause is top
            // term

            // get next hierarchy level (subchildren) and add to TreeTerm !
            // For GROUPS OR SOUPERGROUPS we just add DUMMY CHILD to indicate
            // children, so we reduce requests !
            resultTreeTerm.addChild( new TreeTermImpl() );

// @formatter:off
/*
            // get children and add to TreeTerm
            List<JSONArray> subChildrenList = gemetClient.getChildConcepts( resultTreeTerm.getId(), language );
            for (JSONArray subChildren : subChildrenList) {
                gemetMapper.addChildrenToTreeTerm( resultTreeTerm, subChildren );
            }
*/
// @formatter:on
            resultList.add( resultTreeTerm );
        }

        return resultList.toArray( new TreeTerm[resultList.size()] );
    }

    @Override
    public TreeTerm getHierarchyPathToTop(String termId, Locale locale) {
        return getHierarchyPathToTop( null, termId, locale );
    }

    // NOTICE: Parameter "url" is irrelevant !
    @Override
    public TreeTerm getHierarchyPathToTop(String url, String termId, Locale locale) {
        String language = getGEMETLanguageFilter( locale );

        // get concept and map to TreeTerm
        JSONObject inConcept = gemetClient.getConceptAsJSON( termId, language );
        // we check on null, cause some concepts are buggy in service !
        // (e.g. concept/15041)
        if (inConcept == null) {
            log.error("Problems fetching {} we return empty TreeTerm !", termId);
            return new TreeTermImpl();
        }

        TreeTerm resultTreeTerm = gemetMapper.mapToTreeTerm( inConcept, null, null );

        // set parents up to top. We only produce ONE PATH, so no multiple
        // parents are set !
        // We process "stack" until stack is empty

        Stack<TreeTerm> parentStack = new Stack<TreeTerm>();
        parentStack.add( resultTreeTerm );

        while (!parentStack.empty()) {
            TreeTerm currentTerm = parentStack.pop();
            if (currentTerm.getParents() == null) {
                // no processed parents yet, add first parent found !
                processParentsOfTerm( currentTerm, language, true );
                // check parents for null, may be top node
                if (currentTerm.getParents() != null) {
                    parentStack.addAll( currentTerm.getParents() );
                }
            }
        }

        return resultTreeTerm;
    }

    /**
     * Determine parents of passed TreeTerm and set them in TreeTerm. Only first
     * parent can be set.
     * 
     * @param termToProcess
     *            TreeTerm without parents but id
     * @param language
     *            fetch parents in this language
     * @param onlyFirstParent
     *            pass true if only the first parent should be added to the
     *            parents list of the TreeTerm to get only one path to top !
     * @return the passed TreeTerm with parents !
     */
    private TreeTerm processParentsOfTerm(TreeTerm termToProcess, String language, boolean onlyFirstParent) {
        // get parents
        List<JSONArray> parentsList = gemetClient.getParentConcepts( termToProcess.getId(), language );
        for (JSONArray parents : parentsList) {
            if (onlyFirstParent) {
                // only first parent should be set
                if (parents.size() > 0) {
                    gemetMapper.addParentToTreeTerm( termToProcess, (JSONObject) parents.get( 0 ) );
                    break;
                }
            } else {
                gemetMapper.addParentsToTreeTerm( termToProcess, parents );
            }
        }

        return termToProcess;
    }

    @Override
    public RelatedTerm[] getRelatedTermsFromTerm(String termId, Locale locale) {
        if (termId == null || termId.trim().length() == 0) {
            log.warn("No termId passed ({}), we return empty result !", termId);
            return new RelatedTerm[] {};
        }

        List<RelatedTerm> resultList = new ArrayList<RelatedTerm>();
        String language = getGEMETLanguageFilter( locale );

        // we iterate over all relations !
        for (ConceptRelation concRelation : ConceptRelation.values()) {
            JSONArray relatedConcepts = gemetClient.getRelatedConcepts( termId, concRelation, language );
            resultList.addAll( gemetMapper.mapToRelatedTerms( relatedConcepts, concRelation ) );
        }

        return resultList.toArray( new RelatedTerm[resultList.size()] );
    }

    /**
     * NOTICE: Parameter "ignoreCase" is irrelevant ! Keywords are processed
     * only up to analyzeMaxWords from gemet.properties
     * 
     * @see de.ingrid.external.ThesaurusService#getSimilarTermsFromNames(java.lang.String[],
     *      boolean, java.util.Locale)
     */
    @Override
    public Term[] getSimilarTermsFromNames(String[] keywords, boolean ignoreCase, Locale locale) {
        String language = getGEMETLanguageFilter( locale );

        // We fetch similar terms from every single keyword and then compare
        // with other keywords when mapping to terms. GEMET service always
        // ignores case !
        keywords = processKeywords( keywords, this.analyzeMaxWords );
        List<JSONArray> response = gemetClient.getConceptsMatchingKeywords( keywords, language, MatchingConceptsSearchMode.CONTAINS );

        List<Term> resultList = new ArrayList<Term>();
        if (response != null && response.size() > 0) {
            resultList = gemetMapper.mapToTermsWithKeywordsFilter( response, keywords, locale );
        }

        return resultList.toArray( new Term[resultList.size()] );
    }

    @Override
    public Term getTerm(String termId, Locale locale) {
        // response format determined by property
        Term result = null;
        if (doRDF) {
            result = getTermFromRDF( termId, locale );
        } else {
            result = getTermFromJSON( termId, locale );
        }

        return result;
    }

    /**
     * Fetching term as JSON !
     * 
     * @param termId
     *            e.g. http://www.eionet.europa.eu/gemet/concept/6740
     * @param locale
     *            which language to use
     * @return mapped term from JSON
     */
    private Term getTermFromJSON(String termId, Locale locale) {
        String language = getGEMETLanguageFilter( locale );

        JSONObject response = gemetClient.getConceptAsJSON( termId, language );

        Term result = null;
        if (response != null) {
            result = gemetMapper.mapToTerm( response );

            // handle alternate localization, we have to do another request !
            if (alternateLanguage != null) {
                if (!alternateLanguage.equals( language )) {
                    response = gemetClient.getConceptAsJSON( termId, alternateLanguage );
                }
                gemetMapper.mapAlternateLanguage( response, result );
            }
        }

        return result;
    }

    /**
     * Fetching term as RDF !
     * 
     * @param termId
     *            e.g. http://www.eionet.europa.eu/gemet/concept/6740
     * @param locale
     *            which language to use
     * @return mapped term from RDF
     */
    private Term getTermFromRDF(String termId, Locale locale) {
        Resource response = gemetClient.getConceptAsRDF( termId );

        Term result = null;
        if (response != null) {
            result = gemetMapper.mapToTerm( response, getGEMETLanguageFilter( locale ), this.alternateLanguage );
        }

        return result;
    }

    /**
     * NOTICE: Parameter "ignoreCase" is irrelevant ! Parameter
     * "analyzeMaxWords" is overwritten from gemet.properties if larger !
     * 
     * @see de.ingrid.external.ThesaurusService#getTermsFromText(java.lang.String,
     *      int, boolean, java.util.Locale)
     */
    @Override
    public Term[] getTermsFromText(String text, int analyzeMaxWords, boolean ignoreCase, Locale locale) {
        if (text == null || text.trim().length() == 0) {
            log.warn("Empty text ({}) passed, we return empty list !", text);
            return new Term[] {};
        }

        List<JSONArray> responseList = new ArrayList<JSONArray>();
        if (analyzeMaxWords > this.analyzeMaxWords)
            analyzeMaxWords = this.analyzeMaxWords;

        // split text to words, only maximum of words
        String[] keywords = processKeywords( text.trim().split( " " ), analyzeMaxWords );
        if (keywords.length > 1) {
            String language = getGEMETLanguageFilter( locale );
            responseList.addAll( gemetClient.getConceptsMatchingKeywords( keywords, language, MatchingConceptsSearchMode.EXACT ) );
        }

        List<Term> resultList = new ArrayList<Term>();
        if (responseList != null && responseList.size() > 0) {
            resultList = gemetMapper.mapToTerms( responseList );
        }

        return resultList.toArray( new Term[resultList.size()] );
    }

    /**
     * Prepare keywords for GEMET request, e.g. reduce to max number, remove
     * punctuation ...
     * 
     * @param keywords
     * @param maxKeywords
     * @return
     */
    private String[] processKeywords(String[] keywords, int maxKeywords) {
        if (keywords.length > maxKeywords) {
            String[] keywordsMax = new String[maxKeywords];
            for (int i = 0; i < maxKeywords; i++) {
                keywordsMax[i] = keywords[i];
            }
            keywords = keywordsMax;
        }
        // remove all punctuation
        for (int i = 0; i < keywords.length; i++) {
            keywords[i] = keywords[i].replaceAll( "\\p{P}", "" );
        }

        return keywords;
    }

    /**
     * Determine language filter for GEMET dependent from passed locale !
     * 
     * @param locale
     * @return language used in gemet
     */
    private String getGEMETLanguageFilter(Locale locale) {
        // default is german !
        String langFilter = "de";
        if (locale != null) {
            langFilter = locale.getLanguage();
        }

        return langFilter;
    }

    private MatchingConceptsSearchMode getGEMETSearchMode(MatchingType matching) {
        if (MatchingType.BEGINS_WITH.equals( matching )) {
            return MatchingConceptsSearchMode.BEGINS_WITH;
        } else if (MatchingType.CONTAINS.equals( matching )) {
            return MatchingConceptsSearchMode.CONTAINS;
        }

        return MatchingConceptsSearchMode.EXACT;
    }
}
