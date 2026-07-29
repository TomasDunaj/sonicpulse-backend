package com.sonicpulse.analyzer.repository;

import com.sonicpulse.analyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findBySpotifyId(String spotifyId);
}
