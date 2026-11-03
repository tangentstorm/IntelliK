package com.tangentcode.intellik.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.tangentcode.intellik.KResolver;
import com.tangentcode.intellik.psi.KLambda;
import com.tangentcode.intellik.psi.KParam;
import org.jetbrains.annotations.NotNull;

/** A lambda parameter, as in {@code {[a;b] a+b}}. */
public abstract class KParamMixin extends KNamedElementBase implements KParam {

    protected KParamMixin(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public PsiElement getNameIdentifier() {
        return getFirstChild();
    }

    @Override
    public KLambda getLocalScope() {
        return KResolver.enclosingLambda(this);
    }
}
