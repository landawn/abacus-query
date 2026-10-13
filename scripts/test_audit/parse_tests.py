#!/usr/bin/env python3
"""Inventory Java tests without mistaking comments and literals for code.

Duplicate fingerprints exclude signatures and preserve literal contents.
Runtime discovery and invocation counts must still be verified with JUnit.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from java_tokens import code_mask, matching, normalize_body, normalize_tokens, scan


ASSERTION_RE = re.compile(r"\b(?:assert[A-Z][\w]*|expect[A-Z][\w]*|verify[A-Z][\w]*|fail)\s*\(")
TEST_ANNOTATION_RE = re.compile(r"@(?:org\.junit\.[\w.]+\.)?(Test|ParameterizedTest|RepeatedTest|TestFactory)\b")
CLASS_RE = re.compile(r"\b(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)")


def parse_source(src, file_rel):
    mask = code_mask(src)
    classes = []
    for match in CLASS_RE.finditer(mask):
        opening = mask.find("{", match.end())
        if opening < 0:
            continue
        closing = matching(mask, opening)
        parent = next((c for c in reversed(classes) if c["open"] < match.start() < c["close"]), None)
        header_start = max(mask.rfind(";", 0, match.start()), mask.rfind("{", 0, match.start()), mask.rfind("}", 0, match.start())) + 1
        tags = []
        for tag in re.finditer(r"@(?:org\.junit\.jupiter\.api\.)?Tag\s*\(", mask[header_start:match.start()]):
            tag_start = header_start + tag.start()
            value = re.match(r'@[\w.]+\s*\(\s*"([^"\n]+)"\s*\)', src[tag_start:])
            if value:
                tags.append(value.group(1))
        superclass = re.search(r"\bextends\s+([\w.$]+)", mask[match.end():opening])
        name = match.group(1)
        classes.append({
            "file": file_rel, "name": name,
            "qualified_name": (parent["qualified_name"] + "." if parent else "") + name,
            "parent": parent["qualified_name"] if parent else "",
            "super_class": superclass.group(1) if superclass else "",
            "tags": tags, "tag": tags[0] if tags else "",
            "start_line": src.count("\n", 0, match.start()) + 1,
            "end_line": src.count("\n", 0, closing) + 1,
            "open": opening, "close": closing,
        })

    annotation_spans = []
    for match in re.finditer(r"@[\w.$]+", mask):
        end = match.end()
        while end < len(mask) and mask[end].isspace():
            end += 1
        if mask[end:end + 1] == "(":
            end = matching(mask, end, "(", ")") + 1
        annotation_spans.append((match.start(), end))

    methods = []
    for annotation in TEST_ANNOTATION_RE.finditer(mask):
        pos = annotation.end()
        while pos < len(mask):
            while pos < len(mask) and mask[pos].isspace():
                pos += 1
            if mask[pos:pos + 1] == "(":
                pos = matching(mask, pos, "(", ")") + 1
                continue
            extra = re.match(r"@[\w.$]+", mask[pos:])
            if extra:
                pos += extra.end()
                continue
            break
        sig_start = pos
        annotation_start = annotation.start()
        for start, end in reversed(annotation_spans):
            if end <= annotation_start and not mask[end:annotation_start].strip():
                annotation_start = start
        paren = mask.find("(", sig_start)
        if paren < 0:
            continue
        signature = mask[sig_start:paren]
        name = re.search(r"([A-Za-z_$][\w$]*)\s*$", signature)
        if not name or any(ch in signature for ch in "{};"):
            continue
        params_end = matching(mask, paren, "(", ")")
        opening = mask.find("{", params_end)
        if opening < 0 or ";" in mask[params_end:opening]:
            continue
        closing = matching(mask, opening)
        owner = next((c for c in reversed(classes) if c["open"] < sig_start < c["close"]), None)
        if not owner:
            continue
        sig_body = src[sig_start:closing + 1]
        methods.append({
            "file": file_rel, "class_name": owner["qualified_name"], "class_tag": owner["tag"],
            "name": name.group(1), "annotation": annotation.group(1),
            "annotation_key": normalize_tokens(src[annotation_start:sig_start]),
            "parameter_signature": normalize_tokens(src[paren:params_end + 1]),
            "start_line": src.count("\n", 0, annotation.start()) + 1,
            "sig_line": src.count("\n", 0, sig_start) + 1,
            "end_line": src.count("\n", 0, closing) + 1,
            "body": src[annotation.start():closing + 1], "sig_body": sig_body,
            "has_test_anno": True,
            "has_assertion": bool(ASSERTION_RE.search(mask[opening:closing])),
            "norm_body": normalize_body(sig_body),
        })
    for cls in classes:
        del cls["open"], cls["close"]
    return classes, methods


def inventory(root, only_package=None):
    root = Path(root)
    test_root = root / "src/test/java"
    files = sorted(test_root.rglob("*.java"))
    if only_package:
        package_root = test_root / only_package.replace(".", "/")
        files = [p for p in files if p.is_relative_to(package_root)]
    result = {"files": [], "classes": [], "methods": []}
    for path in files:
        rel = path.relative_to(root).as_posix()
        classes, methods = parse_source(path.read_text(encoding="utf-8"), rel)
        result["files"].append(rel)
        result["classes"].extend(classes)
        result["methods"].extend(methods)

    by_key = {(c["file"], c["qualified_name"]): c for c in result["classes"]}
    by_name = {}
    for cls in result["classes"]:
        by_name.setdefault(cls["name"], []).append(cls)

    def inherited_tags(cls, seen):
        key = (cls["file"], cls["qualified_name"])
        if key in seen:
            return set()
        seen = seen | {key}
        tags = set(cls["tags"])
        parent = by_key.get((cls["file"], cls["parent"]))
        if parent:
            tags.update(inherited_tags(parent, seen))
        candidates = by_name.get(cls["super_class"].split(".")[-1], [])
        if len(candidates) == 1:
            tags.update(inherited_tags(candidates[0], seen))
        return tags

    for cls in result["classes"]:
        cls["effective_tags"] = sorted(inherited_tags(cls, set()))
    for method in result["methods"]:
        owner = by_key[(method["file"], method["class_name"])]
        method["class_tags"] = owner["effective_tags"]
        method["class_tag"] = owner["tag"] or next(iter(owner["effective_tags"]), "")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--out", type=Path)
    parser.add_argument("--only-package")
    args = parser.parse_args()
    result = inventory(args.root, args.only_package)
    text = json.dumps(result, indent=2) + "\n"
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(text, encoding="utf-8")
        print(f"Wrote {args.out}: {len(result['files'])} files, {len(result['methods'])} test methods")
    else:
        print(text, end="")


if __name__ == "__main__":
    main()
