package de.ingrid.external.gemet;

import org.junit.BeforeClass;
import org.junit.Test;

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
    public void getTerm() {}

    @Test
    public void getTermsFromText() {}

}
