# SqlParser and ParsedSql performance implementation

This report preserves the first implementation snapshot and its measurements. The [follow-up fixes and measurements](../parser-followup-2026-10-15/README.md) address the subsequently reported Unicode regression, overlapping bracket regions, incremental memo growth, and cursor cleanup. Its source hashes identify the current follow-up snapshot; the hashes here remain historical.

Implemented the [line-by-line review](../parser-review-2026-10-15/README.md) using three specialist agents and independent cross-review. Production changes are confined to `SqlParser` and `ParsedSql`, with regression tests and generated API documentation.

Baseline: commit `33b655b87de09b33c4cad4a711956f0b93ff266a`. The baseline classes were compiled and preserved before implementation. [Source hashes](source-hashes.json) identify the implemented source and tests; baseline hashes remain in the review directory.

## Changes

| Review item | Implementation | Behavioral constraints |
| --- | --- | --- |
| F01: repeated stateless token walks | Added `SqlParser.cursor(sql)`, `Tokenizer.cursor(sql)`, and `TokenCursor`. A cursor retains lexical memo state and materializes token text only when requested. | Existing stateless APIs remain compatible. Callers must use the cursor to obtain shared context across calls. |
| F02: bracket regions converging after comments | Reuse negative results for convergent first-word positions; keep the common single-position case without a set. | Both flat and nesting comment readings remain active. A positive mutation result still rejects immediately. |
| F03: repeated malformed MyBatis openers | Cache the next `}`, CR, LF, or end-of-input within each masking pass. | A marker spanning a line break remains visible under the same rules as before. |
| F04: memo allocation proportional to unused tails | Cursor and stateless lookup tables grow with the visited context. Full tokenization and classifier paths allocate each required table once at source length. Block-opener scanning advances only to the requested position. | Tables remain lazy. Previously computed answers and distinct comment readings survive growth. |
| F05: repeated classification of equivalent masks | Reuse the latest accepted mask per default comment policy. | Every masking pass, remasking consistency check, validity result, and dialect veto still runs. |
| F06: copies of unchanged masks | Allocate a mask only when a character actually changes. | CR/LF preservation, delimiter rewrites, and immediate lexing-error exits retain their behavior. |
| F07: duplicate normalized SQL strings | Compare the normalized builder range before materialization; reuse the original immutable string when equal. | Existing constructor trimming and `Character.isWhitespace(char)` normalization remain distinct. |
| F08: duplicate positional-token bookkeeping | Compact accepted entries in the classifier-owned array; wrap its accepted prefix only for fallback offset resolution. | Original SQL offsets, bracket parameters, mixed-style errors, and public defensive copies remain intact. |
| F09: unused DDL reconstruction | Return normalized original SQL after tokenization identifies an unrecognized data operation. | Tokenization still runs. Such statements retain their marker text and empty parameter metadata. |
| F10: temporary comment-boundary strings | Match separators across existing token ranges and the source SQL. | Custom/Unicode separators, longest matches, and overlapping comment openers remain compatible. |
| F11: unused parenthesized-group state | Allocate companion state only when a nested mutation candidate needs it. | CTE and nested mutation detection remain active. |

Additional changes use the existing ASCII classification table during tokenization, handle a whole-token valid named parameter without an intermediate marker array/builder, and delegate `ParsedSql.hashCode()` to the immutable SQL string's existing lazy hash cache.

Private named-parameter lists start at capacity two and compact larger backing arrays before publication. A bounded, construction-local map shares repeated name strings for large lists when an early repeat is found: at least 32 occurrences, the first name repeated among the first 16 entries, and at most 16 canonical names. Every binding occurrence remains in order. This deliberately does not promise deduplication for every name distribution.

## Cursor use

```java
SqlParser.TokenCursor cursor = SqlParser.cursor(sql);
while (cursor.next()) {
    int start = cursor.startIndex();
    int end = cursor.endIndex();
    // Request cursor.token() only if the token text is needed.
}
```

A configured tokenizer also exposes `tokenizer.cursor(sql)`. The cursor follows the existing next-token APIs' whitespace/comment rules and is mutable and not thread-safe. With the follow-up fix, it releases its memo when `next()` first returns `false`; its source and configuration remain until the cursor is released. The tokenizer facade remains reusable and thread-safe. Accessing token text or bounds before a successful `next()` or after exhaustion throws `IllegalStateException`; repeated `next()` calls after exhaustion return `false`.

## Correctness validation

