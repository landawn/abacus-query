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

package com.landawn.abacus.query;

import static com.landawn.abacus.util.SK._PARENTHESIS_L;
import static com.landawn.abacus.util.SK._PARENTHESIS_R;
import static com.landawn.abacus.util.SK._SPACE;

import java.util.Collection;
import java.util.List;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.query.condition.AbstractIn;
import com.landawn.abacus.query.condition.AbstractInSubQuery;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Binary;
import com.landawn.abacus.query.condition.Cell;
import com.landawn.abacus.query.condition.Clause;
import com.landawn.abacus.query.condition.ComposableCell;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.Having;
import com.landawn.abacus.query.condition.In;
import com.landawn.abacus.query.condition.InSubQuery;
import com.landawn.abacus.query.condition.Junction;
import com.landawn.abacus.query.condition.NotBetween;
import com.landawn.abacus.query.condition.NotIn;
import com.landawn.abacus.query.condition.NotInSubQuery;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Using;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.OperationType;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * A fluent SQL builder that extends {@link AbstractQueryBuilder} with concrete SQL generation,
 * including condition rendering, operator handling, and NULL semantics.
 *
 * <p>Instances are not thread-safe; create a new builder per thread or per query.
 * Always call {@code build()} to finalize construction and release internal resources.
 *
 * <p>Use one of the predefined {@link Dsl} constants based on the desired parameter style and naming
 * convention. The constant name encodes both the parameter style and the identifier naming policy. The
 * parameter style is given by a leading {@code P} (positional {@code ?}), {@code N} (named {@code :name}),
 * or {@code M} (MyBatis/iBATIS {@code #{name}}); the raw/un-parameterized family (which inlines literal
 * values) has no such leading style letter and instead ends in the literal {@code SB}. The naming policy is
 * encoded by a two-letter code: {@code SB}=no change, {@code SC}=snake_case, {@code AC}=UPPER_CASE_WITH_UNDERSCORE,
 * {@code LC}=lowerCamelCase. For the parameterized families the naming code is the suffix (e.g. {@code PSC}
 * = positional {@code ?} + snake_case), while the raw family puts the naming code first (e.g. {@code SCSB}
 * = snake_case + raw, {@code ACSB} = UPPER_CASE_WITH_UNDERSCORE + raw, {@code LCSB} = lowerCamelCase + raw).
 * The raw-SQL ({@code *CSB}) family is deprecated due to SQL-injection risk; the MyBatis-style ({@code M*})
 * family is also deprecated &mdash; prefer the named ({@code N*}) family instead.
 * <table border="1">
 *   <caption>Predefined Dsl constants</caption>
 *   <tr><th>Constant</th><th>Parameters</th><th>Naming</th><th>Example</th></tr>
 *   <tr><td>{@link Dsl#SCSB}</td><td>inlined values (deprecated)</td><td>snake_case</td><td>{@code SELECT first_name FROM account WHERE id = 1}</td></tr>
 *   <tr><td>{@link Dsl#ACSB}</td><td>inlined values (deprecated)</td><td>UPPER_CASE</td><td>{@code SELECT FIRST_NAME FROM ACCOUNT WHERE ID = 1}</td></tr>
 *   <tr><td>{@link Dsl#LCSB}</td><td>inlined values (deprecated)</td><td>camelCase</td><td>{@code SELECT firstName FROM account WHERE id = 1}</td></tr>
 *   <tr><td>{@link Dsl#PSB}</td><td>{@code ?}</td><td>no change</td><td>{@code SELECT firstName FROM account WHERE id = ?}</td></tr>
 *   <tr><td>{@link Dsl#PSC}</td><td>{@code ?}</td><td>snake_case</td><td>{@code SELECT first_name FROM account WHERE id = ?}</td></tr>
 *   <tr><td>{@link Dsl#PAC}</td><td>{@code ?}</td><td>UPPER_CASE</td><td>{@code SELECT FIRST_NAME FROM ACCOUNT WHERE ID = ?}</td></tr>
 *   <tr><td>{@link Dsl#PLC}</td><td>{@code ?}</td><td>camelCase</td><td>{@code SELECT firstName FROM account WHERE id = ?}</td></tr>
 *   <tr><td>{@link Dsl#NSB}</td><td>{@code :name}</td><td>no change</td><td>{@code SELECT firstName FROM account WHERE id = :id}</td></tr>
 *   <tr><td>{@link Dsl#NSC}</td><td>{@code :name}</td><td>snake_case</td><td>{@code SELECT first_name FROM account WHERE id = :id}</td></tr>
 *   <tr><td>{@link Dsl#NAC}</td><td>{@code :name}</td><td>UPPER_CASE</td><td>{@code SELECT FIRST_NAME FROM ACCOUNT WHERE ID = :id}</td></tr>
 *   <tr><td>{@link Dsl#NLC}</td><td>{@code :name}</td><td>camelCase</td><td>{@code SELECT firstName FROM account WHERE id = :id}</td></tr>
 *   <tr><td>{@link Dsl#MSB}</td><td>{@code #{name}} (deprecated)</td><td>no change</td><td>{@code SELECT firstName FROM account WHERE id = #{id}}</td></tr>
 *   <tr><td>{@link Dsl#MSC}</td><td>{@code #{name}} (deprecated)</td><td>snake_case</td><td>{@code SELECT first_name FROM account WHERE id = #{id}}</td></tr>
 *   <tr><td>{@link Dsl#MAC}</td><td>{@code #{name}} (deprecated)</td><td>UPPER_CASE</td><td>{@code SELECT FIRST_NAME FROM ACCOUNT WHERE ID = #{id}}</td></tr>
 *   <tr><td>{@link Dsl#MLC}</td><td>{@code #{name}} (deprecated)</td><td>camelCase</td><td>{@code SELECT firstName FROM account WHERE id = #{id}}</td></tr>
 * </table>
 *
 * <p><b>Usage examples:</b>
 * <pre>{@code
 * // SELECT with conditions
 * String sql = PSC.select("firstName", "lastName")
 *     .from("users")
 *     .where(Filters.equal("department", "Engineering"))
 *     .orderBy("lastName")
 *     .build().query();
 *
 * // INSERT from entity
 * String sql = PSC.insert(user).into("users").build().query();
 *
 * // UPDATE
 * String sql = PSC.update("users")
 *     .set("status", "lastModified")
 *     .where(Filters.equal("id", userId))
 *     .build().query();
 *
 * // Named parameters (NSC generates :name placeholders)
 * String sql = NSC.select("*")
 *     .from("orders")
 *     .where(Filters.between("orderDate", startDate, endDate))
 *     .build().query();
 * // Output: SELECT * FROM orders WHERE order_date BETWEEN :minOrderDate AND :maxOrderDate
 * }</pre>
 *
 * @see AbstractQueryBuilder
 * @see Filters
 * @see Condition
 */
public class SqlBuilder extends AbstractQueryBuilder<SqlBuilder> { // NOSONAR

    protected static final Logger logger = LoggerFactory.getLogger(SqlBuilder.class);

    // TODO performance goal: 80% cases (or maybe SQL.length < 1024?) can be composed in 0.1 millisecond. 0.01 millisecond will be fantastic if possible.

    /**
     * Constructs a new SqlBuilder with the specified SqlDialect.
     *
     * @param sqlDialect the dialect (naming policy + parameter style) for this builder
     */
    protected SqlBuilder(final SqlDialect sqlDialect) {
        super(sqlDialect);
    }

    /**
     * Renders the given condition into the SQL being built and appends it to the internal buffer.
     *
     * <p>This is the concrete condition-rendering implementation for the SQL family of builders.
     * It dispatches on the runtime type of {@code cond} and handles {@link Binary}, {@link Between},
     * {@link NotBetween}, {@link In}, {@link InSubQuery}, {@link NotIn}, {@link NotInSubQuery},
     * {@link Where}, {@link Having}, {@link Using}, {@link Cell}, {@link ComposableCell}, {@link Junction},
     * {@link SubQuery} and {@link Expression}. Binary conditions with a {@code null} value and an
     * {@code EQUAL}/{@code IS} (or {@code NOT_EQUAL}/{@code NOT_EQUAL_ANSI}/{@code IS_NOT}) operator
     * are rendered as {@code IS NULL}/{@code IS NOT NULL} respectively. Nested conditions and sub-queries
     * are rendered recursively, with sub-query parameters merged into this builder's parameter list.</p>
     *
     * @param cond the condition to render; must be one of the supported condition types
     * @throws IllegalArgumentException if {@code cond} is an unsupported condition type, or if a
     *         {@link Junction} contains no sub-conditions
     */
    @Override
    protected void appendCondition(final Condition cond) {
        if (cond instanceof final Binary binary) {
            final String propName = binary.getPropName();
            final Object propValue = binary.getPropValue();

            appendColumnName(propName);

            if (propValue == null && (binary.operator() == Operator.EQUAL || binary.operator() == Operator.IS)) {
                _sb.append(_SPACE);
                _sb.append(SK.IS_NULL);
                return;
            } else if (propValue == null
                    && (binary.operator() == Operator.NOT_EQUAL || binary.operator() == Operator.NOT_EQUAL_ANSI || binary.operator() == Operator.IS_NOT)) {
                _sb.append(_SPACE);
                _sb.append(SK.IS_NOT_NULL);
                return;
            }

            _sb.append(_SPACE);
            _sb.append(binary.operator().toString());
            _sb.append(_SPACE);
            setParameter(propName, propValue);
        } else if (cond instanceof final Between bt) {
            appendBetweenClause(bt.getPropName(), bt.operator(), bt.getMinValue(), bt.getMaxValue());
        } else if (cond instanceof final NotBetween nbt) {
            appendBetweenClause(nbt.getPropName(), nbt.operator(), nbt.getMinValue(), nbt.getMaxValue());
        } else if (cond instanceof final AbstractIn anyIn) {
            // Handles both In and NotIn; the IN / NOT IN operator is carried by anyIn.operator().
            appendInClause(anyIn.getPropNames(), anyIn.operator(), anyIn.getValues());
        } else if (cond instanceof final AbstractInSubQuery anyInSubQuery) {
            // Handles both InSubQuery and NotInSubQuery; the IN / NOT IN operator is carried by anyInSubQuery.operator().
            appendInSubQueryClause(anyInSubQuery.getPropNames(), anyInSubQuery.operator(), anyInSubQuery.getSubQuery());
        } else if (cond instanceof Where || cond instanceof Having) {
            final Clause clause = (Clause) cond;

            _sb.append(_SPACE);
            _sb.append(clause.operator().toString());
            _sb.append(_SPACE);

            appendCondition(clause.getCondition());
        } else if (cond instanceof final Using using) {
            // The inner expression of a Using condition already carries the required parentheses,
            // e.g. "(employee_id)"; wrapping it again would produce invalid SQL like "USING ((employee_id))".
            _sb.append(_SPACE);
            _sb.append(using.operator().toString());
            _sb.append(_SPACE);

            appendCondition(using.getCondition());
        } else if (cond instanceof final Cell cell) {
            appendParenthesizedCondition(cell.operator(), cell.getCondition());
        } else if (cond instanceof final ComposableCell cell) {
            appendParenthesizedCondition(cell.operator(), cell.getCondition());
        } else if (cond instanceof final Junction junction) {
            final List<Condition> conditionList = junction.getConditions();

            if (N.isEmpty(conditionList)) {
                throw new IllegalArgumentException("Junction condition (" + junction.operator() + ") must contain at least one element");
            }

            if (conditionList.size() == 1) {
                appendCondition(conditionList.get(0));
            } else {
                // Note: the outer parentheses around the whole junction are intentionally omitted:
                // Cassandra rejects "((id = :id) AND (gui = :gui))" and only accepts "(id = :id) AND (gui = :gui)".
                for (int i = 0, size = conditionList.size(); i < size; i++) {
                    if (i > 0) {
                        _sb.append(_SPACE);
                        _sb.append(junction.operator().toString());
                        _sb.append(_SPACE);
                    }

                    _sb.append(_PARENTHESIS_L);

                    appendCondition(conditionList.get(i));

                    _sb.append(_PARENTHESIS_R);
                }
            }
        } else if (cond instanceof final SubQuery subQuery) {
            final Condition subCond = subQuery.getCondition();

            if (Strings.isNotEmpty(subQuery.sql())) {
                _sb.append(subQuery.sql());
            } else {
                final SqlBuilder subBuilder = newSubQueryBuilder(subQuery);
                seedNamedParameterOccurrences(subBuilder);

                if (subCond != null) {
                    subBuilder.append(subCond);
                }

                final SP subSP = subBuilder.build();
                adoptNamedParameterOccurrences(subBuilder);

                _sb.append(subSP.query());

                if (N.notEmpty(subSP.parameters())) {
                    _parameters.addAll(subSP.parameters());
                }
            }
        } else if (cond instanceof Expression) {
            appendStringExpr(((Expression) cond).getLiteral(), false);
        } else {
            throw new IllegalArgumentException("Unsupported condition type: " + cond);
        }
    }

    private void appendBetweenClause(final String propName, final Operator operator, final Object minValue, final Object maxValue) {
        appendColumnName(propName);

        _sb.append(_SPACE);
        _sb.append(operator.toString());
        _sb.append(_SPACE);

        // Strip any table-alias prefix (e.g. "ord.orderDate" -> "orderDate") so the
        // synthesized "minX"/"maxX" parameter names remain valid identifiers.
        final String cap = Strings.capitalize(sanitizeNamedParameterName(propName));

        setParameter("min" + cap, minValue);

        _sb.append(_SPACE);
        _sb.append(SK.AND);
        _sb.append(_SPACE);

        setParameter("max" + cap, maxValue);
    }

    private void appendInClause(final Collection<String> propNames, final Operator operator, final List<?> values) {
        if (propNames.size() > 1) {
            appendMultiColumnInClause(propNames, operator, values);
            return;
        }

        final String propName = propNames.iterator().next();
        appendColumnName(propName);

        _sb.append(_SPACE);
        _sb.append(operator.toString());
        _sb.append(SK.SPACE_PARENTHESIS_L);

        if (values != null) {
            final boolean indexedParamName = _sqlPolicy == SqlPolicy.NAMED_SQL || _sqlPolicy == SqlPolicy.IBATIS_SQL;

            for (int i = 0, len = values.size(); i < len; i++) {
                if (i > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                if (indexedParamName) {
                    setParameter(propName + (i + 1), values.get(i));
                } else {
                    setParameter(propName, values.get(i));
                }
            }
        }

        _sb.append(SK._PARENTHESIS_R);
    }

    /**
     * Renders a multi-column (row value constructor) IN / NOT IN clause, e.g.
     * {@code (p1, p2) IN ((?, ?), (?, ?))}. Each element of {@code values} is a tuple whose size
     * matches {@code propNames.size()}.
     *
     * @param propNames the property/column names (size {@code > 1})
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param values the value tuples; each element is a {@link Collection} of the row's values
     */
    private void appendMultiColumnInClause(final Collection<String> propNames, final Operator operator, final List<?> values) {
        final String[] colNames = propNames.toArray(new String[0]);

        _sb.append(SK._PARENTHESIS_L);

        for (int c = 0; c < colNames.length; c++) {
            if (c > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(colNames[c]);
        }

        _sb.append(SK._PARENTHESIS_R);
        _sb.append(_SPACE);
        _sb.append(operator.toString());
        _sb.append(SK.SPACE_PARENTHESIS_L);

        if (values != null) {
            final boolean indexedParamName = _sqlPolicy == SqlPolicy.NAMED_SQL || _sqlPolicy == SqlPolicy.IBATIS_SQL;

            for (int i = 0, len = values.size(); i < len; i++) {
                if (i > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                _sb.append(SK._PARENTHESIS_L);

                int c = 0;
                for (final Object value : (Collection<?>) values.get(i)) {
                    if (c > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    if (indexedParamName) {
                        setParameter(colNames[c] + (i + 1), value);
                    } else {
                        setParameter(colNames[c], value);
                    }

                    c++;
                }

                _sb.append(SK._PARENTHESIS_R);
            }
        }

        _sb.append(SK._PARENTHESIS_R);
    }

    private void appendInSubQueryClause(final Collection<String> propNames, final Operator operator, final SubQuery subQuery) {
        if (propNames.size() == 1) {
            appendColumnName(propNames.iterator().next());
        } else {
            _sb.append(SK._PARENTHESIS_L);

            int idx = 0;

            for (final String e : propNames) {
                if (idx++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(e);
            }

            _sb.append(SK._PARENTHESIS_R);
        }

        _sb.append(_SPACE);
        _sb.append(operator.toString());
        _sb.append(SK.SPACE_PARENTHESIS_L);

        appendCondition(subQuery);

        _sb.append(SK._PARENTHESIS_R);
    }

    private void appendParenthesizedCondition(final Operator operator, final Condition inner) {
        _sb.append(_SPACE);
        _sb.append(operator.toString());
        _sb.append(_SPACE);

        _sb.append(_PARENTHESIS_L);
        appendCondition(inner);
        _sb.append(_PARENTHESIS_R);
    }

    /**
     * Allocates a fresh sub-query builder for a {@link SubQuery} condition, carrying over this builder's
     * {@link SqlDialect} so the sub-query is rendered with the same naming and parameter policy as the
     * enclosing statement.
     *
     * @param subQuery the sub-query condition being rendered
     * @return a fresh sub-query builder bound to the same {@link SqlDialect} as {@code this}
     * @throws IllegalArgumentException if {@code subQuery} has no selected property/column names
     */
    private SqlBuilder newSubQueryBuilder(final SubQuery subQuery) {
        final Collection<String> selectPropNames = subQuery.getSelectPropNames();
        N.checkArgNotEmpty(selectPropNames, SELECTION_PART_MSG);

        final SqlBuilder subBuilder = new SqlBuilder(sqlDialect);
        subBuilder._op = OperationType.QUERY;
        subBuilder._propOrColumnNames = selectPropNames;

        if (subQuery.getEntityClass() != null) {
            return subBuilder.from(subQuery.getEntityClass());
        }

        return subBuilder.from(subQuery.getEntityName());
    }

}
