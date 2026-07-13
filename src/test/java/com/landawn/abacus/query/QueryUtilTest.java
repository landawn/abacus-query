package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonColumn;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;

@Tag("2025")
public class QueryUtilTest extends TestBase {
    @Table("value_form_accounts")
    static class ValueFormEntity {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }
    }

    @Table(alias = "ao")
    static class AliasOnlyEntity {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }
    }

    @Test
    public void testGetTableNameAndAliasHonorsTableValueAndAliasOnly() {
        // Regression (2026-07-03): only @Table.name() was read, so the deprecated @Table("...") value
        // form returned "" and an alias-only @Table returned " ao" (leading-space garbage) -- both
        // diverging from the FROM clause the query builders render for the same class.
        assertEquals("value_form_accounts", QueryUtil.tableNameAndAlias(ValueFormEntity.class, NamingPolicy.SNAKE_CASE));
        assertEquals("alias_only_entity ao", QueryUtil.tableNameAndAlias(AliasOnlyEntity.class, NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testGetColumn2PropNameMap() {
        ImmutableMap<String, String> map = QueryUtil.columnToPropertyMap(Account.class);
        assertNotNull(map);
    }

    @Test
    public void testGetColumn2PropNameMap_NullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.columnToPropertyMap(null);
        });
    }

    @Test
    public void testGetProp2ColumnNameMap() {
        ImmutableMap<String, String> map = QueryUtil.propertyToColumnMap(Account.class, NamingPolicy.SNAKE_CASE);
        assertNotNull(map);
    }

    @Test
    public void testGetProp2ColumnNameMap_NoChange() {
        ImmutableMap<String, String> map = QueryUtil.propertyToColumnMap(Account.class, NamingPolicy.NO_CHANGE);
        assertNotNull(map);
    }

    @Test
    public void testGetProp2ColumnNameMap_UpperCase() {
        ImmutableMap<String, String> map = QueryUtil.propertyToColumnMap(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        assertNotNull(map);
    }

    @Test
    public void testGetProp2ColumnNameMap_NullClass() {
        ImmutableMap<String, String> map = QueryUtil.propertyToColumnMap(null, NamingPolicy.SNAKE_CASE);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetProp2ColumnNameMap_MapClass() {
        ImmutableMap<String, String> map = QueryUtil.propertyToColumnMap(HashMap.class, NamingPolicy.SNAKE_CASE);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetInsertPropNames_WithEntity() {
        Account account = new Account();
        Collection<String> props = QueryUtil.insertPropertyNames(account, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetInsertPropNames_WithExcludedProps() {
        Account account = new Account();
        Set<String> excluded = new HashSet<>();
        excluded.add("id");
        Collection<String> props = QueryUtil.insertPropertyNames(account, excluded);
        assertNotNull(props);
        assertFalse(props.contains("id"));
    }

    @Test
    public void testGetInsertPropNames_WithClass() {
        Collection<String> props = QueryUtil.insertPropertyNames(Account.class, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetInsertPropNames_WithClassNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.insertPropertyNames(null, Collections.emptySet());
        });
    }

    @Test
    public void testGetInsertPropNames_WithEntityNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> QueryUtil.insertPropertyNames((Object) null, Collections.emptySet()));
    }

    @Test
    public void testGetInsertPropNames_WithClassAndExcluded() {
        Set<String> excluded = new HashSet<>();
        excluded.add("createdTime");
        Collection<String> props = QueryUtil.insertPropertyNames(Account.class, excluded);
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames() {
        Collection<String> props = QueryUtil.selectPropertyNames(Account.class, false, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames_IncludeSubEntity() {
        Collection<String> props = QueryUtil.selectPropertyNames(Account.class, true, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames_WithExcluded() {
        Set<String> excluded = new HashSet<>();
        excluded.add("id");
        Collection<String> props = QueryUtil.selectPropertyNames(Account.class, false, excluded);
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.selectPropertyNames(null, false, Collections.emptySet());
        });
    }

    @Test
    public void testGetUpdatePropNames() {
        Collection<String> props = QueryUtil.updatePropertyNames(Account.class, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetUpdatePropNames_WithExcluded() {
        Set<String> excluded = new HashSet<>();
        excluded.add("createdTime");
        Collection<String> props = QueryUtil.updatePropertyNames(Account.class, excluded);
        assertNotNull(props);
    }

    @Test
    public void testGetUpdatePropNames_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.updatePropertyNames(null, Collections.emptySet());
        });
    }

    @Test
    public void testGetIdFieldNames() {
        List<String> ids = QueryUtil.idPropertyNames(Account.class);
        assertNotNull(ids);
    }

    @Test
    public void testGetIdFieldNames_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.idPropertyNames(null);
        });
    }

    @Test
    public void testRepeatQuestionMark() {
        String qm = QueryUtil.placeholders(3);
        assertEquals("?, ?, ?", qm);
    }

    @Test
    public void testRepeatQuestionMark_One() {
        String qm = QueryUtil.placeholders(1);
        assertEquals("?", qm);
    }

    @Test
    public void testRepeatQuestionMark_Zero() {
        String qm = QueryUtil.placeholders(0);
        assertEquals("", qm);
    }

    @Test
    public void testRepeatQuestionMark_Large() {
        String qm = QueryUtil.placeholders(100);
        assertNotNull(qm);
        assertTrue(qm.contains("?"));
    }

    @Test
    public void testRepeatQuestionMark_Negative() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.placeholders(-1);
        });
    }

    @Test
    public void testRepeatQuestionMark_Cached() {
        String qm1 = QueryUtil.placeholders(5);
        String qm2 = QueryUtil.placeholders(5);
        assertEquals(qm1, qm2);
    }

    @Test
    public void testRepeatQuestionMark_SpecialCachedValues() {
        assertNotNull(QueryUtil.placeholders(100));
        assertNotNull(QueryUtil.placeholders(200));
        assertNotNull(QueryUtil.placeholders(300));
        assertNotNull(QueryUtil.placeholders(500));
        assertNotNull(QueryUtil.placeholders(1000));
    }

    @Test
    public void testGetTableAlias() {
        String alias = QueryUtil.tableAlias(Account.class);
        assertEquals("acc", alias);
    }

    @Test
    public void testGetTableAlias_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.tableAlias(null);
        });
    }

    @Test
    public void testGetTableNameAndAlias() {
        String tableName = QueryUtil.tableNameAndAlias(Account.class);
        assertNotNull(tableName);
    }

    @Test
    public void testGetTableNameAndAlias_WithNamingPolicy() {
        String tableName = QueryUtil.tableNameAndAlias(Account.class, NamingPolicy.SNAKE_CASE);
        assertNotNull(tableName);
    }

    @Test
    public void testGetTableNameAndAlias_UpperCase() {
        String tableName = QueryUtil.tableNameAndAlias(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        assertNotNull(tableName);
    }

    @Test
    public void testGetTableNameAndAlias_NoChange() {
        String tableName = QueryUtil.tableNameAndAlias(Account.class, NamingPolicy.NO_CHANGE);
        assertNotNull(tableName);
    }

    @Test
    public void testGetTableNameAndAlias_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.tableNameAndAlias(null);
        });
    }

    @Test
    public void testGetTableNameAndAlias_NullCheckWithPolicy() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.tableNameAndAlias(null, NamingPolicy.SNAKE_CASE);
        });
    }

    @Test
    public void testGetProp2ColumnNameMap_DifferentPolicies() {
        ImmutableMap<String, String> lower = QueryUtil.propertyToColumnMap(Account.class, NamingPolicy.SNAKE_CASE);
        ImmutableMap<String, String> upper = QueryUtil.propertyToColumnMap(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        ImmutableMap<String, String> noChange = QueryUtil.propertyToColumnMap(Account.class, NamingPolicy.NO_CHANGE);

        assertNotNull(lower);
        assertNotNull(upper);
        assertNotNull(noChange);
    }

    @Test
    public void testGetInsertPropNames_EmptyExcluded() {
        Collection<String> props = QueryUtil.insertPropertyNames(Account.class, new HashSet<>());
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames_EmptyExcluded() {
        Collection<String> props = QueryUtil.selectPropertyNames(Account.class, false, new HashSet<>());
        assertNotNull(props);
    }

    @Test
    public void testGetUpdatePropNames_EmptyExcluded() {
        Collection<String> props = QueryUtil.updatePropertyNames(Account.class, new HashSet<>());
        assertNotNull(props);
    }

    @Test
    public void testRepeatQuestionMark_AllCachedValues() {
        for (int i = 0; i <= 30; i++) {
            String result = QueryUtil.placeholders(i);
            assertNotNull(result);
            if (i == 0) {
                assertEquals("", result);
            } else if (i == 1) {
                assertEquals("?", result);
            } else {
                assertTrue(result.startsWith("?"));
            }
        }
    }

    @Test
    public void testRepeatQuestionMark_NonCachedValue() {
        String result = QueryUtil.placeholders(50);
        assertNotNull(result);
        assertEquals(148, result.length()); // "?, " * 50 = 150 - 2 = 148 characters
    }

    @Test
    public void testProp2ColumnNameMap() {
        ImmutableMap<String, Tuple.Tuple2<String, Boolean>> map = QueryUtil.prop2ColumnNameMap(Account.class, NamingPolicy.SNAKE_CASE);
        assertNotNull(map);
    }

    @Test
    public void testProp2ColumnNameMap_UpperCase() {
        ImmutableMap<String, Tuple.Tuple2<String, Boolean>> map = QueryUtil.prop2ColumnNameMap(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        assertNotNull(map);
    }

    @Test
    public void testProp2ColumnNameMap_NoChange() {
        ImmutableMap<String, Tuple.Tuple2<String, Boolean>> map = QueryUtil.prop2ColumnNameMap(Account.class, NamingPolicy.NO_CHANGE);
        assertNotNull(map);
    }

    @Test
    public void testProp2ColumnNameMap_NullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.prop2ColumnNameMap(null, NamingPolicy.SNAKE_CASE);
        });
    }

    @Test
    public void testIsNonColumn() {
        Set<String> columnFields = new HashSet<>();
        columnFields.add("id");
        columnFields.add("firstName");

        Set<String> nonColumnFields = new HashSet<>();
        nonColumnFields.add("tempField");

        BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");

        assertFalse(QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo));
    }

    @Test
    public void testIsNonColumn_NonColumnField() {
        Set<String> columnFields = new HashSet<>();
        columnFields.add("id");

        Set<String> nonColumnFields = new HashSet<>();
        nonColumnFields.add("tempField");

        BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
        PropInfo tempProp = beanInfo.getPropInfo("tempField");

        if (tempProp != null) {
            assertTrue(QueryUtil.isNonColumn(columnFields, nonColumnFields, tempProp));
        }
    }

    @Test
    public void testIsNonColumn_EmptySets() {
        Set<String> columnFields = new HashSet<>();
        Set<String> nonColumnFields = new HashSet<>();

        BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");

        boolean result = QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo);
        assertFalse(result);
    }

    @Table(name = "test_user", alias = "tu", columnFields = { "id", "name" }, nonColumnFields = { "tempData" })
    static class TestUser {
        @Id
        private Long id;

        @Column("user_name")
        private String name;

        private String email;

        @NonColumn
        private String nonColumnField;

        @Column
        private String tempData;

        private TestAddress address;

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

        public String getNonColumnField() {
            return nonColumnField;
        }

        public void setNonColumnField(String notColumnField) {
            this.nonColumnField = notColumnField;
        }

        public String getTempData() {
            return tempData;
        }

        public void setTempData(String tempData) {
            this.tempData = tempData;
        }

        public TestAddress getAddress() {
            return address;
        }

        public void setAddress(TestAddress address) {
            this.address = address;
        }
    }

    static class TestAddress {
        private String street;
        private String city;

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    static class NestedLeaf {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    static class NestedBranch {
        private NestedLeaf firstLeaf;
        private NestedLeaf secondLeaf;

        public NestedLeaf getFirstLeaf() {
            return firstLeaf;
        }

        public void setFirstLeaf(NestedLeaf firstLeaf) {
            this.firstLeaf = firstLeaf;
        }

        public NestedLeaf getSecondLeaf() {
            return secondLeaf;
        }

        public void setSecondLeaf(NestedLeaf secondLeaf) {
            this.secondLeaf = secondLeaf;
        }
    }

    static class NestedRoot {
        private NestedBranch branch;

        public NestedBranch getBranch() {
            return branch;
        }

        public void setBranch(NestedBranch branch) {
            this.branch = branch;
        }
    }

    static class DeepNestedRoot {
        private DeepNestedMiddle middle;

        public DeepNestedMiddle getMiddle() {
            return middle;
        }

        public void setMiddle(DeepNestedMiddle middle) {
            this.middle = middle;
        }
    }

    static class DeepNestedMiddle {
        private DeepNestedLeaf leaf;

        public DeepNestedLeaf getLeaf() {
            return leaf;
        }

        public void setLeaf(DeepNestedLeaf leaf) {
            this.leaf = leaf;
        }
    }

    static class DeepNestedLeaf {
        private DeepNestedTail tail;

        public DeepNestedTail getTail() {
            return tail;
        }

        public void setTail(DeepNestedTail tail) {
            this.tail = tail;
        }
    }

    static class DeepNestedTail {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    static class SimpleEntity {
        @Id
        private Integer id;
        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Table(name = "aliasless_table")
    static class AliaslessEntity {
        private String simpleValue;

        public String getSimpleValue() {
            return simpleValue;
        }

        public void setSimpleValue(String simpleValue) {
            this.simpleValue = simpleValue;
        }
    }

    static class NonColumnOnlyEntity {
        @NonColumn
        private String hiddenValue;

        public String getHiddenValue() {
            return hiddenValue;
        }

        public void setHiddenValue(String hiddenValue) {
            this.hiddenValue = hiddenValue;
        }
    }

    static class NoIdEntity {
        private String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    static class SelfReferentialEntity {
        @Id
        private Long id;
        private SelfReferentialEntity parent;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public SelfReferentialEntity getParent() {
            return parent;
        }

        public void setParent(SelfReferentialEntity parent) {
            this.parent = parent;
        }
    }

    @Test
    public void testGetProp2ColumnNameMapWithRepeatedNestedType() {
        final ImmutableMap<String, String> result = QueryUtil.propertyToColumnMap(NestedRoot.class, NamingPolicy.SNAKE_CASE);

        assertTrue(result.containsKey("branch.firstLeaf.value"));
        assertTrue(result.containsKey("branch.secondLeaf.value"));
    }

    @Test
    public void testGetProp2ColumnNameMapCapsDeepNestedExpansion() {
        final ImmutableMap<String, String> result = QueryUtil.propertyToColumnMap(DeepNestedRoot.class, NamingPolicy.SNAKE_CASE);

        assertFalse(result.containsKey("middle.leaf.tail.value"));
        assertFalse(result.containsKey("middle.leaf.tail"));
    }

    @Test
    public void testGetInsertPropNamesWithEntity() {
        TestUser user = new TestUser();

        // Test with default ID value (should exclude ID)
        user.setId(null);
        Set<String> excludedProps = new HashSet<>();
        Collection<String> props = QueryUtil.insertPropertyNames(user, excludedProps);
        assertNotNull(props);
        assertFalse(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));

        // Test with non-default ID value (should include ID)
        user.setId(123L);
        props = QueryUtil.insertPropertyNames(user, excludedProps);
        assertTrue(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));

        // Test with excluded properties
        excludedProps.add("email");
        props = QueryUtil.insertPropertyNames(user, excludedProps);
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));
    }

    @Test
    public void testGetInsertPropNamesWithClass() {
        Set<String> excludedProps = new HashSet<>();

        // Test without exclusions
        Collection<String> props = QueryUtil.insertPropertyNames(TestUser.class, excludedProps);
        assertNotNull(props);
        assertTrue(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));

        // Test with exclusions
        excludedProps.add("email");
        props = QueryUtil.insertPropertyNames(TestUser.class, excludedProps);
        assertTrue(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));
    }

    @Test
    public void testRegisterEntityPropColumnNameMap() {
        // Direct cycle detection should stop recursive expansion instead of failing the entire lookup.
        Set<Class<?>> registeringClasses = new HashSet<>();
        registeringClasses.add(TestUser.class);

        ImmutableMap<String, String> result = QueryUtil.registerEntityPropColumnNameMap(TestUser.class, NamingPolicy.SNAKE_CASE, registeringClasses);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetProp2ColumnNameMap_WithSelfReferentialEntityDoesNotThrow() {
        ImmutableMap<String, String> result = QueryUtil.propertyToColumnMap(SelfReferentialEntity.class, NamingPolicy.SNAKE_CASE);

        assertEquals("id", result.get("id"));
        assertFalse(result.containsKey("parent.parent"));
    }

    @Test
    public void testSqlBuilder_FromSelfReferentialEntityDoesNotFail() {
        String sql = Dsl.PSC.select("id").from(SelfReferentialEntity.class).where(Filters.eq("id", 1L)).build().query();

        assertTrue(sql.contains("FROM self_referential_entity"));
        assertTrue(sql.contains("WHERE id = ?"));
    }

    // --- 2026-06-22 API review: get*PropNames now return immutable lists on BOTH paths ---

    @Test
    public void testGetInsertPropNames_ReturnsImmutableListBothPaths() {
        // no-exclusion path
        ImmutableList<String> noExcl = QueryUtil.insertPropertyNames(TestUser.class, null);
        assertThrows(UnsupportedOperationException.class, () -> noExcl.add("x"));

        // exclusion path
        ImmutableList<String> withExcl = QueryUtil.insertPropertyNames(TestUser.class, new HashSet<>(Arrays.asList("email")));
        assertThrows(UnsupportedOperationException.class, () -> withExcl.add("x"));

        // entity-based no-exclusion path
        ImmutableList<String> entityNoExcl = QueryUtil.insertPropertyNames(new TestUser(), null);
        assertThrows(UnsupportedOperationException.class, () -> entityNoExcl.add("x"));
    }

    @Test
    public void testGetSelectPropNames_ReturnsImmutableListBothPaths() {
        ImmutableList<String> noExcl = QueryUtil.selectPropertyNames(TestUser.class, false, null);
        assertThrows(UnsupportedOperationException.class, () -> noExcl.add("x"));

        ImmutableList<String> withExcl = QueryUtil.selectPropertyNames(TestUser.class, false, new HashSet<>(Arrays.asList("email")));
        assertThrows(UnsupportedOperationException.class, () -> withExcl.add("x"));
    }

    @Test
    public void testGetUpdatePropNames_ReturnsImmutableListBothPaths() {
        ImmutableList<String> noExcl = QueryUtil.updatePropertyNames(TestUser.class, null);
        assertThrows(UnsupportedOperationException.class, () -> noExcl.add("x"));

        ImmutableList<String> withExcl = QueryUtil.updatePropertyNames(TestUser.class, new HashSet<>(Arrays.asList("email")));
        assertThrows(UnsupportedOperationException.class, () -> withExcl.add("x"));
    }

    @Test
    public void testGetSelectPropNames_NoExclusionReturnsStableInstance() {
        // The no-exclusion path is memoized, so repeated calls return the same instance
        // (this identity is relied upon by the builders' == fast paths).
        ImmutableList<String> first = QueryUtil.selectPropertyNames(TestUser.class, false, null);
        ImmutableList<String> second = QueryUtil.selectPropertyNames(TestUser.class, false, null);
        assertSame(first, second);

        // Sub-entity variant uses a different slot, so it is a different (but also stable) instance.
        ImmutableList<String> withSub = QueryUtil.selectPropertyNames(TestUser.class, true, null);
        assertSame(withSub, QueryUtil.selectPropertyNames(TestUser.class, true, null));
    }

    @Test
    public void testGetInsertUpdatePropNames_NoExclusionReturnStableInstance() {
        assertSame(QueryUtil.insertPropertyNames(TestUser.class, null), QueryUtil.insertPropertyNames(TestUser.class, null));
        assertSame(QueryUtil.updatePropertyNames(TestUser.class, null), QueryUtil.updatePropertyNames(TestUser.class, null));
    }

    @Test
    public void testGetSelectPropNames_InstanceOverloadDelegatesToClass() {
        TestUser user = new TestUser();
        ImmutableList<String> viaInstance = QueryUtil.selectPropertyNames(user, false, null);
        ImmutableList<String> viaClass = QueryUtil.selectPropertyNames(TestUser.class, false, null);
        assertEquals(viaClass, viaInstance);
        assertTrue(viaInstance.contains("name"));
        assertFalse(viaInstance.contains("email"));
    }

    @Test
    public void testGetSelectPropNames_InstanceOverloadNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> QueryUtil.selectPropertyNames((Object) null, false, null));
    }

    @Test
    public void testGetUpdatePropNames_InstanceOverloadDelegatesToClass() {
        TestUser user = new TestUser();
        ImmutableList<String> viaInstance = QueryUtil.updatePropertyNames(user, null);
        ImmutableList<String> viaClass = QueryUtil.updatePropertyNames(TestUser.class, null);
        assertEquals(viaClass, viaInstance);
        assertTrue(viaInstance.contains("name"));
        assertFalse(viaInstance.contains("email"));
    }

    @Test
    public void testGetUpdatePropNames_InstanceOverloadNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> QueryUtil.updatePropertyNames((Object) null, null));
    }

    @Test
    public void testIsNonColumn_NullPropInfoThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> QueryUtil.isNonColumn(Collections.emptySet(), Collections.emptySet(), null));
    }

    @Test
    public void testQueryUtil_patternForAlphanumericColumnName() {
        boolean isValid = QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("column_name").matches();
        assertTrue(isValid);
        boolean isInvalid = QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("column name").matches();
        assertFalse(isInvalid);
    }

    @Test
    public void testQueryUtil_RepeatQuestionMark() {
        String placeholders = QueryUtil.placeholders(3);
        assertEquals("?, ?, ?", placeholders);
        String sql = "INSERT INTO users (name, email, age) VALUES (" + placeholders + ")";
        assertEquals("INSERT INTO users (name, email, age) VALUES (?, ?, ?)", sql);
    }

    @Test
    public void testQueryUtil_RepeatQuestionMark_zero() {
        String placeholders = QueryUtil.placeholders(0);
        assertEquals("", placeholders);
    }

    @Test
    public void testPatternForAlphanumericColumnName() {
        assertNotNull(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN);
        assertTrue(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user_name").matches());
        assertTrue(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user123").matches());
        assertTrue(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user-name").matches());
        assertFalse(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user.name").matches());
        assertFalse(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user name").matches());
    }

    @Test
    public void testProp2ColumnNameMap_NullNamingPolicyUsesSnakeCase() {
        ImmutableMap<String, Tuple2<String, Boolean>> result = QueryUtil.prop2ColumnNameMap(QueryUtilTest.AliaslessEntity.class, null);

        assertEquals("simple_value", result.get("simpleValue")._1);
        assertTrue(result.get("simpleValue")._2);
    }

    @Test
    public void testGetProp2ColumnNameMap_NullNamingPolicyUsesSnakeCase() {
        ImmutableMap<String, String> result = QueryUtil.propertyToColumnMap(QueryUtilTest.AliaslessEntity.class, null);

        assertEquals("simple_value", result.get("simpleValue"));
    }

    @Test
    public void testGetProp2ColumnNameMap_AllNonColumnProperties() {
        ImmutableMap<String, String> result = QueryUtil.propertyToColumnMap(QueryUtilTest.NonColumnOnlyEntity.class, NamingPolicy.SNAKE_CASE);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetTableNameAndAlias_AliaslessAnnotation() {
        String tableName = QueryUtil.tableNameAndAlias(QueryUtilTest.AliaslessEntity.class, null);

        assertEquals("aliasless_table", tableName);
    }

    @Test
    public void testGetProp2ColumnNameMap_NullEntityClassReturnsEmpty() {
        // Documented contract: null entityClass returns an empty map (does not throw).
        ImmutableMap<String, String> result = QueryUtil.propertyToColumnMap(null, NamingPolicy.SNAKE_CASE);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetProp2ColumnNameMap_NullEntityClassAndNullPolicyReturnsEmpty() {
        // Both null: still returns empty map.
        ImmutableMap<String, String> result = QueryUtil.propertyToColumnMap(null, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetProp2ColumnNameMap_MapEntityClassReturnsEmpty() {
        // Map-assignable classes get an empty map (documented behavior).
        ImmutableMap<String, String> result = QueryUtil.propertyToColumnMap(HashMap.class, NamingPolicy.SNAKE_CASE);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIncludedSubEntityPropertiesExcludeNonColumns() {
        ImmutableList<String> names = QueryUtil.selectPropertyNames(ParentWithFilteredChild.class, true, null);

        assertTrue(names.contains("child.visible"), names.toString());
        assertFalse(names.contains("child.hiddenByAnnotation"), names.toString());
        assertFalse(names.contains("child.hiddenByTable"), names.toString());
    }

    @Test
    public void testExcludingSubEntityRootAlsoExcludesExpandedProperties() {
        Set<String> excluded = Collections.singleton("child");
        ImmutableList<String> names = QueryUtil.selectPropertyNames(ParentWithFilteredChild.class, true, excluded);

        assertEquals(Collections.singletonList("id"), names);

        String sql = Dsl.PSC.selectFrom(ParentWithFilteredChild.class, true, excluded).build().query();
        assertEquals("SELECT id AS \"id\" FROM parent_with_filtered_child", sql);
    }

    static class ParentWithFilteredChild {
        private long id;

        private FilteredChild child;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public FilteredChild getChild() {
            return child;
        }

        public void setChild(final FilteredChild child) {
            this.child = child;
        }
    }

    @Table(nonColumnFields = { "hiddenByTable" })
    static class FilteredChild {
        private String visible;

        @NonColumn
        private String hiddenByAnnotation;

        private String hiddenByTable;

        public String getVisible() {
            return visible;
        }

        public void setVisible(final String visible) {
            this.visible = visible;
        }

        public String getHiddenByAnnotation() {
            return hiddenByAnnotation;
        }

        public void setHiddenByAnnotation(final String hiddenByAnnotation) {
            this.hiddenByAnnotation = hiddenByAnnotation;
        }

        public String getHiddenByTable() {
            return hiddenByTable;
        }

        public void setHiddenByTable(final String hiddenByTable) {
            this.hiddenByTable = hiddenByTable;
        }
    }
}
