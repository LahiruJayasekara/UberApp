package com.example.mlpj.uberapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.print.PrinterId;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DialogTitle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;


import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.mlpj.uberapp.Models.PlaceInfo;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    @Override
    public void onRoutingFailure(RouteException e) {
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
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        mMap = googleMap;

        if (mLocationPermissionGranted) {
            getDeviceLocation();
            continouseLocationUpdate();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            init();
        }
    }

    private static final String TAG = "MapsActivity";
    private final static String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private final static String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private final static int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private final static float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));


    private boolean mLocationPermissionGranted;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    //private GoogleApiClient mGoogleApiClient;
    private GeoDataClient mGeoDataClient;
    private PlaceInfo placeInfo;
    private LocationManager locationManager;
    private Location mCurrentLocation;
    private LatLng mDestinationLocation;
    private Marker mMarker;
    private  boolean mIsRouteSet;
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.routeColor ,R.color.primary_dark_material_light};


    //widgets
    private AutoCompleteTextView mEtSearch;
    private ImageView mGps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        polylines = new ArrayList<>();
        mEtSearch = findViewById(R.id.input_text);
        mGps = findViewById(R.id.ic_gps);
        getLocationServices();
    }

    private void init() {

        mGeoDataClient = Places.getGeoDataClient(this, null);

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(MapsActivity.this, mGeoDataClient, LAT_LNG_BOUNDS, null);
        mEtSearch.setAdapter(mPlaceAutocompleteAdapter);
        mEtSearch.setOnItemClickListener(mAutoCompleteClickListener);
        mEtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                getLocate();
                return false;
            }
        });

        //on click my location view button
        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceLocation();
            }
        });
        hideSoftKeyBoard();
    }

    //find a place
    private void getLocate() {
        String searchString = mEtSearch.getText().toString();

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Toast.makeText(this, "exception location by name " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        if (list.size() > 0) {
            Address address = list.get(0);
            Log.d(TAG, "getLocate: " + address.toString());
            Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM);
            addMarkerToLocation(new LatLng(address.getLatitude(), address.getLongitude()), address.getAddressLine(0));
        }
    }


    public void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
        try {
            if (mLocationPermissionGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            mCurrentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), DEFAULT_ZOOM);
                        } else {
                            Toast.makeText(MapsActivity.this, "Unable to get current location", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: Security exception " + e.getMessage());
            Toast.makeText(this, "getDeviceLocation: Security exception " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    private void continouseLocationUpdate(){
        locationManager = (LocationManager) MapsActivity.this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                2000,
                10, locationListenerGPS);
    }

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mCurrentLocation = location;
            //moveCamera(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM);
            //addMarkerToLocation(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM, "My Location");
            if(mDestinationLocation != null){
                drawRoutes(mDestinationLocation);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void moveCamera(LatLng latLng, float zoom) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        hideSoftKeyBoard();
    }

    private void addMarkerToLocation(LatLng latLng, String title){
        if(mMarker != null){
            mMarker.remove();
        }
        MarkerOptions options = new MarkerOptions().
                position(latLng).
                title(title);
        mMarker = mMap.addMarker(options);
        hideSoftKeyBoard();

    }

    private void initMaps() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }

    private void getLocationServices(){
        String[] permissions ={Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;
                initMaps();
            }else{
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE : {
                if(grantResults.length>0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    initMaps();
                }
            }
        }
    }

    public void drawRoutes(LatLng destination){
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), destination)
                .build();
        routing.execute();
    }

    private void hideSoftKeyBoard(){
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /*
        ------------------------google places api auto completion suggestions-----------------------
     */

    private AdapterView.OnItemClickListener mAutoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            hideSoftKeyBoard();

            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();

            Task<PlaceBufferResponse> placeResult = mGeoDataClient.getPlaceById(placeId);
            placeResult.addOnCompleteListener(mUpdatePlaceDetailsCallback);
        }
    };

    private OnCompleteListener<PlaceBufferResponse> mUpdatePlaceDetailsCallback = new OnCompleteListener<PlaceBufferResponse>() {
        @Override
        public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
            if(!task.isSuccessful()){
                Toast.makeText(MapsActivity.this, "Query unsuccessful " + task.toString(), Toast.LENGTH_SHORT).show();
                return;
            }
            PlaceBufferResponse places = task.getResult();
            final Place place = places.get(0);
            Toast.makeText(MapsActivity.this, place.getName().toString() +
                    place.getLatLng().toString(), Toast.LENGTH_SHORT).show();

            try{
                placeInfo = new PlaceInfo();
                placeInfo.setAddress(place.getAddress().toString());
                placeInfo.setAttributions(place.getAttributions().toString());
                placeInfo.setId(place.getId().toString());
                placeInfo.setLatLng(place.getLatLng());
                placeInfo.setName(place.getName().toString());
                placeInfo.setPhoneNumber(place.getPhoneNumber().toString());
                placeInfo.setRating(place.getRating());
                placeInfo.setWebSiteUrl(place.getWebsiteUri());
            }catch (NullPointerException e){
                Toast.makeText(MapsActivity.this, "NullPointerException : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude),DEFAULT_ZOOM);
            addMarkerToLocation(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude),place.getName().toString());
            drawRoutes(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude));
            //set destination route global variable --> to access it from continuous current location update
            mDestinationLocation = new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude);
            places.release();
        }
    };
}
