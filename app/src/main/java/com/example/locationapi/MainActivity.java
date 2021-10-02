package com.example.locationapi;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.*;
import com.google.android.gms.location.*;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int CHECK_SETTINGS_CODE = 111;
    private static final int REQUEST_LOCATION_PERMISSION = 222;

    private Button startLocationUpdatesButton, stopLocationUpdatesButton;
    private TextView locationTextView;
    private TextView locationUpdateTimeTextView;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean isLocationUpdatesActive;
    private String locationUpdatesTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        startLocationUpdatesButton = findViewById(R.id.startLocationUpdatesButton);
        stopLocationUpdatesButton = findViewById(R.id.stopLocationUpdatesButton);
        locationTextView = findViewById(R.id.locationTextView);
        locationUpdateTimeTextView = findViewById(R.id.locationUpdateTimeTextView);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        startLocationUpdatesButton.setOnClickListener(v -> startLocationUpdates());

        stopLocationUpdatesButton.setOnClickListener(view -> stopLocationUpdates());

        buildLocationRequest();
        buildLocationCallBack();
        buildLocationSettingsRequest();

    }

    private void stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            return;
        }

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, task -> {
                    isLocationUpdatesActive = false;
                    startLocationUpdatesButton.setEnabled(true);
                    stopLocationUpdatesButton.setEnabled(false);
                });
    }

    private void startLocationUpdates() {

        isLocationUpdatesActive = true;
        startLocationUpdatesButton.setEnabled(false);
        stopLocationUpdatesButton.setEnabled(true);

        settingsClient.checkLocationSettings(locationSettingsRequest).addOnSuccessListener(this, locationSettingsResponse -> {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
            );
            updateLocationUi();
        })
                .addOnFailureListener(this, e -> {

                    int statusCode = ((ApiException) e).getStatusCode();

                    switch (statusCode) {

                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(MainActivity.this, CHECK_SETTINGS_CODE);
                            } catch (IntentSender.SendIntentException sendIntentException) {
                                sendIntentException.printStackTrace();
                            }
                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            String message = "Adjust location settings in your device";
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();

                            isLocationUpdatesActive = false;
                            startLocationUpdatesButton.setEnabled(true);
                            stopLocationUpdatesButton.setEnabled(false);

                    }

                    updateLocationUi();

                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHECK_SETTINGS_CODE) {
            switch (requestCode) {
                case Activity.RESULT_OK:
                    Log.d("MainActivity", "User has agreed to change location settings");
                    startLocationUpdates();
                    break;
                case Activity.RESULT_CANCELED:
                    Log.d("MainActivity", "User has not agreed to change location settings");
                    isLocationUpdatesActive = false;
                    startLocationUpdatesButton.setEnabled(true);
                    stopLocationUpdatesButton.setEnabled(false);
                    updateLocationUi();
                    break;
            }
        }
    }

    private void buildLocationSettingsRequest() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();

                updateLocationUi();

            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);

            }
        };
    }

    private void updateLocationUi() {
        if (currentLocation != null) {
            locationTextView.setText("" + currentLocation.getLatitude() + "/" + currentLocation.getLongitude());
            locationUpdateTimeTextView.setText(DateFormat.getDateInstance().format(new Date()));
        }
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdatesActive && chekLocationPermission()) {
            startLocationUpdates();
        } else if (!chekLocationPermission()) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        if (shouldProvideRationale) {
            showSnackBar(
                    "Location permission is needed for app functionality",
                    "OK",
                    view -> ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION
                            },
                            REQUEST_LOCATION_PERMISSION
                    )
            );
        } else {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION
            );
        }


    }

    private void showSnackBar(
            final String mainText,
            final String action,
            View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                mainText,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(
                        action,
                        listener
                )
                .show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length <= 0) {
                Log.d("onRequestPermissionsResult", "Request was cancelled");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdatesActive) {
                    startLocationUpdates();
                }
            } else {
                showSnackBar(
                        "Turn on location on settings",
                        "Settings",
                        view -> {
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            );
                            Uri uri = Uri.fromParts(
                                    "package",
                                    Build.ID,
                                    null
                            );
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                );
            }
        }
    }

    private boolean chekLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}