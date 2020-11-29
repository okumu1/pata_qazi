package com.example.pata_qazi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProMapActivity extends FragmentActivity implements OnMapReadyCallback, RoutingListener
{

    private GoogleMap mMap;

   // GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    private Button mlogout, mSettings, mJobStatus, mHistory;

    private Switch mWorkingSwitch;

    private int status = 0;

    private String employerId = "", location;
    private LatLng locationLatLng, jobLatLng;

    private float jobDistance;

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
        polylines = new ArrayList<>();


       /* if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ProMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        else */

       mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

       mapFragment.getMapAsync(this);


        mEmoployerInfo = (LinearLayout) findViewById(R.id.employerInfo);

        mEmployerName = (TextView) findViewById(R.id.employerName);

        mEmployerPhone = (TextView) findViewById(R.id.employerPhone);

        mEmployerLocation = (TextView) findViewById(R.id.employerLocation);

        mHistory = (Button) findViewById(R.id.history);

        mWorkingSwitch = (Switch)  findViewById(R.id.workingSwitch);
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    connectPro();
                }
                else
                    {
                        disconnectPro();
                    }
            }
        });

        mSettings = (Button) findViewById(R.id.settings);

        mJobStatus = (Button) findViewById(R.id.jobStatus);
        mJobStatus.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                switch (status)
                {
                    case 1:

                        status=2;
                        erasePolylines();
                        if (locationLatLng.latitude!=0.0 && locationLatLng.longitude!=0.0)
                        {
                            getRouteToMarker(locationLatLng);
                        }
                        mJobStatus.setText("Job completed");

                        break;

                    case 2:
                        recordJob();
                        endJob();
                        break;
                }

            }
        });

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


        mSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(ProMapActivity.this, ProSettingsActivity.class);
                startActivity(intent);
                finish();
                return;

            }
        });

        mHistory.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(ProMapActivity.this, HistoryActivity.class);
                intent.putExtra("employerOrPro", "Professionals");
                startActivity(intent);
                return;
            }
        });

        getAssignedEmployer();
    }

    private void getAssignedEmployer()
    {

        String proId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedEmployerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(proId).child("employerRequest").child("employerJobId");

        assignedEmployerRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    status = 1;
                    employerId = dataSnapshot.getValue().toString();
                    getAssignedEmployerJobLocation();

                    getAssignedEmployerLocation();

                    getAssignedEmployerInfo();
                }

                //called when employer cancels request
                else
                  {
                      endJob();
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
                    LatLng jobLatLng = new LatLng(locationLat,locationLng);

                    jobLocationMarker = mMap.addMarker(new MarkerOptions().position(jobLatLng).title("Job location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.jobicon)));
                    getRouteToMarker(jobLatLng);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }
    //mode of movement

    private void getRouteToMarker(LatLng jobLatLng)
    {
        if(jobLatLng != null && mLastLocation != null ) {

            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), jobLatLng)
                    .build();
            routing.execute();
        }
    }

    private void getAssignedEmployerLocation()
    {

        String proId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedEmployerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(proId).child("employerRequest");

        assignedEmployerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("location")!=null)
                    {
                        location = map.get("location").toString();
                        mEmployerLocation.setText("Location:" + location);
                    }
                    else
                        {
                            mEmployerLocation.setText("Location: --");
                        }

                    Double locationLat = 0.0;
                    Double locationLng = 0.0;
                    if (map.get("locationLat") != null)
                    {
                        locationLat = Double.valueOf(map.get("locationLat").toString());
                    }
                    if (map.get("locationLng") != null)
                    {
                        locationLng = Double.valueOf(map.get("locationLng").toString());
                        locationLatLng = new LatLng(locationLat, locationLng);
                    }

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

    private void endJob()
    {
        mJobStatus.setText("Request accepted");
        erasePolylines();
            String userId = FirebaseAuth.getInstance().getUid();
            DatabaseReference proRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(userId).child("employerRequest");
            proRef.removeValue();

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("employerRequest");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
        employerId = "";
        jobDistance = 0;

        if (jobLocationMarker != null)
        {
            jobLocationMarker.remove();
        }
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

    //saving job with unique ID
   private void recordJob()
   {
       String userId = FirebaseAuth.getInstance().getUid();
       DatabaseReference proRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(userId).child("history");
       DatabaseReference employerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Employer").child(employerId).child("history");
       DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
       String requestId = historyRef.push().getKey();
       proRef.child(requestId).setValue(true);
       employerRef.child(requestId).setValue(true);

       HashMap map = new HashMap();
       map.put("professional", userId);
       map.put("employer", employerId);
       map.put("rating", 0);
       map.put("timestamp", getCurrentTimestamp());
       map.put("location", location);
       //incase of idssues weka locationLatLng.latitude);
       map.put("location/from/lat", jobLatLng.latitude);
       map.put("location/from/lng", jobLatLng.longitude);
       map.put("location/to/lat", locationLatLng.latitude);
       map.put("location/to/lng", locationLatLng.longitude);
       map.put("distance", jobDistance);
       historyRef.child(requestId).updateChildren(map);

   }

    private long getCurrentTimestamp()
    {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
            }
            else
                {
                    checkLocationPermission();
                }
        }
    }

    LocationCallback mLocationCallback = new LocationCallback()
    {
        @Override
        public void onLocationResult(LocationResult locationResult)
        {
            for (Location location : locationResult.getLocations())
            {
                if (getApplicationContext()!= null)
                {
                    //total distance
                    if (!employerId.equals(""))
                    {
                        jobDistance += mLastLocation.distanceTo(location)/1000;
                    }

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
        }
    };

    private void checkLocationPermission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                new AlertDialog.Builder(this)
                    .setTitle("give permission")
                    .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                ActivityCompat.requestPermissions(ProMapActivity.this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION},1);

                            }
                        })
                .create()
                .show();
            }
            else
                {
                    ActivityCompat.requestPermissions(ProMapActivity.this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION},1);
                }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case 1:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();

                }
                break;
            }

        }
    }

    private void connectPro()
    {
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    private void disconnectPro()
    {
        if (mFusedLocationClient != null)
        {}
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("prosAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
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
