package com.translator;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

//import android.os.Bundle;
//import androidx.appcompat.app.AppCompatActivity;

//import com.google.firebase.samples.apps.mlkit.translate.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, TranslateFragment.newInstance())
                    .commitNow();
        }
    }
}

