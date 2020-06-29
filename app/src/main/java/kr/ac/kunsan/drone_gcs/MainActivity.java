package kr.ac.kunsan.drone_gcs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    String pnu = "";
    String ag_geom = "";
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
        naverMap.setMapType(NaverMap.MapType.Basic);
        naverMap.setLocationSource(locationSource);
        naverMap.setOnMapClickListener((point, coord) ->{
            Toast.makeText(this, coord.latitude + ", " + coord.longitude,
                    Toast.LENGTH_SHORT).show();

            String latitude = Double.toString(coord.latitude);
            String longitude = Double.toString(coord.longitude);

            /* DB 대조 */
            ContentValues values = new ContentValues();
            values.put("coords", longitude+","+latitude);
            NetworkTask networkTask = new NetworkTask("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&"+values.toString() +"&sourcecrs=epsg:4326&output=json&orders=addr");
            networkTask.execute();
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
//                    System.out.println(response.toString());
                Log.d("HTTP body: ", response.toString());

                JsonParser jsonParser = new JsonParser();
                JSONObject jsonObj = (JSONObject) jsonParser.parse(response.toString());
                JSONArray array = (JSONArray) jsonObj.get("result");

                JsonArray jsonArray = (JsonArray) jsonParser.parse(response.toString());
                String add = new String();
                for(int i=0; i < array.length(); i++) {
                    JSONObject personObject = (JSONObject) array.get(i);
                    System.out.println(personObject.get("name"));
                }


                return add;


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
            return result; // 결과가 여기에 담깁니다. 아래 onPostExecute()의 파라미터로 전달됩니다.
        }

        @Override
        protected void onPostExecute(String result) {
            // 통신이 완료되면 호출됩니다.
            // 결과에 따른 UI 수정 등은 여기서 합니다.
            System.out.println(result);
            Log.d("HTTP Result : ", result);
        }
    }

}