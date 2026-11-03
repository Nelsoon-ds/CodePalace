const { callGemini, parseJson } = require("./gemini");

// The blind solver sees ONLY the statement and the template. No suite, no reference.
// If its solution passes the hidden suite, the statement is sufficient to solve the kata.
async function blindSolve(statementMd, template) {
  const prompt = `Solve this JavaScript kata. You are given only the problem statement and a starter template.

PROBLEM STATEMENT:
${statementMd}

STARTER TEMPLATE (src/solution.mjs):
${template}

Return a JSON object: { "solution": "<the full ES module with your implementation>" }.
Keep the same named export and signature as the template.`;

  const reply = await callGemini(prompt, {
    schema: {
      type: "object",
      properties: { solution: { type: "string" } },
      required: ["solution"],
    },
  });
  const out = parseJson(reply.text);
  return { solution: out.solution, tokens: { in: reply.tokensIn, out: reply.tokensOut, thinking: reply.tokensThinking } };
}

module.exports = { blindSolve };
