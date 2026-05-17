package com.quickbite.restaurant.security;

import com.quickbite.restaurant.entity.User;
import com.quickbite.restaurant.enums.AuthProvider;
import com.quickbite.restaurant.enums.Role;
import com.quickbite.restaurant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        processOAuth2User(oAuth2User);
        return oAuth2User;
    }

    private void processOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        if (email == null) return;

        userRepository.findByEmail(email).ifPresentOrElse(
            existingUser -> {
                existingUser.setProfilePicUrl(picture);
                if (existingUser.getFullName() == null || existingUser.getFullName().isBlank()) {
                    existingUser.setFullName(name != null ? name : email);
                }
                userRepository.save(existingUser);
            },
            () -> {
                User newUser = User.builder()
                        .fullName(name != null ? name : email)
                        .email(email)
                        .passwordHash(null)
                        .role(Role.CUSTOMER)
                        .provider(AuthProvider.GOOGLE)
                        .profilePicUrl(picture)
                        .isActive(true)
                        .build();
                userRepository.save(newUser);
            }
        );
    }
}
