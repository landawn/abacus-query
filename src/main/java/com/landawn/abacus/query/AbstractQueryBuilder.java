/*
 * Copyright (c) 2025, Haiyang Li.
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

package com.landawn.abacus.query;

import static com.landawn.abacus.query.SK._SPACE;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.query.SQLBuilder.NAC;
import com.landawn.abacus.query.SQLBuilder.NLC;
import com.landawn.abacus.query.SQLBuilder.NSC;
import com.landawn.abacus.query.SQLBuilder.PAC;
import com.landawn.abacus.query.SQLBuilder.PLC;
import com.landawn.abacus.query.SQLBuilder.PSC;
import com.landawn.abacus.query.condition.Clause;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.Join;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Maps;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.ObjectPool;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.OperationType;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
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
 * <p>The builder must be finalized by calling {@code toSql()} or {@code toSqlAndParameters()} to generate
 * the SQL string and release resources.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple SELECT
 * String sql = PSC.select("firstName", "lastName")
 *                 .from("account")
 *                 .where(Filters.equal("id", 1))
 *                 .toSql();
 * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = ?
 * 
 * // INSERT with entity
 * String sql = PSC.insert(account).into("account").toSql();
 * 
 * // UPDATE with conditions
 * String sql = PSC.update("account")
 *                 .set("name", "status")
 *                 .where(Filters.equal("id", 1))
 *                 .toSql();
 * }</pre>
 * 
 * <p>The builder supports different naming policies through its subclasses:</p>
 * <ul>
 *   <li>{@link PSC} - Parameterized SQL with snake_case naming</li>
 *   <li>{@link PAC} - Parameterized SQL with UPPER_CASE naming</li>
 *   <li>{@link PLC} - Parameterized SQL with camelCase naming</li>
 *   <li>{@link NSC} - Named SQL with snake_case naming</li>
 *   <li>{@link NAC} - Named SQL with UPPER_CASE naming</li>
 *   <li>{@link NLC} - Named SQL with camelCase naming</li>
 * </ul>
 * 
 * @see com.landawn.abacus.annotation.ReadOnly
 * @see com.landawn.abacus.annotation.ReadOnlyId
 * @see com.landawn.abacus.annotation.NonUpdatable
 * @see com.landawn.abacus.annotation.Transient
 * @see com.landawn.abacus.annotation.Table
 * @see com.landawn.abacus.annotation.Column
 * 
 * @param <This> the concrete implementation type that extends this builder for method chaining
 */
@SuppressWarnings("deprecation")
public abstract class AbstractQueryBuilder<This extends AbstractQueryBuilder<This>> { // NOSONAR

    // TODO performance goal: 80% cases (or maybe SQL.length < 1024?) can be composed in 0.1 millisecond. 0.01 millisecond will be fantastic if possible.

    protected static final Logger logger = LoggerFactory.getLogger(AbstractQueryBuilder.class);

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

    protected static final char[] _LIMIT = SK.LIMIT.toCharArray();

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

    protected static final char[] _EXCEPT_MINUS = SK.EXCEPT_MINUS.toCharArray();

