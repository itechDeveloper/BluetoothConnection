package wtd.set.bluetoothconnection;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends BaseAdapter {

    Context context;
    LayoutInflater layoutInflater;
    ArrayList<BluetoothDevice> devices;

    public DeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices){
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        view = layoutInflater.inflate(R.layout.device_adapter_view, null);
        BluetoothDevice device = devices.get(position);
        TextView name = view.findViewById(R.id.text_device_name);
        TextView address = view.findViewById(R.id.text_device_address);
        name.setText(device.getName());
        address.setText(device.getAddress());
        return view;
    }
}
