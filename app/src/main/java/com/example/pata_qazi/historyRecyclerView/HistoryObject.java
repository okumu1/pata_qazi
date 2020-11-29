package com.example.pata_qazi.historyRecyclerView;

import com.example.pata_qazi.HistoryActivity;

public class HistoryObject
{
    private String jobId;
    private String time;

    public HistoryObject(String jobId, String time)
    {
        this.jobId = jobId;
        this.time = time;
    }

    public String getJobId(){return jobId;}
    public void setJobId(String jobId) {this.jobId = jobId;}

    public String getTime(){return time;}
    public void setTime(String time) {this.time = time;}

}
