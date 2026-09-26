package br.edu.pucgoias.app.util;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.model.Perfil;

/** Controla o seletor "Nutricionista | Paciente" (layout view_perfil_toggle.xml). */
public class PerfilToggle {

    public interface Listener {
        void onPerfilSelecionado(Perfil perfil);
    }

    private final TextView opcaoNutricionista;
    private final TextView opcaoPaciente;
    private final Listener listener;
    private Perfil perfil;

    public PerfilToggle(View raiz, Perfil inicial, Listener listener) {
        this.opcaoNutricionista = raiz.findViewById(R.id.tvOpcaoNutricionista);
        this.opcaoPaciente = raiz.findViewById(R.id.tvOpcaoPaciente);
        this.listener = listener;
        opcaoNutricionista.setOnClickListener(v -> selecionar(Perfil.NUTRICIONISTA));
        opcaoPaciente.setOnClickListener(v -> selecionar(Perfil.PACIENTE));
        selecionar(inicial);
    }

    public void selecionar(Perfil novo) {
        perfil = novo;
        estilizar(opcaoNutricionista, novo == Perfil.NUTRICIONISTA);
        estilizar(opcaoPaciente, novo == Perfil.PACIENTE);
        if (listener != null) listener.onPerfilSelecionado(novo);
    }

    public Perfil getPerfil() {
        return perfil;
    }

    private void estilizar(TextView tv, boolean selecionado) {
        tv.setBackgroundResource(selecionado ? R.drawable.bg_toggle_selected : 0);
        tv.setTextColor(ContextCompat.getColor(tv.getContext(),
                selecionado ? R.color.white : R.color.lh_text_secondary));
        tv.setTypeface(null, selecionado ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }
}
