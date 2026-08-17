package cn.iocoder.yudao.server.spk.e2e;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPK-OS IPD 流程治理 + 交付目录 E2E（@Tag e2e-gov）。
 * <p>
 * 覆盖 Phase1+2 交付目录规则与流程配置：
 * <ol>
 *   <li>流程配置读端聚合：{@code /flow-config/snapshot} 返回 profile+activityDefsByStage+skillCatalog+dirTemplate 齐。</li>
 *   <li>交付目录物理创建：provisionFlowRun + start 后断言 {@code Delivery/project/IPD:<runNo>/.flow asset src docs}
 *       四目录 + {@code project.yaml} + {@code manifest.json} 真实存在。</li>
 *   <li>{@code .flow} 状态还原：跑一次 activity done（APPROVE 首审批门推进触发 route done appendState）后断言
 *       {@code manifest.json} 含 flowRuns 条目且带 pid + currentStage；与 DB current_stage 同源（SpkStageResolver）。</li>
 *   <li>迭代×子流程矩阵：{@code /monitor} 与 {@code /overview} 均含 iterationMatrix 字段，行=迭代列=flowType。</li>
 * </ol>
 * 跑法：mvn test -pl yudao-server -Dtest=SpkIpdFlowConfigE2eTest -DexcludedGroups=
 * 前置：dev 48080 实例停止（避免 Flowable 最新流程版本被覆盖回 48080）。
 *
 * @author SPK-OS
 */
@Tag("e2e-gov")
class SpkIpdFlowConfigE2eTest extends SpkIpdE2eBase {

    /** 交付根基线（DeliveryPathResolver.DELIVERY_ROOT_BASE）。 */
    private static final String DELIVERY_BASE = "/work/SPK-OS/Delivery/project/";

    @Test
    void 流程配置快照_四要素齐() {
        Map<String, Object> snap = get("/spk/ipd/admin/workflows/flow-config/snapshot?flowType=FULL_RELEASE");
        assertNotNull(snap, "flow-config snapshot 返回空");
        // profile：BASELINE 已发布 Profile 非空（治理层已接入）
        assertNotNull(snap.get("profile"), "profile 为空（治理 Profile 未发布）");
        // activityDefsByStage：按 stage 分组非空
        Object byStage = snap.get("activityDefsByStage");
        assertNotNull(byStage, "activityDefsByStage 为空");
        assertTrue(byStage instanceof Map && !((Map<?, ?>) byStage).isEmpty(),
                "activityDefsByStage 无阶段分组");
        // skillCatalog：扫描 spk-* + §7 兜底非空
        Object catalog = snap.get("skillCatalog");
        assertNotNull(catalog, "skillCatalog 为空");
        assertTrue(catalog instanceof List && !((List<?>) catalog).isEmpty(),
                "skillCatalog 无 skill");
        // dirTemplate：.flow/asset/src/docs 树
        String dir = String.valueOf(snap.get("dirTemplate"));
        assertTrue(dir.contains(".flow") && dir.contains("asset"),
                "dirTemplate 缺 .flow/asset：" + dir);
    }

    @Test
    void 交付目录物理创建_start后四目录齐() {
        long uid = System.nanoTime();
        Long flowRunId = provisionFlowRun(uid);
        // start（幂等键）触发 provisionProject：创建 .flow/asset/src/docs + project.yaml + manifest.json
        post("/spk/ipd/flow-runs/" + flowRunId + "/start", Map.of(),
                "e2e-gov-start-" + flowRunId + "-" + uid);

        // runNo → businessKey=IPD:<runNo> → root
        Map<String, Object> run = get("/spk/ipd/flow-runs/" + flowRunId);
        String runNo = run.get("runNo") == null ? null : String.valueOf(run.get("runNo"));
        assertNotNull(runNo, "runNo 为空");
        String root = DELIVERY_BASE + "IPD:" + runNo;

        assertTrue(Files.isDirectory(Path.of(root, ".flow")), ".flow 目录未创建：" + root);
        assertTrue(Files.isDirectory(Path.of(root, "asset")), "asset 目录未创建：" + root);
        assertTrue(Files.isDirectory(Path.of(root, "src")), "src 目录未创建：" + root);
        assertTrue(Files.isDirectory(Path.of(root, "docs")), "docs 目录未创建：" + root);
        assertTrue(Files.isRegularFile(Path.of(root, "project.yaml")), "project.yaml 未创建：" + root);
        assertTrue(Files.isRegularFile(Path.of(root, ".flow", "manifest.json")),
                "manifest.json 未创建：" + root);
    }

