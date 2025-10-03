package ru.nsu.abramenko.own;

import lombok.Getter;
import ru.nsu.abramenko.own.Node;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class NodeList<T extends Comparable<T>> implements Iterable<T> {
    @Getter
    private Node<T> head;

    public NodeList() {
        head = new Node<T>(null);
    }

    public void addNode(T data) {
        var newNode = new Node<T>(data);
        head.lock();
        newNode.setNext(head.getNext());
        head.setNext(newNode);
        head.unlock();
    }

    public class NodeIterator implements Iterator<T> {
        private Node<T> prev;
        private Node<T> current;
        private boolean hasNext;
        private boolean isLocked;

        public NodeIterator() {
            prev = head;
            prev.lock();
            current = prev.getNext();
            hasNext = (current != null);
            isLocked = true;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext) {
                throw new NoSuchElementException();
            }

            current.lock();
            var data = current.getData();
            Node<T> nextNode = current.getNext();

            // Освобождаем предыдущий узел
            prev.unlock();

            prev = current;
            current = nextNode;
            hasNext = (current != null);

            // Если больше нет элементов, освобождаем последний узел
            if (!hasNext && isLocked) {
                prev.unlock();
                isLocked = false;
            }

            return data;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new NodeIterator();
    }
}
