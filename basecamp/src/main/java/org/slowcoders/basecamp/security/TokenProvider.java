package org.slowcoders.basecamp.security;
// package org.slowcoders.basecamp.fw.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.slowcoders.basecamp.security.exception.JwtSecurityException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.security.Key;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TokenProvider {

    private final long accessTokenExpire;
    private final long refreshTokenExpire;

    private final Key key;
    private final UserDetailsService userDetailsService;

    public TokenProvider(
            byte[] keyBytes,
            long accessTokenExpire,
            long refreshTokenExpire,
            UserDetailsService userDetailsService
    ) {
        this.accessTokenExpire = accessTokenExpire;
        this.refreshTokenExpire = refreshTokenExpire;
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.userDetailsService = userDetailsService;
    }

    public Claims getClaims(String token){
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        if (claims.get("sub") == null || claims.get("role") == null) {
            // TODO : JWT 토큰 오류
            throw new JwtSecurityException("JWT 토큰 오류");
        }
        return claims;
    }

    // 토큰을 받아 클레임을 만들고 권한정보를 빼서 시큐리티 유저객체를 만들어 Authentication 객체 반환
    public Authentication getAuthentication(String token) {

        Claims claims = this.getClaims(token);

//        String roleIdString = (String) claims.get("role");
//        String languageCd   = (String) claims.get("languageCd");
//        String timeZoneCd = (String) claims.get("timeZoneCd");

        UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());

        return new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities());
    }


    /**
     * 토큰 생성
     *
     * @param authentication
     * @return
     */
    public String createToken(Authentication authentication) {
        SecurityUserDetails securityUserDetails = (SecurityUserDetails) authentication.getPrincipal();
        Date accessTokenExpiresIn = new Date(System.currentTimeMillis() + (accessTokenExpire * 1000L));

        SessionInfo sessionInfo = securityUserDetails.getSessionInfo();
        Map<String, Object> claims = new HashMap<>();
        claims.put("timeZoneCd",  sessionInfo.getUserTimeZone());
        claims.put("username", securityUserDetails.getUsername());
        claims.put("languageCd", sessionInfo.getUserLanguageCode());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(securityUserDetails.getUsername())
                .setExpiration(accessTokenExpiresIn)
                .setIssuedAt(Calendar.getInstance().getTime())
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // 토큰 생성
    public String createRefreshToken(Authentication authentication) {
        SecurityUserDetails securityUserDetails = (SecurityUserDetails) authentication.getPrincipal();
        Date refreshTokenExpiresIn = new Date(System.currentTimeMillis() + (refreshTokenExpire * 1000L));
        return Jwts.builder()
                .setSubject(securityUserDetails.getUsername())
                .setIssuedAt(Calendar.getInstance().getTime())
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다. : [[]]", e.toString());
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.: [[]]", e.toString());
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.: [[]]", e.toString());
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.: [[]]", e.toString());
        }
        return false;
    }


}
