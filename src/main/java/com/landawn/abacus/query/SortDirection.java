/*
 * Copyright (C) 2015 HaiYang Li
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

/**
 * Enumeration representing the sort direction for database queries and collections.
 * Provides two possible sort orders: ascending ({@link #ASC}) and descending ({@link #DESC}).
 *
 * <p>This enum is commonly used with the {@link SqlBuilder} family to specify the order of results in
 * SQL queries. The enum constant names ({@code "ASC"}, {@code "DESC"}) correspond to the standard SQL
 * {@code ORDER BY} clause keywords, so {@link #toString()} (the default {@link Enum#toString()}, which
 * is not overridden here) returns the matching SQL fragment.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check sort direction
 * SortDirection direction = SortDirection.ASC;
 * if (direction.isAscending()) {
 *     System.out.println("Sorting in ascending order");
 * }
 *
 * // Use in SQL building (toString() yields "ASC" / "DESC")
 * String sql = "SELECT * FROM users ORDER BY name " + SortDirection.DESC;
 *
 * // Use with a SqlBuilder (typical use case). orderBy(...) may be called only once,
 * // so pass multiple columns in a single call.
 * String built = Dsl.PSC.selectFrom(User.class)
 *     .orderBy(Arrays.asList("lastName", "firstName"), SortDirection.ASC)
 *     .build().query();
 * // built ends with: ... ORDER BY last_name ASC, first_name ASC
 * }</pre>
 *
 * @see SqlBuilder
 */
public enum SortDirection {

    /**
     * Ascending sort order (A to Z, 0 to 9, oldest to newest).
     */
    ASC,

    /**
     * Descending sort order (Z to A, 9 to 0, newest to oldest).
     */
    DESC;

    /**
     * Checks if this sort direction is ascending.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SortDirection dir = SortDirection.ASC;
     * boolean ascending = dir.isAscending();   // true
     * }</pre>
     *
     * @return {@code true} if this is ASC, {@code false} if DESC
     */
    public boolean isAscending() {
        return this == SortDirection.ASC;
    }

    /**
     * Checks if this sort direction is descending.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SortDirection dir = SortDirection.DESC;
     * boolean descending = dir.isDescending();   // true
     * }</pre>
     *
     * @return {@code true} if this is DESC, {@code false} if ASC
     */
    public boolean isDescending() {
        return this == SortDirection.DESC;
    }

    /**
     * Returns the opposite sort direction.
     * {@link #ASC} maps to {@link #DESC} and {@link #DESC} maps to {@link #ASC}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SortDirection reversed = SortDirection.ASC.opposite();    // DESC
     * SortDirection original = reversed.opposite();             // ASC
     * }</pre>
     *
     * @return {@link #DESC} if this is {@link #ASC}, otherwise {@link #ASC}
     */
    public SortDirection opposite() {
        return this == SortDirection.ASC ? SortDirection.DESC : SortDirection.ASC;
    }

    /**
     * Returns the {@code SortDirection} corresponding to the given name.
     * The lookup is case-insensitive, so {@code "asc"}, {@code "ASC"}, {@code "desc"} and
     * {@code "DESC"} are all recognized. Surrounding whitespace is not trimmed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SortDirection asc = SortDirection.of("asc");     // ASC
     * SortDirection desc = SortDirection.of("DESC");   // DESC
     * SortDirection none = SortDirection.of("up");     // null (not recognized)
     * SortDirection nil = SortDirection.of(null);      // null
     * }</pre>
     *
     * @param name the sort direction name to look up (case-insensitive); may be {@code null}
     * @return the matching {@code SortDirection}, or {@code null} if {@code name} is {@code null}
     *         or does not name a recognized direction
     */
    public static SortDirection of(final String name) {
        if ("ASC".equalsIgnoreCase(name)) {
            return SortDirection.ASC;
        } else if ("DESC".equalsIgnoreCase(name)) {
            return SortDirection.DESC;
        }

        return null;
    }
}
