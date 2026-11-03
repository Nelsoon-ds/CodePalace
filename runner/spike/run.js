const fs = require("node:fs");
const path = require("node:path");
const { generateSpec, generateAuthor, specThinking, authorThinking } = require("./generate");
const { buildTemplate } = require("./template");
const { blindSolve } = require("./blind");
const { validate } = require("./validate");

const REPO_ROOT = path.resolve(__dirname, "../..");

function loadDotenv() {
  const file = path.join(REPO_ROOT, ".env");
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, "utf8").split("\n")) {
    const m = line.match(/^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (m && process.env[m[1]] === undefined) process.env[m[1]] = m[2];
  }
}

const CHECK_KEYS = [
  "reference_passes",
  "starter_fails",
  "examples_match_reference",
  "mutations_fail",
  "blind_solver_passes",
];

function csvCell(v) {
  const s = String(v);
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

async function buildKata(index, params) {
  const specRes = await generateSpec(params);
  const spec = specRes.spec;
  const template = buildTemplate(spec);
  const authorRes = await generateAuthor(spec);
  const blindRes = await blindSolve(authorRes.artifacts.statement_md, template);
  const verdict = await validate(spec, authorRes.artifacts, blindRes.solution);

  const tokensIn = specRes.tokens.in + authorRes.tokens.in + blindRes.tokens.in;
  const tokensOut = specRes.tokens.out + authorRes.tokens.out + blindRes.tokens.out;
  const tokensThinking = specRes.tokens.thinking + authorRes.tokens.thinking + blindRes.tokens.thinking;

  return {
    index,
    status: "validated",
    title: spec.title,
    passed: verdict.passed,
    checks: verdict.checks,
    tokensIn,
    tokensOut,
    tokensThinking,
    // What the UI would receive. No suite, no reference.
    ui: {
      title: spec.title,
      statement_md: authorRes.artifacts.statement_md,
      template,
      examples: spec.examples,
      tags: spec.tags,
      difficulty: params.difficulty,
    },
    // Server-only.
    private: {
      spec,
      rule_coverage: authorRes.artifacts.rule_coverage,
      reference_solution: authorRes.artifacts.reference_solution,
      test_suite: authorRes.artifacts.test_suite,
      blind_solution: blindRes.solution,
      details: verdict.details,
    },
  };
}

async function main() {
  loadDotenv();
  if (!process.env.GEMINI_API_KEY || !process.env.GEMINI_MODEL) {
    throw new Error("GEMINI_API_KEY and GEMINI_MODEL must be set in .env");
  }

  const params = {
    category: "algorithms",
    subtopic: "arrays",
    concept: "neutralisation: values of equal magnitude and opposite sign cancel each other; scanning left to right, each value neutralises the most recent unmatched value of opposite sign; return the unmatched values in original order",
    difficulty: "easy",
  };
  const count = 10;
  console.log(`spec: ${process.env.GEMINI_MODEL} (thinking ${specThinking()}) | author: ${process.env.AUTHOR_MODEL || process.env.GEMINI_MODEL} (thinking ${authorThinking()}) | blind: ${process.env.GEMINI_MODEL} (default thinking)`);

  const outDir = path.join("/tmp/opencode/spike", new Date().toISOString().replace(/[:.]/g, "-"));
  fs.mkdirSync(outDir, { recursive: true });
  const csvRows = [["index", "title", "passed", ...CHECK_KEYS, "tokens_in", "tokens_out", "tokens_thinking", "error"].join(",")];

  const records = [];
  for (let i = 1; i <= count; i++) {
    let rec;
    try {
      rec = await buildKata(i, params);
      const verdict = rec.passed ? "PASS" : "FAIL " + JSON.stringify(rec.checks);
      console.log(`[${i}/${count}] ${rec.title} -> ${verdict}`);
    } catch (err) {
      if (err.status >= 400 && err.status < 500) throw err;
      rec = { index: i, status: "generation_failed", error: err.message, checks: {}, tokensIn: 0, tokensOut: 0, tokensThinking: 0 };
      console.log(`[${i}/${count}] GENERATION FAILED: ${err.message}`);
    }
    records.push(rec);
    fs.writeFileSync(path.join(outDir, `kata-${i}.json`), JSON.stringify(rec, null, 2));
    csvRows.push([
      rec.index,
      csvCell(rec.title ?? ""),
      rec.passed ?? false,
      ...CHECK_KEYS.map((k) => rec.checks?.[k] ?? ""),
      rec.tokensIn,
      rec.tokensOut,
      rec.tokensThinking ?? 0,
      csvCell(rec.error ?? ""),
    ].join(","));
  }

  const csvPath = path.join(outDir, "results.csv");
  fs.writeFileSync(csvPath, csvRows.join("\n") + "\n");

  const generated = records.filter((r) => r.status === "validated");
  const summary = {
    total: records.length,
    generation_failed: records.length - generated.length,
    fully_passed: generated.filter((r) => r.passed).length,
    per_check: Object.fromEntries(CHECK_KEYS.map((k) => [k, generated.filter((r) => r.checks[k]).length])),
  };
  console.log("\nSummary:", JSON.stringify(summary, null, 2));
  console.log(`CSV: ${csvPath}`);
  console.log(`Output: ${outDir}`);
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
