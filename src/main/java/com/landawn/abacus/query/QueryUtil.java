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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
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
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

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
     * Describes the database column associated with a property or column lookup key.
     *
     * @param columnName the mapped database column name
     * @param isUnqualified {@code true} if the mapped column name is unqualified (contains no {@code '.'} character)
     */
    public record ColumnInfo(String columnName, boolean isUnqualified) {
    }

    /**
     * Regular expression pattern for validating simple column names.
     * To match, a column name must be non-empty and consist solely of letters, digits, underscores, or hyphens.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean isValid = SIMPLE_COLUMN_NAME_PATTERN.matcher("column_name").matches();
     * }</pre>
     */
    public static final Pattern SIMPLE_COLUMN_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private QueryUtil() {
        // utility class — no instances
    }

    @SuppressWarnings("deprecation")
    static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final int DEFAULT_MAX_NESTED_PROP_DEPTH = 2;

    private static final int MAX_NESTED_PROP_DEPTH = Math.max(0, Integer.getInteger("abacus.query.maxNestedPropDepth", DEFAULT_MAX_NESTED_PROP_DEPTH));

    private static final String ENTITY_CLASS = "entityClass";

    private static final Map<Class<?>, ImmutableMap<String, String>> column2PropNameMapPool = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, String>>> entityTablePropColumnNameMap = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, ColumnInfo>>> entityPropColumnInfoMap = new ConcurrentHashMap<>();

    /**
     * Caches, per entity class, the {@link PropInfo} objects resolved for that class's ID
     * property names (in {@link #idPropNames(Class)} order). This avoids repeating the
     * reflective {@code BeanInfo.getPropInfo(name)} lookups on every insert-prop-name call.
     * A cached entry may contain {@code null} elements, mirroring the original per-call
     * behavior when an id name does not resolve to a {@link PropInfo}.
     */
    private static final Map<Class<?>, PropInfo[]> idPropInfosPool = new ConcurrentHashMap<>();

    /**
     * Caches, per entity class, the immutable {@code ImmutableList} returned for the
     * no-exclusion ({@code excludedPropNames} null/empty) paths of the {@code get*PropNames}
     * methods. Indexed by the array slot of {@code SqlBuilder.loadPropNamesByClass(Class)}
     * ({@code 0}: select incl. sub-entities, {@code 1}: select top-level only, {@code 2}: insert with id,
     * {@code 3}: insert without id, {@code 4}: update). Returning a single stable instance per (class, slot) preserves the
     * reference-identity fast paths in the builders (which compare the stored prop-name list with
     * {@code ==} against a fresh no-exclusion call) while still handing back an immutable list.
     */
    private static final Map<Class<?>, AtomicReferenceArray<ImmutableList<String>>> noExclusionPropNamesPool = new ConcurrentHashMap<>();

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
    private static ImmutableList<String> getNoExclusionPropNames(final Class<?> entityClass, final int slot) {
        final AtomicReferenceArray<ImmutableList<String>> cache = noExclusionPropNamesPool.computeIfAbsent(entityClass, cls -> new AtomicReferenceArray<>(5));

        ImmutableList<String> result = cache.get(slot);

        if (result == null) {
            final ImmutableList<String> candidate = ImmutableList.copyOf(SqlBuilder.loadPropNamesByClass(entityClass)[slot]);

            // Publish the first completed immutable snapshot atomically. A plain array element read
            // outside the writer's synchronized block has no happens-before edge and can expose a
            // stale/null reference; AtomicReferenceArray provides both safe publication and the
            // stable reference identity required by builder fast paths.
            if (cache.compareAndSet(slot, null, candidate)) {
                result = candidate;
            } else {
                result = cache.get(slot);
            }
        }

        return result;
    }

    /**
     * Returns column information keyed by both property names and mapped column names.
     * The {@link ColumnInfo#isUnqualified()} flag describes the mapped column value: it is
     * {@code true} when the column name contains no {@code '.'} character. Query builders use
     * this flag to avoid prepending another table alias to an already-qualified mapping.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get property-to-column details
     * ImmutableMap<String, QueryUtil.ColumnInfo> columnInfoMap =
     *     QueryUtil.propToColumnInfoMap(User.class, NamingPolicy.SNAKE_CASE);
     *
     * QueryUtil.ColumnInfo result = columnInfoMap.get("firstName");
     * String columnName = result.columnName(); // "first_name"
     * boolean unqualified = result.isUnqualified(); // true
     *
     * // Nested property example
     * QueryUtil.ColumnInfo nested = columnInfoMap.get("address.street");
     * String nestedColumn = nested.columnName(); // e.g. "addr.street"
     * boolean nestedUnqualified = nested.isUnqualified(); // false
     * }</pre>
     *
     * <p><b>Note:</b> despite the similar name, this is a different method from
     * {@link #propToColumnNameMap(Class, NamingPolicy)}, which returns a plain
     * property-name-to-column-name map ({@code ImmutableMap<String, String>}) and is the one
     * intended for general use.</p>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param namingPolicy the naming policy to use for column name conversion. If {@code null}, defaults to {@code NamingPolicy.SNAKE_CASE}.
     * @return an immutable map containing property-name keys and, when a mapped column name is not
     *         already a property-name key, an additional column-name key. Each value contains the
     *         mapped column name and whether that column name has no dot.
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     * @see #propToColumnNameMap(Class, NamingPolicy)
     */
    @Beta
    @Internal
    public static ImmutableMap<String, ColumnInfo> propToColumnInfoMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy;

        Map<NamingPolicy, ImmutableMap<String, ColumnInfo>> namingPropColumnInfoMap = entityPropColumnInfoMap.get(entityClass);
        ImmutableMap<String, ColumnInfo> result = null;

        if (namingPropColumnInfoMap == null || (result = namingPropColumnInfoMap.get(effectiveNamingPolicy)) == null) {
            final ImmutableMap<String, String> propToColumnNameMap = propToColumnNameMap(entityClass, effectiveNamingPolicy);
            final Map<String, ColumnInfo> newPropColumnInfoMap = N.newHashMap(propToColumnNameMap.size() * 2);

            for (final Map.Entry<String, String> entry : propToColumnNameMap.entrySet()) {
                newPropColumnInfoMap.put(entry.getKey(), new ColumnInfo(entry.getValue(), entry.getValue().indexOf('.') < 0));

                if (!propToColumnNameMap.containsKey(entry.getValue())) {
                    newPropColumnInfoMap.put(entry.getValue(), new ColumnInfo(entry.getValue(), entry.getValue().indexOf('.') < 0));
                }
            }

            result = ImmutableMap.wrap(newPropColumnInfoMap);

            if (namingPropColumnInfoMap == null) {
                final Map<NamingPolicy, ImmutableMap<String, ColumnInfo>> newMap = Collections.synchronizedMap(new EnumMap<>(NamingPolicy.class));
                final Map<NamingPolicy, ImmutableMap<String, ColumnInfo>> existingMap = entityPropColumnInfoMap.putIfAbsent(entityClass, newMap);
                namingPropColumnInfoMap = existingMap == null ? newMap : existingMap;
            }

            namingPropColumnInfoMap.put(effectiveNamingPolicy, result);
        }

        return result;
    }

    /**
     * Returns a mapping of column names to property names for the specified entity class.
     * The map includes variations of column names in lowercase and uppercase for case-insensitive lookups.
     * An explicitly declared column spelling takes precedence over a generated case-folded variation,
     * so distinct quoted columns such as {@code code} and {@code CODE} retain their exact mappings.
     * Only properties whose effective metadata exposes a column name (i.e.
     * {@link PropInfo#columnName} is present) are included. Metadata resolution also honors
     * {@link NonColumn} and {@link Table#columnFields()}/{@link Table#nonColumnFields()}, so an
     * otherwise annotated property excluded by the table mapping is not added.
     *
     * <p>This method is useful when you need to map database result set columns back to entity properties,
     * especially when dealing with case-insensitive database systems or when column names don't match
     * the exact case in your code.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given an entity class with @Column annotations
     * ImmutableMap<String, String> columnToProp = QueryUtil.columnToPropNameMap(User.class);
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
    public static ImmutableMap<String, String> columnToPropNameMap(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        // Backing map is a ConcurrentHashMap, so computeIfAbsent runs the (expensive)
        // metadata build at most once per class even under concurrent first-touch.
        return column2PropNameMapPool.computeIfAbsent(entityClass, cls -> {
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(cls);
            final Table tableAnno = cls.getAnnotation(Table.class);
            final String[] columnFieldArray = tableAnno == null ? N.EMPTY_STRING_ARRAY : tableAnno.columnFields();
            final String[] nonColumnFieldArray = tableAnno == null ? N.EMPTY_STRING_ARRAY : tableAnno.nonColumnFields();
            final Set<String> columnFields = columnFieldArray.length == 0 ? N.emptySet() : N.toSet(columnFieldArray);
            final Set<String> nonColumnFields = nonColumnFieldArray.length == 0 ? N.emptySet() : N.toSet(nonColumnFieldArray);
            final Map<String, String> map = N.newHashMap(entityInfo.propInfoList.size() * 3);

            // Register every exact spelling first. If two quoted identifiers differ only by case,
            // a folded alias from one must never overwrite the other's exact mapping.
            for (final PropInfo propInfo : entityInfo.propInfoList) {
                if (!isNonColumn(columnFields, nonColumnFields, propInfo) && propInfo.columnName.isPresent()) {
                    final String columnName = propInfo.columnName.get();
                    map.put(columnName, propInfo.name);
                }
            }

            // Add case-insensitive convenience keys only where no exact spelling (or earlier folded
            // spelling) is already registered. Ambiguous folded lookups are inherently lossy, but
            // exact lookups remain deterministic and correct.
            for (final PropInfo propInfo : entityInfo.propInfoList) {
                if (!isNonColumn(columnFields, nonColumnFields, propInfo) && propInfo.columnName.isPresent()) {
                    final String columnName = propInfo.columnName.get();
                    map.putIfAbsent(columnName.toLowerCase(Locale.ROOT), propInfo.name);
                    map.putIfAbsent(columnName.toUpperCase(Locale.ROOT), propInfo.name);
                }
            }

            return ImmutableMap.copyOf(map);
        });
    }

    /**
     * Returns a mapping of property names to column names for the specified entity class using the given naming policy.
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
     * // Build property-to-column mapping with SNAKE_CASE naming policy
     * ImmutableMap<String, String> propToColumn = QueryUtil.propToColumnNameMap(User.class, NamingPolicy.SNAKE_CASE);
     *
     * // If User has property "firstName" without @Column annotation:
     * String columnName = propToColumn.get("firstName");   // "first_name"
     *
     * // With SCREAMING_SNAKE_CASE naming policy
     * ImmutableMap<String, String> propToColumnUpper =
     *     QueryUtil.propToColumnNameMap(User.class, NamingPolicy.SCREAMING_SNAKE_CASE);
     * String upperColumn = propToColumnUpper.get("firstName");   // "FIRST_NAME"
     * }</pre>
     *
     * @param entityClass the entity class to analyze (may be {@code null})
     * @param namingPolicy the naming policy to use for column name conversion. If {@code null}, defaults to {@code NamingPolicy.SNAKE_CASE}.
     * @return an immutable map of property names to column names, or an empty immutable map if {@code entityClass} is {@code null} or is a {@link Map} type
     */
    @Internal
    public static ImmutableMap<String, String> propToColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
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
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

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
                        final String subTableAliasOrName = SqlBuilder.tableAliasOrName(propType.javaType(), namingPolicy);

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
            final Map<NamingPolicy, ImmutableMap<String, String>> newMap = Collections.synchronizedMap(new EnumMap<>(NamingPolicy.class));
            final Map<NamingPolicy, ImmutableMap<String, String>> existingMap = entityTablePropColumnNameMap.putIfAbsent(entityClass, newMap);
            namingPropColumnMap = existingMap == null ? newMap : existingMap;
        }

        namingPropColumnMap.put(namingPolicy, result);

        return result;
    }

    /**
     * Returns the property names to be used for INSERT operations on the given entity instance.
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
     * Collection<String> insertProps = QueryUtil.insertPropNames(user, null);
     * // Returns: ["name", "email", ...] (excludes "id" since it has default value)
     *
     * // With excluded properties
     * Set<String> excluded = N.asSet("email");
     * Collection<String> filteredProps = QueryUtil.insertPropNames(user, excluded);
     * // Returns: ["name", ...] (excludes both "id" and "email")
     * }</pre>
     *
     * @param entity the entity instance to analyze (must not be {@code null})
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for INSERT operations
     * @throws IllegalArgumentException if {@code entity} is {@code null}
     */
    @Internal
    public static ImmutableList<String> insertPropNames(final Object entity, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entity, "entity");

        final Class<?> entityClass = entity.getClass();

        // Resolve the ID PropInfo array once per class (cached); on subsequent calls this
        // is just a value read instead of repeated reflective BeanInfo.getPropInfo lookups.
        // The array mirrors idPropNames(entityClass) order and may contain null
        // elements where an id name does not resolve, exactly as the original per-call code.
        final PropInfo[] idPropInfos = idPropInfosPool.computeIfAbsent(entityClass, cls -> {
            final Collection<String> idPropNames = idPropNames(cls);
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
            final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass);
            // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the
            // exclusion path returns an immutable list, matching the no-exclusion path below.
            return ImmutableList.wrap(N.excludeAll(val[baseSlot], excludedPropNames));
        }

        // Return the memoized immutable list for this (class, slot) so both paths return an
        // ImmutableList<String> while preserving reference identity for the builders' == fast paths.
        return getNoExclusionPropNames(entityClass, baseSlot);
    }

    /**
     * Returns the property names to be used for INSERT operations on the given entity class.
     * This method returns all insertable properties (including ID fields) excluding those specified in {@code excludedPropNames}.
     *
     * <p>Unlike the instance-based version, this method does not check for default ID values since no
     * entity instance is provided. It returns all insertable properties including IDs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get all insertable property names for a class
     * Collection<String> insertProps = QueryUtil.insertPropNames(User.class, null);
     * // Returns: ["id", "name", "email", "createdDate", ...]
     *
     * // Exclude specific properties
     * Set<String> excluded = N.asSet("createdDate", "updatedDate");
     * Collection<String> filteredProps = QueryUtil.insertPropNames(User.class, excluded);
     * // Returns: ["id", "name", "email", ...]
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for INSERT operations
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static ImmutableList<String> insertPropNames(final Class<?> entityClass, final Set<String> excludedPropNames) {
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
     * Returns the property names to be used for SELECT operations on the given entity class.
     * This method can optionally include sub-entity properties for nested object retrieval.
     *
     * <p>When {@code includeSubEntityProperties} is {@code true}, the method returns nested properties using
     * dot notation (e.g., {@code "address.street"}). This is useful for building SELECT statements
     * that need to retrieve data for nested entity relationships.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top-level properties only
     * Collection<String> selectProps = QueryUtil.selectPropNames(User.class, false, null);
     * // Returns: ["id", "name", "email", ...]
     *
     * // Include sub-entity properties (e.g., nested Address)
     * Collection<String> allProps = QueryUtil.selectPropNames(User.class, true, null);
     * // Returns: ["id", "name", "email", "address.street", "address.city", ...]
     *
     * // Exclude specific properties
     * Set<String> excluded = N.asSet("email");
     * Collection<String> filteredProps = QueryUtil.selectPropNames(User.class, false, excluded);
     * // Returns: ["id", "name", ...]
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param includeSubEntityProperties {@code true} to include nested entity properties, {@code false} for top-level only
     * @param excludedPropNames set of property names to exclude (nullable). When sub-entity properties
     *        are included, excluding a root such as {@code "address"} also excludes descendants such as
     *        {@code "address.street"}.
     * @return an immutable list of property names suitable for SELECT operations
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static ImmutableList<String> selectPropNames(final Class<?> entityClass, final boolean includeSubEntityProperties,
            final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        final int slot = includeSubEntityProperties ? 0 : 1;

        if (N.isEmpty(excludedPropNames)) {
            // Return the memoized immutable list for this (class, slot) so both paths return an
            // ImmutableList<String> while preserving reference identity for the builders' == fast paths.
            return getNoExclusionPropNames(entityClass, slot);
        }

        final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass);

        if (includeSubEntityProperties) {
            final List<String> result = new ArrayList<>(val[slot].size());

            outer: for (final String propName : val[slot]) {
                for (final String excludedPropName : excludedPropNames) {
                    if (excludedPropName != null && (propName.equals(excludedPropName) || propName.startsWith(excludedPropName + SK.PERIOD))) {
                        continue outer;
                    }
                }

                result.add(propName);
            }

            return ImmutableList.wrap(result);
        }

        // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the
        // exclusion path also returns an immutable list.
        return ImmutableList.wrap(N.excludeAll(val[slot], excludedPropNames));
    }

    /**
     * Returns the property names to be used for SELECT operations on the given entity instance.
     * This is an instance-based convenience overload that derives the entity class via
     * {@code entity.getClass()} and delegates to {@link #selectPropNames(Class, boolean, Set)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User();
     * Collection<String> selectProps = QueryUtil.selectPropNames(user, false, null);
     * // Returns: ["id", "name", "email", ...]
     * }</pre>
     *
     * @param entity the entity instance to analyze (must not be {@code null})
     * @param includeSubEntityProperties {@code true} to include nested entity properties, {@code false} for top-level only
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for SELECT operations
     * @throws IllegalArgumentException if {@code entity} is {@code null}
     * @see #selectPropNames(Class, boolean, Set)
     */
    @Internal
    public static ImmutableList<String> selectPropNames(final Object entity, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entity, "entity");

        return selectPropNames(entity.getClass(), includeSubEntityProperties, excludedPropNames);
    }

    /**
     * Returns the property names to be used for UPDATE operations on the given entity class.
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
     * Collection<String> updateProps = QueryUtil.updatePropNames(Account.class, null);
     * // returns ["id", "gui", "emailAddress", "firstName", ...] (a plain @Id is NOT excluded)
     *
     * // Exclude additional properties explicitly.
     * Collection<String> filteredProps = QueryUtil.updatePropNames(Account.class, N.asSet("createTime"));
     * // returns the same list without "createTime"
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for UPDATE operations
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static ImmutableList<String> updatePropNames(final Class<?> entityClass, final Set<String> excludedPropNames) {
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
     * Returns the property names to be used for UPDATE operations on the given entity instance.
     * This is an instance-based convenience overload that derives the entity class via
     * {@code entity.getClass()} and delegates to {@link #updatePropNames(Class, Set)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Account account = new Account();
     * Collection<String> updateProps = QueryUtil.updatePropNames(account, null);
     * // Returns the same names as updatePropNames(Account.class, null)
     * }</pre>
     *
     * @param entity the entity instance to analyze (must not be {@code null})
     * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
     * @return an immutable list of property names suitable for UPDATE operations
     * @throws IllegalArgumentException if {@code entity} is {@code null}
     * @see #updatePropNames(Class, Set)
     */
    @Internal
    public static ImmutableList<String> updatePropNames(final Object entity, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entity, "entity");

        return updatePropNames(entity.getClass(), excludedPropNames);
    }

    /**
     * Returns the ID property names for the specified entity class.
     * ID fields are identified by {@code @Id} annotations on the entity properties.
     *
     * <p>This method returns all properties annotated with {@code @Id}. For entities without
     * explicit ID fields, an empty list is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get ID property names for an entity with @Id annotation
     * List<String> idFields = QueryUtil.idPropNames(User.class);
     * // Returns: ["id"] for a class with @Id on the "id" property
     *
     * // Composite key example
     * List<String> compositeIds = QueryUtil.idPropNames(OrderItem.class);
     * // Returns: ["orderId", "itemId"] for a composite key entity
     *
     * // Entity without @Id returns empty list
     * List<String> noIds = QueryUtil.idPropNames(LogEntry.class);
     * // Returns: []
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @return an immutable list of ID property names, or an empty list if no ID properties are defined
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    @Immutable
    public static ImmutableList<String> idPropNames(final Class<?> entityClass) {
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
     * Returns the table alias from the {@code @Table} annotation on the entity class.
     * The alias can be used in SQL queries to reference the table with a shorter name.
     *
     * <p>If no {@code @Table} annotation exists, this method returns {@code null}. If {@code @Table} is present
     * but the alias attribute is not specified, this method returns an empty string.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given: @Table(name = "users", alias = "u") on User class
     * String alias = QueryUtil.tableAlias(User.class);
     * // Returns: "u"
     *
     * // Given: @Table(name = "orders") on Order class (no alias)
     * String alias2 = QueryUtil.tableAlias(Order.class);
     * // Returns: "" (empty string when alias is not specified)
     *
     * // Given: no @Table annotation on LogEntry class
     * String alias3 = QueryUtil.tableAlias(LogEntry.class);
     * // Returns: null
     * }</pre>
     *
     * @param entityClass the entity class to check (must not be {@code null})
     * @return the table alias if defined in {@code @Table} annotation, empty string if {@code @Table} is present but alias is not set, or {@code null} if no {@code @Table} annotation exists
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static String tableAlias(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        final Table anno = entityClass.getAnnotation(Table.class);
        return anno == null ? null : anno.alias();
    }

    /**
     * Returns the table name and optional alias for the entity class using the default naming policy.
     * If {@code @Table} annotation is present, uses its values; otherwise derives the table name from the class name
     * using {@code NamingPolicy.SNAKE_CASE}.
     *
     * <p>The returned string is formatted as "tableName" or "tableName alias" depending on whether
     * an alias is defined.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given: @Table(name = "users", alias = "u") on User class
     * String tableAndAlias = QueryUtil.tableNameAndAlias(User.class);
     * // Returns: "users u"
     *
     * // Given: @Table(name = "orders") on Order class (no alias)
     * String tableOnly = QueryUtil.tableNameAndAlias(Order.class);
     * // Returns: "orders"
     *
     * // Given: no @Table annotation on MyEntity class
     * String derived = QueryUtil.tableNameAndAlias(MyEntity.class);
     * // Returns: "my_entity" (derived from class name using SNAKE_CASE)
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @return the table name, optionally followed by space and alias
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static String tableNameAndAlias(final Class<?> entityClass) {
        return tableNameAndAlias(entityClass, NamingPolicy.SNAKE_CASE);
    }

    /**
     * Returns the table name and optional alias for the entity class using the specified naming policy.
     * The table name is resolved the same way the query builders resolve it: from the {@code @Table}
     * annotation ({@link Table#name() name} or its deprecated {@code value()} alias) or a JPA
     * {@code javax.persistence}/{@code jakarta.persistence} {@code @Table} annotation; only when no
     * annotated name exists is the table name derived from the class name using the provided naming
     * policy. The alias comes from {@link Table#alias()} and is appended after a space when present.
     *
     * <p>Annotated names are used directly without any transformation; the naming policy is only applied
     * to a class-name-derived table name.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given: @Table(name = "users", alias = "u") - annotation takes priority
     * String tableAndAlias = QueryUtil.tableNameAndAlias(User.class, NamingPolicy.SCREAMING_SNAKE_CASE);
     * // Returns: "users u" (annotation values used as-is, naming policy ignored)
     *
     * // Given: no @Table annotation on MyEntity class
     * String snakeCase = QueryUtil.tableNameAndAlias(MyEntity.class, NamingPolicy.SNAKE_CASE);
     * // Returns: "my_entity"
     *
     * String upperCase = QueryUtil.tableNameAndAlias(MyEntity.class, NamingPolicy.SCREAMING_SNAKE_CASE);
     * // Returns: "MY_ENTITY"
     * }</pre>
     *
     * @param entityClass the entity class to analyze (must not be {@code null})
     * @param namingPolicy the naming policy used when no annotated table name exists. If {@code null},
     *        defaults to {@code NamingPolicy.SNAKE_CASE}.
     * @return the table name, optionally followed by space and alias
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    @Internal
    public static String tableNameAndAlias(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy;

        // Resolve the annotated table name the same way the query builders do: BeanInfo.tableName honors
        // @Table.name(), its deprecated @Table.value() alias, and the javax/jakarta persistence @Table
        // annotations, so this helper cannot diverge from the FROM clause rendered for the same class.
        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
        final Table anno = entityClass.getAnnotation(Table.class);
        final String alias = anno == null ? null : anno.alias();

        final String tableName = entityInfo.tableName.isPresent() ? entityInfo.tableName.get()
                : effectiveNamingPolicy.convert(ClassUtil.getSimpleClassName(entityClass));

        return Strings.isEmpty(alias) ? tableName : Strings.concat(tableName, SK.SPACE, alias);
    }
}
