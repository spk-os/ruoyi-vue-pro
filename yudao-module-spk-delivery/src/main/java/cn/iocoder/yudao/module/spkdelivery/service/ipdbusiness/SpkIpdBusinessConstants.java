package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * IPD 业务骨架常量与状态机枚举。集中管理避免散落魔法字符串。
 * 设计文档 §2.4、§9.3、§11。
 *
 * @author SPK-OS
 */
public final class SpkIpdBusinessConstants {

    private SpkIpdBusinessConstants() {}

    // ---------- 流程类型 ----------
    public static final String FLOW_FULL_RELEASE = "FULL_RELEASE";
    public static final String FLOW_INCREMENT_RELEASE = "INCREMENT_RELEASE";
    public static final String FLOW_ISSUE_RESOLUTION = "ISSUE_RESOLUTION";

    // ---------- 版本类型 ----------
    public static final String VERSION_BASELINE = "BASELINE";
    public static final String VERSION_INCREMENT = "INCREMENT";
    public static final String VERSION_HOTFIX = "HOTFIX";

    // ---------- 交付就绪度 ----------
    public static final String READINESS_NOT_READY = "NOT_READY";
    public static final String READINESS_TR5_PASSED = "TR5_PASSED";
    public static final String READINESS_RELEASE_READY = "RELEASE_READY";

    // ---------- 健康度 ----------
    public static final String HEALTH_UNKNOWN = "UNKNOWN";
    public static final String HEALTH_GOOD = "GOOD";
    public static final String HEALTH_WARN = "WARN";
    public static final String HEALTH_CRITICAL = "CRITICAL";

    // ---------- Flowable 流程 key（治理层 flowType → BPM 流程定义映射，D1） ----------
    /** 旧主流程 key（历史实例兼容查询；新启动不再用它） */
    public static final String IPD_FLOW_KEY = "spkIpdFlow";
    /** FULL_RELEASE → 完整 IPD 六阶段重型流程（概念/计划/开发/验证/发布/生命周期 + 4DCP + 6TR + R1-R8 回退） */
    public static final String IPD_FLOW_KEY_FULL = "spkIpdFlowFull";
    /** INCREMENT_RELEASE → 增量发布裁剪轻量流程（跳概念、TR 减为关键 2 个、DCP 合并） */
    public static final String IPD_FLOW_KEY_INCREMENT = "spkIpdFlowIncrement";
    /** ISSUE_RESOLUTION → 问题解决四段轻流程（ROOT_CAUSE/FIX_DEVELOP/VERIFY/CLOSE） */
    public static final String IPD_FLOW_KEY_ISSUE = "spkIpdFlowIssue";

    /**
     * 按 flowType 解析对应的 BPM 流程定义 key。
     * <p>
     * 治理层入口（FlowRunService.start / ProjectService / IntellectService）统一经此方法取 key，
     * 保证「不同 flowType 走不同真实 BPM 流程」（修 G1/G8）。未知 flowType 回退旧 key 兼容历史。
     */
    public static String flowKeyOf(String flowType) {
        if (flowType == null) {
            return IPD_FLOW_KEY;
        }
        return switch (flowType) {
            case FLOW_FULL_RELEASE -> IPD_FLOW_KEY_FULL;
            case FLOW_INCREMENT_RELEASE -> IPD_FLOW_KEY_INCREMENT;
            case FLOW_ISSUE_RESOLUTION -> IPD_FLOW_KEY_ISSUE;
            default -> IPD_FLOW_KEY;
        };
    }

    // ---------- 流程变量受控键（启动时写入，route/触发器只读，遵守互锁铁律） ----------
    public static final String VAR_FLOW_RUN_ID = "flowRunId";
    public static final String VAR_PROJECT_ID = "projectId";
    public static final String VAR_VERSION_ID = "versionId";
    public static final String VAR_ISSUE_CASE_ID = "issueCaseId";
    public static final String VAR_FLOW_TYPE = "flowType";
    public static final String VAR_PROFILE_VERSION = "profileVersion";
    public static final String VAR_TRACE_ID = "traceId";
    public static final String VAR_BUSINESS_KEY = "businessKey";
    public static final String VAR_PROJECT_NAME = "projectName";
    public static final String VAR_PROJECT_CODE = "projectCode";
    public static final String VAR_PROJECT_DESCRIPTION = "projectDescription";
    public static final String VAR_PROJECT_OBJECTIVE = "projectObjective";
    public static final String VAR_PROJECT_ROOT = "projectRoot";
    public static final String VAR_VERSION_NO = "versionNo";
    public static final String VAR_VERSION_OBJECTIVE = "versionObjective";
    public static final String VAR_VERSION_SCOPE = "versionScope";

    // ---------- 命令类型 ----------
    public static final String CMD_START_FLOW = "START_FLOW";
    public static final String CMD_CANCEL_FLOW = "CANCEL_FLOW";
    public static final String CMD_RETRY_FLOW = "RETRY_FLOW";
    public static final String CMD_BLOCK_FLOW = "BLOCK_FLOW";
    public static final String CMD_UNBLOCK_FLOW = "UNBLOCK_FLOW";

    // ---------- FlowRun 状态机（§6.4 / §11.4） ----------
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_STARTING = "STARTING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_WAITING_APPROVAL = "WAITING_APPROVAL";
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_SUPERSEDED = "SUPERSEDED";

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");

    /** 生成项目展示编号 PRJ-2026-000042 */
    public static String projectNo(Long id) {
        return "PRJ-" + LocalDate.now().format(YEAR_FMT) + "-" + pad(id, 6);
    }

    /** 生成 FlowRun 展示编号 FR-20260813-000042 */
    public static String runNo(Long id) {
        return "FR-" + LocalDate.now().format(DAY_FMT) + "-" + pad(id, 6);
    }

    /** 生成问题编号 CASE-2026-000042 */
    public static String caseNo(Long id) {
        return "CASE-" + LocalDate.now().format(YEAR_FMT) + "-" + pad(id, 6);
    }

    /** 业务 key：IPD:{runNo}，如 IPD:FR-20260813-000042 */
    public static String businessKey(String runNo) {
        return "IPD:" + runNo;
    }

    /** 版本展示值 V{major}.{minor} */
    public static String versionNo(int majorNo, int minorNo) {
        return "V" + majorNo + "." + minorNo;
    }

    /** 大版本展示值 V{major} */
    public static String versionLabel(int majorNo) {
        return "V" + majorNo;
    }

    private static String pad(Long id, int width) {
        String s = id == null ? "0" : String.valueOf(id);
        while (s.length() < width) {
            s = "0" + s;
        }
        return s;
    }
}
