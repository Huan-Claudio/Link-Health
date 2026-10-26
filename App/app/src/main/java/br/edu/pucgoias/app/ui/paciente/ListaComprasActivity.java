package br.edu.pucgoias.app.ui.paciente;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.model.Produto;
import br.edu.pucgoias.app.ui.adapter.ItemCompraAdapter;
import br.edu.pucgoias.app.ui.adapter.ProdutoRecomendadoAdapter;
import br.edu.pucgoias.app.ui.adapter.TituloSecaoAdapter;
import br.edu.pucgoias.app.ui.adapter.ShoppingMessageAdapter;
import br.edu.pucgoias.app.ui.nutricionista.EditarListaComprasActivity;
import br.edu.pucgoias.app.util.PatientNavigation;

/** Lista e recomendações reais do paciente autenticado. */
public class ListaComprasActivity extends BaseActivity {
    private ShoppingListViewModel model;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        if (!Sessao.temSessaoRemota()) { toast(R.string.sessao_remota_necessaria); finish(); return; }
        ActivityListaSimplesBinding b = ActivityListaSimplesBinding.inflate(getLayoutInflater()); setContentView(b.getRoot());
        configurarHeader(b.header, getString(R.string.lista_compras_de, Sessao.getNomeUsuario()));
        model = new ViewModelProvider(this).get(ShoppingListViewModel.class);
        ItemCompraAdapter items = new ItemCompraAdapter(model.items, (item, position) -> model.toggle(item));
        ProdutoRecomendadoAdapter products = new ProdutoRecomendadoAdapter(model.products, this::abrirLink);
        ShoppingMessageAdapter status = new ShoppingMessageAdapter(this::carregar);
        b.lista.setLayoutManager(new LinearLayoutManager(this));
        b.lista.setAdapter(new ConcatAdapter(status, items, new TituloSecaoAdapter(R.string.produtos_recomendados), products));
        b.btnRodape.setText("Editar minha lista"); b.btnRodape.setVisibility(View.VISIBLE);
        b.btnRodape.setOnClickListener(v -> startActivity(PatientNavigation.intent(
                this, EditarListaComprasActivity.class, Sessao.getUsuarioId(), Sessao.getNomeUsuario())));
        model.changes().observe(this, ignored -> {
            items.notifyDataSetChanged(); products.notifyDataSetChanged(); status.update(model.status);
            b.btnRodape.setEnabled(!model.pending && !model.loading);
            String message = model.takeMessage(); if (message != null) toast(message);
        });
    }

    @Override protected void onResume() { super.onResume(); if (model != null) carregar(); }
    private void carregar() { model.load(Sessao.getUsuarioId(), Sessao.getAccessToken()); }

    private void abrirLink(Produto product) {
        try {
            Uri uri = Uri.parse(product.getLink().trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) { toast(R.string.link_invalido); return; }
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException | IllegalArgumentException e) { toast(R.string.link_invalido); }
    }
}
