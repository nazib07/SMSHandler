package com.nazib.smshandler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity implements MessageListener{

    private static final int SEND_SMS = 100;
    BlockingQueue<String> sharedQ = new LinkedBlockingQueue<String>();
    BlockingQueue<String> smsQueue = new LinkedBlockingQueue<String>();
    int i=0;

    EditText sms_ted;
    TextView tv_client;
    private String mobile;
    private String mSMSText = "";

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            //Toast.makeText(getApplicationContext(), msg.getData().getString("MSG", "Toast"), Toast.LENGTH_SHORT).show();

            mSMSText = msg.getData().getString("MSG", "Toast");
            String MobileNumber = "01832056678";
            checkAndroidVersion(MobileNumber);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sms_ted = findViewById(R.id.ted_to_client);
        tv_client = findViewById(R.id.tv_client);

        MessageReceiver.bindListener(this);

        Consumer c = new Consumer(sharedQ, smsQueue);
        c.start();

        SMSThread smsThread = new SMSThread(smsQueue);
        smsThread.start();
    }

    public void onSendButtonClicked(View view) {
//        System.out.println(" produced " + i);
//        try {
//            sharedQ.put(String.valueOf(i++));
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        String MobileNumber = "01832056678";
//        checkAndroidVersion(MobileNumber);
    }

    @Override
    public void messageReceived(String message) {
        Log.d("Nazib", "message: " + message);
        try {
            sharedQ.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void checkAndroidVersion(String mobile){
        this.mobile= mobile;
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
            if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},SEND_SMS);
                return;
            }else{
                sendSms(mobile);
            }
        } else {
            sendSms(mobile);
        }
    }

    private void sendSms(String mobileNo){
        try {
//            String Message = sms_ted.getText().toString();
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(mobileNo, null, mSMSText, null, null);
            //  smsManager.sendTextMessage(number,null,matn,null,null);

            Toast.makeText(this, "Message Sent", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error"+e, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case SEND_SMS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSms(mobile);
                } else {
                    Toast.makeText(MainActivity.this, "SEND_SMS Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }




class Consumer extends Thread {
    private BlockingQueue<String> sharedQueue;
    private BlockingQueue<String> sharedSMSQueue;

    public Consumer(BlockingQueue<String> aQueue, BlockingQueue<String> smsQueue) {
        super("CONSUMER");
        this.sharedQueue = aQueue;
        this.sharedSMSQueue = smsQueue;
    }

    public void run() {

        ServerSocket ss = null;
        DataOutputStream dout =null;
        DataInputStream din =null;

        try {
            ss = new ServerSocket(3333);
            System.out.println("Waiting for connection on port: 3333");
            Socket s = ss.accept();
            dout = new DataOutputStream(s.getOutputStream());
            din = new DataInputStream(s.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {

            if(sharedQueue.isEmpty()) {

                try {
                    String fromclient = null;
                    if (din.available() > 0)
                        fromclient = din.readUTF();//FIXME
                    if (fromclient != null) {
                        if (!fromclient.isEmpty()) {
                            try {
                                sharedSMSQueue.put(fromclient);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {

                try {
                    String item = sharedQueue.take();
                    System.out.println(getName() + " consumed " + item);
                    dout.writeUTF(item);
                    dout.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }
}

//Send SMS Thread



class SMSThread extends Thread {
    private BlockingQueue < String > smsQueue;
    public SMSThread(BlockingQueue< String > aQueue) {
        super("SMSThread");
        this.smsQueue = aQueue;
    }
    public void run() {

            while (true) {

                String item = null;
                try {
                    item = smsQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(getName() + " sms " + item);

                tv_client.setText(item);

                if(item != null) {

                    Message message = Message.obtain();
                    Bundle bundle = new Bundle();
                    bundle.putString("MSG", item);
                    message.setData(bundle);
                    handler.sendMessage(message);
                }
            }

    }
}

}