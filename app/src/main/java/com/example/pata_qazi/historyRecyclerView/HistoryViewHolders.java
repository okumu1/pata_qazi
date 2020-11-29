package com.example.pata_qazi.historyRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pata_qazi.HistorySingleActivity;
import com.example.pata_qazi.R;

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener
{
    public TextView jobId;
    public TextView time;
    public HistoryViewHolders(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        jobId = (TextView) itemView.findViewById(R.id.jobId);
        time = (TextView) itemView.findViewById(R.id.time);
    }

    @Override
    public void onClick(View v)
    {
        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("jobId", jobId.getText().toString());
        intent.putExtras(b);
        v.getContext().startActivity(intent);
    }
}
