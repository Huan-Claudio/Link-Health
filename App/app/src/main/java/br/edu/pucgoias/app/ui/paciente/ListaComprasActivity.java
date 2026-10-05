package br.edu.pucgoias.app.ui.paciente;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Produto;
import br.edu.pucgoias.app.ui.adapter.ItemCompraAdapter;
import br.edu.pucgoias.app.ui.adapter.ProdutoRecomendadoAdapter;
import br.edu.pucgoias.app.ui.adapter.TituloSecaoAdapter;

/**
 * Figma: "Editar Lista de Compra" (visão do paciente).
 * O paciente marca os itens comprados e vê os produtos recomendados com link de compra.
 * Itens, título e produtos ficam numa única RecyclerView (ConcatAdapter).
 */
public class ListaComprasActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        ActivityListaSimplesBinding binding = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Paciente paciente = MockData.getPacienteLogado();
        configurarHeader(binding.header, getString(R.string.lista_compras_de, paciente.getNome()));

        ConcatAdapter adapter = new ConcatAdapter(new ItemCompraAdapter(paciente.getListaCompras()));
        if (!paciente.getProdutos().isEmpty()) {
            adapter.addAdapter(new TituloSecaoAdapter(R.string.produtos_recomendados));
            adapter.addAdapter(new ProdutoRecomendadoAdapter(paciente.getProdutos(), this::abrirLink));
        }
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setAdapter(adapter);
    }

    /** Abre o link do produto no navegador com uma Intent implícita (ACTION_VIEW). */
    private void abrirLink(Produto produto) {
        String link = produto.getLink().trim();
        if (link.isEmpty()) {
            toast(R.string.link_invalido);
            return;
        }
        try {
            String url = link.startsWith("http") ? link : "https://" + link;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            toast(R.string.link_invalido);
        }
    }
}
