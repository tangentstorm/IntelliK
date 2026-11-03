package com.tangentcode.intellik;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.tangentcode.intellik.dialect.KDialect;
import com.tangentcode.intellik.dialect.KDialectSettings;
import com.tangentcode.intellik.psi.KDefine;
import com.tangentcode.intellik.psi.KLambda;
import com.tangentcode.intellik.psi.KParam;

public class KResolveTest extends BasePlatformTestCase {

    private PsiReference refAtCaret(String text) {
        myFixture.configureByText("a.k", text);
        // The caret sits just after the name, as when typing.
        PsiReference ref = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset() - 1);
        assertNotNull("no reference at caret", ref);
        return ref;
    }

    private PsiElement resolve(String text) {
        return refAtCaret(text).resolve();
    }

    private void assertDefineAt(PsiElement target, String name, int nameOffset) {
        assertInstanceOf(target, KDefine.class);
        assertEquals(name, ((KDefine) target).getName());
        assertEquals(nameOffset, target.getTextOffset());
    }

    public void testGlobal() {
        assertDefineAt(resolve("a:1\nb:a<caret>+1\n"), "a", 0);
    }

    public void testLocalShadowsGlobal() {
        String text = "a:1\nf:{a:2; a<caret>+x}\n";
        assertDefineAt(resolve(text), "a", text.indexOf("{a") + 1);
    }

    public void testLambdaParams() {
        PsiElement p = resolve("f:{[p;q] q<caret>+p}\n");
        assertInstanceOf(p, KParam.class);
        assertEquals("q", ((KParam) p).getName());
    }

    public void testImplicitXResolvesToLambda() {
        assertInstanceOf(resolve("f:{x<caret>+1}\n"), KLambda.class);
    }

    public void testNoImplicitXWhenLambdaHasSignature() {
        assertDefineAt(resolve("x:5\nf:{[a] a+x<caret>}\n"), "x", 0);
    }

    public void testUnresolvedName() {
        assertNull(resolve("b:zz<caret>+1\n"));
    }

    public void testLocalIsNotVisibleOutsideItsLambda() {
        assertNull(resolve("f:{t:1}\nh:t<caret>\n"));
    }

    public void testDoubleColonAssignsGlobal() {
        String text = "f:{g::1}\nh:g<caret>\n";
        assertDefineAt(resolve(text), "g", text.indexOf("g::"));
    }

    // K3 closures: a nested lambda reads the enclosing lambda's names, one level up.

    public void testNestedLambdaSeesEnclosingLocal() {
        String text = "a:1\nf:{a:2; g:{a<caret>}}\n";
        assertDefineAt(resolve(text), "a", text.indexOf("{a") + 1);
    }

    public void testNestedLambdaSeesEnclosingParam() {
        PsiElement p = resolve("f:{[p] {p<caret>+x}}\n");
        assertInstanceOf(p, KParam.class);
        assertEquals("p", ((KParam) p).getName());
    }

    public void testNestedLambdaWithSignatureSeesEnclosingImplicitX() {
        String text = "f:{{[a] a+x<caret>}}\n";
        PsiElement target = resolve(text);
        assertInstanceOf(target, KLambda.class);
        assertEquals(text.indexOf("{"), target.getTextOffset());
    }

    public void testNestedLambdaWithoutSignatureHasItsOwnX() {
        String text = "f:{[q] {x<caret>+q}}\nx:9\n";
        PsiElement target = resolve(text);
        assertInstanceOf(target, KLambda.class);
        assertEquals(text.indexOf("{x"), target.getTextOffset());
    }

    public void testAssignmentInNestedLambdaMakesItsOwnLocal() {
        String text = "f:{a:2; {a:3; a<caret>}}\n";
        assertDefineAt(resolve(text), "a", text.indexOf("a:3"));
    }

    public void testGrandparentLocalIsOutOfReach() {
        assertDefineAt(resolve("a:1\nf:{a:2; {{a<caret>}}}\n"), "a", 0);
    }

    public void testGrandparentLocalReadByTheMiddleLambdaIsVisible() {
        String text = "a:1\nf:{a:2; {a; {a<caret>}}}\n";
        assertDefineAt(resolve(text), "a", text.indexOf("{a:2") + 1);
    }

    public void testEnclosingLocalShadowsGlobalInNestedLambda() {
        String text = "t:0\nf:{t:1; {t<caret>*x}'!3}\n";
        assertDefineAt(resolve(text), "t", text.indexOf("t:1"));
    }

    public void testNoClosuresInQ() {
        KDialectSettings.getInstance(getProject()).setDialect(KDialect.Q);
        try {
            assertDefineAt(resolve("a:1\nf:{a:2; g:{a<caret>}}\n"), "a", 0);
        } finally {
            KDialectSettings.getInstance(getProject()).setDialect(KDialect.DEFAULT);
        }
    }

    public void testOkClosuresReachAnyDepth() {
        KDialectSettings.getInstance(getProject()).setDialect(KDialect.OK);
        try {
            String text = "a:1\nf:{a:2; {{a<caret>}}}\n";
            assertDefineAt(resolve(text), "a", text.indexOf("{a:2") + 1);
        } finally {
            KDialectSettings.getInstance(getProject()).setDialect(KDialect.DEFAULT);
        }
    }

    public void testReassignmentRefersToFirstDeclaration() {
        assertDefineAt(resolve("a:1\na<caret>:2\n"), "a", 0);
    }

    public void testIndexedAssignmentIsAUse() {
        assertDefineAt(resolve("a:1 2\na<caret>[0]:5\n"), "a", 0);
    }

    public void testFirstDeclarationHasNoReference() {
        myFixture.configureByText("a.k", "a<caret>:1\n");
        assertNull(myFixture.getFile().findReferenceAt(myFixture.getCaretOffset() - 1));
        assertInstanceOf(myFixture.getElementAtCaret(), KDefine.class);
    }

    public void testResolvesGlobalsInOtherProjectFiles() {
        myFixture.addFileToProject("lib.k", "lib:{x*2}\n");
        PsiElement target = resolve("r:lib<caret> 3\n");
        assertDefineAt(target, "lib", 0);
        assertEquals("lib.k", target.getContainingFile().getName());
    }

    public void testSameFileGlobalWinsOverOtherFile() {
        myFixture.addFileToProject("lib.k", "v:1\n");
        PsiElement target = resolve("v:2\nr:v<caret>\n");
        assertEquals("a.k", target.getContainingFile().getName());
    }
}
