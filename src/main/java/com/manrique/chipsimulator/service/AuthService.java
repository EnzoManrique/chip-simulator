package com.manrique.chipsimulator.service;

import com.manrique.chipsimulator.dto.AuthResponseDTO;
import com.manrique.chipsimulator.dto.LoginRequestDTO;
import com.manrique.chipsimulator.dto.RegisterRequestDTO;
import com.manrique.chipsimulator.model.User;
import com.manrique.chipsimulator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponseDTO registerUser(RegisterRequestDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        User savedUser = userRepository.save(user);

        return AuthResponseDTO.builder()
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .build();
    }

    public AuthResponseDTO loginUser(LoginRequestDTO request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getIdentifier());
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByUsername(request.getIdentifier());
        }

        User user = userOptional.orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return AuthResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
