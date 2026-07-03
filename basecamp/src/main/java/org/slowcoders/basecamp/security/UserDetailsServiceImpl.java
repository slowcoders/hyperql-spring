package org.slowcoders.basecamp.security;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slowcoders.basecamp.app.model.UserDto;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService, UserCache {

    private final SecurityUtil securityUtil;

    private final AuthenticationProvider authenticationProvider;

    private final HashMap<String, SecurityUserDetails> userCache = new HashMap<>();

    @PostConstruct
    public void init() {
        /**
         * AuthenticationProvider 를 별도로 구현하지 않으면, DaoAuthenticationProvider 가 사용된다.
         * -- class DaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider
         */
        ((AbstractUserDetailsAuthenticationProvider)authenticationProvider).setUserCache(this);
    }

    @Override
    public SecurityUserDetails getUserFromCache(String loginId) {
        return userCache.get(loginId);
    }

    @Override
    public void putUserInCache(UserDetails user) {
        userCache.put(user.getUsername(), (SecurityUserDetails) user);
    }

    @Override
    public void removeUserFromCache(String loginId) {
        userCache.remove(loginId);
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {

        /**
         * Todo. Logout 처리!!
         */

        /**
         * 로드된 UserDetails 의 password 는 아래의 함수를 통해 검사된다.
         *   AbstractUserDetailsAuthenticationProvider.performPreCheck
         *      DaoAuthenticationProvider.additionalAuthenticationChecks
         */

        // 사용자 정보 조회
        UserDto userDto = UserDto.builder().loginId(loginId)
                .password(securityUtil.encrypt("1234")).build();
//        UserDto userDto = fwcm0000Service.getLoginId4User2(loginId);
//        if (userDto == null) {
//            throw new BusinessException("ER-FWCM-1016");
//        }
//
//        LocalDate now = LocalDate.now();
//        // 계정 만료 ... ER-FWCM-1037
//        if(now.isAfter(userDto.getAccountExpireDate())) {
//            throw new BusinessException("ER-FWCM-1037");
//        }
//
//        if(ObjectUtils.isEmpty(userDto.getUserPassword()) || "".equals(userDto.getUserPassword())){
//            throw new BusinessException("ER-FWCM-1016");
//        }
//
//        // 패스워드 만료 ... ER-FWCM-1020
//        if(now.isAfter(userDto.getPasswordExpireDate())) {
//            throw new CredentialsExpiredException("ER-FWCM-1020");
//        }
//
//        // 사용자 권한 조회
//        UserAuthorityPVo userAuthorityPVo = UserAuthorityPVo.builder()
//                .chainId(userDto.getChainId())
//                .propertyId(userDto.getPropertyId())
//                .userId(userDto.getUserId())
//                .build();
//
//        List<String> listRoleName = null;
//
//        if("Y".equals(userDto.getApiUserYn())){ // API 사용자의 경우
//            listRoleName = new ArrayList<>(1);
//            listRoleName.add("API_USER");
//        }else{
//            listRoleName = fwcm0000Service.getAuthorityIdList(userAuthorityPVo);
//            if (ObjectUtils.isEmpty(listRoleName) || listRoleName.size() == 0) {
//                throw new BusinessException("ER-FWCM-1036");
//            }
//        }
//
//        // 유효성을 체크하고 넘겨 준다면....
//		boolean enabled = "ACTIVE".equals(userDto.getUserStatusCd());
//		boolean accountNonExpired = userDto.getAccountExpireDate() != null && now.isBefore(userDto.getAccountExpireDate());
//		boolean credentialsNonExpired = userDto.getPasswordExpireDate() != null && now.isBefore(userDto.getPasswordExpireDate().plusDays(1));
//
        return new SecurityUserDetails(userDto);
    }
}