package com.example.giltsl.wachtest2;

import android.app.Activity;
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

import java.util.Set;

public class WachMainActivity extends Activity {

    final String TAG = getClass().getName();

    private TextView mTextView;
    static int count  = 0 ;

    private String transcriptionNodeId = null;
    private static final String DATA_TRANSCRIPTION_CAPABILITY_NAME = "data_transcription";

    GoogleApiClient gApiClient;
    private boolean connected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wach_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String dataString = "Hello Gil " + count++;

                        ((TextView) v).setText(dataString);
                        if (connected) {
                            Log.d(TAG, "I am connected, going to send data");
                            //send
                            requestData(dataString.getBytes());
                        } else {

                            Log.w(TAG, "Not connected, cant send the data, it will be lost ;(");git 
                        }
                    }
                });

            }
        });

        gApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "onConnected bundle = " + bundle.toString());
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
    }

    private void setupDataTranscription() {
        CapabilityApi.GetCapabilityResult result = Wearable
                .CapabilityApi
                .getCapability(gApiClient, DATA_TRANSCRIPTION_CAPABILITY_NAME, CapabilityApi.FILTER_REACHABLE)
                .await();

        updateTranscriptionCapability(result.getCapability());
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

    private void requestData(byte[] data) {
        if (transcriptionNodeId != null) {
            Wearable.MessageApi.sendMessage(gApiClient, transcriptionNodeId,  DATA_TRANSCRIPTION_CAPABILITY_NAME, data)
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.d(TAG, "on sendMessage Result " + sendMessageResult.toString());

                        }
                    });
        } else {
            // Unable to retrieve node with transcription capability
            Log.d(TAG, "Unable to retrieve node with transcription capability");
        }
    }
}
