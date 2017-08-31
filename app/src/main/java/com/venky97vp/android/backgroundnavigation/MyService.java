package com.venky97vp.android.backgroundnavigation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

import static com.venky97vp.android.backgroundnavigation.Constants.ACTION_PLAY;
import static com.venky97vp.android.backgroundnavigation.Constants.ACTION_REPLAY;
import static com.venky97vp.android.backgroundnavigation.Constants.ACTION_STOP;
import static com.venky97vp.android.backgroundnavigation.Constants.CHITH;
import static com.venky97vp.android.backgroundnavigation.Constants.PLACE;
import static com.venky97vp.android.backgroundnavigation.Constants.TAG;

public class MyService extends Service {
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyManager;
    private Socket socket;
    private MediaPlayer mediaPlayer;
    private RemoteViews playerViews;
    private RemoteViews notificationViews;
    private WifiManager wifiManager;
    private Place currentPlace;
    private List<ScanResult> wifiList;

    public MyService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: inside");
        if (intent.getAction() == null) {
            return START_STICKY;
        }
        checkNearbyWifi();
        Log.d(TAG, "onStartCommand: not null");
        if (intent.getAction().equals(ACTION_PLAY)) {
            Log.d(TAG, "onStartCommand: paused/played");
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playerViews.setImageViewResource(R.id.play_button, R.drawable.ic_play_arrow_black_24dp);
                playerViews.setTextViewText(R.id.message, "Paused");
            } else {
                mediaPlayer.start();
                playerViews.setImageViewResource(R.id.play_button, R.drawable.ic_pause_black_24dp);
                playerViews.setTextViewText(R.id.message, "Playing");
            }
        } else if (intent.getAction().equals(ACTION_STOP)) {
            Log.d(TAG, "onStartCommand: stopped");
            mediaPlayer.seekTo(0);
            mediaPlayer.pause();
            playerViews.setImageViewResource(R.id.play_button, R.drawable.ic_play_arrow_black_24dp);
            playerViews.setTextViewText(R.id.message, "Stopped");
        } else if (intent.getAction().equals(ACTION_REPLAY)) {
            playAudio(Uri.fromFile(Environment.getExternalStoragePublicDirectory("/background/" + currentPlace.audio + ".mp3")));
        }
        mBuilder.setContent(playerViews);
        updateNotification();
        return START_STICKY;
    }

    private void updateNotification() {
        mNotifyManager.notify(0, mBuilder.build());
    }

    private void nextPlaceNotification() {
        if (currentPlace.nextPlace != null)
            createNotification("Next " + currentPlace.nextPlace.name, currentPlace.nextPlace.description);
    }

    private void checkNearbyWifi() {
        if (!wifiManager.isWifiEnabled()) {
            Log.d(TAG, "checkNearbyWifi: Turned on");
            wifiManager.setWifiEnabled(true);
        }
        wifiManager.startScan();
        wifiList = wifiManager.getScanResults();
        Log.d(TAG, "checkNearbyWifi: " + wifiList);
        for (ScanResult s : wifiList) {
            Log.d(TAG, "checkNearbyWifi: " + s);
            for (Place aPLACE : PLACE) {
                if (s.SSID.equals(aPLACE.SSID)) {
                    currentPlace = aPLACE;
                }
            }
        }
        if (currentPlace != null) {
            Log.d(TAG, "checkNearbyWifi: currentPlace is " + currentPlace.SSID);
            new RetrieveData().execute();
        } else {
            Log.d(TAG, "checkNearbyWifi: currentPlace is null");
        }
        //new CheckWifi().execute();
    }

    private void connectToWifi(String networkSSID, String networkPass) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";
        conf.preSharedKey = "\"" + networkPass + "\"";
        wifiManager.addNetwork(conf);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: started");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextPlaceNotification();
            }
        });
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        createNotification("Welcome", "Thanks for using the application. This is going to take tourism to the whole new level. \nHappy touring!");
        checkNearbyWifi();
    }

    private void createNotification(String title, String message) {
        notificationViews = new RemoteViews(getPackageName(), R.layout.notification_layout);

        Intent replayIntent = new Intent(this, MyService.class);
        replayIntent.setAction(ACTION_REPLAY);
        PendingIntent preplayIntent = PendingIntent.getService(this, 0, replayIntent, 0);

        notificationViews.setTextViewText(R.id.label, title);
        notificationViews.setTextViewText(R.id.message, message);
        notificationViews.setOnClickPendingIntent(R.id.replay_button, preplayIntent);

        mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setContent(notificationViews)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        updateNotification();
    }

    private void createConnection() throws IOException {
        socket = new Socket("192.168.137.1", 5005);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Chith Vihar")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentText("loading in progress");

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        File file = null;
                        try {
                            byte[] contents = new byte[10000];
                            //file = File.createTempFile(currentPlace.audio, ".mp3", getApplicationContext().getCacheDir());
//                            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS)
//                                            + File.separator + currentPlace.audio,
//                                    "scouthouse_thumbnails");
//                            getApplication().sendBroadcast(
//                                    new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
//                                            + Environment.getExternalStorageDirectory())));
//                            if (!file.mkdirs()) {
//                                Log.d("file", "file not created");
//                            }else{
//                                Log.d("file", "file created");
//                            }
                            file = new File(getApplicationContext().getFilesDir(), currentPlace.audio + ".mp3");
                            Log.d(TAG, "run: fileCreated = " + file.createNewFile());
                            FileOutputStream fos = new FileOutputStream(file);
                            BufferedOutputStream bos = new BufferedOutputStream(fos);
                            InputStream is = socket.getInputStream();
                            int bytesRead = 0;
                            long progressBytes = 0;
                            mBuilder.setProgress(0, 0, true);
                            updateNotification();
                            while ((bytesRead = is.read(contents)) != -1) {
                                progressBytes += bytesRead;
                                //Log.d(TAG, "createConnection: completing = " + progressBytes);
                                bos.write(contents, 0, bytesRead);
                            }
                            Log.d(TAG, "run: Getting the audio");
                            bos.flush();
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mBuilder.setContentText("Loading completed")
                                .setProgress(0, 0, false);
                        updateNotification();
                        playAudio(Uri.fromFile(file));
                    }
                }
        ).start();

