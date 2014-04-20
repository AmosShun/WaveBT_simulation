package com.example.wavebt_simulation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

public class TcpClientThread extends Thread{
	public final String ip = "192.168.1.100";
	public final int port = 4000;
	public Socket socket = null;
	public OutputStream output = null;
	public int[] data = null;
	/*
	 * 构造函数，连接服务器
	 */
	public TcpClientThread(){
       	Runnable runnable = new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
	                try {  
	                    System.out.println("Client：Connecting");  
	                    //IP地址和端口号（对应服务端）
	                    socket = new Socket(ip, port);
	                    output = socket.getOutputStream();
	                } catch (UnknownHostException e1) {  
	                    e1.printStackTrace();  
	                } catch (IOException e) {  
	                    e.printStackTrace();  
	                }  
	  
				}
        		
        	};
        	new Thread(runnable).start();
        	start();
	}
	/*
	 * 线程run方法
	 */
	public void run(){
		while(true){
			if(data != null){
				for(int i=0; i<data.length; i++){
					if(data[i] == 0){
						Log.d("datazero","zero");
					}
					byte l = (byte)data[i];
					byte h = (byte)(data[i]>>8);
					try {
						output.write(l);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						output.write(h);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				data = null;
			}
		}
	}
	
	public void send(int[] s){
		data = new int[s.length];
		System.arraycopy(s, 0, data, 0, s.length);
	}
}
