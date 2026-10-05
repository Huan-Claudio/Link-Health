package br.edu.pucgoias.app.ui.nutricionista;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Locale;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Refeicao;
import br.edu.pucgoias.app.ui.adapter.BotaoAdicionarRefeicaoAdapter;
import br.edu.pucgoias.app.ui.adapter.RefeicaoEditavelAdapter;
import br.edu.pucgoias.app.ui.nutricionista.edicao.EdicaoViewModel;
import br.edu.pucgoias.app.ui.nutricionista.edicao.RascunhoRefeicao;

/**
 * Figma: "Editar Plano Alimentar do paciente".
 * Cada card edita um rascunho da refeição (guardado no ViewModel); as mudanças só vão para o plano
 * do paciente ao tocar em "Salvar Alterações" daquele card.
 */
public class EditarPlanoActivity extends BaseActivity implements RefeicaoEditavelAdapter.Acoes {

    private ActivityListaSimplesBinding binding;
    private Paciente paciente;
    private EdicaoViewModel viewModel;
    private RefeicaoEditavelAdapter adapter;
    private String[] tiposRefeicao;
    private PopupMenu popupTipo;

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
        configurarHeader(binding.header, getString(R.string.plano_alimentar_de, paciente.getNome()));

        tiposRefeicao = getResources().getStringArray(R.array.tipos_refeicao);
        viewModel = new ViewModelProvider(this).get(EdicaoViewModel.class);
        if (viewModel.precisaCarregar()) {
            for (Refeicao refeicao : paciente.getPlano()) {
                viewModel.refeicoes.add(new RascunhoRefeicao(refeicao, tiposRefeicao));
            }
        }

        adapter = new RefeicaoEditavelAdapter(viewModel.refeicoes, this);
        // "+ Adicionar Refeição" fica logo abaixo do último card: é o último item da lista.
        ConcatAdapter lista = new ConcatAdapter(adapter,
                new BotaoAdicionarRefeicaoAdapter(v -> adicionarRefeicao()));
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setAdapter(lista);
    }

    @Override
    protected void onDestroy() {
        if (popupTipo != null) popupTipo.dismiss();
        super.onDestroy();
    }

    private void adicionarRefeicao() {
        // TODO: criar a refeição no back-end.
        Refeicao nova = new Refeicao(tiposRefeicao[0], "00:00");
        paciente.getPlano().add(nova);
        viewModel.refeicoes.add(new RascunhoRefeicao(nova, tiposRefeicao));
        int posicao = viewModel.refeicoes.size() - 1;
        adapter.notifyItemInserted(posicao);
        binding.lista.smoothScrollToPosition(posicao);
    }

    /** Dropdown com os tipos de refeição. */
    @Override
    public void escolherTipo(int posicao, View ancora) {
        if (popupTipo != null) popupTipo.dismiss();
        popupTipo = new PopupMenu(this, ancora);
        for (int i = 0; i < tiposRefeicao.length; i++) {
            popupTipo.getMenu().add(0, i, i, tiposRefeicao[i]);
        }
        popupTipo.setOnMenuItemClickListener(item -> {
            if (posicao < viewModel.refeicoes.size()) {
                viewModel.refeicoes.get(posicao).nome = tiposRefeicao[item.getItemId()];
                adapter.notifyItemChanged(posicao);
            }
            return true;
        });
        popupTipo.show();
    }

    /** Relógio nativo do Android (24h) para escolher o horário da refeição. */
    @Override
    public void escolherHorario(int posicao) {
        RascunhoRefeicao r = viewModel.refeicoes.get(posicao);
        int hora = 0;
        int minuto = 0;
        try {
            String[] partes = r.horario.split(":");
            hora = Integer.parseInt(partes[0].trim());
            minuto = Integer.parseInt(partes[1].trim());
        } catch (RuntimeException ignorada) {
            // horário inválido: começa em 00:00
        }
        TimePickerDialog relogio = new TimePickerDialog(this, (view, horaEscolhida, minutoEscolhido) -> {
            r.horario = String.format(Locale.ROOT, "%02d:%02d", horaEscolhida, minutoEscolhido);
            int atual = viewModel.refeicoes.indexOf(r);
            if (atual >= 0) adapter.notifyItemChanged(atual);
        }, hora, minuto, true);
        registrarDialog(relogio);
        relogio.show();
    }

    @Override
    public void remover(int posicao) {
        // TODO: remover a refeição no back-end.
        RascunhoRefeicao r = viewModel.refeicoes.remove(posicao);
        paciente.getPlano().remove(r.original);
        adapter.notifyItemRemoved(posicao);
    }

    /** Aplica o rascunho do card na refeição do paciente. */
    @Override
    public void salvar(int posicao) {
        // TODO: salvar a refeição no back-end.
        viewModel.refeicoes.get(posicao).aplicar(tiposRefeicao);
        adapter.notifyItemChanged(posicao);
        toast(R.string.refeicao_salva);
    }
}
