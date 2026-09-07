import request from '@/utils/request'

export function getWorkDetailApi(taskId) {
  return request({
    // 后端路径：/workDetail/{taskId}，路径占位符拼接
    url: `/render/workDetail/${taskId}`,
    method: 'GET'
    // ❌ 删掉 params: {taskId}，不要问号参数
  })
}

// 记录用户下载行为（下载PPTX/视频时通知后端落库）
export function recordDownloadApi(taskId) {
  return request({
    url: '/mydownload/record',
    method: 'POST',
    data: { taskId }
  })
}