package com.venky97vp.android.backgroundnavigation;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import static com.venky97vp.android.backgroundnavigation.Constants.ACTION_PLAY;
import static com.venky97vp.android.backgroundnavigation.Constants.ACTION_STOP;
import static com.venky97vp.android.backgroundnavigation.Constants.TAG;

public class MyService extends Service {
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyManager;
    private Socket socket;
    private MediaPlayer mediaPlayer;
    private RemoteViews remoteViews;
    private WifiManager wifiManager;
    private Place currentPlace;

    public MyService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: inside");
        if(intent.getAction()==null){
            return START_STICKY;
        }
        checkNearbyWifi();
        Log.d(TAG, "onStartCommand: not null");
        if (intent.getAction().equals(ACTION_PLAY)) {
            Log.d(TAG, "onStartCommand: paused/played");
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                remoteViews.setImageViewResource(R.id.play_button,R.drawable.ic_play_arrow_black_24dp);
                remoteViews.setTextViewText(R.id.message,"Paused");
            } else {
                mediaPlayer.start();
                remoteViews.setImageViewResource(R.id.play_button,R.drawable.ic_pause_black_24dp);
                remoteViews.setTextViewText(R.id.message,"Playing");
            }
        }else if(intent.getAction().equals(ACTION_STOP)){
            Log.d(TAG, "onStartCommand: stopped");
            mediaPlayer.seekTo(0);
            mediaPlayer.pause();
            remoteViews.setImageViewResource(R.id.play_button,R.drawable.ic_play_arrow_black_24dp);
            remoteViews.setTextViewText(R.id.message,"Stopped");
        }
        mBuilder.setContent(remoteViews);
        mNotifyManager.notify(0, mBuilder.build());
        return START_STICKY;
    }

    private void checkNearbyWifi() {
        
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: started");
        createNotification("SASTRA", "");
        new RetrieveData().execute();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                createNotification("Chith Vihar","");
            }
        });
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void createNotification(String title, String message) {
        mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText(message);
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyManager.notify(0, mBuilder.build());
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
                        try {
                            byte[] contents = new byte[10000];
                            File file = new File(String.valueOf(Environment.getExternalStoragePublicDirectory("/background/audio.mp3")));
                            Log.d(TAG, "run: filecreated = " + file.createNewFile());
                            FileOutputStream fos = new FileOutputStream(file);
                            BufferedOutputStream bos = new BufferedOutputStream(fos);
                            InputStream is = socket.getInputStream();
                            int bytesRead = 0;
                            long progressBytes = 0;
                            mBuilder.setProgress(0, 0, true);
                            mNotifyManager.notify(0, mBuilder.build());
                            while ((bytesRead = is.read(contents)) != -1) {
                                progressBytes += bytesRead;
                                //Log.d(TAG, "createConnection: completing = " + progressBytes);
                                bos.write(contents, 0, bytesRead);

                            }
                            bos.flush();
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mBuilder.setContentText("Loading completed")
                                .setProgress(0, 0, false);
                        mNotifyManager.notify(0, mBuilder.build());
                        playAudio(Uri.fromFile(Environment.getExternalStoragePublicDirectory("/background/audio.mp3")));
                    }
                }
        ).start();
    }

    private void playAudio(Uri myUri) {
        customNotification("Chith Vihar", "Playing");
        try {
            mediaPlayer.setDataSource(getApplicationContext(), myUri);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
    }

    private void customNotification(String label, String message) {
        remoteViews = new RemoteViews(getPackageName(), R.layout.playerlayout);
        Intent playIntent = new Intent(this, MyService.class);
        playIntent.setAction(ACTION_PLAY);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0, playIntent, 0);

        Intent stopIntent = new Intent(this, MyService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pstopIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        remoteViews.setTextViewText(R.id.label, label);
        remoteViews.setTextViewText(R.id.message, message);
        remoteViews.setOnClickPendingIntent(R.id.play_button, pplayIntent);
        remoteViews.setOnClickPendingIntent(R.id.stop_button, pstopIntent);
        mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContent(remoteViews);
        Log.d(TAG, "customNotification: listener is set");
        mNotifyManager.notify(0, mBuilder.build());
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
        private Exception exception;

        protected void onPostExecute() {
        }

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

}
