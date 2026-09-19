package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class IsTest extends TestBase {

    @Test
    public void testConstructorAcceptsNullBooleanAndExplicitExpression() {
        final Is nullCondition = new Is("deletedAt", null);
        final Is booleanCondition = new Is("enabled", true);
        final SqlExpression unknown = Filters.expr("UNKNOWN");
        final Is expressionCondition = new Is("status", unknown);

        assertEquals("deletedAt", nullCondition.propName());
        assertEquals(Operator.IS, nullCondition.operator());
        // A Boolean is normalized to the SQL keyword expression at construction.
        assertEquals(SqlExpression.of("TRUE"), booleanCondition.propValue());
        assertEquals(unknown, expressionCondition.propValue());
    }

    @Test
    public void testConstructorRejectsBlankExpression() {
        assertThrows(IllegalArgumentException.class, () -> new Is("status", Filters.expr("")));
        assertThrows(IllegalArgumentException.class, () -> new Is("status", Filters.expr("   ")));
    }

    @Test
    public void testConstructorRejectsInvalidPropertyNames() {
        assertThrows(IllegalArgumentException.class, () -> new Is(null, true));
        assertThrows(IllegalArgumentException.class, () -> new Is("", true));
        assertThrows(IllegalArgumentException.class, () -> new Is("   ", true));
    }

    @Test
    public void testConstructorRejectsArbitraryLiteralsAndPredicates() {
        assertThrows(IllegalArgumentException.class, () -> new Is("status", "UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> new Is("count", 1));
        assertThrows(IllegalArgumentException.class, () -> new Is("flag", Filters.eq("other", true)));
        assertThrows(IllegalArgumentException.class, () -> new Is("flag", Filters.subQuery("SELECT flag FROM settings")));
        assertThrows(IllegalArgumentException.class, () -> new Is("flag", new Any(Filters.subQuery("SELECT flag FROM settings"))));
    }

    @Test
    public void testNullRendersKeywordAndHasNoParameter() {
        final Is condition = new Is("deletedAt", null);

        assertEquals("deletedAt IS NULL", condition.toString());
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    public void testBooleanRendersLiteralAndIsReportedAsParameter() {
        // `x IS ?` is not valid SQL, so a Boolean is rendered as the TRUE/FALSE keyword and never bound.
        final Is condition = new Is("enabled", true);

        assertEquals("enabled IS TRUE", condition.toString());
        assertTrue(condition.parameters().isEmpty());
        assertEquals(SqlExpression.of("TRUE"), condition.propValueAs(SqlExpression.class));

        final Is falseCondition = new Is("enabled", false);
        assertEquals("enabled IS FALSE", falseCondition.toString());
        assertTrue(falseCondition.parameters().isEmpty());
    }

    @Test
    public void testBooleanOperandEqualsIsTrueIsFalseFactories() {
        assertEquals(Filters.isTrue("enabled"), new Is("enabled", true));
        assertEquals(Filters.isTrue("enabled").hashCode(), new Is("enabled", true).hashCode());
        assertEquals(Filters.isFalse("enabled"), Filters.is("enabled", false));
        assertEquals(Filters.isFalse("enabled").hashCode(), Filters.is("enabled", false).hashCode());
        assertEquals(List.of(), Filters.is("enabled", true).parameters());
    }

    @Test
    public void testExplicitExpressionRendersVerbatimWithoutParameter() {
        final Is condition = new Is("status", Filters.expr("UNKNOWN"));

        assertEquals("status IS UNKNOWN", condition.toString());
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    public void testNamingPolicy() {
        assertEquals("first_name IS NULL", new Is("firstName", null).toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testEqualityHashCodeAndComposition() {
        final Is left = new Is("enabled", true);
        final Is equal = new Is("enabled", true);
        final Is different = new Is("enabled", false);

        assertEquals(left, equal);
        assertEquals(left.hashCode(), equal.hashCode());
        assertNotEquals(left, different);
        assertNotEquals(left, new IsNot("enabled", true));
        assertEquals(2, left.and(new Is("verified", true)).conditions().size());
        assertEquals(2, left.or(new Is("verified", false)).conditions().size());
        assertEquals(Operator.NOT, left.not().operator());
    }

    @Test
    public void testDefaultConstructorState() {
        final Is condition = new Is();

        assertNull(condition.operator());
        assertNull(condition.propName());
        assertNull(condition.propValue());
    }
}
