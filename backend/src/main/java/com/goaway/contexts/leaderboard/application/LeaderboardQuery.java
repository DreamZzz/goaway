package com.goaway.contexts.leaderboard.application;

/**
 * 排行榜查询参数的白名单枚举，确保拼入 SQL 的表名/列名/聚合式安全可控。
 */
public final class LeaderboardQuery {

    public enum Board {
        FISHING("fishing_sessions", "session_date", "SUM(t.total_seconds)"),
        CHECKIN("checkin_records", "checkin_date", "COUNT(DISTINCT t.checkin_date)");

        public final String table;
        public final String dateColumn;
        public final String scoreExpr;

        Board(String table, String dateColumn, String scoreExpr) {
            this.table = table;
            this.dateColumn = dateColumn;
            this.scoreExpr = scoreExpr;
        }

        public static Board parse(String raw) {
            if (raw != null && raw.equalsIgnoreCase("checkin")) {
                return CHECKIN;
            }
            return FISHING;
        }

        public String code() {
            return name().toLowerCase();
        }
    }

    public enum Dimension {
        ALL(null, "all"),
        CITY("city", "city"),
        INDUSTRY("industry", "industry"),
        JOB_TYPE("job_type", "jobType");

        /** work_profiles 中的列名，ALL 时为 null（不切片）。 */
        public final String column;
        /** 对外暴露的维度代码。 */
        public final String code;

        Dimension(String column, String code) {
            this.column = column;
            this.code = code;
        }

        public static Dimension parse(String raw) {
            if (raw == null) {
                return ALL;
            }
            return switch (raw.toLowerCase()) {
                case "city" -> CITY;
                case "industry" -> INDUSTRY;
                case "jobtype", "job_type" -> JOB_TYPE;
                default -> ALL;
            };
        }
    }

    private LeaderboardQuery() {}
}
