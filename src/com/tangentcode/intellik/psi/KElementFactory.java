package com.tangentcode.intellik.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.tangentcode.intellik.KLanguage;
import org.jetbrains.annotations.NotNull;

/** Builds PSI fragments from text, for refactorings. */
public final class KElementFactory {
    private KElementFactory() {
    }

    /** A fresh IDENT leaf with the given text. */
    @NotNull
    public static PsiElement createIdentifier(@NotNull Project project, @NotNull String name) {
        KFile file = (KFile) PsiFileFactory.getInstance(project)
                .createFileFromText("dummy.k", KLanguage.INSTANCE, name + "\n");
        KNameRef ref = PsiTreeUtil.findChildOfType(file, KNameRef.class);
        if (ref == null || !name.equals(ref.getText())) {
            throw new IllegalArgumentException("Not a K identifier: " + name);
        }
        return ref.getFirstChild();
    }
}
