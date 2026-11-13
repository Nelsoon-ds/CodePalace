
// Code to test the sandbox runner with a set of files. This function is not used in production, but can be used for local testing.
// Add files to the `files` array and run this script to test the sandbox runner.

export const files = [
  {
    name: "src/solution.mjs",
    content: `export function reverse(s) {
  return s.split("").reverse().join("");
}
`,
  },
  {
    name: "tests/suite.test.mjs",
    content: `import { test, expect } from "vitest";
import { reverse } from "../src/solution.mjs";

test("reverses a string", () => {
  expect(reverse("abc")).toBe("cba");
});

test("handles empty string", () => {
  expect(reverse("")).toBe("");
});
`,
  },
];

async function testSandbox(files) {
  const verbose = Boolean(process.env.VERBOSE);
  const log = (...args) => verbose && console.log("[verbose]", ...args);

  log("image:", IMAGE, "| timeout:", TIMEOUT_MS, "ms | memory:", MEMORY_BYTES, "bytes");
  log("files:", files.map((f) => f.name).join(", "));
  const started = Date.now();

  await docker.ping();
  log("docker ping ok");

  const { exitCode, timedOut, oomKilled, truncated, results, stdout, stderr } =
    await runInSandbox(files);

  log(`finished in ${Date.now() - started} ms`);
  log("timedOut:", timedOut, "| oomKilled:", oomKilled, "| truncated:", truncated);
  if (verbose) {
    console.log("[verbose] stdout:\n" + stdout);
    console.log("[verbose] stderr:\n" + stderr);
  }

  console.log("exit code:", exitCode);
  if (results) {
    console.log(
      `passed: ${results.numPassedTests}, failed: ${results.numFailedTests}`,
    );
  } else {
    console.log("could not parse JSON. stdout:\n", stdout);
  }
  if (stderr) console.log("stderr:\n", stderr);
}

// Here you can see the results of running the sandbox with the provided files. You can modify the `files` array above to test different code and test cases.
testSandbox(files).catch((err) => {
  console.error("Error running testSandbox:", err);
  process.exit(1);
});