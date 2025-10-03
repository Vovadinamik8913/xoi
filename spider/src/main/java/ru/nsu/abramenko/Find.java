package ru.nsu.abramenko;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class Find implements Runnable {
    private String path;
    private List<String> messages;
    private Spider spider;
    @SneakyThrows
    @Override
    public void run() {
        Response response = spider.get(path);
        if (response == null) {
            return;
        }
        messages.add(response.getMessage());
        var threads = new ArrayList<Thread>();
        for (String successor : response.getSuccessors()) {
            var thread = Thread.ofVirtual().start(new Find(successor, messages, spider));
            threads.add(thread);
        }
        for (var thread : threads) {
            thread.join();
        }
    }
}
