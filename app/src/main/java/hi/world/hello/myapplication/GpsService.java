package hi.world.hello.myapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GpsService extends Service implements LocationListener {

    private Context mContext = null;

    private static final String TAG = "GPS_TEST";

    private TMapData tMapData = new TMapData();
    private TMapPoint myLocation = null;    // current location
    private TMapPoint end = null;           // target location

    // whenever changing location, trying bluetooth communication
    private MessageHandler mMessageHandler = null;

    // GPS ON/OFF
    boolean isGPSEnabled = false;

    // network ON/OFF
    boolean isNetworkEnabled = false;

    // current state of GPS
    boolean isGetLocation = false;

    Location location;
    double lat; // latitude
    double lon; // longitude

    // minimum distance for updating GPS information : 10m
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    // minimum time for updating GPS information : 1 second
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1;

    protected LocationManager locationManager;

    private RoadGuide roadGuide = null;

    public GpsService() {

    }

    public GpsService(Context context, MessageHandler msgService) {
        this.mContext = context;
        this.mMessageHandler = msgService;
        roadGuide = new RoadGuide();
        getLocation();
    }

    @TargetApi(23)
    public Location getLocation(){
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return null;
        }

        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            // getting GPS information
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting current network state value
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                //todo:GPS와 네트워크 사용이 가능하지 않을 떄
            } else {
                this.isGetLocation = true;
                // 네트워크 정보로 부터 위치값 가져오기
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if (location != null) {
                            // 위도 경도 저장
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                            roadGuide.setMyLocation(myLocation);
                        }
                    }
                }

                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                            if (location != null) {
                                lat = location.getLatitude();
                                lon = location.getLongitude();
                                roadGuide.setMyLocation(myLocation);
                            }
                        }
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return location;
    }

    /**
     * @brief GPS 종료
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GpsService.this);
        }
    }

    /**
     * @brief 위도값을 가져온다
     * @return 위도값
     */
    public double getLatitude() {
        return lat;

    }

    /**
     * @brief 경도값을 가져온다
     * @return 경도값
     */
    public double getLongitude() {
        return lon;
    }

    /**
     * @brief GPS나 wifi 정보가 켜져있는지 확인
     * @return 현재위치
     */
    public boolean isGetLocation() {
        return this.isGetLocation;
    }

    public void showSettingAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        alertDialog.setTitle("GPS 사용유무세팅");
        alertDialog.setMessage("GPS 세팅이 되지 않았을 수 도 있습니다. \n 설정창으로 가시겠습니까?");

        // OK를 누르면 설정창으로 이동합니다
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        mContext.startActivity(intent);
                    }
                });

        // cancel를 누르면 종료합니다
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

   @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();
        sendBroadcast(lat, lon);

        Log.e(TAG,"lnLocationChanged: " + location);
        myLocation = new TMapPoint(location.getLatitude(), location.getLongitude());

        roadGuide.setMyLocation(myLocation);

        Log.i("BluetoothState", "" + mMessageHandler.getmBluetoothService().getState() + ", " + BluetoothService.STATE_CONNECTED);

        if (mMessageHandler.getmBluetoothService().getState() == BluetoothService.STATE_CONNECTED) {
            Log.i(TAG, "bluetooth is connected");
            String message = updateGuide(end);
            mMessageHandler.sendMessage(message);
        } else {
            Log.i(TAG, "bluetooth is not connected");
        }

    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public void onProviderEnabled(String provider) {

    }

    public void onProviderDisabled(String provider) {

    }

    public synchronized void setDest(TMapPoint src, TMapPoint des) {
        end = des;
        myLocation = src;

        roadGuide.toward(myLocation, end);

    }

    public String updateGuide(TMapPoint dest) {

        TMapPOIItem next = roadGuide.getNextPOI();
        String turn = roadGuide.getTurntype();
        // 기기로 보낼 메시지
        String msg;

        if (next != null && turn != null) {
            // 다음 경유지 POI까지 거리
            int distance = (int)next.getDistance(myLocation);

            Log.i("updateGuide", "다음 경유지까지 거리 " + distance + "\n" + "다음 경유지에서의 회전 방향 " + turn);
            if (turn == "201" && distance <= 10) {
                msg = turn;
            } else {
                msg = turn + ", " + distance;
            }
        } else {
            msg = "경로를 탐색중입니다";
        }

        return msg;

    }

    private void sendBroadcast(double la, double lo) {
        Log.d("BroadcastGPS", "" + la + ", " + lo);
        Intent intent = new Intent("myLocation");
        intent.putExtra("latitude", la);
        intent.putExtra("longitude", lo);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}
