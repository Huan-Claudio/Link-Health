package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.UUID;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.ui.adapter.ProdutoEditavelAdapter;
import br.edu.pucgoias.app.ui.nutricionista.edicao.ProdutosViewModel;

/** Edita recomendações reais; o ViewModel preserva o salvamento durante a rotação. */
public class EditarProdutosActivity extends BaseActivity implements ProdutoEditavelAdapter.Acoes {
    public static final String EXTRA_PATIENT_ID = "recommendation_patient_id";
    public static final String EXTRA_PATIENT_NAME = "recommendation_patient_name";
    private ProdutosViewModel viewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        UUID patientId;
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
        viewModel = new ViewModelProvider(this).get(ProdutosViewModel.class);
        ProdutoEditavelAdapter adapter = new ProdutoEditavelAdapter(viewModel.atual().produtos, this);
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setAdapter(adapter);
        binding.btnRodape.setVisibility(View.VISIBLE);
        binding.btnRodape.setText(R.string.adicionar_produto);
        binding.btnRodape.setOnClickListener(v -> {
            viewModel.adicionar();
            binding.lista.smoothScrollToPosition(viewModel.atual().produtos.size() - 1);
        });
        viewModel.observar().observe(this, estado -> {
            binding.btnRodape.setEnabled(estado.carregado);
            adapter.notifyDataSetChanged();
            String mensagem = viewModel.consumirMensagem();
            if (mensagem != null) toast(mensagem);
            if (estado.falhaInicial) finish();
        });
        viewModel.carregar(patientId, Sessao.getAccessToken());
    }

    @Override public void salvar(int posicao) { viewModel.salvar(posicao); }
    @Override public void remover(int posicao) { viewModel.remover(posicao); }
}
