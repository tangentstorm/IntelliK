package com.tangentcode.intellik;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import com.tangentcode.intellik.psi.KNamedElement;
import com.tangentcode.intellik.psi.impl.KNamedElementBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Enables in-place rename: locals and parameters as local variables, globals as members. */
public class KRefactoringSupportProvider extends RefactoringSupportProvider {

    @Override
    public boolean isInplaceRenameAvailable(@NotNull PsiElement element, @Nullable PsiElement context) {
        return element instanceof KNamedElementBase named && named.getLocalScope() != null;
    }

    @Override
    public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement element, @Nullable PsiElement context) {
        return element instanceof KNamedElement;
    }
}
