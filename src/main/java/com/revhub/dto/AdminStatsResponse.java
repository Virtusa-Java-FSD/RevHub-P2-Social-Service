package com.revhub.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long bannedUsers;
    private long deletedUsers;
    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;
    private long totalPosts;
    private long postsToday;
    private long postsThisWeek;
    private long postsThisMonth;
    private long pendingReports;
    private long resolvedReports;
}
