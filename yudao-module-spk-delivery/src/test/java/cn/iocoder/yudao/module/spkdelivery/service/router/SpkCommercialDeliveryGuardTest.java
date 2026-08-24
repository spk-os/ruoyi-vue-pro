package cn.iocoder.yudao.module.spkdelivery.service.router;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpkCommercialDeliveryGuardTest {

    @Test
    void commercialRelease_requiresResolvedSkill() {
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireSkill("commercial-release", null, null, "activity"));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireSkill("commercial-release", " ", "/tmp/SKILL.md", "activity"));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireSkill("commercial-release", "skill", " ", "activity"));
        assertDoesNotThrow(() ->
                SpkCommercialDeliveryGuard.requireSkill("commercial-release", "skill", "/tmp/SKILL.md", "activity"));
        assertDoesNotThrow(() ->
                SpkCommercialDeliveryGuard.requireSkill("default", null, null, "activity"));
    }

    @Test
    void independentVerification_onlyPassMayCompleteActivity() {
        assertDoesNotThrow(() ->
                SpkCommercialDeliveryGuard.requireVerificationPassed(true, "verifier", "PASS"));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireVerificationPassed(true, null, null));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireVerificationPassed(true, "verifier", "FAIL"));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireVerificationPassed(true, "verifier", "CONDITIONAL"));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireVerificationPassed(true, "verifier", "ERROR"));
        assertDoesNotThrow(() ->
                SpkCommercialDeliveryGuard.requireVerificationPassed(false, null, null));
    }

    @Test
    void commercialRelease_requiresOmnigentAndAliGlm52() {
        assertDoesNotThrow(() -> SpkCommercialDeliveryGuard.requireOmnigentClaudeCodeRoute(
                "commercial-release", "omnigent", "ali_glm-5.2", "claude",
                "spk-ipd-executor", "lead-coding"));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireOmnigentClaudeCodeRoute(
                        "commercial-release", "native-ai", "ali_glm-5.2", "claude",
                        "spk-ipd-executor", "lead-coding"));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireOmnigentClaudeCodeRoute(
                        "commercial-release", "omnigent", "glm-5.2", "claude",
                        "spk-ipd-executor", "lead-coding"));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireOmnigentClaudeCodeRoute(
                        "commercial-release", "omnigent", "ali_glm-5.2", "native",
                        "spk-ipd-executor", "lead-coding"));
        assertThrows(IllegalStateException.class,
                () -> SpkCommercialDeliveryGuard.requireOmnigentClaudeCodeRoute(
                        "commercial-release", "omnigent", "ali_glm-5.2", "claude",
                        "spk-reporter", "lead-coding"));
        assertDoesNotThrow(() -> SpkCommercialDeliveryGuard.requireOmnigentClaudeCodeRoute(
                "commercial-release", "omnigent", "ali_glm-5.2", "claude",
                "spk-architect", "lead-arch-design"));
        assertDoesNotThrow(() -> SpkCommercialDeliveryGuard.requireOmnigentClaudeCodeRoute(
                "commercial-release", "omnigent", "ali_glm-5.2", "claude",
                "spk-ipd-verifier", "verifier-tr"));
        assertDoesNotThrow(() -> SpkCommercialDeliveryGuard.requireOmnigentClaudeCodeRoute(
                "default", "native-ai", "glm-5.2", "native", null, "lead-coding"));
    }
}
