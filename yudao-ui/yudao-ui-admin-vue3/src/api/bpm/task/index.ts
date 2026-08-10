import request from '@/config/axios'

/**
 * 任务状态枚举
 */
export enum TaskStatusEnum {
  /**
   * 跳过
   */
  SKIP = -2,
  /**
   * 未开始
   */
  NOT_START = -1,

  /**
   * 待审批
   */
  WAIT = 0,
  /**
   * 审批中
   */
  RUNNING = 1,
  /**
   * 审批通过
   */
  APPROVE = 2,

  /**
   * 审批不通过
   */
  REJECT = 3,

  /**
   * 已取消
   */
  CANCEL = 4,
  /**
   * 已退回
   */
  RETURN = 5,
  /**
   * 审批通过中
   */
  APPROVING = 7
}

export const getTaskTodoPage = async (params: any) => {
  return await request.get({ url: '/bpm/task/todo-page', params })
}

export const getTaskDonePage = async (params: any) => {
  return await request.get({ url: '/bpm/task/done-page', params })
}

export const getTaskManagerPage = async (params: any) => {
  return await request.get({ url: '/bpm/task/manager-page', params })
}

// 复用 yudao 原生 approve/reject（默认 axios 超时 30s，不单独上调）。
// 历史：曾因 IPD 审批同步跑后续 type1 serviceTask（HTTP 触发器→LLM ~3min）导致 30s 超时→重复点击→
// TASK_STATUS 乐观锁回滚，故临时把单请求超时上调到 600s 兜底。现 spk-ipd-flow.json 全部触发器已改
// type=2（HTTP_CALLBACK）：serviceTask 发请求即返回（/run 调 dispatchActivityAsync 立即返 dispatched），
// 流程卡 receiveTask 等 LLM 后台回调推进——complete() 不再阻塞等 LLM，审批毫秒级返回（实测 n_start
// userTask complete→serviceTask→receiveTask pause 共 0.1s）。根因消除，恢复原生 30s 默认即可。
// 详见 SPK-OS-Cortext-IPD-Core.md §17 + [[spk-ipd-approve-timeout-optimistic-lock]]。
export const approveTask = async (data: any) => {
  return await request.put({ url: '/bpm/task/approve', data })
}

export const rejectTask = async (data: any) => {
  return await request.put({ url: '/bpm/task/reject', data })
}

export const getTaskListByProcessInstanceId = async (processInstanceId: string) => {
  return await request.get({
    url: '/bpm/task/list-by-process-instance-id?processInstanceId=' + processInstanceId
  })
}

// 获取所有可退回的节点
export const getTaskListByReturn = async (id: string) => {
  return await request.get({ url: '/bpm/task/list-by-return', params: { id } })
}

// 退回
export const returnTask = async (data: any) => {
  return await request.put({ url: '/bpm/task/return', data })
}

// 委派
export const delegateTask = async (data: any) => {
  return await request.put({ url: '/bpm/task/delegate', data })
}

// 转派
export const transferTask = async (data: any) => {
  return await request.put({ url: '/bpm/task/transfer', data })
}

// 加签
export const signCreateTask = async (data: any) => {
  return await request.put({ url: '/bpm/task/create-sign', data })
}

// 减签
export const signDeleteTask = async (data: any) => {
  return await request.delete({ url: '/bpm/task/delete-sign', data })
}

// 抄送
export const copyTask = async (data: any) => {
  return await request.put({ url: '/bpm/task/copy', data })
}

// 撤回
export const withdrawTask = async (taskId: string) => {
  return await request.put({ url: '/bpm/task/withdraw', params: { taskId } })
}

// 获取我的待办任务
export const myTodoTask = async (processInstanceId: string) => {
  return await request.get({ url: '/bpm/task/my-todo?processInstanceId=' + processInstanceId })
}

// 获取减签任务列表
export const getChildrenTaskList = async (id: string) => {
  return await request.get({ url: '/bpm/task/list-by-parent-task-id?parentTaskId=' + id })
}
