package cn.iocoder.yudao.server.spk.e2e;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IPD 节点 skill 绑定注入端到端铁证测试（真实 LLM 模式）。
 * <p>
 * e2e-test profile 已切 {@code fast-mode=false}（真实调 LLM：new-api 网关 ali_glm-5.2）+
 * {@code adapter-override=native-ai}（强制含 mode=omnigent 的 concept 主 Lead lead-req-insight 也走 NativeAiAdapter，
 * 不依赖 omnigent host/workspace）+ {@code skill.default-env=test}（test skill 简化指令让 LLM 快速生成符合三段格式的产物）。
 *
 * <ul>
 *   <li>测试①（skill 真前置 + 产物非桩铁证）：真实 BPMN flow 跑 concept 阶段，LogCaptor 同时捕获
 *       <ul>
 *         <li>{@code SpkTaskRouterService} 的 {@code [route][skill ... skillPath=.../SKILL.md]}（def→resolveSkill→
 *             resolveSkillEnv→resolveSkillPath 全链解析到真实 test/spk-ipd-concept/SKILL.md）；</li>
 *         <li>{@code SpkSkillInjector} 的 {@code [inject][skill=spk-ipd-concept ... 已前置 SKILL.md 进 prompt]}
 *             （NativeAiAdapter 真实派发路径调 SpkSkillInjector.inject 读 SKILL.md 全文前置进 prompt——
 *             fast-mode 桩不调 inject，此日志只在真实 LLM 模式出现，是 skill 真生效铁证）；</li>
 *         <li>concept 产物正文非桩（metadata 不含 stub/fast-mode/omnigent-fast，长度>50，证 LLM 真出格式产物）。</li>
 *       </ul>
 *   <li>测试②（adapter-override 强制 native-ai 铁证）：concept 主 Lead 在 DB 里 mode=omnigent，
 *       adapter-override 配置后须被强制走 NativeAiAdapter。查 {@code spk_run_receipt.provider} 断言
 *       concept Activity 的 provider=native-ai（而非 omnigent）——直接回答"主 Lead 是否走 omnigent"：
 *       e2e 下被强制 native-ai。</li>
 * </ul>
 *
 * <p>手动跑：mvn test -pl yudao-server -Dtest=SpkIpdSkillInjectionE2eTest -Dsurefire.excludedGroups=
 * <br>前置：dev 48080 实例停止（避免 Flowable 最新流程版本被覆盖回 48080）；new-api 网关 :3500 可用。
 *
 * @author SPK-OS
 */
@Tag("e2e")
class SpkIpdSkillInjectionE2eTest extends SpkIpdE2eBase {

    private static final String ROUTER_LOGGER =
            "cn.iocoder.yudao.module.spkdelivery.service.router.SpkTaskRouterService";
    private static final String INJECTOR_LOGGER =
            "cn.iocoder.yudao.module.spkdelivery.service.skill.SpkSkillInjector";

    /**
     * concept 节点真实 LLM 注入 skill 并产出非桩产物（skill 生效全链铁证）。
     * <p>
     * dispatch 异步（dispatchActivityAsync→supplyAsync，TTL 池保租户），用 Awaitility 轮询 appender；
     * 产物落库在 LLM 响应后（~10-90s），再轮询 spk_artifact_manifest.metadata 非桩。
     */
    @Test
    void concept节点真实LLM注入skill并产出非桩产物() {
        long uid = System.nanoTime();
        Long flowRunId = provisionFlowRun(uid);

        // 挂 ListAppender 同时捕获 router + injector 两个 logger 的 skill 铁证日志
        Logger routerLogger = (Logger) LoggerFactory.getLogger(ROUTER_LOGGER);
        Logger injectorLogger = (Logger) LoggerFactory.getLogger(INJECTOR_LOGGER);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        routerLogger.addAppender(appender);
        injectorLogger.addAppender(appender);
        try {
            // 幂等启动：start 同步返 processInstanceId（type2 后 ~0.06s 到首 receiveTask）；concept serviceTask 异步 dispatch 触发 route→inject→LLM
            Map<String, Object> start = post("/spk/ipd/flow-runs/" + flowRunId + "/start",
                    Map.of(), "e2e-skill-real-" + flowRunId + "-" + uid);
            assertNotNull(start, "start 返回空");
            // pid 须在 start 之后取（start 前 flow_run 为 DRAFT，process_instance_id=null；
            // start 同步提交后 flow_run.process_instance_id 已落库）
            String pid = processInstanceIdOf(flowRunId);
            assertNotNull(pid, "start 后 process_instance_id 仍为空（start 未同步提交）");

            // 等 [route][skill skillPath=非空 SKILL.md] + [inject][skill=spk-ipd-concept 已前置 SKILL.md]
            Awaitility.await()
                    .atMost(Duration.ofSeconds(180))
                    .pollInterval(Duration.ofSeconds(2))
                    .untilAsserted(() -> {
                        boolean routeHit = false;
                        boolean injectHit = false;
                        for (ILoggingEvent ev : appender.list) {
                            String msg = ev.getFormattedMessage();
                            if (msg.contains("[route][skill") && msg.contains("SKILL.md")
                                    && !"null".equals(extractField(msg, "skillPath="))) {
                                routeHit = true;
                            }
                            if (msg.contains("[inject][skill=spk-ipd-concept")
                                    && msg.contains("已前置 SKILL.md")) {
                                injectHit = true;
                            }
                        }
                        assertTrue(routeHit && injectHit,
                                "未同时捕获 route解析链 与 真实 inject 铁证（routeHit=" + routeHit
                                        + " injectHit=" + injectHit + "）；已捕获 "
                                        + appender.list.size() + " 条日志");
                    });

            // 等 concept 产物落库且非桩（真实 LLM 出格式产物，非 fastDispatch 桩合成）
            Awaitility.await()
                    .atMost(Duration.ofSeconds(180))
                    .pollInterval(Duration.ofSeconds(3))
                    .untilAsserted(() -> {
                        String ar = conceptActivityRunId(pid);
                        assertNotNull(ar, "concept task_contract 未落库（dispatch 未触发）");
                        String doc = artifactMetadata(ar);
                        assertNotNull(doc, "concept 产物未落库（LLM 未响应或 route 失败）");
                        String low = doc.toLowerCase();
                        // fast-mode 桩的真实签名是"本产物由 omnigent fast-mode 合成"（桩合成回执）；
                        // 仅盯此签名 + deliverable:stub，不盯裸"stub"子串——test skill 占位示例会让 LLM
                        // 写出"concept/stub"作 locator，属真实 LLM 富内容输出（含用户画像/痛点/需求条目），非桩。
                        assertFalse(low.contains("omnigent fast-mode 合成") || low.contains("本产物由")
                                        || low.contains("\"deliverable\":\"stub\""),
                                "concept 产物仍为桩合成（未走真实 LLM）：" + truncate(doc));
                        assertTrue(doc.length() > 50,
                                "concept 产物正文过短，疑似空 content（模型只出 reasoning）：" + truncate(doc));
                    });
        } finally {
            routerLogger.detachAppender(appender);
            injectorLogger.detachAppender(appender);
        }
    }

