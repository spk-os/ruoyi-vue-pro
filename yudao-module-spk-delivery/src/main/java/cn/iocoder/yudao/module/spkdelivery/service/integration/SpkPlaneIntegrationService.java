package cn.iocoder.yudao.module.spkdelivery.service.integration;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPK-OS Plane 需求管理集成 —— 把 IPD 概念/计划阶段产出的 IR/SR/AR 录入 Plane。
 * <p>
 * 设计文档 §4.3 ACT-P4：需求清单 → Plane Issue 三级（IR/SR/AR parent 关联）。
 * Plane API 实测契约（2026-08-04）：issues 端点用 project UUID（非 slug）：
 * <pre>
 *   POST /api/v1/workspaces/{ws}/projects/{project_uuid}/issues/
 *   header: x-api-key: plane_api_xxx
 *   body: {name, description, priority, type_id?, parent?}  // parent=父 issue UUID（IR→SR→AR 层级）
 *   resp: 200 {id, sequence_id, name, ...}
 * </pre>
 * workspace=spk-os，project UUID 从配置 spk-delivery.plane.project-id 注入（env/secrets/plane.env）。
 * 铁律：Cortex 只存 Plane issue URI 引用，不存需求正文（设计文档 §3「Plane/Gitea 是数据源」）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkPlaneIntegrationService {

    @Value("${spk-delivery.plane.base-url:http://192.168.56.101}")
    private String baseUrl;
    @Value("${spk-delivery.plane.api-key:${PLANE_API_KEY:}}")
    private String apiKey;
    @Value("${spk-delivery.plane.workspace:spk-os}")
    private String workspace;
    /** Plane project UUID（非 slug；从 PLANE_PROJECT_ID 注入，实测 d0159ae0-... 对应 SPK-OS 项目） */
    @Value("${spk-delivery.plane.project-id:${PLANE_PROJECT_ID:}}")
    private String projectId;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * 录入需求清单：IR → SR → AR 三级 parent 关联。
     *
     * @param reqs 需求清单（level=IR/SR/AR，name，description，parentKey 指向上一级 reqKey）
     * @return 每个 reqKey → Plane issue URI（seq_id 形式），写流程变量用
     */
    public Map<String, String> importRequirements(List<PlaneRequirementReq> reqs) {
        return importRequirements(reqs, "legacy");
    }

    /**
     * 幂等录入需求清单。
     * <p>
     * DCP 驳回会让同一项目版本自动回到计划阶段重做；若每次都 POST，Plane 会出现重复需求。
     * 因此用“项目版本 scope + reqKey”生成稳定标记，已存在则 PATCH 更新，首次才创建。
     *
     * @param reqs 需求清单
     * @param idempotencyScope 稳定的项目版本范围（例如 project-40-version-38）
     */
    public Map<String, String> importRequirements(List<PlaneRequirementReq> reqs, String idempotencyScope) {
        Map<String, String> keyToUri = new LinkedHashMap<>();
        Map<String, String> keyToIssueId = new LinkedHashMap<>();
        if (reqs == null || reqs.isEmpty()) {
            return keyToUri;
        }
        String scope = normalizeMarkerToken(idempotencyScope, "scope");
        Map<String, ExistingIssue> existing = findExistingIssues(reqs, scope);
        for (PlaneRequirementReq req : reqs) {
            try {
                if (req == null || req.getKey() == null || req.getKey().isBlank()) {
                    throw new IllegalArgumentException("Plane requirement key 不能为空");
                }
                String parentId = req.getParentKey() == null ? null : keyToIssueId.get(req.getParentKey());
                String managedName = managedIssueName(scope, req.getKey(), req.getName());
                ExistingIssue current = existing.get(req.getKey());
                String issueId;
                String seq;
                if (current != null) {
                    updateIssue(current.id(), managedName, req.getDescription(), req.getLevel(), parentId);
                    issueId = current.id();
                    seq = current.sequenceId();
                } else {
                    issueId = createIssue(managedName, req.getDescription(), req.getLevel(), parentId);
                    seq = readSequenceId(issueId);
                }
                keyToIssueId.put(req.getKey(), issueId);
                String uri = baseUrl + "/workspaces/" + workspace + "/projects/" + projectId
                        + "/issues/" + issueId;
                keyToUri.put(req.getKey(), seq != null ? (workspace.toUpperCase() + "-" + seq) : uri);
                log.info("[importRequirements][{} {} => issue={} seq={} action={}]", req.getLevel(),
                        req.getKey(), issueId, seq, current == null ? "created" : "updated");
            } catch (Exception e) {
                log.error("[importRequirements][录入失败 key={} name={}：{}]", req.getKey(), req.getName(), e.getMessage());
            }
        }
        return keyToUri;
    }

    /** 单条 issue 创建（带 parent 则挂到父 issue 下，IR→SR→AR 层级）。 */
    public String createIssue(String name, String description, String level, String parentIssueId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name == null ? "(未命名需求)" : name);
        if (description != null) body.put("description", description);
        body.put("priority", "medium");
        if (parentIssueId != null) body.put("parent", parentIssueId);
        String resp = postJson(issuesPath(), JsonUtils.toJsonString(body));
        JsonNode node = JsonUtils.parseTree(resp);
        JsonNode idNode = node.get("id");
        if (idNode == null || idNode.isNull()) {
            throw new RuntimeException("Plane 创建 issue 无 id：" + truncate(resp, 300));
        }
        return idNode.asText();
    }

    /** 已存在的流程托管 Issue 必须原位更新，避免审批驳回重做时重复创建。 */
    private void updateIssue(String issueId, String name, String description,
                             String level, String parentIssueId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name == null ? "(未命名需求)" : name);
        if (description != null) body.put("description", description);
        body.put("priority", "medium");
        if (parentIssueId != null) body.put("parent", parentIssueId);
        patchJson(issuesPath() + issueId + "/", JsonUtils.toJsonString(body));
    }

    private Map<String, ExistingIssue> findExistingIssues(List<PlaneRequirementReq> reqs, String scope) {
        Map<String, ExistingIssue> result = new LinkedHashMap<>();
        try {
            JsonNode root = JsonUtils.parseTree(getJson(issuesPath() + "?limit=100"));
            JsonNode rows = root != null && root.has("results") ? root.get("results") : root;
            if (rows == null || !rows.isArray()) {
                return result;
            }
            for (JsonNode row : rows) {
                String id = text(row, "id");
                String name = text(row, "name");
                if (id == null || name == null) {
                    continue;
                }
                for (PlaneRequirementReq req : reqs) {
                    if (req == null || req.getKey() == null) {
                        continue;
                    }
                    String key = req.getKey();
                    if (name.startsWith(managedIssueMarker(scope, key))) {
                        ExistingIssue previous = result.putIfAbsent(key,
                                new ExistingIssue(id, text(row, "sequence_id")));
                        if (previous != null && !previous.id().equals(id)) {
                            throw new IllegalStateException("Plane 存在重复幂等标记: " + scope + "/" + key);
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("读取 Plane 既有需求失败，禁止非幂等重试", e);
        }
    }

    static String managedIssueName(String scope, String key, String name) {
        String actualName = name == null || name.isBlank() ? "(未命名需求)" : name.trim();
        return managedIssueMarker(normalizeMarkerToken(scope, "scope"), key) + " " + actualName;
    }

    private static String managedIssueMarker(String scope, String key) {
        return "[SPK-IPD:" + normalizeMarkerToken(scope, "scope") + ":"
                + normalizeMarkerToken(key, "req") + "]";
    }

    private static String normalizeMarkerToken(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (normalized.isBlank()) {
            normalized = fallback;
        }
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record ExistingIssue(String id, String sequenceId) {
    }

    /** 列出当前 project 的 issues（Dashboard 需求 Tab 代理用）。 */
    public String listIssues(int limit) {
        try {
            return getJson(issuesPath() + "?limit=" + limit);
        } catch (Exception e) {
            log.warn("[listIssues][失败：{}]", e.getMessage());
            return "[]";
        }
    }

    public boolean healthCheck() {
        try {
            String resp = getJson("/api/v1/workspaces/" + workspace + "/projects/" + projectId + "/issues/?limit=1");
            return resp != null && !resp.contains("Page not found");
        } catch (Exception e) {
            return false;
        }
    }

    private String readSequenceId(String issueId) {
        try {
            String resp = getJson(issuesPath() + issueId + "/");
            JsonNode n = JsonUtils.parseTree(resp);
            JsonNode seq = n.get("sequence_id");
            return seq == null || seq.isNull() ? null : seq.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String issuesPath() {
        return "/api/v1/workspaces/" + workspace + "/projects/" + projectId + "/issues/";
    }

    private String postJson(String path, String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("POST " + path + " -> " + resp.statusCode() + "：" + truncate(resp.body(), 300));
        }
        return resp.body();
    }

    private String patchJson(String path, String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofSeconds(30))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("PATCH " + path + " -> " + resp.statusCode() + "：" + truncate(resp.body(), 300));
        }
        return resp.body();
    }

    private String getJson(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Accept", "application/json").header("x-api-key", apiKey)
                .timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("GET " + path + " -> " + resp.statusCode() + "：" + truncate(resp.body(), 200));
        }
        return resp.body();
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max));
    }

    /** Plane 需求录入请求（三级 IR/SR/AR + parent 关联）。 */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PlaneRequirementReq {
        /** 业务 key（IR-1 / SR-1 / AR-1 …），用于 parentKey 引用 */
        private String key;
        /** IR / SR / AR */
        private String level;
        private String name;
        private String description;
        /** 父需求 key（SR 的 parentKey 指向 IR key；AR 指向 SR key） */
        private String parentKey;
    }
}
