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

package com.landawn.abacus.query.condition;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.query.condition.ConditionFactory.CF;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * A utility class that provides a fluent API for creating conditions based on a property name.
 * This class caches instances to avoid creating duplicate objects for the same property name.
 * 
 * <p>NamedProperty simplifies the creation of various SQL conditions by providing convenient
 * methods that automatically include the property name.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a named property
 * NamedProperty age = NamedProperty.of("age");
 * 
 * // Use it to create various conditions
 * Condition c1 = age.eq(25);              // age = 25
 * Condition c2 = age.gt(18);              // age > 18
 * Condition c3 = age.between(20, 30);     // age BETWEEN 20 AND 30
 * Condition c4 = age.in(Arrays.asList(25, 30, 35)); // age IN (25, 30, 35)
 * 
 * // Chain conditions
 * Or orCondition = age.eqOr(25, 30, 35); // age = 25 OR age = 30 OR age = 35
 * }</pre>
 */
public final class NamedProperty {

    private static final Map<String, NamedProperty> instancePool = new ConcurrentHashMap<>();

    // for Kryo
    final String propName;

    /**
     * Constructs a NamedProperty with the specified property name.
     * This constructor is primarily used internally by the {@link #of(String)} factory method.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty age = new NamedProperty("age");
     * // However, prefer using: NamedProperty.of("age") for caching benefits
     * }</pre>
     *
     * @param propName the property name. Must not be null.
     */
    public NamedProperty(final String propName) {
        this.propName = N.requireNonNull(propName);
    }

    /**
     * Gets or creates a NamedProperty instance for the specified property name.
     * This method uses caching to ensure only one instance exists per property name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamedProperty username = NamedProperty.of("username");
     * NamedProperty status = NamedProperty.of("status");
     * }</pre>
     *
     * @param propName the property name
     * @return a cached or new NamedProperty instance
     * @throws IllegalArgumentException if propName is null or empty
     */
    public static NamedProperty of(final String propName) {
        if (Strings.isEmpty(propName)) {
            throw new IllegalArgumentException("the property name can't be null or empty string.");
        }

        return instancePool.computeIfAbsent(propName, NamedProperty::new);
    }

    /**
     * Returns the property name associated with this NamedProperty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamedProperty age = NamedProperty.of("age");
     * String name = age.propName(); // Returns "age"
     * }</pre>
     *
     * @return the property name
     */
    public String propName() {
        return propName;
    }

    /**
     * Creates an EQUAL condition for this property.
     * This is a convenience method that generates an equality comparison condition
     * using the property name already stored in this NamedProperty instance.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("status").eq("active"); // status = 'active'
     * NamedProperty.of("count").eq(5); // count = 5
     * }</pre>
     *
     * @param values the value to compare against. Can be of any type compatible with the property.
     * @return an Equal condition for this property
     * @see Equal
     * @see ConditionFactory.CF#eq(String, Object)
     */
    public Equal eq(final Object values) {
        return CF.eq(propName, values);
    }

    /**
     * Creates an OR condition with multiple EQUAL checks for this property.
     * This is a convenience method that generates multiple equality conditions combined with OR logic.
     * Each value in the array is compared for equality with the property, and the results are ORed together.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("color").eqOr("red", "green", "blue");
     * // Results in: color = 'red' OR color = 'green' OR color = 'blue'
     *
     * NamedProperty.of("priority").eqOr(1, 2, 3);
     * // Results in: priority = 1 OR priority = 2 OR priority = 3
     * }</pre>
     *
     * @param values array of values to check equality against. Each value will be tested with OR logic.
     * @return an Or condition containing multiple Equal conditions
     * @see Or
     * @see Equal
     */
    public Or eqOr(final Object... values) {
        final Or or = CF.or();

        for (final Object propValue : values) {
            or.add(CF.eq(propName, propValue));
        }

        return or;
    }

