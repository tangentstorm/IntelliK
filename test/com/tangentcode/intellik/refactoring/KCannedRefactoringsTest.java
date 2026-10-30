package com.tangentcode.intellik.refactoring;

import com.intellij.openapi.ui.TestDialogManager;
import com.intellij.openapi.ui.TestInputDialog;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.safeDelete.SafeDeleteHandler;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class KCannedRefactoringsTest extends BasePlatformTestCase {

    private void extract(String before, String name, String after) {
        myFixture.configureByText("a.k", before);
        new KExtractFunctionHandler((p, t, s, c) -> name)
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null);
        myFixture.checkResult(after);
    }

    private void introduce(String before, String name, String after) {
        myFixture.configureByText("a.k", before);
        new KIntroduceVariableHandler((p, t, s, c) -> name)
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null);
        myFixture.checkResult(after);
    }

    private void inline(String before, String after) {
        myFixture.configureByText("a.k", before);
        PsiElement target = myFixture.getElementAtCaret();
        KInlineHandler h = new KInlineHandler();
        assertTrue(h.canInlineElement(target));
        h.inlineElement(getProject(), myFixture.getEditor(), target);
        myFixture.checkResult(after);
    }

    private void assertRefused(Runnable r, String messagePart) {
        String before = myFixture.getEditor() == null ? null : myFixture.getEditor().getDocument().getText();
        try {
            r.run();
            fail("expected refusal containing: " + messagePart);
        } catch (CommonRefactoringUtil.RefactoringErrorHintException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(messagePart));
        }
        if (before != null) assertEquals("refusal must not change the file", before, myFixture.getEditor().getDocument().getText());
    }

    // ---- Extract Function ----------------------------------------------------------------

    public void testExtractExpressionWithParamsAndLocals() {
        extract("g:1\nf:{[a;b] c:a*2; <selection>b+c*g</selection>}\n", "h",
                "g:1\nh:{[b;c] b+c*g}\nf:{[a;b] c:a*2; h[b;c]}\n");
    }

    public void testExtractImplicitXBecomesExplicitParam() {
        extract("f:{1+<selection>x*2</selection>}\n", "dbl", "dbl:{[x] x*2}\nf:{1+dbl[x]}\n");
    }

    public void testExtractTopLevelExpressionHasNoParams() {
        extract("a:1\nb:2*<selection>a+1</selection>\n", "inc", "a:1\ninc:{[] a+1}\nb:2*inc[]\n");
    }

    public void testExtractLocalThatShadowsGlobalBecomesParam() {
        extract("a:1\nf:{a:2; <selection>a*3</selection>}\n", "g", "a:1\ng:{[a] a*3}\nf:{a:2; g[a]}\n");
    }

    public void testExtractSingleTermInMiddle() {
        extract("r:1+<selection>(2*3)</selection>-4\n", "six", "six:{[] (2*3)}\nr:1+six[]-4\n");
    }

    public void testExtractStatements() {
        extract("f:{[a] <selection>t:a+1\n  u:t*2\n  u-1</selection>}\n", "g",
                "g:{[a] t:a+1\n  u:t*2\n  u-1}\nf:{[a] g[a]}\n");
    }

    public void testExtractRefusesLeftPrefix() {
        myFixture.configureByText("a.k", "r:<selection>a-b</selection>+c\n");
        assertRefused(() -> new KExtractFunctionHandler((p, t, s, c) -> "g")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "complete expression");
    }

    public void testExtractRefusesLocalUsedAfterSelection() {
        myFixture.configureByText("a.k", "f:{[a] <selection>t:a+1</selection>; t*2}\n");
        assertRefused(() -> new KExtractFunctionHandler((p, t, s, c) -> "g")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "used after");
    }

    public void testExtractRefusesAssignmentToOuterLocal() {
        myFixture.configureByText("a.k", "f:{[a] t:1; <selection>t:a</selection>; t}\n");
        assertRefused(() -> new KExtractFunctionHandler((p, t, s, c) -> "g")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "declared outside");
    }

    public void testExtractRefusesGlobalAssign() {
        myFixture.configureByText("a.k", "f:{[a] <selection>g::a</selection>; 1}\n");
        assertRefused(() -> new KExtractFunctionHandler((p, t, s, c) -> "h")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "::");
    }

    public void testExtractRefusesTakenName() {
        myFixture.configureByText("a.k", "a:1\nb:2*<selection>a+1</selection>\n");
        assertRefused(() -> new KExtractFunctionHandler((p, t, s, c) -> "a")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "already defined");
    }

    public void testExtractRefusesWithoutSelection() {
        myFixture.configureByText("a.k", "b:2*a<caret>+1\n");
        assertRefused(() -> new KExtractFunctionHandler((p, t, s, c) -> "g")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "Select");
    }

    // ---- Introduce Variable --------------------------------------------------------------

    public void testIntroduceAtTopLevel() {
        introduce("r:2*<selection>a+b</selection>\n", "v", "v:a+b\nr:2*v\n");
    }

    public void testIntroduceInsideOneLineLambda() {
        introduce("f:{[a] a*<selection>a+1</selection>}\n", "t", "f:{[a] t:a+1; a*t}\n");
    }

    public void testIntroduceInsideMultiLineLambdaKeepsIndent() {
        introduce("f:{[a]\n  r:a*<selection>a+1</selection>\n  r}\n", "t", "f:{[a]\n  t:a+1\n  r:a*t\n  r}\n");
    }

    public void testIntroduceGoesBeforeIfStatementNotIntoIt() {
        introduce("f:{[a] if[a>0; r:<selection>a-1</selection>]; r}\n", "t", "f:{[a] t:a-1; if[a>0; r:t]; r}\n");
    }

    public void testIntroduceRefusesLeftPrefix() {
        myFixture.configureByText("a.k", "r:<selection>a-b</selection>+c\n");
        assertRefused(() -> new KIntroduceVariableHandler((p, t, s, c) -> "v")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "complete expression");
    }

    public void testIntroduceRefusesTakenAndInvalidNames() {
        myFixture.configureByText("a.k", "a:1\nr:<selection>a+1</selection>\n");
        assertRefused(() -> new KIntroduceVariableHandler((p, t, s, c) -> "a")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "already defined");
        assertRefused(() -> new KIntroduceVariableHandler((p, t, s, c) -> "1x")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "not a valid");
        assertRefused(() -> new KIntroduceVariableHandler((p, t, s, c) -> "_bin")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "not a valid");
    }

    public void testIntroduceRefusesEmbeddedAssignment() {
        myFixture.configureByText("a.k", "r:1+<selection>t:2</selection>\n");
        assertRefused(() -> new KIntroduceVariableHandler((p, t, s, c) -> "v")
                .invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), null), "assignment");
    }

    // ---- Inline Variable -----------------------------------------------------------------

    public void testInlineVariableParenthesizesLeftOperandsOnly() {
        inline("t<caret>:a+b\nr:t*2\ns:2*t\n", "r:(a+b)*2\ns:2*a+b\n");
    }

    public void testInlineAtomicValueFromUse() {
        inline("t:5\nr:t<caret>*2\n", "r:5*2\n");
    }

    public void testInlineLocal() {
        inline("f:{[a] t<caret>:a+1; t*t}\n", "f:{[a] (a+1)*a+1}\n");
    }

    public void testInlineParenthesizesNextToStrand() {
        inline("t<caret>:3+4\nr:1 2 t\n", "r:1 2 (3+4)\n");
    }

    public void testInlineParenthesizesIndexedUseAndNegatives() {
        inline("t<caret>:a,b\nr:t[0]\n", "r:(a,b)[0]\n");
        inline("t<caret>:-1\nr:2-t\n", "r:2-(-1)\n");
    }

    public void testInlineRefusesReassigned() {
        myFixture.configureByText("a.k", "t<caret>:1\nt:2\nr:t\n");
        assertRefused(this::inlineAtCaret, "more than once");
    }

    public void testInlineRefusesIndexedAssignment() {
        myFixture.configureByText("a.k", "t<caret>:1 2\nt[0]:5\nr:t\n");
        assertRefused(this::inlineAtCaret, "indexed assignment");
    }

    public void testInlineRefusesModifiedInPlace() {
        myFixture.configureByText("a.k", "t<caret>:1\nt+:1\nr:t\n");
        assertRefused(this::inlineAtCaret, "modified in place");
    }

    public void testInlineRefusesWhenNameIsShadowedAtUse() {
        myFixture.configureByText("a.k", "b:2\nt<caret>:b+1\nf:{b:5; t}\n");
        assertRefused(this::inlineAtCaret, "means something else");
    }

    public void testInlineRefusesWhenDependencyIsReassigned() {
        myFixture.configureByText("a.k", "a:1\nt<caret>:a+1\na:5\nr:t\n");
        assertRefused(this::inlineAtCaret, "reassigned");
    }

    public void testInlineRefusesParameter() {
        myFixture.configureByText("a.k", "f:{[a] a<caret>+1}\n");
        assertRefused(this::inlineAtCaret, "Parameters");
    }

    private void inlineAtCaret() {
        PsiElement target = myFixture.getElementAtCaret();
        new KInlineHandler().inlineElement(getProject(), myFixture.getEditor(), target);
    }

    // ---- Inline Function -----------------------------------------------------------------

    public void testInlineFunctionBracketAndMonadicCalls() {
        inline("inc<caret>:{x+1}\nr:inc[5]\ns:2*inc 3\n", "r:5+1\ns:2*3+1\n");
    }

    public void testInlineFunctionParenthesizesArgsAndResult() {
        inline("add<caret>:{[a;b] a+b}\nr:add[1-2;3]*2\n", "r:((1-2)+3)*2\n");
    }

    public void testInlineFunctionRefusesMultiStatementBody() {
        myFixture.configureByText("a.k", "f<caret>:{t:x; t*2}\nr:f[1]\n");
        assertRefused(this::inlineAtCaret, "single expression");
    }

    public void testInlineFunctionRefusesNonCallUse() {
        myFixture.configureByText("a.k", "f<caret>:{x+1}\nr:f'1 2\n");
        assertRefused(this::inlineAtCaret, "other than");
    }

    public void testInlineFunctionRefusesWrongArity() {
        myFixture.configureByText("a.k", "f<caret>:{[a;b] a+b}\nr:f[1]\n");
        assertRefused(this::inlineAtCaret, "takes 2");
    }

    public void testInlineFunctionRefusesDroppingAnArgument() {
        myFixture.configureByText("a.k", "f<caret>:{[a;b] a}\nr:f[1;2+3]\n");
        assertRefused(this::inlineAtCaret, "no longer be evaluated");
    }

    // ---- Safe Delete ---------------------------------------------------------------------

    public void testSafeDeleteUnusedLine() {
        myFixture.configureByText("a.k", "t<caret>:1\nr:2\n");
        SafeDeleteHandler.invoke(getProject(), new PsiElement[]{myFixture.getElementAtCaret()}, true);
        myFixture.checkResult("r:2\n");
    }

    public void testSafeDeleteStatementInLine() {
        myFixture.configureByText("a.k", "a:1; t<caret>:2; b:3\n");
        SafeDeleteHandler.invoke(getProject(), new PsiElement[]{myFixture.getElementAtCaret()}, true);
        myFixture.checkResult("a:1; b:3\n");
    }

    public void testSafeDeleteUsedReportsConflict() {
        myFixture.configureByText("a.k", "t<caret>:1\nr:t\n");
        try {
            SafeDeleteHandler.invoke(getProject(), new PsiElement[]{myFixture.getElementAtCaret()}, true);
            fail("expected a conflict");
        } catch (BaseRefactoringProcessor.ConflictsInTestsException expected) {
            // the use of t is reported
        }
    }

    // ---- IDE actions are wired -----------------------------------------------------------

    public void testStandardActionsReachTheHandlers() {
        TestInputDialog old = TestDialogManager.setTestInputDialog(message -> "v");
        try {
            myFixture.configureByText("a.k", "r:2*<selection>a+b</selection>\n");
            myFixture.performEditorAction("IntroduceVariable");
            myFixture.checkResult("v:a+b\nr:2*v\n");

            myFixture.configureByText("b.k", "q:2*<selection>c+d</selection>\n");
            TestDialogManager.setTestInputDialog(message -> "g");
            myFixture.performEditorAction("ExtractMethod");
            myFixture.checkResult("g:{[] c+d}\nq:2*g[]\n");

            myFixture.configureByText("c.k", "t<caret>:a+b\nr:t*2\n");
            myFixture.performEditorAction("Inline");
            myFixture.checkResult("r:(a+b)*2\n");
        } finally {
            TestDialogManager.setTestInputDialog(old);
        }
    }
}
