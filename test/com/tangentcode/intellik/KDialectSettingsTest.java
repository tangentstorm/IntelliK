package com.tangentcode.intellik;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.tangentcode.intellik.dialect.KDialect;
import com.tangentcode.intellik.dialect.KDialectService;
import com.tangentcode.intellik.dialect.KDialectSettings;
import com.tangentcode.intellik.dialect.PrimitiveTable;

public class KDialectSettingsTest extends BasePlatformTestCase {

    public void testDefaultDialectIsK3() {
        KDialectSettings settings = KDialectSettings.getInstance(getProject());
        assertSame(KDialect.K3, settings.getDialect());
        assertEquals("k3", settings.getState().dialect);
    }

    public void testServiceResolvesFileDialectFromProjectSetting() {
        PsiFile file = myFixture.configureByText("a.k", "x:1\n");
        KDialectService service = KDialectService.getInstance(getProject());
        assertSame(KDialect.K3, service.dialectFor(file));
        assertSame(PrimitiveTable.forDialect(KDialect.K3), service.tableFor(file));

        KDialectSettings settings = KDialectSettings.getInstance(getProject());
        try {
            settings.setDialect(KDialect.NGN);
            assertSame(KDialect.NGN, service.dialectFor(file));
            assertSame(PrimitiveTable.EMPTY, service.tableFor(file));
        } finally {
            settings.setDialect(KDialect.K3);
        }
    }

    public void testStateRoundTripsAndUnknownIdFallsBackToK3() {
        KDialectSettings settings = KDialectSettings.getInstance(getProject());
        KDialectSettings.State saved = settings.getState();
        try {
            KDialectSettings.State s = new KDialectSettings.State();
            s.dialect = "ok";
            settings.loadState(s);
            assertSame(KDialect.OK, settings.getDialect());
            s.dialect = "no-such-dialect";
            assertSame(KDialect.K3, settings.getDialect());
        } finally {
            settings.loadState(saved);
        }
    }
}
