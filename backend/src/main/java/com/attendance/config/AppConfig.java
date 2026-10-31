package com.attendance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** Injectable clock so date rules (future dates, edit window) are testable. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OpenAPI openAPI() {
        String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Online Student Attendance System API")
                        .version("1.0.0")
                        .description("Log in via POST /api/auth/login, then click 'Authorize' and paste the token."))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
