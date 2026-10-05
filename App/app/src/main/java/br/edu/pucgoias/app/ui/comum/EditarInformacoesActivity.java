package br.edu.pucgoias.app.ui.comum;

import android.os.Bundle;
import android.util.Patterns;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityEditarInformacoesBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.Formatador;
import br.edu.pucgoias.app.util.Navegacao;

/**
 * Figma: "Editar Informações Paciente" / "Editar Informações Nutricionista".
 * Se receber o CPF de um paciente (Navegacao.EXTRA_PACIENTE_CPF), edita os dados daquele paciente.
 */
public class EditarInformacoesActivity extends BaseActivity {

    private ActivityEditarInformacoesBinding binding;
    private Paciente pacienteEditado;
    private boolean editandoOutroPaciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;

        editandoOutroPaciente = getIntent().hasExtra(Navegacao.EXTRA_PACIENTE_CPF);
        if (editandoOutroPaciente) {
            pacienteEditado = pacienteDaIntent();
            if (pacienteEditado == null) {
                fecharPacienteNaoEncontrado();
                return;
            }
        } else if (Sessao.getPerfil() == Perfil.PACIENTE) {
            pacienteEditado = MockData.getPacienteLogado();
        }

        binding = ActivityEditarInformacoesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, R.string.editar_informacoes);
        Formatador.configurarCampoData(this, binding.etDataNascimento);

        // Só preenche na primeira abertura; ao girar a tela o Android restaura o que foi digitado.
        if (savedInstanceState == null) {
            if (pacienteEditado != null) {
                binding.etNome.setText(pacienteEditado.getNome());
                binding.etEmail.setText(pacienteEditado.getEmail());
                binding.etTelefone.setText(pacienteEditado.getTelefone());
                binding.etDataNascimento.setText(pacienteEditado.getDataNascimento());
            } else {
                binding.etNome.setText(Sessao.getNomeUsuario());
            }
        }

        binding.btnSalvar.setOnClickListener(v -> salvar());
    }

    private void salvar() {
        String nome = binding.etNome.getText().toString().trim();
        if (nome.isEmpty()) {
            binding.etNome.setError(getString(R.string.campo_obrigatorio));
            return;
        }
        String email = binding.etEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError(getString(R.string.email_invalido));
            return;
        }

        // TODO: enviar as alterações para o back-end.
        if (pacienteEditado != null) {
            pacienteEditado.setNome(nome);
            pacienteEditado.setEmail(email);
            pacienteEditado.setTelefone(binding.etTelefone.getText().toString().trim());
            pacienteEditado.setDataNascimento(binding.etDataNascimento.getText().toString().trim());
        }
        if (!editandoOutroPaciente) {
            Sessao.setNomeUsuario(nome);
        }
        toast(R.string.informacoes_salvas);
        finish();
    }
}
