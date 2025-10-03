package ru.nsu.abramenko.own;

import lombok.AllArgsConstructor;
import ru.nsu.abramenko.own.Node;

import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class Sorter<T extends Comparable<T>> implements Runnable {
    private static final int TIMEOUT = 5;
    private static final int DELAY = 1;
    private Node<T> head;
    private AtomicBoolean isRunning;

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                Thread.sleep(TIMEOUT * 1000);
                boolean sorted = false;
                while (!sorted) {
                    sorted = true;
                    Node<T> prev = head;
                    while (prev != null) {
                        boolean flag = false;

                        prev.lock();
                        Node<T> cur0 = prev.getNext();
                        if (cur0 == null) {
                            prev.unlock();
                            break;
                        }

                        cur0.lock();
                        Node<T> next = cur0;
                        Node<T> cur1 = cur0.getNext();
                        if (cur1 == null) {
                            cur0.unlock();
                            prev.unlock();
                            break;
                        }
                        cur1.lock();
                        if (cur1.getData().compareTo(cur0.getData()) < 0) {
                            cur0.setNext(cur1.getNext());
                            prev.setNext(cur1);
                            cur1.setNext(cur0);
                            next = cur1;
                            sorted = false;
                            flag = true;
                        }
                        cur1.unlock();
                        cur0.unlock();
                        prev.unlock();

                        if (flag) {
                            Thread.sleep(DELAY * 1000);
                        }
                        prev = next;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