//        if(new File(String.valueOf(Environment.getExternalStoragePublicDirectory("/background/"+currentPlace.audio+".mp3"))).exists()){
//            playAudio(Uri.fromFile(Environment.getExternalStoragePublicDirectory("/background/"+currentPlace.audio+".mp3")));
//        }
//        else{
//
//        }
    }

    private void playAudio(Uri myUri) {
        mediaPlayer.reset();
        customNotification(currentPlace.name, "Playing");
        try {
            mediaPlayer.setDataSource(getApplicationContext(), myUri);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
    }

    private void customNotification(String label, String message) {
        playerViews = new RemoteViews(getPackageName(), R.layout.player_layout);

        Intent playIntent = new Intent(this, MyService.class);
        playIntent.setAction(ACTION_PLAY);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0, playIntent, 0);

        Intent stopIntent = new Intent(this, MyService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pstopIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        playerViews.setTextViewText(R.id.label, label);
        playerViews.setTextViewText(R.id.message, message);
        playerViews.setOnClickPendingIntent(R.id.play_button, pplayIntent);
        playerViews.setOnClickPendingIntent(R.id.stop_button, pstopIntent);
        mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContent(playerViews);
        updateNotification();
    }

    @Override
    public void onDestroy() {
        mediaPlayer.release();
        mediaPlayer = null;
        super.onDestroy();
        Log.d(TAG, "onDestroy: destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: bound");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class RetrieveData extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                createConnection();
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: ", e);
            }
            return null;
        }
    }

    private class CheckWifi extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d(TAG, "onPostExecute: Executing RetrieveData");
            new RetrieveData().execute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (!wifiManager.isWifiEnabled()) {
                Log.d(TAG, "checkNearbyWifi: Turned on");
                wifiManager.setWifiEnabled(true);
            }
            wifiManager.startScan();
            wifiList = wifiManager.getScanResults();
            Log.d(TAG, "checkNearbyWifi: " + wifiList);
            for (ScanResult s : wifiList) {
                Log.d(TAG, "checkNearbyWifi: " + s);
                for (Place aPLACE : PLACE) {
                    if (s.SSID.equals(aPLACE.SSID)) {
                        currentPlace = aPLACE;
                        Log.d(TAG, "checkNearbyWifi: " + currentPlace.SSID);
                    }
                }
            }
            //connectToWifi(currentPlace.SSID,currentPlace.password);
            return null;
        }
    }
}
