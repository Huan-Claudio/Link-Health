package br.edu.pucgoias.app.ui.paciente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Paciente;

/** Figma: "Dietitian dashboard" (paciente) – convites de nutricionistas. */
public class ConvitesActivity extends BaseActivity {

    private LinearLayout container;
    private TextView tvVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convites);

        String nome = Sessao.getNomeUsuario();
        ((TextView) findViewById(R.id.tvAvatar)).setText(Paciente.iniciais(nome));
        ((TextView) findViewById(R.id.tvOla)).setText(getString(R.string.ola_nome, nome));
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        container = findViewById(R.id.container);
        tvVazio = findViewById(R.id.tvVazio);
        montarLista();
    }

    private void montarLista() {
        container.removeAllViews();
        List<String> convites = MockData.getConvites();
        tvVazio.setVisibility(convites.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (String nutricionista : convites) {
            View item = inflater.inflate(R.layout.item_convite, container, false);
            ((TextView) item.findViewById(R.id.tvNome)).setText(nutricionista);
            item.findViewById(R.id.btnAdicionar).setOnClickListener(v -> {
                // TODO: registrar o aceite do convite no back-end.
                convites.remove(nutricionista);
                toast(getString(R.string.convite_aceito, nutricionista));
                montarLista();
            });
            container.addView(item);
        }
    }
}
