package br.edu.pucgoias.app.ui.comum;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityEvolucaoCorporalBinding;
import br.edu.pucgoias.app.databinding.DialogImagemBinding;
import br.edu.pucgoias.app.model.FotoEvolucao;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.adapter.FotoEvolucaoAdapter;
import br.edu.pucgoias.app.util.Formatador;
import br.edu.pucgoias.app.util.Navegacao;

/**
 * Figma: "Linha do tempo de evolução".
 * Paciente: pode tirar foto com a câmera. Nutricionista: apenas visualiza o histórico.
 */
public class EvolucaoCorporalActivity extends BaseActivity {

    private ActivityEvolucaoCorporalBinding binding;
    private Paciente paciente;
    private FotoEvolucaoAdapter adapter;

    /** Abre o app de câmera nativo e devolve uma miniatura da foto. */
    private final ActivityResultLauncher<Void> tirarFoto =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), this::onFotoTirada);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;

        // Nutricionista abre a evolução de um paciente (CPF na Intent); o paciente abre a própria.
        if (getIntent().hasExtra(Navegacao.EXTRA_PACIENTE_CPF)) {
            paciente = pacienteDaIntent();
        } else if (Sessao.getPerfil() == Perfil.PACIENTE) {
            paciente = MockData.getPacienteLogado();
        }
        if (paciente == null) {
            fecharPacienteNaoEncontrado();
            return;
        }

        binding = ActivityEvolucaoCorporalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, R.string.evolucao_corporal);

        if (Sessao.getPerfil() == Perfil.PACIENTE) {
            binding.btnTirarFoto.setOnClickListener(v -> abrirCamera());
        } else {
            binding.btnTirarFoto.setVisibility(View.GONE);
        }

        adapter = new FotoEvolucaoAdapter(paciente.getFotos(), this::abrirImagem);
        binding.listaFotos.setLayoutManager(new GridLayoutManager(this, 2));
        binding.listaFotos.setHasFixedSize(true);
        binding.listaFotos.setAdapter(adapter);
    }

    private void abrirCamera() {
        try {
            tirarFoto.launch(null);
        } catch (ActivityNotFoundException e) {
            toast(R.string.camera_indisponivel);
        }
    }

    private void onFotoTirada(Bitmap bitmap) {
        if (bitmap == null || binding == null) return;
        pedirTexto(getString(R.string.informe_peso), "70",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, null, texto -> {
                    double peso = Formatador.lerNumero(texto);
                    if (peso <= 0) return;
                    paciente.getFotos().add(0, new FotoEvolucao(Formatador.hoje(), peso, bitmap));
                    adapter.notifyItemInserted(0);
                    binding.listaFotos.scrollToPosition(0);
                });
    }

    /** Mostra a foto ampliada (segundo frame "Linha do tempo de evolução" do Figma). */
    private void abrirImagem(FotoEvolucao foto) {
        DialogImagemBinding dialogBinding = DialogImagemBinding.inflate(getLayoutInflater());
        FotoEvolucaoAdapter.mostrarFoto(dialogBinding.ivImagemGrande, foto);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogBinding.getRoot()).create();
        dialogBinding.btnFechar.setOnClickListener(v -> dialog.dismiss());
        registrarDialog(dialog);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
