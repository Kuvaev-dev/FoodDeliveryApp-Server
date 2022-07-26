package kuvaev.mainapp.eatit_server;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {
    Button btnSignInAsAdmin, btnSignInAsStaff;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        btnSignInAsAdmin = findViewById(R.id.btnSignInAsAdmin);
        btnSignInAsStaff = findViewById(R.id.btnSignInAsStaff);

        btnSignInAsStaff.setOnClickListener(v -> {
            Intent signInAsStaff = new Intent(MainActivity.this, SignInAsStaff.class);
            startActivity(signInAsStaff);
        });
        btnSignInAsAdmin.setOnClickListener(v -> {
            Intent signInAsAdmin = new Intent(MainActivity.this, SignInAsAdmin.class);
            startActivity(signInAsAdmin);
        });
    }
}
