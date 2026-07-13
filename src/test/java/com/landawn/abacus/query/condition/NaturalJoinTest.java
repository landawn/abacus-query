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
public class NaturalJoinTest extends TestBase {

    @Test
    public void testSingleEntity() {
        NaturalJoin join = new NaturalJoin("employees");
        assertEquals(Operator.NATURAL_JOIN, join.operator());
        assertEquals(Arrays.asList("employees"), join.joinEntities());
        assertNull(join.condition());
        assertTrue(join.parameters().isEmpty());
        assertEquals("NATURAL JOIN employees", join.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testMultipleEntities() {
        NaturalJoin join = new NaturalJoin(Arrays.asList("employees", "departments"));
        assertEquals(Arrays.asList("employees", "departments"), join.joinEntities());
        assertEquals("NATURAL JOIN (employees, departments)", join.toSql(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testFactories() {
        assertEquals(new NaturalJoin("employees"), Filters.naturalJoin("employees"));
        assertEquals(new NaturalJoin(Arrays.asList("employees", "departments")), Filters.naturalJoin(Arrays.asList("employees", "departments")));
    }

    @Test
    public void testConditionOverloadsAreAbsent() {
        assertThrows(NoSuchMethodException.class, () -> NaturalJoin.class.getConstructor(String.class, Condition.class));
        assertThrows(NoSuchMethodException.class, () -> NaturalJoin.class.getConstructor(java.util.Collection.class, Condition.class));
        assertFalse(Arrays.stream(Filters.class.getMethods())
                .filter(method -> method.getName().equals("naturalJoin"))
                .map(Method::getParameterTypes)
                .anyMatch(types -> Arrays.asList(types).contains(Condition.class)));
        assertFalse(Arrays.stream(NaturalJoin.class.getConstructors())
                .map(Constructor::getParameterTypes)
                .anyMatch(types -> Arrays.asList(types).contains(Condition.class)));
    }
}
