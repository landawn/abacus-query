# SqlParser performance tuning — 2026-09-20

Comparison: exact commit 965f6afef86d928fbff63589fb3d59edc322574e versus the tuned working tree, starting from commit 4986c32bdf620aac68d88f56178a766e8cd78683. Only SqlParser production code changed. ParsedSql bytecode is identical to the starting snapshot.

The final implementation has lower median latency in **93/93 measured cases** versus the exact old commit, across five fresh JVMs per version. Allocation is **lower in 76 cases, equal in 17, and higher in none**. All **91 equivalent-result cases** improve; two custom-separator cases retain intentional correctness differences from the old version. The smallest median reduction in runtime is 11.9%.

The final matrix contains **11,718 samples from 14 JVMs**. See [summary.csv](final/summary.csv), [per-fork.csv](final/per-fork.csv) and [summary.json](final/summary.json) for every case and its fork range.

| Case | 965f6afe ns/op | Tuned ns/op | Less time | Allocated B/op, old → tuned |
|---|---:|---:|---:|---:|
| tokenize_simple | 1,214.9 | 651.7 | 46.4% | 1,216 → 976 |
| tokenize_large | 53,628.5 | 25,115.9 | 53.2% | 38,120 → 25,832 |
| tokenize_long_quote | 50,753.0 | 1,647.5 | 96.8% | 17,672 → 17,624 |
| tokenize_dense_concat_8192 | 330,880.0 | 130,703.0 | 60.5% | 61,720 → 61,624 |
| index_missing | 58,874.1 | 16,702.4 | 71.6% | 50,728 → 0 |
| index_near_matches | 2,933,950.0 | 16,619.2 | 99.4% | 3,275,376 → 0 |
| next_word | 76.7 | 35.6 | 53.6% | 48 → 48 |
| end_quote | 10,522.4 | 146.1 | 98.6% | 0 → 0 |
| read_simple | 6,027.5 | 1,466.0 | 75.7% | 7,392 → 1,344 |
| read_insert_upsert | 2,063.5 | 804.3 | 61.0% | 2,696 → 64 |
| select_ambiguous | 473.7 | 21.9 | 95.4% | 1,056 → 48 |
| config_default | 2,960.0 | 8.3 | 99.7% | 9,880 → 24 |
| config_custom | 2,936.1 | 2,436.3 | 17.0% | 10,288 → 6,704 |

Allocation means bytes allocated by the benchmark thread per operation. It is not a measurement of peak heap, retained heap, class initialization or concurrent throughput. Position-only paths already at zero allocation cannot allocate less. The fixed cache of 128 immutable single-character strings has a one-time memory cost in exchange for fewer per-call objects. These results establish the measured corpus target, not superiority for every possible SQL string, machine or JVM.

Two historical outputs differ from current behavior: next_custom and index_custom_separator. The old tokenizer does not consistently recognize the whitespace-leading custom separator in these APIs. The table flags these cases; all current and pre-tuning outputs must match the reference signatures before measurement.

## Implementation and correctness

- Tokenization emits contiguous source ranges instead of copying every character through a pooled StringBuilder. Single-character token text is reused.
- Token extraction and end-position lookup share one range scanner. Token search compares ranges without allocating preceding token strings; composite matches reuse token boundaries.
- Quote-end scanning is shared by tokenization and classification. It preserves doubled delimiters, backslash parity, bracket quoting and unterminated regions.
- Hash-context memoization remains for possible table names and comment-aware lookbacks. MyBatis markers and punctuation operators avoid it. ASCII context comparisons avoid temporary uppercase strings; non-ASCII input retains the original ROOT-uppercase semantics.
- Query classifiers skip lexical-mode combinations that cannot differ. Mask storage is lazy; unchanged SQL reuses its memo. Leading-verb predicates can resolve a plain unquoted initial verb directly, while read/write gates continue to validate the entire input. Validated single statements avoid a redundant top-level scan, and upserts are rejected before unrelated clause scans.
- Immutable configuration builders copy separators on mutation and may reuse an unchanged built configuration. Compilation uses exact-size ASCII separator buckets instead of temporary per-bucket maps/lists.

SqlParser.java is 327 lines shorter than the starting version. No parameter-classification rule or deferred renderer issue was intentionally changed.

All 81 production sources and 86 test sources compiled with JDK 25 targeting Java 17; changed tests were recompiled after additions. The final JUnit run passed 5,288 tests with zero failures or skips. Ten new methods in the existing SqlParserTest cover configuration ownership and derived buckets, scanner bounds and custom newline behavior, large/unterminated quoted regions, Unicode hash context, dense operators, independent lexical modes, short hash tokens, and the distinction between leading-verb predicates and read/write gates.

