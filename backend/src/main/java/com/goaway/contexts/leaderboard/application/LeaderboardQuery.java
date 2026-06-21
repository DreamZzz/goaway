package com.goaway.contexts.leaderboard.application;

/**
 * 排行榜查询参数的白名单枚举，确保拼入 SQL 的表名/列名/聚合式安全可控。
 */
public final class LeaderboardQuery {

    public enum Board {
        // 既有：日累计摸鱼时长 / 打卡天数
        FISHING("fishing_sessions", "session_date", "SUM(t.total_seconds)", null, "seconds"),
        CHECKIN("checkin_records", "checkin_date", "COUNT(DISTINCT t.checkin_date)", null, "count"),
        // 基于事件级 activity_events 的单次/累计榜（filter 为 type 白名单过滤）
        FISH_SINGLE("activity_events", "occurred_at::date", "MAX(t.duration_seconds)", "t.type = 'FISH'", "seconds"),
        POOP_SINGLE("activity_events", "occurred_at::date", "MAX(t.duration_seconds)", "t.type = 'POOP'", "seconds"),
        FISH_TOTAL("activity_events", "occurred_at::date", "SUM(t.duration_seconds)", "t.type = 'FISH'", "seconds"),
        WATER_TOTAL("activity_events", "occurred_at::date", "COUNT(*)", "t.type = 'WATER'", "count"),
        SMOKE_TOTAL("activity_events", "occurred_at::date", "COUNT(*)", "t.type = 'SMOKE'", "count"),
        POOP_TOTAL("activity_events", "occurred_at::date", "COUNT(*)", "t.type = 'POOP'", "count");

        public final String table;
        public final String dateColumn;
        public final String scoreExpr;
        /** 追加到 WHERE 的 type 过滤（来自白名单，无注入风险）；为 null 表示不过滤。 */
        public final String filter;
        /** 分数单位：seconds（时长）/ count（次数/天数），供前端格式化。 */
        public final String unit;

        Board(String table, String dateColumn, String scoreExpr, String filter, String unit) {
            this.table = table;
            this.dateColumn = dateColumn;
            this.scoreExpr = scoreExpr;
            this.filter = filter;
            this.unit = unit;
        }

        public static Board parse(String raw) {
            if (raw == null) {
                return FISHING;
            }
            return switch (raw.toLowerCase()) {
                case "checkin" -> CHECKIN;
                case "fish_single" -> FISH_SINGLE;
                case "poop_single" -> POOP_SINGLE;
                case "fish_total" -> FISH_TOTAL;
                case "water_total" -> WATER_TOTAL;
                case "smoke_total" -> SMOKE_TOTAL;
                case "poop_total" -> POOP_TOTAL;
                default -> FISHING;
            };
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
