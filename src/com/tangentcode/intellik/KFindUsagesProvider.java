package com.tangentcode.intellik;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.tree.TokenSet;
import com.tangentcode.intellik.psi.KDefine;
import com.tangentcode.intellik.psi.KNamedElement;
import com.tangentcode.intellik.psi.KParam;
import com.tangentcode.intellik.psi.KTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KFindUsagesProvider implements FindUsagesProvider {

    @Nullable
    @Override
    public WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(new KLexerAdapter(), TokenSet.create(KTypes.IDENT),
                KParserDefinition.COMMENTS, KParserDefinition.STRINGS);
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement element) {
        return element instanceof KNamedElement named && named.getNameIdentifier() != null;
    }

    @Nullable
    @Override
    public String getHelpId(@NotNull PsiElement element) {
        return null;
    }

    @NotNull
    @Override
    public String getType(@NotNull PsiElement element) {
        if (element instanceof KParam) return "parameter";
        if (element instanceof KDefine d) return KResolver.definesLambda(d) ? "function" : "variable";
        return "";
    }

    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement element) {
        String name = element instanceof PsiNamedElement n ? n.getName() : null;
        return name == null ? "" : name;
    }

    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        return getDescriptiveName(element);
    }
}
