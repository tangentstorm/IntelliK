package com.tangentcode.intellik.refactoring;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.RefactoringActionHandler;
import com.tangentcode.intellik.KResolver;
import com.tangentcode.intellik.psi.*;
import com.tangentcode.intellik.psi.impl.KNamedElementBase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.tangentcode.intellik.refactoring.KRefactorUtil.*;

/**
 * Extract Function: moves the selected expression or statements into a new global
 * {@code name:{[a;b] ...}} placed before the enclosing top-level statement, and replaces the
 * selection with the call {@code name[a;b]} ({@code name[]} when there are no parameters).
 *
 * <p>Parameters are the selection's free names that are parameters or locals of the enclosing
 * lambda, in order of first use. Globals stay as they are. The new function always gets an
 * explicit signature, so a global {@code x} in the body never turns into an implicit argument.
 */
public class KExtractFunctionHandler implements RefactoringActionHandler {

    public static final String TITLE = "Extract Function";

    private final KNameChooser names;

    public KExtractFunctionHandler() {
        this(KNameChooser.DIALOG);
    }

    public KExtractFunctionHandler(@NotNull KNameChooser names) {
        this.names = names;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        if (editor == null || !(file instanceof KFile)) return;
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        try {
            run(project, editor, file);
        } catch (Refusal r) {
            showError(project, editor, TITLE, r.getMessage());
        }
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] elements, DataContext dataContext) {
        // Only available from the editor.
    }

    private void run(Project project, Editor editor, PsiFile file) {
        SelectionModel sel = editor.getSelectionModel();
        if (!sel.hasSelection()) refuse("Select an expression or whole statements to extract.");
        Document doc = editor.getDocument();
        TextRange range = trimmed(doc, sel.getSelectionStart(), sel.getSelectionEnd());

        PsiElement root;
        PsiElement context;
        ExprSelection expr = findExpression(file, range);
        if (expr != null) {
            root = expr.expr();
            context = expr.first();
        } else {
            StmtSelection stmts = findStatements(file, range);
            if (stmts == null) {
                refuse("Select a complete expression or whole statements. K evaluates right to left, "
                        + "so only a single term or the right-hand end of an expression is a complete expression.");
            }
            root = stmts.scope();
            context = stmts.first();
        }

        List<String> params = analyze(root, range);

        String name = names.choose(project, TITLE, "f", context);
        if (name == null) return;
        String problem = KNameChooser.problem(project, context, name);
        if (problem == null) problem = KNameChooser.problem(project, file.getFirstChild(), name);
        if (problem != null) refuse(problem);

        String sig = String.join(";", params);
        String body = doc.getText(range);
        String definition = name + ":{[" + sig + "] " + body + "}\n";
        String call = name + "[" + sig + "]";
        int insertAt = topLevelLineStart(doc, context);

        WriteCommandAction.writeCommandAction(project, file).withName(TITLE).run(() -> {
            doc.replaceString(range.getStartOffset(), range.getEndOffset(), call);
            doc.insertString(insertAt, definition);
            PsiDocumentManager.getInstance(project).commitDocument(doc);
        });
        editor.getSelectionModel().removeSelection();
    }

    /** Checks the selection can be moved and returns the parameter names. */
    private static List<String> analyze(PsiElement root, TextRange range) {
        Set<String> params = new LinkedHashSet<>();
        for (KNameRef ref : nameRefsIn(root, range)) {
            PsiElement target = KResolver.resolve(ref);
            KDefine assignment = assignmentOf(ref);
            if (assignment != null) {
                if (KResolver.isGlobalAssign(assignment)) {
                    refuse("The selection assigns a global with '::'.");
                }
                if (target != null && !range.contains(target.getTextRange())) {
                    refuse("The selection assigns to '" + ref.getText() + "', which is declared outside it. "
                            + "In the new function that would be a separate local.");
                }
                if (assignment.getNameIdentifier() != null && target == assignment) {
                    for (PsiReference use : ReferencesSearch.search(assignment, assignment.getUseScope()).findAll()) {
                        if (!range.contains(use.getElement().getTextRange())) {
                            refuse("'" + ref.getText() + "' is assigned in the selection and used after it.");
                        }
                    }
                }
                continue;
            }
            if (target == null || range.contains(target.getTextRange())) continue;
            if (isWrite(ref)) {
                refuse("The selection modifies '" + ref.getText() + "', which is declared outside it.");
            }
            boolean outerLocal = target instanceof KParam || target instanceof KLambda
                    || (target instanceof KNamedElementBase named && named.getLocalScope() != null);
            if (outerLocal) params.add(ref.getText());
        }
        return new ArrayList<>(params);
    }
}
