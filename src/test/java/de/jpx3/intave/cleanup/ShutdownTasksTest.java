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

package de.jpx3.intave.cleanup;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShutdownTasksTest {

  @Test
  @SuppressWarnings("unchecked")
  void secondRunIsNoOpAndLateAddsAreIgnored() throws Exception {
    // snapshot static state so this test does not leak into other tests
    Field tasksField = ShutdownTasks.class.getDeclaredField("tasks");
    tasksField.setAccessible(true);
    Deque<Runnable> tasks = (Deque<Runnable>) tasksField.get(null);
    Field doneField = ShutdownTasks.class.getDeclaredField("done");
    doneField.setAccessible(true);
    List<Runnable> saved = new ArrayList<>(tasks);
    boolean savedDone = doneField.getBoolean(null);
    synchronized (tasks) {
      tasks.clear();
      doneField.setBoolean(null, false);
    }
    try {
      AtomicInteger counter = new AtomicInteger();
      ShutdownTasks.add(counter::incrementAndGet);
      ShutdownTasks.add(counter::incrementAndGet);

      ShutdownTasks.runAll();
      assertEquals(2, counter.get());

      // second pass must not rerun anything, see issue 204
      ShutdownTasks.runAll();
      assertEquals(2, counter.get());

      // registrations after shutdown never run again, they are ignored
      ShutdownTasks.add(counter::incrementAndGet);
      ShutdownTasks.runAll();
      assertEquals(2, counter.get());

      // reset re-arms the queue for the next lifecycle in this classloader
      ShutdownTasks.reset();
      ShutdownTasks.add(counter::incrementAndGet);
      ShutdownTasks.runAll();
      assertEquals(3, counter.get());
    } finally {
      // restore state for the rest of the suite
      synchronized (tasks) {
        tasks.clear();
        saved.forEach(tasks::offerLast);
        doneField.setBoolean(null, savedDone);
      }
    }
  }
}
