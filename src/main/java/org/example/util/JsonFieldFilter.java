package org.example.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Projects a JSON document down to a set of dot-delimited field paths
 * (e.g. "basicInfo.pubDate", "tags.tagType"). A path segment applied to an
 * array is applied to every element of that array, so "tags.tagType" keeps
 * only the tagType field of each object in the "tags" array.
 * <p>
 * A segment that targets an array may also carry a JSONPath-style value
 * predicate, e.g. {@code tags[?(@.type == 'ANALYST')]}, which keeps only
 * the array elements matching the condition before any further projection
 * is applied. A leading "$." root prefix, if present, is ignored, so
 * {@code $.tags[?(@.type == 'ANALYST')]} and {@code tags[?(@.type == 'ANALYST')]}
 * are equivalent. Supported operators are {@code ==} and {@code !=}; the
 * left-hand side ({@code @.someField} or {@code @.nested.field}) is
 * evaluated against each array element, and the right-hand side is a
 * quoted string, a number, {@code true}/{@code false}, or {@code null}.
 */
public final class JsonFieldFilter {

    private static final Pattern SEGMENT_PATTERN = Pattern.compile("^([A-Za-z0-9_]+)(?:\\[\\?\\((.*)\\)\\])?$");
    private static final Pattern PREDICATE_PATTERN = Pattern.compile("^@\\.([A-Za-z0-9_.]+)\\s*(==|!=)\\s*(.+)$");

    private JsonFieldFilter() {
    }

    public static JsonNode filter(JsonNode source, List<String> fieldPaths) {
        if (fieldPaths == null || fieldPaths.isEmpty()) {
            return source;
        }
        return applyTree(source, buildFieldTree(fieldPaths));
    }

    private static FieldNode buildFieldTree(List<String> fieldPaths) {
        FieldNode root = new FieldNode();
        for (String rawPath : fieldPaths) {
            if (rawPath == null || rawPath.isBlank()) {
                continue;
            }
            FieldNode current = root;
            for (String segment : splitPath(stripRootPrefix(rawPath.trim()))) {
                Matcher matcher = SEGMENT_PATTERN.matcher(segment);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Invalid field path segment: '" + segment + "' in '" + rawPath + "'");
                }
                current = current.children.computeIfAbsent(matcher.group(1), key -> new FieldNode());
                if (matcher.group(2) != null) {
                    current.predicate = parsePredicate(matcher.group(2), rawPath);
                }
            }
        }
        return root;
    }

    private static String stripRootPrefix(String path) {
        if (path.startsWith("$.")) {
            return path.substring(2);
        }
        if (path.equals("$")) {
            return "";
        }
        return path;
    }

    /** Splits on '.' but treats anything inside [...] as opaque, since a predicate can itself contain dots (e.g. "@.a.b"). */
    private static List<String> splitPath(String path) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (char c : path.toCharArray()) {
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            }
            if (c == '.' && depth == 0) {
                segments.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            segments.add(current.toString());
        }
        return segments;
    }

    private static Predicate parsePredicate(String expression, String rawPath) {
        Matcher matcher = PREDICATE_PATTERN.matcher(expression.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid predicate '" + expression + "' in '" + rawPath + "'");
        }
        List<String> attributePath = List.of(matcher.group(1).split("\\."));
        String operator = matcher.group(2);
        Object expectedValue = parseValue(matcher.group(3).trim());
        return new Predicate(attributePath, operator, expectedValue);
    }

    private static Object parseValue(String raw) {
        if (raw.length() >= 2 && ((raw.startsWith("'") && raw.endsWith("'")) || (raw.startsWith("\"") && raw.endsWith("\"")))) {
            return raw.substring(1, raw.length() - 1);
        }
        if (raw.equals("true") || raw.equals("false")) {
            return Boolean.valueOf(raw);
        }
        if (raw.equals("null")) {
            return null;
        }
        try {
            return Double.valueOf(raw);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private static JsonNode applyTree(JsonNode source, FieldNode node) {
        if (source == null || source.isMissingNode() || source.isNull()) {
            return null;
        }
        if (source.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            for (JsonNode element : source) {
                if (node.predicate != null && !node.predicate.test(element)) {
                    continue;
                }
                JsonNode filteredElement = applyTree(element, node);
                if (filteredElement != null) {
                    result.add(filteredElement);
                }
            }
            return result;
        }
        if (!source.isObject() || node.children.isEmpty()) {
            return source;
        }
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, FieldNode> entry : node.children.entrySet()) {
            JsonNode child = source.get(entry.getKey());
            if (child == null) {
                continue;
            }
            JsonNode filteredChild = applyTree(child, entry.getValue());
            if (filteredChild != null) {
                result.set(entry.getKey(), filteredChild);
            }
        }
        return result;
    }

    private static boolean valuesEqual(JsonNode actual, Object expected) {
        if (actual == null || actual.isMissingNode() || actual.isNull()) {
            return expected == null;
        }
        if (expected == null) {
            return false;
        }
        if (expected instanceof String) {
            return actual.isTextual() && actual.asText().equals(expected);
        }
        if (expected instanceof Boolean) {
            return actual.isBoolean() && actual.asBoolean() == (Boolean) expected;
        }
        if (expected instanceof Double) {
            return actual.isNumber() && actual.asDouble() == (Double) expected;
        }
        return actual.asText().equals(String.valueOf(expected));
    }

    /** Node in the requested-fields tree; a non-null predicate only applies when the matching JSON value is an array. */
    private static final class FieldNode {
        final Map<String, FieldNode> children = new LinkedHashMap<>();
        Predicate predicate;
    }

    private static final class Predicate {
        private final List<String> attributePath;
        private final String operator;
        private final Object expectedValue;

        Predicate(List<String> attributePath, String operator, Object expectedValue) {
            this.attributePath = attributePath;
            this.operator = operator;
            this.expectedValue = expectedValue;
        }

        boolean test(JsonNode element) {
            JsonNode actual = element;
            for (String segment : attributePath) {
                actual = actual == null ? null : actual.get(segment);
            }
            boolean equal = valuesEqual(actual, expectedValue);
            return "!=".equals(operator) ? !equal : equal;
        }
    }
}
