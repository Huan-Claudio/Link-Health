package br.edu.pucgoias.app.util;

import android.widget.ImageView;

import br.edu.pucgoias.app.R;

/** Ícone redondo de "marcado / não marcado" usado no plano alimentar e na lista de compras. */
public final class IconeCheck {

    private IconeCheck() {
    }

    public static void atualizar(ImageView icone, boolean marcado) {
        icone.setImageResource(marcado ? R.drawable.bg_check_checked : R.drawable.bg_circle_unchecked);
    }
}
