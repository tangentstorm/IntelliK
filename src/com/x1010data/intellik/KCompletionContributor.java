package com.x1010data.intellik;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.ProcessingContext;
import com.x1010data.intellik.dialect.KDialectService;
import com.x1010data.intellik.dialect.PrimitiveTable;
import com.x1010data.intellik.psi.KTypes;
import org.jetbrains.annotations.NotNull;

/** Completes the active dialect's built-in names and keywords from its {@link PrimitiveTable}. */
public class KCompletionContributor extends CompletionContributor {

    private static final TokenSet NO_COMPLETION =
            TokenSet.create(KTypes.COMMENT, KTypes.COMMAND, KTypes.STRING, KTypes.SYM);

    public KCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(KLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters params,
                                                  @NotNull ProcessingContext ctx,
                                                  @NotNull CompletionResultSet res) {
                        IElementType type = params.getPosition().getNode().getElementType();
                        if (NO_COMPLETION.contains(type)) return;

                        PrimitiveTable table = KDialectService.getInstance(params.getPosition().getProject())
                                .tableFor(params.getOriginalFile());
                        String prefix = namePrefix(params.getEditor().getDocument().getCharsSequence(),
                                params.getOffset());
                        CompletionResultSet out = res.withPrefixMatcher(prefix);
                        for (PrimitiveTable.Entry e : table.getBuiltins()) {
                            out.addElement(lookup(e, "builtin"));
                        }
                        for (PrimitiveTable.Entry e : table.getKeywords()) {
                            out.addElement(lookup(e, "keyword").bold());
                        }
                    }
                });
    }

    private static LookupElementBuilder lookup(PrimitiveTable.Entry e, String kind) {
        LookupElementBuilder b = LookupElementBuilder.create(e.getName()).withTypeText(kind);
        return e.getDescription().isEmpty() ? b : b.withTailText(" " + e.getDescription(), true);
    }

    /**
     * The name being typed before {@code offset}: letters, digits and underscores, but not starting
     * with a digit. The platform's default prefix would drop a leading "_", since the lexer splits
     * an unknown "_name" into the "_" primitive and an identifier.
     */
    static String namePrefix(CharSequence text, int offset) {
        int start = offset;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (!(Character.isLetterOrDigit(c) || c == '_')) break;
            start--;
        }
        while (start < offset && Character.isDigit(text.charAt(start))) start++;
        return text.subSequence(start, offset).toString();
    }
}
