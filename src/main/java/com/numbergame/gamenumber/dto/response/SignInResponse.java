package com.numbergame.gamenumber.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignInResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn; // seconds
    private String username;
    private String email;
    private Integer score;
    private Integer turns;
}
