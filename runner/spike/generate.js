const { callGemini, parseJson } = require("./gemini");
const { SPEC_SCHEMA, AUTHOR_SCHEMA, specPrompt, authorPrompt } = require("./spec");

// Thinking depth per call. Gemini 3.5 Flash-Lite defaults to "minimal", which is close to no
// reasoning, so the spec call raises it. Override with SPEC_THINKING_LEVEL / AUTHOR_THINKING_LEVEL.
const specThinking = () => process.env.SPEC_THINKING_LEVEL || "high";
const authorThinking = () => process.env.AUTHOR_THINKING_LEVEL || "high";

function requireFields(obj, fields, label) {
  for (const f of fields) {
    if (!(f in obj)) throw new Error(`${label} missing field: ${f}`);
  }
}

function decodeCases(cases) {
  return (cases ?? []).map((c) => ({
    input: JSON.parse(c.input_json),
    output: JSON.parse(c.output_json),
    why: c.why,
  }));
}

function normalizeSpec(raw) {
  return {
    title: raw.title,
    tags: raw.tags ?? [],
    function: {
      name: raw.function_name,
      params: (raw.param_names ?? []).map((n) => ({ name: n, type: "any" })),
      returns: raw.return_type ?? "",
    },
    rules: raw.rules ?? [],
    examples: decodeCases(raw.examples),
    edge_cases: decodeCases(raw.edge_cases),
  };
}

const tokensOf = (reply) => ({ in: reply.tokensIn, out: reply.tokensOut, thinking: reply.tokensThinking });

async function generateSpec(params) {
  const reply = await callGemini(specPrompt(params), {
    schema: SPEC_SCHEMA,
    thinkingLevel: specThinking(),
  });
  const spec = normalizeSpec(parseJson(reply.text));
  if (!spec.function.name) throw new Error("spec.function_name missing");
  if (!Array.isArray(spec.examples) || spec.examples.length === 0) throw new Error("spec.examples empty");
  return { spec, tokens: tokensOf(reply) };
}

async function generateAuthor(spec) {
  const reply = await callGemini(authorPrompt(spec), {
    jsonSchema: AUTHOR_SCHEMA,
    model: process.env.AUTHOR_MODEL,
    thinkingLevel: authorThinking(),
  });
  const art = parseJson(reply.text);
  requireFields(art, ["rule_coverage", "statement_md", "reference_solution", "test_suite"], "author");
  return { artifacts: art, tokens: tokensOf(reply) };
}

module.exports = { generateSpec, generateAuthor, specThinking, authorThinking };
