package ru.nsu.abramenko.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.nsu.abramenko.model.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.cert.CertificateEncodingException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@AllArgsConstructor
public class Sender implements Runnable {
    private final SynchronousQueue<Response> responses;
    private final AtomicBoolean isRunning;
    @SneakyThrows
    @Override
    public void run() {
        while (isRunning.get()) {
            Response res = responses.take();
            //log.debug("Processing response for name: {}", res.keyPairWithCert().name());
            Thread.ofVirtual().start(() -> {
                try (Socket socket = res.socket();
                     OutputStream output = socket.getOutputStream()) {
                    res.keyPairWithCert().writeToStream(output);
                    //log.debug("processed for name: {}", res.keyPairWithCert().name());
                } catch (IOException | CertificateEncodingException e) {
                    log.error("Error processing response: " + e.getMessage());
                }
            });
        }
    }
}
