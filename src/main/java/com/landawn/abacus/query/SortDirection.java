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
 * Provides two possible sort orders: ascending (ASC) and descending (DESC).
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * SortDirection direction = SortDirection.ASC;
 * if (direction.isAscending()) {
 *     // Sort in ascending order
 * }
 * 
 * // Use in SQL building
 * String sql = "SELECT * FROM users ORDER BY name " + SortDirection.DESC;
 * }</pre>
 * 
 * @see SQLBuilder
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
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized SortDirection instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    SortDirection() {
    }

    /**
     * Checks if this sort direction is ascending.
     * This is a convenience method equivalent to checking if the direction equals ASC.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * SortDirection dir = SortDirection.ASC;
     * if (dir.isAscending()) {
     *     System.out.println("Sorting in ascending order");
     * }
     * }</pre>
     *
     * @return {@code true} if this sort direction is ASC, {@code false} if it is DESC
     */
    public boolean isAscending() {
        return this == SortDirection.ASC;
    }
}