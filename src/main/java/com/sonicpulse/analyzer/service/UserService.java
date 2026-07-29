package com.sonicpulse.analyzer.service;

import com.sonicpulse.analyzer.entity.User;
import com.sonicpulse.analyzer.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User processOAuthUser(String spotifyId, String email, String displayName, String accessToken, String refreshToken) {
    Optional<User> userToSave = this.userRepository.findBySpotifyId(spotifyId);

        if (userToSave.isEmpty()) {
            User newUser =  new User();
            newUser.setSpotifyId(spotifyId);
            newUser.setEmail(email);
            newUser.setDisplayName(displayName);
            newUser.setAccessToken(accessToken);
            newUser.setRefreshToken(refreshToken);

            return this.userRepository.save(newUser);
        }

        User existingUser = userToSave.get();
        existingUser.setEmail(email);
        existingUser.setDisplayName(displayName);
        existingUser.setAccessToken(accessToken);
        existingUser.setRefreshToken(refreshToken);

        return this.userRepository.save(existingUser);
    }
}
