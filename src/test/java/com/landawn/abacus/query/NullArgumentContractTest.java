package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
class NullArgumentContractTest {
    @Test
    void dialectConstructionUsesTheSameValidationAsTheFactory() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Dsl(null)).getMessage().contains(cs.sqlDialect));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dsl.forDialect(null)).getMessage().contains(cs.sqlDialect));
    }

    @Test
    void aliasValidationRejectsNullMapsButPreservesEmptyMaps() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dsl.validateColumnAliases(null)).getMessage()
                .contains(cs.propOrColumnNameAliases));
        Dsl.validateColumnAliases(Collections.emptyMap());
        Dsl.validateColumnAliases(Collections.singletonMap("name", "display_name"));
        assertThrows(IllegalArgumentException.class, () -> Dsl.validateColumnAliases(Collections.singletonMap("name", null)));
    }

    @Test
    void topLevelMetadataRegistrationRejectsNullPolicyBeforeInspectingTheClass() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> QueryUtil.registerEntityPropColumnNameMap(null, null, null))
                .getMessage().contains(cs.entityClass));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> QueryUtil.registerEntityPropColumnNameMap(String.class, null, null))
                .getMessage().contains(cs.namingPolicy));
        assertThrows(IllegalArgumentException.class, () -> QueryUtil.registerEntityPropColumnNameMap(Person.class, null, null));
    }

    @Test
    void recursiveMetadataRegistrationAndPublicDefaultsStillAcceptNullPolicy() {
        assertTrue(QueryUtil.registerEntityPropColumnNameMap(Person.class, null, new HashSet<>()).containsKey("firstName"));
        assertTrue(QueryUtil.registerEntityPropColumnNameMap(Person.class, null, Collections.singleton(Person.class)).isEmpty());
        assertEquals(QueryUtil.propToColumnNameMap(Person.class, NamingPolicy.SNAKE_CASE), QueryUtil.propToColumnNameMap(Person.class, null));
        assertTrue(QueryUtil.propToColumnNameMap(null, null).isEmpty());
    }

    @Test
    void dynamicQueryPreservesOptionalNullsAndChecksClosedStateFirst() {
        final DynamicQuery.Builder builder = DynamicQuery.builder();
        builder.select().append((Collection<String>) null).append((Map<String, String>) null).appendIf(false, null).append("id");
        builder.from().append((Collection<String>) null).append("people");
        builder.appendIf(false, null);
        assertEquals("SELECT id FROM people", builder.build());
        assertThrows(IllegalStateException.class, () -> builder.select().append((Collection<String>) null));
    }

    @Test
    void qualifyingDotScannerRetainsItsRequiredSourcePrecondition() {
        assertThrows(NullPointerException.class, () -> QueryUtil.indexOfQualifyingDot(null));
        assertEquals(-1, QueryUtil.indexOfQualifyingDot(""));
        assertEquals(1, QueryUtil.indexOfQualifyingDot("p.first_name"));
    }

    public static class Person {
        private String firstName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }
    }
}
