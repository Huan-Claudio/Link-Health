package br.edu.pucgoias.app.ui.comum;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ViewHeaderBinding;

/** Elementos compartilhados pelas telas de registros, com estados explícitos e opção de recarregar. */
public final class RecordsScreen {
    public final LinearLayout root, actions, rows;
    public final TextView status;
    public final MaterialButton refresh;
    private final Activity activity;

    public RecordsScreen(Activity activity, String title) {
        this.activity = activity;
        root = column(); root.setBackgroundColor(activity.getColor(R.color.lh_background));
        root.setPadding(dp(20), dp(16), dp(20), dp(12));
        ViewHeaderBinding header = ViewHeaderBinding.inflate(activity.getLayoutInflater(), root, false);
        header.tvTituloHeader.setText(title); header.btnVoltar.setOnClickListener(v -> activity.finish());
        root.addView(header.getRoot());
        status = text("Carregando…", 14); status.setId(R.id.recordsStatus); root.addView(status);
        refresh = button("Atualizar", null); refresh.setId(R.id.recordsRefresh); root.addView(refresh);
        actions = column();
        ScrollView scroll = new ScrollView(activity); scroll.setFillViewport(true);
        LinearLayout content = column(); content.addView(actions);
        rows = column(); content.addView(rows); scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }
    public LinearLayout column() { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.VERTICAL); return view; }
    public TextView text(String value, int size) {
        TextView view = new TextView(activity); view.setText(value); view.setTextSize(size);
        view.setTextColor(activity.getColor(R.color.lh_text_primary)); view.setPadding(0, dp(6), 0, dp(6)); return view;
    }
    public MaterialButton button(String title, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(activity); button.setText(title); button.setMinHeight(dp(48));
        button.setOnClickListener(listener); return button;
    }
    public LinearLayout card(String title, String details) {
        LinearLayout card = column(); card.setBackgroundResource(R.drawable.bg_card); card.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, dp(10), 0, dp(4));
        rows.addView(card, params); card.addView(text(title, 17)); if (!details.isEmpty()) card.addView(text(details, 14)); return card;
    }
    public void state(RecordsViewModel vm) {
        status.setText(!vm.error.isEmpty() ? vm.error : vm.busy ? "Salvando… Aguarde a confirmação." : vm.loading > 0 ? "Carregando…" : "Dados atualizados.");
        status.setTextColor(!vm.error.isEmpty() ? Color.rgb(255, 160, 150) : activity.getColor(R.color.lh_text_primary));
        refresh.setEnabled(vm.ready());
        for (int i = 0; i < actions.getChildCount(); i++) actions.getChildAt(i).setEnabled(vm.ready());
    }
    public int dp(int value) { return (int) (value * activity.getResources().getDisplayMetrics().density); }
}
