package BreakthroughGame.backEnd.UserModule.UserRestful.config.security;

import org.springframework.beans.factory.annotation.Value;                         // 中文备注：读取 yml 配置（可选）
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 中文备注：Swagger / OpenAPI 资源白名单（springdoc 2.x 默认路径）
    private static final String[] SWAGGER_WHITELIST = new String[]{
            "/v3/api-docs/**",         // OpenAPI JSON
            "/v3/api-docs.yaml",       // OpenAPI YAML（可选）
            "/swagger-ui.html",        // 旧入口（会重定向）
            "/swagger-ui/**"           // 静态页面与资源
    };

    /**
     * 中文备注：密码编码器（推荐用 Delegating，生成 {bcrypt} 前缀；并兼容无前缀的旧数据）
     */
    @Bean
    public PasswordEncoder passwordEncoder(@Value("${security.password.bcrypt-strength:10}") int strength) {
        String idForEncode = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(strength);
        encoders.put(idForEncode, bcrypt);

        // 中文备注：encode 产物形如 {bcrypt}$2a$...；匹配无前缀旧数据时也使用 bcrypt
        DelegatingPasswordEncoder dpe = new DelegatingPasswordEncoder(idForEncode, encoders);
        dpe.setDefaultPasswordEncoderForMatches(bcrypt);
        return dpe;
    }

    /**
     * 中文备注：核心安全链
     * - 放行 Swagger 端点与你自己的认证端点（/api/auth/**）
     * - 放行 CORS 预检（OPTIONS）
     * - 其余需要认证（如果你用的是 JWT，可保持无状态会话）
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 中文备注：如果用 JWT，通常关闭 CSRF；如使用表单登录则不要关闭
                .csrf(csrf -> csrf.disable())

                // 中文备注：按需开启/关闭登录方式。若你只用 JWT，可禁用基础认证和表单登录
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())

                // 中文备注：无状态会话（JWT 场景）；如果你用 session，可去掉这行
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 中文备注：鉴权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()          // ✅ Swagger 直接放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // ✅ 预检请求放行（跨域）
                        .requestMatchers("/api/auth/**").permitAll()             // ✅ 你的注册/登录放行
                        .requestMatchers("/api/characters/**").permitAll()             // ✅ 经验值系统放行
                        .requestMatchers("/api/dungeons/import/**").permitAll()             // ✅ 副本导入放行
                        .requestMatchers("/api/dungeons/**").permitAll()             // ✅ 副本查询放行
                        .requestMatchers("/api/dungeonDaily/**").permitAll()             // ✅ 角色副本查询放行
                        .requestMatchers("/api/dungeonRun/**").permitAll()             // ✅ 角色副本日志登记放行
                        .requestMatchers("/api/equipment/**").permitAll()             // ✅ 装备图鉴放行
                        .requestMatchers("/api/definition/equipment/**").permitAll()             // ✅ 装备图鉴放行
                        .requestMatchers("/api/character/equipment/**").permitAll()             // ✅ 角色穿戴装备
                        .requestMatchers("/api/character/**").permitAll()             // ✅ 角色可穿戴装备

                        // .requestMatchers("/actuator/**").permitAll()          // （可选）放行健康检查
                        .anyRequest().authenticated()                            // 其余都需要认证
                )

                // （可选）如果你还有自定义过滤器（如 JWT 认证过滤器），可在此处 addFilterBefore(...)
                .headers(Customizer.withDefaults());

        return http.build();
    }
}
