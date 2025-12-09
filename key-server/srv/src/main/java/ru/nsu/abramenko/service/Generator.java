package ru.nsu.abramenko.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import ru.nsu.abramenko.model.KeyPairWithCert;
import ru.nsu.abramenko.model.Request;
import ru.nsu.abramenko.cache.Cache;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class Generator implements Runnable {
    private static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    private static final int KEY_SIZE = 8192;
    private final BlockingQueue<Request> requests;
    private final X500Name issuerName;
    private final PrivateKey caPrivateKey;
    private final Cache cache;
    private final AtomicBoolean isRunning;
    @SneakyThrows
    @Override
    public void run() {
        while (isRunning.get()) {
            var request = requests.take();
            Thread.ofVirtual().start(() -> {
                //log.debug("Processing request for name: {}", request.name());
                try {
                    var keyGen = KeyPairGenerator.getInstance("RSA");
                    keyGen.initialize(KEY_SIZE);
                    var keyPair = keyGen.generateKeyPair();
                    var cert = createCertificate(request.name(), keyPair);
                    var result = new KeyPairWithCert(request.name(), keyPair, cert);
                    //log.debug("result: {}", result);
                    request.future().complete(result);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            });
        }
    }
    private X509Certificate createCertificate(String subjectName, KeyPair keyPair)
            throws OperatorCreationException, CertificateException {
        var subject = new X500Name("CN=" + subjectName);
        var serial = BigInteger.valueOf(System.currentTimeMillis());
        var notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24);
        var notAfter = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365);
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerName, serial, notBefore, notAfter, subject, keyPair.getPublic()
        );
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .build(caPrivateKey);
        return new JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));
    }
}
