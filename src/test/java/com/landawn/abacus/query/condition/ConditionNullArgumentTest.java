package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.SortDirection;

@Tag("2025")
class ConditionNullArgumentTest {

    @Test
    void subqueryArityHelperRejectsNullAsAnArgumentError() {
        assertThrows(IllegalArgumentException.class, () -> AbstractCondition.validateSubQuerySelectArity(1, null));
        AbstractCondition.validateSubQuerySelectArity(1, new SubQuery("SELECT id FROM users"));
        AbstractCondition.validateSubQuerySelectArity(1, new SubQuery("users", List.of("id"), null));
    }

    @Test
    void expressionHelpersRequireAnOperatorWhenTheyRenderIt() {
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.link((Operator) null, "age", 18));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.link(null, "age", 18, 65));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.link2(null, "age", "NULL"));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.link2(null, new String[] { "age > 18", "active = 1" }));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> SqlExpression.link((Operator) null, null, 18)).getMessage().contains("operator"));
    }

    @Test
    void unusedOperatorRemainsOptionalForEmptyAndSingletonLists() {
        assertEquals("", SqlExpression.link2(null, (String[]) null));
        assertEquals("", SqlExpression.link2(null, new String[0]));
        assertEquals("(age > 18)", SqlExpression.link2(null, new String[] { "age > 18" }));
        assertThrows(IllegalArgumentException.class, () -> SqlExpression.link2(null, new String[] { null }));
    }

    @Test
    void nullSortEntriesAreRejectedBeforeDereference() {
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
    void nullDefaultsAndEmptyInputsKeepTheirExistingMeaning() {
        assertEquals("age IS NULL", new Equal("age", null).toSql(null));
        assertEquals("age IS NOT NULL", SqlExpression.notEqual("age", null));
        assertEquals("SELECT id FROM users", new SubQuery("users", List.of("id"), null).toSql(null));
        assertEquals("1 = 1", new And((Condition[]) null).toString());
        assertEquals("1 = 0", new Or((Collection<Condition>) null).toString());
        assertEquals("", SqlExpression.and((String[]) null));
        assertEquals("", SqlExpression.plus((Object[]) null));
        assertNull(Operator.of(null));
        assertTrue(Criteria.builder().build().findConditions(null).isEmpty());
        assertFalse(new Equal("age", null).equals(null));
    }

    @Test
    void nullPointerFailuresInsideCustomValueRenderingAreNotTranslated() {
        final NullPointerException failure = new NullPointerException("custom number failed");
        final Number value = new Number() {
            private static final long serialVersionUID = 1L;

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
                throw failure;
            }
        };

        assertSame(failure, assertThrows(NullPointerException.class, () -> SqlExpression.renderValue(value)));
    }
}
