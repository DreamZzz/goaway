package com.goaway.contexts.leaderboard.api.dto;

public class LeaderboardEntryDTO {
    private int rank;
    private String nickname;
    private long score;
    private boolean me;

    public LeaderboardEntryDTO() {}

    public LeaderboardEntryDTO(int rank, String nickname, long score, boolean me) {
        this.rank = rank;
        this.nickname = nickname;
        this.score = score;
        this.me = me;
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public long getScore() { return score; }
    public void setScore(long score) { this.score = score; }

    public boolean isMe() { return me; }
    public void setMe(boolean me) { this.me = me; }
}
