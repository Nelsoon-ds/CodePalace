const Docker = require("dockerode");
const tar = require("tar-stream");
const { Writable } = require("node:stream");

const docker = new Docker({ socketPath: "/var/run/docker.sock" });

const IMAGE = "codepalace/runner-node:22";
const SANDBOX_DIR = "/sandbox";
const KATA_UID = 10001;
const MEMORY_BYTES = 256 * 1024 * 1024;
const TIMEOUT_MS = 10_000;
const OUTPUT_TAIL_BYTES = 64 * 1024;

const files = [
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

function buildTar(entries) {
  const pack = tar.pack();
  for (const { name, content } of entries) {
    pack.entry({ name, uid: 0, gid: 0, mode: 0o444 }, content);
  }
  pack.finalize();
  return pack;
}

// Keeps only the last `max` bytes written, so noisy output can't bloat runner memory.
class TailBuffer extends Writable {
  constructor(max = OUTPUT_TAIL_BYTES) {
    super();
    this.max = max;
    this.buf = Buffer.alloc(0);
    this.truncated = false;
  }
  _write(chunk, _enc, cb) {
    this.buf = Buffer.concat([this.buf, chunk]);
    if (this.buf.length > this.max) {
      this.buf = this.buf.subarray(this.buf.length - this.max);
      this.truncated = true;
    }
    cb();
  }
  toString() {
    return this.buf.toString("utf8");
  }
}

async function runInSandbox(entries, { timeoutMs = TIMEOUT_MS } = {}) {
  let container;
  let timer;
  try {
    container = await docker.createContainer({
      Image: IMAGE,
      WorkingDir: SANDBOX_DIR,
      NetworkDisabled: true,
      HostConfig: {
        NanoCpus: 1e9,
        PidsLimit: 128,
        MemorySwap: MEMORY_BYTES,
        Memory: MEMORY_BYTES,
        CapDrop: ["ALL"],
        SecurityOpt: ["no-new-privileges"],
        Tmpfs: {
          "/tmp": `rw,noexec,nosuid,size=64m,uid=${KATA_UID}`,
          "/sandbox/tmp": `rw,noexec,nosuid,size=32m,uid=${KATA_UID}`,
          "/sandbox/.vitest": `rw,noexec,nosuid,size=16m,uid=${KATA_UID}`,
          "/var/tmp": `rw,noexec,nosuid,size=16m,uid=${KATA_UID}`,
          "/home/kata": `rw,noexec,nosuid,size=16m,uid=${KATA_UID}`,
        },
      },
    });

    await container.putArchive(buildTar(entries), { path: SANDBOX_DIR });

    await container.start();

    const out = new TailBuffer();
    const err = new TailBuffer();
    const logStream = await container.logs({
      follow: true,
      stdout: true,
      stderr: true,
    });
    docker.modem.demuxStream(logStream, out, err);
    const logsDone = new Promise((resolve) => {
      logStream.on("end", resolve);
      logStream.on("error", resolve);
    });

    let timedOut = false;
    timer = setTimeout(() => {
      timedOut = true;
      container.kill().catch(() => {});
    }, timeoutMs);
    const { StatusCode } = await container.wait();
    clearTimeout(timer);

    await logsDone;
    const { State } = await container.inspect();
    const stdout = out.toString();
    const stderr = err.toString();

    let results = null;
    try {
      results = JSON.parse(stdout);
    } catch {
      // leave results null; caller inspects stdout/stderr
    }

    return {
      exitCode: StatusCode,
      timedOut,
      oomKilled: State.OOMKilled,
      truncated: out.truncated || err.truncated,
      results,
      stdout,
      stderr,
    };
  } finally {
    clearTimeout(timer);
    if (container) {
      await container.remove({ force: true }).catch(() => {});
    }
  }
}

async function main() {
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

if (require.main === module) {
  main().catch((err) => {
    console.error(err);
    process.exitCode = 1;
  });
}

module.exports = { runInSandbox };
