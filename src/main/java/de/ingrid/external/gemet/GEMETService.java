package de.ingrid.external.gemet;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import de.ingrid.external.ThesaurusService;
import de.ingrid.external.om.RelatedTerm;
import de.ingrid.external.om.Term;
import de.ingrid.external.om.TreeTerm;

public class GEMETService implements ThesaurusService {

    private Logger log = Logger.getLogger( GEMETService.class );

    GEMETClient gemetClient;
    GEMETMapper gemetMapper;

    public void init() throws Exception {
        ResourceBundle gemetProps = ResourceBundle.getBundle( "gemet" );
        String serviceUrl = gemetProps.getString( "service.url" );
        String thesaurusUrl = gemetProps.getString( "service.thesaurus_uri" );
        gemetClient = new GEMETClient( serviceUrl, thesaurusUrl );
        gemetMapper = new GEMETMapper();
    }

    @Override
    public Term[] findTermsFromQueryTerm(String arg0, MatchingType arg1, boolean arg2, Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Term[] findTermsFromQueryTerm(String arg0, String arg1, MatchingType arg2, boolean arg3, Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeTerm[] getHierarchyNextLevel(String arg0, Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeTerm[] getHierarchyNextLevel(String arg0, String arg1, Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeTerm getHierarchyPathToTop(String arg0, Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeTerm getHierarchyPathToTop(String arg0, String arg1, Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RelatedTerm[] getRelatedTermsFromTerm(String arg0, Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Term[] getSimilarTermsFromNames(String[] arg0, boolean arg1, Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Term getTerm(String termId, Locale language) {
        if (log.isDebugEnabled()) {
            log.debug( "getTerm(): " + termId + " " + language );
        }

        Term result = null;

        // Resource response = gemetClient.getTerm( termId, language );
        // Term result = gemetMapper.mapToTerm( response );

        if (log.isDebugEnabled()) {
            log.debug( "return term: " + result );
        }

        return result;
    }

    @Override
    public Term[] getTermsFromText(String arg0, int arg1, boolean arg2, Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

}
