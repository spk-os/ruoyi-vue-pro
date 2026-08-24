package cn.iocoder.yudao.module.spkdelivery.service.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpkPlaneIntegrationServiceTest {

    @Test
    void managedIssueName_isStableAndSanitizedForApprovalRework() {
        assertEquals("[SPK-IPD:project-40-version-38:REQ-01] 数据接入重构",
                SpkPlaneIntegrationService.managedIssueName(
                        "project-40-version-38", "REQ-01", "数据接入重构"));
        assertEquals("[SPK-IPD:project_40:REQ_01] (未命名需求)",
                SpkPlaneIntegrationService.managedIssueName(
                        "project/40", "REQ 01", " "));
    }
}
