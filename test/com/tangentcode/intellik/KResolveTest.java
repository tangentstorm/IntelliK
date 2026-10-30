package com.tangentcode.intellik;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
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

    public void testNoClosuresNestedLambdaSeesGlobalNotEnclosingLocal() {
        assertDefineAt(resolve("a:1\nf:{a:2; g:{a<caret>}}\n"), "a", 0);
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
