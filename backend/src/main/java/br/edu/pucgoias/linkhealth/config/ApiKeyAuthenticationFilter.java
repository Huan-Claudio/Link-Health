package br.edu.pucgoias.linkhealth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica o cliente pela chave enviada no cabeçalho X-API-Key.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private final byte[] expectedApiKey;

    public ApiKeyAuthenticationFilter(LinkHealthSecurityProperties properties) {
        this.expectedApiKey = properties.getApiKey().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && MessageDigest.isEqual(expectedApiKey, apiKey.getBytes(StandardCharsets.UTF_8))) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    "link-health-client",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))));
            SecurityContextHolder.setContext(context);
        }

        filterChain.doFilter(request, response);
    }
}
