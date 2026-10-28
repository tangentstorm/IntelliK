package com.x1010data.intellik;

import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.x1010data.intellik.psi.KDefine;
import com.x1010data.intellik.psi.KFile;
import com.x1010data.intellik.psi.KTypes;

/** Smoke tests that the generated lexer and parser load and run inside the platform. */
public class KParsingTest extends BasePlatformTestCase {

    private static final String SAMPLE = new KColorSettingsPage().getDemoText();

    public void testLexerCoversDemoTextWithoutBadCharacters() {
        KLexerAdapter lexer = new KLexerAdapter();
        lexer.start(SAMPLE);
        int tokens = 0;
        boolean sawIdent = false;
        for (IElementType t; (t = lexer.getTokenType()) != null; lexer.advance()) {
            assertNotSame("bad character at " + lexer.getTokenStart(), TokenType.BAD_CHARACTER, t);
            if (t == KTypes.IDENT) sawIdent = true;
            tokens++;
        }
        assertTrue(tokens > 10);
        assertTrue(sawIdent);
    }

    public void testFileTypeAndParserAreRegistered() {
        PsiFile file = myFixture.configureByText("fizz.k", "x: 1 2 3\nf:{x+1}\n");
        assertInstanceOf(file, KFile.class);
        assertSame(KFileType.INSTANCE, file.getFileType());
        assertEmpty(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class));
        assertEquals(2, PsiTreeUtil.findChildrenOfType(file, KDefine.class).size());
    }
}
