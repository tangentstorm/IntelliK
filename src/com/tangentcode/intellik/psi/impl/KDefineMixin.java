package com.tangentcode.intellik.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import com.tangentcode.intellik.refactoring.KRefactorUtil;
import com.tangentcode.intellik.KResolver;
import com.tangentcode.intellik.psi.KDefine;
import com.tangentcode.intellik.psi.KLambda;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** An assignment {@code name:expr}. Plain assignments declare the name; indexed ones ({@code a[i]:v}) don't. */
public abstract class KDefineMixin extends KNamedElementBase implements KDefine {

    protected KDefineMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return KResolver.isDeclaration(this) ? getLvalue().getNameRef().getFirstChild() : null;
    }

    /** Deletes the whole statement (used by Safe Delete). */
    @Override
    public void delete() throws IncorrectOperationException {
        KRefactorUtil.deleteStatement(this);
    }

    @Nullable
    @Override
    public KLambda getLocalScope() {
        return KResolver.isGlobalAssign(this) ? null : KResolver.enclosingLambda(this);
    }
}
