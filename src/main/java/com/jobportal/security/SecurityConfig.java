package com.jobportal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf()
                .ignoringAntMatchers("/api/**")
                .and()
            .headers()
                .frameOptions().sameOrigin()
                .and()
            .authorizeRequests()
                // Static resources
                .antMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                // Public view pages
                .antMatchers("/", "/jobs", "/jobs/**", "/companies", "/companies/**", "/about", "/contact", "/login", "/register").permitAll()
                // Public REST APIs
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/jobs/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/companies/**").permitAll()
                // Role-based page access
                .antMatchers("/seeker/**").hasAuthority("ROLE_JOB_SEEKER")
                .antMatchers("/recruiter/**").hasAuthority("ROLE_RECRUITER")
                .antMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                // Role-based API access
                .antMatchers("/api/saved-jobs/**").hasAuthority("ROLE_JOB_SEEKER")
                .antMatchers(HttpMethod.POST, "/api/applications").hasAuthority("ROLE_JOB_SEEKER")
                .antMatchers("/api/applications/my").hasAuthority("ROLE_JOB_SEEKER")
                .antMatchers("/api/resumes/upload").hasAuthority("ROLE_JOB_SEEKER")
                .antMatchers("/api/resumes/download/**").authenticated()
                .antMatchers("/api/recruiter/**").hasAuthority("ROLE_RECRUITER")
                .antMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .antMatchers("/api/profile/**").authenticated()
                // Any other request
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
                .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll();

        return http.build();
    }
}
