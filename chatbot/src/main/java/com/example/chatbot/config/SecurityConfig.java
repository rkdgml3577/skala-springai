package com.example.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인증과 인가.
 *
 * 이 파일이 생기는 순간 사용자가 한 명("dev")에서 여러 명이 된다.
 * 그 말은 대화와 사용량이 사용자별로 갈라져야 한다는 뜻이기도 하다 —
 * 인증만 붙이고 그 분리를 빼먹으면 "로그인은 되는데 남의 대화가 보이는" 상태가 된다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // 화면 자체는 누구나 받을 수 있다. 정작 막아야 할 것은 그 아래 API 다.
                        .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()

                        // 프로메테우스가 컨테이너 네트워크 안에서 긁어 간다.
                        // 여기를 막으면 모니터링이 통째로 멈춘다.
                        // 다만 이건 "안에서만 닿는다" 는 가정 위에 서 있다 —
                        // 운영에서는 management.server.port 를 따로 두고
                        // 그 포트를 바깥에 열지 않는 쪽이 맞다.
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()

                        .anyRequest().authenticated())

                // 브라우저 세션 쿠키가 아니라 요청마다 Authorization 헤더로 오는 API 다.
                // CSRF 는 쿠키를 자동으로 실어 보내는 것을 노리는 공격이므로 여기서는 대상이 아니다.
                .csrf(csrf -> csrf.disable())

                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 실습용 사용자.
     *
     * 비밀번호가 코드에 박혀 있다 — 실제 서비스라면 절대 이렇게 두지 않는다.
     * 여기서 확인하려는 것은 "사용자가 둘일 때 서로의 대화가 보이지 않는가" 하나뿐이다.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("alice").password(encoder.encode("alice-pw")).roles("USER").build(),
                User.withUsername("bob").password(encoder.encode("bob-pw")).roles("USER").build());
    }
}
