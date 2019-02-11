package com.escombros.prueba;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Date;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;


public class BackgroundConnectionService extends IntentService {

    boolean isActivated = true;
    BluetoothSPP bluetooth;

    public BackgroundConnectionService() {
        super("BackgroundConnectionService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    @Override
    public int onStartCommand(@Nullable final Intent intent, int flags, int startId) {
        startServiceOreoCondition();
        onReceive(this, intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lowerAudio(this);
        bluetooth.stopService();
    }

    private void startServiceOreoCondition() {
        if (Build.VERSION.SDK_INT >= 26) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            Notification notification = new NotificationCompat.Builder(this, "earthmozione")
                    .setContentTitle("Earthmozione")
                    .setContentText("Early Earthquake alert.")
                    .setSmallIcon(R.drawable.logo)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);
        }
    }

    public void onReceive(Context context, Intent intent) {
        System.out.println("OOOOOOOOOO");
        if (intent == null) return;
        if (!initBT(context)) return;

        String msg = intent.getStringExtra("msg");
        int requestCode = intent.getIntExtra("requestCode", -1);
        int resultCode = intent.getIntExtra("resultCode", -1);

        switch (msg) {
            case "connect":
                if (resultCode == Activity.RESULT_OK) {
                    if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
                        bluetooth.connect(intent);
                    } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
                        bluetooth.setupService();
                    }
                } else {
                    Toast.makeText(context, "Bluetooth not enabled.", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            case "disconnect":
                lowerAudio(context);
                bluetooth.stopService();
                Toast.makeText(context, "Desconectado", Toast.LENGTH_SHORT).show();
                break;
            case "activate":
                isActivated = true;
                break;
            case "deactivate":
                isActivated = false;
                break;
        }
    }

    public boolean initBT(final Context context) {
        if (bluetooth == null)
            bluetooth = new BluetoothSPP(context);
        if (!bluetooth.isBluetoothAvailable()) {
            Toast.makeText(context, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!bluetooth.isBluetoothEnabled()) {
            bluetooth.enable();
        }
        if (!bluetooth.isServiceAvailable()) {
            bluetooth.setupService();
            bluetooth.startService(BluetoothState.DEVICE_OTHER);
        }
        bluetooth.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                System.out.println("mensaje: " + message);
                int intensidad = Integer.parseInt(message);
                if (intensidad > 0) {
                    try {
                        Date date = new Date();
                        long time = date.getTime();
                        Timestamp ts = new Timestamp(time);
                        String jsobject = "{id_sensor : 1, timestamp : " + ts.toString() + " ," +
                                "location: {x: 19.4510848, y : -99.1289344, magnitud : " + intensidad + "} }";
//                        las coordenadas de arriba son estaticas por flojera XD
                        System.out.println(jsobject);
                        JSONObject jsonObject = new JSONObject(jsobject);
//                        sendHttpRequest("201.166.145.191:80", jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    raiseAudio(context);
                    showNotification("Alerta", "Se aproxima un sismo, ¡Tome precauciones!", context);
                }
            }
        });

        bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(context, "Connected to " + name, Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() {
                Toast.makeText(context, "Connection lost", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() {
                Toast.makeText(context, "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });
        return true;
    }

    public void raiseAudio(Context context) {
        if (!isActivated) return;
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        assert audio != null;
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        float percent = 1f;
        int seventyVolume = (int) (maxVolume * percent);
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, seventyVolume, 0);
    }

    public void lowerAudio(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        assert audio != null;
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
    }

    public void showNotification(String title, String message, Context mContext) {
        if (!isActivated) return;
        //Creates an explicit intent for an Activity in your app*
        Intent resultIntent = new Intent(mContext, MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + mContext.getPackageName() + "/" + R.raw.alarm);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(false)
                .setSound(alarmSound)
                .setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel("earthmozione_alert", "earthmozione_alert", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            assert mNotificationManager != null;
            mBuilder.setChannelId("earthmozione_alert");
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        assert mNotificationManager != null;
        raiseAudio(mContext);
        mNotificationManager.notify(0, mBuilder.build());
    }
}
