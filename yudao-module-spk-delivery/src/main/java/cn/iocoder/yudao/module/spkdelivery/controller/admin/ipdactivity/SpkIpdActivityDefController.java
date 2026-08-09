package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdactivity;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdactivity.vo.SpkIpdActivityDefPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity.SpkIpdActivityDefMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * IPD Activity 定义 Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD Activity 定义")
@RestController
@RequestMapping("/spk/ipd-activity-def")
@Validated
@Slf4j
public class SpkIpdActivityDefController {

    @Resource
    private SpkIpdActivityDefMapper activityDefMapper;

    /** JDBC 数据源（dynamic-datasource 注入；getConnection 默认走 primary=master） */
    @Resource
    private DataSource dataSource;

    /** Spring 资源加载器，用于按 classpath:/file: 前缀解析 init.sql 路径 */
    @Resource
    private ResourceLoader resourceLoader;

    /**
     * Activity 定义初始化脚本路径。
     * <p>
     * 默认 {@code classpath:sql/spk_ipd_activity_def_init.sql}（打包进 jar，改后须 mvn install）；
     * dev profile 覆盖为 {@code file:} 指向仓库源文件 → 改完 init.sql 直接调 {@code /reload} 即刷 DB，
     * 无需 install、无需重启后端。脚本本身是 DELETE+INSERT 幂等，重跑即刷新 10 个 Activity。
     */
    @Value("${spk-delivery.flow.def-init-sql-path:classpath:sql/spk_ipd_activity_def_init.sql}")
    private String defInitSqlPath;

    @GetMapping("/page")
    @Operation(summary = "分页查询 IPD Activity 定义")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-activity-def:query')")
    public CommonResult<PageResult<SpkIpdActivityDefDO>> page(SpkIpdActivityDefPageReqVO reqVO) {
        return success(activityDefMapper.selectPage(reqVO));
    }

    @GetMapping("/get-by-activity-id")
    @Operation(summary = "按 activityId+version 查询 Activity 定义")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-activity-def:query')")
    public CommonResult<SpkIpdActivityDefDO> getByActivityId(
            @Parameter(description = "Activity 业务标识") @RequestParam("activityId") String activityId,
            @Parameter(description = "版本") @RequestParam(value = "version", required = false) String version) {
        return success(activityDefMapper.selectByActivityIdAndVersion(activityId, version != null ? version : "1.0.0"));
    }

    @GetMapping("/list-by-stage")
    @Operation(summary = "按阶段查询 Activity 定义")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-activity-def:query')")
    public CommonResult<List<SpkIpdActivityDefDO>> listByStage(
            @Parameter(description = "阶段") @RequestParam("stage") String stage) {
        return success(activityDefMapper.selectListByStage(stage));
    }

    @PostMapping("/reload")
    @Operation(summary = "重载 IPD Activity 定义（执行 init.sql 幂等刷新 prompt/配置；无需重启后端）",
            description = "改完 init.sql 直接调本端点即刷 DB。脚本为 DELETE+INSERT 幂等，重跑即刷新 10 个 Activity。"
                    + "dev profile 路径指向仓库源文件，无需 mvn install。")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-activity-def:query')")
    public CommonResult<Map<String, Object>> reload() {
        // 用全限定名避开与 jakarta.annotation.Resource 同名冲突（两者都叫 Resource）
        org.springframework.core.io.Resource sql = resourceLoader.getResource(defInitSqlPath);
        if (!sql.exists()) {
            throw new RuntimeException("Activity 定义 init.sql 不存在：" + defInitSqlPath);
        }
        Long before = activityDefMapper.selectCount();
        // ScriptUtils 按 ';' 拆分逐条执行原生 JDBC，绕过 MyBatis tenant 拦截器；
        // init.sql 显式写 tenant_id=1（见 [[spk-ipd-activity-def-tenant-filter]]），与拦截器预期一致。
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, sql);
        } catch (Exception e) {
            log.error("[reload][执行 init.sql 失败 path={}]", defInitSqlPath, e);
            throw new RuntimeException("执行 init.sql 失败：" + e.getMessage(), e);
        }
        Long after = activityDefMapper.selectCount();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("before", before);
        result.put("after", after);
        result.put("path", defInitSqlPath);
        log.info("[reload][Activity 定义已重载 before={} after={} path={}]", before, after, defInitSqlPath);
        return success(result);
    }

}
