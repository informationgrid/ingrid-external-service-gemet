package de.ingrid.external.gemet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.hp.hpl.jena.rdf.model.Resource;

import de.ingrid.external.ThesaurusService;
import de.ingrid.external.om.RelatedTerm;
import de.ingrid.external.om.Term;
import de.ingrid.external.om.TreeTerm;

public class GEMETService implements ThesaurusService {

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

    public void init() throws Exception {
        ResourceBundle gemetProps = ResourceBundle.getBundle( "gemet" );

        this.doRDF = Boolean.parseBoolean( gemetProps.getString( "service.request.rdf" ) );

        this.gemetClient = new GEMETClient( gemetProps );
        this.gemetMapper = new GEMETMapper();

    }

    @Override
    public Term[] findTermsFromQueryTerm(String arg0, MatchingType arg1, boolean arg2, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Term[] findTermsFromQueryTerm(String arg0, String arg1, MatchingType arg2, boolean arg3, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeTerm[] getHierarchyNextLevel(String arg0, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeTerm[] getHierarchyNextLevel(String arg0, String arg1, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeTerm getHierarchyPathToTop(String arg0, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeTerm getHierarchyPathToTop(String arg0, String arg1, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RelatedTerm[] getRelatedTermsFromTerm(String arg0, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Term[] getSimilarTermsFromNames(String[] keywords, boolean ignoreCase, Locale locale) {
        String language = getGEMETLanguageFilter( locale );

        // We fetch similar terms from every single keyword and then compare
        // with other keywords when mapping to terms. GEMET service always
        // ignores case !

        List<JSONArray> response = gemetClient.getSimilarTerms( keywords, language );

        List<Term> resultList = new ArrayList<Term>();
        if (response != null && response.size() > 0) {
            resultList = gemetMapper.mapSimilarTerms( response, keywords, locale );
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

        JSONObject response = gemetClient.getTermAsJSON( termId, language );

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
        Resource response = gemetClient.getTermAsRDF( termId );

        Term result = null;
        if (response != null) {
            result = gemetMapper.mapToTerm( response, getGEMETLanguageFilter( locale ) );
        }

        return result;
    }

    @Override
    public Term[] getTermsFromText(String arg0, int arg1, boolean arg2, Locale locale) {
        // TODO Auto-generated method stub
        return null;
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
}
