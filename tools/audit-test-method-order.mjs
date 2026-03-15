#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const mainRoot = path.join(repoRoot, "src", "main", "java");
const testRoot = path.join(repoRoot, "src", "test", "java");

function walk(dir) {
    const results = [];

    if (!fs.existsSync(dir)) {
        return results;
    }

    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const fullPath = path.join(dir, entry.name);

        if (entry.isDirectory()) {
            results.push(...walk(fullPath));
        } else if (entry.isFile() && entry.name.endsWith(".java")) {
            results.push(fullPath);
        }
    }

    return results;
}

function stripComments(source) {
    return source
        .replace(/\/\*[\s\S]*?\*\//g, " ")
        .replace(/\/\/.*$/gm, " ");
}

function normalizeWhitespace(value) {
    return value.replace(/\s+/g, " ").trim();
}

function splitParameters(paramsText) {
    const text = paramsText.trim();

    if (!text) {
        return [];
    }

    const parts = [];
    let current = "";
    let angleDepth = 0;
    let parenDepth = 0;
    let bracketDepth = 0;

    for (const ch of text) {
        if (ch === "<") {
            angleDepth += 1;
        } else if (ch === ">") {
            angleDepth = Math.max(0, angleDepth - 1);
        } else if (ch === "(") {
            parenDepth += 1;
        } else if (ch === ")") {
            parenDepth = Math.max(0, parenDepth - 1);
        } else if (ch === "[") {
            bracketDepth += 1;
        } else if (ch === "]") {
            bracketDepth = Math.max(0, bracketDepth - 1);
        } else if (ch === "," && angleDepth === 0 && parenDepth === 0 && bracketDepth === 0) {
            parts.push(current.trim());
            current = "";
            continue;
        }

        current += ch;
    }

    if (current.trim()) {
        parts.push(current.trim());
    }

    return parts;
}

function normalizeParameterType(parameter) {
    const cleaned = normalizeWhitespace(parameter)
        .replace(/@\w+(?:\([^)]*\))?\s*/g, "")
        .replace(/\bfinal\s+/g, "")
        .trim();

    const match = cleaned.match(/(.+?)\s+([A-Za-z_$][\w$]*)$/);
    return normalizeWhitespace(match ? match[1] : cleaned).replace(/\s*\.\.\./g, "...");
}

function relativePath(filePath) {
    return path.relative(repoRoot, filePath).replace(/\\/g, "/");
}

function expectedTestPath(sourcePath) {
    const relative = path.relative(mainRoot, sourcePath);
    return path.join(testRoot, relative.replace(/\.java$/, "Test.java"));
}

function extractPublicMethods(sourceText, sourcePath) {
    const cleaned = stripComments(sourceText);
    const fileClassName = path.basename(sourcePath, ".java");
    const methodPattern =
        /(^|\n)\s*(?:@[\w$.]+(?:\([^)]*\))?\s*)*(public)\s+(?!class\b|interface\b|enum\b|record\b)(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:default\s+)?(?:<[^>{;]+>\s+)?([\w$<>\[\].?,\s]+?)\s+([A-Za-z_$][\w$]*)\s*\(([^)]*)\)\s*(?:throws\s+[^{;]+)?([;{])/gm;

    const methods = [];
    let match;

    while ((match = methodPattern.exec(cleaned)) !== null) {
        const methodName = match[4];
        if (methodName === fileClassName) {
            continue;
        }

        methods.push({
            methodName,
            signature: `${methodName}(${splitParameters(match[5]).map(normalizeParameterType).join(", ")})`
        });
    }

    return methods;
}

function extractTestMethods(testText) {
    const methods = [];
    const annotationPattern = /(^|\n)\s*@Test\b/gm;
    let match;

    while ((match = annotationPattern.exec(testText)) !== null) {
        let searchIndex = annotationPattern.lastIndex;

        while (true) {
            const nextLineBreak = testText.indexOf("\n", searchIndex);
            if (nextLineBreak < 0) {
                break;
            }

            const line = testText.slice(searchIndex, nextLineBreak).trim();
            if (line.startsWith("@")) {
                searchIndex = nextLineBreak + 1;
                continue;
            }

            break;
        }

        const sourceAfter = testText.slice(searchIndex);
        const headerMatch =
            /\b(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:<[^>]+>\s+)?[\w$<>\[\], ?]+\s+([A-Za-z_$][\w$]*)\s*\(/m.exec(
                sourceAfter
            );

        if (!headerMatch) {
            continue;
        }

        methods.push(headerMatch[1]);
    }

    return methods;
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

function extractPrimaryTestClassSource(testText, expectedClassName) {
    const classPattern = new RegExp(`\\bpublic\\s+class\\s+${expectedClassName}\\b`);
    const match = classPattern.exec(testText);

    if (!match) {
        return testText;
    }

    const braceIndex = testText.indexOf("{", match.index);
    const closeBraceIndex = braceIndex >= 0 ? findMatchingBrace(testText, braceIndex) : -1;

    if (braceIndex < 0 || closeBraceIndex < 0) {
        return testText;
    }

    return testText.slice(match.index, closeBraceIndex + 1);
}

function inferSourceMethodName(testMethodName) {
    const rawName = testMethodName.replace(/^test/, "");
    const primary = rawName.split("_")[0];

    if (!primary) {
        return "";
    }

    return primary.charAt(0).toLowerCase() + primary.slice(1);
}

const sourceFiles = walk(mainRoot)
    .filter(file => !file.endsWith("package-info.java"))
    .filter(file => relativePath(file).startsWith("src/main/java/com/landawn/abacus/"));

const report = [];

for (const sourceFile of sourceFiles) {
    const testFile = expectedTestPath(sourceFile);

    if (!fs.existsSync(testFile)) {
        continue;
    }

    const sourceMethods = extractPublicMethods(fs.readFileSync(sourceFile, "utf8"), sourceFile);
    const order = new Map(sourceMethods.map((method, index) => [method.methodName, index]));
    const testClassName = path.basename(testFile, ".java");
    const testSource = extractPrimaryTestClassSource(fs.readFileSync(testFile, "utf8"), testClassName);
    const testMethods = extractTestMethods(testSource);
    const mapped = testMethods
        .map(name => ({ name, sourceMethodName: inferSourceMethodName(name) }))
        .filter(item => order.has(item.sourceMethodName));

    let previousIndex = -1;
    const violations = [];

    for (const item of mapped) {
        const currentIndex = order.get(item.sourceMethodName);
        if (currentIndex < previousIndex) {
            violations.push(item);
        } else {
            previousIndex = currentIndex;
        }
    }

    if (violations.length > 0) {
        report.push({
            sourceFile: relativePath(sourceFile),
            testFile: relativePath(testFile),
            violations
        });
    }
}

console.log(JSON.stringify(report, null, 2));
