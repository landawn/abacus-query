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
public class IsNotTest extends TestBase {

    @Test
    public void testConstructorAcceptsNullBooleanAndExplicitExpression() {
        final IsNot nullCondition = new IsNot("deletedAt", null);
        final IsNot booleanCondition = new IsNot("enabled", false);
        final SqlExpression unknown = Filters.expr("UNKNOWN");
        final IsNot expressionCondition = new IsNot("status", unknown);

        assertEquals("deletedAt", nullCondition.propName());
        assertEquals(Operator.IS_NOT, nullCondition.operator());
        // A Boolean is normalized to the SQL keyword expression at construction.
        assertEquals(SqlExpression.of("FALSE"), booleanCondition.propValue());
        assertEquals(unknown, expressionCondition.propValue());
    }

    @Test
    public void testConstructorRejectsBlankExpression() {
        assertThrows(IllegalArgumentException.class, () -> new IsNot("status", Filters.expr("")));
        assertThrows(IllegalArgumentException.class, () -> new IsNot("status", Filters.expr("   ")));
    }

    @Test
    public void testConstructorRejectsInvalidPropertyNames() {
        assertThrows(IllegalArgumentException.class, () -> new IsNot(null, true));
        assertThrows(IllegalArgumentException.class, () -> new IsNot("", true));
        assertThrows(IllegalArgumentException.class, () -> new IsNot("   ", true));
    }

    @Test
    public void testConstructorRejectsArbitraryLiteralsAndPredicates() {
        assertThrows(IllegalArgumentException.class, () -> new IsNot("status", "UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> new IsNot("count", 1));
        assertThrows(IllegalArgumentException.class, () -> new IsNot("flag", Filters.eq("other", true)));
        assertThrows(IllegalArgumentException.class, () -> new IsNot("flag", Filters.subQuery("SELECT flag FROM settings")));
        assertThrows(IllegalArgumentException.class, () -> new IsNot("flag", new All(Filters.subQuery("SELECT flag FROM settings"))));
    }

    @Test
    public void testNullRendersKeywordAndHasNoParameter() {
        final IsNot condition = new IsNot("deletedAt", null);

        assertEquals("deletedAt IS NOT NULL", condition.toString());
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    public void testBooleanRendersLiteralAndIsReportedAsParameter() {
        // `x IS NOT ?` is not valid SQL, so a Boolean is rendered as the TRUE/FALSE keyword and never bound.
        final IsNot condition = new IsNot("enabled", false);

        assertEquals("enabled IS NOT FALSE", condition.toString());
        assertTrue(condition.parameters().isEmpty());
        assertEquals(SqlExpression.of("FALSE"), condition.propValueAs(SqlExpression.class));

        final IsNot trueCondition = new IsNot("enabled", true);
        assertEquals("enabled IS NOT TRUE", trueCondition.toString());
        assertTrue(trueCondition.parameters().isEmpty());
        assertEquals(List.of(), Filters.isNot("enabled", true).parameters());
    }

    @Test
    public void testExplicitExpressionRendersVerbatimWithoutParameter() {
        final IsNot condition = new IsNot("status", Filters.expr("UNKNOWN"));

        assertEquals("status IS NOT UNKNOWN", condition.toString());
        assertTrue(condition.parameters().isEmpty());
    }

    @Test
    public void testNamingPolicy() {
        assertEquals("first_name IS NOT NULL", new IsNot("firstName", null).toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testEqualityHashCodeAndComposition() {
        final IsNot left = new IsNot("enabled", false);
        final IsNot equal = new IsNot("enabled", false);
        final IsNot different = new IsNot("enabled", true);

        assertEquals(left, equal);
        assertEquals(left.hashCode(), equal.hashCode());
        assertNotEquals(left, different);
        assertNotEquals(left, new Is("enabled", false));
        assertEquals(2, left.and(new IsNot("verified", false)).conditions().size());
        assertEquals(2, left.or(new IsNot("verified", true)).conditions().size());
        assertEquals(Operator.NOT, left.not().operator());
    }

    @Test
    public void testDefaultConstructorState() {
        final IsNot condition = new IsNot();

        assertNull(condition.operator());
        assertNull(condition.propName());
        assertNull(condition.propValue());
    }
}
