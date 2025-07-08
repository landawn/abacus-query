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

package com.landawn.abacus.condition;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * A utility class that provides a fluent API for creating conditions based on a property name.
 * This class caches instances to avoid creating duplicate objects for the same property name.
 * 
 * <p>NamedProperty simplifies the creation of various SQL conditions by providing convenient
 * methods that automatically include the property name.</p>
 * 
 * <p>Example usage:</p>
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
     *
     * @param propName the property name
     * @throws NullPointerException if propName is null
     */
    public NamedProperty(final String propName) {
        this.propName = N.requireNonNull(propName);
    }

    /**
     * Gets or creates a NamedProperty instance for the specified property name.
     * This method uses caching to ensure only one instance exists per property name.
     *
     * @param propName the property name
     * @return a cached or new NamedProperty instance
     * @throws IllegalArgumentException if propName is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty username = NamedProperty.of("username");
     * NamedProperty status = NamedProperty.of("status");
     * }</pre>
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
     * @return the property name
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty age = NamedProperty.of("age");
     * String name = age.propName(); // Returns "age"
     * }</pre>
     */
    public String propName() {
        return propName;
    }

    /**
     * Creates an EQUAL condition for this property.
     *
     * @param values the value to compare against
     * @return an Equal condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("status").eq("active"); // status = 'active'
     * }</pre>
     */
    public Equal eq(final Object values) {
        return CF.eq(propName, values);
    }

    /**
     * Creates an OR condition with multiple EQUAL checks for this property.
     *
     * @param values array of values to check equality against
     * @return an Or condition containing multiple Equal conditions
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("color").eqOr("red", "green", "blue");
     * // Results in: color = 'red' OR color = 'green' OR color = 'blue'
     * }</pre>
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
     *
     * @param values collection of values to check equality against
     * @return an Or condition containing multiple Equal conditions
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago");
     * NamedProperty.of("city").eqOr(cities);
     * // Results in: city = 'New York' OR city = 'Los Angeles' OR city = 'Chicago'
     * }</pre>
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
     *
     * @param values the value to compare against
     * @return a NotEqual condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("status").ne("deleted"); // status != 'deleted'
     * }</pre>
     */
    public NotEqual ne(final Object values) {
        return CF.ne(propName, values);
    }

    /**
     * Creates a GREATER THAN condition for this property.
     *
     * @param value the value to compare against
     * @return a GreaterThan condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("age").gt(18); // age > 18
     * }</pre>
     */
    public GreaterThan gt(final Object value) {
        return CF.gt(propName, value);
    }

    /**
     * Creates a GREATER THAN OR EQUAL condition for this property.
     *
     * @param value the value to compare against
     * @return a GreaterEqual condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("score").ge(60); // score >= 60
     * }</pre>
     */
    public GreaterEqual ge(final Object value) {
        return CF.ge(propName, value);
    }

    /**
     * Creates a LESS THAN condition for this property.
     *
     * @param value the value to compare against
     * @return a LessThan condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("price").lt(100); // price < 100
     * }</pre>
     */
    public LessThan lt(final Object value) {
        return CF.lt(propName, value);
    }

    /**
     * Creates a LESS THAN OR EQUAL condition for this property.
     *
     * @param value the value to compare against
     * @return a LessEqual condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("quantity").le(10); // quantity <= 10
     * }</pre>
     */
    public LessEqual le(final Object value) {
        return CF.le(propName, value);
    }

    /**
     * Creates an IS NULL condition for this property.
     *
     * @return an IsNull condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("deletedDate").isNull(); // deletedDate IS NULL
     * }</pre>
     */
    public IsNull isNull() {
        return CF.isNull(propName);
    }

    /**
     * Creates an IS NOT NULL condition for this property.
     *
     * @return an IsNotNull condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("email").isNotNull(); // email IS NOT NULL
     * }</pre>
     */
    public IsNotNull isNotNull() {
        return CF.isNotNull(propName);
    }

    /**
     * Creates a BETWEEN condition for this property.
     *
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (inclusive)
     * @return a Between condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("age").between(18, 65); // age BETWEEN 18 AND 65
     * }</pre>
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
     *
     * @param value the pattern to match (can include % and _ wildcards)
     * @return a Like condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("name").like("John%"); // name LIKE 'John%'
     * }</pre>
     */
    public Like like(final Object value) {
        return CF.like(propName, value);
    }

    /**
     * Creates a NOT LIKE condition for this property.
     *
     * @param value the pattern to exclude (can include % and _ wildcards)
     * @return a NotLike condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("email").notLike("%@temp.com"); // email NOT LIKE '%@temp.com'
     * }</pre>
     */
    public NotLike notLike(final Object value) {
        return CF.notLike(propName, value);
    }

    /**
     * Creates a LIKE condition that matches values starting with the specified prefix.
     *
     * @param value the prefix to match
     * @return a Like condition with % appended
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("name").startsWith("John"); // name LIKE 'John%'
     * }</pre>
     */
    public Like startsWith(final Object value) {
        return CF.startsWith(propName, value);
    }

    /**
     * Creates a LIKE condition that matches values ending with the specified suffix.
     *
     * @param value the suffix to match
     * @return a Like condition with % prepended
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("email").endsWith("@example.com"); // email LIKE '%@example.com'
     * }</pre>
     */
    public Like endsWith(final Object value) {
        return CF.endsWith(propName, value);
    }

    /**
     * Creates a LIKE condition that matches values containing the specified substring.
     *
     * @param value the substring to match
     * @return a Like condition with % on both sides
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("description").contains("important"); // description LIKE '%important%'
     * }</pre>
     */
    public Like contains(final Object value) {
        return CF.contains(propName, value);
    }

    /**
     * Creates an IN condition for this property with an array of values.
     *
     * @param values array of values to check membership against
     * @return an In condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * NamedProperty.of("status").in("active", "pending", "approved");
     * // Results in: status IN ('active', 'pending', 'approved')
     * }</pre>
     */
    public In in(final Object... values) {
        return CF.in(propName, values);
    }

    /**
     * Creates an IN condition for this property with a collection of values.
     *
     * @param values collection of values to check membership against
     * @return an In condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Set<Integer> validIds = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
     * NamedProperty.of("id").in(validIds);
     * // Results in: id IN (1, 2, 3, 4, 5)
     * }</pre>
     */
    public In in(final Collection<?> values) {
        return CF.in(propName, values);
    }

    /**
     * Generates the hash code for this NamedProperty based on the property name.
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
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof NamedProperty && N.equals(((NamedProperty) obj).propName, propName));
    }

    /**
     * Returns the string representation of this NamedProperty.
     *
     * @return the property name
     */
    @Override
    public String toString() {
        return propName;
    }
}