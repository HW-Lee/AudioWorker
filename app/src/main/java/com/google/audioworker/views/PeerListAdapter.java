package com.google.audioworker.views;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.communicate.base.Communicator;

import java.util.ArrayList;
import java.util.Collection;

public class PeerListAdapter extends BaseAdapter {
    private final static String TAG = Constants.packageTag("PeerListAdapter");

    class PeerConfigInfo extends Communicator.PeerInfo {
        boolean selected;

        PeerConfigInfo(String name, String status) {
            this.name = name;
            this.selected = false;
            this.status = status;
        }
    }

    private final ArrayList<PeerConfigInfo> mPeers = new ArrayList<>(10);
    private Context mContext;

    public PeerListAdapter(Context ctx) {
        mContext = ctx;
    }

    public void updatePeers(Collection<Communicator.PeerInfo> peers) {
        synchronized (mPeers) {
            ArrayList<String> mPeerNames = new ArrayList<>(10);
            for (int i = 0; i < mPeers.size(); i++)
                mPeerNames.add(mPeers.get(i).name);

            ArrayList<PeerConfigInfo> updatedList = new ArrayList<>(10);
            for (Communicator.PeerInfo peer : peers) {
                String name = peer.name;
                String status = peer.status;
                PeerConfigInfo info = new PeerConfigInfo(name, status);
                if (mPeerNames.contains(name)) {
                    int idx = mPeerNames.indexOf(name);
                    info.selected = mPeers.get(idx).selected;
                }
                updatedList.add(info);
            }

            mPeers.clear();
            mPeers.addAll(updatedList);
        }
        notifyDataSetChanged();
    }

    public Collection<String> getSelectedPeers() {
        ArrayList<String> selectedPeers = new ArrayList<>(10);
        for (PeerConfigInfo peer : mPeers) {
            if (peer.selected)
                selectedPeers.add(peer.name);
        }

        return selectedPeers;
    }

    @Override
    public int getCount() {
        return mPeers.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= mPeers.size())
            return null;

        PeerView v = new PeerView(mContext, mPeers.get(position));
        v.nameText.setText(mPeers.get(position).name + " [" + mPeers.get(position).status + "]");
        v.enableCheck.setSelected(mPeers.get(position).selected);
        v.enableCheck.setEnabled("available".equals(mPeers.get(position).status));

        LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) v.getLayoutParams();
        param.height = parent.getMeasuredHeight() / 4;
        param.width = parent.getMeasuredWidth();
        v.setLayoutParams(param);

        return v;
    }

    class PeerView extends LinearLayout {
        PeerConfigInfo mInfo;
        TextView nameText;
        CheckBox enableCheck;

        public PeerView(Context context, PeerConfigInfo info) {
            super(context);
            mInfo = info;

            setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            setOrientation(LinearLayout.HORIZONTAL);

            initChildViews(context);
        }

        private void initChildViews(Context ctx) {
            nameText = new TextView(ctx);
            nameText.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 4.0f));
            nameText.setGravity(Gravity.CENTER);
            enableCheck = new CheckBox(ctx);
            enableCheck.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f));
            enableCheck.setGravity(Gravity.CENTER);

            enableCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mInfo.selected = isChecked;
                }
            });

            addView(nameText);
            addView(enableCheck);
        }
    }
}
