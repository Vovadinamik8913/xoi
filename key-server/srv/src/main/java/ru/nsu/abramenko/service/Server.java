package ru.nsu.abramenko.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.nsu.abramenko.model.Request;
import ru.nsu.abramenko.model.Response;
import ru.nsu.abramenko.cache.Cache;
import ru.nsu.abramenko.model.KeyPairWithCert;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class Server {
    private final int port;
    private final AtomicBoolean isRunning;
    private ServerSocket serverSocket;
    private final Cache cache;
    private final BlockingQueue<Request> requests;
    private final SynchronousQueue<Response> responses;

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        log.info("Server started on port: {}", port);

        while (isRunning.get()) {
            Socket clientSocket = serverSocket.accept();
            Thread.ofVirtual().start(() -> handleClient(clientSocket));
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            InputStream input = clientSocket.getInputStream();
            String clientName = readNullTerminatedString(input);
            log.info("Received request for name: {}", clientName);

            CompletableFuture<KeyPairWithCert> future = cache.computeIfAbsent(clientName,
                    name -> {
                        CompletableFuture<KeyPairWithCert> newFuture = new CompletableFuture<>();
                        try {
                            requests.put(new Request(name, clientSocket, newFuture));
                            log.info("Request queued for name: {}", name);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            newFuture.completeExceptionally(e);
                        }
                        return newFuture;
                    });

            future.whenComplete((result, exception) -> {
                try {
                    if (exception != null) {
                        responses.put(new Response(clientSocket, null));
                    } else {
                        responses.put(new Response(clientSocket, result));
                    }
                    log.info("Response queued for name: {}", result.name());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

        } catch (Exception e) {
            log.error("Error handling client request", e);
        }
    }

    private String readNullTerminatedString(InputStream input) throws IOException {
        DataInputStream dis = new DataInputStream(input);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte b;
        while ((b = dis.readByte()) != 0) {
            buffer.write(b);
        }

        return buffer.toString("ASCII");
    }
}
