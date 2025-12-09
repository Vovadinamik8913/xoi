package ru.nsu.abramenko;

import org.bouncycastle.asn1.x500.X500Name;
import ru.nsu.abramenko.config.AppConfig;
import ru.nsu.abramenko.model.Request;
import ru.nsu.abramenko.model.Response;
import ru.nsu.abramenko.cache.Cache;
import ru.nsu.abramenko.service.Generator;
import ru.nsu.abramenko.service.Sender;
import ru.nsu.abramenko.service.Server;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SrvApp {
    public static void main(String[] args) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        var cache = new Cache();
        BlockingQueue<Request> requests = new LinkedBlockingQueue<>();
        var responses = new SynchronousQueue<Response>();
        var isRunning = new AtomicBoolean(true);

        var config = AppConfig.loadConfig(
                ClassLoader.getSystemResource("application.yml").getPath()
        );

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(config.getCertificate().caKeyFile)) {
            keyStore.load(fis, config.getCertificate().caKeyPassword.toCharArray());
        }

        String alias = keyStore.aliases().nextElement();
        var caPrivateKey = (PrivateKey) keyStore.getKey(alias, config.getCertificate().caKeyPassword.toCharArray());
        var issuer = new X500Name(config.getCertificate().issuerName);

        Thread sender = new Thread(
                new Sender(responses, isRunning)
        );
        sender.start();

        Thread gen = new Thread(
                new Generator(
                        requests, issuer, caPrivateKey,
                        cache, isRunning
                )
        );
        gen.start();

        var server = new Server(config.getServer().port, isRunning, cache, requests, responses);
        server.start();
    }
}
