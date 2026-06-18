package com.goaway.contexts.leaderboard.application;

import com.goaway.contexts.leaderboard.api.dto.LeaderboardDTO;
import com.goaway.contexts.leaderboard.api.dto.LeaderboardEntryDTO;
import com.goaway.contexts.leaderboard.application.LeaderboardQuery.Board;
import com.goaway.contexts.leaderboard.application.LeaderboardQuery.Dimension;
import com.goaway.contexts.leaderboard.infrastructure.LeaderboardJdbcRepository;
import com.goaway.contexts.leaderboard.infrastructure.LeaderboardJdbcRepository.ScoreRow;
import com.goaway.shared.time.PeriodRange;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardService {

    private static final int TOP_LIMIT = 50;
    private static final String ANONYMOUS = "匿名打工人";

    private final LeaderboardJdbcRepository repository;

    public LeaderboardService(LeaderboardJdbcRepository repository) {
        this.repository = repository;
    }

    public LeaderboardDTO load(String boardRaw, String dimensionRaw, String slice,
                               String periodRaw, Long currentUserId) {
        Board board = Board.parse(boardRaw);
        Dimension dimension = Dimension.parse(dimensionRaw);
        String effectiveSlice = trimToNull(slice);
        // 指定了切片维度却没给取值时，退回全站榜
        if (dimension.column != null && effectiveSlice == null) {
            dimension = Dimension.ALL;
        }

        PeriodRange.Period period = PeriodRange.parsePeriod(periodRaw);
        PeriodRange range = PeriodRange.of(period, LocalDate.now());

        List<ScoreRow> rows = repository.topEntries(board, dimension, effectiveSlice,
                range.start(), range.end(), TOP_LIMIT);

        List<LeaderboardEntryDTO> entries = new ArrayList<>(rows.size());
        int rank = 0;
        for (ScoreRow row : rows) {
            rank++;
            boolean me = currentUserId != null && row.userId() == currentUserId;
            String nickname = (row.nickname() == null || row.nickname().isBlank()) ? ANONYMOUS : row.nickname();
            entries.add(new LeaderboardEntryDTO(rank, nickname, row.score(), me));
        }

        LeaderboardDTO dto = new LeaderboardDTO();
        dto.setBoard(board.code());
        dto.setDimension(dimension.code);
        dto.setSlice(dimension == Dimension.ALL ? null : effectiveSlice);
        dto.setPeriod(period == PeriodRange.Period.WEEK ? "week" : "day");
        dto.setEntries(entries);

        if (currentUserId != null) {
            Long myScore = repository.myScore(board, dimension, effectiveSlice,
                    range.start(), range.end(), currentUserId);
            if (myScore != null) {
                dto.setMyScore(myScore);
                dto.setMyRank(repository.rankForScore(board, dimension, effectiveSlice,
                        range.start(), range.end(), myScore));
            }
        }
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
