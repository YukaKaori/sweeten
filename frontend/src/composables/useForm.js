import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 通用表单弹窗逻辑（打开/关闭/校验/提交）
 *
 * @param {Object} options
 * @param {Function} options.defaultForm - 返回默认表单数据的工厂函数
 * @param {Function} options.submitApi - 提交函数，接收表单数据
 * @param {Function} [options.onSuccess] - 提交成功后的回调（通常用于刷新列表）
 *
 * @example
 * const { visible, formRef, form, isEdit, submitLoading, open, submit } = useForm({
 *   defaultForm: () => ({ name: '' }),
 *   submitApi: async (data) => data.id ? updateApi(data) : addApi(data),
 *   onSuccess: () => fetchData(),
 * })
 */
export function useForm(options) {
  const { defaultForm, submitApi, onSuccess } = options

  const visible = ref(false)
  const formRef = ref()
  const form = reactive(defaultForm())
  const isEdit = ref(false)
  const submitLoading = ref(false)

  /**
   * 打开表单弹窗
   * @param {Object} [row] - 编辑时传入已有数据，新增时不传
   */
  const open = (row) => {
    visible.value = true
    if (row) {
      isEdit.value = true
      Object.assign(form, row)
    } else {
      isEdit.value = false
      Object.assign(form, defaultForm())
    }
  }

  /**
   * 提交表单（自动校验 + loading + 消息提示）
   */
  const submit = async () => {
    try {
      await formRef.value?.validate()
    } catch {
      return
    }

    submitLoading.value = true
    try {
      await submitApi({ ...form })
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      visible.value = false
      onSuccess?.()
    } catch {
      // 错误已在 http 拦截器中处理
    } finally {
      submitLoading.value = false
    }
  }

  /**
   * 关闭弹窗
   */
  const close = () => {
    visible.value = false
  }

  return {
    visible,
    formRef,
    form,
    isEdit,
    submitLoading,
    open,
    submit,
    close,
  }
}
