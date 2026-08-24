package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import java.util.Map;
import java.util.Optional;

/**
 * IPD 人审驳回后的自动返工路由。
 *
 * <p>审批人只表达 APPROVE/REJECT。REJECT 必须回到本阶段首个自动执行节点，
 * 由 Flowable 再次触发 Omnigent，而不是结束流程或要求审批人手工重启 Agent。</p>
 */
final class SpkIpdReworkPolicy {

    private static final Map<String, Route> ROUTES = Map.of(
            "n_cdcp", new Route("CDCP", "n_concept_t1", "concept"),
            "n_pdcp", new Route("PDCP", "n_plan_t1", "plan"),
            "n_adcp", new Route("ADCP", "n_dev_t1", "develop"),
            "n_ldcp", new Route("LDCP", "n_launch_t1", "launch"),
            "n_rcdcp", new Route("RCDCP", "n_rc_t1", "requirement-change"),
            "n_fdcp", new Route("FDCP", "n_fix_t1", "fix"),
            "n_vdcp", new Route("VDCP", "n_verify_t1", "verify")
    );

    private SpkIpdReworkPolicy() {
    }

    static Optional<Route> resolve(String approvalTaskDefinitionKey) {
        return Optional.ofNullable(approvalTaskDefinitionKey == null
                ? null : ROUTES.get(approvalTaskDefinitionKey));
    }

    record Route(String gate, String targetNode, String stage) {
    }
}
