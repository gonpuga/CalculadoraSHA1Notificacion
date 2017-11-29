package com.example.calculadorasha1notificacion;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Sha1HashBroadCastNotiService mService;
    boolean mBound = false;

    private DigestReceiver mReceiver = new DigestReceiver();

    private static class DigestReceiver extends BroadcastReceiver {

        private TextView view;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (view != null) {
                String result = intent.getStringExtra(
                        Sha1HashBroadCastNotiService.RESULT);
                intent.putExtra(Sha1HashBroadCastNotiService.HANDLED, true);
                view.setText(result);
            } else {
                Log.i("Sha1HashService", " ignoring - we're detached");
            }
        }

        public void attach(TextView view) {
            this.view = view;
        }
        public void detach() {
            this.view = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button queryButton = (Button)findViewById(R.id.hashIt);
        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.text);
                if ( mService != null ) {
                    mService.getSha1Digest(et.getText().toString());
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, Sha1HashBroadCastNotiService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mReceiver.attach((TextView)
                findViewById(R.id.hashResult));
        IntentFilter filter = new IntentFilter(
                Sha1HashBroadCastNotiService.SHA1_BROADCAST);
        LocalBroadcastManager.getInstance(this).
                registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        LocalBroadcastManager.getInstance(this).
                unregisterReceiver(mReceiver);
        mReceiver.detach();
    }
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Sha1HashBroadCastNotiService.LocalBinder binder = (Sha1HashBroadCastNotiService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService= null;
        }
    };

}