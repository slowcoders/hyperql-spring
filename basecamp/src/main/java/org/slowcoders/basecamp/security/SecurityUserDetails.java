package org.slowcoders.basecamp.security;

// import org.slowcoders.basecamp.security.SecurityUserDetails;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class SecurityUserDetails implements UserDetails {

    private static final long serialVersionUID = -5620385446816594050L;

    private final SessionInfo sessionInfo;

    enum State {
        ACCOUNT_LOCKED,
        ACCOUNT_EXPIRED,
        PASSWORD_EXPIRED,
        DISABLED,
    }

    public SecurityUserDetails(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return sessionInfo.getAuthorities();
    }

    @Override
    public String getPassword() {
        return sessionInfo.getPassword();
    }

    @Override
    public String getUsername() {
        return sessionInfo.getLoginId();
    }

    public SessionInfo getSessionInfo() {
        return this.sessionInfo;
    }

    @Override
    public boolean isAccountNonExpired() {
        // user.getAccountExpireDate() 검사.
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // return !this.sessionInfo.isAccountLocked();
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 암호 사용기간 만료!
        return true;
    }

    @Override
    public boolean isEnabled() {
        /**
         * 이메일 인증 대기 상태:
         * 서비스 탈퇴 회원:
         * 관리자에 의한 정지:
         */
        return true;
    }

}