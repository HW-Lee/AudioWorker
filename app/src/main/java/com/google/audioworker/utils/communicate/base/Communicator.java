package com.google.audioworker.utils.communicate.base;

import java.util.Collection;

public interface Communicator {
    class PeerInfo {
        public String name;
        public String status;
    }

    interface CommunicatorListener {
        public void onPeerInfoUpdated(Collection<PeerInfo> peers);
    }
}
