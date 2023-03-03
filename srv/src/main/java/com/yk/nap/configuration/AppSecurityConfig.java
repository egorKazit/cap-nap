package com.yk.nap.configuration;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Order(1)
@Primary
@Log4j2
public class AppSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(@NonNull HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf().disable().authorizeHttpRequests().anyRequest().authenticated().and().formLogin().and().httpBasic()
                .and().headers().frameOptions().disable();
        return httpSecurity.build();
    }

    @Bean
    public UserDetailsService users(UserHolder userHolder) {

        var users = userHolder.users.stream().map(user -> User.withUsername(user.getUsername())
                .password("{noop}" + user.getPassword())
                .roles()
                .build()).toArray(UserDetails[]::new);

        return new InMemoryUserDetailsManager(users);
    }

}
