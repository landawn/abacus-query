package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Binary;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.CrossJoin;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.Exists;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.FullJoin;
import com.landawn.abacus.query.condition.GreaterThanOrEqual;
import com.landawn.abacus.query.condition.GreaterThan;
import com.landawn.abacus.query.condition.GroupBy;
import com.landawn.abacus.query.condition.In;
import com.landawn.abacus.query.condition.InSubQuery;
import com.landawn.abacus.query.condition.InnerJoin;
import com.landawn.abacus.query.condition.IsNotNull;
import com.landawn.abacus.query.condition.IsNull;
import com.landawn.abacus.query.condition.Join;
import com.landawn.abacus.query.condition.LeftJoin;
import com.landawn.abacus.query.condition.LessThanOrEqual;
import com.landawn.abacus.query.condition.LessThan;
import com.landawn.abacus.query.condition.Like;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.NamedProperty;
import com.landawn.abacus.query.condition.NaturalJoin;
import com.landawn.abacus.query.condition.Not;
import com.landawn.abacus.query.condition.NotBetween;
import com.landawn.abacus.query.condition.NotEqual;
import com.landawn.abacus.query.condition.NotExists;
import com.landawn.abacus.query.condition.NotIn;
import com.landawn.abacus.query.condition.NotInSubQuery;
import com.landawn.abacus.query.condition.NotLike;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Or;
import com.landawn.abacus.query.condition.OrderBy;
import com.landawn.abacus.query.condition.RightJoin;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.query.SQLBuilder.PSC;
import com.landawn.abacus.query.SQLBuilder.NSC;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;

/**
 * Test class that verifies ALL Javadoc usage examples from query package source files.
 * Each test method corresponds to a specific Javadoc example.
 */
public class JavadocExamplesQueryTest {

    // ===================== ParsedSql.java Examples =====================

    @Test
    public void testParsedSql_classLevelExample() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
        String parameterized = parsed.parameterizedSql();
        List<String> params = parsed.namedParameters();

        assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterized);
        assertEquals(Arrays.asList("userId", "status"), params);
    }

    @Test
    public void testParsedSql_parseNamedParameters() {
        ParsedSql ps1 = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        assertEquals("SELECT * FROM users WHERE id = ?", ps1.parameterizedSql());
    }

    @Test
    public void testParsedSql_parseIBatisStyle() {
        ParsedSql ps2 = ParsedSql.parse("INSERT INTO users (name, email) VALUES (#{name}, #{email})");
        assertEquals(Arrays.asList("name", "email"), ps2.namedParameters());
    }

    @Test
    public void testParsedSql_parseJdbcPlaceholders() {
        ParsedSql ps3 = ParsedSql.parse("UPDATE users SET status = ? WHERE id = ?");
        assertEquals(2, ps3.parameterCount());
    }

    @Test
    public void testParsedSql_sql() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
        String original = parsed.sql();
        assertEquals("SELECT * FROM users WHERE id = :userId", original);
    }

    @Test
    public void testParsedSql_parameterizedSql() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
        String sql = parsed.parameterizedSql();
        assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", sql);
    }

    @Test
    public void testParsedSql_parameterizedSqlForJdbc() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :name");
        String jdbcSql = parsed.parameterizedSql(false);
        assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", jdbcSql);
    }

    @Test
    public void testParsedSql_parameterizedSqlForCouchbase() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :name");
        String couchbaseSql = parsed.parameterizedSql(true);
        assertEquals("SELECT * FROM users WHERE id = $1 AND name = $2", couchbaseSql);
    }

    @Test
    public void testParsedSql_namedParameters() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");
        ImmutableList<String> params = parsed.namedParameters();
        assertEquals(Arrays.asList("name", "minAge"), params);

        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        ImmutableList<String> params2 = parsed2.namedParameters();
        assertTrue(params2.isEmpty());
    }

    @Test
    public void testParsedSql_namedParametersWithCouchbase() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");

        ImmutableList<String> params = parsed.namedParameters(false);
        assertEquals(Arrays.asList("name", "minAge"), params);

        ImmutableList<String> cbParams = parsed.namedParameters(true);
        assertEquals(Arrays.asList("name", "minAge"), cbParams);

        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
        ImmutableList<String> cbParams2 = parsed2.namedParameters(true);
        assertTrue(cbParams2.isEmpty());
    }

    @Test
    public void testParsedSql_parameterCount() {
        ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, email, age) VALUES (:name, :email, :age)");
        int count = parsed.parameterCount();
        assertEquals(3, count);

        ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
        int count2 = parsed2.parameterCount();
        assertEquals(0, count2);
    }

    @Test
    public void testParsedSql_parameterCountWithCouchbase() {
        ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");
        int count = parsed.parameterCount(false);
        assertEquals(2, count);
        int cbCount = parsed.parameterCount(true);
        assertEquals(2, cbCount);
    }

    // ===================== QueryUtil.java Examples =====================

    @Test
    public void testQueryUtil_patternForAlphanumericColumnName() {
        boolean isValid = QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("column_name").matches();
        assertTrue(isValid);
        boolean isInvalid = QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("column name").matches();
        assertFalse(isInvalid);
    }

    @Test
    public void testQueryUtil_repeatQM() {
        String placeholders = QueryUtil.repeatQM(3);
        assertEquals("?, ?, ?", placeholders);
        String sql = "INSERT INTO users (name, email, age) VALUES (" + placeholders + ")";
        assertEquals("INSERT INTO users (name, email, age) VALUES (?, ?, ?)", sql);
    }

    @Test
    public void testQueryUtil_repeatQM_zero() {
        String placeholders = QueryUtil.repeatQM(0);
        assertEquals("", placeholders);
    }

    // ===================== SortDirection.java Examples =====================

    @Test
    public void testSortDirection_classLevelExample() {
        SortDirection direction = SortDirection.ASC;
        assertTrue(direction.isAscending());

        String sql = "SELECT * FROM users ORDER BY name " + SortDirection.DESC;
        assertTrue(sql.contains("DESC"));
    }

    @Test
    public void testSortDirection_isAscending() {
        SortDirection direction = SortDirection.ASC;
        boolean ascending = direction.isAscending();
        assertTrue(ascending);

        SortDirection descDirection = SortDirection.DESC;
        boolean descAscending = descDirection.isAscending();
        assertFalse(descAscending);
    }

    // ===================== SQLMapper.java Examples =====================

    @Test
    public void testSQLMapper_addAndGet() {
        SQLMapper mapper = new SQLMapper();
        ParsedSql parsedSql = ParsedSql.parse("select * from users where id = ?");
        mapper.add("findUserById", parsedSql);

        ParsedSql retrieved = mapper.get("findUserById");
        assertNotNull(retrieved);
        assertEquals("select * from users where id = ?", retrieved.parameterizedSql());
    }

    @Test
    public void testSQLMapper_addWithAttrs() {
        SQLMapper mapper = new SQLMapper();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("batchSize", "100");
        attrs.put("timeout", "30");
        mapper.add("insertUser", "insert into users (id, name) values (?, ?)", attrs);

        ParsedSql sql = mapper.get("insertUser");
        assertNotNull(sql);

        ImmutableMap<String, String> retrievedAttrs = mapper.getAttributes("insertUser");
        assertNotNull(retrievedAttrs);
        assertEquals("100", retrievedAttrs.get("batchSize"));
        assertEquals("30", retrievedAttrs.get("timeout"));
    }

    @Test
    public void testSQLMapper_getReturnsNull() {
        SQLMapper mapper = new SQLMapper();
        ParsedSql unknown = mapper.get("nonExistentId");
        assertNull(unknown);
    }

    @Test
    public void testSQLMapper_remove() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("deprecatedQuery", "select 1", null);
        assertNotNull(mapper.get("deprecatedQuery"));
        mapper.remove("deprecatedQuery");
        boolean removed = mapper.get("deprecatedQuery") == null;
        assertTrue(removed);
    }

    @Test
    public void testSQLMapper_copy() {
        SQLMapper original = new SQLMapper();
        original.add("query1", "select 1", null);
        SQLMapper copy = original.copy();
        copy.add("newQuery", ParsedSql.parse("SELECT 1"));
        boolean originalHasIt = original.get("newQuery") != null;
        boolean copyHasIt = copy.get("newQuery") != null;
        assertFalse(originalHasIt);
        assertTrue(copyHasIt);
    }

    @Test
    public void testSQLMapper_isEmpty() {
        SQLMapper emptyMapper = new SQLMapper();
        boolean empty = emptyMapper.isEmpty();
        assertTrue(empty);
    }

    @Test
    public void testSQLMapper_keySet() {
        SQLMapper mapper = new SQLMapper();
        mapper.add("findUser", "select * from users where id = ?", null);
        mapper.add("updateUser", "update users set name = ? where id = ?", null);
        Set<String> sqlIds = mapper.keySet();
        assertTrue(sqlIds.contains("findUser"));
        assertTrue(sqlIds.contains("updateUser"));
        assertEquals(2, sqlIds.size());
    }

    // ===================== SQLParser.java Examples =====================

    @Test
    public void testSQLParser_classLevelExample() {
        String sql = "SELECT * FROM users WHERE age > 25 ORDER BY name";
        List<String> words = SQLParser.parse(sql);
        assertNotNull(words);
        assertFalse(words.isEmpty());
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("users"));
        assertTrue(words.contains("WHERE"));
        // Javadoc shows "ORDER" and "BY" as separate tokens in parse() output
        assertTrue(words.contains("ORDER"));
        assertTrue(words.contains("BY"));
        assertTrue(words.contains("name"));
    }

    @Test
    public void testSQLParser_parse() {
        List<String> words = SQLParser.parse("SELECT name, age FROM users WHERE age >= 18");
        assertNotNull(words);
        assertTrue(words.contains("SELECT"));
        assertTrue(words.contains("name"));
        assertTrue(words.contains(","));
        assertTrue(words.contains("age"));
        assertTrue(words.contains("FROM"));
        assertTrue(words.contains("users"));
        assertTrue(words.contains("WHERE"));
        assertTrue(words.contains(">="));
        assertTrue(words.contains("18"));
    }

    @Test
    public void testSQLParser_indexOfWord() {
        String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
        int index = SQLParser.indexOfWord(sql, "ORDER BY", 0, false);
        assertTrue(index >= 0, "ORDER BY should be found in the SQL");
        int whereIndex = SQLParser.indexOfWord(sql, "WHERE", 0, false);
        assertTrue(whereIndex >= 0, "WHERE should be found in the SQL");
        assertTrue(whereIndex < index, "WHERE should come before ORDER BY");
    }

    @Test
    public void testSQLParser_nextWord() {
        String sql = "SELECT   name,   age FROM users";
        String word1 = SQLParser.nextWord(sql, 6);
        assertEquals("name", word1);
        String word2 = SQLParser.nextWord(sql, 13);
        assertEquals(",", word2);
        String word3 = SQLParser.nextWord(sql, 14);
        assertEquals("age", word3);
    }

    @Test
    public void testSQLParser_registerSeparatorChar() {
        SQLParser.registerSeparator('$');
        List<String> words = SQLParser.parse("SELECT$FROM$users");
        assertNotNull(words);
        assertTrue(words.contains("$"));
    }

    @Test
    public void testSQLParser_registerSeparatorString() {
        SQLParser.registerSeparator("<=>");
        SQLParser.registerSeparator("::");
        // Just verify no exception
    }

    // ===================== SQLOperation.java Examples =====================

    @Test
    public void testSQLOperation_classLevelExample() {
        String sql = "SELECT * FROM users";
        String firstWord = sql.trim().split("\\s+")[0].toUpperCase();
        SQLOperation op = SQLOperation.of(firstWord);
        assertEquals(SQLOperation.SELECT, op);
    }

    @Test
    public void testSQLOperation_of() {
        SQLOperation selectOp = SQLOperation.of("SELECT");
        assertEquals(SQLOperation.SELECT, selectOp);
        SQLOperation insertOp = SQLOperation.of("INSERT");
        assertEquals(SQLOperation.INSERT, insertOp);
        SQLOperation mergeOp = SQLOperation.of("MERGE");
        assertEquals(SQLOperation.MERGE, mergeOp);
        SQLOperation unknownOp = SQLOperation.of("TRUNCATE");
        assertNull(unknownOp);
    }

    @Test
    public void testSQLOperation_sqlText() {
        SQLOperation op = SQLOperation.SELECT;
        String sqlKeyword = op.getName();
        assertEquals("SELECT", sqlKeyword);
        SQLOperation txOp = SQLOperation.BEGIN_TRANSACTION;
        String txText = txOp.getName();
        assertEquals("BEGIN TRANSACTION", txText);
    }

    // ===================== SK.java Examples =====================

    @Test
    public void testSK_classLevelExample_buildingQuery() {
        String query = SK.SELECT + SK.SPACE + "*" + SK.SPACE + SK.FROM + SK.SPACE + "users";
        assertEquals("SELECT * FROM users", query);
    }

    @Test
    public void testSK_classLevelExample_buildingCSV() {
        String csv = "John" + SK.COMMA_SPACE + "Doe" + SK.COMMA_SPACE + "30";
        assertEquals("John, Doe, 30", csv);
    }

    @Test
    public void testSK_classLevelExample_sqlOperators() {
        String condition = "age" + SK.SPACE + SK.GREATER_THAN_OR_EQUAL + SK.SPACE + "18";
        assertEquals("age >= 18", condition);
    }

    @Test
    public void testSK_constants() {
        assertEquals(" ", SK.SPACE);
        assertEquals(".", SK.PERIOD);
        assertEquals(",", SK.COMMA);
        assertEquals(", ", SK.COMMA_SPACE);
        assertEquals(":", SK.COLON);
        assertEquals(";", SK.SEMICOLON);
        assertEquals("?", SK.QUESTION_MARK);
        assertEquals("(", SK.PARENTHESES_L);
        assertEquals(")", SK.PARENTHESES_R);
        assertEquals("=", SK.EQUAL);
        assertEquals("!=", SK.NOT_EQUAL);
        assertEquals("<>", SK.NOT_EQUAL_ANSI);
        assertEquals(">=", SK.GREATER_THAN_OR_EQUAL);
        assertEquals("<=", SK.LESS_THAN_OR_EQUAL);
        assertEquals("SELECT", SK.SELECT);
        assertEquals("INSERT", SK.INSERT);
        assertEquals("UPDATE", SK.UPDATE);
        assertEquals("DELETE", SK.DELETE);
        assertEquals("FROM", SK.FROM);
        assertEquals("WHERE", SK.WHERE);
        assertEquals("AND", SK.AND);
        assertEquals("OR", SK.OR);
        assertEquals("ORDER BY", SK.ORDER_BY);
        assertEquals("GROUP BY", SK.GROUP_BY);
        assertEquals("LEFT JOIN", SK.LEFT_JOIN);
        assertEquals("INNER JOIN", SK.INNER_JOIN);
        assertEquals("IS NULL", SK.IS_NULL);
        assertEquals("IS NOT NULL", SK.IS_NOT_NULL);
        assertEquals("NOT IN", SK.NOT_IN);
        assertEquals("BETWEEN", SK.BETWEEN);
        assertEquals("LIKE", SK.LIKE);
    }

    // ===================== Selection.java Examples =====================

    @Test
    public void testSelection_simpleSelection() {
        Selection userSelection = new Selection().entityClass(String.class).selectPropNames(Arrays.asList("id", "name", "email"));
        assertNotNull(userSelection);
        assertEquals(String.class, userSelection.entityClass());
        assertEquals(Arrays.asList("id", "name", "email"), userSelection.selectPropNames());
    }

    @Test
    public void testSelection_withAliases() {
        Selection orderSelection = new Selection().entityClass(Object.class)
                .tableAlias("o")
                .classAlias("order")
                .includeSubEntityProperties(true)
                .excludedPropNames(Set.of("internalNotes"));
        assertEquals("o", orderSelection.tableAlias());
        assertEquals("order", orderSelection.classAlias());
        assertTrue(orderSelection.includeSubEntityProperties());
        assertTrue(orderSelection.excludedPropNames().contains("internalNotes"));
    }

    @Test
    public void testSelection_multiSelectionBuilder() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(Object.class, "u", "user").add(Object.class, "a", "address").build();
        assertEquals(2, selections.size());
        assertEquals("u", selections.get(0).tableAlias());
        assertEquals("user", selections.get(0).classAlias());
        assertEquals("a", selections.get(1).tableAlias());
        assertEquals("address", selections.get(1).classAlias());
    }

    @Test
    public void testSelection_multiSelectionBuilderWithProps() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(Object.class, "u", "user", Arrays.asList("id", "name")).build();
        assertEquals(1, selections.size());
        assertEquals(Arrays.asList("id", "name"), selections.get(0).selectPropNames());
    }

    @Test
    public void testSelection_multiSelectionBuilderSimple() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(Object.class).build();
        assertEquals(1, selections.size());
        assertNull(selections.get(0).tableAlias());
    }

    @Test
    public void testSelection_multiSelectionBuilderWithExcluded() {
        List<Selection> selections = Selection.multiSelectionBuilder().add(Object.class, true, Set.of("password", "internalNotes")).build();
        assertEquals(1, selections.size());
        assertTrue(selections.get(0).includeSubEntityProperties());
        assertTrue(selections.get(0).excludedPropNames().contains("password"));
    }

    // ===================== Filters.java Examples (20+ representative) =====================

    @Test
    public void testFilters_alwaysTrue() {
        Condition condition = Filters.alwaysTrue();
        assertNotNull(condition);
    }

    @Test
    public void testFilters_not() {
        Like likeCondition = Filters.like("name", "%test%");
        Not notLike = Filters.not(likeCondition);
        assertNotNull(notLike);

        Not notIn = Filters.not(Filters.in("status", Arrays.asList("inactive", "deleted")));
        assertNotNull(notIn);

        Not notBetween = Filters.not(Filters.between("age", 18, 65));
        assertNotNull(notBetween);

        Not complexNot = Filters.not(Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.like("email", "%@company.com")));
        assertNotNull(complexNot);
    }

    @Test
    public void testFilters_namedProperty() {
        NamedProperty prop = Filters.namedProperty("user_name");
        assertNotNull(prop);
        assertEquals("user_name", prop.propName());
    }

    @Test
    public void testFilters_expr() {
        Expression expr = Filters.expr("UPPER(name) = 'JOHN'");
        assertNotNull(expr);
        assertEquals("UPPER(name) = 'JOHN'", expr.getLiteral());
    }

    @Test
    public void testFilters_binary() {
        Binary condition = Filters.binary("price", Operator.GREATER_THAN, 100);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_equal() {
        Equal condition = Filters.equal("username", "john_doe");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_equalParameterized() {
        Equal condition = Filters.equal("user_id");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eq() {
        Equal condition = Filters.eq("status", "active");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eqParameterized() {
        Equal condition = Filters.eq("email");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eqAnyOf_map() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", "John");
        props.put("email", "john@example.com");
        Or condition = Filters.eqAnyOf(props);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eqAnyOf_twoProps() {
        Or condition = Filters.eqAnyOf("name", "John", "email", "john@example.com");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eqAnyOf_threeProps() {
        Or condition = Filters.eqAnyOf("status", "active", "type", "premium", "verified", true);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eqAnd_map() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("status", "active");
        props.put("type", "premium");
        And condition = Filters.eqAnd(props);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eqAnd_twoProps() {
        And condition = Filters.eqAnd("status", "active", "type", "premium");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eqAnd_threeProps() {
        And condition = Filters.eqAnd("status", "active", "type", "premium", "verified", true);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_gtAndLt() {
        And condition = Filters.gtAndLt("age", 18, 65);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_gtAndLt_parameterized() {
        And condition = Filters.gtAndLt("price");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_geAndLt() {
        And condition = Filters.geAndLt("price", 100, 500);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_geAndLe() {
        And condition = Filters.geAndLe("date", "2023-01-01", "2023-12-31");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_gtAndLe() {
        And condition = Filters.gtAndLe("score", 0, 100);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notEqual() {
        NotEqual condition = Filters.notEqual("status", "deleted");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_ne() {
        NotEqual condition = Filters.ne("status", "inactive");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_greaterThan() {
        GreaterThan condition = Filters.greaterThan("age", 18);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_gt() {
        GreaterThan condition = Filters.gt("price", 100);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_greaterThanOrEqual() {
        GreaterThanOrEqual condition = Filters.greaterThanOrEqual("score", 60);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_ge() {
        GreaterThanOrEqual condition = Filters.ge("age", 18);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_lessThan() {
        LessThan condition = Filters.lt("price", 1000);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_lessThanOrEqual() {
        LessThanOrEqual condition = Filters.le("age", 65);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_between() {
        Between condition = Filters.between("price", 100.0, 500.0);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notBetween() {
        NotBetween condition = Filters.notBetween("age", 0, 17);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_like() {
        Like condition = Filters.like("email", "%@company.com");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notLike() {
        NotLike condition = Filters.notLike("name", "%test%");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_isNull() {
        IsNull condition = Filters.isNull("optional_field");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_isNotNull() {
        IsNotNull condition = Filters.isNotNull("required_field");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_in() {
        In condition = Filters.in("status", Arrays.asList("PENDING", "APPROVED", "ACTIVE"));
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notIn() {
        NotIn condition = Filters.notIn("status", Arrays.asList("deleted", "banned"));
        assertNotNull(condition);
    }

    @Test
    public void testFilters_and() {
        And condition = Filters.and(Filters.eq("department", "Engineering"), Filters.or(Filters.gt("salary", 75000), Filters.eq("level", "Senior")),
                Filters.isNotNull("manager_id"));
        assertNotNull(condition);
    }

    @Test
    public void testFilters_or() {
        Or condition = Filters.or(Filters.eq("status", "active"), Filters.eq("status", "pending"));
        assertNotNull(condition);
    }

    @Test
    public void testFilters_contains() {
        Like condition = Filters.contains("name", "John");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_startsWith() {
        Like condition = Filters.startsWith("name", "John");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_endsWith() {
        Like condition = Filters.endsWith("email", "@company.com");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_exists() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = customers.id AND status = 'COMPLETED'");
        Exists hasOrders = Filters.exists(subQuery);
        assertNotNull(hasOrders);
    }

    @Test
    public void testFilters_inSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM active_users");
        InSubQuery condition = Filters.in("id", subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notInSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM blocked_users");
        NotInSubQuery condition = Filters.notIn("id", subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_subQuery() {
        SubQuery sq = Filters.subQuery("SELECT COUNT(*) FROM orders WHERE user_id = users.id");
        assertNotNull(sq);
    }

    @Test
    public void testFilters_where() {
        Where where = Filters.where("status = 'active'");
        assertNotNull(where);
    }

    @Test
    public void testFilters_groupBy() {
        GroupBy groupBy = Filters.groupBy("department");
        assertNotNull(groupBy);
    }

    @Test
    public void testFilters_orderBy() {
        OrderBy orderBy = Filters.orderBy("name");
        assertNotNull(orderBy);
    }

    @Test
    public void testFilters_limit() {
        Limit limit = Filters.limit(10);
        assertNotNull(limit);
    }

    @Test
    public void testFilters_limitWithOffset() {
        Limit limit = Filters.limit(10, 20);
        assertNotNull(limit);
    }

    @Test
    public void testFilters_join() {
        Join join = Filters.join("orders ON users.id = orders.user_id");
        assertNotNull(join);
    }

    @Test
    public void testFilters_leftJoin() {
        LeftJoin leftJoin = Filters.leftJoin("orders ON users.id = orders.user_id");
        assertNotNull(leftJoin);
    }

    @Test
    public void testFilters_rightJoin() {
        RightJoin rightJoin = Filters.rightJoin("orders ON users.id = orders.user_id");
        assertNotNull(rightJoin);
    }

    @Test
    public void testFilters_innerJoin() {
        InnerJoin innerJoin = Filters.innerJoin("orders ON users.id = orders.user_id");
        assertNotNull(innerJoin);
    }

    @Test
    public void testFilters_fullJoin() {
        FullJoin fullJoin = Filters.fullJoin("orders ON users.id = orders.user_id");
        assertNotNull(fullJoin);
    }

    @Test
    public void testFilters_crossJoin() {
        CrossJoin crossJoin = Filters.crossJoin("products");
        assertNotNull(crossJoin);
    }

    @Test
    public void testFilters_naturalJoin() {
        NaturalJoin naturalJoin = Filters.naturalJoin("department");
        assertNotNull(naturalJoin);
    }

    // ===================== DynamicSQLBuilder.java Examples (10+ representative) =====================

    @Test
    public void testDynamicSQLBuilder_classLevelExample() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("id", "user_id").append("name");
        b.from().append("users", "u");
        b.where().append("u.active = ?").and("u.age > ?");
        b.orderBy().append("u.name ASC");
        b.limit(10);
        String sql = b.build();
        assertEquals("SELECT id AS user_id, name FROM users u WHERE u.active = ? AND u.age > ? ORDER BY u.name ASC LIMIT 10", sql);
    }

    @Test
    public void testDynamicSQLBuilder_create() {
        DynamicSQLBuilder builder = DynamicSQLBuilder.create();
        assertNotNull(builder);
    }

    @Test
    public void testDynamicSQLBuilder_selectAppend() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("id").append("name", "user_name");
        b.from().append("users");
        String sql = b.build();
        assertEquals("SELECT id, name AS user_name FROM users", sql);
    }

    @Test
    public void testDynamicSQLBuilder_fromWithJoin() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
        String sql = b.build();
        assertTrue(sql.contains("LEFT JOIN orders o ON u.id = o.user_id"));
    }

    @Test
    public void testDynamicSQLBuilder_where() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users");
        b.where().append("status = ?").and("created_date > ?");
        String sql = b.build();
        assertTrue(sql.contains("WHERE status = ? AND created_date > ?"));
    }

    @Test
    public void testDynamicSQLBuilder_groupBy() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("department").append("COUNT(*)");
        b.from().append("employees");
        b.groupBy().append("department");
        String sql = b.build();
        assertTrue(sql.contains("GROUP BY department"));
    }

    @Test
    public void testDynamicSQLBuilder_having() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("department").append("COUNT(*)");
        b.from().append("employees");
        b.groupBy().append("department");
        b.having().append("COUNT(*) > ?");
        String sql = b.build();
        assertTrue(sql.contains("HAVING COUNT(*) > ?"));
    }

    @Test
    public void testDynamicSQLBuilder_orderBy() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users");
        b.orderBy().append("created_date DESC").append("name ASC");
        String sql = b.build();
        assertTrue(sql.contains("ORDER BY created_date DESC, name ASC"));
    }

    @Test
    public void testDynamicSQLBuilder_limitIntInt() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users");
        b.limit(20, 10);
        String sql = b.build();
        assertTrue(sql.contains("LIMIT 20, 10"));
    }

    @Test
    public void testDynamicSQLBuilder_offsetAndFetch() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users");
        b.offsetRows(20).fetchNextRows(10);
        String sql = b.build();
        assertTrue(sql.contains("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"));
    }

    @Test
    public void testDynamicSQLBuilder_fetchFirst() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users");
        b.fetchFirstRows(10);
        String sql = b.build();
        assertTrue(sql.contains("FETCH FIRST 10 ROWS ONLY"));
    }

    @Test
    public void testDynamicSQLBuilder_union() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("id").append("name");
        b.from().append("active_users");
        b.union("SELECT id, name FROM archived_users");
        String sql = b.build();
        assertTrue(sql.contains("UNION SELECT id, name FROM archived_users"));
    }

    @Test
    public void testDynamicSQLBuilder_unionAll() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("id").append("name");
        b.from().append("users");
        b.unionAll("SELECT id, name FROM temp_users");
        String sql = b.build();
        assertTrue(sql.contains("UNION ALL SELECT id, name FROM temp_users"));
    }

    @Test
    public void testDynamicSQLBuilder_build() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users");
        b.where().append("active = true");
        String sql = b.build();
        assertEquals("SELECT * FROM users WHERE active = true", sql);
    }

    @Test
    public void testDynamicSQLBuilder_selectAppendCollection() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append(Arrays.asList("id", "name", "email"));
        b.from().append("users");
        String sql = b.build();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    public void testDynamicSQLBuilder_selectAppendIf() {
        boolean includeSalary = true;
        boolean includeBonus = false;
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("id").appendIf(includeSalary, "salary").appendIf(includeBonus, "bonus");
        b.from().append("employees");
        String sql = b.build();
        assertTrue(sql.contains("salary"));
        assertFalse(sql.contains("bonus"));
    }

    @Test
    public void testDynamicSQLBuilder_intersect() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("user_id");
        b.from().append("all_users");
        b.intersect("SELECT user_id FROM premium_users");
        String sql = b.build();
        assertTrue(sql.contains("INTERSECT SELECT user_id FROM premium_users"));
    }

    @Test
    public void testDynamicSQLBuilder_except() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("user_id");
        b.from().append("all_users");
        b.except("SELECT user_id FROM blocked_users");
        String sql = b.build();
        assertTrue(sql.contains("EXCEPT SELECT user_id FROM blocked_users"));
    }

    @Test
    public void testDynamicSQLBuilder_minus() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("user_id");
        b.from().append("all_users");
        b.minus("SELECT user_id FROM inactive_users");
        String sql = b.build();
        assertTrue(sql.contains("MINUS SELECT user_id FROM inactive_users"));
    }

    @Test
    public void testDynamicSQLBuilder_fromAppendWithAlias() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users", "u");
        String sql = b.build();
        assertEquals("SELECT * FROM users u", sql);
    }

    @Test
    public void testDynamicSQLBuilder_fromInnerJoin() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("u.id").append("p.name");
        b.from().append("users", "u").innerJoin("products p", "u.id = p.user_id");
        String sql = b.build();
        assertTrue(sql.contains("INNER JOIN products p ON u.id = p.user_id"));
    }

    @Test
    public void testDynamicSQLBuilder_fromRightJoin() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users", "u").rightJoin("orders o", "u.id = o.user_id");
        String sql = b.build();
        assertTrue(sql.contains("RIGHT JOIN orders o ON u.id = o.user_id"));
    }

    @Test
    public void testDynamicSQLBuilder_fromFullJoin() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users", "u").fullJoin("orders o", "u.id = o.user_id");
        String sql = b.build();
        assertTrue(sql.contains("FULL JOIN orders o ON u.id = o.user_id"));
    }

    @Test
    public void testDynamicSQLBuilder_limitByRowNum() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users");
        b.limitByRowNum(10);
        String sql = b.build();
        assertTrue(sql.contains("ROWNUM <= 10"));
    }

    @Test
    public void testDynamicSQLBuilder_selectAppendIfOrElse() {
        DynamicSQLBuilder b1 = DynamicSQLBuilder.create();
        b1.select().appendIfOrElse(true, "first_name || ' ' || last_name AS full_name", "first_name");
        b1.from().append("users");
        String sql1 = b1.build();
        assertTrue(sql1.contains("first_name || ' ' || last_name AS full_name"));

        DynamicSQLBuilder b2 = DynamicSQLBuilder.create();
        b2.select().appendIfOrElse(false, "first_name || ' ' || last_name AS full_name", "first_name");
        b2.from().append("users");
        String sql2 = b2.build();
        assertTrue(sql2.contains("SELECT first_name FROM"));
        assertFalse(sql2.contains("full_name"));
    }

    @Test
    public void testDynamicSQLBuilder_whereOr() {
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users");
        b.where().append("status = 'active'").or("role = 'admin'");
        String sql = b.build();
        assertTrue(sql.contains("WHERE status = 'active' OR role = 'admin'"));
    }

    @Test
    public void testDynamicSQLBuilder_whereAppendIf() {
        boolean filterByStatus = true;
        boolean filterByRole = false;
        DynamicSQLBuilder b = DynamicSQLBuilder.create();
        b.select().append("*");
        b.from().append("users");
        b.where().append("1 = 1").appendIf(filterByStatus, "AND status = 'active'").appendIf(filterByRole, "AND role = 'admin'");
        String sql = b.build();
        assertTrue(sql.contains("status = 'active'"));
        assertFalse(sql.contains("role = 'admin'"));
    }

    // ===================== AbstractQueryBuilder / SQLBuilder Examples (20+ representative) =====================

    @Test
    public void testSQLBuilder_PSC_simpleSelect() {
        String sql = PSC.select("id", "name").from("account").where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM account"));
    }

    @Test
    public void testSQLBuilder_PSC_updateWithConditions() {
        String sql = PSC.update("account").set("name", "status").where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("UPDATE account"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testSQLBuilder_PSC_deleteFrom() {
        String sql = PSC.deleteFrom("account").where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM account"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithWhere() {
        String sql = PSC.select("id", "name").from("users").where(Filters.gt("age", 18)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("FROM users"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithJoin() {
        String sql = PSC.select("u.id", "u.name", "o.total").from("users u").leftJoin("orders o").on("u.id = o.user_id").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithGroupBy() {
        String sql = PSC.select("department", "COUNT(*) AS cnt").from("employees").groupBy("department").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("GROUP BY"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithOrderBy() {
        String sql = PSC.select("id", "name").from("users").orderBy("name").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithLimit() {
        String sql = PSC.select("id", "name").from("users").limit(10).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithBetween() {
        String sql = PSC.select("id", "name", "age").from("users").where(Filters.between("age", 18, 65)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("BETWEEN"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithIn() {
        String sql = PSC.select("id", "name").from("users").where(Filters.in("status", Arrays.asList("active", "pending"))).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("IN"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithLike() {
        String sql = PSC.select("id", "name", "email").from("users").where(Filters.like("email", "%@company.com")).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("LIKE"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithIsNull() {
        String sql = PSC.select("id", "name").from("users").where(Filters.isNull("deleted_at")).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithAndOr() {
        String sql = PSC.select("id", "name")
                .from("users")
                .where(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.gt("age", 18), Filters.eq("verified", true))))
                .sql();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithRightJoin() {
        String sql = PSC.select("u.id", "o.total").from("users u").rightJoin("orders o").on("u.id = o.user_id").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithFullJoin() {
        String sql = PSC.select("u.id", "o.total").from("users u").fullJoin("orders o").on("u.id = o.user_id").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithInnerJoin() {
        String sql = PSC.select("u.id", "o.total").from("users u").innerJoin("orders o").on("u.id = o.user_id").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithCrossJoin() {
        String sql = PSC.select("u.id", "p.name").from("users u").crossJoin("products p").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithHaving() {
        String sql = PSC.select("department", "COUNT(*) AS cnt").from("employees").groupBy("department").having(Filters.expr("COUNT(*) > 5")).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("HAVING"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithUnion() {
        String sql = PSC.select("id", "name").from("active_users").union(PSC.select("id", "name").from("archived_users")).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("UNION"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithUnionAll() {
        String sql = PSC.select("id", "name").from("active_users").unionAll(PSC.select("id", "name").from("temp_users")).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("UNION ALL"));
    }

    @Test
    public void testSQLBuilder_NSC_simpleSelect() {
        String sql = NSC.select("id", "name").from("account").where(Filters.eq("id", 1)).sql();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM account"));
    }

    @Test
    public void testSQLBuilder_PSC_insertIntoValues() {
        String sql = PSC.insert("id", "name", "email").into("users").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO users"));
        assertTrue(sql.contains("VALUES"));
    }

    @Test
    public void testSQLBuilder_PSC_selectDistinct() {
        String sql = PSC.select("DISTINCT department").from("employees").sql();
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testSQLBuilder_PSC_selectWithOffsetAndLimit() {
        String sql = PSC.select("id", "name").from("users").offset(10).limit(5).sql();
        assertNotNull(sql);
    }
}
