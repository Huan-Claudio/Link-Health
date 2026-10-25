package br.edu.pucgoias.linkhealth.api.shopping;

import jakarta.validation.constraints.NotNull;

public record PurchaseStatusRequest(
        @NotNull(message = "Informe se o item foi comprado.") Boolean purchased) {
}
