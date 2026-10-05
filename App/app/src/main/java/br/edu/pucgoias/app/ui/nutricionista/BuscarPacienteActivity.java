package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Collections;
import java.util.List;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityBuscarPacienteBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.adapter.BuscaPacienteAdapter;
import br.edu.pucgoias.app.ui.nutricionista.edicao.EdicaoViewModel;
import br.edu.pucgoias.app.util.TextoAlterado;

/** Figma: "Dietitian dashboard" (nutricionista) – busca paciente por CPF/e-mail e envia convite. */
public class BuscarPacienteActivity extends BaseActivity {

    private ActivityBuscarPacienteBinding binding;
    private BuscaPacienteAdapter adapter;
    private EdicaoViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        binding = ActivityBuscarPacienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Os convites enviados ficam no ViewModel e continuam marcados ao girar a tela.
        viewModel = new ViewModelProvider(this).get(EdicaoViewModel.class);

        binding.headerSaudacao.tvAvatar.setBackgroundResource(R.drawable.bg_circle_gray);
        binding.headerSaudacao.tvAvatar.setText("");
        binding.headerSaudacao.tvOla.setText(getString(R.string.ola_nome, Sessao.getNomeUsuario()));
        binding.headerSaudacao.tvSubtitulo.setVisibility(View.VISIBLE);
        binding.headerSaudacao.tvSubtitulo.setText(
                getString(R.string.pacientes_ativos, MockData.getPacientes().size()));
        binding.headerSaudacao.btnVoltar.setOnClickListener(v -> finish());

        adapter = new BuscaPacienteAdapter(viewModel.convidados, this::convidar);
        binding.listaResultados.setLayoutManager(new LinearLayoutManager(this));
        binding.listaResultados.setHasFixedSize(true);
        binding.listaResultados.setAdapter(adapter);

        binding.etBusca.addTextChangedListener(new TextoAlterado(texto -> buscar()));
        buscar();
    }

    /** Lista os pacientes cujo CPF ou e-mail começa com o texto digitado. */
    private void buscar() {
        String busca = binding.etBusca.getText().toString().trim();
        if (busca.isEmpty()) {
            mostrar(Collections.emptyList(), getString(R.string.buscar_paciente_instrucao));
            return;
        }
        // TODO: refazer a busca no back-end (consultar os pacientes por CPF/e-mail no banco).
        //       Por enquanto a busca é feita nos dados de exemplo do MockData.
        List<Paciente> encontrados = MockData.buscarPacientesPorCpfOuEmail(busca);
        mostrar(encontrados, encontrados.isEmpty() ? getString(R.string.nenhum_paciente) : null);
    }

    private void mostrar(List<Paciente> pacientes, String mensagem) {
        adapter.atualizar(pacientes);
        binding.tvVazio.setText(mensagem);
        binding.tvVazio.setVisibility(mensagem == null ? View.GONE : View.VISIBLE);
    }

    private void convidar(Paciente paciente, int posicao) {
        // TODO: enviar o convite pelo back-end.
        viewModel.convidados.add(paciente.getCpf());
        toast(getString(R.string.convite_enviado, paciente.getNome()));
        adapter.notifyItemChanged(posicao);
    }
}
