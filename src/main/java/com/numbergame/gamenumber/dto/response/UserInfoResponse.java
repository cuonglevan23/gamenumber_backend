package com.numbergame.gamenumber.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private Integer score;
    private Integer turns;
    private Long rank;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}

