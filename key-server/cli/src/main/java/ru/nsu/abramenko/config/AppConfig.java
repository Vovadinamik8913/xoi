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
    private ClientConfig client;
    private DelayConfig delay;

    public static class ServerConfig {
        public String address;
        public int port;
    }

    public static class ClientConfig {
        public String name;
        public int count;
        public boolean isRandomDelay;
        public boolean isRandomName;
    }

    public static class DelayConfig {
        public int sendSeconds;
        public int readSeconds;
        public boolean exitBeforeReading;
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