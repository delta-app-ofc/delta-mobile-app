package com.aula.delta_mobile_app;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.aula.delta_mobile_app.splash_screen.DeltaSplashView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.delta_splash_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DeltaSplashView splashView = findViewById(R.id.delta_splash);
        View mainContent = findViewById(R.id.main_content);
        splashView.start(() -> {
            mainContent.setVisibility(View.VISIBLE);
            splashView.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        DeltaSplashView splashView = findViewById(R.id.delta_splash);
        if (splashView != null) {
            splashView.stop();
        }
        super.onDestroy();
    }
}
