#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const roots = [
    path.join(repoRoot, "src", "test", "java", "com", "landawn", "abacus", "query"),
    path.join(repoRoot, "src", "test", "java", "com", "landawn", "abacus", "query", "condition")
];

const ASSERTION_PATTERN =
    /\b(?:Assertions\.)?(?:assertEquals|assertNotEquals|assertSame|assertNotSame|assertTrue|assertFalse|assertNull|assertNotNull|assertThrows|assertDoesNotThrow|assertDoesNotThrowExactly|assertIterableEquals|assertArrayEquals|assertLinesMatch|assertInstanceOf|assertAll|assertTimeout|assertTimeoutPreemptively|fail|assertHaveSameElements)\s*\(/;

function walk(dir) {
    const results = [];

    if (!fs.existsSync(dir)) {
        return results;
    }

    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            results.push(...walk(fullPath));
        } else if (entry.isFile() && entry.name.endsWith("Test.java")) {
            results.push(fullPath);
        }
    }

    return results;
}

function stripComments(source) {
    let result = "";
    let i = 0;
    let inLineComment = false;
    let inBlockComment = false;
    let inString = false;
    let inChar = false;
    let escaped = false;

    while (i < source.length) {
        const ch = source[i];
        const next = source[i + 1];

        if (inLineComment) {
            if (ch === "\n") {
                inLineComment = false;
                result += ch;
            }
            i += 1;
            continue;
        }

        if (inBlockComment) {
            if (ch === "*" && next === "/") {
                inBlockComment = false;
                i += 2;
            } else {
                i += 1;
            }
            continue;
        }

        if (inString) {
            result += ch;
            if (!escaped && ch === "\"") {
                inString = false;
            }
            escaped = !escaped && ch === "\\";
            i += 1;
            continue;
        }

        if (inChar) {
            result += ch;
            if (!escaped && ch === "'") {
                inChar = false;
            }
            escaped = !escaped && ch === "\\";
            i += 1;
            continue;
        }

        if (ch === "/" && next === "/") {
            inLineComment = true;
            i += 2;
            continue;
        }

        if (ch === "/" && next === "*") {
            inBlockComment = true;
            i += 2;
            continue;
        }

        if (ch === "\"") {
            inString = true;
            escaped = false;
            result += ch;
            i += 1;
            continue;
        }

        if (ch === "'") {
            inChar = true;
            escaped = false;
            result += ch;
            i += 1;
            continue;
        }

        result += ch;
        i += 1;
    }

    return result;
}

function findMatchingBrace(source, openBraceIndex) {
    let depth = 0;
    let inLineComment = false;
    let inBlockComment = false;
    let inString = false;
    let inChar = false;
    let escaped = false;

    for (let i = openBraceIndex; i < source.length; i += 1) {
        const ch = source[i];
        const next = source[i + 1];

        if (inLineComment) {
            if (ch === "\n") {
                inLineComment = false;
            }
            continue;
        }

        if (inBlockComment) {
            if (ch === "*" && next === "/") {
                inBlockComment = false;
                i += 1;
            }
            continue;
        }

        if (inString) {
            if (!escaped && ch === "\"") {
                inString = false;
            }
            escaped = !escaped && ch === "\\";
            continue;
        }

        if (inChar) {
            if (!escaped && ch === "'") {
                inChar = false;
            }
            escaped = !escaped && ch === "\\";
            continue;
        }

        if (ch === "/" && next === "/") {
            inLineComment = true;
            i += 1;
            continue;
        }

        if (ch === "/" && next === "*") {
            inBlockComment = true;
            i += 1;
            continue;
        }

        if (ch === "\"") {
            inString = true;
            escaped = false;
            continue;
        }

        if (ch === "'") {
            inChar = true;
            escaped = false;
            continue;
        }

        if (ch === "{") {
            depth += 1;
        } else if (ch === "}") {
            depth -= 1;
            if (depth === 0) {
                return i;
            }
        }
    }

    return -1;
}

function getLineNumber(source, index) {
    let line = 1;
    for (let i = 0; i < index; i += 1) {
        if (source[i] === "\n") {
            line += 1;
        }
    }
    return line;
}

function extractClassName(source, filePath) {
    const sanitized = stripComments(source);
    const match = sanitized.match(/\b(?:public\s+)?class\s+([A-Za-z_$][\w$]*)\b/);
    return match ? match[1] : path.basename(filePath, ".java");
}

function extractMethods(filePath) {
    const source = fs.readFileSync(filePath, "utf8");
    const className = extractClassName(source, filePath);
    const methods = [];
    const annotationPattern = /(^|\n)\s*@Test\b/gm;
    let match;

    while ((match = annotationPattern.exec(source)) !== null) {
        const annotationIndex = match.index + match[1].length;
        let searchIndex = annotationPattern.lastIndex;

        while (true) {
            const nextLineBreak = source.indexOf("\n", searchIndex);
            if (nextLineBreak < 0) {
                break;
            }

            const line = source.slice(searchIndex, nextLineBreak).trim();
            if (line.startsWith("@")) {
                searchIndex = nextLineBreak + 1;
                continue;
            }
            break;
        }

        const methodHeaderPattern =
            /\b(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:<[^>]+>\s+)?[\w$<>\[\], ?]+\s+([A-Za-z_$][\w$]*)\s*\(/m;
        const sourceAfter = source.slice(searchIndex);
        const headerMatch = methodHeaderPattern.exec(sourceAfter);
        if (!headerMatch) {
            continue;
        }

        const headerStart = searchIndex + headerMatch.index;
        const openBraceIndex = source.indexOf("{", headerStart + headerMatch[0].length);
        const closeBraceIndex = findMatchingBrace(source, openBraceIndex);
        const body = source.slice(openBraceIndex + 1, closeBraceIndex);

        methods.push({
            className,
            filePath,
            relativePath: path.relative(repoRoot, filePath).replace(/\\/g, "/"),
            methodName: headerMatch[1],
            line: getLineNumber(source, headerStart),
            body
        });

        annotationPattern.lastIndex = closeBraceIndex + 1;
    }

    return methods;
}

function hasAssertion(body) {
    const cleaned = stripComments(body);
    return ASSERTION_PATTERN.test(cleaned);
}

function main() {
    const files = roots.flatMap(walk);
    const methods = files.flatMap(extractMethods);
    const missing = methods.filter(method => !hasAssertion(method.body));

    console.log(
        JSON.stringify(
            {
                scannedFiles: files.length,
                scannedTestMethods: methods.length,
                methodsWithoutAssertions: missing.map(method => ({
                    className: method.className,
                    methodName: method.methodName,
                    relativePath: method.relativePath,
                    line: method.line
                }))
            },
            null,
            2
        )
    );
}

main();
