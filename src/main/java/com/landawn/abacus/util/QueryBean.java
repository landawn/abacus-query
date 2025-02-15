/*
 * Copyright (c) 2022, Haiyang Li.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import com.landawn.abacus.condition.Condition;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.condition.OrderBy;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.stream.Stream.StreamEx;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

// TODO
/**
 *
 *
 * @see <a href="https://github.com/troyzhxu/bean-searcher">bean-searcher in github</a>
 * @see <a href="https://gitee.com/troyzhxu/bean-searcher">bean-searcher in gitee</a>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class QueryBean {

    private static final Type<Object> strType = Type.of(String.class);
    private static final Type<Collection<Object>> strListType = Type.of("List<String>");
    private static final BiFunction<Object, String, Collection<Object>> toCollectionFunc = (val,
            param) -> val instanceof Collection ? ((Collection<Object>) val) : strListType.valueOf(param);

    private List<String> select;
    private String from;
    private List<FilterBean> where;
    private List<FilterBean> having;
    private List<OrderByBean> orderBy;
    private boolean distinct;
    private Integer offset;
    private Integer limit;

    //    /**
    //     *
    //     *
    //     * @param resultEntityClass
    //     * @return
    //     */
    //    public List<Join> getJoinConditions(@SuppressWarnings("unused") final Class<?> resultEntityClass) {
    //        // TODO
    //        throw new UnsupportedOperationException();
    //    }

    @Accessors(fluent = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class FilterBean {
        private String fieldName;
        private String fieldType;
        private Operator operator;
        private String parameter;

        private List<FilterBean> and;
        private List<FilterBean> or;

        /**
         *
         *
         * @return
         */
        public Condition toCondition() {
            return toCondition((BeanInfo) null);
        }

        /**
         *
         *
         * @param resultEntityClass
         * @return
         */
        public Condition toCondition(final Class<?> resultEntityClass) {
            return toCondition(ParserUtil.getBeanInfo(resultEntityClass));
        }

        /**
         *
         *
         * @param beanInfo
         * @return
         */
        public Condition toCondition(final BeanInfo beanInfo) {
            if (N.notEmpty(and) && N.notEmpty(or)) {
                throw new IllegalArgumentException("'and'/'or' can't have values at the same time");
            }

            Type<Object> propType = strType;

            if (Strings.isNotEmpty(fieldType)) {
                propType = Type.of(fieldType);
            } else if (beanInfo != null) {
                final PropInfo propInfo = beanInfo.getPropInfo(fieldName);

                if (propInfo != null) {
                    propType = propInfo.type;
                }
            }

            final Object propVal = propType.valueOf(parameter);

            Condition cond = null;

            switch (operator) {
                case equals:
                    cond = CF.eq(fieldName, propVal);
                    break;

                case not_equals:
                    cond = CF.ne(fieldName, propVal);
                    break;

                case greater_than:
                case after:
                    cond = CF.gt(fieldName, propVal);
                    break;

                case greater_than_or_equals:
                case not_less_than:
                case not_before:
                    cond = CF.ge(fieldName, propVal);
                    break;

                case less_than:
                case before:
                    cond = CF.lt(fieldName, propVal);
                    break;

                case less_than_or_equals:
                case not_greater_than:
                case not_after:
                    cond = CF.le(fieldName, propVal);
                    break;

                case starts_with:
                    cond = CF.startsWith(fieldName, propVal);
                    break;

                case not_starts_with:
                    cond = CF.notStartsWith(fieldName, propVal);
                    break;

                case ends_with:
                    cond = CF.endsWith(fieldName, propVal);
                    break;

                case not_ends_with:
                    cond = CF.notEndsWith(fieldName, propVal);
                    break;

                case contains:
                    cond = CF.like(fieldName, propVal);
                    break;

                case not_contains:
                    cond = CF.notLike(fieldName, propVal);
                    break;

                case in: {
                    final Collection<Object> c = toCollectionFunc.apply(propVal, parameter);
                    cond = CF.in(fieldName, c);
                    break;
                }

                case not_in: {
                    final Collection<Object> c = toCollectionFunc.apply(propVal, parameter);
                    cond = CF.notIn(fieldName, c);
                    break;
                }

                case between: {
                    final Collection<Object> c = toCollectionFunc.apply(propVal, parameter);
                    cond = CF.between(fieldName, N.getElement(c, 0), N.getElement(c, 1));
                    break;
                }

                case not_between: {
                    final Collection<Object> c = toCollectionFunc.apply(propVal, parameter);
                    cond = CF.notBetween(fieldName, N.getElement(c, 0), N.getElement(c, 1));
                    break;
                }

                default:
                    cond = CF.binary(fieldName, com.landawn.abacus.condition.Operator.getOperator(operator.sqlOperator), propVal);
            }

            if (N.notEmpty(and)) {
                final List<Condition> conditions = new ArrayList<>();
                conditions.add(cond);
                conditions.addAll(N.map(and, it -> it.toCondition(beanInfo)));

                cond = CF.and(conditions);
            } else if (N.notEmpty(or)) {
                final List<Condition> conditions = new ArrayList<>();
                conditions.add(cond);
                conditions.addAll(N.map(or, it -> it.toCondition(beanInfo)));

                cond = CF.or(conditions);
            }

            return cond;
        }
    }

    @Accessors(fluent = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class OrderByBean {
        private String fieldName;
        private List<String> fieldNames;
        private SortDirection sortDirection;

        /**
         *
         *
         * @return
         */
        public OrderBy toOrderBy() {
            if (N.notEmpty(fieldNames)) {
                return CF.orderBy(fieldNames, sortDirection);
            } else {
                return CF.orderBy(fieldName, sortDirection);
            }
        }
    }

    private static final QuadFunction<StringBuilder, String, Operator, Object, String> generalSqlConverter = (sb, column, op, param) -> {
        if (sb == null) {
            if (column == null) {
                if (param instanceof Number) {
                    return op.sqlOperator + " " + N.stringOf(param);
                } else {
                    return op.sqlOperator + " '" + N.stringOf(param) + "'";
                }

            } else {
                if (param instanceof Number) {
                    return column + " " + op.sqlOperator + " " + N.stringOf(param);
                } else {
                    return column + " " + op.sqlOperator + " '" + N.stringOf(param) + "'";
                }
            }
        } else {
            if (column != null) {
                sb.append(column).append(" ");
            }

            if (param instanceof Number) {
                sb.append(op.sqlOperator).append(" ").append(N.stringOf(param));
            } else {
                sb.append(op.sqlOperator).append(" '").append(N.stringOf(param)).append('\'');
            }

            return null;
        }
    };

    private static final QuadFunction<StringBuilder, String, Operator, Object, String> generalParameterizedSqlConverter = (sb, column, op, param) -> {
        if (sb == null) {
            if (column == null) {
                return op.sqlOperator + " ?";
            } else {
                return column + " " + op.sqlOperator + " ?";
            }
        } else {
            if (column != null) {
                sb.append(column).append(" ");
            }

            sb.append(op.sqlOperator).append(" ?");

            return null;
        }
    };

    private static final QuadFunction<StringBuilder, String, Operator, Object, String> sqlConverterForLike = (sb, column, op, param) -> {
        if (sb == null) {
            if (column == null) {
                return op.sqlOperator + " '%" + N.stringOf(param) + "%'";
            } else {
                return column + " " + op.sqlOperator + " '%" + N.stringOf(param) + "%'";
            }
        } else {
            if (column != null) {
                sb.append(column).append(" ");
            }

            sb.append(op.sqlOperator).append(" '%").append(N.stringOf(param)).append("%'");

            return null;
        }
    };

    private static final QuadFunction<StringBuilder, String, Operator, Object, String> sqlConverterForStartsWith = (sb, column, op, param) -> {
        if (sb == null) {
            if (column == null) {
                return op.sqlOperator + " '" + N.stringOf(param) + "%'";
            } else {
                return column + " " + op.sqlOperator + " '" + N.stringOf(param) + "%'";
            }
        } else {
            if (column != null) {
                sb.append(column).append(" ");
            }

            sb.append(op.sqlOperator).append(" '").append(N.stringOf(param)).append("%'");

            return null;
        }
    };

    private static final QuadFunction<StringBuilder, String, Operator, Object, String> sqlConverterForEndsWith = (sb, column, op, param) -> {
        if (sb == null) {
            if (column == null) {
                return op.sqlOperator + " '%" + N.stringOf(param) + "'";
            } else {
                return column + " " + op.sqlOperator + " '%" + N.stringOf(param) + "'";
            }
        } else {
            if (column != null) {
                sb.append(column).append(" ");
            }

            sb.append(op.sqlOperator).append(" '%").append(N.stringOf(param)).append("'");

            return null;
        }
    };

    private static final QuadFunction<StringBuilder, String, Operator, Object, String> sqlConverterForIn = (sb, column, op, param) -> {
        final Collection<Object> c = param instanceof Collection ? (Collection<Object>) param : toCollectionFunc.apply(param, N.stringOf(param));
        final Optional<Object> firstEle = N.firstNonNull(c);

        String ret = null;

        if (firstEle.isPresent() && firstEle.get() instanceof Number) {
            //noinspection resource,DuplicateExpressions
            ret = StreamEx.of(c).map(N::stringOf).join(", ", (column == null ? "" : column + " ") + op.sqlOperator + " (", ")");
        } else {
            //noinspection resource,DuplicateExpressions
            ret = StreamEx.of(c).map(it -> "'" + N.stringOf(it) + "'").join(", ", (column == null ? "" : column + " ") + op.sqlOperator + " (", ")");
        }

        if (sb != null) {
            sb.append(ret);
        }

        return ret;
    };

    private static final QuadFunction<StringBuilder, String, Operator, Object, String> parameterizedSqlConverterForIn = (sb, column, op, param) -> {
        final Collection<Object> c = param instanceof Collection ? (Collection<Object>) param : toCollectionFunc.apply(param, N.stringOf(param));

        final String ret = (column == null ? "" : column + " ") + op.sqlOperator + " (" + Strings.repeat("?", c.size(), ", ") + ")";

        if (sb != null) {
            sb.append(ret);
        }

        return ret;
    };

    private static final QuadFunction<StringBuilder, String, Operator, Object, String> sqlConverterForBetween = (sb, column, op, param) -> {
        final Collection<Object> c = param instanceof Collection ? (Collection<Object>) param : toCollectionFunc.apply(param, N.stringOf(param));
        final Optional<Object> firstEle = N.firstNonNull(c);

        String ret = null;

        if (sb == null) {
            if (firstEle.isPresent() && firstEle.get() instanceof Number) {
                ret = (column == null ? "" : column + " ") + op.sqlOperator + " (" + firstEle.get() + ", " + N.getElement(c, 1) + ")";
            } else {
                ret = (column == null ? "" : column + " ") + op.sqlOperator + " ('" + N.stringOf(firstEle.get()) + "', '" + N.stringOf(N.getElement(c, 1))
                        + "')";
            }
        } else {
            if (column != null) {
                sb.append(column).append(" ");
            }

            sb.append(op.sqlOperator);

            if (firstEle.isPresent() && firstEle.get() instanceof Number) {
                sb.append(" (").append(firstEle.get()).append(", ").append(N.getElement(c, 1)).append(")");
            } else {
                sb.append(" ('").append(N.stringOf(firstEle.get())).append("', '").append(N.stringOf(N.getElement(c, 1))).append("')");
            }
        }

        return ret;
    };

    private static final QuadFunction<StringBuilder, String, Operator, Object, String> parameterizedSqlConverterForBetween = (sb, column, op, param) -> {
        final String ret = (column == null ? "" : column + " ") + op.sqlOperator + " (?, ?)";

        if (sb != null) {
            sb.append(ret);
        }

        return ret;
    };

    public enum Operator {
        equals("=", generalSqlConverter, generalParameterizedSqlConverter), //
        not_equals("!=", generalSqlConverter, generalParameterizedSqlConverter),
        greater_than(">", generalSqlConverter, generalParameterizedSqlConverter),
        greater_than_or_equals(">=", generalSqlConverter, generalParameterizedSqlConverter),
        not_less_than(">=", generalSqlConverter, generalParameterizedSqlConverter), // same as greater_than_or_equals
        less_than("<", generalSqlConverter, generalParameterizedSqlConverter),
        less_than_or_equals("<=", generalSqlConverter, generalParameterizedSqlConverter),
        not_greater_than("<=", generalSqlConverter, generalParameterizedSqlConverter), // same as less_than_or_equals
        before("<", generalSqlConverter, generalParameterizedSqlConverter), // same as less_than
        not_after("<=", generalSqlConverter, generalParameterizedSqlConverter), // same as less_than_or_equals
        after(">", generalSqlConverter, generalParameterizedSqlConverter), // same greater_than
        not_before(">=", generalSqlConverter, generalParameterizedSqlConverter), // same greater_than_or_equals
        starts_with("LIKE", sqlConverterForStartsWith, generalParameterizedSqlConverter),
        not_starts_with("NOT LIKE", sqlConverterForStartsWith, generalParameterizedSqlConverter), //NOSONAR
        ends_with("LIKE", sqlConverterForEndsWith, generalParameterizedSqlConverter),
        not_ends_with("NOT LIKE", sqlConverterForEndsWith, generalParameterizedSqlConverter),
        contains("LIKE", sqlConverterForLike, generalParameterizedSqlConverter),
        not_contains("NOT LIKE", sqlConverterForLike, generalParameterizedSqlConverter),
        in("IN", sqlConverterForIn, parameterizedSqlConverterForIn),
        not_in("NOT IN", sqlConverterForIn, parameterizedSqlConverterForIn),
        between("BETWEEN", sqlConverterForBetween, parameterizedSqlConverterForBetween),
        not_between("NOT BETWEEN", sqlConverterForBetween, parameterizedSqlConverterForBetween);

        private final String sqlOperator;
        private final QuadFunction<StringBuilder, String, Operator, Object, String> sqlConverter;
        private final QuadFunction<StringBuilder, String, Operator, Object, String> parameterizedSqlConverter;

        Operator(final String sqlOperator, final QuadFunction<StringBuilder, String, Operator, Object, String> sqlConverter,
                final QuadFunction<StringBuilder, String, Operator, Object, String> parameterizedSqlConverter) {
            this.sqlOperator = sqlOperator;
            this.sqlConverter = sqlConverter;
            this.parameterizedSqlConverter = parameterizedSqlConverter;
        }

        /**
         *
         *
         * @return
         */
        public String sqlOperator() {
            return sqlOperator;
        }

        /**
         *
         *
         * @param parameter
         * @return
         */
        public String toSql(final Object parameter) {
            return sqlConverter.apply(null, null, this, parameter);
        }

        /**
         *
         *
         * @param columnName
         * @param parameter
         * @return
         */
        public String toSql(final String columnName, final Object parameter) {
            return sqlConverter.apply(null, columnName, this, parameter);
        }

        /**
         *
         *
         * @param sqlBuilder
         * @param parameter
         */
        public void appendSql(final StringBuilder sqlBuilder, final Object parameter) {
            sqlConverter.apply(sqlBuilder, null, this, parameter);
        }

        /**
         *
         *
         * @param sqlBuilder
         * @param columnName
         * @param parameter
         */
        public void appendSql(final StringBuilder sqlBuilder, final String columnName, final Object parameter) {
            sqlConverter.apply(sqlBuilder, columnName, this, parameter);
        }

        /**
         *
         *
         * @param parameter
         * @return
         */
        public String toParameterizedSql(final Object parameter) {
            return parameterizedSqlConverter.apply(null, null, this, parameter);
        }

        /**
         *
         *
         * @param columnName
         * @param parameter
         * @return
         */
        public String toParameterizedSql(final String columnName, final Object parameter) {
            return parameterizedSqlConverter.apply(null, columnName, this, parameter);
        }

        /**
         *
         *
         * @param sqlBuilder
         * @param parameter
         */
        public void appendParameterizedSql(final StringBuilder sqlBuilder, final Object parameter) {
            parameterizedSqlConverter.apply(sqlBuilder, null, this, parameter);
        }

        /**
         *
         *
         * @param sqlBuilder
         * @param columnName
         * @param parameter
         */
        public void appendParameterizedSql(final StringBuilder sqlBuilder, final String columnName, final Object parameter) {
            parameterizedSqlConverter.apply(sqlBuilder, columnName, this, parameter);
        }
    }

}
