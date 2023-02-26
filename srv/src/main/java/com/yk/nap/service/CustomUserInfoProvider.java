package com.yk.nap.service;

import com.sap.cds.feature.xsuaa.XsuaaUserInfo;
import com.sap.cds.services.request.ModifiableUserInfo;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.UserInfoProvider;
import org.springframework.stereotype.Service;

@Service
public class CustomUserInfoProvider implements UserInfoProvider {

    private UserInfoProvider defaultProvider;

    @Override
    public UserInfo get() {
        ModifiableUserInfo userInfo = UserInfo.create();
        if (defaultProvider != null) {
            UserInfo prevUserInfo = defaultProvider.get();
            if (prevUserInfo != null) {
                userInfo = prevUserInfo.copy();
            }
        }
        if (userInfo != null) {
            XsuaaUserInfo xsuaaUserInfo = userInfo.as(XsuaaUserInfo.class);
            userInfo.setName("Yahor");
        }

        return userInfo;
    }

    @Override
    public void setPrevious(UserInfoProvider previous) {
        this.defaultProvider = previous;
    }
}
