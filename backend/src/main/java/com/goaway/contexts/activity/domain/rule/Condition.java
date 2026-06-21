package com.goaway.contexts.activity.domain.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 比较条件：{"left":<Expr>,"cmp":">|>=|<|<=|==|!=","right":<Expr>,"negate":false}
 * negate=true 表示「非」（取反）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Condition {
    private Expr left;
    private String cmp;
    private Expr right;
    private boolean negate;

    public Expr getLeft() { return left; }
    public void setLeft(Expr left) { this.left = left; }

    public String getCmp() { return cmp; }
    public void setCmp(String cmp) { this.cmp = cmp; }

    public Expr getRight() { return right; }
    public void setRight(Expr right) { this.right = right; }

    public boolean isNegate() { return negate; }
    public void setNegate(boolean negate) { this.negate = negate; }
}
