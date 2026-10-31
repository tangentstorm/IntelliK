package com.tangentcode.intellik.refactoring;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;
import com.tangentcode.intellik.KResolver;
import com.tangentcode.intellik.psi.KDefine;
import com.tangentcode.intellik.psi.KFile;
import com.tangentcode.intellik.psi.KNameRef;
import com.tangentcode.intellik.psi.KStmt;
import org.jetbrains.annotations.NotNull;

import static com.tangentcode.intellik.refactoring.KRefactorUtil.*;

/**
 * Introduce Variable: assigns the selected expression to a new name in a statement placed
 * just before the current statement, in the same scope, and replaces the selection with the name.
 * The new statement goes on its own line when the current statement starts a line, otherwise it
 * is joined with {@code ;}.
 */
public class KIntroduceVariableHandler implements RefactoringActionHandler {

    public static final String TITLE = "Introduce Variable";

    private final KNameChooser names;

    public KIntroduceVariableHandler() {
        this(KNameChooser.DIALOG);
    }

    public KIntroduceVariableHandler(@NotNull KNameChooser names) {
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
    }

    private void run(Project project, Editor editor, PsiFile file) {
        SelectionModel sel = editor.getSelectionModel();
        if (!sel.hasSelection()) refuse("Select an expression to introduce a variable for.");
        Document doc = editor.getDocument();
        TextRange range = trimmed(doc, sel.getSelectionStart(), sel.getSelectionEnd());
        ExprSelection expr = findExpression(file, range);
        if (expr == null) {
            refuse("Select a complete expression. K evaluates right to left, so only a single term "
                    + "or the right-hand end of an expression is a complete expression.");
        }
        KStmt anchor = anchorStatement(expr.expr());
        if (anchor == null) refuse("Cannot find the statement to put the new variable before.");

        for (KNameRef ref : nameRefsIn(expr.expr(), range)) {
            if (isWrite(ref)) refuse("The selection contains an assignment to '" + ref.getText() + "'.");
            PsiElement target = KResolver.resolve(ref);
            if (target instanceof KDefine && anchor.getTextRange().contains(target.getTextRange())
                    && target.getTextRange().getStartOffset() < range.getStartOffset()) {
                refuse("'" + ref.getText() + "' is assigned earlier in the same statement, "
                        + "so the expression can't move before it.");
            }
        }

        String name = names.choose(project, TITLE, "v", expr.first());
        if (name == null) return;
        String problem = KNameChooser.problem(project, expr.first(), name);
        if (problem != null) refuse(problem);

        String value = doc.getText(range);
        int at = anchor.getTextRange().getStartOffset();
        int lineStart = doc.getLineStartOffset(doc.getLineNumber(at));
        String before = doc.getText(new TextRange(lineStart, at));
        String statement = before.isBlank()
                ? name + ":" + value + "\n" + before
                : name + ":" + value + "; ";

        WriteCommandAction.writeCommandAction(project, file).withName(TITLE).run(() -> {
            doc.replaceString(range.getStartOffset(), range.getEndOffset(), name);
            doc.insertString(at, statement);
            PsiDocumentManager.getInstance(project).commitDocument(doc);
        });
        editor.getSelectionModel().removeSelection();
    }
}
