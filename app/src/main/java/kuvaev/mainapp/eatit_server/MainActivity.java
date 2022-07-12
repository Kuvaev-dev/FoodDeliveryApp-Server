package kuvaev.mainapp.eatit_server;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    Button btnSignIn;
    TextView txtSlogan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSignIn = (Button)findViewById(R.id.btnSignIn);
        txtSlogan = (TextView)findViewById(R.id.txtslogan);

        Typeface typeface = Typeface.createFromAsset(getAssets() , "fonts/NABILA.TTF");
        txtSlogan.setTypeface(typeface);

        btnSignIn.setOnClickListener(v -> {
            Intent signIn = new Intent(MainActivity.this , SignInActivity.class);
            startActivity(signIn);
        });
    }
}