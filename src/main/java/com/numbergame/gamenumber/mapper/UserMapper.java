package com.numbergame.gamenumber.mapper;

import com.numbergame.gamenumber.dto.response.UserInfoResponse;
import com.numbergame.gamenumber.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserInfoResponse toUserInfoResponse(User user, Long rank) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .score(user.getScore())
                .turns(user.getTurns())
                .rank(rank)
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserInfoResponse toUserInfoResponse(User user) {
        return toUserInfoResponse(user, null);
    }
}

