package com.tangentcode.intellik.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.tangentcode.intellik.KReference;
import com.tangentcode.intellik.KResolver;
import com.tangentcode.intellik.psi.KDefine;
import com.tangentcode.intellik.psi.KElementFactory;
import com.tangentcode.intellik.psi.KNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** An identifier in expression or assignment position. */
public abstract class KNameRefMixin extends ASTWrapperPsiElement implements KNameRef {

    protected KNameRefMixin(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * The reference to this name's declaration. Null when this identifier <em>is</em> the
     * declaration (the first assignment of the name in its scope), so the platform treats it
     * as the declared name rather than as a use.
     */
    @Nullable
    @Override
    public PsiReference getReference() {
        PsiElement target = KResolver.resolve(this);
        if (target instanceof KDefine def && def.getNameIdentifier() == getFirstChild()) return null;
        return new KReference(this);
    }

    @Override
    public PsiReference @NotNull [] getReferences() {
        PsiReference ref = getReference();
        return ref == null ? PsiReference.EMPTY_ARRAY : new PsiReference[]{ref};
    }

    /** Replaces the identifier text, used by rename. */
    public void rename(@NotNull String name) {
        getFirstChild().replace(KElementFactory.createIdentifier(getProject(), name));
    }

}
