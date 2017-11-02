package drivers.miurasample.module.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import com.miurasystems.miuralibrary.BaseBluetooth;

import java.util.ArrayList;

/**
 * Created by mgadzala on 2016-06-09.
 */
public class BluetoothPairing {

    private static final String TAG = "BluetoothPairing";
    private Context context;

    /**
     * whole list visible by Android system
     */
    private ArrayList<BluetoothDevice> allVisibleDevices;

    /**
     * all Mkura's device available in allVisibleDevices
     */
    private ArrayList<BluetoothDevice> allMiuraDevices;

    /**
     * only default devices selected by user
     */
    private ArrayList<BluetoothDevice> pairedDevices;

    /**
     * only Miura's devices defined in {@link BluetoothDeviceType}, but not default
     */
    private ArrayList<BluetoothDevice> nonPairedDevices;


    public BluetoothPairing(Context context) {
        this.context = context;
        initAllAvailableDevices();
        initMiuraDevices();
        initPairedDevices();
        initNonPairedDevices();
    }

    private void initAllAvailableDevices() {
        ArrayList<BluetoothDevice> devicesSet = BaseBluetooth.getInstance().getDevice();
        allVisibleDevices = new ArrayList<>();
        if (devicesSet != null) {
            allVisibleDevices.addAll(devicesSet);
        }
    }

    private void initMiuraDevices() {
        allMiuraDevices = new ArrayList<>();
        for (BluetoothDevice bluetoothDevice : allVisibleDevices) {
            for (BluetoothDeviceType deviceType : BluetoothDeviceType.values()) {
                if (bluetoothDevice.getName().toLowerCase().contains(deviceType.getDeviceTypeName().toLowerCase())) {
                    allMiuraDevices.add(bluetoothDevice);
                    break;
                }
            }
        }
    }

    private void initPairedDevices() {
        pairedDevices = new ArrayList<>();
        //for all device type
        for (BluetoothDeviceType deviceType : BluetoothDeviceType.values()) {
            //get default address for selected type
            String deviceAddress = getDefaultDeviceAddress(context, deviceType);
            if (deviceAddress != null) {
                BluetoothDevice device = findByAddress(deviceAddress, allMiuraDevices);
                if (device != null) {
                    pairedDevices.add(device);
                }
            }
        }
    }

    private void initNonPairedDevices() {
        nonPairedDevices = new ArrayList<>();
        for (BluetoothDevice miuraDevice : allMiuraDevices) {

            boolean isAlreadyPaired = false;
            for (BluetoothDevice device : pairedDevices) {
                if (device.getAddress().equals(miuraDevice.getAddress())) {
                    isAlreadyPaired = true;
                    break;
                }
            }

            if (!isAlreadyPaired) {
                nonPairedDevices.add(miuraDevice);
            }
        }
    }

    public static void setDefaultDevice(Context context, BluetoothDeviceType deviceType, String address) {
        SharedPreferences prefs = context.getSharedPreferences("DevicePreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(deviceType.getDeviceTypeName(), address);
        editor.apply();
    }

    public static String getDefaultDeviceAddress(Context context, BluetoothDeviceType deviceType) {
        SharedPreferences prefs = context.getSharedPreferences("DevicePreferences", Context.MODE_PRIVATE);
        return prefs.getString(deviceType.getDeviceTypeName(), null);
    }

    public static BluetoothDevice findByAddress(String address, ArrayList<BluetoothDevice> bluetoothDevices) {

        if (address == null) {
            return null;
        }

        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            if (address.equals(bluetoothDevice.getAddress())) {
                return bluetoothDevice;
            }
        }

        return null;
    }

    public ArrayList<BluetoothDevice> getPairedDevices() {
        return pairedDevices;
    }

    public ArrayList<BluetoothDevice> getNonPairedDevices() {
        return nonPairedDevices;
    }

    public BluetoothDevice getDefaultByType(BluetoothDeviceType type) {
        String address = getDefaultDeviceAddress(context, type);
        return findByAddress(address, getPairedDevices());
    }
}
