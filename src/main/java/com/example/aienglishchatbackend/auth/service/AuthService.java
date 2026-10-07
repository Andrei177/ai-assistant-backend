package com.example.aienglishchatbackend.auth.service;

import com.example.aienglishchatbackend.auth.dto.req.RegisterRequest;
import com.example.aienglishchatbackend.common.exception.ResourceAlreadyExist;
import com.example.aienglishchatbackend.user.entity.User;
import com.example.aienglishchatbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(RegisterRequest req){
        Optional<User> candidate = userRepository.findByUserEmail(req.getUserEmail());
        if(candidate.isPresent()){
            throw new ResourceAlreadyExist("Пользователь с таким email уже существует");
        }


        String hashPassword = passwordEncoder.encode(req.getPassword());
        User newUser = User.builder()
                .userEmail(req.getUserEmail())
                .hashPassword(hashPassword)
                .build();

        return userRepository.save(newUser);
    }
}
