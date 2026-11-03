// Hand-written few-shot example for the author prompt.
// Deliberately a different concept from the kata being generated, so it teaches
// the output format and quality bar without biasing the content.

const EXAMPLE_SPEC = {
  title: "Collapse Adjacent Duplicates",
  tags: ["arrays", "algorithms"],
  function: {
    name: "collapseAdjacent",
    params: [{ name: "values", type: "any" }],
    returns: "number[]",
  },
  rules: [
    "Scan the input from left to right.",
    "A value is dropped when it is strictly equal to the value immediately before it in the input.",
    "All other values are kept in their original order.",
    "The input array is not modified; a new array is returned.",
  ],
  examples: [
    { input: [[1, 1, 2]], output: [1, 2] },
    { input: [[3, 3, 3, 3]], output: [3] },
    { input: [[1, 2, 1]], output: [1, 2, 1] },
  ],
  edge_cases: [
    { input: [[]], output: [], why: "empty array" },
    { input: [[7]], output: [7], why: "single element" },
    { input: [[0, 0]], output: [0], why: "zero is an ordinary value" },
  ],
};

const EXAMPLE_OUTPUT = {
  rule_coverage: [
    "Rule 1: scan left to right => Rule 1: order is preserved while scanning",
    "Rule 2: drop value equal to the one before it => Rule 2: drops adjacent duplicates",
    "Rule 3: keep all other values in order => Rule 3: keeps non-adjacent repeats",
    "Rule 4: input not modified => Rule 4: does not mutate the input",
    "Edge case: empty array => Edge case: empty array",
    "Edge case: single element => Edge case: single element",
    "Edge case: zero is an ordinary value => Edge case: zeros collapse like any value",
  ],
  statement_md: `# Collapse Adjacent Duplicates

Write a function \`collapseAdjacent(values)\` that takes an array of numbers and returns a new array in which runs of equal neighbouring values are reduced to a single value.

## Rules

1. Scan the input from left to right.
2. A value is dropped when it is strictly equal to the value immediately before it in the input.
3. All other values are kept in their original order. A value that appears again later, but not directly after itself, is kept.
4. Do not modify the input array. Return a new array.

## Examples

| Input | Output |
| --- | --- |
| \`[1, 1, 2]\` | \`[1, 2]\` |
| \`[3, 3, 3, 3]\` | \`[3]\` |
| \`[1, 2, 1]\` | \`[1, 2, 1]\` |

An empty array returns an empty array, and \`0\` is treated like any other value.
`,
  reference_solution: `export function collapseAdjacent(values) {
  const result = [];
  for (let i = 0; i < values.length; i++) {
    if (i === 0 || values[i] !== values[i - 1]) {
      result.push(values[i]);
    }
  }
  return result;
}
`,
  test_suite: `import { test, expect } from "vitest";
import { collapseAdjacent } from "../src/solution.mjs";

test("Rule 1: order is preserved while scanning", () => {
  expect(collapseAdjacent([4, 5, 6])).toEqual([4, 5, 6]);
});

test("Rule 2: drops adjacent duplicates", () => {
  expect(collapseAdjacent([1, 1, 2])).toEqual([1, 2]);
  expect(collapseAdjacent([3, 3, 3, 3])).toEqual([3]);
});

test("Rule 3: keeps non-adjacent repeats", () => {
  expect(collapseAdjacent([1, 2, 1])).toEqual([1, 2, 1]);
  expect(collapseAdjacent([5, 5, 6, 6, 5])).toEqual([5, 6, 5]);
});

test("Rule 4: does not mutate the input", () => {
  const input = [1, 1, 2];
  collapseAdjacent(input);
  expect(input).toEqual([1, 1, 2]);
});

test("Edge case: empty array", () => {
  expect(collapseAdjacent([])).toEqual([]);
});

test("Edge case: single element", () => {
  expect(collapseAdjacent([7])).toEqual([7]);
});

test("Edge case: zeros collapse like any value", () => {
  expect(collapseAdjacent([0, 0])).toEqual([0]);
});
`,
};

module.exports = { EXAMPLE_SPEC, EXAMPLE_OUTPUT };
