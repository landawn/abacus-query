/*
 * Copyright (C) 2021 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Immutable;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.NotColumn;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Tuple.Tuple2;

public final class QueryUtil {

    private QueryUtil() {
        // singleton
    }

    private static final Map<Class<?>, ImmutableMap<String, String>> column2PropNameNameMapPool = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, String>>> entityTablePropColumnNameMap = new ObjectPool<>(N.POOL_SIZE);

    private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, Tuple2<String, Boolean>>>> entityTablePropColumnNameMap2 = new ObjectPool<>(
            N.POOL_SIZE);

    /**
     *
     * @param entityClass
     * @param namingPolicy
     * @return
     * @deprecated for internal use only.
     */
    @Deprecated
    @Beta
    public static ImmutableMap<String, Tuple2<String, Boolean>> prop2ColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        Map<NamingPolicy, ImmutableMap<String, Tuple2<String, Boolean>>> namingPropColumnNameMap = entityTablePropColumnNameMap2.get(entityClass);
        ImmutableMap<String, Tuple2<String, Boolean>> result = null;

        if (namingPropColumnNameMap == null || (result = namingPropColumnNameMap.get(namingPolicy)) == null) {
            final ImmutableMap<String, String> prop2ColumnNameMap = getProp2ColumnNameMap(entityClass, namingPolicy);
            final Map<String, Tuple2<String, Boolean>> newProp2ColumnNameMap = N.newHashMap(prop2ColumnNameMap.size() * 2);

            for (Map.Entry<String, String> entry : prop2ColumnNameMap.entrySet()) {
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
     * Gets the column 2 field name map.
     *
     * @param entityClass
     * @return
     */
    public static ImmutableMap<String, String> getColumn2PropNameMap(Class<?> entityClass) {
        ImmutableMap<String, String> result = column2PropNameNameMapPool.get(entityClass);

        if (result == null) {
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);
            final Map<String, String> map = N.newHashMap(entityInfo.propInfoList.size() * 3);

            for (PropInfo propInfo : entityInfo.propInfoList) {
                if (propInfo.columnName.isPresent()) {
                    map.put(propInfo.columnName.get(), propInfo.name);
                    map.put(propInfo.columnName.get().toLowerCase(), propInfo.name);
                    map.put(propInfo.columnName.get().toUpperCase(), propInfo.name);
                }
            }

            result = ImmutableMap.copyOf(map);

            column2PropNameNameMapPool.put(entityClass, result);
        }

        return result;
    }

    /**
     * Gets the prop column name map.
     *
     * @return
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
        N.checkArgNotNull(entityClass);

        if (registeringClasses != null) {
            if (registeringClasses.contains(entityClass)) {
                throw new RuntimeException("Cycling references found among: " + registeringClasses);
            }

            registeringClasses.add(entityClass);
        }

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);
        Map<String, String> propColumnNameMap = N.newHashMap(entityInfo.propInfoList.size() * 2);

        for (PropInfo propInfo : entityInfo.propInfoList) {
            if (propInfo.isTransient || propInfo.isAnnotationPresent(NotColumn.class)) {
                continue;
            }

            if (propInfo.columnName.isPresent()) {
                propColumnNameMap.put(propInfo.name, propInfo.columnName.get());
            } else {
                propColumnNameMap.put(propInfo.name, SQLBuilder.formalizeColumnName(propInfo.name, namingPolicy));

                final Type<?> propType = propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type;

                if (propType.isEntity() && (registeringClasses == null || !registeringClasses.contains(propType.clazz()))) {
                    final Set<Class<?>> newRegisteringClasses = registeringClasses == null ? N.<Class<?>> newLinkedHashSet() : registeringClasses;
                    newRegisteringClasses.add(entityClass);

                    final Map<String, String> subPropColumnNameMap = registerEntityPropColumnNameMap(propType.clazz(), namingPolicy, newRegisteringClasses);

                    if (N.notNullOrEmpty(subPropColumnNameMap)) {
                        final String subTableName = SQLBuilder.getTableName(propType.clazz(), namingPolicy);

                        for (Map.Entry<String, String> entry : subPropColumnNameMap.entrySet()) {
                            propColumnNameMap.put(propInfo.name + WD.PERIOD + entry.getKey(), subTableName + WD.PERIOD + entry.getValue());
                        }

                        propColumnNameMap.remove(propInfo.name); // remove sub entity prop.
                    }
                }
            }
        }

        //    final Map<String, String> tmp = entityTablePropColumnNameMap.get(entityClass);
        //
        //    if (N.notNullOrEmpty(tmp)) {
        //        propColumnNameMap.putAll(tmp);
        //    }

        if (N.isNullOrEmpty(propColumnNameMap)) {
            propColumnNameMap = N.<String, String> emptyMap();
        }

        @SuppressWarnings("deprecation")
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
     * Gets the insert prop names by class.
     *
     * @param entity
     * @param excludedPropNames
     * @return
     */
    @Internal
    public static Collection<String> getInsertPropNames(final Object entity, final Set<String> excludedPropNames) {
        final Class<?> entityClass = entity.getClass();

        final Collection<String>[] val = SQLBuilder.loadPropNamesByClass(entityClass);

        if (!N.isNullOrEmpty(excludedPropNames)) {
            final List<String> tmp = new ArrayList<>(val[2]);
            tmp.removeAll(excludedPropNames);
            return tmp;
        }

        final Collection<String> idPropNames = getIdFieldNames(entityClass);

        if (N.isNullOrEmpty(idPropNames)) {
            return val[2];
        }

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);

        for (String idPropName : idPropNames) {
            if (!SQLBuilder.isDefaultIdPropValue(entityInfo.getPropInfo(idPropName))) {
                return val[2];
            }
        }

        return val[3];
    }

    /**
     * Gets the insert prop names by class.
     *
     * @param entityClass
     * @param excludedPropNames
     * @return
     */
    @Internal
    public static Collection<String> getInsertPropNames(final Class<?> entityClass, final Set<String> excludedPropNames) {
        final Collection<String>[] val = SQLBuilder.loadPropNamesByClass(entityClass);
        final Collection<String> propNames = val[2];

        if (N.isNullOrEmpty(excludedPropNames)) {
            return propNames;
        }
        final List<String> tmp = new ArrayList<>(propNames);
        tmp.removeAll(excludedPropNames);
        return tmp;
    }

    /**
     * Gets the select prop names by class.
     *
     * @param entityClass
     * @param includeSubEntityProperties
     * @param excludedPropNames
     * @return
     */
    @Internal
    public static Collection<String> getSelectPropNames(final Class<?> entityClass, final boolean includeSubEntityProperties,
            final Set<String> excludedPropNames) {
        final Collection<String>[] val = SQLBuilder.loadPropNamesByClass(entityClass);
        final Collection<String> propNames = includeSubEntityProperties ? val[0] : val[1];

        if (N.isNullOrEmpty(excludedPropNames)) {
            return propNames;
        }

        return N.excludeAll(propNames, excludedPropNames);
    }

    /**
     * Gets the update prop names by class.
     *
     * @param entityClass
     * @param excludedPropNames
     * @return
     */
    @Internal
    public static Collection<String> getUpdatePropNames(final Class<?> entityClass, final Set<String> excludedPropNames) {
        final Collection<String>[] val = SQLBuilder.loadPropNamesByClass(entityClass);
        final Collection<String> propNames = val[4];

        if (N.isNullOrEmpty(excludedPropNames)) {
            return propNames;
        }
        final List<String> tmp = new ArrayList<>(propNames);
        tmp.removeAll(excludedPropNames);
        return tmp;
    }

    /**
     * Gets the id field names.
     *
     * @param targetClass
     * @return an immutable List.
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    @Immutable
    public static List<String> getIdFieldNames(final Class<?> targetClass) {
        return getIdFieldNames(targetClass, false);
    }

    /**
     * Gets the id field names.
     *
     * @param targetClass
     * @param fakeIdForEmpty
     * @return an immutable List.
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    @Immutable
    public static List<String> getIdFieldNames(final Class<?> targetClass, boolean fakeIdForEmpty) {
        final ImmutableList<String> idPropNames = ParserUtil.getEntityInfo(targetClass).idPropNameList;

        return N.isNullOrEmpty(idPropNames) && fakeIdForEmpty ? fakeIds : idPropNames;
    }

    private static final ImmutableList<String> fakeIds = ImmutableList.of("not_defined_fake_id_in_abacus_" + N.uuid());

    /**
     * Checks if is fake id.
     *
     * @param idPropNames
     * @return true, if is fake id
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    public static boolean isFakeId(List<String> idPropNames) {
        if (idPropNames != null && idPropNames.size() == 1 && fakeIds.get(0).equals(idPropNames.get(0))) {
            return true;
        }

        return false;
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
     * Repeat question mark({@code ?}) {@code n} times with delimiter {@code ", "}.
     * <br />
     * It's designed for batch SQL builder.
     *
     * @param n
     * @return
     */
    public static String repeatQM(int n) {
        N.checkArgNotNegative(n, "count");

        String result = QM_CACHE.get(n);

        if (result == null) {
            result = Strings.repeat("?", n, ", ");
        }

        return result;
    }
}
