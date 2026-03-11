package com.example.prompt.security;

import com.example.prompt.domain.PlanEntity;
import com.example.prompt.domain.UserEntity;
import com.example.prompt.repository.PlanRepository;
import com.example.prompt.repository.UserRepository;
import com.example.prompt.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("===== OAuth2 loadUser 실행됨 =====");
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String provider   = userRequest.getClientRegistration().getRegistrationId();
        String providerId = (String) attributes.get("sub");
        String email      = (String) attributes.get("email");
        String name       = (String) attributes.get("name");

        UserEntity user = findOrCreateUser(provider, providerId, email, name);

        log.info("OAuth2 유저 처리 완료 - userId: {}", user.getId());
        return new CustomOAuth2UserDetails(user, attributes);
    }

    @Transactional
    public UserEntity findOrCreateUser(String provider, String providerId, String email, String name) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    return userRepository.findByEmail(email).orElseGet(() -> {
                        PlanEntity normalPlan = planRepository.findByPlanName("NORMAL")
                                .orElseThrow(() -> new IllegalStateException("기본 플랜이 존재하지 않습니다"));
                        UserEntity newUser = UserEntity.builder()
                                .userid(provider + "_" + UUID.randomUUID().toString().substring(0, 8))
                                .username(name)
                                .password("OAUTH2_NO_PASSWORD")
                                .email(email)
                                .plan(normalPlan)
                                .provider(provider)
                                .providerId(providerId)
                                .build();
                        return userRepository.save(newUser);
                    });
                });
    }
}
