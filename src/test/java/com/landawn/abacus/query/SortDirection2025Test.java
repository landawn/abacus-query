/*
 * Copyright (C) 2025 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class SortDirection2025Test extends TestBase {

    @Test
    public void testIsAscending_ASC() {
        assertTrue(SortDirection.ASC.isAscending());
    }

    @Test
    public void testIsAscending_DESC() {
        assertFalse(SortDirection.DESC.isAscending());
    }

    @Test
    public void testValues() {
        SortDirection[] values = SortDirection.values();
        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals(SortDirection.ASC, values[0]);
        assertEquals(SortDirection.DESC, values[1]);
    }

    @Test
    public void testValueOf() {
        assertEquals(SortDirection.ASC, SortDirection.valueOf("ASC"));
        assertEquals(SortDirection.DESC, SortDirection.valueOf("DESC"));
    }

    @Test
    public void testToString() {
        assertEquals("ASC", SortDirection.ASC.toString());
        assertEquals("DESC", SortDirection.DESC.toString());
    }

    @Test
    public void testName() {
        assertEquals("ASC", SortDirection.ASC.name());
        assertEquals("DESC", SortDirection.DESC.name());
    }

    @Test
    public void testOrdinal() {
        assertEquals(0, SortDirection.ASC.ordinal());
        assertEquals(1, SortDirection.DESC.ordinal());
    }
}
