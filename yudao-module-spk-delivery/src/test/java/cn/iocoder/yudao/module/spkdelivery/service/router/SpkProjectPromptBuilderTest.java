package cn.iocoder.yudao.module.spkdelivery.service.router;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpkProjectPromptBuilderTest {

    @Test
    void appendsFrozenWorkspaceAndBusinessContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("projectId", 42L);
        context.put("projectCode", "spk-infomation");
        context.put("projectRoot", "/work/SPK-OS/dev/spk-infomation");
        context.put("projectObjective", "完成真实 IPD 验证");
        String prompt = SpkProjectPromptBuilder.append("执行活动", context);
        assertTrue(prompt.contains("/work/SPK-OS/dev/spk-infomation"));
        assertTrue(prompt.contains("spk-infomation"));
        assertTrue(prompt.contains("完成真实 IPD 验证"));
    }
}
