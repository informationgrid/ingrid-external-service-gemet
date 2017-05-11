package de.ingrid.external.gemet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;

import de.ingrid.external.om.Term;
import de.ingrid.external.om.Term.TermType;

public class GEMETServiceTest {

    private static GEMETService service;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        service = new GEMETService();
        service.init();
    }

    @Test
    public void findTermsFromQueryTerm() {}

    @Test
    public void getHierarchyNextLevel() {}

    @Test
    public void getHierarchyPathToTop() {}

    @Test
    public void getRelatedTermsFromTerm() {}

    @Test
    public void getSimilarTermsFromNames() {}

    @Test
    public void getTerm() {
        // DESCRIPTOR term in german
        String termId = "http://www.eionet.europa.eu/gemet/concept/6740";
        Locale locale = Locale.GERMAN;
        Term term = service.getTerm( termId, locale );
        checkTerm( term, termId, TermType.DESCRIPTOR, "Schutzgebiet" );

        // in english
        locale = Locale.ENGLISH;
        term = service.getTerm( termId, locale );
        checkTerm( term, termId, TermType.DESCRIPTOR, "protected area" );

        // check Umlaute
        termId = "http://www.eionet.europa.eu/gemet/concept/6743";
        locale = Locale.GERMAN;
        term = service.getTerm( termId, locale );
        checkTerm( term, termId, TermType.DESCRIPTOR, "Geschützte Landschaft" );

        // INVALID term
        termId = "wrong id";
        term = service.getTerm( termId, locale );
        assertThat( term, is( nullValue() ) );
    }

    @Test
    public void getTermsFromText() {}

    private void checkTerm(Term term, String id, TermType type, String name) {
        assertThat( term, is( not( nullValue() ) ) );
        assertThat( term.getId(), is( not( nullValue() ) ) );
        assertThat( term.getType(), is( not( nullValue() ) ) );
        assertThat( term.getName(), is( not( nullValue() ) ) );
        if (id != null) {
            assertThat( id, is( term.getId() ) );
        }
        if (type != null) {
            assertThat( type, is( term.getType() ) );
        }
        if (name != null) {
            assertThat( name, is( term.getName() ) );
        }
    }
}
