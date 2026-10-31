package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.OperationType;

class QueryBuilderSelectionTuningTest {
    @Test
    void testCustomCollectionMembershipKeepsTheGeneralPath() throws Exception {
        final List<String> custom = new ArrayList<>(Collections.nCopies(32, "id")) {
            @Override
            public boolean contains(final Object value) {
                return "right".equals(value) || super.contains(value);
            }
        };
        final Map<?, ?> selected = listedRoots(ParserUtil.getBeanInfo(WideRoot.class), custom, NamingPolicy.SNAKE_CASE);
        assertEquals(List.of("right"), new ArrayList<>(selected.keySet()));
    }

    @Test
    void testDirectionalArraysKeepVirtualDispatchAndAnIndependentMutableArrayList() {
        for (int variant = 0; variant < 4; variant++) {
            final String[] input = { "first", "second" };
            final TrackingBuilder builder = new TrackingBuilder();
            builder.from("t");
            applyDirection(builder, input, variant);
            assertArrayEquals(new String[] { "first", "second" }, input);
            assertEquals(ArrayList.class, builder.received.getClass());
            assertEquals(1, builder.calls);
            final String clause = variant < 2 ? "GROUP BY" : "ORDER BY";
            final String direction = variant % 2 == 0 ? "ASC" : "DESC";
            assertEquals("SELECT * FROM t " + clause + " changed " + direction + ", second " + direction + ", extra " + direction,
                    builder.build().query());
            builder.received.clear();
            assertThrows(IllegalStateException.class, () -> builder.orderByAsc((String[]) null));
            assertEquals(1, builder.calls);
        }
    }