    /**
     * Creates an OR condition with multiple EQUAL checks for this property using a collection.
     * This is similar to {@link #eqOr(Object...)} but accepts a collection instead of varargs.
     * Useful when the values are already in a collection or list.
     *
     * <p>Example usage:
     * <pre>{@code
     * List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago");
     * NamedProperty.of("city").eqOr(cities);
     * // Results in: city = 'New York' OR city = 'Los Angeles' OR city = 'Chicago'
     *
     * Set<Integer> validIds = Set.of(10, 20, 30);
     * NamedProperty.of("department_id").eqOr(validIds);
     * // Results in: department_id = 10 OR department_id = 20 OR department_id = 30
     * }</pre>
     *
     * @param values collection of values to check equality against. Each value will be tested with OR logic.
     * @return an Or condition containing multiple Equal conditions
     * @see Or
     * @see Equal
     */
    public Or eqOr(final Collection<?> values) {
        final Or or = CF.or();

        for (final Object propValue : values) {
            or.add(CF.eq(propName, propValue));
        }

        return or;
    }

    /**
     * Creates a NOT EQUAL condition for this property.
     * This generates a condition that checks if the property value is not equal to the specified value.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("status").ne("deleted"); // status != 'deleted'
     * NamedProperty.of("count").ne(0); // count != 0
     * }</pre>
     *
     * @param values the value to compare against. Can be of any type compatible with the property.
     * @return a NotEqual condition for this property
     * @see NotEqual
     * @see ConditionFactory.CF#ne(String, Object)
     */
    public NotEqual ne(final Object values) {
        return CF.ne(propName, values);
    }

    /**
     * Creates a GREATER THAN condition for this property.
     * This generates a condition that checks if the property value is strictly greater than the specified value.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("age").gt(18); // age > 18
     * NamedProperty.of("price").gt(99.99); // price > 99.99
     * }</pre>
     *
     * @param value the value to compare against. Can be numeric, date, string, or any comparable type.
     * @return a GreaterThan condition for this property
     * @see GreaterThan
     * @see ConditionFactory.CF#gt(String, Object)
     */
    public GreaterThan gt(final Object value) {
        return CF.gt(propName, value);
    }

    /**
     * Creates a GREATER THAN OR EQUAL condition for this property.
     * This generates a condition that checks if the property value is greater than or equal to the specified value.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("score").ge(60); // score >= 60
     * NamedProperty.of("age").ge(21); // age >= 21
     * }</pre>
     *
     * @param value the value to compare against (inclusive). Can be numeric, date, string, or any comparable type.
     * @return a GreaterEqual condition for this property
     * @see GreaterEqual
     * @see ConditionFactory.CF#ge(String, Object)
     */
    public GreaterEqual ge(final Object value) {
        return CF.ge(propName, value);
    }

    /**
     * Creates a LESS THAN condition for this property.
     * This generates a condition that checks if the property value is strictly less than the specified value.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("price").lt(100); // price < 100
     * NamedProperty.of("age").lt(18); // age < 18
     * }</pre>
     *
     * @param value the value to compare against. Can be numeric, date, string, or any comparable type.
     * @return a LessThan condition for this property
     * @see LessThan
     * @see ConditionFactory.CF#lt(String, Object)
     */
    public LessThan lt(final Object value) {
        return CF.lt(propName, value);
    }

    /**
     * Creates a LESS THAN OR EQUAL condition for this property.
     * This generates a condition that checks if the property value is less than or equal to the specified value.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("quantity").le(10); // quantity <= 10
     * NamedProperty.of("age").le(65); // age <= 65
     * }</pre>
     *
     * @param value the value to compare against (inclusive). Can be numeric, date, string, or any comparable type.
     * @return a LessEqual condition for this property
     * @see LessEqual
     * @see ConditionFactory.CF#le(String, Object)
     */
    public LessEqual le(final Object value) {
        return CF.le(propName, value);
    }

