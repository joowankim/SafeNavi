package hi.world.hello.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;

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
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapTapi;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // 지도
    private LinearLayout linearLayoutTmap;

    // 검색 리스트
    private List<TMapPOIItem> list;
    private EditText src, des;
    private ListView listview;
    private ListViewAdapter adapter;

    // 경로 그리기
    private TMapPOIItem startPOI;
    private TMapPOIItem endPOI;
    private String srcPoint;
    private String desPoint;
    private String choiceID;
    private boolean srcInputKeyboard = true;
    private boolean desInputKeyborad = true;

    // 검색 버튼
    private Button btn;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final TMapView tMapView = new TMapView(this);
        final TMapData tMapData = new TMapData();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 지도 그리기
        linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        tMapView.setSKTMapApiKey("056a056a-7ffc-49cf-8836-d8ce2e053f76\n");
        drawMap(tMapView);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        src = (EditText)findViewById(R.id.startPoint);
        des = (EditText)findViewById(R.id.endPoint);
        btn = (Button)findViewById(R.id.btn);

        listview = (ListView) findViewById(R.id.listview1);

        //list 생성
        list = new ArrayList<TMapPOIItem>();

        // 리스트에 연동될 adapter를 생성
        adapter = new ListViewAdapter(list, this);

        // 리스트뷰에 adapter를 연결
        listview.setAdapter(adapter);

        // EditText 바뀔 때마다 검색 자동으로
        src.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {  }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {  }
            @Override
            public void afterTextChanged(Editable editable) {
                if (srcInputKeyboard) {    // 키보드 입력이면 자동검색 실행
                    choiceID = "src";
                    // POI data 검색
                    srcPoint = src.getText().toString();
                    makePOIList(tMapData, srcPoint);
                }
            }
        });

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

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                switch(choiceID){
                    case "src":
                        Log.i("srcInputKeyboard", "false" );
                        srcInputKeyboard = false;
                        startPOI = list.get(position);
                        src.setText(startPOI.getPOIName().toString());
                        list.clear();
                        adapter.notifyDataSetChanged();
                        break;
                    case "des":
                        Log.i("desInputKeyboard", "false" );
                        desInputKeyborad = false;
                        endPOI = list.get(position);
                        des.setText(endPOI.getPOIName().toString());
                        list.clear();
                        adapter.notifyDataSetChanged();
                        break;
                }
            }
        });

        // 경로 표시
        // SKT타워(출발지)
        // N서울타워(목적지)

        src.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.i("srcInputKeyboard", "true");
                    srcInputKeyboard = true;
                }
                return false;
            }
        });
        des.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.i("desInputKeyboard", "true");
                    desInputKeyborad = true;
                }
                return false;
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    tMapView.setCenterPoint(startPOI.getPOIPoint().getLongitude(), startPOI.getPOIPoint().getLatitude(), true);
                    drawPath(tMapView, startPOI, endPOI);
                    hideKeyboard();
                    Toast.makeText(getApplicationContext(), startPOI.getPOIAddress().toString() + "부터 " + endPOI.getPOIAddress().toString() + "까지 ", Toast.LENGTH_SHORT).show();
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "출발지와 목적지를 입력하고 선택해주세요", Toast.LENGTH_SHORT).show();
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
     * @param startPoint    출발지
     * @param endPoint      목적지
     */
    public void drawPath(final TMapView tMapView, TMapPOIItem startPoint, TMapPOIItem endPoint) {

        final TMapPoint start = new TMapPoint(startPoint.getPOIPoint().getLatitude(), startPoint.getPOIPoint().getLongitude());
        final TMapPoint end = new TMapPoint(endPoint.getPOIPoint().getLatitude(), endPoint.getPOIPoint().getLongitude());

        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    TMapPolyLine tMapPolyLine = new TMapData().findPathData(start, end);
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
        imm.hideSoftInputFromWindow(src.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(des.getWindowToken(), 0);
    }
}
