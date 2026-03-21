package com.landawn.abacus.query;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonColumn;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class QueryUtil2025Test extends TestBase {

    @Test
    public void testGetColumn2PropNameMap() {
        ImmutableMap<String, String> map = QueryUtil.getColumn2PropNameMap(Account.class);
        assertNotNull(map);
    }

    @Test
    public void testGetColumn2PropNameMap_NullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.getColumn2PropNameMap(null);
        });
    }

    @Test
    public void testGetProp2ColumnNameMap() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.SNAKE_CASE);
        assertNotNull(map);
    }

    @Test
    public void testGetProp2ColumnNameMap_NoChange() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.NO_CHANGE);
        assertNotNull(map);
    }

    @Test
    public void testGetProp2ColumnNameMap_UpperCase() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        assertNotNull(map);
    }

    @Test
    public void testGetProp2ColumnNameMap_NullClass() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(null, NamingPolicy.SNAKE_CASE);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetProp2ColumnNameMap_MapClass() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(HashMap.class, NamingPolicy.SNAKE_CASE);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetInsertPropNames_WithEntity() {
        Account account = new Account();
        Collection<String> props = QueryUtil.getInsertPropNames(account, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetInsertPropNames_WithExcludedProps() {
        Account account = new Account();
        Set<String> excluded = new HashSet<>();
        excluded.add("id");
        Collection<String> props = QueryUtil.getInsertPropNames(account, excluded);
        assertNotNull(props);
        assertFalse(props.contains("id"));
    }

    @Test
    public void testGetInsertPropNames_WithClass() {
        Collection<String> props = QueryUtil.getInsertPropNames(Account.class, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetInsertPropNames_WithClassNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.getInsertPropNames(null, Collections.emptySet());
        });
    }

    @Test
    public void testGetInsertPropNames_WithEntityNullCheck() {
        assertThrows(IllegalArgumentException.class, () -> QueryUtil.getInsertPropNames((Object) null, Collections.emptySet()));
    }

    @Test
    public void testGetInsertPropNames_WithClassAndExcluded() {
        Set<String> excluded = new HashSet<>();
        excluded.add("createdTime");
        Collection<String> props = QueryUtil.getInsertPropNames(Account.class, excluded);
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames() {
        Collection<String> props = QueryUtil.getSelectPropNames(Account.class, false, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames_IncludeSubEntity() {
        Collection<String> props = QueryUtil.getSelectPropNames(Account.class, true, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames_WithExcluded() {
        Set<String> excluded = new HashSet<>();
        excluded.add("id");
        Collection<String> props = QueryUtil.getSelectPropNames(Account.class, false, excluded);
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.getSelectPropNames(null, false, Collections.emptySet());
        });
    }

    @Test
    public void testGetUpdatePropNames() {
        Collection<String> props = QueryUtil.getUpdatePropNames(Account.class, Collections.emptySet());
        assertNotNull(props);
    }

    @Test
    public void testGetUpdatePropNames_WithExcluded() {
        Set<String> excluded = new HashSet<>();
        excluded.add("createdTime");
        Collection<String> props = QueryUtil.getUpdatePropNames(Account.class, excluded);
        assertNotNull(props);
    }

    @Test
    public void testGetUpdatePropNames_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.getUpdatePropNames(null, Collections.emptySet());
        });
    }

    @Test
    public void testGetIdFieldNames() {
        List<String> ids = QueryUtil.getIdPropNames(Account.class);
        assertNotNull(ids);
    }

    @Test
    public void testGetIdFieldNames_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.getIdPropNames(null);
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
        String alias = QueryUtil.getTableAlias(Account.class);
        assertEquals("acc", alias);
    }

    @Test
    public void testGetTableAlias_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.getTableAlias(null);
        });
    }

    @Test
    public void testGetTableNameAndAlias() {
        String tableName = QueryUtil.getTableNameAndAlias(Account.class);
        assertNotNull(tableName);
    }

    @Test
    public void testGetTableNameAndAlias_WithNamingPolicy() {
        String tableName = QueryUtil.getTableNameAndAlias(Account.class, NamingPolicy.SNAKE_CASE);
        assertNotNull(tableName);
    }

    @Test
    public void testGetTableNameAndAlias_UpperCase() {
        String tableName = QueryUtil.getTableNameAndAlias(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        assertNotNull(tableName);
    }

    @Test
    public void testGetTableNameAndAlias_NoChange() {
        String tableName = QueryUtil.getTableNameAndAlias(Account.class, NamingPolicy.NO_CHANGE);
        assertNotNull(tableName);
    }

    @Test
    public void testGetTableNameAndAlias_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.getTableNameAndAlias(null);
        });
    }

    @Test
    public void testGetTableNameAndAlias_NullCheckWithPolicy() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.getTableNameAndAlias(null, NamingPolicy.SNAKE_CASE);
        });
    }

    @Test
    public void testGetProp2ColumnNameMap_DifferentPolicies() {
        ImmutableMap<String, String> lower = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.SNAKE_CASE);
        ImmutableMap<String, String> upper = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        ImmutableMap<String, String> noChange = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.NO_CHANGE);

        assertNotNull(lower);
        assertNotNull(upper);
        assertNotNull(noChange);
    }

    @Test
    public void testGetInsertPropNames_EmptyExcluded() {
        Collection<String> props = QueryUtil.getInsertPropNames(Account.class, new HashSet<>());
        assertNotNull(props);
    }

    @Test
    public void testGetSelectPropNames_EmptyExcluded() {
        Collection<String> props = QueryUtil.getSelectPropNames(Account.class, false, new HashSet<>());
        assertNotNull(props);
    }

    @Test
    public void testGetUpdatePropNames_EmptyExcluded() {
        Collection<String> props = QueryUtil.getUpdatePropNames(Account.class, new HashSet<>());
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
}

