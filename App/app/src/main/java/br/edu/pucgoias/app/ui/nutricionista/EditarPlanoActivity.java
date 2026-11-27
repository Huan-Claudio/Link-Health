package br.edu.pucgoias.app.ui.nutricionista;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;

import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;
import java.util.UUID;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.adapter.BotaoAdicionarRefeicaoAdapter;
import br.edu.pucgoias.app.ui.adapter.PlanoStatusAdapter;
import br.edu.pucgoias.app.ui.adapter.RefeicaoEditavelAdapter;
import br.edu.pucgoias.app.ui.nutricionista.edicao.PlansViewModel;
import br.edu.pucgoias.app.ui.nutricionista.edicao.RascunhoRefeicao;
import br.edu.pucgoias.app.util.PatientNavigation;

/**
 * Figma: "Editar Plano Alimentar do paciente".
 * Lista (RecyclerView + ConcatAdapter): aviso de carregamento/erro, um card por refeição
 * (tipo no menu suspenso, horário no relógio nativo, alimentos com lixeira, "+ Adicionar Alimento"
 * e "Salvar Alterações") e o botão "+ Adicionar Refeição".
 * O plano é publicado para o paciente automaticamente depois de cada salvamento (ver PlansViewModel).
 */
public class EditarPlanoActivity extends BaseActivity implements RefeicaoEditavelAdapter.Acoes {

    private ActivityListaSimplesBinding binding;
    private PlansViewModel model;
    private RefeicaoEditavelAdapter adapter;
    private PlanoStatusAdapter status;
    private String[] tiposRefeicao;
    private PopupMenu popupTipo;
    private int versaoExibida = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        UUID patient = PatientNavigation.patientId(getIntent());
        if (patient == null || !Sessao.temSessaoRemota() || Sessao.getPerfil() != Perfil.NUTRICIONISTA) {
            fecharPacienteNaoEncontrado();
            return;
        }
        binding = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header,
                getString(R.string.plano_alimentar_de, PatientNavigation.name(getIntent())));
        // Voltar (seta ou gesto) avisa se há cards com alterações não salvas.
        binding.header.btnVoltar.setOnClickListener(v -> sair());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                sair();
            }
        });

        tiposRefeicao = getResources().getStringArray(R.array.tipos_refeicao);
        model = new ViewModelProvider(this).get(PlansViewModel.class);

        status = new PlanoStatusAdapter(() -> {
            if (model.error != null) model.load();
        });
        adapter = new RefeicaoEditavelAdapter(model.refeicoes, this);
        // Aviso no topo (só ao carregar ou com erro), cards no meio e "+ Adicionar Refeição" logo abaixo do último card.
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setAdapter(new ConcatAdapter(status, adapter,
                new BotaoAdicionarRefeicaoAdapter(v -> adicionarRefeicao())));

        model.changes.observe(this, ignorado -> render());
        model.start(patient.toString(), tiposRefeicao);
    }

    @Override
    protected void onDestroy() {
        if (popupTipo != null) popupTipo.dismiss();
        super.onDestroy();
    }

    private void render() {
        if (model.versaoLista != versaoExibida) {
            versaoExibida = model.versaoLista;
            adapter.notifyDataSetChanged();
        }
        adapter.setOcupado(model.busy || model.loading);

        if (model.loading && !model.loaded) {
            status.mostrar(getString(R.string.plano_carregando), "");
        } else if (model.error != null) {
            status.mostrar(getString(R.string.plano_status_erro),
                    model.error + "\n" + getString(R.string.toque_tentar_novamente));
        } else {
            status.ocultar();
        }

        String aviso = model.consumirMensagem();
        if (aviso != null) toast(aviso);
    }

    private void adicionarRefeicao() {
        if (!model.loaded || model.loading) return;
        if (!model.temAcompanhamento()) {
            toast(model.error != null ? model.error : getString(R.string.plano_carregando));
            return;
        }
        int posicao = model.adicionarRefeicao(proximoTipo());
        adapter.notifyItemInserted(posicao);
        binding.lista.smoothScrollToPosition(posicao + status.getItemCount()); // o aviso, se houver, vem antes
    }

    /** Sugere o primeiro tipo de refeição que ainda não está no plano. */
    private String proximoTipo() {
        for (String tipo : tiposRefeicao) {
            boolean usado = false;
            for (RascunhoRefeicao r : model.refeicoes) if (r.nome.equals(tipo)) usado = true;
            if (!usado) return tipo;
        }
        return tiposRefeicao[0];
    }

    /** Dropdown com os tipos de refeição. */
    @Override
    public void escolherTipo(RascunhoRefeicao refeicao, View ancora) {
        if (popupTipo != null) popupTipo.dismiss();
        popupTipo = new PopupMenu(this, ancora);
        for (int i = 0; i < tiposRefeicao.length; i++) {
            popupTipo.getMenu().add(0, i, i, tiposRefeicao[i]);
        }
        popupTipo.setOnMenuItemClickListener(item -> {
            refeicao.nome = tiposRefeicao[item.getItemId()];
            int posicao = model.refeicoes.indexOf(refeicao);
            if (posicao >= 0) adapter.notifyItemChanged(posicao);
            return true;
        });
        popupTipo.show();
    }

    /** Relógio nativo do Android (24h) para escolher o horário da refeição. */
    @Override
    public void escolherHorario(RascunhoRefeicao refeicao) {
        int hora = 0;
        int minuto = 0;
        try {
            String[] partes = refeicao.horario.split(":");
            hora = Integer.parseInt(partes[0].trim());
            minuto = Integer.parseInt(partes[1].trim());
        } catch (RuntimeException ignorada) {
            // horário inválido: começa em 00:00
        }
        TimePickerDialog relogio = new TimePickerDialog(this, (view, h, m) -> {
            refeicao.horario = String.format(Locale.ROOT, "%02d:%02d", h, m);
            int posicao = model.refeicoes.indexOf(refeicao);
            if (posicao >= 0) adapter.notifyItemChanged(posicao);
        }, hora, minuto, true);
        registrarDialog(relogio);
        relogio.show();
    }

    @Override
    public void remover(RascunhoRefeicao refeicao) {
        registrarDialog(new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remover_refeicao_titulo)
                .setMessage(R.string.remover_refeicao_msg)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.remover, (d, w) -> model.remover(refeicao))
                .show());
    }

    @Override
    public void salvar(RascunhoRefeicao refeicao) {
        model.salvar(refeicao);
    }

    private void sair() {
        if (model == null || !model.temAlteracoesNaoSalvas()) {
            finish();
            return;
        }
        registrarDialog(new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sair_sem_salvar_titulo)
                .setMessage(R.string.sair_sem_salvar_msg)
                .setNegativeButton(R.string.continuar_editando, null)
                .setPositiveButton(R.string.sair_sem_salvar, (d, w) -> finish())
                .show());
    }
}
