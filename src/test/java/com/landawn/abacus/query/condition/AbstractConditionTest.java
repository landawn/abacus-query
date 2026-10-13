package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Dsl;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.SqlDialect;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.NamingPolicy;

public class AbstractConditionTest extends TestBase {

    @Test
    void testAnd() {
        Equal cond1 = new Equal("status", "active");
        Equal cond2 = new Equal("age", 18);

        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Operator.AND, result.operator());
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testAnd_NullCondition() {
        Equal cond = new Equal("status", "active");
        assertThrows(IllegalArgumentException.class, () -> cond.and(null));
    }

    @Test
    void testArraySnapshotsRejectDirectAndIndirectCycles() {
        final Object[] direct = new Object[1];
        direct[0] = direct;
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.snapshotMutableValue(direct));

        final Object[] first = new Object[1];
        final Object[] second = { first };
        first[0] = second;
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.snapshotMutableValue(first));

        assertArrayEquals(new Object[] { "still usable" }, (Object[]) AbstractCondition.snapshotMutableValue(new Object[] { "still usable" }));
    }

    @Test
    void testChainedOperations() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);

        And and = cond1.and(cond2).and(cond3);
        assertEquals(3, and.conditions().size());
    }

    @Test
    void testClauseAliasesRemainPredicatesInStructuredSubqueries() {
        // Misclassifying an enum alias here omits WHERE entirely from the structured SELECT.
        for (final String prefix : Arrays.asList("/* ORDER BY id */ ", "-- ORDER BY id\n")) {
            for (final String column : Arrays.asList("order_by", "union_all", "left_join")) {
                final String predicate = prefix + column + " = 1";
                final SubQuery subQuery = new SubQuery("accounts", "id", Filters.expr(predicate));
                assertTrue(subQuery.condition() instanceof Where, predicate);
                assertEquals("SELECT id FROM accounts WHERE " + column + " = 1", subQuery.toString().replaceAll("\\s+", " ").trim());
            }
        }

        // Actual clause keywords still bypass WHERE, even when separated by a comment.
        final String clauseSql = "/* order_by = 1 */ ORDER /* gap */ BY id";
        final SqlExpression clause = Filters.expr(clauseSql);
        final SubQuery subQuery = new SubQuery("accounts", "id", clause);
        assertSame(clause, subQuery.condition());
        assertEquals("SELECT id FROM accounts ORDER BY id", subQuery.toString().replaceAll("\\s+", " ").trim());
    }

    @Test
    void testClauseDetectionDoesNotTreatEnumNamesAsSqlKeywords() {
        for (final String column : Arrays.asList("order_by", "group_by", "left_join", "right_join", "full_join", "cross_join", "inner_join", "natural_join",
                "union_all", "for_update", "ORDER_BY")) {
            final SqlExpression predicate = Filters.expr(column + " = 1");
            assertFalse(AbstractCondition.isClause(predicate), column);
            assertEquals("WHERE " + column + " = 1", new Where(predicate).toString());
            assertEquals("ORDER BY " + column, new OrderBy(column).toString());
            assertEquals("((" + column + " = 1) AND (enabled = true))", predicate.and(new Equal("enabled", true)).toString());
        }

        // Operator lookup still supports enum aliases; only SQL-text clause detection differs.
        assertEquals(Operator.ORDER_BY, Operator.of("order_by"));
        assertTrue(AbstractCondition.isClause(Filters.expr("order by id")));
        assertThrows(IllegalArgumentException.class, () -> new Where(Filters.expr("ORDER BY id")));

        // The String overload applies the same rule, so both entry points agree on every spelling.
        for (final String alias : Arrays.asList("order_by", "group_by", "left_join", "union_all", "for_update", "ORDER_BY")) {
            assertFalse(AbstractCondition.isClause(alias), alias);
            assertNotNull(Operator.of(alias), alias);
        }

        for (final String sqlToken : Arrays.asList("WHERE", "ORDER BY", "group by", "Left Join", "UNION ALL", "FOR UPDATE")) {
            assertTrue(AbstractCondition.isClause(sqlToken), sqlToken);
        }

        assertFalse(AbstractCondition.isClause("="));
        assertFalse(AbstractCondition.isClause("AND"));
        assertFalse(AbstractCondition.isClause((String) null));
        assertFalse(AbstractCondition.isClause(""));
    }

    @Test
    void testClauseKeywordColumnFollowedByExtendedPredicateOperatorIsNotAClause() {
        for (final String predicate : new String[] { "offset <=> 5", "offset == 5", "offset ^= 5", "offset !< 5", "offset !> 5", "offset ~ 'x'", "minus ~'x'",
                "offset !~ 'x'", "offset ~* 'x'", "offset !~* 'x'", "offset REGEXP 'x'", "offset rlike 'x'", "minus GLOB 'x*'", "offset REGEXP col2",
                "offset SIMILAR TO 'x'", "offset similar to'x'", "offset NOT REGEXP 'x'", "offset NOT RLIKE 'x'", "offset NOT GLOB 'x'",
                "offset NOT SIMILAR TO 'x'", "offset LIKE'x%'", "offset ILIKE'x%'", "offset REGEXP'x'", "offset BETWEEN'1' AND '2'", "offset NOT LIKE'x%'",
                "offset IN(1, 2)", "offset /* c */ <=> 5" }) {
            assertFalse(AbstractCondition.isClause(Filters.expr(predicate)), predicate);
        }

        assertEquals("SELECT a FROM t WHERE offset <=> 5", Dsl.PSC.select("a").from("t").where(Filters.expr("offset <=> 5")).build().query());
        assertEquals("((a = 1) AND (offset REGEXP 'x'))", Filters.eq("a", 1).and(Filters.expr("offset REGEXP 'x'")).toString());
        assertEquals("WHERE offset LIKE'x%'", Filters.where(Filters.expr("offset LIKE'x%'")).toString());
    }

    @Test
    void testClauseKeywordColumnFollowedByPredicateOperatorIsNotAClause() {
        // OFFSET/MINUS are non-reserved in several databases, so they can be unquoted column names.
        assertEquals("WHERE offset > 5", Filters.where(Filters.expr("offset > 5")).toString());
        assertEquals("((minus = 1) AND (a = 1))", Filters.and(Filters.expr("minus = 1"), Filters.eq("a", 1)).toString());
        assertEquals("NOT (offset IS NULL)", Filters.not(Filters.expr("offset IS NULL")).toString());
        assertEquals("WHERE offset NOT IN (1, 2)", Filters.where(Filters.expr("offset NOT IN (1, 2)")).toString());
        assertEquals("SELECT a FROM t WHERE offset > 5", Dsl.PSC.select("a").from("t").where(Filters.expr("offset > 5")).build().query());

        // Real clause fragments are still rejected.
        assertThrows(IllegalArgumentException.class, () -> Filters.where(Filters.expr("OFFSET 5")));
        assertThrows(IllegalArgumentException.class, () -> Filters.where(Filters.expr("WHERE NOT a = 1")));
        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.expr("LIMIT 10"), Filters.eq("a", 1)));
    }

    @Test
    void testClauseKeywordGluedToQuotedIdentifierIsStillAClause() {
        // Regression: SqlParser.nextToken keeps a quoted region glued to the preceding word ("BY\"id\"" is one token),
        // so these clauses were classified as predicates: subQuery wrapped them in WHERE and junctions accepted them.
        assertEquals("SELECT id FROM t ORDER BY\"id\"", Filters.subQuery("t", Arrays.asList("id"), Filters.expr("ORDER BY\"id\"")).toString());
        assertEquals("SELECT id FROM t GROUP BY`id`", Filters.subQuery("t", Arrays.asList("id"), Filters.expr("GROUP BY`id`")).toString());
        assertEquals("SELECT id FROM t WHERE\"x\" = 1", Filters.subQuery("t", Arrays.asList("id"), Filters.expr("WHERE\"x\" = 1")).toString());

        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.eq("a", 1), Filters.expr("ORDER BY\"id\"")));
        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.eq("a", 1), Filters.expr("LEFT JOIN[t] ON 1 = 1")));
        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.eq("a", 1), Filters.expr("ON\"a\" = \"b\"")));

        // A non-reserved keyword used as a column with a subscript is still a predicate.
        assertEquals("((a = 1) AND (offset[1] > 5))", Filters.and(Filters.eq("a", 1), Filters.expr("offset[1] > 5")).toString());
    }

    // The wildcard arity decision ignores comments, but SqlBuilder still rejects a comment token in a select item when the
    // query is built. A #temp qualifier is a hash comment there too, except under a SQL Server dialect, which renders it.
    @Test
    void testCommentedWildcardProjectionsAreRejectedWhenBuilt() {
        final Dsl sqlServer = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());

        for (final String projection : List.of("users /* table */ . *", "* /* all columns */", "users.* -- all columns", "\"some table\" . /* columns */ *",
                "/* columns */ *")) {
            final InSubQuery in = new InSubQuery(List.of("id", "name"), new SubQuery("users", List.of(projection), new Equal("active", true)));

            assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("accounts").where(in).build(), projection);
            assertThrows(IllegalArgumentException.class, () -> sqlServer.select("id").from("accounts").where(in).build(), projection);
        }

        for (final String projection : List.of("#users.*", "#users. *")) {
            final InSubQuery in = new InSubQuery(List.of("id", "name"), new SubQuery("users", List.of(projection), new Equal("active", true)));

            assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("accounts").where(in).build(), projection);
            assertEquals("SELECT id FROM accounts WHERE (id, name) IN (SELECT " + projection + " FROM users WHERE active = ?)",
                    sqlServer.select("id").from("accounts").where(in).build().query());
        }

        final InSubQuery spaced = new InSubQuery(List.of("id", "name"), new SubQuery("users", List.of("users.\n\t*"), new Equal("active", true)));
        assertEquals("SELECT id FROM accounts WHERE (id, name) IN (SELECT users. * FROM users WHERE active = ?)",
                Dsl.PSC.select("id").from("accounts").where(spaced).build().query());
    }

    @Test
    void testComplexConditionChaining() {
        TestCondition cond1 = new TestCondition(Operator.EQUAL, "val1");
        TestCondition cond2 = new TestCondition(Operator.NOT_EQUAL, "val2");
        TestCondition cond3 = new TestCondition(Operator.GREATER_THAN, "val3");
        TestCondition cond4 = new TestCondition(Operator.LESS_THAN, "val4");

        // Test complex chaining: (cond1 AND cond2) OR (cond3 AND cond4)
        And and1 = cond1.and(cond2);
        And and2 = cond3.and(cond4);
        Or complex = and1.or(and2);

        assertNotNull(complex);
        assertEquals(2, complex.conditions().size());

        // Test NOT of complex condition
        Not notComplex = complex.not();
        assertNotNull(notComplex);
        assertEquals(complex, notComplex.condition());
    }

    @Test
    void testComposableOperandRejectionNamesTheConditionType() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Not(Filters.subQuery("SELECT 1")));
        assertTrue(e.getMessage().contains("SubQuery"), e.getMessage());
    }

    @Test
    void testConcatPropNames_CollectionFourOrMore() {
        OrderBy orderBy = new OrderBy("col1", "col2", "col3", "col4", "col5");
        String str = orderBy.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
        assertTrue(str.contains("col3"));
        assertTrue(str.contains("col4"));
        assertTrue(str.contains("col5"));
    }

    @Test
    void testConcatPropNames_CollectionSingleItem() {
        OrderBy orderBy = new OrderBy("single");
        String str = orderBy.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("single"));
    }

    @Test
    void testConcatPropNames_CollectionThreeItems() {
        OrderBy orderBy = new OrderBy("a", "b", "c");
        String str = orderBy.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
        assertTrue(str.contains("c"));
    }

    @Test
    void testConcatPropNames_CollectionTwoItems() {
        OrderBy orderBy = new OrderBy("first", "second");
        String str = orderBy.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("first"));
        assertTrue(str.contains("second"));
    }

    @Test
    void testConcatPropNames_EmptyArray() {
        // Testing through GroupBy with no props - just verify it can be created
        GroupBy groupBy = new GroupBy();
        assertNotNull(groupBy);
        // Note: toString() on empty GroupBy throws NPE, this is expected behavior
    }

    @Test
    void testConcatPropNames_FourNames() {
        GroupBy groupBy = new GroupBy("col1", "col2", "col3", "col4");
        String str = groupBy.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
        assertTrue(str.contains("col3"));
        assertTrue(str.contains("col4"));
    }

    @Test
    void testConcatPropNames_SingleName() {
        // Tested indirectly through other condition classes
        Equal condition = new Equal("name", "value");
        assertNotNull(condition.propName());
    }

    @Test
    void testConcatPropNames_ThreeNames() {
        GroupBy groupBy = new GroupBy("col1", "col2", "col3");
        String str = groupBy.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
        assertTrue(str.contains("col3"));
    }

    @Test
    void testConcatPropNames_TwoNames() {
        GroupBy groupBy = new GroupBy("col1", "col2");
        String str = groupBy.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
    }

    @Test
    void testConcatPropNamesArray() {
        // Test empty array
        String result = AbstractCondition.concatPropNames();
        assertEquals("", result);

        // Test single element
        result = AbstractCondition.concatPropNames("name");
        assertEquals("name", result);

        // Test two elements
        result = AbstractCondition.concatPropNames("city", "state");
        assertEquals("(city, state)", result);

        // Test three elements
        result = AbstractCondition.concatPropNames("a", "b", "c");
        assertEquals("(a, b, c)", result);

        // Test more than three elements
        result = AbstractCondition.concatPropNames("col1", "col2", "col3", "col4", "col5");
        assertEquals("(col1, col2, col3, col4, col5)", result);
    }

    @Test
    void testConcatPropNamesCollection() {
        // Test empty collection
        List<String> empty = new ArrayList<>();
        String result = AbstractCondition.concatPropNames(empty);
        assertEquals("", result);

        // Test single element
        List<String> single = Arrays.asList("name");
        result = AbstractCondition.concatPropNames(single);
        assertEquals("name", result);

        // Test two elements
        List<String> two = Arrays.asList("city", "state");
        result = AbstractCondition.concatPropNames(two);
        assertEquals("(city, state)", result);

        // Test three elements
        List<String> three = Arrays.asList("a", "b", "c");
        result = AbstractCondition.concatPropNames(three);
        assertEquals("(a, b, c)", result);

        // Test more than three elements
        List<String> many = Arrays.asList("col1", "col2", "col3", "col4", "col5");
        result = AbstractCondition.concatPropNames(many);
        assertEquals("(col1, col2, col3, col4, col5)", result);
    }

    @Test
    void testConcatPropNamesWithSet() {
        // Test with LinkedHashSet to maintain order
        Set<String> props = new LinkedHashSet<>();
        props.add("first");
        props.add("second");
        props.add("third");

        String result = AbstractCondition.concatPropNames(props);
        assertEquals("(first, second, third)", result);
    }

    @Test
    void testConstructor() {
        TestCondition condition = new TestCondition(Operator.EQUAL, "test");

        assertNotNull(condition);
        assertEquals(Operator.EQUAL, condition.operator());
        assertEquals("test", condition.value);
    }

    @Test
    void testConstructor_NullOperatorThrowsIllegalArgumentException() {
        // AbstractCondition(Operator) via ComposableCondition(Operator): a null operator is an
        // IllegalArgumentException (previously threw NullPointerException).
        assertThrows(IllegalArgumentException.class, () -> new TestCondition(null, "value"));
    }

    @Test
    void testCreateSortExpression_CollectionRejectsEmptyProperty() {
        assertThrows(IllegalArgumentException.class,
                () -> AbstractCondition.createSortSpec(Arrays.asList("id", ""), com.landawn.abacus.query.SortDirection.ASC));
    }

    @Test
    void testCreateSortExpression_CollectionRejectsNullDirection() {
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortSpec(Arrays.asList("id"), null));
    }

    @Test
    void testCreateSortExpression_MapRejectsEmptyProperty() {
        java.util.Map<String, com.landawn.abacus.query.SortDirection> orders = new java.util.LinkedHashMap<>();
        orders.put("", com.landawn.abacus.query.SortDirection.ASC);

        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortSpec(orders));
    }

    @Test
    void testCreateSortExpression_MapRejectsNullDirection() {
        java.util.Map<String, com.landawn.abacus.query.SortDirection> orders = new java.util.LinkedHashMap<>();
        orders.put("id", null);

        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortSpec(orders));
    }

    @Test
    void testCreateSortExpression_SinglePropertyRejectsNullDirection() {
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortSpec("id", null));
    }

    @Test
    void testCreateSortExpression_StringArrayRejectsEmptyProperty() {
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortSpec("id", ""));
    }

    @Test
    void testCustomNumberMustRenderAsNumericLiteral() {
        assertEquals("amount = 1.25e+2", new Equal("amount", customNumber("1.25e+2")).toString());

        final Number unsafe = customNumber("0); DROP TABLE users; --");
        assertThrows(IllegalArgumentException.class, () -> new Equal("amount", unsafe).toString());
    }

    @Test
    void testCustomRendererLineCommentsDoNotConsumeWrapperParentheses() {
        final SqlExpression condition = new SqlExpression("active = 1") {
            @Override
            public String toSql(final NamingPolicy namingPolicy) {
                return "active = 1 -- trailing comment";
            }
        };

        assertEquals("NOT (active = 1 -- trailing comment\n)", new Not(condition).toString());
        assertEquals("((active = 1 -- trailing comment\n) AND (id = 1))", new And(condition, new Equal("id", 1)).toString());
        assertEquals("score BETWEEN active = 1 -- trailing comment\n AND 10", new Between("score", condition, 10).toString());
    }

    @Test
    void testDateFamilyValuesRenderAsLocalWallClockTextInConditions() {
        // Covers full Timestamp precision, IN/BETWEEN value lists of date-family values, and a Calendar rendered at its
        // instant in the JVM time zone (as it is bound), including a wall-clock time inside a JVM-zone DST gap.
        assertEquals("ts = '2020-01-02 03:04:05.123456789'", Filters.eq("ts", java.sql.Timestamp.valueOf("2020-01-02 03:04:05.123456789")).toString());
        assertEquals("d IN ('2020-01-02', '2020-01-03')",
                Filters.in("d", Arrays.asList(java.sql.Date.valueOf("2020-01-02"), java.sql.Date.valueOf("2020-01-03"))).toString());
        assertEquals("d BETWEEN '2020-01-02' AND '2020-01-03'",
                Filters.between("d", java.sql.Date.valueOf("2020-01-02"), java.sql.Date.valueOf("2020-01-03")).toString());
        assertEquals("d NOT BETWEEN '2020-01-02 03:04:05.0' AND '2020-01-03 00:00:00.0'",
                Filters.notBetween("d", java.sql.Timestamp.valueOf("2020-01-02 03:04:05"), java.sql.Timestamp.valueOf("2020-01-03 00:00:00")).toString());

        final java.util.TimeZone defaultZone = java.util.TimeZone.getDefault();

        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

            final java.util.Calendar tokyo = new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
            tokyo.clear();
            tokyo.set(2020, java.util.Calendar.JANUARY, 2, 3, 4, 5);
            assertEquals("c = '2020-01-01 18:04:05.0'", Filters.eq("c", tokyo).toString());
            assertEquals("c IN ('2020-01-01 18:04:05.0')", Filters.in("c", Arrays.asList(tokyo)).toString());

            // 2020-03-08 02:30 is in New York's spring-forward gap; rendering the instant (12:30 the day before in New York)
            // avoids the one-hour shift the old wall-clock round trip produced ('2020-03-08 03:30:00.0').
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"));
            final java.util.Calendar gap = new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
            gap.clear();
            gap.set(2020, java.util.Calendar.MARCH, 8, 2, 30, 0);
            assertEquals("c = '2020-03-07 12:30:00.0'", Filters.eq("c", gap).toString());
        } finally {
            java.util.TimeZone.setDefault(defaultZone);
        }
    }

    @Test
    void testFormatParameter_CharacterIsQuoted() {
        // BUG: Previously a Character was rendered via Character.toString(),
        // producing an unquoted bare letter. Worse, a single-quote character would
        // produce a bare "'" that breaks the surrounding SQL.
        String resultLetter = AbstractCondition.formatParameter('X', NamingPolicy.NO_CHANGE);
        assertEquals("'X'", resultLetter);

        String resultQuote = AbstractCondition.formatParameter('\'', NamingPolicy.NO_CHANGE);
        assertNotNull(resultQuote);
        assertTrue(resultQuote.startsWith("'") && resultQuote.endsWith("'"));
        // The escaped quote inside must be present so the literal stays balanced.
        assertTrue(resultQuote.contains("\\'") || resultQuote.contains("''"), "Embedded single-quote must be escaped, got: " + resultQuote);
    }

    // ---------------------------------------------------------------------
    // Third-pass review: SQL-escaping / injection-vector regression tests.
    // ---------------------------------------------------------------------

    @Test
    void testFormatParameter_DateProducesQuotedISOLiteral() {
        // BUG: Previously java.util.Date fell through to Date.toString(),
        // emitting an unquoted "Mon Jan 01 ... 1970" sequence that is not valid SQL.
        // After the fix, Date is rendered via N.stringOf and wrapped in single quotes.
        String result = AbstractCondition.formatParameter(new java.util.Date(0L), NamingPolicy.NO_CHANGE);
        assertNotNull(result);
        assertTrue(result.startsWith("'") && result.endsWith("'"), "Date literal must be single-quoted, got: " + result);
        assertFalse(result.contains("PST") || result.contains("PDT") || result.contains("UTC ") || result.contains("GMT "),
                "Date literal must not use Java's Date.toString() form, got: " + result);
    }

    @Test
    void testFormatParameter_LocalDateTimeProducesQuotedLiteral() {
        java.time.LocalDateTime ldt = java.time.LocalDateTime.of(2024, 1, 2, 3, 4, 5);
        String result = AbstractCondition.formatParameter(ldt, NamingPolicy.NO_CHANGE);
        assertNotNull(result);
        assertTrue(result.startsWith("'") && result.endsWith("'"), "LocalDateTime literal must be single-quoted, got: " + result);
        assertTrue(result.contains("2024"), "LocalDateTime literal must include the date, got: " + result);
    }

    @Test
    void testFormatParameter_NaNAndInfinityRejected() {
        // BUG: Previously NaN / Infinity were emitted as bare "NaN" / "Infinity",
        // which most SQL dialects reject. Callers must use IsNaN / IsInfinite instead.
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Double.NaN, NamingPolicy.NO_CHANGE));
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Double.POSITIVE_INFINITY, NamingPolicy.NO_CHANGE));
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Double.NEGATIVE_INFINITY, NamingPolicy.NO_CHANGE));
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Float.NaN, NamingPolicy.NO_CHANGE));
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatParameter(Float.POSITIVE_INFINITY, NamingPolicy.NO_CHANGE));
    }

    @Test
    void testFormatParameter_NumberStillUnchanged() {
        // Regression guard: ordinary numeric values must keep their bare numeric form.
        assertEquals("123", AbstractCondition.formatParameter(123, NamingPolicy.NO_CHANGE));
        assertEquals("123.45", AbstractCondition.formatParameter(123.45, NamingPolicy.NO_CHANGE));
        assertEquals("true", AbstractCondition.formatParameter(Boolean.TRUE, NamingPolicy.NO_CHANGE));
    }

    @Test
    void testFormatParameter_TrailingBackslashIsPreserved() {
        // SQL-standard literals do not assign escape semantics to backslash. A renderer for a
        // non-standard backslash mode must be dialect-aware rather than changing the value here.
        String backslashAtEnd = "x" + (char) 92;
        String result = AbstractCondition.formatParameter(backslashAtEnd, NamingPolicy.NO_CHANGE);
        assertEquals("'x\\'", result);
    }

    @Test
    void testGetOperator() {
        AbstractCondition condition = new Equal("name", "John");
        assertEquals(Operator.EQUAL, condition.operator());

        AbstractCondition and = new And(new Equal("a", 1));
        assertEquals(Operator.AND, and.operator());
    }

    @Test
    void testGluedClauseKeywordsConnectorsAndPredicateNearMisses() {
        // Covers clause detection for a glued third keyword and a glued bracket, ON/USING connectors glued to a quoted or
        // bracketed name inside a Join, and identifier near misses that must stay predicates.
        assertTrue(AbstractCondition.isClause(Filters.expr("LEFT OUTER JOIN\"t\" ON 1=1")));
        assertEquals("SELECT id FROM t LEFT OUTER JOIN\"t\" ON 1=1",
                Filters.subQuery("t", Arrays.asList("id"), Filters.expr("LEFT OUTER JOIN\"t\" ON 1=1")).toString());
        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.eq("a", 1), Filters.expr("LEFT OUTER JOIN\"t\" ON 1=1")));
        assertEquals("SELECT id FROM t ORDER BY[id]", Filters.subQuery("t", Arrays.asList("id"), Filters.expr("ORDER BY[id]")).toString());
        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.eq("a", 1), Filters.expr("ORDER BY[id]")));

        assertThrows(IllegalArgumentException.class, () -> new Join("t", Filters.expr("USING\"a\"")));
        assertThrows(IllegalArgumentException.class, () -> new Join("t", Filters.expr("ON\"a\" = \"b\"")));
        assertThrows(IllegalArgumentException.class, () -> new Join("t", Filters.expr("USING[a]")));

        assertEquals("((a = 1) AND (offset[1] LIKE'x%'))", Filters.and(Filters.eq("a", 1), Filters.expr("offset[1] LIKE'x%'")).toString());
        assertEquals("((a = 1) AND (\"where\" = 1))", Filters.and(Filters.eq("a", 1), Filters.expr("\"where\" = 1")).toString());
        assertEquals("((a = 1) AND (group\"x\" = 1))", Filters.and(Filters.eq("a", 1), Filters.expr("group\"x\" = 1")).toString());
    }

    @Test
    void testIsClause_LeadingCommentContainingClauseToken() {
        // Regression: the second-token scan must locate the token boundary via the comment-aware
        // nextTokenEndIndex. A raw indexOf(firstToken) would bind to the token's earlier occurrence
        // inside the leading comment and misread the second token, so these would return false.
        assertTrue(AbstractCondition.isClause(Filters.expr("/* GROUP */ GROUP BY x")));
        assertTrue(AbstractCondition.isClause(Filters.expr("/* ORDER */ ORDER BY name")));
    }

    @Test
    void testIsClause_StringEdgeCases() {
        assertFalse(AbstractCondition.isClause((String) null));
        assertTrue(AbstractCondition.isClause("WHERE"));
    }

    @Test
    void testJunctionConstructorRejectionOmitsEmptyOperator() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.eq("a", 1), Filters.subQuery("select 1")));
        assertTrue(e.getMessage().contains("SubQuery"), e.getMessage());
        assertFalse(e.getMessage().contains("operator ''"), e.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> Filters.or(Filters.eq("a", 1), Filters.expr("WHERE b = 1")));
        assertTrue(e2.getMessage().contains("SqlExpression \"WHERE b = 1\""), e2.getMessage());

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> Filters.eq("a", 1).and(Filters.subQuery("select 1")));
        assertFalse(e3.getMessage().contains("operator ''"), e3.getMessage());
    }

    @Test
    void testMixedComposableOperations() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);

        And and = cond1.and(cond2);
        Not not = and.not();

        assertNotNull(not);
        assertEquals(Operator.NOT, not.operator());
    }

    @Test
    void testMultiColumnSubqueriesRemainValidForTupleMembershipAndExistence() {
        final SubQuery multiColumn = new SubQuery("accounts", Arrays.asList("id", "tenantId"), new Equal("active", true));
        final InSubQuery in = new InSubQuery(Arrays.asList("accountId", "tenantId"), multiColumn);
        final NotInSubQuery notIn = new NotInSubQuery(Arrays.asList("accountId", "tenantId"), multiColumn);
        final Exists exists = new Exists(multiColumn);
        final NotExists notExists = new NotExists(multiColumn);

        assertEquals("(accountId, tenantId) IN (SELECT id, tenantId FROM accounts WHERE active = true)", in.toString());
        assertEquals("(accountId, tenantId) NOT IN (SELECT id, tenantId FROM accounts WHERE active = true)", notIn.toString());
        assertEquals("EXISTS (SELECT id, tenantId FROM accounts WHERE active = true)", exists.toString());
        assertEquals("NOT EXISTS (SELECT id, tenantId FROM accounts WHERE active = true)", notExists.toString());

        for (final Condition condition : Arrays.asList(in, notIn, exists, notExists)) {
            assertEquals(Arrays.asList(true), condition.parameters());
        }

        assertThrows(IllegalArgumentException.class, () -> new InSubQuery("accountId", multiColumn));
        assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery("accountId", multiColumn));
    }

    @Test
    void testNonReservedClauseKeywordWithGluedSubscriptIsAPredicate() {
        // Regression: "minus[1] + 2 > 0" (legal PostgreSQL; MINUS is not a PostgreSQL keyword) was classified as a clause, so
        // junctions rejected it and subQuery dropped the WHERE before "offset[1]::int > 5".
        assertFalse(AbstractCondition.isClause(Filters.expr("minus[1] + 2 > 0")));
        assertEquals("((a = 1) AND (minus[1] + 2 > 0))", Filters.and(Filters.eq("a", 1), Filters.expr("minus[1] + 2 > 0")).toString());
        assertEquals("SELECT id FROM t WHERE offset[1]::int > 5", Filters.subQuery("t", Arrays.asList("id"), Filters.expr("offset[1]::int > 5")).toString());
        assertEquals("SELECT id FROM t WHERE limit[1] > 0", Filters.subQuery("t", Arrays.asList("id"), Filters.expr("limit[1] > 0")).toString());
        assertEquals("SELECT id FROM t WHERE minus[1] + 2 > 0", Dsl.PSC.select("id").from("t").where(Filters.expr("minus[1] + 2 > 0")).build().query());

        // Keywords reserved everywhere, and the unglued form, are still clauses.
        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.eq("a", 1), Filters.expr("where[1] > 0")));
        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.eq("a", 1), Filters.expr("union[1] > 0")));
        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.eq("a", 1), Filters.expr("minus + 2 > 0")));
        assertEquals("SELECT id FROM t OFFSET 5", Filters.subQuery("t", Arrays.asList("id"), Filters.expr("OFFSET 5")).toString());
    }

    @Test
    void testNot() {
        Equal cond = new Equal("status", "active");
        Not result = cond.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
        assertSame(cond, result.condition());
    }

    @Test
    void testNullOperatorHandling() {
        // Test condition with null operator (through default constructor)
        AbstractCondition condition = new AbstractCondition() {
            @Override
            public ImmutableList<Object> parameters() {
                return ImmutableList.empty();
            }

            @Override
            public String toSql(NamingPolicy namingPolicy) {
                return "NULL_OP";
            }
        };

        assertNull(condition.operator());
    }

    @Test
    void testOffsetAndForUpdateCannotBeComposedAsPredicates() {
        TestCondition offset = new TestCondition(Operator.OFFSET, "10");
        TestCondition forUpdate = new TestCondition(Operator.FOR_UPDATE, "");

        assertThrows(IllegalArgumentException.class, () -> offset.and(Filters.eq("id", 1)));
        assertThrows(IllegalArgumentException.class, () -> forUpdate.or(Filters.eq("id", 1)));
    }

    @Test
    void testOr() {
        Equal cond1 = new Equal("status", "active");
        Equal cond2 = new Equal("status", "pending");

        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Operator.OR, result.operator());
        assertEquals(Integer.valueOf(2), result.conditions().size());
    }

    @Test
    void testOr_NullCondition() {
        Equal cond = new Equal("status", "active");
        assertThrows(IllegalArgumentException.class, () -> cond.or(null));
    }

    @Test
    void testOuterJoinExpressionsAreRecognizedAsClauses() {
        assertTrue(AbstractCondition.isClause(Filters.expr("LEFT OUTER JOIN orders o ON o.user_id = u.id")));
        assertTrue(AbstractCondition.isClause(Filters.expr("right /* hint */ outer join orders o on o.user_id = u.id")));
        assertTrue(AbstractCondition.isClause(Filters.expr("FULL OUTER JOIN orders o ON o.user_id = u.id")));

        assertThrows(IllegalArgumentException.class,
                () -> Filters.eq("active", true).and(Filters.expr("LEFT OUTER JOIN orders o ON o.user_id = u.id")));
    }

    @Test
    void testParameter2String_Null() {
        Equal condition = new Equal("name", null);
        List<Object> params = condition.parameters();
        assertEquals(0, params.size());
    }

    @Test
    void testParameter2String_PreservesDoubleQuotesAndBackslashes() {
        assertEquals("'a\"b\\c'", AbstractCondition.formatParameter("a\"b\\c", NamingPolicy.NO_CHANGE));
        assertEquals("'x'' OR 1=1 --'", AbstractCondition.formatParameter("x' OR 1=1 --", NamingPolicy.NO_CHANGE));
    }

    @Test
    void testParameter2String_StringUsesSqlStandardQuoteDoubling() {
        Equal condition = new Equal("name", "O'Brien");
        String str = condition.toSql(NamingPolicy.NO_CHANGE);
        assertEquals("name = 'O''Brien'", str);
    }

    @Test
    void testParameter2String_WithCondition() {
        Equal innerCondition = new Equal("id", 100);
        String str = AbstractCondition.formatParameter(innerCondition, NamingPolicy.NO_CHANGE);
        assertEquals("id = 100", str);
    }

    @Test
    void testParameter2String_WithIsNull() {
        Equal condition = new Equal("field", IsNull.NULL);
        String str = condition.toSql(NamingPolicy.NO_CHANGE);
        assertNotNull(str);
        assertTrue(str.contains("NULL"));
    }

    @Test
    void testParameter2String_WithSubQueryAddsParentheses() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Equal outerCondition = new Equal("userId", subQuery);
        String str = outerCondition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("= (SELECT id FROM users)"));
    }

    @Test
    void testParameter2StringWithCondition() {
        Equal eq = Filters.eq("name", "John");
        String result = AbstractCondition.formatParameter(eq, NamingPolicy.NO_CHANGE);
        assertEquals("name = 'John'", result);
    }

    @Test
    void testParameter2StringWithConditionAndNamingPolicy() {
        Equal eq = Filters.eq("firstName", "John");
        String result = AbstractCondition.formatParameter(eq, NamingPolicy.SNAKE_CASE);
        assertEquals("first_name = 'John'", result);
    }

    @Test
    void testParameter2StringWithNull() {
        String result = AbstractCondition.formatParameter(null, NamingPolicy.NO_CHANGE);
        assertNull(result);
    }

    @Test
    void testParameter2StringWithNumber() {
        String result = AbstractCondition.formatParameter(123, NamingPolicy.NO_CHANGE);
        assertEquals("123", result);
    }

    @Test
    void testParameter2StringWithString() {
        String result = AbstractCondition.formatParameter("test", NamingPolicy.NO_CHANGE);
        assertEquals("'test'", result);
    }

    @Test
    void testPrimitiveArraySnapshotsKeepTheirTypeAndStorageIsolated() {
        for (final Object value : List.of(new boolean[] { true, false }, new byte[] { 1, 2 }, new short[] { 1, 2 }, new char[] { 'a', 'b' }, new int[] { 1, 2 },
                new long[] { 1, 2 }, new float[] { 1, 2 }, new double[] { 1, 2 })) {
            final Object copy = AbstractCondition.snapshotMutableValue(value);
            assertNotSame(value, copy);
            assertEquals(value.getClass(), copy.getClass());
            assertEquals(Array.get(value, 0), Array.get(copy, 0));
            assertEquals(Array.get(value, 1), Array.get(copy, 1));

            final Object firstValue = Array.get(value, 0);
            Array.set(copy, 0, Array.get(copy, 1));
            assertEquals(Array.get(value, 1), Array.get(copy, 0));
            assertEquals(firstValue, Array.get(value, 0));
            assertEquals(2, Array.getLength(value));
        }
    }

    @Test
    void testPropertyLineCommentsDoNotConsumePredicateSyntax() {
        final String propName = "id -- trailing comment";
        final String prefix = propName + "\n ";

        assertEquals(prefix + "= 1", new Equal(propName, 1).toString());
        assertEquals(prefix + "IS NULL", new Equal(propName, null).toString());
        assertEquals(prefix + "BETWEEN 1 AND 2", new Between(propName, 1, 2).toString());
        assertEquals(prefix + "IN (1, 2)", new In(propName, List.of(1, 2)).toString());
        assertEquals(prefix + "IN (SELECT id FROM users)", new InSubQuery(propName, new SubQuery("SELECT id FROM users")).toString());
        assertEquals("(" + propName + "\n, tenantId) IN ((1, 2))", new In(List.of(propName, "tenantId"), List.of(List.of(1, 2))).toString());
        assertEquals("\"id--tail\" = 1", new Equal("\"id--tail\"", 1).toString());
        assertEquals(prefix + "= 1", new Equal(propName + "\n", 1).toString());
    }

    @Test
    void testProtectedHelpers_NullArgumentsThrowIllegalArgumentException() {
        // previously threw NullPointerException
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.formatNumberLiteral(null));
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.validateNonQuantifiedValueOperands(null, "values"));

        assertEquals("42", AbstractCondition.formatNumberLiteral(42));
        AbstractCondition.validateNonQuantifiedValueOperands(Arrays.asList(1, null, "a"), "values");
    }

    @Test
    void testRealClausesStillClassifiedAsClausesAfterOperatorWidening() {
        for (final String clause : new String[] { "OFFSET 5 ROWS", "OFFSET 5", "WHERE a = 1", "LIMIT 10", "UNION SELECT 1", "ORDER BY x", "GROUP BY x",
                "WHERE ~flags & 4 = 0", "WHERE glob = 1", "WHERE regexp IS NULL", "WHERE rlike NOT IN (1)", "WHERE NOT regexp = 1", "WHERE similar = 1",
                "WHERE glob", "HAVING COUNT(*) > 1", "EXCEPT SELECT 1", "MINUS SELECT 1" }) {
            assertTrue(AbstractCondition.isClause(Filters.expr(clause)), clause);
        }

        assertThrows(IllegalArgumentException.class, () -> Filters.where(Filters.expr("OFFSET 5 ROWS")));
        assertThrows(IllegalArgumentException.class, () -> Filters.where(Filters.expr("WHERE glob = 1")));
        assertThrows(IllegalArgumentException.class, () -> Filters.and(Filters.expr("WHERE ~flags = 0"), Filters.eq("a", 1)));
    }

    @Test
    void testReferenceArraySnapshotsCopyMutableLeavesAndAllowRepeatedReferences() {
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1_000L);
        final Date date = new Date(2_000L);
        final Object[] shared = { new byte[] { 1, 2 }, date, calendar, null };
        final Object[][] input = { shared, shared };

        final Object[][] copy = (Object[][]) AbstractCondition.snapshotMutableValue(input);
        assertNotSame(input, copy);
        assertNotSame(shared, copy[0]);
        assertNotSame(copy[0], copy[1]);
        assertEquals(input.getClass(), copy.getClass());
        assertNull(copy[0][3]);

        ((byte[]) copy[0][0])[0] = 9;
        ((Date) copy[0][1]).setTime(9_000L);
        ((Calendar) copy[0][2]).setTimeInMillis(9_000L);

        for (final Object[] row : new Object[][] { shared, copy[1] }) {
            assertArrayEquals(new byte[] { 1, 2 }, (byte[]) row[0]);
            assertEquals(2_000L, ((Date) row[1]).getTime());
            assertEquals(1_000L, ((Calendar) row[2]).getTimeInMillis());
        }
    }

    @Test
    void testReservedClauseKeywordFollowedBySpacedUnaryTildeIsAClause() {
        // WHERE/HAVING/UNION/JOIN are reserved everywhere, so "~ " after them is unary bitwise NOT, not a regex match
        // on a column named "where" (previously Filters.where rendered "WHERE WHERE ~ flags & 4 = 0").
        for (final String clause : new String[] { "WHERE ~ flags & 4 = 0", "HAVING ~ flags = 0", "where = 1", "UNION ~ x", "JOIN ~ x" }) {
            assertTrue(AbstractCondition.isClause(Filters.expr(clause)), clause);
        }

        assertThrows(IllegalArgumentException.class, () -> Filters.where("WHERE ~ flags & 4 = 0"));
        assertThrows(IllegalArgumentException.class, () -> Filters.where("HAVING ~ flags = 0"));

        // Keywords that are non-reserved somewhere keep the identifier carve-out.
        assertFalse(AbstractCondition.isClause(Filters.expr("offset ~ 'x'")));
        assertFalse(AbstractCondition.isClause(Filters.expr("minus IS NULL")));
    }

    // Same wildcard detection for scalar and quantified (ALL/ANY/SOME) operands.
    @Test
    void testScalarAndQuantifiedOperandsLeaveCommentedWildcardArityToTheDatabase() {
        final SubQuery unknownArity = new SubQuery("users", List.of("id", "users. /* columns */ *"), null);

        assertSame(unknownArity, assertDoesNotThrow(() -> new Equal("id", unknownArity)).propValue());
        assertSame(unknownArity, assertDoesNotThrow(() -> new All(unknownArity)).subQuery());
        assertSame(unknownArity, assertDoesNotThrow(() -> new Any(unknownArity)).subQuery());
        assertSame(unknownArity, assertDoesNotThrow(() -> new Some(unknownArity)).subQuery());
    }

    @Test
    void testScalarSnapshotsRetainTheirIdentity() {
        assertNull(AbstractCondition.snapshotMutableValue(null));

        for (final Object value : List.of("literal", 42, Boolean.TRUE, new Object())) {
            assertSame(value, AbstractCondition.snapshotMutableValue(value));
        }
    }

    @Test
    void testScalarSubqueryOperandsAcceptSingleColumnsAndRetainBindings() {
        final SubQuery singleColumn = new SubQuery("accounts", "id", new Equal("active", true));
        assertEquals("accountId = (SELECT id FROM accounts WHERE active = true)", new Equal("accountId", singleColumn).toString());
        assertEquals(Arrays.asList(true, 10), new Between("accountId", singleColumn, 10).parameters());
        assertEquals(Arrays.asList(1, true), new NotBetween("accountId", 1, singleColumn).parameters());
        assertEquals(Arrays.asList(true), new In("accountId", Arrays.asList(singleColumn)).parameters());
        assertEquals(Arrays.asList(1, true), new NotIn("accountId", Arrays.asList(1, singleColumn)).parameters());
        assertEquals(Arrays.asList(true, 2), new In(Arrays.asList("accountId", "tenantId"), Arrays.asList(Arrays.asList(singleColumn, 2))).parameters());
        assertEquals(Arrays.asList(1, true), new NotIn(Arrays.asList("accountId", "tenantId"), Arrays.asList(Arrays.asList(1, singleColumn))).parameters());
    }

    @Test
    void testScalarSubqueryOperandsLeaveRawWildcardAndBuilderProjectionArityUnchecked() {
        // Builder snapshots retain SQL and bindings, but expose no projection metadata to this validator.
        final SubQuery builderSnapshot = Dsl.PSC.select("id", "tenantId").from("accounts").where(new Equal("active", true)).toSubQuery();
        assertNull(builderSnapshot.selectPropNames());

        // Raw text is not parsed for column count, and a wildcard can expand beyond its list entry.
        for (final SubQuery unknownArity : Arrays.asList(new SubQuery("SELECT id, tenantId FROM accounts WHERE active = ?", Arrays.asList(true)),
                new SubQuery("accounts", "*", new Equal("active", true)), new SubQuery("accounts a", Arrays.asList("id", "a.*"), new Equal("active", true)),
                builderSnapshot)) {
            assertSame(unknownArity, new Equal("accountId", unknownArity).propValue());
            assertSame(unknownArity, new Binary("accountId", Operator.IN, unknownArity).propValue());
            assertSame(unknownArity, new Binary("accountId", Operator.NOT_IN, unknownArity).propValue());
            assertSame(unknownArity, new Between("accountId", unknownArity, 10).minValue());
            assertSame(unknownArity, new NotBetween("accountId", 1, unknownArity).maxValue());
            assertSame(unknownArity, new In("accountId", Arrays.asList(unknownArity)).values().get(0));
            assertSame(unknownArity, new NotIn("accountId", Arrays.asList(unknownArity)).values().get(0));
            assertEquals(Arrays.asList(true), new Equal("accountId", unknownArity).parameters());
        }
    }

    @Test
    void testScalarSubqueryOperandsRejectKnownMultiColumnProjections() {
        final SubQuery multiColumn = new SubQuery("accounts", Arrays.asList("id", "tenantId"), null);

        for (final Operator operator : Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.NOT_EQUAL_ANSI, Operator.GREATER_THAN,
                Operator.GREATER_THAN_OR_EQUAL, Operator.LESS_THAN, Operator.LESS_THAN_OR_EQUAL, Operator.LIKE, Operator.NOT_LIKE)) {
            assertThrows(IllegalArgumentException.class, () -> new Binary("accountId", operator, multiColumn), operator.toString());
        }

        assertThrows(IllegalArgumentException.class, () -> new Equal("accountId", multiColumn));
        assertThrows(IllegalArgumentException.class, () -> new GreaterThan("accountId", multiColumn));

        for (final Operator operator : Arrays.asList(Operator.IN, Operator.NOT_IN)) {
            assertThrows(IllegalArgumentException.class, () -> new Binary("accountId", operator, multiColumn), operator.toString());
            assertThrows(IllegalArgumentException.class, () -> new Binary("accountId", operator, Arrays.asList(1, multiColumn)), operator.toString());
            assertThrows(IllegalArgumentException.class, () -> new Binary("accountId", operator, new Object[] { 1, multiColumn }), operator.toString());
        }

        assertThrows(IllegalArgumentException.class, () -> new Between("accountId", multiColumn, 10));
        assertThrows(IllegalArgumentException.class, () -> new Between("accountId", 1, multiColumn));
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("accountId", multiColumn, 10));
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("accountId", 1, multiColumn));
        assertThrows(IllegalArgumentException.class, () -> new In("accountId", Arrays.asList(1, multiColumn)));
        assertThrows(IllegalArgumentException.class, () -> new NotIn("accountId", Arrays.asList(1, multiColumn)));
        assertThrows(IllegalArgumentException.class, () -> new In(Arrays.asList("accountId", "tenantId"), Arrays.asList(Arrays.asList(multiColumn, 2))));
        assertThrows(IllegalArgumentException.class, () -> new NotIn(Arrays.asList("accountId", "tenantId"), Arrays.asList(Arrays.asList(1, multiColumn))));
    }

    @Test
    void testSortCollectionRejectsAnEmptySnapshot() {
        final Collection<String> propNames = new AbstractCollection<>() {
            @Override
            public Iterator<String> iterator() {
                return List.<String> of().iterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortSpec(propNames, SortDirection.ASC));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy(propNames, SortDirection.ASC));
        assertThrows(IllegalArgumentException.class, () -> new GroupBy(propNames, SortDirection.ASC));
    }

    @Test
    void testSortCollectionRendersTheValidatedSnapshot() {
        for (final boolean grouping : new boolean[] { false, true }) {
            final Collection<String> propNames = new AbstractCollection<>() {
                private int traversal;

                @Override
                public Iterator<String> iterator() {
                    return (traversal++ == 0 ? List.of("firstName", "lastName") : List.of(" ")).iterator();
                }

                @Override
                public int size() {
                    return 2;
                }
            };

            final Condition condition = grouping ? new GroupBy(propNames, SortDirection.DESC) : new OrderBy(propNames, SortDirection.DESC);
            assertEquals((grouping ? "GROUP BY" : "ORDER BY") + " first_name DESC, last_name DESC", condition.toSql(NamingPolicy.SNAKE_CASE));
        }
    }

    @Test
    void testSortKeyWithUnterminatedBlockCommentIsRejected() {
        // Regression: an unclosed "/*" swallowed the key's direction and every later key, and the renderer silently
        // dropped the comment ("ORDER BY name " instead of "ORDER BY name DESC").
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("name /*", SortDirection.DESC));
        assertThrows(IllegalArgumentException.class, () -> new OrderBy("a /* x", "b"));
        assertThrows(IllegalArgumentException.class, () -> new GroupBy(Arrays.asList("dept /* x", "team"), SortDirection.DESC));
        assertThrows(IllegalArgumentException.class, () -> Criteria.builder().groupBy("dept /* x", "team"));
        assertThrows(IllegalArgumentException.class, () -> Filters.orderBy("name /*/", SortDirection.DESC));

        // Closed block comments and comment openers inside literals are still accepted.
        assertTrue(new OrderBy("name /* c */", SortDirection.DESC).toString().endsWith("DESC"));
        assertTrue(new OrderBy("COALESCE(name, '/*')", SortDirection.DESC).toString().endsWith("DESC"));
        assertTrue(new OrderBy("name -- /*\n", SortDirection.DESC).toString().endsWith("DESC"));
    }

    @Test
    void testSortLineCommentsDoNotConsumeDirectionsOrLaterColumns() {
        final String propName = "firstName -- trailing comment";
        assertEquals(propName + "\n, lastName", AbstractCondition.createSortSpec(propName, "lastName"));
        assertEquals(propName + "\n DESC", AbstractCondition.createSortSpec(propName, SortDirection.DESC));
        assertEquals(propName + "\n DESC, lastName DESC", AbstractCondition.createSortSpec(List.of(propName, "lastName"), SortDirection.DESC));
        assertEquals(propName + "\n DESC", AbstractCondition.createSortSpec(java.util.Map.of(propName, SortDirection.DESC)));
        assertEquals("ORDER BY first_name DESC, last_name DESC", new OrderBy(List.of(propName, "lastName"), SortDirection.DESC).toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    void testStandaloneSubQueryIsRejectedButExistsPredicateIsAllowed() {
        SubQuery subQuery = new SubQuery("SELECT id FROM users");
        Equal predicate = Filters.eq("active", true);

        assertThrows(IllegalArgumentException.class, () -> predicate.and(subQuery));
        assertThrows(IllegalArgumentException.class, () -> new Where(subQuery));
        assertThrows(IllegalArgumentException.class, () -> new On(subQuery));

        Exists exists = new Exists(subQuery);
        assertDoesNotThrow(() -> predicate.and(exists));
        assertDoesNotThrow(() -> new Where(exists));
        assertDoesNotThrow(() -> new On(exists));
        assertDoesNotThrow(() -> new Union(subQuery));
    }

    // Controls: a '*' in a literal, quoted identifier, expression or comment does not disable arity validation.
    @Test
    void testStarsInsideLiteralsIdentifiersAndExpressionsDoNotDisableArityValidation() {
        for (final String projection : List.of("COUNT(*) /* rows */", "'users.*'", "\"users.*\"", "users.id * 2 /* product */", "id /* users.* */",
                "id -- ignored.*", "id # ignored.*")) {
            final SubQuery subQuery = new SubQuery("users", List.of(projection), null);

            assertThrows(IllegalArgumentException.class, () -> new InSubQuery(List.of("id", "name"), subQuery), projection);
            assertThrows(IllegalArgumentException.class, () -> new NotInSubQuery(List.of("id", "name"), subQuery), projection);
            assertDoesNotThrow(() -> new Equal("id", subQuery), projection);

            final SubQuery twoColumns = new SubQuery("users", List.of(projection, "id"), null);
            assertThrows(IllegalArgumentException.class, () -> new Equal("id", twoColumns), projection);
            assertThrows(IllegalArgumentException.class, () -> new Any(twoColumns), projection);
        }
    }

    @Test
    void testToString() {
        TestCondition condition = new TestCondition(Operator.IN, "list");

        String result = condition.toString();
        assertEquals("IN list", result);
    }

    @Test
    void testToString_DefaultNamingPolicy() {
        Equal condition = new Equal("userName", "John");
        String result = condition.toString();
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("John"));
    }

    @Test
    void testToString_WithNamingPolicy() {
        Equal condition = new Equal("userName", "John");
        String result = condition.toSql(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
        assertTrue(result.contains("John"));
    }

    @Test
    void testToString_WithNoChangePolicy() {
        Equal condition = new Equal("firstName", "Jane");
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("Jane"));
    }

    // Regression: arity validation recognized only "*" and a trailing ".*", so a wildcard split by whitespace or comments
    // ("users. *") failed tuple IN validation, while a comment ending in ".*" ("id -- x.*") disabled the check.
    @Test
    void testTupleMembershipAcceptsWildcardProjectionsWithWhitespaceAndComments() {
        for (final String projection : List.of("users. *", "users /* table */ . *", "* /* all columns */", "users.* -- all columns",
                "\"some table\" . /* columns */ *", "users.\n\t*", "/* columns */ *", "#users.*", "#users. *", "##users . *")) {
            final SubQuery subQuery = new SubQuery("users", List.of(projection), new Equal("active", true));

            assertSame(subQuery, assertDoesNotThrow(() -> new InSubQuery(List.of("id", "name"), subQuery), projection).subQuery());
            assertSame(subQuery, assertDoesNotThrow(() -> new NotInSubQuery(List.of("id", "name"), subQuery), projection).subQuery());
        }
    }

    // A whitespace-separated wildcard renders through the builder with its parameters.
    @Test
    void testWildcardMembershipRetainsParametersWhenRenderedThroughTheBuilder() {
        final SubQuery subQuery = new SubQuery("users", List.of("users. *"), new Equal("active", true));
        final InSubQuery in = assertDoesNotThrow(() -> new InSubQuery(List.of("id", "name"), subQuery));
        final NotInSubQuery notIn = assertDoesNotThrow(() -> new NotInSubQuery(List.of("id", "name"), subQuery));

        assertEquals(List.of(true), in.parameters());
        assertEquals(List.of(true), notIn.parameters());

        final var inSql = Dsl.PSC.select("id").from("accounts").where(in).build();
        final var notInSql = Dsl.PSC.select("id").from("accounts").where(notIn).build();

        assertEquals("SELECT id FROM accounts WHERE (id, name) IN (SELECT users. * FROM users WHERE active = ?)", inSql.query());
        assertEquals("SELECT id FROM accounts WHERE (id, name) NOT IN (SELECT users. * FROM users WHERE active = ?)", notInSql.query());
        assertEquals(List.of(true), inSql.parameters());
        assertEquals(List.of(true), notInSql.parameters());
    }

    @Nested
    class Validation {
        @Test
        void testNullSortEntriesAreRejectedBeforeDereference() {
            final Map<String, SortDirection> orders = new AbstractMap<>() {
                @Override
                public Set<Entry<String, SortDirection>> entrySet() {
                    return Collections.singleton(null);
                }
            };

            assertThrows(IllegalArgumentException.class, () -> AbstractCondition.createSortSpec(orders));
            assertThrows(IllegalArgumentException.class, () -> new OrderBy(orders));
            assertThrows(IllegalArgumentException.class, () -> new GroupBy(orders));
            assertThrows(IllegalArgumentException.class, () -> Criteria.builder().orderBy(orders));
            assertThrows(IllegalArgumentException.class, () -> Criteria.builder().groupBy(orders));
        }

        @Test
        void testSubqueryArityHelperRejectsNullAsAnArgumentError() {
            assertThrows(IllegalArgumentException.class, () -> AbstractCondition.validateSubQuerySelectArity(1, null));
            AbstractCondition.validateSubQuerySelectArity(1, new SubQuery("SELECT id FROM users"));
            AbstractCondition.validateSubQuerySelectArity(1, new SubQuery("users", List.of("id"), null));
        }
    }

    private static Number customNumber(final String literal) {
        return new Number() {
            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0;
            }

            @Override
            public float floatValue() {
                return 0;
            }

            @Override
            public double doubleValue() {
                return 0;
            }

            @Override
            public String toString() {
                return literal;
            }
        };
    }

    // Create a concrete implementation for testing
    private static class TestCondition extends ComposableCondition {
        private String value;

        public TestCondition(Operator operator, String value) {
            super(operator);
            this.value = value;
        }

        @Override
        public ImmutableList<Object> parameters() {
            return value == null ? ImmutableList.empty() : ImmutableList.of(value);
        }

        @Override
        public String toSql(NamingPolicy namingPolicy) {
            return operator().toString() + " " + value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TestCondition)) {
                return false;
            }
            TestCondition other = (TestCondition) obj;
            return Objects.equals(operator, other.operator) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operator, value);
        }
    }
}
