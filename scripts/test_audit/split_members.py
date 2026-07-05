#!/usr/bin/env python3
"""
Split a Java class/enum body into its direct member blocks.

A member is one of:
  - a method (with preceding annotations + modifiers + body)
  - a nested class/interface/enum/record (with preceding annotations + body)
  - a field (with preceding annotations + modifiers + initializer, ending in ';')
  - a static/instance initializer block (a '{ ... }' not part of a method)

Returns a list of dicts: {kind, name, text, start_offset, end_offset}.
  kind in {"method","class","field","init-block","unknown"}
  name  = simple name for method/class/field, "" for init-block

Used by merge_file.py to union the members of several test classes into one.
"""
from __future__ import annotations

import re
from typing import List, Dict

from java_struct import _scan_strings_comments, _find_matching_brace


CLASS_KW_RE = re.compile(r"\b(class|interface|enum|record)\b")
IDENT_RE = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*")
# method signature heuristic: [modifiers] Type name ( ... )
METHOD_RE = re.compile(
    r"^(?P<mods>(?:public|protected|private|static|final|synchronized|native|abstract|default|strictfp|\s|<[^<>]*>)*)"
    r"(?P<ret>[A-Za-z_$][\w.$]*(?:\s*<[^<>]*>)?(?:\s*\[\s*\])*)\s+"
    r"(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*\("
)


def split_members(body: str) -> List[Dict]:
    n = len(body)
    members: List[Dict] = []
    i = 0
    # We need to capture leading annotations + comments for each member.
    # Strategy: walk depth 0; accumulate a "pending" buffer of annotations/comments/ws;
    # when we hit a member start, attach the pending buffer to it.
    pending_start = 0
    while i < n:
        c = body[i]
        # whitespace
        if c in " \t\r\n":
            i += 1
            continue
        # comments
        if c == "/" and i + 1 < n and body[i+1] == "/":
            while i < n and body[i] != "\n":
                i += 1
            continue
        if c == "/" and i + 1 < n and body[i+1] == "*":
            j = i + 2
            while j + 1 < n and not (body[j] == "*" and body[j+1] == "/"):
                j += 1
            i = j + 2
            continue
        # string/char (shouldn't appear at member-start depth 0, but be safe)
        if c == '"' or c == "'":
            i = _scan_strings_comments(body, i, c)
            continue
        # annotation
        if c == "@":
            # consume @Ident(.ident)*(...)
            j = i + 1
            while j < n and (body[j].isalnum() or body[j] in "_$."):
                j += 1
            while j < n and body[j] in " \t":
                j += 1
            if j < n and body[j] == "(":
                depth = 1
                j += 1
                while j < n and depth > 0:
                    cj = body[j]
                    if cj == "(":
                        depth += 1
                    elif cj == ")":
                        depth -= 1
                    elif cj == '"':
                        j = _scan_strings_comments(body, j, '"')
                        continue
                    elif cj == "'":
                        j = _scan_strings_comments(body, j, "'")
                        continue
                    j += 1
            i = j
            continue
        # static/instance initializer block: '{' at depth 0 not preceded by a signature
        if c == "{":
            close = _find_matching_brace(body, i)
            block = body[pending_start:close+1]
            members.append({"kind": "init-block", "name": "", "text": block,
                            "start_offset": pending_start, "end_offset": close+1})
            i = close + 1
            pending_start = i
            continue
        # nested class?
        m = CLASS_KW_RE.match(body, i)
        # ensure preceding char is not ident char
        if m and (i == 0 or not (body[i-1].isalnum() or body[i-1] in "_$")):
            # find name
            rest = body[i:]
            nm = re.match(r"\b(?:class|interface|enum|record)\s+([A-Za-z_$][A-Za-z0-9_$]*)", rest)
            if nm:
                name = nm.group(1)
                # find opening brace
                ob = i + nm.end()
                while ob < n and body[ob] != "{":
                    if body[ob] == '"':
                        ob = _scan_strings_comments(body, ob, '"')
                        continue
                    if body[ob] == "'":
                        ob = _scan_strings_comments(body, ob, "'")
                        continue
                    if body[ob] == "/" and ob+1 < n and body[ob+1] == "/":
                        while ob < n and body[ob] != "\n":
                            ob += 1
                        continue
                    if body[ob] == "/" and ob+1 < n and body[ob+1] == "*":
                        ob += 2
                        while ob+1 < n and not (body[ob] == "*" and body[ob+1] == "/"):
                            ob += 1
                        ob += 2
                        continue
                    ob += 1
                if ob < n and body[ob] == "{":
                    close = _find_matching_brace(body, ob)
                    block = body[pending_start:close+1]
                    members.append({"kind": "class", "name": name, "text": block,
                                    "start_offset": pending_start, "end_offset": close+1})
                    i = close + 1
                    pending_start = i
                    continue
        # method or field: capture up to '{' ... '}' (method) or ';' (field)
        # First, capture the member header from pending_start
        # Scan forward at depth 0 (paren/angle/bracket aware) until '{' or ';'
        j = i
        depth_paren = 0
        depth_angle = 0
        depth_brack = 0
        member_end = -1
        kind = "unknown"
        name = ""
        while j < n:
            cj = body[j]
            if cj == '"':
                j = _scan_strings_comments(body, j, '"')
                continue
            if cj == "'":
                j = _scan_strings_comments(body, j, "'")
                continue
            if cj == "/" and j+1 < n and body[j+1] == "/":
                while j < n and body[j] != "\n":
                    j += 1
                continue
            if cj == "/" and j+1 < n and body[j+1] == "*":
                j += 2
                while j+1 < n and not (body[j] == "*" and body[j+1] == "/"):
                    j += 1
                j += 2
                continue
            if cj == "(":
                depth_paren += 1
                j += 1
                continue
            if cj == ")":
                depth_paren -= 1
                j += 1
                continue
            if cj == "[":
                depth_brack += 1
                j += 1
                continue
            if cj == "]":
                depth_brack -= 1
                j += 1
                continue
            if cj == "<":
                depth_angle += 1
                j += 1
                continue
            if cj == ">":
                if depth_angle > 0:
                    depth_angle -= 1
                j += 1
                continue
            if cj == "{" and depth_paren == 0:
                # method or init block body
                close = _find_matching_brace(body, j)
                block = body[pending_start:close+1]
                # determine if this is a method (has a signature with name(...) before {)
                header = body[pending_start:j]
                # strip leading annotations to find signature
                kind, name = _classify_member(header, is_block=True)
                members.append({"kind": kind, "name": name, "text": block,
                                "start_offset": pending_start, "end_offset": close+1})
                i = close + 1
                pending_start = i
                member_end = close + 1
                break
            if cj == ";" and depth_paren == 0 and depth_brack == 0:
                # field
                block = body[pending_start:j+1]
                header = body[pending_start:j+1]
                kind, name = _classify_member(header, is_block=False)
                members.append({"kind": kind, "name": name, "text": block,
                                "start_offset": pending_start, "end_offset": j+1})
                i = j + 1
                pending_start = i
                member_end = j + 1
                break
            j += 1
        if member_end < 0:
            # ran out; attach remainder as unknown
            if pending_start < n:
                members.append({"kind": "unknown", "name": "",
                                "text": body[pending_start:n],
                                "start_offset": pending_start, "end_offset": n})
            i = n
            break
    return members


