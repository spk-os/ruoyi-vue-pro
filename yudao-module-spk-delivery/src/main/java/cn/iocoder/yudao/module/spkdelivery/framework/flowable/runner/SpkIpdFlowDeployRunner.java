package cn.iocoder.yudao.module.spkdelivery.framework.flowable.runner;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.BpmModelSaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.simple.BpmSimpleModelNodeVO;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmModelFormTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmModelTypeEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.spkdelivery.service.contract.SpkContractVersionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * SPK-OS IPD 主流程部署 Runner
 * <p>
 * 启动时幂等地把 {@code resources/ipd/spk-ipd-flow.json} 落为 yudao 流程模型并部署：
 * <ol>
 *   <li>读 JSON + 替换 {@code ${spk-base-url}} 占位（支持端口覆盖）。</li>
 *   <li>模型不存在则 createModel，已存在则 updateModel 刷新 simpleModel。</li>
 *   <li>对比流程契约哈希：相对上一版 simpleModel 是否变更（{@link SpkContractVersionService#diff}）。</li>
 *   <li>部署决策：未部署 或 simpleModel 变更 则 deployModel 产生新版本（Flowable 自动递增版本号，
 *       新实例用最新版，旧实例保持原版本继续运行）；已部署且无变更则跳过。</li>
 *   <li>部署后记录一版 Workflow Contract 契约快照（hash/diff/版本），作为下次启动的变更比对基准。</li>
 * </ol>
 * 仿 {@code TDengineTableInitRunner}：任何异常仅告警，不阻断应用启动。
 *
 * @author SPK-OS
 */
@Slf4j
@Component
public class SpkIpdFlowDeployRunner implements ApplicationRunner {

    private static final String MODEL_KEY = "spkIpdFlow";
    private static final String MODEL_NAME = "SPK-OS IPD 全流程";
    private static final String FLOW_RESOURCE = "/ipd/spk-ipd-flow.json";

    /** 本机网关地址（HTTP 触发器回调目标），默认 48080 与 yudao server.port 对齐 */
    @org.springframework.beans.factory.annotation.Value("${spk-delivery.self.base-url:127.0.0.1:48080}")
    private String baseUrl;
    /** 部署/管理用的系统用户编号（须 ∈ managerUserIds） */
    @org.springframework.beans.factory.annotation.Value("${spk-delivery.self.system-user-id:1}")
    private Long systemUserId;

    @Resource
    private BpmModelService modelService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private SpkContractVersionService contractVersionService;

    @Override
    public void run(ApplicationArguments args) {
        // SPK-OS 单租户(tenant=1)：启动时无租户上下文，显式设为 1，使流程部署到 tenant 1，
        // 实例继承 tenant 1，HTTP 触发器回写 tenant-id:1 头，/admin-api/spk/* 回调端点免 400。
        Long prevTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(1L);
        try {
            // 1. 读取 simpleModel JSON 并替换占位
            String json = StreamUtils.copyToString(
                    new ClassPathResource(FLOW_RESOURCE).getInputStream(), StandardCharsets.UTF_8);
            json = json.replace("${spk-base-url}", baseUrl);
            BpmSimpleModelNodeVO simpleModel = JsonUtils.parseObject(json, BpmSimpleModelNodeVO.class);

            // 2. 构造保存 VO
            BpmModelSaveReqVO reqVO = new BpmModelSaveReqVO();
            reqVO.setKey(MODEL_KEY);
            reqVO.setName(MODEL_NAME);
            reqVO.setCategory("spk");
            reqVO.setType(BpmModelTypeEnum.SIMPLE.getType());
            reqVO.setFormType(BpmModelFormTypeEnum.CUSTOM.getType());
            reqVO.setFormCustomCreatePath("/spk/ipd/start");
            reqVO.setFormCustomViewPath("/spk/ipd/view");
            reqVO.setVisible(Boolean.TRUE);
            reqVO.setManagerUserIds(Collections.singletonList(systemUserId));
            reqVO.setStartUserIds(Collections.singletonList(systemUserId));
            reqVO.setSimpleModel(simpleModel);

            // 3. 幂等：模型已存在则更新，否则创建
            Model existing = repositoryService.createModelQuery()
                    .modelKey(MODEL_KEY).modelTenantId(FlowableUtils.getTenantId()).singleResult();
            String modelId;
            if (existing == null) {
                modelId = modelService.createModel(reqVO);
                log.info("[run][创建 IPD 流程模型 modelId={}]", modelId);
            } else {
                reqVO.setId(existing.getId());
                modelService.updateModel(systemUserId, reqVO);
                modelId = existing.getId();
                log.info("[run][更新 IPD 流程模型 modelId={}]", modelId);
            }

            // 4. 流程契约：检测相对上一版是否变更（决定是否需要重新部署）
            SpkContractVersionService.ContractDiff contractDiff = contractVersionService.diff(MODEL_KEY, json);
            boolean changed = contractDiff.isChanged();

            // 5. 部署：首次部署 或 simpleModel 变更 则部署新版本；否则跳过，避免每次启动产生新版本号
            //    （按当前租户作用域，避免命中其他租户的旧部署）
            List<ProcessDefinition> deployed = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(MODEL_KEY).processDefinitionTenantId(FlowableUtils.getTenantId())
                    .latestVersion().list();
            if (deployed.isEmpty()) {
                modelService.deployModel(systemUserId, modelId);
                log.info("[run][IPD 流程首次部署完成 modelId={}]", modelId);
            } else if (changed) {
                // simpleModel 变更：部署新版本（Flowable 自动递增版本号，新实例用最新版，旧实例保持原版本）
                modelService.deployModel(systemUserId, modelId);
                log.info("[run][IPD 流程 simpleModel 变更，重新部署新版本 modelId={} prevHash={} curHash={}]",
                        modelId, contractDiff.getPreviousHash(), contractDiff.getCurrentHash());
            } else {
                log.info("[run][IPD 流程已部署且无变更，跳过 deploymentId={} tenant={}]",
                        deployed.get(0).getDeploymentId(), FlowableUtils.getTenantId());
            }

            // 6. 记录流程契约快照（部署后落库，作为下次启动的变更比对基准）
            try {
                contractVersionService.record(MODEL_KEY, json);
            } catch (Exception e) {
                log.warn("[run][记录流程契约快照失败，不影响部署]", e);
            }
        } catch (Exception e) {
            // 仿 TDengineTableInitRunner：部署失败不阻断应用启动
            log.error("[run][IPD 流程自动部署失败，可经管理后台手工导入]", e);
        } finally {
            // 恢复租户上下文（避免污染启动期其他 Runner）
            if (prevTenant == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(prevTenant);
            }
        }
    }

}
