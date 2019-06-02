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

/// @class ListViewAdapter
/// @brief show ListView
public class ListViewAdapter extends BaseAdapter {

    private Context context;
    private List<TMapPOIItem> list;
    private LayoutInflater inflate;
    private ViewHolder viewHolder;
    
    /// @brief constructor of ListViewAdapter
    /// @param list list
    /// @param context context of current scene
    public ListViewAdapter(List<TMapPOIItem> list, Context context) {
        this.list = list;
        this.context = context;
        this.inflate = LayoutInflater.from(context);
    }
    
    /// @brief get list size
    /// @return list size
    @Override
    public int getCount(){
        return list.size();
    }
    /// @brief get item id
    /// @param position current item's location in ListView
    /// @return current position of the item
    @Override
    public long getItemId(int position){
        return position;
    }
    /// @brief get item
    /// @param position current item's location in ListView
    /// @return the pointing item information
    @Override
    public Object getItem(int position){
        return list.get(position);
    }
    /// @brief floating view of item on list
    /// @param position item's position
    /// @param covertView view
    /// @param parent 
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        if (convertView == null){
            convertView = inflate.inflate(R.layout.custom_listview, null);

            viewHolder = new ViewHolder();
            viewHolder.label = (TextView) convertView.findViewById(R.id.name);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }
        try {
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

