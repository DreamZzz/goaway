package com.goaway.contexts.admin.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaway.contexts.activity.application.rule.RuleSqlCompiler;
import com.goaway.contexts.activity.domain.BadgeDefinition;
import com.goaway.contexts.activity.domain.LeaderboardDefinition;
import com.goaway.contexts.activity.domain.rule.Expr;
import com.goaway.contexts.activity.domain.rule.Metric;
import com.goaway.contexts.activity.domain.rule.Operators;
import com.goaway.contexts.activity.domain.rule.Rule;
import com.goaway.contexts.activity.infrastructure.persistence.BadgeDefinitionRepository;
import com.goaway.contexts.activity.infrastructure.persistence.LeaderboardDefinitionRepository;
import com.goaway.contexts.admin.api.dto.AdminMetaDTO;
import com.goaway.contexts.admin.api.dto.BadgeDefInput;
import com.goaway.contexts.admin.api.dto.LeaderboardDefInput;
import com.goaway.contexts.admin.api.dto.RuleValidateRequest;
import com.goaway.contexts.admin.api.dto.RuleValidateResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AdminService {

    private final BadgeDefinitionRepository badgeRepo;
    private final LeaderboardDefinitionRepository boardRepo;
    private final RuleSqlCompiler compiler;
    private final ObjectMapper objectMapper;

    public AdminService(BadgeDefinitionRepository badgeRepo, LeaderboardDefinitionRepository boardRepo,
                        RuleSqlCompiler compiler, ObjectMapper objectMapper) {
        this.badgeRepo = badgeRepo;
        this.boardRepo = boardRepo;
        this.compiler = compiler;
        this.objectMapper = objectMapper;
    }

    public AdminMetaDTO meta() {
        AdminMetaDTO m = new AdminMetaDTO();
        List<AdminMetaDTO.MetricInfo> metrics = new ArrayList<>();
        for (Metric metric : Metric.values()) {
            metrics.add(new AdminMetaDTO.MetricInfo(metric.getKey(), metric.getLabel(), metric.getUnit()));
        }
        m.setMetrics(metrics);
        m.setArithOps(new ArrayList<>(Operators.ARITH));
        m.setCompareOps(new ArrayList<>(Operators.CMP));
        m.setLogicOps(new ArrayList<>(Operators.LOGIC));
        m.setBadgeKinds(List.of("SINGLE", "CUMULATIVE"));
        m.setIcons(Arrays.asList("trophy", "fish", "toilet", "water", "smoke", "soup", "doc", "roast", "fortune", "coin", "bell"));
        return m;
    }

    // ── 勋章 ──

    public List<BadgeDefinition> listBadges() {
        return badgeRepo.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public BadgeDefinition saveBadge(Long id, BadgeDefInput in) {
        BadgeDefinition def = id == null ? new BadgeDefinition() : findBadge(id);
        if (id == null) {
            requireKey(in.getKey());
            if (badgeRepo.existsByKey(in.getKey())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "勋章 key 已存在: " + in.getKey());
            }
            def.setKey(in.getKey());
        }
        def.setTitle(require(in.getTitle(), "title"));
        def.setDescription(in.getDescription());
        def.setIcon(in.getIcon() == null ? "trophy" : in.getIcon());
        def.setKind(in.getKind() == null ? "CUMULATIVE" : in.getKind());
        def.setRuleJson(compileRuleJson(in.getRule())); // 校验并落库
        if (in.getEnabled() != null) def.setEnabled(in.getEnabled());
        if (in.getSortOrder() != null) def.setSortOrder(in.getSortOrder());
        return badgeRepo.save(def);
    }

    @Transactional
    public BadgeDefinition toggleBadge(Long id) {
        BadgeDefinition def = findBadge(id);
        def.setEnabled(!def.isEnabled());
        return badgeRepo.save(def);
    }

    @Transactional
    public void deleteBadge(Long id) {
        badgeRepo.delete(findBadge(id));
    }

    // ── 榜单 ──

    public List<LeaderboardDefinition> listLeaderboards() {
        return boardRepo.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public LeaderboardDefinition saveLeaderboard(Long id, LeaderboardDefInput in) {
        LeaderboardDefinition def = id == null ? new LeaderboardDefinition() : findBoard(id);
        if (id == null) {
            requireKey(in.getKey());
            if (boardRepo.existsByKey(in.getKey())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "榜单 key 已存在: " + in.getKey());
            }
            def.setKey(in.getKey());
        }
        def.setTitle(require(in.getTitle(), "title"));
        def.setScoreExprJson(compileExprJson(in.getScoreExpr())); // 校验打分公式
        def.setHavingRuleJson(in.getHaving() == null || in.getHaving().isNull()
                ? null : compileRuleJson(in.getHaving()));         // 校验可选过滤
        if (in.getUnit() != null) def.setUnit(in.getUnit());
        if (in.getEnabled() != null) def.setEnabled(in.getEnabled());
        if (in.getSortOrder() != null) def.setSortOrder(in.getSortOrder());
        if (in.getPeriodDefault() != null) def.setPeriodDefault(in.getPeriodDefault());
        return boardRepo.save(def);
    }

    @Transactional
    public LeaderboardDefinition toggleLeaderboard(Long id) {
        LeaderboardDefinition def = findBoard(id);
        def.setEnabled(!def.isEnabled());
        return boardRepo.save(def);
    }

    @Transactional
    public void deleteLeaderboard(Long id) {
        boardRepo.delete(findBoard(id));
    }

    // ── 校验 ──

    public RuleValidateResult validate(RuleValidateRequest req) {
        try {
            if ("expr".equalsIgnoreCase(req.getKind())) {
                Expr expr = objectMapper.treeToValue(req.getPayload(), Expr.class);
                return new RuleValidateResult(true, null, compiler.compileExpr(expr));
            }
            Rule rule = objectMapper.treeToValue(req.getPayload(), Rule.class);
            return new RuleValidateResult(true, null, compiler.compileRule(rule));
        } catch (Exception e) {
            return new RuleValidateResult(false, e.getMessage(), null);
        }
    }

    // ── 内部：解析 + 编译校验，返回归一化 JSON 字符串 ──

    private String compileRuleJson(JsonNode node) {
        if (node == null || node.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "规则不能为空");
        }
        try {
            Rule rule = objectMapper.treeToValue(node, Rule.class);
            compiler.compileRule(rule); // 校验：非法 metric/op 会抛错
            return objectMapper.writeValueAsString(rule);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "规则非法: " + e.getMessage());
        }
    }

    private String compileExprJson(JsonNode node) {
        if (node == null || node.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "打分公式不能为空");
        }
        try {
            Expr expr = objectMapper.treeToValue(node, Expr.class);
            compiler.compileExpr(expr); // 校验
            return objectMapper.writeValueAsString(expr);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "打分公式非法: " + e.getMessage());
        }
    }

    private BadgeDefinition findBadge(Long id) {
        return badgeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "勋章不存在: " + id));
    }

    private LeaderboardDefinition findBoard(Long id) {
        return boardRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "榜单不存在: " + id));
    }

    private void requireKey(String key) {
        if (key == null || !key.matches("[a-z0-9_]{2,40}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key 需为 2-40 位小写字母/数字/下划线");
        }
    }

    private String require(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 不能为空");
        }
        return v.trim();
    }
}
