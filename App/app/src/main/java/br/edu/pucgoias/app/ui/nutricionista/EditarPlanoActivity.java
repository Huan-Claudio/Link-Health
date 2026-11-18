package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.data.remote.ClinicalApi;
import br.edu.pucgoias.app.ui.comum.ClinicalScreen;
import br.edu.pucgoias.app.ui.nutricionista.edicao.PlansViewModel;
import br.edu.pucgoias.app.util.PatientNavigation;
import br.edu.pucgoias.app.util.TextoAlterado;
import org.json.*;
import java.util.UUID;

/** Rascunhos, prescrição e histórico usando o acompanhamento real. */
public class EditarPlanoActivity extends BaseActivity {
    private PlansViewModel model;
    private ClinicalScreen screen;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        UUID patient = PatientNavigation.patientId(getIntent());
        if (patient == null || !Sessao.temSessaoRemota() || Sessao.getPerfil() != Perfil.NUTRICIONISTA) { finish(); return; }
        screen = new ClinicalScreen(this, "Planos · " + PatientNavigation.name(getIntent()));
        model = new ViewModelProvider(this).get(PlansViewModel.class);
        model.changes.observe(this, ignored -> render());
        model.start(patient.toString());
    }
    private void render() {
        screen.content.removeAllViews();
        if (model.busy) screen.text("Aguarde a confirmação do servidor...", false);
        if (model.error != null) screen.text(model.error, false);
        screen.button("Atualizar planos", !model.busy, v -> model.load());
        screen.button("Criar novo rascunho", !model.busy && model.followUp != null, v -> model.create());
        if (model.loaded && model.plans.length() == 0) screen.text("Nenhum plano. Crie um rascunho para começar.", false);
        for (int i = 0; i < model.plans.length(); i++) {
            JSONObject plan = model.plans.optJSONObject(i);
            String id = plan.optString("id");
            String status = plan.optBoolean("active") ? "Ativo" : plan.optBoolean("published") ? "Histórico" : "Rascunho";
            screen.button("Plano " + id.substring(0, 8) + " · " + status, !model.busy, v -> model.select(id));
        }
        JSONObject plan = model.selected();
        if (plan == null) return;
        boolean editable = !plan.optBoolean("published") && !model.busy;
        String path = "/api/v1/meal-plans/" + plan.optString("id");
        screen.text(plan.optBoolean("active") ? "Plano ativo" : plan.optBoolean("published") ? "Histórico preservado" : "Editar rascunho", true);
        screen.text("A publicação exige pelo menos três refeições, cada uma com um item. Planos publicados são preservados; copie para ajustar a dieta.", false);
        screen.button("Copiar como novo rascunho", !model.busy, v -> model.mutate("POST", path + "/copy", null));
        if (!plan.optBoolean("published")) {
            screen.button("Adicionar refeição", editable, v -> mealDialog(path, null));
            screen.button("Publicar plano", editable, v -> confirm("Publicar este plano?", "Ele substituirá o plano ativo deste acompanhamento.", () -> model.mutate("POST", path + "/activate", null)));
            screen.button("Excluir rascunho", editable, v -> confirm("Excluir rascunho?", "As refeições e os itens deste rascunho serão removidos.", () -> model.mutate("DELETE", path, null)));
        }
        JSONArray meals = plan.optJSONArray("meals");
        if (meals == null) return;
        for (int i = 0; i < meals.length(); i++) {
            JSONObject meal = meals.optJSONObject(i);
            String mealPath = path + "/meals/" + meal.optString("id");
            screen.text(meal.optString("name") + " · " + meal.optString("time"), true);
            if (!plan.optBoolean("published")) {
                screen.button("Editar refeição: " + meal.optString("name"), editable, v -> mealDialog(path, meal));
                screen.button("Adicionar item em " + meal.optString("name"), editable, v -> itemDialog(mealPath, null));
            }
            JSONArray items = meal.optJSONArray("items");
            if (items != null) for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.optJSONObject(j);
                screen.text(item.optString("description"), false);
                if (!plan.optBoolean("published")) {
                    screen.button("Editar item: " + item.optString("description"), editable, v -> itemDialog(mealPath, item));
                    screen.button("Remover item: " + item.optString("description"), editable, v -> confirm("Remover item?", item.optString("description"), () -> model.mutate("DELETE", mealPath + "/items/" + item.optString("id"), null)));
                }
            }
            if (!plan.optBoolean("published")) screen.button("Remover refeição: " + meal.optString("name"), editable,
                    v -> confirm("Remover refeição?", "Todos os itens desta refeição serão removidos.", () -> model.mutate("DELETE", mealPath, null)));
        }
    }
    private void confirm(String title, String message, Runnable action) {
        registrarDialog(new MaterialAlertDialogBuilder(this).setTitle(title).setMessage(message)
                .setNegativeButton("Cancelar", null).setPositiveButton("Confirmar", (d, w) -> action.run()).show());
    }
    private void mealDialog(String planPath, JSONObject meal) {
        String path = planPath + "/meals" + (meal == null ? "" : "/" + meal.optString("id"));
        form(path, meal == null ? "Adicionar refeição" : "Editar refeição", meal == null ? "" : meal.optString("name"),
                meal == null ? "08:00" : meal.optString("time").substring(0, 5), true,
                (name, time) -> model.mutate(meal == null ? "POST" : "PUT", path, ClinicalApi.body("name", name, "time", time + ":00")));
    }
    private void itemDialog(String mealPath, JSONObject item) {
        String path = mealPath + "/items" + (item == null ? "" : "/" + item.optString("id"));
        form(path, item == null ? "Adicionar item" : "Editar item", item == null ? "" : item.optString("description"), "", false,
                (description, ignored) -> model.mutate(item == null ? "POST" : "PUT", path, ClinicalApi.body("description", description)));
    }
    private interface Save { void run(String text, String time); }
    private void form(String key, String title, String text, String time, boolean meal, Save save) {
        if (!key.equals(model.draftKey)) { model.draftKey = key; model.draftName = text; model.draftTime = time; }
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        int padding = screen.dp(20); layout.setPadding(padding, 0, padding, 0);
        EditText name = new EditText(this); name.setHint(meal ? "Nome da refeição" : "Descrição do item");
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setText(model.draftName); name.addTextChangedListener(new TextoAlterado(value -> model.draftName = value)); layout.addView(name);
        EditText hour = new EditText(this); hour.setHint("Horário (HH:mm)"); hour.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        hour.setText(model.draftTime); hour.addTextChangedListener(new TextoAlterado(value -> model.draftTime = value));
        if (meal) layout.addView(hour);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(title).setView(layout)
                .setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create();
        registrarDialog(dialog); dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = name.getText().toString().trim(), h = hour.getText().toString().trim();
            int max = meal ? 50 : 150;
            if (value.isEmpty() || value.length() > max) { name.setError("Informe de 1 a " + max + " caracteres."); return; }
            if (meal && !h.matches("([01][0-9]|2[0-3]):[0-5][0-9]")) { hour.setError("Use HH:mm, por exemplo 08:30."); return; }
            save.run(value, h); dialog.dismiss();
        });
    }
}
