package br.edu.pucgoias.app.ui.comum;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.FotoEvolucao;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.Formatador;

/**
 * Figma: "Linha do tempo de evolução".
 * Paciente: pode tirar foto com a câmera. Nutricionista: apenas visualiza o histórico.
 */
public class EvolucaoCorporalActivity extends BaseActivity {

    private Paciente paciente;
    private LinearLayout containerFotos;

    /** Abre o app de câmera nativo e devolve uma miniatura da foto. */
    private final ActivityResultLauncher<Void> tirarFoto =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), this::onFotoTirada);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evolucao_corporal);
        configurarHeader(R.string.evolucao_corporal);

        paciente = MockData.getPaciente(getIntent().getIntExtra(MockData.EXTRA_PACIENTE, -1));
        containerFotos = findViewById(R.id.containerFotos);

        View btnTirarFoto = findViewById(R.id.btnTirarFoto);
        if (Sessao.getPerfil() == Perfil.PACIENTE) {
            btnTirarFoto.setOnClickListener(v -> {
                try {
                    tirarFoto.launch(null);
                } catch (ActivityNotFoundException e) {
                    toast(R.string.camera_indisponivel);
                }
            });
        } else {
            btnTirarFoto.setVisibility(View.GONE);
        }

        montarGrade();
    }

    private void onFotoTirada(Bitmap bitmap) {
        if (bitmap == null) return;
        pedirTexto(getString(R.string.informe_peso), "70",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, null, texto -> {
                    double peso = Formatador.lerNumero(texto);
                    if (peso <= 0) return;
                    paciente.getFotos().add(0, new FotoEvolucao(Formatador.hoje(), peso, bitmap));
                    montarGrade();
                });
    }

    /** Monta a grade de 2 colunas com os cards de foto. */
    private void montarGrade() {
        containerFotos.removeAllViews();
        List<FotoEvolucao> fotos = paciente.getFotos();
        LayoutInflater inflater = LayoutInflater.from(this);
        int espaco = getResources().getDimensionPixelSize(R.dimen.lh_space_m);

        for (int i = 0; i < fotos.size(); i += 2) {
            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lpLinha = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpLinha.bottomMargin = espaco;
            linha.setLayoutParams(lpLinha);

            for (int col = 0; col < 2; col++) {
                int indice = i + col;
                View card = inflater.inflate(R.layout.item_foto_evolucao, linha, false);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
                if (col == 1) lp.setMarginStart(espaco);
                if (indice < fotos.size()) {
                    preencherCard(card, fotos.get(indice));
                } else {
                    card.setVisibility(View.INVISIBLE); // mantém o alinhamento da grade
                }
                linha.addView(card);
            }
            containerFotos.addView(linha);
        }
    }

    private void preencherCard(View card, FotoEvolucao foto) {
        ImageView iv = card.findViewById(R.id.ivFoto);
        mostrarFoto(iv, foto);
        ((TextView) card.findViewById(R.id.tvData)).setText(foto.getData());
        ((TextView) card.findViewById(R.id.tvPeso))
                .setText(getString(R.string.peso_valor, Formatador.numero(foto.getPeso())));
        card.findViewById(R.id.btnAbrirImagem).setOnClickListener(v -> abrirImagem(foto));
    }

    private void mostrarFoto(ImageView iv, FotoEvolucao foto) {
        if (foto.getFoto() != null) {
            iv.setImageBitmap(foto.getFoto());
            iv.setImageTintList(null);
            iv.setPadding(0, 0, 0, 0);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setClipToOutline(true);
        }
    }

    /** Mostra a foto ampliada (segundo frame "Linha do tempo de evolução" do Figma). */
    private void abrirImagem(FotoEvolucao foto) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_imagem, null, false);
        mostrarFoto(view.findViewById(R.id.ivImagemGrande), foto);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(view).create();
        view.findViewById(R.id.btnFechar).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }
}
