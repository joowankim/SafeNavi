package hi.world.hello.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // 지도
    private LinearLayout linearLayoutTmap;
    private TMapView tMapView;

    // 검색 리스트
    private List<TMapPOIItem> list;
    private EditText des;
    private ListView listview;
    private ListViewAdapter adapter;

    // 경로 그리기
    private TMapPOIItem endPOI;
    private String desPoint;
    private String choiceID;
    private boolean desInputKeyborad = true;
    private TMapPoint myLocation;

    // 검색 버튼
    private Button searchBtn;
    private InputMethodManager imm;

    // 길찾기 종료 버튼
    private Button stopBtn;

    // GPS
    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isPermission = false;

    // GPS Tracker class
    private GpsService gps;

    // Intent request code
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // bluetooth service
    private MessageHandler msgService = null;
    private BluetoothService btService = null;
    private Button connectBtn;
    private Button sendBtn;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    // road guide
    private Button guideBtn;


    // 나중에 삭제함
    // bluetooth용 editText
    private EditText BTText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        tMapView = new TMapView(this);
        final TMapData tMapData = new TMapData();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 지도 그리기
        linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        tMapView.setSKTMapApiKey("056a056a-7ffc-49cf-8836-d8ce2e053f76\n");
        drawMap(tMapView);

        // BluetoothService 클래스 생성
        if (btService == null) {
            btService = new BluetoothService(this, mHandler);
        }
        if (msgService == null) {
            msgService = new MessageHandler(this, btService);
        }


        // Bluetooth 연결 버튼
        connectBtn = (Button)findViewById(R.id.btnConnect);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btService.getDeviceState()) {
                    // 블루투스가 지원 가능한 기기일 때
                    btService.enableBluetooth();
                } else {
                    finish();
                }
            }
        });

        // BTText
        BTText = (EditText)findViewById(R.id.BTText);

        // Bluetooth 보내기 버튼
        sendBtn = (Button)findViewById(R.id.btnSend);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = BTText.getText().toString();
                msgService.sendMessage(msg);
            }
        });

        // keyboard 매니저
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        des = (EditText)findViewById(R.id.endPoint);
        searchBtn = (Button)findViewById(R.id.btn);
        stopBtn = (Button)findViewById(R.id.stop);
        listview = (ListView) findViewById(R.id.listview1);

        // list 생성
        // 리스트에 연동될 adapter를 생성
        // 리스트뷰에 adapter를 연결
        list = new ArrayList<TMapPOIItem>();
        adapter = new ListViewAdapter(list, this);
        listview.setAdapter(adapter);

        // 내 위치를 지도에 표시할 것인지 결정 (파란색 원)
        tMapView.setIconVisibility(true);
        // 마시멜로우(sdk 23) 이상에서 gps 사용할 때 권한 허가 받기
        callPermission();

        // GPS 권한 요청
        if (!isPermission) {
            callPermission();
            return;
        }

        gps = new GpsService(MainActivity.this, msgService);
        // GPS 사용 유무 가져오기
        if (gps.isGetLocation()){
            myLocation = new TMapPoint(gps.getLatitude(), gps.getLongitude());
            tMapView.setCenterPoint(myLocation.getLongitude(), myLocation.getLatitude());
            tMapView.setLocationPoint(myLocation.getLongitude(), myLocation.getLatitude());
        } else {
            // GPS 사용할수 없음
            gps.showSettingAlert();
        }


        // EditText 바뀔 때마다 검색 자동으로
        des.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {  }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {  }
            @Override
            public void afterTextChanged(Editable editable) {
                if (desInputKeyborad) {    // 키보드 입력이면 자동검색 실행
                    choiceID = "des";
                    // POI data 검색
                    desPoint = des.getText().toString();
                    makePOIList(tMapData, desPoint);
                }
            }
        });

        // 자동 검색 목록 중에 선택된 아이템이 있을 때
        // 해당 아이템으로 EditText 내용을 바꿔준다
        // 그리고 검색 리스트는 모두 지워 화면에 나타나지 않게 한다
        // 이때 setText는 addTextChangedListener가 반응하지 않도록
        // keyboard flag를 false로 바꿔준다
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                switch(choiceID){
                    case "des":
                        desInputKeyborad = false;
                        endPOI = list.get(position);
                        des.setText(endPOI.getPOIName().toString());
                        list.clear();
                        adapter.notifyDataSetChanged();
                        break;
                }
            }
        });

        // EditText가 터치될 때만 addTextChangedListener가 반응하도록 flag를 변경
        des.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    desInputKeyborad = true;
                }
                return false;
            }
        });

        // 검색 버튼 눌렀을 때
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    // 화면 중심 출발지로 이동, 경로 표시, 키보드 내림
                    tMapView.setCenterPoint(myLocation.getLongitude(), myLocation.getLatitude(), true);
                    if (endPOI != null) {
                        drawPath(tMapView, endPOI);
                    } else {
                        Toast.makeText(getApplicationContext(), "목적지를 입력해주세요", Toast.LENGTH_SHORT).show();
                    }
                    hideKeyboard();
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "현재 위치를 탐색중입니다", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // gps 서비스 종료 버튼
        stopBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                gps.stopUsingGPS();
            }
        });

        // 길 안내 버튼
        guideBtn = (Button)findViewById(R.id.btnGuide);
        guideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("BluetoothState", "" + msgService.getmBluetoothService().getState() + ", " + BluetoothService.STATE_CONNECTED);

                if (msgService.getmBluetoothService().getState() == BluetoothService.STATE_CONNECTED) {
                    if (endPOI != null) {
                        Log.i("guideBtn", "bluetooth is connected");
                        TMapPoint end = new TMapPoint(endPOI.getPOIPoint().getLatitude(), endPOI.getPOIPoint().getLongitude());
                        String message = gps.updateGuide(end);
                        msgService.sendMessage(message);
                    } else {
                        Toast.makeText(getApplicationContext(), "목적지를 입력해주세요", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.i("guideBtn", "bluetooth is not connected");
                }
            }
        });
    }

    /**
     * @brief 지도 그리기
     * @param tMapView
     */
    public void drawMap(final TMapView tMapView){
        linearLayoutTmap.addView(tMapView);
    }

    /**
     * @brief 지도에 경로를 그려준다
     * @param tMapView      현재 보여지는 지도
     * @param endPoint      목적지
     */
    public void drawPath(final TMapView tMapView, TMapPOIItem endPoint) {

        final TMapPoint end = new TMapPoint(endPoint.getPOIPoint().getLatitude(), endPoint.getPOIPoint().getLongitude());

        gps.setDest(myLocation, end);

        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    TMapPolyLine tMapPolyLine = new TMapData().findPathDataWithType(TMapData.TMapPathType.CAR_PATH, myLocation, end);
                    tMapPolyLine.setLineColor(Color.BLUE);
                    tMapPolyLine.setLineWidth(2);
                    tMapView.addTMapPolyLine("Line1", tMapPolyLine);

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }

    /**
     * @brief 실시간 POI 리스트 만들기
     * @param tMapData
     * @param searchPoint
     */
    private void makePOIList(final TMapData tMapData, final String searchPoint){
        // 리스트 초기화
        list.clear();
        // 검색된 리스트 추가
        findPOI(tMapData, searchPoint);
        adapter.notifyDataSetChanged();
    }

    /**
     * @brief 검색된 리스트 추가가
     * @param tMapData
     * @param searchPoint
     */
    private void findPOI(TMapData tMapData, String searchPoint){

        tMapData.findAllPOI(searchPoint, new TMapData.FindAllPOIListenerCallback() {
            @Override
            public void onFindAllPOI(ArrayList poiItem) {
                for (int i = 0; i < poiItem.size(); i++) {
                    final TMapPOIItem item = (TMapPOIItem) poiItem.get(i);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            list.add(item);
                            adapter.notifyDataSetChanged();
                        }
                    });

                    Log.d("POI Name: ", item.getPOIName().toString() + ", " +
                            "Address: " + item.getPOIAddress().replace("null", "") + ", " +
                            "Point: " + item.getPOIPoint().toString());

                }
            }
        });
    }

    /**
     * @brief 키보드 내리기
     */
    private void hideKeyboard()
    {
        imm.hideSoftInputFromWindow(des.getWindowToken(), 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isAccessFineLocation = true;
        } else if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isAccessCoarseLocation = true;
        }

        if (isAccessFineLocation && isAccessCoarseLocation) {
            isPermission = true;
        }
    }

    private void callPermission() {
        // Check the SDK version and whether the permission is already granted or not
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            isPermission = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // action 이름이 "myLocation"으로 정의된 intent를 수신한다
        // observer의 이름은 mMessageReceiver이다
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("myLocation"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "myLocation" is broadcasted
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            double la = intent.getDoubleExtra("latitude", Double.MIN_VALUE);
            double lo = intent.getDoubleExtra("longitude", Double.MIN_VALUE);

            myLocation.setLatitude(la);
            myLocation.setLongitude(lo);
            tMapView.setLocationPoint(myLocation.getLongitude(), myLocation.getLatitude());

            Log.d("receiver", "location: " + la + ", " + lo);
            Toast.makeText(getApplicationContext(), "현재 위치 : " + la + ", " + lo, Toast.LENGTH_SHORT).show();
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d("onActivityResult", "" + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    btService.getDeviceInfo(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // 확인 눌렀을 때 취할 행동
                    btService.scanDevice();
                } else {
                    // 취소 눌렀을 때 취할 행동
                    Log.d("BluetoothService", "Bluetooth is not enabled");
                }
                break;
        }
    }

}
