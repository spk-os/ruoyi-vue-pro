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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SPK-OS Gitea 代码/PR/CI/Release 集成 —— 把 IPD 开发/发布阶段产出落到 Gitea。
 * <p>
 * 设计文档 §4.2-§4.5：开发阶段建分支/PR/CI、发布阶段建 Release。
 * Gitea API 实测契约（2026-08-04，Authorization: token xxx）：
 * <pre>
 *   POST /api/v1/repos/{owner}/{repo}/branches  body {new_branch_name, old_ref_name}  -> 201 {name, commit{id}}
 *   POST /api/v1/repos/{owner}/{repo}/pulls      body {head, base, title, body}      -> 201 {number, html_url}
 *   POST /api/v1/repos/{owner}/{repo}/tags       body {tag_name, target, message}   -> 201 {name, commit{sha}}
 *   POST /api/v1/repos/{owner}/{repo}/releases   body {tag_name, name, body, target_commitish} -> 201 {id, tag_name, html_url}
 *   GET  /api/v1/repos/{owner}/{repo}/actions/runs?page=1&limit=20                 -> 200 {workflow_runs:[{status,conclusion,html_url}]}
 * </pre>
 * 铁律：Cortex 只存 Gitea PR/Release URI 引用（设计文档 §3「Gitea 是数据源」）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkGiteaIntegrationService {

    @Value("${spk-delivery.gitea.base-url:http://192.168.56.101:3000}")
    private String baseUrl;
    @Value("${spk-delivery.gitea.token:${GITEA_TOKEN:}}")
    private String token;
    @Value("${spk-delivery.gitea.owner:spk-os}")
    private String owner;
    @Value("${spk-delivery.gitea.repo:smart-home-hub}")
    private String repo;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /** 建分支（ACT-D1）：从 base 分支拉新分支。返回新分支名（成功）或抛异常。 */
    public String createBranch(String newBranch, String baseBranch) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("new_branch_name", newBranch);
        body.put("old_ref_name", baseBranch);
        try {
            String resp = postJson(repoPath("/branches"), JsonUtils.toJsonString(body));
            log.info("[createBranch][{}/{} new={} base={}]", owner, repo, newBranch, baseBranch);
            return newBranch;
        } catch (Exception e) {
            // 分支已存在（409）幂等返回
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                return newBranch;
            }
            throw new RuntimeException("Gitea 建分支失败：" + e.getMessage(), e);
        }
    }

    /** 建 PR（ACT-D5）：head → base。返回 PR html_url。 */
    public String createPR(String head, String base, String title, String body) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("head", head);
        b.put("base", base);
        b.put("title", title == null ? "feat: " + head : title);
        if (body != null) b.put("body", body);
        try {
            String resp = postJson(repoPath("/pulls"), JsonUtils.toJsonString(b));
            JsonNode n = JsonUtils.parseTree(resp);
            JsonNode url = n.get("html_url");
            String num = n.get("number") == null ? "?" : n.get("number").asText();
            String prUrl = url == null ? (baseUrl + "/" + owner + "/" + repo + "/pulls/" + num) : url.asText();
            log.info("[createPR][{}/{} #{} merged?={} url={}]", owner, repo, num, prUrl);
            return prUrl;
        } catch (Exception e) {
            throw new RuntimeException("Gitea 建 PR 失败：" + e.getMessage(), e);
        }
    }

    /** 建 Tag（ACT-L2/L3）：在指定 commitish 打 tag。返回 tag 名。 */
    public String createTag(String tagName, String targetCommitish, String message) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("tag_name", tagName);
        b.put("target", targetCommitish);
        if (message != null) b.put("message", message);
        try {
            postJson(repoPath("/tags"), JsonUtils.toJsonString(b));
            log.info("[createTag][{}/{} tag={} sha={}]", owner, repo, tagName, targetCommitish);
            return tagName;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                return tagName; // 已存在幂等
            }
            throw new RuntimeException("Gitea 建 tag 失败：" + e.getMessage(), e);
        }
    }

    /** 建 Release（ACT-L3/L4）：关联 tag，附 release notes。返回 release html_url。 */
    public String createRelease(String tagName, String name, String body, String targetCommitish) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("tag_name", tagName);
        b.put("name", name == null ? tagName : name);
        if (body != null) b.put("body", body);
        if (targetCommitish != null) b.put("target_commitish", targetCommitish);
        try {
            String resp = postJson(repoPath("/releases"), JsonUtils.toJsonString(b));
            JsonNode n = JsonUtils.parseTree(resp);
            JsonNode url = n.get("html_url");
            String relUrl = url == null ? (baseUrl + "/" + owner + "/" + repo + "/releases/tag/" + tagName) : url.asText();
            log.info("[createRelease][{}/{} tag={} url={}]", owner, repo, tagName, relUrl);
            return relUrl;
        } catch (Exception e) {
            throw new RuntimeException("Gitea 建 Release 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 提交文件到 Gitea 仓库（P6：LLM 长文档落 Gitea，流程变量只存摘要+链接）。
     * <p>
     * Gitea contents API：POST /api/v1/repos/{owner}/{repo}/contents/{path}
     * body {content: Base64, message, branch} -> 201 {content:{html_url}}。
     * 文件已存在返回 409，幂等返回 src/branch url。
     *
     * @param path    仓库内路径（如 docs/concept/run-xxx.md）
     * @param content 文件正文（UTF-8）
     * @param branch  目标分支（如 main）
     * @param message commit message
     * @return 文件 html_url（失败抛异常）
     */
    public String createFile(String path, String content, String branch, String message) {
        String encoded = Base64.getEncoder().encodeToString(
                content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("content", encoded);
        if (branch != null) b.put("branch", branch);
        if (message != null) b.put("message", message);
        String safeBranch = branch == null ? "main" : branch;
        try {
            String resp = postJson("/api/v1/repos/" + owner + "/" + repo + "/contents/" + path,
                    JsonUtils.toJsonString(b));
            JsonNode n = JsonUtils.parseTree(resp);
            JsonNode contentNode = n.get("content");
            if (contentNode != null && contentNode.has("html_url")) {
                String url = contentNode.get("html_url").asText();
                log.info("[createFile][{}/{} path={} url={}]", owner, repo, path, url);
                return url;
            }
            // 兜底构造 url
            String url = baseUrl + "/" + owner + "/" + repo + "/src/branch/" + safeBranch + "/" + path;
            log.info("[createFile][{}/{} path={} fallback-url={}]", owner, repo, path, url);
            return url;
        } catch (Exception e) {
            // 409 文件已存在 -> 幂等返回
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                String url = baseUrl + "/" + owner + "/" + repo + "/src/branch/" + safeBranch + "/" + path;
                log.info("[createFile][{}/{} path={} exists-url={}]", owner, repo, path, url);
                return url;
            }
            throw new RuntimeException("Gitea 提交文件失败：" + e.getMessage(), e);
        }
    }

    /** 取最近 CI 运行状态（ACT-D6/验证阶段）：返回最新一次 run 的 status/conclusion 串。 */
    public String getCiRunStatus() {
        try {
            String resp = getJson(repoPath("/actions/runs?page=1&limit=5"));
            JsonNode n = JsonUtils.parseTree(resp);
            JsonNode runs = n.get("workflow_runs");
            if (runs == null || !runs.isArray() || runs.isEmpty()) {
                return "no-runs";
            }
            JsonNode latest = runs.get(0);
            String status = textOf(latest.get("status"));
            String conclusion = textOf(latest.get("conclusion"));
            return "status=" + status + ", conclusion=" + conclusion;
        } catch (Exception e) {
            log.warn("[getCiRunStatus][失败：{}]", e.getMessage());
            return "unknown";
        }
    }

    public boolean healthCheck() {
        try {
            String resp = getJson("/api/v1/version");
            return resp != null && resp.contains("version");
        } catch (Exception e) {
            return false;
        }
    }

    public String getRepoUrl() {
        return baseUrl + "/" + owner + "/" + repo;
    }

    private String repoPath(String suffix) {
        return "/api/v1/repos/" + owner + "/" + repo + suffix;
    }

    private String postJson(String path, String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .header("Authorization", "token " + token)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("POST " + path + " -> " + resp.statusCode() + "：" + truncate(resp.body(), 300));
        }
        return resp.body();
    }

    private String getJson(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Accept", "application/json").header("Authorization", "token " + token)
                .timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("GET " + path + " -> " + resp.statusCode() + "：" + truncate(resp.body(), 200));
        }
        return resp.body();
    }

    private static String textOf(JsonNode n) {
        return n == null || n.isNull() ? null : n.asText();
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max));
    }
}
