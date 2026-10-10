package com.sonicpulse.analyzer.security;

import com.sonicpulse.analyzer.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2Successhandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2Successhandler(UserService userService, OAuth2AuthorizedClientService oAuth2AuthorizedClientService) {
        this.userService = userService;
        this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient client = oAuth2AuthorizedClientService.loadAuthorizedClient(authToken.getAuthorizedClientRegistrationId(), authToken.getName());

        userService.processOAuthUser(
                user.getAttribute("id"),
                user.getAttribute("email"),
                user.getAttribute("display_name"),
                client.getAccessToken() != null
                        ? client.getAccessToken().getTokenValue() :
                        null,
                client.getRefreshToken() != null
                        ? client.getRefreshToken().getTokenValue()
                        : null
        );

        response.sendRedirect(frontendUrl);
    }
}