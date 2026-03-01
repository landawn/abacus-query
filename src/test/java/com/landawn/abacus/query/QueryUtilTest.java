package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonColumn;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple.Tuple2;

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

        // Test with fakeIdForEmpty = true
        idFields = QueryUtil.getIdPropNames(NoIdEntity.class, true);
        assertNotNull(idFields);
        assertEquals(1, idFields.size());
        assertTrue(idFields.get(0).startsWith("not_defined_fake_id_in_abacus_"));

        // Test with fakeIdForEmpty = false
        idFields = QueryUtil.getIdPropNames(NoIdEntity.class, false);
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
    public void testIsFakeId() {
        // Test with regular ID list
        List<String> regularIds = Arrays.asList("id");
        assertFalse(QueryUtil.isFakeId(regularIds));

        // Test with fake ID from getIdPropNames
        List<String> fakeIds = QueryUtil.getIdPropNames(NoIdEntity.class, true);
        assertTrue(QueryUtil.isFakeId(fakeIds));

        // Test with null
        assertFalse(QueryUtil.isFakeId(null));

        // Test with empty list
        assertFalse(QueryUtil.isFakeId(Collections.emptyList()));

        // Test with multiple IDs
        assertFalse(QueryUtil.isFakeId(Arrays.asList("id1", "id2")));
    }

    @Test
    public void testRepeatQuestionMark() {
        // Test zero count
        assertEquals("", QueryUtil.repeatQuestionMark(0));

        // Test small counts (cached)
        assertEquals("?", QueryUtil.repeatQuestionMark(1));
        assertEquals("?, ?", QueryUtil.repeatQuestionMark(2));
        assertEquals("?, ?, ?", QueryUtil.repeatQuestionMark(3));
        assertEquals("?, ?, ?, ?, ?", QueryUtil.repeatQuestionMark(5));

        // Test larger cached values
        assertEquals(Strings.repeat("?", 30, ", "), QueryUtil.repeatQuestionMark(30));
        assertEquals(Strings.repeat("?", 100, ", "), QueryUtil.repeatQuestionMark(100));
        assertEquals(Strings.repeat("?", 200, ", "), QueryUtil.repeatQuestionMark(200));
        assertEquals(Strings.repeat("?", 300, ", "), QueryUtil.repeatQuestionMark(300));
        assertEquals(Strings.repeat("?", 500, ", "), QueryUtil.repeatQuestionMark(500));
        assertEquals(Strings.repeat("?", 1000, ", "), QueryUtil.repeatQuestionMark(1000));

        // Test non-cached value
        assertEquals(Strings.repeat("?", 37, ", "), QueryUtil.repeatQuestionMark(37));

        // Test negative value should throw exception
        assertThrows(IllegalArgumentException.class, () -> QueryUtil.repeatQuestionMark(-1));
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
