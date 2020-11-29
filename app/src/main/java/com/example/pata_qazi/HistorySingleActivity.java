package com.example.pata_qazi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener
{
    private String jobId, currentUserId, employerId, proId, userProOrEmployer ;

    private TextView jobLocation;
    private TextView jobDistance;
    private TextView jobDate;
    private TextView userName;
    private TextView userPhone;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;

    private DatabaseReference historyJobInfoDb;

    private LatLng jobLatLng, employerLatLng;

    private RatingBar mRatingBar;

    private String distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);
        polylines = new ArrayList<>();

        jobId = getIntent().getExtras().getString("jobId");

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        jobLocation = (TextView) findViewById(R.id.jobLocation);
        jobDistance = (TextView) findViewById(R.id.jobDistance);
        jobDate = (TextView) findViewById(R.id.jobDate);
        userName = (TextView) findViewById(R.id.userName);
        userPhone = (TextView) findViewById(R.id.userPhone);

        mRatingBar =(RatingBar) findViewById(R.id.ratingBar);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyJobInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(jobId);
        getJobInformation();

    }

    private void getJobInformation()
    {
        historyJobInfoDb.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    for (DataSnapshot child:dataSnapshot.getChildren())
                    {
                        if (child.getKey().equals("employer"))
                        {
                            employerId = child.getValue().toString();
                            if (!employerId.equals(currentUserId))
                            {
                                userProOrEmployer = "Professionals";
                                getUserInformation ("Employer", employerId);
                            }
                        }
                        if (child.getKey().equals("professional"))
                        {
                            proId = child.getValue().toString();
                            if (!proId.equals(currentUserId))
                            {
                                userProOrEmployer = "Employer";
                                getUserInformation ("Professionals", proId);
                                displayEmployerRelatedObjects();
                            }
                        }
                        if (child.getKey().equals("timestamp"))
                        {
                            jobDate.setText(getDate(Long.valueOf(child.getValue().toString())));

                        }
                        if (child.getKey().equals("rating"))
                        {
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }
                        if (child.getKey().equals("distance"))
                        {
                           distance = child.getValue().toString();
                           jobDistance.setText(distance.substring(0, Math.min(distance.length(),5)) + "km");
                        }

                        if (child.getKey().equals("location"))
                        {
                            jobLocation.setText(getDate(Long.valueOf(child.getValue().toString())));

                        }
                        if (child.getKey().equals("site"))
                        {
                            employerLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            jobLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()),Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if (jobLatLng != new LatLng(0,0))
                            {
                                getRouteToMarker();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }

    private void displayEmployerRelatedObjects()
    {
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyJobInfoDb.child("rating").setValue(rating);
                DatabaseReference mDriverRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(proId).child("rating");
                mDriverRatingDb.child(jobId).setValue(rating);
            }
        });
    }

    private void getUserInformation(String otherUserProOrEmployer, String otherUserId)
    {
        DatabaseReference motherUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserProOrEmployer).child(otherUserId);
        motherUserDb.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null)
                    {
                        userName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null)
                    {
                        userPhone.setText(map.get("phone").toString());
                    }
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
    private void getRouteToMarker()
    {
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.WALKING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(employerLatLng, jobLatLng)
                    .build();
            routing.execute();

    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
    }

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e)
    {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex)
    {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(employerLatLng);
        builder.include(jobLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cameraUpdate);
        mMap.addMarker(new MarkerOptions().position(employerLatLng).title("employer location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.jobicon)));
        mMap.addMarker(new MarkerOptions().position(jobLatLng).title("site"));

        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingCancelled() {

    }

    //clearing route from map
    private void erasePolylines()
    {
        for (Polyline line : polylines)
        {
            line.remove();
        }
        polylines.clear();
    }
}
