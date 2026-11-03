package com.tangentcode.intellik.refactoring;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Asks for the new function's name and whether to use implicit x/y/z parameters. Tests substitute a fixed answer. */
@FunctionalInterface
public interface KExtractFunctionChooser {

    /** Remembers the checkbox between invocations. */
    String IMPLICIT_KEY = "intellik.extractFunction.implicitParams";

    record Choice(@NotNull String name, boolean implicit) {
    }

    /** The definition that would be generated for a name and setting. */
    @FunctionalInterface
    interface Preview {
        @NotNull
        String definition(@NotNull String name, boolean implicit);
    }

    /**
     * The choice, or null if cancelled. {@code implicitPossible} is false when the function has no
     * parameters, more than three, or renaming them to x/y/z would clash; then {@code implicit} is ignored.
     */
    @Nullable
    Choice choose(@NotNull Project project, @NotNull String suggested, @NotNull PsiElement context,
                  boolean implicitPossible, @NotNull Preview preview);

    static KExtractFunctionChooser fixed(@NotNull String name, boolean implicit) {
        return (project, suggested, context, possible, preview) -> new Choice(name, implicit);
    }

    KExtractFunctionChooser DIALOG = (project, suggested, context, possible, preview) -> {
        PropertiesComponent props = PropertiesComponent.getInstance();
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            // DialogWrapper can't be shown in tests; ask for the name the way TestInputDialog can answer.
            String name = Messages.showInputDialog(project, "Name:", KExtractFunctionHandler.TITLE, null, suggested, null);
            return name == null ? null : new Choice(name, props.getBoolean(IMPLICIT_KEY, true));
        }
        KExtractFunctionDialog dialog = new KExtractFunctionDialog(project, suggested, context, possible,
                props.getBoolean(IMPLICIT_KEY, true), preview);
        if (!dialog.showAndGet()) return null;
        if (possible) props.setValue(IMPLICIT_KEY, dialog.implicit(), true);
        return new Choice(dialog.name(), dialog.implicit());
    };
}
