package de.ingrid.external.gemet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.hp.hpl.jena.rdf.model.Resource;

import de.ingrid.external.ThesaurusService;
import de.ingrid.external.gemet.GEMETClient.ConceptRelation;
import de.ingrid.external.gemet.GEMETClient.ConceptType;
import de.ingrid.external.gemet.GEMETClient.MatchingConceptsSearchMode;
import de.ingrid.external.om.RelatedTerm;
import de.ingrid.external.om.Term;
import de.ingrid.external.om.TreeTerm;

public class GEMETService implements ThesaurusService {

    private final static Logger log = Logger.getLogger( GEMETService.class );

    GEMETClient gemetClient;
    GEMETMapper gemetMapper;

    /** request RDF format from service where possible (true) or JSON (false) */
    Boolean doRDF;

    public Boolean getDoRDF() {
        return doRDF;
    }

    public void setDoRDF(Boolean doRDF) {
        this.doRDF = doRDF;
    }

    // Init Method is called by the Spring Framework on initialization
    public void init() throws Exception {
        ResourceBundle gemetProps = ResourceBundle.getBundle( "gemet" );

        this.doRDF = Boolean.parseBoolean( gemetProps.getString( "service.request.rdf" ) );

        this.gemetClient = new GEMETClient( gemetProps );
        this.gemetMapper = new GEMETMapper();

    }

    // NOTICE: Parameter "addDescriptors" is irrelevant !
    @Override
    public Term[] findTermsFromQueryTerm(String queryTerm, MatchingType matching, boolean addDescriptors, Locale locale) {
        return findTermsFromQueryTerm( null, queryTerm, matching, addDescriptors, locale );
    }

    // NOTICE: Parameter "url" and "addDescriptors" are irrelevant !
    @Override
    public Term[] findTermsFromQueryTerm(String url, String queryTerm, MatchingType matching, boolean addDescriptors, Locale locale) {
        if (queryTerm == null || queryTerm.trim().length() == 0) {
            log.warn( "Empty queryTerm (" + queryTerm + ") passed, we return empty list !" );
            return new Term[] {};
        }

        String language = getGEMETLanguageFilter( locale );
        MatchingConceptsSearchMode gemetSearchMode = getGEMETSearchMode( matching );

        List<JSONArray> responseList = new ArrayList<JSONArray>();

        // first search exact query term
        responseList.add( gemetClient.getConceptsMatchingKeyword( queryTerm, language, gemetSearchMode ) );

        // then search single keywords
        String[] keywords = queryTerm.trim().split( " " );
        if (keywords.length > 1) {
            responseList.addAll( gemetClient.getConceptsMatchingKeywords( keywords, language, gemetSearchMode ) );
        }

        List<Term> resultList = new ArrayList<Term>();
        if (responseList != null && responseList.size() > 0) {
            resultList = gemetMapper.mapToTermsWithKeywordsFilter( responseList, keywords, locale );
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
            log.warn( "No termId passed (" + termId + "), we return empty result !" );
            return new TreeTerm[] {};
        }

        String language = getGEMETLanguageFilter( locale );

        // get concept itself, this is the parent
        JSONObject parent = gemetClient.getConceptAsJSON( termId, language );

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
                gemetMapper.addParentsToTreeTerm( resultTreeTerm, JSONUtils.toJSONArray( parent ) );

                // get children and add to TreeTerm
                List<JSONArray> subChildrenList = gemetClient.getChildConcepts( resultTreeTerm.getId(), language );
                for (JSONArray subChildren : subChildrenList) {
                    gemetMapper.addChildrenToTreeTerm( resultTreeTerm, subChildren );
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

            // get children and add to TreeTerm
            List<JSONArray> subChildrenList = gemetClient.getChildConcepts( resultTreeTerm.getId(), language );
            for (JSONArray subChildren : subChildrenList) {
                gemetMapper.addChildrenToTreeTerm( resultTreeTerm, subChildren );
            }

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
            log.warn( "No termId passed (" + termId + "), we return empty result !" );
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

    // NOTICE: Parameter "ignoreCase" is irrelevant !
    @Override
    public Term[] getSimilarTermsFromNames(String[] keywords, boolean ignoreCase, Locale locale) {
        String language = getGEMETLanguageFilter( locale );

        // We fetch similar terms from every single keyword and then compare
        // with other keywords when mapping to terms. GEMET service always
        // ignores case !

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
            result = gemetMapper.mapToTerm( response, getGEMETLanguageFilter( locale ) );
        }

        return result;
    }

    // NOTICE: Parameter "ignoreCase" is irrelevant !
    @Override
    public Term[] getTermsFromText(String text, int analyzeMaxWords, boolean ignoreCase, Locale locale) {
        if (text == null || text.trim().length() == 0) {
            log.warn( "Empty text (" + text + ") passed, we return empty list !" );
            return new Term[] {};
        }

        List<JSONArray> responseList = new ArrayList<JSONArray>();

        // split text to words, only maximum of words
        String[] keywords = text.trim().split( " " );
        if (keywords.length > 1) {
            if (keywords.length > analyzeMaxWords) {
                String[] keywordsMax = new String[analyzeMaxWords];
                for (int i = 0; i < analyzeMaxWords; i++) {
                    keywordsMax[i] = keywords[i];
                }
                keywords = keywordsMax;
            }
            // remove all punctuation
            for (int i = 0; i < keywords.length; i++) {
                keywords[i] = keywords[i].replaceAll( "\\p{P}", "" );
            }
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
