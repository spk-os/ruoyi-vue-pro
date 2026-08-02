import request from '@/config/axios'

// SPK IPD 产物只读查询接口（按流程实例聚合），供流程实例详情页「IPD 产物」tab 使用
// 复用 yudao CommonResult 剥壳（request 已自动解包 data）

export const getGateListByInstance = async (processInstanceId: string) => {
  return await request.get({ url: '/spk/gate/list-by-instance', params: { processInstanceId } })
}

export const getAgentTaskListByInstance = async (processInstanceId: string) => {
  return await request.get({
    url: '/spk/agent-task/list-by-instance',
    params: { processInstanceId }
  })
}

export const getAegisByInstance = async (processInstanceId: string) => {
  return await request.get({
    url: '/spk/aegis/get-by-instance',
    params: { processInstanceId }
  })
}

export const getCcbListByInstance = async (processInstanceId: string) => {
  return await request.get({ url: '/spk/ccb/list-by-instance', params: { processInstanceId } })
}

export const getDcpListByInstance = async (processInstanceId: string) => {
  return await request.get({ url: '/spk/dcp/list-by-instance', params: { processInstanceId } })
}

export const getFeedbackListByInstance = async (processInstanceId: string) => {
  return await request.get({
    url: '/spk/feedback/list-by-instance',
    params: { processInstanceId }
  })
}

export const getSunsetByInstance = async (processInstanceId: string) => {
  return await request.get({
    url: '/spk/sunset/get-by-instance',
    params: { processInstanceId }
  })
}
