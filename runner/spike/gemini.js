// Minimal Gemini client.
// - No temperature/top_p/top_k: deprecated on Gemini 3.x (ignored on 3.6 Flash, 400 on future models).
// - thinkingLevel controls reasoning depth (minimal|low|medium|high). Never combine with thinkingBudget.
// - schema -> responseSchema (OpenAPI subset); jsonSchema -> responseJsonSchema (standard JSON Schema,
//   output follows the schema's key order).
async function callGemini(prompt, { schema, jsonSchema, model: modelOverride, thinkingLevel } = {}) {
  const apiKey = process.env.GEMINI_API_KEY;
  const model = modelOverride || process.env.GEMINI_MODEL;
  if (!apiKey || !model) throw new Error("GEMINI_API_KEY and GEMINI_MODEL must be set");

  const generationConfig = { responseMimeType: "application/json" };
  if (schema) generationConfig.responseSchema = schema;
  if (jsonSchema) generationConfig.responseJsonSchema = jsonSchema;
  if (thinkingLevel) generationConfig.thinkingConfig = { thinkingLevel };

  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json", "x-goog-api-key": apiKey },
    body: JSON.stringify({
      contents: [{ role: "user", parts: [{ text: prompt }] }],
      generationConfig,
    }),
  });
  if (!res.ok) {
    const err = new Error(`Gemini HTTP ${res.status}: ${(await res.text()).slice(0, 300)}`);
    err.status = res.status;
    throw err;
  }
  const body = await res.json();
  const text = (body.candidates?.[0]?.content?.parts ?? [])
    .filter((p) => !p.thought)
    .map((p) => p.text ?? "")
    .join("");
  return {
    text,
    tokensIn: body.usageMetadata?.promptTokenCount ?? 0,
    tokensOut: body.usageMetadata?.candidatesTokenCount ?? 0,
    tokensThinking: body.usageMetadata?.thoughtsTokenCount ?? 0,
  };
}

function parseJson(text) {
  const cleaned = text.trim().replace(/^```(?:json)?\s*/i, "").replace(/```$/, "").trim();
  return JSON.parse(cleaned);
}

module.exports = { callGemini, parseJson };
