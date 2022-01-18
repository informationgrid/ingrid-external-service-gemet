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

import java.util.List;
import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;

import de.ingrid.external.ThesaurusService.MatchingType;
import de.ingrid.external.om.RelatedTerm;
import de.ingrid.external.om.RelatedTerm.RelationType;
import de.ingrid.external.om.Term;
import de.ingrid.external.om.Term.TermType;
import de.ingrid.external.om.TreeTerm;

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

        // we always use passed matching type ignoring the one from
        // gemet.properties
        service.setIgnorePassedMatchingType( false );

        // but also check every request with additional localization of term !
        // NOTICE: Only localized when EXACT matching requested !
        boolean[] doAlternateLanguageChoice = { false, true };

        for (boolean doAlternateLanguage : doAlternateLanguageChoice) {
            if (doAlternateLanguage)
                service.setAlternateLanguage( "fr" );
            else
                service.setAlternateLanguage( null );

            // begins with "Wasser"
            Term[] terms = service.findTermsFromQueryTerm( "Wasser", MatchingType.BEGINS_WITH, true, Locale.GERMAN );
            assertThat( terms.length, greaterThan( 0 ) );
            for (Term term : terms) {
                checkTerm( term, null, TermType.DESCRIPTOR, null );
                assertThat( term.getName().toLowerCase(), startsWith( "wasser" ) );
                assertThat( term.getAlternateName(), is( nullValue() ) );
            }

            // exact "Wasser"
            terms = service.findTermsFromQueryTerm( "Wasser", MatchingType.EXACT, true, Locale.GERMAN );
            assertThat( terms, arrayWithSize( 1 ) );
            checkTerm( terms[0], "http://www.eionet.europa.eu/gemet/concept/9242", TermType.DESCRIPTOR, "Wasser" );
            if (doAlternateLanguage)
                assertThat( terms[0].getAlternateName(), is( "eau (substance)" ) );
            else
                assertThat( terms[0].getAlternateName(), is( nullValue() ) );

            // contains "Wasser"
            terms = service.findTermsFromQueryTerm( "Wasser", MatchingType.CONTAINS, true, Locale.GERMAN );
            assertThat( terms.length, greaterThan( 0 ) );
            for (Term term : terms) {
                checkTerm( term, null, TermType.DESCRIPTOR, null );
                assertThat( term.getName().toLowerCase(), containsString( "wasser" ) );
                assertThat( term.getAlternateName(), is( nullValue() ) );
            }

            // contains "Wasser" and "Schutz"
            terms = service.findTermsFromQueryTerm( "Wasser Schutz", MatchingType.CONTAINS, true, Locale.GERMAN );
            assertThat( terms.length, greaterThan( 0 ) );
            for (Term term : terms) {
                checkTerm( term, null, TermType.DESCRIPTOR, null );
                assertThat( term.getName().toLowerCase(), containsString( "wasser" ) );
                assertThat( term.getName().toLowerCase(), containsString( "schutz" ) );
                assertThat( term.getAlternateName(), is( nullValue() ) );
            }

            // begins with "Wasser" or "Schutz" and then contains both
            terms = service.findTermsFromQueryTerm( "Wasser Schutz", MatchingType.BEGINS_WITH, true, Locale.GERMAN );
            assertThat( terms.length, greaterThan( 0 ) );
            for (Term term : terms) {
                checkTerm( term, null, TermType.DESCRIPTOR, null );
                assertThat( term.getName().toLowerCase(), anyOf( startsWith( "wasser" ), startsWith( "schutz" ) ) );
                assertThat( term.getAlternateName(), is( nullValue() ) );
            }

            // english term

            // begins with "Water"
            terms = service.findTermsFromQueryTerm( "Water", MatchingType.BEGINS_WITH, true, Locale.ENGLISH );
            assertThat( terms.length, greaterThan( 0 ) );
            for (Term term : terms) {
                checkTerm( term, null, TermType.DESCRIPTOR, null );
                assertThat( term.getName().toLowerCase(), startsWith( "water" ) );
                assertThat( term.getAlternateName(), is( nullValue() ) );
            }

            // exact "water (substance)"
            terms = service.findTermsFromQueryTerm( "Water (Substance)", MatchingType.EXACT, true, Locale.ENGLISH );
            assertThat( terms, arrayWithSize( 1 ) );
            checkTerm( terms[0], null, TermType.DESCRIPTOR, null );
            assertThat( terms[0].getName().toLowerCase(), equalTo( "water (substance)" ) );
            if (doAlternateLanguage)
                assertThat( terms[0].getAlternateName(), is( "eau (substance)" ) );
            else
                assertThat( terms[0].getAlternateName(), is( nullValue() ) );

            // contains "Water" and "Substance"
            terms = service.findTermsFromQueryTerm( "Water Substance", MatchingType.CONTAINS, true, Locale.ENGLISH );
            assertThat( terms.length, greaterThan( 0 ) );
            for (Term term : terms) {
                checkTerm( term, null, TermType.DESCRIPTOR, null );
                assertThat( term.getName().toLowerCase(), containsString( "water" ) );
                assertThat( term.getName().toLowerCase(), containsString( "substance" ) );
                assertThat( term.getAlternateName(), is( nullValue() ) );
            }
        }
    }

    @Test
    public void getHierarchyNextLevel() {

        // @formatter:off
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
        // @formatter:on

        // TOP TERMS
        TreeTerm[] terms = service.getHierarchyNextLevel( null, Locale.GERMAN );
        assertThat( terms.length, equalTo( 4 ) );
        for (TreeTerm term : terms) {
            assertThat( term.getType(), is( TermType.NODE_LABEL ) );
            assertThat( term.getParents(), equalTo( null ) );
            assertThat( term.getId(),
                    anyOf( containsString( "supergroup/4044" ), containsString( "supergroup/5499" ), containsString( "supergroup/2894" ), containsString( "supergroup/5306" ) ) );
            assertThat(
                    term.getName(),
                    anyOf( equalTo( "ANTHROPOGENE AKTIVITÄTEN UND PRODUKTE, WIRKUNGEN AUF DIE UMWELT" ), equalTo( "NATÜRLICHE UND ANTHROPOGEN ÜBERFORMTE UMWELT" ),
                            equalTo( "SOZIALE ASPEKTE, UMWELTPOLITISCHE MASSNAHMEN" ), equalTo( "ZUSATZVERZEICHNISSE" ) ) );
            // NO, we always pass only one dummy child for groups in backend !
            // assertThat( term.getChildren().size(), anyOf( is( 12 ), is( 9 ),
            // is( 2 ) ) );
            assertThat( term.getChildren().size(), is( 1 ) );
        }

        // supergroup: ZUSATZVERZEICHNISSE -> 2 Untergruppen (type group)
        terms = service.getHierarchyNextLevel( "http://www.eionet.europa.eu/gemet/supergroup/5306", Locale.GERMAN );
        assertThat( terms.length, equalTo( 2 ) );
        for (TreeTerm term : terms) {
            assertThat( term.getType(), is( TermType.NODE_LABEL ) );
            assertThat( term.getParents().get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/supergroup/5306" ) );
            assertThat( term.getId(), anyOf( containsString( "group/10117" ), containsString( "group/14980" ) ) );
            assertThat( term.getName(), anyOf( equalTo( "ALLGEMEINE UND ÜBEGREIFENDE BEGRIFFE" ), equalTo( "HILFSBEGRIFFE" ) ) );
            // only group members which have NO broader concept !!!
            assertThat( term.getChildren().size(), anyOf( is( 2 ), is( 1 ) ) );
        }

        // group: HILFSBEGRIFFE -> 3 group members, but only 1 member has NO
        // broader concept, meaning the only parent is the group !
        terms = service.getHierarchyNextLevel( "http://www.eionet.europa.eu/gemet/group/14980", Locale.GERMAN );
        // only group members which have NO broader concept !!!
        assertThat( terms.length, equalTo( 1 ) );
        for (TreeTerm term : terms) {
            assertThat( term.getType(), is( TermType.DESCRIPTOR ) );
            assertThat( term.getParents().get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/group/14980" ) );
            assertThat( term.getId(), containsString( "concept/14848" ) );
            assertThat( term.getName(), equalTo( "Thema" ) );
            assertThat( term.getChildren().size(), is( 2 ) );
        }

        // concept: Thema -> 2 child concepts
        terms = service.getHierarchyNextLevel( "http://www.eionet.europa.eu/gemet/concept/14848", Locale.GERMAN );
        assertThat( terms.length, equalTo( 2 ) );
        for (TreeTerm term : terms) {
            assertThat( term.getType(), is( TermType.DESCRIPTOR ) );
            assertThat( term.getParents().get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/concept/14848" ) );
            assertThat( term.getId(), anyOf( containsString( "concept/4359" ), containsString( "concept/5825" ) ) );
            assertThat( term.getName(), anyOf( equalTo( "in-situ" ), equalTo( "Off-Site" ) ) );
            assertThat( term.getChildren(), equalTo( null ) );
        }
    }

    @Test
    public void getHierarchyPathToTop() {

        // term with only group parent
        TreeTerm term = service.getHierarchyPathToTop( "http://www.eionet.europa.eu/gemet/concept/11089", Locale.GERMAN );
        checkTerm( term, "http://www.eionet.europa.eu/gemet/concept/11089", TermType.DESCRIPTOR, "Handelsaktivität" );
        assertThat( term.getChildren(), equalTo( null ) );

        // parent 1. level
        List<TreeTerm> parents = term.getParents();
        assertThat( parents.size(), is( 1 ) );
        assertThat( parents.get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/group/10117" ) );
        assertThat( parents.get( 0 ).getName(), equalTo( "ALLGEMEINE UND ÜBEGREIFENDE BEGRIFFE" ) );
        assertThat( parents.get( 0 ).getChildren().get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/concept/11089" ) );
        // parent 2. level (top node)
        parents = parents.get( 0 ).getParents();
        assertThat( parents.size(), is( 1 ) );
        assertThat( parents.get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/supergroup/5306" ) );
        assertThat( parents.get( 0 ).getName(), equalTo( "ZUSATZVERZEICHNISSE" ) );
        assertThat( parents.get( 0 ).getParents(), equalTo( null ) );
        assertThat( parents.get( 0 ).getChildren().get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/group/10117" ) );

        // NOTICE: Circular relations !

        // @formatter:off
        // SOZIALE ASPEKTE,...   ZUSATZVERZEICHNISSE
        //      |                  |
        // WISSEN, ...           HILFSBEGRIFFE
        //      |                  |
        //      |             ----------
        //      |             |        |
        //  Kenngröße         |      Thema
        //      |             |        |
        //      -----------------------|
        //                    |
        //                 Off-Site
        // @formatter:on

        // "Off-Site"
        term = service.getHierarchyPathToTop( "http://www.eionet.europa.eu/gemet/concept/5825", Locale.GERMAN );
        checkTerm( term, "http://www.eionet.europa.eu/gemet/concept/5825", TermType.DESCRIPTOR, "Off-Site" );
        assertThat( term.getChildren(), equalTo( null ) );
        // parent 1. level
        parents = term.getParents();
        assertThat( parents.size(), is( 1 ) );
        assertThat( parents.get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/concept/6033" ) );
        assertThat( parents.get( 0 ).getName(), equalTo( "Kenngröße" ) );
        assertThat( parents.get( 0 ).getChildren().get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/concept/5825" ) );
        // parent 2. level
        parents = parents.get( 0 ).getParents();
        assertThat( parents.size(), is( 1 ) );
        assertThat( parents.get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/group/7136" ) );
        assertThat( parents.get( 0 ).getName(), equalTo( "WISSEN, WISSENSCHAFT, FORSCHUNG, INFORMATIONSGEWINNUNG" ) );
        assertThat( parents.get( 0 ).getChildren().get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/concept/6033" ) );
        // parent 3. level (top node)
        parents = parents.get( 0 ).getParents();
        assertThat( parents.size(), is( 1 ) );
        assertThat( parents.get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/supergroup/2894" ) );
        assertThat( parents.get( 0 ).getName(), equalTo( "SOZIALE ASPEKTE, UMWELTPOLITISCHE MASSNAHMEN" ) );
        assertThat( parents.get( 0 ).getParents(), equalTo( null ) );
        assertThat( parents.get( 0 ).getChildren().get( 0 ).getId(), equalTo( "http://www.eionet.europa.eu/gemet/group/7136" ) );
    }

    @Test
    public void getRelatedTermsFromTerm() {

        // german terms

        // NOTICE: Circular relations !

        // @formatter:off
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
        // @formatter:on

        // supergroup

        // "ANTHROPOGENE AKTIVITÄTEN UND PRODUKTE, WIRKUNGEN AUF DIE UMWELT" ->
        // 12 Untergruppen
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
        // check with alternateName in different language and without
        boolean[] doAlternateLanguageChoice = { true, false };

        for (boolean doRDF : doRDFChoice) {
            service.setDoRDF( doRDF );

            for (boolean doAlternateLanguage : doAlternateLanguageChoice) {
                if (doAlternateLanguage)
                    service.setAlternateLanguage( "fr" );
                else
                    service.setAlternateLanguage( null );

                // DESCRIPTOR term in german
                String termId = "http://www.eionet.europa.eu/gemet/concept/6740";
                Term term = service.getTerm( termId, Locale.GERMAN );
                checkTerm( term, termId, TermType.DESCRIPTOR, "Schutzgebiet" );
                if (doAlternateLanguage)
                    assertThat( term.getAlternateName().toLowerCase(), is( "espace protégé" ) );
                else
                    assertThat( term.getAlternateName(), is( nullValue() ) );

                // in english
                term = service.getTerm( termId, Locale.ENGLISH );
                checkTerm( term, termId, TermType.DESCRIPTOR, "protected area" );
                if (doAlternateLanguage)
                    assertThat( term.getAlternateName().toLowerCase(), is( "espace protégé" ) );
                else
                    assertThat( term.getAlternateName(), is( nullValue() ) );

                // check Umlaute
                termId = "http://www.eionet.europa.eu/gemet/concept/6743";
                term = service.getTerm( termId, Locale.GERMAN );
                checkTerm( term, termId, TermType.DESCRIPTOR, "Geschützte Landschaft" );
                if (doAlternateLanguage)
                    assertThat( term.getAlternateName().toLowerCase(), is( "site naturel prot\u00e9g\u00e9" ) );
                else
                    assertThat( term.getAlternateName(), is( nullValue() ) );

                // INVALID term
                termId = "wrong id";
                term = service.getTerm( termId, Locale.GERMAN );
                assertThat( term, is( nullValue() ) );
            }
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
