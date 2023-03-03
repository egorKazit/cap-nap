package com.yk.nap.configuration;

import com.sap.cds.services.request.ModifiableUserInfo;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.UserInfoProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Order(0)
@Service
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
        try {
            userInfo.setName(userHolder.users.stream().filter(user -> user.username.equals(request.getUserPrincipal().getName()))
                    .map(UserHolder.User::getUser).findFirst().orElse("unrecognized"));
        } catch (IllegalStateException illegalStateException) {
            log.atWarn().log("Illegal state: " + illegalStateException.getMessage());
        }
        return userInfo;

    }



}
