package com.example.pata_qazi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProSettingsActivity extends AppCompatActivity
{

    private EditText mNameField, mPhoneField, mCategoryField ;

    private Button mBack, mConfirm;

    // private ImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mProDatabase;

    private String userID;
    private String mName;
    private String mPhone;
    private String mCategory;
    private String mService;

    //private String mProfileImageUrl;
    // private  Uri resultUri;

    private RadioGroup mRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pro_settings);

        mNameField = (EditText) findViewById(R.id.name);
        mPhoneField = (EditText) findViewById(R.id.phone);
        mCategoryField = (EditText) findViewById(R.id.category);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        mBack = (Button) findViewById(R.id.back);
        mConfirm = (Button) findViewById(R.id.confirm);

        mAuth = FirebaseAuth.getInstance();
        userID =mAuth.getCurrentUser().getUid();
        mProDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(userID);

        getUserInfo();

        mConfirm.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                saveUserInformation();
            }
        });

        mBack.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
                return;
            }
        });


    }

    //getting employer info
    private void getUserInfo()
    {
        mProDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    //change into name from the db
                    if(map.get("name") !=null)
                    {
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if(map.get("phone") !=null)
                    {
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }

                    if(map.get("category") !=null)
                    {
                        mCategory = map.get("category").toString();
                        mCategoryField.setText(mCategory);
                    }
                    if(map.get("service") !=null)
                    {
                        mService = map.get("service").toString();
                        switch (mService)
                        {
                            case"HomeCare":
                                mRadioGroup.check(R.id.HomeCare);
                                break;
                            case"Maintenance":
                                mRadioGroup.check(R.id.Maintenance);
                                break;
                            case"Freelance":
                                mRadioGroup.check(R.id.Freelance);
                                break;
                        }
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation()
    {
        mName = mNameField.getText().toString();
        mPhone = mPhoneField.getText().toString();
        mCategory = mCategoryField.getText().toString();

        int selectId = mRadioGroup.getCheckedRadioButtonId();

        final RadioButton radioButton = (RadioButton) findViewById(selectId);

        if (radioButton.getText() == null)
        {
            return;
        }

        mService = radioButton.getText().toString();

        //hashmap cuz of different info being saved
        Map userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);
        userInfo.put("category", mCategory);
        userInfo.put("service", mService);
        mProDatabase.updateChildren(userInfo);

        //saving to firebase

        finish();

    }
}