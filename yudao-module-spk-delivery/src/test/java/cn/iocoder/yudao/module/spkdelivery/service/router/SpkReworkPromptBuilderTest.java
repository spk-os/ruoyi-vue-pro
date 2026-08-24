package cn.iocoder.yudao.module.spkdelivery.service.router;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpkReworkPromptBuilderTest {

    @Test
    void matchingStage_injectsReviewerReasonAndAutomaticReworkMetadata() {
        String prompt = SpkReworkPromptBuilder.append(
                "原始任务", "plan", "plan", "PDCP", "缺少 REQ-03 的 410 验收用例", 2);

        assertTrue(prompt.contains("自动返工指令"));
        assertTrue(prompt.contains("PDCP"));
        assertTrue(prompt.contains("缺少 REQ-03 的 410 验收用例"));
        assertTrue(prompt.contains("第 2 次"));
        assertTrue(prompt.contains("修订完成后重新提交"));
    }

    @Test
    void differentStage_doesNotLeakOldRejectionIntoLaterStage() {
        assertEquals("原始任务", SpkReworkPromptBuilder.append(
                "原始任务", "develop", "plan", "PDCP", "旧理由", 1));
    }
}
