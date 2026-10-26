package br.edu.pucgoias.app.ui.comum;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.lifecycle.ViewModelProvider;

import br.edu.pucgoias.app.*;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecordsApi;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.PatientNavigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.*;

import java.util.*;

/** Uma avaliação é um card com três fotos, peso e data. */
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
            LinearLayout card =
                    screen.card(
                            record.optString("weightKg") + " kg",
                            RecordFormatting.time(record.optString("recordedAt")));
            LinearLayout strip = screen.column();
            strip.setOrientation(LinearLayout.HORIZONTAL);
            card.addView(strip);
            JSONArray photos = record.optJSONArray("photos");
            String[] positions = {"Frente", "Lado", "Costas"};
            for (int j = 0; photos != null && j < photos.length(); j++) {
                JSONObject photo = photos.optJSONObject(j);
                if (photo == null) continue;
                String id = photo.optString("photoId");
                photoIds.add(id);
                final int index = j;
                LinearLayout cell = screen.column();
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
                p.setMargins(screen.dp(3), screen.dp(8), screen.dp(3), screen.dp(8));
                strip.addView(cell, p);
                ImageView image = new ImageView(this);
                image.setImageResource(R.drawable.ic_image);
                image.setBackgroundResource(R.drawable.bg_image_placeholder);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setContentDescription("Foto de " + positions[j]);
                cell.addView(image, new LinearLayout.LayoutParams(-1, screen.dp(112)));
                TextView text = screen.text(positions[j], 12);
                text.setGravity(17);
                cell.addView(text);
                cell.setOnClickListener(
                        v ->
                                registrarDialog(
                                        EvolutionBook.show(
                                                this,
                                                patient,
                                                Sessao.getAccessToken(),
                                                record,
                                                index)));
                EvolutionBook.thumbnail(this, patient, Sessao.getAccessToken(), id, image, 360);
            }
            MaterialButton book =
                    screen.button(
                            "Ver 3 fotos",
                            v ->
                                    registrarDialog(
                                            EvolutionBook.show(
                                                    this,
                                                    patient,
                                                    Sessao.getAccessToken(),
                                                    record,
                                                    0)));
            card.addView(book);
            if (own) {
                MaterialButton remove =
                        screen.button(
                                "Remover avaliação",
                                v ->
                                        registrarDialog(
                                                new MaterialAlertDialogBuilder(this)
                                                        .setTitle("Remover avaliação?")
                                                        .setMessage(
                                                                "As três fotos e o peso deste"
                                                                    + " registro serão removidos"
                                                                    + " juntos.")
                                                        .setNegativeButton("Cancelar", null)
                                                        .setPositiveButton(
                                                                "Remover",
                                                                (d, n) ->
                                                                        vm.delete(
                                                                                RecordsApi
                                                                                        .ASSESSMENTS,
                                                                                record.optString(
                                                                                        "id")))
                                                        .show()));
                remove.setEnabled(vm.ready());
                card.addView(remove);
            }
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
}
