package com.goaway.contexts.activity.application.rule;

import com.goaway.contexts.activity.domain.rule.Condition;
import com.goaway.contexts.activity.domain.rule.Expr;
import com.goaway.contexts.activity.domain.rule.Rule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();
    private final RuleSqlCompiler compiler = new RuleSqlCompiler();

    private static Expr metric(String m) { Expr e = new Expr(); e.setType("metric"); e.setMetric(m); return e; }
    private static Expr c(double v) { Expr e = new Expr(); e.setType("const"); e.setValue(v); return e; }
    private static Expr arith(String op, Expr l, Expr r) {
        Expr e = new Expr(); e.setType("arith"); e.setOp(op); e.setLeft(l); e.setRight(r); return e;
    }
    private static Condition cond(Expr l, String cmp, Expr r, boolean neg) {
        Condition c = new Condition(); c.setLeft(l); c.setCmp(cmp); c.setRight(r); c.setNegate(neg); return c;
    }
    private static Rule rule(String logic, Condition... cs) {
        Rule r = new Rule(); r.setLogic(logic); r.setConditions(List.of(cs)); return r;
    }

    // ── Java 求值器 ──

    @Test
    @DisplayName("累计喝水 >= 100：50 不达成，120 达成")
    void evalCumulative() {
        Rule r = rule("AND", cond(metric("water_count"), ">=", c(100), false));
        assertFalse(evaluator.evalRule(r, Map.of("water_count", 50L)));
        assertTrue(evaluator.evalRule(r, Map.of("water_count", 120L)));
    }

    @Test
    @DisplayName("四则运算：累计摸鱼 / 3600 >= 2 小时")
    void evalArith() {
        Rule r = rule("AND", cond(arith("/", metric("fish_total_seconds"), c(3600)), ">=", c(2), false));
        assertTrue(evaluator.evalRule(r, Map.of("fish_total_seconds", 7200L)));
        assertFalse(evaluator.evalRule(r, Map.of("fish_total_seconds", 3599L)));
    }

    @Test
    @DisplayName("与/或/非组合")
    void evalLogic() {
        Map<String, Long> snap = Map.of("water_count", 10L, "smoke_count", 0L);
        // OR：喝水>=100 或 抽烟==0  → 真
        assertTrue(evaluator.evalRule(rule("OR",
                cond(metric("water_count"), ">=", c(100), false),
                cond(metric("smoke_count"), "==", c(0), false)), snap));
        // 非：NOT(抽烟==0) → 假
        assertFalse(evaluator.evalRule(rule("AND",
                cond(metric("smoke_count"), "==", c(0), true)), snap));
    }

    @Test
    @DisplayName("未知指标 / 非法比较符 / 条件数越界 抛错")
    void evalRejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                evaluator.evalRule(rule("AND", cond(metric("hack; DROP TABLE"), ">", c(1), false)), Map.of()));
        assertThrows(IllegalArgumentException.class, () ->
                evaluator.evalRule(rule("AND", cond(metric("water_count"), "LIKE", c(1), false)), Map.of()));
        Rule four = new Rule(); four.setLogic("AND");
        four.setConditions(List.of(
                cond(metric("water_count"), ">", c(1), false), cond(metric("water_count"), ">", c(1), false),
                cond(metric("water_count"), ">", c(1), false), cond(metric("water_count"), ">", c(1), false)));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evalRule(four, Map.of()));
    }

    // ── SQL 编译器 ──

    @Test
    @DisplayName("指标编译为带 type 过滤的聚合片段")
    void compileMetric() {
        assertEquals("COUNT(*) FILTER (WHERE t.type = 'WATER')", compiler.compileExpr(metric("water_count")));
        assertEquals("COALESCE(MAX(t.duration_seconds) FILTER (WHERE t.type = 'FISH'), 0)",
                compiler.compileExpr(metric("fish_max_seconds")));
    }

    @Test
    @DisplayName("除法包 NULLIF 防除零；公式加括号")
    void compileArith() {
        String sql = compiler.compileExpr(arith("/", metric("fish_total_seconds"), c(3600)));
        assertTrue(sql.contains("NULLIF"), sql);
        assertTrue(sql.startsWith("(") && sql.endsWith(")"), sql);
    }

    @Test
    @DisplayName("规则编译为 HAVING 布尔，== → =、!= → <>，negate → NOT")
    void compileRuleSql() {
        String sql = compiler.compileRule(rule("AND",
                cond(metric("water_count"), "==", c(100), false),
                cond(metric("smoke_count"), "!=", c(0), true)));
        assertTrue(sql.contains(" = 100"), sql);
        assertTrue(sql.contains("<>"), sql);
        assertTrue(sql.contains("NOT "), sql);
        assertTrue(sql.contains(" AND "), sql);
    }

    @Test
    @DisplayName("注入防护：未知指标/运算符直接抛错，不产出 SQL")
    void compileRejectsInjection() {
        Expr bad = metric("water_count'; DROP TABLE users;--");
        assertThrows(IllegalArgumentException.class, () -> compiler.compileExpr(bad));
        Expr badOp = arith("%", metric("water_count"), c(2));
        assertThrows(IllegalArgumentException.class, () -> compiler.compileExpr(badOp));
    }
}