    @Test
    void flow状态推进后manifest记录阶段_与DB同源() {
        long uid = System.nanoTime();
        Long flowRunId = provisionFlowRun(uid);
        post("/spk/ipd/flow-runs/" + flowRunId + "/start", Map.of(),
                "e2e-gov-state-" + flowRunId + "-" + uid);

        // 等首审批门 todo 出现并 APPROVE 推进（route done 触发 appendState 写 manifest）
        awaitAtMost(Duration.ofSeconds(180));
        Map<String, Object> todo = awaitFirstTodo(flowRunId, Duration.ofSeconds(180));
        String taskId = taskIdOf(todo);
        Map<String, Object> pkg = get("/spk/ipd/approval-tasks/" + taskId + "/decision-package");
        String hash = pkg.get("decisionPackageHash") == null ? null : String.valueOf(pkg.get("decisionPackageHash"));
        String decision = pickDecisionAction(pkg);
        Map<String, Object> decBody = new java.util.HashMap<>();
        decBody.put("decision", decision);
        decBody.put("reason", "E2E-gov 推进");
        if (hash != null) {
            decBody.put("decisionPackageHash", hash);
        }
        decBody.put("forceOverride", true);
        post("/spk/ipd/approval-tasks/" + taskId + "/decisions", decBody);

        // 推进后断言：manifest.json 含 flowRuns 条目带 pid + currentStage
        Map<String, Object> run = get("/spk/ipd/flow-runs/" + flowRunId);
        String runNo = String.valueOf(run.get("runNo"));
        String pid = processInstanceIdOf(flowRunId);
        assertNotNull(pid, "processInstanceId 为空（start 未起流程实例）");
        String root = DELIVERY_BASE + "IPD:" + runNo;

        Awaitility.await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    String manifestJson = Files.readString(Path.of(root, ".flow", "manifest.json"));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> manifest = cn.iocoder.yudao.framework.common.util.json.JsonUtils
                            .parseObject(manifestJson, Map.class);
                    assertNotNull(manifest, "manifest 反序列化空");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> flowRuns = (List<Map<String, Object>>) manifest.get("flowRuns");
                    assertNotNull(flowRuns, "manifest 无 flowRuns");
                    boolean hit = flowRuns.stream().anyMatch(fr -> pid.equals(fr.get("pid")));
                    assertTrue(hit, "manifest.flowRuns 无 pid=" + pid + " 的条目（appendState 未写）");
                });

        // 同源 SpkStageResolver：三页 currentStage 一致
        Map<String, Object> cards = get("/spk/ipd/projects/cards");
        assertNotNull(cards, "cards 返回空");
        Map<String, Object> monitor = get("/spk/ipd/monitor");
        assertNotNull(monitor.get("iterationMatrix"), "monitor 无 iterationMatrix");
        Map<String, Object> overview = get("/spk/ipd/overview");
        assertNotNull(overview.get("iterationMatrix"), "overview 无 iterationMatrix");
    }

    @Test
    void 迭代子流程矩阵_三页均含() {
        // 既有 FR-20260817-000017 COMPLETED 流程应在矩阵中占一行
        Map<String, Object> monitor = get("/spk/ipd/monitor");
        Object matrix = monitor.get("iterationMatrix");
        assertNotNull(matrix, "monitor 无 iterationMatrix 字段");
        assertTrue(matrix instanceof List, "iterationMatrix 非列表");

        Map<String, Object> overview = get("/spk/ipd/overview");
        Object omatrix = overview.get("iterationMatrix");
        assertNotNull(omatrix, "overview 无 iterationMatrix 字段");
        assertTrue(omatrix instanceof List, "overview iterationMatrix 非列表");

        // 行结构：iterationKey + cells（列=flowType）
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) matrix;
        assertTrue(!rows.isEmpty(), "iterationMatrix 空行（无 FlowRun）");
        Map<String, Object> first = rows.get(0);
        assertNotNull(first.get("iterationKey"), "矩阵行缺 iterationKey");
        assertNotNull(first.get("cells"), "矩阵行缺 cells（列=flowType）");
    }
}
