package br.edu.pucgoias.linkhealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Ponto de entrada da API REST do Link Health.
 * Responsável: Felipe Araújo Lemos.
 */
// A API usa filtros próprios (API key e token móvel), sem login padrão do Spring Security.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LinkHealthApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkHealthApiApplication.class, args);
    }
}
