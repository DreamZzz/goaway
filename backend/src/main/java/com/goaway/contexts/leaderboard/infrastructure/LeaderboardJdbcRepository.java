package com.goaway.contexts.leaderboard.infrastructure;

import com.goaway.contexts.leaderboard.application.LeaderboardQuery.Board;
import com.goaway.contexts.leaderboard.application.LeaderboardQuery.Dimension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜聚合查询。首版采用实时聚合（用户规模可控），
 * 后续可替换为定时预聚合到 leaderboard_snapshot 而不改动上层接口。
 *
 * 用原生 SQL 跨表聚合 fishing_sessions / checkin_records JOIN work_profiles，
 * 避免在 leaderboard 域里耦合其它域的 JPA 实体。表名/列名均来自白名单枚举。
 */
@Repository
public class LeaderboardJdbcRepository {

    public record ScoreRow(long userId, String nickname, long score) {}

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ScoreRow> topEntries(Board board, Dimension dimension, String slice,
                                     LocalDate start, LocalDate end, int limit) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT w.user_id, w.nickname, ").append(board.scoreExpr).append(" AS score ")
                .append("FROM ").append(board.table).append(" t ")
                .append("JOIN work_profiles w ON w.user_id = t.user_id ")
                .append("WHERE t.").append(board.dateColumn).append(" BETWEEN ? AND ? ");
        List<Object> args = new ArrayList<>();
        args.add(start);
        args.add(end);
        if (board.filter != null) {
            sql.append("AND ").append(board.filter).append(" ");
        }
        if (dimension.column != null) {
            sql.append("AND w.").append(dimension.column).append(" = ? ");
            args.add(slice);
        }
        sql.append("GROUP BY w.user_id, w.nickname ")
                .append("ORDER BY score DESC, w.user_id ASC ")
                .append("LIMIT ?");
        args.add(limit);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) ->
                        new ScoreRow(rs.getLong("user_id"), rs.getString("nickname"), rs.getLong("score")),
                args.toArray());
    }

    /** 当前用户在该切片下的分数（无数据返回 null）。 */
    public Long myScore(Board board, Dimension dimension, String slice,
                        LocalDate start, LocalDate end, long userId) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT ").append(board.scoreExpr).append(" AS score ")
                .append("FROM ").append(board.table).append(" t ")
                .append("WHERE t.user_id = ? AND t.").append(board.dateColumn).append(" BETWEEN ? AND ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.add(start);
        args.add(end);
        if (board.filter != null) {
            sql.append(" AND ").append(board.filter);
        }
        List<Long> result = jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> rs.getObject("score") == null ? null : rs.getLong("score"),
                args.toArray());
        if (result.isEmpty() || result.get(0) == null || result.get(0) == 0L) {
            return null;
        }
        return result.get(0);
    }

    // ── 配置榜（基于 activity_events，scoreExpr/having 由 RuleSqlCompiler 从白名单编译，安全）──

    public List<ScoreRow> topEntriesConfig(String scoreExpr, String having, Dimension dimension,
                                           String slice, LocalDate start, LocalDate end, int limit) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT w.user_id, w.nickname, ").append(scoreExpr).append(" AS score ")
                .append("FROM activity_events t JOIN work_profiles w ON w.user_id = t.user_id ")
                .append("WHERE t.occurred_at::date BETWEEN ? AND ? ");
        List<Object> args = new ArrayList<>();
        args.add(start);
        args.add(end);
        if (dimension.column != null) {
            sql.append("AND w.").append(dimension.column).append(" = ? ");
            args.add(slice);
        }
        sql.append("GROUP BY w.user_id, w.nickname ");
        if (having != null && !having.isBlank()) {
            sql.append("HAVING ").append(having).append(" ");
        }
        sql.append("ORDER BY score DESC, w.user_id ASC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) ->
                        new ScoreRow(rs.getLong("user_id"), rs.getString("nickname"), rs.getLong("score")),
                args.toArray());
    }

    public Long myScoreConfig(String scoreExpr, String having, LocalDate start, LocalDate end, long userId) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT score FROM (SELECT ").append(scoreExpr).append(" AS score ")
                .append("FROM activity_events t WHERE t.user_id = ? AND t.occurred_at::date BETWEEN ? AND ? ")
                .append("GROUP BY t.user_id ");
        if (having != null && !having.isBlank()) {
            sql.append("HAVING ").append(having).append(" ");
        }
        sql.append(") q");
        List<Long> result = jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> rs.getObject("score") == null ? null : rs.getLong("score"),
                userId, start, end);
        if (result.isEmpty() || result.get(0) == null || result.get(0) == 0L) {
            return null;
        }
        return result.get(0);
    }

    public int rankForScoreConfig(String scoreExpr, String having, Dimension dimension, String slice,
                                  LocalDate start, LocalDate end, long myScore) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(*) FROM (SELECT ").append(scoreExpr).append(" AS score ")
                .append("FROM activity_events t JOIN work_profiles w ON w.user_id = t.user_id ")
                .append("WHERE t.occurred_at::date BETWEEN ? AND ? ");
        List<Object> args = new ArrayList<>();
        args.add(start);
        args.add(end);
        if (dimension.column != null) {
            sql.append("AND w.").append(dimension.column).append(" = ? ");
            args.add(slice);
        }
        sql.append("GROUP BY w.user_id HAVING ");
        if (having != null && !having.isBlank()) {
            sql.append("(").append(having).append(") AND ");
        }
        sql.append(scoreExpr).append(" > ?) ranked");
        args.add(myScore);
        Integer higher = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return (higher == null ? 0 : higher) + 1;
    }

    /** 在该切片下分数严格高于 myScore 的用户数 + 1 即为排名。 */
    public int rankForScore(Board board, Dimension dimension, String slice,
                            LocalDate start, LocalDate end, long myScore) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(*) FROM (")
                .append("SELECT ").append(board.scoreExpr).append(" AS score ")
                .append("FROM ").append(board.table).append(" t ")
                .append("JOIN work_profiles w ON w.user_id = t.user_id ")
                .append("WHERE t.").append(board.dateColumn).append(" BETWEEN ? AND ? ");
        List<Object> args = new ArrayList<>();
        args.add(start);
        args.add(end);
        if (board.filter != null) {
            sql.append("AND ").append(board.filter).append(" ");
        }
        if (dimension.column != null) {
            sql.append("AND w.").append(dimension.column).append(" = ? ");
            args.add(slice);
        }
        sql.append("GROUP BY w.user_id HAVING ").append(board.scoreExpr).append(" > ?) ranked");
        args.add(myScore);

        Integer higher = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return (higher == null ? 0 : higher) + 1;
    }
}
