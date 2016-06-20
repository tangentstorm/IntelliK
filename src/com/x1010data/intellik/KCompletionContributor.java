package com.x1010data.intellik;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.x1010data.intellik.psi.KTypes;
import org.jetbrains.annotations.NotNull;

public class KCompletionContributor extends CompletionContributor {
    public KCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(KTypes.IDENT).withLanguage(KLanguage.INSTANCE),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters params,
                                                  ProcessingContext ctx,
                                                  @NotNull CompletionResultSet res) {
                        res.addElement(LookupElementBuilder.create("youCompleteMe"));
                    }
                });

    }
}
