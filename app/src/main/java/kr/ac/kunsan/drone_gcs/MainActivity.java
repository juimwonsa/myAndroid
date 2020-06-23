package kr.ac.kunsan.drone_gcs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        FragmentManager fm = getSupportFragmentManager();

        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map_fragment);
        if(mapFragment==null){
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

    }



    @Override
    public void onMapReady(final NaverMap naverMap) {

        naverMap.setMapType(NaverMap.MapType.Basic);

        final Spinner spinner = (Spinner)findViewById(R.id.spinner2);
        final String[] data = getResources().getStringArray(R.array.choiceMapMode);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,data);
        spinner.setAdapter(adapter);

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





    }

}