    /**
     * adapter-override 强制 concept 主 Lead（DB mode=omnigent）走 native-ai 铁证。
     * <p>
     * concept 阶段 lead=lead-req-insight（spk_agent_def.mode=omnigent），无 override 时会路由到
     * OmnigentAdapter 真实模式（依赖 omnigent host）。e2e-test 设 adapter-override=native-ai 后，
     * 查 concept Activity 的 spk_run_receipt.provider 必须为 native-ai——证明 override 生效，主 Lead
     * 在 e2e 下被强制 native-ai 真实 LLM，不触达 omnigent。
     */
    @Test
    void adapterOverride强制concept主Lead走nativeAi() {
        long uid = System.nanoTime();
        Long flowRunId = provisionFlowRun(uid);

        post("/spk/ipd/flow-runs/" + flowRunId + "/start", Map.of(),
                "e2e-skill-override-" + flowRunId + "-" + uid);
        // pid 须在 start 之后取（start 同步提交后 flow_run.process_instance_id 已落库）
        String pid = processInstanceIdOf(flowRunId);
        assertNotNull(pid, "start 后 process_instance_id 仍为空（start 未同步提交）");

        // 等 concept contract done + run_receipt 落库（provider 由 writeRunReceipt 据 adapter.getName() 写）
        Awaitility.await()
                .atMost(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    String provider = conceptProvider(pid);
                    assertNotNull(provider, "concept run_receipt 未落库（dispatch/route 未完成）");
                    assertEquals("native-ai", provider,
                    "concept 主 Lead（DB mode=omnigent）未被 adapter-override 强制 native-ai，provider=" + provider);
                });
    }

    // ==================== jdbc 兜底直查 helpers ====================

    /** concept 阶段 Activity 的 activityRunId（spk_task_contract.phase=concept，按 id DESC 取最新）。 */
    private String conceptActivityRunId(String pid) {
        try {
            return jdbc.queryForObject(
                    "SELECT activity_run_id FROM spk_task_contract "
                            + "WHERE process_instance_id = ? AND phase = 'concept' AND deleted = 0 "
                            + "ORDER BY id DESC LIMIT 1", String.class, pid);
        } catch (Exception e) {
            return null;
        }
    }

    /** concept 产物正文（spk_artifact_manifest.metadata 存 LLM 报告全文，非 JSON）。 */
    private String artifactMetadata(String activityRunId) {
        try {
            return jdbc.queryForObject(
                    "SELECT metadata FROM spk_artifact_manifest "
                            + "WHERE activity_run_id = ? AND deleted = 0 "
                            + "ORDER BY id DESC LIMIT 1", String.class, activityRunId);
        } catch (Exception e) {
            return null;
        }
    }

    /** concept Activity 的派发 provider（spk_run_receipt.provider = adapter.getName()）。 */
    private String conceptProvider(String pid) {
        String ar = conceptActivityRunId(pid);
        if (ar == null) {
            return null;
        }
        try {
            return jdbc.queryForObject(
                    "SELECT provider FROM spk_run_receipt "
                            + "WHERE activity_run_id = ? AND deleted = 0 "
                            + "ORDER BY id DESC LIMIT 1", String.class, ar);
        } catch (Exception e) {
            return null;
        }
    }

    /** 从 "xxx skillPath=yyy zzz" 形态日志取 field= 后的 token（到空格/末尾）。 */
    private static String extractField(String msg, String field) {
        int i = msg.indexOf(field);
        if (i < 0) {
            return "";
        }
        int start = i + field.length();
        int end = msg.indexOf(' ', start);
        if (end < 0) {
            end = msg.length();
        }
        String v = msg.substring(start, end);
        if (v.endsWith("]")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static String truncate(Object o) {
        if (o == null) return "null";
        String s = String.valueOf(o);
        return s.length() <= 200 ? s : s.substring(0, 200);
    }
}
