package com.fongmi.quickjs.utils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.Selector;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class HtmlQuery {

    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private static final int MAX_INPUT = 2 * 1024 * 1024;
    private static final int MAX_PLAN = 16 * 1024;
    private static final int MAX_QUERIES = 16;
    private static final int MAX_ROWS = 1024;
    private static final int MAX_FIELDS = 32;
    private static final int MAX_OUTPUT = 1024 * 1024;
    private static final Set<String> PLAN_KEYS = Set.of("queries");
    private static final Set<String> QUERY_KEYS = Set.of("selector", "fields", "maxRows");
    private static final Set<String> FIELD_KEYS = Set.of("selector", "attribute", "all");

    public static String query(String source, String json) throws IOException {
        if (source == null || source.length() > MAX_INPUT) throw new IllegalArgumentException("HTML input must be a string of at most 2097152 UTF-16 characters");
        return writeQueries(source, parseQueries(json));
    }

    private static List<Query> parseQueries(String json) {
        if (json == null || json.length() > MAX_PLAN) throw new IllegalArgumentException("HTML query plan exceeds 16384 characters");
        JsonElement parsed = GSON.fromJson(json, JsonElement.class);
        if (parsed == null || !parsed.isJsonObject()) throw new IllegalArgumentException("HTML query plan must be a JSON object");
        JsonObject plan = parsed.getAsJsonObject();
        keys(plan, PLAN_KEYS);
        JsonElement value = plan.get("queries");
        if (value == null || !value.isJsonObject() || value.getAsJsonObject().isEmpty() || value.getAsJsonObject().size() > MAX_QUERIES) throw new IllegalArgumentException("HTML query requires 1..16 queries");
        return value.getAsJsonObject().entrySet().stream().map(entry -> parseQuery(entry.getKey(), entry.getValue())).toList();
    }

    private static Query parseQuery(String name, JsonElement value) {
        if (name.isEmpty() || name.length() > 128 || !value.isJsonObject()) throw new IllegalArgumentException("Invalid HTML query");
        JsonObject options = value.getAsJsonObject();
        keys(options, QUERY_KEYS);
        Evaluator selector = Selector.evaluatorOf(string(options, "selector"));
        int maxRows = rows(options);
        List<Field> fields = fields(options);
        return new Query(name, selector, maxRows, fields);
    }

    private static int rows(JsonObject plan) {
        if (!plan.has("maxRows")) return MAX_ROWS;
        JsonElement value = plan.get("maxRows");
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("maxRows must be an integer from 1 to 1024");
        int rows;
        try {
            rows = new BigDecimal(value.getAsString()).intValueExact();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("maxRows must be an integer from 1 to 1024", error);
        }
        if (rows < 1 || rows > MAX_ROWS) throw new IllegalArgumentException("maxRows must be an integer from 1 to 1024");
        return rows;
    }

    private static List<Field> fields(JsonObject plan) {
        JsonElement value = plan.get("fields");
        if (value == null || !value.isJsonObject() || value.getAsJsonObject().isEmpty() || value.getAsJsonObject().size() > MAX_FIELDS) throw new IllegalArgumentException("HTML query requires 1..32 fields");
        return value.getAsJsonObject().entrySet().stream().map(entry -> parseField(entry.getKey(), entry.getValue())).toList();
    }

    private static Field parseField(String name, JsonElement value) {
        if (name.isEmpty() || name.length() > 128 || !value.isJsonObject()) throw new IllegalArgumentException("Invalid HTML query field");
        JsonObject options = value.getAsJsonObject();
        keys(options, FIELD_KEYS);
        boolean all = parseAll(options);
        Evaluator selector = options.has("selector") ? Selector.evaluatorOf(string(options, "selector")) : null;
        String attribute = options.has("attribute") ? string(options, "attribute") : null;
        return new Field(name, selector, attribute, all);
    }

    private static boolean parseAll(JsonObject options) {
        JsonElement value = options.get("all");
        if (value == null) return false;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("HTML query all must be boolean");
        return value.getAsBoolean();
    }

    private static void keys(JsonObject object, Set<String> allowed) {
        for (String key : object.keySet()) if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown HTML query option: " + key);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().isBlank()) throw new IllegalArgumentException("HTML query " + key + " must be a non-empty string");
        return value.getAsString();
    }

    private static String writeQueries(String source, List<Query> queries) throws IOException {
        Output output = new Output();
        JsonWriter writer = new JsonWriter(output);
        Element document = Jsoup.parse(source);
        writer.beginObject();
        for (Query query : queries) {
            writer.name(query.name);
            writeRows(writer, output, document, query);
        }
        writer.endObject();
        return output.toString();
    }

    private static void writeRows(JsonWriter writer, Output output, Element document, Query query) throws IOException {
        writer.beginArray();
        try (Stream<Element> matches = document.selectStream(query.selector)) {
            Iterator<Element> iterator = matches.iterator();
            int count = 0;
            while (iterator.hasNext()) {
                if (++count > query.maxRows) throw new IllegalArgumentException("HTML query exceeds maxRows: " + query.name);
                writeRow(writer, output, iterator.next(), query.fields);
            }
        }
        writer.endArray();
    }

    private static void writeRow(JsonWriter writer, Output output, Element row, List<Field> fields) throws IOException {
        writer.beginObject();
        for (Field field : fields) {
            writer.name(field.name);
            writeField(writer, output, row, field);
        }
        writer.endObject();
    }

    private static void writeField(JsonWriter writer, Output output, Element row, Field field) throws IOException {
        try (Stream<Element> selected = field.selector == null ? Stream.of(row) : row.selectStream(field.selector).filter(child -> child != row)) {
            if (field.all) writeValues(writer, output, selected, field);
            else writeValue(writer, output, selected.findFirst().orElse(null), field);
        }
    }

    private static void writeValues(JsonWriter writer, Output output, Stream<Element> selected, Field field) throws IOException {
        writer.beginArray();
        Iterator<Element> nodes = selected.iterator();
        while (nodes.hasNext()) writeValue(writer, output, nodes.next(), field);
        writer.endArray();
    }

    private static void writeValue(JsonWriter writer, Output output, Element node, Field field) throws IOException {
        if (node == null || field.attribute != null && !node.hasAttr(field.attribute)) writer.nullValue();
        else writer.value(field.attribute == null ? text(node, MAX_OUTPUT - output.length()) : node.attr(field.attribute));
    }

    private static String text(Element element, int remaining) {
        StringBuilder text = new StringBuilder();
        NodeTraversor.traverse((node, depth) -> {
            String value = node instanceof TextNode item ? item.getWholeText() : node instanceof DataNode item ? item.getWholeData() : null;
            if (value == null) return;
            if (value.length() > remaining - text.length()) throw new IllegalArgumentException("HTML query output exceeds 1048576 characters");
            text.append(value);
        }, element);
        return text.toString();
    }

    private record Field(String name, Evaluator selector, String attribute, boolean all) {
    }

    private record Query(String name, Evaluator selector, int maxRows, List<Field> fields) {
    }

    private static final class Output extends Writer {

        private final StringBuilder value = new StringBuilder();

        private void reserve(int count) {
            if (count > MAX_OUTPUT - value.length()) throw new IllegalArgumentException("HTML query output exceeds 1048576 characters");
        }

        int length() {
            return value.length();
        }

        @Override
        public void write(char[] chars, int offset, int count) {
            reserve(count);
            value.append(chars, offset, count);
        }

        @Override
        public void write(String text, int offset, int count) {
            reserve(count);
            value.append(text, offset, offset + count);
        }

        @Override
        public void write(int character) {
            reserve(1);
            value.append((char) character);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @NonNull
        @Override
        public String toString() {
            return value.toString();
        }
    }
}
