# SqlParser and ParsedSql performance and memory review

Three specialist agents read all 11,751 lines of the two classes; the coordinating reviewer cross-checked the main findings, callers, dependency behavior, and evidence. Coverage includes comments, public contracts, nested classes, and executable code. This is the historical review of commit `33b655b87de09b33c4cad4a711956f0b93ff266a`. Production and test sources were unchanged during this review. Exact baseline source and test hashes are in [source-hashes.json](source-hashes.json).

The subsequent implementation, compatibility validation, and before/after measurements are recorded in the [implementation report](../parser-implementation-2026-10-15/README.md). Later reported regressions and remaining allocation/scaling issues are addressed in the [follow-up report](../parser-followup-2026-10-15/README.md). The findings and measurements below preserve the original baseline.

Each specialist also independently checked the completed report against source and measured evidence. All three final checks passed without material corrections.

The largest remaining costs are repeated prefix/suffix scanning in `SqlParser`, large per-source memo arrays, repeated classification of identical lexical output, and unnecessary temporary or retained objects in `ParsedSql`. Earlier range-based scanning, quote scanning, hash memoization, and parameter-array tuning are already present and were not counted as new recommendations.

The configured full suite passed **6,228 tests, zero failures/errors/skips** after freshly compiling 82 production and 88 test sources with JDK 25 targeting Java 21, using abacus-common 8.2.0 and JUnit 6.1.3. The configured suite excludes the `slow-test` tag. Maven's offline invocation could not resolve its publishing extension in this sandbox, so verification used direct compilation and JUnit ConsoleLauncher. Java also required execution outside the filesystem sandbox to resolve classpath paths. See [validation.txt](validation.txt) for the successful verification record.

Measurements cover **95 cases, three sequential JVM forks, seven rounds per case: 1,995 final samples**. They characterize current costs, not speedups from unimplemented fixes. Allocation is thread-allocated bytes per operation, not retained heap or process memory. Structural probes separately establish retained string/array identities. Timing has substantial fork variance; use the recorded ranges, source-level complexity, and allocation together.

| ID | Priority | Finding | Evidence |
| --- | --- | --- | --- |
| F01 | P2, first group to address | Public token-by-token iteration recreates context and becomes quadratic | Source analysis; scaling and allocation |
| F02 | P2, first group to address | Raw bracket checks repeatedly scan the same candidate region | Source analysis; public predicate reproduction |
| F03 | P2, first group to address | Rejected MyBatis markers repeatedly search suffixes for a closer | Source analysis; public predicate reproduction |
| F04 | P2 | Early token lookup allocates memo arrays for an unused tail | Source analysis; allocation measurement |
| F05 | P2 | Identical lexical output receives repeated complete classification | 96-pass source trace; measured workload |
| F06 | P2 | Remasking already hidden text copies the whole SQL again | Source analysis; allocation; safety counterexample |
| F07 | P2 | Unchanged rendered SQL retains duplicate text | String and backing-array identity probe |
| F08 | P2 | Ordinary positional indexes are copied into unused fallback metadata | Exact data-flow and dependency-source check |
| F09 | P2 | Non-data statements reconstruct SQL that is discarded | Exact data flow; large-DDL allocation |
| F10 | P2/P3 | Comment-boundary checks create temporary builders and strings | Source analysis; comment-heavy allocation |
| F11 | P2/P3 | Ordinary expression groups allocate unused classifier state | Source analysis; allocation scaling |

P2 means a concrete, workload-dependent optimization worth addressing. P3 means a lower-priority opportunity whose implementation cost needs stronger workload justification. No proposed change has been claimed to pass regression tests: the tested implementation is the current one.

## F01: retain scanning context across a whole token walk

