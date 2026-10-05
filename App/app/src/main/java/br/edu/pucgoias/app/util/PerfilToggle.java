package br.edu.pucgoias.app.util;

import br.edu.pucgoias.app.databinding.ViewPerfilToggleBinding;
import br.edu.pucgoias.app.model.Perfil;

/** Controla o seletor "Nutricionista | Paciente" (MaterialButtonToggleGroup do layout view_perfil_toggle.xml). */
public final class PerfilToggle {

    public interface Listener {
        void onPerfilSelecionado(Perfil perfil);
    }

    private final ViewPerfilToggleBinding binding;
    private final Listener listener;
    private Perfil perfil;

    public PerfilToggle(ViewPerfilToggleBinding binding, Perfil inicial, Listener listener) {
        this.binding = binding;
        this.listener = listener;
        binding.getRoot().addOnButtonCheckedListener((grupo, idMarcado, marcado) -> {
            if (!marcado) return;
            perfil = idMarcado == binding.btnOpcaoNutricionista.getId()
                    ? Perfil.NUTRICIONISTA : Perfil.PACIENTE;
            if (this.listener != null) this.listener.onPerfilSelecionado(perfil);
        });
        selecionar(inicial);
    }

    public void selecionar(Perfil novo) {
        perfil = novo;
        binding.getRoot().check(novo == Perfil.NUTRICIONISTA
                ? binding.btnOpcaoNutricionista.getId()
                : binding.btnOpcaoPaciente.getId());
    }

    public Perfil getPerfil() {
        return perfil;
    }
}