    /**
     * Creates an IS NULL condition for this property.
     * This generates a condition that checks if the property value is NULL in the database.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("deletedDate").isNull(); // deletedDate IS NULL
     * NamedProperty.of("endDate").isNull(); // endDate IS NULL
     * }</pre>
     *
     * @return an IsNull condition for this property
     * @see IsNull
     * @see ConditionFactory.CF#isNull(String)
     */
    public IsNull isNull() {
        return CF.isNull(propName);
    }

    /**
     * Creates an IS NOT NULL condition for this property.
     * This generates a condition that checks if the property value is not NULL in the database.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("email").isNotNull(); // email IS NOT NULL
     * NamedProperty.of("phoneNumber").isNotNull(); // phoneNumber IS NOT NULL
     * }</pre>
     *
     * @return an IsNotNull condition for this property
     * @see IsNotNull
     * @see ConditionFactory.CF#isNotNull(String)
     */
    public IsNotNull isNotNull() {
        return CF.isNotNull(propName);
    }

    /**
     * Creates a BETWEEN condition for this property.
     * This generates a condition that checks if the property value falls within the specified range (inclusive).
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("age").between(18, 65); // age BETWEEN 18 AND 65
     * NamedProperty.of("price").between(10.0, 100.0); // price BETWEEN 10.0 AND 100.0
     * }</pre>
     *
     * @param minValue the minimum value (inclusive). Can be numeric, date, string, or any comparable type.
     * @param maxValue the maximum value (inclusive). Can be numeric, date, string, or any comparable type.
     * @return a Between condition for this property
     * @see Between
     * @see ConditionFactory.CF#between(String, Object, Object)
     */
    public Between between(final Object minValue, final Object maxValue) {
        return CF.between(propName, minValue, maxValue);
    }

    /**
     * Creates a BETWEEN condition for this property.
     *
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (inclusive)
     * @return a Between condition
     * @deprecated please use {@link #between(Object, Object)}
     */
    @Deprecated
    public Between bt(final Object minValue, final Object maxValue) {
        return CF.between(propName, minValue, maxValue);
    }

    /**
     * Creates a LIKE condition for this property.
     * This generates a pattern matching condition using SQL LIKE syntax with wildcards.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("name").like("John%"); // name LIKE 'John%'
     * NamedProperty.of("email").like("%@example.com"); // email LIKE '%@example.com'
     * }</pre>
     *
     * @param value the pattern to match (can include % for any characters and _ for single character)
     * @return a Like condition for this property
     * @see Like
     * @see ConditionFactory.CF#like(String, Object)
     */
    public Like like(final Object value) {
        return CF.like(propName, value);
    }

    /**
     * Creates a NOT LIKE condition for this property.
     * This generates a pattern matching condition that excludes values matching the SQL LIKE pattern.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("email").notLike("%@temp.com"); // email NOT LIKE '%@temp.com'
     * NamedProperty.of("name").notLike("test%"); // name NOT LIKE 'test%'
     * }</pre>
     *
     * @param value the pattern to exclude (can include % for any characters and _ for single character)
     * @return a NotLike condition for this property
     * @see NotLike
     * @see ConditionFactory.CF#notLike(String, Object)
     */
    public NotLike notLike(final Object value) {
        return CF.notLike(propName, value);
    }

    /**
     * Creates a LIKE condition that matches values starting with the specified prefix.
     * This is a convenience method that automatically appends the % wildcard to the value.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("name").startsWith("John"); // name LIKE 'John%'
     * NamedProperty.of("code").startsWith("PRD"); // code LIKE 'PRD%'
     * }</pre>
     *
     * @param value the prefix to match. The % wildcard will be automatically appended.
     * @return a Like condition with % appended to the value
     * @see Like
     * @see ConditionFactory.CF#startsWith(String, Object)
     */
    public Like startsWith(final Object value) {
        return CF.startsWith(propName, value);
    }

