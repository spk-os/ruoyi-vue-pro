package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * IPD 版本启动就绪度 Response VO。设计文档 §10.3 /ready、§10.5 preflight。
 * checks 逐项可解释：PASS/WARN/BLOCK。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 版本就绪度 Response VO")
@Data
public class SpkIpdReadinessRespVO {

    @Schema(description = "是否就绪")
    private Boolean ready;

    @Schema(description = "解析得到的流程档案版本")
    private Integer resolvedProfileVersion;

    @Schema(description = "就绪检查项")
    private List<Check> checks;

    @Schema(description = "生效阶段")
    private List<String> effectiveStages;

    @Data
    public static class Check {
        private String code;
        private String status;
        private String message;
        private String action;
    }
}
