package br.edu.pucgoias.app.ui.comum;

import android.content.ActivityNotFoundException;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import br.edu.pucgoias.app.*;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.PatientNavigation;
import br.edu.pucgoias.app.util.TextoAlterado;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.UUID;

public class NovaEvolucaoActivity extends BaseActivity {
    private AssessmentDraftViewModel vm;
    private RecordsScreen screen;
    private UUID patient;
    private final ImageView[] previews = new ImageView[3];
    private final TextView[] labels = new TextView[3];
    private final byte[][] displayed = new byte[3][];
    private LinearLayout weightPanel;
    private MaterialButton save;
    private int slot;
    private String cameraName;
    private final ActivityResultLauncher<String> gallery =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null && vm != null)
                            vm.prepare(getContentResolver(), uri, slot, null);
                    });
    private final ActivityResultLauncher<Uri> camera =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    ok -> {
                        File file = cameraFile();
                        if (ok && file != null && vm != null)
                            vm.prepare(getContentResolver(), Uri.fromFile(file), slot, file);
                        else if (file != null) file.delete();
                        cameraName = null;
                    });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        patient = PatientNavigation.patientId(getIntent());
        if (patient == null || Sessao.getPerfil() != Perfil.PACIENTE) {
            finish();
            return;
        }
        if (state != null) {
            slot = state.getInt("slot");
            cameraName = state.getString("cameraName");
        }
        vm = new ViewModelProvider(this).get(AssessmentDraftViewModel.class);
        screen = new RecordsScreen(this, "Novo registro de evolução");
        setContentView(screen.root);
        screen.refresh.setVisibility(View.GONE);
        screen.rows.addView(screen.text("Três fotos, um registro", 22));
        screen.rows.addView(
                screen.text(
                        "Adicione uma foto de cada posição. Depois informe seu peso para salvar"
                            + " tudo junto.",
                        14));
        LinearLayout photos = screen.column();
        photos.setOrientation(LinearLayout.HORIZONTAL);
        screen.rows.addView(photos);
        String[] names = {"Frente", "Lado", "Costas"};
        int[] ids = {R.id.assessmentFront, R.id.assessmentSide, R.id.assessmentBack};
        for (int i = 0; i < 3; i++) {
            final int index = i;
            LinearLayout cell = screen.column();
            cell.setId(ids[i]);
            cell.setBackgroundResource(R.drawable.bg_card);
            cell.setPadding(screen.dp(5), screen.dp(8), screen.dp(5), screen.dp(8));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
            p.setMargins(screen.dp(3), screen.dp(12), screen.dp(3), screen.dp(12));
            photos.addView(cell, p);
            TextView title = screen.text(names[i], 16);
            title.setGravity(17);
            cell.addView(title);
            previews[i] = new ImageView(this);
            previews[i].setImageResource(R.drawable.ic_camera);
            previews[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
            previews[i].setContentDescription("Foto de " + names[i]);
            cell.addView(previews[i], new LinearLayout.LayoutParams(-1, screen.dp(138)));
            labels[i] = screen.text("Adicionar foto", 12);
            labels[i].setGravity(17);
            cell.addView(labels[i]);
            cell.setOnClickListener(v -> choose(index));
        }
        weightPanel = screen.column();
        screen.rows.addView(weightPanel);
        weightPanel.addView(screen.text("Seu peso nesta avaliação", 18));
        EditText weight = new EditText(this);
        weight.setId(R.id.assessmentWeight);
        weight.setHint("Peso em kg");
        weight.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        weight.setText(vm.weight);
        weight.addTextChangedListener(new TextoAlterado(vm::changedWeight));
        weightPanel.addView(weight);
        weightPanel.addView(
                screen.text("A data e a hora são registradas ao salvar a avaliação.", 13));
        save = screen.button("Salvar evolução", v -> vm.save(patient, Sessao.getAccessToken()));
        save.setId(R.id.assessmentSave);
        weightPanel.addView(save);
        vm.changes.observe(this, ignored -> render());
    }

    private void render() {
        if (vm.done) {
            toast("Evolução registrada.");
            finish();
            return;
        }
        screen.status.setText(
                !vm.error.isEmpty()
                        ? vm.error
                        : vm.busy
                                ? "Salvando fotos e peso…"
                                : vm.preparing
                                        ? "Preparando foto…"
                                        : vm.count() + " de 3 fotos adicionadas");
        weightPanel.setVisibility(vm.count() == 3 ? View.VISIBLE : View.GONE);
        save.setEnabled(!vm.busy && !vm.preparing);
        for (int i = 0; i < 3; i++) {
            ((View) previews[i].getParent()).setEnabled(!vm.busy && !vm.preparing);
            if (displayed[i] != vm.images[i]) {
                displayed[i] = vm.images[i];
                if (displayed[i] != null) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inSampleSize = 4;
                    previews[i].setImageBitmap(
                            BitmapFactory.decodeByteArray(displayed[i], 0, displayed[i].length, o));
                    labels[i].setText("Trocar foto");
                }
            }
        }
        weightPanel.getChildAt(1).setEnabled(!vm.busy && !vm.preparing);
    }

    private void choose(int index) {
        slot = index;
        registrarDialog(
                new MaterialAlertDialogBuilder(this)
                        .setTitle(
                                new String[] {"Foto de frente", "Foto de lado", "Foto de costas"}
                                        [index])
                        .setItems(
                                new String[] {"Tirar foto", "Escolher da galeria"},
                                (d, n) -> {
                                    if (n == 0) capture();
                                    else
                                        try {
                                            gallery.launch("image/*");
                                        } catch (ActivityNotFoundException e) {
                                            vm.report("Galeria indisponível.");
                                        }
                                })
                        .show());
    }

    private File cameraFile() {
        return cameraName != null && cameraName.matches("capture-[a-f0-9-]+\\.jpg")
                ? new File(new File(getCacheDir(), "evolution-camera"), cameraName)
                : null;
    }

    private void capture() {
        try {
            File folder = new File(getCacheDir(), "evolution-camera");
            if (!folder.exists() && !folder.mkdirs()) throw new java.io.IOException();
            cameraName = "capture-" + UUID.randomUUID() + ".jpg";
            camera.launch(
                    FileProvider.getUriForFile(
                            this, getPackageName() + ".evolution-files", cameraFile()));
        } catch (Exception e) {
            vm.report("Não foi possível abrir a câmera. Escolha uma imagem da galeria.");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putInt("slot", slot);
        out.putString("cameraName", cameraName);
        super.onSaveInstanceState(out);
    }
}
