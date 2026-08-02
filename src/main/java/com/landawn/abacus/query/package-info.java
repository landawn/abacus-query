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
 * SQL generation and inspection: fluent query builders, a condition factory, and utilities for
 * parsing, classifying, and externalizing SQL text.
 *
 * <h2>Building SQL</h2>
 * <p>{@link com.landawn.abacus.query.Dsl} is the entry point. Every predefined constant on it is a
 * {@code Dsl} bound to one {@link com.landawn.abacus.query.SqlDialect}; calling a statement method
 * ({@code select}, {@code selectFrom}, {@code insert}, {@code update}, {@code deleteFrom},
 * {@code count}, &hellip;) returns a fresh {@link com.landawn.abacus.query.SqlBuilder} configured for
 * that operation. Finish the builder with {@code build()}, which yields an
 * {@link com.landawn.abacus.query.AbstractQueryBuilder.SP} record pairing the generated
 * {@linkplain com.landawn.abacus.query.AbstractQueryBuilder.SP#query() SQL} with its
 * {@linkplain com.landawn.abacus.query.AbstractQueryBuilder.SP#parameters() parameter values}.
 * {@link com.landawn.abacus.query.AbstractQueryBuilder} holds the shared builder machinery;
 * {@code SqlBuilder} adds condition rendering, operator handling, and NULL semantics.</p>
 *
 * <p>A constant's name encodes both the parameter style and the naming policy applied to property
 * names. The leading letter gives the parameter style &mdash; {@code P} for positional {@code ?}
 * ({@link com.landawn.abacus.query.Dsl#PSB PSB}, {@link com.landawn.abacus.query.Dsl#PSC PSC},
 * {@link com.landawn.abacus.query.Dsl#PAC PAC}, {@link com.landawn.abacus.query.Dsl#PLC PLC}),
 * {@code N} for named {@code :name} ({@link com.landawn.abacus.query.Dsl#NSB NSB},
 * {@link com.landawn.abacus.query.Dsl#NSC NSC}, {@link com.landawn.abacus.query.Dsl#NAC NAC},
 * {@link com.landawn.abacus.query.Dsl#NLC NLC}), and {@code M} for iBATIS/MyBatis
 * <code>#{name}</code> ({@link com.landawn.abacus.query.Dsl#MSB MSB},
 * {@link com.landawn.abacus.query.Dsl#MSC MSC}, {@link com.landawn.abacus.query.Dsl#MAC MAC},
 * {@link com.landawn.abacus.query.Dsl#MLC MLC}; this family is deprecated in favor of the
 * corresponding named-parameter constants). The two-letter suffix gives the naming policy:
 * {@code SB} = {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE},
 * {@code SC} = {@link com.landawn.abacus.util.NamingPolicy#SNAKE_CASE},
 * {@code AC} = {@link com.landawn.abacus.util.NamingPolicy#SCREAMING_SNAKE_CASE},
 * {@code LC} = {@link com.landawn.abacus.util.NamingPolicy#CAMEL_CASE}. The raw-SQL family
 * ({@link com.landawn.abacus.query.Dsl#SCSB SCSB}, {@link com.landawn.abacus.query.Dsl#ACSB ACSB},
 * {@link com.landawn.abacus.query.Dsl#LCSB LCSB}) has no leading style letter: it puts the naming code
 * first and ends in a literal {@code SB}. It inlines literal values rather than binding them, and is
 * deprecated because of the resulting SQL-injection risk.</p>
 *
 * <h2>Dialects</h2>
 * <p>A {@link com.landawn.abacus.query.SqlDialect} is not a full database grammar &mdash; it is the
 * immutable set of rendering choices a builder needs: the {@link com.landawn.abacus.util.NamingPolicy}
 * translating property names to identifiers, the
 * {@link com.landawn.abacus.query.SqlDialect.SqlPolicy} selecting literal, positional, named, or
 * MyBatis parameter rendering, the {@link com.landawn.abacus.query.SqlDialect.IdentifierQuote} used
 * when identifiers must be quoted, an optional {@link com.landawn.abacus.query.SqlDialect.ProductInfo}
 * that drives product-specific syntax such as pagination clauses, an optional named-parameter handler,
 * and an optional tokenizer configuration. Build a custom combination with
 * {@code SqlDialect.builder()} and obtain a DSL for it via
 * {@link com.landawn.abacus.query.Dsl#forDialect(com.landawn.abacus.query.SqlDialect)}.</p>
 *
 * <h2>Conditions and projections</h2>
 * <p>{@link com.landawn.abacus.query.Filters} is the factory for the condition objects defined in
 * {@link com.landawn.abacus.query.condition} &mdash; comparisons, logical junctions, pattern matches,
 * null checks, subqueries, joins, and clauses &mdash; which builders accept in {@code where(...)},
 * {@code having(...)}, {@code join(...)}, and {@code append(...)}.
 * {@link com.landawn.abacus.query.Selection} describes a per-table projection (entity class, table and
 * class aliases, included/excluded properties) for multi-table selects, and
 * {@link com.landawn.abacus.query.SortDirection} supplies {@code ASC}/{@code DESC}.
 * {@link com.landawn.abacus.query.QueryUtil} exposes the underlying entity-to-column mapping helpers.</p>
 *
 * <h2>Assembling SQL from text fragments</h2>
 * <p>{@link com.landawn.abacus.query.DynamicQuery} builds SELECT statements out of raw string
 * fragments through typed clause builders, emitting the clauses in grammar order regardless of the
 * order in which they are requested. It performs no identifier quoting, literal escaping, or
 * parameter binding: fragments are appended verbatim, so keep them application-controlled and
 * represent untrusted values as placeholders. The same caution applies to the raw-fragment APIs
 * elsewhere in this package, such as {@link com.landawn.abacus.query.Filters#expr(String)}.</p>
 *
 * <h2>Parsing and inspecting SQL</h2>
 * <ul>
 *   <li>{@link com.landawn.abacus.query.SqlParser} tokenizes SQL &mdash; handling quoted literals and
 *       identifiers, comments, multi-character operators, and hash-prefixed temp-table names &mdash;
 *       and classifies statements through {@code isSelectQuery}, {@code isInsertQuery},
 *       {@code isUpdateQuery}, {@code isDeleteQuery}, {@code isReadOnlyQuery}, and related methods.</li>
 *   <li>{@link com.landawn.abacus.query.ParsedSql} converts the named parameters ({@code :name} or
 *       <code>#{name}</code>) of a recognized data-operation statement into standard JDBC {@code ?}
 *       placeholders and reports the parameter names in order of appearance.</li>
 *   <li>{@link com.landawn.abacus.query.SqlOperation} enumerates statement kinds (DML, DDL, TCL, and
 *       others) and resolves them case-insensitively from a keyword.</li>
 *   <li>{@link com.landawn.abacus.query.SqlMapper} keeps SQL scripts in XML files, keyed by short
 *       identifiers, so statements can be maintained outside application code.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>{@code Dsl}, {@code SqlDialect}, and {@code Selection} are immutable and safe to share (a
 * {@code Dsl} being thread-safe additionally requires any custom named-parameter handler on its
 * dialect to tolerate concurrent invocation). {@code SqlBuilder} and
 * {@link com.landawn.abacus.query.DynamicQuery.Builder} are <em>not</em> thread-safe and are
 * single-use: create one per query and finish it, which releases the internal pooled resources.
 * A {@code SqlBuilder} is finished by {@code build()} or by one of the terminal helpers
 * {@code apply(...)}, {@code accept(...)}, and {@code debugPrint()}; a {@code DynamicQuery.Builder}
 * is finished by {@code build()}.</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * SP sp = Dsl.PSC.select("firstName", "lastName")
 *         .from("account")
 *         .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("age", 18)))
 *         .orderBy("lastName")
 *         .build();
 *
 * sp.query();
 * // SELECT first_name AS "firstName", last_name AS "lastName" FROM account
 * //   WHERE (status = ?) AND (age > ?) ORDER BY last_name
 * sp.parameters();   // [ACTIVE, 18]
 *
 * // Named parameters instead of positional ones:
 * Dsl.NSC.update("account").set("firstName").where(Filters.eq("id", 1)).build().query();
 * // UPDATE account SET first_name = :firstName WHERE id = :id
 * }</pre>
 *
 * @see com.landawn.abacus.query.Dsl
 * @see com.landawn.abacus.query.SqlBuilder
 * @see com.landawn.abacus.query.Filters
 * @see com.landawn.abacus.query.SqlDialect
 */
package com.landawn.abacus.query;
