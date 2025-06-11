package projects.kunal.kamelthinks.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import projects.kunal.kamelthinks.api.repository.UserRepository;
import projects.kunal.kamelthinks.api.security.JwtFilter;

import java.util.ArrayList; // Assuming this is needed for User

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        new ArrayList<>())) // Replace with actual roles/authorities if you have them
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // <-- MOST LIKELY FIX FOR 403 ON POST
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWTs are stateless
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/auth/**").permitAll() // <-- Ensure this allows /api/auth/login
                        .requestMatchers("/api/posts/**").permitAll() // <-- If blog posts are also public (as per your tests)
                        // .anyRequest().authenticated() // For any other endpoint, require authentication
                        .anyRequest().permitAll() // If all your current endpoints are public for now
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // Add your JWT filter
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}