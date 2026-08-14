package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * IPD 工作台自然语言命令 VO（设计文档 §7.5 / §10.9）。
 * <p>
 * parse 返回只读预览（对象+动作+歧义+影响）；execute 在用户确认后执行白名单命令，必须幂等。
 * 禁止把自然语言直接转为数据库操作。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 工作台命令 VO")
public class SpkIpdCommandVO {

    @Schema(description = "命令解析请求")
    @Data
    public static class ParseReq {
        @NotEmpty(message = "命令文本不能为空")
        private String text;
        private Long projectId;
        private Long versionId;
    }

    @Schema(description = "命令解析预览响应")
    @Data
    public static class ParseResp {
        @Schema(description = "识别意图 ASSIGN/START_AGENT/RETRY/INTERVENE/BLOCK/UNBLOCK/CCB/APPROVE/UNKNOWN")
        private String intent;
        @Schema(description = "解析出的对象（版本/需求/人/Agent）")
        private List<Map<String, Object>> targets;
        @Schema(description = "解析出的动作步骤")
        private List<Map<String, Object>> actions;
        @Schema(description = "歧义点")
        private List<String> ambiguities;
        @Schema(description = "影响范围说明")
        private String impact;
        @Schema(description = "预生成的幂等命令键")
        private String idempotencyKey;
        @Schema(description = "是否可执行（确认后走 execute）")
        private Boolean executable;
    }

    @Schema(description = "命令执行请求")
    @Data
    public static class ExecuteReq {
        @NotEmpty(message = "幂等键不能为空")
        private String idempotencyKey;
        @NotEmpty(message = "意图不能为空")
        private String intent;
        private Map<String, Object> params;
    }

    @Schema(description = "命令执行响应")
    @Data
    public static class ExecuteResp {
        private String intent;
        private String status; // SUCCESS/REJECTED/IDEMPOTENT/NOT_SUPPORTED
        private String message;
        private Map<String, Object> result;
    }
}
