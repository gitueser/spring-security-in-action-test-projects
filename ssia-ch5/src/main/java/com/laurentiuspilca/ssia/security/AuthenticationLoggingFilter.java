package com.laurentiuspilca.ssia.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

public class AuthenticationLoggingFilter implements Filter {

    private final Logger logger = Logger.getLogger(AuthenticationLoggingFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {
        var httpRequest = (HttpServletRequest) request;
        System.out.println("MY AuthenticationLoggingFilter httpRequest is: " + httpRequest.toString());
        String requestId = httpRequest.getHeader("Request-Id");
        System.out.println("My AuthenticationLoggingFilter requestId is: " + requestId);

        logger.info(String.format("Successfully authenticated request with id %s", requestId));
        filterChain.doFilter(request, response);
    }
}
