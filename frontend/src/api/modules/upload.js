import http from '@/utils/http'

/** 后端 /upload 接口限制：jpg/png/gif/webp，≤ 5MB（UploadController） */
export const UPLOAD_ACCEPT = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
export const UPLOAD_MAX_MB = 5

/** 上传图片到阿里云 OSS，返回可访问的 URL（result.data） */
export const uploadImageApi = (file) => {
  const form = new FormData()
  form.append('file', file)
  return http.post('/upload', form, { timeout: 60000 })
}