    /**
     * Creates a LIKE condition that matches values ending with the specified suffix.
     * This is a convenience method that automatically prepends the % wildcard to the value.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("email").endsWith("@example.com"); // email LIKE '%@example.com'
     * NamedProperty.of("filename").endsWith(".pdf"); // filename LIKE '%.pdf'
     * }</pre>
     *
     * @param value the suffix to match. The % wildcard will be automatically prepended.
     * @return a Like condition with % prepended to the value
     * @see Like
     * @see ConditionFactory.CF#endsWith(String, Object)
     */
    public Like endsWith(final Object value) {
        return CF.endsWith(propName, value);
    }

    /**
     * Creates a LIKE condition that matches values containing the specified substring.
     * This is a convenience method that automatically adds % wildcards to both sides of the value.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("description").contains("important"); // description LIKE '%important%'
     * NamedProperty.of("title").contains("query"); // title LIKE '%query%'
     * }</pre>
     *
     * @param value the substring to match. The % wildcard will be automatically added to both sides.
     * @return a Like condition with % on both sides of the value
     * @see Like
     * @see ConditionFactory.CF#contains(String, Object)
     */
    public Like contains(final Object value) {
        return CF.contains(propName, value);
    }

    /**
     * Creates an IN condition for this property with an array of values.
     * This generates a condition that checks if the property value matches any of the specified values.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty.of("status").in("active", "pending", "approved");
     * // Results in: status IN ('active', 'pending', 'approved')
     *
     * NamedProperty.of("priority").in(1, 2, 3);
     * // Results in: priority IN (1, 2, 3)
     * }</pre>
     *
     * @param values array of values to check membership against
     * @return an In condition for this property
     * @see In
     * @see ConditionFactory.CF#in(String, Object[])
     */
    public In in(final Object... values) {
        return CF.in(propName, values);
    }

    /**
     * Creates an IN condition for this property with a collection of values.
     * This is similar to {@link #in(Object...)} but accepts a collection instead of varargs.
     * Useful when the values are already in a collection or list.
     *
     * <p>Example usage:
     * <pre>{@code
     * Set<Integer> validIds = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
     * NamedProperty.of("id").in(validIds);
     * // Results in: id IN (1, 2, 3, 4, 5)
     *
     * List<String> departments = Arrays.asList("Sales", "Marketing", "IT");
     * NamedProperty.of("department").in(departments);
     * // Results in: department IN ('Sales', 'Marketing', 'IT')
     * }</pre>
     *
     * @param values collection of values to check membership against
     * @return an In condition for this property
     * @see In
     * @see ConditionFactory.CF#in(String, Collection)
     */
    public In in(final Collection<?> values) {
        return CF.in(propName, values);
    }

    /**
     * Generates the hash code for this NamedProperty based on the property name.
     * Two NamedProperty instances with the same property name will have the same hash code,
     * ensuring correct behavior in hash-based collections.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty p1 = NamedProperty.of("age");
     * NamedProperty p2 = NamedProperty.of("age");
     * assert p1.hashCode() == p2.hashCode(); // true due to caching
     * }</pre>
     *
     * @return hash code of the property name
     */
    @Override
    public int hashCode() {
        return propName.hashCode();
    }

    /**
     * Checks if this NamedProperty is equal to another object.
     * Two NamedProperty instances are equal if they have the same property name.
     * The comparison is case-sensitive and requires exact match.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty p1 = NamedProperty.of("age");
     * NamedProperty p2 = NamedProperty.of("age");
     * assert p1.equals(p2); // true
     *
     * NamedProperty p3 = NamedProperty.of("Age");
     * assert !p1.equals(p3); // false - case sensitive
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal (same property name), {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof NamedProperty && N.equals(((NamedProperty) obj).propName, propName));
    }

    /**
     * Returns the string representation of this NamedProperty.
     * The string representation is simply the property name itself.
     *
     * <p>Example usage:
     * <pre>{@code
     * NamedProperty age = NamedProperty.of("age");
     * String str = age.toString(); // Returns "age"
     * }</pre>
     *
     * @return the property name
     */
    @Override
    public String toString() {
        return propName;
    }
}