import Docker from "dockerode";
import tar from "tar-stream";
import { Writable } from "node:stream";

const docker = new Docker({ socketPath: "/var/run/docker.sock" });

// Constants for the sandbox environment
const IMAGE = "codepalace/runner-node:22";
const SANDBOX_DIR = "/sandbox";
const KATA_UID = 10001;
const MEMORY_BYTES = 256 * 1024 * 1024;
const TIMEOUT_MS = 10_000;
const OUTPUT_TAIL_BYTES = 64 * 1024;

// Helper function to build a tar archive from the provided files
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

// Runs the provided files in a sandboxed Docker container and returns the results.
export async function runInSandbox(entries, { timeoutMs = TIMEOUT_MS } = {}) {
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


