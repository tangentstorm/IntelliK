package com.x1010data.intellik;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.psi.PsiFile;
import com.x1010data.intellik.psi.KFile;
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
        return el instanceof KFile;
    }
}
