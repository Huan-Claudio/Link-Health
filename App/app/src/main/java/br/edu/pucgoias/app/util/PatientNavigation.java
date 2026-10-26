package br.edu.pucgoias.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import java.util.UUID;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Perfil;

/** Identidade real usada pelas telas conectadas. Não converte CPF de demonstração em conta. */
public final class PatientNavigation {
    public static final String EXTRA_PATIENT_ID = "patient_id";
    public static final String EXTRA_PATIENT_NAME = "patient_name";
    private PatientNavigation() { }

    public static UUID patientId(Intent intent) {
        if (!Sessao.temSessaoRemota()) return null;
        if (Sessao.getPerfil() == Perfil.PACIENTE) return Sessao.getUsuarioId();
        try { return UUID.fromString(intent.getStringExtra(EXTRA_PATIENT_ID)); }
        catch (RuntimeException exception) { return null; }
    }

    public static String name(Intent intent) {
        if (Sessao.getPerfil() == Perfil.PACIENTE) return Sessao.getNomeUsuario();
        String name = intent.getStringExtra(EXTRA_PATIENT_NAME);
        return name == null ? "Paciente" : name;
    }

    public static Intent intent(Context context, Class<? extends Activity> screen, UUID id, String name) {
        return new Intent(context, screen)
                .putExtra(EXTRA_PATIENT_ID, id.toString())
                .putExtra(EXTRA_PATIENT_NAME, name)
                .putExtra("recommendation_patient_id", id.toString())
                .putExtra("recommendation_patient_name", name);
    }
}
