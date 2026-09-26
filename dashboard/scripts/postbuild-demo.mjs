// GitHub Pages has no server-side rewrite: an unknown path (a deep link, or a
// hard refresh on a client-side route) gets served 404.html verbatim. Copying
// index.html there lets the SPA boot anyway and take over routing client-side.
import { copyFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const dashboardRoot = path.dirname(path.dirname(fileURLToPath(import.meta.url)))
const distDir = path.join(dashboardRoot, 'dist')
const src = path.join(distDir, 'index.html')
const dest = path.join(distDir, '404.html')

await copyFile(src, dest)
console.log(`[postbuild-demo] copied ${path.relative(dashboardRoot, src)} -> ${path.relative(dashboardRoot, dest)}`)
