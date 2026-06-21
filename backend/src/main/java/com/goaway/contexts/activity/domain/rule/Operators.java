package com.goaway.contexts.activity.domain.rule;

import java.util.Set;

/** 运算符白名单 + SQL 映射。规则引擎只接受这些运算符，其余一律报错。 */
public final class Operators {

    public static final Set<String> ARITH = Set.of("+", "-", "*", "/");
    public static final Set<String> CMP = Set.of(">", ">=", "<", "<=", "==", "!=");
    public static final Set<String> LOGIC = Set.of("AND", "OR");

    private Operators() {}

    public static boolean isArith(String op) { return op != null && ARITH.contains(op); }

    public static boolean isCmp(String cmp) { return cmp != null && CMP.contains(cmp); }

    public static boolean isLogic(String logic) { return logic != null && LOGIC.contains(logic); }

    /** 比较符映射到 SQL（== → =，!= → <>）。 */
    public static String cmpToSql(String cmp) {
        return switch (cmp) {
            case "==" -> "=";
            case "!=" -> "<>";
            default -> cmp; // > >= < <= 原样
        };
    }
}
