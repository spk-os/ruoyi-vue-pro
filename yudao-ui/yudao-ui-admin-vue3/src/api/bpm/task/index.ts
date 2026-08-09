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

// SPK-OS 扩展：approve/reject 单请求超时上调到 600s（10min 上限）。
// 根因：IPD 流程审批（含 n_start 发起人节点 / CDCP 之后 plan 阶段）会同步跑后续 service tasks
// （HTTP 触发器→LLM，单 stage 5 个任务 ~2-3.5min）。yudao 默认 axios 超时 30s（config.ts:19）
// 远小于此 → 前端 30s 报"接口请求超时" → 用户以为失败重复点击 → 同一 task 上并发 approve/reject
// tx 末尾 setVariableLocal(TASK_STATUS) 撞 Flowable 乐观锁 → 回滚 → 任务停开。
// 调高超时让单次审批在 ~3min 内自然完成，杜绝重复点击并发，从根上消除乐观锁回滚。
// 此为纯前端配置扩展：不改 BPMN 流程、不改 yudao 审批逻辑；快速审批仍即时返回（超时只是上限）。
const APPROVE_REJECT_TIMEOUT = 600000

export const approveTask = async (data: any) => {
  return await request.put({ url: '/bpm/task/approve', data, timeout: APPROVE_REJECT_TIMEOUT })
}

export const rejectTask = async (data: any) => {
  return await request.put({ url: '/bpm/task/reject', data, timeout: APPROVE_REJECT_TIMEOUT })
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
