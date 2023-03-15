package com.yk.nap.configuration.security;

import com.google.gson.Gson;
import com.yk.nap.configuration.ParameterHolder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
@Order(0)
@Primary
public class UserHolder {

    final List<User> users;

    UserHolder(ParameterHolder parameterHolder) throws IOException {
        String credentials = Files.readString(Path.of(parameterHolder.getCredentialsFile()));
        users = Stream.of(new Gson().fromJson(credentials, User[].class)).toList();
    }

    @Getter
    @AllArgsConstructor
    static class User{
        final String username;
        final String user;
        final String password;
    }

}
