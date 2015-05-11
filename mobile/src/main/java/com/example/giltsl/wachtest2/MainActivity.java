package com.example.giltsl.wachtest2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;


public class MainActivity extends ActionBarActivity {

    final String TAG = getClass().getName();
    private GoogleApiClient gApiClient;
    TextView textView;
    Button connectButton;
    ImageView imageView;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.text);
        imageView = (ImageView)findViewById(R.id.ImageView);
        connect();
    }


    @Override
    protected void onResume() {
        super.onResume();
        gApiClient.connect();
    }

    private void connect() {

        gApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "onConnected bundle = ");

                        connected = true;

                        Wearable.MessageApi.addListener(gApiClient, new MessageApi.MessageListener() {

                                @Override
                                public void onMessageReceived(MessageEvent messageEvent) {

                                    Log.d(TAG, "onMessageReceived " + messageEvent.getPath());

                                    if (messageEvent.getPath().compareTo("ScreenShot") == 0) {
                                        setNewImage(messageEvent);
                                    } else if (messageEvent.getPath().compareTo("logs") == 0) {
                                        setNewLog(messageEvent);

                                    } else {
                                        Log.d(TAG, "Unknown msg type");
                                    }
                                }
                            });
                        }

                            @Override
                            public void onConnectionSuspended ( int i){
                                Log.d(TAG, "onConnectionSuspended int = " + i);
                                connected = false;

                            }
                        }
                    )
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed (ConnectionResult connectionResult){
                            Log.d(TAG, "onConnectionFailed ConnectionResult  = " + connectionResult.toString());
                            connected = false;

                        }
                    }
                ).addApiIfAvailable(Wearable.API)
                .build();
    }

    private void setNewLog(MessageEvent messageEvent) {

        byte[] bytesData = messageEvent.getData();
        try {
            final String decoded = new String(bytesData, "UTF-8");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(decoded);
                }
            });


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private void setNewImage(MessageEvent messageEvent) {
        byte[] bytesData = messageEvent.getData();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytesData, 0, bytesData.length);
//
        if (imageView != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageBitmap(bitmap);
                }
            });


        } else {
            Log.d(TAG, "could not set imageView");
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
