package br.edu.pucgoias.linkhealth.api.error;

public record ValidationIssue(String field, String message) {
}
