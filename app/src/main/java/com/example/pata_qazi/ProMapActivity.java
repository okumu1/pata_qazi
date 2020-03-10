package com.example.pata_qazi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.Permission;
import java.util.List;
import java.util.Map;

public class ProMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
{



    private GoogleMap mMap;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mlogout;

    private String employerId = "";

    private Boolean isLoggingOut = false;

    private LinearLayout mEmoployerInfo;

    //private ImageView mEmployerProfileImage;

    private TextView mEmployerName, mEmployerPhone, mEmployerLocation;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pro_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ProMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        else
            {
                mapFragment.getMapAsync(this);
            }

        mEmoployerInfo = (LinearLayout) findViewById(R.id.employerInfo);

        mEmployerName = (TextView) findViewById(R.id.employerName);

        mEmployerPhone = (TextView) findViewById(R.id.employerPhone);

        mEmployerLocation = (TextView) findViewById(R.id.employerLocation);

        mlogout = (Button) findViewById(R.id.logout);
        mlogout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                isLoggingOut = true;

                disconnectPro();

                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(ProMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        getAssignedEmployer();

    }

    private void getAssignedEmployer()
    {

        String proId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedEmployerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(proId).child("employerRequest").child("employerJobId");

        assignedEmployerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    employerId = dataSnapshot.getValue().toString();
                    getAssignedEmployerJobLocation();

                    getAssignedEmployerLocation();

                    getAssignedEmployerInfo();
                }

                //called when employer cancels request
                else
                  {
                      employerId = "";

                      if (jobLocationMarker != null)
                      {
                          jobLocationMarker.remove();
                      }

                      if ( assignedEmployerJobLocationRefListener != null)
                      {
                          assignedEmployerJobLocationRef.removeEventListener(assignedEmployerJobLocationRefListener);
                      }
                      mEmoployerInfo.setVisibility(View.GONE);

                      mEmployerName.setText("");
                      mEmployerPhone.setText("");
                      mEmployerLocation.setText("Location: --" );
                  }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }

    Marker jobLocationMarker;

    private DatabaseReference  assignedEmployerJobLocationRef;

    private ValueEventListener assignedEmployerJobLocationRefListener;

    private void  getAssignedEmployerJobLocation()
    {
        assignedEmployerJobLocationRef = FirebaseDatabase.getInstance().getReference().child("employerRequest").child("employerId").child("l");

        assignedEmployerJobLocationRefListener = assignedEmployerJobLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists() && !employerId.equals(""))
                {
                    List <Object> map = (List<Object>) dataSnapshot.getValue();

                    double locationLat = 0;
                    double locationLng = 0;

                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng proLatLng = new LatLng(locationLat,locationLng);

                    jobLocationMarker = mMap.addMarker(new MarkerOptions().position(proLatLng).title("Job location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.jobicon)));
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }

    private void getAssignedEmployerLocation()
    {

        String proId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedEmployerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(proId).child("employerRequest").child("location");

        assignedEmployerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                   String location = dataSnapshot.getValue().toString();

                   mEmployerLocation.setText("Location: " + location);

                }

                //called when employer doesn't set location
                else
                {
                    mEmployerLocation.setText("Location: --" );

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }


    private void getAssignedEmployerInfo()
    {
        mEmoployerInfo.setVisibility(View.VISIBLE);

        DatabaseReference mEmployerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Employer").child(employerId);
        mEmployerDatabase.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    //change into name from the db
                    if(map.get("name") !=null)
                    {
                        mEmployerName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") !=null)
                    {
                       mEmployerPhone.setText(map.get("phone").toString());
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    //validate API client
    protected synchronized void buildGoogleApiClient()
    {
       mGoogleApiClient = new  GoogleApiClient.Builder(this)
               .addConnectionCallbacks(this)
               .addOnConnectionFailedListener(this)
               .addApi(LocationServices.API)
               .build();
       mGoogleApiClient.connect();

    }

    //getting updated location
    @Override
    public void onLocationChanged(Location location)
    {
        if (getApplicationContext()!= null)
        {

            mLastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("prosAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("prosWorking");

            //using geofire to add location data to db
            GeoFire geoFireAvailable = new  GeoFire(refAvailable);
            GeoFire geoFireWorking = new  GeoFire(refWorking);


            switch (employerId)
            {
                case "":
                    geoFireWorking.removeLocation(userId);

                    geoFireAvailable .setLocation(userId, new GeoLocation(location.getLongitude(), location.getAltitude()));
                    break;

                    default:
                        geoFireAvailable.removeLocation(userId);

                        geoFireWorking.setLocation(userId, new GeoLocation(location.getLongitude(), location.getAltitude()));

                        break;
            }

        }

    }

    //when map is called
    @Override
    public void onConnected(@Nullable Bundle bundle)

    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ProMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i)
    {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {

    }

    private void disconnectPro()
    {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("prosAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }


    final int LOCATION_REQUEST_CODE = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
         switch (requestCode)
         {
             case LOCATION_REQUEST_CODE:
                 {
                     if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                     {
                         //incase of issues toa hii
                         MapFragment mapFragment = null;
                         mapFragment.getMapAsync(this);
                     }
                     else
                         {
                             Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();

                         }
                     break;
                 }

         }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
            //pro logging out
        if (!isLoggingOut)
        {
            disconnectPro();
        }

    }
}
