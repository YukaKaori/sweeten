// 比较两个后端版本的接口契约（Node 20+，无第三方依赖）
//
//   node scripts/smoke/diff-openapi.mjs http://127.0.0.1:8092/v3/api-docs http://127.0.0.1:8091/v3/api-docs
//
// 契约 = 每个 (方法, 路径) 的参数（位置 / 名称 / 必填 / 类型）与请求体字段集合（展开 $ref）。
// 标签、摘要与 schema 名称只是文档，不计入差异。接口文档只在非 prod profile 下开放。
const [oldUrl, newUrl] = process.argv.slice(2)
if (!oldUrl || !newUrl) {
  console.error('用法: node diff-openapi.mjs <旧版 /v3/api-docs 地址> <新版 /v3/api-docs 地址>')
  process.exit(2)
}

async function load(url) {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`${url} -> ${res.status}`)
  return res.json()
}

function resolve(doc, schema) {
  if (!schema) return null
  if (schema.$ref) return resolve(doc, doc.components.schemas[schema.$ref.split('/').pop()])
  if (schema.type === 'array') return { array: resolve(doc, schema.items) }
  if (schema.properties) {
    const out = {}
    for (const [k, v] of Object.entries(schema.properties)) {
      out[k] = v.$ref || v.items ? resolve(doc, v) : v.type || 'any'
    }
    return out
  }
  return schema.type || 'any'
}

function contract(doc) {
  const ops = new Map()
  for (const [path, item] of Object.entries(doc.paths)) {
    for (const [method, op] of Object.entries(item)) {
      const params = (op.parameters || [])
        .map((p) => `${p.in}:${p.name}${p.required ? '*' : ''}:${p.schema?.type || p.schema?.$ref || ''}`)
        .sort()
      const body = op.requestBody?.content
        ? JSON.stringify(resolve(doc, Object.values(op.requestBody.content)[0].schema))
        : null
      ops.set(`${method.toUpperCase()} ${path}`, JSON.stringify({ params, body }))
    }
  }
  return ops
}

const [oldDoc, newDoc] = await Promise.all([load(oldUrl), load(newUrl)])
const a = contract(oldDoc)
const b = contract(newDoc)
let diffs = 0
for (const key of new Set([...a.keys(), ...b.keys()])) {
  if (!a.has(key)) { console.log(`+ 仅新版: ${key}`); diffs++; continue }
  if (!b.has(key)) { console.log(`- 仅旧版: ${key}`); diffs++; continue }
  if (a.get(key) !== b.get(key)) {
    console.log(`~ 变更: ${key}\n    旧 ${a.get(key)}\n    新 ${b.get(key)}`)
    diffs++
  }
}
console.log(`接口数：旧 ${a.size}，新 ${b.size}；契约差异 ${diffs} 处`)
// 用 exitCode 而不是 process.exit()：Windows 上 fetch 的连接尚在关闭时调用 process.exit() 会触发 libuv 断言崩溃
process.exitCode = diffs ? 1 : 0