The 42,000-probe tokenizer/ParsedSql corpus and 24,000-probe classifier corpus match the pre-tuning snapshot exactly. They check tokens, offsets, search positions, parameter counts, names, rendered SQL, positional offsets and rejection messages across custom separators and dialect ambiguities. See differential-summary.json and the two differential harnesses.

Javadoc doclint passes for the full source tree with three existing warnings outside SqlParser (two missing tags on AbstractQueryBuilder.mutateAtomically and the SqlDialect default constructor). API documentation was regenerated. Production/test source retains UTF-8 without BOM and CRLF; git diff --check is clean. The direct JUnit runner propagates failures because Maven in this repository is configured to ignore them; temporary-directory cleanup is disabled for the Windows sandbox, while the directory assertions still run.

## Protocol and artifacts

Windows 10.0.26100, AMD Ryzen 7 PRO 7840U, 16 logical processors, JDK 25+37; -Xms512m -Xmx1024m -XX:+UseG1GC. Exact baseline uses abacus-common 7.9.3; current and pre-tuning use 8.0.0. Three additional forks run the old source with 8.0.0 to separate the source change from the dependency change.

Only one benchmark JVM runs at a time. Five forks per primary version alternate baseline/current order. Each case has at least 250 ms of repeated warmup, with at least 20,000 configuration operations to avoid measuring the custom constructor before compilation. Nine measured rounds shuffle the cases with a fixed per-fork seed. Batch sizes target 20 ms, capped at 262,144 operations. Thirty-two distinct equal-valued strings avoid relying on interned input. Result references escape to a volatile sink; primitive APIs are timed without boxing. Setup, reference comparisons and CSV formatting are outside timing/allocation windows.

Summaries use the median of each fork's nine samples, then the median across forks. Fork ranges, allocation, result equivalence and every measured round are retained. No compilation or test suite runs alongside final measurements. manifest.json records source, test, harness and snapshot hashes.

The same-dependency control also has **93/93 lower latency medians and no allocation increases**. The single-fork starting-snapshot diagnostic is less decisive: tokenize_comments is 1,294.9 → 1,332.0 ns/op; tokenize_hash_rows is 9,851.7 → 11,180.7 ns/op. These are disclosed rather than claiming every path improved over the already-tuned starting tree; the requested comparison is with 965f6afe.

The separate 40-case ParsedSql integration guard uses three current and three pre-tuning forks. All outputs remain correct, and allocation increases in none of its cases. 38/40 timing medians improve against the starting snapshot. Slower observations are array_mybatis_8192 (776,236.4 → 790,558.3 ns/op, +1.8%); array_mybatis_256 (24,526.1 → 24,651.6 ns/op, +0.5%). The two MyBatis fork ranges overlap the starting snapshot. Cached lookups bypass SqlParser and ParsedSql bytecode is unchanged, so their lower medians are not evidence of a cache optimization. Full fork values are in [the integration summary](parsed-sql-guard/summary.json).

- final/: final 93-case measurements, per-fork summaries and raw CSV/log files.
- first-pass/: complete initial 83-case experiment, retained even though its capacity cap was later removed.
- second-pass/: complete 87-case experiment after restoring the capacity estimate, before the final leading-verb and short-hash optimizations.
- capacity-pass-aborted/: an intentionally interrupted intermediate run after integration allocation regressions were found; it is excluded from final summaries.
- parsed-sql-guard-first-pass/: the original integration check that exposed the capacity regression.
- parsed-sql-guard/: the final integration guard against the pre-tuning snapshot.

The capacity cap initially increased allocation for dense concatenation by up to about 55 KB per 8,192-binding parse. Restoring the original source-length estimate removed that regression; the direct corpus now includes all four dense-expression sizes. The early experiments are retained rather than filtering out unfavorable runs.

## Reproduction

Build each complete source snapshot with --release 17 and its corresponding dependency version. Compile SqlParserBenchmark.java against a compatible snapshot, then use the same benchmark classes with each snapshot's runtime classpath. The benchmark uses only APIs present in the baseline (including the deprecated isReadOnlyQuery alias).

PowerShell invocation pattern (supply the classpaths for your builds):

    javac --release 17 -cp $beforeCp -d bench-classes SqlParserBenchmark.java
    java -cp "bench-classes;$beforeCp" SqlParserBenchmark before 1 reference.txt probe
    java -Xms512m -Xmx1024m -XX:+UseG1GC -cp "bench-classes;$baselineCp" SqlParserBenchmark baseline 1 reference.txt steady
    java -Xms512m -Xmx1024m -XX:+UseG1GC -cp "bench-classes;$currentCp" SqlParserBenchmark current 1 reference.txt steady

Repeat for fork numbers 1 through 5, alternating launch order. Capture stdout as final/<version>-<fork>.csv and stderr separately; run node summarize.cjs to regenerate the summaries. Use version baseline-control with the old source and current dependency for the three control forks. The before run is a single-fork diagnostic, not a five-fork confidence comparison. The changed production/test code is included in implementation.patch.

