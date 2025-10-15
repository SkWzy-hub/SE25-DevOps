package com.SE2025BackEnd_16.project.config;

import com.SE2025BackEnd_16.project.filter.JwtAuthenticationTokenFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@AllArgsConstructor
public class SecurityConfig {

    private UserDetailsService userDetailsService;
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                // CORS配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 无状态会话
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 授权配置
                .authorizeHttpRequests((authorize) -> {
                    // 允许匿名访问登录和注册
                    authorize.requestMatchers("/api/login").permitAll();
                    authorize.requestMatchers("/api/register").permitAll();
                    authorize.requestMatchers("/api/register/**").permitAll();
                    authorize.requestMatchers("/api/auth/**").permitAll();

                    // 允许忘记密码相关端点
                    authorize.requestMatchers("/api/forgot-password/**").permitAll();

                    // 允许页面关闭时的自动登出（无法携带Authorization header）
                    authorize.requestMatchers("/api/logout-on-close").permitAll();

                    // 允许匿名访问商品相关接口（浏览商品）
                    authorize.requestMatchers("/api/items/**").permitAll();
                    authorize.requestMatchers("/api/categories/**").permitAll();

                    // 允许前端页面访问
                    authorize.requestMatchers("/", "/login", "/register").permitAll();
                    authorize.requestMatchers("/static/**", "/assets/**").permitAll();

                    // 其他API需要认证
                    authorize.requestMatchers("/api/**").authenticated();

                    // 其他请求允许
                    authorize.anyRequest().permitAll();
                })
                // 添加您的JWT过滤器到Spring Security过滤器链
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许本地开发常见端口和通配符，生产环境请收紧
        // 简化配置：允许所有来源（开发/测试环境适用）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        // 允许所有请求方法
        configuration.setAllowedMethods(Arrays.asList("*"));
        // 允许所有请求头（包括JWT的Authorization等）
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 允许携带Cookie（登录状态需要）
        configuration.setAllowCredentials(true);
        // 预检请求有效期（1小时，减少重复预检）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 对所有接口生效
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
