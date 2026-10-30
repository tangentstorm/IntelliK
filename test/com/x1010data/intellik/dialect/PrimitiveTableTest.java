package com.x1010data.intellik.dialect;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class PrimitiveTableTest {

    /** The builtins K.flex hardcoded before they moved into dialects/k3.json. */
    private static final List<String> K3_BUILTINS = List.of(
            "_bin", "_di", "_dv", "_dvl", "_draw", "_gtime", "_ic", "_ci", "_jd", "_dj", "_lsq", "_dot",
            "_mul", "_inv", "_in", "_lin", "_sv", "_sm", "_ss", "_ssr", "_vs", "_abs", "_floor", "_sin",
            "_cos", "_tan", "_sinh", "_cosh", "_tanh", "_exp", "_log", "_sqr", "_sqrt");

    private static List<String> names(List<PrimitiveTable.Entry> entries) {
        return entries.stream().map(PrimitiveTable.Entry::getName).collect(Collectors.toList());
    }

    @Test
    public void k3TableLoadsTheFormerlyHardcodedBuiltins() {
        PrimitiveTable k3 = PrimitiveTable.forDialect(KDialect.K3);
        assertSame(KDialect.K3, k3.getDialect());
        assertEquals(K3_BUILTINS, names(k3.getBuiltins()));
        for (String b : K3_BUILTINS) assertTrue(b, k3.isBuiltin(b));
        assertFalse(k3.isBuiltin("_foo"));
    }

    @Test
    public void k3TableHasGlyphsAdverbsAndKeywords() {
        PrimitiveTable k3 = PrimitiveTable.forDialect(KDialect.K3);
        assertTrue(names(k3.getPrimitives()).containsAll(List.of("+", "+:", "!", "_", "0:", "-", ".")));
        assertEquals(List.of("'", "/", "\\", "':", "/:", "\\:"), names(k3.getAdverbs()));
        assertEquals(List.of("if", "do", "while"), names(k3.getKeywords()));
        for (PrimitiveTable.Entry e : k3.getBuiltins()) assertNotNull(e.getDescription());
    }

    @Test
    public void tablesAreCached() {
        assertSame(PrimitiveTable.forDialect(KDialect.K3), PrimitiveTable.forDialect(KDialect.K3));
    }

    @Test
    public void dialectWithoutTableIsEmpty() {
        PrimitiveTable ngn = PrimitiveTable.forDialect(KDialect.NGN);
        assertSame(PrimitiveTable.EMPTY, ngn);
        assertTrue(ngn.getBuiltins().isEmpty());
        assertEquals(0, ngn.longestBuiltinPrefix("_bin"));
    }

    @Test
    public void longestBuiltinPrefixPicksTheLongestMatch() {
        PrimitiveTable k3 = PrimitiveTable.forDialect(KDialect.K3);
        assertEquals(4, k3.longestBuiltinPrefix("_dvl"));
        assertEquals(4, k3.longestBuiltinPrefix("_dvlx"));
        assertEquals(3, k3.longestBuiltinPrefix("_dvx"));
        assertEquals(4, k3.longestBuiltinPrefix("_inv"));
        assertEquals(3, k3.longestBuiltinPrefix("_ina"));
        assertEquals(0, k3.longestBuiltinPrefix("_foo"));
        assertEquals(0, k3.longestBuiltinPrefix(""));
    }

    @Test
    public void parsesDescriptionsAndToleratesMissingSections() {
        PrimitiveTable t = PrimitiveTable.parse(null, new StringReader(
                "{\"builtins\": [{\"name\": \"_x\", \"description\": \"does x\"}, {\"name\": \"_y\"}]}"));
        assertEquals("does x", t.getBuiltins().get(0).getDescription());
        assertEquals("", t.getBuiltins().get(1).getDescription());
        assertTrue(t.getPrimitives().isEmpty());
        assertTrue(t.getKeywords().isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void rejectsEntriesWithoutNames() {
        PrimitiveTable.parse(null, new StringReader("{\"builtins\": [{\"description\": \"nameless\"}]}"));
    }

    @Test
    public void dialectIdsRoundTrip() {
        for (KDialect d : KDialect.values()) assertSame(d, KDialect.fromId(d.getId()));
        assertSame(KDialect.K3, KDialect.fromId("nonsense"));
        assertSame(KDialect.K3, KDialect.fromId(null));
    }
}
