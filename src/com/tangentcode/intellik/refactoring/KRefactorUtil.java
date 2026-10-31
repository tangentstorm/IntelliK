package com.tangentcode.intellik.refactoring;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.tangentcode.intellik.KLanguage;
import com.tangentcode.intellik.KResolver;
import com.tangentcode.intellik.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for K refactorings.
 *
 * <p>K evaluates right to left with no operator precedence: {@code a-b+c} is {@code a-(b+c)}.
 * So the complete subexpressions of an expression are its right-hand suffixes that start with a
 * value ({@code b+c}, {@code c}) and any single term ({@code b}, {@code f[x]}, {@code (a-b)}),
 * but not a left prefix like {@code a-b}.
 */
public final class KRefactorUtil {

    private KRefactorUtil() {
    }

    /** A thrown refusal; handlers turn it into an error hint. */
    public static final class Refusal extends RuntimeException {
        public Refusal(String message) {
            super(message);
        }
    }

    public static void refuse(String message) {
        throw new Refusal(message);
    }

    static void showError(Project project, Editor editor, String title, String message) {
        CommonRefactoringUtil.showErrorHint(project, editor, message, title, null);
    }

    // ---- expression structure ------------------------------------------------------------

    static IElementType type(PsiElement e) {
        return e.getNode().getElementType();
    }

