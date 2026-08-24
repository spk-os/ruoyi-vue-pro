package cn.iocoder.yudao.module.spkdelivery.service.router;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpkActivityRetryPromptBuilderTest {

    @Test
    void appendsFailureOnlyForSameActivity() {
        String prompt = SpkActivityRetryPromptBuilder.append("base", "ACT-1", "ACT-1",
                "schema FAIL", 1, 3);
        assertTrue(prompt.contains("第 2/3 次执行"));
        assertTrue(prompt.contains("schema FAIL"));
        assertTrue(prompt.contains("新的 Omnigent→Claude Code"));
    }

    @Test
    void doesNotLeakFailureIntoAnotherActivity() {
        assertEquals("base", SpkActivityRetryPromptBuilder.append(
                "base", "ACT-2", "ACT-1", "schema FAIL", 1, 3));
    }

    @Test
    void capsVerifierFailureSoRetriesDoNotGrowPromptWithoutBound() {
        String prompt = SpkActivityRetryPromptBuilder.append("base", "ACT-1", "ACT-1",
                "x".repeat(20_000), 2, 3);

        assertTrue(prompt.length() < 6_000,
                "重试只应携带本轮可执行问题摘要，不能把全量旧回执不断叠加进新会话");
        assertTrue(prompt.contains("已截断"));
    }
}
