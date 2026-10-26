package br.edu.pucgoias.app.ui.comum;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecordsApi;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.PatientNavigation;

/** Fotos autenticadas no banco e histórico de peso; o nutricionista vinculado apenas consulta. */
public class EvolucaoCorporalActivity extends BaseActivity {
    private RecordsScreen screen;
    private RecordsViewModel vm;
    private UUID patient;
    private boolean own;
    private String cameraName, retryId;
    private boolean previewLoading;
    private final ActivityResultLauncher<String> gallery = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null && vm != null) vm.preparePhoto(getApplicationContext().getContentResolver(), uri, retryId, null);
        retryId = null;
    });
    private final ActivityResultLauncher<Uri> camera = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
        File file = cameraFile();
        if (success && file != null && vm != null) vm.preparePhoto(getApplicationContext().getContentResolver(), Uri.fromFile(file), retryId, file);
        else if (file != null) file.delete();
        cameraName = null; retryId = null;
    });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if (sessaoExpirada()) return;
        patient = PatientNavigation.patientId(getIntent());
        if (patient == null) { fecharPacienteNaoEncontrado(); return; }
        own = Sessao.getPerfil() == Perfil.PACIENTE;
        if (state != null) { cameraName = state.getString("cameraName"); retryId = state.getString("retryId"); }
        screen = new RecordsScreen(this, getString(R.string.evolucao_corporal)); setContentView(screen.root);
        vm = new ViewModelProvider(this).get(RecordsViewModel.class);
        if (own) {
            MaterialButton weight = screen.button("Registrar peso", v -> registrarDialog(RecordDialogs.edit(this, vm, RecordsApi.WEIGHT, null)));
            weight.setId(R.id.recordAddWeight); screen.actions.addView(weight);
            MaterialButton photo = screen.button("Escolher foto da galeria", v -> choosePhoto(null));
            photo.setId(R.id.recordAddPhoto); screen.actions.addView(photo);
            MaterialButton take = screen.button("Tirar foto", v -> takePhoto());
            take.setId(R.id.btnTirarFoto); screen.actions.addView(take);
        }
        screen.refresh.setOnClickListener(v -> vm.reload());
        vm.changes.observe(this, ignored -> render());
        vm.configure(patient, Sessao.getAccessToken(), false, RecordsApi.PHOTOS, RecordsApi.WEIGHT);
    }
    @Override protected void onResume() { super.onResume(); if (vm != null) vm.reload(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("cameraName", cameraName); out.putString("retryId", retryId); super.onSaveInstanceState(out);
    }
    private File cameraFile() {
        if (cameraName == null || !cameraName.matches("capture-[a-f0-9-]+\\.jpg")) return null;
        return new File(new File(getCacheDir(), "evolution-camera"), cameraName);
    }
    private void takePhoto() {
        try {
            File directory = new File(getCacheDir(), "evolution-camera");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException();
            cameraName = "capture-" + UUID.randomUUID() + ".jpg"; retryId = null;
            camera.launch(FileProvider.getUriForFile(this, getPackageName() + ".evolution-files", cameraFile()));
        } catch (IOException | ActivityNotFoundException e) { vm.report("Não foi possível abrir a câmera. Você pode escolher uma foto da galeria."); }
    }
    private void choosePhoto(String pendingId) {
        retryId = pendingId;
        try { gallery.launch("image/*"); }
        catch (ActivityNotFoundException e) { vm.report("Nenhum aplicativo disponível para selecionar a imagem."); }
    }
    private void render() {
        screen.state(vm); screen.rows.removeAllViews();
        screen.rows.addView(screen.text("Fotos de evolução", 20));
        JSONArray photos = vm.items(RecordsApi.PHOTOS);
        if (photos.length() == 0 && vm.loading == 0) screen.rows.addView(screen.text("Nenhuma foto enviada.", 15));
        for (int i = 0; i < photos.length(); i++) {
            JSONObject photo = photos.optJSONObject(i); if (photo == null) continue;
            String id = photo.optString("id"); boolean stored = "STORED".equals(photo.optString("uploadStatus"));
            LinearLayout card = screen.card(RecordFormatting.time(photo.optString("capturedAt")), stored ? "Foto armazenada" : "Envio pendente");
            if (stored) {
                MaterialButton open = screen.button("Ver foto", v -> preview(id)); open.setEnabled(vm.ready()); card.addView(open);
            } else if (own) {
                MaterialButton retry = screen.button("Enviar foto pendente", v -> choosePhoto(id)); retry.setEnabled(vm.ready()); card.addView(retry);
            }
            if (own) {
                MaterialButton remove = screen.button("Remover foto", v -> registrarDialog(RecordDialogs.remove(this, vm, RecordsApi.PHOTOS, id)));
                remove.setEnabled(vm.ready()); card.addView(remove);
            }
        }
        screen.rows.addView(screen.text("Histórico de peso", 20));
        JSONArray weights = vm.items(RecordsApi.WEIGHT);
        if (weights.length() == 0 && vm.loading == 0) screen.rows.addView(screen.text("Nenhum peso registrado.", 15));
        for (int i = 0; i < weights.length(); i++) {
            JSONObject record = weights.optJSONObject(i); if (record == null) continue;
            LinearLayout card = screen.card(RecordFormatting.value(record, "weightKg") + " kg", RecordFormatting.time(record.optString("measuredAt")) + "\n" + RecordFormatting.value(record, "notes"));
            if (own) {
                MaterialButton edit = screen.button("Editar peso", v -> registrarDialog(RecordDialogs.edit(this, vm, RecordsApi.WEIGHT, record)));
                MaterialButton remove = screen.button("Remover peso", v -> registrarDialog(RecordDialogs.remove(this, vm, RecordsApi.WEIGHT, record.optString("id"))));
                edit.setEnabled(vm.ready()); remove.setEnabled(vm.ready()); card.addView(edit); card.addView(remove);
            }
        }
    }
    private void preview(String id) {
        if (previewLoading) return;
        previewLoading = true; screen.status.setText("Carregando foto…");
        new RecordsApi().download(patient, Sessao.getAccessToken(), id, new RecordsApi.Callback<byte[]>() {
            public void onSuccess(byte[] bytes) {
                BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = 1;
                while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1600) options.inSampleSize *= 2;
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                runOnUiThread(() -> {
                    previewLoading = false;
                    if (isFinishing() || isDestroyed()) { if (bitmap != null) bitmap.recycle(); return; }
                    screen.state(vm);
                    if (bitmap == null) { vm.report("Não foi possível visualizar a imagem."); return; }
                    ImageView image = new ImageView(EvolucaoCorporalActivity.this); image.setImageBitmap(bitmap);
                    image.setAdjustViewBounds(true); image.setContentDescription("Foto de evolução"); image.setMaxHeight(screen.dp(480));
                    registrarDialog(new MaterialAlertDialogBuilder(EvolucaoCorporalActivity.this).setTitle("Foto de evolução").setView(image).setPositiveButton("Fechar", null).show());
                });
            }
            public void onError(String value) { runOnUiThread(() -> { previewLoading = false; if (!isDestroyed()) vm.report(value); }); }
        });
    }
}
