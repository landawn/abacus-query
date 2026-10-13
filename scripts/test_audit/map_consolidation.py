#!/usr/bin/env python3
"""
Map every test class to the source class it tests, and report:

  (1) cross-file scattering  -- methods for source class Foo live in >1 file
  (2) multi-class files       -- a single FooTest.java file holds >1 class that
                                 all test the same source class (e.g. Xxx2025Test
                                 tagged + XxxTest untagged), the "two classes per
                                 file" pattern that needs merging to satisfy
                                 "exactly one test class per source class"
  (3) orphan test classes     -- test class with no matching source file
  (4) source classes with no test class at all

Mapping heuristic:
  - Strip suffixes Test/2025Test/2026Test/BatchTest/Batch2Test/BugFixTest/
        CoverageTest/FromFiltersTest/UtilTest/... from the test class name.
  - Match the remaining stem to a source class simple name in src/main/java.
  - PSCTest -> PreparedStatementCompiler? handled by explicit alias map.
  - A test class with no clear source-class stem is reported as orphan.

Usage:
  python map_consolidation.py [--out report.json]
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import defaultdict


SUFFIXES = [
    "2026Batch2Test", "2026BatchTest", "2026Test", "2025Test",
    "BugFixTest", "FromFiltersTest", "CoverageTest", "BatchTest",
    "UtilTest", "PaginationTest", "OperationTest", "Test",
]
# known aliases: test class stem -> source class simple name
ALIASES = {
    "SqlBuilderSupportedSqlTest": "SqlBuilder",
    "ParsedSqlValidationTest": "ParsedSql",
    "Simple": "AbstractQueryBuilder",
    "CriteriaBuilderAdd": "Criteria",
}
INTENTIONAL_SPLITS = {
    "SqlBuilder": "The supported-SQL catalog is a separate executable specification.",
    "SqlDialect": "Pagination is kept as a focused dialect behavior suite.",
    "ParsedSql": "Scanner preconditions are kept in a focused validation suite.",
}


def source_classes(repo_root: str) -> dict:
    """Return {simple_name: relpath} for every top-level type in src/main/java."""
    main_root = os.path.join(repo_root, "src", "main", "java")
    out = {}
    for dp, _, fns in os.walk(main_root):
        for fn in fns:
            if not fn.endswith(".java"):
                continue
            stem = fn[:-5]
            if stem in ("package-info", "module-info"):
                continue
            # nested types share a file; record the file stem as the source class
            rel = os.path.relpath(os.path.join(dp, fn), repo_root).replace(os.sep, "/")
            out.setdefault(stem, rel)
    return out


def stem_to_source(test_name: str) -> str:
    if test_name in ALIASES:
        return ALIASES[test_name]
    for suf in SUFFIXES:
        if test_name.endswith(suf):
            stem = test_name[:-len(suf)]
            if stem:
                return stem
    return test_name


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", default=".audit/test-audit/parse_output.json")
    ap.add_argument("--out", default=None)
    args = ap.parse_args()

    here = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.abspath(os.path.join(here, "..", ".."))
    srcs = source_classes(repo_root)

    data = json.load(open(args.input, encoding="utf-8"))
    classes = data["classes"]
    methods = data["methods"]

    # map: source class -> { file -> set(class_name) }
    by_source = defaultdict(lambda: defaultdict(set))
    orphan_classes = []
    dependency_contracts = []
    method_owners = {(m["file"], m["class_name"]) for m in methods}
    for c in classes:
        qualified_name = c.get("qualified_name", c["name"])
        if (c["file"], qualified_name) not in method_owners:
            continue  # Fixtures and helper classes are not independent test units.
        # skip the file-level suite / helpers / TestBase
        if c["name"] in ("AbacusQueryTestSuite", "TestBase", "CodeHelper", "Maven"):
            continue
        owner = qualified_name.split(".")[0]
        if owner == "SKTest":
            dependency_contracts.append({"class": owner, "file": c["file"], "source": "com.landawn.abacus.util.SK (abacus-common)"})
            continue
        direct = owner.removesuffix("Test")
        stem = direct if direct in srcs else stem_to_source(owner)
        if stem not in srcs:
            # try alias fallback
            if owner in ALIASES and ALIASES[owner] in srcs:
                stem = ALIASES[owner]
            else:
                orphan_classes.append({"class": c["name"], "file": c["file"],
                                       "guessed_source": stem})
                continue
        by_source[stem][c["file"]].add(qualified_name)

    # also attach methods count per class
    class_methods = defaultdict(int)
    for m in methods:
        class_methods[(m["file"], m["class_name"])] += 1

    cross_file = []
    intentional_splits = []
    multi_class_one_file = []
    for src, files in sorted(by_source.items()):
        if len(files) > 1:
            entry = {
                "source": src,
                "source_file": srcs.get(src, "?"),
                "test_files": [
                    {"file": f, "classes": sorted(cs),
                     "method_counts": {c: class_methods[(f, c)] for c in cs}}
                    for f, cs in sorted(files.items())
                ],
            }
            if src in INTENTIONAL_SPLITS:
                entry["reason"] = INTENTIONAL_SPLITS[src]
                intentional_splits.append(entry)
            else:
                cross_file.append(entry)
        for f, cs in files.items():
            if len({name.split(".")[0] for name in cs}) > 1:
                multi_class_one_file.append({
                    "source": src,
                    "file": f,
                    "classes": sorted(cs),
                    "method_counts": {c: class_methods[(f, c)] for c in cs},
                    "tags": {c: next((m["class_tag"] for m in methods
                                     if m["file"] == f and m["class_name"] == c), "")
                             for c in cs},
                })

    # source classes with no test class at all
    tested = set(by_source.keys())
    untested = [s for s in srcs if s not in tested and s != "module-info"]

    report = {
        "cross_file_scattering": cross_file,
        "intentional_splits": intentional_splits,
        "dependency_contracts": dependency_contracts,
        "multi_class_one_file": multi_class_one_file,
        "orphan_test_classes": orphan_classes,
        "untested_source_classes": sorted(untested),
        "summary": {
            "source_class_count": len(srcs),
            "tested_source_class_count": len(tested),
            "cross_file_groups": len(cross_file),
            "multi_class_files": len(multi_class_one_file),
            "orphan_test_classes": len(orphan_classes),
            "untested_source_classes": len(untested),
        },
    }
    text = json.dumps(report, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(text)
        s = report["summary"]
        print(f"wrote {args.out}: {s['cross_file_groups']} cross-file groups, "
              f"{s['multi_class_files']} multi-class files, "
              f"{s['orphan_test_classes']} orphan test classes, "
              f"{s['untested_source_classes']} untested source classes", file=sys.stderr)
    else:
        print(text)


if __name__ == "__main__":
    main()
