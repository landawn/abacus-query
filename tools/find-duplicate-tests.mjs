#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const testRoot = path.join(repoRoot, "src", "test", "java", "com", "landawn", "abacus");
const packageRoot = path.join(testRoot);

const javaKeywords = new Set([
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while",
    "true",
    "false",
    "null",
    "var",
    "record",
    "sealed",
    "permits",
    "non-sealed",
    "yield"
]);

function walk(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    const results = [];

    for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            results.push(...walk(fullPath));
        } else if (entry.isFile() && entry.name.endsWith("Test.java")) {
            const relative = path.relative(packageRoot, fullPath).replace(/\\/g, "/");
            if (!relative.includes("/")) {
                continue;
            }

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
        const char = source[i];
        const next = source[i + 1];

        if (inLineComment) {
            if (char === "\n") {
                inLineComment = false;
                result += char;
            }
            i += 1;
            continue;
        }

        if (inBlockComment) {
            if (char === "*" && next === "/") {
                inBlockComment = false;
                i += 2;
            } else {
                i += 1;
            }
            continue;
        }

        if (inString) {
            result += char;
            if (!escaped && char === "\"") {
                inString = false;
            }
            escaped = !escaped && char === "\\";
            i += 1;
            continue;
        }

        if (inChar) {
            result += char;
            if (!escaped && char === "'") {
                inChar = false;
            }
            escaped = !escaped && char === "\\";
            i += 1;
            continue;
        }

        if (char === "/" && next === "/") {
            inLineComment = true;
            i += 2;
            continue;
        }

        if (char === "/" && next === "*") {
            inBlockComment = true;
            i += 2;
            continue;
        }

        if (char === "\"") {
            inString = true;
            escaped = false;
            result += char;
            i += 1;
            continue;
        }

        if (char === "'") {
            inChar = true;
            escaped = false;
            result += char;
            i += 1;
            continue;
        }

        result += char;
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
        const char = source[i];
        const next = source[i + 1];

        if (inLineComment) {
            if (char === "\n") {
                inLineComment = false;
            }
            continue;
        }

        if (inBlockComment) {
            if (char === "*" && next === "/") {
                inBlockComment = false;
                i += 1;
            }
            continue;
        }

        if (inString) {
            if (!escaped && char === "\"") {
                inString = false;
            }
            escaped = !escaped && char === "\\";
            continue;
        }

        if (inChar) {
            if (!escaped && char === "'") {
                inChar = false;
            }
            escaped = !escaped && char === "\\";
            continue;
        }

        if (char === "/" && next === "/") {
            inLineComment = true;
            i += 1;
            continue;
        }

        if (char === "/" && next === "*") {
            inBlockComment = true;
            i += 1;
            continue;
        }

        if (char === "\"") {
            inString = true;
            escaped = false;
            continue;
        }

        if (char === "'") {
            inChar = true;
            escaped = false;
            continue;
        }

        if (char === "{") {
            depth += 1;
        } else if (char === "}") {
            depth -= 1;
            if (depth === 0) {
                return i;
            }
        }
    }

    return -1;
}

function getLineNumber(source, index) {
    let lines = 1;
    for (let i = 0; i < index; i += 1) {
        if (source[i] === "\n") {
            lines += 1;
        }
    }
    return lines;
}

function extractLocalVariableNames(body) {
    const cleaned = stripComments(body);
    const declarationPattern =
        /(?:^|[;{}]\s*)(?:final\s+)?(?:[\w$.<>\[\]?]+(?:\s*,\s*[\w$.<>\[\]?]+)*|var)\s+([a-zA-Z_$][\w$]*)\s*=(?!=)/g;
    const names = [];
    const seen = new Set();

    let match;
    while ((match = declarationPattern.exec(cleaned)) !== null) {
        const candidate = match[1];
        if (!javaKeywords.has(candidate) && !seen.has(candidate)) {
            seen.add(candidate);
            names.push(candidate);
        }
    }

    return names;
}

function normalizeWhitespace(source) {
    return source.replace(/\s+/g, " ").trim();
}

function normalizeExact(body) {
    return normalizeWhitespace(stripComments(body))
        .replace(/\s*([(){}[\].,;:+\-*/%<>=!&|?])\s*/g, "$1")
        .trim();
}

function normalizeAlpha(body) {
    let normalized = stripComments(body);
    const localNames = extractLocalVariableNames(body);

    localNames.forEach((name, index) => {
        const token = `__var${index + 1}`;
        normalized = normalized.replace(new RegExp(`\\b${name}\\b`, "g"), token);
    });

    normalized = normalizeWhitespace(normalized)
        .replace(/\s*([(){}[\].,;:+\-*/%<>=!&|?])\s*/g, "$1")
        .trim();

    return normalized;
}

