package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class CrossJoinTest extends TestBase {

    @Test
    public void testSingleEntity() {
        CrossJoin join = new CrossJoin("colors");
        assertEquals(Operator.CROSS_JOIN, join.operator());
        assertEquals(Arrays.asList("colors"), join.getJoinEntities());
        assertNull(join.getCondition());
        assertTrue(join.parameters().isEmpty());
        assertEquals("CROSS JOIN colors", join.toString(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testMultipleEntities() {
        CrossJoin join = new CrossJoin(Arrays.asList("sizes", "colors"));
        assertEquals(Arrays.asList("sizes", "colors"), join.getJoinEntities());
        assertEquals("CROSS JOIN (sizes, colors)", join.toString(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testFactories() {
        assertEquals(new CrossJoin("colors"), Filters.crossJoin("colors"));
        assertEquals(new CrossJoin(Arrays.asList("sizes", "colors")), Filters.crossJoin(Arrays.asList("sizes", "colors")));
    }

    @Test
    public void testConditionOverloadsAreAbsent() {
        assertThrows(NoSuchMethodException.class, () -> CrossJoin.class.getConstructor(String.class, Condition.class));
        assertThrows(NoSuchMethodException.class, () -> CrossJoin.class.getConstructor(java.util.Collection.class, Condition.class));
        assertFalse(Arrays.stream(Filters.class.getMethods())
                .filter(method -> method.getName().equals("crossJoin"))
                .map(Method::getParameterTypes)
                .anyMatch(types -> Arrays.asList(types).contains(Condition.class)));
        assertFalse(Arrays.stream(CrossJoin.class.getConstructors())
                .map(Constructor::getParameterTypes)
                .anyMatch(types -> Arrays.asList(types).contains(Condition.class)));
    }
}