- Compiled all 82 production sources and 88 test sources with `--release 21` and the locally installed dependencies.
- Ran the complete configured `AbacusQueryTestSuite`: **6,252 passing invocations**, no failures, errors, skipped or aborted tests. This includes 24 added regression methods; the original suite passed 6,228 invocations.
- Compared **101,530 deterministic records byte-for-byte** with the preserved baseline: 42,000 tuning records, 24,000 classifier records, and 35,530 additional edge records. These include token text, classification, normalized SQL, names/counts, exception messages and positional offsets. The edge corpus also checks cursor text and bounds against stateless walks.
- Added tests cover cursor lifecycle and independent positions, custom separators, memo growth and lazy full-scan allocation, convergent brackets, malformed marker scaling, lexical-mode vetoes, mutation companion state, Unicode names, mixed styles, unchanged-string sharing, and positional-array ownership.
- Preserved the no-hash remasking counterexample: masking a parameter in `SELECT :x$tag$ abc x$tag$` exposes a dollar-quote boundary. The second consistency mask is still necessary and still rejects this shape.
- Javadoc with `-Xdoclint:all` produced no diagnostics. Regenerated `docs/ai/API.md` and `docs/ai/api-index.json`.
- A javac-AST check validated 6,004 test methods in 88 files with zero naming, visibility or ordering issues. This substitutes for the repository's Python layout checker because the installed launcher had no Python runtime.

See [validation.json](validation.json), [memory-after.txt](memory-after.txt), and [remask-after.txt](remask-after.txt). Raw build/test/differential outputs are under `.audit/parser-implementation-2026-10-15/`. The configured suite excludes the existing `slow-test` tag. Direct `javac` and the JUnit console runner were used because offline Maven could not resolve its publishing plugin in this environment; this is not a successful Maven lifecycle claim.

## Measured results

Time is the median of three JVM medians; allocation is bytes per operation. These are comparisons of the same API on the same prepared inputs unless the cursor table explicitly says otherwise. Every case and fork is available in [comparison.csv](comparison.csv), [comparison.json](comparison.json), [supplement-comparison.csv](supplement-comparison.csv), and [cursor-summary.csv](cursor-summary.csv).

| Case | Baseline time | Implemented time | Baseline bytes | Implemented bytes |
| --- | ---: | ---: | ---: | ---: |
| Early hash lookup with block comment and 1 MiB unused tail | 1.493 ms | 0.215 µs | 9,437,488 | 720 |
| 8,192 bracket openers converging on a shared suffix | 103.953 ms | 0.747 ms | 986,456 | 789,344 |
| 16,384 malformed multiline markers, no closing brace | 17.747 ms | 3.449 ms | 296,080 | 296,160 |
| 16,384 malformed multiline markers, final closing brace | 19.232 ms | 3.369 ms | 296,080 | 296,160 |
| Read classification of a 1 MiB quoted literal | 3.517 ms | 3.806 ms | 6,291,944 | 3,146,120 |
| Parse unrecognized DDL with a 1 MiB literal | 491.788 µs | 240.003 µs | 4,243,968 | 1,081,664 |
| Tokenize 1,024 removed-comment boundaries | 82.167 µs | 40.999 µs | 138,368 | 15,488 |
| Classify 1,024 ordinary parenthesized expressions | 411.919 µs | 390.025 µs | 33,120 | 352 |
| Classify 1,024 expressions with mode features only in a comment | 7.464 ms | 0.705 ms | 37,008 | 9,192 |
| Parse 1,024 positional parameters | 139.850 µs | 135.763 µs | 64,488 | 48,944 |
| Parse 1,024 repeated named parameters | 162.519 µs | 126.470 µs | 339,912 | 137,496 |

The supplementary distributions support keeping the bounded name-sharing heuristic: distinct colon names allocate 342,968 → 133,000 bytes with a 1.50× time ratio; an early repeat followed by unique names allocates 342,968 → 133,784 bytes with a 1.50× ratio. Repeated MyBatis names allocate 343,984 → 321,792 bytes with effectively unchanged runtime (1.01×); unique MyBatis names allocate 348,888 → 326,536 bytes with a 1.09× ratio. These are whole-implementation comparisons, not isolated attribution of gains to the heuristic.

The cursor improvement requires using the new API. At 1,024 elements, comparing the **implemented stateless bounds walk** with the **implemented bounds-only cursor**:

| Input | Stateless time | Cursor time | Stateless bytes | Cursor bytes |
| --- | ---: | ---: | ---: | ---: |
| Hash-prefixed table names | 55.846 ms | 0.177 ms | 49,302,880 | 125,616 |
| Bracket-quoted names and aliases | 15.931 ms | 0.094 ms | 15,214,056 | 62,544 |
| Chained subscripts | 4.619 ms | 0.144 ms | 2,228,608 | 12,512 |

The hash cursor takes 0.177, 0.370, 0.707 and 1.317 ms at 1,024, 2,048, 4,096 and 8,192 elements respectively, consistent with roughly linear scaling over this range. Requesting text for every token at 1,024 elements uses 174,864 bytes instead of the bounds-only cursor's 125,616 bytes. The old stateless API still discards context between calls; its repeated full walk is not made linear by this patch.

