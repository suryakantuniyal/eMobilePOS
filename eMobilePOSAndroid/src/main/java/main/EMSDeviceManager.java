package main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.dao.DeviceTableDAO;
import com.android.emobilepos.R;
import com.android.emobilepos.models.Orders;
import com.android.emobilepos.models.realms.Device;
import com.android.support.DeviceUtils;
import com.android.support.Global;
import com.android.support.MyPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import drivers.EMSAPT50;
import drivers.EMSBixolon;
import drivers.EMSBixolonRD;
import drivers.EMSBlueBambooP25;
import drivers.EMSDeviceDriver;
import drivers.EMSELO;
import drivers.EMSEM100;
import drivers.EMSEM70;
import drivers.EMSEpson;
import drivers.EMSGPrinterPT380;
import drivers.EMSHPEngageOnePrimePrinter;
import drivers.EMSHandpoint;
import drivers.EMSIngenico;
import drivers.EMSIngenicoEVO;
import drivers.EMSIngenicoMoby85;
import drivers.EMSKDC425;
import drivers.EMSMagtekAudioCardReader;
import drivers.EMSMagtekSwiper;
import drivers.EMSNomad;
import drivers.EMSOT310;
import drivers.EMSOneil4te;
import drivers.EMSPAT215;
import drivers.EMSPaxA920;
import drivers.EMSPowaPOS;
import drivers.EMSStar;
import drivers.EMSZebraEM220ii;
import drivers.EMSmePOS;
import drivers.EMSsnbc;
import drivers.EMSKDS;
import interfaces.EMSConnectionDelegate;
import interfaces.EMSDeviceManagerPrinterDelegate;
import interfaces.EMSPrintingDelegate;

public class EMSDeviceManager implements EMSPrintingDelegate, EMSConnectionDelegate {

    private Dialog promptDialog;
    private AlertDialog.Builder dialogBuilder;
    private EMSDeviceDriver aDevice = null;
    private EMSDeviceManagerPrinterDelegate currentDevice;

    // String = product category assigned to the printer.
    // List<Orders> = items to be printed.
    private HashMap<String, List<Orders>> remoteStationQueue = new HashMap<>();

    public EMSDeviceManager getManager() {
        return this;
    }

    public void loadDrivers(Context activity, int type, PrinterInterfase interfase) {

        switch (type) {
            case Global.APT_50:
                aDevice = new EMSAPT50();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.MAGTEK:
                aDevice = new EMSMagtekAudioCardReader();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.MAGTEK_EMBEDDED:
                aDevice = new EMSMagtekSwiper();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.STAR:
                aDevice = new EMSStar();
                if (interfase == PrinterInterfase.BLUETOOTH)
                    promptTypeOfStarPrinter(activity);
                else
                    promptStarPrinterSize(true, activity);
                break;
            case Global.GPRINTER:
                aDevice = new EMSGPrinterPT380();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.BIXOLON_RD:
                aDevice = new EMSBixolonRD(EMSBixolonRD.BixolonCountry.PANAMA);
                aDevice.connect(activity, -1, true, this);
                break;
            case Global.BAMBOO:
                aDevice = new EMSBlueBambooP25();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.BIXOLON:
                aDevice = new EMSBixolon();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.ZEBRA:
                aDevice = new EMSZebraEM220ii();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.ONEIL:
                aDevice = new EMSOneil4te();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.HP_EONEPRIME:
                aDevice = new EMSHPEngageOnePrimePrinter();
                aDevice.connect(activity, -1, true, this);
                break;
            case Global.SNBC:
                aDevice = new EMSsnbc();
                aDevice.connect(activity, -1, true, this);
                break;
            case Global.POWA:
                aDevice = new EMSPowaPOS();
                aDevice.connect(activity, -1, true, this);
                break;
            case Global.PAT215:
                aDevice = new EMSPAT215();
                aDevice.connect(activity, -1, true, this);
                break;
            case Global.EM100:
                aDevice = new EMSEM100();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.EM70:
                aDevice = new EMSEM70();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.OT310:
                aDevice = new EMSOT310();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.INGENICOMOBY85:
                aDevice = new EMSIngenicoMoby85();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.KDC425:
                aDevice = new EMSKDC425();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.HANDPOINT:
                aDevice = new EMSHandpoint();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.NOMAD:
                aDevice = new EMSNomad();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.MEPOS:
                aDevice = new EMSmePOS();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.ELOPAYPOINT:
                aDevice = new EMSELO();
                aDevice.connect(activity, -1, true, this);
                break;
            case Global.ISMP:
                aDevice = new EMSIngenico();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.ICMPEVO:
                aDevice = new EMSIngenicoEVO();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.PAX_A920:
                aDevice = new EMSPaxA920();
                aDevice.connect(activity, -1, false, this);
                break;
            case Global.EPSON:
                aDevice = new EMSEpson();
                aDevice.connect(activity, -1, false, this);
                break;
        }
    }

