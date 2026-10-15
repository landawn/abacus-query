package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class JoinTest extends TestBase {

    /** A column-to-column ON predicate: renders {@code ON a.id = b.id} and binds no parameters. */
    private static final On ON_AB = Filters.on("a.id", "b.id");

    @Test
    void testComplexCondition() {
        And andCondition = new And(Arrays.asList(new Equal("o.customer_id", "c.id"), new GreaterThan("o.total", 100)));
        Join join = new Join("orders o", andCondition);
        assertEquals(2, join.parameters().size());
    }

    @Test
    void testConstructor_MultipleEntities() {
        Join join = new Join(Arrays.asList("orders o", "customers c"), new Equal("o.customer_id", "c.id"));
        assertNotNull(join);
        assertEquals(2, join.joinEntities().size());
    }

    @Test
    void testConstructor_SimpleJoin() {
        Join join = new Join("orders", ON_AB);
        assertNotNull(join);
        assertEquals(Operator.JOIN, join.operator());
    }

    @Test
    void testConstructor_WithCondition() {
        Join join = new Join("orders o", new Equal("customers.id", "o.customer_id"));
        assertNotNull(join);
        assertNotNull(join.condition());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testConstructorRejectsBlankJoinEntitiesAndBlankExpressionCondition() {
        assertThrows(IllegalArgumentException.class, () -> new Join("   "));
        assertThrows(IllegalArgumentException.class, () -> new Join("orders", Filters.expr("   ")));
        assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders", "   "), Filters.eq("a", "b")));
    }

    @Test
    void testConstructorRejectsClauseCondition() {
        assertThrows(IllegalArgumentException.class, () -> new Join("orders", Filters.where(Filters.eq("a", 1))));
    }

    @Test
    void testConstructorRejectsNestedOnConnectorCondition() {
        Condition nestedOn = new Junction(Operator.AND, Arrays.asList(Filters.on("sales.product_id", "products.id"), Filters.eq("products.active", true)),
                true);

        assertThrows(IllegalArgumentException.class, () -> new Join("products", nestedOn));
    }

    @Test
    void testConstructorRejectsNonJoinOperator() {
        assertThrows(IllegalArgumentException.class, () -> new Join(Operator.EQUAL, "t1", null));
    }

    @Test
    void testConstructorRejectsQuantifiedSubQueryOperand() {
        // ANY/ALL/SOME are quantified-subquery operands (valid only on the RHS of a comparison);
        // "JOIN t1 ON ANY (SELECT ...)" is invalid SQL in every dialect, so the constructor must
        // reject them, consistent with Clause and the composable and()/or()/not() operations.
        SubQuery sub = Filters.subQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> new Join("t1", Filters.any(sub)));
        assertThrows(IllegalArgumentException.class, () -> new Join("t1", Filters.all(sub)));
        assertThrows(IllegalArgumentException.class, () -> new Join("t1", Filters.some(sub)));
        assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("t1", "t2"), Filters.any(sub)));
        assertThrows(IllegalArgumentException.class, () -> Filters.join("t1", Filters.any(sub)));
        assertThrows(IllegalArgumentException.class, () -> Filters.innerJoin("t1", Filters.all(sub)));
        assertThrows(IllegalArgumentException.class, () -> Filters.leftJoin("t1", Filters.some(sub)));
        assertThrows(IllegalArgumentException.class, () -> Filters.rightJoin("t1", Filters.any(sub)));
        assertThrows(IllegalArgumentException.class, () -> Filters.fullJoin("t1", Filters.all(sub)));

        // On/Using instances are still accepted; a null condition is rejected for a qualified JOIN.
        assertNotNull(new Join("t1", new On("a.id", "b.id")));
        assertNotNull(new Join("t1", new Using("id")));
        assertThrows(IllegalArgumentException.class, () -> new Join("t1", null));
    }

    @Test
    void testConstructorRejectsRawOnUsingExpressionsButAllowsExplicitConnectors() {
        assertThrows(IllegalArgumentException.class, () -> new Join("orders", Filters.expr("ON users.id = orders.user_id")));
        assertThrows(IllegalArgumentException.class, () -> new Join("orders", Filters.expr("USING (id)")));

        assertDoesNotThrow(() -> new Join("orders", Filters.on("users.id", "orders.user_id")));
        assertDoesNotThrow(() -> new Join("orders", Filters.using("id")));
    }

    @Test
    void testConstructorRejectsWrappedNonPredicateComponents() {
        final SubQuery subQuery = new SubQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> new Join("orders", new TestComposableWrapper(new OrderBy("name"))));
        assertThrows(IllegalArgumentException.class, () -> new Join("orders", new TestComposableWrapper(new Any(subQuery))));
        assertThrows(IllegalArgumentException.class, () -> new Join("orders", new TestComposableWrapper(new SqlExpression(" "))));
        assertThrows(IllegalArgumentException.class, () -> new Join("orders", new Equal()));
        assertThrows(IllegalArgumentException.class, () -> new Join("orders", new On()));
        assertDoesNotThrow(() -> new Join("orders", new TestComposableWrapper(new Equal("customers.id", "orders.customer_id"))));
    }

    @Test
    void testConstructorValidatesCopiedJoinEntitySnapshotIsNotEmpty() {
        Collection<String> inconsistentCollection = new java.util.AbstractCollection<>() {
            @Override
            public java.util.Iterator<String> iterator() {
                return java.util.Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> new Join(inconsistentCollection, ON_AB));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testConstructorWithInvalidJoinEntity() {
        assertThrows(IllegalArgumentException.class, () -> new Join((String) null));
        assertThrows(IllegalArgumentException.class, () -> new Join(""));
        assertThrows(IllegalArgumentException.class, () -> new Join((String) null, Filters.eq("a", "b")));
        assertThrows(IllegalArgumentException.class, () -> new Join("", Filters.eq("a", "b")));
    }

    @Test
    void testConstructorWithJoinEntity() {
        Join join = new Join("products", ON_AB);

        assertNotNull(join);
        assertEquals(Operator.JOIN, join.operator());
        assertEquals(1, join.joinEntities().size());
        assertEquals("products", join.joinEntities().get(0));
        assertSame(ON_AB, join.condition());
    }

    @Test
    void testConstructorWithJoinEntityAndCondition() {
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        Join join = new Join("orders o", condition);

        assertNotNull(join);
        assertEquals(Operator.JOIN, join.operator());
        assertEquals("orders o", join.joinEntities().get(0));
        assertEquals(condition, join.condition());
    }

    @Test
    void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("orders o", "customers c");
        Condition condition = Filters.eq("o.customer_id", "c.id");
        Join join = new Join(entities, condition);

        assertNotNull(join);
        assertEquals(2, join.joinEntities().size());
        assertTrue(join.joinEntities().containsAll(entities));
        assertEquals(condition, join.condition());
    }

    @Test
    void testConstructorWithMultipleEntitiesRejectsInvalidElements() {
        Condition condition = Filters.eq("o.customer_id", "c.id");

        assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders o", null), condition));
        assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders o", ""), condition));
        assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders o", "   "), condition));
    }

    @Test
    void testDefaultConstructor_EmptyState() {
        Join join = new Join();
        Join same = new Join();

        assertTrue(join.joinEntities().isEmpty());
        assertTrue(join.parameters().isEmpty());
        assertEquals(join, same);
        assertEquals(join.hashCode(), same.hashCode());
    }

    @Test
    void testDefaultConstructorToString() {
        Join join = new Join();

        assertNotNull(join.toSql(NamingPolicy.NO_CHANGE));
        assertNotNull(join.toString());
        assertNotNull(join.toSql(null));
    }

    @Test
    void testDefaultConstructorToStringHasNoTrailingSpace() {
        // Bug fix: previously rendered "null " (literal null + trailing space) when both operator
        // and join entities were null. The trailing space combined with the "null" placeholder
        // produced invalid SQL fragments. Now should render as plain "null" with no trailing space.
        Join join = new Join();

        String rendered = join.toSql(NamingPolicy.NO_CHANGE);
        assertEquals("null", rendered);
        assertFalse(rendered.endsWith(" "), "toString() must not end with a trailing space");
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDeprecatedSingleArgConstructorAlwaysThrowsPredicateMessage() {
        // The single-argument qualified-join constructor cannot succeed: a plain JOIN needs an ON/USING predicate.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Join("orders"));
        assertTrue(ex.getMessage().contains("JOIN requires a non-null ON/USING predicate"), ex.getMessage());
        assertTrue(ex.getMessage().contains("use CROSS JOIN for an unconditional join"), ex.getMessage());

        // The protected condition-less form is rejected for every non-CROSS/NATURAL operator with the same message.
        final IllegalArgumentException left = assertThrows(IllegalArgumentException.class, () -> new TestJoin(Operator.LEFT_JOIN, "orders"));
        assertTrue(left.getMessage().contains("LEFT JOIN requires a non-null ON/USING predicate"), left.getMessage());

        // The advertised alternatives work.
        assertEquals("JOIN orders ON a.id = b.id", new Join("orders", ON_AB).toSql(NamingPolicy.NO_CHANGE));
        assertEquals("CROSS JOIN orders", new CrossJoin("orders").toSql(NamingPolicy.NO_CHANGE));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testDeprecatedSingleArgumentCtorsReportUnterminatedBlockCommentFirst() {
        // Documented: an invalid entity "is reported first", before the always-thrown missing-predicate rejection.
        final String expected = "joinEntity must not contain an unterminated block comment: t /* x";
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> new Join("t /* x")).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> new InnerJoin("t /* x")).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> new LeftJoin("t /* x")).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> new RightJoin("t /* x")).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> new FullJoin("t /* x")).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Filters.leftJoin("t /* x")).getMessage());

        // A valid entity still gets the missing-predicate rejection.
        assertEquals("JOIN requires a non-null ON/USING predicate; use CROSS JOIN for an unconditional join",
                assertThrows(IllegalArgumentException.class, () -> new Join("t")).getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testEntityValidationPrecedesJoinConditionPolicy() {
        // A null/blank entity must be reported as such, not as a missing (or unexpected) join predicate,
        // regardless of the condition supplied and of the join operator.
        final String entityMessage = "must not be null, empty, or blank";
        final String predicateMessage = "requires a non-null ON/USING predicate";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Join((String) null));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
        assertFalse(ex.getMessage().contains(predicateMessage), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new Join("   "));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
        assertFalse(ex.getMessage().contains(predicateMessage), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new Join("", null));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders", "  "), null));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());

        // CROSS/NATURAL with an (unexpected) condition and a blank entity: still the entity message first.
        ex = assertThrows(IllegalArgumentException.class, () -> new TestJoin(Operator.CROSS_JOIN, "   ", ON_AB));
        assertTrue(ex.getMessage().contains(entityMessage), ex.getMessage());
        assertFalse(ex.getMessage().contains("derives its row combinations"), ex.getMessage());

        // An empty entity collection is reported before the condition policy as well.
        ex = assertThrows(IllegalArgumentException.class, () -> new Join(java.util.Collections.emptyList(), null));
        assertFalse(ex.getMessage().contains(predicateMessage), ex.getMessage());
    }

    @Test
    void testEquals() {
        Condition condition = Filters.eq("a", "b");
        Join join1 = new Join("table", condition);
        Join join2 = new Join("table", condition);
        Join join3 = new Join("other", condition);
        Join join4 = new Join("table", Filters.eq("a", "c"));

        assertEquals(join1, join1);
        assertEquals(join1, join2);
        assertNotEquals(join1, join3);
        assertNotEquals(join1, join4);
        assertNotEquals(join1, null);
        assertNotEquals(join1, "string");
    }

    @Test
    void testEquals_DifferentEntities() {
        Join join1 = new Join("orders", ON_AB);
        Join join2 = new Join("products", ON_AB);
        assertNotEquals(join1, join2);
    }

    @Test
    void testEquals_EqualObjects() {
        Join join1 = new Join("orders o", new Equal("a", "b"));
        Join join2 = new Join("orders o", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    void testEquals_Null() {
        Join join = new Join("orders", ON_AB);
        assertNotEquals(null, join);
    }

    @Test
    void testEquals_SameObject() {
        Join join = new Join("orders", ON_AB);
        assertEquals(join, join);
    }

    @Test
    void testEqualsWithDifferentOperators() {
        TestJoin join1 = new TestJoin(Operator.CROSS_JOIN, "table");
        TestJoin join2 = new TestJoin(Operator.NATURAL_JOIN, "table");

        assertNotEquals(join1, join2);
    }

    @Test
    void testGetCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        Join join = new Join("table_b b", condition);
        Condition retrieved = join.condition();
        assertEquals(condition, retrieved);
    }

    @Test
    void testGetCondition_Null() {
        // Only the conditionless join types (CROSS/NATURAL) can be built without a predicate.
        Join join = new TestJoin(Operator.CROSS_JOIN, "orders");
        assertNull(join.condition());
    }

    @Test
    void testGetJoinEntities() {
        Join join = new Join(Arrays.asList("table1", "table2"), Filters.on("table1.id", "table2.id"));
        List<String> entities = join.joinEntities();
        assertEquals(2, entities.size());
        assertTrue(entities.contains("table1"));
        assertTrue(entities.contains("table2"));
    }

    @Test
    void testHasCondition_NullConditionPassedExplicitly() {
        Join join = new TestJoin(Operator.NATURAL_JOIN, Arrays.asList("table1", "table2"), null);
        assertFalse(join.hasCondition());
    }

    @Test
    void testHasCondition_WithCondition() {
        Join join = new Join("orders o", new Equal("customers.id", "o.customer_id"));
        assertTrue(join.hasCondition());
        assertNotNull(join.condition());
    }

    @Test
    void testHasCondition_WithoutCondition() {
        Join join = new TestJoin(Operator.CROSS_JOIN, "orders");
        assertFalse(join.hasCondition());
        assertNull(join.condition());
    }

    @Test
    void testHashCode() {
        Join join1 = new Join("orders", new Equal("a", "b"));
        Join join2 = new Join("orders", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    void testHashCodeAndEqualsSnapshotMutableValueInJoinCondition() {
        // Array values are snapshotted at construction: later external mutation of the source array
        // changes neither equality nor the hash code, and two joins built from equal-content arrays are equal.
        final byte[] value = { 1 };
        final Join join = new Join("orders", Filters.eq("payload", value));
        final int h = join.hashCode();

        value[0] = 2;

        final Join sameContent = new Join("orders", Filters.eq("payload", new byte[] { 1 }));
        assertEquals(join, sameContent);
        assertEquals(h, join.hashCode());
        assertEquals(join.hashCode(), sameContent.hashCode());
        assertNotEquals(join, new Join("orders", Filters.eq("payload", new byte[] { 2 })));
    }

    @Test
    void testMySqlDashReadingOfUnterminatedBlockCommentInJoinEntity() {
        // Regression: MySQL reads "t --/*" as "t - -" plus an open block comment (it needs whitespace after "--"), so the
        // generic line-comment terminator does not help there; the entity was accepted.
        assertEquals("joinEntity must not contain an unterminated block comment: t --/*",
                assertThrows(IllegalArgumentException.class, () -> new Join("t --/*", ON_AB)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> new CrossJoin("t --/*"));
        assertThrows(IllegalArgumentException.class, () -> new Join(List.of("a", "t --x /* y"), ON_AB));

        // With whitespace after "--" every dialect reads a line comment, which the renderer terminates.
        assertEquals("JOIN t -- /* x\n ON a.id = b.id", new Join("t -- /* x", ON_AB).toString());
        assertEquals("JOIN t --/* x */\n ON a.id = b.id", new Join("t --/* x */", ON_AB).toString());
    }

    @Test
    void testNestedQuotedTempTableAndHintCommentsInJoinEntity() {
        // PostgreSQL and SQL Server nest block comments: a balanced nest is closed, an inner opener alone is not.
        assertEquals("JOIN t /* /* */ */ ON a.id = b.id", new Join("t /* /* */ */", ON_AB).toString());
        assertThrows(IllegalArgumentException.class, () -> new Join("t /* /* */", ON_AB));

        // Quoted identifiers, a SQL Server #temp table and optimizer hints are not unterminated comments.
        assertEquals("JOIN \"t/*\" ON a.id = b.id", new Join("\"t/*\"", ON_AB).toString());
        assertEquals("JOIN `t/*` ON a.id = b.id", new Join("`t/*`", ON_AB).toString());
        assertEquals("JOIN #temp /* x */ ON a.id = b.id", new Join("#temp /* x */", ON_AB).toString());
        assertEquals("JOIN t /*+ INDEX(t i) */ ON a.id = b.id", new Join("t /*+ INDEX(t i) */", ON_AB).toString());
        assertEquals("JOIN t /*! STRAIGHT_JOIN */ ON a.id = b.id", new Join("t /*! STRAIGHT_JOIN */", ON_AB).toString());
    }

    @Test
    void testNullNamingPolicyIsNormalizedBeforeRenderingPredicate() {
        final SqlExpression predicate = new SqlExpression("customerId = id") {
            @Override
            public String toSql(final NamingPolicy namingPolicy) {
                return java.util.Objects.requireNonNull(namingPolicy, "namingPolicy").convert("customerId") + " = id";
            }
        };
        final Join join = new Join("orders", predicate);

        assertEquals("JOIN orders ON customerId = id", join.toSql(null));
        assertEquals("JOIN orders ON customer_id = id", join.toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    void testParameters() {
        Condition condition = Filters.and(Filters.eq("status", "active"), Filters.gt("amount", 100));
        Join join = new Join("orders", condition);

        List<Object> params = join.parameters();

        assertNotNull(params);
        assertEquals(2, params.size());
        assertTrue(params.contains("active"));
        assertTrue(params.contains(100));
    }

    @Test
    void testParameters_Empty() {
        // A column-to-column ON predicate binds no parameters.
        Join join = new Join("orders", ON_AB);
        assertTrue(join.parameters().isEmpty());
    }

    @Test
    void testParameters_WithCondition() {
        Join join = new Join("orders o", new Equal("status", "active"));
        List<Object> params = join.parameters();
        assertEquals(1, params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    void testParametersArrayCopyIsNotSharedAcrossCalls() {
        // Join.parameters() is not memoized: each call must hand out the join condition's fresh defensive copy.
        final Join join = new Join("orders", Filters.eq("payload", new byte[] { 1 }));

        final byte[] first = (byte[]) join.parameters().get(0);
        first[0] = 9;

        assertTrue(Arrays.equals(new byte[] { 1 }, (byte[]) join.parameters().get(0)));
        assertEquals("JOIN orders ON payload = '[1]'", join.toString());

        // Date variant: mutating a returned Date must not leak into the next caller.
        final Join dateJoin = new Join("orders", Filters.eq("created", new Date(1000L)));

        final Date firstDate = (Date) dateJoin.parameters().get(0);
        firstDate.setTime(5000L);

        assertEquals(1000L, ((Date) dateJoin.parameters().get(0)).getTime());
    }

    @Test
    void testParametersNoCondition() {
        Join join = new TestJoin(Operator.CROSS_JOIN, "products");
        List<Object> params = join.parameters();

        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    void testProtectedConstructorRejectsConditionsForConditionlessJoinTypes() {
        Condition condition = Filters.eq("a.id", Filters.expr("b.id"));

        assertThrows(IllegalArgumentException.class, () -> new TestJoin(Operator.CROSS_JOIN, "b", condition));
        assertThrows(IllegalArgumentException.class, () -> new TestJoin(Operator.NATURAL_JOIN, Arrays.asList("b", "c"), condition));

        assertDoesNotThrow(() -> new TestJoin(Operator.CROSS_JOIN, "b", null));
        assertDoesNotThrow(() -> new TestJoin(Operator.NATURAL_JOIN, Arrays.asList("b", "c"), null));
    }

    @Test
    void testProtectedConstructors() {
        // Test protected constructors through a test subclass. The condition-less 2-arg form succeeds
        // only for CROSS/NATURAL joins.
        TestJoin join1 = new TestJoin(Operator.CROSS_JOIN, "table1");
        assertEquals(Operator.CROSS_JOIN, join1.operator());
        assertEquals("table1", join1.joinEntities().get(0));

        Condition condition = Filters.eq("a", "b");
        TestJoin join2 = new TestJoin(Operator.RIGHT_JOIN, "table2", condition);
        assertEquals(Operator.RIGHT_JOIN, join2.operator());
        assertEquals("table2", join2.joinEntities().get(0));
        assertEquals(condition, join2.condition());

        List<String> entities = Arrays.asList("t1", "t2");
        TestJoin join3 = new TestJoin(Operator.FULL_JOIN, entities, condition);
        assertEquals(Operator.FULL_JOIN, join3.operator());
        assertEquals(2, join3.joinEntities().size());
        assertEquals(condition, join3.condition());
    }

    @Test
    void testProtectedConstructors_NullOperatorThrowsIllegalArgumentException() {
        // A null operator is an IllegalArgumentException (previously threw NullPointerException).
        assertThrows(IllegalArgumentException.class, () -> new TestJoin(null, "orders"));
        assertThrows(IllegalArgumentException.class, () -> new TestJoin(null, "orders", ON_AB));
        assertThrows(IllegalArgumentException.class, () -> new TestJoin(null, Arrays.asList("orders", "items"), ON_AB));
    }

    @Test
    void testRegularJoinsUnaffectedByFix() {
        // Sanity check that the fix does not regress normal join rendering.
        Join j1 = new Join("orders", ON_AB);
        assertEquals("JOIN orders ON a.id = b.id", j1.toSql(NamingPolicy.NO_CHANGE));

        Join j2 = new Join("orders o", new Equal("c.id", "o.cid"));
        String r2 = j2.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(r2.startsWith("JOIN orders o"));
        assertTrue(r2.contains("c.id"));

        InnerJoin ij = new InnerJoin("orders o", ON_AB);
        assertEquals("INNER JOIN orders o ON a.id = b.id", ij.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    void testSingleEntityBlankMessageNamesJoinEntity() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Join(" ", Filters.expr("a = b")));
        assertEquals("joinEntity must not be null, empty, or blank", e.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> new InnerJoin((String) null, Filters.expr("a = b")));
        assertEquals("joinEntity must not be null, empty, or blank", e2.getMessage());

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> new CrossJoin(""));
        assertEquals("joinEntity must not be null, empty, or blank", e3.getMessage());

        // The collection form still names the element of joinEntities.
        final IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> new Join(Arrays.asList("orders o", " "), Filters.expr("a = b")));
        assertTrue(e4.getMessage().contains("joinEntities"), e4.getMessage());
    }

    @Test
    void testTableLineCommentsDoNotConsumeJoinBoundaries() {
        assertEquals("JOIN orders o -- trailing comment\n ON o.id = a.id", new Join("orders o -- trailing comment", new On("o.id", "a.id")).toString());
        assertEquals("JOIN (orders o -- trailing comment\n CROSS JOIN products p) ON o.id = p.id",
                new Join(List.of("orders o -- trailing comment", "products p"), new On("o.id", "p.id")).toString());
        assertEquals("CROSS JOIN (orders o CROSS JOIN products p -- trailing comment\n)",
                new CrossJoin(List.of("orders o", "products p -- trailing comment")).toString());
    }

    @Test
    void testToString() {
        Join join = new Join("orders", ON_AB);
        String result = join.toString();

        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orders"));
        assertFalse(result.contains("INNER"));
        assertFalse(result.contains("LEFT"));
    }

    @Test
    void testToString_Simple() {
        Join join = new Join("orders", ON_AB);
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orders"));
    }

    @Test
    void testToString_WithCondition() {
        Join join = new Join("orders o", new Equal("c.id", "o.customer_id"));
        String result = join.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orders o"));
    }

    @Test
    void testToStringAddsOnForPlainPredicateCondition() {
        Join plainPredicate = new Join("orders", Filters.expr("customers.id = orders.customer_id"));
        Join explicitOn = new Join("orders", Filters.on("customers.id", "orders.customer_id"));

        assertEquals("JOIN orders ON customers.id = orders.customer_id", plainPredicate.toSql(NamingPolicy.NO_CHANGE));
        assertEquals("JOIN orders ON customers.id = orders.customer_id", explicitOn.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    void testToStringOperatorOnlyHasNoTrailingSpace() throws Exception {
        // Bug fix: when joinEntities is null/empty but operator is set, the output should not have
        // a trailing space (which would yield "INNER JOIN " instead of "INNER JOIN").
        Join join = new Join();
        java.lang.reflect.Field operatorField = AbstractCondition.class.getDeclaredField("operator");
        operatorField.setAccessible(true);
        operatorField.set(join, Operator.INNER_JOIN);

        String rendered = join.toSql(NamingPolicy.NO_CHANGE);
        assertEquals("INNER JOIN", rendered);
        assertFalse(rendered.endsWith(" "), "toString() must not end with a trailing space");
    }

    @Test
    void testToStringWithCondition() {
        Condition condition = Filters.eq("customers.id", "orders.customer_id");
        Join join = new Join("orders o", condition);
        String result = join.toString();

        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orders o"));
        assertTrue(result.contains("customers.id"));
        assertTrue(result.contains("orders.customer_id"));
    }

    @Test
    void testToStringWithMultipleEntities() {
        List<String> entities = Arrays.asList("t1", "t2", "t3");
        Condition condition = Filters.eq("t1.id", "t2.t1_id");
        Join join = new Join(entities, condition);

        // Multiple entities render as a parenthesized CROSS JOIN tree (a standard joined-table operand).
        String result = join.toString();
        assertTrue(result.contains("(t1 CROSS JOIN t2 CROSS JOIN t3)"));
        assertFalse(result.contains("t1, t2, t3"));
    }

    @Test
    void testToStringWithNamingPolicy() {
        Condition condition = Filters.eq("customerId", Filters.expr("orderId"));
        Join join = new Join("orderTable", condition);
        String result = join.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);

        assertTrue(result.contains("JOIN"));
        assertTrue(result.contains("orderTable"));
        assertTrue(result.contains("CUSTOMER_ID"));
        assertTrue(result.contains("ORDER_ID"));
    }

    @Test
    void testUnterminatedBlockCommentInJoinEntityIsRejected() {
        // Regression: "t /* x" was accepted and rendered as "JOIN t /* x ON a.id = b.id", commenting out the join predicate.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Join("t /* x", ON_AB));
        assertEquals("joinEntity must not contain an unterminated block comment: t /* x", e.getMessage());

        assertThrows(IllegalArgumentException.class, () -> new InnerJoin("t /* x", ON_AB));
        assertThrows(IllegalArgumentException.class, () -> new CrossJoin("t /* x"));
        assertThrows(IllegalArgumentException.class, () -> Filters.leftJoin("t /* x", ON_AB));
        assertThrows(IllegalArgumentException.class, () -> Criteria.builder().join("t /* x", ON_AB));

        // The collection form names the element of joinEntities.
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> new Join(List.of("a", "t /* x"), ON_AB));
        assertEquals("joinEntity in joinEntities must not contain an unterminated block comment: t /* x", e2.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new CrossJoin(List.of("a", "t /* x")));

        // A terminated block comment is kept verbatim.
        assertEquals("JOIN t /* x */ u ON a.id = b.id", new Join("t /* x */ u", ON_AB).toString());
        assertEquals("CROSS JOIN (a CROSS JOIN t /* x */)", new CrossJoin(List.of("a", "t /* x */")).toString());
    }

    @Test
    void testValidateJoinConditionMessageMatchesOnWording() {
        // The Join rejection uses the same wording as On.validateOnCondition (a blank expression is not a predicate).
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Join("orders", Filters.expr("  ")));

        assertTrue(ex.getMessage().contains("use a predicate without clause, quantified, ON, or USING operators (a blank expression is not a predicate)"),
                ex.getMessage());
        assertFalse(ex.getMessage().contains("non-empty predicate"), ex.getMessage());
    }

    private static final class TestComposableWrapper extends ComposableCell {
        TestComposableWrapper(final Condition condition) {
            super(Operator.NOT, condition);
        }
    }

    // Test subclass to access protected constructors
    private static class TestJoin extends Join {
        public TestJoin(Operator operator, String joinEntity) {
            super(operator, joinEntity);
        }

        public TestJoin(Operator operator, String joinEntity, Condition condition) {
            super(operator, joinEntity, condition);
        }

        public TestJoin(Operator operator, Collection<String> joinEntities, Condition condition) {
            super(operator, joinEntities, condition);
        }
    }
}
