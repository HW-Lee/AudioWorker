package com.google.audioworker.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.audioworker.R;
import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.communicate.base.Communicator;
import com.google.audioworker.views.PeerListAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class ConnectFragment extends WorkerFragment implements Communicator.CommunicatorListener {
    private final static String TAG = Constants.packageTag("ConnectFragment");

    private EditText mThisDeviceInfoText;
    private Button mConnectBtn;

    private ArrayList<Communicator.PeerInfo> mPeers;
    private ArrayList<String> mConnectedPeers;
    private String mSelectedReceiver;

    private PeerListAdapter mPeerListAdapter;
    private final Object lock = new Object();

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.connect_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initUI();
    }

    private void initUI() {
        if (mActivityRef.get() == null)
            return;

        mThisDeviceInfoText = mActivityRef.get().findViewById(R.id.this_device_info);
        mConnectBtn = mActivityRef.get().findViewById(R.id.connect_btn);
        mConnectBtn.setText("Connect");

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnecting();
            }
        });

        mPeers = new ArrayList<>(10);

        mConnectedPeers = new ArrayList<>(10);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivityRef.get(), android.R.layout.simple_list_item_1, android.R.id.text1, mConnectedPeers);
        ((Spinner) mActivityRef.get().findViewById(R.id.send_to_spinner)).setAdapter(adapter);
        ((Spinner) mActivityRef.get().findViewById(R.id.send_to_spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Selected Receiver: '" + mConnectedPeers.get(position) + "'");
                mSelectedReceiver = mConnectedPeers.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mActivityRef.get().findViewById(R.id.send_to_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = ((EditText) mActivityRef.get().findViewById(R.id.msg_text)).getText().toString();
                if (mSelectedReceiver != null) {
                    CommandHelper.Command cmd = null;
                    try {
                        cmd = new CommandHelper.Command(text);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (cmd != null)
                        text = cmd.toString();
                    mActivityRef.get().getCommunicator().send(mSelectedReceiver, text);
                }
                Toast.makeText(mActivityRef.get(), "send '" + text + "' to '" + mSelectedReceiver + "'", Toast.LENGTH_SHORT).show();
            }
        });

        mActivityRef.get().findViewById(R.id.refresh_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityRef.get().getCommunicator().refreshPeers();
            }
        });

        mPeerListAdapter = new PeerListAdapter(mActivityRef.get());
        ((ListView) mActivityRef.get().findViewById(R.id.peer_list)).setAdapter(mPeerListAdapter);

        if (mActivityRef.get().getCommunicator().getName() != null)
            updateThisDeviceInfo(mActivityRef.get().getCommunicator().getName());
    }

    @Override
    public void onPeerInfoUpdated(Collection<Communicator.PeerInfo> peers) {
        if (mActivityRef.get() == null)
            return;

        synchronized (lock) {
            mPeers.clear();
            mPeers.addAll(peers);
            mConnectedPeers.clear();
            mConnectedPeers.addAll(mActivityRef.get().getCommunicator().getConnected());
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
            ((ArrayAdapter) ((Spinner) mActivityRef.get().findViewById(R.id.send_to_spinner)).getAdapter()).notifyDataSetChanged();
            if (mConnectedPeers.size() > 0)
                mSelectedReceiver = mConnectedPeers.get(0);
        }
    }

    private void startConnecting() {
        if (mActivityRef.get() == null)
            return;

        for (String selectedPeer : mPeerListAdapter.getSelectedPeers()) {
            if (!mActivityRef.get().getCommunicator().isConnected(selectedPeer))
                mActivityRef.get().getCommunicator().connectTo(selectedPeer);
        }
    }

    public void updateThisDeviceInfo(String deviceInfo) {
        mThisDeviceInfoText.setText(deviceInfo);
    }
}
