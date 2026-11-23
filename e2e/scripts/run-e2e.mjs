import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { dirname, join, relative, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const e2eDirectory = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const repositoryRoot = resolve(e2eDirectory, '..')
const playwrightCli = join(e2eDirectory, 'node_modules', '@playwright', 'test', 'cli.js')
const playwrightDockerImage = process.env.PLAYWRIGHT_DOCKER_IMAGE
const dockerE2eDirectory = process.platform === 'win32'
  ? e2eDirectory.replace(/^([A-Za-z]):[\\/]/, (_, drive) => `/mnt/${drive.toLowerCase()}/`).replaceAll('\\', '/')
  : e2eDirectory
const projectName = 'personal-finance-planner-e2e'
const frontendUrl = 'http://127.0.0.1:18088'
const apiUrl = `${frontendUrl}/api/auth/bootstrap`
// Stable across runs because the isolated E2E-only PostgreSQL volume persists.
const e2eDatabasePassword = 'planner-e2e-local-only'

if (!playwrightDockerImage && !existsSync(playwrightCli)) {
  console.error('E2E dependencies are missing. Run `npm ci` from the e2e/ directory first.')
  process.exit(1)
}

// Keep the temporary env file under the checkout so WSL-backed Docker CLI
// wrappers can resolve it through the repository's /mnt/<drive> mount.
const tempDirectory = await mkdtemp(join(repositoryRoot, '.e2e-tmp-'))
const envFile = join(tempDirectory, '.env')
const composeEnvFile = relative(repositoryRoot, envFile).split(sep).join('/')
const composePrefix = [
  'compose',
  '--project-name', projectName,
  '--env-file', composeEnvFile,
  '-f', 'docker-compose.yml',
  '-f', 'docker-compose.e2e.yml',
]
const composeEnvironment = {
  ...process.env,
  POSTGRES_DB: 'personal_finance_planner_e2e',
  POSTGRES_USER: 'planner',
  POSTGRES_PASSWORD: e2eDatabasePassword,
  REDIS_PASSWORD: 'e2e-only-redis-password',
}
let activeChild = null
let receivedSignal = null

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    receivedSignal = signal
    activeChild?.kill()
  })
}

function run(command, args, cwd = repositoryRoot) {
  return new Promise((resolveRun, rejectRun) => {
    const child = spawn(command, args, {
      cwd,
      env: composeEnvironment,
      stdio: 'inherit',
      windowsHide: true,
      shell: process.platform === 'win32' && command === 'docker',
    })
    activeChild = child
    child.once('error', rejectRun)
    child.once('close', (code) => {
      if (activeChild === child) activeChild = null
      resolveRun(code ?? 1)
    })
  })
}

async function waitForStack() {
  const deadline = Date.now() + 180_000
  let lastError

  while (Date.now() < deadline) {
    if (receivedSignal) throw new Error(`Interrupted by ${receivedSignal}.`)
    try {
      const [frontend, api] = await Promise.all([fetch(frontendUrl), fetch(apiUrl)])
      if (frontend.ok && api.ok) return
      lastError = new Error(`Frontend HTTP ${frontend.status}; API HTTP ${api.status}`)
    } catch (error) {
      lastError = error
    }
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 2_000))
  }

  throw new Error(`E2E stack did not become ready within 180 seconds: ${lastError}`)
}

let exitCode = 1
let stackStarted = false

try {
  await writeFile(envFile, [
    `POSTGRES_DB=${composeEnvironment.POSTGRES_DB}`,
    `POSTGRES_USER=${composeEnvironment.POSTGRES_USER}`,
    `POSTGRES_PASSWORD=${composeEnvironment.POSTGRES_PASSWORD}`,
    `REDIS_PASSWORD=${composeEnvironment.REDIS_PASSWORD}`,
    '',
  ].join('\n'), { mode: 0o600 })

  console.log('\nStarting isolated Playwright Compose stack...')
  stackStarted = true
  exitCode = await run('docker', [...composePrefix, 'up', '-d', '--build'])
  if (exitCode !== 0) throw new Error('Docker Compose failed to start the isolated E2E stack.')

  console.log('\nWaiting for frontend and backend API...')
  await waitForStack()

  console.log('\nRunning Playwright tests...')
  if (playwrightDockerImage) {
    // GitHub Actions uses the official image so Chromium and its OS libraries
    // are already present. Both commands run in the image; the bind mount
    // keeps node_modules and reports available to the host runner. Keep them
    // as separate Docker invocations so Windows docker.cmd wrappers do not
    // reinterpret shell operators or quoting.
    const dockerRunPrefix = [
      'run', '--rm', '--network', 'host',
      '--mount', `type=bind,source=${dockerE2eDirectory},target=/work`,
      '--workdir', '/work',
      playwrightDockerImage,
    ]
    exitCode = await run('docker', [...dockerRunPrefix, 'npm', 'ci', '--no-audit', '--no-fund'], e2eDirectory)
    if (exitCode === 0) {
      exitCode = await run('docker', [...dockerRunPrefix, 'npx', 'playwright', 'test', ...process.argv.slice(2)], e2eDirectory)
    }
  } else {
    exitCode = await run(process.execPath, [
      playwrightCli,
      'test',
      ...process.argv.slice(2),
    ], e2eDirectory)
  }
} catch (error) {
  console.error(`\nE2E execution failed: ${error.message}`)
  exitCode = 1
  if (stackStarted) {
    await run('docker', [...composePrefix, 'logs', '--tail=100'])
  }
} finally {
  if (stackStarted) {
    console.log('\nStopping isolated E2E containers (preserving only the E2E database volume)...')
    const downCode = await run('docker', [...composePrefix, 'down'])
    if (downCode !== 0 && exitCode === 0) exitCode = downCode
  }
  await rm(tempDirectory, { recursive: true, force: true })
}

if (receivedSignal) exitCode = receivedSignal === 'SIGINT' ? 130 : 143
process.exitCode = exitCode
