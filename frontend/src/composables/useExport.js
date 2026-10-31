import { ElMessage } from 'element-plus'

/**
 * 导出数据为 CSV 文件
 * @param {Array} data - 数据数组
 * @param {Array} columns - 列定义 [{ key, label }]
 * @param {string} filename - 文件名（不含扩展名）
 */
export function exportCSV(data, columns, filename = 'export') {
  if (!data || data.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  const BOM = '﻿'
  const header = columns.map(col => `"${col.label}"`).join(',')
  const rows = data.map(row =>
    columns.map(col => {
      const val = row[col.key]
      if (val === null || val === undefined) return '""'
      return `"${String(val).replace(/"/g, '""')}"`
    }).join(',')
  )

  const csv = BOM + [header, ...rows].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  downloadBlob(blob, `${filename}.csv`)
  ElMessage.success(`已导出 ${data.length} 条数据`)
}

/**
 * 导出数据为 JSON 文件
 * @param {Array} data - 数据数组
 * @param {string} filename - 文件名（不含扩展名）
 */
export function exportJSON(data, filename = 'export') {
  if (!data || data.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  const json = JSON.stringify(data, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  downloadBlob(blob, `${filename}.json`)
  ElMessage.success(`已导出 ${data.length} 条数据`)
}

/**
 * 通用下载 Blob
 */
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