    public boolean loadMultiDriver(Activity activity, int type, int paperSize, boolean isPOSPrinter, String portName, String portNumber) {
        switch (type) {
            case Global.MAGTEK:
                aDevice = new EMSMagtekAudioCardReader();
                break;
            case Global.APT_50:
                aDevice = new EMSAPT50();
                break;
            case Global.MAGTEK_EMBEDDED:
                aDevice = new EMSMagtekSwiper();
                break;
            case Global.BAMBOO:
                aDevice = new EMSBlueBambooP25();
                break;
            case Global.BIXOLON:
                aDevice = new EMSBixolon();
                break;
            case Global.BIXOLON_RD:
                aDevice = new EMSBixolonRD(EMSBixolonRD.BixolonCountry.PANAMA);
                break;
            case Global.ZEBRA:
                aDevice = new EMSZebraEM220ii();
                break;
            case Global.ONEIL:
                aDevice = new EMSOneil4te();
                break;
            case Global.SNBC:
                aDevice = new EMSsnbc();
                break;
            case Global.HP_EONEPRIME:
                aDevice = new EMSHPEngageOnePrimePrinter();
                break;
            case Global.POWA:
                aDevice = new EMSPowaPOS();
                break;
            case Global.EM100:
                aDevice = new EMSEM100();
                break;
            case Global.PAT215:
                aDevice = new EMSPAT215();
                break;
            case Global.ISMP:
                aDevice = new EMSIngenico();
                break;
            case Global.STAR:
                aDevice = new EMSStar();
                break;
            case Global.GPRINTER:
                aDevice = new EMSGPrinterPT380();
                break;
            case Global.EM70:
                aDevice = new EMSEM70();
                break;
            case Global.OT310:
                aDevice = new EMSOT310();
                break;
            case Global.INGENICOMOBY85:
                aDevice = new EMSIngenicoMoby85();
                break;
            case Global.KDC425:
                aDevice = new EMSKDC425();
                break;
            case Global.HANDPOINT:
                aDevice = new EMSHandpoint();
                break;
            case Global.MEPOS:
                aDevice = new EMSmePOS();
                break;
            case Global.NOMAD:
                aDevice = new EMSNomad();
                break;
            case Global.ELOPAYPOINT:
                aDevice = new EMSELO();
                break;
            case Global.ICMPEVO:
                aDevice = new EMSIngenicoEVO();
                break;
            case Global.PAX_A920:
                aDevice = new EMSPaxA920();
                break;
            case Global.EPSON:
                aDevice = new EMSEpson();
                break;
            case Global.KDS:
                aDevice = new EMSKDS(portName,Integer.valueOf(portNumber));
                break;
        }
        return aDevice != null && aDevice.autoConnect(activity, this, paperSize, isPOSPrinter, portName, portNumber);
    }

    private void promptTypeOfStarPrinter(final Context activity) {
        ListView listViewPrinterType = new ListView(activity);
        ArrayAdapter<String> typesAdapter;
        dialogBuilder = new AlertDialog.Builder(activity);

        String[] values = new String[]{"Thermal POS Printer", "Portable Printer"};
        typesAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, values);

