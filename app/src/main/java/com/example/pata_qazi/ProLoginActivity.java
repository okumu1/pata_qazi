package com.example.pata_qazi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProLoginActivity extends AppCompatActivity
{
    //linking
    private EditText mEmail, mPassword ;
    private Button mLogin, mRegistration ;

    private FirebaseAuth mAuth ;
    private FirebaseAuth.AuthStateListener firebaseAuthListener ;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pro_login);

        //login status
        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                //store info of current user
                FirebaseUser user = firebaseAuth.getInstance().getCurrentUser();
                if (user != null)
                {
                    Intent intent = new Intent(ProLoginActivity.this, ProMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;

                }

            }
        };


        mEmail = (EditText) findViewById(R.id.email);
        mPassword  = (EditText) findViewById(R.id.password);

        mLogin = (Button) findViewById(R.id.login);
        mRegistration = (Button) findViewById(R.id.registration);


        //registering new user
        mRegistration.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
               //get values of email n password
               final String email = mEmail.getText().toString();
               final String password = mPassword .getText().toString();

               //create new user
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(ProLoginActivity.this, new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (!task.isSuccessful())
                        {
                            Toast.makeText(ProLoginActivity.this, "sign up error", Toast.LENGTH_LONG).show();
                        }
                        else
                            {
                                //add user id to professionals
                                String user_id = mAuth.getCurrentUser().getUid();
                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child("user_id").child("name");
                                current_user_db.setValue(email);

                            }

                    }
                });
            }
        });
        //login as user
        mLogin.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final String email = mEmail.getText().toString();
                final String password = mPassword .getText().toString();
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(ProLoginActivity.this, new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (!task.isSuccessful())
                        {
                            Toast.makeText(ProLoginActivity.this, "sign in error", Toast.LENGTH_LONG).show();
                        }

                    }
                });

            }
        });

    }

    //start listener when prologin is called

    @Override
    protected void onStart()
    {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);

    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mAuth.removeAuthStateListener (firebaseAuthListener);
    }
}


