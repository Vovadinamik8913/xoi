package ru.nsu.abramenko.client;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;

@Slf4j
@RequiredArgsConstructor
public class KeyClient implements Runnable {
    private final String serverAddress;
    private final int serverPort;
    private final String name;
    private final int sendDelaySeconds;
    private final int readDelaySeconds;
    private final boolean exitBeforeReading;

    @SneakyThrows
    @Override
    public void run() {
        try (Socket socket = new Socket(serverAddress, serverPort);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            if (sendDelaySeconds > 0) {
                log.info("Waiting for " + sendDelaySeconds + " seconds before sending request...");
                Thread.sleep(sendDelaySeconds * 1000);
            }

            dos.write(name.getBytes("ASCII"));
            dos.writeByte(0);
            dos.flush();
            log.info("Sent request for name: " + name);

            if (exitBeforeReading) {
                log.info("Exiting before reading response (simulating crash)");
                return;
            }

            if (readDelaySeconds > 0) {
                log.info("Waiting for " + readDelaySeconds + " seconds before reading response...");
                Thread.sleep(readDelaySeconds * 1000);
            }
            int privateKeyLength = dis.readInt();
            byte[] privateKeyBytes = new byte[privateKeyLength];
            dis.readFully(privateKeyBytes);

            if (readDelaySeconds > 0) {
                log.info("Waiting for " + readDelaySeconds + " seconds before reading response...");
                Thread.sleep(readDelaySeconds * 1000);
            }
            int certLength = dis.readInt();
            byte[] certBytes = new byte[certLength];
            dis.readFully(certBytes);

            saveKeyAndCertificate(privateKeyBytes, certBytes);
            log.info("Successfully received and saved key pair for: " + name);
        }
    }

    private void saveKeyAndCertificate(byte[] privateKeyBytes, byte[] certBytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream("test/" + name + ".key")) {
            fos.write(privateKeyBytes);
        }
        try (FileOutputStream fos = new FileOutputStream("test/" +name + ".crt")) {
            fos.write(certBytes);
        }
    }
}