package com.example.mlpj.uberapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    //widgets
    private EditText mEtEmail, mEtPassword;
    private Button mBtnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        final FirebaseAuth mAuth = FirebaseAuth.getInstance();

        mEtEmail = findViewById(R.id.etEmail);
        mEtPassword = findViewById(R.id.etPassword);
        mBtnRegister = findViewById(R.id.btnRegister);

        mBtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEtEmail.getText().toString();
                String password = mEtPassword.getText().toString();

                mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(RegisterActivity.this, "Sign up failed, please try again : " + task.getResult().toString(), Toast.LENGTH_LONG).show();
                        }else{
                            //adding user to firebase database
                            String userId = mAuth.getCurrentUser().getUid();
                            DatabaseReference firebaseDatabase = FirebaseDatabase.getInstance().getReference().child("users").child("customers").child(userId);
                            firebaseDatabase.setValue(true);

                            //starting maps activity
                            Intent intent = new Intent(RegisterActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
            }
        });
    }
}
