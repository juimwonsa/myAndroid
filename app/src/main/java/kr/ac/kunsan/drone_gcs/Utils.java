package kr.ac.kunsan.drone_gcs;

import com.naver.maps.geometry.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Utils {


    public static ArrayList<LatLng> sortLatLngArray(ArrayList<LatLng> mPolyCoord){
        float averageX = 0;
        float averageY = 0;
        for(LatLng coord : mPolyCoord){
            averageX += coord.latitude;
            averageY += coord.longitude;
        }

        final float finalAverageX = averageX / mPolyCoord.size();
        final float finalAverageY = averageY / mPolyCoord.size();

        Comparator<LatLng> comparator = (lhs, rhs) ->{
            double lhsAngle = Math.atan2(lhs.longitude - finalAverageY, lhs.latitude - finalAverageX);
            double rhsAngle = Math.atan2(rhs.longitude - finalAverageY, rhs.latitude - finalAverageX);

            if(lhsAngle < rhsAngle) return -1;
            if(lhsAngle > rhsAngle) return 1;

            return 0;
        };

        Collections.sort(mPolyCoord, comparator);
        return mPolyCoord;
    }


}
