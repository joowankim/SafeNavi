package hi.world.hello.myapplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class BluetoothService {
    /// @static final
    private static final String TAG = "BluetoothService";   ///< for Debugging

    /// @static Intent request code
    private static final int REQUEST_CONNECT_DEVICE = 1;    ///< request
    private static final int REQUEST_ENABLE_BT = 2;         ///< permission

    /// @static RFCOMM Protocol
    private static final UUID MY_UUID = UUID
            .fromString("your UUID KEY");
    
    private BluetoothAdapter btAdapter; ///< bluetooth adapter

    private Activity mActivity; ///< context
    private Handler mHandler;   ///< message handler

    private ConnectThread mConnectThread; ///< connection thread
    private ConnectedThread mConnectedThread; ///< check the state of connection

    private int mState; ///< represent current state

    // @static variables for states of bluetooth
    public static final int STATE_NONE = 0; ///< we're doing nothing
    public static final int STATE_LISTEN = 1; ///< now listening for incoming
    // @static connections
    public static final int STATE_CONNECTING = 2; ///< now initiating an outgoing
    // @static connection
    public static final int STATE_CONNECTED = 3; ///< now connected to a remote

    /// @brief Constructors
    /// @param ac context
    /// @param h message handler
    public BluetoothService(Activity ac, Handler h) {
        mActivity = ac;
        mHandler = h;

        // get BluetoothAdapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    /// @brief get bluetooth adapter
    public BluetoothAdapter getBtAdapter() {
        return btAdapter;
    }

    /**
     * @brief Check the Bluetooth support
     *
     * @return boolean
     */
    public boolean getDeviceState() {
        Log.i(TAG, "Check the Bluetooth support");

        if (btAdapter == null) {
            Log.d(TAG, "Bluetooth is not available");

            return false;

        } else {
            Log.d(TAG, "Bluetooth is available");

            return true;
        }
    }

    /**
     * @brief Check the enabled Bluetooth
     */
    public void enableBluetooth() {
        Log.i(TAG, "Check the enabled Bluetooth");

        if (btAdapter.isEnabled()) {
            // bluetooth state of device is ON
            Log.d(TAG, "Bluetooth Enable Now");

            // Next Step
            scanDevice();
        } else {
            // bluetooth state of device is OFF
            Log.d(TAG, "Bluetooth Enable Request");

            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(i, REQUEST_ENABLE_BT);
        }
    }

    /**
     * @brief Available device search
     */
    public void scanDevice() {
        Log.d(TAG, "Scan Device");

        Intent serverIntent = new Intent(mActivity, DeviceListActivity.class);
        mActivity.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    /**
     * @brief after scanning and get device info
     * @details get the MAC addresses of each device that can be connected and 
              objects of the devices
     * @param data device list that can be connected through bluetooth
     */
    public void getDeviceInfo(Intent data) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        Log.d(TAG, "Get Device Info \n" + "address : " + address);

        connect(device);
    }

    /** 
     * @brief set the state of Bluetooth
     * @param state current state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    /// @brief get the state of Bluetooth 
    /// @param current state
    public synchronized int getState() {
        return mState;
    }
    /// @brief connection start
    /// @details if mConnectThread is null, it cancel any thread attempting to make a connection
    ///         if mConnectedThread is null, it cancel any thread currently running a connection
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread == null) {

        } else {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    /// @brief Initialize ConnectThread (remove connection of device)
    /// @details if this device is connected sth, it cancel any thread attempting to make a connection
    ///         and it cancel any thread currently running a connection
    /// @param device device name that will be tried to connect
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread == null) {

            } else {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);

        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /// @brief Initialize ConnectedThread
    /// @param socket it it used to connect deivces
    /// @param device taregt device name
    public synchronized void connected(BluetoothSocket socket,
                                       BluetoothDevice device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread == null) {

        } else {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    /// @brief all thread stop
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /// @brief writing values (sending part)
    /// @param out sent message string
    public void write(byte[] out) { // Create temporary object
        ConnectedThread r; // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        } // Perform the write unsynchronized
        r.write(out);
    }

    /// @brief setting that connection state is failed
    private void connectionFailed() {
        setState(STATE_LISTEN);
    }

    /// @brief setting that connection state is lost
    private void connectionLost() {
        setState(STATE_LISTEN);

    }
    
    /** @class ConnectThread
     * @brief get a BluethoothSocket to connect with the given
     * BluetoothDevice try { // MY_UUID is the app's UUID string, also
     * used by the server // code tmp =
     * device.createRfcommSocketToServiceRecord(MY_UUID);
     *
     * try { Method m = device.getClass().getMethod(
     * "createInsecureRfcommSocket", new Class[] { int.class }); try {
     * tmp = (BluetoothSocket) m.invoke(device, 15); } catch
     * (IllegalArgumentException e) { // TODO Auto-generated catch block
     * e.printStackTrace(); } catch (IllegalAccessException e) { // TODO
     * Auto-generated catch block e.printStackTrace(); } catch
     * (InvocationTargetException e) { // TODO Auto-generated catch
     * block e.printStackTrace(); }
     *
     * } catch (NoSuchMethodException e) { // TODO Auto-generated catch
     * block e.printStackTrace(); } } catch (IOException e) { } /
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            /*
             * / // Get a BluetoothSocket to connect with the given
             * BluetoothDevice try { // MY_UUID is the app's UUID string, also
             * used by the server // code tmp =
             * device.createRfcommSocketToServiceRecord(MY_UUID);
             *
             * try { Method m = device.getClass().getMethod(
             * "createInsecureRfcommSocket", new Class[] { int.class }); try {
             * tmp = (BluetoothSocket) m.invoke(device, 15); } catch
             * (IllegalArgumentException e) { // TODO Auto-generated catch block
             * e.printStackTrace(); } catch (IllegalAccessException e) { // TODO
             * Auto-generated catch block e.printStackTrace(); } catch
             * (InvocationTargetException e) { // TODO Auto-generated catch
             * block e.printStackTrace(); }
             *
             * } catch (NoSuchMethodException e) { // TODO Auto-generated catch
             * block e.printStackTrace(); } } catch (IOException e) { } /
             */

            // create BluetoothSocket using device information
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }
        /// @brief work of ConnectThread
        /// @details stop device searching before trying connection
        ///          to prevent connection to be slower by searching devices
        ///          try Bluetoothsocket connection
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // stop device searching before trying connection
            // to prevent connection to be slower by searching deivices
            btAdapter.cancelDiscovery();

            // try BluetoothSocket connection
            try {
                // BluetoothSocket connection return : succes or exception
                mmSocket.connect();
                Log.d(TAG, "Connect Success");

            } catch (IOException e) {
                connectionFailed(); // loaded method when connectio failed
                Log.d(TAG, "Connect Fail");

                // close socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG,
                            "unable to close() socket during connection failure",
                            e2);
                }
                // 연결중? 혹은 연결 대기상태인 메소드를 호출한다. call methods on trying connection
                BluetoothService.this.start();
                return;
            }

            // reset ConnectThread class
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // start ConnectThread
            connected(mmSocket, mmDevice);
        }
        /// @brief socket close
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    /// @class ConnectedThread
    /// @brief get inputstream and outputstream of bluetoothSocket
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // get inputstream and outputstream of BluetoothSocket
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        /// @brief keep listening to the InputStream while connected
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // reading values getting from InputStream (getting values)
                    bytes = mmInStream.read(buffer);

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * @brief Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                // writing values (sending values)
                mmOutStream.write(buffer);

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        /// @brief socket close
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}
