package com.example.calculadorasha1notificacion;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.security.MessageDigest;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Sha1HashBroadCastNotiService extends Service {
    public static final String SHA1_BROADCAST = "SHA1_BROADCAST";
    public static final String RESULT = "sha1";
    public static final String HANDLED = "intent_handled";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    //
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAXIMUM_POOL_SIZE = 4;
    private static final int MAX_QUEUE_SIZE = 16;

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(MAX_QUEUE_SIZE);

    //notifications
    private NotificationManager nm;
    private static final String CHANNEL_ID="myChannel";

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r,"Sha1HashBroadcastService #" + mCount.getAndIncrement());
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    };

    private ThreadPoolExecutor mExecutor;

    public class LocalBinder extends Binder {
        Sha1HashBroadCastNotiService getService() {
            // Return this instance of LocalService so clients can call public methods
            return Sha1HashBroadCastNotiService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    private void broadcastResult(final String text,final String result) {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler =  new Handler(mainLooper);
        handler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SHA1_BROADCAST);
                intent.putExtra(RESULT, result);
                LocalBroadcastManager.getInstance(Sha1HashBroadCastNotiService.this).
                        sendBroadcastSync(intent);
                boolean handled = intent.getBooleanExtra(HANDLED, false);
                if(!handled){
                    notifyUser(text, result);
                }
            }
        });
    }

    private void notifyUser(final String text,final String digest) {
        String msg = String.format(
                "The Hash for %s is %s", text,digest);
        // Gets an instance of the NotificationManager service
        nm = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        //create channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name="Sha1 Channel";
            String description="Channel to share Sha1 Hash";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Hashing Result")
                .setContentText(msg);
        // Sets an unique ID for this notification
        nm.notify(text.hashCode(), builder.build());
    }

    void getSha1Digest(final String text) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.i("Sha1HashService", "Hashing text " + text + " on Thread " +
                        Thread.currentThread().getName());
                try {
                    // Execute the Long Running Computation
                    final String result = SHA1(text);
                    Log.i("Sha1HashService", "Hash result for "+text+" is "+result);
                    // broadcast result to Subscribers
                    broadcastResult(text,result);
                } catch (Exception e){
                    Log.e("Sha1HashService", "Hash failed", e);
                }
            }
        };
        // Submit the Runnable on the ThreadPool
        mExecutor.execute(runnable);
    }


    @Override
    public void onCreate() {

        Log.i("Sha1HashService", "Starting Hashing Service");
        super.onCreate();
        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, 5,
                TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);
        mExecutor.prestartAllCoreThreads();

    }

    @Override
    public void onDestroy() {
        Log.i("Sha1HashService", "Stopping Hashing Service");
        super.onDestroy();
        mExecutor.shutdown();
    }

    public  String SHA1(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }
    private  String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) :
                        (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}