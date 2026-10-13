#!/usr/bin/env python3
"""Merge legacy top-level TestBase classes into their canonical test class.

Dry-run by default; --apply writes the result. Member-name collisions abort
before writing. Review fixtures and lifecycle semantics before applying a merge.
"""
from __future__ import annotations

import argparse
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from java_struct import parse_file, ClassInfo, MethodInfo
from split_members import split_members


def class_stem(name: str) -> str:
    for suf in ("2026Batch2Test", "2026BatchTest", "2026Test", "2025Test",
                "BugFixTest", "FromFiltersTest", "JavadocExamples",
                "CoverageTest", "BatchTest", "UtilTest", "PaginationTest",
                "OperationTest", "Test"):
        if name.endswith(suf):
            return name[:-len(suf)]
    return name


def pick_canonical(classes, file_stem: str):
    """Pick the canonical class to keep (and rename to file_stem)."""
    tagged = [c for c in classes if _has_tag_2025(c)]
    if tagged:
        # prefer the tagged class whose stem matches the file stem
        for c in tagged:
            if class_stem(c.name) == class_stem(file_stem):
                return c
        return tagged[0]
    # no tagged class: prefer the one named exactly file_stem
    for c in classes:
        if c.name == file_stem:
            return c
    # else the largest
    return max(classes, key=lambda c: len(c.file_body))


def _has_tag_2025(c: ClassInfo) -> bool:
    return '@Tag("2025")' in c.leading_annotations or "@Tag(\"2025\")" in c.leading_annotations


def _build_header(leading: str, signature: str, file_stem: str) -> str:
    """Build the header, preserving annotations other than the obsolete year tag."""
    leading = re.sub(r'@Tag\s*\(\s*"2025"\s*\)\s*', "", leading)
    leading = re.sub(r"\b(?:public|abstract|final|static)\s*$", "", leading).strip()
    signature = re.sub(r"\b(class|interface|enum|record)\s+[A-Za-z_$][A-Za-z0-9_$]*",
                       lambda match: match.group(1) + " " + file_stem, signature.strip(), count=1)
    signature = re.sub(r"^public\s+", "", signature)
    return (leading + "\n" if leading else "") + "public " + signature + " {"


def merge_file(src: str, file_stem: str):
    pkg, imports, classes = parse_file(src)

    # Separate merge candidates (extend TestBase) from kept-as-is classes.
    candidates = [c for c in classes if c.super_class == "TestBase"]
    kept = [c for c in classes if c.super_class != "TestBase"]

    if len(candidates) <= 1:
        return None  # nothing to merge

    canonical = pick_canonical(candidates, file_stem)
    others = [c for c in candidates if c is not canonical]

    # Build the merged body: start from canonical's members, then add unique
    # members from each other class.
    canonical_members = split_members(canonical.file_body)
    # name -> member (for dedup)
    by_name: dict = {}
    order: list = []
    for m in canonical_members:
        key = (m["kind"], m["name"])
        if m["name"]:
            by_name[key] = m
        order.append(m)

    collisions = []
    nested_class_names = {m["name"] for m in canonical_members if m["kind"] == "class"}
    field_names = {m["name"] for m in canonical_members if m["kind"] == "field"}
    method_names = {m["name"] for m in canonical_members if m["kind"] == "method"}

    for c in others:
        members = split_members(c.file_body)
        for m in members:
            if m["kind"] == "method" and m["name"]:
                if m["name"] in method_names:
                    # collision: keep the canonical (tagged, known-passing, newer
                    # naming) version; skip the other. This prioritizes suite
                    # stability and aligns with "most descriptive".
                    collisions.append(("method", m["name"], c.name, canonical.name, "kept-canonical"))
                    continue
                method_names.add(m["name"])
                by_name[("method", m["name"])] = m
                order.append(m)
            elif m["kind"] == "class" and m["name"]:
                if m["name"] in nested_class_names:
                    collisions.append(("nested-class", m["name"], c.name, canonical.name, "skipped"))
                    continue
                nested_class_names.add(m["name"])
                order.append(m)
            elif m["kind"] == "field" and m["name"]:
                if m["name"] in field_names:
                    collisions.append(("field", m["name"], c.name, canonical.name, "skipped"))
                    continue
                field_names.add(m["name"])
                order.append(m)
            else:
                # init-block or unknown: append (rare)
                order.append(m)

    if collisions:
        names = ", ".join(f"{kind} {name}" for kind, name, *_ in collisions)
        raise ValueError(f"Refusing to discard colliding members: {names}. Review and rename or remove them explicitly.")

    # Reconstruct the merged class source. Preserve each member's original
    # indentation (members were already at 4-space indent inside their source
    # class). Join with a blank line between members for readability.
    body_text = "\n\n".join(m["text"].strip("\n") for m in order)

    header = _build_header(canonical.leading_annotations, canonical.signature, file_stem)
    merged_class = f"{header}\n{body_text}\n}}\n"

    # Re-emit file
    out_lines = []
    if pkg:
        out_lines.append(pkg.rstrip("\n"))
        out_lines.append("")
    if imports:
        out_lines.extend(l.rstrip("\n") for l in imports)
        out_lines.append("")
    out_lines.append(merged_class.rstrip("\n"))
    out_lines.append("")
    for k in kept:
        out_lines.append("")
        out_lines.append(k.leading_annotations.rstrip("\n") + k.signature.rstrip("\n") + " {")
        # body of kept class verbatim
        out_lines.append(k.file_body.strip("\n"))
        out_lines.append("}")
    out_text = "\n".join(out_lines) + "\n"

    return {
        "text": out_text,
        "canonical": canonical.name,
        "merged_away": [c.name for c in others],
        "collisions": collisions,
        "method_count": sum(1 for m in order if m["kind"] == "method"),
    }


def _reindent(body: str) -> str:
    """Reindent member bodies to a uniform 4-space indent inside the class."""
    lines = body.split("\n")
    out = []
    for ln in lines:
        if ln.strip() == "":
            out.append("")
            continue
        # strip existing leading whitespace, then add 4 spaces
        out.append("    " + ln.lstrip())
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("file")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--out", default=None, help="write merged file here (else overwrite with --apply)")
    args = ap.parse_args()

    path = args.file
    file_stem = os.path.basename(path)[:-5]
    with open(path, encoding="utf-8") as f:
        src = f.read()
    res = merge_file(src, file_stem)
    if res is None:
        print(f"{path}: nothing to merge (<=1 test class)", file=sys.stderr)
        return
    target = args.out or path
    if args.apply:
        with open(target, "w", encoding="utf-8") as f:
            f.write(res["text"])
        print(f"{target}: merged canonical={res['canonical']} "
              f"removed={res['merged_away']} methods={res['method_count']}", file=sys.stderr)
    else:
        print(f"{path}: DRY RUN canonical={res['canonical']} "
              f"would remove={res['merged_away']} methods={res['method_count']}", file=sys.stderr)
    for col in res["collisions"]:
        print(f"  collision: {col[0]} '{col[1]}' from {col[2]} vs {col[3]} -> {col[4]}", file=sys.stderr)


if __name__ == "__main__":
    main()
