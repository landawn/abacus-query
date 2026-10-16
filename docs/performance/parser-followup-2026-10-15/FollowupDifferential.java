package com.landawn.abacus.query;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;

/** Compatibility corpus for ambiguous Unicode names, overlapping raw regions and contextual searches. */
public final class FollowupDifferential {
    private static BufferedWriter out;
    private static int rows;
    private static final SqlParser.Tokenizer[] TOKENIZERS = {SqlParser.tokenizer(),
        SqlParser.tokenizer(SqlParser.TokenizerConfig.builder().withSemicolonlessBatches(true).build())};

    public static void main(String[] args) {
        java.util.logging.LogManager.getLogManager().reset();
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8)) {
            out = writer;
            names();
            regions();
            searches();
            System.out.println("rows=" + rows);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static void names() throws Exception {
        String hi = String.valueOf((char) 0xD800), lo = String.valueOf((char) 0xDC00);
        String[] prefixes = {"SELECT ", "SELECT * FROM t WHERE a = ", "SELECT f()", "SELECT [q]", "SELECT 'q'", "SET a = "};
        String[] markers = {":id", ":", ":1", ":_id", ":\uD835\uDD38.id", ":" + hi, ":" + lo,
                ":a" + hi, ":a." + hi, ":" + hi + ":id", ":a:b", "::" + hi};
        String[] suffixes = {"", "['\\'':x]", "['\\'', :x]", "['\\\\',:x]", "[E'\\'':x]", "['a''b',:x]",
                "[\"\\\":x]", "[`\\`:x]", "[/* :x */]", "['\\'':x]['c',:d]", "['\\'':?]", "['\\'':#{id}]", "\\", "::text"};
        String[] tails = {"", " AND b = :ok", " AND b = :ok AND c = ?", " AND b = #{other}", " AND b = :a" + hi};
        for (String prefix : prefixes) for (String marker : markers) for (String suffix : suffixes) for (String tail : tails) {
            parse(prefix + marker + suffix + tail);
        }
    }

    private static void regions() throws Exception {
        String[] openers = {"[/*", "[/*[ordinary", "[/*[(", "[/*[WITH", "x[/*"};
        String[] bodies = {"*/ UPDATE x ", "*/ UPDATE t SET a = 1 ", "*/ WITH c AS (SELECT 1) ",
                "*/ INSERT INTO t VALUES (1) ", "*/ ordinary ", "*/ UPDATE_RETURNING ", "*/ DELETE$$x$$ ", "*/ UPDATE x[q] "};
        String[] tails = {"]'", " RETURNING x]'", " RETURNINGx]'", " UPDATE t]'", "]; SELECT 1'"};
        for (int n : new int[] {0, 1, 2, 3, 8, 16, 32}) for (String opener : openers) for (String body : bodies) for (String tail : tails) {
            classify("SELECT '" + opener.repeat(n) + body.repeat(n) + tail);
            classify("SELECT " + opener.repeat(n) + body.repeat(n) + tail);
        }
        Random random = new Random(202610156702L);
        String[] chunks = {"[", "]", "[/*", "/*", "*/", "-- x\n", "-- x\r", "(", ")", "'", "\"", "`", "$$", "$tag$",
                "UPDATE", "DELETE", "UPSERT", "MERGE", "INSERT", "RETURNING", "WITH", "EXPLAIN", "SELECT", "FROM", "SET", "INTO",
                "UPDATE_RETURNING", "xRETURNING", "returningx", "returning$tag$", "x", "x.y", " ", "\t", ",", ";", "#{id}", "?"};
        for (int n = 0; n < 20000; n++) {
            StringBuilder sql = new StringBuilder(n % 3 == 0 ? "SELECT '" : n % 3 == 1 ? "SELECT " : "WITH c AS (");
            for (int i = 0, size = 4 + random.nextInt(24); i < size; i++) {
                sql.append(chunks[random.nextInt(chunks.length)]);
                if (random.nextInt(3) == 0) sql.append(' ');
            }
            classify(sql.toString());
        }
        // Physical stop boundaries must not be confused with extended answers reaching end-of-input.
        for (int n = 1; n <= 64; n++) {
            for (String join : List.of("] [", "' [", "/* [", "-- [\n", "$tag$ [")) {
                classify("SELECT '" + ("[/*" + join).repeat(n) + "*/ UPDATE x ".repeat(n) + "] RETURNING'");
                classify("SELECT [update date], " + "[/*".repeat(n) + "*/ UPDATE x ".repeat(n) + "] [returning]");
            }
        }
    }

    private static void searches() throws Exception {
        for (int n : new int[] {1, 2, 8, 16, 32, 64, 128}) {
            StringBuilder hash = new StringBuilder("SELECT * FROM ");
            StringBuilder bracket = new StringBuilder("SELECT ");
            for (int i = 0; i < n; i++) {
                if (i > 0) { hash.append(", "); bracket.append(", "); }
                hash.append(i % 3 == 0 ? "/* gap */ #t" : "#t").append(i);
                bracket.append("[order-id").append(i).append("] [alias-id").append(i).append(']');
            }
            for (String text : List.of(hash.toString(), bracket.toString(), "SELECT x[0]" + " /* gap */ [1]".repeat(n))) {
                for (String tail : List.of("", " x", " /* ignored */ WHERE a = ?", " -- x\nWHERE a = :id")) {
                    String sql = text + tail;
                    StringBuilder result = new StringBuilder();
                    for (SqlParser.Tokenizer tokenizer : TOKENIZERS) {
                        for (String needle : List.of("SELECT", "FROM", "#t0", "[1]", "WHERE", "missing")) {
                            for (int from : new int[] {-1, 0, sql.length() / 2, sql.length(), sql.length() + 1}) {
                                result.append(tokenizer.indexOfToken(sql, needle, from, false)).append(',');
                            }
                        }
                    }
                    var matcher = SqlParser.hashPrefixedIdentifierMatcher(sql, SqlParser.defaultTokenizerConfig());
                    for (int at = sql.indexOf('#'); at >= 0; at = sql.indexOf('#', at + 1)) result.append(at).append(':').append(matcher.test(at)).append(',');
                    for (int at = sql.lastIndexOf('#'); at >= 0; at = sql.lastIndexOf('#', at - 1)) result.append(at).append(':').append(matcher.test(at)).append(',');
                    emit("search", sql, result.toString());
                }
            }
        }
    }

    private static void parse(String sql) throws Exception {
        try {
            ParsedSql parsed = ParsedSql.parse(sql, null);
            emit("parse", sql, parsed.originalSql() + "|" + parsed.parameterizedSql() + "|" + parsed.parameterCount() + "|"
                    + parsed.namedParameters() + "|" + parsed.isDataOperation() + "|" + Arrays.toString(parsed.positionalParameterOffsets()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            emit("parse", sql, failure.getClass().getName() + "|" + failure.getMessage());
        }
    }

    private static void classify(String sql) throws Exception {
        StringBuilder result = new StringBuilder();
        for (SqlParser.Tokenizer tokenizer : TOKENIZERS) {
            result.append(tokenizer.isSyntacticallyReadQuery(sql)).append(',').append(tokenizer.isReadOrInsertQuery(sql)).append(',');
        }
        emit("classify", sql, result.toString());
    }

    private static void emit(String kind, String sql, String result) throws Exception {
        out.write(kind + '\t' + rawUtf16(sql) + '\t' + rawUtf16(result));
        out.newLine();
        rows++;
    }

    // Charset encoders replace unpaired surrogates; preserve every raw UTF-16 code unit in this corpus.
    private static String rawUtf16(String value) {
        byte[] bytes = new byte[value.length() * 2];
        for (int i = 0; i < value.length(); i++) {
            bytes[2 * i] = (byte) (value.charAt(i) >>> 8);
            bytes[2 * i + 1] = (byte) value.charAt(i);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }
}