    protected static final char[] _SPACE_EXCEPT_MINUS_SPACE = (SK.SPACE + SK.EXCEPT_MINUS + SK.SPACE).toCharArray();

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
                        sqlKeyWords.add(e.toUpperCase(Locale.ROOT));
                        sqlKeyWords.add(e.toLowerCase(Locale.ROOT));
                    }
                } catch (final Exception e) {
                    // ignore, should never happen.
                }
            }
        }
    }

    protected static final Map<Class<?>, ImmutableSet<String>> subEntityPropNamesPool = new ObjectPool<>(QueryUtil.POOL_SIZE);

    // private static final Map<Class<?>, ImmutableSet<String>> nonSubEntityPropNamesPool = new ObjectPool<>(N.POOL_SIZE);

    protected static final Map<Class<?>, Set<String>[]> defaultPropNamesPool = new ObjectPool<>(QueryUtil.POOL_SIZE);

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

    protected String _selectModifier; //NOSONAR

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
     * @param namingPolicy the naming policy for column names, defaults to SNAKE_CASE if null
     * @param sqlPolicy the SQL generation policy, defaults to RAW_SQL if null
     */
    protected AbstractQueryBuilder(final NamingPolicy namingPolicy, final SQLPolicy sqlPolicy) {
        if (activeStringBuilderCounter.incrementAndGet() > 1024) {
            logger.error("Too many(" + activeStringBuilderCounter.get()
                    + ") StringBuilder instances are created in SQLBuilder. The method toSql()/toSqlAndParameters()"
                    + " must be called to release resources and close SQLBuilder");
        }

        _sb = Objectory.createStringBuilder();

        _namingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy;
        _sqlPolicy = sqlPolicy == null ? SQLPolicy.RAW_SQL : sqlPolicy;

        _handlerForNamedParameter = handlerForNamedParameter_TL.get();
    }

    /**
     * Checks if this SQL builder generates named SQL (with named parameters).
     * 
     * @return {@code true} if this builder generates named SQL, {@code false} otherwise
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
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy;

        if (entityTableNames == null) {
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);

            if (entityInfo.tableName.isPresent()) {
                entityTableNames = Array.repeat(entityInfo.tableName.get(), 4);
            } else {
                final String simpleClassName = ClassUtil.getSimpleClassName(entityClass);
                entityTableNames = new String[] { Beans.toSnakeCase(simpleClassName), Beans.toScreamingSnakeCase(simpleClassName),
                        Beans.toCamelCase(simpleClassName), simpleClassName };
            }

            classTableNameMap.put(entityClass, entityTableNames);
        }

        switch (effectiveNamingPolicy) {
            case SNAKE_CASE:
                return entityTableNames[0];

            case SCREAMING_SNAKE_CASE:
                return entityTableNames[1];

            case CAMEL_CASE:
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
        if (entityClass == null) {
            return "";
        }

        String alias = classTableAliasMap.get(entityClass);

        if (alias == null) {
            if (entityClass.getAnnotation(Table.class) != null) {
                alias = entityClass.getAnnotation(Table.class).alias();
            }

            if (alias == null) {
                alias = "";
            }

            classTableAliasMap.put(entityClass, alias);
        }

        return alias;
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
     * @return {@code true} if the value is null or a number equal to 0, {@code false} otherwise
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
                val = defaultPropNamesPool.get(entityClass);

                if (val != null) {
                    return val;
                }

                final Set<String> entityPropNames = N.newLinkedHashSet(Beans.getPropNameList(entityClass));
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

                    subEntityPropNameList = N.newLinkedHashSet(Beans.getPropNameList(subEntityClass));
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

                    if (QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo)) {
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

                for (final String idPropName : QueryUtil.getIdPropNames(entityClass)) {
                    val[3].remove(idPropName);

                    final java.lang.reflect.Method getter = Beans.getPropGetter(entityClass, idPropName);

                    if (getter != null) {
                        val[3].remove(Beans.getPropNameByMethod(getter));
                    }
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
                subEntityPropNames = subEntityPropNamesPool.get(entityClass);

                if (subEntityPropNames == null) {
                    final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
                    final Set<String> subEntityPropNameSet = N.newLinkedHashSet(entityInfo.subEntityPropNameList);
                    subEntityPropNames = ImmutableSet.wrap(subEntityPropNameSet);

                    subEntityPropNamesPool.put(entityClass, subEntityPropNames);
                }
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

        if (Strings.isEmpty(tableAlias)) {
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
    //                    propNameList = N.asImmutableList(new ArrayList<>(N.getPropGetterList(entityClass).keySet()));
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
    //                    propNameSet = N.asImmutableSet(N.newLinkedHashSet(N.getPropGetterList(entityClass).keySet()));
    //                    classPropNameSetPool.put(entityClass, propNameSet);
    //                }
    //            }
    //        }
    //
    //        return propNameSet;
    //    }

    /**
     * Creates a map with property names as keys and {@code Filters.QME} (question mark expression) as values.
     * <p>This is useful for creating parameterized queries with named parameters.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Expression> params = SQLBuilder.named("firstName", "lastName");
     * }</pre>
     * 
     * @param propNames the property names
     * @return a map with property names mapped to question mark expressions
     */
    @Beta
    protected static Map<String, Expression> named(final String... propNames) {
        final Map<String, Expression> m = N.newLinkedHashMap(propNames.length);

        for (final String propName : propNames) {
            m.put(propName, Filters.QME);
        }

        return m;
    }

    /**
     * Creates a map with property names as keys and {@code Filters.QME} (question mark expression) as values.
     * <p>This is useful for creating parameterized queries with named parameters.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Expression> params = SQLBuilder.named(Arrays.asList("firstName", "lastName"));
     * }</pre>
     * 
     * @param propNames the collection of property names
     * @return a map with property names mapped to question mark expressions
     */
    @Beta
    protected static Map<String, Expression> named(final Collection<String> propNames) {
        final Map<String, Expression> m = N.newLinkedHashMap(propNames.size());

        for (final String propName : propNames) {
            m.put(propName, Filters.QME);
        }

        return m;
    }

    /**
     * Specifies the target table for an INSERT or SELECT INTO operation.
     * <p>Must be called after setting the columns/values to insert or columns to select.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String insertSql = PSC.insert("firstName", "lastName").into("account").toSql();
     * String selectIntoSql = PSC.select("firstName").into("account_backup").from("account").toSql();
     * }</pre>
     *
     * @param tableName the name of the table to insert into
     * @return this SQLBuilder instance for method chaining
     * @throws IllegalStateException if called on non-INSERT/SELECT operation or if columns/values not set
     */
    public This into(final String tableName) {
        if (!(_op == OperationType.ADD || _op == OperationType.QUERY)) {
            throw new IllegalStateException("Invalid operation for into(): " + _op + ". Expected ADD or QUERY");
        }

        if (_op == OperationType.QUERY) {
            if (N.isEmpty(_propOrColumnNames) && N.isEmpty(_propOrColumnNameAliases) && N.isEmpty(_multiSelects)) {
                throw new IllegalStateException("Column names must be set by select() before calling into()");
            }
        } else {
            if (N.isEmpty(_propOrColumnNames) && N.isEmpty(_props) && N.isEmpty(_propsList)) {
                throw new IllegalStateException("Column names must be set by insert() before calling into()");
            }
        }

        _tableName = tableName;

        _sb.append(_INSERT);
        _sb.append(_SPACE_INTO_SPACE);

        _sb.append(tableName);

        _sb.append(_SPACE);
        _sb.append(SK._PARENTHESIS_L);

        final Collection<String> insertColumnNames;

        if (N.notEmpty(_propOrColumnNames)) {
            insertColumnNames = _propOrColumnNames;
        } else if (N.notEmpty(_propOrColumnNameAliases)) {
            insertColumnNames = _propOrColumnNameAliases.keySet();
        } else if (N.notEmpty(_multiSelects)) {
            final List<String> allPropNames = new ArrayList<>();

            for (final Selection selection : _multiSelects) {
                final Collection<String> selectPropNames = N.notEmpty(selection.selectPropNames()) ? selection.selectPropNames()
                        : QueryUtil.getSelectPropNames(selection.entityClass(), selection.includeSubEntityProperties(), selection.excludedPropNames());
                allPropNames.addAll(selectPropNames);
            }

            insertColumnNames = allPropNames;
        } else {
            final Map<String, Object> localProps = N.isEmpty(_props) ? _propsList.iterator().next() : _props;
            insertColumnNames = localProps.keySet();
        }

        int colIdx = 0;
        for (final String columnName : insertColumnNames) {
            if (colIdx++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(columnName);
        }

        _sb.append(SK._PARENTHESIS_R);

        if (_op == OperationType.ADD) {
            _sb.append(_SPACE_VALUES_SPACE);

            _sb.append(SK._PARENTHESIS_L);

            if (N.notEmpty(_propOrColumnNames)) {
                switch (_sqlPolicy) {
                    case RAW_SQL:
                    case PARAMETERIZED_SQL: {
                        for (int i = 0, size = insertColumnNames.size(); i < size; i++) {
                            if (i > 0) {
                                _sb.append(_COMMA_SPACE);
                            }

                            _sb.append(SK._QUESTION_MARK);
                        }

                        break;
                    }

                    case NAMED_SQL: {
                        int i = 0;
                        for (final String columnName : insertColumnNames) {
                            if (i++ > 0) {
                                _sb.append(_COMMA_SPACE);
                            }

                            _handlerForNamedParameter.accept(_sb, columnName);
                        }

                        break;
                    }

                    case IBATIS_SQL: {
                        int i = 0;
                        for (final String columnName : insertColumnNames) {
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
                        throw new UnsupportedOperationException("SQL policy not supported: " + _sqlPolicy); //NOSONAR
                }
            } else if (N.notEmpty(_props)) {
                appendInsertProps(_props, insertColumnNames);
            } else {
                int i = 0;
                for (final Map<String, Object> localProps : _propsList) {
                    if (i++ > 0) {
                        _sb.append(SK._PARENTHESIS_R);
                        _sb.append(_COMMA_SPACE);
                        _sb.append(SK._PARENTHESIS_L);
                    }

                    appendInsertProps(localProps, insertColumnNames, i - 1);
                }
            }

            _sb.append(SK._PARENTHESIS_R);
        }
        // When _op is QUERY (i.e., select().into().from()), skip the VALUES clause.
        // The subsequent from() call will append "SELECT ... FROM ..." to produce:
        // INSERT INTO target (cols) SELECT cols FROM source

        return (This) this;
    }

    /**
     * Specifies the target table for an INSERT operation using an entity class.
     * <p>The table name will be derived from the entity class based on the naming policy.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.insert(account).into(Account.class).toSql();
     * // Table name derived from Account class based on naming policy
     * }</pre>
     * 
     * @param entityClass the entity class representing the target table
     * @return this SQLBuilder instance for method chaining
     */
    public This into(final Class<?> entityClass) {
        if (_entityClass == null) {
            setEntityClass(entityClass);
        }

        return into(getTableName(entityClass, _namingPolicy));
    }

    /**
     * Specifies the target table for an INSERT operation with explicit table name and entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.insert(account).into("account_archive", Account.class).toSql();
     * // Inserts into specified table with Account class mapping
     * }</pre>
     *
     * @param tableName the name of the table to insert into
     * @param entityClass the entity class for property mapping (can be null)
     * @return this SQLBuilder instance for method chaining
     */
    public This into(final String tableName, final Class<?> entityClass) {
        if (entityClass != null) {
            setEntityClass(entityClass);
        }

        return into(tableName);
    }

    /**
     * Adds DISTINCT clause to the SELECT statement.
     * <p>This method is equivalent to calling {@code selectModifier(DISTINCT)}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("name").distinct().from("account").toSql();
     * // Output: SELECT DISTINCT name FROM account
     * }</pre>
     * 
     * @return this SQLBuilder instance for method chaining
     */
    public This distinct() { //NOSONAR
        return selectModifier(DISTINCT);
    }

    /**
     * Adds a pre-select modifier to the SELECT statement.
     * <p>For better performance, this method should be called before {@code from}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").selectModifier("TOP 10").from("account").toSql();
     * // Output: SELECT TOP 10 * FROM account
     * }</pre>
     * 
     * @param selectModifier modifiers like ALL, DISTINCT, DISTINCTROW, TOP, etc.
     * @return this SQLBuilder instance for method chaining
     * @throws IllegalStateException if selectModifier has already been set
     */
    public This selectModifier(final String selectModifier) {
        if (Strings.isNotEmpty(_selectModifier)) {
            throw new IllegalStateException("selectModifier has already been set and cannot be set again");
        }

        if (Strings.isNotEmpty(selectModifier)) {
            _selectModifier = selectModifier;

            final int selectIdx = _sb.indexOf(SK.SELECT);

            if (selectIdx >= 0) {
                final int len = _sb.length();

                _sb.append(_SPACE);

                appendStringExpr(_selectModifier, false);

                final int newLength = _sb.length();

                _sb.insert(selectIdx + SK.SELECT.length(), _sb.substring(len));
                _sb.setLength(newLength);
            }
        }

        return (This) this;
    }

    /**
     * Sets the FROM clause with multiple table names.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from("users", "orders").toSql();
     * // Output: SELECT * FROM users, orders
     * }</pre>
     * 
     * @param tableNames the table names to use in the FROM clause
     * @return this SQLBuilder instance for method chaining
     */
    public This from(final String... tableNames) {
        N.checkArgNotEmpty(tableNames, "tableNames");

        if (tableNames.length == 1) {
            return from(tableNames[0].trim());
        }

        final String localTableName = tableNames[0].trim();
        return from(localTableName, Strings.join(tableNames, SK.COMMA_SPACE));
    }

    /**
     * Sets the FROM clause with a collection of table names.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> tables = Arrays.asList("users", "orders");
     * String sql = PSC.select("*").from(tables).toSql();
     * // Output: SELECT * FROM users, orders
     * }</pre>
     * 
     * @param tableNames the collection of table names to use in the FROM clause
     * @return this SQLBuilder instance for method chaining
     */
    public This from(final Collection<String> tableNames) {
        N.checkArgNotEmpty(tableNames, "tableNames");

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from("users u").toSql();
     * // Output: SELECT * FROM users u
     * 
     * String sql2 = PSC.select("*").from("(SELECT * FROM users) t").toSql();
     * // Output: SELECT * FROM (SELECT * FROM users) t
     * }</pre>
     * 
     * @param expr the FROM clause expression
     * @return this SQLBuilder instance for method chaining
     */
    public This from(String expr) {
        expr = expr.trim();

        int depth = 0;
        int commaIdx = -1;

        for (int i = 0; i < expr.length(); i++) {
            final char c = expr.charAt(i);

            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == SK._COMMA && depth == 0) {
                commaIdx = i;
                break;
            }
        }

        final String localTableName = commaIdx > 0 ? expr.substring(0, commaIdx) : expr;

        return from(localTableName.trim(), expr);
    }

    /**
     * Sets the FROM clause with an expression and associates it with an entity class.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from("users u", User.class).toSql();
     * // Associates the User class for property mapping
     * }</pre>
     * 
     * @param expr the FROM clause expression
     * @param entityClass the entity class for property mapping
     * @return this SQLBuilder instance for method chaining
     */
    public This from(final String expr, final Class<?> entityClass) {
        if (entityClass != null) {
            setEntityClass(entityClass);
        }

        return from(expr);
    }

    /**
     * Sets the FROM clause using an entity class.
     * <p>The table name will be derived from the entity class.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).toSql();
     * // Table name derived from User class based on naming policy
     * }</pre>
     * 
     * @param entityClass the entity class representing the table
     * @return this SQLBuilder instance for method chaining
     */
    public This from(final Class<?> entityClass) {
        return from(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Sets the FROM clause using an entity class with an alias.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").toSql();
     * // Output: SELECT * FROM users u (table name based on naming policy)
     * }</pre>
     * 
     * @param entityClass the entity class representing the table
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public This from(final Class<?> entityClass, final String alias) {
        setEntityClass(entityClass);

        if (Strings.isEmpty(alias)) {
            return from(getTableName(entityClass, _namingPolicy));
        } else {
            return from(getTableName(entityClass, _namingPolicy) + " " + alias);
        }
    }

    protected This from(final Class<?> entityClass, final Collection<String> tableNames) {
        setEntityClass(entityClass);

        return from(tableNames);
    }

    /**
     * Sets the FROM clause with a custom table expression or clause.
     * 
     * <p>This method allows specifying additional FROM clause expressions beyond just a table name.</p>
     *
     * @param tableName the name of the table to select from
     * @param fromClause additional FROM clause expression or conditions
     * @return this builder instance for method chaining
     */
    protected This from(final String tableName, final String fromClause) {
        appendOperationBeforeFrom(tableName);

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

                        sb.append(normalizeColumnName(_propColumnNameMap, columnName));

                        if (_namingPolicy != NamingPolicy.NO_CHANGE && !SK.ASTERISK.equals(columnName)) {
                            sb.append(SPACE_AS_SPACE).append(SK.DOUBLE_QUOTE).append(columnName).append(SK.DOUBLE_QUOTE);
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
                selectionBeanInfo = Beans.isBeanClass(selectionEntityClass) ? ParserUtil.getBeanInfo(selectionEntityClass) : null;
                selectionPropColumnNameMap = Beans.isBeanClass(selectionEntityClass) ? prop2ColumnNameMap(selectionEntityClass, _namingPolicy) : null;
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
        }

        _sb.append(_SPACE_FROM_SPACE);

        _sb.append(fromClause);

        _hasFromBeenSet = true;

        return (This) this;
    }

    protected void appendOperationBeforeFrom(final String tableName) {
        if (_op != OperationType.QUERY) {
            throw new IllegalStateException("Invalid operation for from(): " + _op + ". Expected QUERY");
        }

        if (N.isEmpty(_propOrColumnNames) && N.isEmpty(_propOrColumnNameAliases) && N.isEmpty(_multiSelects)) {
            throw new IllegalStateException("Column names must be set by select() before calling from()");
        }

        final String trimmedTableName = tableName.trim();
        int idx = -1;

        if (!trimmedTableName.isEmpty() && trimmedTableName.charAt(0) == '(') {
            // For subquery expressions like "(SELECT * FROM users) t", find the closing parenthesis first
            int depth = 0;
            for (int i = 0; i < trimmedTableName.length(); i++) {
                final char ch = trimmedTableName.charAt(i);
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth--;
                    if (depth == 0) {
                        // Find the space after the closing parenthesis
                        final int spaceIdx = trimmedTableName.indexOf(' ', i + 1);
                        if (spaceIdx > 0) {
                            idx = spaceIdx;
                        }
                        break;
                    }
                }
            }
        } else {
            idx = trimmedTableName.indexOf(' ');
        }

        if (idx > 0) {
            _tableName = trimmedTableName.substring(0, idx).trim();
            _tableAlias = trimmedTableName.substring(idx + 1).trim();
        } else {
            _tableName = trimmedTableName;
        }

        if (_entityClass != null && Strings.isNotEmpty(_tableAlias)) {
            addPropColumnMapForAlias(_entityClass, _tableAlias);
        }

        if (!_sb.isEmpty() && _sb.charAt(_sb.length() - 1) != ' ') {
            _sb.append(_SPACE);
        }

        _sb.append(_SELECT);
        _sb.append(_SPACE);

        if (Strings.isNotEmpty(_selectModifier)) {
            appendStringExpr(_selectModifier, false);

            _sb.append(_SPACE);
        }
    }

    protected void addPropColumnMapForAlias(final Class<?> entityClass, final String alias) {
        if (_aliasPropColumnNameMap == null) {
            _aliasPropColumnNameMap = new HashMap<>();
        }

        if (N.isEmpty(_propColumnNameMap) && Beans.isBeanClass(entityClass)) {
            _propColumnNameMap = prop2ColumnNameMap(entityClass, _namingPolicy);
        }

        _aliasPropColumnNameMap.put(alias, Beans.isBeanClass(entityClass) ? prop2ColumnNameMap(entityClass, _namingPolicy) : _propColumnNameMap);
    }

    /**
     * Adds a JOIN clause to the SQL statement.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o ON u.id = o.user_id")
     *                 .toSql();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression (e.g., "orders o ON u.id = o.user_id")
     * @return this SQLBuilder instance for method chaining
     */
    public This join(final String expr) {
        _sb.append(_SPACE_JOIN_SPACE);

        _sb.append(expr);

        return (This) this;
    }

    /**
     * Adds a JOIN clause using an entity class.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from(User.class)
     *                 .join(Order.class)
     *                 .on("users.id = orders.user_id")
     *                 .toSql();
     * }</pre>
     * 
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public This join(final Class<?> entityClass) {
        return join(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a JOIN clause using an entity class with an alias.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from(User.class, "u")
     *                 .join(Order.class, "o")
     *                 .on("u.id = o.user_id")
     *                 .toSql();
     * }</pre>
     * 
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public This join(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return (This) this;
    }

    /**
     * Adds an INNER JOIN clause to the SQL statement.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .innerJoin("orders o ON u.id = o.user_id")
     *                 .toSql();
     * // Output: SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public This innerJoin(final String expr) {
        _sb.append(_SPACE_INNER_JOIN_SPACE);

        _sb.append(expr);

        return (This) this;
    }

    /**
     * Adds an INNER JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).innerJoin(Order.class).on("users.id = orders.user_id").toSql();
     * // Output: SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public This innerJoin(final Class<?> entityClass) {
        return innerJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds an INNER JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").innerJoin(Order.class, "o").on("u.id = o.user_id").toSql();
     * // Output: SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public This innerJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_INNER_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return (This) this;
    }

    /**
     * Adds a LEFT JOIN clause to the SQL statement.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .leftJoin("orders o ON u.id = o.user_id")
     *                 .toSql();
     * // Output: SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public This leftJoin(final String expr) {
        _sb.append(_SPACE_LEFT_JOIN_SPACE);

        _sb.append(expr);

        return (This) this;
    }

    /**
     * Adds a LEFT JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).leftJoin(Order.class).on("users.id = orders.user_id").toSql();
     * // Output: SELECT * FROM users LEFT JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public This leftJoin(final Class<?> entityClass) {
        return leftJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a LEFT JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").leftJoin(Order.class, "o").on("u.id = o.user_id").toSql();
     * // Output: SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public This leftJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_LEFT_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return (This) this;
    }

    /**
     * Adds a RIGHT JOIN clause to the SQL statement.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .rightJoin("orders o ON u.id = o.user_id")
     *                 .toSql();
     * // Output: SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public This rightJoin(final String expr) {
        _sb.append(_SPACE_RIGHT_JOIN_SPACE);

        _sb.append(expr);

        return (This) this;
    }

    /**
     * Adds a RIGHT JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).rightJoin(Order.class).on("users.id = orders.user_id").toSql();
     * // Output: SELECT * FROM users RIGHT JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public This rightJoin(final Class<?> entityClass) {
        return rightJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a RIGHT JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").rightJoin(Order.class, "o").on("u.id = o.user_id").toSql();
     * // Output: SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public This rightJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_RIGHT_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return (This) this;
    }

    /**
     * Adds a FULL JOIN clause to the SQL statement.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .fullJoin("orders o ON u.id = o.user_id")
     *                 .toSql();
     * // Output: SELECT * FROM users u FULL JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public This fullJoin(final String expr) {
        _sb.append(_SPACE_FULL_JOIN_SPACE);

        _sb.append(expr);

        return (This) this;
    }

    /**
     * Adds a FULL JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).fullJoin(Order.class).on("users.id = orders.user_id").toSql();
     * // Output: SELECT * FROM users FULL JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public This fullJoin(final Class<?> entityClass) {
        return fullJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a FULL JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").fullJoin(Order.class, "o").on("u.id = o.user_id").toSql();
     * // Output: SELECT * FROM users u FULL JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public This fullJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_FULL_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return (This) this;
    }

    /**
     * Adds a CROSS JOIN clause to the SQL statement.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .crossJoin("orders")
     *                 .toSql();
     * // Output: SELECT * FROM users CROSS JOIN orders
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public This crossJoin(final String expr) {
        _sb.append(_SPACE_CROSS_JOIN_SPACE);

        _sb.append(expr);

        return (This) this;
    }

    /**
     * Adds a CROSS JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).crossJoin(Order.class).toSql();
     * // Output: SELECT * FROM users CROSS JOIN orders
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public This crossJoin(final Class<?> entityClass) {
        return crossJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a CROSS JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").crossJoin(Order.class, "o").toSql();
     * // Output: SELECT * FROM users u CROSS JOIN orders o
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public This crossJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_CROSS_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return (This) this;
    }

    /**
     * Adds a NATURAL JOIN clause to the SQL statement.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .naturalJoin("orders")
     *                 .toSql();
     * // Output: SELECT * FROM users NATURAL JOIN orders
     * }</pre>
     * 
     * @param expr the join expression
     * @return this SQLBuilder instance for method chaining
     */
    public This naturalJoin(final String expr) {
        _sb.append(_SPACE_NATURAL_JOIN_SPACE);

        _sb.append(expr);

        return (This) this;
    }

    /**
     * Adds a NATURAL JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).naturalJoin(Order.class).toSql();
     * // Output: SELECT * FROM users NATURAL JOIN orders
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SQLBuilder instance for method chaining
     */
    public This naturalJoin(final Class<?> entityClass) {
        return naturalJoin(entityClass, QueryUtil.getTableAlias(entityClass));
    }

    /**
     * Adds a NATURAL JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").naturalJoin(Order.class, "o").toSql();
     * // Output: SELECT * FROM users u NATURAL JOIN orders o
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SQLBuilder instance for method chaining
     */
    public This naturalJoin(final Class<?> entityClass, final String alias) {
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_NATURAL_JOIN_SPACE);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return (This) this;
    }

    /**
     * Adds an ON clause for join conditions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o")
     *                 .on("u.id = o.user_id")
     *                 .toSql();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
     * }</pre>
     * 
     * @param expr the join condition expression
     * @return this SQLBuilder instance for method chaining
     */
    public This on(final String expr) {
        _sb.append(_SPACE_ON_SPACE);

        appendStringExpr(expr, false);

        return (This) this;
    }

    /**
     * Adds an ON clause with a condition object for join conditions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o")
     *                 .on(Filters.equal("u.id", "o.user_id"))
     *                 .toSql();
     * }</pre>
     * 
     * @param cond the join condition
     * @return this SQLBuilder instance for method chaining
     */
    public This on(final Condition cond) {
        _sb.append(_SPACE_ON_SPACE);

        appendCondition(cond);

        return (This) this;
    }

    /**
     * Adds a USING clause for join conditions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .join("orders")
     *                 .using("user_id")
     *                 .toSql();
     * // Output: SELECT * FROM users JOIN orders USING (user_id)
     * }</pre>
     * 
     * @param expr the column name(s) for the USING clause
     * @return this SQLBuilder instance for method chaining
     */
    public This using(final String expr) {
        _sb.append(_SPACE_USING_SPACE);

        final String trimmedExpr = expr == null ? null : expr.trim();

        if (Strings.isNotEmpty(trimmedExpr) && trimmedExpr.startsWith(SK.PARENTHESIS_L) && trimmedExpr.endsWith(SK.PARENTHESIS_R)) {
            appendStringExpr(trimmedExpr, false);
        } else {
            _sb.append(SK._PARENTHESIS_L);
            appendColumnName(expr);
            _sb.append(SK._PARENTHESIS_R);
        }

        return (This) this;
    }

    /**
     * Adds a WHERE clause with a string expression.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where("age > 18")
     *                 .toSql();
     * // Output: SELECT * FROM users WHERE age > 18
     * }</pre>
     * 
     * @param expr the WHERE condition expression
     * @return this SQLBuilder instance for method chaining
     */
    public This where(final String expr) {
        checkIfAlreadyCalled(SK.WHERE);

        init(true);

        _sb.append(_SPACE_WHERE_SPACE);

        appendStringExpr(expr, false);

        return (This) this;
    }

    /**
     * Adds a WHERE clause with a condition object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where(Filters.greaterThan("age", 18))
     *                 .toSql();
     * // Output: SELECT * FROM users WHERE age > ?
     * }</pre>
     * 
     * @param cond the WHERE condition
     * @return this SQLBuilder instance for method chaining
     * @see Filters
     */
    public This where(final Condition cond) {
        checkIfAlreadyCalled(SK.WHERE);

        init(true);

        _sb.append(_SPACE_WHERE_SPACE);

        appendCondition(cond);

        return (This) this;
    }

    /**
     * Adds a GROUP BY clause with a single column.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy("category")
     *                 .toSql();
     * // Output: SELECT category, COUNT(*) FROM products GROUP BY category
     * }</pre>
     * 
     * @param expr the column to group by
     * @return this SQLBuilder instance for method chaining
     */
    public This groupBy(final String expr) {
        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        appendColumnName(expr);

        return (This) this;
    }

    /**
     * Adds a GROUP BY clause with multiple columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy("category", "brand")
     *                 .toSql();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand
     * }</pre>
     * 
     * @param propOrColumnNames the columns to group by
     * @return this SQLBuilder instance for method chaining
     */
    public This groupBy(final String... propOrColumnNames) {
        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        for (int i = 0, len = propOrColumnNames.length; i < len; i++) {
            if (i > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(propOrColumnNames[i]);
        }

        return (This) this;
    }

    /**
     * Adds a GROUP BY clause with a single column and sort direction.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy("category", SortDirection.DESC)
     *                 .toSql();
     * // Output: SELECT category, COUNT(*) FROM products GROUP BY category DESC
     * }</pre>
     * 
     * @param columnName the column to group by
     * @param direction the sort direction
     * @return this SQLBuilder instance for method chaining
     */
    public This groupBy(final String columnName, final SortDirection direction) {
        groupBy(columnName);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return (This) this;
    }

    /**
     * Adds a GROUP BY clause with a collection of columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("category", "brand");
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy(columns)
     *                 .toSql();
     * }</pre>
     * 
     * @param propOrColumnNames the collection of columns to group by
     * @return this SQLBuilder instance for method chaining
     */
    public This groupBy(final Collection<String> propOrColumnNames) {
        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        int i = 0;
        for (final String columnName : propOrColumnNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(columnName);
        }

        return (This) this;
    }

    /**
     * Adds a GROUP BY clause with a collection of columns and sort direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("category", "brand");
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy(columns, SortDirection.DESC)
     *                 .toSql();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to group by
     * @param direction the direction appended after each column in the GROUP BY clause
     * @return this SQLBuilder instance for method chaining
     */
    public This groupBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        int i = 0;
        for (final String columnName : propOrColumnNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(columnName);
            _sb.append(_SPACE);
            _sb.append(direction.toString());
        }

        return (This) this;
    }

    /**
     * Adds a GROUP BY clause with columns and individual sort directions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("category", SortDirection.ASC);
     * orders.put("brand", SortDirection.DESC);
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy(orders)
     *                 .toSql();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand DESC
     * }</pre>
     * 
     * @param orders map of columns to their sort directions
     * @return this SQLBuilder instance for method chaining
     */
    public This groupBy(final Map<String, SortDirection> orders) {
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

        return (This) this;
    }

    /**
     * Adds a HAVING clause with a string expression.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*) as count")
     *                 .from("products")
     *                 .groupBy("category")
     *                 .having("COUNT(*) > 10")
     *                 .toSql();
     * // Output: SELECT category, COUNT(*) as count FROM products GROUP BY category HAVING COUNT(*) > 10
     * }</pre>
     * 
     * @param expr the HAVING condition expression
     * @return this SQLBuilder instance for method chaining
     */
    public This having(final String expr) {
        checkIfAlreadyCalled(SK.HAVING);

        _sb.append(_SPACE_HAVING_SPACE);

        appendStringExpr(expr, false);

        return (This) this;
    }

    /**
     * Adds a HAVING clause with a condition object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*) as count")
     *                 .from("products")
     *                 .groupBy("category")
     *                 .having(Filters.greaterThan("COUNT(*)", 10))
     *                 .toSql();
     * // Output: SELECT category, COUNT(*) as count FROM products GROUP BY category HAVING COUNT(*) > ?
     * }</pre>
     * 
     * @param cond the HAVING condition
     * @return this SQLBuilder instance for method chaining
     * @see Filters
     */
    public This having(final Condition cond) {
        checkIfAlreadyCalled(SK.HAVING);

        _sb.append(_SPACE_HAVING_SPACE);

        appendCondition(cond);

        return (This) this;
    }

    /**
     * Adds an ORDER BY clause with a single column.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("name")
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY name
     * }</pre>
     * 
     * @param expr the column to order by
     * @return this SQLBuilder instance for method chaining
     */
    public This orderBy(final String expr) {
        checkIfAlreadyCalled(SK.ORDER_BY);

        _sb.append(_SPACE_ORDER_BY_SPACE);

        appendColumnName(expr);

        return (This) this;
    }

    /**
     * Adds an ORDER BY clause with multiple columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("lastName", "firstName")
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY last_name, first_name
     * }</pre>
     * 
     * @param propOrColumnNames the columns to order by
     * @return this SQLBuilder instance for method chaining
     */
    public This orderBy(final String... propOrColumnNames) {
        checkIfAlreadyCalled(SK.ORDER_BY);

        _sb.append(_SPACE_ORDER_BY_SPACE);

        for (int i = 0, len = propOrColumnNames.length; i < len; i++) {
            if (i > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(propOrColumnNames[i]);
        }

        return (This) this;
    }

    /**
     * Adds an ORDER BY clause with a single column and sort direction.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("name", SortDirection.DESC)
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY name DESC
     * }</pre>
     * 
     * @param columnName the column to order by
     * @param direction the sort direction
     * @return this SQLBuilder instance for method chaining
     */
    public This orderBy(final String columnName, final SortDirection direction) {
        orderBy(columnName);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return (This) this;
    }

    /**
     * Adds an ORDER BY clause with a collection of columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("lastName", "firstName");
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy(columns)
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY last_name, first_name
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to order by
     * @return this SQLBuilder instance for method chaining
     */
    public This orderBy(final Collection<String> propOrColumnNames) {
        checkIfAlreadyCalled(SK.ORDER_BY);

        _sb.append(_SPACE_ORDER_BY_SPACE);

        int i = 0;
        for (final String columnName : propOrColumnNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(columnName);
        }

        return (This) this;
    }

    /**
     * Adds an ORDER BY clause with a collection of columns and sort direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("lastName", "firstName");
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy(columns, SortDirection.DESC)
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY last_name DESC, first_name DESC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to order by
     * @param direction the direction appended after each column in the ORDER BY clause
     * @return this SQLBuilder instance for method chaining
     */
    public This orderBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        checkIfAlreadyCalled(SK.ORDER_BY);

        _sb.append(_SPACE_ORDER_BY_SPACE);

        int i = 0;
        for (final String columnName : propOrColumnNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(columnName);

            _sb.append(_SPACE);
            _sb.append(direction.toString());
        }

        return (This) this;
    }

    /**
     * Adds an ORDER BY clause with columns and individual sort directions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("lastName", SortDirection.ASC);
     * orders.put("firstName", SortDirection.DESC);
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy(orders)
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY last_name ASC, first_name DESC
     * }</pre>
     * 
     * @param orders map of columns to their sort directions
     * @return this SQLBuilder instance for method chaining
     */
    public This orderBy(final Map<String, SortDirection> orders) {
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

        return (This) this;
    }

    /**
     * Adds an ORDER BY ASC clause with a single column.
     * Convenience method equivalent to {@code orderBy(expr, SortDirection.ASC)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByAsc("name")
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY name ASC
     * }</pre>
     * 
     * @param expr the column to order by ascending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This orderByAsc(final String expr) {
        return orderBy(expr, SortDirection.ASC);
    }

    /**
     * Adds an ORDER BY ASC clause with multiple columns.
     * Convenience method equivalent to {@code orderBy(propOrColumnNames, SortDirection.ASC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByAsc("lastName", "firstName")
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY last_name ASC, first_name ASC
     * }</pre>
     *
     * @param propOrColumnNames the columns to order by ascending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This orderByAsc(final String... propOrColumnNames) {
        return orderBy(N.asList(propOrColumnNames), SortDirection.ASC);
    }

    /**
     * Adds an ORDER BY ASC clause with a collection of columns.
     * Convenience method equivalent to {@code orderBy(propOrColumnNames, SortDirection.ASC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("lastName", "firstName");
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByAsc(columns)
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY last_name ASC, first_name ASC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to order by ascending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This orderByAsc(final Collection<String> propOrColumnNames) {
        return orderBy(propOrColumnNames, SortDirection.ASC);
    }

    /**
     * Adds an ORDER BY DESC clause with a single column.
     * Convenience method equivalent to {@code orderBy(expr, SortDirection.DESC)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByDesc("createdDate")
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY created_date DESC
     * }</pre>
     * 
     * @param expr the column to order by descending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This orderByDesc(final String expr) {
        return orderBy(expr, SortDirection.DESC);
    }

    /**
     * Adds an ORDER BY DESC clause with multiple columns.
     * Convenience method equivalent to {@code orderBy(propOrColumnNames, SortDirection.DESC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByDesc("createdDate", "id")
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY created_date DESC, id DESC
     * }</pre>
     *
     * @param propOrColumnNames the columns to order by descending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This orderByDesc(final String... propOrColumnNames) {
        return orderBy(N.asList(propOrColumnNames), SortDirection.DESC);
    }

    /**
     * Adds an ORDER BY DESC clause with a collection of columns.
     * Convenience method equivalent to {@code orderBy(propOrColumnNames, SortDirection.DESC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("createdDate", "id");
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByDesc(columns)
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY created_date DESC, id DESC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to order by descending
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This orderByDesc(final Collection<String> propOrColumnNames) {
        return orderBy(propOrColumnNames, SortDirection.DESC);
    }

    /**
     * Adds a LIMIT clause to restrict the number of rows returned.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .limit(10)
     *                 .toSql();
     * // Output: SELECT * FROM users LIMIT 10
     * }</pre>
     * 
     * @param count the maximum number of rows to return
     * @return this SQLBuilder instance for method chaining
     */
    public This limit(final int count) {
        checkIfAlreadyCalled(SK.LIMIT);

        _sb.append(_SPACE_LIMIT_SPACE);

        _sb.append(count);

        return (This) this;
    }

    /**
     * Adds a LIMIT clause with count and offset for pagination.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .limit(10, 20)  // limit 10, offset 20
     *                 .toSql();
     * // Output: SELECT * FROM users LIMIT 10 OFFSET 20
     * }</pre>
     *
     * @param count the maximum number of rows to return (appears as LIMIT in SQL)
     * @param offset the number of rows to skip (appears as OFFSET in SQL)
     * @return this SQLBuilder instance for method chaining
     */
    public This limit(final int count, final int offset) {
        checkIfAlreadyCalled(SK.LIMIT);

        _sb.append(_SPACE_LIMIT_SPACE);

        _sb.append(count);

        _sb.append(_SPACE_OFFSET_SPACE);

        _sb.append(offset);

        return (This) this;
    }

    /**
     * Adds an OFFSET clause to skip a number of rows.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .limit(10)
     *                 .offset(20)
     *                 .toSql();
     * // Output: SELECT * FROM users LIMIT 10 OFFSET 20
     * }</pre>
     * 
     * @param offset the number of rows to skip
     * @return this SQLBuilder instance for method chaining
     */
    public This offset(final int offset) {
        checkIfAlreadyCalled(SK.OFFSET);

        _sb.append(_SPACE_OFFSET_SPACE).append(offset);

        return (This) this;
    }

    /**
     * Adds an OFFSET ROWS clause (SQL Server syntax).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("id")
     *                 .offsetRows(20)
     *                 .fetchNextRows(10)
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
     * }</pre>
     * 
     * @param offset the number of rows to skip
     * @return this SQLBuilder instance for method chaining
     */
    public This offsetRows(final int offset) {
        checkIfAlreadyCalled(SK.OFFSET);

        _sb.append(_SPACE_OFFSET_SPACE).append(offset).append(_SPACE_ROWS);

        return (This) this;
    }

    /**
     * Adds a FETCH NEXT N ROWS ONLY clause (SQL Server syntax).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("id")
     *                 .offsetRows(0)
     *                 .fetchNextRows(10)
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
     * }</pre>
     * 
     * @param rowCount the number of rows to fetch
     * @return this SQLBuilder instance for method chaining
     */
    public This fetchNextRows(final int rowCount) {
        checkIfAlreadyCalled(SK.FETCH_NEXT);

        _sb.append(" FETCH NEXT ").append(rowCount).append(" ROWS ONLY");

        return (This) this;
    }

    /**
     * Adds a FETCH FIRST N ROWS ONLY clause (SQL standard syntax).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("id")
     *                 .fetchFirstRows(10)
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY
     * }</pre>
     * 
     * @param rowCount the number of rows to fetch
     * @return this SQLBuilder instance for method chaining
     */
    public This fetchFirstRows(final int rowCount) {
        checkIfAlreadyCalled(SK.FETCH_FIRST);

        _sb.append(" FETCH FIRST ").append(rowCount).append(" ROWS ONLY");

        return (This) this;
    }

    protected void checkIfAlreadyCalled(final String op) {
        if (!calledOpSet.add(op)) {
            throw new IllegalStateException("'" + op + "' has already been set and cannot be set again");
        }
    }

    /**
     * Appends a {@code Criteria} or {@code Where} condition to the SQL statement.
     * Automatically adds WHERE clause if not already present.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .append(Filters.and(Filters.greaterThan("age", 18), Filters.lessThan("age", 65)))
     *                 .toSql();
     * // Output: SELECT * FROM users WHERE ((age > ?) AND (age < ?))
     * }</pre>
     * 
     * @param cond the condition to append
     * @return this SQLBuilder instance for method chaining
     * @see Filters
     */
    @Beta
    public This append(final Condition cond) {
        init(true);

        if (cond instanceof final Criteria criteria) {
            final Collection<Join> joins = criteria.getJoins();

            // appendPreselect(criteria.distinct());

            if (N.notEmpty(joins)) {
                for (final Join join : joins) {
                    _sb.append(_SPACE).append(join.operator()).append(_SPACE);

                    if (join.getJoinEntities().size() == 1) {
                        _sb.append(join.getJoinEntities().get(0));
                    } else {
                        _sb.append(SK._PARENTHESIS_L);
                        int idx = 0;

                        for (final String joinTableName : join.getJoinEntities()) {
                            if (idx++ > 0) {
                                _sb.append(_COMMA_SPACE);
                            }

                            _sb.append(joinTableName);
                        }

                        _sb.append(SK._PARENTHESIS_R);
                    }

                    if (join.getCondition() != null) {
                        appendCondition(join.getCondition());
                    }
                }
            }

            final Clause where = criteria.getWhere();

            if ((where != null)) {
                _sb.append(_SPACE_WHERE_SPACE);
                appendCondition(where.getCondition());
            }

            final Clause groupBy = criteria.getGroupBy();

            if (groupBy != null) {
                _sb.append(_SPACE_GROUP_BY_SPACE);
                appendCondition(groupBy.getCondition());
            }

            final Clause having = criteria.getHaving();

            if (having != null) {
                _sb.append(_SPACE_HAVING_SPACE);
                appendCondition(having.getCondition());
            }

            final List<Clause> aggregations = criteria.getSetOperations();

            if (N.notEmpty(aggregations)) {
                for (final Clause aggregation : aggregations) {
                    _sb.append(_SPACE).append(aggregation.operator()).append(_SPACE);
                    appendCondition(aggregation.getCondition());
                }
            }

            final Clause orderBy = criteria.getOrderBy();

            if (orderBy != null) {
                _sb.append(_SPACE_ORDER_BY_SPACE);
                appendCondition(orderBy.getCondition());
            }

            final Limit limit = criteria.getLimit();

            if (limit != null) {
                if (Strings.isNotEmpty(limit.getExpression())) {
                    _sb.append(_SPACE).append(limit.getExpression());
                } else if (limit.getOffset() > 0) {
                    limit(limit.getCount(), limit.getOffset());
                } else {
                    limit(limit.getCount());
                }
            }
        } else if (cond instanceof Clause) {
            _sb.append(_SPACE).append(cond.operator()).append(_SPACE);
            appendCondition(((Clause) cond).getCondition());
        } else {
            if (!_isForConditionOnly) {
                _sb.append(_SPACE_WHERE_SPACE);
            }

            appendCondition(cond);
        }

        return (This) this;
    }

    /**
     * Appends a string expression to the SQL statement.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .append(" FOR UPDATE")
     *                 .toSql();
     * // Output: SELECT * FROM users FOR UPDATE
     * }</pre>
     * 
     * @param expr the expression to append
     * @return this SQLBuilder instance for method chaining
     */
    public This append(final String expr) {
        _sb.append(expr);

        return (This) this;
    }

    /**
     * Conditionally appends a condition to the SQL statement.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean includeAgeFilter = true;
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .appendIf(includeAgeFilter, Filters.greaterThan("age", 18))
     *                 .toSql();
     * // Output: SELECT * FROM users WHERE age > ?
     * }</pre>
     * 
     * @param condition if true, the condition will be appended
     * @param cond the condition to append
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This appendIf(final boolean condition, final Condition cond) {
        if (condition) {
            append(cond);
        }

        return (This) this;
    }

    /**
     * Conditionally appends a string expression to the SQL statement.
     * Useful for building dynamic SQL based on runtime conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean includeForUpdate = true;
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where(Filters.equal("id", 1))
     *                 .appendIf(includeForUpdate, " FOR UPDATE")
     *                 .toSql();
     * // Output: SELECT * FROM users WHERE id = ? FOR UPDATE
     * }</pre>
     *
     * @param condition if true, the expression will be appended
     * @param expr the expression to append
     * @return this SQLBuilder instance for method chaining
     */
    public This appendIf(final boolean condition, final String expr) {
        if (condition) {
            append(expr);
        }

        return (This) this;
    }

    /**
     * Conditionally executes an append operation using a consumer function.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean complexFilter = true;
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .appendIf(complexFilter, builder -> 
     *                     builder.where(Filters.greaterThan("age", 18))
     *                            .orderBy("name"))
     *                 .toSql();
     * }</pre>
     * 
     * @param condition if true, the consumer will be executed
     * @param append the consumer function to execute
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This appendIf(final boolean condition, final java.util.function.Consumer<? super This> append) {
        if (condition) {
            append.accept((This) this);
        }

        return (This) this;
    }

    /**
     * Conditionally appends one of two conditions based on a boolean value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean isActive = true;
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .appendIfOrElse(isActive, 
     *                     Filters.equal("status", "active"),
     *                     Filters.equal("status", "inactive"))
     *                 .toSql();
     * // Output: SELECT * FROM users WHERE status = ?
     * }</pre>
     * 
     * @param condition if true, append condToAppendForTrue; otherwise append condToAppendForFalse
     * @param condToAppendForTrue the condition to append if condition is true
     * @param condToAppendForFalse the condition to append if condition is false
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This appendIfOrElse(final boolean condition, final Condition condToAppendForTrue, final Condition condToAppendForFalse) {
        if (condition) {
            append(condToAppendForTrue);
        } else {
            append(condToAppendForFalse);
        }

        return (This) this;
    }

    /**
     * Conditionally appends one of two string expressions based on a boolean value.
     * Useful for building dynamic SQL with alternative clauses.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean sortAscending = true;
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .appendIfOrElse(sortAscending,
     *                     " ORDER BY name ASC",
     *                     " ORDER BY name DESC")
     *                 .toSql();
     * // Output: SELECT * FROM users ORDER BY name ASC
     * }</pre>
     *
     * @param condition if true, append exprToAppendForTrue; otherwise append exprToAppendForFalse
     * @param exprToAppendForTrue the expression to append if condition is true
     * @param exprToAppendForFalse the expression to append if condition is false
     * @return this SQLBuilder instance for method chaining
     */
    @Beta
    public This appendIfOrElse(final boolean condition, final String exprToAppendForTrue, final String exprToAppendForFalse) {
        if (condition) {
            append(exprToAppendForTrue);
        } else {
            append(exprToAppendForFalse);
        }

        return (This) this;
    }

    /**
     * Adds a UNION clause with another SQL query.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLBuilder query1 = PSC.select("id", "name").from("users");
     * SQLBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.union(query2).toSql();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     * 
     * @param sqlBuilder the SQL builder containing the query to union
     * @return this SQLBuilder instance for method chaining
     */
    public This union(final This sqlBuilder) {
        final String sql = sqlBuilder.toSql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return union(sql);
    }

    /**
     * Adds a UNION clause with a SQL query string.
     * UNION combines result sets from two queries and removes duplicates.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .union("SELECT id, name FROM customers")
     *                 .toSql();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     *
     * @param query the SQL query to union
     * @return this SQLBuilder instance for method chaining
     */
    public This union(final String query) {
        return union(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for UNION operation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .union("id", "name")
     *                 .from("customers")
     *                 .toSql();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     * 
     * @param propOrColumnNames the columns for the union query
     * @return this SQLBuilder instance for method chaining
     */
    public This union(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_UNION_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return (This) this;
    }

    /**
     * Starts a new SELECT query for UNION operation with a collection of columns.
     * This method prepares the builder to specify a second SELECT query after UNION.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("id", "name");
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .union(columns)
     *                 .from("customers")
     *                 .toSql();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the union query
     * @return this SQLBuilder instance for method chaining
     */
    public This union(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_UNION_SPACE);

        return (This) this;
    }

    /**
     * Adds a UNION ALL clause with another SQL query.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLBuilder query1 = PSC.select("id", "name").from("users");
     * SQLBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.unionAll(query2).toSql();
     * // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
     * }</pre>
     * 
     * @param sqlBuilder the SQL builder containing the query to union all
     * @return this SQLBuilder instance for method chaining
     */
    public This unionAll(final This sqlBuilder) {
        final String sql = sqlBuilder.toSql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return unionAll(sql);
    }

    /**
     * Adds a UNION ALL clause with a SQL query string.
     * UNION ALL combines result sets from two queries and keeps all duplicates.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .unionAll("SELECT id, name FROM customers")
     *                 .toSql();
     * // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
     * }</pre>
     *
     * @param query the SQL query to union all
     * @return this SQLBuilder instance for method chaining
     */
    public This unionAll(final String query) {
        return unionAll(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for UNION ALL operation.
     * This method prepares the builder to specify a second SELECT query after UNION ALL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .unionAll("id", "name")
     *                 .from("customers")
     *                 .toSql();
     * // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
     * }</pre>
     *
     * @param propOrColumnNames the columns for the union all query
     * @return this SQLBuilder instance for method chaining
     */
    public This unionAll(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_UNION_ALL_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return (This) this;
    }

    /**
     * Starts a new SELECT query for UNION ALL operation with a collection of columns.
     * This method prepares the builder to specify a second SELECT query after UNION ALL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("id", "name");
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .unionAll(columns)
     *                 .from("customers")
     *                 .toSql();
     * // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the union all query
     * @return this SQLBuilder instance for method chaining
     */
    public This unionAll(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_UNION_ALL_SPACE);

        return (This) this;
    }

    /**
     * Adds an INTERSECT clause with another SQL query.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLBuilder query1 = PSC.select("id", "name").from("users");
     * SQLBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.intersect(query2).toSql();
     * // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM customers
     * }</pre>
     * 
     * @param sqlBuilder the SQL builder containing the query to intersect
     * @return this SQLBuilder instance for method chaining
     */
    public This intersect(final This sqlBuilder) {
        final String sql = sqlBuilder.toSql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return intersect(sql);
    }

    /**
     * Adds an INTERSECT clause with a SQL query string.
     * INTERSECT returns only rows that appear in both result sets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .intersect("SELECT id, name FROM premium_users")
     *                 .toSql();
     * // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM premium_users
     * }</pre>
     *
     * @param query the SQL query to intersect
     * @return this SQLBuilder instance for method chaining
     */
    public This intersect(final String query) {
        return intersect(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for INTERSECT operation.
     * This method prepares the builder to specify a second SELECT query after INTERSECT.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .intersect("id", "name")
     *                 .from("premium_users")
     *                 .toSql();
     * // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM premium_users
     * }</pre>
     *
     * @param propOrColumnNames the columns for the intersect query
     * @return this SQLBuilder instance for method chaining
     */
    public This intersect(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_INTERSECT_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return (This) this;
    }

    /**
     * Starts a new SELECT query for INTERSECT operation with a collection of columns.
     * This method prepares the builder to specify a second SELECT query after INTERSECT.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("id", "name");
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .intersect(columns)
     *                 .from("premium_users")
     *                 .toSql();
     * // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM premium_users
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the intersect query
     * @return this SQLBuilder instance for method chaining
     */
    public This intersect(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_INTERSECT_SPACE);

        return (This) this;
    }

    /**
     * Adds an EXCEPT clause with another SQL query.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLBuilder query1 = PSC.select("id", "name").from("users");
     * SQLBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.except(query2).toSql();
     * // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM customers
     * }</pre>
     * 
     * @param sqlBuilder the SQL builder containing the query to except
     * @return this SQLBuilder instance for method chaining
     */
    public This except(final This sqlBuilder) {
        final String sql = sqlBuilder.toSql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return except(sql);
    }

    /**
     * Adds an EXCEPT clause with a SQL query string.
     * EXCEPT returns rows from the first query that don't appear in the second query.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .except("SELECT id, name FROM inactive_users")
     *                 .toSql();
     * // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param query the SQL query to except
     * @return this SQLBuilder instance for method chaining
     */
    public This except(final String query) {
        return except(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for EXCEPT operation.
     * This method prepares the builder to specify a second SELECT query after EXCEPT.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .except("id", "name")
     *                 .from("inactive_users")
     *                 .toSql();
     * // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param propOrColumnNames the columns for the except query
     * @return this SQLBuilder instance for method chaining
     */
    public This except(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_EXCEPT_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return (This) this;
    }

    /**
     * Starts a new SELECT query for EXCEPT operation with a collection of columns.
     * This method prepares the builder to specify a second SELECT query after EXCEPT.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("id", "name");
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .except(columns)
     *                 .from("inactive_users")
     *                 .toSql();
     * // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the except query
     * @return this SQLBuilder instance for method chaining
     */
    public This except(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_EXCEPT_SPACE);

        return (This) this;
    }

    /**
     * Adds a MINUS clause with another SQL query (Oracle syntax).
     * MINUS is Oracle's equivalent to EXCEPT - returns rows from the first query that don't appear in the second.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLBuilder query1 = PSC.select("id", "name").from("users");
     * SQLBuilder query2 = PSC.select("id", "name").from("inactive_users");
     * String sql = query1.minus(query2).toSql();
     * // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param sqlBuilder the SQL builder containing the query to minus
     * @return this SQLBuilder instance for method chaining
     */
    public This minus(final This sqlBuilder) {
        final String sql = sqlBuilder.toSql();

        if (N.notEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return minus(sql);
    }

    /**
     * Adds a MINUS clause with a SQL query string (Oracle syntax).
     * MINUS is Oracle's equivalent to EXCEPT - returns rows from the first query that don't appear in the second.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .minus("SELECT id, name FROM inactive_users")
     *                 .toSql();
     * // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param query the SQL query to minus
     * @return this SQLBuilder instance for method chaining
     */
    public This minus(final String query) {
        return minus(N.asArray(query));
    }

    /**
     * Starts a new SELECT query for MINUS operation (Oracle syntax).
     * This method prepares the builder to specify a second SELECT query after MINUS.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .minus("id", "name")
     *                 .from("inactive_users")
     *                 .toSql();
     * // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param propOrColumnNames the columns for the minus query
     * @return this SQLBuilder instance for method chaining
     */
    public This minus(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = Array.asList(propOrColumnNames);
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_EXCEPT_MINUS_SPACE);

        // it's subquery
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            _propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return (This) this;
    }

    /**
     * Starts a new SELECT query for MINUS operation with a collection of columns (Oracle syntax).
     * This method prepares the builder to specify a second SELECT query after MINUS.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("id", "name");
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .minus(columns)
     *                 .from("inactive_users")
     *                 .toSql();
     * // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the minus query
     * @return this SQLBuilder instance for method chaining
     */
    public This minus(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNames;
        _propOrColumnNameAliases = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;

        _sb.append(_SPACE_EXCEPT_MINUS_SPACE);

        return (This) this;
    }

    /**
     * Adds a FOR UPDATE clause to lock selected rows.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where(Filters.equal("id", 1))
     *                 .forUpdate()
     *                 .toSql();
     * // Output: SELECT * FROM users WHERE id = ? FOR UPDATE
     * }</pre>
     * 
     * @return this SQLBuilder instance for method chaining
     */
    public This forUpdate() {
        _sb.append(_SPACE_FOR_UPDATE);

        return (This) this;
    }

    /**
     * Sets columns for UPDATE operation with a single expression.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update("users")
     *                 .set("name = 'John'")
     *                 .where(Filters.equal("id", 1))
     *                 .toSql();
     * // Output: UPDATE users SET name = 'John' WHERE id = ?
     * }</pre>
     * 
     * @param expr the SET expression
     * @return this SQLBuilder instance for method chaining
     */
    public This set(final String expr) {
        return set(Array.asList(expr));
    }

    /**
     * Sets columns for UPDATE operation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update("users")
     *                 .set("firstName", "lastName", "email")
     *                 .where(Filters.equal("id", 1))
     *                 .toSql();
     * // Output: UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?
     * }</pre>
     * 
     * @param propOrColumnNames the columns to update
     * @return this SQLBuilder instance for method chaining
     */
    public This set(final String... propOrColumnNames) {
        return set(Array.asList(propOrColumnNames));
    }

    /**
     * Sets columns for UPDATE operation with a collection of property or column names.
     * Generates parameterized placeholders ({@code ?}, {@code :name}, or {@code #{name}}) based on the SQL policy.
     * If a column name already contains an {@code =} sign, it is treated as a raw SET expression and no placeholder is appended.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("firstName", "lastName", "email");
     * String sql = PSC.update("users")
     *                 .set(columns)
     *                 .where("id = ?")
     *                 .toSql();
     * // Output: UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to update
     * @return this SQLBuilder instance for method chaining
     */
    public This set(final Collection<String> propOrColumnNames) {
        init(false);

        switch (_sqlPolicy) {
            case RAW_SQL:
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
                throw new UnsupportedOperationException("SQL policy not supported: " + _sqlPolicy);
        }

        _propOrColumnNames = null;

        return (This) this;
    }

    /**
     * Sets columns and values for UPDATE operation using a map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> values = new HashMap<>();
     * values.put("firstName", "John");
     * values.put("lastName", "Doe");
     * String sql = PSC.update("users")
     *                 .set(values)
     *                 .where(Filters.equal("id", 1))
     *                 .toSql();
     * // Output: UPDATE users SET first_name = ?, last_name = ? WHERE id = ?
     * }</pre>
     * 
     * @param props map of column names to values
     * @return this SQLBuilder instance for method chaining
     */
    public This set(final Map<String, Object> props) {
        init(false);

        switch (_sqlPolicy) {
            case RAW_SQL: {
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

            case PARAMETERIZED_SQL: {
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
                throw new UnsupportedOperationException("SQL policy not supported: " + _sqlPolicy);
        }

        _propOrColumnNames = null;

        return (This) this;
    }

    /**
     * Sets properties to update from an entity object.
     * Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update("account")
     *                 .set(accountEntity)
     *                 .where(Filters.equal("id", 1))
     *                 .toSql();
     * }</pre>
     * 
     * @param entity the entity object containing properties to set
     * @return this SQLBuilder instance for method chaining
     */
    public This set(final Object entity) {
        return set(entity, null);
    }

    /**
     * Sets properties to update from an entity object, excluding specified properties.
     * Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("createdDate", "version");
     * String sql = PSC.update("account")
     *                 .set(accountEntity, excluded)
     *                 .where(Filters.equal("id", 1))
     *                 .toSql();
     * }</pre>
     * 
     * @param entity the entity object containing properties to set
     * @param excludedPropNames properties to exclude from the update
     * @return this SQLBuilder instance for method chaining
     */
    public This set(final Object entity, final Set<String> excludedPropNames) {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update("account")
     *                 .set(Account.class)
     *                 .where(Filters.equal("id", 1))
     *                 .toSql();
     * }</pre>
     *
     * @param entityClass the entity class to get properties from
     * @return this SQLBuilder instance for method chaining
     */
    public This set(final Class<?> entityClass) {
        setEntityClass(entityClass);

        return set(entityClass, null);
    }

    /**
     * Sets updatable properties from an entity class for UPDATE operation, excluding specified properties.
     * Properties marked with @NonUpdatable, @ReadOnly, @ReadOnlyId, or @Transient annotations are automatically excluded.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("lastModified");
     * String sql = PSC.update("account")
     *                 .set(Account.class, excluded)
     *                 .where(Filters.equal("id", 1))
     *                 .toSql();
     * }</pre>
     *
     * @param entityClass the entity class to get properties from
     * @param excludedPropNames additional properties to exclude from the update
     * @return this SQLBuilder instance for method chaining
     */
    public This set(final Class<?> entityClass, final Set<String> excludedPropNames) {
        setEntityClass(entityClass);

        return set(QueryUtil.getUpdatePropNames(entityClass, excludedPropNames));
    }

    /**
     * Returns the list of parameter values for the generated SQL.
     * For parameterized SQL (using ?), this list contains the actual values in order.
     * For named SQL, this list contains the values corresponding to named parameters
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SQLBuilder builder = PSC.select("*")
     *                        .from("account")
     *                        .where(Filters.and(
     *                            Filters.equal("name", "John"),
     *                            Filters.greaterThan("age", 25)
     *                        ));
     * List<Object> params = builder.parameters();
     * // params contains: ["John", 25]
     * }</pre>
     *
     * @return an unmodifiable view of the parameter values
     */
    protected List<Object> parameters() {
        return Collections.unmodifiableList(_parameters);
    }

    /**
     * Generates the final SQL query string and releases resources.
     * This method should be called only once. After calling this method, the SQLBuilder instance cannot be used again.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where(Filters.equal("status", "ACTIVE"))
     *                 .toSql();
     * // sql contains: "SELECT * FROM users WHERE status = ?"
     * }</pre>
     *
     * @return the generated SQL query string
     */
    public String toSql() {
        if (_sb == null) {
            throw new IllegalStateException("SQLBuilder is closed and cannot be reused after toSql()/toSqlAndParameters() was called");
        }

        String sql = null;

        try {
            init(true);

            sql = !_sb.isEmpty() && _sb.charAt(0) == ' ' ? _sb.substring(1) : _sb.toString();
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
     * Generates both the SQL string and its parameters as a pair.
     * This method finalizes the SQL builder and releases resources. The builder cannot be used after calling this method.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SP sqlPair = PSC.select("*")
     *                 .from("account")
     *                 .where(Filters.equal("status", "ACTIVE"))
     *                 .toSqlAndParameters();
     * // sqlPair.sql contains: "SELECT * FROM account WHERE status = ?"
     * // sqlPair.parameters contains: ["ACTIVE"]
     * }</pre>
     *
     * @return an SP (SQL-Parameters) pair containing the SQL string and parameter list
     */
    public SP toSqlAndParameters() {
        final String sql = toSql();

        return new SP(sql, _parameters);
    }

    /**
     * @deprecated Use {@link #toSqlAndParameters()} instead.
     */
    @Deprecated
    public SP build() {
        return toSqlAndParameters();
    }

    //    /**
    //     * Generates both the SQL string and its parameters as a pair.
    //     * This method finalizes the SQL builder and releases resources. The builder cannot be used after calling this method.
    //     * 
    //     * <p><b>Usage Examples:</b></p>
    //     * <pre>{@code
    //     * // Example usage:
    //     * SP sqlPair = PSC.select("*")
    //     *                 .from("account")
    //     *                 .where(Filters.equal("status", "ACTIVE"))
    //     *                 .pair();
    //     * // sqlPair.sql contains: "SELECT * FROM account WHERE status = ?"
    //     * // sqlPair.parameters contains: ["ACTIVE"]
    //     * }</pre>
    //     * @deprecated Use {@link #build()} instead
    //     *
    //     * @return an SP (SQL-Parameters) pair containing the SQL string and parameter list
    //     */
    //    public SP pair() {
    //        return build();
    //    }

    /**
     * Applies a function to the SQL-Parameters pair and returns the result.
     * This is useful for executing the SQL directly with a data access framework
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Account> accounts = PSC.select("*")
     *     .from("account")
     *     .where(Filters.equal("status", "ACTIVE"))
     *     .apply(sp -> jdbcTemplate.query(sp.sql, sp.parameters, accountRowMapper));
     * }</pre>
     *
     * @param <T> the return type of the function
     * @param <E> the exception type that may be thrown
     * @param func the function to apply to the SP pair
     * @return the result of applying the function
     * @throws E if the function throws an exception
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.Function<? super SP, T, E> func) throws E {
        return func.apply(toSqlAndParameters());
    }

    /**
     * Applies a bi-function to the SQL string and parameters separately and returns the result.
     * This is useful for executing the SQL directly with a data access framework that takes SQL and parameters separately.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int count = PSC.update("account")
     *     .set("status", "INACTIVE")
     *     .where(Filters.lessThan("lastLogin", oneYearAgo))
     *     .apply((sql, params) -> jdbcTemplate.update(sql, params.toArray()));
     * }</pre>
     *
     * @param <T> the return type of the function
     * @param <E> the exception type that may be thrown
     * @param func the bi-function to apply to the SQL and parameters
     * @return the result of applying the function
     * @throws E if the function throws an exception
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.BiFunction<? super String, ? super List<Object>, T, E> func) throws E {
        final SP sP = toSqlAndParameters();

        return func.apply(sP.sql, sP.parameters);
    }

    /**
     * Accepts a consumer for the SQL-Parameters pair.
     * This is useful for executing the SQL with a data access framework when no return value is needed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PSC.insert("name", "email", "status")
     *    .into("account")
     *    .accept(sp -> jdbcTemplate.update(sp.sql, sp.parameters.toArray()));
     * }</pre>
     *
     * @param <E> the exception type that may be thrown
     * @param consumer the consumer to accept the SP pair
     * @throws E if the consumer throws an exception
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.Consumer<? super SP, E> consumer) throws E {
        consumer.accept(toSqlAndParameters());
    }

    /**
     * Accepts a bi-consumer for the SQL string and parameters separately.
     * This is useful for executing the SQL with a data access framework when no return value is needed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PSC.deleteFrom("account")
     *    .where(Filters.equal("status", "DELETED"))
     *    .accept((sql, params) -> {
     *        logger.info("Executing: {} with params: {}", sql, params);
     *        jdbcTemplate.update(sql, params.toArray());
     *    });
     * }</pre>
     *
     * @param <E> the exception type that may be thrown
     * @param consumer the bi-consumer to accept the SQL and parameters
     * @throws E if the consumer throws an exception
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.BiConsumer<? super String, ? super List<Object>, E> consumer) throws E {
        final SP sP = toSqlAndParameters();

        consumer.accept(sP.sql, sP.parameters);
    }

    /**
     * Prints the generated SQL to standard output.
     * This is useful for debugging and development.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PSC.select("*")
     *    .from("account")
     *    .where(Filters.between("age", 18, 65))
     *    .println();
     * // Prints: SELECT * FROM account WHERE age BETWEEN ? AND ?
     * }</pre>
     */
    public void println() {
        N.println(toSql());
    }

    //    4. toString() Finalizes the Builder (Design Issue)
    //
    //    - Problem: AbstractQueryBuilder.toString() calls internal finalization
    //     logic (generateSql()), making it destructive. Calling toString() for
    //    debugging can alter builder state.
    //    - Suggestion: Make toString() read-only (return a preview without
    //    mutating state), or rename the finalizing method to build() / toSql()
    //    and have toString() delegate without side effects.
    //    /**
    //     * Returns the generated SQL string representation of this builder.
    //     * Note: This method finalizes the builder and it cannot be used afterwards.
    //     *
    //     * @return the generated SQL string
    //     */
    //    @Override
    //    public String toString() {
    //        return toSql();
    //    }

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
    //        final SP sp = this.build();
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
    //        final SP sp = this.build();
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
    //    public <Q extends com.landawn.abacus.jdbc.AbstractQuery> Q toPreparedQueryForLargeResult(final javax.sql.DataSource dataSource) throws SQLException {
    //        final SP sp = this.build();
    //
    //        final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql()
    //                ? com.landawn.abacus.jdbc.JdbcUtil.prepareNamedQueryForLargeResult(dataSource, sp.sql)
    //                : com.landawn.abacus.jdbc.JdbcUtil.prepareQueryForLargeResult(dataSource, sp.sql);
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
    //    public <Q extends com.landawn.abacus.jdbc.AbstractQuery> Q toPreparedQueryForLargeResult(final java.sql.Connection conn) throws SQLException {
    //        final SP sp = this.build();
    //
    //        final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? com.landawn.abacus.jdbc.JdbcUtil.prepareNamedQueryForLargeResult(conn, sp.sql)
    //                : com.landawn.abacus.jdbc.JdbcUtil.prepareQueryForLargeResult(conn, sp.sql);
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
    //        final SP sp = this.build();
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
    //        final SP sp = this.build();
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
    //        final SP sp = this.build();
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
    //        final SP sp = this.build();
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
    //    public <R> R executeQueryForLargeResult(final javax.sql.DataSource dataSource, final Throwables.Function<com.landawn.abacus.jdbc.AbstractQuery, R, SQLException> queryCall)
    //            throws SQLException {
    //        final SP sp = this.build();
    //
    //        try (final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? JdbcUtil.prepareNamedQueryForLargeResult(dataSource, sp.sql)
    //                : JdbcUtil.prepareQueryForLargeResult(dataSource, sp.sql)) {
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
    //    public <R> R executeQueryForLargeResult(final java.sql.Connection conn, final Throwables.Function<com.landawn.abacus.jdbc.AbstractQuery, R, SQLException> queryOrUpdateCall)
    //            throws SQLException {
    //        final SP sp = this.build();
    //
    //        try (final com.landawn.abacus.jdbc.AbstractQuery preparedQuery = isNamedSql() ? JdbcUtil.prepareNamedQueryForLargeResult(conn, sp.sql)
    //                : JdbcUtil.prepareQueryForLargeResult(conn, sp.sql)) {
    //
    //            if (N.notEmpty(sp.parameters)) {
    //                preparedQuery.setParameters(sp.parameters);
    //            }
    //
    //            return queryOrUpdateCall.apply(preparedQuery);
    //        }
    //    }

    /**
     * Initializes the query builder with the appropriate SQL operation type and parameters.
     * 
     * <p>This method sets up the builder's internal state based on the operation type and whether
     * this is an update operation that affects row data.</p>
     *
     * @param setForUpdate whether this operation will update data (affects entity field filtering)
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
            throw new IllegalStateException("from() method must be called before building query for operation: " + _op);
        }
    }

    protected void setEntityClass(final Class<?> entityClass) {
        _entityClass = entityClass;

        if (Beans.isBeanClass(entityClass)) {
            _entityInfo = ParserUtil.getBeanInfo(entityClass);
            _propColumnNameMap = prop2ColumnNameMap(entityClass, _namingPolicy);
        } else {
            _entityInfo = null;
            _propColumnNameMap = null;
        }
    }

    /**
     * Sets the parameter for raw SQL (inlines the value directly into the SQL string).
     *
     * @param propValue the new parameter for raw SQL
     */
    protected void setParameterForRawSQL(final Object propValue) {
        if (Filters.QME.equals(propValue)) {
            _sb.append(SK._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendConditionAsParameter((Condition) propValue);
        } else {
            _sb.append(Expression.normalize(propValue));
        }
    }

    /**
     * Sets the parameter for parameterized SQL (uses '?' placeholder and adds value to parameter list).
     *
     * @param propValue the new parameter for parameterized SQL
     */
    protected void setParameterForSQL(final Object propValue) {
        if (Filters.QME.equals(propValue)) {
            _sb.append(SK._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendConditionAsParameter((Condition) propValue);
        } else {
            _sb.append(SK._QUESTION_MARK);

            _parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter for named SQL.
     *
     * @param propName the property or parameter name for the named SQL placeholder
     * @param propValue the value to bind to the named parameter
     */
    protected void setParameterForNamedSQL(final String propName, final Object propValue) {
        if (Filters.QME.equals(propValue)) {
            _handlerForNamedParameter.accept(_sb, propName);
        } else if (propValue instanceof Condition) {
            appendConditionAsParameter((Condition) propValue);
        } else {
            _handlerForNamedParameter.accept(_sb, propName);

            _parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter for ibatis named SQL.
     *
     * @param propName the property or parameter name for the ibatis named SQL placeholder
     * @param propValue the value to bind to the ibatis named parameter
     */
    protected void setParameterForIbatisNamedSQL(final String propName, final Object propValue) {
        if (Filters.QME.equals(propValue)) {
            _sb.append("#{");
            _sb.append(propName);
            _sb.append('}');
        } else if (propValue instanceof Condition) {
            appendConditionAsParameter((Condition) propValue);
        } else {
            _sb.append("#{");
            _sb.append(propName);
            _sb.append('}');

            _parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter based on the current SQL policy.
     *
     * @param propName the property or parameter name 
     * @param propValue the value to bind to the parameter
     */
    protected void setParameter(final String propName, final Object propValue) {
        switch (_sqlPolicy) {
            case RAW_SQL: {
                setParameterForRawSQL(propValue);

                break;
            }

            case PARAMETERIZED_SQL: {
                setParameterForSQL(propValue);

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
                throw new UnsupportedOperationException("SQL policy not supported: " + _sqlPolicy);
        }
    }

    /**
     * Appends the column names and values for an INSERT operation.
     *
     * @param props a map of property names to values to be inserted
     */
    protected void appendInsertProps(final Map<String, Object> props) {
        appendInsertProps(props, props.keySet(), -1);
    }

    /**
     * Appends the values for an INSERT operation in the specified column order.
     *
     * @param props a map of property names to values to be inserted
     * @param propNames the ordered column names
     */
    protected void appendInsertProps(final Map<String, Object> props, final Collection<String> propNames) {
        appendInsertProps(props, propNames, -1);
    }

    /**
     * Appends the values for an INSERT operation in the specified column order.
     *
     * @param props a map of property names to values to be inserted
     * @param propNames the ordered column names
     * @param rowIndex zero-based row index in batch insert mode; negative for single-row insert
     */
    protected void appendInsertProps(final Map<String, Object> props, final Collection<String> propNames, final int rowIndex) {
        switch (_sqlPolicy) {
            case RAW_SQL: {
                int i = 0;
                for (final String propName : propNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    final Object propValue = props.get(propName);
                    setParameterForRawSQL(propValue);
                }

                break;
            }

            case PARAMETERIZED_SQL: {
                int i = 0;
                for (final String propName : propNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    final Object propValue = props.get(propName);
                    setParameterForSQL(propValue);
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                for (final String propName : propNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    final String namedPropName = rowIndex >= 0 ? propName + "_" + rowIndex : propName;
                    setParameterForNamedSQL(namedPropName, props.get(propName));
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                for (final String propName : propNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    final String namedPropName = rowIndex >= 0 ? propName + "_" + rowIndex : propName;
                    setParameterForIbatisNamedSQL(namedPropName, props.get(propName));
                }

                break;
            }

            default:
                throw new UnsupportedOperationException("SQL policy not supported: " + _sqlPolicy);
        }
    }

    protected abstract void appendCondition(final Condition cond);

    protected void appendConditionAsParameter(final Condition cond) {
        if (cond instanceof SubQuery) {
            _sb.append(SK._PARENTHESIS_L);
            appendCondition(cond);
            _sb.append(SK._PARENTHESIS_R);
        } else {
            appendCondition(cond);
        }
    }

    protected void appendStringExpr(final String expr, final boolean isFromAppendColumn) {
        // TODO performance improvement.

        if (expr.length() < 16) {
            final boolean matched = QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher(expr).find();

            if (matched) {
                if (isFromAppendColumn) {
                    _sb.append(normalizeColumnName(expr, _namingPolicy));
                } else {
                    _sb.append(normalizeColumnName(_propColumnNameMap, expr));
                }

                return;
            }
        }

        final List<String> words = SQLParser.parse(expr);

        String word = null;
        for (int i = 0, len = words.size(); i < len; i++) {
            word = words.get(i);

            if (word.isEmpty() || !Strings.isAsciiAlpha(word.charAt(0)) || SQLParser.isFunctionName(words, len, i)) {
                _sb.append(word);
            } else {
                _sb.append(normalizeColumnName(_propColumnNameMap, word));
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
                    _sb.append(SK._DOUBLE_QUOTE);
                }

                if (withClassAlias) {
                    _sb.append(classAlias).append(SK._PERIOD);
                }

                _sb.append(Strings.isNotEmpty(propAlias) ? propAlias : propName);

                if (quotePropAlias) {
                    _sb.append(SK._DOUBLE_QUOTE);
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

                    final Tuple2<String, Boolean> subTp = subPropColumnNameMap.get(subPropName);
                    _sb.append(propEntityTableAliasOrName)
                            .append(SK._PERIOD)
                            .append(subTp != null ? subTp._1 : normalizeColumnName(subPropName, _namingPolicy));

                    if (isForSelect) {
                        _sb.append(_SPACE_AS_SPACE);

                        if (quotePropAlias) {
                            _sb.append(SK._DOUBLE_QUOTE);
                        }

                        _sb.append(propInfo.name).append(SK._PERIOD).append(subPropName);

                        if (quotePropAlias) {
                            _sb.append(SK._DOUBLE_QUOTE);
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
                                _sb.append(SK._DOUBLE_QUOTE);
                            }

                            if (withClassAlias) {
                                _sb.append(classAlias).append(SK._PERIOD);
                            }

                            _sb.append(Strings.isNotEmpty(propAlias) ? propAlias : propName);

                            if (quotePropAlias) {
                                _sb.append(SK._DOUBLE_QUOTE);
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
            if (isForSelect && (withClassAlias || propAlias.length() > _sb.length()
                    || (_sb.length() - propAlias.length() - 1 >= 0 && _sb.charAt(_sb.length() - propAlias.length() - 1) != _SPACE)
                    || _sb.indexOf(propAlias, _sb.length() - propAlias.length()) < 0 || ((idx = propAlias.indexOf(SK._PERIOD)) > 0
                            && (Strings.isEmpty(tableAlias) || tableAlias.length() != idx || !propAlias.startsWith(tableAlias))))) {
                _sb.append(_SPACE_AS_SPACE);

                if (quotePropAlias) {
                    _sb.append(SK._DOUBLE_QUOTE);
                }

                if (withClassAlias) {
                    _sb.append(classAlias).append(SK._PERIOD);
                }

                _sb.append(propAlias);

                if (quotePropAlias) {
                    _sb.append(SK._DOUBLE_QUOTE);
                }
            }
        } else if (isForSelect) {
            int index = Strings.indexOfIgnoreCase(propName, " AS ");

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
                //        _sb.append(SK._DOUBLE_QUOTE);
                //
                //        if (withClassAlias) {
                //            _sb.append(classAlias).append(SK._PERIOD);
                //        }
                //
                //        _sb.append(propName);
                //        _sb.append(SK._DOUBLE_QUOTE);
                //    }

                int idx = -1;
                if (withClassAlias || propName.length() > _sb.length()
                        || (_sb.length() - propName.length() - 1 >= 0 && _sb.charAt(_sb.length() - propName.length() - 1) != _SPACE)
                        || _sb.indexOf(propName, _sb.length() - propName.length()) < 0 || ((idx = propName.indexOf(SK._PERIOD)) > 0
                                && (Strings.isEmpty(tableAlias) || tableAlias.length() != idx || !propName.startsWith(tableAlias)))) {
                    _sb.append(_SPACE_AS_SPACE);

                    if (quotePropAlias) {
                        _sb.append(SK._DOUBLE_QUOTE);
                    }

                    if (withClassAlias) {
                        _sb.append(classAlias).append(SK._PERIOD);
                    }

                    _sb.append(propName);

                    if (quotePropAlias) {
                        _sb.append(SK._DOUBLE_QUOTE);
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
     * Checks if the provided property or column names represent a subquery.
     *
     * @param propOrColumnNames array of property or column names to check
     * @return {@code true} if any of the names represents a subquery, {@code false} otherwise
     */
    protected static boolean isSubQuery(final String... propOrColumnNames) {
        if (propOrColumnNames.length == 1) {
            int index = SQLParser.indexOfWord(propOrColumnNames[0], SK.SELECT, 0, false);

            if (index >= 0) {
                index = SQLParser.indexOfWord(propOrColumnNames[0], SK.FROM, index, false);

                return index >= 1;
            }
        }

        return false;
    }

    protected static String normalizeColumnName(final String word, final NamingPolicy namingPolicy) {
        if (sqlKeyWords.contains(word) || namingPolicy == NamingPolicy.NO_CHANGE) {
            return word;
        }
        if (namingPolicy == NamingPolicy.CAMEL_CASE) {
            return Beans.normalizePropName(word);
        }
        return namingPolicy.convert(word);
    }

    protected String normalizeColumnName(final ImmutableMap<String, Tuple2<String, Boolean>> propColumnNameMap, final String propName) {
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
            return _tableAlias + "." + normalizeColumnName(propName.substring(_tableAlias.length() + 1), _namingPolicy);
        } else {
            return normalizeColumnName(propName, _namingPolicy);
        }
    }

    protected static void parseInsertEntity(@SuppressWarnings("rawtypes") final AbstractQueryBuilder instance, final Object entity,
            final Set<String> excludedPropNames) {
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

                map.put(propName, propValue);
            }

            instance._props = map;
        }
    }

    protected static List<Map<String, Object>> toInsertPropsList(final Collection<?> propsList) {
        final Optional<?> first = N.firstNonNull(propsList);

        if (first.isPresent() && first.get() instanceof Map) {
            final List<Map<String, Object>> newPropsList = new ArrayList<>(propsList.size());

            for (final Object props : propsList) {
                if (props == null) {
                    continue;
                }

                N.checkArgument(props instanceof Map, "All elements in propsList must be Map when the first non-null element is Map");
                newPropsList.add((Map<String, Object>) props);
            }

            N.checkArgument(N.notEmpty(newPropsList), "All elements in propsList are null");

            return newPropsList;
        }

        N.checkArgument(first.isPresent(), "All elements in propsList are null");

        final Class<?> entityClass = first.get().getClass();
        final Collection<String> propNames = QueryUtil.getInsertPropNames(entityClass, null);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(entityClass);
        final List<Map<String, Object>> newPropsList = new ArrayList<>(propsList.size());

        for (final Object entity : propsList) {
            if (entity == null) {
                continue;
            }

            final Map<String, Object> props = N.newHashMap(propNames.size());

            for (final String propName : propNames) {
                props.put(propName, beanInfo.getPropValue(entity, propName));
            }

            newPropsList.add(props);
        }

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

    protected static final BiConsumer<StringBuilder, String> defaultHandlerForNamedParameter = (sb, propName) -> sb.append(":").append(propName);
    // private static final BiConsumer<StringBuilder, String> mybatisHandlerForNamedParameter = (sb, propName) -> sb.append("#{").append(propName).append("}");

    protected static final ThreadLocal<BiConsumer<StringBuilder, String>> handlerForNamedParameter_TL = ThreadLocal //NOSONAR
            .withInitial(() -> defaultHandlerForNamedParameter);

    /**
     * Sets a custom handler for formatting named parameters in SQL strings.
     * The default handler formats parameters as {@code :paramName}.
     * This is a thread-local setting, so each thread can have its own handler.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use MyBatis-style named parameters: #{paramName}
     * AbstractQueryBuilder.setHandlerForNamedParameter(
     *     (sb, propName) -> sb.append("#{").append(propName).append("}"));
     *
     * // Reset to default when done
     * AbstractQueryBuilder.resetHandlerForNamedParameter();
     * }</pre>
     *
     * @param handlerForNamedParameter the handler to format named parameters; must not be null
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // After using a custom handler, reset to default
     * SQLBuilder.resetHandlerForNamedParameter();
     * 
     * // Named SQL will now use :paramName format again
     * String sql = NSC.select("name").from("users").where(Filters.equal("id", 1)).toSql();
     * // Output: SELECT name FROM users WHERE id = :id
     * }</pre>
     */
    public static void resetHandlerForNamedParameter() {
        handlerForNamedParameter_TL.set(defaultHandlerForNamedParameter);
    }

    protected static String getFromClause(final List<Selection> multiSelects, final NamingPolicy namingPolicy) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int idx = 0;

            for (final Selection selection : multiSelects) {
                if (idx++ > 0) {
                    sb.append(_COMMA_SPACE);
                }

                sb.append(getTableName(selection.entityClass(), namingPolicy));

                if (Strings.isNotEmpty(selection.tableAlias())) {
                    sb.append(' ').append(selection.tableAlias());
                }

                if (N.notEmpty(selection.selectPropNames()) || selection.includeSubEntityProperties()) {
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

                        final String subEntityTableAlias = getTableAlias(subEntityClass);
                        if (Strings.isNotEmpty(subEntityTableAlias)) {
                            sb.append(' ').append(subEntityTableAlias);
                        }
                    }
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    protected enum SQLPolicy {
        RAW_SQL, PARAMETERIZED_SQL, NAMED_SQL, IBATIS_SQL
    }

    /**
     * Represents a SQL string and its associated parameters.
     * This class is used to encapsulate the generated SQL and the parameters required for execution.
     * It is immutable, meaning once created, the SQL and parameters cannot be changed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Build a query and get SQL with parameters
     * SP sqlPair = PSC.select("firstName", "lastName")
     *                 .from("users")
     *                 .where(Filters.equal("id", 123))
     *                 .build();
     *
     * // Access SQL and parameters
     * String sql = sqlPair.sql;
     * // "SELECT first_name AS \"firstName\", last_name AS \"lastName\" FROM users WHERE id = ?"
     *
     * List<Object> params = sqlPair.parameters;
     * // [123]
     * }</pre>
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
        public final ImmutableList<Object> parameters;

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
         * @return {@code true} if this object is the same as the obj argument; false otherwise
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
}
