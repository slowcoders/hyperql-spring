package org.slowcoders.basecamp.security.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.slowcoders.basecamp.security.SecurityUserDetails;
import org.slowcoders.basecamp.security.SecurityUtil;
import org.slowcoders.basecamp.security.TokenProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AbstractAuthService implements AuthService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final TokenProvider tokenProvider;
    private final SecurityUtil securityUtil;

    @Override
    public TokenDTO login(String userId, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userId, password);

        // CustomAuthenticationProvider 를 통해, SecurityUserDetails principal 을 가진 Authentication 획득
        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);

        SecurityUserDetails userDetails = (SecurityUserDetails)authentication.getPrincipal();

        // Current authentication 설정. --> 불필요.
        // SecurityContextHolder.getContext().setAuthentication(authentication);

        // 세션 데이터 생성 --> 불필요.
        // registerSessionInfo(userDetails);

        return TokenDTO.builder()
                .accessToken(tokenProvider.createToken(authentication))
                .refreshToken(tokenProvider.createRefreshToken(authentication))
                .build();
    }

    @Override
    public void logout(@Nullable String accessToken) {

        // 2. 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;

        if (authentication != null && authentication.getPrincipal() instanceof SecurityUserDetails userDetails) {
            username = userDetails.getUsername();
        }

        // 3. 리프레시 토큰 무효화
        if (username != null) {
            // fwcm0001Service.invalidateRefreshTokenByUsername(username);
        }

        // 4. Session 정보 삭제
        if (accessToken != null) {
//            CustomJwtFilter.addLogoutToken(accessToken);
        }

        // 5. SecurityContext 클리어
        SecurityContextHolder.clearContext();

    }

}
