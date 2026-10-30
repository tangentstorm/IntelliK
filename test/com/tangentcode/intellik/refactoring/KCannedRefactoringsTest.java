package com.tangentcode.intellik.refactoring;

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

}
