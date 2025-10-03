package ru.nsu.abramenko.own;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@AllArgsConstructor
public class Node<T extends Comparable<T>> {
    @Getter @Setter
    private T data;
    @Getter @Setter
    private Node next;
    private Lock lock;

    public Node(T data) {
        this.data = data;
        next = null;
        lock = new ReentrantLock();
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

}
