package com.sonicpulse.analyzer;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "You are not authenticated. Please visit /oauth2/authorization/spotify to log in.";
        }

        String displayName = principal.getAttribute("display_name");
        return "<h1>Success!</h1> You are successfully authenticated via Spotify.<br>User: " + displayName;
    }
}