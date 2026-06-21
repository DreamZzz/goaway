package com.goaway.contexts.activity.domain.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 表达式节点（JSON 存库）。三选一：
 * - 指标：{"type":"metric","metric":"fish_total_seconds"}
 * - 常量：{"type":"const","value":3600}
 * - 四则：{"type":"arith","op":"+|-|*|/","left":<Expr>,"right":<Expr>}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Expr {
    private String type;
    private String metric;
    private Double value;
    private String op;
    private Expr left;
    private Expr right;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }

    public Expr getLeft() { return left; }
    public void setLeft(Expr left) { this.left = left; }

    public Expr getRight() { return right; }
    public void setRight(Expr right) { this.right = right; }
}
