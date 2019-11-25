package mictest2.example.com.mictest2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;


import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Delayed;


public class MainActivity extends Activity {
    private ProgressBar mProgressBar;
    private int mProgressStatus =0;
    private Handler mHandler = new Handler();
    private Handler textHandler = new Handler();
    private Handler timeHandler = new Handler();
    private TextView text, numberOfUtterances, numberSaved;

    public byte[] receiveData;
    public static DatagramSocket socket;
    private int port = 9858;

    private int array_view = 0;
    private int array_size = 0;
    private ArrayList<String> words = new ArrayList<String>();
    private int number_saved = 0;
    private int number_received = 0;
    public volatile boolean isSending = false;
    AudioRecord recorder;
    private long currentTime;
    private long lastUpdated;



    private int sampleRate = 44100; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = 4096;
    private boolean status = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        words.add("No further data has been collected.");
        //words.add("number two");
        //words.add("smaller words with small");
        //words.add("twolargewords sidebyside in consideratinable longest sentence structuration of the for");
        //words.add("this is a mid size amount with a largeableaidaif word in the middle");
        //words.add("This is the example of a long piece of txt that might take up a significant amount of room");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        text = (TextView) findViewById(R.id.textView1);
        text.setText(words.get(0)); //temporary for testing
        numberOfUtterances = (TextView) findViewById(R.id.numberOf);
        numberSaved = (TextView) findViewById(R.id.numberSaved);
        currentTime = new Date().getTime();
        lastUpdated = new Date().getTime();
        status = true;
        startStreaming();
        monitorTime();
    }

    public void monitorTime() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                while (status == true) {
                    currentTime = new Date().getTime();
                    System.out.println(lastUpdated);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex){
                    }
                    if (currentTime - lastUpdated > 15000) {
                        changeText(words.get(array_size));
                        lastUpdated = new Date().getTime();
                    }
                }
            }

        }).start();
    }

    public void changeText(final String sentence) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                text.setText(sentence);
                array_view = array_size;

            }
        });
    }

    public void addToQueue(final String texty) {
        final String newText = texty;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                array_size = array_size + 1;
                number_received = number_received + 1;
                words.add(array_size, newText);

                numberOfUtterances.setText(Integer.toString(number_received));

            }
        });
    }

    public void changeTextColor(String color) {
        text.setTextColor(Color.parseColor(color));
        new Thread(new Runnable(){
            @Override
            public void run(){
                textHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        android.os.SystemClock.sleep(1000);
                        text.setTextColor(Color.parseColor("#FFFFFF"));
                        words.remove(array_view);
                        array_size = array_size - 1;
                        //text.setText(words.get(array_size));
                        array_view = array_size;
                        System.out.println(array_size);
                        changeText(words.get(array_size));
                        mProgressStatus = 0;
                        mProgressBar.setProgress(mProgressStatus);

                    }
                });


            }
        }).start();
    }

    public void deleteAndUpdate() {
        words.remove(array_view);
        array_size = array_size - 1;
        //text.setText(words.get(array_size));
        array_view = array_size;
        //System.out.print(array_view);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        lastUpdated = new Date().getTime();

        if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT) && array_size != 0){
            if (!isSending) {
                changeTextColor("#FF0000");
                isSending = false;
                mProgressStatus = 0;
            } else {
                isSending = false;
            }
        }



        if ((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)){

            if (!isSending && array_size != 0) {


                mProgressStatus = 0;
                isSending = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (mProgressStatus < 100 && isSending) {

                            mProgressStatus += 1;
                            android.os.SystemClock.sleep(40);
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {

                                    mProgressBar.setProgress(mProgressStatus);
                                    if (mProgressStatus == 100) {

                                        changeTextColor("#32CD32");


                                        isSending = false;
                                    }
                                }
                            });
                            if (!isSending) {
                                mProgressStatus = 0;

                            }

                        }

                    }


                }).start();


            }else if (array_size > 0){
                mProgressStatus = 99;
                number_saved++;
                numberSaved.setText(String.valueOf(number_saved));
            } else {

            }

        }

        return true;
    }


    private final OnClickListener nextInQueue = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            array_view = array_view + 1;
            //changeText(words[array_view]);
        }

    };
    private final OnClickListener stopListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            status = false;
            recorder.release();
            Log.d("VS", "Recorder released");
        }

    };


    public void startStreaming() {


        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    System.out.println(minBufSize);
                    DatagramSocket socket = new DatagramSocket();
                    Log.d("VS", "Socket Created");

                    byte[] buffer = new byte[minBufSize];
                    receiveData = new byte[minBufSize];

                    Log.d("VS", "Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName("192.168.43.99");
                    Log.d("VS", "Address retrieved");


                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10);
                    Log.d("VS", "Recorder initialized");

                    recorder.startRecording();


                    while (status == true) {


                        //reading data from MIC into buffer
                        minBufSize = recorder.read(buffer, 0, buffer.length);

                        //putting buffer in the packet
                        packet = new DatagramPacket(buffer, buffer.length, destination, port);

                        socket.send(packet);
                        //System.out.println("MinBufferSize: " + minBufSize);

                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(receivePacket);
                        String modifiedSentence = new String(receivePacket.getData(),0, receivePacket.getLength() );
                        addToQueue(modifiedSentence);
                    }


                } catch (UnknownHostException e) {
                    Log.e("VS", "UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("VS", "IOException");
                }
            }

        });
        streamThread.start();
    }
}