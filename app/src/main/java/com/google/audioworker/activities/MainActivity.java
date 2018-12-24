package com.google.audioworker.activities;

import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.audioworker.R;
import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.MainController;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.communicate.base.Communicable;
import com.google.audioworker.utils.communicate.base.Communicator;
import com.google.audioworker.utils.communicate.base.Exchangeable;
import com.google.audioworker.utils.communicate.wifip2p.WifiCommunicator;
import com.google.audioworker.views.PeerListAdapter;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity
        implements Exchangeable.ExchangeListener<String, String>, Communicator.CommunicatorListener,
            CommandHelper.BroadcastHandler.FunctionReceivedListener, WorkerFunction.WorkerFunctionListener {
    private final static String TAG = Constants.packageTag("MainActivity");

    private Communicable<String, String> mCommunicator;

    private EditText mThisDeviceInfoText;
    private PeerListAdapter mPeerListAdapter;
    private final Object lock = new Object();
    private Button mConnectBtn;

    private boolean isRunning = false;

    private ArrayList<Communicator.PeerInfo> mPeers;
    private ArrayList<String> mConnectedPeers;
    private String mSelectedReceiver;

    private MainController mMainController;
    private CommandHelper.BroadcastHandler mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPermissionCheck();
        initUI();
        initControllers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCommunicator.notifyOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCommunicator.notifyOnPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CommandHelper.BroadcastHandler.unregisterReceiver(this, mBroadcastReceiver);
        mMainController.destroy();
    }

    private void initPermissionCheck() {
        for (String permission : Constants.PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS_REQUIRED, 1);
                break;
            }
        }
    }

    private void initUI() {
        mThisDeviceInfoText = findViewById(R.id.this_device_info);
        CheckBox cb = findViewById(R.id.master_select);

        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    findViewById(R.id.peer_list).setVisibility(View.VISIBLE);
                    findViewById(R.id.connect_btn).setVisibility(View.VISIBLE);
                    findViewById(R.id.connect_btn).setClickable(true);
                } else {
                    findViewById(R.id.peer_list).setVisibility(View.INVISIBLE);
                    findViewById(R.id.connect_btn).setVisibility(View.INVISIBLE);
                    findViewById(R.id.connect_btn).setClickable(false);
                }
            }
        });

        mPeers = new ArrayList<>(10);

        mConnectBtn = findViewById(R.id.connect_btn);
        mConnectBtn.setText("Connect");

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnecting();
            }
        });

        mPeerListAdapter = new PeerListAdapter(this);
        ((ListView) findViewById(R.id.peer_list)).setAdapter(mPeerListAdapter);

        mConnectedPeers = new ArrayList<>(10);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mConnectedPeers);
        ((Spinner) findViewById(R.id.send_to_spinner)).setAdapter(adapter);
        ((Spinner) findViewById(R.id.send_to_spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Selected Receiver: '" + mConnectedPeers.get(position) + "'");
                mSelectedReceiver = mConnectedPeers.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        findViewById(R.id.send_to_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = ((EditText) findViewById(R.id.msg_text)).getText().toString();
                if (mSelectedReceiver != null) {
                    CommandHelper.Command cmd = null;
                    try {
                        cmd = new CommandHelper.Command(text);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (cmd != null)
                        text = cmd.toString();
                    mCommunicator.send(mSelectedReceiver, text);
                }
                Toast.makeText(MainActivity.this, "send '" + text + "' to '" + mSelectedReceiver + "'", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.refresh_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCommunicator.refreshPeers();
            }
        });
    }

    private void initControllers() {
        mCommunicator = new WifiCommunicator(this, this);
        mBroadcastReceiver = CommandHelper.BroadcastHandler.registerReceiver(this, this);
        mBroadcastReceiver.registerCommuncator(mCommunicator);
        mMainController = new MainController();
        mMainController.activate(this);
    }

    public void updateThisDeviceInfo(String deviceInfo) {
        mThisDeviceInfoText.setText(deviceInfo);
    }

    private void startConnecting() {
        for (String selectedPeer : mPeerListAdapter.getSelectedPeers()) {
            if (!mCommunicator.isConnected(selectedPeer))
                mCommunicator.connectTo(selectedPeer);
        }
    }

    private void startRunning() {
    }

    private void stopRunning() {
    }

    @Override
    public void postSendData(int ret, String msg) {
        Log.d(TAG, "postSendData('" + msg + "') returns " + ret);
        WorkerFunction function = CommandHelper.getFunction(msg);
        if (function != null) {
            mMainController.addRequestedFunction(function);
        }
    }

    @Override
    public void onDataReceived(final String from, final String msg) {
        Log.d(TAG, "onDataReceived('" + msg + "') from " + from);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "got msg '" + msg + "' from '" + from + "'", Toast.LENGTH_SHORT).show();
            }
        });

        WorkerFunction function = CommandHelper.getFunction(msg);
        if (function != null) {
            this.onFunctionReceived(function);
            return;
        }

        WorkerFunction.Ack ack = WorkerFunction.Ack.parseAck(msg);
        if (ack != null) {
            Log.d(TAG, "send ack to controller: " + ack);
            mMainController.receiveAck(ack);
        }
    }

    @Override
    public void onPeerInfoUpdated(Collection<Communicator.PeerInfo> peers) {
        synchronized (lock) {
            mPeers.clear();
            mPeers.addAll(peers);
            mConnectedPeers.clear();
            mConnectedPeers.addAll(mCommunicator.getConnected());
            Collections.sort(mPeers, new Comparator<Communicator.PeerInfo>() {
                @Override
                public int compare(Communicator.PeerInfo o1, Communicator.PeerInfo o2) {
                    if (o1.status.equals(o2.status))
                        return 0;
                    String[] statusList = {
                            "connected",
                            "invited",
                            "available",
                            "unavailable",
                            "failed"
                    };
                    for (String status : statusList) {
                        if (status.equals(o1.status))
                            return -1;
                        if (status.equals(o2.status))
                            return 1;
                    }
                    return 0;
                }
            });
            mPeerListAdapter.updatePeers(peers);
            ((ArrayAdapter) ((Spinner) findViewById(R.id.send_to_spinner)).getAdapter()).notifyDataSetChanged();
            if (mConnectedPeers.size() > 0)
                mSelectedReceiver = mConnectedPeers.get(0);
        }
    }

    @Override
    public void onFunctionReceived(WorkerFunction function) {
        Log.d(TAG, "onFunctionReceived(" + function + ")");
        mMainController.execute(function, this);
    }

    @Override
    public void onAckReceived(WorkerFunction.Ack ack) {
        Log.d(TAG, "onAckReceived(" + ack + ")");
        String sender;
        try {
            sender = ack.getString(Constants.MessageSpecification.COMMAND_ACK_TARGET);
            if (sender.contains("::")) {
                sender = sender.split("::")[0];
                mCommunicator.send(sender, ack.toString());
            } else {
                Log.w(TAG, "invalid target name: " + sender);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
