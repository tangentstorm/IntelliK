package com.x1010data.intellik;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.x1010data.intellik.dialect.KDialect;
import com.x1010data.intellik.dialect.KDialectSettings;

import java.util.List;

public class KCompletionTest extends BasePlatformTestCase {

    private List<String> complete(String text) {
        myFixture.configureByText("c.k", text);
        myFixture.completeBasic();
        return myFixture.getLookupElementStrings();
    }

    public void testUnderscorePrefixOffersMatchingBuiltins() {
        List<String> items = complete("x: _s<caret>\n");
        assertNotNull(items);
        assertContainsElements(items, "_sin", "_sinh", "_sqr", "_sqrt", "_ss", "_ssr", "_sv", "_sm");
        assertDoesntContain(items, "_bin", "youCompleteMe", "if");
    }

    public void testCompletionAfterAKnownBuiltinPrefix() {
        // "_in" is itself a builtin, so the lexer sees BUILTIN followed by the dummy identifier.
        List<String> items = complete("x: _in<caret>\n");
        assertNotNull(items);
        assertContainsElements(items, "_in", "_inv");
    }

    public void testUniqueKeywordIsInserted() {
        assertNull(complete("wh<caret>\n"));
        myFixture.checkResult("while\n");
    }

    public void testUniqueBuiltinAfterDropIsInserted() {
        assertNull(complete("3 _flo<caret>\n"));
        myFixture.checkResult("3 _floor\n");
    }

    public void testNoTableMeansNoBuiltins() {
        KDialectSettings settings = KDialectSettings.getInstance(getProject());
        try {
            settings.setDialect(KDialect.NGN);
            List<String> items = complete("x: _s<caret>\n");
            assertTrue(items == null || items.isEmpty());
        } finally {
            settings.setDialect(KDialect.K3);
        }
    }

    public void testNoCompletionInComments() {
        List<String> items = complete("/ _s<caret>\n");
        assertTrue(items == null || items.isEmpty());
    }
}
