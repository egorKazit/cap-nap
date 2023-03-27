package com.yk.nap.configuration.security;

import com.yk.nap.configuration.ParameterHolder;
import org.junit.Test;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomUserInfoProviderTest {

    @Test
    public void get() throws IOException {
        RequestAttributes requestAttributes = mock(RequestAttributes.class);
        RequestContextHolder.setRequestAttributes(requestAttributes);
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getUserPrincipal()).thenReturn(() -> "username");
        ParameterHolder parameterHolder = mock(ParameterHolder.class);
        when(parameterHolder.getCredentialsFile()).thenReturn("src/test/resources/creds.json");
        UserHolder userHolder = new UserHolder(parameterHolder);
        CustomUserInfoProvider customUserInfoProvider = new CustomUserInfoProvider(httpServletRequest, userHolder);
        var user = customUserInfoProvider.get();
        assertEquals("user", user.getName());
    }

    @Test
    public void getNull() {
        RequestContextHolder.setRequestAttributes(null);
        CustomUserInfoProvider customUserInfoProvider = new CustomUserInfoProvider(null, null);
        var user = customUserInfoProvider.get();
        assertEquals("anonymous", user.getName());
    }

    @Test
    public void checkUser() {
        UserHolder.User user = new UserHolder.User("username", "user", "password");
        assertEquals("username", user.getUsername());
        assertEquals("user", user.getUser());
        assertEquals("password", user.getPassword());
    }

}