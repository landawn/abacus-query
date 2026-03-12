#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const mainRoot = path.join(root, "src", "main", "java");
const testRoot = path.join(root, "src", "test", "java");

function walk(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    const files = [];

    for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);

        if (entry.isDirectory()) {
            files.push(...walk(fullPath));
        } else if (entry.isFile() && entry.name.endsWith(".java")) {
            files.push(fullPath);
        }
    }

    return files;
}

function stripComments(source) {
    return source
        .replace(/\/\*[\s\S]*?\*\//g, " ")
        .replace(/\/\/.*$/gm, " ");
}

function parseMethodParameters(signature) {
    const params = signature.trim();

    if (!params) {
        return [];
    }

    let depth = 0;
    let current = "";
    const parts = [];

    for (const ch of params) {
        if (ch === "<") {
            depth += 1;
        } else if (ch === ">") {
            depth = Math.max(0, depth - 1);
        } else if (ch === "," && depth === 0) {
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

function classifyParameterKinds(parameters) {
    return parameters.map(param => {
        const normalized = param
            .replace(/@\w+(?:\([^)]*\))?\s*/g, "")
            .replace(/\bfinal\s+/g, "")
            .trim();

        if (normalized.endsWith("... values")) {
            return "varargs";
        }

        if (/\bCollection<|\bList<|\bSet<|\bMap</.test(normalized)) {
            return "collection";
        }

        if (/\[\]/.test(normalized)) {
            return "array";
        }

        if (/\bSubQuery\b/.test(normalized)) {
            return "subquery";
        }

        if (/\bCondition\b/.test(normalized)) {
            return "condition";
        }

        return "other";
    });
}

function extractPublicMethods(filePath) {
    const source = stripComments(fs.readFileSync(filePath, "utf8"));
    const lines = source.split(/\r?\n/);
    const methods = [];

    for (let i = 0; i < lines.length; i += 1) {
        const line = lines[i].trim();

        if (!line.includes("public ") || !line.includes("(") || line.startsWith("package ") || line.startsWith("import ")) {
            continue;
        }

        let signature = line;
        let j = i;

        while (!/[{;]\s*$/.test(signature.trim()) && j + 1 < lines.length) {
            j += 1;
            signature += ` ${lines[j].trim()}`;
        }

        const normalized = signature.replace(/\s+/g, " ");

        if (/public (?:class|interface|enum|record)\b/.test(normalized)) {
            i = j;
            continue;
        }

        const match = normalized.match(/public\s+(?:static\s+)?(?:<[^>]+>\s+)?(?:[\w.$<>\[\],?]+\s+)+(?<name>[\w$]+)\s*\((?<params>[^)]*)\)\s*(?:throws\s+[^{;]+)?[{;]/);

        if (!match) {
            i = j;
            continue;
        }

        const name = match.groups.name;

        methods.push({
            file: filePath,
            line: i + 1,
            name,
            params: parseMethodParameters(match.groups.params),
            parameterKinds: classifyParameterKinds(parseMethodParameters(match.groups.params))
        });

        i = j;
    }

    return methods;
}

function extractTestMethods(filePath) {
    if (!fs.existsSync(filePath)) {
        return [];
    }

    const source = stripComments(fs.readFileSync(filePath, "utf8"));
    const lines = source.split(/\r?\n/);
    const tests = [];

    for (let i = 0; i < lines.length; i += 1) {
        if (!/@Test\b/.test(lines[i])) {
            continue;
        }

        let j = i + 1;
        while (j < lines.length && !/\(/.test(lines[j])) {
            j += 1;
        }

        if (j >= lines.length) {
            continue;
        }

        const signature = lines[j].trim().replace(/\s+/g, " ");
        const match = signature.match(/(?:public\s+)?void\s+([\w$]+)\s*\(/);

        if (match) {
            tests.push({ line: j + 1, name: match[1] });
        }
    }

    return tests;
}

function expectedTestPath(sourcePath) {
    const relative = path.relative(mainRoot, sourcePath);
    return path.join(testRoot, relative.replace(/\.java$/, "Test.java"));
}

function scoreCoverage(method, testName) {
    const lower = testName.toLowerCase();
    const methodName = method.name.toLowerCase();

    if (lower.includes(methodName)) {
        return 10;
    }

    if (method.name === "operator" && lower.includes("getoperator")) {
        return 9;
    }

    if (method.name === "toString" && lower.includes("tostring")) {
        return 9;
    }

    if (method.name === "getParameters" && lower.includes("parameter")) {
        return 8;
    }

    if (method.name === "clearParameters" && lower.includes("clearparameter")) {
        return 8;
    }

    if (method.name.startsWith("get") && lower.includes(method.name.slice(3).toLowerCase())) {
        return 7;
    }

    return 0;
}

const sourceFiles = walk(mainRoot).filter(file => !file.endsWith("package-info.java"));
const report = [];

for (const sourceFile of sourceFiles) {
    const testFile = expectedTestPath(sourceFile);
    const methods = extractPublicMethods(sourceFile);
    const tests = extractTestMethods(testFile);

    const gaps = methods.filter(method => !tests.some(test => scoreCoverage(method, test.name) > 0));

    report.push({
        sourceFile: path.relative(root, sourceFile),
        testFile: path.relative(root, testFile),
        testExists: fs.existsSync(testFile),
        publicMethodCount: methods.length,
        testMethodCount: tests.length,
        unmatchedMethods: gaps.map(method => ({
            line: method.line,
            name: method.name,
            arity: method.params.length,
            parameterKinds: method.parameterKinds
        }))
    });
}

console.log(JSON.stringify(report, null, 2));
