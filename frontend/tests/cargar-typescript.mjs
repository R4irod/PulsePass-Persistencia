// Permite probar los módulos existentes con Node, sin añadir dependencias.
// TypeScript ya es una dependencia de desarrollo del proyecto.
import { registerHooks } from 'node:module'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import ts from 'typescript'

registerHooks({
  resolve(specifier, context, nextResolve) {
    if (specifier.startsWith('.') && context.parentURL) {
      const base = new URL(specifier, context.parentURL)
      for (const extension of ['', '.ts', '.tsx']) {
        const candidato = new URL(base.href + extension)
        if (existsSync(fileURLToPath(candidato))) {
          return nextResolve(candidato.href, context)
        }
      }
    }
    return nextResolve(specifier, context)
  },
  load(url, context, nextLoad) {
    if (/\.tsx?$/.test(url) && !url.includes('/node_modules/')) {
      const source = readFileSync(fileURLToPath(url), 'utf8')
      const resultado = ts.transpileModule(source, {
        fileName: fileURLToPath(url),
        compilerOptions: {
          target: ts.ScriptTarget.ESNext,
          module: ts.ModuleKind.ESNext,
          jsx: ts.JsxEmit.ReactJSX,
        },
      })
      return { format: 'module', source: resultado.outputText, shortCircuit: true }
    }
    return nextLoad(url, context)
  },
})
