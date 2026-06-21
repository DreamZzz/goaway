package com.goaway.contexts.activity.application.rule;

import com.goaway.contexts.activity.domain.rule.Condition;
import com.goaway.contexts.activity.domain.rule.Expr;
import com.goaway.contexts.activity.domain.rule.Metric;
import com.goaway.contexts.activity.domain.rule.Operators;
import com.goaway.contexts.activity.domain.rule.Rule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 规则的 Java 求值器（勋章判定用）：对一份用户指标快照 Map&lt;metricKey, value&gt; 求值。
 * 非法指标/运算符抛 IllegalArgumentException（供后台校验）。
 */
@Component
public class RuleEvaluator {

    public double evalExpr(Expr e, Map<String, Long> snapshot) {
        if (e == null || e.getType() == null) {
            throw new IllegalArgumentException("表达式节点缺少 type");
        }
        switch (e.getType()) {
            case "metric": {
                Metric m = Metric.byKey(e.getMetric())
                        .orElseThrow(() -> new IllegalArgumentException("未知指标: " + e.getMetric()));
                return snapshot.getOrDefault(m.getKey(), 0L);
            }
            case "const":
                if (e.getValue() == null || e.getValue().isNaN() || e.getValue().isInfinite()) {
                    throw new IllegalArgumentException("常量值非法");
                }
                return e.getValue();
            case "arith": {
                if (!Operators.isArith(e.getOp())) {
                    throw new IllegalArgumentException("非法运算符: " + e.getOp());
                }
                double l = evalExpr(e.getLeft(), snapshot);
                double r = evalExpr(e.getRight(), snapshot);
                return switch (e.getOp()) {
                    case "+" -> l + r;
                    case "-" -> l - r;
                    case "*" -> l * r;
                    case "/" -> r == 0 ? 0 : l / r;
                    default -> throw new IllegalArgumentException("非法运算符: " + e.getOp());
                };
            }
            default:
                throw new IllegalArgumentException("未知表达式类型: " + e.getType());
        }
    }

    public boolean evalCondition(Condition c, Map<String, Long> snapshot) {
        if (c == null || !Operators.isCmp(c.getCmp())) {
            throw new IllegalArgumentException("非法比较符: " + (c == null ? null : c.getCmp()));
        }
        double l = evalExpr(c.getLeft(), snapshot);
        double r = evalExpr(c.getRight(), snapshot);
        boolean res = switch (c.getCmp()) {
            case ">" -> l > r;
            case ">=" -> l >= r;
            case "<" -> l < r;
            case "<=" -> l <= r;
            case "==" -> l == r;
            case "!=" -> l != r;
            default -> throw new IllegalArgumentException("非法比较符: " + c.getCmp());
        };
        return c.isNegate() ? !res : res;
    }

    public boolean evalRule(Rule rule, Map<String, Long> snapshot) {
        List<Condition> conds = rule == null ? null : rule.getConditions();
        if (conds == null || conds.isEmpty() || conds.size() > 3) {
            throw new IllegalArgumentException("规则需要 1-3 个条件");
        }
        if (!Operators.isLogic(rule.getLogic())) {
            throw new IllegalArgumentException("非法逻辑运算: " + rule.getLogic());
        }
        boolean and = "AND".equals(rule.getLogic());
        for (Condition c : conds) {
            boolean v = evalCondition(c, snapshot);
            if (and && !v) return false;
            if (!and && v) return true;
        }
        return and; // AND 全真返回 true；OR 全假返回 false
    }
}
