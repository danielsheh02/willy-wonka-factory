package com.example.demo.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.security.jwt.OncePerRequestFilterImpl;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    OncePerRequestFilterImpl oncePerRequestFilterImpl;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/auth/signin",
                                "/api/auth/signup",
                                // Публичные endpoints для золотых билетов и бронирования
                                "/api/tickets/validate/**",
                                "/api/tickets/book",
                                "/api/tickets/*/cancel")
                        .permitAll()
                        
                        // Экскурсии - просмотр для всех, управление только для ADMIN и GUIDE
                        .requestMatchers(HttpMethod.GET, "/api/excursions/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/excursions/**").hasAnyRole("ADMIN", "GUIDE")
                        .requestMatchers(HttpMethod.PUT, "/api/excursions/**").hasAnyRole("ADMIN", "GUIDE")
                        .requestMatchers(HttpMethod.DELETE, "/api/excursions/**").hasAnyRole("ADMIN", "GUIDE")
                        
                        // Золотые билеты - только ADMIN может генерировать
                        .requestMatchers(HttpMethod.POST, "/api/tickets/generate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/**").hasAnyRole("ADMIN", "GUIDE")
                        
                        // Оборудование - создание/редактирование для FOREMAN, ADMIN, MASTER
                        .requestMatchers(HttpMethod.POST, "/api/equipments/**").hasAnyRole("FOREMAN", "ADMIN", "MASTER")
                        .requestMatchers(HttpMethod.PUT, "/api/equipments/**").hasAnyRole("FOREMAN", "ADMIN", "MASTER")
                        .requestMatchers(HttpMethod.DELETE, "/api/equipments/**").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/equipments/**").hasAnyRole("FOREMAN", "WORKER", "ADMIN", "MASTER", "GUIDE")
                        
                        // Цеха - только FOREMAN и ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/workshops/**").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/workshops/**").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/workshops/**").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/workshops/**").hasAnyRole("FOREMAN", "WORKER", "ADMIN", "MASTER", "GUIDE")
                        
                        // Пользователи - только FOREMAN и ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("FOREMAN", "WORKER", "ADMIN", "MASTER", "GUIDE")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAnyRole("FOREMAN", "ADMIN")
                        
                        // Задачи - все могут читать, PUT для всех (проверка владельца на уровне сервиса)
                        .requestMatchers(HttpMethod.POST, "/api/tasks/distribute").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tasks/unassigned").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tasks/**").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/tasks/**").hasAnyRole("FOREMAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tasks/**").hasAnyRole("FOREMAN", "WORKER", "ADMIN", "MASTER", "GUIDE")
                        .requestMatchers(HttpMethod.PUT, "/api/tasks/**").hasAnyRole("FOREMAN", "WORKER", "ADMIN", "MASTER", "GUIDE")
                        
                        // Отчеты - только FOREMAN и ADMIN
                        .requestMatchers("/api/reports/**").hasAnyRole("FOREMAN", "ADMIN")
                        
                        // Остальные GET запросы
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(oncePerRequestFilterImpl, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        // configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean
    static GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }
}
