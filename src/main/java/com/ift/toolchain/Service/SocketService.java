package com.ift.toolchain.Service;

import org.springframework.stereotype.Service;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@Service
public class SocketService {

    public void sendDataToSocket(String ip, int port, String message) throws IOException {

        Socket chainSocket = new Socket(ip, port);
        DataOutputStream out = new DataOutputStream(chainSocket.getOutputStream());
        out.writeBytes(message);
        out.flush();
        out.close();
        chainSocket.close();
    }
}
