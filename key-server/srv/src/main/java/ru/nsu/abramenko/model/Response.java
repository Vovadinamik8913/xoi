package ru.nsu.abramenko.model;

import ru.nsu.abramenko.model.KeyPairWithCert;

import java.net.Socket;

public record Response(Socket socket, KeyPairWithCert keyPairWithCert) {
}
