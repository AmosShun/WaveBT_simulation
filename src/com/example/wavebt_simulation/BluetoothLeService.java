package com.example.wavebt_simulation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;


public class BluetoothLeService extends Service{
	private BluetoothAdapter mBluetoothAdapter;
	
    /*
	 * 初始化
	 */
	public boolean initialize(){
		// Initializes Bluetooth adapter.
		final BluetoothManager bluetoothManager =
		        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if(mBluetoothAdapter == null){
			Log.i("BLE","No BluetoothAdapter!!");
			return false;
		}
		else{
			Log.i("BLE","Got BluetoothAdapter!!");
			return true;
		}
	}

    /*
     * 连接设备
     */
    private String mDeviceAdress = "34:B1:F7:D0:63:2E";
    private BluetoothGatt mBluetoothGatt;
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w("BLE", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w("BLE", "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d("BLE", "Trying to create a new connection.");
        return true;
    }
    
 // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {
        	Log.i("BLE","onConnectingStateChange!");
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BLE", "Connected to GATT server.");
                //Discover Services
                Log.i("BLE", "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            	Log.i("BLE", "Disconnected from GATT server.");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	Log.i("BLE","onServicesDiscovered!");

        	new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
		        	byte[] data = new byte[1];
		        	data[0] = 1;
		        	//Turn on MOTION Service
		        	writeCharacteristic("0000ffa0-0000-1000-8000-00805f9b34fb", "0000ffa1-0000-1000-8000-00805f9b34fb", data);
		        	try {
						Thread.sleep(500);	//Delay
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        	//Turn on ECG Service
		        	writeCharacteristic("0000ffff-0000-1000-8000-00805f9b34fb", "0000fffe-0000-1000-8000-00805f9b34fb", data);
				}
        		
        	}).start();

        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
        	Log.i("BLE","onCharacteristicRead!");
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, 
				BluetoothGattCharacteristic characteristic, 
				int status) {
        	Log.i("BLE","onCharacteristicWrite!");
        	String CHAR_UUID = characteristic.getUuid().toString();
        	if(CHAR_UUID.equals("0000fffe-0000-1000-8000-00805f9b34fb")){
            	Log.i("BLE","ECG Service on");
        		setCharacteristicNotification("0000ffff-0000-1000-8000-00805f9b34fb", "0000ffff-0000-1000-8000-00805f9b34fb", true);
        	}
        	else if(CHAR_UUID.equals("0000ffa1-0000-1000-8000-00805f9b34fb")){
        		Log.i("BLE","MOTION Service on");
            	new Thread(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
		            	setCharacteristicNotification("0000ffa0-0000-1000-8000-00805f9b34fb", "0000ffa6-0000-1000-8000-00805f9b34fb", true);
					}
            		
            	}).start();
        	}
        }
    
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	String CHAR_UUID = characteristic.getUuid().toString();
        	//Log.i("BLE","CHAR_UUID:"+CHAR_UUID);
        	byte[] data = characteristic.getValue();
        	if(CHAR_UUID.equals("0000ffff-0000-1000-8000-00805f9b34fb")){
        		//ECG
        		handle_ecg(data);
        	}
        	else if(CHAR_UUID.equals("0000ffa6-0000-1000-8000-00805f9b34fb")){
        		//MOTION
        		handle_motion(data);
        	}
        }
    };

    /*
     * 断开连接
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("BLE", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }
    
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
	
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.i("BLE","Service onDestroy.");
	}

	public void onCreate(){
		Log.i("BLE","Service onCreate.");
	}

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(String serviceUuid, String charUuid,
                                              boolean enabled) {
        BluetoothGattService gattService = null;
        BluetoothGattCharacteristic gattChar = null;
        
    	if (mBluetoothAdapter == null) {
            Log.w("BLE", "Bluetooth connection not initialized");
            return;
        }

        gattService = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
        if (gattService == null) {
        	Log.w("BLE", "Service " + serviceUuid + " not found");
        	return;
        }
        gattChar = gattService.getCharacteristic(UUID.fromString(charUuid));
        if (gattChar == null) {
        	Log.w("BLE", "Char " + charUuid + " not found");
        	return;
        }
               
        BluetoothGattDescriptor descriptor = gattChar.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (enabled == true) {
        	descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        	mBluetoothGatt.writeDescriptor(descriptor);
        
        	mBluetoothGatt.setCharacteristicNotification(gattChar, enabled);
        } else {
        	descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        	mBluetoothGatt.writeDescriptor(descriptor);
        
        	mBluetoothGatt.setCharacteristicNotification(gattChar, enabled);
        }
    }
    
    public void writeCharacteristic(String serviceUuid, String charUuid, byte[] bArray) {
        BluetoothGattService gattService = null;
        BluetoothGattCharacteristic gattChar = null;
        
    	if (mBluetoothAdapter == null) {
            Log.w("BLE", "Bluetooth connection not initialized");
            return;
        }
        gattService = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
        if (gattService == null) {
        	Log.w("BLE", "Service " + serviceUuid + " not found");
        	return;
        }
        gattChar = gattService.getCharacteristic(UUID.fromString(charUuid));
        if (gattChar == null) {
        	Log.w("BLE", "Char " + charUuid + " not found... known char:");
        	return;
        }
        gattChar.setValue(bArray);
        Log.i("BLE", "writeCharacteristic do send write cmd " + Integer.toString(gattChar.getProperties()));
        mBluetoothGatt.writeCharacteristic(gattChar);
    }
    
    private byte[] pkg_ecg = new byte[49];
    private int pkg_count_ecg = 0;
    private int length_ecg = 46;
    private int sample_rate_ecg = 100;
    private int seq_num_ecg = 0;
    private FileLogger exg_logger = new FileLogger("EXG_DATA");
    private void handle_ecg(byte[] data){
    	System.arraycopy(data, 1, pkg_ecg, pkg_count_ecg*10+9, 10);
    	pkg_count_ecg++;
    	if(pkg_count_ecg == 4){
    		pkg_count_ecg = 0;
    		pkg_ecg[0] = 0x02;
    		pkg_ecg[1] = (byte)length_ecg;
    		pkg_ecg[2] = (byte)(length_ecg>>8);
    		pkg_ecg[3] = (byte)seq_num_ecg;
    		pkg_ecg[4] = (byte)(seq_num_ecg>>8);
    		pkg_ecg[5] = 0x00;
    		pkg_ecg[6] = 0x00;
    		pkg_ecg[7] = (byte)sample_rate_ecg;
    		pkg_ecg[8] = (byte)(sample_rate_ecg>>8);
    		seq_num_ecg++;
    		exg_logger.LogData(pkg_ecg);
    	}
    }
    
    private byte[] pkg_motion = new byte[66];
    private int pkg_count_motion = 0;
    private int length_motion = 64;
    private int sample_rate_motion = 20;
    private int seq_num_motion = 0;
    private FileLogger motion_logger = new FileLogger("MOTION_DATA");
    private void handle_motion(byte[] data){
    	System.arraycopy(data, 0, pkg_motion, pkg_count_motion*12+6, 12);
    	pkg_count_motion++;
    	if(pkg_count_motion == 5){
    		pkg_count_motion = 0;
    		pkg_motion[0] = (byte)length_motion;
    		pkg_motion[1] = (byte)(length_motion>>8);
    		pkg_motion[2] = (byte)seq_num_motion;
    		pkg_motion[3] = (byte)(seq_num_motion>>8);
    		pkg_motion[4] = (byte)sample_rate_motion;
    		pkg_motion[5] = (byte)(sample_rate_motion>>8);
    		seq_num_motion++;
    		motion_logger.LogData(pkg_motion);
    	}
    }
    
	class FileLogger {
		String ID;	//区分ECG,MOTION的FileLogger
		private FileOutputStream exg_output = null;
		private FileInputStream exg_input = null;
		private File exg_folder;
		File current_file;
		private String exg_folder_name;
		private int prev_unix_time = 0;
		private int current_unix_time = 0;
		private int next_unix_time = 0;
		private int current_file_size = 0;
		private int current_pkt_count = 0;
		private List<Integer> next_ut_list = new ArrayList<Integer>();
		private int buffer_current_unix_time = 0;
		 
		/*
		 * Initialize folder
		 */
		public FileLogger(String folderName) {
			ID = folderName;
			current_file_size = 0;
			current_pkt_count = 0;
			exg_folder = new File(Environment.getExternalStorageDirectory() + "/" + folderName);
			if (!exg_folder.exists()) {
				exg_folder.mkdir();
			}
			exg_folder_name = new String(Environment.getExternalStorageDirectory() + "/" + folderName + "/");
		}
		
		public void Stop() {
			if (exg_output != null) {
			try {
				exg_output.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
		}
				
		public void ReaderInit() {
			do {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (current_unix_time == 0);
			buffer_current_unix_time = current_unix_time;
			Log.i("EXG Wave", "Buffer Management open initial file: " + Integer.toString(buffer_current_unix_time));
			try {
				exg_input = new FileInputStream(new File(exg_folder_name + Integer.toString(buffer_current_unix_time)));
				byte tmpBuf[] = new byte[12];
				int bytesToRead = 12;
				int readRet = 0;
				
				while (bytesToRead > 0) {
					readRet = exg_input.read(tmpBuf, 0, bytesToRead);
					if (readRet < 0) {
						Log.e("EXG Wave", "Failed to read file");
					}
					bytesToRead -= readRet;
				}
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		
		public int ReaderAvailable() throws IOException {
			return exg_input.available();
		}
		
		public int ReaderRead(byte[] buf, int offset, int length) throws IOException {
			return exg_input.read(buf, offset, length);
		}
		
		public void ReaderSwitchFile() {
			if (next_ut_list.size() > 0) {
				buffer_current_unix_time = next_ut_list.get(0);
				next_ut_list.remove(0);
				try {
					exg_input.close();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				Log.i("EXG Wave", "Buffer switched to file " + Integer.toString(buffer_current_unix_time));
				try {
					exg_input = new FileInputStream(new File(exg_folder_name + Integer.toString(buffer_current_unix_time)));
					byte tmpBuf[] = new byte[12];
					int bytesToRead = 12;
					int readRet = 0;
					
					while (bytesToRead > 0) {
						readRet = exg_input.read(tmpBuf, 0, bytesToRead);
						if (readRet < 0) {
							Log.e("EXG Wave", "Failed to read file");
						}
						bytesToRead -= readRet;
					}
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
		}
		
		private void echoInt (int n) {
			byte[] b = new byte[4];
			for(int i = 0; i < 4; i++){
				b[i] = (byte)(n >> (i * 8)); 
			}
			try {
				exg_output.write(b, 0, 4);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void LogData(byte[] buf) {
			if (current_unix_time == 0 && prev_unix_time == 0) {
				// Create the initial file
				current_unix_time = (int) (System.currentTimeMillis() / 1000L);
				prev_unix_time = current_unix_time;
				
				Log.i("EXG Wave", "Recorder write initial file: " + Integer.toString(current_unix_time));
				current_file = new File(exg_folder_name + Integer.toString(current_unix_time));
				if (!current_file.exists()) {
					try {
						current_file.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					exg_output = new FileOutputStream(current_file);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}					
				current_file_size = 0;
				current_pkt_count = 0;
				echoInt(prev_unix_time);
				echoInt(0);
				echoInt(0);
				//文件创建完成，广播
				Bundle bundle = new Bundle();
				bundle.putInt("START_TIME", current_unix_time);
				Intent intent = new Intent(ID+"_CREATED");
				intent.putExtras(bundle);
				sendBroadcast(intent);
				Log.d("BLE","Broadcast sent!");
			}
			
			// Log the packet
			try {
				exg_output.write(buf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			current_file_size += buf.length;
			current_pkt_count++;
			
			// Create a new file if file size larger than 1K
			if (current_file_size > 10000) {
				try {
					exg_output.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				next_unix_time = (int) (System.currentTimeMillis() / 1000L);
				byte b[] = new byte[8];
				for(int i = 0; i < 4; i++){
					b[i] = (byte)(next_unix_time >> (i * 8)); 
					b[4+i] = (byte)(current_pkt_count >> (i * 8));
				}
				next_ut_list.add(next_unix_time);
				//文件切换，广播
				Intent intent = new Intent(ID+"_FILE_SWITCHED");
				intent.putExtra("NEXT_TIME", next_unix_time);
				sendBroadcast(intent);
				
				RandomAccessFile rf;
				try {
					rf = new RandomAccessFile(exg_folder_name + Integer.toString(current_unix_time), "rw");
					rf.seek(4);
					rf.write(b);
					rf.close();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				prev_unix_time = current_unix_time;
				current_unix_time = next_unix_time;
				next_unix_time = 0;
				Log.i("EXG Wave", "Recorder change file to: " + Integer.toString(current_unix_time));
				current_file = new File(exg_folder_name + Integer.toString(current_unix_time));
				if (!current_file.exists()) {
					try {
						current_file.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					exg_output = new FileOutputStream(current_file);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}					
				current_file_size = 0;
				current_pkt_count = 0;
				echoInt(prev_unix_time);
				echoInt(0);
				echoInt(0);
			}
		}
		
	}
}