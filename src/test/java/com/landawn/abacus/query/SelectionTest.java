package com.landawn.abacus.query;

import static com.landawn.abacus.query.Dsl.PSC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;

public class SelectionTest extends TestBase {

    // A non-empty table alias is prefixed verbatim to every rendered column, so it is validated like the class
    // alias: a whitespace-only alias would render " .col" and a comment token would swallow the SELECT list.
    @Test
    void testBlankOrUnsafeTableAliasIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(Selection.builder(Account.class).tableAlias("  ").classAlias("acc").build()));
        assertThrows(IllegalArgumentException.class, () -> PSC.select(Selection.builder(Account.class).tableAlias(" ").build()));
        assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(Selection.builder(Account.class).tableAlias("a -- x").classAlias("acc").build()));
        assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(Selection.builder(Account.class).tableAlias("a\" x").build()));

        // null / empty mean "no alias"; a plain alias still renders.
        assertEquals(PSC.selectFrom(Selection.builder(Account.class).build()).build().query(),
                PSC.selectFrom(Selection.builder(Account.class).tableAlias("").build()).build().query());
        assertTrue(PSC.selectFrom(Selection.builder(Account.class).tableAlias("a").classAlias("acc").build()).build().query().endsWith("FROM account a"));
    }

    @Test
    void testBuilderDefaults() {
        final Selection selection = Selection.builder(Account.class).build();

        assertEquals(Account.class, selection.entityClass());
        assertNull(selection.tableAlias());
        assertNull(selection.classAlias());
        assertNull(selection.includedPropNames());
        assertNull(selection.excludedPropNames());
        assertFalse(selection.includesSubEntityProperties());
    }

    @Test
    void testBuilderFullyPopulated() {
        final Selection selection = Selection.builder(Account.class)
                .tableAlias("a")
                .classAlias("account")
                .includedPropNames(Arrays.asList("id", "firstName"))
                .includeSubEntityProperties(true)
                .excludedPropNames(new LinkedHashSet<>(Arrays.asList("status")))
                .build();

        assertEquals(Account.class, selection.entityClass());
        assertEquals("a", selection.tableAlias());
        assertEquals("account", selection.classAlias());
        assertEquals(Arrays.asList("id", "firstName"), selection.includedPropNames());
        assertTrue(selection.includesSubEntityProperties());
        assertEquals(Set.of("status"), selection.excludedPropNames());
    }

    @Test
    void testBuilderMethodsChainOnBuilder() {
        final Selection.SelectionBuilder builder = Selection.builder(Account.class);

        assertTrue(builder == builder.tableAlias("a"));
        assertTrue(builder == builder.classAlias("account"));
        assertTrue(builder == builder.includeSubEntityProperties(true));
        assertTrue(builder == builder.includedPropNames(List.of("id")));
        assertTrue(builder == builder.excludedPropNames(Set.of("status")));
    }

    @Test
    void testBuilderRequiresEntityClassAtCreation() throws NoSuchMethodException {
        assertThrows(IllegalArgumentException.class, () -> Selection.builder(null));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("builder"));
        assertEquals(0, Selection.class.getConstructors().length);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testDeprecatedTwoEntitySelectionValidatesBothEntityClassesConsistently() {
        final IllegalArgumentException selectFirst = assertThrows(IllegalArgumentException.class,
                () -> PSC.select(null, "a", "first", null, Account.class, "b", "second", null));
        final IllegalArgumentException selectSecond = assertThrows(IllegalArgumentException.class,
                () -> PSC.select(Account.class, "a", "first", null, null, "b", "second", null));
        final IllegalArgumentException selectFromFirst = assertThrows(IllegalArgumentException.class,
                () -> PSC.selectFrom(null, "a", "first", null, Account.class, "b", "second", null));
        final IllegalArgumentException selectFromSecond = assertThrows(IllegalArgumentException.class,
                () -> PSC.selectFrom(Account.class, "a", "first", null, null, "b", "second", null));

        assertEquals(selectFirst.getMessage(), selectSecond.getMessage());
        assertEquals(selectFirst.getMessage(), selectFromFirst.getMessage());
        assertEquals(selectFirst.getMessage(), selectFromSecond.getMessage());
    }

    @Test
    void testDslSelectFromMultipleSelections() {
        final List<Selection> selections = List.of(Selection.builder(Account.class).tableAlias("a").classAlias("account").build(),
                Selection.builder(Account.class).tableAlias("b").classAlias("account2").build());
        final String sql = PSC.selectFrom(selections).build().query();

        assertNotNull(sql);
        assertTrue(sql.startsWith("SELECT"));
        assertTrue(sql.contains("account"));
    }

    @Test
    void testDslSelectFromSingleSelection() {
        final Selection selection = Selection.builder(Account.class).tableAlias("a").classAlias("account").build();
        final String sql = PSC.selectFrom(selection).build().query();

        assertNotNull(sql);
        assertTrue(sql.startsWith("SELECT"));
        assertTrue(sql.contains("account"));
    }

    @Test
    void testDslSelectNullSelectionThrows() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select((Selection) null));
        assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom((Selection) null));
    }

    @Test
    void testDslSelectSingleSelectionThenFrom() {
        final Selection selection = Selection.builder(Account.class).tableAlias("a").classAlias("account").build();
        final String sql = PSC.select(selection).from("account a").build().query();

        assertNotNull(sql);
        assertTrue(sql.startsWith("SELECT"));
    }

    @Test
    void testDslSelectUsesOneListSnapshotForValidationAndRendering() {
        final Selection firstPass = Selection.builder(Account.class).tableAlias("a").classAlias("account").includedPropNames(List.of("id")).build();
        final Selection laterPass = Selection.builder(Account.class).tableAlias("a").classAlias("account").includedPropNames(List.of("firstName")).build();

        assertEquals("SELECT a.id AS \"account.id\" FROM account a", PSC.select(changingSelectionList(firstPass, laterPass)).from("account a").build().query());
        assertEquals("SELECT a.id AS \"account.id\" FROM account a", PSC.selectFrom(changingSelectionList(firstPass, laterPass)).build().query());
    }

    @Test
    void testEqualsAndHashCode() {
        final Selection first = Selection.builder(Account.class)
                .tableAlias("a")
                .classAlias("account")
                .includedPropNames(Arrays.asList("id", "firstName"))
                .excludedPropNames(Set.of("status"))
                .build();
        final Selection equal = Selection.builder(Account.class)
                .tableAlias("a")
                .classAlias("account")
                .includedPropNames(Arrays.asList("id", "firstName"))
                .excludedPropNames(Set.of("status"))
                .build();

        assertEquals(first, first);
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, "not a selection");
        assertNotEquals(first, Selection.builder(Account.class).tableAlias("b").classAlias("account").build());
        assertNotEquals(first, Selection.builder(Account.class).tableAlias("a").classAlias("acct").build());
        assertNotEquals(first, Selection.builder(Account.class).tableAlias("a").classAlias("account").includeSubEntityProperties(true).build());
    }

    @Test
    void testImmutableWrappersOverMutableCollectionsAreCopiedToo() {
        // ImmutableList.wrap(...) / ImmutableSet.wrap(...) are live views: the build must copy them, not adopt
        // them, or a later change to the backing collection would reach through into the built selection.
        final List<String> includedBacking = new ArrayList<>(Arrays.asList("id", "firstName"));
        final Set<String> excludedBacking = new LinkedHashSet<>(Arrays.asList("status"));
        final Selection selection = Selection.builder(Account.class)
                .includedPropNames(ImmutableList.wrap(includedBacking))
                .excludedPropNames(ImmutableSet.wrap(excludedBacking))
                .build();

        includedBacking.add("lastName");
        excludedBacking.add("createTime");

        assertEquals(Arrays.asList("id", "firstName"), selection.includedPropNames());
        assertEquals(Set.of("status"), selection.excludedPropNames());
        assertThrows(UnsupportedOperationException.class, () -> selection.includedPropNames().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> selection.excludedPropNames().add("other"));
    }

    @Test
    void testNullPropertyCollectionsPreserveDefaultSemantics() {
        final Selection selection = Selection.builder(Account.class).includedPropNames(null).excludedPropNames(null).build();

        assertNull(selection.includedPropNames());
        assertNull(selection.excludedPropNames());
    }

    @Test
    void testPropertyCollectionsAreDefensivelyCopiedAndImmutable() {
        final List<String> included = new ArrayList<>(Arrays.asList("id", "firstName"));
        final Set<String> excluded = new LinkedHashSet<>(Arrays.asList("status"));
        final Selection.SelectionBuilder builder = Selection.builder(Account.class).includedPropNames(included).excludedPropNames(excluded);
        final Selection selection = builder.build();

        included.add("lastName");
        excluded.add("createTime");
        builder.includedPropNames(List.of("changed")).excludedPropNames(Set.of("changed"));

        assertEquals(Arrays.asList("id", "firstName"), selection.includedPropNames());
        assertEquals(Set.of("status"), selection.excludedPropNames());
        assertThrows(UnsupportedOperationException.class, () -> selection.includedPropNames().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> selection.excludedPropNames().add("other"));
    }

    @Test
    void testSelectionDeclaresExplicitReadOnlyAccessors() throws NoSuchMethodException {
        assertEquals(Class.class, Selection.class.getMethod("entityClass").getReturnType());
        assertEquals(String.class, Selection.class.getMethod("tableAlias").getReturnType());
        assertEquals(String.class, Selection.class.getMethod("classAlias").getReturnType());
        assertEquals(boolean.class, Selection.class.getMethod("includesSubEntityProperties").getReturnType());
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("entityClass", Class.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("tableAlias", String.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("classAlias", String.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("includeSubEntityProperties", boolean.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("includedPropNames", Collection.class));
        assertThrows(NoSuchMethodException.class, () -> Selection.class.getMethod("excludedPropNames", Set.class));
    }

    // Regression: keeping a sorted set's comparator made equals asymmetric: a case-insensitive {"FIRSTNAME"} equaled a hash
    // set {"firstName"} (TreeSet.equals asks the tree), but not the other way round, and their hash codes differed.
    @Test
    void testSortedExcludedPropNamesEqualityIsSymmetricAndHashConsistent() {
        final Set<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("FIRSTNAME");
        final Selection sorted = Selection.builder(Account.class).excludedPropNames(caseInsensitive).build();
        final Selection hashed = Selection.builder(Account.class).excludedPropNames(Set.of("firstName")).build();

        assertNotEquals(sorted, hashed);
        assertNotEquals(hashed, sorted);

        // Same comparator and names: equal both ways, with equal hash codes.
        final Set<String> sameNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sameNames.add("FIRSTNAME");
        final Selection sortedCopy = Selection.builder(Account.class).excludedPropNames(sameNames).build();
        assertEquals(sorted, sortedCopy);
        assertEquals(sortedCopy, sorted);
        assertEquals(sorted.hashCode(), sortedCopy.hashCode());

        // Names that the comparator alone treats as equal still differ, as their hash codes do.
        final Set<String> otherSpelling = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        otherSpelling.add("firstName");
        assertNotEquals(sorted, Selection.builder(Account.class).excludedPropNames(otherSpelling).build());

        // A natural-order sorted set matches names exactly, like a hash set, so the two compare as plain sets.
        final Selection natural = Selection.builder(Account.class).excludedPropNames(new TreeSet<>(Set.of("firstName"))).build();
        assertEquals(natural, hashed);
        assertEquals(hashed, natural);
        assertEquals(natural.hashCode(), hashed.hashCode());
    }

    @Test
    void testSortedExcludedPropNamesKeepComparator() {
        final Set<String> excluded = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        excluded.add("FIRSTNAME");
        excluded.add("CONTACT");
        final Selection selection = Selection.builder(Account.class).excludedPropNames(excluded).build();

        // A case-insensitive TreeSet excludes the same properties through a Selection as through select(Class, Set).
        final String expected = "SELECT id AS \"id\", gui AS \"gui\", email_address AS \"emailAddress\", middle_name AS \"middleName\", "
                + "last_name AS \"lastName\", birth_date AS \"birthDate\", status AS \"status\", last_update_time AS \"lastUpdateTime\", "
                + "create_time AS \"createTime\" FROM account";
        assertEquals(expected, PSC.select(Account.class, excluded).from("account").build().query());
        assertEquals(expected, PSC.select(selection).from("account").build().query());
        assertTrue(selection.excludedPropNames().contains("firstName"));
        assertThrows(UnsupportedOperationException.class, () -> selection.excludedPropNames().add("other"));

        // A plain hash set still matches property names exactly, so it excludes other properties and is not equal.
        final Selection hashed = Selection.builder(Account.class).excludedPropNames(Set.of("CONTACT", "FIRSTNAME")).build();
        assertNotEquals(hashed, selection);
        assertNotEquals(selection, hashed);
        assertTrue(PSC.select(hashed).from("account").build().query().contains("first_name AS \"firstName\""));
    }

    @Test
    void testToStringContainsFieldValues() {
        final String str = Selection.builder(Account.class).tableAlias("a").classAlias("account").build().toString();

        assertNotNull(str);
        assertTrue(str.contains("Selection("));
        assertTrue(str.contains("tableAlias=a"));
        assertTrue(str.contains("classAlias=account"));
    }

    private static List<Selection> changingSelectionList(final Selection firstPass, final Selection laterPass) {
        return new AbstractList<>() {
            private int iteratorCount;

            @Override
            public Iterator<Selection> iterator() {
                return List.of(++iteratorCount == 1 ? firstPass : laterPass).iterator();
            }

            @Override
            public Selection get(final int index) {
                if (index != 0) {
                    throw new IndexOutOfBoundsException(index);
                }

                return firstPass;
            }

            @Override
            public int size() {
                return 1;
            }
        };
    }
}
