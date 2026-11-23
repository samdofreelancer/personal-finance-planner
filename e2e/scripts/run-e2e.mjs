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
const readinessUrl = 'http://127.0.0.1:18080/actuator/health/readiness'
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
      const [frontend, api, readiness] = await Promise.all([
        fetch(frontendUrl),
        fetch(apiUrl),
        fetch(readinessUrl),
      ])
      const health = readiness.ok ? await readiness.json() : null
      if (frontend.ok && api.ok && readiness.ok && health?.status === 'UP') return
      lastError = new Error(`Frontend HTTP ${frontend.status}; API HTTP ${api.status}; readiness ${health?.status ?? readiness.status}`)
    } catch (error) {
      lastError = error
    }
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 2_000))
  }

  throw new Error(`E2E stack did not become ready within 180 seconds: ${lastError}`)
}

function setCookieValue(response, name) {
  const headers = response.headers.getSetCookie?.() ?? [response.headers.get('set-cookie') ?? '']
  const header = headers.find((value) => value.startsWith(`${name}=`))
  if (!header) throw new Error(`${name} cookie was not returned.`)
  return header.split(';', 1)[0]
}

async function waitForSession(apiSessionCookie, expectedEmail, description) {
  const deadline = Date.now() + 90_000
  let lastStatus = 'no response'

  while (Date.now() < deadline) {
    try {
      const response = await fetch(`${frontendUrl}/api/auth/me`, {
        headers: { Cookie: apiSessionCookie },
      })
      lastStatus = `HTTP ${response.status}`
      if (response.ok) {
        const user = await response.json()
        if (user.email !== expectedEmail) throw new Error('Session resolved to an unexpected user.')
        return
      }
    } catch (error) {
      if (error.message.includes('unexpected user')) throw error
      lastStatus = error.message
    }
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 1_000))
  }

  throw new Error(`${description} did not recover the authenticated session within 90 seconds (${lastStatus}).`)
}

async function verifyRedisSessionPersistence() {
  console.log('\nVerifying Redis-backed session persistence across service restarts...')
  const csrfResponse = await fetch(`${frontendUrl}/api/csrf`)
  if (!csrfResponse.ok) throw new Error(`CSRF bootstrap failed with HTTP ${csrfResponse.status}.`)
  const csrf = await csrfResponse.json()
  const csrfCookie = setCookieValue(csrfResponse, 'XSRF-TOKEN')
  const email = `redis-session-${Date.now()}@example.test`
  const registration = await fetch(`${frontendUrl}/api/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Cookie: csrfCookie,
      'X-XSRF-TOKEN': csrf.token,
    },
    body: JSON.stringify({ email, password: 'redis-session-smoke-password' }),
  })
  if (!registration.ok) {
    throw new Error(`Synthetic session registration failed with HTTP ${registration.status}: ${await registration.text()}`)
  }

  const sessionCookie = setCookieValue(registration, 'SESSION')
  await waitForSession(sessionCookie, email, 'Initial Redis session check')

  let restartCode = await run('docker', ['restart', 'finance-planner-e2e-backend'])
  if (restartCode !== 0) throw new Error('Could not restart the isolated E2E backend container.')
  await waitForStack()
  await waitForSession(sessionCookie, email, 'Backend restart')
  console.log('  PASS: authenticated session survived backend restart.')

  // Redis uses appendfsync everysec; allow the registration/session write to
  // reach the AOF before deliberately restarting the isolated Redis container.
  await new Promise((resolveDelay) => setTimeout(resolveDelay, 1_500))
  restartCode = await run('docker', [...composePrefix, 'up', '-d', '--force-recreate', 'redis'])
  if (restartCode !== 0) throw new Error('Could not recreate the isolated E2E Redis container.')
  await waitForSession(sessionCookie, email, 'Redis AOF restart')
  console.log('  PASS: authenticated session survived Redis container recreation (AOF + named volume).')
}

async function verifyRedisLoginRateLimit() {
  console.log('\nVerifying Redis-backed login throttling...')
  const csrfResponse = await fetch(`${frontendUrl}/api/csrf`)
  if (!csrfResponse.ok) throw new Error(`CSRF bootstrap failed with HTTP ${csrfResponse.status}.`)
  const csrf = await csrfResponse.json()
  const csrfCookie = setCookieValue(csrfResponse, 'XSRF-TOKEN')
  const email = `rate-limit-${Date.now()}@example.test`
  const request = () => fetch(`${frontendUrl}/api/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Cookie: csrfCookie,
      'X-XSRF-TOKEN': csrf.token,
    },
    body: JSON.stringify({ email, password: 'incorrect-password-for-e2e' }),
  })

  for (let attempt = 1; attempt <= 5; attempt += 1) {
    const response = await request()
    if (response.status !== 401) {
      throw new Error(`Failed login attempt ${attempt} returned HTTP ${response.status}; expected 401.`)
    }
  }
  const blocked = await request()
  if (blocked.status !== 429) {
    throw new Error(`Sixth failed login returned HTTP ${blocked.status}; expected Redis rate-limit HTTP 429.`)
  }
  console.log('  PASS: five failed logins are allowed; the sixth is throttled by Redis.')
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

  if (exitCode === 0) await verifyRedisLoginRateLimit()
  if (exitCode === 0) await verifyRedisSessionPersistence()
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
