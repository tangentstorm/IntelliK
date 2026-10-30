package com.tangentcode.intellik.refactoring;

import com.intellij.lang.Language;
import com.intellij.lang.refactoring.InlineActionHandler;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.tangentcode.intellik.KLanguage;
import com.tangentcode.intellik.KResolver;
import com.tangentcode.intellik.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.tangentcode.intellik.refactoring.KRefactorUtil.*;

/**
 * Inline Variable / Inline Function (Ctrl+Alt+N) for a name assigned once.
 *
 * <ul>
 *   <li>Variable: every use is replaced by the assigned expression, in parentheses unless the
 *       expression is a single term or the use is the right-hand end of its expression.</li>
 *   <li>Function ({@code f:{...}} whose body is one expression): every use must be a call,
 *       {@code f[a;b]}, or {@code f a} for a one-argument function. Arguments replace the
 *       parameters in the body, parenthesized by the same rule.</li>
 * </ul>
 * Then the definition is deleted. Anything else is refused and nothing changes.
 */
public class KInlineHandler extends InlineActionHandler {

    public static final String TITLE = "Inline";

    @Override
    public boolean isEnabledForLanguage(Language l) {
        return l == KLanguage.INSTANCE;
    }

    @Override
    public boolean canInlineElement(PsiElement element) {
        return element instanceof KDefine || element instanceof KParam;
    }

