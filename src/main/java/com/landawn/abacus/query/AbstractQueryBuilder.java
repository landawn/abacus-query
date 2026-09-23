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

import static com.landawn.abacus.util.SK._SPACE;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.landawn.abacus.query.SqlDialect.IdentifierQuote;
import com.landawn.abacus.query.SqlDialect.ProductInfo;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.query.condition.Clause;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Join;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.On;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SqlExpression;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Using;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ConcurrentCacheMap;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Maps;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.OperationType;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.query.QueryUtil.ColumnInfo;
import com.landawn.abacus.util.u.Optional;

/**
 * Base class for fluent SQL builders. Provides clause-by-clause construction of SQL statements
 * (SELECT, INSERT, UPDATE, DELETE) with support for:
 * <ul>
 *   <li>Multiple naming policies (snake_case, SCREAMING_SNAKE_CASE, camelCase)</li>
 *   <li>Parameterized ({@code ?}) and named ({@code :name}, {@code #{name}}) parameter styles</li>
 *   <li>Entity class mapping driven by annotations</li>
 *   <li>Joins, subqueries, set operations ({@code UNION}, {@code INTERSECT}, etc.) and arbitrary conditions</li>
 * </ul>
 *
 * <p>{@link SqlBuilder} supplies the concrete SQL renderer. Choose a predefined {@link Dsl} constant
 * by parameter style and naming policy, or use {@link Dsl#forDialect(SqlDialect)} for custom settings.</p>
 *
 * <p>Instances are <b>not thread-safe</b>; build one per thread or per query and finish it with
 * {@link #build()} or a terminal helper such as {@code apply(...)}, {@code accept(...)}, or
 * {@link #debugPrint()} to release pooled resources. Once a terminal operation begins final rendering,
 * the builder is closed and must not be reused; attempting to build it again throws {@link IllegalStateException}.
 * Preliminary validation can fail without closing the builder; see {@link #build()}.</p>
 *
 * <p>SELECT clauses are order-checked as they are added: {@code from(...)} must precede JOIN,
 * WHERE, GROUP BY, HAVING, ORDER BY, pagination, and FOR UPDATE; each later clause prevents an
 * earlier clause from being appended afterward. Invalid calls fail before changing builder state,
 * so the caller may still complete or build the statement.</p>
 *
 * <p>Raw SQL fragments follow the lexical limitations documented by {@link SqlParser}; in particular,
 * PostgreSQL dollar-quoted strings and nested block comments are not supported.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple SELECT
 * String sql = PSC.select("firstName", "lastName")
 *                 .from("account")
 *                 .where(Filters.equal("id", 1))
 *                 .build().query();
 * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = ?
 *
 * // INSERT with entity
 * String sql2 = PSC.insert(account).into("account").build().query();
 *
 * // UPDATE with conditions
 * String sql3 = PSC.update("account")
 *                  .set(Arrays.asList("name", "status"))
 *                  .where(Filters.equal("id", 1))
 *                  .build().query();
 * }</pre>
 *
 * <p>The builder supports different naming policies through the predefined {@link Dsl} constants:</p>
 * <ul>
 *   <li>{@link Dsl#PSC} - Parameterized SQL with snake_case naming</li>
 *   <li>{@link Dsl#PAC} - Parameterized SQL with SCREAMING_SNAKE_CASE naming</li>
 *   <li>{@link Dsl#PLC} - Parameterized SQL with camelCase naming</li>
 *   <li>{@link Dsl#NSC} - Named SQL with snake_case naming</li>
 *   <li>{@link Dsl#NAC} - Named SQL with SCREAMING_SNAKE_CASE naming</li>
 *   <li>{@link Dsl#NLC} - Named SQL with camelCase naming</li>
 * </ul>
 *
 * @param <This> the concrete subclass type, used as the return type for chained calls (CRTP/self-type)
 *
 * @see com.landawn.abacus.query.SqlBuilder
 * @see com.landawn.abacus.annotation.ReadOnly
 * @see com.landawn.abacus.annotation.ReadOnlyId
 * @see com.landawn.abacus.annotation.NonUpdatable
 * @see com.landawn.abacus.annotation.Transient
 * @see com.landawn.abacus.annotation.Table
 * @see com.landawn.abacus.annotation.Column
 */
public abstract class AbstractQueryBuilder<This extends AbstractQueryBuilder<This>> { // NOSONAR

    /** Shared logger for builder lifecycle and pooled-resource usage diagnostics. */
    protected static final Logger logger = LoggerFactory.getLogger(AbstractQueryBuilder.class);

    /** Constant for the {@code ALL} select modifier (the SQL default; opposite of {@code DISTINCT}). */
    protected static final String ALL = SK.ALL;

    /** Constant for the {@code TOP} select modifier (e.g. {@code TOP 10}). */
    protected static final String TOP = SK.TOP;

    /** Constant for the {@code UNIQUE} select modifier. */
    protected static final String UNIQUE = SK.UNIQUE;

    /** Constant for the {@code DISTINCT} select modifier (opposite of {@code ALL}). */
    protected static final String DISTINCT = SK.DISTINCT;

    /** Constant for the {@code DISTINCTROW} select modifier. */
    protected static final String DISTINCTROW = SK.DISTINCTROW;

    /** Constant for the asterisk (*) wildcard in SQL queries. */
    protected static final String ASTERISK = SK.ASTERISK;

    /** Constant for the COUNT(*) aggregate function. */
    protected static final String COUNT_ALL = SK.COUNT_ALL;

    /** Immutable single-element list holding {@link #COUNT_ALL}, for selecting {@code COUNT(*)}. */
    protected static final List<String> COUNT_ALL_LIST = ImmutableList.of(COUNT_ALL);

    /** Char array for the "INSERT" keyword. */
    protected static final char[] _INSERT = SK.INSERT.toCharArray();

    /** Char array for " INSERT ". */
    protected static final char[] _SPACE_INSERT_SPACE = (SK.SPACE + SK.INSERT + SK.SPACE).toCharArray();

    /** Char array for the "INTO" keyword. */
    protected static final char[] _INTO = SK.INTO.toCharArray();

    /** Char array for " INTO ". */
    protected static final char[] _SPACE_INTO_SPACE = (SK.SPACE + SK.INTO + SK.SPACE).toCharArray();

    /** Char array for the "VALUES" keyword. */
    protected static final char[] _VALUES = SK.VALUES.toCharArray();

    /** Char array for " VALUES ". */
    protected static final char[] _SPACE_VALUES_SPACE = (SK.SPACE + SK.VALUES + SK.SPACE).toCharArray();

    /** Char array for the "SELECT" keyword. */
    protected static final char[] _SELECT = SK.SELECT.toCharArray();

    /** Char array for " SELECT ". */
    protected static final char[] _SPACE_SELECT_SPACE = (SK.SPACE + SK.SELECT + SK.SPACE).toCharArray();

    /** Char array for the "FROM" keyword. */
    protected static final char[] _FROM = SK.FROM.toCharArray();

    /** Char array for " FROM ". */
    protected static final char[] _SPACE_FROM_SPACE = (SK.SPACE + SK.FROM + SK.SPACE).toCharArray();

    /** Char array for the "UPDATE" keyword. */
    protected static final char[] _UPDATE = SK.UPDATE.toCharArray();

    /** Char array for " UPDATE ". */
    protected static final char[] _SPACE_UPDATE_SPACE = (SK.SPACE + SK.UPDATE + SK.SPACE).toCharArray();

    /** Char array for the "SET" keyword. */
    protected static final char[] _SET = SK.SET.toCharArray();

    /** Char array for " SET ". */
    protected static final char[] _SPACE_SET_SPACE = (SK.SPACE + SK.SET + SK.SPACE).toCharArray();

    /** Char array for the "DELETE" keyword. */
    protected static final char[] _DELETE = SK.DELETE.toCharArray();

    /** Char array for " DELETE ". */
    protected static final char[] _SPACE_DELETE_SPACE = (SK.SPACE + SK.DELETE + SK.SPACE).toCharArray();

    /** Char array for the "JOIN" keyword. */
    protected static final char[] _JOIN = SK.JOIN.toCharArray();

    /** Char array for " JOIN ". */
    protected static final char[] _SPACE_JOIN_SPACE = (SK.SPACE + SK.JOIN + SK.SPACE).toCharArray();

    /** Char array for the "LEFT JOIN" keyword. */
    protected static final char[] _LEFT_JOIN = SK.LEFT_JOIN.toCharArray();

    /** Char array for " LEFT JOIN ". */
    protected static final char[] _SPACE_LEFT_JOIN_SPACE = (SK.SPACE + SK.LEFT_JOIN + SK.SPACE).toCharArray();

    /** Char array for the "RIGHT JOIN" keyword. */
    protected static final char[] _RIGHT_JOIN = SK.RIGHT_JOIN.toCharArray();

    /** Char array for " RIGHT JOIN ". */
    protected static final char[] _SPACE_RIGHT_JOIN_SPACE = (SK.SPACE + SK.RIGHT_JOIN + SK.SPACE).toCharArray();

    /** Char array for the "FULL JOIN" keyword. */
    protected static final char[] _FULL_JOIN = SK.FULL_JOIN.toCharArray();

    /** Char array for " FULL JOIN ". */
    protected static final char[] _SPACE_FULL_JOIN_SPACE = (SK.SPACE + SK.FULL_JOIN + SK.SPACE).toCharArray();

    /** Char array for the "CROSS JOIN" keyword. */
    protected static final char[] _CROSS_JOIN = SK.CROSS_JOIN.toCharArray();

    /** Char array for " CROSS JOIN ". */
    protected static final char[] _SPACE_CROSS_JOIN_SPACE = (SK.SPACE + SK.CROSS_JOIN + SK.SPACE).toCharArray();

    /** Char array for the "INNER JOIN" keyword. */
    protected static final char[] _INNER_JOIN = SK.INNER_JOIN.toCharArray();

    /** Char array for " INNER JOIN ". */
    protected static final char[] _SPACE_INNER_JOIN_SPACE = (SK.SPACE + SK.INNER_JOIN + SK.SPACE).toCharArray();

    /** Char array for the "NATURAL JOIN" keyword. */
    protected static final char[] _NATURAL_JOIN = SK.NATURAL_JOIN.toCharArray();

    /** Char array for " NATURAL JOIN ". */
    protected static final char[] _SPACE_NATURAL_JOIN_SPACE = (SK.SPACE + SK.NATURAL_JOIN + SK.SPACE).toCharArray();

    /** Char array for the "ON" keyword. */
    protected static final char[] _ON = SK.ON.toCharArray();

    /** Char array for " ON ". */
    protected static final char[] _SPACE_ON_SPACE = (SK.SPACE + SK.ON + SK.SPACE).toCharArray();

    /** Char array for the "USING" keyword. */
    protected static final char[] _USING = SK.USING.toCharArray();

    /** Char array for " USING ". */
    protected static final char[] _SPACE_USING_SPACE = (SK.SPACE + SK.USING + SK.SPACE).toCharArray();

    /** Char array for the "WHERE" keyword. */
    protected static final char[] _WHERE = SK.WHERE.toCharArray();

    /** Char array for " WHERE ". */
    protected static final char[] _SPACE_WHERE_SPACE = (SK.SPACE + SK.WHERE + SK.SPACE).toCharArray();

    /** Char array for the "GROUP BY" keyword. */
    protected static final char[] _GROUP_BY = SK.GROUP_BY.toCharArray();

    /** Char array for " GROUP BY ". */
    protected static final char[] _SPACE_GROUP_BY_SPACE = (SK.SPACE + SK.GROUP_BY + SK.SPACE).toCharArray();

    /** Char array for the "HAVING" keyword. */
    protected static final char[] _HAVING = SK.HAVING.toCharArray();

    /** Char array for " HAVING ". */
    protected static final char[] _SPACE_HAVING_SPACE = (SK.SPACE + SK.HAVING + SK.SPACE).toCharArray();

    /** Char array for the "ORDER BY" keyword. */
    protected static final char[] _ORDER_BY = SK.ORDER_BY.toCharArray();

    /** Char array for " ORDER BY ". */
    protected static final char[] _SPACE_ORDER_BY_SPACE = (SK.SPACE + SK.ORDER_BY + SK.SPACE).toCharArray();

    /** Char array for the "LIMIT" keyword. */
    protected static final char[] _LIMIT = SK.LIMIT.toCharArray();

    /** Char array for " LIMIT ". */
    protected static final char[] _SPACE_LIMIT_SPACE = (SK.SPACE + SK.LIMIT + SK.SPACE).toCharArray();

    /** Char array for the "OFFSET" keyword. */
    protected static final char[] _OFFSET = SK.OFFSET.toCharArray();

    /** Char array for " OFFSET ". */
    protected static final char[] _SPACE_OFFSET_SPACE = (SK.SPACE + SK.OFFSET + SK.SPACE).toCharArray();

    /** Char array for " ROWS". */
    protected static final char[] _SPACE_ROWS = (SK.SPACE + SK.ROWS).toCharArray();

    /**
     * Matches the generic {@code LIMIT count [OFFSET offset]} expressions that reach the builder as an
     * unresolved {@link Limit#expression()}, where each token is an integer literal. In practice int-range forms are
     * parsed into concrete count/offset by {@link Limit#Limit(String)} and rendered via {@link #limit(int)} /
     * {@link #limit(int, int)}, so this pattern normally handles integer literals outside the {@code int} range.
     */
    private static final String LIMIT_SLOT_PATTERN = "(\\d+)";
    /** Matches a generic {@code LIMIT count [OFFSET offset]} or {@code LIMIT offset, count} expression. */
    private static final Pattern GENERIC_LIMIT_EXPRESSION_PATTERN = Pattern.compile(
            "LIMIT\\s+" + LIMIT_SLOT_PATTERN + "(?:\\s+OFFSET\\s+" + LIMIT_SLOT_PATTERN + "|\\s*,\\s*" + LIMIT_SLOT_PATTERN + ")?", Pattern.CASE_INSENSITIVE);
    /** Matches the SQL:2008 {@code [OFFSET offset ROWS] FETCH FIRST|NEXT count ROWS ONLY} expression. */
    private static final Pattern FETCH_LIMIT_EXPRESSION_PATTERN = Pattern.compile(
            "(?:OFFSET\\s+" + LIMIT_SLOT_PATTERN + "\\s+ROWS?\\s+)?FETCH\\s+(?:FIRST|NEXT)\\s+" + LIMIT_SLOT_PATTERN + "\\s+ROWS?\\s+ONLY",
            Pattern.CASE_INSENSITIVE);

    /** Internal clause-state marker distinguishing {@code OFFSET n ROWS} from limit-style {@code OFFSET n}. */
    private static final String OFFSET_ROWS_SLOT = "OFFSET ROWS syntax";

    /** Char array for the "AND" keyword. */
    protected static final char[] _AND = SK.AND.toCharArray();

    /** Char array for " AND ". */
    protected static final char[] _SPACE_AND_SPACE = (SK.SPACE + SK.AND + SK.SPACE).toCharArray();

    /** Char array for the "OR" keyword. */
    protected static final char[] _OR = SK.OR.toCharArray();

    /** Char array for " OR ". */
    protected static final char[] _SPACE_OR_SPACE = (SK.SPACE + SK.OR + SK.SPACE).toCharArray();

    /** Char array for the "UNION" keyword. */
    protected static final char[] _UNION = SK.UNION.toCharArray();

    /** Char array for " UNION ". */
    protected static final char[] _SPACE_UNION_SPACE = (SK.SPACE + SK.UNION + SK.SPACE).toCharArray();

    /** Char array for the "UNION ALL" keyword. */
    protected static final char[] _UNION_ALL = SK.UNION_ALL.toCharArray();

    /** Char array for " UNION ALL ". */
    protected static final char[] _SPACE_UNION_ALL_SPACE = (SK.SPACE + SK.UNION_ALL + SK.SPACE).toCharArray();

    /** Char array for the "INTERSECT" keyword. */
    protected static final char[] _INTERSECT = SK.INTERSECT.toCharArray();

    /** Char array for " INTERSECT ". */
    protected static final char[] _SPACE_INTERSECT_SPACE = (SK.SPACE + SK.INTERSECT + SK.SPACE).toCharArray();

    /** Char array for the "EXCEPT" keyword. */
    protected static final char[] _EXCEPT = SK.EXCEPT.toCharArray();

    /** Char array for " EXCEPT ". */
    protected static final char[] _SPACE_EXCEPT_SPACE = (SK.SPACE + SK.EXCEPT + SK.SPACE).toCharArray();

    /** Char array for the "MINUS" keyword (Oracle's EXCEPT equivalent). */
    protected static final char[] _EXCEPT_MINUS = SK.EXCEPT_MINUS.toCharArray();

    /** Char array for " MINUS ". */
    protected static final char[] _SPACE_EXCEPT_MINUS_SPACE = (SK.SPACE + SK.EXCEPT_MINUS + SK.SPACE).toCharArray();

    /** Char array for the "AS" keyword. */
    protected static final char[] _AS = SK.AS.toCharArray();

    /** Char array for " AS ". */
    protected static final char[] _SPACE_AS_SPACE = (SK.SPACE + SK.AS + SK.SPACE).toCharArray();

    /** Char array for " = ". */
    protected static final char[] _SPACE_EQUAL_SPACE = (SK.SPACE + SK.EQUAL + SK.SPACE).toCharArray();

    /** Char array for " FOR UPDATE". */
    protected static final char[] _SPACE_FOR_UPDATE = (SK.SPACE + SK.FOR_UPDATE).toCharArray();

    /** Char array for ", ". */
    protected static final char[] _COMMA_SPACE = SK.COMMA_SPACE.toCharArray();

    /** String for " AS ". */
    protected static final String SPACE_AS_SPACE = SK.SPACE + SK.AS + SK.SPACE;

    /** Error message for an invalid selection-part argument. */
    protected static final String SELECTION_PART_MSG = "The specified parameter is not valid for selection part. It must not be null or empty";

    /** Error message for an invalid insertion-part argument. */
    protected static final String INSERTION_PART_MSG = "The specified parameter is not valid for insertion part. It must not be null or empty";

    /** Error message for an invalid update-part argument. */
    protected static final String UPDATE_PART_MSG = "The specified parameter is not valid for update part. It must not be null or empty";

    /** Error message for an invalid deletion-part argument. */
    protected static final String DELETION_PART_MSG = "The specified parameter is not valid for deletion part. It must not be null or empty";

    /**
     * Registry of SQL keywords and niladic (no-parentheses) keyword functions that naming-policy
     * column-name conversion must leave untouched. Populated from the public {@link SK} string
     * constants in original and upper case (lower-case forms are deliberately not registered, so a
     * column genuinely named like a keyword, e.g. {@code order} or {@code count}, is still converted),
     * plus UPPER-case niladic functions such as {@code CURRENT_TIMESTAMP}; kept in sync with
     * {@code SqlExpression}'s own registry, which follows the same rule.
     */
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
                    }
                } catch (final Exception e) {
                    // ignore, should never happen.
                }
            }
        }

        // Niladic (no-parentheses) keyword functions that SK does not contribute. Those containing an
        // underscore are indistinguishable from a snake_case column name, so without registration a naming
        // policy rewrites them into an identifier (CURRENT_USER -> currentUser under CAMEL_CASE, and the
        // un-parseable current-user under KEBAB_CASE). Registered UPPER-case only, so a real column
        // genuinely named e.g. "current_user" is still converted; this mirrors SqlExpression's own registry,
        // which these must stay in sync with -- SqlExpression.toSql and this builder are two independent
        // rendering paths for the same expression text.
        for (final String keyword : new String[] { "CURRENT_CATALOG", "CURRENT_DATE", "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_SCHEMA", "CURRENT_TIME",
                "CURRENT_TIMESTAMP", "CURRENT_USER", "LOCALTIME", "LOCALTIMESTAMP", "SESSION_USER", "SYSTEM_USER", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP",
                "NAN", "INFINITE", "UNKNOWN", "TRUE", "FALSE" }) {
            sqlKeyWords.add(keyword);
        }
    }

    /** Cache of the sub-entity property names per entity class; see {@link #getSubEntityPropNames(Class)}. */
    protected static final Map<Class<?>, ImmutableSet<String>> subEntityPropNamesPool = new ConcurrentCacheMap<>(QueryUtil.POOL_SIZE);

    /** Cache of the categorized property-name sets per entity class; see {@link #loadPropNamesByClass(Class)}. */
    protected static final Map<Class<?>, Set<String>[]> defaultPropNamesPool = new ConcurrentCacheMap<>(QueryUtil.POOL_SIZE);

    /** Cache of the fully rendered select part per entity class, per naming policy. */
    protected static final Map<NamingPolicy, Map<Class<?>, String>> fullSelectPartsPool = N.newHashMap(NamingPolicy.values().length);

    /**
     * Same cache as {@link #fullSelectPartsPool} for backtick-quoting dialects. The cached select parts
     * embed the dialect's identifier quote, so backtick dialects use a separate pool to avoid
     * cross-dialect cache poisoning between dialects sharing the same naming policy.
     */
    protected static final Map<NamingPolicy, Map<Class<?>, String>> fullSelectPartsPoolForBacktick = N.newHashMap(NamingPolicy.values().length);

    static {
        for (final NamingPolicy np : NamingPolicy.values()) {
            fullSelectPartsPool.put(np, new ConcurrentHashMap<>());
            fullSelectPartsPoolForBacktick.put(np, new ConcurrentHashMap<>());
        }
    }

    /** Cache of the per-naming-policy table names per entity class; see {@link #getTableName(Class, NamingPolicy)}. */
    protected static final Map<Class<?>, String[]> classTableNameMap = new ConcurrentHashMap<>();

    /** Cache of the {@link Table}-defined alias per entity class; see {@link #tableAlias(Class)}. */
    protected static final Map<Class<?>, String> classTableAliasMap = new ConcurrentHashMap<>();

    /** Count of live builders, used to warn when pooled resources are not released via {@code build()}. */
    protected static final AtomicInteger activeStringBuilderCounter = new AtomicInteger();

    /** The SQL dialect this builder renders SQL with. */
    protected final SqlDialect sqlDialect;

    /** The naming policy applied when converting property/entity names to column/table names. */
    protected final NamingPolicy _namingPolicy; //NOSONAR

    /** The SQL policy governing how parameter placeholders are rendered. */
    protected final SqlPolicy _sqlPolicy; //NOSONAR

    /** The quote character wrapped around generated aliases ({@code `} for MySQL-family dialects, {@code "} otherwise). */
    protected final char _identifierQuote; //NOSONAR

    /** The dialect family resolved from the dialect's product info; selects the pagination syntax. */
    final DialectFamily _dialectFamily; //NOSONAR

    /** The parameter values collected for the built statement, in placeholder order. */
    protected final List<Object> _parameters = new ArrayList<>(); //NOSONAR

    /** Occurrence count per named-parameter base name, used to generate unique {@code <base>_<n>} names. */
    protected final Map<String, Integer> _namedParameterNameOccurrences = new HashMap<>(); //NOSONAR

    /**
     * Tracks placeholders emitted by builder APIs (including QME placeholders that have no entry in
     * {@link #_parameters}). This lets sibling set operations validate policy compatibility without
     * consuming the child builder first or mistaking SQL operators such as PostgreSQL's JSON '?'
     * for parameters.
     */
    protected boolean _hasGeneratedParameterPlaceholder = false; //NOSONAR

    /**
     * Every named-parameter name emitted into the SQL so far. Needed in addition to the occurrence
     * counts because a generated {@code "<base>_<n>"} may collide with a property literally named
     * {@code "<base>_<n>"}.
     */
    protected final Set<String> _generatedNamedParameterNames = new HashSet<>(); //NOSONAR

    /**
     * The exact {@code NAMED_SQL} token emitted for each generated name. Custom formatters are allowed
     * to emit forms other than {@code ":name"}, so set-operation collision handling must not assume
     * the default form.
     */
    protected final Map<String, String> _renderedNamedParameterTokens = new HashMap<>(); //NOSONAR

    /** The buffer accumulating the SQL text. */
    protected StringBuilder _sb; //NOSONAR

    /** The entity class associated with this query for property-to-column mapping, if any. */
    protected Class<?> _entityClass; //NOSONAR

    /** Bean metadata for {@link #_entityClass}. */
    protected BeanInfo _entityInfo; //NOSONAR

    /** Property-name to column-info mapping for {@link #_entityClass}. */
    protected ImmutableMap<String, ColumnInfo> _propColumnNameMap; //NOSONAR

    /** The operation type (e.g. {@code ADD}, {@code QUERY}, {@code UPDATE}, {@code DELETE}) of the statement being built. */
    protected OperationType _op; //NOSONAR

    /** The primary table name parsed from the current clause. */
    protected String _tableName; //NOSONAR

    /** The primary table alias parsed from the current clause, if any. */
    protected String _tableAlias; //NOSONAR

    /** The staged select modifier (e.g. {@code DISTINCT}, {@code TOP 10}) for the current SELECT segment. */
    protected String _selectModifier; //NOSONAR

    /**
     * Buffer position right after the current segment's emitted SELECT keyword, or -1 if the
     * current segment's SELECT has not been emitted yet (reset by set operations like union()).
     */
    protected int _selectKeywordEndIdx = -1; //NOSONAR

    /** The staged select/insert column or property names. */
    protected Collection<String> _propOrColumnNames; //NOSONAR

    /** The staged select expressions and their aliases. */
    protected Map<String, String> _propOrColumnNameAliases; //NOSONAR

    /** The staged multi-entity selections. */
    protected List<Selection> _multiSelects; //NOSONAR

    /** Property-to-column mappings registered per table alias (FROM and JOIN aliases). */
    protected Map<String, Map<String, ColumnInfo>> _aliasPropColumnNameMap; //NOSONAR

    /** The staged property values for an INSERT or UPDATE. */
    protected Map<String, Object> _props; //NOSONAR

    /** The staged batch of property-value maps for a multi-row INSERT. */
    protected Collection<Map<String, Object>> _propsList; //NOSONAR

    /** Whether the FROM clause of the current query segment has been emitted. */
    protected boolean _hasFromBeenSet = false; //NOSONAR

    /** Whether this builder only renders a condition rather than a full statement. */
    protected boolean _isForConditionOnly = false; //NOSONAR

    /** True only after a JOIN form that may accept ON/USING, until that connector is emitted. */
    protected boolean _joinConditionAllowed = false; //NOSONAR

    /**
     * True after a set operation has appended a complete right-hand query. At that point only
     * compound-result clauses (another set operation, ORDER BY, pagination, FOR UPDATE) may follow.
     */
    protected boolean _hasCompletedSetOperation = false; //NOSONAR

    /** Whether this statement contains a top-level set operation, including a currently open SELECT segment. */
    protected boolean _hasSetOperation = false; //NOSONAR

    /**
     * Whether a set(...) call has already written assignments, so chained set(...) calls know a
     * leading comma is required (sniffing the buffer's last char breaks on trailing whitespace).
     */
    protected boolean _setListStarted = false; //NOSONAR

    /** Renders a named-parameter token into the SQL buffer under the {@code NAMED_SQL} policy. */
    protected final BiConsumer<StringBuilder, String> _handlerForNamedParameter; //NOSONAR

    /** The SQL tokenizer configured by the dialect, used to validate raw SQL fragments and sub-queries. */
    protected final SqlParser.Tokenizer _tokenizer; //NOSONAR

    /** Keywords of the clauses already appended, tracked for clause-order validation. */
    protected final Set<String> calledOpSet = new HashSet<>(); //NOSONAR

    /**
     * Constructs a new AbstractQueryBuilder with the specified SQL dialect.
     *
     * @param sqlDialect the SQL dialect supplying the naming and SQL policies; a {@code null} dialect is treated as an
     *                   all-defaults dialect. A {@code null} naming policy on the dialect defaults to {@code SNAKE_CASE},
     *                   and a {@code null} SQL policy defaults to {@code RAW_SQL}.
     *                   A {@code null} identifier quote defaults to backtick when the dialect's product info names
     *                   MySQL/MariaDB and to double quote otherwise, and the product info selects the dialect-specific
     *                   pagination syntax used by {@link #limit(int)}, {@link #limit(int, int)} and {@link #offset(int)}.
     *                   The dialect also scopes named-parameter rendering and tokenizer configuration to this builder.
     */
    protected AbstractQueryBuilder(final SqlDialect sqlDialect) {
        final int activeBuilderCount = activeStringBuilderCounter.incrementAndGet();

        if (activeBuilderCount > 1024) {
            logger.error("Too many active query builders ({}). Call build() on each builder to release its resources", activeBuilderCount);
        } else if (activeBuilderCount > 512 && logger.isWarnEnabled()) {
            logger.warn("{} active query builders. Call build() on each builder to release its resources", activeBuilderCount);
        }

        this.sqlDialect = sqlDialect == null ? SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(SqlPolicy.RAW_SQL).build() : sqlDialect;

        _sb = Objectory.createStringBuilder();

        _namingPolicy = this.sqlDialect.namingPolicy() == null ? NamingPolicy.SNAKE_CASE : this.sqlDialect.namingPolicy();
        _sqlPolicy = this.sqlDialect.sqlPolicy() == null ? SqlPolicy.RAW_SQL : this.sqlDialect.sqlPolicy();
        _dialectFamily = resolveDialectFamily(this.sqlDialect.productInfo());
        _identifierQuote = this.sqlDialect.identifierQuote() == null //
                ? (_dialectFamily == DialectFamily.MYSQL ? SK._BACKTICK : SK._DOUBLE_QUOTE)
                : (this.sqlDialect.identifierQuote() == IdentifierQuote.BACKTICK ? SK._BACKTICK : SK._DOUBLE_QUOTE);

        _handlerForNamedParameter = this.sqlDialect.namedParameterHandler() == null ? SqlDialect.DEFAULT_NAMED_PARAMETER_HANDLER
                : this.sqlDialect.namedParameterHandler();
        _tokenizer = this.sqlDialect.tokenizerConfig() == null ? SqlParser.tokenizer() : SqlParser.tokenizer(this.sqlDialect.tokenizerConfig());

        if (logger.isDebugEnabled()) {
            logger.debug("SqlBuilder created. Active builders: {}", activeBuilderCount);
        }
    }

    /**
     * Resolves the dialect family from the optional product info. The product name is matched
     * case-insensitively as a substring, so raw JDBC names from
     * {@code DatabaseMetaData.getDatabaseProductName()} such as {@code "Microsoft SQL Server"} or
     * {@code "Oracle Database 19c"} are recognized. A {@code null} product info, blank name, or
     * unrecognized name resolves to {@link DialectFamily#DEFAULT}.
     *
     * @param productInfo the dialect's product info, may be {@code null}
     * @return the resolved dialect family, never {@code null}
     */
    static DialectFamily resolveDialectFamily(final ProductInfo productInfo) {
        if (productInfo == null || Strings.isBlank(productInfo.name())) {
            return DialectFamily.DEFAULT;
        }

        if (productInfo.isOracle()) {
            return DialectFamily.ORACLE;
        }

        if (productInfo.isDB2()) {
            return DialectFamily.DB2;
        }

        if (productInfo.isSQLServer()) {
            return DialectFamily.SQL_SERVER;
        }

        if (productInfo.isMySQL() || productInfo.isMariaDB()) {
            return DialectFamily.MYSQL;
        }

        if (productInfo.isPostgreSQL() || productInfo.isSQLite() || productInfo.isH2()) {
            return DialectFamily.LIMIT_STYLE;
        }

        return DialectFamily.DEFAULT;
    }

    /**
     * Whether this builder's dialect paginates with SQL:2008 {@code OFFSET ... ROWS} /
     * {@code FETCH ... ROWS ONLY} instead of {@code LIMIT}/{@code OFFSET}.
     */
    private boolean usesFetchPagination() {
        return _dialectFamily == DialectFamily.ORACLE || _dialectFamily == DialectFamily.DB2 || _dialectFamily == DialectFamily.SQL_SERVER;
    }

    /**
     * Checks whether this builder generates named SQL with {@code :name}-style parameters, i.e. uses the
     * {@link SqlPolicy#NAMED_SQL} policy. Returns {@code false} for the iBATIS {@code #{name}} policy
     * ({@link SqlPolicy#IBATIS_SQL}).
     *
     * @return {@code true} if this builder uses the {@link SqlPolicy#NAMED_SQL} policy, {@code false} otherwise
     */
    protected boolean isNamedSql() {
        return _sqlPolicy == SqlPolicy.NAMED_SQL;
    }

    /**
     * Gets the table name for the specified entity class based on the naming policy.
     * If the entity class has a {@link Table} annotation with a name attribute, that name is used.
     * Otherwise the class name is converted for {@code SNAKE_CASE}, {@code SCREAMING_SNAKE_CASE} and
     * {@code CAMEL_CASE}; any other policy (including {@code NO_CHANGE}) returns the simple class name unchanged.
     *
     * @param entityClass the entity class
     * @param namingPolicy the naming policy to apply
     * @return the table name
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    protected static String getTableName(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        N.checkArgNotNull(entityClass, cs.entityClass);

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
     * The alias is retrieved from the {@link Table} annotation's alias attribute.
     *
     * @param entityClass the entity class (may be {@code null})
     * @return the table alias, or an empty string if not defined or if {@code entityClass} is {@code null}
     */
    protected static String tableAlias(final Class<?> entityClass) {
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
    protected static String tableAlias(final String alias, final Class<?> entityClass) {
        if (Strings.isNotEmpty(alias)) {
            return alias;
        }

        return tableAlias(entityClass);
    }

    /**
     * Gets the table alias or table name for the specified entity class.
     *
     * @param entityClass the entity class
     * @param namingPolicy the naming policy to apply
     * @return the table alias if defined, otherwise the table name
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, or if it defines no {@link Table} alias and is
     *         not a valid entity bean class
     */
    protected static String tableAliasOrName(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        return tableAliasOrName(null, entityClass, namingPolicy);
    }

    /**
     * Gets the table alias or table name for the specified entity class.
     * Priority: specified alias > class-defined alias > table name
     *
     * @param alias the specified alias
     * @param entityClass the entity class
     * @param namingPolicy the naming policy to apply
     * @return the table alias if specified or defined, otherwise the table name
     * @throws IllegalArgumentException if {@code alias} is {@code null} or empty and either {@code entityClass} is {@code null},
     *         or {@code entityClass} defines no {@link Table} alias and is not a valid entity bean class
     */
    protected static String tableAliasOrName(final String alias, final Class<?> entityClass, final NamingPolicy namingPolicy) {
        String tableAliasOrName = alias;

        if (Strings.isEmpty(tableAliasOrName)) {
            tableAliasOrName = tableAlias(entityClass);
        }

        if (Strings.isEmpty(tableAliasOrName)) {
            tableAliasOrName = getTableName(entityClass, namingPolicy);
        }

        return tableAliasOrName;
    }

    /**
     * Checks if the given property value is a default (unset) ID property value.
     * A value is considered default if it is {@code null}, or if it is a numeric type
     * whose mathematical value equals zero (including {@link java.math.BigDecimal},
     * {@link java.math.BigInteger}, {@link Double}, {@link Float}, and all integral types
     * via {@link Number#longValue()}).
     *
     * @param propValue the property value to check
     * @return {@code true} if the value is {@code null} or numerically zero, {@code false} otherwise
     */
    @Internal
    protected static boolean isDefaultIdPropValue(final Object propValue) {
        if (propValue == null) {
            return true;
        }
        if (propValue instanceof java.math.BigDecimal bd) {
            return bd.signum() == 0;
        }
        if (propValue instanceof java.math.BigInteger bi) {
            return bi.signum() == 0;
        }
        if (propValue instanceof Double d) {
            return d == 0.0;
        }
        if (propValue instanceof Float f) {
            return f == 0.0f;
        }
        if (propValue instanceof Number n) {
            return n.longValue() == 0;
        }
        return false;
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
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    protected static Set<String>[] loadPropNamesByClass(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, cs.entityClass);

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
                final Set<String> columnFields = tableAnno == null ? N.emptySet() : N.toSet(tableAnno.columnFields());
                final Set<String> nonColumnFields = tableAnno == null ? N.emptySet() : N.toSet(tableAnno.nonColumnFields());
                final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
                Class<?> subEntityClass = null;
                Set<String> subEntityPropNameList = null;

                for (final String subEntityPropName : subEntityPropNames) {
                    final PropInfo propInfo = entityInfo.getPropInfo(subEntityPropName);

                    if (propInfo == null) {
                        continue;
                    }

                    subEntityClass = (propInfo.type.isCollection() ? propInfo.type.elementType() : propInfo.type).javaType();

                    subEntityPropNameList = N.newLinkedHashSet();

                    final Table subTableAnno = subEntityClass.getAnnotation(Table.class);
                    final Set<String> subColumnFields = subTableAnno == null ? N.emptySet() : N.toSet(subTableAnno.columnFields());
                    final Set<String> subNonColumnFields = subTableAnno == null ? N.emptySet() : N.toSet(subTableAnno.nonColumnFields());

                    for (final PropInfo subPropInfo : ParserUtil.getBeanInfo(subEntityClass).propInfoList) {
                        if (!subPropInfo.isSubEntity && !QueryUtil.isNonColumn(subColumnFields, subNonColumnFields, subPropInfo)) {
                            subEntityPropNameList.add(subPropInfo.name);
                        }
                    }

                    for (final String pn : subEntityPropNameList) {
                        val[0].add(Strings.concat(subEntityPropName, SK.PERIOD, pn));
                    }
                }

                final Set<String> nonUpdatableNonWritablePropNames = N.newHashSet();
                final Set<String> nonUpdatablePropNames = N.newHashSet();
                final Set<String> transientPropNames = N.newHashSet();

                for (final PropInfo propInfo : entityInfo.propInfoList) {
                    if (propInfo.isAnnotationPresent(ReadOnly.class) || propInfo.isAnnotationPresent(ReadOnlyId.class) || propInfo.isMarkedAsReadOnlyId) {
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

                for (final String idPropName : QueryUtil.idPropNames(entityClass)) {
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
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    protected static ImmutableSet<String> getSubEntityPropNames(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, cs.entityClass);

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

    /**
     * Gets the table names for a SELECT query involving the specified entity class and its sub-entities.
     * Returns a list of table names (with aliases if defined) for the main entity and its sub-entity properties.
     *
     * @param entityClass the entity class
     * @param alias the table alias for the main entity (can be null or empty)
     * @param excludedPropNames sub-entity property names to exclude (can be null)
     * @param namingPolicy the naming policy for table name conversion
     * @return a list of table name expressions, or an empty list if there are no sub-entity properties
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    protected static List<String> buildFromTableRefs(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames,
            final NamingPolicy namingPolicy) {
        final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

        if (N.isEmpty(subEntityPropNames)) {
            return N.emptyList();
        }

        final List<String> res = new ArrayList<>(subEntityPropNames.size() + 1);

        String tableAlias = tableAlias(alias, entityClass);

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

            if (propInfo == null) {
                continue;
            }

            subEntityClass = (propInfo.type.isCollection() ? propInfo.type.elementType() : propInfo.type).javaType();
            tableAlias = tableAlias(subEntityClass);

            if (Strings.isEmpty(tableAlias)) {
                res.add(getTableName(subEntityClass, namingPolicy));
            } else {
                res.add(getTableName(subEntityClass, namingPolicy) + " " + tableAlias);
            }
        }

        return res;
    }

    /**
     * Creates a map with property names as keys and {@link Filters#QME} (question-mark expression) as values.
     * <p>This is useful for building INSERT/UPDATE column-to-placeholder maps that the builder will
     * later render as {@code ?} (or as {@code :name} / {@code #{name}} when named/iBATIS SQL is used).</p>
     *
     * <p>This is an internal helper available to subclasses; it is not part of the public API.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Within a subclass of AbstractQueryBuilder:
     * Map<String, SqlExpression> params = namedPlaceholders("firstName", "lastName");
     * }</pre>
     *
     * @param propNames the property names (must not be {@code null})
     * @return a map with property names mapped to {@code Filters.QME}
     * @throws IllegalArgumentException if {@code propNames} is {@code null}
     */
    @Beta
    protected static Map<String, SqlExpression> namedPlaceholders(final String... propNames) {
        N.checkArgNotNull(propNames, cs.propNames);

        final Map<String, SqlExpression> m = N.newLinkedHashMap(propNames.length);

        for (final String propName : propNames) {
            m.put(propName, Filters.QME);
        }

        return m;
    }

    /**
     * Creates a map with property names as keys and {@link Filters#QME} (question-mark expression) as values.
     * <p>This is useful for building INSERT/UPDATE column-to-placeholder maps that the builder will
     * later render as {@code ?} (or as {@code :name} / {@code #{name}} when named/iBATIS SQL is used).</p>
     *
     * <p>This is an internal helper available to subclasses; it is not part of the public API.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Within a subclass of AbstractQueryBuilder:
     * Map<String, SqlExpression> params = namedPlaceholders(Arrays.asList("firstName", "lastName"));
     * }</pre>
     *
     * @param propNames the collection of property names (must not be {@code null})
     * @return a map with property names mapped to {@code Filters.QME}
     * @throws IllegalArgumentException if {@code propNames} is {@code null}
     */
    @Beta
    protected static Map<String, SqlExpression> namedPlaceholders(final Collection<String> propNames) {
        N.checkArgNotNull(propNames, cs.propNames);

        final Map<String, SqlExpression> m = N.newLinkedHashMap(propNames.size());

        for (final String propName : propNames) {
            m.put(propName, Filters.QME);
        }

        return m;
    }

    /**
     * Validates that a raw SQL fragment argument is not {@code null}, empty, or blank.
     *
     * @param value the SQL fragment to validate
     * @param argName the argument name used in the exception message
     * @throws IllegalArgumentException if {@code value} is {@code null}, empty, or blank
     */
    static void checkSqlFragmentNotBlank(final String value, final String argName) {
        if (Strings.isBlank(value)) {
            throw new IllegalArgumentException(argName + " must not be null, empty, or blank");
        }
    }

    /**
     * Validates that an array of raw SQL fragment arguments is not {@code null} or empty and contains
     * no {@code null}, empty, or blank element.
     *
     * @param values the SQL fragments to validate
     * @param argName the argument name used in the exception message
     * @throws IllegalArgumentException if {@code values} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    static void checkSqlFragmentsNotBlank(final String[] values, final String argName) {
        N.checkArgNotEmpty(values, argName);

        for (int i = 0; i < values.length; i++) {
            // The indexed argument label is only needed when validation fails.
            if (Strings.isBlank(values[i])) {
                checkSqlFragmentNotBlank(values[i], argName + "[" + i + "]");
            }
        }
    }

    /**
     * Validates that a collection of raw SQL fragment arguments is not {@code null} or empty and contains
     * no {@code null}, empty, or blank element.
     *
     * @param values the SQL fragments to validate
     * @param argName the argument name used in the exception message
     * @throws IllegalArgumentException if {@code values} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    static void checkSqlFragmentsNotBlank(final Collection<String> values, final String argName) {
        N.checkArgNotEmpty(values, argName);

        int i = 0;

        for (final String value : values) {
            if (Strings.isBlank(value)) {
                checkSqlFragmentNotBlank(value, argName + "[" + i + "]");
            }
            i++;
        }
    }

    /**
     * Takes one stable snapshot of a caller-owned collection and validates the same elements that
     * the builder will subsequently render. This also rejects collections whose reported size is
     * non-zero but whose iterator is empty.
     */
    private static List<String> copyAndValidateSqlFragments(final Collection<String> values, final String argName) {
        N.checkArgNotNull(values, argName);

        final List<String> copy = new ArrayList<>(values);
        checkSqlFragmentsNotBlank(copy, argName);

        return copy;
    }

    /**
     * Validates that a map carrying raw SQL fragments is not {@code null} or empty and that every key
     * is a non-blank {@link String}.
     *
     * @param values the map whose keys to validate
     * @param argName the argument name used in the exception message
     * @throws IllegalArgumentException if {@code values} is {@code null} or empty, or has a key that is
     *         not a {@link String} or is {@code null}, empty, or blank
     */
    static void checkSqlFragmentKeysNotBlank(final Map<?, ?> values, final String argName) {
        N.checkArgNotEmpty(values, argName);

        for (final Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new IllegalArgumentException(argName + " keys must be non-blank strings, but found: " + entry.getKey());
            }

            final String key = (String) entry.getKey();
            if (Strings.isBlank(key)) {
                checkSqlFragmentNotBlank(key, "Key in " + argName);
            }
        }
    }

    /**
     * Takes one insertion-order-preserving snapshot of a caller-owned map and validates the same
     * keys that the builder will subsequently render.
     */
    private static <V> Map<String, V> copyAndValidateSqlFragmentMap(final Map<String, V> values, final String argName) {
        N.checkArgNotNull(values, argName);

        final Map<String, V> copy = new LinkedHashMap<>(values);
        checkSqlFragmentKeysNotBlank(copy, argName);

        return copy;
    }

    /**
     * Validates the complete-sub-query argument of a set-operation overload
     * ({@code union}/{@code unionAll}/{@code intersect}/{@code except}/{@code minus} taking a single query
     * string, or the SQL built by the sibling-builder overloads). These overloads are dedicated to appending
     * a complete sub-query, so the argument must satisfy the {@link #isInlineQuery(String...)} heuristic.
     * The same validation is applied to every set-operation operand carried by a {@link Criteria}.
     *
     * @param query the query string to validate
     * @param operationName the set-operation SQL keyword (e.g. {@code "UNION"}) used in the error message
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, or does not appear to be a {@code SELECT} sub-query
     */
    private void checkSetOperationSubQuery(final String query, final String operationName) {
        checkSqlFragmentNotBlank(query, "query");

        if (!isInlineQuery(_tokenizer, query) || !_tokenizer.isSyntacticallyReadQuery(query)) {
            throw new IllegalArgumentException("The query argument to " + operationName
                    + " must be a complete SELECT sub-query (starting with 'SELECT', optionally wrapped in balanced parentheses, or containing 'SELECT ... FROM'), but was: \""
                    + query + "\". To start a new SELECT from a column list, use " + setOperationMethodName(operationName)
                    + "(Collection) followed by from(...).");
        }
    }

    /** Validates a query captured for a builder-backed derived table or condition subquery. */
    private void checkSubQuerySnapshot(final String query) {
        checkSqlFragmentNotBlank(query, "query");

        if (!isInlineQuery(_tokenizer, query) || !_tokenizer.isSyntacticallyReadQuery(query)) {
            throw new IllegalArgumentException("A builder-backed subquery must be a complete syntactic SELECT query candidate, but was: \"" + query + "\"");
        }
    }

    /**
     * Maps a set-operation SQL keyword to the builder method that accepts a column list, so the remediation
     * hint in {@link #checkSetOperationSubQuery(String, String)} names a method that actually exists
     * ({@code "UNION ALL"} -&gt; {@code unionAllSelect}, not {@code "UNION ALL(Collection)"}).
     *
     * @param operationName the set-operation SQL keyword
     * @return the corresponding builder method name, or {@code operationName} itself if it is not a known keyword
     */
    private static String setOperationMethodName(final String operationName) {
        return switch (operationName) {
            case SK.UNION -> "unionSelect";
            case SK.UNION_ALL -> "unionAllSelect";
            case SK.INTERSECT -> "intersectSelect";
            case SK.EXCEPT -> "exceptSelect";
            case SK.EXCEPT_MINUS -> "minusSelect";
            default -> operationName;
        };
    }

    /**
     * Returns the {@link SqlDialect} this builder renders SQL with.
     *
     * @return the complete rendering and tokenizer configuration bound to this builder
     */
    public SqlDialect sqlDialect() {
        return sqlDialect;
    }

    /**
     * Specifies the target table for an {@code INSERT} or {@code INSERT ... SELECT} operation.
     * <p>Must be called after setting the columns/values via {@code insert(...)} or the columns to copy via {@code select(...)}.
     * When chained after {@code select(...)}, the eventual {@code from(...)} call appends the source query, producing
     * {@code INSERT INTO target (cols) SELECT cols FROM source}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String insertSql = PSC.insert("firstName", "lastName").into("account").build().query();
     * // Output: INSERT INTO account (first_name, last_name) VALUES (?, ?)
     *
     * String insertSelectSql = PSC.select("firstName").into("account_backup").from("account").build().query();
     * // Output: INSERT INTO account_backup (first_name) SELECT first_name AS "firstName" FROM account
     * }</pre>
     *
     * @param tableName the name of the target table (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current operation is neither {@code ADD} nor {@code QUERY}, if columns/values
     *             have not been set, or if it is called after SQL has already been emitted (e.g., after {@code from()} or a second {@code into()})
     * @throws IllegalArgumentException if {@code tableName} is {@code null}, empty, or blank, or if a staged column name contains a SQL comment token
     */
    public This into(final String tableName) {
        checkCanAppendInto();
        checkSqlFragmentNotBlank(tableName, "tableName");

        final String normalizedTableName = tableName.trim();

        return mutateAtomically(() -> appendIntoClause(normalizedTableName));
    }

    /**
     * Renders the {@code INSERT INTO} clause (and, for {@code ADD} operations, the {@code VALUES} list)
     * after the public {@code into(...)} entry point has installed its mutation checkpoint. Rendering a
     * column can still reject an unsafe fragment after the INSERT prefix has been emitted, and
     * named-parameter rendering updates several correlated collections, so this must run transactionally.
     *
     * @param normalizedTableName the validated, trimmed target table name
     */
    private void appendIntoClause(final String normalizedTableName) {
        _tableName = normalizedTableName;

        _sb.append(_INSERT);
        _sb.append(_SPACE_INTO_SPACE);

        _sb.append(normalizedTableName);

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
                final Collection<String> selectPropNames = N.notEmpty(selection.includedPropNames()) ? selection.includedPropNames()
                        : QueryUtil.selectPropNames(selection.entityClass(), selection.includesSubEntityProperties(), selection.excludedPropNames());
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
                        _hasGeneratedParameterPlaceholder = true;

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

                            appendNamedParameter(nextNamedParameterName(columnName));
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
                            _sb.append(nextNamedParameterName(columnName));
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
    }

    /** Validates the structural preconditions shared by all {@code into(...)} overloads. */
    private void checkCanAppendInto() {
        assertNotClosed();

        if (!(_op == OperationType.ADD || _op == OperationType.QUERY)) {
            throw new IllegalStateException("Invalid operation for into(): " + _op + ". Expected ADD or QUERY");
        }

        if (_op == OperationType.QUERY) {
            if (N.isEmpty(_propOrColumnNames) && N.isEmpty(_propOrColumnNameAliases) && N.isEmpty(_multiSelects)) {
                throw new IllegalStateException("Column names must be set by select() before calling into()");
            }
        } else if (N.isEmpty(_propOrColumnNames) && N.isEmpty(_props) && N.isEmpty(_propsList)) {
            throw new IllegalStateException("Column names must be set by insert() before calling into()");
        }

        // Guard against calling into() after SQL has already been emitted (e.g. after from(), or a second
        // into()), which would blindly concatenate a new INSERT fragment onto the existing statement.
        if (!_sb.isEmpty()) {
            throw new IllegalStateException("into() must be called before from() and any other SQL-emitting method, and can only be called once");
        }
    }

    /**
     * Specifies the target table for an {@code INSERT} or {@code INSERT ... SELECT} operation using an entity class.
     * <p>The table name will be derived from the entity class based on the naming policy. Installing
     * the entity mapping and rendering the INSERT clause are atomic; if rendering fails, neither the
     * mapping nor a partial clause is retained.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.insert(account).into(Account.class).build().query();
     * // Table name derived from Account class based on naming policy
     * }</pre>
     *
     * @param entityClass the entity class representing the target table (must not be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current operation is neither {@code ADD} nor {@code QUERY}, if columns/values
     *             have not been set, or if it is called after SQL has already been emitted (e.g., after {@code from()} or a second {@code into()})
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class, or if a staged column
     *         name contains a SQL comment token
     */
    public This into(final Class<?> entityClass) {
        checkCanAppendInto();
        N.checkArgNotNull(entityClass, cs.entityClass);

        return mutateAtomically(() -> {
            setEntityClass(entityClass);
            appendIntoClause(getTableName(entityClass, _namingPolicy));
        });
    }

    /**
     * Specifies the target table for an {@code INSERT} or {@code INSERT ... SELECT} operation with an explicit table name and entity class.
     * Installing the optional entity mapping and rendering the INSERT clause are atomic; if rendering
     * fails, neither the mapping nor a partial clause is retained.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.insert(account).into("account_archive", Account.class).build().query();
     * // Inserts into specified table with Account class mapping
     * }</pre>
     *
     * @param tableName the name of the target table (must not be {@code null}, empty, or blank)
     * @param entityClass the entity class for property mapping (may be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current operation is neither {@code ADD} nor {@code QUERY}, if columns/values
     *             have not been set, or if it is called after SQL has already been emitted (e.g., after {@code from()} or a second {@code into()})
     * @throws IllegalArgumentException if {@code tableName} is {@code null}, empty, or blank, or if a staged column name contains a SQL comment token
     */
    public This into(final String tableName, final Class<?> entityClass) {
        checkCanAppendInto();
        checkSqlFragmentNotBlank(tableName, "tableName");
        final String normalizedTableName = tableName.trim();

        return mutateAtomically(() -> {
            if (entityClass != null) {
                setEntityClass(entityClass);
            }

            appendIntoClause(normalizedTableName);
        });
    }

    /**
     * Adds the {@code DISTINCT} select modifier to the SELECT statement.
     * <p>This method is equivalent to calling {@code selectModifier(DISTINCT)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("name").distinct().from("account").build().query();
     * // Output: SELECT DISTINCT name FROM account
     * }</pre>
     *
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent a SELECT query, or a select
     *                               modifier has already been set for the current SELECT segment
     */
    public This distinct() { //NOSONAR
        return selectModifier(DISTINCT);
    }

    /**
     * Adds PostgreSQL-style {@code DISTINCT ON (expressions)} to the SELECT statement.
     * A {@code null}, empty, or blank expression falls back to plain {@link #distinct()}.
     * The expression is trusted SQL and the target database must support this syntax.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * String sql = PSC.select("department")
     *                 .distinctOn("department")
     *                 .from("employees")
     *                 .build().query();
     * // Output: SELECT DISTINCT ON (department) department FROM employees
     * }</pre>
     *
     * @param expressions the expressions inside {@code DISTINCT ON (...)}; blank means plain {@code DISTINCT}
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent a SELECT query, or a select
     *                               modifier has already been set for the current SELECT segment
     */
    public This distinctOn(final String expressions) {
        return Strings.isBlank(expressions) ? distinct() : selectModifier(DISTINCT + " ON (" + expressions + ")");
    }

    /**
     * Adds the MySQL-style {@code DISTINCTROW} modifier to the SELECT statement.
     * The target database must support this syntax.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * String sql = PSC.select("department").distinctRow().from("employees").build().query();
     * // Output: SELECT DISTINCTROW department FROM employees
     * }</pre>
     *
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent a SELECT query, or a select
     *                               modifier has already been set for the current SELECT segment
     */
    public This distinctRow() {
        return selectModifier(SK.DISTINCTROW);
    }

    /**
     * Adds a pre-select modifier to the SELECT statement.
     * <p>For better performance, this method should be called before {@code from}.
     * A {@code null} or empty value is silently ignored; a non-empty but blank value is rejected.</p>
     *
     * <p>The modifier applies only to the current SELECT segment: starting a new set-operation
     * segment ({@code union}, {@code unionAll}, {@code intersect}, {@code except}, {@code minus})
     * clears it, so each segment can carry its own modifier. A modifier staged after a set operation
     * that appended a complete sub-query (e.g. {@code union("SELECT ...")}) has no SELECT of its own
     * to attach to, and {@code build()} fails with an {@link IllegalStateException} rather than
     * dropping it silently.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").selectModifier("TOP 10").from("account").build().query();
     * // Output: SELECT TOP 10 * FROM account
     * }</pre>
     *
     * <p>The text is emitted verbatim and is <b>not</b> trimmed, so {@code selectModifier(" DISTINCT ")} renders
     * {@code "SELECT  DISTINCT  id FROM t"} with the padding kept. This differs from
     * {@link com.landawn.abacus.query.condition.Criteria.Builder#selectModifier(String)}, whose value is stripped
     * (and an all-blank one is normalized to "no modifier" instead of rejected).</p>
     *
     * @param selectModifier modifiers like {@code ALL}, {@code DISTINCT}, {@code DISTINCTROW},
     *                       {@code TOP}, etc.; may be {@code null} or empty (no-op)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent a SELECT query, or a select
     *                               modifier has already been set for the current SELECT segment
     * @throws IllegalArgumentException if {@code selectModifier} is non-empty but blank (whitespace only)
     */
    public This selectModifier(final String selectModifier) {
        assertNotClosed();

        if (Strings.isEmpty(selectModifier)) {
            return (This) this;
        }

        checkSqlFragmentNotBlank(selectModifier, "selectModifier");

        if (_op != OperationType.QUERY || _isForConditionOnly) {
            throw new IllegalStateException("selectModifier() is only valid for SELECT queries");
        }

        if (Strings.isNotEmpty(_selectModifier)) {
            throw new IllegalStateException("selectModifier has already been set and cannot be set again");
        }

        _selectModifier = selectModifier;

        // Insert into the buffer only if the current segment's SELECT keyword has already been emitted;
        // otherwise the modifier is emitted by appendOperationBeforeFrom() when from(...) runs. A raw
        // indexOf("SELECT") search must not be used here: it can match a column/table name containing
        // "SELECT" (e.g. "SELECTED_FLAG") or the SELECT of an earlier set-operation segment.
        if (_selectKeywordEndIdx >= 0) {
            final int len = _sb.length();

            _sb.append(_SPACE);

            // The modifier is a raw SQL fragment (e.g. "DISTINCT", "TOP 10", "SQL_CALC_FOUND_ROWS") and must
            // be emitted verbatim: appendStringExpr would apply column-name normalization, corrupting any
            // modifier keyword the naming policy does not recognize (e.g. camelCasing "SQL_CALC_FOUND_ROWS").
            _sb.append(_selectModifier);

            final int newLength = _sb.length();

            _sb.insert(_selectKeywordEndIdx, _sb.substring(len));
            _sb.setLength(newLength);
        }

        return (This) this;
    }

    /**
     * Sets the FROM clause with multiple table names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from("users", "orders", "items").build().query();
     * // Output: SELECT * FROM users, orders, items
     * }</pre>
     *
     * @param tableNames the table names to use in the FROM clause (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code tableNames} is {@code null} or empty, contains a {@code null}, empty, or blank element,
     *         or if a staged select column name contains a SQL comment token
     *         or has an explicit top-level {@code AS} alias containing a quote character or line break
     * @throws IllegalStateException if this builder is closed, if the current operation is not {@code QUERY}, no columns have been set by
     *                               {@code select()}, or {@code from(...)} was already called for this query segment
     */
    public This from(final String... tableNames) {
        N.checkArgNotEmpty(tableNames, cs.tableNames);

        return from(Array.asList(tableNames));
    }

    /**
     * Sets the FROM clause with a collection of table names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> tables = Arrays.asList("users", "orders");
     * String sql = PSC.select("*").from(tables).build().query();
     * // Output: SELECT * FROM users, orders
     * }</pre>
     *
     * @param tableNames the collection of table names to use in the FROM clause (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code tableNames} is {@code null} or empty, contains a {@code null}, empty, or blank element,
     *         or if a staged select column name contains a SQL comment token
     *         or has an explicit top-level {@code AS} alias containing a quote character or line break
     * @throws IllegalStateException if this builder is closed, if the current operation is not {@code QUERY}, no columns have been set by
     *                               {@code select()}, or {@code from(...)} was already called for this query segment
     */
    public This from(final Collection<String> tableNames) {
        final List<String> tableNamesSnapshot = copyAndValidateSqlFragments(tableNames, "tableNames");

        final List<String> normalizedTableNames = new ArrayList<>(tableNamesSnapshot.size());
        int idx = 0;

        for (final String tableName : tableNamesSnapshot) {
            final String normalizedTableName = tableName.trim();
            N.checkArgNotEmpty(normalizedTableName, "tableNames[" + idx + "]");
            normalizedTableNames.add(normalizedTableName);
            idx++;
        }

        if (normalizedTableNames.size() == 1) {
            return from(normalizedTableNames.get(0));
        }

        // Derive the primary table reference exactly as from(String) does: only the text before the first
        // top-level separator (comma or JOIN) supplies the table/alias used while rendering entity properties.
        final String firstTableName = normalizedTableNames.get(0);
        final int separatorIdx = findFirstTopLevelFromSeparator(firstTableName, _dialectFamily == DialectFamily.SQL_SERVER);
        final String localTableName = (separatorIdx > 0 ? firstTableName.substring(0, separatorIdx) : firstTableName).trim();

        return appendFromClause(localTableName, Strings.join(normalizedTableNames, SK.COMMA_SPACE));
    }

    /**
     * Sets the FROM clause with a single expression.
     * <p>The expression can be a table name, subquery, or multiple tables separated by commas.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from("users u").build().query();
     * // Output: SELECT * FROM users u
     *
     * String sql2 = PSC.select("*").from("(SELECT * FROM users) t").build().query();
     * // Output: SELECT * FROM (SELECT * FROM users) t
     * }</pre>
     *
     * @param expr the FROM clause expression (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or if a staged select column name contains a SQL comment token
     *         or has an explicit top-level {@code AS} alias containing a quote character or line break
     * @throws IllegalStateException if this builder is closed, if the current operation is not {@code QUERY}, no columns have been set by
     *                               {@code select()}, or {@code from(...)} was already called for this query segment
     */
    public This from(final String expr) {
        N.checkArgNotEmpty(expr, cs.expr);
        final String trimmedExpr = expr.trim();
        N.checkArgNotEmpty(trimmedExpr, cs.expr);

        // Only the first table reference supplies the primary table/alias used while rendering entity
        // properties. A raw FROM body may contain quoted commas or an inline JOIN; a character-only comma
        // scan either split quoted identifiers (for example, "accounts,archive") or handed the whole JOIN
        // to the alias scanner, which then mistook the final predicate token for the primary table alias.
        final int separatorIdx = findFirstTopLevelFromSeparator(trimmedExpr, _dialectFamily == DialectFamily.SQL_SERVER);
        final String localTableName = separatorIdx > 0 ? trimmedExpr.substring(0, separatorIdx) : trimmedExpr;

        return appendFromClause(localTableName.trim(), trimmedExpr);
    }

    /**
     * Uses a complete SELECT built by another builder as a derived table in this query's {@code FROM}
     * clause. After argument, parent-state, and SQL-policy prevalidation succeeds, the child builder is
     * finalized and consumed. A failure during the preceding prevalidation leaves the child reusable;
     * a failure while finalizing or validating the child's built SQL consumes it. Child parameters are
     * merged before parameters from subsequent outer clauses, and generated named parameters are renamed
     * when necessary to avoid collisions with the parent query.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * SqlBuilder activeUsers = PSC.select("id").from("users").where(Filters.eq("active", true));
     * String sql = PSC.select("*").from(activeUsers, "u").build().query();
     * // Output: SELECT * FROM (SELECT id FROM users WHERE active = ?) u
     * }</pre>
     *
     * @param sqlBuilder the child builder supplying a complete, lexically SELECT-only query (a syntactic {@code SELECT} candidate); consumed once child finalization begins
     * @param alias the derived-table alias, emitted as trusted SQL
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, is not building a SELECT, has no projection,
     *                               or already has a FROM clause, or if the child is incomplete or was already consumed
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this builder, does not build a
     *                                  complete, lexically SELECT-only {@code SELECT} query, uses an incompatible parameter policy, or
     *                                  {@code alias} is {@code null}, empty, or blank
     */
    public This from(final This sqlBuilder, final String alias) {
        checkCanAppendFrom();
        N.checkArgNotNull(sqlBuilder, cs.sqlBuilder);
        N.checkArgument(sqlBuilder != this, "A builder cannot use itself as a derived table");
        checkSqlFragmentNotBlank(alias, "alias");
        N.checkArgument(_sqlPolicy == sqlBuilder._sqlPolicy || !sqlBuilder._hasGeneratedParameterPlaceholder,
                "A derived-table child with generated parameter placeholders must use the parent's SQL policy: parent=" + _sqlPolicy + ", child="
                        + sqlBuilder._sqlPolicy);

        final String normalizedAlias = alias.trim();
        final SubQuerySnapshot subQuery = sqlBuilder.buildSubQuery();

        return mutateAtomically(() -> {
            final String query = prepareSubQuerySnapshot(subQuery);
            final String fromClause = "(" + query + ") " + normalizedAlias;
            appendSelectListAndFromClause(fromClause, fromClause);
        });
    }

    /**
     * Locates the first top-level separator after the primary table reference in a raw FROM body.
     * Commas and JOIN-family keywords inside quoted regions, comments, or parentheses are ignored.
     *
     * @param fromClause the trimmed text that will be emitted after {@code FROM}
     * @param sqlServerTempIdentifiers whether {@code #name}/{@code ##name} are SQL Server temporary-table
     *        identifiers (data tokens) rather than MySQL hash comments
     * @return the separator index, or {@code -1} when the clause contains one table reference
     */
    private static int findFirstTopLevelFromSeparator(final String fromClause, final boolean sqlServerTempIdentifiers) {
        int depth = 0;

        for (int i = 0, len = fromClause.length(); i < len; i++) {
            final int next = skipSqlQuotedOrComment(fromClause, i, sqlServerTempIdentifiers);

            if (next != i) {
                i = next - 1;
                continue;
            }

            final char ch = fromClause.charAt(i);

            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                if (depth > 0) {
                    depth--;
                }
            } else if (depth == 0 && (ch == SK._COMMA || isTopLevelJoinStart(fromClause, i))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Reports whether a JOIN-family keyword (e.g. {@code JOIN}, {@code LEFT}, {@code CROSS},
     * {@code STRAIGHT_JOIN}) starts at {@code index}, preceded by whitespace or a block-comment end.
     * The caller is responsible for the nesting-depth check.
     */
    private static boolean isTopLevelJoinStart(final String sql, final int index) {
        if (index <= 0 || !isJoinLeadingTrivia(sql, index)) {
            return false;
        }

        return isSqlWordAt(sql, index, SK.JOIN) || isSqlWordAt(sql, index, "INNER") || isSqlWordAt(sql, index, "LEFT") || isSqlWordAt(sql, index, "RIGHT")
                || isSqlWordAt(sql, index, "FULL") || isSqlWordAt(sql, index, "CROSS") || isSqlWordAt(sql, index, "NATURAL") || isSqlWordAt(sql, index, "OUTER")
                || isSqlWordAt(sql, index, "STRAIGHT_JOIN") || isSqlWordAt(sql, index, "ASOF") || isSqlWordAt(sql, index, "SEMI")
                || isSqlWordAt(sql, index, "ANTI");
    }

    /** Reports whether the character before {@code index} can separate a table reference from a JOIN keyword. */
    private static boolean isJoinLeadingTrivia(final String sql, final int index) {
        final char previous = sql.charAt(index - 1);
        return Character.isWhitespace(previous) || (previous == '/' && index > 1 && sql.charAt(index - 2) == '*');
    }

    /** Reports whether {@code word} occurs at {@code index} (case-insensitively) followed by a keyword boundary. */
    private static boolean isSqlWordAt(final String sql, final int index, final String word) {
        final int end = index + word.length();
        return end <= sql.length() && sql.regionMatches(true, index, word, 0, word.length()) && isAliasKeywordBoundary(sql, end);
    }

    /**
     * Reports whether a raw JOIN expression already supplies its top-level {@code ON} or {@code USING}
     * connector. Nested subqueries, quoted regions and comments are ignored; connector-looking text
     * there must not close the outer join's still-available connector slot.
     */
    private boolean containsTopLevelJoinCondition(final String joinExpr) {
        final boolean sqlServerTempIdentifiers = _dialectFamily == DialectFamily.SQL_SERVER;
        int depth = 0;

        for (int i = 0, len = joinExpr.length(); i < len; i++) {
            final int next = skipSqlQuotedOrComment(joinExpr, i, sqlServerTempIdentifiers);

            if (next != i) {
                i = next - 1;
                continue;
            }

            final char ch = joinExpr.charAt(i);

            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                if (depth > 0) {
                    depth--;
                }
            } else if (depth == 0 && isAliasKeywordBoundary(joinExpr, i - 1) && (isSqlWordAt(joinExpr, i, SK.ON) || isSqlWordAt(joinExpr, i, SK.USING))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validates that {@code condition} is a non-{@code null} predicate usable in a WHERE/HAVING/ON position.
     *
     * @param condition the condition to validate
     * @return the validated predicate condition
     * @throws IllegalArgumentException if {@code condition} is {@code null}, has a {@code null} operator, or
     *         is/contains a {@link Criteria}, standalone {@link SubQuery}, SQL clause, JOIN, {@code ON}/{@code USING}
     *         connector, quantified-subquery operand, or blank {@link SqlExpression}. An empty {@link com.landawn.abacus.query.condition.Junction} is
     *         accepted and renders its Boolean identity (for example {@code 1 = 1})
     */
    private static Condition validatePredicateCondition(final Condition condition) {
        N.checkArgNotNull(condition, cs.condition);

        return new On(condition).condition();
    }

    /**
     * Sets the FROM clause with an expression and associates it with an entity class.
     * Installing the optional entity mapping and rendering the SELECT/FROM text are atomic; if
     * rendering fails, neither the mapping nor a partial clause is retained.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from("users u", User.class).build().query();
     * // Associates the User class for property mapping
     * }</pre>
     *
     * @param expr the FROM clause expression (must not be {@code null}, empty, or blank)
     * @param entityClass the entity class for property mapping (may be {@code null}, in which case no entity-class association is performed)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current operation is not {@code QUERY}, no columns have been set by
     *                               {@code select()}, or {@code from(...)} was already called for this query segment
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or if a staged select column name contains a SQL comment token
     *         or has an explicit top-level {@code AS} alias containing a quote character or line break
     */
    public This from(final String expr, final Class<?> entityClass) {
        checkCanAppendFrom();
        checkSqlFragmentNotBlank(expr, "expr");

        return mutateAtomically(() -> {
            if (entityClass != null) {
                setEntityClass(entityClass);
            }

            from(expr);
        });
    }

    /**
     * Sets the FROM clause using an entity class.
     * <p>The table name will be derived from the entity class.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).build().query();
     * // Table name derived from User class based on naming policy
     * }</pre>
     *
     * @param entityClass the entity class representing the table (must not be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class, or if a staged select
     *         column name contains a SQL comment token or has an explicit top-level {@code AS} alias containing a quote character or line break
     * @throws IllegalStateException if this builder is closed, if the current operation is not {@code QUERY}, no columns have been set by
     *                               {@code select()}, or {@code from(...)} was already called for this query segment
     */
    public This from(final Class<?> entityClass) {
        return from(entityClass, QueryUtil.tableAlias(entityClass));
    }

    /**
     * Sets the FROM clause using an entity class with an alias.
     * Installing the entity mapping and rendering the SELECT/FROM text are atomic; if rendering
     * fails, neither the mapping nor a partial clause is retained.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").build().query();
     * // Output: SELECT * FROM user u (table name derived from User class based on naming policy)
     * }</pre>
     *
     * @param entityClass the entity class representing the table (must not be {@code null})
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current operation is not {@code QUERY}, no columns have been set by
     *                               {@code select()}, or {@code from(...)} was already called for this query segment
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class, or if a staged select
     *         column name contains a SQL comment token or has an explicit top-level {@code AS} alias containing a quote character or line break
     */
    public This from(final Class<?> entityClass, final String alias) {
        checkCanAppendFrom();
        N.checkArgNotNull(entityClass, cs.entityClass);

        return mutateAtomically(() -> {
            setEntityClass(entityClass);

            if (Strings.isEmpty(alias)) {
                from(getTableName(entityClass, _namingPolicy));
            } else {
                from(getTableName(entityClass, _namingPolicy) + " " + alias);
            }
        });
    }

    /**
     * Sets the FROM clause using the specified entity class and multiple table names.
     * Installing the entity mapping and rendering the SELECT/FROM text are atomic; if rendering
     * fails, neither the mapping nor a partial clause is retained.
     *
     * @param entityClass the entity class to associate with this query (must not be {@code null})
     * @param tableNames the collection of table names for the FROM clause (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this builder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current operation is not {@code QUERY}, no columns have been set by
     *                               {@code select()}, or {@code from(...)} was already called for this query segment
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, if {@code tableNames} is {@code null} or empty or contains a
     *         {@code null}, empty, or blank element, or if a staged select column name contains a SQL comment token
     *         or has an explicit top-level {@code AS} alias containing a quote character or line break
     */
    protected This from(final Class<?> entityClass, final Collection<String> tableNames) {
        checkCanAppendFrom();
        N.checkArgNotNull(entityClass, cs.entityClass);

        return mutateAtomically(() -> {
            setEntityClass(entityClass);
            from(tableNames);
        });
    }

    /**
     * Compatibility bridge for subclasses compiled against the former two-string protected helper.
     * A legacy helper call supplies a primary table reference followed by a complete FROM body that
     * begins with that same reference; such calls retain their original behavior. Otherwise the two
     * arguments are treated as two table expressions, matching the public {@link #from(String...)} API.
     * New subclass code should call the public FROM overloads rather than this internal bridge.
     *
     * @param tableName the legacy primary table reference, or the first table expression
     * @param fromClause the legacy complete FROM body, or the second table expression
     * @return this builder instance for method chaining
     * @deprecated retained for source and binary compatibility with subclasses; use a public {@code from(...)} overload
     */
    @Deprecated
    protected This from(final String tableName, final String fromClause) {
        if (isLegacyPrimaryTableAndFromClause(tableName, fromClause)) {
            return appendFromClause(tableName, fromClause);
        }

        return from(Arrays.asList(tableName, fromClause));
    }

    /**
     * Returns whether the pair has the shape accepted by the former protected helper: the complete
     * FROM body starts with the supplied primary table reference and then ends or continues at a SQL
     * token boundary. The boundary check avoids mistaking two tables such as {@code users} and
     * {@code users_archive} for a legacy helper call.
     */
    private static boolean isLegacyPrimaryTableAndFromClause(final String tableName, final String fromClause) {
        if (Strings.isBlank(tableName) || Strings.isBlank(fromClause)) {
            return false;
        }

        final String primary = tableName.trim();
        final String body = fromClause.trim();

        if (!body.regionMatches(true, 0, primary, 0, primary.length())) {
            return false;
        }

        if (body.length() == primary.length()) {
            return true;
        }

        final char boundary = body.charAt(primary.length());
        return Character.isWhitespace(boundary) || boundary == ',' || boundary == ')' || boundary == ';';
    }

    /**
     * Sets the FROM clause using a separate primary table name and a complete FROM-body expression.
     * <p>{@code tableName} is parsed to extract the optional alias and used to set the builder's
     * {@code _tableName}/{@code _tableAlias} state, while {@code fromClause} is appended verbatim
     * after the {@code FROM} keyword (it may include commas, joins, subqueries, etc.).</p>
     *
     * @param tableName the primary table name (with optional alias) used for column resolution
     * @param fromClause the full text emitted after {@code FROM} (e.g. {@code "users u, orders o"})
     * @return this builder instance for method chaining
     */
    private This appendFromClause(final String tableName, final String fromClause) {
        return mutateAtomically(() -> appendSelectListAndFromClause(tableName, fromClause));
    }

    /**
     * Renders the SELECT list and FROM clause after {@link #appendFromClause(String, String)} has installed its
     * mutation checkpoint. Rendering a staged select column can still reject an unsafe fragment after
     * the SELECT prefix has been emitted, so this must run transactionally to keep a failed
     * {@code from(...)} from leaving a partial SELECT behind.
     *
     * @param tableName the primary table name (with optional alias) used for column resolution
     * @param fromClause the full text emitted after {@code FROM}
     */
    private void appendSelectListAndFromClause(final String tableName, final String fromClause) {
        appendOperationBeforeFrom(tableName);

        final boolean withAlias = Strings.isNotEmpty(_tableAlias);
        final boolean isForSelect = _op == OperationType.QUERY;

        if (N.notEmpty(_propOrColumnNames)) {
            if (_entityClass != null && !withAlias && _propOrColumnNames == QueryUtil.selectPropNames(_entityClass, false, null)) { // NOSONAR
                final Map<Class<?>, String> fullSelectPartsCache = (_identifierQuote == SK._BACKTICK ? fullSelectPartsPoolForBacktick : fullSelectPartsPool)
                        .get(_namingPolicy);
                String fullSelectParts = fullSelectPartsCache.get(_entityClass);

                if (Strings.isEmpty(fullSelectParts)) {
                    final StringBuilder sb = new StringBuilder();

                    int i = 0;
                    for (final String columnName : _propOrColumnNames) {
                        if (i++ > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }

                        sb.append(normalizeColumnName(_propColumnNameMap, columnName));

                        if (_namingPolicy != NamingPolicy.NO_CHANGE && !SK.ASTERISK.equals(columnName)) {
                            sb.append(SPACE_AS_SPACE).append(_identifierQuote).append(columnName).append(_identifierQuote);
                        }
                    }

                    fullSelectParts = sb.toString();

                    fullSelectPartsCache.put(_entityClass, fullSelectParts);
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
            // Merge into any mapping registered moments earlier (appendOperationBeforeFrom registers the
            // main table's alias via addPropColumnMapForAlias); replacing the map would drop it.
            if (_aliasPropColumnNameMap == null) {
                _aliasPropColumnNameMap = N.newHashMap(_multiSelects.size());
            }

            for (final Selection selection : _multiSelects) {
                if (Strings.isNotEmpty(selection.tableAlias())) {
                    _aliasPropColumnNameMap.put(selection.tableAlias(), propToColumnInfoMap(selection.entityClass(), _namingPolicy));
                }
            }

            Class<?> selectionEntityClass = null;
            BeanInfo selectionBeanInfo = null;
            ImmutableMap<String, ColumnInfo> selectionPropColumnNameMap = null;
            String selectionTableAlias = null;
            String selectionClassAlias = null;
            boolean selectionWithClassAlias = false;

            int i = 0;

            for (final Selection selection : _multiSelects) {
                selectionEntityClass = selection.entityClass();
                selectionBeanInfo = Beans.isBeanClass(selectionEntityClass) ? ParserUtil.getBeanInfo(selectionEntityClass) : null;
                selectionPropColumnNameMap = Beans.isBeanClass(selectionEntityClass) ? propToColumnInfoMap(selectionEntityClass, _namingPolicy) : null;
                selectionTableAlias = selection.tableAlias();

                selectionClassAlias = selection.classAlias();
                selectionWithClassAlias = Strings.isNotEmpty(selectionClassAlias);

                final Collection<String> selectPropNames = N.notEmpty(selection.includedPropNames()) ? selection.includedPropNames()
                        : QueryUtil.selectPropNames(selectionEntityClass, selection.includesSubEntityProperties(), selection.excludedPropNames());

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
    }

    /**
     * Appends the SELECT operation and modifier to the SQL string builder before the FROM clause.
     * Parses the table name to extract a top-level table alias if present, ignoring whitespace and
     * {@code AS} text inside quoted regions, comments, and nested parentheses. Also validates that
     * the operation is a QUERY and that column names have been set.
     *
     * @param tableName the table name, optionally including an alias (e.g., "users u"; must not be {@code null})
     * @throws IllegalStateException if this builder is closed, if the current operation is not {@code QUERY}, no columns have been set by
     *                               {@code select()}, or {@code from(...)} was already called for this query segment
     * @throws IllegalArgumentException if {@code tableName} is {@code null}
     */
    protected void appendOperationBeforeFrom(final String tableName) {
        checkCanAppendFrom();
        N.checkArgNotNull(tableName, cs.tableName);

        final String trimmedTableName = tableName.trim();
        final TopLevelAlias tableAlias = findTopLevelAlias(trimmedTableName, false);

        if (tableAlias != null) {
            _tableName = trimmedTableName.substring(0, tableAlias.expressionEnd()).trim();
            _tableAlias = normalizeTableAlias(trimmedTableName.substring(tableAlias.aliasStart(), tableAlias.aliasEnd()).trim());
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
        _selectKeywordEndIdx = _sb.length();
        _sb.append(_SPACE);

        if (Strings.isNotEmpty(_selectModifier)) {
            // Emitted verbatim -- see selectModifier(String) for why appendStringExpr must not be used here.
            _sb.append(_selectModifier);

            _sb.append(_SPACE);
        }
    }

    /** Validates the structural preconditions shared by all {@code from(...)} overloads. */
    private void checkCanAppendFrom() {
        assertNotClosed();

        if (_op != OperationType.QUERY) {
            throw new IllegalStateException("Invalid operation for from(): " + _op + ". Expected QUERY");
        }

        // Guard against a second from() in the same query segment, which would silently emit a second
        // "SELECT ... FROM ..." fragment. Set operations (union/intersect/...) reset this flag when they
        // start a new segment, so multi-segment queries are unaffected.
        if (_hasFromBeenSet) {
            throw new IllegalStateException("from() has already been called for the current query segment");
        }

        if (N.isEmpty(_propOrColumnNames) && N.isEmpty(_propOrColumnNameAliases) && N.isEmpty(_multiSelects)) {
            throw new IllegalStateException("Column names must be set by select() before calling from()");
        }
    }

    /**
     * Registers a property-to-column-name mapping for a table alias.
     * This allows column names to be resolved correctly when a table alias is used in the query.
     *
     * <p>Only the alias-scoped mapping is registered: the joined entity never supplies this builder's primary
     * mapping, so an unqualified property name in a later clause keeps resolving against the FROM table (and,
     * for a plain-string FROM, is emitted verbatim rather than qualified with the FROM table's alias).</p>
     *
     * @param entityClass the entity class whose property-column mapping to register
     * @param alias the table alias to associate with the mapping
     */
    protected void addPropColumnMapForAlias(final Class<?> entityClass, final String alias) {
        if (_aliasPropColumnNameMap == null) {
            _aliasPropColumnNameMap = new HashMap<>();
        }

        // Only the alias-scoped mapping is registered here. Adopting the joined entity's mapping as the
        // builder's PRIMARY mapping (when the FROM table was a plain string) resolved unqualified property
        // names in later clauses through the joined entity and prefixed them with the primary table's alias.
        _aliasPropColumnNameMap.put(alias, Beans.isBeanClass(entityClass) ? propToColumnInfoMap(entityClass, _namingPolicy) : _propColumnNameMap);
    }

    /**
     * Strips a leading {@code AS} keyword (case-insensitive) from a table alias.
     * {@code null} and empty aliases are returned unchanged.
     */
    private static String normalizeTableAlias(final String tableAlias) {
        if (Strings.isEmpty(tableAlias)) {
            return tableAlias;
        }

        if (Strings.startsWithIgnoreCase(tableAlias, SK.AS + SK._SPACE)) {
            return tableAlias.substring(3).trim();
        }

        return tableAlias;
    }

    /**
     * Finds an alias boundary at SQL nesting depth zero. Quoted strings/identifiers, line, hash and
     * block comments, and parenthesized regions are skipped, so their whitespace, parentheses and
     * {@code AS} text cannot be mistaken for the outer alias boundary. Table aliases must be final
     * non-comment tokens and must not follow a qualification dot; this prevents fragments such as
     * {@code unnest (items)} and {@code schema . table} from being misclassified, while excluding
     * trailing comments from the alias span. For a SELECT expression, the first explicit top-level
     * {@code AS} boundary is retained because one {@code select(String)} argument may contain a complete
     * comma-separated expression list whose suffix must be rendered and validated intact.
     *
     * @param sqlFragment the table or select-expression fragment to scan
     * @param explicitAsRequired whether only an explicit top-level {@code AS} is accepted; if
     *        {@code false}, the last top-level trivia boundary is accepted as an implicit table-alias separator
     * @return the expression end and alias token span, or {@code null} when no qualifying alias is present
     */
    private static TopLevelAlias findTopLevelAlias(final String sqlFragment, final boolean explicitAsRequired) {
        final int len = sqlFragment.length();
        int depth = 0;
        char quoteChar = 0;
        boolean bracketQuoted = false;
        boolean backslashEscaped = false;
        int pendingImplicitExpressionEnd = -1;
        TopLevelAlias explicitAlias = null;
        TopLevelAlias implicitAlias = null;
        boolean explicitAliasKeywordSeen = false;

        for (int i = 0; i < len; i++) {
            final char ch = sqlFragment.charAt(i);

            if (quoteChar != 0) {
                if (ch == quoteChar) {
                    if (backslashEscaped) {
                        backslashEscaped = false;
                    } else if (i < len - 1 && sqlFragment.charAt(i + 1) == quoteChar) {
                        i++;
                    } else {
                        quoteChar = 0;
                    }
                } else if (ch == '\\' && quoteChar == SK._SINGLE_QUOTE) {
                    // Backslash escapes apply only inside single-quoted string literals (MySQL semantics);
                    // quoted identifiers ("..." / `...`) use doubling only — mirror skipSqlQuotedOrComment.
                    backslashEscaped = !backslashEscaped;
                } else {
                    backslashEscaped = false;
                }

                continue;
            }

            if (bracketQuoted) {
                if (ch == ']') {
                    if (i < len - 1 && sqlFragment.charAt(i + 1) == ']') {
                        i++;
                    } else {
                        bracketQuoted = false;
                    }
                }

                continue;
            }

            if (Character.isWhitespace(ch)) {
                if (depth == 0 && pendingImplicitExpressionEnd < 0) {
                    pendingImplicitExpressionEnd = i;
                }

                continue;
            } else if (ch == '-' && i < len - 1 && sqlFragment.charAt(i + 1) == '-') {
                if (depth == 0 && pendingImplicitExpressionEnd < 0) {
                    pendingImplicitExpressionEnd = i;
                }

                i += 2;

                while (i < len && sqlFragment.charAt(i) != '\n' && sqlFragment.charAt(i) != '\r') {
                    i++;
                }

                i--;
                continue;
            } else if (ch == '#' && isAliasScannerHashCommentStart(sqlFragment, i)) {
                if (depth == 0 && pendingImplicitExpressionEnd < 0) {
                    pendingImplicitExpressionEnd = i;
                }

                while (++i < len && sqlFragment.charAt(i) != '\n' && sqlFragment.charAt(i) != '\r') {
                    // Skip MySQL hash comment.
                }

                i--;
                continue;
            } else if (ch == '/' && i < len - 1 && sqlFragment.charAt(i + 1) == '*') {
                if (depth == 0 && pendingImplicitExpressionEnd < 0) {
                    pendingImplicitExpressionEnd = i;
                }

                i += 2;

                while (i < len - 1 && !(sqlFragment.charAt(i) == '*' && sqlFragment.charAt(i + 1) == '/')) {
                    i++;
                }

                if (i < len - 1) {
                    i++;
                }

                continue;
            }

            if (depth == 0 && pendingImplicitExpressionEnd >= 0) {
                if (pendingImplicitExpressionEnd > 0) {
                    implicitAlias = new TopLevelAlias(pendingImplicitExpressionEnd, i, findAliasTokenEnd(sqlFragment, i));
                }

                pendingImplicitExpressionEnd = -1;
            }

            if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK) {
                quoteChar = ch;
                backslashEscaped = false;
                continue;
            } else if (ch == '[') {
                bracketQuoted = true;
                continue;
            } else if (ch == '(') {
                depth++;
                continue;
            } else if (ch == ')') {
                if (depth > 0) {
                    depth--;
                }

                continue;
            }

            if (depth != 0) {
                continue;
            }

            if ((ch == 'A' || ch == 'a') && i < len - 1 && (sqlFragment.charAt(i + 1) == 'S' || sqlFragment.charAt(i + 1) == 's')
                    && isAliasKeywordBoundary(sqlFragment, i - 1) && isAliasKeywordBoundary(sqlFragment, i + 2)) {
                if (i > 0) {
                    explicitAliasKeywordSeen = true;

                    final int aliasStart = skipAliasTrivia(sqlFragment, i + 2);

                    if ((explicitAlias == null || !explicitAsRequired) && aliasStart >= 0 && aliasStart < len) {
                        // SELECT accepts a complete comma-separated expression string, so it preserves the
                        // first boundary. A table fragment keeps the last boundary because an earlier AS may
                        // belong to temporal-table AS OF syntax rather than to its optional final alias.
                        explicitAlias = new TopLevelAlias(i, aliasStart, findAliasTokenEnd(sqlFragment, aliasStart));
                    }
                }

                i++;
            }
        }

        if (explicitAsRequired) {
            return explicitAlias;
        }

        if (isValidTopLevelAlias(sqlFragment, explicitAlias)) {
            return explicitAlias;
        }

        if (explicitAliasKeywordSeen) {
            return null;
        }

        return isValidTopLevelAlias(sqlFragment, implicitAlias) ? implicitAlias : null;
    }

    /**
     * Returns the end index (exclusive) of the alias token starting at {@code aliasStart}, honoring quoted
     * and bracket-quoted aliases, or {@code -1} when no valid alias token starts there.
     */
    private static int findAliasTokenEnd(final String sqlFragment, final int aliasStart) {
        final int len = sqlFragment.length();

        if (aliasStart < 0 || aliasStart >= len) {
            return -1;
        }

        final char first = sqlFragment.charAt(aliasStart);

        if (first == SK._SINGLE_QUOTE || first == SK._DOUBLE_QUOTE || first == SK._BACKTICK || first == '[') {
            return skipSqlQuotedOrComment(sqlFragment, aliasStart);
        }

        if (!isAliasIdentifierChar(first)) {
            return -1;
        }

        int end = aliasStart + 1;

        while (end < len && isAliasIdentifierChar(sqlFragment.charAt(end))) {
            end++;
        }

        return end;
    }

    /**
     * Reports whether {@code alias} spans the final token of {@code sqlFragment} (only trivia may follow it)
     * and its expression does not end with a qualification dot.
     */
    private static boolean isValidTopLevelAlias(final String sqlFragment, final TopLevelAlias alias) {
        if (alias == null || alias.aliasEnd() <= alias.aliasStart() || skipAliasTrivia(sqlFragment, alias.aliasEnd()) != sqlFragment.length()) {
            return false;
        }

        int expressionEnd = alias.expressionEnd() - 1;

        while (expressionEnd >= 0 && Character.isWhitespace(sqlFragment.charAt(expressionEnd))) {
            expressionEnd--;
        }

        return expressionEnd >= 0 && sqlFragment.charAt(expressionEnd) != SK._PERIOD;
    }

    /** Skips whitespace and SQL comments, returning {@code -1} for an unterminated block comment. */
    private static int skipAliasTrivia(final String sqlFragment, int index) {
        final int len = sqlFragment.length();

        while (index < len) {
            final char ch = sqlFragment.charAt(index);

            if (Character.isWhitespace(ch)) {
                index++;
            } else if (ch == '-' && index < len - 1 && sqlFragment.charAt(index + 1) == '-') {
                index += 2;

                while (index < len && sqlFragment.charAt(index) != '\n' && sqlFragment.charAt(index) != '\r') {
                    index++;
                }
            } else if (ch == '#' && isAliasScannerHashCommentStart(sqlFragment, index)) {
                while (++index < len && sqlFragment.charAt(index) != '\n' && sqlFragment.charAt(index) != '\r') {
                    // Skip MySQL hash comment.
                }
            } else if (ch == '/' && index < len - 1 && sqlFragment.charAt(index + 1) == '*') {
                final int commentEnd = sqlFragment.indexOf("*/", index + 2);

                if (commentEnd < 0) {
                    return -1;
                }

                index = commentEnd + 2;
            } else {
                break;
            }
        }

        return index;
    }

    /** Reports whether {@code ch} can be part of a bare alias identifier (letter, digit, {@code _}, {@code $}, {@code #}, or {@code @}). */
    private static boolean isAliasIdentifierChar(final char ch) {
        return ch == '_' || ch == '$' || ch == '#' || ch == '@' || Character.isLetterOrDigit(ch);
    }

    /**
     * Reports whether {@code index} is at a keyword boundary: out of range, or not inside an identifier
     * or qualified name (the character is not a letter, digit, dot, {@code _}, or {@code $}).
     */
    private static boolean isAliasKeywordBoundary(final String sqlFragment, final int index) {
        if (index < 0 || index >= sqlFragment.length()) {
            return true;
        }

        final char ch = sqlFragment.charAt(index);
        return ch != '.' && ch != '_' && ch != '$' && !Character.isLetterOrDigit(ch);
    }

    /**
     * Reports whether the {@code '#'} at {@code index} starts a MySQL hash comment rather than being a data
     * token (a <code>#{...}</code> marker, a {@code #>}/{@code ##}/{@code #-} operator, the second character
     * of a {@code ?#} operator, or part of a SQL Server {@code #name}/{@code ##name} temporary-table identifier).
     */
    private static boolean isAliasScannerHashCommentStart(final String sqlFragment, final int index) {
        if (index < sqlFragment.length() - 1) {
            final char next = sqlFragment.charAt(index + 1);

            if (next == '{' || next == '>' || next == '-' || next == '#' || (index > 0 && sqlFragment.charAt(index - 1) == '?')) {
                return false;
            }

            // The second '#' in a SQL Server global temporary-table identifier (##name) is part
            // of the identifier, not the start of a MySQL hash comment. The first '#' is already
            // excluded by the next == '#' branch above; explicitly excluding the second keeps the
            // alias scanner running so a trailing alias in "##stage s" is still discovered.
            if (index > 0 && sqlFragment.charAt(index - 1) == '#' && (index == 1 || !isAliasIdentifierChar(sqlFragment.charAt(index - 2)))
                    && (next == '_' || next == '$' || Character.isLetterOrDigit(next))) {
                return false;
            }

            if ((index == 0 || sqlFragment.charAt(index - 1) == '.') && (next == '_' || next == '$' || Character.isLetterOrDigit(next))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compact scan result shared by FROM-table and SELECT-expression alias parsing.
     *
     * @param expressionEnd the end index (exclusive) of the expression preceding the alias
     * @param aliasStart the start index of the alias token
     * @param aliasEnd the end index (exclusive) of the alias token
     */
    private record TopLevelAlias(int expressionEnd, int aliasStart, int aliasEnd) {
    }

    /**
     * Adds a JOIN clause to the SQL statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o ON u.id = o.user_id")
     *                 .build().query();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param joinExpr the full join expression, including the {@code ON} clause if present, e.g. {@code "orders o ON u.id = o.user_id"} (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This join(final String joinExpr) {
        return appendJoinExpr(_SPACE_JOIN_SPACE, joinExpr, true);
    }

    /**
     * Shared implementation for the string {@code *Join(String)} overloads: validates the join
     * expression, appends the join keyword and the expression verbatim, and records whether a
     * follow-up {@code on(...)}/{@code using(...)} connector is still allowed.
     *
     * @param joinKeyword the leading join keyword token (e.g. {@link #_SPACE_LEFT_JOIN_SPACE})
     * @param joinExpr the full join expression, including the {@code ON} clause if present
     * @param joinConditionAllowed whether this join type accepts an {@code ON}/{@code USING} connector
     *        at all ({@code false} for {@code CROSS JOIN} and {@code NATURAL JOIN})
     * @return this builder instance for method chaining
     */
    @SuppressWarnings("unchecked")
    private This appendJoinExpr(final char[] joinKeyword, final String joinExpr, final boolean joinConditionAllowed) {
        checkCanAppendJoin();
        checkSqlFragmentNotBlank(joinExpr, "joinExpr");

        if (!joinConditionAllowed && containsTopLevelJoinCondition(joinExpr)) {
            throw new IllegalArgumentException("CROSS JOIN and NATURAL JOIN expressions must not contain a top-level ON or USING connector");
        }

        _sb.append(joinKeyword);

        _sb.append(joinExpr);
        _joinConditionAllowed = joinConditionAllowed && !containsTopLevelJoinCondition(joinExpr);

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
     *                 .build().query();
     * // Output: SELECT * FROM users JOIN orders ON users.id = orders.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     */
    public This join(final Class<?> entityClass) {
        return join(entityClass, QueryUtil.tableAlias(entityClass));
    }

    /**
     * Shared implementation for the entity-class {@code *Join(Class, alias)} overloads: registers the
     * alias-to-column mapping (when an alias is given), appends the join keyword, then appends the
     * table name (optionally followed by the alias).
     *
     * @param joinKeyword the leading join keyword token (e.g. {@link #_SPACE_LEFT_JOIN_SPACE})
     * @param entityClass the entity class to join
     * @param alias the table alias; may be {@code null} or empty
     * @return this builder instance for method chaining
     */
    @SuppressWarnings("unchecked")
    private This appendJoin(final char[] joinKeyword, final Class<?> entityClass, final String alias) {
        checkCanAppendJoin();
        N.checkArgNotNull(entityClass, cs.entityClass);

        // Resolve the table name (which rejects a non-bean class) before any builder state is changed.
        final String tableName = getTableName(entityClass, _namingPolicy);

        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(joinKeyword);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(tableName).append(" ").append(alias);
        } else {
            _sb.append(tableName);
        }

        _joinConditionAllowed = joinKeyword != _SPACE_CROSS_JOIN_SPACE && joinKeyword != _SPACE_NATURAL_JOIN_SPACE;

        return (This) this;
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
     *                 .build().query();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * <p>The joined entity's property mapping is registered for {@code alias} only; it never becomes this
     * builder's primary mapping. An <i>unqualified</i> property name in a later clause is therefore resolved
     * against the FROM table, and when the FROM table was given as a plain string (which carries no entity
     * mapping) such a name is emitted verbatim and unqualified - ambiguous if both tables define it. Prefer
     * {@link #from(Class, String)} over {@code from("table alias")} so unqualified names resolve, or qualify
     * them explicitly ({@code "alias.propName"}).</p>
     *
     * @param entityClass the entity class to join (must not be {@code null})
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    public This join(final Class<?> entityClass, final String alias) {
        return appendJoin(_SPACE_JOIN_SPACE, entityClass, alias);
    }

    /**
     * Adds an INNER JOIN clause to the SQL statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .innerJoin("orders o ON u.id = o.user_id")
     *                 .build().query();
     * // Output: SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param joinExpr the full join expression, including the {@code ON} clause if present (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This innerJoin(final String joinExpr) {
        return appendJoinExpr(_SPACE_INNER_JOIN_SPACE, joinExpr, true);
    }

    /**
     * Adds an INNER JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).innerJoin(Order.class).on("users.id = orders.user_id").build().query();
     * // Output: SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     */
    public This innerJoin(final Class<?> entityClass) {
        return innerJoin(entityClass, QueryUtil.tableAlias(entityClass));
    }

    /**
     * Adds an INNER JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").innerJoin(Order.class, "o").on("u.id = o.user_id").build().query();
     * // Output: SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    public This innerJoin(final Class<?> entityClass, final String alias) {
        return appendJoin(_SPACE_INNER_JOIN_SPACE, entityClass, alias);
    }

    /**
     * Adds a LEFT JOIN clause to the SQL statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .leftJoin("orders o ON u.id = o.user_id")
     *                 .build().query();
     * // Output: SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param joinExpr the full join expression, including the {@code ON} clause if present (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This leftJoin(final String joinExpr) {
        return appendJoinExpr(_SPACE_LEFT_JOIN_SPACE, joinExpr, true);
    }

    /**
     * Adds a LEFT JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).leftJoin(Order.class).on("users.id = orders.user_id").build().query();
     * // Output: SELECT * FROM users LEFT JOIN orders ON users.id = orders.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     */
    public This leftJoin(final Class<?> entityClass) {
        return leftJoin(entityClass, QueryUtil.tableAlias(entityClass));
    }

    /**
     * Adds a LEFT JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").leftJoin(Order.class, "o").on("u.id = o.user_id").build().query();
     * // Output: SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    public This leftJoin(final Class<?> entityClass, final String alias) {
        return appendJoin(_SPACE_LEFT_JOIN_SPACE, entityClass, alias);
    }

    /**
     * Adds a RIGHT JOIN clause to the SQL statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .rightJoin("orders o ON u.id = o.user_id")
     *                 .build().query();
     * // Output: SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param joinExpr the full join expression, including the {@code ON} clause if present (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This rightJoin(final String joinExpr) {
        return appendJoinExpr(_SPACE_RIGHT_JOIN_SPACE, joinExpr, true);
    }

    /**
     * Adds a RIGHT JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).rightJoin(Order.class).on("users.id = orders.user_id").build().query();
     * // Output: SELECT * FROM users RIGHT JOIN orders ON users.id = orders.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     */
    public This rightJoin(final Class<?> entityClass) {
        return rightJoin(entityClass, QueryUtil.tableAlias(entityClass));
    }

    /**
     * Adds a RIGHT JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").rightJoin(Order.class, "o").on("u.id = o.user_id").build().query();
     * // Output: SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    public This rightJoin(final Class<?> entityClass, final String alias) {
        return appendJoin(_SPACE_RIGHT_JOIN_SPACE, entityClass, alias);
    }

    /**
     * Adds a FULL JOIN clause to the SQL statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .fullJoin("orders o ON u.id = o.user_id")
     *                 .build().query();
     * // Output: SELECT * FROM users u FULL JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param joinExpr the full join expression, including the {@code ON} clause if present (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This fullJoin(final String joinExpr) {
        return appendJoinExpr(_SPACE_FULL_JOIN_SPACE, joinExpr, true);
    }

    /**
     * Adds a FULL JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).fullJoin(Order.class).on("users.id = orders.user_id").build().query();
     * // Output: SELECT * FROM users FULL JOIN orders ON users.id = orders.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     */
    public This fullJoin(final Class<?> entityClass) {
        return fullJoin(entityClass, QueryUtil.tableAlias(entityClass));
    }

    /**
     * Adds a FULL JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").fullJoin(Order.class, "o").on("u.id = o.user_id").build().query();
     * // Output: SELECT * FROM users u FULL JOIN orders o ON u.id = o.user_id
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    public This fullJoin(final Class<?> entityClass, final String alias) {
        return appendJoin(_SPACE_FULL_JOIN_SPACE, entityClass, alias);
    }

    /**
     * Adds a CROSS JOIN clause to the SQL statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .crossJoin("orders")
     *                 .build().query();
     * // Output: SELECT * FROM users CROSS JOIN orders
     * }</pre>
     *
     * @param joinExpr the join expression (a table reference, optionally with alias; a {@code CROSS JOIN} takes no {@code ON} clause) (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank, or contains a top-level
     *                                  {@code ON}/{@code USING} connector
     */
    public This crossJoin(final String joinExpr) {
        return appendJoinExpr(_SPACE_CROSS_JOIN_SPACE, joinExpr, false);
    }

    /**
     * Adds a CROSS JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).crossJoin(Order.class).build().query();
     * // Output: SELECT * FROM users CROSS JOIN orders
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     */
    public This crossJoin(final Class<?> entityClass) {
        return crossJoin(entityClass, QueryUtil.tableAlias(entityClass));
    }

    /**
     * Adds a CROSS JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").crossJoin(Order.class, "o").build().query();
     * // Output: SELECT * FROM users u CROSS JOIN orders o
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    public This crossJoin(final Class<?> entityClass, final String alias) {
        return appendJoin(_SPACE_CROSS_JOIN_SPACE, entityClass, alias);
    }

    /**
     * Adds a NATURAL JOIN clause to the SQL statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .naturalJoin("orders")
     *                 .build().query();
     * // Output: SELECT * FROM users NATURAL JOIN orders
     * }</pre>
     *
     * @param joinExpr the join expression (a table reference, optionally with alias; a {@code NATURAL JOIN} takes no {@code ON} clause) (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank, or contains a top-level
     *                                  {@code ON}/{@code USING} connector
     */
    public This naturalJoin(final String joinExpr) {
        return appendJoinExpr(_SPACE_NATURAL_JOIN_SPACE, joinExpr, false);
    }

    /**
     * Adds a NATURAL JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).naturalJoin(Order.class).build().query();
     * // Output: SELECT * FROM users NATURAL JOIN orders
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     */
    public This naturalJoin(final Class<?> entityClass) {
        return naturalJoin(entityClass, QueryUtil.tableAlias(entityClass));
    }

    /**
     * Adds a NATURAL JOIN clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").naturalJoin(Order.class, "o").build().query();
     * // Output: SELECT * FROM users u NATURAL JOIN orders o
     * // (assumes @Table(name = "users") / @Table(name = "orders"); otherwise the table
     * // names are derived from the class names)
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if the current SELECT segment has no {@code FROM} clause yet, a later SQL clause or a completed
     *         set-operation operand has already been emitted, or the preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}
     * @throws IllegalArgumentException if {@code entityClass} is {@code null} or is not a valid entity bean class
     */
    public This naturalJoin(final Class<?> entityClass, final String alias) {
        return appendJoin(_SPACE_NATURAL_JOIN_SPACE, entityClass, alias);
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
     *                 .build().query();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param expr the join condition expression (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or, on a SQL Server dialect, contains a
     *                                  temporary-table identifier ({@code #name}) together with a SQL comment token
     * @throws IllegalStateException if this builder is closed, if there is no immediately preceding JOIN that accepts an
     *         {@code ON}/{@code USING} connector, or if a completed set-operation operand or a later clause (WHERE,
     *         GROUP BY, HAVING, ORDER BY, pagination, FOR UPDATE) has already been emitted
     */
    public This on(final String expr) {
        checkSqlFragmentNotBlank(expr, "expr");

        return mutateAtomically(() -> {
            checkCanAppendJoinCondition();

            _sb.append(_SPACE_ON_SPACE);

            appendStringExpr(expr, false);
            _joinConditionAllowed = false;
        });
    }

    /**
     * Adds an ON clause for a composite join condition, joining the given expressions with {@code AND}.
     *
     * <p>This is a convenience for multi-column ON conditions. When there are multiple elements,
     * each expression is enclosed in parentheses before the fragments are combined with {@code AND},
     * preserving the precedence of any {@code OR} within an expression. A single element is rendered
     * exactly as by {@link #on(String)}.</p>
     *
     * <p><b>Note:</b> unlike {@link Filters#on(String, String)} — where two strings mean an equality
     * {@code ON left = right} — each argument here is a <em>complete</em> boolean expression and multiple
     * arguments are joined with {@code AND}: {@code on("u.id = o.user_id", "o.active = 1")}. Calling
     * {@code on("u.id", "o.user_id")} renders {@code ON (u.id) AND (o.user_id)}, not an equality comparison.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o")
     *                 .on("u.id = o.user_id", "u.tenant_id = o.tenant_id")
     *                 .build().query();
     * // Output: SELECT * FROM users u JOIN orders o ON (u.id = o.user_id) AND (u.tenant_id = o.tenant_id)
     * }</pre>
     *
     * @param exprs the join condition expressions (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code exprs} is {@code null} or empty, or contains a {@code null}, empty, or blank element,
     *                                  or, on a SQL Server dialect, an element that contains a temporary-table identifier
     *                                  ({@code #name}) together with a SQL comment token
     * @throws IllegalStateException if this builder is closed, if there is no immediately preceding JOIN that accepts an
     *         {@code ON}/{@code USING} connector, or if a completed set-operation operand or a later clause (WHERE,
     *         GROUP BY, HAVING, ORDER BY, pagination, FOR UPDATE) has already been emitted
     */
    public This on(final String... exprs) {
        checkSqlFragmentsNotBlank(exprs, "exprs");

        return mutateAtomically(() -> {
            checkCanAppendJoinCondition();

            _sb.append(_SPACE_ON_SPACE);

            for (int i = 0, len = exprs.length; i < len; i++) {
                if (i > 0) {
                    _sb.append(_SPACE_AND_SPACE);
                }

                if (len > 1) {
                    _sb.append(SK._PARENTHESIS_L);
                }

                appendStringExpr(exprs[i], false);

                if (len > 1) {
                    _sb.append(SK._PARENTHESIS_R);
                }
            }

            _joinConditionAllowed = false;
        });
    }

    /**
     * Adds an ON clause with a condition object for join conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o")
     *                 .on(Filters.expr("u.id = o.user_id"))
     *                 .build().query();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param condition the join condition (must not be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code condition} is {@code null}, or if a condition other than an explicit
     *                                  {@link On}/{@link Using} has a {@code null} operator or is/contains a {@link Criteria},
     *                                  standalone {@link SubQuery}, SQL clause, JOIN, {@code ON}/{@code USING} connector,
     *                                  quantified-subquery operand, or blank {@link SqlExpression}. An empty
     *                                  {@link com.landawn.abacus.query.condition.Junction} is accepted and renders its Boolean identity (for example {@code ON 1 = 1}).
     *                                  A {@link Using} connector is rendered like {@link #using(String)}, so it is also rejected when a
     *                                  column maps to a qualified name, which {@code USING (...)} does not accept
     * @throws IllegalStateException if this builder is closed, if there is no immediately preceding JOIN that accepts an
     *         {@code ON}/{@code USING} connector, or if a completed set-operation operand or a later clause (WHERE,
     *         GROUP BY, HAVING, ORDER BY, pagination, FOR UPDATE) has already been emitted
     */
    public This on(final Condition condition) {
        N.checkArgNotNull(condition, cs.condition);

        final boolean explicitJoinCondition = condition instanceof On || condition instanceof Using;

        if (!explicitJoinCondition) {
            validatePredicateCondition(condition);
        }

        return mutateAtomically(() -> {
            checkCanAppendJoinCondition();

            // Mirror Join.toString() and the append(Criteria) join path: a raw predicate needs an explicit
            // ON keyword, while an On/Using condition renders its own keyword. Emitting ON unconditionally
            // would produce "ON ON (...)" or the impossible "ON USING (...)".
            if (!explicitJoinCondition) {
                _sb.append(_SPACE_ON_SPACE);
            }

            appendCondition(condition);
            _joinConditionAllowed = false;
        });
    }

    /**
     * Adds a USING clause for join conditions.
     * Entity property names are mapped to column names without adding the current table alias,
     * because SQL requires unqualified names inside {@code USING (...)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .join("orders")
     *                 .using("user_id")
     *                 .build().query();
     * // Output: SELECT * FROM users JOIN orders USING (user_id)
     * }</pre>
     *
     * @param expr the property or column name(s) for the USING clause (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, contains a SQL comment token,
     *                                  or renders a qualified column name (a dot outside a quoted identifier), which
     *                                  {@code USING (...)} does not accept
     * @throws IllegalStateException if this builder is closed, if there is no immediately preceding JOIN that accepts an
     *         {@code ON}/{@code USING} connector, or if a completed set-operation operand or a later clause (WHERE,
     *         GROUP BY, HAVING, ORDER BY, pagination, FOR UPDATE) has already been emitted
     */
    public This using(final String expr) {
        checkSqlFragmentNotBlank(expr, "expr");

        if (containsSqlCommentToken(expr)) {
            throw new IllegalArgumentException("SQL comment token is not allowed in column expression: " + expr);
        }

        final String trimmedExpr = expr.trim();

        return mutateAtomically(() -> {
            checkCanAppendJoinCondition();

            appendUsingClause(() -> {
                if (trimmedExpr.startsWith(SK.PARENTHESIS_L) && trimmedExpr.endsWith(SK.PARENTHESIS_R)) {
                    appendStringExpr(trimmedExpr, false);
                } else {
                    _sb.append(SK._PARENTHESIS_L);
                    appendColumnName(trimmedExpr);
                    _sb.append(SK._PARENTHESIS_R);
                }
            });

            _joinConditionAllowed = false;
        });
    }

    /**
     * Adds a USING clause with multiple columns for join conditions.
     * Entity mappings are applied without automatically qualifying columns with the current table alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("orders")
     *                 .join("order_items")
     *                 .using("order_id", "tenant_id")
     *                 .build().query();
     * // Output: SELECT * FROM orders JOIN order_items USING (order_id, tenant_id)
     * }</pre>
     *
     * @param propOrColumnNames the property or column names for the USING clause (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null},
     *                                  empty, or blank element or an element containing a SQL comment token, or renders
     *                                  a qualified column name (a dot outside a quoted identifier), which
     *                                  {@code USING (...)} does not accept
     * @throws IllegalStateException if this builder is closed, if there is no immediately preceding JOIN that accepts an
     *         {@code ON}/{@code USING} connector, or if a completed set-operation operand or a later clause (WHERE,
     *         GROUP BY, HAVING, ORDER BY, pagination, FOR UPDATE) has already been emitted
     */
    public This using(final String... propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

        return using(Array.asList(propOrColumnNames));
    }

    /**
     * Adds a USING clause with a collection of columns for join conditions.
     * Entity mappings are applied without automatically qualifying columns with the current table alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("order_id", "tenant_id");
     * String sql = PSC.select("*")
     *                 .from("orders")
     *                 .join("order_items")
     *                 .using(columns)
     *                 .build().query();
     * // Output: SELECT * FROM orders JOIN order_items USING (order_id, tenant_id)
     * }</pre>
     *
     * @param propOrColumnNames the collection of property or column names for the USING clause (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null},
     *                                  empty, or blank element or an element containing a SQL comment token, or renders
     *                                  a qualified column name (a dot outside a quoted identifier), which
     *                                  {@code USING (...)} does not accept
     * @throws IllegalStateException if this builder is closed, if there is no immediately preceding JOIN that accepts an
     *         {@code ON}/{@code USING} connector, or if a completed set-operation operand or a later clause (WHERE,
     *         GROUP BY, HAVING, ORDER BY, pagination, FOR UPDATE) has already been emitted
     */
    public This using(final Collection<String> propOrColumnNames) {
        final List<String> propOrColumnNamesSnapshot = copyAndValidateSqlFragments(propOrColumnNames, "propOrColumnNames");

        return mutateAtomically(() -> {
            checkCanAppendJoinCondition();

            appendUsingClause(() -> {
                _sb.append(SK._PARENTHESIS_L);

                int i = 0;
                for (final String propOrColumnName : propOrColumnNamesSnapshot) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(propOrColumnName);
                }

                _sb.append(SK._PARENTHESIS_R);
            });
            _joinConditionAllowed = false;
        });
    }

    /**
     * Emits a USING clause without automatically qualifying its column names, and rejects a rendered
     * column list that is qualified anyway. The current entity mapping is retained, and the table alias
     * is restored even if column rendering fails.
     *
     * @param appendColumns renders the parenthesized column list (must not be {@code null})
     * @throws IllegalArgumentException if {@code appendColumns} is {@code null}, or if a rendered column name carries a table
     *         or schema qualifier
     */
    protected void appendUsingClause(final Runnable appendColumns) {
        N.checkArgNotNull(appendColumns, cs.appendColumns);

        final String tableAlias = _tableAlias;
        _tableAlias = null;

        try {
            _sb.append(_SPACE_USING_SPACE);

            final int columnListStartIdx = _sb.length();

            appendColumns.run();

            checkUsingColumnsAreUnqualified(columnListStartIdx);
        } finally {
            _tableAlias = tableAlias;
        }
    }

    /**
     * Rejects a rendered USING column list that still carries a qualifier. Clearing the table alias only
     * stops this builder from adding one: a property mapped to an already-qualified column (for example
     * {@code @Column("p.display_name")}) and an explicitly qualified raw expression both still render a
     * qualifier, which SQL does not accept inside {@code USING (...)}. A dot inside a quoted identifier
     * ({@code USING ("weird.col")}) is part of the name and is allowed.
     *
     * @param columnListStartIdx the buffer index at which the parenthesized column list begins
     * @throws IllegalArgumentException if a rendered column name carries a table or schema qualifier
     */
    private void checkUsingColumnsAreUnqualified(final int columnListStartIdx) {
        final String columnList = _sb.substring(columnListStartIdx);

        if (QueryUtil.indexOfQualifyingDot(columnList) >= 0) {
            throw new IllegalArgumentException("USING column names must be unqualified, but rendered: " + columnList.trim()
                    + ". Pass the unqualified column name, or use on(...) when the join columns are qualified");
        }
    }

    /**
     * Adds a WHERE clause with a string expression.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where("age > 18")
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE age > 18
     * }</pre>
     *
     * @param expr the WHERE condition expression (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or, on a SQL Server dialect, contains a
     *                                  temporary-table identifier ({@code #name}) together with a SQL comment token
     * @throws IllegalStateException if this builder is closed, if {@code WHERE} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet or a completed set-operation operand has already been emitted, or if a later clause
     *         (GROUP BY, HAVING, ORDER BY, pagination, FOR UPDATE) has already been emitted
     */
    public This where(final String expr) {
        checkSqlFragmentNotBlank(expr, "expr");

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.WHERE);

            _sb.append(_SPACE_WHERE_SPACE);

            appendStringExpr(expr, false);
        });
    }

    /**
     * Adds a WHERE clause with a condition object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where(Filters.greaterThan("age", 18))
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE age > ?
     * }</pre>
     *
     * @param condition the WHERE condition (must not be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code condition} is {@code null}, has a {@code null} operator, or is/contains a
     *                                  {@link Criteria}, standalone {@link SubQuery}, SQL clause, JOIN, {@code ON}/{@code USING}
     *                                  connector, quantified-subquery operand, or blank {@link SqlExpression}. An empty
     *                                  {@link com.landawn.abacus.query.condition.Junction} is accepted and renders its Boolean identity (for example {@code WHERE 1 = 1})
     * @throws IllegalStateException if this builder is closed, if {@code WHERE} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet or a completed set-operation operand has already been emitted, or if a later clause
     *         (GROUP BY, HAVING, ORDER BY, pagination, FOR UPDATE) has already been emitted
     * @see Filters
     */
    public This where(final Condition condition) {
        final Condition predicate = validatePredicateCondition(condition);

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.WHERE);

            _sb.append(_SPACE_WHERE_SPACE);

            appendCondition(predicate);
        });
    }

    /**
     * Adds a GROUP BY ASC clause with a single column.
     * Convenience method equivalent to {@code groupBy(propOrColumnName, SortDirection.ASC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*)")
     *                 .from("products")
     *                 .groupByAsc("category")
     *                 .build().query();
     * // Output: SELECT category, COUNT(*) FROM products GROUP BY category ASC
     * }</pre>
     *
     * @param propOrColumnName the property or column name to group by ascending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank, or contains a SQL comment token
     */
    @Beta
    public This groupByAsc(final String propOrColumnName) {
        return groupBy(propOrColumnName, SortDirection.ASC);
    }

    /**
     * Adds a GROUP BY ASC clause with multiple columns.
     * Convenience method equivalent to {@code groupBy(propOrColumnNames, SortDirection.ASC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupByAsc("category", "brand")
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC
     * }</pre>
     *
     * @param propOrColumnNames the columns to group by ascending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     */
    @Beta
    public This groupByAsc(final String... propOrColumnNames) {
        return groupBy(N.toList(propOrColumnNames), SortDirection.ASC);
    }

    /**
     * Adds a GROUP BY ASC clause with a collection of columns.
     * Convenience method equivalent to {@code groupBy(propOrColumnNames, SortDirection.ASC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("category", "brand");
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupByAsc(columns)
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to group by ascending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     */
    @Beta
    public This groupByAsc(final Collection<String> propOrColumnNames) {
        return groupBy(propOrColumnNames, SortDirection.ASC);
    }

    /**
     * Adds a GROUP BY DESC clause with a single column.
     * Convenience method equivalent to {@code groupBy(propOrColumnName, SortDirection.DESC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*)")
     *                 .from("products")
     *                 .groupByDesc("category")
     *                 .build().query();
     * // Output: SELECT category, COUNT(*) FROM products GROUP BY category DESC
     * }</pre>
     *
     * @param propOrColumnName the property or column name to group by descending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank, or contains a SQL comment token
     */
    @Beta
    public This groupByDesc(final String propOrColumnName) {
        return groupBy(propOrColumnName, SortDirection.DESC);
    }

    /**
     * Adds a GROUP BY DESC clause with multiple columns.
     * Convenience method equivalent to {@code groupBy(propOrColumnNames, SortDirection.DESC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupByDesc("category", "brand")
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC
     * }</pre>
     *
     * @param propOrColumnNames the columns to group by descending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     */
    @Beta
    public This groupByDesc(final String... propOrColumnNames) {
        return groupBy(N.toList(propOrColumnNames), SortDirection.DESC);
    }

    /**
     * Adds a GROUP BY DESC clause with a collection of columns.
     * Convenience method equivalent to {@code groupBy(propOrColumnNames, SortDirection.DESC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("category", "brand");
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupByDesc(columns)
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to group by descending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     */
    @Beta
    public This groupByDesc(final Collection<String> propOrColumnNames) {
        return groupBy(propOrColumnNames, SortDirection.DESC);
    }

    /**
     * Adds a GROUP BY clause with a single column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy("category")
     *                 .build().query();
     * // Output: SELECT category, COUNT(*) FROM products GROUP BY category
     * }</pre>
     *
     * @param propOrColumnName the property or column name to group by (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank, or contains a SQL comment token
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     */
    public This groupBy(final String propOrColumnName) {
        checkSqlFragmentNotBlank(propOrColumnName, "propOrColumnName");

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.GROUP_BY);

            _sb.append(_SPACE_GROUP_BY_SPACE);

            appendColumnName(propOrColumnName);
        });
    }

    /**
     * Adds a GROUP BY clause with multiple columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "brand", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy("category", "brand")
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand
     * }</pre>
     *
     * @param propOrColumnNames the columns to group by (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     */
    public This groupBy(final String... propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.GROUP_BY);

            _sb.append(_SPACE_GROUP_BY_SPACE);

            for (int i = 0, len = propOrColumnNames.length; i < len; i++) {
                if (i > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(propOrColumnNames[i]);
            }
        });
    }

    /**
     * Adds a GROUP BY clause with a single column and sort direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("category", "COUNT(*)")
     *                 .from("products")
     *                 .groupBy("category", SortDirection.DESC)
     *                 .build().query();
     * // Output: SELECT category, COUNT(*) FROM products GROUP BY category DESC
     * }</pre>
     *
     * @param expr the column or expression to group by
     * @param direction the sort direction
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, if {@code expr} contains a SQL
     *         comment token, or if {@code direction} is {@code null}
     */
    public This groupBy(final String expr, final SortDirection direction) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in limit(int)
        checkSqlFragmentNotBlank(expr, "expr"); // report this overload's parameter name, not the delegate's
        N.checkArgNotNull(direction, cs.direction);

        groupBy(expr);

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
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to group by
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     */
    public This groupBy(final Collection<String> propOrColumnNames) {
        final List<String> propOrColumnNamesSnapshot = copyAndValidateSqlFragments(propOrColumnNames, "propOrColumnNames");

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.GROUP_BY);

            _sb.append(_SPACE_GROUP_BY_SPACE);

            int i = 0;
            for (final String columnName : propOrColumnNamesSnapshot) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);
            }
        });
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
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to group by
     * @param direction the direction appended after each column in the GROUP BY clause
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element or an element containing a SQL comment token, or if {@code direction} is {@code null}
     */
    public This groupBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in limit(int)
        final List<String> propOrColumnNamesSnapshot = copyAndValidateSqlFragments(propOrColumnNames, "propOrColumnNames");
        N.checkArgNotNull(direction, cs.direction);

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.GROUP_BY);

            _sb.append(_SPACE_GROUP_BY_SPACE);

            int i = 0;
            for (final String columnName : propOrColumnNamesSnapshot) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);
                _sb.append(_SPACE);
                _sb.append(direction.toString());
            }
        });
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
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand DESC
     * }</pre>
     *
     * <p><b>Note:</b> The order of columns in the generated {@code GROUP BY} clause follows the map's
     * iteration order. Pass a {@link java.util.LinkedHashMap} (or other insertion-ordered {@code Map})
     * to guarantee deterministic clause order.</p>
     *
     * @param groupings map of columns to their sort directions
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code groupings} is {@code null} or empty, contains a {@code null}, empty, or blank key,
     *         a key containing a SQL comment token, or maps any key to a {@code null} direction
     * @throws IllegalStateException if this builder is closed, if {@code GROUP BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (HAVING, ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     */
    public This groupBy(final Map<String, SortDirection> groupings) {
        final Map<String, SortDirection> groupingsSnapshot = copyAndValidateSqlFragmentMap(groupings, "groupings");

        for (final Map.Entry<String, SortDirection> entry : groupingsSnapshot.entrySet()) {
            N.checkArgNotNull(entry.getValue(), "Value in groupings");
        }

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.GROUP_BY);

            _sb.append(_SPACE_GROUP_BY_SPACE);

            int i = 0;
            for (final Map.Entry<String, SortDirection> entry : groupingsSnapshot.entrySet()) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(entry.getKey());

                _sb.append(_SPACE);
                _sb.append(entry.getValue().toString());
            }
        });
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
     *                 .build().query();
     * // Output: SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > 10
     * }</pre>
     *
     * @param expr the HAVING condition expression (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or, on a SQL Server dialect, contains a
     *                                  temporary-table identifier ({@code #name}) together with a SQL comment token
     * @throws IllegalStateException if this builder is closed, if {@code HAVING} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     */
    public This having(final String expr) {
        checkSqlFragmentNotBlank(expr, "expr");

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.HAVING);

            _sb.append(_SPACE_HAVING_SPACE);

            appendStringExpr(expr, false);
        });
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
     *                 .build().query();
     * // Output: SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > ?
     * }</pre>
     *
     * @param condition the HAVING condition (must not be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code condition} is {@code null}, has a {@code null} operator, or is/contains a
     *                                  {@link Criteria}, standalone {@link SubQuery}, SQL clause, JOIN, {@code ON}/{@code USING}
     *                                  connector, quantified-subquery operand, or blank {@link SqlExpression}. An empty
     *                                  {@link com.landawn.abacus.query.condition.Junction} is accepted and renders its Boolean identity (for example {@code HAVING 1 = 1})
     * @throws IllegalStateException if this builder is closed, if {@code HAVING} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES, UPDATE, or DELETE statement, if the current SELECT segment has no {@code FROM} clause yet or a
     *         completed set-operation operand has already been emitted, or if a later clause (ORDER BY, pagination,
     *         FOR UPDATE) has already been emitted
     * @see Filters
     */
    public This having(final Condition condition) {
        final Condition predicate = validatePredicateCondition(condition);

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.HAVING);

            _sb.append(_SPACE_HAVING_SPACE);

            appendCondition(predicate);
        });
    }

    /**
     * Adds an ORDER BY ASC clause with a single column.
     * Convenience method equivalent to {@code orderBy(propOrColumnName, SortDirection.ASC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByAsc("name")
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY name ASC
     * }</pre>
     *
     * @param propOrColumnName the property or column name to order by ascending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank, or contains a SQL comment token
     */
    @Beta
    public This orderByAsc(final String propOrColumnName) {
        return orderBy(propOrColumnName, SortDirection.ASC);
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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY last_name ASC, first_name ASC
     * }</pre>
     *
     * @param propOrColumnNames the columns to order by ascending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     */
    @Beta
    public This orderByAsc(final String... propOrColumnNames) {
        return orderBy(N.toList(propOrColumnNames), SortDirection.ASC);
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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY last_name ASC, first_name ASC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to order by ascending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     */
    @Beta
    public This orderByAsc(final Collection<String> propOrColumnNames) {
        return orderBy(propOrColumnNames, SortDirection.ASC);
    }

    /**
     * Adds an ORDER BY DESC clause with a single column.
     * Convenience method equivalent to {@code orderBy(propOrColumnName, SortDirection.DESC)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderByDesc("createdDate")
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY created_date DESC
     * }</pre>
     *
     * @param propOrColumnName the property or column name to order by descending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank, or contains a SQL comment token
     */
    @Beta
    public This orderByDesc(final String propOrColumnName) {
        return orderBy(propOrColumnName, SortDirection.DESC);
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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY created_date DESC, id DESC
     * }</pre>
     *
     * @param propOrColumnNames the columns to order by descending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     */
    @Beta
    public This orderByDesc(final String... propOrColumnNames) {
        return orderBy(N.toList(propOrColumnNames), SortDirection.DESC);
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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY created_date DESC, id DESC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to order by descending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     */
    @Beta
    public This orderByDesc(final Collection<String> propOrColumnNames) {
        return orderBy(propOrColumnNames, SortDirection.DESC);
    }

    /**
     * Adds an ORDER BY clause with a single column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("name")
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY name
     * }</pre>
     *
     * @param propOrColumnName the property or column name to order by (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank, or contains a SQL comment token
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     */
    public This orderBy(final String propOrColumnName) {
        checkSqlFragmentNotBlank(propOrColumnName, "propOrColumnName");

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.ORDER_BY);

            _sb.append(_SPACE_ORDER_BY_SPACE);

            appendColumnName(propOrColumnName);
        });
    }

    /**
     * Adds an ORDER BY clause with multiple columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("lastName", "firstName")
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY last_name, first_name
     * }</pre>
     *
     * @param propOrColumnNames the columns to order by (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     */
    public This orderBy(final String... propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.ORDER_BY);

            _sb.append(_SPACE_ORDER_BY_SPACE);

            for (int i = 0, len = propOrColumnNames.length; i < len; i++) {
                if (i > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(propOrColumnNames[i]);
            }
        });
    }

    /**
     * Adds an ORDER BY clause with a single column and sort direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("name", SortDirection.DESC)
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY name DESC
     * }</pre>
     *
     * @param expr the column or expression to order by
     * @param direction the sort direction
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, if {@code expr} contains a SQL
     *         comment token, or if {@code direction} is {@code null}
     */
    public This orderBy(final String expr, final SortDirection direction) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in limit(int)
        checkSqlFragmentNotBlank(expr, "expr"); // report this overload's parameter name, not the delegate's
        N.checkArgNotNull(direction, cs.direction);

        orderBy(expr);

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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY last_name, first_name
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to order by
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element, or an element containing a SQL comment token
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     */
    public This orderBy(final Collection<String> propOrColumnNames) {
        final List<String> propOrColumnNamesSnapshot = copyAndValidateSqlFragments(propOrColumnNames, "propOrColumnNames");

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.ORDER_BY);

            _sb.append(_SPACE_ORDER_BY_SPACE);

            int i = 0;
            for (final String columnName : propOrColumnNamesSnapshot) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);
            }
        });
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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY last_name DESC, first_name DESC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to order by
     * @param direction the direction appended after each column in the ORDER BY clause
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, contains a {@code null}, empty, or blank
     *         element or an element containing a SQL comment token, or if {@code direction} is {@code null}
     */
    public This orderBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in limit(int)
        final List<String> propOrColumnNamesSnapshot = copyAndValidateSqlFragments(propOrColumnNames, "propOrColumnNames");
        N.checkArgNotNull(direction, cs.direction);

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.ORDER_BY);

            _sb.append(_SPACE_ORDER_BY_SPACE);

            int i = 0;
            for (final String columnName : propOrColumnNamesSnapshot) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(columnName);

                _sb.append(_SPACE);
                _sb.append(direction.toString());
            }
        });
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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY last_name ASC, first_name DESC
     * }</pre>
     *
     * <p><b>Note:</b> The order of columns in the generated {@code ORDER BY} clause follows the map's
     * iteration order. Pass a {@link java.util.LinkedHashMap} (or other insertion-ordered {@code Map})
     * to guarantee deterministic clause order.</p>
     *
     * @param orders map of columns to their sort directions
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code orders} is {@code null} or empty, contains a {@code null}, empty, or blank key,
     *         a key containing a SQL comment token, or maps any key to a {@code null} direction
     * @throws IllegalStateException if this builder is closed, if {@code ORDER BY} has already been set on this builder,
     *         if a preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)}, if this is an
     *         INSERT VALUES statement or an UPDATE statement that has no SET columns, if the current SELECT segment has no
     *         {@code FROM} clause yet, or if pagination or {@code FOR UPDATE} has already been emitted
     */
    public This orderBy(final Map<String, SortDirection> orders) {
        final Map<String, SortDirection> ordersSnapshot = copyAndValidateSqlFragmentMap(orders, "orders");

        for (final Map.Entry<String, SortDirection> entry : ordersSnapshot.entrySet()) {
            N.checkArgNotNull(entry.getValue(), "Value in orders");
        }

        return mutateAtomically(() -> {
            checkIfAlreadyCalled(SK.ORDER_BY);

            _sb.append(_SPACE_ORDER_BY_SPACE);

            int i = 0;

            for (final Map.Entry<String, SortDirection> entry : ordersSnapshot.entrySet()) {
                if (i++ > 0) {
                    _sb.append(_COMMA_SPACE);
                }

                appendColumnName(entry.getKey());

                _sb.append(_SPACE);
                _sb.append(entry.getValue().toString());
            }
        });
    }

    /**
     * Adds a row-count restriction to the query, rendered in the dialect's pagination syntax.
     *
     * <p>The generated clause depends on the product named by {@link SqlDialect.ProductInfo}:</p>
     * <ul>
     *   <li>Oracle, DB2: {@code FETCH FIRST count ROWS ONLY}</li>
     *   <li>SQL Server: {@code OFFSET 0 ROWS FETCH NEXT count ROWS ONLY} (SQL Server only allows
     *       {@code OFFSET ... FETCH} together with an {@code ORDER BY} clause); the {@code OFFSET 0 ROWS}
     *       prefix is omitted when {@link #offset(int)} has already been called</li>
     *   <li>any other product, or no product info: {@code LIMIT count}</li>
     * </ul>
     *
     * <p>On the {@code FETCH}-style dialects (Oracle, DB2, SQL Server) this method also consumes the
     * {@code OFFSET} and {@code FETCH} slots, because {@code OFFSET} must precede {@code FETCH}: call
     * {@link #offset(int)} <i>before</i> this method, or prefer {@link #limit(int, int)}, which emits the
     * combined clause in the correct order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .limit(10)
     *                 .build().query();
     * // Output: SELECT * FROM users LIMIT 10
     *
     * Dsl oracleDsl = Dsl.forDialect(SqlDialect.builder()
     *         .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
     *         .productInfo(SqlDialect.ProductInfo.of("Oracle"))
     *         .build());
     * String oracleSql = oracleDsl.select("*").from("users").limit(10).build().query();
     * // Output: SELECT * FROM users FETCH FIRST 10 ROWS ONLY
     * }</pre>
     *
     * @param count the maximum number of rows to return
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder has already been closed by {@link #build()}, or if
     *         {@code LIMIT} has already been set on this builder, or on a {@code FETCH}-style dialect if
     *         {@code FETCH FIRST}/{@code FETCH NEXT} has already been set, or if {@code OFFSET} was already
     *         emitted on a limit-style dialect where {@code LIMIT} must precede {@code OFFSET}, for SQL
     *         Server if {@code ORDER BY} has not been set, if a preceding qualified JOIN has not been
     *         completed with {@code on(...)}/{@code using(...)}, on a {@code FETCH}-style dialect for
     *         UPDATE/DELETE statements, on an INSERT VALUES statement or an UPDATE statement that has no SET
     *         columns, if the current SELECT segment has no {@code FROM} clause yet, or after {@code FOR UPDATE}
     *         has been emitted
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public This limit(final int count) {
        // Report the "closed builder" state first: the lifecycle error takes precedence over argument
        // validation, and calledOpSet survives build(), so a closed builder that had offset(...) would
        // otherwise report a misleading pagination-ordering error instead of the closed error every
        // sibling clause method reports.
        assertNotClosed();
        N.checkArgNotNegative(count, cs.count);

        // Only report the dialect-ordering error when LIMIT has not been claimed yet. limit(count, offset)
        // claims BOTH slots, so without this guard a duplicate limit(...) on a limit-style dialect would
        // report an ordering error instead of the accurate "already set" error that checkIfAlreadyCalled
        // produces -- and that a FETCH-style dialect already reports for the very same mistake.
        if (!usesFetchPagination() && !calledOpSet.contains(SK.LIMIT) && calledOpSet.contains(SK.OFFSET)) {
            throw new IllegalStateException("'" + SK.LIMIT + "' must be added before '" + SK.OFFSET + "' for this SQL dialect");
        }

        checkIfAlreadyCalled(SK.LIMIT);

        if (usesFetchPagination()) {
            appendFetchFirst(String.valueOf(count));
        } else {
            _sb.append(_SPACE_LIMIT_SPACE);

            _sb.append(count);
        }

        return (This) this;
    }

    /**
     * Emits a count-only restriction in the dialect's FETCH pagination syntax and consumes the related
     * clause slots. Oracle/DB2 render {@code FETCH FIRST count ROWS ONLY}; SQL Server renders
     * {@code OFFSET 0 ROWS FETCH NEXT count ROWS ONLY}, omitting the {@code OFFSET 0 ROWS} prefix when
     * an {@code OFFSET} clause was already emitted. The caller must have consumed the {@code LIMIT} slot.
     *
     * @param countToken the row count as an integer literal
     * @throws IllegalStateException if {@code FETCH FIRST}/{@code FETCH NEXT} has already been set
     */
    private void appendFetchFirst(final String countToken) {
        if (_dialectFamily == DialectFamily.SQL_SERVER) {
            checkIfAlreadyCalled(SK.FETCH_NEXT);
            calledOpSet.add(SK.FETCH_FIRST);

            if (calledOpSet.add(SK.OFFSET)) {
                calledOpSet.add(OFFSET_ROWS_SLOT);
                _sb.append(" OFFSET 0 ROWS");
            }

            _sb.append(" FETCH NEXT ").append(countToken).append(" ROWS ONLY");
        } else {
            checkIfAlreadyCalled(SK.FETCH_FIRST);
            calledOpSet.add(SK.FETCH_NEXT);
            calledOpSet.add(SK.OFFSET);

            _sb.append(" FETCH FIRST ").append(countToken).append(" ROWS ONLY");
        }
    }

    /**
     * Emits a count-plus-offset restriction in the dialect's FETCH pagination syntax
     * ({@code OFFSET offset ROWS FETCH NEXT count ROWS ONLY}) and consumes the FETCH slots.
     * The caller must have consumed the {@code LIMIT} and {@code OFFSET} slots already.
     *
     * @param countToken the row count as an integer literal
     * @param offsetToken the offset as an integer literal
     * @throws IllegalStateException if {@code FETCH FIRST}/{@code FETCH NEXT} has already been set
     */
    private void appendOffsetFetchNext(final String countToken, final String offsetToken) {
        checkIfAlreadyCalled(SK.FETCH_NEXT);
        calledOpSet.add(SK.FETCH_FIRST);

        _sb.append(_SPACE_OFFSET_SPACE).append(offsetToken).append(_SPACE_ROWS);

        _sb.append(" FETCH NEXT ").append(countToken).append(" ROWS ONLY");
    }

    /**
     * Adds a count-plus-offset pagination clause, rendered in the dialect's pagination syntax.
     *
     * <p>The generated clause depends on the product named by {@link SqlDialect.ProductInfo}:</p>
     * <ul>
     *   <li>Oracle, DB2, SQL Server: {@code OFFSET offset ROWS FETCH NEXT count ROWS ONLY}
     *       (SQL Server only allows {@code OFFSET ... FETCH} together with an {@code ORDER BY} clause)</li>
     *   <li>any other product, or no product info: {@code LIMIT count OFFSET offset}</li>
     * </ul>
     *
     * <p>The combined clause is emitted atomically in the order the dialect requires, so this method is
     * the preferred way to paginate portably across dialects.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .limit(10, 20)  // limit 10, offset 20
     *                 .build().query();
     * // Output: SELECT * FROM users LIMIT 10 OFFSET 20
     *
     * Dsl oracleDsl = Dsl.forDialect(SqlDialect.builder()
     *         .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
     *         .productInfo(SqlDialect.ProductInfo.of("Oracle"))
     *         .build());
     * String oracleSql = oracleDsl.select("*").from("users").limit(10, 20).build().query();
     * // Output: SELECT * FROM users OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
     * }</pre>
     *
     * @param count the maximum number of rows to return
     * @param offset the number of rows to skip
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder has already been closed by {@link #build()}, if
     *         {@code LIMIT} or {@code OFFSET} has already been set on this builder, on a {@code FETCH}-style
     *         dialect if {@code FETCH FIRST}/{@code FETCH NEXT} has already been set, for SQL Server if
     *         {@code ORDER BY} has not been set, on INSERT VALUES statements, on UPDATE/DELETE statements (the
     *         {@code OFFSET} portion is not valid there), if a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, if the current SELECT segment has no {@code FROM} clause yet, or
     *         after {@code FOR UPDATE} has been emitted
     * @throws IllegalArgumentException if {@code count} or {@code offset} is negative
     */
    public This limit(final int count, final int offset) {
        // Like limit(int): the lifecycle error takes precedence over argument validation.
        assertNotClosed();
        N.checkArgNotNegative(count, cs.count);
        N.checkArgNotNegative(offset, cs.offset);
        claimClauseSlots(SK.LIMIT, SK.OFFSET);

        if (usesFetchPagination()) {
            appendOffsetFetchNext(String.valueOf(count), String.valueOf(offset));
        } else {
            _sb.append(_SPACE_LIMIT_SPACE);

            _sb.append(count);

            _sb.append(_SPACE_OFFSET_SPACE);

            _sb.append(offset);
        }

        return (This) this;
    }

    /**
     * Renders a {@link Limit} condition into the buffer. A numeric limit — from the numeric constructors
     * or from a string expression that {@link Limit#Limit(String)} parsed into a concrete count/offset
     * (the {@code LIMIT}-family and SQL:2008 {@code FETCH}-family integer forms) — delegates to
     * {@link #limit(int)} / {@link #limit(int, int)} based on the offset, so it is rendered in the dialect's
     * pagination syntax. An unresolved out-of-range numeric expression is re-rendered
     * in the dialect's FETCH pagination syntax when this builder uses one (Oracle, DB2, SQL Server) and the
     * expression is a generic {@code LIMIT count [OFFSET offset]} form.
     * Shared by the {@link Criteria} and standalone-{@link Limit} branches of {@link #append(Condition)}.
     *
     * @param limit the limit condition to render (must not be {@code null})
     * @throws IllegalStateException if the required {@code LIMIT} (and, for a form carrying an offset, {@code OFFSET})
     *         or FETCH slot has already been set or cannot be emitted at the builder's current position, or if
     *         {@code OFFSET} was already emitted on a limit-style dialect where {@code LIMIT} must precede it
     *         (see {@link #limit(int)} and {@link #limit(int, int)})
     */
    private void appendLimit(final Limit limit) {
        // An out-of-int-range string expression is signalled by isResolved() == false; render it from its
        // validated numeric expression. Everything else —
        // the numeric constructors and string expressions parsed into concrete count/offset — is emitted
        // in the dialect's pagination syntax via limit(int) / limit(int, int).
        if (Strings.isNotEmpty(limit.expression()) && !limit.isResolved()) {
            if (usesFetchPagination() && appendLimitExpressionInFetchSyntax(limit.expression())) {
                return;
            }

            if (!usesFetchPagination() && calledOpSet.contains(SK.OFFSET)
                    && (!isFetchLimitExpression(limit.expression()) || !calledOpSet.contains(OFFSET_ROWS_SLOT))) {
                throw new IllegalStateException("'" + SK.LIMIT + "' must be added before '" + SK.OFFSET + "' for this SQL dialect");
            }

            // The retained numeric literal may itself carry an OFFSET portion; consume the
            // OFFSET slot too so a follow-up offset(...) call is rejected instead of silently emitting a
            // second OFFSET clause. Limit has already normalized and validated the full expression, so its
            // grammar's structural slots can be inspected directly.
            if (limitExpressionHasOffset(limit.expression())) {
                claimClauseSlots(SK.LIMIT, SK.OFFSET);
            } else {
                checkIfAlreadyCalled(SK.LIMIT);

                // A FETCH clause is terminal with respect to OFFSET: an existing OFFSET may legally
                // precede it, but a later offset(...) call must not be allowed after it.
                if (isFetchLimitExpression(limit.expression())) {
                    calledOpSet.add(SK.OFFSET);
                }
            }

            _sb.append(_SPACE).append(limit.expression());
        } else if (limit.offset() > 0) {
            limit(limit.count(), limit.offset());
        } else {
            limit(limit.count());
        }
    }

    /**
     * Attempts to re-render a generic {@code LIMIT count [OFFSET offset]} or {@code LIMIT offset, count}
     * numeric expression in the dialect's FETCH pagination syntax, consuming the same clause slots as
     * {@link #limit(int)} / {@link #limit(int, int)}. On SQL Server, an unresolved SQL-standard
     * {@code [OFFSET ...] FETCH FIRST|NEXT ...} expression is normalized to the SQL Server-required
     * {@code OFFSET ... FETCH NEXT ...} form as well. Returns {@code false} without emitting anything
     * when no translatable form matches, in which case the caller emits the expression verbatim.
     *
     * @param expression the normalized limit expression from {@link Limit#expression()}
     * @return {@code true} if the expression was rendered in FETCH pagination syntax
     * @throws IllegalStateException if a matching form is found but its {@code LIMIT}/{@code OFFSET}/FETCH slots have
     *         already been set or cannot be emitted at the builder's current position
     */
    private boolean appendLimitExpressionInFetchSyntax(final String expression) {
        final Matcher matcher = GENERIC_LIMIT_EXPRESSION_PATTERN.matcher(expression);

        if (matcher.matches()) {
            final String commaCountToken = matcher.group(3);
            final String countToken = commaCountToken == null ? matcher.group(1) : commaCountToken;
            final String offsetToken = commaCountToken == null ? matcher.group(2) : matcher.group(1);

            appendFetchTokens(countToken, offsetToken);
            return true;
        }

        if (_dialectFamily == DialectFamily.SQL_SERVER) {
            final Matcher fetchMatcher = FETCH_LIMIT_EXPRESSION_PATTERN.matcher(expression);

            if (fetchMatcher.matches()) {
                appendFetchTokens(fetchMatcher.group(2), fetchMatcher.group(1));
                return true;
            }
        }

        return false;
    }

    /**
     * Claims the appropriate pagination slots and renders count/optional-offset FETCH tokens.
     *
     * @throws IllegalStateException if the {@code LIMIT}, {@code OFFSET} (when {@code offsetToken} is present), or
     *         FETCH slots have already been set or cannot be emitted at the builder's current position
     */
    private void appendFetchTokens(final String countToken, final String offsetToken) {
        if (offsetToken == null) {
            checkIfAlreadyCalled(SK.LIMIT);
            appendFetchFirst(countToken);
        } else {
            claimClauseSlots(SK.LIMIT, SK.OFFSET);
            appendOffsetFetchNext(countToken, offsetToken);
        }
    }

    /** Returns whether an unresolved LIMIT/FETCH literal contains a semantic offset slot. */
    private boolean limitExpressionHasOffset(final String expression) {
        final Matcher matcher = GENERIC_LIMIT_EXPRESSION_PATTERN.matcher(expression);

        if (matcher.matches()) {
            return matcher.group(2) != null || matcher.group(3) != null;
        }

        // Limit normalizes and validates every accepted expression. The only other family is
        // [OFFSET slot ROWS] FETCH ..., whose semantic offset is therefore unambiguously the prefix.
        return expression.startsWith(SK.OFFSET + SK.SPACE);
    }

    /** Returns whether an unresolved pagination literal starts with FETCH FIRST/NEXT syntax. */
    private boolean isFetchLimitExpression(final String expression) {
        // Expressions with their own leading OFFSET take the offset-aware branch above.
        return expression.startsWith("FETCH ");
    }

    /**
     * Adds an OFFSET clause to skip a number of rows, rendered in the dialect's pagination syntax.
     *
     * <p>On Oracle, DB2 and SQL Server dialects (per {@link SqlDialect.ProductInfo}) the clause is
     * rendered as {@code OFFSET offset ROWS}; on those dialects call this method <i>before</i>
     * {@link #limit(int)}, because {@code OFFSET} must precede {@code FETCH}. On all other dialects the
     * clause is rendered as {@code OFFSET offset}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .limit(10)
     *                 .offset(20)
     *                 .build().query();
     * // Output: SELECT * FROM users LIMIT 10 OFFSET 20
     * }</pre>
     *
     * @param offset the number of rows to skip
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code OFFSET} has already been set on this builder,
     *                               for SQL Server if {@code ORDER BY} has not been set, if a preceding qualified JOIN has
     *                               not been completed with {@code on(...)}/{@code using(...)}, on INSERT VALUES,
     *                               UPDATE, or DELETE statements, if the current SELECT segment has no {@code FROM}
     *                               clause yet, or after {@code FOR UPDATE} has been emitted
     * @throws IllegalArgumentException if {@code offset} is negative
     */
    public This offset(final int offset) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in limit(int)
        N.checkArgNotNegative(offset, cs.offset);
        checkIfAlreadyCalled(SK.OFFSET);

        _sb.append(_SPACE_OFFSET_SPACE).append(offset);

        if (usesFetchPagination()) {
            calledOpSet.add(OFFSET_ROWS_SLOT);
            _sb.append(_SPACE_ROWS);
        }

        return (This) this;
    }

    /**
     * Adds an OFFSET ROWS clause (SQL:2008 standard syntax).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("id")
     *                 .offsetRows(20)
     *                 .fetchNextRows(10)
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
     * }</pre>
     *
     * @param offset the number of rows to skip
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code OFFSET} has already been set on this builder,
     *                               for SQL Server if {@code ORDER BY} has not been set, if a preceding qualified JOIN has
     *                               not been completed with {@code on(...)}/{@code using(...)}, on INSERT VALUES,
     *                               UPDATE, or DELETE statements, if the current SELECT segment has no {@code FROM}
     *                               clause yet, or after {@code FOR UPDATE} has been emitted
     * @throws IllegalArgumentException if {@code offset} is negative
     * @see #offset(int)
     * @see #limit(int, int)
     */
    public This offsetRows(final int offset) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in limit(int)
        N.checkArgNotNegative(offset, cs.offset);
        checkIfAlreadyCalled(SK.OFFSET);
        calledOpSet.add(OFFSET_ROWS_SLOT);

        _sb.append(_SPACE_OFFSET_SPACE).append(offset).append(_SPACE_ROWS);

        return (This) this;
    }

    /**
     * Adds a FETCH NEXT N ROWS ONLY clause (SQL:2008 standard syntax).
     * Calling either {@link #fetchNextRows(int)} or {@link #fetchFirstRows(int)} consumes both
     * FETCH slots and the general row-limit slot; it also closes the OFFSET slot because SQL
     * requires OFFSET to precede FETCH. Call {@link #offsetRows(int)} first when an offset is needed;
     * {@link #offset(int)} is also compatible when this builder uses a FETCH-style dialect. On SQL
     * Server, an omitted offset is rendered as {@code OFFSET 0 ROWS} automatically.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("id")
     *                 .offsetRows(0)
     *                 .fetchNextRows(10)
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
     * }</pre>
     *
     * @param count the number of rows to fetch
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code LIMIT}, {@code FETCH NEXT}, or {@code FETCH FIRST}
     *                               has already been set, if a prior offset used limit-style {@code OFFSET n} rather than
     *                               {@code OFFSET n ROWS}, for SQL Server if {@code ORDER BY} has not been set, if a
     *                               preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)},
     *                               on INSERT VALUES, UPDATE, or DELETE statements, if the current SELECT segment has no
     *                               {@code FROM} clause yet, or after {@code FOR UPDATE} has been emitted
     * @throws IllegalArgumentException if {@code count} is negative
     * @see #limit(int)
     * @see #limit(int, int)
     */
    public This fetchNextRows(final int count) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in limit(int)
        N.checkArgNotNegative(count, cs.count);
        final boolean offsetAlreadySet = calledOpSet.contains(SK.OFFSET);
        checkExplicitFetchSlotsAvailable(SK.FETCH_NEXT);
        calledOpSet.add(SK.LIMIT);
        calledOpSet.add(SK.FETCH_NEXT);
        calledOpSet.add(SK.FETCH_FIRST);
        calledOpSet.add(SK.OFFSET);

        if (_dialectFamily == DialectFamily.SQL_SERVER && !offsetAlreadySet) {
            calledOpSet.add(OFFSET_ROWS_SLOT);
            _sb.append(" OFFSET 0 ROWS");
        }

        _sb.append(" FETCH NEXT ").append(count).append(" ROWS ONLY");

        return (This) this;
    }

    /**
     * Adds a FETCH FIRST N ROWS ONLY clause (SQL standard syntax).
     * Calling either {@link #fetchFirstRows(int)} or {@link #fetchNextRows(int)} consumes both
     * FETCH slots and the general row-limit slot; it also closes the OFFSET slot because SQL
     * requires OFFSET to precede FETCH. Call {@link #offsetRows(int)} first when an offset is needed;
     * {@link #offset(int)} is also compatible when this builder uses a FETCH-style dialect. SQL Server
     * renders this request as {@code OFFSET 0 ROWS FETCH NEXT ...} (or reuses an existing offset),
     * because it supports neither a standalone {@code FETCH FIRST} nor FETCH without OFFSET.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .orderBy("id")
     *                 .fetchFirstRows(10)
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY
     * }</pre>
     *
     * @param count the number of rows to fetch
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, if {@code LIMIT}, {@code FETCH FIRST}, or {@code FETCH NEXT}
     *                               has already been set, if a prior offset used limit-style {@code OFFSET n} rather than
     *                               {@code OFFSET n ROWS}, for SQL Server if {@code ORDER BY} has not been set, if a
     *                               preceding qualified JOIN has not been completed with {@code on(...)}/{@code using(...)},
     *                               on INSERT VALUES, UPDATE, or DELETE statements, if the current SELECT segment has no
     *                               {@code FROM} clause yet, or after {@code FOR UPDATE} has been emitted
     * @throws IllegalArgumentException if {@code count} is negative
     * @see #limit(int)
     * @see #limit(int, int)
     */
    public This fetchFirstRows(final int count) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in limit(int)
        N.checkArgNotNegative(count, cs.count);
        final boolean offsetAlreadySet = calledOpSet.contains(SK.OFFSET);
        checkExplicitFetchSlotsAvailable(SK.FETCH_FIRST);
        calledOpSet.add(SK.LIMIT);
        calledOpSet.add(SK.FETCH_FIRST);
        calledOpSet.add(SK.FETCH_NEXT);
        calledOpSet.add(SK.OFFSET);

        if (_dialectFamily == DialectFamily.SQL_SERVER) {
            if (!offsetAlreadySet) {
                calledOpSet.add(OFFSET_ROWS_SLOT);
                _sb.append(" OFFSET 0 ROWS");
            }

            _sb.append(" FETCH NEXT ").append(count).append(" ROWS ONLY");
        } else {
            _sb.append(" FETCH FIRST ").append(count).append(" ROWS ONLY");
        }

        return (This) this;
    }

    /**
     * Ensures a JOIN is appended to the FROM portion of a live SELECT segment, before filtering,
     * grouping, ordering, pagination, or row-locking clauses.
     *
     * @throws IllegalStateException if this builder is closed, if a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, if a completed set-operation operand has already been emitted, if there is
     *         no current SELECT segment with a completed {@code FROM} clause, or if a WHERE, GROUP BY, HAVING, ORDER BY,
     *         pagination, or FOR UPDATE clause has already been emitted
     */
    private void checkCanAppendJoin() {
        assertNotClosed();

        if (_joinConditionAllowed) {
            throw new IllegalStateException("The preceding qualified JOIN must be completed with on(...) or using(...) before another JOIN");
        }

        if (_hasCompletedSetOperation) {
            throw new IllegalStateException("JOIN clauses cannot be added after a completed set-operation operand");
        }

        if (_op != OperationType.QUERY || _isForConditionOnly || !_hasFromBeenSet) {
            throw new IllegalStateException("A JOIN requires a current SELECT segment with a completed FROM clause");
        }

        if (calledOpSet.contains(SK.WHERE) || calledOpSet.contains(SK.GROUP_BY) || calledOpSet.contains(SK.HAVING) || calledOpSet.contains(SK.ORDER_BY)
                || calledOpSet.contains(SK.LIMIT) || calledOpSet.contains(SK.OFFSET) || calledOpSet.contains(SK.FETCH_FIRST)
                || calledOpSet.contains(SK.FETCH_NEXT) || calledOpSet.contains(SK.FOR_UPDATE)) {
            throw new IllegalStateException("JOIN clauses must be added before WHERE, GROUP BY, HAVING, ORDER BY, pagination, and FOR UPDATE clauses");
        }
    }

    /**
     * Ensures ON/USING is attached once, directly after a JOIN form that supports it.
     *
     * @throws IllegalStateException if this builder is closed, if a completed set-operation operand has already been
     *         emitted, if there is no immediately preceding JOIN that accepts an {@code ON}/{@code USING} connector, or if
     *         a WHERE, GROUP BY, HAVING, ORDER BY, pagination, or FOR UPDATE clause has already been emitted
     */
    private void checkCanAppendJoinCondition() {
        assertNotClosed();

        if (_hasCompletedSetOperation) {
            throw new IllegalStateException("ON/USING cannot be added after a completed set-operation operand");
        }

        if (!_joinConditionAllowed) {
            throw new IllegalStateException("ON/USING requires an immediately preceding JOIN that has no connector yet");
        }

        if (calledOpSet.contains(SK.WHERE) || calledOpSet.contains(SK.GROUP_BY) || calledOpSet.contains(SK.HAVING) || calledOpSet.contains(SK.ORDER_BY)
                || calledOpSet.contains(SK.LIMIT) || calledOpSet.contains(SK.OFFSET) || calledOpSet.contains(SK.FETCH_FIRST)
                || calledOpSet.contains(SK.FETCH_NEXT) || calledOpSet.contains(SK.FOR_UPDATE)) {
            throw new IllegalStateException("ON/USING must be added before WHERE, GROUP BY, HAVING, ORDER BY, pagination, and FOR UPDATE clauses");
        }
    }

    /**
     * Validates the mutually exclusive row-limit slots before changing any builder state, then emits the
     * lazily-rendered statement prefix via {@link #init(boolean)}. Performing every check first ensures
     * that a rejected pagination call does not poison an otherwise valid builder that the caller may still
     * finish or build.
     *
     * @param requestedFetchSlot the explicit FETCH slot requested by the caller
     * @throws IllegalStateException if the builder is closed, if a preceding qualified JOIN has not been
     *                               completed, if this is an INSERT VALUES, UPDATE, or DELETE statement, if
     *                               the current SELECT segment has no {@code FROM} clause yet, if a LIMIT or
     *                               either FETCH form has already been emitted, if a prior offset did not use
     *                               {@code OFFSET n ROWS} syntax, if {@code FOR UPDATE} has already been
     *                               emitted, or, for SQL Server, if {@code ORDER BY} has not been set
     */
    private void checkExplicitFetchSlotsAvailable(final String requestedFetchSlot) {
        checkClauseCanBeAppended(requestedFetchSlot);

        if (calledOpSet.contains(SK.LIMIT)) {
            throw new IllegalStateException("'" + SK.LIMIT + "' has already been set and cannot be combined with '" + requestedFetchSlot + "'");
        }

        if (calledOpSet.contains(SK.FETCH_FIRST) || calledOpSet.contains(SK.FETCH_NEXT)) {
            throw new IllegalStateException("A FETCH row-limit clause has already been set and cannot be set again");
        }

        if (calledOpSet.contains(SK.OFFSET) && !calledOpSet.contains(OFFSET_ROWS_SLOT)) {
            throw new IllegalStateException("'" + requestedFetchSlot + "' requires OFFSET ROWS syntax; use offsetRows(...) before FETCH");
        }

        init(true);
    }

    /**
     * Runs a structured-clause mutation transactionally. Rendering a column or condition can still
     * reject an unsafe fragment after the clause prefix has been selected, and named-parameter
     * rendering can update several correlated collections. If rendering fails, restore all builder
     * state that such a mutation can change so the caller may retry with a valid clause. Raw
     * {@link #append(String)} deliberately remains an unstructured escape hatch and is not wrapped.
     *
     * @param mutation the structured-clause rendering operation; must not be {@code null}
     * @return this builder after successful rendering
     * @throws IllegalStateException if this builder is closed; any {@link RuntimeException} or {@link Error} thrown by
     *         {@code mutation} is rethrown unchanged after the builder state has been restored
     * @throws IllegalArgumentException if {@code mutation} is {@code null}
     */
    @SuppressWarnings("unchecked")
    protected This mutateAtomically(final Runnable mutation) {
        assertNotClosed();
        N.checkArgNotNull(mutation, cs.mutation);

        final MutationCheckpoint checkpoint = new MutationCheckpoint(this);

        try {
            mutation.run();
            return (This) this;
        } catch (final RuntimeException | Error e) {
            checkpoint.restore(this);
            throw e;
        }
    }

    /** Snapshot of the mutable state touched while initializing or rendering a structured clause. */
    private static final class MutationCheckpoint {
        private static final Object[] EMPTY_SNAPSHOT = new Object[0];

        private final String sql;
        private final Object[] parameters;
        private final Object[] namedParameterNameOccurrences;
        private final Object[] generatedNamedParameterNames;
        private final Object[] renderedNamedParameterTokens;
        private final Object[] calledOperations;
        private final Object[] aliasPropColumnNameMap;
        private final OperationType operation;
        private final Class<?> entityClass;
        private final BeanInfo entityInfo;
        private final ImmutableMap<String, ColumnInfo> propColumnNameMap;
        private final Collection<String> propOrColumnNames;
        private final Map<String, String> propOrColumnNameAliases;
        private final List<Selection> multiSelects;
        private final Map<String, Object> props;
        private final Collection<Map<String, Object>> propsList;
        private final String tableName;
        private final String tableAlias;
        private final String selectModifier;
        private final int selectKeywordEndIdx;
        private final boolean hasGeneratedParameterPlaceholder;
        private final boolean hasFromBeenSet;
        private final boolean joinConditionAllowed;
        private final boolean hasCompletedSetOperation;
        private final boolean hasSetOperation;
        private final boolean setListStarted;

        /** Captures a snapshot of the mutable state that structured-clause rendering may change. */
        MutationCheckpoint(final AbstractQueryBuilder<?> builder) {
            sql = builder._sb.toString();
            parameters = snapshot(builder._parameters);
            namedParameterNameOccurrences = snapshot(builder._namedParameterNameOccurrences);
            generatedNamedParameterNames = snapshot(builder._generatedNamedParameterNames);
            renderedNamedParameterTokens = snapshot(builder._renderedNamedParameterTokens);
            calledOperations = snapshot(builder.calledOpSet);
            aliasPropColumnNameMap = builder._aliasPropColumnNameMap == null ? null : snapshot(builder._aliasPropColumnNameMap);
            operation = builder._op;
            entityClass = builder._entityClass;
            entityInfo = builder._entityInfo;
            propColumnNameMap = builder._propColumnNameMap;
            propOrColumnNames = builder._propOrColumnNames;
            propOrColumnNameAliases = builder._propOrColumnNameAliases;
            multiSelects = builder._multiSelects;
            props = builder._props;
            propsList = builder._propsList;
            tableName = builder._tableName;
            tableAlias = builder._tableAlias;
            selectModifier = builder._selectModifier;
            selectKeywordEndIdx = builder._selectKeywordEndIdx;
            hasGeneratedParameterPlaceholder = builder._hasGeneratedParameterPlaceholder;
            hasFromBeenSet = builder._hasFromBeenSet;
            joinConditionAllowed = builder._joinConditionAllowed;
            hasCompletedSetOperation = builder._hasCompletedSetOperation;
            hasSetOperation = builder._hasSetOperation;
            setListStarted = builder._setListStarted;
        }

        /** Restores the captured snapshot into the given builder, discarding every mutation made since capture. */
        void restore(final AbstractQueryBuilder<?> builder) {
            builder._sb.setLength(0);
            builder._sb.append(sql);

            restore(builder._parameters, parameters);
            restore(builder._namedParameterNameOccurrences, namedParameterNameOccurrences);
            restore(builder._generatedNamedParameterNames, generatedNamedParameterNames);
            restore(builder._renderedNamedParameterTokens, renderedNamedParameterTokens);
            restore(builder.calledOpSet, calledOperations);

            builder._aliasPropColumnNameMap = aliasPropColumnNameMap == null ? null : new HashMap<>();
            if (aliasPropColumnNameMap != null) {
                restore(builder._aliasPropColumnNameMap, aliasPropColumnNameMap);
            }
            builder._op = operation;
            builder._entityClass = entityClass;
            builder._entityInfo = entityInfo;
            builder._propColumnNameMap = propColumnNameMap;
            builder._propOrColumnNames = propOrColumnNames;
            builder._propOrColumnNameAliases = propOrColumnNameAliases;
            builder._multiSelects = multiSelects;
            builder._props = props;
            builder._propsList = propsList;
            builder._tableName = tableName;
            builder._tableAlias = tableAlias;
            builder._selectModifier = selectModifier;
            builder._selectKeywordEndIdx = selectKeywordEndIdx;
            builder._hasGeneratedParameterPlaceholder = hasGeneratedParameterPlaceholder;
            builder._hasFromBeenSet = hasFromBeenSet;
            builder._joinConditionAllowed = joinConditionAllowed;
            builder._hasCompletedSetOperation = hasCompletedSetOperation;
            builder._hasSetOperation = hasSetOperation;
            builder._setListStarted = setListStarted;
        }

        // Checkpoints are read once, only on failure: flat snapshots avoid allocating a second
        // hash table and one node per entry on every successful clause. Copy keys AND values;
        // saving Map.Entry instances would retain live nodes whose values can subsequently change.
        private static Object[] snapshot(final Map<?, ?> values) {
            if (values.isEmpty()) {
                return EMPTY_SNAPSHOT;
            }
            final Object[] result = new Object[values.size() * 2];
            int i = 0;
            for (final Map.Entry<?, ?> entry : values.entrySet()) {
                result[i++] = entry.getKey();
                result[i++] = entry.getValue();
            }
            return result;
        }

        private static Object[] snapshot(final Collection<?> values) {
            return values.isEmpty() ? EMPTY_SNAPSHOT : values.toArray();
        }

        @SuppressWarnings("unchecked")
        private static <K, V> void restore(final Map<K, V> target, final Object[] values) {
            target.clear();
            for (int i = 0; i < values.length; i += 2) {
                target.put((K) values[i], (V) values[i + 1]);
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> void restore(final Collection<T> target, final Object[] values) {
            target.clear();
            for (final Object value : values) {
                target.add((T) value);
            }
        }
    }

    /**
     * Validates and records that the given clause keyword has been emitted, and throws if it was
     * already recorded or would be emitted outside SQL clause order. Validation is performed before
     * mutating the recorded-clause set, so a rejected call does not poison the builder.
     * Used by clause methods (e.g. {@code WHERE}, {@code GROUP BY}, {@code HAVING}, {@code ORDER BY},
     * {@code LIMIT}, {@code OFFSET}) that may appear at most once per built statement.
     *
     * @param op the clause keyword that is being emitted (e.g. {@link SK#WHERE}, {@link SK#GROUP_BY})
     * @throws IllegalStateException if the builder is closed, a preceding qualified JOIN has not been
     *                               completed, {@code op} is not valid for the current statement type or
     *                               dialect (any clause on an INSERT VALUES statement, GROUP BY/HAVING or
     *                               OFFSET/FETCH on UPDATE/DELETE, SQL Server pagination without ORDER BY),
     *                               a SELECT clause is requested before FROM, WHERE/GROUP BY/HAVING is
     *                               requested after a completed set-operation operand, an earlier SQL clause
     *                               is requested after a later one, {@code op} has already been recorded, or
     *                               the statement prefix cannot be rendered (see {@link #init(boolean)})
     */
    protected void checkIfAlreadyCalled(final String op) {
        checkClauseCanBeAppended(op);

        if (calledOpSet.contains(op)) {
            throw new IllegalStateException("'" + op + "' has already been set and cannot be set again");
        }

        // UPDATE and DELETE prefixes are emitted lazily. Initialize only after every side-effect-free
        // placement/duplicate check, and reserve the slot only after initialization succeeds.
        init(true);
        calledOpSet.add(op);
    }

    /**
     * Checks the structural position of a clause without changing builder state. SQL pagination has
     * dialect-dependent internal ordering, so LIMIT/OFFSET/FETCH are treated as one terminal family;
     * their mutual-exclusion and ordering rules remain with the pagination methods themselves.
     */
    private void checkClauseCanBeAppended(final String op) {
        checkClauseCanBeAppended(op, false);
    }

    /**
     * Checks the structural position of a clause without changing builder state.
     * {@code orderByCarriedByCriteria} marks a pagination slot pre-validated on behalf of a {@link Criteria}
     * that carries its own ORDER BY: that ORDER BY renders before the criteria's LIMIT, so it satisfies
     * SQL Server's OFFSET/FETCH prerequisite even though this side-effect-free pre-validation has not yet
     * recorded it in {@code calledOpSet}.
     *
     * @throws IllegalStateException if the builder is closed, a preceding qualified JOIN has not been completed,
     *         {@code op} is not valid for the current statement type or dialect, a SELECT clause is requested
     *         before FROM, WHERE/GROUP BY/HAVING is requested after a completed set-operation operand, or an
     *         earlier SQL clause is requested after a later one
     */
    private void checkClauseCanBeAppended(final String op, final boolean orderByCarriedByCriteria) {
        assertNotClosed();

        if (_joinConditionAllowed) {
            throw new IllegalStateException("The preceding qualified JOIN must be completed with on(...) or using(...) before '" + op + "'");
        }

        if (_op == OperationType.ADD) {
            throw new IllegalStateException("'" + op + "' cannot be added to an INSERT VALUES statement");
        }

        if (_op == OperationType.UPDATE || _op == OperationType.DELETE) {
            if (SK.GROUP_BY.equals(op) || SK.HAVING.equals(op)) {
                throw new IllegalStateException("'" + op + "' is only valid for SELECT queries");
            }

            if (SK.OFFSET.equals(op) || SK.FETCH_FIRST.equals(op) || SK.FETCH_NEXT.equals(op) || (SK.LIMIT.equals(op) && usesFetchPagination())) {
                throw new IllegalStateException("'" + op + "' pagination is not valid for " + _op + " statements in this SQL dialect");
            }
        }

        // SQL Server's OFFSET/FETCH grammar is part of ORDER BY. Enforce that prerequisite for
        // every public route into the pagination renderer: LIMIT is translated to OFFSET/FETCH for
        // this dialect, while OFFSET and the explicit FETCH methods arrive under their own slots.
        // Oracle and DB2 also use FETCH syntax but do not share this SQL Server-only restriction.
        if (_dialectFamily == DialectFamily.SQL_SERVER && (SK.LIMIT.equals(op) || SK.OFFSET.equals(op) || SK.FETCH_FIRST.equals(op) || SK.FETCH_NEXT.equals(op))
                && !calledOpSet.contains(SK.ORDER_BY) && !orderByCarriedByCriteria) {
            throw new IllegalStateException("SQL Server OFFSET/FETCH pagination requires an ORDER BY clause");
        }

        if (SK.FOR_UPDATE.equals(op) && (_op != OperationType.QUERY || _isForConditionOnly)) {
            throw new IllegalStateException("'" + SK.FOR_UPDATE + "' is only valid for SELECT queries");
        }

        if (_hasCompletedSetOperation && (SK.WHERE.equals(op) || SK.GROUP_BY.equals(op) || SK.HAVING.equals(op))) {
            throw new IllegalStateException("'" + op + "' cannot be added after a completed set-operation operand");
        }

        if (_op == OperationType.QUERY && !_isForConditionOnly && !_hasFromBeenSet && !_hasCompletedSetOperation) {
            throw new IllegalStateException("'" + op + "' requires a current SELECT segment with a completed FROM clause");
        }

        final String laterClause = findAlreadyEmittedLaterClause(op);

        if (laterClause != null) {
            throw new IllegalStateException("'" + op + "' must be added before '" + laterClause + "'");
        }
    }

    /**
     * Returns the first already-emitted clause that {@code op} must precede in SQL clause order, or
     * {@code null} when {@code op} may be appended at the current position. Pagination clauses are one
     * terminal family whose internal ordering is enforced by the pagination methods themselves.
     */
    private String findAlreadyEmittedLaterClause(final String op) {
        if (SK.WHERE.equals(op)) {
            return firstCalledClause(1);
        } else if (SK.GROUP_BY.equals(op)) {
            return firstCalledClause(2);
        } else if (SK.HAVING.equals(op)) {
            return firstCalledClause(3);
        } else if (SK.ORDER_BY.equals(op)) {
            return firstCalledClause(4);
        } else if (SK.LIMIT.equals(op) || SK.OFFSET.equals(op) || SK.FETCH_FIRST.equals(op) || SK.FETCH_NEXT.equals(op)) {
            return calledOpSet.contains(SK.FOR_UPDATE) ? SK.FOR_UPDATE : null;
        }

        return null;
    }

    // One shared order avoids allocating a varargs array at every clause check. Pagination
    // retains its dialect-specific rules; this order only identifies an already-emitted later clause.
    private static final String[] CLAUSE_ORDER = { SK.WHERE, SK.GROUP_BY, SK.HAVING, SK.ORDER_BY, SK.LIMIT, SK.OFFSET, SK.FETCH_FIRST, SK.FETCH_NEXT,
            SK.FOR_UPDATE };

    /** Returns the first recorded clause at or after the given index in SQL clause order. */
    private String firstCalledClause(final int firstIndex) {
        for (int i = firstIndex; i < CLAUSE_ORDER.length; i++) {
            final String clause = CLAUSE_ORDER[i];
            if (calledOpSet.contains(clause)) {
                return clause;
            }
        }

        return null;
    }

    /**
     * Atomically reserves clause slots that must be emitted together. All conflicts are checked before
     * any slot is added, so a rejected combined clause does not leave the builder in a partially changed
     * state.
     *
     * @param ops the clause keywords to reserve
     * @throws IllegalStateException if any requested clause has already been emitted or cannot be
     *                               emitted at the builder's current structural position, or if the
     *                               statement prefix cannot be rendered (see {@link #init(boolean)})
     */
    private void claimClauseSlots(final String... ops) {
        for (final String op : ops) {
            checkClauseCanBeAppended(op);

            if (calledOpSet.contains(op)) {
                throw new IllegalStateException("'" + op + "' has already been set and cannot be set again");
            }
        }

        init(true);
        Collections.addAll(calledOpSet, ops);
    }

    /**
     * Appends a condition to the SQL statement.
     * <p>A {@link Criteria} applies its SELECT modifier to the current SELECT segment and is then expanded
     * into its JOIN/WHERE/GROUP&nbsp;BY/HAVING/set-operation/ORDER&nbsp;BY/LIMIT parts. A clause condition
     * ({@code Where}, {@code GroupBy}, {@code Having}, {@code OrderBy}, {@code Limit})
     * is rendered with its own keyword. A set-operation clause ({@code Union}, {@code UnionAll},
     * {@code Intersect}, {@code Except}, {@code Minus}) is validated like the corresponding
     * {@code union(...)}/{@code intersect(...)}/... method: it must follow a SELECT segment completed by
     * {@code from(...)}, must precede {@code ORDER BY}, pagination, and {@code FOR UPDATE}, its operand
     * must be a complete, lexically SELECT-only {@code SELECT} sub-query (a syntactic check, not a read-only
     * guarantee). Structured and builder-backed operands with their own set operations, ORDER BY,
     * pagination, or FOR UPDATE require explicit derived-table isolation first. ORDER BY and pagination
     * intended for the combined result belong on this builder after the final set operation, rather than
     * inside its operand. Raw SQL operands remain responsible for their own grouping. Afterwards only compound-result clauses
     * ({@code ORDER BY}, pagination, {@code FOR UPDATE}) may follow. Any other condition is appended with
     * a leading {@code WHERE} keyword
     * (unless this builder is condition-only). A multi-element {@link com.landawn.abacus.query.condition.Junction}
     * is rendered with each member
     * parenthesized individually and joined by the junction operator, with no surrounding parentheses.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .append(Filters.and(Filters.greaterThan("age", 18), Filters.lessThan("age", 65)))
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE (age > ?) AND (age < ?)
     * }</pre>
     *
     * @param condition the condition to append
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code condition} is {@code null}, if a predicate position contains a
     *                                  non-predicate condition, if a generic clause uses an operator that requires a
     *                                  dedicated builder method or condition type, or if a set-operation operand
     *                                  (standalone or carried by a Criteria) is not a complete, lexically SELECT-only {@code SELECT} query
     *                                  or requires explicit branch isolation, or if a Criteria JOIN's {@link Using} connector
     *                                  renders a qualified column name
     * @throws IllegalStateException if this builder is closed; if a clause emitted by {@code condition} (WHERE, GROUP BY,
     *                               HAVING, ORDER BY, or pagination, including the implicit WHERE of a plain predicate) has
     *                               already been set, would be emitted after a clause that must follow it, or cannot be placed
     *                               at the builder's current position (as documented for {@link #where(Condition)},
     *                               {@link #groupBy(String)}, {@link #having(Condition)}, {@link #orderBy(String)}, and
     *                               {@link #limit(int)}); if a Criteria carries JOINs that cannot be appended at the current
     *                               position (see {@link #join(String)}); if a Criteria select modifier has no current SELECT
     *                               segment or that segment already has a select modifier; or if a set-operation clause is
     *                               appended to a builder that is not building a SELECT query (or is in condition-only
     *                               mode), while a preceding qualified JOIN is incomplete, before the current SELECT segment
     *                               has been completed by {@code from(...)}, or after {@code ORDER BY}, pagination, or
     *                               {@code FOR UPDATE}
     * @see Filters
     */
    @Beta
    public This append(final Condition condition) {
        N.checkArgNotNull(condition, cs.condition);

        return mutateAtomically(() -> appendConditionObject(condition));
    }

    /**
     * Renders a condition after the public entry point has installed its mutation checkpoint.
     *
     * @param condition the non-{@code null} condition to render
     * @throws IllegalArgumentException under the argument conditions documented for {@link #append(Condition)}
     * @throws IllegalStateException under the builder-state conditions documented for {@link #append(Condition)}
     */
    private void appendConditionObject(final Condition condition) {

        if (condition instanceof final Criteria criteria) {
            final String criteriaSelectModifier = criteria.selectModifier();

            if (N.notEmpty(criteria.joins())) {
                checkCanAppendJoin();
            }

            if (N.notEmpty(criteria.setOperations())) {
                checkCanAppendSetOperation("A Criteria set operation");
                checkCriteriaSetOperationOperands(criteria);
            }

            if (Strings.isNotEmpty(criteriaSelectModifier)) {
                if (_op != OperationType.QUERY || _isForConditionOnly || _selectKeywordEndIdx < 0) {
                    throw new IllegalStateException("The Criteria select modifier ('" + criteriaSelectModifier + "') requires a current SELECT segment");
                }

                if (Strings.isNotEmpty(_selectModifier)) {
                    throw new IllegalStateException("selectModifier has already been set and cannot be set again");
                }
            }

            // Keep all Criteria validation before init(true): init may emit an UPDATE/DELETE prefix,
            // so a rejected Criteria must not be able to leave even that partial mutation behind.
            checkCriteriaClauseSlotsAvailable(criteria);
        } else if (condition instanceof final Clause clause) {
            if (isSetOperationOperator(clause.operator())) {
                // A standalone set-operation clause (Union/UnionAll/Intersect/Except/Minus) must satisfy the
                // same position and operand rules as the union(...)/intersect(...)/... methods and the
                // Criteria set-operation path; without this, an appended Union could smuggle in a
                // non-SELECT operand or land after ORDER BY/pagination/FOR UPDATE.
                checkCanAppendSetOperation(clause.operator().toString());
                checkSetOperationIsolation((SubQuery) clause.condition(), clause.operator().toString());
                checkSetOperationSubQuery(((SubQuery) clause.condition()).toSql(_namingPolicy), clause.operator().toString());
            } else if (clause.operator() == Operator.WHERE || clause.operator() == Operator.HAVING) {
                // Directly appended clause objects must not bypass the predicate checks performed by
                // where(Condition) and having(Condition).
                validatePredicateCondition(clause.condition());
            } else if (!(clause instanceof Limit) && clause.operator() != Operator.GROUP_BY && clause.operator() != Operator.ORDER_BY) {
                // OFFSET and FOR UPDATE require dedicated builder state, while LIMIT must carry the
                // structured Limit representation used by appendLimit. Rendering an arbitrary Clause
                // for any of them would bypass ordering/duplicate checks and dialect-specific syntax.
                throw new IllegalArgumentException(
                        "Unsupported clause type for append(Condition): " + clause.getClass().getName() + " with operator " + clause.operator());
            }
        } else if (!_isForConditionOnly) {
            // Conditions in this branch acquire an implicit WHERE below. Reject structural fragments
            // (for example a standalone sub-query, ON/USING, JOIN, or ANY/ALL/SOME operand) before
            // emitting the query prefix, just as where(Condition) does.
            validatePredicateCondition(condition);
        }

        init(true);

        if (condition instanceof final Criteria criteria) {
            final Collection<Join> joins = criteria.joins();
            final String criteriaSelectModifier = criteria.selectModifier();

            // Apply the modifier before any set operation below closes the current SELECT segment:
            // closeSetOperationSegment() resets _selectKeywordEndIdx, after which the modifier could no
            // longer be spliced into the segment it was validated against and build() would reject it as
            // an unattached staged modifier. Applying it here is still atomic -- the enclosing mutation
            // checkpoint rolls the insertion back if any Criteria clause is rejected during rendering.
            if (Strings.isNotEmpty(criteriaSelectModifier)) {
                selectModifier(criteriaSelectModifier);
            }

            if (N.notEmpty(joins)) {
                for (final Join join : joins) {
                    _sb.append(_SPACE).append(join.operator()).append(_SPACE);

                    if (join.joinEntities().size() == 1) {
                        _sb.append(join.joinEntities().get(0));
                    } else {
                        _sb.append(SK._PARENTHESIS_L);
                        int idx = 0;

                        for (final String joinTableName : join.joinEntities()) {
                            if (idx++ > 0) {
                                // A parenthesized comma list here is a MySQL extension. A CROSS JOIN tree
                                // is a standard joined-table operand and preserves the intended Cartesian input.
                                _sb.append(_SPACE_CROSS_JOIN_SPACE);
                            }

                            _sb.append(joinTableName);
                        }

                        _sb.append(SK._PARENTHESIS_R);
                    }

                    final Condition joinCond = join.condition();

                    if (joinCond != null) {
                        // Mirror Join.toString(): a raw join condition (e.g. an SqlExpression or Binary) needs an
                        // explicit ON keyword and a separating space; an On/Using condition renders its own keyword.
                        if (joinCond.operator() != Operator.ON && joinCond.operator() != Operator.USING) {
                            _sb.append(_SPACE_ON_SPACE);
                        }

                        appendCondition(joinCond);
                    }

                    // Join's constructor guarantees that a qualified join carries its own ON/USING predicate and
                    // that CROSS/NATURAL joins take none, so a Criteria join never leaves the connector slot open
                    // for a follow-up on()/using().
                    _joinConditionAllowed = false;
                }
            }

            final Clause where = criteria.where();

            if (where != null) {
                checkIfAlreadyCalled(SK.WHERE);
                _sb.append(_SPACE_WHERE_SPACE);
                appendCondition(where.condition());
            }

            final Clause groupBy = criteria.groupBy();

            if (groupBy != null) {
                checkIfAlreadyCalled(SK.GROUP_BY);
                _sb.append(_SPACE_GROUP_BY_SPACE);
                appendCondition(groupBy.condition());
            }

            final Clause having = criteria.having();

            if (having != null) {
                checkIfAlreadyCalled(SK.HAVING);
                _sb.append(_SPACE_HAVING_SPACE);
                appendCondition(having.condition());
            }

            final List<Clause> aggregations = criteria.setOperations();

            if (N.notEmpty(aggregations)) {
                for (final Clause aggregation : aggregations) {
                    _sb.append(_SPACE).append(aggregation.operator()).append(_SPACE);
                    appendCondition(aggregation.condition());
                }

                // Same per-segment reset the union(...)/intersect(...)/... methods perform, so a trailing
                // ORDER BY sorts the combined result unqualified rather than inheriting the left operand's
                // table alias.
                closeSetOperationSegment();
            }

            final Clause orderBy = criteria.orderBy();

            if (orderBy != null) {
                checkIfAlreadyCalled(SK.ORDER_BY);
                _sb.append(_SPACE_ORDER_BY_SPACE);
                appendCondition(orderBy.condition());
            }

            final Limit limit = criteria.limit();

            if (limit != null) {
                appendLimit(limit);
            }
        } else if (condition instanceof Clause) {
            if (condition instanceof final Limit limit) {
                appendLimit(limit);
            } else if (isSetOperationOperator(condition.operator())) {
                // Mirror the Criteria set-operation path: the operand was validated above, so emit it and
                // close the segment against further WHERE/GROUP BY/HAVING/JOIN clauses.
                _sb.append(_SPACE).append(condition.operator()).append(_SPACE);
                appendCondition(((Clause) condition).condition());
                closeSetOperationSegment();
            } else {
                if (condition.operator() == Operator.WHERE) {
                    checkIfAlreadyCalled(SK.WHERE);
                } else if (condition.operator() == Operator.GROUP_BY) {
                    checkIfAlreadyCalled(SK.GROUP_BY);
                } else if (condition.operator() == Operator.HAVING) {
                    checkIfAlreadyCalled(SK.HAVING);
                } else if (condition.operator() == Operator.ORDER_BY) {
                    checkIfAlreadyCalled(SK.ORDER_BY);
                }

                _sb.append(_SPACE).append(condition.operator()).append(_SPACE);
                appendCondition(((Clause) condition).condition());
            }
        } else {
            if (!_isForConditionOnly) {
                checkIfAlreadyCalled(SK.WHERE);
                _sb.append(_SPACE_WHERE_SPACE);
            }

            appendCondition(condition);
        }

    }

    /** Returns whether the operator is a set-operation connector (UNION, UNION ALL, INTERSECT, EXCEPT, or MINUS). */
    private static boolean isSetOperationOperator(final Operator operator) {
        return operator == Operator.UNION || operator == Operator.UNION_ALL || operator == Operator.INTERSECT || operator == Operator.EXCEPT
                || operator == Operator.MINUS;
    }

    /**
     * Pre-validates, without changing builder state, that every clause carried by the {@link Criteria} may
     * still be emitted in SQL clause order and that none of the required clause slots has been claimed yet.
     *
     * @param criteria the criteria whose WHERE/GROUP BY/HAVING/ORDER BY/LIMIT clauses to validate
     * @throws IllegalStateException if a criteria clause cannot be appended at the builder's current
     *         structural position, would follow a clause that must come after it, or has already been set
     */
    private void checkCriteriaClauseSlotsAvailable(final Criteria criteria) {
        checkCriteriaClauseOrderAvailable(criteria.where(), false, SK.WHERE, SK.GROUP_BY, SK.HAVING, SK.ORDER_BY, SK.LIMIT, SK.OFFSET, SK.FETCH_FIRST,
                SK.FETCH_NEXT, SK.FOR_UPDATE);
        checkCriteriaClauseOrderAvailable(criteria.groupBy(), false, SK.GROUP_BY, SK.HAVING, SK.ORDER_BY, SK.LIMIT, SK.OFFSET, SK.FETCH_FIRST, SK.FETCH_NEXT,
                SK.FOR_UPDATE);
        checkCriteriaClauseOrderAvailable(criteria.having(), false, SK.HAVING, SK.ORDER_BY, SK.LIMIT, SK.OFFSET, SK.FETCH_FIRST, SK.FETCH_NEXT, SK.FOR_UPDATE);
        checkCriteriaClauseOrderAvailable(criteria.orderBy(), false, SK.ORDER_BY, SK.LIMIT, SK.OFFSET, SK.FETCH_FIRST, SK.FETCH_NEXT, SK.FOR_UPDATE);
        // The criteria's own ORDER BY renders before its LIMIT, so it satisfies SQL Server's OFFSET/FETCH
        // ORDER BY prerequisite even though this side-effect-free pre-validation has not recorded it yet.
        checkCriteriaClauseOrderAvailable(criteria.limit(), criteria.orderBy() != null, SK.LIMIT, SK.FOR_UPDATE);

        checkClauseSlotAvailable(criteria.where(), SK.WHERE);
        checkClauseSlotAvailable(criteria.groupBy(), SK.GROUP_BY);
        checkClauseSlotAvailable(criteria.having(), SK.HAVING);
        checkClauseSlotAvailable(criteria.orderBy(), SK.ORDER_BY);

        final Limit limit = criteria.limit();

        if (limit != null) {
            checkClauseSlotAvailable(SK.LIMIT);

            final boolean unresolvedFetchCanFollowOffsetRows = !limit.isResolved() && Strings.isNotEmpty(limit.expression())
                    && isFetchLimitExpression(limit.expression()) && calledOpSet.contains(OFFSET_ROWS_SLOT);

            if (!usesFetchPagination() && calledOpSet.contains(SK.OFFSET) && !unresolvedFetchCanFollowOffsetRows) {
                throw new IllegalStateException("'" + SK.LIMIT + "' must be added before '" + SK.OFFSET + "' for this SQL dialect");
            }

            if ((limit.isResolved() && limit.offset() > 0)
                    || (!limit.isResolved() && Strings.isNotEmpty(limit.expression()) && limitExpressionHasOffset(limit.expression()))) {
                checkClauseSlotAvailable(SK.OFFSET);
            }
        }
    }

    /**
     * Ensures a Criteria clause is not appended after a clause that must follow it in SQL grammar.
     *
     * @throws IllegalStateException if {@code clause} is non-{@code null} and cannot be appended at the builder's current
     *         structural position, or if any of {@code laterClauseNames} has already been emitted
     */
    private void checkCriteriaClauseOrderAvailable(final Condition clause, final boolean orderByCarriedByCriteria, final String clauseName,
            final String... laterClauseNames) {
        if (clause == null) {
            return;
        }

        // checkClauseCanBeAppended also rejects WHERE/GROUP BY/HAVING after a completed set-operation operand.
        checkClauseCanBeAppended(clauseName, orderByCarriedByCriteria);

        for (final String laterClauseName : laterClauseNames) {
            if (calledOpSet.contains(laterClauseName)) {
                throw new IllegalStateException("'" + clauseName + "' must be added before '" + laterClauseName + "'");
            }
        }
    }

    /**
     * Validates every complete right-hand query in a Criteria set operation before any SQL is emitted.
     *
     * @throws IllegalArgumentException if an operand requires explicit branch isolation or is not a complete, lexically
     *         SELECT-only {@code SELECT} query
     */
    private void checkCriteriaSetOperationOperands(final Criteria criteria) {
        for (final Clause aggregation : criteria.setOperations()) {
            final SubQuery subQuery = (SubQuery) aggregation.condition();
            checkSetOperationIsolation(subQuery, aggregation.operator().toString());
            checkSetOperationSubQuery(subQuery.toSql(_namingPolicy), aggregation.operator().toString());
        }
    }

    /**
     * Ensures the slot of an optional Criteria clause has not already been claimed when the clause is present.
     *
     * @throws IllegalStateException if {@code clause} is non-{@code null} and {@code op} has already been set
     */
    private void checkClauseSlotAvailable(final Object clause, final String op) {
        if (clause != null) {
            checkClauseSlotAvailable(op);
        }
    }

    /**
     * Ensures the given clause slot has not already been claimed.
     *
     * @param op the clause keyword to check
     * @throws IllegalStateException if {@code op} has already been set
     */
    private void checkClauseSlotAvailable(final String op) {
        if (calledOpSet.contains(op)) {
            throw new IllegalStateException("'" + op + "' has already been set and cannot be set again");
        }
    }

    /**
     * Appends a string expression to the SQL statement.
     *
     * <p>When the statement built so far is nonempty, a separating space is inserted before
     * {@code expr} if the statement does not already end with a space and {@code expr} does not
     * already begin with one. Caller-supplied edge whitespace is otherwise preserved; if the
     * statement ends with a space and {@code expr} begins with one, both spaces remain. The rest of
     * {@code expr} is emitted verbatim without SQL syntax validation, escaping, or column-name conversion.
     * The leading join-connector check described below still updates builder state.</p>
     *
     * <p>A fragment that begins with an {@code ON} or {@code USING} keyword completes a pending qualified
     * JOIN exactly as {@link #on(String)} / {@link #using(String)} would, so
     * {@code join("orders o").append("ON u.id = o.user_id").where(...)} is accepted; any other fragment
     * leaves the pending JOIN open and the next clause or {@link #build()} rejects it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .append("FOR UPDATE")
     *                 .build().query();
     * // Output: SELECT * FROM users FOR UPDATE
     * }</pre>
     *
     * @param expr the expression to append
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder has already been closed by {@link #build()}, or if the
     *         statement prefix cannot be rendered yet (an INSERT with no {@code into(...)} table, an UPDATE
     *         with no {@code set(...)} columns, or a SELECT segment not completed by {@code from(...)})
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     */
    public This append(final String expr) {
        assertNotClosed();
        checkSqlFragmentNotBlank(expr, "expr");

        // Mirror append(Condition): emit any not-yet-rendered statement prefix first. Without this, an
        // append(...) that is the first write into the buffer permanently suppresses the lazily-emitted
        // "UPDATE t SET " / "DELETE FROM t" prefix, and build() then returns the bare fragment as the
        // whole statement.
        init(true);

        if (_sb.length() > 0 && _sb.charAt(_sb.length() - 1) != ' ' && expr.charAt(0) != ' ') {
            _sb.append(_SPACE);
        }

        _sb.append(expr);

        // A raw fragment that starts with a top-level ON/USING connector completes the pending qualified
        // JOIN, mirroring what appendJoinExpr does for an inline connector, so the escape-hatch idiom
        // join("orders o").append("ON u.id = o.user_id").where(...) keeps working.
        if (_joinConditionAllowed && startsWithJoinConnector(expr)) {
            _joinConditionAllowed = false;
        }

        return (This) this;
    }

    /**
     * Tests whether {@code expr}, ignoring leading whitespace, begins with an {@code ON} or {@code USING}
     * keyword that is followed by a non-identifier character or the end of the text (the same boundary
     * rule as {@link #isAliasKeywordBoundary(String, int)}, so {@code ON=}, {@code ON(} and {@code ON,} all
     * qualify while {@code ONLY}, {@code ON_HAND} and {@code ON.x} do not).
     *
     * @param expr the raw fragment to inspect (never {@code null} or blank)
     * @return {@code true} if the fragment starts with a join connector keyword
     */
    private static boolean startsWithJoinConnector(final String expr) {
        int i = 0;

        while (i < expr.length() && Character.isWhitespace(expr.charAt(i))) {
            i++;
        }

        return isSqlWordAt(expr, i, SK.ON) || isSqlWordAt(expr, i, SK.USING);
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
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE age > ?
     * }</pre>
     *
     * @param b if true, the condition will be appended
     * @param condition the condition to append
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder has already been closed by {@link #build()}, or if {@code b} is
     *                               {@code true} and {@code condition} cannot be appended in the builder's current state
     *                               (for example, a clause it emits has already been set; see {@link #append(Condition)})
     * @throws IllegalArgumentException if {@code b} is {@code true} and {@code condition} is {@code null} or is rejected by
     *                                  the argument checks of {@link #append(Condition)}
     */
    @Beta
    public This appendIf(final boolean b, final Condition condition) {
        assertNotClosed();

        if (b) {
            append(condition);
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
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE id = ? FOR UPDATE
     * }</pre>
     *
     * @param b if true, the expression will be appended
     * @param expr the expression to append
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder has already been closed by {@link #build()}, or if {@code b} is
     *         {@code true} and the statement prefix cannot be rendered yet (see {@link #append(String)})
     * @throws IllegalArgumentException if {@code b} is {@code true} and {@code expr} is {@code null}, empty, or blank
     */
    @Beta
    public This appendIf(final boolean b, final String expr) {
        assertNotClosed();

        if (b) {
            append(expr);
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
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE status = ?
     * }</pre>
     *
     * @param b if true, append conditionToAppendForTrue; otherwise append conditionToAppendForFalse
     * @param conditionToAppendForTrue the condition to append if {@code b} is true
     * @param conditionToAppendForFalse the condition to append if {@code b} is false
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder has already been closed by {@link #build()}, or if the selected
     *                               condition cannot be appended in the builder's current state (for example, a clause it
     *                               emits has already been set; see {@link #append(Condition)})
     * @throws IllegalArgumentException if the selected condition (the one chosen by {@code b}) is {@code null} or is
     *                                  rejected by the argument checks of {@link #append(Condition)}
     */
    @Beta
    public This appendIfOrElse(final boolean b, final Condition conditionToAppendForTrue, final Condition conditionToAppendForFalse) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in appendIf

        if (b) {
            append(conditionToAppendForTrue);
        } else {
            append(conditionToAppendForFalse);
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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY name ASC
     * }</pre>
     *
     * @param b if true, append exprToAppendForTrue; otherwise append exprToAppendForFalse
     * @param exprToAppendForTrue the expression to append if {@code b} is true
     * @param exprToAppendForFalse the expression to append if {@code b} is false
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder has already been closed by {@link #build()}, or if the statement
     *         prefix cannot be rendered yet (see {@link #append(String)})
     * @throws IllegalArgumentException if the selected expression (the one chosen by {@code b}) is {@code null}, empty, or blank
     */
    @Beta
    public This appendIfOrElse(final boolean b, final String exprToAppendForTrue, final String exprToAppendForFalse) {
        assertNotClosed(); // the lifecycle error takes precedence over argument validation, as in appendIf

        if (b) {
            append(exprToAppendForTrue);
        } else {
            append(exprToAppendForFalse);
        }

        return (This) this;
    }

    /**
     * Adds a UNION clause with another SQL query.
     * <p>The child is consumed once final rendering begins. Argument, parent-state, SQL-policy, and
     * branch-isolation checks run first; rejection by those checks leaves both builders reusable.
     * For ORDER BY or pagination of the combined result, apply those clauses to this builder after the
     * final set operation, leaving them off the child. To retain the child's own set operations, ORDER BY,
     * pagination, or FOR UPDATE, isolate it explicitly with {@code dsl.select("*").from(child, "branch")}.
     * Choose a projection and derived-table syntax supported by the target database. Compound children
     * are rejected uniformly; this method does not flatten their operators or add grouping implicitly.
     * Clauses supplied through raw SQL fragments remain the caller's responsibility.</p>
     * <p>For {@link SqlPolicy#NAMED_SQL}, child placeholders are rendered with this parent builder's
     * named-parameter handler so the compound statement uses one placeholder syntax.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlBuilder query1 = PSC.select("id", "name").from("users");
     * SqlBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.union(query2).build().query();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     *
     * // Sort and limit the combined result:
     * String combined = PSC.select("id").from("users")
     *         .union(PSC.select("id").from("customers")).orderBy("id").limit(5).build().query();
     *
     * // Keep the child's ordering and limit inside that branch:
     * SqlBuilder child = PSC.select("id").from("customers").orderBy("id").limit(5);
     * String isolated = PSC.select("id").from("users")
     *         .union(PSC.select("id").from(child, "branch")).build().query();
     * }</pre>
     *
     * @param sqlBuilder the SQL builder containing the query to union (must not be {@code null} and must not be this same instance)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder or {@code sqlBuilder} is closed; if a preceding qualified JOIN on this builder
     *         has not been completed with {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is
     *         in condition-only mode), its current SELECT segment has not been completed by {@code from(...)}, or ORDER BY,
     *         pagination, or FOR UPDATE has already been added; or if {@code sqlBuilder}'s statement is incomplete
     *         (see {@link #build()})
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         has generated parameter placeholders under a different SQL policy, requires explicit branch isolation,
     *         or does not build a complete, lexically SELECT-only SELECT query. Only the last check occurs after
     *         the child has been consumed by {@code build()}
     */
    public This union(final This sqlBuilder) {
        return appendSetOperation(_SPACE_UNION_SPACE, sqlBuilder, "UNION");
    }

    /**
     * Adds a UNION clause with a SQL query string.
     * UNION combines result sets from two queries and removes duplicates.
     * This overload always treats its argument as a complete query; use {@link #unionSelect(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
     * The query text is appended without adding branch isolation; callers must supply any grouping
     * required by their database for branch-local clauses or nested set operations. ORDER BY or pagination
     * intended for the combined result can be applied to this builder after the final set operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .union("SELECT id, name FROM customers")
     *                 .build().query();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     *
     * @param query the complete {@code SELECT} sub-query to union (lexically SELECT-only; a syntactic check, not a read-only guarantee)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, or is not a complete, lexically SELECT-only {@code SELECT} sub-query
     */
    public This union(final String query) {
        return appendSetOperation(_SPACE_UNION_SPACE, "UNION", query);
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
     *                 .unionSelect(columns)
     *                 .from("customers")
     *                 .build().query();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the union query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This unionSelect(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_UNION_SPACE, propOrColumnNames);
    }

    /**
     * Adds a UNION ALL clause with another SQL query.
     * <p>The child is consumed once final rendering begins. Argument, parent-state, SQL-policy, and
     * branch-isolation checks run first; rejection by those checks leaves both builders reusable.
     * For ORDER BY or pagination of the combined result, apply those clauses to this builder after the
     * final set operation, leaving them off the child. To retain the child's own set operations, ORDER BY,
     * pagination, or FOR UPDATE, isolate it explicitly with {@code dsl.select("*").from(child, "branch")}.
     * Choose a projection and derived-table syntax supported by the target database. Compound children
     * are rejected uniformly; this method does not flatten their operators or add grouping implicitly.
     * Clauses supplied through raw SQL fragments remain the caller's responsibility.</p>
     * <p>For {@link SqlPolicy#NAMED_SQL}, child placeholders are rendered with this parent builder's
     * named-parameter handler so the compound statement uses one placeholder syntax.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlBuilder query1 = PSC.select("id", "name").from("users");
     * SqlBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.unionAll(query2).build().query();
     * // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
     * }</pre>
     *
     * @param sqlBuilder the SQL builder containing the query to union all (must not be {@code null} and must not be this same instance)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder or {@code sqlBuilder} is closed; if a preceding qualified JOIN on this builder
     *         has not been completed with {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is
     *         in condition-only mode), its current SELECT segment has not been completed by {@code from(...)}, or ORDER BY,
     *         pagination, or FOR UPDATE has already been added; or if {@code sqlBuilder}'s statement is incomplete
     *         (see {@link #build()})
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         has generated parameter placeholders under a different SQL policy, requires explicit branch isolation,
     *         or does not build a complete, lexically SELECT-only SELECT query. Only the last check occurs after
     *         the child has been consumed by {@code build()}
     */
    public This unionAll(final This sqlBuilder) {
        return appendSetOperation(_SPACE_UNION_ALL_SPACE, sqlBuilder, "UNION ALL");
    }

    /**
     * Adds a UNION ALL clause with a SQL query string.
     * UNION ALL combines result sets from two queries and keeps all duplicates.
     * This overload always treats its argument as a complete query; use {@link #unionAllSelect(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
     * The query text is appended without adding branch isolation; callers must supply any grouping
     * required by their database for branch-local clauses or nested set operations. ORDER BY or pagination
     * intended for the combined result can be applied to this builder after the final set operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .unionAll("SELECT id, name FROM customers")
     *                 .build().query();
     * // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
     * }</pre>
     *
     * @param query the complete {@code SELECT} sub-query to union all (lexically SELECT-only; a syntactic check, not a read-only guarantee)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, or is not a complete, lexically SELECT-only {@code SELECT} sub-query
     */
    public This unionAll(final String query) {
        return appendSetOperation(_SPACE_UNION_ALL_SPACE, "UNION ALL", query);
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
     *                 .unionAllSelect(columns)
     *                 .from("customers")
     *                 .build().query();
     * // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the union all query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This unionAllSelect(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_UNION_ALL_SPACE, propOrColumnNames);
    }

    /**
     * Adds an INTERSECT clause with another SQL query.
     * <p>The child is consumed once final rendering begins. Argument, parent-state, SQL-policy, and
     * branch-isolation checks run first; rejection by those checks leaves both builders reusable.
     * For ORDER BY or pagination of the combined result, apply those clauses to this builder after the
     * final set operation, leaving them off the child. To retain the child's own set operations, ORDER BY,
     * pagination, or FOR UPDATE, isolate it explicitly with {@code dsl.select("*").from(child, "branch")}.
     * Choose a projection and derived-table syntax supported by the target database. Compound children
     * are rejected uniformly; this method does not flatten their operators or add grouping implicitly.
     * Clauses supplied through raw SQL fragments remain the caller's responsibility.</p>
     * <p>For {@link SqlPolicy#NAMED_SQL}, child placeholders are rendered with this parent builder's
     * named-parameter handler so the compound statement uses one placeholder syntax.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlBuilder query1 = PSC.select("id", "name").from("users");
     * SqlBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.intersect(query2).build().query();
     * // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM customers
     * }</pre>
     *
     * @param sqlBuilder the SQL builder containing the query to intersect (must not be {@code null} and must not be this same instance)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder or {@code sqlBuilder} is closed; if a preceding qualified JOIN on this builder
     *         has not been completed with {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is
     *         in condition-only mode), its current SELECT segment has not been completed by {@code from(...)}, or ORDER BY,
     *         pagination, or FOR UPDATE has already been added; or if {@code sqlBuilder}'s statement is incomplete
     *         (see {@link #build()})
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         has generated parameter placeholders under a different SQL policy, requires explicit branch isolation,
     *         or does not build a complete, lexically SELECT-only SELECT query. Only the last check occurs after
     *         the child has been consumed by {@code build()}
     */
    public This intersect(final This sqlBuilder) {
        return appendSetOperation(_SPACE_INTERSECT_SPACE, sqlBuilder, "INTERSECT");
    }

    /**
     * Adds an INTERSECT clause with a SQL query string.
     * INTERSECT returns only rows that appear in both result sets.
     * This overload always treats its argument as a complete query; use {@link #intersectSelect(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
     * The query text is appended without adding branch isolation; callers must supply any grouping
     * required by their database for branch-local clauses or nested set operations. ORDER BY or pagination
     * intended for the combined result can be applied to this builder after the final set operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .intersect("SELECT id, name FROM premium_users")
     *                 .build().query();
     * // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM premium_users
     * }</pre>
     *
     * @param query the complete {@code SELECT} sub-query to intersect (lexically SELECT-only; a syntactic check, not a read-only guarantee)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, or is not a complete, lexically SELECT-only {@code SELECT} sub-query
     */
    public This intersect(final String query) {
        return appendSetOperation(_SPACE_INTERSECT_SPACE, "INTERSECT", query);
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
     *                 .intersectSelect(columns)
     *                 .from("premium_users")
     *                 .build().query();
     * // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM premium_users
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the intersect query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This intersectSelect(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_INTERSECT_SPACE, propOrColumnNames);
    }

    /**
     * Adds an EXCEPT clause with another SQL query.
     * <p>The child is consumed once final rendering begins. Argument, parent-state, SQL-policy, and
     * branch-isolation checks run first; rejection by those checks leaves both builders reusable.
     * For ORDER BY or pagination of the combined result, apply those clauses to this builder after the
     * final set operation, leaving them off the child. To retain the child's own set operations, ORDER BY,
     * pagination, or FOR UPDATE, isolate it explicitly with {@code dsl.select("*").from(child, "branch")}.
     * Choose a projection and derived-table syntax supported by the target database. Compound children
     * are rejected uniformly; this method does not flatten their operators or add grouping implicitly.
     * Clauses supplied through raw SQL fragments remain the caller's responsibility.</p>
     * <p>For {@link SqlPolicy#NAMED_SQL}, child placeholders are rendered with this parent builder's
     * named-parameter handler so the compound statement uses one placeholder syntax.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlBuilder query1 = PSC.select("id", "name").from("users");
     * SqlBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.except(query2).build().query();
     * // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM customers
     * }</pre>
     *
     * @param sqlBuilder the SQL builder containing the query to except (must not be {@code null} and must not be this same instance)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder or {@code sqlBuilder} is closed; if a preceding qualified JOIN on this builder
     *         has not been completed with {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is
     *         in condition-only mode), its current SELECT segment has not been completed by {@code from(...)}, or ORDER BY,
     *         pagination, or FOR UPDATE has already been added; or if {@code sqlBuilder}'s statement is incomplete
     *         (see {@link #build()})
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         has generated parameter placeholders under a different SQL policy, requires explicit branch isolation,
     *         or does not build a complete, lexically SELECT-only SELECT query. Only the last check occurs after
     *         the child has been consumed by {@code build()}
     */
    public This except(final This sqlBuilder) {
        return appendSetOperation(_SPACE_EXCEPT_SPACE, sqlBuilder, "EXCEPT");
    }

    /**
     * Adds an EXCEPT clause with a SQL query string.
     * EXCEPT returns rows from the first query that don't appear in the second query.
     * This overload always treats its argument as a complete query; use {@link #exceptSelect(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
     * The query text is appended without adding branch isolation; callers must supply any grouping
     * required by their database for branch-local clauses or nested set operations. ORDER BY or pagination
     * intended for the combined result can be applied to this builder after the final set operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .except("SELECT id, name FROM inactive_users")
     *                 .build().query();
     * // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param query the complete {@code SELECT} sub-query to except (lexically SELECT-only; a syntactic check, not a read-only guarantee)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, or is not a complete, lexically SELECT-only {@code SELECT} sub-query
     */
    public This except(final String query) {
        return appendSetOperation(_SPACE_EXCEPT_SPACE, "EXCEPT", query);
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
     *                 .exceptSelect(columns)
     *                 .from("inactive_users")
     *                 .build().query();
     * // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the except query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This exceptSelect(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_EXCEPT_SPACE, propOrColumnNames);
    }

    /**
     * Adds a MINUS clause with another SQL query (Oracle syntax).
     * MINUS is Oracle's equivalent to EXCEPT - returns rows from the first query that don't appear in the second.
     * <p>The child is consumed once final rendering begins. Argument, parent-state, SQL-policy, and
     * branch-isolation checks run first; rejection by those checks leaves both builders reusable.
     * For ORDER BY or pagination of the combined result, apply those clauses to this builder after the
     * final set operation, leaving them off the child. To retain the child's own set operations, ORDER BY,
     * pagination, or FOR UPDATE, isolate it explicitly with {@code dsl.select("*").from(child, "branch")}.
     * Choose a projection and derived-table syntax supported by the target database. Compound children
     * are rejected uniformly; this method does not flatten their operators or add grouping implicitly.
     * Clauses supplied through raw SQL fragments remain the caller's responsibility.</p>
     * <p>For {@link SqlPolicy#NAMED_SQL}, child placeholders are rendered with this parent builder's
     * named-parameter handler so the compound statement uses one placeholder syntax.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlBuilder query1 = PSC.select("id", "name").from("users");
     * SqlBuilder query2 = PSC.select("id", "name").from("inactive_users");
     * String sql = query1.minus(query2).build().query();
     * // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param sqlBuilder the SQL builder containing the query to minus (must not be {@code null} and must not be this same instance)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder or {@code sqlBuilder} is closed; if a preceding qualified JOIN on this builder
     *         has not been completed with {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is
     *         in condition-only mode), its current SELECT segment has not been completed by {@code from(...)}, or ORDER BY,
     *         pagination, or FOR UPDATE has already been added; or if {@code sqlBuilder}'s statement is incomplete
     *         (see {@link #build()})
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         has generated parameter placeholders under a different SQL policy, requires explicit branch isolation,
     *         or does not build a complete, lexically SELECT-only SELECT query. Only the last check occurs after
     *         the child has been consumed by {@code build()}
     */
    public This minus(final This sqlBuilder) {
        return appendSetOperation(_SPACE_EXCEPT_MINUS_SPACE, sqlBuilder, "MINUS");
    }

    /**
     * Adds a MINUS clause with a SQL query string (Oracle syntax).
     * MINUS is Oracle's equivalent to EXCEPT - returns rows from the first query that don't appear in the second.
     * This overload always treats its argument as a complete query; use {@link #minusSelect(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
     * The query text is appended without adding branch isolation; callers must supply any grouping
     * required by their database for branch-local clauses or nested set operations. ORDER BY or pagination
     * intended for the combined result can be applied to this builder after the final set operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "name")
     *                 .from("users")
     *                 .minus("SELECT id, name FROM inactive_users")
     *                 .build().query();
     * // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param query the complete {@code SELECT} sub-query to subtract with MINUS (lexically SELECT-only; a syntactic check, not a read-only guarantee)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, or is not a complete, lexically SELECT-only {@code SELECT} sub-query
     */
    public This minus(final String query) {
        return appendSetOperation(_SPACE_EXCEPT_MINUS_SPACE, "MINUS", query);
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
     *                 .minusSelect(columns)
     *                 .from("inactive_users")
     *                 .build().query();
     * // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the minus query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed with
     *         {@code on(...)}/{@code using(...)}, this builder is not building a SELECT query (or is in condition-only mode),
     *         the current SELECT segment has not been completed by {@code from(...)}, or ORDER BY, pagination, or FOR UPDATE
     *         has already been added
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This minusSelect(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_EXCEPT_MINUS_SPACE, propOrColumnNames);
    }

    /**
     * Shared implementation for the raw-query set-operation overloads. It validates and appends
     * the complete right-hand {@code SELECT} query after resetting per-segment builder state.
     *
     * @param keyword the set-operation keyword token (e.g. {@link #_SPACE_UNION_SPACE})
     * @param operationName the set-operation SQL keyword (e.g. {@code "UNION ALL"}) used in validation messages
     * @param query the complete, lexically SELECT-only {@code SELECT} query to append
     * @return this builder instance for method chaining
     * @throws IllegalStateException if a set operation cannot be appended at the builder's current position
     *         (see {@link #checkCanAppendSetOperation(String)})
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, or is not a complete, lexically
     *         SELECT-only {@code SELECT} sub-query
     */
    private This appendSetOperation(final char[] keyword, final String operationName, final String query) {
        checkCanAppendSetOperation(operationName);
        checkSetOperationSubQuery(query, operationName);

        return appendCheckedSetOperation(keyword, query);
    }

    /**
     * Appends an already-validated complete right-hand query after resetting per-segment builder state.
     * Callers must have run {@link #checkSetOperationSubQuery} on this exact operand and
     * {@link #checkCanAppendSetOperation} at the builder's current structural position first.
     *
     * @param keyword the set-operation keyword token (e.g. {@link #_SPACE_UNION_SPACE})
     * @param query the complete, lexically SELECT-only {@code SELECT} query to append
     * @return this builder instance for method chaining
     */
    @SuppressWarnings("unchecked")
    private This appendCheckedSetOperation(final char[] keyword, final String query) {
        _op = OperationType.QUERY;

        closeSetOperationSegment();

        _sb.append(keyword).append(query);

        return (This) this;
    }

    /**
     * Discards the per-segment SELECT state once a complete set-operation operand has been emitted, so
     * that nothing from the finished left-hand SELECT leaks into the clauses that may still follow.
     *
     * <p>In particular the table alias must be dropped: a trailing {@code ORDER BY} after a set operation
     * sorts the combined result, where an alias-qualified column (e.g. {@code ORDER BY acc.first_name})
     * is rejected by PostgreSQL, MySQL, Oracle and SQL Server. Clearing {@code _selectModifier} and
     * {@code _selectKeywordEndIdx} likewise prevents a later {@code distinct()}/{@code selectModifier(...)}
     * from being spliced retro-actively into the already-finished left operand.</p>
     */
    private void closeSetOperationSegment() {
        _hasSetOperation = true;
        _propOrColumnNames = null;
        _propOrColumnNameAliases = null;
        _multiSelects = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;
        _selectModifier = null;
        _selectKeywordEndIdx = -1;
        _joinConditionAllowed = false;

        _hasCompletedSetOperation = true;
    }

    /**
     * Shared implementation for the collection set-operation starters
     * ({@code unionSelect}/{@code unionAllSelect}/{@code intersectSelect}/{@code exceptSelect}/{@code minusSelect}): resets the
     * builder to a fresh QUERY using the given columns and appends the set-operation keyword.
     *
     * @param keyword the set-operation keyword token (e.g. {@link #_SPACE_UNION_SPACE})
     * @param propOrColumnNames the columns of the following SELECT
     * @return this builder instance for method chaining
     * @throws IllegalStateException if this builder cannot accept a set operator at its current position
     *         (see {@link #checkCanAppendSetOperation(String)})
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    @SuppressWarnings("unchecked")
    private This appendSetOperation(final char[] keyword, final Collection<String> propOrColumnNames) {
        checkCanAppendSetOperation(new String(keyword).trim());
        final List<String> propOrColumnNamesSnapshot = copyAndValidateSqlFragments(propOrColumnNames, "propOrColumnNames");

        _op = OperationType.QUERY;

        _propOrColumnNames = propOrColumnNamesSnapshot;
        _propOrColumnNameAliases = null;
        _multiSelects = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;
        _selectModifier = null;
        _selectKeywordEndIdx = -1;
        _joinConditionAllowed = false;
        _hasCompletedSetOperation = false;

        // Record the compound query before from(...) completes its next SELECT. A sibling's isolation
        // check must reject it before build() would consume the unfinished child.
        _hasSetOperation = true;

        _sb.append(keyword);

        return (This) this;
    }

    /**
     * Shared implementation for the sibling-builder set-operation overloads: finalizes {@code sqlBuilder}
     * via {@link #build()}, validates the operand and this builder's structural position, rewrites any
     * colliding named parameters, and merges the child's parameters and named-parameter bookkeeping so the
     * placeholder sequence stays unique across the full compound query.
     *
     * @param keyword the set-operation keyword token (e.g. {@link #_SPACE_UNION_SPACE})
     * @param sqlBuilder the sibling builder supplying the right-hand query; consumed once prevalidation succeeds
     * @param operationName the set-operation SQL keyword (e.g. {@code "UNION"}) used in validation messages
     * @return this builder instance for method chaining
     * @throws IllegalStateException if this builder cannot accept a set operator at its current position
     *         (see {@link #checkCanAppendSetOperation(String)}), if {@code sqlBuilder} is closed or its
     *         statement is incomplete (see {@link #build()}), or if this builder's named-parameter handler
     *         emits an empty token while the child's placeholders are rewritten
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         requires explicit branch isolation, has generated parameter placeholders under a different SQL
     *         policy, or does not build a complete, lexically SELECT-only {@code SELECT} query
     */
    private This appendSetOperation(final char[] keyword, final This sqlBuilder, final String operationName) {
        checkCanAppendSetOperation(operationName);
        N.checkArgNotNull(sqlBuilder, cs.sqlBuilder);
        N.checkArgument(sqlBuilder != this, "Cannot apply " + operationName + " with the same SqlBuilder instance");
        // A closed child's retained clause/policy metadata must not mask its lifecycle error.
        sqlBuilder.assertNotClosed();
        checkSetOperationIsolation(sqlBuilder.requiresSetOperationIsolation(), operationName);
        N.checkArgument(_sqlPolicy == sqlBuilder._sqlPolicy || !sqlBuilder._hasGeneratedParameterPlaceholder,
                "A set-operation child with generated parameter placeholders must use the parent's SQL policy: parent=" + _sqlPolicy + ", child="
                        + sqlBuilder._sqlPolicy);
        final Map<String, Integer> parentOccurrences = N.isEmpty(_namedParameterNameOccurrences) ? Collections.emptyMap()
                : new HashMap<>(_namedParameterNameOccurrences);

        final SP sp = sqlBuilder.build();

        // Same validation as the single-String overloads: only a SELECT query is a valid set-operation
        // operand. Without this check, e.g. an UPDATE builder's SQL would be staged as a "column list"
        // and dropped, silently truncating the compound query.
        checkSetOperationSubQuery(sp.query(), operationName);

        final Set<String> childParameterNames = new HashSet<>(sqlBuilder._generatedNamedParameterNames);
        final Map<String, String> childParameterTokens = new HashMap<>(sqlBuilder._renderedNamedParameterTokens);
        final String sql = uniquifyChildNamedParameters(sp.query(), sqlBuilder._namedParameterNameOccurrences, parentOccurrences, childParameterNames,
                childParameterTokens, sqlBuilder._sqlPolicy);

        // A custom named-parameter handler can make the rewritten operand structurally different
        // from the child SQL validated above. Validate that final text before merging any child
        // metadata into the parent so a rejected operand leaves the parent reusable.
        if (!sql.equals(sp.query())) {
            checkSetOperationSubQuery(sql, operationName);
        }

        mergeNamedParameterOccurrences(sqlBuilder._namedParameterNameOccurrences);
        _generatedNamedParameterNames.addAll(childParameterNames);
        _renderedNamedParameterTokens.putAll(childParameterTokens);

        if (N.notEmpty(sp.parameters())) {
            _parameters.addAll(sp.parameters());
        }

        _hasGeneratedParameterPlaceholder |= sqlBuilder._hasGeneratedParameterPlaceholder;

        // Both the operand and its structural position were validated before parent state was changed.
        return appendCheckedSetOperation(keyword, sql);
    }

    /**
     * Returns whether a clause that applies to a completed result has already been emitted:
     * {@code ORDER BY}, pagination, or {@code FOR UPDATE}. Such a clause must follow the final
     * set-operation operand, which makes it both the reason a set operator can no longer be appended
     * here and the reason this statement cannot become somebody else's operand without isolation.
     *
     * @return {@code true} if one of those clauses has been emitted
     */
    private boolean hasCompoundResultClause() {
        return calledOpSet.contains(SK.ORDER_BY) || calledOpSet.contains(SK.LIMIT) || calledOpSet.contains(SK.OFFSET) || calledOpSet.contains(SK.FETCH_FIRST)
                || calledOpSet.contains(SK.FETCH_NEXT) || calledOpSet.contains(SK.FOR_UPDATE);
    }

    /**
     * Returns whether this complete statement must be isolated before use as one set-operation operand.
     * Only this statement's clauses count: nested condition subqueries and derived tables already have
     * their own parentheses, so their compound or terminal clauses do not escape into this statement.
     */
    final boolean requiresSetOperationIsolation() {
        return _hasSetOperation || hasCompoundResultClause();
    }

    /**
     * Rejects structured operands whose own compound or terminal clauses would escape into the enclosing query.
     *
     * @throws IllegalArgumentException if {@code subQuery} is a builder-backed snapshot or a structured
     *         condition that carries its own set operations, ORDER BY, pagination, or FOR UPDATE
     */
    private void checkSetOperationIsolation(final SubQuery subQuery, final String operationName) {
        if (subQuery instanceof final SubQuerySnapshot snapshot) {
            checkSetOperationIsolation(snapshot.requiresSetOperationIsolation, operationName);
        } else if (Strings.isEmpty(subQuery.rawSql())) {
            final Condition condition = subQuery.condition();

            if (condition instanceof final Criteria criteria) {
                checkSetOperationIsolation(criteria.orderBy() != null || criteria.limit() != null || N.notEmpty(criteria.setOperations()), operationName);
            } else if (condition != null) {
                final Operator operator = condition.operator();
                checkSetOperationIsolation(operator == Operator.ORDER_BY || operator == Operator.LIMIT || operator == Operator.OFFSET
                        || operator == Operator.FOR_UPDATE || isSetOperationOperator(operator), operationName);
            }
        }
    }

    /**
     * Explains combined-result versus child-local scope without moving clauses or flattening compound operands.
     *
     * @throws IllegalArgumentException if {@code required} is {@code true}
     */
    private static void checkSetOperationIsolation(final boolean required, final String operationName) {
        if (required) {
            // Even identical operators cannot always be flattened: A EXCEPT (B EXCEPT C) differs from
            // (A EXCEPT B) EXCEPT C. Require explicit scope for every structured compound child.
            throw new IllegalArgumentException(operationName + " requires explicit scope for a right-hand query with its own set operations, ORDER BY, "
                    + "pagination, or FOR UPDATE. For ORDER BY or pagination of the combined result, apply those clauses to the parent after the final "
                    + "set operation and leave them off the child. To preserve child-local scope, create an isolated right-hand query with "
                    + "select(...).from(sourceBuilder, alias) before composing it or creating its SubQuery snapshot; "
                    + "choose a projection and derived-table syntax supported by the target database.");
        }
    }

    /**
     * Verifies that a set operation has a complete query on its left-hand side. A staged SELECT list
     * is not a query until {@link #from(String)} renders it, and data-modification statements cannot
     * legally be used as the left operand of {@code UNION}, {@code INTERSECT}, {@code EXCEPT}, or
     * {@code MINUS}. A preceding set-operation operand supplied as a complete SQL string is considered
     * complete even though it did not call {@code from(...)} on this builder.
     * Terminal clauses that apply to a completed result ({@code ORDER BY}, pagination, and
     * {@code FOR UPDATE}) must be added after the final set operand; appending a set operator after
     * one of them would place the operator in an invalid position.
     *
     * @param operationName the public set-operation name used in the exception message
     * @throws IllegalStateException if this builder is closed, a preceding qualified JOIN has not been completed
     *         with {@code on(...)}/{@code using(...)}, the current operation is not a query (or this builder is in
     *         condition-only mode), the current SELECT segment has not been completed by {@code from(...)}, or
     *         {@code ORDER BY}, pagination, or {@code FOR UPDATE} has already been added
     */
    private void checkCanAppendSetOperation(final String operationName) {
        assertNotClosed();

        if (_joinConditionAllowed) {
            throw new IllegalStateException("The preceding qualified JOIN must be completed with on(...) or using(...) before " + operationName);
        }

        if (_op != OperationType.QUERY || _isForConditionOnly) {
            throw new IllegalStateException(operationName + " requires a complete SELECT query on its left-hand side");
        }

        final boolean completeLiteralOperand = !_sb.isEmpty() && N.isEmpty(_propOrColumnNames) && N.isEmpty(_propOrColumnNameAliases)
                && N.isEmpty(_multiSelects) && Strings.isEmpty(_selectModifier) && _selectKeywordEndIdx < 0;

        if (!_hasFromBeenSet && !completeLiteralOperand) {
            throw new IllegalStateException(operationName + " requires from(...) to complete the current SELECT segment first");
        }

        if (hasCompoundResultClause()) {
            throw new IllegalStateException(operationName + " must be added before ORDER BY, pagination, or FOR UPDATE clauses");
        }
    }

    /**
     * Rewrites and merges a previously built child SELECT into this builder. The caller's mutation
     * checkpoint, when needed, is responsible for rolling back these metadata changes together with
     * any surrounding SQL text.
     *
     * @throws IllegalArgumentException if the snapshot has generated parameter placeholders under a different
     *         SQL policy, or if its rewritten SQL is blank or is not a complete syntactic {@code SELECT} query candidate
     * @throws IllegalStateException if this builder's named-parameter handler emits an empty token while the
     *         snapshot's placeholders are rewritten
     */
    private String prepareSubQuerySnapshot(final SubQuerySnapshot subQuery) {
        N.checkArgument(_sqlPolicy == subQuery.sqlPolicy || !subQuery.hasGeneratedParameterPlaceholder,
                "A builder-backed subquery with generated parameter placeholders must use the parent's SQL policy: parent=" + _sqlPolicy + ", child="
                        + subQuery.sqlPolicy);

        final Map<String, Integer> parentOccurrences = N.isEmpty(_namedParameterNameOccurrences) ? Collections.emptyMap()
                : new HashMap<>(_namedParameterNameOccurrences);
        final Set<String> childParameterNames = new HashSet<>(subQuery.generatedNamedParameterNames);
        final Map<String, String> childParameterTokens = new HashMap<>(subQuery.renderedNamedParameterTokens);
        final String sql = uniquifyChildNamedParameters(subQuery.rawSql(), subQuery.namedParameterNameOccurrences, parentOccurrences, childParameterNames,
                childParameterTokens, subQuery.sqlPolicy);

        checkSubQuerySnapshot(sql);

        mergeNamedParameterOccurrences(subQuery.namedParameterNameOccurrences);
        _generatedNamedParameterNames.addAll(childParameterNames);
        _renderedNamedParameterTokens.putAll(childParameterTokens);

        if (N.notEmpty(subQuery.parameters)) {
            _parameters.addAll(subQuery.parameters);
        }

        _hasGeneratedParameterPlaceholder |= subQuery.hasGeneratedParameterPlaceholder;

        return sql;
    }

    /**
     * Appends a builder-backed condition subquery and merges its parameters and placeholder metadata.
     *
     * @throws IllegalArgumentException if the snapshot has generated parameter placeholders under a different
     *         SQL policy, or if its rewritten SQL is blank or is not a complete syntactic {@code SELECT} query candidate
     * @throws IllegalStateException if this builder's named-parameter handler emits an empty token while the
     *         snapshot's placeholders are rewritten
     */
    final void appendSubQuerySnapshot(final SubQuerySnapshot subQuery) {
        _sb.append(prepareSubQuerySnapshot(subQuery));
    }

    /**
     * Rewrites a child query's named parameters so they cannot collide with names already
     * generated on this (the parent) builder: colliding {@code <base>_<n>} names are shifted to unused
     * suffixes and the operand's placeholder tokens are replaced outside SQL quoted regions and comments.
     * {@code childParameterNames} and {@code childParameterTokens} are updated in place to the final names
     * and tokens, ready to be merged into the parent's bookkeeping.
     *
     * @param sql the operand query text built by the child builder
     * @param childOccurrences the child's named-parameter occurrence counts per base name
     * @param parentOccurrences the parent's occurrence counts snapshot taken before the child was built
     * @param childParameterNames the generated names emitted by the child; updated to the final names
     * @param childParameterTokens the rendered tokens per generated name; updated to the final tokens
     * @param childSqlPolicy the SQL policy of the child builder
     * @return the operand query with collision-free parameter names (may be the unchanged {@code sql})
     * @throws IllegalArgumentException if the child generated named parameters under a different SQL policy
     * @throws IllegalStateException under {@link SqlPolicy#NAMED_SQL} if this builder's named-parameter handler emits an empty token
     */
    private String uniquifyChildNamedParameters(final String sql, final Map<String, Integer> childOccurrences, final Map<String, Integer> parentOccurrences,
            final Set<String> childParameterNames, final Map<String, String> childParameterTokens, final SqlPolicy childSqlPolicy) {
        if (N.isEmpty(childOccurrences)) {
            return sql;
        }

        if (_sqlPolicy != childSqlPolicy) {
            throw new IllegalArgumentException(
                    "Child builders with generated named parameters must use the same SQL policy: parent=" + _sqlPolicy + ", child=" + childSqlPolicy);
        }

        if (_sqlPolicy != SqlPolicy.NAMED_SQL && _sqlPolicy != SqlPolicy.IBATIS_SQL) {
            return sql;
        }

        // The combined query executes on this (the parent) builder's target server: on SQL Server a
        // #name/##name temporary-table identifier is a data token, never a MySQL hash comment, so the
        // token-replacement scanners must not skip the rest of the line after one.
        final boolean sqlServerTempIdentifiers = _dialectFamily == DialectFamily.SQL_SERVER;

        String result = sql;

        for (final Map.Entry<String, Integer> entry : childOccurrences.entrySet()) {
            final String name = entry.getKey();
            final int parentCount = parentOccurrences.getOrDefault(name, 0);

            for (int i = entry.getValue(); i > 0; i--) {
                final String oldName = indexedNamedParameterName(name, i);

                if (!childParameterNames.contains(oldName)) {
                    continue; // this suffix was never emitted by the child (skipped due to a literal-name collision)
                }

                // Rename when the parent generated names from the same base (the child's suffixes must shift
                // past the parent's), or when this exact name collides with a parent name generated from a
                // DIFFERENT base (e.g. parent property "id_2" vs the child's "id" + "_2" suffix, or the
                // reverse) -- otherwise the same placeholder would be bound to two different values.
                final boolean rename = parentCount > 0 || _generatedNamedParameterNames.contains(oldName);
                String newName = oldName;

                if (rename) {
                    int suffix = parentCount + i;
                    newName = indexedNamedParameterName(name, suffix);

                    // Bump past names already taken on either side, e.g. a property literally named "<base>_<n>".
                    while (_generatedNamedParameterNames.contains(newName) || childParameterNames.contains(newName)) {
                        newName = indexedNamedParameterName(name, ++suffix);
                    }
                }

                if (_sqlPolicy == SqlPolicy.NAMED_SQL) {
                    final String oldToken = childParameterTokens.getOrDefault(oldName, ":" + oldName);
                    final String newToken = renderNamedParameterToken(_handlerForNamedParameter, newName);

                    // An identical token (no rename and the parent renders the same ":name" default) needs no
                    // rewrite: skip the full-SQL scan and only keep the token bookkeeping current.
                    if (!oldToken.equals(newToken)) {
                        if (oldToken.equals(":" + oldName)) {
                            result = replaceDefaultNamedParameterToken(result, oldName, newToken, sqlServerTempIdentifiers, _tokenizer);
                        } else {
                            result = replaceRenderedNamedParameterToken(result, oldToken, newToken, sqlServerTempIdentifiers, _tokenizer);
                        }
                    }

                    childParameterTokens.remove(oldName);
                    childParameterTokens.put(newName, newToken);
                } else if (rename) {
                    result = replaceIbatisParameterName(result, oldName, newName, sqlServerTempIdentifiers, _tokenizer);
                }

                if (rename) {
                    childParameterNames.remove(oldName);
                    childParameterNames.add(newName);
                }
            }
        }

        return result;
    }

    /**
     * Renders the placeholder token for a named parameter with the given handler.
     *
     * @param handler the named-parameter handler to render with
     * @param parameterName the parameter name to render
     * @return the rendered token (e.g. {@code ":name"})
     * @throws IllegalStateException if the handler emits an empty token
     */
    private static String renderNamedParameterToken(final BiConsumer<StringBuilder, String> handler, final String parameterName) {
        final StringBuilder sb = new StringBuilder(parameterName.length() + 8);
        handler.accept(sb, parameterName);

        if (sb.isEmpty()) {
            throw new IllegalStateException("The custom named-parameter handler emitted an empty token for: " + parameterName);
        }

        return sb.toString();
    }

    /**
     * Merges another builder's named-parameter occurrence counts into this builder's, summing the counts
     * per base name so names generated later stay unique across the compound statement.
     *
     * @param occurrences the occurrence counts to merge (may be {@code null} or empty)
     */
    private void mergeNamedParameterOccurrences(final Map<String, Integer> occurrences) {
        if (N.isEmpty(occurrences)) {
            return;
        }

        for (final Map.Entry<String, Integer> entry : occurrences.entrySet()) {
            _namedParameterNameOccurrences.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    /**
     * Returns the parameter name for the given occurrence of a base name: the base name itself for the
     * first occurrence, otherwise {@code <name>_<occurrence>}.
     *
     * @param name the base parameter name
     * @param occurrence the 1-based occurrence index
     * @return the indexed parameter name
     */
    private static String indexedNamedParameterName(final String name, final int occurrence) {
        return occurrence == 1 ? name : name + "_" + occurrence;
    }

    /** Replaces a default {@code :name} placeholder with an arbitrary rendered token. */
    private static String replaceDefaultNamedParameterToken(final String sql, final String oldName, final String newToken,
            final boolean sqlServerTempIdentifiers, final SqlParser.Tokenizer tokenizer) {
        StringBuilder sb = null;
        int last = 0;
        final int[] subscriptOpenings = ParsedSql.subscriptOpeningOffsets(sql, tokenizer);

        for (int i = 0, len = sql.length(); i < len; i++) {
            final int next = skipParameterQuotedOrComment(sql, i, sqlServerTempIdentifiers, subscriptOpenings);

            if (next != i) {
                i = next - 1;
                continue;
            }

            if (sql.charAt(i) != ':' || i + 1 >= len || !isSqlParameterNameChar(sql.charAt(i + 1))
                    || (i > 0 && (sql.charAt(i - 1) == ':' || isSqlParameterNameChar(sql.charAt(i - 1))))) {
                continue;
            }

            int end = i + 2;
            while (end < len && isSqlParameterNameChar(sql.charAt(end))) {
                end++;
            }

            if (sql.substring(i + 1, end).equals(oldName)) {
                if (sb == null) {
                    sb = new StringBuilder(sql.length() + Math.max(0, newToken.length() - oldName.length() - 1));
                }

                sb.append(sql, last, i).append(newToken);
                last = end;
            }

            i = end - 1;
        }

        return sb == null ? sql : sb.append(sql, last, sql.length()).toString();
    }

    /**
     * Replaces every iBATIS <code>#{oldName}</code> placeholder with <code>#{newName}</code> outside SQL
     * quoted regions and comments.
     *
     * @param sql the query text to rewrite
     * @param oldName the parameter name to replace
     * @param newName the replacement parameter name
     * @param sqlServerTempIdentifiers whether {@code #name}/{@code ##name} are SQL Server temporary-table
     *        identifiers (data tokens) rather than MySQL hash comments
     * @param tokenizer the tokenizer used to distinguish array subscripts from bracket-quoted identifiers
     * @return the rewritten query, or {@code sql} unchanged when no placeholder matches
     */
    private static String replaceIbatisParameterName(final String sql, final String oldName, final String newName, final boolean sqlServerTempIdentifiers,
            final SqlParser.Tokenizer tokenizer) {
        StringBuilder sb = null;
        int last = 0;
        final int[] subscriptOpenings = ParsedSql.subscriptOpeningOffsets(sql, tokenizer);

        for (int start = 0, len = sql.length(); start < len; start++) {
            final int next = skipParameterQuotedOrComment(sql, start, sqlServerTempIdentifiers, subscriptOpenings);

            if (next != start) {
                start = next - 1;
                continue;
            }

            if (sql.charAt(start) != '#' || start + 1 >= len || sql.charAt(start + 1) != '{') {
                continue;
            }

            final int end = sql.indexOf('}', start + 2);

            if (end < 0) {
                break;
            }

            if (sql.substring(start + 2, end).equals(oldName)) {
                if (sb == null) {
                    sb = new StringBuilder(sql.length() + Math.max(0, newName.length() - oldName.length()));
                }

                sb.append(sql, last, start + 2).append(newName);
                last = end;
            }

            start = end;
        }

        return sb == null ? sql : sb.append(sql, last, sql.length()).toString();
    }

    /** Replaces an exact custom placeholder token outside SQL quoted regions and comments. */
    private static String replaceRenderedNamedParameterToken(final String sql, final String oldToken, final String newToken,
            final boolean sqlServerTempIdentifiers, final SqlParser.Tokenizer tokenizer) {
        StringBuilder sb = null;
        int last = 0;
        final int tokenLength = oldToken.length();
        final int[] subscriptOpenings = ParsedSql.subscriptOpeningOffsets(sql, tokenizer);

        for (int i = 0, len = sql.length(); i <= len - tokenLength; i++) {
            final int next = skipParameterQuotedOrComment(sql, i, sqlServerTempIdentifiers, subscriptOpenings);

            if (next != i) {
                i = next - 1;
                continue;
            }

            if (!sql.startsWith(oldToken, i)) {
                continue;
            }

            final int end = i + tokenLength;
            final boolean hasNameCharBefore = isSqlParameterNameChar(oldToken.charAt(0)) && i > 0 && isSqlParameterNameChar(sql.charAt(i - 1));
            final boolean hasNameCharAfter = isSqlParameterNameChar(oldToken.charAt(tokenLength - 1)) && end < len && isSqlParameterNameChar(sql.charAt(end));

            if (hasNameCharBefore || hasNameCharAfter) {
                continue;
            }

            if (sb == null) {
                sb = new StringBuilder(sql.length() + Math.max(0, newToken.length() - oldToken.length()));
            }

            sb.append(sql, last, i).append(newToken);
            last = end;
            i = end - 1;
        }

        return sb == null ? sql : sb.append(sql, last, sql.length()).toString();
    }

    /**
     * Skips quoted text during parameter rewriting while leaving recognized array subscripts open
     * for scanning. Bracket-quoted identifiers remain opaque, as do strings and comments inside a subscript.
     */
    private static int skipParameterQuotedOrComment(final String sql, final int start, final boolean sqlServerTempIdentifiers, final int[] subscriptOpenings) {
        if (sql.charAt(start) == '[' && Arrays.binarySearch(subscriptOpenings, start) >= 0) {
            return start;
        }

        return skipSqlQuotedOrComment(sql, start, sqlServerTempIdentifiers);
    }

    /**
     * Returns the index just past the quoted region or comment starting at {@code start}, or {@code start}
     * itself when none starts there. Equivalent to {@code skipSqlQuotedOrComment(sql, start, false)}.
     *
     * @param sql the SQL text to scan
     * @param start the index to inspect
     * @return the end index of the quoted region or comment, or {@code start}
     */
    private static int skipSqlQuotedOrComment(final String sql, final int start) {
        return skipSqlQuotedOrComment(sql, start, false);
    }

    /**
     * Variant used by dialect-aware scanners: when {@code sqlServerTempIdentifiers} is {@code true}
     * (the builder's dialect family is SQL Server), a {@code #name}/{@code ##name} temporary-table
     * identifier is a data token rather than the start of a MySQL hash comment, so the scanner must
     * not skip the rest of the line. Without this, a set-operation operand such as
     * {@code "SELECT id FROM #tmp WHERE id = :id"} had everything after {@code #tmp} skipped and the
     * named-parameter uniquify pass silently left a colliding {@code :id} in place.
     *
     * @param sql the SQL text to scan
     * @param start the index to inspect
     * @param sqlServerTempIdentifiers whether {@code #name}/{@code ##name} are SQL Server temporary-table
     *        identifiers (data tokens) rather than MySQL hash comments
     * @return the index just past the quoted region or comment starting at {@code start}, or {@code start}
     *         itself when none starts there
     */
    private static int skipSqlQuotedOrComment(final String sql, final int start, final boolean sqlServerTempIdentifiers) {
        final int len = sql.length();
        final char ch = sql.charAt(start);

        if (ch == '\'' || ch == '"' || ch == '`') {
            for (int i = start + 1; i < len; i++) {
                final char current = sql.charAt(i);

                if (current == ch) {
                    // A doubled quote ('', "" or ``) is an escaped quote, not the terminator.
                    if (i + 1 < len && sql.charAt(i + 1) == ch) {
                        i++;
                        continue;
                    }

                    return i + 1;
                }

                // Backslash escapes apply only inside single-quoted string literals (MySQL semantics);
                // quoted identifiers ("..." / `...`) do not use backslash escaping.
                if (ch == '\'' && current == '\\' && i + 1 < len) {
                    i++;
                }
            }

            return len;
        }

        if (ch == '[') {
            for (int i = start + 1; i < len; i++) {
                if (sql.charAt(i) == ']') {
                    if (i + 1 < len && sql.charAt(i + 1) == ']') {
                        i++;
                    } else {
                        return i + 1;
                    }
                }
            }

            return len;
        }

        if (ch == '-' && start + 1 < len && sql.charAt(start + 1) == '-') {
            int i = start + 2;

            while (i < len && sql.charAt(i) != '\r' && sql.charAt(i) != '\n') {
                i++;
            }

            return i;
        }

        if (ch == '/' && start + 1 < len && sql.charAt(start + 1) == '*') {
            final int end = sql.indexOf("*/", start + 2);
            return end < 0 ? len : end + 2;
        }

        if (ch == '#' && !(sqlServerTempIdentifiers && isSqlServerTempIdentifierAt(sql, start)) && isAliasScannerHashCommentStart(sql, start)) {
            int i = start + 1;

            while (i < len && sql.charAt(i) != '\r' && sql.charAt(i) != '\n') {
                i++;
            }

            return i;
        }

        return start;
    }

    /** Reports whether {@code ch} can be part of a named-parameter name (letter, digit, {@code _}, dot, or non-ASCII). */
    private static boolean isSqlParameterNameChar(final char ch) {
        return ch == '_' || ch == '.' || (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || ch >= 128;
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
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE id = ? FOR UPDATE
     * }</pre>
     *
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, is not building a SELECT query (or is in
     *                               condition-only mode), the current SELECT segment has not been completed
     *                               by {@code from(...)}, {@code FOR UPDATE} has already been set on this
     *                               builder, or a preceding qualified JOIN has not been completed with
     *                               {@code on(...)}/{@code using(...)}
     */
    public This forUpdate() {
        checkIfAlreadyCalled(SK.FOR_UPDATE);

        _sb.append(_SPACE_FOR_UPDATE);

        return (This) this;
    }

    /**
     * Sets a single column or raw assignment expression for an UPDATE operation.
     *
     * <p>If {@code expr} contains an {@code =} sign, it is treated as a complete assignment and no
     * placeholder is generated (identifiers are still normalized according to the naming policy).
     * Otherwise, it is treated as a column name and a parameter placeholder
     * ({@code = ?}, {@code = :name}, or {@code = #{name}}) is appended based on the SQL policy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Raw assignment expression (already contains '='):
     * String sql = PSC.update("users")
     *                 .set("name = 'John'")
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * // Output: UPDATE users SET name = 'John' WHERE id = ?
     *
     * // Column name (placeholder is generated):
     * String sql2 = PSC.update("users")
     *                  .set("status")
     *                  .where(Filters.equal("id", 1))
     *                  .build().query();
     * // Output: UPDATE users SET status = ? WHERE id = ?
     * }</pre>
     *
     * @param expr a column name (placeholder will be appended) or a complete {@code col = value} assignment
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an {@code UPDATE},
     *                               or a post-SET clause such as {@code WHERE} has already been emitted
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or contains a SQL comment token
     */
    public This set(final String expr) {
        return set(Array.asList(expr));
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
     *                 .build().query();
     * // Output: UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to update
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an {@code UPDATE},
     *                               or a post-SET clause such as {@code WHERE} has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element, or an element containing a SQL comment token
     */
    public This set(final Collection<String> propOrColumnNames) {
        checkUpdateOperation();
        final List<String> propOrColumnNamesSnapshot = copyAndValidateSqlFragments(propOrColumnNames, "propOrColumnNames");

        return mutateAtomically(() -> appendSetColumns(propOrColumnNamesSnapshot));
    }

    /** Renders a validated SET column list after the public entry point has installed its checkpoint. */
    private void appendSetColumns(final List<String> propOrColumnNamesSnapshot) {
        init(false);

        // When set() is chained (called more than once), _sb already has SET content and
        // a comma must separate it from the next assignment list.
        final boolean needsLeadingComma = _setListStarted;
        _setListStarted = true;

        switch (_sqlPolicy) {
            case RAW_SQL:
            case PARAMETERIZED_SQL: {
                _hasGeneratedParameterPlaceholder = true;

                int i = needsLeadingComma ? 1 : 0;
                for (final String columnName : propOrColumnNamesSnapshot) {
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
                int i = needsLeadingComma ? 1 : 0;
                for (final String columnName : propOrColumnNamesSnapshot) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(columnName);

                    if (columnName.indexOf('=') < 0) {
                        _sb.append(" = ");

                        appendNamedParameter(nextNamedParameterName(columnName));
                    }
                }

                break;
            }

            case IBATIS_SQL: {
                int i = needsLeadingComma ? 1 : 0;
                for (final String columnName : propOrColumnNamesSnapshot) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(columnName);

                    if (columnName.indexOf('=') < 0) {
                        _sb.append(" = #{");
                        _sb.append(nextNamedParameterName(columnName));
                        _sb.append('}');
                    }
                }

                break;
            }

            default:
                throw new UnsupportedOperationException("SQL policy not supported: " + _sqlPolicy);
        }

        _propOrColumnNames = null;
    }

    /**
     * Performs the same pre-validation as {@link #set(Collection)} and renders the SET column list,
     * relying on the caller's already-installed mutation checkpoint instead of opening a second one.
     */
    private void validateAndAppendSetColumns(final Collection<String> propOrColumnNames) {
        checkUpdateOperation();
        final List<String> propOrColumnNamesSnapshot = copyAndValidateSqlFragments(propOrColumnNames, "propOrColumnNames");

        appendSetColumns(propOrColumnNamesSnapshot);
    }

    /**
     * Sets columns and values for UPDATE operation using a map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> values = new LinkedHashMap<>();
     * values.put("firstName", "John");
     * values.put("lastName", "Doe");
     * String sql = PSC.update("users")
     *                 .set(values)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * // Output: UPDATE users SET first_name = ?, last_name = ? WHERE id = ?
     * }</pre>
     *
     * @param props map of column names to values
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an {@code UPDATE},
     *                               or a post-SET clause such as {@code WHERE} has already been emitted
     * @throws IllegalArgumentException if {@code props} is {@code null} or empty, or contains a {@code null}, empty, or blank key, or a key containing a SQL comment token
     */
    public This set(final Map<String, Object> props) {
        checkUpdateOperation();
        final Map<String, Object> propsSnapshot = copyAndValidateSqlFragmentMap(props, "props");

        return mutateAtomically(() -> appendSetProperties(propsSnapshot));
    }

    /**
     * Sets one UPDATE column to a supplied value. Ordinary values are rendered according to this
     * builder's SQL policy (bound for parameterized/named policies and inlined for raw SQL), while
     * a {@link SqlExpression} is embedded as an expression.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PSC.update("users").set("status", "INACTIVE");
     * // UPDATE users SET status = ?
     *
     * PSC.update("users").set("loginCount", SqlExpression.of("login_count + 1"));
     * // UPDATE users SET login_count = login_count + 1
     * }</pre>
     *
     * @param propOrColumnName the property or column to assign
     * @param value the value to render according to the SQL policy, or a {@link SqlExpression} to embed
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an UPDATE, or a post-SET clause has already been emitted
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank, or contains a SQL comment token
     */
    public This set(final String propOrColumnName, final Object value) {
        return set(Collections.singletonMap(propOrColumnName, value));
    }

    /**
     * Sets two UPDATE columns to supplied values in argument order. Each ordinary value is rendered
     * according to this builder's SQL policy; a {@link SqlExpression} is embedded as an expression.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * PSC.update("users").set("firstName", "Ada", "status", "ACTIVE");
     * // UPDATE users SET first_name = ?, status = ?
     * }</pre>
     *
     * @param propOrColumnName1 the first property or column to assign
     * @param value1 the first value to render, or a {@link SqlExpression} to embed
     * @param propOrColumnName2 the second property or column to assign
     * @param value2 the second value to render, or a {@link SqlExpression} to embed
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an UPDATE, or a post-SET clause has already been emitted
     * @throws IllegalArgumentException if either property or column name is {@code null}, empty, or blank, contains a SQL
     *         comment token, or if the same name is given more than once
     */
    public This set(final String propOrColumnName1, final Object value1, final String propOrColumnName2, final Object value2) {
        checkUpdateOperation();
        checkNoDuplicateSetName(propOrColumnName1, propOrColumnName2);

        return set(N.asMap(propOrColumnName1, value1, propOrColumnName2, value2));
    }

    /**
     * Sets three UPDATE columns to supplied values in argument order. Each ordinary value is rendered
     * according to this builder's SQL policy; a {@link SqlExpression} is embedded as an expression.
     * For more assignments, use {@link #set(Map)} with a {@link LinkedHashMap} to preserve order.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * PSC.update("users").set("firstName", "Ada", "status", "ACTIVE",
     *         "updatedAt", SqlExpression.of("CURRENT_TIMESTAMP"));
     * // UPDATE users SET first_name = ?, status = ?, updated_at = CURRENT_TIMESTAMP
     * }</pre>
     *
     * @param propOrColumnName1 the first property or column to assign
     * @param value1 the first value to render, or a {@link SqlExpression} to embed
     * @param propOrColumnName2 the second property or column to assign
     * @param value2 the second value to render, or a {@link SqlExpression} to embed
     * @param propOrColumnName3 the third property or column to assign
     * @param value3 the third value to render, or a {@link SqlExpression} to embed
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an UPDATE, or a post-SET clause has already been emitted
     * @throws IllegalArgumentException if any property or column name is {@code null}, empty, or blank, contains a SQL
     *         comment token, or if the same name is given more than once
     */
    public This set(final String propOrColumnName1, final Object value1, final String propOrColumnName2, final Object value2, final String propOrColumnName3,
            final Object value3) {
        checkUpdateOperation();
        checkNoDuplicateSetName(propOrColumnName1, propOrColumnName2);
        checkNoDuplicateSetName(propOrColumnName1, propOrColumnName3);
        checkNoDuplicateSetName(propOrColumnName2, propOrColumnName3);

        return set(N.asMap(propOrColumnName1, value1, propOrColumnName2, value2, propOrColumnName3, value3));
    }

    /**
     * Rejects a repeated column name in the positional {@code set(name, value, name, value[, name, value])}
     * overloads, where a map-backed assignment list would otherwise silently collapse the duplicate.
     *
     * @param first one property or column name (blank names are skipped here and rejected, with the
     *        blank-name error, by the map overload)
     * @param second another property or column name
     * @throws IllegalArgumentException if {@code first} is non-blank and equal to {@code second}
     */
    private static void checkNoDuplicateSetName(final String first, final String second) {
        if (Strings.isNotBlank(first) && first.equals(second)) {
            throw new IllegalArgumentException("Duplicate property or column name in set(...): " + first);
        }
    }

    /** Renders a validated SET property map after the public entry point has installed its checkpoint. */
    private void appendSetProperties(final Map<String, Object> propsSnapshot) {
        init(false);

        // When set() is chained (called more than once), _sb already has SET content and
        // a comma must separate it from the next assignment list.
        final boolean needsLeadingComma = _setListStarted;
        _setListStarted = true;

        switch (_sqlPolicy) {
            case RAW_SQL: {
                int i = needsLeadingComma ? 1 : 0;
                for (final Map.Entry<String, Object> entry : propsSnapshot.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    _sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForRawSql(entry.getValue());
                }

                break;
            }

            case PARAMETERIZED_SQL: {
                int i = needsLeadingComma ? 1 : 0;
                for (final Map.Entry<String, Object> entry : propsSnapshot.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    _sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForParameterizedSql(entry.getValue());
                }

                break;
            }

            case NAMED_SQL: {
                int i = needsLeadingComma ? 1 : 0;
                for (final Map.Entry<String, Object> entry : propsSnapshot.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    _sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForNamedSql(entry.getKey(), entry.getValue());
                }

                break;
            }

            case IBATIS_SQL: {
                int i = needsLeadingComma ? 1 : 0;
                for (final Map.Entry<String, Object> entry : propsSnapshot.entrySet()) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(entry.getKey());

                    _sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForIbatisNamedSql(entry.getKey(), entry.getValue());
                }

                break;
            }

            default:
                throw new UnsupportedOperationException("SQL policy not supported: " + _sqlPolicy);
        }

        _propOrColumnNames = null;
    }

    /**
     * Performs the same pre-validation as {@link #set(Map)} and renders the SET property map,
     * relying on the caller's already-installed mutation checkpoint instead of opening a second one.
     */
    private void validateAndAppendSetProperties(final Map<String, Object> props) {
        checkUpdateOperation();
        final Map<String, Object> propsSnapshot = copyAndValidateSqlFragmentMap(props, "props");

        appendSetProperties(propsSnapshot);
    }

    /**
     * Sets properties to update from an entity object, a {@code Map}, or a single column-name {@code String}.
     * For bean entities, properties annotated as {@code @NonUpdatable}, {@code @ReadOnly}, {@code @ReadOnlyId},
     * or {@code @Transient} are automatically excluded.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * String sql = PSC.update("account")
     *                 .set(accountEntity)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * }</pre>
     *
     * @param entity the entity object, {@code Map<String, Object>}, or column-name {@code String} containing properties to set
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an {@code UPDATE},
     *                               or a post-SET clause such as {@code WHERE} has already been emitted
     * @throws IllegalArgumentException if {@code entity} is {@code null}; if {@code entity} is a {@code Collection} or array
     *         (use {@link #set(Collection)} for column lists); if a {@code String} {@code entity} is empty or blank or
     *         contains a SQL comment token; if a {@code Map} {@code entity} is empty or has a {@code null}, empty, or blank
     *         key or a key containing a SQL comment token; or if any other {@code entity} is not an instance of an entity
     *         bean class or its bean has no updatable property
     */
    public This set(final Object entity) {
        return set(entity, null);
    }

    /**
     * Sets properties to update from an entity object, a {@code Map}, or a single column-name {@code String},
     * excluding the specified properties. For bean entities, properties annotated as {@code @NonUpdatable},
     * {@code @ReadOnly}, {@code @ReadOnlyId}, or {@code @Transient} are also excluded.
     * Entity metadata selection, bean-property extraction, and SET rendering are atomic: if a getter or
     * parameter renderer fails, this builder is restored to the state it had before this call.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("createdDate", "version");
     * String sql = PSC.update("account")
     *                 .set(accountEntity, excluded)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * }</pre>
     *
     * @param entity the entity object, {@code Map<String, Object>}, or column-name {@code String} containing properties to set
     * @param excludedPropNames property names to exclude from the update (may be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an {@code UPDATE},
     *                               or a post-SET clause such as {@code WHERE} has already been emitted
     * @throws IllegalArgumentException if {@code entity} is {@code null}; if {@code entity} is a {@code Collection} or array
     *         (use {@link #set(Collection)} for column lists); if a {@code String} {@code entity} is empty or blank or
     *         contains a SQL comment token; if a {@code Map} {@code entity} has a {@code null}, empty, or blank key or a
     *         key containing a SQL comment token, or has no entry remaining after exclusions are applied; or if any other
     *         {@code entity} is not an instance of an entity bean class or its bean has no updatable property remaining
     *         after exclusions are applied
     */
    public This set(final Object entity, final Set<String> excludedPropNames) {
        checkUpdateOperation();
        N.checkArgNotNull(entity, cs.entity);

        return mutateAtomically(() -> {
            if (entity instanceof String) {
                validateAndAppendSetColumns(Array.asList((String) entity));
                return;
            }

            if (entity instanceof Map) {
                if (N.isEmpty(excludedPropNames)) {
                    validateAndAppendSetProperties((Map<String, Object>) entity);
                } else {
                    final Map<String, Object> localProps = new LinkedHashMap<>((Map<String, Object>) entity); //NOSONAR
                    Maps.removeKeys(localProps, excludedPropNames);
                    validateAndAppendSetProperties(localProps);
                }

                return;
            }

            if (entity instanceof Collection || entity.getClass().isArray()) {
                throw new IllegalArgumentException(
                        "A Collection or array is not a valid entity for set(Object, Set). Use set(Collection<String>) for column lists.");
            }

            final Class<?> entityClass = entity.getClass();
            final Collection<String> propNames = QueryUtil.updatePropNames(entityClass, excludedPropNames);
            N.checkArgNotEmpty(propNames, "No updatable properties remain after exclusions are applied");
            setEntityClass(entityClass);
            final Map<String, Object> localProps = N.newLinkedHashMap(propNames.size());

            for (final String propName : propNames) {
                localProps.put(propName, _entityInfo.getPropValue(entity, propName));
            }

            validateAndAppendSetProperties(localProps);
        });
    }

    /**
     * Sets all updatable properties from an entity class for UPDATE operation.
     * Properties marked with {@code @NonUpdatable}, {@code @ReadOnly}, {@code @ReadOnlyId}, or {@code @Transient} annotations are excluded.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * String sql = PSC.update("account")
     *                 .set(Account.class)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * }</pre>
     *
     * @param entityClass the entity class to get properties from
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an {@code UPDATE},
     *                               or a post-SET clause such as {@code WHERE} has already been emitted
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, is not an entity bean class, or declares no updatable property
     */
    public This set(final Class<?> entityClass) {
        return set(entityClass, (Set<String>) null);
    }

    /**
     * Ensures that a SET assignment API is used only in the SET-list portion of a live UPDATE builder.
     *
     * @throws IllegalStateException if this builder is closed, does not represent an {@code UPDATE},
     *         or a post-SET clause such as {@code WHERE} has already been emitted
     */
    private void checkUpdateOperation() {
        assertNotClosed();

        if (_op != OperationType.UPDATE) {
            throw new IllegalStateException("set() requires an UPDATE builder, but current operation is: " + _op);
        }

        final String laterClause = firstCalledClause(0);

        if (laterClause != null) {
            throw new IllegalStateException("set() must be called before the '" + laterClause + "' clause");
        }
    }

    /**
     * Sets updatable properties from an entity class for UPDATE operation, excluding specified properties.
     * Properties marked with {@code @NonUpdatable}, {@code @ReadOnly}, {@code @ReadOnlyId}, or
     * {@code @Transient} are automatically excluded. Entity metadata selection and SET rendering are
     * atomic: if parameter rendering fails, this builder is restored to its state before this call.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("lastUpdateTime");
     * String sql = PSC.update("account")
     *                 .set(Account.class, excluded)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * }</pre>
     *
     * @param entityClass the entity class to get properties from
     * @param excludedPropNames additional properties to exclude from the update
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if this builder is closed, does not represent an {@code UPDATE},
     *                               or a post-SET clause such as {@code WHERE} has already been emitted
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, is not an entity bean class, or no updatable property
     *         remains after exclusions are applied
     */
    public This set(final Class<?> entityClass, final Set<String> excludedPropNames) {
        checkUpdateOperation();
        N.checkArgNotNull(entityClass, cs.entityClass);
        final Collection<String> propNames = QueryUtil.updatePropNames(entityClass, excludedPropNames);
        N.checkArgNotEmpty(propNames, "No updatable properties remain after exclusions are applied");

        return mutateAtomically(() -> {
            setEntityClass(entityClass);
            validateAndAppendSetColumns(propNames);
        });
    }

    /**
     * Returns the list of parameter values accumulated so far for the generated SQL.
     * For parameterized SQL (using {@code ?}), this list contains the actual values in order.
     * For named SQL, this list contains the values corresponding to named parameters.
     *
     * <p>This is a live, unmodifiable view of the builder's internal parameter buffer; to retrieve the
     * finished SQL and parameters together (after which the builder is closed), use {@link #build()}.</p>
     *
     * @return an unmodifiable view of the parameter values
     */
    protected List<Object> parameters() {
        return Collections.unmodifiableList(_parameters);
    }

    /**
     * Generates the final SQL string and its parameters as an {@link SP} pair, then releases resources.
     * This is the canonical method for obtaining the SQL output from a builder.
     * Once final rendering begins, the builder is closed even if rendering fails. A rejected incomplete
     * qualified JOIN leaves the builder open so its {@code on(...)} or {@code using(...)} connector can be supplied.
     *
     * <p>To get just the SQL string, call {@code build().query()}. To get the parameter values,
     * call {@code build().parameters()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get SQL and parameters together
     * SP sqlPair = PSC.select("*")
     *                 .from("account")
     *                 .where(Filters.equal("status", "ACTIVE"))
     *                 .build();
     * // sqlPair.query() returns: "SELECT * FROM account WHERE status = ?"
     * // sqlPair.parameters() returns: ["ACTIVE"]
     *
     * // Get just the SQL string
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where(Filters.equal("status", "ACTIVE"))
     *                 .build()
     *                 .query();
     * // Output: SELECT * FROM users WHERE status = ?
     * }</pre>
     *
     * @return an SP (SQL-Parameters) pair containing the SQL string and parameter list
     * @throws IllegalStateException if this method is called after the builder has already been closed by a prior
     *         call to {@code build()}, or if the statement is incomplete (e.g. a query segment staged columns or a
     *         select modifier that no {@code from(...)} rendered, an INSERT has no target table, or an UPDATE has no
     *         SET columns, or a qualified JOIN was appended without a following {@code on(...)}/{@code using(...)})
     */
    public SP build() {
        assertNotClosed();

        if (_joinConditionAllowed) {
            throw new IllegalStateException("The statement ends with an incomplete qualified JOIN; call on(...) or using(...) before build()");
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

        if (logger.isDebugEnabled()) {
            logger.debug("Built SQL metadata. Operation: {}, policy: {}, length: {}, parameter count: {}", _op, _sqlPolicy, sql.length(), _parameters.size());
        }

        // SP's canonical constructor snapshots even a wrapped list, so a subclass retaining access to
        // this protected buffer cannot mutate an already built result.
        return new SP(sql, ImmutableList.wrap(_parameters));
    }

    /**
     * Applies a function to the SQL-Parameters pair and returns the result.
     * This is useful for executing the SQL directly with a data access framework.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Account> accounts = PSC.select("*")
     *     .from("account")
     *     .where(Filters.equal("status", "ACTIVE"))
     *     .apply(sp -> jdbcTemplate.query(sp.query(), accountRowMapper, sp.parameters().toArray()));
     * }</pre>
     *
     * @param <T> the return type of the function
     * @param <E> the exception type that may be thrown
     * @param function the function to apply to the SP pair; must not be {@code null}
     * @return the result of applying the function
     * @throws IllegalArgumentException if {@code function} is {@code null}
     * @throws IllegalStateException if this builder is closed or the statement is incomplete (see {@link #build()})
     * @throws E if the function throws an exception
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.Function<? super SP, T, E> function) throws IllegalArgumentException, E {
        N.checkArgNotNull(function, cs.function);

        return function.apply(build());
    }

    /**
     * Applies a bi-function to the SQL string and parameters separately and returns the result.
     * This is useful for executing the SQL directly with a data access framework that takes SQL and parameters separately.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> updates = new LinkedHashMap<>();
     * updates.put("status", "INACTIVE");
     *
     * int count = PSC.update("account")
     *     .set(updates)
     *     .where(Filters.lessThan("lastLogin", "2025-01-01"))
     *     .apply((sql, params) -> jdbcTemplate.update(sql, params.toArray()));
     * }</pre>
     *
     * @param <T> the return type of the function
     * @param <E> the exception type that may be thrown
     * @param function the bi-function to apply to the SQL and parameters; must not be {@code null}
     * @return the result of applying the function
     * @throws IllegalArgumentException if {@code function} is {@code null}
     * @throws IllegalStateException if this builder is closed or the statement is incomplete (see {@link #build()})
     * @throws E if the function throws an exception
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.BiFunction<? super String, ? super List<Object>, T, E> function)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(function, cs.function);

        final SP sP = build();

        return function.apply(sP.query, sP.parameters);
    }

    /**
     * Accepts a consumer for the SQL-Parameters pair.
     * This is useful for executing the SQL with a data access framework when no return value is needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> account = new LinkedHashMap<>();
     * account.put("name", "Alice");
     * account.put("email", "alice@example.com");
     * account.put("status", "ACTIVE");
     *
     * PSC.insert(account)
     *    .into("account")
     *    .accept(sp -> jdbcTemplate.update(sp.query(), sp.parameters().toArray()));
     * }</pre>
     *
     * @param <E> the exception type that may be thrown
     * @param consumer the consumer to accept the SP pair; must not be {@code null}
     * @throws IllegalArgumentException if {@code consumer} is {@code null}
     * @throws IllegalStateException if this builder is closed or the statement is incomplete (see {@link #build()})
     * @throws E if the consumer throws an exception
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.Consumer<? super SP, E> consumer) throws IllegalArgumentException, E {
        N.checkArgNotNull(consumer, cs.consumer);

        consumer.accept(build());
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
     *        logger.info("Executing SQL with {} parameters", params.size());
     *        jdbcTemplate.update(sql, params.toArray());
     *    });
     * }</pre>
     *
     * @param <E> the exception type that may be thrown
     * @param consumer the bi-consumer to accept the SQL and parameters; must not be {@code null}
     * @throws IllegalArgumentException if {@code consumer} is {@code null}
     * @throws IllegalStateException if this builder is closed or the statement is incomplete (see {@link #build()})
     * @throws E if the consumer throws an exception
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.BiConsumer<? super String, ? super List<Object>, E> consumer) throws IllegalArgumentException, E {
        N.checkArgNotNull(consumer, cs.consumer);

        final SP sP = build();

        consumer.accept(sP.query, sP.parameters);
    }

    /**
     * Finalizes and consumes this builder, validates that the resulting statement is a complete,
     * syntactic {@code SELECT} query candidate (as classified by {@code SqlParser.isSyntacticallyReadQuery}; a
     * syntactic check, not a read-only guarantee), and captures it for use as a condition or derived-table subquery.
     *
     * <p>{@link #build()} runs before SELECT validation, so once finalization begins this builder is
     * consumed even if the completed SQL is subsequently rejected. The returned snapshot receives the
     * parameter list already stabilized by {@link SP}; the source builder's generated-placeholder maps
     * and set are retained by reference as ownership-transferred metadata and must be treated as read-only.
     * Parent builders create working copies for collision rewriting, so the snapshot can be reused.</p>
     *
     * @return a reusable internal snapshot containing the SQL, parameters, SQL policy, and generated-placeholder metadata
     * @throws IllegalStateException if this builder is incomplete or was already consumed
     * @throws IllegalArgumentException if the built statement is blank or is not a complete syntactic {@code SELECT} query candidate
     */
    final SubQuerySnapshot buildSubQuery() {
        final SP sp = build();
        checkSubQuerySnapshot(sp.query());

        return new SubQuerySnapshot(sp.query(), sp.parameters(), _sqlPolicy, _hasGeneratedParameterPlaceholder, requiresSetOperationIsolation(),
                _namedParameterNameOccurrences, _generatedNamedParameterNames, _renderedNamedParameterTokens);
    }

    /**
     * Finalizes and consumes this builder, validates that the resulting statement is a complete,
     * syntactic {@code SELECT} query candidate (as classified by {@code SqlParser.isSyntacticallyReadQuery}; a
     * syntactic check, not a read-only guarantee), and captures it as a reusable {@link SubQuery}. The returned snapshot retains
     * the rendered SQL, parameter values, SQL policy, and generated-placeholder metadata so it can be
     * safely composed into an enclosing builder without losing binding order.
     *
     * <p>Finalization begins before SELECT validation, so this builder is consumed whether snapshot
     * creation succeeds or the built SQL is rejected. If the captured SQL contains generated
     * placeholders, an enclosing builder must use the same SQL policy; compatibility is checked when
     * the snapshot is composed.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * SubQuery paidOrders = PSC.select("userId")
     *     .from("orders")
     *     .where(Filters.gt("total", 100))
     *     .toSubQuery();
     *
     * SP result = PSC.select("*")
     *     .from("users")
     *     .where(Filters.in("id", paidOrders))
     *     .build();
     * }</pre>
     *
     * @return a reusable builder-backed subquery snapshot
     * @throws IllegalStateException if this builder is incomplete or was already consumed
     * @throws IllegalArgumentException if the built statement is blank or is not a complete syntactic {@code SELECT} query candidate
     */
    @Beta
    public SubQuery toSubQuery() {
        return buildSubQuery();
    }

    /**
     * Builds the SQL and prints the resulting query string to standard output.
     * This finalizes the builder (it cannot be reused after this call) and is intended for
     * debugging and development.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PSC.select("*")
     *    .from("account")
     *    .where(Filters.between("age", 18, 65))
     *    .debugPrint();
     * // Prints: SELECT * FROM account WHERE age BETWEEN ? AND ?
     * }</pre>
     *
     * @throws IllegalStateException if the builder has already been closed by a prior call to {@code build()}, or if the
     *         statement is incomplete (see {@link #build()})
     */
    @Beta
    public void debugPrint() {
        N.println(build().query());
    }

    /**
     * Ensures this builder is still open.
     *
     * @throws IllegalStateException if the builder has been closed by {@code build()} or a terminal helper
     */
    protected void assertNotClosed() {
        if (_sb == null) {
            throw new IllegalStateException("SqlBuilder is closed and cannot be reused after build() was called");
        }
    }

    /**
     * Lazily emits the leading SQL keyword for the current operation (UPDATE/DELETE) into the buffer
     * the first time it is required, and validates that {@code from()} has been called for QUERY operations.
     * Returns immediately if the buffer is already non-empty.
     *
     * @param setForUpdate when {@code true} and the current operation is UPDATE, the staged
     *                     {@code _propOrColumnNames} are emitted as a {@code SET col = ?} list.
     *                     When {@code false}, the SET list is left to the caller (typically
     *                     {@link #set(Collection)}). Has no effect for non-UPDATE operations.
     * @throws IllegalStateException if the operation is {@code QUERY} but {@code from(...)} has not
     *         been called and this builder is not in condition-only mode; if a query segment started
     *         by a set operation or {@code INSERT ... SELECT} staged columns (or a select modifier)
     *         that no {@code from(...)} ever rendered; if {@code setForUpdate} is {@code true} for
     *         an UPDATE with no columns staged and no prior {@code set(...)} call; if the operation is
     *         INSERT and no {@code into(...)} target table has been set; or if this builder is closed
     */
    protected void init(final boolean setForUpdate) {
        assertNotClosed();

        // Note: any change, please take a look at: Dsl.renderCondition(final Condition cond, final Class<?> entityClass) first.

        if (_op == OperationType.ADD && Strings.isEmpty(_tableName)) {
            throw new IllegalStateException("into() must be called to specify the target table before building an INSERT statement");
        }

        if (!_sb.isEmpty()) {
            if (_op == OperationType.UPDATE && setForUpdate && !_setListStarted) {
                throw new IllegalStateException("set() must be called to specify the columns to update before building an UPDATE statement");
            }

            if (_op == OperationType.QUERY && !_hasFromBeenSet && !_isForConditionOnly) {
                // A set operation (union/intersect/...) stages new columns and resets _hasFromBeenSet, and an
                // INSERT ... SELECT started by into() likewise still needs its FROM. If the follow-up from(...)
                // never happens, the staged columns would otherwise be dropped silently here, emitting truncated
                // SQL such as "SELECT id FROM t UNION " or "INSERT INTO account (first_name)". The columns may
                // be staged in any of the three select forms: plain names, name-alias map, or multi-selects.
                if (N.notEmpty(_propOrColumnNames) || N.notEmpty(_propOrColumnNameAliases) || N.notEmpty(_multiSelects)) {
                    throw new IllegalStateException("from() must be called to complete the current query segment "
                            + "(a set operation or INSERT ... SELECT was started but no from(...) followed)");
                }

                // A select modifier staged after a verbatim set-operation sub-query (e.g. union("SELECT ..."))
                // has no pending SELECT to attach to (_selectKeywordEndIdx < 0) and no from(...) can legally
                // follow, so it would be dropped silently.
                if (Strings.isNotEmpty(_selectModifier) && _selectKeywordEndIdx < 0) {
                    throw new IllegalStateException("A select modifier ('" + _selectModifier
                            + "') was set but there is no SELECT segment to apply it to (the preceding set operation appended a complete sub-query)");
                }
            }

            return;
        }

        if (_op == OperationType.UPDATE) {
            // Finalizing an UPDATE (setForUpdate == true: called from build()/where()/append(...)) with no
            // columns staged by update(Class, ...) and no prior set(...) call would emit an empty SET list
            // ("UPDATE t SET  WHERE ..."), so fail fast instead. set(...) itself initializes with
            // setForUpdate == false and then writes the assignments.
            if (setForUpdate && !_setListStarted && N.isEmpty(_propOrColumnNames)) {
                throw new IllegalStateException("set() must be called to specify the columns to update before building an UPDATE statement");
            }

            _sb.append(_UPDATE);

            _sb.append(_SPACE);
            _sb.append(_tableName);

            _sb.append(_SPACE_SET_SPACE);

            if (setForUpdate && N.notEmpty(_propOrColumnNames)) {
                set(_propOrColumnNames);
            }
        } else if (_op == OperationType.DELETE) {
            _sb.append(_DELETE);
            _sb.append(_SPACE);
            _sb.append(_FROM);
            _sb.append(_SPACE);
            _sb.append(_tableName);
        } else if (_op == OperationType.QUERY && !_hasFromBeenSet && !_isForConditionOnly) {
            throw new IllegalStateException("from() method must be called before building query for operation: " + _op);
        }
    }

    /**
     * Sets the entity class for this query builder and initializes the associated bean info
     * and property-to-column-name mapping if the class is a bean class.
     *
     * @param entityClass the entity class to set
     */
    protected void setEntityClass(final Class<?> entityClass) {
        _entityClass = entityClass;

        if (Beans.isBeanClass(entityClass)) {
            _entityInfo = ParserUtil.getBeanInfo(entityClass);
            _propColumnNameMap = propToColumnInfoMap(entityClass, _namingPolicy);
        } else {
            _entityInfo = null;
            _propColumnNameMap = null;
        }
    }

    /**
     * Sets the parameter for raw SQL (inlines the value directly into the SQL string).
     *
     * @param propValue the value to render into the SQL string
     * @throws IllegalArgumentException if {@code propValue} is a {@code Float} or {@code Double} that is {@code NaN} or
     *         infinite, or another {@code Number} whose text is not a valid numeric literal
     */
    protected void setParameterForRawSql(final Object propValue) {
        // These final JDK wrapper types always produce valid decimal literals. Append their
        // primitive values directly; arbitrary Number implementations still need validation.
        if (propValue instanceof Integer || propValue instanceof Short || propValue instanceof Byte) {
            _sb.append(((Number) propValue).intValue());
        } else if (propValue instanceof Long) {
            _sb.append(((Long) propValue).longValue());
        } else if (Filters.QME.equals(propValue)) {
            _hasGeneratedParameterPlaceholder = true;
            _sb.append(SK._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendConditionAsParameter((Condition) propValue);
        } else {
            _sb.append(SqlExpression.renderValue(propValue));
        }
    }

    /**
     * Sets the parameter for parameterized SQL (uses '?' placeholder and adds value to parameter list).
     *
     * @param propValue the value to bind to the {@code ?} placeholder
     */
    protected void setParameterForParameterizedSql(final Object propValue) {
        if (Filters.QME.equals(propValue)) {
            _hasGeneratedParameterPlaceholder = true;
            _sb.append(SK._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendConditionAsParameter((Condition) propValue);
        } else {
            _hasGeneratedParameterPlaceholder = true;
            _sb.append(SK._QUESTION_MARK);

            _parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter for named SQL.
     *
     * @param propName the property or parameter name for the named SQL placeholder
     * @param propValue the value to bind to the named parameter
     * @throws IllegalStateException if the named-parameter handler emits an empty token
     */
    protected void setParameterForNamedSql(final String propName, final Object propValue) {
        if (Filters.QME.equals(propValue)) {
            final String namedPropName = nextNamedParameterName(propName);
            appendNamedParameter(namedPropName);
        } else if (propValue instanceof Condition) {
            appendConditionAsParameter((Condition) propValue);
        } else {
            final String namedPropName = nextNamedParameterName(propName);
            appendNamedParameter(namedPropName);

            _parameters.add(propValue);
        }
    }

    /**
     * Renders a named-parameter token into the SQL buffer with this builder's handler and records the
     * rendered token for set-operation collision handling.
     *
     * @param parameterName the parameter name to emit
     * @throws IllegalStateException if the handler emits an empty token
     */
    private void appendNamedParameter(final String parameterName) {
        final int start = _sb.length();
        _handlerForNamedParameter.accept(_sb, parameterName);

        if (_sb.length() == start) {
            throw new IllegalStateException("The named-parameter handler emitted an empty token for: " + parameterName);
        }

        _renderedNamedParameterTokens.put(parameterName, _sb.substring(start));
    }

    /** Base name for the generated parameter names that replace the positional {@code ?} of a bound raw sub-query. */
    private static final String RAW_SUB_QUERY_PARAMETER_NAME = "param";

    /**
     * Prepares the SQL text of a raw sub-query ({@code SubQuery(String, Collection)}) for embedding into
     * this builder's statement. Under {@code PARAMETERIZED_SQL} the text is returned unchanged and its
     * positional {@code ?} placeholders line up with {@code rawParameters}, which the caller binds. Under
     * {@code NAMED_SQL} and {@code IBATIS_SQL} a raw sub-query with bindings would otherwise leave
     * {@code ?} placeholders inside a {@code :name}/{@code #{name}} statement, which named-parameter
     * consumers such as {@code ParsedSql} reject ("Cannot mix parameter styles"). So each positional
     * {@code ?} (including array-subscript bindings, but excluding quoted text, comments and JSON operators) is rewritten, in order,
     * to the next collision-safe generated name ({@code :param}, {@code :param_2}, ... or
     * {@code #{param}}, ...) exactly as structured conditions are named, and the caller binds
     * {@code rawParameters} in that same order. Under {@code RAW_SQL} each positional {@code ?} is instead
     * replaced, in order, by the literal rendering of its binding ({@code SqlExpression.renderValue}: the
     * same rendering a structured condition's value receives, so strings are quoted and escaped, numbers and
     * booleans are emitted verbatim, {@code null} becomes the {@code null} literal and a {@link SqlExpression}
     * is embedded verbatim) and the caller must NOT bind {@code rawParameters}; the result carries no
     * placeholders.
     *
     * <p>The placeholders are located by {@code ParsedSql}, the same classifier {@code SubQuery(String, Collection)}
     * validated the binding count with, so exactly the {@code ?} markers it counted are rewritten and nothing
     * else: a {@code ?} inside a quoted literal, a (bracket-)quoted identifier or a comment, and a PostgreSQL
     * JSON {@code ?} operator ({@code doc ? 'key'}), stay verbatim, while a {@code ?} inside an array subscript
     * ({@code arr[?]}) is a placeholder. Should the number of placeholders nevertheless differ from the number
     * of bindings, the rewrite is refused with a pointed {@link IllegalArgumentException} instead of binding
     * values to the wrong placeholders.</p>
     *
     * @param rawSql the verbatim SQL text of the raw sub-query (never {@code null} or empty)
     * @param rawParameters the positional bindings of the raw sub-query (may be empty)
     * @return the text to append: {@code rawSql} itself, the copy with its {@code ?} placeholders renamed, or
     *         (under {@code RAW_SQL}) the copy with its {@code ?} placeholders replaced by literals
     * @throws IllegalArgumentException if the SQL policy is {@code NAMED_SQL}/{@code IBATIS_SQL}/{@code RAW_SQL},
     *         the sub-query has bindings, and the number of positional {@code ?} placeholders differs from the
     *         number of bindings; or, under {@code RAW_SQL}, if a binding is a {@code Float} or {@code Double} that is
     *         {@code NaN} or infinite, or another {@code Number} whose text is not a valid numeric literal
     * @throws IllegalStateException if the parsed text of the sub-query does not match its raw text, or if a recorded
     *         placeholder offset does not point at a {@code ?} in the raw text (both defensive; neither is expected
     *         for {@link ParsedSql} output); or, under {@code NAMED_SQL}, if the named-parameter handler emits an empty token
     */
    protected final String renameRawSubQueryPlaceholders(final String rawSql, final List<Object> rawParameters) {
        if (N.isEmpty(rawParameters) || _sqlPolicy == SqlPolicy.PARAMETERIZED_SQL) {
            return rawSql;
        }

        final boolean inlineAsLiterals = _sqlPolicy == SqlPolicy.RAW_SQL;
        final int bindingCount = rawParameters.size();

        // ParsedSql is the single source of truth for which '?' are placeholders (SubQuery(String, Collection)
        // validated the binding count against it). Its offsets refer to the trimmed text, so the leading
        // whitespace String.trim() drops from rawSql is added back below.
        final ParsedSql parsedSql = ParsedSql.parse(rawSql);
        final int[] placeholderOffsets = parsedSql.positionalParameterOffsets();
        final int placeholderCount = placeholderOffsets.length;

        // Checked before any parameter name is generated (leaves the builder's naming state untouched).
        if (placeholderCount != bindingCount) {
            throw new IllegalArgumentException("Cannot rewrite the positional '?' placeholders of a raw sub-query to "
                    + (inlineAsLiterals ? "inline " + SqlPolicy.RAW_SQL + " literals" : _sqlPolicy + " named parameters") + ": found " + placeholderCount
                    + " positional '?' placeholder(s) but " + bindingCount + " binding(s): " + rawSql);
        }

        int leadingWhitespace = 0;

        while (leadingWhitespace < rawSql.length() && rawSql.charAt(leadingWhitespace) <= ' ') {
            leadingWhitespace++;
        }

        if (!rawSql.startsWith(parsedSql.originalSql(), leadingWhitespace)) {
            throw new IllegalStateException("The parsed text of a raw sub-query does not match its raw text: " + rawSql);
        }

        final StringBuilder sb = new StringBuilder(rawSql.length() + bindingCount * 8);
        int last = 0;
        int bindingIndex = 0;

        for (final int placeholderOffset : placeholderOffsets) {
            final int i = leadingWhitespace + placeholderOffset;

            if (rawSql.charAt(i) != '?') {
                throw new IllegalStateException("No positional '?' placeholder at offset " + i + " of the raw sub-query: " + rawSql);
            }

            sb.append(rawSql, last, i);

            // A placeholder glued to a preceding identifier character ("AND?=b") must not be rewritten into
            // "AND:param_2" / "AND2": separate the replacement from the identifier with a space.
            if (i > 0 && isIdentifierGlueChar(rawSql.charAt(i - 1))) {
                sb.append(' ');
            }

            if (inlineAsLiterals) {
                // Same literal rendering as setParameterForRawSql applies to a structured condition's value:
                // strings quoted/escaped, numbers/booleans verbatim, null as NULL, SqlExpression embedded verbatim.
                sb.append(SqlExpression.renderValue(rawParameters.get(bindingIndex++)));
            } else {
                final String parameterName = nextNamedParameterName(RAW_SUB_QUERY_PARAMETER_NAME);

                if (_sqlPolicy == SqlPolicy.NAMED_SQL) {
                    final String token = renderNamedParameterToken(_handlerForNamedParameter, parameterName);
                    _renderedNamedParameterTokens.put(parameterName, token);
                    sb.append(token);
                } else {
                    sb.append("#{").append(parameterName).append('}');
                }
            }

            last = i + 1;

            // A placeholder glued to a following identifier character ("a=?AND b=?") must not be rewritten into
            // ":paramAND" / "1AND": separate the replacement from the identifier with a space. Same helper as the
            // leading side above, so the two tests cannot drift apart.
            if (last < rawSql.length() && isIdentifierGlueChar(rawSql.charAt(last))) {
                sb.append(' ');
            }
        }

        return sb.append(rawSql, last, rawSql.length()).toString();
    }

    /**
     * Returns {@code true} for a character that would fuse with an adjacent SQL token into one identifier or
     * numeric literal (letters, digits, {@code _}, {@code $} and {@code .}); used to keep rewritten placeholders
     * separated from their neighbours.
     *
     * <p>{@code .} is included because it fuses in all three rewrite modes: the raw text {@code "a=?.5"} bound to
     * {@code 1} would inline as the literal {@code 1.5} (valid SQL carrying the WRONG value) and render as
     * {@code :param.5} / {@code #{param}.5}, and {@code "a=?.x"} would re-parse as the parameter <i>name</i>
     * {@code param.x}, because {@link ParsedSql} accepts dotted parameter names. {@code $} is kept although
     * {@code ParsedSql} never accepts it inside a name, since it is an identifier character in Oracle/MySQL and
     * would still fuse with an inlined literal.</p>
     */
    private static boolean isIdentifierGlueChar(final char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$' || ch == '.';
    }

    /**
     * Returns {@code true} when a select item ends with {@code *} (ignoring trailing whitespace), which is how a
     * wildcard select item - {@code *} or {@code alias.*} - is recognised; a wildcard can never carry a column
     * alias. The test is the plain trailing character, so any other item ending in {@code *} (e.g. {@code "qty*"})
     * is treated the same way and loses its automatic alias; such an item is not valid SQL to begin with.
     */
    private static boolean endsWithWildcard(final String propName) {
        int end = propName.length();

        while (end > 0 && Character.isWhitespace(propName.charAt(end - 1))) {
            end--;
        }

        return end > 0 && propName.charAt(end - 1) == '*';
    }

    /**
     * Sets the parameter for ibatis named SQL.
     *
     * @param propName the property or parameter name for the ibatis named SQL placeholder
     * @param propValue the value to bind to the ibatis named parameter
     */
    protected void setParameterForIbatisNamedSql(final String propName, final Object propValue) {
        if (Filters.QME.equals(propValue)) {
            final String namedPropName = nextNamedParameterName(propName);
            _sb.append("#{");
            _sb.append(namedPropName);
            _sb.append('}');
        } else if (propValue instanceof Condition) {
            appendConditionAsParameter((Condition) propValue);
        } else {
            final String namedPropName = nextNamedParameterName(propName);
            _sb.append("#{");
            _sb.append(namedPropName);
            _sb.append('}');

            _parameters.add(propValue);
        }
    }

    /**
     * Generates the next unique named parameter name for the given property name.
     * The source text is normalized so that names like {@code "u.id"}, {@code "ord.orderDate"},
     * or {@code "COUNT(*)"} produce SQL-compatible placeholders (e.g. {@code :id},
     * {@code :orderDate}, {@code :COUNT}) rather than placeholders with punctuation that most
     * named-parameter parsers (Spring, MyBatis, etc.) reject.
     * On the first occurrence of a simple name, returns it as-is. On subsequent occurrences,
     * appends a numeric suffix (e.g., {@code "propName_2"}, {@code "propName_3"}). A candidate that
     * is already taken — e.g. a generated {@code "id_2"} when a property is literally named
     * {@code "id_2"} — is skipped, so no two placeholders in the SQL ever share a name.
     *
     * @param propName the property name to generate a parameter name for
     * @return the unique named parameter name
     */
    protected String nextNamedParameterName(final String propName) {
        _hasGeneratedParameterPlaceholder = true;

        final String sanitized = sanitizeNamedParameterName(propName);
        int occurrence = _namedParameterNameOccurrences.compute(sanitized, (k, v) -> v == null ? 1 : v + 1);
        String result = indexedNamedParameterName(sanitized, occurrence);

        while (!_generatedNamedParameterNames.add(result)) {
            occurrence = _namedParameterNameOccurrences.compute(sanitized, (k, v) -> v == null ? 1 : v + 1);
            result = indexedNamedParameterName(sanitized, occurrence);
        }

        return result;
    }

    /**
     * Seeds a nested or sibling builder with this builder's named-parameter occurrence counts so the
     * child cannot generate placeholders that already exist in the surrounding SQL.
     *
     * @param childBuilder the nested or sibling builder to seed; must not be {@code null}
     * @throws IllegalArgumentException if {@code childBuilder} is {@code null}
     */
    protected final void seedNamedParameterOccurrences(final AbstractQueryBuilder<?> childBuilder) {
        N.checkArgNotNull(childBuilder, cs.childBuilder);

        if (N.isEmpty(_namedParameterNameOccurrences)) {
            return;
        }

        for (final Map.Entry<String, Integer> entry : _namedParameterNameOccurrences.entrySet()) {
            childBuilder._namedParameterNameOccurrences.merge(entry.getKey(), entry.getValue(), Math::max);
        }

        childBuilder._generatedNamedParameterNames.addAll(_generatedNamedParameterNames);
        childBuilder._renderedNamedParameterTokens.putAll(_renderedNamedParameterTokens);
    }

    /**
     * Copies back named-parameter occurrence counts after a nested or sibling builder has rendered.
     *
     * @param childBuilder the nested or sibling builder whose occurrence counts to adopt; must not be {@code null}
     * @throws IllegalArgumentException if {@code childBuilder} is {@code null} (checked before any state of this builder is changed)
     */
    protected final void adoptNamedParameterOccurrences(final AbstractQueryBuilder<?> childBuilder) {
        N.checkArgNotNull(childBuilder, cs.childBuilder);

        _namedParameterNameOccurrences.clear();
        _namedParameterNameOccurrences.putAll(childBuilder._namedParameterNameOccurrences);

        _generatedNamedParameterNames.clear();
        _generatedNamedParameterNames.addAll(childBuilder._generatedNamedParameterNames);

        _renderedNamedParameterTokens.clear();
        _renderedNamedParameterTokens.putAll(childBuilder._renderedNamedParameterTokens);

        // A structured sub-query is rendered by a child builder and then embedded in this builder.
        // Preserve its placeholder state so a later sibling set operation cannot combine the resulting
        // query with an incompatible parameter policy.
        _hasGeneratedParameterPlaceholder |= childBuilder._hasGeneratedParameterPlaceholder;
    }

    /**
     * Normalizes {@code propName} so the remaining text is a valid named-parameter identifier.
     * {@code "u.id"} becomes {@code "id"}, {@code "COUNT(*)"} becomes {@code "COUNT"}, and
     * invalid identifier characters are converted to underscores. {@code null} and empty strings
     * are returned unchanged. Runs of invalid identifier characters collapse into a single underscore,
     * leading invalid characters are dropped and trailing underscores trimmed; a name that starts with
     * a digit is prefixed with {@code p} (e.g. {@code "1abc"} becomes {@code "p1abc"}), and a name with
     * no usable characters becomes {@code "param"}.
     *
     * @param propName the property name (may include table-alias prefix)
     * @return the sanitized identifier suitable for use after {@code :} or inside {@code #{}}
     */
    protected static String sanitizeNamedParameterName(final String propName) {
        if (propName == null || propName.isEmpty()) {
            return propName;
        }

        String name = propName.trim();
        final int parenthesisIdx = name.indexOf('(');

        if (parenthesisIdx > 0) {
            name = name.substring(0, parenthesisIdx).trim();
        } else {
            final int dotIdx = name.lastIndexOf('.');

            if (dotIdx >= 0 && dotIdx < name.length() - 1) {
                name = name.substring(dotIdx + 1);
            }
        }

        // Most names are already valid. Preserve Unicode letter/digit semantics and the
        // historical removal of trailing underscores before reusing the original text.
        if (!name.isEmpty() && !Character.isDigit(name.charAt(0)) && name.charAt(name.length() - 1) != '_') {
            int i = 0;
            for (; i < name.length(); i++) {
                final char ch = name.charAt(i);
                if (!(Character.isLetterOrDigit(ch) || ch == '_')) {
                    break;
                }
            }
            if (i == name.length()) {
                return name;
            }
        }

        final StringBuilder sb = new StringBuilder(name.length());

        for (int i = 0, len = name.length(); i < len; i++) {
            final char ch = name.charAt(i);

            if (Character.isLetterOrDigit(ch) || ch == '_') {
                sb.append(ch);
            } else if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '_') {
                sb.append('_');
            }
        }

        while (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '_') {
            sb.setLength(sb.length() - 1);
        }

        if (sb.isEmpty()) {
            return "param";
        }

        if (Character.isDigit(sb.charAt(0))) {
            sb.insert(0, 'p');
        }

        return sb.toString();
    }

    /**
     * Sets the parameter based on the current SQL policy.
     *
     * @param propName the property or parameter name
     * @param propValue the value to bind to the parameter
     * @throws IllegalArgumentException under {@link SqlPolicy#RAW_SQL} if {@code propValue} is a {@code Float} or
     *         {@code Double} that is {@code NaN} or infinite, or another {@code Number} whose text is not a valid numeric literal
     * @throws IllegalStateException under {@link SqlPolicy#NAMED_SQL} if the named-parameter handler emits an empty token
     */
    protected void setParameter(final String propName, final Object propValue) {
        appendParameter(propName, propValue);
    }

    // INSERT values share policy dispatch without newly invoking the overridable generic
    // setParameter hook; the existing policy-specific setter hooks remain in use.
    private void appendParameter(final String propName, final Object propValue) {
        switch (_sqlPolicy) {
            case RAW_SQL -> setParameterForRawSql(propValue);
            case PARAMETERIZED_SQL -> setParameterForParameterizedSql(propValue);
            case NAMED_SQL -> setParameterForNamedSql(propName, propValue);
            case IBATIS_SQL -> setParameterForIbatisNamedSql(propName, propValue);
            default -> throw new UnsupportedOperationException("SQL policy not supported: " + _sqlPolicy);
        }
    }

    /**
     * Appends the values for an INSERT operation, in the iteration order of the map's key set.
     *
     * @param props a map of property names to values to be inserted; must not be {@code null}
     * @throws IllegalArgumentException if {@code props} is {@code null}; or, under {@link SqlPolicy#RAW_SQL}, if a value
     *         is a {@code Float} or {@code Double} that is {@code NaN} or infinite, or another {@code Number} whose text is
     *         not a valid numeric literal
     * @throws IllegalStateException under {@link SqlPolicy#NAMED_SQL} if the named-parameter handler emits an empty token
     */
    protected void appendInsertProps(final Map<String, Object> props) {
        N.checkArgNotNull(props, cs.props);

        appendInsertProps(props, props.keySet(), -1);
    }

    /**
     * Appends the values for an INSERT operation in the specified column order.
     *
     * @param props a map of property names to values to be inserted; must not be {@code null}
     * @param propNames the ordered column names; must not be {@code null}
     * @throws IllegalArgumentException if {@code props} or {@code propNames} is {@code null}; or, under
     *         {@link SqlPolicy#RAW_SQL}, if a value is a {@code Float} or {@code Double} that is {@code NaN} or infinite,
     *         or another {@code Number} whose text is not a valid numeric literal
     * @throws IllegalStateException under {@link SqlPolicy#NAMED_SQL} if the named-parameter handler emits an empty token
     */
    protected void appendInsertProps(final Map<String, Object> props, final Collection<String> propNames) {
        appendInsertProps(props, propNames, -1);
    }

    /**
     * Appends the values for an INSERT operation in the specified column order.
     *
     * @param props a map of property names to values to be inserted; must not be {@code null}
     * @param propNames the ordered column names; must not be {@code null}
     * @param rowIndex zero-based row index in batch insert mode; negative for single-row insert
     * @throws IllegalArgumentException if {@code props} or {@code propNames} is {@code null}; or, under
     *         {@link SqlPolicy#RAW_SQL}, if a value is a {@code Float} or {@code Double} that is {@code NaN} or infinite,
     *         or another {@code Number} whose text is not a valid numeric literal
     * @throws IllegalStateException under {@link SqlPolicy#NAMED_SQL} if the named-parameter handler emits an empty token
     */
    protected void appendInsertProps(final Map<String, Object> props, final Collection<String> propNames, final int rowIndex) {
        N.checkArgNotNull(props, cs.props);
        N.checkArgNotNull(propNames, cs.propNames);

        final boolean indexedNames = rowIndex >= 0 && (_sqlPolicy == SqlPolicy.NAMED_SQL || _sqlPolicy == SqlPolicy.IBATIS_SQL);
        int i = 0;
        for (final String propName : propNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }
            appendParameter(indexedNames ? propName + "_" + rowIndex : propName, props.get(propName));
        }
    }

    /**
     * Appends the given condition to the SQL string builder.
     * Implementations must reject a {@code null} condition with an {@link IllegalArgumentException}.
     *
     * @param cond the condition to append; must not be {@code null}
     * @throws IllegalArgumentException if {@code cond} is {@code null}
     */
    protected abstract void appendCondition(final Condition cond);

    /**
     * Appends the given condition as a parameter value expression. If the condition is a {@code SubQuery},
     * it is wrapped in parentheses; otherwise, it is appended directly.
     *
     * @param cond the condition to append; must not be {@code null}
     * @throws IllegalArgumentException if {@code cond} is {@code null} (rejected by {@link #appendCondition(Condition)})
     */
    protected void appendConditionAsParameter(final Condition cond) {
        if (cond instanceof SubQuery) {
            _sb.append(SK._PARENTHESIS_L);
            appendCondition(cond);
            _sb.append(SK._PARENTHESIS_R);
        } else {
            appendCondition(cond);
        }
    }

    /**
     * Appends a string expression to the SQL string builder, normalizing column names according to the current naming policy.
     * Simple alphanumeric column names are normalized directly; complex expressions are parsed and each identifier is normalized individually.
     * A SQL Server expression containing a local or global temporary-table identifier ({@code #name} or
     * {@code ##name}) is emitted verbatim after comment validation, because a dialect-neutral tokenizer
     * otherwise has to interpret a context-free {@code #name} as a MySQL hash comment.
     *
     * @param expr the string expression to append (must not be {@code null}, empty, or blank)
     * @param isFromAppendColumn {@code true} if the expression originates from an append-column call (applies stricter validation and naming policy conversion),
     *                           {@code false} otherwise
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or if {@code expr} contains a SQL comment token and either
     *                                  {@code isFromAppendColumn} is {@code true} or {@code expr} contains a SQL Server temporary-table identifier
     */
    protected void appendStringExpr(final String expr, final boolean isFromAppendColumn) {
        checkSqlFragmentNotBlank(expr, "expr");

        final boolean containsSqlServerTempIdentifier = _dialectFamily == DialectFamily.SQL_SERVER && containsSqlServerTempIdentifier(expr);

        if ((isFromAppendColumn || containsSqlServerTempIdentifier) && containsSqlCommentToken(expr)) {
            throw new IllegalArgumentException("SQL comment token is not allowed in SQL expression: " + expr);
        }

        if (containsSqlServerTempIdentifier) {
            _sb.append(expr);
            return;
        }

        if (expr.length() < 16) {
            // Although the simple-column pattern permits '-', a hyphen can be the subtraction operator.
            // Let the tokenizer split those expressions so each operand is normalized independently.
            // This mirrors SqlExpression.toSql and prevents a naming policy from treating the complete
            // expression (for example, "aB-cD") as one identifier. Digit-leading tokens (for example
            // "2faCode") also fall through to the tokenizer, which passes them through unconverted, again
            // mirroring SqlExpression.toSql; the simple-column pattern alone would admit them here.
            final boolean matched = isSimpleColumnName(expr);

            if (matched) {
                if (isFromAppendColumn) {
                    _sb.append(normalizeColumnName(expr, _namingPolicy));
                } else {
                    _sb.append(normalizeColumnName(_propColumnNameMap, expr));
                }

                return;
            }
        }

        final List<String> words = _tokenizer.tokenize(expr);

        String word = null;
        for (int i = 0, len = words.size(); i < len; i++) {
            word = words.get(i);

            if (word.isEmpty() || !isIdentifierStart(word.charAt(0)) || SqlParser.isFunctionName(words, i) || containsQuotedLiteral(word)
                    || isSqlVariable(words, i)) {
                _sb.append(word);
            } else {
                _sb.append(normalizeColumnName(_propColumnNameMap, word));
            }
        }
    }

    /** Same ASCII identifier shape as the short-expression fast path, without a Matcher allocation. */
    private static boolean isSimpleColumnName(final String expr) {
        if (!isIdentifierStart(expr.charAt(0))) {
            return false;
        }
        for (int i = 1; i < expr.length(); i++) {
            final char ch = expr.charAt(i);
            if (!(isIdentifierStart(ch) || (ch >= '0' && ch <= '9'))) {
                return false;
            }
        }
        return true;
    }

    // The three predicates below mirror their namesakes in SqlExpression.toSql. As noted where
    // sqlKeyWords is declared, SqlExpression and this builder are two independent rendering paths for the
    // same expression text, so a guard added to one must be added to the other or the same fragment
    // renders differently depending on how it reaches the SQL. Parity is pinned by
    // AbstractQueryBuilderTest.testRawExpressionRendersIdenticallyThroughBothPaths.

    /** Leading character of a convertible identifier: an ASCII letter or an underscore. */
    private static boolean isIdentifierStart(final char ch) {
        return Strings.isAsciiAlpha(ch) || ch == '_';
    }

    /**
     * SqlParser keeps a SQL literal prefix and its quoted body in one token (for example {@code N'camelCase'}
     * or {@code _utf8mb4'camelCase'}). Applying a naming policy to that whole token would modify data inside
     * the literal.
     */
    private static boolean containsQuotedLiteral(final String word) {
        return word.indexOf(SK._SINGLE_QUOTE) > 0;
    }

    /**
     * SQL Server/MySQL variable markers ({@code @name}, {@code @@name}) sit immediately before the variable
     * name, so any token following a bare {@code "@"}/{@code "@@"} token is a variable name, not a column.
     */
    private static boolean isSqlVariable(final List<String> words, final int index) {
        if (index == 0) {
            return false;
        }

        final String previous = words.get(index - 1);

        return "@".equals(previous) || "@@".equals(previous);
    }

    /**
     * Appends a single column name to the SQL string builder, using the current entity class and table alias context.
     *
     * @param propName the property or column name to append (must not be {@code null}, empty, or blank, and must not contain a SQL comment token)
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or contains a SQL comment token
     */
    protected void appendColumnName(final String propName) {
        checkSqlFragmentNotBlank(propName, "propName");

        if (containsSqlCommentToken(propName)) {
            throw new IllegalArgumentException("SQL comment token is not allowed in column expression: " + propName);
        }

        appendColumnName(_entityClass, _entityInfo, _propColumnNameMap, _tableAlias, propName, null, false, null, false, true);
    }

    /**
     * Detects SQL comment openers outside quoted regions. Hash-prefixed operators, MyBatis markers,
     * and SQL Server temporary identifiers are data tokens rather than hash comments.
     */
    private boolean containsSqlCommentToken(final String expr) {
        char quoteChar = 0;

        for (int i = 0, len = expr.length(); i < len; i++) {
            final char ch = expr.charAt(i);

            if (quoteChar != 0) {
                if (ch == quoteChar) {
                    // Backslash escapes apply only inside single-quoted string literals (MySQL semantics);
                    // quoted identifiers ("..." / `...`) do not use backslash escaping. Count the backslashes
                    // immediately preceding this quote BEFORE the doubled-quote ('') check: an odd count means
                    // the quote is escaped (\') and is a literal character that stays in the string. Doing the
                    // doubled-quote check first would misread "\''" as a '' escape, treat the string as never
                    // closing, and hide any trailing comment token from the guard.
                    int backslashCount = 0;

                    if (quoteChar == SK._SINGLE_QUOTE) {
                        for (int k = i - 1; k >= 0 && expr.charAt(k) == '\\'; k--) {
                            backslashCount++;
                        }
                    }

                    if (backslashCount % 2 == 0) {
                        // Not a backslash-escaped quote: a doubled quote ('' / "" / ``) is an escaped literal
                        // quote that stays in the string; otherwise this is the closing quote.
                        if (i < len - 1 && expr.charAt(i + 1) == quoteChar) {
                            i++;
                        } else {
                            quoteChar = 0;
                        }
                    }
                    // else: odd backslash count -> escaped quote (\'), a literal character; stays in the string.
                }

                continue;
            }

            if (ch == SK._SINGLE_QUOTE || ch == SK._DOUBLE_QUOTE || ch == SK._BACKTICK) {
                quoteChar = ch;
                continue;
            }

            if (ch == '[') {
                for (i++; i < len; i++) {
                    if (expr.charAt(i) == ']') {
                        if (i < len - 1 && expr.charAt(i + 1) == ']') {
                            i++;
                        } else {
                            break;
                        }
                    }
                }

                continue;
            }

            if (ch == '-' && i < len - 1 && expr.charAt(i + 1) == '-') {
                return true;
            }

            if (ch == '/' && i < len - 1 && expr.charAt(i + 1) == '*') {
                return true;
            }

            if (ch == '#') {
                if (_dialectFamily == DialectFamily.SQL_SERVER && isSqlServerTempIdentifierAt(expr, i)) {
                    if (i < len - 1 && expr.charAt(i + 1) == '#') {
                        i++;
                    }

                    continue;
                }

                if (i > 0 && expr.charAt(i - 1) == '?') {
                    // The '#' is the second character of a configured ?# operator. Do not advance
                    // past the following character; it may itself open a real comment token.
                    continue;
                }

                if (i < len - 1) {
                    final char nextChar = expr.charAt(i + 1);

                    if (nextChar == '>' || nextChar == '#' || nextChar == '{' || nextChar == '-') {
                        i++; // consume both chars of the ##, #>, #{, or #- token as a unit
                        continue;
                    }
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Reports whether {@code expr} contains a SQL Server local or global temporary-table identifier
     * ({@code #name} or {@code ##name}) outside quoted regions and comments.
     */
    private static boolean containsSqlServerTempIdentifier(final String expr) {
        for (int i = 0, len = expr.length(); i < len; i++) {
            if (isSqlServerTempIdentifierAt(expr, i)) {
                return true;
            }

            final int next = skipSqlQuotedOrComment(expr, i);

            if (next != i) {
                i = next - 1;
            }
        }

        return false;
    }

    /**
     * Reports whether a SQL Server temporary-table identifier ({@code #name} or {@code ##name}) starts at
     * {@code index}: one or two {@code '#'} characters followed by an identifier character, and not
     * immediately preceded by an identifier character.
     */
    private static boolean isSqlServerTempIdentifierAt(final String expr, final int index) {
        final int len = expr.length();

        if (index < 0 || index >= len - 1 || expr.charAt(index) != '#') {
            return false;
        }

        int nameStart = index + 1;

        if (expr.charAt(nameStart) == '#') {
            nameStart++;
        }

        if (nameStart >= len || !isAliasIdentifierChar(expr.charAt(nameStart))) {
            return false;
        }

        return index == 0 || !isAliasIdentifierChar(expr.charAt(index - 1));
    }

    /**
     * Appends a column name to the SQL string builder with full control over aliasing, table prefix, and sub-entity expansion.
     * A mapped column that already includes a qualifier is emitted as-is; table aliases and sub-entity
     * table names are prefixed only to unqualified mappings. Explicit SELECT aliases are honored under
     * every naming policy, including {@link NamingPolicy#NO_CHANGE}.
     *
     * @param entityClass the entity class for resolving sub-entity properties
     * @param entityInfo the bean info for the entity class, or {@code null}
     * @param propColumnNameMap the property-to-column-name mapping
     * @param tableAlias the table alias to prefix the column name, or {@code null}
     * @param propName the property or column name to append; must not be {@code null}
     * @param propAlias the column alias for the SELECT clause, or {@code null}
     * @param withClassAlias whether to prefix the alias with the class alias
     * @param classAlias the class alias to use when {@code withClassAlias} is {@code true}
     * @param isForSelect whether this column is being appended in a SELECT clause (adds AS alias)
     * @param quotePropAlias whether to wrap the property alias in the dialect's identifier quote
     * @throws IllegalArgumentException if {@code propName} is {@code null}; if it does not resolve to a mapped column or
     *         sub-entity property and is empty, blank, or contains a SQL comment token; or if, in a SELECT clause, it carries
     *         a top-level {@code AS} alias that is blank, quoted, or contains a line break or an SQL comment token
     */
    protected void appendColumnName(final Class<?> entityClass, final BeanInfo entityInfo, final ImmutableMap<String, ColumnInfo> propColumnNameMap,
            final String tableAlias, final String propName, final String propAlias, final boolean withClassAlias, final String classAlias,
            final boolean isForSelect, boolean quotePropAlias) {
        N.checkArgNotNull(propName, cs.propName);

        ColumnInfo tp = propColumnNameMap == null ? null : propColumnNameMap.get(propName);

        if (tp != null) {
            appendMappedColumn(tp, tableAlias, propName, propAlias, withClassAlias, classAlias, isForSelect, quotePropAlias);
            return;
        }

        if (Strings.isEmpty(propAlias) && entityInfo != null) {
            final PropInfo propInfo = entityInfo.getPropInfo(propName);

            if (propInfo != null && propInfo.isSubEntity) {
                final Class<?> propEntityClass = propInfo.type.isCollection() ? propInfo.type.elementType().javaType() : propInfo.clazz;

                final String propEntityTableAliasOrName = tableAliasOrName(propEntityClass, _namingPolicy);

                final ImmutableMap<String, ColumnInfo> subPropColumnNameMap = propToColumnInfoMap(propEntityClass, _namingPolicy);

                final Collection<String> subSelectPropNames = QueryUtil.selectPropNames(propEntityClass, false, null);
                int i = 0;

                for (final String subPropName : subSelectPropNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    final ColumnInfo subTp = subPropColumnNameMap.get(subPropName);
                    if (subTp == null || subTp.isUnqualified()) {
                        _sb.append(propEntityTableAliasOrName).append(SK._PERIOD);
                    }

                    _sb.append(subTp != null ? subTp.columnName() : normalizeColumnName(subPropName, _namingPolicy));

                    if (isForSelect) {
                        _sb.append(_SPACE_AS_SPACE);

                        if (quotePropAlias) {
                            _sb.append(_identifierQuote);
                        }

                        _sb.append(propInfo.name).append(SK._PERIOD).append(subPropName);

                        if (quotePropAlias) {
                            _sb.append(_identifierQuote);
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
                final Map<String, ColumnInfo> newPropColumnNameMap = _aliasPropColumnNameMap.get(propTableAlias);

                if (newPropColumnNameMap != null) {
                    final String newPropName = propName.substring(index + 1);
                    tp = newPropColumnNameMap.get(newPropName);

                    if (tp != null) {
                        appendMappedColumn(tp, propTableAlias, propName, propAlias, withClassAlias, classAlias, isForSelect, quotePropAlias);
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
                    _sb.append(_identifierQuote);
                }

                if (withClassAlias) {
                    _sb.append(classAlias).append(SK._PERIOD);
                }

                _sb.append(propAlias);

                if (quotePropAlias) {
                    _sb.append(_identifierQuote);
                }
            }
        } else if (isForSelect) {
            final TopLevelAlias selectAlias = findTopLevelAlias(propName, true);

            if (selectAlias != null) {
                final String expression = propName.substring(0, selectAlias.expressionEnd()).trim();
                // A single select(String) may contain a comma-separated expression list. Preserve the
                // complete suffix after the first top-level AS for backward-compatible raw rendering and
                // to ensure validateColumnAlias sees any unsafe quote/comment token in that suffix.
                final String alias = propName.substring(selectAlias.aliasStart()).trim();
                Dsl.validateColumnAlias(expression, alias);
                //noinspection ConstantValue
                appendColumnName(entityClass, entityInfo, propColumnNameMap, tableAlias, expression, alias, withClassAlias, classAlias, isForSelect, false);
            } else {
                appendStringExpr(propName, true);

                int idx = -1;
                // A wildcard ("*", "d.*") can never carry an alias: "d.* AS "d.*"" is invalid SQL, and a joined table's
                // qualifier is never _tableAlias, so the dotted branch below would otherwise alias every "alias.*".
                if (!endsWithWildcard(propName) && (withClassAlias || propName.length() > _sb.length()
                        || (_sb.length() - propName.length() - 1 >= 0 && _sb.charAt(_sb.length() - propName.length() - 1) != _SPACE)
                        || _sb.indexOf(propName, _sb.length() - propName.length()) < 0 || ((idx = propName.indexOf(SK._PERIOD)) > 0
                                && (Strings.isEmpty(tableAlias) || tableAlias.length() != idx || !propName.startsWith(tableAlias))))) {
                    _sb.append(_SPACE_AS_SPACE);

                    if (quotePropAlias) {
                        _sb.append(_identifierQuote);
                    }

                    if (withClassAlias) {
                        _sb.append(classAlias).append(SK._PERIOD);
                    }

                    _sb.append(propName);

                    if (quotePropAlias) {
                        _sb.append(_identifierQuote);
                    }
                }
            }
        } else {
            appendStringExpr(propName, true);
        }
    }

    /** Shared rendering for direct and table-alias property mappings. */
    private void appendMappedColumn(final ColumnInfo column, final String tableAlias, final String propName, final String propAlias,
            final boolean withClassAlias, final String classAlias, final boolean isForSelect, final boolean quotePropAlias) {
        if (column.isUnqualified() && tableAlias != null && !tableAlias.isEmpty()) {
            _sb.append(tableAlias).append(SK._PERIOD);
        }

        _sb.append(column.columnName());

        if (isForSelect && (Strings.isNotEmpty(propAlias) || withClassAlias || _namingPolicy != NamingPolicy.NO_CHANGE)) {
            _sb.append(_SPACE_AS_SPACE);

            if (quotePropAlias) {
                _sb.append(_identifierQuote);
            }

            if (withClassAlias) {
                _sb.append(classAlias).append(SK._PERIOD);
            }

            _sb.append(Strings.isNotEmpty(propAlias) ? propAlias : propName);

            if (quotePropAlias) {
                _sb.append(_identifierQuote);
            }
        }
    }

    /**
     * Checks whether the specified entity class has sub-entity properties that should be included in the query.
     *
     * @param entityClass the entity class to check; must not be {@code null}
     * @param includeSubEntityProperties whether sub-entity properties are requested to be included
     * @return {@code true} if sub-entity properties should be included and the entity class has them
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    protected static boolean hasSubEntityToInclude(final Class<?> entityClass, final boolean includeSubEntityProperties) {
        N.checkArgNotNull(entityClass, cs.entityClass);

        return includeSubEntityProperties && N.notEmpty(getSubEntityPropNames(entityClass));
    }

    /**
     * Checks whether the array represents a single inline query rather than a list of column names.
     * Returns {@code true} when the array has exactly one element and that element either starts with
     * the {@code SELECT} keyword (a FROM-less query such as {@code "SELECT 1"} is still a valid query),
     * is a balanced parenthesized query such as {@code "(SELECT 1)"} ({@code UNION (SELECT 1)} is
     * valid SQL), or contains both a {@code SELECT} and a {@code FROM} keyword (in that order).
     *
     * @param propOrColumnNames array of property or column names to check
     * @return {@code true} if the array contains a single inline query, {@code false} otherwise
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null}, or has exactly one element and that element is {@code null}
     */
    protected static boolean isInlineQuery(final String... propOrColumnNames) {
        N.checkArgNotNull(propOrColumnNames, cs.propOrColumnNames);
        N.checkArgument(propOrColumnNames.length != 1 || propOrColumnNames[0] != null, "The single element of 'propOrColumnNames' must not be null");

        return isInlineQuery(SqlParser.tokenizer(), propOrColumnNames);
    }

    /**
     * Tokenizer-aware implementation used by builders whose dialect supplies a custom tokenizer
     * configuration. Keeping the public-to-subclasses {@link #isInlineQuery(String...)} helper static
     * preserves its existing source and binary contract.
     */
    private static boolean isInlineQuery(final SqlParser.Tokenizer tokenizer, final String... propOrColumnNames) {
        if (propOrColumnNames.length == 1) {
            final String query = propOrColumnNames[0].trim();
            int index = tokenizer.indexOfToken(query, SK.SELECT, 0, false);

            if (index == 0) {
                return true;
            }

            if (index > 0) {
                // A FROM-less query wrapped in balanced parentheses is still a complete query
                // ("UNION (SELECT 1)" is valid SQL). This is an additional acceptance path only:
                // strings it rejects fall through to the SELECT-...-FROM ordering check below.
                if (isParenthesizedSelect(query, index)) {
                    return true;
                }

                index = tokenizer.indexOfToken(query, SK.FROM, index, false);

                return index >= 1;
            }
        }

        return false;
    }

    /**
     * Reports whether {@code query} is one or more balanced opening parentheses (plus whitespace)
     * followed by a {@code SELECT} query, e.g. {@code "(SELECT 1)"} or {@code "((SELECT 1))"}.
     * The balance scan is quote- and comment-aware, so an unbalanced operand like {@code "(SELECT 1"}
     * is rejected while a parenthesis inside a string literal does not affect the count. Once the
     * outermost parenthesis closes, only whitespace or comments may follow it.
     *
     * @param query the trimmed candidate operand
     * @param selectIndex the index of the first {@code SELECT} token in {@code query} (must be {@code > 0})
     * @return {@code true} if the text before {@code SELECT} is only opening parentheses and whitespace,
     *         and all parentheses in {@code query} balance out
     */
    private static boolean isParenthesizedSelect(final String query, final int selectIndex) {
        int leadingParens = 0;

        for (int i = 0; i < selectIndex; i++) {
            final char ch = query.charAt(i);

            if (ch == '(') {
                leadingParens++;
            } else if (!Character.isWhitespace(ch)) {
                return false;
            }
        }

        if (leadingParens == 0) {
            return false;
        }

        int depth = leadingParens;

        for (int i = selectIndex, len = query.length(); i < len; i++) {
            final int next = skipSqlQuotedOrComment(query, i);

            if (next != i) {
                i = next - 1;
                continue;
            }

            final char ch = query.charAt(i);

            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                if (--depth < 0) {
                    return false;
                }

                if (depth == 0) {
                    // The closing parenthesis must terminate the operand. Accept only whitespace or
                    // comments after it; otherwise fragments such as "(SELECT 1) + 2" are not complete
                    // parenthesized SELECT queries and must not be admitted as set-operation operands.
                    for (int j = i + 1; j < len; j++) {
                        final char trailingChar = query.charAt(j);

                        if (Character.isWhitespace(trailingChar)) {
                            continue;
                        }

                        final boolean lineComment = trailingChar == '-' && j < len - 1 && query.charAt(j + 1) == '-';
                        final boolean blockComment = trailingChar == '/' && j < len - 1 && query.charAt(j + 1) == '*';
                        final boolean hashComment = trailingChar == '#' && isAliasScannerHashCommentStart(query, j);

                        if ((!lineComment && !blockComment && !hashComment) || (blockComment && query.indexOf("*/", j + 2) < 0)) {
                            return false;
                        }

                        final int afterTrivia = skipSqlQuotedOrComment(query, j);

                        if (afterTrivia == j) {
                            return false;
                        }

                        j = afterTrivia - 1;
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Normalizes a column name according to the specified naming policy.
     * SQL keywords (and any name when the policy is {@code NO_CHANGE}) are returned unchanged.
     * Otherwise the name is converted with {@link QueryUtil#convertIdentifier(String, NamingPolicy)},
     * exactly as {@code SqlExpression.toSql} does, so the two rendering paths agree. Leading and
     * trailing underscore runs are preserved ({@code _firstName} renders as {@code _first_name} under
     * {@code SNAKE_CASE}); only the identifier between them is converted.
     *
     * @param word the column name to normalize
     * @param namingPolicy the naming policy to apply
     * @return the normalized column name
     */
    protected static String normalizeColumnName(final String word, final NamingPolicy namingPolicy) {
        if (sqlKeyWords.contains(word) || namingPolicy == NamingPolicy.NO_CHANGE) {
            return word;
        }

        // CAMEL_CASE used to go through Beans.normalizePropName, whose Java-keyword map rewrote the
        // identifier "class" (any case) to "clazz" -- a bean-property convention that has no place in a
        // SQL column name and that SqlExpression.toSql never applied. The shared helper keeps both paths
        // equal and restores the leading/trailing underscores that NamingPolicy.convert strips.
        if (namingPolicy == NamingPolicy.CAMEL_CASE && isPlainCamelIdentifier(word)) {
            return word;
        }
        return QueryUtil.convertIdentifier(word, namingPolicy);
    }

    /**
     * Conservative identity check: lower camel ASCII words with isolated capitals already have
     * the requested spelling. Acronyms, separators, digit/capital boundaries, and Unicode still
     * use the shared converter; no global cache or retained identifier strings are needed.
     */
    private static boolean isPlainCamelIdentifier(final String word) {
        if (word == null || word.isEmpty() || word.charAt(0) < 'a' || word.charAt(0) > 'z') {
            return false;
        }
        for (int i = 1; i < word.length(); i++) {
            final char ch = word.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                continue;
            }
            if (ch < 'A' || ch > 'Z' || word.charAt(i - 1) < 'a' || word.charAt(i - 1) > 'z' || i + 1 == word.length() || word.charAt(i + 1) < 'a'
                    || word.charAt(i + 1) > 'z') {
                return false;
            }
        }
        return true;
    }

    /**
     * Normalizes a column name using the property-to-column-name mapping, with support for table alias resolution.
     * Falls back to the static naming policy conversion if no mapping is found.
     * An existing qualifier in a mapped column takes precedence over the property path's table alias.
     *
     * @param propColumnNameMap the property-to-column-name mapping, or {@code null}
     * @param propName the property name to normalize; must not be {@code null}
     * @return the normalized column name, optionally prefixed with a table alias
     * @throws IllegalArgumentException if {@code propName} is {@code null}
     */
    protected String normalizeColumnName(final ImmutableMap<String, ColumnInfo> propColumnNameMap, final String propName) {
        N.checkArgNotNull(propName, cs.propName);

        ColumnInfo tp = propColumnNameMap == null ? null : propColumnNameMap.get(propName);

        if (tp != null) {
            if (tp.isUnqualified() && _tableAlias != null && !_tableAlias.isEmpty()) {
                return _tableAlias + "." + tp.columnName();
            }
            return tp.columnName();
        }

        if (_aliasPropColumnNameMap != null && !_aliasPropColumnNameMap.isEmpty()) {
            final int index = propName.indexOf('.');

            if (index > 0) {
                final String propTableAlias = propName.substring(0, index);
                final Map<String, ColumnInfo> newPropColumnNameMap = _aliasPropColumnNameMap.get(propTableAlias);

                if (newPropColumnNameMap != null) {
                    final String newPropName = propName.substring(index + 1);
                    tp = newPropColumnNameMap.get(newPropName);

                    if (tp != null) {
                        return tp.isUnqualified() ? propTableAlias + "." + tp.columnName() : tp.columnName();
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

    /**
     * Parses an entity (String, Map, or bean) for an INSERT operation and populates the builder's
     * property names or property-value map. Null values are skipped for bean entities, as are ID
     * properties still holding their type's default value (for a composite ID, only when every ID
     * component is still default).
     *
     * @param instance the query builder instance to populate; must not be {@code null}
     * @param entity the entity to parse (a column name String, a Map of properties, or a bean object)
     * @param excludedPropNames property names to exclude from the insert, or {@code null}
     * @throws IllegalArgumentException if {@code instance} is {@code null}; if {@code entity} is a Map that is empty, has a
     *                                  non-String/null/empty/blank key, or has no entries left after exclusions are applied;
     *                                  if a String entity is empty or blank; or if {@code entity} is {@code null} or is not an
     *                                  instance of an entity bean class
     */
    protected static void parseInsertEntity(@SuppressWarnings("rawtypes") final AbstractQueryBuilder instance, final Object entity,
            final Set<String> excludedPropNames) {
        N.checkArgNotNull(instance, cs.instance);

        if (entity instanceof String) {
            checkSqlFragmentNotBlank((String) entity, "entity");
            instance._propOrColumnNames = Array.asList((String) entity);
        } else if (entity instanceof Map) {
            instance._props = new LinkedHashMap<>((Map<String, Object>) entity);
            checkSqlFragmentKeysNotBlank(instance._props, "entity map");

            if (N.notEmpty(excludedPropNames)) {
                Maps.removeKeys(instance._props, excludedPropNames);
            }

            N.checkArgument(!instance._props.isEmpty(), "entity map must contain at least one non-excluded property");
        } else {
            final Collection<String> propNames = QueryUtil.insertPropNames(entity, excludedPropNames);
            final Map<String, Object> map = N.newLinkedHashMap(propNames.size());
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(entity.getClass());
            final ImmutableList<String> idPropNameList = beanInfo.idPropNameList;
            boolean allIdPropsWithDefaultValue = true;
            Object propValue;

            // check for composite id.
            if (N.size(idPropNameList) > 1) {
                for (String idPropName : idPropNameList) {
                    propValue = beanInfo.getPropValue(entity, idPropName);

                    if (!isDefaultIdPropValue(propValue)) {
                        allIdPropsWithDefaultValue = false;
                        break;
                    }
                }
            }

            for (final String propName : propNames) {
                propValue = beanInfo.getPropValue(entity, propName);

                if (propValue == null
                        || (allIdPropsWithDefaultValue && !idPropNameList.isEmpty() && idPropNameList.contains(propName) && isDefaultIdPropValue(propValue))) {
                    continue; // skip null or zero id values
                }

                map.put(propName, propValue);
            }

            instance._props = map;
        }
    }

    /**
     * Converts a collection of entities (Maps or beans) into a list of property-value maps for batch INSERT operations.
     * {@code null} elements are skipped. The element type is determined by the first non-null element:
     * <ul>
     *   <li>If it is a {@link Map}, every other non-null element must also be a {@code Map} with the
     *       exact same key set; each row is defensively copied and no property is removed.</li>
     *   <li>If it is a bean, every other non-null element must have the same runtime class;
     *       a property is removed from every row only when it is {@code null} across all entities. A
     *       default-valued ID column is removed only when every row has a completely default ID; for
     *       composite IDs, assigning any component retains every non-null component column.</li>
     * </ul>
     *
     * @param propsList the collection of entities to convert
     * @return a list of property-value maps suitable for batch insert
     * @throws IllegalArgumentException if {@code propsList} is {@code null} or empty or every element is {@code null};
     *         if a map is empty, has a non-string or {@code null}, empty, or blank key, or does not share the same key
     *         set as the other map rows; if elements have mixed types (some {@code Map}, some bean); if the first
     *         non-null element is neither a {@code Map} nor an instance of an entity bean class; if bean rows do not
     *         have the same runtime class; or if no bean column remains after all-null/default columns are removed
     */
    protected static List<Map<String, Object>> toInsertPropsList(final Collection<?> propsList) {
        final Optional<?> first = N.firstNonNull(propsList);

        if (first.isPresent() && first.get() instanceof Map) {
            final List<Map<String, Object>> newPropsList = new ArrayList<>(propsList.size());

            Set<String> expectedKeys = null;

            for (final Object props : propsList) {
                if (props == null) {
                    continue;
                }

                N.checkArgument(props instanceof Map, "All elements in propsList must be Map when the first non-null element is Map");
                final Map<String, Object> propsMap = (Map<String, Object>) props;

                for (final Object propName : propsMap.keySet()) {
                    if (!(propName instanceof String)) {
                        throw new IllegalArgumentException("All keys in batch INSERT maps must be String: " + propName);
                    }
                    checkSqlFragmentNotBlank((String) propName, "Batch INSERT map key");
                }

                if (expectedKeys == null) {
                    N.checkArgument(!propsMap.isEmpty(), "Map at first non-null position in propsList must not be empty");
                    expectedKeys = new LinkedHashSet<>(propsMap.keySet());
                } else {
                    // All rows in a batch INSERT must share the same column set; otherwise extra keys are silently
                    // dropped and missing keys produce stray NULL parameters, leading to data loss / corruption.
                    // Build diagnostic strings only on failure; rendering both key sets on every
                    // valid row otherwise costs more than copying the row itself.
                    if (!propsMap.keySet().equals(expectedKeys)) {
                        throw new IllegalArgumentException("All non-null Maps in propsList must have the same key set for batch INSERT. Expected: "
                                + expectedKeys + ", current: " + propsMap.keySet());
                    }
                }

                newPropsList.add(new LinkedHashMap<>(propsMap));
            }

            N.checkArgument(N.notEmpty(newPropsList), "All elements in propsList are null");

            return newPropsList;
        }

        N.checkArgument(first.isPresent(), "All elements in propsList are null");

        final Class<?> entityClass = first.get().getClass();
        final Collection<String> propNames = QueryUtil.insertPropNames(entityClass, null);
        final BeanInfo firstEntityBeanInfo = ParserUtil.getBeanInfo(entityClass);
        final List<Map<String, Object>> newPropsList = new ArrayList<>(propsList.size());

        for (final Object entity : propsList) {
            if (entity == null) {
                continue;
            }

            final Class<?> currentEntityClass = entity.getClass();
            if (currentEntityClass != entityClass) {
                throw new IllegalArgumentException("All non-null bean entities in propsList must have the same runtime class. Expected: "
                        + entityClass.getName() + ", current: " + currentEntityClass.getName());
            }

            final Map<String, Object> props = N.newLinkedHashMap(propNames.size());

            for (final String propName : propNames) {
                props.put(propName, firstEntityBeanInfo.getPropValue(entity, propName));
            }

            newPropsList.add(props);
        }

        final ImmutableList<String> idPropNameList = firstEntityBeanInfo.idPropNameList;
        boolean removeDefaultIdValues = idPropNameList.size() <= 1;
        if (!removeDefaultIdValues) {
            removeDefaultIdValues = true;
            outer: for (final Map<String, Object> props : newPropsList) {
                for (final String idPropName : idPropNameList) {
                    if (!isDefaultIdPropValue(props.get(idPropName))) {
                        removeDefaultIdValues = false;
                        break outer;
                    }
                }
            }
        }

        // A column disappears only when every row is null/default. Resolve its ID status once
        // and stop at the first retained value, avoiding a stream and captured lambda per column.
        final List<String> nullPropToRemove = new ArrayList<>();
        for (final String propName : propNames) {
            final boolean defaultIdMayBeRemoved = removeDefaultIdValues && idPropNameList.contains(propName);
            boolean remove = true;
            for (final Map<String, Object> props : newPropsList) {
                final Object value = props.get(propName);
                if (value != null && !(defaultIdMayBeRemoved && isDefaultIdPropValue(value))) {
                    remove = false;
                    break;
                }
            }
            if (remove) {
                nullPropToRemove.add(propName);
            }
        }

        if (N.notEmpty(nullPropToRemove)) {
            for (final Map<String, Object> props : newPropsList) {
                Maps.removeKeys(props, nullPropToRemove);
            }
        }

        N.checkArgument(!newPropsList.get(0).isEmpty(), "No insertable values remain after removing columns that are null/default in every batch row");

        return newPropsList;
    }

    /**
     * Validates that the multi-select list is not empty and that each selection has a non-null entity class,
     * valid selected-property fragments, and a safe result alias.
     *
     * @param multiSelects the list of selections to validate
     * @throws IllegalArgumentException if {@code multiSelects} is {@code null} or empty, contains a
     *         {@code null} selection or one with a {@code null} entity class, a {@code null}, empty, or blank included property name,
     *         an unsafe (blank, quoted, or comment-bearing) table alias, or an unsafe class alias
     */
    protected static void checkMultiSelects(final List<Selection> multiSelects) {
        N.checkArgNotEmpty(multiSelects, cs.multiSelects);

        for (final Selection selection : multiSelects) {
            N.checkArgNotNull(selection, "Selection can't be null in 'multiSelects'");
            N.checkArgNotNull(selection.entityClass(), "Class can't be null in 'multiSelects'");

            if (N.notEmpty(selection.includedPropNames())) {
                checkSqlFragmentsNotBlank(selection.includedPropNames(), "selection.includedPropNames");
            }

            // A non-empty table alias is prefixed verbatim to every rendered column, so validate it like the
            // class alias: a whitespace-only alias would render " .col" and a comment token would swallow the list.
            if (Strings.isNotEmpty(selection.tableAlias())) {
                validateTableAlias(selection.entityClass().getSimpleName(), selection.tableAlias());
            }

            if (Strings.isNotEmpty(selection.classAlias())) {
                Dsl.validateColumnAlias(selection.entityClass().getSimpleName(), selection.classAlias());
            }
        }
    }

    /**
     * Validates that a table alias is safe to prefix to rendered column names, applying exactly the checks
     * of {@code Dsl.validateColumnAlias(String, String)} (both delegate to the same {@code Dsl.validateAlias}
     * helper) but reporting the failure as a table alias: it must be non-blank and must not contain a quote
     * character, a line break, or an SQL comment token.
     *
     * @param entityName the simple name of the entity the alias belongs to (used in the error message)
     * @param tableAlias the table alias to validate
     * @throws IllegalArgumentException if {@code tableAlias} is {@code null}, blank, contains a quote character
     *                                  ({@code "}, {@code `}, or {@code '}), a line break, or an SQL comment
     *                                  token ({@code --}, {@code /*}, {@code *}{@code /}, or {@code #})
     */
    private static void validateTableAlias(final String entityName, final String tableAlias) {
        Dsl.validateAlias("Table alias", entityName, tableAlias);
    }

    /**
     * Returns the property-to-column-name mapping for the specified entity class and naming policy.
     *
     * @param entityClass the entity class
     * @param namingPolicy the naming policy
     * @return an immutable map from property and column lookup keys to column information
     */
    protected static ImmutableMap<String, ColumnInfo> propToColumnInfoMap(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        return QueryUtil.propToColumnInfoMap(entityClass, namingPolicy);
    }

    /**
     * Builds the FROM clause string for a multi-select query, including table names, aliases,
     * and any sub-entity tables referenced by the resolved projection. A selection whose properties
     * are all excluded contributes its own table but no sub-entity tables.
     *
     * @param multiSelects the list of selections defining the tables and their properties; must not be {@code null}
     *        (its elements are expected to have passed {@link #checkMultiSelects(List)})
     * @param namingPolicy the naming policy for table name conversion
     * @return the constructed FROM clause string
     * @throws IllegalArgumentException if {@code multiSelects} is {@code null}
     */
    protected static String getFromClause(final List<Selection> multiSelects, final NamingPolicy namingPolicy) {
        N.checkArgNotNull(multiSelects, cs.multiSelects);

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

                if (N.notEmpty(selection.includedPropNames()) || selection.includesSubEntityProperties()) {
                    final Class<?> entityClass = selection.entityClass();
                    final Collection<String> selectPropNames = N.notEmpty(selection.includedPropNames()) ? selection.includedPropNames()
                            : QueryUtil.selectPropNames(entityClass, selection.includesSubEntityProperties(), selection.excludedPropNames());
                    final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

                    if (N.isEmpty(subEntityPropNames)) {
                        continue;
                    }

                    final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass);
                    PropInfo propInfo = null;
                    Class<?> subEntityClass = null;

                    for (final String subEntityPropName : subEntityPropNames) {
                        if (!containsSelectedPropOrSubProp(selectPropNames, subEntityPropName)) {
                            continue;
                        }

                        propInfo = entityInfo.getPropInfo(subEntityPropName);

                        if (propInfo == null) {
                            continue;
                        }

                        subEntityClass = (propInfo.type.isCollection() ? propInfo.type.elementType() : propInfo.type).javaType();

                        sb.append(_COMMA_SPACE).append(getTableName(subEntityClass, namingPolicy));

                        final String subEntityTableAlias = tableAlias(subEntityClass);
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

    /**
     * Reports whether {@code propName} itself or any of its sub-properties (prefixed by
     * {@code propName + "."}) is contained in {@code selectPropNames}.
     */
    private static boolean containsSelectedPropOrSubProp(final Collection<String> selectPropNames, final String propName) {
        if (N.isEmpty(selectPropNames)) {
            return false;
        }

        if (selectPropNames.contains(propName)) {
            return true;
        }

        final String prefix = propName + SK.PERIOD;

        for (final String selectPropName : selectPropNames) {
            if (selectPropName != null && selectPropName.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Database product family resolved once from {@link SqlDialect.ProductInfo}. Drives the
     * product-specific parts of SQL generation: the pagination syntax emitted by {@link #limit(int)},
     * {@link #limit(int, int)} and {@link #offset(int)}, and the identifier quote used when the
     * dialect leaves {@code SqlDialect.identifierQuote()} unset.
     */
    enum DialectFamily {
        /** Oracle Database: pagination via {@code OFFSET ... ROWS} / {@code FETCH ... ROWS ONLY}. */
        ORACLE,
        /** IBM DB2: pagination via {@code OFFSET ... ROWS} / {@code FETCH ... ROWS ONLY}. */
        DB2,
        /** Microsoft SQL Server: pagination via {@code OFFSET ... ROWS FETCH NEXT ... ROWS ONLY}. */
        SQL_SERVER,
        /** MySQL/MariaDB: {@code LIMIT}/{@code OFFSET} pagination; unset identifier quote defaults to backtick. */
        MYSQL,
        /** Other products known to use {@code LIMIT}/{@code OFFSET} pagination (PostgreSQL, SQLite, H2). */
        LIMIT_STYLE,
        /** No product info, or an unrecognized product: default {@code LIMIT}/{@code OFFSET} pagination. */
        DEFAULT
    }

    /**
     * Represents a SQL string and its associated parameters.
     * This record is used to encapsulate the generated SQL and the parameters required for execution.
     * The SQL and parameter-list structure are fixed once created. Parameter values are retained by
     * reference, so mutable value objects must not be changed while the pair is in use.
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
     * String sql = sqlPair.query();
     * // "SELECT first_name AS \"firstName\", last_name AS \"lastName\" FROM users WHERE id = ?"
     *
     * List<Object> params = sqlPair.parameters();
     * // [123]
     * }</pre>
     *
     * @param query the generated SQL query string
     * @param parameters the parameter values corresponding to placeholders in the SQL; the list is defensively copied,
     *                   but its elements are not
     */
    public record SP(String query, ImmutableList<Object> parameters) {

        /**
         * Creates an immutable SQL/parameter pair. The parameter list is copied even when the supplied
         * {@link ImmutableList} wraps another collection, so later changes to that backing collection cannot
         * alter this value.
         *
         * @throws IllegalArgumentException if {@code query} or {@code parameters} is {@code null}
         */
        public SP {
            N.checkArgNotNull(query, cs.query);
            N.checkArgNotNull(parameters, cs.parameters);
            // ImmutableList.copyOf may return its argument unchanged. Force a fresh backing list because
            // callers can supply an ImmutableList created with wrap(mutableList).
            parameters = ImmutableList.wrap(new ArrayList<>(parameters));
        }
    }
}
