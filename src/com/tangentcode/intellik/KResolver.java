package com.tangentcode.intellik;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.tangentcode.intellik.dialect.KDialect;
import com.tangentcode.intellik.dialect.KDialectService;
import com.tangentcode.intellik.dialect.KDialectSettings;
import com.tangentcode.intellik.dialect.KScoping;
import com.tangentcode.intellik.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Name resolution with K scoping rules.
 *
 * <ul>
 *   <li>Inside a lambda, a name is first looked up among the lambda's parameters. A lambda
 *       without a signature has the implicit parameters {@code x}, {@code y} and {@code z},
 *       which resolve to the lambda itself.</li>
 *   <li>Next come the lambda's locals: names assigned with {@code name:expr} directly in that
 *       lambda (not in a nested one). {@code name::expr} assigns a global instead. Assigning a
 *       name anywhere in a lambda makes it local throughout that lambda.</li>
 *   <li>Then the enclosing lambdas, as the dialect's {@link KScoping} allows:
 *       <ul>
 *         <li>{@link KScoping#ONE_LEVEL} (K3): the parameters and locals of the lambda one level
 *             up. A name that lambda reads from its own parent is a constant in it, so it is
 *             visible one more level down too; otherwise grandparents are out of reach.</li>
 *         <li>{@link KScoping#LEXICAL} (oK): every enclosing lambda, innermost first.</li>
 *         <li>{@link KScoping#LOCAL_OR_GLOBAL} (k4/q, ngn/k): none.</li>
 *       </ul></li>
 *   <li>Finally globals: assignments outside any lambda, plus {@code name::expr} anywhere.
 *       Globals in the same file win; otherwise other {@code .k} files in the project are
 *       searched.</li>
 *   <li>When a name is assigned more than once in a scope, the first assignment is the
 *       declaration and later ones are references to it.</li>
 * </ul>
 */
public final class KResolver {

    private static final Set<String> IMPLICIT_PARAMS = Set.of("x", "y", "z");

    private KResolver() {
    }

    /** The declaration {@code ref} refers to: a {@link KDefine}, a {@link KParam}, a {@link KLambda} (implicit x/y/z), or null. */
    @Nullable
    public static PsiElement resolve(@NotNull KNameRef ref) {
        return CachedValuesManager.getCachedValue(ref, () -> CachedValueProvider.Result.create(
                doResolve(ref), PsiModificationTracker.getInstance(ref.getProject()),
                KDialectSettings.getInstance(ref.getProject())));
    }

    @Nullable
    private static PsiElement doResolve(@NotNull KNameRef ref) {
        return resolveName(ref, ref.getText());
    }

    /** What {@code name} would refer to if written at {@code context}. */
    @Nullable
    public static PsiElement resolveName(@NotNull PsiElement context, @NotNull String name) {
        PsiElement local = resolveInLambdas(enclosingLambda(context), name, scoping(context), null);
        return local != null ? local : resolveGlobal(context, name);
    }

    /** The global {@code name} means in {@code context}'s file: that file's own first, then other K files. */
    @Nullable
    public static KDefine resolveGlobal(@NotNull PsiElement context, @NotNull String name) {
        PsiFile file = context.getContainingFile();
        KDefine global = fileGlobals(file).get(name);
        if (global != null) return global;
        return projectGlobal(file, name);
    }

    /**
     * The parameter or local {@code name} means inside {@code lambda}: its own, or one it sees in
     * an enclosing lambda. Null when the name is global (or unknown). {@code alsoReads}, if not
     * null, is a lambda to treat as reading {@code name} in its own body, for asking what would
     * change if code that reads it were moved there.
     */
    @Nullable
    public static PsiElement resolveInLambdas(@Nullable KLambda lambda, @NotNull String name,
                                              @NotNull KScoping scoping, @Nullable KLambda alsoReads) {
        if (lambda == null) return null;
        PsiElement own = lambdaScope(lambda).get(name);
        if (own != null) return own;
        return visibleInside(enclosingLambda(lambda), name, scoping, alsoReads);
    }

    /**
     * What a lambda written directly in {@code parent} can see as {@code name} from outside
     * itself, apart from globals. Null for top level ({@code parent == null}).
     *
     * <p>K3 ({@link KScoping#ONE_LEVEL}): the parent's parameters and locals, plus anything the
     * parent itself captured and reads, since a captured value is a constant of the parent's
     * (Arthur Whitney: subfunctions are in effect projections of their free names).
     */
    @Nullable
    public static PsiElement visibleInside(@Nullable KLambda parent, @NotNull String name,
                                           @NotNull KScoping scoping, @Nullable KLambda alsoReads) {
        if (parent == null) return null;
        return switch (scoping) {
            case LOCAL_OR_GLOBAL -> null;
            case LEXICAL -> resolveInLambdas(parent, name, scoping, alsoReads);
            case ONE_LEVEL -> {
                PsiElement own = lambdaScope(parent).get(name);
                if (own != null) yield own;
                boolean reads = parent == alsoReads || ownReads(parent).contains(name);
                yield reads ? visibleInside(enclosingLambda(parent), name, scoping, alsoReads) : null;
            }
        };
    }

    /** The scoping rule of the dialect {@code context}'s file is written in. */
    @NotNull
    public static KScoping scoping(@NotNull PsiElement context) {
        PsiFile file = context.getContainingFile();
        if (file == null) return KDialect.DEFAULT.getScoping();
        return KDialectService.getInstance(file.getProject()).dialectFor(file).getScoping();
    }

    /** Names visible at {@code context}, for completion. */
    @NotNull
    public static Collection<String> visibleNames(@NotNull PsiElement context) {
        Set<String> names = new LinkedHashSet<>();
        KScoping scoping = scoping(context);
        KLambda lambda = enclosingLambda(context);
        if (lambda != null) {
            names.addAll(lambdaScope(lambda).keySet());
            for (KLambda p = enclosingLambda(lambda); p != null; p = enclosingLambda(p)) {
                for (String n : lambdaScope(p).keySet()) {
                    if (resolveInLambdas(lambda, n, scoping, null) != null) names.add(n);
                }
            }
        }
        names.addAll(fileGlobals(context.getContainingFile()).keySet());
        return names;
    }

    /** Names a lambda reads in its own body (not in nested lambdas), not counting assignments to them. */
    @NotNull
    public static Set<String> ownReads(@NotNull KLambda lambda) {
        return CachedValuesManager.getCachedValue(lambda, () -> {
            Set<String> names = new HashSet<>();
            for (KNameRef r : PsiTreeUtil.findChildrenOfType(lambda.getBlock(), KNameRef.class)) {
                if (enclosingLambda(r) != lambda) continue;
                if (r.getParent() instanceof KLvalue lv && lv.getParent() instanceof KDefine d
                        && (isDeclaration(d) || isGlobalAssign(d))) continue;
                names.add(r.getText());
            }
            return CachedValueProvider.Result.create(names, PsiModificationTracker.getInstance(lambda.getProject()));
        });
    }

    /** Parameters (or implicit x/y/z) and locals of a lambda, first declaration of each name. */
    @NotNull
    private static Map<String, PsiElement> lambdaScope(@NotNull KLambda lambda) {
        return CachedValuesManager.getCachedValue(lambda, () -> {
            Map<String, PsiElement> scope = new LinkedHashMap<>();
            KSig sig = lambda.getSig();
            if (sig != null) {
                for (KParam p : sig.getParamList()) scope.putIfAbsent(p.getText(), p);
            } else {
                for (String implicit : IMPLICIT_PARAMS) scope.put(implicit, lambda);
            }
            for (KDefine d : PsiTreeUtil.findChildrenOfType(lambda.getBlock(), KDefine.class)) {
                if (isDeclaration(d) && !isGlobalAssign(d) && enclosingLambda(d) == lambda) {
                    scope.putIfAbsent(nameOf(d), d);
                }
            }
            return CachedValueProvider.Result.create(scope, PsiModificationTracker.getInstance(lambda.getProject()));
        });
    }

    /** Global declarations in a file, first declaration of each name, in file order. */
    @NotNull
    public static Map<String, KDefine> fileGlobals(@NotNull PsiFile file) {
        return CachedValuesManager.getCachedValue(file, () -> {
            Map<String, KDefine> globals = new LinkedHashMap<>();
            for (KDefine d : PsiTreeUtil.findChildrenOfType(file, KDefine.class)) {
                if (isDeclaration(d) && (isGlobalAssign(d) || enclosingLambda(d) == null)) {
                    globals.putIfAbsent(nameOf(d), d);
                }
            }
            return CachedValueProvider.Result.create(globals, file);
        });
    }

    /** A global declared in another K file of the project (files in path order). */
    @Nullable
    private static KDefine projectGlobal(@NotNull PsiFile from, @NotNull String name) {
        Project project = from.getProject();
        VirtualFile self = from.getOriginalFile().getVirtualFile();
        if (self == null || DumbService.isDumb(project)) return null;
        List<VirtualFile> files = new ArrayList<>(
                FileTypeIndex.getFiles(KFileType.INSTANCE, GlobalSearchScope.projectScope(project)));
        files.sort(Comparator.comparing(VirtualFile::getPath));
        PsiManager psi = PsiManager.getInstance(project);
        for (VirtualFile vf : files) {
            if (vf.equals(self)) continue;
            PsiFile other = psi.findFile(vf);
            if (other == null) continue;
            KDefine d = fileGlobals(other).get(name);
            if (d != null) return d;
        }
        return null;
    }

    @Nullable
    public static KLambda enclosingLambda(@NotNull PsiElement e) {
        return PsiTreeUtil.getParentOfType(e, KLambda.class, true);
    }

    /** {@code a:1} declares {@code a}; {@code a[i]:1} and {@code a.b:1} assign into it. */
    public static boolean isDeclaration(@NotNull KDefine d) {
        return d.getLvalue().getTail().getTextLength() == 0;
    }

    /** {@code a::1}: a global assignment (inside a lambda) or a K3 dependency (at top level). */
    public static boolean isGlobalAssign(@NotNull KDefine d) {
        PsiElement first = d.getExpr().getFirstChild();
        while (first instanceof PsiWhiteSpace) first = first.getNextSibling();
        return first instanceof KOp && first.getFirstChild() != null
                && first.getFirstChild().getNode().getElementType() == KTypes.COLON;
    }

    /** Does this assignment define a function, as in {@code f:{x+1}}? */
    public static boolean definesLambda(@NotNull KDefine d) {
        PsiElement first = d.getExpr().getFirstChild();
        return first instanceof KValue v && v.getLambda() != null
                && (v.getTail() == null || v.getTail().getTextLength() == 0) && v.getNextSibling() == null;
    }

    @NotNull
    private static String nameOf(@NotNull KDefine d) {
        return d.getLvalue().getNameRef().getText();
    }
}
