package br.edu.pucgoias.app.ui.comum;

import android.content.Context;
import android.widget.*;
import android.view.View;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.databinding.ViewHeaderBinding;
import com.google.android.material.button.MaterialButton;

/** Estrutura simples de formulários com a paleta e o cabeçalho existentes. */
public final class ClinicalScreen {
    public final LinearLayout content;
    private final Context context;
    public ClinicalScreen(BaseActivity activity, String title) {
        context = activity;
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(context.getColor(R.color.lh_background));
        ViewHeaderBinding header = ViewHeaderBinding.inflate(activity.getLayoutInflater(), root, false);
        header.tvTituloHeader.setText(title);
        header.btnVoltar.setOnClickListener(v -> activity.finish());
        root.addView(header.getRoot());
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        activity.setContentView(root);
    }
    public TextView text(String value, boolean heading) {
        TextView view = new TextView(context);
        view.setText(value); view.setTextSize(heading ? 20 : 16);
        view.setTextColor(context.getColor(heading ? R.color.lh_text_primary : R.color.lh_text_secondary));
        view.setPadding(0, dp(12), 0, dp(8));
        content.addView(view);
        return view;
    }
    public MaterialButton button(String title, boolean enabled, View.OnClickListener action) {
        MaterialButton button = new MaterialButton(context);
        button.setText(title); button.setAllCaps(false); button.setEnabled(enabled);
        button.setOnClickListener(action);
        content.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }
    public int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}
