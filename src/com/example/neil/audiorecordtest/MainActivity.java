package com.example.neil.audiorecordtest;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Process;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

	// 采集音频的设备
	private static final int AUDIO_RESOURCE = MediaRecorder.AudioSource.REMOTE_SUBMIX;
	// 音频的采集率
	private final static int AUDIO_SAMPLE_RATE = 44100;
	// 音频通道类型 立体声
	private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	// 音频格式
	private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	// 编码的mime值
	private final static String MIME = "audio/mp4a-latm"; // 录音编码的mime
	// 编码的key bit rate
	private final static int RATE = 128000;
	// //音频采样通道，默认2通道
	private final static int CHANNELCOUNT = 1;

	private MediaCodec mEnc;

	public int bufferSizeInBytes;
	public boolean isRecording;
	private AudioRecord audioRecord;
	private MediaFormat format;
	private FileDescriptor fd;
	FileOutputStream fos;
	byte[] buffer;
	private FileDescriptor fd2;
	private DatagramSocket socket;
	private InetAddress host;

	public FileDescriptor getFileDescriptor(Socket socket) {
		Class<? extends Socket> claz = socket.getClass();
		try {
			Field impl_field = claz.getDeclaredField("impl");
			impl_field.setAccessible(true);
			SocketImpl object = (SocketImpl) impl_field.get(socket);
			Class<? extends SocketImpl> class1 = object.getClass();
			Class<?> superclass = class1.getSuperclass();
			Class<?> superclass2 = superclass.getSuperclass();
			Class<?> superclass3 = superclass2.getSuperclass();
			Method declaredMethod = superclass3.getDeclaredMethod("getFileDescriptor");
			declaredMethod.setAccessible(true);
			FileDescriptor fd = (FileDescriptor) declaredMethod.invoke(object);
			return fd;

		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Socket socketInit(String hostname, int port) throws UnknownHostException, IOException {
		Socket socket = new Socket(hostname, port);
//		new SocketAddress(abstractName)
		boolean connected = socket.isConnected();
		System.out.println("socketB  status :" + connected);
		return socket;
	}

	// 初始化音频编码器
	public void initAudioEncode() throws IOException {
		MediaFormat format = MediaFormat.createAudioFormat(MIME, AUDIO_SAMPLE_RATE, CHANNELCOUNT);
		format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		format.setInteger(MediaFormat.KEY_BIT_RATE, RATE);
		mEnc = MediaCodec.createEncoderByType(MIME);
		mEnc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); // 设置为编码器
	}

	// 检查手机音频
	public void checkAudio() {
		if (AudioRecord.ERROR_BAD_VALUE == bufferSizeInBytes || AudioRecord.ERROR == bufferSizeInBytes) {
			throw new RuntimeException("Unable to getMinBufferSize");
		}
		int state = audioRecord.getState();

		if (state == AudioRecord.STATE_UNINITIALIZED) {
			throw new RuntimeException("AudioRecord STATE_UNINITIALIZED");
		}
	}

	// 初始化音频录制
	public void initAudioRecord() {

//		bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2;
	//	bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
		bufferSizeInBytes = 4096;
		audioRecord = new AudioRecord(AUDIO_RESOURCE, AUDIO_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
				bufferSizeInBytes);

		Log.i("TTTTT", "bufferSizeInBytes" + bufferSizeInBytes);
		checkAudio();
	}

	// 添加AAC规则
	private void addADTStoPacket(byte[] packet, int packetLen) {
		int profile = 2; // AAC LC
		int freqIdx = 4; // 44.1KHz
		int chanCfg = 2; // CPE
		packet[0] = (byte) 0xFF;
		packet[1] = (byte) 0xF9;
		packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
		packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
		packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
		packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
		packet[6] = (byte) 0xFC;
	}

	public void test(ByteBuffer buffer, FileOutputStream fos, MediaCodec.BufferInfo mInfo) {
		buffer.position(mInfo.offset);
		// AAC编码，需要加数据头，AAC编码数据头固定为7个字节
		byte[] temp = new byte[mInfo.size + 7];
		buffer.get(temp, 7, mInfo.size);
		addADTStoPacket(temp, temp.length);
		Log.i("TTTTT", "temp.lenth" + temp.length);
		Log.i("TTTTT", "mInfo.size" + mInfo.size);
		try {
			fos.write(temp);
			fos.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// 音频编码处理
	public void audioEncodeProcess() {

		// ByteBuffer buffer = ByteBuffer.allocate(bufferSizeInBytes);
		// buffer.clear();// 清空内容
		// 读取放入盒子中

		int length = audioRecord.read(buffer, 0, bufferSizeInBytes);
		if (AudioRecord.ERROR_INVALID_OPERATION != length) {
			try {
				Log.i("TAG", "COMING ...." + fd + " " + bufferSizeInBytes + " " + length);
//				IO.writeFully(fd, buffer, 0, bufferSizeInBytes);
				DatagramPacket request = new DatagramPacket(buffer, length, host, 27183);
				socket.send(request);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
//		buffer.clear();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		try {
		// 初始化音频编码器
		// initAudioEncode();
		// mEnc.start();
		// 初始化音频录制
		// fos = new FileOutputStream(new File("/sdcard/aa.pcm"));
		initAudioRecord();
		audioRecord.startRecording();// 开始录制
//		}
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		new ReadThread().start();
	}

	class ReadThread extends Thread {

		@Override
		public void run() {
			android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
			try {
				socket = new DatagramSocket(27188);
				host = InetAddress.getByName("192.168.255.105");

			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			isRecording = true;
			buffer = new byte[bufferSizeInBytes];
//			try {
//				fd2 = fos.getFD();
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			while (isRecording) {
				audioEncodeProcess();
			}

			super.run();
		}
	}
}
