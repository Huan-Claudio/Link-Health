package br.edu.pucgoias.app.data.remote;

import java.util.UUID;

import br.edu.pucgoias.app.model.Perfil;

/** Dados da sessão temporária devolvidos pela API, mantidos apenas enquanto o app está aberto. */
public final class AccountSession {

    private final UUID id;
    private final String nome;
    private final String email;
    private final Perfil perfil;
    private final String accessToken;

    public AccountSession(UUID id, String nome, String email, Perfil perfil, String accessToken) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.accessToken = accessToken;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public Perfil getPerfil() { return perfil; }
    public String getAccessToken() { return accessToken; }
}
