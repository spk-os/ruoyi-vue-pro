package cn.iocoder.yudao.module.spkdelivery.service.ipdworkitem;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkitem.vo.SpkIpdWorkItemVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdWorkItemLinkDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdWorkItemLinkMapper;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkPlaneIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdCommandService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdCommandService.CommandEnvelope;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plane 工作项服务实现（设计文档 §10.7）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkIpdWorkItemServiceImpl implements SpkIpdWorkItemService {

    @Resource
    private SpkIpdWorkItemLinkMapper workItemLinkMapper;
    @Resource
    private SpkPlaneIntegrationService planeIntegrationService;
    @Resource
    private SpkIpdCommandService commandService;

    @Override
    public List<SpkIpdWorkItemVO.RespVO> getWorkItems(Long projectId) {
        // 本地已绑定工作项
        List<SpkIpdWorkItemLinkDO> links = workItemLinkMapper.selectListByProject(projectId);
        Map<String, SpkIpdWorkItemLinkDO> byIssue = new LinkedHashMap<>();
        if (links != null) {
            for (SpkIpdWorkItemLinkDO l : links) {
                byIssue.put(l.getPlaneIssueId(), l);
            }
        }
        List<SpkIpdWorkItemVO.RespVO> result = new ArrayList<>();
        // 合并 Plane 实时 issues
        for (SpkIpdWorkItemVO.RespVO r : fetchPlaneIssues()) {
            SpkIpdWorkItemLinkDO l = byIssue.remove(r.getPlaneIssueId());
            if (l != null) {
                r.setLinked(true);
                r.setVersionId(l.getVersionId());
                r.setFlowRunId(l.getFlowRunId());
                r.setActivityCode(l.getActivityCode());
                r.setLinkType(l.getLinkType() == null ? r.getLinkType() : l.getLinkType());
                r.setSyncStatus(l.getSyncStatus());
                r.setLastSyncedAt(l.getLastSyncedAt());
            } else {
                r.setLinked(false);
                r.setSyncStatus("PENDING");
            }
            result.add(r);
        }
        // Plane 已删但本地仍有绑定的，标 ORPHAN
        for (SpkIpdWorkItemLinkDO l : byIssue.values()) {
            SpkIpdWorkItemVO.RespVO r = new SpkIpdWorkItemVO.RespVO();
            r.setPlaneIssueId(l.getPlaneIssueId());
            r.setPlaneIssueSeq(l.getPlaneIssueSeq());
            r.setLinkType(l.getLinkType());
            r.setSyncStatus("ORPHAN");
            r.setVersionId(l.getVersionId());
            r.setFlowRunId(l.getFlowRunId());
            r.setActivityCode(l.getActivityCode());
            r.setLastSyncedAt(l.getLastSyncedAt());
            r.setLinked(true);
            result.add(r);
        }
        return result;
    }

    @Override
    public SpkIpdWorkItemVO.RespVO linkWorkItem(Long projectId, SpkIpdWorkItemVO.LinkReqVO req) {
        SpkIpdWorkItemLinkDO exist = workItemLinkMapper.selectByPlaneIssueId(projectId, req.getPlaneIssueId());
        LocalDateTime now = LocalDateTime.now();
        if (exist == null) {
            SpkIpdWorkItemLinkDO link = SpkIpdWorkItemLinkDO.builder()
                    .projectId(projectId)
                    .versionId(req.getVersionId())
                    .flowRunId(req.getFlowRunId())
                    .activityCode(req.getActivityCode())
                    .planeIssueId(req.getPlaneIssueId())
                    .planeIssueSeq(req.getPlaneIssueSeq())
                    .linkType(req.getLinkType() == null ? "REQUIREMENT" : req.getLinkType())
                    .syncStatus("LINKED")
                    .lastSyncedAt(now)
                    .build();
            workItemLinkMapper.insert(link);
        } else {
            SpkIpdWorkItemLinkDO patch = new SpkIpdWorkItemLinkDO();
            patch.setId(exist.getId());
            patch.setVersionId(req.getVersionId());
            patch.setFlowRunId(req.getFlowRunId());
            patch.setActivityCode(req.getActivityCode());
            patch.setLinkType(req.getLinkType() == null ? exist.getLinkType() : req.getLinkType());
            patch.setSyncStatus("LINKED");
            patch.setLastSyncedAt(now);
            workItemLinkMapper.updateById(patch);
        }
        SpkIpdWorkItemVO.RespVO resp = new SpkIpdWorkItemVO.RespVO();
        resp.setPlaneIssueId(req.getPlaneIssueId());
        resp.setPlaneIssueSeq(req.getPlaneIssueSeq());
        resp.setLinkType(req.getLinkType() == null ? "REQUIREMENT" : req.getLinkType());
        resp.setVersionId(req.getVersionId());
        resp.setFlowRunId(req.getFlowRunId());
        resp.setActivityCode(req.getActivityCode());
        resp.setSyncStatus("LINKED");
        resp.setLinked(true);
        resp.setLastSyncedAt(now);
        return resp;
    }

    @Override
    public SpkIpdWorkItemVO.SyncRespVO syncWorkItems(Long projectId, String idempotencyKey) {
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? ("plane-sync-" + projectId + "-" + System.currentTimeMillis()) : idempotencyKey;
        CommandEnvelope env = commandService.enlist(key, "PLANE_SYNC", "PROJECT",
                String.valueOf(projectId), "{}");
        SpkIpdWorkItemVO.SyncRespVO resp = new SpkIpdWorkItemVO.SyncRespVO();
        resp.setCommandId(env.commandId());
        if (!env.isNew()) {
            resp.setStatus("IDEMPOTENT");
            resp.setSyncedAt(LocalDateTime.now());
            return resp;
        }
        commandService.markRunning(env.commandId());
        try {
            List<SpkIpdWorkItemVO.RespVO> issues = fetchPlaneIssues();
            int count = 0;
            LocalDateTime now = LocalDateTime.now();
            for (SpkIpdWorkItemVO.RespVO r : issues) {
                SpkIpdWorkItemLinkDO exist = workItemLinkMapper.selectByPlaneIssueId(projectId, r.getPlaneIssueId());
                String snapshot = JsonUtils.toJsonString(Map.of(
                        "name", r.getName() == null ? "" : r.getName(),
                        "seq", r.getPlaneIssueSeq() == null ? "" : r.getPlaneIssueSeq()));
                if (exist == null) {
                    SpkIpdWorkItemLinkDO link = SpkIpdWorkItemLinkDO.builder()
                            .projectId(projectId)
                            .planeIssueId(r.getPlaneIssueId())
                            .planeIssueSeq(r.getPlaneIssueSeq())
                            .linkType("REQUIREMENT")
                            .syncStatus("SYNCED")
                            .lastSnapshotJson(snapshot)
                            .lastSyncedAt(now)
                            .build();
                    workItemLinkMapper.insert(link);
                } else {
                    SpkIpdWorkItemLinkDO patch = new SpkIpdWorkItemLinkDO();
                    patch.setId(exist.getId());
                    patch.setPlaneIssueSeq(r.getPlaneIssueSeq());
                    patch.setSyncStatus("SYNCED");
                    patch.setLastSnapshotJson(snapshot);
                    patch.setLastSyncedAt(now);
                    workItemLinkMapper.updateById(patch);
                }
                count++;
            }
            commandService.markSuccess(env.commandId(),
                    JsonUtils.toJsonString(Map.of("synced", count)));
            resp.setStatus("SUCCESS");
            resp.setSyncedCount(count);
            resp.setSyncedAt(now);
        } catch (Exception e) {
            log.warn("[syncWorkItems] projectId={} err={}", projectId, e.getMessage());
            commandService.markFailed(env.commandId(), "PLANE_SYNC_FAIL", e.getMessage());
            resp.setStatus("FAILED");
            resp.setSyncedAt(LocalDateTime.now());
        }
        return resp;
    }

    /** 解析 Plane listIssues 返回，抽取 id/sequence_id/name */
    @SuppressWarnings("unchecked")
    private List<SpkIpdWorkItemVO.RespVO> fetchPlaneIssues() {
        String raw = planeIntegrationService.listIssues(100);
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        List<SpkIpdWorkItemVO.RespVO> out = new ArrayList<>();
        try {
            JsonNode tree = JsonUtils.parseTree(raw);
            JsonNode arr = tree.has("results") ? tree.get("results") : tree;
            if (arr.isArray()) {
                for (int i = 0; i < arr.size(); i++) {
                    JsonNode n = arr.get(i);
                    SpkIpdWorkItemVO.RespVO r = new SpkIpdWorkItemVO.RespVO();
                    r.setPlaneIssueId(text(n, "id"));
                    r.setPlaneIssueSeq(text(n, "sequence_id"));
                    r.setName(text(n, "name"));
                    out.add(r);
                }
            }
        } catch (Exception e) {
            log.warn("[fetchPlaneIssues] 解析失败：{}", e.getMessage());
        }
        return out;
    }

    private String text(JsonNode n, String field) {
        JsonNode v = n == null ? null : n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
