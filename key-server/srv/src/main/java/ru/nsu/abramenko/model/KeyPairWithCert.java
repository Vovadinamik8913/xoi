package ru.nsu.abramenko.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public record KeyPairWithCert(String name, KeyPair keyPair, X509Certificate certificate) {
    public void writeToStream(OutputStream output) throws IOException, CertificateEncodingException {
        DataOutputStream dos = new DataOutputStream(output);

        //dos.write(name.getBytes("ASCII"));
        //dos.writeByte(0);

        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        dos.writeInt(privateKeyBytes.length);
        dos.write(privateKeyBytes);

        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        dos.writeInt(publicKeyBytes.length);
        dos.write(publicKeyBytes);

        byte[] certBytes = certificate.getEncoded();
        dos.writeInt(certBytes.length);
        dos.write(certBytes);

        dos.flush();
    }
}
