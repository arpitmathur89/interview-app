package com.interview.app;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** One line per API request: method, path, status, latency. Never logs bodies (PII). */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            String query = req.getQueryString() == null ? "" : "?" + req.getQueryString();
            log.info("method={} path={}{} status={} ms={}", req.getMethod(), req.getRequestURI(), query,
                    res.getStatus(), ms);
        }
    }
}
