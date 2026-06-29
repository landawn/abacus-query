/*
 * Copyright (C) 2021 HaiYang Li
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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Immutable;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.NonColumn;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.ObjectPool;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;

/**
 * Utility class for handling database query operations, entity-column mappings, and SQL generation helpers.
 * This class provides methods to map between entity properties and database columns, handle insert/update/select
 * operations, and manage ID field names for entities.
 * 
 * <p>The class maintains internal caches for performance optimization when dealing with entity-to-column mappings
 * and supports various naming policies for column name conversion.</p>
 * 
 * @see SqlBuilder
 * @see NamingPolicy
 */
public final class QueryUtil {

    /**
     * Regular expression pattern for validating simple column names.
     * To match, a column name must be non-empty and consist solely of letters, digits, underscores, or hyphens.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean isValid = PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("column_name").matches();
     * }</pre>
     */
    public static final Pattern PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private QueryUtil() {
        // utility class — no instances
    }

    @SuppressWarnings("deprecation")
    static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final int DEFAULT_MAX_NESTED_PROP_DEPTH = 2;

    private static final int MAX_NESTED_PROP_DEPTH = Math.max(0, Integer.getInteger("abacus.query.maxNestedPropDepth", DEFAULT_MAX_NESTED_PROP_DEPTH));

    private static final String ENTITY_CLASS = "entityClass";

    private static final Map<Class<?>, ImmutableMap<String, String>> column2PropNameNameMapPool = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, String>>> entityTablePropColumnNameMap = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, Tuple2<String, Boolean>>>> entityTablePropColumnNameMap2 = new ObjectPool<>(
            POOL_SIZE);

    /**
     * Caches, per entity class, the {@link PropInfo} objects resolved for that class's ID
     * property names (in {@link #getIdPropNames(Class)} order). This avoids repeating the
     * reflective {@code BeanInfo.getPropInfo(name)} lookups on every insert-prop-name call.
     * A cached entry may contain {@code null} elements, mirroring the original per-call
     * behavior when an id name does not resolve to a {@link PropInfo}.
     */
    private static final Map<Class<?>, PropInfo[]> idPropInfosPool = new ConcurrentHashMap<>();

    /**
     * Caches, per entity class, the immutable {@code ImmutableList} returned for the
     * no-exclusion ({@code excludedPropNames} null/empty) paths of the {@code get*PropNames}
     * methods. Index by the column slot of {@code SqlBuilder.loadPropNamesByClass(Class)}
     * ({@code 0}: select incl. sub-entities, {@code 1}: select top-level only, {@code 2}: insert with id,
     * {@code 3}: insert without id, {@code 4}: update). Returning a single stable instance per (class, slot) preserves the
     * reference-identity fast paths in the builders (which compare the stored prop-name list with
     * {@code ==} against a fresh no-exclusion call) while still handing back an immutable list.
     */
    private static final Map<Class<?>, ImmutableList<String>[]> noExclusionPropNamesPool = new ConcurrentHashMap<>();

    /**
     * Returns the cached immutable, no-exclusion property-name list for the given entity class and
     * {@code SqlBuilder.loadPropNamesByClass(Class)} slot, building (and memoizing) it on first use.
     * The cached list is a defensive immutable copy of the corresponding cached prop-name set, so a
     * single stable instance is returned for every no-exclusion call.
     *
     * @param entityClass the entity class whose prop-name list is requested
     * @param slot the {@code loadPropNamesByClass} array index (0, 1, 2, 3, or 4)
     * @return the memoized immutable list for that (class, slot)
     */
    @SuppressWarnings("unchecked")
    private static ImmutableList<String> getNoExclusionPropNames(final Class<?> entityClass, final int slot) {
        final ImmutableList<String>[] cache = noExclusionPropNamesPool.computeIfAbsent(entityClass, cls -> new ImmutableList[5]);

        ImmutableList<String> result = cache[slot];

        if (result == null) {
            // Defensive immutable copy of the cached prop-name set (order preserved from the
            // backing LinkedHashSet). Benign if two threads race: both build equal lists and the
            // last write wins; callers always observe a fully-built immutable list.
            result = ImmutableList.copyOf(SqlBuilder.loadPropNamesByClass(entityClass)[slot]);
            cache[slot] = result;
        }

        return result;
    }

