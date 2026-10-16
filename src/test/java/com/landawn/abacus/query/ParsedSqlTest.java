package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.pool.KeyedObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.pool.PoolableAdapter;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Strings;

public class ParsedSqlTest extends TestBase {

    @Nested
    class Caching {
        @Test
        void testParameterizedSql_cacheReuse() {
            String sql = "SELECT * FROM users WHERE id = :userId";
            ParsedSql parsed = ParsedSql.parse(sql);

            String parameterizedSql1 = parsed.parameterizedSql();
            String parameterizedSql2 = parsed.parameterizedSql();

            assertEquals("SELECT * FROM users WHERE id = ?", parameterizedSql1);
            assertEquals("SELECT * FROM users WHERE id = ?", parameterizedSql2);
            assertEquals(List.of("userId"), parsed.namedParameters());
            assertEquals(1, parsed.parameterCount());
        }

        // Regression: a cache closed concurrently (isClosed() still false while get/put throw IllegalStateException) must be a miss, not an error.
        @Test
        void testParse_cacheClosedConcurrentlyIsTreatedAsMiss() {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> closing = (KeyedObjectPool<String, PoolableAdapter<ParsedSql>>) java.lang.reflect.Proxy
                    .newProxyInstance(KeyedObjectPool.class.getClassLoader(), new Class<?>[] { KeyedObjectPool.class }, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "isClosed":
                                return false;
                            case "get":
                            case "put":
                                throw new IllegalStateException("closed");
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });

            final ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE id = :id", closing);
            assertEquals("SELECT * FROM t WHERE id = ?", parsed.parameterizedSql());
            assertEquals(List.of("id"), parsed.namedParameters());
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE a = ? AND b = :b", closing));
        }

        @Test
        void testParse_CachedInstance_ReturnsCorrectParameterCount() {
            // First parse populates the cache; second parse hits the unsynchronized fast path.
            // Both observations of parameterCount must agree (regression guard for unsafe publication).
            String sql = "SELECT * FROM users WHERE a = :a AND b = :b AND c = :c";
            ParsedSql first = ParsedSql.parse(sql);
            ParsedSql second = ParsedSql.parse(sql);
            assertSame(first, second, "ParsedSql.parse should return the cached instance");
            assertEquals(3, first.parameterCount());
            assertEquals(3, second.parameterCount());
        }

        // Characterisation: cache hits are served from a lock-free mirror in front of the pool; every thread must still see the one pooled instance.
        @Test
        void testParse_cachedInstanceIsSharedAcrossThreads() throws Exception {
            final String[] sqls = new String[8];
            final ParsedSql[] expected = new ParsedSql[sqls.length];

            for (int i = 0; i < sqls.length; i++) {
                sqls[i] = "SELECT * FROM shared_cache_t WHERE id = :id AND k = " + i + " AND x IN (:xs)";
                expected[i] = ParsedSql.parse(sqls[i]);
            }

            final int threads = 8;
            final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
            final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            final List<java.util.concurrent.Future<Boolean>> results = new ArrayList<>();

            try {
                for (int t = 0; t < threads; t++) {
                    final int offset = t;
                    results.add(executor.submit(() -> {
                        start.await();

                        for (int i = 0; i < 20_000; i++) {
                            final int k = (i + offset) % sqls.length;

                            if (ParsedSql.parse(sqls[k]) != expected[k]) {
                                return false;
                            }
                        }

                        return true;
                    }));
                }

                start.countDown();

                for (final java.util.concurrent.Future<Boolean> result : results) {
                    assertTrue(result.get(60, java.util.concurrent.TimeUnit.SECONDS), "every thread must get the cached instance");
                }
            } finally {
                executor.shutdownNow();
            }

            assertEquals(List.of("id", "xs"), expected[0].namedParameters());
            assertEquals("SELECT * FROM shared_cache_t WHERE id = ? AND k = 0 AND x IN (?)", expected[0].parameterizedSql());
        }

        // Once the cache is closed (JVM shutdown), its front keeps serving the entries already mirrored, while new SQL is parsed uncached and not mirrored.
        @Test
        void testParse_cacheFrontAfterCacheClosedServesExistingEntriesOnly() {
            final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> cache = PoolFactory.createKeyedObjectPool(16, 60_000);
            final java.util.concurrent.ConcurrentMap<String, ParsedSql> front = new java.util.concurrent.ConcurrentHashMap<>();
            final String cachedSql = "SELECT * FROM front_closed_t WHERE id = :id";
            final ParsedSql cached;

            try {
                cached = ParsedSql.parse(cachedSql, cache, front);
                assertSame(cached, front.get(cachedSql));
            } finally {
                cache.close();
            }

            assertTrue(cache.isClosed());
            assertSame(cached, ParsedSql.parse(cachedSql, cache, front));

            final String newSql = "SELECT * FROM front_closed_t WHERE name = :name";
            final ParsedSql first = ParsedSql.parse(newSql, cache, front);
            assertEquals(List.of("name"), first.namedParameters());
            assertNotSame(first, ParsedSql.parse(newSql, cache, front));
            assertEquals(first, ParsedSql.parse(newSql, cache, front));
            assertNull(front.get(newSql));
            assertEquals(1, front.size());
        }

        // The lock-free mirror only holds instances the pool admitted and is bounded by the pool's capacity (cleared wholesale when full).
        @Test
        void testParse_cacheFrontNeverExceedsPoolCapacity() throws Exception {
            final int capacity = 32;
            final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> cache = PoolFactory.createKeyedObjectPool(capacity, 60_000);
            final java.util.concurrent.ConcurrentMap<String, ParsedSql> front = new java.util.concurrent.ConcurrentHashMap<>();

            try {
                assertEquals(capacity, cache.capacity());

                // Mirrored after a successful pool insertion, and again after a pool hit once the mirror was dropped.
                final String sql = "SELECT * FROM front_t WHERE id = :id";
                final ParsedSql first = ParsedSql.parse(sql, cache, front);
                assertSame(first, front.get(sql));
                assertSame(first, ParsedSql.parse(sql, cache, front));
                front.clear();
                assertSame(first, ParsedSql.parse(sql, cache, front));
                assertSame(first, front.get(sql));

                // Oversized SQL is neither pooled nor mirrored.
                final String oversized = "SELECT * FROM front_t WHERE name = '" + Strings.repeat('x', ParsedSql.MAX_CACHEABLE_SQL_LENGTH) + "'";
                assertNotSame(ParsedSql.parse(oversized, cache, front), ParsedSql.parse(oversized, cache, front));
                assertNull(front.get(oversized));
                assertEquals(1, cache.size());

                int maxSize = 0;

                for (int i = 0; i < capacity + 500; i++) {
                    final String distinct = "SELECT * FROM front_t WHERE id = ? AND k = " + i;
                    final ParsedSql parsed = ParsedSql.parse(distinct, cache, front);
                    assertSame(parsed, front.get(distinct));
                    assertSame(parsed, ParsedSql.parse(distinct, cache, front));
                    maxSize = Math.max(maxSize, front.size());
                    assertTrue(front.size() <= capacity, "front size " + front.size() + " after statement " + i);
                    assertTrue(cache.size() <= capacity, "pool size " + cache.size() + " after statement " + i);
                }

                assertEquals(capacity, maxSize, "the mirror fills up to the pool capacity before being dropped");
            } finally {
                cache.close();
            }

            // The public entry point's mirror of the shared pool obeys the same bound.
            final java.lang.reflect.Field frontField = ParsedSql.class.getDeclaredField("poolFront");
            frontField.setAccessible(true);
            final java.util.Map<?, ?> poolFront = (java.util.Map<?, ?>) frontField.get(null);
            final java.lang.reflect.Field poolField = ParsedSql.class.getDeclaredField("pool");
            poolField.setAccessible(true);
            final KeyedObjectPool<?, ?> pool = (KeyedObjectPool<?, ?>) poolField.get(null);
            assertNotNull(pool);

            for (int i = 0; i < pool.capacity() + 500; i++) {
                ParsedSql.parse("SELECT * FROM front_bound_t WHERE id = ? AND k = " + i);
                assertTrue(poolFront.size() <= pool.capacity(), "shared front size " + poolFront.size() + " after statement " + i);
            }

            final String fresh = "SELECT * FROM front_bound_t WHERE id = ? AND fresh = 1";
            final ParsedSql cached = ParsedSql.parse(fresh);
            assertSame(cached, poolFront.get(fresh));
            assertSame(cached, ParsedSql.parse(fresh));
        }

        // The front is keyed by the pool's own key instance (the entry's originalSql()) rather than retaining an equal copy passed by the caller.
        @Test
        void testParse_cacheFrontReusesPooledKeyInstance() {
            final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> cache = PoolFactory.createKeyedObjectPool(16, 60_000);
            final java.util.concurrent.ConcurrentMap<String, ParsedSql> front = new java.util.concurrent.ConcurrentHashMap<>();

            try {
                final String sql = "SELECT * FROM front_key_t WHERE id = :id";
                final String inserted = new String(sql.toCharArray());
                final ParsedSql first = ParsedSql.parse(inserted, cache, front);
                assertSame(inserted, first.originalSql());
                assertSame(inserted, front.keySet().iterator().next());

                // Refilled from a pool hit with a fresh, equal String: keyed by the pooled instance, not by the lookup copy.
                front.clear();
                final String lookup = new String(sql.toCharArray());
                assertSame(first, ParsedSql.parse(lookup, cache, front));
                assertEquals(1, front.size());
                final String key = front.keySet().iterator().next();
                assertSame(first.originalSql(), key);
                assertNotSame(lookup, key);
                assertSame(first, ParsedSql.parse(new String(sql.toCharArray()), cache, front));

                // A padded variant's originalSql() is trimmed, so its entry keeps the padded String it is looked up by.
                final String padded = "  " + sql + " ";
                final ParsedSql paddedParsed = ParsedSql.parse(padded, cache, front);
                front.clear();
                final String paddedLookup = new String(padded.toCharArray());
                assertSame(paddedParsed, ParsedSql.parse(paddedLookup, cache, front));
                assertSame(paddedLookup, front.keySet().iterator().next());
                assertSame(paddedParsed, ParsedSql.parse(new String(padded.toCharArray()), cache, front));
            } finally {
                cache.close();
            }
        }

        // A put the pool rejects (full, auto-balancing off, so put returns false) leaves the statement uncached, and therefore unmirrored.
        @Test
        void testParse_cacheFrontSkipsStatementsThePoolRejects() {
            final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> cache = PoolFactory.createKeyedObjectPool(1, 60_000, null, false, 0.2f);
            final java.util.concurrent.ConcurrentMap<String, ParsedSql> front = new java.util.concurrent.ConcurrentHashMap<>();

            try {
                final String admittedSql = "SELECT * FROM front_reject_t WHERE id = :id";
                final ParsedSql admitted = ParsedSql.parse(admittedSql, cache, front);
                assertSame(admitted, front.get(admittedSql));
                assertEquals(1, cache.size());

                final String rejectedSql = "SELECT * FROM front_reject_t WHERE name = :name";
                final ParsedSql rejected = ParsedSql.parse(rejectedSql, cache, front);
                assertEquals(List.of("name"), rejected.namedParameters());
                assertNull(cache.get(rejectedSql));
                assertNull(front.get(rejectedSql));
                assertNotSame(rejected, ParsedSql.parse(rejectedSql, cache, front));
                assertEquals(rejected, ParsedSql.parse(rejectedSql, cache, front));
                assertNull(front.get(rejectedSql));
                assertEquals(1, front.size());
                assertSame(admitted, ParsedSql.parse(admittedSql, cache, front));
            } finally {
                cache.close();
            }
        }

        // The cache-length gate measures the SQL as given: surrounding whitespace counts, so no cached key exceeds the limit.
        @Test
        void testParse_cacheLengthGateCountsSurroundingWhitespace() {
            final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> cache = PoolFactory.createKeyedObjectPool(16, 60_000);

            try {
                final String prefix = "SELECT * FROM t WHERE id = :id AND name = '";
                final String suffix = "'";
                final int fill = ParsedSql.MAX_CACHEABLE_SQL_LENGTH - prefix.length() - suffix.length();
                final String atLimit = prefix + Strings.repeat('x', fill) + suffix;
                assertEquals(ParsedSql.MAX_CACHEABLE_SQL_LENGTH, atLimit.length());

                // Exactly at the limit: cached.
                final ParsedSql first = ParsedSql.parse(atLimit, cache);
                assertSame(first, ParsedSql.parse(atLimit, cache));
                assertEquals(List.of("id"), first.namedParameters());
                assertEquals(1, cache.size());

                // One whitespace character over the limit: parsed fresh each time, never stored.
                for (final String padded : List.of(atLimit + " ", " " + atLimit, "\n" + atLimit)) {
                    assertEquals(ParsedSql.MAX_CACHEABLE_SQL_LENGTH + 1, padded.length());
                    final ParsedSql parsed = ParsedSql.parse(padded, cache);
                    assertNotSame(parsed, ParsedSql.parse(padded, cache));
                    assertEquals(atLimit, parsed.originalSql());
                    assertNull(cache.get(padded));
                }

                assertEquals(1, cache.size());

                // Below the limit, whitespace variants are separate (equal-valued) entries.
                final String shortSql = "SELECT 1 FROM t WHERE id = :id";
                final ParsedSql plain = ParsedSql.parse(shortSql, cache);
                final ParsedSql padded = ParsedSql.parse(shortSql + " ", cache);
                assertNotSame(plain, padded);
                assertEquals(plain, padded);
                assertSame(padded, ParsedSql.parse(shortSql + " ", cache));
                assertEquals(3, cache.size());
            } finally {
                cache.close();
            }
        }

        // Regression: when the first use of ParsedSql happened inside a JVM shutdown hook, creating the cache threw and class init failed for good.
        @Test
        void testParse_cacheThatCouldNotBeCreatedIsPermanentMiss() {
            // What creating a pool throws once JVM shutdown has begun: the hook registration refused while the pool classes
            // initialize (wrapped by the JVM), their failed initialization later on, or the terminated shared evictor executor.
            for (final Throwable failure : List.of(new IllegalStateException("Shutdown in progress"),
                    new ExceptionInInitializerError(new IllegalStateException("Shutdown in progress")), new NoClassDefFoundError("Could not initialize class"),
                    new java.util.concurrent.RejectedExecutionException("rejected from Terminated executor"), new OutOfMemoryError("test"))) {
                assertNull(ParsedSql.createCache(() -> {
                    if (failure instanceof RuntimeException) {
                        throw (RuntimeException) failure;
                    }

                    throw (Error) failure;
                }), failure.toString());
            }

            final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> created = PoolFactory.createKeyedObjectPool(16, 60_000);
            try {
                assertSame(created, ParsedSql.createCache(() -> created));
            } finally {
                created.close();
            }

            // Without a cache every parse is fresh and still correct.
            final String sql = "SELECT * FROM t WHERE id = :id";
            final ParsedSql first = ParsedSql.parse(sql, null);
            assertEquals("SELECT * FROM t WHERE id = ?", first.parameterizedSql());
            assertEquals(List.of("id"), first.namedParameters());
            assertNotSame(first, ParsedSql.parse(sql, null));
            assertEquals(first, ParsedSql.parse(sql, null));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE a = ? AND b = :b", null));
        }

        @Test
        void testParse_Caching() {
            String sql = "SELECT * FROM users WHERE id = :userId";
            ParsedSql parsed1 = ParsedSql.parse(sql);
            ParsedSql parsed2 = ParsedSql.parse(sql);
            assertSame(parsed1, parsed2);
        }

        // Regression: the parse cache pool closes itself in a JVM shutdown hook, after which every parse threw IllegalStateException.
        @Test
        void testParse_closedCacheIsTreatedAsMiss() {
            final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> cache = PoolFactory.createKeyedObjectPool(16, 60_000);
            final String sql = "SELECT * FROM t WHERE id = :id";

            try {
                final ParsedSql cached = ParsedSql.parse(sql, cache);
                assertSame(cached, ParsedSql.parse(sql, cache));
            } finally {
                cache.close();
            }

            // Both previously cached and new SQL still parse, uncached.
            final ParsedSql afterClose = ParsedSql.parse(sql, cache);
            assertEquals("SELECT * FROM t WHERE id = ?", afterClose.parameterizedSql());
            assertEquals(List.of("id"), afterClose.namedParameters());
            assertEquals("SELECT * FROM u WHERE a = ?", ParsedSql.parse("SELECT * FROM u WHERE a = #{a}", cache).parameterizedSql());

            // Parse errors still propagate.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE a = ? AND b = :b", cache));
        }

