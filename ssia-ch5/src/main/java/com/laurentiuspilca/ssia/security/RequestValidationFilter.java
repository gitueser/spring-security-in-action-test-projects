package com.laurentiuspilca.ssia.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class RequestValidationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {
        var httpRequest = (HttpServletRequest) request;
        System.out.println("MY RequestValidationFilter httpRequest is: " + httpRequest.toString());
        var httpResponse = (HttpServletResponse) response;
        System.out.println("MY RequestValidationFilter httpResponse is: " + httpResponse.toString());

        String requestId = httpRequest.getHeader("Request-Id");
        System.out.println("My RequestValidationFilter requestId is: " + requestId);
        if (requestId == null || requestId.isBlank()) {
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