    /**
     * Returns a mapping of property names to their corresponding column names and a flag indicating if it's a simple property.
     * This method is for internal use only and provides detailed mapping information including whether properties
     * contain dots (indicating nested properties).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get property to column mapping with simple property flag
     * ImmutableMap<String, Tuple2<String, Boolean>> propMap =
     *     QueryUtil.prop2ColumnNameMap(User.class, NamingPolicy.SNAKE_CASE);
     *
     * Tuple2<String, Boolean> result = propMap.get("firstName");
     * String columnName = result._1;  // "first_name"
     * boolean isSimple = result._2;   // true (no dots in property name)
     *
     * // Nested property example
     * Tuple2<String, Boolean> nested = propMap.get("address.street");
     * String nestedColumn = nested._1;     // "<sub-table-alias-or-name>.street" (e.g. "addr.street")
     * boolean isNestedSimple = nested._2;  // false (key contains a dot)
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param namingPolicy the naming policy to use for column name conversion. If {@code null}, defaults to {@code NamingPolicy.SNAKE_CASE}.
     * @return an immutable map whose entries come in two kinds:
     *         (1) property-name keys — each property name maps to a {@code (columnName, hasNoDot)} tuple, where
     *         {@code hasNoDot} is {@code true} when the property name contains no {@code '.'} character; and
     *         (2) column-name keys — for each entry whose column value is not already a property-name key, the column
     *         name itself is also inserted as a key mapping to {@code (columnName, hasNoDot)} (where {@code hasNoDot}
     *         reflects whether the column name contains no {@code '.'}).
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     * @deprecated for internal use only. No public replacement is provided.
     */
    @Deprecated
    @Beta
    public static ImmutableMap<String, Tuple2<String, Boolean>> prop2ColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy;

        Map<NamingPolicy, ImmutableMap<String, Tuple2<String, Boolean>>> namingPropColumnNameMap = entityTablePropColumnNameMap2.get(entityClass);
        ImmutableMap<String, Tuple2<String, Boolean>> result = null;

        if (namingPropColumnNameMap == null || (result = namingPropColumnNameMap.get(effectiveNamingPolicy)) == null) {
            final ImmutableMap<String, String> prop2ColumnNameMap = getProp2ColumnNameMap(entityClass, effectiveNamingPolicy);
            final Map<String, Tuple2<String, Boolean>> newProp2ColumnNameMap = N.newHashMap(prop2ColumnNameMap.size() * 2);

            for (final Map.Entry<String, String> entry : prop2ColumnNameMap.entrySet()) {
                newProp2ColumnNameMap.put(entry.getKey(), Tuple.of(entry.getValue(), entry.getKey().indexOf('.') < 0));

                if (!prop2ColumnNameMap.containsKey(entry.getValue())) {
                    newProp2ColumnNameMap.put(entry.getValue(), Tuple.of(entry.getValue(), entry.getValue().indexOf('.') < 0));
                }
            }

            result = ImmutableMap.wrap(newProp2ColumnNameMap);

            if (namingPropColumnNameMap == null) {
                namingPropColumnNameMap = Collections.synchronizedMap(new EnumMap<>(NamingPolicy.class));
                entityTablePropColumnNameMap2.put(entityClass, namingPropColumnNameMap);
            }

            namingPropColumnNameMap.put(effectiveNamingPolicy, result);
        }

