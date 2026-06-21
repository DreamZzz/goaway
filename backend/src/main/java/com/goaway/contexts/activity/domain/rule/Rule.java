package com.goaway.contexts.activity.domain.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 规则：{"logic":"AND|OR","conditions":[<Condition> × 1..3]}
 * 与/或在 logic，非在各 Condition.negate。用于勋章达成判定与榜单 HAVING 过滤。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rule {
    private String logic = "AND";
    private List<Condition> conditions;

    public String getLogic() { return logic; }
    public void setLogic(String logic) { this.logic = logic; }

    public List<Condition> getConditions() { return conditions; }
    public void setConditions(List<Condition> conditions) { this.conditions = conditions; }
}
