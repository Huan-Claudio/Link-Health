package br.edu.pucgoias.app.data;

import br.edu.pucgoias.app.model.Perfil;

/**
 * Guarda quem está logado enquanto o app está aberto.
 * Se o Android encerrar o processo, "logado" volta a ser false e a BaseActivity manda para o login.
 * TODO: substituir pela autenticação real (API + banco) quando o back-end estiver pronto.
 */
public final class Sessao {

    private static boolean logado;
    private static Perfil perfil = Perfil.PACIENTE;
    private static String nomeUsuario = MockData.NOME_PACIENTE_LOGADO;

    private Sessao() {
    }

    public static void entrar(Perfil novoPerfil) {
        logado = true;
        perfil = novoPerfil;
        nomeUsuario = novoPerfil == Perfil.NUTRICIONISTA
                ? MockData.NOME_NUTRICIONISTA
                : MockData.getPacienteLogado().getNome();
    }

    public static void sair() {
        logado = false;
        perfil = Perfil.PACIENTE;
        nomeUsuario = MockData.NOME_PACIENTE_LOGADO;
    }

    public static boolean isLogado() { return logado; }
    public static Perfil getPerfil() { return perfil; }
    public static String getNomeUsuario() { return nomeUsuario; }
    public static void setNomeUsuario(String nome) { nomeUsuario = nome; }
}
