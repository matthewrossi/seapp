package com.example.showcaseapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Class[] classes = {
            UseCase1Activity.class, UseCase2Activity.class, UseCase3Activity.class
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void goToUseCase(View view) {
        Button clickedButton = (Button) view;
        String text = clickedButton.getText().toString();
        int idx = Character.getNumericValue(text.charAt(text.indexOf("#") + 1)) - 1;
        Intent toUseCase = new Intent(MainActivity.this, classes[idx]);
        startActivity(toUseCase);
    }
}