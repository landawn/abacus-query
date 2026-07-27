/*
 * Copyright (C) 2026 HaiYang Li
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

/**
 * An object model for the parts of a SQL query: comparisons, logical junctions, clauses, joins, and
 * subqueries, which the query builders in {@link com.landawn.abacus.query} render into SQL.
 *
 * <h2>Creating conditions</h2>
 * <p>{@link com.landawn.abacus.query.Filters} is the recommended factory for every condition type
 * ({@code equal}/{@code eq}, {@code greaterThan}/{@code gt}, {@code between}, {@code in},
 * {@code like}, {@code isNull}, {@code and}, {@code or}, {@code not}, {@code exists},
 * {@code leftJoin}, {@code where}, {@code orderBy}, &hellip;); the constructors are public as well.
 * {@link com.landawn.abacus.query.condition.NamedProperty} offers a fluent alternative that binds one
 * property name once and then produces many conditions from it. Every condition is fed back into a
 * builder through {@code where(...)}, {@code having(...)}, {@code join(...)}, or {@code append(...)}.</p>
 *
 * <h2>The hierarchy</h2>
 * <p>{@link com.landawn.abacus.query.condition.Condition} is the root interface, exposing only
 * {@link com.landawn.abacus.query.condition.Condition#operator() operator()},
 * {@link com.landawn.abacus.query.condition.Condition#parameters() parameters()}, and
 * {@link com.landawn.abacus.query.condition.Condition#toSql(com.landawn.abacus.util.NamingPolicy)
 * toSql(NamingPolicy)}; {@link com.landawn.abacus.query.condition.Operator} enumerates the SQL
 * operators. {@link com.landawn.abacus.query.condition.AbstractCondition} is the common base class,
 * and {@link com.landawn.abacus.query.condition.ComposableCondition} adds the logical operations
 * {@code and(Condition)}, {@code or(Condition)}, {@code not()}, and {@code xor(Condition)}.</p>
 *
 * <ul>
 *   <li><b>Comparisons</b> &mdash; {@link com.landawn.abacus.query.condition.Binary} and its
 *       subclasses {@link com.landawn.abacus.query.condition.Equal},
 *       {@link com.landawn.abacus.query.condition.NotEqual},
 *       {@link com.landawn.abacus.query.condition.GreaterThan},
 *       {@link com.landawn.abacus.query.condition.GreaterThanOrEqual},
 *       {@link com.landawn.abacus.query.condition.LessThan},
 *       {@link com.landawn.abacus.query.condition.LessThanOrEqual},
 *       {@link com.landawn.abacus.query.condition.Like},
 *       {@link com.landawn.abacus.query.condition.NotLike},
 *       {@link com.landawn.abacus.query.condition.Is}, and
 *       {@link com.landawn.abacus.query.condition.IsNot}. The keyword checks
 *       {@link com.landawn.abacus.query.condition.IsNull},
 *       {@link com.landawn.abacus.query.condition.IsNotNull},
 *       {@link com.landawn.abacus.query.condition.IsNaN},
 *       {@link com.landawn.abacus.query.condition.IsNotNaN},
 *       {@link com.landawn.abacus.query.condition.IsInfinite}, and
 *       {@link com.landawn.abacus.query.condition.IsNotInfinite} specialize {@code Is}/{@code IsNot}.</li>
 *   <li><b>Ranges and membership</b> &mdash;
 *       {@link com.landawn.abacus.query.condition.Between} and
 *       {@link com.landawn.abacus.query.condition.NotBetween} (both
 *       {@link com.landawn.abacus.query.condition.AbstractBetween});
 *       {@link com.landawn.abacus.query.condition.In} and
 *       {@link com.landawn.abacus.query.condition.NotIn} for value lists, including the
 *       multi-column row-value form (both {@link com.landawn.abacus.query.condition.AbstractIn}); and
 *       {@link com.landawn.abacus.query.condition.InSubQuery} /
 *       {@link com.landawn.abacus.query.condition.NotInSubQuery} for subquery membership (both
 *       {@link com.landawn.abacus.query.condition.AbstractInSubQuery}).</li>
 *   <li><b>Logical composition</b> &mdash; {@link com.landawn.abacus.query.condition.Junction} with
 *       {@link com.landawn.abacus.query.condition.And} and
 *       {@link com.landawn.abacus.query.condition.Or}, plus the
 *       {@link com.landawn.abacus.query.condition.ComposableCell} wrappers
 *       {@link com.landawn.abacus.query.condition.Not},
 *       {@link com.landawn.abacus.query.condition.Exists}, and
 *       {@link com.landawn.abacus.query.condition.NotExists}.</li>
 *   <li><b>Subqueries</b> &mdash; {@link com.landawn.abacus.query.condition.SubQuery} carries either
 *       raw query text or a structured SELECT built from an entity, property names, and a condition.
 *       {@link com.landawn.abacus.query.condition.All},
 *       {@link com.landawn.abacus.query.condition.Any}, and
 *       {@link com.landawn.abacus.query.condition.Some} are quantified right-hand operands rather than
 *       predicates in their own right.</li>
 *   <li><b>Clauses</b> &mdash; {@link com.landawn.abacus.query.condition.Clause} covers
 *       {@link com.landawn.abacus.query.condition.Where},
 *       {@link com.landawn.abacus.query.condition.GroupBy},
 *       {@link com.landawn.abacus.query.condition.Having},
 *       {@link com.landawn.abacus.query.condition.OrderBy},
 *       {@link com.landawn.abacus.query.condition.Limit}, and the set operations
 *       {@link com.landawn.abacus.query.condition.Union},
 *       {@link com.landawn.abacus.query.condition.UnionAll},
 *       {@link com.landawn.abacus.query.condition.Intersect},
 *       {@link com.landawn.abacus.query.condition.Except}, and
 *       {@link com.landawn.abacus.query.condition.Minus}. {@code Clause} extends
 *       {@link com.landawn.abacus.query.condition.Cell}, as do the join qualifiers
 *       {@link com.landawn.abacus.query.condition.On} and
 *       {@link com.landawn.abacus.query.condition.Using}.</li>
 *   <li><b>Joins</b> &mdash; {@link com.landawn.abacus.query.condition.Join} with
 *       {@link com.landawn.abacus.query.condition.InnerJoin},
 *       {@link com.landawn.abacus.query.condition.LeftJoin},
 *       {@link com.landawn.abacus.query.condition.RightJoin},
 *       {@link com.landawn.abacus.query.condition.FullJoin},
 *       {@link com.landawn.abacus.query.condition.CrossJoin}, and
 *       {@link com.landawn.abacus.query.condition.NaturalJoin}.</li>
 *   <li><b>Raw SQL</b> &mdash; {@link com.landawn.abacus.query.condition.SqlExpression} embeds an
 *       arbitrary SQL fragment and also provides static helpers that assemble common function and
 *       operator text.</li>
 *   <li><b>Whole-query container</b> &mdash; {@link com.landawn.abacus.query.condition.Criteria}
 *       aggregates joins, a WHERE, a GROUP BY, a HAVING, set operations, an ORDER BY, and a LIMIT into
 *       one condition, assembled through {@link com.landawn.abacus.query.condition.Criteria#builder()}.</li>
 * </ul>
 *
 * <h2>What can be combined with what</h2>
 * <p>Only conditions extending {@code ComposableCondition} take part in boolean logic. Clauses and
 * joins are structural query components and deliberately have no {@code and()}/{@code or()}/
 * {@code not()} &mdash; two WHERE clauses cannot be ANDed together; combine the conditions inside a
 * single WHERE instead. Junctions enforce this at construction time and reject clause conditions and
 * {@code Criteria} with {@link java.lang.IllegalArgumentException}. Quantified operands
 * ({@code All}, {@code Any}, {@code Some}) inherit the composition methods for type compatibility but
 * are rejected when composed directly.</p>
 *
 * <h2>Immutability and rendering</h2>
 * <p>Conditions are structurally immutable: nothing can be added or removed after construction, and
 * collection accessors return unmodifiable views. Parameter values and custom {@code Condition}
 * implementations are not deep-copied, so callers must not mutate them while a containing condition is
 * in use. {@code toString()} delegates to {@code toSql(NamingPolicy.NO_CHANGE)} and inlines literal
 * values, which makes it useful for diagnostics but not for execution &mdash; parameter binding happens
 * when a query builder renders the condition, according to the builder's
 * {@link com.landawn.abacus.query.SqlDialect}.</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("age", 18));
 * cond.toString();       // ((status = 'ACTIVE') AND (age > 18))
 * cond.parameters();     // [ACTIVE, 18]
 *
 * Criteria criteria = Criteria.builder()
 *         .join("orders o", new On("a.id", "o.account_id"))
 *         .where(Filters.eq("a.status", "ACTIVE"))
 *         .groupBy("a.id")
 *         .having(Filters.gt("COUNT(*)", 5))
 *         .orderBy("a.id", SortDirection.DESC)
 *         .limit(10)
 *         .build();
 *
 * Dsl.PSC.select("id").from("account a").append(criteria).build().query();
 * // SELECT id FROM account a JOIN orders o ON (a.id = o.account_id) WHERE a.status = ?
 * //   GROUP BY a.id HAVING COUNT(*) > ? ORDER BY a.id DESC LIMIT 10
 * }</pre>
 *
 * @see com.landawn.abacus.query.Filters
 * @see com.landawn.abacus.query.condition.Condition
 * @see com.landawn.abacus.query.condition.Criteria
 * @see com.landawn.abacus.query.SqlBuilder
 */
package com.landawn.abacus.query.condition;
