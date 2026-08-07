package cn.iocoder.yudao.module.spkdelivery.controller.admin.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SPK-OS Cortext-IPD 自然语言发起 请求 VO
 * <p>
 * 用户在 hermes（或任意前端）用自然语言提一个产品诉求，后端经 LLM 抽取
 * projectName / payload 后调 {@code SpkIpdProjectService.start} 发起 IPD 主流程。
 * 这是设计文档未明写、但用户跨轮反复要求的「在 hermes 说一句话整个流程就开始运行」入口。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 自然语言发起 Request VO")
@Data
public class SpkIpdIntakeReqVO {

    @Schema(description = "自然语言诉求原文", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "我想做一个智能家居中控，主打老人语音控制和家庭能源管理，三个月内出概念包")
    @NotBlank(message = "诉求原文不能为空")
    private String request;

}
