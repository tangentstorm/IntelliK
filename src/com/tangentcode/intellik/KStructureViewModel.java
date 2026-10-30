package com.tangentcode.intellik;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.psi.PsiFile;
import com.tangentcode.intellik.psi.KFile;
import org.jetbrains.annotations.NotNull;


public class KStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {

    public KStructureViewModel(PsiFile psiFile) {
        super(psiFile, new KStructureViewElement(psiFile));
    }

    @NotNull
    public Sorter[] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement el) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement el) {
        // Only the file node can have children; everything below it is a leaf.
        return !(el.getValue() instanceof KFile);
    }
}