        // SQL longer than MAX_CACHEABLE_SQL_LENGTH (as given, surrounding whitespace included) bypasses the cache entirely: neither looked up nor stored.
        @Test
        void testParse_sqlLongerThanMaxCacheableLengthBypassesCache() {
            assertEquals(4096, ParsedSql.MAX_CACHEABLE_SQL_LENGTH);

            final KeyedObjectPool<String, PoolableAdapter<ParsedSql>> cache = PoolFactory.createKeyedObjectPool(16, 60_000);

            try {
                final String prefix = "SELECT * FROM t WHERE id = :id AND name = '";
                final String suffix = "'";
                final int fill = ParsedSql.MAX_CACHEABLE_SQL_LENGTH - prefix.length() - suffix.length();
                final String atLimit = prefix + Strings.repeat('x', fill) + suffix;
                final String overLimit = prefix + Strings.repeat('x', fill + 1) + suffix;
                assertEquals(ParsedSql.MAX_CACHEABLE_SQL_LENGTH, atLimit.length());
                assertEquals(ParsedSql.MAX_CACHEABLE_SQL_LENGTH + 1, overLimit.length());

                // Exactly at the limit: still cached.
                final ParsedSql cachedAtLimit = ParsedSql.parse(atLimit, cache);
                assertSame(cachedAtLimit, ParsedSql.parse(atLimit, cache));
                assertEquals(1, cache.size());

                // One over the limit: parsed correctly every time, never stored, never looked up.
                final ParsedSql first = ParsedSql.parse(overLimit, cache);
                final ParsedSql second = ParsedSql.parse(overLimit, cache);
                assertNotSame(first, second);
                assertEquals(first, second);
                assertEquals(List.of("id"), first.namedParameters());
                assertEquals(prefix.replace(":id", "?") + Strings.repeat('x', fill + 1) + suffix, first.parameterizedSql());
                assertEquals(1, cache.size());
                assertNull(cache.get(overLimit));

                // Surrounding whitespace counts toward the limit, so the padded variant is over it and parsed fresh rather than the cached instance...
                assertNotSame(ParsedSql.parse(atLimit), ParsedSql.parse("   " + atLimit + "   "));
                // ...and oversized SQL through the public entry point is parsed fresh each call as well.
                assertNotSame(ParsedSql.parse(overLimit), ParsedSql.parse(overLimit));

                // Parse errors still propagate for oversized SQL.
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(overLimit + " AND x = ?", cache));
            } finally {
                cache.close();
            }
        }

        @Test
        void testParse_WhitespaceVariantsUseSeparateCacheEntries() {
            ParsedSql canonical = ParsedSql.parse("SELECT * FROM canonical_cache_test");
            ParsedSql padded = ParsedSql.parse("  SELECT * FROM canonical_cache_test  ");

            assertNotSame(canonical, padded);
            assertEquals("SELECT * FROM canonical_cache_test", canonical.originalSql());
            assertEquals(canonical.originalSql(), padded.originalSql());
            assertEquals("SELECT * FROM canonical_cache_test", canonical.parameterizedSql());
            assertEquals(canonical.parameterizedSql(), padded.parameterizedSql());
            assertSame(canonical, ParsedSql.parse("SELECT * FROM canonical_cache_test"));
            assertSame(padded, ParsedSql.parse("  SELECT * FROM canonical_cache_test  "));
        }
    }

    @Nested
    class GeometricOperators {
        @Test
        void testIncompleteGeometricOperandsKeepQuestionMarksAsBindings() {
            for (final String operand : List.of("CAST(? AS)", "CAST(? AS text)", "(?", "?::", "?::pg_catalog", "?::pg_catalog.", "?::pg_catalog other",
                    "?::\"unfinished", "?::1bad", "?::text.", "?::timestamp WITH wrong", "?::timestamp WITH TIME wrong", "?::text [x]", "?::text [3]tail",
                    "line", "line + ?", "U & ?", "N ?", "foo(?)", "foo(?", "() + ?")) {
                final String sql = "SELECT ?- " + operand;
                final int[] expected = java.util.stream.IntStream.range(0, sql.length()).filter(index -> sql.charAt(index) == '?').toArray();
                assertOffsets(sql, expected);
            }
        }

        @Test
        void testParse_CastChainsSkipQuotedColonsAndArraySuffixes() {
            for (final String operand : List.of("?::\"odd::type\" ::line", "?::\"odd\"\"::type\" ::line", "?::text[]::text ::line", "?::text [] ::text ::line",
                    "?::text[ ][]::text ::line", "?::varchar(30)[] ::line", "?::line [] ::line", "?::text[3] ::line", "?::text [ 30 ][] ::line",
                    "?::text ARRAY::text ::line", "?::text ARRAY[3]::text ::line", "?::text array [3] ::text ::line")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = grouped ? "SELECT ARRAY[?- " + operand + "]" : "SELECT ?- " + operand;
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(1, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
                }
            }

            for (final String operand : List.of("?::\"odd::line\"", "?::line[]", "?::line []", "?::line[ ][]", "?::line [3]", "?::line ARRAY",
                    "?::line ARRAY[3]", "?::line ARRAY [3]", "?::text[3] + other::line", "?::text[x] ::line", "?::text ARRAY[3] + other::line",
                    "?::text ARRAY[x] ::line")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = grouped ? "SELECT ARRAY[?- " + operand + "]" : "SELECT ?- " + operand;
                    assertEquals(2, ParsedSql.parse(sql).parameterCount(), sql);
                }
            }
        }

        @Test
        void testParse_ChainedGeometricCastsUseTheFinalType() {
            for (final String operand : List.of("CAST(? AS text)::line", "?::text ::line", "? :: text :: line", "CAST(? AS text) :: pg_catalog . \"line\"",
                    "?::text /* cast */ ::lseg", "?::varchar(30)::line", "?::character varying::line", "?::character varying(30) ::line",
                    "?::double precision::text ::line", "?::timestamp(3) without time zone::text ::line")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = grouped ? "SELECT ARRAY[?- " + operand + "]" : "SELECT ?- " + operand;
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(1, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
                }
            }

            for (final String operand : List.of("?::line ::text", "CAST(? AS line)::text", "?::text + other::line", "?::line ::varchar(30)",
                    "?::line ::character varying", "?::varchar(30) + other::line")) {
                final String sql = "SELECT ?- " + operand;
                assertEquals(2, ParsedSql.parse(sql).parameterCount(), sql);
            }
        }

        @Test
        void testParse_DeeplyParenthesizedGeometricOperandsPreserveOnlyRealBindings() {
            // Explicit type recognition unwraps parentheses iteratively. Deep but finite input must not
            // overflow the Java stack or count the unary operator as an extra parameter in either lexer.
            final String opening = "(".repeat(4_096);
            final String closing = ")".repeat(4_096);
            for (final String binding : List.of("?", ":geometry", "#{ geometry, jdbcType=OTHER }")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = "SELECT " + (grouped ? "ARRAY[" : "") + "?- " + opening + "CAST(" + binding + " AS line)" + closing
                            + (grouped ? "]" : "");
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    final boolean positional = binding.equals("?");

                    assertEquals(1, parsed.parameterCount());
                    assertEquals(positional ? List.of() : List.of("geometry"), parsed.namedParameters());
                    assertEquals(positional ? sql : sql.replace(binding, "?"), parsed.parameterizedSql());
                    assertArrayEquals(positional ? new int[] { sql.indexOf("CAST(") + 5 } : new int[0], parsed.positionalParameterOffsets());
                }
            }
        }

        @Test
        void testParse_GeometricCastsIgnoreCommentsAndKeepTypeNamesExact() {
            for (final String cast : List.of("?:: /* type */ \"line\"", "? :: -- type\n lseg", "CAST(? AS /* schema */ pg_catalog /* dot */ . \"line\")",
                    "CAST(? AS \"pg_catalog\". /* type */ lseg)", "(? + 0):: /* type */ line", "COALESCE(?, NULL):: \"lseg\"")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = grouped ? "SELECT ARRAY[?- " + cast + "]" : "SELECT ?- " + cast;
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(1, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.indexOf('?', sql.indexOf('?') + 1) }, parsed.positionalParameterOffsets(), sql);
                }
            }

            // Quoted identifiers are case-sensitive, and an unrelated schema/type is not evidence of geometry.
            for (final String type : List.of("\"LINE\"", "\"LSEG\"", "\"PG_CATALOG\".line", "other.line", "line_extra", "\"pg_catalog.line\"", "integer")) {
                for (final String cast : List.of("?:: " + type, "CAST(? AS " + type + ")")) {
                    final String sql = "SELECT ?- " + cast;
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(2, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
                }
            }

            // A quoted type suffix must not hide the binding before it, or expose markers inside the name.
            for (final String binding : List.of(":geometry", "#{geometry}")) {
                final String sql = "SELECT " + binding + "::\"type:literal#{literal}\"";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertEquals(List.of("geometry"), parsed.namedParameters(), sql);
                assertEquals("SELECT ?::\"type:literal#{literal}\"", parsed.parameterizedSql(), sql);
            }

            for (final String sql : List.of("SELECT ':geometry::\"line\"'", "SELECT '#{geometry}::\"line\"'", "SELECT \"column::line:geometry\"",
                    "SELECT [column::line:geometry]")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(0, parsed.parameterCount(), sql);
                assertEquals(sql, parsed.parameterizedSql(), sql);
            }
        }

        @Test
        void testParse_GeometricCastsRespectTokenBoundariesAndQuotedTypes() {
            // A cast may span tokens in either lexer. Only the operand binds; the leading ?-/ ?| is unary.
            for (final String type : List.of("line", "lseg")) {
                final String literal = type.equals("line") ? "'{1,0,0}'" : "'(0,0),(1,0)'";
                for (final String typeName : List.of(type, type.toUpperCase(java.util.Locale.ROOT), "\"" + type + "\"", "pg_catalog." + type,
                        "\"pg_catalog\".\"" + type + "\"", "\"pg_catalog\" . \"" + type + "\"", "pg_catalog . " + type, "pg_catalog.\"" + type + "\"",
                        "\"pg_catalog\"." + type)) {
                    for (final String operand : List.of("?", ":geometry", "#{geometry}", "#{ geometry, jdbcType=OTHER }", literal)) {
                        for (final String cast : List.of(operand + "::" + typeName, operand + ":: " + typeName, operand + " :: " + typeName,
                                "CAST(" + operand + " AS " + typeName + ")")) {
                            for (final String operator : List.of("?-", "?|")) {
                                for (final boolean grouped : new boolean[] { false, true }) {
                                    final String sql = grouped ? "SELECT ARRAY[" + operator + " " + cast + "]" : "SELECT " + operator + " " + cast;
                                    final ParsedSql parsed = ParsedSql.parse(sql);
                                    final boolean named = operand.startsWith(":") || operand.startsWith("#{");

                                    assertEquals(operand.equals(literal) ? 0 : 1, parsed.parameterCount(), sql);
                                    assertEquals(named ? List.of("geometry") : List.of(), parsed.namedParameters(), sql);
                                    assertEquals(named ? sql.replace(operand, "?") : sql, parsed.parameterizedSql(), sql);
                                    assertArrayEquals(operand.equals("?") ? new int[] { sql.indexOf('?', sql.indexOf('?') + 1) } : new int[0],
                                            parsed.positionalParameterOffsets(), sql);
                                }
                            }
                        }
                    }
                }
            }
        }

        @Test
        void testParse_GeometricLiteralsAndConstructorsHonorFinalCasts() {
            for (final String operand : List.of("line '(0,0),(1,0)'::text", "pg_catalog.line'(0,0),(1,0)' ::text", "line E'(0,0),(1,0)'::text",
                    "line N'(0,0),(1,0)'::text", "line U&'(0,0),(1,0)'::text", "line(point(0,0), point(1,0))::text",
                    "pg_catalog.lseg(point(0,0), point(1,0)) /* gap */ ::text")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = grouped ? "SELECT ARRAY[?- " + operand + "]" : "SELECT ?- " + operand;
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(1, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + ", :other"), sql);
                }
            }
        }

        // Regression: unary geometric operators on prefixed, cast and parenthesized literals inside brackets.
        @Test
        void testParse_geometricOperandVariantsInsideBrackets() {
            assertEquals(0, ParsedSql.parse("SELECT ARRAY[?| E'(0,0),(1,0)'::lseg]").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT ARRAY[?- E'(0,0),(1,0)'::line::text]").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT ARRAY[?- (E'(0,0),(1,0)'::line)]").parameterCount());
        }

        // Regression: inside brackets a unary ?-/?| on a prefixed literal with a cast (E'...'::line) was counted, unlike outside brackets.
        @Test
        void testParse_prefixedLiteralCastIsGeometricOperandInsideBrackets() {
            for (final String prefix : List.of("E", "N", "U&")) {
                final String literal = prefix + "'(0,0),(1,0)'::line";
                assertEquals(0, ParsedSql.parse("SELECT ARRAY[?- " + literal + "]").parameterCount(), literal);
                assertEquals(0, ParsedSql.parse("SELECT ?- " + literal).parameterCount(), literal);
            }

            // Without the cast the operand is untyped, and the '?' stays a placeholder.
            assertEquals(1, ParsedSql.parse("SELECT ARRAY[?- E'(0,0),(1,0)']").parameterCount());
        }

        @Test
        void testParse_QualifiedAdjacentGeometricLiteralsMatchSpacedLiterals() {
            for (final String type : List.of("pg_catalog.line", "pg_catalog.lseg", "PG_CATALOG.LINE", "\"pg_catalog\".\"line\"")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String expression = "?-" + type + "'(0,0),(1,0)'";
                    final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
                    assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
                    assertArrayEquals(new int[0], ParsedSql.parse(sql).positionalParameterOffsets(), sql);
                    assertEquals(List.of("other"), ParsedSql.parse(sql + ", :other").namedParameters(), sql);
                }
            }

            for (final String type : List.of("other.line", "\"PG_CATALOG\".line", "pg_catalog.line_extra")) {
                assertEquals(1, ParsedSql.parse("SELECT ?-" + type + "'(0,0),(1,0)'").parameterCount(), type);
            }
        }

        @Test
        void testParse_QuotedColonsInEscapeLiteralsAreNotCasts() {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String expression = "?- line E'escaped\\'::text'";
                final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
                assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
                assertEquals(List.of("other"), ParsedSql.parse(sql + ", :other").namedParameters(), sql);
            }
        }

        @Test
        void testParse_RepeatedGeometricCastsKeepEveryOperandBindingOffset() {
            // Repeated casts must share the parenthesis index. A suffix scan for every operator turns
            // a long OR expression quadratic; verify offsets too so skipping operands cannot pass the bound.
            final int operands = 4_000;
            for (final boolean grouped : new boolean[] { false, true }) {
                final StringBuilder sql = new StringBuilder(grouped ? "SELECT ARRAY[" : "SELECT ");
                final int[] expectedOffsets = new int[operands];
                for (int i = 0; i < operands; i++) {
                    if (i > 0) {
                        sql.append(" OR ");
                    }
                    sql.append("?- CAST(");
                    expectedOffsets[i] = sql.length();
                    sql.append("? AS line)");
                }
                if (grouped) {
                    sql.append(']');
                }
                final String statement = sql.toString();
                assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
                    final ParsedSql parsed = ParsedSql.parse(statement);
                    assertEquals(operands, parsed.parameterCount());
                    assertArrayEquals(expectedOffsets, parsed.positionalParameterOffsets());
                    assertTrue(parsed.namedParameters().isEmpty());
                });
            }
        }

        @Test
        void testParse_TypedUnaryGeometricOperatorsDoNotRequireBindings() {
            // Explicit geometry types disambiguate unary ?-/ ?| from a JDBC marker followed by arithmetic.
            // The equivalent bracket-free lseg spelling keeps cast fixtures independent of the tokenizer's
            // preexisting ambiguity when a bracket-group token contains a quoted closing bracket.
            for (final String expression : List.of("?- lseg '[(0,0),(1,0)]'", "?| line '{1,0,0}'", "?-lseg(point(0,0), point(1,0))",
                    "?|line(point(0,0), point(0,1))", "?- '(0,0),(1,0)'::lseg", "?| '{1,0,0}'::line", "?- CAST('(0,0),(1,0)' AS lseg)",
                    "?| CAST('{1,0,0}' AS line)", "?- /* geometry */ lseg '[(0,0),(1,0)]'", "?| -- geometry\n line '{1,0,0}'")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(0, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                    assertTrue(parsed.namedParameters().isEmpty(), sql);
                }
            }

            for (final String expression : List.of("?-1", "?|1", "?-2.5", "?-column_value", "?-line", "?|line")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(1, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
                }
            }
        }

        @Test
        void testSpacedTypeSuffixesStillResolveTheFinalGeometricCast() {
            for (final String operand : List.of("?::timestamp WITH TIME ZONE ::line", "?::time WITHOUT TIME ZONE ::lseg", "?::text [3]::line",
                    "line '(0,0),(1,0)'::text::line")) {
                final String sql = "SELECT ?- " + operand;
                assertOffsets(sql, operand.indexOf('?') >= 0 ? new int[] { sql.lastIndexOf('?') } : new int[0]);
            }
            assertOffsets("SELECT ?- line '(0,0),(1,0)'::text", 7);
        }
    }

    @Nested
    class JdbcCalls {
        // Regression: the return slot of a JDBC call escape ended at the first '}' of its MyBatis binding, even one inside a
        // quoted map key or label. "{#{map['}']} = call p(#{a})}" was then not read as a call and kept both bindings unparsed,
        // and a quoted '}' label passed the text through unparsed instead of being rejected like any other quoted label.
        @Test
        void testJdbcCallReturnSlotIgnoresQuotedClosingBrace() {
            for (final String key : List.of("map['}']", "map['x''y']", "map[\"x\"\"y\"]", "map[`x``y`]")) {
                final ParsedSql call = ParsedSql.parse("{#{" + key + "} = call p(#{a})}");
                assertTrue(call.isDataOperation(), key);
                assertEquals(List.of(key, "a"), call.namedParameters(), key);
                assertEquals("{? = call p(?)}", call.parameterizedSql(), key);
                assertEquals(2, call.parameterCount(), key);
            }

            assertEquals(List.of("r", "a"), ParsedSql.parse("{#{r /* } */} = call p(#{a})}").namedParameters());
            assertEquals(List.of("r", "a"), ParsedSql.parse("{#{r, mode=OUT}= call p(#{a})}").namedParameters());

            for (final String sql : List.of("{#{r, mode=OUT, label='}'} = call p(#{a})}", "{ #{r, label='}'} = call p(#{a})}",
                    "{#{r, mode=OUT, label='x'} = call p(#{a})}")) {
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
            }
        }

        @Test
        void testJdbcCallReturnSlotRejectsMalformedMybatisBindings() {
            for (final String prefix : List.of("{", "{ ", "{ /* return */ ")) {
                for (final String slot : List.of("#{map['x\\'y']}", "#{map[\"x\\\"y\"]}", "#{map[`x\\`y`]}", "#{ map['x\\'y']}", "#{ map['x\\'y'] }",
                        "#{r#{nested}}", "#{r #{nested}}")) {
                    final String sql = prefix + slot + " = call p(#{a})}";
                    final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
                    assertTrue(error.getMessage().contains("Malformed iBatis/MyBatis parameter"), sql);
                }
            }
        }

        @Test
        void testParse_Call() {
            ParsedSql parsed = ParsedSql.parse("CALL refresh_user(:userId, :mode)");
            assertEquals("CALL refresh_user(?, ?)", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals("userId", parsed.namedParameters().get(0));
            assertEquals("mode", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_callStatement() {
            String sql = "CALL refresh_user(:userId, :mode)";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals("CALL refresh_user(?, ?)", parsed.parameterizedSql());
            List<String> namedParams = parsed.namedParameters();
            assertEquals(2, namedParams.size());
            assertEquals("userId", namedParams.get(0));
            assertEquals("mode", namedParams.get(1));
        }

        @Test
        void testParse_JdbcCallEscape_IbatisNamedParameter() {
            ParsedSql parsed = ParsedSql.parse("{call proc(#{id})}");
            assertEquals("{call proc(?)}", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_JdbcCallEscape_NamedParameters() {
            ParsedSql parsed = ParsedSql.parse("{call refresh_user(:userId, :mode)}");
            assertEquals("{call refresh_user(?, ?)}", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(Arrays.asList("userId", "mode"), parsed.namedParameters());
        }

        @Test
        void testParse_JdbcCallEscape_ReturnParameterForm_PositionalOnly() {
            ParsedSql parsed = ParsedSql.parse("{? = call get_val(?)}");
            assertEquals("{? = call get_val(?)}", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        @Test
        void testParse_JdbcCallEscape_SpacedCall() {
            ParsedSql parsed = ParsedSql.parse("{ CALL proc(?) }");
            assertEquals("{ CALL proc(?) }", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        // Regression: JDBC call escapes with named or MyBatis return slots in their compact, split, commented and BOM-prefixed forms.
        @Test
        void testParse_jdbcCallEscapeReturnSlotVariants() {
            assertCall("{#{ r } = call f(#{p})}", "{? = call f(?)}", List.of("r", "p"));
            assertCall("{:r=call f(:p)}", "{?=call f(?)}", List.of("r", "p"));
            assertCall("{ #{r}=call f(#{p})}", "{ ?=call f(?)}", List.of("r", "p"));
            assertCall("{ :r =\n call f(:p)}", "{ ? = call f(?)}", List.of("r", "p"));
            assertCall("{:r.x = call f(:p)}", "{? = call f(?)}", List.of("r.x", "p"));
            assertCall("{ #{r} /* c */ = call f(#{p})}", "{ ? = call f(?)}", List.of("r", "p"));
            assertCall("﻿{:r = call f(:p)}", "﻿{? = call f(?)}", List.of("r", "p"));
            assertCall("﻿{call f(:p)}", "﻿{call f(?)}", List.of("p"));
            assertCall("(  {:r = call f(:p)})", "( {? = call f(?)})", List.of("r", "p"));

            ParsedSql parsed = ParsedSql.parse("{/*c*/ ? = call f(?)}");
            assertEquals(2, parsed.parameterCount());
            assertArrayEquals(new int[] { 7, 18 }, parsed.positionalParameterOffsets());

            parsed = ParsedSql.parse("﻿{call f(?)}");
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { 9 }, parsed.positionalParameterOffsets());

            // A slot that is not a single marker is not a JDBC call escape.
            for (final String sql : List.of("{#{r} x = call f(#{p})}", "{::r = call f(:p)}", "{:1 = call f(:p)}", "{#{r = call f(#{p})}", "{:r = call}",
                    "{#{map['x\\'y']} = other(#{p})}")) {
                parsed = ParsedSql.parse(sql);
                assertFalse(parsed.isDataOperation(), sql);
                assertEquals(sql, parsed.parameterizedSql(), sql);
                assertEquals(0, parsed.parameterCount(), sql);
            }

            // Mixed styles are rejected.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("{:r = call f(?)}"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("{#{result} = call f(:p)}"));
        }

        // Regression: a JDBC call escape whose return slot was a named or MyBatis marker was not recognized as CALL, so nothing was converted.
        @Test
        void testParse_jdbcCallEscapeWithNamedOrMyBatisReturnSlot() {
            ParsedSql parsed = ParsedSql.parse("{#{result, mode=OUT, jdbcType=INTEGER} = call my_func(#{param})}");
            assertEquals("{? = call my_func(?)}", parsed.parameterizedSql());
            assertEquals(List.of("result", "param"), parsed.namedParameters());
            assertEquals(2, parsed.parameterCount());
            assertTrue(parsed.isDataOperation());

            parsed = ParsedSql.parse("{ #{result, mode=OUT, jdbcType=INTEGER} = call my_func(#{param})}");
            assertEquals("{ ? = call my_func(?)}", parsed.parameterizedSql());
            assertEquals(List.of("result", "param"), parsed.namedParameters());

            parsed = ParsedSql.parse("{#{result} = CALL my_func(#{param})}");
            assertEquals("{? = CALL my_func(?)}", parsed.parameterizedSql());
            assertEquals(List.of("result", "param"), parsed.namedParameters());

            parsed = ParsedSql.parse("{:result = call my_func(:param)}");
            assertEquals("{? = call my_func(?)}", parsed.parameterizedSql());
            assertEquals(List.of("result", "param"), parsed.namedParameters());

            parsed = ParsedSql.parse("{ :result = call my_func(:param)}");
            assertEquals("{ ? = call my_func(?)}", parsed.parameterizedSql());
            assertEquals(List.of("result", "param"), parsed.namedParameters());

            // The positional forms are unchanged; a non-marker slot is not a JDBC call escape.
            assertEquals(2, ParsedSql.parse("{? = call my_func(?)}").parameterCount());
            assertEquals(2, ParsedSql.parse("{?=call my_func(?)}").parameterCount());
            assertEquals(1, ParsedSql.parse("{call my_func(?)}").parameterCount());
            assertFalse(ParsedSql.parse("{ x = call my_func(:p)}").isDataOperation());
            assertTrue(ParsedSql.parse("{ x = call my_func(:p)}").namedParameters().isEmpty());
        }
    }

    @Nested
    class MetadataAndEquality {
        @Test
        void testEquals() {
            String sql1 = "SELECT * FROM users WHERE id = :userId";
            String sql2 = "SELECT * FROM users WHERE id = :userId";
            String sql3 = "SELECT * FROM users WHERE id = :id";

            ParsedSql parsed1 = ParsedSql.parse(sql1);
            ParsedSql parsed2 = ParsedSql.parse(sql2);
            ParsedSql parsed3 = ParsedSql.parse(sql3);

            // Test equals with same object
            assertEquals(parsed1, parsed1);

            // Test equals with equivalent object
            assertEquals(parsed1, parsed2);

            // Test not equals with different SQL
            assertNotEquals(parsed1, parsed3);

            // Test not equals with null
            assertNotEquals(parsed1, null);

            // Test not equals with different type
            assertNotEquals(parsed1, "SELECT * FROM users WHERE id = :userId");
        }

        @Test
        void testEquals_DifferentClass() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
            assertFalse(parsed.equals("SELECT * FROM users"));
        }

        @Test
        void testEquals_DifferentObjects() {
            ParsedSql parsed1 = ParsedSql.parse("SELECT * FROM users");
            ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM orders");
            assertFalse(parsed1.equals(parsed2));
        }

        @Test
        void testEquals_EqualObjects() {
            ParsedSql parsed1 = ParsedSql.parse("SELECT * FROM users");
            ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
            assertTrue(parsed1.equals(parsed2));
        }

        @Test
        void testEquals_Null() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
            assertFalse(parsed.equals(null));
        }

        @Test
        void testEquals_SameObject() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
            assertTrue(parsed.equals(parsed));
        }

        @Test
        void testGetNamedParameters() {
            String sql = "SELECT * FROM users WHERE age > :minAge AND age < :maxAge";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> params = parsed.namedParameters();
            assertEquals(2, params.size());
            assertEquals("minAge", params.get(0));
            assertEquals("maxAge", params.get(1));
        }

        @Test
        void testGetNamedParameters_WithNamedParameter() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
            assertNotNull(parsed.namedParameters());
            assertEquals(1, parsed.namedParameters().size());
            assertEquals("userId", parsed.namedParameters().get(0));
        }

        @Test
        void testGetNamedParameters_withNamedParameters() {
            String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> namedParams = parsed.namedParameters();
            assertEquals(2, namedParams.size());
            assertEquals("userId", namedParams.get(0));
            assertEquals("status", namedParams.get(1));
        }

        @Test
        void testGetNamedParameters_withQuestionMarks() {
            String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> namedParams = parsed.namedParameters();
            assertTrue(namedParams.isEmpty());
        }

        @Test
        void testGetNamedParameters_withSingleNamedParameter() {
            String sql = "SELECT * FROM users WHERE id = :userId";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> namedParams = parsed.namedParameters();
            assertEquals(1, namedParams.size());
            assertEquals("userId", namedParams.get(0));
        }

        @Test
        void testGetParameterCount() {
            String sql = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals(3, parsed.parameterCount());
        }

        @Test
        void testGetParameterCount_withNamedParameters() {
            String sql = "INSERT INTO users (name, email) VALUES (:name, :email)";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals(2, parsed.parameterCount());
        }

        @Test
        void testGetParameterCount_withNamedParametersInSelect() {
            String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
            ParsedSql parsed = ParsedSql.parse(sql);

            int parameterCount = parsed.parameterCount();
            assertEquals(2, parameterCount);
        }

        @Test
        void testGetParameterCount_WithQuestionMarkParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ? AND name = ?");
            int count = parsed.parameterCount();
            assertEquals(2, count);
        }

        @Test
        void testGetParameterCount_withSingleNamedParameterInSelect() {
            String sql = "SELECT * FROM users WHERE id = :userId";
            ParsedSql parsed = ParsedSql.parse(sql);

            int parameterCount = parsed.parameterCount();
            assertEquals(1, parameterCount);
        }

        @Test
        void testGetParameterizedSql_WithNamedParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :userName");
            String sql = parsed.parameterizedSql();
            assertNotNull(sql);
            assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", sql);
        }

        @Test
        void testGetParameterizedSql_withNamedParameters() {
            String sql = "SELECT * FROM users WHERE id = :userId AND status = :status";
            ParsedSql parsed = ParsedSql.parse(sql);

            String parameterizedSql = parsed.parameterizedSql();
            assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterizedSql);
        }

        @Test
        void testGetParameterizedSql_WithQuestionMarkParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
            String sql = parsed.parameterizedSql();
            assertNotNull(sql);
            assertEquals("SELECT * FROM users WHERE id = ?", sql);
        }

        @Test
        void testGetParameterizedSql_withQuestionMarks() {
            String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
            ParsedSql parsed = ParsedSql.parse(sql);

            String parameterizedSql = parsed.parameterizedSql();
            assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterizedSql);
        }

        @Test
        void testGetParameterizedSql_WithSingleNamedParameter() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
            String sql = parsed.parameterizedSql();
            assertEquals("SELECT * FROM users WHERE id = ?", sql);
        }

        @Test
        void testHashCode() {
            ParsedSql parsed1 = ParsedSql.parse("SELECT * FROM users");
            ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
            assertEquals(parsed1.hashCode(), parsed2.hashCode());
        }

        @Test
        void testParsedSql_classLevelExample() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
            String parameterized = parsed.parameterizedSql();
            List<String> params = parsed.namedParameters();

            assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parameterized);
            assertEquals(Arrays.asList("userId", "status"), params);
        }

        @Test
        void testParsedSql_namedParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");
            ImmutableList<String> params = parsed.namedParameters();
            assertEquals(Arrays.asList("name", "minAge"), params);

            ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
            ImmutableList<String> params2 = parsed2.namedParameters();
            assertTrue(params2.isEmpty());
        }

        @Test
        void testParsedSql_parameterCount() {
            ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, email, age) VALUES (:name, :email, :age)");
            int count = parsed.parameterCount();
            assertEquals(3, count);

            ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
            int count2 = parsed2.parameterCount();
            assertEquals(0, count2);
        }

        @Test
        void testParsedSql_parameterizedSql() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
            String sql = parsed.parameterizedSql();
            assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", sql);
        }

        @Test
        void testParsedSql_parameterizedSqlForJdbc() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND name = :name");
            String jdbcSql = parsed.parameterizedSql();
            assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", jdbcSql);
        }

        @Test
        void testParsedSql_sql() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
            String original = parsed.originalSql();
            assertEquals("SELECT * FROM users WHERE id = :userId", original);
        }

        @Test
        void testSql() {
            String originalSql = "SELECT * FROM users WHERE id = :userId";
            ParsedSql parsed = ParsedSql.parse(originalSql);
            assertEquals(originalSql, parsed.originalSql());
        }

        @Test
        void testToString() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
            String str = parsed.toString();
            assertNotNull(str);
            assertTrue(str.contains("SELECT * FROM users WHERE id = :userId"));
            assertTrue(str.contains("SELECT * FROM users WHERE id = ?"));
        }
    }

    @Nested
    class MybatisParameters {
        @Test
        void testParse_AdjacentIbatisTokenAfterValid_doesNotInfiniteLoop() {
            // Make sure the empty / literal "#{}" doesn't cause an infinite loop after a valid one.
            ParsedSql parsed = ParsedSql.parse("SELECT #{a}#{} FROM dual");
            assertEquals("SELECT ?#{} FROM dual", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("a", parsed.namedParameters().get(0));
        }

        // --- Bug fix: parameterCount must be safely published via the static cache (now final field) ---

        // Bug fix: when SqlParser produces a single token containing multiple "#{...}" iBatis
        // markers (e.g. "#{a}#{b}" with no separator between), the previous code extracted only
        // the first marker and silently embedded later "#{...}" markers into the parameterized
        // SQL as literal text, losing those parameter bindings entirely.
        @Test
        void testParse_AdjacentIbatisTokens_extractsAllParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT #{a}#{b} FROM dual");
            assertEquals("SELECT ? ? FROM dual", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals("a", parsed.namedParameters().get(0));
            assertEquals("b", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_BlankIbatisTokenBetweenValid_extractsAllParameters() {
            // "#{a}#{ }#{b}" : the blank/empty marker in the middle must not abort extraction
            // of the trailing "#{b}" parameter.
            ParsedSql parsed = ParsedSql.parse("SELECT #{a}#{ }#{b} FROM dual");
            assertEquals("SELECT ?#{ }? FROM dual", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(2, parsed.namedParameters().size());
            assertEquals("a", parsed.namedParameters().get(0));
            assertEquals("b", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_BlankIbatisTokenRemainsLiteral() {
            ParsedSql parsed = ParsedSql.parse("SELECT #{   } FROM dual");

            assertEquals("SELECT #{ } FROM dual", parsed.parameterizedSql());
            assertEquals(0, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        @Test
        void testParse_DanglingIbatisOpenerAtEnd_throws() {
            // A standalone "#{" with no closing '}' anywhere now fails fast with the documented
            // IllegalArgumentException instead of silently passing through as literal text.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT #{"));
        }

        @Test
        void testParse_EmptyIbatisTokenBeforeValid_extractsTrailingParameter() {
            // Bug fix: a literal "#{}" followed by a real "#{a}" marker inside the SAME token
            // (e.g. "#{}#{a}") previously broke out of the marker loop and silently embedded
            // "#{a}" as literal text, losing the parameter binding entirely.
            ParsedSql parsed = ParsedSql.parse("SELECT #{}#{a} FROM dual");
            assertEquals("SELECT #{}? FROM dual", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals(1, parsed.namedParameters().size());
            assertEquals("a", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_EmptyIbatisTokenRemainsLiteral() {
            ParsedSql parsed = ParsedSql.parse("SELECT #{} FROM dual");

            assertEquals("SELECT #{} FROM dual", parsed.parameterizedSql());
            assertEquals(0, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        @Test
        void testParse_EscapedMyBatisProbeInQuotedTextKeepsOperatorGapsAndSourceOffsets() {
            // The cheap SQL-text probe can fire on a literal or comment. It must not then reinterpret
            // separated operators as escapes, or move a real placeholder's offset after comments disappear.
            for (final String gap : List.of(" ", "/* gap */", "/* first *//* second */", " -- gap\n")) {
                final String sql = "SELECT doc ?" + gap + "?, '??#{literal}' FROM t /* ??#{comment} */";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertTrue(parsed.namedParameters().isEmpty(), sql);
                assertArrayEquals(new int[] { sql.indexOf("?, '") }, parsed.positionalParameterOffsets(), sql);
            }
        }

        // Regression: MyBatis ends the property name at ':' (the oldJdbcType shorthand) as well as ',', so #{id:BIGINT} binds "id".
        @Test
        void testParse_ibatisJdbcTypeShorthandEndsParameterName() {
            final ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE id = #{id:BIGINT} AND name = #{name:VARCHAR,javaType=String}");
            assertEquals("SELECT * FROM t WHERE id = ? AND name = ?", parsed.parameterizedSql());
            assertEquals(List.of("id", "name"), parsed.namedParameters());
            assertEquals(2, parsed.parameterCount());

            assertEquals(List.of("id"), ParsedSql.parse("SELECT * FROM t WHERE id = #{ id : BIGINT }").namedParameters());
            assertEquals(List.of("user.id"), ParsedSql.parse("SELECT * FROM t WHERE id = #{user.id:NUMERIC, mode=IN}").namedParameters());
        }

        @Test
        void testParse_IbatisMarkerInsideQuotedToken_remainsLiteral() {
            ParsedSql parsed = ParsedSql.parse("SELECT '#{ignored}', pre#{actual}post FROM dual");
            assertEquals("SELECT '#{ignored}', pre?post FROM dual", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals(Arrays.asList("actual"), parsed.namedParameters());
        }

        @Test
        void testParse_IbatisMarkersSeparatedByLiteralText_extractsAllParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT pre#{a}mid#{b}post FROM dual");
            assertEquals("SELECT pre?mid?post FROM dual", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(Arrays.asList("a", "b"), parsed.namedParameters());
        }

        // Regression: a balanced quote inside a MyBatis marker (map-key access, a literal attribute) was rejected as an unclosed marker.
        @Test
        void testParse_ibatisMarkerWithBalancedQuotesBinds() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE x = #{map['k']}");
            assertEquals("SELECT * FROM t WHERE x = ?", parsed.parameterizedSql());
            assertEquals(List.of("map['k']"), parsed.namedParameters());

            assertEquals(List.of("map[\"k\"]"), ParsedSql.parse("SELECT * FROM t WHERE x = #{map[\"k\"]}").namedParameters());
            assertEquals(List.of("map['k']"), ParsedSql.parse("SELECT * FROM t WHERE x = #{map['k'], jdbcType=VARCHAR}").namedParameters());
            assertEquals(List.of("p['a']", "p['id']"), ParsedSql.parse("UPDATE t SET a = #{p['a']} WHERE id = #{p['id']}").namedParameters());
            assertEquals(List.of("row['col']"), ParsedSql.parse("INSERT INTO t (a) VALUES (#{row['col']})").namedParameters());
            assertEquals(List.of("ids[0]", "ids['1']"), ParsedSql.parse("SELECT * FROM t WHERE x IN (#{ids[0]}, #{ids['1']})").namedParameters());
            assertEquals(List.of("\"a\""), ParsedSql.parse("SELECT ARRAY[#{\"a\"}] FROM t").namedParameters());

            parsed = ParsedSql.parse("SELECT * FROM t WHERE x = ARRAY[#{a, 'x'}]");
            assertEquals("SELECT * FROM t WHERE x = ARRAY[?]", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());

            // A '}' inside a literal still cannot close the marker, and a quote left open is rejected.
            for (final String sql : List.of("SELECT ARRAY[#{a, '}'] FROM t", "SELECT ARRAY[#{a, 'x'] FROM t WHERE b = #{c}", "SELECT ARRAY[#{a, '}' FROM t",
                    "SELECT * FROM t WHERE c = #{ x AND d = '}'")) {
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
            }

            // Comments inside a marker in a bracket group are skipped, quotes and braces in them included.
            parsed = ParsedSql.parse("SELECT ARRAY[#{a, jdbcType=VARCHAR /* it's } */}] FROM t");
            assertEquals("SELECT ARRAY[?] FROM t", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());
            parsed = ParsedSql.parse("SELECT ARRAY[#{a, jdbcType=VARCHAR -- it's\n}] FROM t");
            assertEquals("SELECT ARRAY[?] FROM t", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());
        }

        // Regression: #{:id} (a MyBatis marker with an empty property name) was kept verbatim and the ':id' inside it converted, giving "#{?}".
        @Test
        void testParse_ibatisMarkerWithEmptyPropertyNameIsRejected() {
            for (final String marker : List.of("#{:id}", "#{:id:INT}", "#{ :id, mode=IN }", "#{:INTEGER, mode=IN}", "#{,x}", "#{,:x}", "#{ , mode=IN}")) {
                final String sql = "SELECT * FROM t WHERE a = " + marker;
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
                assertTrue(e.getMessage().contains("empty property name"), sql);
            }

            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE a = #{a} AND b = #{:b}"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE a = #{:x} AND b = :b"));

            // Blank markers are still kept verbatim without a binding.
            final ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a = #{} AND b = #{ } AND c = #{c}");
            assertEquals("SELECT * FROM t WHERE a = #{} AND b = #{ } AND c = ?", parsed.parameterizedSql());
            assertEquals(List.of("c"), parsed.namedParameters());

            // The ':jdbcType' shorthand after a name.
            assertEquals(List.of("list[0]"), ParsedSql.parse("SELECT * FROM t WHERE a = #{list[0]:INTEGER}").namedParameters());
            assertEquals(List.of("a"), ParsedSql.parse("SELECT * FROM t WHERE a = #{a::int}").namedParameters());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT * FROM t WHERE a = #{ id :BIGINT, mode=IN }").namedParameters());
            assertEquals("SELECT * FROM t WHERE a = ?::int", ParsedSql.parse("SELECT * FROM t WHERE a = #{id:BIGINT}::int").parameterizedSql());
            assertEquals(List.of("a", "b"), ParsedSql.parse("SELECT * FROM t WHERE id = ARRAY[#{a:INT}, #{b:VARCHAR}]").namedParameters());
        }

        @Test
        void testParse_IbatisNamedParameterWithSuffix() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = #{id}::int");
            assertEquals("SELECT * FROM users WHERE id = ?::int", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_IbatisParameterTokenAssemblyStopsAtFirstClosingBrace() {
            // Verifies the loop stops at the FIRST '}', not the last character being '}'
            ParsedSql parsed = ParsedSql.parse("INSERT INTO t VALUES (#{a},#{b})");
            assertEquals(2, parsed.parameterCount(), "Two iBatis parameters should be detected");
            assertEquals("a", parsed.namedParameters().get(0));
            assertEquals("b", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_IbatisParameterWithLeadingWhitespaceOnly_binds() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = #{ id}");
            assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals(Arrays.asList("id"), parsed.namedParameters());
        }

        // --- Bug fix: "#{ id }" (whitespace immediately after the "#{" opener) must bind as a
        // parameter. The tokenizer emits a standalone 2-char "#{" token in that case; the old
        // "length >= 3" gate skipped the marker-assembly loop and left the marker as literal text. ---

        @Test
        void testParse_IbatisParameterWithWhitespaceAfterOpener_binds() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = #{ id }");
            assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals(Arrays.asList("id"), parsed.namedParameters());
        }

        @Test
        void testParse_JdbcEscapedMyBatisBoundaryDoesNotHideOtherTokens() {
            // Odd-length runs retain a real JDBC marker after the escaped pairs.
            for (final String expression : List.of("doc ???#{key}", "doc @???#{path}", "doc ?????#{key}", "doc @?????#{path}")) {
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT " + expression));
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[" + expression + "]"));
            }

            final String sql = "SELECT left_line ??# right_line, '??#{literal}', ? /* ??#{comment} */";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
            assertEquals(sql.substring(0, sql.indexOf(" /*")), parsed.parameterizedSql());
            assertArrayEquals(new int[] { sql.indexOf(", ?") + 2 }, parsed.positionalParameterOffsets());
        }

        @Test
        void testParse_JdbcEscapesPreserveAdjacentMyBatisBindings() {
            for (final String operator : List.of("??", "@??", "??|", "??&")) {
                for (final String binding : List.of("#{key}", "#{ key, jdbcType=OTHER }")) {
                    for (final boolean grouped : new boolean[] { false, true }) {
                        final String expression = "doc " + operator + binding;
                        final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t";
                        final ParsedSql parsed = ParsedSql.parse(sql);

                        assertEquals(1, parsed.parameterCount(), sql);
                        assertEquals(List.of("key"), parsed.namedParameters(), sql);
                        assertEquals(sql.replace(binding, "?"), parsed.parameterizedSql(), sql);
                        assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = ?"), sql);
                        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = :id"), sql);
                    }
                }
            }

            for (final int questionMarks : new int[] { 4, 6 }) {
                final String sql = "SELECT doc " + "?".repeat(questionMarks) + "#{key}";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertEquals(List.of("key"), parsed.namedParameters(), sql);
                assertEquals(sql.replace("#{key}", "?"), parsed.parameterizedSql(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
            }

            final ParsedSql repeated = ParsedSql.parse("SELECT doc ??#{key} OR other @??#{path}");
            assertEquals(2, repeated.parameterCount());
            assertEquals(List.of("key", "path"), repeated.namedParameters());
            assertEquals("SELECT doc ??? OR other @???", repeated.parameterizedSql());
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT doc ??#{key"));
        }

        @Test
        void testParse_JdbcEscapesPreserveSplitMyBatisMetadataAndQuotedCasts() {
            // Restoring the opener must happen before operand classification: option text is opaque,
            // even when it contains question marks or unbalanced parentheses. A quoted cast suffix
            // must not make the restored binding look like a quoted SQL token.
            for (final String binding : List.of("#{ key, typeHandler=handler(?) }", "#{ key, typeHandler=handler( }", "#{ key, typeHandler=handler) }")) {
                for (final String operator : List.of("??", "@??")) {
                    final String type = operator.equals("??") ? "text" : "jsonpath";
                    for (final String cast : List.of("", "::\"" + type + "\"", " :: \"" + type + "\"")) {
                        for (final boolean grouped : new boolean[] { false, true }) {
                            final String expression = "doc " + operator + binding + cast;
                            final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t WHERE id = #{id}";
                            final ParsedSql parsed = ParsedSql.parse(sql);

                            assertEquals(2, parsed.parameterCount(), sql);
                            assertEquals(List.of("key", "id"), parsed.namedParameters(), sql);
                            assertEquals(sql.replace(binding, "?").replace("#{id}", "?"), parsed.parameterizedSql(), sql);
                            assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                        }
                    }
                }
            }
        }

        @Test
        void testParse_LimitOffsetPaddedIbatisMarkers_bindBoth() {
            // Limit.toString() renders "LIMIT #{ maxRows } OFFSET #{ startRow }" — both padded
            // markers must bind instead of surviving as literal SQL text.
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t LIMIT #{ maxRows } OFFSET #{ startRow }");
            assertEquals("SELECT * FROM t LIMIT ? OFFSET ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(Arrays.asList("maxRows", "startRow"), parsed.namedParameters());
        }

        @Test
        void testParse_malformedIbatisParameter_missingClosingBracket_throwsException() {
            // Bug fix: #{param without closing } should throw, not silently corrupt SQL
            assertThrows(IllegalArgumentException.class, () -> {
                ParsedSql.parse("INSERT INTO users (id) VALUES (#{userId)");
            });
        }

        @Test
        void testParse_malformedIbatisParameter_missingClosingBracketAtEnd_throwsException() {
            // #{param at end of SQL without closing }
            assertThrows(IllegalArgumentException.class, () -> {
                ParsedSql.parse("SELECT * FROM users WHERE id = #{userId");
            });
        }

        @Test
        void testParse_malformedIbatisParameter_withTrailingTokensConsumed_throwsException() {
            // Bug fix: the while-loop was consuming all remaining tokens when } was missing,
            // which silently lost subsequent parameters and corrupted SQL structure
            assertThrows(IllegalArgumentException.class, () -> {
                ParsedSql.parse("INSERT INTO t VALUES (#{id}, #{name)");
            });
        }

        @Test
        void testParse_RepeatedMybatisBindingsKeepNamesAndAttributesSeparate() {
            final String sql = "SELECT " + "#{ id, jdbcType=BIGINT },#{ name:VARCHAR },".repeat(20) + "#{id}";
            final ParsedSql parsed = ParsedSql.parse(sql, null);
            final List<String> expected = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                expected.add("id");
                expected.add("name");
            }
            expected.add("id");

            assertEquals("SELECT " + "?,".repeat(40) + "?", parsed.parameterizedSql());
            assertEquals(expected, parsed.namedParameters());
            assertEquals(41, parsed.parameterCount());
            assertSame(parsed.namedParameters().get(0), parsed.namedParameters().get(40));
            assertSame(parsed.namedParameters().get(1), parsed.namedParameters().get(39));
            assertThrows(UnsupportedOperationException.class, () -> parsed.namedParameters().set(0, "changed"));
        }

        @Test
        void testParse_SimpleIbatisInsertProducesPlaceholders() {
            ParsedSql parsed = ParsedSql.parse("INSERT INTO t (id) VALUES (#{id})");
            assertEquals("INSERT INTO t (id) VALUES (?)", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        // Regression: a balanced quote in a split MyBatis marker (#{ map['k'] }) was rejected although the glued #{map['k']} binds.
        @Test
        void testParse_splitIbatisMarkerWithBalancedQuotesBinds() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a = #{ map['k'] }");
            assertEquals("SELECT * FROM t WHERE a = ?", parsed.parameterizedSql());
            assertEquals(List.of("map['k']"), parsed.namedParameters());

            parsed = ParsedSql.parse("SELECT * FROM t WHERE a = #{ map['k'], jdbcType=VARCHAR } AND b = #{ b }");
            assertEquals("SELECT * FROM t WHERE a = ? AND b = ?", parsed.parameterizedSql());
            assertEquals(List.of("map['k']", "b"), parsed.namedParameters());

            // A '}' inside a literal still cannot close the marker, and a new "#{" before the '}' means it was never closed.
            for (final String sql : List.of("SELECT * FROM t WHERE c = #{ x AND d = '}'", "SELECT * FROM t WHERE c = #{ x AND d = 'a}b' AND e = #{y}",
                    "SELECT * FROM t WHERE c = #{x AND d = 'a}b' AND e = #{ y }", "SELECT * FROM t WHERE c = #{ x AND d = 1 AND e = #{y}",
                    "SELECT * FROM t WHERE c = #{a#{b}", "SELECT * FROM t WHERE c = #{ 'x' }", "SELECT * FROM t WHERE c = #{ \"x\" }",
                    "UPDATE t SET a = #{ '\n' x #{ map['k'] }#{a, '}'}")) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
                assertTrue(e.getMessage().contains("missing closing '}'"), sql);
            }
        }

        @Test
        void testParse_ThreeAdjacentIbatisTokens_extractsAllParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT #{a}#{b}#{c} FROM dual");
            assertEquals("SELECT ? ? ? FROM dual", parsed.parameterizedSql());
            assertEquals(3, parsed.parameterCount());
            assertEquals("a", parsed.namedParameters().get(0));
            assertEquals("b", parsed.namedParameters().get(1));
            assertEquals("c", parsed.namedParameters().get(2));
        }

        @Test
        void testParse_unclosedIbatisMarker_notClosedByBraceInsideLaterQuotedLiteral() {
            // A '}' inside a later quoted literal/identifier must not close an unterminated "#{".
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE c = #{ x AND d = '}'"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE c = #{x AND d = 'a}b' AND e = #{y}"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE c = #{ x AND d = \"}\""));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE c = #{ x AND d = `}`"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE c = (#{ x) AND d = '}'"));

            // Legit forms are unaffected.
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE c = #{ name } AND d = '}'");
            assertEquals("SELECT * FROM t WHERE c = ? AND d = '}'", parsed.parameterizedSql());
            assertEquals(List.of("name"), parsed.namedParameters());

            parsed = ParsedSql.parse("SELECT * FROM t WHERE c = #{ name , jdbcType=VARCHAR } AND d = 'x'");
            assertEquals("SELECT * FROM t WHERE c = ? AND d = 'x'", parsed.parameterizedSql());
            assertEquals(List.of("name"), parsed.namedParameters());

            parsed = ParsedSql.parse("SELECT * FROM t WHERE c = #{name,jdbcType=VARCHAR} AND e = #{ other }");
            assertEquals("SELECT * FROM t WHERE c = ? AND e = ?", parsed.parameterizedSql());
            assertEquals(List.of("name", "other"), parsed.namedParameters());

            parsed = ParsedSql.parse("SELECT * FROM t WHERE c = #{ x /* it's */ }");
            assertEquals("SELECT * FROM t WHERE c = ?", parsed.parameterizedSql());
            assertEquals(List.of("x"), parsed.namedParameters());
        }

        @Test
        void testParse_UnescapedJsonOperatorGluedToIbatisBinding() {
            final ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE doc ?#{key}");
            assertEquals(Arrays.asList("key"), parsed.namedParameters());
            assertEquals(1, parsed.parameterCount());
            assertFalse(parsed.parameterizedSql().contains("#{"), parsed.parameterizedSql());
            assertEquals(parsed.namedParameters(), ParsedSql.parse("SELECT ARRAY[doc ?#{key}]").namedParameters());

            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE a = ?#{key}"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[?#{key}]"));
        }

        @Test
        void testParse_validIbatisParameter_stillWorks() {
            // Ensure the fix doesn't break valid iBatis parameters
            ParsedSql parsed = ParsedSql.parse("INSERT INTO t (a, b) VALUES (#{id}, #{name})");
            assertEquals("INSERT INTO t (a, b) VALUES (?, ?)", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
            assertEquals("name", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_validIbatisParameterWithOptions_stillWorks() {
            // Ensure the fix doesn't break valid iBatis parameters with options like jdbcType
            ParsedSql parsed = ParsedSql.parse("INSERT INTO t (a) VALUES (#{id,jdbcType=INTEGER})");
            assertEquals("INSERT INTO t (a) VALUES (?)", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_WithIBatisParameter() {
            ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, email) VALUES (#{name}, #{email})");
            assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(2, parsed.namedParameters().size());
            assertEquals("name", parsed.namedParameters().get(0));
            assertEquals("email", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_WithIBatisParameterOptions() {
            ParsedSql parsed = ParsedSql.parse("INSERT INTO users (id) VALUES (#{id,jdbcType=INTEGER})");
            assertEquals("INSERT INTO users (id) VALUES (?)", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals(1, parsed.namedParameters().size());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_withIBatisStyleParameters() {
            String sql = "INSERT INTO users (name, email) VALUES (#{userName}, #{userEmail})";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertNotNull(parsed);
            assertEquals("INSERT INTO users (name, email) VALUES (#{userName}, #{userEmail})", parsed.originalSql());
            assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", parsed.parameterizedSql());

            List<String> namedParams = parsed.namedParameters();
            assertEquals(2, namedParams.size());
            assertEquals("userName", namedParams.get(0));
            assertEquals("userEmail", namedParams.get(1));
            assertEquals(2, parsed.parameterCount());
        }

        @Test
        void testParsedSql_parseIBatisStyle() {
            ParsedSql ps2 = ParsedSql.parse("INSERT INTO users (name, email) VALUES (#{name}, #{email})");
            assertEquals(Arrays.asList("name", "email"), ps2.namedParameters());
        }

        // Regression: inside a bracket group a MyBatis binding ended at its first '}', even one in quoted metadata or a
        // comment; the quote left behind then hid a following positional '?', so mixed parameter styles were not rejected.
        @Test
        void testQuotedMybatisClosingBraceCannotHideFollowingPositionalBinding() {
            for (final String metadata : List.of("label='}'", "label=\"}\"", "label=`}`", "/* } */ jdbcType=INTEGER", "-- }\n jdbcType=INTEGER")) {
                final String sql = "SELECT ARRAY[#{id, " + metadata + "}, ?]";
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
            }
        }

        // Controls: a quoted or commented '?', '}' or '(' in bracketed MyBatis metadata stays part of the binding.
        @Test
        void testQuotedMybatisMetadataKeepsQuestionMarksAndBracesOutOfSqlClassification() {
            for (final String metadata : List.of("label='}?('", "label=\"}?\"", "label=`}?`", "/* }? */ jdbcType=INTEGER")) {
                final ParsedSql parsed = ParsedSql.parse("SELECT ARRAY[#{id, " + metadata + "}, doc ? 'key']");
                assertEquals("SELECT ARRAY[?, doc ? 'key']", parsed.parameterizedSql());
                assertEquals(List.of("id"), parsed.namedParameters());
                assertEquals(1, parsed.parameterCount());
            }
        }
    }

    @Nested
    class NamedParameters {
        @Test
        void testComplexNamedParameters() {
            String sql = "SELECT * FROM users WHERE user_id = :user_id AND user_name123 = :user_name123";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> params = parsed.namedParameters();
            assertEquals(2, params.size());
            assertEquals("user_id", params.get(0));
            assertEquals("user_name123", params.get(1));
        }

        @Test
        void testDollarNumericParameters_areNotNamedParameters() {
            String sql = "SELECT * FROM users WHERE id = $1 AND status = $2";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> namedParams = parsed.namedParameters();
            assertTrue(namedParams.isEmpty());
        }

        @Test
        void testMultipleSameNamedParameters() {
            String sql = "SELECT * FROM users WHERE id = :id OR parent_id = :id";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> params = parsed.namedParameters();
            assertEquals(2, params.size());
            assertEquals("id", params.get(0));
            assertEquals("id", params.get(1));
            assertEquals(2, parsed.parameterCount());
        }

        @Test
        void testNamedParameterBoundariesRejectUnpairedSurrogates() {
            for (final String boundary : List.of("\uD800", "\uDC00", "a\uDC00")) {
                final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT " + boundary + ":name"));
                assertTrue(error.getMessage().contains("unpaired"));
            }
            final ParsedSql symbol = ParsedSql.parse("SELECT \uD83D\uDE00:name");
            assertEquals(List.of("name"), symbol.namedParameters());
            assertEquals("SELECT \uD83D\uDE00?", symbol.parameterizedSql());
        }

        @Test
        void testParameterWithSpecialCharacters() {
            // Test that colons not followed by valid parameter characters are not treated as parameters
            String sql = "SELECT * FROM users WHERE time > '10:30:00' AND id = :userId";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> params = parsed.namedParameters();
            assertEquals(1, params.size());
            assertEquals("userId", params.get(0));
            assertEquals(1, parsed.parameterCount());
        }

        // Bug fix: when SqlParser produces a single token containing multiple ":named"
        // markers (e.g. ":a:b" with no separator between, since ':' is not a token separator),
        // the previous code extracted only the leading marker and silently embedded later
        // ":named" markers into the parameterized SQL as literal text, losing those parameter
        // bindings entirely. This mirrors the analogous "#{a}#{b}" iBatis fix.
        @Test
        void testParse_AdjacentNamedParameters_extractsAllParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a=:a:b");
            assertEquals("SELECT * FROM t WHERE a=? ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(2, parsed.namedParameters().size());
            assertEquals("a", parsed.namedParameters().get(0));
            assertEquals("b", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_AdjacentNamedParametersWithCast_extractsBothAndPreservesCast() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a=:a:b::int");
            assertEquals("SELECT * FROM t WHERE a=? ?::int", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals("a", parsed.namedParameters().get(0));
            assertEquals("b", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_AmbiguousQuoteTokensKeepMalformedNamedTextVerbatim() {
            // The two quote readings disagree about ':x', so the entire token is opaque, including earlier malformed names.
            for (final String name : List.of("\uD800", "\uDC00", "valid\uD800", "valid\uDC00", "valid.\uD800", "valid.\uDC00", "first:\uD800",
                    "first:valid.\uDC00")) {
                final String sql = "SELECT * FROM t WHERE a = :" + name + "['\\'':x]";
                final ParsedSql parsed = ParsedSql.parse(sql, null);
                assertEquals(sql, parsed.parameterizedSql(), sql);
                assertEquals(List.of(), parsed.namedParameters(), sql);
                assertEquals(0, parsed.parameterCount(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);

                final ParsedSql withBinding = ParsedSql.parse(sql + " AND id = :id", null);
                assertEquals(sql + " AND id = ?", withBinding.parameterizedSql(), sql);
                assertEquals(List.of("id"), withBinding.namedParameters(), sql);
                assertEquals(1, withBinding.parameterCount(), sql);
            }
        }

        @Test
        void testParse_AmbiguousQuoteTokensPreserveLaterMixedStyleErrors() {
            for (final String name : List.of("\uD800", "\uDC00", "valid\uD800", "valid.\uDC00")) {
                for (final String laterStyles : List.of(" AND b = :id AND c = #{name}", " AND b = :id AND c = ?")) {
                    final String sql = "SELECT * FROM t WHERE a = :" + name + "['\\'':x]" + laterStyles;
                    final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql, null), sql);
                    final String foundStyles = laterStyles.endsWith("?") ? "found positional '?' and named ':propName'"
                            : "found named ':propName' and iBatis/MyBatis '#{propName}'";
                    assertTrue(error.getMessage().startsWith("Cannot mix parameter styles"), sql);
                    assertTrue(error.getMessage().contains(foundStyles), sql);
                }
            }
        }

        // Regression: a ':' glued to a closed value (backtick, bracket, MyBatis-joined token) and the classifier's named-marker lookups.
        @Test
        void testParse_colonGluedToClosedValueVariants() {
            ParsedSql parsed = ParsedSql.parse("SELECT JSON_OBJECT('k':x#{ v }) FROM t");
            assertEquals("SELECT JSON_OBJECT('k':x?) FROM t", parsed.parameterizedSql());
            assertEquals(List.of("v"), parsed.namedParameters());

            for (final String sql : List.of("SELECT `col`:x FROM t", "SELECT arr[1]:x FROM t", "SELECT [col]:x FROM t")) {
                parsed = ParsedSql.parse(sql);
                assertEquals(sql, parsed.parameterizedSql(), sql);
                assertTrue(parsed.namedParameters().isEmpty(), sql);
            }

            // A comment breaks the adjacency.
            parsed = ParsedSql.parse("SELECT * FROM t WHERE a = 'x'/**/:y");
            assertEquals("SELECT * FROM t WHERE a = 'x' ?", parsed.parameterizedSql());
            assertEquals(List.of("y"), parsed.namedParameters());

            // The ambiguous "SELECT skip ?" asks whether the statement holds a named marker; a glued 'a':col is none.
            assertOffsets("SELECT skip ? x FROM t WHERE JSON_OBJECT('a':col) IS NOT NULL", 12);
            assertOffsets("SELECT x[(SELECT skip ? y FROM u WHERE JSON_OBJECT('a':c) IS NULL)] FROM t", 22);
            assertEquals(List.of("col"), ParsedSql.parse("SELECT skip ? x FROM t WHERE JSON_OBJECT('a' :col) IS NOT NULL").namedParameters());
        }

        // Regression: a ':' glued to a closing quote, ')' or ']' (compact JSON_OBJECT('k':v), slice bounds) was bound as a named parameter.
        @Test
        void testParse_colonGluedToClosingQuoteParenthesisOrBracketIsNotNamedMarker() {
            final String sqlServer = "SELECT s.session_id, JSON_OBJECT('security_id':s.security_id, 'login':s.login_name, 'status':s.status) AS info"
                    + " FROM sys.dm_exec_sessions AS s";
            ParsedSql parsed = ParsedSql.parse(sqlServer);
            assertEquals(sqlServer, parsed.parameterizedSql());
            assertTrue(parsed.namedParameters().isEmpty());
            assertEquals(0, parsed.parameterCount());

            for (final String sql : List.of("SELECT JSON_OBJECT('a':true, 'b':null) FROM t", "SELECT JSON_OBJECT(\"a\":col) FROM t",
                    "SELECT JSON_OBJECT(CONCAT('a','b'):col) FROM t", "SELECT ARRAY[(1):v] FROM t", "SELECT arr[f(x):n] FROM t",
                    "SELECT ARRAY['a':v] FROM t")) {
                parsed = ParsedSql.parse(sql);
                assertEquals(sql, parsed.parameterizedSql(), sql);
                assertTrue(parsed.namedParameters().isEmpty(), sql);
                assertEquals(0, parsed.parameterCount(), sql);
            }

            // Positional bindings next to such a separator are not a second parameter style.
            parsed = ParsedSql.parse("SELECT JSON_OBJECT('a':col) FROM t WHERE id = ?");
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { 46 }, parsed.positionalParameterOffsets());

            // Markers after any other character are unaffected.
            assertEquals(List.of("key"), ParsedSql.parse("SELECT * FROM t WHERE payload?:key").namedParameters());
            assertEquals(List.of("a", "b"), ParsedSql.parse("SELECT * FROM t WHERE x = :a:b").namedParameters());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT * FROM t WHERE x IN (:id)").namedParameters());
            assertEquals(List.of("col"), ParsedSql.parse("SELECT JSON_OBJECT('a' :col) FROM t").namedParameters());
            assertEquals(List.of("i"), ParsedSql.parse("SELECT ARRAY['a', :i] FROM t").namedParameters());
        }

        @Test
        void testParse_ComplexNamedParameter() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :user_id_123");
            assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("user_id_123", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_InvalidIdentifierStartsAndUnicodePunctuationAreNotSwallowed() {
            final ParsedSql invalidStarts = ParsedSql.parse("SELECT :1name, :\u0301name, :\u2014name, :\u2003name FROM users");

            assertEquals(0, invalidStarts.parameterCount());
            assertTrue(invalidStarts.namedParameters().isEmpty());
            assertTrue(invalidStarts.parameterizedSql().contains(":1name"));
            assertTrue(invalidStarts.parameterizedSql().contains(":\u2014name"));

            final ParsedSql delimited = ParsedSql.parse("SELECT :name\u2014suffix, :user..address, :user.2address FROM users");
            assertEquals("SELECT ?\u2014suffix, ?..address, ?.2address FROM users", delimited.parameterizedSql());
            assertEquals(Arrays.asList("name", "user", "user"), delimited.namedParameters());
        }

        @Test
        void testParse_NamedParameterFollowedByPostgresCast_preservesCast() {
            // Regression guard: the multi-marker loop must NOT mistake the "::" of a PostgreSQL
            // cast for a second ":named" marker. ":id::int" -> extract "id", keep "::int" verbatim.
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE id = :id::int");
            assertEquals("SELECT * FROM t WHERE id = ?::int", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals(1, parsed.namedParameters().size());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_NamedParameterInsidePreservedBlockCommentIsIgnored() {
            final ParsedSql parsed = ParsedSql.parse("-- Keep comments\nSELECT /* audit :ignored */ :actual FROM users");

            assertEquals(Arrays.asList("actual"), parsed.namedParameters());
            assertEquals(1, parsed.parameterCount());
            assertTrue(parsed.parameterizedSql().contains("/* audit :ignored */"));
            assertTrue(parsed.parameterizedSql().contains("SELECT"));
            assertTrue(parsed.parameterizedSql().contains("? FROM users"));
        }

        @Test
        void testParse_NamedParameterWithSuffix() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :id::int");
            assertEquals("SELECT * FROM users WHERE id = ?::int", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_ParameterOrderingWithRepeats() {
            // Repeated named parameters preserve order and duplicates.
            ParsedSql parsed = ParsedSql.parse("SELECT :b, :a, :b FROM x");
            assertEquals(3, parsed.parameterCount());
            assertEquals(3, parsed.namedParameters().size());
            assertEquals("b", parsed.namedParameters().get(0));
            assertEquals("a", parsed.namedParameters().get(1));
            assertEquals("b", parsed.namedParameters().get(2));
        }

        @Test
        void testParse_RepeatedNamedBindingsKeepEveryOccurrenceAndImmutableStorage() {
            final String sql = "SELECT " + ":id,:name,".repeat(24) + ":id";
            final ParsedSql parsed = ParsedSql.parse(sql, null);
            final List<String> expected = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                expected.add("id");
                expected.add("name");
            }
            expected.add("id");

            assertEquals("SELECT " + "?,".repeat(48) + "?", parsed.parameterizedSql());
            assertEquals(expected, parsed.namedParameters());
            assertEquals(49, parsed.parameterCount());
            for (int i = 2; i < expected.size(); i++) {
                assertSame(parsed.namedParameters().get(i % 2), parsed.namedParameters().get(i));
            }
            assertThrows(UnsupportedOperationException.class, () -> parsed.namedParameters().clear());

            // A repeated prefix followed by many distinct names must retain all names after the reuse cache fills.
            final StringBuilder varied = new StringBuilder("SELECT :id,:id");
            final List<String> variedNames = new ArrayList<>(List.of("id", "id"));
            for (int i = 0; i < 40; i++) {
                varied.append(",:p").append(i);
                variedNames.add("p" + i);
            }
            final ParsedSql distinct = ParsedSql.parse(varied.toString(), null);
            assertEquals("SELECT " + "?,".repeat(41) + "?", distinct.parameterizedSql());
            assertEquals(variedNames, distinct.namedParameters());
            assertEquals(42, distinct.parameterCount());
        }

        @Test
        void testParse_ThreeAdjacentNamedParameters_extractsAllParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a=:a:b:c");
            assertEquals("SELECT * FROM t WHERE a=? ? ?", parsed.parameterizedSql());
            assertEquals(3, parsed.parameterCount());
            assertEquals(3, parsed.namedParameters().size());
            assertEquals("a", parsed.namedParameters().get(0));
            assertEquals("b", parsed.namedParameters().get(1));
            assertEquals("c", parsed.namedParameters().get(2));
        }

        @Test
        void testParse_UnambiguousQuoteTokensStillRejectMalformedNames() {
            for (final String name : List.of("\uD800", "\uDC00", "valid\uD800", "valid\uDC00", "valid.\uD800", "valid.\uDC00")) {
                // Backslashes alone do not suppress validation: both readings must disagree about marker positions.
                // E-prefixed literals always use backslash escapes, so they cannot produce that disagreement.
                for (final String suffix : List.of("", "[':x']", "['\\'']", "[E'\\'':x]", "[e'\\'':x]")) {
                    final String sql = "SELECT * FROM t WHERE a = :" + name + suffix + " AND b = :id AND c = #{name}";
                    final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql, null), sql);
                    assertTrue(error.getMessage().startsWith("Malformed named parameter: unpaired "), sql);
                }
            }
        }

        @Test
        void testParse_UnicodeIdentifierNamesAndDottedPaths() {
            final String supplementaryLetter = new String(Character.toChars(0x10400));
            final ParsedSql parsed = ParsedSql.parse("SELECT :用户.地址_2, :na\u0301me, :" + supplementaryLetter + "Value FROM users");

            assertEquals("SELECT ?, ?, ? FROM users", parsed.parameterizedSql());
            assertEquals(Arrays.asList("用户.地址_2", "na\u0301me", supplementaryLetter + "Value"), parsed.namedParameters());
            assertEquals(3, parsed.parameterCount());
        }

        @Test
        void testParse_UnpairedSurrogatesInProspectiveNamedParametersAreRejected() {
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :\uD800 FROM users"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :\uDC00 FROM users"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :valid\uD800 FROM users"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT arr[:\uD800] FROM users"));
        }

        @Test
        void testParse_WholeTokenNamedBindingsRetainUnicodeAndEscapeBoundaries() {
            final String supplementary = new String(Character.toChars(0x10400));
            final String sql = "SELECT :user.address.city, :" + supplementary + "name, :_a1, payload?:key, payload??:key";
            final ParsedSql parsed = ParsedSql.parse(sql, null);
            assertEquals("SELECT ?, ?, ?, payload? ?, payload???", parsed.parameterizedSql());
            assertEquals(List.of("user.address.city", supplementary + "name", "_a1", "key", "key"), parsed.namedParameters());
            assertEquals(5, parsed.parameterCount());
            assertEquals(5, ParsedSql.parse(parsed.parameterizedSql(), null).parameterCount());

            final ParsedSql boundaries = ParsedSql.parse("SELECT f():name, [q]:name, 'q':name, :id::\"int\", :a:b", null);
            assertEquals("SELECT f():name, [q]:name, 'q':name, ?::\"int\", ? ?", boundaries.parameterizedSql());
            assertEquals(List.of("id", "a", "b"), boundaries.namedParameters());
            assertEquals(3, boundaries.parameterCount());
            final ParsedSql invalidStarts = ParsedSql.parse("SELECT :, :1name, :., :\uD83D\uDE00, :_ok", null);
            assertEquals("SELECT :, :1name, :., :\uD83D\uDE00, ?", invalidStarts.parameterizedSql());
            assertEquals(List.of("_ok"), invalidStarts.namedParameters());
            assertEquals(1, invalidStarts.parameterCount());
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :id\uD800", null));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :id, ?", null));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :id, #{name}", null));
        }

        @Test
        void testParse_WithMultipleNamedParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE age > :minAge AND age < :maxAge");
            assertEquals("SELECT * FROM users WHERE age > ? AND age < ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(2, parsed.namedParameters().size());
            assertEquals("minAge", parsed.namedParameters().get(0));
            assertEquals("maxAge", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_WithNamedParameter() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
            assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals(1, parsed.namedParameters().size());
            assertEquals("userId", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_withNamedParameters() {
            String sql = "SELECT * FROM users WHERE id = :userId AND status = :userStatus";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertNotNull(parsed);
            assertEquals("SELECT * FROM users WHERE id = :userId AND status = :userStatus", parsed.originalSql());
            assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());

            List<String> namedParams = parsed.namedParameters();
            assertEquals(2, namedParams.size());
            assertEquals("userId", namedParams.get(0));
            assertEquals("userStatus", namedParams.get(1));
            assertEquals(2, parsed.parameterCount());
        }

        @Test
        void testParsedSql_parseNamedParameters() {
            ParsedSql ps1 = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
            assertEquals("SELECT * FROM users WHERE id = ?", ps1.parameterizedSql());
        }
    }

    @Nested
    class ParameterStyleValidation {
        @Test
        void testMixedParameterStyles_iBatisAndNamed_throwsException() {
            String sql = "SELECT * FROM users WHERE id = #{userId} AND status = :status";

            assertThrows(IllegalArgumentException.class, () -> {
                ParsedSql.parse(sql);
            });
        }

        @Test
        void testParse_AdjacentMixedIBatisAndNamedThrows() {
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT :id#{name} FROM users"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT #{name}:id FROM users"));
        }

        @Test
        void testParse_JdbcCallEscape_RejectsMixedParameterStyles() {
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("{? = call get_val(:id)}"));
        }

        @Test
        void testParse_MixedIBatisAndNamedThrows() {
            assertThrows(IllegalArgumentException.class, () -> {
                ParsedSql.parse("SELECT * FROM users WHERE id = :id AND name = #{name}");
            });
        }

        @Test
        void testParse_MixedParametersThrows() {
            assertThrows(IllegalArgumentException.class, () -> {
                ParsedSql.parse("SELECT * FROM users WHERE id = ? AND name = :name");
            });
        }

        @Test
        void testParse_MixedParameterStyleMessageNamesStylesAndSubscriptRule() {
            // A '?' that opens a subscript element is a placeholder, so a named binding in the same group is a
            // real mix. The rejection explains which '?' counted and why a JSON operator would not have.
            final String mixedSubscript = "SELECT ARRAY [?, :id]";
            final String subscriptMessage = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(mixedSubscript)).getMessage();

            assertTrue(subscriptMessage.contains("found positional '?' and named ':propName'"), subscriptMessage);
            assertTrue(subscriptMessage.contains("inside a bracket group"), subscriptMessage);
            assertTrue(subscriptMessage.contains("JSON existence operator"), subscriptMessage);
            assertTrue(subscriptMessage.contains("typed unary geometric operators"), subscriptMessage);
            assertTrue(subscriptMessage.contains("SQL/JSON constructor clauses"), subscriptMessage);
            assertTrue(subscriptMessage.contains("SQL: " + mixedSubscript), subscriptMessage);

            final String ibatisMessage = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY [?, #{id}]")).getMessage();
            assertTrue(ibatisMessage.contains("found positional '?' and iBatis/MyBatis '#{propName}'"), ibatisMessage);
            assertTrue(ibatisMessage.contains("inside a bracket group"), ibatisMessage);

            // Ordinary positional conflicts get the same general guidance, but no bracket-specific sentence.
            final String plainMessage = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE a = ? AND b = :b"))
                    .getMessage();
            assertTrue(plainMessage.contains("found positional '?' and named ':propName'"), plainMessage);
            assertFalse(plainMessage.contains("bracket group"), plainMessage);
            assertTrue(plainMessage.contains("JSON existence operator"), plainMessage);
            assertTrue(plainMessage.contains("typed unary geometric operators"), plainMessage);
            assertTrue(plainMessage.contains("SQL/JSON constructor clauses"), plainMessage);
            assertTrue(plainMessage.contains("SQL: SELECT * FROM t WHERE a = ? AND b = :b"), plainMessage);

            // Question marks in operators or quoted text do not justify positional-marker guidance.
            for (final String sql : List.of("SELECT * FROM t WHERE a = :a AND b = #{b}", "SELECT payload ? 'key', :a, #{b} FROM t",
                    "SELECT ARRAY[:a, #{b}, '?']")) {
                final String namedAndIbatis = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql)).getMessage();
                assertTrue(namedAndIbatis.contains("found named ':propName' and iBatis/MyBatis '#{propName}'"), namedAndIbatis);
                assertFalse(namedAndIbatis.contains("bracket group"), namedAndIbatis);
                assertFalse(namedAndIbatis.contains("JSON existence operators"), namedAndIbatis);
                assertFalse(namedAndIbatis.contains("typed unary geometric operators"), namedAndIbatis);
                assertFalse(namedAndIbatis.contains("SQL/JSON constructor clauses"), namedAndIbatis);
                assertFalse(namedAndIbatis.contains("In compact expressions"), namedAndIbatis);
                assertTrue(namedAndIbatis.contains("SQL: " + sql), namedAndIbatis);
            }
        }

        @Test
        void testParse_MixedStyleMessageExplainsCompactBindings() {
            // Compact arithmetic/concatenation retains its leading binding in both token streams.
            for (final String expression : List.of("?-1", "?||'x'")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    for (final String binding : List.of(":other", "#{other}")) {
                        final String sql = grouped ? "SELECT ARRAY[" + expression + ", " + binding + "]" : "SELECT " + expression + ", " + binding + " FROM t";
                        final String message = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql)).getMessage();

                        assertEquals(grouped, message.contains("inside a bracket group"), message);
                        assertTrue(message.contains("In compact expressions such as ?-1 and ?||'x', the leading '?' is a positional parameter."), message);
                        assertTrue(message.contains("SQL: " + sql), message);
                    }
                }
            }
        }

        @Test
        void testParse_MixedStyleMessageExplainsSqlJsonValuesAndUnaryOperators() {
            // Clause keywords do not turn a constructor's value placeholder into an operator. The diagnostic
            // must explain why that value still counts even though words surround its question mark.
            for (final boolean grouped : new boolean[] { false, true }) {
                final String prefix = grouped ? "SELECT ARRAY[" : "SELECT ";
                final String suffix = grouped ? "]" : " FROM t";

                for (final String binding : List.of(":other", "#{other}")) {
                    final String found = binding.startsWith(":") ? "found positional '?' and named ':propName'"
                            : "found positional '?' and iBatis/MyBatis '#{propName}'";

                    for (final String clause : List.of("NULL ON NULL", "ABSENT ON NULL", "FORMAT JSON")) {
                        final String sql = prefix + "JSON_OBJECT('k' VALUE ? " + clause + "), " + binding + suffix;
                        final String message = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql)).getMessage();

                        assertTrue(message.contains(found), message);
                        assertEquals(grouped, message.contains("inside a bracket group"), message);
                        assertTrue(message.contains("SQL/JSON constructor clauses"), message);
                        assertTrue(message.contains("are still parameters"), message);
                        assertTrue(message.contains("SQL: " + sql), message);
                    }

                    // The unary operator does not bind. Only the separate '?' creates the mixed-style error.
                    for (final String geometry : List.of("?- lseg '(0,0),(1,0)'", "?| line '{1,0,0}'")) {
                        final String sql = prefix + geometry + ", ?, " + binding + suffix;
                        final String message = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql)).getMessage();

                        assertTrue(message.contains(found), message);
                        assertEquals(grouped, message.contains("inside a bracket group"), message);
                        assertTrue(message.contains("typed unary geometric operators ('?-', '?|') are not parameters"), message);
                        assertTrue(message.contains("SQL: " + sql), message);

                        final ParsedSql namedOnly = ParsedSql.parse(prefix + geometry + ", " + binding + suffix);
                        assertEquals(1, namedOnly.parameterCount());
                        assertEquals(List.of("other"), namedOnly.namedParameters());
                        assertArrayEquals(new int[0], namedOnly.positionalParameterOffsets());
                    }
                }
            }
        }

        @Test
        void testParse_StandaloneSubscriptsRejectMixedParameterStyles() {
            for (final String sql : List.of("SELECT ARRAY [?, :id]", "SELECT ARRAY [ :id, ? ]", "SELECT ARRAY [?, #{id}]",
                    "SELECT ARRAY [ ? /* comment */ , :id ]", "SELECT ARRAY [ :id /* comment */ , ? ]")) {
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
            }
        }

        @Test
        void testParse_withMixedParameters_throwsException() {
            String sql = "SELECT * FROM users WHERE id = ? AND name = :userName";

            assertThrows(IllegalArgumentException.class, () -> {
                ParsedSql.parse(sql);
            });
        }
    }

    @Nested
    class PositionalParameters {
        @Test
        void testParameterizedSql_placeholderGluedToLineComment_dropsComment() {
            // "?--" used to lex as the operator "?-" plus "-", so the comment survived, the newline was
            // collapsed and the comment swallowed the second predicate (1 placeholder instead of 2).
            final String sql = "SELECT * FROM t WHERE id = ?-- the id\nAND name = ?";
            final ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals(2, parsed.parameterCount());
            assertEquals("SELECT * FROM t WHERE id = ? AND name = ?", parsed.parameterizedSql());
            assertArrayEquals(new int[] { 27, 49 }, parsed.positionalParameterOffsets());

            // the same shape with a block comment glued to the placeholder
            final ParsedSql block = ParsedSql.parse("SELECT * FROM t WHERE id = ?/* the id */AND name = ?");
            assertEquals(2, block.parameterCount());
            assertEquals("SELECT * FROM t WHERE id = ? AND name = ?", block.parameterizedSql());
        }

        // Regression: the CASE ... END operand and AT TIME ZONE forms around a JSON '?' operator.
        @Test
        void testParse_caseEndAndTimeZoneQuestionMarkForms() {
            assertEquals(1, ParsedSql.parse("SELECT CASE WHEN a THEN j1 ELSE j2 END ?").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT CASE WHEN a THEN j1 ELSE j2 END ? FROM t").parameterCount());

            final String sql = "SELECT CASE WHEN a THEN ? END ?| array['a'] FROM t";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { 24 }, parsed.positionalParameterOffsets());

            assertEquals(1, ParsedSql.parse("SELECT ts AT TIME /* c */ ZONE ? local_ts FROM t").parameterCount());
        }

        @Test
        void testParse_CommaBindingShortcutFallsBackForExpressionSyntax() {
            // Commas alone do not establish a simple binding list: quoted text, comments, operators
            // and nested expressions still require the complete classifier.
            for (final String expression : List.of("?, doc ?? 'k', ?", "?, doc ? 'k', ?", "?, '?', ?", "?, COALESCE(?, 0)", "? /* ? */ , ?", "? -- ?\n, ?",
                    "? + ?", "JSON_OBJECT('k' VALUE ? NULL ON NULL), ?")) {
                final String sql = "SELECT ARRAY[" + expression + "] FROM t";
                assertEquals(2, ParsedSql.parse(sql).parameterCount(), sql);
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = :id"), sql);
            }
            assertEquals(0, ParsedSql.parse("SELECT ARRAY[??]").parameterCount());
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[?, :id]"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[?, #{id}]"));
        }

        @Test
        void testParse_CommaSeparatedBindingsRetainAllOffsetsAcrossGroups() {
            // A large first group donates its offset array; later markers must append without losing it.
            for (final String prefix : List.of("ARRAY", "arr", "ARRAY ")) {
                final String sql = "SELECT " + prefix + "[ ? ,\t? ,\n? ], arr[?][?], ARRAY[" + String.join(", ", java.util.Collections.nCopies(2048, "?"))
                        + "] FROM t WHERE id = ?";
                final ParsedSql parsed = ParsedSql.parse(sql);
                final int[] expected = java.util.stream.IntStream.range(0, sql.length()).filter(i -> sql.charAt(i) == '?').toArray();
                assertEquals(2054, parsed.parameterCount(), sql);
                assertArrayEquals(expected, parsed.positionalParameterOffsets(), sql);
                assertEquals(sql, parsed.parameterizedSql(), sql);
            }
        }

        @Test
        void testParse_CompactOperatorsDoNotSwallowPositionalPlaceholders() {
            // Longest-match tokenization claims "?-", "?|", "?&", "?#", "?||" and "?-|" as single operator
            // tokens, so a placeholder written against one of them used to disappear from parameterCount()
            // while a JDBC driver still saw it and demanded a binding.
            for (final String compact : List.of("-1", "|1", "&1", "#1", "||'x'")) {
                final String bare = "SELECT ?" + compact + " FROM t";
                final ParsedSql parsedBare = ParsedSql.parse(bare);
                assertEquals(1, parsedBare.parameterCount(), bare);
                assertEquals(bare, parsedBare.parameterizedSql(), bare);
                assertArrayEquals(new int[] { bare.indexOf('?') }, parsedBare.positionalParameterOffsets(), bare);

                // A bracket group reaches the same verdict for the same text.
                final String grouped = "SELECT ARRAY[?" + compact + "]";
                final ParsedSql parsedGrouped = ParsedSql.parse(grouped);
                assertEquals(1, parsedGrouped.parameterCount(), grouped);
                assertArrayEquals(new int[] { grouped.indexOf('?') }, parsedGrouped.positionalParameterOffsets(), grouped);
            }

            // Every marker of a compact concatenation is counted, in both positions.
            for (final String sql : List.of("SELECT ?||? FROM t", "SELECT ARRAY[?||?]")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(2, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }

            // The swallowed marker also hid a genuine mix of styles from the guard.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ?-:n FROM t"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[?-:n]"));

            // In operator position the same spellings remain PostgreSQL operators and bind nothing.
            for (final String sql : List.of("SELECT * FROM t WHERE box1 ?- box2", "SELECT * FROM t WHERE line1 ?|| line2", "SELECT * FROM t WHERE p1 ?| p2",
                    "SELECT * FROM t WHERE tags ?& ARRAY['a']", "SELECT ARRAY[a ?- b]", "SELECT ARRAY[a ?|| b]")) {
                assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
            }

            assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE tags ?| ARRAY['a'] AND id = ?").parameterCount());
        }

        // Regression: a compact '?-'/'?||' token after a RANGE/GROUPS frame unit or a bare VALUES lost its placeholder.
        @Test
        void testParse_compactPlaceholderAfterFrameUnitOrValuesIsCounted() {
            String sql = "SELECT sum(x) OVER (ORDER BY d RANGE ?-1 PRECEDING) FROM t";
            ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { 37 }, parsed.positionalParameterOffsets());

            for (final String frame : List.of("ORDER BY d DESC RANGE ?-1 PRECEDING", "PARTITION BY g ORDER BY d RANGE ?-1 PRECEDING",
                    "ORDER BY d GROUPS ?- 1 FOLLOWING", "ORDER BY d GROUPS ?-1 PRECEDING", "ORDER BY d ROWS ?-1 PRECEDING",
                    "ORDER BY d RANGE ?||'1 day' PRECEDING", "ORDER BY d RANGE BETWEEN ?-1 PRECEDING AND CURRENT ROW", "ORDER BY d RANGE ? * 2 PRECEDING",
                    "ORDER BY d RANGE ?::interval PRECEDING", "ORDER BY d RANGE (?-1) PRECEDING")) {
                sql = "SELECT sum(x) OVER (" + frame + ") FROM t";
                assertEquals(1, ParsedSql.parse(sql).parameterCount(), sql);
            }

            assertEquals(2, ParsedSql.parse("SELECT sum(x) OVER (ORDER BY d RANGE BETWEEN ? PRECEDING AND ? FOLLOWING) FROM t").parameterCount());

            for (final String values : List.of("VALUES ?-1", "VALUES ?||'x'", "VALUES ?")) {
                sql = "INSERT INTO t (a) " + values;
                parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }

            assertEquals(2, ParsedSql.parse("INSERT INTO t (a) VALUES ?, ?").parameterCount());

            // Columns named groups, range and values keep the JSON-operator reading, also inside a window definition.
            for (final String json : List.of("SELECT * FROM t WHERE values ?| array['a']", "SELECT * FROM t WHERE values ?& array['a']",
                    "SELECT * FROM t WHERE groups ? 'admin' ORDER BY d", "SELECT sum(x) OVER (PARTITION BY groups ? 'k' ORDER BY d ROWS 1 PRECEDING) FROM t",
                    "SELECT sum(x) OVER (ORDER BY range ? 'k' RANGE 1 PRECEDING) FROM t")) {
                assertEquals(0, ParsedSql.parse(json).parameterCount(), json);
            }

            sql = "SELECT * FROM t WHERE groups ?& array['a','b'] AND id = ?";
            parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets());
            assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE range ?| array['a'] AND id = ?").parameterCount());
        }

        @Test
        void testParse_ConvertedMarkerNeverFormsPgJdbcEscape() {
            // A converted marker adjacent to an unpaired '?' used to yield "??", which pgJDBC and ParsedSql
            // itself read as an escaped operator, silently dropping the counted binding.
            final String[][] cases = { { "SELECT * FROM t WHERE payload?:key", "SELECT * FROM t WHERE payload? ?" },
                    { "SELECT * FROM t WHERE payload?#{key}", "SELECT * FROM t WHERE payload? ?" },
                    { "SELECT * FROM t WHERE payload @?:path", "SELECT * FROM t WHERE payload @? ?" },
                    { "SELECT * FROM t WHERE x = :a:b", "SELECT * FROM t WHERE x = ? ?" },
                    { "SELECT * FROM t WHERE x = #{a}#{b}", "SELECT * FROM t WHERE x = ? ?" }, { "SELECT a[:lo:hi] FROM t", "SELECT a[? ?] FROM t" },
                    { "SELECT * FROM t WHERE #{a}?|array['x']", "SELECT * FROM t WHERE ? ?|array['x']" },
                    { "SELECT * FROM t WHERE :a?|array['x']", "SELECT * FROM t WHERE ? ?|array['x']" },
                    { "SELECT a[#{a}?|x] FROM t", "SELECT a[? ?|x] FROM t" },
                    // Complete escapes cannot pair with the converted marker, so the output stays compact.
                    { "SELECT * FROM t WHERE payload??:key", "SELECT * FROM t WHERE payload???" },
                    { "SELECT * FROM t WHERE payload ?? :key", "SELECT * FROM t WHERE payload ?? ?" } };

            for (final String[] c : cases) {
                final ParsedSql parsed = ParsedSql.parse(c[0]);
                assertEquals(c[1], parsed.parameterizedSql(), c[0]);
                assertEquals(parsed.parameterCount(), ParsedSql.parse(parsed.parameterizedSql()).parameterCount(), c[0]);
            }
        }

        @Test
        void testParse_DeferredOperandChecksKeepQuestionMarkContexts() {
            for (final String expression : List.of("doc /* gap */ ? 'k'", "(doc) ? 'k'", "doc ? :key", "doc ? #{key}", "?- CAST(:key AS line)",
                    "JSON_OBJECT('k' VALUE :key NULL ON NULL)")) {
                final ParsedSql parsed = ParsedSql.parse("SELECT " + expression + " FROM t");
                final boolean named = expression.contains(":key") || expression.contains("#{key}");
                assertEquals(named ? 1 : 0, parsed.parameterCount(), expression);
                assertEquals(named ? List.of("key") : List.of(), parsed.namedParameters(), expression);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), expression);
            }
            for (final String expression : List.of("? + ?", "? || ?", "? /* gap */ + ?", "COALESCE(?, ?)", "?- ?::line, ?")) {
                assertEquals(2, ParsedSql.parse("SELECT " + expression).parameterCount(), expression);
            }
        }

        @Test
        void testParse_PlaceholderAfterSkipAndAsOfKeywordsIsCounted() {
            final String[] sqls = { "SELECT FIRST ? SKIP ? id, name FROM t", "SELECT SKIP ? id FROM t", "SELECT FIRST 10 SKIP ? id FROM t",
                    "SELECT e.name FROM employees AS OF TIMESTAMP ? e WHERE e.id = ?", "SELECT e.name FROM employees AS OF SCN ? e WHERE e.id = ?",
                    "SELECT * FROM emp FOR SYSTEM_TIME AS OF ? e WHERE e.id = ?" };

            for (final String sql : sqls) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                final int expected = (int) sql.chars().filter(ch -> ch == '?').count();
                assertEquals(expected, parsed.parameterCount(), sql);
            }

            // Outside their keyword position, columns named timestamp/scn/skip keep the JSON existence operator.
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE timestamp ? 'key'").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE scn ? key").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE skip ? 'key'").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE doc ? 'key' AND x = ?").parameterCount());
        }

        @Test
        void testParse_PositionalMetadataKeepsFallbackOffsetsAndOwnership() {
            final String sql = "SELECT #{ }, '?', ?, ARRAY[? /* ? */, ?], doc ? 'key', ?";
            final ParsedSql parsed = ParsedSql.parse(sql, null);
            final int array = sql.indexOf("ARRAY[");
            final int[] expected = { sql.indexOf("?, ARRAY"), sql.indexOf('?', array), sql.indexOf(", ?]") + 2, sql.lastIndexOf('?') };
            assertEquals(sql, parsed.parameterizedSql());
            assertEquals(4, parsed.parameterCount());
            assertArrayEquals(expected, parsed.positionalParameterOffsets());
            assertArrayEquals(expected, QueryUtil.positionalParameterOffsets(sql));
            final int[] exposed = parsed.positionalParameterOffsets();
            exposed[0] = -1;
            assertArrayEquals(expected, parsed.positionalParameterOffsets());

            final String direct = "SELECT " + "?,".repeat(127) + "?";
            final int[] directExpected = java.util.stream.IntStream.range(0, 128).map(i -> 7 + 2 * i).toArray();
            assertArrayEquals(directExpected, ParsedSql.parse(direct, null).positionalParameterOffsets());
        }

        @Test
        void testParse_PositionalOffsetsExcludeExtraSourceQuestionMarks() {
            // Equal raw/classified counts are the shortcut's proof. An extra '?' must force alignment,
            // whether it is before or after the bindings and whether the binding list is sparse or dense.
            for (final int count : new int[] { 1, 64 }) {
                final String array = "ARRAY[" + String.join(",", java.util.Collections.nCopies(count, "?")) + "]";
                for (final String extra : List.of("'?'", "doc ? 'k'", "doc ?? 'k'", "?- line '{1,0,0}'", "[what?]")) {
                    for (final boolean before : new boolean[] { false, true }) {
                        final String sql = "SELECT " + (before ? extra + ", " + array : array + ", " + extra) + " FROM t /* ? */ WHERE id = ?";
                        final int start = sql.indexOf("ARRAY[");
                        final int[] expected = new int[count + 1];
                        for (int i = 0; i < count; i++) {
                            expected[i] = start + "ARRAY[".length() + 2 * i;
                        }
                        expected[count] = sql.lastIndexOf('?');
                        final ParsedSql parsed = ParsedSql.parse(sql);
                        assertEquals(count + 1, parsed.parameterCount(), sql);
                        assertArrayEquals(expected, parsed.positionalParameterOffsets(), sql);
                    }
                }
            }
        }

        @Test
        void testParse_PositionalOffsetsRetainSourceLocationsWithoutSpecialSyntax() {
            // The direct-offset shortcut must retain original spacing and the trimmed source's indexes.
            for (final String body : List.of("?", "? + ?", "?::int, ?", "ARRAY[? ,\t?], ?", "arr[?][?], ?")) {
                final String source = "SELECT " + body + " FROM t /* no binding here */ WHERE id = ?";
                final ParsedSql parsed = ParsedSql.parse("  " + source + "  ");
                final int[] expected = java.util.stream.IntStream.range(0, source.length()).filter(i -> source.charAt(i) == '?').toArray();
                assertEquals(expected.length, parsed.parameterCount(), source);
                assertArrayEquals(expected, parsed.positionalParameterOffsets(), source);
                final int[] returned = parsed.positionalParameterOffsets();
                returned[0] = -1;
                assertArrayEquals(expected, parsed.positionalParameterOffsets(), "Cached offsets must remain immutable");
            }
        }

        @Test
        void testParse_QuotePrefixesAndSparseOffsetFallbackAcrossSeveralGroups() {
            // Prefix-only quote checks must leave prefixed literals opaque. Extra literal markers force
            // the sparse ordinary/group positions to merge back into their original source order.
            final String sql = "SELECT ?, N'ARRAY[?]', ARRAY['?', ?], arr[?][?], '?' FROM t WHERE id = ?";
            final ParsedSql parsed = ParsedSql.parse(sql);
            final int arrayMarker = sql.indexOf(", ?]") + 2;
            final int chained = sql.indexOf("arr[");
            assertEquals(5, parsed.parameterCount());
            assertArrayEquals(new int[] { sql.indexOf('?'), arrayMarker, chained + 4, chained + 7, sql.lastIndexOf('?') }, parsed.positionalParameterOffsets());
            assertEquals(sql, parsed.parameterizedSql());
            for (final String marker : List.of(":id", "#{id}")) {
                final String namedSql = "SELECT N'ARRAY[" + marker + "]', ARRAY['" + marker + "', " + marker + "]";
                final ParsedSql named = ParsedSql.parse(namedSql);
                assertEquals(1, named.parameterCount(), namedSql);
                assertEquals(List.of("id"), named.namedParameters(), namedSql);
                assertEquals("SELECT N'ARRAY[" + marker + "]', ARRAY['" + marker + "', ?]", named.parameterizedSql(), namedSql);
            }
        }

        @Test
        void testParse_WindowFrameOffsetPlaceholderIsCounted() {
            for (final String unit : new String[] { "ROWS", "RANGE", "GROUPS" }) {
                final String sql = "SELECT sum(x) OVER (ORDER BY d " + unit + " ? PRECEDING) FROM t";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }

            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT sum(x) OVER (ORDER BY d RANGE ? PRECEDING) FROM t WHERE id = :id"));
        }

        @Test
        void testParse_WithMultipleQuestionMarks() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE age > ? AND status = ?");
            assertEquals("SELECT * FROM users WHERE age > ? AND status = ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        @Test
        void testParse_WithQuestionMarkParameter() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
            assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        @Test
        void testParse_withQuestionMarkParameters() {
            String sql = "SELECT * FROM users WHERE id = ? AND status = ?";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertNotNull(parsed);
            assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.originalSql());
            assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());
            assertTrue(parsed.namedParameters().isEmpty());
            assertEquals(2, parsed.parameterCount());
        }

        @Test
        void testParsedSql_parseJdbcPlaceholders() {
            ParsedSql ps3 = ParsedSql.parse("UPDATE users SET status = ? WHERE id = ?");
            assertEquals(2, ps3.parameterCount());
        }

        @Test
        void testPositionalParameterOffsets_verticalTabGluedToIdentifier_alignsInSlowPath() {
            // A vertical tab is Java whitespace but not tokenizer whitespace, so it stays inside the token
            // "\u000Bx"; the source walk must not skip it, or the token is never found (used to throw ISE).
            final String sql = "SELECT ?\u000Bx FROM t WHERE '?' = ?"; // the quoted '?' forces the source-alignment path
            final ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals(2, parsed.parameterCount());
            assertArrayEquals(new int[] { 7, 30 }, parsed.positionalParameterOffsets());

            // and the raw-offset fast path agrees for the same shape without the quoted marker
            final ParsedSql fast = ParsedSql.parse("SELECT ?\u000Bx FROM t WHERE a = ?");
            assertArrayEquals(new int[] { 7, 28 }, fast.positionalParameterOffsets());
        }

        @Test
        void testPositionalParameterOffsetsMatchCountedPlaceholders() {
            // The offsets point at exactly the '?' characters parameterCount() counted, in the ORIGINAL text:
            // not the JSON operator, not the '?' in a literal, a comment or a bracket-quoted identifier, but the
            // one inside an array subscript.
            final String sql = "SELECT arr[?] FROM t WHERE doc ? 'key' AND k = 'a?' AND [w?] = ? /* ? */ -- ?\n AND z = ?";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(3, parsed.parameterCount());

            final int[] offsets = parsed.positionalParameterOffsets();
            assertEquals(3, offsets.length);
            assertEquals(sql.indexOf("arr[?]") + 4, offsets[0]);
            assertEquals(sql.indexOf("[w?] = ?") + 7, offsets[1]);
            assertEquals(sql.lastIndexOf('?'), offsets[2]);

            for (final int offset : offsets) {
                assertEquals('?', sql.charAt(offset));
            }

            // Fresh array on every call; whitespace runs collapsed by the tokenizer do not shift the offsets.
            assertEquals(false, offsets == parsed.positionalParameterOffsets());
            final String spaced = "SELECT  *\tFROM t\n WHERE  a = ?  AND b = ?";
            final int[] spacedOffsets = ParsedSql.parse(spaced).positionalParameterOffsets();
            assertEquals(2, spacedOffsets.length);
            assertEquals(spaced.indexOf('?'), spacedOffsets[0]);
            assertEquals(spaced.lastIndexOf('?'), spacedOffsets[1]);

            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE name = :name").positionalParameterOffsets().length);
            assertEquals(0, ParsedSql.parse("SELECT [what?] FROM t").positionalParameterOffsets().length);
        }

        @Test
        void testPositionalParameterOffsetsSkipACommentSharingItsFirstCharacterWithTheNextToken() {
            // The comment openers "/*" and "--" start with the operator tokens "/" and "-", so a comment
            // immediately followed by such an operator must still be skipped while the token stream is walked back
            // onto the original text. Otherwise the operator matches the opener, the comment is never skipped, and
            // the offset of the real placeholder lands on the commented-out '?' instead.
            final String block = "SELECT 1 /* ? */ / ?";
            final ParsedSql blockParsed = ParsedSql.parse(block);
            assertEquals(1, blockParsed.parameterCount());
            assertArrayEquals(new int[] { block.lastIndexOf('?') }, blockParsed.positionalParameterOffsets());

            final String line = "SELECT 1 -- ?\n -?";
            final ParsedSql lineParsed = ParsedSql.parse(line);
            assertEquals(1, lineParsed.parameterCount());
            assertArrayEquals(new int[] { line.lastIndexOf('?') }, lineParsed.positionalParameterOffsets());

            final String hash = "SELECT 1 # ?\n -?";
            assertArrayEquals(new int[] { hash.lastIndexOf('?') }, ParsedSql.parse(hash).positionalParameterOffsets());

            // Several comments in a row, and a comment that is itself followed by another comment opener.
            final String chained = "SELECT ? /* ? */ /* ? */ / ? FROM t";
            final ParsedSql chainedParsed = ParsedSql.parse(chained);
            assertEquals(2, chainedParsed.parameterCount());
            assertArrayEquals(new int[] { chained.indexOf('?'), chained.lastIndexOf('?') }, chainedParsed.positionalParameterOffsets());

            // A "-" or "/" token that merely follows a comment-free expression is unaffected.
            final String plain = "SELECT 1 / ? - ?";
            assertArrayEquals(new int[] { plain.indexOf('?'), plain.lastIndexOf('?') }, ParsedSql.parse(plain).positionalParameterOffsets());
        }
    }

    @Nested
    class QuestionMarkOperators {
        @Test
        void testParse_JdbcEscapedQuestionOperatorsDoNotBind() {
            for (final String expression : List.of("doc ?? 'k'", "doc??'k'", "doc ??| keys", "doc ??& keys", "doc @?? path", "??- line '{1,0,0}'")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t";
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(0, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                    assertEquals(sql, parsed.parameterizedSql(), sql);

                    final String positional = sql + " WHERE id = ?";
                    final ParsedSql withBinding = ParsedSql.parse(positional);
                    assertEquals(1, withBinding.parameterCount(), positional);
                    assertArrayEquals(new int[] { positional.lastIndexOf('?') }, withBinding.positionalParameterOffsets(), positional);
                    assertEquals(positional, withBinding.parameterizedSql(), positional);

                    for (final String binding : List.of(":id", "#{id}")) {
                        final ParsedSql named = ParsedSql.parse(sql + " WHERE id = " + binding);
                        assertEquals(1, named.parameterCount(), sql);
                        assertEquals(List.of("id"), named.namedParameters(), sql);
                        assertEquals(positional, named.parameterizedSql(), sql);
                        assertArrayEquals(new int[0], named.positionalParameterOffsets(), sql);
                        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = ? AND other_id = " + binding), sql);
                    }
                }
            }
        }

        @Test
        void testParse_JdbcEscapesPreserveRealQuestionMarkOperands() {
            // The final question mark binds after an escaped operator; offsets must never identify its escape.
            for (final String expression : List.of("doc ???", "doc ?? ?", "doc ??|?", "doc ??&?", "doc @???", "doc ???? ?")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t";
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(1, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
                }
            }

            for (final String expression : List.of("? ?? ?", "? ??| ?", "? ??& ?")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression);
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(2, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
                }
            }
        }

        @Test
        void testParse_JdbcQuestionEscapesRequireAdjacency() {
            // The bracket lexer discards gaps, so source offsets must distinguish ? ? from the ?? escape.
            for (final String gap : List.of(" ", "\t", "\n", "/* gap */", "/* first *//* second */", " -- gap\n")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String expression = "doc ?" + gap + "?";
                    final String sql = "SELECT " + (grouped ? "ARRAY[" + expression + "]" : expression) + " FROM t";
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(1, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
                }
            }

            final String quoted = "SELECT '??', \"??\", [??], t.[??], ? /* ?? */";
            final ParsedSql parsed = ParsedSql.parse(quoted);
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { quoted.indexOf(", ?") + 2 }, parsed.positionalParameterOffsets());
        }

        @Test
        void testParse_JsonOperatorAcceptsNullAndClauseNamedOperands() {
            for (final String rightOperand : List.of("NULL", "null", "format", "absent", "t.format", "t.absent", "t.\"NULL\"", "format('%s', 'key')",
                    "pg_catalog.format('%s', 'key')")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String expression = "payload ? " + rightOperand;
                    final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(sql, parsed.parameterizedSql());
                    assertEquals(0, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);

                    // A named binding elsewhere must not turn the genuine operator into a mixed-style error.
                    for (final String binding : List.of(":id", "#{id}")) {
                        final ParsedSql withBinding = ParsedSql.parse(sql + " WHERE id = " + binding);
                        assertEquals(sql + " WHERE id = ?", withBinding.parameterizedSql());
                        assertEquals(List.of("id"), withBinding.namedParameters());
                        assertEquals(1, withBinding.parameterCount());
                        assertArrayEquals(new int[0], withBinding.positionalParameterOffsets());
                    }
                }
            }
        }

        @Test
        void testParse_JsonOperatorOnColumnsNamedOfOrSkipIsNotAPlaceholder() {
            // OF leads a placeholder only in AS OF; SKIP right after SELECT reads a following string literal as a JSON key.
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE of ? 'key'").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT skip ? 'key' FROM t").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT of ? 'key' FROM t").parameterCount());

            // So a named parameter elsewhere no longer trips the mixed-parameter-style check.
            final ParsedSql named = ParsedSql.parse("SELECT skip ? 'key' FROM t WHERE of ? 'k' AND id = :id");
            assertEquals(List.of("id"), named.namedParameters());
            assertEquals(1, named.parameterCount());

            // A prefixed string literal is a JSON key too, and so is any operand in a statement with named/MyBatis markers,
            // which cannot also carry positional bindings.
            assertEquals(0, ParsedSql.parse("SELECT skip ? E'key' FROM t").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT skip ? N'key' FROM t").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT skip ? U&'key' FROM t").parameterCount());
            for (final String sql : new String[] { "SELECT skip ? :key FROM t", "SELECT skip ? lower(:key) FROM t", "SELECT skip ? #{key} FROM t" }) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(List.of("key"), parsed.namedParameters(), sql);
                assertEquals(1, parsed.parameterCount(), sql);
            }
            assertEquals(List.of("id"), ParsedSql.parse("SELECT skip ? name FROM t WHERE id = :id").namedParameters());

            // Markers inside a subscript count too, whether in the JSON operand or elsewhere in the statement.
            for (final String sql : new String[] { "SELECT skip ? keys[:idx] FROM t", "SELECT skip ? keys[#{idx}] FROM t",
                    "SELECT skip ? name FROM t WHERE a = arr[:idx]", "SELECT skip ? name FROM t WHERE a = arr[1][#{idx}]" }) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(List.of("idx"), parsed.namedParameters(), sql);
                assertEquals(1, parsed.parameterCount(), sql);
            }

            // A cast or quoted text is not a marker, so the Firebird reading stays.
            assertEquals(1, ParsedSql.parse("SELECT SKIP ? id::int FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT SKIP ? id FROM t WHERE a = ':x'").parameterCount());

            // The keyword readings are kept.
            assertEquals(1, ParsedSql.parse("SELECT SKIP ? id FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT SKIP ? lower(name) FROM t").parameterCount());
            assertEquals(2, ParsedSql.parse("SELECT FIRST ? SKIP ? 'a' FROM t").parameterCount());
            assertEquals(2, ParsedSql.parse("SELECT * FROM emp FOR SYSTEM_TIME AS OF ? e WHERE e.id = ?").parameterCount());
        }

        @Test
        void testParse_JsonOperatorRetainsCompactRightHandPlaceholderOffset() {
            for (final String sql : List.of("SELECT payload ? ?||'suffix'", "SELECT ARRAY[payload ? ?||'suffix']")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(sql, parsed.parameterizedSql());
                assertEquals(1, parsed.parameterCount());
                assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets());
            }

            // The same words form an operand and implicit alias outside a SQL/JSON constructor.
            final String ordinarySelect = "SELECT payload ? format JSON FROM t";
            final ParsedSql parsed = ParsedSql.parse(ordinarySelect);
            assertEquals(ordinarySelect, parsed.parameterizedSql());
            assertEquals(0, parsed.parameterCount());
            assertArrayEquals(new int[0], parsed.positionalParameterOffsets());
        }

        @Test
        void testParse_JsonQuestionOperatorAfterSupplementaryIdentifier() {
            final String column = "\uD801\uDC00payload";
            final ParsedSql named = ParsedSql.parse("SELECT " + column + " ? :key FROM events");
            assertEquals("SELECT " + column + " ? ? FROM events", named.parameterizedSql());
            assertEquals(List.of("key"), named.namedParameters());
            assertEquals(1, named.parameterCount());

            final String sql = "SELECT " + column + " ? ? FROM events";
            final ParsedSql positional = ParsedSql.parse(sql);
            assertEquals(1, positional.parameterCount());
            assertArrayEquals(new int[] { sql.lastIndexOf('?') }, positional.positionalParameterOffsets());
            assertEquals(0, ParsedSql.parse("SELECT " + column + " ? 'key' FROM events").parameterCount());
        }

        @Test
        void testParse_KeywordLengthDispatchKeepsIdentifierOperands() {
            // An identifier sharing a keyword's length or prefix is still a JSON-operator operand.
            for (final String column : List.of("id", "key", "name", "order_id", "oncall", "intervals", "selection", "returning_value")) {
                final String sql = "SELECT doc ? " + column + " FROM t WHERE id = ?";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }
            for (final String sql : List.of("SELECT INTERVAL ? DAY", "SELECT * FROM t WHERE name ILIKE ? ESCAPE '!'", "SELECT * FROM t WHERE name SIMILAR TO ?",
                    "SELECT CURRENT_TIMESTAMP AT TIME ZONE ?")) {
                assertEquals(1, ParsedSql.parse(sql).parameterCount(), sql);
            }
        }

        // Regression: GROUPS/RANGE/ZONE/VALUES columns, and CASE ... END, before a JSON '?' operator made it count as a placeholder.
        @Test
        void testParse_keywordNamedColumnsAndCaseEndAreJsonOperatorOperands() {
            for (final String sql : List.of("SELECT * FROM users WHERE groups ? 'admin'", "SELECT * FROM users WHERE range ? 'k'",
                    "SELECT * FROM users WHERE zone ? 'k'", "SELECT * FROM users WHERE values ? 'k'", "SELECT CASE WHEN a THEN j1 ELSE j2 END ? 'k' FROM t")) {
                assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
            }

            assertEquals(List.of("id"), ParsedSql.parse("SELECT * FROM users WHERE groups ? 'admin' AND id = :id").namedParameters());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT * FROM users WHERE zone ? 'k' AND id = :id").namedParameters());

            // In their keyword positions they still lead a placeholder.
            assertEquals(1, ParsedSql.parse("SELECT sum(x) OVER (ORDER BY d RANGE ? PRECEDING) FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT sum(x) OVER (ORDER BY d GROUPS ? FOLLOWING) FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT ts AT TIME ZONE ? local_ts FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT CASE WHEN a THEN ? END FROM t").parameterCount());
            assertEquals(2, ParsedSql.parse("SELECT * FROM t WHERE a IN (VALUES ?, ?)").parameterCount());
        }

        @Test
        void testParse_PlaceholderAfterOperatorOrValueTakingKeywordIsNotJsonOperator() {
            // Regression: a "?" preceded by a value-taking keyword (INTERVAL, ILIKE, SIMILAR TO, ...) or by a
            // multi-character operator ("||", "->>") and followed by an identifier-like word (DAY, ESCAPE, an
            // alias) was misclassified as the PostgreSQL JSON "?" operator and dropped from parameterCount().
            ParsedSql mysql = ParsedSql.parse("SELECT * FROM t WHERE d > DATE_SUB(NOW(), INTERVAL ? DAY) AND id = ?");
            assertEquals("SELECT * FROM t WHERE d > DATE_SUB(NOW(), INTERVAL ? DAY) AND id = ?", mysql.parameterizedSql());
            assertEquals(2, mysql.parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE d > NOW() - INTERVAL ? DAY").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE name ILIKE ? ESCAPE '!'").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE name ILIKE ? COLLATE \"C\"").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE name SIMILAR TO ? ESCAPE '!'").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT a || ? b FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT payload ->> ? val FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT ts AT TIME ZONE ? local_ts FROM t").parameterCount());

            // A genuine JSON existence operator is still not a parameter.
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE payload ? 'active'").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE p1 ? day").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE x::jsonb ? 'k'").parameterCount());
        }

        @Test
        void testParse_PositionalPaginationAndOrderingParametersAreNotJsonOperators() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM events ORDER BY ? ASC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

            assertEquals(3, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        @Test
        void testParse_PostgresJsonQuestionAndOperatorWithNamedParameters() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ?& array[:keys] AND id = :id");
            assertEquals("SELECT * FROM events WHERE payload ?& array[?] AND id = ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(Arrays.asList("keys", "id"), parsed.namedParameters());
        }

        @Test
        void testParse_PostgresJsonQuestionOperatorDoesNotCountAsJdbcParameter() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? 'active'");
            assertEquals("SELECT * FROM events WHERE payload ? 'active'", parsed.parameterizedSql());
            assertEquals(0, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        @Test
        void testParse_PostgresJsonQuestionOperatorWithFunctionRhs() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? lower(:key) AND id = :id");
            assertEquals("SELECT * FROM events WHERE payload ? lower(?) AND id = ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(Arrays.asList("key", "id"), parsed.namedParameters());

            parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? CAST(:key AS text) AND id = :id");
            assertEquals("SELECT * FROM events WHERE payload ? CAST(? AS text) AND id = ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(Arrays.asList("key", "id"), parsed.namedParameters());
        }

        @Test
        void testParse_PostgresJsonQuestionOperatorWithJdbcParameterRhs() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? ? AND tenant_id = ?");
            assertEquals("SELECT * FROM events WHERE payload ? ? AND tenant_id = ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        @Test
        void testParse_PostgresJsonQuestionOperatorWithNamedParameterRhs() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM events WHERE payload ? :key AND id = :id");
            assertEquals("SELECT * FROM events WHERE payload ? ? AND id = ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals(Arrays.asList("key", "id"), parsed.namedParameters());
        }

        @Test
        void testParse_QuestionOperatorLookaheadRespectsQuotedMarkersAndSplitBindings() {
            final String quotedMarker = "SELECT ARRAY['#{'], ? FROM t";
            final ParsedSql quoted = ParsedSql.parse(quotedMarker);
            assertEquals(quotedMarker, quoted.parameterizedSql());
            assertEquals(1, quoted.parameterCount());
            assertArrayEquals(new int[] { quotedMarker.indexOf('?') }, quoted.positionalParameterOffsets());
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY['#{'], ?, :id FROM t"));

            for (final String binding : List.of("#{ segment }", "#{ segment, typeHandler=handler(foo) }", "#{ segment, typeHandler=handler( }",
                    "#{ segment, typeHandler=handler) }")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String prefix = grouped ? "SELECT ARRAY[" : "SELECT ";
                    final String suffix = grouped ? "]" : "";
                    final ParsedSql geometry = ParsedSql.parse(prefix + "?- " + binding + "::lseg" + suffix);
                    assertEquals(prefix + "?- ?::lseg" + suffix, geometry.parameterizedSql());
                    assertEquals(1, geometry.parameterCount());
                    assertEquals(List.of("segment"), geometry.namedParameters());
                    assertArrayEquals(new int[0], geometry.positionalParameterOffsets());

                    // MyBatis option text is opaque metadata: even parentheses in it are not SQL grouping.
                    final ParsedSql json = ParsedSql.parse(prefix + "JSON_ARRAY(" + binding + " ? NULL)" + suffix);
                    assertEquals(prefix + "JSON_ARRAY(? ? NULL)" + suffix, json.parameterizedSql());
                    assertEquals(1, json.parameterCount());
                    assertEquals(List.of("segment"), json.namedParameters());
                    assertArrayEquals(new int[0], json.positionalParameterOffsets());
                }
            }

            for (final String sql : List.of("SELECT ?- lseg E'[(0,0),(1,0)]'", "SELECT ARRAY[?- lseg E'[(0,0),(1,0)]']")) {
                final ParsedSql typedLiteral = ParsedSql.parse(sql);
                assertEquals(sql, typedLiteral.parameterizedSql());
                assertEquals(0, typedLiteral.parameterCount());
                assertArrayEquals(new int[0], typedLiteral.positionalParameterOffsets());
            }
        }
    }

    @Nested
    class SqlJsonExpressions {
        @Test
        void testNestedJsonConstructorsRetainPlaceholderAndOperatorScopes() {
            final String sql = "SELECT " + "JSON_OBJECT(KEY ? VALUE JSON_ARRAY(".repeat(9) + "doc ? 'key', ?" + "))".repeat(9) + " FROM records";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(10, parsed.parameterCount());
            assertEquals(List.of(), parsed.namedParameters());
            assertEquals(sql, parsed.parameterizedSql());
            final int[] expected = new int[10];
            int from = 0;
            for (int i = 0; i < 9; i++) {
                expected[i] = sql.indexOf("KEY ?", from) + 4;
                from = expected[i] + 1;
            }
            expected[9] = sql.indexOf(", ?", from) + 2;
            assertArrayEquals(expected, parsed.positionalParameterOffsets());
        }

        @Test
        void testParse_JsonFormatFunctionPreservesOperandBindings() {
            for (final boolean grouped : new boolean[] { false, true }) {
                final String prefix = grouped ? "SELECT ARRAY[" : "SELECT ";
                final String suffix = grouped ? "] FROM t" : " FROM t";

                for (final String expression : List.of(":payload ? format('%s', :key)", "#{payload} ? format('%s', #{key})")) {
                    final ParsedSql parsed = ParsedSql.parse(prefix + expression + suffix);
                    assertEquals(prefix + "? ? format('%s', ?)" + suffix, parsed.parameterizedSql());
                    assertEquals(List.of("payload", "key"), parsed.namedParameters());
                    assertEquals(2, parsed.parameterCount());
                    assertArrayEquals(new int[0], parsed.positionalParameterOffsets());
                }

                final String positionalSql = prefix + "payload ? format('%s', ?)" + suffix;
                final ParsedSql positional = ParsedSql.parse(positionalSql);
                assertEquals(1, positional.parameterCount());
                assertArrayEquals(new int[] { positionalSql.lastIndexOf('?') }, positional.positionalParameterOffsets());
            }
        }

        // Regression: a compact operator after a JSON_OBJECT KEY placeholder hid the key binding, and a column named value after '?' became a placeholder.
        @Test
        void testParse_jsonObjectKeyLeadsPlaceholderAndValueColumnStaysOperand() {
            for (final String sql : List.of("SELECT JSON_OBJECT(KEY ?||'_x' VALUE ?) FROM t", "SELECT JSON_OBJECT(KEY ?-1 VALUE ?) FROM t",
                    "SELECT JSON_OBJECT('a' VALUE 1, KEY ? VALUE ?) FROM t", "SELECT JSON_OBJECT(KEY ? VALUE ? ABSENT ON NULL) FROM t",
                    "SELECT ARRAY[JSON_OBJECT(KEY ?||'_x' VALUE ?)] FROM t")) {
                assertEquals(2, ParsedSql.parse(sql).parameterCount(), sql);
            }

            final ParsedSql parsed = ParsedSql.parse("SELECT ARRAY[JSON_OBJECT(KEY ? VALUE ?)] FROM t");
            assertEquals(2, parsed.parameterCount());
            assertArrayEquals(new int[] { 29, 37 }, parsed.positionalParameterOffsets());
            assertEquals(1, ParsedSql.parse("SELECT JSON_OBJECTAGG(KEY ? VALUE col) FROM t").parameterCount());

            // A column named value or key keeps the JSON-operator reading.
            for (final String sql : List.of("SELECT JSON_OBJECT('k' VALUE doc ? value) FROM t", "SELECT JSON_ARRAY(doc ? value) FROM t",
                    "SELECT JSON_ARRAYAGG(doc ? value) FROM t", "SELECT JSON_ARRAY(key ? 'x') FROM t", "SELECT JSON_OBJECT(KEY 'k' VALUE key ? 'x') FROM t",
                    "SELECT * FROM t WHERE key ? 'x'")) {
                assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
            }
        }

        // Regression: in JSON_OBJECT(KEY ? VALUE ...) the key placeholder was read as a JSON '?' operator.
        @Test
        void testParse_jsonObjectKeyValuePlaceholders() {
            final ParsedSql parsed = ParsedSql.parse("SELECT JSON_OBJECT(KEY ? VALUE ?) FROM t");
            assertEquals(2, parsed.parameterCount());
            assertArrayEquals(new int[] { 23, 31 }, parsed.positionalParameterOffsets());
            assertEquals(1, ParsedSql.parse("SELECT JSON_OBJECTAGG(KEY ? VALUE col) FROM t").parameterCount());

            // Outside a constructor, a column named value keeps the JSON-operator reading.
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE payload ? value").parameterCount());
        }

        @Test
        void testParse_ParenthesizedJsonArrayQueryKeepsQueryScope() {
            for (final String query : List.of("(SELECT 1) UNION SELECT 1 FROM t JOIN u JOIN v ON v.payload ? NULL ON NULL",
                    "((SELECT 1)) UNION SELECT 1 FROM t JOIN u JOIN v ON v.payload ? NULL ON NULL")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String expression = "JSON_ARRAY(" + query + ")";
                    final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(0, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
                    assertEquals(List.of("other"), ParsedSql.parse(sql + ", :other").namedParameters(), sql);
                }
            }

            // A scalar subquery can instead be the first value in a constructor's value list.
            final String values = "SELECT JSON_ARRAY((SELECT 1), ? NULL ON NULL)";
            assertEquals(1, ParsedSql.parse(values).parameterCount());
            assertArrayEquals(new int[] { values.indexOf('?') }, ParsedSql.parse(values).positionalParameterOffsets());
        }

        @Test
        void testParse_SqlJsonClauseKeywordsKeepPlaceholdersCountable() {
            // Inside a SQL/JSON constructor, NULL ON NULL, ABSENT ON NULL and FORMAT JSON begin clauses
            // after the value. Their word-shaped tokens must not turn that value's marker into an operator.
            for (final String clause : List.of("NULL ON NULL", "ABSENT ON NULL", "FORMAT JSON")) {
                final String bare = "SELECT JSON_OBJECT('k' VALUE ? " + clause + ") FROM t";
                assertEquals(1, ParsedSql.parse(bare).parameterCount(), bare);

                final String grouped = "SELECT ARRAY[JSON_OBJECT('k' VALUE ? " + clause + ")]";
                assertEquals(1, ParsedSql.parse(grouped).parameterCount(), grouped);
            }

            assertEquals(1, ParsedSql.parse("SELECT JSON_OBJECT(KEY 'k' VALUE ? ABSENT ON NULL) FROM t").parameterCount());
            assertEquals(List.of("v"), ParsedSql.parse("SELECT JSON_OBJECT('k' VALUE :v NULL ON NULL) FROM t").namedParameters());

            // The marker is a real parameter, so a named binding beside it is a genuine mix of styles.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT JSON_OBJECT('k' VALUE ? NULL ON NULL), :other FROM t"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[JSON_OBJECT('k' VALUE ? NULL ON NULL), :other]"));

            // Outside the clause position, a column named like a clause keyword still carries the genuine operator.
            for (final String column : List.of("payload", "value", "format")) {
                assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE " + column + " ? 'key'").parameterCount(), column);
            }
        }

        @Test
        void testParse_SqlJsonClausePlaceholdersSurviveCommentsCaseAndNesting() {
            for (final String clause : List.of("null on null", "absent on null", "format json", "NULL /* gap */ ON NULL", "ABSENT -- gap\n ON NULL",
                    "FORMAT /* gap */ JSON")) {
                for (final String expression : List.of("json_object('k' value ? " + clause + ")",
                        "json_array(json_object('outer' value json_object('inner' value ? " + clause + ")))")) {
                    for (final boolean grouped : new boolean[] { false, true }) {
                        final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                        final ParsedSql parsed = ParsedSql.parse(sql);
                        assertEquals(1, parsed.parameterCount(), sql);
                        assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
                        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = :id"), sql);
                        assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE id = #{id}"), sql);
                    }
                }
            }
        }

        @Test
        void testParse_SqlJsonQueryBodiesDoNotUseConstructorValueClauses() {
            // FORMAT is the right operand and JSON its output alias in these SELECT bodies, not an option.
            for (final String query : List.of("SELECT payload ? format JSON FROM t", "select payload ? FORMAT json from t",
                    "/* query */ SELECT payload ? format JSON FROM t",
                    "WITH source AS (SELECT payload, format FROM t) SELECT payload ? format JSON FROM source", "(SELECT payload ? format JSON FROM t)",
                    "SELECT payload ? /* operand */ format JSON FROM t")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String expression = "JSON_ARRAY(" + query + ")";
                    final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(0, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);

                    for (final String binding : List.of(":id", "#{id}")) {
                        final ParsedSql named = ParsedSql.parse(sql + " WHERE id = " + binding);
                        assertEquals(1, named.parameterCount(), sql);
                        assertEquals(List.of("id"), named.namedParameters(), sql);
                        assertArrayEquals(new int[0], named.positionalParameterOffsets(), sql);
                    }
                }
            }

            for (final String binding : List.of(":id", "#{id}")) {
                final String sql = "SELECT JSON_ARRAY(SELECT payload ? format JSON FROM t WHERE id = " + binding + ")";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertEquals(List.of("id"), parsed.namedParameters(), sql);
                assertEquals(sql.replace(binding, "?"), parsed.parameterizedSql(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
            }

            for (final String sql : List.of("SELECT JSON_ARRAY(SELECT payload ? format JSON FROM t WHERE id = ?)",
                    "SELECT JSON_ARRAY(SELECT JSON_OBJECT('k' VALUE ? FORMAT JSON) FROM t)", "SELECT JSON_ARRAY(SELECT JSON_ARRAY(? NULL ON NULL) FROM t)",
                    "SELECT JSON_OBJECT('query' VALUE JSON_ARRAY(SELECT payload ? format JSON FROM t), 'binding' VALUE ? FORMAT JSON)",
                    "SELECT JSON_ARRAY(JSON_ARRAY(SELECT payload ? format JSON FROM t), ? FORMAT JSON)")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + " WHERE other = :other"), sql);
            }
        }

        @Test
        void testParse_SqlJsonUniquenessClausesKeepRealBindings() {
            for (final String clause : List.of("WITH UNIQUE KEYS", "WITHOUT UNIQUE KEYS", "WITH UNIQUE", "without /* gap */ unique keys")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String expression = "JSON_OBJECT('k' VALUE ? " + clause + ")";
                    final String sql = grouped ? "SELECT ARRAY[" + expression + "]" : "SELECT " + expression;
                    final ParsedSql parsed = ParsedSql.parse(sql);
                    assertEquals(1, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { sql.indexOf('?') }, parsed.positionalParameterOffsets(), sql);
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + ", :other"), sql);
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql + ", #{other}"), sql);
                }
            }
        }

        @Test
        void testValuesColumnAfterOperatorsAndReturningKeepsJsonOperatorsUnbound() {
            for (final String sql : List.of("SELECT 1 = values ? 'key'", "SELECT 1 + values ? 'key'", "UPDATE t SET id = 1 RETURNING values ? 'key'",
                    "SELECT * FROM t JOIN u ON values ? 'key'", "SELECT * FROM t WHERE id = 1 OR values ? 'key'")) {
                assertOffsets(sql);
            }
            assertOffsets("SELECT * FROM VALUES ? COLLATE \"C\"", 21);
            assertOffsets("SELECT * FROM VALUES ? AT TIME ZONE 'UTC'", 21);
        }

        // Regression: the VALUES row-constructor fallback counted JSON operators as bindings after VALUE, ':', a
        // qualification dot or IS [NOT] DISTINCT FROM, rejecting valid named bindings in the same query as mixed styles.
        @Test
        void testValuesColumnInJsonAndQualifiedExpressions() {
            for (final String sql : new String[] { "SELECT JSON_OBJECT('present' VALUE values ? 'k') FROM t",
                    "SELECT JSON_OBJECT('present': values ? 'k') FROM t", "SELECT JSON_OBJECTAGG('present' VALUE values ? 'k') FROM t",
                    "SELECT JSON_OBJECT(KEY values ? 'k' VALUE 1) FROM t", "SELECT JSON_OBJECT('present' VALUE /* operand */ values ? 'k') FROM t",
                    "SELECT JSON_OBJECT('present' VALUE values ?| array['k']) FROM t", "SELECT JSON_OBJECT('present': values ?& array['k']) FROM t",
                    "SELECT JSON_OBJECT('nested' VALUE JSON_OBJECT('present' VALUE values ? 'k')) FROM t", "SELECT t . values ? 'k' FROM t",
                    "SELECT t. values ? 'k' FROM t", "SELECT \"t\". values ? 'k' FROM t", "SELECT t /* qualifier */ . /* column */ values ? 'k' FROM t",
                    "SELECT t . values ?| array['k'] FROM t", "SELECT t. values ?& array['k'] FROM t", "SELECT true IS DISTINCT FROM values ? 'k' FROM t",
                    "SELECT true IS NOT DISTINCT FROM values ? 'k' FROM t",
                    "SELECT true IS /* predicate */ DISTINCT /* operand */ FROM values ? 'k' FROM t" }) {
                assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
                assertEquals(1, ParsedSql.parse(sql + " WHERE id = ?").parameterCount(), sql);
                assertEquals(List.of("id"), ParsedSql.parse(sql + " WHERE id = :id").namedParameters(), sql);
                assertEquals(List.of("id"), ParsedSql.parse(sql + " WHERE id = #{id}").namedParameters(), sql);
            }

            assertEquals(List.of("key", "id"), ParsedSql.parse("SELECT JSON_OBJECT('present' VALUE values ? :key) FROM t WHERE id = :id").namedParameters());
            assertEquals(List.of("keys"), ParsedSql.parse("SELECT t. values ?| #{keys} FROM t").namedParameters());
        }

        // Regression: VALUES stopped leading a bare '?' (so "values ? 'k'" tests a jsonb column), which also read the binding in
        // H2's "VALUES ? FORMAT JSON" as a JSON operator: parameterCount() returned 0 and SubQuery rejected the binding.
        @Test
        void testValuesMarkerBeforePostfixClauseIsAPlaceholder() {
            for (final String sql : new String[] { "VALUES ? FORMAT JSON", "SELECT * FROM (VALUES ? FORMAT JSON) AS t(j)", "INSERT INTO t VALUES ? FORMAT JSON",
                    "SELECT 1 UNION VALUES ? FORMAT JSON", "VALUES ? FORMAT /* c */ JSON", "VALUES ? COLLATE \"C\"", "VALUES ? AT TIME ZONE 'UTC'" }) {
                assertEquals(1, ParsedSql.parse(sql).parameterCount(), sql);
            }

            assertEquals(2, ParsedSql.parse("VALUES ? FORMAT JSON, ? FORMAT JSON").parameterCount());

            // VALUES after a ')', INSERT or the table of INSERT INTO is the row constructor too.
            for (final String sql : new String[] { "INSERT INTO t (a) VALUES ? FORMAT JSON", "INSERT INTO s.t VALUES ? FORMAT JSON",
                    "INSERT INTO \"s\".\"t\" VALUES ? FORMAT JSON", "INSERT INTO s . t VALUES ? FORMAT JSON",
                    "WITH x AS (VALUES ? FORMAT JSON) SELECT * FROM x",
                    "MERGE INTO t USING s ON t.id = s.id WHEN NOT MATCHED THEN INSERT VALUES ? FORMAT JSON" }) {
                assertEquals(1, ParsedSql.parse(sql).parameterCount(), sql);
            }

            // A set operation's ALL/DISTINCT, EXPLAIN (ANALYZE) and an identity override also introduce a row constructor.
            for (final String sql : new String[] { "SELECT CAST('[1]' AS JSON) UNION ALL VALUES ? FORMAT JSON",
                    "SELECT CAST('[1]' AS JSON) UNION DISTINCT VALUES ? FORMAT JSON", "SELECT 1 EXCEPT ALL VALUES ? FORMAT JSON",
                    "EXPLAIN VALUES ? FORMAT JSON", "EXPLAIN ANALYZE VALUES ? FORMAT JSON", "INSERT INTO t OVERRIDING SYSTEM VALUE VALUES ? FORMAT JSON",
                    "INSERT INTO t (j) OVERRIDING USER VALUE VALUES ? FORMAT JSON", "SELECT * FROM t WHERE x = ALL (VALUES ? FORMAT JSON)" }) {
                assertEquals(1, ParsedSql.parse(sql).parameterCount(), sql);
            }

            // A column named values still takes the JSON existence operator, also before words that read like a postfix clause
            // (PostgreSQL: values ? format tests the key in column format, and JSON is a bare alias).
            for (final String sql : new String[] { "SELECT * FROM t WHERE values ? 'k'", "SELECT * FROM t WHERE (values ? 'k')",
                    "SELECT DISTINCT values ? 'k' FROM t", "SELECT * FROM t WHERE values ? format", "SELECT values ? format JSON FROM t",
                    "SELECT a, values ? format JSON FROM t", "SELECT ALL values ? 'k' FROM t", "SELECT count(DISTINCT values ? 'k') FROM t",
                    "SELECT * FROM t WHERE a = 1 AND values ? 'k'", "SELECT CASE WHEN values ? 'k' THEN 1 END FROM t",
                    "SELECT * FROM t ORDER BY values ? 'k'" }) {
                assertEquals(0, ParsedSql.parse(sql).parameterCount(), sql);
            }

            assertEquals(List.of("id"), ParsedSql.parse("SELECT values ? format JSON FROM t WHERE id = :id").namedParameters());
        }

        @Test
        void testWindowFrameOffsetsRetainBindingsThroughNestedArithmetic() {
            for (final String expression : List.of("? * (2 + 1) PRECEDING", "? + values[1] FOLLOWING", "? * (2 + (3 - 1)) PRECEDING")) {
                final String sql = "SELECT sum(n) OVER (ORDER BY id RANGE " + expression + ") FROM t";
                assertOffsets(sql, sql.indexOf('?'));
            }
        }
    }

    @Nested
    class SqlNormalization {
        // Regression: a block comment that opens on a "//" line and closes on a later line mid-statement was dropped,
        // losing the lines H2 reads as SQL in between ("AND id = :id" below) while the kept line break let the rest run.
        @Test
        void testParse_blockCommentOpenedOnDoubleSlashLineIsKeptAsWritten() {
            // The gap is now kept as written, so H2 reads exactly the source there and the unconverted ":id" fails loudly.
            ParsedSql parsed = ParsedSql.parse("DELETE FROM t WHERE tenant_id = :t\n// AND s = 'x' /* f\nAND id = :id\n// */\nAND x = :x");
            assertEquals("DELETE FROM t WHERE tenant_id = ? // AND s = 'x' /* f\nAND id = :id\n// */\nAND x = ?", parsed.parameterizedSql());
            assertEquals(List.of("t", "x"), parsed.namedParameters());
            assertEquals(2, parsed.parameterCount());
            assertEquals("DELETE FROM t WHERE tenant_id = 7 // AND s = 'x' /* f\nAND id = 1\n// */\nAND x = 0",
                    ParsedSql.parse("DELETE FROM t WHERE tenant_id = 7\n// AND s = 'x' /* f\nAND id = 1\n// */\nAND x = 0").parameterizedSql());
            parsed = ParsedSql.parse("UPDATE t SET a = :a WHERE tenant_id = :t // scope /* old\nAND id = :id -- */\nAND x = :x");
            assertEquals("UPDATE t SET a = ? WHERE tenant_id = ? // scope /* old\nAND id = :id -- */\nAND x = ?", parsed.parameterizedSql());
            assertEquals(List.of("a", "t", "x"), parsed.namedParameters());
            // The kept gap leaves the line state set: a "//" on the comment's last line still keeps the next line break.
            assertEquals("SELECT a FROM t WHERE x = ? // c /* d\n// e */ y\nAND z = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a // c /* d\n// e */ y\nAND z = 2").parameterizedSql());
            // Controls: a comment that closes on the "//" line, or opens on a later line (a real comment on H2 too), and a
            // "/*" inside a "--" comment that the line break ends, still leave one line break.
            assertEquals("SELECT a FROM t WHERE x = ? // c\nAND y = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a // c /* d */\nAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? // c\nAND y = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a // c\n/* d\n*/ AND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? // c\nAND y = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a // c -- /* d\nAND y = 2").parameterizedSql());
            // Without "//" such a comment is dropped as before.
            assertEquals("SELECT a FROM t WHERE x = ? AND z = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a /* d\nAND y = :y */\nAND z = 2").parameterizedSql());
        }

        @Test
        void testParse_CommentOnlySqlHasNoOperationPrefix() {
            ParsedSql parsed = ParsedSql.parse("/* comment only */");

            assertEquals("/* comment only */", parsed.parameterizedSql());
            assertEquals(0, parsed.parameterCount());
        }

        // The line-break rule for "//" must leave every other statement byte-identical to the collapsed form.
        @Test
        void testParse_doubleSlashOnlyInQuotesOrCommentsOrAbsentKeepsCollapsedForm() {
            // Inside a string literal or quoted identifier "//" is data, on H2 as everywhere else.
            assertEquals("SELECT a FROM t WHERE x = 'http://h/p' AND y = ?",
                    ParsedSql.parse("SELECT a FROM t WHERE x = 'http://h/p'\nAND y = :a").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = E'a//b' AND y = ?", ParsedSql.parse("SELECT a FROM t WHERE x = E'a//b'\nAND y = :a").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = N'a//b' AND y = ?", ParsedSql.parse("SELECT a FROM t WHERE x = N'a//b'\nAND y = :a").parameterizedSql());
            assertEquals("SELECT \"a//b\" FROM t WHERE y = ?", ParsedSql.parse("SELECT \"a//b\"\nFROM t\nWHERE y = :a").parameterizedSql());
            assertEquals("SELECT `a//b` FROM t WHERE y = ?", ParsedSql.parse("SELECT `a//b`\nFROM t\nWHERE y = :a").parameterizedSql());

            // In a comment the tokenizer drops, or in a block comment kept as a token.
            assertEquals("SELECT a FROM t WHERE x = ? AND y = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a -- see http://h\nAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? AND y = 2", ParsedSql.parse("SELECT a FROM t WHERE x = :a /* http://h */\nAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t /* http://h */ WHERE x = ?",
                    ParsedSql.parse("-- Keep comments\nSELECT a FROM t /* http://h */\nWHERE x = ?").parameterizedSql());

            // Without "//": a single '/' and "/ /" are division, and a "//" on one line only does not touch later lines.
            assertEquals("SELECT a / 2 FROM t WHERE x = ? AND y = 2", ParsedSql.parse("SELECT a / 2\nFROM t\nWHERE x = :a\nAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = 4 / /2 AND y = ?", ParsedSql.parse("SELECT a FROM t WHERE x = 4 / /2\nAND y = :a").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = 'a//b' AND y = ? AND z = 3",
                    ParsedSql.parse("SELECT a FROM t WHERE x = 'a//b'\nAND y = :a\nAND z = 3").parameterizedSql());

            // A trailing "//" comment is stripped of its trailing whitespace as before.
            assertEquals("SELECT a FROM t WHERE x = ? // c", ParsedSql.parse("SELECT a FROM t WHERE x = :a // c\n;").parameterizedSql());

            // Without "//" on its line an unterminated "/*" is dropped with the rest, as before.
            assertEquals("SELECT a FROM t WHERE x = ?", ParsedSql.parse("SELECT a FROM t WHERE x = :a /* b\nAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? // c\nAND y = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a // c\nAND y = 2 /* d\nAND z = 3").parameterizedSql());
        }

        // End to end: the NAMED_SQL / IBATIS_SQL output of an H2 builder, converted to JDBC by ParsedSql, keeps the line break
        // that the builder put after the "//" comment.
        @Test
        void testParse_h2BuilderOutputKeepsLineBreakAfterDoubleSlashComment() {
            for (final Dsl base : new Dsl[] { Dsl.NSC, Dsl.MSC }) {
                final Dsl h2 = Dsl.forDialect(base.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("H2")).build());

                final AbstractQueryBuilder.SP update = h2.update("t").set("a", 1).where(Filters.expr("tenant_id = 7 // scope\nAND id = 42")).build();
                ParsedSql parsed = ParsedSql.parse(update.query());
                assertEquals("UPDATE t SET a = ? WHERE tenant_id = 7 // scope\nAND id = 42", parsed.parameterizedSql(), update.query());
                assertEquals(List.of("a"), parsed.namedParameters());
                assertEquals(update.parameters().size(), parsed.parameterCount());

                final AbstractQueryBuilder.SP delete = h2.deleteFrom("t // x").where(Filters.eq("id", 2)).build();
                parsed = ParsedSql.parse(delete.query());
                assertEquals("DELETE FROM t // x\nWHERE id = ?", parsed.parameterizedSql(), delete.query());
                assertEquals(List.of("id"), parsed.namedParameters());

                // Nothing to convert: the "/*" inside the "//" comment no longer drops the next line.
                final AbstractQueryBuilder.SP raw = h2.deleteFrom("t").where(Filters.expr("x = 1 // a /* b\nAND y = 2")).build();
                assertTrue(raw.query().endsWith("x = 1 // a /* b\nAND y = 2"), raw.query());
                assertEquals(raw.query(), ParsedSql.parse(raw.query()).parameterizedSql());
            }
        }

        // Regression: rebuilding from tokens collapsed the line break between adjacent string literals, which PostgreSQL then rejects.
        @Test
        void testParse_lineBreakBetweenAdjacentStringLiteralsIsKept() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE msg = 'hello '\n  'world' AND id = ?");
            assertEquals("SELECT * FROM t WHERE msg = 'hello '\n'world' AND id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());

            assertEquals("SELECT * FROM t WHERE msg = 'a'\n'b' AND id = ?",
                    ParsedSql.parse("SELECT * FROM t WHERE msg = 'a' -- c\n'b' AND id = ?").parameterizedSql());
            parsed = ParsedSql.parse("SELECT * FROM t WHERE msg = 'a'\r\n'b' AND id = :id");
            assertEquals("SELECT * FROM t WHERE msg = 'a'\n'b' AND id = ?", parsed.parameterizedSql());
            assertEquals(List.of("id"), parsed.namedParameters());

            // Without a line break between the literals, or between other tokens, whitespace still collapses to one space.
            assertEquals("SELECT * FROM t WHERE msg = 'a' 'b' AND id = ?",
                    ParsedSql.parse("SELECT * FROM t WHERE msg = 'a'   'b' AND id = ?").parameterizedSql());
            assertEquals("SELECT a, 'x' FROM t WHERE id = ?", ParsedSql.parse("SELECT a,\n  'x'\nFROM t\nWHERE id = ?").parameterizedSql());
        }

        // Regression: the line break between adjacent string literals (CR, chains, prefixed literals, comments in the gap).
        @Test
        void testParse_lineBreakBetweenAdjacentStringLiteralsVariants() {
            assertEquals("SELECT 'a'\n'b' FROM t WHERE id = ?", ParsedSql.parse("SELECT 'a'\r'b' FROM t WHERE id = ?").parameterizedSql());
            assertEquals("SELECT 'a'\n'b'\n'c' FROM t WHERE id = ?", ParsedSql.parse("SELECT 'a'\n'b'\n'c' FROM t WHERE id = ?").parameterizedSql());
            assertEquals("SELECT E'a\\n'\n'b' FROM t WHERE id = ?", ParsedSql.parse("SELECT E'a\\n'\n'b' FROM t WHERE id = ?").parameterizedSql());
            assertEquals("SELECT N'a'\n'b' FROM t WHERE id = ?", ParsedSql.parse("SELECT N'a'\n'b' FROM t WHERE id = ?").parameterizedSql());
            assertEquals("SELECT X'0F'\n'FF' FROM t WHERE id = ?", ParsedSql.parse("SELECT X'0F'\n'FF' FROM t WHERE id = ?").parameterizedSql());

            // A prefixed second literal is not a continuation, so the gap collapses.
            assertEquals("SELECT 'a' N'b' FROM t WHERE id = ?", ParsedSql.parse("SELECT 'a'\nN'b' FROM t WHERE id = ?").parameterizedSql());

            // Line breaks elsewhere do not affect a gap without one.
            assertEquals("SELECT 'a' 'b' FROM t WHERE id = ?", ParsedSql.parse("SELECT 'a' 'b'\nFROM t WHERE id = ?").parameterizedSql());
            assertEquals("SELECT 'a' 'b'\n'c' FROM t WHERE id = ?", ParsedSql.parse("SELECT 'a' 'b'\n'c' FROM t WHERE id = ?").parameterizedSql());

            // Comments in the gap.
            assertEquals("SELECT 'a'\n'b' FROM t WHERE id = ?", ParsedSql.parse("SELECT 'a' /* c */\n'b' FROM t WHERE id = ?").parameterizedSql());
            assertEquals("SELECT 'a'\n'b' FROM t WHERE id = ?", ParsedSql.parse("SELECT 'a' # c\n'b' FROM t WHERE id = ?").parameterizedSql());
            assertEquals("SELECT 'a' 'b' FROM t WHERE id = ?", ParsedSql.parse("SELECT 'a'/*c*/'b' FROM t WHERE id = ?").parameterizedSql());

            // A kept comment is a token of its own, so the literals are not adjacent.
            assertEquals("SELECT 'a' /* c */ 'b' FROM t WHERE id = ?",
                    ParsedSql.parse("-- Keep comments\nSELECT 'a' /* c */\n'b' FROM t WHERE id = ?").parameterizedSql());

            // Alignment across a split MyBatis binding and the "??#{" repair.
            assertEquals("SELECT x FROM t WHERE a = ? AND b = 'x' 'y' AND c = 'p'\n'q'",
                    ParsedSql.parse("SELECT x FROM t WHERE a = #{ a } AND b = 'x' 'y'\nAND c = 'p'\n'q'").parameterizedSql());
            final ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE y = 'c'\n'd' AND z = ??#{w}");
            assertEquals("SELECT * FROM t WHERE y = 'c'\n'd' AND z = ???", parsed.parameterizedSql());
            assertEquals(List.of("w"), parsed.namedParameters());
        }

        // Regression: H2 reads "//" as a line comment, but the rebuild collapsed the line break that ends it, so on H2 the
        // comment swallowed the rest of the statement ("... tenant_id = 7 // scope AND id = 42" updated every row of tenant 7).
        @Test
        void testParse_lineBreakEndingALineThatHoldsDoubleSlashIsKept() {
            ParsedSql parsed = ParsedSql.parse("UPDATE t SET a = :a WHERE tenant_id = 7 // scope\nAND id = 42");
            assertEquals("UPDATE t SET a = ? WHERE tenant_id = 7 // scope\nAND id = 42", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());
            assertEquals(1, parsed.parameterCount());

            parsed = ParsedSql.parse("UPDATE t SET a = #{a} WHERE tenant_id = 7 // scope\nAND id = 42");
            assertEquals("UPDATE t SET a = ? WHERE tenant_id = 7 // scope\nAND id = 42", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());

            parsed = ParsedSql.parse("UPDATE t SET a = ? WHERE tenant_id = ? // scope\nAND id = ?");
            assertEquals("UPDATE t SET a = ? WHERE tenant_id = ? // scope\nAND id = ?", parsed.parameterizedSql());
            assertEquals(3, parsed.parameterCount());

            // A whitespace run, CRLF and a lone CR each become one line feed; the comment may end the statement's first clause.
            assertEquals("DELETE FROM t // x\nWHERE id = ?", ParsedSql.parse("DELETE FROM t // x\n WHERE id = :id").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? // c\nAND y = 2", ParsedSql.parse("SELECT a FROM t WHERE x = :a // c\r\nAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? // c\nAND y = 2", ParsedSql.parse("SELECT a FROM t WHERE x = :a // c\rAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? // c\nAND y = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a // c  \n\n  AND y = 2").parameterizedSql());

            // An empty comment, one glued to its operands, and a dropped comment between "//" and the line break.
            assertEquals("SELECT a FROM t WHERE x = ? //\nAND y = 2", ParsedSql.parse("SELECT a FROM t WHERE x = :a //\nAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = 4//2\nAND y = ?", ParsedSql.parse("SELECT a FROM t WHERE x = 4//2\nAND y = :a").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? // c\nAND y = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a // c -- d\nAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? // c\nAND y = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a // c /* d */\nAND y = 2").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? // c\n(y = 2)", ParsedSql.parse("SELECT a FROM t WHERE x = :a // c\n(y = 2)").parameterizedSql());

            // Gaps without a line break still collapse; after the kept line break the next lines collapse again unless they
            // hold "//" themselves.
            assertEquals("SELECT a FROM t WHERE x = ? // c d\nAND y = 2 AND z = 3",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a //   c  d\nAND y = 2\nAND z = 3").parameterizedSql());
            assertEquals("SELECT a FROM t WHERE x = ? //c\nAND y = 2 //d\nAND z = 1",
                    ParsedSql.parse("SELECT a FROM t WHERE x = :a //c\n  AND y = 2 //d\nAND z = 1").parameterizedSql());

            // Together with the adjacent-literal rule, and with a "//" kept glued to a block comment kept as a token.
            assertEquals("SELECT 'a'\n'b' // c\nFROM t WHERE id = ?", ParsedSql.parse("SELECT 'a'\n'b' // c\nFROM t WHERE id = ?").parameterizedSql());
            assertEquals("SELECT a FROM t //* c */\nWHERE x = ?",
                    ParsedSql.parse("-- Keep comments\nSELECT a FROM t //* c */\nWHERE x = ?").parameterizedSql());

            // When the output ends on a "//" line, the text the tokenizer discarded after the last token is kept as written
            // if it holds a "/*" and goes on to a later line. A "/*" on the "//" line is comment text to H2, which reads the
            // next line as SQL again, so that text must not be dropped; its markers stay unconverted (a binding there fails
            // loudly).
            parsed = ParsedSql.parse("DELETE FROM t WHERE x = :a // a /* b\nAND y = 2");
            assertEquals("DELETE FROM t WHERE x = ? // a /* b\nAND y = 2", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());
            parsed = ParsedSql.parse("DELETE FROM t WHERE x = :a //a/*b\r\n  AND y = :b;");
            assertEquals("DELETE FROM t WHERE x = ? //a/*b\r\n  AND y = :b", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());
            // A "/*" opening on a later line is a real, unterminated comment on H2 as well: kept, H2 now reports it instead of
            // running the statement truncated at "// a". A complete comment there is kept too, which is harmless.
            assertEquals("DELETE FROM t WHERE x = ? // a\n/* b\nAND y = 2",
                    ParsedSql.parse("DELETE FROM t WHERE x = :a // a\n/* b\nAND y = 2").parameterizedSql());
            assertEquals("DELETE FROM t WHERE x = ? // a\n/* b */", ParsedSql.parse("DELETE FROM t WHERE x = :a // a\n/* b */").parameterizedSql());

            // Nothing after a line break, or no "/*": nothing was dropped that H2 would read.
            assertEquals("DELETE FROM t WHERE x = ? // a", ParsedSql.parse("DELETE FROM t WHERE x = :a // a /* b").parameterizedSql());
            assertEquals("DELETE FROM t WHERE x = ? // a", ParsedSql.parse("DELETE FROM t WHERE x = :a // a /* b\n  ").parameterizedSql());
            assertEquals("DELETE FROM t WHERE x = ? // a", ParsedSql.parse("DELETE FROM t WHERE x = :a // a -- b\n-- c").parameterizedSql());

            // A literal that holds a backslash may end earlier on H2, which reads no backslash escapes, so its "//" counts.
            assertEquals("SELECT a FROM t WHERE x = 'C:\\' // it's\nAND y = 2",
                    ParsedSql.parse("SELECT a FROM t WHERE x = 'C:\\' // it's\nAND y = 2").parameterizedSql());

            // Dollar quotes are not modelled (see the class documentation): a "//" inside one counts too, which only keeps a
            // line break after the line and leaves the dollar-quoted text intact.
            assertEquals("SELECT a FROM t WHERE x = $$a//b$$\nAND y = ?", ParsedSql.parse("SELECT a FROM t WHERE x = $$a//b$$\nAND y = :a").parameterizedSql());

            // Unrecognized statements are not rebuilt, so their line breaks were never collapsed.
            assertEquals("SET x = 1 // c\nAND y", ParsedSql.parse("SET x = 1 // c\nAND y").parameterizedSql());
        }

        @Test
        void testParse_MultipleTrailingSemicolons() {
            // Bug: previously only a single trailing ';' was stripped.
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users;;");
            assertEquals("SELECT * FROM users", parsed.parameterizedSql());
            assertFalse(parsed.parameterizedSql().endsWith(";"));
        }

        @Test
        void testParse_NormalizedBuilderRangesKeepLiteralContentsAndLineBreaks() {
            final String[][] cases = { { " \tSELECT :id\u2003 ; \n;\u2003", "SELECT ?" }, { "SELECT ?\u2003;\u2003", "SELECT ?" },
                    { "SELECT '\u2003;'\u2003;", "SELECT '\u2003;'" }, { "SELECT 1 /* tail */", "SELECT 1" }, { "SELECT 'a'\n'b';\u2003", "SELECT 'a'\n'b'" },
                    { "SELECT 1 // c\nWHERE id = :id;\u2003", "SELECT 1 // c\nWHERE id = ?" } };
            for (final String[] c : cases) {
                assertEquals(c[1], ParsedSql.parse(c[0], null).parameterizedSql(), c[0]);
            }
        }

        @Test
        void testParse_ParenthesizedCommentOnlySqlHasNoOperationPrefix() {
            ParsedSql parsed = ParsedSql.parse("(/* comment only */)");

            assertEquals("(/* comment only */)", parsed.parameterizedSql());
            assertEquals(0, parsed.parameterCount());
        }

        @Test
        void testParse_RemovingBlockCommentsCannotCreateCommentOpenersOrOperators() {
            assertEquals("SELECT 1- -2", ParsedSql.parse("SELECT 1-/* comment */-2").parameterizedSql());
            assertEquals("SELECT 1/ *2", ParsedSql.parse("SELECT 1//* comment */*2").parameterizedSql());
            assertEquals("SELECT 1< =2", ParsedSql.parse("SELECT 1</* comment */=2").parameterizedSql());
            // Separate the placeholder from '-' because '?-' is a configured PostgreSQL operator.
            final ParsedSql positional = ParsedSql.parse("SELECT ? -/* comment */- ?");
            assertEquals("SELECT ? - - ?", positional.parameterizedSql());
            assertEquals(2, positional.parameterCount());
        }

        @Test
        void testParse_strippedBlockCommentKeepsTokenBoundary() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :id/*comment*/AND status = :status");

            assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());
            assertEquals(Arrays.asList("id", "status"), parsed.namedParameters());
        }

        // --- Bug fix: trailing-semicolon strip must also remove residual whitespace before the ';' ---

        @Test
        void testParse_TrailingSemicolon_WithWhitespaceBefore_StripsBoth() {
            // The body had whitespace before the trailing ';' — that whitespace must NOT leak
            // into the parameterized SQL after the ';' is removed.
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users   ;");
            assertEquals("SELECT * FROM users", parsed.parameterizedSql());
            assertFalse(parsed.parameterizedSql().endsWith(" "), "parameterizedSql must not end with whitespace after ';' removal");
        }

        @Test
        void testParse_TrailingSemicolonAndWhitespace_NamedParam() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId   ;");
            assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
        }

        @Test
        void testParse_TrailingSemicolonsPreservedInsideSql() {
            // Semicolons inside string literals must NOT be stripped — only trailing ones.
            ParsedSql parsed = ParsedSql.parse("SELECT ';' AS sep FROM dual;;");
            assertEquals("SELECT ';' AS sep FROM dual", parsed.parameterizedSql());
        }

        @Test
        void testParse_TrailingSemicolonsWithWhitespace() {
            // Mixed semicolons and whitespace at the end should all be stripped.
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users ;  ;  ");
            assertEquals("SELECT * FROM users", parsed.parameterizedSql());
            assertFalse(parsed.parameterizedSql().endsWith(";"));
            assertFalse(parsed.parameterizedSql().endsWith(" "));
        }

        @Test
        void testParse_UnchangedRenderedSqlSharesOriginalText() {
            for (final String sql : List.of("SELECT * FROM t WHERE id = ?", "SELECT ARRAY[?, ?]", "SELECT 'a'\n'b'", "SELECT " + "x".repeat(4000),
                    "\u0001SELECT ?\u0000")) {
                final ParsedSql parsed = ParsedSql.parse(sql, null);
                assertEquals(sql.trim(), parsed.parameterizedSql(), sql);
                assertSame(parsed.originalSql(), parsed.parameterizedSql(), sql);
            }
            final ParsedSql converted = ParsedSql.parse("SELECT :id", null);
            assertEquals("SELECT ?", converted.parameterizedSql());
            assertNotSame(converted.originalSql(), converted.parameterizedSql());
        }

        @Test
        void testParse_UnrecognizedSqlKeepsTextAndMarkerSyntax() {
            final String[][] cases = { { "\u2003SET @v = :value\u2003 ;\t;\u2003", "SET @v = :value" },
                    { "\uFEFFGRANT SELECT ON t TO :role ; ;", "\uFEFFGRANT SELECT ON t TO :role" },
                    { "/* leading */ CREATE TABLE t (x VARCHAR DEFAULT '#{broken') ;", "/* leading */ CREATE TABLE t (x VARCHAR DEFAULT '#{broken')" },
                    { "SET @v = :\uD800;", "SET @v = :\uD800" }, { "; \u2003;", "" } };
            for (final String[] c : cases) {
                final ParsedSql parsed = ParsedSql.parse(c[0], null);
                assertFalse(parsed.isDataOperation(), c[0]);
                assertEquals(c[1], parsed.parameterizedSql(), c[0]);
                assertEquals(0, parsed.parameterCount(), c[0]);
                assertEquals(List.of(), parsed.namedParameters(), c[0]);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), c[0]);
            }

            final String large = "CREATE TABLE t (x VARCHAR DEFAULT '" + "x".repeat(70_000) + "')";
            final ParsedSql parsed = ParsedSql.parse(large, null);
            assertSame(parsed.originalSql(), parsed.parameterizedSql());
            assertEquals(large, parsed.parameterizedSql());
            assertFalse(parsed.isDataOperation());
        }

        @Test
        void testParse_WithComments() {
            ParsedSql parsed = ParsedSql.parse("-- Comment\nSELECT * FROM users WHERE id = :id");
            assertNotNull(parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
        }

        @Test
        void testParse_withComments() {
            String sql = "-- This is a comment\nSELECT * FROM users WHERE id = :userId";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertNotNull(parsed);
            List<String> namedParams = parsed.namedParameters();
            assertEquals(1, namedParams.size());
            assertEquals("userId", namedParams.get(0));
        }

        @Test
        void testParse_withHashCommentAtLineStartWithoutSpace() {
            String sql = "#ignore :fake ?\nSELECT * FROM users WHERE id = :userId";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertNotNull(parsed);
            List<String> namedParams = parsed.namedParameters();
            assertEquals(1, namedParams.size());
            assertEquals("userId", namedParams.get(0));
        }

        @Test
        void testParse_withHashComments() {
            String sql = "SELECT * FROM users WHERE id = :userId # ignore :fake ?";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertNotNull(parsed);
            List<String> namedParams = parsed.namedParameters();
            assertEquals(1, namedParams.size());
            assertEquals("userId", namedParams.get(0));
        }

        @Test
        void testParse_withHashCommentsWithoutSpace() {
            String sql = "SELECT * FROM users WHERE id = :userId#ignore :fake ?";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertNotNull(parsed);
            List<String> namedParams = parsed.namedParameters();
            assertEquals(1, namedParams.size());
            assertEquals("userId", namedParams.get(0));
        }

        @Test
        void testParse_withMultilineComments() {
            String sql = "/* This is a\nmultiline comment */\nSELECT * FROM users WHERE id = :userId";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertNotNull(parsed);
            List<String> namedParams = parsed.namedParameters();
            assertEquals(1, namedParams.size());
            assertEquals("userId", namedParams.get(0));
        }

        @Test
        void testParse_WithTrailingSemicolon() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users;");
            assertEquals("SELECT * FROM users", parsed.parameterizedSql());
            assertFalse(parsed.parameterizedSql().endsWith(";"));
        }

        @Test
        void testParse_withTrailingSemicolon() {
            String sql = "SELECT * FROM users WHERE id = :userId;";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals("SELECT * FROM users WHERE id = :userId;", parsed.originalSql());
            assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        }

        @Test
        void testParse_WithWhitespace() {
            ParsedSql parsed = ParsedSql.parse("  SELECT * FROM users  ");
            assertEquals("SELECT * FROM users", parsed.originalSql());
            assertEquals("SELECT * FROM users", parsed.parameterizedSql());
        }

        @Test
        void testParse_withWhitespace() {
            String sql = "  SELECT * FROM users WHERE id = :userId  ";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals("SELECT * FROM users WHERE id = :userId", parsed.originalSql());
            assertEquals("SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
        }
    }

    @Nested
    class StatementRecognition {
        @Test
        void testIncompleteStatementPrefixesRemainUnclassified() {
            for (final String sql : List.of("(", "((", "{", "{#{result")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertFalse(parsed.isDataOperation(), sql);
                assertEquals(sql, parsed.parameterizedSql());
                assertEquals(0, parsed.parameterCount());
            }
        }

        @Test
        void testInsertStatement() {
            String sql = "INSERT INTO users (id, name, email) VALUES (:id, :name, :email)";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals("INSERT INTO users (id, name, email) VALUES (?, ?, ?)", parsed.parameterizedSql());
            List<String> params = parsed.namedParameters();
            assertEquals(3, params.size());
            assertEquals("id", params.get(0));
            assertEquals("name", params.get(1));
            assertEquals("email", params.get(2));
        }

        @Test
        void testNonOperationSql() {
            String sql = "SET @variable = :value";
            ParsedSql parsed = ParsedSql.parse(sql);

            String parameterizedSql = parsed.parameterizedSql();
            assertEquals("SET @variable = :value", parameterizedSql);
            assertTrue(parsed.namedParameters().isEmpty());
            assertEquals(0, parsed.parameterCount());
        }

        // Regression: TABLE statements and byte-order marks in EXPLAIN, doubled, after a comment and before a line comment.
        @Test
        void testParse_byteOrderMarkAndTableStatementVariants() {
            assertOffsets("﻿EXPLAIN SELECT ? FROM t", 16);
            assertOffsets("﻿﻿SELECT 1 WHERE a = ?", 21);
            assertOffsets("/* c */﻿SELECT ? FROM t", 15);
            assertOffsets("TABLE t -- c\nLIMIT ?", 19);
            assertOffsets("(TABLE t) UNION (TABLE u LIMIT ?)", 31);

            assertEquals("﻿ SELECT ? FROM t", ParsedSql.parse("﻿-- c\nSELECT ? FROM t").parameterizedSql());

            for (final String sql : List.of("﻿SET a = ?", "TABLESPACE x ?")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertFalse(parsed.isDataOperation(), sql);
                assertEquals(0, parsed.parameterCount(), sql);
            }
        }

        @Test
        void testParse_Delete() {
            ParsedSql parsed = ParsedSql.parse("DELETE FROM users WHERE id = :id");
            assertEquals("DELETE FROM users WHERE id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_deleteStatement() {
            String sql = "DELETE FROM users WHERE id = :id AND status = :status";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals("DELETE FROM users WHERE id = ? AND status = ?", parsed.parameterizedSql());
            List<String> namedParams = parsed.namedParameters();
            assertEquals(2, namedParams.size());
            assertEquals("id", namedParams.get(0));
            assertEquals("status", namedParams.get(1));
        }

        @Test
        void testParse_ExplainAnalyzeSelectPrefix() {
            ParsedSql parsed = ParsedSql.parse("EXPLAIN ANALYZE SELECT * FROM users WHERE id = :id");
            assertEquals("EXPLAIN ANALYZE SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_ExplainSelectPrefix() {
            ParsedSql parsed = ParsedSql.parse("EXPLAIN SELECT * FROM users WHERE id = :id");
            assertEquals("EXPLAIN SELECT * FROM users WHERE id = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_Insert() {
            ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, age) VALUES (:name, :age)");
            assertEquals("INSERT INTO users (name, age) VALUES (?, ?)", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals("name", parsed.namedParameters().get(0));
            assertEquals("age", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_mergeStatement() {
            String sql = "MERGE INTO users USING temp ON users.id = :id WHEN MATCHED THEN UPDATE SET name = :name";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> namedParams = parsed.namedParameters();
            assertEquals(2, namedParams.size());
            assertEquals("id", namedParams.get(0));
            assertEquals("name", namedParams.get(1));
        }

        @Test
        void testParse_nonOperationSql() {
            String sql = "SET @variable = :value";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals("SET @variable = :value", parsed.originalSql());
            assertEquals("SET @variable = :value", parsed.parameterizedSql());
            assertTrue(parsed.namedParameters().isEmpty());
            assertEquals(0, parsed.parameterCount());
        }

        @Test
        void testParse_NonQueryStatement() {
            ParsedSql parsed = ParsedSql.parse("CREATE TABLE users (id INT)");
            assertEquals("CREATE TABLE users (id INT)", parsed.parameterizedSql());
            assertEquals(0, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        @Test
        void testParse_ParenthesizedSelectPrefix() {
            ParsedSql parsed = ParsedSql.parse("(SELECT :id)");
            assertEquals("(SELECT ?)", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
        }

        @Test
        void testParse_ReplaceStatement() {
            ParsedSql parsed = ParsedSql.parse("REPLACE INTO users (id, name) VALUES (:id, :name)");
            assertEquals("REPLACE INTO users (id, name) VALUES (?, ?)", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
            assertEquals("name", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_SimpleSelect() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM users");
            assertNotNull(parsed);
            assertEquals("SELECT * FROM users", parsed.originalSql());
            assertEquals("SELECT * FROM users", parsed.parameterizedSql());
            assertEquals(0, parsed.parameterCount());
            assertTrue(parsed.namedParameters().isEmpty());
        }

        // Regression: a statement verb glued to a quoted name or bracket group (SELECT"a") was not recognized, so no marker was converted.
        @Test
        void testParse_statementVerbGluedToQuotedNameIsDataOperation() {
            ParsedSql parsed = ParsedSql.parse("SELECT\"a\" FROM t WHERE b = :b");
            assertTrue(parsed.isDataOperation());
            assertEquals("SELECT\"a\" FROM t WHERE b = ?", parsed.parameterizedSql());
            assertEquals(List.of("b"), parsed.namedParameters());

            parsed = ParsedSql.parse("UPDATE\"t\" SET a = :a");
            assertEquals("UPDATE\"t\" SET a = ?", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());

            for (final String sql : List.of("SELECT[a] FROM t WHERE b = ?", "SELECT`a` FROM t WHERE b = ?", "SELECT'x' FROM t WHERE b = ?",
                    "WITH\"x\" AS (SELECT 1) SELECT * FROM x WHERE b = ?", "EXPLAIN SELECT\"a\" FROM t WHERE b = ?", "(SELECT\"a\" FROM t WHERE b = ?)",
                    "﻿SELECT\"a\" FROM t WHERE b = ?", "{call\"f\"(?)}")) {
                parsed = ParsedSql.parse(sql);
                assertTrue(parsed.isDataOperation(), sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }

            // A token that starts quoted is a name, never a statement verb.
            assertFalse(ParsedSql.parse("\"SELECT\" a FROM t WHERE b = ?").isDataOperation());
            assertFalse(ParsedSql.parse("[SELECT] a FROM t WHERE b = ?").isDataOperation());
        }

        // Regression: TABLE statements and a leading byte-order mark were not recognized, so their placeholders were never counted.
        @Test
        void testParse_tableStatementAndLeadingByteOrderMarkAreDataOperations() {
            ParsedSql parsed = ParsedSql.parse("TABLE t ORDER BY c LIMIT ?");
            assertTrue(parsed.isDataOperation());
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { 25 }, parsed.positionalParameterOffsets());

            parsed = ParsedSql.parse("table t /* c */ LIMIT :n;");
            assertEquals("table t LIMIT ?", parsed.parameterizedSql());
            assertEquals(List.of("n"), parsed.namedParameters());

            parsed = ParsedSql.parse("\uFEFFSELECT * FROM t WHERE a = ?");
            assertTrue(parsed.isDataOperation());
            assertEquals("\uFEFFSELECT * FROM t WHERE a = ?", parsed.parameterizedSql());
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { 27 }, parsed.positionalParameterOffsets());

            parsed = ParsedSql.parse("\uFEFF (SELECT * FROM t WHERE a = :a)");
            assertEquals("\uFEFF (SELECT * FROM t WHERE a = ?)", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());

            assertFalse(ParsedSql.parse("SET search_path TO app").isDataOperation());
            assertFalse(ParsedSql.parse("\uFEFF").isDataOperation());
        }

        @Test
        void testParse_Update() {
            ParsedSql parsed = ParsedSql.parse("UPDATE users SET name = :name WHERE id = :id");
            assertEquals("UPDATE users SET name = ? WHERE id = ?", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals("name", parsed.namedParameters().get(0));
            assertEquals("id", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_updateStatement() {
            String sql = "UPDATE users SET name = :name, email = :email WHERE id = :id";
            ParsedSql parsed = ParsedSql.parse(sql);

            assertEquals("UPDATE users SET name = ?, email = ? WHERE id = ?", parsed.parameterizedSql());
            List<String> namedParams = parsed.namedParameters();
            assertEquals(3, namedParams.size());
            assertEquals("name", namedParams.get(0));
            assertEquals("email", namedParams.get(1));
            assertEquals("id", namedParams.get(2));
        }

        @Test
        void testParse_ValuesPrefix() {
            ParsedSql parsed = ParsedSql.parse("VALUES(:id, :name)");
            assertEquals("VALUES(?, ?)", parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertEquals("id", parsed.namedParameters().get(0));
            assertEquals("name", parsed.namedParameters().get(1));
        }

        @Test
        void testParse_withClause() {
            String sql = "WITH cte AS (SELECT * FROM users WHERE status = :status) SELECT * FROM cte WHERE id = :id";
            ParsedSql parsed = ParsedSql.parse(sql);

            List<String> namedParams = parsed.namedParameters();
            assertEquals(2, namedParams.size());
            assertEquals("status", namedParams.get(0));
            assertEquals("id", namedParams.get(1));
            assertEquals(2, parsed.parameterCount());
        }
    }

    @Nested
    class Subscripts {
        @Test
        void testChainedAndMarkerLedSubscriptCommentsPreserveBindingRoles() {
            for (final String sql : List.of("SELECT x[1][1 /* ] :ghost */ , :id]", "SELECT x[1] /* gap */ [1 -- ] :ghost\n, :id]",
                    "SELECT [:id /* ] :ghost */]", "SELECT x[1][1 /* [ ' :ghost */ , :id]")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(List.of("id"), parsed.namedParameters(), sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertTrue(parsed.parameterizedSql().contains(":ghost"), sql);
            }
            final String sql = "SELECT x[1][1 /* ] ? */ , ?], ?";
            assertArrayEquals(new int[] { sql.indexOf(", ?") + 2, sql.lastIndexOf('?') }, ParsedSql.parse(sql).positionalParameterOffsets());
            assertEquals(1, ParsedSql.parse("SELECT [q][?]['a', ?]").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT \"q\"[?]['a', ?]").parameterCount());
        }

        // Regression: bracket-group operands of JSON '?' operators (bracket-quoted identifier, spaced chain, subscript placeholder).
        @Test
        void testParse_bracketGroupOperandVariants() {
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE [payload] ? 'k'").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE payload['a'] ['b'] ? 'k'").parameterCount());
            assertOffsets("SELECT * FROM t WHERE x [?] ? 'k'", 25);
            assertEquals(3, ParsedSql.parse("UPDATE t SET [a] = ? WHERE [b] IN (?, ?)").parameterCount());
        }

        // Regression: a bracket-group token (chained or standalone subscript) before ?/?|/?& was not an operand, so the JSON operator counted as a placeholder.
        @Test
        void testParse_bracketGroupTokenIsJsonOperatorOperand() {
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE payload['a']['b'] ? 'k'").parameterCount());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT * FROM t WHERE payload['a']['b'] ? 'k' AND id = :id").namedParameters());
            assertEquals(0, ParsedSql.parse("SELECT * FROM t WHERE (payload)['a'] ? 'k'").parameterCount());

            String sql = "SELECT * FROM t WHERE payload[1][2] ?| array['a'] AND id = ?";
            ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(1, parsed.parameterCount());
            assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets());

            sql = "SELECT * FROM t WHERE payload[?][?] ? 'k'";
            parsed = ParsedSql.parse(sql);
            assertEquals(2, parsed.parameterCount());
            assertArrayEquals(new int[] { 30, 33 }, parsed.positionalParameterOffsets());

            // A '?' opening a bracket group is still a placeholder.
            assertEquals(1, ParsedSql.parse("SELECT ARRAY[?] FROM t").parameterCount());
            assertEquals(2, ParsedSql.parse("SELECT ARRAY [?, ?] FROM t").parameterCount());
        }

        @Test
        void testParse_BracketQuotedIdentifiersDoNotCreateParameters() {
            ParsedSql plain = ParsedSql.parse("SELECT [column] FROM users");
            assertEquals("SELECT [column] FROM users", plain.parameterizedSql());
            assertEquals(0, plain.parameterCount());

            // ':' not at position 1 inside the brackets: bracket-quoted identifier, not a subscript binding.
            ParsedSql colonInside = ParsedSql.parse("SELECT [weird:name] FROM users");
            assertEquals("SELECT [weird:name] FROM users", colonInside.parameterizedSql());
            assertEquals(0, colonInside.parameterCount());

            ParsedSql ibatis = ParsedSql.parse("SELECT [#{identifier}] FROM users");
            assertEquals("SELECT [#{identifier}] FROM users", ibatis.parameterizedSql());
            assertEquals(0, ibatis.parameterCount());

            // '[' immediately after a qualification dot is always SQL Server qualified quoting; a
            // subscript can never directly follow '.', so ".[:identifier]" keeps its literal meaning.
            ParsedSql qualified = ParsedSql.parse("SELECT users.[:identifier] FROM users");
            assertEquals("SELECT users.[:identifier] FROM users", qualified.parameterizedSql());
            assertEquals(0, qualified.parameterCount());

            ParsedSql qualifiedPlain = ParsedSql.parse("SELECT db.[col] FROM db");
            assertEquals("SELECT db.[col] FROM db", qualifiedPlain.parameterizedSql());
            assertEquals(0, qualifiedPlain.parameterCount());

            // "[:]" has no valid parameter-name character after the ':' so it is not a named binding.
            ParsedSql emptySlice = ParsedSql.parse("SELECT arr [:] FROM t");
            assertEquals("SELECT arr [:] FROM t", emptySlice.parameterizedSql());
            assertEquals(0, emptySlice.parameterCount());

            ParsedSql arraySubscript = ParsedSql.parse("SELECT values_array[:index] FROM users");
            assertEquals("SELECT values_array[?] FROM users", arraySubscript.parameterizedSql());
            assertEquals(1, arraySubscript.parameterCount());
        }

        @Test
        void testParse_ChainedSubscriptIsInspectedLikeTheFirstGroup() {
            // Regression: the tokenizer emits the second bracket group of "x['a', :b]['c', :d]" as a standalone
            // literal-first "[...]" token, which used to be treated as a bracket-quoted identifier, so :d was
            // left verbatim and "x[?]['c', ?]" emitted both '?' with parameterCount() == 1. The same holds when
            // the groups are separated by whitespace or a comment, which does not break the subscript chain.
            ParsedSql named = ParsedSql.parse("SELECT x['a', :b]['c', :d] FROM t");
            assertEquals("SELECT x['a', ?]['c', ?] FROM t", named.parameterizedSql());
            assertEquals(2, named.parameterCount());
            assertEquals(List.of("b", "d"), named.namedParameters());

            ParsedSql positional = ParsedSql.parse("SELECT x[?]['c', ?] FROM t");
            assertEquals("SELECT x[?]['c', ?] FROM t", positional.parameterizedSql());
            assertEquals(2, positional.parameterCount());

            ParsedSql ibatis = ParsedSql.parse("SELECT x[#{a}]['c', #{b}] FROM t");
            assertEquals("SELECT x[?]['c', ?] FROM t", ibatis.parameterizedSql());
            assertEquals(2, ibatis.parameterCount());
            assertEquals(List.of("a", "b"), ibatis.namedParameters());

            // a literal-only first group still chains
            ParsedSql literalFirst = ParsedSql.parse("SELECT x[1]['c', :d] FROM t");
            assertEquals("SELECT x[1]['c', ?] FROM t", literalFirst.parameterizedSql());
            assertEquals(List.of("d"), literalFirst.namedParameters());

            // already-working shapes are unchanged
            assertEquals(3, ParsedSql.parse("SELECT arr[?][?][?] FROM t").parameterCount());
            assertEquals(List.of("a", "b"), ParsedSql.parse("SELECT x[:a][:b] FROM t").namedParameters());

            // literal content inside a chained group is never a parameter
            ParsedSql literalOnly = ParsedSql.parse("SELECT x[1][':nope', '#{nope}', '?'] FROM t");
            assertEquals("SELECT x[1][':nope', '#{nope}', '?'] FROM t", literalOnly.parameterizedSql());
            assertEquals(0, literalOnly.parameterCount());

            // Whitespace and comments between the groups do NOT break the chain: "x[?] ['c', ?]" is the same
            // PostgreSQL expression as "x[?]['c', ?]", so both placeholders are counted and parameterCount()
            // matches the number of '?' actually emitted into parameterizedSql().
            ParsedSql spacedChain = ParsedSql.parse("SELECT x[?] ['c', ?] FROM t");
            assertEquals("SELECT x[?] ['c', ?] FROM t", spacedChain.parameterizedSql());
            assertEquals(2, spacedChain.parameterCount());
            assertEquals(2, spacedChain.parameterizedSql().chars().filter(c -> c == '?').count());

            ParsedSql spacedNamedChain = ParsedSql.parse("SELECT x[:a] ['c', :b] FROM t");
            assertEquals("SELECT x[?] ['c', ?] FROM t", spacedNamedChain.parameterizedSql());
            assertEquals(List.of("a", "b"), spacedNamedChain.namedParameters());

            // A comment between the groups behaves like whitespace.
            assertEquals(2, ParsedSql.parse("SELECT x[?] /* c */ ['c', ?] FROM t").parameterCount());

            // The chain root must be a real subscript: a bracket group after a bracket-QUOTED IDENTIFIER is a
            // SQL Server column with a bracketed alias, so it keeps the standalone reading and stays literal.
            ParsedSql bracketedAlias = ParsedSql.parse("SELECT [a] [b:c] FROM t WHERE id = :id");
            assertEquals("SELECT [a] [b:c] FROM t WHERE id = ?", bracketedAlias.parameterizedSql());
            assertEquals(List.of("id"), bracketedAlias.namedParameters());

            ParsedSql qualifiedBracketedAlias = ParsedSql.parse("SELECT t.[a] [b:c] FROM t WHERE id = :id");
            assertEquals("SELECT t.[a] [b:c] FROM t WHERE id = ?", qualifiedBracketedAlias.parameterizedSql());
            assertEquals(List.of("id"), qualifiedBracketedAlias.namedParameters());

            assertEquals("SELECT [a],[b:c] FROM t", ParsedSql.parse("SELECT [a],[b:c] FROM t").parameterizedSql());
            assertEquals("SELECT [db].[schema].[t:col] FROM x", ParsedSql.parse("SELECT [db].[schema].[t:col] FROM x").parameterizedSql());
            assertEquals("INSERT INTO [t]([a],[b]) VALUES (?, ?)", ParsedSql.parse("INSERT INTO [t]([a],[b]) VALUES (?, ?)").parameterizedSql());
            assertEquals(2, ParsedSql.parse("INSERT INTO [t]([a],[b]) VALUES (?, ?)").parameterCount());

            // the mixed-style guard sees the chained marker
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT x[?]['c', :d] FROM t"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT x[:a]['c', #{b}] FROM t"));

            // the documented bracket-identifier exceptions are untouched
            assertEquals("SELECT t.[:name] FROM t", ParsedSql.parse("SELECT t.[:name] FROM t").parameterizedSql());
            assertEquals(0, ParsedSql.parse("SELECT t.[:name] FROM t").parameterCount());
            assertEquals("SELECT [#{ids}] FROM t", ParsedSql.parse("SELECT [#{ids}] FROM t").parameterizedSql());
            assertEquals(0, ParsedSql.parse("SELECT [#{ids}] FROM t").parameterCount());
        }

        @Test
        void testParse_CommentsInsideSubscriptsDoNotCreateParameters() {
            final ParsedSql named = ParsedSql.parse("SELECT ARRAY[:first /* :ignored ? #{ignored} */, :last]");
            assertEquals("SELECT ARRAY[? /* :ignored ? #{ignored} */, ?]", named.parameterizedSql());
            assertEquals(List.of("first", "last"), named.namedParameters());
            assertEquals(2, named.parameterCount());

            final ParsedSql ibatis = ParsedSql.parse("SELECT ARRAY[#{first} /* #{ignored} :ignored ? */, #{last}]");
            assertEquals("SELECT ARRAY[? /* #{ignored} :ignored ? */, ?]", ibatis.parameterizedSql());
            assertEquals(List.of("first", "last"), ibatis.namedParameters());
            assertEquals(2, ibatis.parameterCount());

            final String positionalSql = "SELECT ARRAY[? /* ? :ignored #{ignored} */, ?]";
            final ParsedSql positional = ParsedSql.parse(positionalSql);
            assertEquals(positionalSql, positional.parameterizedSql());
            assertEquals(2, positional.parameterCount());
            assertArrayEquals(new int[] { positionalSql.indexOf('?'), positionalSql.lastIndexOf('?') }, positional.positionalParameterOffsets());

            final ParsedSql lineComment = ParsedSql.parse("SELECT ARRAY[:first -- :ignored ? #{ignored}\n, :last]");
            assertEquals("SELECT ARRAY[? -- :ignored ? #{ignored}\n, ?]", lineComment.parameterizedSql());
            assertEquals(List.of("first", "last"), lineComment.namedParameters());
            assertEquals(2, lineComment.parameterCount());

            assertEquals(List.of("id"), ParsedSql.parse("SELECT arr[1][/* :ignored ? #{ignored} */ :id]").namedParameters());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY['/* :literal */', :id]").namedParameters());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY['-- :literal', :id]").namedParameters());
        }

        @Test
        void testParse_CompactMarkerChainsRetainEveryOffsetInsideAndOutsideBrackets() {
            for (final String expression : List.of("?||?||?", "?-?-?", "?|?|?", "?&?&?", "?#?#?", "? || ? || ?", "?/* left */||?/* right */||?",
                    "?-- left\n||?-- right\n||?")) {
                for (final boolean grouped : new boolean[] { false, true }) {
                    final String sql = grouped ? "SELECT ARRAY[" + expression + "] FROM t" : "SELECT " + expression + " FROM t";
                    final int first = sql.indexOf('?');
                    final int second = sql.indexOf('?', first + 1);
                    final int third = sql.indexOf('?', second + 1);
                    final ParsedSql parsed = ParsedSql.parse(sql);

                    assertEquals(3, parsed.parameterCount(), sql);
                    assertArrayEquals(new int[] { first, second, third }, parsed.positionalParameterOffsets(), sql);
                }
            }

            for (final String binding : List.of(":tail", "#{tail}")) {
                for (final String expression : List.of("?||?||" + binding, binding + "||?||?", "?/* gap */||?||" + binding)) {
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT " + expression + " FROM t"), expression);
                    assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[" + expression + "] FROM t"), expression);
                }
            }
        }

        @Test
        void testParse_EscapedQuoteInsideSubscriptLiteralDoesNotEndTheLiteral() {
            // Where a literal inside a subscript ends depends on the dialect once it contains a backslash: under
            // MySQL "\'" is an escaped quote, under standard-conforming strings "'a\'" is a complete literal.
            // Committing to either reading corrupts the other dialect, so the scanners evaluate a subscript token
            // under BOTH readings and bind only when they agree; a token on which they disagree is left verbatim
            // (nothing bound, nothing counted) so the leftover marker fails loudly at the driver.
            ParsedSql ambiguous = ParsedSql.parse("SELECT ARRAY['it\\'s :literal', :id] FROM t");
            assertEquals("SELECT ARRAY['it\\'s :literal', :id] FROM t", ambiguous.parameterizedSql());
            assertEquals(List.of(), ambiguous.namedParameters());
            assertEquals(0, ambiguous.parameterCount());

            // A PostgreSQL escape string is the exception: that syntax exists only in PostgreSQL and always
            // processes backslash escapes, so both readings see the same literal and the marker next to it is
            // bound with the literal left intact.
            ParsedSql escaped = ParsedSql.parse("SELECT ARRAY[E'it\\'s :literal', :id] FROM t");
            assertEquals("SELECT ARRAY[E'it\\'s :literal', ?] FROM t", escaped.parameterizedSql());
            assertEquals(List.of("id"), escaped.namedParameters());
            assertEquals(1, escaped.parameterCount());

            // The prefix must be a standalone E: a name that merely ends with it is not an escape string, so
            // "code_E'it\'s ...'" stays ambiguous and verbatim.
            assertEquals(0, ParsedSql.parse("SELECT ARRAY[code_E'it\\'s :literal', :id] FROM t").parameterCount());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY[e'it\\'s :literal', :id] FROM t").namedParameters());

            // Agree cases: no backslash, doubled quotes, or an escaped backslash right before the closing quote
            // (both readings: the pair is consumed / is two ordinary characters, then the quote closes).
            ParsedSql doubled = ParsedSql.parse("SELECT ARRAY['it''s :x', :id] FROM t");
            assertEquals("SELECT ARRAY['it''s :x', ?] FROM t", doubled.parameterizedSql());
            assertEquals(List.of("id"), doubled.namedParameters());
            assertEquals(1, doubled.parameterCount());

            ParsedSql trailingBackslash = ParsedSql.parse("SELECT ARRAY['a\\\\', :id] FROM t");
            assertEquals("SELECT ARRAY['a\\\\', ?] FROM t", trailingBackslash.parameterizedSql());
            assertEquals(List.of("id"), trailingBackslash.namedParameters());
            assertEquals(1, ParsedSql.parse("SELECT ARRAY['a\\\\', ?] FROM t").parameterCount());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY['a\\\\', #{id}] FROM t").namedParameters());

            ParsedSql escapedBackslash = ParsedSql.parse("SELECT ARRAY[E'a\\\\b', ?] FROM t");
            assertEquals("SELECT ARRAY[E'a\\\\b', ?] FROM t", escapedBackslash.parameterizedSql());
            assertEquals(1, escapedBackslash.parameterCount());
            assertEquals(1, escapedBackslash.positionalParameterOffsets().length);
            assertEquals("SELECT ARRAY[E'a\\\\b', ?] FROM t".lastIndexOf('?'), escapedBackslash.positionalParameterOffsets()[0]);

            // Double-quoted and backtick-quoted identifiers escape by doubling only (never ambiguous).
            assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY[\"a\"\"b :x\", :id] FROM t").namedParameters());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY[`a``b :x`, :id] FROM t").namedParameters());

            // Disagree cases, one per marker kind: verbatim, count 0, no names, no positional offsets.
            ParsedSql namedDisagree = ParsedSql.parse("SELECT ARRAY['\\'' , :id] FROM t");
            assertEquals("SELECT ARRAY['\\'' , :id] FROM t", namedDisagree.parameterizedSql());
            assertEquals(List.of(), namedDisagree.namedParameters());
            assertEquals(0, namedDisagree.parameterCount());

            ParsedSql ibatisDisagree = ParsedSql.parse("SELECT ARRAY['a\\'', #{id}] FROM t");
            assertEquals("SELECT ARRAY['a\\'', #{id}] FROM t", ibatisDisagree.parameterizedSql());
            assertEquals(List.of(), ibatisDisagree.namedParameters());
            assertEquals(0, ibatisDisagree.parameterCount());

            ParsedSql positionalDisagree = ParsedSql.parse("SELECT ARRAY['a\\'', ?] FROM t");
            assertEquals("SELECT ARRAY['a\\'', ?] FROM t", positionalDisagree.parameterizedSql());
            assertEquals(0, positionalDisagree.parameterCount());
            assertEquals(0, positionalDisagree.positionalParameterOffsets().length);
            assertEquals(0, ParsedSql.parse("SELECT ARRAY['a\\'?', ?] FROM t").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT ARRAY['it\\'s ?', ?, ?] FROM t").parameterCount());
            assertEquals(0, ParsedSql.parse("SELECT ARRAY['a\\'#{x}', #{id}] FROM t").parameterCount());

            // The same shapes with an escape-string prefix: the markers inside the literal stay literal and only
            // the ones next to it are counted.
            assertEquals(1, ParsedSql.parse("SELECT ARRAY[E'a\\'?', ?] FROM t").parameterCount());
            assertEquals(2, ParsedSql.parse("SELECT ARRAY[E'it\\'s ?', ?, ?] FROM t").parameterCount());
            assertEquals(List.of("id"), ParsedSql.parse("SELECT ARRAY[E'a\\'#{x}', #{id}] FROM t").namedParameters());

            // A disagreeing subscript next to an ordinary top-level binding: the tokenizer closes the bracket
            // group under the backslash reading, so ":id" outside it is its own token and is bound alone while
            // the ambiguous group stays verbatim.
            ParsedSql mixed = ParsedSql.parse("SELECT ARRAY['a\\'', :x] FROM t WHERE id = :id");
            assertEquals("SELECT ARRAY['a\\'', :x] FROM t WHERE id = ?", mixed.parameterizedSql());
            assertEquals(List.of("id"), mixed.namedParameters());
            assertEquals(1, mixed.parameterCount());

            // Each group of a chained subscript is judged on its own.
            ParsedSql chained = ParsedSql.parse("SELECT arr['a\\'', :x]['b', :y] FROM t");
            assertEquals("SELECT arr['a\\'', :x]['b', ?] FROM t", chained.parameterizedSql());
            assertEquals(List.of("y"), chained.namedParameters());

            // The mixed-style guard is unchanged for unambiguous tokens; an ambiguous token contributes no
            // marker to it, so the "?" it holds verbatim cannot clash with a named binding elsewhere.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY['it''s :literal', ?] FROM t WHERE id = :id"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[E'it''s :literal', ?] FROM t WHERE id = :id"));
            // An escape string is unambiguous, so the "?" next to it IS a placeholder and does clash.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[E'it\\'s :literal', ?] FROM t WHERE id = :id"));
            ParsedSql verbatimQuestionMark = ParsedSql.parse("SELECT ARRAY['it\\'s :literal', ?] FROM t WHERE id = :id");
            assertEquals("SELECT ARRAY['it\\'s :literal', ?] FROM t WHERE id = ?", verbatimQuestionMark.parameterizedSql());
            assertEquals(List.of("id"), verbatimQuestionMark.namedParameters());
            assertEquals(1, verbatimQuestionMark.parameterCount());
        }

        // Regression: inside one bracket token the closing '}' of #{...} was searched without regard to quotes.
        @Test
        void testParse_ibatisBindingInBracketTokenCannotCloseInsideLiteral() {
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[#{a, '}'] FROM t"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[#{a, 'x'] FROM t WHERE b = #{c}"));

            // A quote after the closing brace is an ordinary literal element.
            final ParsedSql parsed = ParsedSql.parse("SELECT ARRAY[#{a}, '}'] FROM t");
            assertEquals("SELECT ARRAY[?, '}'] FROM t", parsed.parameterizedSql());
            assertEquals(List.of("a"), parsed.namedParameters());
        }

        @Test
        void testParse_JsonExistenceOperatorIsClassifiedAlikeInsideAndOutsideBracketGroups() {
            // The tokenizer glues a bracket region into one token, which used to make the same operator read as
            // a placeholder there and collide with a named binding in the group. Both paths now apply the same
            // operand test: a '?' between two operands is the operator and is preserved, anywhere it appears.
            for (final String operator : List.of("?", "?|", "?&")) {
                final ParsedSql grouped = ParsedSql.parse("SELECT ARRAY [:payload " + operator + " 'key']");
                assertEquals("SELECT ARRAY [? " + operator + " 'key']", grouped.parameterizedSql());
                assertEquals(1, grouped.parameterCount());
                assertEquals(List.of("payload"), grouped.namedParameters());

                final ParsedSql ungrouped = ParsedSql.parse("SELECT * FROM t WHERE payload " + operator + " :key");
                assertEquals(1, ungrouped.parameterCount());
                assertEquals(List.of("key"), ungrouped.namedParameters());
            }

            // Glued and chained subscripts follow the same rule as a standalone group.
            assertEquals(1, ParsedSql.parse("SELECT payload[:key ? 'a'] FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT p[:a]['b' ? 'c'] FROM t").parameterCount());

            // A marker that opens or closes an element has no operand on one side and stays a placeholder,
            // so a named binding in the same group is still a genuine mix of styles.
            for (final String mixed : List.of("SELECT ARRAY [:id, ?]", "SELECT ARRAY [?, :id]", "SELECT ARRAY [:id ?]", "SELECT ARRAY [? :id]")) {
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(mixed), mixed);
            }

            // Positional subscripts keep counting every placeholder they hold.
            assertEquals(2, ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?, ?]").parameterCount());
            assertEquals(2, ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?::text, ?::text]").parameterCount());

            // One group holding both: only the placeholder is counted, and its recorded offset still points at
            // that marker rather than at the operator.
            final String both = "SELECT ARRAY [?, 'a' ? 'b']";
            final ParsedSql mixedGroup = ParsedSql.parse(both);
            assertEquals(1, mixedGroup.parameterCount());
            assertEquals(both, mixedGroup.parameterizedSql());
            assertArrayEquals(new int[] { both.indexOf('?') }, mixedGroup.positionalParameterOffsets());
        }

        @Test
        void testParse_LargeCompactConcatenationRetainsEveryOffsetInsideAndOutsideBrackets() {
            // Both scans must retain all markers in a long chain; a timing assertion would depend on the machine.
            final int markerCount = 16000;
            final String expression = "?||".repeat(markerCount - 1) + "?";

            for (final boolean grouped : new boolean[] { false, true }) {
                final String prefix = grouped ? "SELECT ARRAY[" : "SELECT ";
                final String sql = prefix + expression + (grouped ? "] FROM t" : " FROM t");
                final int[] expectedOffsets = new int[markerCount];

                for (int i = 0; i < markerCount; i++) {
                    expectedOffsets[i] = prefix.length() + 3 * i;
                }

                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(sql, parsed.parameterizedSql());
                assertEquals(markerCount, parsed.parameterCount());
                assertArrayEquals(expectedOffsets, parsed.positionalParameterOffsets());
            }
        }

        @Test
        void testParse_LargeCompactSubscriptRetainsEveryPositionalOffset() {
            // With no spaces or commas, rescanning both sides of each '?' makes this expression quadratic.
            // Verify all offsets without a machine-dependent timing assertion.
            final int markerCount = 16000;
            final String prefix = "SELECT ARRAY[";
            final String sql = prefix + "?+".repeat(markerCount - 1) + "?]";
            final int[] expectedOffsets = new int[markerCount];

            for (int i = 0; i < markerCount; i++) {
                expectedOffsets[i] = prefix.length() + 2 * i;
            }

            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(sql, parsed.parameterizedSql());
            assertEquals(markerCount, parsed.parameterCount());
            assertArrayEquals(expectedOffsets, parsed.positionalParameterOffsets());
        }

        @Test
        void testParse_ManyMarkersInOneSubscriptToken() {
            // A subscript is emitted as a single token, so all of its markers are extracted from that one token.
            // The marker positions are therefore collected once per token instead of once per marker (which was
            // quadratic in the number of markers); this asserts the result of that scan for a token holding many.
            final int markerCount = 2000;
            final StringBuilder named = new StringBuilder("SELECT ARRAY[");
            final StringBuilder ibatis = new StringBuilder("SELECT ARRAY[");
            final List<String> expectedNames = new ArrayList<>(markerCount);

            for (int i = 0; i < markerCount; i++) {
                if (i > 0) {
                    named.append(", ");
                    ibatis.append(", ");
                }

                named.append(':').append("id").append(i);
                ibatis.append("#{id").append(i).append('}');
                expectedNames.add("id" + i);
            }

            final ParsedSql namedParsed = ParsedSql.parse(named.append("] FROM t").toString());
            assertEquals(markerCount, namedParsed.parameterCount());
            assertEquals(expectedNames, namedParsed.namedParameters());
            assertEquals(markerCount, Strings.countMatches(namedParsed.parameterizedSql(), '?'));

            final ParsedSql ibatisParsed = ParsedSql.parse(ibatis.append("] FROM t").toString());
            assertEquals(markerCount, ibatisParsed.parameterCount());
            assertEquals(expectedNames, ibatisParsed.namedParameters());
            assertEquals(markerCount, Strings.countMatches(ibatisParsed.parameterizedSql(), '?'));
        }

        @Test
        void testParse_MultiDimensionalArrayConstructorDoesNotHideLaterBindings() {
            final ParsedSql named = ParsedSql.parse("SELECT * FROM t WHERE m = ARRAY[[1,2],[3,4]] AND id = :id");
            assertEquals("SELECT * FROM t WHERE m = ARRAY[[1,2],[3,4]] AND id = ?", named.parameterizedSql());
            assertEquals(Arrays.asList("id"), named.namedParameters());

            assertEquals(Arrays.asList("id"), ParsedSql.parse("SELECT * FROM t WHERE m = ARRAY[[1,2],[3,4]] AND id = #{id}").namedParameters());

            final String positionalSql = "SELECT * FROM t WHERE m = ARRAY[[1,2],[3,4]] AND id = ?";
            final ParsedSql positional = ParsedSql.parse(positionalSql);
            assertEquals(1, positional.parameterCount());
            assertArrayEquals(new int[] { positionalSql.lastIndexOf('?') }, positional.positionalParameterOffsets());

            assertEquals(Arrays.asList("a"), ParsedSql.parse("SELECT ARRAY[[1,2],[3,:a]]").namedParameters());
            assertEquals(1, ParsedSql.parse("SELECT ARRAY[[1,2],[3,?]]").parameterCount());
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE m = ARRAY[[1,2],[3,?]] AND id = :id"));
        }

        @Test
        void testParse_NamedAndIbatisMarkersInsideSubscriptWithLiteralAreBound() {
            // The tokenizer keeps "ARRAY['a', :id]" as ONE token; a quoted literal inside the subscript must not
            // make the named/iBatis scanners skip the whole token (the positional scanner already handled it).
            ParsedSql named = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY['a', :id]");
            assertEquals("SELECT * FROM t WHERE tags @> ARRAY['a', ?]", named.parameterizedSql());
            assertEquals(1, named.parameterCount());
            assertEquals(List.of("id"), named.namedParameters());

            ParsedSql namedFirst = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[:id, 'a'] AND id = :id2");
            assertEquals("SELECT * FROM t WHERE tags @> ARRAY[?, 'a'] AND id = ?", namedFirst.parameterizedSql());
            assertEquals(List.of("id", "id2"), namedFirst.namedParameters());

            ParsedSql ibatis = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY['a', #{id}]");
            assertEquals("SELECT * FROM t WHERE tags @> ARRAY['a', ?]", ibatis.parameterizedSql());
            assertEquals(List.of("id"), ibatis.namedParameters());

            // literal content inside the subscript is still not a marker
            ParsedSql literalOnly = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[':notparam', '#{nope}']");
            assertEquals("SELECT * FROM t WHERE tags @> ARRAY[':notparam', '#{nope}']", literalOnly.parameterizedSql());
            assertEquals(0, literalOnly.parameterCount());

            // the mixed-style guard must see the marker
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ? FROM t WHERE tags @> ARRAY['a', :id]"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE arr = ARRAY[:a, 'x'] AND id = ?"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE arr = ARRAY['x', #{a}] AND id = :b"));
        }

        @Test
        void testParse_PositionalPlaceholderInsideArraySubscriptIsCounted() {
            // Regression: the tokenizer glues "[...]" to the preceding identifier, so the "?" inside
            // "ARRAY[?]" never surfaced as its own token and was silently dropped from parameterCount(),
            // although the named forms "ARRAY[:ids]" / "ARRAY[#{ids}]" in the same position were bound.
            ParsedSql unspaced = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?] AND id = ?");
            assertEquals("SELECT * FROM t WHERE tags @> ARRAY[?] AND id = ?", unspaced.parameterizedSql());
            assertEquals(2, unspaced.parameterCount());
            assertTrue(unspaced.namedParameters().isEmpty());

            ParsedSql spaced = ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY [?, ?]");
            assertEquals(2, spaced.parameterCount());

            assertEquals(2, ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?::text, ?::text]").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT arr[?] FROM t").parameterCount());

            // A '?' inside a quoted literal within the subscript is not a placeholder.
            assertEquals(1, ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY['a?', ?]").parameterCount());

            // Bracket-quoted identifiers that merely contain '?' are preserved.
            assertEquals(1, ParsedSql.parse("SELECT [what?] FROM t WHERE id = ?").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT [?foo] FROM t WHERE id = ?").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT t.[what?] FROM t WHERE id = ?").parameterCount());

            // A positional marker in a subscript still participates in the mixed-style guard.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[?] AND name = :n"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT * FROM t WHERE tags @> ARRAY[#{a}, ?]"));
        }

        // Regression: a split MyBatis marker after one whose property path holds a closed bracket group was rejected as unclosed.
        @Test
        void testParse_splitIbatisMarkerAfterClosedBracketGroupBinds() {
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a IN (#{a[0]}#{b })");
            assertEquals("SELECT * FROM t WHERE a IN (? ?)", parsed.parameterizedSql());
            assertEquals(List.of("a[0]", "b"), parsed.namedParameters());

            parsed = ParsedSql.parse("SELECT * FROM t WHERE a IN (#{ a[0] }#{ b })");
            assertEquals("SELECT * FROM t WHERE a IN (? ?)", parsed.parameterizedSql());
            assertEquals(List.of("a[0]", "b"), parsed.namedParameters());

            assertEquals(List.of("list[0]", "list[0]"), ParsedSql.parse("SELECT #{list[0]}#{list[0]} 1 FROM t").namedParameters());

            // An opener inside a group that is still open cannot continue past it.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[x[1], #{a] FROM t WHERE b = #{c}"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("SELECT ARRAY[']', #{a] FROM t WHERE b = #{c}"));
        }

        @Test
        void testParse_StandaloneBracketSubscriptWithNamedParameterBinds() {
            // Whitespace before '[' makes the tokenizer emit "[:ids]" as a standalone token; the leading
            // "[:name" shape is recognized as a PostgreSQL-style subscript binding, not a bracket-quoted
            // identifier, so the named parameter is extracted instead of passing through as literal SQL.
            ParsedSql spaced = ParsedSql.parse("SELECT * FROM t WHERE arr = array [:ids]");
            assertEquals("SELECT * FROM t WHERE arr = array [?]", spaced.parameterizedSql());
            assertEquals(1, spaced.parameterCount());
            assertEquals(List.of("ids"), spaced.namedParameters());

            ParsedSql standalone = ParsedSql.parse("SELECT [:identifier] FROM users");
            assertEquals("SELECT [?] FROM users", standalone.parameterizedSql());
            assertEquals(1, standalone.parameterCount());
            assertEquals(List.of("identifier"), standalone.namedParameters());

            // Colon-style names follow identifier grammar. A digit cannot start a segment, so Oracle-style
            // ordinal markers are preserved rather than being misreported as named property bindings.
            ParsedSql ordinalSpaced = ParsedSql.parse("SELECT * FROM t WHERE a = arr [:2]");
            assertEquals("SELECT * FROM t WHERE a = arr [:2]", ordinalSpaced.parameterizedSql());
            assertEquals(0, ordinalSpaced.parameterCount());

            ParsedSql ordinalUnspaced = ParsedSql.parse("SELECT * FROM t WHERE a = arr[:2]");
            assertEquals("SELECT * FROM t WHERE a = arr[:2]", ordinalUnspaced.parameterizedSql());
            assertEquals(0, ordinalUnspaced.parameterCount());
        }

        @Test
        void testParse_StandaloneSubscriptsKeepQuotedAndCommentedMarkersLiteral() {
            final String sql = "SELECT ARRAY [?, ':literal', '#{literal}', /* :ignored #{ignored} */ ?]";
            final ParsedSql parsed = ParsedSql.parse(sql);
            assertEquals(sql, parsed.parameterizedSql());
            assertEquals(2, parsed.parameterCount());
            assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets());

            final ParsedSql named = ParsedSql.parse("SELECT ARRAY [:id, '?', /* ? */ :other]");
            assertEquals(List.of("id", "other"), named.namedParameters());
            assertEquals(2, named.parameterCount());
            assertEquals("SELECT ARRAY [?, '?', /* ? */ ?]", named.parameterizedSql());

            final ParsedSql quoted = ParsedSql.parse("SELECT t.[?, :literal], [#{literal}], [?identifier:literal] FROM t");
            assertEquals(0, quoted.parameterCount());
            assertEquals(quoted.originalSql(), quoted.parameterizedSql());
        }

        @Test
        void testParse_StandaloneSubscriptWithInnerWhitespaceBindsNamedParameter() {
            // "[ :ids ]" binds exactly like "[ ? ]" does; the whitespace after '[' is skipped for both styles.
            ParsedSql spacedInner = ParsedSql.parse("SELECT * FROM t WHERE arr = array [ :ids ]");
            assertEquals("SELECT * FROM t WHERE arr = array [ ? ]", spacedInner.parameterizedSql());
            assertEquals(1, spacedInner.parameterCount());
            assertEquals(List.of("ids"), spacedInner.namedParameters());

            // a standalone "[#{...}]" stays a bracket-quoted identifier (pinned by
            // testParse_BracketQuotedIdentifiersDoNotCreateParameters), with or without inner whitespace
            ParsedSql ibatisSpaced = ParsedSql.parse("SELECT * FROM t WHERE arr = array [ #{ids} ]");
            assertEquals("SELECT * FROM t WHERE arr = array [ #{ids} ]", ibatisSpaced.parameterizedSql());
            assertEquals(0, ibatisSpaced.parameterCount());
        }

        @Test
        void testParse_SubscriptJsonOperandsKeepCommentsQuotesAndBindingsIntact() {
            for (final String sql : List.of("SELECT ARRAY[:payload /* ? :ignored */ ? /* right */ 'key']", "SELECT ARRAY[:payload -- ? :ignored\n ? 'key']",
                    "SELECT ARRAY[:payload ? -- ? :ignored\n 'key']", "SELECT ARRAY[:payload?'key']", "SELECT ARRAY[(:payload)::jsonb ? 'key']",
                    "SELECT ARRAY[:payload ? ('key')]")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                // A converted marker is kept apart from an adjacent operator '?' so the pair is not a pgJDBC "??" escape.
                assertEquals(sql.replace(":payload?", "? ?").replace(":payload", "?"), parsed.parameterizedSql());
                assertEquals(List.of("payload"), parsed.namedParameters(), sql);
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
            }

            final ParsedSql function = ParsedSql.parse("SELECT ARRAY[:payload ? lower(:key)]");
            assertEquals("SELECT ARRAY[? ? lower(?)]", function.parameterizedSql());
            assertEquals(List.of("payload", "key"), function.namedParameters());
            assertEquals(2, function.parameterCount());

            final ParsedSql ibatis = ParsedSql.parse("SELECT ARRAY[#{payload, jdbcType=OTHER} ? 'key']");
            assertEquals("SELECT ARRAY[? ? 'key']", ibatis.parameterizedSql());
            assertEquals(List.of("payload"), ibatis.namedParameters());
            assertEquals(1, ibatis.parameterCount());

            for (final String sql : List.of("SELECT ARRAY['{ \"a\": 1 }'::jsonb ? 'a']", "SELECT ARRAY[\"has spaces\" ? 'key']",
                    "SELECT ARRAY[docs[1] ? 'key']", "SELECT ARRAY[E'it\\'s a key' ? 'key']")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(sql, parsed.parameterizedSql());
                assertEquals(0, parsed.parameterCount(), sql);
                assertArrayEquals(new int[0], parsed.positionalParameterOffsets(), sql);
            }
        }

        @Test
        void testParse_SubscriptJsonOperatorsPreservePositionalOperandOffsets() {
            for (final String sql : List.of("SELECT ARRAY[doc ? ?]", "SELECT ARRAY[doc ? ?::text]", "SELECT ARRAY[payload ? ? AND(flag)]",
                    "SELECT ARRAY[(doc) ? (?)]", "SELECT ARRAY[doc ?| ARRAY[?]]", "SELECT ARRAY[doc ?& ARRAY[?]]")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(sql, parsed.parameterizedSql());
                assertEquals(1, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }

            for (final String sql : List.of("SELECT ARRAY[? ? ?]", "SELECT ARRAY[docs[?] ? ?]")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(sql, parsed.parameterizedSql());
                assertEquals(2, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }

            final String chain = "SELECT ARRAY[? ? ? ? ?]";
            final ParsedSql chained = ParsedSql.parse(chain);
            assertEquals(3, chained.parameterCount());
            assertArrayEquals(new int[] { chain.indexOf('?'), chain.indexOf('?') + 4, chain.lastIndexOf('?') }, chained.positionalParameterOffsets());

            for (final String operator : List.of("@?", "?|", "?&", "?#", "?-", "?-|", "?||")) {
                assertEquals(0, ParsedSql.parse("SELECT ARRAY[doc " + operator + " 'key']").parameterCount(), operator);
            }
        }

        @Test
        void testParse_SubscriptPlaceholdersRespectOperatorAndKeywordBoundaries() {
            for (final String sql : List.of("SELECT ARRAY[x=? AND(y=?)] FROM t", "SELECT ARRAY[x=? OR(y=?)] FROM t", "SELECT ARRAY[(x=?)+(y=?)] FROM t")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(sql, parsed.parameterizedSql());
                assertEquals(2, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
            }

            for (final String sql : List.of("SELECT ARRAY[x=? AND(y=:y)] FROM t", "SELECT ARRAY[x=? OR(y=#{y})] FROM t")) {
                assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
            }
        }

        // Regression: an unclosed #{ inside a bracket group was joined with the tokens after the group, swallowing the rest of the SQL into its name.
        @Test
        void testParse_unclosedIbatisMarkerInsideBracketGroupIsRejected() {
            for (final String sql : List.of("SELECT ARRAY[#{a] FROM t WHERE b = #{c}", "SELECT ARRAY[#{a ] , #{b}", "SELECT x[#{a] FROM t WHERE b = #{c}",
                    "SELECT ARRAY[#{a] FROM t WHERE b = #{c} AND d = #{d}", "SELECT ARRAY[1, #{a] FROM t WHERE b = #{c}", "SELECT ARRAY[#{a]")) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse(sql), sql);
                assertTrue(e.getMessage().contains("missing closing '}'"), sql);
            }

            // A property path whose '}' follows its own bracket group is still one binding.
            ParsedSql parsed = ParsedSql.parse("SELECT * FROM t WHERE a = #{list[0]} AND b = #{map[k]} AND c = #{ list[1] }");
            assertEquals("SELECT * FROM t WHERE a = ? AND b = ? AND c = ?", parsed.parameterizedSql());
            assertEquals(List.of("list[0]", "map[k]", "list[1]"), parsed.namedParameters());

            parsed = ParsedSql.parse("SELECT x[1] FROM t WHERE a = #{ b }");
            assertEquals("SELECT x[1] FROM t WHERE a = ?", parsed.parameterizedSql());
            assertEquals(List.of("b"), parsed.namedParameters());
        }

        @Test
        void testQuotedIdentifierSubscriptChain_documentedLimitation() {
            // Documented: a subscript chain is only followed from an unquoted identifier root. After a quoted
            // identifier the chain stops and a following group is classified by the standalone rules.
            assertEquals(2, ParsedSql.parse("SELECT t.q[?]['a', ?] FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT \"q\"[?]['a', ?] FROM t").parameterCount());
            assertArrayEquals(new int[] { 11 }, ParsedSql.parse("SELECT \"q\"[?]['a', ?] FROM t").positionalParameterOffsets());

            // ... which does NOT mean the following group is ignored: a standalone "[?]" is a positional subscript
            // and is still counted, only a bracket-quoted "['a', ?]" is left alone.
            assertEquals(2, ParsedSql.parse("SELECT \"q\"[?][?] FROM t").parameterCount());
            assertArrayEquals(new int[] { 11, 14 }, ParsedSql.parse("SELECT \"q\"[?][?] FROM t").positionalParameterOffsets());
            assertEquals(3, ParsedSql.parse("SELECT \"q\"[?][?][?] FROM t").parameterCount());
            assertEquals(2, ParsedSql.parse("SELECT t.\"q\"[?][?] FROM t").parameterCount());

            // a backtick-quoted or bracket-quoted root behaves exactly like the double-quoted one
            assertEquals(2, ParsedSql.parse("SELECT `q`[?][?] FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT `q`[?]['a', ?] FROM t").parameterCount());
            assertEquals(2, ParsedSql.parse("SELECT [q][?][?] FROM t").parameterCount());
            assertEquals(1, ParsedSql.parse("SELECT [q][?]['a', ?] FROM t").parameterCount());
        }

        @Test
        void testSubscriptCommentsCannotExposeNamedMarkers() {
            for (final String comment : List.of("/* ] :ghost */", "/* [ ' :ghost */", "-- ] ' :ghost\n", "-- [ :ghost\r\n")) {
                final String sql = "SELECT ARRAY[1 " + comment + ", :id]";
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(List.of("id"), parsed.namedParameters(), sql);
                assertEquals(sql.replace(":id", "?"), parsed.parameterizedSql(), sql);
                assertEquals(1, parsed.parameterCount(), sql);
            }
        }

        @Test
        void testSubscriptCommentsCannotExposePositionalOrMyBatisMarkers() {
            for (final String comment : List.of("/* ] ? #{ghost} */", "/* [ ' ? #{ghost} */", "-- ] ' ? #{ghost}\n")) {
                final String positionalSql = "SELECT ARRAY[1 " + comment + ", ?], ?";
                final ParsedSql positional = ParsedSql.parse(positionalSql);
                final int firstBinding = positionalSql.indexOf(", ?") + 2;
                assertEquals(2, positional.parameterCount(), positionalSql);
                assertArrayEquals(new int[] { firstBinding, positionalSql.lastIndexOf('?') }, positional.positionalParameterOffsets(), positionalSql);
                assertEquals(positionalSql, positional.parameterizedSql(), positionalSql);

                final String namedSql = "SELECT ARRAY[1 " + comment + ", #{id}]";
                final ParsedSql named = ParsedSql.parse(namedSql);
                assertEquals(List.of("id"), named.namedParameters(), namedSql);
                assertEquals(namedSql.replace("#{id}", "?"), named.parameterizedSql(), namedSql);
            }
        }

        // Regression: a custom separator starting with whitespace (" AND") could not be realigned, so subscriptOpeningOffsets threw IllegalStateException.
        @Test
        void testSubscriptOpeningOffsets_customSeparatorStartingWithWhitespace() {
            final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator(" AND").build());
            final String sql = "SELECT a[1] FROM t WHERE b = 1 AND c[2] = 1";

            assertArrayEquals(new int[] { 8, 36 }, ParsedSql.subscriptOpeningOffsets(sql, tokenizer));
            assertArrayEquals(ParsedSql.subscriptOpeningOffsets(sql), ParsedSql.subscriptOpeningOffsets(sql, tokenizer));
            // A longer whitespace run before the separator token.
            assertArrayEquals(new int[] { 8, 37 }, ParsedSql.subscriptOpeningOffsets("SELECT a[1] FROM t WHERE b = 1  AND c[2] = 1", tokenizer));
        }

        // Regression: a line break or tab before a whitespace-led custom separator (" AND") broke the token realignment.
        @Test
        void testSubscriptOpeningOffsets_lineBreakOrTabBeforeWhitespaceLedSeparator() {
            final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.tokenizerConfigBuilder().withSeparator(" AND").build());

            assertArrayEquals(new int[] { 8, 37 }, ParsedSql.subscriptOpeningOffsets("SELECT a[1] FROM t WHERE b = 1\n AND c[2] = 1", tokenizer));
            assertArrayEquals(new int[] { 8, 37 }, ParsedSql.subscriptOpeningOffsets("SELECT a[1] FROM t WHERE b = 1\t AND c[2] = 1", tokenizer));
            assertArrayEquals(new int[] { 8, 44 }, ParsedSql.subscriptOpeningOffsets("SELECT a[1] FROM t WHERE b = 1 /* c */ AND c[2] = 1", tokenizer));
        }

        @Test
        void testSubscriptOpeningOffsetsShareParameterParsingRules() {
            final String sql = "SELECT scores[:a] /* [ignored] */ ['literal [text]', :b] FROM t WHERE id = :id";
            assertArrayEquals(new int[] { sql.indexOf('['), sql.indexOf("['literal") }, ParsedSql.subscriptOpeningOffsets(sql));

            final String nested = "SELECT ARRAY[ARRAY[:id]] FROM t";
            assertArrayEquals(new int[] { nested.indexOf('['), nested.lastIndexOf('[') }, ParsedSql.subscriptOpeningOffsets(nested));

            final String comments = "SELECT arr[/* [ignored] ' */ :id, '-- [literal]'] FROM t";
            assertArrayEquals(new int[] { comments.indexOf('[') }, ParsedSql.subscriptOpeningOffsets(comments));
            final String lineComment = "SELECT arr[1 -- [ignored] '\n, :id] FROM t";
            assertArrayEquals(new int[] { lineComment.indexOf('[') }, ParsedSql.subscriptOpeningOffsets(lineComment));

            assertEquals(0, ParsedSql.subscriptOpeningOffsets("SELECT [literal:param], t.[:param], 'arr[:param]', [#{param}] FROM t").length);
            assertEquals(0, ParsedSql.subscriptOpeningOffsets("SELECT ARRAY['a\\'', :id] FROM t").length);
            assertEquals(0, ParsedSql.subscriptOpeningOffsets("SELECT [a] [b:c] FROM t").length);

            final String standalone = "SELECT [:id], [?] FROM t";
            assertArrayEquals(new int[] { standalone.indexOf('['), standalone.lastIndexOf('[') }, ParsedSql.subscriptOpeningOffsets(standalone));

            final String custom = "SELECT  arr[:id]::jsonb FROM t";
            final SqlParser.Tokenizer tokenizer = SqlParser.tokenizer(SqlParser.TokenizerConfig.builder().withSeparator("::").build());
            assertArrayEquals(new int[] { custom.indexOf('[') }, ParsedSql.subscriptOpeningOffsets(custom, tokenizer));
        }

        @Test
        void testSubscriptOperatorsDoNotConsumeCommentOpeners() {
            for (final String sql : List.of("SELECT ARRAY[?-- ?\n, ?]", "SELECT ARRAY[?||/* ? */?]")) {
                final ParsedSql parsed = ParsedSql.parse(sql);
                assertEquals(2, parsed.parameterCount(), sql);
                assertArrayEquals(new int[] { sql.indexOf('?'), sql.lastIndexOf('?') }, parsed.positionalParameterOffsets(), sql);
                assertEquals(sql, parsed.parameterizedSql(), sql);
            }
        }
    }

    @Nested
    class SourceOffsetFallbacks {
        @Test
        void testBracketContextIgnoresClosingBracketsInComments() throws Exception {
            final var scan = ParsedSql.class.getDeclaredMethod("isInsideOpenBracketGroup", String.class, int.class);
            scan.setAccessible(true);
            for (final String token : List.of("array[/* ] */ :name]", "array[-- ignored ]\n:name]", "array[/* unclosed :name")) {
                assertEquals(true, scan.invoke(null, token, token.indexOf(":name")), token);
            }
            final String closed = "array[0] :name";
            assertEquals(false, scan.invoke(null, closed, closed.indexOf(":name")));
        }

        @Test
        void testDroppedCommentScannerHonorsTheRequestedGapBoundary() throws Exception {
            final var scan = ParsedSql.class.getDeclaredMethod("firstLineBreakInDroppedBlockComment", String.class, int.class, int.class);
            scan.setAccessible(true);
            for (final String gap : List.of(" \t", "/* no line break */", "-- no line break", "# no line break")) {
                assertEquals(false, scan.invoke(null, gap, 0, gap.length()), gap);
            }
            final String unclosed = "/* first\nsecond";
            assertEquals(true, scan.invoke(null, unclosed, 0, unclosed.length()));
            final String closed = "/* first\nsecond */";
            assertEquals(true, scan.invoke(null, closed, 0, closed.indexOf("second")));
            assertEquals(false, scan.invoke(null, "\n/* second\n */", 0, 14));
        }

        @Test
        void testLineBreakScannerRequiresFollowingNonWhitespace() throws Exception {
            final var scan = ParsedSql.class.getDeclaredMethod("hasTextAfterLineBreak", String.class, int.class);
            scan.setAccessible(true);
            for (final String text : List.of("", "same line", "first\n \t\r")) {
                assertEquals(false, scan.invoke(null, text, 0), text);
            }
            assertEquals(true, scan.invoke(null, "first\r\n SELECT", 0));
        }

        @Test
        void testMybatisContinuationRejectsUnclosedCommentsAndNestedMarkers() throws Exception {
            final var scan = ParsedSql.class.getDeclaredMethod("endsInUnclosedIbatisMarker", String.class);
            scan.setAccessible(true);
            assertEquals(true, scan.invoke(null, "#{name /* unfinished"));
            assertEquals(false, scan.invoke(null, "#{name, #{nested}}"));
            assertEquals(false, scan.invoke(null, "#{name, metadata='#{quoted}'}"));
            assertEquals(true, scan.invoke(null, "#{first}#{ second"));
        }

        @Test
        void testNamedParameterScannerStopsAtMissingOrInvalidSegments() throws Exception {
            final var scan = ParsedSql.class.getDeclaredMethod("findNamedParameterEndIndex", String.class, int.class);
            scan.setAccessible(true);
            assertEquals(1, scan.invoke(null, ":", 1));
            assertEquals(1, scan.invoke(null, ":1", 1));
            assertEquals(5, scan.invoke(null, ":name.", 1));
            assertEquals(10, scan.invoke(null, ":name.part", 1));
        }

        @Test
        void testQuestionMarkOffsetFastPathsRejectMismatchedCounts() throws Exception {
            final var scan = ParsedSql.class.getDeclaredMethod("allQuestionMarkOffsets", String.class, int.class);
            scan.setAccessible(true);
            assertArrayEquals(new int[] { 0, 1 }, (int[]) scan.invoke(null, "??", 2));
            assertArrayEquals(new int[] { 7 }, (int[]) scan.invoke(null, "SELECT ?", 1));
            assertNull(scan.invoke(null, "??", 1));
            assertNull(scan.invoke(null, "?", 2));
            assertNull(scan.invoke(null, "SELECT 1", 1));
            assertNull(scan.invoke(null, "SELECT ?, ?", 1));
        }

        @Test
        void testSourceAlignmentRejectsMissingTokensInsteadOfGuessingOffsets() throws Exception {
            final var align = ParsedSql.class.getDeclaredMethod("alignTokenSourceOffsets", String.class, List.class);
            final var end = ParsedSql.class.getDeclaredMethod("endOfLastAlignedToken", List.class, int[].class);
            final var resolve = ParsedSql.class.getDeclaredMethod("resolvePositionalParameterOffsets", String.class, List.class, IntList.class, IntList.class);
            align.setAccessible(true);
            end.setAccessible(true);
            resolve.setAccessible(true);
            final String sql = "SELECT /* ignored ? */ ?";
            assertArrayEquals(new int[] { 0, -1, sql.lastIndexOf('?') }, (int[]) align.invoke(null, sql, List.of("SELECT", " ", "?")));
            assertNull(align.invoke(null, sql, List.of("SELECT", "missing")));
            assertEquals(-1, end.invoke(null, List.of(" ", ""), new int[] { -1, -1 }));
            assertNull(resolve.invoke(null, sql, List.of("missing", "?"), IntList.of(1), IntList.of(0)));
            assertNull(resolve.invoke(null, "SELECT ?", List.of("SELECT", "?"), IntList.of(3), IntList.of(0)));
        }

        @Test
        void testSqlExpressionWordsRequireValidUnicodeCodePoints() throws Exception {
            final var scan = ParsedSql.class.getDeclaredMethod("startsWithSqlExpressionWord", String.class);
            scan.setAccessible(true);
            for (final String word : List.of("\uD800", "\uDC00", "\uD800x", new String(Character.toChars(0x1F600)))) {
                assertEquals(false, scan.invoke(null, word), word);
            }
            for (final String word : List.of(new String(Character.toChars(0x10400)), new String(Character.toChars(0x1D7CE)))) {
                assertEquals(true, scan.invoke(null, word), word);
            }
        }
    }

    @Nested
    class Validation {
        @Test
        void testInternalScannersRetainTheirNonnullSourcePrecondition() {
            // Public entry points validate SQL; these package helpers retain their direct-dereference contract.
            assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets(null));
            assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets(null, null));
        }

        @Test
        void testParse_blankSql_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("   \t"));
        }

        @Test
        void testParse_controlCharactersOnly_throws() {
            // String.trim() strips control characters that Strings.isBlank does not treat as blank.
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("\u0001"));
            assertThrows(IllegalArgumentException.class, () -> ParsedSql.parse("\u0000 \u0001"));
        }

        @Test
        void testParse_EmptyThrows() {
            assertThrows(IllegalArgumentException.class, () -> {
                ParsedSql.parse("");
            });
        }

        @Test
        void testParse_NullThrows() {
            assertThrows(IllegalArgumentException.class, () -> {
                ParsedSql.parse(null);
            });
        }

        @Test
        void testSubscriptScanningDoesNotRequireAnUnusedTokenizer() {
            assertArrayEquals(new int[0], ParsedSql.subscriptOpeningOffsets("", null));
            assertArrayEquals(new int[0], ParsedSql.subscriptOpeningOffsets("SELECT 1", null));
            assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets("SELECT arr[?]", null));
            assertThrows(NullPointerException.class, () -> ParsedSql.subscriptOpeningOffsets("SELECT '['", null));
        }
    }

    private static void assertCall(final String sql, final String expectedSql, final List<String> expectedNames) {
        final ParsedSql parsed = ParsedSql.parse(sql);
        assertTrue(parsed.isDataOperation(), sql);
        assertEquals(expectedSql, parsed.parameterizedSql(), sql);
        assertEquals(expectedNames, parsed.namedParameters(), sql);
    }

    private static void assertOffsets(final String sql, final int... offsets) {
        final ParsedSql parsed = ParsedSql.parse(sql);
        assertTrue(parsed.isDataOperation(), sql);
        assertEquals(offsets.length, parsed.parameterCount(), sql);
        assertArrayEquals(offsets, parsed.positionalParameterOffsets(), sql);
    }
}
