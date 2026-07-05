#!/usr/bin/env python3
"""
Batch-merge every test file under src/test/java that currently contains more
than one top-level test class (extends TestBase) into a single canonical class.

Dry-run by default (prints which files would change). With --apply it rewrites
each file in place. A collision report is written to scripts/test_audit/merge_collisions.txt.

Re-runnable: files that already have a single test class are skipped.
"""
from __future__ import annotations

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from java_struct import parse_file
from merge_file import merge_file


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--root", default=None)
    args = ap.parse_args()

    here = os.path.dirname(os.path.abspath(__file__))
    repo_root = args.root or os.path.abspath(os.path.join(here, "..", ".."))
    test_root = os.path.join(repo_root, "src", "test", "java")

    files = []
    for dp, _, fns in os.walk(test_root):
        for fn in fns:
            if fn.endswith(".java"):
                files.append(os.path.join(dp, fn))
    files.sort()

    coll_lines = []
    changed = 0
    skipped = 0
    for path in files:
        with open(path, encoding="utf-8") as f:
            src = f.read()
        pkg, imps, classes = parse_file(src)
        candidates = [c for c in classes if c.super_class == "TestBase"]
        if len(candidates) <= 1:
            skipped += 1
            continue
        file_stem = os.path.basename(path)[:-5]
        if args.apply:
            res = merge_file(src, file_stem)
            if res is None:
                skipped += 1
                continue
            with open(path, "w", encoding="utf-8") as f:
                f.write(res["text"])
            changed += 1
            print(f"merged {os.path.relpath(path, repo_root)}: "
                  f"canonical={res['canonical']} removed={res['merged_away']} "
                  f"methods={res['method_count']}")
            coll_lines.append(f"\n=== {os.path.relpath(path, repo_root)} ===")
            coll_lines.append(f"canonical={res['canonical']} removed={res['merged_away']}")
            for col in res["collisions"]:
                coll_lines.append(f"  {col[0]} '{col[1]}' from {col[2]} vs {col[3]} -> {col[4]}")
        else:
            print(f"would merge {os.path.relpath(path, repo_root)}: "
                  f"{[c.name for c in candidates]}")
    coll_path = os.path.join(here, "merge_collisions.txt")
    with open(coll_path, "w", encoding="utf-8") as f:
        f.write("\n".join(coll_lines))
    print(f"\n{changed} files merged, {skipped} skipped. "
          f"collision report -> {coll_path}", file=sys.stderr)


if __name__ == "__main__":
    main()