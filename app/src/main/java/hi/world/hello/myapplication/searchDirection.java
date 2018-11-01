package hi.world.hello.myapplication;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class searchDirection extends Service {

    private TMapData tMapData = new TMapData();
    private TMapPoint myLocation;   // 현 위치 좌표
    private TMapPoint end;           // 목적지 좌표

    private static final String TAG = "GPS_TEST";
    private LocationManager mLocationManager = null;        // onCreate에서 초기화
    private static final int LOCATION_INTERVAL = 1000;    // 최소 100초 뒤에 위치 재탐색 할 수 있다
    private static final float LOCATION_DISTANCE = 1f;    // 최소 1m이상 움직여야 재탐색 한다

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    public searchDirection() {

    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.e(TAG, "onCreate");

        initializeLocationManager();

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_INTERVAL,
                    LOCATION_DISTANCE,
                    mLocationListeners[1]
            );
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL,
                    LOCATION_DISTANCE,
                    mLocationListeners[0]
            );
        }catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);

        end = new TMapPoint(intent.getExtras().getDouble("desLa"), intent.getExtras().getDouble("desLo"));

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @brief stop버튼 누르면 gps 종료
     */
    @Override
    public void onDestroy(){
        Log.e(TAG, "onDestroy");
        super.onDestroy();

        if (mLocationManager != null){
            for (int i=0; i<mLocationListeners.length; i++){
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listeners, ignore", ex);
                }
            }
        }
    }

    /**
     * @brief LocationManager 초기화
     */
    private void initializeLocationManager(){
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null){
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private class LocationListener implements android.location.LocationListener{
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener" + provider);
            mLastLocation = new Location(provider);
        }

        /**
         * @brief 현 위치가 바뀔 때마다 findPathData 써서 경로 재탐색
         * @param location update된 위치 좌표를 담고 있다
         */
        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG,"lnLocationChanged: " + location);
            mLastLocation.set(location);
            myLocation = new TMapPoint(location.getLatitude(), location.getLongitude());

            Log.i("시작", String.valueOf(myLocation.getLatitude()) + ", " + String.valueOf(myLocation.getLongitude()));
            Log.i("목적", String.valueOf(end.getLatitude()) + ", " + String.valueOf(end.getLongitude()));

            tMapData.findPathDataAllType(TMapData.TMapPathType.PEDESTRIAN_PATH, myLocation, end, new TMapData.FindPathDataAllListenerCallback() {
                @Override
                public void onFindPathDataAll(Document document) {
                    Element root = document.getDocumentElement();
                    NodeList nodeListPlacemark = root.getElementsByTagName("Placemark");

                    for (int i=0; i<nodeListPlacemark.getLength(); i++){
                        NodeList nodeListPlacemarkItem = nodeListPlacemark.item(i).getChildNodes();

                        // Log로 placemark nodelist 내용 확인하는 부분
                        for (int j=0; j<nodeListPlacemarkItem.getLength(); j++) {
                            if (nodeListPlacemarkItem.item(j).getNodeName().equals("description")) {
                                Log.d("description", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:index")) {
                                Log.d("tmap:index", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:pointIndex")) {
                                Log.d("tmap:pointIndex", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("name")) {
                                Log.d("name", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("styleUrl")) {
                                Log.d("styleUrl", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:direction")) {
                                Log.d("tmap:direction", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:intersectionName")) {
                                Log.d("tmap:intersectionName", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:nearPoiName")) {
                                Log.d("tmap:nearPoiName", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:nodeType")) {
                                Log.d("tmap:nodeType", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:turnType")) {
                                Log.d("tmap:turnType", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:pointType")) {
                                Log.d("tmap:pointType", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:lineIndex")) {
                                Log.d("tmap:lineIndex", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:distance")) {
                                Log.d("tmap:distance", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:time")) {
                                Log.d("tmap:time", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:roadType")) {
                                Log.d("tmap:roadType", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:categoryRoadType")) {
                                Log.d("tmap:categoryRoadType", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            } else if (nodeListPlacemarkItem.item(j).getNodeName().equals("Point")) {
                                Log.d("Point", nodeListPlacemarkItem.item(j).getTextContent().trim());
                            }

                        }

                    }
                    Log.i("END", "--------------------------------------");
                }
            });

            /**
             * 블루투스로 기기에 데이터 보내는 부분을 추가해야한다
             */
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }

    }
}
