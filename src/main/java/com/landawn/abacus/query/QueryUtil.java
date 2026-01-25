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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Immutable;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.NotColumn;
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
 * @see SQLBuilder
 * @see NamingPolicy
 */
public final class QueryUtil {

    /**
     * Regular expression pattern for validating alphanumeric column names.
     * Column names must consist of letters, digits, underscores, or hyphens.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean isValid = PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("column_name").matches();
     * }</pre>
     */
    public static final Pattern PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private QueryUtil() {
        // singleton
    }

    @SuppressWarnings("deprecation")
    static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final String ENTITY_CLASS = "entityClass";

    private static final Map<Class<?>, ImmutableMap<String, String>> column2PropNameNameMapPool = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, String>>> entityTablePropColumnNameMap = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, Tuple2<String, Boolean>>>> entityTablePropColumnNameMap2 = new ObjectPool<>(
            POOL_SIZE);

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
     * boolean isSimple = result._2;  // true (no dots in property name)
     *
     * // Nested property example
     * Tuple2<String, Boolean> nested = propMap.get("address.street");
     * String nestedColumn = nested._1;  // "address.street"
     * boolean isNestedSimple = nested._2;  // false (contains dot)
     * }</pre>
     *
     * @param entityClass the entity class to analyze
     * @param namingPolicy the naming policy to use for column name conversion
     * @return an immutable map where keys are property names and values are tuples of (column name, isSimpleProperty)
     * @deprecated for internal use only.
     */
    @Deprecated
    @Beta
    public static ImmutableMap<String, Tuple2<String, Boolean>> prop2ColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        Map<NamingPolicy, ImmutableMap<String, Tuple2<String, Boolean>>> namingPropColumnNameMap = entityTablePropColumnNameMap2.get(entityClass);
        ImmutableMap<String, Tuple2<String, Boolean>> result = null;

        if (namingPropColumnNameMap == null || (result = namingPropColumnNameMap.get(namingPolicy)) == null) {
            final ImmutableMap<String, String> prop2ColumnNameMap = getProp2ColumnNameMap(entityClass, namingPolicy);
            final Map<String, Tuple2<String, Boolean>> newProp2ColumnNameMap = N.newHashMap(prop2ColumnNameMap.size() * 2);

            for (final Map.Entry<String, String> entry : prop2ColumnNameMap.entrySet()) {
                newProp2ColumnNameMap.put(entry.getKey(), Tuple.of(entry.getValue(), entry.getKey().indexOf('.') < 0));

                if (!prop2ColumnNameMap.containsKey(entry.getValue())) {
                    newProp2ColumnNameMap.put(entry.getValue(), Tuple.of(entry.getValue(), entry.getValue().indexOf('.') < 0));
                }
            }

            result = ImmutableMap.wrap(newProp2ColumnNameMap);

            if (namingPropColumnNameMap == null) {
                namingPropColumnNameMap = new EnumMap<>(NamingPolicy.class);
                // TODO not necessary?
                // namingPropColumnNameMap = Collections.synchronizedMap(namingPropColumnNameMap)
                entityTablePropColumnNameMap2.put(entityClass, namingPropColumnNameMap);
            }

            namingPropColumnNameMap.put(namingPolicy, result);
        }

        return result;
    }

    /**
     * Gets a mapping of column names to property names for the specified entity class.
     * The map includes variations of column names in lowercase and uppercase for case-insensitive lookups.
     * Column names are derived from @Column annotations on the entity properties.
     *
     * <p>This method is useful when you need to map database result set columns back to entity properties,
     * especially when dealing with case-insensitive database systems or when column names don't match
     * the exact case in your code.</p>
     *
     * @param entityClass the entity class to analyze (must not be null)
     * @return an immutable map of column names (including case variations) to property names
     * @throws IllegalArgumentException if entityClass is null
     */
    public static ImmutableMap<String, String> getColumn2PropNameMap(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        ImmutableMap<String, String> result = column2PropNameNameMapPool.get(entityClass);

        if (result == null) {
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
            final Map<String, String> map = N.newHashMap(entityInfo.propInfoList.size() * 3);

            for (final PropInfo propInfo : entityInfo.propInfoList) {
                if (propInfo.columnName.isPresent()) {
                    final String columnName = propInfo.columnName.get();
                    map.put(columnName, propInfo.name);
                    map.put(columnName.toLowerCase(), propInfo.name);
                    map.put(columnName.toUpperCase(), propInfo.name);
                }
            }

            result = ImmutableMap.copyOf(map);

            column2PropNameNameMapPool.put(entityClass, result);
        }

        return result;
    }

    /**
     * Gets a mapping of property names to column names for the specified entity class using the given naming policy.
     * This method handles nested properties and respects {@code @Table} annotations for column field configurations.
     *
     * <p>The naming policy determines how property names are converted to column names when no explicit
     * {@code @Column} annotation is present. For nested bean properties, the method recursively builds mappings
     * with dot notation (e.g., "address.street" -> "address.street").
     *
     * @param entityClass the entity class to analyze
     * @param namingPolicy the naming policy to use for column name conversion
     * @return an immutable map of property names to column names, or empty map if entityClass is null or Map
     */
    public static ImmutableMap<String, String> getProp2ColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        if (entityClass == null || Map.class.isAssignableFrom(entityClass)) {
            return ImmutableMap.empty();
        }

        final Map<NamingPolicy, ImmutableMap<String, String>> namingColumnNameMap = entityTablePropColumnNameMap.get(entityClass);
        ImmutableMap<String, String> result = null;

        if (namingColumnNameMap == null || (result = namingColumnNameMap.get(namingPolicy)) == null) {
            result = registerEntityPropColumnNameMap(entityClass, namingPolicy, null);
        }

        return result;
    }

    static ImmutableMap<String, String> registerEntityPropColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy,
            final Set<Class<?>> registeringClasses) {
        N.checkArgNotNull(entityClass, "entityClass");

        if (registeringClasses != null) {
            if (registeringClasses.contains(entityClass)) {
                throw new IllegalStateException("Cyclic references detected among: " + registeringClasses);
            }

            registeringClasses.add(entityClass);
        }

        final Table tableAnno = entityClass.getAnnotation(Table.class);
        final Set<String> columnFields = tableAnno == null ? N.emptySet() : N.asSet(tableAnno.columnFields());
        final Set<String> nonColumnFields = tableAnno == null ? N.emptySet() : N.asSet(tableAnno.nonColumnFields());
        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
        Map<String, String> propColumnNameMap = N.newHashMap(entityInfo.propInfoList.size() * 2);

        for (final PropInfo propInfo : entityInfo.propInfoList) {
            if (isNotColumn(columnFields, nonColumnFields, propInfo)) {
                continue;
            }

            if (propInfo.columnName.isPresent()) {
                propColumnNameMap.put(propInfo.name, propInfo.columnName.get());
            } else {
                propColumnNameMap.put(propInfo.name, SQLBuilder.formalizeColumnName(propInfo.name, namingPolicy));

                final Type<?> propType = propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type;

                if (propType.isBean() && (registeringClasses == null || !registeringClasses.contains(propType.clazz()))) {
                    final Set<Class<?>> newRegisteringClasses = registeringClasses == null ? N.newLinkedHashSet() : registeringClasses;
                    newRegisteringClasses.add(entityClass);

                    final Map<String, String> subPropColumnNameMap = registerEntityPropColumnNameMap(propType.clazz(), namingPolicy, newRegisteringClasses);

                    if (N.notEmpty(subPropColumnNameMap)) {
                        final String subTableAliasOrName = SQLBuilder.getTableAliasOrName(propType.clazz(), namingPolicy);

                        for (final Map.Entry<String, String> entry : subPropColumnNameMap.entrySet()) {
                            propColumnNameMap.put(propInfo.name + SK.PERIOD + entry.getKey(), subTableAliasOrName + SK.PERIOD + entry.getValue());
                        }

                        propColumnNameMap.remove(propInfo.name); // remove sub entity prop.
                    }
                }
            }
        }

        //    final Map<String, String> tmp = entityTablePropColumnNameMap.get(entityClass);
        //
        //    if (N.notEmpty(tmp)) {
        //        propColumnNameMap.putAll(tmp);
        //    }

        if (N.isEmpty(propColumnNameMap)) {
            propColumnNameMap = N.emptyMap();
        }

        final ImmutableMap<String, String> result = ImmutableMap.wrap(propColumnNameMap);

        Map<NamingPolicy, ImmutableMap<String, String>> namingPropColumnMap = entityTablePropColumnNameMap.get(entityClass);

        if (namingPropColumnMap == null) {
            namingPropColumnMap = new EnumMap<>(NamingPolicy.class);
            // TODO not necessary?
            // namingPropColumnMap = Collections.synchronizedMap(namingPropColumnMap)
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
     *   <li>If all ID fields have default values (0 for numbers, null for objects), they are excluded from the result</li>
     *   <li>If any ID field has a non-default value, all insertable properties including IDs are returned</li>
     *   <li>This allows both auto-generated IDs and manually-assigned IDs to work correctly</li>
     * </ul>
     *
     * @param entity the entity instance to analyze (must not be null)
     * @param excludedPropNames set of property names to exclude from the result (can be empty, must not be null)
     * @return collection of property names suitable for INSERT operations
     * @deprecated for internal use only
     */
    @Internal
    public static Collection<String> getInsertPropNames(final Object entity, final Set<String> excludedPropNames) {
        final Class<?> entityClass = entity.getClass();

        final Collection<String>[] val = SQLBuilder.loadPropNamesByClass(entityClass);

        if (!N.isEmpty(excludedPropNames)) {
            final List<String> tmp = new ArrayList<>(val[2]);
            tmp.removeAll(excludedPropNames);
            return tmp;
        }

        final Collection<String> idPropNames = getIdFieldNames(entityClass);

        if (N.isEmpty(idPropNames)) {
            return val[2];
        }

        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);

        for (final String idPropName : idPropNames) {
            final PropInfo propInfo = entityInfo.getPropInfo(idPropName);
            if (propInfo != null) {
                final Object propValue = propInfo.getPropValue(entity);
                if (!SQLBuilder.isDefaultIdPropValue(propValue)) {
                    return val[2];
                }
            }
        }

        return val[3];
    }

    /**
     * Gets the property names to be used for INSERT operations on the given entity class.
     * This method returns all insertable properties (including ID fields) excluding those specified in excludedPropNames.
     *
     * <p>Unlike the instance-based version, this method does not check for default ID values since no
     * entity instance is provided. It returns all insertable properties including IDs.</p>
     *
     * @param entityClass the entity class to analyze (must not be null)
     * @param excludedPropNames set of property names to exclude from the result (can be empty, must not be null)
     * @return collection of property names suitable for INSERT operations
     * @throws IllegalArgumentException if entityClass is null
     * @deprecated for internal use only
     */
    @Internal
    public static Collection<String> getInsertPropNames(final Class<?> entityClass, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        final Collection<String>[] val = SQLBuilder.loadPropNamesByClass(entityClass);
        final Collection<String> propNames = val[2];

        if (N.isEmpty(excludedPropNames)) {
            return propNames;
        }
        final List<String> tmp = new ArrayList<>(propNames);
        tmp.removeAll(excludedPropNames);
        return tmp;
    }

    /**
     * Gets the property names to be used for SELECT operations on the given entity class.
     * This method can optionally include sub-entity properties for nested object retrieval.
     *
     * <p>When includeSubEntityProperties is true, the method returns nested properties using
     * dot notation (e.g., "address.street"). This is useful for building SELECT statements
     * that need to retrieve data for nested entity relationships.</p>
     *
     * @param entityClass the entity class to analyze (must not be null)
     * @param includeSubEntityProperties {@code true} to include nested entity properties, {@code false} for top-level only
     * @param excludedPropNames set of property names to exclude from the result (can be empty, must not be null)
     * @return collection of property names suitable for SELECT operations
     * @throws IllegalArgumentException if entityClass is null
     * @deprecated for internal use only
     */
    @Internal
    public static Collection<String> getSelectPropNames(final Class<?> entityClass, final boolean includeSubEntityProperties,
            final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        final Collection<String>[] val = SQLBuilder.loadPropNamesByClass(entityClass);
        final Collection<String> propNames = includeSubEntityProperties ? val[0] : val[1];

        if (N.isEmpty(excludedPropNames)) {
            return propNames;
        }

        return N.excludeAll(propNames, excludedPropNames);
    }

    /**
     * Gets the property names to be used for UPDATE operations on the given entity class.
     * This method automatically excludes ID fields and properties marked as non-updatable.
     *
     * <p>Properties are considered non-updatable if they are:</p>
     * <ul>
     *   <li>Annotated with @Id</li>
     *   <li>Marked as insertable=false, updatable=false in @Column</li>
     *   <li>Listed in the excludedPropNames parameter</li>
     * </ul>
     *
     * @param entityClass the entity class to analyze (must not be null)
     * @param excludedPropNames set of property names to exclude from the result (can be empty, must not be null)
     * @return collection of property names suitable for UPDATE operations
     * @throws IllegalArgumentException if entityClass is null
     * @deprecated for internal use only
     */
    @Internal
    public static Collection<String> getUpdatePropNames(final Class<?> entityClass, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        final Collection<String>[] val = SQLBuilder.loadPropNamesByClass(entityClass);
        final Collection<String> propNames = val[4];

        if (N.isEmpty(excludedPropNames)) {
            return propNames;
        }
        final List<String> tmp = new ArrayList<>(propNames);
        tmp.removeAll(excludedPropNames);
        return tmp;
    }

    /**
     * Gets the ID field names for the specified entity class.
     * ID fields are identified by @Id annotations on the entity properties.
     *
     * <p>This method returns all properties annotated with @Id. For entities without
     * explicit ID fields, an empty list is returned.</p>
     *
     * @param targetClass the entity class to analyze (must not be null)
     * @return an immutable list of ID field names, or empty list if no ID fields are defined
     * @throws IllegalArgumentException if targetClass is null
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    @Immutable
    public static List<String> getIdFieldNames(final Class<?> targetClass) {
        return getIdFieldNames(targetClass, false);
    }

    /**
     * Gets the ID field names for the specified entity class with option to return fake ID if none found.
     * This is useful for entities without explicit ID fields where a synthetic ID is needed.
     *
     * @param targetClass the entity class to analyze
     * @param fakeIdForEmpty if true, returns a fake ID when no ID fields are found
     * @return an immutable list of ID field names or fake ID if requested and none found
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    @Immutable
    public static List<String> getIdFieldNames(final Class<?> targetClass, final boolean fakeIdForEmpty) {
        N.checkArgNotNull(targetClass, ENTITY_CLASS);

        final ImmutableList<String> idPropNames = ParserUtil.getBeanInfo(targetClass).idPropNameList;

        return N.isEmpty(idPropNames) && fakeIdForEmpty ? fakeIds : idPropNames;
    }

    /**
     * Determines whether a property should be excluded from database column mapping.
     * A property is not a column if it's transient, annotated with @NotColumn, or excluded by @Table configuration.
     *
     * @param columnFields set of field names explicitly included as columns (from @Table annotation, can be null or empty)
     * @param nonColumnFields set of field names explicitly excluded as columns (from @Table annotation, can be null or empty)
     * @param propInfo the property information to check (must not be null)
     * @return {@code true} if the property should not be mapped to a database column
     */
    public static boolean isNotColumn(final Set<String> columnFields, final Set<String> nonColumnFields, final PropInfo propInfo) {
        return propInfo.isTransient || propInfo.isAnnotationPresent(NotColumn.class) || (N.notEmpty(columnFields) && !columnFields.contains(propInfo.name))
                || (N.notEmpty(nonColumnFields) && nonColumnFields.contains(propInfo.name));
    }

    private static final ImmutableList<String> fakeIds = ImmutableList.of("not_defined_fake_id_in_abacus_" + Strings.uuid());

    /**
     * Checks if the given ID property names represent a fake/synthetic ID.
     * Fake IDs are used internally when entities have no defined ID fields.
     *
     * @param idPropNames the list of ID property names to check
     * @return {@code true} if this is a fake ID
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    public static boolean isFakeId(final List<String> idPropNames) {
        return idPropNames != null && idPropNames.size() == 1 && fakeIds.get(0).equals(idPropNames.get(0));
    }

    private static final Map<Integer, String> QM_CACHE = new HashMap<>();

    static {
        for (int i = 0; i <= 30; i++) {
            QM_CACHE.put(i, Strings.repeat("?", i, ", "));
        }

        QM_CACHE.put(100, Strings.repeat("?", 100, ", "));
        QM_CACHE.put(200, Strings.repeat("?", 200, ", "));
        QM_CACHE.put(300, Strings.repeat("?", 300, ", "));
        QM_CACHE.put(500, Strings.repeat("?", 500, ", "));
        QM_CACHE.put(1000, Strings.repeat("?", 1000, ", "));
    }

    /**
     * Generates a string of question marks (?) repeated n times with comma-space delimiter.
     * This is commonly used for building parameterized SQL queries with multiple placeholders.
     * Common values are pre-cached for performance optimization.
     *
     * <p>Cached values include: 0-30, 100, 200, 300, 500, 1000</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String placeholders = QueryUtil.repeatQM(3);
     * // Returns: "?, ?, ?"
     * String sql = "INSERT INTO users (name, email, age) VALUES (" + placeholders + ")";
     * // Result: "INSERT INTO users (name, email, age) VALUES (?, ?, ?)"
     * }</pre>
     *
     * @param n the number of question marks to generate (must not be negative)
     * @return a string containing n question marks separated by ", ", or empty string if n is 0
     * @throws IllegalArgumentException if n is negative
     */
    public static String repeatQM(final int n) {
        N.checkArgNotNegative(n, "count");

        String result = QM_CACHE.get(n);

        if (result == null) {
            result = Strings.repeat("?", n, ", ");
        }

        return result;
    }

    /**
     * Gets the table alias from the @Table annotation on the entity class.
     * The alias can be used in SQL queries to reference the table with a shorter name.
     *
     * <p>If no @Table annotation exists or if the alias is not specified in the annotation,
     * this method returns null.</p>
     *
     * @param entityClass the entity class to check (must not be null)
     * @return the table alias if defined in @Table annotation, null otherwise
     * @throws IllegalArgumentException if entityClass is null
     */
    public static String getTableAlias(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        final Table anno = entityClass.getAnnotation(Table.class);
        return anno == null ? null : anno.alias();
    }

    /**
     * Gets the table name and optional alias for the entity class using the default naming policy.
     * If @Table annotation is present, uses its values; otherwise derives the table name from the class name
     * using SNAKE_CASE naming policy.
     *
     * <p>The returned string is formatted as "tableName" or "tableName alias" depending on whether
     * an alias is defined.</p>
     *
     * @param entityClass the entity class to analyze (must not be null)
     * @return the table name, optionally followed by space and alias
     * @throws IllegalArgumentException if entityClass is null
     */
    public static String getTableNameAndAlias(final Class<?> entityClass) {
        return getTableNameAndAlias(entityClass, NamingPolicy.SNAKE_CASE);
    }

    /**
     * Gets the table name and optional alias for the entity class using the specified naming policy.
     * If @Table annotation is present, uses its values; otherwise derives the table name from the class name
     * using the provided naming policy.
     *
     * <p>The naming policy is only used when no @Table annotation is present. If @Table is defined,
     * its name and alias values are used directly without any transformation.</p>
     *
     * @param entityClass the entity class to analyze (must not be null)
     * @param namingPolicy the naming policy to use for table name conversion when @Table is not present
     * @return the table name, optionally followed by space and alias
     * @throws IllegalArgumentException if entityClass is null
     */
    public static String getTableNameAndAlias(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        N.checkArgNotNull(entityClass, ENTITY_CLASS);

        final Table anno = entityClass.getAnnotation(Table.class);

        if (anno == null) {
            return namingPolicy.convert(ClassUtil.getSimpleClassName(entityClass));
        } else {
            final String tableName = anno.name();
            final String alias = anno.alias();
            return Strings.isEmpty(alias) ? tableName : Strings.concat(tableName, SK.SPACE, alias);
        }
    }
}