## Every measured case

| Case | 965f6afe ns/op | Tuned ns/op | Less time | Allocated B/op, old → tuned |
|---|---:|---:|---:|---:|
| classify_delete | 422.4 | 21.2 | 95.0% | 960 → 48 |
| classify_insert | 391.7 | 22.1 | 94.4% | 896 → 48 |
| classify_replace | 131.9 | 65.8 | 50.1% | 192 → 0 |
| classify_update | 366.0 | 21.9 | 94.0% | 864 → 48 |
| config_custom | 2,936.1 | 2,436.3 | 17.0% | 10,288 → 6,704 |
| config_default | 2,960.0 | 8.3 | 99.7% | 9,880 → 24 |
| end_custom | 129.2 | 14.1 | 89.1% | 0 → 0 |
| end_empty | 44.8 | 34.1 | 23.9% | 0 → 0 |
| end_hash | 480.0 | 106.5 | 77.8% | 288 → 200 |
| end_hash_operator | 181.2 | 27.8 | 84.7% | 0 → 0 |
| end_mybatis_single | 36.4 | 20.3 | 44.1% | 0 → 0 |
| end_quote | 10,522.4 | 146.1 | 98.6% | 0 → 0 |
| end_trivia | 93.3 | 61.3 | 34.3% | 0 → 0 |
| end_word | 47.8 | 27.3 | 42.8% | 0 → 0 |
| index_composite | 2,570.0 | 466.1 | 81.9% | 2,256 → 0 |
| index_composite_comments | 745.5 | 149.2 | 80.0% | 792 → 0 |
| index_custom_composite | 1,377.5 | 391.1 | 71.6% | 792 → 0 |
| index_custom_separator † | 1,713.5 | 92.1 | 94.6% | 968 → 48 |
| index_early | 199.6 | 67.3 | 66.3% | 120 → 0 |
| index_from_inside_quote | 409.8 | 141.6 | 65.4% | 464 → 0 |
| index_hash | 125,244.2 | 7,002.9 | 94.4% | 128,880 → 632 |
| index_late | 105,481.9 | 16,317.9 | 84.5% | 64,616 → 0 |
| index_missing | 58,874.1 | 16,702.4 | 71.6% | 50,728 → 0 |
| index_near_matches | 2,933,950.0 | 16,619.2 | 99.4% | 3,275,376 → 0 |
| index_quoted | 53,191.4 | 3,518.5 | 93.4% | 17,664 → 0 |
| index_sensitive | 471.7 | 195.9 | 58.5% | 480 → 0 |
| index_whitespace | 107.0 | 46.2 | 56.8% | 72 → 0 |
| next_custom † | 243.9 | 21.5 | 91.2% | 48 → 48 |
| next_empty | 64.6 | 34.5 | 46.6% | 0 → 0 |
| next_hash | 506.4 | 117.6 | 76.8% | 336 → 248 |
| next_hash_operator | 249.6 | 27.7 | 88.9% | 48 → 0 |
| next_mybatis_single | 63.2 | 21.0 | 66.8% | 48 → 0 |
| next_quote | 20,254.0 | 1,020.0 | 95.0% | 8,760 → 8,760 |
| next_trivia | 125.3 | 67.0 | 46.5% | 48 → 48 |
| next_word | 76.7 | 35.6 | 53.6% | 48 → 48 |
| read_comments | 20,384.5 | 10,053.6 | 50.7% | 16,448 → 6,680 |
| read_cte | 15,396.1 | 2,995.0 | 80.5% | 20,064 → 3,720 |
| read_executable_comment | 117.6 | 83.7 | 28.8% | 248 → 0 |
| read_hash | 1,510,444.4 | 11,412.0 | 99.2% | 1,534,112 → 2,360 |
| read_insert | 95.3 | 13.1 | 86.3% | 224 → 0 |
| read_insert_comments | 36,793.4 | 15,686.8 | 57.4% | 33,664 → 6,840 |
| read_insert_cte | 24,715.0 | 4,157.7 | 83.2% | 34,976 → 3,800 |
| read_insert_executable_comment | 123.0 | 79.7 | 35.2% | 248 → 0 |
| read_insert_hash | 2,528,544.4 | 14,901.9 | 99.4% | 2,562,784 → 2,440 |
| read_insert_insert | 4,883.1 | 991.7 | 79.7% | 7,168 → 656 |
| read_insert_large | 314,572.5 | 65,325.1 | 79.2% | 357,376 → 27,280 |
| read_insert_literal | 6,119.6 | 1,200.0 | 80.4% | 7,424 → 752 |
| read_insert_multiple | 3,924.5 | 898.2 | 77.1% | 6,208 → 664 |
| read_insert_mutation | 327.3 | 242.9 | 25.8% | 520 → 200 |
| read_insert_mysql_dash | 336.5 | 288.6 | 14.2% | 544 → 400 |
| read_insert_quoted_large | 383,619.0 | 9,921.4 | 97.4% | 110,528 → 26,816 |
| read_insert_simple | 12,116.4 | 2,275.8 | 81.2% | 16,928 → 1,424 |
| read_insert_upsert | 2,063.5 | 804.3 | 61.0% | 2,696 → 64 |
| read_large | 164,774.4 | 43,989.4 | 73.3% | 140,992 → 27,200 |
| read_literal | 3,661.3 | 719.3 | 80.4% | 4,032 → 672 |
| read_multiple | 2,499.9 | 557.7 | 77.7% | 4,352 → 584 |
| read_mutation | 313.8 | 245.3 | 21.8% | 520 → 200 |
| read_mysql_dash | 335.1 | 295.3 | 11.9% | 544 → 400 |
| read_quoted_large | 278,495.9 | 9,837.7 | 96.5% | 107,904 → 26,736 |
| read_simple | 6,027.5 | 1,466.0 | 75.7% | 7,392 → 1,344 |
| read_upsert | 146.2 | 13.3 | 90.9% | 344 → 0 |
| select_ambiguous | 473.7 | 21.9 | 95.4% | 1,056 → 48 |
| select_cte | 2,623.9 | 643.0 | 75.5% | 4,896 → 1,080 |
| select_parenthesized | 452.0 | 105.4 | 76.7% | 896 → 48 |
| select_quotes | 424.0 | 21.2 | 95.0% | 960 → 48 |
| select_simple | 629.0 | 22.0 | 96.5% | 1,440 → 48 |
| tokenize_array | 11,686.9 | 653.7 | 94.4% | 4,248 → 4,248 |
| tokenize_backslashes | 388.8 | 220.0 | 43.4% | 488 → 344 |
| tokenize_comments | 3,357.3 | 1,332.0 | 60.3% | 2,464 → 736 |
| tokenize_custom | 1,506.2 | 338.6 | 77.5% | 600 → 392 |
| tokenize_dense_concat_1024 | 41,323.7 | 16,842.3 | 59.2% | 7,960 → 7,864 |
| tokenize_dense_concat_256 | 10,880.2 | 4,290.9 | 60.6% | 2,200 → 2,104 |
| tokenize_dense_concat_4096 | 163,181.1 | 67,042.4 | 58.9% | 31,000 → 30,904 |
| tokenize_dense_concat_8192 | 330,880.0 | 130,703.0 | 60.5% | 61,720 → 61,624 |
| tokenize_hash_identifiers | 63,204.9 | 7,483.2 | 88.2% | 65,528 → 1,984 |
| tokenize_hash_operator | 109.9 | 35.6 | 67.6% | 104 → 104 |
| tokenize_hash_rows | 1,922,554.5 | 11,180.7 | 99.4% | 331,144 → 2,624 |
| tokenize_keep_comments | 4,254.0 | 1,530.4 | 64.0% | 3,504 → 1,776 |
| tokenize_large | 53,628.5 | 25,115.9 | 53.2% | 38,120 → 25,832 |
| tokenize_long_quote | 50,753.0 | 1,647.5 | 96.8% | 17,672 → 17,624 |
| tokenize_mybatis | 5,427.5 | 2,879.3 | 46.9% | 5,752 → 3,448 |
| tokenize_mybatis_single | 71.9 | 35.3 | 50.9% | 152 → 104 |
| tokenize_simple | 1,214.9 | 651.7 | 46.4% | 1,216 → 976 |
| tokenize_tiny | 111.6 | 67.9 | 39.2% | 200 → 152 |
| tokenize_unicode | 828.0 | 408.4 | 50.7% | 1,088 → 848 |
| walk_bounds_comments | 3,401.4 | 977.2 | 71.3% | 0 → 0 |
| walk_bounds_hash | 125,620.5 | 19,262.2 | 84.7% | 127,008 → 7,584 |
| walk_bounds_large | 46,332.2 | 17,226.5 | 62.8% | 0 → 0 |
| walk_bounds_simple | 995.8 | 438.8 | 55.9% | 0 → 0 |
| walk_tokens_comments | 9,056.0 | 2,039.2 | 77.5% | 1,920 → 192 |
| walk_tokens_hash | 249,135.7 | 37,585.5 | 84.9% | 255,456 → 15,936 |
| walk_tokens_large | 128,026.2 | 37,193.2 | 70.9% | 25,792 → 13,504 |
| walk_tokens_simple | 2,732.8 | 939.7 | 65.6% | 816 → 576 |

† Historical output differs; see the equivalence discussion above.
