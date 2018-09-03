package com.example.mapdirection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mapdirection.models.DistanceResponse;
import com.example.mapdirection.models.Element;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private Spinner spinnerCities;
    private TextView tvTravelInfo;
    private String[] cities;
    ArrayList<String> stringArrayList = new ArrayList<>();
    String origin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinnerCities = (Spinner) findViewById(R.id.s_cities);
        tvTravelInfo = (TextView) findViewById(R.id.tv_travel_info);

        cities = getResources().getStringArray(R.array.cities);


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.cities, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCities.setAdapter(adapter);


        spinnerCities.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                origin = cities[i];
                getDistanceInfo(origin);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



        Log.d("MainActivity", "current coordinate: " + getCurrentCoordinate());
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private Location getCurrentLocation() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 100);

            return null;
        }
        Location location = null;
        try {

            LocationManager locationManager = (LocationManager) getApplicationContext()
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            boolean isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            boolean isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                // this.canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            58888888,
                            29.0f, (LocationListener) MainActivity.this);
                    Log.d("Network", "Network Enabled");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                1222,
                                33.00f, (LocationListener) MainActivity.this);
                        Log.d("GPS", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    private String getCurrentCoordinate() {
        Location location = getCurrentLocation();
        if (location == null) return "";
        return location.getLatitude() + "," + location.getLongitude();
    }


    public LatLng getLocationFromAddress(String strAddress) {

        Geocoder coder = new Geocoder(this);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng((location.getLatitude()),
                    (location.getLongitude()));

            return p1;
        } catch (Exception e) {

        }
        return p1;
    }

    private void getDistanceInfo(String origin) {
        stringArrayList.clear();
        Map<String, String> mapQuery = new HashMap<>();
        mapQuery.put("units", "imperial");
        mapQuery.put("origins", origin);
        StringBuilder des = new StringBuilder();

        for (int i = 0; i < cities.length; i++) {


            if (!cities[i].equalsIgnoreCase(origin)) {

                if (i == cities.length - 1) {
                    des.append(cities[i]);
                } else {
                    des.append(cities[i] + "|");
                }
                stringArrayList.add(cities[i]);
            }
        }
        mapQuery.put("destinations", des.toString());
//        mapQuery.put("destinations[1]", cities[1]);
//        mapQuery.put("destinations[2]", cities[2]);
        DistanceApiClient client = RestUtil.getInstance().getRetrofit().create(DistanceApiClient.class);

        Call<DistanceResponse> call = client.getDistanceInfo(mapQuery, "driving");
        call.enqueue(new Callback<DistanceResponse>() {
            @Override
            public void onResponse(Call<DistanceResponse> call, Response<DistanceResponse> response) {
                if (response.body() != null &&
                        response.body().getRows() != null &&
                        response.body().getRows().size() > 0 &&
                        response.body().getRows().get(0) != null &&
                        response.body().getRows().get(0).getElements() != null &&
                        response.body().getRows().get(0).getElements().size() > 0 &&
                        response.body().getRows().get(0).getElements().get(0) != null &&
                        response.body().getRows().get(0).getElements().get(0).getDistance() != null &&
                        response.body().getRows().get(0).getElements().get(0).getDuration() != null) {


                    Element element = response.body().getRows().get(0).getElements().get(0);
                    showTravelDistance(element.getDistance().getText() + "\n" + element.getDuration().getText());
                    findShortestDistance(response.body().getRows().get(0).getElements(), response.body().getDestinationAddresses(), response);

                }
            }

            @Override
            public void onFailure(Call<DistanceResponse> call, Throwable t) {

            }
        });
    }

    private void findShortestDistance(List<Element> elements, List<String> destinationAddresses, Response<DistanceResponse> response) {
        long shortest = 999999999;

        //LatLng start = getLocationFromAddress(response.body().getDestinationAddresses().get(0));
        HashMap<Long, Integer> stringHashMap = new HashMap<>();
        HashMap<Long, String> placeMap = new HashMap<>();
        List<Long> dis = new ArrayList<>();

        for (int i = 0; i < elements.size(); i++) {


            if (elements.get(i).getDistance() != null) {
                stringHashMap.put(Long.valueOf(elements.get(i).getDistance().getValue()), i);
                placeMap.put(Long.valueOf(elements.get(i).getDistance().getValue()), stringArrayList.get(i));
                dis.add(Long.valueOf(elements.get(i).getDistance().getValue()));

                if (shortest > elements.get(i).getDistance().getValue()) {
                    shortest = elements.get(i).getDistance().getValue();
                }
            }
        }
        Collections.sort(dis);

        ArrayList<LatLng> latLngs = new ArrayList<>();

        for (int i = 0; i < dis.size(); i++) {

            String latlng = placeMap.get(dis.get(i));
            String[] latlong = latlng.split(",");
            double lat = Double.parseDouble(latlong[0]);
            double lng = Double.parseDouble(latlong[1]);
            LatLng lng1 = new LatLng(lat, lng);
            latLngs.add(lng1);
        }
        String[] latlong = origin.split(",");
        double lat = Double.parseDouble(latlong[0]);
        double lng = Double.parseDouble(latlong[1]);
        LatLng lng1 = new LatLng(lat, lng);
        latLngs.add(0,lng1);



        Type listType = new TypeToken<List<LatLng>>() {
        }.getType();
        Gson gson = new Gson();
        String json = gson.toJson(latLngs, listType);


        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("LAT", json);
        intent.putExtra("ORIGN",origin);
        startActivity(intent);


        // Toast.makeText(this, "shortest" + shortest + stringHashMap.get(shortest), Toast.LENGTH_SHORT).show();
    }

    private void showTravelDistance(String travelInfo) {
        tvTravelInfo.setText(travelInfo);
    }

    private String getAddress(String cities) {


        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        String city = "";
        try {
            String[] latlong = cities.split(",");
            double lat = Double.parseDouble(latlong[0]);
            double lng = Double.parseDouble(latlong[1]);
            addresses = geocoder.getFromLocation(lat, lng, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName(); // On

        } catch (IOException e) {
            e.printStackTrace();
        }

        return city;
    }

    @Override
    public void onLocationChanged(Location location) {

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
}
