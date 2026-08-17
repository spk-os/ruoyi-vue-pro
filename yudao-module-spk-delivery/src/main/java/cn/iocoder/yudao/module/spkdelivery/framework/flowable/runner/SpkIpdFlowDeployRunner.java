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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SPK-OS IPD 流程部署 Runner（D1 多流程改造）。
 * <p>
 * 启动时幂等地把 {@code resources/ipd/spk-ipd-flow-*.json} 三份 simpleModel 落为 yudao 流程模型并部署：
 * <ul>
 *   <li>spkIpdFlowFull —— FULL_RELEASE 全量发布（六阶段 + 4 DCP + 6 TR）</li>
 *   <li>spkIpdFlowIncrement —— INCREMENT_RELEASE 增量发布（四阶段轻量 + 2 DCP + 2 TR）</li>
 *   <li>spkIpdFlowIssue —— ISSUE_RESOLUTION 问题处置（四段轻流程）</li>
 * </ul>
 * 三种 flowType 各对应一套真实 BPM 流程（修 G1）。每份流程独立 createModel/updateModel/deployModel，
 * 契约哈希按 key 隔离比对。租户上下文 tenant=1 覆盖整个循环（try-finally 不能只护第一份，否则后两份裸跑）。
 * 仿 {@code TDengineTableInitRunner}：任何异常仅告警，不阻断应用启动。
 *
 * @author SPK-OS
 */
@Slf4j
@Component
public class SpkIpdFlowDeployRunner implements ApplicationRunner {

    /** 部署的流程规格：key / 名称 / 资源路径 */
    private static final List<FlowSpec> FLOWS = Arrays.asList(
            new FlowSpec("spkIpdFlowFull", "SPK-OS IPD 全量发布流程", "/ipd/spk-ipd-flow-full.json"),
            new FlowSpec("spkIpdFlowIncrement", "SPK-OS IPD 增量发布流程", "/ipd/spk-ipd-flow-increment.json"),
            new FlowSpec("spkIpdFlowIssue", "SPK-OS IPD 问题处置流程", "/ipd/spk-ipd-flow-issue.json"));

    private record FlowSpec(String key, String name, String resource) {}

    /** 本机网关地址（HTTP 触发器回调目标），默认 48080 与 yudao server.port 对齐 */
    @org.springframework.beans.factory.annotation.Value("${spk-delivery.self.base-url:127.0.0.1:48080}")
    private String baseUrl;
    /** 部署/管理用的系统用户编号（须 ∈ managerUserIds） */
    @org.springframework.beans.factory.annotation.Value("${spk-delivery.self.system-user-id:1}")
    private Long systemUserId;
    /**
     * 是否启动时自动部署/刷新 IPD 流程模型。本重建默认 true：把 3 套新流程落库（修 G1 核心交付）。
     * 与用户在界面手改模型冲突时，可置 false 跳过。
     */
    @org.springframework.beans.factory.annotation.Value("${spk-delivery.flow.auto-deploy:true}")
    private boolean autoDeploy;

    @Resource
    private BpmModelService modelService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private SpkContractVersionService contractVersionService;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoDeploy) {
            log.info("[run][spk-delivery.flow.auto-deploy=false，跳过 IPD 流程自动部署；改由「流程模型」设计器维护]");
            return;
        }
        // SPK-OS 单租户(tenant=1)：覆盖整个 3 份循环（风险#8：try-finally 不能只护第一份）
        Long prevTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(1L);
        try {
            for (FlowSpec spec : FLOWS) {
                deployOne(spec);
            }
        } finally {
            if (prevTenant == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(prevTenant);
            }
        }
    }

    private void deployOne(FlowSpec spec) {
        try {
            // 1. 读取 simpleModel JSON 并替换占位
            String json = StreamUtils.copyToString(
                    new ClassPathResource(spec.resource()).getInputStream(), StandardCharsets.UTF_8);
            json = json.replace("${spk-base-url}", baseUrl);
            BpmSimpleModelNodeVO simpleModel = JsonUtils.parseObject(json, BpmSimpleModelNodeVO.class);

            // 2. 构造保存 VO
            BpmModelSaveReqVO reqVO = new BpmModelSaveReqVO();
            reqVO.setKey(spec.key());
            reqVO.setName(spec.name());
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
                    .modelKey(spec.key()).modelTenantId(FlowableUtils.getTenantId()).singleResult();
            String modelId;
            if (existing == null) {
                modelId = modelService.createModel(reqVO);
                log.info("[deployOne][创建流程模型 key={} modelId={}]", spec.key(), modelId);
            } else {
                reqVO.setId(existing.getId());
                modelService.updateModel(systemUserId, reqVO);
                modelId = existing.getId();
                log.info("[deployOne][更新流程模型 key={} modelId={}]", spec.key(), modelId);
            }

            // 4. 流程契约：检测相对上一版是否变更
            SpkContractVersionService.ContractDiff contractDiff = contractVersionService.diff(spec.key(), json);
            boolean changed = contractDiff.isChanged();

            // 5. 部署：首次 或 simpleModel 变更 则部署新版本；否则跳过
            List<ProcessDefinition> deployed = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(spec.key()).processDefinitionTenantId(FlowableUtils.getTenantId())
                    .latestVersion().list();
            if (deployed.isEmpty()) {
                modelService.deployModel(systemUserId, modelId);
                log.info("[deployOne][流程首次部署完成 key={} modelId={}]", spec.key(), modelId);
            } else if (changed) {
                modelService.deployModel(systemUserId, modelId);
                log.info("[deployOne][simpleModel 变更重新部署 key={} prevHash={} curHash={}]",
                        spec.key(), contractDiff.getPreviousHash(), contractDiff.getCurrentHash());
            } else {
                log.info("[deployOne][已部署且无变更跳过 key={} deploymentId={}]",
                        spec.key(), deployed.get(0).getDeploymentId());
            }

            // 6. 记录流程契约快照
            try {
                contractVersionService.record(spec.key(), json);
            } catch (Exception e) {
                log.warn("[deployOne][记录流程契约快照失败 key={}，不影响部署]", spec.key(), e);
            }
        } catch (Exception e) {
            // 单份失败不阻断其余流程部署
            log.error("[deployOne][流程自动部署失败 key={}，可经管理后台手工导入]", spec.key(), e);
        }
    }

}
