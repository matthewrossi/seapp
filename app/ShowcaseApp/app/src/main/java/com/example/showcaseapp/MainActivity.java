package com.example.showcaseapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void goToUseCase(View view) {
        Button clickedButton = (Button) view;
        String text = clickedButton.getText().toString();
        Intent toUseCase = null;
        switch(text.charAt(text.indexOf("#") + 1)) {
            case '1':
                toUseCase = new Intent(MainActivity.this, UseCase1Activity.class);
                break;
            case '2':
                toUseCase = new Intent(MainActivity.this, UseCase2Activity.class);
                break;
            case '3':
                toUseCase = new Intent(MainActivity.this, UseCase3Activity.class);
                break;
        }
        startActivity(toUseCase);
    }
}