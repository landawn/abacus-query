package com.landawn.abacus.query;

import static com.landawn.abacus.query.Dsl.PSC;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.NonColumn;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.NamingPolicy;

public class QueryUtilTest extends TestBase {

    @Nested
    class ColumnMapping {
        @Test
        void testColumnAnnotatedBeanPropertyIsAPlainColumn() {
            // A bean-typed property marked @Column (e.g. a JSON column) is not a sub-entity: it maps to itself and is
            // qualified like any other column instead of being expanded into bogus "geo.lat" paths.
            final ImmutableMap<String, String> map = QueryUtil.propToColumnNameMap(ColumnBeanAddress.class, NamingPolicy.SNAKE_CASE);
            assertEquals("geo", map.get("geo"));
            assertFalse(map.containsKey("geo.lat"));
            assertEquals("SELECT ad.id AS \"id\", ad.street AS \"street\", ad.geo AS \"geo\" FROM address ad",
                    PSC.selectFrom(ColumnBeanAddress.class).build().query());
            assertEquals("SELECT ad.geo AS \"geo\" FROM address ad WHERE ad.geo = ?",
                    PSC.select("geo").from(ColumnBeanAddress.class).where(Filters.eq("geo", 1)).build().query());

            // Nested under a real sub-entity, the column is qualified by the sub-entity's table alias.
            final ImmutableMap<String, String> personMap = QueryUtil.propToColumnNameMap(ColumnBeanPerson.class, NamingPolicy.SNAKE_CASE);
            assertEquals("ad.geo", personMap.get("addr.geo"));
            assertFalse(personMap.containsKey("addr.geo.lat"));
            assertTrue(PSC.selectFrom(ColumnBeanPerson.class, true).build().query().contains("ad.geo AS \"addr.geo\""));
        }

        @Test
        void testColumnInfoIsUnqualifiedIsAHardRename() throws NoSuchMethodException {
            assertEquals(boolean.class, QueryUtil.ColumnInfo.class.getMethod("isUnqualified").getReturnType());
            assertThrows(NoSuchMethodException.class, () -> QueryUtil.ColumnInfo.class.getMethod("hasNoDot"));
        }

        @Test
        void testColumnToPropertyMapExactNamesWinOverCaseFoldedAliases() {
            ImmutableMap<String, String> map = QueryUtil.columnToPropNameMap(CaseDistinctColumns.class);

            assertEquals("lowerCaseColumn", map.get("code"));
            assertEquals("upperCaseColumn", map.get("CODE"));
        }

        @Test
        void testColumnToPropertyMapFiltersExplicitColumnsExcludedByMetadata() {
            final ImmutableMap<String, String> map = QueryUtil.columnToPropNameMap(ReverseLookupFilteredEntity.class);

            assertEquals("visible", map.get("visible_column"));
            assertEquals("visible", map.get("VISIBLE_COLUMN"));
            assertFalse(map.containsKey("hidden_annotation_column"), map.toString());
            assertFalse(map.containsKey("hidden_table_column"), map.toString());
            assertFalse(map.containsValue("hiddenByAnnotation"), map.toString());
            assertFalse(map.containsValue("hiddenByTable"), map.toString());
        }

        @Test
        void testColumnToPropertyMapRespectsTableColumnExclusions() {
            ImmutableMap<String, String> map = QueryUtil.columnToPropNameMap(TestUser.class);

            assertEquals("name", map.get("user_name"));
            assertFalse(map.containsKey("tempData"), map.toString());
            assertFalse(map.containsValue("tempData"), map.toString());
        }