public class QueryUtilTest extends TestBase {

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

    @Test
    public void testProp2ColumnNameMap() {
        // Test with valid entity class
        ImmutableMap<String, Tuple2<String, Boolean>> result = QueryUtil.prop2ColumnNameMap(TestUser.class, NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertTrue(result.containsKey("id"));
        assertTrue(result.containsKey("name"));
        assertFalse(result.containsKey("email"));

        // Check column name mapping
        assertEquals("id", result.get("id")._1);
        assertEquals("user_name", result.get("name")._1);

        // Check simple property flags
        assertTrue(result.get("id")._2);
        assertTrue(result.get("name")._2);

        // Check that notColumnField is not included
        assertFalse(result.containsKey("notColumnField"));

        // Test caching - should return same instance
        ImmutableMap<String, Tuple2<String, Boolean>> result2 = QueryUtil.prop2ColumnNameMap(TestUser.class, NamingPolicy.SNAKE_CASE);
        assertSame(result, result2);

        // Test with different naming policy
        ImmutableMap<String, Tuple2<String, Boolean>> result3 = QueryUtil.prop2ColumnNameMap(TestUser.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        assertNotSame(result, result3);
    }

    @Test
    public void testGetColumn2PropNameMap() {
        ImmutableMap<String, String> result = QueryUtil.getColumn2PropNameMap(TestUser.class);

        assertNotNull(result);
        assertTrue(result.containsKey("user_name"));
        assertTrue(result.containsKey("USER_NAME"));
        assertTrue(result.containsKey("user_name"));

        assertEquals("name", result.get("user_name"));
        assertEquals("name", result.get("USER_NAME"));
        assertEquals("name", result.get("user_name"));

        // Test caching
        ImmutableMap<String, String> result2 = QueryUtil.getColumn2PropNameMap(TestUser.class);
        assertSame(result, result2);
    }

    @Test
    public void testGetProp2ColumnNameMap() {
        // Test with null entity class
        ImmutableMap<String, String> nullResult = QueryUtil.getProp2ColumnNameMap(null, NamingPolicy.SNAKE_CASE);
        assertTrue(nullResult.isEmpty());

        // Test with Map class
        ImmutableMap<String, String> mapResult = QueryUtil.getProp2ColumnNameMap(Map.class, NamingPolicy.SNAKE_CASE);
        assertTrue(mapResult.isEmpty());

        // Test with regular entity class
        ImmutableMap<String, String> result = QueryUtil.getProp2ColumnNameMap(TestUser.class, NamingPolicy.SNAKE_CASE);
        assertNotNull(result);
        assertEquals("id", result.get("id"));
        assertEquals("user_name", result.get("name"));

        // Test that excluded fields are not included
        assertFalse(result.containsKey("notColumnField"));

        // Test caching
        ImmutableMap<String, String> result2 = QueryUtil.getProp2ColumnNameMap(TestUser.class, NamingPolicy.SNAKE_CASE);
        assertSame(result, result2);
    }

    @Test
    public void testGetProp2ColumnNameMapWithRepeatedNestedType() {
        final ImmutableMap<String, String> result = QueryUtil.getProp2ColumnNameMap(NestedRoot.class, NamingPolicy.SNAKE_CASE);

        assertTrue(result.containsKey("branch.firstLeaf.value"));
        assertTrue(result.containsKey("branch.secondLeaf.value"));
    }

    @Test
    public void testGetInsertPropNamesWithEntity() {
        TestUser user = new TestUser();

        // Test with default ID value (should exclude ID)
        user.setId(null);
        Set<String> excludedProps = new HashSet<>();
        Collection<String> props = QueryUtil.getInsertPropNames(user, excludedProps);
        assertNotNull(props);
        assertFalse(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));

        // Test with non-default ID value (should include ID)
        user.setId(123L);
        props = QueryUtil.getInsertPropNames(user, excludedProps);
        assertTrue(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));

        // Test with excluded properties
        excludedProps.add("email");
        props = QueryUtil.getInsertPropNames(user, excludedProps);
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));
    }

    @Test
    public void testGetInsertPropNamesWithClass() {
        Set<String> excludedProps = new HashSet<>();

        // Test without exclusions
        Collection<String> props = QueryUtil.getInsertPropNames(TestUser.class, excludedProps);
        assertNotNull(props);
        assertTrue(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));

        // Test with exclusions
        excludedProps.add("email");
        props = QueryUtil.getInsertPropNames(TestUser.class, excludedProps);
        assertTrue(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));
    }

    @Test
    public void testGetSelectPropNames() {
        Set<String> excludedProps = new HashSet<>();

        // Test without sub-entity properties
        Collection<String> props = QueryUtil.getSelectPropNames(TestUser.class, false, excludedProps);
        assertNotNull(props);
        assertTrue(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));

        // Test with sub-entity properties
        props = QueryUtil.getSelectPropNames(TestUser.class, true, excludedProps);
        assertNotNull(props);
        // Should include nested properties if entity has sub-entities

        // Test with exclusions
        excludedProps.add("email");
        props = QueryUtil.getSelectPropNames(TestUser.class, false, excludedProps);
        assertTrue(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));
    }

    @Test
    public void testGetUpdatePropNames() {
        Set<String> excludedProps = new HashSet<>();

        // Test without exclusions
        Collection<String> props = QueryUtil.getUpdatePropNames(TestUser.class, excludedProps);
        assertNotNull(props);
        assertTrue(props.contains("id")); // ID should be excluded from update
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));

        // Test with exclusions
        excludedProps.add("email");
        props = QueryUtil.getUpdatePropNames(TestUser.class, excludedProps);
        assertTrue(props.contains("id"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("email"));
    }

    @Test
    public void testGetIdFieldNames() {
        // Test entity with ID
        List<String> idFields = QueryUtil.getIdPropNames(TestUser.class);
        assertNotNull(idFields);
        assertEquals(1, idFields.size());
        assertEquals("id", idFields.get(0));

        // Test entity without ID
        idFields = QueryUtil.getIdPropNames(NoIdEntity.class);
        assertNotNull(idFields);
        assertTrue(idFields.isEmpty());
    }

    @Test
    public void testIsNonColumn() {
        PropInfo propInfo = ParserUtil.getBeanInfo(TestUser.class).getPropInfo("nonColumnField");
        Set<String> columnFields = new HashSet<>(Arrays.asList("id", "name"));
        Set<String> nonColumnFields = new HashSet<>(Arrays.asList("tempData"));

        // Test with @NonColumn annotation
        assertTrue(QueryUtil.isNonColumn(Collections.emptySet(), Collections.emptySet(), propInfo));

        // Test with columnFields restriction
        PropInfo emailProp = ParserUtil.getBeanInfo(TestUser.class).getPropInfo("email");
        assertTrue(QueryUtil.isNonColumn(columnFields, Collections.emptySet(), emailProp));

        // Test with nonColumnFields
        PropInfo tempDataProp = ParserUtil.getBeanInfo(TestUser.class).getPropInfo("tempData");
        assertTrue(QueryUtil.isNonColumn(Collections.emptySet(), nonColumnFields, tempDataProp));

        // Test normal column
        PropInfo nameProp = ParserUtil.getBeanInfo(TestUser.class).getPropInfo("name");
        assertFalse(QueryUtil.isNonColumn(columnFields, Collections.emptySet(), nameProp));
    }

    @Test
    public void testRepeatQuestionMark() {
        // Test zero count
        assertEquals("", QueryUtil.placeholders(0));

        // Test small counts (cached)
        assertEquals("?", QueryUtil.placeholders(1));
        assertEquals("?, ?", QueryUtil.placeholders(2));
        assertEquals("?, ?, ?", QueryUtil.placeholders(3));
        assertEquals("?, ?, ?, ?, ?", QueryUtil.placeholders(5));

        // Test larger cached values
        assertEquals(Strings.repeat("?", 30, ", "), QueryUtil.placeholders(30));
        assertEquals(Strings.repeat("?", 100, ", "), QueryUtil.placeholders(100));
        assertEquals(Strings.repeat("?", 200, ", "), QueryUtil.placeholders(200));
        assertEquals(Strings.repeat("?", 300, ", "), QueryUtil.placeholders(300));
        assertEquals(Strings.repeat("?", 500, ", "), QueryUtil.placeholders(500));
        assertEquals(Strings.repeat("?", 1000, ", "), QueryUtil.placeholders(1000));

        // Test non-cached value
        assertEquals(Strings.repeat("?", 37, ", "), QueryUtil.placeholders(37));

        // Test negative value should throw exception
        assertThrows(IllegalArgumentException.class, () -> QueryUtil.placeholders(-1));
    }

    @Test
    public void testGetTableAlias() {
        // Test with @Table annotation that has alias
        String alias = QueryUtil.getTableAlias(TestUser.class);
        assertEquals("tu", alias);

        // Test with class without @Table annotation
        alias = QueryUtil.getTableAlias(SimpleEntity.class);
        assertNull(alias);
    }

    @Test
    public void testGetTableNameAndAlias() {
        // Test with default naming policy
        String tableInfo = QueryUtil.getTableNameAndAlias(TestUser.class);
        assertEquals("test_user tu", tableInfo);

        // Test with specific naming policy
        tableInfo = QueryUtil.getTableNameAndAlias(TestUser.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        assertEquals("test_user tu", tableInfo);

        // Test with class without @Table annotation
        tableInfo = QueryUtil.getTableNameAndAlias(SimpleEntity.class);
        assertEquals("simple_entity", tableInfo);

        // Test with different naming policy
        tableInfo = QueryUtil.getTableNameAndAlias(SimpleEntity.class, NamingPolicy.SCREAMING_SNAKE_CASE);
        assertEquals("SIMPLE_ENTITY", tableInfo);
    }

    @Test
    public void testRegisterEntityPropColumnNameMap() {
        // Test with cycling references detection
        Set<Class<?>> registeringClasses = new HashSet<>();
        registeringClasses.add(TestUser.class);

        // Should throw exception when trying to register same class again
        assertThrows(RuntimeException.class, () -> {
            QueryUtil.registerEntityPropColumnNameMap(TestUser.class, NamingPolicy.SNAKE_CASE, registeringClasses);
        });
    }
}

