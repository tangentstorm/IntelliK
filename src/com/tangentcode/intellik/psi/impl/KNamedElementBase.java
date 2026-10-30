package com.tangentcode.intellik.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import com.tangentcode.intellik.KIcons;
import com.tangentcode.intellik.psi.KElementFactory;
import com.tangentcode.intellik.psi.KLambda;
import com.tangentcode.intellik.psi.KNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared name handling for {@link KDefineMixin} and {@link KParamMixin}. */
public abstract class KNamedElementBase extends ASTWrapperPsiElement implements KNamedElement {

    protected KNamedElementBase(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    @Override
    public String getName() {
        PsiElement id = getNameIdentifier();
        return id == null ? null : id.getText();
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        PsiElement id = getNameIdentifier();
        if (id == null) throw new IncorrectOperationException("No name to rename");
        id.replace(KElementFactory.createIdentifier(getProject(), name));
        return this;
    }

    @Override
    public int getTextOffset() {
        PsiElement id = getNameIdentifier();
        return id == null ? super.getTextOffset() : id.getTextOffset();
    }

    /** The lambda this name is local to, or null if it is global. */
    @Nullable
    public abstract KLambda getLocalScope();

    @NotNull
    @Override
    public SearchScope getUseScope() {
        KLambda scope = getLocalScope();
        return scope != null ? new LocalSearchScope(scope) : super.getUseScope();
    }

    @Override
    public ItemPresentation getPresentation() {
        String name = getName();
        return new PresentationData(name == null ? "" : name, null, KIcons.FILE, null);
    }
}
