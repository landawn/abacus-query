package com.landawn.abacus.query;

import java.io.BufferedWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.IntPredicate;

/** Deterministic compatibility and cursor-equivalence corpus; no timing assertions. */
public final class ParserEdgeDifferential {
    private static BufferedWriter out;
    private static int rows;
    private static Method cursorFactory;
    private static Method cursorNext;
    private static Method cursorStart;
    private static Method cursorEnd;
    private static Method cursorToken;

    public static void main(String[] args) throws Exception {
        java.util.logging.LogManager.getLogManager().reset();
        try {
            cursorFactory = SqlParser.Tokenizer.class.getMethod("cursor", String.class);
            Class<?> type = cursorFactory.getReturnType();
            cursorNext = type.getMethod("next");
            cursorStart = type.getMethod("startIndex");
            cursorEnd = type.getMethod("endIndex");
            cursorToken = type.getMethod("token");
        } catch (NoSuchMethodException expectedOnBaseline) {
            cursorFactory = null;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8)) {
            out = writer;
            Random random = new Random(202610155601L);
            var configs = List.of(SqlParser.defaultTokenizerConfig(),
                    SqlParser.TokenizerConfig.builder().withSemicolonlessBatches(true).build(),
                    SqlParser.TokenizerConfig.builder().withSeparator(" AND").withSeparator("\t=>")
                            .withSeparator("+~+").withSeparator("\u03c0>").withSeparator("a[").withSeparator("N'").build());
            String[] atoms = { "x", "#t", "##tmp", "# comment\n", "#{", "#{\n", "#{id}", "#{ id, jdbcType=INT }",
                    "#{a[0]}", "#{map['k']}", ":id", ":id::text", ":x$tag$ abc x$tag$", ":a:b", ":\ud800", ":\u03c0",
                    "?", "??", "?||?", "?#{a}", "doc ? 'k'", "arr[?]", "x['a', :b]['c', :d]", "x[1] [2]",
                    "[order-id] [alias-id]", "[a]]b]", "/*x*/", "/**/", "/*/", "/* nested /* x */ y */", "*/",
                    "--x\r", "-- x\n", "// x\n", "'a'\n'b'", "'\\'", "E'\\'", "\"a\\\"b\"", "`a``b`",
                    "$$ x $$", "$tag$ x $tag$", "q'[x]'", "\u2003", "\u0000", "\ufeff", " ", "\t", ",", ";", "(", ")",
                    "COALESCE(a,b)", "ARRAY[:id]", "ARRAY[?,?]", "FROM", "JOIN", "SELECT", "UPDATE", "RETURNING", "+~+", ">", "|" };
            for (int n = 0; n < 6000; n++) {
                StringBuilder sql = new StringBuilder(n % 3 == 0 ? "SELECT " : n % 3 == 1 ? "FROM " : "");
                for (int j = 0, count = 1 + random.nextInt(14); j < count; j++) {
                    sql.append(atoms[random.nextInt(atoms.length)]);
                    if (random.nextBoolean()) sql.append(' ');
                }
                for (int c = 0; c < configs.size(); c++) {
                    tokens("tokens" + c, sql.toString(), SqlParser.tokenizer(configs.get(c)));
                }
            }
            String[] expressions = { "?", "?, ?", ":id", ":id,:id", ":user.name", "#{id}", "#{id}:id", ":a:b",
                    "doc ?? :key", "doc ? :key", "doc ? ?", "JSON_OBJECT(KEY ? VALUE ?)", "?- ?::line", "?- CAST(? AS text)::line",
                    "arr[?,?]", "arr['a', :id]", "ARRAY['a\\'', :id]", "x[?]['a', ?]", "'a'\n'b'", "? // x\n AND id = 1",
                    "? /* ? */ + ?", "?#{id}", ":id::text", "f():n", "'k':v", "#{map['k']}", ":\ud800", ":\ud835\udc9c.name",
                    "#{:id}", "#{ }#{id}", "#{ id, typeHandler=handler(?) }", "ARRAY[:id, #{other}]" };
            String[] prefixes = { "SELECT ", "VALUES (", "WITH c AS (SELECT 1) SELECT ", "EXPLAIN SELECT ",
                    "CREATE TABLE t DEFAULT ", "SET x = ", "\ufeff SELECT ", "{call f(", "{:result = call f(" };
            for (int n = 0; n < 8000; n++) {
                StringBuilder sql = new StringBuilder(prefixes[n % prefixes.length]);
                for (int j = 0, count = 1 + random.nextInt(5); j < count; j++) {
                    if (j > 0) sql.append(n % 2 == 0 ? ", " : " + ");
                    sql.append(expressions[random.nextInt(expressions.length)]);
                }
                if (n % 5 == 0) sql.append(" ; ; \u2003");
                parse(sql.toString());
                classify(sql.toString());
            }
            for (int n = 0; n < 1500; n++) {
                StringBuilder sql = new StringBuilder("SELECT * FROM ");
                List<Integer> hashes = new ArrayList<>();
                for (int j = 0, count = 1 + random.nextInt(18); j < count; j++) {
                    if (j > 0) sql.append(", ");
                    if (random.nextBoolean()) sql.append("fn(1, 'x') a, ");
                    if (random.nextBoolean()) sql.append("/* c */ ");
                    hashes.add(sql.length());
                    sql.append("#t").append(j);
                    if (random.nextBoolean()) sql.append(" [alias-id]");
                    if (j % 5 == 0) sql.append(" -- x\n");
                }
                String text = sql.toString();
                StringBuilder values = new StringBuilder();
                for (int order = 0; order < 3; order++) {
                    var positions = new ArrayList<>(hashes);
                    if (order == 1) Collections.reverse(positions);
                    if (order == 2) Collections.shuffle(positions, random);
                    IntPredicate matcher = SqlParser.hashPrefixedIdentifierMatcher(text, configs.get(n % configs.size()));
                    for (int position : positions) values.append(position).append(':').append(matcher.test(position)).append(',');
                }
                emit("memo", text, values.toString());
            }
            for (int n : new int[] { 0, 1, 2, 8, 64, 256 }) {
                classify("SELECT '" + "[/*".repeat(n) + "*/ UPDATE " + "x".repeat(n) + "]'");
                classify("SELECT 1\n" + "#{\n".repeat(n));
                classify("SELECT " + "#{x".repeat(n));
                classify("SELECT " + "#{x".repeat(n) + "}");
                classify("SELECT :x$tag$ abc x$tag$");
            }
        }
        System.out.println("rows=" + rows + " cursorChecked=" + (cursorFactory != null));
        System.exit(0);
    }

    private static void tokens(String kind, String sql, SqlParser.Tokenizer tokenizer) throws Exception {
        List<String> tokens = tokenizer.tokenize(sql);
        StringBuilder value = new StringBuilder(tokens.toString());
        List<String> walked = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        for (int index = 0; index < sql.length();) {
            String token = tokenizer.nextToken(sql, index);
            int end = tokenizer.nextTokenEndIndex(sql, index);
            if (end <= index) throw new AssertionError("Walker failed to advance");
            if (!token.isEmpty()) { walked.add(token); ends.add(end); }
            index = end;
        }
        value.append('|').append(walked).append('|').append(ends);
        for (int from : new int[] { -1, 0, sql.length() / 2, sql.length(), sql.length() + 1 }) {
            value.append('|').append(from).append(':').append(tokenizer.nextToken(sql, from))
                    .append(':').append(tokenizer.nextTokenEndIndex(sql, from));
        }
        if (cursorFactory != null) {
            Object cursor = cursorFactory.invoke(tokenizer, sql);
            int index = 0;
            while ((boolean) cursorNext.invoke(cursor)) {
                String token = (String) cursorToken.invoke(cursor);
                int start = (int) cursorStart.invoke(cursor);
                int end = (int) cursorEnd.invoke(cursor);
                if (index >= walked.size() || !walked.get(index).equals(token) || ends.get(index) != end
                        || !sql.substring(start, end).equals(token)) {
                    throw new AssertionError("Cursor differs from stateless scanner: " + sql);
                }
                index++;
            }
            if (index != walked.size() || (boolean) cursorNext.invoke(cursor)) throw new AssertionError("Cursor EOF differs");
        }
        emit(kind, sql, value.toString());
    }

    private static void parse(String sql) throws Exception {
        try {
            ParsedSql parsed = ParsedSql.parse(sql, null);
            emit("parse", sql, parsed.originalSql() + "|" + parsed.parameterizedSql() + "|" + parsed.parameterCount()
                    + "|" + parsed.namedParameters() + "|" + parsed.isDataOperation() + "|" + parsed.hashCode()
                    + "|" + Arrays.toString(parsed.positionalParameterOffsets()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            emit("parse", sql, failure.getClass().getName() + "|" + failure.getMessage());
        }
    }

    private static void classify(String sql) throws Exception {
        StringBuilder value = new StringBuilder();
        for (SqlParser.Tokenizer tokenizer : List.of(SqlParser.tokenizer(),
                SqlParser.tokenizer(SqlParser.TokenizerConfig.builder().withSemicolonlessBatches(true).build()))) {
            value.append(tokenizer.isSyntacticallyReadQuery(sql)).append('|').append(tokenizer.isReadOrInsertQuery(sql)).append('|');
        }
        value.append(SqlParser.isSelectQuery(sql)).append('|').append(SqlParser.isInsertQuery(sql)).append('|')
                .append(SqlParser.isUpdateQuery(sql)).append('|').append(SqlParser.isDeleteQuery(sql));
        emit("classify", sql, value.toString());
    }

    private static void emit(String kind, String sql, String result) throws Exception {
        out.write(kind + '\t' + Base64.getEncoder().encodeToString(sql.getBytes(StandardCharsets.UTF_8)) + '\t'
                + Base64.getEncoder().encodeToString(result.getBytes(StandardCharsets.UTF_8)));
        out.newLine();
        rows++;
    }
}
