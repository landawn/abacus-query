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
    "PSC": "SqlBuilder",          # PSCTest exercises SqlBuilder (prepared-statement compiler)
    "SK": "Selection",            # SK = selection key? verify
    "Simple": "AbstractQueryBuilder",
    "CriteriaBuilderAdd": "Criteria",
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
    ap.add_argument("--input", default="scripts/test_audit/parse_output.json")
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
    for c in classes:
        # skip the file-level suite / helpers / TestBase
        if c["name"] in ("AbacusQueryTestSuite", "TestBase", "CodeHelper", "Maven"):
            continue
        stem = stem_to_source(c["name"])
        if stem not in srcs:
            # try alias fallback
            if c["name"] in ALIASES and ALIASES[c["name"]] in srcs:
                stem = ALIASES[c["name"]]
            else:
                orphan_classes.append({"class": c["name"], "file": c["file"],
                                       "guessed_source": stem})
                continue
        by_source[stem][c["file"]].add(c["name"])

    # also attach methods count per class
    class_methods = defaultdict(int)
    for m in methods:
        class_methods[(m["file"], m["class_name"])] += 1

    cross_file = []
    multi_class_one_file = []
    for src, files in sorted(by_source.items()):
        if len(files) > 1:
            cross_file.append({
                "source": src,
                "source_file": srcs.get(src, "?"),
                "test_files": [
                    {"file": f, "classes": sorted(cs),
                     "method_counts": {c: class_methods[(f, c)] for c in cs}}
                    for f, cs in sorted(files.items())
                ],
            })
        for f, cs in files.items():
            if len(cs) > 1:
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