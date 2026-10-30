package com.tangentcode.intellik;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler;
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.usageView.UsageInfo;
import com.tangentcode.intellik.psi.KDefine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KRefactoringTest extends BasePlatformTestCase {

    public void testRenameGlobalFromUseSkipsShadowingLocal() {
        myFixture.configureByText("a.k", "a:1\nf:{a:2; a+1}\nb:a<caret>+a\na:3\n");
        myFixture.renameElementAtCaret("total");
        myFixture.checkResult("total:1\nf:{a:2; a+1}\nb:total+total\ntotal:3\n");
    }

    public void testRenameFromDeclaration() {
        myFixture.configureByText("a.k", "c<caret>ount:0\ncount:count+1\nf:{count+x}\n");
        myFixture.renameElementAtCaret("n");
        myFixture.checkResult("n:0\nn:n+1\nf:{n+x}\n");
    }

    public void testRenameParamOnlyInsideItsLambda() {
        myFixture.configureByText("a.k", "f:{[p;q] p<caret>+q}\ng:{[p] p}\n");
        myFixture.renameElementAtCaret("n");
        myFixture.checkResult("f:{[n;q] n+q}\ng:{[p] p}\n");
    }

    public void testRenameLocalLeavesGlobalAlone() {
        myFixture.configureByText("a.k", "t:0\nf:{t<caret>:1; t*2}\ng:t\n");
        myFixture.renameElementAtCaret("u");
        myFixture.checkResult("t:0\nf:{u:1; u*2}\ng:t\n");
    }

    public void testRenameAcrossFiles() {
        myFixture.addFileToProject("lib.k", "lib:{x*2}\n");
        myFixture.configureByText("main.k", "r:lib<caret> 3\n");
        myFixture.renameElementAtCaret("dbl");
        myFixture.checkResult("r:dbl 3\n");
        assertEquals("dbl:{x*2}\n", psiText("lib.k"));
    }

    private String psiText(String path) {
        return com.intellij.psi.PsiManager.getInstance(getProject())
                .findFile(myFixture.findFileInTempDir(path)).getText();
    }

    public void testInplaceRenameOfLocal() {
        myFixture.configureByText("a.k", "t:0\nf:{[p] q:p<caret>+1; q*p}\n");
        CodeInsightTestUtil.doInlineRename(new VariableInplaceRenameHandler(), "n", myFixture);
        myFixture.checkResult("t:0\nf:{[n] q:n+1; q*n}\n");
    }

    public void testInplaceRenameOfGlobal() {
        myFixture.configureByText("a.k", "total:0\nf:{tot<caret>al+x}\n");
        CodeInsightTestUtil.doInlineRename(new MemberInplaceRenameHandler(), "sum", myFixture);
        myFixture.checkResult("sum:0\nf:{sum+x}\n");
    }

    public void testRenameRejectsInvalidNames() {
        myFixture.configureByText("a.k", "a<caret>:1\n");
        PsiElement def = myFixture.getElementAtCaret();
        assertTrue(RenameUtil.isValidName(getProject(), def, "good_name1"));
        assertFalse(RenameUtil.isValidName(getProject(), def, "1abc"));
        assertFalse(RenameUtil.isValidName(getProject(), def, "_bin"));
        assertFalse(RenameUtil.isValidName(getProject(), def, "_new"));
        assertFalse(RenameUtil.isValidName(getProject(), def, "if"));
        assertFalse(RenameUtil.isValidName(getProject(), def, "a.b"));
        assertFalse(RenameUtil.isValidName(getProject(), def, "a b"));
    }

    public void testFindUsages() {
        myFixture.addFileToProject("other.k", "z:a*2\n");
        myFixture.configureByText("a.k", "a<caret>:1\nb:a+a\nf:{a:3; a}\na:2\n");
        Collection<UsageInfo> usages = myFixture.findUsages(myFixture.getElementAtCaret());
        // b:a+a (2), the reassignment a:2 (1), other.k (1); the lambda's local a is not a usage.
        assertEquals(4, usages.size());
    }

    public void testHighlightUsagesInFile() {
        myFixture.configureByText("a.k", "a:1\nb:a<caret>+a\nf:{a:3; a}\n");
        RangeHighlighter[] hl = myFixture.testHighlightUsages("a.k");
        List<Integer> starts = new ArrayList<>();
        for (RangeHighlighter h : hl) starts.add(h.getStartOffset());
        starts.sort(null);
        assertEquals(List.of(0, 6, 8), starts);
    }

    public void testGotoDeclaration() {
        myFixture.configureByText("a.k", "x:5\nfoo:{[a] a+1}\nbar:fo<caret>o x\n");
        myFixture.performEditorAction(IdeActions.ACTION_GOTO_DECLARATION);
        assertEquals("x:5\n".length(), myFixture.getCaretOffset());
    }

    public void testGotoParamDeclaration() {
        myFixture.configureByText("a.k", "foo:{[a;b] b<caret>+a}\n");
        myFixture.performEditorAction(IdeActions.ACTION_GOTO_DECLARATION);
        assertEquals("foo:{[a;".length(), myFixture.getCaretOffset());
    }

    public void testCompletionOffersVisibleNames() {
        myFixture.configureByText("a.k", "alpha:1\nf:{[beta] be<caret>}\n");
        myFixture.completeBasic();
        myFixture.checkResult("alpha:1\nf:{[beta] beta}\n");
    }

    public void testStructureViewListsTopLevelDefinitions() {
        myFixture.configureByText("a.k", "a:1\nf:{t:2; g::3}\na:4\n");
        KStructureViewModel model = new KStructureViewModel(myFixture.getFile());
        try {
            List<String> names = new ArrayList<>();
            for (var child : model.getRoot().getChildren()) {
                names.add(((KDefine) ((KStructureViewElement) child).getValue()).getName());
            }
            assertEquals(List.of("a", "f", "g"), names);
        } finally {
            model.dispose();
        }
    }
}
