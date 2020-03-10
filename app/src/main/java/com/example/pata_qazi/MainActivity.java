package com.example.pata_qazi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity
{
    private Button mProfessional, mEmployer ;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProfessional = (Button) findViewById(R.id.professional);
        mEmployer = (Button) findViewById(R.id.employer);


        mProfessional.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //for login and registration of pros
                Intent intent = new Intent(MainActivity.this, ProLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        mEmployer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //for login and registration of pros
                Intent intent = new Intent(MainActivity.this, EmployerLoginActivty.class);
                startActivity(intent);
                finish();
                return;
            }
        });

    }
}
