package com.example.mapdirection;

import com.example.mapdirection.models.DistanceResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

// http://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=Washington,DC&destinations=New+York+City,NY

public interface DistanceApiClient {
    @GET("maps/api/distancematrix/json")
    Call<DistanceResponse> getDistanceInfo(
            @QueryMap Map<String, String> parameters,

            @Query("mode") String mode
    );
}
