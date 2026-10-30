package com.tangentcode.intellik.dialect;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The primitives, adverbs, built-in names and keywords of one K dialect,
 * loaded from {@code /dialects/<id>.json}. A dialect without a table gets
 * {@link #EMPTY}.
 */
public final class PrimitiveTable {

    /** One table entry: the text as written in source plus an optional short description. */
    public static final class Entry {
        private final String name;
        private final String description;

        public Entry(@NotNull String name, @Nullable String description) {
            this.name = name;
            this.description = description == null ? "" : description;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final PrimitiveTable EMPTY = new PrimitiveTable(
            null, List.of(), List.of(), List.of(), List.of());

    private static final Map<KDialect, PrimitiveTable> CACHE = new ConcurrentHashMap<>();

    private final KDialect dialect;
    private final List<Entry> primitives;
    private final List<Entry> adverbs;
    private final List<Entry> builtins;
    private final List<Entry> keywords;
    private final Set<String> builtinNames;
    private final int longestBuiltin;

    private PrimitiveTable(@Nullable KDialect dialect, List<Entry> primitives, List<Entry> adverbs,
                           List<Entry> builtins, List<Entry> keywords) {
        this.dialect = dialect;
        this.primitives = List.copyOf(primitives);
        this.adverbs = List.copyOf(adverbs);
        this.builtins = List.copyOf(builtins);
        this.keywords = List.copyOf(keywords);
        Set<String> names = new HashSet<>();
        int longest = 0;
        for (Entry e : builtins) {
            names.add(e.getName());
            longest = Math.max(longest, e.getName().length());
        }
        this.builtinNames = Collections.unmodifiableSet(names);
        this.longestBuiltin = longest;
    }

    /** The table for a dialect, or {@link #EMPTY} if that dialect has no table yet. */
    @NotNull
    public static PrimitiveTable forDialect(@NotNull KDialect dialect) {
        return CACHE.computeIfAbsent(dialect, PrimitiveTable::load);
    }

    @NotNull
    private static PrimitiveTable load(@NotNull KDialect dialect) {
        String path = "/dialects/" + dialect.getId() + ".json";
        try (InputStream in = PrimitiveTable.class.getResourceAsStream(path)) {
            if (in == null) return EMPTY;
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parse(dialect, r);
            }
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Cannot load K primitive table " + path, e);
        }
    }

    /** Parses a table in the {@code resources/dialects/*.json} format. */
    @NotNull
    public static PrimitiveTable parse(@Nullable KDialect dialect, @NotNull Reader json) {
        Json data = new Gson().fromJson(json, Json.class);
        if (data == null) throw new JsonParseException("empty primitive table");
        return new PrimitiveTable(dialect, entries(data.primitives), entries(data.adverbs),
                entries(data.builtins), entries(data.keywords));
    }

    private static List<Entry> entries(@Nullable List<JsonEntry> raw) {
        if (raw == null) return List.of();
        List<Entry> out = new ArrayList<>(raw.size());
        for (JsonEntry e : raw) {
            if (e == null || e.name == null || e.name.isEmpty()) {
                throw new JsonParseException("primitive table entry without a name");
            }
            out.add(new Entry(e.name, e.description));
        }
        return out;
    }

    /** Dialect this table was loaded for; null for {@link #EMPTY} or tables parsed without one. */
    @Nullable
    public KDialect getDialect() {
        return dialect;
    }

    @NotNull
    public List<Entry> getPrimitives() {
        return primitives;
    }

    @NotNull
    public List<Entry> getAdverbs() {
        return adverbs;
    }

    @NotNull
    public List<Entry> getBuiltins() {
        return builtins;
    }

    @NotNull
    public List<Entry> getKeywords() {
        return keywords;
    }

    public boolean isBuiltin(@NotNull CharSequence name) {
        return builtinNames.contains(name.toString());
    }

    /**
     * Length of the longest built-in name that {@code text} starts with, or 0 if none.
     * The lexer uses this so that {@code _dvlx} lexes as {@code _dvl} followed by {@code x}.
     */
    public int longestBuiltinPrefix(@NotNull CharSequence text) {
        for (int n = Math.min(text.length(), longestBuiltin); n > 0; n--) {
            if (builtinNames.contains(text.subSequence(0, n).toString())) return n;
        }
        return 0;
    }

    // Gson binding for the JSON file format.
    private static final class Json {
        List<JsonEntry> primitives;
        List<JsonEntry> adverbs;
        List<JsonEntry> builtins;
        List<JsonEntry> keywords;
    }

    private static final class JsonEntry {
        String name;
        String description;
    }
}
