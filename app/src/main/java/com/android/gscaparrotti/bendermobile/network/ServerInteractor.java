package com.android.gscaparrotti.bendermobile.network;

import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by gscap_000 on 01/05/2016.
 */
public class ServerInteractor {

    private Socket socket;

    public ServerInteractor() {
        socket = new Socket();
    }

    public Object sendCommandAndGetResult(final String address, final int port, final Object input) {
        Object datas;
        if (socket.isClosed()) {
            throw new IllegalStateException("You cannot use a ServerInteractor twice");
        }
        try {
            socket.connect(new InetSocketAddress(address, port), 5000);
            socket.setSoTimeout(5000);
            socket.setTcpNoDelay(true);
            final ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            final ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream.writeObject(input);
            datas = inputStream.readObject();
            socket.close();
        } catch (final IOException | ClassNotFoundException e) {
            try {
                socket.close();
                throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreSocketConChiusura));
            } catch (final IOException e1) {
                throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreSocketSenzaChiusura));
            }
        }
        return datas;
    }

}
