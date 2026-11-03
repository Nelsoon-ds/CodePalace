// Build the user-facing starter template from the spec, deterministically.
// No LLM: the signature always matches the contract.
function buildTemplate(spec) {
  const params = spec.function.params.map((p) => p.name).join(", ");
  return `export function ${spec.function.name}(${params}) {
  throw new Error("Not implemented");
}
`;
}

module.exports = { buildTemplate };
