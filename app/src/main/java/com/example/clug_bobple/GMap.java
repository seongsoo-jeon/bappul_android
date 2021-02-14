package com.example.clug_bobple;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import noman.googleplaces.NRPlaces;
import noman.googleplaces.Place;
import noman.googleplaces.PlaceType;
import noman.googleplaces.PlacesException;
import noman.googleplaces.PlacesListener;

public class GMap extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, PlacesListener {
    List<Marker> previous_marker = new ArrayList<>();
    GoogleMap mMap;
    private FragmentManager fragmentManager;
    private MapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fragmentManager = getFragmentManager();
        mapFragment = (MapFragment)fragmentManager.findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng location = new LatLng(37.50532641259903, 126.95701536932249);
        MarkerOptions marker = new MarkerOptions();
        marker.title("중앙대학교");
        marker.position(location);
        mMap.addMarker(marker);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        showPlaceInformation(location);

        this.mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                getMostRecentReview(marker);
                return false;
            }
        });
    }
    @Override
    public void onPlacesFailure(PlacesException e) {

    }
    @Override
    public void onPlacesStart() {

    }
    @Override
    public void onPlacesSuccess(List<Place> places) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                for (noman.googleplaces.Place place : places) {

                    LatLng latLng
                            = new LatLng(place.getLatitude()
                            , place.getLongitude());

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(place.getName());
                    Marker item = mMap.addMarker(markerOptions);
                    previous_marker.add(item);

                }

                //중복 마커 제거
                HashSet<Marker> hashSet = new HashSet<Marker>();
                hashSet.addAll(previous_marker);
                previous_marker.clear();
                previous_marker.addAll(hashSet);
            }
        });
    }
    @Override
    public void onPlacesFinished() {

    }
    public void showPlaceInformation(LatLng location)
    {
        mMap.clear();//지도 클리어

        if (previous_marker != null)
            previous_marker.clear();//지역정보 마커 클리어

        new NRPlaces.Builder()
                .listener(GMap.this)
                .key("AIzaSyCKx9eXDOXbFukMXevz0wTO3wSxyXGoY8A")
                .latlng(location.latitude, location.longitude)//현재 위치
                .radius(500) //500 미터 내에서 검색
                .type(PlaceType.RESTAURANT) //음식점
                .build()
                .execute();
    }

    private void getMostRecentReview(Marker marker) {
        String url = getString(R.string.url) + "/restaurant/" + marker.getTitle() + "/reviews/review";
        RequestQueue queue = Volley.newRequestQueue(GMap.this);

        JSONObject location = new JSONObject();
        try {
            location.put("lat", Double.toString(marker.getPosition().latitude));
            location.put("long", Double.toString(marker.getPosition().longitude));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, location,
                new Response.Listener<JSONObject>(){
                    @Override
                    public void onResponse(JSONObject response) {
                        TextView name = findViewById(R.id.name);
                        TextView review_date = findViewById(R.id.review_date);
                        TextView review = findViewById(R.id.review);
                        RatingBar star_rate = findViewById(R.id.star_rate);

                        try {
                            name.setText(response.get("name").toString());
                            review_date.setText(response.get("date").toString());
                            review.setText(response.get("content").toString());
                            star_rate.setRating(Integer.parseInt(response.get("rate").toString()));
                        } catch (JSONException exception) {
                            exception.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                TextView name = findViewById(R.id.name);
                TextView review_date = findViewById(R.id.review_date);
                TextView review = findViewById(R.id.review);
                RatingBar star_rate = findViewById(R.id.star_rate);

                name.setText("");
                review_date.setText("");
                star_rate.setRating(0);
                review.setText("아직 후기가 없어요. 첫번째 후기를 달아주세요:)");



            }
        });
        queue.add(jsonObjectRequest);
    }
}