The [structural probe](memory-after.txt) separately confirms retained-memory changes. Unchanged 28- and 4,007-character statements share their original/parameterized String and byte-array identities. A cacheable 4,010-character statement with 1,001 repeated `:id` markers retains one name String and one name byte array instead of 1,001 of each; its backing list capacity drops from 1,234 to 1,001. This is structural evidence, not a heap-size estimate or a claim that every temporary name substring was eliminated.

### Controls and tradeoffs

Full tokenization of 1,024 hash names stays near baseline: 190.256 → 191.125 µs, 167,704 → 167,712 bytes. The plain classifier control stays near baseline too: 89.184 → 88.163 µs, 8,704 → 8,712 bytes. The added memo fields account for small fixed allocation changes. Marker delimiter memoization adds 80 bytes in the large malformed-marker cases while removing their repeated suffix scans.

Lower allocation does not imply uniformly lower CPU time. The 1 MiB quoted-literal case allocates half as much but its measured median is about 8% slower; the fork ranges overlap (baseline 3.310–3.981 ms, implementation 3.770–4.033 ms). The 4 KiB unrecognized-DDL case rises from 1.319 to 1.532 µs with unchanged allocation, whereas larger DDL cases improve. The 11-character early-hash control rises from 109 to 122 ns. These costs are retained in the evidence rather than hidden by the larger wins.

Growing cursor/lookup tables can still create discarded arrays during a dense walk; their retained capacity is bounded by SQL length. The full-scan allocation policy prevents that copying in tokenization/classification. Classification may retain up to two accepted masks during one call. Named-parameter canonicalization shares retained strings only for the selected distributions, and its temporary map never escapes construction. No cache capacity, expiry, admission limit, or concurrency policy changed.

## Measurement method

The original [95-case probe](../parser-review-2026-10-15/ParserReviewProbe.java), the [12-case parameter-distribution probe](SupplementaryParsedSqlProbe.java), and the [29-case cursor probe](CursorProbe.java) run sequentially after builds and tests finish. No review-owned compile or test process runs during measurement. The common probe uses exactly the same compiled harness against baseline and implementation classes.

Each case has at least 300 ms warmup, adaptive batches targeting 5 ms (cap 32,768 operations), and seven measured rounds. There are three fresh JVMs per version, with pair order before/after, after/before, before/after. The common and supplementary probes perform two mixed-path preparation passes. Cursor token counts are validated before its per-case warmup. JVM flags are `-Xms256m -Xmx1024m -Xbatch`; the runtime is Oracle JDK 25 on an AMD Ryzen 7 PRO 7840U. SQL inputs are constructed before timing, and uncached parsing uses the null-cache overload. Allocated bytes come from `ThreadMXBean`; results are retained through volatile sinks. Medians of rounds are computed per JVM, then medians of those JVM medians are reported, with each fork retained in JSON.

These are diagnostic microbenchmarks, not JMH or whole-application measurements. Inputs are reused, so construction and the first input-string hash computation are outside steady-state timing. They do not measure cache-hit throughput, concurrent misses, cold class initialization, peak live heap, or production latency. Runtime variability should be considered alongside the more stable allocation counts.

Default dependency logging remains enabled in the common harness. Baseline `Objectory` builder-creation diagnostics appear during DDL warmup and some measured rounds; these are debug stack traces explicitly labeled as non-errors, and every JVM completed successfully. The DDL timings therefore include that default-logging behavior rather than isolating pure computation. Raw logs and all samples are retained.

## Allocation-policy adjustment

The first implementation grew every memo table geometrically. Its exploratory run improved early lookups, but full-scan allocation rose: `tokenize_hash(1024)` went from 167,704 to 208,576 bytes, and `mode_plain(1024)` from 8,704 to 24,936 bytes. The final implementation uses lazy, single-allocation full-size tables for tokenization/classification and incremental tables for cursors and individual lookups. The exploratory samples are preserved under [initial-geometric-memos](initial-geometric-memos/) and are excluded from final summaries.

## Reproduction

Keep separately compiled baseline and implementation classes. The baseline must precede these source edits; never overwrite its output directory. The review README gives the baseline compilation/measurement procedure and exact dependencies. Compile the new probes into a separate probe directory: `ParserEdgeDifferential` and `SupplementaryParsedSqlProbe` compile against the baseline API, while `CursorProbe` requires the implemented API.

From the repository root, with those class directories present:

```powershell
./docs/performance/parser-implementation-2026-10-15/run-probes.ps1
node docs/performance/parser-implementation-2026-10-15/summarize.mjs
node docs/performance/parser-implementation-2026-10-15/collect-evidence.mjs
```

The runner accepts alternative baseline/current class directories, probe directories, and a dependency-classpath file. `summarize.mjs` checks case/result consistency and all 21 samples per case/version. `collect-evidence.mjs` checks source hashes, suite XML, differential hashes, cursor validation, Javadoc and layout logs; it refuses to bless later source changes silently.
