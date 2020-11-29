package com.example.pata_qazi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;



import android.os.Bundle;
import android.text.format.DateFormat;

import com.example.pata_qazi.historyRecyclerView.HistoryAdapter;
import com.example.pata_qazi.historyRecyclerView.HistoryObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



import java.util.ArrayList;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class HistoryActivity extends AppCompatActivity
{
    private String employerOrPro, userId;

    private RecyclerView mHistoryRecyclerView;
    private RecyclerView.Adapter mHistoyAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mHistoryRecyclerView = (RecyclerView) findViewById(R.id.historyRecyclerView);
        mHistoryRecyclerView.setNestedScrollingEnabled(false);
        mHistoryRecyclerView.setHasFixedSize(true);
        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
        mHistoyAdapter = new HistoryAdapter(getDataSetHistory(), HistoryActivity.this);
        mHistoryRecyclerView.setAdapter(mHistoyAdapter);


        employerOrPro = getIntent().getExtras().getString("employerOrPro");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserHistoryIds();

    }

    private void getUserHistoryIds()
    {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(employerOrPro).child(userId).child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    for (DataSnapshot history : dataSnapshot.getChildren())
                    {
                        FetchJobInformation(history.getKey());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }
    private void FetchJobInformation(String jobKey)
    {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(jobKey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    String jobId = dataSnapshot.getKey();
                    Long timestamp = 0L;
                    for (DataSnapshot child : dataSnapshot.getChildren())
                    {
                       if (child.getKey().equals("timestamp"))
                       {
                           timestamp = Long.valueOf(child.getValue().toString());
                       }
                    }
                    HistoryObject obj = new HistoryObject(jobId, getDate(timestamp));
                    resultHistory.add(obj);
                    mHistoyAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }

    private String getDate(Long timestamp)
    {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm",cal).toString();
        return date;
    }

    private ArrayList resultHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDataSetHistory()
    {
        return resultHistory;
    }
}
