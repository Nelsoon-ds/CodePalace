const { runInSandbox } = require("./test-sandbox");

const probe = `import { test, expect } from "vitest";
import fs from "node:fs";
import net from "node:net";
import dns from "node:dns/promises";
import { execFileSync } from "node:child_process";

const writeFails = (p) => () => fs.writeFileSync(p, "pwned");

test("runs as non-root kata (uid 10001)", () => {
  expect(process.getuid()).toBe(10001);
});
test("no effective capabilities", () => {
  const s = fs.readFileSync("/proc/self/status", "utf8");
  expect(s).toMatch(/CapEff:\\s+0+\\s/);
});
test("no_new_privs is set", () => {
  const s = fs.readFileSync("/proc/self/status", "utf8");
  expect(s).toMatch(/NoNewPrivs:\\s+1/);
});
test("no outbound TCP", async () => {
  await expect(new Promise((res, rej) => {
    const s = net.connect({ host: "1.1.1.1", port: 53, timeout: 2000 }, () => res("connected"));
    s.on("error", rej);
    s.on("timeout", () => rej(new Error("timeout")));
  })).rejects.toThrow();
});
test("no DNS", async () => {
  const lookup = dns.lookup("example.com").then(() => "resolved", () => "failed");
  const timeout = new Promise((r) => setTimeout(() => r("failed"), 2000));
  expect(await Promise.race([lookup, timeout])).toBe("failed");
});
test("only loopback interface", async () => {
  const { networkInterfaces } = await import("node:os");
  expect(Object.keys(networkInterfaces())).toEqual(["lo"]);
});
test("cannot overwrite the hidden suite", () => {
  expect(writeFails("/sandbox/tests/suite.test.mjs")).toThrow(/EACCES|EPERM/);
});
test("cannot overwrite the probe itself", () => {
  expect(writeFails("/sandbox/tests/probe.test.mjs")).toThrow(/EACCES|EPERM/);
});
test("cannot create files in /sandbox/tests", () => {
  expect(writeFails("/sandbox/tests/evil.test.mjs")).toThrow(/EACCES|EPERM/);
});
test("cannot modify user code dir /sandbox/src", () => {
  expect(writeFails("/sandbox/src/evil.mjs")).toThrow(/EACCES|EPERM/);
});
test("cannot patch vitest in node_modules", () => {
  expect(writeFails("/sandbox/node_modules/vitest/package.json")).toThrow(/EACCES|EPERM/);
});
test("cannot replace vitest config", () => {
  expect(writeFails("/sandbox/vitest.config.mjs")).toThrow(/EACCES|EPERM/);
});
test("cannot write to /etc/passwd", () => {
  expect(writeFails("/etc/passwd")).toThrow(/EACCES|EPERM/);
});
test("cannot write to /usr/local/bin", () => {
  expect(writeFails("/usr/local/bin/node2")).toThrow(/EACCES|EPERM/);
});
test("/tmp is writable (expected scratch)", () => {
  fs.writeFileSync("/tmp/ok", "ok");
});
test("cannot execute binaries from /tmp (noexec)", () => {
  fs.copyFileSync("/bin/busybox", "/tmp/bb");
  fs.chmodSync("/tmp/bb", 0o755);
  expect(() => execFileSync("/tmp/bb", ["true"])).toThrow(/EACCES|Permission/);
});
test("no docker socket", () => {
  expect(fs.existsSync("/var/run/docker.sock")).toBe(false);
});
test("no host secrets in env", () => {
  const keys = Object.keys(process.env).filter((k) => /KEY|TOKEN|SECRET|PASS|DATABASE/i.test(k));
  expect(keys).toEqual([]);
});
`;

runInSandbox([
  { name: "src/solution.mjs", content: "export const x = 1;\n" },
  { name: "tests/suite.test.mjs", content: "import {test} from 'vitest'; test('noop', () => {});\n" },
  { name: "tests/probe.test.mjs", content: probe },
]).then(({ exitCode, results, stderr }) => {
  if (!results) return console.log("no results", stderr);
  for (const f of results.testResults) {
    if (f.message) console.log("FILE ERROR:", f.message);
    for (const a of f.assertionResults) {
      console.log(a.status === "passed" ? "PASS" : "FAIL", "-", a.title);
      if (a.status !== "passed") console.log("     ", (a.failureMessages[0] || "").split("\n")[0]);
    }
  }
  console.log(`\nexit ${exitCode}: ${results.numPassedTests} passed, ${results.numFailedTests} failed`);
});