    /** Children of an expression, minus whitespace and comments. */
    @NotNull
    static List<PsiElement> terms(@NotNull KExpr expr) {
        List<PsiElement> out = new ArrayList<>();
        for (PsiElement c = expr.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof PsiWhiteSpace || c instanceof PsiComment || c.getTextLength() == 0) continue;
            out.add(c);
        }
        return out;
    }

    /** Can this expression child start a complete subexpression? */
    static boolean startsValue(PsiElement term) {
        return term instanceof KValue || term instanceof KAmend || term instanceof KCond
                || type(term) == KTypes.LPAREN || nameOfTerm(term) != null;
    }

    /**
     * The name a term consists of, if any. The grammar parses a name after an operator as part of
     * an {@code op} run ({@code 2*a+b} is value, op, op(a), op, value), so names show up both as
     * {@link KValue}s and as {@link KOp}s.
     */
    @Nullable
    static KNameRef nameOfTerm(PsiElement term) {
        if (term instanceof KValue v && v.getLvalue() != null) return v.getLvalue().getNameRef();
        if (term instanceof KOp op) return op.getNameRef();
        return null;
    }

    /** The tail (indexing, adverbs) after a name term. */
    @Nullable
    static KTail tailOfNameTerm(PsiElement term) {
        if (term instanceof KValue v && v.getLvalue() != null) return v.getLvalue().getTail();
        if (term instanceof KOp op && op.getNameRef() != null) return op.getTail();
        return null;
    }

    @Nullable
    static PsiElement nextTerm(PsiElement term) {
        PsiElement n = PsiTreeUtil.skipWhitespacesAndCommentsForward(term);
        return n != null && n.getParent() == term.getParent() ? n : null;
    }

    @Nullable
    static PsiElement prevTerm(PsiElement term) {
        PsiElement p = PsiTreeUtil.skipWhitespacesAndCommentsBackward(term);
        return p != null && p.getParent() == term.getParent() ? p : null;
    }

    /**
     * Would text substituted for {@code term} (a child of an expression) need parentheses to keep
     * its meaning? Yes if anything follows it in the expression (then it's a left operand or
     * applied to something), or if a value precedes it directly (juxtaposition, e.g. a number
     * strand {@code 1 2 t}).
     */
    static boolean needsParens(@NotNull PsiElement term) {
        if (nextTerm(term) != null) return true;
        PsiElement prev = prevTerm(term);
        return prev != null && (startsValue(prev) || type(prev) == KTypes.RPAREN);
    }

    /** Is this tail only indexing / attribute access (no adverbs)? */
    static boolean simpleTail(@Nullable KTail tail) {
        if (tail == null) return true;
        for (PsiElement c = tail.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof PsiWhiteSpace) continue;
            if (!(c instanceof KIndex || c instanceof KAttr)) return false;
        }
        return true;
    }

    /** A single term that never needs parentheses: a name, a single literal, a lambda, a parenthesized group. */
    static boolean isAtomic(@NotNull KExpr expr) {
        List<PsiElement> ts = terms(expr);
        if (ts.size() == 3 && type(ts.get(0)) == KTypes.LPAREN && type(ts.get(2)) == KTypes.RPAREN) return true;
        if (ts.size() == 1 && ts.get(0) instanceof KOp op && op.getNameRef() != null) return simpleTail(op.getTail());
        if (ts.size() != 1 || !(ts.get(0) instanceof KValue v)) return false;
        if (v.getLvalue() != null) return simpleTail(v.getLvalue().getTail());
        if (!simpleTail(v.getTail())) return false;
        if (v.getLambda() != null) return true;
        KNoun noun = v.getNoun();
        if (noun == null) return false;
        if (noun.getList() != null || noun.getDict() != null) return true;
        List<PsiElement> parts = new ArrayList<>();
        for (PsiElement c = noun.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (!(c instanceof PsiWhiteSpace)) parts.add(c);
        }
        if (parts.size() != 1) return false;
        PsiElement only = parts.get(0);
        if (only instanceof KNum num) return num.getFirstChild() != null && type(num.getFirstChild()) != KTypes.MINUS;
        return true; // a single STRING or SYM
    }

    /** Parses {@code text} as a standalone expression and checks {@link #isAtomic(KExpr)}. */
    static boolean isAtomicText(@NotNull Project project, @NotNull String text) {
        PsiFile f = PsiFileFactory.getInstance(project).createFileFromText("dummy.k", KLanguage.INSTANCE, text + "\n");
        if (PsiTreeUtil.hasErrorElements(f)) return false;
        KExpr e = PsiTreeUtil.findChildOfType(f, KExpr.class);
        return e != null && e.getText().equals(text) && isAtomic(e);
    }

    /** Wraps non-atomic text in parens where needed; a leading minus always gets them ({@code 2-(-1)}). */
    static String parenthesize(String text, boolean atomic, boolean needed) {
        return !atomic && (needed || text.startsWith("-")) ? "(" + text + ")" : text;
    }

    // ---- names ---------------------------------------------------------------------------

    /** The expression child holding a name used as a value, or null for an assignment target. */
    @Nullable
    static PsiElement termOf(@NotNull KNameRef ref) {
        PsiElement p = ref.getParent();
        if (p instanceof KOp && p.getParent() instanceof KExpr) return p;
        if (p instanceof KLvalue && p.getParent() instanceof KValue v && v.getParent() instanceof KExpr) return v;
        return null;
    }

    @Nullable
    static KDefine assignmentOf(@NotNull KNameRef ref) {
        return ref.getParent() instanceof KLvalue lv && lv.getParent() instanceof KDefine d ? d : null;
    }

    /** {@code a:..}, {@code a[i]:..}, an assignment inside an expression, or a modified assignment like {@code a+:1}. */
    static boolean isWrite(@NotNull KNameRef ref) {
        if (assignmentOf(ref) != null) return true;
        PsiElement term = termOf(ref);
        PsiElement next = term == null ? null : nextTerm(term);
        if (next instanceof KOp op && op.getFirstChild() != null) {
            if (type(op.getFirstChild()) == KTypes.COLON) return true; // assignment inside an expression
            if (type(op.getFirstChild()) == KTypes.PRIM) {
                String prim = op.getFirstChild().getText();
                return prim.length() == 2 && prim.endsWith(":");
            }
        }
        return false;
    }

    @NotNull
    static List<KNameRef> nameRefsIn(@NotNull PsiElement root, @NotNull TextRange range) {
        List<KNameRef> out = new ArrayList<>();
        if (root instanceof KNameRef r && range.contains(r.getTextRange())) out.add(r);
        for (KNameRef r : PsiTreeUtil.findChildrenOfType(root, KNameRef.class)) {
            if (range.contains(r.getTextRange())) out.add(r);
        }
        return out;
    }

    static void checkNewName(@NotNull PsiElement context, @NotNull String name) {
        if (KResolver.resolveName(context, name) != null) {
            refuse("'" + name + "' is already defined here.");
        }
    }

    // ---- selections ----------------------------------------------------------------------

    /** A complete subexpression: terms {@code first..last} of {@code expr}. */
    record ExprSelection(KExpr expr, PsiElement first, PsiElement last, TextRange range) {
    }

    @NotNull
    static TextRange trimmed(@NotNull Document doc, int start, int end) {
        CharSequence t = doc.getCharsSequence();
        while (start < end && Character.isWhitespace(t.charAt(start))) start++;
        while (end > start && Character.isWhitespace(t.charAt(end - 1))) end--;
        return new TextRange(start, end);
    }

    @Nullable
    static ExprSelection findExpression(@NotNull PsiFile file, @NotNull TextRange range) {
        if (range.isEmpty()) return null;
        PsiElement a = file.findElementAt(range.getStartOffset());
        PsiElement b = file.findElementAt(range.getEndOffset() - 1);
        if (a == null || b == null) return null;
        PsiElement common = PsiTreeUtil.findCommonParent(a, b);
        KExpr expr = PsiTreeUtil.getParentOfType(common, KExpr.class, false);
        if (expr == null) return null;
        List<PsiElement> ts = terms(expr);
        int i = -1, j = -1;
        for (int k = 0; k < ts.size(); k++) {
            if (ts.get(k).getTextRange().getStartOffset() == range.getStartOffset()) i = k;
            if (ts.get(k).getTextRange().getEndOffset() == range.getEndOffset()) j = k;
        }
        if (i < 0 || j < i) return null;
        PsiElement first = ts.get(i), last = ts.get(j);
        boolean suffix = j == ts.size() - 1 && startsValue(first);
        boolean single = (i == j && startsValue(first) && type(first) != KTypes.LPAREN)
                || (type(first) == KTypes.LPAREN && j == i + 2 && type(last) == KTypes.RPAREN);
        boolean whole = i == 0 && j == ts.size() - 1;
        if (!(suffix || single || whole)) return null;
        return new ExprSelection(expr, first, last, range);
    }

    /**
     * The scope a statement belongs to (the file, or a lambda's block) if it is a statement of
     * that scope directly rather than nested in an {@code if[...]}, list, index etc.
     */
    @Nullable
    static PsiElement statementScope(@NotNull KStmt stmt) {
        PsiElement p = stmt.getParent();
        if (!(p instanceof KStmts)) return null;
        p = p.getParent();
        if (p instanceof KLine) {
            p = p.getParent();
            if (!(p instanceof KItem)) return null;
            p = p.getParent();
        }
        if (p instanceof KFile) return p;
        if (p instanceof KBlock && p.getParent() instanceof KLambda) return p;
        return null;
    }

    /** The nearest enclosing statement that belongs directly to a file or lambda scope. */
    @Nullable
    static KStmt anchorStatement(@NotNull PsiElement e) {
        for (KStmt s = PsiTreeUtil.getParentOfType(e, KStmt.class, false); s != null;
             s = PsiTreeUtil.getParentOfType(s, KStmt.class, true)) {
            if (statementScope(s) != null) return s;
        }
        return null;
    }

    /** Whole statements {@code first..last} of the same scope. */
    record StmtSelection(KStmt first, KStmt last, PsiElement scope, TextRange range) {
    }

    @Nullable
    static StmtSelection findStatements(@NotNull PsiFile file, @NotNull TextRange range) {
        if (range.isEmpty()) return null;
        KStmt first = null, last = null;
        for (KStmt s = PsiTreeUtil.getParentOfType(file.findElementAt(range.getStartOffset()), KStmt.class, false);
             s != null; s = PsiTreeUtil.getParentOfType(s, KStmt.class, true)) {
            if (s.getTextRange().getStartOffset() == range.getStartOffset() && statementScope(s) != null) first = s;
        }
        for (KStmt s = PsiTreeUtil.getParentOfType(file.findElementAt(range.getEndOffset() - 1), KStmt.class, false);
             s != null; s = PsiTreeUtil.getParentOfType(s, KStmt.class, true)) {
            if (s.getTextRange().getEndOffset() == range.getEndOffset() && statementScope(s) != null) last = s;
        }
        if (first == null || last == null) return null;
        PsiElement scope = statementScope(first);
        if (scope != statementScope(last)) return null;
        return new StmtSelection(first, last, scope, range);
    }

    /** Document offset of the start of the line holding the top-level item that contains {@code e}. */
    static int topLevelLineStart(@NotNull Document doc, @NotNull PsiElement e) {
        PsiElement item = e;
        while (item.getParent() != null && !(item.getParent() instanceof PsiFile)) item = item.getParent();
        return doc.getLineStartOffset(doc.getLineNumber(item.getTextRange().getStartOffset()));
    }

    // ---- deleting a definition -----------------------------------------------------------

    /** Removes the statement holding an assignment, with its separator or its whole line. */
    public static void deleteStatement(@NotNull KDefine def) {
        PsiElement stmt = def.getParent();
        if (!(stmt instanceof KStmt) || !(stmt.getParent() instanceof KStmts stmts)) {
            def.getNode().getTreeParent().removeChild(def.getNode());
            return;
        }
        boolean alone = true;
        for (KStmt s : stmts.getStmtList()) {
            if (s != stmt && s.getTextLength() > 0) alone = false;
        }
        if (alone && stmts.getParent() instanceof KLine line
                && PsiTreeUtil.getChildOfType(line, PsiComment.class) == null
                && line.getNode().findChildByType(KTypes.COMMENT) == null
                && line.getParent() instanceof KItem item) {
            PsiElement indent = item.getPrevSibling();
            ASTNode parent = item.getNode().getTreeParent();
            if (indent instanceof PsiWhiteSpace) parent.removeRange(indent.getNode(), item.getNode().getTreeNext());
            else parent.removeChild(item.getNode());
            return;
        }
        ASTNode node = stmt.getNode();
        ASTNode container = stmts.getNode();
        // Prefer removing "stmt; " (up to the next statement); for the last statement remove "; stmt".
        ASTNode after = node.getTreeNext();
        while (after != null && after.getElementType() == TokenType.WHITE_SPACE) after = after.getTreeNext();
        if (after != null && after.getElementType() == KTypes.SEMI) {
            ASTNode end = after.getTreeNext();
            while (end != null && end.getElementType() == TokenType.WHITE_SPACE) end = end.getTreeNext();
            container.removeRange(node, end);
            return;
        }
        ASTNode before = node.getTreePrev();
        while (before != null && before.getElementType() == TokenType.WHITE_SPACE) before = before.getTreePrev();
        if (before != null && before.getElementType() == KTypes.SEMI) {
            ASTNode start = before;
            ASTNode p = before.getTreePrev();
            while (p != null && p.getElementType() == TokenType.WHITE_SPACE) {
                start = p;
                p = p.getTreePrev();
            }
            container.removeRange(start, node.getTreeNext());
            return;
        }
        container.removeChild(node);
    }
}
