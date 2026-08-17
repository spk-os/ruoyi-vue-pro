package cn.iocoder.yudao.module.spkdelivery.service.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Skill 指令注入器（Phase1 最小闭环——把绑定 skill 的 SKILL.md 全文前置进 prompt，让 LLM 真遵循方法论）。
 * <p>
 * 统一供 {@code NativeAiAdapter} 与 {@code OmnigentAdapter} 调用，避免主 Lead（走 omnigent）链路漏注入 skill。
 * <ul>
 *   <li>skillPath 指向的 SKILL.md 存在 → 读全文（截断至 8KB 防 prompt 膨胀）前置；</li>
 *   <li>文件不存在 → 只注入 skill 名 + 路径占位指令；</li>
 *   <li>读取异常 → 降级只注入 skill 名，不阻断派发。</li>
 * </ul>
 *
 * @author SPK-OS
 */
public final class SpkSkillInjector {

    private static final Logger log = LoggerFactory.getLogger(SpkSkillInjector.class);
    private static final int SKILL_MAX_LEN = 8192;

    private SpkSkillInjector() {
    }

    /**
     * 把 skill 绑定指令块前置进 prompt。skillName 空则原样返回 prompt。
     */
    public static String inject(String prompt, String skillName, String skillPath) {
        if (skillName == null || skillName.isBlank()) {
            return prompt == null ? "" : prompt;
        }
        StringBuilder block = new StringBuilder();
        block.append("【Skill 绑定】本任务须按 skill：").append(skillName).append(" 的方法论执行。\n");
        if (skillPath != null && !skillPath.isBlank()) {
            try {
                Path p = Paths.get(skillPath);
                if (Files.exists(p)) {
                    String body = Files.readString(p, StandardCharsets.UTF_8);
                    int origLen = body.length();
                    if (body.length() > SKILL_MAX_LEN) {
                        body = body.substring(0, SKILL_MAX_LEN)
                                + "\n...（SKILL.md 已截断，全文见 " + skillPath + "）";
                    }
                    block.append("--- SKILL.md 全文 ---\n").append(body).append("\n--- SKILL.md 结束 ---\n");
                    // 成功注入铁证（供 dev 真实模式 grep 验证 skill 真前置进 LLM prompt）
                    log.info("[inject][skill={} path={} bodyLen={} truncated={} 已前置 SKILL.md 进 prompt]",
                            skillName, skillPath, origLen, origLen > SKILL_MAX_LEN);
                } else {
                    log.warn("[inject][skill={} path={} SKILL.md 未找到，仅注入 skill 名]", skillName, skillPath);
                    block.append("（SKILL.md 未找到：").append(skillPath).append("，按 skill 名所述方法论执行）\n");
                }
            } catch (Exception e) {
                log.warn("[inject][skill={} 读 SKILL.md 失败降级：{}]", skillName, e.getMessage());
                block.append("（SKILL.md 读取失败，按 skill 名所述方法论执行）\n");
            }
        } else {
            log.info("[inject][skill={} 无 skillPath，仅注入 skill 名方法论]", skillName);
        }
        block.append("【Skill 绑定结束】\n\n");
        return block.toString() + (prompt == null ? "" : prompt);
    }
}
