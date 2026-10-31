package com.tangentcode.intellik.refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.tangentcode.intellik.KNamesValidator;
import com.tangentcode.intellik.KResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Asks for the name of a new function or variable. Tests substitute a fixed answer. */
@FunctionalInterface
public interface KNameChooser {

    /** The chosen name, or null if cancelled. {@code context} is where the name must be free. */
    @Nullable
    String choose(@NotNull Project project, @NotNull String title, @NotNull String suggested,
                  @NotNull PsiElement context);

    /** Null if {@code name} can be introduced at {@code context}, else why not. */
    @Nullable
    static String problem(@NotNull Project project, @NotNull PsiElement context, @NotNull String name) {
        if (!new KNamesValidator().isIdentifier(name, project)) return "'" + name + "' is not a valid K name.";
        if (KResolver.resolveName(context, name) != null) return "'" + name + "' is already defined here.";
        return null;
    }

    KNameChooser DIALOG = (project, title, suggested, context) ->
            Messages.showInputDialog(project, "Name:", title, null, suggested, new InputValidatorEx() {
                @Override
                public String getErrorText(String s) {
                    return problem(project, context, s.trim());
                }

                @Override
                public boolean checkInput(String s) {
                    return getErrorText(s) == null;
                }

                @Override
                public boolean canClose(String s) {
                    return checkInput(s);
                }
            });
}
