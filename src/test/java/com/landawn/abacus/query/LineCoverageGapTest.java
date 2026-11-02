package com.landawn.abacus.query;

import static com.landawn.abacus.query.Dsl.NSC;
import static com.landawn.abacus.query.Dsl.PSC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.query.SqlDialect.ProductInfo;
import com.landawn.abacus.query.condition.Binary;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.In;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SqlExpression;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Exercises previously untested branches that the existing suites left uncovered.
 * Each case asserts the contract of that branch (rejection message, equality, or rendered SQL).
 */
public class LineCoverageGapTest extends TestBase {

    @Test
    void testAppendRejectsUnterminatedBlockCommentAndKeepsTheBuilder() {
        final SqlBuilder builder = PSC.select("id").from("account");
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> builder.append("frag /* open"));
        assertTrue(failure.getMessage().contains("unterminated block comment"), failure.getMessage());

        final String sql = builder.where(Filters.eq("id", 1)).build().query();
        assertEquals("SELECT id FROM account WHERE id = ?", sql);
    }

    @Test
    void testBinaryInExpressionEqualityAndHash() throws Exception {
        final SqlExpression shared = new SqlExpression("(SELECT 1)");
        final Binary left = new Binary("id", Operator.IN, shared);
        final Binary sameReference = new Binary("id", Operator.IN, shared);
        final Binary different = new Binary("id", Operator.IN, new SqlExpression("(SELECT 2)"));

        assertEquals(left, sameReference);
        assertEquals(left.hashCode(), sameReference.hashCode());
        assertNotEquals(left, different);
        assertNotEquals(left.hashCode(), different.hashCode());

        final Binary cleared = new Binary("id", Operator.IN, new SqlExpression("(SELECT 3)"));
        final Field propValue = Binary.class.getDeclaredField("propValue");
        propValue.setAccessible(true);
        propValue.set(cleared, null);

        assertFalse(left.equals(cleared));
        assertFalse(cleared.equals(left));
        // A null membership value contributes 0 to the collection hash, distinct from an expression hash.
        assertNotEquals(different.hashCode(), cleared.hashCode());
    }

    @Test
    void testBlankSqlFragmentsAreRejectedWithIndexedNames() {
        final IllegalArgumentException orderBy = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("account").orderBy("id", " "));
        assertEquals("propOrColumnNames[1] must not be null, empty, or blank", orderBy.getMessage());

        final IllegalArgumentException groupBy = assertThrows(IllegalArgumentException.class,
                () -> PSC.select("id").from("account").groupBy(List.of("id", "\t")));
        assertEquals("propOrColumnNames[1] must not be null, empty, or blank", groupBy.getMessage());

        final Map<String, Object> props = new LinkedHashMap<>();
        props.put(" ", 1);
        props.put("firstName", "Ada");
        final IllegalArgumentException insert = assertThrows(IllegalArgumentException.class, () -> PSC.insert(props));
        assertEquals("Key in props must not be null, empty, or blank", insert.getMessage());
    }

    @Test
    void testCastTrailingSpaceReadsTheEmptyFollowingToken() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer();
        final String expr = "id::text ";
        final StringBuilder rendered = new StringBuilder();
        final int verbatimFrom = QueryUtil.appendRawExpression(rendered, expr, tokenizer.tokenize(expr), tokenizer, false, word -> word, word -> word);

        assertEquals(-1, verbatimFrom);
        assertEquals("id::text ", rendered.toString());
    }

    @Test
    void testCompoundOrderByScansDoubledQuotesLoneSignsAndAnExtraParenthesis() {
        final String doubled = PSC.select("\"a\"\"b\" AS label")
                .from("account")
                .union(PSC.select("c AS label").from("archive"))
                .orderBy("label")
                .build()
                .query();
        assertTrue(doubled.contains("\"a\"\"b\""), doubled);
        assertTrue(doubled.contains("ORDER BY"), doubled);

        final String signed = PSC.select("-. AS label").from("account").union(PSC.select("c AS label").from("archive")).orderBy("label").build().query();
        assertTrue(signed.contains("-."), signed);

        final String extraParen = PSC.select("(a)) AS label").from("account").union(PSC.select("c AS label").from("archive")).orderBy("label").build().query();
        assertTrue(extraParen.contains("(a))"), extraParen);
    }

    @Test
    void testCriteriaClauseOrderAndLimitAfterOffset() {
        final var ordered = PSC.select("id").from("account").orderBy("id");
        final IllegalStateException whereAfterOrder = assertThrows(IllegalStateException.class,
                () -> ordered.append(Criteria.builder().where(Filters.eq("status", 1)).build()));
        assertEquals("'WHERE' must be added before 'ORDER BY'", whereAfterOrder.getMessage());

        final var offset = PSC.select("id").from("account").offset(2);
        final IllegalStateException criteriaLimit = assertThrows(IllegalStateException.class,
                () -> offset.append(Criteria.builder().limit("9999999999999999999").build()));
        assertTrue(criteriaLimit.getMessage().contains("'" + "LIMIT" + "' must be added before '" + "OFFSET" + "'"), criteriaLimit.getMessage());
    }

    @Test
    void testCustomNamedHandlerSeedsChildParameterNames() {
        final Dsl custom = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sql, name) -> sql.append(':').append(name)).build());
        final String sql = custom.select("id")
                .from("account")
                .where(Filters.and(Filters.eq("id", 1), Filters.exists(new SubQuery("account", List.of("id"), Filters.eq("id", 2)))))
                .build()
                .query();

        assertTrue(sql.contains("EXISTS"), sql);
        assertTrue(sql.contains(":id") && sql.contains(":id_2"), sql);
    }

    @Test
    void testDistinctOnQuantifierIsNotAnInsertColumn() {
        final String plain = PSC.select("DISTINCT ON id").into("backup").from("source").build().query();
        assertTrue(plain.startsWith("INSERT INTO backup"), plain);
        assertTrue(plain.contains("DISTINCT ON id"), plain);

        final String unclosed = PSC.select("DISTINCT ON (a id").into("backup").from("source").build().query();
        assertTrue(unclosed.contains("DISTINCT ON (a id"), unclosed);

        final Dsl postgres = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(ProductInfo.of("PostgreSQL")).build());
        final String dollar = postgres.select("DISTINCT ON ($tag$no-close id").into("backup").from("source").build().query();
        assertTrue(dollar.contains("$tag$"), dollar);
    }

    @Test
    void testDottedInsertSelectItemThatIsNotASubEntityIsKept() {
        final SqlBuilder builder = PSC.select("notAChild.id");
        builder.setEntityClass(Account.class);
        final String sql = builder.into("backup").from("account").build().query();
        assertTrue(sql.startsWith("INSERT INTO backup"), sql);
        assertTrue(sql.contains("notAChild.id") || sql.contains("not_a_child.id"), sql);
    }

    @Test
    void testDynamicQueryRejectsBlankSelectFragments() {
        final IllegalArgumentException element = assertThrows(IllegalArgumentException.class, () -> DynamicQuery.builder().select().append(List.of("id", " ")));
        assertEquals("Element in columns must not be null, empty, or blank", element.getMessage());

        final Map<String, String> blankKey = new LinkedHashMap<>();
        blankKey.put(" ", "alias");
        final IllegalArgumentException key = assertThrows(IllegalArgumentException.class, () -> DynamicQuery.builder().select().append(blankKey));
        assertEquals("Key in columnAliases must not be null, empty, or blank", key.getMessage());

        final Map<String, String> blankValue = new LinkedHashMap<>();
        blankValue.put("id", " ");
        final IllegalArgumentException value = assertThrows(IllegalArgumentException.class, () -> DynamicQuery.builder().select().append(blankValue));
        assertEquals("Value in columnAliases must not be null, empty, or blank", value.getMessage());
    }

    @Test
    void testFlatBeanContributesNoSubEntityTable() {
        assertTrue(AbstractQueryBuilder.buildFromTableRefs(FlatRow.class, null, null, NamingPolicy.SNAKE_CASE).isEmpty());
    }

    @Test
    void testFromRejectsControlOnlyTableName() {
        // U+0001 is not blank (Character.isWhitespace is false) but String.trim() drops it, so the
        // FROM list is empty after normalization and must be rejected.
        final IllegalArgumentException single = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("\u0001"));
        assertEquals("'expr' cannot be null or empty", single.getMessage());

        final IllegalArgumentException indexed = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from(List.of("account", "\u0001")));
        assertTrue(indexed.getMessage().contains("tableNames[1]"), indexed.getMessage());
    }

    @Test
    void testGluedSubscriptWithABareBracketStaysVerbatim() {
        final String sql = PSC.select("myTags[[#]").from("account").build().query();
        assertTrue(sql.contains("my_tags[[#]") || sql.contains("myTags[[#]"), sql);
    }

    @Test
    void testH2DistinctOnKeepsAHashPairAndAnUnterminatedDollarQuote() {
        final Dsl h2 = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(ProductInfo.of("H2")).build());
        final String hashPair = h2.select("id").from("account").append(Criteria.builder().distinctOn("a ## b // c").build()).build().query();
        assertTrue(hashPair.contains("DISTINCT"), hashPair);
        assertTrue(hashPair.contains("##"), hashPair);

        final String dollar = h2.select("id").from("account").append(Criteria.builder().distinctOn("$$no-close // x").build()).build().query();
        assertTrue(dollar.contains("$$no-close"), dollar);
    }

    @Test
    void testH2OrderByRejectsALineCommentAfterAHashPair() {
        final Dsl h2 = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(ProductInfo.of("H2")).build());
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("account").orderBy("a ## b // c"));
        assertTrue(failure.getMessage().contains("comment"), failure.getMessage());
    }

    @Test
    void testInsertBeanOmitsNullProperties() {
        final Account account = new Account();
        account.setFirstName("Ada");

        final String sql = PSC.insert(account).into("account").build().query();

        assertTrue(sql.contains("first_name"), sql);
        assertFalse(sql.contains("last_name"), sql);
        assertFalse(sql.contains("email"), sql);
    }

    @Test
    void testInsertOrReplaceSkipsARedundantLexicalMode() {
        assertTrue(SqlParser.isInsertOrReplaceQuery("INSERT OR REPLACE INTO t VALUES ('a\\\\b', `c`)"));
    }

    @Test
    void testIntoNullEntityClassRendersExplicitTable() {
        final String sql = PSC.insert("firstName").into("account_archive", (Class<?>) null).build().query();
        assertEquals("INSERT INTO account_archive (first_name) VALUES (?)", sql);
    }

    @Test
    void testJournalGrowsWhenNestedCheckpointsExceedTheInitialCapacity() throws Exception {
        final SqlBuilder builder = PSC.select("id").from("account").where(Filters.eq("id", 1));
        nestLibraryCheckpoints(builder, 5);

        final Field journalField = AbstractQueryBuilder.class.getDeclaredField("_mutationJournal");
        journalField.setAccessible(true);
        final Object journal = journalField.get(builder);
        final Field offsets = journal.getClass().getDeclaredField("offsets");
        offsets.setAccessible(true);
        assertTrue(((int[]) offsets.get(journal)).length >= 8, "the fifth nested checkpoint grows the journal arrays");

        assertEquals("SELECT id FROM account WHERE id = ?", builder.build().query());
    }

    @Test
    void testLimitExpressionAfterOffsetRequiresLimitFirst() {
        final var builder = PSC.select("id").from("account").offset(3);
        final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> builder.append(new Limit("9999999999999999999")));
        assertTrue(failure.getMessage().contains("LIMIT") && failure.getMessage().contains("OFFSET"), failure.getMessage());
    }

    @Test
    void testNamedRewriteSkipsAColonGluedToAnIdentifier() {
        final String sql = NSC.select("id")
                .from("account")
                .where(Filters.eq("id", 1))
                .union(NSC.select("foo:bar").from("archive").where(Filters.eq("id", 2)))
                .build()
                .query();
        assertTrue(sql.contains("foo:bar"), sql);
        assertTrue(sql.contains(":id") && sql.contains(":id_2"), sql);
    }

    @Test
    void testNamedUnionLeavesCastColonsAndRenamesTheCollidingParameter() {
        final SqlBuilder left = NSC.select("id::text").from("account").where(Filters.eq("id", 1));
        final SqlBuilder right = NSC.select("id::text").from("archive").where(Filters.eq("id", 2));
        final String sql = left.union(right).build().query();

        assertTrue(sql.contains("::text"), sql);
        assertTrue(sql.contains(":id") && sql.contains(":id_2"), sql);
        assertTrue(sql.contains("UNION"), sql);
    }

    @Test
    void testParenthesizedCteListTreatsASemicolonAsListPunctuation() {
        // A semicolon inside a parenthesized WITH list is list punctuation, not a new statement, so the query stays a read.
        final String sql = "SELECT * FROM (WITH a AS (SELECT 1); SELECT 2) s";
        assertTrue(SqlParser.isSyntacticallyReadQuery(sql));
    }

    @Test
    void testParsedSqlKeepsBindingsBesideOperatorsAndPostfixClauses() {
        final ParsedSql joined = ParsedSql.parse("SELECT ? , #{ } FROM t WHERE b = ?");
        assertEquals(2, joined.parameterCount());
        assertEquals("SELECT ? , #{ } FROM t WHERE b = ?", joined.parameterizedSql());

        // A trailing comma leaves the fast path, and the question mark is still a binding.
        final ParsedSql subscript = ParsedSql.parse("SELECT ARRAY[?, ] FROM t");
        assertEquals(1, subscript.parameterCount());

        // "doc ? 'key'" is a JSON operator, not a positional binding.
        final ParsedSql json = ParsedSql.parse("SELECT doc ? 'key' FROM t");
        assertEquals(0, json.parameterCount());
        assertTrue(json.parameterizedSql().contains("doc ?"), json.parameterizedSql());

        final ParsedSql collate = ParsedSql.parse("SELECT * FROM t WHERE name ILIKE ? COLLATE \"C\"");
        assertEquals(1, collate.parameterCount());

        final ParsedSql frame = ParsedSql.parse("SELECT sum(x) OVER (ORDER BY d RANGE BETWEEN ? PRECEDING AND ? FOLLOWING) FROM t");
        assertEquals(2, frame.parameterCount());

        final ParsedSql geometric = ParsedSql.parse("SELECT ?::line FROM t");
        assertEquals(1, geometric.parameterCount());

        final ParsedSql blankMarker = ParsedSql.parse("SELECT #{} FROM dual");
        assertEquals(0, blankMarker.parameterCount());
        assertTrue(blankMarker.parameterizedSql().contains("#{}"), blankMarker.parameterizedSql());

        // The question mark is not a frame bound: the closing parenthesis ends the scan first.
        final ParsedSql closedFrame = ParsedSql.parse("SELECT sum(x) OVER (ORDER BY d RANGE ?) FROM t");
        assertEquals(1, closedFrame.parameterCount());

        // An incomplete comma-separated subscript falls through to the general classifier.
        final ParsedSql openSubscript = ParsedSql.parse("SELECT ARRAY[?, FROM t");
        assertTrue(openSubscript.parameterizedSql().contains("ARRAY[?,"), openSubscript.parameterizedSql());
    }

    @Test
    void testRepeatedNonKeywordBracketWordsAreRemembered() {
        final String sql = "SELECT * FROM [ /* [ */ alpha ] JOIN [ /* [ */ beta ]";
        assertTrue(SqlParser.isSyntacticallyReadQuery(sql), sql);
    }

    @Test
    void testReservedAliasAfterExplicitAsIsDiscarded() {
        final String sql = PSC.select("a").from("t AS FOR").build().query();
        assertEquals("SELECT a FROM t AS FOR", sql);
    }

    @Test
    void testSelectionExcludedNamesTreatsNullAsIdentity() {
        final Selection bare = Selection.builder(Account.class).build();
        final Selection alsoBare = Selection.builder(Account.class).build();
        final Selection excluded = Selection.builder(Account.class).excludedPropNames(Set.of("gui")).build();

        assertEquals(bare, alsoBare);
        assertEquals(bare.hashCode(), alsoBare.hashCode());
        assertNotEquals(bare, excluded);
        assertNotEquals(excluded, bare);
    }

    @Test
    void testSqlParserKeepsHashIdentifiersAndSkipsAmbiguousLexicalModes() {
        final List<String> tokens = SqlParser.tokenize("MERGE INTO t USING #s AS s ON 1=1 WHEN MATCHED THEN UPDATE SET a = 1");
        assertTrue(tokens.stream().anyMatch(token -> token.contains("#s")), tokens.toString());

        assertTrue(SqlParser.isInsertOrReplaceQuery("WITH a AS (SELECT '\\'), b AS (SELECT 1) INSERT OR REPLACE INTO t VALUES (1)"));
        assertFalse(SqlParser.isReadOrInsertQuery("INSERT INTO t EXEC .@mutate"));
        assertFalse(SqlParser.isSyntacticallyReadQuery("WITH a AS (SELECT 1), b AS (SELECT 2); DELETE FROM t"));
        assertFalse(SqlParser.isSyntacticallyReadQuery("-- [\n".repeat(8) + "SELECT * FROM [DELETE FROM t RETURNING *]"));
    }

    @Test
    void testSubQueryInequalityWhenRawParameterCountsDiffer() throws Exception {
        final SubQuery one = new SubQuery("SELECT id FROM account WHERE id = ?", List.of(1));
        final SubQuery same = new SubQuery("SELECT id FROM account WHERE id = ?", List.of(1));
        final SubQuery longer = new SubQuery("SELECT id FROM account WHERE id = ?", List.of(1));
        final Field rawParameters = SubQuery.class.getDeclaredField("rawParameters");
        rawParameters.setAccessible(true);
        rawParameters.set(longer, com.landawn.abacus.util.ImmutableList.of(1, 2));

        assertEquals(one, same);
        assertNotEquals(one, longer);
        assertNotEquals(longer, one);
    }

    @Test
    void testSubscriptInteriorStartingWithABracketStaysVerbatim() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer();
        // Two closing brackets so the subscript is terminated; the interior starts with '['.
        // One token, so the leading name is converted and the bracket interior is scanned. The '[' that is not
        // glued to a name (a space precedes it) is what keeps the '#' from being re-tokenized as a comment.
        final String expr = "myTags[a [#]]";
        final StringBuilder rendered = new StringBuilder();
        QueryUtil.appendRawExpression(rendered, expr, List.of(expr), tokenizer, false, String::toUpperCase, String::toUpperCase);
        assertEquals("MYTAGS[a [#]]", rendered.toString());
    }

    @Test
    void testTransientOnlyBeanWithAWideExclusionSelectsNothing() {
        final Set<String> excluded = Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        assertTrue(QueryUtil.selectPropNames(TransientOnly.class, true, excluded).isEmpty());
    }

    @Test
    void testUnclosedGluedMyBatisMarkerIsCopiedAsWritten() {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer();
        final String expr = "col#{unclosed";
        final StringBuilder rendered = new StringBuilder();
        QueryUtil.appendRawExpression(rendered, expr, tokenizer.tokenize(expr), tokenizer, false, String::toUpperCase, String::toUpperCase);
        assertEquals("COL#{unclosed", rendered.toString());
    }

    @Test
    void testUnicodeSpaceBeforeAnAliasIsNotPartOfTheName() {
        final String sql = PSC.select("a").from("account \u3000 alias").build().query();
        assertTrue(sql.contains("account") && sql.contains("alias"), sql);
        assertFalse(sql.contains("\u3000alias"), sql);
    }

    @Test
    void testUninitializedInDiffersWhenMembershipIsNull() throws Exception {
        final In valued = new In("status", List.of("open"));
        final In cleared = new In("status", List.of("open"));
        final Field values = com.landawn.abacus.query.condition.AbstractIn.class.getDeclaredField("values");
        values.setAccessible(true);
        values.set(cleared, null);

        assertFalse(valued.equals(cleared));
        assertFalse(cleared.equals(valued));
    }

    @Test
    void testUnterminatedQuotesInsideBlockCommentScan() {
        assertFalse(QueryUtil.hasUnterminatedBlockComment("\"a\\\\b\" /* closed */"));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("\"a\\\\b /*"));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("`open /*"));
    }

    private static void nestLibraryCheckpoints(final SqlBuilder builder, final int remaining) {
        if (remaining == 0) {
            return;
        }

        try {
            final Method mutateAtomically = AbstractQueryBuilder.class.getDeclaredMethod("mutateAtomically", boolean.class, Runnable.class);
            mutateAtomically.setAccessible(true);
            mutateAtomically.invoke(builder, false, (Runnable) () -> nestLibraryCheckpoints(builder, remaining - 1));
        } catch (final InvocationTargetException e) {
            if (e.getCause() instanceof final RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AssertionError(e.getCause());
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @lombok.Data
    static class FlatRow {
        private long id;
        private String name;
    }

    @lombok.Data
    static class TransientOnly {
        @Transient
        private String note;
    }
}
