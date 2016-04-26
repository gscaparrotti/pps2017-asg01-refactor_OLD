package com.android.gscaparrotti.bendermobile.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by gscap_000 on 01/05/2016.
 */
public class ServerInteractor {

    private Socket socket;

    public static ServerInteractor getInstance() {
        return new ServerInteractor();
    }

    private ServerInteractor() {
        socket = new Socket();
    }

    public Object sendCommandAndGetResult(final String address, final int port, final Object input) {
        Object datas;
        if (socket == null) {
            socket = new Socket();
        }
        if (!socket.isConnected()) {
            try {
                socket.connect(new InetSocketAddress(address, port), 5000);
                socket.setSoTimeout(5000);
            } catch (IOException e) {
                try {
                    datas = e;
                    interactionEnded();
                    return datas;
                } catch (Exception e1) {
                    datas = e1;
                    return datas;
                }
            }
        }
        try {
            final OutputStream os = socket.getOutputStream();
            final InputStream is = socket.getInputStream();
            final ObjectOutputStream outputStream = new ObjectOutputStream(os);
            outputStream.writeObject(input);
            final ObjectInputStream inputStream = new ObjectInputStream(is);
            datas = inputStream.readObject();
            interactionEnded(); //aggiunto dopo
        } catch (IOException | ClassNotFoundException e2) {
            try {
                interactionEnded();
                datas = e2;
                return datas;
            } catch (IOException e3) {
                datas = e3;
                return datas;
            }
        }
        return datas;
    }

    private void interactionEnded() throws IOException {
        if (socket != null) {
            socket.close();
        }
        socket = null;
    }

    private class ServerReceiver extends Thread {

        final ObjectInputStream ois;
        final boolean keepListening = true;

        private ServerReceiver(ObjectInputStream ois) {
            this.ois = ois;
        }

        @Override
        public void run() {
            super.run();
            while (keepListening) {
                try {
                    Object inputObject = ois.readObject();

                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
