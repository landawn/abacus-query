/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import static com.landawn.abacus.util.SK._PARENTHESES_L;
import static com.landawn.abacus.util.SK._PARENTHESES_R;
import static com.landawn.abacus.util.SK._SPACE;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.condition.Between;
import com.landawn.abacus.condition.Binary;
import com.landawn.abacus.condition.Cell;
import com.landawn.abacus.condition.Clause;
import com.landawn.abacus.condition.Condition;
import com.landawn.abacus.condition.ConditionFactory;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.condition.Criteria;
import com.landawn.abacus.condition.Expression;
import com.landawn.abacus.condition.Having;
import com.landawn.abacus.condition.In;
import com.landawn.abacus.condition.InSubQuery;
import com.landawn.abacus.condition.Join;
import com.landawn.abacus.condition.Junction;
import com.landawn.abacus.condition.Limit;
import com.landawn.abacus.condition.NotBetween;
import com.landawn.abacus.condition.NotIn;
import com.landawn.abacus.condition.NotInSubQuery;
import com.landawn.abacus.condition.SubQuery;
import com.landawn.abacus.condition.Where;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 * A fluent SQL builder for constructing SQL statements programmatically.
 * 
 * <p>This builder provides a type-safe way to construct SQL statements with support for:</p>
 * <ul>
 *   <li>SELECT, INSERT, UPDATE, DELETE operations</li>
 *   <li>Multiple naming policies (snake_case, UPPER_CASE, camelCase)</li>
 *   <li>Parameterized and named SQL generation</li>
 *   <li>Entity class mapping with annotations</li>
 *   <li>Complex joins, subqueries, and conditions</li>
 * </ul>
 * 
 * <p>The builder must be finalized by calling {@code sql()} or {@code pair()} to generate 
 * the SQL string and release resources.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple SELECT
 * String sql = PSC.select("firstName", "lastName")
 *                 .from("account")
 *                 .where(CF.eq("id", 1))
 *                 .sql();
 * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = ?
 * 
 * // INSERT with entity
 * String sql = PSC.insert(account).into("account").sql();
 * 
 * // UPDATE with conditions
 * String sql = PSC.update("account")
 *                 .set("name", "status")
 *                 .where(CF.eq("id", 1))
 *                 .sql();
 * }</pre>
 * 
 * <p>The builder supports different naming policies through its subclasses:</p>
 * <ul>
 *   <li>{@link PSC} - Parameterized SQL with snake_case naming</li>
 *   <li>{@link PAC} - Parameterized SQL with UPPER_CASE naming</li>
 *   <li>{@link PLC} - Parameterized SQL with lowerCamelCase naming</li>
 *   <li>{@link NSC} - Named SQL with snake_case naming</li>
 *   <li>{@link NAC} - Named SQL with UPPER_CASE naming</li>
 *   <li>{@link NLC} - Named SQL with lowerCamelCase naming</li>
 * </ul>
 * 
 * @see {@link com.landawn.abacus.annotation.ReadOnly}
 * @see {@link com.landawn.abacus.annotation.ReadOnlyId}
 * @see {@link com.landawn.abacus.annotation.NonUpdatable}
 * @see {@link com.landawn.abacus.annotation.Transient}
 * @see {@link com.landawn.abacus.annotation.Table}
 * @see {@link com.landawn.abacus.annotation.Column}
 */
@SuppressWarnings("deprecation")
public abstract class SQLBuilder { // NOSONAR

    // TODO performance goal: 80% cases (or maybe SQL.length < 1024?) can be composed in 0.1 millisecond. 0.01 millisecond will be fantastic if possible.

    protected static final Logger logger = LoggerFactory.getLogger(SQLBuilder.class);

    /** Constant for selecting all columns in SQL queries. */
    public static final String ALL = SK.ALL;

    /** Constant for TOP clause in SQL queries. */
    public static final String TOP = SK.TOP;

    /** Constant for UNIQUE clause in SQL queries. */
    public static final String UNIQUE = SK.UNIQUE;

    /** Constant for DISTINCT clause in SQL queries. */
    public static final String DISTINCT = SK.DISTINCT;

    /** Constant for DISTINCTROW clause in SQL queries. */
    public static final String DISTINCTROW = SK.DISTINCTROW;

    /** Constant for asterisk (*) wildcard in SQL queries. */
    public static final String ASTERISK = SK.ASTERISK;

    /** Constant for COUNT(*) aggregate function. */
    public static final String COUNT_ALL = "count(*)";

    protected static final List<String> COUNT_ALL_LIST = ImmutableList.of(COUNT_ALL);

    //    public static final String _1 = "1";
    //
    //    public static final List<String> _1_list = ImmutableList.of(_1);

    protected static final char[] _INSERT = SK.INSERT.toCharArray();

    protected static final char[] _SPACE_INSERT_SPACE = (SK.SPACE + SK.INSERT + SK.SPACE).toCharArray();

    protected static final char[] _INTO = SK.INTO.toCharArray();

    protected static final char[] _SPACE_INTO_SPACE = (SK.SPACE + SK.INTO + SK.SPACE).toCharArray();

    protected static final char[] _VALUES = SK.VALUES.toCharArray();

    protected static final char[] _SPACE_VALUES_SPACE = (SK.SPACE + SK.VALUES + SK.SPACE).toCharArray();

    protected static final char[] _SELECT = SK.SELECT.toCharArray();

    protected static final char[] _SPACE_SELECT_SPACE = (SK.SPACE + SK.SELECT + SK.SPACE).toCharArray();

    protected static final char[] _FROM = SK.FROM.toCharArray();

    protected static final char[] _SPACE_FROM_SPACE = (SK.SPACE + SK.FROM + SK.SPACE).toCharArray();

    protected static final char[] _UPDATE = SK.UPDATE.toCharArray();

    protected static final char[] _SPACE_UPDATE_SPACE = (SK.SPACE + SK.UPDATE + SK.SPACE).toCharArray();

    protected static final char[] _SET = SK.SET.toCharArray();

    protected static final char[] _SPACE_SET_SPACE = (SK.SPACE + SK.SET + SK.SPACE).toCharArray();

    protected static final char[] _DELETE = SK.DELETE.toCharArray();

    protected static final char[] _SPACE_DELETE_SPACE = (SK.SPACE + SK.DELETE + SK.SPACE).toCharArray();

    protected static final char[] _JOIN = SK.JOIN.toCharArray();

    protected static final char[] _SPACE_JOIN_SPACE = (SK.SPACE + SK.JOIN + SK.SPACE).toCharArray();

    protected static final char[] _LEFT_JOIN = SK.LEFT_JOIN.toCharArray();

    protected static final char[] _SPACE_LEFT_JOIN_SPACE = (SK.SPACE + SK.LEFT_JOIN + SK.SPACE).toCharArray();

    protected static final char[] _RIGHT_JOIN = SK.RIGHT_JOIN.toCharArray();

    protected static final char[] _SPACE_RIGHT_JOIN_SPACE = (SK.SPACE + SK.RIGHT_JOIN + SK.SPACE).toCharArray();

    protected static final char[] _FULL_JOIN = SK.FULL_JOIN.toCharArray();

    protected static final char[] _SPACE_FULL_JOIN_SPACE = (SK.SPACE + SK.FULL_JOIN + SK.SPACE).toCharArray();

    protected static final char[] _CROSS_JOIN = SK.CROSS_JOIN.toCharArray();

    protected static final char[] _SPACE_CROSS_JOIN_SPACE = (SK.SPACE + SK.CROSS_JOIN + SK.SPACE).toCharArray();

    protected static final char[] _INNER_JOIN = SK.INNER_JOIN.toCharArray();

    protected static final char[] _SPACE_INNER_JOIN_SPACE = (SK.SPACE + SK.INNER_JOIN + SK.SPACE).toCharArray();

    protected static final char[] _NATURAL_JOIN = SK.NATURAL_JOIN.toCharArray();

    protected static final char[] _SPACE_NATURAL_JOIN_SPACE = (SK.SPACE + SK.NATURAL_JOIN + SK.SPACE).toCharArray();

    protected static final char[] _ON = SK.ON.toCharArray();

    protected static final char[] _SPACE_ON_SPACE = (SK.SPACE + SK.ON + SK.SPACE).toCharArray();

    protected static final char[] _USING = SK.USING.toCharArray();

    protected static final char[] _SPACE_USING_SPACE = (SK.SPACE + SK.USING + SK.SPACE).toCharArray();

    protected static final char[] _WHERE = SK.WHERE.toCharArray();

    protected static final char[] _SPACE_WHERE_SPACE = (SK.SPACE + SK.WHERE + SK.SPACE).toCharArray();

    protected static final char[] _GROUP_BY = SK.GROUP_BY.toCharArray();

    protected static final char[] _SPACE_GROUP_BY_SPACE = (SK.SPACE + SK.GROUP_BY + SK.SPACE).toCharArray();

    protected static final char[] _HAVING = SK.HAVING.toCharArray();

    protected static final char[] _SPACE_HAVING_SPACE = (SK.SPACE + SK.HAVING + SK.SPACE).toCharArray();

    protected static final char[] _ORDER_BY = SK.ORDER_BY.toCharArray();

    protected static final char[] _SPACE_ORDER_BY_SPACE = (SK.SPACE + SK.ORDER_BY + SK.SPACE).toCharArray();

    protected static final char[] _LIMIT = (SK.SPACE + SK.LIMIT + SK.SPACE).toCharArray();

    protected static final char[] _SPACE_LIMIT_SPACE = (SK.SPACE + SK.LIMIT + SK.SPACE).toCharArray();

    protected static final char[] _OFFSET = SK.OFFSET.toCharArray();

    protected static final char[] _SPACE_OFFSET_SPACE = (SK.SPACE + SK.OFFSET + SK.SPACE).toCharArray();

    protected static final char[] _SPACE_ROWS = (SK.SPACE + SK.ROWS).toCharArray();

    protected static final char[] _AND = SK.AND.toCharArray();

    protected static final char[] _SPACE_AND_SPACE = (SK.SPACE + SK.AND + SK.SPACE).toCharArray();

    protected static final char[] _OR = SK.OR.toCharArray();

    protected static final char[] _SPACE_OR_SPACE = (SK.SPACE + SK.OR + SK.SPACE).toCharArray();

    protected static final char[] _UNION = SK.UNION.toCharArray();

    protected static final char[] _SPACE_UNION_SPACE = (SK.SPACE + SK.UNION + SK.SPACE).toCharArray();

    protected static final char[] _UNION_ALL = SK.UNION_ALL.toCharArray();

    protected static final char[] _SPACE_UNION_ALL_SPACE = (SK.SPACE + SK.UNION_ALL + SK.SPACE).toCharArray();

    protected static final char[] _INTERSECT = SK.INTERSECT.toCharArray();

    protected static final char[] _SPACE_INTERSECT_SPACE = (SK.SPACE + SK.INTERSECT + SK.SPACE).toCharArray();

    protected static final char[] _EXCEPT = SK.EXCEPT.toCharArray();

    protected static final char[] _SPACE_EXCEPT_SPACE = (SK.SPACE + SK.EXCEPT + SK.SPACE).toCharArray();

    protected static final char[] _EXCEPT2 = SK.EXCEPT2.toCharArray();

    protected static final char[] _SPACE_EXCEPT2_SPACE = (SK.SPACE + SK.EXCEPT2 + SK.SPACE).toCharArray();

    protected static final char[] _AS = SK.AS.toCharArray();

    protected static final char[] _SPACE_AS_SPACE = (SK.SPACE + SK.AS + SK.SPACE).toCharArray();

    protected static final char[] _SPACE_EQUAL_SPACE = (SK.SPACE + SK.EQUAL + SK.SPACE).toCharArray();

    protected static final char[] _SPACE_FOR_UPDATE = (SK.SPACE + SK.FOR_UPDATE).toCharArray();

    protected static final char[] _COMMA_SPACE = SK.COMMA_SPACE.toCharArray();

    protected static final String SPACE_AS_SPACE = SK.SPACE + SK.AS + SK.SPACE;

    protected static final String SELECTION_PART_MSG = "The specified parameter is not valid for selection part. It must not be null or empty";
    protected static final String INSERTION_PART_MSG = "The specified parameter is not valid for insertion part. It must not be null or empty";
    protected static final String UPDATE_PART_MSG = "The specified parameter is not valid for update part. It must not be null or empty";
    protected static final String DELETION_PART_MSG = "The specified parameter is not valid for deletion part. It must not be null or empty";

    protected static final Set<String> sqlKeyWords = N.newHashSet(1024);

    static {
        final Field[] fields = SK.class.getDeclaredFields();
        int m = 0;

        for (final Field field : fields) {
            m = field.getModifiers();

            if (Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m) && field.getType().equals(String.class)) {
                try {
                    final String value = (String) field.get(null);

                    for (final String e : Strings.split(value, ' ', true)) {
                        sqlKeyWords.add(e);
                        sqlKeyWords.add(e.toUpperCase());
                        sqlKeyWords.add(e.toLowerCase());
                    }
                } catch (final Exception e) {
                    // ignore, should never happen.
                }
            }
        }
    }

    protected static final Map<Class<?>, ImmutableSet<String>> subEntityPropNamesPool = new ObjectPool<>(N.POOL_SIZE);

    // private static final Map<Class<?>, ImmutableSet<String>> nonSubEntityPropNamesPool = new ObjectPool<>(N.POOL_SIZE);

    protected static final Map<Class<?>, Set<String>[]> defaultPropNamesPool = new ObjectPool<>(N.POOL_SIZE);

    protected static final Map<NamingPolicy, Map<Class<?>, String>> fullSelectPartsPool = N.newHashMap(NamingPolicy.values().length);

    static {
        for (final NamingPolicy np : NamingPolicy.values()) {
            fullSelectPartsPool.put(np, new ConcurrentHashMap<>());
        }
    }

    protected static final Map<String, char[]> tableDeleteFrom = new ConcurrentHashMap<>();

    protected static final Map<Class<?>, String[]> classTableNameMap = new ConcurrentHashMap<>();

    protected static final Map<Class<?>, String> classTableAliasMap = new ConcurrentHashMap<>();

    protected static final AtomicInteger activeStringBuilderCounter = new AtomicInteger();

    protected final NamingPolicy _namingPolicy; //NOSONAR

    protected final SQLPolicy _sqlPolicy; //NOSONAR

    protected final List<Object> _parameters = new ArrayList<>(); //NOSONAR

    protected StringBuilder _sb; //NOSONAR

    protected Class<?> _entityClass; //NOSONAR

    protected BeanInfo _entityInfo; //NOSONAR

    protected ImmutableMap<String, Tuple2<String, Boolean>> _propColumnNameMap; //NOSONAR

    protected OperationType _op; //NOSONAR

    protected String _tableName; //NOSONAR

    protected String _tableAlias; //NOSONAR

    protected String _preselect; //NOSONAR

    protected Collection<String> _propOrColumnNames; //NOSONAR

    protected Map<String, String> _propOrColumnNameAliases; //NOSONAR

    protected List<Selection> _multiSelects; //NOSONAR

    protected Map<String, Map<String, Tuple2<String, Boolean>>> _aliasPropColumnNameMap; //NOSONAR

    protected Map<String, Object> _props; //NOSONAR

    protected Collection<Map<String, Object>> _propsList; //NOSONAR

    protected boolean _hasFromBeenSet = false; //NOSONAR
    protected boolean _isForConditionOnly = false; //NOSONAR

    protected final BiConsumer<StringBuilder, String> _handlerForNamedParameter; //NOSONAR

    protected final Set<String> calledOpSet = new HashSet<>(); //NOSONAR

    /**
     * Constructs a new SQLBuilder with the specified naming policy and SQL policy.
     * 
     * @param namingPolicy the naming policy for column names, defaults to LOWER_CASE_WITH_UNDERSCORE if null
     * @param sqlPolicy the SQL generation policy, defaults to SQL if null
     */
    protected SQLBuilder(final NamingPolicy namingPolicy, final SQLPolicy sqlPolicy) {
        if (activeStringBuilderCounter.incrementAndGet() > 1024) {
            logger.error("Too many(" + activeStringBuilderCounter.get()
                    + ") StringBuilder instances are created in SQLBuilder. The method sql()/pair() must be called to release resources and close SQLBuilder");
        }

        _sb = Objectory.createStringBuilder();

        _namingPolicy = namingPolicy == null ? NamingPolicy.LOWER_CASE_WITH_UNDERSCORE : namingPolicy;
        _sqlPolicy = sqlPolicy == null ? SQLPolicy.SQL : sqlPolicy;

        _handlerForNamedParameter = handlerForNamedParameter_TL.get();
    }

    /**
     * Checks if this SQL builder generates named SQL (with named parameters).
     * 
     * @return true if this builder generates named SQL, false otherwise
     */
    protected boolean isNamedSql() {
        return false;
    }

    /**
     * Gets the table name for the specified entity class based on the naming policy.
     * If the entity class has a @Table annotation with a name attribute, that name is used.
     * Otherwise, the class name is converted according to the naming policy.
     *
     * @param entityClass the entity class
     * @param namingPolicy the naming policy to apply
     * @return the table name
     */
    protected static String getTableName(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        String[] entityTableNames = classTableNameMap.get(entityClass);

        if (entityTableNames == null) {
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);

            if (entityInfo.tableName.isPresent()) {
                entityTableNames = Array.repeat(entityInfo.tableName.get(), 4);
            } else {
                final String simpleClassName = ClassUtil.getSimpleClassName(entityClass);
                entityTableNames = new String[] { ClassUtil.toLowerCaseWithUnderscore(simpleClassName), ClassUtil.toUpperCaseWithUnderscore(simpleClassName),
                        ClassUtil.toCamelCase(simpleClassName), simpleClassName };
            }

            classTableNameMap.put(entityClass, entityTableNames);
        }

        switch (namingPolicy) {
            case LOWER_CASE_WITH_UNDERSCORE:
                return entityTableNames[0];

            case UPPER_CASE_WITH_UNDERSCORE:
                return entityTableNames[1];

            case LOWER_CAMEL_CASE:
                return entityTableNames[2];

            default:
                return entityTableNames[3];
        }
    }

    /**
     * Gets the table alias for the specified entity class.
     * The alias is retrieved from the @Table annotation's alias attribute.
     * 
     * @param entityClass the entity class
     * @return the table alias, or empty string if not defined
     */
    protected static String getTableAlias(final Class<?> entityClass) {
        String alis = classTableAliasMap.get(entityClass);

        if (alis == null) {
            if (entityClass != null && entityClass.getAnnotation(Table.class) != null) {
                alis = entityClass.getAnnotation(Table.class).alias();
            }

            if (alis == null) {
                alis = "";
            }

            classTableAliasMap.put(entityClass, alis);
        }

        return alis;
    }

    /**
     * Gets the table alias if specified, otherwise returns the default table alias for the entity class.
     * 
     * @param alias the specified alias
     * @param entityClass the entity class
     * @return the table alias
     */
    protected static String getTableAlias(final String alias, final Class<?> entityClass) {
        if (Strings.isNotEmpty(alias)) {
            return alias;
        }

        return getTableAlias(entityClass);
    }

    /**
     * Gets the table alias or table name for the specified entity class.
     * 
     * @param entityClass the entity class
     * @param namingPolicy the naming policy to apply
     * @return the table alias if defined, otherwise the table name
     */
    protected static String getTableAliasOrName(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        return getTableAliasOrName(null, entityClass, namingPolicy);
    }

    /**
     * Gets the table alias or table name for the specified entity class.
     * Priority: specified alias > class-defined alias > table name
     * 
     * @param alias the specified alias
     * @param entityClass the entity class
     * @param namingPolicy the naming policy to apply
     * @return the table alias if specified or defined, otherwise the table name
     */
    protected static String getTableAliasOrName(final String alias, final Class<?> entityClass, final NamingPolicy namingPolicy) {
        String tableAliasOrName = alias;

        if (Strings.isEmpty(tableAliasOrName)) {
            tableAliasOrName = getTableAlias(entityClass);
        }

        if (Strings.isEmpty(tableAliasOrName)) {
            tableAliasOrName = getTableName(entityClass, namingPolicy);
        }

        return tableAliasOrName;
    }

    /**
     * Checks if the given property value is a default ID property value.
     * A value is considered default if it's null or a number equal to 0.
     *
     * @param propValue the property value to check
     * @return true if the value is null or a number equal to 0, false otherwise
     */
    @Internal
    protected static boolean isDefaultIdPropValue(final Object propValue) {
        return (propValue == null) || (propValue instanceof Number && (((Number) propValue).longValue() == 0));
    }

    /**
     * Loads property names for the specified entity class, categorized by their usage.
     * Returns an array of 5 sets:
     * <ul>
     *   <li>[0] - All selectable properties including sub-entity properties</li>
     *   <li>[1] - All selectable properties excluding sub-entity properties</li>
     *   <li>[2] - Properties for INSERT operations with ID</li>
     *   <li>[3] - Properties for INSERT operations without ID</li>
     *   <li>[4] - Properties for UPDATE operations</li>
     * </ul>
     *
     * @param entityClass the entity class to analyze
     * @return an array of property name sets categorized by usage
     */
    protected static Set<String>[] loadPropNamesByClass(final Class<?> entityClass) {
        Set<String>[] val = defaultPropNamesPool.get(entityClass);

        if (val == null) {
            synchronized (defaultPropNamesPool) {
                final Set<String> entityPropNames = N.newLinkedHashSet(ClassUtil.getPropNameList(entityClass));
                final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

                if (N.notEmpty(subEntityPropNames)) {
                    entityPropNames.removeAll(subEntityPropNames);
                }

                val = new Set[5];
                val[0] = N.newLinkedHashSet(entityPropNames);
                val[1] = N.newLinkedHashSet(entityPropNames);
                val[2] = N.newLinkedHashSet(entityPropNames);
                val[3] = N.newLinkedHashSet(entityPropNames);
                val[4] = N.newLinkedHashSet(entityPropNames);

                final Table tableAnno = entityClass.getAnnotation(Table.class);
                final Set<String> columnFields = tableAnno == null ? N.emptySet() : N.asSet(tableAnno.columnFields());
                final Set<String> nonColumnFields = tableAnno == null ? N.emptySet() : N.asSet(tableAnno.nonColumnFields());
                final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
                Class<?> subEntityClass = null;
                Set<String> subEntityPropNameList = null;

                for (final String subEntityPropName : subEntityPropNames) {
                    final PropInfo propInfo = entityInfo.getPropInfo(subEntityPropName);
                    subEntityClass = (propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type).clazz();

                    subEntityPropNameList = N.newLinkedHashSet(ClassUtil.getPropNameList(subEntityClass));
                    subEntityPropNameList.removeAll(getSubEntityPropNames(subEntityClass));

                    for (final String pn : subEntityPropNameList) {
                        val[0].add(Strings.concat(subEntityPropName, SK.PERIOD, pn));
                    }
                }

                final Set<String> nonUpdatableNonWritablePropNames = N.newHashSet();
                final Set<String> nonUpdatablePropNames = N.newHashSet();
                final Set<String> transientPropNames = N.newHashSet();

                for (final PropInfo propInfo : entityInfo.propInfoList) {
                    if (propInfo.isAnnotationPresent(ReadOnly.class) || propInfo.isAnnotationPresent(ReadOnlyId.class) || propInfo.isMarkedToReadOnlyId) {
                        nonUpdatableNonWritablePropNames.add(propInfo.name);
                    }

                    if (propInfo.isAnnotationPresent(NonUpdatable.class)) {
                        nonUpdatablePropNames.add(propInfo.name);
                    }

                    if (QueryUtil.isNotColumn(columnFields, nonColumnFields, propInfo)) {
                        nonUpdatableNonWritablePropNames.add(propInfo.name);
                        transientPropNames.add(propInfo.name);
                    }
                }

                nonUpdatablePropNames.addAll(nonUpdatableNonWritablePropNames);

                val[0].removeAll(transientPropNames);
                val[1].removeAll(transientPropNames);
                val[2].removeAll(nonUpdatableNonWritablePropNames);
                val[3].removeAll(nonUpdatableNonWritablePropNames);
                val[4].removeAll(nonUpdatablePropNames);

                for (final String idPropName : QueryUtil.getIdFieldNames(entityClass)) {
                    val[3].remove(idPropName);
                    val[3].remove(ClassUtil.getPropNameByMethod(ClassUtil.getPropGetMethod(entityClass, idPropName)));
                }

                val[0] = ImmutableSet.wrap(val[0]); // for select, including sub entity properties.
                val[1] = ImmutableSet.wrap(val[1]); // for select, no sub entity properties.
                val[2] = ImmutableSet.wrap(val[2]); // for insert with id
                val[3] = ImmutableSet.wrap(val[3]); // for insert without id
                val[4] = ImmutableSet.wrap(val[4]); // for update.

                defaultPropNamesPool.put(entityClass, val);
            }
        }

        return val;
    }

    /**
     * Gets the sub-entity property names for the specified entity class.
     * Sub-entity properties are properties that represent related entities.
     *
     * @param entityClass the entity class
     * @return an immutable set of sub-entity property names
     */
    protected static ImmutableSet<String> getSubEntityPropNames(final Class<?> entityClass) {
        ImmutableSet<String> subEntityPropNames = subEntityPropNamesPool.get(entityClass);
        if (subEntityPropNames == null) {
            synchronized (subEntityPropNamesPool) {
                final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
                final Set<String> subEntityPropNameSet = N.newLinkedHashSet(entityInfo.subEntityPropNameList);
                subEntityPropNames = ImmutableSet.wrap(subEntityPropNameSet);

                subEntityPropNamesPool.put(entityClass, subEntityPropNames);
            }
        }

        return subEntityPropNames;
    }

    protected static List<String> getSelectTableNames(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames,
            final NamingPolicy namingPolicy) {
        final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

        if (N.isEmpty(subEntityPropNames)) {
            return N.emptyList();
        }

        final List<String> res = new ArrayList<>(subEntityPropNames.size() + 1);

        String tableAlias = getTableAlias(alias, entityClass);

        if (Strings.isNotEmpty(tableAlias)) {
            res.add(getTableName(entityClass, namingPolicy));
        } else {
            res.add(getTableName(entityClass, namingPolicy) + " " + tableAlias);
        }

        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
        PropInfo propInfo = null;
        Class<?> subEntityClass = null;

        for (final String subEntityPropName : subEntityPropNames) {
            if (excludedPropNames != null && excludedPropNames.contains(subEntityPropName)) {
                continue;
            }

            propInfo = entityInfo.getPropInfo(subEntityPropName);
            subEntityClass = (propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type).clazz();
            tableAlias = getTableAlias(subEntityClass);

            if (Strings.isEmpty(tableAlias)) {
                res.add(getTableName(subEntityClass, namingPolicy));
            } else {
                res.add(getTableName(subEntityClass, namingPolicy) + " " + tableAlias);
            }
        }

        return res;
    }

    //    /**
    //     * Register the irregular column names which can not be converted from property name by naming policy.
    //     *
    //     * @param propNameTableInterface the interface generated by <code>com.landawn.abacus.util.CodeGenerator</code>
    //     */
    //    public static void registerColumnName(final Class<?> propNameTableInterface) {
    //        final String PCM = "_PCM";
    //
    //        try {
    //            final Map<String, String> _pcm = (Map<String, String>) propNameTableInterface.getField(PCM).get(null);
    //
    //            for (Class<?> cls : propNameTableInterface.getDeclaredClasses()) {
    //                final String entityName = (String) cls.getField(D.UNDERSCORE).get(null);
    //                final Map<String, String> entityPCM = (Map<String, String>) cls.getField(PCM).get(null);
    //
    //                final Map<String, String> propColumnNameMap = new HashMap<>(_pcm);
    //                propColumnNameMap.putAll(entityPCM);
    //
    //                registerColumnName(entityName, propColumnNameMap);
    //            }
    //        } catch (Exception e) {
    //            ExceptionUtil.toRuntimeException(e, true);
    //        }
    //    }

    //    /**
    //     * Returns an immutable list of the property name by the specified entity class.
    //     *
    //     * @param entityClass
    //     * @return
    //     */
    //    public static List<String> propNameList(final Class<?> entityClass) {
    //        List<String> propNameList = classPropNameListPool.get(entityClass);
    //
    //        if (propNameList == null) {
    //            synchronized (classPropNameListPool) {
    //                propNameList = classPropNameListPool.get(entityClass);
    //
    //                if (propNameList == null) {
    //                    propNameList = N.asImmutableList(new ArrayList<>(N.getPropGetMethodList(entityClass).keySet()));
    //                    classPropNameListPool.put(entityClass, propNameList);
    //                }
    //            }
    //        }
    //
    //        return propNameList;
    //    }

    //    /**
    //     * Returns an immutable set of the property name by the specified entity class.
    //     *
    //     * @param entityClass
    //     * @return
    //     */
    //    public static Set<String> propNameSet(final Class<?> entityClass) {
    //        Set<String> propNameSet = classPropNameSetPool.get(entityClass);
    //
    //        if (propNameSet == null) {
    //            synchronized (classPropNameSetPool) {
    //                propNameSet = classPropNameSetPool.get(entityClass);
    //
    //                if (propNameSet == null) {
    //                    propNameSet = N.asImmutableSet(N.newLinkedHashSet(N.getPropGetMethodList(entityClass).keySet()));
    //                    classPropNameSetPool.put(entityClass, propNameSet);
    //                }
    //            }
    //        }
    //
    //        return propNameSet;
    //    }

    /**
     * Creates a map with property names as keys and {@code CF.QME} (question mark expression) as values.
     * <p>This is useful for creating parameterized queries with named parameters.</p>
     * 
     * <pre>{@code
     * Map<String, Expression> params = SQLBuilder.named("firstName", "lastName");
     * // Returns: {"firstName": CF.QME, "lastName": CF.QME}
     * }</pre>
     * 
     * @param propNames the property names
     * @return a map with property names mapped to question mark expressions
     */
    @Beta
    protected static Map<String, Expression> named(final String... propNames) {
        final Map<String, Expression> m = N.newLinkedHashMap(propNames.length);

        for (final String propName : propNames) {
            m.put(propName, CF.QME);
        }

        return m;
    }

    /**
     * Creates a map with property names as keys and {@code CF.QME} (question mark expression) as values.
     * <p>This is useful for creating parameterized queries with named parameters.</p>
     * 
     * <pre>{@code
     * Map<String, Expression> params = SQLBuilder.named(Arrays.asList("firstName", "lastName"));
     * // Returns: {"firstName": CF.QME, "lastName": CF.QME}
     * }</pre>
     * 
     * @param propNames the collection of property names
     * @return a map with property names mapped to question mark expressions
     */
    @Beta
    protected static Map<String, Expression> named(final Collection<String> propNames) {
        final Map<String, Expression> m = N.newLinkedHashMap(propNames.size());

        for (final String propName : propNames) {
            m.put(propName, CF.QME);
        }

        return m;
    }

    /**
     * Specifies the target table for an INSERT operation.
     * <p>Must be called after setting the columns/values to insert.</p>
     * 
     * <pre>{@code
     * String sql = PSC.insert("firstName", "lastName")
     *                 .into("account")
     *                 .sql();
     * // Output: INSERT INTO account (first_name, last_name) VALUES (?, ?)
     * }</pre>
     * 
     * @param tableName the name of the table to insert into
     * @return this SQLBuilder instance for method chaining
     * @throws RuntimeException if called on non-INSERT operation or if columns/values not set
     */
    public SQLBuilder into(final String tableName) {
        if (!(_op == OperationType.ADD || _op == OperationType.QUERY)) {
            throw new RuntimeException("Invalid operation: " + _op);
        }

        if (_op == OperationType.QUERY) {
            if (N.isEmpty(_propOrColumnNames) && N.isEmpty(_propOrColumnNameAliases) && N.isEmpty(_multiSelects)) {
                throw new RuntimeException("Column names or props must be set first by select");
            }
        } else {
            if (N.isEmpty(_propOrColumnNames) && N.isEmpty(_props) && N.isEmpty(_propsList)) {
                throw new RuntimeException("Column names or props must be set first by insert");
            }
        }

        _tableName = tableName;

        _sb.append(_INSERT);
        _sb.append(_SPACE_INTO_SPACE);

        _sb.append(tableName);

        _sb.append(_SPACE);
        _sb.append(SK._PARENTHESES_L);

        if (N.notEmpty(_propOrColumnNames)) {
            int i = 0;
            for (final String columnName : _propOrColumnNames) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);
            }
        } else {
            final Map<String, Object> localProps = N.isEmpty(_props) ? _propsList.iterator().next() : _props;

            int i = 0;
            for (final String columnName : localProps.keySet()) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);
            }
        }

        _sb.append(SK._PARENTHESES_R);

        _sb.append(_SPACE_VALUES_SPACE);

        _sb.append(SK._PARENTHESES_L);

        if (N.notEmpty(_propOrColumnNames)) {
            switch (_sqlPolicy) {
                case SQL:
                case PARAMETERIZED_SQL: {
                    for (int i = 0, size = _propOrColumnNames.size(); i < size; i++) {
                        if (i > 0) {
                            _sb.append(_COMMA_SPACE);
                        }

                        _sb.append(SK._QUESTION_MARK);
                    }

                    break;
                }

                case NAMED_SQL: {
                    int i = 0;
                    for (final String columnName : _propOrColumnNames) {
                        if (i++ > 0) {
                            _sb.append(_COMMA_SPACE);
                        }

                        _handlerForNamedParameter.accept(_sb, columnName);
                    }

                    break;
                }

                case IBATIS_SQL: {
                    int i = 0;
                    for (final String columnName : _propOrColumnNames) {
                        if (i++ > 0) {
                            _sb.append(_COMMA_SPACE);
                        }

                        _sb.append("#{");
                        _sb.append(columnName);
                        _sb.append('}');
                    }

                    break;
                }

                default:
                    throw new RuntimeException("Not supported SQL policy: " + _sqlPolicy); //NOSONAR
            }
        } else if (N.notEmpty(_props)) {
            appendInsertProps(_props);
        } else {
            int i = 0;
            for (final Map<String, Object> localProps : _propsList) {
                if (i++ > 0) {
                    _sb.append(SK._PARENTHESES_R);
                    _sb.append(_COMMA_SPACE);
                    _sb.append(SK._PARENTHESES_L);
                }

                appendInsertProps(localProps);
            }
        }

        _sb.append(SK._PARENTHESES_R);

        return this;
    }

    /**
     * Specifies the target table for an INSERT operation using an entity class.
     * <p>The table name will be derived from the entity class based on the naming policy.</p>
     * 
     * <pre>{@code
     * String sql = PSC.insert(account).into(Account.class).sql();
     * // Table name derived from Account class based on naming policy
     * }</pre>
     * 
     * @param entityClass the entity class representing the target table
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder into(final Class<?> entityClass) {
        if (_entityClass == null) {
            setEntityClass(entityClass);
        }

        return into(getTableName(entityClass, _namingPolicy));
    }

    /**
     * Specifies the target table for an INSERT operation with explicit table name and entity class.
     * 
     * @param tableName the name of the table to insert into
     * @param entityClass the entity class for property mapping (can be null)
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder into(final String tableName, final Class<?> entityClass) {
        if (entityClass != null) {
            setEntityClass(entityClass);
        }

        return into(tableName);
    }

    /**
     * Adds DISTINCT clause to the SELECT statement.
     * <p>This method is equivalent to calling {@code preselect(DISTINCT)}.</p>
     * 
     * <pre>{@code
     * String sql = PSC.select("name").distinct().from("account").sql();
     * // Output: SELECT DISTINCT name FROM account
     * }</pre>
     * 
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder distinct() { //NOSONAR
        return preselect(DISTINCT);
    }

    /**
     * Adds a pre-select modifier to the SELECT statement.
     * <p>For better performance, this method should be called before {@code from}.</p>
     * 
     * <pre>{@code
     * String sql = PSC.select("*").preselect("TOP 10").from("account").sql();
     * // Output: SELECT TOP 10 * FROM account
     * }</pre>
     * 
     * @param preselect modifiers like ALL, DISTINCT, DISTINCTROW, TOP, etc.
     * @return this SQLBuilder instance for method chaining
     * @throws IllegalStateException if preselect has already been set
     */
    public SQLBuilder preselect(final String preselect) {
        if (Strings.isNotEmpty(_preselect)) {
            throw new IllegalStateException("preselect has been set. Can not set it again");
        }

        if (Strings.isNotEmpty(preselect)) {
            _preselect = preselect;

            final int selectIdx = _sb.indexOf(SK.SELECT);

            if (selectIdx >= 0) {
                final int len = _sb.length();

                _sb.append(_SPACE);

                appendStringExpr(_preselect, false);

                final int newLength = _sb.length();

                _sb.insert(selectIdx + SK.SELECT.length(), _sb.substring(len));
                _sb.setLength(newLength);
            }
        }

        return this;
    }

    /**
     * Sets the FROM clause with multiple table names.
     * 
     * <pre>{@code
     * String sql = PSC.select("*").from("users", "orders").sql();
     * // Output: SELECT * FROM users, orders
     * }</pre>
     * 
     * @param tableNames the table names to use in the FROM clause
     * @return this SQLBuilder instance for method chaining
     */
    public final SQLBuilder from(final String... tableNames) {
        if (tableNames.length == 1) {
            return from(tableNames[0].trim());
        }

        final String localTableName = tableNames[0].trim();
        return from(localTableName, Strings.join(tableNames, SK.COMMA_SPACE));
    }

    /**
     * Sets the FROM clause with a collection of table names.
     * 
     * <pre>{@code
     * List<String> tables = Arrays.asList("users", "orders");
     * String sql = PSC.select("*").from(tables).sql();
     * // Output: SELECT * FROM users, orders
     * }</pre>
     * 
     * @param tableNames the collection of table names to use in the FROM clause
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder from(final Collection<String> tableNames) {
        if (tableNames.size() == 1) {
            return from(tableNames.iterator().next().trim());
        }

        final String localTableName = tableNames.iterator().next().trim();
        return from(localTableName, Strings.join(tableNames, SK.COMMA_SPACE));
    }

    /**
     * Sets the FROM clause with a single expression.
     * <p>The expression can be a table name, subquery, or multiple tables separated by comma.</p>
     * 
     * <pre>{@code
     * String sql = PSC.select("*").from("users u").sql();
     * // Output: SELECT * FROM users u
     * 
     * String sql2 = PSC.select("*").from("(SELECT * FROM users) t").sql();
     * // Output: SELECT * FROM (SELECT * FROM users) t
     * }</pre>
     * 
     * @param expr the FROM clause expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder from(String expr) {
        expr = expr.trim();

        final int idx = expr.indexOf(SK._COMMA);
        final String localTableName = idx > 0 ? expr.substring(0, idx) : expr;

        return from(localTableName.trim(), expr);
    }

    /**
     * Sets the FROM clause with an expression and associates it with an entity class.
     * 
     * <pre>{@code
     * String sql = PSC.select("*").from("users u", User.class).sql();
     * // Associates the User class for property mapping
     * }</pre>
     * 
     * @param expr the FROM clause expression
     * @param entityClass the entity class for property mapping
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder from(final String expr, final Class<?> entityClass) {
        if (entityClass != null) {
            setEntityClass(entityClass);
        }

        return from(expr);
    }

    /**
     * Sets the FROM clause using an entity class.
     * <p>The table name will be derived from the entity class.</p>
     * 
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).sql();
     * // Table name derived from User class based on naming policy
     * }</pre>
     * 
     * @param entityClass the entity class representing the table
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder from(final Class<?> entityClass) {
        return from(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Sets the FROM clause using an entity class with an alias.
     * 
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").sql();
     * // Output: SELECT * FROM users u (table name based on naming policy)
     * }</pre>
     * 
     * @param entityClass the entity class representing the table
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder from(final Class<?> entityClass, final String alias) {
        if (_entityClass == null) {
            setEntityClass(entityClass);
        }

        if (Strings.isEmpty(alias)) {
            return from(getTableName(entityClass, _namingPolicy));
        } else {
            return from(getTableName(entityClass, _namingPolicy) + " " + alias);
        }
    }

    protected SQLBuilder from(final Class<?> entityClass, final Collection<String> tableNames) {
        if (_entityClass == null) {
            setEntityClass(entityClass);
        }

        return from(tableNames);
    }

    /**
     *
     * @param tableName
     * @param fromCause
     * @return
     */
    protected SQLBuilder from(final String tableName, final String fromCause) {
        if (_op != OperationType.QUERY) {
            throw new RuntimeException("Invalid operation: " + _op);
        }

        _hasFromBeenSet = true;

        if (N.isEmpty(_propOrColumnNames) && N.isEmpty(_propOrColumnNameAliases) && N.isEmpty(_multiSelects)) {
            throw new RuntimeException("Column names or props must be set first by select");
        }

        final int idx = tableName.indexOf(' ');

        if (idx > 0) {
            _tableName = tableName.substring(0, idx).trim();
            _tableAlias = tableName.substring(idx + 1).trim();
        } else {
            _tableName = tableName.trim();
        }

        if (_entityClass != null && Strings.isNotEmpty(_tableAlias)) {
            addPropColumnMapForAlias(_entityClass, _tableAlias);
        }

        _sb.append(_SELECT);
        _sb.append(_SPACE);

        if (Strings.isNotEmpty(_preselect)) {
            appendStringExpr(_preselect, false);

            _sb.append(_SPACE);
        }

        final boolean withAlias = Strings.isNotEmpty(_tableAlias);
        final boolean isForSelect = _op == OperationType.QUERY;

        if (N.notEmpty(_propOrColumnNames)) {
            if (_entityClass != null && !withAlias && _propOrColumnNames == QueryUtil.getSelectPropNames(_entityClass, false, null)) { // NOSONAR
                String fullSelectParts = fullSelectPartsPool.get(_namingPolicy).get(_entityClass);

                if (Strings.isEmpty(fullSelectParts)) {
                    final StringBuilder sb = new StringBuilder();

                    int i = 0;
                    for (final String columnName : _propOrColumnNames) {
                        if (i++ > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }

                        sb.append(formalizeColumnName(_propColumnNameMap, columnName));

                        if ((_namingPolicy != NamingPolicy.LOWER_CAMEL_CASE && _namingPolicy != NamingPolicy.NO_CHANGE) && !SK.ASTERISK.equals(columnName)) {
                            sb.append(SPACE_AS_SPACE).append(SK.QUOTATION_D).append(columnName).append(SK.QUOTATION_D);
                        }
                    }

                    fullSelectParts = sb.toString();

                    fullSelectPartsPool.get(_namingPolicy).put(_entityClass, fullSelectParts);
                }

                _sb.append(fullSelectParts);
            } else {
                int i = 0;
                for (final String columnName : _propOrColumnNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(_entityClass, _entityInfo, _propColumnNameMap, _tableAlias, columnName, null, false, null, isForSelect, true);
                }
            }
        } else if (N.notEmpty(_propOrColumnNameAliases)) {
            int i = 0;
            for (final Map.Entry<String, String> entry : _propOrColumnNameAliases.entrySet()) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(_entityClass, _entityInfo, _propColumnNameMap, _tableAlias, entry.getKey(), entry.getValue(), false, null, isForSelect, true);
            }
        } else if (N.notEmpty(_multiSelects)) {
            _aliasPropColumnNameMap = N.newHashMap(_multiSelects.size());

            for (final Selection selection : _multiSelects) {
                if (Strings.isNotEmpty(selection.tableAlias())) {
                    _aliasPropColumnNameMap.put(selection.tableAlias(), prop2ColumnNameMap(selection.entityClass(), _namingPolicy));
                }
            }

            Class<?> selectionEntityClass = null;
            BeanInfo selectionBeanInfo = null;
            ImmutableMap<String, Tuple2<String, Boolean>> selectionPropColumnNameMap = null;
            String selectionTableAlias = null;
            String selectionClassAlias = null;
            boolean selectionWithClassAlias = false;

            int i = 0;

            for (final Selection selection : _multiSelects) {
                selectionEntityClass = selection.entityClass();
                selectionBeanInfo = ClassUtil.isBeanClass(selectionEntityClass) ? ParserUtil.getBeanInfo(selectionEntityClass) : null;
                selectionPropColumnNameMap = ClassUtil.isBeanClass(selectionEntityClass) ? prop2ColumnNameMap(selectionEntityClass, _namingPolicy) : null;
                selectionTableAlias = selection.tableAlias();

                selectionClassAlias = selection.classAlias();
                selectionWithClassAlias = Strings.isNotEmpty(selectionClassAlias);

                final Collection<String> selectPropNames = N.notEmpty(selection.selectPropNames()) ? selection.selectPropNames()
                        : QueryUtil.getSelectPropNames(selectionEntityClass, selection.includeSubEntityProperties(), selection.excludedPropNames());

                for (final String propName : selectPropNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(selectionEntityClass, selectionBeanInfo, selectionPropColumnNameMap, selectionTableAlias, propName, null,
                            selectionWithClassAlias, selectionClassAlias, isForSelect, true);
                }
            }
        } else {
            throw new UnsupportedOperationException("No select part specified");
        }

        _sb.append(_SPACE_FROM_SPACE);

        _sb.append(fromCause);

        return this;
    }

    protected void addPropColumnMapForAlias(final Class<?> entityClass, final String alias) {
        if (_aliasPropColumnNameMap == null) {
            _aliasPropColumnNameMap = new HashMap<>();
        }

        if (N.isEmpty(_propColumnNameMap) && ClassUtil.isBeanClass(entityClass)) {
            _propColumnNameMap = prop2ColumnNameMap(entityClass, _namingPolicy);
        }

        _aliasPropColumnNameMap.put(alias, _propColumnNameMap);
    }

    /**
     * Adds a JOIN clause to the SQL statement.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o ON u.id = o.user_id")
     *                 .sql();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression (e.g., "orders o ON u.id = o.user_id")
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder join(final String expr) {
        _sb.append(_SPACE_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     * Adds a JOIN clause with a table name and entity class.
     * 
     * @param tableName the table name to join
     * @param entityClass the entity class (currently unused but reserved for future use)
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder join(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass) {
        return join(tableName);
    }

    /**
     * Adds a JOIN clause using an entity class.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from(User.class)
     *                 .join(Order.class)
     *                 .on("users.id = orders.user_id")
     *                 .sql();
     * }</pre>
     * 
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder join(final Class<?> entityClass) {
        return join(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a JOIN clause using an entity class with an alias.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from(User.class, "u")
     *                 .join(Order.class, "o")
     *                 .on("u.id = o.user_id")
     *                 .sql();
     * }</pre>
     * 
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder join(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     * Adds an INNER JOIN clause to the SQL statement.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .innerJoin("orders o ON u.id = o.user_id")
     *                 .sql();
     * // Output: SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder innerJoin(final String expr) {
        _sb.append(_SPACE_INNER_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     * Adds an INNER JOIN clause with a table name and entity class.
     * 
     * @param tableName the table name to join
     * @param entityClass the entity class (currently unused but reserved for future use)
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder innerJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass) {
        return innerJoin(tableName);
    }

    /**
     * Adds an INNER JOIN clause using an entity class.
     * 
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder innerJoin(final Class<?> entityClass) {
        return innerJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds an INNER JOIN clause using an entity class with an alias.
     * 
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder innerJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_INNER_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     * Adds a LEFT JOIN clause to the SQL statement.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .leftJoin("orders o ON u.id = o.user_id")
     *                 .sql();
     * // Output: SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder leftJoin(final String expr) {
        _sb.append(_SPACE_LEFT_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     * Adds a LEFT JOIN clause with a table name and entity class.
     * 
     * @param tableName the table name to join
     * @param entityClass the entity class (currently unused but reserved for future use)
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder leftJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass) {
        return leftJoin(tableName);
    }

    /**
     * Adds a LEFT JOIN clause using an entity class.
     * 
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder leftJoin(final Class<?> entityClass) {
        return leftJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a LEFT JOIN clause using an entity class with an alias.
     * 
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder leftJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_LEFT_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     * Adds a RIGHT JOIN clause to the SQL statement.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .rightJoin("orders o ON u.id = o.user_id")
     *                 .sql();
     * // Output: SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder rightJoin(final String expr) {
        _sb.append(_SPACE_RIGHT_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     * Adds a RIGHT JOIN clause with a table name and entity class.
     * 
     * @param tableName the table name to join
     * @param entityClass the entity class (currently unused but reserved for future use)
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder rightJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass) {
        return rightJoin(tableName);
    }

    /**
     * Adds a RIGHT JOIN clause using an entity class.
     * 
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder rightJoin(final Class<?> entityClass) {
        return rightJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a RIGHT JOIN clause using an entity class with an alias.
     * 
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder rightJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_RIGHT_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     * Adds a FULL JOIN clause to the SQL statement.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .fullJoin("orders o ON u.id = o.user_id")
     *                 .sql();
     * // Output: SELECT * FROM users u FULL JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder fullJoin(final String expr) {
        _sb.append(_SPACE_FULL_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     * Adds a FULL JOIN clause with a table name and entity class.
     * 
     * @param tableName the table name to join
     * @param entityClass the entity class (currently unused but reserved for future use)
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder fullJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass) {
        return fullJoin(tableName);
    }

    /**
     * Adds a FULL JOIN clause using an entity class.
     * 
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder fullJoin(final Class<?> entityClass) {
        return fullJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a FULL JOIN clause using an entity class with an alias.
     * 
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder fullJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_FULL_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     * Adds a CROSS JOIN clause to the SQL statement.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .crossJoin("orders")
     *                 .sql();
     * // Output: SELECT * FROM users CROSS JOIN orders
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder crossJoin(final String expr) {
        _sb.append(_SPACE_CROSS_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     * Adds a CROSS JOIN clause with a table name and entity class.
     * 
     * @param tableName the table name to join
     * @param entityClass the entity class (currently unused but reserved for future use)
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder crossJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass) {
        return crossJoin(tableName);
    }

    /**
     * Adds a CROSS JOIN clause using an entity class.
     * 
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder crossJoin(final Class<?> entityClass) {
        return crossJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a CROSS JOIN clause using an entity class with an alias.
     * 
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder crossJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_CROSS_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     * Adds a NATURAL JOIN clause to the SQL statement.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .naturalJoin("orders")
     *                 .sql();
     * // Output: SELECT * FROM users NATURAL JOIN orders
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder naturalJoin(final String expr) {
        _sb.append(_SPACE_NATURAL_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     * Adds a NATURAL JOIN clause with a table name and entity class.
     * 
     * @param tableName the table name to join
     * @param entityClass the entity class (currently unused but reserved for future use)
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder naturalJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass) {
        return naturalJoin(tableName);
    }

    /**
     * Adds a NATURAL JOIN clause using an entity class.
     * 
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder naturalJoin(final Class<?> entityClass) {
        return naturalJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a NATURAL JOIN clause using an entity class with an alias.
     * 
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder naturalJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_NATURAL_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     * Adds an ON clause for join conditions.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o")
     *                 .on("u.id = o.user_id")
     *                 .sql();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join condition expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder on(final String expr) {
        _sb.append(_SPACE_ON_SPACE);

        appendStringExpr(expr, false);

        return this;
    }

    /**
     * Adds an ON clause with a condition object for join conditions.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o")
     *                 .on(CF.eq("u.id", "o.user_id"))
     *                 .sql();
     * }</pre>
     * 
     * @param cond the join condition
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder on(final Condition cond) {
        _sb.append(_SPACE_ON_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     * Adds a USING clause for join conditions.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .join("orders")
     *                 .using("user_id")
     *                 .sql();
     * // Output: SELECT * FROM users JOIN orders USING (user_id)
     * }</pre>
     * 
     * @param expr the column name(s) for the USING clause
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder using(final String expr) {
        _sb.append(_SPACE_USING_SPACE);

        appendColumnName(expr);

        return this;
    }

    /**
     * Adds a WHERE clause with a string expression.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where("age > 18")
     *                 .sql();
     * // Output: SELECT * FROM users WHERE age > 18
     * }</pre>
     * 
     * @param expr the WHERE condition expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder where(final String expr) {
        checkIfAlreadyCalled(SK.WHERE);

        init(true);

        _sb.append(_SPACE_WHERE_SPACE);

        appendStringExpr(expr, false);

        return this;
    }

    /**
     * Adds a WHERE clause with a condition object.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where(CF.gt("age", 18))
     *                 .sql();
     * // Output: SELECT * FROM users WHERE age > ?
     * }</pre>
     * 
     * @param cond the WHERE condition
     * @return this SQLBuilder instance for method chaining
     * @see ConditionFactory
     * @see ConditionFactory.CF
     */
    public SQLBuilder where(final Condition cond) {
        checkIfAlreadyCalled(SK.WHERE);

        init(true);

        _sb.append(_SPACE_WHERE_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     * Adds a GROUP BY clause with a single column.
     * 
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy("category")
     *                 .sql();
     * // Output: SELECT category, COUNT(*) FROM products GROUP BY category
     * }</pre>
     * 
     * @param expr the column to group by
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder groupBy(final String expr) {
        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        appendColumnName(expr);

        return this;
    }

    /**
     * Adds a GROUP BY clause with multiple columns.
     * 
     * <pre>{@code
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy("category", "brand")
     *                 .sql();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand
     * }</pre>
     * 
     * @param propOrColumnNames the columns to group by
     * @return this SQLBuilder instance for method chaining
     */
    public final SQLBuilder groupBy(final String... propOrColumnNames) {
        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        for (int i = 0, len = propOrColumnNames.length; i < len; i++) {
            if (i > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(propOrColumnNames[i]);
        }

        return this;
    }

    /**
     * Adds a GROUP BY clause with a single column and sort direction.
     * 
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy("category", SortDirection.DESC)
     *                 .sql();
     * // Output: SELECT category, COUNT(*) FROM products GROUP BY category DESC
     * }</pre>
     * 
     * @param columnName the column to group by
     * @param direction the sort direction
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder groupBy(final String columnName, final SortDirection direction) {
        groupBy(columnName);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return this;
    }

    /**
     * Adds a GROUP BY clause with a collection of columns.
     * 
     * <pre>{@code
     * List<String> columns = Arrays.asList("category", "brand");
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy(columns)
     *                 .sql();
     * }</pre>
     * 
     * @param propOrColumnNames the collection of columns to group by
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder groupBy(final Collection<String> propOrColumnNames) {
        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        int i = 0;
        for (final String columnName : propOrColumnNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(columnName);
        }

        return this;
    }

    /**
     * Adds a GROUP BY clause with a collection of columns and sort direction.
     * 
     * @param propOrColumnNames the collection of columns to group by
     * @param direction the sort direction for all columns
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder groupBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        groupBy(propOrColumnNames);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return this;
    }

    /**
     * Adds a GROUP BY clause with columns and individual sort directions.
     * 
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("category", SortDirection.ASC);
     * orders.put("brand", SortDirection.DESC);
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy(orders)
     *                 .sql();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand DESC
     * }</pre>
     * 
     * @param orders map of columns to their sort directions
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder groupBy(final Map<String, SortDirection> orders) {
        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        int i = 0;
        for (final Map.Entry<String, SortDirection> entry : orders.entrySet()) {
            if (i++ > 0) {

                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(entry.getKey());

            _sb.append(_SPACE);
            _sb.append(entry.getValue().toString());
        }

        return this;
    }

    /**
     * Adds a HAVING clause with a string expression.
     * 
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*) as count")
     *                 .from("products")
     *                 .groupBy("category")
     *                 .having("COUNT(*) > 10")
     *                 .sql();
     * // Output: SELECT category, COUNT(*) as count FROM products GROUP BY category HAVING COUNT(*) > 10
     * }</pre>
     * 
     * @param expr the HAVING condition expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder having(final String expr) {
        checkIfAlreadyCalled(SK.HAVING);

        _sb.append(_SPACE_HAVING_SPACE);

        appendStringExpr(expr, false);

        return this;
    }

    /**
     * Adds a HAVING clause with a condition object.
     * 
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*) as count")
     *                 .from("products")
     *                 .groupBy("category")
     *                 .having(CF.gt("COUNT(*)", 10))
     *                 .sql();
     * // Output: SELECT category, COUNT(*) as count FROM products GROUP BY category HAVING COUNT(*) > ?
     * }</pre>
     * 
     * @param cond the HAVING condition
     * @return this SQLBuilder instance for method chaining
     * @see ConditionFactory
     * @see ConditionFactory.CF
     */
    public SQLBuilder having(final Condition cond) {
        checkIfAlreadyCalled(SK.HAVING);

        _sb.append(_SPACE_HAVING_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     * Adds an ORDER BY clause with a single column.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("name")
     *                 .sql();
     * // Output: SELECT * FROM users ORDER BY name
     * }</pre>
     * 
     * @param expr the column to order by
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder orderBy(final String expr) {
        checkIfAlreadyCalled(SK.ORDER_BY);

        _sb.append(_SPACE_ORDER_BY_SPACE);

        appendColumnName(expr);

        return this;
    }

    /**
     * Adds an ORDER BY clause with multiple columns.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("lastName", "firstName")
     *                 .sql();
     * // Output: SELECT * FROM users ORDER BY last_name, first_name
     * }</pre>
     * 
     * @param propOrColumnNames the columns to order by
     * @return this SQLBuilder instance for method chaining
     */
    public final SQLBuilder orderBy(final String... propOrColumnNames) {
        checkIfAlreadyCalled(SK.ORDER_BY);

        _sb.append(_SPACE_ORDER_BY_SPACE);

        for (int i = 0, len = propOrColumnNames.length; i < len; i++) {
            if (i > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(propOrColumnNames[i]);
        }

        return this;
    }

    /**
     * Adds an ORDER BY clause with a single column and sort direction.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("name", SortDirection.DESC)
     *                 .sql();
     * // Output: SELECT * FROM users ORDER BY name DESC
     * }</pre>
     * 
     * @param columnName the column to order by
     * @param direction the sort direction
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder orderBy(final String columnName, final SortDirection direction) {
        orderBy(columnName);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return this;
    }

    /**
     * Adds an ORDER BY clause with a collection of columns.
     * 
     * @param propOrColumnNames the collection of columns to order by
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder orderBy(final Collection<String> propOrColumnNames) {
        checkIfAlreadyCalled(SK.ORDER_BY);

        _sb.append(_SPACE_ORDER_BY_SPACE);

        int i = 0;
        for (final String columnName : propOrColumnNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(columnName);
        }

        return this;
    }

    /**
     * Adds an ORDER BY clause with a collection of columns and sort direction.
     * 
     * @param propOrColumnNames the collection of columns to order by
     * @param direction the sort direction for all columns
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder orderBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        orderBy(propOrColumnNames);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return this;
    }

    /**
     * Adds an ORDER BY clause with columns and individual sort directions.
     * 
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("lastName", SortDirection.ASC);
     * orders.put("firstName", SortDirection.DESC);
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy(orders)
     *                 .sql();
     * // Output: SELECT * FROM users ORDER BY last_name ASC, first_name DESC
     * }</pre>
     * 
     * @param orders map of columns to their sort directions
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder orderBy(final Map<String, SortDirection> orders) {
        checkIfAlreadyCalled(SK.ORDER_BY);

        _sb.append(_SPACE_ORDER_BY_SPACE);

        int i = 0;

        for (final Map.Entry<String, SortDirection> entry : orders.entrySet()) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(entry.getKey());

            _sb.append(_SPACE);
            _sb.append(entry.getValue().toString());
        }

        return this;
    }

    /**
     * Adds an ORDER BY ASC clause with a single column.
     * Convenience method equivalent to {@code orderBy(expr, SortDirection.ASC)}.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByAsc("name")
     *                 .sql();
     * // Output: SELECT * FROM users ORDER BY name ASC
     * }</pre>
     * 
     * @param expr the column to order by ascending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder orderByAsc(final String expr) {
        return orderBy(expr, SortDirection.ASC);
    }

    /**
     * Adds an ORDER BY ASC clause with multiple columns.
     * 
     * @param propOrColumnNames the columns to order by ascending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public final SQLBuilder orderByAsc(final String... propOrColumnNames) {
        return orderBy(N.asList(propOrColumnNames), SortDirection.ASC);
    }

    /**
     * Adds an ORDER BY ASC clause with a collection of columns.
     * 
     * @param propOrColumnNames the collection of columns to order by ascending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public final SQLBuilder orderByAsc(final Collection<String> propOrColumnNames) {
        return orderBy(propOrColumnNames, SortDirection.ASC);
    }

    /**
     * Adds an ORDER BY DESC clause with a single column.
     * Convenience method equivalent to {@code orderBy(expr, SortDirection.DESC)}.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByDesc("createdDate")
     *                 .sql();
     * // Output: SELECT * FROM users ORDER BY created_date DESC
     * }</pre>
     * 
     * @param expr the column to order by descending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder orderByDesc(final String expr) {
        return orderBy(expr, SortDirection.DESC);
    }

    /**
     * Adds an ORDER BY DESC clause with multiple columns.
     * 
     * @param propOrColumnNames the columns to order by descending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public final SQLBuilder orderByDesc(final String... propOrColumnNames) {
        return orderBy(N.asList(propOrColumnNames), SortDirection.DESC);
    }

    /**
     * Adds an ORDER BY DESC clause with a collection of columns.
     * 
     * @param propOrColumnNames the collection of columns to order by descending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public final SQLBuilder orderByDesc(final Collection<String> propOrColumnNames) {
        return orderBy(propOrColumnNames, SortDirection.DESC);
    }

    /**
     * Adds a LIMIT clause to restrict the number of rows returned.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .limit(10)
     *                 .sql();
     * // Output: SELECT * FROM users LIMIT 10
     * }</pre>
     * 
     * @param count the maximum number of rows to return
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder limit(final int count) {
        checkIfAlreadyCalled(SK.LIMIT);

        _sb.append(_SPACE_LIMIT_SPACE);

        _sb.append(count);

        return this;
    }

    /**
     * Adds a LIMIT clause with an offset for pagination.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .limit(20, 10)  // offset 20, limit 10
     *                 .sql();
     * // Output: SELECT * FROM users LIMIT 10 OFFSET 20
     * }</pre>
     * 
     * @param offset the number of rows to skip
     * @param count the maximum number of rows to return
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder limit(final int offset, final int count) {
        checkIfAlreadyCalled(SK.LIMIT);

        _sb.append(_SPACE_LIMIT_SPACE);

        _sb.append(count);

        _sb.append(_SPACE_OFFSET_SPACE);

        _sb.append(offset);

        return this;
    }

    /**
     * Adds an OFFSET clause to skip a number of rows.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .limit(10)
     *                 .offset(20)
     *                 .sql();
     * // Output: SELECT * FROM users LIMIT 10 OFFSET 20
     * }</pre>
     * 
     * @param offset the number of rows to skip
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder offset(final int offset) {
        checkIfAlreadyCalled(SK.OFFSET);

        _sb.append(_SPACE_OFFSET_SPACE).append(offset);

        return this;
    }

    /**
     * Adds an OFFSET ROWS clause (SQL Server syntax).
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("id")
     *                 .offsetRows(20)
     *                 .fetchNextNRowsOnly(10)
     *                 .sql();
     * // Output: SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
     * }</pre>
     * 
     * @param offset the number of rows to skip
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder offsetRows(final int offset) {
        checkIfAlreadyCalled(SK.OFFSET);

        _sb.append(_SPACE_OFFSET_SPACE).append(offset).append(_SPACE_ROWS);

        return this;
    }

    /**
     * Adds a FETCH NEXT N ROWS ONLY clause (SQL Server syntax).
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("id")
     *                 .offsetRows(0)
     *                 .fetchNextNRowsOnly(10)
     *                 .sql();
     * // Output: SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
     * }</pre>
     * 
     * @param n the number of rows to fetch
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder fetchNextNRowsOnly(final int n) {
        checkIfAlreadyCalled(SK.FETCH_NEXT);

        _sb.append(" FETCH NEXT ").append(n).append(" ROWS ONLY");

        return this;
    }

    /**
     * Adds a FETCH FIRST N ROWS ONLY clause (SQL standard syntax).
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("id")
     *                 .fetchFirstNRowsOnly(10)
     *                 .sql();
     * // Output: SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY
     * }</pre>
     * 
     * @param n the number of rows to fetch
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder fetchFirstNRowsOnly(final int n) {
        checkIfAlreadyCalled(SK.FETCH_FIRST);

        _sb.append(" FETCH FIRST ").append(n).append(" ROWS ONLY");

        return this;
    }

    protected void checkIfAlreadyCalled(final String op) {
        if (!calledOpSet.add(op)) {
            throw new IllegalStateException("'" + op + "' has already been set. Can not set it again.");
        }
    }

    /**
     * Appends a {@code Criteria} or {@code Where} condition to the SQL statement.
     * Automatically adds WHERE clause if not already present.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .append(CF.and(CF.gt("age", 18), CF.lt("age", 65)))
     *                 .sql();
     * // Output: SELECT * FROM users WHERE ((age > ?) AND (age < ?))
     * }</pre>
     * 
     * @param cond the condition to append
     * @return this SQLBuilder instance for method chaining
     * @see ConditionFactory
     * @see ConditionFactory.CF
     */
    @Beta
    SQLBuilder append(final Condition cond) {
        init(true);

        if (cond instanceof final Criteria criteria) {
            final Collection<Join> joins = criteria.getJoins();

            // appendPreselect(criteria.distinct());

            if (N.notEmpty(joins)) {
                for (final Join join : joins) {
                    _sb.append(_SPACE).append(join.getOperator()).append(_SPACE);

                    if (join.getJoinEntities().size() == 1) {
                        _sb.append(join.getJoinEntities().get(0));
                    } else {
                        _sb.append(SK._PARENTHESES_L);
                        int idx = 0;

                        for (final String joinTableName : join.getJoinEntities()) {
                            if (idx++ > 0) {
                                _sb.append(_COMMA_SPACE);
                            }

                            _sb.append(joinTableName);
                        }

                        _sb.append(SK._PARENTHESES_R);
                    }

                    appendCondition(join.getCondition());
                }
            }

            final Cell where = criteria.getWhere();

            if ((where != null)) {
                _sb.append(_SPACE_WHERE_SPACE);
                appendCondition(where.getCondition());
            }

            final Cell groupBy = criteria.getGroupBy();

            if (groupBy != null) {
                _sb.append(_SPACE_GROUP_BY_SPACE);
                appendCondition(groupBy.getCondition());
            }

            final Cell having = criteria.getHaving();

            if (having != null) {
                _sb.append(_SPACE_HAVING_SPACE);
                appendCondition(having.getCondition());
            }

            final List<Cell> aggregations = criteria.getAggregation();

            if (N.notEmpty(aggregations)) {
                for (final Cell aggregation : aggregations) {
                    _sb.append(_SPACE).append(aggregation.getOperator()).append(_SPACE);
                    appendCondition(aggregation.getCondition());
                }
            }

            final Cell orderBy = criteria.getOrderBy();

            if (orderBy != null) {
                _sb.append(_SPACE_ORDER_BY_SPACE);
                appendCondition(orderBy.getCondition());
            }

            final Limit limit = criteria.getLimit();

            if (limit != null) {
                if (Strings.isNotEmpty(limit.getExpr())) {
                    _sb.append(_SPACE).append(limit.getExpr());
                } else if (limit.getOffset() > 0) {
                    limit(limit.getOffset(), limit.getCount());
                } else {
                    limit(limit.getCount());
                }
            }
        } else if (cond instanceof Clause) {
            _sb.append(_SPACE).append(cond.getOperator()).append(_SPACE);
            appendCondition(((Clause) cond).getCondition());
        } else {
            if (!_isForConditionOnly) {
                _sb.append(_SPACE_WHERE_SPACE);
            }

            appendCondition(cond);
        }

        return this;
    }

    /**
     * Appends a string expression to the SQL statement.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .append(" FOR UPDATE")
     *                 .sql();
     * // Output: SELECT * FROM users FOR UPDATE
     * }</pre>
     * 
     * @param expr the expression to append
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder append(final String expr) {
        _sb.append(expr);

        return this;
    }

    //    /**
    //     * Conditionally appends a condition to the SQL statement.
    //     * 
    //     * <pre>{@code
    //     * boolean includeAgeFilter = true;
    //     * String sql = PSC.select("*")
    //     *                 .from("users")
    //     *                 .appendIf(includeAgeFilter, CF.gt("age", 18))
    //     *                 .sql();
    //     * // Output: SELECT * FROM users WHERE age > ?
    //     * }</pre>
    //     * 
    //     * @param b if true, the condition will be appended
    //     * @param cond the condition to append
    //     * @return this SQLBuilder instance for method chaining
    //     */
    //    @Beta
    //    public SQLBuilder appendIf(final boolean b, final Condition cond) {
    //        if (b) {
    //            append(cond);
    //        }
    //
    //        return this;
    //    }

    /**
     * Conditionally appends a string expression to the SQL statement.
     * 
     * @param b if true, the expression will be appended
     * @param expr the expression to append
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder appendIf(final boolean b, final String expr) {
        if (b) {
            append(expr);
        }

        return this;
    }

    /**
     * Conditionally executes an append operation using a consumer function.
     * 
     * <pre>{@code
     * boolean complexFilter = true;
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .appendIf(complexFilter, builder -> 
     *                     builder.where(CF.gt("age", 18))
     *                            .orderBy("name"))
     *                 .sql();
     * }</pre>
     * 
     * @param b if true, the consumer will be executed
     * @param append the consumer function to execute
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder appendIf(final boolean b, final java.util.function.Consumer<SQLBuilder> append) {
        if (b) {
            append.accept(this);
        }

        return this;
    }

    //    /**
    //     * Conditionally appends one of two conditions based on a boolean value.
    //     * 
    //     * <pre>{@code
    //     * boolean isActive = true;
    //     * String sql = PSC.select("*")
    //     *                 .from("users")
    //     *                 .appendIfOrElse(isActive, 
    //     *                     CF.eq("status", "active"),
    //     *                     CF.eq("status", "inactive"))
    //     *                 .sql();
    //     * // Output: SELECT * FROM users WHERE status = ?
    //     * }</pre>
    //     * 
    //     * @param b if true, append condToAppendForTrue; otherwise append condToAppendForFalse
    //     * @param condToAppendForTrue the condition to append if b is true
    //     * @param condToAppendForFalse the condition to append if b is false
    //     * @return this SQLBuilder instance for method chaining
    //     */
    //    @Beta
    //    public SQLBuilder appendIfOrElse(final boolean b, final Condition condToAppendForTrue, final Condition condToAppendForFalse) {
    //        if (b) {
    //            append(condToAppendForTrue);
    //        } else {
    //            append(condToAppendForFalse);
    //        }
    //
    //        return this;
    //    }

    /**
     * Conditionally appends one of two string expressions based on a boolean value.
     * 
     * @param b if true, append exprToAppendForTrue; otherwise append exprToAppendForFalse
     * @param exprToAppendForTrue the expression to append if b is true
     * @param exprToAppendForFalse the expression to append if b is false
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder appendIfOrElse(final boolean b, final String exprToAppendForTrue, final String exprToAppendForFalse) {
        if (b) {
            append(exprToAppendForTrue);
        } else {
            append(exprToAppendForFalse);
        }

        return this;
    }

    /**
     * Adds a UNION clause with another SQL query.
     * 
     * <pre>{@code
     * SQLBuilder query1 = PSC.select("id", "name").from("users");
     * SQLBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.union(query2).sql();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     * 
     * @param sqlBuilder the SQL builder containing the query to union
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder union(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return union(sql);
    }

    /**
     * Adds a UNION clause with a SQL query string.
     * 
     * @param query the SQL query to union
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder union(final String query) {
        return union(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for UNION operation.
     * 
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .union("id", "name")
     *                 .from("customers")
     *                 .sql();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     * 
     * @param propOrColumnNames the columns for the union query
     * @return this SQLBuilder instance for method chaining
     */
    public final SQLBuilder union(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_UNION_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     * Starts a new SELECT query for UNION operation with a collection of columns.
     * 
     * @param propOrColumnNames the collection of columns for the union query
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder union(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_UNION_SPACE);

        return this;
    }

    /**
     * Adds a UNION ALL clause with another SQL query.
     * 
     * <pre>{@code
     * SQLBuilder query1 = PSC.select("id", "name").from("users");
     * SQLBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.unionAll(query2).sql();
     * // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
     * }</pre>
     * 
     * @param sqlBuilder the SQL builder containing the query to union all
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder unionAll(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return unionAll(sql);
    }

    /**
     * Adds a UNION ALL clause with a SQL query string.
     * 
     * @param query the SQL query to union all
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder unionAll(final String query) {
        return unionAll(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for UNION ALL operation.
     * 
     * @param propOrColumnNames the columns for the union all query
     * @return this SQLBuilder instance for method chaining
     */
    public final SQLBuilder unionAll(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_UNION_ALL_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     * Starts a new SELECT query for UNION ALL operation with a collection of columns.
     * 
     * @param propOrColumnNames the collection of columns for the union all query
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder unionAll(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_UNION_ALL_SPACE);

        return this;
    }

    /**
     * Adds an INTERSECT clause with another SQL query.
     * 
     * <pre>{@code
     * SQLBuilder query1 = PSC.select("id", "name").from("users");
     * SQLBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.intersect(query2).sql();
     * // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM customers
     * }</pre>
     * 
     * @param sqlBuilder the SQL builder containing the query to intersect
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder intersect(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return intersect(sql);
    }

    /**
     * Adds an INTERSECT clause with a SQL query string.
     * 
     * @param query the SQL query to intersect
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder intersect(final String query) {
        return intersect(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for INTERSECT operation.
     * 
     * @param propOrColumnNames the columns for the intersect query
     * @return this SQLBuilder instance for method chaining
     */
    public final SQLBuilder intersect(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_INTERSECT_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     * Starts a new SELECT query for INTERSECT operation with a collection of columns.
     * 
     * @param propOrColumnNames the collection of columns for the intersect query
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder intersect(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_INTERSECT_SPACE);

        return this;
    }

    /**
     * Adds an EXCEPT clause with another SQL query.
     * 
     * <pre>{@code
     * SQLBuilder query1 = PSC.select("id", "name").from("users");
     * SQLBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.except(query2).sql();
     * // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM customers
     * }</pre>
     * 
     * @param sqlBuilder the SQL builder containing the query to except
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder except(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return except(sql);
    }

    /**
     * Adds an EXCEPT clause with a SQL query string.
     * 
     * @param query the SQL query to except
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder except(final String query) {
        return except(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for EXCEPT operation.
     * 
     * @param propOrColumnNames the columns for the except query
     * @return this SQLBuilder instance for method chaining
     */
    public final SQLBuilder except(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_EXCEPT_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     * Starts a new SELECT query for EXCEPT operation with a collection of columns.
     * 
     * @param propOrColumnNames the collection of columns for the except query
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder except(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_EXCEPT_SPACE);

        return this;
    }

    /**
     * Adds a MINUS clause with another SQL query (Oracle syntax).
     * 
     * @param sqlBuilder the SQL builder containing the query to minus
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder minus(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return minus(sql);
    }

    /**
     * Adds a MINUS clause with a SQL query string (Oracle syntax).
     * 
     * @param query the SQL query to minus
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder minus(final String query) {
        return minus(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for MINUS operation (Oracle syntax).
     * 
     * @param propOrColumnNames the columns for the minus query
     * @return this SQLBuilder instance for method chaining
     */
    public final SQLBuilder minus(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_EXCEPT2_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     * Starts a new SELECT query for MINUS operation with a collection of columns (Oracle syntax).
     * 
     * @param propOrColumnNames the collection of columns for the minus query
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder minus(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        _sb.append(_SPACE_EXCEPT2_SPACE);

        return this;
    }

    /**
     * Adds a FOR UPDATE clause to lock selected rows.
     * 
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where(CF.eq("id", 1))
     *                 .forUpdate()
     *                 .sql();
     * // Output: SELECT * FROM users WHERE id = ? FOR UPDATE
     * }</pre>
     * 
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder forUpdate() {
        _sb.append(_SPACE_FOR_UPDATE);

        return this;
    }

    /**
     * Sets columns for UPDATE operation with a single expression.
     * 
     * <pre>{@code
     * String sql = PSC.update("users")
     *                 .set("name = 'John'")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: UPDATE users SET name = 'John' WHERE id = ?
     * }</pre>
     * 
     * @param expr the SET expression
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder set(final String expr) {
        return set(Array.asList(expr));
    }

    /**
     * Sets columns for UPDATE operation.
     * 
     * <pre>{@code
     * String sql = PSC.update("users")
     *                 .set("firstName", "lastName", "email")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?
     * }</pre>
     * 
     * @param propOrColumnNames the columns to update
     * @return this SQLBuilder instance for method chaining
     */
    public final SQLBuilder set(final String... propOrColumnNames) {
        return set(Array.asList(propOrColumnNames));
    }

    /**
     * Sets columns for UPDATE operation with a collection.
     * 
     * @param propOrColumnNames the collection of columns to update
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder set(final Collection<String> propOrColumnNames) {
        init(false);

        switch (_sqlPolicy) {
            case SQL:
            case PARAMETERIZED_SQL: {
                int i = 0;
                for (final String columnName : propOrColumnNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(columnName);

                    if (columnName.indexOf('=') < 0) {
                        _sb.append(" = ?");
                    }
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                for (final String columnName : propOrColumnNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(columnName);

                    if (columnName.indexOf('=') < 0) {
                        _sb.append(" = ");

                        _handlerForNamedParameter.accept(_sb, columnName);
                    }
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                for (final String columnName : propOrColumnNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(columnName);

                    if (columnName.indexOf('=') < 0) {
                        _sb.append(" = #{");
                        _sb.append(columnName);
                        _sb.append('}');
                    }
                }

                break;
            }

            default:
                throw new RuntimeException("Not supported SQL policy: " + _sqlPolicy);
        }

        _propOrColumnNames = null;

        return this;
    }

    /**
     * Sets columns and values for UPDATE operation using a map.
     * 
     * <pre>{@code
     * Map<String, Object> values = new HashMap<>();
     * values.put("firstName", "John");
     * values.put("lastName", "Doe");
     * String sql = PSC.update("users")
     *                 .set(values)
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: UPDATE users SET first_name = ?, last_name = ? WHERE id = ?
     * }</pre>
     * 
     * @param props map of column names to values
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder set(final Map<String, Object> props) {
        init(false);

        switch (_sqlPolicy) {
            case SQL: {
                int i = 0;
                for (final Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    _sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForSQL(entry.getValue());
                }

                break;
            }

            case PARAMETERIZED_SQL: {
                int i = 0;
                for (final Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    _sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForRawSQL(entry.getValue());
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                for (final Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    _sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForNamedSQL(entry.getKey(), entry.getValue());
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                for (final Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    _sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForIbatisNamedSQL(entry.getKey(), entry.getValue());
                }

                break;
            }

            default:
                throw new RuntimeException("Not supported SQL policy: " + _sqlPolicy);
        }

        _propOrColumnNames = null;

        return this;
    }

    /**
     * Sets properties to update from an entity object.
     * Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
     * 
     * @param entity the entity object containing properties to set
     * @return this SQLBuilder instance for method chaining
     * 
     * <pre>{@code
     * // Example usage:
     * String sql = PSC.update("account")
     *                 .set(accountEntity)
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * }</pre>
     */
    public SQLBuilder set(final Object entity) {
        return set(entity, null);
    }

    /**
     * Sets properties to update from an entity object, excluding specified properties.
     * Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
     * 
     * @param entity the entity object containing properties to set
     * @param excludedPropNames properties to exclude from the update
     * @return this SQLBuilder instance for method chaining
     * 
     * <pre>{@code
     * // Example usage:
     * Set<String> excluded = N.asSet("createdDate", "version");
     * String sql = PSC.update("account")
     *                 .set(accountEntity, excluded)
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * }</pre>
     */
    public SQLBuilder set(final Object entity, final Set<String> excludedPropNames) {
        if (entity instanceof String) {
            return set(N.asArray((String) entity));
        }
        if (entity instanceof Map) {
            if (N.isEmpty(excludedPropNames)) {
                return set((Map<String, Object>) entity);
            }
            final Map<String, Object> localProps = new LinkedHashMap<>((Map<String, Object>) entity); //NOSONAR
            Maps.removeKeys(localProps, excludedPropNames);
            return set(localProps);
        }

        final Class<?> entityClass = entity.getClass();
        setEntityClass(entityClass);
        final Collection<String> propNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);
        final Map<String, Object> localProps = N.newHashMap(propNames.size());

        for (final String propName : propNames) {
            localProps.put(propName, _entityInfo.getPropValue(entity, propName));
        }

        return set(localProps);
    }

    /**
     * Sets all updatable properties from an entity class for UPDATE operation.
     * Properties marked with @NonUpdatable, @ReadOnly, @ReadOnlyId, or @Transient annotations are excluded.
     *
     * @param entityClass the entity class to get properties from
     * @return this SQLBuilder instance for method chaining
     * 
     * <pre>{@code
     * // Example usage:
     * String sql = PSC.update("account")
     *                 .set(Account.class)
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * }</pre>
     */
    public SQLBuilder set(final Class<?> entityClass) {
        setEntityClass(entityClass);

        return set(entityClass, null);
    }

    /**
     * Sets updatable properties from an entity class for UPDATE operation, excluding specified properties.
     * Properties marked with @NonUpdatable, @ReadOnly, @ReadOnlyId, or @Transient annotations are automatically excluded.
     *
     * @param entityClass the entity class to get properties from
     * @param excludedPropNames additional properties to exclude from the update
     * @return this SQLBuilder instance for method chaining
     * 
     * <pre>{@code
     * // Example usage:
     * Set<String> excluded = N.asSet("lastModified");
     * String sql = PSC.update("account")
     *                 .set(Account.class, excluded)
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * }</pre>
     */
    public SQLBuilder set(final Class<?> entityClass, final Set<String> excludedPropNames) {
        setEntityClass(entityClass);

        return set(QueryUtil.getUpdatePropNames(entityClass, excludedPropNames));
    }

    /**
     * Generates the final SQL string from this builder.
     * This method finalizes the SQL builder and releases resources. The builder cannot be used after calling this method.
     *
     * @return the generated SQL string
     * @throws RuntimeException if the builder has already been closed
     * 
     * <pre>{@code
     * // Example usage:
     * String sql = PSC.select("id", "name")
     *                 .from("account")
     *                 .where(CF.gt("age", 18))
     *                 .sql();
     * // Result: SELECT id, name FROM account WHERE age > ?
     * }</pre>
     */
    public String sql() {
        if (_sb == null) {
            throw new RuntimeException("This SQLBuilder has been closed after sql() was called previously");
        }

        init(true);

        String sql = null;

        try {
            sql = _sb.charAt(0) == ' ' ? _sb.substring(1) : _sb.toString();
        } finally {
            Objectory.recycle(_sb);
            _sb = null;

            activeStringBuilderCounter.decrementAndGet();
        }

        //    if (logger.isDebugEnabled()) {
        //        logger.debug(sql);
        //    }

        return sql;
    }

    /**
     * Returns the list of parameter values for the generated SQL.
     * For parameterized SQL (using ?), this list contains the actual values in order.
     * For named SQL, this list contains the values corresponding to named parameters.
     *
     * @return an immutable list of parameter values
     * 
     * <pre>{@code
     * // Example usage:
     * SQLBuilder builder = PSC.select("*")
     *                        .from("account")
     *                        .where(CF.eq("name", "John"))
     *                        .where(CF.gt("age", 25));
     * List<Object> params = builder.parameters();
     * // params contains: ["John", 25]
     * }</pre>
     */
    public List<Object> parameters() {
        return _parameters;
    }

    /**
     * Generates both the SQL string and its parameters as a pair.
     * This method finalizes the SQL builder and releases resources. The builder cannot be used after calling this method.
     *
     * @return an SP (SQL-Parameters) pair containing the SQL string and parameter list
     * 
     * <pre>{@code
     * // Example usage:
     * SP sqlPair = PSC.select("*")
     *                 .from("account")
     *                 .where(CF.eq("status", "ACTIVE"))
     *                 .pair();
     * // sqlPair.sql contains: "SELECT * FROM account WHERE status = ?"
     * // sqlPair.parameters contains: ["ACTIVE"]
     * }</pre>
     */
    public SP build() {
        final String sql = sql();

        return new SP(sql, _parameters);
    }

    /**
     * Generates both the SQL string and its parameters as a pair.
     * This method finalizes the SQL builder and releases resources. The builder cannot be used after calling this method.
     *
     * @return an SP (SQL-Parameters) pair containing the SQL string and parameter list
     * 
     * <pre>{@code
     * // Example usage:
     * SP sqlPair = PSC.select("*")
     *                 .from("account")
     *                 .where(CF.eq("status", "ACTIVE"))
     *                 .pair();
     * // sqlPair.sql contains: "SELECT * FROM account WHERE status = ?"
     * // sqlPair.parameters contains: ["ACTIVE"]
     * }</pre>
     * @deprecated Use {@link #build()} instead
     */
    public SP pair() {
        return build();
    }

    /**
     * Applies a function to the SQL-Parameters pair and returns the result.
     * This is useful for executing the SQL directly with a data access framework.
     *
     * @param <T> the return type of the function
     * @param <E> the exception type that may be thrown
     * @param func the function to apply to the SP pair
     * @return the result of applying the function
     * @throws E if the function throws an exception
     * 
     * <pre>{@code
     * // Example usage:
     * List<Account> accounts = PSC.select("*")
     *     .from("account")
     *     .where(CF.eq("status", "ACTIVE"))
     *     .apply(sp -> jdbcTemplate.query(sp.sql, sp.parameters, accountRowMapper));
     * }</pre>
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.Function<? super SP, T, E> func) throws E {
        return func.apply(build());
    }

    /**
     * Applies a bi-function to the SQL string and parameters separately and returns the result.
     * This is useful for executing the SQL directly with a data access framework that takes SQL and parameters separately.
     *
     * @param <T> the return type of the function
     * @param <E> the exception type that may be thrown
     * @param func the bi-function to apply to the SQL and parameters
     * @return the result of applying the function
     * @throws E if the function throws an exception
     * 
     * <pre>{@code
     * // Example usage:
     * int count = PSC.update("account")
     *     .set("status", "INACTIVE")
     *     .where(CF.lt("lastLogin", oneYearAgo))
     *     .apply((sql, params) -> jdbcTemplate.update(sql, params.toArray()));
     * }</pre>
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.BiFunction<? super String, ? super List<Object>, T, E> func) throws E {
        final SP sp = build();

        return func.apply(sp.sql, sp.parameters);
    }

    /**
     * Accepts a consumer for the SQL-Parameters pair.
     * This is useful for executing the SQL with a data access framework when no return value is needed.
     *
     * @param <E> the exception type that may be thrown
     * @param consumer the consumer to accept the SP pair
     * @throws E if the consumer throws an exception
     * 
     * <pre>{@code
     * // Example usage:
     * PSC.insert("account")
     *    .values("name", "email", "status")
     *    .accept(sp -> jdbcTemplate.update(sp.sql, sp.parameters.toArray()));
     * }</pre>
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.Consumer<? super SP, E> consumer) throws E {
        consumer.accept(build());
    }

    /**
     * Accepts a bi-consumer for the SQL string and parameters separately.
     * This is useful for executing the SQL with a data access framework when no return value is needed.
     *
     * @param <E> the exception type that may be thrown
     * @param consumer the bi-consumer to accept the SQL and parameters
     * @throws E if the consumer throws an exception
     * 
     * <pre>{@code
     * // Example usage:
     * PSC.delete()
     *    .from("account")
     *    .where(CF.eq("status", "DELETED"))
     *    .accept((sql, params) -> {
     *        logger.info("Executing: {} with params: {}", sql, params);
     *        jdbcTemplate.update(sql, params.toArray());
     *    });
     * }</pre>
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.BiConsumer<? super String, ? super List<Object>, E> consumer) throws E {
        final SP sp = build();

        consumer.accept(sp.sql, sp.parameters);
    }

    /**
     * Prints the generated SQL to standard output.
     * This is useful for debugging and development.
     * 
     * <pre>{@code
     * // Example usage:
     * PSC.select("*")
     *    .from("account")
     *    .where(CF.between("age", 18, 65))
     *    .println();
     * // Prints: SELECT * FROM account WHERE age BETWEEN ? AND ?
     * }</pre>
     */
    public void println() {
        N.println(sql());
    }

    /**
     * Returns the generated SQL string representation of this builder.
     * Note: This method finalizes the builder and it cannot be used afterwards.
     *
     * @return the generated SQL string
     */
    @Override
    public String toString() {
        return sql();
    }
    //    /**
    //     *
    //     * @param <Q>
    //     * @param dataSource
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <Q extends com.landawn.abacus.jdbc.AbstractQuery> Q toPreparedQuery(final javax.sql.DataSource dataSource) throws SQLException {
    //        return toPreparedQuery(dataSource, null);
    //    }
    //
    //    /**
    //     *
    //     * @param <Q>
    //     * @param conn
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <Q extends com.landawn.abacus.jdbc.AbstractQuery> Q toPreparedQuery(final java.sql.Connection conn) throws SQLException {
    //        return toPreparedQuery(conn, null);
    //    }
    //
    //    /**
    //     *
    //     * @param <Q>
    //     * @param dataSource
    //     * @param stmtSetter
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <Q extends com.landawn.abacus.jdbc.AbstractQuery> Q toPreparedQuery(final javax.sql.DataSource dataSource,
    //            final Throwables.Consumer<? super java.sql.PreparedStatement, ? extends SQLException> stmtSetter) throws SQLException {
    //        final SP sp = this.pair();
    //
    //        final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? com.landawn.abacus.jdbc.JdbcUtil.prepareNamedQuery(dataSource, sp.sql)
    //                : com.landawn.abacus.jdbc.JdbcUtil.prepareQuery(dataSource, sp.sql);
    //
    //        boolean noException = false;
    //
    //        try {
    //            if (stmtSetter != null) {
    //                preparedQuery.configStmt(stmtSetter);
    //            }
    //
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            noException = true;
    //        } finally {
    //            if (!noException) {
    //                preparedQuery.close();
    //            }
    //        }
    //
    //        return (Q) preparedQuery;
    //    }
    //
    //    /**
    //     *
    //     * @param <Q>
    //     * @param conn
    //     * @param stmtSetter
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <Q extends com.landawn.abacus.jdbc.AbstractQuery> Q toPreparedQuery(final java.sql.Connection conn,
    //            final Throwables.Consumer<? super java.sql.PreparedStatement, ? extends SQLException> stmtSetter) throws SQLException {
    //        final SP sp = this.pair();
    //
    //        final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? com.landawn.abacus.jdbc.JdbcUtil.prepareNamedQuery(conn, sp.sql)
    //                : com.landawn.abacus.jdbc.JdbcUtil.prepareQuery(conn, sp.sql);
    //
    //        boolean noException = false;
    //
    //        try {
    //            if (stmtSetter != null) {
    //                preparedQuery.configStmt(stmtSetter);
    //            }
    //
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            noException = true;
    //        } finally {
    //            if (!noException) {
    //                preparedQuery.close();
    //            }
    //        }
    //
    //        return (Q) preparedQuery;
    //    }
    //
    //    /**
    //     *
    //     * @param <Q>
    //     * @param dataSource
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <Q extends com.landawn.abacus.jdbc.AbstractQuery> Q toPreparedQueryForBigResult(final javax.sql.DataSource dataSource) throws SQLException {
    //        final SP sp = this.pair();
    //
    //        final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql()
    //                ? com.landawn.abacus.jdbc.JdbcUtil.prepareNamedQueryForBigResult(dataSource, sp.sql)
    //                : com.landawn.abacus.jdbc.JdbcUtil.prepareQueryForBigResult(dataSource, sp.sql);
    //
    //        boolean noException = false;
    //
    //        try {
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            noException = true;
    //        } finally {
    //            if (!noException) {
    //                preparedQuery.close();
    //            }
    //        }
    //
    //        return (Q) preparedQuery;
    //    }
    //
    //    /**
    //     *
    //     * @param <Q>
    //     * @param conn
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <Q extends com.landawn.abacus.jdbc.AbstractQuery> Q toPreparedQueryForBigResult(final java.sql.Connection conn) throws SQLException {
    //        final SP sp = this.pair();
    //
    //        final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? com.landawn.abacus.jdbc.JdbcUtil.prepareNamedQueryForBigResult(conn, sp.sql)
    //                : com.landawn.abacus.jdbc.JdbcUtil.prepareQueryForBigResult(conn, sp.sql);
    //
    //        boolean noException = false;
    //
    //        try {
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            noException = true;
    //        } finally {
    //            if (!noException) {
    //                preparedQuery.close();
    //            }
    //        }
    //
    //        return (Q) preparedQuery;
    //    }

    //    /**
    //     *
    //     * @param <R>
    //     * @param dataSource
    //     * @param queryOrUpdateCall
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <R> R execute(final javax.sql.DataSource dataSource, final Throwables.Function<com.landawn.abacus.jdbc.AbstractQuery, R, SQLException> queryOrUpdateCall)
    //            throws SQLException {
    //        final SP sp = this.pair();
    //
    //        try (final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? JdbcUtil.prepareNamedQuery(dataSource, sp.sql)
    //                : JdbcUtil.prepareQuery(dataSource, sp.sql)) {
    //
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            return queryOrUpdateCall.apply(preparedQuery);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param dataSource
    //     * @param queryOrUpdateCall
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <R> R execute(final java.sql.Connection conn, final Throwables.Function<com.landawn.abacus.jdbc.AbstractQuery, R, SQLException> queryOrUpdateCall)
    //            throws SQLException {
    //        final SP sp = this.pair();
    //
    //        try (final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? JdbcUtil.prepareNamedQuery(conn, sp.sql) : JdbcUtil.prepareQuery(conn, sp.sql)) {
    //
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            return queryOrUpdateCall.apply(preparedQuery);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param dataSource
    //     * @param stmtSetter
    //     * @param queryOrUpdateCall
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <R> R execute(final javax.sql.DataSource dataSource,
    //            final Throwables.Consumer<? super java.sql.PreparedStatement, ? extends SQLException> stmtSetter,
    //            final Throwables.Function<com.landawn.abacus.jdbc.AbstractQuery, R, SQLException> queryOrUpdateCall) throws SQLException {
    //        final SP sp = this.pair();
    //
    //        try (final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? JdbcUtil.prepareNamedQuery(dataSource, sp.sql)
    //                : JdbcUtil.prepareQuery(dataSource, sp.sql)) {
    //
    //            preparedQuery.configStmt(stmtSetter);
    //
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            return queryOrUpdateCall.apply(preparedQuery);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param conn
    //     * @param stmtSetter
    //     * @param queryCall
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <R> R execute(final java.sql.Connection conn, final Throwables.Consumer<? super java.sql.PreparedStatement, ? extends SQLException> stmtSetter,
    //            final Throwables.Function<com.landawn.abacus.jdbc.AbstractQuery, R, SQLException> queryCall) throws SQLException {
    //        final SP sp = this.pair();
    //
    //        try (final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? JdbcUtil.prepareNamedQuery(conn, sp.sql) : JdbcUtil.prepareQuery(conn, sp.sql)) {
    //
    //            preparedQuery.configStmt(stmtSetter);
    //
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            return queryCall.apply(preparedQuery);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param dataSource
    //     * @param queryCall
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <R> R executeQueryForBigResult(final javax.sql.DataSource dataSource, final Throwables.Function<com.landawn.abacus.jdbc.AbstractQuery, R, SQLException> queryCall)
    //            throws SQLException {
    //        final SP sp = this.pair();
    //
    //        try (final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? JdbcUtil.prepareNamedQueryForBigResult(dataSource, sp.sql)
    //                : JdbcUtil.prepareQueryForBigResult(dataSource, sp.sql)) {
    //
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            return queryCall.apply(preparedQuery);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param dataSource
    //     * @param queryOrUpdateCall
    //     * @return
    //     * @throws SQLException
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    public <R> R executeQueryForBigResult(final java.sql.Connection conn, final Throwables.Function<com.landawn.abacus.jdbc.AbstractQuery, R, SQLException> queryOrUpdateCall)
    //            throws SQLException {
    //        final SP sp = this.pair();
    //
    //        try (final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? JdbcUtil.prepareNamedQueryForBigResult(conn, sp.sql)
    //                : JdbcUtil.prepareQueryForBigResult(conn, sp.sql)) {
    //
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            return queryOrUpdateCall.apply(preparedQuery);
    //        }
    //    }

    /**
     *
     * @param setForUpdate
     */
    protected void init(final boolean setForUpdate) {
        // Note: any change, please take a look at: parse(final Class<?> entityClass, final Condition cond) first.

        if (!_sb.isEmpty()) {
            return;
        }

        if (_op == OperationType.UPDATE) {
            _sb.append(_UPDATE);

            _sb.append(_SPACE);
            _sb.append(_tableName);

            _sb.append(_SPACE_SET_SPACE);

            if (setForUpdate && N.notEmpty(_propOrColumnNames)) {
                set(_propOrColumnNames);
            }
        } else if (_op == OperationType.DELETE) {
            final String newTableName = _tableName;

            final char[] deleteFromTableChars = tableDeleteFrom.computeIfAbsent(newTableName,
                    n -> (SK.DELETE + SK.SPACE + SK.FROM + SK.SPACE + n).toCharArray());

            _sb.append(deleteFromTableChars);
        } else if (_op == OperationType.QUERY && !_hasFromBeenSet && !_isForConditionOnly) {
            throw new RuntimeException("'from' methods has not been called for query: " + _op);
        }
    }

    protected void setEntityClass(final Class<?> entityClass) {
        _entityClass = entityClass;

        if (ClassUtil.isBeanClass(entityClass)) {
            _entityInfo = ParserUtil.getBeanInfo(entityClass);
            _propColumnNameMap = prop2ColumnNameMap(entityClass, _namingPolicy);
        } else {
            _entityInfo = null;
            _propColumnNameMap = null;
        }
    }

    /**
     * Sets the parameter for SQL.
     *
     * @param propValue the new parameter for SQL
     */
    protected void setParameterForSQL(final Object propValue) {
        if (CF.QME.equals(propValue)) {
            _sb.append(SK._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            _sb.append(Expression.formalize(propValue));
        }
    }

    /**
     * Sets the parameter for raw SQL.
     *
     * @param propValue the new parameter for raw SQL
     */
    protected void setParameterForRawSQL(final Object propValue) {
        if (CF.QME.equals(propValue)) {
            _sb.append(SK._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            _sb.append(SK._QUESTION_MARK);

            _parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter for named SQL.
     *
     * @param propName
     * @param propValue
     */
    protected void setParameterForNamedSQL(final String propName, final Object propValue) {
        if (CF.QME.equals(propValue)) {
            _handlerForNamedParameter.accept(_sb, propName);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            _handlerForNamedParameter.accept(_sb, propName);

            _parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter for ibatis named SQL.
     *
     * @param propName
     * @param propValue
     */
    protected void setParameterForIbatisNamedSQL(final String propName, final Object propValue) {
        if (CF.QME.equals(propValue)) {
            _sb.append("#{");
            _sb.append(propName);
            _sb.append('}');
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            _sb.append("#{");
            _sb.append(propName);
            _sb.append('}');

            _parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter.
     *
     * @param propName
     * @param propValue
     */
    protected void setParameter(final String propName, final Object propValue) {
        switch (_sqlPolicy) {
            case SQL: {
                setParameterForSQL(propValue);

                break;
            }

            case PARAMETERIZED_SQL: {
                setParameterForRawSQL(propValue);

                break;
            }

            case NAMED_SQL: {
                setParameterForNamedSQL(propName, propValue);

                break;
            }

            case IBATIS_SQL: {
                setParameterForIbatisNamedSQL(propName, propValue);

                break;
            }

            default:
                throw new RuntimeException("Not supported SQL policy: " + _sqlPolicy);
        }
    }

    /**
     * Append insert props.
     *
     * @param props
     */
    protected void appendInsertProps(final Map<String, Object> props) {
        switch (_sqlPolicy) {
            case SQL: {
                int i = 0;
                for (final Object propValue : props.values()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    setParameterForSQL(propValue);
                }

                break;
            }

            case PARAMETERIZED_SQL: {
                int i = 0;
                for (final Object propValue : props.values()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    setParameterForRawSQL(propValue);
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                for (final Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    setParameterForNamedSQL(entry.getKey(), entry.getValue());
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                for (final Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    setParameterForIbatisNamedSQL(entry.getKey(), entry.getValue());
                }

                break;
            }

            default:
                throw new RuntimeException("Not supported SQL policy: " + _sqlPolicy);
        }
    }

    /**
     *
     * @param cond
     */
    protected void appendCondition(final Condition cond) {
        //    if (sb.charAt(sb.length() - 1) != _SPACE) {
        //        sb.append(_SPACE);
        //    }

        if (cond instanceof final Binary binary) {
            final String propName = binary.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(binary.getOperator().toString());
            _sb.append(_SPACE);

            final Object propValue = binary.getPropValue();
            setParameter(propName, propValue);
        } else if (cond instanceof final Between bt) {
            final String propName = bt.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(bt.getOperator().toString());
            _sb.append(_SPACE);

            final Object minValue = bt.getMinValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("min" + Strings.capitalize(propName), minValue);
            } else {
                setParameter(propName, minValue);
            }

            _sb.append(_SPACE);
            _sb.append(SK.AND);
            _sb.append(_SPACE);

            final Object maxValue = bt.getMaxValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("max" + Strings.capitalize(propName), maxValue);
            } else {
                setParameter(propName, maxValue);
            }
        } else if (cond instanceof final NotBetween nbt) {
            final String propName = nbt.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(nbt.getOperator().toString());
            _sb.append(_SPACE);

            final Object minValue = nbt.getMinValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("min" + Strings.capitalize(propName), minValue);
            } else {
                setParameter(propName, minValue);
            }

            _sb.append(_SPACE);
            _sb.append(SK.AND);
            _sb.append(_SPACE);

            final Object maxValue = nbt.getMaxValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("max" + Strings.capitalize(propName), maxValue);
            } else {
                setParameter(propName, maxValue);
            }
        } else if (cond instanceof final In in) {
            final String propName = in.getPropName();
            final List<Object> params = in.getParameters();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(in.getOperator().toString());
            _sb.append(SK.SPACE_PARENTHESES_L);

            for (int i = 0, len = params.size(); i < len; i++) {
                if (i > 0) {
                    _sb.append(SK.COMMA_SPACE);
                }

                if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                    setParameter(propName + (i + 1), params.get(i));
                } else {
                    setParameter(propName, params.get(i));
                }
            }

            _sb.append(SK._PARENTHESES_R);
        } else if (cond instanceof final InSubQuery inSubQuery) {
            final String propName = inSubQuery.getPropName();

            if (Strings.isNotEmpty(propName)) {
                appendColumnName(propName);
            } else {
                _sb.append(SK._PARENTHESES_L);

                int idx = 0;

                for (final String e : inSubQuery.getPropNames()) {
                    if (idx++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(e);
                }

                _sb.append(SK._PARENTHESES_R);
            }

            _sb.append(_SPACE);
            _sb.append(inSubQuery.getOperator().toString());

            _sb.append(SK.SPACE_PARENTHESES_L);

            appendCondition(inSubQuery.getSubQuery());

            _sb.append(SK._PARENTHESES_R);
        } else if (cond instanceof final NotIn notIn) {
            final String propName = notIn.getPropName();
            final List<Object> params = notIn.getParameters();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(notIn.getOperator().toString());
            _sb.append(SK.SPACE_PARENTHESES_L);

            for (int i = 0, len = params.size(); i < len; i++) {
                if (i > 0) {
                    _sb.append(SK.COMMA_SPACE);
                }

                if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                    setParameter(propName + (i + 1), params.get(i));
                } else {
                    setParameter(propName, params.get(i));
                }
            }

            _sb.append(SK._PARENTHESES_R);
        } else if (cond instanceof final NotInSubQuery notInSubQuery) {
            final String propName = notInSubQuery.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(notInSubQuery.getOperator().toString());
            _sb.append(SK.SPACE_PARENTHESES_L);

            appendCondition(notInSubQuery.getSubQuery());

            _sb.append(SK._PARENTHESES_R);
        } else if (cond instanceof Where || cond instanceof Having) {
            final Cell cell = (Cell) cond;

            _sb.append(_SPACE);
            _sb.append(cell.getOperator().toString());
            _sb.append(_SPACE);

            appendCondition(cell.getCondition());
        } else if (cond instanceof final Cell cell) {
            _sb.append(_SPACE);
            _sb.append(cell.getOperator().toString());
            _sb.append(_SPACE);

            _sb.append(_PARENTHESES_L);
            appendCondition(cell.getCondition());
            _sb.append(_PARENTHESES_R);
        } else if (cond instanceof final Junction junction) {
            final List<Condition> conditionList = junction.getConditions();

            if (N.isEmpty(conditionList)) {
                throw new IllegalArgumentException("The junction condition(" + junction.getOperator().toString() + ") doesn't include any element.");
            }

            if (conditionList.size() == 1) {
                appendCondition(conditionList.get(0));
            } else {
                // TODO ((id = :id) AND (gui = :gui)) is not support in Cassandra.
                // only (id = :id) AND (gui = :gui) works.
                // sb.append(_PARENTHESES_L);

                for (int i = 0, size = conditionList.size(); i < size; i++) {
                    if (i > 0) {
                        _sb.append(_SPACE);
                        _sb.append(junction.getOperator().toString());
                        _sb.append(_SPACE);
                    }

                    _sb.append(_PARENTHESES_L);

                    appendCondition(conditionList.get(i));

                    _sb.append(_PARENTHESES_R);
                }

                // sb.append(_PARENTHESES_R);
            }
        } else if (cond instanceof final SubQuery subQuery) {
            final Condition subCond = subQuery.getCondition();

            if (Strings.isNotEmpty(subQuery.getSql())) {
                _sb.append(subQuery.getSql());
            } else if (subQuery.getEntityClass() != null) {
                if (this instanceof SCSB) {
                    _sb.append(SCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof PSC) {
                    _sb.append(PSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof MSC) {
                    _sb.append(MSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof NSC) {
                    _sb.append(NSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof ACSB) {
                    _sb.append(ACSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof PAC) {
                    _sb.append(PAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof MAC) {
                    _sb.append(MAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof NAC) {
                    _sb.append(NAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof LCSB) {
                    _sb.append(LCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof PLC) {
                    _sb.append(PLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof MLC) {
                    _sb.append(MLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof NLC) {
                    _sb.append(NLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof PSB) {
                    _sb.append(PSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof NSB) {
                    _sb.append(NSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else {
                    throw new RuntimeException("Unsupported subQuery condition: " + cond);
                }
            } else if (this instanceof SCSB) {
                _sb.append(SCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof PSC) {
                _sb.append(PSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof MSC) {
                _sb.append(MSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof NSC) {
                _sb.append(NSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof ACSB) {
                _sb.append(ACSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof PAC) {
                _sb.append(PAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof MAC) {
                _sb.append(MAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof NAC) {
                _sb.append(NAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof LCSB) {
                _sb.append(LCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof PLC) {
                _sb.append(PLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof MLC) {
                _sb.append(MLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof NLC) {
                _sb.append(NLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof PSB) {
                _sb.append(PSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof NSB) {
                _sb.append(NSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else {
                throw new RuntimeException("Unsupported subQuery condition: " + cond);
            }
        } else if (cond instanceof Expression) {
            // ==== version 1
            // sb.append(cond.toString());

            // ==== version 2
            //    final List<String> words = SQLParser.parse(((Expression) cond).getLiteral());
            //    final Map<String, String> propColumnNameMap = getPropColumnNameMap(entityClass, namingPolicy);
            //
            //    String word = null;
            //
            //    for (int i = 0, size = words.size(); i < size; i++) {
            //        word = words.get(i);
            //
            //        if ((i > 2) && SK.AS.equalsIgnoreCase(words.get(i - 2))) {
            //            sb.append(word);
            //        } else if ((i > 1) && SK.SPACE.equalsIgnoreCase(words.get(i - 1))
            //                && (propColumnNameMap.containsKey(words.get(i - 2)) || propColumnNameMap.containsValue(words.get(i - 2)))) {
            //            sb.append(word);
            //        } else {
            //            sb.append(formalizeColumnName(propColumnNameMap, word));
            //        }
            //    }

            // ==== version 3
            appendStringExpr(((Expression) cond).getLiteral(), false);
        } else {
            throw new IllegalArgumentException("Unsupported condition: " + cond.toString());
        }
    }

    protected void appendStringExpr(final String expr, final boolean isFromAppendColumn) {
        // TODO performance improvement.

        if (expr.length() < 16) {
            final boolean matched = CF.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher(expr).find();

            if (matched) {
                if (isFromAppendColumn) {
                    _sb.append(formalizeColumnName(expr, _namingPolicy));
                } else {
                    _sb.append(formalizeColumnName(_propColumnNameMap, expr));
                }

                return;
            }
        }

        final List<String> words = SQLParser.parse(expr);

        String word = null;
        for (int i = 0, len = words.size(); i < len; i++) {
            word = words.get(i);

            if (!Strings.isAsciiAlpha(word.charAt(0)) || SQLParser.isFunctionName(words, len, i)) {
                _sb.append(word);
            } else {
                _sb.append(formalizeColumnName(_propColumnNameMap, word));
            }
        }
    }

    protected void appendColumnName(final String propName) {
        appendColumnName(_entityClass, _entityInfo, _propColumnNameMap, _tableAlias, propName, null, false, null, false, true);
    }

    protected void appendColumnName(final Class<?> entityClass, final BeanInfo entityInfo,
            final ImmutableMap<String, Tuple2<String, Boolean>> propColumnNameMap, final String tableAlias, final String propName, final String propAlias,
            final boolean withClassAlias, final String classAlias, final boolean isForSelect, boolean quotePropAlias) {
        Tuple2<String, Boolean> tp = propColumnNameMap == null ? null : propColumnNameMap.get(propName);

        if (tp != null) {
            if (tp._2 && tableAlias != null && !tableAlias.isEmpty()) {
                _sb.append(tableAlias).append(SK._PERIOD);
            }

            _sb.append(tp._1);

            if (isForSelect && (withClassAlias || _namingPolicy != NamingPolicy.NO_CHANGE)) {
                _sb.append(_SPACE_AS_SPACE);

                if (quotePropAlias) {
                    _sb.append(SK._QUOTATION_D);
                }

                if (withClassAlias) {
                    _sb.append(classAlias).append(SK._PERIOD);
                }

                _sb.append(Strings.isNotEmpty(propAlias) ? propAlias : propName);

                if (quotePropAlias) {
                    _sb.append(SK._QUOTATION_D);
                }
            }

            return;
        }

        if (Strings.isEmpty(propAlias) && entityInfo != null) {
            final PropInfo propInfo = entityInfo.getPropInfo(propName);

            if (propInfo != null && propInfo.isSubEntity) {
                final Class<?> propEntityClass = propInfo.type.isCollection() ? propInfo.type.getElementType().clazz() : propInfo.clazz;

                final String propEntityTableAliasOrName = getTableAliasOrName(propEntityClass, _namingPolicy);

                final ImmutableMap<String, Tuple2<String, Boolean>> subPropColumnNameMap = prop2ColumnNameMap(propEntityClass, _namingPolicy);

                final Collection<String> subSelectPropNames = QueryUtil.getSelectPropNames(propEntityClass, false, null);
                int i = 0;

                for (final String subPropName : subSelectPropNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    _sb.append(propEntityTableAliasOrName).append(SK._PERIOD).append(subPropColumnNameMap.get(subPropName)._1);

                    if (isForSelect) {
                        _sb.append(_SPACE_AS_SPACE);

                        if (quotePropAlias) {
                            _sb.append(SK._QUOTATION_D);
                        }

                        _sb.append(propInfo.name).append(SK._PERIOD).append(subPropName);

                        if (quotePropAlias) {
                            _sb.append(SK._QUOTATION_D);
                        }
                    }
                }

                return;
            }
        }

        if (_aliasPropColumnNameMap != null && !_aliasPropColumnNameMap.isEmpty()) {
            final int index = propName.indexOf('.');

            if (index > 0) {
                final String propTableAlias = propName.substring(0, index);
                final Map<String, Tuple2<String, Boolean>> newPropColumnNameMap = _aliasPropColumnNameMap.get(propTableAlias);

                if (newPropColumnNameMap != null) {
                    final String newPropName = propName.substring(index + 1);
                    tp = newPropColumnNameMap.get(newPropName);

                    if (tp != null) {
                        _sb.append(propTableAlias).append('.').append(tp._1);

                        if (isForSelect && (withClassAlias || _namingPolicy != NamingPolicy.NO_CHANGE)) {
                            _sb.append(_SPACE_AS_SPACE);

                            if (quotePropAlias) {
                                _sb.append(SK._QUOTATION_D);
                            }

                            if (withClassAlias) {
                                _sb.append(classAlias).append(SK._PERIOD);
                            }

                            _sb.append(Strings.isNotEmpty(propAlias) ? propAlias : propName);

                            if (quotePropAlias) {
                                _sb.append(SK._QUOTATION_D);
                            }
                        }

                        return;
                    }
                }
            }
        }

        if (Strings.isNotEmpty(propAlias)) {
            appendStringExpr(propName, true);

            int idx = -1;
            if (isForSelect && (withClassAlias || propAlias.length() >= _sb.length() || _sb.charAt(_sb.length() - propAlias.length() - 1) != _SPACE
                    || _sb.indexOf(propAlias, _sb.length() - propAlias.length()) < 0 || ((idx = propAlias.indexOf(SK._PERIOD)) > 0
                            && (Strings.isEmpty(tableAlias) || tableAlias.length() != idx || !propAlias.startsWith(tableAlias))))) {
                _sb.append(_SPACE_AS_SPACE);

                if (quotePropAlias) {
                    _sb.append(SK._QUOTATION_D);
                }

                if (withClassAlias) {
                    _sb.append(classAlias).append(SK._PERIOD);
                }

                _sb.append(propAlias);

                if (quotePropAlias) {
                    _sb.append(SK._QUOTATION_D);
                }
            }
        } else if (isForSelect) {
            int index = propName.indexOf(" AS ");

            if (index < 0) {
                index = propName.indexOf(" as ");
            }

            if (index > 0) {
                //noinspection ConstantValue
                appendColumnName(entityClass, entityInfo, propColumnNameMap, tableAlias, propName.substring(0, index).trim(),
                        propName.substring(index + 4).trim(), withClassAlias, classAlias, isForSelect, false);
            } else {
                appendStringExpr(propName, true);

                //    char lastChar = propName.charAt(propName.length() - 1);
                //
                //    if (_namingPolicy != NamingPolicy.NO_CHANGE && !(lastChar == '*' || lastChar == ')')) {
                //        _sb.append(_SPACE_AS_SPACE);
                //        _sb.append(SK._QUOTATION_D);
                //
                //        if (withClassAlias) {
                //            _sb.append(classAlias).append(SK._PERIOD);
                //        }
                //
                //        _sb.append(propName);
                //        _sb.append(SK._QUOTATION_D);
                //    }

                int idx = -1;
                if (withClassAlias || propName.length() >= _sb.length() || _sb.charAt(_sb.length() - propName.length() - 1) != _SPACE
                        || _sb.indexOf(propName, _sb.length() - propName.length()) < 0 || ((idx = propName.indexOf(SK._PERIOD)) > 0
                                && (Strings.isEmpty(tableAlias) || tableAlias.length() != idx || !propName.startsWith(tableAlias)))) {
                    _sb.append(_SPACE_AS_SPACE);

                    if (quotePropAlias) {
                        _sb.append(SK._QUOTATION_D);
                    }

                    if (withClassAlias) {
                        _sb.append(classAlias).append(SK._PERIOD);
                    }

                    _sb.append(propName);

                    if (quotePropAlias) {
                        _sb.append(SK._QUOTATION_D);
                    }
                }
            }
        } else {
            appendStringExpr(propName, true);
        }
    }

    protected static boolean hasSubEntityToInclude(final Class<?> entityClass, final boolean includeSubEntityProperties) {
        return includeSubEntityProperties && N.notEmpty(getSubEntityPropNames(entityClass));
    }

    /**
     * Checks if is subquery.
     *
     * @param propOrColumnNames
     * @return true, if is subquery
     */
    protected static boolean isSubQuery(final String... propOrColumnNames) {
        if (propOrColumnNames.length == 1) {
            int index = SQLParser.indexWord(propOrColumnNames[0], SK.SELECT, 0, false);

            if (index >= 0) {
                index = SQLParser.indexWord(propOrColumnNames[0], SK.FROM, index, false);

                return index >= 1;
            }
        }

        return false;
    }

    protected static String formalizeColumnName(final String word, final NamingPolicy namingPolicy) {
        if (sqlKeyWords.contains(word) || namingPolicy == NamingPolicy.NO_CHANGE) {
            return word;
        }
        if (namingPolicy == NamingPolicy.LOWER_CAMEL_CASE) {
            return ClassUtil.formalizePropName(word);
        }
        return namingPolicy.convert(word);
    }

    protected String formalizeColumnName(final ImmutableMap<String, Tuple2<String, Boolean>> propColumnNameMap, final String propName) {
        Tuple2<String, Boolean> tp = propColumnNameMap == null ? null : propColumnNameMap.get(propName);

        if (tp != null) {
            if (tp._2 && _tableAlias != null && !_tableAlias.isEmpty()) {
                return _tableAlias + "." + tp._1;
            }
            return tp._1;
        }

        if (_aliasPropColumnNameMap != null && !_aliasPropColumnNameMap.isEmpty()) {
            final int index = propName.indexOf('.');

            if (index > 0) {
                final String propTableAlias = propName.substring(0, index);
                final Map<String, Tuple2<String, Boolean>> newPropColumnNameMap = _aliasPropColumnNameMap.get(propTableAlias);

                if (newPropColumnNameMap != null) {
                    final String newPropName = propName.substring(index + 1);
                    tp = newPropColumnNameMap.get(newPropName);

                    if (tp != null) {
                        return propTableAlias + "." + tp._1;
                    }
                }
            }
        }

        if (Strings.isNotEmpty(_tableAlias) && propName.length() > _tableAlias.length() + 1 && propName.charAt(_tableAlias.length()) == '.'
                && propName.startsWith(_tableAlias)) {
            return _tableAlias + "." + formalizeColumnName(propName.substring(_tableAlias.length() + 1), _namingPolicy);
        } else {
            return formalizeColumnName(propName, _namingPolicy);
        }
    }

    protected static void parseInsertEntity(final SQLBuilder instance, final Object entity, final Set<String> excludedPropNames) {
        if (entity instanceof String) {
            instance._propOrColumnNames = Array.asList((String) entity);
        } else if (entity instanceof Map) {
            if (N.isEmpty(excludedPropNames)) {
                instance._props = (Map<String, Object>) entity;
            } else {
                instance._props = new LinkedHashMap<>((Map<String, Object>) entity);
                Maps.removeKeys(instance._props, excludedPropNames);
            }
        } else {
            final Collection<String> propNames = QueryUtil.getInsertPropNames(entity, excludedPropNames);
            final Map<String, Object> map = N.newHashMap(propNames.size());
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(entity.getClass());
            final ImmutableList<String> idPropNameList = beanInfo.idPropNameList;
            Object propValue;

            for (final String propName : propNames) {
                propValue = beanInfo.getPropValue(entity, propName);

                if (propValue == null || (!idPropNameList.isEmpty() && idPropNameList.contains(propName) && propValue instanceof Number
                        && ((Number) propValue).longValue() == 0)) {
                    continue; // skip null or zero id values
                }

                map.put(propName, beanInfo.getPropValue(entity, propName));
            }

            instance._props = map;
        }
    }

    protected static List<Map<String, Object>> toInsertPropsList(final Collection<?> propsList) {
        final Optional<?> first = N.firstNonNull(propsList);

        if (first.isPresent() && first.get() instanceof Map) {
            return (List<Map<String, Object>>) propsList;
        }

        final Class<?> entityClass = first.get().getClass();
        final Collection<String> propNames = QueryUtil.getInsertPropNames(entityClass, null);
        final List<Map<String, Object>> newPropsList = new ArrayList<>(propsList.size());

        for (final Object entity : propsList) {
            final Map<String, Object> props = N.newHashMap(propNames.size());
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);

            for (final String propName : propNames) {
                props.put(propName, entityInfo.getPropValue(entity, propName));
            }

            newPropsList.add(props);
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(entityClass);
        final ImmutableList<String> idPropNameList = beanInfo.idPropNameList;

        final List<String> nullPropToRemove = Stream.of(propNames).filter(propName -> Stream.of(newPropsList).allMatch(map -> {
            Object propValue = map.get(propName);

            return propValue == null
                    || (!idPropNameList.isEmpty() && idPropNameList.contains(propName) && propValue instanceof Number && ((Number) propValue).longValue() == 0);
        })).toList();

        if (N.notEmpty(nullPropToRemove)) {
            for (final Map<String, Object> props : newPropsList) {
                Maps.removeKeys(props, nullPropToRemove);
            }
        }

        return newPropsList;
    }

    protected static void checkMultiSelects(final List<Selection> multiSelects) {
        N.checkArgNotEmpty(multiSelects, "multiSelects");

        for (final Selection selection : multiSelects) {
            N.checkArgNotNull(selection.entityClass(), "Class can't be null in 'multiSelects'");
        }
    }

    protected static ImmutableMap<String, Tuple2<String, Boolean>> prop2ColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        return QueryUtil.prop2ColumnNameMap(entityClass, namingPolicy);
    }

    protected enum SQLPolicy {
        SQL, PARAMETERIZED_SQL, NAMED_SQL, IBATIS_SQL
    }

    /**
     * Un-parameterized SQL builder with snake case (lower case with underscore) field/column naming strategy.
     * This builder generates SQL with actual values embedded directly in the SQL string (not recommended for production use).
     *
     * For example:
     * <pre>
     * <code>
     * SCSB.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = 1
     * </code>
     * </pre>
     *
     * @deprecated {@code PSC or NSC} is preferred for better security and performance
     */
    @Deprecated
    public static class SCSB extends SQLBuilder {

        SCSB() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.SQL);
        }

        protected static SCSB createInstance() {
            return new SCSB();
        }

        /**
         * Creates an INSERT SQL builder for a single column.
         *
         * @param expr the column name or expression
         * @return a new SQLBuilder instance for INSERT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.insert("name").into("account").sql();
         * // Output: INSERT INTO account (name)
         * }</pre>
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for multiple columns.
         *
         * @param propOrColumnNames the column names to insert
         * @return a new SQLBuilder instance for INSERT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.insert("name", "email", "status")
         *                  .into("account")
         *                  .sql();
         * // Output: INSERT INTO account (name, email, status)
         * }</pre>
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for a collection of columns.
         *
         * @param propOrColumnNames the collection of column names to insert
         * @return a new SQLBuilder instance for INSERT operation
         * 
         * <pre>{@code
         * // Example:
         * List<String> columns = Arrays.asList("name", "email", "status");
         * String sql = SCSB.insert(columns).into("account").sql();
         * // Output: INSERT INTO account (name, email, status)
         * }</pre>
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with column-value mappings.
         *
         * @param props map of column names to values
         * @return a new SQLBuilder instance for INSERT operation
         * 
         * <pre>{@code
         * // Example:
         * Map<String, Object> props = N.asMap("name", "John", "age", 25);
         * String sql = SCSB.insert(props).into("account").sql();
         * // Output: INSERT INTO account (name, age) VALUES ('John', 25)
         * }</pre>
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object.
         * Properties marked with @Transient, @ReadOnly, or similar annotations are excluded.
         *
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance for INSERT operation
         * 
         * <pre>{@code
         * // Example:
         * Account account = new Account("John", "john@email.com");
         * String sql = SCSB.insert(account).into("account").sql();
         * // Output: INSERT INTO account (name, email) VALUES ('John', 'john@email.com')
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object, excluding specified properties.
         *
         * @param entity the entity object to insert
         * @param excludedPropNames properties to exclude from the insert
         * @return a new SQLBuilder instance for INSERT operation
         * 
         * <pre>{@code
         * // Example:
         * Account account = new Account("John", "john@email.com");
         * Set<String> excluded = N.asSet("createdDate");
         * String sql = SCSB.insert(account, excluded).into("account").sql();
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity class.
         * Generates INSERT statement for all insertable properties of the class.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for INSERT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.insert(Account.class).into("account").sql();
         * // Output: INSERT INTO account (name, email, status, created_date)
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity class, excluding specified properties.
         *
         * @param entityClass the entity class
         * @param excludedPropNames properties to exclude from the insert
         * @return a new SQLBuilder instance for INSERT operation
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class.
         * Convenience method that combines insert() and into() operations.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for INSERT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.insertInto(Account.class).values(...).sql();
         * }</pre>
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class, excluding specified properties.
         *
         * @param entityClass the entity class
         * @param excludedPropNames properties to exclude from the insert
         * @return a new SQLBuilder instance for INSERT operation
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple entities or property maps.
         * Generates MySQL-style batch insert SQL.
         *
         * @param propsList list of entities or property maps to insert
         * @return a new SQLBuilder instance for batch INSERT operation
         * 
         * <pre>{@code
         * // Example:
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "john@email.com"),
         *     new Account("Jane", "jane@email.com")
         * );
         * String sql = SCSB.batchInsert(accounts).into("account").sql();
         * // Output: INSERT INTO account (name, email) VALUES ('John', 'john@email.com'), ('Jane', 'jane@email.com')
         * }</pre>
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table.
         *
         * @param tableName the table name to update
         * @return a new SQLBuilder instance for UPDATE operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.update("account")
         *                  .set("status", "'ACTIVE'")
         *                  .where(CF.eq("id", 1))
         *                  .sql();
         * // Output: UPDATE account SET status = 'ACTIVE' WHERE id = 1
         * }</pre>
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with entity class context.
         * The entity class provides property-to-column name mapping.
         *
         * @param tableName the table name to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for UPDATE operation
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class.
         * The table name is derived from the entity class.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for UPDATE operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.update(Account.class)
         *                  .set("status", "'INACTIVE'")
         *                  .where(CF.lt("lastLogin", "2023-01-01"))
         *                  .sql();
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class, excluding specified properties.
         * Properties marked with @NonUpdatable, @ReadOnly, etc. are automatically excluded.
         *
         * @param entityClass the entity class
         * @param excludedPropNames additional properties to exclude from updates
         * @return a new SQLBuilder instance for UPDATE operation
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table.
         *
         * @param tableName the table name to delete from
         * @return a new SQLBuilder instance for DELETE operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.deleteFrom("account")
         *                  .where(CF.eq("status", "'DELETED'"))
         *                  .sql();
         * // Output: DELETE FROM account WHERE status = 'DELETED'
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table with entity class context.
         *
         * @param tableName the table name to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for DELETE operation
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for an entity class.
         * The table name is derived from the entity class.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for DELETE operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.deleteFrom(Account.class)
         *                  .where(CF.and(
         *                      CF.eq("status", "'INACTIVE'"),
         *                      CF.lt("lastLogin", "2022-01-01")
         *                  ))
         *                  .sql();
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a custom select expression.
         *
         * @param selectPart the select expression (e.g., "COUNT(*)", "DISTINCT name")
         * @return a new SQLBuilder instance for SELECT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.select("COUNT(DISTINCT customer_id)")
         *                  .from("orders")
         *                  .where(CF.between("order_date", "2023-01-01", "2023-12-31"))
         *                  .sql();
         * }</pre>
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for multiple columns.
         *
         * @param propOrColumnNames the column names to select
         * @return a new SQLBuilder instance for SELECT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.select("firstName", "lastName", "email")
         *                  .from("account")
         *                  .where(CF.eq("status", "'ACTIVE'"))
         *                  .sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = 'ACTIVE'
         * }</pre>
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for a collection of columns.
         *
         * @param propOrColumnNames the collection of column names to select
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         *
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance for SELECT operation
         * 
         * <pre>{@code
         * // Example:
         * Map<String, String> aliases = N.asMap(
         *     "firstName", "fname",
         *     "lastName", "lname"
         * );
         * String sql = SCSB.select(aliases).from("account").sql();
         * // Output: SELECT first_name AS fname, last_name AS lname FROM account
         * }</pre>
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for SELECT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.select(Account.class)
         *                  .from("account")
         *                  .sql();
         * // Selects all columns mapped to Account properties
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with optional sub-entity properties.
         *
         * @param entityClass the entity class
         * @param includeSubEntityProperties whether to include properties from related entities
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class, excluding specified properties.
         *
         * @param entityClass the entity class
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         * 
         * <pre>{@code
         * // Example:
         * Set<String> excluded = N.asSet("password", "salt");
         * String sql = SCSB.select(Account.class, excluded)
         *                  .from("account")
         *                  .sql();
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with full control over property inclusion.
         *
         * @param entityClass the entity class
         * @param includeSubEntityProperties whether to include properties from related entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class.
         * Convenience method that combines select() and from() operations.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for SELECT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.selectFrom(Account.class)
         *                  .where(CF.eq("status", "'ACTIVE'"))
         *                  .sql();
         * // Automatically determines table name from entity class
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with table alias.
         *
         * @param entityClass the entity class
         * @param alias the table alias
         * @return a new SQLBuilder instance for SELECT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.selectFrom(Account.class, "a")
         *                  .innerJoin("orders", "o").on("a.id = o.account_id")
         *                  .sql();
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity inclusion option.
         *
         * @param entityClass the entity class
         * @param includeSubEntityProperties whether to include properties from related entities
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and sub-entity inclusion option.
         *
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include properties from related entities
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder excluding specified properties.
         *
         * @param entityClass the entity class
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias, excluding specified properties.
         *
         * @param entityClass the entity class
         * @param alias the table alias
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity inclusion and property exclusion.
         *
         * @param entityClass the entity class
         * @param includeSubEntityProperties whether to include properties from related entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with full control over all options.
         *
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include properties from related entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes.
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA property prefix for the first entity in results
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB property prefix for the second entity in results
         * @return a new SQLBuilder instance for SELECT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.select(Account.class, "a", "account",
         *                         Order.class, "o", "order")
         *                  .from("account a")
         *                  .innerJoin("orders o").on("a.id = o.account_id")
         *                  .sql();
         * // Selects columns like: a.name AS "account.name", o.total AS "order.total"
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes with property exclusions.
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA property prefix for the first entity in results
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB property prefix for the second entity in results
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * Used for complex joins involving multiple tables.
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance for SELECT operation
         * 
         * <pre>{@code
         * // Example:
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account"),
         *     new Selection(Order.class, "o", "order"),
         *     new Selection(Product.class, "p", "product")
         * );
         * String sql = SCSB.select(selections)
         *                  .from("account a")
         *                  .innerJoin("orders o").on("a.id = o.account_id")
         *                  .innerJoin("products p").on("o.product_id = p.id")
         *                  .sql();
         * }</pre>
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for joining two entity classes.
         * Convenience method that combines select() and from() for joins.
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA property prefix for the first entity in results
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB property prefix for the second entity in results
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for joining two entity classes with property exclusions.
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA property prefix for the first entity in results
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB property prefix for the second entity in results
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * Automatically generates the FROM clause based on the entity classes.
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for a table.
         *
         * @param tableName the table name to count rows from
         * @return a new SQLBuilder instance for COUNT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.count("account")
         *                  .where(CF.eq("status", "'ACTIVE'"))
         *                  .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE status = 'ACTIVE'
         * }</pre>
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for an entity class.
         * The table name is derived from the entity class.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for COUNT operation
         * 
         * <pre>{@code
         * // Example:
         * String sql = SCSB.count(Account.class)
         *                  .where(CF.between("createdDate", "2023-01-01", "2023-12-31"))
         *                  .sql();
         * }</pre>
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context.
         * Used to generate SQL fragments for conditions only.
         *
         * @param cond the condition to parse
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance containing the condition SQL
         * 
         * <pre>{@code
         * // Example:
         * Condition cond = CF.and(
         *     CF.eq("status", "'ACTIVE'"),
         *     CF.gt("balance", 1000)
         * );
         * String sql = SCSB.parse(cond, Account.class).sql();
         * // Output: status = 'ACTIVE' AND balance > 1000
         * }</pre>
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Un-parameterized SQL builder with all capital case (upper case with underscore) field/column naming strategy.
     * This builder generates SQL with actual values embedded directly in the SQL string (not recommended for production use).
     * 
     * <p>ACSB stands for "All Capital SQL Builder" and uses UPPER_CASE_WITH_UNDERSCORE naming policy for database columns.
     * It converts camelCase property names to UPPER_CASE_WITH_UNDERSCORE column names automatically.</p>
     *
     * <pre>
     * // Example: property "firstName" becomes column "FIRST_NAME"
     * ACSB.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql();
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = 1
     * </pre>
     *
     * @deprecated {@code PAC or NAC} is preferred for better security and performance. 
     *             Un-parameterized SQL is vulnerable to SQL injection attacks.
     */
    @Deprecated
    public static class ACSB extends SQLBuilder {

        /**
         * Creates a new instance of ACSB with UPPER_CASE_WITH_UNDERSCORE naming policy
         * and SQL (non-parameterized) SQL policy.
         */
        ACSB() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.SQL);
        }

        /**
         * Factory method to create a new ACSB instance.
         * 
         * @return a new ACSB instance
         */
        protected static ACSB createInstance() {
            return new ACSB();
        }

        /**
         * Creates an INSERT SQL builder for a single column.
         * This method starts building an INSERT statement with one column.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.insert("name").into("users").values("John").sql();
         * // Output: INSERT INTO USERS (NAME) VALUES ('John')
         * </pre>
         *
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for multiple columns.
         * This method starts building an INSERT statement with multiple columns.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.insert("name", "age", "email")
         *                  .into("users")
         *                  .values("John", 30, "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO USERS (NAME, AGE, EMAIL) VALUES ('John', 30, 'john@example.com')
         * </pre>
         *
         * @param propOrColumnNames the column names to insert, in order
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for a collection of columns.
         * This method is useful when column names are dynamically determined.
         * 
         * <pre>
         * // Example:
         * List<String> columns = Arrays.asList("name", "age", "email");
         * String sql = ACSB.insert(columns)
         *                  .into("users")
         *                  .values("John", 30, "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO USERS (NAME, AGE, EMAIL) VALUES ('John', 30, 'john@example.com')
         * </pre>
         *
         * @param propOrColumnNames the collection of column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with column-value mappings.
         * This method allows specifying both column names and their values together.
         * 
         * <pre>
         * // Example:
         * Map<String, Object> data = new HashMap<>();
         * data.put("name", "John");
         * data.put("age", 30);
         * String sql = ACSB.insert(data).into("users").sql();
         * // Output: INSERT INTO USERS (NAME, AGE) VALUES ('John', 30)
         * </pre>
         *
         * @param props map of column names to their corresponding values
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object.
         * The entity's properties are mapped to column names using the naming policy.
         * All properties with non-null values will be included in the INSERT statement.
         * 
         * <pre>
         * // Example:
         * User user = new User("John", 30, "john@example.com");
         * String sql = ACSB.insert(user).into("users").sql();
         * // Output: INSERT INTO USERS (NAME, AGE, EMAIL) VALUES ('John', 30, 'john@example.com')
         * </pre>
         *
         * @param entity the entity object containing data to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object, excluding specified properties.
         * This method allows selective insertion of entity properties.
         * 
         * <pre>
         * // Example:
         * User user = new User("John", 30, "john@example.com");
         * Set<String> excluded = new HashSet<>(Arrays.asList("createdDate", "modifiedDate"));
         * String sql = ACSB.insert(user, excluded).into("users").sql();
         * // Output: INSERT INTO USERS (NAME, AGE, EMAIL) VALUES ('John', 30, 'john@example.com')
         * </pre>
         *
         * @param entity the entity object containing data to insert
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity class.
         * This generates an INSERT template for all insertable properties of the class.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.insert(User.class).into("users").sql();
         * // Output: INSERT INTO USERS (NAME, AGE, EMAIL) VALUES (?, ?, ?)
         * </pre>
         *
         * @param entityClass the entity class to use as template
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity class, excluding specified properties.
         * This generates an INSERT template excluding certain properties.
         * 
         * <pre>
         * // Example:
         * Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
         * String sql = ACSB.insert(User.class, excluded).into("users").sql();
         * // Output: INSERT INTO USERS (NAME, AGE, EMAIL) VALUES (?, ?, ?)
         * </pre>
         *
         * @param entityClass the entity class to use as template
         * @param excludedPropNames properties to exclude from the insert template
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class.
         * This is a convenience method that combines insert() and into() operations.
         * The table name is derived from the entity class name.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.insertInto(User.class).values("John", 30, "john@example.com").sql();
         * // Output: INSERT INTO USER (NAME, AGE, EMAIL) VALUES ('John', 30, 'john@example.com')
         * </pre>
         *
         * @param entityClass the entity class to insert into
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class, excluding specified properties.
         * Combines insert() and into() operations with property exclusion.
         * 
         * <pre>
         * // Example:
         * Set<String> excluded = new HashSet<>(Arrays.asList("id"));
         * String sql = ACSB.insertInto(User.class, excluded)
         *                  .values("John", 30, "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO USER (NAME, AGE, EMAIL) VALUES ('John', 30, 'john@example.com')
         * </pre>
         *
         * @param entityClass the entity class to insert into
         * @param excludedPropNames properties to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple entities or property maps.
         * This generates MySQL-style batch insert SQL for efficient bulk inserts.
         * 
         * <pre>
         * // Example:
         * List<User> users = Arrays.asList(
         *     new User("John", 30, "john@example.com"),
         *     new User("Jane", 25, "jane@example.com")
         * );
         * String sql = ACSB.batchInsert(users).into("users").sql();
         * // Output: INSERT INTO USERS (NAME, AGE, EMAIL) VALUES 
         * //         ('John', 30, 'john@example.com'), 
         * //         ('Jane', 25, 'jane@example.com')
         * </pre>
         *
         * @param propsList list of entities or property maps to insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table.
         * This starts building an UPDATE statement for the specified table.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.update("users")
         *                  .set("age", 31)
         *                  .where("name = 'John'")
         *                  .sql();
         * // Output: UPDATE USERS SET AGE = 31 WHERE NAME = 'John'
         * </pre>
         *
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with entity class context.
         * This allows property name mapping when building the UPDATE statement.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.update("users", User.class)
         *                  .set("age", 31)  // "age" is mapped to "AGE" column
         *                  .where("name = 'John'")
         *                  .sql();
         * // Output: UPDATE USERS SET AGE = 31 WHERE NAME = 'John'
         * </pre>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class.
         * The table name is derived from the entity class name.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.update(User.class)
         *                  .set("age", 31)
         *                  .where("name = 'John'")
         *                  .sql();
         * // Output: UPDATE USER SET AGE = 31 WHERE NAME = 'John'
         * </pre>
         *
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class, excluding specified properties.
         * This prepares an UPDATE template excluding certain properties (like id, version fields).
         * 
         * <pre>
         * // Example:
         * Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
         * String sql = ACSB.update(User.class, excluded)
         *                  .set("age", 31)
         *                  .where("id = 1")
         *                  .sql();
         * // Output: UPDATE USER SET AGE = 31 WHERE ID = 1
         * </pre>
         *
         * @param entityClass the entity class to update
         * @param excludedPropNames additional properties to exclude from updates
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table.
         * This starts building a DELETE statement for the specified table.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.deleteFrom("users")
         *                  .where("age < 18")
         *                  .sql();
         * // Output: DELETE FROM USERS WHERE AGE < 18
         * </pre>
         *
         * @param tableName the table name to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table with entity class context.
         * This allows property name mapping in WHERE conditions.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.deleteFrom("users", User.class)
         *                  .where(CF.eq("age", 18))  // "age" is mapped to "AGE" column
         *                  .sql();
         * // Output: DELETE FROM USERS WHERE AGE = 18
         * </pre>
         *
         * @param tableName the table name to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for an entity class.
         * The table name is derived from the entity class name.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.deleteFrom(User.class)
         *                  .where("id = 1")
         *                  .sql();
         * // Output: DELETE FROM USER WHERE ID = 1
         * </pre>
         *
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a custom select expression.
         * This allows complex SELECT expressions including functions and aliases.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.select("COUNT(*) AS total")
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT COUNT(*) AS total FROM USERS
         * </pre>
         *
         * @param selectPart the select expression (can include functions, aliases, etc.)
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for multiple columns.
         * Column names are automatically mapped using the naming policy.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.select("firstName", "lastName", "age")
         *                  .from("users")
         *                  .where("age >= 18")
         *                  .sql();
         * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName", AGE AS "age" 
         * //         FROM USERS WHERE AGE >= 18
         * </pre>
         *
         * @param propOrColumnNames the column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for a collection of columns.
         * Useful when column names are determined dynamically.
         * 
         * <pre>
         * // Example:
         * List<String> columns = getRequiredColumns(); // returns ["name", "email"]
         * String sql = ACSB.select(columns)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT NAME AS "name", EMAIL AS "email" FROM USERS
         * </pre>
         *
         * @param propOrColumnNames collection of column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * This allows renaming columns in the result set.
         * 
         * <pre>
         * // Example:
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = ACSB.select(aliases).from("users").sql();
         * // Output: SELECT FIRST_NAME AS "fname", LAST_NAME AS "lname" FROM USERS
         * </pre>
         *
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class.
         * This selects all mapped properties of the entity.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.select(User.class)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT ID AS "id", NAME AS "name", AGE AS "age", EMAIL AS "email" FROM USERS
         * </pre>
         *
         * @param entityClass the entity class whose properties to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with sub-entity option.
         * When includeSubEntityProperties is true, properties of nested entities are included.
         * 
         * <pre>
         * // Example with sub-entities:
         * String sql = ACSB.select(Order.class, true)  // includes Customer sub-entity
         *                  .from("orders")
         *                  .sql();
         * // Output includes both Order and nested Customer properties
         * </pre>
         *
         * @param entityClass the entity class whose properties to select
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class, excluding specified properties.
         * This allows selective property selection.
         * 
         * <pre>
         * // Example:
         * Set<String> excluded = new HashSet<>(Arrays.asList("password", "secretKey"));
         * String sql = ACSB.select(User.class, excluded)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT ID AS "id", NAME AS "name", AGE AS "age", EMAIL AS "email" FROM USERS
         * </pre>
         *
         * @param entityClass the entity class whose properties to select
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with full control over property selection.
         * Combines sub-entity inclusion and property exclusion options.
         * 
         * <pre>
         * // Example:
         * Set<String> excluded = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = ACSB.select(Order.class, true, excluded)
         *                  .from("orders")
         *                  .sql();
         * // Output includes Order and Customer properties, excluding internalNotes
         * </pre>
         *
         * @param entityClass the entity class whose properties to select
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM SQL builder for an entity class.
         * This is a convenience method that combines select() and from() operations.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.selectFrom(User.class)
         *                  .where("age >= 18")
         *                  .sql();
         * // Output: SELECT ID AS "id", NAME AS "name", AGE AS "age", EMAIL AS "email" 
         * //         FROM USER WHERE AGE >= 18
         * </pre>
         *
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias for an entity class.
         * The alias is used to qualify column names in complex queries.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.selectFrom(User.class, "u")
         *                  .where("u.age >= 18")
         *                  .sql();
         * // Output: SELECT u.ID AS "id", u.NAME AS "name", u.AGE AS "age", u.EMAIL AS "email" 
         * //         FROM USER u WHERE u.AGE >= 18
         * </pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity inclusion option.
         * When includeSubEntityProperties is true, joins are added for sub-entities.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.selectFrom(Order.class, true)
         *                  .where("status = 'ACTIVE'")
         *                  .sql();
         * // Output includes JOINs for sub-entities like Customer
         * </pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include and join sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and sub-entity options.
         * Combines table aliasing with sub-entity inclusion.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.selectFrom(Order.class, "o", true)
         *                  .where("o.status = 'ACTIVE'")
         *                  .sql();
         * // Output includes aliased columns and JOINs for sub-entities
         * </pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include and join sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with property exclusion.
         * Allows selective property selection with automatic FROM clause.
         * 
         * <pre>
         * // Example:
         * Set<String> excluded = new HashSet<>(Arrays.asList("largeBlob", "metadata"));
         * String sql = ACSB.selectFrom(User.class, excluded)
         *                  .where("active = true")
         *                  .sql();
         * // Output: SELECT ID AS "id", NAME AS "name", AGE AS "age", EMAIL AS "email" 
         * //         FROM USER WHERE ACTIVE = true
         * </pre>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and property exclusion.
         * Combines aliasing with selective property selection.
         * 
         * <pre>
         * // Example:
         * Set<String> excluded = new HashSet<>(Arrays.asList("internalCode"));
         * String sql = ACSB.selectFrom(User.class, "u", excluded)
         *                  .innerJoin("orders", "o").on("u.id = o.user_id")
         *                  .sql();
         * // Output uses alias "u" and excludes internalCode property
         * </pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity and exclusion options.
         * Provides full control over entity selection including sub-entities.
         * 
         * <pre>
         * // Example:
         * Set<String> excluded = new HashSet<>(Arrays.asList("deletedFlag"));
         * String sql = ACSB.selectFrom(Order.class, true, excluded)
         *                  .where("createdDate > '2023-01-01'")
         *                  .sql();
         * // Output includes Order with Customer sub-entity, excluding deletedFlag
         * </pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include and join sub-entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with full control over all options.
         * This is the most comprehensive selectFrom method with all configuration options.
         * 
         * <pre>
         * // Example:
         * Set<String> excluded = new HashSet<>(Arrays.asList("debugInfo"));
         * String sql = ACSB.selectFrom(Order.class, "ord", true, excluded)
         *                  .where("ord.totalAmount > 1000")
         *                  .sql();
         * // Output: Complex SELECT with alias, sub-entities, and exclusions
         * </pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include and join sub-entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes.
         * This method sets up a query to select from two tables with aliasing.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.select(User.class, "u", "user", Order.class, "o", "order")
         *                  .from("users", "u")
         *                  .innerJoin("orders", "o").on("u.id = o.user_id")
         *                  .sql();
         * // Output: SELECT with columns from both entities properly aliased
         * </pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA class alias prefix for first entity columns
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB class alias prefix for second entity columns
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes with exclusions.
         * Allows excluding specific properties from each entity in the join.
         * 
         * <pre>
         * // Example:
         * Set<String> userExclusions = new HashSet<>(Arrays.asList("password"));
         * Set<String> orderExclusions = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = ACSB.select(User.class, "u", "user", userExclusions,
         *                         Order.class, "o", "order", orderExclusions)
         *                  .from("users", "u")
         *                  .innerJoin("orders", "o").on("u.id = o.user_id")
         *                  .sql();
         * // Output: SELECT with filtered columns from both entities
         * </pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA class alias prefix for first entity columns
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB class alias prefix for second entity columns
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * This is the most flexible method for complex multi-table queries.
         * 
         * <pre>
         * // Example:
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, null)
         * );
         * String sql = ACSB.select(selections)
         *                  .from("users", "u")
         *                  .innerJoin("orders", "o").on("u.id = o.user_id")
         *                  .innerJoin("products", "p").on("o.product_id = p.id")
         *                  .sql();
         * // Output: Complex SELECT with columns from all three entities
         * </pre>
         *
         * @param multiSelects list of Selection objects defining what to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if multiSelects is invalid
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM SQL builder for joining two entities.
         * Convenience method that includes both SELECT and FROM clauses.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.selectFrom(User.class, "u", "user", Order.class, "o", "order")
         *                  .where("u.active = true")
         *                  .sql();
         * // Output: SELECT ... FROM USER u, ORDER o WHERE u.ACTIVE = true
         * </pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA class alias prefix for first entity columns
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB class alias prefix for second entity columns
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for joining two entities with exclusions.
         * Combines multi-entity selection with property filtering.
         * 
         * <pre>
         * // Example:
         * Set<String> userExcl = new HashSet<>(Arrays.asList("passwordHash"));
         * Set<String> orderExcl = new HashSet<>(Arrays.asList("debugData"));
         * String sql = ACSB.selectFrom(User.class, "u", "user", userExcl,
         *                             Order.class, "o", "order", orderExcl)
         *                  .where("o.status = 'COMPLETED'")
         *                  .sql();
         * // Output: SELECT ... FROM USER u, ORDER o WHERE o.STATUS = 'COMPLETED'
         * </pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA class alias prefix for first entity columns
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB class alias prefix for second entity columns
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * Automatically generates the FROM clause based on the selections.
         * 
         * <pre>
         * // Example:
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, true, null)
         * );
         * String sql = ACSB.selectFrom(selections)
         *                  .where("u.verified = true")
         *                  .sql();
         * // Output: SELECT ... FROM USER u, ORDER o WHERE u.VERIFIED = true
         * </pre>
         *
         * @param multiSelects list of Selection objects defining what to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if multiSelects is invalid
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for a table.
         * This is a convenience method for counting rows.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.count("users")
         *                  .where("active = true")
         *                  .sql();
         * // Output: SELECT COUNT(*) FROM USERS WHERE ACTIVE = true
         * </pre>
         *
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for an entity class.
         * The table name is derived from the entity class.
         * 
         * <pre>
         * // Example:
         * String sql = ACSB.count(User.class)
         *                  .where("age >= 18")
         *                  .sql();
         * // Output: SELECT COUNT(*) FROM USER WHERE AGE >= 18
         * </pre>
         *
         * @param entityClass the entity class to count
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a Condition object into SQL with entity class mapping.
         * This method is used to generate SQL fragments from Condition objects.
         * 
         * <pre>
         * // Example:
         * Condition cond = CF.and(CF.eq("name", "John"), CF.gt("age", 18));
         * String sql = ACSB.parse(cond, User.class).sql();
         * // Output: NAME = 'John' AND AGE > 18
         * </pre>
         *
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for property name mapping (can be null)
         * @return a new SQLBuilder instance containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * SQL builder implementation with lower camel case naming policy.
     * 
     * <p>This class generates SQL with column names in lowerCamelCase format. It is marked as deprecated
     * and users should consider using one of the other naming policy implementations like {@link PSC}, 
     * {@link PAC}, {@link NSC}, etc.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Column names will be in lowerCamelCase
     * String sql = LCSB.select("firstName", "lastName")
     *                  .from("userAccount")
     *                  .where(CF.eq("userId", 1))
     *                  .sql();
     * // Output: SELECT firstName, lastName FROM userAccount WHERE userId = ?
     * }</pre>
     * 
     * @deprecated Use other naming policy implementations instead
     */
    @Deprecated
    public static class LCSB extends SQLBuilder {

        LCSB() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.SQL);
        }

        protected static LCSB createInstance() {
            return new LCSB();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * 
         * <p>This method is a convenience wrapper that delegates to {@link #insert(String...)} 
         * with a single element array.</p>
         * 
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * @see #insert(String...)
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified columns.
         * 
         * <p>This method initializes a new SQLBuilder for INSERT operations with the specified
         * column names. The actual values should be provided later using the VALUES clause.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.insert("firstName", "lastName", "email")
         *                  .into("users")
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified columns collection.
         * 
         * <p>This method is similar to {@link #insert(String...)} but accepts a Collection
         * of column names instead of varargs.</p>
         * 
         * @param propOrColumnNames collection of property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with property name-value pairs.
         * 
         * <p>This method allows direct specification of column names and their corresponding
         * values as a Map.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * 
         * String sql = LCSB.insert(props)
         *                  .into("users")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName) VALUES (?, ?)
         * }</pre>
         * 
         * @param props map of property names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object.
         * 
         * <p>This method extracts property values from the given entity object and creates
         * an INSERT statement. All non-transient properties of the entity will be included.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * String sql = LCSB.insert(user)
         *                  .into("users")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * @see #insert(Object, Set)
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object with excluded properties.
         * 
         * <p>This method is similar to {@link #insert(Object)} but allows exclusion of
         * specific properties from the INSERT statement.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * Set<String> excluded = new HashSet<>(Arrays.asList("createdDate", "modifiedDate"));
         * 
         * String sql = LCSB.insert(user, excluded)
         *                  .into("users")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for an entity class.
         * 
         * <p>This method generates an INSERT template for the specified entity class,
         * including all insertable properties of the class.</p>
         * 
         * @param entityClass the entity class to create INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * @see #insert(Class, Set)
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for an entity class with excluded properties.
         * 
         * <p>This method generates an INSERT template for the specified entity class,
         * excluding the specified properties. Properties marked with {@link ReadOnly},
         * {@link ReadOnlyId}, or {@link Transient} annotations are automatically excluded.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
         * String sql = LCSB.insert(User.class, excluded)
         *                  .into("users")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to create INSERT for
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class.
         * 
         * <p>This is a convenience method that combines {@link #insert(Class)} and
         * {@link #into(Class)} operations.</p>
         * 
         * @param entityClass the entity class to insert into
         * @return a new SQLBuilder instance configured for INSERT INTO operation
         * 
         * @see #insertInto(Class, Set)
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class with excluded properties.
         * 
         * <p>This is a convenience method that combines {@link #insert(Class, Set)} and
         * {@link #into(Class)} operations. The table name is derived from the entity class.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("id"));
         * String sql = LCSB.insertInto(User.class, excluded)
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to insert into
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT INTO operation
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple entities or property maps.
         * 
         * <p>This method generates MySQL-style batch insert SQL for inserting multiple
         * rows in a single statement. The input collection can contain either entity
         * objects or Map instances.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * 
         * String sql = LCSB.batchInsert(users)
         *                  .into("users")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName) VALUES (?, ?), (?, ?)
         * }</pre>
         * 
         * @param propsList collection of entities or property maps to batch insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * 
         * @apiNote This is a beta feature and may be subject to change
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * <p>This method initializes a new SQLBuilder for UPDATE operations on the
         * specified table. The columns to update should be specified using the
         * {@code set()} method.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.update("users")
         *                  .set("lastName", "Smith")
         *                  .where(CF.eq("id", 123))
         *                  .sql();
         * // Output: UPDATE users SET lastName = ? WHERE id = ?
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class context.
         * 
         * <p>This method is similar to {@link #update(String)} but also provides entity
         * class information for better type safety and property name mapping.</p>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class corresponding to the table
         * @return a new SQLBuilder instance configured for UPDATE operation
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class and includes
         * all updatable properties in the UPDATE statement.</p>
         * 
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * 
         * @see #update(Class, Set)
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class with excluded properties.
         * 
         * <p>This method generates an UPDATE template for the specified entity class,
         * excluding the specified properties. Properties marked with {@link ReadOnly},
         * {@link NonUpdatable}, or {@link Transient} annotations are automatically excluded.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
         * String sql = LCSB.update(User.class, excluded)
         *                  .set("firstName", "John")
         *                  .where(CF.eq("id", 123))
         *                  .sql();
         * // Output: UPDATE users SET firstName = ? WHERE id = ?
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the update
         * @return a new SQLBuilder instance configured for UPDATE operation
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table.
         * 
         * <p>This method initializes a new SQLBuilder for DELETE operations on the
         * specified table.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.deleteFrom("users")
         *                  .where(CF.eq("status", "inactive"))
         *                  .sql();
         * // Output: DELETE FROM users WHERE status = ?
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table with entity class context.
         * 
         * <p>This method is similar to {@link #deleteFrom(String)} but also provides entity
         * class information for better type safety in WHERE conditions.</p>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class corresponding to the table
         * @return a new SQLBuilder instance configured for DELETE operation
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class using the
         * configured naming policy.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.deleteFrom(User.class)
         *                  .where(CF.lt("lastLoginDate", oneYearAgo))
         *                  .sql();
         * // Output: DELETE FROM users WHERE lastLoginDate < ?
         * }</pre>
         * 
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a custom select expression.
         * 
         * <p>This method allows specification of complex SELECT expressions including
         * aggregate functions, calculated fields, etc.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.select("COUNT(*) as total, AVG(salary) as avgSalary")
         *                  .from("employees")
         *                  .sql();
         * // Output: SELECT COUNT(*) as total, AVG(salary) as avgSalary FROM employees
         * }</pre>
         * 
         * @param selectPart the SELECT expression or clause
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified columns.
         * 
         * <p>This method initializes a SELECT query with the specified column names.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.select("firstName", "lastName", "email")
         *                  .from("users")
         *                  .where(CF.eq("active", true))
         *                  .sql();
         * // Output: SELECT firstName, lastName, email FROM users WHERE active = ?
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified columns collection.
         * 
         * <p>This method is similar to {@link #select(String...)} but accepts a Collection
         * of column names instead of varargs.</p>
         * 
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * 
         * <p>This method allows specification of column names with their aliases for
         * the SELECT statement.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * 
         * String sql = LCSB.select(aliases)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT firstName AS fname, lastName AS lname FROM users
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class.
         * 
         * <p>This method generates a SELECT statement including all properties of the
         * specified entity class, excluding any transient fields.</p>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT operation
         * 
         * @see #select(Class, boolean)
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with sub-entity option.
         * 
         * <p>When includeSubEntityProperties is true, properties of sub-entities
         * (nested objects) will also be included in the SELECT statement.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * // If User has an Address sub-entity
         * String sql = LCSB.select(User.class, true)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT firstName, lastName, address.street, address.city FROM users
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with excluded properties.
         * 
         * <p>This method generates a SELECT statement for the entity class, excluding
         * the specified properties.</p>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with sub-entity option and excluded properties.
         * 
         * <p>This method provides full control over which properties to include in the
         * SELECT statement, with options for sub-entities and property exclusion.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("password", "secretKey"));
         * String sql = LCSB.select(User.class, true, excluded)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT firstName, lastName, email, address.street, address.city FROM users
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class.
         * 
         * <p>This is a convenience method that combines SELECT and FROM operations.
         * The table name is derived from the entity class.</p>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         * 
         * @see #selectFrom(Class, boolean)
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with table alias.
         * 
         * <p>This method allows specification of a table alias for use in complex queries
         * with joins or subqueries.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.selectFrom(User.class, "u")
         *                  .where(CF.eq("u.active", true))
         *                  .sql();
         * // Output: SELECT u.firstName, u.lastName FROM users u WHERE u.active = ?
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with sub-entity option.
         * 
         * <p>This is a convenience method that combines SELECT and FROM operations
         * with the option to include sub-entity properties.</p>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and sub-entity option.
         * 
         * <p>This method combines table aliasing with sub-entity property inclusion
         * for complex query construction.</p>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with excluded properties.
         * 
         * <p>This is a convenience method that combines SELECT and FROM operations
         * while excluding specified properties.</p>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and excluded properties.
         * 
         * <p>This method provides aliasing capability while excluding specified properties
         * from the SELECT statement.</p>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity option and excluded properties.
         * 
         * <p>This method provides a convenient way to create a complete SELECT FROM
         * statement with control over sub-entities and property exclusion.</p>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with full configuration options.
         * 
         * <p>This method provides complete control over the SELECT FROM statement generation,
         * including table alias, sub-entity properties, and property exclusion. When
         * sub-entities are included, appropriate joins will be generated.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("password"));
         * String sql = LCSB.selectFrom(User.class, "u", true, excluded)
         *                  .where(CF.eq("u.active", true))
         *                  .sql();
         * // Output: SELECT u.firstName, u.lastName, a.street, a.city 
         * //         FROM users u 
         * //         LEFT JOIN addresses a ON u.addressId = a.id 
         * //         WHERE u.active = ?
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases.
         * 
         * <p>This method is designed for queries that need to select from multiple tables,
         * typically used in JOIN operations. Each entity class can have both a table alias
         * and a class alias for property disambiguation.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.select(User.class, "u", "user", 
         *                         Order.class, "o", "order")
         *                  .from("users u")
         *                  .join("orders o").on("u.id = o.userId")
         *                  .sql();
         * // Output: SELECT u.firstName AS "user.firstName", u.lastName AS "user.lastName",
         * //                o.orderId AS "order.orderId", o.orderDate AS "order.orderDate"
         * //         FROM users u
         * //         JOIN orders o ON u.id = o.userId
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for the first entity (used in result mapping)
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for the second entity (used in result mapping)
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases and excluded properties.
         * 
         * <p>This method extends {@link #select(Class, String, String, Class, String, String)}
         * by allowing exclusion of specific properties from each entity class.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> userExclusions = new HashSet<>(Arrays.asList("password"));
         * Set<String> orderExclusions = new HashSet<>(Arrays.asList("internalNotes"));
         * 
         * String sql = LCSB.select(User.class, "u", "user", userExclusions,
         *                         Order.class, "o", "order", orderExclusions)
         *                  .from("users u")
         *                  .join("orders o").on("u.id = o.userId")
         *                  .sql();
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * 
         * <p>This method provides the most flexible way to construct complex SELECT
         * statements involving multiple entities with different configurations. Each
         * Selection object encapsulates the configuration for one entity.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, excludedProps)
         * );
         * 
         * String sql = LCSB.select(selections)
         *                  .from("users u")
         *                  .join("orders o").on("u.id = o.userId")
         *                  .join("products p").on("o.productId = p.id")
         *                  .sql();
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining the entities to select
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with aliases.
         * 
         * <p>This is a convenience method that combines the multi-entity SELECT with
         * automatic FROM clause generation.</p>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT FROM operation
         * 
         * @see #select(Class, String, String, Class, String, String)
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with aliases and excluded properties.
         * 
         * <p>This is a convenience method that combines the multi-entity SELECT with
         * automatic FROM clause generation, while allowing property exclusions.</p>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * 
         * <p>This method automatically generates the appropriate FROM clause based on
         * the provided Selection configurations. It's the most convenient way to build
         * complex multi-table queries.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null)
         * );
         * 
         * String sql = LCSB.selectFrom(selections)
         *                  .where(CF.eq("u.active", true))
         *                  .sql();
         * // Output: SELECT u.firstName AS "user.firstName", ... 
         * //         FROM users u, orders o 
         * //         WHERE u.active = ?
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining the entities to select
         * @return a new SQLBuilder instance configured for multi-table SELECT FROM operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         * 
         * <p>This is a convenience method for creating COUNT queries.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.count("users")
         *                  .where(CF.eq("active", true))
         *                  .sql();
         * // Output: SELECT COUNT(*) FROM users WHERE active = ?
         * }</pre>
         * 
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class and creates
         * a COUNT query.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = LCSB.count(User.class)
         *                  .where(CF.between("age", 18, 65))
         *                  .sql();
         * // Output: SELECT COUNT(*) FROM users WHERE age BETWEEN ? AND ?
         * }</pre>
         * 
         * @param entityClass the entity class to count
         * @return a new SQLBuilder instance configured for COUNT operation
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context.
         * 
         * <p>This method is useful for generating just the SQL representation of a
         * condition without building a complete statement. It's typically used for
         * debugging or building dynamic query parts.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("active", true),
         *     CF.gt("age", 18)
         * );
         * 
         * String sql = LCSB.parse(cond, User.class).sql();
         * // Output: active = ? AND age > ?
         * }</pre>
         * 
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for property name resolution
         * @return a new SQLBuilder instance containing only the condition SQL
         * @throws IllegalArgumentException if cond is null
         * 
         * @see ConditionFactory
         * @see ConditionFactory.CF
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Parameterized SQL builder with no naming policy transformation.
     * 
     * <p>This builder generates parameterized SQL statements using '?' placeholders and preserves
     * the original casing of property and column names without any transformation.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Property names are preserved as-is
     * String sql = PSB.select("first_Name", "last_NaMe")
     *                 .from("account")
     *                 .where(CF.eq("last_NaMe", 1))
     *                 .sql();
     * // Output: SELECT first_Name, last_NaMe FROM account WHERE last_NaMe = ?
     * }</pre>
     */
    public static class PSB extends SQLBuilder {

        PSB() {
            super(NamingPolicy.NO_CHANGE, SQLPolicy.PARAMETERIZED_SQL);
        }

        protected static PSB createInstance() {
            return new PSB();
        }

        /**
         * Creates an INSERT statement builder for a single column expression.
         * 
         * <p>This method is a convenience wrapper that internally calls {@link #insert(String...)}
         * with a single-element array.</p>
         *
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.insert("user_name").into("users");
         * }</pre>
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement builder for the specified columns.
         * 
         * <p>The column names are used as-is without any naming transformation.
         * The actual values must be provided later using the {@code values()} method.</p>
         *
         * @param propOrColumnNames the property or column names to include in the INSERT statement
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.insert("name", "email", "age")
         *                         .into("users")
         *                         .values("John", "john@example.com", 25);
         * }</pre>
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement builder for the specified collection of columns.
         * 
         * <p>This method allows using any Collection implementation (List, Set, etc.) to specify
         * the columns for insertion.</p>
         *
         * @param propOrColumnNames collection of property or column names to include in the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * <pre>{@code
         * List<String> columns = Arrays.asList("name", "email", "age");
         * SQLBuilder builder = PSB.insert(columns).into("users");
         * }</pre>
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement builder using a map of property names to values.
         * 
         * <p>The map keys represent column names and the values are the corresponding values
         * to be inserted. This provides a convenient way to specify both columns and values
         * in a single call.</p>
         *
         * @param props map where keys are column names and values are the values to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         * 
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("name", "John");
         * data.put("age", 25);
         * SQLBuilder builder = PSB.insert(data).into("users");
         * }</pre>
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement builder from an entity object.
         * 
         * <p>All non-null properties of the entity will be included in the INSERT statement,
         * except those marked with {@code @Transient}, {@code @ReadOnly}, or {@code @ReadOnlyId}
         * annotations.</p>
         *
         * @param entity the entity object whose properties will be inserted
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         * 
         * <pre>{@code
         * User user = new User("John", "john@example.com", 25);
         * SQLBuilder builder = PSB.insert(user).into("users");
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement builder from an entity object with excluded properties.
         * 
         * <p>Properties can be excluded from the INSERT statement by specifying their names
         * in the excludedPropNames set. This is useful when certain properties should not
         * be inserted even if they have values.</p>
         *
         * @param entity the entity object whose properties will be inserted
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         * 
         * <pre>{@code
         * User user = new User();
         * Set<String> excluded = N.asSet("createdTime", "updatedTime");
         * SQLBuilder builder = PSB.insert(user, excluded).into("users");
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement builder for an entity class.
         * 
         * <p>This method generates an INSERT template for all insertable properties of the
         * specified entity class. Properties marked with {@code @Transient}, {@code @ReadOnly},
         * or {@code @ReadOnlyId} annotations are automatically excluded.</p>
         *
         * @param entityClass the entity class to generate INSERT statement for
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.insert(User.class)
         *                         .into("users")
         *                         .values("John", "john@example.com", 25);
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement builder for an entity class with excluded properties.
         * 
         * <p>Generates an INSERT template excluding the specified properties in addition to
         * those automatically excluded by annotations.</p>
         *
         * @param entityClass the entity class to generate INSERT statement for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * Set<String> excluded = N.asSet("version", "lastModified");
         * SQLBuilder builder = PSB.insert(User.class, excluded).into("users");
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO statement builder for an entity class.
         * 
         * <p>This is a convenience method that combines {@link #insert(Class)} and {@link #into(Class)}
         * in a single call. The table name is derived from the entity class name or its {@code @Table}
         * annotation.</p>
         *
         * @param entityClass the entity class to generate INSERT INTO statement for
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.insertInto(User.class)
         *                         .values("John", "john@example.com", 25);
         * }</pre>
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO statement builder for an entity class with excluded properties.
         * 
         * <p>Combines INSERT and INTO operations while excluding specified properties from
         * the generated statement.</p>
         *
         * @param entityClass the entity class to generate INSERT INTO statement for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * Set<String> excluded = N.asSet("id", "version");
         * SQLBuilder builder = PSB.insertInto(User.class, excluded)
         *                         .values("John", "john@example.com");
         * }</pre>
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT statement builder for multiple records.
         * 
         * <p>Generates MySQL-style batch insert SQL that can insert multiple rows in a single
         * statement. The input collection can contain either entity objects or Map instances
         * representing the data to insert.</p>
         *
         * @param propsList collection of entities or property maps to insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * 
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "john@example.com"),
         *     new User("Jane", "jane@example.com")
         * );
         * SQLBuilder builder = PSB.batchInsert(users).into("users");
         * // Generates: INSERT INTO users (name, email) VALUES (?, ?), (?, ?)
         * }</pre>
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement builder for the specified table.
         * 
         * <p>The table name is used as-is without any transformation. Columns to update
         * must be specified using the {@code set()} method.</p>
         *
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.update("users")
         *                         .set("name", "email")
         *                         .where(CF.eq("id", 1));
         * }</pre>
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement builder for a table with entity class mapping.
         * 
         * <p>Specifying the entity class enables property name transformation and validation
         * based on the entity's field definitions.</p>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName or entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.update("users", User.class)
         *                         .set("name", "email")
         *                         .where(CF.eq("id", 1));
         * }</pre>
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement builder for an entity class.
         * 
         * <p>The table name is derived from the entity class name or its {@code @Table} annotation.
         * All updatable properties (excluding those marked with {@code @NonUpdatable}, {@code @ReadOnly},
         * or {@code @Transient}) are included in the SET clause.</p>
         *
         * @param entityClass the entity class to generate UPDATE statement for
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.update(User.class)
         *                         .where(CF.eq("id", 1));
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement builder for an entity class with excluded properties.
         * 
         * <p>Generates an UPDATE statement excluding the specified properties in addition to
         * those automatically excluded by annotations.</p>
         *
         * @param entityClass the entity class to generate UPDATE statement for
         * @param excludedPropNames set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * Set<String> excluded = N.asSet("createdTime", "createdBy");
         * SQLBuilder builder = PSB.update(User.class, excluded)
         *                         .where(CF.eq("id", 1));
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement builder for the specified table.
         * 
         * <p>The table name is used as-is without any transformation. WHERE conditions
         * should be added to avoid deleting all records.</p>
         *
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.deleteFrom("users")
         *                         .where(CF.eq("status", "inactive"));
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM statement builder for a table with entity class mapping.
         * 
         * <p>Specifying the entity class enables property name validation in WHERE conditions
         * based on the entity's field definitions.</p>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName or entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.deleteFrom("users", User.class)
         *                         .where(CF.eq("lastLogin", null));
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement builder for an entity class.
         * 
         * <p>The table name is derived from the entity class name or its {@code @Table} annotation.</p>
         *
         * @param entityClass the entity class to generate DELETE FROM statement for
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.deleteFrom(User.class)
         *                         .where(CF.eq("id", 1));
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement builder for a single column or expression.
         * 
         * <p>The selectPart can be a simple column name or a complex expression including
         * functions, aliases, etc.</p>
         *
         * @param selectPart the column name or expression to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.select("COUNT(*)").from("users");
         * SQLBuilder builder2 = PSB.select("name AS userName").from("users");
         * }</pre>
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement builder for multiple columns.
         * 
         * <p>Column names are used as-is without any transformation. Each column can be
         * a simple name or include expressions and aliases.</p>
         *
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.select("id", "name", "email")
         *                         .from("users")
         *                         .where(CF.gt("age", 18));
         * }</pre>
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement builder for a collection of columns.
         * 
         * <p>This method allows using any Collection implementation (List, Set, etc.) to specify
         * the columns to select.</p>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * <pre>{@code
         * List<String> columns = Arrays.asList("id", "name", "email");
         * SQLBuilder builder = PSB.select(columns).from("users");
         * }</pre>
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement builder with column aliases.
         * 
         * <p>The map keys represent the column names or expressions to select, and the values
         * are their corresponding aliases in the result set.</p>
         *
         * @param propOrColumnNameAliases map where keys are column names and values are aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         * 
         * <pre>{@code
         * Map<String, String> aliases = new LinkedHashMap<>();
         * aliases.put("u.name", "userName");
         * aliases.put("u.email", "userEmail");
         * SQLBuilder builder = PSB.select(aliases).from("users u");
         * // Generates: SELECT u.name AS userName, u.email AS userEmail FROM users u
         * }</pre>
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement builder for all properties of an entity class.
         * 
         * <p>Selects all properties of the entity class except those marked with
         * {@code @Transient} annotation. Sub-entity properties are not included by default.</p>
         *
         * @param entityClass the entity class whose properties to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.select(User.class)
         *                         .from("users")
         *                         .where(CF.eq("active", true));
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement builder for an entity class with sub-entity option.
         * 
         * <p>When includeSubEntityProperties is true, properties of sub-entities (nested objects)
         * are also included in the selection with appropriate aliasing.</p>
         *
         * @param entityClass the entity class whose properties to select
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * // If User has an Address sub-entity
         * SQLBuilder builder = PSB.select(User.class, true)
         *                         .from("users u")
         *                         .join("addresses a").on("u.address_id = a.id");
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement builder for an entity class with excluded properties.
         * 
         * <p>Generates a SELECT statement excluding the specified properties in addition to
         * those automatically excluded by {@code @Transient} annotation.</p>
         *
         * @param entityClass the entity class whose properties to select
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "secretKey");
         * SQLBuilder builder = PSB.select(User.class, excluded)
         *                         .from("users");
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement builder with full control over entity property selection.
         * 
         * <p>Provides complete control over which properties to include or exclude, including
         * sub-entity properties.</p>
         *
         * @param entityClass the entity class whose properties to select
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * Set<String> excluded = N.asSet("internalNotes");
         * SQLBuilder builder = PSB.select(User.class, true, excluded)
         *                         .from("users u")
         *                         .join("addresses a").on("u.address_id = a.id");
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement builder for an entity class.
         * 
         * <p>This is a convenience method that combines SELECT and FROM operations.
         * The table name is derived from the entity class name or its {@code @Table} annotation.</p>
         *
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @return a new SQLBuilder instance with both SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class)
         *                         .where(CF.eq("status", "active"));
         * // Equivalent to: PSB.select(User.class).from(User.class)
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement builder with a table alias.
         * 
         * <p>The alias is used to qualify column names in the generated SQL, which is useful
         * for joins and subqueries.</p>
         *
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param alias the table alias to use in the FROM clause
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class, "u")
         *                         .join("orders o").on("u.id = o.user_id");
         * // Generates: SELECT u.id, u.name, ... FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement builder with sub-entity properties option.
         * 
         * <p>When includeSubEntityProperties is true, appropriate joins are automatically
         * generated for sub-entities.</p>
         *
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class, true)
         *                         .where(CF.isNotNull("address.city"));
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement builder with alias and sub-entity properties option.
         * 
         * <p>Combines table aliasing with sub-entity property inclusion for complex queries.</p>
         *
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param alias the table alias to use in the FROM clause
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class, "u", true)
         *                         .where(CF.like("u.name", "John%"));
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement builder with excluded properties.
         * 
         * <p>Convenience method for creating a complete SELECT FROM statement while excluding
         * specific properties.</p>
         *
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * Set<String> excluded = N.asSet("largeBlob", "internalData");
         * SQLBuilder builder = PSB.selectFrom(User.class, excluded)
         *                         .limit(10);
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement builder with alias and excluded properties.
         * 
         * <p>Provides aliasing capability while excluding specific properties from selection.</p>
         *
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param alias the table alias to use in the FROM clause
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * Set<String> excluded = N.asSet("password");
         * SQLBuilder builder = PSB.selectFrom(User.class, "u", excluded)
         *                         .join("roles r").on("u.role_id = r.id");
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement builder with sub-entities and excluded properties.
         * 
         * <p>Allows including sub-entity properties while excluding specific properties
         * from the selection.</p>
         *
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * Set<String> excluded = N.asSet("user.password", "user.salt");
         * SQLBuilder builder = PSB.selectFrom(Order.class, true, excluded);
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement builder with full control over all options.
         * 
         * <p>This method provides complete control over the SELECT FROM generation, including
         * aliasing, sub-entity properties, and property exclusion. When sub-entities are included,
         * appropriate JOIN clauses may be automatically generated.</p>
         *
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param alias the table alias to use in the FROM clause
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * Set<String> excluded = N.asSet("audit.createdBy", "audit.modifiedBy");
         * SQLBuilder builder = PSB.selectFrom(Product.class, "p", true, excluded)
         *                         .where(CF.gt("p.price", 100))
         *                         .orderBy("p.name");
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement builder for joining two entity classes.
         * 
         * <p>This method facilitates creating SELECT statements that retrieve data from two
         * related tables. Each entity class can have its own table alias and class alias
         * for property prefixing in the result set.</p>
         *
         * @param entityClassA the first entity class to select from
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the prefix for properties of the first entity in results
         * @param entityClassB the second entity class to select from
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the prefix for properties of the second entity in results
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if any required parameter is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.select(User.class, "u", "user", 
         *                                Order.class, "o", "order")
         *                         .from("users u")
         *                         .join("orders o").on("u.id = o.user_id");
         * // Properties will be prefixed: user.name, user.email, order.id, order.total
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement builder for joining two entities with excluded properties.
         * 
         * <p>Provides fine-grained control over which properties to include from each entity
         * when performing joins.</p>
         *
         * @param entityClassA the first entity class to select from
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the prefix for properties of the first entity in results
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class to select from
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the prefix for properties of the second entity in results
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if entity classes are null
         * 
         * <pre>{@code
         * Set<String> userExclude = N.asSet("password", "salt");
         * Set<String> orderExclude = N.asSet("internalNotes");
         * 
         * SQLBuilder builder = PSB.select(User.class, "u", "user", userExclude,
         *                                Order.class, "o", "order", orderExclude)
         *                         .from("users u")
         *                         .join("orders o").on("u.id = o.user_id");
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement builder for multiple entity selections.
         * 
         * <p>This method provides the most flexibility for complex multi-table queries,
         * allowing each table to have its own configuration including aliases, column
         * selections, and sub-entity handling.</p>
         *
         * @param multiSelects list of Selection configurations for each table
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if multiSelects is null, empty, or contains invalid selections
         * 
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", Arrays.asList("id", "name"), false, null),
         *     new Selection(Order.class, "o", "order", null, true, N.asSet("deleted"))
         * );
         * 
         * SQLBuilder builder = PSB.select(selections)
         *                         .from("users u")
         *                         .join("orders o").on("u.id = o.user_id");
         * }</pre>
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for joining two entity classes.
         * 
         * <p>This convenience method combines SELECT and FROM operations for two-table joins.
         * The FROM clause is automatically generated based on the entity configurations.</p>
         *
         * @param entityClassA the first entity class to select from
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the prefix for properties of the first entity in results
         * @param entityClassB the second entity class to select from
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the prefix for properties of the second entity in results
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if any required parameter is null
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class, "u", "user",
         *                                    Order.class, "o", "order")
         *                         .where(CF.eq("u.status", "active"));
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for two entities with excluded properties.
         * 
         * <p>Generates a complete SELECT FROM statement for joining two tables while
         * excluding specified properties from each entity.</p>
         *
         * @param entityClassA the first entity class to select from
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the prefix for properties of the first entity in results
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class to select from
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the prefix for properties of the second entity in results
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entity classes are null
         * 
         * <pre>{@code
         * Set<String> excludeUser = N.asSet("passwordHash");
         * Set<String> excludeOrder = N.asSet("internalId");
         * 
         * SQLBuilder builder = PSB.selectFrom(User.class, "u", "user", excludeUser,
         *                                    Order.class, "o", "order", excludeOrder)
         *                         .where(CF.between("o.created", startDate, endDate));
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement builder for multiple entity selections.
         * 
         * <p>This method automatically generates both SELECT and FROM clauses based on
         * the provided Selection configurations, including proper table aliasing and joins
         * for sub-entities when specified.</p>
         *
         * @param multiSelects list of Selection configurations for each table
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if multiSelects is null, empty, or invalid
         * 
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Customer.class, "c", "customer", null, true, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", Arrays.asList("name", "price"), false, null)
         * );
         * 
         * SQLBuilder builder = PSB.selectFrom(selections)
         *                         .where(CF.and(
         *                             CF.eq("c.status", "premium"),
         *                             CF.gt("o.total", 1000)
         *                         ));
         * }</pre>
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query builder for the specified table.
         * 
         * <p>This is a convenience method for creating count queries without specifying
         * the COUNT(*) expression explicitly.</p>
         *
         * @param tableName the name of the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT query
         * @throws IllegalArgumentException if tableName is null or empty
         * 
         * <pre>{@code
         * SQLBuilder builder = PSB.count("users")
         *                         .where(CF.eq("status", "active"));
         * // Generates: SELECT COUNT(*) FROM users WHERE status = ?
         * }</pre>
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query builder for an entity class.
         * 
         * <p>The table name is derived from the entity class name or its {@code @Table} annotation.</p>
         *
         * @param entityClass the entity class to count rows for
         * @return a new SQLBuilder instance configured for COUNT query
         * @throws IllegalArgumentException if entityClass is null
         * 
         * <pre>{@code
         * long count = PSB.count(User.class)
         *                 .where(CF.like("email", "%@example.com"))
         *                 .queryForSingleResult(Long.class);
         * }</pre>
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context.
         * 
         * <p>This method is useful for generating SQL fragments from Condition objects,
         * particularly for debugging or when building complex dynamic queries. The entity
         * class provides context for property name resolution.</p>
         *
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for property name context
         * @return a new SQLBuilder instance containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         * 
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("status", "active"),
         *     CF.gt("age", 18)
         * );
         * String sql = PSB.parse(cond, User.class).sql();
         * // Result: "status = ? AND age > ?"
         * }</pre>
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Parameterized SQL builder with snake_case (lower case with underscore) field/column naming strategy.
     * 
     * <p>PSC (Parameterized Snake Case) generates SQL with placeholder parameters (?) and converts
     * property names from camelCase to snake_case. This is the most commonly used SQL builder
     * for applications using standard SQL databases with snake_case column naming conventions.</p>
     * 
     * <p><b>Naming Convention:</b></p>
     * <ul>
     *   <li>Property: firstName → Column: first_name</li>
     *   <li>Property: accountNumber → Column: account_number</li>
     *   <li>Property: isActive → Column: is_active</li>
     * </ul>
     * 
     * <p><b>Basic Usage Examples:</b></p>
     * <pre>{@code
     * // Simple SELECT
     * String sql = PSC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = ?
     * 
     * // INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = PSC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (first_name, last_name) VALUES (?, ?)
     * 
     * // UPDATE with specific fields
     * String sql = PSC.update("account")
     *                 .set("firstName", "John")
     *                 .set("lastName", "Smith")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: UPDATE account SET first_name = ?, last_name = ? WHERE id = ?
     * }</pre>
     * 
     * <p><b>Advanced Examples:</b></p>
     * <pre>{@code
     * // SELECT with entity class
     * String sql = PSC.selectFrom(Account.class)
     *                 .where(CF.gt("createdDate", new Date()))
     *                 .orderBy("lastName ASC")
     *                 .limit(10)
     *                 .sql();
     * 
     * // Batch INSERT
     * List<Account> accounts = Arrays.asList(account1, account2, account3);
     * SP sqlPair = PSC.batchInsert(accounts).into("account").pair();
     * // sqlPair.sql: INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?), (?, ?)
     * // sqlPair.parameters: ["John", "Doe", "Jane", "Smith", "Bob", "Johnson"]
     * 
     * // Complex JOIN query
     * String sql = PSC.select("a.id", "a.firstName", "COUNT(o.id) AS orderCount")
     *                 .from("account a")
     *                 .leftJoin("orders o").on("a.id = o.account_id")
     *                 .groupBy("a.id", "a.firstName")
     *                 .having(CF.gt("COUNT(o.id)", 5))
     *                 .sql();
     * }</pre>
     * 
     * @see SQLBuilder
     * @see NSC
     */
    public static class PSC extends SQLBuilder {

        PSC() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.PARAMETERIZED_SQL);
        }

        protected static PSC createInstance() {
            return new PSC();
        }

        /**
         * Creates an INSERT statement for a single column expression.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.insert("firstName").into("account").sql();
         * // Output: INSERT INTO account (first_name) VALUES (?)
         * }</pre>
         * 
         * @param expr The column name or expression to insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for multiple columns.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.insert("firstName", "lastName", "email")
         *                 .into("account")
         *                 .sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param propOrColumnNames The property or column names to insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for a collection of columns.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = PSC.insert(columns).into("account").sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param propOrColumnNames Collection of property or column names to insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement from a map of property names and values.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * SP sqlPair = PSC.insert(props).into("account").pair();
         * // sqlPair.sql: INSERT INTO account (first_name, last_name) VALUES (?, ?)
         * // sqlPair.parameters: ["John", "Doe"]
         * }</pre>
         * 
         * @param props Map of property names to their values
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement from an entity object.
         * 
         * <p>This method extracts all non-null properties from the entity object,
         * excluding those marked with @Transient, @ReadOnly, or @ReadOnlyId annotations.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setEmail("john.doe@example.com");
         * 
         * SP sqlPair = PSC.insert(account).into("account").pair();
         * // sqlPair.sql: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john.doe@example.com"]
         * }</pre>
         * 
         * @param entity The entity object to insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement from an entity object with excluded properties.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setEmail("john.doe@example.com");
         * account.setCreatedDate(new Date());
         * 
         * Set<String> excluded = N.asSet("createdDate");
         * SP sqlPair = PSC.insert(account, excluded).into("account").pair();
         * // sqlPair.sql: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john.doe@example.com"]
         * }</pre>
         * 
         * @param entity The entity object to insert
         * @param excludedPropNames Set of property names to exclude from the insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for an entity class.
         * 
         * <p>This method includes all properties of the entity class that are suitable for insertion,
         * excluding those marked with @Transient, @ReadOnly, or @ReadOnlyId annotations.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.insert(Account.class).into("account").sql();
         * // Output: INSERT INTO account (first_name, last_name, email, created_date) VALUES (?, ?, ?, ?)
         * }</pre>
         * 
         * @param entityClass The entity class to generate INSERT for
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class with excluded properties.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("createdDate", "modifiedDate");
         * String sql = PSC.insert(Account.class, excluded).into("account").sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass The entity class to generate INSERT for
         * @param excludedPropNames Set of property names to exclude from the insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO statement for an entity class.
         * 
         * <p>This is a convenience method that combines insert() and into() operations.
         * The table name is derived from the entity class name or @Table annotation.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.insertInto(Account.class).sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass The entity class to generate INSERT INTO for
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO statement for an entity class with excluded properties.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("id", "createdDate");
         * String sql = PSC.insertInto(Account.class, excluded).sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass The entity class to generate INSERT INTO for
         * @param excludedPropNames Set of property names to exclude from the insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generates a MySQL-style batch INSERT statement.
         * 
         * <p>This method creates an efficient batch insert statement with multiple value sets
         * in a single INSERT statement, which is particularly useful for MySQL databases.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "Doe"),
         *     new Account("Jane", "Smith"),
         *     new Account("Bob", "Johnson")
         * );
         * 
         * SP sqlPair = PSC.batchInsert(accounts).into("account").pair();
         * // sqlPair.sql: INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?), (?, ?)
         * // sqlPair.parameters: ["John", "Doe", "Jane", "Smith", "Bob", "Johnson"]
         * }</pre>
         * 
         * @param propsList List of entities or property maps to insert
         * @return A new SQLBuilder instance for method chaining
         * @deprecated This feature is in beta and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.update("account")
         *                 .set("firstName", "John")
         *                 .set("lastName", "Smith")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET first_name = ?, last_name = ? WHERE id = ?
         * }</pre>
         * 
         * @param tableName The name of the table to update
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class mapping.
         * 
         * <p>The entity class provides property-to-column name mapping information.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.update("account", Account.class)
         *                 .set("firstName", "John")
         *                 .set("lastModified", new Date())
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET first_name = ?, last_modified = ? WHERE id = ?
         * }</pre>
         * 
         * @param tableName The name of the table to update
         * @param entityClass The entity class for property mapping
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.
         * All updatable properties are included by default.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.update(Account.class)
         *                 .set("status", "active")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET status = ? WHERE id = ?
         * }</pre>
         * 
         * @param entityClass The entity class to update
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class with excluded properties.
         * 
         * <p>Properties marked with @NonUpdatable or @ReadOnly are automatically excluded.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("createdDate", "createdBy");
         * String sql = PSC.update(Account.class, excluded)
         *                 .set(account)
         *                 .where(CF.eq("id", account.getId()))
         *                 .sql();
         * }</pre>
         * 
         * @param entityClass The entity class to update
         * @param excludedPropNames Set of property names to exclude from the update
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for a table.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.deleteFrom("account")
         *                 .where(CF.eq("status", "inactive"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE status = ?
         * }</pre>
         * 
         * @param tableName The name of the table to delete from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for a table with entity class mapping.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.deleteFrom("account", Account.class)
         *                 .where(CF.lt("lastLoginDate", thirtyDaysAgo))
         *                 .sql();
         * // Output: DELETE FROM account WHERE last_login_date < ?
         * }</pre>
         * 
         * @param tableName The name of the table to delete from
         * @param entityClass The entity class for property mapping
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.deleteFrom(Account.class)
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id = ?
         * }</pre>
         * 
         * @param entityClass The entity class to delete from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression.
         * 
         * <p>This method is useful for complex select expressions or aggregate functions.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.select("COUNT(*)")
         *                 .from("account")
         *                 .where(CF.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE status = ?
         * 
         * String sql2 = PSC.select("firstName || ' ' || lastName AS fullName")
         *                  .from("account")
         *                  .sql();
         * // Output: SELECT firstName || ' ' || lastName AS fullName FROM account
         * }</pre>
         * 
         * @param selectPart The select expression
         * @return A new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement with multiple columns.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.select("id", "firstName", "lastName", "email")
         *                 .from("account")
         *                 .where(CF.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = ?
         * }</pre>
         * 
         * @param propOrColumnNames The property or column names to select
         * @return A new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement with a collection of columns.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("id", "firstName", "lastName");
         * String sql = PSC.select(columns)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName" FROM account
         * }</pre>
         * 
         * @param propOrColumnNames Collection of property or column names to select
         * @return A new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with column aliases.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Map<String, String> columnAliases = new HashMap<>();
         * columnAliases.put("firstName", "fname");
         * columnAliases.put("lastName", "lname");
         * columnAliases.put("emailAddress", "email");
         * 
         * String sql = PSC.select(columnAliases)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT first_name AS "fname", last_name AS "lname", email_address AS "email" FROM account
         * }</pre>
         * 
         * @param propOrColumnNameAliases Map of property/column names to their aliases
         * @return A new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all properties of an entity class.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.select(Account.class)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email, created_date AS "createdDate" FROM account
         * }</pre>
         * 
         * @param entityClass The entity class to select properties from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with optional sub-entity properties.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * // Without sub-entities
         * String sql1 = PSC.select(Order.class, false)
         *                  .from("orders")
         *                  .sql();
         * 
         * // With sub-entities (includes nested object properties)
         * String sql2 = PSC.select(Order.class, true)
         *                  .from("orders")
         *                  .sql();
         * }</pre>
         * 
         * @param entityClass The entity class to select properties from
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class with excluded properties.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "secretKey");
         * String sql = PSC.select(Account.class, excluded)
         *                 .from("account")
         *                 .sql();
         * // Selects all Account properties except password and secretKey
         * }</pre>
         * 
         * @param entityClass The entity class to select properties from
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement for an entity class with sub-entities and exclusions.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("internalNotes", "auditLog");
         * String sql = PSC.select(Order.class, true, excluded)
         *                 .from("orders")
         *                 .sql();
         * // Selects all Order properties including sub-entities, except excluded ones
         * }</pre>
         * 
         * @param entityClass The entity class to select properties from
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for an entity class.
         * 
         * <p>This is a convenience method that combines select() and from() operations.
         * The table name is derived from the entity class name or @Table annotation.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.selectFrom(Account.class)
         *                 .where(CF.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = ?
         * }</pre>
         * 
         * @param entityClass The entity class to select from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement for an entity class with table alias.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.selectFrom(Account.class, "a")
         *                 .where(CF.eq("a.status", "active"))
         *                 .sql();
         * // Output: SELECT a.id, a.first_name AS "firstName", a.last_name AS "lastName", a.email FROM account a WHERE a.status = ?
         * }</pre>
         * 
         * @param entityClass The entity class to select from
         * @param alias The table alias to use
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement with optional sub-entity properties.
         * 
         * @param entityClass The entity class to select from
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with alias and sub-entity option.
         * 
         * @param entityClass The entity class to select from
         * @param alias The table alias to use
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with excluded properties.
         * 
         * @param entityClass The entity class to select from
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with alias and excluded properties.
         * 
         * @param entityClass The entity class to select from
         * @param alias The table alias to use
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with sub-entities and exclusions.
         * 
         * @param entityClass The entity class to select from
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM statement with all options.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "internalNotes");
         * String sql = PSC.selectFrom(Account.class, "a", true, excluded)
         *                 .innerJoin("orders o").on("a.id = o.account_id")
         *                 .where(CF.gt("o.total", 1000))
         *                 .sql();
         * }</pre>
         * 
         * @param entityClass The entity class to select from
         * @param alias The table alias to use
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for multiple entity classes (for joins).
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.select(Account.class, "a", "account",
         *                        Order.class, "o", "order")
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.account_id")
         *                 .sql();
         * // Selects columns from both Account and Order with prefixes
         * }</pre>
         * 
         * @param entityClassA First entity class
         * @param tableAliasA Table alias for first entity
         * @param classAliasA Property prefix for first entity
         * @param entityClassB Second entity class
         * @param tableAliasB Table alias for second entity
         * @param classAliasB Property prefix for second entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for multiple entity classes with exclusions.
         * 
         * @param entityClassA First entity class
         * @param tableAliasA Table alias for first entity
         * @param classAliasA Property prefix for first entity
         * @param excludedPropNamesA Excluded properties for first entity
         * @param entityClassB Second entity class
         * @param tableAliasB Table alias for second entity
         * @param classAliasB Property prefix for second entity
         * @param excludedPropNamesB Excluded properties for second entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entity selections.
         * 
         * <p>This method supports complex queries involving multiple entities with different
         * selection criteria for each entity.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, null),
         *     new Selection(Order.class, "o", "order", Arrays.asList("id", "total"), false, null),
         *     new Selection(Product.class, "p", "product", null, false, N.asSet("description"))
         * );
         * 
         * String sql = PSC.select(selections)
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.account_id")
         *                 .innerJoin("order_items oi").on("o.id = oi.order_id")
         *                 .innerJoin("products p").on("oi.product_id = p.id")
         *                 .sql();
         * }</pre>
         * 
         * @param multiSelects List of Selection objects defining what to select from each entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM statement for multiple entity classes.
         * 
         * <p>This convenience method combines select() and from() for multi-table queries.</p>
         * 
         * @param entityClassA First entity class
         * @param tableAliasA Table alias for first entity
         * @param classAliasA Property prefix for first entity
         * @param entityClassB Second entity class
         * @param tableAliasB Table alias for second entity
         * @param classAliasB Property prefix for second entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for multiple entity classes with exclusions.
         * 
         * @param entityClassA First entity class
         * @param tableAliasA Table alias for first entity
         * @param classAliasA Property prefix for first entity
         * @param excludedPropNamesA Excluded properties for first entity
         * @param entityClassB Second entity class
         * @param tableAliasB Table alias for second entity
         * @param classAliasB Property prefix for second entity
         * @param excludedPropNamesB Excluded properties for second entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement for multiple entity selections.
         * 
         * @param multiSelects List of Selection objects defining what to select from each entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for a table.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.count("account")
         *                 .where(CF.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE status = ?
         * }</pre>
         * 
         * @param tableName The table to count rows from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PSC.count(Account.class)
         *                 .where(CF.isNotNull("email"))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE email IS NOT NULL
         * }</pre>
         * 
         * @param entityClass The entity class to count
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class mapping.
         * 
         * <p>This method is useful for generating just the WHERE clause portion of a query
         * with proper property-to-column name mapping.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("firstName", "John"),
         *     CF.like("email", "%@example.com")
         * );
         * 
         * String sql = PSC.parse(cond, Account.class).sql();
         * // Output: first_name = ? AND email LIKE ?
         * }</pre>
         * 
         * @param cond The condition to parse
         * @param entityClass The entity class for property mapping
         * @return A new SQLBuilder instance containing just the condition SQL
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Parameterized SQL builder with UPPER_CASE_WITH_UNDERSCORE naming policy.
     * 
     * <p>This builder generates parameterized SQL statements (using '?' placeholders) with column names 
     * converted to uppercase with underscores. This follows the traditional database naming convention.</p>
     * 
     * <p>Key features:</p>
     * <ul>
     *   <li>Converts camelCase property names to UPPER_CASE_WITH_UNDERSCORE column names</li>
     *   <li>Uses '?' placeholders for parameter binding</li>
     *   <li>Maintains property name aliases in result sets for proper object mapping</li>
     * </ul>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Property 'firstName' becomes column 'FIRST_NAME'
     * String sql = PAC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = ?
     * }</pre>
     */
    public static class PAC extends SQLBuilder {

        PAC() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.PARAMETERIZED_SQL);
        }

        protected static PAC createInstance() {
            return new PAC();
        }

        /**
         * Creates an INSERT statement for a single expression or column.
         * 
         * <p>This method is a convenience wrapper that delegates to {@link #insert(String...)} 
         * with a single element array.</p>
         * 
         * @param expr the expression or column name to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.insert("name").into("users").sql();
         * // Output: INSERT INTO USERS (NAME) VALUES (?)
         * }</pre>
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for specified columns.
         * 
         * <p>The column names will be converted according to the UPPER_CASE_WITH_UNDERSCORE naming policy.
         * Use {@link #into(String)} to specify the target table.</p>
         * 
         * @param propOrColumnNames the property or column names to include in the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.insert("firstName", "lastName", "email")
         *                 .into("users")
         *                 .sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for specified columns from a collection.
         * 
         * <p>This method accepts a collection of column names, providing flexibility when 
         * the column list is dynamically generated.</p>
         * 
         * @param propOrColumnNames collection of property or column names to include
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * @example
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = PAC.insert(columns).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement from a map of property names to values.
         * 
         * <p>The map keys represent column names and will be converted according to the naming policy.
         * The values are used to determine the number of parameter placeholders needed.</p>
         * 
         * @param props map of property names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         * 
         * @example
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("firstName", "John");
         * data.put("lastName", "Doe");
         * String sql = PAC.insert(data).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME) VALUES (?, ?)
         * }</pre>
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement from an entity object.
         * 
         * <p>This method inspects the entity object and includes all properties that are not marked 
         * with exclusion annotations (@Transient, @ReadOnly, etc.). The table name is inferred 
         * from the entity class or @Table annotation.</p>
         * 
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         * 
         * @example
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * String sql = PAC.insert(user).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement from an entity object with excluded properties.
         * 
         * <p>This method allows fine-grained control over which properties to include in the INSERT.
         * Properties in the exclusion set will not be included even if they are normally insertable.</p>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         * 
         * @example
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * Set<String> exclude = new HashSet<>(Arrays.asList("createdDate", "modifiedDate"));
         * String sql = PAC.insert(user, exclude).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement template for an entity class.
         * 
         * <p>This method generates an INSERT statement based on the class structure without 
         * requiring an actual entity instance. All insertable properties are included.</p>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.insert(User.class).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement template for an entity class with excluded properties.
         * 
         * <p>This method generates an INSERT statement based on the class structure, excluding 
         * specified properties. Useful for creating reusable INSERT templates.</p>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = PAC.insert(User.class, exclude).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for an entity class with automatic table name resolution.
         * 
         * <p>This is a convenience method that combines {@link #insert(Class)} with {@link #into(Class)}.
         * The table name is determined from the @Table annotation or class name.</p>
         * 
         * @param entityClass the entity class to insert into
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.insertInto(User.class).sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class with excluded properties and automatic table name.
         * 
         * <p>Combines the functionality of specifying excluded properties with automatic table name resolution.</p>
         * 
         * @param entityClass the entity class to insert into
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("id"));
         * String sql = PAC.insertInto(User.class, exclude).sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT statement for multiple entities (MySQL style).
         * 
         * <p>This method generates a single INSERT statement with multiple value sets, 
         * which is more efficient than multiple individual INSERTs. This is particularly 
         * useful for MySQL and compatible databases.</p>
         * 
         * <p>Note: This is a beta feature and may change in future versions.</p>
         * 
         * @param propsList collection of entities or property maps to batch insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * 
         * @example
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * String sql = PAC.batchInsert(users).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME) VALUES (?, ?), (?, ?)
         * }</pre>
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for a specified table.
         * 
         * <p>This method starts building an UPDATE statement. Use {@link #set(String...)} 
         * to specify which columns to update.</p>
         * 
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.update("users")
         *                 .set("firstName", "lastName")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE USERS SET FIRST_NAME = ?, LAST_NAME = ? WHERE ID = ?
         * }</pre>
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class context.
         * 
         * <p>This method allows specifying both the table name and entity class, 
         * which enables proper property-to-column name mapping.</p>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName or entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.update("users", User.class)
         *                 .set("firstName", "lastName")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE USERS SET FIRST_NAME = ?, LAST_NAME = ? WHERE ID = ?
         * }</pre>
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class with automatic table name.
         * 
         * <p>The table name is determined from the @Table annotation or class name. 
         * All updatable properties (excluding @ReadOnly, @NonUpdatable) are included.</p>
         * 
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.update(User.class)
         *                 .set("firstName", "lastName")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE USERS SET FIRST_NAME = ?, LAST_NAME = ? WHERE ID = ?
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class with excluded properties.
         * 
         * <p>This method generates an UPDATE statement excluding specified properties 
         * in addition to those marked with @ReadOnly or @NonUpdatable annotations.</p>
         * 
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from updates
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("version", "modifiedDate"));
         * String sql = PAC.update(User.class, exclude)
         *                 .set("firstName", "lastName")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE USERS SET FIRST_NAME = ?, LAST_NAME = ? WHERE ID = ?
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE statement for a specified table.
         * 
         * <p>This method starts building a DELETE FROM statement. Typically followed 
         * by WHERE conditions to specify which rows to delete.</p>
         * 
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.deleteFrom("users")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM USERS WHERE ID = ?
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE statement for a table with entity class context.
         * 
         * <p>This method allows specifying both the table name and entity class 
         * for proper property-to-column name mapping in WHERE conditions.</p>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName or entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.deleteFrom("users", User.class)
         *                 .where(CF.eq("email", "john@example.com"))
         *                 .sql();
         * // Output: DELETE FROM USERS WHERE EMAIL = ?
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE statement for an entity class with automatic table name.
         * 
         * <p>The table name is determined from the @Table annotation or class name.</p>
         * 
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.deleteFrom(User.class)
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM USERS WHERE ID = ?
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression or column.
         * 
         * <p>This method can accept complex expressions like aggregate functions, 
         * calculations, or simple column names.</p>
         * 
         * @param selectPart the SELECT expression (e.g., "COUNT(*)", "MAX(age)", "firstName")
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.select("COUNT(*)").from("users").sql();
         * // Output: SELECT COUNT(*) FROM USERS
         * 
         * String sql2 = PAC.select("MAX(age)").from("users").sql();
         * // Output: SELECT MAX(AGE) FROM USERS
         * }</pre>
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement with multiple columns.
         * 
         * <p>Column names will be converted according to the UPPER_CASE_WITH_UNDERSCORE 
         * naming policy and aliased back to their original property names.</p>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.select("firstName", "lastName", "email")
         *                 .from("users")
         *                 .sql();
         * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName", EMAIL AS "email" FROM USERS
         * }</pre>
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement with columns from a collection.
         * 
         * <p>This method accepts a collection of column names, useful when the column 
         * list is dynamically generated.</p>
         * 
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * @example
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = PAC.select(columns).from("users").sql();
         * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName", EMAIL AS "email" FROM USERS
         * }</pre>
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with column aliases.
         * 
         * <p>This method allows specifying custom aliases for selected columns. 
         * The map keys are column names and values are their aliases.</p>
         * 
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         * 
         * @example
         * <pre>{@code
         * Map<String, String> aliases = new LinkedHashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = PAC.select(aliases).from("users").sql();
         * // Output: SELECT FIRST_NAME AS "fname", LAST_NAME AS "lname" FROM USERS
         * }</pre>
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all properties of an entity class.
         * 
         * <p>This method selects all properties from the entity class that are not 
         * marked with @Transient. Sub-entity properties are not included by default.</p>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.select(User.class).from("users").sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", LAST_NAME AS "lastName", EMAIL AS "email" FROM USERS
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with sub-entity control.
         * 
         * <p>When includeSubEntityProperties is true, properties of nested entity types 
         * are also included in the selection with appropriate prefixes.</p>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * // If User has an Address sub-entity
         * String sql = PAC.select(User.class, true).from("users").sql();
         * // Output includes address properties: ADDRESS_STREET AS "address.street", etc.
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class with excluded properties.
         * 
         * <p>This method selects all properties except those specified in the exclusion set.</p>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password", "salt"));
         * String sql = PAC.select(User.class, exclude).from("users").sql();
         * // Output excludes password and salt columns
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement with full control over entity property selection.
         * 
         * <p>This method combines sub-entity inclusion control with property exclusion, 
         * providing maximum flexibility in column selection.</p>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from sub-entities
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.select(User.class, true, exclude)
         *                 .from("users")
         *                 .sql();
         * // Output includes sub-entity properties but excludes password
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for an entity class.
         * 
         * <p>This convenience method combines SELECT and FROM operations. The table name 
         * is derived from the @Table annotation or entity class name.</p>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance with both SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class).where(CF.eq("active", true)).sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", ... FROM USERS WHERE ACTIVE = ?
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement with a table alias.
         * 
         * <p>The alias is used to qualify column names in the generated SQL, useful 
         * for self-joins or disambiguating columns in complex queries.</p>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class, "u")
         *                 .where(CF.eq("u.active", true))
         *                 .sql();
         * // Output: SELECT u.ID AS "id", u.FIRST_NAME AS "firstName", ... FROM USERS u WHERE u.ACTIVE = ?
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement with sub-entity property control.
         * 
         * <p>When includeSubEntityProperties is true and the entity has sub-entities, 
         * appropriate joins may be generated automatically.</p>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include sub-entity properties
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class, true)
         *                 .where(CF.eq("active", true))
         *                 .sql();
         * // Output includes joins for sub-entities if present
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with table alias and sub-entity control.
         * 
         * <p>Combines table aliasing with sub-entity property inclusion for complex queries.</p>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include sub-entity properties
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class, "u", true)
         *                 .where(CF.eq("u.active", true))
         *                 .sql();
         * // Output: SELECT u.ID AS "id", ... FROM USERS u WHERE u.ACTIVE = ?
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with excluded properties.
         * 
         * <p>This method provides a convenient way to select from an entity while 
         * excluding specific properties.</p>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, exclude).sql();
         * // Output excludes the password column
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with alias and excluded properties.
         * 
         * <p>Combines table aliasing with property exclusion for precise query control.</p>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, "u", exclude).sql();
         * // Output: SELECT u.ID AS "id", ... FROM USERS u (excluding password)
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with sub-entity control and exclusions.
         * 
         * <p>Provides control over both sub-entity inclusion and property exclusion.</p>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include sub-entity properties
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, true, exclude).sql();
         * // Output includes sub-entities but excludes password
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with full control over all options.
         * 
         * <p>This method provides maximum flexibility by allowing control over table alias, 
         * sub-entity inclusion, and property exclusion.</p>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include sub-entity properties
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, "u", true, exclude)
         *                 .innerJoin("addresses", "a").on("u.id = a.user_id")
         *                 .sql();
         * // Complex query with full control
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for two entity classes with aliases.
         * 
         * <p>This method is designed for queries that need to select from two tables, 
         * typically used with joins. Each entity gets both a table alias and a class alias 
         * for result mapping.</p>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity results
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity results
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.select(User.class, "u", "user", Order.class, "o", "order")
         *                 .from("users", "u")
         *                 .innerJoin("orders", "o").on("u.id = o.user_id")
         *                 .sql();
         * // Output: SELECT u.ID AS "user.id", ..., o.ID AS "order.id", ... 
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for two entity classes with aliases and exclusions.
         * 
         * <p>Extended version that allows excluding specific properties from each entity 
         * in the multi-table select.</p>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * 
         * @example
         * <pre>{@code
         * Set<String> userExclude = new HashSet<>(Arrays.asList("password"));
         * Set<String> orderExclude = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = PAC.select(User.class, "u", "user", userExclude,
         *                        Order.class, "o", "order", orderExclude)
         *                 .from("users", "u")
         *                 .innerJoin("orders", "o").on("u.id = o.user_id")
         *                 .sql();
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entities using Selection descriptors.
         * 
         * <p>This is the most flexible method for multi-entity queries, allowing any number 
         * of entities with full control over aliases, sub-entities, and exclusions.</p>
         * 
         * @param multiSelects list of Selection descriptors for each entity
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if multiSelects is null or empty
         * 
         * @example
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, null)
         * );
         * String sql = PAC.select(selections)
         *                 .from("users", "u")
         *                 .innerJoin("orders", "o").on("u.id = o.user_id")
         *                 .innerJoin("products", "p").on("o.product_id = p.id")
         *                 .sql();
         * }</pre>
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for two entities.
         * 
         * <p>Convenience method that combines SELECT and FROM for two-table queries. 
         * The FROM clause is automatically generated based on the entity classes.</p>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class, "u", "user", 
         *                            Order.class, "o", "order")
         *                 .innerJoin("o").on("u.id = o.user_id")
         *                 .sql();
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for two entities with exclusions.
         * 
         * <p>Extended version allowing property exclusions for each entity in the query.</p>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * 
         * @example
         * <pre>{@code
         * Set<String> userExclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, "u", "user", userExclude,
         *                            Order.class, "o", "order", null)
         *                 .innerJoin("o").on("u.id = o.user_id")
         *                 .sql();
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement for multiple entities.
         * 
         * <p>Most flexible method for multi-entity queries with automatic FROM clause generation.</p>
         * 
         * @param multiSelects list of Selection descriptors
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if multiSelects is null or empty
         * 
         * @example
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null)
         * );
         * String sql = PAC.selectFrom(selections)
         *                 .innerJoin("o").on("u.id = o.user_id")
         *                 .sql();
         * }</pre>
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for a table.
         * 
         * <p>Convenience method for generating count queries.</p>
         * 
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT query
         * @throws IllegalArgumentException if tableName is null or empty
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.count("users").where(CF.eq("active", true)).sql();
         * // Output: SELECT COUNT(*) FROM USERS WHERE ACTIVE = ?
         * }</pre>
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>Table name is derived from the entity class.</p>
         * 
         * @param entityClass the entity class to count
         * @return a new SQLBuilder instance configured for COUNT query
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @example
         * <pre>{@code
         * String sql = PAC.count(User.class)
         *                 .where(CF.gt("age", 18))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM USERS WHERE AGE > ?
         * }</pre>
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity property mapping.
         * 
         * <p>This method is useful for generating just the SQL representation of a condition, 
         * typically for use in complex queries or debugging.</p>
         * 
         * @param cond the condition to parse
         * @param entityClass entity class for property name mapping
         * @return a new SQLBuilder instance containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         * 
         * @example
         * <pre>{@code
         * Condition cond = CF.and(CF.eq("firstName", "John"), CF.gt("age", 21));
         * String sql = PAC.parse(cond, User.class).sql();
         * // Output: FIRST_NAME = ? AND AGE > ?
         * }</pre>
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Parameterized SQL builder with lowerCamelCase field/column naming strategy.
     * 
     * <p>PLC (Parameterized Lower Camel Case) generates SQL with placeholder parameters (?) 
     * while maintaining camelCase naming for both properties and columns. This is useful when 
     * your database columns follow camelCase naming convention instead of the traditional 
     * snake_case.</p>
     * 
     * <p><b>Naming Convention:</b></p>
     * <ul>
     *   <li>Property: firstName → Column: firstName</li>
     *   <li>Property: accountNumber → Column: accountNumber</li>
     *   <li>Property: isActive → Column: isActive</li>
     * </ul>
     * 
     * <p><b>Basic Usage Examples:</b></p>
     * <pre>{@code
     * // Simple SELECT
     * String sql = PLC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: SELECT firstName, lastName FROM account WHERE id = ?
     * 
     * // INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = PLC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (firstName, lastName) VALUES (?, ?)
     * 
     * // UPDATE with specific fields
     * String sql = PLC.update("account")
     *                 .set("firstName", "John")
     *                 .set("lastName", "Smith")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: UPDATE account SET firstName = ?, lastName = ? WHERE id = ?
     * }</pre>
     * 
     * <p><b>Advanced Examples:</b></p>
     * <pre>{@code
     * // Complex JOIN query with camelCase columns
     * String sql = PLC.select("a.id", "a.firstName", "COUNT(o.id) AS orderCount")
     *                 .from("account a")
     *                 .leftJoin("orders o").on("a.id = o.accountId")
     *                 .groupBy("a.id", "a.firstName")
     *                 .having(CF.gt("COUNT(o.id)", 5))
     *                 .sql();
     * 
     * // Using with MongoDB-style collections
     * String sql = PLC.selectFrom(UserProfile.class)
     *                 .where(CF.and(
     *                     CF.eq("isActive", true),
     *                     CF.gte("lastLoginDate", lastWeek)
     *                 ))
     *                 .orderBy("lastLoginDate DESC")
     *                 .sql();
     * }</pre>
     * 
     * @see SQLBuilder
     * @see PSC
     * @see NLC
     */
    public static class PLC extends SQLBuilder {

        PLC() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.PARAMETERIZED_SQL);
        }

        protected static PLC createInstance() {
            return new PLC();
        }

        /**
         * Creates an INSERT statement for a single column expression.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.insert("firstName").into("account").sql();
         * // Output: INSERT INTO account (firstName) VALUES (?)
         * }</pre>
         * 
         * @param expr The column name or expression to insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for multiple columns.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.insert("firstName", "lastName", "email")
         *                 .into("account")
         *                 .sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param propOrColumnNames The property or column names to insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for a collection of columns.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = PLC.insert(columns).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param propOrColumnNames Collection of property or column names to insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement from a map of property names and values.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * SP sqlPair = PLC.insert(props).into("account").pair();
         * // sqlPair.sql: INSERT INTO account (firstName, lastName) VALUES (?, ?)
         * // sqlPair.parameters: ["John", "Doe"]
         * }</pre>
         * 
         * @param props Map of property names to their values
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement from an entity object.
         * 
         * <p>This method extracts all non-null properties from the entity object,
         * excluding those marked with @Transient, @ReadOnly, or @ReadOnlyId annotations.
         * Property names maintain their camelCase format.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setEmailAddress("john.doe@example.com");
         * 
         * SP sqlPair = PLC.insert(account).into("account").pair();
         * // sqlPair.sql: INSERT INTO account (firstName, lastName, emailAddress) VALUES (?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john.doe@example.com"]
         * }</pre>
         * 
         * @param entity The entity object to insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement from an entity object with excluded properties.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setCreatedDate(new Date());
         * 
         * Set<String> excluded = N.asSet("createdDate");
         * SP sqlPair = PLC.insert(account, excluded).into("account").pair();
         * // sqlPair.sql: INSERT INTO account (firstName, lastName) VALUES (?, ?)
         * // sqlPair.parameters: ["John", "Doe"]
         * }</pre>
         * 
         * @param entity The entity object to insert
         * @param excludedPropNames Set of property names to exclude from the insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for an entity class.
         * 
         * <p>This method includes all properties of the entity class that are suitable for insertion,
         * excluding those marked with @Transient, @ReadOnly, or @ReadOnlyId annotations.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.insert(Account.class).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, email, createdDate) VALUES (?, ?, ?, ?)
         * }</pre>
         * 
         * @param entityClass The entity class to generate INSERT for
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class with excluded properties.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("createdDate", "modifiedDate");
         * String sql = PLC.insert(Account.class, excluded).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass The entity class to generate INSERT for
         * @param excludedPropNames Set of property names to exclude from the insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO statement for an entity class.
         * 
         * <p>This is a convenience method that combines insert() and into() operations.
         * The table name is derived from the entity class name or @Table annotation.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.insertInto(Account.class).sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass The entity class to generate INSERT INTO for
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO statement for an entity class with excluded properties.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("id", "createdDate");
         * String sql = PLC.insertInto(Account.class, excluded).sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass The entity class to generate INSERT INTO for
         * @param excludedPropNames Set of property names to exclude from the insert
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generates a MySQL-style batch INSERT statement.
         * 
         * <p>This method creates an efficient batch insert statement with multiple value sets
         * in a single INSERT statement. Property names maintain their camelCase format.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "Doe"),
         *     new Account("Jane", "Smith"),
         *     new Account("Bob", "Johnson")
         * );
         * 
         * SP sqlPair = PLC.batchInsert(accounts).into("account").pair();
         * // sqlPair.sql: INSERT INTO account (firstName, lastName) VALUES (?, ?), (?, ?), (?, ?)
         * // sqlPair.parameters: ["John", "Doe", "Jane", "Smith", "Bob", "Johnson"]
         * }</pre>
         * 
         * @param propsList List of entities or property maps to insert
         * @return A new SQLBuilder instance for method chaining
         * @deprecated This feature is in beta and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.update("account")
         *                 .set("firstName", "John")
         *                 .set("lastName", "Smith")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = ?, lastName = ? WHERE id = ?
         * }</pre>
         * 
         * @param tableName The name of the table to update
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class mapping.
         * 
         * <p>The entity class provides property-to-column name mapping information.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.update("account", Account.class)
         *                 .set("firstName", "John")
         *                 .set("lastModified", new Date())
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = ?, lastModified = ? WHERE id = ?
         * }</pre>
         * 
         * @param tableName The name of the table to update
         * @param entityClass The entity class for property mapping
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.
         * All updatable properties are included by default.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.update(Account.class)
         *                 .set("status", "active")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET status = ? WHERE id = ?
         * }</pre>
         * 
         * @param entityClass The entity class to update
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class with excluded properties.
         * 
         * <p>Properties marked with @NonUpdatable or @ReadOnly are automatically excluded.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("createdDate", "createdBy");
         * String sql = PLC.update(Account.class, excluded)
         *                 .set(account)
         *                 .where(CF.eq("id", account.getId()))
         *                 .sql();
         * }</pre>
         * 
         * @param entityClass The entity class to update
         * @param excludedPropNames Set of property names to exclude from the update
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for a table.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.deleteFrom("account")
         *                 .where(CF.eq("status", "inactive"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE status = ?
         * }</pre>
         * 
         * @param tableName The name of the table to delete from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for a table with entity class mapping.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.deleteFrom("account", Account.class)
         *                 .where(CF.lt("lastLoginDate", thirtyDaysAgo))
         *                 .sql();
         * // Output: DELETE FROM account WHERE lastLoginDate < ?
         * }</pre>
         * 
         * @param tableName The name of the table to delete from
         * @param entityClass The entity class for property mapping
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.deleteFrom(Account.class)
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id = ?
         * }</pre>
         * 
         * @param entityClass The entity class to delete from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression.
         * 
         * <p>This method is useful for complex select expressions or aggregate functions.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.select("COUNT(*)")
         *                 .from("account")
         *                 .where(CF.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE status = ?
         * 
         * String sql2 = PLC.select("firstName || ' ' || lastName AS fullName")
         *                  .from("account")
         *                  .sql();
         * // Output: SELECT firstName || ' ' || lastName AS fullName FROM account
         * }</pre>
         * 
         * @param selectPart The select expression
         * @return A new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement with multiple columns.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.select("id", "firstName", "lastName", "email")
         *                 .from("account")
         *                 .where(CF.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email FROM account WHERE status = ?
         * }</pre>
         * 
         * @param propOrColumnNames The property or column names to select
         * @return A new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement with a collection of columns.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("id", "firstName", "lastName");
         * String sql = PLC.select(columns)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, firstName, lastName FROM account
         * }</pre>
         * 
         * @param propOrColumnNames Collection of property or column names to select
         * @return A new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with column aliases.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Map<String, String> columnAliases = new HashMap<>();
         * columnAliases.put("firstName", "fname");
         * columnAliases.put("lastName", "lname");
         * columnAliases.put("emailAddress", "email");
         * 
         * String sql = PLC.select(columnAliases)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT firstName AS fname, lastName AS lname, emailAddress AS email FROM account
         * }</pre>
         * 
         * @param propOrColumnNameAliases Map of property/column names to their aliases
         * @return A new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all properties of an entity class.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.select(Account.class)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email, createdDate FROM account
         * }</pre>
         * 
         * @param entityClass The entity class to select properties from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with optional sub-entity properties.
         * 
         * @param entityClass The entity class to select properties from
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class with excluded properties.
         * 
         * @param entityClass The entity class to select properties from
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement for an entity class with all options.
         * 
         * @param entityClass The entity class to select properties from
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for an entity class.
         * 
         * <p>This is a convenience method that combines select() and from() operations.
         * The table name is derived from the entity class name or @Table annotation.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.selectFrom(Account.class)
         *                 .where(CF.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email FROM account WHERE status = ?
         * }</pre>
         * 
         * @param entityClass The entity class to select from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement for an entity class with table alias.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.selectFrom(Account.class, "a")
         *                 .where(CF.eq("a.status", "active"))
         *                 .sql();
         * // Output: SELECT a.id, a.firstName, a.lastName, a.email FROM account a WHERE a.status = ?
         * }</pre>
         * 
         * @param entityClass The entity class to select from
         * @param alias The table alias to use
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement with optional sub-entity properties.
         * 
         * @param entityClass The entity class to select from
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with alias and sub-entity option.
         * 
         * @param entityClass The entity class to select from
         * @param alias The table alias to use
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with excluded properties.
         * 
         * @param entityClass The entity class to select from
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with alias and excluded properties.
         * 
         * @param entityClass The entity class to select from
         * @param alias The table alias to use
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with sub-entities and exclusions.
         * 
         * @param entityClass The entity class to select from
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM statement with all options.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "internalNotes");
         * String sql = PLC.selectFrom(Account.class, "a", true, excluded)
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .where(CF.gt("o.total", 1000))
         *                 .sql();
         * }</pre>
         * 
         * @param entityClass The entity class to select from
         * @param alias The table alias to use
         * @param includeSubEntityProperties Whether to include properties of nested entity objects
         * @param excludedPropNames Set of property names to exclude from selection
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for multiple entity classes (for joins).
         * 
         * @param entityClassA First entity class
         * @param tableAliasA Table alias for first entity
         * @param classAliasA Property prefix for first entity
         * @param entityClassB Second entity class
         * @param tableAliasB Table alias for second entity
         * @param classAliasB Property prefix for second entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for multiple entity classes with exclusions.
         * 
         * @param entityClassA First entity class
         * @param tableAliasA Table alias for first entity
         * @param classAliasA Property prefix for first entity
         * @param excludedPropNamesA Excluded properties for first entity
         * @param entityClassB Second entity class
         * @param tableAliasB Table alias for second entity
         * @param classAliasB Property prefix for second entity
         * @param excludedPropNamesB Excluded properties for second entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entity selections.
         * 
         * @param multiSelects List of Selection objects defining what to select from each entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM statement for multiple entity classes.
         * 
         * @param entityClassA First entity class
         * @param tableAliasA Table alias for first entity
         * @param classAliasA Property prefix for first entity
         * @param entityClassB Second entity class
         * @param tableAliasB Table alias for second entity
         * @param classAliasB Property prefix for second entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for multiple entity classes with exclusions.
         * 
         * @param entityClassA First entity class
         * @param tableAliasA Table alias for first entity
         * @param classAliasA Property prefix for first entity
         * @param excludedPropNamesA Excluded properties for first entity
         * @param entityClassB Second entity class
         * @param tableAliasB Table alias for second entity
         * @param classAliasB Property prefix for second entity
         * @param excludedPropNamesB Excluded properties for second entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement for multiple entity selections.
         * 
         * @param multiSelects List of Selection objects defining what to select from each entity
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for a table.
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.count("account")
         *                 .where(CF.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE status = ?
         * }</pre>
         * 
         * @param tableName The table to count rows from
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * String sql = PLC.count(Account.class)
         *                 .where(CF.isNotNull("email"))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE email IS NOT NULL
         * }</pre>
         * 
         * @param entityClass The entity class to count
         * @return A new SQLBuilder instance for method chaining
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class mapping.
         * 
         * <p>This method is useful for generating just the WHERE clause portion of a query
         * with proper property-to-column name mapping.</p>
         * 
         * <p><b>Example:</b></p>
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("firstName", "John"),
         *     CF.like("emailAddress", "%@example.com")
         * );
         * 
         * String sql = PLC.parse(cond, Account.class).sql();
         * // Output: firstName = ? AND emailAddress LIKE ?
         * }</pre>
         * 
         * @param cond The condition to parse
         * @param entityClass The entity class for property mapping
         * @return A new SQLBuilder instance containing just the condition SQL
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with {@code NamingPolicy.NO_CHANGE} field/column naming strategy.
     * 
     * <p>This builder generates SQL with named parameters (e.g., :paramName) and preserves the original
     * naming convention of fields and columns without any transformation. It's particularly useful when
     * working with databases where column names match exactly with your Java field names.</p>
     *
     * <p>For example:</p>
     * <pre>{@code
     * N.println(NSB.select("first_Name", "last_NaMe").from("account").where(CF.eq("last_NaMe", 1)).sql());
     * // SELECT first_Name, last_NaMe FROM account WHERE last_NaMe = :last_NaMe
     * }</pre>
     */
    public static class NSB extends SQLBuilder {

        NSB() {
            super(NamingPolicy.NO_CHANGE, SQLPolicy.NAMED_SQL);
        }

        @Override
        protected boolean isNamedSql() {
            return true;
        }

        protected static NSB createInstance() {
            return new NSB();
        }

        /**
         * Creates an INSERT SQL builder with a single column expression.
         * 
         * <p>This method is a convenience wrapper that internally calls {@link #insert(String...)} 
         * with a single-element array.</p>
         *
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.insert("user_name").into("users").sql();
         * // INSERT INTO users (user_name) VALUES (:user_name)
         * }</pre>
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder with specified column names.
         * 
         * <p>The generated SQL will include placeholders for the specified columns using named parameters.</p>
         *
         * @param propOrColumnNames the property or column names to include in the INSERT statement
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.insert("first_name", "last_name", "email")
         *                 .into("users")
         *                 .sql();
         * // INSERT INTO users (first_name, last_name, email) VALUES (:first_name, :last_name, :email)
         * }</pre>
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with a collection of column names.
         * 
         * <p>This method allows using any Collection implementation (List, Set, etc.) to specify
         * the columns for the INSERT statement.</p>
         *
         * @param propOrColumnNames collection of property or column names to include
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("id", "name", "created_date");
         * String sql = NSB.insert(columns).into("products").sql();
         * // INSERT INTO products (id, name, created_date) VALUES (:id, :name, :created_date)
         * }</pre>
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from a map of column names to values.
         * 
         * <p>The map keys represent column names and the values are the corresponding values to insert.
         * This method is useful when you have dynamic column-value pairs.</p>
         *
         * @param props map of column names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("username", "john_doe");
         * data.put("age", 25);
         * String sql = NSB.insert(data).into("users").sql();
         * // INSERT INTO users (username, age) VALUES (:username, :age)
         * }</pre>
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object.
         * 
         * <p>This method extracts all non-null properties from the entity object to create the INSERT statement.
         * Properties annotated with {@code @Transient}, {@code @ReadOnly}, or {@code @ReadOnlyId} are automatically excluded.</p>
         *
         * @param entity the entity object containing data to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * User user = new User();
         * user.setName("John");
         * user.setEmail("john@example.com");
         * String sql = NSB.insert(user).into("users").sql();
         * // INSERT INTO users (name, email) VALUES (:name, :email)
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object with excluded properties.
         * 
         * <p>This method allows fine-grained control over which properties to exclude from the INSERT statement,
         * in addition to the automatically excluded annotated properties.</p>
         *
         * @param entity the entity object containing data to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * User user = new User();
         * user.setName("John");
         * user.setEmail("john@example.com");
         * user.setPassword("secret");
         * Set<String> exclude = N.asSet("password");
         * String sql = NSB.insert(user, exclude).into("users").sql();
         * // INSERT INTO users (name, email) VALUES (:name, :email)
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for a specific entity class.
         * 
         * <p>This method generates an INSERT template for all insertable properties of the entity class.
         * Properties are determined by the class structure and annotations.</p>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.insert(User.class).into("users").sql();
         * // INSERT INTO users (id, name, email, created_date) VALUES (:id, :name, :email, :created_date)
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for a specific entity class with excluded properties.
         * 
         * <p>This method provides control over which properties to include in the INSERT statement
         * when generating SQL from a class definition.</p>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for INSERT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("id", "createdDate");
         * String sql = NSB.insert(User.class, exclude).into("users").sql();
         * // INSERT INTO users (name, email) VALUES (:name, :email)
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with automatic table name detection.
         * 
         * <p>The table name is automatically determined from the entity class using the {@code @Table} annotation
         * or by converting the class name according to the naming policy.</p>
         *
         * @param entityClass the entity class for INSERT operation
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * 
         * <p>Example:</p>
         * <pre>{@code
         * @Table("user_accounts")
         * class User { ... }
         * 
         * String sql = NSB.insertInto(User.class).sql();
         * // INSERT INTO user_accounts (id, name, email) VALUES (:id, :name, :email)
         * }</pre>
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder with automatic table name detection and excluded properties.
         * 
         * <p>Combines automatic table name detection with the ability to exclude specific properties
         * from the INSERT statement.</p>
         *
         * @param entityClass the entity class for INSERT operation
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("version", "lastModified");
         * String sql = NSB.insertInto(User.class, exclude).sql();
         * // INSERT INTO users (id, name, email) VALUES (:id, :name, :email)
         * }</pre>
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple records (MySQL style).
         * 
         * <p>This method generates a single INSERT statement with multiple value sets, which is more efficient
         * than multiple individual INSERT statements. The input can be a collection of entity objects or maps.</p>
         *
         * @param propsList collection of entities or property maps to insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @beta This is a beta feature and may be subject to changes
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "john@email.com"),
         *     new User("Jane", "jane@email.com")
         * );
         * String sql = NSB.batchInsert(users).into("users").sql();
         * // INSERT INTO users (name, email) VALUES (:name_0, :email_0), (:name_1, :email_1)
         * }</pre>
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * <p>This method starts building an UPDATE statement for the given table. You must call
         * {@code set()} methods to specify which columns to update.</p>
         *
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.update("users")
         *                 .set("last_login", "status")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // UPDATE users SET last_login = :last_login, status = :status WHERE id = :id
         * }</pre>
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with entity class mapping.
         * 
         * <p>This method allows specifying both the table name and entity class, which enables
         * proper property-to-column name mapping based on the entity's annotations.</p>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.update("user_accounts", User.class)
         *                 .set("lastLogin", "active")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // UPDATE user_accounts SET last_login = :lastLogin, active = :active WHERE id = :id
         * }</pre>
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class with automatic table name detection.
         * 
         * <p>The table name is derived from the entity class, and all updatable properties
         * (excluding those marked with {@code @NonUpdatable}, {@code @ReadOnly}, etc.) are included.</p>
         *
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.update(User.class)
         *                 .set("name", "email")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // UPDATE users SET name = :name, email = :email WHERE id = :id
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class with excluded properties.
         * 
         * <p>This method automatically determines updatable properties from the entity class
         * while allowing additional properties to be excluded from the UPDATE.</p>
         *
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("password", "createdDate");
         * String sql = NSB.update(User.class, exclude)
         *                 .set("name", "email")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // UPDATE users SET name = :name, email = :email WHERE id = :id
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table.
         * 
         * <p>This method initiates a DELETE statement. Typically, you'll want to add WHERE conditions
         * to avoid deleting all records in the table.</p>
         *
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.deleteFrom("users")
         *                 .where(CF.eq("status", "inactive"))
         *                 .sql();
         * // DELETE FROM users WHERE status = :status
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for a table with entity class mapping.
         * 
         * <p>This method enables proper property-to-column name mapping when building WHERE conditions
         * for the DELETE statement.</p>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.deleteFrom("user_accounts", User.class)
         *                 .where(CF.lt("lastLogin", someDate))
         *                 .sql();
         * // DELETE FROM user_accounts WHERE last_login < :lastLogin
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for an entity class with automatic table name detection.
         * 
         * <p>The table name is derived from the entity class using {@code @Table} annotation
         * or naming policy conversion.</p>
         *
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.deleteFrom(User.class)
         *                 .where(CF.eq("id", 123))
         *                 .sql();
         * // DELETE FROM users WHERE id = :id
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single column or expression.
         * 
         * <p>The selectPart parameter can be a simple column name or a complex SQL expression.
         * This method is useful for selecting computed values or using SQL functions.</p>
         *
         * @param selectPart the column name or SQL expression to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.select("COUNT(*) AS total").from("users").sql();
         * // SELECT COUNT(*) AS total FROM users
         * 
         * String sql2 = NSB.select("MAX(salary) - MIN(salary) AS salary_range")
         *                  .from("employees")
         *                  .sql();
         * // SELECT MAX(salary) - MIN(salary) AS salary_range FROM employees
         * }</pre>
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder with multiple columns.
         * 
         * <p>Each string in the array represents a column to select. The columns will be
         * included in the SELECT clause in the order specified.</p>
         *
         * @param propOrColumnNames array of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.select("id", "name", "email", "created_date")
         *                 .from("users")
         *                 .where(CF.eq("active", true))
         *                 .sql();
         * // SELECT id, name, email, created_date FROM users WHERE active = :active
         * }</pre>
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a collection of columns.
         * 
         * <p>This method allows using any Collection implementation to specify the columns
         * to select, providing flexibility in how column lists are constructed.</p>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> columns = getRequiredColumns(); // Dynamic column list
         * String sql = NSB.select(columns)
         *                 .from("products")
         *                 .where(CF.gt("price", 100))
         *                 .sql();
         * // SELECT column1, column2, ... FROM products WHERE price > :price
         * }</pre>
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * 
         * <p>The map keys represent the column names or expressions, and the values are their aliases.
         * This is useful for renaming columns in the result set.</p>
         *
         * @param propOrColumnNameAliases map of column names/expressions to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, String> aliases = new LinkedHashMap<>();
         * aliases.put("u.first_name", "firstName");
         * aliases.put("u.last_name", "lastName");
         * aliases.put("COUNT(o.id)", "orderCount");
         * 
         * String sql = NSB.select(aliases)
         *                 .from("users u")
         *                 .leftJoin("orders o").on("u.id = o.user_id")
         *                 .groupBy("u.id")
         *                 .sql();
         * // SELECT u.first_name AS firstName, u.last_name AS lastName, COUNT(o.id) AS orderCount
         * // FROM users u LEFT JOIN orders o ON u.id = o.user_id GROUP BY u.id
         * }</pre>
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class.
         * 
         * <p>This method selects all properties from the entity class that are not marked
         * as transient. Sub-entity properties are not included by default.</p>
         *
         * @param entityClass the entity class to select properties from
         * @return a new SQLBuilder instance configured for SELECT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * // If User class has properties: id, name, email, address
         * String sql = NSB.select(User.class).from("users").sql();
         * // SELECT id, name, email, address FROM users
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with optional sub-entity properties.
         * 
         * <p>When includeSubEntityProperties is true, properties of nested entity types are also included
         * in the selection, which is useful for fetching related data in a single query.</p>
         *
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties from nested entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * // If User has an Address sub-entity
         * String sql = NSB.select(User.class, true)
         *                 .from("users u")
         *                 .leftJoin("addresses a").on("u.address_id = a.id")
         *                 .sql();
         * // SELECT u.id, u.name, u.email, a.street, a.city, a.zip FROM users u
         * // LEFT JOIN addresses a ON u.address_id = a.id
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with excluded properties.
         * 
         * <p>This method allows selecting most properties from an entity while excluding specific ones,
         * which is useful when you want to omit large fields like BLOBs or sensitive data.</p>
         *
         * @param entityClass the entity class to select properties from
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("password", "profilePicture");
         * String sql = NSB.select(User.class, exclude).from("users").sql();
         * // SELECT id, name, email, created_date FROM users
         * // (assuming User has id, name, email, created_date, password, and profilePicture)
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder with full control over entity property selection.
         * 
         * <p>This method combines the ability to include sub-entity properties and exclude specific
         * properties, providing maximum flexibility in constructing SELECT statements.</p>
         *
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties from nested entities
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("user.password", "address.coordinates");
         * String sql = NSB.select(User.class, true, exclude)
         *                 .from("users u")
         *                 .leftJoin("addresses a").on("u.address_id = a.id")
         *                 .sql();
         * // Selects all User and Address properties except password and coordinates
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for an entity class.
         * 
         * <p>This is a convenience method that combines select() and from() operations.
         * The table name is automatically derived from the entity class.</p>
         *
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.selectFrom(User.class).where(CF.eq("active", true)).sql();
         * // SELECT id, name, email FROM users WHERE active = :active
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with a table alias.
         * 
         * <p>This method allows specifying a table alias for use in joins and qualified column references.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.selectFrom(User.class, "u")
         *                 .leftJoin("orders o").on("u.id = o.user_id")
         *                 .where(CF.isNotNull("o.id"))
         *                 .sql();
         * // SELECT u.id, u.name, u.email FROM users u
         * // LEFT JOIN orders o ON u.id = o.user_id WHERE o.id IS NOT NULL
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT...FROM SQL builder with sub-entity property inclusion.
         * 
         * <p>When includeSubEntityProperties is true, the method automatically handles joining
         * related tables for nested entities.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include nested entity properties
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * // Automatically includes joins for sub-entities
         * String sql = NSB.selectFrom(Order.class, true).sql();
         * // May generate: SELECT o.*, c.*, p.* FROM orders o
         * // LEFT JOIN customers c ON o.customer_id = c.id
         * // LEFT JOIN products p ON o.product_id = p.id
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT...FROM SQL builder with alias and sub-entity control.
         * 
         * <p>Combines table aliasing with sub-entity property inclusion for complex queries.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include nested entity properties
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.selectFrom(Order.class, "o", true)
         *                 .where(CF.gt("o.total", 1000))
         *                 .sql();
         * // Generates SELECT with proper aliases for main and sub-entities
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT...FROM SQL builder with property exclusion.
         * 
         * <p>This convenience method combines selecting specific properties and setting the FROM clause.</p>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("largeData", "internalNotes");
         * String sql = NSB.selectFrom(User.class, exclude).sql();
         * // SELECT id, name, email FROM users (excluding largeData and internalNotes)
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT...FROM SQL builder with alias and property exclusion.
         * 
         * <p>Provides aliasing capability while excluding specific properties from selection.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("password");
         * String sql = NSB.selectFrom(User.class, "u", exclude)
         *                 .join("profiles p").on("u.id = p.user_id")
         *                 .sql();
         * // SELECT u.id, u.name, u.email FROM users u JOIN profiles p ON u.id = p.user_id
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT...FROM SQL builder with sub-entities and exclusions.
         * 
         * <p>This method automatically handles complex FROM clauses when sub-entities are included.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include nested entity properties
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("customer.creditCard");
         * String sql = NSB.selectFrom(Order.class, true, exclude).sql();
         * // Selects Order with Customer sub-entity but excludes creditCard field
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a fully-configured SELECT...FROM SQL builder.
         * 
         * <p>This is the most comprehensive selectFrom method, providing full control over
         * aliasing, sub-entity inclusion, and property exclusion.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include nested entity properties
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("audit.details", "customer.internalNotes");
         * String sql = NSB.selectFrom(Order.class, "o", true, exclude)
         *                 .where(CF.between("o.orderDate", startDate, endDate))
         *                 .orderBy("o.orderDate DESC")
         *                 .sql();
         * // Complex SELECT with multiple tables, aliases, and exclusions
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases.
         * 
         * <p>This method is designed for queries that need to select from multiple tables with
         * proper aliasing for both table names and result set column prefixes.</p>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA column prefix for first entity in results
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB column prefix for second entity in results
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.select(User.class, "u", "user_", Order.class, "o", "order_")
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .sql();
         * // SELECT u.id AS user_id, u.name AS user_name, o.id AS order_id, o.total AS order_total
         * // FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases and exclusions.
         * 
         * <p>This method provides full control over selecting from two entities, including the ability
         * to exclude specific properties from each entity.</p>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA column prefix for first entity
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB column prefix for second entity
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludeUser = N.asSet("password");
         * Set<String> excludeOrder = N.asSet("internalNotes");
         * 
         * String sql = NSB.select(User.class, "u", "user_", excludeUser,
         *                        Order.class, "o", "order_", excludeOrder)
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .sql();
         * // Selects all fields except excluded ones with proper prefixes
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * 
         * <p>This is the most flexible method for multi-table selections, accepting a list of
         * Selection objects that define how each entity should be selected.</p>
         *
         * @param multiSelects list of Selection configurations
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if multiSelects is invalid
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user_", null, false, null),
         *     new Selection(Order.class, "o", "order_", null, true, N.asSet("notes")),
         *     new Selection(Product.class, "p", "product_", null, false, null)
         * );
         * 
         * String sql = NSB.select(selections)
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .join("products p").on("o.product_id = p.id")
         *                 .sql();
         * // Complex multi-table SELECT with different configurations per table
         * }</pre>
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for two entities.
         * 
         * <p>This convenience method combines select() and from() for two-table queries.</p>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA column prefix for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB column prefix for second entity
         * @return a new SQLBuilder with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.selectFrom(User.class, "u", "user_",
         *                            Order.class, "o", "order_")
         *                 .where(CF.eq("u.id", 123))
         *                 .sql();
         * // Automatically generates FROM clause with proper joins
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for two entities with exclusions.
         * 
         * <p>This method automatically generates the appropriate FROM clause based on the
         * provided entity classes and their relationships.</p>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA column prefix for first entity
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB column prefix for second entity
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.selectFrom(Customer.class, "c", "cust_", N.asSet("password"),
         *                            Account.class, "a", "acct_", N.asSet("pin"))
         *                 .sql();
         * // Generates complete SELECT...FROM with exclusions
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for multiple entities.
         * 
         * <p>This method automatically generates the FROM clause based on the Selection
         * configurations, handling complex multi-table queries.</p>
         *
         * @param multiSelects list of Selection configurations
         * @return a new SQLBuilder with SELECT and FROM clauses configured
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = createSelectionList();
         * String sql = NSB.selectFrom(selections)
         *                 .where(CF.gt("o.amount", 100))
         *                 .orderBy("o.date DESC")
         *                 .sql();
         * // Automatically generates complete multi-table query
         * }</pre>
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for the specified table.
         * 
         * <p>This is a convenience method for creating count queries.</p>
         *
         * @param tableName the table to count records from
         * @return a new SQLBuilder configured for COUNT query
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.count("users").where(CF.eq("active", true)).sql();
         * // SELECT COUNT(*) FROM users WHERE active = :active
         * }</pre>
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>The table name is automatically derived from the entity class.</p>
         *
         * @param entityClass the entity class to count
         * @return a new SQLBuilder configured for COUNT query
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = NSB.count(User.class)
         *                 .where(CF.eq("status", "active"))
         *                 .sql();
         * // SELECT COUNT(*) FROM users WHERE status = :status
         * }</pre>
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class mapping.
         * 
         * <p>This method is useful for generating just the SQL representation of a condition,
         * without the full query structure. It's primarily used for debugging or building
         * dynamic query parts.</p>
         *
         * @param cond the condition to parse
         * @param entityClass the entity class for property name mapping
         * @return a new SQLBuilder containing only the condition SQL
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("status", "active"),
         *     CF.gt("age", 18)
         * );
         * String sql = NSB.parse(cond, User.class).sql();
         * // status = :status AND age > :age
         * }</pre>
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with snake_case (lower case with underscore) field/column naming strategy.
     * 
     * <p>This builder generates SQL statements using named parameters (e.g., :paramName) instead of 
     * positional parameters (?), and converts property names from camelCase to snake_case for column names.</p>
     * 
     * <p>Named parameters are useful for:</p>
     * <ul>
     *   <li>Better readability of generated SQL</li>
     *   <li>Reusing the same parameter multiple times in a query</li>
     *   <li>Integration with frameworks that support named parameters</li>
     * </ul>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Simple SELECT with named parameters
     * String sql = NSC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = :id
     * 
     * // INSERT with entity - generates named parameters
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = NSC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (first_name, last_name) VALUES (:firstName, :lastName)
     * }</pre>
     * 
     * @author Haiyang Li
     * @since 0.8
     */
    public static class NSC extends SQLBuilder {

        NSC() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.NAMED_SQL);
        }

        @Override
        protected boolean isNamedSql() {
            return true;
        }

        protected static NSC createInstance() {
            return new NSC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.insert("name").into("users").sql();
         * // Output: INSERT INTO users (name) VALUES (:name)
         * }</pre>
         * 
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for multiple columns with named parameters.
         * 
         * <p>Each column will have a corresponding named parameter in the VALUES clause.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.insert("firstName", "lastName", "email")
         *                 .into("users")
         *                 .sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for a collection of columns with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = NSC.insert(columns).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param propOrColumnNames the collection of property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from a map of column-value pairs with named parameters.
         * 
         * <p>The map keys become both column names and parameter names.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * String sql = NSC.insert(props).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param props the map of property names to values
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object with named parameters.
         * 
         * <p>Property names from the entity become named parameters in the SQL.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * User user = new User();
         * user.setFirstName("John");
         * user.setLastName("Doe");
         * String sql = NSC.insert(user).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object with excluded properties and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * User user = new User();
         * user.setFirstName("John");
         * user.setLastName("Doe");
         * user.setCreatedDate(new Date());
         * 
         * Set<String> excluded = Set.of("createdDate");
         * String sql = NSC.insert(user, excluded).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for an entity class with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.insert(User.class).into("users").sql();
         * // Output: INSERT INTO users (id, first_name, last_name, email) VALUES (:id, :firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class to base the INSERT on
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for an entity class with excluded properties and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> excluded = Set.of("id", "createdDate");
         * String sql = NSC.insert(User.class, excluded).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class to base the INSERT on
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class with named parameters.
         * 
         * <p>This is a convenience method that automatically determines the table name from the entity class.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * @Table("users")
         * public class User { ... }
         * 
         * String sql = NSC.insertInto(User.class).sql();
         * // Output: INSERT INTO users (id, first_name, last_name, email) VALUES (:id, :firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class to insert into
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class with excluded properties and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> excluded = Set.of("id");
         * String sql = NSC.insertInto(User.class, excluded).sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class to insert into
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder with named parameters in MySQL style.
         * 
         * <p>Note: Named parameters in batch inserts may have limited support depending on the database driver.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * String sql = NSC.batchInsert(users).into("users").sql();
         * // Output format depends on the implementation
         * }</pre>
         * 
         * @param propsList list of entities or property maps to insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @beta This API is in beta and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.update("users")
         *                 .set("firstName", "John")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = :firstName WHERE id = :id
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation with named parameters
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with entity class context and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.update("users", User.class)
         *                 .set("firstName", "John")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = :firstName WHERE id = :id
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation with named parameters
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.update(User.class)
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = :firstName, last_name = :lastName WHERE id = :id
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation with named parameters
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class with excluded properties and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> excluded = Set.of("createdDate", "createdBy");
         * String sql = NSC.update(User.class, excluded)
         *                 .set("firstName", "John")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = :firstName WHERE id = :id
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @param excludedPropNames the set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation with named parameters
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.deleteFrom("users")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM users WHERE id = :id
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation with named parameters
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table with entity class context and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.deleteFrom("users", User.class)
         *                 .where(CF.eq("firstName", "John"))
         *                 .sql();
         * // Output: DELETE FROM users WHERE first_name = :firstName
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation with named parameters
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for an entity class with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.deleteFrom(User.class)
         *                 .where(CF.eq("firstName", "John"))
         *                 .sql();
         * // Output: DELETE FROM users WHERE first_name = :firstName
         * }</pre>
         * 
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation with named parameters
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single column or expression using named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.select("COUNT(*)").from("users").where(CF.eq("active", true)).sql();
         * // Output: SELECT COUNT(*) FROM users WHERE active = :active
         * }</pre>
         * 
         * @param selectPart the column name or SQL expression to select
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder with multiple columns using named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.select("firstName", "lastName", "email")
         *                 .from("users")
         *                 .where(CF.eq("active", true))
         *                 .sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", email FROM users WHERE active = :active
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a collection of columns using named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = NSC.select(columns).from("users").sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", email FROM users
         * }</pre>
         * 
         * @param propOrColumnNames the collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases using named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = NSC.select(aliases).from("users").sql();
         * // Output: SELECT first_name AS fname, last_name AS lname FROM users
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.select(User.class).from("users").where(CF.eq("active", true)).sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM users WHERE active = :active
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with sub-entity option and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.select(User.class, true).from("users").sql();
         * // Includes properties from User and any embedded entities
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with excluded properties and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password", "secretKey");
         * String sql = NSC.select(User.class, excluded).from("users").sql();
         * // Selects all User properties except password and secretKey
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with all options and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.select(User.class, true, excluded)
         *                 .from("users")
         *                 .where(CF.eq("active", true))
         *                 .sql();
         * // Output uses named parameter :active
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with named parameters.
         * 
         * <p>This is a convenience method that combines SELECT and FROM operations.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class).where(CF.eq("id", 1)).sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName" FROM users WHERE id = :id
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with table alias and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class, "u")
         *                 .where(CF.eq("u.active", true))
         *                 .sql();
         * // Output uses named parameter :active
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity option and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class, true).sql();
         * // Includes properties from User and any embedded entities with automatic joins
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and sub-entity option using named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class, "u", true).sql();
         * // Includes properties from User and embedded entities with table alias
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with excluded properties and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.selectFrom(User.class, excluded).where(CF.eq("active", true)).sql();
         * // Selects all properties except password, uses :active parameter
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and excluded properties using named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.selectFrom(User.class, "u", excluded).sql();
         * // Selects all properties except password with table alias
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity and exclusion options using named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.selectFrom(User.class, true, excluded).sql();
         * // Selects all properties including sub-entities except password
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with all options and named parameters.
         * 
         * <p>This is the most flexible selectFrom method.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.selectFrom(User.class, "u", true, excluded)
         *                 .where(CF.and(
         *                     CF.eq("u.active", true),
         *                     CF.like("u.email", "%@example.com")
         *                 ))
         *                 .sql();
         * // Complex select with alias, sub-entities, exclusions, and named parameters
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity classes with named parameters.
         * 
         * <p>Used for multi-table queries with joins.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.select(User.class, "u", "user", Order.class, "o", "order")
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .where(CF.eq("u.active", true))
         *                 .sql();
         * // Uses named parameter :active
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for property prefixing of the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for property prefixing of the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity classes with exclusions and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> userExclusions = Set.of("password");
         * Set<String> orderExclusions = Set.of("internalNotes");
         * 
         * String sql = NSC.select(User.class, "u", "user", userExclusions,
         *                        Order.class, "o", "order", orderExclusions)
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .where(CF.eq("u.active", true))
         *                 .sql();
         * // Uses named parameters for conditions
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for property prefixing of the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for property prefixing of the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, Set.of("description"))
         * );
         * String sql = NSC.select(selections)
         *                 .from("users u")
         *                 .where(CF.eq("u.status", "ACTIVE"))
         *                 .sql();
         * // Uses named parameter :status
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining the entities to select
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity classes with named parameters.
         * 
         * <p>Automatically generates the FROM clause based on entity classes.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class, "u", "user", Order.class, "o", "order")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .where(CF.gt("o.amount", 100))
         *                 .sql();
         * // Uses named parameter :amount
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for property prefixing of the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for property prefixing of the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entities with exclusions and named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Set<String> userExclusions = Set.of("password");
         * Set<String> orderExclusions = Set.of("internalNotes");
         * 
         * String sql = NSC.selectFrom(User.class, "u", "user", userExclusions,
         *                            Order.class, "o", "order", orderExclusions)
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .where(CF.between("o.orderDate", startDate, endDate))
         *                 .sql();
         * // Uses named parameters :startDate and :endDate
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for property prefixing of the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for property prefixing of the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT with FROM clause
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple selections with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null)
         * );
         * String sql = NSC.selectFrom(selections)
         *                 .where(CF.in("u.id", Arrays.asList(1, 2, 3)))
         *                 .sql();
         * // Uses named parameters for the IN clause
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining the entities to select
         * @return a new SQLBuilder instance configured for multi-table SELECT with FROM clause
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for a table with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.count("users").where(CF.eq("active", true)).sql();
         * // Output: SELECT COUNT(*) FROM users WHERE active = :active
         * }</pre>
         * 
         * @param tableName the name of the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for an entity class with named parameters.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String sql = NSC.count(User.class)
         *                 .where(CF.and(
         *                     CF.eq("firstName", "John"),
         *                     CF.gt("age", 18)
         *                 ))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM users WHERE first_name = :firstName AND age = :age
         * }</pre>
         * 
         * @param entityClass the entity class to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context and named parameters.
         * 
         * <p>This method generates just the condition part of SQL with named parameters.</p>
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("firstName", "John"),
         *     CF.gt("age", 18),
         *     CF.like("email", "%@example.com")
         * );
         * String sql = NSC.parse(cond, User.class).sql();
         * // Output: first_name = :firstName AND age = :age AND email LIKE :email
         * }</pre>
         * 
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for property-to-column mapping
         * @return a new SQLBuilder instance containing only the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with all capital case (upper case with underscore) field/column naming strategy.
     * This builder generates SQL with named parameters (e.g., :paramName) and converts property names
     * to UPPER_CASE_WITH_UNDERSCORE format.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Simple SELECT with named parameters
     * N.println(NAC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = :id
     * 
     * // INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = NAC.insert(account).into("ACCOUNT").sql();
     * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (:firstName, :lastName)
     * }</pre>
     */
    public static class NAC extends SQLBuilder {

        /**
         * Constructs a new NAC instance with UPPER_CASE_WITH_UNDERSCORE naming policy
         * and named SQL parameter style.
         */
        NAC() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.NAMED_SQL);
        }

        /**
         * Indicates whether this builder generates named SQL parameters.
         * 
         * @return always returns {@code true} for NAC
         */
        @Override
        protected boolean isNamedSql() {
            return true;
        }

        /**
         * Creates a new instance of NAC builder.
         * 
         * @return a new NAC instance
         */
        protected static NAC createInstance() {
            return new NAC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * 
         * @param expr the column expression to insert
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NAC.insert("FIRST_NAME").into("ACCOUNT")}
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified property or column names.
         * Property names will be converted to UPPER_CASE_WITH_UNDERSCORE format.
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NAC.insert("firstName", "lastName").into("ACCOUNT")}
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified collection of property or column names.
         * Property names will be converted to UPPER_CASE_WITH_UNDERSCORE format.
         * 
         * @param propOrColumnNames the collection of property or column names to insert
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NAC.insert(Arrays.asList("firstName", "lastName")).into("ACCOUNT")}
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified property-value map.
         * Property names will be converted to UPPER_CASE_WITH_UNDERSCORE format.
         * 
         * @param props the map of property names to values
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NAC.insert(Map.of("firstName", "John", "lastName", "Doe")).into("ACCOUNT")}
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity object.
         * The entity's properties will be extracted and used for the INSERT statement.
         * Properties marked with @ReadOnly, @ReadOnlyId, or @Transient will be excluded.
         * 
         * @param entity the entity object to insert
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code 
         * Account account = new Account();
         * account.setFirstName("John");
         * NAC.insert(account).into("ACCOUNT").sql();
         * }
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity object with excluded properties.
         * The entity's properties will be extracted and used for the INSERT statement,
         * excluding the specified property names.
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NAC.insert(account, Set.of("createdTime")).into("ACCOUNT")}
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class.
         * All insertable properties of the class will be included.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NAC.insert(Account.class).into("ACCOUNT")}
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with excluded properties.
         * All insertable properties of the class will be included except those specified.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NAC.insert(Account.class, Set.of("id", "createdTime")).into("ACCOUNT")}
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for INSERT operation with table name set
         * @example {@code NAC.insertInto(Account.class).sql()}
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for the specified entity class with excluded properties.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation with table name set
         * @example {@code NAC.insertInto(Account.class, Set.of("id")).sql()}
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for MySQL-style batch inserts.
         * Generates a single INSERT statement with multiple value rows.
         * 
         * @param propsList collection of entities or property maps to insert
         * @return an SQLBuilder configured for batch INSERT operation
         * @example {@code 
         * List<Account> accounts = Arrays.asList(account1, account2, account3);
         * NAC.batchInsert(accounts).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (:firstName_1, :lastName_1), (:firstName_2, :lastName_2), (:firstName_3, :lastName_3)
         * }
         * @Beta This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * @param tableName the name of the table to update
         * @return an SQLBuilder configured for UPDATE operation
         * @example {@code NAC.update("ACCOUNT").set("STATUS", "ACTIVE").where(CF.eq("ID", 1))}
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class mapping.
         * The entity class provides property-to-column mapping information.
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return an SQLBuilder configured for UPDATE operation
         * @example {@code NAC.update("ACCOUNT", Account.class).set("status").where(CF.eq("id", 1))}
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * All updatable properties will be included.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for UPDATE operation
         * @example {@code NAC.update(Account.class).set("status").where(CF.eq("id", 1))}
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class with excluded properties.
         * The table name will be derived from the entity class name or @Table annotation.
         * Properties marked with @NonUpdatable or in the excluded set will be omitted.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the UPDATE
         * @return an SQLBuilder configured for UPDATE operation
         * @example {@code NAC.update(Account.class, Set.of("createdTime")).set("status").where(CF.eq("id", 1))}
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table.
         * 
         * @param tableName the name of the table to delete from
         * @return an SQLBuilder configured for DELETE operation
         * @example {@code NAC.deleteFrom("ACCOUNT").where(CF.eq("STATUS", "INACTIVE"))}
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table with entity class mapping.
         * The entity class provides property-to-column mapping information for conditions.
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return an SQLBuilder configured for DELETE operation
         * @example {@code NAC.deleteFrom("ACCOUNT", Account.class).where(CF.eq("status", "INACTIVE"))}
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for DELETE operation
         * @example {@code NAC.deleteFrom(Account.class).where(CF.eq("status", "INACTIVE"))}
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single select expression.
         * The expression can be a column name, function call, or any valid SQL expression.
         * 
         * @param selectPart the select expression
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         * @example {@code NAC.select("COUNT(*)").from("ACCOUNT")}
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified property or column names.
         * Property names will be converted to UPPER_CASE_WITH_UNDERSCORE format.
         * 
         * @param propOrColumnNames the property or column names to select
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * @example {@code NAC.select("firstName", "lastName", "email").from("ACCOUNT")}
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified collection of property or column names.
         * Property names will be converted to UPPER_CASE_WITH_UNDERSCORE format.
         * 
         * @param propOrColumnNames the collection of property or column names to select
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * @example {@code NAC.select(Arrays.asList("firstName", "lastName")).from("ACCOUNT")}
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * The map keys are property/column names and values are their aliases.
         * 
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         * @example {@code NAC.select(Map.of("firstName", "fname", "lastName", "lname")).from("ACCOUNT")}
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of the specified entity class.
         * Properties marked with @Transient will be excluded.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code NAC.select(Account.class).from("ACCOUNT")}
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class.
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code NAC.select(Account.class, true).from("ACCOUNT")}
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class with exclusions.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code NAC.select(Account.class, Set.of("password")).from("ACCOUNT")}
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class with options.
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code NAC.select(Account.class, true, Set.of("password")).from("ACCOUNT")}
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class.
         * Combines SELECT and FROM operations in a single call.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NAC.selectFrom(Account.class).where(CF.eq("status", "ACTIVE"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class with table alias.
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NAC.selectFrom(Account.class, "a").where(CF.eq("a.status", "ACTIVE"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class with sub-entity option.
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NAC.selectFrom(Account.class, true).where(CF.eq("status", "ACTIVE"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and sub-entity option.
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NAC.selectFrom(Account.class, "a", true).where(CF.eq("a.status", "ACTIVE"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with excluded properties.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NAC.selectFrom(Account.class, Set.of("password")).where(CF.eq("status", "ACTIVE"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and excluded properties.
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NAC.selectFrom(Account.class, "a", Set.of("password")).where(CF.eq("a.status", "ACTIVE"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity option and excluded properties.
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NAC.selectFrom(Account.class, true, Set.of("password")).where(CF.eq("status", "ACTIVE"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with all options.
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NAC.selectFrom(Account.class, "a", true, Set.of("password")).where(CF.eq("a.status", "ACTIVE"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with table and class aliases.
         * This is useful for JOIN queries where columns from multiple tables need distinct aliases.
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code 
         * NAC.select(Account.class, "a", "account", Order.class, "o", "order")
         *    .from("ACCOUNT a")
         *    .join("ORDER o", CF.eq("a.ID", "o.ACCOUNT_ID"))
         * }
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases and exclusions.
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code 
         * NAC.select(Account.class, "a", "account", Set.of("password"), 
         *           Order.class, "o", "order", Set.of("internalNotes"))
         *    .from("ACCOUNT a")
         *    .join("ORDER o", CF.eq("a.ID", "o.ACCOUNT_ID"))
         * }
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * This is the most flexible way to select from multiple entities with different configurations.
         * 
         * @param multiSelects list of Selection configurations for each entity
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         * @example {@code 
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, Set.of("password")),
         *     new Selection(Order.class, "o", "order", null, true, null)
         * );
         * NAC.select(selections).from("ACCOUNT a").join("ORDER o", CF.eq("a.ID", "o.ACCOUNT_ID"))
         * }
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with table and class aliases.
         * Automatically generates the FROM clause with proper table names.
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code 
         * NAC.selectFrom(Account.class, "a", "account", Order.class, "o", "order")
         *    .where(CF.eq("a.ID", "o.ACCOUNT_ID"))
         * }
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with aliases and exclusions.
         * Automatically generates the FROM clause with proper table names.
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code 
         * NAC.selectFrom(Account.class, "a", "account", Set.of("password"),
         *               Order.class, "o", "order", Set.of("internalNotes"))
         *    .where(CF.eq("a.ID", "o.ACCOUNT_ID"))
         * }
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * Automatically generates the FROM clause based on the selection configurations.
         * 
         * @param multiSelects list of Selection configurations for each entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if multiSelects is null or empty
         * @example {@code 
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, Set.of("password")),
         *     new Selection(Order.class, "o", "order", null, true, null)
         * );
         * NAC.selectFrom(selections).where(CF.eq("a.ID", "o.ACCOUNT_ID"))
         * }
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         * 
         * @param tableName the name of the table to count rows from
         * @return an SQLBuilder configured for COUNT query
         * @example {@code NAC.count("ACCOUNT").where(CF.eq("STATUS", "ACTIVE"))}
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for COUNT query
         * @example {@code NAC.count(Account.class).where(CF.eq("status", "ACTIVE"))}
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL using the entity class for property mapping.
         * This method is useful for generating just the SQL fragment for a condition.
         * 
         * @param cond the condition to parse
         * @param entityClass the entity class for property-to-column mapping
         * @return an SQLBuilder containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         * @example {@code 
         * Condition cond = CF.and(CF.eq("status", "ACTIVE"), CF.gt("balance", 1000));
         * String sql = NAC.parse(cond, Account.class).sql();
         * // Output: STATUS = :status AND BALANCE > :balance
         * }
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with lower camel case field/column naming strategy.
     * This builder generates SQL with named parameters (e.g., :paramName) and preserves
     * property names in lowerCamelCase format.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Simple SELECT with named parameters
     * N.println(NLC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // Output: SELECT firstName, lastName FROM account WHERE id = :id
     * 
     * // INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = NLC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (firstName, lastName) VALUES (:firstName, :lastName)
     * }</pre>
     */
    public static class NLC extends SQLBuilder {

        /**
         * Constructs a new NLC instance with LOWER_CAMEL_CASE naming policy
         * and named SQL parameter style.
         */
        NLC() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.NAMED_SQL);
        }

        /**
         * Indicates whether this builder generates named SQL parameters.
         * 
         * @return always returns {@code true} for NLC
         */
        @Override
        protected boolean isNamedSql() {
            return true;
        }

        /**
         * Creates a new instance of NLC builder.
         * 
         * @return a new NLC instance
         */
        protected static NLC createInstance() {
            return new NLC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * 
         * @param expr the column expression to insert
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NLC.insert("firstName").into("account")}
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified property or column names.
         * Property names will be preserved in lowerCamelCase format.
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NLC.insert("firstName", "lastName").into("account")}
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified collection of property or column names.
         * Property names will be preserved in lowerCamelCase format.
         * 
         * @param propOrColumnNames the collection of property or column names to insert
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NLC.insert(Arrays.asList("firstName", "lastName")).into("account")}
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified property-value map.
         * Property names will be preserved in lowerCamelCase format.
         * 
         * @param props the map of property names to values
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NLC.insert(Map.of("firstName", "John", "lastName", "Doe")).into("account")}
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity object.
         * The entity's properties will be extracted and used for the INSERT statement.
         * Properties marked with @ReadOnly, @ReadOnlyId, or @Transient will be excluded.
         * 
         * @param entity the entity object to insert
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code 
         * Account account = new Account();
         * account.setFirstName("John");
         * NLC.insert(account).into("account").sql();
         * }
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity object with excluded properties.
         * The entity's properties will be extracted and used for the INSERT statement,
         * excluding the specified property names.
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NLC.insert(account, Set.of("createdTime")).into("account")}
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class.
         * All insertable properties of the class will be included.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NLC.insert(Account.class).into("account")}
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with excluded properties.
         * All insertable properties of the class will be included except those specified.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation
         * @example {@code NLC.insert(Account.class, Set.of("id", "createdTime")).into("account")}
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for INSERT operation with table name set
         * @example {@code NLC.insertInto(Account.class).sql()}
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for the specified entity class with excluded properties.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation with table name set
         * @example {@code NLC.insertInto(Account.class, Set.of("id")).sql()}
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for MySQL-style batch inserts.
         * Generates a single INSERT statement with multiple value rows.
         * 
         * @param propsList collection of entities or property maps to insert
         * @return an SQLBuilder configured for batch INSERT operation
         * @example {@code 
         * List<Account> accounts = Arrays.asList(account1, account2, account3);
         * NLC.batchInsert(accounts).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName) VALUES (:firstName_1, :lastName_1), (:firstName_2, :lastName_2), (:firstName_3, :lastName_3)
         * }
         * @Beta This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * @param tableName the name of the table to update
         * @return an SQLBuilder configured for UPDATE operation
         * @example {@code NLC.update("account").set("status", "active").where(CF.eq("id", 1))}
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class mapping.
         * The entity class provides property-to-column mapping information.
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return an SQLBuilder configured for UPDATE operation
         * @example {@code NLC.update("account", Account.class).set("status").where(CF.eq("id", 1))}
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * All updatable properties will be included.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for UPDATE operation
         * @example {@code NLC.update(Account.class).set("status").where(CF.eq("id", 1))}
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class with excluded properties.
         * The table name will be derived from the entity class name or @Table annotation.
         * Properties marked with @NonUpdatable or in the excluded set will be omitted.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the UPDATE
         * @return an SQLBuilder configured for UPDATE operation
         * @example {@code NLC.update(Account.class, Set.of("createdTime")).set("status").where(CF.eq("id", 1))}
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table.
         * 
         * @param tableName the name of the table to delete from
         * @return an SQLBuilder configured for DELETE operation
         * @example {@code NLC.deleteFrom("account").where(CF.eq("status", "inactive"))}
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table with entity class mapping.
         * The entity class provides property-to-column mapping information for conditions.
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return an SQLBuilder configured for DELETE operation
         * @example {@code NLC.deleteFrom("account", Account.class).where(CF.eq("status", "inactive"))}
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for DELETE operation
         * @example {@code NLC.deleteFrom(Account.class).where(CF.eq("status", "inactive"))}
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single select expression.
         * The expression can be a column name, function call, or any valid SQL expression.
         * 
         * @param selectPart the select expression
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         * @example {@code NLC.select("COUNT(*)").from("account")}
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified property or column names.
         * Property names will be preserved in lowerCamelCase format.
         * 
         * @param propOrColumnNames the property or column names to select
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * @example {@code NLC.select("firstName", "lastName", "email").from("account")}
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified collection of property or column names.
         * Property names will be preserved in lowerCamelCase format.
         * 
         * @param propOrColumnNames the collection of property or column names to select
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         * @example {@code NLC.select(Arrays.asList("firstName", "lastName")).from("account")}
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * The map keys are property/column names and values are their aliases.
         * 
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         * @example {@code NLC.select(Map.of("firstName", "fname", "lastName", "lname")).from("account")}
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of the specified entity class.
         * Properties marked with @Transient will be excluded.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code NLC.select(Account.class).from("account")}
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class.
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code NLC.select(Account.class, true).from("account")}
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class with exclusions.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code NLC.select(Account.class, Set.of("password")).from("account")}
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class with options.
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code NLC.select(Account.class, true, Set.of("password")).from("account")}
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class.
         * Combines SELECT and FROM operations in a single call.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NLC.selectFrom(Account.class).where(CF.eq("status", "active"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class with table alias.
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NLC.selectFrom(Account.class, "a").where(CF.eq("a.status", "active"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class with sub-entity option.
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NLC.selectFrom(Account.class, true).where(CF.eq("status", "active"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and sub-entity option.
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NLC.selectFrom(Account.class, "a", true).where(CF.eq("a.status", "active"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with excluded properties.
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NLC.selectFrom(Account.class, Set.of("password")).where(CF.eq("status", "active"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and excluded properties.
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NLC.selectFrom(Account.class, "a", Set.of("password")).where(CF.eq("a.status", "active"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity option and excluded properties.
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NLC.selectFrom(Account.class, true, Set.of("password")).where(CF.eq("status", "active"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with all options.
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code NLC.selectFrom(Account.class, "a", true, Set.of("password")).where(CF.eq("a.status", "active"))}
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with table and class aliases.
         * This is useful for JOIN queries where columns from multiple tables need distinct aliases.
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code 
         * NLC.select(Account.class, "a", "account", Order.class, "o", "order")
         *    .from("account a")
         *    .join("order o", CF.eq("a.id", "o.accountId"))
         * }
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases and exclusions.
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return an SQLBuilder configured for SELECT operation
         * @example {@code 
         * NLC.select(Account.class, "a", "account", Set.of("password"), 
         *           Order.class, "o", "order", Set.of("internalNotes"))
         *    .from("account a")
         *    .join("order o", CF.eq("a.id", "o.accountId"))
         * }
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * This is the most flexible way to select from multiple entities with different configurations.
         * 
         * @param multiSelects list of Selection configurations for each entity
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         * @example {@code 
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, Set.of("password")),
         *     new Selection(Order.class, "o", "order", null, true, null)
         * );
         * NLC.select(selections).from("account a").join("order o", CF.eq("a.id", "o.accountId"))
         * }
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with table and class aliases.
         * Automatically generates the FROM clause with proper table names.
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code 
         * NLC.selectFrom(Account.class, "a", "account", Order.class, "o", "order")
         *    .where(CF.eq("a.id", "o.accountId"))
         * }
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with aliases and exclusions.
         * Automatically generates the FROM clause with proper table names.
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @example {@code 
         * NLC.selectFrom(Account.class, "a", "account", Set.of("password"),
         *               Order.class, "o", "order", Set.of("internalNotes"))
         *    .where(CF.eq("a.id", "o.accountId"))
         * }
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * Automatically generates the FROM clause based on the selection configurations.
         * 
         * @param multiSelects list of Selection configurations for each entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if multiSelects is null or empty
         * @example {@code 
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, Set.of("password")),
         *     new Selection(Order.class, "o", "order", null, true, null)
         * );
         * NLC.selectFrom(selections).where(CF.eq("a.id", "o.accountId"))
         * }
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         * 
         * @param tableName the name of the table to count rows from
         * @return an SQLBuilder configured for COUNT query
         * @example {@code NLC.count("account").where(CF.eq("status", "active"))}
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for COUNT query
         * @example {@code NLC.count(Account.class).where(CF.eq("status", "active"))}
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL using the entity class for property mapping.
         * This method is useful for generating just the SQL fragment for a condition.
         * 
         * @param cond the condition to parse
         * @param entityClass the entity class for property-to-column mapping
         * @return an SQLBuilder containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         * @example {@code 
         * Condition cond = CF.and(CF.eq("status", "active"), CF.gt("balance", 1000));
         * String sql = NLC.parse(cond, Account.class).sql();
         * // Output: status = :status AND balance > :balance
         * }
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with {@code NamingPolicy.NO_CHANGE} field/column naming strategy.
     * This class generates SQL with MyBatis-style named parameters (#{paramName}).
     * 
     * <p>This builder preserves the exact case of property and column names as they are provided,
     * without any transformation. It's useful when working with databases that have specific
     * naming conventions that should not be altered.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Column names are preserved exactly as written
     * String sql = MSB.select("first_Name", "last_NaMe")
     *                 .from("account")
     *                 .where(CF.eq("last_NaMe", 1))
     *                 .sql();
     * // Output: SELECT first_Name, last_NaMe FROM account WHERE last_NaMe = #{last_NaMe}
     * }</pre>
     * 
     * @deprecated Use {@link NSB} or other non-deprecated builders instead
     */
    @Deprecated
    public static class MSB extends SQLBuilder {

        MSB() {
            super(NamingPolicy.NO_CHANGE, SQLPolicy.IBATIS_SQL);
        }

        protected static MSB createInstance() {
            return new MSB();
        }

        /**
         * Creates an INSERT statement for a single column.
         * 
         * <p>This is a convenience method equivalent to calling {@code insert(new String[]{expr})}.</p>
         *
         * @param expr the column name or expression to insert
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.insert("name").into("users").sql();
         * // Output: INSERT INTO users (name) VALUES (#{name})
         * }</pre>
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for the specified columns.
         * 
         * <p>The column names will be used both in the INSERT column list and as parameter names
         * in the VALUES clause.</p>
         *
         * @param propOrColumnNames the property or column names to include in the INSERT
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.insert("firstName", "lastName", "email")
         *                 .into("users")
         *                 .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) 
         * //         VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for the specified columns provided as a collection.
         * 
         * <p>This method is useful when the column names are dynamically determined.</p>
         *
         * @param propOrColumnNames collection of property or column names to include
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("id", "name", "status");
         * String sql = MSB.insert(columns).into("products").sql();
         * // Output: INSERT INTO products (id, name, status) 
         * //         VALUES (#{id}, #{name}, #{status})
         * }</pre>
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement using a map of column names to values.
         * 
         * <p>The map keys represent column names, and the values are the corresponding
         * values to be inserted. This is useful for dynamic INSERT statements.</p>
         *
         * @param props map of column names to their values
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("name", "John");
         * data.put("age", 30);
         * String sql = MSB.insert(data).into("users").sql();
         * // Output: INSERT INTO users (name, age) VALUES (#{name}, #{age})
         * }</pre>
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement based on an entity object.
         * 
         * <p>All non-null properties of the entity will be included in the INSERT statement,
         * except those marked with {@code @Transient}, {@code @ReadOnly}, or {@code @ReadOnlyId}
         * annotations.</p>
         *
         * @param entity the entity object containing data to insert
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * String sql = MSB.insert(user).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName, email) 
         * //         VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement based on an entity object, excluding specified properties.
         * 
         * <p>This method allows fine-grained control over which properties are included
         * in the INSERT statement.</p>
         *
         * @param entity the entity object containing data to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * User user = new User();
         * Set<String> exclude = new HashSet<>(Arrays.asList("createdDate", "modifiedDate"));
         * String sql = MSB.insert(user, exclude).into("users").sql();
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for all insertable properties of an entity class.
         * 
         * <p>This generates an INSERT template based on the entity class structure,
         * including all properties except those annotated with {@code @Transient},
         * {@code @ReadOnly}, or {@code @ReadOnlyId}.</p>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.insert(User.class).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName, email, age) 
         * //         VALUES (#{firstName}, #{lastName}, #{email}, #{age})
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class, excluding specified properties.
         * 
         * <p>This method provides control over which properties are included when
         * generating an INSERT template from an entity class.</p>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = MSB.insert(User.class, exclude).into("users").sql();
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for an entity class with automatic table name detection.
         * 
         * <p>The table name is determined from the {@code @Table} annotation on the entity class,
         * or derived from the class name if no annotation is present.</p>
         *
         * @param entityClass the entity class to insert
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * @Table("users")
         * public class User { ... }
         * 
         * String sql = MSB.insertInto(User.class).sql();
         * // Output: INSERT INTO users (firstName, lastName, email) 
         * //         VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class with automatic table name detection,
         * excluding specified properties.
         * 
         * <p>Combines automatic table name detection with property exclusion.</p>
         *
         * @param entityClass the entity class to insert
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT statement for multiple records in MySQL style.
         * 
         * <p>This generates a single INSERT statement with multiple value sets,
         * which is more efficient than multiple individual INSERT statements.</p>
         * 
         * <p>The method accepts a collection of entities or maps. All items must have
         * the same structure (same properties/keys).</p>
         *
         * @param propsList collection of entities or property maps to insert
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * String sql = MSB.batchInsert(users).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName) 
         * //         VALUES (#{0.firstName}, #{0.lastName}), 
         * //                (#{1.firstName}, #{1.lastName})
         * }</pre>
         * 
         * @Beta This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for the specified table.
         * 
         * <p>After calling this method, use {@code set()} to specify which columns to update.</p>
         *
         * @param tableName the name of the table to update
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.update("users")
         *                 .set("status", "lastModified")
         *                 .where(CF.eq("id", 123))
         *                 .sql();
         * // Output: UPDATE users SET status = #{status}, lastModified = #{lastModified} 
         * //         WHERE id = #{id}
         * }</pre>
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class context.
         * 
         * <p>This method is useful when you want to specify a custom table name
         * but still use entity class metadata for column mapping.</p>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for column mapping
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.update("user_archive", User.class)
         *                 .set("status")
         *                 .where(CF.eq("userId", 123))
         *                 .sql();
         * }</pre>
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class with automatic table name detection.
         * 
         * <p>All updatable properties (excluding those marked with {@code @ReadOnly},
         * {@code @ReadOnlyId}, {@code @NonUpdatable}, or {@code @Transient}) will be
         * included in the SET clause.</p>
         *
         * @param entityClass the entity class to update
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.update(User.class)
         *                 .where(CF.eq("id", 123))
         *                 .sql();
         * // Output: UPDATE users SET firstName = #{firstName}, lastName = #{lastName}, 
         * //         email = #{email} WHERE id = #{id}
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class, excluding specified properties.
         * 
         * <p>This method provides fine-grained control over which properties are included
         * in the UPDATE statement's SET clause.</p>
         *
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the update
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("createdDate", "createdBy"));
         * String sql = MSB.update(User.class, exclude)
         *                 .where(CF.eq("id", 123))
         *                 .sql();
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE statement for the specified table.
         * 
         * <p>Use {@code where()} to add conditions to the DELETE statement.
         * Be careful with DELETE statements without WHERE clauses.</p>
         *
         * @param tableName the name of the table to delete from
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.deleteFrom("users")
         *                 .where(CF.eq("status", "INACTIVE"))
         *                 .sql();
         * // Output: DELETE FROM users WHERE status = #{status}
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE statement for a table with entity class context.
         * 
         * <p>This method is useful when you want to use a custom table name
         * but still benefit from entity class metadata for column mapping in WHERE conditions.</p>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for column mapping
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE statement for an entity class with automatic table name detection.
         * 
         * <p>The table name is determined from the {@code @Table} annotation on the entity class,
         * or derived from the class name if no annotation is present.</p>
         *
         * @param entityClass the entity class representing the table
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.deleteFrom(User.class)
         *                 .where(CF.lt("lastLoginDate", someDate))
         *                 .sql();
         * // Output: DELETE FROM users WHERE lastLoginDate < #{lastLoginDate}
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression or column.
         * 
         * <p>The expression can be a simple column name, a function call, or any valid SQL expression.</p>
         *
         * @param selectPart the SELECT expression (column, function, etc.)
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.select("COUNT(*)").from("users").sql();
         * // Output: SELECT COUNT(*) FROM users
         * 
         * String sql2 = MSB.select("MAX(salary)").from("employees").sql();
         * // Output: SELECT MAX(salary) FROM employees
         * }</pre>
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement with multiple columns or expressions.
         * 
         * <p>Each string in the array represents a column name or expression to be selected.</p>
         *
         * @param propOrColumnNames array of property or column names to select
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.select("firstName", "lastName", "email")
         *                 .from("users")
         *                 .where(CF.eq("active", true))
         *                 .sql();
         * // Output: SELECT firstName, lastName, email FROM users WHERE active = #{active}
         * }</pre>
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement with columns specified as a collection.
         * 
         * <p>This method is useful when the columns to select are determined dynamically.</p>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> columns = getRequiredColumns();
         * String sql = MSB.select(columns)
         *                 .from("users")
         *                 .sql();
         * }</pre>
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with column aliases.
         * 
         * <p>The map keys represent the column names or expressions, and the values
         * represent their aliases in the result set.</p>
         *
         * @param propOrColumnNameAliases map of column names/expressions to their aliases
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = MSB.select(aliases).from("users").sql();
         * // Output: SELECT firstName AS fname, lastName AS lname FROM users
         * }</pre>
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all columns of an entity class.
         * 
         * <p>Selects all properties that are not marked with {@code @Transient} annotation.</p>
         *
         * @param entityClass the entity class to select from
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.select(User.class).from("users").sql();
         * // Output: SELECT id, firstName, lastName, email FROM users
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with sub-entity control.
         * 
         * <p>When {@code includeSubEntityProperties} is true, properties that are themselves
         * entities will have their properties included in the selection with prefixed names.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * // If User has an Address property
         * String sql = MSB.select(User.class, true).from("users").sql();
         * // May include: id, firstName, address.street, address.city, etc.
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class, excluding specified properties.
         * 
         * <p>This method allows you to select most properties of an entity while excluding
         * a few specific ones.</p>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from selection
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password", "secretKey"));
         * String sql = MSB.select(User.class, exclude).from("users").sql();
         * // Output: SELECT id, firstName, lastName, email FROM users
         * // (password and secretKey are excluded)
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement for an entity class with full control over selection.
         * 
         * <p>This method combines sub-entity inclusion control with property exclusion,
         * providing maximum flexibility in determining what to select.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from selection
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for an entity class.
         * 
         * <p>This is a convenience method that combines select() and from() operations.
         * The table name is automatically determined from the entity class.</p>
         *
         * @param entityClass the entity class to select from
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.selectFrom(User.class)
         *                 .where(CF.eq("active", true))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email FROM users WHERE active = #{active}
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement for an entity class with a table alias.
         * 
         * <p>The alias can be used in WHERE conditions and JOIN clauses to disambiguate
         * column references.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.selectFrom(User.class, "u")
         *                 .where(CF.eq("u.active", true))
         *                 .sql();
         * // Output: SELECT u.id, u.firstName, u.lastName FROM users u WHERE u.active = #{active}
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement with sub-entity inclusion control.
         * 
         * <p>When sub-entities are included, the query may generate JOINs to fetch
         * related entity data in a single query.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with table alias and sub-entity control.
         * 
         * <p>Combines table aliasing with sub-entity property inclusion for complex queries.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement excluding specified properties.
         * 
         * <p>This is a convenience method that combines property exclusion with
         * automatic FROM clause generation.</p>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with alias and property exclusion.
         * 
         * <p>Provides table aliasing while excluding specific properties from selection.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with sub-entity control and property exclusion.
         * 
         * <p>This method provides control over both sub-entity inclusion and property exclusion
         * without specifying a table alias.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with full control over all options.
         * 
         * <p>This is the most flexible selectFrom method, allowing control over table alias,
         * sub-entity inclusion, and property exclusion.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = MSB.selectFrom(User.class, "u", true, exclude)
         *                 .where(CF.eq("u.active", true))
         *                 .sql();
         * // May generate complex query with JOINs for sub-entities
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for joining two entity classes.
         * 
         * <p>This method sets up a query that will select columns from two different tables,
         * preparing for a JOIN operation. Each entity can have its own table alias and
         * result set column prefix.</p>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA column prefix for the first entity's columns in the result
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB column prefix for the second entity's columns in the result
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.select(User.class, "u", "user", Order.class, "o", "order")
         *                 .from("users", "u")
         *                 .join("orders", "o").on("u.id = o.user_id")
         *                 .sql();
         * // Output: SELECT u.id AS "user.id", u.name AS "user.name", 
         * //                o.id AS "order.id", o.total AS "order.total"
         * //         FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for joining two entity classes with property exclusion.
         * 
         * <p>This method extends the two-entity select by allowing you to exclude specific
         * properties from each entity class independently.</p>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA column prefix for the first entity's columns
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB column prefix for the second entity's columns
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entity classes with detailed configuration.
         * 
         * <p>This is the most flexible select method, supporting any number of entities
         * with individual configuration for each through the Selection objects.</p>
         *
         * @param multiSelects list of Selection configurations for each entity
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, excludeSet)
         * );
         * String sql = MSB.select(selections).from(...).sql();
         * }</pre>
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for joining two entities.
         * 
         * <p>This is a convenience method that combines multi-entity selection with
         * automatic FROM clause generation including proper table aliases.</p>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA column prefix for the first entity's columns
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB column prefix for the second entity's columns
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for two entities with property exclusion.
         * 
         * <p>Combines multi-entity selection with property exclusion and automatic
         * FROM clause generation.</p>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA column prefix for the first entity's columns
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB column prefix for the second entity's columns
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement for multiple entities with detailed configuration.
         * 
         * <p>This method automatically generates the appropriate FROM clause with all
         * necessary table names and aliases based on the Selection configurations.</p>
         *
         * @param multiSelects list of Selection configurations for each entity
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for the specified table.
         * 
         * <p>This is a convenience method for creating count queries.</p>
         *
         * @param tableName the table to count rows from
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.count("users")
         *                 .where(CF.eq("active", true))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM users WHERE active = #{active}
         * }</pre>
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>The table name is automatically determined from the entity class.</p>
         *
         * @param entityClass the entity class representing the table
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSB.count(User.class)
         *                 .where(CF.between("age", 18, 65))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM users WHERE age BETWEEN #{age} AND #{age2}
         * }</pre>
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context.
         * 
         * <p>This method is useful for generating just the SQL representation of a condition,
         * without building a complete statement. It can be used for debugging or for
         * building complex dynamic queries.</p>
         *
         * @param cond the condition to parse
         * @param entityClass the entity class for column name mapping
         * @return the SQLBuilder instance containing the parsed condition
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("active", true),
         *     CF.gt("age", 18)
         * );
         * String sql = MSB.parse(cond, User.class).sql();
         * // Output: active = #{active} AND age > #{age}
         * }</pre>
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * MyBatis-style SQL builder with snake_case field/column naming strategy.
     * This class automatically converts camelCase property names to snake_case column names.
     * 
     * <p>This builder is ideal for databases that follow the snake_case naming convention
     * (e.g., user_name, first_name) while keeping Java property names in camelCase.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Property names are automatically converted to snake_case
     * String sql = MSC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(CF.eq("userId", 1))
     *                 .sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" 
     * //         FROM account WHERE user_id = #{userId}
     * }</pre>
     * 
     * @deprecated Use {@link NSC} or other non-deprecated builders instead
     */
    @Deprecated
    public static class MSC extends SQLBuilder {

        /**
         * Package-private constructor for internal use only.
         * Creates a new MSC instance with snake_case naming policy and MyBatis SQL format.
         */
        MSC() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Factory method to create a new MSC instance.
         * 
         * @return a new MSC SQLBuilder instance
         */
        protected static MSC createInstance() {
            return new MSC();
        }

        /**
         * Creates an INSERT statement for a single column.
         * 
         * <p>The property name will be converted to snake_case for the column name.</p>
         *
         * @param expr the property name or expression to insert
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.insert("userName").into("users").sql();
         * // Output: INSERT INTO users (user_name) VALUES (#{userName})
         * }</pre>
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for the specified properties.
         * 
         * <p>Property names will be converted to snake_case for column names,
         * while keeping the original names for parameter placeholders.</p>
         *
         * @param propOrColumnNames the property names to include in the INSERT
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.insert("firstName", "lastName", "emailAddress")
         *                 .into("users")
         *                 .sql();
         * // Output: INSERT INTO users (first_name, last_name, email_address) 
         * //         VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for properties provided as a collection.
         * 
         * <p>This method is useful when property names are determined at runtime.</p>
         *
         * @param propOrColumnNames collection of property names to include
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> props = Arrays.asList("firstName", "lastName");
         * String sql = MSC.insert(props).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement using a map of properties to values.
         * 
         * <p>Map keys (property names) will be converted to snake_case for column names.</p>
         *
         * @param props map of property names to their values
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("firstName", "John");
         * data.put("lastName", "Doe");
         * String sql = MSC.insert(data).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) 
         * //         VALUES (#{firstName}, #{lastName})
         * }</pre>
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement based on an entity object.
         * 
         * <p>Property names from the entity will be converted to snake_case for column names.
         * Properties marked with {@code @Transient}, {@code @ReadOnly}, or {@code @ReadOnlyId}
         * annotations will be excluded.</p>
         *
         * @param entity the entity object containing data to insert
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * User user = new User("John", "Doe");
         * String sql = MSC.insert(user).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement based on an entity object, excluding specified properties.
         * 
         * <p>Provides fine-grained control over which properties are included,
         * with automatic snake_case conversion for column names.</p>
         *
         * @param entity the entity object containing data to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * User user = new User("John", "Doe", new Date());
         * Set<String> exclude = Set.of("createdDate");
         * String sql = MSC.insert(user, exclude).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement template for an entity class.
         * 
         * <p>Generates an INSERT template with all insertable properties,
         * automatically converting property names to snake_case column names.</p>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.insert(User.class).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement template for an entity class, excluding specified properties.
         * 
         * <p>Allows selective property inclusion with automatic snake_case conversion.</p>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = Set.of("id", "createdDate");
         * String sql = MSC.insert(User.class, exclude).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement with automatic table name detection.
         * 
         * <p>The table name is determined from the {@code @Table} annotation,
         * with the entity's property names converted to snake_case columns.</p>
         *
         * @param entityClass the entity class to insert
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * @Table("users")
         * class User { ... }
         * 
         * String sql = MSC.insertInto(User.class).sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT statement with automatic table name detection, excluding properties.
         * 
         * <p>Combines automatic table name detection with selective property inclusion.</p>
         *
         * @param entityClass the entity class to insert
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT statement for multiple records.
         * 
         * <p>Generates a single INSERT with multiple value sets, with property names
         * converted to snake_case for column names.</p>
         *
         * @param propsList collection of entities or property maps to insert
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * String sql = MSC.batchInsert(users).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) 
         * //         VALUES (#{0.firstName}, #{0.lastName}), 
         * //                (#{1.firstName}, #{1.lastName})
         * }</pre>
         * 
         * @Beta This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for the specified table.
         * 
         * <p>Use {@code set()} to specify columns to update. Column names in conditions
         * will be converted from camelCase to snake_case.</p>
         *
         * @param tableName the name of the table to update
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.update("users")
         *                 .set("firstName", "lastName")
         *                 .where(CF.eq("userId", 123))
         *                 .sql();
         * // Output: UPDATE users SET first_name = #{firstName}, last_name = #{lastName} 
         * //         WHERE user_id = #{userId}
         * }</pre>
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class context.
         * 
         * <p>Property names will be automatically converted to snake_case column names.</p>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.update("users", User.class)
         *                 .set("firstName", "lastName")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = #{firstName}, last_name = #{lastName} WHERE id = #{id}
         * }</pre>
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class.
         * 
         * <p>All updatable properties will be included in the SET clause,
         * with automatic conversion to snake_case column names.</p>
         *
         * @param entityClass the entity class to update
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.update(User.class)
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = #{firstName}, last_name = #{lastName} WHERE id = #{id}
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class, excluding specified properties.
         * 
         * <p>Provides control over which properties to update, with automatic
         * snake_case conversion for column names.</p>
         *
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the update
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = Set.of("id", "createdDate");
         * String sql = MSC.update(User.class, exclude)
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = #{firstName}, last_name = #{lastName} WHERE id = #{id}
         * }</pre>
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE statement for the specified table.
         * 
         * <p>Property names in WHERE conditions will be converted to snake_case column names.</p>
         *
         * @param tableName the name of the table to delete from
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.deleteFrom("users")
         *                 .where(CF.eq("userId", 123))
         *                 .sql();
         * // Output: DELETE FROM users WHERE user_id = #{userId}
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE statement for a table with entity class context.
         * 
         * <p>Provides property name mapping for WHERE conditions.</p>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.deleteFrom("users", User.class)
         *                 .where(CF.eq("userId", 123))
         *                 .sql();
         * // Output: DELETE FROM users WHERE user_id = #{userId}
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE statement for an entity class.
         * 
         * <p>The table name is determined from the {@code @Table} annotation.</p>
         *
         * @param entityClass the entity class to delete from
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.deleteFrom(User.class)
         *                 .where(CF.eq("id", 123))
         *                 .sql();
         * // Output: DELETE FROM users WHERE id = #{id}
         * }</pre>
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression or column.
         * 
         * <p>This method accepts raw SQL expressions or column names.</p>
         *
         * @param selectPart the SQL expression or column name to select
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.select("COUNT(*)").from("users").sql();
         * // Output: SELECT COUNT(*) FROM users
         * 
         * String sql2 = MSC.select("firstName").from("users").sql();
         * // Output: SELECT first_name AS "firstName" FROM users
         * }</pre>
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement for the specified columns or properties.
         * 
         * <p>Property names will be converted to snake_case column names with aliases.</p>
         *
         * @param propOrColumnNames the property or column names to select
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.select("firstName", "lastName", "emailAddress")
         *                 .from("users")
         *                 .sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", 
         * //               email_address AS "emailAddress" FROM users
         * }</pre>
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement for columns provided as a collection.
         * 
         * <p>Useful when column names are determined at runtime.</p>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName");
         * String sql = MSC.select(columns).from("users").sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM users
         * }</pre>
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with custom column aliases.
         * 
         * <p>Map keys are property/column names, values are their aliases.</p>
         *
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = MSC.select(aliases).from("users").sql();
         * // Output: SELECT first_name AS "fname", last_name AS "lname" FROM users
         * }</pre>
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all properties of an entity class.
         * 
         * <p>Properties marked with {@code @Transient} annotation will be excluded.</p>
         *
         * @param entityClass the entity class to select from
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.select(User.class).from("users").sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", 
         * //               email AS "email" FROM users
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with sub-entity control.
         * 
         * <p>When includeSubEntityProperties is true, properties of embedded entities
         * will also be included in the selection.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class, excluding specified properties.
         * 
         * <p>Allows fine-grained control over which properties to include in the selection.</p>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from selection
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> exclude = Set.of("password", "secretKey");
         * String sql = MSC.select(User.class, exclude).from("users").sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM users
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement for an entity class with full control options.
         * 
         * <p>Combines sub-entity inclusion control with property exclusion.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @param excludedPropNames set of property names to exclude from selection
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT statement with automatic table name detection.
         * 
         * <p>The table name is determined from the {@code @Table} annotation.</p>
         *
         * @param entityClass the entity class to select from
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * @Table("users")
         * class User { ... }
         * 
         * String sql = MSC.selectFrom(User.class).sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM users
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a complete SELECT statement with table alias.
         * 
         * <p>The table alias will be used in the FROM clause and can be referenced
         * in WHERE conditions.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.selectFrom(User.class, "u")
         *                 .where(CF.eq("u.active", true))
         *                 .sql();
         * // Output: SELECT u.first_name AS "firstName", u.last_name AS "lastName" 
         * //         FROM users u WHERE u.active = #{u.active}
         * }</pre>
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT statement with sub-entity inclusion control.
         * 
         * <p>When includeSubEntityProperties is true, performs joins for embedded entities.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement with table alias and sub-entity control.
         * 
         * <p>Combines table aliasing with sub-entity property inclusion.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement excluding specified properties.
         * 
         * <p>Automatically determines table name from {@code @Table} annotation.</p>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement with table alias and property exclusion.
         * 
         * <p>Combines table aliasing with selective property inclusion.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement with sub-entity control and property exclusion.
         * 
         * <p>Provides full control over property selection with automatic table detection.</p>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT statement with full control over all options.
         * 
         * <p>This is the most comprehensive selectFrom method, providing control over
         * table alias, sub-entity inclusion, and property exclusion.</p>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for joining two entity classes.
         * 
         * <p>This method sets up a join between two tables with specified aliases
         * for both table names and result set mapping.</p>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.select(User.class, "u", "user", 
         *                        Order.class, "o", "order")
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .sql();
         * // Output: SELECT u.first_name AS "user.firstName", u.last_name AS "user.lastName",
         * //               o.order_id AS "order.orderId", o.total AS "order.total" 
         * //         FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for joining two entity classes with property exclusion.
         * 
         * <p>Provides control over which properties to include from each entity in the join.</p>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entity selections.
         * 
         * <p>This method supports complex queries involving multiple entities with
         * individual configuration for each selection.</p>
         *
         * @param multiSelects list of Selection objects defining entities and their configurations
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, Set.of("description"))
         * );
         * String sql = MSC.select(selections)
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .join("products p").on("o.product_id = p.id")
         *                 .sql();
         * }</pre>
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT statement for joining two entities with automatic FROM clause.
         * 
         * <p>Automatically generates the FROM clause based on entity annotations.</p>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a complete SELECT statement for joining entities with property exclusion.
         * 
         * <p>Combines automatic FROM clause generation with selective property inclusion.</p>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a complete SELECT statement for multiple entities with automatic FROM clause.
         * 
         * <p>Generates both SELECT and FROM clauses based on the provided selections.</p>
         *
         * @param multiSelects list of Selection objects defining entities and their configurations
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a SELECT COUNT(*) statement for the specified table.
         * 
         * <p>Generates a count query to get the total number of rows in a table.</p>
         *
         * @param tableName the name of the table to count
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.count("users").sql();
         * // Output: SELECT COUNT(*) FROM users
         * 
         * String sql2 = MSC.count("users")
         *                  .where(CF.eq("active", true))
         *                  .sql();
         * // Output: SELECT COUNT(*) FROM users WHERE active = #{active}
         * }</pre>
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a SELECT COUNT(*) statement for an entity class.
         * 
         * <p>The table name is determined from the {@code @Table} annotation.</p>
         *
         * @param entityClass the entity class to count
         * @return the SQLBuilder instance for method chaining
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MSC.count(User.class)
         *                 .where(CF.gt("age", 18))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM users WHERE age > #{age}
         * }</pre>
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Generates SQL for a condition only, without a complete statement.
         * 
         * <p>This method is useful for generating WHERE clause fragments or
         * testing condition SQL generation.</p>
         *
         * @param cond the condition to generate SQL for
         * @param entityClass the entity class for property mapping
         * @return the SQLBuilder instance containing only the condition SQL
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("firstName", "John"),
         *     CF.gt("age", 18)
         * );
         * String sql = MSC.parse(cond, User.class).sql();
         * // Output: first_name = #{firstName} AND age > #{age}
         * }</pre>
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * MyBatis-style SQL builder with all capital case (upper case with underscore) field/column naming strategy.
     * 
     * <p>This builder generates SQL with MyBatis-style parameter placeholders (#{paramName}) and converts
     * property names to UPPER_CASE_WITH_UNDERSCORE format. For example, a property named "firstName" 
     * will be converted to "FIRST_NAME" in the SQL.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Generate SELECT with column aliasing
     * String sql = MAC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = #{id}
     * 
     * // Generate INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = MAC.insert(account).into("ACCOUNT").sql();
     * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (#{firstName}, #{lastName})
     * }</pre>
     * 
     * @deprecated Use {@link NAC} (Named SQL with All Caps) instead for better clarity
     */
    @Deprecated
    public static class MAC extends SQLBuilder {

        /**
         * Creates a new instance of MAC SQL builder.
         * Internal constructor - use static factory methods instead.
         */
        MAC() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Creates a new MAC instance.
         * Internal factory method.
         * 
         * @return a new MAC SQL builder instance
         */
        protected static MAC createInstance() {
            return new MAC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MAC.insert("firstName").into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME) VALUES (#{firstName})
         * }</pre>
         *
         * @param expr the column expression or property name to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified columns.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MAC.insert("firstName", "lastName", "email")
         *                 .into("ACCOUNT")
         *                 .sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified collection of columns.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = MAC.insert(columns).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder using a map of property names to values.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * String sql = MAC.insert(props).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param props map of property names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the given entity object.
         * All non-null properties of the entity will be included in the INSERT statement.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * String sql = MAC.insert(account).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder for the given entity object, excluding specified properties.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Account account = new Account();
         * account.setId(1L);
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * Set<String> excludes = new HashSet<>(Arrays.asList("id"));
         * String sql = MAC.insert(account, excludes).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class.
         * All properties marked as insertable will be included.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MAC.insert(Account.class).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class, excluding specified properties.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "createTime"));
         * String sql = MAC.insert(Account.class, excludes).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with automatic table name detection.
         * The table name is derived from the entity class name or @Table annotation.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MAC.insertInto(Account.class).sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with automatic table name detection,
         * excluding specified properties.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = MAC.insertInto(Account.class, excludes).sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple entities or property maps.
         * Generates MySQL-style batch insert syntax with camelCase column names.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "Doe"),
         *     new Account("Jane", "Smith")
         * );
         * String sql = MLC.batchInsert(accounts).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName) VALUES (#{firstName_0}, #{lastName_0}), (#{firstName_1}, #{lastName_1})
         * }</pre>
         *
         * @param propsList collection of entities or property maps to batch insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @deprecated This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.update("account")
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .set("modifiedDate", new Date())
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName}, lastName = #{lastName}, modifiedDate = #{modifiedDate} WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class mapping.
         * The entity class is used for property name validation and type checking.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.update("account", Account.class)
         *                 .set("isActive", false)
         *                 .set("deactivatedDate", new Date())
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET isActive = #{isActive}, deactivatedDate = #{deactivatedDate} WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class.
         * The table name is derived from the entity class name or @Table annotation.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.update(Account.class)
         *                 .set("status", "ACTIVE")
         *                 .set("lastLoginDate", new Date())
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET status = #{status}, lastLoginDate = #{lastLoginDate} WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class, excluding specified properties.
         * Properties marked with @NonUpdatable or in the excluded set will not be updated.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "createdDate", "createdBy"));
         * String sql = MLC.update(Account.class, excludes)
         *                 .set("firstName", "John")
         *                 .set("modifiedDate", new Date())
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName}, modifiedDate = #{modifiedDate} WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.deleteFrom("account")
         *                 .where(CF.eq("status", "INACTIVE"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE status = #{status}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table with entity class mapping.
         * The entity class is used for property name to column name mapping in WHERE conditions.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.deleteFrom("account", Account.class)
         *                 .where(CF.and(
         *                     CF.eq("isActive", false),
         *                     CF.lt("lastLoginDate", lastYear)
         *                 ))
         *                 .sql();
         * // Output: DELETE FROM account WHERE isActive = #{isActive} AND lastLoginDate < #{lastLoginDate}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified entity class.
         * The table name is derived from the entity class name or @Table annotation.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.deleteFrom(Account.class)
         *                 .where(CF.in("id", Arrays.asList(1, 2, 3)))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id IN (#{id_0}, #{id_1}, #{id_2})
         * }</pre>
         *
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single column or expression.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.select("COUNT(*)").from("account").sql();
         * // Output: SELECT COUNT(*) FROM account
         * 
         * String sql2 = MLC.select("firstName").from("account").sql();
         * // Output: SELECT firstName FROM account
         * }</pre>
         *
         * @param selectPart the column name or SQL expression to select
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder with multiple columns.
         * Column names remain in camelCase format.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.select("firstName", "lastName", "emailAddress")
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT firstName, lastName, emailAddress FROM account
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a collection of columns.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
         * String sql = MLC.select(columns).from("account").sql();
         * // Output: SELECT firstName, lastName, phoneNumber FROM account
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * aliases.put("emailAddress", "email");
         * String sql = MLC.select(aliases).from("account").sql();
         * // Output: SELECT firstName AS fname, lastName AS lname, emailAddress AS email FROM account
         * }</pre>
         *
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of the specified entity class.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.select(Account.class).from("account").sql();
         * // Output: SELECT id, firstName, lastName, emailAddress, createdDate FROM account
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class with optional sub-entity properties.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * // Without sub-entities
         * String sql = MLC.select(Order.class, false).from("orders").sql();
         * // Output: SELECT id, customerId, orderDate, totalAmount FROM orders
         * 
         * // With sub-entities (includes properties from related entities)
         * String sql = MLC.select(Order.class, true).from("orders").sql();
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class, excluding specified properties.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("passwordHash", "securityToken"));
         * String sql = MLC.select(Account.class, excludes).from("account").sql();
         * // Output: SELECT id, firstName, lastName, emailAddress FROM account
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class with full control over included properties.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes", "debugInfo"));
         * String sql = MLC.select(Customer.class, true, excludes)
         *                 .from("customer")
         *                 .sql();
         * // Selects all Customer properties and sub-entity properties, except excluded ones
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM SQL builder for the specified entity class.
         * Automatically determines the table name from the entity class.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class).where(CF.eq("isActive", true)).sql();
         * // Output: SELECT id, firstName, lastName, emailAddress FROM account WHERE isActive = #{isActive}
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with a table alias.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class, "a")
         *                 .where(CF.like("a.emailAddress", "%@example.com"))
         *                 .sql();
         * // Output: SELECT a.id, a.firstName, a.lastName, a.emailAddress FROM account a WHERE a.emailAddress LIKE #{emailAddress}
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with optional sub-entity properties.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Order.class, true)
         *                 .where(CF.between("orderDate", startDate, endDate))
         *                 .sql();
         * // Includes Order properties and related sub-entity properties
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with table alias and sub-entity control.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Product.class, "p", true)
         *                 .innerJoin("category", "c").on("p.categoryId = c.id")
         *                 .where(CF.eq("c.isActive", true))
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with property exclusions.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("largeJsonData", "binaryContent"));
         * String sql = MLC.selectFrom(Document.class, excludes)
         *                 .where(CF.eq("documentType", "PDF"))
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with table alias and property exclusions.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("encryptedData"));
         * String sql = MLC.selectFrom(User.class, "u", excludes)
         *                 .leftJoin("user_roles", "ur").on("u.id = ur.userId")
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with sub-entity control and property exclusions.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("auditLog"));
         * String sql = MLC.selectFrom(Invoice.class, true, excludes)
         *                 .where(CF.eq("isPaid", false))
         *                 .orderBy("dueDate")
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with full control over all options.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("tempData"));
         * String sql = MLC.selectFrom(Transaction.class, "t", true, excludes)
         *                 .innerJoin("account", "a").on("t.accountId = a.id")
         *                 .where(CF.and(
         *                     CF.eq("a.isActive", true),
         *                     CF.gt("t.amount", 1000)
         *                 ))
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.select(Order.class, "o", "order", 
         *                        Customer.class, "c", "customer")
         *                 .from("orders o")
         *                 .innerJoin("customers c").on("o.customerId = c.id")
         *                 .sql();
         * // Selects columns from both entities with proper aliasing
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for the first entity's columns
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for the second entity's columns
         * @return a new SQLBuilder instance configured for multi-entity SELECT
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes with property exclusions.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> orderExcludes = new HashSet<>(Arrays.asList("internalNotes"));
         * Set<String> customerExcludes = new HashSet<>(Arrays.asList("creditCardInfo"));
         * String sql = MLC.select(Order.class, "o", "order", orderExcludes,
         *                        Customer.class, "c", "customer", customerExcludes)
         *                 .from("orders o")
         *                 .innerJoin("customers c").on("o.customerId = c.id")
         *                 .where(CF.gt("o.totalAmount", 500))
         *                 .sql();
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for the first entity's columns
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for the second entity's columns
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections with custom configurations.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Order.class, "o", "order", null, true, null),
         *     new Selection(Customer.class, "c", "customer", null, false, excludedProps),
         *     new Selection(Product.class, "p", "product", null, false, null)
         * );
         * String sql = MLC.select(selections)
         *                 .from("orders o")
         *                 .innerJoin("customers c").on("o.customerId = c.id")
         *                 .innerJoin("order_items oi").on("o.id = oi.orderId")
         *                 .innerJoin("products p").on("oi.productId = p.id")
         *                 .sql();
         * }</pre>
         *
         * @param multiSelects list of Selection configurations for multiple entities
         * @return a new SQLBuilder instance configured for multi-entity SELECT
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM SQL builder for joining two entity classes.
         * Automatically generates the FROM clause with proper table names.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Order.class, "o", "order",
         *                            Customer.class, "c", "customer")
         *                 .on("o.customerId = c.id")
         *                 .where(CF.eq("c.country", "USA"))
         *                 .sql();
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for the first entity's columns
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for the second entity's columns
         * @return a new SQLBuilder instance configured for multi-entity SELECT FROM
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a complete SELECT FROM SQL builder for joining two entity classes with property exclusions.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> orderExcludes = new HashSet<>(Arrays.asList("tempData"));
         * Set<String> productExcludes = new HashSet<>(Arrays.asList("warehouseNotes"));
         * String sql = MLC.selectFrom(Order.class, "o", "order", orderExcludes,
         *                            Product.class, "p", "product", productExcludes)
         *                 .innerJoin("order_items", "oi").on("o.id = oi.orderId")
         *                 .on("oi.productId = p.id")
         *                 .sql();
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for the first entity's columns
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for the second entity's columns
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT FROM
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a complete SELECT FROM SQL builder for multiple entity selections.
         * Automatically generates the FROM clause based on the Selection configurations.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Department.class, "d", null, null, false, null),
         *     new Selection(Employee.class, "e", null, null, true, excludedProps),
         *     new Selection(Project.class, "p", null, null, false, null)
         * );
         * String sql = MLC.selectFrom(selections)
         *                 .on("d.id = e.departmentId")
         *                 .leftJoin("employee_projects", "ep").on("e.id = ep.employeeId")
         *                 .on("ep.projectId = p.id")
         *                 .sql();
         * }</pre>
         *
         * @param multiSelects list of Selection configurations for multiple entities
         * @return a new SQLBuilder instance configured for multi-entity SELECT FROM
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.count("account").where(CF.eq("isActive", true)).sql();
         * // Output: SELECT COUNT(*) FROM account WHERE isActive = #{isActive}
         * }</pre>
         *
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified entity class.
         * The table name is derived from the entity class.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.count(Account.class)
         *                 .where(CF.or(
         *                     CF.isNull("emailAddress"),
         *                     CF.eq("emailVerified", false)
         *                 ))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE emailAddress IS NULL OR emailVerified = #{emailVerified}
         * }</pre>
         *
         * @param entityClass the entity class to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL format using the specified entity class for property mapping.
         * This is useful for generating WHERE clause fragments with camelCase naming.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("isActive", true),
         *     CF.like("emailAddress", "%@company.com"),
         *     CF.gt("accountBalance", 0)
         * );
         * String sql = MLC.parse(cond, Account.class).sql();
         * // Output: isActive = #{isActive} AND emailAddress LIKE #{emailAddress} AND accountBalance > #{accountBalance}
         * }</pre>
         *
         * @param cond the condition to parse
         * @param entityClass the entity class for property to column mapping
         * @return a new SQLBuilder instance containing the parsed condition
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * MyBatis-style SQL builder with lower camel case field/column naming strategy.
     * 
     * <p>This builder generates SQL with MyBatis-style parameter placeholders (#{paramName}) and uses
     * lowerCamelCase naming convention. Property names are used as-is without transformation to 
     * snake_case or upper case.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Property names remain in camelCase
     * String sql = MLC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(CF.eq("id", 1))
     *                 .sql();
     * // Output: SELECT firstName, lastName FROM account WHERE id = #{id}
     * 
     * // INSERT with camelCase columns
     * Account account = new Account();
     * account.setFirstName("John");
     * String sql = MLC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (firstName, lastName) VALUES (#{firstName}, #{lastName})
     * }</pre>
     * 
     * @deprecated Use {@link NLC} (Named SQL with Lower Camel) instead for better clarity
     */
    @Deprecated
    public static class MLC extends SQLBuilder {

        /**
         * Creates a new instance of MLC SQL builder.
         * Internal constructor - use static factory methods instead.
         */
        MLC() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Creates a new MLC instance.
         * Internal factory method.
         * 
         * @return a new MLC SQL builder instance
         */
        protected static MLC createInstance() {
            return new MLC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * The column name remains in camelCase format and uses MyBatis-style parameter placeholder.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.insert("firstName").into("account").sql();
         * // Output: INSERT INTO account (firstName) VALUES (#{firstName})
         * }</pre>
         *
         * @param expr the column expression or property name to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified columns.
         * Column names remain in camelCase format and use MyBatis-style parameter placeholders.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.insert("firstName", "lastName", "emailAddress")
         *                 .into("account")
         *                 .sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress) VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified collection of columns.
         * Column names remain in camelCase format and use MyBatis-style parameter placeholders.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
         * String sql = MLC.insert(columns).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, phoneNumber) VALUES (#{firstName}, #{lastName}, #{phoneNumber})
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder using a map of property names to values.
         * The map keys are used as column names in camelCase format with MyBatis-style placeholders.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * props.put("isActive", true);
         * String sql = MLC.insert(props).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, isActive) VALUES (#{firstName}, #{lastName}, #{isActive})
         * }</pre>
         *
         * @param props map of property names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the given entity object.
         * All non-null properties of the entity will be included in the INSERT statement
         * with camelCase column names and MyBatis-style placeholders.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setCreatedDate(new Date());
         * String sql = MLC.insert(account).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, createdDate) VALUES (#{firstName}, #{lastName}, #{createdDate})
         * }</pre>
         *
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder for the given entity object, excluding specified properties.
         * Properties not in the exclude set will be included with camelCase names and MyBatis-style placeholders.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Account account = new Account();
         * account.setId(1L);  // Will be excluded
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = MLC.insert(account, excludes).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class.
         * All properties marked as insertable will be included with camelCase names and MyBatis-style placeholders.
         * Properties annotated with @ReadOnly, @ReadOnlyId, or @Transient are automatically excluded.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.insert(Account.class).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress, createdDate) VALUES (#{firstName}, #{lastName}, #{emailAddress}, #{createdDate})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class, excluding specified properties.
         * Properties not in the exclude set and not annotated as read-only will be included
         * with camelCase names and MyBatis-style placeholders.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "auditFields"));
         * String sql = MLC.insert(Account.class, excludes).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress) VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with automatic table name detection.
         * The table name is derived from the entity class name or @Table annotation.
         * All insertable properties are included with camelCase names and MyBatis-style placeholders.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.insertInto(Account.class).sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress) VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with automatic table name detection,
         * excluding specified properties. The table name is derived from the entity class name or @Table annotation.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = MLC.insertInto(Account.class, excludes).sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress) VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generates MySQL-style batch insert SQL for multiple entities or property maps.
         * This method creates a single INSERT statement with multiple value rows for efficient batch insertion.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "Doe"),
         *     new Account("Jane", "Smith")
         * );
         * String sql = MLC.batchInsert(accounts).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName) VALUES (#{firstName_0}, #{lastName_0}), (#{firstName_1}, #{lastName_1})
         * }</pre>
         *
         * @param propsList list of entities or properties maps to batch insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * @beta This API is in beta and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * Use the {@code set()} method to specify which columns to update.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.update("account")
         *                 .set("firstName", "updatedName")
         *                 .set("modifiedDate", new Date())
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName}, modifiedDate = #{modifiedDate} WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class context.
         * The entity class provides metadata for property-to-column mapping.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.update("account", Account.class)
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName}, lastName = #{lastName} WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for column mapping metadata
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * All updatable properties (excluding @ReadOnly, @NonUpdatable) are included.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.update(Account.class)
         *                 .set("firstName", "John")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName} WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to generate UPDATE for
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class, excluding specified properties.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * Properties in the exclude set or annotated as non-updatable are not included.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("createdDate", "createdBy"));
         * String sql = MLC.update(Account.class, excludes)
         *                 .set("firstName", "John")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName} WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to generate UPDATE for
         * @param excludedPropNames set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.deleteFrom("account")
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table with entity class context.
         * The entity class provides metadata for property-to-column mapping in WHERE conditions.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.deleteFrom("account", Account.class)
         *                 .where(CF.eq("emailAddress", "john@example.com"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE emailAddress = #{emailAddress}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for column mapping metadata
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified entity class.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.deleteFrom(Account.class)
         *                 .where(CF.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to generate DELETE for
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a custom select expression.
         * This method allows for complex select expressions including functions and calculations.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.select("COUNT(*) AS total, MAX(createdDate) AS latest")
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT COUNT(*) AS total, MAX(createdDate) AS latest FROM account
         * }</pre>
         *
         * @param selectPart the custom select expression
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified columns.
         * Column names remain in camelCase format.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.select("firstName", "lastName", "emailAddress")
         *                 .from("account")
         *                 .where(CF.gt("createdDate", someDate))
         *                 .sql();
         * // Output: SELECT firstName, lastName, emailAddress FROM account WHERE createdDate > #{createdDate}
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified collection of columns.
         * This is useful when column names are dynamically determined.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<String> columns = getColumnsToSelect();
         * String sql = MLC.select(columns)
         *                 .from("account")
         *                 .orderBy("createdDate DESC")
         *                 .sql();
         * // Output: SELECT firstName, lastName, emailAddress FROM account ORDER BY createdDate DESC
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * The map keys are column names and values are their aliases.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = MLC.select(aliases)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT firstName AS fname, lastName AS lname FROM account
         * }</pre>
         *
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of the specified entity class.
         * Properties annotated with @Transient are automatically excluded.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.select(Account.class)
         *                 .from("account")
         *                 .where(CF.eq("isActive", true))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, emailAddress, isActive FROM account WHERE isActive = #{isActive}
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT for
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class with optional sub-entity inclusion.
         * When includeSubEntityProperties is true, properties of embedded entities are also selected.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.select(Account.class, true)
         *                 .from("account")
         *                 .innerJoin("address").on("account.addressId = address.id")
         *                 .sql();
         * // Output: SELECT account.*, address.* FROM account INNER JOIN address ON account.addressId = address.id
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class, excluding specified properties.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("password", "secretKey"));
         * String sql = MLC.select(Account.class, excludes)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, emailAddress FROM account
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT for
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class with sub-entity inclusion control
         * and property exclusion.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = MLC.select(Account.class, true, excludes)
         *                 .from("account")
         *                 .innerJoin("profile").on("account.profileId = profile.id")
         *                 .sql();
         * // Output: SELECT account columns except internalNotes, profile.* FROM account INNER JOIN profile ON account.profileId = profile.id
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for the specified entity class.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class)
         *                 .where(CF.eq("isActive", true))
         *                 .orderBy("createdDate DESC")
         *                 .sql();
         * // Output: SELECT * FROM account WHERE isActive = #{isActive} ORDER BY createdDate DESC
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for the specified entity class with table alias.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class, "a")
         *                 .innerJoin("profile p").on("a.profileId = p.id")
         *                 .where(CF.eq("a.isActive", true))
         *                 .sql();
         * // Output: SELECT a.* FROM account a INNER JOIN profile p ON a.profileId = p.id WHERE a.isActive = #{isActive}
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param alias the table alias to use
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with sub-entity inclusion control.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class, true)
         *                 .innerJoin("address").on("account.addressId = address.id")
         *                 .sql();
         * // Output: SELECT account.*, address.* FROM account INNER JOIN address ON account.addressId = address.id
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with table alias and sub-entity inclusion control.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class, "acc", true)
         *                 .innerJoin("profile p").on("acc.profileId = p.id")
         *                 .sql();
         * // Output: SELECT acc.*, p.* FROM account acc INNER JOIN profile p ON acc.profileId = p.id
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with property exclusion.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
         * String sql = MLC.selectFrom(Account.class, excludes)
         *                 .where(CF.eq("status", "ACTIVE"))
         *                 .sql();
         * // Output: SELECT columns except largeBlob, internalData FROM account WHERE status = #{status}
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with table alias and property exclusion.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("password"));
         * String sql = MLC.selectFrom(Account.class, "a", excludes)
         *                 .where(CF.like("a.emailAddress", "%@example.com"))
         *                 .sql();
         * // Output: SELECT a.columns except password FROM account a WHERE a.emailAddress LIKE #{emailAddress}
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with sub-entity inclusion control and property exclusion.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
         * String sql = MLC.selectFrom(Account.class, true, excludes)
         *                 .innerJoin("orders").on("account.id = orders.accountId")
         *                 .sql();
         * // Output: SELECT account columns except temporaryData, orders.* FROM account INNER JOIN orders ON account.id = orders.accountId
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with full control over alias, sub-entity inclusion, and property exclusion.
         * This is the most comprehensive selectFrom method providing complete customization.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("debugInfo"));
         * String sql = MLC.selectFrom(Account.class, "acc", true, excludes)
         *                 .innerJoin("orders o").on("acc.id = o.accountId")
         *                 .innerJoin("items i").on("o.id = i.orderId")
         *                 .where(CF.gt("o.total", 100))
         *                 .sql();
         * // Output: Complex SELECT with multiple joins
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity classes with table and result aliases.
         * This method is useful for complex queries involving multiple tables with custom result mapping.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.select(Account.class, "a", "account", Order.class, "o", "order")
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .sql();
         * // Output: SELECT a.id AS "account.id", a.name AS "account.name", o.id AS "order.id", o.total AS "order.total" FROM account a INNER JOIN orders o ON a.id = o.accountId
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation
         * @throws IllegalArgumentException if any entity class is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity classes with table aliases, result aliases, and property exclusion.
         * This provides full control over multi-entity queries with custom column selection.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> accountExcludes = new HashSet<>(Arrays.asList("password"));
         * Set<String> orderExcludes = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = MLC.select(Account.class, "a", "account", accountExcludes,
         *                        Order.class, "o", "order", orderExcludes)
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .sql();
         * // Output: SELECT with excluded columns from both tables
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation
         * @throws IllegalArgumentException if any entity class is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * This is the most flexible method for complex multi-table queries with different configurations for each table.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, true, null),
         *     new Selection(Order.class, "o", "order", Arrays.asList("id", "total"), false, null),
         *     new Selection(Product.class, "p", "product", null, false, new HashSet<>(Arrays.asList("description")))
         * );
         * String sql = MLC.select(selections).from("account a")
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .innerJoin("order_items oi").on("o.id = oi.orderId")
         *                 .innerJoin("products p").on("oi.productId = p.id")
         *                 .sql();
         * // Output: Complex SELECT with multiple tables and custom column selection
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for multiple entity classes.
         * Automatically generates the FROM clause with proper table names.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class, "a", "account", Order.class, "o", "order")
         *                 .innerJoin("a.id = o.accountId")
         *                 .where(CF.gt("o.createdDate", someDate))
         *                 .sql();
         * // Output: SELECT with automatic FROM clause generation
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation with FROM clause
         * @throws IllegalArgumentException if any entity class is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for multiple entity classes with property exclusion.
         * Automatically generates the FROM clause and excludes specified properties.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Set<String> accountExcludes = new HashSet<>(Arrays.asList("sensitiveData"));
         * String sql = MLC.selectFrom(Account.class, "a", "account", accountExcludes,
         *                            Order.class, "o", "order", null)
         *                 .innerJoin("a.id = o.accountId")
         *                 .sql();
         * // Output: SELECT with automatic FROM clause and excluded columns
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation with FROM clause
         * @throws IllegalArgumentException if any entity class is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for multiple entity selections.
         * Automatically generates the FROM clause based on the Selection configurations.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * List<Selection> selections = createComplexSelections();
         * String sql = MLC.selectFrom(selections)
         *                 .where(complexConditions)
         *                 .groupBy("account.type")
         *                 .having(CF.gt("COUNT(*)", 5))
         *                 .sql();
         * // Output: Complex SELECT with automatic FROM clause generation
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation with FROM clause
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.count("account")
         *                 .where(CF.eq("isActive", true))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE isActive = #{isActive}
         * }</pre>
         *
         * @param tableName the name of the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified entity class.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * String sql = MLC.count(Account.class)
         *                 .where(CF.between("createdDate", startDate, endDate))
         *                 .sql();
         * // Output: SELECT COUNT(*) FROM account WHERE createdDate BETWEEN #{createdDate_1} AND #{createdDate_2}
         * }</pre>
         *
         * @param entityClass the entity class to count rows for
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL format for the specified entity class.
         * This method is useful for generating SQL fragments from condition objects.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Condition cond = CF.and(
         *     CF.eq("status", "ACTIVE"),
         *     CF.gt("balance", 1000)
         * );
         * String sql = MLC.parse(cond, Account.class).sql();
         * // Output: status = #{status} AND balance > #{balance}
         * }</pre>
         *
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for column mapping metadata
         * @return a new SQLBuilder instance containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Represents a SQL string and its associated parameters.
     * This class is used to encapsulate the generated SQL and the parameters required for execution.
     * It is immutable, meaning once created, the SQL and parameters cannot be changed.
     */
    public static final class SP {
        /**
         * The generated SQL string with parameter placeholders.
         * For parameterized SQL, placeholders are '?'.
         * For named SQL, placeholders are ':paramName' or '#{paramName}'.
         */
        public final String sql;

        /**
         * The list of parameter values in the order they appear in the SQL.
         * This list is immutable and cannot be modified after creation.
         */
        public final List<Object> parameters;

        /**
         * Creates a new SQL-Parameters pair.
         * Internal constructor - instances are created by SQLBuilder.
         * 
         * @param sql the SQL string
         * @param parameters the parameter values
         */
        SP(final String sql, final List<Object> parameters) {
            this.sql = sql;
            this.parameters = ImmutableList.wrap(parameters);
        }

        /**
         * Returns a hash code value for this SP object.
         * The hash code is computed based on both the SQL string and parameters.
         *
         * @return a hash code value for this object
         */
        @Override
        public int hashCode() {
            return N.hashCode(sql) * 31 + N.hashCode(parameters);
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         * Two SP objects are equal if they have the same SQL string and parameters.
         *
         * @param obj the reference object with which to compare
         * @return true if this object is the same as the obj argument; false otherwise
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof final SP other) {
                return N.equals(other.sql, sql) && N.equals(other.parameters, parameters);
            }

            return false;
        }

        /**
         * Returns a string representation of this SP object.
         * The string contains both the SQL and parameters for debugging purposes.
         *
         * @return a string representation of the object
         */
        @Override
        public String toString() {
            return "{sql=" + sql + ", parameters=" + N.toString(parameters) + "}";
        }
    }

    protected static final BiConsumer<StringBuilder, String> defaultHandlerForNamedParameter = (sb, propName) -> sb.append(":").append(propName);
    // private static final BiConsumer<StringBuilder, String> mybatisHandlerForNamedParameter = (sb, propName) -> sb.append("#{").append(propName).append("}");

    protected static final ThreadLocal<BiConsumer<StringBuilder, String>> handlerForNamedParameter_TL = ThreadLocal //NOSONAR
            .withInitial(() -> defaultHandlerForNamedParameter);

    /**
     * Sets a custom handler for formatting named parameters in SQL strings.
     * The default handler formats parameters as ":paramName".
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // Set MyBatis-style parameter handler
     * SQLBuilder.setHandlerForNamedParameter((sb, propName) -> 
     *     sb.append("#{").append(propName).append("}"));
     * 
     * // Now named SQL will use #{paramName} format
     * String sql = NSC.select("name").from("users").where(CF.eq("id", 1)).sql();
     * // Output: SELECT name FROM users WHERE id = #{id}
     * }</pre>
     *
     * @param handlerForNamedParameter the handler to format named parameters
     * @throws IllegalArgumentException if handlerForNamedParameter is null
     */
    public static void setHandlerForNamedParameter(final BiConsumer<StringBuilder, String> handlerForNamedParameter) {
        N.checkArgNotNull(handlerForNamedParameter, "handlerForNamedParameter");
        handlerForNamedParameter_TL.set(handlerForNamedParameter);
    }

    /**
     * Resets the named parameter handler to the default format.
     * The default handler formats parameters as ":paramName".
     * 
     * <p>Example:</p>
     * <pre>{@code
     * // After using a custom handler, reset to default
     * SQLBuilder.resetHandlerForNamedParameter();
     * 
     * // Named SQL will now use :paramName format again
     * String sql = NSC.select("name").from("users").where(CF.eq("id", 1)).sql();
     * // Output: SELECT name FROM users WHERE id = :id
     * }</pre>
     */
    public static void resetHandlerForNamedParameter() {
        handlerForNamedParameter_TL.set(defaultHandlerForNamedParameter);
    }

    protected static String getFromClause(final List<Selection> multiSelects, final NamingPolicy namingPolicy) {
        final StringBuilder sb = Objectory.createStringBuilder();
        int idx = 0;

        for (final Selection selection : multiSelects) {
            if (idx++ > 0) {
                sb.append(_COMMA_SPACE);
            }

            sb.append(getTableName(selection.entityClass(), namingPolicy));

            if (Strings.isNotEmpty(selection.tableAlias())) {
                sb.append(' ').append(selection.tableAlias());
            }

            if (N.notEmpty(selection.selectPropNames()) || selection.includeSubEntityProperties) {
                final Class<?> entityClass = selection.entityClass();
                final Collection<String> selectPropNames = selection.selectPropNames();
                final Set<String> excludedPropNames = selection.excludedPropNames();
                final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

                if (N.isEmpty(subEntityPropNames)) {
                    continue;
                }

                final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
                PropInfo propInfo = null;
                Class<?> subEntityClass = null;

                for (final String subEntityPropName : subEntityPropNames) {
                    if (N.notEmpty(selectPropNames)) {
                        if (!selectPropNames.contains(subEntityPropName)) {
                            continue;
                        }
                    } else if (excludedPropNames != null && excludedPropNames.contains(subEntityPropName)) {
                        continue;
                    }

                    propInfo = entityInfo.getPropInfo(subEntityPropName);
                    subEntityClass = (propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type).clazz();

                    sb.append(_COMMA_SPACE).append(getTableName(subEntityClass, namingPolicy));
                }
            }
        }

        final String fromClause = sb.toString();

        Objectory.recycle(sb);
        return fromClause;
    }
}
