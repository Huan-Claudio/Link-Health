package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.adapter.RegistroDiaAdapter;

/** Figma: "Registro diário do paciente" – água e refeições feitas por dia. */
public class RegistroDiarioActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        Paciente paciente = pacienteDaIntent();
        if (paciente == null) {
            fecharPacienteNaoEncontrado();
            return;
        }
        ActivityListaSimplesBinding binding = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, R.string.registro_diario);

        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setHasFixedSize(true);
        binding.lista.setAdapter(new RegistroDiaAdapter(paciente.getRegistros()));
    }
}
