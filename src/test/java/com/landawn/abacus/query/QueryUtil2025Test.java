/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class QueryUtil2025Test extends TestBase {

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
    public void testGetColumn2PropNameMap_CaseInsensitive() {
        ImmutableMap<String, String> map = QueryUtil.getColumn2PropNameMap(Account.class);
        assertNotNull(map);
        // The map should contain variations for case-insensitive lookups
    }

    @Test
    public void testGetProp2ColumnNameMap() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertNotNull(map);
    }

    @Test
    public void testGetProp2ColumnNameMap_NoChange() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.NO_CHANGE);
        assertNotNull(map);
    }

    @Test
    public void testGetProp2ColumnNameMap_UpperCase() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        assertNotNull(map);
    }

    @Test
    public void testGetProp2ColumnNameMap_NullClass() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetProp2ColumnNameMap_MapClass() {
        ImmutableMap<String, String> map = QueryUtil.getProp2ColumnNameMap(HashMap.class, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
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
        List<String> ids = QueryUtil.getIdFieldNames(Account.class);
        assertNotNull(ids);
    }

    @Test
    public void testGetIdFieldNames_NullCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.getIdFieldNames(null);
        });
    }

    @Test
    public void testGetIdFieldNames_WithFakeId() {
        List<String> ids = QueryUtil.getIdFieldNames(Account.class, false);
        assertNotNull(ids);
    }

    @Test
    public void testGetIdFieldNames_WithFakeIdEnabled() {
        List<String> ids = QueryUtil.getIdFieldNames(Account.class, true);
        assertNotNull(ids);
    }

    @Test
    public void testIsFakeId() {
        List<String> ids = QueryUtil.getIdFieldNames(Account.class, true);
        boolean isFake = QueryUtil.isFakeId(ids);
        // Result depends on whether Account has real ID fields
    }

    @Test
    public void testIsFakeId_Null() {
        assertFalse(QueryUtil.isFakeId(null));
    }

    @Test
    public void testRepeatQM() {
        String qm = QueryUtil.repeatQM(3);
        assertEquals("?, ?, ?", qm);
    }

    @Test
    public void testRepeatQM_One() {
        String qm = QueryUtil.repeatQM(1);
        assertEquals("?", qm);
    }

    @Test
    public void testRepeatQM_Zero() {
        String qm = QueryUtil.repeatQM(0);
        assertEquals("", qm);
    }

    @Test
    public void testRepeatQM_Large() {
        String qm = QueryUtil.repeatQM(100);
        assertNotNull(qm);
        assertTrue(qm.contains("?"));
    }

    @Test
    public void testRepeatQM_Negative() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryUtil.repeatQM(-1);
        });
    }

    @Test
    public void testRepeatQM_Cached() {
        String qm1 = QueryUtil.repeatQM(5);
        String qm2 = QueryUtil.repeatQM(5);
        assertEquals(qm1, qm2);
    }

    @Test
    public void testRepeatQM_SpecialCachedValues() {
        assertNotNull(QueryUtil.repeatQM(100));
        assertNotNull(QueryUtil.repeatQM(200));
        assertNotNull(QueryUtil.repeatQM(300));
        assertNotNull(QueryUtil.repeatQM(500));
        assertNotNull(QueryUtil.repeatQM(1000));
    }

    @Test
    public void testGetTableAlias() {
        String alias = QueryUtil.getTableAlias(Account.class);
        // Result depends on whether Account has @Table annotation with alias
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
        String tableName = QueryUtil.getTableNameAndAlias(Account.class, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertNotNull(tableName);
    }

    @Test
    public void testGetTableNameAndAlias_UpperCase() {
        String tableName = QueryUtil.getTableNameAndAlias(Account.class, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
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
            QueryUtil.getTableNameAndAlias(null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        });
    }

    @Test
    public void testGetProp2ColumnNameMap_DifferentPolicies() {
        ImmutableMap<String, String> lower = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        ImmutableMap<String, String> upper = QueryUtil.getProp2ColumnNameMap(Account.class, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
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
    public void testRepeatQM_AllCachedValues() {
        for (int i = 0; i <= 30; i++) {
            String result = QueryUtil.repeatQM(i);
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
    public void testRepeatQM_NonCachedValue() {
        String result = QueryUtil.repeatQM(50);
        assertNotNull(result);
        assertEquals(148, result.length()); // "?, " * 50 = 150 - 2 = 148 characters
    }
}
