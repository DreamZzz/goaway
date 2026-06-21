package com.goaway.contexts.admin.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** 规则/表达式校验请求。kind=rule（Rule 布尔规则）或 expr（打分表达式）。 */
public class RuleValidateRequest {
    private String kind;     // rule | expr
    private JsonNode payload;

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }
}
