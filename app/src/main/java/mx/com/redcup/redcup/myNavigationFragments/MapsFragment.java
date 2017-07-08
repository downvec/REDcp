package mx.com.redcup.redcup.myNavigationFragments;


import android.Manifest;
import android.app.Fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import mx.com.redcup.redcup.NavActivity;
import mx.com.redcup.redcup.R;
import mx.com.redcup.redcup.myDataModels.MyEvents;

public class MapsFragment extends Fragment implements OnMapReadyCallback, OnLocationUpdatedListener {
    String TAG = "MapsFragment";

    static GoogleMap googleMap_fragment;
    DatabaseReference mDataBase;
    FloatingActionButton fabNewEvent;


    LatLng mDefaultLocation = new LatLng(0, 0);
    Location mCurrentLocation;

    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        //Get UI Elements
        fabNewEvent = (FloatingActionButton) view.findViewById(R.id.fab_create_event);


        return view;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        MapFragment myGoogleMap = (MapFragment) getChildFragmentManager().findFragmentById(R.id.my_google_map);
        myGoogleMap.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap_fragment = googleMap;

        //Adding style options to map. Json options in raw folder
        //googleMap_fragment.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.style_json));


        //TODO: Zoom on the current location

        getCurrentLocation();

        if (mCurrentLocation != null) {
            LatLng currentlatlng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            googleMap_fragment.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlatlng,11));
            googleMap_fragment.setMyLocationEnabled(true);

        } else {
            googleMap_fragment.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation,3));
        }


        //Draw markers from event objects in firebase database
        drawMarkersFromFirebaseDB();


        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                fabNewEvent.setVisibility(View.GONE);
                return false;
            }
        });

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                fabNewEvent.setVisibility(View.VISIBLE);
            }
        });
    }

    void getCurrentLocation(){

        SmartLocation.with(getActivity()).location().start(new OnLocationUpdatedListener() {
            @Override
            public void onLocationUpdated(Location location) {
                Log.e("Hellod","This is from the listeners");
                Log.e("Coordinates", String.valueOf(location.getLatitude()));
                mCurrentLocation = location;

                LatLng coordinates = new LatLng(location.getLatitude(),location.getLongitude());
                googleMap_fragment.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates,11));
            }
        });

    }

    public void drawMarkersFromFirebaseDB() {
        mDataBase = FirebaseDatabase.getInstance().getReference().child("Events_parent");
        mDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot myDataSnapshot : dataSnapshot.getChildren()) {
                    MyEvents newEvent = myDataSnapshot.getValue(MyEvents.class);
                    //TODO This is useless. Make this only draw if userID is within invitee list or if the event is public
                    if (newEvent.eventIsPublic()) {
                        LatLng coords = new LatLng(newEvent.getEventLatitude(), newEvent.getEventLongitude());
                        MarkerOptions options = new MarkerOptions().position(coords).title(newEvent.getEventName());
                        googleMap_fragment.addMarker(options);
                    } //End if statement

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onLocationUpdated(Location location) {
        Log.e("maybe","dide we get an update? ");
        Log.e("Coordinates", String.valueOf(location.getLatitude()));
        mCurrentLocation = location;
    }
}

