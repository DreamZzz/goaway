package com.goaway.contexts.leaderboard.application;

/**
 * 排行榜查询参数的白名单枚举，确保拼入 SQL 的表名/列名/聚合式安全可控。
 */
public final class LeaderboardQuery {

    public enum Board {
        // 既有：日累计摸鱼时长 / 打卡天数
        FISHING("摸鱼时长", "fishing_sessions", "session_date", "SUM(t.total_seconds)", null, "seconds"),
        CHECKIN("连续打卡", "checkin_records", "checkin_date", "COUNT(DISTINCT t.checkin_date)", null, "count"),
        // 基于事件级 activity_events 的单次/累计榜（filter 为 type 白名单过滤）
        FISH_SINGLE("单次最长摸鱼", "activity_events", "occurred_at::date", "MAX(t.duration_seconds)", "t.type = 'FISH'", "seconds"),
        POOP_SINGLE("单次最长带薪", "activity_events", "occurred_at::date", "MAX(t.duration_seconds)", "t.type = 'POOP'", "seconds"),
        FISH_TOTAL("累计摸鱼", "activity_events", "occurred_at::date", "SUM(t.duration_seconds)", "t.type = 'FISH'", "seconds"),
        WATER_TOTAL("喝水次数", "activity_events", "occurred_at::date", "COUNT(*)", "t.type = 'WATER'", "count"),
        SMOKE_TOTAL("抽烟次数", "activity_events", "occurred_at::date", "COUNT(*)", "t.type = 'SMOKE'", "count"),
        POOP_TOTAL("带薪拉屎次数", "activity_events", "occurred_at::date", "COUNT(*)", "t.type = 'POOP'", "count");

        public final String label;
        public final String table;
        public final String dateColumn;
        public final String scoreExpr;
        /** 追加到 WHERE 的 type 过滤（来自白名单，无注入风险）；为 null 表示不过滤。 */
        public final String filter;
        /** 分数单位：seconds（时长）/ count（次数/天数），供前端格式化。 */
        public final String unit;

        Board(String label, String table, String dateColumn, String scoreExpr, String filter, String unit) {
            this.label = label;
            this.table = table;
            this.dateColumn = dateColumn;
            this.scoreExpr = scoreExpr;
            this.filter = filter;
            this.unit = unit;
        }

        public static Board parse(String raw) {
            return tryParse(raw).orElse(FISHING);
        }

        /** 精确匹配内置榜代码；非内置（如配置榜 key）返回空。 */
        public static java.util.Optional<Board> tryParse(String raw) {
            if (raw == null) {
                return java.util.Optional.empty();
            }
            return switch (raw.toLowerCase()) {
                case "fishing" -> java.util.Optional.of(FISHING);
                case "checkin" -> java.util.Optional.of(CHECKIN);
                case "fish_single" -> java.util.Optional.of(FISH_SINGLE);
                case "poop_single" -> java.util.Optional.of(POOP_SINGLE);
                case "fish_total" -> java.util.Optional.of(FISH_TOTAL);
                case "water_total" -> java.util.Optional.of(WATER_TOTAL);
                case "smoke_total" -> java.util.Optional.of(SMOKE_TOTAL);
                case "poop_total" -> java.util.Optional.of(POOP_TOTAL);
                default -> java.util.Optional.empty();
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
