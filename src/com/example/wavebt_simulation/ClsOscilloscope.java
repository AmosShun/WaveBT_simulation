package com.example.wavebt_simulation;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Set;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;

import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class ClsOscilloscope {

	class FileLogger {
		private FileOutputStream exg_output = null;
		private FileInputStream exg_input = null;
		private File exg_folder;
		File current_file;
		private String exg_folder_name;
		private int current_unix_time = 0;
		private List<Integer> next_ut_list = new ArrayList<Integer>();
		private int buffer_current_unix_time = 0;
		private static final int START_UNIX_TIME = 1395107776;
		 
		/*
		 * Initialize folder
		 */
		public FileLogger(String folderName) {
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
			current_unix_time = START_UNIX_TIME;
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
	};
	
	public int skip_buf_num = 1;
	public int skip_buf_num_delta = 1;
	
	public int start_y = 0;
	public int start_y_delta = 20;
	
	public int total_y = 1024;
	public int total_y_delta = 2;
	public int alive = 0;
	public int frames_per_sec = 5;
	public int total_write_count = 0;
	public int total_read_count = 0;
    public OutputStream outStream = null;
    private FileLogger exgLogger = new FileLogger("EXG_DATA");
    private FileLogger motionLogger = new FileLogger("MOTION_DATA");

    private Derivative derivative = new Derivative();
    private Filter filter = new Filter();
    /*
    int send_over_socket = 0;
    Socket client = null;
    DataOutputStream dout;
	
    public void start_socket(String sock_info) {
    	String [] temp = null;
    	temp = sock_info.split(":");
    	SocketAddress addr = new InetSocketAddress(temp[0], Integer.parseInt(temp[1]));
    	
    	try {
    		Log.i("EXG Wave", "begin connect to: "+temp[0]+":"+temp[1]);
			client = new Socket();
			client.connect(addr, 2000);
			Log.i("EXG Wave", "Socket connected");
			dout = new DataOutputStream(client.getOutputStream());
			send_over_socket = 1;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    */
    
	/**
	 * 初始化
	 */
	public void initOscilloscope() {
	}

	public void motion_toggle() {
		byte[] msg = new byte[6];
		/* 0x03 means motion rate toggle */
		msg[0] = 'C';
		msg[1] = 'M';
		msg[2] = 'D';		
		msg[3] = 0x03;
		msg[4] = 0x00;
		msg[5] = 0x00;
		try {
			outStream.write(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 	
	
	public void set_trigger() {
		byte[] msg = new byte[6];
		/* 0x04 means trigger */
		msg[0] = 'C';
		msg[1] = 'M';
		msg[2] = 'D';		
		msg[3] = 0x04;
		msg[4] = 0x00;
		msg[5] = 0x00;
		try {
			outStream.write(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
	
	public void set_sample_rate(int rate) {
		byte[] msg = new byte[6];
		/* 0x05 means sample rate */
		msg[0] = 'C';
		msg[1] = 'M';
		msg[2] = 'D';		
		msg[3] = 0x05;
		msg[4] = (byte)(rate & 0xff);
		msg[5] = (byte)((rate >> 8) & 0xff);
		try {
			outStream.write(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void adjust_speed(int delta) {
		if (delta == 1) {
			if (skip_buf_num < 5) {
				skip_buf_num += skip_buf_num_delta;
			}
		} else {
			if (skip_buf_num > 1) {
				skip_buf_num -= skip_buf_num_delta;
			}
		}
	}
	
	public void adjust_start_y(int delta) {
		if (delta == 1) {
			if (start_y < (1023-start_y_delta)) {
				start_y += start_y_delta;
			}
		} else {
			if (start_y > (start_y_delta)) {
				start_y -= start_y_delta;
			}
		}
	}
	
	public void adjust_total_y(int delta) {
		if (delta == 1) {
			total_y /= total_y_delta;
		} else {
			if (total_y > total_y_delta) {
				total_y *= total_y_delta;
			}
		}
	}
	
	/**
	 * 开始
	 * 
	 * @param recBufSize
	 *            AudioRecord的MinBufferSize
	 */
	public void Start(SurfaceView sfv, Paint mPaint) {
		//usb_recv_thread = new RecordThread();
		//usb_recv_thread.start();// 开始录制线程
		//new BufferListThread().start();
		new DrawThread(sfv, mPaint).start();// 开始绘制线程
		//Register with server
		List<NameValuePair> login_parm = new ArrayList<NameValuePair>();
		login_parm.add(new BasicNameValuePair("user", "user"));
		login_parm.add(new BasicNameValuePair("password", "USER"));
		/*
		try {
			String ret_msg = HttpUtils.post("http://dan.dminorstudio.com/mobile/user/login", 
					"{\"username\":\"user\", \"password\":\"USER\"}");
			Log.i("EXG_Wave", "login result: "+ret_msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		new PostData().start();
		*/
	}

	/**
	 * 停止
	 */
	public void Stop() {
		exgLogger.Stop();
		motionLogger.Stop();

		/*
		try {
			String ret_msg = HttpUtils.post("http://dan.dminorstudio.com/mobile/user/logout", "");
			Log.i("EXG_Wave", "logout result: "+ret_msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}

	class BufferManagement {
		
		/* paint_list to keep track of historical buffers */
		private ArrayList<int[]> paint_list = new ArrayList<int[]>();
		
		/* number of points per buffer */
		private int per_buf_points = 0;
		
		/* 
		 * input buffer and its related state machine,
		 * to keep track of data fragments
		 */
		private byte[] head_buf = new byte[3];
		private int head_idx = 0;
		private byte[] data_buf;
		private int data_idx = 0;
		private int[] data_pool;
		
		private int screen_width = 0;
		/*
		 * FIXME: assume sample_per_sec is larger than screen_width!!!
		 */
		private int sample_per_sec = 0;		/* number of samples/sec */
		private int prev_sample_per_sec = 0;
		private int down_sample_rate = 0;	/* number of samples/shown sample */
		private int sub_sample_counter = 0;
		private int remain_points = 0;
		private int read_head_stage = 0;
		private short msg_length = 0;		
		/* the single temporary buffer */
		int[] tmp_buf = null;
		int tmp_i = 0;
		int tmp_buf_debt = 0;
		
		public BufferManagement(int width) {
			// We choose the latest header among all headers in EXG_DATA folder
			// First wait until EXG_DATA folder show up
			exgLogger.ReaderInit();
			screen_width = width;
		}
		
		public void fill_buffer(int buffer[]) {
			int i = 0;
			int paint_i = 0;
			int paint_buf_i = 0;
			int[] paint_buf = null;
			
			for (i = 0; i < buffer.length; i++) {
				buffer[i] = 0;
			}
			if (paint_list.isEmpty()) {
				//Log.i("EXG Wave", "fill buffer exit because NULL");
				return;
			}
			i = 0;
			paint_buf = paint_list.get(0);
			while (i < buffer.length) {
				if (paint_buf_i < per_buf_points) {
					buffer[i] = paint_buf[paint_buf_i];
					i++;
					paint_buf_i++;
				} else {
					paint_buf_i = 0;
					paint_i++;
					if (paint_list.size() > paint_i) {
						paint_buf = paint_list.get(paint_i);
					} else {
						return;
					}
				}
			}
		}
		
		public int get_show_buffer(int buffer[]) {	
			int remove_threshold = 0;
			
			//Log.i("EXG Wave", "get_show_buffer");
			if (per_buf_points == 0) {
				remove_threshold = 20;	//一个画板最多画20个数据包
			} else {
				remove_threshold = (screen_width/per_buf_points);	//一个画板画多少个数据包
			}
			if (paint_list.size() > remove_threshold) {
				//Log.i("EXG Wave", "remove head buffer");
				paint_list.remove(0);	//paint_list超出屏幕宽度，左移
			}
			if (remain_points == 0) {
				if (pull_more_data() != 0) {
					fill_buffer(buffer);
					return -1;
				}
			}
			tmp_buf_debt++;
			while (tmp_buf_debt > 0) {
				while (tmp_i < per_buf_points) {
					/* get one sample point from data_pool */
					while (sub_sample_counter != (down_sample_rate-1)) {
						if (remain_points > 0) {
							/* skip over data_idx */
							//Log.i("EXG Wave", "skip one");
							data_idx++;
							remain_points--;
							sub_sample_counter++;
							total_read_count++;
						} else {
							if (pull_more_data() != 0) {
								fill_buffer(buffer);
								return -1;
							}
						}
					}
					if (remain_points > 0) {
						//Log.i("EXG Wave", "sample: "+data_pool[data_idx]);
						tmp_buf[tmp_i] = data_pool[data_idx];
						tmp_i++;
						sub_sample_counter = 0;
						data_idx++;
						remain_points--;
						total_read_count++;
					} else {
						if (pull_more_data() != 0) {
							fill_buffer(buffer);
							return -1;
						}
					}
				}
				paint_list.add(paint_list.size(), tmp_buf);
				tmp_buf_debt--;
				tmp_i = 0;
				tmp_buf = new int[per_buf_points];
			}
			fill_buffer(buffer);
			return 0;
		}

		private int pull_more_data() {
			int rc = 0;
			
			//Log.i("EXG Wave", "pull_more_data");
			if (remain_points != 0) {
				Log.e("EXG Wave", "should not be called when still has data");
				return -1;
			}
			if (read_head_stage == 0) {
				//Log.i("EXG Wave", "read_head_stage == 0");
				head_idx = 0;
				data_idx = 0;
				try {
					if (exgLogger.ReaderAvailable() < 3) {
						//Log.i("EXG Wave", "header not available");
						//At this point, need to search for alternative file
						exgLogger.ReaderSwitchFile();
						return -1;
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				while (head_idx < 3) {
					try {
						rc = exgLogger.ReaderRead(head_buf, head_idx, (3-head_idx));
						head_idx += rc;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (head_buf[0] != 0x02) {
					Log.e("EXG Wave", "Error in exg.data: header type ne 0x02");
				}
				msg_length = (short) ((head_buf[1] & 0xff) | ((head_buf[2] & 0xff) << 8));
				data_buf = new byte[msg_length];
				read_head_stage = 1;
			}
			
			/* wait until there is at least msg_length data to read */
			try {
				if (exgLogger.ReaderAvailable() < msg_length) {
					//Log.i("EXG Wave", "msg_length not available");
					return -1;
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			while (data_idx < msg_length) {
				try {
					rc = exgLogger.ReaderRead(data_buf, data_idx, (msg_length-data_idx));
					data_idx += rc;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			/* Now we have data, add them to paint_list */
			//short channel_num;
			//short seq_num;
			//seq_num = (short) ((data_buf[0] & 0xff) | ((data_buf[1] & 0xff) << 8));
			//channel_num = (short) ((data_buf[2] & 0xff) | ((data_buf[3] & 0xff) << 8));
			sample_per_sec = (short) ((data_buf[4] & 0xff) | ((data_buf[5] & 0xff) << 8));
			remain_points = (msg_length-6)/2;
			read_head_stage = 0;
			data_pool = new int[remain_points];
			for (int i = 0; i < remain_points; i++) {
				data_pool[i] = (int) ((data_buf[6+i*2] & 0xff) | ((data_buf[6+i*2+1] & 0xff) << 8));
			}
			
			//差分法寻找QRS波群
			derivative.process(data_pool);
			//高通滤波消除基线漂移
			data_pool = filter.highpass(data_pool);
			
			
			if (prev_sample_per_sec != sample_per_sec) {
				/* 
				 * sample/sec changed, this could be the init case, 
				 * or user changed sample rate
				 */
				prev_sample_per_sec = sample_per_sec;
				paint_list.clear();
				sub_sample_counter = 0;
				tmp_i = 0;
				tmp_buf_debt = 0;
				if (sample_per_sec >= screen_width) {
					/* down sampling */
					down_sample_rate = sample_per_sec/screen_width;
					per_buf_points = (sample_per_sec/down_sample_rate)/frames_per_sec;
					
				} else {
					/* full sample */
					down_sample_rate = 1;
					per_buf_points = sample_per_sec/frames_per_sec;
					if (per_buf_points < 1) {
						per_buf_points = 1;
					}
				}
				tmp_buf = new int[per_buf_points];
				data_idx = 0;
				return -1;
			} 
			data_idx = 0;	
			return 0;
		}
	}
	
	/**
	 * 负责绘制inBuf中的数据
	 */
	class DrawThread extends Thread {
		private SurfaceView sfv;// 画板
		private Paint mPaint;// 画笔
		private int max_y = 0;
		private int max_x = 0;
		Timer timer = new Timer();
		private int [] paint_buffer = null;
		private BufferManagement buf_mgmt = null;
		private int rc;
		private int debug_i = 0;
		
	    TimerTask task = new TimerTask(){
	    	/* 
	    	 * Each run period shows a frame of data
	    	 */
	        public void run() {
	        	debug_i++;
	        	if (debug_i == frames_per_sec) {
	        		debug_i = 0;
	        		//Log.i("EXG Wave", "total_write_count: "+total_write_count+" total_read_count: "+total_read_count);
	        	}
	        	/* Initialize everything */
	        	if (max_y == 0) {
	        		max_y = sfv.getHeight();
	        		max_x = sfv.getWidth();
	        		paint_buffer = new int[max_x];
	        		buf_mgmt = new BufferManagement(max_x);
	        	}
	        	rc = buf_mgmt.get_show_buffer(paint_buffer);
	        	if (rc == -1) {
	        		//Log.i("EXG Wave", "get_show_buffer returned error");
	        	}
	        	//画在画布正中间
	        	
	        	SimpleDraw(paint_buffer);
	        }  	          
	    }; 

		public DrawThread(SurfaceView sfv, Paint mPaint) {
			this.sfv = sfv;
			this.mPaint = mPaint;
			timer.schedule(task, 1000, 1000/frames_per_sec); 
		}

		public void run() {			
			/* Everything runs in timer handler */
		}

		/**
		 * 绘制指定区域
		 * 
		 * @param start
		 *            X轴开始的位置(全屏)
		 * @param buffer
		 *            缓冲区
		 * @param rate
		 *            Y轴数据缩小的比例
		 * @param baseLine
		 *            Y轴基线
		 */
		void SimpleDraw(int[] buffer) {
			Canvas canvas = sfv.getHolder().lockCanvas(
					new Rect(0, 0, sfv.getWidth(), sfv.getHeight()));// 关键:获取画布
			canvas.drawColor(Color.BLACK);// 清除背景
			//Log.i("EXG Wave", "create "+sfv.getWidth()+" X "+sfv.getHeight());
			int y;
			int oldX = 0, oldY = 0;
			for (int i = 0; i < buffer.length; i++) {// 有多少画多少
				int x = i;
				if ((buffer[i]/* + total_y/2*/) < start_y) {
					buffer[i] = start_y;
				} else if (buffer[i] > (start_y+total_y)) {
					buffer[i] = (start_y+total_y);
				}
				y = ((buffer[i] - start_y) * max_y) / total_y;
				y = max_y - y /*- max_y/2*/;
				if (oldX == 0) {
					oldY = y;
				}
				canvas.drawLine(oldX, oldY, x, y, mPaint);
				oldX = x;
				oldY = y;
			}
			sfv.getHolder().unlockCanvasAndPost(canvas);// 解锁画布，提交画好的图像z
		}
	}
}
