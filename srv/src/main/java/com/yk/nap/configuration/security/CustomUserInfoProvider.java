package com.yk.nap.configuration.security;

import com.sap.cds.services.request.ModifiableUserInfo;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.UserInfoProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;

@Component
@Log4j2
public class CustomUserInfoProvider implements UserInfoProvider {

    private final HttpServletRequest request;
    private final UserHolder userHolder;

    public CustomUserInfoProvider(HttpServletRequest request, UserHolder userHolder) {
        this.request = request;
        this.userHolder = userHolder;
    }

    @Override
    public UserInfo get() {

        ModifiableUserInfo userInfo = UserInfo.create();
        if(RequestContextHolder.getRequestAttributes()  == null)
            return userInfo;
        try {
            userInfo.setName(userHolder.users.stream().filter(user -> user.username.equals(request.getUserPrincipal().getName()))
                    .map(UserHolder.User::getUser).findFirst().orElse("unrecognized"));
        } catch (IllegalStateException illegalStateException) {
            log.atWarn().log("Illegal state: " + illegalStateException.getMessage());
        }
        return userInfo;

    }



}
