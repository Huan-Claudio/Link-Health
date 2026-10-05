package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ActivityEditarListaComprasBinding;
import br.edu.pucgoias.app.model.ItemCompra;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.adapter.CampoEditavelAdapter;
import br.edu.pucgoias.app.ui.nutricionista.edicao.EdicaoViewModel;
import br.edu.pucgoias.app.ui.nutricionista.edicao.LinhaCompra;

/**
 * Figma: "Editar Lista de Compras" (nutricionista).
 * Os itens são editados num rascunho (guardado no ViewModel) e só vão para a lista do paciente
 * ao tocar em "Salvar Alterações".
 */
public class EditarListaComprasActivity extends BaseActivity {

    private ActivityEditarListaComprasBinding binding;
    private Paciente paciente;
    private EdicaoViewModel viewModel;
    private CampoEditavelAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        paciente = pacienteDaIntent();
        if (paciente == null) {
            fecharPacienteNaoEncontrado();
            return;
        }
        binding = ActivityEditarListaComprasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, getString(R.string.lista_compras_de, paciente.getNome()));

        viewModel = new ViewModelProvider(this).get(EdicaoViewModel.class);
        if (viewModel.precisaCarregar()) carregarRascunho();

        adapter = new CampoEditavelAdapter(viewModel.linhasCompra, R.string.hint_item);
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setAdapter(adapter);

        binding.btnAdicionar.getRoot().setOnClickListener(v -> adicionarItem());
        binding.btnSalvar.getRoot().setOnClickListener(v -> salvar());
    }

    private void carregarRascunho() {
        viewModel.linhasCompra.clear();
        for (ItemCompra item : paciente.getListaCompras()) {
            viewModel.linhasCompra.add(new LinhaCompra(item, item.getDescricao()));
        }
    }

    private void adicionarItem() {
        viewModel.linhasCompra.add(new LinhaCompra(null, ""));
        int posicao = viewModel.linhasCompra.size() - 1;
        adapter.notifyItemInserted(posicao);
        binding.lista.scrollToPosition(posicao);
        CampoEditavelAdapter.focarCampo(binding.lista, posicao);
    }

    /** Aplica o rascunho na lista do paciente (itens em branco são descartados). */
    private void salvar() {
        List<ItemCompra> novaLista = new ArrayList<>();
        for (LinhaCompra linha : viewModel.linhasCompra) {
            if (linha.isVazio()) continue;
            String texto = linha.texto.trim();
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
        adapter.notifyDataSetChanged();
        toast(R.string.lista_compras_salva);
    }
}
