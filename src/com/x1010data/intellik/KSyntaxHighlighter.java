package com.x1010data.intellik;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.sun.jna.platform.unix.X11;
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
    public static final TextAttributesKey STRING =
            TextAttributesKey.createTextAttributesKey("K_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey IDENT =
            TextAttributesKey.createTextAttributesKey("K_IDENT", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey SYMBOL =
            TextAttributesKey.createTextAttributesKey("K_SYMBOL", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey PRIM =
            TextAttributesKey.createTextAttributesKey("K_PRIM", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey BRACE =
            TextAttributesKey.createTextAttributesKey("K_BRACE", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey BRACK =
            TextAttributesKey.createTextAttributesKey("K_BRACK", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey PAREN =
            TextAttributesKey.createTextAttributesKey("K_PAREN", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey BUILTIN =
            TextAttributesKey.createTextAttributesKey("K_BUILTIN", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    public static final TextAttributesKey KEYWORD =
            TextAttributesKey.createTextAttributesKey("K_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey ADVERB =
            TextAttributesKey.createTextAttributesKey("K_ADVERB", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey COMMAND =
            TextAttributesKey.createTextAttributesKey("K_COMMAND", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey SEMI =
            TextAttributesKey.createTextAttributesKey("K_SEMI", DefaultLanguageHighlighterColors.SEMICOLON);
    public static final TextAttributesKey DOT =
            TextAttributesKey.createTextAttributesKey("K_DOT", DefaultLanguageHighlighterColors.DOT);

    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{NUMBER};
    private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
    private static final TextAttributesKey[] IDENT_KEYS = new TextAttributesKey[]{IDENT};
    private static final TextAttributesKey[] SYMBOL_KEYS = new TextAttributesKey[]{SYMBOL};
    private static final TextAttributesKey[] PRIM_KEYS = new TextAttributesKey[]{PRIM};
    private static final TextAttributesKey[] BRACE_KEYS = new TextAttributesKey[]{BRACE};
    private static final TextAttributesKey[] BRACK_KEYS = new TextAttributesKey[]{BRACK};
    private static final TextAttributesKey[] PAREN_KEYS = new TextAttributesKey[]{PAREN};
    private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[]{KEYWORD};
    private static final TextAttributesKey[] BUILTIN_KEYS = new TextAttributesKey[]{BUILTIN};
    private static final TextAttributesKey[] ADVERB_KEYS = new TextAttributesKey[]{ADVERB};
    private static final TextAttributesKey[] COMMAND_KEYS = new TextAttributesKey[]{COMMAND};
    private static final TextAttributesKey[] SEMI_KEYS = new TextAttributesKey[]{SEMI};
    private static final TextAttributesKey[] DOT_KEYS = new TextAttributesKey[]{DOT};
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
        else if (t.equals(KTypes.INT) || t.equals(KTypes.RAT)) return NUMBER_KEYS;
        else if (t.equals(KTypes.STRING)) return STRING_KEYS;
        else if (t.equals(KTypes.SYM)) return SYMBOL_KEYS;
        else if (t.equals(KTypes.IDENT)) return IDENT_KEYS;
        else if (t.equals(KTypes.PRIM)) return PRIM_KEYS;
        else if (t.equals(KTypes.LBRACE) || t.equals(KTypes.RBRACE)) return BRACE_KEYS;
        else if (t.equals(KTypes.LBRACK) || t.equals(KTypes.RBRACK)) return BRACK_KEYS;
        else if (t.equals(KTypes.LPAREN) || t.equals(KTypes.RPAREN)) return PAREN_KEYS;
        else if (t.equals(KTypes.BUILTIN)) return BUILTIN_KEYS;
        else if (t.equals(KTypes.ADV)) return ADVERB_KEYS;
        else if (t.equals(KTypes.SEMI)) return SEMI_KEYS;
        else if (t.equals(KTypes.DOT)) return DOT_KEYS;
        else if (t.equals(KTypes.COMMAND)) return COMMAND_KEYS;
        else if (t.equals(KTypes.DO) || t.equals(KTypes.IF) || t.equals(KTypes.WHILE)) return KEYWORD_KEYS;
        else return EMPTY_KEYS;
    }
}