function canonicalClassName(className) {
    return className.replace(/2025(?=Test$)/, "");
}

function extractClassName(source, filePath) {
    const sanitized = stripComments(source);
    const match = sanitized.match(/\b(?:public\s+)?class\s+([A-Za-z_$][\w$]*)\b/);
    if (match) {
        return match[1];
    }

    return path.basename(filePath, ".java");
}

function extractTestMethods(filePath) {
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
                searchIndex = source.length;
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
            /\b(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:<[^>]+>\s+)?[\w$<>\[\], ?]+\s+([A-Za-z_$][\w$]*)\s*\(/m;
        const sourceAfterAnnotation = source.slice(searchIndex);
        const headerMatch = methodHeaderPattern.exec(sourceAfterAnnotation);
        if (!headerMatch) {
            continue;
        }

        const methodName = headerMatch[1];
        const headerStart = searchIndex + headerMatch.index;
        const openBraceIndex = source.indexOf("{", headerStart + headerMatch[0].length);
        if (openBraceIndex < 0) {
            continue;
        }

        const closeBraceIndex = findMatchingBrace(source, openBraceIndex);
        if (closeBraceIndex < 0) {
            continue;
        }

        const body = source.slice(openBraceIndex + 1, closeBraceIndex);
        const fullSource = source.slice(annotationIndex, closeBraceIndex + 1);
        methods.push({
            className,
            canonicalClassName: canonicalClassName(className),
            filePath,
            relativePath: path.relative(repoRoot, filePath).replace(/\\/g, "/"),
            methodName,
            line: getLineNumber(source, headerStart),
            body,
            fullSource,
            exactFingerprint: normalizeExact(body),
            alphaFingerprint: normalizeAlpha(body)
        });

        annotationPattern.lastIndex = closeBraceIndex + 1;
    }

    return methods;
}

function groupByFingerprint(methods, key) {
    const groups = new Map();

    for (const method of methods) {
        const fingerprint = method[key];
        if (!fingerprint) {
            continue;
        }

        if (!groups.has(fingerprint)) {
            groups.set(fingerprint, []);
        }
        groups.get(fingerprint).push(method);
    }

    return [...groups.values()].filter(group => group.length > 1);
}

function scoreMethod(method) {
    const nameScore = method.methodName.length;
    const bodyScore = method.body.length;
    const canonicalBonus = method.className === method.canonicalClassName ? 1000 : 0;
    return canonicalBonus + bodyScore + nameScore;
}

function reportGroup(group) {
    const ranked = [...group].sort((left, right) => scoreMethod(right) - scoreMethod(left));
    return {
        canonicalSuggestion: ranked[0],
        duplicates: ranked.slice(1)
    };
}

function main() {
    const files = walk(testRoot);
    const methods = files.flatMap(extractTestMethods);

    const exactGroups = groupByFingerprint(methods, "exactFingerprint").map(reportGroup);
    const alphaGroups = groupByFingerprint(methods, "alphaFingerprint")
        .map(reportGroup)
        .filter(group => group.duplicates.some(duplicate => duplicate.exactFingerprint !== group.canonicalSuggestion.exactFingerprint));

    const result = {
        scannedFiles: files.length,
        scannedTestMethods: methods.length,
        exactDuplicateGroups: exactGroups.map(group => ({
            canonicalSuggestion: {
                className: group.canonicalSuggestion.className,
                methodName: group.canonicalSuggestion.methodName,
                relativePath: group.canonicalSuggestion.relativePath,
                line: group.canonicalSuggestion.line
            },
            duplicates: group.duplicates.map(duplicate => ({
                className: duplicate.className,
                methodName: duplicate.methodName,
                relativePath: duplicate.relativePath,
                line: duplicate.line
            }))
        })),
        alphaDuplicateGroups: alphaGroups.map(group => ({
            canonicalSuggestion: {
                className: group.canonicalSuggestion.className,
                methodName: group.canonicalSuggestion.methodName,
                relativePath: group.canonicalSuggestion.relativePath,
                line: group.canonicalSuggestion.line
            },
            duplicates: group.duplicates.map(duplicate => ({
                className: duplicate.className,
                methodName: duplicate.methodName,
                relativePath: duplicate.relativePath,
                line: duplicate.line
            }))
        }))
    };

    console.log(JSON.stringify(result, null, 2));
}

main();
