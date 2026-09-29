package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.ItemCompra;
import br.edu.pucgoias.app.model.Paciente;

/** Figma: "Editar Lista de Compras" (nutricionista) – remover e adicionar itens. */
public class EditarListaComprasActivity extends BaseActivity {

    private Paciente paciente;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);

        paciente = MockData.getPaciente(getIntent().getIntExtra(MockData.EXTRA_PACIENTE, 0));
        configurarHeader(getString(R.string.lista_compras_de, paciente.getNome()));
        container = findViewById(R.id.container);
        montarLista();
    }

    private void montarLista() {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout card = (LinearLayout) inflater.inflate(R.layout.view_card_container, container, false);

        for (ItemCompra item : paciente.getListaCompras()) {
            View linha = inflater.inflate(R.layout.item_compra, card, false);
            ((TextView) linha.findViewById(R.id.tvDescricao)).setText(item.getDescricao());
            ImageView ivAcao = linha.findViewById(R.id.ivAcao);
            ivAcao.setImageResource(R.drawable.ic_delete);
            ivAcao.setImageTintList(getColorStateList(R.color.lh_text_secondary));
            ivAcao.setPadding(2, 2, 2, 2);
            ivAcao.setContentDescription(getString(R.string.menu_editar_lista));
            ivAcao.setOnClickListener(v -> {
                paciente.getListaCompras().remove(item);
                montarLista();
            });
            // Toque no item para editar o texto.
            linha.setOnClickListener(v -> pedirTexto(getString(R.string.novo_item),
                    getString(R.string.hint_item), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                    item.getDescricao(), texto -> {
                        item.setDescricao(texto);
                        montarLista();
                    }));
            card.addView(linha);
        }

        MaterialButton btnAdicionar = (MaterialButton) inflater.inflate(
                R.layout.view_botao_adicionar, card, false);
        btnAdicionar.setText(R.string.adicionar_item_lista);
        btnAdicionar.setOnClickListener(v -> pedirTexto(getString(R.string.novo_item),
                getString(R.string.hint_item), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                null, texto -> {
                    paciente.getListaCompras().add(new ItemCompra(texto));
                    montarLista();
                }));
        card.addView(btnAdicionar);

        container.addView(card);
    }
}
