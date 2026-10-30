const Docker = require("dockerode");
const tar = require("tar-stream");

const docker = new Docker({ socketPath: "/var/run/docker.sock" });

const IMAGE = "codepalace/runner-node:22";
const SANDBOX_DIR = "/sandbox";
const KATA_UID = 10001;

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

// Docker multiplexes stdout/stderr: 8-byte header [type, 0, 0, 0, size(uint32 BE)] per frame.
function demux(buffer) {
  let stdout = "";
  let stderr = "";
  let offset = 0;
  while (offset + 8 <= buffer.length) {
    const type = buffer[offset];
    const size = buffer.readUInt32BE(offset + 4);
    const chunk = buffer.toString("utf8", offset + 8, offset + 8 + size);
    if (type === 1) stdout += chunk;
    else if (type === 2) stderr += chunk;
    offset += 8 + size;
  }
  return { stdout, stderr };
}

async function runInSandbox(entries) {
  let container;
  try {
    container = await docker.createContainer({
      Image: IMAGE,
      WorkingDir: SANDBOX_DIR,
      NetworkDisabled: true,
      HostConfig: {
        CapDrop: ["ALL"],
        SecurityOpt: ["no-new-privileges"],
        Tmpfs: {
          "/tmp": `rw,noexec,nosuid,size=64m,uid=${KATA_UID}`,
          "/sandbox/tmp": `rw,noexec,nosuid,size=32m,uid=${KATA_UID}`,
        },
      },
    });

    await container.putArchive(buildTar(entries), { path: SANDBOX_DIR });

    await container.start();
    const { StatusCode } = await container.wait();

    const logs = await container.logs({ stdout: true, stderr: true });
    const { stdout, stderr } = demux(logs);

    let results = null;
    try {
      results = JSON.parse(stdout);
    } catch {
      // leave results null; caller inspects stdout/stderr
    }

    return { exitCode: StatusCode, results, stdout, stderr };
  } finally {
    if (container) {
      await container.remove({ force: true }).catch(() => {});
    }
  }
}

async function main() {
  await docker.ping();
  const { exitCode, results, stdout, stderr } = await runInSandbox(files);

  console.log("exit code:", exitCode);
  if (results) {
    console.log(`passed: ${results.numPassedTests}, failed: ${results.numFailedTests}`);
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
