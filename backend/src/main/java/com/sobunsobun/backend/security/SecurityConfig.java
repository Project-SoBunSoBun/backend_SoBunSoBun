package com.sobunsobun.backend.security;

<<<<<<< Updated upstream
=======
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
>>>>>>> Stashed changes
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(); // 사용자 없음
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var jwtFilter = new JwtAuthenticationFilter(jwt, users);

        http
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.disable()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new RestAuthEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/health",
                                "/auth/**",
                                "/users/check-nickname"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(f -> f.disable())
                .httpBasic(Customizer.withDefaults()); // 필요 시 .disable()

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
