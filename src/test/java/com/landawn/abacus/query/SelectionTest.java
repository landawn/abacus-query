package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
class Selection2025Test extends TestBase {

    @Test
    public void testNoArgsConstructor() {
        Selection selection = new Selection();
        assertNotNull(selection);
        assertNull(selection.entityClass());
        assertNull(selection.tableAlias());
        assertNull(selection.classAlias());
        assertNull(selection.selectPropNames());
        assertFalse(selection.includeSubEntityProperties());
        assertNull(selection.excludedPropNames());
    }

    @Test
    public void testAllArgsConstructor() {
        List<String> propNames = Arrays.asList("id", "name");
        Set<String> excluded = new HashSet<>(Arrays.asList("password"));

        Selection selection = new Selection(String.class, "t", "alias", propNames, true, excluded);

        assertEquals(String.class, selection.entityClass());
        assertEquals("t", selection.tableAlias());
        assertEquals("alias", selection.classAlias());
        assertEquals(propNames, selection.selectPropNames());
        assertTrue(selection.includeSubEntityProperties());
        assertEquals(excluded, selection.excludedPropNames());
    }

    @Test
    public void testFluentSetters() {
        List<String> propNames = Arrays.asList("id", "name");
        Set<String> excluded = new HashSet<>(Arrays.asList("password"));

        Selection selection = new Selection().entityClass(String.class)
                .tableAlias("t")
                .classAlias("alias")
                .selectPropNames(propNames)
                .includeSubEntityProperties(true)
                .excludedPropNames(excluded);

        assertEquals(String.class, selection.entityClass());
        assertEquals("t", selection.tableAlias());
        assertEquals("alias", selection.classAlias());
        assertEquals(propNames, selection.selectPropNames());
        assertTrue(selection.includeSubEntityProperties());
        assertEquals(excluded, selection.excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_Create() {
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder();
        assertNotNull(builder);
    }

    @Test
    public void testMultiSelectionBuilder_AddSimple() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertNull(selections.get(0).tableAlias());
        assertNull(selections.get(0).classAlias());
        assertNull(selections.get(0).selectPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithPropNames() {
        List<String> propNames = Arrays.asList("id", "name");

        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, propNames).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals(propNames, selections.get(0).selectPropNames());
        assertNull(selections.get(0).tableAlias());
        assertNull(selections.get(0).classAlias());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithAliases() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, "t", "alias").build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals("t", selections.get(0).tableAlias());
        assertEquals("alias", selections.get(0).classAlias());
        assertNull(selections.get(0).selectPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithFull() {
        List<String> propNames = Arrays.asList("id", "name");

        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, "t", "alias", propNames).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals("t", selections.get(0).tableAlias());
        assertEquals("alias", selections.get(0).classAlias());
        assertEquals(propNames, selections.get(0).selectPropNames());
        assertFalse(selections.get(0).includeSubEntityProperties());
        assertNull(selections.get(0).excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_DefensiveCopyForSelectPropNames() {
        List<String> propNames = new ArrayList<>(Arrays.asList("id", "name"));
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, "t", "alias", propNames).build();

        propNames.add("mutated");

        assertEquals(Arrays.asList("id", "name"), selections.get(0).selectPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithSubEntity() {
        Set<String> excluded = new HashSet<>(Arrays.asList("password"));

        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, true, excluded).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertTrue(selections.get(0).includeSubEntityProperties());
        assertEquals(excluded, selections.get(0).excludedPropNames());
        assertNull(selections.get(0).tableAlias());
        assertNull(selections.get(0).classAlias());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithAllOptions() {
        Set<String> excluded = new HashSet<>(Arrays.asList("password"));

        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, "t", "alias", true, excluded).build();

        assertEquals(1, selections.size());
        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals("t", selections.get(0).tableAlias());
        assertEquals("alias", selections.get(0).classAlias());
        assertTrue(selections.get(0).includeSubEntityProperties());
        assertEquals(excluded, selections.get(0).excludedPropNames());
        assertNull(selections.get(0).selectPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_DefensiveCopyForExcludedPropNames() {
        Set<String> excluded = new LinkedHashSet<>(Arrays.asList("password", "token"));
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, "t", "alias", true, excluded).build();

        excluded.add("mutated");

        assertEquals(new LinkedHashSet<>(Arrays.asList("password", "token")), selections.get(0).excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_MultipleAdds() {
        List<String> propNames1 = Arrays.asList("id", "name");
        List<String> propNames2 = Arrays.asList("orderId", "orderDate");

        List<Selection> selections = Selection.multiSelectionBuilder()
                .add(String.class, "u", "user", propNames1)
                .add(Integer.class, "o", "order", propNames2)
                .add(Double.class)
                .build();

        assertEquals(3, selections.size());

        assertEquals(String.class, selections.get(0).entityClass());
        assertEquals("u", selections.get(0).tableAlias());
        assertEquals("user", selections.get(0).classAlias());
        assertEquals(propNames1, selections.get(0).selectPropNames());

        assertEquals(Integer.class, selections.get(1).entityClass());
        assertEquals("o", selections.get(1).tableAlias());
        assertEquals("order", selections.get(1).classAlias());
        assertEquals(propNames2, selections.get(1).selectPropNames());

        assertEquals(Double.class, selections.get(2).entityClass());
        assertNull(selections.get(2).tableAlias());
    }

    @Test
    public void testMultiSelectionBuilder_Build() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class).add(Integer.class).build();

        assertNotNull(selections);
        assertEquals(2, selections.size());
    }

    @Test
    public void testMultiSelectionBuilder_BuildReturnsSnapshot() {
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder().add(String.class);
        List<Selection> firstBuild = builder.build();

        builder.add(Integer.class);
        List<Selection> secondBuild = builder.build();

        assertEquals(1, firstBuild.size());
        assertEquals(2, secondBuild.size());
        assertThrows(UnsupportedOperationException.class, () -> firstBuild.add(new Selection()));
    }

    @Test
    public void testMultiSelectionBuilder_EmptyBuild() {
        List<Selection> selections = Selection.multiSelectionBuilder().build();

        assertNotNull(selections);
        assertEquals(0, selections.size());
    }

    @Test
    public void testMultiSelectionBuilder_Chaining() {
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder();
        Selection.MultiSelectionBuilder result = builder.add(String.class);

        assertEquals(builder, result);
    }

    @Test
    public void testIncludeSubEntityPropertiesDefault() {
        Selection selection = new Selection();
        assertFalse(selection.includeSubEntityProperties());
    }

    @Test
    public void testIncludeSubEntityPropertiesTrue() {
        Selection selection = new Selection();
        selection.includeSubEntityProperties(true);
        assertTrue(selection.includeSubEntityProperties());
    }

    @Test
    public void testIncludeSubEntityPropertiesFalse() {
        Selection selection = new Selection();
        selection.includeSubEntityProperties(false);
        assertFalse(selection.includeSubEntityProperties());
    }

    @Test
    public void testSettersReturnSelf() {
        Selection selection = new Selection();
        Selection result;

        result = selection.entityClass(String.class);
        assertEquals(selection, result);

        result = selection.tableAlias("t");
        assertEquals(selection, result);

        result = selection.classAlias("alias");
        assertEquals(selection, result);

        result = selection.selectPropNames(Arrays.asList("id"));
        assertEquals(selection, result);

        result = selection.includeSubEntityProperties(true);
        assertEquals(selection, result);

        result = selection.excludedPropNames(new HashSet<>());
        assertEquals(selection, result);
    }

    @Test
    public void testNullValues() {
        Selection selection = new Selection().entityClass(null).tableAlias(null).classAlias(null).selectPropNames(null).excludedPropNames(null);

        assertNull(selection.entityClass());
        assertNull(selection.tableAlias());
        assertNull(selection.classAlias());
        assertNull(selection.selectPropNames());
        assertNull(selection.excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilder_ApplyWithPSC() {
        // Test that apply method works with a function
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder().add(String.class, "t", "alias");

        // We can't test actual PSC::selectFrom without full SqlBuilder setup,
        // but we can test that apply returns a result from the function
        com.landawn.abacus.query.SqlBuilder result = builder.apply(selections -> {
            assertNotNull(selections);
            assertEquals(1, selections.size());
            return null; // Return null since we can't create actual SqlBuilder in unit test
        });

        assertNull(result); // Expected since our test function returns null
    }

    @Test
    public void testMultiSelectionBuilder_ApplyExecutesFunction() {
        final boolean[] functionCalled = { false };

        com.landawn.abacus.query.SqlBuilder result = Selection.multiSelectionBuilder().add(String.class).add(Integer.class).apply(selections -> {
            functionCalled[0] = true;
            assertEquals(2, selections.size());
            return null;
        });

        assertTrue(functionCalled[0]);
        assertNull(result);
    }

    @Test
    public void testMultiSelectionBuilder_ApplyRejectsNullFunction() {
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder().add(String.class);
        assertThrows(IllegalArgumentException.class, () -> builder.apply(null));
    }

    @Test
    public void testSelectionEquality() {
        List<String> propNames = Arrays.asList("id", "name");

        Selection s1 = new Selection(String.class, "t", "alias", propNames, false, null);
        Selection s2 = new Selection(String.class, "t", "alias", propNames, false, null);

        // Lombok @Data should provide equals
        assertEquals(s1, s2);
    }

    @Test
    public void testSelectionHashCode() {
        List<String> propNames = Arrays.asList("id", "name");

        Selection s1 = new Selection(String.class, "t", "alias", propNames, false, null);
        Selection s2 = new Selection(String.class, "t", "alias", propNames, false, null);

        // Lombok @Data should provide hashCode
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testSelectionToString() {
        Selection selection = new Selection(String.class, "t", "alias", Arrays.asList("id"), true, null);

        // Lombok @Data should provide toString
        String str = selection.toString();
        assertNotNull(str);
        assertTrue(str.contains("String"));
    }

    @Test
    public void testMultiSelectionBuilder_AddNullEntity() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(null).build();

        assertEquals(1, selections.size());
        assertNull(selections.get(0).entityClass());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithEmptyPropNames() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, Arrays.asList()).build();

        assertEquals(1, selections.size());
        assertTrue(selections.get(0).selectPropNames().isEmpty());
    }

    @Test
    public void testMultiSelectionBuilder_AddWithEmptyExcluded() {
        Set<String> emptySet = new HashSet<>();
        List<Selection> selections = Selection.multiSelectionBuilder().add(String.class, false, emptySet).build();

        assertEquals(1, selections.size());
        assertTrue(selections.get(0).excludedPropNames().isEmpty());
    }
}

public class SelectionTest extends TestBase {

    static class User {
        private Long id;
        private String name;
        private String email;

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    static class Order {
        private Long id;
        private String orderDate;
        private Double total;

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(String orderDate) {
            this.orderDate = orderDate;
        }

        public Double getTotal() {
            return total;
        }

        public void setTotal(Double total) {
            this.total = total;
        }
    }

    static class Product {
        private Long id;
        private String name;
        private Double price;
        private Double cost;

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public Double getCost() {
            return cost;
        }

        public void setCost(Double cost) {
            this.cost = cost;
        }
    }

    @Test
    public void testSelectionConstructorsAndAccessors() {
        // Test no-args constructor
        Selection selection = new Selection();
        assertNull(selection.entityClass());
        assertNull(selection.tableAlias());
        assertNull(selection.classAlias());
        assertNull(selection.selectPropNames());
        assertFalse(selection.includeSubEntityProperties());
        assertNull(selection.excludedPropNames());

        // Test fluent setters
        Set<String> excludedProps = new HashSet<>(Arrays.asList("internalField"));
        selection.entityClass(User.class)
                .tableAlias("u")
                .classAlias("user")
                .selectPropNames(Arrays.asList("id", "name"))
                .includeSubEntityProperties(true)
                .excludedPropNames(excludedProps);

        assertEquals(User.class, selection.entityClass());
        assertEquals("u", selection.tableAlias());
        assertEquals("user", selection.classAlias());
        assertEquals(Arrays.asList("id", "name"), selection.selectPropNames());
        assertTrue(selection.includeSubEntityProperties());
        assertEquals(excludedProps, selection.excludedPropNames());

        // Test all-args constructor
        Selection selection2 = new Selection(Order.class, "o", "order", Arrays.asList("id", "orderDate"), false, new HashSet<>(Arrays.asList("internalNote")));

        assertEquals(Order.class, selection2.entityClass());
        assertEquals("o", selection2.tableAlias());
        assertEquals("order", selection2.classAlias());
        assertEquals(Arrays.asList("id", "orderDate"), selection2.selectPropNames());
        assertFalse(selection2.includeSubEntityProperties());
        assertEquals(new HashSet<>(Arrays.asList("internalNote")), selection2.excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilder() {
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder();
        assertNotNull(builder);

        // Test add(Class)
        builder.add(User.class);

        // Test add(Class, Collection)
        builder.add(Order.class, Arrays.asList("id", "orderDate", "total"));

        // Test add(Class, String, String)
        builder.add(Product.class, "p", "product");

        // Test add(Class, String, String, Collection)
        builder.add(User.class, "u2", "user2", Arrays.asList("name", "email"));

        // Test add(Class, boolean, Set)
        builder.add(Order.class, true, new HashSet<>(Arrays.asList("internalField")));

        // Test add(Class, String, String, boolean, Set)
        builder.add(Product.class, "p2", "product2", false, new HashSet<>(Arrays.asList("cost")));

        // Build and verify
        List<Selection> selections = builder.build();
        assertEquals(6, selections.size());

        // Verify first selection (simple class)
        Selection sel1 = selections.get(0);
        assertEquals(User.class, sel1.entityClass());
        assertNull(sel1.tableAlias());
        assertNull(sel1.classAlias());
        assertNull(sel1.selectPropNames());
        assertFalse(sel1.includeSubEntityProperties());
        assertNull(sel1.excludedPropNames());

        // Verify second selection (with properties)
        Selection sel2 = selections.get(1);
        assertEquals(Order.class, sel2.entityClass());
        assertEquals(Arrays.asList("id", "orderDate", "total"), sel2.selectPropNames());

        // Verify third selection (with aliases)
        Selection sel3 = selections.get(2);
        assertEquals(Product.class, sel3.entityClass());
        assertEquals("p", sel3.tableAlias());
        assertEquals("product", sel3.classAlias());

        // Verify fourth selection (full config)
        Selection sel4 = selections.get(3);
        assertEquals(User.class, sel4.entityClass());
        assertEquals("u2", sel4.tableAlias());
        assertEquals("user2", sel4.classAlias());
        assertEquals(Arrays.asList("name", "email"), sel4.selectPropNames());

        // Verify fifth selection (with sub-entities and exclusions)
        Selection sel5 = selections.get(4);
        assertEquals(Order.class, sel5.entityClass());
        assertTrue(sel5.includeSubEntityProperties());
        assertEquals(new HashSet<>(Arrays.asList("internalField")), sel5.excludedPropNames());

        // Verify sixth selection (complete config)
        Selection sel6 = selections.get(5);
        assertEquals(Product.class, sel6.entityClass());
        assertEquals("p2", sel6.tableAlias());
        assertEquals("product2", sel6.classAlias());
        assertFalse(sel6.includeSubEntityProperties());
        assertEquals(new HashSet<>(Arrays.asList("cost")), sel6.excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilderApply() {
        // Create a mock function to test apply
        List<Selection> capturedSelections = null;

        SqlBuilder result = Selection.multiSelectionBuilder().add(User.class, "u", "user").add(Order.class, "o", "order").apply(selections -> {
            // Capture selections for verification
            assertNotNull(selections);
            assertEquals(2, selections.size());
            return new SqlBuilder.PSC();
        });

        assertNotNull(result);
        assertTrue(result instanceof SqlBuilder);
    }

    @Test
    public void testMultiSelectionBuilderChaining() {
        // Test that all add methods return the builder for chaining
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder();

        Selection.MultiSelectionBuilder result = builder.add(User.class)
                .add(Order.class, Arrays.asList("id", "total"))
                .add(Product.class, "p", "product")
                .add(User.class, "u", "user", Arrays.asList("name"))
                .add(Order.class, true, new HashSet<>())
                .add(Product.class, "p2", "product2", false, new HashSet<>());

        assertSame(builder, result);

        List<Selection> selections = result.build();
        assertEquals(6, selections.size());
    }
}

class SelectionJavadocExamples extends TestBase {

    @Test
    public void testSelection_simpleSelection() {
        Selection userSelection = new Selection().entityClass(String.class).selectPropNames(Arrays.asList("id", "name", "email"));
        assertNotNull(userSelection);
        assertEquals(String.class, userSelection.entityClass());
        assertEquals(Arrays.asList("id", "name", "email"), userSelection.selectPropNames());
    }

    @Test
    public void testSelection_withAliases() {
        Selection orderSelection = new Selection().entityClass(Object.class)
                .tableAlias("o")
                .classAlias("order")
                .includeSubEntityProperties(true)
                .excludedPropNames(Set.of("internalNotes"));
        assertEquals("o", orderSelection.tableAlias());
        assertEquals("order", orderSelection.classAlias());
        assertTrue(orderSelection.includeSubEntityProperties());
        assertTrue(orderSelection.excludedPropNames().contains("internalNotes"));
    }

    @Test
    public void testSelection_multiSelectionBuilder() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(Object.class, "u", "user").add(Object.class, "a", "address").build();
        assertEquals(2, selections.size());
        assertEquals("u", selections.get(0).tableAlias());
        assertEquals("user", selections.get(0).classAlias());
        assertEquals("a", selections.get(1).tableAlias());
        assertEquals("address", selections.get(1).classAlias());
    }

    @Test
    public void testSelection_multiSelectionBuilderWithProps() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(Object.class, "u", "user", Arrays.asList("id", "name")).build();
        assertEquals(1, selections.size());
        assertEquals(Arrays.asList("id", "name"), selections.get(0).selectPropNames());
    }

    @Test
    public void testSelection_multiSelectionBuilderSimple() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(Object.class).build();
        assertEquals(1, selections.size());
        assertNull(selections.get(0).tableAlias());
    }

    @Test
    public void testSelection_multiSelectionBuilderWithExcluded() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(Object.class, true, Set.of("password", "internalNotes")).build();
        assertEquals(1, selections.size());
        assertTrue(selections.get(0).includeSubEntityProperties());
        assertTrue(selections.get(0).excludedPropNames().contains("password"));
    }
}
