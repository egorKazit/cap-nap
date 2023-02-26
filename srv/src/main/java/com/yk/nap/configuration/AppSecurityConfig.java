package com.yk.nap.configuration;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
@Order(1)
public class AppSecurityConfig {

    private final UserDetailsManager userDetailsManager;

    public AppSecurityConfig(UserDetailsManager userDetailsManager, @NonNull ParameterHolder parameterHolder) {
        this.userDetailsManager = userDetailsManager;
        setupUsers(parameterHolder);
    }

    @Bean
    public SecurityFilterChain filterChain(@NonNull HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf().disable().authorizeHttpRequests().anyRequest().authenticated().and().formLogin().and().httpBasic();
        return httpSecurity.build();
    }

    private void setupUsers(@NonNull ParameterHolder parameterHolder) {
        UserDetails userWithP = new User(parameterHolder.getUsername(), "{noop}" + parameterHolder.getPassword(), List.of(new SimpleGrantedAuthority("ThreadOperator")));
        userDetailsManager.createUser(userWithP);
    }

}
