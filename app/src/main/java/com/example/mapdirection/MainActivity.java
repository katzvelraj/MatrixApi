package com.example.mapdirection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerCities;
    private TextView tvTravelInfo;
    private String[] cities;

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
                Toast.makeText(getApplicationContext(), getAddress(cities[i]) + "", Toast.LENGTH_SHORT).show();
                getDistanceInfo(getCurrentCoordinate(), cities[i]);

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

            return null;
        }

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

//        Criteria criteria = new Criteria();
//        String bestProvider = lm.getBestProvider(criteria, false);
//        Location location = lm.getLastKnownLocation(bestProvider);

        return location;
    }

    private String getCurrentCoordinate() {
        Location location = getCurrentLocation();
        if (location == null) return "";
        return location.getLatitude() + "," + location.getLongitude();
    }


    private void getDistanceInfo(String origin, String destination) {
        // http://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=Washington,DC&destinations=New+York+City,NY
        Map<String, String> mapQuery = new HashMap<>();
        mapQuery.put("units", "imperial");
        mapQuery.put("origins", origin);
        mapQuery.put("destinations", destination + "|" + cities[1] + "|" + cities[2]
                + "|" + cities[3] + "|" + cities[4]);
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
                    findShortestDistance(response.body().getRows().get(0).getElements(), response.body().getDestinationAddresses());

                }
            }

            @Override
            public void onFailure(Call<DistanceResponse> call, Throwable t) {

            }
        });
    }

    private void findShortestDistance(List<Element> elements, List<String> destinationAddresses) {
        long shortest = 999999999;


        HashMap<Long, String> stringHashMap = new HashMap<>();
        List<Long> dis = new ArrayList<>();
        int shortestPos = 99999999;

        for (int i = 0; i < elements.size(); i++) {
            stringHashMap.put(Long.valueOf(elements.get(i).getDistance().getValue()),
                    cities[i]);
            dis.add(Long.valueOf(elements.get(i).getDistance().getValue()));

            if (shortest > elements.get(i).getDistance().getValue()) {
                shortest = elements.get(i).getDistance().getValue();
            }
        }
        Collections.sort(dis);
        ArrayList<LatLng> latLngs = new ArrayList<>();
        for (int i = 0; i < dis.size(); i++) {

            String latlng = stringHashMap.get(dis.get(i));
            String[] latlong = latlng.split(",");
            double lat = Double.parseDouble(latlong[0]);
            double lng = Double.parseDouble(latlong[1]);
            LatLng lng1 = new LatLng(lat, lng);
            latLngs.add(lng1);
        }


        Type listType = new TypeToken<List<LatLng>>() {
        }.getType();


        Gson gson = new Gson();
        String json = gson.toJson(latLngs, listType);


        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("LAT", json);
        startActivity(intent);


        List<String> target2 = gson.fromJson(json, listType);
        Toast.makeText(this, "shortest" + shortest + stringHashMap.get(shortest), Toast.LENGTH_SHORT).show();
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

}
