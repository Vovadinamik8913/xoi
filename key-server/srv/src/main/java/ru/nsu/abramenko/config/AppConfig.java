package ru.nsu.abramenko.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class AppConfig {
    private ServerConfig server;
    private ThreadingConfig threading;
    private CertificateConfig certificate;

    public static class ServerConfig {
        public int port;
    }

    public static class ThreadingConfig {
        public int generationThreads;
    }

    public static class CertificateConfig {
        public String caKeyFile;
        public String issuerName;
        public String caKeyPassword;
    }

    public static AppConfig loadConfig(String configFile) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(new File(configFile), AppConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}