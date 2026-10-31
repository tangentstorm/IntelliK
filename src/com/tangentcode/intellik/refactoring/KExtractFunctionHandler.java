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
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.tangentcode.intellik.refactoring.KRefactorUtil.*;

/**
 * Extract Function: moves the selected expression or statements into a new global function placed
 * before the enclosing top-level statement, and replaces the selection with a call.
 *
 * <p>Parameters are the selection's free names that are parameters or locals of the enclosing
 * lambda, in order of first use; globals stay as they are. Generated forms:
 * <ul>
 *   <li>no parameters: {@code f:{...}}, called as {@code f[]};</li>
 *   <li>one to three parameters, with implicit parameters on (the default): they are renamed to
 *       {@code x}, {@code y}, {@code z} in order and the signature is left out, e.g.
 *       {@code f:{x+y}} called as {@code f[a;b]};</li>
 *   <li>otherwise an explicit signature with the original names, {@code f:{[a;b] a+b}}.</li>
 * </ul>
 * A lambda without a signature takes x, y and z implicitly, with arity set by the highest one used.
 * So the implicit form (and, for no parameters, leaving out {@code []}) is only used when nothing
 * else in the selection's own scope is called x, y or z; otherwise the signature is explicit.
 */
public class KExtractFunctionHandler implements RefactoringActionHandler {

    public static final String TITLE = "Extract Function";

    private static final List<String> IMPLICIT = List.of("x", "y", "z");

    private final KExtractFunctionChooser chooser;

    public KExtractFunctionHandler() {
        this(KExtractFunctionChooser.DIALOG);
    }

    public KExtractFunctionHandler(@NotNull KExtractFunctionChooser chooser) {
        this.chooser = chooser;
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

        Plan plan = analyze(root, range, KResolver.enclosingLambda(context), doc.getText(range));

        KExtractFunctionChooser.Choice choice = chooser.choose(project, "f", context, plan.implicitPossible(), plan::definition);
        if (choice == null) return;
        String name = choice.name().trim();
        String problem = KNameChooser.problem(project, context, name);
        if (problem == null) problem = KNameChooser.problem(project, file.getFirstChild(), name);
        if (problem != null) refuse(problem);

        String definition = plan.definition(name, choice.implicit()) + "\n";
        String call = name + "[" + String.join(";", plan.params()) + "]";
        int insertAt = topLevelLineStart(doc, context);

        WriteCommandAction.writeCommandAction(project, file).withName(TITLE).run(() -> {
            doc.replaceString(range.getStartOffset(), range.getEndOffset(), call);
            doc.insertString(insertAt, definition);
            PsiDocumentManager.getInstance(project).commitDocument(doc);
        });
        editor.getSelectionModel().removeSelection();
    }

    /**
     * What to generate. {@code body} is the selected text; {@code implicitBody} is the same text with
     * the parameters renamed to x, y, z (null if that isn't safe); {@code bareOk} says whether a
     * signature-less lambda is safe with no parameters.
     */
    record Plan(List<String> params, String body, @Nullable String implicitBody, boolean bareOk) {

        boolean implicitPossible() {
            return implicitBody != null;
        }

        String definition(String name, boolean implicit) {
            if (params.isEmpty()) return name + (bareOk ? ":{" : ":{[] ") + body + "}";
            if (implicit && implicitBody != null) return name + ":{" + implicitBody + "}";
            return name + ":{[" + String.join(";", params) + "] " + body + "}";
        }
    }

    /** Checks the selection can be moved and works out the parameters. {@code outer} is the enclosing lambda. */
    private static Plan analyze(PsiElement root, TextRange range, @Nullable KLambda outer, String body) {
        Map<String, PsiElement> params = new LinkedHashMap<>();
        List<KNameRef> refs = nameRefsIn(root, range);
        for (KNameRef ref : refs) {
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
            if (outerLocal) params.putIfAbsent(ref.getText(), target);
        }

        // Names at the new function's own level (not inside a lambda nested in the selection)
        // that would become implicit parameters in a lambda without a signature.
        Map<PsiElement, String> renames = new HashMap<>();
        List<String> names = new ArrayList<>(params.keySet());
        for (int i = 0; i < names.size() && i < IMPLICIT.size(); i++) renames.put(params.get(names.get(i)), IMPLICIT.get(i));
        boolean bareOk = true;
        boolean implicitOk = !params.isEmpty() && params.size() <= IMPLICIT.size();
        List<KNameRef> toRename = new ArrayList<>();
        for (KNameRef ref : refs) {
            if (KResolver.enclosingLambda(ref) != outer) continue;
            PsiElement target = KResolver.resolve(ref);
            if (target != null && renames.containsKey(target)) {
                toRename.add(ref);
                continue;
            }
            String head = ref.getText().split("\\.", 2)[0];
            if (IMPLICIT.contains(head)) {
                bareOk = false;
                implicitOk = false;
            }
        }

        String implicitBody = null;
        if (implicitOk) {
            toRename.sort(Comparator.comparingInt(r -> r.getTextRange().getStartOffset()));
            StringBuilder sb = new StringBuilder();
            int at = range.getStartOffset();
            for (KNameRef ref : toRename) {
                TextRange r = ref.getTextRange();
                sb.append(body, at - range.getStartOffset(), r.getStartOffset() - range.getStartOffset());
                sb.append(renames.get(KResolver.resolve(ref)));
                at = r.getEndOffset();
            }
            sb.append(body.substring(at - range.getStartOffset()));
            implicitBody = sb.toString();
        }
        return new Plan(names, body, implicitBody, bareOk);
    }
}
