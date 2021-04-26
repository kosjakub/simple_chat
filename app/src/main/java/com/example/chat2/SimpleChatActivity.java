package com.example.chat2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.lang.ref.WeakReference;
import java.text.BreakIterator;
import java.util.ArrayList;

public class SimpleChatActivity extends AppCompatActivity {

    ArrayList<String> listItems = new ArrayList<String>();

    ArrayAdapter<String> adapter;
    private ListView chatListView;
    private EditText messageEditText;
    private Button postButton;
    private String nick;
    private String ip;
    private TextView nickTextView;

    private static class MyHandler extends Handler {
        private final WeakReference<SimpleChatActivity> sActivity;

        MyHandler(SimpleChatActivity activity) {
            sActivity = new WeakReference<SimpleChatActivity>(activity);
        }

        public void handleMessage(Message msg) {
            SimpleChatActivity activity = sActivity.get();
            activity.listItems.add("[" + msg.getData().getString("NICK") + "]" + msg.getData().getString("MSG"));
            activity.adapter.notifyDataSetChanged();
            activity.chatListView.setSelection(activity.listItems.size() - 1);
        }
    }

    Handler myHandler = new MyHandler(this);

    public void postOnClick(View view) throws MqttException {
        Message msg = myHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("NICK", nick);
        b.putString("MSG", messageEditText.getText().toString());
        msg.setData(b);
        String line = messageEditText.getText().toString();
        MqttMessage message = new MqttMessage(line.getBytes());
        message.setQos(0);
        sampleClient.publish(nick, message);

    }

    MqttClient sampleClient = null;

    private void startMQTT() {
        String clientId;
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            String broker = "tcp://" + ip + ":1883";
            clientId = nick;
            sampleClient = new MqttClient(broker, clientId, persistence);
            sampleClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    System.out.println("connection lost");
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                    Message msg = myHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("NICK", topic);
                    b.putString("MSG", mqttMessage.toString());
                    msg.setData(b);
                    myHandler.sendMessage(msg);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    System.out.println("deliveryComplete");
                }
            });
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            sampleClient.subscribe("#");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_chat);

        nick = getIntent().getStringExtra(MainActivity.NICK);
        ip = getIntent().getStringExtra(MainActivity.IP);
        nickTextView = findViewById(R.id.nickTextView);
        nickTextView.setText(getIntent().getStringExtra(MainActivity.NICK));

        chatListView = (ListView) findViewById(R.id.chatListView);

        messageEditText = findViewById(R.id.messageEditText);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, listItems);
        new Thread(new Runnable() {
            @Override
            public void run() {
                startMQTT();
            }
        }).start();
        postButton = findViewById(R.id.postButton);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    postOnClick(v);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        chatListView.setAdapter(adapter);

    }
}
