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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for IN and NOT IN conditions in SQL queries.
 * This class provides all shared implementation for collection membership conditions,
 * similar to how {@link Binary} serves as the base for {@link Equal}, {@link NotEqual}, etc.
 *
 * <p>Two forms are supported:</p>
 * <ul>
 *   <li><b>Single-column</b> ({@link #AbstractIn(String, Operator, Collection)}): each value is a
 *       scalar, rendered as {@code propName IN (v1, v2, ...)}.</li>
 *   <li><b>Row value constructor</b>
 *       ({@link #AbstractIn(Collection, Operator, Collection)}): each value is itself a row whose
 *       width matches the number of property names, rendered as
 *       {@code (p1, p2) IN ((v1a, v1b), (v2a, v2b), ...)}. One-column row values are also allowed,
 *       for example {@code (p1) IN ((v1), (v2))}. This mirrors the subquery form provided by
 *       {@link AbstractInSubQuery}. A row may be supplied as a {@link Collection} or other
 *       {@link Iterable}, an object array, a {@link Map} (looked up by property name) or a bean (read
 *       by property name); see {@link #AbstractIn(Collection, Operator, Collection)} for details.</li>
 * </ul>
 *
 * <p><b>Portability note:</b> the row value-list form is supported by MySQL, PostgreSQL,
 * Oracle and DB2, but <i>not</i> by SQL Server (which only supports the {@code (a, b) IN (subquery)}
 * form — see {@link InSubQuery}).</p>
 *
 * <p>The only difference between {@link In} and {@link NotIn} is the operator
 * ({@code IN} vs {@code NOT IN}). All fields, getters, and methods
 * for parameters, string rendering, hashing, and equality are identical.</p>
 *
 * @see In
 * @see NotIn
 * @see InSubQuery
 * @see ComposableCondition
 */
public abstract class AbstractIn extends ComposableCondition {

    // For Kryo
    final Collection<String> propNames;

    private final boolean rowValueConstructor;

    private List<?> values;

    /** Lazily memoized parameters (performance only). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized hashCode (0 == not computed). */
    private transient int cachedHashCode;

    /** Lazily memoized immutable view of {@link #values} (performance only). */
    private transient ImmutableList<?> cachedValuesView;

    /**
     * Default constructor for serialization frameworks like Kryo.
     */
    AbstractIn() {
        propNames = Collections.emptyList();
        rowValueConstructor = false;
    }

    /**
     * Creates a new single-column IN or NOT IN condition. The given values are copied into an internal
     * {@link ArrayList}, so later mutations to the supplied collection do not affect this
     * condition. Individual elements may be literal values or {@link Condition} instances; the
     * latter have their parameters spliced into {@link #getParameters()}.
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param values the collection of values to check membership against (must not be {@code null} or empty);
     *               elements may be {@code null}
     * @throws IllegalArgumentException if {@code propName} is {@code null}/empty/blank or {@code values} is {@code null}/empty
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    protected AbstractIn(final String propName, final Operator operator, final Collection<?> values) {
        super(operator);

        checkPropName(propName);
        N.checkArgNotEmpty(values, "values");

        this.propNames = Collections.singletonList(propName);
        this.rowValueConstructor = false;
        this.values = new ArrayList<>(values);
    }

    /**
     * Creates a new row value constructor IN or NOT IN condition, rendered as
     * {@code (p1, p2) IN ((v1a, v1b), (v2a, v2b), ...)}. Each element of {@code valueRows} is a row whose
     * width must equal {@code propNames.size()}. A singleton {@code propNames} collection is valid and
     * renders as {@code (p1) IN ((v1), (v2), ...)}. A row may be supplied in any of the following forms:
     * <ul>
     *   <li>a {@link Collection} or other {@link Iterable} of exactly {@code propNames.size()} elements,
     *       taken positionally;</li>
     *   <li>an object array ({@code Object[]}) of exactly {@code propNames.size()} elements, taken
     *       positionally;</li>
     *   <li>a {@link Map} whose values are looked up by property name (a missing key yields {@code null});
     *       or</li>
     *   <li>a bean whose property values are read by property name.</li>
     * </ul>
     * Both the property names and each row are copied internally, so later mutations to the supplied
     * collections do not affect this condition. Individual row values may be literal values or
     * {@link Condition} instances; the latter have their parameters spliced into {@link #getParameters()}.
     *
     * @param propNames the property/column names (must not be {@code null} or empty and must not contain
     *                  {@code null}/blank names)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param valueRows the collection of value rows (must not be {@code null} or empty); each row must be
     *               non-{@code null} and resolve to exactly {@code propNames.size()} values (which may be
     *               {@code null}). A row may be a {@link Collection}, {@link Iterable}, object array,
     *               {@link Map} or bean
     * @throws IllegalArgumentException if {@code propNames} is {@code null}/empty or contains any {@code null}/blank name,
     *                                  if {@code valueRows} is {@code null}/empty, if any row is {@code null} or of an
     *                                  unsupported type, or if a positional row's width does not match {@code propNames.size()}
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    protected AbstractIn(final Collection<String> propNames, final Operator operator, final Collection<?> valueRows) {
        super(operator);

        this.propNames = copyAndValidatePropNames(propNames);
        this.rowValueConstructor = true;
        N.checkArgNotEmpty(valueRows, "valueRows");

        final int arity = this.propNames.size();
        final List<List<Object>> copy = new ArrayList<>(valueRows.size());

        for (final Object row : valueRows) {
            N.checkArgNotNull(row, "value row");

            copy.add(toRowTuple(row, this.propNames, arity));
        }

        this.values = copy;
    }

    /**
     * Normalizes a single row-value row into a list of exactly {@code arity} values, ordered to
     * match {@code propNames}. See {@link #AbstractIn(Collection, Operator, Collection)} for the accepted
     * row forms.
     */
    private static List<Object> toRowTuple(final Object row, final Collection<String> propNames, final int arity) {
        if (row instanceof Map) {
            final Map<?, ?> map = (Map<?, ?>) row;
            final List<Object> tuple = new ArrayList<>(arity);

            for (final String propName : propNames) {
                tuple.add(map.get(propName));
            }

            return tuple;
        } else if (row instanceof Object[]) {
            final Object[] array = (Object[]) row;
            checkRowWidth(array.length, arity);

            final List<Object> tuple = new ArrayList<>(arity);
            Collections.addAll(tuple, array);

            return tuple;
        } else if (row instanceof Iterable) {
            final List<Object> tuple = new ArrayList<>(arity);

            for (final Object element : (Iterable<?>) row) {
                tuple.add(element);
            }

            checkRowWidth(tuple.size(), arity);

            return tuple;
        } else if (Beans.isBeanClass(row.getClass())) {
            final List<Object> tuple = new ArrayList<>(arity);

            for (final String propName : propNames) {
                tuple.add(Beans.getPropValue(row, propName));
            }

            return tuple;
        } else {
            throw new IllegalArgumentException(
                    "Each row-value row must be a Collection, Iterable, array, Map or bean, but found: " + row.getClass().getName());
        }
    }

    private static void checkRowWidth(final int actual, final int arity) {
        if (actual != arity) {
            throw new IllegalArgumentException(
                    "Each value row must have exactly " + arity + " element(s) to match the number of property names, but found " + actual);
        }
    }

    private static Collection<String> copyAndValidatePropNames(final Collection<String> propNames) {
        N.checkArgNotEmpty(propNames, "propNames");

        final List<String> copy = new ArrayList<>(propNames.size());

        for (final String propName : propNames) {
            checkPropName(propName);

            copy.add(propName);
        }

        return Collections.unmodifiableList(copy);
    }

    /**
     * Gets the property name being checked in this IN or NOT IN condition. For a row-value
     * condition this returns the first property name; prefer {@link #getPropNames()} in that case.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In inCond = new In("status", Arrays.asList("active", "pending"));
     * String prop = inCond.getPropName();   // "status"
     * }</pre>
     *
     * @return the (first) property name, or {@code null} for an uninitialized instance
     */
    public String getPropName() {
        return N.firstOrNullIfEmpty(propNames);
    }

    /**
     * Gets the property names checked in this IN or NOT IN condition. For a single-column condition
     * the returned collection holds exactly one name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In single = new In("status", Arrays.asList("active", "pending"));
     * Collection<String> p1 = single.getPropNames();   // ["status"]
     *
     * In multi = new In(Arrays.asList("first_name", "last_name"),
     *                   Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));
     * Collection<String> p2 = multi.getPropNames();   // ["first_name", "last_name"]
     * }</pre>
     *
     * @return non-null immutable collection of property names
     */
    public Collection<String> getPropNames() {
        return propNames;
    }

    /**
     * Gets the values used by this IN or NOT IN condition. For a row-value condition each element
     * is itself a tuple (a list of values, one per property name).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In inCond = new In("status", Arrays.asList("active", "pending"));
     * List<?> values = inCond.getValues();   // ["active", "pending"]
     * }</pre>
     *
     * @return an immutable list of the values (or value tuples), or an empty immutable list for an uninitialized instance
     */
    public ImmutableList<?> getValues() { //NOSONAR
        if (values == null) {
            return ImmutableList.empty();
        }

        ImmutableList<?> view = cachedValuesView;

        if (view == null) {
            view = ImmutableList.wrap(values);
            cachedValuesView = view;
        }

        return view;
    }

    private boolean isRowValueConstructor() {
        return rowValueConstructor;
    }

    /**
     * Gets the parameter values for this condition, flattened in declaration order. For a single-column
     * scalar condition the parameters are the values from {@link #getValues()}; for a row-value
     * condition they are the row tuples flattened row by row, column within row. Any individual value
     * that is itself a {@link Condition} (a nested sub-condition) has its parameters spliced into the
     * result in place of that value; non-{@code Condition} values are included as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // String values listed in order
     * In in = new In("status", Arrays.asList("active", "pending"));
     * List<Object> p1 = in.getParameters();   // ["active", "pending"]
     *
     * // Numeric values
     * In nums = new In("id", Arrays.asList(1, 2, 3));
     * List<Object> p2 = nums.getParameters();   // [1, 2, 3]
     * }</pre>
     *
     * @return an immutable list of parameter values, or an empty immutable list for an uninitialized instance
     *         (e.g. created via the no-arg constructor for deserialization)
     */
    @Override
    public ImmutableList<Object> getParameters() {
        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = computeParameters();
            cachedParameters = result;
        }

        return result;
    }

    private ImmutableList<Object> computeParameters() {
        if (values == null) {
            return ImmutableList.empty();
        }

        final List<Object> parameters = new ArrayList<>(values.size());

        if (isRowValueConstructor()) {
            for (final Object tuple : values) {
                for (final Object value : (Collection<?>) tuple) {
                    addParameter(parameters, value);
                }
            }
        } else {
            for (final Object value : values) {
                addParameter(parameters, value);
            }
        }

        return ImmutableList.wrap(parameters);
    }

    private static void addParameter(final List<Object> parameters, final Object value) {
        if (value instanceof Condition) {
            parameters.addAll(((Condition) value).getParameters());
        } else {
            parameters.add(value);
        }
    }

    /**
     * Converts this condition to its string representation.
     * The format is {@code propName IN (v1, v2, ...)} for {@link In}, or
     * {@code propName NOT IN (v1, v2, ...)} for {@link NotIn}. If the operator is {@code null}
     * (only possible for an uninitialized instance), the literal {@code "null"} is rendered
     * in place of the operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // String values are single-quoted
     * In in = new In("status", Arrays.asList("active", "pending"));
     * String s1 = in.toString(NamingPolicy.NO_CHANGE);   // "status IN ('active', 'pending')"
     *
     * // NotIn uses the NOT IN operator
     * NotIn notIn = new NotIn("status", Arrays.asList("active", "pending"));
     * String s2 = notIn.toString(NamingPolicy.NO_CHANGE);   // "status NOT IN ('active', 'pending')"
     *
     * // Numeric values are unquoted; a null naming policy uses NO_CHANGE
     * In nums = new In("id", Arrays.asList(1, 2, 3));
     * String s3 = nums.toString(null);   // "id IN (1, 2, 3)"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to the property name(s);
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return the string representation, e.g., {@code "status IN ('active', 'pending')"} or, for a
     *         multi-column condition, {@code "(first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))"}
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Operator op = operator();
        final String opStr = op == null ? Strings.NULL : op.toString();

        final int size = values == null ? 0 : values.size();
        final StringBuilder sb = new StringBuilder(16 + (size << 3));

        if (isRowValueConstructor()) {
            sb.append(SK._PARENTHESIS_L);
            int p = 0;
            for (final String propName : propNames) {
                if (p++ > 0) {
                    sb.append(SK.COMMA_SPACE);
                }
                sb.append(effectiveNamingPolicy.convert(propName));
            }
            sb.append(SK._PARENTHESIS_R).append(SK._SPACE).append(opStr).append(SK.SPACE_PARENTHESIS_L);

            if (values != null) {
                for (int i = 0; i < size; i++) {
                    if (i > 0) {
                        sb.append(SK.COMMA_SPACE);
                    }
                    sb.append(SK._PARENTHESIS_L);
                    int c = 0;
                    for (final Object value : (Collection<?>) values.get(i)) {
                        if (c++ > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }
                        sb.append(formatParameter(value, effectiveNamingPolicy));
                    }
                    sb.append(SK._PARENTHESIS_R);
                }
            }

            sb.append(SK._PARENTHESIS_R);
            return sb.toString();
        }

        sb.append(effectiveNamingPolicy.convert(getPropName())).append(SK._SPACE).append(opStr).append(SK.SPACE_PARENTHESIS_L);

        if (values != null) {
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    sb.append(SK.COMMA_SPACE);
                }
                sb.append(formatParameter(values.get(i), effectiveNamingPolicy));
            }
        }

        sb.append(SK._PARENTHESIS_R);
        return sb.toString();
    }

    /**
     * Generates the hash code for this condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Same property/operator/values -> equal hash codes
     * In a = new In("status", Arrays.asList("active", "pending"));
     * In b = new In("status", Arrays.asList("active", "pending"));
     * boolean same = a.hashCode() == b.hashCode();   // true
     *
     * // Different values -> different hash codes
     * In c = new In("status", Arrays.asList("active"));
     * boolean diff = a.hashCode() == c.hashCode();   // false
     * }</pre>
     *
     * @return the hash code based on property name(s), operator, row-value mode, and values
     */
    @Override
    public int hashCode() {
        int h = cachedHashCode;

        if (h == 0) {
            h = 17;
            h = (h * 31) + N.hashCode(propNames);
            h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
            h = (h * 31) + (rowValueConstructor ? 1231 : 1237);
            h = (h * 31) + ((values == null) ? 0 : values.hashCode());

            if (h == 0) {
                h = 1;
            }

            cachedHashCode = h;
        }

        return h;
    }

    /**
     * Checks if this condition is equal to another object.
     * Two conditions are equal if they have the same property name(s),
     * operator, row-value mode, and values list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In a = new In("status", Arrays.asList("active", "pending"));
     * In b = new In("status", Arrays.asList("active", "pending"));
     * boolean eq = a.equals(b);   // true
     *
     * // Different values -> not equal
     * boolean neValues = a.equals(new In("status", Arrays.asList("active")));   // false
     *
     * // Different operator (IN vs NOT IN) -> not equal
     * boolean neOp = a.equals(new NotIn("status", Arrays.asList("active", "pending")));   // false
     *
     * // Non-AbstractIn object -> not equal
     * boolean neType = a.equals("status");   // false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final AbstractIn other = (AbstractIn) obj;
        return rowValueConstructor == other.rowValueConstructor && N.equals(propNames, other.propNames) && N.equals(operator, other.operator)
                && N.equals(values, other.values);
    }
}
