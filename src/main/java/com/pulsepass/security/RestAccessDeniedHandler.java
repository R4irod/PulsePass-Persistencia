package com.pulsepass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 403: identidad válida, pero sin permiso para esta operación. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    
    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.warn("Access denied for {} on {} {}",
                authentication != null ? authentication.getName() : "unknown",
                request.getMethod(), request.getRequestURI());
        SecurityErrorWriter.write(response, HttpStatus.FORBIDDEN,
                "You do not have permission to perform this operation");
    }
}