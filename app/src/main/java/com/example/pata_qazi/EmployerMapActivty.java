package com.example.pata_qazi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EmployerMapActivty extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
{

    private GoogleMap mMap;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mlogout, mRequest, mSettings, mHistory;

    private LatLng employerLocation;

    private Boolean requestBol = false;

    private Marker jobLocationMarker;

    private String location, requestService;

    private LatLng locationLatLng;

    private LinearLayout mProInfo;

    //private ImageView mProProfileImage;

    private TextView mProName, mProPhone, mProCategory;

    private RadioGroup mRadioGroup;

    private RatingBar mRatingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_map_activty);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(EmployerMapActivty.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        else
        {
            mapFragment.getMapAsync(this);
        }

        locationLatLng = new LatLng(0.0, 0.0);

        mProInfo = (LinearLayout) findViewById(R.id.proInfo);
        mProName = (TextView) findViewById(R.id.proName);
        mProPhone = (TextView) findViewById(R.id.proPhone);
        mProCategory = (TextView) findViewById(R.id.proCategory);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.Maintenance);

        mlogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);
        mHistory = (Button) findViewById(R.id.history);

        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);

        mlogout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(EmployerMapActivty.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        mRequest.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                if (requestBol)

                {
                    endJob();
                }
                else
                {
                    int selectId = mRadioGroup.getCheckedRadioButtonId();

                    final RadioButton radioButton = (RadioButton) findViewById(selectId);

                    if (radioButton.getText() == null)
                    {
                        return;
                    }

                    requestService = radioButton.getText().toString();

                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("employerRequest");

                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    employerLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    jobLocationMarker = mMap.addMarker(new MarkerOptions().position(employerLocation).title("We are here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.jobicon)));


                    mRequest.setText("Getting you a professional...");

                    getClosestPro();

                }
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(EmployerMapActivty.this, EmployerSettingsActivity.class);
                startActivity(intent);
                return;

            }
        });

        mHistory.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(EmployerMapActivty.this, HistoryActivity.class);
                intent.putExtra("employerOrPro", "Employer");
                startActivity(intent);
                return;
            }
        });

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

// Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

// Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place)
            {
                //hapa toa tostring
                // TODO: Get info about the selected place.
                location = place.getName().toString();

                locationLatLng = place.getLatLng();

            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.

            }
        });


        //location user chooses

    }

    private int radius = 10 ;

    private Boolean proFound = false;

    private String proFoundID;

    GeoQuery geoQuery;

    private void getClosestPro()
    {
        DatabaseReference proLocation = FirebaseDatabase.getInstance().getReference().child("prosAvailable");

        GeoFire geoFire = new GeoFire(proLocation);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(employerLocation.latitude, employerLocation .longitude ), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener()
        {
            @Override
            public void onKeyEntered(final String key, final GeoLocation location)
            {

                if (!proFound  && requestBol)
                {
                    DatabaseReference mEmployerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(key);
                    mEmployerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                        {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()> 0)
                            {
                                Map<String, Object> ProMap = (Map<String, Object>) dataSnapshot.getValue();
                                if (proFound)
                                {
                                    return;
                                }

                                if(ProMap.get("service").equals(requestService))
                                {
                                    proFound=true;
                                    proFoundID = dataSnapshot.getKey();

                                    //notify pro for a job
                                    DatabaseReference proRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(proFoundID).child("employerRequest");

                                    String employerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                    HashMap map = new HashMap();

                                    map.put("employerJobId", employerId);
                                    map.put("location", location);
                                    map.put("locationLat", locationLatLng.latitude);
                                    map.put("locationLng", locationLatLng.longitude);
                                    proRef.updateChildren(map);

                                    //pro location for employer

                                    getProLocation();
                                    getProInfo();
                                    getHasJobEnded();
                                    mRequest.setText("Finding Pro's location...");

                                }

                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError)
                        {

                        }
                    });


                }

            }

            @Override
            public void onKeyExited(String key)
            {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location)
            {

            }

            @Override
            public void onGeoQueryReady()
            {
                if (!proFound )
                {
                    radius++ ;
                    getClosestPro();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error)
            {

            }
        });

    }
    //getting pro location
    private Marker mProMarker;

    private DatabaseReference proLocationRef;

    private ValueEventListener proLocationRefListener;

    private void getProLocation()
    {
        proLocationRef = FirebaseDatabase.getInstance().getReference().child("prosWorking").child("proFoundID").child("l");

        proLocationRefListener =  proLocationRef.addValueEventListener(new ValueEventListener()

                //update location of pro
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol)
                {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Found A Professional");
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng proLatLng = new LatLng(locationLat,locationLng);
                    if(mProMarker != null){
                        mProMarker.remove();
                    }
                    //distance btwn two locations in meters
                    Location loc1 = new Location("");
                    loc1.setLatitude(employerLocation.latitude);
                    loc1.setLatitude(employerLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(proLatLng.latitude);
                    loc2.setLatitude(proLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance<100)
                    {
                        mRequest.setText("Professional is here");
                    }
                    else
                    {
                        mRequest.setText("Found A Professional: " + distance);
                    }


                    mRequest.setText("Found a Professional" + distance);

                    mProMarker = mMap.addMarker(new MarkerOptions().position(proLatLng).title("your worker").icon(BitmapDescriptorFactory.fromResource(R.mipmap.proicon)));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }

        });

    }

    private void getProInfo()
    {
        mProInfo.setVisibility(View.VISIBLE);

        DatabaseReference mEmployerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(proFoundID).child("");
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
                        mProName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") !=null)
                    {
                        mProPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("category") !=null)
                    {
                        mProCategory.setText(map.get("category").toString());
                    }
                    //calculating pro rating
                    int ratingSum = 0;
                    float ratingsTotal = 0;
                    float ratingsAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren())
                    {
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingsTotal++;
                    }
                    if (ratingsTotal != 0)
                    {
                        ratingsAvg = ratingSum/ratingsTotal;
                        mRatingBar.setRating(ratingsAvg);
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference jobHasEndedRef;
    private ValueEventListener jobHasEndedRefListener;
    private void getHasJobEnded()
    {

        jobHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(proFoundID).child("employerRequest").child("employerJobId");

        jobHasEndedRefListener = jobHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {

                }
                else
                {
                    //pro cancelling job
                    endJob();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }

    private void endJob()
    {
        requestBol = false;
        geoQuery.removeAllListeners();
        proLocationRef.removeEventListener(proLocationRefListener);
        jobHasEndedRef.removeEventListener(jobHasEndedRefListener);

        if (proFoundID != null)
        {
            DatabaseReference proRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Professionals").child(proFoundID).child("employerRequest");

            proRef.removeValue();
            proFoundID = null;
        }

        proFound = false;
        radius = 10;

        String userId = FirebaseAuth.getInstance().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("employerRequest");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

        if (jobLocationMarker != null)
        {
            jobLocationMarker.remove();
        }

        mRequest.setText("Call Professional");

        mProInfo.setVisibility(View.GONE);
        mProName.setText("");
        mProPhone.setText("");
        mProCategory.setText("Location: -- " );

    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(EmployerMapActivty.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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
        mLastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

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
            ActivityCompat.requestPermissions(EmployerMapActivty.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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


    //displaying all available pros
    List<Marker> markerList = new ArrayList<Marker>();
    private void getProsAround()
    {
        DatabaseReference prosLocation = FirebaseDatabase.getInstance().getReference().child(("prosAvailable"));

        GeoFire geoFire = new GeoFire(prosLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()),1000);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener()
        {
            @Override
            public void onKeyEntered(String key, GeoLocation location)
            {
                for (Marker markerIt : markerList)
                {
                    if (markerIt.getTag().equals(key))
                        return;
                }
                LatLng proLocation = new LatLng(location.latitude, location.longitude);

                Marker mProMarker = mMap.addMarker(new MarkerOptions().position(proLocation).title(key));
                mProMarker.setTag(key);

                markerList.add(mProMarker);
            }

            @Override
            public void onKeyExited(String key)
            {
                for (Marker markerIt : markerList)
                {
                    if (markerIt.getTag().equals(key))
                    {
                        markerIt.remove();
                        markerList.remove(markerIt);
                        return;
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location)
            {

                for (Marker markerIt : markerList)
                {
                    if (markerIt.getTag().equals(key))
                    {
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
}

