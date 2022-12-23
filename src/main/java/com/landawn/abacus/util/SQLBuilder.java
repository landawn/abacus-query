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
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
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

    public static final String _1 = "1";

    @Deprecated
    public static final List<String> _1_list = ImmutableList.of(_1);

    public static final List<String> LIST_1 = ImmutableList.of(_1);

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
            fullSelectPartsPool.put(np, new ConcurrentHashMap<Class<?>, String>());
        }
    }

    private static final Map<String, char[]> tableDeleteFrom = new ConcurrentHashMap<>();

    private static final Map<Class<?>, String[]> classTableNameMap = new ConcurrentHashMap<>();

    private static final AtomicInteger activeStringBuilderCounter = new AtomicInteger();

    private final NamingPolicy namingPolicy;

    private final SQLPolicy sqlPolicy;

    private final List<Object> parameters = new ArrayList<>();

    private StringBuilder sb;

    private Class<?> entityClass;

    private EntityInfo entityInfo;

    private ImmutableMap<String, Tuple2<String, Boolean>> propColumnNameMap;

    private OperationType op;

    private String tableName;

    private String tableAlias;

    private String preselect;

    private Collection<String> propOrColumnNames;

    private Map<String, String> propOrColumnNameAliases;

    private List<Selection> multiSelects;

    private Map<String, Map<String, Tuple2<String, Boolean>>> aliasPropColumnNameMap;

    private Map<String, Object> props;

    private Collection<Map<String, Object>> propsList;

    private boolean hasFromBeenSet = false;
    private boolean isForConditionOnly = false;

    private final BiConsumer<StringBuilder, String> handlerForNamedParameter;

    SQLBuilder(final NamingPolicy namingPolicy, final SQLPolicy sqlPolicy) {
        if (activeStringBuilderCounter.incrementAndGet() > 1024) {
            logger.error("Too many(" + activeStringBuilderCounter.get()
                    + ") StringBuilder instances are created in SQLBuilder. The method sql()/pair() must be called to release resources and close SQLBuilder");
        }

        this.sb = Objectory.createStringBuilder();

        this.namingPolicy = namingPolicy == null ? NamingPolicy.LOWER_CASE_WITH_UNDERSCORE : namingPolicy;
        this.sqlPolicy = sqlPolicy == null ? SQLPolicy.SQL : sqlPolicy;

        this.handlerForNamedParameter = handlerForNamedParameter_TL.get();
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
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);

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

                final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);
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
                final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);
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

        if (N.isNullOrEmpty(alias)) {
            res.add(getTableName(entityClass, namingPolicy));
        } else {
            res.add(getTableName(entityClass, namingPolicy) + " " + alias);
        }

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);
        PropInfo propInfo = null;
        Class<?> subEntityClass = null;

        for (String subEntityPropName : subEntityPropNames) {
            if (excludedPropNames != null && excludedPropNames.contains(subEntityPropName)) {
                continue;
            }

            propInfo = entityInfo.getPropInfo(subEntityPropName);
            subEntityClass = (propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type).clazz();
            res.add(getTableName(subEntityClass, namingPolicy));
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
        if (!(op == OperationType.ADD || op == OperationType.QUERY)) {
            throw new RuntimeException("Invalid operation: " + op);
        }

        if (op == OperationType.QUERY) {
            if (N.isNullOrEmpty(propOrColumnNames) && N.isNullOrEmpty(propOrColumnNameAliases) && N.isNullOrEmpty(multiSelects)) {
                throw new RuntimeException("Column names or props must be set first by select");
            }
        } else {
            if (N.isNullOrEmpty(propOrColumnNames) && N.isNullOrEmpty(props) && N.isNullOrEmpty(propsList)) {
                throw new RuntimeException("Column names or props must be set first by insert");
            }
        }

        this.tableName = tableName;

        sb.append(_INSERT);
        sb.append(_SPACE_INTO_SPACE);

        sb.append(tableName);

        sb.append(_SPACE);
        sb.append(WD._PARENTHESES_L);

        if (N.notNullOrEmpty(propOrColumnNames)) {
            int i = 0;
            for (String columnName : propOrColumnNames) {
                if (i++ > 0) {
                    sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);
            }
        } else {
            final Map<String, Object> props = N.isNullOrEmpty(this.props) ? propsList.iterator().next() : this.props;

            int i = 0;
            for (String columnName : props.keySet()) {
                if (i++ > 0) {
                    sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);
            }
        }

        sb.append(WD._PARENTHESES_R);

        sb.append(_SPACE_VALUES_SPACE);

        sb.append(WD._PARENTHESES_L);

        if (N.notNullOrEmpty(propOrColumnNames)) {
            switch (sqlPolicy) {
                case SQL:
                case PARAMETERIZED_SQL: {
                    for (int i = 0, size = propOrColumnNames.size(); i < size; i++) {
                        if (i > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append(WD._QUESTION_MARK);
                    }

                    break;
                }

                case NAMED_SQL: {
                    int i = 0;
                    for (String columnName : propOrColumnNames) {
                        if (i++ > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        handlerForNamedParameter.accept(sb, columnName);
                    }

                    break;
                }

                case IBATIS_SQL: {
                    int i = 0;
                    for (String columnName : propOrColumnNames) {
                        if (i++ > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append("#{");
                        sb.append(columnName);
                        sb.append('}');
                    }

                    break;
                }

                default:
                    throw new RuntimeException("Not supported SQL policy: " + sqlPolicy);
            }
        } else if (N.notNullOrEmpty(props)) {
            appendInsertProps(props);
        } else {
            int i = 0;
            for (Map<String, Object> props : propsList) {
                if (i++ > 0) {
                    sb.append(WD._PARENTHESES_R);
                    sb.append(_COMMA_SPACE);
                    sb.append(WD._PARENTHESES_L);
                }

                appendInsertProps(props);
            }
        }

        sb.append(WD._PARENTHESES_R);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder into(final Class<?> entityClass) {
        if (this.entityClass == null) {
            setEntityClass(entityClass);
        }

        return into(getTableName(entityClass, namingPolicy));
    }

    /**
     *
     * @return
     */
    public SQLBuilder distinct() {
        return preselect(DISTINCT);
    }

    /**
     *
     * @param preselect <code>ALL | DISTINCT | DISTINCTROW...</code>
     * @return
     */
    public SQLBuilder preselect(final String preselect) {
        N.checkArgNotNull(preselect, "preselect");

        if (sb.length() > 0) {
            throw new IllegalStateException("'distinct|preselect' must be called before 'from' operation");
        }

        if (N.isNullOrEmpty(this.preselect)) {
            this.preselect = preselect;
        } else {
            this.preselect += preselect;
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
        final String tableName = idx > 0 ? expr.substring(0, idx) : expr;

        return from(tableName.trim(), expr);
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

        final String tableName = tableNames[0].trim();
        return from(tableName, Strings.join(tableNames, WD.COMMA_SPACE));
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

        final String tableName = tableNames.iterator().next().trim();
        return from(tableName, Strings.join(tableNames, WD.COMMA_SPACE));
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder from(final Class<?> entityClass) {
        if (this.entityClass == null) {
            setEntityClass(entityClass);
        }

        return from(getTableName(entityClass, namingPolicy));
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder from(final Class<?> entityClass, final String alias) {
        if (this.entityClass == null) {
            setEntityClass(entityClass);
        }

        if (N.isNullOrEmpty(alias)) {
            return from(getTableName(entityClass, namingPolicy));
        }
        return from(getTableName(entityClass, namingPolicy) + " " + alias);
    }

    private SQLBuilder from(final Class<?> entityClass, final Collection<String> tableNames) {
        if (this.entityClass == null) {
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
        if (op != OperationType.QUERY) {
            throw new RuntimeException("Invalid operation: " + op);
        }

        hasFromBeenSet = true;

        if (N.isNullOrEmpty(propOrColumnNames) && N.isNullOrEmpty(propOrColumnNameAliases) && N.isNullOrEmpty(multiSelects)) {
            throw new RuntimeException("Column names or props must be set first by select");
        }

        int idx = tableName.indexOf(' ');

        if (idx > 0) {
            this.tableName = tableName.substring(0, idx).trim();
            this.tableAlias = tableName.substring(idx + 1).trim();
        } else {
            this.tableName = tableName.trim();
        }

        if (entityClass != null && N.notNullOrEmpty(tableAlias)) {
            addPropColumnMapForAlias(entityClass, tableAlias);
        }

        sb.append(_SELECT);
        sb.append(_SPACE);

        if (N.notNullOrEmpty(preselect)) {
            sb.append(preselect);
            sb.append(_SPACE);
        }

        final boolean withAlias = N.notNullOrEmpty(tableAlias);
        final boolean isForSelect = op == OperationType.QUERY;

        if (N.notNullOrEmpty(propOrColumnNames)) {
            if (entityClass != null && withAlias == false && propOrColumnNames == QueryUtil.getSelectPropNames(entityClass, false, null)) {
                String fullSelectParts = fullSelectPartsPool.get(namingPolicy).get(entityClass);

                if (N.isNullOrEmpty(fullSelectParts)) {
                    fullSelectParts = "";

                    int i = 0;
                    for (String columnName : propOrColumnNames) {
                        if (i++ > 0) {
                            fullSelectParts += WD.COMMA_SPACE;
                        }

                        fullSelectParts += formalizeColumnName(propColumnNameMap, columnName);

                        if ((namingPolicy != NamingPolicy.LOWER_CAMEL_CASE && namingPolicy != NamingPolicy.NO_CHANGE) && !WD.ASTERISK.equals(columnName)) {
                            fullSelectParts += SPACE_AS_SPACE;

                            fullSelectParts += WD.QUOTATION_D;
                            fullSelectParts += columnName;
                            fullSelectParts += WD.QUOTATION_D;
                        }
                    }

                    fullSelectPartsPool.get(namingPolicy).put(entityClass, fullSelectParts);
                }

                sb.append(fullSelectParts);
            } else {
                int i = 0;
                for (String columnName : propOrColumnNames) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entityClass, entityInfo, propColumnNameMap, tableAlias, columnName, null, false, null, isForSelect);
                }
            }
        } else if (N.notNullOrEmpty(propOrColumnNameAliases)) {
            int i = 0;
            for (Map.Entry<String, String> entry : propOrColumnNameAliases.entrySet()) {
                if (i++ > 0) {
                    sb.append(_COMMA_SPACE);
                }

                appendColumnName(entityClass, entityInfo, propColumnNameMap, tableAlias, entry.getKey(), entry.getValue(), false, null, isForSelect);
            }
        } else if (N.notNullOrEmpty(multiSelects)) {
            this.aliasPropColumnNameMap = N.newHashMap(multiSelects.size());

            for (Selection selection : multiSelects) {
                if (N.notNullOrEmpty(selection.tableAlias())) {
                    this.aliasPropColumnNameMap.put(selection.tableAlias(), QueryUtil.prop2ColumnNameMap(selection.entityClass(), namingPolicy));
                }
            }

            Class<?> selectionEntityClass = null;
            EntityInfo selectionEntityInfo = null;
            ImmutableMap<String, Tuple2<String, Boolean>> selectionPropColumnNameMap = null;
            String selectionTableAlias = null;
            String selectionClassAlias = null;
            boolean selectionWithClassAlias = false;

            int i = 0;

            for (Selection selection : multiSelects) {
                selectionEntityClass = selection.entityClass();
                selectionEntityInfo = selectionEntityClass == null && ClassUtil.isEntity(selectionEntityClass) == false ? null
                        : ParserUtil.getEntityInfo(selectionEntityClass);
                selectionPropColumnNameMap = selectionEntityClass == null && ClassUtil.isEntity(selectionEntityClass) == false ? null
                        : QueryUtil.prop2ColumnNameMap(selectionEntityClass, namingPolicy);
                selectionTableAlias = selection.tableAlias();

                selectionClassAlias = selection.classAlias();
                selectionWithClassAlias = N.notNullOrEmpty(selectionClassAlias);

                final Collection<String> selectPropNames = N.notNullOrEmpty(selection.selectPropNames()) ? selection.selectPropNames()
                        : QueryUtil.getSelectPropNames(selectionEntityClass, selection.includeSubEntityProperties(), selection.excludedPropNames());

                for (String propName : selectPropNames) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(selectionEntityClass, selectionEntityInfo, selectionPropColumnNameMap, selectionTableAlias, propName, null,
                            selectionWithClassAlias, selectionClassAlias, isForSelect);
                }
            }
        } else {
            throw new UnsupportedOperationException("No select part specified");
        }

        sb.append(_SPACE_FROM_SPACE);

        sb.append(fromCause);

        return this;
    }

    private void addPropColumnMapForAlias(final Class<?> entityClass, final String alias) {
        if (aliasPropColumnNameMap == null) {
            aliasPropColumnNameMap = new HashMap<>();
        }

        if (N.isNullOrEmpty(propColumnNameMap) && entityClass != null && ClassUtil.isEntity(entityClass)) {
            propColumnNameMap = QueryUtil.prop2ColumnNameMap(entityClass, namingPolicy);
        }

        aliasPropColumnNameMap.put(alias, propColumnNameMap);
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder join(final String expr) {
        sb.append(_SPACE_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder join(final Class<?> entityClass) {
        sb.append(_SPACE_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder join(final Class<?> entityClass, final String alias) {
        addPropColumnMapForAlias(entityClass, alias);

        sb.append(_SPACE_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy) + " " + alias);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder innerJoin(final String expr) {
        sb.append(_SPACE_INNER_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder innerJoin(final Class<?> entityClass) {
        sb.append(_SPACE_INNER_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder innerJoin(final Class<?> entityClass, final String alias) {
        addPropColumnMapForAlias(entityClass, alias);

        sb.append(_SPACE_INNER_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy) + " " + alias);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder leftJoin(final String expr) {
        sb.append(_SPACE_LEFT_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder leftJoin(final Class<?> entityClass) {
        sb.append(_SPACE_LEFT_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder leftJoin(final Class<?> entityClass, final String alias) {
        addPropColumnMapForAlias(entityClass, alias);

        sb.append(_SPACE_LEFT_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy) + " " + alias);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder rightJoin(final String expr) {
        sb.append(_SPACE_RIGHT_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder rightJoin(final Class<?> entityClass) {
        sb.append(_SPACE_RIGHT_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder rightJoin(final Class<?> entityClass, final String alias) {
        addPropColumnMapForAlias(entityClass, alias);

        sb.append(_SPACE_RIGHT_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy) + " " + alias);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder fullJoin(final String expr) {
        sb.append(_SPACE_FULL_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder fullJoin(final Class<?> entityClass) {
        sb.append(_SPACE_FULL_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder fullJoin(final Class<?> entityClass, final String alias) {
        addPropColumnMapForAlias(entityClass, alias);

        sb.append(_SPACE_FULL_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy) + " " + alias);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder crossJoin(final String expr) {
        sb.append(_SPACE_CROSS_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder crossJoin(final Class<?> entityClass) {
        sb.append(_SPACE_CROSS_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder crossJoin(final Class<?> entityClass, final String alias) {
        addPropColumnMapForAlias(entityClass, alias);

        sb.append(_SPACE_CROSS_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy) + " " + alias);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder naturalJoin(final String expr) {
        sb.append(_SPACE_NATURAL_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder naturalJoin(final Class<?> entityClass) {
        sb.append(_SPACE_NATURAL_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param entityClass
     * @param alias
     * @return
     */
    public SQLBuilder naturalJoin(final Class<?> entityClass, final String alias) {
        addPropColumnMapForAlias(entityClass, alias);

        sb.append(_SPACE_NATURAL_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy) + " " + alias);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder on(final String expr) {
        sb.append(_SPACE_ON_SPACE);

        appendStringExpr(expr, false);

        return this;
    }

    /**
     *
     * @param cond any literal written in <code>Expression</code> condition won't be formalized
     * @return
     */
    public SQLBuilder on(final Condition cond) {
        sb.append(_SPACE_ON_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder using(final String expr) {
        sb.append(_SPACE_USING_SPACE);

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

        sb.append(_SPACE_WHERE_SPACE);

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

        sb.append(_SPACE_WHERE_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder groupBy(final String expr) {
        sb.append(_SPACE_GROUP_BY_SPACE);

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
        sb.append(_SPACE_GROUP_BY_SPACE);

        for (int i = 0, len = propOrColumnNames.length; i < len; i++) {
            if (i > 0) {
                sb.append(_COMMA_SPACE);
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

        sb.append(_SPACE);
        sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder groupBy(final Collection<String> propOrColumnNames) {
        sb.append(_SPACE_GROUP_BY_SPACE);

        int i = 0;
        for (String columnName : propOrColumnNames) {
            if (i++ > 0) {
                sb.append(_COMMA_SPACE);
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

        sb.append(_SPACE);
        sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param orders
     * @return
     */
    public SQLBuilder groupBy(final Map<String, SortDirection> orders) {
        sb.append(_SPACE_GROUP_BY_SPACE);

        int i = 0;
        for (Map.Entry<String, SortDirection> entry : orders.entrySet()) {
            if (i++ > 0) {

                sb.append(_COMMA_SPACE);
            }

            appendColumnName(entry.getKey());

            sb.append(_SPACE);
            sb.append(entry.getValue().toString());
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder having(final String expr) {
        sb.append(_SPACE_HAVING_SPACE);

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
        sb.append(_SPACE_HAVING_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder orderBy(final String expr) {
        sb.append(_SPACE_ORDER_BY_SPACE);

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
        sb.append(_SPACE_ORDER_BY_SPACE);

        for (int i = 0, len = propOrColumnNames.length; i < len; i++) {
            if (i > 0) {
                sb.append(_COMMA_SPACE);
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

        sb.append(_SPACE);
        sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param propOrColumnNames
     * @return
     */
    public SQLBuilder orderBy(final Collection<String> propOrColumnNames) {
        sb.append(_SPACE_ORDER_BY_SPACE);

        int i = 0;
        for (String columnName : propOrColumnNames) {
            if (i++ > 0) {
                sb.append(_COMMA_SPACE);
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

        sb.append(_SPACE);
        sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param orders
     * @return
     */
    public SQLBuilder orderBy(final Map<String, SortDirection> orders) {
        sb.append(_SPACE_ORDER_BY_SPACE);

        int i = 0;

        for (Map.Entry<String, SortDirection> entry : orders.entrySet()) {
            if (i++ > 0) {
                sb.append(_COMMA_SPACE);
            }

            appendColumnName(entry.getKey());

            sb.append(_SPACE);
            sb.append(entry.getValue().toString());
        }

        return this;
    }

    /**
     *
     * @param count
     * @return
     */
    public SQLBuilder limit(final int count) {
        sb.append(_SPACE_LIMIT_SPACE);

        sb.append(count);

        return this;
    }

    /**
     *
     * @param offset
     * @param count
     * @return
     */
    public SQLBuilder limit(final int offset, final int count) {
        sb.append(_SPACE_LIMIT_SPACE);

        sb.append(count);

        sb.append(_SPACE_OFFSET_SPACE);

        sb.append(offset);

        return this;
    }

    /**
     *
     * @param offset
     * @return
     */
    public SQLBuilder offset(final int offset) {
        sb.append(_SPACE_OFFSET_SPACE).append(offset);

        return this;
    }

    /**
     *
     * @param offset
     * @return
     */
    public SQLBuilder offsetRows(final int offset) {
        sb.append(_SPACE_OFFSET_SPACE).append(offset).append(_SPACE_ROWS_SPACE);

        return this;
    }

    public SQLBuilder fetchNextNRowsOnly(final int n) {
        sb.append(" FETCH NEXT ").append(n).append(" ROWS ONLY");

        return this;
    }

    public SQLBuilder fetchFirstNRowsOnly(final int n) {
        sb.append(" FETCH FIRST ").append(n).append(" ROWS ONLY");

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
                    sb.append(_SPACE).append(join.getOperator()).append(_SPACE);

                    if (join.getJoinEntities().size() == 1) {
                        sb.append(join.getJoinEntities().get(0));
                    } else {
                        sb.append(WD._PARENTHESES_L);
                        int idx = 0;

                        for (String joinTableName : join.getJoinEntities()) {
                            if (idx++ > 0) {
                                sb.append(_COMMA_SPACE);
                            }

                            sb.append(joinTableName);
                        }

                        sb.append(WD._PARENTHESES_R);
                    }

                    appendCondition(join.getCondition());
                }
            }

            final Cell where = criteria.getWhere();

            if ((where != null)) {
                sb.append(_SPACE_WHERE_SPACE);
                appendCondition(where.getCondition());
            }

            final Cell groupBy = criteria.getGroupBy();

            if (groupBy != null) {
                sb.append(_SPACE_GROUP_BY_SPACE);
                appendCondition(groupBy.getCondition());
            }

            final Cell having = criteria.getHaving();

            if (having != null) {
                sb.append(_SPACE_HAVING_SPACE);
                appendCondition(having.getCondition());
            }

            List<Cell> aggregations = criteria.getAggregation();

            if (N.notNullOrEmpty(aggregations)) {
                for (Cell aggregation : aggregations) {
                    sb.append(_SPACE).append(aggregation.getOperator()).append(_SPACE);
                    appendCondition(aggregation.getCondition());
                }
            }

            final Cell orderBy = criteria.getOrderBy();

            if (orderBy != null) {
                sb.append(_SPACE_ORDER_BY_SPACE);
                appendCondition(orderBy.getCondition());
            }

            final Limit limit = criteria.getLimit();

            if (limit != null) {
                if (N.notNullOrEmpty(limit.getExpr())) {
                    sb.append(_SPACE).append(limit.getExpr());
                } else if (limit.getOffset() > 0) {
                    limit(limit.getOffset(), limit.getCount());
                } else {
                    limit(limit.getCount());
                }
            }
        } else if (cond instanceof Clause) {
            sb.append(_SPACE).append(cond.getOperator()).append(_SPACE);
            appendCondition(((Clause) cond).getCondition());
        } else {
            if (!isForConditionOnly) {
                sb.append(_SPACE_WHERE_SPACE);
            }

            appendCondition(cond);
        }

        return this;
    }

    public SQLBuilder append(final String expr) {
        sb.append(expr);

        return this;
    }

    public SQLBuilder appendIf(final boolean b, final Condition cond) {
        if (b) {
            append(cond);
        }

        return this;
    }

    public SQLBuilder appendIf(final boolean b, final String expr) {
        if (b) {
            append(expr);
        }

        return this;
    }

    @Beta
    public SQLBuilder appendIf(final boolean b, final java.util.function.Consumer<SQLBuilder> append) {
        if (b) {
            append.accept(this);
        }

        return this;
    }

    @Beta
    public SQLBuilder appendIfOrElse(final boolean b, final Condition condToAppendForTrue, final Condition condToAppendForFalse) {
        if (b) {
            append(condToAppendForTrue);
        } else {
            append(condToAppendForFalse);
        }

        return this;
    }

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
            parameters.addAll(sqlBuilder.parameters());
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
        op = OperationType.QUERY;

        this.propOrColumnNames = Array.asList(propOrColumnNames);
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_UNION_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            sb.append(propOrColumnNames[0]);

            this.propOrColumnNames = null;
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
        op = OperationType.QUERY;

        this.propOrColumnNames = propOrColumnNames;
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_UNION_SPACE);

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
            parameters.addAll(sqlBuilder.parameters());
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
        op = OperationType.QUERY;

        this.propOrColumnNames = Array.asList(propOrColumnNames);
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_UNION_ALL_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            sb.append(propOrColumnNames[0]);

            this.propOrColumnNames = null;
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
        op = OperationType.QUERY;

        this.propOrColumnNames = propOrColumnNames;
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_UNION_ALL_SPACE);

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
            parameters.addAll(sqlBuilder.parameters());
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
        op = OperationType.QUERY;

        this.propOrColumnNames = Array.asList(propOrColumnNames);
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_INTERSECT_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            sb.append(propOrColumnNames[0]);

            this.propOrColumnNames = null;
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
        op = OperationType.QUERY;

        this.propOrColumnNames = propOrColumnNames;
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_INTERSECT_SPACE);

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
            parameters.addAll(sqlBuilder.parameters());
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
        op = OperationType.QUERY;

        this.propOrColumnNames = Array.asList(propOrColumnNames);
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_EXCEPT_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            sb.append(propOrColumnNames[0]);

            this.propOrColumnNames = null;
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
        op = OperationType.QUERY;

        this.propOrColumnNames = propOrColumnNames;
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_EXCEPT_SPACE);

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
            parameters.addAll(sqlBuilder.parameters());
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
        op = OperationType.QUERY;

        this.propOrColumnNames = Array.asList(propOrColumnNames);
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_EXCEPT2_SPACE);

        // it's sub query
        if (isSubQuery(propOrColumnNames)) {
            sb.append(propOrColumnNames[0]);

            this.propOrColumnNames = null;
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
        op = OperationType.QUERY;

        this.propOrColumnNames = propOrColumnNames;
        this.propOrColumnNameAliases = null;

        sb.append(_SPACE_EXCEPT2_SPACE);

        return this;
    }

    public SQLBuilder forUpdate() {
        sb.append(_SPACE_FOR_UPDATE);

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

        switch (sqlPolicy) {
            case SQL:
            case PARAMETERIZED_SQL: {
                int i = 0;
                for (String columnName : propOrColumnNames) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(columnName);

                    if (columnName.indexOf('=') < 0) {
                        sb.append(" = ?");
                    }
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                for (String columnName : propOrColumnNames) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(columnName);

                    if (columnName.indexOf('=') < 0) {
                        sb.append(" = :");
                        sb.append(columnName);
                    }
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                for (String columnName : propOrColumnNames) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(columnName);

                    if (columnName.indexOf('=') < 0) {
                        sb.append(" = #{");
                        sb.append(columnName);
                        sb.append('}');
                    }
                }

                break;
            }

            default:
                throw new RuntimeException("Not supported SQL policy: " + sqlPolicy);
        }

        this.propOrColumnNames = null;

        return this;
    }

    /**
     *
     * @param props
     * @return
     */
    public SQLBuilder set(final Map<String, Object> props) {
        init(false);

        switch (sqlPolicy) {
            case SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForSQL(entry.getValue());
                }

                break;
            }

            case PARAMETERIZED_SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForRawSQL(entry.getValue());
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForNamedSQL(entry.getKey(), entry.getValue());
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForIbatisNamedSQL(entry.getKey(), entry.getValue());
                }

                break;
            }

            default:
                throw new RuntimeException("Not supported SQL policy: " + sqlPolicy);
        }

        this.propOrColumnNames = null;

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
            final Map<String, Object> props = new LinkedHashMap<>((Map<String, Object>) entity);
            Maps.removeKeys(props, excludedPropNames);
            return set(props);
        }

        final Class<?> entityClass = entity.getClass();
        setEntityClass(entityClass);
        final Collection<String> propNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);
        final Map<String, Object> props = N.newHashMap(propNames.size());
        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);

        for (String propName : propNames) {
            props.put(propName, entityInfo.getPropValue(entity, propName));
        }

        return set(props);
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
        if (sb == null) {
            throw new RuntimeException("This SQLBuilder has been closed after sql() was called previously");
        }

        init(true);

        String sql = null;

        try {
            sql = sb.charAt(0) == ' ' ? sb.substring(1) : sb.toString();
        } finally {
            Objectory.recycle(sb);
            sb = null;

            activeStringBuilderCounter.decrementAndGet();
        }

        //    if (logger.isDebugEnabled()) {
        //        logger.debug(sql);
        //    }

        return sql;
    }

    public List<Object> parameters() {
        return parameters;
    }

    /**
     *  This SQLBuilder will be closed after <code>pair()</code> is called.
     *
     * @return
     */
    public SP pair() {
        final String sql = sql();

        return new SP(sql, parameters);
    }

    @Beta
    public <T, E extends Exception> T apply(final Throwables.Function<? super SP, T, E> func) throws E {
        return func.apply(this.pair());
    }

    @Beta
    public <T, E extends Exception> T apply(final Throwables.BiFunction<? super String, ? super List<Object>, T, E> func) throws E {
        final SP sp = this.pair();

        return func.apply(sp.sql, sp.parameters);
    }

    @Beta
    public <E extends Exception> void accept(final Throwables.Consumer<? super SP, E> consumer) throws E {
        consumer.accept(this.pair());
    }

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

        if (sb.length() > 0) {
            return;
        }

        if (op == OperationType.UPDATE) {
            sb.append(_UPDATE);

            sb.append(_SPACE);
            sb.append(tableName);

            sb.append(_SPACE_SET_SPACE);

            if (setForUpdate && N.notNullOrEmpty(propOrColumnNames)) {
                set(propOrColumnNames);
            }
        } else if (op == OperationType.DELETE) {
            final String newTableName = tableName;

            char[] deleteFromTableChars = tableDeleteFrom.get(newTableName);

            if (deleteFromTableChars == null) {
                deleteFromTableChars = (WD.DELETE + WD.SPACE + WD.FROM + WD.SPACE + newTableName).toCharArray();
                tableDeleteFrom.put(newTableName, deleteFromTableChars);
            }

            sb.append(deleteFromTableChars);
        } else if (op == OperationType.QUERY && hasFromBeenSet == false && isForConditionOnly == false) {
            throw new RuntimeException("'from' methods has not been called for query: " + op);
        }
    }

    private void setEntityClass(final Class<?> entityClass) {
        this.entityClass = entityClass;

        if (entityClass != null && ClassUtil.isEntity(entityClass)) {
            this.entityInfo = ParserUtil.getEntityInfo(entityClass);
            this.propColumnNameMap = QueryUtil.prop2ColumnNameMap(entityClass, namingPolicy);
        } else {
            this.entityInfo = null;
            this.propColumnNameMap = null;
        }
    }

    /**
     * Sets the parameter for SQL.
     *
     * @param propValue the new parameter for SQL
     */
    private void setParameterForSQL(final Object propValue) {
        if (CF.QME.equals(propValue)) {
            sb.append(WD._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            sb.append(Expression.formalize(propValue));
        }
    }

    /**
     * Sets the parameter for raw SQL.
     *
     * @param propValue the new parameter for raw SQL
     */
    private void setParameterForRawSQL(final Object propValue) {
        if (CF.QME.equals(propValue)) {
            sb.append(WD._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            sb.append(WD._QUESTION_MARK);

            parameters.add(propValue);
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
            handlerForNamedParameter.accept(sb, propName);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            handlerForNamedParameter.accept(sb, propName);

            parameters.add(propValue);
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
            sb.append("#{");
            sb.append(propName);
            sb.append('}');
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            sb.append("#{");
            sb.append(propName);
            sb.append('}');

            parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter.
     *
     * @param propName
     * @param propValue
     */
    private void setParameter(final String propName, final Object propValue) {
        switch (sqlPolicy) {
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
                throw new RuntimeException("Not supported SQL policy: " + sqlPolicy);
        }
    }

    /**
     * Append insert props.
     *
     * @param props
     */
    private void appendInsertProps(final Map<String, Object> props) {
        switch (sqlPolicy) {
            case SQL: {
                int i = 0;
                Object propValue = null;
                for (String propName : props.keySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    propValue = props.get(propName);

                    setParameterForSQL(propValue);
                }

                break;
            }

            case PARAMETERIZED_SQL: {
                int i = 0;
                Object propValue = null;
                for (String propName : props.keySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    propValue = props.get(propName);

                    setParameterForRawSQL(propValue);
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                Object propValue = null;
                for (String propName : props.keySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    propValue = props.get(propName);

                    setParameterForNamedSQL(propName, propValue);
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                Object propValue = null;
                for (String propName : props.keySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    propValue = props.get(propName);

                    setParameterForIbatisNamedSQL(propName, propValue);
                }

                break;
            }

            default:
                throw new RuntimeException("Not supported SQL policy: " + sqlPolicy);
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

            sb.append(_SPACE);
            sb.append(binary.getOperator().toString());
            sb.append(_SPACE);

            Object propValue = binary.getPropValue();
            setParameter(propName, propValue);
        } else if (cond instanceof Between) {
            final Between bt = (Between) cond;
            final String propName = bt.getPropName();

            appendColumnName(propName);

            sb.append(_SPACE);
            sb.append(bt.getOperator().toString());
            sb.append(_SPACE);

            Object minValue = bt.getMinValue();
            if (sqlPolicy == SQLPolicy.NAMED_SQL || sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("min" + Strings.capitalize(propName), minValue);
            } else {
                setParameter(propName, minValue);
            }

            sb.append(_SPACE);
            sb.append(WD.AND);
            sb.append(_SPACE);

            Object maxValue = bt.getMaxValue();
            if (sqlPolicy == SQLPolicy.NAMED_SQL || sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("max" + Strings.capitalize(propName), maxValue);
            } else {
                setParameter(propName, maxValue);
            }
        } else if (cond instanceof NotBetween) {
            final NotBetween bt = (NotBetween) cond;
            final String propName = bt.getPropName();

            appendColumnName(propName);

            sb.append(_SPACE);
            sb.append(bt.getOperator().toString());
            sb.append(_SPACE);

            Object minValue = bt.getMinValue();
            if (sqlPolicy == SQLPolicy.NAMED_SQL || sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("min" + Strings.capitalize(propName), minValue);
            } else {
                setParameter(propName, minValue);
            }

            sb.append(_SPACE);
            sb.append(WD.AND);
            sb.append(_SPACE);

            Object maxValue = bt.getMaxValue();
            if (sqlPolicy == SQLPolicy.NAMED_SQL || sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("max" + Strings.capitalize(propName), maxValue);
            } else {
                setParameter(propName, maxValue);
            }
        } else if (cond instanceof In) {
            final In in = (In) cond;
            final String propName = in.getPropName();
            final List<Object> parameters = in.getParameters();

            appendColumnName(propName);

            sb.append(_SPACE);
            sb.append(in.getOperator().toString());
            sb.append(WD.SPACE_PARENTHESES_L);

            for (int i = 0, len = parameters.size(); i < len; i++) {
                if (i > 0) {
                    sb.append(WD.COMMA_SPACE);
                }

                if (sqlPolicy == SQLPolicy.NAMED_SQL || sqlPolicy == SQLPolicy.IBATIS_SQL) {
                    setParameter(propName + (i + 1), parameters.get(i));
                } else {
                    setParameter(propName, parameters.get(i));
                }
            }

            sb.append(WD._PARENTHESES_R);
        } else if (cond instanceof InSubQuery) {
            final InSubQuery inSubQuery = (InSubQuery) cond;
            final String propName = inSubQuery.getPropName();

            if (N.notNullOrEmpty(propName)) {
                appendColumnName(propName);
            } else {
                sb.append(WD._PARENTHESES_L);

                int idx = 0;

                for (String e : inSubQuery.getPropNames()) {
                    if (idx++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(e);
                }

                sb.append(WD._PARENTHESES_R);
            }

            sb.append(_SPACE);
            sb.append(inSubQuery.getOperator().toString());

            sb.append(WD.SPACE_PARENTHESES_L);

            appendCondition(inSubQuery.getSubQuery());

            sb.append(WD._PARENTHESES_R);
        } else if (cond instanceof NotIn) {
            final NotIn notIn = (NotIn) cond;
            final String propName = notIn.getPropName();
            final List<Object> parameters = notIn.getParameters();

            appendColumnName(propName);

            sb.append(_SPACE);
            sb.append(notIn.getOperator().toString());
            sb.append(WD.SPACE_PARENTHESES_L);

            for (int i = 0, len = parameters.size(); i < len; i++) {
                if (i > 0) {
                    sb.append(WD.COMMA_SPACE);
                }

                if (sqlPolicy == SQLPolicy.NAMED_SQL || sqlPolicy == SQLPolicy.IBATIS_SQL) {
                    setParameter(propName + (i + 1), parameters.get(i));
                } else {
                    setParameter(propName, parameters.get(i));
                }
            }

            sb.append(WD._PARENTHESES_R);
        } else if (cond instanceof NotInSubQuery) {
            final NotInSubQuery notInSubQuery = (NotInSubQuery) cond;
            final String propName = notInSubQuery.getPropName();

            appendColumnName(propName);

            sb.append(_SPACE);
            sb.append(notInSubQuery.getOperator().toString());
            sb.append(WD.SPACE_PARENTHESES_L);

            appendCondition(notInSubQuery.getSubQuery());

            sb.append(WD._PARENTHESES_R);
        } else if (cond instanceof Where || cond instanceof Having) {
            final Cell cell = (Cell) cond;

            sb.append(_SPACE);
            sb.append(cell.getOperator().toString());
            sb.append(_SPACE);

            appendCondition(cell.getCondition());
        } else if (cond instanceof Cell) {
            final Cell cell = (Cell) cond;

            sb.append(_SPACE);
            sb.append(cell.getOperator().toString());
            sb.append(_SPACE);

            sb.append(_PARENTHESES_L);
            appendCondition(cell.getCondition());
            sb.append(_PARENTHESES_R);
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
                        sb.append(_SPACE);
                        sb.append(junction.getOperator().toString());
                        sb.append(_SPACE);
                    }

                    sb.append(_PARENTHESES_L);

                    appendCondition(conditionList.get(i));

                    sb.append(_PARENTHESES_R);
                }

                // sb.append(_PARENTHESES_R);
            }
        } else if (cond instanceof SubQuery) {
            final SubQuery subQuery = (SubQuery) cond;
            final Condition subCond = subQuery.getCondition();

            if (N.notNullOrEmpty(subQuery.getSql())) {
                sb.append(subQuery.getSql());
            } else if (subQuery.getEntityClass() != null) {
                if (this instanceof SCSB) {
                    sb.append(SCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof PSC) {
                    sb.append(PSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof MSC) {
                    sb.append(MSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof NSC) {
                    sb.append(NSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof ACSB) {
                    sb.append(ACSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof PAC) {
                    sb.append(PAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof MAC) {
                    sb.append(MAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof NAC) {
                    sb.append(NAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof LCSB) {
                    sb.append(LCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof PLC) {
                    sb.append(PLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof MLC) {
                    sb.append(MLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof NLC) {
                    sb.append(NLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof PSB) {
                    sb.append(PSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else if (this instanceof NSB) {
                    sb.append(NSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass()).append(subCond).sql());
                } else {
                    throw new RuntimeException("Unsupproted subQuery condition: " + cond);
                }
            } else if (this instanceof SCSB) {
                sb.append(SCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof PSC) {
                sb.append(PSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof MSC) {
                sb.append(MSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof NSC) {
                sb.append(NSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof ACSB) {
                sb.append(ACSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof PAC) {
                sb.append(PAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof MAC) {
                sb.append(MAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof NAC) {
                sb.append(NAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof LCSB) {
                sb.append(LCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof PLC) {
                sb.append(PLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof MLC) {
                sb.append(MLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof NLC) {
                sb.append(NLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof PSB) {
                sb.append(PSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
            } else if (this instanceof NSB) {
                sb.append(NSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).append(subCond).sql());
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
                    sb.append(formalizeColumnName(expr, namingPolicy));
                } else {
                    sb.append(formalizeColumnName(propColumnNameMap, expr));
                }

                return;
            }
        }

        final List<String> words = SQLParser.parse(expr);

        String word = null;
        for (int i = 0, len = words.size(); i < len; i++) {
            word = words.get(i);

            if (!Strings.isAsciiAlpha(word.charAt(0))) {
                sb.append(word);
            } else if (SQLParser.isFunctionName(words, len, i)) {
                sb.append(word);
            } else {
                sb.append(formalizeColumnName(propColumnNameMap, word));
            }
        }
    }

    private void appendColumnName(final String propName) {
        appendColumnName(entityClass, entityInfo, propColumnNameMap, tableAlias, propName, null, false, null, false);
    }

    private void appendColumnName(final Class<?> entityClass, final EntityInfo entityInfo,
            final ImmutableMap<String, Tuple2<String, Boolean>> propColumnNameMap, final String tableAlias, final String propName, final String propAlias,
            final boolean withClassAlias, final String classAlias, final boolean isForSelect) {
        Tuple2<String, Boolean> tp = propColumnNameMap == null ? null : propColumnNameMap.get(propName);

        if (tp != null) {
            if (tp._2.booleanValue() && tableAlias != null && tableAlias.length() > 0) {
                sb.append(tableAlias).append(WD._PERIOD);
            }

            sb.append(tp._1);

            if (isForSelect && namingPolicy != NamingPolicy.NO_CHANGE) {
                sb.append(_SPACE_AS_SPACE);
                sb.append(WD._QUOTATION_D);

                if (withClassAlias) {
                    sb.append(classAlias).append(WD._PERIOD);
                }

                sb.append(N.notNullOrEmpty(propAlias) ? propAlias : propName);
                sb.append(WD._QUOTATION_D);
            }

            return;
        }

        if (N.isNullOrEmpty(propAlias) && entityInfo != null) {
            final PropInfo propInfo = entityInfo.getPropInfo(propName);

            if (isEntityProp(propInfo)) {
                final Class<?> propEntityClass = propInfo.type.isCollection() ? propInfo.type.getElementType().clazz() : propInfo.clazz;
                final String propEntityTableName = getTableName(propEntityClass, namingPolicy);
                final ImmutableMap<String, Tuple2<String, Boolean>> subPropColumnNameMap = QueryUtil.prop2ColumnNameMap(propEntityClass, namingPolicy);

                final Collection<String> subSelectPropNames = QueryUtil.getSelectPropNames(propEntityClass, false, null);
                int i = 0;

                for (String subPropName : subSelectPropNames) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    sb.append(propEntityTableName).append(WD._PERIOD).append(subPropColumnNameMap.get(subPropName)._1);

                    if (isForSelect) {
                        sb.append(_SPACE_AS_SPACE);
                        sb.append(WD._QUOTATION_D);
                        sb.append(propInfo.name).append(WD._PERIOD).append(subPropName);
                        sb.append(WD._QUOTATION_D);
                    }
                }

                return;
            }
        }

        if (aliasPropColumnNameMap != null && aliasPropColumnNameMap.size() > 0) {
            int index = propName.indexOf('.');

            if (index > 0) {
                final String propTableAlias = propName.substring(0, index);
                final Map<String, Tuple2<String, Boolean>> newPropColumnNameMap = aliasPropColumnNameMap.get(propTableAlias);

                if (newPropColumnNameMap != null) {
                    final String newPropName = propName.substring(index + 1);
                    tp = newPropColumnNameMap.get(newPropName);

                    if (tp != null) {
                        sb.append(propTableAlias).append('.').append(tp._1);

                        if (isForSelect && namingPolicy != NamingPolicy.NO_CHANGE) {
                            sb.append(_SPACE_AS_SPACE);
                            sb.append(WD._QUOTATION_D);

                            if (withClassAlias) {
                                sb.append(classAlias).append(WD._PERIOD);
                            }

                            sb.append(N.notNullOrEmpty(propAlias) ? propAlias : propName);
                            sb.append(WD._QUOTATION_D);
                        }

                        return;
                    }
                }
            }
        }

        if (N.notNullOrEmpty(propAlias)) {
            appendStringExpr(propName, true);

            if (isForSelect && namingPolicy != NamingPolicy.NO_CHANGE) {
                sb.append(_SPACE_AS_SPACE);
                sb.append(WD._QUOTATION_D);

                if (withClassAlias) {
                    sb.append(classAlias).append(WD._PERIOD);
                }

                sb.append(propAlias);
                sb.append(WD._QUOTATION_D);
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

                if (namingPolicy != NamingPolicy.NO_CHANGE && propName.charAt(propName.length() - 1) != '*') {
                    sb.append(_SPACE_AS_SPACE);
                    sb.append(WD._QUOTATION_D);

                    if (withClassAlias) {
                        sb.append(classAlias).append(WD._PERIOD);
                    }

                    sb.append(propName);
                    sb.append(WD._QUOTATION_D);
                }
            }
        } else {
            appendStringExpr(propName, true);
        }
    }

    private static boolean isEntityProp(final PropInfo propInfo) {
        // final ImmutableSet<String> nonSubEntityPropNames = nonSubEntityPropNamesPool.get(entityClass);

        return propInfo != null
                && (!propInfo.isMarkedToColumn && (propInfo.type.isEntity() || (propInfo.type.isCollection() && propInfo.type.getElementType().isEntity())));
    }

    /**
     * Checks if is sub query.
     *
     * @param propOrColumnNames
     * @return true, if is sub query
     */
    private boolean isSubQuery(final String... propOrColumnNames) {
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

    public void println() {
        N.println(sql());
    }

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
            if (tp._2.booleanValue() && tableAlias != null && tableAlias.length() > 0) {
                return tableAlias + "." + tp._1;
            }
            return tp._1;
        }

        if (aliasPropColumnNameMap != null && aliasPropColumnNameMap.size() > 0) {
            int index = propName.indexOf('.');

            if (index > 0) {
                final String propTableAlias = propName.substring(0, index);
                final Map<String, Tuple2<String, Boolean>> newPropColumnNameMap = aliasPropColumnNameMap.get(propTableAlias);

                if (newPropColumnNameMap != null) {
                    final String newPropName = propName.substring(index + 1);
                    tp = newPropColumnNameMap.get(newPropName);

                    if (tp != null) {
                        return propTableAlias + "." + tp._1;
                    }
                }
            }
        }

        return formalizeColumnName(propName, namingPolicy);
    }

    private static void parseInsertEntity(final SQLBuilder instance, final Object entity, final Set<String> excludedPropNames) {
        if (entity instanceof String) {
            instance.propOrColumnNames = Array.asList((String) entity);
        } else if (entity instanceof Map) {
            if (N.isNullOrEmpty(excludedPropNames)) {
                instance.props = (Map<String, Object>) entity;
            } else {
                instance.props = new LinkedHashMap<>((Map<String, Object>) entity);
                Maps.removeKeys(instance.props, excludedPropNames);
            }
        } else {
            final Collection<String> propNames = QueryUtil.getInsertPropNames(entity, excludedPropNames);
            final Map<String, Object> map = N.newHashMap(propNames.size());
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(entity.getClass());

            for (String propName : propNames) {
                map.put(propName, entityInfo.getPropValue(entity, propName));
            }

            instance.props = map;
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
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);

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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.NO_CHANGE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
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
            instance.op = OperationType.QUERY;
            instance.isForConditionOnly = true;
            instance.append(cond);

            return instance;
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

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         *
         * @param propOrColumnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

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

            instance.op = OperationType.ADD;
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

            instance.op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

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

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && ClassUtil.isEntity(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

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

            instance.op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
            instance.propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(selectPart);
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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = Array.asList(propOrColumnNames);

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNames = propOrColumnNames;

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

            instance.op = OperationType.QUERY;
            instance.propOrColumnNameAliases = propOrColumnNameAliases;

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

            instance.op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance.propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

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
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.LOWER_CAMEL_CASE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance.multiSelects = multiSelects;

            return instance;
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.LOWER_CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        public static SQLBuilder count(final String tableName) {
            return select(COUNT_ALL_LIST).from(tableName);
        }

        public static SQLBuilder count(final Class<?> entityClass) {
            return select(COUNT_ALL_LIST).from(entityClass);
        }
    }

    public static final class SP {
        public final String sql;
        public final List<Object> parameters;

        SP(final String sql, final List<Object> parameters) {
            this.sql = sql;
            this.parameters = ImmutableList.wrap(parameters);
        }

        @Override
        public int hashCode() {
            return N.hashCode(sql) * 31 + N.hashCode(parameters);
        }

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

        @Override
        public String toString() {
            return "{sql=" + sql + ", parameters=" + N.toString(parameters) + "}";
        }
    }

    private static final BiConsumer<StringBuilder, String> defaultHandlerForNamedParameter = (sb, propName) -> sb.append(":").append(propName);
    // private static final BiConsumer<StringBuilder, String> mybatisHandlerForNamedParameter = (sb, propName) -> sb.append("#{").append(propName).append("}");

    private static final ThreadLocal<BiConsumer<StringBuilder, String>> handlerForNamedParameter_TL = ThreadLocal
            .withInitial(() -> defaultHandlerForNamedParameter);

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

                final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);
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
