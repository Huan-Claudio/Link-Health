package br.edu.pucgoias.app.ui.paciente;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.ShoppingApi;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.model.ItemCompra;
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

    private final ShoppingApi shoppingApi = new ShoppingApi();
    private final List<ItemCompra> itens = new ArrayList<>();
    private final Set<java.util.UUID> itensEmAtualizacao = new HashSet<>();
    private ItemCompraAdapter itemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        ActivityListaSimplesBinding binding = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Paciente paciente = MockData.getPacienteLogado();
        configurarHeader(binding.header, getString(R.string.lista_compras_de, Sessao.getNomeUsuario()));

        itemAdapter = new ItemCompraAdapter(itens, this::alterarStatusCompra);
        ConcatAdapter adapter = new ConcatAdapter(itemAdapter);
        if (!paciente.getProdutos().isEmpty()) {
            adapter.addAdapter(new TituloSecaoAdapter(R.string.produtos_recomendados));
            adapter.addAdapter(new ProdutoRecomendadoAdapter(paciente.getProdutos(), this::abrirLink));
        }
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setAdapter(adapter);
        carregarItens();
    }

    /** Busca a lista persistida no banco para o paciente da sessão, não a lista de exemplo. */
    private void carregarItens() {
        if (!Sessao.temSessaoRemota()) {
            toast(R.string.sessao_remota_necessaria);
            return;
        }
        shoppingApi.list(Sessao.getUsuarioId(), Sessao.getAccessToken(), new ShoppingApi.ListCallback() {
            @Override
            public void onSuccess(List<ShoppingApi.Item> resposta) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    itens.clear();
                    for (ShoppingApi.Item item : resposta) {
                        itens.add(new ItemCompra(item.getId(), descricao(item), item.isPurchased()));
                    }
                    itemAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) toast(message);
                });
            }
        });
    }

    /** Só muda a interface depois que a API confirma a alteração no banco. */
    private void alterarStatusCompra(ItemCompra item, int position) {
        if (item.getId() == null || itensEmAtualizacao.contains(item.getId())) return;
        boolean comprado = !item.isComprado();
        itensEmAtualizacao.add(item.getId());
        shoppingApi.updatePurchaseStatus(Sessao.getUsuarioId(), item.getId(), comprado, Sessao.getAccessToken(),
                new ShoppingApi.ItemCallback() {
                    @Override
                    public void onSuccess(ShoppingApi.Item resposta) {
                        runOnUiThread(() -> {
                            itensEmAtualizacao.remove(item.getId());
                            if (isFinishing() || isDestroyed()) return;
                            item.setComprado(resposta.isPurchased());
                            itemAdapter.notifyItemChanged(position);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            itensEmAtualizacao.remove(item.getId());
                            if (!isFinishing() && !isDestroyed()) toast(message);
                        });
                    }
                });
    }

    private String descricao(ShoppingApi.Item item) {
        return item.getQuantity() + " " + unidade(item.getUnit()) + " de " + item.getName();
    }

    private String unidade(String unit) {
        switch (unit) {
            case "GRAM": return "g";
            case "KILOGRAM": return "kg";
            case "MILLILITER": return "ml";
            case "LITER": return "L";
            case "PACKAGE": return "pacote(s)";
            case "BOX": return "caixa(s)";
            default: return "unidade(s)";
        }
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
