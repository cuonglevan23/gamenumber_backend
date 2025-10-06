package com.numbergame.gamenumber.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn; // seconds
}
