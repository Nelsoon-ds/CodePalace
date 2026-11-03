const { runInSandbox } = require("../test-sandbox");

const SOLUTION_FILE = "src/solution.mjs";
const SUITE_FILE = "tests/suite.test.mjs";
const EXAMPLE_FILE = "tests/examples.test.mjs";

const MUTATIONS = [
  { name: "constant_return", apply: (c) => c.replace(/return\s+[^;]+;/, "return null;") },
  {
    name: "inverted_condition",
    apply: (c) => {
      for (const [a, b] of [["===", "!=="], ["!==", "==="], ["==", "!="], ["!=", "=="]]) {
        if (c.includes(a)) return c.replace(a, b);
      }
      return c.replace(/(?<![=<>!])([<>])(?!=)/, (m) => (m === "<" ? ">" : "<"));
    },
  },
  {
    name: "off_by_one",
    apply: (c) => {
      if (/(?<![=!<>])(<=|>=)/.test(c)) return c.replace(/(?<![=!<>])(<=|>=)/, (m) => (m === "<=" ? "<" : ">"));
      return c.replace(/(?<![=<>!])([<>])(?!=)/, (m) => (m === "<" ? "<=" : ">="));
    },
  },
];

// Platform-generated example tests from the spec, so examples are ground truth
// regardless of what the author wrote in the suite.
function buildExampleSuite(spec) {
  const name = spec.function.name;
  const cases = [...spec.examples, ...spec.edge_cases];
  const lines = cases.map((c, i) => {
    const args = c.input.map((a) => JSON.stringify(a)).join(", ");
    return `test("example ${i + 1}", () => { expect(${name}(${args})).toEqual(${JSON.stringify(c.output)}); });`;
  });
  return `import { test, expect } from "vitest";\nimport { ${name} } from "../src/solution.mjs";\n\n${lines.join("\n")}\n`;
}

async function runFiles(files) {
  const started = Date.now();
  const r = await runInSandbox(files);
  return {
    exitCode: r.exitCode,
    timedOut: r.timedOut,
    parsed: Boolean(r.results),
    passed: r.results?.numPassedTests ?? 0,
    failed: r.results?.numFailedTests ?? 0,
    ms: Date.now() - started,
    stderrTail: r.stderr.slice(-300),
  };
}

function isGreen(run) {
  return run.parsed && run.failed === 0 && run.passed > 0 && run.exitCode === 0 && !run.timedOut;
}

const sol = (code) => ({ name: SOLUTION_FILE, content: code });
const suite = (code) => ({ name: SUITE_FILE, content: code });

async function validate(spec, artifacts, blindSolution) {
  const ref = artifacts.reference_solution;
  const suiteCode = artifacts.test_suite;
  const exampleSuite = buildExampleSuite(spec);

  const reference = await runFiles([sol(ref), suite(suiteCode)]);
  const starter = await runFiles([
    sol(`export function ${spec.function.name}(){ throw new Error("Not implemented"); }`),
    suite(suiteCode),
  ]);
  const examples = await runFiles([sol(ref), { name: EXAMPLE_FILE, content: exampleSuite }]);
  const referencePasses = isGreen(reference);

  const mutations = [];
  for (const m of MUTATIONS) {
    if (!referencePasses) { mutations.push({ name: m.name, skipped: true }); continue; }
    const mutated = m.apply(ref);
    if (mutated === ref) { mutations.push({ name: m.name, applicable: false }); continue; }
    const run = await runFiles([sol(mutated), suite(suiteCode)]);
    mutations.push({ name: m.name, applicable: true, killed: !isGreen(run) });
  }
  const applicable = mutations.filter((m) => m.applicable);

  let blind = null;
  if (blindSolution) blind = await runFiles([sol(blindSolution), suite(suiteCode)]);

  const checks = {
    reference_passes: referencePasses,
    starter_fails: !isGreen(starter),
    examples_match_reference: isGreen(examples),
    // A suite that kills no mutant is suspect. A single survivor is often an
    // equivalent mutant (e.g. `> 0` vs `>= 0` on a length), so require >=1 kill
    // rather than all. Flagged as a known spike limitation.
    mutations_fail: referencePasses && applicable.length >= 1 && applicable.some((m) => m.killed),
    blind_solver_passes: blind ? isGreen(blind) : false,
  };

  return {
    passed: Object.values(checks).every(Boolean),
    checks,
    details: { reference, starter, examples, mutations, blind },
  };
}

module.exports = { validate, buildExampleSuite };
