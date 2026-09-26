package br.edu.pucgoias.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import br.edu.pucgoias.app.ui.auth.LoginActivity;

/**
 * Ponto de entrada do app. Apenas redireciona para a tela de Login.
 * (Quando houver login persistido, aqui é o lugar para decidir qual Home abrir.)
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