        return result;
    }

    /**
     * Gets a mapping of column names to property names for the specified entity class.
     * The map includes variations of column names in lowercase and uppercase for case-insensitive lookups.
     * Only properties whose column name is resolvable from a {@code @Column} annotation (i.e.
     * {@link PropInfo#columnName} is present) are included; properties without a column annotation
     * are not added to the returned map.
     *
     * <p>This method is useful when you need to map database result set columns back to entity properties,
     * especially when dealing with case-insensitive database systems or when column names don't match
     * the exact case in your code.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given an entity class with @Column annotations
     * ImmutableMap<String, String> columnToProp = QueryUtil.getColumn2PropNameMap(User.class);
     *
     * // If User has @Column("User_Name") on property "userName":
     * String propName  = columnToProp.get("User_Name");   // "userName" (looked up by original column name)
     * String propName2 = columnToProp.get("USER_NAME");   // "userName" (looked up by uppercase variant)
     * String propName3 = columnToProp.get("user_name");   // "userName" (looked up by lowercase variant)
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @return an immutable map of column names (including upper- and lower-case variations) to property names
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static ImmutableMap<String, String> getColumn2PropNameMap(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        // Backing map is a ConcurrentHashMap, so computeIfAbsent runs the (expensive)
        // metadata build at most once per class even under concurrent first-touch.
        return column2PropNameNameMapPool.computeIfAbsent(entityClass, cls -> {
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(cls);
            final Map<String, String> map = N.newHashMap(entityInfo.propInfoList.size() * 3);

            for (final PropInfo propInfo : entityInfo.propInfoList) {
                if (propInfo.columnName.isPresent()) {
                    final String columnName = propInfo.columnName.get();
                    map.put(columnName, propInfo.name);
                    map.put(columnName.toLowerCase(Locale.ROOT), propInfo.name);
                    map.put(columnName.toUpperCase(Locale.ROOT), propInfo.name);
                }
            }

            return ImmutableMap.copyOf(map);
        });
    }

    /**
     * Gets a mapping of property names to column names for the specified entity class using the given naming policy.
     * This method handles nested properties and respects {@code @Table} annotations for column field configurations.
     *
     * <p>The naming policy determines how property names are converted to column names when no explicit
     * {@code @Column} annotation is present. For nested bean properties, the method recursively builds mappings
     * with dot notation up to {@code abacus.query.maxNestedPropDepth} bean hops (default: 2): a nested property
     * like {@code "address.street"} resolves to a value of the form {@code "<sub-table-alias-or-name>.<column>"}
     * (e.g. {@code "addr.street"} when the {@code Address} entity declares an alias {@code "addr"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get property-to-column mapping with SNAKE_CASE naming policy
     * ImmutableMap<String, String> propToColumn = QueryUtil.getProp2ColumnNameMap(User.class, NamingPolicy.SNAKE_CASE);
     *
     * // If User has property "firstName" without @Column annotation:
     * String columnName = propToColumn.get("firstName");   // "first_name"
     *
     * // With SCREAMING_SNAKE_CASE naming policy
     * ImmutableMap<String, String> propToColumnUpper =
     *     QueryUtil.getProp2ColumnNameMap(User.class, NamingPolicy.SCREAMING_SNAKE_CASE);
     * String upperColumn = propToColumnUpper.get("firstName");   // "FIRST_NAME"
     * }</pre>
     *
     * @param entityClass the entity class to analyze (may be {@code null})
     * @param namingPolicy the naming policy to use for column name conversion. If {@code null}, defaults to {@code NamingPolicy.SNAKE_CASE}.
     * @return an immutable map of property names to column names, or an empty immutable map if {@code entityClass} is {@code null} or is a {@link Map} type
     */
    @Internal
    public static ImmutableMap<String, String> getProp2ColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy;
        if (entityClass == null || Map.class.isAssignableFrom(entityClass)) {
            return ImmutableMap.empty();
        }

        final Map<NamingPolicy, ImmutableMap<String, String>> namingColumnNameMap = entityTablePropColumnNameMap.get(entityClass);
        ImmutableMap<String, String> result = null;

        if (namingColumnNameMap == null || (result = namingColumnNameMap.get(effectiveNamingPolicy)) == null) {
            result = registerEntityPropColumnNameMap(entityClass, effectiveNamingPolicy, null);
        }

        return result;
    }

    static ImmutableMap<String, String> registerEntityPropColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy,
            final Set<Class<?>> registeringClasses) {
        return registerEntityPropColumnNameMap(entityClass, namingPolicy, registeringClasses, MAX_NESTED_PROP_DEPTH);
    }

    private static ImmutableMap<String, String> registerEntityPropColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy,
            final Set<Class<?>> registeringClasses, final int remainingNestedPropDepth) {
        N.checkArgNotNull(entityClass, "entityClass");

        if (registeringClasses != null) {
            if (registeringClasses.contains(entityClass)) {
                // Stop expanding nested bean paths once a cycle is detected so
                // self-referential entities remain queryable instead of failing fast.
                return ImmutableMap.empty();
            }

            registeringClasses.add(entityClass);
        }

        final Table tableAnno = entityClass.getAnnotation(Table.class);
        final Set<String> columnFields;
        final Set<String> nonColumnFields;

        if (tableAnno == null) {
            columnFields = N.emptySet();
            nonColumnFields = N.emptySet();
        } else {
            // @Table accessors clone their array on each call and N.toSet allocates a HashSet even
            // for an empty array; capture each array once and reuse the shared empty set when the
            // field list is absent (the common case for tables that only declare name/alias).
            final String[] columnFieldArray = tableAnno.columnFields();
            columnFields = columnFieldArray.length == 0 ? N.emptySet() : N.toSet(columnFieldArray);

            final String[] nonColumnFieldArray = tableAnno.nonColumnFields();
            nonColumnFields = nonColumnFieldArray.length == 0 ? N.emptySet() : N.toSet(nonColumnFieldArray);
        }
        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
        Map<String, String> propColumnNameMap = N.newHashMap(entityInfo.propInfoList.size() * 2);

        for (final PropInfo propInfo : entityInfo.propInfoList) {
            if (isNonColumn(columnFields, nonColumnFields, propInfo)) {
                continue;
            }

            if (propInfo.columnName.isPresent()) {
                propColumnNameMap.put(propInfo.name, propInfo.columnName.get());
            } else {
                final Type<?> propType = propInfo.type.isCollection() ? propInfo.type.elementType() : propInfo.type;

                if (propType.isBean()) {
                    if (remainingNestedPropDepth <= 0 || (registeringClasses != null && registeringClasses.contains(propType.javaType()))) {
                        continue;
                    }

                    final Set<Class<?>> newRegisteringClasses = registeringClasses == null ? N.newLinkedHashSet() : new LinkedHashSet<>(registeringClasses);
                    newRegisteringClasses.add(entityClass);

                    final Map<String, String> subPropColumnNameMap = registerEntityPropColumnNameMap(propType.javaType(), namingPolicy, newRegisteringClasses,
                            remainingNestedPropDepth - 1);

                    if (N.notEmpty(subPropColumnNameMap)) {
                        final String subTableAliasOrName = SqlBuilder.getTableAliasOrName(propType.javaType(), namingPolicy);

                        for (final Map.Entry<String, String> entry : subPropColumnNameMap.entrySet()) {
                            propColumnNameMap.put(propInfo.name + SK.PERIOD + entry.getKey(), subTableAliasOrName + SK.PERIOD + entry.getValue());
                        }
                    }
                } else {
                    propColumnNameMap.put(propInfo.name, SqlBuilder.normalizeColumnName(propInfo.name, namingPolicy));
                }
            }
        }

        if (N.isEmpty(propColumnNameMap)) {
            propColumnNameMap = N.emptyMap();
        }

        final ImmutableMap<String, String> result = ImmutableMap.wrap(propColumnNameMap);

        if (registeringClasses != null) {
            return result;
        }

        Map<NamingPolicy, ImmutableMap<String, String>> namingPropColumnMap = entityTablePropColumnNameMap.get(entityClass);

        if (namingPropColumnMap == null) {
            namingPropColumnMap = Collections.synchronizedMap(new EnumMap<>(NamingPolicy.class));
            entityTablePropColumnNameMap.put(entityClass, namingPropColumnMap);
        }

        namingPropColumnMap.put(namingPolicy, result);

        return result;
    }

    /**
     * Gets the property names to be used for INSERT operations on the given entity instance.
     * This method considers ID fields and excludes properties marked as non-insertable.
     *
     * <p>The method intelligently handles ID fields:</p>
     * <ul>
     *   <li>If all ID fields have default values (e.g. {@code 0} for primitive numeric types,
     *       {@code null} for reference types) as determined by {@link SqlBuilder#isDefaultIdPropValue(Object)},
     *       they are excluded from the result.</li>
     *   <li>If any ID field has a non-default value, all insertable properties including IDs are returned.</li>
     *   <li>This allows both auto-generated IDs and manually-assigned IDs to work correctly.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User();
     * user.setName("John");
     * user.setEmail("john@example.com");
     * // user.id is at its default value (e.g. null for Long, 0 for long primitive)
     * // so the "id" property will be excluded.
     *
     * Collection<String> insertProps = QueryUtil.getInsertPropNames(user, null);
     * // Returns: ["name", "email", ...] (excludes "id" since it has default value)
     *
     * // With excluded properties
     * Set<String> excluded = N.asSet("email");
     * Collection<String> filteredProps = QueryUtil.getInsertPropNames(user, excluded);
     * // Returns: ["name", ...] (excludes both "id" and "email")
     * }</pre>
     *
     * @param entity the entity instance to analyze (must not be {@code null})
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for INSERT operations
     * @throws IllegalArgumentException if {@code entity} is {@code null}
     */
    @Internal
    public static ImmutableList<String> getInsertPropNames(final Object entity, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entity, "entity");

        final Class<?> entityClass = entity.getClass();

        final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass);

        // Resolve the ID PropInfo array once per class (cached); on subsequent calls this
        // is just a value read instead of repeated reflective BeanInfo.getPropInfo lookups.
        // The array mirrors getIdPropNames(entityClass) order and may contain null
        // elements where an id name does not resolve, exactly as the original per-call code.
        final PropInfo[] idPropInfos = idPropInfosPool.computeIfAbsent(entityClass, cls -> {
            final Collection<String> idPropNames = getIdPropNames(cls);
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(cls);
            final PropInfo[] resolved = new PropInfo[idPropNames.size()];
            int i = 0;

            for (final String idPropName : idPropNames) {
                resolved[i++] = entityInfo.getPropInfo(idPropName);
            }

            return resolved;
        });

        // Determine the base property names based on whether ID fields have default values.
        // val[2] includes ID fields, val[3] excludes them.
        int baseSlot = 2;

        if (idPropInfos.length > 0) {
            boolean allDefault = true;

            for (final PropInfo propInfo : idPropInfos) {
                if (propInfo != null) {
                    final Object propValue = propInfo.getPropValue(entity);
                    if (!SqlBuilder.isDefaultIdPropValue(propValue)) {
                        allDefault = false;
                        break;
                    }
                }
            }

            if (allDefault) {
                baseSlot = 3;
            }
        }

        if (!N.isEmpty(excludedPropNames)) {
            // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the
            // exclusion path returns an immutable list, matching the no-exclusion path below.
            return ImmutableList.wrap(N.excludeAll(val[baseSlot], excludedPropNames));
        }

        // Return the memoized immutable list for this (class, slot) so both paths return an
        // ImmutableList<String> while preserving reference identity for the builders' == fast paths.
        return getNoExclusionPropNames(entityClass, baseSlot);
    }

    /**
     * Gets the property names to be used for INSERT operations on the given entity class.
     * This method returns all insertable properties (including ID fields) excluding those specified in {@code excludedPropNames}.
     *
     * <p>Unlike the instance-based version, this method does not check for default ID values since no
     * entity instance is provided. It returns all insertable properties including IDs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get all insertable property names for a class
     * Collection<String> insertProps = QueryUtil.getInsertPropNames(User.class, null);
     * // Returns: ["id", "name", "email", "createdDate", ...]
     *
     * // Exclude specific properties
     * Set<String> excluded = N.asSet("createdDate", "updatedDate");
     * Collection<String> filteredProps = QueryUtil.getInsertPropNames(User.class, excluded);
     * // Returns: ["id", "name", "email", ...]
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for INSERT operations
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static ImmutableList<String> getInsertPropNames(final Class<?> entityClass, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        if (N.isEmpty(excludedPropNames)) {
            // Return the memoized immutable list (slot 2: insert with id) so both paths return an
            // ImmutableList<String> while preserving reference identity for the builders' == fast paths.
            return getNoExclusionPropNames(entityClass, 2);
        }

        final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass);

        // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the
        // exclusion path also returns an immutable list.
        return ImmutableList.wrap(N.excludeAll(val[2], excludedPropNames));
    }

    /**
     * Gets the property names to be used for SELECT operations on the given entity class.
     * This method can optionally include sub-entity properties for nested object retrieval.
     *
     * <p>When {@code includeSubEntityProperties} is {@code true}, the method returns nested properties using
     * dot notation (e.g., {@code "address.street"}). This is useful for building SELECT statements
     * that need to retrieve data for nested entity relationships.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top-level properties only
     * Collection<String> selectProps = QueryUtil.getSelectPropNames(User.class, false, null);
     * // Returns: ["id", "name", "email", ...]
     *
     * // Include sub-entity properties (e.g., nested Address)
     * Collection<String> allProps = QueryUtil.getSelectPropNames(User.class, true, null);
     * // Returns: ["id", "name", "email", "address.street", "address.city", ...]
     *
     * // Exclude specific properties
     * Set<String> excluded = N.asSet("email");
     * Collection<String> filteredProps = QueryUtil.getSelectPropNames(User.class, false, excluded);
     * // Returns: ["id", "name", ...]
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param includeSubEntityProperties {@code true} to include nested entity properties, {@code false} for top-level only
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for SELECT operations
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static ImmutableList<String> getSelectPropNames(final Class<?> entityClass, final boolean includeSubEntityProperties,
            final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        final int slot = includeSubEntityProperties ? 0 : 1;

        if (N.isEmpty(excludedPropNames)) {
            // Return the memoized immutable list for this (class, slot) so both paths return an
            // ImmutableList<String> while preserving reference identity for the builders' == fast paths.
            return getNoExclusionPropNames(entityClass, slot);
        }

        final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass);

        // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the
        // exclusion path also returns an immutable list.
        return ImmutableList.wrap(N.excludeAll(val[slot], excludedPropNames));
    }

    /**
     * Gets the property names to be used for SELECT operations on the given entity instance.
     * This is an instance-based convenience overload that derives the entity class via
     * {@code entity.getClass()} and delegates to {@link #getSelectPropNames(Class, boolean, Set)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User();
     * Collection<String> selectProps = QueryUtil.getSelectPropNames(user, false, null);
     * // Returns: ["id", "name", "email", ...]
     * }</pre>
     *
     * @param entity the entity instance to analyze (must not be {@code null})
     * @param includeSubEntityProperties {@code true} to include nested entity properties, {@code false} for top-level only
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for SELECT operations
     * @throws IllegalArgumentException if {@code entity} is {@code null}
     * @see #getSelectPropNames(Class, boolean, Set)
     */
    @Internal
    public static ImmutableList<String> getSelectPropNames(final Object entity, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entity, "entity");

        return getSelectPropNames(entity.getClass(), includeSubEntityProperties, excludedPropNames);
    }

    /**
     * Gets the property names to be used for UPDATE operations on the given entity class.
     * This method excludes properties marked as non-updatable. Note that a plain read-write
     * {@code @Id} property is <i>not</i> excluded; only {@code @ReadOnly}/{@code @ReadOnlyId}
     * (read-only) id properties are.
     *
     * <p>Properties are considered non-updatable if they are:</p>
     * <ul>
     *   <li>Annotated with {@code @ReadOnly}, {@code @ReadOnlyId}, or otherwise marked as a read-only id property</li>
     *   <li>Annotated with {@code @NonUpdatable}</li>
     *   <li>Excluded from column mapping (e.g. {@code transient}, annotated with {@code @NonColumn},
     *       or filtered out by the {@code @Table} {@code columnFields}/{@code nonColumnFields} configuration)</li>
     *   <li>Listed in the {@code excludedPropNames} parameter</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Account declares a plain read-write @Id "id", so it remains updatable.
     * Collection<String> updateProps = QueryUtil.getUpdatePropNames(Account.class, null);
     * // returns ["id", "gui", "emailAddress", "firstName", ...] (a plain @Id is NOT excluded)
     *
     * // Exclude additional properties explicitly.
     * Collection<String> filteredProps = QueryUtil.getUpdatePropNames(Account.class, N.asSet("createTime"));
     * // returns the same list without "createTime"
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for UPDATE operations
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static ImmutableList<String> getUpdatePropNames(final Class<?> entityClass, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        if (N.isEmpty(excludedPropNames)) {
            // Return the memoized immutable list (slot 4: update) so both paths return an
            // ImmutableList<String> while preserving reference identity for the builders' == fast paths.
            return getNoExclusionPropNames(entityClass, 4);
        }

        final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass);

        // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the
        // exclusion path also returns an immutable list.
        return ImmutableList.wrap(N.excludeAll(val[4], excludedPropNames));
    }

    /**
     * Gets the property names to be used for UPDATE operations on the given entity instance.
     * This is an instance-based convenience overload that derives the entity class via
     * {@code entity.getClass()} and delegates to {@link #getUpdatePropNames(Class, Set)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Account account = new Account();
     * Collection<String> updateProps = QueryUtil.getUpdatePropNames(account, null);
     * // Returns the same names as getUpdatePropNames(Account.class, null)
     * }</pre>
     *
     * @param entity the entity instance to analyze (must not be {@code null})
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for UPDATE operations
     * @throws IllegalArgumentException if {@code entity} is {@code null}
     * @see #getUpdatePropNames(Class, Set)
     */
    @Internal
    public static ImmutableList<String> getUpdatePropNames(final Object entity, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entity, "entity");

        return getUpdatePropNames(entity.getClass(), excludedPropNames);
    }

    /**
     * Gets the ID field names for the specified entity class.
     * ID fields are identified by {@code @Id} annotations on the entity properties.
     *
     * <p>This method returns all properties annotated with {@code @Id}. For entities without
     * explicit ID fields, an empty list is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get ID field names for an entity with @Id annotation
     * List<String> idFields = QueryUtil.getIdPropNames(User.class);
     * // Returns: ["id"] for a class with @Id on the "id" property
     *
     * // Composite key example
     * List<String> compositeIds = QueryUtil.getIdPropNames(OrderItem.class);
     * // Returns: ["orderId", "itemId"] for a composite key entity
     *
     * // Entity without @Id returns empty list
     * List<String> noIds = QueryUtil.getIdPropNames(LogEntry.class);
     * // Returns: []
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @return an immutable list of ID field names, or an empty list if no ID fields are defined
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    @Immutable
    public static ImmutableList<String> getIdPropNames(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        return ParserUtil.getBeanInfo(entityClass).idPropNameList;
    }

    /**
     * Determines whether a property should be excluded from database column mapping.
     * A property is not a column if it's {@code transient}, annotated with {@code @NonColumn}, or excluded by {@code @Table} configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BeanInfo beanInfo = ParserUtil.getBeanInfo(User.class);
     * PropInfo propInfo = beanInfo.getPropInfo("tempField");
     *
     * // Check if a property is excluded from column mapping
     * Set<String> columnFields = N.asSet("id", "name", "email");
     * Set<String> nonColumnFields = N.emptySet();
     * boolean excluded = QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo);
     * // Returns true if "tempField" is not in columnFields
     *
     * // Check with nonColumnFields
     * Set<String> nonColumns = N.asSet("tempField", "transientData");
     * boolean excluded2 = QueryUtil.isNonColumn(N.emptySet(), nonColumns, propInfo);
     * // Returns true if "tempField" is in nonColumnFields
     * }</pre>
     *
     * @param columnFields set of field names explicitly included as columns (typically derived from
     *                     {@link Table#columnFields()}; may be {@code null} or empty for no whitelist)
     * @param nonColumnFields set of field names explicitly excluded as columns (typically derived from
     *                        {@link Table#nonColumnFields()}; may be {@code null} or empty for no blacklist)
     * @param propInfo the property information to check (must not be {@code null})
     * @return {@code true} if the property should not be mapped to a database column
     * @throws IllegalArgumentException if {@code propInfo} is {@code null}
     */
    @Internal
    public static boolean isNonColumn(final Set<String> columnFields, final Set<String> nonColumnFields, final PropInfo propInfo) {
        N.checkArgNotNull(propInfo, "propInfo");

        return propInfo.isTransient || propInfo.isAnnotationPresent(NonColumn.class) || (N.notEmpty(columnFields) && !columnFields.contains(propInfo.name))
                || (N.notEmpty(nonColumnFields) && nonColumnFields.contains(propInfo.name));
    }

    /**
     * Dense lookup of pre-built placeholder strings for the common counts {@code 0..30}.
     * Indexed directly by count so {@link #placeholders(int)} avoids both {@link Integer}
     * boxing and a hash lookup on the hot path; {@code QM_CACHE[i]} equals
     * {@code Strings.repeat("?", i, ", ")}.
     */
    private static final String[] QM_CACHE = new String[31];

    // Pre-built placeholder strings for the larger "round" counts that the previous map-based
    // cache also retained, kept as constants so batch INSERT / large IN (...) clauses reuse a
    // single instance instead of rebuilding the string on every call.
    private static final String QM_100;

    private static final String QM_200;

    private static final String QM_300;

    private static final String QM_500;

    private static final String QM_1000;

    static {
        for (int i = 0; i < QM_CACHE.length; i++) {
            QM_CACHE[i] = Strings.repeat("?", i, ", ");
        }

        QM_100 = Strings.repeat("?", 100, ", ");
        QM_200 = Strings.repeat("?", 200, ", ");
        QM_300 = Strings.repeat("?", 300, ", ");
        QM_500 = Strings.repeat("?", 500, ", ");
        QM_1000 = Strings.repeat("?", 1000, ", ");
    }

    /**
     * Generates a string of question marks ({@code ?}) repeated {@code placeholderCount} times with comma-space delimiter ({@code ", "}).
     * This is commonly used for building parameterized SQL queries with multiple placeholders.
     * Common values are pre-cached for performance optimization.
     *
     * <p>Cached values include: 0-30, 100, 200, 300, 500, 1000</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String placeholders = QueryUtil.placeholders(3);
     * // Returns: "?, ?, ?"
     * String sql = "INSERT INTO users (name, email, age) VALUES (" + placeholders + ")";
     * // Result: "INSERT INTO users (name, email, age) VALUES (?, ?, ?)"
     * }</pre>
     *
     * @param placeholderCount the number of question marks to generate (must not be negative)
     * @return a string containing {@code placeholderCount} question marks separated by {@code ", "}, or empty string if {@code placeholderCount} is 0
     * @throws IllegalArgumentException if {@code placeholderCount} is negative
     */
    public static String placeholders(final int placeholderCount) {
        N.checkArgNotNegative(placeholderCount, "placeholderCount");

        if (placeholderCount < QM_CACHE.length) {
            return QM_CACHE[placeholderCount];
        }

        switch (placeholderCount) {
            case 100:
                return QM_100;
            case 200:
                return QM_200;
            case 300:
                return QM_300;
            case 500:
                return QM_500;
            case 1000:
                return QM_1000;
            default:
                return Strings.repeat("?", placeholderCount, ", ");
        }
    }

    /**
     * Gets the table alias from the {@code @Table} annotation on the entity class.
     * The alias can be used in SQL queries to reference the table with a shorter name.
     *
     * <p>If no {@code @Table} annotation exists, this method returns {@code null}. If {@code @Table} is present
     * but the alias attribute is not specified, this method returns an empty string.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given: @Table(name = "users", alias = "u") on User class
     * String alias = QueryUtil.getTableAlias(User.class);
     * // Returns: "u"
     *
     * // Given: @Table(name = "orders") on Order class (no alias)
     * String alias2 = QueryUtil.getTableAlias(Order.class);
     * // Returns: "" (empty string when alias is not specified)
     *
     * // Given: no @Table annotation on LogEntry class
     * String alias3 = QueryUtil.getTableAlias(LogEntry.class);
     * // Returns: null
     * }</pre>
     *
     * @param entityClass the entity class to check (must not be {@code null})
     * @return the table alias if defined in {@code @Table} annotation, empty string if {@code @Table} is present but alias is not set, or {@code null} if no {@code @Table} annotation exists
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static String getTableAlias(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        final Table anno = entityClass.getAnnotation(Table.class);
        return anno == null ? null : anno.alias();
    }

    /**
     * Gets the table name and optional alias for the entity class using the default naming policy.
     * If {@code @Table} annotation is present, uses its values; otherwise derives the table name from the class name
     * using {@code NamingPolicy.SNAKE_CASE}.
     *
     * <p>The returned string is formatted as "tableName" or "tableName alias" depending on whether
     * an alias is defined.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given: @Table(name = "users", alias = "u") on User class
     * String tableAndAlias = QueryUtil.getTableNameAndAlias(User.class);
     * // Returns: "users u"
     *
     * // Given: @Table(name = "orders") on Order class (no alias)
     * String tableOnly = QueryUtil.getTableNameAndAlias(Order.class);
     * // Returns: "orders"
     *
     * // Given: no @Table annotation on MyEntity class
     * String derived = QueryUtil.getTableNameAndAlias(MyEntity.class);
     * // Returns: "my_entity" (derived from class name using SNAKE_CASE)
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @return the table name, optionally followed by space and alias
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static String getTableNameAndAlias(final Class<?> entityClass) {
        return getTableNameAndAlias(entityClass, NamingPolicy.SNAKE_CASE);
    }

    /**
     * Gets the table name and optional alias for the entity class using the specified naming policy.
     * If {@code @Table} annotation is present, uses its values; otherwise derives the table name from the class name
     * using the provided naming policy.
     *
     * <p>The naming policy is only used when no {@code @Table} annotation is present. If {@code @Table} is defined,
     * its {@link Table#name() name} and {@link Table#alias() alias} values are used directly without any
     * transformation; the naming policy is ignored. If {@code @Table} is present but its {@code name} attribute
     * is left at the default (empty string), the returned table portion will likewise be empty.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given: @Table(name = "users", alias = "u") - annotation takes priority
     * String tableAndAlias = QueryUtil.getTableNameAndAlias(User.class, NamingPolicy.SCREAMING_SNAKE_CASE);
     * // Returns: "users u" (annotation values used as-is, naming policy ignored)
     *
     * // Given: no @Table annotation on MyEntity class
     * String snakeCase = QueryUtil.getTableNameAndAlias(MyEntity.class, NamingPolicy.SNAKE_CASE);
     * // Returns: "my_entity"
     *
     * String upperCase = QueryUtil.getTableNameAndAlias(MyEntity.class, NamingPolicy.SCREAMING_SNAKE_CASE);
     * // Returns: "MY_ENTITY"
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param namingPolicy the naming policy to use for table name conversion when {@code @Table} is not present. If {@code null}, defaults to {@code NamingPolicy.SNAKE_CASE}.
     * @return the table name, optionally followed by space and alias
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static String getTableNameAndAlias(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy;

        final Table anno = entityClass.getAnnotation(Table.class);

        if (anno == null) {
            return effectiveNamingPolicy.convert(ClassUtil.getSimpleClassName(entityClass));
        } else {
            final String tableName = anno.name();
            final String alias = anno.alias();
            return Strings.isEmpty(alias) ? tableName : Strings.concat(tableName, SK.SPACE, alias);
        }
    }
}
