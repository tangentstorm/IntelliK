package com.tangentcode.intellik.refactoring;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiElement;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/** Name, "use implicit x/y/z" and a one-line preview of the generated definition. */
class KExtractFunctionDialog extends DialogWrapper {

    private final Project project;
    private final PsiElement context;
    private final boolean implicitPossible;
    private final KExtractFunctionChooser.Preview preview;
    private final JBTextField nameField;
    private final JBCheckBox implicitBox;
    private final JBLabel previewLabel = new JBLabel();

    KExtractFunctionDialog(@NotNull Project project, @NotNull String suggested, @NotNull PsiElement context,
                           boolean implicitPossible, boolean implicitDefault,
                           @NotNull KExtractFunctionChooser.Preview preview) {
        super(project, true);
        this.project = project;
        this.context = context;
        this.implicitPossible = implicitPossible;
        this.preview = preview;
        nameField = new JBTextField(suggested, 20);
        implicitBox = new JBCheckBox("Use implicit x/y/z parameters", implicitPossible && implicitDefault);
        implicitBox.setEnabled(implicitPossible);
        if (!implicitPossible) {
            implicitBox.setToolTipText("Needs 1 to 3 parameters, and the selection must not already use x, y or z.");
        }
        previewLabel.setFont(new Font(EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName(),
                Font.PLAIN, previewLabel.getFont().getSize()));
        nameField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updatePreview();
            }
        });
        implicitBox.addActionListener(e -> updatePreview());
        setTitle(KExtractFunctionHandler.TITLE);
        init();
        updatePreview();
    }

    String name() {
        return nameField.getText().trim();
    }

    boolean implicit() {
        return implicitPossible && implicitBox.isSelected();
    }

    private void updatePreview() {
        String n = name().isEmpty() ? "f" : name();
        String text = preview.definition(n, implicit());
        int nl = text.indexOf('\n');
        if (nl >= 0) text = text.substring(0, nl) + " …";
        previewLabel.setText(text.length() > 80 ? text.substring(0, 79) + "…" : text);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Name:", nameField)
                .addComponent(implicitBox)
                .addLabeledComponent("Preview:", previewLabel)
                .getPanel();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return nameField;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String problem = KNameChooser.problem(project, context, name());
        if (problem == null) problem = KNameChooser.problem(project, context.getContainingFile().getFirstChild(), name());
        return problem == null ? null : new ValidationInfo(problem, nameField);
    }
}
