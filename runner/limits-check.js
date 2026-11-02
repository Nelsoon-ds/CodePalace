const { execSync } = require("node:child_process");
const { runInSandbox } = require("./test-sandbox");

const suite = (body, timeout = 60000) => [
  {
    name: "tests/suite.test.mjs",
    content: `import { test, expect } from "vitest";
import fs from "node:fs";
import { spawn } from "node:child_process";
test("payload", async () => {
${body}
}, ${timeout});
`,
  },
];

const cases = [
  {
    name: "infinite loop is killed by the watchdog",
    files: suite("while (true) {}"),
    opts: { timeoutMs: 3000 },
    check: (r) => r.timedOut && r.exitCode === 137,
  },
  {
    name: "PID cap stops fork exhaustion (spawn fails with EAGAIN)",
    files: suite(`
  const kids = [];
  let err;
  for (let i = 0; i < 400; i++) {
    try {
      kids.push(spawn("sleep", ["30"], { stdio: "ignore" }));
    } catch (e) { err = e; break; }
    if (kids.length !== i + 1) break;
  }
  const failed = await new Promise((res) => {
    let pending = kids.length, bad = 0;
    if (!pending) return res(0);
    kids.forEach((k) => {
      k.on("error", () => { bad++; if (--pending === 0) res(bad); });
      k.on("spawn", () => { if (--pending === 0) res(bad); });
    });
  });
  kids.forEach((k) => k.kill());
  expect(failed + (err ? 1 : 0)).toBeGreaterThan(0);
  expect(kids.length - failed).toBeLessThan(128);
`),
    opts: { timeoutMs: 15000 },
    check: (r) => r.exitCode === 0 && !r.timedOut,
  },
  {
    name: "unbounded fork bomb is contained and container removed",
    files: suite(`
  for (;;) { try { spawn("sh", ["-c", "while :; do :; done"], { stdio: "ignore" }); } catch {} }
`),
    opts: { timeoutMs: 4000 },
    check: (r) => r.timedOut,
  },
  {
    name: "memory hog is OOM-killed (cap 256MB)",
    files: suite(`
  const hold = [];
  for (;;) hold.push(Buffer.alloc(32 * 1024 * 1024, 1));
`),
    opts: { timeoutMs: 20000 },
    check: (r) => r.oomKilled === true && !r.timedOut,
  },
  {
    name: "log flood is tail-capped at 64KB without bloating the runner",
    files: suite(`
  const line = "x".repeat(1000);
  const fd = fs.openSync("/proc/1/fd/1", "w");
  for (let i = 0; i < 20000; i++) fs.writeSync(fd, line + "\\n");
`),
    opts: { timeoutMs: 20000 },
    check: (r) => r.truncated && r.stdout.length <= 64 * 1024,
    after: (r) => `stdout kept ${r.stdout.length} bytes`,
  },
  {
    name: "disk fill: every kata-writable path is a size-capped tmpfs",
    files: suite(`
  const paths = ["/tmp", "/var/tmp", "/home/kata", "/sandbox/tmp", "/sandbox/.vitest", "/sandbox", "/etc", "/usr/local", "/"];
  const written = {};
  for (const dir of paths) {
    let total = 0;
    try {
      const fd = fs.openSync(dir + "/fill.bin", "w");
      const chunk = Buffer.alloc(1024 * 1024, 1);
      for (let i = 0; i < 200; i++) { fs.writeSync(fd, chunk); total += chunk.length; }
    } catch {}
    try { fs.unlinkSync(dir + "/fill.bin"); } catch {}
    written[dir] = Math.round(total / 1024 / 1024);
  }
  fs.writeSync(fs.openSync("/proc/1/fd/1", "w"), "WRITTEN=" + JSON.stringify(written) + "\\n");
  expect(Math.max(...Object.values(written))).toBeLessThan(70);
`),
    opts: { timeoutMs: 30000 },
    check: (r) => r.exitCode === 0 && !r.timedOut,
    after: (r) => (r.stdout.match(/WRITTEN=(\{.*?\})/) || [])[0] || "no write report",
  },
];

const leftover = () =>
  execSync("docker ps -a --filter ancestor=codepalace/runner-node:22 -q").toString().trim();

(async () => {
  let failures = 0;
  for (const c of cases) {
    const t0 = Date.now();
    const r = await runInSandbox(c.files, c.opts);
    const ok = c.check(r);
    if (!ok) failures++;
    const extra = c.after ? ` | ${c.after(r)}` : "";
    console.log(
      `${ok ? "PASS" : "FAIL"} - ${c.name}\n       exit=${r.exitCode} timedOut=${r.timedOut} oom=${r.oomKilled} ${Date.now() - t0}ms${extra}`,
    );
  }
  const left = leftover();
  console.log(`\nleftover containers: ${left ? left : "none"}`);
  if (left) failures++;
  process.exitCode = failures ? 1 : 0;
})();
