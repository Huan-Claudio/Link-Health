package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Produto;
import br.edu.pucgoias.app.ui.adapter.ProdutoEditavelAdapter;
import br.edu.pucgoias.app.ui.nutricionista.edicao.EdicaoViewModel;
import br.edu.pucgoias.app.ui.nutricionista.edicao.RascunhoProduto;

/** Figma: "Editar Produtos Recomendados" – nome, preço e link externo de compra. */
public class EditarProdutosActivity extends BaseActivity implements ProdutoEditavelAdapter.Acoes {

    private ActivityListaSimplesBinding binding;
    private Paciente paciente;
    private EdicaoViewModel viewModel;
    private ProdutoEditavelAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        paciente = pacienteDaIntent();
        if (paciente == null) {
            fecharPacienteNaoEncontrado();
            return;
        }
        binding = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, getString(R.string.produtos_recomendados_de, paciente.getNome()));

        viewModel = new ViewModelProvider(this).get(EdicaoViewModel.class);
        if (viewModel.precisaCarregar()) {
            for (Produto produto : paciente.getProdutos()) viewModel.produtos.add(new RascunhoProduto(produto));
        }

        adapter = new ProdutoEditavelAdapter(viewModel.produtos, this);
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setHasFixedSize(true);
        binding.lista.setAdapter(adapter);

        binding.btnRodape.setVisibility(View.VISIBLE);
        binding.btnRodape.setText(R.string.adicionar_produto);
        binding.btnRodape.setOnClickListener(v -> adicionarProduto());
    }

    private void adicionarProduto() {
        Produto novo = new Produto("", "", "");
        paciente.getProdutos().add(novo);
        viewModel.produtos.add(new RascunhoProduto(novo));
        int posicao = viewModel.produtos.size() - 1;
        adapter.notifyItemInserted(posicao);
        binding.lista.smoothScrollToPosition(posicao);
    }

    @Override
    public void salvar(int posicao) {
        // TODO: salvar no back-end.
        viewModel.produtos.get(posicao).aplicar();
        toast(R.string.produto_salvo);
    }

    @Override
    public void remover(int posicao) {
        RascunhoProduto removido = viewModel.produtos.remove(posicao);
        paciente.getProdutos().remove(removido.original);
        adapter.notifyItemRemoved(posicao);
    }
}
