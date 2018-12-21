package com.google.audioworker.utils.communicate.base;

public interface Exchangeable<I, T> {
    public void send(I to, T msg);
    public boolean isConnected();

    interface ExchangeListener<I, T> {
        public void postSendData(int ret, T msg);
        public void onDataReceived(I from, T msg);
    }
}
