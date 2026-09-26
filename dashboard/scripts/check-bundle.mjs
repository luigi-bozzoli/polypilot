// Guards the two build modes against leaking into each other: a demo bundle
// must ship the MSW worker and the marker `startDemo()` logs, a prod bundle
// must ship neither. Run as `node scripts/check-bundle.mjs <distDir> <demo|prod>`.
import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'

const MARKER = '__POLYPILOT_DEMO_BUNDLE__'
const WORKER_FILENAME = 'mockServiceWorker.js'

const [, , distDirArg, modeArg] = process.argv

if (!distDirArg || (modeArg !== 'demo' && modeArg !== 'prod')) {
  console.error('Usage: node scripts/check-bundle.mjs <distDir> <demo|prod>')
  process.exit(1)
}

const distDir = path.resolve(distDirArg)

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      files.push(...(await walk(full)))
    } else {
      files.push(full)
    }
  }
  return files
}

const files = await walk(distDir)
const hasWorkerFile = files.some((f) => path.basename(f) === WORKER_FILENAME)

let hasMarker = false
for (const file of files) {
  if (!/\.(js|mjs|html)$/.test(file)) continue
  const content = await readFile(file, 'utf8')
  if (content.includes(MARKER)) {
    hasMarker = true
    break
  }
}

console.log(
  `[check-bundle] ${path.relative(process.cwd(), distDir)}: marker=${hasMarker} worker=${hasWorkerFile} (expecting mode=${modeArg})`,
)

if (modeArg === 'demo') {
  if (!hasMarker || !hasWorkerFile) {
    console.error('[check-bundle] FAIL: demo bundle is missing the demo marker and/or mockServiceWorker.js')
    process.exit(1)
  }
} else {
  if (hasMarker || hasWorkerFile) {
    console.error('[check-bundle] FAIL: prod bundle leaked demo code (marker and/or mockServiceWorker.js present)')
    process.exit(1)
  }
}

console.log('[check-bundle] OK')
