package com.tuckersoft.branchengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuckersoft.branchengine.dto.Dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse err = new ErrorResponse(
                "UNAUTHORIZED", "Acceso no autorizado", Instant.now().toString(), request.getRequestURI()
        );
        new ObjectMapper().writeValue(response.getOutputStream(), err);
    }
}
