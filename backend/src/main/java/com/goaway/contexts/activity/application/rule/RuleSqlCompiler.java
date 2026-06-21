package com.goaway.contexts.activity.application.rule;

import com.goaway.contexts.activity.domain.rule.Condition;
import com.goaway.contexts.activity.domain.rule.Expr;
import com.goaway.contexts.activity.domain.rule.Metric;
import com.goaway.contexts.activity.domain.rule.Operators;
import com.goaway.contexts.activity.domain.rule.Rule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则的 SQL 编译器（榜单用）：把表达式编成对 activity_events（别名 t）的聚合 SQL 片段，
 * 把规则编成 HAVING 布尔表达式。
 *
 * 安全：叶子只能是白名单指标（来自 Metric.sqlFragment，常量字符串）或数值常量（Java 解析为 double 后格式化），
 * 运算符/比较符来自白名单枚举。任何外部字符串都不会进入 SQL，无注入风险。
 */
@Component
public class RuleSqlCompiler {

    public String compileExpr(Expr e) {
        if (e == null || e.getType() == null) {
            throw new IllegalArgumentException("表达式节点缺少 type");
        }
        switch (e.getType()) {
            case "metric":
                return Metric.byKey(e.getMetric())
                        .orElseThrow(() -> new IllegalArgumentException("未知指标: " + e.getMetric()))
                        .sqlFragment();
            case "const":
                return formatNumber(e.getValue());
            case "arith": {
                if (!Operators.isArith(e.getOp())) {
                    throw new IllegalArgumentException("非法运算符: " + e.getOp());
                }
                String left = compileExpr(e.getLeft());
                String right = compileExpr(e.getRight());
                if ("/".equals(e.getOp())) {
                    right = "NULLIF(" + right + ", 0)"; // 防除零
                }
                return "(" + left + " " + e.getOp() + " " + right + ")";
            }
            default:
                throw new IllegalArgumentException("未知表达式类型: " + e.getType());
        }
    }

    public String compileCondition(Condition c) {
        if (c == null || !Operators.isCmp(c.getCmp())) {
            throw new IllegalArgumentException("非法比较符: " + (c == null ? null : c.getCmp()));
        }
        String s = "(" + compileExpr(c.getLeft()) + " " + Operators.cmpToSql(c.getCmp())
                + " " + compileExpr(c.getRight()) + ")";
        return c.isNegate() ? "NOT " + s : s;
    }

    /** 规则 → HAVING 布尔表达式。 */
    public String compileRule(Rule rule) {
        List<Condition> conds = rule == null ? null : rule.getConditions();
        if (conds == null || conds.isEmpty() || conds.size() > 3) {
            throw new IllegalArgumentException("规则需要 1-3 个条件");
        }
        if (!Operators.isLogic(rule.getLogic())) {
            throw new IllegalArgumentException("非法逻辑运算: " + rule.getLogic());
        }
        String joiner = " " + rule.getLogic() + " ";
        return conds.stream().map(this::compileCondition).collect(Collectors.joining(joiner));
    }

    /** 数值常量格式化：整数去小数点，避免科学计数法；非法值报错。 */
    private String formatNumber(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            throw new IllegalArgumentException("常量值非法");
        }
        double v = value;
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return Long.toString((long) v);
        }
        return java.math.BigDecimal.valueOf(v).toPlainString();
    }
}
