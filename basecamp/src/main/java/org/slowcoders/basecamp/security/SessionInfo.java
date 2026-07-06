package org.slowcoders.basecamp.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.ZoneId;
import java.util.Collection;

public interface SessionInfo extends UserDetails {

    static SessionInfo current() {
        return ((SecurityUserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSessionInfo();
    }

    String getLoginId();

    String getPassword();

    default ZoneId getUserTimeZone() { return ZoneId.systemDefault(); }

    default String getUserLanguageCode() { return "en"; }

    Collection<? extends GrantedAuthority> getAuthorities();
    /*
        List <GrantedAuthority> listGrantedAuthority = null;
        if(this.authorityIdList != null && !authorityIdList.isEmpty()) {
            listGrantedAuthority = new ArrayList<>();
            for(String role : authorityIdList) {
                listGrantedAuthority.add(new SimpleGrantedAuthority(role));
            }
        }
        return listGrantedAuthority;

     */
}
