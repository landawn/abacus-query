package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
class SortDirection2025Test extends TestBase {

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
    public void testCompareTo() {
        // Enum compareTo is based on ordinal
        assertTrue(SortDirection.ASC.compareTo(SortDirection.DESC) < 0);
        assertTrue(SortDirection.DESC.compareTo(SortDirection.ASC) > 0);
        assertEquals(0, SortDirection.ASC.compareTo(SortDirection.ASC));
    }

    @Test
    public void testEquals() {
        assertEquals(SortDirection.ASC, SortDirection.ASC);
        assertEquals(SortDirection.DESC, SortDirection.DESC);
        assertFalse(SortDirection.ASC.equals(SortDirection.DESC));
    }

    @Test
    public void testHashCode() {
        assertNotNull(SortDirection.ASC.hashCode());
        assertNotNull(SortDirection.DESC.hashCode());
    }

    @Test
    public void testGetDeclaringClass() {
        assertEquals(SortDirection.class, SortDirection.ASC.getDeclaringClass());
        assertEquals(SortDirection.class, SortDirection.DESC.getDeclaringClass());
    }
}

public class SortDirectionTest extends TestBase {

    @Test
    public void testIsAscending() {
        // Test ASC
        SortDirection ascDirection = SortDirection.ASC;
        assertTrue(ascDirection.isAscending());

        // Test DESC
        SortDirection descDirection = SortDirection.DESC;
        assertFalse(descDirection.isAscending());
    }

    @Test
    public void testEnumValues() {
        // Test that we have exactly 2 values
        SortDirection[] values = SortDirection.values();
        assertEquals(2, values.length);

        // Test that ASC and DESC are present
        boolean hasAsc = false;
        boolean hasDesc = false;
        for (SortDirection dir : values) {
            if (dir == SortDirection.ASC) {
                hasAsc = true;
            } else if (dir == SortDirection.DESC) {
                hasDesc = true;
            }
        }
        assertTrue(hasAsc);
        assertTrue(hasDesc);
    }

    @Test
    public void testValueOf() {
        // Test valueOf for valid values
        assertEquals(SortDirection.ASC, SortDirection.valueOf("ASC"));
        assertEquals(SortDirection.DESC, SortDirection.valueOf("DESC"));

        // Test valueOf for invalid value
        assertThrows(IllegalArgumentException.class, () -> SortDirection.valueOf("INVALID"));
    }

    @Test
    public void testToString() {
        // Test toString returns the enum name
        assertEquals("ASC", SortDirection.ASC.toString());
        assertEquals("DESC", SortDirection.DESC.toString());
    }

    @Test
    public void testEnumComparison() {
        // Test that enum instances are singletons
        SortDirection asc1 = SortDirection.ASC;
        SortDirection asc2 = SortDirection.ASC;
        assertSame(asc1, asc2);

        SortDirection desc1 = SortDirection.DESC;
        SortDirection desc2 = SortDirection.DESC;
        assertSame(desc1, desc2);

        // Test that ASC and DESC are different
        assertNotSame(SortDirection.ASC, SortDirection.DESC);
    }

    @Test
    public void testOrdinal() {
        // Test ordinal values
        assertEquals(0, SortDirection.ASC.ordinal());
        assertEquals(1, SortDirection.DESC.ordinal());
    }

    @Test
    public void testName() {
        // Test name() method
        assertEquals("ASC", SortDirection.ASC.name());
        assertEquals("DESC", SortDirection.DESC.name());
    }
}

class SortDirectionJavadocExamples extends TestBase {

    @Test
    public void testSortDirection_classLevelExample() {
        SortDirection direction = SortDirection.ASC;
        assertTrue(direction.isAscending());

        String sql = "SELECT * FROM users ORDER BY name " + SortDirection.DESC;
        assertTrue(sql.contains("DESC"));
    }

    @Test
    public void testSortDirection_isAscending() {
        SortDirection direction = SortDirection.ASC;
        boolean ascending = direction.isAscending();
        assertTrue(ascending);

        SortDirection descDirection = SortDirection.DESC;
        boolean descAscending = descDirection.isAscending();
        assertFalse(descAscending);
    }
}
