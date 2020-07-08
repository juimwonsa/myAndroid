package kr.ac.kunsan.drone_gcs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    NaverMap naverMap;
    String pnu = "";
    String ag_geom = "";
    ArrayList<LatLng> mPolyCoord = new ArrayList<LatLng>();
    ArrayList<Marker> mMarkers = new ArrayList<Marker>();
    LatLng mMapClickCoord;
    String mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_main);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map_fragment);

        if(mapFragment==null){
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull final NaverMap naverMap) {
        Context context ;
        PolygonOverlay polygon = new PolygonOverlay();
        naverMap.setMapType(NaverMap.MapType.Basic);
        naverMap.setLocationSource(locationSource);
        naverMap.setOnMapClickListener((point, coord) ->{
            mMapClickCoord = new LatLng(coord.latitude,coord.longitude);
            mPolyCoord.add(mMapClickCoord);

            if(mPolyCoord.size()>2) {
                mPolyCoord = Utils.sortLatLngArray(mPolyCoord);
                polygon.setCoords(mPolyCoord);
                polygon.setMap(naverMap);
                polygon.setColor(Color.RED);
            }

            String latitude = Double.toString(coord.latitude);
            String longitude = Double.toString(coord.longitude);

            ContentValues values = new ContentValues();
            values.put("coords", longitude+","+latitude);
            NetworkTask networkTask = new NetworkTask("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&"+values.toString() +"&sourcecrs=epsg:4326&output=json&orders=addr");
            networkTask.execute();

            InfoWindow infoWindow = new InfoWindow();
            Marker marker = new Marker();
            mMarkers.add(marker);
            marker.getPosition();
            infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(MainActivity.this) {
                @NonNull
                @Override
                public CharSequence getText(@NonNull InfoWindow infoWindow) {
                    return mResult;
                }
            });
            marker.setPosition(mMapClickCoord);
            marker.setMap(naverMap);
            infoWindow.open(marker);

            marker.setOnClickListener(overlay -> {
                marker.setMap(null);
                mMarkers.remove(marker);

                for(LatLng polyCoord : mPolyCoord){
                    if(marker.getPosition().equals(polyCoord)){
                        mPolyCoord.remove(polyCoord);
                        break;
                    }
                }

                for(Marker markers : mMarkers){
                    if(markers.getPosition().equals(marker.getPosition())){
                        mMarkers.remove(markers);
                        break;
                    }
                }
                if(mPolyCoord.size() > 2) {
                    polygon.setCoords(mPolyCoord);
                    polygon.setMap(naverMap);
                    polygon.setColor(Color.RED);
                }
                else polygon.setMap(null);
                return true;
            });
        });

        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        final Button btnOnOff = findViewById(R.id.layer_groupe_cadastral);
        final Spinner spinner = findViewById(R.id.spinner2);
        final String[] data = getResources().getStringArray(R.array.choiceMapMode);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,data);
        spinner.setAdapter(adapter);

        //타입 선택 스피너
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String text = spinner.getSelectedItem().toString();

                switch(text){
                    case "Basic":
                        naverMap.setMapType(NaverMap.MapType.Basic);
                        break;
                    case "Navi":
                        naverMap.setMapType(NaverMap.MapType.Navi);
                        break;
                    case "Satellite":
                        naverMap.setMapType(NaverMap.MapType.Satellite);
                        break;
                    case "Hybrid":
                        naverMap.setMapType(NaverMap.MapType.Hybrid);
                        break;
                    case "Terrain":
                        naverMap.setMapType(NaverMap.MapType.Terrain);
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //지적 편집도 활성화버튼
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!naverMap.isLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL)) {
                    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                    btnOnOff.setText("지적 편집도 비활성화");
                }
                else{
                    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                    btnOnOff.setText("지적 편집도 활성화");
                }
            }
        });

        Button changeActivityBtn = findViewById(R.id.changeActivityBtn);

        changeActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        getApplicationContext(),
                        MapViewActivity.class);
                startActivity(intent);
            }
        });
    }

    public static class RequestHttpURLConnection {

        public String request(String _url) {


            try {
                URL url = new URL(_url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                Log.d("NaverReverseGeocoding", "apiURL : " + _url);
                // [2-1]. conn 설정.
                conn.setRequestMethod("GET"); // URL 요청에 대한 메소드 설정 : GET/POST.
                conn.setRequestProperty("Content-type", "application/json");
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", "f8pw9359ww");
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", "gcQAP4XlKaVxoEPw4cNTlX8uNlDA6HIwVDBw4VGg");
                int responseCode = conn.getResponseCode();
                Log.d("HTTP 응답 코드: ", ""+responseCode);
                BufferedReader br;
                if (responseCode == 200) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                Log.d("HTTP body: ", response.toString());
                String address = "";
                try {
                    JsonParser jsonParser = new JsonParser();
                    Object obj = jsonParser.parse(response.toString());
                    JsonObject jsonObj = (JsonObject) obj;
                    JsonElement jsonElement = jsonObj.get("results");
                    address = jsonElement.toString().substring(1);
                    address = address.substring(0, address.length()-1);
                    obj = jsonParser.parse(address);
                    jsonObj = (JsonObject) obj;
                    jsonElement = jsonObj.get("region");
                    JsonElement jsonElement1 = jsonElement.getAsJsonObject().get("area1").getAsJsonObject().get("name");
                    address = jsonElement1.toString() + " ";
                    jsonElement1 = jsonElement.getAsJsonObject().get("area2").getAsJsonObject().get("name");
                    address += jsonElement1.toString() + " ";
                    jsonElement1 = jsonElement.getAsJsonObject().get("area3").getAsJsonObject().get("name");
                    address += jsonElement1.toString();
                    address = address.replaceAll("\"", "");
//                    for(int i=0; i < addressArray.size(); i++) {
//                        JsonObject personObject = (JsonObject) addressArray.get(i);
//                        address += personObject.get("name");
//                    }

                    return address;
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }

                Log.d("실주소 ", address);
                return address;


            } catch (MalformedURLException e) { // for URL.
                e.printStackTrace();
            } catch (IOException e) { // for openConnection().
                e.printStackTrace();
            }
            return null;
        }
    }
    public class NetworkTask extends AsyncTask<Void, Void, String> {
        String url;

        NetworkTask(String url){
            this.url = url;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            String result;
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url);
            mResult = result;
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println(result);
            Log.d("HTTP Result : ", result);

        }
    }

}