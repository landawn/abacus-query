#!/usr/bin/env python3
"""
Merge test classes from one or more source files into a single target test file,
then delete the source files. Used to consolidate cross-file scattering so that
every source class has exactly one FooTest.java.

All top-level test classes (extends TestBase) in each source file are merged into
the target file's canonical test class. Imports are unioned. @Test method-name
collisions keep the target's version. Non-test top-level classes in source files
(helper entities, etc.) are also moved into the target file as top-level classes
(Jave allows multiple package-private top-level types alongside the public one).

Usage:
    python merge_across_files.py --target FooTest.java --source FooUtilTest.java [MiscTest.java ...]
                                 [--apply]   # without --apply, dry-run only
"""
from __future__ import annotations

import argparse
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from java_struct import parse_file
from split_members import split_members


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--target", required=True, help="canonical test file to merge INTO")
    ap.add_argument("--source", nargs="+", required=True, help="file(s) to merge FROM (deleted after)")
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    target_src = open(args.target, encoding="utf-8").read()
    tpkg, timps, tclasses = parse_file(target_src)
    file_stem = os.path.basename(args.target)[:-5]

    # Find the target's canonical test class (extends TestBase, ideally named file_stem)
    tcanon = None
    for c in tclasses:
        if c.super_class == "TestBase" and c.name == file_stem:
            tcanon = c
            break
    if tcanon is None:
        for c in tclasses:
            if c.super_class == "TestBase":
                tcanon = c
                break
    if tcanon is None:
        print(f"error: no TestBase class in target {args.target}", file=sys.stderr)
        sys.exit(2)

    # Collect target members + names
    target_members = split_members(tcanon.file_body)
    order = list(target_members)
    method_names = {m["name"] for m in target_members if m["kind"] == "method" and m["name"]}
    nested_names = {m["name"] for m in target_members if m["kind"] == "class" and m["name"]}
    field_names = {m["name"] for m in target_members if m["kind"] == "field" and m["name"]}
    collisions = []

    # Union of imports (preserve order: target imports first, then new ones)
    import_set = set(i.strip() for i in timps)
    extra_imports = []

    # Other top-level classes in target (non-test helpers) kept as-is
    target_kept = [c for c in tclasses if c is not tcanon]

    # Extra top-level classes from sources (helpers) to append to the file
    extra_top_classes = []

    for sp in args.source:
        ssrc = open(sp, encoding="utf-8").read()
        spkg, simps, sclasses = parse_file(ssrc)
        for imp in simps:
            if imp.strip() not in import_set:
                import_set.add(imp.strip())
                extra_imports.append(imp)
        for c in sclasses:
            if c.super_class == "TestBase":
                # merge its members into the target canonical
                for m in split_members(c.file_body):
                    if m["kind"] == "method" and m["name"]:
                        if m["name"] in method_names:
                            collisions.append(("method", m["name"], c.name, tcanon.name, "kept-target"))
                            continue
                        method_names.add(m["name"])
                        order.append(m)
                    elif m["kind"] == "class" and m["name"]:
                        if m["name"] in nested_names:
                            collisions.append(("nested-class", m["name"], c.name, tcanon.name, "kept-target"))
                            continue
                        nested_names.add(m["name"])
                        order.append(m)
                    elif m["kind"] == "field" and m["name"]:
                        if m["name"] in field_names:
                            collisions.append(("field", m["name"], c.name, tcanon.name, "kept-target"))
                            continue
                        field_names.add(m["name"])
                        order.append(m)
                    else:
                        order.append(m)
            else:
                # non-test top-level class from source -> keep as top-level in target file
                extra_top_classes.append(c)

    # Reconstruct target file
    body_text = "\n\n".join(m["text"].strip("\n") for m in order)
    # rebuild class header: keep target canonical's leading annotations + signature
    header = tcanon.leading_annotations.rstrip("\n") + "\n" + tcanon.signature.strip()
    # ensure @Tag("2025") present
    if '@Tag("2025")' not in header:
        # strip other @Tag, add 2025
        header = re.sub(r'@Tag\s*\(\s*"[^"]*"\s*\)\s*', "", header)
        header = '@Tag("2025")\n' + header.strip()
    merged_class = f"{header} {{\n{body_text}\n}}\n"

    out_lines = []
    if tpkg:
        out_lines.append(tpkg.rstrip("\n"))
        out_lines.append("")
    out_lines.extend(l.rstrip("\n") for l in timps)
    out_lines.extend(l.rstrip("\n") for l in extra_imports)
    out_lines.append("")
    out_lines.append(merged_class.rstrip("\n"))
    out_lines.append("")
    for c in target_kept + extra_top_classes:
        out_lines.append("")
        out_lines.append(c.leading_annotations.rstrip("\n") + "\n" + c.signature.strip() + " {")
        out_lines.append(c.file_body.strip("\n"))
        out_lines.append("}")
    out_text = "\n".join(out_lines) + "\n"

    print(f"target={args.target} canonical={tcanon.name} "
          f"added_methods={len([m for m in order if m not in target_members])} "
          f"collisions={len(collisions)}", file=sys.stderr)
    for col in collisions:
        print(f"  collision: {col}", file=sys.stderr)

    if args.apply:
        with open(args.target, "w", encoding="utf-8") as f:
            f.write(out_text)
        for sp in args.source:
            os.remove(sp)
            print(f"deleted {sp}", file=sys.stderr)
    else:
        print("DRY RUN -- pass --apply to write", file=sys.stderr)


if __name__ == "__main__":
    main()