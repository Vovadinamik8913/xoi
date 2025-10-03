package ru.nsu.abramenko.own;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static int MAXLEN = 80;

    public static void main(String[] args) throws IOException {
        var list = new NodeList<String>();
        var isRunning = new AtomicBoolean(true);
        int n = 3;
        for (int i = 0; i < n; i++) {
            Thread.ofVirtual().start(
                    new Sorter(
                            list.getHead(), isRunning
                    )
            );
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            char[] buffer = new char[MAXLEN];

            while (true) {
                int bytesRead = reader.read(buffer, 0, MAXLEN);

                if (bytesRead == -1) {
                    break;
                }

                String input = new String(buffer, 0, bytesRead);

                if (input.endsWith("\n")) {
                    input = input.substring(0, input.length() - 1);
                    if (input.endsWith("\r")) {
                        input = input.substring(0, input.length() - 1);
                    }
                }

                if (!input.isEmpty()) {
                    list.addNode(input);
                } else {
                    for(var elem : list) {
                        System.out.println(elem);
                    }
                }
            }
        }
    }
}
