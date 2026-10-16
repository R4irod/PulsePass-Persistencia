package com.pulsepass.controller;

import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtProperties;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;

import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Seguridad REAL (SecurityConfig, filtro JWT, handlers) para los @WebMvcTest de controllers.
 * Solo se simula el acceso a base de datos (CustomUserDetailsService).
 */
@TestConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
public class ControllerTestSecurityConfig {

    @Bean
    CustomUserDetailsService customUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }

    // springSecurity() activa la cadena de filtros reales y hace que @WithMockUser
    // llegue a cada request.
    @Bean
    MockMvc mockMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
}
