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
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.Join;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.Operator;
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
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.query.QueryUtil.ColumnInfo;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

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
 * <p>Concrete subclasses live in {@link com.landawn.abacus.query.SqlBuilder}. Pick a subclass
 * by parameter style and naming policy (see {@link com.landawn.abacus.query.SqlBuilder} for the full table).</p>
 *
 * <p>Instances are <b>not thread-safe</b>; build one per thread or per query and always call
 * {@link #build()} to obtain the {@link SP} pair and release pooled resources. After {@code build()}
 * the builder is closed and must not be reused; calling {@code build()} again throws
 * {@link IllegalStateException}.</p>
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
 *                  .set("name", "status")
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

    protected static final Logger logger = LoggerFactory.getLogger(AbstractQueryBuilder.class);

    /** Constant for the {@code ALL} select modifier (the SQL default; opposite of {@code DISTINCT}). */
    public static final String ALL = SK.ALL;

    /** Constant for the TOP clause in SQL queries. */
    public static final String TOP = SK.TOP;

    /** Constant for the UNIQUE clause in SQL queries. */
    public static final String UNIQUE = SK.UNIQUE;

    /** Constant for the DISTINCT clause in SQL queries. */
    public static final String DISTINCT = SK.DISTINCT;

    /** Constant for the DISTINCTROW clause in SQL queries. */
    public static final String DISTINCTROW = SK.DISTINCTROW;

    /** Constant for the asterisk (*) wildcard in SQL queries. */
    public static final String ASTERISK = SK.ASTERISK;

    /** Constant for the COUNT(*) aggregate function. */
    public static final String COUNT_ALL = "count(*)";

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
     * unparsed {@link Limit#literal() literal}, where each token is an integer literal or a {@code ?} /
     * {@code :name} / <code>#{name}</code> parameter placeholder. In practice the integer-only forms are
     * parsed into concrete count/offset by {@link Limit#Limit(String)} and rendered via {@link #limit(int)} /
     * {@link #limit(int, int)}, so this pattern normally handles the placeholder-bearing forms. Deliberately
     * product-specific expressions that are not recognized (e.g. a vendor function) do not match and are
     * emitted verbatim.
     */
    private static final String LIMIT_SLOT_PATTERN = "(\\d+|\\?|:\\w+|#\\{[^}]+\\})";
    private static final Pattern GENERIC_LIMIT_EXPRESSION_PATTERN = Pattern.compile(
            "LIMIT\\s+" + LIMIT_SLOT_PATTERN + "(?:\\s+OFFSET\\s+" + LIMIT_SLOT_PATTERN + "|\\s*,\\s*" + LIMIT_SLOT_PATTERN + ")?", Pattern.CASE_INSENSITIVE);

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

    /** Char array for the "EXCEPT" or "MINUS" keyword. */
    protected static final char[] _EXCEPT_MINUS = SK.EXCEPT_MINUS.toCharArray();

    /** Char array for " EXCEPT " or " MINUS ". */
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

    protected static final Map<Class<?>, Set<String>[]> defaultPropNamesPool = new ObjectPool<>(QueryUtil.POOL_SIZE);

    protected static final Map<NamingPolicy, Map<Class<?>, String>> fullSelectPartsPool = N.newHashMap(NamingPolicy.values().length);

    // The cached select parts embed the dialect's identifier quote, so backtick dialects use a separate
    // pool to avoid cross-dialect cache poisoning between dialects sharing the same naming policy.
    protected static final Map<NamingPolicy, Map<Class<?>, String>> fullSelectPartsPoolForBacktick = N.newHashMap(NamingPolicy.values().length);

    static {
        for (final NamingPolicy np : NamingPolicy.values()) {
            fullSelectPartsPool.put(np, new ConcurrentHashMap<>());
            fullSelectPartsPoolForBacktick.put(np, new ConcurrentHashMap<>());
        }
    }

    protected static final Map<String, char[]> tableDeleteFrom = new ConcurrentHashMap<>();

    protected static final Map<Class<?>, String[]> classTableNameMap = new ConcurrentHashMap<>();

    protected static final Map<Class<?>, String> classTableAliasMap = new ConcurrentHashMap<>();

    protected static final AtomicInteger activeStringBuilderCounter = new AtomicInteger();

    protected final SqlDialect sqlDialect;

    protected final NamingPolicy _namingPolicy; //NOSONAR

    protected final SqlPolicy _sqlPolicy; //NOSONAR

    protected final char _identifierQuote; //NOSONAR

    final DialectFamily _dialectFamily; //NOSONAR

    protected final List<Object> _parameters = new ArrayList<>(); //NOSONAR

    protected final Map<String, Integer> _namedParameterNameOccurrences = new HashMap<>(); //NOSONAR

    // Every named-parameter name emitted into the SQL so far. Needed in addition to the occurrence
    // counts because a generated "<base>_<n>" may collide with a property literally named "<base>_<n>".
    protected final Set<String> _generatedNamedParameterNames = new HashSet<>(); //NOSONAR

    // Exact NAMED_SQL token emitted for each generated name. Custom formatters are allowed to emit
    // forms other than ":name", so set-operation collision handling must not assume the default form.
    protected final Map<String, String> _renderedNamedParameterTokens = new HashMap<>(); //NOSONAR

    protected StringBuilder _sb; //NOSONAR

    protected Class<?> _entityClass; //NOSONAR

    protected BeanInfo _entityInfo; //NOSONAR

    protected ImmutableMap<String, ColumnInfo> _propColumnNameMap; //NOSONAR

    protected OperationType _op; //NOSONAR

    protected String _tableName; //NOSONAR

    protected String _tableAlias; //NOSONAR

    protected String _selectModifier; //NOSONAR

    // Buffer position right after the current segment's emitted SELECT keyword, or -1 if the
    // current segment's SELECT has not been emitted yet (reset by set operations like union()).
    protected int _selectKeywordEndIdx = -1; //NOSONAR

    protected Collection<String> _propOrColumnNames; //NOSONAR

    protected Map<String, String> _propOrColumnNameAliases; //NOSONAR

    protected List<Selection> _multiSelects; //NOSONAR

    protected Map<String, Map<String, ColumnInfo>> _aliasPropColumnNameMap; //NOSONAR

    protected Map<String, Object> _props; //NOSONAR

    protected Collection<Map<String, Object>> _propsList; //NOSONAR

    protected boolean _hasFromBeenSet = false; //NOSONAR
    protected boolean _isForConditionOnly = false; //NOSONAR

    // Whether a set(...) call has already written assignments, so chained set(...) calls know a
    // leading comma is required (sniffing the buffer's last char breaks on trailing whitespace).
    protected boolean _setListStarted = false; //NOSONAR

    protected final BiConsumer<StringBuilder, String> _handlerForNamedParameter; //NOSONAR

    protected final SqlParser.Tokenizer _tokenizer; //NOSONAR

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

                for (final String idPropName : QueryUtil.idPropertyNames(entityClass)) {
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

    /**
     * Gets the table names for a SELECT query involving the specified entity class and its sub-entities.
     * Returns a list of table names (with aliases if defined) for the main entity and its sub-entity properties.
     *
     * @param entityClass the entity class
     * @param alias the table alias for the main entity (can be null or empty)
     * @param excludedPropNames sub-entity property names to exclude (can be null)
     * @param namingPolicy the naming policy for table name conversion
     * @return a list of table name expressions, or an empty list if there are no sub-entity properties
     */
    protected static List<String> getSelectTableNames(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames,
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
     * Map<String, Expression> params = namedPlaceholders("firstName", "lastName");
     * }</pre>
     *
     * @param propNames the property names
     * @return a map with property names mapped to {@code Filters.QME}
     */
    @Beta
    protected static Map<String, Expression> namedPlaceholders(final String... propNames) {
        final Map<String, Expression> m = N.newLinkedHashMap(propNames.length);

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
     * Map<String, Expression> params = namedPlaceholders(Arrays.asList("firstName", "lastName"));
     * }</pre>
     *
     * @param propNames the collection of property names
     * @return a map with property names mapped to {@code Filters.QME}
     */
    @Beta
    protected static Map<String, Expression> namedPlaceholders(final Collection<String> propNames) {
        final Map<String, Expression> m = N.newLinkedHashMap(propNames.size());

        for (final String propName : propNames) {
            m.put(propName, Filters.QME);
        }

        return m;
    }

    private static void checkSqlFragmentNotBlank(final String value, final String argName) {
        if (Strings.isBlank(value)) {
            throw new IllegalArgumentException(argName + " must not be null, empty, or blank");
        }
    }

    private static void checkSqlFragmentsNotBlank(final String[] values, final String argName) {
        N.checkArgNotEmpty(values, argName);

        for (int i = 0; i < values.length; i++) {
            checkSqlFragmentNotBlank(values[i], argName + "[" + i + "]");
        }
    }

    private static void checkSqlFragmentsNotBlank(final Collection<String> values, final String argName) {
        N.checkArgNotEmpty(values, argName);

        int i = 0;

        for (final String value : values) {
            checkSqlFragmentNotBlank(value, argName + "[" + i++ + "]");
        }
    }

    private static void checkSqlFragmentKeysNotBlank(final Map<String, ?> values, final String argName) {
        N.checkArgNotEmpty(values, argName);

        for (final Map.Entry<String, ?> entry : values.entrySet()) {
            checkSqlFragmentNotBlank(entry.getKey(), "Key in " + argName);
        }
    }

    /**
     * Validates the complete-sub-query argument of a set-operation overload
     * ({@code union}/{@code unionAll}/{@code intersect}/{@code except}/{@code minus} taking a single query
     * string, or the SQL built by the sibling-builder overloads). These overloads are dedicated to appending
     * a complete sub-query, so the argument must satisfy the {@link #isSubQuery(String...)} heuristic.
     *
     * @param query the query string to validate
     * @param operationName the set-operation method name (e.g. {@code "union"}) used in the error message
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, or does not appear to be a {@code SELECT} sub-query
     */
    private void checkSetOperationSubQuery(final String query, final String operationName) {
        checkSqlFragmentNotBlank(query, "query");

        if (!isSubQuery(_tokenizer, query) || !SqlParser.isReadOnlyQuery(query)) {
            throw new IllegalArgumentException("The query argument to " + operationName
                    + " must be a complete SELECT sub-query (starting with 'SELECT', or containing 'SELECT ... FROM'), but was: \"" + query
                    + "\". To start a new SELECT from a column list, use " + operationName + "(Collection) followed by from(...).");
        }
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
     * @throws IllegalArgumentException if {@code tableName} is {@code null}, empty, or blank
     * @throws IllegalStateException if the current operation is neither {@code ADD} nor {@code QUERY}, if columns/values
     *             have not been set, or if it is called after SQL has already been emitted (e.g., after {@code from()} or a second {@code into()})
     */
    public This into(final String tableName) {
        checkSqlFragmentNotBlank(tableName, "tableName");
        final String normalizedTableName = tableName.trim();

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

        // Guard against calling into() after SQL has already been emitted (e.g. after from(), or a second
        // into()), which would blindly concatenate a new INSERT fragment onto the existing statement.
        if (!_sb.isEmpty()) {
            throw new IllegalStateException("into() must be called before from() and any other SQL-emitting method, and can only be called once");
        }

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
                        : QueryUtil.selectPropertyNames(selection.entityClass(), selection.includesSubEntityProperties(), selection.excludedPropNames());
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

        return (This) this;
    }

    /**
     * Specifies the target table for an {@code INSERT} or {@code INSERT ... SELECT} operation using an entity class.
     * <p>The table name will be derived from the entity class based on the naming policy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.insert(account).into(Account.class).build().query();
     * // Table name derived from Account class based on naming policy
     * }</pre>
     *
     * @param entityClass the entity class representing the target table
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if the current operation is neither {@code ADD} nor {@code QUERY}, if columns/values
     *             have not been set, or if it is called after SQL has already been emitted (e.g., after {@code from()} or a second {@code into()})
     */
    public This into(final Class<?> entityClass) {
        setEntityClass(entityClass);

        return into(getTableName(entityClass, _namingPolicy));
    }

    /**
     * Specifies the target table for an {@code INSERT} or {@code INSERT ... SELECT} operation with an explicit table name and entity class.
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
     * @throws IllegalArgumentException if {@code tableName} is {@code null}, empty, or blank
     * @throws IllegalStateException if the current operation is neither {@code ADD} nor {@code QUERY}, if columns/values
     *             have not been set, or if it is called after SQL has already been emitted (e.g., after {@code from()} or a second {@code into()})
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
     * String sql = PSC.select("name").distinct().from("account").build().query();
     * // Output: SELECT DISTINCT name FROM account
     * }</pre>
     *
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if a select modifier has already been set
     */
    public This distinct() { //NOSONAR
        return selectModifier(DISTINCT);
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
     * @param selectModifier modifiers like {@code ALL}, {@code DISTINCT}, {@code DISTINCTROW},
     *                       {@code TOP}, etc.; may be {@code null} or empty (no-op)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if a select modifier has already been set for the current SELECT segment
     * @throws IllegalArgumentException if {@code selectModifier} is non-empty but blank (whitespace only)
     */
    public This selectModifier(final String selectModifier) {
        if (Strings.isNotEmpty(_selectModifier)) {
            throw new IllegalStateException("selectModifier has already been set and cannot be set again");
        }

        if (Strings.isEmpty(selectModifier)) {
            return (This) this;
        }

        checkSqlFragmentNotBlank(selectModifier, "selectModifier");

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
     * @throws IllegalArgumentException if {@code tableNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if the current operation is not {@code QUERY}, or if no columns have been set by {@code select()}
     */
    public This from(final String... tableNames) {
        N.checkArgNotEmpty(tableNames, "tableNames");

        final List<String> normalizedTableNames = new ArrayList<>(tableNames.length);

        for (int i = 0; i < tableNames.length; i++) {
            final String tableName = tableNames[i];
            N.checkArgNotEmpty(tableName, "tableNames[" + i + "]");
            final String normalizedTableName = tableName.trim();
            N.checkArgNotEmpty(normalizedTableName, "tableNames[" + i + "]");
            normalizedTableNames.add(normalizedTableName);
        }

        if (normalizedTableNames.size() == 1) {
            return from(normalizedTableNames.get(0));
        }

        final String localTableName = normalizedTableNames.get(0);
        return from(localTableName, Strings.join(normalizedTableNames, SK.COMMA_SPACE));
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
     * @throws IllegalArgumentException if {@code tableNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if the current operation is not {@code QUERY}, or if no columns have been set by {@code select()}
     */
    public This from(final Collection<String> tableNames) {
        N.checkArgNotEmpty(tableNames, "tableNames");

        final List<String> normalizedTableNames = new ArrayList<>(tableNames.size());
        int idx = 0;

        for (final String tableName : tableNames) {
            N.checkArgNotEmpty(tableName, "tableNames[" + idx + "]");
            final String normalizedTableName = tableName.trim();
            N.checkArgNotEmpty(normalizedTableName, "tableNames[" + idx + "]");
            normalizedTableNames.add(normalizedTableName);
            idx++;
        }

        if (normalizedTableNames.size() == 1) {
            return from(normalizedTableNames.get(0));
        }

        final String localTableName = normalizedTableNames.get(0);
        return from(localTableName, Strings.join(normalizedTableNames, SK.COMMA_SPACE));
    }

    /**
     * Sets the FROM clause with a single expression.
     * <p>The expression can be a table name, subquery, or multiple tables separated by comma.</p>
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
     * @param expr the FROM clause expression
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     * @throws IllegalStateException if the current operation is not {@code QUERY}, or if no columns have been set by {@code select()}
     */
    public This from(final String expr) {
        N.checkArgNotEmpty(expr, "expr");
        final String trimmedExpr = expr.trim();
        N.checkArgNotEmpty(trimmedExpr, "expr");

        // Only the first table reference supplies the primary table/alias used while rendering entity
        // properties. A raw FROM body may contain quoted commas or an inline JOIN; a character-only comma
        // scan either split quoted identifiers (for example, "accounts,archive") or handed the whole JOIN
        // to the alias scanner, which then mistook the final predicate token for the primary table alias.
        final int separatorIdx = findFirstTopLevelFromSeparator(trimmedExpr);
        final String localTableName = separatorIdx > 0 ? trimmedExpr.substring(0, separatorIdx) : trimmedExpr;

        return from(localTableName.trim(), trimmedExpr);
    }

    /**
     * Locates the first top-level separator after the primary table reference in a raw FROM body.
     * Commas and JOIN-family keywords inside quoted regions, comments, or parentheses are ignored.
     *
     * @param fromClause the trimmed text that will be emitted after {@code FROM}
     * @return the separator index, or {@code -1} when the clause contains one table reference
     */
    private static int findFirstTopLevelFromSeparator(final String fromClause) {
        int depth = 0;

        for (int i = 0, len = fromClause.length(); i < len; i++) {
            final int next = skipSqlQuotedOrComment(fromClause, i);

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

    private static boolean isTopLevelJoinStart(final String sql, final int index) {
        if (index <= 0 || !isJoinLeadingTrivia(sql, index)) {
            return false;
        }

        return isSqlWordAt(sql, index, SK.JOIN) || isSqlWordAt(sql, index, "INNER") || isSqlWordAt(sql, index, "LEFT") || isSqlWordAt(sql, index, "RIGHT")
                || isSqlWordAt(sql, index, "FULL") || isSqlWordAt(sql, index, "CROSS") || isSqlWordAt(sql, index, "NATURAL") || isSqlWordAt(sql, index, "OUTER")
                || isSqlWordAt(sql, index, "STRAIGHT_JOIN") || isSqlWordAt(sql, index, "ASOF") || isSqlWordAt(sql, index, "SEMI")
                || isSqlWordAt(sql, index, "ANTI");
    }

    private static boolean isJoinLeadingTrivia(final String sql, final int index) {
        final char previous = sql.charAt(index - 1);
        return Character.isWhitespace(previous) || (previous == '/' && index > 1 && sql.charAt(index - 2) == '*');
    }

    private static boolean isSqlWordAt(final String sql, final int index, final String word) {
        final int end = index + word.length();
        return end <= sql.length() && sql.regionMatches(true, index, word, 0, word.length()) && isAliasKeywordBoundary(sql, end);
    }

    /**
     * Sets the FROM clause with an expression and associates it with an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from("users u", User.class).build().query();
     * // Associates the User class for property mapping
     * }</pre>
     *
     * @param expr the FROM clause expression
     * @param entityClass the entity class for property mapping (may be {@code null}, in which case no entity-class association is performed)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     * @throws IllegalStateException if the current operation is not {@code QUERY}, or if no columns have been set by {@code select()}
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
     * String sql = PSC.select("*").from(User.class).build().query();
     * // Table name derived from User class based on naming policy
     * }</pre>
     *
     * @param entityClass the entity class representing the table
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if the current operation is not {@code QUERY}, or if no columns have been set by {@code select()}
     */
    public This from(final Class<?> entityClass) {
        return from(entityClass, QueryUtil.tableAlias(entityClass));
    }

    /**
     * Sets the FROM clause using an entity class with an alias.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class, "u").build().query();
     * // Output: SELECT * FROM users u (table name based on naming policy)
     * }</pre>
     *
     * @param entityClass the entity class representing the table
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalStateException if the current operation is not {@code QUERY}, or if no columns have been set by {@code select()}
     */
    public This from(final Class<?> entityClass, final String alias) {
        setEntityClass(entityClass);

        if (Strings.isEmpty(alias)) {
            return from(getTableName(entityClass, _namingPolicy));
        } else {
            return from(getTableName(entityClass, _namingPolicy) + " " + alias);
        }
    }

    /**
     * Sets the FROM clause using the specified entity class and multiple table names.
     *
     * @param entityClass the entity class to associate with this query
     * @param tableNames the collection of table names for the FROM clause
     * @return this builder instance for method chaining
     */
    protected This from(final Class<?> entityClass, final Collection<String> tableNames) {
        setEntityClass(entityClass);

        return from(tableNames);
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
    protected This from(final String tableName, final String fromClause) {
        appendOperationBeforeFrom(tableName);

        final boolean withAlias = Strings.isNotEmpty(_tableAlias);
        final boolean isForSelect = _op == OperationType.QUERY;

        if (N.notEmpty(_propOrColumnNames)) {
            if (_entityClass != null && !withAlias && _propOrColumnNames == QueryUtil.selectPropertyNames(_entityClass, false, null)) { // NOSONAR
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
            _aliasPropColumnNameMap = N.newHashMap(_multiSelects.size());

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
                        : QueryUtil.selectPropertyNames(selectionEntityClass, selection.includesSubEntityProperties(), selection.excludedPropNames());

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

    /**
     * Appends the SELECT operation and modifier to the SQL string builder before the FROM clause.
     * Parses the table name to extract a top-level table alias if present, ignoring whitespace and
     * {@code AS} text inside quoted regions, comments, and nested parentheses. Also validates that
     * the operation is a QUERY and that column names have been set.
     *
     * @param tableName the table name, optionally including an alias (e.g., "users u")
     * @throws IllegalStateException if the current operation is not {@code QUERY}, or if no columns have been set by {@code select()}
     */
    protected void appendOperationBeforeFrom(final String tableName) {
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

        final String trimmedTableName = tableName.trim();
        final TopLevelAlias tableAlias = findTopLevelAlias(trimmedTableName, false);

        if (tableAlias != null) {
            _tableName = trimmedTableName.substring(0, tableAlias.expressionEnd()).trim();
            _tableAlias = normalizeTableAlias(trimmedTableName.substring(tableAlias.aliasStart()).trim());
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

    /**
     * Registers a property-to-column-name mapping for a table alias.
     * This allows column names to be resolved correctly when a table alias is used in the query.
     *
     * @param entityClass the entity class whose property-column mapping to register
     * @param alias the table alias to associate with the mapping
     */
    protected void addPropColumnMapForAlias(final Class<?> entityClass, final String alias) {
        if (_aliasPropColumnNameMap == null) {
            _aliasPropColumnNameMap = new HashMap<>();
        }

        if (N.isEmpty(_propColumnNameMap) && Beans.isBeanClass(entityClass)) {
            _propColumnNameMap = propToColumnInfoMap(entityClass, _namingPolicy);
        }

        _aliasPropColumnNameMap.put(alias, Beans.isBeanClass(entityClass) ? propToColumnInfoMap(entityClass, _namingPolicy) : _propColumnNameMap);
    }

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
     * Finds an alias separator at SQL nesting depth zero. Quoted strings/identifiers, line, hash
     * and block comments, and parenthesized regions are skipped, so their whitespace, parentheses and
     * {@code AS} text cannot be mistaken for the outer alias boundary.
     *
     * @param sqlFragment the table or select-expression fragment to scan
     * @param explicitAsRequired whether only an explicit top-level {@code AS} is accepted; if
     *        {@code false}, the last top-level trivia boundary is accepted as an implicit table-alias separator
     * @return the expression end and alias start, or {@code null} when no qualifying alias is present
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
                } else if (ch == '\\') {
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
                    implicitAlias = new TopLevelAlias(pendingImplicitExpressionEnd, i);
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
                int aliasStart = i + 2;

                while (aliasStart < len && Character.isWhitespace(sqlFragment.charAt(aliasStart))) {
                    aliasStart++;
                }

                if (explicitAlias == null && i > 0 && aliasStart < len) {
                    explicitAlias = new TopLevelAlias(i, aliasStart);
                }

                i++;
            }
        }

        return explicitAlias != null ? explicitAlias : explicitAsRequired ? null : implicitAlias;
    }

    private static boolean isAliasKeywordBoundary(final String sqlFragment, final int index) {
        if (index < 0 || index >= sqlFragment.length()) {
            return true;
        }

        final char ch = sqlFragment.charAt(index);
        return ch != '.' && ch != '_' && ch != '$' && !Character.isLetterOrDigit(ch);
    }

    private static boolean isAliasScannerHashCommentStart(final String sqlFragment, final int index) {
        if (index < sqlFragment.length() - 1) {
            final char next = sqlFragment.charAt(index + 1);

            if (next == '{' || next == '>' || next == '-' || next == '#' || (index > 0 && sqlFragment.charAt(index - 1) == '?')) {
                return false;
            }

            if ((index == 0 || sqlFragment.charAt(index - 1) == '.') && (next == '_' || next == '$' || Character.isLetterOrDigit(next))) {
                return false;
            }
        }

        return true;
    }

    private record TopLevelAlias(int expressionEnd, int aliasStart) {
        // Compact scan result shared by FROM-table and SELECT-expression alias parsing.
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
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This join(final String joinExpr) {
        checkSqlFragmentNotBlank(joinExpr, "joinExpr");

        _sb.append(_SPACE_JOIN_SPACE);

        _sb.append(joinExpr);

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
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
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
        if (Strings.isNotEmpty(alias)) {
            addPropColumnMapForAlias(entityClass, alias);
        }

        _sb.append(joinKeyword);

        if (Strings.isNotEmpty(alias)) {
            _sb.append(getTableName(entityClass, _namingPolicy)).append(" ").append(alias);
        } else {
            _sb.append(getTableName(entityClass, _namingPolicy));
        }

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
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
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
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This innerJoin(final String joinExpr) {
        checkSqlFragmentNotBlank(joinExpr, "joinExpr");

        _sb.append(_SPACE_INNER_JOIN_SPACE);

        _sb.append(joinExpr);

        return (This) this;
    }

    /**
     * Adds an INNER JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).innerJoin(Order.class).on("users.id = orders.user_id").build().query();
     * // Output: SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
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
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
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
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This leftJoin(final String joinExpr) {
        checkSqlFragmentNotBlank(joinExpr, "joinExpr");

        _sb.append(_SPACE_LEFT_JOIN_SPACE);

        _sb.append(joinExpr);

        return (This) this;
    }

    /**
     * Adds a LEFT JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).leftJoin(Order.class).on("users.id = orders.user_id").build().query();
     * // Output: SELECT * FROM users LEFT JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
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
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
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
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This rightJoin(final String joinExpr) {
        checkSqlFragmentNotBlank(joinExpr, "joinExpr");

        _sb.append(_SPACE_RIGHT_JOIN_SPACE);

        _sb.append(joinExpr);

        return (This) this;
    }

    /**
     * Adds a RIGHT JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).rightJoin(Order.class).on("users.id = orders.user_id").build().query();
     * // Output: SELECT * FROM users RIGHT JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
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
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
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
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This fullJoin(final String joinExpr) {
        checkSqlFragmentNotBlank(joinExpr, "joinExpr");

        _sb.append(_SPACE_FULL_JOIN_SPACE);

        _sb.append(joinExpr);

        return (This) this;
    }

    /**
     * Adds a FULL JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).fullJoin(Order.class).on("users.id = orders.user_id").build().query();
     * // Output: SELECT * FROM users FULL JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
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
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
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
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This crossJoin(final String joinExpr) {
        checkSqlFragmentNotBlank(joinExpr, "joinExpr");

        _sb.append(_SPACE_CROSS_JOIN_SPACE);

        _sb.append(joinExpr);

        return (This) this;
    }

    /**
     * Adds a CROSS JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).crossJoin(Order.class).build().query();
     * // Output: SELECT * FROM users CROSS JOIN orders
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
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
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
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
     * @throws IllegalArgumentException if {@code joinExpr} is {@code null}, empty, or blank
     */
    public This naturalJoin(final String joinExpr) {
        checkSqlFragmentNotBlank(joinExpr, "joinExpr");

        _sb.append(_SPACE_NATURAL_JOIN_SPACE);

        _sb.append(joinExpr);

        return (This) this;
    }

    /**
     * Adds a NATURAL JOIN clause using an entity class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*").from(User.class).naturalJoin(Order.class).build().query();
     * // Output: SELECT * FROM users NATURAL JOIN orders
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @return this SqlBuilder instance for method chaining
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
     * }</pre>
     *
     * @param entityClass the entity class to join
     * @param alias the table alias
     * @return this SqlBuilder instance for method chaining
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
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     */
    public This on(final String expr) {
        checkSqlFragmentNotBlank(expr, "expr");

        _sb.append(_SPACE_ON_SPACE);

        appendStringExpr(expr, false);

        return (This) this;
    }

    /**
     * Adds an ON clause for a composite join condition, joining the given expressions with {@code AND}.
     *
     * <p>This is a convenience for multi-column ON conditions. Each element is rendered as a separate
     * expression and the resulting fragments are combined with {@code AND}.</p>
     *
     * <p><b>Note:</b> unlike {@link Filters#on(String, String)} — where two strings mean an equality
     * {@code ON left = right} — each argument here is a <em>complete</em> boolean expression and multiple
     * arguments are joined with {@code AND}: {@code on("u.id = o.user_id", "o.active = 1")}. Calling
     * {@code on("u.id", "o.user_id")} renders the invalid SQL {@code ON u.id AND o.user_id}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("*")
     *                 .from("users u")
     *                 .join("orders o")
     *                 .on("u.id = o.user_id", "u.tenant_id = o.tenant_id")
     *                 .build().query();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id AND u.tenant_id = o.tenant_id
     * }</pre>
     *
     * @param exprs the join condition expressions (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code exprs} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This on(final String... exprs) {
        checkSqlFragmentsNotBlank(exprs, "exprs");

        _sb.append(_SPACE_ON_SPACE);

        for (int i = 0, len = exprs.length; i < len; i++) {
            if (i > 0) {
                _sb.append(_SPACE_AND_SPACE);
            }

            appendStringExpr(exprs[i], false);
        }

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
     *                 .on(Filters.expr("u.id = o.user_id"))
     *                 .build().query();
     * // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
     * }</pre>
     *
     * @param condition the join condition (must not be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code condition} is {@code null}
     */
    public This on(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        _sb.append(_SPACE_ON_SPACE);

        appendCondition(condition);

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
     *                 .build().query();
     * // Output: SELECT * FROM users JOIN orders USING (user_id)
     * }</pre>
     *
     * @param expr the column name(s) for the USING clause
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or contains a SQL comment token
     */
    public This using(final String expr) {
        checkSqlFragmentNotBlank(expr, "expr");

        if (containsSqlCommentToken(expr)) {
            throw new IllegalArgumentException("SQL comment token is not allowed in column expression: " + expr);
        }

        _sb.append(_SPACE_USING_SPACE);

        final String trimmedExpr = expr.trim();

        if (Strings.isNotEmpty(trimmedExpr) && trimmedExpr.startsWith(SK.PARENTHESIS_L) && trimmedExpr.endsWith(SK.PARENTHESIS_R)) {
            appendStringExpr(trimmedExpr, false);
        } else {
            _sb.append(SK._PARENTHESIS_L);
            appendColumnName(trimmedExpr);
            _sb.append(SK._PARENTHESIS_R);
        }

        return (This) this;
    }

    /**
     * Adds a USING clause with multiple columns for join conditions.
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This using(final String... propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

        return using(Array.asList(propOrColumnNames));
    }

    /**
     * Adds a USING clause with a collection of columns for join conditions.
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This using(final Collection<String> propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

        _sb.append(_SPACE_USING_SPACE);

        _sb.append(SK._PARENTHESIS_L);

        int i = 0;
        for (final String propOrColumnName : propOrColumnNames) {
            if (i++ > 0) {
                _sb.append(_COMMA_SPACE);
            }

            appendColumnName(propOrColumnName);
        }

        _sb.append(SK._PARENTHESIS_R);

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
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE age > 18
     * }</pre>
     *
     * @param expr the WHERE condition expression (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     * @throws IllegalStateException if {@code WHERE} has already been set on this builder
     */
    public This where(final String expr) {
        checkSqlFragmentNotBlank(expr, "expr");

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
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE age > ?
     * }</pre>
     *
     * @param condition the WHERE condition (must not be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code condition} is {@code null}
     * @throws IllegalStateException if {@code WHERE} has already been set on this builder
     * @see Filters
     */
    public This where(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        checkIfAlreadyCalled(SK.WHERE);

        init(true);

        _sb.append(_SPACE_WHERE_SPACE);

        appendCondition(condition);

        return (This) this;
    }

    /**
     * Adds a GROUP BY ASC clause with a single column.
     * Convenience method equivalent to {@code groupBy(expr, SortDirection.ASC)}.
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
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
     */
    @Beta
    public This groupByAsc(final Collection<String> propOrColumnNames) {
        return groupBy(propOrColumnNames, SortDirection.ASC);
    }

    /**
     * Adds a GROUP BY DESC clause with a single column.
     * Convenience method equivalent to {@code groupBy(expr, SortDirection.DESC)}.
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
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
     */
    public This groupBy(final String propOrColumnName) {
        checkSqlFragmentNotBlank(propOrColumnName, "propOrColumnName");

        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        appendColumnName(propOrColumnName);

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
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand
     * }</pre>
     *
     * @param propOrColumnNames the columns to group by (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
     */
    public This groupBy(final String... propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

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
     *                 .build().query();
     * // Output: SELECT category, COUNT(*) FROM products GROUP BY category DESC
     * }</pre>
     *
     * @param expr the column or expression to group by
     * @param direction the sort direction
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or if {@code direction} is {@code null}
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
     */
    public This groupBy(final String expr, final SortDirection direction) {
        N.checkArgNotNull(direction, "direction");

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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
     */
    public This groupBy(final Collection<String> propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

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
     *                 .build().query();
     * // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to group by
     * @param direction the direction appended after each column in the GROUP BY clause
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element, or if {@code direction} is {@code null}
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
     */
    public This groupBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");
        N.checkArgNotNull(direction, "direction");

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
     * @throws IllegalArgumentException if {@code groupings} is {@code null} or empty, contains a {@code null}, empty, or blank key, or maps any key to a {@code null} direction
     * @throws IllegalStateException if {@code GROUP BY} has already been set on this builder
     */
    public This groupBy(final Map<String, SortDirection> groupings) {
        checkSqlFragmentKeysNotBlank(groupings, "groupings");

        for (final Map.Entry<String, SortDirection> entry : groupings.entrySet()) {
            N.checkArgNotNull(entry.getValue(), "Value in groupings");
        }

        checkIfAlreadyCalled(SK.GROUP_BY);

        _sb.append(_SPACE_GROUP_BY_SPACE);

        int i = 0;
        for (final Map.Entry<String, SortDirection> entry : groupings.entrySet()) {
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
     *                 .build().query();
     * // Output: SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > 10
     * }</pre>
     *
     * @param expr the HAVING condition expression (must not be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     * @throws IllegalStateException if {@code HAVING} has already been set on this builder
     */
    public This having(final String expr) {
        checkSqlFragmentNotBlank(expr, "expr");

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
     *                 .build().query();
     * // Output: SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > ?
     * }</pre>
     *
     * @param condition the HAVING condition (must not be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code condition} is {@code null}
     * @throws IllegalStateException if {@code HAVING} has already been set on this builder
     * @see Filters
     */
    public This having(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        checkIfAlreadyCalled(SK.HAVING);

        _sb.append(_SPACE_HAVING_SPACE);

        appendCondition(condition);

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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY name ASC
     * }</pre>
     *
     * @param propOrColumnName the property or column name to order by ascending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY created_date DESC
     * }</pre>
     *
     * @param propOrColumnName the property or column name to order by descending
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code propOrColumnName} is {@code null}, empty, or blank
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
     */
    public This orderBy(final String propOrColumnName) {
        checkSqlFragmentNotBlank(propOrColumnName, "propOrColumnName");

        checkIfAlreadyCalled(SK.ORDER_BY);

        _sb.append(_SPACE_ORDER_BY_SPACE);

        appendColumnName(propOrColumnName);

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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY last_name, first_name
     * }</pre>
     *
     * @param propOrColumnNames the columns to order by (must not be {@code null} or empty, and no element may be {@code null}, empty, or blank)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
     */
    public This orderBy(final String... propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY name DESC
     * }</pre>
     *
     * @param expr the column or expression to order by
     * @param direction the sort direction
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or if {@code direction} is {@code null}
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
     */
    public This orderBy(final String expr, final SortDirection direction) {
        N.checkArgNotNull(direction, "direction");

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
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
     */
    public This orderBy(final Collection<String> propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

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
     *                 .build().query();
     * // Output: SELECT * FROM users ORDER BY last_name DESC, first_name DESC
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to order by
     * @param direction the direction appended after each column in the ORDER BY clause
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element, or if {@code direction} is {@code null}
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
     */
    public This orderBy(final Collection<String> propOrColumnNames, final SortDirection direction) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");
        N.checkArgNotNull(direction, "direction");

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
     * @throws IllegalArgumentException if {@code orders} is {@code null} or empty, contains a {@code null}, empty, or blank key, or maps any key to a {@code null} direction
     * @throws IllegalStateException if {@code ORDER BY} has already been set on this builder
     */
    public This orderBy(final Map<String, SortDirection> orders) {
        checkSqlFragmentKeysNotBlank(orders, "orders");

        for (final Map.Entry<String, SortDirection> entry : orders.entrySet()) {
            N.checkArgNotNull(entry.getValue(), "Value in orders");
        }

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
     * @throws IllegalArgumentException if {@code count} is negative
     * @throws IllegalStateException if {@code LIMIT} has already been set on this builder, or on a
     *         {@code FETCH}-style dialect if {@code FETCH FIRST}/{@code FETCH NEXT} has already been set
     */
    public This limit(final int count) {
        N.checkArgNotNegative(count, "count");
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
     * @param countToken the row count as an integer literal or parameter placeholder
     * @throws IllegalStateException if {@code FETCH FIRST}/{@code FETCH NEXT} has already been set
     */
    private void appendFetchFirst(final String countToken) {
        if (_dialectFamily == DialectFamily.SQL_SERVER) {
            checkIfAlreadyCalled(SK.FETCH_NEXT);
            calledOpSet.add(SK.FETCH_FIRST);

            if (calledOpSet.add(SK.OFFSET)) {
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
     * @param countToken the row count as an integer literal or parameter placeholder
     * @param offsetToken the offset as an integer literal or parameter placeholder
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
     * @throws IllegalArgumentException if {@code count} or {@code offset} is negative
     * @throws IllegalStateException if {@code LIMIT} or {@code OFFSET} has already been set on this builder,
     *         or on a {@code FETCH}-style dialect if {@code FETCH FIRST}/{@code FETCH NEXT} has already been set
     */
    public This limit(final int count, final int offset) {
        N.checkArgNotNegative(count, "count");
        N.checkArgNotNegative(offset, "offset");
        checkIfAlreadyCalled(SK.LIMIT);
        checkIfAlreadyCalled(SK.OFFSET);

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
     * pagination syntax. An <i>unparsed</i> expression (one carrying a {@code ?} / {@code :name} /
     * <code>#{name}</code> placeholder, or product-specific syntax not otherwise recognized) is re-rendered
     * in the dialect's FETCH pagination syntax when this builder uses one (Oracle, DB2, SQL Server) and the
     * expression is a generic {@code LIMIT count [OFFSET offset]} form with placeholder tokens; any other
     * unparsed expression is emitted verbatim.
     * Shared by the {@link Criteria} and standalone-{@link Limit} branches of {@link #append(Condition)}.
     *
     * @param limit the limit condition to render (must not be {@code null})
     */
    private void appendLimit(final Limit limit) {
        // An unparsed string expression (placeholder or product-specific/opaque syntax) is signalled by
        // the sentinel count == MAX_VALUE / offset == 0; render it from its literal. Everything else —
        // the numeric constructors and string expressions parsed into concrete count/offset — is emitted
        // in the dialect's pagination syntax via limit(int) / limit(int, int).
        if (Strings.isNotEmpty(limit.literal()) && !limit.isResolved()) {
            if (usesFetchPagination() && appendLimitExpressionInFetchSyntax(limit.literal())) {
                return;
            }

            checkIfAlreadyCalled(SK.LIMIT);

            // The verbatim literal may itself carry an OFFSET portion (e.g. "LIMIT ? OFFSET ?"); consume the
            // OFFSET slot too so a follow-up offset(...) call is rejected instead of silently emitting a
            // second OFFSET clause. Matched as a whole, case-sensitive token: Limit's normalization
            // upper-cases the OFFSET keyword only outside placeholders, so ":offset" / "#{ offset }" do not
            // misfire, and the whole-token match (unlike a raw substring test) keeps a placeholder name such
            // as ":rowOFFSETCount" from spuriously consuming the slot.
            if (_tokenizer.indexOfToken(limit.literal(), SK.OFFSET, 0, true) >= 0) {
                checkIfAlreadyCalled(SK.OFFSET);
            }

            _sb.append(_SPACE).append(limit.literal());
        } else if (limit.offset() > 0) {
            limit(limit.count(), limit.offset());
        } else {
            limit(limit.count());
        }
    }

    /**
     * Attempts to re-render a generic {@code LIMIT count [OFFSET offset]} or {@code LIMIT offset, count}
     * expression in the dialect's
     * FETCH pagination syntax, consuming the same clause slots as {@link #limit(int)} /
     * {@link #limit(int, int)}. Returns {@code false} without emitting anything when the expression
     * does not match {@link #GENERIC_LIMIT_EXPRESSION_PATTERN}, in which case the caller emits it
     * verbatim.
     *
     * @param expression the normalized limit expression from {@link Limit#literal()}
     * @return {@code true} if the expression was rendered in FETCH pagination syntax
     */
    private boolean appendLimitExpressionInFetchSyntax(final String expression) {
        final Matcher matcher = GENERIC_LIMIT_EXPRESSION_PATTERN.matcher(expression);

        if (!matcher.matches()) {
            return false;
        }

        final String commaCountToken = matcher.group(3);
        final String countToken = commaCountToken == null ? matcher.group(1) : commaCountToken;
        final String offsetToken = commaCountToken == null ? matcher.group(2) : matcher.group(1);

        checkIfAlreadyCalled(SK.LIMIT);

        if (offsetToken == null) {
            appendFetchFirst(countToken);
        } else {
            checkIfAlreadyCalled(SK.OFFSET);
            appendOffsetFetchNext(countToken, offsetToken);
        }

        return true;
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
     * @throws IllegalArgumentException if {@code offset} is negative
     * @throws IllegalStateException if {@code OFFSET} has already been set on this builder
     */
    public This offset(final int offset) {
        N.checkArgNotNegative(offset, "offset");
        checkIfAlreadyCalled(SK.OFFSET);

        _sb.append(_SPACE_OFFSET_SPACE).append(offset);

        if (usesFetchPagination()) {
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
     * @throws IllegalArgumentException if {@code offset} is negative
     * @throws IllegalStateException if {@code OFFSET} has already been set on this builder
     * @see #offset(int)
     * @see #limit(int, int)
     */
    public This offsetRows(final int offset) {
        N.checkArgNotNegative(offset, "offset");
        checkIfAlreadyCalled(SK.OFFSET);

        _sb.append(_SPACE_OFFSET_SPACE).append(offset).append(_SPACE_ROWS);

        return (This) this;
    }

    /**
     * Adds a FETCH NEXT N ROWS ONLY clause (SQL:2008 standard syntax).
     * Calling either {@link #fetchNextRows(int)} or {@link #fetchFirstRows(int)} consumes both
     * slots; you may not also call the other after this method.
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
     * @throws IllegalArgumentException if {@code count} is negative
     * @throws IllegalStateException if {@code FETCH NEXT} or {@code FETCH FIRST} has already been set
     * @see #limit(int)
     * @see #limit(int, int)
     */
    public This fetchNextRows(final int count) {
        N.checkArgNotNegative(count, "count");
        checkIfAlreadyCalled(SK.FETCH_NEXT);
        calledOpSet.add(SK.FETCH_FIRST);

        _sb.append(" FETCH NEXT ").append(count).append(" ROWS ONLY");

        return (This) this;
    }

    /**
     * Adds a FETCH FIRST N ROWS ONLY clause (SQL standard syntax).
     * Calling either {@link #fetchFirstRows(int)} or {@link #fetchNextRows(int)} consumes both
     * slots; you may not also call the other after this method.
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
     * @throws IllegalArgumentException if {@code count} is negative
     * @throws IllegalStateException if {@code FETCH FIRST} or {@code FETCH NEXT} has already been set
     * @see #limit(int)
     * @see #limit(int, int)
     */
    public This fetchFirstRows(final int count) {
        N.checkArgNotNegative(count, "count");
        checkIfAlreadyCalled(SK.FETCH_FIRST);
        calledOpSet.add(SK.FETCH_NEXT);

        _sb.append(" FETCH FIRST ").append(count).append(" ROWS ONLY");

        return (This) this;
    }

    /**
     * Records that the given clause keyword has been emitted, and throws if it was already recorded.
     * Used by clause methods (e.g. {@code WHERE}, {@code GROUP BY}, {@code HAVING}, {@code ORDER BY},
     * {@code LIMIT}, {@code OFFSET}) that may appear at most once per built statement.
     *
     * @param op the clause keyword that is being emitted (e.g. {@link SK#WHERE}, {@link SK#GROUP_BY})
     * @throws IllegalStateException if {@code op} has already been recorded for this builder
     */
    protected void checkIfAlreadyCalled(final String op) {
        if (!calledOpSet.add(op)) {
            throw new IllegalStateException("'" + op + "' has already been set and cannot be set again");
        }
    }

    /**
     * Appends a condition to the SQL statement.
     * <p>A {@link Criteria} applies its SELECT modifier to the current SELECT segment and is then expanded
     * into its JOIN/WHERE/GROUP&nbsp;BY/HAVING/set-operation/ORDER&nbsp;BY/LIMIT parts. A clause condition
     * ({@code Where}, {@code GroupBy}, {@code Having}, {@code OrderBy}, {@code Limit})
     * is rendered with its own keyword. Any other condition is appended with a leading {@code WHERE} keyword
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
     * @throws IllegalArgumentException if {@code condition} is {@code null}
     * @throws IllegalStateException if there is no current SELECT segment, if that segment already
     *                               has a select modifier, or if a clause emitted by the criteria has already been set
     * @see Filters
     */
    @Beta
    public This append(final Condition condition) {
        N.checkArgNotNull(condition, "condition");

        init(true);

        if (condition instanceof final Criteria criteria) {
            final Collection<Join> joins = criteria.joins();
            final String criteriaSelectModifier = criteria.selectModifier();

            if (Strings.isNotEmpty(criteriaSelectModifier)) {
                if (_op != OperationType.QUERY || _isForConditionOnly || _selectKeywordEndIdx < 0) {
                    throw new IllegalStateException("The Criteria select modifier ('" + criteriaSelectModifier + "') requires a current SELECT segment");
                }

                if (Strings.isNotEmpty(_selectModifier)) {
                    throw new IllegalStateException("selectModifier has already been set and cannot be set again");
                }
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
                                _sb.append(_COMMA_SPACE);
                            }

                            _sb.append(joinTableName);
                        }

                        _sb.append(SK._PARENTHESIS_R);
                    }

                    final Condition joinCond = join.condition();

                    if (joinCond != null) {
                        // Mirror Join.toString(): a raw join condition (e.g. an Expression or Binary) needs an
                        // explicit ON keyword and a separating space; an On/Using condition renders its own keyword.
                        if (joinCond.operator() != Operator.ON && joinCond.operator() != Operator.USING) {
                            _sb.append(_SPACE_ON_SPACE);
                        }

                        appendCondition(joinCond);
                    }
                }
            }

            final Clause where = criteria.where();

            if ((where != null)) {
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

            // Apply the modifier only after all Criteria clauses have been accepted. This prevents a
            // duplicate-clause failure from leaving an otherwise rejected Criteria's modifier behind.
            if (Strings.isNotEmpty(criteriaSelectModifier)) {
                selectModifier(criteriaSelectModifier);
            }
        } else if (condition instanceof Clause) {
            if (condition instanceof final Limit limit) {
                appendLimit(limit);
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

        return (This) this;
    }

    /**
     * Appends a string expression to the SQL statement.
     *
     * <p>A single separating space is inserted before {@code expr} when, and only when, it is
     * needed: that is, when the statement built so far does not already end with a space and
     * {@code expr} does not already begin with one. As a result both {@code .append("FOR UPDATE")}
     * and {@code .append(" FOR UPDATE")} produce the same, correctly spaced output (a doubled space
     * is possible only when the statement built so far already ends with a space and {@code expr}
     * also begins with one). The rest of {@code expr} is emitted verbatim and is not validated,
     * escaped, or interpreted in any way.</p>
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
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     */
    public This append(final String expr) {
        checkSqlFragmentNotBlank(expr, "expr");

        if (_sb.length() > 0 && _sb.charAt(_sb.length() - 1) != ' ' && expr.charAt(0) != ' ') {
            _sb.append(_SPACE);
        }

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
     *                 .build().query();
     * // Output: SELECT * FROM users WHERE age > ?
     * }</pre>
     *
     * @param b if true, the condition will be appended
     * @param condition the condition to append
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code b} is {@code true} and {@code condition} is {@code null}
     * @throws IllegalStateException if this builder has already been closed by {@link #build()}, or if {@code b} is
     *                               {@code true} and a clause emitted by {@code condition} has already been set
     */
    @Beta
    public This appendIf(final boolean b, final Condition condition) {
        checkOpen();

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
     * @throws IllegalArgumentException if {@code b} is {@code true} and {@code expr} is {@code null}, empty, or blank
     * @throws IllegalStateException if this builder has already been closed by {@link #build()}
     */
    @Beta
    public This appendIf(final boolean b, final String expr) {
        checkOpen();

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
     * @param b if true, append condToAppendForTrue; otherwise append condToAppendForFalse
     * @param conditionToAppendForTrue the condition to append if {@code b} is true
     * @param conditionToAppendForFalse the condition to append if {@code b} is false
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if the selected condition (the one chosen by {@code b}) is {@code null}
     * @throws IllegalStateException if a clause emitted by the selected condition has already been set
     */
    @Beta
    public This appendIfOrElse(final boolean b, final Condition conditionToAppendForTrue, final Condition conditionToAppendForFalse) {
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
     * @throws IllegalArgumentException if the selected expression (the one chosen by {@code b}) is {@code null}, empty, or blank
     */
    @Beta
    public This appendIfOrElse(final boolean b, final String exprToAppendForTrue, final String exprToAppendForFalse) {
        if (b) {
            append(exprToAppendForTrue);
        } else {
            append(exprToAppendForFalse);
        }

        return (This) this;
    }

    /**
     * Adds a UNION clause with another SQL query.
     * <p><b>&#9888;&#65039;</b> The passed {@code sqlBuilder} is finalized via {@link #build()} and cannot be reused after this call.</p>
     * <p>For {@link SqlPolicy#NAMED_SQL}, child placeholders are rendered with this parent builder's
     * named-parameter handler so the compound statement uses one placeholder syntax.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SqlBuilder query1 = PSC.select("id", "name").from("users");
     * SqlBuilder query2 = PSC.select("id", "name").from("customers");
     * String sql = query1.union(query2).build().query();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     *
     * @param sqlBuilder the SQL builder containing the query to union (must not be {@code null} and must not be this same instance)
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         or generated named parameters with a different SQL policy
     */
    public This union(final This sqlBuilder) {
        return appendSetOperation(_SPACE_UNION_SPACE, sqlBuilder, "UNION");
    }

    /**
     * Adds a UNION clause with a SQL query string.
     * UNION combines result sets from two queries and removes duplicates.
     * This overload always treats its argument as a complete query; use {@link #union(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
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
     * @param query the complete read-only {@code SELECT} sub-query to union
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, not a complete SELECT sub-query, or is not read-only
     */
    public This union(final String query) {
        return appendSetOperation(_SPACE_UNION_SPACE, "union", query);
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
     *                 .build().query();
     * // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the union query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This union(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_UNION_SPACE, propOrColumnNames);
    }

    /**
     * Adds a UNION ALL clause with another SQL query.
     * <p><b>&#9888;&#65039;</b> The passed {@code sqlBuilder} is finalized via {@link #build()} and cannot be reused after this call.</p>
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
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         or generated named parameters with a different SQL policy
     */
    public This unionAll(final This sqlBuilder) {
        return appendSetOperation(_SPACE_UNION_ALL_SPACE, sqlBuilder, "UNION ALL");
    }

    /**
     * Adds a UNION ALL clause with a SQL query string.
     * UNION ALL combines result sets from two queries and keeps all duplicates.
     * This overload always treats its argument as a complete query; use {@link #unionAll(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
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
     * @param query the complete read-only {@code SELECT} sub-query to union all
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, not a complete SELECT sub-query, or is not read-only
     */
    public This unionAll(final String query) {
        return appendSetOperation(_SPACE_UNION_ALL_SPACE, "unionAll", query);
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
     *                 .build().query();
     * // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the union all query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This unionAll(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_UNION_ALL_SPACE, propOrColumnNames);
    }

    /**
     * Adds an INTERSECT clause with another SQL query.
     * <p><b>&#9888;&#65039;</b> The passed {@code sqlBuilder} is finalized via {@link #build()} and cannot be reused after this call.</p>
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
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         or generated named parameters with a different SQL policy
     */
    public This intersect(final This sqlBuilder) {
        return appendSetOperation(_SPACE_INTERSECT_SPACE, sqlBuilder, "INTERSECT");
    }

    /**
     * Adds an INTERSECT clause with a SQL query string.
     * INTERSECT returns only rows that appear in both result sets.
     * This overload always treats its argument as a complete query; use {@link #intersect(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
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
     * @param query the complete read-only {@code SELECT} sub-query to intersect
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, not a complete SELECT sub-query, or is not read-only
     */
    public This intersect(final String query) {
        return appendSetOperation(_SPACE_INTERSECT_SPACE, "intersect", query);
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
     *                 .build().query();
     * // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM premium_users
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the intersect query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This intersect(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_INTERSECT_SPACE, propOrColumnNames);
    }

    /**
     * Adds an EXCEPT clause with another SQL query.
     * <p><b>&#9888;&#65039;</b> The passed {@code sqlBuilder} is finalized via {@link #build()} and cannot be reused after this call.</p>
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
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         or generated named parameters with a different SQL policy
     */
    public This except(final This sqlBuilder) {
        return appendSetOperation(_SPACE_EXCEPT_SPACE, sqlBuilder, "EXCEPT");
    }

    /**
     * Adds an EXCEPT clause with a SQL query string.
     * EXCEPT returns rows from the first query that don't appear in the second query.
     * This overload always treats its argument as a complete query; use {@link #except(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
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
     * @param query the complete read-only {@code SELECT} sub-query to except
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, not a complete SELECT sub-query, or is not read-only
     */
    public This except(final String query) {
        return appendSetOperation(_SPACE_EXCEPT_SPACE, "except", query);
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
     *                 .build().query();
     * // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the except query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This except(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_EXCEPT_SPACE, propOrColumnNames);
    }

    /**
     * Adds a MINUS clause with another SQL query (Oracle syntax).
     * MINUS is Oracle's equivalent to EXCEPT - returns rows from the first query that don't appear in the second.
     * <p><b>&#9888;&#65039;</b> The passed {@code sqlBuilder} is finalized via {@link #build()} and cannot be reused after this call.</p>
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
     * @throws IllegalArgumentException if {@code sqlBuilder} is {@code null}, is this same builder instance,
     *         or generated named parameters with a different SQL policy
     */
    public This minus(final This sqlBuilder) {
        return appendSetOperation(_SPACE_EXCEPT_MINUS_SPACE, sqlBuilder, "MINUS");
    }

    /**
     * Adds a MINUS clause with a SQL query string (Oracle syntax).
     * MINUS is Oracle's equivalent to EXCEPT - returns rows from the first query that don't appear in the second.
     * This overload always treats its argument as a complete query; use {@link #minus(Collection)}
     * to generate the right-hand {@code SELECT} from property or column names.
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
     * @param query the complete read-only {@code SELECT} sub-query to subtract with MINUS
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code query} is {@code null}, empty, blank, not a complete SELECT sub-query, or is not read-only
     */
    public This minus(final String query) {
        return appendSetOperation(_SPACE_EXCEPT_MINUS_SPACE, "minus", query);
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
     *                 .build().query();
     * // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns for the minus query
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This minus(final Collection<String> propOrColumnNames) {
        return appendSetOperation(_SPACE_EXCEPT_MINUS_SPACE, propOrColumnNames);
    }

    /**
     * Shared implementation for the raw-query set-operation overloads. It validates and appends
     * the complete right-hand {@code SELECT} query after resetting per-segment builder state.
     *
     * @param keyword the set-operation keyword token (e.g. {@link #_SPACE_UNION_SPACE})
     * @param operationName the public set-operation method name used in validation messages
     * @param query the complete read-only {@code SELECT} query to append
     * @return this builder instance for method chaining
     */
    @SuppressWarnings("unchecked")
    private This appendSetOperation(final char[] keyword, final String operationName, final String query) {
        checkSetOperationSubQuery(query, operationName);
        checkCanAppendSetOperation(operationName);

        _op = OperationType.QUERY;

        _propOrColumnNames = null;
        _propOrColumnNameAliases = null;
        _multiSelects = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;
        _selectModifier = null;
        _selectKeywordEndIdx = -1;

        _sb.append(keyword).append(query);

        return (This) this;
    }

    /**
     * Shared implementation for the collection set-operation starters
     * ({@code union}/{@code unionAll}/{@code intersect}/{@code except}/{@code minus}): resets the
     * builder to a fresh QUERY using the given columns and appends the set-operation keyword.
     *
     * @param keyword the set-operation keyword token (e.g. {@link #_SPACE_UNION_SPACE})
     * @param propOrColumnNames the columns of the following SELECT
     * @return this builder instance for method chaining
     */
    @SuppressWarnings("unchecked")
    private This appendSetOperation(final char[] keyword, final Collection<String> propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");
        checkCanAppendSetOperation(new String(keyword).trim());

        _op = OperationType.QUERY;

        _propOrColumnNames = new ArrayList<>(propOrColumnNames);
        _propOrColumnNameAliases = null;
        _multiSelects = null;

        calledOpSet.clear();
        _hasFromBeenSet = false;
        _tableAlias = null;
        _selectModifier = null;
        _selectKeywordEndIdx = -1;

        _sb.append(keyword);

        return (This) this;
    }

    /**
     * Renders a sibling builder for set operations while preserving the unique named-parameter
     * sequence across the full compound query.
     */
    private This appendSetOperation(final char[] keyword, final This sqlBuilder, final String operationName) {
        N.checkArgNotNull(sqlBuilder, "sqlBuilder");
        N.checkArgument(sqlBuilder != this, "Cannot apply " + operationName + " with the same SqlBuilder instance");
        checkCanAppendSetOperation(operationName);
        final Map<String, Integer> parentOccurrences = N.isEmpty(_namedParameterNameOccurrences) ? Collections.emptyMap()
                : new HashMap<>(_namedParameterNameOccurrences);

        final SP sp = sqlBuilder.build();

        // Same validation as the single-String overloads: only a SELECT query is a valid set-operation
        // operand. Without this check, e.g. an UPDATE builder's SQL would be staged as a "column list"
        // and dropped, silently truncating the compound query.
        checkSetOperationSubQuery(sp.query(), operationName);

        final Set<String> childParameterNames = new HashSet<>(sqlBuilder._generatedNamedParameterNames);
        final Map<String, String> childParameterTokens = new HashMap<>(sqlBuilder._renderedNamedParameterTokens);
        final String sql = uniquifySetOperationNamedParameters(sp.query(), sqlBuilder._namedParameterNameOccurrences, parentOccurrences, childParameterNames,
                childParameterTokens, sqlBuilder._sqlPolicy);
        mergeNamedParameterOccurrences(sqlBuilder._namedParameterNameOccurrences);
        _generatedNamedParameterNames.addAll(childParameterNames);
        _renderedNamedParameterTokens.putAll(childParameterTokens);

        if (N.notEmpty(sp.parameters())) {
            _parameters.addAll(sp.parameters());
        }

        return appendSetOperation(keyword, operationName, sql);
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
     * @throws IllegalStateException if the current operation is not a query or its current SELECT
     *         segment has not been completed
     */
    private void checkCanAppendSetOperation(final String operationName) {
        if (_op != OperationType.QUERY || _isForConditionOnly) {
            throw new IllegalStateException(operationName + " requires a complete SELECT query on its left-hand side");
        }

        final boolean completeLiteralOperand = !_sb.isEmpty() && N.isEmpty(_propOrColumnNames) && N.isEmpty(_propOrColumnNameAliases)
                && N.isEmpty(_multiSelects) && Strings.isEmpty(_selectModifier) && _selectKeywordEndIdx < 0;

        if (!_hasFromBeenSet && !completeLiteralOperand) {
            throw new IllegalStateException(operationName + " requires from(...) to complete the current SELECT segment first");
        }

        if (calledOpSet.contains(SK.ORDER_BY) || calledOpSet.contains(SK.LIMIT) || calledOpSet.contains(SK.OFFSET) || calledOpSet.contains(SK.FETCH_FIRST)
                || calledOpSet.contains(SK.FETCH_NEXT) || calledOpSet.contains(SK.FOR_UPDATE)) {
            throw new IllegalStateException(operationName + " must be added before ORDER BY, pagination, or FOR UPDATE clauses");
        }
    }

    private String uniquifySetOperationNamedParameters(final String sql, final Map<String, Integer> childOccurrences,
            final Map<String, Integer> parentOccurrences, final Set<String> childParameterNames, final Map<String, String> childParameterTokens,
            final SqlPolicy childSqlPolicy) {
        if (N.isEmpty(childOccurrences)) {
            return sql;
        }

        if (_sqlPolicy != childSqlPolicy) {
            throw new IllegalArgumentException(
                    "Set-operation builders with generated named parameters must use the same SQL policy: parent=" + _sqlPolicy + ", child=" + childSqlPolicy);
        }

        if (_sqlPolicy != SqlPolicy.NAMED_SQL && _sqlPolicy != SqlPolicy.IBATIS_SQL) {
            return sql;
        }

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

                    if (oldToken.equals(":" + oldName)) {
                        result = replaceDefaultNamedParameterToken(result, oldName, newToken);
                    } else if (!oldToken.equals(newToken)) {
                        result = replaceRenderedNamedParameterToken(result, oldToken, newToken);
                    }

                    childParameterTokens.remove(oldName);
                    childParameterTokens.put(newName, newToken);
                } else if (rename) {
                    result = replaceIbatisParameterName(result, oldName, newName);
                }

                if (rename) {
                    childParameterNames.remove(oldName);
                    childParameterNames.add(newName);
                }
            }
        }

        return result;
    }

    private static String renderNamedParameterToken(final BiConsumer<StringBuilder, String> handler, final String parameterName) {
        final StringBuilder sb = new StringBuilder(parameterName.length() + 8);
        handler.accept(sb, parameterName);

        if (sb.isEmpty()) {
            throw new IllegalStateException("The custom named-parameter handler emitted an empty token for: " + parameterName);
        }

        return sb.toString();
    }

    private void mergeNamedParameterOccurrences(final Map<String, Integer> occurrences) {
        if (N.isEmpty(occurrences)) {
            return;
        }

        for (final Map.Entry<String, Integer> entry : occurrences.entrySet()) {
            _namedParameterNameOccurrences.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    private static String indexedNamedParameterName(final String name, final int occurrence) {
        return occurrence == 1 ? name : name + "_" + occurrence;
    }

    /** Replaces a default {@code :name} placeholder with an arbitrary rendered token. */
    private static String replaceDefaultNamedParameterToken(final String sql, final String oldName, final String newToken) {
        StringBuilder sb = null;
        int last = 0;

        for (int i = 0, len = sql.length(); i < len; i++) {
            final int next = skipSqlQuotedOrComment(sql, i);

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

    private static String replaceIbatisParameterName(final String sql, final String oldName, final String newName) {
        StringBuilder sb = null;
        int last = 0;

        for (int start = 0, len = sql.length(); start < len; start++) {
            final int next = skipSqlQuotedOrComment(sql, start);

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
    private static String replaceRenderedNamedParameterToken(final String sql, final String oldToken, final String newToken) {
        StringBuilder sb = null;
        int last = 0;
        final int tokenLength = oldToken.length();

        for (int i = 0, len = sql.length(); i <= len - tokenLength; i++) {
            final int next = skipSqlQuotedOrComment(sql, i);

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

    private static int skipSqlQuotedOrComment(final String sql, final int start) {
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

        if (ch == '#' && isAliasScannerHashCommentStart(sql, start)) {
            int i = start + 1;

            while (i < len && sql.charAt(i) != '\r' && sql.charAt(i) != '\n') {
                i++;
            }

            return i;
        }

        return start;
    }

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
     * @throws IllegalStateException if {@code FOR UPDATE} has already been set on this builder
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
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     */
    public This set(final String expr) {
        return set(Array.asList(expr));
    }

    /**
     * Sets columns for UPDATE operation.
     * <p>Generates parameterized placeholders ({@code ?}, {@code :name}, or {@code #{name}}) based on the SQL policy.
     * If a column name already contains an {@code =} sign, it is treated as a raw SET expression and no placeholder is appended.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update("users")
     *                 .set("firstName", "lastName", "email")
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * // Output: UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?
     * }</pre>
     *
     * @param propOrColumnNames the columns to update
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
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
     *                 .build().query();
     * // Output: UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?
     * }</pre>
     *
     * @param propOrColumnNames the collection of columns to update
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code propOrColumnNames} is {@code null} or empty, or contains a {@code null}, empty, or blank element
     */
    public This set(final Collection<String> propOrColumnNames) {
        checkSqlFragmentsNotBlank(propOrColumnNames, "propOrColumnNames");

        init(false);

        // When set() is chained (called more than once), _sb already has SET content and
        // a comma must separate it from the next assignment list.
        final boolean needsLeadingComma = _setListStarted;
        _setListStarted = true;

        switch (_sqlPolicy) {
            case RAW_SQL:
            case PARAMETERIZED_SQL: {
                int i = needsLeadingComma ? 1 : 0;
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
                int i = needsLeadingComma ? 1 : 0;
                for (final String columnName : propOrColumnNames) {
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
                for (final String columnName : propOrColumnNames) {
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

        return (This) this;
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
     * @throws IllegalArgumentException if {@code props} is {@code null} or empty, or contains a {@code null}, empty, or blank key
     */
    public This set(final Map<String, Object> props) {
        checkSqlFragmentKeysNotBlank(props, "props");

        init(false);

        // When set() is chained (called more than once), _sb already has SET content and
        // a comma must separate it from the next assignment list.
        final boolean needsLeadingComma = _setListStarted;
        _setListStarted = true;

        switch (_sqlPolicy) {
            case RAW_SQL: {
                int i = needsLeadingComma ? 1 : 0;
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
                int i = needsLeadingComma ? 1 : 0;
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
                int i = needsLeadingComma ? 1 : 0;
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
                int i = needsLeadingComma ? 1 : 0;
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
     * Sets properties to update from an entity object, a {@code Map}, or a single column-name {@code String}.
     * For bean entities, properties annotated as {@code @NonUpdatable}, {@code @ReadOnly}, {@code @ReadOnlyId},
     * or {@code @Transient} are automatically excluded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update("account")
     *                 .setEntity(accountEntity)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * }</pre>
     *
     * @param entity the entity object, {@code Map<String, Object>}, or column-name {@code String} containing properties to set
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entity} is {@code null}, or if {@code entity} is a {@code Collection} or array
     *         (use {@link #set(Collection)} or {@link #set(String...)} for column lists)
     */
    public This setEntity(final Object entity) {
        return setEntity(entity, null);
    }

    /**
     * Sets properties to update from an entity object, a {@code Map}, or a single column-name {@code String}.
     *
     * @param entity the entity object, {@code Map<String, Object>}, or column-name {@code String} containing properties to set
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entity} is {@code null}, or if {@code entity} is a {@code Collection} or array
     *         (use {@link #set(Collection)} or {@link #set(String...)} for column lists)
     * @deprecated use {@link #setEntity(Object)}
     */
    @Deprecated
    public This set(final Object entity) {
        return setEntity(entity, null);
    }

    /**
     * Sets properties to update from an entity object, a {@code Map}, or a single column-name {@code String},
     * excluding the specified properties. For bean entities, properties annotated as {@code @NonUpdatable},
     * {@code @ReadOnly}, {@code @ReadOnlyId}, or {@code @Transient} are also excluded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("createdDate", "version");
     * String sql = PSC.update("account")
     *                 .setEntity(accountEntity, excluded)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * }</pre>
     *
     * @param entity the entity object, {@code Map<String, Object>}, or column-name {@code String} containing properties to set
     * @param excludedPropNames property names to exclude from the update (may be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entity} is {@code null}, or if {@code entity} is a {@code Collection} or array
     *         (use {@link #set(Collection)} or {@link #set(String...)} for column lists)
     */
    public This setEntity(final Object entity, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entity, "entity");

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

        if (entity instanceof Collection || entity.getClass().isArray()) {
            throw new IllegalArgumentException(
                    "A Collection or array is not a valid entity for setEntity(...). Use set(Collection<String>) or set(String...) for column lists.");
        }

        final Class<?> entityClass = entity.getClass();
        setEntityClass(entityClass);
        final Collection<String> propNames = QueryUtil.updatePropertyNames(entityClass, excludedPropNames);
        final Map<String, Object> localProps = N.newLinkedHashMap(propNames.size());

        for (final String propName : propNames) {
            localProps.put(propName, _entityInfo.getPropValue(entity, propName));
        }

        return set(localProps);
    }

    /**
     * Sets properties to update from an entity object, a {@code Map}, or a single column-name {@code String},
     * excluding the specified properties.
     *
     * @param entity the entity object, {@code Map<String, Object>}, or column-name {@code String} containing properties to set
     * @param excludedPropNames property names to exclude from the update (may be {@code null})
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entity} is {@code null}, or if {@code entity} is a {@code Collection} or array
     *         (use {@link #set(Collection)} or {@link #set(String...)} for column lists)
     * @deprecated use {@link #setEntity(Object, Set)}
     */
    @Deprecated
    public This set(final Object entity, final Set<String> excludedPropNames) {
        return setEntity(entity, excludedPropNames);
    }

    /**
     * Sets all updatable properties from an entity class for UPDATE operation.
     * Properties marked with {@code @NonUpdatable}, {@code @ReadOnly}, {@code @ReadOnlyId}, or {@code @Transient} annotations are excluded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update("account")
     *                 .setEntity(Account.class)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * // Output: UPDATE account SET id = ?, gui = ?, email_address = ?, first_name = ?, middle_name = ?, last_name = ?, birth_date = ?, status = ?, last_update_time = ?, create_time = ?, contact = ? WHERE id = ?
     * }</pre>
     *
     * @param entityClass the entity class to get properties from
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    public This setEntity(final Class<?> entityClass) {
        return setEntity(entityClass, (Set<String>) null);
    }

    /**
     * Sets all updatable properties from an entity class for UPDATE operation.
     *
     * @param entityClass the entity class to get properties from
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     * @deprecated use {@link #setEntity(Class)}
     */
    @Deprecated
    public This set(final Class<?> entityClass) {
        return setEntity(entityClass);
    }

    /**
     * Sets updatable properties from an entity class for UPDATE operation, excluding specified properties.
     * Properties marked with {@code @NonUpdatable}, {@code @ReadOnly}, {@code @ReadOnlyId}, or {@code @Transient} annotations are automatically excluded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("lastUpdateTime");
     * String sql = PSC.update("account")
     *                 .setEntity(Account.class, excluded)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * // Output: UPDATE account SET id = ?, gui = ?, email_address = ?, first_name = ?, middle_name = ?, last_name = ?, birth_date = ?, status = ?, create_time = ?, contact = ? WHERE id = ?
     * }</pre>
     *
     * @param entityClass the entity class to get properties from
     * @param excludedPropNames additional properties to exclude from the update
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     */
    public This setEntity(final Class<?> entityClass, final Set<String> excludedPropNames) {
        setEntityClass(entityClass);

        return set(QueryUtil.updatePropertyNames(entityClass, excludedPropNames));
    }

    /**
     * Sets updatable properties from an entity class for UPDATE operation, excluding specified properties.
     *
     * @param entityClass the entity class to get properties from
     * @param excludedPropNames additional properties to exclude from the update
     * @return this SqlBuilder instance for method chaining
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}
     * @deprecated use {@link #setEntity(Class, Set)}
     */
    @Deprecated
    public This set(final Class<?> entityClass, final Set<String> excludedPropNames) {
        return setEntity(entityClass, excludedPropNames);
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
     * The builder cannot be reused after calling this method.
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
     *         SET columns)
     */
    public SP build() {
        checkOpen();

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

        return new SP(sql, ImmutableList.wrap(_parameters));
    }

    private void checkOpen() {
        if (_sb == null) {
            throw new IllegalStateException("SqlBuilder is closed and cannot be reused after build() was called");
        }
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
     *     .apply(sp -> jdbcTemplate.query(sp.query(), sp.parameters(), accountRowMapper));
     * }</pre>
     *
     * @param <T> the return type of the function
     * @param <E> the exception type that may be thrown
     * @param function the function to apply to the SP pair
     * @return the result of applying the function
     * @throws E if the function throws an exception
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.Function<? super SP, T, E> function) throws E {
        return function.apply(build());
    }

    /**
     * Applies a bi-function to the SQL string and parameters separately and returns the result.
     * This is useful for executing the SQL directly with a data access framework that takes SQL and parameters separately.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int count = PSC.update("account")
     *     .set("status", "lastLogin")
     *     .where(Filters.lessThan("lastLogin", oneYearAgo))
     *     .apply((sql, params) -> jdbcTemplate.update(sql, params.toArray()));
     * }</pre>
     *
     * @param <T> the return type of the function
     * @param <E> the exception type that may be thrown
     * @param function the bi-function to apply to the SQL and parameters
     * @return the result of applying the function
     * @throws E if the function throws an exception
     */
    @Beta
    public <T, E extends Exception> T apply(final Throwables.BiFunction<? super String, ? super List<Object>, T, E> function) throws E {
        final SP sP = build();

        return function.apply(sP.query, sP.parameters);
    }

    /**
     * Accepts a consumer for the SQL-Parameters pair.
     * This is useful for executing the SQL with a data access framework when no return value is needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PSC.insert("name", "email", "status")
     *    .into("account")
     *    .accept(sp -> jdbcTemplate.update(sp.query(), sp.parameters().toArray()));
     * }</pre>
     *
     * @param <E> the exception type that may be thrown
     * @param consumer the consumer to accept the SP pair
     * @throws E if the consumer throws an exception
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.Consumer<? super SP, E> consumer) throws E {
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
     * @param consumer the bi-consumer to accept the SQL and parameters
     * @throws E if the consumer throws an exception
     */
    @Beta
    public <E extends Exception> void accept(final Throwables.BiConsumer<? super String, ? super List<Object>, E> consumer) throws E {
        final SP sP = build();

        consumer.accept(sP.query, sP.parameters);
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
     * @throws IllegalStateException if the builder has already been closed by a prior call to {@code build()}
     */
    @Beta
    public void debugPrint() {
        N.println(build().query());
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
     *         that no {@code from(...)} ever rendered; or if {@code setForUpdate} is {@code true} for
     *         an UPDATE with no columns staged and no prior {@code set(...)} call
     */
    protected void init(final boolean setForUpdate) {
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
            final String newTableName = _tableName;

            final char[] deleteFromTableChars = tableDeleteFrom.computeIfAbsent(newTableName,
                    n -> (SK.DELETE + SK.SPACE + SK.FROM + SK.SPACE + n).toCharArray());

            _sb.append(deleteFromTableChars);
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
     * @param propValue the new parameter for raw SQL
     */
    protected void setParameterForRawSQL(final Object propValue) {
        if (Filters.QME.equals(propValue)) {
            _sb.append(SK._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendConditionAsParameter((Condition) propValue);
        } else {
            _sb.append(Expression.renderValue(propValue));
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

    private void appendNamedParameter(final String parameterName) {
        final int start = _sb.length();
        _handlerForNamedParameter.accept(_sb, parameterName);

        if (_sb.length() == start) {
            throw new IllegalStateException("The named-parameter handler emitted an empty token for: " + parameterName);
        }

        _renderedNamedParameterTokens.put(parameterName, _sb.substring(start));
    }

    /**
     * Sets the parameter for ibatis named SQL.
     *
     * @param propName the property or parameter name for the ibatis named SQL placeholder
     * @param propValue the value to bind to the ibatis named parameter
     */
    protected void setParameterForIbatisNamedSQL(final String propName, final Object propValue) {
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
     * @param childBuilder the nested or sibling builder to seed
     */
    protected final void seedNamedParameterOccurrences(final AbstractQueryBuilder<?> childBuilder) {
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
     * @param childBuilder the nested or sibling builder whose occurrence counts to adopt
     */
    protected final void adoptNamedParameterOccurrences(final AbstractQueryBuilder<?> childBuilder) {
        _namedParameterNameOccurrences.clear();
        _namedParameterNameOccurrences.putAll(childBuilder._namedParameterNameOccurrences);

        _generatedNamedParameterNames.clear();
        _generatedNamedParameterNames.addAll(childBuilder._generatedNamedParameterNames);

        _renderedNamedParameterTokens.clear();
        _renderedNamedParameterTokens.putAll(childBuilder._renderedNamedParameterTokens);
    }

    /**
     * Normalizes {@code propName} so the remaining text is a valid named-parameter identifier.
     * {@code "u.id"} becomes {@code "id"}, {@code "COUNT(*)"} becomes {@code "COUNT"}, and
     * invalid identifier characters are converted to underscores. {@code null} and empty strings
     * are returned unchanged.
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
     * Appends the values for an INSERT operation, in the iteration order of the map's key set.
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

    /**
     * Appends the given condition to the SQL string builder.
     *
     * @param condition the condition to append
     */
    protected abstract void appendCondition(final Condition cond);

    /**
     * Appends the given condition as a parameter value expression. If the condition is a {@code SubQuery},
     * it is wrapped in parentheses; otherwise, it is appended directly.
     *
     * @param condition the condition to append
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
     *
     * @param expr the string expression to append (must not be {@code null}, empty, or blank)
     * @param isFromAppendColumn {@code true} if the expression originates from an append-column call (applies stricter validation and naming policy conversion),
     *                           {@code false} otherwise
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or if {@code isFromAppendColumn} is {@code true} and {@code expr} contains a SQL comment token
     */
    protected void appendStringExpr(final String expr, final boolean isFromAppendColumn) {
        checkSqlFragmentNotBlank(expr, "expr");

        if (isFromAppendColumn && containsSqlCommentToken(expr)) {
            throw new IllegalArgumentException("SQL comment token is not allowed in column expression: " + expr);
        }

        if (expr.length() < 16) {
            final boolean matched = QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher(expr).matches();

            if (matched) {
                if (isFromAppendColumn) {
                    _sb.append(normalizeColumnName(expr, _namingPolicy));
                } else {
                    _sb.append(normalizeColumnName(_propColumnNameMap, expr));
                }

                return;
            }
        }

        final List<String> words = _tokenizer.parse(expr);

        String word = null;
        for (int i = 0, len = words.size(); i < len; i++) {
            word = words.get(i);

            if (word.isEmpty() || !Strings.isAsciiAlpha(word.charAt(0)) || SqlParser.isFunctionName(words, i)) {
                _sb.append(word);
            } else {
                _sb.append(normalizeColumnName(_propColumnNameMap, word));
            }
        }
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

    private static boolean containsSqlCommentToken(final String expr) {
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
     * Appends a column name to the SQL string builder with full control over aliasing, table prefix, and sub-entity expansion.
     *
     * @param entityClass the entity class for resolving sub-entity properties
     * @param entityInfo the bean info for the entity class, or {@code null}
     * @param propColumnNameMap the property-to-column-name mapping
     * @param tableAlias the table alias to prefix the column name, or {@code null}
     * @param propName the property or column name to append
     * @param propAlias the column alias for the SELECT clause, or {@code null}
     * @param withClassAlias whether to prefix the alias with the class alias
     * @param classAlias the class alias to use when {@code withClassAlias} is {@code true}
     * @param isForSelect whether this column is being appended in a SELECT clause (adds AS alias)
     * @param quotePropAlias whether to wrap the property alias in the dialect's identifier quote
     */
    protected void appendColumnName(final Class<?> entityClass, final BeanInfo entityInfo, final ImmutableMap<String, ColumnInfo> propColumnNameMap,
            final String tableAlias, final String propName, final String propAlias, final boolean withClassAlias, final String classAlias,
            final boolean isForSelect, boolean quotePropAlias) {
        ColumnInfo tp = propColumnNameMap == null ? null : propColumnNameMap.get(propName);

        if (tp != null) {
            if (tp.isUnqualified() && tableAlias != null && !tableAlias.isEmpty()) {
                _sb.append(tableAlias).append(SK._PERIOD);
            }

            _sb.append(tp.columnName());

            if (isForSelect && (withClassAlias || _namingPolicy != NamingPolicy.NO_CHANGE)) {
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

            return;
        }

        if (Strings.isEmpty(propAlias) && entityInfo != null) {
            final PropInfo propInfo = entityInfo.getPropInfo(propName);

            if (propInfo != null && propInfo.isSubEntity) {
                final Class<?> propEntityClass = propInfo.type.isCollection() ? propInfo.type.elementType().javaType() : propInfo.clazz;

                final String propEntityTableAliasOrName = tableAliasOrName(propEntityClass, _namingPolicy);

                final ImmutableMap<String, ColumnInfo> subPropColumnNameMap = propToColumnInfoMap(propEntityClass, _namingPolicy);

                final Collection<String> subSelectPropNames = QueryUtil.selectPropertyNames(propEntityClass, false, null);
                int i = 0;

                for (final String subPropName : subSelectPropNames) {
                    if (i++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    final ColumnInfo subTp = subPropColumnNameMap.get(subPropName);
                    _sb.append(propEntityTableAliasOrName)
                            .append(SK._PERIOD)
                            .append(subTp != null ? subTp.columnName() : normalizeColumnName(subPropName, _namingPolicy));

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
                        _sb.append(propTableAlias).append('.').append(tp.columnName());

                        if (isForSelect && (withClassAlias || _namingPolicy != NamingPolicy.NO_CHANGE)) {
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
                final String alias = propName.substring(selectAlias.aliasStart()).trim();
                Dsl.validateColumnAlias(expression, alias);
                //noinspection ConstantValue
                appendColumnName(entityClass, entityInfo, propColumnNameMap, tableAlias, expression, alias, withClassAlias, classAlias, isForSelect, false);
            } else {
                appendStringExpr(propName, true);

                int idx = -1;
                if (withClassAlias || propName.length() > _sb.length()
                        || (_sb.length() - propName.length() - 1 >= 0 && _sb.charAt(_sb.length() - propName.length() - 1) != _SPACE)
                        || _sb.indexOf(propName, _sb.length() - propName.length()) < 0 || ((idx = propName.indexOf(SK._PERIOD)) > 0
                                && (Strings.isEmpty(tableAlias) || tableAlias.length() != idx || !propName.startsWith(tableAlias)))) {
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

    /**
     * Checks whether the specified entity class has sub-entity properties that should be included in the query.
     *
     * @param entityClass the entity class to check
     * @param includeSubEntityProperties whether sub-entity properties are requested to be included
     * @return {@code true} if sub-entity properties should be included and the entity class has them
     */
    protected static boolean hasSubEntityToInclude(final Class<?> entityClass, final boolean includeSubEntityProperties) {
        return includeSubEntityProperties && N.notEmpty(getSubEntityPropNames(entityClass));
    }

    /**
     * Checks whether the array represents a single inline query rather than a list of column names.
     * Returns {@code true} when the array has exactly one element and that element either starts with
     * the {@code SELECT} keyword (a FROM-less query such as {@code "SELECT 1"} is still a valid query),
     * or contains both a {@code SELECT} and a {@code FROM} keyword (in that order).
     *
     * @param propOrColumnNames array of property or column names to check
     * @return {@code true} if the array contains a single inline query, {@code false} otherwise
     */
    protected static boolean isSubQuery(final String... propOrColumnNames) {
        return isSubQuery(SqlParser.tokenizer(), propOrColumnNames);
    }

    /**
     * Tokenizer-aware implementation used by builders whose dialect supplies a custom tokenizer
     * configuration. Keeping the public-to-subclasses {@link #isSubQuery(String...)} helper static
     * preserves its existing source and binary contract.
     */
    private static boolean isSubQuery(final SqlParser.Tokenizer tokenizer, final String... propOrColumnNames) {
        if (propOrColumnNames.length == 1) {
            final String query = propOrColumnNames[0].trim();
            int index = tokenizer.indexOfToken(query, SK.SELECT, 0, false);

            if (index == 0) {
                return true;
            }

            if (index > 0) {
                index = tokenizer.indexOfToken(query, SK.FROM, index, false);

                return index >= 1;
            }
        }

        return false;
    }

    /**
     * Normalizes a column name according to the specified naming policy.
     * SQL keywords (and any name when the policy is {@code NO_CHANGE}) are returned unchanged.
     * For the {@code CAMEL_CASE} policy, the name is normalized as a bean property name;
     * otherwise it is converted using the naming policy.
     *
     * @param word the column name to normalize
     * @param namingPolicy the naming policy to apply
     * @return the normalized column name
     */
    protected static String normalizeColumnName(final String word, final NamingPolicy namingPolicy) {
        if (sqlKeyWords.contains(word) || namingPolicy == NamingPolicy.NO_CHANGE) {
            return word;
        }
        if (namingPolicy == NamingPolicy.CAMEL_CASE) {
            return Beans.normalizePropName(word);
        }
        return namingPolicy.convert(word);
    }

    /**
     * Normalizes a column name using the property-to-column-name mapping, with support for table alias resolution.
     * Falls back to the static naming policy conversion if no mapping is found.
     *
     * @param propColumnNameMap the property-to-column-name mapping, or {@code null}
     * @param propName the property name to normalize
     * @return the normalized column name, optionally prefixed with a table alias
     */
    protected String normalizeColumnName(final ImmutableMap<String, ColumnInfo> propColumnNameMap, final String propName) {
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
                        return propTableAlias + "." + tp.columnName();
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
     * property names or property-value map. Null values are skipped for bean entities.
     *
     * @param instance the query builder instance to populate
     * @param entity the entity to parse (a column name String, a Map of properties, or a bean object)
     * @param excludedPropNames property names to exclude from the insert, or {@code null}
     * @throws IllegalArgumentException if {@code entity} is an empty Map and no {@code excludedPropNames} are given
     */
    protected static void parseInsertEntity(@SuppressWarnings("rawtypes") final AbstractQueryBuilder instance, final Object entity,
            final Set<String> excludedPropNames) {
        if (entity instanceof String) {
            instance._propOrColumnNames = Array.asList((String) entity);
        } else if (entity instanceof Map) {
            instance._props = new LinkedHashMap<>((Map<String, Object>) entity);

            if (N.isEmpty(excludedPropNames)) {
                N.checkArgument(!instance._props.isEmpty(), "entity map must not be empty");
            } else {
                Maps.removeKeys(instance._props, excludedPropNames);
            }
        } else {
            final Collection<String> propNames = QueryUtil.insertPropertyNames(entity, excludedPropNames);
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
     * @throws IllegalArgumentException if every element is {@code null}; if a map is empty, has a non-string
     *         or blank key, or does not share the same key set as the other map rows; if elements have mixed
     *         types (some {@code Map}, some bean); if bean rows do not have the same runtime class;
     *         or if no bean column remains after all-null/default columns are removed
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
                    N.checkArgument(propName instanceof String, "All keys in batch INSERT maps must be String: " + propName);
                    checkSqlFragmentNotBlank((String) propName, "Batch INSERT map key");
                }

                if (expectedKeys == null) {
                    N.checkArgument(!propsMap.isEmpty(), "Map at first non-null position in propsList must not be empty");
                    expectedKeys = new LinkedHashSet<>(propsMap.keySet());
                } else {
                    // All rows in a batch INSERT must share the same column set; otherwise extra keys are silently
                    // dropped and missing keys produce stray NULL parameters, leading to data loss / corruption.
                    N.checkArgument(propsMap.keySet().equals(expectedKeys),
                            "All non-null Maps in propsList must have the same key set for batch INSERT. Expected: " + expectedKeys + ", current: "
                                    + propsMap.keySet());
                }

                newPropsList.add(new LinkedHashMap<>(propsMap));
            }

            N.checkArgument(N.notEmpty(newPropsList), "All elements in propsList are null");

            return newPropsList;
        }

        N.checkArgument(first.isPresent(), "All elements in propsList are null");

        final Class<?> entityClass = first.get().getClass();
        final Collection<String> propNames = QueryUtil.insertPropertyNames(entityClass, null);
        final BeanInfo firstEntityBeanInfo = ParserUtil.getBeanInfo(entityClass);
        final List<Map<String, Object>> newPropsList = new ArrayList<>(propsList.size());

        for (final Object entity : propsList) {
            if (entity == null) {
                continue;
            }

            final Class<?> currentEntityClass = entity.getClass();
            N.checkArgument(currentEntityClass == entityClass, "All non-null bean entities in propsList must have the same runtime class. Expected: "
                    + entityClass.getName() + ", current: " + currentEntityClass.getName());

            final Map<String, Object> props = N.newLinkedHashMap(propNames.size());

            for (final String propName : propNames) {
                props.put(propName, firstEntityBeanInfo.getPropValue(entity, propName));
            }

            newPropsList.add(props);
        }

        final ImmutableList<String> idPropNameList = firstEntityBeanInfo.idPropNameList;
        final boolean removeDefaultIdValues = N.size(idPropNameList) <= 1
                || Stream.of(newPropsList).allMatch(map -> Stream.of(idPropNameList).allMatch(idPropName -> isDefaultIdPropValue(map.get(idPropName))));

        final List<String> nullPropToRemove = Stream.of(propNames).filter(propName -> Stream.of(newPropsList).allMatch(map -> {
            final Object propValue = map.get(propName);

            return propValue == null
                    || (removeDefaultIdValues && !idPropNameList.isEmpty() && idPropNameList.contains(propName) && isDefaultIdPropValue(propValue));
        })).toList();

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
     */
    protected static void checkMultiSelects(final List<Selection> multiSelects) {
        N.checkArgNotEmpty(multiSelects, "multiSelects");

        for (final Selection selection : multiSelects) {
            N.checkArgNotNull(selection, "Selection can't be null in 'multiSelects'");
            N.checkArgNotNull(selection.entityClass(), "Class can't be null in 'multiSelects'");

            if (N.notEmpty(selection.includedPropNames())) {
                checkSqlFragmentsNotBlank(selection.includedPropNames(), "selection.includedPropNames");
            }

            if (Strings.isNotEmpty(selection.classAlias())) {
                Dsl.validateColumnAlias(selection.entityClass().getSimpleName(), selection.classAlias());
            }
        }
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
     * and any sub-entity tables that need to be joined.
     *
     * @param multiSelects the list of selections defining the tables and their properties
     * @param namingPolicy the naming policy for table name conversion
     * @return the constructed FROM clause string
     */
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

                if (N.notEmpty(selection.includedPropNames()) || selection.includesSubEntityProperties()) {
                    final Class<?> entityClass = selection.entityClass();
                    final Collection<String> selectPropNames = N.notEmpty(selection.includedPropNames()) ? selection.includedPropNames()
                            : QueryUtil.selectPropertyNames(entityClass, selection.includesSubEntityProperties(), selection.excludedPropNames());
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
                            if (!containsSelectedPropOrSubProp(selectPropNames, subEntityPropName)) {
                                continue;
                            }
                        } else if (excludedPropNames != null && excludedPropNames.contains(subEntityPropName)) {
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
     * String sql = sqlPair.query();
     * // "SELECT first_name AS \"firstName\", last_name AS \"lastName\" FROM users WHERE id = ?"
     *
     * List<Object> params = sqlPair.parameters();
     * // [123]
     * }</pre>
     *
     * @param query the generated SQL query string
     * @param parameters the immutable list of parameter values corresponding to placeholders in the SQL
     */
    public record SP(String query, ImmutableList<Object> parameters) {
    }
}
