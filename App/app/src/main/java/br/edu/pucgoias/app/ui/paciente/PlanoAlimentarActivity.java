package br.edu.pucgoias.app.ui.paciente;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.ui.adapter.RefeicaoPacienteAdapter;

/** Figma: "Meu Plano Alimentar" – o paciente marca as refeições que já fez. */
public class PlanoAlimentarActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        ActivityListaSimplesBinding binding = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, R.string.meu_plano_alimentar);

        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setHasFixedSize(true);
        binding.lista.setAdapter(new RefeicaoPacienteAdapter(MockData.getPacienteLogado().getPlano()));
    }
}
