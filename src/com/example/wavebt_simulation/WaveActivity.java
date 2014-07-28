package com.example.wavebt_simulation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class WaveActivity extends Activity {


	private Button btnUp;
	private Button btnDown;
	private Button btnX;
	private Button btnx;
	private Button btnTG;
	private Button btnSampleRate;
	//private Button btnMotion;
	private EditText editSR;
	//private EditText recvMsg;
	private SurfaceView sfv;	//ECG画布
	private SurfaceView sfv_acc;	//Acc画布
	//private int refresh_stage = 0;
	private TextView RR_interval;
	private TextView heart_rate;
	private TextView QRS_interval;
	private TextView arrhythmia;
	private int prev_alive = 0;
	public static Handler handler;

	public ClsOscilloscope clsOscilloscope = new ClsOscilloscope();
	
	Paint mPaint;
	

	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// 按键
		btnUp = (Button) this.findViewById(R.id.btnUp);
		btnUp.setOnClickListener(new ClickEvent());
		btnDown = (Button) this.findViewById(R.id.btnDown);
		btnDown.setOnClickListener(new ClickEvent());
		btnX = (Button) this.findViewById(R.id.btnX);
		btnX.setOnClickListener(new ClickEvent());
		btnx = (Button) this.findViewById(R.id.btnx);
		btnx.setOnClickListener(new ClickEvent());
		btnTG = (Button) this.findViewById(R.id.btnTG);
		btnTG.setOnClickListener(new ClickEvent());
		//btnSampleRate = (Button) this.findViewById(R.id.btnSampleRate);
		//btnSampleRate.setOnClickListener(new ClickEvent());
		//btnMotion = (Button) this.findViewById(R.id.btnMotion);
		//btnMotion.setOnClickListener(new ClickEvent());
		//editSR = (EditText) this.findViewById(R.id.editSR);
		//recvMsg = (EditText) this.findViewById(R.id.recvMsg);
		
		
		//参数显示
		RR_interval = (TextView)this.findViewById(R.id.rR);
		RR_interval.setText("RR间期：初始化中...");
		heart_rate = (TextView)this.findViewById(R.id.heartRate);
		heart_rate.setText("心率：初始化中...");
		QRS_interval = (TextView)this.findViewById(R.id.qRs);
		QRS_interval.setText("QRS间期：初始化中...");
		arrhythmia = (TextView)this.findViewById(R.id.arrhythmia);
		arrhythmia.setText("异常心率：初始化中...");
		
		// 画板和画笔
		sfv = (SurfaceView) this.findViewById(R.id.SurfaceView01);
		//sfv.setOnTouchListener(new TouchEvent());
		sfv.setOnClickListener(new ClickEvent());
		sfv_acc = (SurfaceView) this.findViewById(R.id.SurfaceView02);
		mPaint = new Paint();
		mPaint.setColor(Color.GREEN);// 画笔为绿色
		mPaint.setStrokeWidth(1);// 设置画笔粗细
		// 示波器类库
		clsOscilloscope.initOscilloscope();
		
/***********************************绑定Service***************************************/
		
	    //动态注册广播接收器  
        msgReceiver = new MsgReceiver();  
        IntentFilter intentFilter = new IntentFilter();  
        intentFilter.addAction("EXG_DATA_CREATED"); 
        intentFilter.addAction("EXG_DATA_FILE_SWITCHED");
        registerReceiver(msgReceiver, intentFilter);  
        
	    Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
	    bindService(gattServiceIntent, mServiceConnection, this.BIND_AUTO_CREATE);
/*************************************************************************************/

		/* start ASAP */		
		//clsOscilloscope.Start(sfv,sfv_acc, mPaint);
		
		handler = new Handler() {
			@Override
            public void handleMessage(Message msg) {
				if (msg.getData().containsKey("msg")) {
					Log.i("EXG Wave", "handleMessage: " + msg.getData().getString("msg"));
					//recvMsg.setText(msg.getData().getString("msg"));
				} else if (msg.getData().containsKey("seq")) {
					refresh_title(msg.getData().getString("seq"));
				}
				if(msg.what == 1){
					RR_interval.setText("RR间期："+msg.arg1+" ms");
				}
				if(msg.what == 2){
					QRS_interval.setText("QRS间期："+msg.arg1+" ms");
				}
				if(msg.what == 3){
					heart_rate.setText("心率："+msg.arg1+" bpm");
				}
				if(msg.what == 4){
					arrhythmia.setText("心律失常："+msg.arg1);
				}
				if(msg.what == 5){
					clsOscilloscope.save_arrhythmia();
				}
			}
		};
	}
	
	/*菜单*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	/*菜单按钮*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
		case R.id.save:
			SaveDialog();
			break;
		case R.id.load:
			LoadDialog();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// Method 
	static public void updateMessage (String msg) {
		
	}
	@Override
	protected void onDestroy() {
		clsOscilloscope.Stop();
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	public void refresh_title(String seq) {
		setTitle(seq);
	}
	
	/**
	 * 按键事件处理
	 */
	class ClickEvent implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (v == btnUp) {
				clsOscilloscope.adjust_start_y(-1);
			} else if (v == btnDown) {
				clsOscilloscope.adjust_start_y(1);
			} else if (v == btnX) {
				clsOscilloscope.adjust_total_y(1);
			} else if (v == btnx) {
				clsOscilloscope.adjust_total_y(-1);
			} else if (v == btnTG) {
				//clsOscilloscope.set_trigger();
			} else if (v == btnSampleRate) {
				if (editSR.getText().toString().matches("[0-9]+.[0-9]+.[0-9]+.[0-9]+:[0-9]+")) {
					Log.i("EXG Wave", "Got socket request to address: "+editSR.getText().toString());
					//clsOscilloscope.start_socket(editSR.getText().toString());
				} else {
					int val = Integer.parseInt(editSR.getText().toString());
				//	clsOscilloscope.set_sample_rate(val);
				}
			//} else if (v == btnMotion) {
			//	clsOscilloscope.motion_toggle();
			}else if (v == sfv){
				/*点击SurfaceView,冻结屏幕*/
				if(clsOscilloscope.PAUSE == false){
					clsOscilloscope.PAUSE = true;
				}
				else
					clsOscilloscope.PAUSE = false;
			}
		}
	}

	/**
	 * 触摸屏动态设置波形图基线
	 */
	class TouchEvent implements OnTouchListener {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (prev_alive != clsOscilloscope.alive) {
				prev_alive = clsOscilloscope.alive;
			}
			return true;
		}
	}
	
	/*
	 * 保存波形的对话框
	 */
	public void SaveDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(WaveActivity.this);
		//设置对话框布局文件
		View view = View.inflate(this,R.layout.save_dialog,null);
		builder.setView(view);
		final EditText edit_text = (EditText)view.findViewById(R.id.filename);
		//按键
        builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String file_name = edit_text.getText().toString();
                clsOscilloscope.save(clsOscilloscope.paint_buffer_pause,file_name);
            }
        });

		

		builder.create().show();
	}
	/*
	 * 读取波形的对话框
	 */
	public void LoadDialog(){
		/*获得文件列表*/
		File root = new File(Environment.getExternalStorageDirectory()+"/EXG_DATA/save");
		List<String> items = new ArrayList<String>();
		for(File file : root.listFiles()){
			items.add(file.getName());
		}	//得到文件名列表
		final String[] files = new String[items.size()];	//转成String数组
		items.toArray(files);
		/*设置对话框*/
		AlertDialog.Builder builder = new AlertDialog.Builder(WaveActivity.this);
		builder.setTitle("选择波形文件");
		builder.setItems(files, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				clsOscilloscope.load(files[which]);
			}
			
		});
		builder.create().show();
	}
	
	/*************************************BluetoothLeService********************************/
	private BluetoothLeService mBluetoothLeService;	//蓝牙Service
	private MsgReceiver msgReceiver;
	private String mDeviceAddress = "34:B1:F7:D0:63:2E";
	// Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	Log.i("BLE","onServiceConnected!");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.i("BLE", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
/*            mBluetoothLeService = null;*/
        }
    };
    
    //广播接收器，接收EXG,MOTION文件创建消息
    private class MsgReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.d("BLE","Got broadcast!");
			String action = intent.getAction();
			if(action.equals("EXG_DATA_CREATED")){
				Bundle bundle = intent.getExtras();
				int START_TIME_ECG = bundle.getInt("START_TIME");
				Log.d("BLE",Integer.toString(START_TIME_ECG));

				clsOscilloscope.Start(sfv, sfv_acc, mPaint, START_TIME_ECG);
			}
			else if(action.equals("EXG_DATA_FILE_SWITCHED")){
				int next_unix_time = intent.getIntExtra("NEXT_TIME", 0);
				if(next_unix_time!=0){
					clsOscilloscope.switchfile_ecg(next_unix_time);
				}
			}


		}
    	
    }
/*****************************************************************************************/

}
