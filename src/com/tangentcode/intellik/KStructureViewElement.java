package com.tangentcode.intellik;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.tangentcode.intellik.psi.KDefine;
import com.tangentcode.intellik.psi.KFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class KStructureViewElement implements StructureViewTreeElement, SortableTreeElement {

    private PsiElement element;

    public KStructureViewElement(PsiElement el) {
        this.element = el;
    }

    @Override
    public Object getValue() {
        return element;
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (element instanceof NavigationItem) ((NavigationItem) element).navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return element instanceof NavigationItem && ((NavigationItem) element).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return element instanceof NavigationItem && ((NavigationItem) element).canNavigateToSource();
    }


    @NotNull
    @Override
    public String getAlphaSortKey() {
        String name = element instanceof PsiNamedElement ? ((PsiNamedElement) element).getName() : null;
        return name != null ? name : "";
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
        ItemPresentation p = element instanceof NavigationItem ? ((NavigationItem) element).getPresentation() : null;
        return p != null ? p : new PresentationData(getAlphaSortKey(), null, null, null);
    }

    @NotNull
    @Override
    public TreeElement[] getChildren() {
        if (element instanceof KFile file) {
            List<TreeElement> els = new ArrayList<>();
            for (KDefine def : KResolver.fileGlobals(file).values()) {
                els.add(new KStructureViewElement(def));
            }
            return els.toArray(TreeElement.EMPTY_ARRAY);
        }
        return EMPTY_ARRAY;
    }
}
