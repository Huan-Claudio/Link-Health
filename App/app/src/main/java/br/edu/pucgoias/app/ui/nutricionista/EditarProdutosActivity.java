package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Produto;

public class EditarProdutosActivity extends BaseActivity {

    private Paciente paciente;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);

        paciente = MockData.getPaciente(getIntent().getIntExtra(MockData.EXTRA_PACIENTE, 0));
        configurarHeader(getString(R.string.produtos_recomendados_de, paciente.getNome()));
        container = findViewById(R.id.container);

        Button btnRodape = findViewById(R.id.btnRodape);
        btnRodape.setVisibility(View.VISIBLE);
        btnRodape.setText(R.string.adicionar_produto);
        btnRodape.setOnClickListener(v -> {
            paciente.getProdutos().add(new Produto("", "", ""));
            montarLista();
        });

        montarLista();
    }

    private void montarLista() {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Produto produto : paciente.getProdutos()) {
            View card = inflater.inflate(R.layout.item_produto_editavel, container, false);
            EditText etNome = card.findViewById(R.id.etNomeProduto);
            EditText etPreco = card.findViewById(R.id.etPrecoProduto);
            EditText etLink = card.findViewById(R.id.etLinkProduto);
            etNome.setText(produto.getNome());
            etPreco.setText(produto.getPreco().replace(",", "."));
            etLink.setText(produto.getLink());

            card.findViewById(R.id.btnSalvarProduto).setOnClickListener(v -> {
                String nome = etNome.getText().toString().trim();
                if (nome.isEmpty()) {
                    etNome.setError(getString(R.string.campo_obrigatorio));
                    return;
                }
                // TODO: salvar no back-end.
                produto.setNome(nome);
                produto.setPreco(etPreco.getText().toString().trim().replace(".", ","));
                produto.setLink(etLink.getText().toString().trim());
                toast(R.string.produto_salvo);
            });
            card.findViewById(R.id.btnRemover).setOnClickListener(v -> {
                paciente.getProdutos().remove(produto);
                montarLista();
            });
            container.addView(card);
        }
    }
}
