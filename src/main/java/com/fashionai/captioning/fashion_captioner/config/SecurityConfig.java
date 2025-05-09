package com.fashionai.captioning.fashion_captioner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // disable CSRF for simplicity
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").authenticated() // require login
                .anyRequest().permitAll()                     // others are public
            )
            .httpBasic(Customizer.withDefaults()); // enable HTTP Basic auth

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
            .password("{noop}admin") // {noop} tells Spring to use plain text
            .roles("ADMIN")
            .build();

        UserDetails user = User.withUsername("user")
            .password("{noop}user")
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(admin, user);
    }
}


