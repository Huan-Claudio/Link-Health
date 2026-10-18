package br.edu.pucgoias.app.data;

import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.data.remote.AccountSession;

import java.util.UUID;

/**
 * Guarda quem está logado enquanto o app está aberto.
 * Se o Android encerrar o processo, "logado" volta a ser false e a BaseActivity manda para o login.
 * TODO: substituir pela autenticação real (API + banco) quando o back-end estiver pronto.
 */
public final class Sessao {

    private static boolean logado;
    private static Perfil perfil = Perfil.PACIENTE;
    private static String nomeUsuario = MockData.NOME_PACIENTE_LOGADO;
    private static UUID usuarioId;
    private static String accessToken = "";

    private Sessao() {
    }

    public static void entrar(Perfil novoPerfil) {
        logado = true;
        usuarioId = null;
        accessToken = "";
        perfil = novoPerfil;
        nomeUsuario = novoPerfil == Perfil.NUTRICIONISTA
                ? MockData.NOME_NUTRICIONISTA
                : MockData.getPacienteLogado().getNome();
    }

    /** Guarda a identidade retornada pela API para que a lista de compras use o paciente correto. */
    public static void entrar(AccountSession conta) {
        logado = true;
        usuarioId = conta.getId();
        perfil = conta.getPerfil();
        nomeUsuario = conta.getNome();
        accessToken = conta.getAccessToken();
    }

    public static void sair() {
        logado = false;
        perfil = Perfil.PACIENTE;
        nomeUsuario = MockData.NOME_PACIENTE_LOGADO;
        usuarioId = null;
        accessToken = "";
    }

    public static boolean isLogado() { return logado; }
    public static boolean temSessaoRemota() { return usuarioId != null && !accessToken.isEmpty(); }
    public static UUID getUsuarioId() { return usuarioId; }
    public static String getAccessToken() { return accessToken; }
    public static Perfil getPerfil() { return perfil; }
    public static String getNomeUsuario() { return nomeUsuario; }
    public static void setNomeUsuario(String nome) { nomeUsuario = nome; }
}
