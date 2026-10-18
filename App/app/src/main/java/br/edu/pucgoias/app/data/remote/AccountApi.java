package br.edu.pucgoias.app.data.remote;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

import br.edu.pucgoias.app.model.Perfil;

/** Cadastro e login usados como apoio para identificar com segurança o dono da lista de compras. */
public final class AccountApi {

    private static final DateTimeFormatter DATA_TELA = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.US);

    public interface Callback {
        void onSuccess(AccountSession session);
        void onError(String message);
    }

    public static final class RegisterData {
        private final String nome;
        private final String dataNascimento;
        private final String email;
        private final String telefone;
        private final String documento;
        private final String senha;
        private final Perfil perfil;

        public RegisterData(String nome, String dataNascimento, String email, String telefone,
                            String documento, String senha, Perfil perfil) {
            this.nome = nome;
            this.dataNascimento = dataNascimento;
            this.email = email;
            this.telefone = telefone;
            this.documento = documento;
            this.senha = senha;
            this.perfil = perfil;
        }
    }

    public void login(String email, String senha, Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", senha);
            BackendApi.post("/api/v1/auth/mobile-session", body, null, new BackendApi.Callback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        callback.onSuccess(parseSession(response));
                    } catch (JSONException | IllegalArgumentException exception) {
                        callback.onError("A resposta do servidor para o login é inválida.");
                    }
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        } catch (JSONException exception) {
            callback.onError("Não foi possível preparar o login.");
        }
    }

    public void register(RegisterData data, Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("fullName", data.nome);
            body.put("email", data.email);
            body.put("document", data.documento);
            body.put("password", data.senha);
            body.put("role", data.perfil.name());
            if (!blank(data.telefone)) body.put("phone", data.telefone);
            if (!blank(data.dataNascimento)) {
                body.put("birthDate", LocalDate.parse(data.dataNascimento.trim(), DATA_TELA).toString());
            }
            BackendApi.post("/api/v1/auth/register", body, null, new BackendApi.Callback() {
                @Override
                public void onSuccess(String ignored) {
                    // O servidor criou a conta; a sessão devolvida abaixo identifica o paciente no app.
                    login(data.email, data.senha, callback);
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        } catch (DateTimeParseException exception) {
            callback.onError("Informe a data no formato dd/mm/aaaa.");
        } catch (JSONException exception) {
            callback.onError("Não foi possível preparar o cadastro.");
        }
    }

    private static AccountSession parseSession(String response) throws JSONException {
        JSONObject json = new JSONObject(response);
        return new AccountSession(
                UUID.fromString(json.getString("id")),
                json.getString("fullName"),
                json.getString("email"),
                Perfil.valueOf(json.getString("role")),
                json.getString("accessToken"));
    }

    private static boolean blank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