def _classify_member(header: str, is_block: bool) -> (str, str):
    """Given a member header (annotations + modifiers + signature up to { or ;),
    classify as ('method', name) / ('field', name) / ('class', name) / ('unknown','')."""
    # strip annotations
    h = re.sub(r"@[A-Za-z_$][\w.$]*(?:\s*\([^)]*\))?", " ", header)
    # strip comments
    h = re.sub(r"//[^\n]*", " ", h)
    h = re.sub(r"/\*.*?\*/", " ", h, flags=re.S)
    h = h.strip()
    if not h:
        return ("unknown", "")
    # nested class?
    if re.search(r"\b(class|interface|enum|record)\b", h):
        cm = re.search(r"\b(?:class|interface|enum|record)\s+([A-Za-z_$][A-Za-z0-9_$]*)", h)
        if cm:
            return ("class", cm.group(1))
        return ("class", "")
    if is_block:
        # method: last identifier before '('
        mm = METHOD_RE.match(h)
        if mm:
            return ("method", mm.group("name"))
        # fallback: find identifier before '('
        pm = re.search(r"([A-Za-z_$][A-Za-z0-9_$]*)\s*\(", h)
        if pm:
            return ("method", pm.group(1))
        return ("method", "")
    else:
        # field: identifier before '=' or ';' or end
        # take the last identifier not followed by '('
        # remove any array dims
        # split on '='
        eqpos = h.find("=")
        decl = h[:eqpos] if eqpos >= 0 else h
        # last identifier in decl
        idents = re.findall(r"[A-Za-z_$][A-Za-z0-9_$]*", decl)
        # remove modifiers/type keywords
        bad = {"public","protected","private","static","final","transient","volatile","synchronized","native","abstract","default","strictfp",
               "void","class","interface","enum","record","extends","implements","throws","new","return","if","else","for","while","switch","case","do"}
        candidates = [x for x in idents if x not in bad]
        if candidates:
            return ("field", candidates[-1])
        return ("field", "")


if __name__ == "__main__":
    import sys
    sys.path.insert(0, "scripts/test_audit")
    body = open(sys.argv[1], encoding="utf-8").read()
    ms = split_members(body)
    for m in ms:
        head = m["text"][:80].replace("\n", " ")
        print(f"  {m['kind']:10} {m['name']:30} {head}")