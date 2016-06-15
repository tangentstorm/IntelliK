package com.x1010data.intellik;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.x1010data.intellik.psi.KTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Created by michal.wallace on 6/15/16.
 */
public class KSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey COMMENT =
            TextAttributesKey.createTextAttributesKey("K_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey NUMBER =
            TextAttributesKey.createTextAttributesKey("K_NUMBER", DefaultLanguageHighlighterColors.NUMBER);

    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{NUMBER};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];


    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new KLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType t) {
        if (t.equals(KTypes.COMMENT)) return COMMENT_KEYS;
        else if (t.equals(KTypes.INT)) return NUMBER_KEYS;
        else return EMPTY_KEYS;
    }
}
