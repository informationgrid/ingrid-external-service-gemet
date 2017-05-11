package de.ingrid.external.gemet;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.hp.hpl.jena.rdf.model.Resource;

import de.ingrid.external.ThesaurusService;
import de.ingrid.external.om.RelatedTerm;
import de.ingrid.external.om.Term;
import de.ingrid.external.om.TreeTerm;

public class GEMETService implements ThesaurusService {

    private final static Logger log = Logger.getLogger( GEMETService.class );

    GEMETClient gemetClient;
    GEMETMapper gemetMapper;

    /** request RDF format from service (true) or JSON (false) */
    Boolean doRDF;

    public Boolean getDoRDF() {
        return doRDF;
    }

    public void setDoRDF(Boolean doRDF) {
        this.doRDF = doRDF;
    }

    public void init() throws Exception {
        ResourceBundle gemetProps = ResourceBundle.getBundle( "gemet" );

        gemetClient = new GEMETClient( gemetProps );
        gemetMapper = new GEMETMapper( gemetProps );

        this.doRDF = Boolean.parseBoolean( gemetProps.getString( "service.request.rdf" ) );
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
    public Term[] getSimilarTermsFromNames(String[] arg0, boolean arg1, Locale locale) {
        // TODO Auto-generated method stub
        return null;
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
        Term result = null;
        String language = getGEMETLanguageFilter( locale );
        JSONObject response = gemetClient.getTermAsJSON( termId, language );
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
        Term result = null;
        Resource response = gemetClient.getTermAsRDF( termId );
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
