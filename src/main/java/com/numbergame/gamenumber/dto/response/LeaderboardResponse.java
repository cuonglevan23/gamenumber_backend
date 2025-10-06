package com.numbergame.gamenumber.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private Integer rank;
    private Long userId;
    private String username;
    private Integer score;
}