Locations: [SqlParser.java:1553](../../../src/main/java/com/landawn/abacus/query/SqlParser.java#L1553), lines 1604-1638, 1654-1657, 1106-1187 and 2900-2934.

Every public `nextToken` or `nextTokenEndIndex` call starts with a null memo. A contextual hash name or bracket can then allocate a new `HashScanMemo` and reconstruct context from the beginning. A caller following the documented end-index iteration pattern therefore loses the memo between tokens. Full `tokenize` calls already share their memo; this particular problem is not the path used by the `ParsedSql` constructor.

Triggers include `SELECT * FROM #t0, #t1, ...`, SQL Server columns and aliases such as `SELECT [order-id0] [alias-id0], ...`, and `SELECT x[0] [1] [1] ...`. Hyphenated bracket names fail the simple local reset check, forcing a prefix scan. The existing bracket-walk linearity test uses simple `[c0] [a0]` names and misses this case.

| Temporary tables in a complete end-index walk | SQL characters | Allocated B/walk | Median ms/walk |
| ---: | ---: | ---: | ---: |
| 256 | 1,694 | 3,766,272 | 3.46 |
| 512 | 3,486 | 15,265,792 | 14.22 |
| 1,024 | 7,094 | 61,587,456 | 75.19 |

The 1,024-table fork medians ranged from 54.08 to 101.59 ms. Allocations approximately quadruple when the number of targets doubles. For comparison, a whole `tokenize` on the same 1,024-table input allocated 167,704 B. The APIs return different representations, so this comparison is context, not a drop-in replacement benchmark. Walking 1,024 hyphenated column/alias pairs allocated 30,441,472 B; walking 1,024 continued subscript groups allocated 4,317,272 B.

Add an explicit per-SQL cursor/iterator that returns text and bounds from one scan while retaining a memo. This is an additive API direction; the stateless methods cannot transparently share arbitrary caller state. Expand local bracket fast paths only where their correctness is established. Avoid a global or thread-local memo that retains arbitrary input strings. Test whole walks against tokenization, including temporary tables, comments, hyphenated identifiers, doubled bracket closers, and true subscript chains. The probe already verifies walker/tokenizer text agreement before measurement.

## F02: memoize repeated bracket-region answers

Location: [SqlParser.java:4040](../../../src/main/java/com/landawn/abacus/query/SqlParser.java#L4040), especially 4045-4050 and 4070-4116.

The raw CockroachDB statement-source check claims its bounded regions cannot overlap. Different bracket openers can actually skip comments and converge on the same first word:

```java
String sql = "SELECT '" + "[/*".repeat(n) + "*/ UPDATE " + "x".repeat(n) + "]'";
SqlParser.isSyntacticallyReadQuery(sql);
```

Each flat comment lookup reaches the same `UPDATE`, after which the region check scans the same `n`-character tail. Comment-end caching does not cache that answer. This is quadratic despite the surrounding SQL being a quoted literal. The public predicate returned true at every measured size.

At `n=4,096`, the 16,404-character input took a median 26.18 ms; at `n=8,192`, the 32,788-character input took 168.80 ms, with fork medians 98.58-177.08 ms. Timing variance is significant, but the overlapping suffix scans are explicit in the source. The existing repeated-`[/*` test closes the comment immediately before the literal ends and does not exercise this shared candidate region.

Memoize the answer by actual region start, or retain region word facts so repeated starts do not repeat the scan. The function's result depends on the SQL, region start, and `allowInsert`; a memo must preserve those inputs. Keep both flat and nested comment readings and the raw-text rejection policy. Add this accepted case and rejected variants containing `RETURNING`, a `WITH` mutation, and `INSERT ... UPDATE` under the appropriate predicate.

## F03: bound and reuse failed MyBatis closer searches

Locations: [SqlParser.java:4701](../../../src/main/java/com/landawn/abacus/query/SqlParser.java#L4701), lines 4702-4708 and 4893-4895; helper callers at 4579 and 4661.

The classifier searches all remaining SQL for `}` before checking whether a line break already disqualifies a marker. A rejected opener leaves later openers to repeat the same suffix search:

```java
String sql = "SELECT 1\n" + "#{\n".repeat(n);
// Also reproduce with a final '}' appended.
SqlParser.isSyntacticallyReadQuery(sql);
```

Both public probe families returned true under the current comment/lexical contract. The no-closer family took 1.73 ms at 4,096 markers and 16.32 ms at 16,384 markers; the latter fork range was 15.88-16.36 ms. Source-level work is quadratic: every opener searches a suffix whose total lengths sum quadratically.

Stop a candidate search at the first `}`, CR, or LF, and reuse next-delimiter/failure information when multiple openers occur on the same line. Merely replacing `indexOf` with a bounded loop does not solve repeated same-line unterminated openers. Do not reject or consume more input as a shortcut: malformed markers must continue exposing the same dialect-specific SQL. Cover multiline and same-line failures, a final closer, valid metadata, and statements following a line break.

## F04: size memo storage to visited context

Locations: [SqlParser.java:2750](../../../src/main/java/com/landawn/abacus/query/SqlParser.java#L2750), lines 2790-2793, 2876-2892, 2900-2903, 2956-2963, 2982-2989 and 3028-3031.

Memo arrays are lazy by feature, but each allocation uses the full SQL length. One near-start hash target can allocate `int[n]` for line-comment coverage and `byte[n]` for hash state. A preceding block comment additionally initializes an `int[n]` nearest-opener table by scanning the entire source, even for an early-token request.

```java
String sql = "FROM /*x*/ #temp " + "x".repeat(tailLength);
SqlParser.nextTokenEndIndex(sql, 4); // always returns 16
```

| Unused tail characters | Allocated B/call |
| ---: | ---: |
| 0 | 304 |
| 1,024 | 9,520 |
| 65,536 | 590,128 |
| 1,048,576 | 9,437,488 |

Without the block-comment prefix, the 1 MiB-tail case still allocated 5,243,064 B. These are temporary arrays, not a persistent memory leak.

Grow tables through the highest requested position or use chunked/sparse representations for sparse events. Make the block-opener pass incremental or store ordered opener positions. Preserve nearest *raw* opener semantics, including quoted or malformed comment fragments. Removing memoization would reintroduce earlier complexity bugs; the goal is to retain its amortized benefit while reducing its initial footprint.

## F05: reuse classification of identical lexical output

Locations: [SqlParser.java:3881](../../../src/main/java/com/landawn/abacus/query/SqlParser.java#L3881), lines 3884-3912, 4418-4445 and 4898-4934.

Raw feature detection can activate lexical modes for characters entirely inside an inert block comment:

```sql
SELECT 1 /* \ ' " ` E' $ [ -- */
```

This activates axes 63: 48 valid quote/bracket/dollar combinations, multiplied by two dash-comment policies. All **96 default masking passes return the original String**, yet each repeats complete statement classification. This is a large constant factor, not exponential growth with SQL length.

With 1,024 ordinary projection elements, changing an inert `/* plain */` comment to this feature-rich comment changed the observed median from 0.103 ms to 14.64 ms. Corresponding fork ranges were 0.091-0.178 ms and 7.68-14.97 ms. The projected query shape and acceptance result are unchanged. These numbers measure the current overhead; they do not establish the speedup from caching only the classification phase, since masking itself must still run.

Reuse an acceptance result for identical masked input with the same comment policy, veto semantics, and tokenizer configuration. A tiny per-call identity/content cache avoids retaining every mask. Preserve every mode's validity check and every dialect veto. Do not delete modes based solely on this example; features outside comments can change parsing independently.

## F06: avoid copying already masked ranges, while preserving remask validation

Locations: [SqlParser.java:3905](../../../src/main/java/com/landawn/abacus/query/SqlParser.java#L3905), lines 4562, 4813 and 5305-5318.

The second masking pass validates that a changed lexical representation is stable. `maskRange` allocates a full `char[]` whenever a requested range is nonempty, even when every character in it is already a space or newline. The caller then creates another full String. A `SELECT` containing a 1 MiB ordinary literal allocated **6,291,944 B/classification**; the two full masks explain the dominant approximately six-bytes-per-input-character payload on this Latin-1 sample.

Make the initial copy conditional on finding a character that would actually change. Preserve the null sentinel for unchanged input and audit direct array writes and `new String(maskRange(...))` callers. The current direct comma write follows masking a literal `[`, underscore writes explicitly ensure an array, and the five immediate String-construction exits include a nonblank delimiter/prefix. Regression coverage should pin those preconditions.

**Do not restrict the second pass to SQL containing `#`.** Cross-review disproved that tempting shortcut: with dollar-quote mode enabled, `SELECT :x$tag$ abc x$tag$` first becomes `SELECT : $tag$ abc x$tag$`; the erased parameter name exposes a dollar-quote opener, so remasking changes the result. [RemaskBoundaryProbe.java](RemaskBoundaryProbe.java) asserts this against the current private scanner; [remask-boundary.txt](remask-boundary.txt) records the passing counterexample. The validation itself has value beyond the hash-related comment describing it.

## F07: reuse original text when rendering is unchanged

Locations: [ParsedSql.java:260](../../../src/main/java/com/landawn/abacus/query/ParsedSql.java#L260), lines 617-629.

For recognized data operations, `sb.toString()` creates the rendered SQL even when its final content equals `this.sql`. Both fields then retain separate String objects and separate backing arrays. The structural probe confirmed this for a 28-character positional statement and a 4,007-character no-binding statement. Non-data operations already reuse the original text when normalization leaves it unchanged.

Reuse `this.sql` when final content matches. Comparing the normalized builder range before materialization can avoid the temporary copy too; equality reuse after materialization saves retention only. A 4,007-byte redundant payload in 8,000 distinct cached entries would total about 30.6 MiB before object headers; this is an illustrative capacity calculation, not a measured live-heap total. The backing representation and duplicate-string deduplication settings affect real heap cost.

Preserve all output transformations: comment removal, parameter substitution, trailing semicolon stripping, Unicode whitespace, adjacent literal newlines, H2 `//`, and pgJDBC escape separation. Measure the added comparison's cold-parse cost as well as retained memory. Structural evidence is in [memory-evidence.txt](memory-evidence.txt).

## F08: reuse the classifier's ordinary positional metadata

Locations: [ParsedSql.java:327](../../../src/main/java/com/landawn/abacus/query/ParsedSql.java#L327), lines 372-377, 637-639 and 1172-1205.

`QuestionMarkClassifier` returns an owned array of positional token indexes. The constructor copies each ordinary binding's index into a second growable `IntList`. When `allQuestionMarkOffsets` succeeds, that second collection is never read. The exact installed `IntList` source confirms backing-array allocation and growth, so this is concrete redundant bookkeeping on ordinary uncached positional SQL.

Let fallback alignment consume the classifier's existing array with the appropriate consumed/skipped-token accounting, or build fallback metadata only after the direct-offset path fails. Do not discard information required when a MyBatis continuation joins tokens. Keep ordinary/subscript ordering and quoted/comment/operator question-mark exclusions intact.

The current probe's 1,024-positional-binding parse allocated 64,488 B in total; that is not the estimated savings from this change. No isolated patched variant was measured. Validate exact source offsets with and without quoted or operator question marks, mixed ordinary/bracket bindings, comments, joined markers, and raw subquery composition through `QueryUtil` and `AbstractQueryBuilder`.

## F09: skip reconstruction of non-data SQL

Locations: [ParsedSql.java:302](../../../src/main/java/com/landawn/abacus/query/ParsedSql.java#L302), lines 338-346, 593 and 617.

After operation resolution establishes `isOpSqlPrefix == false`, the constructor still allocates/borrows a builder, traverses all tokens, and appends each word. At line 617 it ignores that output and normalizes `this.sql` instead. Large DDL, SET, GRANT, and other unrecognized statements pay this repeatedly above the 4,096-character cache limit.

The large-DDL probe with a 1 MiB default literal allocated 4,243,968 B/parse. That total includes tokenization and other work; it is not all recoverable by removing reconstruction. `Objectory.recycle` rejects builders above its buffer-size limit, so the issue is transient growth and repeated work, not indefinite retention of oversized pooled builders.

After resolving the operation, initialize the zero-binding result fields and normalize the original SQL directly. Existing operation detection still requires its tokenization work; do not claim that this change alone removes it. Validate BOM and whitespace handling, repeated trailing semicolons, comments, marker-looking text in DDL, and the recognized EXPLAIN/JDBC CALL exceptions.

## F10: compare comment boundaries as ranges

Location: [SqlParser.java:1750](../../../src/main/java/com/landawn/abacus/query/SqlParser.java#L1750), especially 1771-1800.

For an operator-adjacent dropped block comment, `wouldMergeAcrossComment` builds a temporary StringBuilder, copies suffix pieces, copies following source text, and concatenates a joined String to ask whether concatenation would create an operator/comment/pgJDBC escape. Most default separators need only a small amount of boundary context.

`SELECT ` followed by 1,024 repetitions of `1/**/+` and a final `1` allocated 138,368 B/tokenization. The comment-free control allocated 27,264 B. The difference also includes source-length-based list capacity and other comment work, so it must not all be attributed to this helper.

Compare separator candidates against emitted-token suffix ranges and the following source range directly. Retain insertion of required spaces between separated `--`, `/*`, `<=`, custom `+~+`, and `??` fragments. Validate suffixes spanning multiple emitted tokens, Unicode, and longer custom separators before specializing the default path.

## F11: defer classifier state until a nested statement candidate needs it

Locations: [SqlParser.java:6569](../../../src/main/java/com/landawn/abacus/query/SqlParser.java#L6569), lines 6643-6652 and 6717-6724.

For the first ordinary word after `(`, the caller invokes `topGroup(groups)` before checking whether the word is any candidate mutation verb. `topGroup` allocates `ParenthesizedGroup`; the callee then returns immediately for `a` in `COALESCE(a,b)`, for example. Ordinary parenthesized predicates follow the same unnecessary path.

The current 1,024-function probe allocated 33,120 B/classification, with allocation increasing roughly 32 B per additional ordinary group at the larger sizes. This is total observed allocation, not a guaranteed per-object layout or measured improvement.

Defer group creation until the candidate verb survives the initial keyword/function/column checks. Preserve existing group state for nested WITH resolution and delayed companion-word detection; false positives such as `OPTION (MERGE JOIN)` must stay accepted. A related opportunity is to replace the private list of copied statement keywords (6488-6499, 6548, 6558, 6576, 6750 and 7005) with keyword bits or a rejection predicate. Early return is safe only once a mutation is confirmed, not while companion-word checks remain pending.

## Other reviewed opportunities and constraints

| Area | Observation | Recommendation/constraint |
| --- | --- | --- |
| Repeated parameter names, ParsedSql 459-460, 524, 631 | A cacheable 4,010-character statement with 1,001 `:id` occurrences retains 1,001 distinct name Strings, 1,001 backing byte arrays, and an ArrayList capacity of 1,234. | Consider local per-parse name reuse for sufficiently large/repetitive workloads. Preserve every occurrence and order. A map can worsen common 1-2-name queries; avoid global interning and quadratic list searches. |
| Simple named markers, ParsedSql 503-534 | A token containing only `:id` still creates marker-index storage, a token builder, and a rebuilt String commonly equal to `?`. | Benchmark a strict whole-token fast path that retains Unicode grammar, closed-value boundaries, mixed-style rejection, and escape separation. |
| Name-list capacity, ParsedSql 470, 522, 631 | `ImmutableList.wrap` retains the supplied ArrayList. A one-name result keeps capacity 10. | Compact only when retained savings justify another copy. Exact dependency and reflection evidence support the ownership finding. |
| Eager SQL hashing, ParsedSql 296 | Fresh oversized SQL is hashed even when the result is never used as a key; String itself already caches hashes. | Measure using fresh input Strings. This review's repeated-input benchmark warms String hashes and cannot quantify the cold hashing cost. Do not claim object-size savings without a layout measurement. |
| Source alignment, ParsedSql 569, 605, 637-639 | Newline/H2 preservation and positional fallback can independently align tokens on the same parse. | Reuse an already available alignment in that intersection; do not eagerly allocate one for ordinary SQL. |
| Tokenizer ASCII dispatch, SqlParser 976-1048 | `nextTokenBounds` has an ordinary-ASCII fast path at 1624-1629 that `tokenize` lacks. | A configuration-aware equivalent is plausible; measure ordinary words, Unicode and custom letter separators before adding complexity. |
| Configuration, SqlParser 611-612, 692-758 | No-op add/remove operations can copy separator sets; flag-only changes rebuild matching structures. | Setup-path priority unless callers rebuild configurations frequently. Preserve immutable ownership. |
| Token search, SqlParser 1373-1436 | Some impossible searches can return before scanning or creating context. | Investigate empty tokens and starting offsets at/past EOF with existing boundary contracts. |
| Sparse token lists, SqlParser 970-972 | Very sparse/comment-only SQL can reserve 8,192 reference slots. | Keep the existing cap. Any targeted lazy strategy needs dense controls: prior reports documented allocation regressions from blanket capacity changes. |
| Long custom separators, SqlParser 446-476, 1772-1786, 3126-3219 | Maximum separator length can include spellings excluded from compiled quote-crossing matches. | Separate effective matching length if such configurations matter; this was not timed. |
| Statement slices and depth state, SqlParser 5680-5691, 6088, 6194-6211 | Per-statement substrings and `List<Boolean>` store more than bounded scans/primitive state require. | Lower priority. Boolean objects are shared, not newly allocated per depth. Preserve statement-local context. |

The cache was reviewed independently as well as within the ParsedSql review. The 4,096-character admission limit, separate whitespace-variant keys, lock-free front hits, parsing outside the pool lock, shutdown fallback, and final-field publication are intentional current behavior. Front hits can retain entries beyond pool expiry; wholesale reset can cause reparsing; concurrent first touches can construct duplicate results. These are documented tradeoffs, not newly discovered unbounded leaks. The pool and front can retain different subsets, so memory budgeting must account for their union, potentially approaching two capacity-sized sets plus bounded concurrent overshoot, and for the complete ParsedSql object graph rather than just input length. A weighted cache or different eviction policy would require concurrent churn/retention measurements and contract review; neither was claimed here. Returning owned positional-offset arrays would violate existing defensive-copy guarantees.

## Complete coverage record

All ranges are inclusive and cover the current source files without gaps.

| SqlParser lines | Responsibility checked |
| --- | --- |
| 1-412 | Public contracts, separator vocabulary, composite tokens, factory entrypoints |
| 413-766 | Immutable configuration, compiled buckets, ownership, builder mutations, equality/hash |
| 767-966 | Tokenizer facade, argument checks, public tokenization contract |
| 967-1277 | Tokenization loop, token allocation, bracket/quote/subscript scanning |
| 1278-1693 | Token search, composite matching, range comparison, stateless next-token/bounds APIs |
| 1694-1920 | Separator decisions, comment boundaries, hash comment/name dispatch |
| 1921-2687 | Hash targets/list context, backward names/groups/quotes/trivia scans |
| 2688-3103 | Every HashScanMemo field, lazy table, frontier, and reentrant path |
| 3104-3327 | Hash fallback, labels, identifier predicates, separator matching, function detection |
| 3328-3900 | Leading-verb/read-write API contracts, leading-verb fast paths, lexical-mode setup |
| 3901-4179 | Stability validation, dialect veto dispatch, raw bracket-region scanning |
| 4180-4417 | RawCommentSkipper flat/nested memoization and lexical configuration |
| 4418-4897 | All dialect mask branches, parameters/comments/quotes, lexical-error exits |
| 4898-5338 | Axes, quote/marker helpers, Oracle alternatives, mask allocation, memo selection |
| 5339-6087 | Executable-comment rejection, statement boundaries, batches, accepted-shape checks |
| 6088-6487 | INSERT procedure/modifier/upsert/INTO and token-sequence checks |
| 6488-6931 | Query-start collection, nested data-change candidates, group state creation |
| 6932-7790 | ParenthesizedGroup state, nested WITH/CTE and suffix/value scans, remaining leaf helpers |

| ParsedSql lines | Responsibility checked |
| --- | --- |
| 1-280 | Complete public contract, constants, cache/result fields, ownership |
| 281-644 | Entire constructor, classification, marker conversion, newline/H2 handling, final result/offsets |
| 645-743 | Converted-marker separation, question-mark runs, mixed-style errors |
| 744-951 | Parse entrypoints, optional cache creation, races, front mirror, shutdown handling |
| 952-1150 | Accessors, defensive copy, direct question-mark offsets |
| 1151-1387 | Source alignment, comment matching, H2/newline gap and tail helpers |
| 1388-1581 | Operation resolution, BOM, EXPLAIN, JDBC call/return slots |
| 1582-1795 | Unicode names, subscript openings, ambiguity handling, chain classification |
| 1796-2251 | Marker eligibility/collection, quote/cast/bracket boundaries, significant-token lookup |
| 2252-2390 | Positional array shortcut, general bracket lexer and offset extraction |
| 2391-2581 | QuestionMarkClassifier main pass, lazy marker detection, scopes and emitted indexes |
| 2582-2825 | JSON constructor/query contexts, clauses, contextual/window-frame lookahead |
| 2826-3401 | Geometric operand/group/cast recognition, parenthesis indexing, split MyBatis metadata |
| 3402-3683 | Separator setup, keyword/operator/literal predicates, named-marker presence |
| 3684-3891 | MyBatis closure/metadata, bracket state, identifier and surrogate validation |
| 3892-3961 | hashCode, equals and toString |

The review retained existing linear/iterative CTE handling, bounded window-frame lookahead, one-pass marker collection, bracket fast paths, quote-reading agreement, primitive JSON scope stacks, range-based token extraction, ASCII token reuse, and the hash/group/list/backward memo fixes. No additional proven quadratic scanner was found inside ParsedSql itself; its constructor can still inherit SqlParser costs. This is a source review plus targeted evidence, not an exhaustive proof of parser correctness.

## Measurement and reproduction

[ParserReviewProbe.java](ParserReviewProbe.java) generates inputs before timing, forces uncached ParsedSql construction with the package-private null-cache overload, retains object results through a volatile sink, checks stable results, and measures allocated bytes using `com.sun.management.ThreadMXBean`. Both text and bounds walkers are checked against tokenizer output outside timing. Inputs are reused, so input construction and first String hash computation are excluded. It does not measure cache-hit throughput, concurrent misses, cold class initialization, peak heap, or whole-application performance.

Final runs used JDK 25, `--release 21` compilation, `-Xms256m -Xmx1024m -Xbatch`, two mixed-path preparation passes, at least 300 ms per-case warmup, adaptive batches targeting 5 ms capped at 32,768 operations, and seven measured rounds. Cases run in fixed order. Large operations can exceed the target; short cases can remain below it. Three fresh JVMs ran sequentially after tests and compilation completed. No review-owned test or compile process ran during measurement. Median-of-rounds per fork and median-of-fork-medians are reported; full ranges and every sample are preserved. No confidence interval, speedup from a fix, or universal latency guarantee is claimed.

An initial 80 ms-warmup protocol exposed first-family JIT effects (notably the first positional case). Its harness, all 1,995 samples, and summaries remain under [initial-80ms-warmup](initial-80ms-warmup/); they were not mixed into the final summaries. Timing also varies in the final run, which is why the findings emphasize reproducible source behavior, allocation, and disclosed fork ranges. Objectory's periodic "No error. It's only for debug" creation logs are retained in stderr files; these are not thrown benchmark failures.

The main evidence files are [summary.csv](summary.csv), [summary.json](summary.json), three `baseline-*.csv` raw files, their stderr logs, [ParsedSqlMemoryEvidence.java](ParsedSqlMemoryEvidence.java), [memory-evidence.txt](memory-evidence.txt), and the remask counterexample above. The aggregator rejects incomplete forks/rounds and differing results for the same case. Run it from the repository root.

After building this exact revision normally, the probes can be reproduced in PowerShell:

```powershell
mvn -B -ntp test '-Djacoco.skip=true'
$reviewDir = 'docs/performance/parser-review-2026-10-15'
$reviewCp = "target/classes;$env:USERPROFILE/.m2/repository/com/landawn/abacus/abacus-common/8.2.0/abacus-common-8.2.0.jar"
$reviewProbeSources = Get-ChildItem -LiteralPath $reviewDir -Filter '*.java' | ForEach-Object FullName
New-Item -ItemType Directory -Force -Path target/parser-review-probes | Out-Null
javac --release 21 -proc:none -cp $reviewCp -d target/parser-review-probes $reviewProbeSources
$reviewRunCp = "target/parser-review-probes;$reviewCp"
foreach ($reviewFork in 1..3) {
    java -Xms256m -Xmx1024m -Xbatch -cp $reviewRunCp com.landawn.abacus.query.ParserReviewProbe $reviewFork > "$reviewDir/baseline-$reviewFork.csv" 2> "$reviewDir/baseline-$reviewFork.log"
}
java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED -cp $reviewRunCp com.landawn.abacus.query.ParsedSqlMemoryEvidence
java -cp $reviewRunCp RemaskBoundaryProbe
node "$reviewDir/summarize.mjs"
```

Check each native command's exit code when reproducing. The successful isolated build, full test log and JUnit XML used for this review remain under `.audit/parser-review-2026-10-15`; they are local verification artifacts rather than production changes.
