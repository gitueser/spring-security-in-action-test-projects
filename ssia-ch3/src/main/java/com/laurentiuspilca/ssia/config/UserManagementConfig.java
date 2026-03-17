package com.laurentiuspilca.ssia.config;

import com.laurentiuspilca.ssia.models.DummyUser;
import com.laurentiuspilca.ssia.service.InMemoryUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.List;

@Configuration
public class UserManagementConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails dummyUser = new DummyUser("dummy", "111", "read");
        List<UserDetails> dummyUsers = List.of(dummyUser);

        return new InMemoryUserDetailsService(dummyUsers);
    }

//    @Bean
//    public UserDetailsService userDetailsService() {
//        var userDetailsService = new InMemoryUserDetailsManager();
//
//        var user = User.withUsername("john")
//                .password("12345")
//                .authorities("read")
//                .build();
//
//        userDetailsService.createUser(user);
//        return userDetailsService;
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
