
package org.CrossApp.lib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.CrossApp.lib.CrossAppHelper.CrossAppHelperListener;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import org.CrossApp.lib.AndroidVolumeControl;
import org.CrossApp.lib.AndroidNetWorkManager;

import android.text.ClipboardManager;
@SuppressLint("HandlerLeak")
public abstract class CrossAppActivity extends Activity implements CrossAppHelperListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = CrossAppActivity.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================
    private CrossAppWebViewHelper mWebViewHelper = null;
	private CrossAppGLSurfaceView mGLSurfaceView;
	private CrossAppHandler mHandler;
	public static CrossAppRenderer mCrossAppRenderer;
	AndroidNativeTool actAndroidNativeTool;
	AndroidVolumeControl androidVolumeControl;
	
	private static View rootview;
	public static int currentBattery=0;
	private static CrossAppActivity crossAppActivity;
	private static FrameLayout frame;
	native static void getWifiList(ArrayList<CustomScanResult> s);
	public static List<ScanResult> list;
	public static ScanResult mScanResult;
	public static CustomScanResult cScanResult;
	private static BluetoothAdapter mAdapter = null;
    private final int REQUEST_OPEN_BT_CODE = 1;
    private final int REQUEST_DISCOVERY_BT_CODE = 2;
    native static void returnBlueToothState(int state);
    native static void returnDiscoveryDevice(AndroidBlueTooth sender);
    native static void returnStartedDiscoveryDevice();
    native static void returnFinfishedDiscoveryDevice();
    public static Handler msHandler;
    
    
    //退到后台返回时候用
    public static CrossAppTextField _sTextField = null;
    public static CrossAppTextView _sTextView = null;
    public static void setSingleTextField(CrossAppTextField text) {
		_sTextField = text;
	}
	public static void setSingleTextView(CrossAppTextView text) {
		_sTextView = text;
	}
	
	
	public static CrossAppActivity getContext()
	{
		return CrossAppActivity.crossAppActivity;
	}
	
	public static FrameLayout getFrameLayout()
	{
		return CrossAppActivity.frame;
	}
	
	
	
	public static Handler mLightHandler;
	// ===========================================================
	// Constructors
	// ===========================================================

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		crossAppActivity = this;

    	this.mHandler = new CrossAppHandler(this);
    	actAndroidNativeTool = new AndroidNativeTool(this);
    	AndroidVolumeControl.setContext(crossAppActivity);
    	AndroidPersonList.Init(this);

    	this.init();
    	rootview = this.getWindow().getDecorView();
		CrossAppHelper.init(this, this);

		exeHandler();
		AndroidNetWorkManager.setContext(this);
		
		 if(savedInstanceState == null)
		 {
			mWebViewHelper = new CrossAppWebViewHelper(frame);
			CrossAppTextField.initWithHandler();
			CrossAppTextView.initWithHandler();
		 }
		 else if (savedInstanceState != null && savedInstanceState.containsKey("WEBVIEW"))
		 {
			 mWebViewHelper = new CrossAppWebViewHelper(frame);
			 String[] strs = savedInstanceState.getStringArray("WEBVIEW");
			 mWebViewHelper.setAllWebviews(strs);
			 savedInstanceState.clear();
			 CrossAppTextField.reload();
			 CrossAppTextView.reload();
		 }
		 IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

	     BatteryReceiver batteryReceiver = new BatteryReceiver();

	     registerReceiver(batteryReceiver, intentFilter);
	     
	}

	@Override
	protected void onResume() 
	{
		super.onResume();
		if (_sTextField != null) 
		{
			_sTextField.resume();
		}
		
		if (_sTextView != null)
		{
			_sTextView.resume();
		}

		CrossAppHelper.onResume();
		this.mGLSurfaceView.onResume();
		if (AndroidGPS.locationManager!=null)
		{
			AndroidGPS.locationManager.requestLocationUpdates(AndroidGPS.locationManager.GPS_PROVIDER, 1000, 1, AndroidGPS.locationListener);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		CrossAppHelper.onPause();
		this.mGLSurfaceView.onPause();
		if (AndroidGPS.locationManager!=null)
		{
			AndroidGPS.locationManager.removeUpdates(AndroidGPS.locationListener);
		}
	}
//	private static native void reshare(final int pWidth, final int pHeight);
//	public void onConfigurationChanged(Configuration newConfiguration)
//	{
//		super.onConfigurationChanged(newConfiguration);
//		
//		final DisplayMetrics displayMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        final int width = displayMetrics.widthPixels;
//        final int height = displayMetrics.heightPixels;
//        reshare(width, height);
//	}
	
//	@Override
//	protected void onDestroy() 
//	{
//		super.onDestroy();
//		unregisterReceiver(BluetoothReciever) ; 
//		unregisterReceiver(BTDiscoveryReceiver) ; 
//	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		
		outState.putStringArray("WEBVIEW", mWebViewHelper.getAllWebviews());
		super.onSaveInstanceState(outState);
	}


	
	class BatteryReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){

                int level = intent.getIntExtra("level", 0);

                int scale = intent.getIntExtra("scale", 100);

                currentBattery =level*100/ scale;
            }
        }
    }


	public void initBlueTooth()
	{
		mAdapter = BluetoothAdapter.getDefaultAdapter();

		IntentFilter bluetoothFilter = new IntentFilter();
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(BluetoothReciever, bluetoothFilter);


        IntentFilter btDiscoveryFilter = new IntentFilter();
        btDiscoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btDiscoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btDiscoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
        btDiscoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(BTDiscoveryReceiver, btDiscoveryFilter);

        Set<BluetoothDevice> bts = mAdapter.getBondedDevices();
        Iterator<BluetoothDevice> iterator  = bts.iterator();
        while(iterator.hasNext())
        {
            BluetoothDevice bd = iterator.next() ;
            Log.i(TAG , " Name : " + bd.getName() + " Address : "+ bd.getAddress() ); ;
            Log.i(TAG, "Device class" + bd.getBluetoothClass());
        }

        BluetoothDevice findDevice =  mAdapter.getRemoteDevice("00:11:22:33:AA:BB");

        Log.i(TAG , "findDevice Name : " + findDevice.getName() + "  findDevice Address : "+ findDevice.getAddress() ); ;
        Log.i(TAG , "findDevice class" + findDevice.getBluetoothClass());
	}

	public static void getWifiList()
	{
		AndroidNetWorkManager.setContext(crossAppActivity);
		AndroidNetWorkManager.startScan();
		list = AndroidNetWorkManager.getWifiList();
		ArrayList<CustomScanResult> cList = new ArrayList<CustomScanResult>();
		if(list!=null){
            for(int i=0;i<list.size();i++){
                //锟矫碉拷扫锟斤拷锟斤拷
                mScanResult=list.get(i);
                cScanResult = new CustomScanResult(mScanResult.SSID, mScanResult.BSSID, mScanResult.level);
                if (cScanResult!=null) {
                	cList.add(cScanResult);
				}

            }
            if (cList!=null)
            {
            	getWifiList(cList);
            }
		}
	}

    public void setPasteBoardStr(String sender)
    {
        Message msg=new Message();
        msg.obj = sender;
        msg.what = 0;
        msHandler.sendMessage(msg);
    }

    public String getPasteBoardStr()
    {
		Callable<String> callable = new Callable<String>() 
		{
            @Override
            public String call() throws Exception 
            {
            	ClipboardManager clipboard =  (ClipboardManager)crossAppActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.getText();
                return clipboard.getText().toString();
            }
        };
        try {
            return CrossAppWebViewHelper.callInMainThread(callable);
        } catch (Exception e) {
        }
		return "";
    }

	public BroadcastReceiver BluetoothReciever = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // TODO Auto-generated method stub
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction()))
            {
                Log.v(TAG, "### Bluetooth State has changed ##");

                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.STATE_OFF);

                printBTState(btState);
            }
            else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction()))
            {
                Log.v(TAG, "### ACTION_SCAN_MODE_CHANGED##");
                int cur_mode_state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
                int previous_mode_state = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);

                Log.v(TAG, "### cur_mode_state ##" + cur_mode_state + " ~~ previous_mode_state" + previous_mode_state);

            }
        }

    };

    public void setBlueToothActionType(int type)
    {
    	boolean wasBtOpened = mAdapter.isEnabled();
    	switch (type) {
		case 0:
			boolean result = mAdapter.enable();
			if(result)
				returnBlueToothState(0);
			else if(wasBtOpened)
				returnBlueToothState(1);
			else
			{
				returnBlueToothState(2);
			}
			break;

		case 1:
			boolean result1 = mAdapter.disable();
			if(result1)
				returnBlueToothState(3);
			else if(!wasBtOpened)
				returnBlueToothState(4);
			else
				returnBlueToothState(5);
			break;
		case 2:
			if (!wasBtOpened)
			{
				Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(intent, REQUEST_OPEN_BT_CODE);
			}
			else
				returnBlueToothState(1);
			break;
		case 3:
			if (!mAdapter.isDiscovering()){
                mAdapter.startDiscovery();
            }
            else
			break;
		case 4:
			if (mAdapter.isDiscovering()){
                mAdapter.cancelDiscovery();
            }
			break;
		case 5:
            Intent discoveryintent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryintent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(discoveryintent, REQUEST_DISCOVERY_BT_CODE);
			break;
		default:
			break;
		}
    }
    
    public BroadcastReceiver BTDiscoveryReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction()))
            {
                returnStartedDiscoveryDevice();
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction()))
            {
                returnFinfishedDiscoveryDevice();
            }
            else if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction()))
            {
                BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(btDevice != null)
                {
                	AndroidBlueTooth mAndroidBlueTooth = new AndroidBlueTooth(btDevice.getAddress(),btDevice.getName());
                	returnDiscoveryDevice(mAndroidBlueTooth);
                }
            }
            else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction()))
            {

                int cur_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                int previous_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
            }
        }

    };

    private void printBTState(int btState)
    {
        switch (btState)
        {
            case BluetoothAdapter.STATE_OFF:
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                break;
            case BluetoothAdapter.STATE_ON:
                break;
            default:
                break;
        }
    }

    private void toast(String str)
    {
    	System.out.println(str);

        //Toast.makeText(CrossAppActivity.this, str, Toast.LENGTH_SHORT).show();
    }

	public static CustomScanResult getWifiConnectionInfo()
	{
		WifiInfo mWifiInfo = AndroidNetWorkManager.getWifiConnectionInfo();
		CustomScanResult connectionInfo = null;
		if(mWifiInfo!=null)
		{
			connectionInfo = new CustomScanResult(mWifiInfo.getSSID(), mWifiInfo.getBSSID(), 0);
		}
		return connectionInfo;
	}

	public static int getBatteryLevel()
	{
		return currentBattery;
	}

	 public void onActivityResult(int requestCode, int resultCode, Intent intent)
	 {
		 actAndroidNativeTool.onActivityResult(requestCode, resultCode, intent);

	 }
	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	public static void setScreenBrightness(int value) {
		try {
			// System.putInt(s_pContext.getContentResolver(),android.provider.Settings.System.SCREEN_BRIGHTNESS,value);
			WindowManager.LayoutParams lp = crossAppActivity.getWindow().getAttributes();
			lp.screenBrightness = (value<=0?1:value) / 255f;
			crossAppActivity.getWindow().setAttributes(lp);
		} catch (Exception e) {
			Toast.makeText(crossAppActivity,"error",Toast.LENGTH_SHORT).show();
		}
	}
	private void exeHandler(){
		if(mLightHandler ==null){
			mLightHandler = new Handler(){

				 @Override
				public void handleMessage(Message msg) {
					int value = msg.what;
					 WindowManager.LayoutParams lp = crossAppActivity.getWindow().getAttributes();
					 lp.screenBrightness = value/255.0f;
					 crossAppActivity.getWindow().setAttributes(lp);
				}
			 };
		 }
        if(msHandler ==null){
            msHandler = new Handler(){

                @Override
                public void handleMessage(Message msg) {
                    String value = (String)msg.obj;
                    int what = msg.what;
                    ClipboardManager cmb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if(what == 0)
                    {

                        cmb.setText(value);
                    }
                }
            };
        }
	}
	public static void startGps() {
		AndroidGPS.Init(crossAppActivity);

	}
	
	@Override
	public void showDialog(final String pTitle, final String pMessage) {
		Message msg = new Message();
		msg.what = CrossAppHandler.HANDLER_SHOW_DIALOG;
		msg.obj = new CrossAppHandler.DialogMessage(pTitle, pMessage);
		this.mHandler.sendMessage(msg);
	}

	@Override
	public void showEditTextDialog(final String pTitle, final String pContent, final int pInputMode, final int pInputFlag, final int pReturnType, final int pMaxLength) {
		Message msg = new Message();
		msg.what = CrossAppHandler.HANDLER_SHOW_EDITBOX_DIALOG;
		msg.obj = new CrossAppHandler.EditBoxMessage(pTitle, pContent, pInputMode, pInputFlag, pReturnType, pMaxLength);
		this.mHandler.sendMessage(msg);
	}

	@Override
	public void runOnGLThread(final Runnable pRunnable) {
		this.mGLSurfaceView.queueEvent(pRunnable);
	}

	// ===========================================================
	// Methods
	// ===========================================================
	public void init() {

        ViewGroup.LayoutParams framelayout_params =
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                       ViewGroup.LayoutParams.FILL_PARENT);
        FrameLayout framelayout = new FrameLayout(this);
        framelayout.setLayoutParams(framelayout_params);
        frame = framelayout;

        this.mGLSurfaceView = this.onCreateView();

        framelayout.addView(this.mGLSurfaceView);

        if (isAndroidEmulator())
        {
           this.mGLSurfaceView.setEGLConfigChooser(8 , 8, 8, 8, 16, 0);
        }
        
        mCrossAppRenderer = new CrossAppRenderer();
        this.mGLSurfaceView.setCrossAppRenderer(mCrossAppRenderer);

		setContentView(framelayout);
	}
	
	public static int dip2px(Context context, float dpValue) {
	     final float scale = context.getResources().getDisplayMetrics().density;
	     return (int) (dpValue * scale + 0.5f);
	}

    public CrossAppGLSurfaceView onCreateView() {
    	return new CrossAppGLSurfaceView(this);
    }

   private final static boolean isAndroidEmulator() {
      String model = Build.MODEL;
      String product = Build.PRODUCT;
      boolean isEmulator = false;
      if (product != null) {
         isEmulator = product.equals("sdk") || product.contains("_sdk") || product.contains("sdk_");
      }
      Log.d(TAG, "isEmulator=" + isEmulator);
      return isEmulator;
   }


	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
