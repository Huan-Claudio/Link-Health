package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecommendedProductsApi;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.model.Produto;
import br.edu.pucgoias.app.ui.adapter.ProdutoEditavelAdapter;
import br.edu.pucgoias.app.ui.nutricionista.edicao.RascunhoProduto;

/** Edição dos produtos de um paciente com acompanhamento ativo. */
public class EditarProdutosActivity extends BaseActivity implements ProdutoEditavelAdapter.Acoes {
    public static final String EXTRA_PATIENT_ID = "recommendation_patient_id";
    public static final String EXTRA_PATIENT_NAME = "recommendation_patient_name";
    private final RecommendedProductsApi api = new RecommendedProductsApi();
    private final List<RascunhoProduto> produtos = new ArrayList<>();
    private final Set<RascunhoProduto> pendentes = new HashSet<>();
    private UUID patientId;
    private ProdutoEditavelAdapter adapter;
    private boolean carregado;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        try { patientId = UUID.fromString(getIntent().getStringExtra(EXTRA_PATIENT_ID)); }
        catch (RuntimeException e) {
            toast("Escolha um paciente vinculado em Produtos recomendados na tela inicial.");
            finish(); return;
        }
        if (!Sessao.temSessaoRemota()) { toast(R.string.sessao_remota_necessaria); finish(); return; }
        ActivityListaSimplesBinding binding = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, getString(R.string.produtos_recomendados_de,
                getIntent().getStringExtra(EXTRA_PATIENT_NAME)));
        adapter = new ProdutoEditavelAdapter(produtos, this);
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setAdapter(adapter);
        binding.btnRodape.setVisibility(View.VISIBLE);
        binding.btnRodape.setText(R.string.adicionar_produto);
        binding.btnRodape.setEnabled(false);
        binding.btnRodape.setOnClickListener(v -> {
            produtos.add(new RascunhoProduto(new Produto("", "", "")));
            adapter.notifyItemInserted(produtos.size() - 1);
            binding.lista.smoothScrollToPosition(produtos.size() - 1);
        });
        if (savedInstanceState != null && savedInstanceState.getBoolean("produtos_carregados")) {
            ArrayList<String> nomes = savedInstanceState.getStringArrayList("produtos_nomes");
            ArrayList<String> precos = savedInstanceState.getStringArrayList("produtos_precos");
            ArrayList<String> links = savedInstanceState.getStringArrayList("produtos_links");
            ArrayList<String> ids = savedInstanceState.getStringArrayList("produtos_ids");
            if (nomes != null && precos != null && links != null && ids != null) {
                for (int i = 0; i < nomes.size(); i++) {
                    Produto produto = new Produto(nomes.get(i), precos.get(i), links.get(i));
                    if (!ids.get(i).isEmpty()) produto.setId(UUID.fromString(ids.get(i)));
                    produtos.add(new RascunhoProduto(produto));
                }
                carregado = true;
                adapter.notifyDataSetChanged(); binding.btnRodape.setEnabled(true); return;
            }
        }
        api.list(patientId, Sessao.getAccessToken(), new RecommendedProductsApi.ListCallback() {
            public void onSuccess(List<Produto> response) { runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                for (Produto produto : response) produtos.add(new RascunhoProduto(produto));
                carregado = true; adapter.notifyDataSetChanged(); binding.btnRodape.setEnabled(true);
            }); }
            public void onError(String message) { runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) { toast(message); finish(); }
            }); }
        });
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("produtos_carregados", carregado);
        ArrayList<String> nomes = new ArrayList<>(), precos = new ArrayList<>(), links = new ArrayList<>(), ids = new ArrayList<>();
        for (RascunhoProduto r : produtos) {
            nomes.add(r.nome); precos.add(r.preco); links.add(r.link);
            ids.add(r.original.getId() == null ? "" : r.original.getId().toString());
        }
        outState.putStringArrayList("produtos_nomes", nomes);
        outState.putStringArrayList("produtos_precos", precos);
        outState.putStringArrayList("produtos_links", links);
        outState.putStringArrayList("produtos_ids", ids);
    }

    @Override public void salvar(int posicao) {
        RascunhoProduto draft = produtos.get(posicao);
        if (pendentes.contains(draft)) return;
        BigDecimal price;
        try {
            price = new BigDecimal(draft.preco.trim().replace(',', '.'));
            if (price.signum() < 0 || price.scale() > 2
                    || price.compareTo(new BigDecimal("9999999999.99")) > 0) throw new NumberFormatException();
        } catch (NumberFormatException e) { toast("Informe um preço válido, com até duas casas decimais."); return; }
        String link = draft.link.trim();
        try {
            URI uri = URI.create(link);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) { toast("Informe um link válido começando com https:// ou http://."); return; }
        pendentes.add(draft);
        api.save(patientId, draft.original, draft.nome.trim(), price, link, Sessao.getAccessToken(),
                new RecommendedProductsApi.ProductCallback() {
                    public void onSuccess(Produto result) { runOnUiThread(() -> {
                        pendentes.remove(draft);
                        if (isFinishing() || isDestroyed()) return;
                        draft.original.setId(result.getId()); draft.original.setNome(result.getNome());
                        draft.original.setPreco(result.getPreco()); draft.original.setLink(result.getLink());
                        toast(R.string.produto_salvo);
                    }); }
                    public void onError(String message) { runOnUiThread(() -> {
                        pendentes.remove(draft); if (!isFinishing() && !isDestroyed()) toast(message);
                    }); }
                });
    }

    @Override public void remover(int posicao) {
        RascunhoProduto draft = produtos.get(posicao);
        if (pendentes.contains(draft)) return;
        if (draft.original.getId() == null) { produtos.remove(draft); adapter.notifyItemRemoved(posicao); return; }
        pendentes.add(draft);
        api.delete(patientId, draft.original.getId(), Sessao.getAccessToken(), new RecommendedProductsApi.DeleteCallback() {
            public void onSuccess() { runOnUiThread(() -> {
                pendentes.remove(draft); if (isFinishing() || isDestroyed()) return;
                int index = produtos.indexOf(draft);
                if (index >= 0) { produtos.remove(index); adapter.notifyItemRemoved(index); }
            }); }
            public void onError(String message) { runOnUiThread(() -> {
                pendentes.remove(draft); if (!isFinishing() && !isDestroyed()) toast(message);
            }); }
        });
    }
}
