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

import static com.landawn.abacus.util.WD._PARENTHESES_L;
import static com.landawn.abacus.util.WD._PARENTHESES_R;
import static com.landawn.abacus.util.WD._SPACE;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    private static final Logger logger = LoggerFactory.getLogger(SQLBuilder.class);

    /** Constant for selecting all columns in SQL queries. */
    public static final String ALL = WD.ALL;

    /** Constant for TOP clause in SQL queries. */
    public static final String TOP = WD.TOP;

    /** Constant for UNIQUE clause in SQL queries. */
    public static final String UNIQUE = WD.UNIQUE;

    /** Constant for DISTINCT clause in SQL queries. */
    public static final String DISTINCT = WD.DISTINCT;

    /** Constant for DISTINCTROW clause in SQL queries. */
    public static final String DISTINCTROW = WD.DISTINCTROW;

    /** Constant for asterisk (*) wildcard in SQL queries. */
    public static final String ASTERISK = WD.ASTERISK;

    /** Constant for COUNT(*) aggregate function. */
    public static final String COUNT_ALL = "count(*)";

    static final List<String> COUNT_ALL_LIST = ImmutableList.of(COUNT_ALL);

    //    public static final String _1 = "1";
    //
    //    public static final List<String> _1_list = ImmutableList.of(_1);

    static final char[] _INSERT = WD.INSERT.toCharArray();

    static final char[] _SPACE_INSERT_SPACE = (WD.SPACE + WD.INSERT + WD.SPACE).toCharArray();

    static final char[] _INTO = WD.INTO.toCharArray();

    static final char[] _SPACE_INTO_SPACE = (WD.SPACE + WD.INTO + WD.SPACE).toCharArray();

    static final char[] _VALUES = WD.VALUES.toCharArray();

    static final char[] _SPACE_VALUES_SPACE = (WD.SPACE + WD.VALUES + WD.SPACE).toCharArray();

    static final char[] _SELECT = WD.SELECT.toCharArray();

    static final char[] _SPACE_SELECT_SPACE = (WD.SPACE + WD.SELECT + WD.SPACE).toCharArray();

    static final char[] _FROM = WD.FROM.toCharArray();

    static final char[] _SPACE_FROM_SPACE = (WD.SPACE + WD.FROM + WD.SPACE).toCharArray();

    static final char[] _UPDATE = WD.UPDATE.toCharArray();

    static final char[] _SPACE_UPDATE_SPACE = (WD.SPACE + WD.UPDATE + WD.SPACE).toCharArray();

    static final char[] _SET = WD.SET.toCharArray();

    static final char[] _SPACE_SET_SPACE = (WD.SPACE + WD.SET + WD.SPACE).toCharArray();

    static final char[] _DELETE = WD.DELETE.toCharArray();

    static final char[] _SPACE_DELETE_SPACE = (WD.SPACE + WD.DELETE + WD.SPACE).toCharArray();

    static final char[] _JOIN = WD.JOIN.toCharArray();

    static final char[] _SPACE_JOIN_SPACE = (WD.SPACE + WD.JOIN + WD.SPACE).toCharArray();

    static final char[] _LEFT_JOIN = WD.LEFT_JOIN.toCharArray();

    static final char[] _SPACE_LEFT_JOIN_SPACE = (WD.SPACE + WD.LEFT_JOIN + WD.SPACE).toCharArray();

    static final char[] _RIGHT_JOIN = WD.RIGHT_JOIN.toCharArray();

    static final char[] _SPACE_RIGHT_JOIN_SPACE = (WD.SPACE + WD.RIGHT_JOIN + WD.SPACE).toCharArray();

    static final char[] _FULL_JOIN = WD.FULL_JOIN.toCharArray();

    static final char[] _SPACE_FULL_JOIN_SPACE = (WD.SPACE + WD.FULL_JOIN + WD.SPACE).toCharArray();

    static final char[] _CROSS_JOIN = WD.CROSS_JOIN.toCharArray();

    static final char[] _SPACE_CROSS_JOIN_SPACE = (WD.SPACE + WD.CROSS_JOIN + WD.SPACE).toCharArray();

    static final char[] _INNER_JOIN = WD.INNER_JOIN.toCharArray();

    static final char[] _SPACE_INNER_JOIN_SPACE = (WD.SPACE + WD.INNER_JOIN + WD.SPACE).toCharArray();

    static final char[] _NATURAL_JOIN = WD.NATURAL_JOIN.toCharArray();

    static final char[] _SPACE_NATURAL_JOIN_SPACE = (WD.SPACE + WD.NATURAL_JOIN + WD.SPACE).toCharArray();

    static final char[] _ON = WD.ON.toCharArray();

    static final char[] _SPACE_ON_SPACE = (WD.SPACE + WD.ON + WD.SPACE).toCharArray();

    static final char[] _USING = WD.USING.toCharArray();

    static final char[] _SPACE_USING_SPACE = (WD.SPACE + WD.USING + WD.SPACE).toCharArray();

    static final char[] _WHERE = WD.WHERE.toCharArray();

    static final char[] _SPACE_WHERE_SPACE = (WD.SPACE + WD.WHERE + WD.SPACE).toCharArray();

    static final char[] _GROUP_BY = WD.GROUP_BY.toCharArray();

    static final char[] _SPACE_GROUP_BY_SPACE = (WD.SPACE + WD.GROUP_BY + WD.SPACE).toCharArray();

    static final char[] _HAVING = WD.HAVING.toCharArray();

    static final char[] _SPACE_HAVING_SPACE = (WD.SPACE + WD.HAVING + WD.SPACE).toCharArray();

    static final char[] _ORDER_BY = WD.ORDER_BY.toCharArray();

    static final char[] _SPACE_ORDER_BY_SPACE = (WD.SPACE + WD.ORDER_BY + WD.SPACE).toCharArray();

    static final char[] _LIMIT = (WD.SPACE + WD.LIMIT + WD.SPACE).toCharArray();

    static final char[] _SPACE_LIMIT_SPACE = (WD.SPACE + WD.LIMIT + WD.SPACE).toCharArray();

    static final char[] _OFFSET = WD.OFFSET.toCharArray();

    static final char[] _SPACE_OFFSET_SPACE = (WD.SPACE + WD.OFFSET + WD.SPACE).toCharArray();

    static final char[] _SPACE_ROWS_SPACE = (WD.SPACE + WD.ROWS + WD.SPACE).toCharArray();

    static final char[] _AND = WD.AND.toCharArray();

    static final char[] _SPACE_AND_SPACE = (WD.SPACE + WD.AND + WD.SPACE).toCharArray();

    static final char[] _OR = WD.OR.toCharArray();

    static final char[] _SPACE_OR_SPACE = (WD.SPACE + WD.OR + WD.SPACE).toCharArray();

    static final char[] _UNION = WD.UNION.toCharArray();

    static final char[] _SPACE_UNION_SPACE = (WD.SPACE + WD.UNION + WD.SPACE).toCharArray();

    static final char[] _UNION_ALL = WD.UNION_ALL.toCharArray();

    static final char[] _SPACE_UNION_ALL_SPACE = (WD.SPACE + WD.UNION_ALL + WD.SPACE).toCharArray();

    static final char[] _INTERSECT = WD.INTERSECT.toCharArray();

    static final char[] _SPACE_INTERSECT_SPACE = (WD.SPACE + WD.INTERSECT + WD.SPACE).toCharArray();

    static final char[] _EXCEPT = WD.EXCEPT.toCharArray();

    static final char[] _SPACE_EXCEPT_SPACE = (WD.SPACE + WD.EXCEPT + WD.SPACE).toCharArray();

    static final char[] _EXCEPT2 = WD.EXCEPT2.toCharArray();

    static final char[] _SPACE_EXCEPT2_SPACE = (WD.SPACE + WD.EXCEPT2 + WD.SPACE).toCharArray();

    static final char[] _AS = WD.AS.toCharArray();

    static final char[] _SPACE_AS_SPACE = (WD.SPACE + WD.AS + WD.SPACE).toCharArray();

    static final char[] _SPACE_EQUAL_SPACE = (WD.SPACE + WD.EQUAL + WD.SPACE).toCharArray();

    static final char[] _SPACE_FOR_UPDATE = (WD.SPACE + WD.FOR_UPDATE).toCharArray();

    static final char[] _COMMA_SPACE = WD.COMMA_SPACE.toCharArray();

    static final String SPACE_AS_SPACE = WD.SPACE + WD.AS + WD.SPACE;

    static final String SELECT_PART = "selectPart";
    static final String PROP_OR_COLUMN_NAMES = "propOrColumnNames";
    static final String PROP_OR_COLUMN_NAME_ALIASES = "propOrColumnNameAliases";

    private static final Set<String> sqlKeyWords = N.newHashSet(1024);

    static {
        final Field[] fields = WD.class.getDeclaredFields();
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

    private static final Map<Class<?>, ImmutableSet<String>> subEntityPropNamesPool = new ObjectPool<>(N.POOL_SIZE);

    // private static final Map<Class<?>, ImmutableSet<String>> nonSubEntityPropNamesPool = new ObjectPool<>(N.POOL_SIZE);

    private static final Map<Class<?>, Set<String>[]> defaultPropNamesPool = new ObjectPool<>(N.POOL_SIZE);

    private static final Map<NamingPolicy, Map<Class<?>, String>> fullSelectPartsPool = N.newHashMap(NamingPolicy.values().length);

    static {
        for (final NamingPolicy np : NamingPolicy.values()) {
            fullSelectPartsPool.put(np, new ConcurrentHashMap<>());
        }
    }

    private static final Map<String, char[]> tableDeleteFrom = new ConcurrentHashMap<>();

    private static final Map<Class<?>, String[]> classTableNameMap = new ConcurrentHashMap<>();

    private static final Map<Class<?>, String> classTableAliasMap = new ConcurrentHashMap<>();

    private static final AtomicInteger activeStringBuilderCounter = new AtomicInteger();

    private final NamingPolicy _namingPolicy; //NOSONAR

    private final SQLPolicy _sqlPolicy; //NOSONAR

    private final List<Object> _parameters = new ArrayList<>(); //NOSONAR

    private StringBuilder _sb; //NOSONAR

    private Class<?> _entityClass; //NOSONAR

    private BeanInfo _entityInfo; //NOSONAR

    private ImmutableMap<String, Tuple2<String, Boolean>> _propColumnNameMap; //NOSONAR

    private OperationType _op; //NOSONAR

    private String _tableName; //NOSONAR

    private String _tableAlias; //NOSONAR

    private String _preselect; //NOSONAR

    private Collection<String> _propOrColumnNames; //NOSONAR

    private Map<String, String> _propOrColumnNameAliases; //NOSONAR

    private List<Selection> _multiSelects; //NOSONAR

    private Map<String, Map<String, Tuple2<String, Boolean>>> _aliasPropColumnNameMap; //NOSONAR

    private Map<String, Object> _props; //NOSONAR

    private Collection<Map<String, Object>> _propsList; //NOSONAR

    private boolean _hasFromBeenSet = false; //NOSONAR
    private boolean _isForConditionOnly = false; //NOSONAR

    private final BiConsumer<StringBuilder, String> _handlerForNamedParameter; //NOSONAR

    /**
     * Constructs a new SQLBuilder with the specified naming policy and SQL policy.
     * 
     * @param namingPolicy the naming policy for column names, defaults to LOWER_CASE_WITH_UNDERSCORE if null
     * @param sqlPolicy the SQL generation policy, defaults to SQL if null
     */
    SQLBuilder(final NamingPolicy namingPolicy, final SQLPolicy sqlPolicy) {
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
    static String getTableName(final Class<?> entityClass, final NamingPolicy namingPolicy) {
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
    static String getTableAlias(final Class<?> entityClass) {
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
    static String getTableAlias(final String alias, final Class<?> entityClass) {
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
    static String getTableAliasOrName(final Class<?> entityClass, final NamingPolicy namingPolicy) {
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
    static String getTableAliasOrName(final String alias, final Class<?> entityClass, final NamingPolicy namingPolicy) {
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
    static boolean isDefaultIdPropValue(final Object propValue) {
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
    static Set<String>[] loadPropNamesByClass(final Class<?> entityClass) {
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
                        val[0].add(Strings.concat(subEntityPropName, WD.PERIOD, pn));
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
    static ImmutableSet<String> getSubEntityPropNames(final Class<?> entityClass) {
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

    private static List<String> getSelectTableNames(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames,
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
    static Map<String, Expression> named(final String... propNames) {
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
    static Map<String, Expression> named(final Collection<String> propNames) {
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
        _sb.append(WD._PARENTHESES_L);

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

        _sb.append(WD._PARENTHESES_R);

        _sb.append(_SPACE_VALUES_SPACE);

        _sb.append(WD._PARENTHESES_L);

        if (N.notEmpty(_propOrColumnNames)) {
            switch (_sqlPolicy) {
                case SQL:
                case PARAMETERIZED_SQL: {
                    for (int i = 0, size = _propOrColumnNames.size(); i < size; i++) {
                        if (i > 0) {
                            _sb.append(_COMMA_SPACE);
                        }

                        _sb.append(WD._QUESTION_MARK);
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
                    _sb.append(WD._PARENTHESES_R);
                    _sb.append(_COMMA_SPACE);
                    _sb.append(WD._PARENTHESES_L);
                }

                appendInsertProps(localProps);
            }
        }

        _sb.append(WD._PARENTHESES_R);

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

            final int selectIdx = _sb.indexOf(WD.SELECT);

            if (selectIdx >= 0) {
                final int len = _sb.length();

                _sb.append(_SPACE);

                appendStringExpr(_preselect, false);

                final int newLength = _sb.length();

                _sb.insert(selectIdx + WD.SELECT.length(), _sb.substring(len));
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
        return from(localTableName, Strings.join(tableNames, WD.COMMA_SPACE));
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
        return from(localTableName, Strings.join(tableNames, WD.COMMA_SPACE));
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

        final int idx = expr.indexOf(WD._COMMA);
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

    private SQLBuilder from(final Class<?> entityClass, final Collection<String> tableNames) {
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
    private SQLBuilder from(final String tableName, final String fromCause) {
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
                            sb.append(WD.COMMA_SPACE);
                        }

                        sb.append(formalizeColumnName(_propColumnNameMap, columnName));

                        if ((_namingPolicy != NamingPolicy.LOWER_CAMEL_CASE && _namingPolicy != NamingPolicy.NO_CHANGE) && !WD.ASTERISK.equals(columnName)) {
                            sb.append(SPACE_AS_SPACE).append(WD.QUOTATION_D).append(columnName).append(WD.QUOTATION_D);
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

                    appendColumnName(_entityClass, _entityInfo, _propColumnNameMap, _tableAlias, columnName, null, false, null, isForSelect);
                }
            }
        } else if (N.notEmpty(_propOrColumnNameAliases)) {
            int i = 0;
            for (final Map.Entry<String, String> entry : _propOrColumnNameAliases.entrySet()) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(_entityClass, _entityInfo, _propColumnNameMap, _tableAlias, entry.getKey(), entry.getValue(), false, null, isForSelect);
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
                            selectionWithClassAlias, selectionClassAlias, isForSelect);
                }
            }
        } else {
            throw new UnsupportedOperationException("No select part specified");
        }

        _sb.append(_SPACE_FROM_SPACE);

        _sb.append(fromCause);

        return this;
    }

    private void addPropColumnMapForAlias(final Class<?> entityClass, final String alias) {
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
        _sb.append(_SPACE_OFFSET_SPACE).append(offset).append(_SPACE_ROWS_SPACE);

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
        _sb.append(" FETCH FIRST ").append(n).append(" ROWS ONLY");

        return this;
    }

    /**
     * Appends a condition to the SQL statement.
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
    public SQLBuilder append(final Condition cond) {
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
                        _sb.append(WD._PARENTHESES_L);
                        int idx = 0;

                        for (final String joinTableName : join.getJoinEntities()) {
                            if (idx++ > 0) {
                                _sb.append(_COMMA_SPACE);
                            }

                            _sb.append(joinTableName);
                        }

                        _sb.append(WD._PARENTHESES_R);
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

    /**
     * Conditionally appends a condition to the SQL statement.
     * 
     * <pre>{@code
     * boolean includeAgeFilter = true;
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .appendIf(includeAgeFilter, CF.gt("age", 18))
     *                 .sql();
     * // Output: SELECT * FROM users WHERE age > ?
     * }</pre>
     * 
     * @param b if true, the condition will be appended
     * @param cond the condition to append
     * @return this SQLBuilder instance for method chaining
     */
    public SQLBuilder appendIf(final boolean b, final Condition cond) {
        if (b) {
            append(cond);
        }

        return this;
    }

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

    /**
     * Conditionally appends one of two conditions based on a boolean value.
     * 
     * <pre>{@code
     * boolean isActive = true;
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .appendIfOrElse(isActive, 
     *                     CF.eq("status", "active"),
     *                     CF.eq("status", "inactive"))
     *                 .sql();
     * // Output: SELECT * FROM users WHERE status = ?
     * }</pre>
     * 
     * @param b if true, append condToAppendForTrue; otherwise append condToAppendForFalse
     * @param condToAppendForTrue the condition to append if b is true
     * @param condToAppendForFalse the condition to append if b is false
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public SQLBuilder appendIfOrElse(final boolean b, final Condition condToAppendForTrue, final Condition condToAppendForFalse) {
        if (b) {
            append(condToAppendForTrue);
        } else {
            append(condToAppendForFalse);
        }

        return this;
    }

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
                        _sb.append(" = :");
                        _sb.append(columnName);
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
    public SP pair() {
        final String sql = sql();

        return new SP(sql, _parameters);
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
        return func.apply(pair());
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
        final SP sp = pair();

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
        consumer.accept(pair());
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
        final SP sp = pair();

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
    void init(final boolean setForUpdate) {
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
                    n -> (WD.DELETE + WD.SPACE + WD.FROM + WD.SPACE + n).toCharArray());

            _sb.append(deleteFromTableChars);
        } else if (_op == OperationType.QUERY && !_hasFromBeenSet && !_isForConditionOnly) {
            throw new RuntimeException("'from' methods has not been called for query: " + _op);
        }
    }

    private void setEntityClass(final Class<?> entityClass) {
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
    private void setParameterForSQL(final Object propValue) {
        if (CF.QME.equals(propValue)) {
            _sb.append(WD._QUESTION_MARK);
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
    private void setParameterForRawSQL(final Object propValue) {
        if (CF.QME.equals(propValue)) {
            _sb.append(WD._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            _sb.append(WD._QUESTION_MARK);

            _parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter for named SQL.
     *
     * @param propName
     * @param propValue
     */
    private void setParameterForNamedSQL(final String propName, final Object propValue) {
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
    private void setParameterForIbatisNamedSQL(final String propName, final Object propValue) {
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
    private void setParameter(final String propName, final Object propValue) {
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
    private void appendInsertProps(final Map<String, Object> props) {
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
    private void appendCondition(final Condition cond) {
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
            _sb.append(WD.AND);
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
            _sb.append(WD.AND);
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
            _sb.append(WD.SPACE_PARENTHESES_L);

            for (int i = 0, len = params.size(); i < len; i++) {
                if (i > 0) {
                    _sb.append(WD.COMMA_SPACE);
                }

                if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                    setParameter(propName + (i + 1), params.get(i));
                } else {
                    setParameter(propName, params.get(i));
                }
            }

            _sb.append(WD._PARENTHESES_R);
        } else if (cond instanceof final InSubQuery inSubQuery) {
            final String propName = inSubQuery.getPropName();

            if (Strings.isNotEmpty(propName)) {
                appendColumnName(propName);
            } else {
                _sb.append(WD._PARENTHESES_L);

                int idx = 0;

                for (final String e : inSubQuery.getPropNames()) {
                    if (idx++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(e);
                }

                _sb.append(WD._PARENTHESES_R);
            }

            _sb.append(_SPACE);
            _sb.append(inSubQuery.getOperator().toString());

            _sb.append(WD.SPACE_PARENTHESES_L);

            appendCondition(inSubQuery.getSubQuery());

            _sb.append(WD._PARENTHESES_R);
        } else if (cond instanceof final NotIn notIn) {
            final String propName = notIn.getPropName();
            final List<Object> params = notIn.getParameters();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(notIn.getOperator().toString());
            _sb.append(WD.SPACE_PARENTHESES_L);

            for (int i = 0, len = params.size(); i < len; i++) {
                if (i > 0) {
                    _sb.append(WD.COMMA_SPACE);
                }

                if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                    setParameter(propName + (i + 1), params.get(i));
                } else {
                    setParameter(propName, params.get(i));
                }
            }

            _sb.append(WD._PARENTHESES_R);
        } else if (cond instanceof final NotInSubQuery notInSubQuery) {
            final String propName = notInSubQuery.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(notInSubQuery.getOperator().toString());
            _sb.append(WD.SPACE_PARENTHESES_L);

            appendCondition(notInSubQuery.getSubQuery());

            _sb.append(WD._PARENTHESES_R);
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
            //        if ((i > 2) && WD.AS.equalsIgnoreCase(words.get(i - 2))) {
            //            sb.append(word);
            //        } else if ((i > 1) && WD.SPACE.equalsIgnoreCase(words.get(i - 1))
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

    private void appendStringExpr(final String expr, final boolean isFromAppendColumn) {
        // TODO performance improvement.

        if (expr.length() < 16) {
            boolean allChars = true;
            char ch = 0;

            for (int i = 0, len = expr.length(); i < len; i++) {
                ch = expr.charAt(i);

                // https://www.sciencebuddies.org/science-fair-projects/references/ascii-table
                if (ch < 'A' || (ch > 'Z' && ch < '_') || ch > 'z') {
                    allChars = false;
                    break;
                }
            }

            if (allChars) {
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

    private void appendColumnName(final String propName) {
        appendColumnName(_entityClass, _entityInfo, _propColumnNameMap, _tableAlias, propName, null, false, null, false);
    }

    private void appendColumnName(final Class<?> entityClass, final BeanInfo entityInfo, final ImmutableMap<String, Tuple2<String, Boolean>> propColumnNameMap,
            final String tableAlias, final String propName, final String propAlias, final boolean withClassAlias, final String classAlias,
            final boolean isForSelect) {
        Tuple2<String, Boolean> tp = propColumnNameMap == null ? null : propColumnNameMap.get(propName);

        if (tp != null) {
            if (tp._2 && tableAlias != null && !tableAlias.isEmpty()) {
                _sb.append(tableAlias).append(WD._PERIOD);
            }

            _sb.append(tp._1);

            if (isForSelect && _namingPolicy != NamingPolicy.NO_CHANGE) {
                _sb.append(_SPACE_AS_SPACE);
                _sb.append(WD._QUOTATION_D);

                if (withClassAlias) {
                    _sb.append(classAlias).append(WD._PERIOD);
                }

                _sb.append(Strings.isNotEmpty(propAlias) ? propAlias : propName);
                _sb.append(WD._QUOTATION_D);
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

                    _sb.append(propEntityTableAliasOrName).append(WD._PERIOD).append(subPropColumnNameMap.get(subPropName)._1);

                    if (isForSelect) {
                        _sb.append(_SPACE_AS_SPACE);
                        _sb.append(WD._QUOTATION_D);
                        _sb.append(propInfo.name).append(WD._PERIOD).append(subPropName);
                        _sb.append(WD._QUOTATION_D);
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

                        if (isForSelect && _namingPolicy != NamingPolicy.NO_CHANGE) {
                            _sb.append(_SPACE_AS_SPACE);
                            _sb.append(WD._QUOTATION_D);

                            if (withClassAlias) {
                                _sb.append(classAlias).append(WD._PERIOD);
                            }

                            _sb.append(Strings.isNotEmpty(propAlias) ? propAlias : propName);
                            _sb.append(WD._QUOTATION_D);
                        }

                        return;
                    }
                }
            }
        }

        if (Strings.isNotEmpty(propAlias)) {
            appendStringExpr(propName, true);

            if (isForSelect && _namingPolicy != NamingPolicy.NO_CHANGE) {
                _sb.append(_SPACE_AS_SPACE);
                _sb.append(WD._QUOTATION_D);

                if (withClassAlias) {
                    _sb.append(classAlias).append(WD._PERIOD);
                }

                _sb.append(propAlias);
                _sb.append(WD._QUOTATION_D);
            }
        } else if (isForSelect) {
            int index = propName.indexOf(" AS ");

            if (index < 0) {
                index = propName.indexOf(" as ");
            }

            if (index > 0) {
                //noinspection ConstantValue
                appendColumnName(entityClass, entityInfo, propColumnNameMap, tableAlias, propName.substring(0, index).trim(),
                        propName.substring(index + 4).trim(), withClassAlias, classAlias, isForSelect);
            } else {
                appendStringExpr(propName, true);

                if (_namingPolicy != NamingPolicy.NO_CHANGE && !(propName.charAt(propName.length() - 1) == '*' || propName.endsWith(")"))) {
                    _sb.append(_SPACE_AS_SPACE);
                    _sb.append(WD._QUOTATION_D);

                    if (withClassAlias) {
                        _sb.append(classAlias).append(WD._PERIOD);
                    }

                    _sb.append(propName);
                    _sb.append(WD._QUOTATION_D);
                }
            }
        } else {
            appendStringExpr(propName, true);
        }
    }

    private static boolean hasSubEntityToInclude(final Class<?> entityClass, final boolean includeSubEntityProperties) {
        return includeSubEntityProperties && N.notEmpty(getSubEntityPropNames(entityClass));
    }

    /**
     * Checks if is subquery.
     *
     * @param propOrColumnNames
     * @return true, if is subquery
     */
    private static boolean isSubQuery(final String... propOrColumnNames) {
        if (propOrColumnNames.length == 1) {
            int index = SQLParser.indexWord(propOrColumnNames[0], WD.SELECT, 0, false);

            if (index >= 0) {
                index = SQLParser.indexWord(propOrColumnNames[0], WD.FROM, index, false);

                return index >= 1;
            }
        }

        return false;
    }

    static String formalizeColumnName(final String word, final NamingPolicy namingPolicy) {
        if (sqlKeyWords.contains(word) || namingPolicy == NamingPolicy.NO_CHANGE) {
            return word;
        }
        if (namingPolicy == NamingPolicy.LOWER_CAMEL_CASE) {
            return ClassUtil.formalizePropName(word);
        }
        return namingPolicy.convert(word);
    }

    private String formalizeColumnName(final ImmutableMap<String, Tuple2<String, Boolean>> propColumnNameMap, final String propName) {
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

        return formalizeColumnName(propName, _namingPolicy);
    }

    private static void parseInsertEntity(final SQLBuilder instance, final Object entity, final Set<String> excludedPropNames) {
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
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(entity.getClass());

            for (final String propName : propNames) {
                map.put(propName, entityInfo.getPropValue(entity, propName));
            }

            instance._props = map;
        }
    }

    private static List<Map<String, Object>> toInsertPropsList(final Collection<?> propsList) {
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

        return newPropsList;
    }

    static void checkMultiSelects(final List<Selection> multiSelects) {
        N.checkArgNotEmpty(multiSelects, "multiSelects");

        for (final Selection selection : multiSelects) {
            N.checkArgNotNull(selection.entityClass(), "Class can't be null in 'multiSelects'");
        }
    }

    static ImmutableMap<String, Tuple2<String, Boolean>> prop2ColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        return QueryUtil.prop2ColumnNameMap(entityClass, namingPolicy);
    }

    enum SQLPolicy {
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

        static SCSB createInstance() {
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
            N.checkArgNotEmpty(selectPart, SELECT_PART);

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
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

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
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

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
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

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
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for a table with entity class context.
         *
         * @param tableName the table name to count rows from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for COUNT operation
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
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
     * For example:
     * <pre>
     * <code>
     * N.println(ACSB.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = 1
     * </code>
     * </pre>
     *
     * @deprecated {@code PAC or NAC} is preferred for better security and performance
     */
    @Deprecated
    public static class ACSB extends SQLBuilder {

        ACSB() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.SQL);
        }

        static ACSB createInstance() {
            return new ACSB();
        }

        /**
         * Creates an INSERT SQL builder for a single column.
         *
         * @param expr the column name or expression
         * @return a new SQLBuilder instance for INSERT operation
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for multiple columns.
         *
         * @param propOrColumnNames the column names to insert
         * @return a new SQLBuilder instance for INSERT operation
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
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
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
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
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object.
         *
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance for INSERT operation
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
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity class.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for INSERT operation
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
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with entity class context.
         *
         * @param tableName the table name to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for UPDATE operation
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for UPDATE operation
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class, excluding specified properties.
         *
         * @param entityClass the entity class
         * @param excludedPropNames additional properties to exclude from updates
         * @return a new SQLBuilder instance for UPDATE operation
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
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
         */
        public static SQLBuilder deleteFrom(final String tableName) {
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
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for an entity class.
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for DELETE operation
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a custom select expression.
         *
         * @param selectPart the select expression
         * @return a new SQLBuilder instance for SELECT operation
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

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
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * Un-parameterized SQL builder with lower camel case field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(LCSB.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT firstName, lastName FROM account WHERE id = 1
     * </code>
     * </pre>
     *
     * @deprecated {@code PLC or NLC} is preferred.
     */
    @Deprecated
    public static class LCSB extends SQLBuilder {

        LCSB() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.SQL);
        }

        static LCSB createInstance() {
            return new LCSB();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * Parameterized('?') SQL builder with {@code NamingPolicy.NO_CHANGE} field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(PSB.select("first_Name", "last_NaMe").from("account").where(CF.eq("last_NaMe", 1)).sql());
     * // SELECT first_Name, last_NaMe FROM account WHERE last_NaMe = ?
     * </code>
     * </pre>
     */
    public static class PSB extends SQLBuilder {

        PSB() {
            super(NamingPolicy.NO_CHANGE, SQLPolicy.PARAMETERIZED_SQL);
        }

        static PSB createInstance() {
            return new PSB();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * Parameterized('?') SQL builder with snake case (lower case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(PSC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = ?
     * </code>
     * </pre>
     */
    public static class PSC extends SQLBuilder {

        PSC() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.PARAMETERIZED_SQL);
        }

        static PSC createInstance() {
            return new PSC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * Parameterized('?') SQL builder with all capital case (upper case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(PAC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = ?
     * </code>
     * </pre>
     */
    public static class PAC extends SQLBuilder {

        PAC() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.PARAMETERIZED_SQL);
        }

        static PAC createInstance() {
            return new PAC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * Parameterized('?') SQL builder with lower camel case field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(PLC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT firstName, lastName FROM account WHERE id = ?
     * </code>
     * </pre>
     */
    public static class PLC extends SQLBuilder {

        PLC() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.PARAMETERIZED_SQL);
        }

        static PLC createInstance() {
            return new PLC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * For example:
     * <pre>
     * <code>
     * N.println(NSB.select("first_Name", "last_NaMe").from("account").where(CF.eq("last_NaMe", 1)).sql());
     * // SELECT first_Name, last_NaMe FROM account WHERE last_NaMe = :last_NaMe
     * </code>
     * </pre>
     */
    public static class NSB extends SQLBuilder {

        NSB() {
            super(NamingPolicy.NO_CHANGE, SQLPolicy.NAMED_SQL);
        }

        @Override
        protected boolean isNamedSql() {
            return true;
        }

        static NSB createInstance() {
            return new NSB();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * Named SQL builder with snake case (lower case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(NSC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = :id
     * </code>
     * </pre>
     */
    public static class NSC extends SQLBuilder {

        NSC() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.NAMED_SQL);
        }

        @Override
        protected boolean isNamedSql() {
            return true;
        }

        static NSC createInstance() {
            return new NSC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     *
     * For example:
     * <pre>
     * <code>
     * N.println(NAC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = :id
     * </code>
     * </pre>
     */
    public static class NAC extends SQLBuilder {

        NAC() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.NAMED_SQL);
        }

        @Override
        protected boolean isNamedSql() {
            return true;
        }

        static NAC createInstance() {
            return new NAC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     *
     * For example:
     * <pre>
     * <code>
     * N.println(NLC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT firstName, lastName FROM account WHERE id = :id
     * </code>
     * </pre>
     */
    public static class NLC extends SQLBuilder {

        NLC() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.NAMED_SQL);
        }

        @Override
        protected boolean isNamedSql() {
            return true;
        }

        static NLC createInstance() {
            return new NLC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * For example:
     * <pre>
     * <code>
     * N.println(MSB.select("first_Name", "last_NaMe").from("account").where(CF.eq("last_NaMe", 1)).sql());
     * // SELECT first_Name, last_NaMe FROM account WHERE last_NaMe = #{last_NaMe}
     * </code>
     * </pre>
     * @deprecated
     */
    @Deprecated
    public static class MSB extends SQLBuilder {

        MSB() {
            super(NamingPolicy.NO_CHANGE, SQLPolicy.IBATIS_SQL);
        }

        static MSB createInstance() {
            return new MSB();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * For example:
     * <pre>
     * <code>
     * N.println(MLC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = #{id}
     * </code>
     * </pre>
     * @deprecated
     */
    @Deprecated
    public static class MSC extends SQLBuilder {

        MSC() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.IBATIS_SQL);
        }

        static MSC createInstance() {
            return new MSC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * For example:
     * <pre>
     * <code>
     * N.println(MAC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = #{id}
     * </code>
     * </pre>
     * @deprecated
     */
    @Deprecated
    public static class MAC extends SQLBuilder {

        MAC() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.IBATIS_SQL);
        }

        static MAC createInstance() {
            return new MAC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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
     * For example:
     * <pre>
     * <code>
     * N.println(MLC.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // SELECT firstName, lastName FROM account WHERE id = #{id}
     * </code>
     * </pre>
     * @deprecated
     */
    @Deprecated
    public static class MLC extends SQLBuilder {

        MLC() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.IBATIS_SQL);
        }

        static MLC createInstance() {
            return new MLC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert SQL.
         *
         * @param propsList list of entities or properties maps.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
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
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECT_PART);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, PROP_OR_COLUMN_NAMES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param propOrColumnNameAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, PROP_OR_COLUMN_NAME_ALIASES);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param alias
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
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
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         *
         *
         * @param entityClassA
         * @param tableAliasA
         * @param classAliasA
         * @param excludedPropNamesA
         * @param entityClassB
         * @param tableAliasB
         * @param classAliasB
         * @param excludedPropNamesB
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         *
         *
         * @param multiSelects
         * @return
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         *
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         *
         *
         * @param tableName
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final String tableName, final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(tableName, entityClass);
        }

        /**
         *
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * To generate {@code sql} part for the specified {@code cond} only.
         *
         * @param cond
         * @param entityClass
         * @return
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

    public static final class SP {
        public final String sql;
        public final List<Object> parameters;

        SP(final String sql, final List<Object> parameters) {
            this.sql = sql;
            this.parameters = ImmutableList.wrap(parameters);
        }

        /**
         *
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(sql) * 31 + N.hashCode(parameters);
        }

        /**
         *
         *
         * @param obj
         * @return
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
         *
         *
         * @return
         */
        @Override
        public String toString() {
            return "{sql=" + sql + ", parameters=" + N.toString(parameters) + "}";
        }
    }

    private static final BiConsumer<StringBuilder, String> defaultHandlerForNamedParameter = (sb, propName) -> sb.append(":").append(propName);
    // private static final BiConsumer<StringBuilder, String> mybatisHandlerForNamedParameter = (sb, propName) -> sb.append("#{").append(propName).append("}");

    private static final ThreadLocal<BiConsumer<StringBuilder, String>> handlerForNamedParameter_TL = ThreadLocal //NOSONAR
            .withInitial(() -> defaultHandlerForNamedParameter);

    /**
     *
     *
     * @param handlerForNamedParameter
     */
    public static void setHandlerForNamedParameter(final BiConsumer<StringBuilder, String> handlerForNamedParameter) {
        N.checkArgNotNull(handlerForNamedParameter, "handlerForNamedParameter");
        handlerForNamedParameter_TL.set(handlerForNamedParameter);
    }

    public static void resetHandlerForNamedParameter() {
        handlerForNamedParameter_TL.set(defaultHandlerForNamedParameter);
    }

    private static String getFromClause(final List<Selection> multiSelects, final NamingPolicy namingPolicy) {
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
