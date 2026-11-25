package br.edu.pucgoias.app.ui.comum;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.lifecycle.ViewModelProvider;

import br.edu.pucgoias.app.*;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecordsApi;
import br.edu.pucgoias.app.databinding.ItemAvaliacaoEvolucaoBinding;
import br.edu.pucgoias.app.databinding.ViewFotoAvaliacaoBinding;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.PatientNavigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.*;

import java.util.*;

/** Uma avaliação é um card com quatro fotos, peso e data. */
public class EvolucaoCorporalActivity extends BaseActivity {
    private RecordsScreen screen;
    private RecordsViewModel vm;
    private UUID patient;
    private boolean own, oldVisible;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        patient = PatientNavigation.patientId(getIntent());
        if (patient == null) {
            fecharPacienteNaoEncontrado();
            return;
        }
        own = Sessao.getPerfil() == Perfil.PACIENTE;
        oldVisible = state != null && state.getBoolean("oldVisible");
        screen = new RecordsScreen(this, getString(R.string.evolucao_corporal));
        setContentView(screen.root);
        vm = new ViewModelProvider(this).get(RecordsViewModel.class);
        if (own) {
            MaterialButton add =
                    screen.button(
                            "Nova avaliação · fotos e peso",
                            v -> startActivity(new Intent(this, NovaEvolucaoActivity.class)));
            add.setId(R.id.assessmentNew);
            screen.actions.addView(add);
        }
        screen.refresh.setOnClickListener(v -> vm.reload());
        vm.changes.observe(this, x -> render());
        vm.configure(
                patient,
                Sessao.getAccessToken(),
                false,
                RecordsApi.ASSESSMENTS,
                RecordsApi.PHOTOS,
                RecordsApi.WEIGHT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null) vm.reload();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putBoolean("oldVisible", oldVisible);
        super.onSaveInstanceState(out);
    }

    private void render() {
        screen.state(vm);
        if (vm.loading > 0) return;
        screen.rows.removeAllViews();
        JSONArray records = vm.items(RecordsApi.ASSESSMENTS);
        Set<String> photoIds = new HashSet<>(), weightIds = new HashSet<>();
        if (records.length() == 0)
            screen.card(
                    "Sua evolução ao longo do tempo",
                    "Cada avaliação reúne frente, lado, costas e peso. Registre uma nova avaliação"
                        + " para começar.");
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.optJSONObject(i);
            if (record == null) continue;
            weightIds.add(record.optString("weightRecordId"));
            // Card: grade 2x2 (Frente | Costas / Lado direito | Lado esquerdo), com peso e data abaixo.
            ItemAvaliacaoEvolucaoBinding item =
                    ItemAvaliacaoEvolucaoBinding.inflate(getLayoutInflater(), screen.rows, false);
            item.tvPesoAvaliacao.setText(
                    getString(R.string.peso_avaliacao, record.optString("weightKg")));
            item.tvDataAvaliacao.setText(RecordFormatting.time(record.optString("recordedAt")));
            ViewFotoAvaliacaoBinding[] celulas = {
                item.fotoFrente, item.fotoCostas, item.fotoLadoDireito, item.fotoLadoEsquerdo
            };
            int[] rotulos = {
                R.string.foto_frente,
                R.string.foto_costas,
                R.string.foto_lado_direito,
                R.string.foto_lado_esquerdo
            };
            // Posição da foto no servidor e página dela no álbum "Ver fotos".
            // O servidor guarda só três fotos (FRONT, SIDE, BACK): "Lado esquerdo" fica como "Sem foto".
            String[] posicoes = {"FRONT", "BACK", "SIDE", null};
            int[] paginas = {0, 2, 1, -1};
            JSONArray photos = record.optJSONArray("photos");
            for (int j = 0; j < celulas.length; j++) {
                ViewFotoAvaliacaoBinding celula = celulas[j];
                celula.tvRotulo.setText(rotulos[j]);
                celula.ivFoto.setContentDescription(
                        getString(R.string.foto_de, getString(rotulos[j])));
                JSONObject photo =
                        posicoes[j] == null ? null : fotoNaPosicao(photos, posicoes[j], paginas[j]);
                if (photo == null) {
                    celula.tvSemFoto.setVisibility(View.VISIBLE);
                    celula.getRoot().setEnabled(false);
                    celula.getRoot().setClickable(false);
                    continue;
                }
                String id = photo.optString("photoId");
                photoIds.add(id);
                final int pagina = paginas[j];
                celula.getRoot()
                        .setOnClickListener(
                                v ->
                                        registrarDialog(
                                                EvolutionBook.show(
                                                        this,
                                                        patient,
                                                        Sessao.getAccessToken(),
                                                        record,
                                                        pagina)));
                EvolutionBook.thumbnail(
                        this, patient, Sessao.getAccessToken(), id, celula.ivFoto, 360);
            }
            item.btnVerFotos.setOnClickListener(
                    v ->
                            registrarDialog(
                                    EvolutionBook.show(
                                            this, patient, Sessao.getAccessToken(), record, 0)));
            if (own) {
                item.btnRemoverAvaliacao.setVisibility(View.VISIBLE);
                item.btnRemoverAvaliacao.setEnabled(vm.ready());
                item.btnRemoverAvaliacao.setOnClickListener(
                        v ->
                                registrarDialog(
                                        new MaterialAlertDialogBuilder(this)
                                                .setTitle("Remover avaliação?")
                                                .setMessage(
                                                        "As fotos e o peso deste"
                                                                + " registro serão removidos"
                                                                + " juntos.")
                                                .setNegativeButton("Cancelar", null)
                                                .setPositiveButton(
                                                        "Remover",
                                                        (d, n) ->
                                                                vm.delete(
                                                                        RecordsApi.ASSESSMENTS,
                                                                        record.optString("id")))
                                                .show()));
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, screen.dp(10), 0, screen.dp(4));
            screen.rows.addView(item.getRoot(), params);
        }
        JSONArray photos = vm.items(RecordsApi.PHOTOS), weights = vm.items(RecordsApi.WEIGHT);
        boolean legacy = false;
        for (int i = 0; i < photos.length(); i++)
            if (!photoIds.contains(photos.optJSONObject(i).optString("id"))) legacy = true;
        for (int i = 0; i < weights.length(); i++)
            if (!weightIds.contains(weights.optJSONObject(i).optString("id"))) legacy = true;
        if (legacy) {
            MaterialButton old =
                    screen.button(
                            oldVisible
                                    ? "Ocultar registros anteriores"
                                    : "Ver registros anteriores",
                            v -> {
                                oldVisible = !oldVisible;
                                render();
                            });
            screen.rows.addView(old);
            if (oldVisible) {
                screen.rows.addView(screen.text("Registros anteriores sem avaliação conjunta", 18));
                screen.rows.addView(
                        screen.text(
                                "Esses dados foram salvos antes do novo formato. Não associamos"
                                    + " fotos e pesos por suposição.",
                                13));
                for (int i = 0; i < weights.length(); i++) {
                    JSONObject w = weights.optJSONObject(i);
                    if (!weightIds.contains(w.optString("id")))
                        screen.card(
                                w.optString("weightKg") + " kg",
                                RecordFormatting.time(w.optString("measuredAt")));
                }
                for (int i = 0; i < photos.length(); i++) {
                    JSONObject photo = photos.optJSONObject(i);
                    if (photoIds.contains(photo.optString("id"))) continue;
                    LinearLayout c =
                            screen.card(
                                    "Foto anterior",
                                    RecordFormatting.time(photo.optString("capturedAt")));
                    if ("STORED".equals(photo.optString("uploadStatus"))) {
                        ImageView image = new ImageView(this);
                        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        c.addView(image, new LinearLayout.LayoutParams(-1, screen.dp(240)));
                        EvolutionBook.thumbnail(
                                this,
                                patient,
                                Sessao.getAccessToken(),
                                photo.optString("id"),
                                image,
                                1200);
                    } else c.addView(screen.text("Envio anterior pendente", 14));
                }
            }
        }
    }

    /** Foto da posição pedida (FRONT, SIDE, BACK); se o servidor não informar a posição, usa a ordem. */
    private static JSONObject fotoNaPosicao(JSONArray photos, String posicao, int ordem) {
        if (photos == null) return null;
        for (int i = 0; i < photos.length(); i++) {
            JSONObject photo = photos.optJSONObject(i);
            if (photo != null && posicao.equals(photo.optString("position"))) return photo;
        }
        return photos.optJSONObject(ordem);
    }
}
