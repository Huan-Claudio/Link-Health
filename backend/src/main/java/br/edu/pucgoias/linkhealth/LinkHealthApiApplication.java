package br.edu.pucgoias.linkhealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API REST do Link Health.
 * Responsável: Felipe Araújo Lemos.
 */
@SpringBootApplication
public class LinkHealthApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkHealthApiApplication.class, args);
    }
}
