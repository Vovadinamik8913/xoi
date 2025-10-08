package ru.nsu.abramenko;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.Collections;

public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        var messages = Collections.synchronizedList(new ArrayList<String>());
        var spider = new Spider("localhost", 8080);
        var time = System.nanoTime();
        var init = Thread.ofVirtual()
                .start( new Find("/", messages, spider));
        init.join();
        var end = System.nanoTime();
        System.out.println("Time: " + (end - time) / 1_000_000_000 + " seconds");
        messages.sort(String::compareTo);
        System.out.println("Found " + messages.size() + " messages:");
        for (var message : messages) {
            System.out.println(message);
        }
    }
}
