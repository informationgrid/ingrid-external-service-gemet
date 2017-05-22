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
import de.ingrid.external.om.RelatedTerm;
import de.ingrid.external.om.RelatedTerm.RelationType;
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
        checkTerm( terms[0], "http://www.eionet.europa.eu/gemet/concept/9242", TermType.DESCRIPTOR, "Wasser" );

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
    public void getRelatedTermsFromTerm() {
        // german terms

        // NOTICE: Circular relations !
        
        //              ZUSATZVERZEICHNISSE
        //                      |
        //          -----------------------------
        //          |                           |
        //    HILFSBEGRIFFE            ALLGEMEINE UND ÜBEGREIFENDE BEGRIFFE
        //          |
        //     -------------------------------------
        //     |                |                  |
        //     |              Thema                |
        //     |                |                  |
        //     |  -------------------------------  |
        //     |  |                             |  |
        //   Off-Site                          in-situ

        // supergroup

        // "ANTHROPOGENE AKTIVITÄTEN UND PRODUKTE, WIRKUNGEN AUF DIE UMWELT" -> 12 Untergruppen
        RelatedTerm[] terms = service.getRelatedTermsFromTerm( "http://www.eionet.europa.eu/gemet/supergroup/4044", Locale.GERMAN );
        assertThat( terms.length, equalTo( 12 ) );
        for (RelatedTerm term : terms) {
            checkTerm( term, null, TermType.NODE_LABEL, null );
            assertThat( term.getRelationType().equals( RelationType.CHILD ), is( true ) );
        }

        // ZUSATZVERZEICHNISSE -> 2 Untergruppen
        terms = service.getRelatedTermsFromTerm( "http://www.eionet.europa.eu/gemet/supergroup/5306", Locale.GERMAN );
        assertThat( terms.length, equalTo( 2 ) );
        for (RelatedTerm term : terms) {
            checkTerm( term, null, TermType.NODE_LABEL, null );
            assertThat( term.getRelationType().equals( RelationType.CHILD ), is( true ) );
            assertThat( term.getName(), anyOf( equalTo( "ALLGEMEINE UND ÜBEGREIFENDE BEGRIFFE" ), equalTo( "HILFSBEGRIFFE" ) ) );
        }

        // group
        
        // HILFSBEGRIFFE
        terms = service.getRelatedTermsFromTerm( "http://www.eionet.europa.eu/gemet/group/14980", Locale.GERMAN );
        assertThat( terms.length, equalTo( 4 ) );
        for (RelatedTerm term : terms) {
            assertThat( term.getId(),
                    anyOf( containsString( "supergroup/5306" ), containsString( "concept/4359" ), containsString( "concept/5825" ), containsString( "concept/14848" ) ) );
            if (term.getId().contains( "supergroup/5306" )) {
                checkTerm( term, "http://www.eionet.europa.eu/gemet/supergroup/5306", TermType.NODE_LABEL, "ZUSATZVERZEICHNISSE" );
                assertThat( term.getRelationType().equals( RelationType.PARENT ), is( true ) );
            }
            if (term.getId().contains( "concept/14848" )) {
                checkTerm( term, "http://www.eionet.europa.eu/gemet/concept/14848", TermType.DESCRIPTOR, "Thema" );
                assertThat( term.getRelationType().equals( RelationType.CHILD ), is( true ) );
            }
            if (term.getId().contains( "concept/5825" )) {
                checkTerm( term, "http://www.eionet.europa.eu/gemet/concept/5825", TermType.DESCRIPTOR, "Off-Site" );
                assertThat( term.getRelationType().equals( RelationType.CHILD ), is( true ) );
            }
            if (term.getId().contains( "concept/4359" )) {
                checkTerm( term, "http://www.eionet.europa.eu/gemet/concept/4359", TermType.DESCRIPTOR, "in-situ" );
                assertThat( term.getRelationType().equals( RelationType.CHILD ), is( true ) );
            }
        }

        // concept

        // "Thema" -> NO PARENT, only group as parent !
        terms = service.getRelatedTermsFromTerm( "http://www.eionet.europa.eu/gemet/concept/14848", Locale.GERMAN );
        assertThat( terms.length, equalTo( 3 ) );
        for (RelatedTerm term : terms) {
            assertThat( term.getId(), anyOf( containsString( "concept/4359" ), containsString( "concept/5825" ), containsString( "group/14980" ) ) );
            if (term.getId().contains( "concept/4359" )) {
                checkTerm( term, "http://www.eionet.europa.eu/gemet/concept/4359", TermType.DESCRIPTOR, "in-situ" );
                assertThat( term.getRelationType().equals( RelationType.CHILD ), is( true ) );
            }
            if (term.getId().contains( "concept/5825" )) {
                checkTerm( term, "http://www.eionet.europa.eu/gemet/concept/5825", TermType.DESCRIPTOR, "Off-Site" );
                assertThat( term.getRelationType().equals( RelationType.CHILD ), is( true ) );
            }
            // NO PARENT, only group as parent !
            if (term.getId().contains( "group/14980" )) {
                checkTerm( term, "http://www.eionet.europa.eu/gemet/group/14980", TermType.NODE_LABEL, "HILFSBEGRIFFE" );
                assertThat( term.getRelationType().equals( RelationType.PARENT ), is( true ) );
            }
        }

        // INVALID term
        terms = service.getRelatedTermsFromTerm( "http://www.eionet.europa.eu/gemet/concept/mmmm", Locale.GERMAN );
        assertThat( terms.length, equalTo( 0 ) );
    }

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
    public void getTermsFromText() {
        // german locations

        String text = "Das Waldsterben nimmt zu und liegt am sauren Regen oder Wasser von der Wolke.";

        // analyze full text (100 words)
        Term[] terms = service.getTermsFromText( text, 100, true, Locale.GERMAN );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName(), anyOf( equalTo( "Waldsterben" ), equalTo( "Wasser" ), equalTo( "Wolke" ) ) );
        }

        // analyze only 5 words
        terms = service.getTermsFromText( text, 5, true, Locale.GERMAN );
        assertThat( terms, arrayWithSize( 1 ) );
        checkTerm( terms[0], "http://www.eionet.europa.eu/gemet/concept/12013", TermType.DESCRIPTOR, "Waldsterben" );

        // english results

        terms = service.getTermsFromText( "The death of the forest is increasing and is due to the acid rain or water from the sky.", 100, true, Locale.ENGLISH );
        assertThat( terms.length, greaterThan( 0 ) );
        for (Term term : terms) {
            checkTerm( term, null, TermType.DESCRIPTOR, null );
            assertThat( term.getName(), anyOf( equalTo( "forest" ), equalTo( "acid" ), equalTo( "rain" ) ) );
        }
    }

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