        listViewPrinterType.setAdapter(typesAdapter);
        listViewPrinterType.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                                    long arg3) {
                promptDialog.dismiss();
                if (pos == 0)
                    promptStarPrinterSize(true, activity);
                else
                    promptStarPrinterSize(false, activity);
            }
        });

        dialogBuilder.setView(listViewPrinterType);

        dialogBuilder.setView(listViewPrinterType);
        dialogBuilder.setTitle("Choose Printer Type");
        promptDialog = dialogBuilder.create();

        promptDialog.show();
    }

    private void promptStarPrinterSize(final boolean isPOSPrinter, final Context activity) {
        ListView listViewPaperSizes = new ListView(activity);
        ArrayAdapter<String> bondedAdapter;
        dialogBuilder = new AlertDialog.Builder(activity);

        String[] values = new String[]{"2inch (58mm)", "3inch (78mm)", "4inch (112mm)"};
        final int[] paperSize = new int[]{32, 48, 69};
        bondedAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, values);

        listViewPaperSizes.setAdapter(bondedAdapter);

        listViewPaperSizes.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                    long arg3) {
                promptDialog.dismiss();
                MyPreferences myPref = new MyPreferences(activity);
                myPref.posPrinter(false, isPOSPrinter);
                myPref.printerAreaSize(false, paperSize[position]);
                aDevice.connect(activity, paperSize[position], isPOSPrinter, EMSDeviceManager.this);
                List<Device> list = new ArrayList<>();
                Device device = DeviceTableDAO.getByName(myPref.getPrinterName());
                if (device != null) {
                    device.setPOS(isPOSPrinter);
                    device.setTextAreaSize(paperSize[position]);
                    list.add(device);
                    DeviceTableDAO.insert(list);
                    device.setEmsDeviceManager(EMSDeviceManager.this);
                    Global.printerDevices.add(device);
                }
            }
        });

        dialogBuilder.setView(listViewPaperSizes);

        dialogBuilder.setView(listViewPaperSizes);
        dialogBuilder.setTitle("Choose Printable Area Size");
        promptDialog = dialogBuilder.create();

        promptDialog.show();
    }

    public EMSDeviceManagerPrinterDelegate getCurrentDevice() {
        return currentDevice;
    }

    public void setCurrentDevice(EMSDeviceManagerPrinterDelegate currentDevice) {
        this.currentDevice = currentDevice;
    }

    public void printerDidFinish() {

    }

    public void printerDidDisconnect(Error err) {

    }

    public void printerDidBegin() {

    }

    public void driverDidConnectToDevice(EMSDeviceDriver theDevice, boolean showPrompt, Context activity) {
        boolean isDestroyed = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (activity != null && activity instanceof Activity) {
                if (((Activity) activity).isDestroyed()) {
                    isDestroyed = true;
                }
            }
        }
        if (activity != null && activity instanceof Activity) {
            if (showPrompt && !((Activity) activity).isFinishing() && !isDestroyed) {
                Builder dialog = new Builder(activity);
                dialog.setNegativeButton(R.string.button_ok, null);
                AlertDialog alert = dialog.create();
                alert.setTitle(R.string.dlog_title_confirm);
                alert.setMessage("Device is Online");
                alert.show();
            }
        }
        if (activity != null) {
            DeviceUtils.sendBroadcastDeviceConnected(activity);
        }
        theDevice.registerAll();

    }

    public void driverDidDisconnectFromDevice(EMSDeviceDriver theDevice, boolean showPrompt) {

    }

    public void driverDidNotConnectToDevice(EMSDeviceDriver theDevice, String err, boolean showPrompt, Context activity) {

        boolean isDestroyed = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (activity instanceof Activity) {
                if (((Activity) activity).isDestroyed()) {
                    isDestroyed = true;
                }
            }
        }
        if (activity instanceof Activity) {
            if (showPrompt && !((Activity) activity).isFinishing() && !isDestroyed) {
                Builder dialog = new Builder(activity);
                dialog.setNegativeButton("Ok", null);
                AlertDialog alert = dialog.create();
                alert.setTitle(R.string.dlog_title_error);
                alert.setMessage("Failed to connect device: \n" + err);
                alert.show();
            }
        }
    }

    public HashMap<String, List<Orders>> getRemoteStationQueue() {
        return remoteStationQueue;
    }

    public void setRemoteStationQueue(HashMap<String, List<Orders>> remoteStationQueue) {
        this.remoteStationQueue = remoteStationQueue;
    }

    public enum PrinterInterfase {
        USB, BLUETOOTH, TCP
    }
}
