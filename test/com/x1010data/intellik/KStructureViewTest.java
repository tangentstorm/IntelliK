package com.x1010data.intellik;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class KStructureViewTest extends BasePlatformTestCase {

    public void testRootElementHonoursNotNullContracts() {
        PsiFile file = myFixture.configureByText("s.k", "f:{x+1}\n");
        KStructureViewModel model = new KStructureViewModel(file);
        try {
            StructureViewTreeElement root = model.getRoot();
            assertNotNull(root.getPresentation());
            assertNotNull(((KStructureViewElement) root).getAlphaSortKey());
            assertFalse("the file node can have children", model.isAlwaysLeaf(root));

            KStructureViewElement other = new KStructureViewElement(file.getFirstChild());
            assertNotNull(other.getPresentation());
            assertNotNull(other.getAlphaSortKey());
            assertTrue(model.isAlwaysLeaf(other));
        } finally {
            model.dispose();
        }
    }

    public void testStringTokensAreStringLiterals() {
        assertTrue(new KParserDefinition().getStringLiteralElements()
                .contains(com.x1010data.intellik.psi.KTypes.STRING));
    }
}
