package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionPackageRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis.SpkAegisReviewDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpkIpdApprovalArtifactSelectionTest {

    @Test
    void selectsOnlyDoneContractsFromCurrentStageAndApprovalCycle() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 8, 22, 12, 0);
        List<SpkTaskContractDO> contracts = List.of(
                contract("run-old-done", "concept", "done", cutoff.minusMinutes(2)),
                contract("run-current-failed", "concept", "failed", cutoff.plusMinutes(1)),
                contract("run-current-req-1", "concept", "done", cutoff.plusMinutes(2)),
                contract("run-current-req-2", "concept", "DONE", cutoff.plusMinutes(3)),
                contract("run-other-stage", "plan", "done", cutoff.plusMinutes(4)));
        List<SpkArtifactManifestDO> artifacts = List.of(
                artifact("art-old-draft", "run-old-done", "draft"),
                artifact("art-failed-draft", "run-current-failed", "draft"),
                artifact("art-current-1", "run-current-req-1", "signed"),
                artifact("art-current-2", "run-current-req-2", "signed"),
                artifact("art-other", "run-other-stage", "signed"));
        SpkIpdDecisionRecordDO previousDecision = SpkIpdDecisionRecordDO.builder().decision("REJECT").build();
        previousDecision.setCreateTime(cutoff);

        List<SpkArtifactManifestDO> selected = SpkIpdApprovalServiceImpl.selectCurrentStageArtifacts(
                artifacts, contracts, List.of(previousDecision), "concept");

        assertEquals(List.of("art-current-1", "art-current-2"),
                selected.stream().map(SpkArtifactManifestDO::getArtifactId).toList());
    }

    @Test
    void missingCurrentStageArtifactFailsClosed() {
        List<SpkIpdDecisionPackageRespVO.RequiredArtifact> required =
                SpkIpdApprovalServiceImpl.buildRequiredArtifacts(
                        List.of(), List.of(), List.of(), true, "verify", null, null);

        assertEquals(1, required.size());
        assertEquals("current-stage-artifact:verify", required.get(0).getRef());
        assertEquals("MISSING", required.get(0).getStatus());
        assertTrue(required.get(0).getBlocking());
    }

    @Test
    void commercialApprovalBlocksCurrentAegisConditional() {
        SpkArtifactManifestDO artifact = artifact("art-plan", "run-plan", "signed");
        artifact.setCreateTime(LocalDateTime.of(2026, 8, 22, 12, 0));
        SpkAegisReviewDO review = SpkAegisReviewDO.builder()
                .reviewId("review-plan").nodeKey("n_plan_tr3").verdict("conditional").build();
        review.setCreateTime(LocalDateTime.of(2026, 8, 22, 12, 1));

        List<SpkIpdDecisionPackageRespVO.RequiredArtifact> required =
                SpkIpdApprovalServiceImpl.buildRequiredArtifacts(
                        List.of(artifact), List.of(), List.of(), true,
                        "plan", "FULL_RELEASE", review);

        SpkIpdDecisionPackageRespVO.RequiredArtifact aegis = required.stream()
                .filter(r -> "AEGIS".equals(r.getType())).findFirst().orElseThrow();
        assertEquals("CONDITIONAL", aegis.getStatus());
        assertTrue(aegis.getBlocking());
    }

    @Test
    void commercialApprovalAcceptsOnlyMatchingCurrentAegisPass() {
        SpkArtifactManifestDO artifact = artifact("art-plan", "run-plan", "signed");
        artifact.setCreateTime(LocalDateTime.of(2026, 8, 22, 12, 0));
        SpkAegisReviewDO review = SpkAegisReviewDO.builder()
                .reviewId("review-plan").nodeKey("n_plan_tr3").verdict("pass").build();
        review.setCreateTime(LocalDateTime.of(2026, 8, 22, 12, 1));

        List<SpkIpdDecisionPackageRespVO.RequiredArtifact> required =
                SpkIpdApprovalServiceImpl.buildRequiredArtifacts(
                        List.of(artifact), List.of(), List.of(), true,
                        "plan", "FULL_RELEASE", review);

        SpkIpdDecisionPackageRespVO.RequiredArtifact aegis = required.stream()
                .filter(r -> "AEGIS".equals(r.getType())).findFirst().orElseThrow();
        assertEquals("PASS", aegis.getStatus());
        assertTrue(!aegis.getBlocking());
        assertTrue(SpkIpdApprovalServiceImpl.aegisMatchesStage(review, "plan"));
        assertTrue(!SpkIpdApprovalServiceImpl.aegisMatchesStage(review, "concept"));
    }

    @Test
    void approvalTaskStageOverridesStaleFlowRunStage() {
        assertEquals("concept", SpkIpdApprovalServiceImpl.resolveApprovalStage("n_cdcp", "concept"));
        assertEquals("plan", SpkIpdApprovalServiceImpl.resolveApprovalStage("n_pdcp", "concept"));
        assertEquals("develop", SpkIpdApprovalServiceImpl.resolveApprovalStage("n_adcp", "plan"));
        assertEquals("launch", SpkIpdApprovalServiceImpl.resolveApprovalStage("n_ldcp", "verify"));
        assertEquals("verify", SpkIpdApprovalServiceImpl.resolveApprovalStage("n_vdcp", "fix"));
        assertEquals("fallback", SpkIpdApprovalServiceImpl.resolveApprovalStage("unknown", "fallback"));
    }

    private static SpkTaskContractDO contract(String runId, String phase, String status, LocalDateTime queuedAt) {
        return SpkTaskContractDO.builder()
                .activityRunId(runId)
                .phase(phase)
                .status(status)
                .queuedAt(queuedAt)
                .build();
    }

    private static SpkArtifactManifestDO artifact(String artifactId, String runId, String status) {
        SpkArtifactManifestDO artifact = new SpkArtifactManifestDO();
        artifact.setArtifactId(artifactId);
        artifact.setActivityRunId(runId);
        artifact.setStatus(status);
        return artifact;
    }
}
