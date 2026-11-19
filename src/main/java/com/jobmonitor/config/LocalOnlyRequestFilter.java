package com.jobmonitor.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The app has no login, so it protects itself in two ways:
 * <ul>
 *   <li>It only answers requests addressed to 127.0.0.1 or localhost. This blocks
 *       "DNS rebinding", where a web page points its own domain at your computer.</li>
 *   <li>Anything that changes data (POST and similar) must come from the app's own
 *       pages. A web page on another site can't submit forms to it.</li>
 * </ul>
 */
@Component
public class LocalOnlyRequestFilter extends OncePerRequestFilter {

    private static final Set<String> LOCAL_HOSTS = Set.of("127.0.0.1", "localhost");
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isLocalHost(request.getServerName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "job-monitor only answers at http://127.0.0.1 or http://localhost.");
            return;
        }
        if (!SAFE_METHODS.contains(request.getMethod()) && comesFromAnotherSite(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Changes can only be made from job-monitor's own pages.");
            return;
        }
        chain.doFilter(request, response);
    }

    static boolean isLocalHost(String host) {
        return host != null && LOCAL_HOSTS.contains(host.toLowerCase(Locale.ROOT));
    }

    private static boolean comesFromAnotherSite(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            if ("null".equals(origin)) {
                return true;
            }
            try {
                URI uri = URI.create(origin);
                return !isLocalHost(uri.getHost()) || uri.getPort() != request.getServerPort();
            } catch (IllegalArgumentException e) {
                return true;
            }
        }
        // No Origin header: fall back to the browser's Sec-Fetch-Site hint.
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        return "cross-site".equalsIgnoreCase(fetchSite) || "same-site".equalsIgnoreCase(fetchSite);
    }
}
