package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpkIpdReworkPolicyTest {

    @Test
    void everyCommercialApprovalGate_hasDeterministicAutomaticReworkTarget() {
        Map<String, SpkIpdReworkPolicy.Route> expected = Map.of(
                "n_cdcp", new SpkIpdReworkPolicy.Route("CDCP", "n_concept_t1", "concept"),
                "n_pdcp", new SpkIpdReworkPolicy.Route("PDCP", "n_plan_t1", "plan"),
                "n_adcp", new SpkIpdReworkPolicy.Route("ADCP", "n_dev_t1", "develop"),
                "n_ldcp", new SpkIpdReworkPolicy.Route("LDCP", "n_launch_t1", "launch"),
                "n_rcdcp", new SpkIpdReworkPolicy.Route("RCDCP", "n_rc_t1", "requirement-change"),
                "n_fdcp", new SpkIpdReworkPolicy.Route("FDCP", "n_fix_t1", "fix"),
                "n_vdcp", new SpkIpdReworkPolicy.Route("VDCP", "n_verify_t1", "verify")
        );

        expected.forEach((taskKey, route) ->
                assertEquals(route, SpkIpdReworkPolicy.resolve(taskKey).orElseThrow(), taskKey));
    }

    @Test
    void unknownApprovalGate_failsClosed() {
        assertTrue(SpkIpdReworkPolicy.resolve("unknown_gate").isEmpty());
        assertTrue(SpkIpdReworkPolicy.resolve(null).isEmpty());
    }
}
