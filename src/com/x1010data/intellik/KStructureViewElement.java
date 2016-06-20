package com.x1010data.intellik;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.x1010data.intellik.psi.KDefine;
import com.x1010data.intellik.psi.KFile;
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
        return element instanceof PsiNamedElement ? ((PsiNamedElement) element).getName() : null;
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
        return element instanceof NavigationItem ? ((NavigationItem) element).getPresentation() : null;
    }

    @NotNull
    @Override
    public TreeElement[] getChildren() {
        /* TODO if (element instanceof KFile) {
            KDefine[] defs = PsiTreeUtil.getChildrenOfType(element, KDefine.class);
            List<TreeElement> els = new ArrayList<>(defs.length);
            for (KDefine def : defs) {
                els.add(new KStructureViewElement(def));
            }
            return els.toArray(new TreeElement[els.size()]);
        } else
        */
        return EMPTY_ARRAY;
    }
}
