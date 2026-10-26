package br.edu.pucgoias.app.ui.comum;

import android.app.Activity;
import android.graphics.*;
import android.view.*;
import android.view.animation.TranslateAnimation;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import br.edu.pucgoias.app.data.remote.RecordsApi;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.*;

import java.util.UUID;

/** Álbum de três páginas autenticadas, com posição, peso e horário sempre identificados. */
public final class EvolutionBook {
    private EvolutionBook() {}

    public static void thumbnail(
            Activity a, UUID patient, String token, String photo, ImageView target, int max) {
        new RecordsApi()
                .download(
                        patient,
                        token,
                        photo,
                        new RecordsApi.Callback<byte[]>() {
                            public void onSuccess(byte[] bytes) {
                                Bitmap b = decode(bytes, max);
                                a.runOnUiThread(
                                        () -> {
                                            if (a.isDestroyed()) {
                                                if (b != null) b.recycle();
                                                return;
                                            }
                                            if (b != null) target.setImageBitmap(b);
                                        });
                            }

                            public void onError(String value) {
                                a.runOnUiThread(
                                        () -> {
                                            if (!a.isDestroyed())
                                                target.setContentDescription(
                                                        "Não foi possível carregar a miniatura."
                                                            + " Abra a foto para tentar"
                                                            + " novamente.");
                                        });
                            }
                        });
    }

    private static Bitmap decode(byte[] bytes, int max) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / sample > max) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
    }

    public static AlertDialog show(
            Activity a, UUID patient, String token, JSONObject record, int initial) {
        int pad = (int) (16 * a.getResources().getDisplayMetrics().density);
        LinearLayout body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(pad, 0, pad, 0);
        TextView label = new TextView(a);
        label.setTextSize(18);
        label.setGravity(17);
        body.addView(label);
        ViewFlipper pages = new ViewFlipper(a);
        body.addView(
                pages,
                new LinearLayout.LayoutParams(
                        -1, (int) (300 * a.getResources().getDisplayMetrics().density)));
        JSONArray photos = record.optJSONArray("photos");
        String[] names = {"Frente", "Lado", "Costas"};
        ImageView[] images = new ImageView[3];
        TextView[] messages = new TextView[3];
        for (int i = 0; i < 3; i++) {
            FrameLayout frame = new FrameLayout(a);
            pages.addView(frame);
            images[i] = new ImageView(a);
            images[i].setScaleType(ImageView.ScaleType.FIT_CENTER);
            images[i].setContentDescription("Foto de " + names[i]);
            frame.addView(images[i], new FrameLayout.LayoutParams(-1, -1));
            messages[i] = new TextView(a);
            messages[i].setGravity(17);
            messages[i].setText("Carregando foto…");
            frame.addView(messages[i], new FrameLayout.LayoutParams(-1, -1));
        }
        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(17);
        body.addView(nav);
        Button previous = new com.google.android.material.button.MaterialButton(a,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);
        previous.setText("‹");
        previous.setContentDescription("Foto anterior");
        previous.setMinWidth(0);
        nav.addView(previous,new LinearLayout.LayoutParams(pad*3,pad*3));
        TextView number = new TextView(a);
        number.setGravity(17);
        nav.addView(number, new LinearLayout.LayoutParams(0, -2, 1));
        Button next = new com.google.android.material.button.MaterialButton(a,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);
        next.setText("›");
        next.setContentDescription("Próxima foto");
        next.setMinWidth(0);
        nav.addView(next,new LinearLayout.LayoutParams(pad*3,pad*3));
        TextView hint=new TextView(a);
        hint.setText("Deslize para ver as outras posições");
        hint.setTextSize(12);
        hint.setGravity(android.view.Gravity.CENTER);
        body.addView(hint);
        TextView info = new TextView(a);
        info.setTextSize(16);
        info.setPadding(0, pad, 0, pad);
        info.setText(
                record.optString("weightKg")
                        + " kg\n"
                        + RecordFormatting.time(record.optString("recordedAt")));
        body.addView(info);
        int[] current = {Math.max(0, Math.min(2, initial))};
        pages.setDisplayedChild(current[0]);
        Runnable update =
                () -> {
                    label.setText(names[current[0]]);
                    number.setText((current[0] + 1) + " / 3");
                    previous.setEnabled(current[0] > 0);
                    next.setEnabled(current[0] < 2);
                };
        update.run();
        java.util.function.IntConsumer move =
                direction -> {
                    int target = current[0] + direction;
                    if (target < 0 || target > 2) return;
                    TranslateAnimation in =
                            new TranslateAnimation(1, direction > 0 ? 1 : -1, 1, 0, 1, 0, 1, 0);
                    in.setDuration(180);
                    TranslateAnimation out =
                            new TranslateAnimation(1, 0, 1, direction > 0 ? -1 : 1, 1, 0, 1, 0);
                    out.setDuration(180);
                    pages.setInAnimation(in);
                    pages.setOutAnimation(out);
                    current[0] = target;
                    pages.setDisplayedChild(target);
                    update.run();
                };
        previous.setOnClickListener(v -> move.accept(-1));
        next.setOnClickListener(v -> move.accept(1));
        GestureDetector gesture =
                new GestureDetector(
                        a,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onDown(android.view.MotionEvent e) {
                                return true;
                            }

                            @Override
                            public boolean onFling(
                                    android.view.MotionEvent first,
                                    android.view.MotionEvent last,
                                    float vx,
                                    float vy) {
                                if (first != null
                                        && Math.abs(last.getX() - first.getX()) > 60
                                        && Math.abs(vx) > Math.abs(vy)) {
                                    move.accept(last.getX() < first.getX() ? 1 : -1);
                                    return true;
                                }
                                return false;
                            }
                        });
        for (int i = 0; i < 3; i++) {
            images[i].setOnTouchListener((v, event) -> gesture.onTouchEvent(event));
            messages[i].setOnTouchListener((v, event) -> gesture.onTouchEvent(event));
        }
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(a)
                        .setTitle("Registro de evolução")
                        .setView(body)
                        .setPositiveButton("Fechar", null)
                        .create();
        dialog.show();
        for (int i = 0; i < 3; i++) {
            final int index = i;
            JSONObject p = photos == null ? null : photos.optJSONObject(i);
            if (p == null) {
                messages[i].setText("Foto indisponível.");
                continue;
            }
            new RecordsApi()
                    .download(
                            patient,
                            token,
                            p.optString("photoId"),
                            new RecordsApi.Callback<byte[]>() {
                                public void onSuccess(byte[] bytes) {
                                    Bitmap bitmap = decode(bytes, 1600);
                                    a.runOnUiThread(
                                            () -> {
                                                if (a.isDestroyed() || !dialog.isShowing()) {
                                                    if (bitmap != null) bitmap.recycle();
                                                    return;
                                                }
                                                if (bitmap != null) {
                                                    images[index].setImageBitmap(bitmap);
                                                    messages[index].setVisibility(View.GONE);
                                                } else messages[index].setText("Imagem inválida.");
                                            });
                                }

                                public void onError(String value) {
                                    a.runOnUiThread(
                                            () -> {
                                                if (dialog.isShowing())
                                                    messages[index].setText(value);
                                            });
                                }
                            });
        }
        return dialog;
    }
}
