/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.landawn.abacus.annotation.NotColumn;
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
 * It's easier to write/maintain the sql by <code>SQLBuilder</code> and more efficient, comparing to write sql in plain text.
 * <br>The <code>sql()</code> or <code>pair()</code> method must be called to release resources.
 * <br />Here is a sample:
 * <p>
 * String sql = NE.insert("gui", "firstName", "lastName").into("account").sql();
 * <br />// SQL: INSERT INTO account (gui, first_name, last_name) VALUES (:gui, :firstName, :lastName)
 * </p>
 *
 * The {@code tableName} will NOT be formalized.
 * <li>{@code select(...).from(String tableName).where(...)}</li>
 * <li>{@code insert(...).into(String tableName).values(...)}</li>
 * <li>{@code update(String tableName).set(...).where(...)}</li>
 * <li>{@code deleteFrom(String tableName).where(...)}</li>
 *
 * <br />
 *
 * @author Haiyang Li
 * @see {@link com.landawn.abacus.annotation.ReadOnly}
 * @see {@link com.landawn.abacus.annotation.ReadOnlyId}
 * @see {@link com.landawn.abacus.annotation.NonUpdatable}
 * @see {@link com.landawn.abacus.annotation.Transient}
 * @see {@link com.landawn.abacus.annotation.Table}
 * @see {@link com.landawn.abacus.annotation.Column}
 * @since 0.8
 */
@SuppressWarnings("deprecation")
public abstract class SQLBuilder {

    // TODO performance goal: 80% cases (or maybe sql.length < 1024?) can be composed in 0.1 millisecond. 0.01 millisecond will be fantastic if possible.

    private static final Logger logger = LoggerFactory.getLogger(SQLBuilder.class);

    public static final String ALL = WD.ALL;

    public static final String TOP = WD.TOP;

    public static final String UNIQUE = WD.UNIQUE;

    public static final String DISTINCT = WD.DISTINCT;

    public static final String DISTINCTROW = WD.DISTINCTROW;

    public static final String ASTERISK = WD.ASTERISK;

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

    private static final Set<String> sqlKeyWords = N.newHashSet(1024);

