package contact.helper.android.myapplication;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Created by danger on 15/9/3.
 */
public class TestAdapter extends BaseAdapter {
    Context context;

    public TestAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = View.inflate(context, R.layout.adapter_test, null);
        return convertView;
    }

    public class ViewHolder {
        public final TextView tvtess;
        public final View root;

        public ViewHolder(View root) {
            tvtess = (TextView) root.findViewById(R.id.tv_tess);
            this.root = root;
        }
    }
}
