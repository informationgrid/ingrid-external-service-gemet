package de.ingrid.external.gemet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;

import de.ingrid.external.ThesaurusService.MatchingType;
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
    public void findTermsFromQueryTerm() {
        // german terms

        // begins with "Wasser"
        Term[] terms = service.findTermsFromQueryTerm( "Wasser", MatchingType.BEGINS_WITH, true, Locale.GERMAN );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), startsWith( "wasser" ) );
        }

        // exact "Wasser"
        terms = service.findTermsFromQueryTerm( "Wasser", MatchingType.EXACT, true, Locale.GERMAN );
        assertThat( terms, arrayWithSize( 1 ) );
        checkTerm( terms[0], null, TermType.DESCRIPTOR, null );
        assertThat( terms[0].getName().toLowerCase(), equalTo( "wasser" ) );

        // contains "Wasser"
        terms = service.findTermsFromQueryTerm( "Wasser", MatchingType.CONTAINS, true, Locale.GERMAN );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), containsString( "wasser" ) );
        }

        // contains "Wasser" and "Schutz"
        terms = service.findTermsFromQueryTerm( "Wasser Schutz", MatchingType.CONTAINS, true, Locale.GERMAN );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), containsString( "wasser" ) );
            assertThat( term.getName().toLowerCase(), containsString( "schutz" ) );
        }

        // begins with "Wasser" or "Schutz" and then contains both
        terms = service.findTermsFromQueryTerm( "Wasser Schutz", MatchingType.BEGINS_WITH, true, Locale.GERMAN );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), anyOf( startsWith( "wasser" ), startsWith( "schutz" ) ) );
        }

        // english term

        // begins with "Water"
        terms = service.findTermsFromQueryTerm( "Water", MatchingType.BEGINS_WITH, true, Locale.ENGLISH );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), startsWith( "water" ) );
        }

        // exact "water (substance)"
        terms = service.findTermsFromQueryTerm( "Water (Substance)", MatchingType.EXACT, true, Locale.ENGLISH );
        assertThat( terms, arrayWithSize( 1 ) );
        checkTerm( terms[0], null, TermType.DESCRIPTOR, null );
        assertThat( terms[0].getName().toLowerCase(), equalTo( "water (substance)" ) );

        // contains "Water" and "Substance"
        terms = service.findTermsFromQueryTerm( "Water Substance", MatchingType.CONTAINS, true, Locale.ENGLISH );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), containsString( "water" ) );
            assertThat( term.getName().toLowerCase(), containsString( "substance" ) );
        }
    }

    @Test
    public void getHierarchyNextLevel() {}

    @Test
    public void getHierarchyPathToTop() {}

    @Test
    public void getRelatedTermsFromTerm() {}

    @Test
    public void getSimilarTermsFromNames() {
        // german term(s)
        Term[] terms = service.getSimilarTermsFromNames( new String[] { "Wasser" }, true, Locale.GERMAN );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), containsString( "wasser" ) );
        }

        terms = service.getSimilarTermsFromNames( new String[] { "Schutz" }, true, Locale.GERMAN );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), containsString( "schutz" ) );
        }

        // only terms containing both keywords !
        terms = service.getSimilarTermsFromNames( new String[] { "Wasser", "Schutz" }, true, Locale.GERMAN );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), containsString( "wasser" ) );
            assertThat( term.getName().toLowerCase(), containsString( "schutz" ) );
        }

        // english term
        terms = service.getSimilarTermsFromNames( new String[] { "Water" }, true, Locale.ENGLISH );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName().toLowerCase(), containsString( "water" ) );
        }
    }

    @Test
    public void getTerm() {
        // check RDF and JSON response
        boolean[] doRDFChoice = { true, false };
        for (boolean doRDF : doRDFChoice) {
            service.setDoRDF( doRDF );

            // DESCRIPTOR term in german
            String termId = "http://www.eionet.europa.eu/gemet/concept/6740";
            Term term = service.getTerm( termId, Locale.GERMAN );
            checkTerm( term, termId, TermType.DESCRIPTOR, "Schutzgebiet" );

            // in english
            term = service.getTerm( termId, Locale.ENGLISH );
            checkTerm( term, termId, TermType.DESCRIPTOR, "protected area" );

            // check Umlaute
            termId = "http://www.eionet.europa.eu/gemet/concept/6743";
            term = service.getTerm( termId, Locale.GERMAN );
            checkTerm( term, termId, TermType.DESCRIPTOR, "Geschützte Landschaft" );

            // INVALID term
            termId = "wrong id";
            term = service.getTerm( termId, Locale.GERMAN );
            assertThat( term, is( nullValue() ) );
        }
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
