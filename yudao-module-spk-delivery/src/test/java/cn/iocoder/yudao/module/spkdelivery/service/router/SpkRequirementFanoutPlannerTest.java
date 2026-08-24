package cn.iocoder.yudao.module.spkdelivery.service.router;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpkRequirementFanoutPlannerTest {

    @Test
    void parsesFrozenRequirementsInOrder() {
        String scope = "版本范围\nREQ-01 第一项\nREQ-02 第二项\nREQ-05 第五项";

        List<SpkRequirementFanoutPlanner.RequirementSlice> slices =
                SpkRequirementFanoutPlanner.parseFrozenScope(scope);

        assertEquals(List.of("REQ-01", "REQ-02", "REQ-05"),
                slices.stream().map(SpkRequirementFanoutPlanner.RequirementSlice::id).toList());
        assertEquals("第二项", slices.get(1).statement());
    }

    @Test
    void rejectsMissingOrDuplicateRequirements() {
        assertThrows(IllegalStateException.class,
                () -> SpkRequirementFanoutPlanner.parseFrozenScope("只有自然语言范围"));
        assertThrows(IllegalStateException.class,
                () -> SpkRequirementFanoutPlanner.parseFrozenScope("REQ-01 A\nREQ-01 B"));
    }

    @Test
    void emitsSeparatedRequirementAndIntegrationContracts() {
        var slice = new SpkRequirementFanoutPlanner.RequirementSlice("REQ-03", "API 收敛");
        String requirement = SpkRequirementFanoutPlanner.requirementPrompt(slice, 3, 5, 2);
        String integration = SpkRequirementFanoutPlanner.integrationPrompt(List.of(slice), List.of("art-1"));

        assertTrue(requirement.contains("REQUIREMENT"));
        assertTrue(requirement.contains("requirementId: REQ-03"));
        assertTrue(requirement.contains("fanoutApprovalCycle: 2"));
        assertTrue(SpkRequirementFanoutPlanner.matchesRequirementContract(requirement, slice, 2));
        assertTrue(!SpkRequirementFanoutPlanner.matchesRequirementContract(requirement, slice, 1));
        assertTrue(integration.contains("INTEGRATION"));
        assertTrue(integration.contains("art-1"));
    }
}
