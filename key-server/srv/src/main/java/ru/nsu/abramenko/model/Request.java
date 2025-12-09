package ru.nsu.abramenko.model;

import ru.nsu.abramenko.model.KeyPairWithCert;

import java.net.Socket;
import java.util.concurrent.CompletableFuture;
public record Request(String name, Socket socket, CompletableFuture<KeyPairWithCert> future) {
}
