package kuvaev.mainapp.eatit_server;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.Objects;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.User;

public class SignInActivity extends AppCompatActivity {
    Button btnSignIn;
    MaterialEditText edtPhone , edtPassword;

    FirebaseDatabase db;
    DatabaseReference users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        btnSignIn = findViewById(R.id.btnSignIn);

        //Init FireBase
        db = FirebaseDatabase.getInstance();
        users = db.getReference("User");

        btnSignIn.setOnClickListener(v -> signInUser(Objects.requireNonNull(edtPhone.getText()).toString() , Objects.requireNonNull(edtPassword.getText()).toString()));
    }

    private void signInUser(String phone, String password) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please waiting...");
        progressDialog.show();

        final String localPhone = phone;
        final String localPassword = password;
        users.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.child(localPhone).exists()){

                    progressDialog.dismiss();
                    User user = dataSnapshot.child(localPhone).getValue(User.class);
                    assert user != null;
                    user.setPhone(localPhone);

                    if (Boolean.parseBoolean(user.getIsStaff())){  //If IsStaff == true
                        if (user.getPassword().equals(localPassword)){
                            Intent login = new Intent(SignInActivity.this , Home.class);
                            Common.currentUser = user;
                            startActivity(login);
                            finish();
                        }
                        else
                            Toast.makeText(SignInActivity.this, "Wroning password !", Toast.LENGTH_SHORT).show();
                    }
                    else
                        Toast.makeText(SignInActivity.this, "Please login with staff account", Toast.LENGTH_SHORT).show();
                }
                else{
                    progressDialog.dismiss();
                    Toast.makeText(SignInActivity.this, "User not exist in Database", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}