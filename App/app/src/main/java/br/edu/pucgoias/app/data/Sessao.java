package br.edu.pucgoias.app.data;

import br.edu.pucgoias.app.model.Perfil;

/**
 * Guarda quem está logado enquanto o app está aberto.
 * TODO: substituir pela autenticação real (API + MySQL) quando o back-end estiver pronto.
 */
public final class Sessao {

    private static Perfil perfil = Perfil.PACIENTE;
    private static String nomeUsuario = MockData.NOME_PACIENTE_LOGADO;

    private Sessao() {
    }

    public static void entrar(Perfil novoPerfil) {
        perfil = novoPerfil;
        nomeUsuario = novoPerfil == Perfil.NUTRICIONISTA
                ? MockData.NOME_NUTRICIONISTA
                : MockData.getPacienteLogado().getNome();
    }

    public static void sair() {
        perfil = Perfil.PACIENTE;
        nomeUsuario = MockData.NOME_PACIENTE_LOGADO;
    }

    public static Perfil getPerfil() { return perfil; }
    public static String getNomeUsuario() { return nomeUsuario; }
    public static void setNomeUsuario(String nome) { nomeUsuario = nome; }
}
