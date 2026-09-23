/*
 * Copyright 2026 Intave
 *
 * This software is licensed under the PolyForm Perimeter License 1.0.0.
 * You may use this software for any purpose, except for providing to
 * others any product that competes with the software.
 *
 * A copy of the license is available at:
 *   https://polyformproject.org/licenses/perimeter/1.0.0/
 */

package de.jpx3.intave.share.opt;


import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public final class ElasticRingDeque<T> extends AbstractCollection<T> implements Deque<T> {
	private static final int DEFAULT_INITIAL_CAPACITY = 16;
	private static final int MAX_CAPACITY = 32 * 1024;

	private final ReentrantLock lock = new ReentrantLock();

	private Object[] elements;
	private final int minCapacity;

	// Index of the first element.
	private int head;

	// Number of stored elements.
	private int size;

	public ElasticRingDeque() {
		this(DEFAULT_INITIAL_CAPACITY);
	}

	public ElasticRingDeque(int initialCapacity) {
		if (initialCapacity <= 0 || initialCapacity > MAX_CAPACITY) {
			throw new IllegalArgumentException("Initial capacity must be between 1 and " + MAX_CAPACITY);
		}
		this.minCapacity = nextPowerOfTwo(initialCapacity);
		this.elements = new Object[minCapacity];
	}

	private boolean insertFirst(T value) {
		Objects.requireNonNull(value);

		lock.lock();
		try {
			if (!ensureCapacityForAdd()) {
				return false;
			}

			head = decrement(head);
			elements[head] = value;
			size++;

			return true;
		} finally {
			lock.unlock();
		}
	}

	private boolean insertLast(T value) {
		Objects.requireNonNull(value);

		lock.lock();
		try {
			if (!ensureCapacityForAdd()) {
				return false;
			}

			int index = index(size);
			elements[index] = value;
			size++;

			return true;
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public T removeFirst() {
		lock.lock();
		try {
			if (size == 0) {
				throw new NoSuchElementException("Deque is empty");
			}

			T value = (T) elements[head];

			elements[head] = null;
			head = increment(head);
			size--;

			shrinkIfNeeded();

			return value;
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public T removeLast() {
		lock.lock();
		try {
			if (size == 0) {
				throw new NoSuchElementException("Deque is empty");
			}

			int index = index(size - 1);

			T value = (T) elements[index];
			elements[index] = null;
			size--;

			shrinkIfNeeded();

			return value;
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public T peekFirst() {
		lock.lock();
		try {
			if (size == 0) {
				return null;
			}

			return (T) elements[head];
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public T peekLast() {
		lock.lock();
		try {
			if (size == 0) {
				return null;
			}

			return (T) elements[index(size - 1)];
		} finally {
			lock.unlock();
		}
	}

	public int size() {
		lock.lock();
		try {
			return size;
		} finally {
			lock.unlock();
		}
	}

	public int capacity() {
		lock.lock();
		try {
			return elements.length;
		} finally {
			lock.unlock();
		}
	}

	public boolean isEmpty() {
		lock.lock();
		try {
			return size == 0;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean add(T value) {
		if (!offerLast(value)) {
			throw new IllegalStateException("Deque is full");
		}
		return true;
	}

	@Override
	public void addFirst(T value) {
		if (!offerFirst(value)) throw new IllegalStateException("Deque is full");
	}

	@Override
	public void addLast(T value) {
		if (!offerLast(value)) throw new IllegalStateException("Deque is full");
	}

	@Override
	public boolean offerFirst(T value) {
		return insertFirst(value);
	}

	@Override
	public boolean offerLast(T value) {
		return insertLast(value);
	}

	@Override
	public T pollFirst() {
		return isEmpty() ? null : removeFirst();
	}

	@Override
	public T pollLast() {
		return isEmpty() ? null : removeLast();
	}

	@Override
	public T getFirst() {
		T value = peekFirst();
		if (value == null) throw new NoSuchElementException("Deque is empty");
		return value;
	}

	@Override
	public T getLast() {
		T value = peekLast();
		if (value == null) throw new NoSuchElementException("Deque is empty");
		return value;
	}

	@Override
	public T element() {
		return getFirst();
	}

	@Override
	public boolean offer(T value) {
		return offerLast(value);
	}

	@Override
	public T poll() {
		return pollFirst();
	}

	@Override
	public T remove() {
		return removeFirst();
	}

	@Override
	public T peek() {
		return peekFirst();
	}

	@Override
	public void push(T value) {
		if (!offerFirst(value)) {
			throw new IllegalStateException("Deque is full");
		}
	}

	@Override
	public T pop() {
		return removeFirst();
	}

	@Override
	public boolean removeFirstOccurrence(Object value) {
		return removeOccurrence(value, false);
	}

	@Override
	public boolean removeLastOccurrence(Object value) {
		return removeOccurrence(value, true);
	}

	@Override
	public boolean remove(Object value) {
		return removeFirstOccurrence(value);
	}

	private boolean removeOccurrence(Object value, boolean reverse) {
		lock.lock();
		try {
			for (int i = reverse ? size - 1 : 0; reverse ? i >= 0 : i < size; i += reverse ? -1 : 1) {
				if (Objects.equals(value, elements[index(i)])) {
					for (int j = i; j < size - 1; j++) {
						elements[index(j)] = elements[index(j + 1)];
					}
					elements[index(size - 1)] = null;
					size--;
					shrinkIfNeeded();
					return true;
				}
			}
			return false;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean contains(Object value) {
		lock.lock();
		try {
			for (int i = 0; i < size; i++) {
				if (Objects.equals(value, elements[index(i)])) return true;
			}
			return false;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void clear() {
		lock.lock();
		try {
			Arrays.fill(elements, null);
			head = 0;
			size = 0;
			shrinkIfNeeded();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public @NonNull Iterator<T> iterator() {
		return snapshotIterator(false);
	}

	@Override
	public @NonNull Iterator<T> descendingIterator() {
		return snapshotIterator(true);
	}

	private @NonNull Iterator<T> snapshotIterator(boolean reverse) {
		return new Iterator<T>() {
			private int cursor;

			@Override
			public boolean hasNext() {
				lock.lock();
				try {
					return cursor < size;
				} finally {
					lock.unlock();
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public T next() {
				lock.lock();
				try {
					if (cursor >= size) throw new NoSuchElementException();
					int offset = reverse ? size - 1 - cursor : cursor;
					cursor++;
					return (T) elements[index(offset)];
				} finally {
					lock.unlock();
				}
			}
		};
	}

	public boolean isFull() {
		lock.lock();
		try {
			return size == MAX_CAPACITY;
		} finally {
			lock.unlock();
		}
	}

	/*
	 * Returns false only when we have reached the hard 32K limit.
	 */
	private boolean ensureCapacityForAdd() {
		if (size < elements.length) {
			return true;
		}
		if (elements.length == MAX_CAPACITY) {
			return false;
		}
		int newCapacity = Math.min(elements.length << 1, MAX_CAPACITY);
		resize(newCapacity);
		return true;
	}

	/*
	 * Shrink when we're using at most 25% of the array.
	 *
	 * The 25% threshold prevents constant grow/shrink cycles when
	 * the size fluctuates around 50%.
	 */
	private void shrinkIfNeeded() {
		int capacity = elements.length;
		if (capacity <= minCapacity) {
			return;
		}
		if (size <= capacity / 4) {
			int newCapacity = Math.max(minCapacity, capacity / 2);
			resize(newCapacity);
		}
	}

	private void resize(int newCapacity) {
		Object[] newElements = new Object[newCapacity];
		for (int i = 0; i < size; i++) {
			newElements[i] = elements[index(i)];
		}
		elements = newElements;
		head = 0;
	}

	/*
	 * Convert a logical offset from the head into a physical
	 * array index.
	 *
	 * Since capacity is always a power of two:
	 *
	 *     x % capacity
	 *
	 * can be replaced with:
	 *
	 *     x & (capacity - 1)
	 */
	private int index(int offset) {
		return (head + offset) & (elements.length - 1);
	}

	private int increment(int index) {
		return (index + 1) & (elements.length - 1);
	}

	private int decrement(int index) {
		return (index - 1) & (elements.length - 1);
	}

	private static int nextPowerOfTwo(int value) {
		int n = 1;
		while (n < value) {
			n <<= 1;
		}
		return Math.min(n, MAX_CAPACITY);
	}
}
