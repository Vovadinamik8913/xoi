package ru.nsu.abramenko;

import lombok.SneakyThrows;
import ru.nsu.abramenko.client.KeyClient;
import ru.nsu.abramenko.config.AppConfig;

import java.util.Random;
import java.util.UUID;

public class CliApp {
    @SneakyThrows
    public static void main(String[] args) {
        var config = AppConfig.loadConfig(
                ClassLoader.getSystemResource("application.yml").getPath()
        );

        for (int i = 0; i < config.getClient().count; i++) {
            String name;
            if (config.getClient().isRandomName) {
                name = UUID.randomUUID().toString();
            } else {
                name = config.getClient().name;
            }
            KeyClient client;
            if (config.getClient().isRandomDelay) {
                Random random = new Random();
                client = new KeyClient(
                        config.getServer().address, config.getServer().port, name,
                        random.nextInt(10), random.nextInt(10), false
                );
            } else {
                client = new KeyClient(
                        config.getServer().address, config.getServer().port, name,
                        config.getDelay().sendSeconds, config.getDelay().readSeconds, config.getDelay().exitBeforeReading
                );
            }
            Thread.ofVirtual().start(client);
        }
        while (true) {
            Thread.sleep(1000);
        }
    }
}
