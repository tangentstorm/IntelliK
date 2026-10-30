package com.x1010data.intellik;

import com.intellij.lexer.FlexAdapter;
import com.x1010data.intellik.dialect.KDialect;
import com.x1010data.intellik.dialect.PrimitiveTable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by michal.wallace on 6/14/16.
 */
public class KLexerAdapter extends FlexAdapter {
    public KLexerAdapter() {
        this(PrimitiveTable.forDialect(KDialect.DEFAULT));
    }

    public KLexerAdapter(@NotNull PrimitiveTable table) {
        super(new KLexer(table));
    }
}
