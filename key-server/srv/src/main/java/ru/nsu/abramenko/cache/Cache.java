package ru.nsu.abramenko.cache;


import ru.nsu.abramenko.model.KeyPairWithCert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class Cache {
    private ConcurrentMap<String, CompletableFuture<KeyPairWithCert>> data;

    public Cache() {
        data = new ConcurrentHashMap<>();
    }

    public CompletableFuture<KeyPairWithCert> computeIfAbsent(String name,
                                                              Function<String, CompletableFuture<KeyPairWithCert>> mappingFunction) {
        return data.computeIfAbsent(name, mappingFunction);
    }

    public CompletableFuture<KeyPairWithCert> get(String name) {
        return data.get(name);
    }
}