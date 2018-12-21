package com.google.audioworker.utils.communicate.base;

import java.util.Collection;

public interface Communicable<I, T> extends Exchangeable<I, T> {
    public void refreshPeers();
    public void notifyOnPause();
    public void notifyOnResume();
    public void connectTo(T obj);
    public boolean isConnected(T obj);
    public Collection<T> getConnected();
}
