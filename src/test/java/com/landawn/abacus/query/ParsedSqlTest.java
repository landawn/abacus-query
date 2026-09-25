package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.Strings;

@Tag("2025")
public class ParsedSqlTest extends TestBase {

    @Test
    public void testParse_GeometricLiteralsAndConstructorsHonorFinalCasts() {
        for (final String operand : List.of("line '(0,0),(1,0)'::text", "pg_catalog.line'(0,0),(1,0)' ::text",
                "line E'(0,0),(1,0)'::text", "line N'(0,0),(1,0)'::text", "line U&'(0,0),(1,0)'::text",
                "line(point(0,0), point(1,0))::text", "pg_catalog.lseg(point(0,0), point(1,0)) /* gap */ ::text")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = grouped ? "SELECT ARRAY[?- " + operand + "]" : "SELECT ?- " + operand;
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + ", :other"), sql);
            }
        }
    }

    @Test
    public void testParse_CastChainsSkipQuotedColonsAndArraySuffixes() {
        for (final String operand : List.of("?::\"odd::type\" ::line", "?::\"odd\"\"::type\" ::line", "?::text[]::text ::line",
                "?::text [] ::text ::line", "?::text[ ][]::text ::line", "?::varchar(30)[] ::line", "?::line [] ::line",
                "?::text[3] ::line", "?::text [ 30 ][] ::line", "?::text ARRAY::text ::line", "?::text ARRAY[3]::text ::line",
                "?::text array [3] ::text ::line")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = grouped ? "SELECT ARRAY[?- " + operand + "]" : "SELECT ?- " + operand;
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }
        }

        for (final String operand : List.of("?::\"odd::line\"", "?::line[]", "?::line []", "?::line[ ][]", "?::line [3]",
                "?::line ARRAY", "?::line ARRAY[3]", "?::line ARRAY [3]", "?::text[3] + other::line", "?::text[x] ::line",
                "?::text ARRAY[3] + other::line", "?::text ARRAY[x] ::line")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = grouped ? "SELECT ARRAY[?- " + operand + "]" : "SELECT ?- " + operand;
                assertEquals(2, ParsedSql.parse(sql).parameterCount(), sql);
            }
        }
    }

    @Test
    public void testParse_QuotedColonsInEscapeLiteralsAreNotCasts() {
        for (final boolean grouped : new boolean[] { false, true }) {
            final String expression = "?- line E'escaped\\'::text'";
            final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
            assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
            assertEquals(List.of("other"), ParsedSql.parse(sql + ", :other").namedParameters(), sql);
        }
    }

    @Test
    public void testParse_SqlJsonUniquenessClausesKeepRealBindings() {
        for (final String clause : List.of("WITH UNIQUE KEYS", "WITHOUT UNIQUE KEYS", "WITH UNIQUE", "without /* gap */ unique keys")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String expression = "JSON_OBJECT('k' VALUE ? " + clause + ")";
                final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + ", :other"), sql);
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + ", #{other}"), sql);
            }
        }
    }

    @Test
    public void testParse_ParenthesizedJsonArrayQueryKeepsQueryScope() {
        for (final String query : List.of("(SELECT 1) UNION SELECT 1 FROM t JOIN u JOIN v ON v.payload ? NULL ON NULL",
                "((SELECT 1)) UNION SELECT 1 FROM t JOIN u JOIN v ON v.payload ? NULL ON NULL")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String expression = "JSON_ARRAY(" + query + ")";
                final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(0, parsed.parameterCount(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                assertEquals(List.of("other"), ParsedSql.parse(sql + ", :other").namedParameters(), sql);
            }
        }

        // A scalar subquery can instead be the first value in a constructor's value list.
        final String values = "SELECT JSON_ARRAY((SELECT 1), ? NULL ON NULL)";
        assertEquals(1, ParsedSql.parse(values).parameterCount());
        assertArrayEquals(new int[] { values.indexOf('?') }, ParsedSql.parse(values).positionalParameterOffsets());
    }

    @Test
    public void testParse_ChainedGeometricCastsUseTheFinalType() {
        for (final String operand : List.of("CAST(? AS text)::line", "?::text ::line", "? :: text :: line",
                "CAST(? AS text) :: pg_catalog . \"line\"", "?::text /* cast */ ::lseg", "?::varchar(30)::line",
                "?::character varying::line", "?::character varying(30) ::line", "?::double precision::text ::line",
                "?::timestamp(3) without time zone::text ::line")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = grouped ? "SELECT ARRAY[?- " + operand + "]" : "SELECT ?- " + operand;
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }
        }

        for (final String operand : List.of("?::line ::text", "CAST(? AS line)::text", "?::text + other::line",
                "?::line ::varchar(30)", "?::line ::character varying", "?::varchar(30) + other::line")) {
            final String sql = "SELECT ?- " + operand;
            assertEquals(2, ParsedSql.parse(sql).parameterCount(), sql);
        }
    }

    @Test
    public void testParse_QualifiedAdjacentGeometricLiteralsMatchSpacedLiterals() {
        for (final String type : List.of("pg_catalog.line", "pg_catalog.lseg", "PG_CATALOG.LINE", "\"pg_catalog\".\"line\"")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String expression = "?-" + type + "'(0,0),(1,0)'";
                final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
                assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
                assertArrayEquals(new int[0], ParsedSql.parse(sql).positionalParameterOffsets(), sql);
                assertEquals(List.of("other"), ParsedSql.parse(sql + ", :other").namedParameters(), sql);
            }
        }

        for (final String type : List.of("other.line", "\"PG_CATALOG\".line", "pg_catalog.line_extra")) {
            assertEquals(1, ParsedSql.parse("SELECT ?-" + type + "'(0,0),(1,0)'").parameterCount(), type);
        }
    }

    @Test
    public void testParse_QuotePrefixesAndSparseOffsetFallbackAcrossSeveralGroups() {
        // Prefix-only quote checks must leave prefixed literals opaque. Extra literal markers force
        // the sparse ordinary/group positions to merge back into their original source order.
        final String sql = "SELECT ?, N'ARRAY[?]', ARRAY['?', ?], arr[?][?], '?' FROM t WHERE id = ?";
        final ParsedSql parsed = ParsedSql.parse(sql);
        final int arrayMarker = sql.indexOf(", ?]") + 2;
        final int chained = sql.indexOf("arr[");
        assertEquals(5, parsed.parameterCount());
        assertArrayEquals(new int[] { sql.indexOf('?'), arrayMarker, chained + 4, chained + 7, sql.lastIndexOf('?') },
                parsed.positionalParameterOffsets());
        assertEquals(sql, parsed.parameterizedSql());
        for (final String marker : List.of(":id", "#{id}")) {
            final String namedSql = "SELECT N'ARRAY[" + marker + "]', ARRAY['" + marker + "', " + marker + "]";
            final ParsedSql named = ParsedSql.parse(namedSql);
            assertEquals(1, named.parameterCount(), namedSql);
            assertEquals(List.of("id"), named.namedParameters(), namedSql);
            assertEquals("SELECT N'ARRAY[" + marker + "]', ARRAY['" + marker + "', ?]", named.parameterizedSql(), namedSql);
        }
    }

    @Test
    public void testParse_KeywordLengthDispatchKeepsIdentifierOperands() {
        // An identifier sharing a keyword's length or prefix is still a JSON-operator operand.
        for (final String column : List.of("id", "key", "name", "order_id", "oncall", "intervals", "selection", "returning_value")) {
            final String sql = "SELECT doc ? " + column + " FROM t WHERE id = ?";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount(), sql);
            assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
        }
        for (final String sql : List.of("SELECT INTERVAL ? DAY", "SELECT * FROM t WHERE name ILIKE ? ESCAPE '!'",
                "SELECT * FROM t WHERE name SIMILAR TO ?", "SELECT CURRENT_TIMESTAMP AT TIME ZONE ?")) {
            assertEquals(1, ParsedSql.parse(sql).parameterCount(), sql);
        }
    }


    @Test
    public void testParse_PositionalOffsetsRetainSourceLocationsWithoutSpecialSyntax() {
        // The direct-offset shortcut must retain original spacing and the trimmed source's indexes.
        for (final String body : List.of("?", "? + ?", "?::int, ?", "ARRAY[? ,\t?], ?", "arr[?][?], ?")) {
            final String source = "SELECT " + body + " FROM t /* no binding here */ WHERE id = ?";
            final ParsedSql parsed = ParsedSql.parse("  " + source + "  ");
            final int[] expected = java.util.stream.IntStream.range(0, source.length()).filter(i -> source.charAt(i) == '?').toArray();
            assertEquals(expected.length, parsed.parameterCount(), source);
            assertArrayEquals(expected, parsed.positionalParameterOffsets(), source);
            final int[] returned = parsed.positionalParameterOffsets();
            returned[0] = -1;
            assertArrayEquals(expected, parsed.positionalParameterOffsets(), "Cached offsets must remain immutable");
        }
    }

    @Test
    public void testParse_PositionalOffsetsExcludeExtraSourceQuestionMarks() {
        // Equal raw/classified counts are the shortcut's proof. An extra '?' must force alignment,
        // whether it is before or after the bindings and whether the binding list is sparse or dense.
        for (final int count : new int[] { 1, 64 }) {
            final String array = "ARRAY[" + String.join(",", java.util.Collections.nCopies(count, "?")) + "]";
            for (final String extra : List.of("'?'", "doc ? 'k'", "doc ?? 'k'", "?- line '{1,0,0}'", "[what?]")) {
                for (final boolean before : new boolean[] { false, true }) {
                    final String sql = "SELECT " + (before ? extra + ", " + array : array + ", " + extra) + " FROM t /* ? */ WHERE id = ?";
                    final int start = sql.indexOf("ARRAY[");
                    final int[] expected = new int[count + 1];
                    for (int i = 0; i < count; i++) {
                        expected[i] = start + "ARRAY[".length() + 2 * i;
                    }
                    expected[count] = sql.lastIndexOf('?');
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(count + 1, parsed.parameterCount(), sql);
                    assertArrayEquals(expected, parsed.positionalParameterOffsets(), sql);
                }
            }
        }
    }

    @Test
    public void testParse_CommaSeparatedBindingsRetainAllOffsetsAcrossGroups() {
        // A large first group donates its offset array; later markers must append without losing it.
        for (final String prefix : List.of("ARRAY", "arr", "ARRAY ")) {
            final String sql = "SELECT " + prefix + "[ ? ,\t? ,\n? ], arr[?][?], ARRAY["
                    + String.join(", ", java.util.Collections.nCopies(2048, "?")) + "] FROM t WHERE id = ?";
            final ParsedSql parsed = ParsedSql.parse(sql);
            final int[] expected = java.util.stream.IntStream.range(0, sql.length()).filter(i -> sql.charAt(i) == '?').toArray();
            assertEquals(2054, parsed.parameterCount(), sql);
            assertArrayEquals(expected, parsed.positionalParameterOffsets(), sql);
            assertEquals(sql, parsed.parameterizedSql(), sql);
        }
    }

    @Test
    public void testParse_CommaBindingShortcutFallsBackForExpressionSyntax() {
        // Commas alone do not establish a simple binding list: quoted text, comments, operators
        // and nested expressions still require the complete classifier.
        for (final String expression : List.of("?, doc ?? 'k', ?", "?, doc ? 'k', ?", "?, '?', ?", "?, COALESCE(?, 0)",
                "? /* ? */ , ?", "? -- ?\n, ?", "? + ?", "JSON_OBJECT('k' VALUE ? NULL ON NULL), ?")) {
            final String sql = "SELECT ARRAY[" + expression + "] FROM t";
            assertEquals(2, ParsedSql.parse(sql).parameterCount(), sql);
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = :id"), sql);
        }
        assertEquals(0, ParsedSql.parse("SELECT ARRAY[??]").parameterCount());
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[?, :id]"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[?, #{id}]"));
    }

    @Test
    public void testParse_DeferredOperandChecksKeepQuestionMarkContexts() {
        for (final String expression : List.of("doc /* gap */ ? 'k'", "(doc) ? 'k'", "doc ? :key", "doc ? #{key}",
                "?- CAST(:key AS line)", "JSON_OBJECT('k' VALUE :key NULL ON NULL)")) {
            final ParsedSql parsed = ParsedSql.parse("SELECT " + expression + " FROM t");
            final boolean named = expression.contains(":key") || expression.contains("#{key}");
            assertEquals(named ? 1 : 0, parsed.parameterCount(), expression);
            assertEquals(named ? List.of("key") : List.of(), parsed.namedParameters(), expression);
            assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), expression);
        }
        for (final String expression : List.of("? + ?", "? || ?", "? /* gap */ + ?", "COALESCE(?, ?)", "?- ?::line, ?")) {
            assertEquals(2, ParsedSql.parse("SELECT " + expression).parameterCount(), expression);
        }
    }

    @Test
    public void testParse_StandaloneSubscriptsRejectMixedParameterStyles() {
        for (final String sql : List.of("SELECT ARRAY [?, :id]", "SELECT ARRAY [ :id, ? ]", "SELECT ARRAY [?, #{id}]",
                "SELECT ARRAY [ ? /* comment */ , :id ]", "SELECT ARRAY [ :id /* comment */ , ? ]")) {
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
        }
    }

    @Test
    public void testParse_JdbcEscapedQuestionOperatorsDoNotBind() {
        for (final String expression : List.of("doc ?? 'k'", "doc??'k'", "doc ??| keys", "doc ??& keys", "doc @?? path", "??- line '{1,0,0}'")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(0, parsed.parameterCount(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                assertEquals(sql, parsed.parameterizedSql(), sql);

                final String positional = sql + " WHERE id = ?";
                final ParsedSql withBinding = ParsedSql.parse(positional);
                assertEquals(1, withBinding.parameterCount(), positional);
                assertArrayEquals(new int[] { positional.lastIndexOf('?') }, withBinding.positionalParameterOffsets(), positional);
                assertEquals(positional, withBinding.parameterizedSql(), positional);

                for (final String binding : List.of(":id", "#{id}")) {
                    final ParsedSql named = ParsedSql.parse(sql + " WHERE id = " + binding);
                    assertEquals(1, named.parameterCount(), sql);
                    assertEquals(List.of("id"), named.namedParameters(), sql);
                    assertEquals(positional, named.parameterizedSql(), sql);
                    assertArrayEquals(new int[0], named.positionalParameterOffsets(), sql);
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = ? AND other_id = " + binding), sql);
                }
            }
        }
    }

    @Test
    public void testParse_JdbcEscapesPreserveAdjacentMyBatisBindings() {
        for (final String operator : List.of("??", "@??", "??|", "??&")) {
            for (final String binding : List.of("#{key}", "#{ key, jdbcType=OTHER }")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String expression = "doc " + operator + binding;
                    final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t";
                    final ParsedSql parsed = ParsedSql.parse(sql);

                    assertEquals(1, parsed.parameterCount(), sql);
                    assertEquals(List.of("key"), parsed.namedParameters(), sql);
                    assertEquals(sql.replace(binding, "?"), parsed.parameterizedSql(), sql);
                    assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = ?"), sql);
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = :id"), sql);
                }
            }
        }

        for (final int questionMarks : new int[] { 4, 6 }) {
            final String sql = "SELECT doc " + "?".repeat(questionMarks) + "#{key}";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount(), sql);
            assertEquals(List.of("key"), parsed.namedParameters(), sql);
            assertEquals(sql.replace("#{key}", "?"), parsed.parameterizedSql(), sql);
            assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
        }

        final ParsedSql repeated = ParsedSql.parse("SELECT doc ??#{key} OR other @??#{path}");
        assertEquals(2, repeated.parameterCount());
        assertEquals(List.of("key", "path"), repeated.namedParameters());
        assertEquals("SELECT doc ??? OR other @???", repeated.parameterizedSql());
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT doc ??#{key"));
    }

    @Test
    public void testParse_JdbcEscapesPreserveSplitMyBatisMetadataAndQuotedCasts() {
        // Restoring the opener must happen before operand classification: option text is opaque,
        // even when it contains question marks or unbalanced parentheses. A quoted cast suffix
        // must not make the restored binding look like a quoted SQL token.
        for (final String binding : List.of("#{ key, typeHandler=handler(?) }", "#{ key, typeHandler=handler( }",
                "#{ key, typeHandler=handler) }")) {
            for (final String operator : List.of("??", "@??")) {
                final String type = operator.equals("??") ? "text" : "jsonpath";
                for (final String cast : List.of("", "::\"" + type + "\"", " :: \"" + type + "\"")) {
                    for (final boolean grouped : new boolean[] { false, true }) {
                        final String expression = "doc " + operator + binding + cast;
                        final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t WHERE id = #{id}";
                        final ParsedSql parsed = ParsedSql.parse(sql);

                        assertEquals(2, parsed.parameterCount(), sql);
                        assertEquals(List.of("key", "id"), parsed.namedParameters(), sql);
                        assertEquals(sql.replace(binding, "?").replace("#{id}", "?"), parsed.parameterizedSql(), sql);
                        assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                    }
                }
            }
        }
    }

    @Test
    public void testParse_EscapedMyBatisProbeInQuotedTextKeepsOperatorGapsAndSourceOffsets() {
        // The cheap SQL-text probe can fire on a literal or comment. It must not then reinterpret
        // separated operators as escapes, or move a real placeholder's offset after comments disappear.
        for (final String gap : List.of(" ", "/* gap */", "/* first *//* second */", " -- gap\n")) {
            final String sql = "SELECT doc ?" + gap + "?, '??#{literal}' FROM t /* ??#{comment} */";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount(), sql);
            assertTrue(parsed.namedParameters().isEmpty(), sql);
            assertArrayEquals(new int[] { sql.indexOf("?, '") }, parsed.positionalParameterOffsets(), sql);
        }
    }

    @Test
    public void testParse_JdbcEscapedMyBatisBoundaryDoesNotHideOtherTokens() {
        // Odd-length runs retain a real JDBC marker after the escaped pairs.
        for (final String expression : List.of("doc ???#{key}", "doc @???#{path}", "doc ?????#{key}", "doc @?????#{path}")) {
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT " + expression));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[" + expression + "]"));
        }

        final String sql = "SELECT left_line ??# right_line, '??#{literal}', ? /* ??#{comment} */";
        final ParsedSql parsed = ParsedSql.parse(sql);
        assertEquals(1, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
        assertEquals(sql.substring(0, sql.indexOf(" /*")), parsed.parameterizedSql());
        assertArrayEquals(new int[] { sql.indexOf(", ?") + 2 }, parsed.positionalParameterOffsets());
    }

    @Test
    public void testParse_JdbcEscapesPreserveRealQuestionMarkOperands() {
        // The final question mark binds after an escaped operator; offsets must never identify its escape.
        for (final String expression : List.of("doc ???", "doc ?? ?", "doc ??|?", "doc ??&?", "doc @???", "doc ???? ?")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }
        }

        for (final String expression : List.of("? ?? ?", "? ??| ?", "? ??& ?")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression);
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(2, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }
        }
    }

    @Test
    public void testParse_JdbcQuestionEscapesRequireAdjacency() {
        // The bracket lexer discards gaps, so source offsets must distinguish ? ? from the ?? escape.
        for (final String gap : List.of(" ", "\t", "\n", "/* gap */", "/* first *//* second */", " -- gap\n")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String expression = "doc ?" + gap + "?";
                final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }
        }

        final String quoted = "SELECT '??', \"??\", [??], t.[??], ? /* ?? */";
        final ParsedSql parsed = ParsedSql.parse(quoted);
        assertEquals(1, parsed.parameterCount());
        assertArrayEquals(new int[] { quoted.indexOf(", ?") + 2 }, parsed.positionalParameterOffsets());
    }

    @Test
    public void testParse_CompactOperatorsDoNotSwallowPositionalPlaceholders() {
        // Longest-match tokenization claims "?-", "?|", "?&", "?#", "?||" and "?-|" as single operator
        // tokens, so a placeholder written against one of them used to disappear from parameterCount()
        // while a JDBC driver still saw it and demanded a binding.
        for (final String compact : List.of("-1", "|1", "&1", "#1", "||'x'")) {
            final String bare = "SELECT ?" + compact + " FROM t";
            final ParsedSql parsedBare = ParsedSql.parse(bare);
            assertEquals(1, parsedBare.parameterCount(), bare);
            assertEquals(bare, parsedBare.parameterizedSql(), bare);
            assertArrayEquals(new int[] { bare.indexOf('?') }, parsedBare.positionalParameterOffsets(), bare);

            // A bracket group reaches the same verdict for the same text.
            final String grouped = "SELECT ARRAY[?" + compact + "]";
            final ParsedSql parsedGrouped = ParsedSql.parse(grouped);
            assertEquals(1, parsedGrouped.parameterCount(), grouped);
            assertArrayEquals(new int[] { grouped.indexOf('?') }, parsedGrouped.positionalParameterOffsets(), grouped);
        }

        // Every marker of a compact concatenation is counted, in both positions.
        for (final String sql : List.of("SELECT ?||? FROM t", "SELECT ARRAY[?||?]")) {
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(2, parsed.parameterCount(), sql);
            assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
        }

        // The swallowed marker also hid a genuine mix of styles from the guard.
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ?-:n FROM t"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[?-:n]"));

        // In operator position the same spellings remain PostgreSQL operators and bind nothing.
        for (final String sql : List.of("SELECT * FROM t WHERE box1 ?- box2", "SELECT * FROM t WHERE line1 ?|| line2",
                "SELECT * FROM t WHERE p1 ?| p2", "SELECT * FROM t WHERE tags ?& ARRAY['a']", "SELECT ARRAY[a ?- b]",
                "SELECT ARRAY[a ?|| b]")) {
            assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
        }

        assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE tags ?| ARRAY['a'] AND id = ?").parameterCount());
    }

    @Test
    public void testParse_CompactMarkerChainsRetainEveryOffsetInsideAndOutsideBrackets() {
        for (final String expression : List.of("?||?||?", "?-?-?", "?|?|?", "?&?&?", "?#?#?", "? || ? || ?",
                "?/* left */||?/* right */||?", "?-- left\n||?-- right\n||?")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                final int first = sql.indexOf('?');
                final int second = sql.indexOf('?', first + 1);
                final int third = sql.indexOf('?', second + 1);
                final ParsedSql parsed = ParsedSql.parse(sql);

                assertEquals(3, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { first, second, third }, parsed.positionalParameterOffsets(), sql);
            }
        }

        for (final String binding : List.of(":tail", "#{tail}")) {
            for (final String expression : List.of("?||?||" + binding, binding + "||?||?", "?/* gap */||?||" + binding)) {
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT " + expression + " FROM t"), expression);
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[" + expression + "] FROM t"), expression);
            }
        }
    }

    @Test
    public void testParse_LargeCompactConcatenationRetainsEveryOffsetInsideAndOutsideBrackets() {
        // Both scans must retain all markers in a long chain; a timing assertion would depend on the machine.
        final int markerCount = 16000;
        final String expression = "?||".repeat(markerCount - 1) + "?";

        for (final boolean grouped : new boolean[] { false, true }) {
            final String prefix = grouped ? "SELECT ARRAY[" : "SELECT ";
            final String sql = prefix + expression + (grouped ? "] FROM t" : " FROM t");
            final int[] expectedOffsets = new int[markerCount];

            for (int i = 0; i < markerCount; i++) {
                expectedOffsets[i] = prefix.length() + 3 * i;
            }

            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(sql, parsed.parameterizedSql());
            assertEquals(markerCount, parsed.parameterCount());
            assertArrayEquals(expectedOffsets, parsed.positionalParameterOffsets());
        }
    }

    @Test
    public void testParse_JsonOperatorAcceptsNullAndClauseNamedOperands() {
        for (final String rightOperand : List.of("NULL", "null", "format", "absent", "t.format", "t.absent", "t.\"NULL\"",
                "format('%s', 'key')", "pg_catalog.format('%s', 'key')")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String expression = "payload ? " + rightOperand;
                final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(sql, parsed.parameterizedSql());
                assertEquals(0, parsed.parameterCount(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);

                // A named binding elsewhere must not turn the genuine operator into a mixed-style error.
                for (final String binding : List.of(":id", "#{id}")) {
                    final ParsedSql withBinding = ParsedSql.parse(sql + " WHERE id = " + binding);
                    assertEquals(sql + " WHERE id = ?", withBinding.parameterizedSql());
                    assertEquals(List.of("id"), withBinding.namedParameters());
                    assertEquals(1, withBinding.parameterCount());
                    assertArrayEquals(new int[0], withBinding.positionalParameterOffsets());
                }
            }
        }
    }

    @Test
    public void testParse_JsonFormatFunctionPreservesOperandBindings() {
        for (final boolean grouped : new boolean[] { false, true }) {
            final String prefix = grouped ? "SELECT ARRAY[" : "SELECT ";
            final String suffix = grouped ? "] FROM t" : " FROM t";

            for (final String expression : List.of(":payload ? format('%s', :key)", "#{payload} ? format('%s', #{key})")) {
                final ParsedSql parsed = ParsedSql.parse(prefix + expression + suffix);
                assertEquals(prefix + "? ? format('%s', ?)" + suffix, parsed.parameterizedSql());
                assertEquals(List.of("payload", "key"), parsed.namedParameters());
                assertEquals(2, parsed.parameterCount());
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets());
            }

            final String positionalSql = prefix + "payload ? format('%s', ?)" + suffix;
            final ParsedSql positional = ParsedSql.parse(positionalSql);
            assertEquals(1, positional.parameterCount());
            assertArrayEquals(new int[] { positionalSql.lastIndexOf('?') }, positional.positionalParameterOffsets());
        }
    }

    @Test
    public void testParse_JsonOperatorRetainsCompactRightHandPlaceholderOffset() {
        for (final String sql : List.of("SELECT payload ? ?||'suffix'", "SELECT ARRAY[payload ? ?||'suffix']")) {
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(sql, parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets());
        }

        // The same words form an operand and implicit alias outside a SQL/JSON constructor.
        final String ordinarySelect = "SELECT payload ? format JSON FROM t";
        final ParsedSql parsed = ParsedSql.parse(ordinarySelect);
        assertEquals(ordinarySelect, parsed.parameterizedSql());
        assertEquals(0, parsed.parameterCount());
        assertArrayEquals(new int[0], parsed.positionalParameterOffsets());
    }

    @Test
    public void testParse_SqlJsonClausePlaceholdersSurviveCommentsCaseAndNesting() {
        for (final String clause : List.of("null on null", "absent on null", "format json", "NULL /* gap */ ON NULL",
                "ABSENT -- gap\n ON NULL", "FORMAT /* gap */ JSON")) {
            for (final String expression : List.of("json_object('k' value ? " + clause + ")",
                    "json_array(json_object('outer' value json_object('inner' value ? " + clause + ")))")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(1, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = :id"), sql);
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = #{id}"), sql);
                }
            }
        }
    }

    @Test
    public void testParse_TypedUnaryGeometricOperatorsDoNotRequireBindings() {
        // Explicit geometry types disambiguate unary ?-/ ?| from a JDBC marker followed by arithmetic.
        // The equivalent bracket-free lseg spelling keeps cast fixtures independent of the tokenizer's
        // preexisting ambiguity when a bracket-group token contains a quoted closing bracket.
        for (final String expression : List.of("?- lseg '[(0,0),(1,0)]'", "?| line '{1,0,0}'",
                "?-lseg(point(0,0), point(1,0))", "?|line(point(0,0), point(0,1))",
                "?- '(0,0),(1,0)'::lseg", "?| '{1,0,0}'::line",
                "?- CAST('(0,0),(1,0)' AS lseg)", "?| CAST('{1,0,0}' AS line)",
                "?- /* geometry */ lseg '[(0,0),(1,0)]'", "?| -- geometry\n line '{1,0,0}'")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(0, parsed.parameterCount(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                assertTrue(parsed.namedParameters().isEmpty(), sql);
            }
        }

        for (final String expression : List.of("?-1", "?|1", "?-2.5", "?-column_value", "?-line", "?|line")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }
        }
    }

    @Test
    public void testParse_DeeplyParenthesizedGeometricOperandsPreserveOnlyRealBindings() {
        // Explicit type recognition unwraps parentheses iteratively. Deep but finite input must not
        // overflow the Java stack or count the unary operator as an extra parameter in either lexer.
        final String opening = "(".repeat(4_096);
        final String closing = ")".repeat(4_096);
        for (final String binding : List.of("?", ":geometry", "#{ geometry, jdbcType=OTHER }")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = "SELECT " + (grouped ? "ARRAY[" : "") + "?- " + opening + "CAST(" + binding + " AS line)" + closing
                        + (grouped ? "]" : "");
                final ParsedSql parsed = ParsedSql.parse(sql);
                final boolean positional = binding.equals("?");

                assertEquals(1, parsed.parameterCount());
                assertEquals(positional ? List.of() : List.of("geometry"), parsed.namedParameters());
                assertEquals(positional ? sql : sql.replace(binding, "?"), parsed.parameterizedSql());
                assertArrayEquals(positional ? new int[] { sql.indexOf("CAST(") + 5 } : new int[0], parsed.positionalParameterOffsets());
            }
        }
    }

    @Test
    public void testParse_RepeatedGeometricCastsKeepEveryOperandBindingOffset() {
        // Repeated casts must share the parenthesis index. A suffix scan for every operator turns
        // a long OR expression quadratic; verify offsets too so skipping operands cannot pass the bound.
        final int operands = 4_000;
        for (final boolean grouped : new boolean[] { false, true }) {
            final StringBuilder sql = new StringBuilder(grouped ? "SELECT ARRAY[" : "SELECT ");
            final int[] expectedOffsets = new int[operands];
            for (int i = 0; i < operands; i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append("?- CAST(");
                expectedOffsets[i] = sql.length();
                sql.append("? AS line)");
            }
            if (grouped) {
                sql.append(']');
            }
            final String statement = sql.toString();
            Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
                final ParsedSql parsed = ParsedSql.parse(statement);
                assertEquals(operands, parsed.parameterCount());
                assertArrayEquals(expectedOffsets, parsed.positionalParameterOffsets());
                assertTrue(parsed.namedParameters().isEmpty());
            });
        }
    }

    @Test
    public void testParse_GeometricCastsRespectTokenBoundariesAndQuotedTypes() {
        // A cast may span tokens in either lexer. Only the operand binds; the leading ?-/ ?| is unary.
        for (final String type : List.of("line", "lseg")) {
            final String literal = type.equals("line") ? "'{1,0,0}'" : "'(0,0),(1,0)'";
            for (final String typeName : List.of(type, type.toUpperCase(java.util.Locale.ROOT), "\"" + type + "\"", "pg_catalog." + type,
                    "\"pg_catalog\".\"" + type + "\"", "\"pg_catalog\" . \"" + type + "\"", "pg_catalog . " + type,
                    "pg_catalog.\"" + type + "\"", "\"pg_catalog\"." + type)) {
                for (final String operand : List.of("?", ":geometry", "#{geometry}", "#{ geometry, jdbcType=OTHER }", literal)) {
                    for (final String cast : List.of(operand + "::" + typeName, operand + ":: " + typeName, operand + " :: " + typeName,
                            "CAST(" + operand + " AS " + typeName + ")")) {
                        for (final String operator : List.of("?-", "?|")) {
                            for (final boolean grouped : new boolean[] { false, true }) {
                                final String sql = grouped ? "SELECT ARRAY[" + operator + " " + cast + "]" : "SELECT " + operator + " " + cast;
                                final ParsedSql parsed = ParsedSql.parse(sql);
                                final boolean named = operand.startsWith(":") || operand.startsWith("#{");

                                assertEquals(operand.equals(literal) ? 0 : 1, parsed.parameterCount(), sql);
                                assertEquals(named ? List.of("geometry") : List.of(), parsed.namedParameters(), sql);
                                assertEquals(named ? sql.replace(operand, "?") : sql, parsed.parameterizedSql(), sql);
                                assertArrayEquals(operand.equals("?") ? new int[] { sql.indexOf('?', sql.indexOf('?') + 1) } : new int[0],
                                        parsed.positionalParameterOffsets(), sql);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testParse_GeometricCastsIgnoreCommentsAndKeepTypeNamesExact() {
        for (final String cast : List.of("?:: /* type */ \"line\"", "? :: -- type\n lseg", "CAST(? AS /* schema */ pg_catalog /* dot */ . \"line\")",
                "CAST(? AS \"pg_catalog\". /* type */ lseg)", "(? + 0):: /* type */ line", "COALESCE(?, NULL):: \"lseg\"")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String sql = grouped ? "SELECT ARRAY[?- " + cast + "]" : "SELECT ?- " + cast;
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?', sql.indexOf('?') + 1) }, parsed.positionalParameterOffsets(), sql);
            }
        }

        // Quoted identifiers are case-sensitive, and an unrelated schema/type is not evidence of geometry.
        for (final String type : List.of("\"LINE\"", "\"LSEG\"", "\"PG_CATALOG\".line", "other.line", "line_extra", "\"pg_catalog.line\"", "integer")) {
            for (final String cast : List.of("?:: " + type, "CAST(? AS " + type + ")")) {
                final String sql = "SELECT ?- " + cast;
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(2, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }
        }

        // A quoted type suffix must not hide the binding before it, or expose markers inside the name.
        for (final String binding : List.of(":geometry", "#{geometry}")) {
            final String sql = "SELECT " + binding + "::\"type:literal#{literal}\"";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount(), sql);
            assertEquals(List.of("geometry"), parsed.namedParameters(), sql);
            assertEquals("SELECT ?::\"type:literal#{literal}\"", parsed.parameterizedSql(), sql);
        }

        for (final String sql : List.of("SELECT ':geometry::\"line\"'", "SELECT '#{geometry}::\"line\"'",
                "SELECT \"column::line:geometry\"", "SELECT [column::line:geometry]")) {
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(0, parsed.parameterCount(), sql);
            assertEquals(sql, parsed.parameterizedSql(), sql);
        }
    }

    @Test
    public void testParse_SqlJsonQueryBodiesDoNotUseConstructorValueClauses() {
        // FORMAT is the right operand and JSON its output alias in these SELECT bodies, not an option.
        for (final String query : List.of("SELECT payload ? format JSON FROM t", "select payload ? FORMAT json from t",
                "/* query */ SELECT payload ? format JSON FROM t",
                "WITH source AS (SELECT payload, format FROM t) SELECT payload ? format JSON FROM source",
                "(SELECT payload ? format JSON FROM t)", "SELECT payload ? /* operand */ format JSON FROM t")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String expression = "JSON_ARRAY(" + query + ")";
                final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(0, parsed.parameterCount(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);

                for (final String binding : List.of(":id", "#{id}")) {
                    final ParsedSql named = ParsedSql.parse(sql + " WHERE id = " + binding);
                    assertEquals(1, named.parameterCount(), sql);
                    assertEquals(List.of("id"), named.namedParameters(), sql);
                    assertArrayEquals(new int[0], named.positionalParameterOffsets(), sql);
                }
            }
        }

        for (final String binding : List.of(":id", "#{id}")) {
            final String sql = "SELECT JSON_ARRAY(SELECT payload ? format JSON FROM t WHERE id = " + binding + ")";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount(), sql);
            assertEquals(List.of("id"), parsed.namedParameters(), sql);
            assertEquals(sql.replace(binding, "?"), parsed.parameterizedSql(), sql);
            assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
        }

        for (final String sql : List.of("SELECT JSON_ARRAY(SELECT payload ? format JSON FROM t WHERE id = ?)",
                "SELECT JSON_ARRAY(SELECT JSON_OBJECT('k' VALUE ? FORMAT JSON) FROM t)",
                "SELECT JSON_ARRAY(SELECT JSON_ARRAY(? NULL ON NULL) FROM t)",
                "SELECT JSON_OBJECT('query' VALUE JSON_ARRAY(SELECT payload ? format JSON FROM t), 'binding' VALUE ? FORMAT JSON)",
                "SELECT JSON_ARRAY(JSON_ARRAY(SELECT payload ? format JSON FROM t), ? FORMAT JSON)")) {
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount(), sql);
            assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE other = :other"), sql);
        }
    }

    @Test
    public void testParse_QuestionOperatorLookaheadRespectsQuotedMarkersAndSplitBindings() {
        final String quotedMarker = "SELECT ARRAY['#{'], ? FROM t";
        final ParsedSql quoted = ParsedSql.parse(quotedMarker);
        assertEquals(quotedMarker, quoted.parameterizedSql());
        assertEquals(1, quoted.parameterCount());
        assertArrayEquals(new int[] { quotedMarker.indexOf('?') }, quoted.positionalParameterOffsets());
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY['#{'], ?, :id FROM t"));

        for (final String binding : List.of("#{ segment }", "#{ segment, typeHandler=handler(foo) }",
                "#{ segment, typeHandler=handler( }", "#{ segment, typeHandler=handler) }")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String prefix = grouped ? "SELECT ARRAY[" : "SELECT ";
                final String suffix = grouped ? "]" : "";
                final ParsedSql geometry = ParsedSql.parse(prefix + "?- " + binding + "::lseg" + suffix);
                assertEquals(prefix + "?- ?::lseg" + suffix, geometry.parameterizedSql());
                assertEquals(1, geometry.parameterCount());
                assertEquals(List.of("segment"), geometry.namedParameters());
                assertArrayEquals(new int[0], geometry.positionalParameterOffsets());

                // MyBatis option text is opaque metadata: even parentheses in it are not SQL grouping.
                final ParsedSql json = ParsedSql.parse(prefix + "JSON_ARRAY(" + binding + " ? NULL)" + suffix);
                assertEquals(prefix + "JSON_ARRAY(? ? NULL)" + suffix, json.parameterizedSql());
                assertEquals(1, json.parameterCount());
                assertEquals(List.of("segment"), json.namedParameters());
                assertArrayEquals(new int[0], json.positionalParameterOffsets());
            }
        }

        for (final String sql : List.of("SELECT ?- lseg E'[(0,0),(1,0)]'", "SELECT ARRAY[?- lseg E'[(0,0),(1,0)]']")) {
            final ParsedSql typedLiteral = ParsedSql.parse(sql);
            assertEquals(sql, typedLiteral.parameterizedSql());
            assertEquals(0, typedLiteral.parameterCount());
            assertArrayEquals(new int[0], typedLiteral.positionalParameterOffsets());
        }
    }

    @Test
    public void testParse_SqlJsonClauseKeywordsKeepPlaceholdersCountable() {
        // Inside a SQL/JSON constructor, NULL ON NULL, ABSENT ON NULL and FORMAT JSON begin clauses
        // after the value. Their word-shaped tokens must not turn that value's marker into an operator.
        for (final String clause : List.of("NULL ON NULL", "ABSENT ON NULL", "FORMAT JSON")) {
            final String bare = "SELECT JSON_OBJECT('k' VALUE ? " + clause + ") FROM t";
            assertEquals(1, ParsedSql.parse(bare).parameterCount(), bare);

            final String grouped = "SELECT ARRAY[JSON_OBJECT('k' VALUE ? " + clause + ")]";
            assertEquals(1, ParsedSql.parse(grouped).parameterCount(), grouped);
        }

        assertEquals(1, ParsedSql.parse("SELECT JSON_OBJECT(KEY 'k' VALUE ? ABSENT ON NULL) FROM t").parameterCount());
        assertEquals(List.of("v"), ParsedSql.parse("SELECT JSON_OBJECT('k' VALUE :v NULL ON NULL) FROM t").namedParameters());

        // The marker is a real parameter, so a named binding beside it is a genuine mix of styles.
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT JSON_OBJECT('k' VALUE ? NULL ON NULL), :other FROM t"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[JSON_OBJECT('k' VALUE ? NULL ON NULL), :other]"));

        // Outside the clause position, a column named like a clause keyword still carries the genuine operator.
        for (final String column : List.of("payload", "value", "format")) {
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE " + column + " ? 'key'").parameterCount(), column);
        }
    }

    @Test
    public void testParse_JsonExistenceOperatorIsClassifiedAlikeInsideAndOutsideBracketGroups() {
        // The tokenizer glues a bracket region into one token, which used to make the same operator read as
        // a placeholder there and collide with a named binding in the group. Both paths now apply the same
        // operand test: a '?' between two operands is the operator and is preserved, anywhere it appears.
        for (final String operator : List.of("?", "?|", "?&")) {
            final ParsedSql grouped = ParsedSql.parse("SELECT ARRAY [:payload " + operator + " 'key']");
            assertEquals("SELECT ARRAY [? " + operator + " 'key']", grouped.parameterizedSql());
            assertEquals(1, grouped.parameterCount());
            assertEquals(List.of("payload"), grouped.namedParameters());

            final ParsedSql ungrouped = ParsedSql.parse("SELECT * FROM t WHERE payload " + operator + " :key");
            assertEquals(1, ungrouped.parameterCount());
            assertEquals(List.of("key"), ungrouped.namedParameters());
        }

        // Glued and chained subscripts follow the same rule as a standalone group.
        assertEquals(1, ParsedSql.parse("SELECT payload[:key ? 'a'] FROM t").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT p[:a]['b' ? 'c'] FROM t").parameterCount());

        // A marker that opens or closes an element has no operand on one side and stays a placeholder,
        // so a named binding in the same group is still a genuine mix of styles.
        for (final String mixed : List.of("SELECT ARRAY [:id, ?]", "SELECT ARRAY [?, :id]", "SELECT ARRAY [:id ?]",
                "SELECT ARRAY [? :id]")) {
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(mixed), mixed);
        }

        // Positional subscripts keep counting every placeholder they hold.
        assertEquals(2, ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?, ?]").parameterCount());
        assertEquals(2, ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?::text, ?::text]").parameterCount());

        // One group holding both: only the placeholder is counted, and its recorded offset still points at
        // that marker rather than at the operator.
        final String both = "SELECT ARRAY [?, 'a' ? 'b']";
        final ParsedSql mixedGroup = ParsedSql.parse(both);
        assertEquals(1, mixedGroup.parameterCount());
        assertEquals(both, mixedGroup.parameterizedSql());
        assertArrayEquals(new int[] { both.indexOf('?') }, mixedGroup.positionalParameterOffsets());
    }

    @Test
    public void testParse_SubscriptPlaceholdersRespectOperatorAndKeywordBoundaries() {
        for (final String sql : List.of("SELECT ARRAY[x=? AND(y=?)] FROM t", "SELECT ARRAY[x=? OR(y=?)] FROM t",
                "SELECT ARRAY[(x=?)+(y=?)] FROM t")) {
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(sql, parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount(), sql);
            assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
        }

        for (final String sql : List.of("SELECT ARRAY[x=? AND(y=:y)] FROM t", "SELECT ARRAY[x=? OR(y=#{y})] FROM t")) {
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
        }
    }

    @Test
    public void testParse_SubscriptJsonOperatorsPreservePositionalOperandOffsets() {
        for (final String sql : List.of("SELECT ARRAY[doc ? ?]", "SELECT ARRAY[doc ? ?::text]", "SELECT ARRAY[payload ? ? AND(flag)]",
                "SELECT ARRAY[(doc) ? (?)]", "SELECT ARRAY[doc ?| ARRAY[?]]", "SELECT ARRAY[doc ?& ARRAY[?]]")) {
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(sql, parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount(), sql);
            assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
        }

        for (final String sql : List.of("SELECT ARRAY[? ? ?]", "SELECT ARRAY[docs[?] ? ?]")) {
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(sql, parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount(), sql);
            assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
        }

        final String chain = "SELECT ARRAY[? ? ? ? ?]";
        final ParsedSql chained = ParsedSql.parse(chain);
        assertEquals(3, chained.parameterCount());
        assertArrayEquals(new int[] { chain.indexOf('?'), chain.indexOf('?') + 4, chain.lastIndexOf('?') }, chained.positionalParameterOffsets());

        for (final String operator : List.of("@?", "?|", "?&", "?#", "?-", "?-|", "?||")) {
            assertEquals(0, ParsedSql.parse("SELECT ARRAY[doc " + operator + " 'key']").parameterCount(), operator);
        }
    }

    @Test
    public void testParse_SubscriptJsonOperandsKeepCommentsQuotesAndBindingsIntact() {
        for (final String sql : List.of("SELECT ARRAY[:payload /* ? :ignored */ ? /* right */ 'key']",
                "SELECT ARRAY[:payload -- ? :ignored\n ? 'key']", "SELECT ARRAY[:payload ? -- ? :ignored\n 'key']",
                "SELECT ARRAY[:payload?'key']", "SELECT ARRAY[(:payload)::jsonb ? 'key']", "SELECT ARRAY[:payload ? ('key')]")) {
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(sql.replace(":payload", "?"), parsed.parameterizedSql());
            assertEquals(List.of("payload"), parsed.namedParameters(), sql);
            assertEquals(1, parsed.parameterCount(), sql);
            assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
        }

        final ParsedSql function = ParsedSql.parse("SELECT ARRAY[:payload ? lower(:key)]");
        assertEquals("SELECT ARRAY[? ? lower(?)]", function.parameterizedSql());
        assertEquals(List.of("payload", "key"), function.namedParameters());
        assertEquals(2, function.parameterCount());

        final ParsedSql ibatis = ParsedSql.parse("SELECT ARRAY[#{payload, jdbcType=OTHER} ? 'key']");
        assertEquals("SELECT ARRAY[? ? 'key']", ibatis.parameterizedSql());
        assertEquals(List.of("payload"), ibatis.namedParameters());
        assertEquals(1, ibatis.parameterCount());

        for (final String sql : List.of("SELECT ARRAY['{ \"a\": 1 }'::jsonb ? 'a']", "SELECT ARRAY[\"has spaces\" ? 'key']",
                "SELECT ARRAY[docs[1] ? 'key']", "SELECT ARRAY[E'it\\'s a key' ? 'key']")) {
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(sql, parsed.parameterizedSql());
            assertEquals(0, parsed.parameterCount(), sql);
            assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
        }
    }

    @Test
    public void testParse_LargeCompactSubscriptRetainsEveryPositionalOffset() {
        // With no spaces or commas, rescanning both sides of each '?' makes this expression quadratic.
        // Verify all offsets without a machine-dependent timing assertion.
        final int markerCount = 16000;
        final String prefix = "SELECT ARRAY[";
        final String sql = prefix + "?+".repeat(markerCount - 1) + "?]";
        final int[] expectedOffsets = new int[markerCount];

        for (int i = 0; i < markerCount; i++) {
            expectedOffsets[i] = prefix.length() + 2 * i;
        }

        final ParsedSql parsed = ParsedSql.parse(sql);
        assertEquals(sql, parsed.parameterizedSql());
        assertEquals(markerCount, parsed.parameterCount());
        assertArrayEquals(expectedOffsets, parsed.positionalParameterOffsets());
    }

    @Test
    public void testParse_MixedParameterStyleMessageNamesStylesAndSubscriptRule() {
        // A '?' that opens a subscript element is a placeholder, so a named binding in the same group is a
        // real mix. The rejection explains which '?' counted and why a JSON operator would not have.
        final String mixedSubscript = "SELECT ARRAY [?, :id]";
        final String subscriptMessage = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(mixedSubscript)).getMessage();

        assertTrue(subscriptMessage.contains("found positional '?' and named ':propName'"), subscriptMessage);
        assertTrue(subscriptMessage.contains("inside a bracket group"), subscriptMessage);
        assertTrue(subscriptMessage.contains("JSON existence operator"), subscriptMessage);
        assertTrue(subscriptMessage.contains("typed unary geometric operators"), subscriptMessage);
        assertTrue(subscriptMessage.contains("SQL/JSON constructor clauses"), subscriptMessage);
        assertTrue(subscriptMessage.contains("SQL: " + mixedSubscript), subscriptMessage);

        final String ibatisMessage = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY [?, #{id}]")).getMessage();
        assertTrue(ibatisMessage.contains("found positional '?' and iBatis/MyBatis '#{propName}'"), ibatisMessage);
        assertTrue(ibatisMessage.contains("inside a bracket group"), ibatisMessage);

        // Ordinary positional conflicts get the same general guidance, but no bracket-specific sentence.
        final String plainMessage = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE a = ? AND b = :b"))
                .getMessage();
        assertTrue(plainMessage.contains("found positional '?' and named ':propName'"), plainMessage);
        assertFalse(plainMessage.contains("bracket group"), plainMessage);
        assertTrue(plainMessage.contains("JSON existence operator"), plainMessage);
        assertTrue(plainMessage.contains("typed unary geometric operators"), plainMessage);
        assertTrue(plainMessage.contains("SQL/JSON constructor clauses"), plainMessage);
        assertTrue(plainMessage.contains("SQL: SELECT * FROM t WHERE a = ? AND b = :b"), plainMessage);

        // Question marks in operators or quoted text do not justify positional-marker guidance.
        for (final String sql : List.of("SELECT * FROM t WHERE a = :a AND b = #{b}", "SELECT payload ? 'key', :a, #{b} FROM t",
                "SELECT ARRAY[:a, #{b}, '?']")) {
            final String namedAndIbatis = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql)).getMessage();
            assertTrue(namedAndIbatis.contains("found named ':propName' and iBatis/MyBatis '#{propName}'"), namedAndIbatis);
            assertFalse(namedAndIbatis.contains("bracket group"), namedAndIbatis);
            assertFalse(namedAndIbatis.contains("JSON existence operators"), namedAndIbatis);
            assertFalse(namedAndIbatis.contains("typed unary geometric operators"), namedAndIbatis);
            assertFalse(namedAndIbatis.contains("SQL/JSON constructor clauses"), namedAndIbatis);
            assertFalse(namedAndIbatis.contains("In compact expressions"), namedAndIbatis);
            assertTrue(namedAndIbatis.contains("SQL: " + sql), namedAndIbatis);
        }
    }

    @Test
    public void testParse_MixedStyleMessageExplainsSqlJsonValuesAndUnaryOperators() {
        // Clause keywords do not turn a constructor's value placeholder into an operator. The diagnostic
        // must explain why that value still counts even though words surround its question mark.
        for (final boolean grouped : new boolean[] { false, true }) {
            final String prefix = grouped ? "SELECT ARRAY[" : "SELECT ";
            final String suffix = grouped ? "]" : " FROM t";

            for (final String binding : List.of(":other", "#{other}")) {
                final String found = binding.startsWith(":") ? "found positional '?' and named ':propName'"
                        : "found positional '?' and iBatis/MyBatis '#{propName}'";

                for (final String clause : List.of("NULL ON NULL", "ABSENT ON NULL", "FORMAT JSON")) {
                    final String sql = prefix + "JSON_OBJECT('k' VALUE ? " + clause + "), " + binding + suffix;
                    final String message = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql)).getMessage();

                    assertTrue(message.contains(found), message);
                    assertEquals(grouped, message.contains("inside a bracket group"), message);
                    assertTrue(message.contains("SQL/JSON constructor clauses"), message);
                    assertTrue(message.contains("are still parameters"), message);
                    assertTrue(message.contains("SQL: " + sql), message);
                }

                // The unary operator does not bind. Only the separate '?' creates the mixed-style error.
                for (final String geometry : List.of("?- lseg '(0,0),(1,0)'", "?| line '{1,0,0}'")) {
                    final String sql = prefix + geometry + ", ?, " + binding + suffix;
                    final String message = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql)).getMessage();

                    assertTrue(message.contains(found), message);
                    assertEquals(grouped, message.contains("inside a bracket group"), message);
                    assertTrue(message.contains("typed unary geometric operators ('?-', '?|') are not parameters"), message);
                    assertTrue(message.contains("SQL: " + sql), message);

                    final ParsedSql namedOnly = ParsedSql.parse(prefix + geometry + ", " + binding + suffix);
                    assertEquals(1, namedOnly.parameterCount());
                    assertEquals(List.of("other"), namedOnly.namedParameters());
                    assertArrayEquals(new int[0], namedOnly.positionalParameterOffsets());
                }
            }
        }
    }

    @Test
    public void testParse_MixedStyleMessageExplainsCompactBindings() {
        // Compact arithmetic/concatenation retains its leading binding in both token streams.
        for (final String expression : List.of("?-1", "?||'x'")) {
            for (final boolean grouped : new boolean[] { false, true }) {
                for (final String binding : List.of(":other", "#{other}")) {
                    final String sql = grouped ? "SELECT ARRAY[" + expression + ", " + binding + "]"
                            : "SELECT " + expression + ", " + binding + " FROM t";
                    final String message = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql)).getMessage();

                    assertEquals(grouped, message.contains("inside a bracket group"), message);
                    assertTrue(message.contains("In compact expressions such as ?-1 and ?||'x', the leading '?' is a positional parameter."), message);
                    assertTrue(message.contains("SQL: " + sql), message);
                }
            }
        }
    }

    @Test
    public void testParse_StandaloneSubscriptsKeepQuotedAndCommentedMarkersLiteral() {
        final String sql = "SELECT ARRAY [?, ':literal', '#{literal}', /* :ignored #{ignored} */ ?]";
        final ParsedSql parsed = ParsedSql.parse(sql);
        assertEquals(sql, parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets());

        final ParsedSql named = ParsedSql.parse("SELECT ARRAY [:id, '?', /* ? */ :other]");
        assertEquals(List.of("id", "other"), named.namedParameters());
        assertEquals(2, named.parameterCount());
        assertEquals("SELECT ARRAY [?, '?', /* ? */ ?]", named.parameterizedSql());

        final ParsedSql quoted = ParsedSql.parse("SELECT t.[?, :literal], [#{literal}], [?identifier:literal] FROM t");
        assertEquals(0, quoted.parameterCount());
        assertEquals(quoted.originalSql(), quoted.parameterizedSql());
    }

    @Test
    public void testParse_SimpleSelect() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
        assertNotNull(parsed);
        assertEquals("SELECT * FROM users", parsed.originalSql());
        assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        assertEquals(0, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_WithQuestionMarkParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_WithMultipleQuestionMarks() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE age > ? AND status = ?");
        assertEquals("SELECT * FROM users WHERE age > ? AND status = ?", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_WithNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals(1, parsed.namedParameters().size());
        assertEquals("userId", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_WithMultipleNamedParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE age > :minAge AND age < :maxAge");
        assertEquals("SELECT * FROM users WHERE age > ? AND age < ?", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals(2, parsed.namedParameters().size());
        assertEquals("minAge", parsed.namedParameters().get(0));
        assertEquals("maxAge", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_WithIBatisParameter() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, email) VALUES (#{name}, #{email})");
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals(2, parsed.namedParameters().size());
        assertEquals("name", parsed.namedParameters().get(0));
        assertEquals("email", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_WithIBatisParameterOptions() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO users (id) VALUES (#{id,jdbcType=INTEGER})");
        assertEquals("INSERT INTO users (id) VALUES (?)", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals(1, parsed.namedParameters().size());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_MixedParametersThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("SELECT * FROM users WHERE id = ? AND name = :name");
        });
    }

    @Test
    public void testParse_MixedIBatisAndNamedThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("SELECT * FROM users WHERE id = :id AND name = #{name}");
        });
    }

    @Test
    public void testParse_AdjacentMixedIBatisAndNamedThrows() {
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :id#{name} FROM users"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT #{name}:id FROM users"));
    }

    @Test
    public void testParse_BracketQuotedIdentifiersDoNotCreateParameters() {
        ParsedSql plain = ParsedSql.parse("SELECT [column] FROM users");
        assertEquals("SELECT [column] FROM users", plain.parameterizedSql());
        assertEquals(0, plain.parameterCount());

        // ':' not at position 1 inside the brackets: bracket-quoted identifier, not a subscript binding.
        ParsedSql colonInside = ParsedSql.parse("SELECT [weird:name] FROM users");
        assertEquals("SELECT [weird:name] FROM users", colonInside.parameterizedSql());
        assertEquals(0, colonInside.parameterCount());

        ParsedSql ibatis = ParsedSql.parse("SELECT [#{identifier}] FROM users");
        assertEquals("SELECT [#{identifier}] FROM users", ibatis.parameterizedSql());
        assertEquals(0, ibatis.parameterCount());

        // '[' immediately after a qualification dot is always SQL Server qualified quoting; a
        // subscript can never directly follow '.', so ".[:identifier]" keeps its literal meaning.
        ParsedSql qualified = ParsedSql.parse("SELECT users.[:identifier] FROM users");
        assertEquals("SELECT users.[:identifier] FROM users", qualified.parameterizedSql());
        assertEquals(0, qualified.parameterCount());

        ParsedSql qualifiedPlain = ParsedSql.parse("SELECT db.[col] FROM db");
        assertEquals("SELECT db.[col] FROM db", qualifiedPlain.parameterizedSql());
        assertEquals(0, qualifiedPlain.parameterCount());

        // "[:]" has no valid parameter-name character after the ':' so it is not a named binding.
        ParsedSql emptySlice = ParsedSql.parse("SELECT arr [:] FROM t");
        assertEquals("SELECT arr [:] FROM t", emptySlice.parameterizedSql());
        assertEquals(0, emptySlice.parameterCount());

        ParsedSql arraySubscript = ParsedSql.parse("SELECT values_array[:index] FROM users");
        assertEquals("SELECT values_array[?] FROM users", arraySubscript.parameterizedSql());
        assertEquals(1, arraySubscript.parameterCount());
    }

    @Test
    public void testParse_StandaloneBracketSubscriptWithNamedParameterBinds() {
        // Whitespace before '[' makes the tokenizer emit "[:ids]" as a standalone token; the leading
        // "[:name" shape is recognized as a PostgreSQL-style subscript binding, not a bracket-quoted
        // identifier, so the named parameter is extracted instead of passing through as literal SQL.
        ParsedSql spaced = ParsedSql.parse("SELECT * FROM t WHERE arr = array [:ids]");
        assertEquals("SELECT * FROM t WHERE arr = array [?]", spaced.parameterizedSql());
        assertEquals(1, spaced.parameterCount());
        assertEquals(List.of("ids"), spaced.namedParameters());

        ParsedSql standalone = ParsedSql.parse("SELECT [:identifier] FROM users");
        assertEquals("SELECT [?] FROM users", standalone.parameterizedSql());
        assertEquals(1, standalone.parameterCount());
        assertEquals(List.of("identifier"), standalone.namedParameters());

        // Colon-style names follow identifier grammar. A digit cannot start a segment, so Oracle-style
        // ordinal markers are preserved rather than being misreported as named property bindings.
        ParsedSql ordinalSpaced = ParsedSql.parse("SELECT * FROM t WHERE a = arr [:2]");
        assertEquals("SELECT * FROM t WHERE a = arr [:2]", ordinalSpaced.parameterizedSql());
        assertEquals(0, ordinalSpaced.parameterCount());

        ParsedSql ordinalUnspaced = ParsedSql.parse("SELECT * FROM t WHERE a = arr[:2]");
        assertEquals("SELECT * FROM t WHERE a = arr[:2]", ordinalUnspaced.parameterizedSql());
        assertEquals(0, ordinalUnspaced.parameterCount());
    }

    @Test
    public void testParse_NullThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse(null);
        });
    }

    @Test
    public void testParse_EmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("");
        });
    }

    @Test
    public void testParse_WithTrailingSemicolon() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users;");
        assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        assertFalse(parsed.parameterizedSql().endsWith(";"));
    }

    @Test
    public void testParse_WithWhitespace() {
        ParsedSql parsed = ParsedSql.parse("  SELECT * FROM users  ");
        assertEquals("SELECT * FROM users", parsed.originalSql());
        assertEquals("SELECT * FROM users", parsed.parameterizedSql());
    }

    @Test
    public void testParse_Caching() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed1 = ParsedSql.parse(sql);
        ParsedSql parsed2 = ParsedSql.parse(sql);
        assertSame(parsed1, parsed2);
    }

    @Test
    public void testGetParameterizedSql_WithQuestionMarkParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        String sql = parsed.parameterizedSql();
        assertNotNull(sql);
        assertEquals("SELECT * FROM users WHERE id = ?", sql);
    }

    @Test
    public void testGetParameterizedSql_WithNamedParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :userName");
        String sql = parsed.parameterizedSql();
        assertNotNull(sql);
        assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", sql);
    }

    @Test
    public void testGetParameterizedSql_WithSingleNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        String sql = parsed.parameterizedSql();
        assertEquals("SELECT * FROM users WHERE id = ?", sql);
    }

    @Test
    public void testGetNamedParameters_WithNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        assertNotNull(parsed.namedParameters());
        assertEquals(1, parsed.namedParameters().size());
        assertEquals("userId", parsed.namedParameters().get(0));
    }

    @Test
    public void testGetParameterCount_WithQuestionMarkParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ? AND name = ?");
        int count = parsed.parameterCount();
        assertEquals(2, count);
    }

    @Test
    public void testSql() {
        String originalSql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(originalSql);
        assertEquals(originalSql, parsed.originalSql());
    }

    @Test
    public void testHashCode() {
        ParsedSql parsed1 = ParsedSql.parse("SELECT * FROM users");
        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
        assertEquals(parsed1.hashCode(), parsed2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
        assertTrue(parsed.equals(parsed));
    }

    @Test
    public void testEquals_EqualObjects() {
        ParsedSql parsed1 = ParsedSql.parse("SELECT * FROM users");
        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
        assertTrue(parsed1.equals(parsed2));
    }

    @Test
    public void testEquals_DifferentObjects() {
        ParsedSql parsed1 = ParsedSql.parse("SELECT * FROM users");
        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM orders");
        assertFalse(parsed1.equals(parsed2));
    }

    @Test
    public void testEquals_Null() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
        assertFalse(parsed.equals(null));
    }

    @Test
    public void testEquals_DifferentClass() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
        assertFalse(parsed.equals("SELECT * FROM users"));
    }

    @Test
    public void testToString() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        String str = parsed.toString();
        assertNotNull(str);
        assertTrue(str.contains("SELECT * FROM users WHERE id = :userId"));
        assertTrue(str.contains("SELECT * FROM users WHERE id = ?"));
    }

    @Test
    public void testParse_Update() {
        ParsedSql parsed = ParsedSql.parse("UPDATE users SET name = :name WHERE id = :id");
        assertEquals("UPDATE users SET name = ? WHERE id = ?", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("name", parsed.namedParameters().get(0));
        assertEquals("id", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_Delete() {
        ParsedSql parsed = ParsedSql.parse("DELETE FROM users WHERE id = :id");
        assertEquals("DELETE FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_Insert() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, age) VALUES (:name, :age)");
        assertEquals("INSERT INTO users (name, age) VALUES (?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("name", parsed.namedParameters().get(0));
        assertEquals("age", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_Call() {
        ParsedSql parsed = ParsedSql.parse("CALL refresh_user(:userId, :mode)");
        assertEquals("CALL refresh_user(?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("userId", parsed.namedParameters().get(0));
        assertEquals("mode", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_JdbcCallEscape_NamedParameters() {
        ParsedSql parsed = ParsedSql.parse("{call refresh_user(:userId, :mode)}");
        assertEquals("{call refresh_user(?, ?)}", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals(Arrays.asList("userId", "mode"), parsed.namedParameters());
    }

    @Test
    public void testParse_JdbcCallEscape_SpacedCall() {
        ParsedSql parsed = ParsedSql.parse("{ CALL proc(?) }");
        assertEquals("{ CALL proc(?) }", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_JdbcCallEscape_ReturnParameterForm_PositionalOnly() {
        ParsedSql parsed = ParsedSql.parse("{? = call get_val(?)}");
        assertEquals("{? = call get_val(?)}", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_JdbcCallEscape_IbatisNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("{call proc(#{id})}");
        assertEquals("{call proc(?)}", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_JdbcCallEscape_RejectsMixedParameterStyles() {
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("{? = call get_val(:id)}"));
    }

    @Test
    public void testParse_ReplaceStatement() {
        ParsedSql parsed = ParsedSql.parse("REPLACE INTO users (id, name) VALUES (:id, :name)");
        assertEquals("REPLACE INTO users (id, name) VALUES (?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
        assertEquals("name", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_NonQueryStatement() {
        ParsedSql parsed = ParsedSql.parse("CREATE TABLE users (id INT)");
        assertEquals("CREATE TABLE users (id INT)", parsed.parameterizedSql());
        assertEquals(0, parsed.parameterCount());
        assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_WithComments() {
        ParsedSql parsed = ParsedSql.parse("-- Comment\nSELECT * FROM users WHERE id = :id");
        assertNotNull(parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
    }

    @Test
    public void testParse_ParenthesizedSelectPrefix() {
        ParsedSql parsed = ParsedSql.parse("(SELECT :id)");
        assertEquals("(SELECT ?)", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_ExplainSelectPrefix() {
        ParsedSql parsed = ParsedSql.parse("EXPLAIN SELECT * FROM users WHERE id = :id");
        assertEquals("EXPLAIN SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_ExplainAnalyzeSelectPrefix() {
        ParsedSql parsed = ParsedSql.parse("EXPLAIN ANALYZE SELECT * FROM users WHERE id = :id");
        assertEquals("EXPLAIN ANALYZE SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_ValuesPrefix() {
        ParsedSql parsed = ParsedSql.parse("VALUES(:id, :name)");
        assertEquals("VALUES(?, ?)", parsed.parameterizedSql());
        assertEquals(2, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
        assertEquals("name", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_ComplexNamedParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :user_id_123");
        assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("user_id_123", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_NamedParameterWithSuffix() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :id::int");
        assertEquals("SELECT * FROM users WHERE id = ?::int", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_IbatisNamedParameterWithSuffix() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = #{id}::int");
        assertEquals("SELECT * FROM users WHERE id = ?::int", parsed.parameterizedSql());
        assertEquals(1, parsed.parameterCount());
        assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_MultipleTrailingSemicolons() {
        // Bug: previously only a single trailing ';' was stripped.
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users;;");
        assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        assertFalse(parsed.parameterizedSql().endsWith(";"));
    }

    @Test
    public void testParse_TrailingSemicolonsWithWhitespace() {
        // Mixed semicolons and whitespace at the end should all be stripped.
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users ;  ;  ");
        assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        assertFalse(parsed.parameterizedSql().endsWith(";"));
        assertFalse(parsed.parameterizedSql().endsWith(" "));
    }

    @Test
    public void testParse_TrailingSemicolonsPreservedInsideSql() {
        // Semicolons inside string literals must NOT be stripped — only trailing ones.
        ParsedSql parsed = ParsedSql.parse("SELECT ';' AS sep FROM dual;;");
        assertEquals("SELECT ';' AS sep FROM dual", parsed.parameterizedSql());
    }

    @Test
    public void testParse_ParameterOrderingWithRepeats() {
        // Repeated named parameters preserve order and duplicates.
        ParsedSql parsed = ParsedSql.parse("SELECT :b, :a, :b FROM x");
        assertEquals(3, parsed.parameterCount());
        assertEquals(3, parsed.namedParameters().size());
        assertEquals("b", parsed.namedParameters().get(0));
        assertEquals("a", parsed.namedParameters().get(1));
        assertEquals("b", parsed.namedParameters().get(2));
    }

    @Test
    public void testParse_simpleSelect() {
        String sql = "SELECT * FROM users";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("SELECT * FROM users", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
        Assertions.assertEquals(0, parsed.parameterCount());
    }

    @Test
    public void testParse_withQuestionMarkParameters() {
        String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testParse_withNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :userStatus";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId AND status = :userStatus", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
        Assertions.assertEquals("userStatus", namedParams.get(1));
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testParse_withIBatisStyleParameters() {
        String sql = "INSERT INTO users (name, email) VALUES (#{userName}, #{userEmail})";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("INSERT INTO users (name, email) VALUES (#{userName}, #{userEmail})", parsed.originalSql());
        Assertions.assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", parsed.parameterizedSql());

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userName", namedParams.get(0));
        Assertions.assertEquals("userEmail", namedParams.get(1));
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testParse_withMixedParameters_throwsException() {
        String sql = "SELECT * FROM users WHERE id = ? AND name = :userName";

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse(sql);
        });
    }

    @Test
    public void testParse_withTrailingSemicolon() {
        String sql = "SELECT * FROM users WHERE id = :userId;";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId;", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
    }

    @Test
    public void testParse_withWhitespace() {
        String sql = "  SELECT * FROM users WHERE id = :userId  ";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("SELECT * FROM users WHERE id = :userId", parsed.originalSql());
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
    }

    @Test
    public void testParse_withComments() {
        String sql = "-- This is a comment\nSELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withMultilineComments() {
        String sql = "/* This is a\nmultiline comment */\nSELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withHashComments() {
        String sql = "SELECT * FROM users WHERE id = :userId # ignore :fake ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withHashCommentsWithoutSpace() {
        String sql = "SELECT * FROM users WHERE id = :userId#ignore :fake ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withHashCommentAtLineStartWithoutSpace() {
        String sql = "#ignore :fake ?\nSELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertNotNull(parsed);
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testParse_withIbatisParameterOptions() {
        String sql = "INSERT INTO users (id) VALUES (#{id,jdbcType=INTEGER})";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("INSERT INTO users (id) VALUES (?)", parsed.parameterizedSql());
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("id", namedParams.get(0));
    }

    @Test
    public void testParse_nonOperationSql() {
        String sql = "SET @variable = :value";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("SET @variable = :value", parsed.originalSql());
        Assertions.assertEquals("SET @variable = :value", parsed.parameterizedSql());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
        Assertions.assertEquals(0, parsed.parameterCount());
    }

    @Test
    public void testParse_withClause() {
        String sql = "WITH cte AS (SELECT * FROM users WHERE status = :status) SELECT * FROM cte WHERE id = :id";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("status", namedParams.get(0));
        Assertions.assertEquals("id", namedParams.get(1));
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testParse_parenthesizedSelectPrefix() {
        String sql = "(SELECT :id)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("(SELECT ?)", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_updateStatement() {
        String sql = "UPDATE users SET name = :name, email = :email WHERE id = :id";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("UPDATE users SET name = ?, email = ? WHERE id = ?", parsed.parameterizedSql());
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(3, namedParams.size());
        Assertions.assertEquals("name", namedParams.get(0));
        Assertions.assertEquals("email", namedParams.get(1));
        Assertions.assertEquals("id", namedParams.get(2));
    }

    @Test
    public void testParse_deleteStatement() {
        String sql = "DELETE FROM users WHERE id = :id AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("DELETE FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("id", namedParams.get(0));
        Assertions.assertEquals("status", namedParams.get(1));
    }

    @Test
    public void testParse_mergeStatement() {
        String sql = "MERGE INTO users USING temp ON users.id = :id WHEN MATCHED THEN UPDATE SET name = :name";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("id", namedParams.get(0));
        Assertions.assertEquals("name", namedParams.get(1));
    }

    @Test
    public void testParse_callStatement() {
        String sql = "CALL refresh_user(:userId, :mode)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("CALL refresh_user(?, ?)", parsed.parameterizedSql());
        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
        Assertions.assertEquals("mode", namedParams.get(1));
    }

    @Test
    public void testParse_nullSql_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse(null);
        });
    }

    @Test
    public void testParse_emptySql_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("");
        });
    }

    @Test
    public void testParse_blankSql_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("   \t"));
    }

    @Test
    public void testParse_strippedBlockCommentKeepsTokenBoundary() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :id/*comment*/AND status = :status");

        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());
        Assertions.assertEquals(Arrays.asList("id", "status"), parsed.namedParameters());
    }

    @Test
    public void testParse_cacheReuse() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed1 = ParsedSql.parse(sql);
        ParsedSql parsed2 = ParsedSql.parse(sql);

        // Should return the same cached instance
        Assertions.assertSame(parsed1, parsed2);
    }

    @Test
    public void testGetParameterizedSql_withNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        String parameterizedSql = parsed.parameterizedSql();
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterizedSql);
    }

    @Test
    public void testGetParameterizedSql_withQuestionMarks() {
        String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        String parameterizedSql = parsed.parameterizedSql();
        Assertions.assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterizedSql);
    }

    @Test
    public void testGetParameterizedSql_withSingleNamedParameter() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        String parameterizedSql = parsed.parameterizedSql();
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parameterizedSql);
    }

    @Test
    public void testParameterizedSql_cacheReuse() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        // First call should trigger parsing
        String parameterizedSql1 = parsed.parameterizedSql();
        // Second call should use cached result
        String parameterizedSql2 = parsed.parameterizedSql();

        Assertions.assertEquals(parameterizedSql1, parameterizedSql2);
    }

    @Test
    public void testGetNamedParameters() {
        String sql = "SELECT * FROM users WHERE age > :minAge AND age < :maxAge";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("minAge", params.get(0));
        Assertions.assertEquals("maxAge", params.get(1));
    }

    @Test
    public void testGetNamedParameters_withNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(2, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
        Assertions.assertEquals("status", namedParams.get(1));
    }

    @Test
    public void testGetNamedParameters_withQuestionMarks() {
        String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertTrue(namedParams.isEmpty());
    }

    @Test
    public void testGetNamedParameters_withSingleNamedParameter() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertEquals(1, namedParams.size());
        Assertions.assertEquals("userId", namedParams.get(0));
    }

    @Test
    public void testGetParameterCount() {
        String sql = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals(3, parsed.parameterCount());
    }

    @Test
    public void testGetParameterCount_withNamedParameters() {
        String sql = "INSERT INTO users (name, email) VALUES (:name, :email)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testGetParameterCount_withNamedParametersInSelect() {
        String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
        ParsedSql parsed = ParsedSql.parse(sql);

        int parameterCount = parsed.parameterCount();
        Assertions.assertEquals(2, parameterCount);
    }

    @Test
    public void testGetParameterCount_withSingleNamedParameterInSelect() {
        String sql = "SELECT * FROM users WHERE id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        int parameterCount = parsed.parameterCount();
        Assertions.assertEquals(1, parameterCount);
    }

    @Test
    public void testEquals() {
        String sql1 = "SELECT * FROM users WHERE id = :userId";
        String sql2 = "SELECT * FROM users WHERE id = :userId";
        String sql3 = "SELECT * FROM users WHERE id = :id";

        ParsedSql parsed1 = ParsedSql.parse(sql1);
        ParsedSql parsed2 = ParsedSql.parse(sql2);
        ParsedSql parsed3 = ParsedSql.parse(sql3);

        // Test equals with same object
        Assertions.assertEquals(parsed1, parsed1);

        // Test equals with equivalent object
        Assertions.assertEquals(parsed1, parsed2);

        // Test not equals with different SQL
        Assertions.assertNotEquals(parsed1, parsed3);

        // Test not equals with null
        Assertions.assertNotEquals(parsed1, null);

        // Test not equals with different type
        Assertions.assertNotEquals(parsed1, "SELECT * FROM users WHERE id = :userId");
    }

    @Test
    public void testComplexNamedParameters() {
        String sql = "SELECT * FROM users WHERE user_id = :user_id AND user_name123 = :user_name123";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("user_id", params.get(0));
        Assertions.assertEquals("user_name123", params.get(1));
    }

    @Test
    public void testParameterWithSpecialCharacters() {
        // Test that colons not followed by valid parameter characters are not treated as parameters
        String sql = "SELECT * FROM users WHERE time > '10:30:00' AND id = :userId";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("userId", params.get(0));
        Assertions.assertEquals(1, parsed.parameterCount());
    }

    @Test
    public void testMultipleSameNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = :id OR parent_id = :id";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("id", params.get(0));
        Assertions.assertEquals("id", params.get(1));
        Assertions.assertEquals(2, parsed.parameterCount());
    }

    @Test
    public void testNonOperationSql() {
        String sql = "SET @variable = :value";
        ParsedSql parsed = ParsedSql.parse(sql);

        String parameterizedSql = parsed.parameterizedSql();
        Assertions.assertEquals("SET @variable = :value", parameterizedSql);
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
        Assertions.assertEquals(0, parsed.parameterCount());
    }

    @Test
    public void testInsertStatement() {
        String sql = "INSERT INTO users (id, name, email) VALUES (:id, :name, :email)";
        ParsedSql parsed = ParsedSql.parse(sql);

        Assertions.assertEquals("INSERT INTO users (id, name, email) VALUES (?, ?, ?)", parsed.parameterizedSql());
        List<String> params = parsed.namedParameters();
        Assertions.assertEquals(3, params.size());
        Assertions.assertEquals("id", params.get(0));
        Assertions.assertEquals("name", params.get(1));
        Assertions.assertEquals("email", params.get(2));
    }

    @Test
    public void testMixedParameterStyles_iBatisAndNamed_throwsException() {
        String sql = "SELECT * FROM users WHERE id = #{userId} AND status = :status";

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse(sql);
        });
    }

    @Test
    public void testDollarNumericParameters_areNotNamedParameters() {
        String sql = "SELECT * FROM users WHERE id = $1 AND status = $2";
        ParsedSql parsed = ParsedSql.parse(sql);

        List<String> namedParams = parsed.namedParameters();
        Assertions.assertTrue(namedParams.isEmpty());
    }

    @Test
    public void testParse_malformedIbatisParameter_missingClosingBracket_throwsException() {
        // Bug fix: #{param without closing } should throw, not silently corrupt SQL
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("INSERT INTO users (id) VALUES (#{userId)");
        });
    }

    @Test
    public void testParse_malformedIbatisParameter_missingClosingBracketAtEnd_throwsException() {
        // #{param at end of SQL without closing }
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("SELECT * FROM users WHERE id = #{userId");
        });
    }

    @Test
    public void testParse_malformedIbatisParameter_withTrailingTokensConsumed_throwsException() {
        // Bug fix: the while-loop was consuming all remaining tokens when } was missing,
        // which silently lost subsequent parameters and corrupted SQL structure
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParsedSql.parse("INSERT INTO t VALUES (#{id}, #{name)");
        });
    }

    @Test
    public void testParse_validIbatisParameter_stillWorks() {
        // Ensure the fix doesn't break valid iBatis parameters
        ParsedSql parsed = ParsedSql.parse("INSERT INTO t (a, b) VALUES (#{id}, #{name})");
        Assertions.assertEquals("INSERT INTO t (a, b) VALUES (?, ?)", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals("id", parsed.namedParameters().get(0));
        Assertions.assertEquals("name", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_validIbatisParameterWithOptions_stillWorks() {
        // Ensure the fix doesn't break valid iBatis parameters with options like jdbcType
        ParsedSql parsed = ParsedSql.parse("INSERT INTO t (a) VALUES (#{id,jdbcType=INTEGER})");
        Assertions.assertEquals("INSERT INTO t (a) VALUES (?)", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParsedSql_classLevelExample() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
        String parameterized = parsed.parameterizedSql();
        List<String> params = parsed.namedParameters();

        assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterized);
        assertEquals(Arrays.asList("userId", "status"), params);
    }

    @Test
    public void testParsedSql_parseNamedParameters() {
        ParsedSql ps1 = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        assertEquals("SELECT * FROM users WHERE id = ?", ps1.parameterizedSql());
    }

    @Test
    public void testParsedSql_parseIBatisStyle() {
        ParsedSql ps2 = ParsedSql.parse("INSERT INTO users (name, email) VALUES (#{name}, #{email})");
        assertEquals(Arrays.asList("name", "email"), ps2.namedParameters());
    }

    @Test
    public void testParsedSql_parseJdbcPlaceholders() {
        ParsedSql ps3 = ParsedSql.parse("UPDATE users SET status = ? WHERE id = ?");
        assertEquals(2, ps3.parameterCount());
    }

    @Test
    public void testParsedSql_sql() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        String original = parsed.originalSql();
        assertEquals("SELECT * FROM users WHERE id = :userId", original);
    }

    @Test
    public void testParsedSql_parameterizedSql() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
        String sql = parsed.parameterizedSql();
        assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", sql);
    }

    @Test
    public void testParsedSql_parameterizedSqlForJdbc() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :name");
        String jdbcSql = parsed.parameterizedSql();
        assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", jdbcSql);
    }

    @Test
    public void testParsedSql_namedParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");
        ImmutableList<String> params = parsed.namedParameters();
        assertEquals(Arrays.asList("name", "minAge"), params);

        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        ImmutableList<String> params2 = parsed2.namedParameters();
        assertTrue(params2.isEmpty());
    }

    @Test
    public void testParsedSql_parameterCount() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, email, age) VALUES (:name, :email, :age)");
        int count = parsed.parameterCount();
        assertEquals(3, count);

        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
        int count2 = parsed2.parameterCount();
        assertEquals(0, count2);
    }

    @Test
    public void testParse_EmptyIbatisTokenRemainsLiteral() {
        ParsedSql parsed = ParsedSql.parse("SELECT #{} FROM dual");

        Assertions.assertEquals("SELECT #{} FROM dual", parsed.parameterizedSql());
        Assertions.assertEquals(0, parsed.parameterCount());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_BlankIbatisTokenRemainsLiteral() {
        ParsedSql parsed = ParsedSql.parse("SELECT #{   } FROM dual");

        Assertions.assertEquals("SELECT #{ } FROM dual", parsed.parameterizedSql());
        Assertions.assertEquals(0, parsed.parameterCount());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_CommentOnlySqlHasNoOperationPrefix() {
        ParsedSql parsed = ParsedSql.parse("/* comment only */");

        Assertions.assertEquals("/* comment only */", parsed.parameterizedSql());
        Assertions.assertEquals(0, parsed.parameterCount());
    }

    @Test
    public void testParse_ParenthesizedCommentOnlySqlHasNoOperationPrefix() {
        ParsedSql parsed = ParsedSql.parse("(/* comment only */)");

        Assertions.assertEquals("(/* comment only */)", parsed.parameterizedSql());
        Assertions.assertEquals(0, parsed.parameterCount());
    }

    @Test
    public void testParse_IbatisParameterTokenAssemblyStopsAtFirstClosingBrace() {
        // Verifies the loop stops at the FIRST '}', not the last character being '}'
        ParsedSql parsed = ParsedSql.parse("INSERT INTO t VALUES (#{a},#{b})");
        Assertions.assertEquals(2, parsed.parameterCount(), "Two iBatis parameters should be detected");
        Assertions.assertEquals("a", parsed.namedParameters().get(0));
        Assertions.assertEquals("b", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_SimpleIbatisInsertProducesPlaceholders() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO t (id) VALUES (#{id})");
        Assertions.assertEquals("INSERT INTO t (id) VALUES (?)", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals("id", parsed.namedParameters().get(0));
    }

    // --- Bug fix: "#{ id }" (whitespace immediately after the "#{" opener) must bind as a
    // parameter. The tokenizer emits a standalone 2-char "#{" token in that case; the old
    // "length >= 3" gate skipped the marker-assembly loop and left the marker as literal text. ---

    @Test
    public void testParse_IbatisParameterWithWhitespaceAfterOpener_binds() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = #{ id }");
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals(Arrays.asList("id"), parsed.namedParameters());
    }

    @Test
    public void testParse_IbatisParameterWithLeadingWhitespaceOnly_binds() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = #{ id}");
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals(Arrays.asList("id"), parsed.namedParameters());
    }

    @Test
    public void testParse_LimitOffsetPaddedIbatisMarkers_bindBoth() {
        // Limit.toString() renders "LIMIT #{ maxRows } OFFSET #{ startRow }" — both padded
        // markers must bind instead of surviving as literal SQL text.
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM t LIMIT #{ maxRows } OFFSET #{ startRow }");
        Assertions.assertEquals("SELECT * FROM t LIMIT ? OFFSET ?", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals(Arrays.asList("maxRows", "startRow"), parsed.namedParameters());
    }

    @Test
    public void testParse_DanglingIbatisOpenerAtEnd_throws() {
        // A standalone "#{" with no closing '}' anywhere now fails fast with the documented
        // IllegalArgumentException instead of silently passing through as literal text.
        Assertions.assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT #{"));
    }

    // --- Bug fix: trailing-semicolon strip must also remove residual whitespace before the ';' ---

    @Test
    public void testParse_TrailingSemicolon_WithWhitespaceBefore_StripsBoth() {
        // The body had whitespace before the trailing ';' — that whitespace must NOT leak
        // into the parameterized SQL after the ';' is removed.
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users   ;");
        Assertions.assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        Assertions.assertFalse(parsed.parameterizedSql().endsWith(" "), "parameterizedSql must not end with whitespace after ';' removal");
    }

    @Test
    public void testParse_TrailingSemicolonAndWhitespace_NamedParam() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId   ;");
        Assertions.assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
    }

    // --- Bug fix: parameterCount must be safely published via the static cache (now final field) ---

    // Bug fix: when SqlParser produces a single token containing multiple "#{...}" iBatis
    // markers (e.g. "#{a}#{b}" with no separator between), the previous code extracted only
    // the first marker and silently embedded later "#{...}" markers into the parameterized
    // SQL as literal text, losing those parameter bindings entirely.
    @Test
    public void testParse_AdjacentIbatisTokens_extractsAllParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT #{a}#{b} FROM dual");
        Assertions.assertEquals("SELECT ?? FROM dual", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals("a", parsed.namedParameters().get(0));
        Assertions.assertEquals("b", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_ThreeAdjacentIbatisTokens_extractsAllParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT #{a}#{b}#{c} FROM dual");
        Assertions.assertEquals("SELECT ??? FROM dual", parsed.parameterizedSql());
        Assertions.assertEquals(3, parsed.parameterCount());
        Assertions.assertEquals("a", parsed.namedParameters().get(0));
        Assertions.assertEquals("b", parsed.namedParameters().get(1));
        Assertions.assertEquals("c", parsed.namedParameters().get(2));
    }

    @Test
    public void testParse_AdjacentIbatisTokenAfterValid_doesNotInfiniteLoop() {
        // Make sure the empty / literal "#{}" doesn't cause an infinite loop after a valid one.
        ParsedSql parsed = ParsedSql.parse("SELECT #{a}#{} FROM dual");
        Assertions.assertEquals("SELECT ?#{} FROM dual", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals("a", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_EmptyIbatisTokenBeforeValid_extractsTrailingParameter() {
        // Bug fix: a literal "#{}" followed by a real "#{a}" marker inside the SAME token
        // (e.g. "#{}#{a}") previously broke out of the marker loop and silently embedded
        // "#{a}" as literal text, losing the parameter binding entirely.
        ParsedSql parsed = ParsedSql.parse("SELECT #{}#{a} FROM dual");
        Assertions.assertEquals("SELECT #{}? FROM dual", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals(1, parsed.namedParameters().size());
        Assertions.assertEquals("a", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_BlankIbatisTokenBetweenValid_extractsAllParameters() {
        // "#{a}#{ }#{b}" : the blank/empty marker in the middle must not abort extraction
        // of the trailing "#{b}" parameter.
        ParsedSql parsed = ParsedSql.parse("SELECT #{a}#{ }#{b} FROM dual");
        Assertions.assertEquals("SELECT ?#{ }? FROM dual", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals(2, parsed.namedParameters().size());
        Assertions.assertEquals("a", parsed.namedParameters().get(0));
        Assertions.assertEquals("b", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_IbatisMarkersSeparatedByLiteralText_extractsAllParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT pre#{a}mid#{b}post FROM dual");
        Assertions.assertEquals("SELECT pre?mid?post FROM dual", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals(Arrays.asList("a", "b"), parsed.namedParameters());
    }

    @Test
    public void testParse_IbatisMarkerInsideQuotedToken_remainsLiteral() {
        ParsedSql parsed = ParsedSql.parse("SELECT '#{ignored}', pre#{actual}post FROM dual");
        Assertions.assertEquals("SELECT '#{ignored}', pre?post FROM dual", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals(Arrays.asList("actual"), parsed.namedParameters());
    }

    @Test
    public void testParse_WhitespaceVariantsShareCanonicalCacheEntry() {
        ParsedSql canonical = ParsedSql.parse("SELECT * FROM canonical_cache_test");
        ParsedSql padded = ParsedSql.parse("  SELECT * FROM canonical_cache_test  ");

        Assertions.assertSame(canonical, padded);
    }

    // Bug fix: when SqlParser produces a single token containing multiple ":named"
    // markers (e.g. ":a:b" with no separator between, since ':' is not a token separator),
    // the previous code extracted only the leading marker and silently embedded later
    // ":named" markers into the parameterized SQL as literal text, losing those parameter
    // bindings entirely. This mirrors the analogous "#{a}#{b}" iBatis fix.
    @Test
    public void testParse_AdjacentNamedParameters_extractsAllParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a=:a:b");
        Assertions.assertEquals("SELECT * FROM t WHERE a=??", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals(2, parsed.namedParameters().size());
        Assertions.assertEquals("a", parsed.namedParameters().get(0));
        Assertions.assertEquals("b", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_ThreeAdjacentNamedParameters_extractsAllParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a=:a:b:c");
        Assertions.assertEquals("SELECT * FROM t WHERE a=???", parsed.parameterizedSql());
        Assertions.assertEquals(3, parsed.parameterCount());
        Assertions.assertEquals(3, parsed.namedParameters().size());
        Assertions.assertEquals("a", parsed.namedParameters().get(0));
        Assertions.assertEquals("b", parsed.namedParameters().get(1));
        Assertions.assertEquals("c", parsed.namedParameters().get(2));
    }

    @Test
    public void testParse_NamedParameterFollowedByPostgresCast_preservesCast() {
        // Regression guard: the multi-marker loop must NOT mistake the "::" of a PostgreSQL
        // cast for a second ":named" marker. ":id::int" -> extract "id", keep "::int" verbatim.
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE id = :id::int");
        Assertions.assertEquals("SELECT * FROM t WHERE id = ?::int", parsed.parameterizedSql());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertEquals(1, parsed.namedParameters().size());
        Assertions.assertEquals("id", parsed.namedParameters().get(0));
    }

    @Test
    public void testParse_AdjacentNamedParametersWithCast_extractsBothAndPreservesCast() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a=:a:b::int");
        Assertions.assertEquals("SELECT * FROM t WHERE a=??::int", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals("a", parsed.namedParameters().get(0));
        Assertions.assertEquals("b", parsed.namedParameters().get(1));
    }

    @Test
    public void testParse_UnicodeIdentifierNamesAndDottedPaths() {
        final String supplementaryLetter = new String(Character.toChars(0x10400));
        final ParsedSql parsed = ParsedSql.parse(
                "SELECT :用户.地址_2, :na\u0301me, :" + supplementaryLetter + "Value FROM users");

        Assertions.assertEquals("SELECT ?, ?, ? FROM users", parsed.parameterizedSql());
        Assertions.assertEquals(Arrays.asList("用户.地址_2", "na\u0301me", supplementaryLetter + "Value"), parsed.namedParameters());
        Assertions.assertEquals(3, parsed.parameterCount());
    }

    @Test
    public void testParse_InvalidIdentifierStartsAndUnicodePunctuationAreNotSwallowed() {
        final ParsedSql invalidStarts = ParsedSql.parse("SELECT :1name, :\u0301name, :\u2014name, :\u2003name FROM users");

        Assertions.assertEquals(0, invalidStarts.parameterCount());
        Assertions.assertTrue(invalidStarts.namedParameters().isEmpty());
        Assertions.assertTrue(invalidStarts.parameterizedSql().contains(":1name"));
        Assertions.assertTrue(invalidStarts.parameterizedSql().contains(":\u2014name"));

        final ParsedSql delimited = ParsedSql.parse("SELECT :name\u2014suffix, :user..address, :user.2address FROM users");
        Assertions.assertEquals("SELECT ?\u2014suffix, ?..address, ?.2address FROM users", delimited.parameterizedSql());
        Assertions.assertEquals(Arrays.asList("name", "user", "user"), delimited.namedParameters());
    }

    @Test
    public void testParse_UnpairedSurrogatesInProspectiveNamedParametersAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :\uD800 FROM users"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :\uDC00 FROM users"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :valid\uD800 FROM users"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT arr[:\uD800] FROM users"));
    }

    @Test
    public void testParse_CachedInstance_ReturnsCorrectParameterCount() {
        // First parse populates the cache; second parse hits the unsynchronized fast path.
        // Both observations of parameterCount must agree (regression guard for unsafe publication).
        String sql = "SELECT * FROM users WHERE a = :a AND b = :b AND c = :c";
        ParsedSql first = ParsedSql.parse(sql);
        ParsedSql second = ParsedSql.parse(sql);
        Assertions.assertSame(first, second, "ParsedSql.parse should return the cached instance");
        Assertions.assertEquals(3, first.parameterCount());
        Assertions.assertEquals(3, second.parameterCount());
    }

    @Test
    public void testParse_PostgresJsonQuestionOperatorDoesNotCountAsJdbcParameter() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? 'active'");
        Assertions.assertEquals("SELECT * FROM events WHERE payload ? 'active'", parsed.parameterizedSql());
        Assertions.assertEquals(0, parsed.parameterCount());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_PostgresJsonQuestionOperatorWithNamedParameterRhs() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? :key AND id = :id");
        Assertions.assertEquals("SELECT * FROM events WHERE payload ? ? AND id = ?", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals(Arrays.asList("key", "id"), parsed.namedParameters());
    }

    @Test
    public void testParse_PostgresJsonQuestionOperatorWithFunctionRhs() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? lower(:key) AND id = :id");
        Assertions.assertEquals("SELECT * FROM events WHERE payload ? lower(?) AND id = ?", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals(Arrays.asList("key", "id"), parsed.namedParameters());

        parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? CAST(:key AS text) AND id = :id");
        Assertions.assertEquals("SELECT * FROM events WHERE payload ? CAST(? AS text) AND id = ?", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals(Arrays.asList("key", "id"), parsed.namedParameters());
    }

    @Test
    public void testParse_PostgresJsonQuestionAndOperatorWithNamedParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ?& array[:keys] AND id = :id");
        Assertions.assertEquals("SELECT * FROM events WHERE payload ?& array[?] AND id = ?", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertEquals(Arrays.asList("keys", "id"), parsed.namedParameters());
    }

    @Test
    public void testParse_PostgresJsonQuestionOperatorWithJdbcParameterRhs() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? ? AND tenant_id = ?");
        Assertions.assertEquals("SELECT * FROM events WHERE payload ? ? AND tenant_id = ?", parsed.parameterizedSql());
        Assertions.assertEquals(2, parsed.parameterCount());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_PositionalPaginationAndOrderingParametersAreNotJsonOperators() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM events ORDER BY ? ASC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        Assertions.assertEquals(3, parsed.parameterCount());
        Assertions.assertTrue(parsed.namedParameters().isEmpty());
    }

    @Test
    public void testParse_PlaceholderAfterOperatorOrValueTakingKeywordIsNotJsonOperator() {
        // Regression: a "?" preceded by a value-taking keyword (INTERVAL, ILIKE, SIMILAR TO, ...) or by a
        // multi-character operator ("||", "->>") and followed by an identifier-like word (DAY, ESCAPE, an
        // alias) was misclassified as the PostgreSQL JSON "?" operator and dropped from parameterCount().
        ParsedSql mysql = ParsedSql.parse("SELECT * FROM t WHERE d > DATE_SUB(NOW(), INTERVAL ? DAY) AND id = ?");
        assertEquals("SELECT * FROM t WHERE d > DATE_SUB(NOW(), INTERVAL ? DAY) AND id = ?", mysql.parameterizedSql());
        assertEquals(2, mysql.parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE d > NOW() - INTERVAL ? DAY").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE name ILIKE ? ESCAPE '!'").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE name ILIKE ? COLLATE \"C\"").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE name SIMILAR TO ? ESCAPE '!'").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT a || ? b FROM t").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT payload ->> ? val FROM t").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT ts AT TIME ZONE ? local_ts FROM t").parameterCount());

        // A genuine JSON existence operator is still not a parameter.
        assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE payload ? 'active'").parameterCount());
        assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE p1 ? day").parameterCount());
        assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE x::jsonb ? 'k'").parameterCount());
    }

    @Test
    public void testParse_PositionalPlaceholderInsideArraySubscriptIsCounted() {
        // Regression: the tokenizer glues "[...]" to the preceding identifier, so the "?" inside
        // "ARRAY[?]" never surfaced as its own token and was silently dropped from parameterCount(),
        // although the named forms "ARRAY[:ids]" / "ARRAY[#{ids}]" in the same position were bound.
        ParsedSql unspaced = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?] AND id = ?");
        assertEquals("SELECT * FROM t WHERE tags @> ARRAY[?] AND id = ?", unspaced.parameterizedSql());
        assertEquals(2, unspaced.parameterCount());
        assertTrue(unspaced.namedParameters().isEmpty());

        ParsedSql spaced = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY [?, ?]");
        assertEquals(2, spaced.parameterCount());

        assertEquals(2, ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?::text, ?::text]").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT arr[?] FROM t").parameterCount());

        // A '?' inside a quoted literal within the subscript is not a placeholder.
        assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY['a?', ?]").parameterCount());

        // Bracket-quoted identifiers that merely contain '?' are preserved.
        assertEquals(1, ParsedSql.parse("SELECT [what?] FROM t WHERE id = ?").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT [?foo] FROM t WHERE id = ?").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT t.[what?] FROM t WHERE id = ?").parameterCount());

        // A positional marker in a subscript still participates in the mixed-style guard.
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?] AND name = :n"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[#{a}, ?]"));
    }

    @Test
    public void testParse_NamedParameterInsidePreservedBlockCommentIsIgnored() {
        final ParsedSql parsed = ParsedSql.parse("-- Keep comments\nSELECT /* audit :ignored */ :actual FROM users");

        Assertions.assertEquals(Arrays.asList("actual"), parsed.namedParameters());
        Assertions.assertEquals(1, parsed.parameterCount());
        Assertions.assertTrue(parsed.parameterizedSql().contains("/* audit :ignored */"));
        Assertions.assertTrue(parsed.parameterizedSql().contains("SELECT"));
        Assertions.assertTrue(parsed.parameterizedSql().contains("? FROM users"));
    }

    @Test
    public void testParse_NamedAndIbatisMarkersInsideSubscriptWithLiteralAreBound() {
        // The tokenizer keeps "ARRAY['a', :id]" as ONE token; a quoted literal inside the subscript must not
        // make the named/iBatis scanners skip the whole token (the positional scanner already handled it).
        ParsedSql named = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY['a', :id]");
        assertEquals("SELECT * FROM t WHERE tags @> ARRAY['a', ?]", named.parameterizedSql());
        assertEquals(1, named.parameterCount());
        assertEquals(List.of("id"), named.namedParameters());

        ParsedSql namedFirst = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[:id, 'a'] AND id = :id2");
        assertEquals("SELECT * FROM t WHERE tags @> ARRAY[?, 'a'] AND id = ?", namedFirst.parameterizedSql());
        assertEquals(List.of("id", "id2"), namedFirst.namedParameters());

        ParsedSql ibatis = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY['a', #{id}]");
        assertEquals("SELECT * FROM t WHERE tags @> ARRAY['a', ?]", ibatis.parameterizedSql());
        assertEquals(List.of("id"), ibatis.namedParameters());

        // literal content inside the subscript is still not a marker
        ParsedSql literalOnly = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[':notparam', '#{nope}']");
        assertEquals("SELECT * FROM t WHERE tags @> ARRAY[':notparam', '#{nope}']", literalOnly.parameterizedSql());
        assertEquals(0, literalOnly.parameterCount());

        // the mixed-style guard must see the marker
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ? FROM t WHERE tags @> ARRAY['a', :id]"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE arr = ARRAY[:a, 'x'] AND id = ?"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE arr = ARRAY['x', #{a}] AND id = :b"));
    }

    @Test
    public void testParse_StandaloneSubscriptWithInnerWhitespaceBindsNamedParameter() {
        // "[ :ids ]" binds exactly like "[ ? ]" does; the whitespace after '[' is skipped for both styles.
        ParsedSql spacedInner = ParsedSql.parse("SELECT * FROM t WHERE arr = array [ :ids ]");
        assertEquals("SELECT * FROM t WHERE arr = array [ ? ]", spacedInner.parameterizedSql());
        assertEquals(1, spacedInner.parameterCount());
        assertEquals(List.of("ids"), spacedInner.namedParameters());

        // a standalone "[#{...}]" stays a bracket-quoted identifier (pinned by
        // testParse_BracketQuotedIdentifiersDoNotCreateParameters), with or without inner whitespace
        ParsedSql ibatisSpaced = ParsedSql.parse("SELECT * FROM t WHERE arr = array [ #{ids} ]");
        assertEquals("SELECT * FROM t WHERE arr = array [ #{ids} ]", ibatisSpaced.parameterizedSql());
        assertEquals(0, ibatisSpaced.parameterCount());
    }

    @Test
    public void testParse_ChainedSubscriptIsInspectedLikeTheFirstGroup() {
        // Regression: the tokenizer emits the second bracket group of "x['a', :b]['c', :d]" as a standalone
        // literal-first "[...]" token, which used to be treated as a bracket-quoted identifier, so :d was
        // left verbatim and "x[?]['c', ?]" emitted both '?' with parameterCount() == 1. The same holds when
        // the groups are separated by whitespace or a comment, which does not break the subscript chain.
        ParsedSql named = ParsedSql.parse("SELECT x['a', :b]['c', :d] FROM t");
        assertEquals("SELECT x['a', ?]['c', ?] FROM t", named.parameterizedSql());
        assertEquals(2, named.parameterCount());
        assertEquals(List.of("b", "d"), named.namedParameters());

        ParsedSql positional = ParsedSql.parse("SELECT x[?]['c', ?] FROM t");
        assertEquals("SELECT x[?]['c', ?] FROM t", positional.parameterizedSql());
        assertEquals(2, positional.parameterCount());

        ParsedSql ibatis = ParsedSql.parse("SELECT x[#{a}]['c', #{b}] FROM t");
        assertEquals("SELECT x[?]['c', ?] FROM t", ibatis.parameterizedSql());
        assertEquals(2, ibatis.parameterCount());
        assertEquals(List.of("a", "b"), ibatis.namedParameters());

        // a literal-only first group still chains
        ParsedSql literalFirst = ParsedSql.parse("SELECT x[1]['c', :d] FROM t");
        assertEquals("SELECT x[1]['c', ?] FROM t", literalFirst.parameterizedSql());
        assertEquals(List.of("d"), literalFirst.namedParameters());

        // already-working shapes are unchanged
        assertEquals(3, ParsedSql.parse("SELECT arr[?][?][?] FROM t").parameterCount());
        assertEquals(List.of("a", "b"), ParsedSql.parse("SELECT x[:a][:b] FROM t").namedParameters());

        // literal content inside a chained group is never a parameter
        ParsedSql literalOnly = ParsedSql.parse("SELECT x[1][':nope', '#{nope}', '?'] FROM t");
        assertEquals("SELECT x[1][':nope', '#{nope}', '?'] FROM t", literalOnly.parameterizedSql());
        assertEquals(0, literalOnly.parameterCount());

        // Whitespace and comments between the groups do NOT break the chain: "x[?] ['c', ?]" is the same
        // PostgreSQL expression as "x[?]['c', ?]", so both placeholders are counted and parameterCount()
        // matches the number of '?' actually emitted into parameterizedSql().
        ParsedSql spacedChain = ParsedSql.parse("SELECT x[?] ['c', ?] FROM t");
        assertEquals("SELECT x[?] ['c', ?] FROM t", spacedChain.parameterizedSql());
        assertEquals(2, spacedChain.parameterCount());
        assertEquals(2, spacedChain.parameterizedSql().chars().filter(c -> c == '?').count());

        ParsedSql spacedNamedChain = ParsedSql.parse("SELECT x[:a] ['c', :b] FROM t");
        assertEquals("SELECT x[?] ['c', ?] FROM t", spacedNamedChain.parameterizedSql());
        assertEquals(List.of("a", "b"), spacedNamedChain.namedParameters());

        // A comment between the groups behaves like whitespace.
        assertEquals(2, ParsedSql.parse("SELECT x[?] /* c */ ['c', ?] FROM t").parameterCount());

        // The chain root must be a real subscript: a bracket group after a bracket-QUOTED IDENTIFIER is a
        // SQL Server column with a bracketed alias, so it keeps the standalone reading and stays literal.
        ParsedSql bracketedAlias = ParsedSql.parse("SELECT [a] [b:c] FROM t WHERE id = :id");
        assertEquals("SELECT [a] [b:c] FROM t WHERE id = ?", bracketedAlias.parameterizedSql());
        assertEquals(List.of("id"), bracketedAlias.namedParameters());

        ParsedSql qualifiedBracketedAlias = ParsedSql.parse("SELECT t.[a] [b:c] FROM t WHERE id = :id");
        assertEquals("SELECT t.[a] [b:c] FROM t WHERE id = ?", qualifiedBracketedAlias.parameterizedSql());
        assertEquals(List.of("id"), qualifiedBracketedAlias.namedParameters());

        assertEquals("SELECT [a],[b:c] FROM t", ParsedSql.parse("SELECT [a],[b:c] FROM t").parameterizedSql());
        assertEquals("SELECT [db].[schema].[t:col] FROM x", ParsedSql.parse("SELECT [db].[schema].[t:col] FROM x").parameterizedSql());
        assertEquals("INSERT INTO [t]([a],[b]) VALUES (?, ?)", ParsedSql.parse("INSERT INTO [t]([a],[b]) VALUES (?, ?)").parameterizedSql());
        assertEquals(2, ParsedSql.parse("INSERT INTO [t]([a],[b]) VALUES (?, ?)").parameterCount());

        // the mixed-style guard sees the chained marker
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT x[?]['c', :d] FROM t"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT x[:a]['c', #{b}] FROM t"));

        // the documented bracket-identifier exceptions are untouched
        assertEquals("SELECT t.[:name] FROM t", ParsedSql.parse("SELECT t.[:name] FROM t").parameterizedSql());
        assertEquals(0, ParsedSql.parse("SELECT t.[:name] FROM t").parameterCount());
        assertEquals("SELECT [#{ids}] FROM t", ParsedSql.parse("SELECT [#{ids}] FROM t").parameterizedSql());
        assertEquals(0, ParsedSql.parse("SELECT [#{ids}] FROM t").parameterCount());
    }

    @Test
    public void testParse_EscapedQuoteInsideSubscriptLiteralDoesNotEndTheLiteral() {
        // Where a literal inside a subscript ends depends on the dialect once it contains a backslash: under
        // MySQL "\'" is an escaped quote, under standard-conforming strings "'a\'" is a complete literal.
        // Committing to either reading corrupts the other dialect, so the scanners evaluate a subscript token
        // under BOTH readings and bind only when they agree; a token on which they disagree is left verbatim
        // (nothing bound, nothing counted) so the leftover marker fails loudly at the driver.
        ParsedSql ambiguous = ParsedSql.parse("SELECT ARRAY['it\\'s :literal', :id] FROM t");
        assertEquals("SELECT ARRAY['it\\'s :literal', :id] FROM t", ambiguous.parameterizedSql());
        assertEquals(List.of(), ambiguous.namedParameters());
        assertEquals(0, ambiguous.parameterCount());

        // A PostgreSQL escape string is the exception: that syntax exists only in PostgreSQL and always
        // processes backslash escapes, so both readings see the same literal and the marker next to it is
        // bound with the literal left intact.
        ParsedSql escaped = ParsedSql.parse("SELECT ARRAY[E'it\\'s :literal', :id] FROM t");
        assertEquals("SELECT ARRAY[E'it\\'s :literal', ?] FROM t", escaped.parameterizedSql());
        assertEquals(List.of("id"), escaped.namedParameters());
        assertEquals(1, escaped.parameterCount());

        // The prefix must be a standalone E: a name that merely ends with it is not an escape string, so
        // "code_E'it\'s ...'" stays ambiguous and verbatim.
        assertEquals(0, ParsedSql.parse("SELECT ARRAY[code_E'it\\'s :literal', :id] FROM t").parameterCount());
        assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY[e'it\\'s :literal', :id] FROM t").namedParameters());

        // Agree cases: no backslash, doubled quotes, or an escaped backslash right before the closing quote
        // (both readings: the pair is consumed / is two ordinary characters, then the quote closes).
        ParsedSql doubled = ParsedSql.parse("SELECT ARRAY['it''s :x', :id] FROM t");
        assertEquals("SELECT ARRAY['it''s :x', ?] FROM t", doubled.parameterizedSql());
        assertEquals(List.of("id"), doubled.namedParameters());
        assertEquals(1, doubled.parameterCount());

        ParsedSql trailingBackslash = ParsedSql.parse("SELECT ARRAY['a\\\\', :id] FROM t");
        assertEquals("SELECT ARRAY['a\\\\', ?] FROM t", trailingBackslash.parameterizedSql());
        assertEquals(List.of("id"), trailingBackslash.namedParameters());
        assertEquals(1, ParsedSql.parse("SELECT ARRAY['a\\\\', ?] FROM t").parameterCount());
        assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY['a\\\\', #{id}] FROM t").namedParameters());

        ParsedSql escapedBackslash = ParsedSql.parse("SELECT ARRAY[E'a\\\\b', ?] FROM t");
        assertEquals("SELECT ARRAY[E'a\\\\b', ?] FROM t", escapedBackslash.parameterizedSql());
        assertEquals(1, escapedBackslash.parameterCount());
        assertEquals(1, escapedBackslash.positionalParameterOffsets().length);
        assertEquals("SELECT ARRAY[E'a\\\\b', ?] FROM t".lastIndexOf('?'), escapedBackslash.positionalParameterOffsets()[0]);

        // Double-quoted and backtick-quoted identifiers escape by doubling only (never ambiguous).
        assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY[\"a\"\"b :x\", :id] FROM t").namedParameters());
        assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY[`a``b :x`, :id] FROM t").namedParameters());

        // Disagree cases, one per marker kind: verbatim, count 0, no names, no positional offsets.
        ParsedSql namedDisagree = ParsedSql.parse("SELECT ARRAY['\\'' , :id] FROM t");
        assertEquals("SELECT ARRAY['\\'' , :id] FROM t", namedDisagree.parameterizedSql());
        assertEquals(List.of(), namedDisagree.namedParameters());
        assertEquals(0, namedDisagree.parameterCount());

        ParsedSql ibatisDisagree = ParsedSql.parse("SELECT ARRAY['a\\'', #{id}] FROM t");
        assertEquals("SELECT ARRAY['a\\'', #{id}] FROM t", ibatisDisagree.parameterizedSql());
        assertEquals(List.of(), ibatisDisagree.namedParameters());
        assertEquals(0, ibatisDisagree.parameterCount());

        ParsedSql positionalDisagree = ParsedSql.parse("SELECT ARRAY['a\\'', ?] FROM t");
        assertEquals("SELECT ARRAY['a\\'', ?] FROM t", positionalDisagree.parameterizedSql());
        assertEquals(0, positionalDisagree.parameterCount());
        assertEquals(0, positionalDisagree.positionalParameterOffsets().length);
        assertEquals(0, ParsedSql.parse("SELECT ARRAY['a\\'?', ?] FROM t").parameterCount());
        assertEquals(0, ParsedSql.parse("SELECT ARRAY['it\\'s ?', ?, ?] FROM t").parameterCount());
        assertEquals(0, ParsedSql.parse("SELECT ARRAY['a\\'#{x}', #{id}] FROM t").parameterCount());

        // The same shapes with an escape-string prefix: the markers inside the literal stay literal and only
        // the ones next to it are counted.
        assertEquals(1, ParsedSql.parse("SELECT ARRAY[E'a\\'?', ?] FROM t").parameterCount());
        assertEquals(2, ParsedSql.parse("SELECT ARRAY[E'it\\'s ?', ?, ?] FROM t").parameterCount());
        assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY[E'a\\'#{x}', #{id}] FROM t").namedParameters());

        // A disagreeing subscript next to an ordinary top-level binding: the tokenizer closes the bracket
        // group under the backslash reading, so ":id" outside it is its own token and is bound alone while
        // the ambiguous group stays verbatim.
        ParsedSql mixed = ParsedSql.parse("SELECT ARRAY['a\\'', :x] FROM t WHERE id = :id");
        assertEquals("SELECT ARRAY['a\\'', :x] FROM t WHERE id = ?", mixed.parameterizedSql());
        assertEquals(List.of("id"), mixed.namedParameters());
        assertEquals(1, mixed.parameterCount());

        // Each group of a chained subscript is judged on its own.
        ParsedSql chained = ParsedSql.parse("SELECT arr['a\\'', :x]['b', :y] FROM t");
        assertEquals("SELECT arr['a\\'', :x]['b', ?] FROM t", chained.parameterizedSql());
        assertEquals(List.of("y"), chained.namedParameters());

        // The mixed-style guard is unchanged for unambiguous tokens; an ambiguous token contributes no
        // marker to it, so the "?" it holds verbatim cannot clash with a named binding elsewhere.
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY['it''s :literal', ?] FROM t WHERE id = :id"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[E'it''s :literal', ?] FROM t WHERE id = :id"));
        // An escape string is unambiguous, so the "?" next to it IS a placeholder and does clash.
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[E'it\\'s :literal', ?] FROM t WHERE id = :id"));
        ParsedSql verbatimQuestionMark = ParsedSql.parse("SELECT ARRAY['it\\'s :literal', ?] FROM t WHERE id = :id");
        assertEquals("SELECT ARRAY['it\\'s :literal', ?] FROM t WHERE id = ?", verbatimQuestionMark.parameterizedSql());
        assertEquals(List.of("id"), verbatimQuestionMark.namedParameters());
        assertEquals(1, verbatimQuestionMark.parameterCount());
    }

    @Test
    public void testPositionalParameterOffsetsMatchCountedPlaceholders() {
        // The offsets point at exactly the '?' characters parameterCount() counted, in the ORIGINAL text:
        // not the JSON operator, not the '?' in a literal, a comment or a bracket-quoted identifier, but the
        // one inside an array subscript.
        final String sql = "SELECT arr[?] FROM t WHERE doc ? 'key' AND k = 'a?' AND [w?] = ? /* ? */ -- ?\n AND z = ?";
        final ParsedSql parsed = ParsedSql.parse(sql);
        assertEquals(3, parsed.parameterCount());

        final int[] offsets = parsed.positionalParameterOffsets();
        assertEquals(3, offsets.length);
        assertEquals(sql.indexOf("arr[?]") + 4, offsets[0]);
        assertEquals(sql.indexOf("[w?] = ?") + 7, offsets[1]);
        assertEquals(sql.lastIndexOf('?'), offsets[2]);

        for (final int offset : offsets) {
            assertEquals('?', sql.charAt(offset));
        }

        // Fresh array on every call; whitespace runs collapsed by the tokenizer do not shift the offsets.
        assertEquals(false, offsets == parsed.positionalParameterOffsets());
        final String spaced = "SELECT  *\tFROM t\n WHERE  a = ?  AND b = ?";
        final int[] spacedOffsets = ParsedSql.parse(spaced).positionalParameterOffsets();
        assertEquals(2, spacedOffsets.length);
        assertEquals(spaced.indexOf('?'), spacedOffsets[0]);
        assertEquals(spaced.lastIndexOf('?'), spacedOffsets[1]);

        assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE name = :name").positionalParameterOffsets().length);
        assertEquals(0, ParsedSql.parse("SELECT [what?] FROM t").positionalParameterOffsets().length);
    }

    @Test
    public void testPositionalParameterOffsetsSkipACommentSharingItsFirstCharacterWithTheNextToken() {
        // The comment openers "/*" and "--" start with the operator tokens "/" and "-", so a comment
        // immediately followed by such an operator must still be skipped while the token stream is walked back
        // onto the original text. Otherwise the operator matches the opener, the comment is never skipped, and
        // the offset of the real placeholder lands on the commented-out '?' instead.
        final String block = "SELECT 1 /* ? */ / ?";
        final ParsedSql blockParsed = ParsedSql.parse(block);
        assertEquals(1, blockParsed.parameterCount());
        assertArrayEquals(new int[] { block.lastIndexOf('?') }, blockParsed.positionalParameterOffsets());

        final String line = "SELECT 1 -- ?\n -?";
        final ParsedSql lineParsed = ParsedSql.parse(line);
        assertEquals(1, lineParsed.parameterCount());
        assertArrayEquals(new int[] { line.lastIndexOf('?') }, lineParsed.positionalParameterOffsets());

        final String hash = "SELECT 1 # ?\n -?";
        assertArrayEquals(new int[] { hash.lastIndexOf('?') }, ParsedSql.parse(hash).positionalParameterOffsets());

        // Several comments in a row, and a comment that is itself followed by another comment opener.
        final String chained = "SELECT ? /* ? */ /* ? */ / ? FROM t";
        final ParsedSql chainedParsed = ParsedSql.parse(chained);
        assertEquals(2, chainedParsed.parameterCount());
        assertArrayEquals(new int[] { chained.indexOf('?'), chained.lastIndexOf('?') }, chainedParsed.positionalParameterOffsets());

        // A "-" or "/" token that merely follows a comment-free expression is unaffected.
        final String plain = "SELECT 1 / ? - ?";
        assertArrayEquals(new int[] { plain.indexOf('?'), plain.lastIndexOf('?') }, ParsedSql.parse(plain).positionalParameterOffsets());
    }

    @Test
    public void testParse_ManyMarkersInOneSubscriptToken() {
        // A subscript is emitted as a single token, so all of its markers are extracted from that one token.
        // The marker positions are therefore collected once per token instead of once per marker (which was
        // quadratic in the number of markers); this asserts the result of that scan for a token holding many.
        final int markerCount = 2000;
        final StringBuilder named = new StringBuilder("SELECT ARRAY[");
        final StringBuilder ibatis = new StringBuilder("SELECT ARRAY[");
        final List<String> expectedNames = new ArrayList<>(markerCount);

        for (int i = 0; i < markerCount; i++) {
            if (i > 0) {
                named.append(", ");
                ibatis.append(", ");
            }

            named.append(':').append("id").append(i);
            ibatis.append("#{id").append(i).append('}');
            expectedNames.add("id" + i);
        }

        final ParsedSql namedParsed = ParsedSql.parse(named.append("] FROM t").toString());
        assertEquals(markerCount, namedParsed.parameterCount());
        assertEquals(expectedNames, namedParsed.namedParameters());
        assertEquals(markerCount, Strings.countMatches(namedParsed.parameterizedSql(), '?'));

        final ParsedSql ibatisParsed = ParsedSql.parse(ibatis.append("] FROM t").toString());
        assertEquals(markerCount, ibatisParsed.parameterCount());
        assertEquals(expectedNames, ibatisParsed.namedParameters());
        assertEquals(markerCount, Strings.countMatches(ibatisParsed.parameterizedSql(), '?'));
    }

    @Test
    public void testParse_CommentsInsideSubscriptsDoNotCreateParameters() {
        final ParsedSql named = ParsedSql.parse("SELECT ARRAY[:first /* :ignored ? #{ignored} */, :last]");
        assertEquals("SELECT ARRAY[? /* :ignored ? #{ignored} */, ?]", named.parameterizedSql());
        assertEquals(List.of("first", "last"), named.namedParameters());
        assertEquals(2, named.parameterCount());

        final ParsedSql ibatis = ParsedSql.parse("SELECT ARRAY[#{first} /* #{ignored} :ignored ? */, #{last}]");
        assertEquals("SELECT ARRAY[? /* #{ignored} :ignored ? */, ?]", ibatis.parameterizedSql());
        assertEquals(List.of("first", "last"), ibatis.namedParameters());
        assertEquals(2, ibatis.parameterCount());

        final String positionalSql = "SELECT ARRAY[? /* ? :ignored #{ignored} */, ?]";
        final ParsedSql positional = ParsedSql.parse(positionalSql);
        assertEquals(positionalSql, positional.parameterizedSql());
        assertEquals(2, positional.parameterCount());
        Assertions.assertArrayEquals(new int[] { positionalSql.indexOf('?'), positionalSql.lastIndexOf('?') }, positional.positionalParameterOffsets());

        final ParsedSql lineComment = ParsedSql.parse("SELECT ARRAY[:first -- :ignored ? #{ignored}\n, :last]");
        assertEquals("SELECT ARRAY[? -- :ignored ? #{ignored}\n, ?]", lineComment.parameterizedSql());
        assertEquals(List.of("first", "last"), lineComment.namedParameters());
        assertEquals(2, lineComment.parameterCount());

        assertEquals(List.of("id"), ParsedSql.parse("SELECT arr[1][/* :ignored ? #{ignored} */ :id]").namedParameters());
        assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY['/* :literal */', :id]").namedParameters());
        assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY['-- :literal', :id]").namedParameters());
    }

    @Test
    public void testParse_JsonQuestionOperatorAfterSupplementaryIdentifier() {
        final String column = "\uD801\uDC00payload";
        final ParsedSql named = ParsedSql.parse("SELECT " + column + " ? :key FROM events");
        assertEquals("SELECT " + column + " ? ? FROM events", named.parameterizedSql());
        assertEquals(List.of("key"), named.namedParameters());
        assertEquals(1, named.parameterCount());

        final String sql = "SELECT " + column + " ? ? FROM events";
        final ParsedSql positional = ParsedSql.parse(sql);
        assertEquals(1, positional.parameterCount());
        Assertions.assertArrayEquals(new int[] { sql.lastIndexOf('?') }, positional.positionalParameterOffsets());
        assertEquals(0, ParsedSql.parse("SELECT " + column + " ? 'key' FROM events").parameterCount());
    }

    @Test
    public void testParse_RemovingBlockCommentsCannotCreateCommentOpenersOrOperators() {
        assertEquals("SELECT 1- -2", ParsedSql.parse("SELECT 1-/* comment */-2").parameterizedSql());
        assertEquals("SELECT 1/ *2", ParsedSql.parse("SELECT 1//* comment */*2").parameterizedSql());
        assertEquals("SELECT 1< =2", ParsedSql.parse("SELECT 1</* comment */=2").parameterizedSql());
        // Separate the placeholder from '-' because '?-' is a configured PostgreSQL operator.
        final ParsedSql positional = ParsedSql.parse("SELECT ? -/* comment */- ?");
        assertEquals("SELECT ? - - ?", positional.parameterizedSql());
        assertEquals(2, positional.parameterCount());
    }

    @Test
    public void testSubscriptOpeningOffsetsShareParameterParsingRules() {
        final String sql = "SELECT scores[:a] /* [ignored] */ ['literal [text]', :b] FROM t WHERE id = :id";
        Assertions.assertArrayEquals(new int[] { sql.indexOf('['), sql.indexOf("['literal") }, ParsedSql.subscriptOpeningOffsets(sql));

        final String nested = "SELECT ARRAY[ARRAY[:id]] FROM t";
        Assertions.assertArrayEquals(new int[] { nested.indexOf('['), nested.lastIndexOf('[') }, ParsedSql.subscriptOpeningOffsets(nested));

        final String comments = "SELECT arr[/* [ignored] ' */ :id, '-- [literal]'] FROM t";
        Assertions.assertArrayEquals(new int[] { comments.indexOf('[') }, ParsedSql.subscriptOpeningOffsets(comments));
        final String lineComment = "SELECT arr[1 -- [ignored] '\n, :id] FROM t";
        Assertions.assertArrayEquals(new int[] { lineComment.indexOf('[') }, ParsedSql.subscriptOpeningOffsets(lineComment));

        assertEquals(0, ParsedSql.subscriptOpeningOffsets("SELECT [literal:param], t.[:param], 'arr[:param]', [#{param}] FROM t").length);
        assertEquals(0, ParsedSql.subscriptOpeningOffsets("SELECT ARRAY['a\\'', :id] FROM t").length);
        assertEquals(0, ParsedSql.subscriptOpeningOffsets("SELECT [a] [b:c] FROM t").length);

        final String standalone = "SELECT [:id], [?] FROM t";
        Assertions.assertArrayEquals(new int[] { standalone.indexOf('['), standalone.lastIndexOf('[') }, ParsedSql.subscriptOpeningOffsets(standalone));

        final String custom = "SELECT  arr[:id]::jsonb FROM t";
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.TokenizerConfig.builder().withSeparator("::").build());
        Assertions.assertArrayEquals(new int[] { custom.indexOf('[') }, ParsedSql.subscriptOpeningOffsets(custom, tokenizer));
    }

    @Test
    public void positionalParameterOffsets_verticalTabGluedToIdentifier_alignsInSlowPath() {
        // A vertical tab is Java whitespace but not tokenizer whitespace, so it stays inside the token
        // "\u000Bx"; the source walk must not skip it, or the token is never found (used to throw ISE).
        final String sql = "SELECT ?\u000Bx FROM t WHERE '?' = ?"; // the quoted '?' forces the source-alignment path
        final ParsedSql parsed = ParsedSql.parse(sql);

        assertEquals(2, parsed.parameterCount());
        assertArrayEquals(new int[] { 7, 30 }, parsed.positionalParameterOffsets());

        // and the raw-offset fast path agrees for the same shape without the quoted marker
        final ParsedSql fast = ParsedSql.parse("SELECT ?\u000Bx FROM t WHERE a = ?");
        assertArrayEquals(new int[] { 7, 28 }, fast.positionalParameterOffsets());
    }

    @Test
    public void parameterizedSql_placeholderGluedToLineComment_dropsComment() {
        // "?--" used to lex as the operator "?-" plus "-", so the comment survived, the newline was
        // collapsed and the comment swallowed the second predicate (1 placeholder instead of 2).
        final String sql = "SELECT * FROM t WHERE id = ?-- the id\nAND name = ?";
        final ParsedSql parsed = ParsedSql.parse(sql);

        assertEquals(2, parsed.parameterCount());
        assertEquals("SELECT * FROM t WHERE id = ? AND name = ?", parsed.parameterizedSql());
        assertArrayEquals(new int[] { 27, 49 }, parsed.positionalParameterOffsets());

        // the same shape with a block comment glued to the placeholder
        final ParsedSql block = ParsedSql.parse("SELECT * FROM t WHERE id = ?/* the id */AND name = ?");
        assertEquals(2, block.parameterCount());
        assertEquals("SELECT * FROM t WHERE id = ? AND name = ?", block.parameterizedSql());
    }

    @Test
    public void quotedIdentifierSubscriptChain_documentedLimitation() {
        // Documented: a subscript chain is only followed from an unquoted identifier root. After a quoted
        // identifier the chain stops and a following group is classified by the standalone rules.
        assertEquals(2, ParsedSql.parse("SELECT t.q[?]['a', ?] FROM t").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT \"q\"[?]['a', ?] FROM t").parameterCount());
        assertArrayEquals(new int[] { 11 }, ParsedSql.parse("SELECT \"q\"[?]['a', ?] FROM t").positionalParameterOffsets());

        // ... which does NOT mean the following group is ignored: a standalone "[?]" is a positional subscript
        // and is still counted, only a bracket-quoted "['a', ?]" is left alone.
        assertEquals(2, ParsedSql.parse("SELECT \"q\"[?][?] FROM t").parameterCount());
        assertArrayEquals(new int[] { 11, 14 }, ParsedSql.parse("SELECT \"q\"[?][?] FROM t").positionalParameterOffsets());
        assertEquals(3, ParsedSql.parse("SELECT \"q\"[?][?][?] FROM t").parameterCount());
        assertEquals(2, ParsedSql.parse("SELECT t.\"q\"[?][?] FROM t").parameterCount());

        // a backtick-quoted or bracket-quoted root behaves exactly like the double-quoted one
        assertEquals(2, ParsedSql.parse("SELECT `q`[?][?] FROM t").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT `q`[?]['a', ?] FROM t").parameterCount());
        assertEquals(2, ParsedSql.parse("SELECT [q][?][?] FROM t").parameterCount());
        assertEquals(1, ParsedSql.parse("SELECT [q][?]['a', ?] FROM t").parameterCount());
    }

    @Test
    public void testParse_MultiDimensionalArrayConstructorDoesNotHideLaterBindings() {
        final ParsedSql named = ParsedSql.parse("SELECT * FROM t WHERE m = ARRAY[[1,2],[3,4]] AND id = :id");
        assertEquals("SELECT * FROM t WHERE m = ARRAY[[1,2],[3,4]] AND id = ?", named.parameterizedSql());
        assertEquals(Arrays.asList("id"), named.namedParameters());

        assertEquals(Arrays.asList("id"), ParsedSql.parse("SELECT * FROM t WHERE m = ARRAY[[1,2],[3,4]] AND id = #{id}").namedParameters());

        final String positionalSql = "SELECT * FROM t WHERE m = ARRAY[[1,2],[3,4]] AND id = ?";
        final ParsedSql positional = ParsedSql.parse(positionalSql);
        assertEquals(1, positional.parameterCount());
        assertArrayEquals(new int[] { positionalSql.lastIndexOf('?') }, positional.positionalParameterOffsets());

        assertEquals(Arrays.asList("a"), ParsedSql.parse("SELECT ARRAY[[1,2],[3,:a]]").namedParameters());
        assertEquals(1, ParsedSql.parse("SELECT ARRAY[[1,2],[3,?]]").parameterCount());
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE m = ARRAY[[1,2],[3,?]] AND id = :id"));
    }

    @Test
    public void testParse_UnescapedJsonOperatorGluedToIbatisBinding() {
        final ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE doc ?#{key}");
        assertEquals(Arrays.asList("key"), parsed.namedParameters());
        assertEquals(1, parsed.parameterCount());
        assertFalse(parsed.parameterizedSql().contains("#{"), parsed.parameterizedSql());
        assertEquals(parsed.namedParameters(), ParsedSql.parse("SELECT ARRAY[doc ?#{key}]").namedParameters());

        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE a = ?#{key}"));
        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[?#{key}]"));
    }

    @Test
    public void testParse_WindowFrameOffsetPlaceholderIsCounted() {
        for (final String unit : new String[] { "ROWS", "RANGE", "GROUPS" }) {
            final String sql = "SELECT sum(x) OVER (ORDER BY d " + unit + " ? PRECEDING) FROM t";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount(), sql);
            assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
        }

        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT sum(x) OVER (ORDER BY d RANGE ? PRECEDING) FROM t WHERE id = :id"));
    }
}
