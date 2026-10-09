package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.ItemCompra;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.util.TextoAlterado;

/**
 * Figma: "Editar Lista de Compras" (nutricionista).
 * Os itens são editados numa cópia (rascunho) e só vão para a lista do paciente
 * ao tocar em "Salvar Alterações".
 */
public class EditarListaComprasActivity extends BaseActivity {

    /** Linha do rascunho: o item original (null se for novo) e o texto digitado. */
    private static class Linha {
        final ItemCompra original;
        String texto;

        Linha(ItemCompra original, String texto) {
            this.original = original;
            this.texto = texto;
        }
    }

    private Paciente paciente;
    private LinearLayout container;
    private final List<Linha> rascunho = new ArrayList<>();
    private EditText ultimoCampo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);

        paciente = MockData.getPaciente(getIntent().getIntExtra(MockData.EXTRA_PACIENTE, 0));
        configurarHeader(getString(R.string.lista_compras_de, paciente.getNome()));
        container = findViewById(R.id.container);
        carregarRascunho();
        montarLista();
    }

    private void carregarRascunho() {
        rascunho.clear();
        for (ItemCompra item : paciente.getListaCompras()) {
            rascunho.add(new Linha(item, item.getDescricao()));
        }
    }

    private void montarLista() {
        container.removeAllViews();
        ultimoCampo = null;
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout card = (LinearLayout) inflater.inflate(R.layout.view_card_container, container, false);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        card.setPadding(padding, padding, padding, padding);

        for (Linha linha : rascunho) {
            View view = inflater.inflate(R.layout.item_alimento_editavel, card, false);
            EditText et = view.findViewById(R.id.etAlimento);
            et.setHint(R.string.hint_item);
            et.setText(linha.texto);
            et.addTextChangedListener(new TextoAlterado(texto -> linha.texto = texto));
            view.findViewById(R.id.btnRemoverAlimento).setOnClickListener(v -> {
                rascunho.remove(linha);
                montarLista();
            });
            card.addView(view);
            ultimoCampo = et;
        }

        View btnAdicionar = inflater.inflate(R.layout.view_botao_adicionar, card, false);
        btnAdicionar.setOnClickListener(v -> {
            rascunho.add(new Linha(null, ""));
            montarLista();
            // Foca o novo campo para digitar direto.
            if (ultimoCampo != null) ultimoCampo.requestFocus();
        });
        card.addView(btnAdicionar);

        View btnSalvar = inflater.inflate(R.layout.view_botao_salvar, card, false);
        btnSalvar.setOnClickListener(v -> salvar());
        card.addView(btnSalvar);

        container.addView(card);
    }

    /** Aplica o rascunho na lista do paciente (itens em branco são descartados). */
    private void salvar() {
        List<ItemCompra> novaLista = new ArrayList<>();
        for (Linha linha : rascunho) {
            String texto = linha.texto.trim();
            if (texto.isEmpty()) continue;
            if (linha.original != null) {
                linha.original.setDescricao(texto); // mantém a marcação "comprado" do paciente
                novaLista.add(linha.original);
            } else {
                novaLista.add(new ItemCompra(texto));
            }
        }
        // TODO: salvar a lista de compras no back-end.
        paciente.getListaCompras().clear();
        paciente.getListaCompras().addAll(novaLista);
        carregarRascunho();
        toast(R.string.lista_compras_salva);
        montarLista();
    }
}
