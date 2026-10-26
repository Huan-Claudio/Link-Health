package br.edu.pucgoias.app.ui.comum;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.*;

import androidx.lifecycle.*;

import br.edu.pucgoias.app.data.remote.RecordsApi;

import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Executors;

/** Um único envio contém as três imagens e o peso. O identificador é mantido nas tentativas. */
public class AssessmentDraftViewModel extends ViewModel {
    public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    public final byte[][] images = new byte[3][];
    public String weight = "", error = "";
    public boolean preparing, busy, done;
    private UUID requestId = UUID.randomUUID();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean cleared;
    private static final java.util.concurrent.ExecutorService FILES =
            Executors.newSingleThreadExecutor();

    public int count() {
        int n = 0;
        for (byte[] image : images) if (image != null) n++;
        return n;
    }

    public void changedWeight(String value) {
        if (!weight.equals(value)) {
            weight = value;
            requestId = UUID.randomUUID();
        }
    }

    public void prepare(ContentResolver resolver, Uri uri, int slot, File temporary) {
        if (busy || preparing || slot < 0 || slot > 2) return;
        preparing = true;
        error = "";
        notifyChange();
        FILES.execute(
                () -> {
                    try {
                        byte[] value = PhotoPreparation.read(resolver, uri, "image/jpeg");
                        main.post(
                                () -> {
                                    if (cleared) return;
                                    images[slot] = value;
                                    requestId = UUID.randomUUID();
                                    preparing = false;
                                    notifyChange();
                                });
                    } catch (Exception e) {
                        main.post(
                                () -> {
                                    if (cleared) return;
                                    error =
                                            "Não foi possível preparar a foto. Escolha outra"
                                                + " imagem.";
                                    preparing = false;
                                    notifyChange();
                                });
                    } finally {
                        if (temporary != null) temporary.delete();
                    }
                });
    }

    public void save(UUID patient, String token) {
        if (busy || preparing) return;
        if (count() != 3) {
            error = "Adicione as fotos de frente, de lado e de costas.";
            notifyChange();
            return;
        }
        try {
            BigDecimal kg = new BigDecimal(weight.trim().replace(',', '.'));
            if (kg.compareTo(BigDecimal.ONE) < 0
                    || kg.compareTo(new BigDecimal("500")) > 0
                    || kg.scale() > 2) throw new IllegalArgumentException();
            JSONObject details =
                    new JSONObject().put("requestId", requestId.toString()).put("weightKg", kg);
            busy = true;
            error = "";
            notifyChange();
            new RecordsApi()
                    .assessment(
                            patient,
                            token,
                            details,
                            images,
                            new RecordsApi.Callback<JSONObject>() {
                                public void onSuccess(JSONObject value) {
                                    main.post(
                                            () -> {
                                                if (cleared) return;
                                                busy = false;
                                                done = true;
                                                notifyChange();
                                            });
                                }

                                public void onError(String value) {
                                    main.post(
                                            () -> {
                                                if (cleared) return;
                                                busy = false;
                                                error = value;
                                                notifyChange();
                                            });
                                }
                            });
        } catch (Exception e) {
            error = "Informe seu peso de 1 a 500 kg, com até duas casas decimais.";
            notifyChange();
        }
    }

    public void report(String message) {
        error = message;
        notifyChange();
    }

    private void notifyChange() {
        changes.setValue(changes.getValue() + 1);
    }

    @Override
    protected void onCleared() {
        cleared = true;
    }
}
