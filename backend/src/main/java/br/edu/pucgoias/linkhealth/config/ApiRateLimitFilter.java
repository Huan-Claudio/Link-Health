package br.edu.pucgoias.linkhealth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite simples por endereço IP para reduzir tentativas automatizadas.
 *
 * <p>Em uma implantação com várias instâncias, esse controle deve ser movido
 * para um serviço compartilhado ou para o gateway da infraestrutura.</p>
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private final int maximumRequests;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public ApiRateLimitFilter(int maximumRequests, ObjectMapper objectMapper) {
        this.maximumRequests = maximumRequests;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String clientAddress = request.getRemoteAddr();
        WindowCounter counter = counters.compute(clientAddress, (ignored, current) -> {
            if (current == null || now - current.startedAtMillis() >= WINDOW_MILLIS) {
                return new WindowCounter(now, 1);
            }
            return new WindowCounter(current.startedAtMillis(), current.requestCount() + 1);
        });

        if (counters.size() > 10_000) {
            counters.entrySet().removeIf(entry -> now - entry.getValue().startedAtMillis() >= WINDOW_MILLIS);
        }
        if (counter.requestCount() > maximumRequests) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", HttpServletResponse.SC_TOO_MANY_REQUESTS,
                    "code", "RATE_LIMITED",
                    "message", "Muitas requisições. Tente novamente em breve.",
                    "path", request.getRequestURI()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private record WindowCounter(long startedAtMillis, int requestCount) {
    }
}
