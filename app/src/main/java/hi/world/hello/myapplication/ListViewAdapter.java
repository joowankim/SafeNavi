package hi.world.hello.myapplication;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.Tmap.TMapPOIItem;

import java.util.List;

public class ListViewAdapter extends BaseAdapter {

    private Context context;
    private List<TMapPOIItem> list;
    private LayoutInflater inflate;
    private ViewHolder viewHolder;

    public ListViewAdapter(List<TMapPOIItem> list, Context context) {
        this.list = list;
        this.context = context;
        this.inflate = LayoutInflater.from(context);
    }

    @Override
    public int getCount(){
        return list.size();
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public Object getItem(int position){
        return list.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Log.i("TEST", "getView 호출");
        if (convertView == null){
            convertView = inflate.inflate(R.layout.custom_listview, null);

            viewHolder = new ViewHolder();
            viewHolder.label = (TextView) convertView.findViewById(R.id.name);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }
        try {
            // 아이템 내 각 위젯에 데이터 반영
            viewHolder.label.setText(list.get(position).getPOIName().toString());
        } catch(Exception e){
            e.printStackTrace();
        }
        return convertView;
    }

    class ViewHolder{
        public TextView label;
    }
}

