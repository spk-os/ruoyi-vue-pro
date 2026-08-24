package cn.iocoder.yudao.module.spkdelivery.service.flowconfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpkFlowConfigServiceTest {

    @Test
    void parseBinaryFlag_acceptsOnlyConfigurationContractValues() {
        assertEquals(0, SpkFlowConfigService.parseBinaryFlag("useWorkerAgent", "0"));
        assertEquals(1, SpkFlowConfigService.parseBinaryFlag("useWorkerAgent", "1"));
        assertNull(SpkFlowConfigService.parseBinaryFlag("useWorkerAgent", " "));
        assertThrows(IllegalArgumentException.class,
                () -> SpkFlowConfigService.parseBinaryFlag("useWorkerAgent", "true"));
        assertThrows(IllegalArgumentException.class,
                () -> SpkFlowConfigService.parseBinaryFlag("useWorkerAgent", "2"));
    }
}
