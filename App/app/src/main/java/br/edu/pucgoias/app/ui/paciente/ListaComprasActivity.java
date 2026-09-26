package br.edu.pucgoias.app.ui.paciente;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.ItemCompra;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Produto;

/**
 * Figma: "Editar Lista de Compra" (visão do paciente).
 * O paciente marca os itens comprados e vê os produtos recomendados com link de compra.
 */
public class ListaComprasActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);

        Paciente paciente = MockData.getPacienteLogado();
        configurarHeader(getString(R.string.lista_compras_de, paciente.getNome()));

        LinearLayout container = findViewById(R.id.container);
        LayoutInflater inflater = LayoutInflater.from(this);

        // Card com os itens da lista
        LinearLayout cardItens = (LinearLayout) inflater.inflate(R.layout.view_card_container, container, false);
        for (ItemCompra item : paciente.getListaCompras()) {
            View linha = inflater.inflate(R.layout.item_compra, cardItens, false);
            ((TextView) linha.findViewById(R.id.tvDescricao)).setText(item.getDescricao());
            ImageView ivAcao = linha.findViewById(R.id.ivAcao);
            PlanoAlimentarActivity.atualizarCheck(ivAcao, item.isComprado());
            linha.setOnClickListener(v -> {
                item.setComprado(!item.isComprado());
                PlanoAlimentarActivity.atualizarCheck(ivAcao, item.isComprado());
            });
            cardItens.addView(linha);
        }
        container.addView(cardItens);

        // Produtos recomendados pelo nutricionista
        if (!paciente.getProdutos().isEmpty()) {
            TextView titulo = new TextView(this, null, 0, R.style.LH_Text_Section);
            titulo.setText(R.string.produtos_recomendados);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = getResources().getDimensionPixelSize(R.dimen.lh_space_s);
            lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.lh_space_m);
            container.addView(titulo, lp);
        }
        for (Produto produto : paciente.getProdutos()) {
            View card = inflater.inflate(R.layout.item_produto_recomendado, container, false);
            ((TextView) card.findViewById(R.id.tvNomeProduto)).setText(produto.getNome());
            ((TextView) card.findViewById(R.id.tvPrecoProduto)).setText("R$ " + produto.getPreco());
            card.findViewById(R.id.btnComprar).setOnClickListener(v -> abrirLink(produto.getLink()));
            container.addView(card);
        }
    }

    private void abrirLink(String link) {
        try {
            String url = link.startsWith("http") ? link : "https://" + link;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            toast(R.string.link_invalido);
        }
    }
}
