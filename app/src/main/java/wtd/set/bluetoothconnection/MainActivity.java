package wtd.set.bluetoothconnection;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    SwitchCompat switchCompat;
    BluetoothAdapter myBluetoothAdapter;
    public ArrayList<BluetoothDevice> devices;
    public DeviceListAdapter deviceListAdapter;
    ListView list_devices;
    Button btn_refresh;

    int callCounterDiscover = 0;
    int callCounterDiscoverability = 0;
    boolean functDiscoverCalled = false;
    boolean functDiscoverabilityCalled = false;

    public TextView text_connected_device;
    public String name;
    public String address;
    public String threadName;

    Button btn_disconnect;
    boolean connected;
    BluetoothDevice pairedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initObjects();
        checkConnected();

        switchCompat.setOnClickListener(view -> {
            if (switchCompat.isChecked()) {
                if (!connected){
                    callCounterDiscover = 0; // set inits to 0
                    callCounterDiscoverability = 0;
                    functDiscoverCalled = false;
                    functDiscoverabilityCalled = false;
                    enableBluetooth(); // call functions
                    callDiscover();
                    callDiscoverability();
                    btn_refresh.setBackgroundResource(R.color.green);
                }
            }else{
                connected = false;
                clearlogs();
                disableBluetooth();
                btn_refresh.setBackgroundResource(R.color.grey);
            }
        });

        btn_refresh.setOnClickListener(view -> {
            if (switchCompat.isChecked()){
                clearlogs();
                callCounterDiscover = 0; // set inits to 0
                functDiscoverCalled = false;
                callDiscover();
            }
        });

        btn_disconnect.setOnClickListener(view -> unpairDevice(pairedDevice));

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(myBroadcastReceiver, filter);
        onClickItem();
    }

    @Override
    protected void onDestroy(){
        logInfo("onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(myBroadcastReceiver);
    }

    private void initObjects() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        myBluetoothAdapter = bluetoothManager.getAdapter();
        devices = new ArrayList<>();
        list_devices = findViewById(R.id.list_devices);
        switchCompat = findViewById(R.id.switch_bluetooth);
        btn_refresh = findViewById(R.id.btn_refresh);
        btn_refresh.setBackgroundResource(R.color.grey);
        text_connected_device = findViewById(R.id.text_connected_device);
        text_connected_device.setText("");
        btn_disconnect = findViewById(R.id.btn_disconnect);
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth(){
        IntentFilter bluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (myBluetoothAdapter != null && !myBluetoothAdapter.isEnabled()) {
            myBluetoothAdapter.enable();
            registerReceiver(myBroadcastReceiver, bluetoothIntent);
        }
    }

    @SuppressLint("MissingPermission")
    private void disableBluetooth(){
        IntentFilter bluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (myBluetoothAdapter.isEnabled()){
            myBluetoothAdapter.disable();
            registerReceiver(myBroadcastReceiver, bluetoothIntent);
        }
    }

    @SuppressLint("MissingPermission")
    private void setDiscoverability(){
        if (myBluetoothAdapter.isEnabled()){
            logInfo("onDiscoverable-button: Making device discoverable for 5 mins.");
            try {
                Method method = myBluetoothAdapter.getClass().getMethod("setScanMode", int.class, int.class);
                method.invoke(myBluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 300);
                functDiscoverabilityCalled = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            logInfo("Trying to call discoverability. Bluetooth is disabled.");
        }
    }

    @SuppressLint("MissingPermission")
    private void disableDiscoverability(){
        try {
            Method method = myBluetoothAdapter.getClass().getMethod("setScanMode", int.class, int.class);
            method.invoke(myBluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 1);
            functDiscoverabilityCalled = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callDiscoverability(){
        new Handler().postDelayed(() -> {
            if (callCounterDiscoverability < 300 && !functDiscoverabilityCalled && !connected){ // set to 5 mins
                callCounterDiscoverability++;
                setDiscoverability();
                callDiscoverability();
            }
        }, 1000);
    }

    @SuppressLint("MissingPermission")
    private void discover(){
        if (myBluetoothAdapter.isEnabled()){
            if (!connected){
                logInfo("onDiscover-button: Looking for unpaired devices");

                if (myBluetoothAdapter.isDiscovering()){
                    myBluetoothAdapter.cancelDiscovery();
                    logInfo("onDiscover-button: Canceling discovery.");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        checkPermissions();
                        myBluetoothAdapter.startDiscovery();
                        IntentFilter discoverIntent = new IntentFilter((BluetoothDevice.ACTION_FOUND));
                        registerReceiver(myBroadcastReceiver, discoverIntent);
                    }
                }
                if (!myBluetoothAdapter.isDiscovering()){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        checkPermissions();
                        myBluetoothAdapter.startDiscovery();
                        IntentFilter discoverFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(myBroadcastReceiver, discoverFilter);
                    }
                }
                if (!switchCompat.isChecked()){
                    switchCompat.setChecked(true);
                }
                functDiscoverCalled = true;

            }else{
                logInfo("Device is already connected!");
            }
        }else{
            logInfo("Trying to call discover. Bluetooth is disabled.");
            if (switchCompat.isChecked()){
                switchCompat.setChecked(false);
            }
        }
    }

    private void callDiscover(){
        new Handler().postDelayed(() -> {
            if (callCounterDiscover < 300 && !functDiscoverCalled && !connected){ // set to 5 mins
                callCounterDiscover++;
                callDiscover();
                discover();
            }
        }, 1000);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions(){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            }
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (permissionCheck != 0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        }else{
            logInfo("onChechPermission: SDK version < LOLLIPOP.");
        }
    }

    @SuppressLint("MissingPermission")
    public void onClickItem(){
        list_devices.setOnItemClickListener((adapterView, view, position, l) -> {
            myBluetoothAdapter.cancelDiscovery();

            String deviceName = devices.get(position).getName();
            String deviceAddress = devices.get(position).getAddress();
            logInfo("onItemClick: You clicked on " + deviceName + " - " + deviceAddress);

            logInfo("Trying to pair with " + deviceName);
            devices.get(position).createBond();
        });
    }

    public void clearlogs(){
        devices = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(getApplicationContext(), devices);
        list_devices.setAdapter(deviceListAdapter);
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    // CHECK CONNECTION
    public void checkConnected()
    {
        new Handler().postDelayed(() -> {
            connected = false;
            if (myBluetoothAdapter.isEnabled()){
                BluetoothAdapter.getDefaultAdapter().getProfileProxy(this, serviceListener, BluetoothProfile.HEADSET);
            }else{
                btn_disconnect.setVisibility(View.INVISIBLE);
                btn_disconnect.setEnabled(false);
                name = "";
                text_connected_device.setText("");
            }
            checkConnected();
            logInfo("Checking");
        }, 2000);
    }

    private final BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener()
    {
        @Override
        public void onServiceDisconnected(int profile){}

        @SuppressLint("MissingPermission")
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy)
        {
            for (BluetoothDevice device : proxy.getConnectedDevices()) {

                pairedDevice = device;
                name = device.getName();
                address = device.getAddress();
                threadName = Thread.currentThread().getName();
                Log.i("onServiceConnected", "|" + device.getName() + " | " + device.getAddress() + " | " + proxy.getConnectionState(device) + "(connected = "
                        + BluetoothProfile.STATE_CONNECTED + ")");
                connected = true;

                // SET INFO
                if (name != null && !name.equals("")) {
                    if (text_connected_device.getText() == null || text_connected_device.getText().equals("")){
                        text_connected_device.setText(name);
                        clearlogs();
                        btn_refresh.setEnabled(false);
                        btn_refresh.setBackgroundResource(R.color.grey);
                        disableDiscoverability();
                        btn_disconnect.setVisibility(View.VISIBLE);
                        btn_disconnect.setEnabled(true);
                    }
                }
            }

            if (!connected){
                callDiscover();
                callDiscoverability();
                btn_refresh.setEnabled(true);
                btn_refresh.setBackgroundResource(R.color.green);
                name = "";
                text_connected_device.setText("");
                btn_disconnect.setVisibility(View.INVISIBLE);
                btn_disconnect.setEnabled(false);
            }

            BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, proxy);
        }
    };

    // BROADCAST RECEIVER
    private final BroadcastReceiver myBroadcastReceiver =  new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            // ON-OFF STATES
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state){
                    case BluetoothAdapter.STATE_OFF:
                        logInfo("onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        logInfo("onReceive: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        logInfo("onReceive: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        logInfo("onReceive: STATE TURNING ON");
                        break;
                }
            }

            // DISCOVERABLE STATES
            if(action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)){
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode){
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        logInfo("onDiscoverable: Discoverability enabled");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        logInfo("onDiscoverable: Discoverability enabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        logInfo("onDiscoverable: Discoverability disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        logInfo("onDiscoverable: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        logInfo("onDiscoverable: Connected");
                        break;
                }
            }

            // DISCOVER STATES
            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                if (!(name != null && !name.equals(""))){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                    logInfo("onReceive: " + device.getName() + " : " + device.getAddress());
                    deviceListAdapter = new DeviceListAdapter(context, devices);
                    list_devices.setAdapter(deviceListAdapter);
                }
            }

            //PAIRING STATES
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice myDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (myDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    logInfo("onBroadcastReceiver: BOND_BONDED");
                }
                if (myDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    logInfo("onBroadcastReceiver: BOND_BONDING");
                }
                if (myDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    logInfo("onBroadcastReceiver: BOND_NONE");
                }
            }
        }
    };

    private void logInfo(String message){
        Log.i(TAG, message);
    }
}