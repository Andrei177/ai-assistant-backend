package com.example.aienglishchatbackend.user.mapper;

import com.example.aienglishchatbackend.user.dto.res.UserResponse;
import com.example.aienglishchatbackend.user.entity.User;

public class UserMapper {
    public static UserResponse userToUserResponse(User user){
        return new UserResponse(user.getUserId(), user.getUserEmail(), user.getCreatedAt());
    }
}
