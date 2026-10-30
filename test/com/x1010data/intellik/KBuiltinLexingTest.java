package com.x1010data.intellik;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.x1010data.intellik.dialect.KDialect;
import com.x1010data.intellik.dialect.PrimitiveTable;
import com.x1010data.intellik.psi.KTypes;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** Builtins are recognised through the dialect's primitive table, the same way K.flex used to. */
public class KBuiltinLexingTest {

    private static List<String> lex(PrimitiveTable table, String text) {
        KLexerAdapter lexer = new KLexerAdapter(table);
        lexer.start(text);
        List<String> out = new ArrayList<>();
        for (IElementType t; (t = lexer.getTokenType()) != null; lexer.advance()) {
            if (t == TokenType.WHITE_SPACE) continue;
            out.add(t + ":" + text.substring(lexer.getTokenStart(), lexer.getTokenEnd()));
        }
        return out;
    }

    private static List<String> lex(String text) {
        return lex(PrimitiveTable.forDialect(KDialect.K3), text);
    }

    private static String tok(IElementType t, String text) {
        return t + ":" + text;
    }

    @Test
    public void knownBuiltinsLexAsBuiltin() {
        assertEquals(List.of(tok(KTypes.BUILTIN, "_bin")), lex("_bin"));
        assertEquals(List.of(tok(KTypes.BUILTIN, "_sqrt"), tok(KTypes.IDENT, "x")), lex("_sqrt x"));
        assertEquals(List.of(tok(KTypes.BUILTIN, "_inv")), lex("_inv"));
    }

    @Test
    public void longestBuiltinWinsAndTheRestIsAnIdentifier() {
        // Same results the old hardcoded regex alternation gave.
        assertEquals(List.of(tok(KTypes.BUILTIN, "_dvl"), tok(KTypes.IDENT, "x")), lex("_dvlx"));
        assertEquals(List.of(tok(KTypes.BUILTIN, "_in"), tok(KTypes.IDENT, "a")), lex("_ina"));
    }

    @Test
    public void unknownUnderscoreNameIsDropPrimitiveThenIdentifier() {
        assertEquals(List.of(tok(KTypes.PRIM, "_"), tok(KTypes.IDENT, "foo")), lex("_foo"));
        assertEquals(List.of(tok(KTypes.INT, "2"), tok(KTypes.PRIM, "_"), tok(KTypes.IDENT, "x")), lex("2_x"));
        assertEquals(List.of(tok(KTypes.PRIM, "_:"), tok(KTypes.IDENT, "x")), lex("_:x"));
        assertEquals(List.of(tok(KTypes.IDENT, "x_y")), lex("x_y"));
    }

    @Test
    public void dialectWithoutTableHasNoBuiltins() {
        assertEquals(List.of(tok(KTypes.PRIM, "_"), tok(KTypes.IDENT, "bin")), lex(PrimitiveTable.EMPTY, "_bin"));
    }

    @Test
    public void builtinsStillGetBuiltinHighlighting() {
        KSyntaxHighlighter h = new KSyntaxHighlighter();
        assertArrayEquals(new TextAttributesKey[]{KSyntaxHighlighter.BUILTIN}, h.getTokenHighlights(KTypes.BUILTIN));
        KLexerAdapter lexer = (KLexerAdapter) h.getHighlightingLexer();
        lexer.start("_sin x");
        assertSame(KTypes.BUILTIN, lexer.getTokenType());
        assertArrayEquals(new TextAttributesKey[]{KSyntaxHighlighter.BUILTIN},
                h.getTokenHighlights(lexer.getTokenType()));
    }
}
