// Spec-first generation for the kata pipeline.
// Call 1 produces a structured spec (schema-enforced by Gemini).
// Call 2 produces the author artifacts (statement, reference, suite) from the spec.
// Call 3 (blind solver, in blind.js) only ever sees statement + template.

const { EXAMPLE_SPEC, EXAMPLE_OUTPUT } = require("./example-kata");

const SPEC_SCHEMA = {
  type: "object",
  properties: {
    title: { type: "string" },
    tags: { type: "array", items: { type: "string" } },
    function_name: { type: "string" },
    param_names: { type: "array", items: { type: "string" } },
    return_type: { type: "string" },
    rules: { type: "array", items: { type: "string" } },
    // input_json / output_json are JSON-encoded strings so inputs can be any shape.
    examples: {
      type: "array",
      items: {
        type: "object",
        properties: { input_json: { type: "string" }, output_json: { type: "string" } },
        required: ["input_json", "output_json"],
      },
    },
    edge_cases: {
      type: "array",
      items: {
        type: "object",
        properties: {
          input_json: { type: "string" },
          output_json: { type: "string" },
          why: { type: "string" },
        },
        required: ["input_json", "output_json", "why"],
      },
    },
  },
  required: ["title", "function_name", "param_names", "return_type", "rules", "examples", "edge_cases"],
};

// Key order matters: with structured output the model emits fields in schema order.
// rule_coverage comes first so the model plans which test covers each rule before
// it writes the statement, reference, and suite. All fields are required.
const AUTHOR_SCHEMA = {
  type: "object",
  properties: {
    rule_coverage: {
      type: "array",
      items: { type: "string" },
      description:
        'One entry per rule and per edge case, formatted "<rule or edge case> => <exact name of the test that covers it>".',
    },
    statement_md: {
      type: "string",
      description: "Markdown problem statement for the user. Must not reveal the implementation.",
    },
    reference_solution: {
      type: "string",
      description: "Complete ES module source for src/solution.mjs.",
    },
    test_suite: {
      type: "string",
      description: "Complete ES module source for tests/suite.test.mjs.",
    },
  },
  required: ["rule_coverage", "statement_md", "reference_solution", "test_suite"],
};

function specPrompt({ category, subtopic, concept, difficulty }) {
  return `Design the contract for one ${difficulty} JavaScript coding kata.

Category: ${category}
Subtopic: ${subtopic}
Concept: ${concept}

Produce a precise specification. Rules:
- Keep the kata focused on exactly one task. No side requirements.
- Write rules precisely enough that two correct implementations cannot disagree
  on any input. State tie-breaking and ordering explicitly.
- Every rule must be testable from the function's input and output alone.
- The function has one or more named parameters with JS types (e.g. number[], string).
- examples: 2 to 4 representative cases. input_json is a JSON array of the
  positional arguments, encoded as a string. output_json is the expected return
  value, encoded as a JSON string. Example: input_json "[[1,-1,2]]", output_json "[2]".
- edge_cases: 2 to 4 boundary cases in the same encoding, each with a short why.
- All example and edge_case outputs must be correct for the rules you wrote.`;
}

function authorPrompt(spec) {
  const sig = `${spec.function.name}(${spec.function.params.map((p) => p.name).join(", ")})`;
  return `<role>
You are the author of a JavaScript kata for a coding-practice platform.
</role>

<constraints>
- reference_solution is the ES module for src/solution.mjs. It has one named export
  matching the signature exactly: export function ${spec.function.name}(...).
- test_suite is the ES module for tests/suite.test.mjs. It starts with exactly
  import { test, expect } from "vitest"; and imports { ${spec.function.name} } from "../src/solution.mjs".
- Write one labelled test per rule and one per edge case. Use expect(...).toEqual(...).
  Do not depend on files, network, timers, or randomness.
- statement_md describes ${sig}, every rule, and the examples. It must not reveal the implementation.
- Every behavior the tests check must be stated in statement_md. A user who reads only the
  statement must be able to predict the result of every test.
- Do not add behavior that the specification does not define.
</constraints>

<example>
<specification>
${JSON.stringify(EXAMPLE_SPEC, null, 2)}
</specification>
<output>
${JSON.stringify(EXAMPLE_OUTPUT, null, 2)}
</output>
</example>

<specification>
${JSON.stringify(spec, null, 2)}
</specification>

Based on the specification above, produce the kata for ${sig} in the same format as the example.`;
}

module.exports = { SPEC_SCHEMA, AUTHOR_SCHEMA, specPrompt, authorPrompt };
