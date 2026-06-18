package com.goaway.contexts.leaderboard.api.dto;

import java.util.List;

public class LeaderboardDTO {
    private String board;       // fishing | checkin
    private String dimension;   // all | city | industry | jobType
    private String slice;       // 维度取值（all 时为 null）
    private String period;      // day | week
    private List<LeaderboardEntryDTO> entries;
    private Integer myRank;     // 未登录 / 未设画像 / 无数据时为 null
    private Long myScore;

    public LeaderboardDTO() {}

    public String getBoard() { return board; }
    public void setBoard(String board) { this.board = board; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public String getSlice() { return slice; }
    public void setSlice(String slice) { this.slice = slice; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public List<LeaderboardEntryDTO> getEntries() { return entries; }
    public void setEntries(List<LeaderboardEntryDTO> entries) { this.entries = entries; }

    public Integer getMyRank() { return myRank; }
    public void setMyRank(Integer myRank) { this.myRank = myRank; }

    public Long getMyScore() { return myScore; }
    public void setMyScore(Long myScore) { this.myScore = myScore; }
}