        @Test
        void testConvertIdentifier_qualifiedPascalCaseSegment_noSpuriousUnderscore() {
            // Each dot-separated segment is converted on its own: the qualifier must not act as a word
            // boundary of the next segment (converting the whole name turned "acc.Id" into "acc._id").
            assertEquals("acc.id", QueryUtil.convertIdentifier("acc.Id", NamingPolicy.SNAKE_CASE));
            assertEquals("o.account_id", QueryUtil.convertIdentifier("o.AccountId", NamingPolicy.SNAKE_CASE));
            assertEquals("T.FIRST_NAME", QueryUtil.convertIdentifier("T.FirstName", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("acc.firstName", QueryUtil.convertIdentifier("acc.first_name", NamingPolicy.CAMEL_CASE));

            // per-segment underscore-run preservation and empty segments are unchanged
            assertEquals("t.__v", QueryUtil.convertIdentifier("t.__v", NamingPolicy.SNAKE_CASE));
            assertEquals("a..b", QueryUtil.convertIdentifier("a..b", NamingPolicy.SNAKE_CASE));

            // the condition rendering path agrees with the builder, which strips its alias before converting
            assertEquals("acc.id = 1", Filters.eq("acc.Id", 1).toSql(NamingPolicy.SNAKE_CASE));
            assertTrue(PSC.select("*").from("account acc").where(Filters.eq("acc.Id", 1)).build().query().endsWith("acc.id = ?"));
        }

        @Test
        void testConvertIdentifierPreservesLeadingAndTrailingUnderscoreRuns() {
            // NamingPolicy.convert alone drops leading/trailing '_' runs; convertIdentifier must keep them
            // and convert only the part in between (shared by SqlExpression.toSql and the builders).
            assertEquals("first_name", QueryUtil.convertIdentifier("firstName", NamingPolicy.SNAKE_CASE));
            assertEquals("_first_name", QueryUtil.convertIdentifier("_firstName", NamingPolicy.SNAKE_CASE));
            assertEquals("first_name_", QueryUtil.convertIdentifier("firstName_", NamingPolicy.SNAKE_CASE));
            assertEquals("__first_name__", QueryUtil.convertIdentifier("__firstName__", NamingPolicy.SNAKE_CASE));
            assertEquals("_FIRST_NAME", QueryUtil.convertIdentifier("_firstName", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("_firstName", QueryUtil.convertIdentifier("_first_name", NamingPolicy.CAMEL_CASE));
            assertEquals("__x", QueryUtil.convertIdentifier("__x", NamingPolicy.SNAKE_CASE));
            assertEquals("__X", QueryUtil.convertIdentifier("__x", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("_1", QueryUtil.convertIdentifier("_1", NamingPolicy.SNAKE_CASE));
            assertEquals("_1", QueryUtil.convertIdentifier("_1", NamingPolicy.SCREAMING_SNAKE_CASE));

            // all-underscore identifiers are returned as-is
            assertEquals("_", QueryUtil.convertIdentifier("_", NamingPolicy.SNAKE_CASE));
            assertEquals("___", QueryUtil.convertIdentifier("___", NamingPolicy.SCREAMING_SNAKE_CASE));

            // NO_CHANGE / null policy, null / empty identifier: returned as-is
            assertEquals("_firstName_", QueryUtil.convertIdentifier("_firstName_", NamingPolicy.NO_CHANGE));
            assertEquals("_firstName_", QueryUtil.convertIdentifier("_firstName_", null));
            assertEquals("", QueryUtil.convertIdentifier("", NamingPolicy.SNAKE_CASE));
            assertEquals(null, QueryUtil.convertIdentifier(null, NamingPolicy.SNAKE_CASE));

            // the condition rendering path goes through the same helper (builder-path parity is pinned in AbstractQueryBuilderTest)
            assertEquals("_first_name = other_value", Filters.expr("_firstName = otherValue").toSql(NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testConvertIdentifierPreservesQuotedSegments() {
            assertEquals("t.\"First.Name\"", QueryUtil.convertIdentifier("t.\"First.Name\"", NamingPolicy.SNAKE_CASE));
            assertEquals("\"firstName\"", QueryUtil.convertIdentifier("\"firstName\"", NamingPolicy.SNAKE_CASE));
            assertEquals("[firstName]", QueryUtil.convertIdentifier("[firstName]", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("my_table.`firstName`", QueryUtil.convertIdentifier("myTable.`firstName`", NamingPolicy.SNAKE_CASE));
            assertEquals("\"My.Schema\".first_name", QueryUtil.convertIdentifier("\"My.Schema\".firstName", NamingPolicy.SNAKE_CASE));
            assertEquals("U&\"aB\".first_name", QueryUtil.convertIdentifier("U&\"aB\".firstName", NamingPolicy.SNAKE_CASE));
            // unquoted behavior unchanged
            assertEquals("acc.id", QueryUtil.convertIdentifier("acc.Id", NamingPolicy.SNAKE_CASE));
            assertEquals("t.__v", QueryUtil.convertIdentifier("t.__v", NamingPolicy.SNAKE_CASE));
            assertEquals("user_ids[1]", QueryUtil.convertIdentifier("userIds[1]", NamingPolicy.SNAKE_CASE));
            // condition and builder paths now agree
            assertEquals("t.\"First.Name\" = 1", Filters.eq("t.\"First.Name\"", 1).toSql(NamingPolicy.SNAKE_CASE));
            assertEquals("\"firstName\" = 1", Filters.eq("\"firstName\"", 1).toSql(NamingPolicy.SNAKE_CASE));
            assertEquals("SELECT a FROM t WHERE t.\"First.Name\" = ?", PSC.select("a").from("t").where(Filters.eq("t.\"First.Name\"", 1)).build().query());
        }

        @Test
        void testConvertIdentifierPreservesUnderscoreRunsPerQualifiedSegment() {
            // leading/trailing '_' runs are preserved for EVERY dot-separated segment, not just the whole string
            assertEquals("t.__v", QueryUtil.convertIdentifier("t.__v", NamingPolicy.SNAKE_CASE));
            assertEquals("T.__V", QueryUtil.convertIdentifier("t.__v", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("t.__v", QueryUtil.convertIdentifier("t.__v", NamingPolicy.CAMEL_CASE));
            assertEquals("t.__v", QueryUtil.convertIdentifier("t.__v", NamingPolicy.KEBAB_CASE));
            assertEquals("acc._id", QueryUtil.convertIdentifier("acc._id", NamingPolicy.SNAKE_CASE));
            assertEquals("ACC._ID", QueryUtil.convertIdentifier("acc._id", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("a._b_.c_", QueryUtil.convertIdentifier("a._b_.c_", NamingPolicy.SNAKE_CASE));
            assertEquals("A._B_.C_", QueryUtil.convertIdentifier("a._b_.c_", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("_t._id_", QueryUtil.convertIdentifier("_t._id_", NamingPolicy.SNAKE_CASE));
            assertEquals("t.first_name_", QueryUtil.convertIdentifier("t.firstName_", NamingPolicy.SNAKE_CASE));
            assertEquals("acc._first_name", QueryUtil.convertIdentifier("acc._firstName", NamingPolicy.SNAKE_CASE));
            assertEquals("acc._firstName", QueryUtil.convertIdentifier("acc._first_name", NamingPolicy.CAMEL_CASE));

            // all-underscore segments and identifiers are kept as-is
            assertEquals("t.__", QueryUtil.convertIdentifier("t.__", NamingPolicy.SNAKE_CASE));
            assertEquals("_._", QueryUtil.convertIdentifier("_._", NamingPolicy.SCREAMING_SNAKE_CASE));

            // segments WITHOUT edge runs are converted one segment at a time: the qualifier is not part of the
            // column's word structure (converting "t.firstName" as one name gave "T.firstName" under UPPER_CAMEL_CASE)
            assertEquals("t.first_name", QueryUtil.convertIdentifier("t.firstName", NamingPolicy.SNAKE_CASE));
            assertEquals("T.FirstName", QueryUtil.convertIdentifier("t.firstName", NamingPolicy.UPPER_CAMEL_CASE));

            // internal runs are NOT preserved: they follow the naming policy (8.0.0 collapses a__b -> a_b)
            assertEquals(NamingPolicy.SNAKE_CASE.convert("a__b"), QueryUtil.convertIdentifier("a__b", NamingPolicy.SNAKE_CASE));
            assertEquals(NamingPolicy.CAMEL_CASE.convert("a__b"), QueryUtil.convertIdentifier("a__b", NamingPolicy.CAMEL_CASE));

            // the SqlExpression path renders qualified names through the same helper
            assertEquals("t.__v = acc._id", Filters.expr("t.__v = acc._id").toSql(NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testConvertIdentifierQualifiedIdentityAndConversionBoundaries() {
            // Qualified ASCII names need no temporary segments when snake-case conversion is an identity.
            for (final String name : new String[] { "u.id", "schema.table.name", "a..b", ".id.", "." }) {
                assertSame(name, QueryUtil.convertIdentifier(name, NamingPolicy.SNAKE_CASE));
            }
            // The identity path must not bypass per-segment conversion or edge-underscore preservation.
            assertEquals("acc.id", QueryUtil.convertIdentifier("acc.Id", NamingPolicy.SNAKE_CASE));
            assertEquals("u.first_name", QueryUtil.convertIdentifier("u.firstName", NamingPolicy.SNAKE_CASE));
            assertEquals("u._first_name_", QueryUtil.convertIdentifier("u._firstName_", NamingPolicy.SNAKE_CASE));
            assertEquals("u.a_b", QueryUtil.convertIdentifier("u.a__b", NamingPolicy.SNAKE_CASE));
            assertEquals("u.名称", QueryUtil.convertIdentifier("u.名称", NamingPolicy.SNAKE_CASE));
            assertEquals("U.ID", QueryUtil.convertIdentifier("u.id", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("u.firstName", QueryUtil.convertIdentifier("u.first_name", NamingPolicy.CAMEL_CASE));
        }

        @Test
        void testExcludingSubEntityRootAlsoExcludesExpandedProperties() {
            Set<String> excluded = Collections.singleton("child");
            ImmutableList<String> names = QueryUtil.selectPropNames(ParentWithFilteredChild.class, true, excluded);

            assertEquals(Collections.singletonList("id"), names);

            String sql = PSC.selectFrom(ParentWithFilteredChild.class, true, excluded).build().query();
            assertEquals("SELECT id AS \"id\" FROM parent_with_filtered_child", sql);
        }

        @Test
        void testGetColumn2PropNameMap() {
            ImmutableMap<String, String> map = QueryUtil.columnToPropNameMap(Account.class);
            assertNotNull(map);
        }

        @Test
        void testGetColumn2PropNameMap_NullClass() {
            assertThrows(IllegalArgumentException.class, () -> {
                QueryUtil.columnToPropNameMap(null);
            });
        }

        @Test
        void testLongQuotedIdentifierChainsRetainEverySegment() {
            final String input = "\"part.with.dot\".firstName.".repeat(1_024) + "[last.part]";
            final String expected = "\"part.with.dot\".first_name.".repeat(1_024) + "[last.part]";

            assertEquals(expected, QueryUtil.convertIdentifier(input, NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testNestedQuotedColumnDotsRetainSubEntityQualifier() {
            final ImmutableMap<String, String> map = QueryUtil.propToColumnNameMap(NestedQuotedColumns.class, NamingPolicy.SNAKE_CASE);

            assertEquals("q.\"double.dot\"", map.get("child.doubleQuoted"));
            assertEquals("q.`backtick.dot`", map.get("child.backtickQuoted"));
            assertEquals("q.[bracket.dot]", map.get("child.bracketQuoted"));
            assertEquals("q.\"escaped\"\".dot\"", map.get("child.escapedQuote"));
            assertEquals("q.[escaped]].dot]", map.get("child.escapedBracket"));
            assertEquals("source.\"qualified.dot\"", map.get("child.qualified"));
        }

        @Test
        void testPatternForAlphanumericColumnName() {
            assertNotNull(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN);
            assertTrue(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user_name").matches());
            assertTrue(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user123").matches());
            assertTrue(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user-name").matches());
            assertFalse(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user.name").matches());
            assertFalse(QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("user name").matches());
        }

        @Test
        void testPropertyToColumnMap() {
            ImmutableMap<String, String> map = QueryUtil.propToColumnNameMap(Account.class, NamingPolicy.SNAKE_CASE);
            assertNotNull(map);
        }

        @Test
        void testPropertyToColumnMap_DifferentPolicies() {
            ImmutableMap<String, String> lower = QueryUtil.propToColumnNameMap(Account.class, NamingPolicy.SNAKE_CASE);
            ImmutableMap<String, String> upper = QueryUtil.propToColumnNameMap(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
            ImmutableMap<String, String> noChange = QueryUtil.propToColumnNameMap(Account.class, NamingPolicy.NO_CHANGE);

            assertNotNull(lower);
            assertNotNull(upper);
            assertNotNull(noChange);
        }

        @Test
        void testPropertyToColumnMap_MapEntityClassReturnsEmpty() {
            // Map-assignable classes get an empty map (documented behavior).
            ImmutableMap<String, String> result = QueryUtil.propToColumnNameMap(HashMap.class, NamingPolicy.SNAKE_CASE);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void testPropertyToColumnMap_NoChange() {
            ImmutableMap<String, String> map = QueryUtil.propToColumnNameMap(Account.class, NamingPolicy.NO_CHANGE);
            assertNotNull(map);
        }

        @Test
        void testPropertyToColumnMap_NullEntityClassAndNullPolicyReturnsEmpty() {
            // Both null: still returns empty map.
            ImmutableMap<String, String> result = QueryUtil.propToColumnNameMap(null, null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void testPropertyToColumnMap_NullEntityClassReturnsEmpty() {
            // Documented contract: null entityClass returns an empty map (does not throw).
            ImmutableMap<String, String> result = QueryUtil.propToColumnNameMap(null, NamingPolicy.SNAKE_CASE);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void testPropertyToColumnMap_NullNamingPolicyUsesSnakeCase() {
            ImmutableMap<String, String> result = QueryUtil.propToColumnNameMap(QueryUtilTest.AliaslessEntity.class, null);

            assertEquals("simple_value", result.get("simpleValue"));
        }

        @Test
        void testPropertyToColumnMap_UpperCase() {
            ImmutableMap<String, String> map = QueryUtil.propToColumnNameMap(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
            assertNotNull(map);
        }

        @Test
        void testPropertyToColumnMap_WithSelfReferentialEntityDoesNotThrow() {
            ImmutableMap<String, String> result = QueryUtil.propToColumnNameMap(SelfReferentialEntity.class, NamingPolicy.SNAKE_CASE);

            assertEquals("id", result.get("id"));
            assertFalse(result.containsKey("parent.parent"));
        }

        @Test
        void testPropertyToColumnMapCapsDeepNestedExpansion() {
            final ImmutableMap<String, String> result = QueryUtil.propToColumnNameMap(DeepNestedRoot.class, NamingPolicy.SNAKE_CASE);

            assertFalse(result.containsKey("middle.leaf.tail.value"));
            assertFalse(result.containsKey("middle.leaf.tail"));
        }

        @Test
        void testPropertyToColumnMapWithRepeatedNestedType() {
            final ImmutableMap<String, String> result = QueryUtil.propToColumnNameMap(NestedRoot.class, NamingPolicy.SNAKE_CASE);

            assertEquals("nested_leaf.value", result.get("branch.firstLeaf.value"));
            assertEquals("nested_leaf.value", result.get("branch.secondLeaf.value"));
        }

        @Test
        void testPropToColumnInfoMap() {
            ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(Account.class, NamingPolicy.SNAKE_CASE);

            assertEquals(new QueryUtil.ColumnInfo("first_name", true), map.get("firstName"));
            assertEquals(new QueryUtil.ColumnInfo("first_name", true), map.get("first_name"));
            assertEquals("first_name", map.get("firstName").columnName());
            assertTrue(map.get("firstName").isUnqualified());

            ImmutableMap<String, QueryUtil.ColumnInfo> nestedMap = QueryUtil.propToColumnInfoMap(NestedRoot.class, NamingPolicy.SNAKE_CASE);
            QueryUtil.ColumnInfo nested = nestedMap.get("branch.firstLeaf.value");
            assertEquals(QueryUtil.propToColumnNameMap(NestedRoot.class, NamingPolicy.SNAKE_CASE).get("branch.firstLeaf.value"), nested.columnName());
            assertFalse(nested.isUnqualified());
        }

        @Test
        void testPropToColumnInfoMap_NoChange() {
            ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(Account.class, NamingPolicy.NO_CHANGE);

            assertEquals(new QueryUtil.ColumnInfo("firstName", true), map.get("firstName"));
        }

        @Test
        void testPropToColumnInfoMap_NullClass() {
            assertThrows(IllegalArgumentException.class, () -> {
                QueryUtil.propToColumnInfoMap(null, NamingPolicy.SNAKE_CASE);
            });
        }

        @Test
        void testPropToColumnInfoMap_NullNamingPolicyUsesSnakeCase() {
            ImmutableMap<String, QueryUtil.ColumnInfo> result = QueryUtil.propToColumnInfoMap(QueryUtilTest.AliaslessEntity.class, null);

            assertEquals("simple_value", result.get("simpleValue").columnName());
            assertTrue(result.get("simpleValue").isUnqualified());
        }

        @Test
        void testPropToColumnInfoMap_QualifiedMappedColumnIsNotPrefixedAgain() {
            ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(QualifiedColumnEntity.class, NamingPolicy.SNAKE_CASE);

            assertEquals(new QueryUtil.ColumnInfo("source.value", false), map.get("value"));
            assertEquals(new QueryUtil.ColumnInfo("source.value", false), map.get("source.value"));

            String sql = PSC.selectFrom(QualifiedColumnEntity.class).build().query();
            assertEquals("SELECT source.value AS \"value\" FROM qualified_column_entity q", sql);
            assertFalse(sql.contains("q.source.value"));
        }

        @Test
        void testPropToColumnInfoMap_UpperCase() {
            ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);

            assertEquals(new QueryUtil.ColumnInfo("FIRST_NAME", true), map.get("firstName"));
        }

        @Test
        void testQueryUtil_patternForAlphanumericColumnName() {
            boolean isValid = QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("column_name").matches();
            assertTrue(isValid);
            boolean isInvalid = QueryUtil.SIMPLE_COLUMN_NAME_PATTERN.matcher("column name").matches();
            assertFalse(isInvalid);
        }

        @Test
        void testQuotedColumnMappingsRetainAliasesInConditionsAndOrdering() {
            final ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(QuotedColumnNames.class, NamingPolicy.SNAKE_CASE);

            for (final String propName : List.of("doubleQuoted", "backtickQuoted", "bracketQuoted", "escapedQuote", "escapedBracket")) {
                final String columnName = map.get(propName).columnName();
                // WHERE resolves the joined table's property map; ORDER BY resolves the original table's map.
                final AbstractQueryBuilder.SP result = PSC.select(propName)
                        .from(QuotedColumnNames.class, "a")
                        .join(QuotedColumnNames.class, "b")
                        .on("a.id = b.id")
                        .where(Filters.eq("b." + propName, "match"))
                        .orderBy("a." + propName)
                        .build();

                assertEquals("SELECT a." + columnName + " AS \"" + propName + "\" FROM quoted_columns a JOIN quoted_columns b ON a.id = b.id WHERE b."
                        + columnName + " = ? ORDER BY a." + columnName, result.query(), propName);
                assertEquals(List.of("match"), result.parameters(), propName);
            }
        }

        @Test
        void testQuotedIdentifierChainsPreserveSegmentBoundariesForEveryNamingPolicy() {
            final String quoted = "U&\"part.with.dot\" UESCAPE '.'";
            final String identifier = "._first_name_." + quoted + "..__last_name__.";
            final Map<NamingPolicy, String> expected = Map.of(NamingPolicy.CAMEL_CASE, "._firstName_." + quoted + "..__lastName__.",
                    NamingPolicy.UPPER_CAMEL_CASE, "._FirstName_." + quoted + "..__LastName__.", NamingPolicy.SNAKE_CASE, identifier,
                    NamingPolicy.SCREAMING_SNAKE_CASE, "._FIRST_NAME_." + quoted + "..__LAST_NAME__.", NamingPolicy.KEBAB_CASE,
                    "._first-name_." + quoted + "..__last-name__.", NamingPolicy.NO_CHANGE, identifier);

            for (final NamingPolicy policy : NamingPolicy.values()) {
                assertEquals(expected.get(policy), QueryUtil.convertIdentifier(identifier, policy), policy.toString());
            }

            assertSame(identifier, QueryUtil.convertIdentifier(identifier, null));
            assertSame(identifier, QueryUtil.convertIdentifier(identifier, NamingPolicy.NO_CHANGE));
        }

        @Test
        void testQuotedIdentifierConversionPreservesEscapedAndUnicodeSegments() {
            for (final String quoted : List.of("\"a.\"\"b\"", "`a.``b`", "[a.]]b]", "U&\"a.002e\" UESCAPE '.'", "u&\"a.002e\" UESCAPE E'.'",
                    "U&\"a[002e\" UESCAPE '['", "U&\"a.002e\" UESCAPE E'\\056'", "U&\"a.002e\" UESCAPE E'\\x2e'", "U&\"a.002e\" UESCAPE E'\\u002e'",
                    "U&\"a.002e\" UESCAPE E'\\U0000002e'")) {
                final String identifier = "firstName." + quoted + ".lastName";
                assertEquals("first_name." + quoted + ".last_name", QueryUtil.convertIdentifier(identifier, NamingPolicy.SNAKE_CASE));
                assertEquals("FIRST_NAME." + quoted + ".LAST_NAME", QueryUtil.convertIdentifier(identifier, NamingPolicy.SCREAMING_SNAKE_CASE));
            }

            assertEquals(".\"a.b\"..first_name.", QueryUtil.convertIdentifier(".\"a.b\"..firstName.", NamingPolicy.SNAKE_CASE));
            assertEquals("first_name.\"unterminated.lastName", QueryUtil.convertIdentifier("firstName.\"unterminated.lastName", NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testRegisterEntityPropColumnNameMap() {
            // Direct cycle detection should stop recursive expansion instead of failing the entire lookup.
            Set<Class<?>> registeringClasses = new HashSet<>();
            registeringClasses.add(TestUser.class);

            ImmutableMap<String, String> result = QueryUtil.registerEntityPropColumnNameMap(TestUser.class, NamingPolicy.SNAKE_CASE, registeringClasses);

            assertTrue(result.isEmpty());
        }

        @Test
        void testSqlBuilder_FromSelfReferentialEntityDoesNotFail() {
            String sql = PSC.select("id").from(SelfReferentialEntity.class).where(Filters.eq("id", 1L)).build().query();

            assertTrue(sql.contains("FROM self_referential_entity"));
            assertTrue(sql.contains("WHERE id = ?"));
        }

        @Test
        void testUnicodeEscapeClausesDoNotHideFollowingUsingColumns() {
            final ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(UnicodeColumnNames.class, NamingPolicy.NO_CHANGE);
            final SqlBuilder builder = PSC.select("*").from(UnicodeColumnNames.class, "a").join(UnicodeColumnNames.class, "b");

            // The '[' escape belongs to the first identifier, and must not hide a qualifier in the next one.
            assertThrows(IllegalArgumentException.class, () -> builder.using(List.of("bracketEscape", "qualifiedUnicode")));

            // The rejected list must also leave the join ready for a valid list containing punctuation escapes.
            assertEquals("SELECT * FROM unicode_columns a JOIN unicode_columns b USING (" + map.get("dotEscape").columnName() + ", "
                    + map.get("bracketEscape").columnName() + ")", builder.using(List.of("dotEscape", "bracketEscape")).build().query());
        }

        @Test
        void testUnicodeEscapeStringClausesDoNotHideOrInventQualifyingDots() {
            for (final String escape : List.of("E'.'", "e'['", "E'\\\\'", "E'\\056'", "E'\\x2e'", "E'\\u002e'", "E'\\U0000002e'")) {
                final String identifier = "U&\"name\" UESCAPE " + escape;
                assertEquals(-1, QueryUtil.indexOfQualifyingDot(identifier), escape);
                assertEquals(identifier.length(), QueryUtil.indexOfQualifyingDot(identifier + ".value"), escape);
                assertEquals(identifier + ".first_name", QueryUtil.convertIdentifier(identifier + ".firstName", NamingPolicy.SNAKE_CASE), escape);
            }
        }

        @Test
        void testUnicodeEscapeStringClausesRetainTableAndNestedQualifiers() {
            final ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(UnicodeColumnNames.class, NamingPolicy.NO_CHANGE);
            final ImmutableMap<String, String> nestedMap = QueryUtil.propToColumnNameMap(NestedUnicodeColumns.class, NamingPolicy.NO_CHANGE);

            for (final String propName : List.of("escapeString", "lowercaseEscapeString", "escapedBackslash", "octalEscapeString", "hexEscapeString",
                    "unicodeEscapeString", "literalXEscapeString")) {
                final QueryUtil.ColumnInfo column = map.get(propName);
                assertTrue(column.isUnqualified(), propName);
                assertEquals("SELECT a." + column.columnName() + " AS \"" + propName + "\" FROM unicode_columns a JOIN unicode_columns b ON a.id = b.id",
                        PSC.select(propName).from(UnicodeColumnNames.class, "a").join(UnicodeColumnNames.class, "b").on("a.id = b.id").build().query(),
                        propName);
                assertEquals("u." + column.columnName(), nestedMap.get("child." + propName), propName);
            }
        }

        @Test
        void testUnicodeEscapeStringsDoNotTreatInvalidClausesOrExpressionsAsIdentifiers() {
            final ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(UnicodeColumnNames.class, NamingPolicy.NO_CHANGE);

            for (final String propName : List.of("invalidWhitespaceEscape", "invalidPlusEscape", "invalidMultipleEscapes", "invalidEscapeKeyword",
                    "invalidEmptyEscape", "invalidShortUnicodeEscape", "invalidOutOfRangeUnicodeEscape", "invalidNullEscape", "invalidSurrogateEscape",
                    "invalidHexDigitEscape", "escapeStringExpression")) {
                final QueryUtil.ColumnInfo column = map.get(propName);
                assertFalse(column.isUnqualified(), propName);
                assertEquals("SELECT " + column.columnName() + " AS \"" + propName + "\" FROM unicode_columns a",
                        PSC.select(propName).from(UnicodeColumnNames.class, "a").build().query(), propName);
            }
        }

        @Test
        void testUnicodeIdentifierEscapeClausesDoNotHideOrInventUsingQualifiers() {
            final ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(UnicodeColumnNames.class, NamingPolicy.NO_CHANGE);

            for (final String propName : List.of("unicodeQuoted", "unicodeQuotedDot", "unicodeEscapedQuote", "customEscape", "dotEscape", "bracketEscape")) {
                final String columnName = map.get(propName).columnName();
                final String expected = "SELECT * FROM unicode_columns a JOIN unicode_columns b USING (" + columnName + ")";
                assertEquals(expected, PSC.select("*").from(UnicodeColumnNames.class, "a").join(UnicodeColumnNames.class, "b").using(propName).build().query());
                assertEquals(expected,
                        PSC.select("*").from(UnicodeColumnNames.class, "a").join(UnicodeColumnNames.class, "b").on(Filters.using(propName)).build().query());
            }

            for (final String propName : List.of("qualifiedUnicode", "unicodeQuotedQualifier", "escapedQualifier")) {
                final SqlBuilder builder = PSC.select("*").from(UnicodeColumnNames.class, "a").join(UnicodeColumnNames.class, "b");
                assertThrows(IllegalArgumentException.class, () -> builder.using(propName), propName);
                builder.on("a.id = b.id").build();
            }
        }

        @Test
        void testUnicodeIdentifiersRetainSelfJoinAndNestedQualifiers() {
            final ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(UnicodeColumnNames.class, NamingPolicy.NO_CHANGE);
            final ImmutableMap<String, String> nestedMap = QueryUtil.propToColumnNameMap(NestedUnicodeColumns.class, NamingPolicy.NO_CHANGE);

            for (final String propName : List.of("combiningMark", "spacingMark", "enclosingMark", "supplementaryLetter", "letterNumber", "connectorPunctuation",
                    "supplementarySymbol", "unicodeQuoted", "unicodeQuotedDot", "unicodeEscapedQuote", "customEscape", "dotEscape", "bracketEscape")) {
                final QueryUtil.ColumnInfo column = map.get(propName);
                assertTrue(column.isUnqualified(), propName);
                assertEquals(column, map.get(column.columnName()));

                // A self-join makes dropping this qualifier observable: both tables expose the same mapped name.
                assertEquals("SELECT a." + column.columnName() + " AS \"" + propName + "\" FROM unicode_columns a JOIN unicode_columns b ON a.id = b.id",
                        PSC.select(propName).from(UnicodeColumnNames.class, "a").join(UnicodeColumnNames.class, "b").on("a.id = b.id").build().query(),
                        propName);
                assertEquals("u." + column.columnName(), nestedMap.get("child." + propName), propName);
            }
        }

        @Test
        void testUnicodeQualifiedNamesAndExpressionsAreNotPrefixedAgain() {
            final ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(UnicodeColumnNames.class, NamingPolicy.NO_CHANGE);
            final ImmutableMap<String, String> nestedMap = QueryUtil.propToColumnNameMap(NestedUnicodeColumns.class, NamingPolicy.NO_CHANGE);

            for (final String propName : List.of("qualifiedUnicode", "unicodeQuotedQualifier", "escapedQualifier", "unicodeExpression",
                    "unicodeEscapeExpression")) {
                final QueryUtil.ColumnInfo column = map.get(propName);
                assertFalse(column.isUnqualified(), propName);
                assertEquals("SELECT " + column.columnName() + " AS \"" + propName + "\" FROM unicode_columns a",
                        PSC.select(propName).from(UnicodeColumnNames.class, "a").build().query(), propName);
                assertEquals(column.columnName(), nestedMap.get("child." + propName), propName);
            }
        }
    }

    @Nested
    class FragmentScanning {
        // R13-01: a "#{"/"${" token opens a binding only when some later token closes a brace; the unterminated ones are
        // converted like ordinary text. The closing index is memoized, so many unterminated markers render in linear time.
        @Test
        void testAppendRawExpressionKeepsUnterminatedMarkersOutOfBindingsAndRendersThemLinearly() {
            final int[] verbatimFrom = new int[1];

            assertEquals("${a ${b} c_d ${e", renderRaw("${a ${b} cD ${e", false, verbatimFrom));
            assertEquals("#{a ${b} c_d #{e f_g", renderRaw("#{a ${b} cD #{e fG", false, verbatimFrom));
            assertEquals("${a cD} e_f ${g hI #{j} k_l", renderRaw("${a cD} eF ${g hI #{j} kL", false, verbatimFrom));
            assertEquals("first_name = #{x", renderRaw("firstName = #{x", false, verbatimFrom));
            assertEquals("${a} ${b", renderRaw("${a} ${b", false, verbatimFrom));
            assertEquals("first_name = #{x} AND last_name = ${y", renderRaw("firstName = #{x} AND lastName = ${y", false, verbatimFrom));
            assertEquals(-1, verbatimFrom[0]);

            // 64,000 unterminated markers: the pre-fix per-marker rescan took ~10 s here, the memoized check under 100 ms.
            final String expr = "${a ".repeat(64_000);
            final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer();
            final List<String> tokens = tokenizer.tokenize(expr);
            final StringBuilder sb = new StringBuilder(expr.length());

            assertTimeoutPreemptively(Duration.ofSeconds(2),
                    () -> assertEquals(-1, QueryUtil.appendRawExpression(sb, expr, tokens, tokenizer, false, w -> w.toLowerCase(), w -> w)));
            assertEquals(expr, sb.toString());
        }

        // Regression: appendRawExpression had no direct tests; its argument checks used literal names instead of cs constants,
        // and a backslash before a quote that does not close its own region (\" inside '...') switched the rest to verbatim.
        @Test
        void testAppendRawExpressionReturnsTheVerbatimStartAndRejectsNullArguments() {
            final int[] verbatimFrom = new int[1];

            assertEquals("first_name = 'x'", renderRaw("firstName = 'x'", false, verbatimFrom));
            assertEquals(-1, verbatimFrom[0]);

            // Verbatim from the first token whose extent depends on the backslash reading, at its exact index in expr.
            assertEquals("a_b = 'C:\\' AND firstName = 1", renderRaw("aB = 'C:\\' AND firstName = 1", false, verbatimFrom));
            assertEquals(5, verbatimFrom[0]);

            // A backslash before a quote that does not close the region leaves both readings in agreement.
            assertEquals("payload = '{\"a\":\"b\\\"c\"}' AND created_at > 1",
                    renderRaw("payload = '{\"a\":\"b\\\"c\"}' AND createdAt > 1", false, verbatimFrom));
            assertEquals(-1, verbatimFrom[0]);

            // backslashEscapesOnly (MySQL) takes the tokenizer's reading as authoritative and never goes verbatim.
            assertEquals("a = 'It\\'s' AND first_name = 1", renderRaw("a = 'It\\'s' AND firstName = 1", true, verbatimFrom));
            assertEquals(-1, verbatimFrom[0]);

            // Appends to what the builder already holds, and keeps the line break between two adjacent string literals.
            final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer();
            final StringBuilder sb = new StringBuilder("WHERE ");
            final String lines = "msg = 'hello '\n'world'";
            assertEquals(-1, QueryUtil.appendRawExpression(sb, lines, tokenizer.tokenize(lines), tokenizer, false, w -> w, w -> w));
            assertEquals("WHERE msg = 'hello '\n'world'", sb.toString());

            // Tokens that cannot be located in expr: nothing is guessed, the whole of expr is appended as written.
            final StringBuilder fallback = new StringBuilder();
            assertEquals(0, QueryUtil.appendRawExpression(fallback, "zz = 'C:\\' AND firstName = 1", tokenizer.tokenize("aB = 'C:\\' AND firstName = 1"),
                    tokenizer, false, w -> w.toUpperCase(), w -> w.toUpperCase()));
            assertEquals("zz = 'C:\\' AND firstName = 1", fallback.toString());

            final List<String> tokens = tokenizer.tokenize("a");
            final java.util.function.UnaryOperator<String> same = w -> w;
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.appendRawExpression(null, "a", tokens, tokenizer, false, same, same));
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.appendRawExpression(new StringBuilder(), null, tokens, tokenizer, false, same, same));
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.appendRawExpression(new StringBuilder(), "a", null, tokenizer, false, same, same));
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.appendRawExpression(new StringBuilder(), "a", tokens, null, false, same, same));
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.appendRawExpression(new StringBuilder(), "a", tokens, tokenizer, false, null, same));
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> QueryUtil.appendRawExpression(new StringBuilder(), "a", tokens, tokenizer, false, same, null));
            assertTrue(e.getMessage().contains(cs.qualifiedPartConverter), e.getMessage());
        }

        @Test
        void testCommentAndEscapeConstructsDoNotHideColumnQualifiers() {
            final ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(LexicalColumnNames.class, NamingPolicy.NO_CHANGE);

            // Only identifier quoting hides a dot. A hash comment, a backslash escape, a string literal and a
            // dash comment are SQL-script constructs: reading them here would drop a real qualifier and let the
            // builder prepend a table alias to an already-qualified column.
            for (final String propName : List.of("hashQualified", "backslashQualified", "literalDotExpression", "dashQualified", "bracketedLiteral",
                    "unterminatedBracketLiteral", "dotlessExpression")) {
                assertFalse(map.get(propName).isUnqualified(), propName);

                final Selection selection = Selection.builder(LexicalColumnNames.class).tableAlias("l").includedPropNames(List.of(propName)).build();
                assertEquals("SELECT " + map.get(propName).columnName() + " AS \"" + propName + "\" FROM lexical_columns l",
                        PSC.selectFrom(selection).build().query());
            }

            final ImmutableMap<String, String> nestedMap = QueryUtil.propToColumnNameMap(NestedLexicalColumns.class, NamingPolicy.NO_CHANGE);
            assertEquals("#temp.column", nestedMap.get("child.hashQualified"));
            assertEquals("COALESCE(x, 'N.A')", nestedMap.get("child.literalDotExpression"));
            assertEquals("COALESCE(x, '[N.A]')", nestedMap.get("child.bracketedLiteral"));
            assertEquals("COALESCE(x, '[') || t.suffix", nestedMap.get("child.unterminatedBracketLiteral"));
            assertEquals("COALESCE(x, 1)", nestedMap.get("child.dotlessExpression"));

            // A single identifier, quoted or not, still takes the sub-entity qualifier.
            assertEquals("q.\"double.dot\"", QueryUtil.propToColumnNameMap(NestedQuotedColumns.class, NamingPolicy.NO_CHANGE).get("child.doubleQuoted"));
        }

        @Test
        void testDollarQuoteEnd() {
            assertEquals(5, QueryUtil.dollarQuoteEnd("$$a$$", 0));
            assertEquals(4, QueryUtil.dollarQuoteEnd("$$$$ x", 0));
            assertEquals(14, QueryUtil.dollarQuoteEnd("x = $t$a$$b$t$", 4));
            assertEquals(18, QueryUtil.dollarQuoteEnd("$tag_1$it's$tag_1$ AS x", 0));
            // Unterminated.
            assertEquals(-1, QueryUtil.dollarQuoteEnd("$$a", 0));
            assertEquals(-1, QueryUtil.dollarQuoteEnd("$t$a$$", 0));
            // No opener: a name continues before the '$', a positional marker, a MyBatis marker, a '$' that is no tag, another character.
            assertEquals(0, QueryUtil.dollarQuoteEnd("a$$b$$", 1));
            assertEquals(0, QueryUtil.dollarQuoteEnd("$1", 0));
            assertEquals(0, QueryUtil.dollarQuoteEnd("${x}", 0));
            assertEquals(0, QueryUtil.dollarQuoteEnd("$1$ a $1$", 0));
            assertEquals(0, QueryUtil.dollarQuoteEnd("$col", 0));
            assertEquals(0, QueryUtil.dollarQuoteEnd("x$$", 0));
            assertEquals(0, QueryUtil.dollarQuoteEnd("é$$a$$", 1));
            // H2 reads a Unicode space as a separator.
            assertEquals(6, QueryUtil.dollarQuoteEnd("　$$a$$", 1));

            assertThrows(IllegalArgumentException.class, () -> QueryUtil.dollarQuoteEnd(null, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> QueryUtil.dollarQuoteEnd("$$", 2));
        }

        // Regression: PostgreSQL and H2 read a dollar-quoted string as text, so in "$$'$$ /* x '" the quote is data and the comment
        // is open; every reading of the scan took the quote for the start of a string that hid the opener.
        @Test
        void testHasUnterminatedBlockCommentDollarQuoteReading() {
            assertTrue(QueryUtil.hasUnterminatedBlockComment("$$'$$ /* x '"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("$t$'$t$ /* x '"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("a = $$it's$$ AND b = 'c' /* x 'd'"));
            // An opener inside a dollar-quoted string is still open under the reading without dollar quotes (fail closed).
            assertTrue(QueryUtil.hasUnterminatedBlockComment("$$ /* $$"));

            assertFalse(QueryUtil.hasUnterminatedBlockComment("$$a$$ /* x */"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("$$'$$ /* x */ '"));
            // An unterminated dollar quote holds the rest of the text; a "$$" glued to a name opens none.
            assertFalse(QueryUtil.hasUnterminatedBlockComment("$$'a /* x '"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("a$$'$$ /* x '"));
            // "$1" and "${x}" are no dollar quotes.
            assertFalse(QueryUtil.hasUnterminatedBlockComment("$1 = '/*' AND ${x} = 1"));
            // H2 reads a Unicode space before "$$" as a separator, PostgreSQL as part of a name: the opener counts.
            assertTrue(QueryUtil.hasUnterminatedBlockComment("　$$'$$ /* x '"));
        }

        @Test
        void testHasUnterminatedBlockCommentIsLinear() {
            // Regression: resuming after each unterminated quote, and searching for '\r' from every "--", was quadratic
            // (80 KB of "\"\\" took seconds). Each input below takes milliseconds when the scan is linear.
            final String quotes = "/*x*/ a" + "\"\\".repeat(100_000) + "'\\".repeat(50_000) + "`\\".repeat(50_000);
            final String comments = "a /* x */ " + "-- c\n".repeat(60_000) + "--\r".repeat(60_000);

            final long start = System.nanoTime();
            assertFalse(QueryUtil.hasUnterminatedBlockComment(quotes));
            assertFalse(QueryUtil.hasUnterminatedBlockComment(comments));
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 5_000, "hasUnterminatedBlockComment is not linear");
        }

        // Regression: MySQL/MariaDB read "--" as a comment only before whitespace, a control character or the end, so "t --/*" is
        // "t", "-", "-" and an open block comment there; the scan read every "--" as a line comment and accepted it.
        @Test
        void testHasUnterminatedBlockCommentMySqlDashReading() {
            assertTrue(QueryUtil.hasUnterminatedBlockComment("t --/*"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("t ---/*"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("t --x /* y"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("t --/* x */ u --/*"));

            // Whitespace, a control character, DEL or the end of the text after "--" starts a comment in MySQL too.
            assertFalse(QueryUtil.hasUnterminatedBlockComment("t -- /* x"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("t --" + (char) 0x09 + "/* x"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("t --" + (char) 0x7F + "/* x"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("t /* x */ --"));
            // A closed comment, or quoted dashes, stay accepted under every reading.
            assertFalse(QueryUtil.hasUnterminatedBlockComment("t --/* x */"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("t '--/*'"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("t --x"));
        }

        // Regression: PostgreSQL, SQL Server and H2 nest block comments, so "id /* outer /* inner */" is still open there; the scan
        // stopped at the first "*/" and accepted it.
        @Test
        void testHasUnterminatedBlockCommentNestedReading() {
            assertTrue(QueryUtil.hasUnterminatedBlockComment("id /* outer /* inner */"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("/*/**/"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("a /* x */ b /* y /* z */"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("a /* x /* y */ */ /* z"));
            // Quotes are not special inside a comment, so a quote there does not hide a nested opener.
            assertTrue(QueryUtil.hasUnterminatedBlockComment("a /* it's /* x */"));

            // Balanced nesting, or comments one after another, are closed under both readings.
            assertFalse(QueryUtil.hasUnterminatedBlockComment("id /* a /* b */ */"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("id /* a */ /* b */"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("/* a /* b /* c */ */ */ x"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("/**/ /**/"));
            // A quoted "/*" is data, not a nested opener.
            assertFalse(QueryUtil.hasUnterminatedBlockComment("'/*' /* a */"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("/* a */ \"/*\""));
        }

        @Test
        void testHasUnterminatedBlockCommentReadings() {
            // Covers every lexical reading of the fail-closed sort-key / DISTINCT ON check.
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.hasUnterminatedBlockComment(null));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("a DESC"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("a /* c */"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("a /* c"));

            // Quoted text, doubled quotes and line comments hide an opener.
            assertFalse(QueryUtil.hasUnterminatedBlockComment("COALESCE(a, '/*')"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("\"a/*b\""));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("'it''s /*'"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("a -- /*\n"));

            // Backslash escapes: '\'' is closed under one reading and open under the other.
            assertTrue(QueryUtil.hasUnterminatedBlockComment("'a\\'/*'"));
            // A "--" comment ended by a lone '\r' (default tokenizer) exposes the opener; MySQL/SQLite end it only at '\n'.
            assertTrue(QueryUtil.hasUnterminatedBlockComment("a -- c\r/* x"));
            // A stray quote in a '#' comment or an [it's] identifier must not hide the opener, even if a later quote pairs with it.
            assertTrue(QueryUtil.hasUnterminatedBlockComment("a # it's\n/* x 'y'"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("[it's] /* it's"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("[a'] /* x '"));
            // Fail-closed trade-offs: one reading sees an open comment.
            assertTrue(QueryUtil.hasUnterminatedBlockComment("[a/*b]"));
            assertTrue(QueryUtil.hasUnterminatedBlockComment("a # note /*"));
        }

        // Regression: QueryUtil.terminateLineComment (used by every condition renderer) missed a comment after a dollar-quoted
        // string holding a quote, so "(b = $$'$$ -- c) AND (id = ?)" commented out the rest of the WHERE clause.
        @Test
        void testTerminateLineCommentAfterDollarQuotedString() {
            assertEquals("$$'$$ -- c\n", QueryUtil.terminateLineComment("$$'$$ -- c"));
            assertEquals("$t$'$t$ -- c\n", QueryUtil.terminateLineComment("$t$'$t$ -- c"));
            assertEquals("a = $$\"$$ -- c\n", QueryUtil.terminateLineComment("a = $$\"$$ -- c"));
            // Read without dollar quotes, the text ends in a comment too: the extra line feed is harmless.
            assertEquals("$$-- c$$\n", QueryUtil.terminateLineComment("$$-- c$$"));

            assertEquals("$$a$$", QueryUtil.terminateLineComment("$$a$$"));
            assertEquals("$$'$$ -- c\n", QueryUtil.terminateLineComment("$$'$$ -- c\n"));
            // "a$$" is a name: the quote opens a string that holds the "--". So is "é$$"; a Unicode space is no name character on H2.
            assertEquals("a$$'$$ -- c", QueryUtil.terminateLineComment("a$$'$$ -- c"));
            assertEquals("é$$'$$ -- c", QueryUtil.terminateLineComment("é$$'$$ -- c"));
            assertEquals("　$$'$$ -- c\n", QueryUtil.terminateLineComment("　$$'$$ -- c"));
        }

        @Test
        void testTerminateLineCommentProtectsOnlyOpenCommentBoundaries() {
            for (final String sql : List.of("SELECT 1 -- tail", "SELECT 1 # tail", "SELECT '-- quoted' -- tail")) {
                assertEquals(sql + '\n', QueryUtil.terminateLineComment(sql));
            }

            for (final String sql : List.of("", "SELECT 1", "SELECT '-- quoted'", "SELECT '# quoted'", "SELECT [-- quoted]", "SELECT 1 /* tail */",
                    "SELECT 1 -- tail\n", "SELECT 1 # tail\r\n")) {
                assertSame(sql, QueryUtil.terminateLineComment(sql), sql);
            }

            assertThrows(IllegalArgumentException.class, () -> QueryUtil.terminateLineComment(null));
        }

        // R14-02: a fragment without '-' and '#' can never end inside a line comment, so it is returned as is without the
        // multi-reading scan; fragments that do carry a comment opener keep their exact prior results.
        @Test
        void testTerminateLineCommentShortCircuitsFragmentsWithoutCommentOpeners() {
            // The dialect-neutral reading the short circuit relies on never takes an H2 "//" for a line comment.
            final String doubleSlash = "a // c";
            assertSame(doubleSlash, QueryUtil.terminateLineComment(doubleSlash));

            for (final String sql : List.of("age", "customer_account_number", "first_name = 'x' /* c */", "'--'", "a -- c\n", "a # c\r\n")) {
                assertSame(sql, QueryUtil.terminateLineComment(sql), sql);
            }

            assertEquals("a -- c\n", QueryUtil.terminateLineComment("a -- c"));
            assertEquals("a # c\n", QueryUtil.terminateLineComment("a # c"));
        }

        @Test
        void testTerminateLineCommentSupportsStandardAndEscapeStrings() {
            for (final String sql : List.of("'a\\' -- tail", "'a\\' # tail", "N'a\\' -- tail", "E'a\\' -- quoted' -- tail", "a\u0301E'a\\' -- tail")) {
                assertEquals(sql + '\n', QueryUtil.terminateLineComment(sql), sql);
            }
            for (final String sql : List.of("'a\\' -- tail\n", "'a\\'", "E'a\\' -- quoted'", "e'a\\' # quoted'")) {
                assertSame(sql, QueryUtil.terminateLineComment(sql), sql);
            }
        }
    }

    @Nested
    class Placeholders {
        @Test
        void testPlaceholdersComposeInsertValues() {
            String placeholders = QueryUtil.placeholders(3);
            assertEquals("?, ?, ?", placeholders);
            assertEquals("INSERT INTO users (name, email, age) VALUES (?, ?, ?)", "INSERT INTO users (name, email, age) VALUES (" + placeholders + ")");
        }

        @ParameterizedTest(name = "negative placeholder count: {0}")
        @ValueSource(ints = { -1, Integer.MIN_VALUE })
        void testPlaceholdersRejectNegativeCounts(int count) {
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.placeholders(count));
        }

        @ParameterizedTest(name = "{0} placeholders preserve count and separators")
        @MethodSource("placeholderCounts")
        void testPlaceholdersRenderExactCountAndSeparators(int count) {
            String expected = String.join(", ", Collections.nCopies(count, "?"));
            assertEquals(expected, QueryUtil.placeholders(count));
            assertEquals(expected, QueryUtil.placeholders(count), "repeated lookup");
        }

        private static IntStream placeholderCounts() {
            return IntStream.concat(IntStream.rangeClosed(0, 31), IntStream.of(50, 99, 100, 101, 199, 200, 201, 299, 300, 301, 499, 500, 501, 999, 1000, 1001));
        }
    }

    @Nested
    class PropertySelection {
        @Test
        void testGetIdFieldNames() {
            List<String> ids = QueryUtil.idPropNames(Account.class);
            assertNotNull(ids);
        }

        @Test
        void testGetIdFieldNames_NullCheck() {
            assertThrows(IllegalArgumentException.class, () -> {
                QueryUtil.idPropNames(null);
            });
        }

        @Test
        void testGetInsertPropNames_EmptyExcluded() {
            Collection<String> props = QueryUtil.insertPropNames(Account.class, new HashSet<>());
            assertNotNull(props);
        }

        // --- 2026-06-22 API review: get*PropNames now return immutable lists on BOTH paths ---

        @Test
        void testGetInsertPropNames_ReturnsImmutableListBothPaths() {
            // no-exclusion path
            ImmutableList<String> noExcl = QueryUtil.insertPropNames(TestUser.class, null);
            assertThrows(UnsupportedOperationException.class, () -> noExcl.add("x"));

            // exclusion path
            ImmutableList<String> withExcl = QueryUtil.insertPropNames(TestUser.class, new HashSet<>(Arrays.asList("email")));
            assertThrows(UnsupportedOperationException.class, () -> withExcl.add("x"));

            // entity-based no-exclusion path
            ImmutableList<String> entityNoExcl = QueryUtil.insertPropNames(new TestUser(), null);
            assertThrows(UnsupportedOperationException.class, () -> entityNoExcl.add("x"));
        }

        @Test
        void testGetInsertPropNames_WithClass() {
            Collection<String> props = QueryUtil.insertPropNames(Account.class, Collections.emptySet());
            assertNotNull(props);
        }

        @Test
        void testGetInsertPropNames_WithClassAndExcluded() {
            Set<String> excluded = new HashSet<>();
            excluded.add("createdTime");
            Collection<String> props = QueryUtil.insertPropNames(Account.class, excluded);
            assertNotNull(props);
        }

        @Test
        void testGetInsertPropNames_WithClassNullCheck() {
            assertThrows(IllegalArgumentException.class, () -> {
                QueryUtil.insertPropNames(null, Collections.emptySet());
            });
        }

        @Test
        void testGetInsertPropNames_WithEntity() {
            Account account = new Account();
            Collection<String> props = QueryUtil.insertPropNames(account, Collections.emptySet());
            assertNotNull(props);
        }

        @Test
        void testGetInsertPropNames_WithEntityNullCheck() {
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.insertPropNames((Object) null, Collections.emptySet()));
        }

        @Test
        void testGetInsertPropNames_WithExcludedProps() {
            Account account = new Account();
            Set<String> excluded = new HashSet<>();
            excluded.add("id");
            Collection<String> props = QueryUtil.insertPropNames(account, excluded);
            assertNotNull(props);
            assertFalse(props.contains("id"));
        }

        @Test
        void testGetInsertPropNamesWithClass() {
            Set<String> excludedProps = new HashSet<>();

            // Test without exclusions
            Collection<String> props = QueryUtil.insertPropNames(TestUser.class, excludedProps);
            assertNotNull(props);
            assertTrue(props.contains("id"));
            assertTrue(props.contains("name"));
            assertFalse(props.contains("email"));

            // Test with exclusions
            excludedProps.add("email");
            props = QueryUtil.insertPropNames(TestUser.class, excludedProps);
            assertTrue(props.contains("id"));
            assertTrue(props.contains("name"));
            assertFalse(props.contains("email"));
        }

        @Test
        void testGetInsertPropNamesWithEntity() {
            TestUser user = new TestUser();

            // Test with default ID value (should exclude ID)
            user.setId(null);
            Set<String> excludedProps = new HashSet<>();
            Collection<String> props = QueryUtil.insertPropNames(user, excludedProps);
            assertNotNull(props);
            assertFalse(props.contains("id"));
            assertTrue(props.contains("name"));
            assertFalse(props.contains("email"));

            // Test with non-default ID value (should include ID)
            user.setId(123L);
            props = QueryUtil.insertPropNames(user, excludedProps);
            assertTrue(props.contains("id"));
            assertTrue(props.contains("name"));
            assertFalse(props.contains("email"));

            // Test with excluded properties
            excludedProps.add("email");
            props = QueryUtil.insertPropNames(user, excludedProps);
            assertTrue(props.contains("name"));
            assertFalse(props.contains("email"));
        }

        @Test
        void testGetInsertUpdatePropNames_NoExclusionReturnStableInstance() {
            assertSame(QueryUtil.insertPropNames(TestUser.class, null), QueryUtil.insertPropNames(TestUser.class, null));
            assertSame(QueryUtil.updatePropNames(TestUser.class, null), QueryUtil.updatePropNames(TestUser.class, null));
        }

        @Test
        void testGetSelectPropNames() {
            Collection<String> props = QueryUtil.selectPropNames(Account.class, false, Collections.emptySet());
            assertNotNull(props);
        }

        @Test
        void testGetSelectPropNames_ConcurrentFirstAccessReturnsSafelyPublishedStableInstance() throws InterruptedException {
            final int threadCount = 24;
            final ImmutableList<?>[] results = new ImmutableList<?>[threadCount];
            final CountDownLatch ready = new CountDownLatch(threadCount);
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(threadCount);
            final Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    ready.countDown();

                    try {
                        start.await();
                        results[index] = QueryUtil.selectPropNames(ConcurrentCacheEntity.class, false, null);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
                threads[i].setDaemon(true);
                threads[i].start();
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS), "Worker threads did not become ready");
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS), "Concurrent cache initialization did not complete");

            for (final Thread thread : threads) {
                thread.join();
            }

            assertNotNull(results[0]);
            assertTrue(results[0].contains("id"));
            assertTrue(results[0].contains("value"));

            for (int i = 1; i < results.length; i++) {
                assertSame(results[0], results[i]);
            }
        }

        @Test
        void testGetSelectPropNames_EmptyExcluded() {
            Collection<String> props = QueryUtil.selectPropNames(Account.class, false, new HashSet<>());
            assertNotNull(props);
        }

        @Test
        void testGetSelectPropNames_IncludeSubEntity() {
            Collection<String> props = QueryUtil.selectPropNames(Account.class, true, Collections.emptySet());
            assertNotNull(props);
        }

        @Test
        void testGetSelectPropNames_InstanceOverloadDelegatesToClass() {
            TestUser user = new TestUser();
            ImmutableList<String> viaInstance = QueryUtil.selectPropNames(user, false, null);
            ImmutableList<String> viaClass = QueryUtil.selectPropNames(TestUser.class, false, null);
            assertEquals(viaClass, viaInstance);
            assertTrue(viaInstance.contains("name"));
            assertFalse(viaInstance.contains("email"));
        }

        @Test
        void testGetSelectPropNames_InstanceOverloadNullCheck() {
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.selectPropNames((Object) null, false, null));
        }

        @Test
        void testGetSelectPropNames_NoExclusionReturnsStableInstance() {
            // The no-exclusion path is memoized, so repeated calls return the same instance
            // (this identity is relied upon by the builders' == fast paths).
            ImmutableList<String> first = QueryUtil.selectPropNames(TestUser.class, false, null);
            ImmutableList<String> second = QueryUtil.selectPropNames(TestUser.class, false, null);
            assertSame(first, second);

            // Sub-entity variant uses a different slot, so it is a different (but also stable) instance.
            ImmutableList<String> withSub = QueryUtil.selectPropNames(TestUser.class, true, null);
            assertSame(withSub, QueryUtil.selectPropNames(TestUser.class, true, null));
        }

        @Test
        void testGetSelectPropNames_NullCheck() {
            assertThrows(IllegalArgumentException.class, () -> {
                QueryUtil.selectPropNames(null, false, Collections.emptySet());
            });
        }

        @Test
        void testGetSelectPropNames_ReturnsImmutableListBothPaths() {
            ImmutableList<String> noExcl = QueryUtil.selectPropNames(TestUser.class, false, null);
            assertThrows(UnsupportedOperationException.class, () -> noExcl.add("x"));

            ImmutableList<String> withExcl = QueryUtil.selectPropNames(TestUser.class, false, new HashSet<>(Arrays.asList("email")));
            assertThrows(UnsupportedOperationException.class, () -> withExcl.add("x"));
        }

        @Test
        void testGetSelectPropNames_WithExcluded() {
            Set<String> excluded = new HashSet<>();
            excluded.add("id");
            Collection<String> props = QueryUtil.selectPropNames(Account.class, false, excluded);
            assertNotNull(props);
        }

        @Test
        void testGetUpdatePropNames() {
            Collection<String> props = QueryUtil.updatePropNames(Account.class, Collections.emptySet());
            assertNotNull(props);
        }

        @Test
        void testGetUpdatePropNames_EmptyExcluded() {
            Collection<String> props = QueryUtil.updatePropNames(Account.class, new HashSet<>());
            assertNotNull(props);
        }

        @Test
        void testGetUpdatePropNames_InstanceOverloadDelegatesToClass() {
            TestUser user = new TestUser();
            ImmutableList<String> viaInstance = QueryUtil.updatePropNames(user, null);
            ImmutableList<String> viaClass = QueryUtil.updatePropNames(TestUser.class, null);
            assertEquals(viaClass, viaInstance);
            assertTrue(viaInstance.contains("name"));
            assertFalse(viaInstance.contains("email"));
        }

        @Test
        void testGetUpdatePropNames_InstanceOverloadNullCheck() {
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.updatePropNames((Object) null, null));
        }

        @Test
        void testGetUpdatePropNames_NullCheck() {
            assertThrows(IllegalArgumentException.class, () -> {
                QueryUtil.updatePropNames(null, Collections.emptySet());
            });
        }

        @Test
        void testGetUpdatePropNames_ReturnsImmutableListBothPaths() {
            ImmutableList<String> noExcl = QueryUtil.updatePropNames(TestUser.class, null);
            assertThrows(UnsupportedOperationException.class, () -> noExcl.add("x"));

            ImmutableList<String> withExcl = QueryUtil.updatePropNames(TestUser.class, new HashSet<>(Arrays.asList("email")));
            assertThrows(UnsupportedOperationException.class, () -> withExcl.add("x"));
        }

        @Test
        void testGetUpdatePropNames_WithExcluded() {
            Set<String> excluded = new HashSet<>();
            excluded.add("createdTime");
            Collection<String> props = QueryUtil.updatePropNames(Account.class, excluded);
            assertNotNull(props);
        }

        @Test
        void testIncludedSubEntityPropertiesExcludeNonColumns() {
            ImmutableList<String> names = QueryUtil.selectPropNames(ParentWithFilteredChild.class, true, null);

            assertTrue(names.contains("child.visible"), names.toString());
            assertFalse(names.contains("child.hiddenByAnnotation"), names.toString());
            assertFalse(names.contains("child.hiddenByTable"), names.toString());
        }

        @Test
        void testIsNonColumn() {
            Set<String> columnFields = new HashSet<>();
            columnFields.add("id");
            columnFields.add("firstName");

            Set<String> nonColumnFields = new HashSet<>();
            nonColumnFields.add("tempField");

            BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
            PropInfo propInfo = beanInfo.getPropInfo("id");

            assertFalse(QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo));
        }

        @Test
        void testIsNonColumn_EmptySets() {
            Set<String> columnFields = new HashSet<>();
            Set<String> nonColumnFields = new HashSet<>();

            BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
            PropInfo propInfo = beanInfo.getPropInfo("id");

            boolean result = QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo);
            assertFalse(result);
        }

        @Test
        void testIsNonColumn_NonColumnField() {
            Set<String> columnFields = new HashSet<>();
            columnFields.add("id");

            Set<String> nonColumnFields = new HashSet<>();
            nonColumnFields.add("tempField");

            BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
            PropInfo tempProp = beanInfo.getPropInfo("tempField");

            if (tempProp != null) {
                assertTrue(QueryUtil.isNonColumn(columnFields, nonColumnFields, tempProp));
            }
        }

        @Test
        void testIsNonColumn_NullPropInfoThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.isNonColumn(Collections.emptySet(), Collections.emptySet(), null));
        }

        @Test
        void testNestedPropertyExclusionsMatchCompletePathSegments() {
            final Class<?> entityClass = NestedBranch.class;
            final List<String> all = List.of("firstLeaf.value", "secondLeaf.value");

            assertEquals(all, QueryUtil.selectPropNames(entityClass, true, null));
            assertEquals(List.of("secondLeaf.value"), QueryUtil.selectPropNames(entityClass, true, Set.of("firstLeaf")));
            assertEquals(List.of("secondLeaf.value"), QueryUtil.selectPropNames(entityClass, true, Set.of("firstLeaf.value")));
            assertEquals(List.of(), QueryUtil.selectPropNames(entityClass, true, Set.of("firstLeaf", "secondLeaf")));
            assertEquals(all, QueryUtil.selectPropNames(entityClass, true, new HashSet<>(Arrays.asList(null, "", "first", "firstLeaf.", "firstLeaf.val"))));
        }

        @Test
        void testPropertyToColumnMap_AllNonColumnProperties() {
            ImmutableMap<String, String> result = QueryUtil.propToColumnNameMap(QueryUtilTest.NonColumnOnlyEntity.class, NamingPolicy.SNAKE_CASE);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class TableMetadata {
        @Test
        void testGetTableAlias() {
            String alias = QueryUtil.tableAlias(Account.class);
            assertEquals("acc", alias);
        }

        @Test
        void testGetTableAlias_NullCheck() {
            assertThrows(IllegalArgumentException.class, () -> {
                QueryUtil.tableAlias(null);
            });
        }

        @Test
        void testGetTableNameAndAlias() {
            String tableName = QueryUtil.tableNameAndAlias(Account.class);
            assertNotNull(tableName);
        }

        @Test
        void testGetTableNameAndAlias_AliaslessAnnotation() {
            String tableName = QueryUtil.tableNameAndAlias(QueryUtilTest.AliaslessEntity.class, null);

            assertEquals("aliasless_table", tableName);
        }

        @Test
        void testGetTableNameAndAlias_MatchesBuilderResolverForEveryNamingPolicy() {
            // Regression: the class-name-derived branch applied NamingPolicy.convert() directly, while the
            // builders go through SqlBuilder.getTableName(), which keeps the simple class name untouched for
            // KEBAB_CASE / UPPER_CAMEL_CASE -- so the helper rendered "alias-only-entity ao" where the
            // FROM clause said "AliasOnlyEntity ao".
            for (final NamingPolicy np : NamingPolicy.values()) {
                assertEquals(AbstractQueryBuilder.getTableName(AliasOnlyEntity.class, np) + " ao", QueryUtil.tableNameAndAlias(AliasOnlyEntity.class, np),
                        np.name());
                assertEquals(AbstractQueryBuilder.getTableName(Account.class, np) + " " + AbstractQueryBuilder.tableAlias(Account.class),
                        QueryUtil.tableNameAndAlias(Account.class, np), np.name());
            }

            assertEquals("AliasOnlyEntity ao", QueryUtil.tableNameAndAlias(AliasOnlyEntity.class, NamingPolicy.KEBAB_CASE));
            assertEquals("alias_only_entity ao", QueryUtil.tableNameAndAlias(AliasOnlyEntity.class, NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testGetTableNameAndAlias_NoChange() {
            String tableName = QueryUtil.tableNameAndAlias(Account.class, NamingPolicy.NO_CHANGE);
            assertNotNull(tableName);
        }

        @Test
        void testGetTableNameAndAlias_NullCheck() {
            assertThrows(IllegalArgumentException.class, () -> {
                QueryUtil.tableNameAndAlias(null);
            });
        }

        @Test
        void testGetTableNameAndAlias_NullCheckWithPolicy() {
            assertThrows(IllegalArgumentException.class, () -> {
                QueryUtil.tableNameAndAlias(null, NamingPolicy.SNAKE_CASE);
            });
        }

        @Test
        void testGetTableNameAndAlias_UpperCase() {
            String tableName = QueryUtil.tableNameAndAlias(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE);
            assertNotNull(tableName);
        }

        @Test
        void testGetTableNameAndAlias_WithNamingPolicy() {
            String tableName = QueryUtil.tableNameAndAlias(Account.class, NamingPolicy.SNAKE_CASE);
            assertNotNull(tableName);
        }

        @Test
        void testGetTableNameAndAliasHonorsTableValueAndAliasOnly() {
            // Regression (2026-07-03): only @Table.name() was read, so the deprecated @Table("...") value
            // form returned "" and an alias-only @Table returned " ao" (leading-space garbage) -- both
            // diverging from the FROM clause the query builders render for the same class.
            assertEquals("value_form_accounts", QueryUtil.tableNameAndAlias(ValueFormEntity.class, NamingPolicy.SNAKE_CASE));
            assertEquals("alias_only_entity ao", QueryUtil.tableNameAndAlias(AliasOnlyEntity.class, NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testQuotedColumnDotsDoNotSuppressTableAliases() {
            final ImmutableMap<String, QueryUtil.ColumnInfo> map = QueryUtil.propToColumnInfoMap(QuotedColumnNames.class, NamingPolicy.SNAKE_CASE);

            for (final String propName : List.of("doubleQuoted", "backtickQuoted", "bracketQuoted", "escapedQuote", "escapedBracket")) {
                final QueryUtil.ColumnInfo column = map.get(propName);
                assertTrue(column.isUnqualified(), propName);
                assertEquals(column, map.get(column.columnName()));
                final Selection selection = Selection.builder(QuotedColumnNames.class).tableAlias("q").includedPropNames(List.of(propName)).build();
                assertEquals("SELECT q." + column.columnName() + " AS \"" + propName + "\" FROM quoted_columns q", PSC.selectFrom(selection).build().query());
            }

            assertFalse(map.get("qualified").isUnqualified());
            final Selection qualified = Selection.builder(QuotedColumnNames.class).tableAlias("q").includedPropNames(List.of("qualified")).build();
            assertEquals("SELECT source.\"qualified.dot\" AS \"qualified\" FROM quoted_columns q", PSC.selectFrom(qualified).build().query());
        }
    }

    @Nested
    class LexicalBoundaries {
        @Test
        void testGluedColonScannerSkipsQuotedMarkersAndNestedSubscripts() throws Exception {
            final var scan = QueryUtil.class.getDeclaredMethod("containsGluedColon", String.class, int.class);
            scan.setAccessible(true);
            for (final String token : List.of("name'ignored:colon'", "name#{missing:brace", "array[lo:hi]", "array[nested[lo:hi]]")) {
                assertEquals(false, scan.invoke(null, token, 0), token);
            }
            assertEquals(true, scan.invoke(null, "array[lo:hi]::int", 0));
            assertEquals(true, scan.invoke(null, "name#{ignored:colon}:upper", 0));
        }

        @Test
        void testIdentifierShapeRejectsEmptyMappingsAndNonAsciiEscapeDigits() throws Exception {
            final var unqualified = QueryUtil.class.getDeclaredMethod("isUnqualifiedColumnName", String.class);
            unqualified.setAccessible(true);
            assertEquals(false, unqualified.invoke(null, ""));
            assertEquals(false, unqualified.invoke(null, "U&\"name\" UESCAPE E'\\xＦ1'"));
            final var identifier = QueryUtil.class.getDeclaredMethod("isIdentifierNameChar", char.class);
            identifier.setAccessible(true);
            assertEquals(true, identifier.invoke(null, '名'));
            assertEquals(true, identifier.invoke(null, '\uD83D'));
            assertEquals(true, identifier.invoke(null, '\u0301'));
            assertEquals(false, identifier.invoke(null, '\u3000'));
        }

        @Test
        void testPositionalOffsetsIncludeLeadingWhitespaceAndIgnoreQuotedMarkers() {
            final String sql = " \t\r\n SELECT '?' AS literal, ? AS first_value FROM t WHERE id = ? ";
            assertArrayEquals(new int[] { sql.indexOf("? AS first_value"), sql.lastIndexOf('?') }, QueryUtil.positionalParameterOffsets(sql));
        }

        @Test
        void testRawExpressionFallsBackWhenDollarQuotedTokensCannotBeLocated() {
            final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer();
            final String sql = "firstName = $$unclosed";
            final StringBuilder result = new StringBuilder("WHERE ");
            assertEquals(0, QueryUtil.appendRawExpression(result, sql, List.of("otherName", "=", "$$unclosed"), tokenizer, false, String::toUpperCase,
                    String::toUpperCase));
            assertEquals("WHERE " + sql, result.toString());
        }

        @Test
        void testRawTokenAlignmentMarksTheRemainingTokensUnlocated() throws Exception {
            final var offsets = QueryUtil.class.getDeclaredMethod("tokenSourceOffsets", String.class, List.class, int.class);
            offsets.setAccessible(true);
            assertArrayEquals(new int[] { 0, -1, -1 }, (int[]) offsets.invoke(null, "a", List.of("a", "b", "c"), 2));
            assertArrayEquals(new int[] { 0, -1 }, (int[]) offsets.invoke(null, "a /* unfinished", List.of("a", "b"), 1));
        }

        @Test
        void testSubscriptCommentGuardPreservesQuotedAndBracketedText() throws Exception {
            final var contains = QueryUtil.class.getDeclaredMethod("containsUnquotedCommentOrHash", String.class);
            contains.setAccessible(true);
            assertEquals(true, contains.invoke(null, "[name'#']"));
            assertEquals(false, contains.invoke(null, "'#' / -"));
            assertEquals(true, contains.invoke(null, "'#' + #"));
            final var ends = QueryUtil.class.getDeclaredMethod("endsWithDelimiter", String.class);
            ends.setAccessible(true);
            assertEquals(false, ends.invoke(null, ""));
            assertEquals(false, ends.invoke(null, "'"));
            assertEquals(false, ends.invoke(null, "name"));
            assertEquals(true, ends.invoke(null, "`name`"));
        }

        @Test
        void testSubscriptRenderingPreservesCommentAndQuotedContents() {
            final int[] verbatimFrom = new int[1];
            for (final String interior : List.of("'a''b\\c'", "/* \\ ignored */ firstName", "-- \\ ignored\n firstName", "/* \\ unclosed", "$$firstName$$",
                    "$$firstName")) {
                final String expression = "myTags[" + interior + "]";
                assertEquals("my_tags[" + interior + "]", renderRaw(expression, false, verbatimFrom), expression);
            }
        }

        @Test
        void testUnicodeEscapeClausesRejectTruncationAndWhitespaceEscapes() {
            final var columns = QueryUtil.propToColumnInfoMap(IncompleteUnicodeColumns.class, NamingPolicy.NO_CHANGE);
            for (final String property : List.of("unclosed", "missingLiteral", "trailingBackslash", "formFeed", "carriageReturn", "tab")) {
                assertFalse(columns.get(property).isUnqualified(), property);
            }
            // A backspace is a single non-whitespace character, so it remains a valid escape delimiter.
            assertTrue(columns.get("backspace").isUnqualified());
        }

        @Test
        void testUnterminatedBracketDoesNotHideAnUnterminatedBlockComment() {
            assertTrue(QueryUtil.hasUnterminatedBlockComment("SELECT [unclosed /* unfinished"));
            assertFalse(QueryUtil.hasUnterminatedBlockComment("SELECT [unclosed /* finished */"));
        }
    }

    @Nested
    class MetadataPublication {
        @Test
        void testConcurrentColumnInfoLookupsShareOneCompletedMap() throws Exception {
            final List<ImmutableMap<String, QueryUtil.ColumnInfo>> results = concurrentMetadata(
                    () -> QueryUtil.propToColumnInfoMap(ConcurrentColumnEntity.class, NamingPolicy.SNAKE_CASE));

            assertTrue(results.get(0).containsKey("first.firstName"));
            for (final ImmutableMap<String, QueryUtil.ColumnInfo> result : results) {
                assertSame(results.get(0), result);
            }
        }

        @Test
        void testConcurrentNameLookupsShareOneCompletedMap() throws Exception {
            final List<ImmutableMap<String, String>> results = concurrentMetadata(
                    () -> QueryUtil.propToColumnNameMap(ConcurrentNameEntity.class, NamingPolicy.SNAKE_CASE));

            assertTrue(results.get(0).containsKey("first.firstName"));
            for (final ImmutableMap<String, String> result : results) {
                assertSame(results.get(0), result);
            }
        }

        @Test
        void testConcurrentNestedMetadataCallbacksDoNotDeadlock(@TempDir final Path directory) throws Exception {
            // A regression must not leave ParserUtil's global monitor locked in the suite's JVM.
            final Path output = directory.resolve("metadata-callback.txt");
            final Path javaBin = Path.of(System.getProperty("java.home"), "bin");
            final Path java = javaBin.resolve(Files.isRegularFile(javaBin.resolve("java.exe")) ? "java.exe" : "java");
            final Process child = new ProcessBuilder(java.toString(), "-Dfile.encoding=UTF-8", "-Dabacus.query.maxNestedPropDepth=2", "-cp",
                    metadataProbeClasspath(), MetadataCallbackProbe.class.getName()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            final boolean completed;

            try {
                completed = child.waitFor(40, TimeUnit.SECONDS);
            } finally {
                if (child.isAlive()) {
                    child.destroyForcibly();
                    assertTrue(child.waitFor(5, TimeUnit.SECONDS), "Metadata callback probe did not terminate after being killed");
                }
            }

            final String diagnostic = Files.readString(output, StandardCharsets.UTF_8);
            assertTrue(completed, "Metadata callback probe timed out:\n" + diagnostic);
            assertEquals(0, child.exitValue(), diagnostic);
            assertTrue(diagnostic.contains("metadata-callback-check=passed"), diagnostic);
        }

        @Test
        void testFailedNestedMetadataDoesNotPublishCacheHolders() throws Exception {
            final Map<?, ?> names = metadataCache("entityTablePropColumnNameMap");
            final Map<?, ?> infos = metadataCache("entityPropColumnInfoMap");
            ParserUtil.getBeanInfo(FailedNestedRoot.class); // Root metadata is valid; only the nested bean fails.
            assertFalse(names.containsKey(FailedNestedRoot.class));
            assertFalse(infos.containsKey(FailedNestedRoot.class));

            try {
                for (int attempt = 0; attempt < 2; attempt++) {
                    final UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class,
                            () -> QueryUtil.propToColumnNameMap(FailedNestedRoot.class, NamingPolicy.SNAKE_CASE));
                    assertEquals("Date format cannot be 'long' for type java.time.LocalTime/LocalDate", error.getMessage());
                    assertFalse(names.containsKey(FailedNestedRoot.class), "A failed nested build must not leave an empty class holder");
                    assertThrows(UnsupportedOperationException.class, () -> QueryUtil.propToColumnInfoMap(FailedNestedRoot.class, NamingPolicy.SNAKE_CASE));
                    assertFalse(names.containsKey(FailedNestedRoot.class));
                    assertFalse(infos.containsKey(FailedNestedRoot.class));
                }
            } finally {
                names.remove(FailedNestedRoot.class);
                infos.remove(FailedNestedRoot.class);
            }
        }

        @Test
        void testMapClassesDoNotBecomeColumnInfoCacheKeys() throws Exception {
            final Field field = QueryUtil.class.getDeclaredField("entityPropColumnInfoMap");
            field.setAccessible(true);
            final Map<?, ?> cache = (Map<?, ?>) field.get(null);

            assertFalse(cache.containsKey(UnmappedMap.class));
            for (final NamingPolicy policy : NamingPolicy.values()) {
                assertSame(ImmutableMap.empty(), QueryUtil.propToColumnInfoMap(UnmappedMap.class, policy));
            }
            assertSame(ImmutableMap.empty(), QueryUtil.propToColumnInfoMap(UnmappedMap.class, null));
            assertFalse(cache.containsKey(UnmappedMap.class), "An empty mapping must not pin an otherwise unused Map class");
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.propToColumnInfoMap(null, null));
        }

        @Test
        void testRecursiveMappingDoesNotReuseAnotherTopLevelAncestorContext() {
            final ImmutableMap<String, String> child = QueryUtil.propToColumnNameMap(MappingChild.class, NamingPolicy.SNAKE_CASE);
            assertTrue(child.containsKey("parent.label"));
            assertFalse(child.containsKey("parent.child.code"));

            final ImmutableMap<String, String> parent = QueryUtil.propToColumnNameMap(MappingParent.class, NamingPolicy.SNAKE_CASE);
            assertTrue(parent.containsKey("child.code"));
            assertFalse(parent.containsKey("child.parent.label"));
            assertSame(parent, QueryUtil.registerEntityPropColumnNameMap(MappingParent.class, NamingPolicy.SNAKE_CASE, null));
            assertTrue(QueryUtil.registerEntityPropColumnNameMap(MappingParent.class, NamingPolicy.SNAKE_CASE, Set.of(MappingParent.class)).isEmpty());
        }

        @Test
        void testRecursiveMetadataCallbacksReturnThePublishedWinner() {
            // ParserUtil runs a bean constructor while it builds the bean's metadata. A constructor that asks QueryUtil about its
            // own class re-enters the cold cache on the same thread, which a computeIfAbsent build rejected as a "Recursive update".
            RecursiveMetadataCallback.callback = () -> QueryUtil.columnToPropNameMap(RecursiveColumnCallback.class);
            final ImmutableMap<String, String> columns = QueryUtil.columnToPropNameMap(RecursiveColumnCallback.class);
            assertEquals("name", columns.get("CALLBACK_NAME"));
            assertSame(columns, RecursiveMetadataCallback.callbackResult);

            final RecursiveInsertCallback entity = new RecursiveInsertCallback();
            RecursiveMetadataCallback.callback = () -> QueryUtil.insertPropNames(new RecursiveInsertCallback(), null);
            final ImmutableList<String> insertNames = QueryUtil.insertPropNames(entity, null);
            assertEquals(List.of("name"), insertNames);
            assertSame(insertNames, RecursiveMetadataCallback.callbackResult);

            RecursiveMetadataCallback.callback = () -> QueryUtil.selectPropNames(RecursiveSelectCallback.class, false, null);
            final ImmutableList<String> selectNames = QueryUtil.selectPropNames(RecursiveSelectCallback.class, false, null);
            assertEquals(List.of("name", "id"), selectNames);
            assertSame(selectNames, RecursiveMetadataCallback.callbackResult);
        }

        @Test
        void testRejectedClassesDoNotBecomePropNameCacheKeys() throws Exception {
            final Map<?, ?> cache = metadataCache("noExclusionPropNamesPool");
            final Class<?> anonymousMap = new HashMap<String, Object>() {
            }.getClass();

            for (final Class<?> rejected : List.of(anonymousMap, UnmappedMap.class, String.class)) {
                assertThrows(IllegalArgumentException.class, () -> QueryUtil.selectPropNames(rejected, false, null));
                assertThrows(IllegalArgumentException.class, () -> QueryUtil.insertPropNames(rejected, null));
                assertThrows(IllegalArgumentException.class, () -> QueryUtil.updatePropNames(rejected, null));
                assertFalse(cache.containsKey(rejected), "A rejected class must not pin its class loader: " + rejected.getName());
            }

            // An accepted class is cached, and every no-exclusion call returns its one stable list.
            final ImmutableList<String> names = QueryUtil.selectPropNames(ExclusionLeaf.class, false, null);
            assertTrue(cache.containsKey(ExclusionLeaf.class));
            assertSame(names, QueryUtil.selectPropNames(ExclusionLeaf.class, false, Collections.emptySet()));
        }
    }

    @Nested
    class NestedExclusionLookup {
        @Test
        void testLargeExclusionsPreservePathBoundariesNullsAndOrder() {
            final Set<String> exclusions = exclusionMisses(32);
            exclusions.addAll(Arrays.asList(null, "", "chil", "child.", "child.first", "childish."));
            final List<String> all = List.of("visible", "childish", "child.firstName", "child.lastName");

            assertEquals(all, QueryUtil.selectPropNames(ExclusionRoot.class, true, exclusions));
            exclusions.add("child.firstName");
            assertEquals(List.of("visible", "childish", "child.lastName"), QueryUtil.selectPropNames(ExclusionRoot.class, true, exclusions));
            exclusions.add("child");
            final ImmutableList<String> result = QueryUtil.selectPropNames(ExclusionRoot.class, true, exclusions);
            assertEquals(List.of("visible", "childish"), result);
            assertThrows(UnsupportedOperationException.class, () -> result.add("other"));
            assertSame(QueryUtil.selectPropNames(ExclusionRoot.class, true, null),
                    QueryUtil.selectPropNames(ExclusionRoot.class, true, Collections.emptySet()));
        }

        @Test
        void testLargeExclusionsUseExactStringsForComparatorAndIdentitySets() {
            final Set<String> insensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            insensitive.addAll(exclusionMisses(32));
            insensitive.add("CHILD");
            assertEquals(List.of("visible", "childish", "child.firstName", "child.lastName"),
                    QueryUtil.selectPropNames(ExclusionRoot.class, true, insensitive));

            final Set<String> identities = Collections.newSetFromMap(new IdentityHashMap<>());
            identities.addAll(exclusionMisses(32));
            identities.add(new String("child.firstName"));
            identities.add(new String("child.firstName"));
            identities.add(null);
            assertEquals(List.of("visible", "childish", "child.lastName"), QueryUtil.selectPropNames(ExclusionRoot.class, true, identities));

            insensitive.remove("CHILD");
            insensitive.add("child");
            assertEquals(List.of("visible", "childish"), QueryUtil.selectPropNames(ExclusionRoot.class, true, insensitive));
        }

        @Test
        void testLargeExclusionsVisitIrrelevantCallerEntriesOnlyLinearly() {
            final CountingExclusions exclusions = new CountingExclusions(exclusionMisses(64));
            assertEquals(List.of("visible", "childish", "child.firstName", "child.lastName"), QueryUtil.selectPropNames(ExclusionRoot.class, true, exclusions));
            assertTrue(exclusions.visits <= exclusions.size() * 2L, "Do not traverse the caller's whole set for every property");
        }

        @Test
        void testSmallExclusionsRetainExactStringSemantics() {
            final Set<String> insensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            insensitive.add("CHILD");
            assertEquals(List.of("visible", "childish", "child.firstName", "child.lastName"),
                    QueryUtil.selectPropNames(ExclusionRoot.class, true, insensitive));
            assertEquals(List.of("visible", "childish"), QueryUtil.selectPropNames(ExclusionRoot.class, true, Set.of("child")));
            assertEquals(List.of("visible", "childish", "child.firstName", "child.lastName"),
                    QueryUtil.selectPropNames(ExclusionRoot.class, true, Collections.singleton(null)));
        }
    }

    @Nested
    class QuotedIdentifierConversion {
        @Test
        void testChangedSegmentsPreserveEveryUnchangedSourceSpan() {
            assertEquals("\"fixed\".first_name.[Last].second_name",
                    QueryUtil.convertIdentifier("\"fixed\".firstName.[Last].secondName", NamingPolicy.SNAKE_CASE));
            assertEquals("first_name.\"fixed\".second_name.[last]",
                    QueryUtil.convertIdentifier("firstName.\"fixed\".secondName.[last]", NamingPolicy.SNAKE_CASE));
            assertEquals("..first_name.\"a.b\".last_name..", QueryUtil.convertIdentifier("..FirstName.\"a.b\".LastName..", NamingPolicy.SNAKE_CASE));
            assertEquals("U&\"caf.00e9\" UESCAPE '.'.first_name", QueryUtil.convertIdentifier("U&\"caf.00e9\" UESCAPE '.'.firstName", NamingPolicy.SNAKE_CASE));
            assertEquals("first_name.\"unterminated.LastName", QueryUtil.convertIdentifier("firstName.\"unterminated.LastName", NamingPolicy.SNAKE_CASE));
            assertEquals("__first_name_.\"Exact\"", QueryUtil.convertIdentifier("__FirstName_.\"Exact\"", NamingPolicy.SNAKE_CASE));

            for (final NamingPolicy policy : NamingPolicy.values()) {
                assertEquals(policy.convert("firstName") + ".\"Exact.Name\"." + policy.convert("lastName"),
                        QueryUtil.convertIdentifier("firstName.\"Exact.Name\".lastName", policy), policy.name());
            }
        }

        @Test
        void testUnchangedQuotedNamesReuseTheirOriginalStrings() {
            for (final String text : List.of("\"Exact.Name\"", "`back``tick`", "[a]]b]", "U&\"name\" UESCAPE '!'", "\"first\"..[last]", "\"unterminated",
                    "U&\"name\" UESCAPE E'!!'", "\"fixed\".".repeat(128) + "[last]")) {
                final String source = new String(text);
                for (final NamingPolicy policy : NamingPolicy.values()) {
                    assertSame(source, QueryUtil.convertIdentifier(source, policy), policy + ": " + text);
                }
            }

            final String mixed = new String("p.\"final.name\"");
            assertSame(mixed, QueryUtil.convertIdentifier(mixed, NamingPolicy.SNAKE_CASE));
        }
    }

    @Nested
    class Validation {
        @Test
        void testQualifyingDotScannerRetainsItsRequiredSourcePrecondition() {
            assertThrows(NullPointerException.class, () -> QueryUtil.indexOfQualifyingDot(null));
            assertEquals(-1, QueryUtil.indexOfQualifyingDot(""));
            assertEquals(1, QueryUtil.indexOfQualifyingDot("p.first_name"));
        }

        @Test
        void testRecursiveMetadataRegistrationAndPublicDefaultsStillAcceptNullPolicy() {
            assertTrue(QueryUtil.registerEntityPropColumnNameMap(Person.class, null, new HashSet<>()).containsKey("firstName"));
            assertTrue(QueryUtil.registerEntityPropColumnNameMap(Person.class, null, Collections.singleton(Person.class)).isEmpty());
            assertEquals(QueryUtil.propToColumnNameMap(Person.class, NamingPolicy.SNAKE_CASE), QueryUtil.propToColumnNameMap(Person.class, null));
            assertTrue(QueryUtil.propToColumnNameMap(null, null).isEmpty());
        }

        @Test
        void testTopLevelMetadataRegistrationRejectsNullPolicyBeforeInspectingTheClass() {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> QueryUtil.registerEntityPropColumnNameMap(null, null, null)).getMessage()
                    .contains(cs.entityClass));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> QueryUtil.registerEntityPropColumnNameMap(String.class, null, null)).getMessage()
                    .contains(cs.namingPolicy));
            assertThrows(IllegalArgumentException.class, () -> QueryUtil.registerEntityPropColumnNameMap(Person.class, null, null));
        }

        public static class Person {
            private String firstName;

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(final String firstName) {
                this.firstName = firstName;
            }
        }
    }

    private static <T> List<T> concurrentMetadata(final Supplier<T> supplier) throws Exception {
        final int readers = 16;
        final CountDownLatch ready = new CountDownLatch(readers);
        final CountDownLatch start = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(readers);
        final List<Future<T>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < readers; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return supplier.get();
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS), "Metadata readers did not reach the start barrier");
            start.countDown();
            final List<T> results = new ArrayList<>();
            for (final Future<T> future : futures) {
                results.add(future.get(15, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private static Set<String> exclusionMisses(final int count) {
        final Set<String> exclusions = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            exclusions.add("missing" + i);
        }
        return exclusions;
    }

    private static Map<?, ?> metadataCache(final String name) throws Exception {
        final Field field = QueryUtil.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<?, ?>) field.get(null);
    }

    private static String metadataProbeClasspath() throws Exception {
        final Set<String> entries = new LinkedHashSet<>();
        // Prefer the actually loaded classes, including when a console launcher uses its own child classloader.
        for (final Class<?> type : new Class<?>[] { QueryUtilTest.class, QueryUtil.class, ParserUtil.class, Test.class, ParameterizedTest.class,
                jakarta.xml.bind.annotation.XmlRootElement.class }) {
            entries.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        }
        entries.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        return String.join(File.pathSeparator, entries);
    }

    private static final class CountingExclusions extends AbstractSet<String> {
        private final Set<String> delegate;
        private long visits;

        private CountingExclusions(final Set<String> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public Iterator<String> iterator() {
            final Iterator<String> iterator = delegate.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public String next() {
                    visits++;
                    return iterator.next();
                }
            };
        }
    }

    public static class ExclusionRoot {
        private String visible;
        private ExclusionLeaf child;
        private String childish;

        public String getVisible() {
            return visible;
        }

        public void setVisible(final String value) {
            visible = value;
        }

        public ExclusionLeaf getChild() {
            return child;
        }

        public void setChild(final ExclusionLeaf value) {
            child = value;
        }

        public String getChildish() {
            return childish;
        }

        public void setChildish(final String value) {
            childish = value;
        }
    }

    @Table(alias = "leaf")
    public static class ExclusionLeaf {
        private String firstName;
        private String lastName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String value) {
            firstName = value;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(final String value) {
            lastName = value;
        }
    }

    public static class ConcurrentMetadataBase {
        private ExclusionLeaf first;
        private ExclusionLeaf second;
        private ExclusionLeaf third;

        public ExclusionLeaf getFirst() {
            return first;
        }

        public void setFirst(final ExclusionLeaf value) {
            first = value;
        }

        public ExclusionLeaf getSecond() {
            return second;
        }

        public void setSecond(final ExclusionLeaf value) {
            second = value;
        }

        public ExclusionLeaf getThird() {
            return third;
        }

        public void setThird(final ExclusionLeaf value) {
            third = value;
        }
    }

    public static class ConcurrentColumnEntity extends ConcurrentMetadataBase {
    }

    public static class ConcurrentNameEntity extends ConcurrentMetadataBase {
    }

    public static class FailedNestedRoot {
        private InvalidNestedDate child;

        public InvalidNestedDate getChild() {
            return child;
        }

        public void setChild(final InvalidNestedDate value) {
            child = value;
        }
    }

    public static class InvalidNestedDate {
        @JsonXmlField(dateFormat = "long")
        private LocalDate date;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(final LocalDate value) {
            date = value;
        }
    }

    /** Runs only in a child JVM, so a failed lock-order regression cannot poison the remaining tests. */
    public static final class MetadataCallbackProbe {
        private static final AtomicReference<Supplier<?>> CALLBACK = new AtomicReference<>();
        private static final AtomicReference<Object> CALLBACK_RESULT = new AtomicReference<>();
        private static volatile CountDownLatch constructorEntered;
        private static volatile CountDownLatch callbackAllowed;

        public static void main(final String[] args) {
            try {
                ParserUtil.getBeanInfo(CallbackRoot.class); // Warm only the root; the nested leaf remains cold.
                // Every metadata cache gets its own cold classes: ParserUtil runs a bean constructor only on the bean's first lookup.
                verifyCallbacks(CallbackEntity.class, () -> QueryUtil.propToColumnNameMap(CallbackRoot.class, NamingPolicy.SNAKE_CASE), "child.name");
                verifyCallbacks(ColumnCallbackEntity.class, () -> QueryUtil.columnToPropNameMap(ColumnCallbackTarget.class), "TARGET_NAME");
                verifyCallbacks(InsertCallbackEntity.class, () -> QueryUtil.insertPropNames(new InsertCallbackTarget(), null), "name");
                verifyCallbacks(SelectCallbackEntity.class, () -> QueryUtil.selectPropNames(SelectCallbackRoot.class, true, null), "child.name");
                verifyCallbacks(SubEntityCallbackEntity.class, () -> AbstractQueryBuilder.getSubEntityPropNames(SubEntityCallbackRoot.class), "child");
                System.out.println("metadata-callback-check=passed");
                System.exit(0);
            } catch (final Throwable error) {
                error.printStackTrace(System.err);
                System.exit(1);
            }
        }

        private static void verifyCallbacks(final Class<?> callbackEntity, final Supplier<?> query, final String expected) throws Exception {
            final String name = callbackEntity.getSimpleName();
            final Field field = ParserUtil.class.getDeclaredField("beanInfoPool");
            field.setAccessible(true);
            final Object beanInfoMonitor = field.get(null);
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            final AtomicReference<Object> concurrentResult = new AtomicReference<>();
            final CountDownLatch completed = new CountDownLatch(2);
            constructorEntered = new CountDownLatch(1);
            callbackAllowed = new CountDownLatch(1);
            CALLBACK_RESULT.set(null);
            CALLBACK.set(query);
            final Thread callbackThread = new Thread(() -> {
                try {
                    ParserUtil.getBeanInfo(callbackEntity);
                } catch (final Throwable error) {
                    failure.compareAndSet(null, error);
                } finally {
                    completed.countDown();
                }
            }, "metadata-constructor-callback");
            final Thread queryThread = new Thread(() -> {
                try {
                    concurrentResult.set(query.get());
                } catch (final Throwable error) {
                    failure.compareAndSet(null, error);
                } finally {
                    completed.countDown();
                }
            }, "metadata-nested-query");
            callbackThread.setDaemon(true);
            queryThread.setDaemon(true);

            try {
                callbackThread.start();
                require(constructorEntered.await(10, TimeUnit.SECONDS), name + ": bean constructor did not reach its callback barrier");
                queryThread.start();
                awaitDependencyMonitor(queryThread, callbackThread, beanInfoMonitor);
                // The query is now waiting for ParserUtil's monitor, while the constructor owns it.
                // Only now allow the constructor to re-enter the same top-level QueryUtil mapping.
                callbackAllowed.countDown();
                require(completed.await(10, TimeUnit.SECONDS), name + ": metadata callbacks did not finish; possible policy/dependency lock inversion");
                if (failure.get() != null) {
                    throw new AssertionError(name + ": metadata worker failed", failure.get());
                }
                final Object result = concurrentResult.get();
                require(result instanceof Map<?, ?> map ? map.containsKey(expected) : result instanceof Collection<?> names && names.contains(expected),
                        name + ": metadata was incomplete: " + result);
                require(CALLBACK_RESULT.get() == result, name + ": concurrent builds did not return the published winner");
                System.out.println(name + "=passed");
            } finally {
                callbackAllowed.countDown();
            }
        }

        private static void awaitDependencyMonitor(final Thread waiting, final Thread owner, final Object monitor) throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                final ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(waiting.threadId());
                if (info != null && info.getThreadState() == Thread.State.BLOCKED && info.getLockOwnerId() == owner.threadId() && info.getLockInfo() != null
                        && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(monitor)) {
                    return;
                }
                require(waiting.isAlive(), "Nested query exited before reaching the dependency monitor");
                Thread.sleep(1);
            }
            throw new AssertionError("Nested query did not reach the dependency-monitor barrier");
        }

        private static void require(final boolean condition, final String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        public static class CallbackRoot {
            private CallbackLeaf child;

            public CallbackLeaf getChild() {
                return child;
            }

            public void setChild(final CallbackLeaf value) {
                child = value;
            }
        }

        public static class CallbackLeaf {
            private String name;

            public String getName() {
                return name;
            }

            public void setName(final String value) {
                name = value;
            }
        }

        public static class CallbackEntity {
            private String name;

            public CallbackEntity() {
                final Supplier<?> callback = CALLBACK.getAndSet(null);

                if (callback == null) {
                    return;
                }

                constructorEntered.countDown();
                try {
                    require(callbackAllowed.await(15, TimeUnit.SECONDS), "Metadata callback was not released");
                } catch (final InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(error);
                }
                CALLBACK_RESULT.set(callback.get());
            }

            public String getName() {
                return name;
            }

            public void setName(final String value) {
                name = value;
            }
        }

        public static class ColumnCallbackEntity extends CallbackEntity {
        }

        public static class InsertCallbackEntity extends CallbackEntity {
        }

        public static class SelectCallbackEntity extends CallbackEntity {
        }

        public static class SubEntityCallbackEntity extends CallbackEntity {
        }

        public static class ColumnCallbackTarget {
            @Column("TARGET_NAME")
            private String name;

            public String getName() {
                return name;
            }

            public void setName(final String value) {
                name = value;
            }
        }

        public static class InsertCallbackTarget extends CallbackLeaf {
            private long id;

            public long getId() {
                return id;
            }

            public void setId(final long value) {
                id = value;
            }
        }

        public static class SelectCallbackRoot extends CallbackRoot {
        }

        public static class SubEntityCallbackRoot extends CallbackRoot {
        }
    }

    /** A bean whose constructor, run by ParserUtil while it builds the bean's metadata, asks QueryUtil about its own class. */
    public static class RecursiveMetadataCallback {
        private static Supplier<?> callback;
        private static Object callbackResult;

        @Column("CALLBACK_NAME")
        private String name;
        private long id;

        public RecursiveMetadataCallback() {
            final Supplier<?> action = callback;
            callback = null; // Call back once: the callback may construct this class again.

            if (action != null) {
                callbackResult = action.get();
            }
        }

        public String getName() {
            return name;
        }

        public void setName(final String value) {
            name = value;
        }

        public long getId() {
            return id;
        }

        public void setId(final long value) {
            id = value;
        }
    }

    public static class RecursiveColumnCallback extends RecursiveMetadataCallback {
    }

    public static class RecursiveInsertCallback extends RecursiveMetadataCallback {
    }

    public static class RecursiveSelectCallback extends RecursiveMetadataCallback {
    }

    public static class MappingParent {
        private String label;
        private MappingChild child;

        public String getLabel() {
            return label;
        }

        public void setLabel(final String value) {
            label = value;
        }

        public MappingChild getChild() {
            return child;
        }

        public void setChild(final MappingChild value) {
            child = value;
        }
    }

    public static class MappingChild {
        private String code;
        private MappingParent parent;

        public String getCode() {
            return code;
        }

        public void setCode(final String value) {
            code = value;
        }

        public MappingParent getParent() {
            return parent;
        }

        public void setParent(final MappingParent value) {
            parent = value;
        }
    }

    private static final class UnmappedMap extends HashMap<String, String> {
        private static final long serialVersionUID = 1L;
    }

    @lombok.Data
    static class IncompleteUnicodeColumns {
        @Column("U&\"unclosed")
        private String unclosed;
        @Column("U&\"name\" UESCAPE")
        private String missingLiteral;
        @Column("U&\"name\" UESCAPE E'\\")
        private String trailingBackslash;
        @Column("U&\"name\" UESCAPE E'\\b'")
        private String backspace;
        @Column("U&\"name\" UESCAPE E'\\f'")
        private String formFeed;
        @Column("U&\"name\" UESCAPE E'\\r'")
        private String carriageReturn;
        @Column("U&\"name\" UESCAPE E'\\t'")
        private String tab;
    }

    @Table("value_form_accounts")
    static class ValueFormEntity {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }
    }

    @Table(alias = "ao")
    static class AliasOnlyEntity {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }
    }

    static class CaseDistinctColumns {
        @Column("code")
        private String lowerCaseColumn;

        @Column("CODE")
        private String upperCaseColumn;

        public String getLowerCaseColumn() {
            return lowerCaseColumn;
        }

        public void setLowerCaseColumn(final String lowerCaseColumn) {
            this.lowerCaseColumn = lowerCaseColumn;
        }

        public String getUpperCaseColumn() {
            return upperCaseColumn;
        }

        public void setUpperCaseColumn(final String upperCaseColumn) {
            this.upperCaseColumn = upperCaseColumn;
        }
    }

    @Table(alias = "q")
    static class QualifiedColumnEntity {
        @Column("source.value")
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @lombok.Data
    @Table(name = "quoted_columns", alias = "q")
    static class QuotedColumnNames {
        @Column("\"double.dot\"")
        private String doubleQuoted;

        @Column("`backtick.dot`")
        private String backtickQuoted;

        @Column("[bracket.dot]")
        private String bracketQuoted;

        @Column("\"escaped\"\".dot\"")
        private String escapedQuote;

        @Column("[escaped]].dot]")
        private String escapedBracket;

        @Column("source.\"qualified.dot\"")
        private String qualified;
    }

    @lombok.Data
    static class NestedQuotedColumns {
        private QuotedColumnNames child;
    }

    @lombok.Data
    @Table(name = "unicode_columns", alias = "u")
    static class UnicodeColumnNames {
        @Column("cafe\u0301")
        private String combiningMark;

        @Column("\u0915\u093e")
        private String spacingMark;

        @Column("x\u20dd")
        private String enclosingMark;

        @Column("\uD801\uDC00col")
        private String supplementaryLetter;

        @Column("\u2163value")
        private String letterNumber;

        @Column("a\u203fb")
        private String connectorPunctuation;

        @Column("\uD83D\uDE00value")
        private String supplementarySymbol;

        @Column("U&\"caf\\00e9\"")
        private String unicodeQuoted;

        @Column("u&\"na.me\"")
        private String unicodeQuotedDot;

        @Column("u&\"na\"\"me\"")
        private String unicodeEscapedQuote;

        @Column("U&\"caf!00e9\" UESCAPE '!'")
        private String customEscape;

        @Column("U&\"caf.00e9\" uescape '.'")
        private String dotEscape;

        @Column("U&\"caf[00e9\" UESCAPE '['")
        private String bracketEscape;

        @Column("U&\"caf!00e9\" UESCAPE E'!'")
        private String escapeString;

        @Column("U&\"caf.00e9\" UESCAPE e'.'")
        private String lowercaseEscapeString;

        @Column("U&\"caf\\00e9\" UESCAPE E'\\\\'")
        private String escapedBackslash;

        @Column("U&\"caf!00e9\" UESCAPE E'\\041'")
        private String octalEscapeString;

        @Column("U&\"caf!00e9\" UESCAPE E'\\x21'")
        private String hexEscapeString;

        @Column("U&\"caf!00e9\" UESCAPE E'\\u0021'")
        private String unicodeEscapeString;

        @Column("U&\"name\" UESCAPE E'\\x'")
        private String literalXEscapeString;

        @Column("U&\"value\" UESCAPE E'\\n'")
        private String invalidWhitespaceEscape;

        @Column("U&\"value\" UESCAPE E'\\u002b'")
        private String invalidPlusEscape;

        @Column("U&\"value\" UESCAPE E'!!'")
        private String invalidMultipleEscapes;

        @Column("U&\"value\" UESCAPEE'!'")
        private String invalidEscapeKeyword;

        @Column("U&\"value\" UESCAPE E''")
        private String invalidEmptyEscape;

        @Column("U&\"value\" UESCAPE E'\\u021'")
        private String invalidShortUnicodeEscape;

        @Column("U&\"value\" UESCAPE E'\\U00110000'")
        private String invalidOutOfRangeUnicodeEscape;

        @Column("U&\"value\" UESCAPE E'\\000'")
        private String invalidNullEscape;

        @Column("U&\"value\" UESCAPE E'\\uD800'")
        private String invalidSurrogateEscape;

        @Column("U&\"value\" UESCAPE E'\\x41'")
        private String invalidHexDigitEscape;

        @Column("U&\"value\" UESCAPE E'!' || 'suffix'")
        private String escapeStringExpression;

        @Column("t.U&\"caf\\00e9\"")
        private String qualifiedUnicode;

        @Column("U&\"schema\\002e\".\"value\"")
        private String unicodeQuotedQualifier;

        @Column("U&\"table\" UESCAPE '.'.value")
        private String escapedQualifier;

        @Column("U&\"value\" || 'x'")
        private String unicodeExpression;

        @Column("U&\"value\" UESCAPE '!' || 'x'")
        private String unicodeEscapeExpression;
    }

    @lombok.Data
    static class NestedUnicodeColumns {
        private UnicodeColumnNames child;
    }

    @lombok.Data
    @Table(name = "lexical_columns", alias = "l")
    static class LexicalColumnNames {
        // A mapped column is a rendered identifier, not a SQL script: none of the constructs below hides
        // the qualifier that the generated SQL actually carries.
        @Column("#temp.column")
        private String hashQualified;

        @Column("\"a\\\".b")
        private String backslashQualified;

        @Column("COALESCE(x, 'N.A')")
        private String literalDotExpression;

        @Column("a--b.c")
        private String dashQualified;

        // Expression mappings: punctuation inside them must never be read as identifier quoting, in either
        // direction -- neither hiding a qualifier that follows, nor making the expression look prefixable.
        @Column("COALESCE(x, '[N.A]')")
        private String bracketedLiteral;

        @Column("COALESCE(x, '[') || t.suffix")
        private String unterminatedBracketLiteral;

        @Column("COALESCE(x, 1)")
        private String dotlessExpression;
    }

    @Table(columnFields = { "visible", "hiddenByAnnotation" }, nonColumnFields = { "hiddenByTable" })
    static class ReverseLookupFilteredEntity {
        @Column("visible_column")
        private String visible;

        @Column("hidden_annotation_column")
        @NonColumn
        private String hiddenByAnnotation;

        @Column("hidden_table_column")
        private String hiddenByTable;

        public String getVisible() {
            return visible;
        }

        public void setVisible(final String visible) {
            this.visible = visible;
        }

        public String getHiddenByAnnotation() {
            return hiddenByAnnotation;
        }

        public void setHiddenByAnnotation(final String hiddenByAnnotation) {
            this.hiddenByAnnotation = hiddenByAnnotation;
        }

        public String getHiddenByTable() {
            return hiddenByTable;
        }

        public void setHiddenByTable(final String hiddenByTable) {
            this.hiddenByTable = hiddenByTable;
        }
    }

    @lombok.Data
    static class NestedLexicalColumns {
        private LexicalColumnNames child;
    }

    @Table(name = "test_user", alias = "tu", columnFields = { "id", "name" }, nonColumnFields = { "tempData" })
    static class TestUser {
        @Id
        private Long id;

        @Column("user_name")
        private String name;

        private String email;

        @NonColumn
        private String nonColumnField;

        @Column
        private String tempData;

        private TestAddress address;

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNonColumnField() {
            return nonColumnField;
        }

        public void setNonColumnField(String notColumnField) {
            this.nonColumnField = notColumnField;
        }

        public String getTempData() {
            return tempData;
        }

        public void setTempData(String tempData) {
            this.tempData = tempData;
        }

        public TestAddress getAddress() {
            return address;
        }

        public void setAddress(TestAddress address) {
            this.address = address;
        }
    }

    static class TestAddress {
        private String street;
        private String city;

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    static class NestedLeaf {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    static class NestedBranch {
        private NestedLeaf firstLeaf;
        private NestedLeaf secondLeaf;

        public NestedLeaf getFirstLeaf() {
            return firstLeaf;
        }

        public void setFirstLeaf(NestedLeaf firstLeaf) {
            this.firstLeaf = firstLeaf;
        }

        public NestedLeaf getSecondLeaf() {
            return secondLeaf;
        }

        public void setSecondLeaf(NestedLeaf secondLeaf) {
            this.secondLeaf = secondLeaf;
        }
    }

    static class NestedRoot {
        private NestedBranch branch;

        public NestedBranch getBranch() {
            return branch;
        }

        public void setBranch(NestedBranch branch) {
            this.branch = branch;
        }
    }

    static class DeepNestedRoot {
        private DeepNestedMiddle middle;

        public DeepNestedMiddle getMiddle() {
            return middle;
        }

        public void setMiddle(DeepNestedMiddle middle) {
            this.middle = middle;
        }
    }

    static class DeepNestedMiddle {
        private DeepNestedLeaf leaf;

        public DeepNestedLeaf getLeaf() {
            return leaf;
        }

        public void setLeaf(DeepNestedLeaf leaf) {
            this.leaf = leaf;
        }
    }

    static class DeepNestedLeaf {
        private DeepNestedTail tail;

        public DeepNestedTail getTail() {
            return tail;
        }

        public void setTail(DeepNestedTail tail) {
            this.tail = tail;
        }
    }

    static class DeepNestedTail {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    static class SimpleEntity {
        @Id
        private Integer id;
        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class ConcurrentCacheEntity {
        private long id;
        private String value;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @Table(name = "aliasless_table")
    static class AliaslessEntity {
        private String simpleValue;

        public String getSimpleValue() {
            return simpleValue;
        }

        public void setSimpleValue(String simpleValue) {
            this.simpleValue = simpleValue;
        }
    }

    static class NonColumnOnlyEntity {
        @NonColumn
        private String hiddenValue;

        public String getHiddenValue() {
            return hiddenValue;
        }

        public void setHiddenValue(String hiddenValue) {
            this.hiddenValue = hiddenValue;
        }
    }

    static class NoIdEntity {
        private String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    static class SelfReferentialEntity {
        @Id
        private Long id;
        private SelfReferentialEntity parent;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public SelfReferentialEntity getParent() {
            return parent;
        }

        public void setParent(SelfReferentialEntity parent) {
            this.parent = parent;
        }
    }

    static class ParentWithFilteredChild {
        private long id;

        private FilteredChild child;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public FilteredChild getChild() {
            return child;
        }

        public void setChild(final FilteredChild child) {
            this.child = child;
        }
    }

    @Table(nonColumnFields = { "hiddenByTable" })
    static class FilteredChild {
        private String visible;

        @NonColumn
        private String hiddenByAnnotation;

        private String hiddenByTable;

        public String getVisible() {
            return visible;
        }

        public void setVisible(final String visible) {
            this.visible = visible;
        }

        public String getHiddenByAnnotation() {
            return hiddenByAnnotation;
        }

        public void setHiddenByAnnotation(final String hiddenByAnnotation) {
            this.hiddenByAnnotation = hiddenByAnnotation;
        }

        public String getHiddenByTable() {
            return hiddenByTable;
        }

        public void setHiddenByTable(final String hiddenByTable) {
            this.hiddenByTable = hiddenByTable;
        }
    }

    public static class ColumnBeanGeo {
        private double lat;

        public double getLat() {
            return lat;
        }

        public void setLat(final double lat) {
            this.lat = lat;
        }
    }

    @Table(name = "address", alias = "ad")
    public static class ColumnBeanAddress {
        private long id;
        private String street;
        @Column
        private ColumnBeanGeo geo;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(final String street) {
            this.street = street;
        }

        public ColumnBeanGeo getGeo() {
            return geo;
        }

        public void setGeo(final ColumnBeanGeo geo) {
            this.geo = geo;
        }
    }

    @Table(name = "person", alias = "p")
    public static class ColumnBeanPerson {
        private long id;
        private ColumnBeanAddress addr;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public ColumnBeanAddress getAddr() {
            return addr;
        }

        public void setAddr(final ColumnBeanAddress addr) {
            this.addr = addr;
        }
    }

    private static String renderRaw(final String expr, final boolean backslashEscapesOnly, final int[] verbatimFrom) {
        final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer();
        final StringBuilder sb = new StringBuilder();
        // Upper-case words stand in for the keyword exemption a real converter applies.
        final java.util.function.UnaryOperator<String> snake = word -> word.equals(word.toUpperCase()) ? word
                : QueryUtil.convertIdentifier(word, NamingPolicy.SNAKE_CASE);

        verbatimFrom[0] = QueryUtil.appendRawExpression(sb, expr, tokenizer.tokenize(expr), tokenizer, backslashEscapesOnly, snake, snake);

        return sb.toString();
    }
}
