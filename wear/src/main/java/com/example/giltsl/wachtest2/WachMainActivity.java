package com.example.giltsl.wachtest2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import logs.LogListener;
import logs.LogReader;
import logs.LogcatReader;
import screenshot.ScreenCapture;

public class WachMainActivity extends Activity {

    final String TAG = getClass().getName();

    private TextView mTextView;
    static int count  = 0 ;

    private String transcriptionNodeId = null;
    private static final String SCREENSHOTS = "ScreenShot";
    private static final String LOGS = "logs";

    GoogleApiClient gApiClient;
    private boolean connected = false;
    private TextView logsView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wach_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                logsView = (TextView) stub.findViewById(R.id.logsView);
                mTextView = (TextView) stub.findViewById(R.id.text);
                mTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String dataString = "Hello Giltsl " + count++;
                        ((TextView) v).setText(dataString);
                        if (connected) {
                            Log.d(TAG, "I am connected, going to send data:" + dataString);
                            //send

                        } else {

                            Log.w(TAG, "Not connected, cant send the data, it will be lost ;(" + dataString);
                        }
                    }
                });

            }
        });

        gApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "onConnected bundle = ");
                        connected = true;
                        setupDataTranscription();

                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "onConnectionSuspended int = " + i );
                        connected = false;
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "onConnectionFailed ConnectionResult  = " + connectionResult.toString());
                        connected = false;
                    }
                })
                .addApiIfAvailable(Wearable.API)
                .build();

        LogReader logReader;
        LogListener logListener = new LogListener() {
            @Override
            public void onNewLog(String level, String tag, final String text) {


                requestData(text.getBytes(), LOGS);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (logsView != null) {
                            logsView.setText(text);


                        } else {
                            Log.d(TAG, "logsView isn't initialised yet");
                        }
                    }
                });


            }

            @Override
            public void onLogFailure() {
                Log.d(TAG, "onLogFailure");

            }
        };
//        if (new File(LogFileReader.DEV_LOG_MAIN).exists()) {
//            Log.d(TAG, "using LogFileReader");
//            logReader = new LogFileReader(logListener);
//
//        } else {
            Log.d(TAG, "using LogcatReader");
            logReader = new LogcatReader(logListener);
//        }

        logReader.start();
    }

    private void setupDataTranscription() {
        Wearable.CapabilityApi
                .getCapability(gApiClient, LOGS, CapabilityApi.FILTER_REACHABLE)
                .setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                        @Override
                        public void onResult(CapabilityApi.GetCapabilityResult getCapabilityResult) {
                                updateTranscriptionCapability(getCapabilityResult.getCapability());

                        }
                });


    }

    private void updateTranscriptionCapability(CapabilityInfo capabilityInfo) {

        Set<Node> connectedNodes = capabilityInfo.getNodes();
        transcriptionNodeId = pickBestNodeId(connectedNodes);
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private void requestData(byte[] data, String dataType) {
        if (transcriptionNodeId != null) {
            Wearable.MessageApi.sendMessage(gApiClient, transcriptionNodeId, dataType, data)
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.d(TAG, "on sendMessage Result " + sendMessageResult.getStatus().getStatusMessage());

                        }
                    });
        } else {
            // Unable to retrieve node with transcription capability
            Log.d(TAG, "Unable to retrieve node with transcription capability");
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        gApiClient.connect();
        if (updateScreenShotTask == null ) {
            updateScreenShotTask = new UpdateScreenShotTask();
            Timer t = new Timer();
            t.schedule(updateScreenShotTask, 0, 1000);
        }

    }

    private UpdateScreenShotTask updateScreenShotTask;



    private class UpdateScreenShotTask extends TimerTask {
        @Override
        public void run() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//					System.out.println("Here's the tree:\n" + ViewUtils.printViewTree(activity.getWindow().getDecorView().getRootView()));

                    long start = System.currentTimeMillis();
                    View v1 = getWindow().getDecorView().getRootView();
                    takeScreenshot(v1);
                    long diff2 = System.currentTimeMillis() - start;

                    if (diff2 >= 100) {
                        Log.d(TAG, "Screen capture took " + diff2 + " ms on UI thread");
                    }
                }
            });
        }
    }


    public void onNewScreenshot(Bitmap screenShot) {

        if (screenShot == null) {
            Log.d(TAG, "onNewScreenshot screenShot is null");
            return;
        }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            screenShot.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

        Log.d(TAG, "NewScreenshot size = " + byteArray.length);
        requestData(byteArray, SCREENSHOTS);
    }


    public void takeScreenshot(View v1) {
        Bitmap bitmap;
        try {
            Log.v(TAG, "takeScreenshot width="+v1.getWidth() + " height="+v1.getHeight());
            if (v1.getWidth() <= 0 || v1.getHeight() <= 0) {
                // failed
                onNewScreenshot(null);
                return;
            }
            // allocate bitmap
            try {
                bitmap = Bitmap.createBitmap(v1.getWidth(), v1.getHeight(), Bitmap.Config.ARGB_8888);
                Log.v(TAG, "Screenshot bitmap allocated: " + v1.getWidth() + "x" + v1.getHeight());

            } catch (OutOfMemoryError oome) {
                    // oh oh!
                    Log.e(TAG, ScreenCapture.COULD_NOT_ALLOCATE_SCREENSHOT_BITMAP_OUT_OF_MEMORY_ERROR);
                    bitmap = null;
                    onNewScreenshot(null);
                    return;
            }

            Canvas c = new Canvas(bitmap);
            v1.draw(c);

            // send callback
            onNewScreenshot(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
            onNewScreenshot(null);
        }
    }

}