class QueryUtilJavadocExamples extends TestBase {

    @Test
    public void testQueryUtil_patternForAlphanumericColumnName() {
        boolean isValid = QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("column_name").matches();
        assertTrue(isValid);
        boolean isInvalid = QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("column name").matches();
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
}

class QueryUtilFromFilters2025Test extends TestBase {

    @Test
    public void testPatternForAlphanumericColumnName() {
        assertNotNull(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME);
        assertTrue(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user_name").matches());
        assertTrue(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user123").matches());
        assertTrue(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user-name").matches());
        assertFalse(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user.name").matches());
        assertFalse(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user name").matches());
    }
}

class QueryUtil2026Batch2Test extends TestBase {

    @Test
    public void testProp2ColumnNameMap_NullNamingPolicyUsesSnakeCase() {
        ImmutableMap<String, Tuple2<String, Boolean>> result = QueryUtil.prop2ColumnNameMap(QueryUtilTest.AliaslessEntity.class, null);

        assertEquals("simple_value", result.get("simpleValue")._1);
        assertTrue(result.get("simpleValue")._2);
    }

    @Test
    public void testGetProp2ColumnNameMap_NullNamingPolicyUsesSnakeCase() {
        ImmutableMap<String, String> result = QueryUtil.getProp2ColumnNameMap(QueryUtilTest.AliaslessEntity.class, null);

        assertEquals("simple_value", result.get("simpleValue"));
    }

    @Test
    public void testGetProp2ColumnNameMap_AllNonColumnProperties() {
        ImmutableMap<String, String> result = QueryUtil.getProp2ColumnNameMap(QueryUtilTest.NonColumnOnlyEntity.class, NamingPolicy.SNAKE_CASE);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetTableNameAndAlias_AliaslessAnnotation() {
        String tableName = QueryUtil.getTableNameAndAlias(QueryUtilTest.AliaslessEntity.class, null);

        assertEquals("aliasless_table", tableName);
    }
}
