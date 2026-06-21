package com.goaway.contexts.admin.api.dto;

/** 规则/表达式校验结果：valid + 错误信息 + 编译出的 SQL 预览。 */
public class RuleValidateResult {
    private boolean valid;
    private String error;
    private String sql;

    public RuleValidateResult() {}

    public RuleValidateResult(boolean valid, String error, String sql) {
        this.valid = valid;
        this.error = error;
        this.sql = sql;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
}
