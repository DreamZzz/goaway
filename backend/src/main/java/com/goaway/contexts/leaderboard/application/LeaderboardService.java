package com.goaway.contexts.leaderboard.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaway.contexts.activity.application.rule.RuleSqlCompiler;
import com.goaway.contexts.activity.domain.LeaderboardDefinition;
import com.goaway.contexts.activity.domain.rule.Expr;
import com.goaway.contexts.activity.domain.rule.Rule;
import com.goaway.contexts.activity.infrastructure.persistence.LeaderboardDefinitionRepository;
import com.goaway.contexts.leaderboard.api.dto.BoardInfoDTO;
import com.goaway.contexts.leaderboard.api.dto.LeaderboardDTO;
import com.goaway.contexts.leaderboard.api.dto.LeaderboardEntryDTO;
import com.goaway.contexts.leaderboard.application.LeaderboardQuery.Board;
import com.goaway.contexts.leaderboard.application.LeaderboardQuery.Dimension;
import com.goaway.contexts.leaderboard.infrastructure.LeaderboardJdbcRepository;
import com.goaway.contexts.leaderboard.infrastructure.LeaderboardJdbcRepository.ScoreRow;
import com.goaway.shared.time.PeriodRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    private static final int TOP_LIMIT = 50;
    private static final String ANONYMOUS = "匿名打工人";

    private final LeaderboardJdbcRepository repository;
    private final LeaderboardDefinitionRepository definitionRepository;
    private final RuleSqlCompiler ruleSqlCompiler;
    private final ObjectMapper objectMapper;

    public LeaderboardService(LeaderboardJdbcRepository repository,
                              LeaderboardDefinitionRepository definitionRepository,
                              RuleSqlCompiler ruleSqlCompiler,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.definitionRepository = definitionRepository;
        this.ruleSqlCompiler = ruleSqlCompiler;
        this.objectMapper = objectMapper;
    }

    /** 榜单列表：内置 + 启用的配置榜，供 App 动态渲染。 */
    public List<BoardInfoDTO> listBoards() {
        List<BoardInfoDTO> list = new ArrayList<>();
        for (Board b : Board.values()) {
            String defaultPeriod = (b == Board.FISHING || b == Board.CHECKIN) ? "day" : "week";
            list.add(new BoardInfoDTO(b.code(), b.label, b.unit, defaultPeriod));
        }
        for (LeaderboardDefinition def : definitionRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            list.add(new BoardInfoDTO(def.getKey(), def.getTitle(), def.getUnit(), def.getPeriodDefault()));
        }
        return list;
    }

    public LeaderboardDTO load(String boardRaw, String dimensionRaw, String slice,
                               String periodRaw, Long currentUserId) {
        Dimension dimension = Dimension.parse(dimensionRaw);
        String effectiveSlice = trimToNull(slice);
        if (dimension.column != null && effectiveSlice == null) {
            dimension = Dimension.ALL;
        }
        PeriodRange.Period period = PeriodRange.parsePeriod(periodRaw);
        PeriodRange range = PeriodRange.of(period, LocalDate.now());

        final Dimension dim = dimension;
        final String sliceFinal = effectiveSlice;
        Optional<Board> builtin = Board.tryParse(boardRaw);
        return builtin
                .map(b -> loadBuiltin(b, dim, sliceFinal, period, range, currentUserId))
                .orElseGet(() -> loadConfig(boardRaw, dim, sliceFinal, period, range, currentUserId));
    }

    // ── 内置榜 ──
    private LeaderboardDTO loadBuiltin(Board board, Dimension dimension, String slice,
                                       PeriodRange.Period period, PeriodRange range, Long currentUserId) {
        List<ScoreRow> rows = repository.topEntries(board, dimension, slice, range.start(), range.end(), TOP_LIMIT);
        LeaderboardDTO dto = baseDto(board.code(), board.unit, dimension, slice, period, rows, currentUserId);
        if (currentUserId != null) {
            Long myScore = repository.myScore(board, dimension, slice, range.start(), range.end(), currentUserId);
            if (myScore != null) {
                dto.setMyScore(myScore);
                dto.setMyRank(repository.rankForScore(board, dimension, slice, range.start(), range.end(), myScore));
            }
        }
        return dto;
    }

    // ── 配置榜（编译 SQL）──
    private LeaderboardDTO loadConfig(String key, Dimension dimension, String slice,
                                      PeriodRange.Period period, PeriodRange range, Long currentUserId) {
        Optional<LeaderboardDefinition> defOpt = definitionRepository.findByKeyAndEnabledTrue(key);
        if (defOpt.isEmpty()) {
            return baseDto(key, "count", dimension, slice, period, List.of(), currentUserId);
        }
        LeaderboardDefinition def = defOpt.get();
        try {
            String scoreSql = ruleSqlCompiler.compileExpr(objectMapper.readValue(def.getScoreExprJson(), Expr.class));
            String havingSql = (def.getHavingRuleJson() == null || def.getHavingRuleJson().isBlank())
                    ? null
                    : ruleSqlCompiler.compileRule(objectMapper.readValue(def.getHavingRuleJson(), Rule.class));

            List<ScoreRow> rows = repository.topEntriesConfig(scoreSql, havingSql, dimension, slice,
                    range.start(), range.end(), TOP_LIMIT);
            LeaderboardDTO dto = baseDto(def.getKey(), def.getUnit(), dimension, slice, period, rows, currentUserId);
            if (currentUserId != null) {
                Long myScore = repository.myScoreConfig(scoreSql, havingSql, range.start(), range.end(), currentUserId);
                if (myScore != null) {
                    dto.setMyScore(myScore);
                    dto.setMyRank(repository.rankForScoreConfig(scoreSql, havingSql, dimension, slice,
                            range.start(), range.end(), myScore));
                }
            }
            return dto;
        } catch (Exception e) {
            log.warn("配置榜 {} 编译/查询失败: {}", key, e.getMessage());
            return baseDto(key, def.getUnit(), dimension, slice, period, List.of(), currentUserId);
        }
    }

    private LeaderboardDTO baseDto(String boardCode, String unit, Dimension dimension, String slice,
                                   PeriodRange.Period period, List<ScoreRow> rows, Long currentUserId) {
        List<LeaderboardEntryDTO> entries = new ArrayList<>(rows.size());
        int rank = 0;
        for (ScoreRow row : rows) {
            rank++;
            boolean me = currentUserId != null && row.userId() == currentUserId;
            String nickname = (row.nickname() == null || row.nickname().isBlank()) ? ANONYMOUS : row.nickname();
            entries.add(new LeaderboardEntryDTO(rank, nickname, row.score(), me));
        }
        LeaderboardDTO dto = new LeaderboardDTO();
        dto.setBoard(boardCode);
        dto.setUnit(unit);
        dto.setDimension(dimension.code);
        dto.setSlice(dimension == Dimension.ALL ? null : slice);
        dto.setPeriod(period == PeriodRange.Period.WEEK ? "week" : "day");
        dto.setEntries(entries);
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