    @Override
    public void inlineElement(Project project, Editor editor, PsiElement element) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        try {
            if (!(element instanceof KDefine def)) throw new Refusal("Parameters can't be inlined.");
            inline(project, def);
        } catch (Refusal r) {
            showError(project, editor, def(element), r.getMessage());
        }
    }

    private static String def(PsiElement e) {
        return e instanceof KDefine d && KResolver.definesLambda(d) ? "Inline Function" : "Inline Variable";
    }

    /** A planned text replacement. */
    private record Edit(PsiFile file, TextRange range, String text) {
    }

    static void inline(Project project, KDefine def) {
        String name = def.getName();
        if (name == null) refuse("This is an indexed assignment, not a definition.");
        if (KResolver.isGlobalAssign(def)) refuse("'" + name + "' is assigned with '::'; inlining that isn't supported.");
        for (KDefine d : PsiTreeUtil.findChildrenOfType(def.getExpr(), KDefine.class)) {
            KLambda scope = KResolver.enclosingLambda(d);
            if (scope == null || !PsiTreeUtil.isAncestor(def.getExpr(), scope, true)) {
                refuse("The value of '" + name + "' contains an assignment.");
            }
        }

        List<KNameRef> uses = new ArrayList<>();
        for (PsiReference r : ReferencesSearch.search(def, def.getUseScope()).findAll()) {
            if (!(r.getElement() instanceof KNameRef ref)) refuse("Unexpected use of '" + name + "'.");
            KDefine assignment = assignmentOf((KNameRef) r.getElement());
            if (assignment != null) {
                refuse(assignment.getNameIdentifier() == null
                        ? "'" + name + "' is changed by an indexed assignment."
                        : "'" + name + "' is assigned more than once.");
            }
            if (isWrite((KNameRef) r.getElement())) refuse("'" + name + "' is modified in place.");
            uses.add((KNameRef) r.getElement());
        }

        List<Edit> edits = KResolver.definesLambda(def) ? inlineFunction(project, def, uses) : inlineVariable(project, def, uses);
        apply(project, def, edits);
    }

    // ---- variable ------------------------------------------------------------------------

    private static List<Edit> inlineVariable(Project project, KDefine def, List<KNameRef> uses) {
        KExpr value = def.getExpr();
        checkFreeNames(def.getName(), value, Set.of(), uses);
        String text = value.getText();
        boolean atomic = isAtomic(value);
        List<Edit> edits = new ArrayList<>();
        for (KNameRef use : uses) {
            PsiElement term = termOf(use);
            if (term == null) refuse("Unexpected use of '" + def.getName() + "'.");
            boolean needed = needsParens(term) || use.getTextRange().getEndOffset() != term.getTextRange().getEndOffset();
            edits.add(new Edit(use.getContainingFile(), use.getTextRange(), parenthesize(text, atomic, needed)));
        }
        return edits;
    }

    /**
     * Each name the moved code uses must mean the same thing at every destination. Names that
     * resolve to {@code ownNames} (the function's own parameters) and names in nested lambdas
     * are position independent.
     */
    private static void checkFreeNames(String what, PsiElement code, Set<PsiElement> ownNames, List<KNameRef> uses) {
        KLambda home = KResolver.enclosingLambda(code);
        for (KNameRef r : PsiTreeUtil.findChildrenOfType(code, KNameRef.class)) {
            KLambda scope = KResolver.enclosingLambda(r);
            if (scope != home && scope != null && PsiTreeUtil.isAncestor(code, scope, false)) continue;
            PsiElement target = KResolver.resolve(r);
            if (target != null && ownNames.contains(target)) continue;
            if (target != null && PsiTreeUtil.isAncestor(code, target, false)) continue;
            if (target instanceof KDefine d) {
                for (PsiReference other : ReferencesSearch.search(d, d.getUseScope()).findAll()) {
                    if (other.getElement() instanceof KNameRef o && isWrite(o)) {
                        refuse("'" + what + "' uses '" + r.getText() + "', which is reassigned, so inlining could change its value.");
                    }
                }
            }
            for (KNameRef use : uses) {
                if (KResolver.resolveName(use, r.getText()) != target) {
                    refuse("'" + r.getText() + "' means something else where '" + what + "' is used.");
                }
            }
        }
    }

    // ---- function ------------------------------------------------------------------------

    private static List<Edit> inlineFunction(Project project, KDefine def, List<KNameRef> uses) {
        String name = def.getName();
        KLambda lambda = Objects.requireNonNull(((KValue) def.getExpr().getFirstChild()).getLambda());
        KBlock block = lambda.getBlock();
        List<KStmt> stmts = block.getStmts() == null ? List.of() : block.getStmts().getStmtList();
        List<KStmt> nonEmpty = stmts.stream().filter(s -> s.getTextLength() > 0).toList();
        if (!block.getItemList().isEmpty() || nonEmpty.size() != 1 || nonEmpty.get(0).getExpr() == null
                || PsiTreeUtil.findChildOfType(block, PsiComment.class) != null) {
            refuse("Only functions whose body is a single expression can be inlined.");
        }
        KExpr body = nonEmpty.get(0).getExpr();
        for (KDefine d : PsiTreeUtil.findChildrenOfType(body, KDefine.class)) {
            if (KResolver.enclosingLambda(d) == lambda) refuse("The body of '" + name + "' contains an assignment.");
        }

        // Parameters: explicit signature, else the implicit x, y, z actually used.
        List<String> params = new ArrayList<>();
        Set<PsiElement> own = new HashSet<>();
        own.add(lambda);
        if (lambda.getSig() != null) {
            for (KParam p : lambda.getSig().getParamList()) {
                params.add(p.getText());
                own.add(p);
            }
        } else {
            int valence = 0;
            for (KNameRef r : PsiTreeUtil.findChildrenOfType(body, KNameRef.class)) {
                if (KResolver.resolve(r) == lambda) valence = Math.max(valence, "xyz".indexOf(r.getText()) + 1);
            }
            params.addAll(List.of("x", "y", "z").subList(0, valence));
        }
        checkFreeNames(name, body, own, uses);

        // Occurrences of each parameter in the body.
        Map<String, List<KNameRef>> occurrences = new HashMap<>();
        for (KNameRef r : PsiTreeUtil.findChildrenOfType(body, KNameRef.class)) {
            PsiElement t = KResolver.resolve(r);
            if (t != null && own.contains(t)) occurrences.computeIfAbsent(r.getText(), k -> new ArrayList<>()).add(r);
        }

        List<Edit> edits = new ArrayList<>();
        for (KNameRef use : uses) {
            PsiElement term = termOf(use);
            KTail tail = term == null ? null : tailOfNameTerm(term);
            if (tail == null) throw new Refusal("'" + name + "' is used other than as a call.");
            List<String> args = new ArrayList<>();
            List<Boolean> argAtomic = new ArrayList<>();
            TextRange replaced;
            boolean callNeedsParens;
            if (tail.getTextLength() > 0) {
                List<KIndex> idx = tail.getIndexList();
                if (idx.size() != 1 || !tail.getText().equals(idx.get(0).getText())) {
                    refuse("'" + name + "' is used other than as a single call " + name + "[...].");
                }
                KBlock argBlock = idx.get(0).getBlock();
                List<KStmt> argStmts = argBlock.getStmts() == null ? List.of() : argBlock.getStmts().getStmtList();
                if (!argBlock.getItemList().isEmpty()) refuse("Calls spanning lines aren't supported.");
                boolean empty = argStmts.stream().allMatch(s -> s.getTextLength() == 0);
                if (!(empty && params.isEmpty())) {
                    if (argStmts.size() != params.size()) {
                        refuse("A call to '" + name + "' has " + argStmts.size() + " arguments; it takes " + params.size() + ".");
                    }
                    for (KStmt s : argStmts) {
                        if (s.getExpr() == null) refuse("A call to '" + name + "' has an empty or assignment argument.");
                        args.add(s.getExpr().getText());
                        argAtomic.add(isAtomic(s.getExpr()));
                    }
                }
                replaced = term.getTextRange();
                callNeedsParens = needsParens(term);
            } else {
                PsiElement next = nextTerm(term);
                if (params.size() != 1 || next == null || !startsValue(next)) {
                    refuse("'" + name + "' is used other than as a call.");
                }
                PsiElement last = next;
                for (PsiElement n = nextTerm(next); n != null; n = nextTerm(n)) last = n;
                TextRange argRange = new TextRange(next.getTextRange().getStartOffset(), last.getTextRange().getEndOffset());
                String arg = argRange.substring(use.getContainingFile().getText());
                args.add(arg);
                argAtomic.add(isAtomicText(project, arg));
                replaced = new TextRange(term.getTextRange().getStartOffset(), argRange.getEndOffset());
                PsiElement prev = prevTerm(term);
                callNeedsParens = prev != null && (startsValue(prev) || type(prev) == KTypes.RPAREN);
            }
            for (int i = 0; i < params.size(); i++) {
                if (!occurrences.containsKey(params.get(i)) && !argAtomic.get(i)) {
                    refuse("The argument for '" + params.get(i) + "' would no longer be evaluated.");
                }
            }
            String text = substitute(body, params, args, argAtomic, occurrences);
            edits.add(new Edit(use.getContainingFile(), replaced,
                    parenthesize(text, isAtomicText(project, text), callNeedsParens)));
        }
        return edits;
    }

    private static String substitute(KExpr body, List<String> params, List<String> args, List<Boolean> atomic,
                                     Map<String, List<KNameRef>> occurrences) {
        int base = body.getTextRange().getStartOffset();
        StringBuilder out = new StringBuilder(body.getText());
        List<Object[]> repl = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            for (KNameRef r : occurrences.getOrDefault(params.get(i), List.of())) {
                PsiElement term = termOf(r);
                boolean needed = term == null || needsParens(term)
                        || r.getTextRange().getEndOffset() != term.getTextRange().getEndOffset();
                repl.add(new Object[]{r.getTextRange().shiftLeft(base), parenthesize(args.get(i), atomic.get(i), needed)});
            }
        }
        repl.sort((a, b) -> ((TextRange) b[0]).getStartOffset() - ((TextRange) a[0]).getStartOffset());
        for (Object[] r : repl) {
            TextRange tr = (TextRange) r[0];
            out.replace(tr.getStartOffset(), tr.getEndOffset(), (String) r[1]);
        }
        return out.toString();
    }

    // ---- applying ------------------------------------------------------------------------

    private static void apply(Project project, KDefine def, List<Edit> edits) {
        SmartPsiElementPointer<KDefine> pointer = SmartPointerManager.createPointer(def);
        Set<PsiFile> files = new LinkedHashSet<>();
        files.add(def.getContainingFile());
        for (Edit e : edits) files.add(e.file());
        PsiDocumentManager docs = PsiDocumentManager.getInstance(project);
        WriteCommandAction.writeCommandAction(project, files.toArray(PsiFile.EMPTY_ARRAY))
                .withName(KResolver.definesLambda(def) ? "Inline Function" : "Inline Variable").run(() -> {
                    List<Edit> sorted = new ArrayList<>(edits);
                    sorted.sort(Comparator.comparing((Edit e) -> e.file().getName())
                            .thenComparing(e -> -e.range().getStartOffset()));
                    for (Edit e : sorted) {
                        Document doc = docs.getDocument(e.file());
                        if (doc == null) continue;
                        doc.replaceString(e.range().getStartOffset(), e.range().getEndOffset(), e.text());
                    }
                    docs.commitAllDocuments();
                    KDefine restored = pointer.getElement();
                    if (restored != null) deleteStatement(restored);
                    for (PsiFile f : files) {
                        Document doc = docs.getDocument(f);
                        if (doc != null) docs.doPostponedOperationsAndUnblockDocument(doc);
                    }
                    docs.commitAllDocuments();
                });
    }

    @Nullable
    @Override
    public String getActionName(PsiElement element) {
        return def(element);
    }
}
