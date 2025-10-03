package ru.nsu.abramenko.standart;

import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class Sorter<T extends Comparable<T>> implements Runnable {
    private static final int TIMEOUT = 5;
    private static final int DELAY = 1;
    private List<T> list;
    private AtomicBoolean isRunning;

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                Thread.sleep(TIMEOUT * 1000);
                for (int j = 1; j < list.size(); j++) {
                    boolean isSorted = true;
                    for (int i = 0; i < list.size() - j; i++) {
                        if (list.get(i+1).compareTo(list.get(i)) < 0) {
                            Collections.swap(list, i, i+1);
                            isSorted = false;
                            Thread.sleep(DELAY * 1000);
                        }
                    }
                    if (isSorted) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