    @Test
    void testFiniteLexicalPlansKeepReadingOrderAndAllFailClosedVariants() throws Exception {
        final SqlBuilder owner = Dsl.PSC.select("id").from("t");
        final SqlBuilder postgres = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build())
                .select("id").from("t");
        final SqlBuilder mysql = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build())
                .select("id").from("t");
        try {
            final String allFlags = "##t 'C:\\x' $$v$$ \"q\"\r x";
            final Object[] base = readings(owner, allFlags, true, true);
            assertArrayEquals(new int[] { 2, 0, 18, 7, 5, 23 }, flags(base));
            assertSame(base, readings(owner, "##other 'D:\\y' $$other$$", true, true));
            final Method failClosed = method("withFailClosedVariants", base.getClass(), String.class);
            final Object[] expanded = (Object[]) failClosed.invoke(null, base, allFlags);
            assertArrayEquals(new int[] { 2, 0, 18, 7, 3, 5, 1, 23, 19, 10, 8, 26, 15, 11, 13, 9, 31, 27 }, flags(expanded));
            assertSame(expanded, failClosed.invoke(null, base, allFlags));
            assertArrayEquals(new int[] { 10, 8, 26, 15, 13, 31 },
                    flags((Object[]) method("withLineFeedOnlyLineComments", base.getClass()).invoke(null, (Object) base)));
            assertArrayEquals(new int[] { 2, 18 }, flags(readings(postgres, allFlags, false, true)));
            assertArrayEquals(new int[] { 2, 18, 3, 19 }, flags(readings(postgres, allFlags, true, true)));
            final Object[] mysqlBase = readings(mysql, allFlags, true, true);
            assertArrayEquals(new int[] { 5 }, flags(mysqlBase));
            assertArrayEquals(new int[] { 5, 1, 13, 9 }, flags((Object[]) failClosed.invoke(null, mysqlBase, allFlags)));
            final Object[] trailing = (Object[]) method("trailingLineCommentReadings", String.class, boolean.class).invoke(null, allFlags, false);
            assertArrayEquals(new int[] { 0, 2, 5, 1, 7, 3, 8, 10, 13, 9, 15, 11 }, flags(trailing));
            assertArrayEquals(new int[] { 2, 0, 18, 7, 5, 23 }, flags(base));
        } finally {
            owner.build();
            postgres.build();
            mysql.build();
        }
    }

    @Test
    void testNullDirectionalArraysRemainMutableForOverridesAndBuiltInValidationCanRetry() {
        for (int variant = 0; variant < 4; variant++) {
            final TrackingBuilder builder = new TrackingBuilder();
            builder.from("t");
            applyDirection(builder, null, variant);
            assertEquals(List.of("extra"), builder.received);
            builder.build();
        }

        final SqlBuilder builder = Dsl.PSC.select("*").from("t");
        assertThrows(IllegalArgumentException.class, () -> builder.orderByAsc((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> builder.orderByAsc(new String[] { "id", null }));
        assertEquals("SELECT * FROM t ORDER BY id ASC, name ASC", builder.orderByAsc("id", "name").build().query());
    }

    @Test
    void testWideProjectionKeepsTheSmallProjectionRootSemanticsAndOrder() throws Exception {
        final BeanInfo info = ParserUtil.getBeanInfo(WideRoot.class);
        assertTrue(info.subEntityPropNameList.size() >= 8);
        final List<String> candidates = Arrays.asList("id", "left", " left ", "LEFT", "left.id", "LEFT.id", "right", "RIGHT", "right.id",
                "right.id.deep", "\u2007right\u202F", "right AS r", "left + right", "*", "count(*)", "excluded", "self", " id ", null, "", ".");

        for (final NamingPolicy policy : NamingPolicy.values()) {
            for (final String first : candidates) {
                for (final String second : candidates) {
                    final List<String> narrow = Arrays.asList(first, second);
                    final List<String> wide = new ArrayList<>();
                    for (int repeat = 0; repeat < 16; repeat++) {
                        wide.addAll(narrow);
                    }
                    final Map<?, ?> expected = listedRoots(info, narrow, policy);
                    final Map<?, ?> actual = listedRoots(info, wide, policy);
                    assertEquals(expected, actual, () -> policy + ": " + narrow);
                    assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()), () -> policy + ": " + narrow);
                }
            }
        }
    }

    @Test
    void testWidePublicSelectionsKeepMappedRootsNestedDependenciesAndDuplicateTableRejection() {
        assertEquals("selection_root", fromClause("left"));
        assertEquals("selection_root", fromClause(" left\u3000"));
        assertEquals("selection_root, selection_child c", fromClause("LEFT"));
        assertEquals("selection_root, selection_child c", fromClause("left.id"));
        assertEquals("selection_root, selection_child c", fromClause("\u2007right\u202F"));
        assertEquals("selection_root, selection_child c", fromClause("excluded"));
        assertEquals("selection_root", fromClause("self"));

        final List<String> mixed = wide("left");
        mixed.add("left.id");
        assertEquals("selection_root, selection_child c", AbstractQueryBuilder.getFromClause(
                List.of(Selection.builder(WideRoot.class).includedPropNames(mixed).build()), NamingPolicy.SNAKE_CASE));

        final List<String> duplicates = wide("right");
        duplicates.add("other0.id");
        assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.getFromClause(
                List.of(Selection.builder(WideRoot.class).includedPropNames(duplicates).build()), NamingPolicy.SNAKE_CASE));

        final List<String> projected = wide("left");
        final String expectedColumns = String.join(", ", Collections.nCopies(31, "id AS \"id\"")) + ", left AS \"left\"";
        assertEquals("SELECT " + expectedColumns + " FROM selection_root",
                Dsl.PSC.selectFrom(Selection.builder(WideRoot.class).includedPropNames(projected).build()).build().query());
    }

    private static Map<?, ?> listedRoots(final BeanInfo info, final Collection<String> selections, final NamingPolicy policy) throws Exception {
        return (Map<?, ?>) method("listedSubEntityProps", Class.class, BeanInfo.class, Collection.class, Collection.class, NamingPolicy.class)
                .invoke(null, WideRoot.class, info, info.subEntityPropNameList, selections, policy);
    }

    private static String fromClause(final String item) {
        return AbstractQueryBuilder.getFromClause(List.of(Selection.builder(WideRoot.class).includedPropNames(wide(item)).build()), NamingPolicy.SNAKE_CASE);
    }

    private static List<String> wide(final String item) {
        final List<String> values = new ArrayList<>(Collections.nCopies(31, "id"));
        values.add(item);
        return values;
    }

    private static void applyDirection(final SqlBuilder builder, final String[] values, final int variant) {
        switch (variant) {
            case 0 -> builder.groupByAsc(values);
            case 1 -> builder.groupByDesc(values);
            case 2 -> builder.orderByAsc(values);
            case 3 -> builder.orderByDesc(values);
            default -> throw new AssertionError(variant);
        }
    }

    private static Object[] readings(final SqlBuilder owner, final String sql, final boolean explicitBackslash, final boolean defaultHash) throws Exception {
        return (Object[]) method("lexicalReadings", String.class, boolean.class, boolean.class).invoke(owner, sql, explicitBackslash, defaultHash);
    }

    private static int[] flags(final Object[] readings) throws Exception {
        final int[] result = new int[readings.length];
        final String[] accessors = { "backslashEscapes", "hashPairTokens", "doubleQuotedBackslashEscapes", "lineFeedOnlyLineComments", "dollarQuotes" };
        for (int i = 0; i < readings.length; i++) {
            for (int bit = 0; bit < accessors.length; bit++) {
                final Method accessor = readings[i].getClass().getDeclaredMethod(accessors[bit]);
                accessor.setAccessible(true);
                if ((boolean) accessor.invoke(readings[i])) {
                    result[i] |= 1 << bit;
                }
            }
        }
        return result;
    }

    private static Method method(final String name, final Class<?>... types) throws Exception {
        final Method method = AbstractQueryBuilder.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }

    private static final class TrackingBuilder extends SqlBuilder {
        private List<String> received;
        private int calls;

        TrackingBuilder() {
            super(Dsl.PSC.sqlDialect());
            _op = OperationType.QUERY;
            _propOrColumnNames = List.of("*");
        }

        private void inspect(final Collection<String> values) {
            received = (List<String>) values;
            calls++;
            if (!received.isEmpty()) {
                received.set(0, "changed");
            }
            received.add("extra");
        }

        @Override
        public SqlBuilder groupBy(final Collection<String> values, final SortDirection direction) {
            inspect(values);
            return super.groupBy(values, direction);
        }

        @Override
        public SqlBuilder orderBy(final Collection<String> values, final SortDirection direction) {
            inspect(values);
            return super.orderBy(values, direction);
        }
    }

    @Table(name = "selection_root", nonColumnFields = { "excluded" })
    public static class WideRoot {
        private int id;
        @Column("left")
        private int leftId;
        private Child left;
        private Child right;
        private Child other0;
        private Child other1;
        private Child other2;
        private Child other3;
        private Child excluded;
        private WideRoot self;

        public int getId() { return id; }
        public void setId(final int value) { id = value; }
        public int getLeftId() { return leftId; }
        public void setLeftId(final int value) { leftId = value; }
        public Child getLeft() { return left; }
        public void setLeft(final Child value) { left = value; }
        public Child getRight() { return right; }
        public void setRight(final Child value) { right = value; }
        public Child getOther0() { return other0; }
        public void setOther0(final Child value) { other0 = value; }
        public Child getOther1() { return other1; }
        public void setOther1(final Child value) { other1 = value; }
        public Child getOther2() { return other2; }
        public void setOther2(final Child value) { other2 = value; }
        public Child getOther3() { return other3; }
        public void setOther3(final Child value) { other3 = value; }
        public Child getExcluded() { return excluded; }
        public void setExcluded(final Child value) { excluded = value; }
        public WideRoot getSelf() { return self; }
        public void setSelf(final WideRoot value) { self = value; }
    }

    @Table(name = "selection_child", alias = "c")
    public static class Child {
        private int id;
        public int getId() { return id; }
        public void setId(final int value) { id = value; }
    }
}
