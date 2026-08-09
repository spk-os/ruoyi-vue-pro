package cn.iocoder.yudao.module.spkdelivery.framework.flowable.runner;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * SPK-OS IPD BPMN 部署集部署器（设计 §6.1「复用 SpkIpdFlowDeployRunner，启动时扫描 processes/ 自动部署」）。
 * <p>
 * 启动时扫描 classpath {@code processes/} 下所有 {@code .bpmn}（含 dcp/tr/ccb 子目录），按流程定义 key + tenant 幂等部署到 Flowable：
 * <ul>
 *   <li>tenant=1 单租户作用域（同 {@link SpkIpdFlowDeployRunner}）。</li>
 *   <li>每文件取 BPMN 内容 SHA-256；与已部署最新版的同名 resource 内容哈希比对，相同则跳过（避免重启产生新版本号）。</li>
 *   <li>内容变更或首次部署 → {@code createDeployment().addInputStream(...).deploy()} 产新版本。</li>
 *   <li>任何异常仅告警，不阻断启动（仿 TDengineTableInitRunner）。</li>
 * </ul>
 *
 * <p><b>开关</b>：{@code spk-delivery.flow.bpmn-auto-deploy} 默认 {@code false}。这些 BPMN 是设计 §6.1
 * 的目标多流程架构（1 主 + 6 阶段 + 4 DCP + 6 TR + 1 CCB），与当前活流程（simpleModel {@code spkIpdFlow}
 * 单流程）并存为可调用流程定义。置 {@code true} 后启动时自动部署，供后续从单流程过渡到多流程模型时使用。
 *
 * @author SPK-OS
 */
@Slf4j
@Component
public class SpkIpdBpmnDeployRunner implements ApplicationRunner {

    private static final String PATTERN = "classpath:processes/**/*.bpmn";

    @org.springframework.beans.factory.annotation.Value("${spk-delivery.flow.bpmn-auto-deploy:false}")
    private boolean autoDeploy;

    @jakarta.annotation.Resource
    private RepositoryService repositoryService;

    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public void run(ApplicationArguments args) {
        if (!autoDeploy) {
            log.info("[run][spk-delivery.flow.bpmn-auto-deploy=false，跳过 BPMN 部署集自动部署（设计 §6.1 目标架构，按需开启）]");
            return;
        }
        Long prevTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(1L);
        int deployed = 0, skipped = 0, failed = 0;
        try {
            Resource[] resources = resolver.getResources(PATTERN);
            log.info("[run][扫描到 BPMN 文件 {} 个]", resources.length);
            for (Resource r : resources) {
                try {
                    String name = r.getFilename();
                    String content = readContent(r);
                    String key = extractProcessKey(content, name);
                    if (key == null) {
                        log.warn("[run][{} 无法解析 process id，跳过]", name);
                        failed++;
                        continue;
                    }
                    if (isSameAsDeployed(key, name, content)) {
                        skipped++;
                        log.info("[run][{} key={} 已部署且内容未变，跳过]", name, key);
                        continue;
                    }
                    Deployment dep = repositoryService.createDeployment()
                            .name("SPK-IPD:" + name)
                            .key(key)
                            .addInputStream(name, new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
                            .tenantId(TenantContextHolder.getTenantId().toString())
                            .deploy();
                    deployed++;
                    log.info("[run][{} key={} 部署完成 deploymentId={}]", name, key, dep.getId());
                } catch (Exception e) {
                    failed++;
                    log.error("[run][BPMN 部署失败 resource={}]", r.getDescription(), e);
                }
            }
            log.info("[run][BPMN 部署集完成 deployed={} skipped={} failed={}]", deployed, skipped, failed);
        } catch (Exception e) {
            log.error("[run][BPMN 部署集扫描/部署异常，不阻断启动]", e);
        } finally {
            if (prevTenant == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(prevTenant);
            }
        }
    }

    /** 读 BPMN 文件全文（UTF-8）。 */
    private String readContent(Resource r) throws Exception {
        try (InputStream in = r.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    /**
     * 从 BPMN XML 提取 {@code <process id="...">} 的 id（作为流程定义 key）。
     * 用字符串匹配避免完整 DOM 解析依赖；失败回退文件名去扩展名。
     */
    private String extractProcessKey(String content, String fallbackName) {
        int idx = content.indexOf("<process ");
        if (idx < 0) {
            idx = content.indexOf("<process>");
        }
        if (idx < 0) {
            return fallbackName.replace(".bpmn", "");
        }
        int idIdx = content.indexOf("id=\"", idx);
        if (idIdx < 0) {
            return fallbackName.replace(".bpmn", "");
        }
        int start = idIdx + 4;
        int end = content.indexOf('"', start);
        return end > start ? content.substring(start, end) : fallbackName.replace(".bpmn", "");
    }

    /**
     * 比对最新已部署版本的 resource 内容哈希是否与当前一致。
     * 一致 → 跳过（避免重启刷版本号）；不一致或不存在 → 部署新版本。
     */
    private boolean isSameAsDeployed(String key, String resourceName, String content) {
        try {
            List<ProcessDefinition> latest = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(key)
                    .processDefinitionTenantId(TenantContextHolder.getTenantId().toString())
                    .latestVersion().list();
            if (latest.isEmpty()) {
                return false;
            }
            ProcessDefinition pd = latest.get(0);
            // 取该 deployment 的同名 resource 字节做哈希比对
            List<String> resNames = repositoryService.getDeploymentResourceNames(pd.getDeploymentId());
            if (resNames == null || !resNames.contains(resourceName)) {
                return false;
            }
            try (InputStream in = repositoryService.getResourceAsStream(pd.getDeploymentId(), resourceName)) {
                String deployed = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
                return sha256(deployed).equals(sha256(content));
            }
        } catch (Exception e) {
            log.warn("[isSameAsDeployed][key={} 比对异常，按需部署：{}]", key, e.getMessage());
            return false;
        }
    }

    private static String sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] h = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : h) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
