package org.michoko.lazyconnectionclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.util.LinkedHashSet;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import lombok.EqualsAndHashCode;

public class MainActivity extends AppCompatActivity {

    ListView messageTypesView;
    ArrayAdapter<String> adapter;
    LinkedHashSet<Integer> messageTypes = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get ListView object from xml
        messageTypesView = findViewById(R.id.messageTypes);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);

        // Assign adapter to ListView
        messageTypesView.setAdapter(adapter);

        // ListView Item Click Listener
        messageTypesView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> onItemClick((String) messageTypesView.getItemAtPosition(position)));

       /*new Thread(() -> {
            while (!isDestroyed()) {
                StringBuilder b = new StringBuilder();
                Random random = new Random();
                int len = random.nextInt(9);
                b.append(Integer.toHexString(random.nextInt(9) + 0xAA).toUpperCase());
                b.append(',');
                b.append(len);
                b.append(',');
                for (int i = 0; i < len; i++) {
                    b.append(Integer.toHexString(random.nextInt(0xFF)).toUpperCase());
                    if (i < len - 1) {
                        b.append(' ');
                    }
                }
                runOnUiThread(() ->
                        onNewMessage(CanbusMessageHelper.parseMessage(b.toString())));

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }).start();*/
    }

    private void onItemClick(String item) {
        Intent intent = new Intent(MainActivity.this, OneMessageTypeView.class);
        Bundle b = new Bundle();
        b.putInt("msgId", Integer.parseInt(item, 16));
        intent.putExtras(b);
        startActivity(intent);
    }

    private void onNewMessage(CanbusMessage msg) {
        if (msg == null) {
            return;
        }
        Log.i("MainActivity", "New msg: " + msg);
        if (messageTypes.add(msg.getId())) {
            adapter.insert(Integer.toHexString(msg.getId()).toUpperCase(), 0);
        }
    }

    @EqualsAndHashCode(of = "mActivity")
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    mActivity.get().onNewMessage((CanbusMessage) msg.obj);
                    break;
            }
        }
    }

    private UsbService usbService;
    private MyHandler handler = new MyHandler(MainActivity.this);
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            usbService = ((UsbService.UsbBinder) binder).getService();
            usbService.addHandler(handler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService.removeHandler(handler);
            usbService = null;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
}
