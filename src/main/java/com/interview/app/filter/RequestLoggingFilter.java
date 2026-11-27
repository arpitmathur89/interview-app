package com.interview.app.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Writes one log line per API request, for example:
 * <pre>
 *   http : method=POST path=/api/items status=201 ms=12
 * </pre>
 *
 * <p>A <b>filter</b> wraps around every HTTP request <i>before</i> it reaches Spring MVC and the controller,
 * and sees the response on the way back out. That makes it the right place for things every request needs,
 * such as logging, timing, request ids or auth checks, without repeating code in each controller method.
 * <pre>
 *   request --> RequestLoggingFilter --> DispatcherServlet --> ItemController --> ...
 *   response <-- (logs status + time) <-------------------------------------------
 * </pre>
 *
 * <p>{@code @Component} makes it a Spring bean; Spring Boot then registers it as a filter automatically.
 * {@code OncePerRequestFilter} guarantees it runs exactly once per request, even when a request is forwarded
 * internally (for example to an error page).
 *
 * <p>Request and response <b>bodies are never logged</b>: in a real verification system they would contain
 * personal data (SSNs, bank account numbers) that must not end up in log files.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    /** Named "http" so these lines are easy to spot (and filter) in the terminal output. */
    private static final Logger log = LoggerFactory.getLogger("http");

    /**
     * Skip anything that isn't an API call ({@code index.html}, the H2 console, favicon...) to keep the log readable.
     * Returning {@code true} means "don't run this filter for this request".
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    /**
     * Starts a timer, lets the request continue ({@code chain.doFilter} runs the rest of the app, including
     * the controller), then logs the result. The logging is in {@code finally} so it runs even if the request
     * throws an exception.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000; // nanoseconds to milliseconds
            String query = req.getQueryString() == null ? "" : "?" + req.getQueryString();
            log.info("method={} path={}{} status={} ms={}", req.getMethod(), req.getRequestURI(), query,
                    res.getStatus(), ms);
        }
    }
}
