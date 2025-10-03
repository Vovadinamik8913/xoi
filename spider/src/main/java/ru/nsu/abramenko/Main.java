package ru.nsu.abramenko;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.Collections;

public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        var messages = Collections.synchronizedList(new ArrayList<String>());
        var spider = new Spider("localhost", 8080);
        var init = Thread.ofVirtual()
                .start( new Find("/", messages, spider));
        init.join();
        messages.sort(String::compareTo);
        for (var message : messages) {
            System.out.println(message);
        }
    }
}
