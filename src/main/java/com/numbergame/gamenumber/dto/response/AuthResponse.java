package com.numbergame.gamenumber.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String email;
    private Integer score;
    private Integer turns;
    private String message;
}

