package com.tangentcode.intellik;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringActionHandler;
import com.tangentcode.intellik.psi.KDefine;
import com.tangentcode.intellik.refactoring.KExtractFunctionHandler;
import com.tangentcode.intellik.refactoring.KIntroduceVariableHandler;
import com.tangentcode.intellik.psi.KNamedElement;
import com.tangentcode.intellik.psi.impl.KNamedElementBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wires K into the IDE's refactoring actions: in-place rename (locals and parameters as local
 * variables, globals as members), Safe Delete, Extract Function (Ctrl+Alt+M) and Introduce
 * Variable (Ctrl+Alt+V). Inline (Ctrl+Alt+N) is registered separately as an inlineActionHandler.
 */
public class KRefactoringSupportProvider extends RefactoringSupportProvider {

    @Override
    public boolean isInplaceRenameAvailable(@NotNull PsiElement element, @Nullable PsiElement context) {
        return element instanceof KNamedElementBase named && named.getLocalScope() != null;
    }

    @Override
    public boolean isSafeDeleteAvailable(@NotNull PsiElement element) {
        return element instanceof KDefine d && d.getNameIdentifier() != null && !KResolver.isGlobalAssign(d);
    }

    @Nullable
    @Override
    public RefactoringActionHandler getExtractMethodHandler() {
        return new KExtractFunctionHandler();
    }

    @Nullable
    @Override
    public RefactoringActionHandler getIntroduceVariableHandler() {
        return new KIntroduceVariableHandler();
    }

    @Override
    public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement element, @Nullable PsiElement context) {
        return element instanceof KNamedElement;
    }
}
