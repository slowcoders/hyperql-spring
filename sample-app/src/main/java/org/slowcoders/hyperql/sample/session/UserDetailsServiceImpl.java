package org.slowcoders.hyperql.sample.session;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slowcoders.basecamp.db.PostgresNotificationListener;
import org.slowcoders.basecamp.security.AbstractUserDetailsService;
import org.slowcoders.basecamp.security.SessionInfo;
import org.slowcoders.basecamp.security.SessionInfoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class UserDetailsServiceImpl extends AbstractUserDetailsService {
//    private final PasswordEncoder passwordEncoder;
    private final PostgresNotificationListener userChangeListener;

    UserDetailsServiceImpl(UserMapper userMapper, DataSource dataSource, PasswordEncoder passwordEncoder) {
        super(new SessionInfoRepository() {
            final UserMapper userRepository = userMapper;
            @Override
            public SessionInfo loadSessionInfoByLoginId(String loginId) {
                if ("string".equals(loginId)) {
                    return UserDto.builder()
                            .userId(loginId)
                            .password(passwordEncoder.encode("1234"))
                            .build();
                }
                return userRepository.findById(loginId).orElse(null);
            }
        });

        userChangeListener = new PostgresNotificationListener("user_changed", dataSource) {
            @Override
            protected void notifyEvent(String channel, String parameter) {
                removeUserFromCache(parameter);
            }
        };
    }

    @PostConstruct
    public void startListening() {
        userChangeListener.startListening();
    }

    @PreDestroy
    public void stopListening() {
        userChangeListener.stopListening();
    }

}