    static {
        final Field[] fields = WD.class.getDeclaredFields();
        int m = 0;

        for (Field field : fields) {
            m = field.getModifiers();

            if (Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m) && field.getType().equals(String.class)) {
                try {
                    final String value = (String) field.get(null);

                    for (String e : Strings.split(value, ' ', true)) {
                        sqlKeyWords.add(e);
                        sqlKeyWords.add(e.toUpperCase());
                        sqlKeyWords.add(e.toLowerCase());
                    }
                } catch (Exception e) {
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
        for (NamingPolicy np : NamingPolicy.values()) {
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

    SQLBuilder(final NamingPolicy namingPolicy, final SQLPolicy sqlPolicy) {
        if (activeStringBuilderCounter.incrementAndGet() > 1024) {
            logger.error("Too many(" + activeStringBuilderCounter.get()
                    + ") StringBuilder instances are created in SQLBuilder. The method sql()/pair() must be called to release resources and close SQLBuilder");
        }

        this._sb = Objectory.createStringBuilder();

        this._namingPolicy = namingPolicy == null ? NamingPolicy.LOWER_CASE_WITH_UNDERSCORE : namingPolicy;
        this._sqlPolicy = sqlPolicy == null ? SQLPolicy.SQL : sqlPolicy;

        this._handlerForNamedParameter = handlerForNamedParameter_TL.get();
    }

    /**
     * Gets the table name.
     *
     * @param entityClass
     * @param namingPolicy
     * @return
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

    static String getTableAlias(final String alias, final Class<?> entityClass) {
        if (N.notNullOrEmpty(alias)) {
            return alias;
        }

        return getTableAlias(entityClass);
    }

    static String getTableAliasOrName(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        return getTableAliasOrName(null, entityClass, namingPolicy);
    }

    static String getTableAliasOrName(final String alias, final Class<?> entityClass, final NamingPolicy namingPolicy) {
        String tableAliasOrName = alias;

        if (N.isNullOrEmpty(tableAliasOrName)) {
            tableAliasOrName = getTableAlias(entityClass);
        }

        if (N.isNullOrEmpty(tableAliasOrName)) {
            tableAliasOrName = getTableName(entityClass, namingPolicy);
        }

        return tableAliasOrName;
    }

    /**
     * Checks if is default id prop value.
     *
     * @param propValue
     * @return true, if is default id prop value
     */
    @Internal
    static boolean isDefaultIdPropValue(final Object propValue) {
        return (propValue == null) || (propValue instanceof Number && (((Number) propValue).longValue() == 0));
    }

    /**
     * Load prop names by class.
     *
     * @param entityClass
     * @return
     */
    static Set<String>[] loadPropNamesByClass(final Class<?> entityClass) {
        Set<String>[] val = defaultPropNamesPool.get(entityClass);

        if (val == null) {
            synchronized (defaultPropNamesPool) {
                final Set<String> entityPropNames = N.newLinkedHashSet(ClassUtil.getPropNameList(entityClass));
                final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

                if (N.notNullOrEmpty(subEntityPropNames)) {
                    entityPropNames.removeAll(subEntityPropNames);
                }

                val = new Set[5];
                val[0] = N.newLinkedHashSet(entityPropNames);
                val[1] = N.newLinkedHashSet(entityPropNames);
                val[2] = N.newLinkedHashSet(entityPropNames);
                val[3] = N.newLinkedHashSet(entityPropNames);
                val[4] = N.newLinkedHashSet(entityPropNames);

                final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
                Class<?> subEntityClass = null;
                Set<String> subEntityPropNameList = null;

                for (String subEntityPropName : subEntityPropNames) {
                    PropInfo propInfo = entityInfo.getPropInfo(subEntityPropName);
                    subEntityClass = (propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type).clazz();

                    subEntityPropNameList = N.newLinkedHashSet(ClassUtil.getPropNameList(subEntityClass));
                    subEntityPropNameList.removeAll(getSubEntityPropNames(subEntityClass));

                    for (String pn : subEntityPropNameList) {
                        val[0].add(Strings.concat(subEntityPropName, WD.PERIOD, pn));
                    }
                }

                final Set<String> nonUpdatableNonWritablePropNames = N.newHashSet();
                final Set<String> nonUpdatablePropNames = N.newHashSet();
                final Set<String> transientPropNames = N.newHashSet();

                for (PropInfo propInfo : entityInfo.propInfoList) {
                    if (propInfo.isAnnotationPresent(ReadOnly.class) || propInfo.isAnnotationPresent(ReadOnlyId.class) || propInfo.isMarkedToReadOnlyId) {
                        nonUpdatableNonWritablePropNames.add(propInfo.name);
                    }

                    if (propInfo.isAnnotationPresent(NonUpdatable.class)) {
                        nonUpdatablePropNames.add(propInfo.name);
                    }

                    if (propInfo.isTransient || propInfo.isAnnotationPresent(NotColumn.class)) {
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

                for (String idPropName : QueryUtil.getIdFieldNames(entityClass)) {
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
     * Gets the sub entity prop names.
     *
     * @param entityClass
     * @return
     */
    static ImmutableSet<String> getSubEntityPropNames(final Class<?> entityClass) {
        ImmutableSet<String> subEntityPropNames = subEntityPropNamesPool.get(entityClass);
        if (subEntityPropNames == null) {
            synchronized (subEntityPropNamesPool) {
                final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
                // final ImmutableSet<String> nonSubEntityPropNames = nonSubEntityPropNamesPool.get(entityClass);
                final Set<String> subEntityPropNameSet = N.newLinkedHashSet();

                for (PropInfo propInfo : entityInfo.propInfoList) {
                    if (isEntityProp(propInfo)) {
                        subEntityPropNameSet.add(propInfo.name);
                    }
                }

                subEntityPropNames = ImmutableSet.wrap(subEntityPropNameSet);

                subEntityPropNamesPool.put(entityClass, subEntityPropNames);
            }
        }

        return subEntityPropNames;
    }

    private static List<String> getSelectTableNames(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames,
            final NamingPolicy namingPolicy) {
        final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

        if (N.isNullOrEmpty(subEntityPropNames)) {
            return N.emptyList();
        }

        final List<String> res = new ArrayList<>(subEntityPropNames.size() + 1);

        String tableAlias = getTableAlias(alias, entityClass);

        if (N.isNullOrEmpty(tableAlias)) {
            res.add(getTableName(entityClass, namingPolicy));
        } else {
            res.add(getTableName(entityClass, namingPolicy) + " " + tableAlias);
        }

        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
        PropInfo propInfo = null;
        Class<?> subEntityClass = null;

        for (String subEntityPropName : subEntityPropNames) {
            if (excludedPropNames != null && excludedPropNames.contains(subEntityPropName)) {
                continue;
            }

            propInfo = entityInfo.getPropInfo(subEntityPropName);
            subEntityClass = (propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type).clazz();
            tableAlias = getTableAlias(subEntityClass);

            if (N.isNullOrEmpty(tableAlias)) {
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
    //            throw N.toRuntimeException(e);
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
     *
     * @param propNames
     * @return
     */
    @Beta
    static Map<String, Expression> named(final String... propNames) {
        final Map<String, Expression> m = N.newLinkedHashMap(propNames.length);

        for (String propName : propNames) {
            m.put(propName, CF.QME);
        }

        return m;
    }

    /**
     *
     * @param propNames
     * @return
     */
    @Beta
    static Map<String, Expression> named(final Collection<String> propNames) {
        final Map<String, Expression> m = N.newLinkedHashMap(propNames.size());

        for (String propName : propNames) {
            m.put(propName, CF.QME);
        }

        return m;
    }

    /**
     *
     * @param tableName
     * @return
     */
    public SQLBuilder into(final String tableName) {
        if (!(_op == OperationType.ADD || _op == OperationType.QUERY)) {
            throw new RuntimeException("Invalid operation: " + _op);
        }

        if (_op == OperationType.QUERY) {
            if (N.isNullOrEmpty(_propOrColumnNames) && N.isNullOrEmpty(_propOrColumnNameAliases) && N.isNullOrEmpty(_multiSelects)) {
                throw new RuntimeException("Column names or props must be set first by select");
            }
        } else {
            if (N.isNullOrEmpty(_propOrColumnNames) && N.isNullOrEmpty(_props) && N.isNullOrEmpty(_propsList)) {
                throw new RuntimeException("Column names or props must be set first by insert");
            }
        }

        this._tableName = tableName;

        _sb.append(_INSERT);
        _sb.append(_SPACE_INTO_SPACE);

        _sb.append(tableName);

        _sb.append(_SPACE);
        _sb.append(WD._PARENTHESES_L);

        if (N.notNullOrEmpty(_propOrColumnNames)) {
            int i = 0;
            for (String columnName : _propOrColumnNames) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);
            }
        } else {
            final Map<String, Object> localProps = N.isNullOrEmpty(this._props) ? _propsList.iterator().next() : this._props;

            int i = 0;
            for (String columnName : localProps.keySet()) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);
            }
        }

        _sb.append(WD._PARENTHESES_R);

        _sb.append(_SPACE_VALUES_SPACE);

        _sb.append(WD._PARENTHESES_L);

        if (N.notNullOrEmpty(_propOrColumnNames)) {
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
                    for (String columnName : _propOrColumnNames) {
                        if (i++ > 0) {
                            _sb.append(_COMMA_SPACE);
                        }

                        _handlerForNamedParameter.accept(_sb, columnName);
                    }

                    break;
                }

                case IBATIS_SQL: {
                    int i = 0;
                    for (String columnName : _propOrColumnNames) {
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
        } else if (N.notNullOrEmpty(_props)) {
            appendInsertProps(_props);
        } else {
            int i = 0;
            for (Map<String, Object> localProps : _propsList) {
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
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder into(final Class<?> entityClass) {
        if (this._entityClass == null) {
            setEntityClass(entityClass);
        }

        return into(getTableName(entityClass, _namingPolicy));
    }

    /**
     *
     * @return
     */
    public SQLBuilder distinct() { //NOSONAR
        return preselect(DISTINCT);
    }

    /**
     *
     * @param preselect <code>ALL | DISTINCT | DISTINCTROW...</code>
     * @return
     */
    public SQLBuilder preselect(final String preselect) {
        N.checkArgNotNull(preselect, "preselect");

        if (_sb.length() > 0) {
            throw new IllegalStateException("'distinct|preselect' must be called before 'from' operation");
        }

        if (N.isNullOrEmpty(this._preselect)) {
            this._preselect = preselect;
        } else {
            this._preselect += preselect;
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder from(String expr) {
        expr = expr.trim();

        final int idx = expr.indexOf(WD._COMMA);
        final String localTableName = idx > 0 ? expr.substring(0, idx) : expr;

        return from(localTableName.trim(), expr);
    }

    /**
     *
     * @param tableNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder from(final String... tableNames) {
        if (tableNames.length == 1) {
            return from(tableNames[0].trim());
        }

        final String localTableName = tableNames[0].trim();
        return from(localTableName, Strings.join(tableNames, WD.COMMA_SPACE));
    }

    /**
     *
     * @param tableNames
     * @return
     */
    public SQLBuilder from(final Collection<String> tableNames) {
        if (tableNames.size() == 1) {
            return from(tableNames.iterator().next().trim());
        }

        final String localTableName = tableNames.iterator().next().trim();
        return from(localTableName, Strings.join(tableNames, WD.COMMA_SPACE));
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder from(final Class<?> entityClass) {
        if (this._entityClass == null) {
            setEntityClass(entityClass);
        }

        return from(getTableName(entityClass, _namingPolicy));
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder from(final Class<?> entityClass, final String alias) {
        if (this._entityClass == null) {
            setEntityClass(entityClass);
        }

        if (N.isNullOrEmpty(alias)) {
            return from(getTableName(entityClass, _namingPolicy));
        } else {
            return from(getTableName(entityClass, _namingPolicy) + " " + alias);
        }
    }

    private SQLBuilder from(final Class<?> entityClass, final Collection<String> tableNames) {
        if (this._entityClass == null) {
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

        if (N.isNullOrEmpty(_propOrColumnNames) && N.isNullOrEmpty(_propOrColumnNameAliases) && N.isNullOrEmpty(_multiSelects)) {
            throw new RuntimeException("Column names or props must be set first by select");
        }

        int idx = tableName.indexOf(' ');

        if (idx > 0) {
            this._tableName = tableName.substring(0, idx).trim();
            this._tableAlias = tableName.substring(idx + 1).trim();
        } else {
            this._tableName = tableName.trim();
        }

        if (_entityClass != null && N.notNullOrEmpty(_tableAlias)) {
            addPropColumnMapForAlias(_entityClass, _tableAlias);
        }

        _sb.append(_SELECT);
        _sb.append(_SPACE);

        if (N.notNullOrEmpty(_preselect)) {
            _sb.append(_preselect);
            _sb.append(_SPACE);
        }

        final boolean withAlias = N.notNullOrEmpty(_tableAlias);
        final boolean isForSelect = _op == OperationType.QUERY;

        if (N.notNullOrEmpty(_propOrColumnNames)) {
            if (_entityClass != null && withAlias == false && _propOrColumnNames == QueryUtil.getSelectPropNames(_entityClass, false, null)) {
                String fullSelectParts = fullSelectPartsPool.get(_namingPolicy).get(_entityClass);

                if (N.isNullOrEmpty(fullSelectParts)) {
                    final StringBuilder sb = new StringBuilder();

                    int i = 0;
                    for (String columnName : _propOrColumnNames) {
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
                for (String columnName : _propOrColumnNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(_entityClass, _entityInfo, _propColumnNameMap, _tableAlias, columnName, null, false, null, isForSelect);
                }
            }
        } else if (N.notNullOrEmpty(_propOrColumnNameAliases)) {
            int i = 0;
            for (Map.Entry<String, String> entry : _propOrColumnNameAliases.entrySet()) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(_entityClass, _entityInfo, _propColumnNameMap, _tableAlias, entry.getKey(), entry.getValue(), false, null, isForSelect);
            }
        } else if (N.notNullOrEmpty(_multiSelects)) {
            this._aliasPropColumnNameMap = N.newHashMap(_multiSelects.size());

            for (Selection selection : _multiSelects) {
                if (N.notNullOrEmpty(selection.tableAlias())) {
                    this._aliasPropColumnNameMap.put(selection.tableAlias(), QueryUtil.prop2ColumnNameMap(selection.entityClass(), _namingPolicy));
                }
            }

            Class<?> selectionEntityClass = null;
            BeanInfo selectionBeanInfo = null;
            ImmutableMap<String, Tuple2<String, Boolean>> selectionPropColumnNameMap = null;
            String selectionTableAlias = null;
            String selectionClassAlias = null;
            boolean selectionWithClassAlias = false;

            int i = 0;

            for (Selection selection : _multiSelects) {
                selectionEntityClass = selection.entityClass();
                selectionBeanInfo = selectionEntityClass == null && ClassUtil.isBeanClass(selectionEntityClass) == false ? null
                        : ParserUtil.getBeanInfo(selectionEntityClass);
                selectionPropColumnNameMap = selectionEntityClass == null && ClassUtil.isBeanClass(selectionEntityClass) == false ? null
                        : QueryUtil.prop2ColumnNameMap(selectionEntityClass, _namingPolicy);
                selectionTableAlias = selection.tableAlias();

                selectionClassAlias = selection.classAlias();
                selectionWithClassAlias = N.notNullOrEmpty(selectionClassAlias);

                final Collection<String> selectPropNames = N.notNullOrEmpty(selection.selectPropNames()) ? selection.selectPropNames()
                        : QueryUtil.getSelectPropNames(selectionEntityClass, selection.includeSubEntityProperties(), selection.excludedPropNames());

                for (String propName : selectPropNames) {
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

        if (N.isNullOrEmpty(_propColumnNameMap) && entityClass != null && ClassUtil.isBeanClass(entityClass)) {
            _propColumnNameMap = QueryUtil.prop2ColumnNameMap(entityClass, _namingPolicy);
        }

        _aliasPropColumnNameMap.put(alias, _propColumnNameMap);
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder join(final String expr) {
        _sb.append(_SPACE_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder join(final Class<?> entityClass) {
        return join(entityClass, null);
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder join(final Class<?> entityClass, final String alias) {
        if (N.notNullOrEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_JOIN_SPACE);

        if (N.notNullOrEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy) + " " + alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder innerJoin(final String expr) {
        _sb.append(_SPACE_INNER_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder innerJoin(final Class<?> entityClass) {
        return innerJoin(entityClass, null);
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder innerJoin(final Class<?> entityClass, final String alias) {
        if (N.notNullOrEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_INNER_JOIN_SPACE);

        if (N.notNullOrEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy) + " " + alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder leftJoin(final String expr) {
        _sb.append(_SPACE_LEFT_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder leftJoin(final Class<?> entityClass) {
        return leftJoin(entityClass, null);
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder leftJoin(final Class<?> entityClass, final String alias) {
        if (N.notNullOrEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_LEFT_JOIN_SPACE);

        if (N.notNullOrEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy) + " " + alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder rightJoin(final String expr) {
        _sb.append(_SPACE_RIGHT_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder rightJoin(final Class<?> entityClass) {
        return rightJoin(entityClass, null);
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder rightJoin(final Class<?> entityClass, final String alias) {
        if (N.notNullOrEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_RIGHT_JOIN_SPACE);

        if (N.notNullOrEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy) + " " + alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder fullJoin(final String expr) {
        _sb.append(_SPACE_FULL_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder fullJoin(final Class<?> entityClass) {
        return fullJoin(entityClass, null);
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder fullJoin(final Class<?> entityClass, final String alias) {
        if (N.notNullOrEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_FULL_JOIN_SPACE);

        if (N.notNullOrEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy) + " " + alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder crossJoin(final String expr) {
        _sb.append(_SPACE_CROSS_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder crossJoin(final Class<?> entityClass) {
        return crossJoin(entityClass, null);
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder crossJoin(final Class<?> entityClass, final String alias) {
        if (N.notNullOrEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_CROSS_JOIN_SPACE);

        if (N.notNullOrEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy) + " " + alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder naturalJoin(final String expr) {
        _sb.append(_SPACE_NATURAL_JOIN_SPACE);

        _sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder naturalJoin(final Class<?> entityClass) {
        return naturalJoin(entityClass, null);
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder naturalJoin(final Class<?> entityClass, final String alias) {
        if (N.notNullOrEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(_SPACE_NATURAL_JOIN_SPACE);

        if (N.notNullOrEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy) + " " + alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder on(final String expr) {
        _sb.append(_SPACE_ON_SPACE);

        appendStringExpr(expr, false);

        return this;
    }

    /**
     *
     * @param cond any literal written in <code>Expression</code> condition won't be formalized
     * @return
     */
    public SQLBuilder on(final Condition cond) {
        _sb.append(_SPACE_ON_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder using(final String expr) {
        _sb.append(_SPACE_USING_SPACE);

        appendColumnName(expr);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder where(final String expr) {
        init(true);

        _sb.append(_SPACE_WHERE_SPACE);

        appendStringExpr(expr, false);

        return this;
    }

    /**
     *
     * @param cond
     * @return
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
     *
     * @param expr
     * @return
     */
    public SQLBuilder groupBy(final String expr) {
        _sb.append(_SPACE_GROUP_BY_SPACE);

        appendColumnName(expr);

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @SafeVarargs
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
     *
     * @param columnName
     * @param direction
     * @return
     */
    public SQLBuilder groupBy(final String columnName, final SortDirection direction) {
        groupBy(columnName);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder groupBy(final Collection<String> propOrColumnNames) {
        _sb.append(_SPACE_GROUP_BY_SPACE);

        int i = 0;
        for (String columnName : propOrColumnNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(columnName);
        }

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @param direction
     * @return
     */
    public SQLBuilder groupBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        groupBy(propOrColumnNames);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param orders
     * @return
     */
    public SQLBuilder groupBy(final Map<String, SortDirection> orders) {
        _sb.append(_SPACE_GROUP_BY_SPACE);

        int i = 0;
        for (Map.Entry<String, SortDirection> entry : orders.entrySet()) {
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
     *
     * @param expr
     * @return
     */
    public SQLBuilder having(final String expr) {
        _sb.append(_SPACE_HAVING_SPACE);

        appendStringExpr(expr, false);

        return this;
    }

    /**
     *
     * @param cond any literal written in <code>Expression</code> condition won't be formalized
     * @return
     * @see ConditionFactory
     * @see ConditionFactory.CF
     */
    public SQLBuilder having(final Condition cond) {
        _sb.append(_SPACE_HAVING_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder orderBy(final String expr) {
        _sb.append(_SPACE_ORDER_BY_SPACE);

        appendColumnName(expr);

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @SafeVarargs
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
     *
     * @param columnName
     * @param direction
     * @return
     */
    public SQLBuilder orderBy(final String columnName, final SortDirection direction) {
        orderBy(columnName);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder orderBy(final Collection<String> propOrColumnNames) {
        _sb.append(_SPACE_ORDER_BY_SPACE);

        int i = 0;
        for (String columnName : propOrColumnNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(columnName);
        }

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @param direction
     * @return
     */
    public SQLBuilder orderBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        orderBy(propOrColumnNames);

        _sb.append(_SPACE);
        _sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param orders
     * @return
     */
    public SQLBuilder orderBy(final Map<String, SortDirection> orders) {
        _sb.append(_SPACE_ORDER_BY_SPACE);

        int i = 0;

        for (Map.Entry<String, SortDirection> entry : orders.entrySet()) {
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
     *
     * @param expr
     * @return
     */
    @Beta
    public SQLBuilder orderByAsc(final String expr) {
        return orderBy(expr, SortDirection.ASC);
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @Beta
    @SafeVarargs
    public final SQLBuilder orderByAsc(final String... propOrColumnNames) {
        return orderBy(N.asList(propOrColumnNames), SortDirection.ASC);
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @Beta
    public final SQLBuilder orderByAsc(final Collection<String> propOrColumnNames) {
        return orderBy(propOrColumnNames, SortDirection.ASC);
    }

    /**
     *
     * @param expr
     * @return
     */
    @Beta
    public SQLBuilder orderByDesc(final String expr) {
        return orderBy(expr, SortDirection.DESC);
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @Beta
    @SafeVarargs
    public final SQLBuilder orderByDesc(final String... propOrColumnNames) {
        return orderBy(N.asList(propOrColumnNames), SortDirection.DESC);
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @Beta
    public final SQLBuilder orderByDesc(final Collection<String> propOrColumnNames) {
        return orderBy(propOrColumnNames, SortDirection.DESC);
    }

    /**
     *
     * @param count
     * @return
     */
    public SQLBuilder limit(final int count) {
        _sb.append(_SPACE_LIMIT_SPACE);

        _sb.append(count);

        return this;
    }

    /**
     *
     * @param offset
     * @param count
     * @return
     */
    public SQLBuilder limit(final int offset, final int count) {
        _sb.append(_SPACE_LIMIT_SPACE);

        _sb.append(count);

        _sb.append(_SPACE_OFFSET_SPACE);

        _sb.append(offset);

        return this;
    }

    /**
     *
     * @param offset
     * @return
     */
    public SQLBuilder offset(final int offset) {
        _sb.append(_SPACE_OFFSET_SPACE).append(offset);

        return this;
    }

    /**
     *
     * @param offset
     * @return
     */
    public SQLBuilder offsetRows(final int offset) {
        _sb.append(_SPACE_OFFSET_SPACE).append(offset).append(_SPACE_ROWS_SPACE);

        return this;
    }

    /**
     *
     *
     * @param n
     * @return
     */
    public SQLBuilder fetchNextNRowsOnly(final int n) {
        _sb.append(" FETCH NEXT ").append(n).append(" ROWS ONLY");

        return this;
    }

    /**
     *
     *
     * @param n
     * @return
     */
    public SQLBuilder fetchFirstNRowsOnly(final int n) {
        _sb.append(" FETCH FIRST ").append(n).append(" ROWS ONLY");

        return this;
    }

    /**
     *
     * @param cond
     * @return
     * @see ConditionFactory
     * @see ConditionFactory.CF
     */
    public SQLBuilder append(final Condition cond) {
        init(true);

        if (cond instanceof Criteria) {
            final Criteria criteria = (Criteria) cond;

            final Collection<Join> joins = criteria.getJoins();

            if (N.notNullOrEmpty(joins)) {
                for (Join join : joins) {
                    _sb.append(_SPACE).append(join.getOperator()).append(_SPACE);

                    if (join.getJoinEntities().size() == 1) {
                        _sb.append(join.getJoinEntities().get(0));
                    } else {
                        _sb.append(WD._PARENTHESES_L);
                        int idx = 0;

                        for (String joinTableName : join.getJoinEntities()) {
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

            List<Cell> aggregations = criteria.getAggregation();

            if (N.notNullOrEmpty(aggregations)) {
                for (Cell aggregation : aggregations) {
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
                if (N.notNullOrEmpty(limit.getExpr())) {
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
     *
     *
     * @param expr
     * @return
     */
    public SQLBuilder append(final String expr) {
        _sb.append(expr);

        return this;
    }

    /**
     *
     *
     * @param b
     * @param cond
     * @return
     */
    public SQLBuilder appendIf(final boolean b, final Condition cond) {
        if (b) {
            append(cond);
        }

        return this;
    }

    /**
     *
     *
     * @param b
     * @param expr
     * @return
     */
    public SQLBuilder appendIf(final boolean b, final String expr) {
        if (b) {
            append(expr);
        }

        return this;
    }

    /**
     *
     *
     * @param b
     * @param append
     * @return
     */
    @Beta
    public SQLBuilder appendIf(final boolean b, final java.util.function.Consumer<SQLBuilder> append) {
        if (b) {
            append.accept(this);
        }

        return this;
    }

    /**
     *
     *
     * @param b
     * @param condToAppendForTrue
     * @param condToAppendForFalse
     * @return
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
     *
     *
     * @param b
     * @param exprToAppendForTrue
     * @param exprToAppendForFalse
     * @return
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
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder union(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return union(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder union(final String query) {
        return union(N.asArray(query));
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder union(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = Array.asList(propOrColumnNames);
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_UNION_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            this._propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder union(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = propOrColumnNames;
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_UNION_SPACE);

        return this;
    }

    /**
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder unionAll(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return unionAll(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder unionAll(final String query) {
        return unionAll(N.asArray(query));
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder unionAll(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = Array.asList(propOrColumnNames);
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_UNION_ALL_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            this._propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder unionAll(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = propOrColumnNames;
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_UNION_ALL_SPACE);

        return this;
    }

    /**
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder intersect(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return intersect(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder intersect(final String query) {
        return intersect(N.asArray(query));
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder intersect(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = Array.asList(propOrColumnNames);
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_INTERSECT_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            this._propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder intersect(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = propOrColumnNames;
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_INTERSECT_SPACE);

        return this;
    }

    /**
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder except(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return except(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder except(final String query) {
        return except(N.asArray(query));
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder except(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = Array.asList(propOrColumnNames);
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_EXCEPT_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            this._propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder except(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = propOrColumnNames;
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_EXCEPT_SPACE);

        return this;
    }

    /**
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder minus(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            _parameters.addAll(sqlBuilder.parameters());
        }

        return minus(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder minus(final String query) {
        return minus(N.asArray(query));
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder minus(final String... propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = Array.asList(propOrColumnNames);
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_EXCEPT2_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            _sb.append(propOrColumnNames[0]);

            this._propOrColumnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder minus(final Collection<String> propOrColumnNames) {
        _op = OperationType.QUERY;

        this._propOrColumnNames = propOrColumnNames;
        this._propOrColumnNameAliases = null;

        _sb.append(_SPACE_EXCEPT2_SPACE);

        return this;
    }

    /**
     *
     *
     * @return
     */
    public SQLBuilder forUpdate() {
        _sb.append(_SPACE_FOR_UPDATE);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder set(final String expr) {
        return set(Array.asList(expr));
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder set(final String... propOrColumnNames) {
        return set(Array.asList(propOrColumnNames));
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder set(final Collection<String> propOrColumnNames) {
        init(false);

        switch (_sqlPolicy) {
            case SQL:
            case PARAMETERIZED_SQL: {
                int i = 0;
                for (String columnName : propOrColumnNames) {
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
                for (String columnName : propOrColumnNames) {
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
                for (String columnName : propOrColumnNames) {
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

        this._propOrColumnNames = null;

        return this;
    }

    /**
     *
     * @param props
     * @return
     */
    public SQLBuilder set(final Map<String, Object> props) {
        init(false);

        switch (_sqlPolicy) {
            case SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
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
                for (Map.Entry<String, Object> entry : props.entrySet()) {
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
                for (Map.Entry<String, Object> entry : props.entrySet()) {
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
                for (Map.Entry<String, Object> entry : props.entrySet()) {
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

        this._propOrColumnNames = null;

        return this;
    }

    /**
     * Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
     *
     * @param entity
     * @return
     */
    public SQLBuilder set(final Object entity) {
        return set(entity, null);
    }

    /**
     * Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
     *
     * @param entity
     * @param excludedPropNames
     * @return
     */
    public SQLBuilder set(final Object entity, final Set<String> excludedPropNames) {
        if (entity instanceof String) {
            return set(N.asArray((String) entity));
        }
        if (entity instanceof Map) {
            if (N.isNullOrEmpty(excludedPropNames)) {
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
        final BeanInfo localEntityInfo = ParserUtil.getBeanInfo(entityClass);

        for (String propName : propNames) {
            localProps.put(propName, localEntityInfo.getPropValue(entity, propName));
        }

        return set(localProps);
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder set(Class<?> entityClass) {
        setEntityClass(entityClass);

        return set(entityClass, null);
    }

    /**
     *
     * @param entityClass
     * @param excludedPropNames
     * @return
     */
    public SQLBuilder set(Class<?> entityClass, final Set<String> excludedPropNames) {
        setEntityClass(entityClass);

        return set(QueryUtil.getUpdatePropNames(entityClass, excludedPropNames));
    }

    /**
     * This SQLBuilder will be closed after <code>sql()</code> is called.
     *
     * @return
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
     *
     *
     * @return
     */
    public List<Object> parameters() {
        return _parameters;
    }

    /**
     *  This SQLBuilder will be closed after <code>pair()</code> is called.
     *
     * @return
     */
    public SP pair() {
        final String sql = sql();

        return new SP(sql, _parameters);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param func
     * @return
     * @throws E
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.Function<? super SP, T, E> func) throws E {
        return func.apply(this.pair());
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param func
     * @return
     * @throws E
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.BiFunction<? super String, ? super List<Object>, T, E> func) throws E {
        final SP sp = this.pair();

        return func.apply(sp.sql, sp.parameters);
    }

    /**
     *
     *
     * @param <E>
     * @param consumer
     * @throws E
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.Consumer<? super SP, E> consumer) throws E {
        consumer.accept(this.pair());
    }

    /**
     *
     *
     * @param <E>
     * @param consumer
     * @throws E
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.BiConsumer<? super String, ? super List<Object>, E> consumer) throws E {
        final SP sp = this.pair();

        consumer.accept(sp.sql, sp.parameters);
    }

    /**
     *
     * @param setForUpdate
     */
    void init(boolean setForUpdate) {
        // Note: any change, please take a look at: parse(final Class<?> entityClass, final Condition cond) first.

        if (_sb.length() > 0) {
            return;
        }

        if (_op == OperationType.UPDATE) {
            _sb.append(_UPDATE);

            _sb.append(_SPACE);
            _sb.append(_tableName);

            _sb.append(_SPACE_SET_SPACE);

            if (setForUpdate && N.notNullOrEmpty(_propOrColumnNames)) {
                set(_propOrColumnNames);
            }
        } else if (_op == OperationType.DELETE) {
            final String newTableName = _tableName;

            char[] deleteFromTableChars = tableDeleteFrom.get(newTableName);

            if (deleteFromTableChars == null) {
                deleteFromTableChars = (WD.DELETE + WD.SPACE + WD.FROM + WD.SPACE + newTableName).toCharArray();
                tableDeleteFrom.put(newTableName, deleteFromTableChars);
            }

            _sb.append(deleteFromTableChars);
        } else if (_op == OperationType.QUERY && _hasFromBeenSet == false && _isForConditionOnly == false) {
            throw new RuntimeException("'from' methods has not been called for query: " + _op);
        }
    }

    private void setEntityClass(final Class<?> entityClass) {
        this._entityClass = entityClass;

        if (entityClass != null && ClassUtil.isBeanClass(entityClass)) {
            this._entityInfo = ParserUtil.getBeanInfo(entityClass);
            this._propColumnNameMap = QueryUtil.prop2ColumnNameMap(entityClass, _namingPolicy);
        } else {
            this._entityInfo = null;
            this._propColumnNameMap = null;
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
                for (Object propValue : props.values()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    setParameterForSQL(propValue);
                }

                break;
            }

            case PARAMETERIZED_SQL: {
                int i = 0;
                for (Object propValue : props.values()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    setParameterForRawSQL(propValue);
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    setParameterForNamedSQL(entry.getKey(), entry.getValue());
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
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

        if (cond instanceof Binary) {
            final Binary binary = (Binary) cond;
            final String propName = binary.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(binary.getOperator().toString());
            _sb.append(_SPACE);

            Object propValue = binary.getPropValue();
            setParameter(propName, propValue);
        } else if (cond instanceof Between) {
            final Between bt = (Between) cond;
            final String propName = bt.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(bt.getOperator().toString());
            _sb.append(_SPACE);

            Object minValue = bt.getMinValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("min" + Strings.capitalize(propName), minValue);
            } else {
                setParameter(propName, minValue);
            }

            _sb.append(_SPACE);
            _sb.append(WD.AND);
            _sb.append(_SPACE);

            Object maxValue = bt.getMaxValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("max" + Strings.capitalize(propName), maxValue);
            } else {
                setParameter(propName, maxValue);
            }
        } else if (cond instanceof NotBetween) {
            final NotBetween bt = (NotBetween) cond;
            final String propName = bt.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(bt.getOperator().toString());
            _sb.append(_SPACE);

            Object minValue = bt.getMinValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("min" + Strings.capitalize(propName), minValue);
            } else {
                setParameter(propName, minValue);
            }

            _sb.append(_SPACE);
            _sb.append(WD.AND);
            _sb.append(_SPACE);

            Object maxValue = bt.getMaxValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("max" + Strings.capitalize(propName), maxValue);
            } else {
                setParameter(propName, maxValue);
            }
        } else if (cond instanceof In) {
            final In in = (In) cond;
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
        } else if (cond instanceof InSubQuery) {
            final InSubQuery inSubQuery = (InSubQuery) cond;
            final String propName = inSubQuery.getPropName();

            if (N.notNullOrEmpty(propName)) {
                appendColumnName(propName);
            } else {
                _sb.append(WD._PARENTHESES_L);

                int idx = 0;

                for (String e : inSubQuery.getPropNames()) {
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
        } else if (cond instanceof NotIn) {
            final NotIn notIn = (NotIn) cond;
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
        } else if (cond instanceof NotInSubQuery) {
            final NotInSubQuery notInSubQuery = (NotInSubQuery) cond;
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
        } else if (cond instanceof Cell) {
            final Cell cell = (Cell) cond;

            _sb.append(_SPACE);
            _sb.append(cell.getOperator().toString());
            _sb.append(_SPACE);

            _sb.append(_PARENTHESES_L);
            appendCondition(cell.getCondition());
            _sb.append(_PARENTHESES_R);
        } else if (cond instanceof Junction) {
            final Junction junction = (Junction) cond;
            final List<Condition> conditionList = junction.getConditions();

            if (N.isNullOrEmpty(conditionList)) {
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
        } else if (cond instanceof SubQuery) {
            final SubQuery subQuery = (SubQuery) cond;
            final Condition subCond = subQuery.getCondition();

            if (N.notNullOrEmpty(subQuery.getSql())) {
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
                    throw new RuntimeException("Unsupproted subQuery condition: " + cond);
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
                throw new RuntimeException("Unsupproted subQuery condition: " + cond);
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
            throw new IllegalArgumentException("Unsupported condtion: " + cond.toString());
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

            if (!Strings.isAsciiAlpha(word.charAt(0))) {
                _sb.append(word);
            } else if (SQLParser.isFunctionName(words, len, i)) {
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
            if (tp._2.booleanValue() && tableAlias != null && tableAlias.length() > 0) {
                _sb.append(tableAlias).append(WD._PERIOD);
            }

            _sb.append(tp._1);

            if (isForSelect && _namingPolicy != NamingPolicy.NO_CHANGE) {
                _sb.append(_SPACE_AS_SPACE);
                _sb.append(WD._QUOTATION_D);

                if (withClassAlias) {
                    _sb.append(classAlias).append(WD._PERIOD);
                }

                _sb.append(N.notNullOrEmpty(propAlias) ? propAlias : propName);
                _sb.append(WD._QUOTATION_D);
            }

            return;
        }

        if (N.isNullOrEmpty(propAlias) && entityInfo != null) {
            final PropInfo propInfo = entityInfo.getPropInfo(propName);

            if (isEntityProp(propInfo)) {
                final Class<?> propEntityClass = propInfo.type.isCollection() ? propInfo.type.getElementType().clazz() : propInfo.clazz;

                final String propEntityTableAliasOrName = getTableAliasOrName(propEntityClass, _namingPolicy);

                final ImmutableMap<String, Tuple2<String, Boolean>> subPropColumnNameMap = QueryUtil.prop2ColumnNameMap(propEntityClass, _namingPolicy);

                final Collection<String> subSelectPropNames = QueryUtil.getSelectPropNames(propEntityClass, false, null);
                int i = 0;

                for (String subPropName : subSelectPropNames) {
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

        if (_aliasPropColumnNameMap != null && _aliasPropColumnNameMap.size() > 0) {
            int index = propName.indexOf('.');

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

                            _sb.append(N.notNullOrEmpty(propAlias) ? propAlias : propName);
                            _sb.append(WD._QUOTATION_D);
                        }

                        return;
                    }
                }
            }
        }

        if (N.notNullOrEmpty(propAlias)) {
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

    private static boolean isEntityProp(final PropInfo propInfo) {
        // final ImmutableSet<String> nonSubEntityPropNames = nonSubEntityPropNamesPool.get(entityClass);

        return propInfo != null
                && (!propInfo.isMarkedToColumn && (propInfo.type.isBean() || (propInfo.type.isCollection() && propInfo.type.getElementType().isBean())));
    }

    private static boolean hasSubEntityToInclude(final Class<?> entityClass, final boolean includeSubEntityProperties) {
        return includeSubEntityProperties && N.notNullOrEmpty(getSubEntityPropNames(entityClass));
    }

    /**
     * Checks if is sub query.
     *
     * @param propOrColumnNames
     * @return true, if is sub query
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

    //    @Override
    //    public int hashCode() {
    //        return sb.hashCode();
    //    }
    //
    //    @Override
    //    public boolean equals(Object obj) {
    //        if (this == obj) {
    //            return true;
    //        }
    //
    //        if (obj instanceof SQLBuilder) {
    //            final SQLBuilder other = (SQLBuilder) obj;
    //
    //            return N.equals(this.sb, other.sb) && N.equals(this.parameters, other.parameters);
    //        }
    //
    //        return false;
    //    }

    /**
     *
     */
    public void println() {
        N.println(sql());
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return sql();
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
            if (tp._2.booleanValue() && _tableAlias != null && _tableAlias.length() > 0) {
                return _tableAlias + "." + tp._1;
            }
            return tp._1;
        }

        if (_aliasPropColumnNameMap != null && _aliasPropColumnNameMap.size() > 0) {
            int index = propName.indexOf('.');

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
            if (N.isNullOrEmpty(excludedPropNames)) {
                instance._props = (Map<String, Object>) entity;
            } else {
                instance._props = new LinkedHashMap<>((Map<String, Object>) entity);
                Maps.removeKeys(instance._props, excludedPropNames);
            }
        } else {
            final Collection<String> propNames = QueryUtil.getInsertPropNames(entity, excludedPropNames);
            final Map<String, Object> map = N.newHashMap(propNames.size());
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(entity.getClass());

            for (String propName : propNames) {
                map.put(propName, entityInfo.getPropValue(entity, propName));
            }

            instance._props = map;
        }
    }

    private static Collection<Map<String, Object>> toInsertPropsList(final Collection<?> propsList) {
        final Optional<?> first = N.firstNonNull(propsList);

        if (first.isPresent() && first.get() instanceof Map) {
            return (List<Map<String, Object>>) propsList;
        }
        final Class<?> entityClass = first.get().getClass();
        final Collection<String> propNames = QueryUtil.getInsertPropNames(entityClass, null);
        final List<Map<String, Object>> newPropsList = new ArrayList<>(propsList.size());

        for (Object entity : propsList) {
            final Map<String, Object> props = N.newHashMap(propNames.size());
            final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);

            for (String propName : propNames) {
                props.put(propName, entityInfo.getPropValue(entity, propName));
            }

            newPropsList.add(props);
        }

        return newPropsList;
    }

    static void checkMultiSelects(final List<Selection> multiSelects) {
        N.checkArgNotNullOrEmpty(multiSelects, "multiSelects");

        for (Selection selection : multiSelects) {
            N.checkArgNotNull(selection.entityClass(), "Class can't be null in 'multiSelects'");
        }
    }

    enum SQLPolicy {
        SQL, PARAMETERIZED_SQL, NAMED_SQL, IBATIS_SQL;
    }

    /**
     * Un-parameterized SQL builder with snake case (lower case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * SCSB.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = 1
     * </code>
     * </pre>
     *
     * @deprecated {@code PSC or NSC} is preferred.
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart"); //NOSONAR

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames"); //NOSONAR

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases"); //NOSONAR

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
     * Un-parameterized SQL builder with all capitals case (upper case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(ACSB.select("firstName", "lastName").from("account").where(CF.eq("id", 1)).sql());
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = 1
     * </code>
     * </pre>
     *
     * @deprecated {@code PAC or NAC} is preferred.
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
     * Parameterized('?') SQL builder with all capitals case (upper case with underscore) field/column naming strategy.
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
     * Named SQL builder with all capitals case (upper case with underscore) field/column naming strategy.
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
     * MyBatis-style SQL builder with all capitals case (upper case with underscore) field/column naming strategy.
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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
        @SafeVarargs
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
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
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
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
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
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

            return instance;
        }

        /**
         *
         * @param selectPart
         * @return
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotNullOrEmpty(selectPart, "selectPart");

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
        @SafeVarargs
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNames, "propOrColumnNames");

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
            N.checkArgNotNullOrEmpty(propOrColumnNameAliases, "propOrColumnNameAliases");

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
            return selectFrom(entityClass, null, includeSubEntityProperties, excludedPropNames);
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

            if (obj instanceof SP) {
                SP other = (SP) obj;

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

    /**
     *
     */
    public static void resetHandlerForNamedParameter() {
        handlerForNamedParameter_TL.set(defaultHandlerForNamedParameter);
    }

    private static String getFromClause(final List<Selection> multiSelects, final NamingPolicy namingPolicy) {
        final StringBuilder sb = Objectory.createStringBuilder();
        int idx = 0;

        for (Selection selection : multiSelects) {
            if (idx++ > 0) {
                sb.append(_COMMA_SPACE);
            }

            sb.append(getTableName(selection.entityClass(), namingPolicy));

            if (N.notNullOrEmpty(selection.tableAlias())) {
                sb.append(' ').append(selection.tableAlias());
            }

            if (N.notNullOrEmpty(selection.selectPropNames()) || selection.includeSubEntityProperties) {
                final Class<?> entityClass = selection.entityClass();
                final Collection<String> selectPropNames = selection.selectPropNames();
                final Set<String> excludedPropNames = selection.excludedPropNames();
                final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

                if (N.isNullOrEmpty(subEntityPropNames)) {
                    continue;
                }

                final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
                PropInfo propInfo = null;
                Class<?> subEntityClass = null;

                for (String subEntityPropName : subEntityPropNames) {
                    if (N.notNullOrEmpty(selectPropNames)) {
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

        String fromClause = sb.toString();

        Objectory.recycle(sb);
        return fromClause;
    }
}
