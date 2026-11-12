#!/usr/bin/env node
/**
 * One-shot local verification for the Personal Finance Planner.
 *
 * Runs:
 *   1. backend Maven tests
 *   2. frontend production build
 *   3. optionally the runtime smoke test (--smoke)
 *
 * Usage:
 *   node scripts/verify.mjs
 *   node scripts/verify.mjs --skip-frontend
 *   node scripts/verify.mjs --smoke
 */

import { spawnSync } from "node:child_process";
import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const flags = new Set(process.argv.slice(2));

if (flags.has("--help") || flags.has("-h")) {
  console.log(`Usage: node scripts/verify.mjs [options]

Options:
  --skip-backend    Skip the Maven test run
  --skip-frontend   Skip the frontend production build
  --smoke           Also run scripts/smoke.mjs against a running stack
  -h, --help        Show this help

Environment:
  API_URL, WEB_URL, SMOKE_TIMEOUT_MS are passed through to the smoke test.`);
  process.exit(0);
}

const isWindows = process.platform === "win32";
const mvn = isWindows ? "mvn.cmd" : "mvn";
const npm = isWindows ? "npm.cmd" : "npm";

const steps = [];

if (!flags.has("--skip-backend")) {
  steps.push({
    name: "Backend tests (mvn -B test)",
    cmd: mvn,
    args: ["-B", "test"],
    cwd: join(root, "backend"),
    shell: isWindows,
  });
}

if (!flags.has("--skip-frontend")) {
  if (!existsSync(join(root, "frontend", "node_modules"))) {
    console.error(
      "frontend/node_modules is missing. Run `cd frontend && npm ci` first, " +
        "or use --skip-frontend.",
    );
    process.exit(1);
  }
  steps.push({
    name: "Frontend production build (npm run build)",
    cmd: npm,
    args: ["run", "build"],
    cwd: join(root, "frontend"),
    shell: isWindows,
  });
}

if (flags.has("--smoke")) {
  steps.push({
    name: "Runtime smoke test (scripts/smoke.mjs)",
    cmd: process.execPath,
    args: [join(root, "scripts", "smoke.mjs")],
    cwd: root,
    shell: false,
  });
}

if (steps.length === 0) {
  console.log("Nothing to run: every step was skipped.");
  process.exit(0);
}

console.log("Personal Finance Planner — local verification");

const results = [];

for (const [index, step] of steps.entries()) {
  console.log(`\n[${index + 1}/${steps.length}] ${step.name}`);
  console.log(`  cwd: ${step.cwd}`);
  console.log(`  $ ${step.cmd} ${step.args.join(" ")}`);

  const started = Date.now();
  const result = spawnSync(step.cmd, step.args, {
    cwd: step.cwd,
    stdio: "inherit",
    shell: step.shell,
  });
  const seconds = ((Date.now() - started) / 1000).toFixed(1);

  if (result.error) {
    console.error(`  ERROR: ${result.error.message}`);
    results.push({ name: step.name, ok: false, detail: `error: ${result.error.message}` });
    continue;
  }

  if (result.status !== 0) {
    results.push({
      name: step.name,
      ok: false,
      detail: `exit code ${result.status}${result.signal ? ` (${result.signal})` : ""}`,
    });
    continue;
  }

  results.push({ name: step.name, ok: true, detail: `${seconds}s` });
}

console.log("\nSummary");
for (const result of results) {
  console.log(`  ${result.ok ? "PASS" : "FAIL"}  ${result.name}  (${result.detail})`);
}

const failed = results.filter((result) => !result.ok);
if (failed.length > 0) {
  console.error(`\nVerification failed: ${failed.length} step(s) did not pass.`);
  process.exit(1);
}

console.log("\nAll verification steps passed.");
