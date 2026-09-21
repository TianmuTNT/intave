package de.jpx3.intave.cleanup;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ShutdownTasks {
  private static final Deque<Runnable> tasks = new ArrayDeque<>();
  private static boolean done = false;

  private ShutdownTasks() {
    throw new UnsupportedOperationException("Initialization of helper class");
  }

  public static void add(Runnable runnable) {
    if (runnable == null) {
      throw new NullPointerException("Null shutdown task");
    }
    synchronized (tasks) {
      // late registrations would never run again, ignore them with a warning
      if (done) {
        System.out.println("[Intave] shutdown task added after shutdown, ignoring " + runnable);
        return;
      }
      tasks.offerLast(runnable);
    }
  }

  public static void addBeforeAll(Runnable runnable) {
    if (runnable == null) {
      throw new NullPointerException("Null shutdown task");
    }
    synchronized (tasks) {
      // late registrations would never run again, ignore them with a warning
      if (done) {
        System.out.println("[Intave] shutdown task added after shutdown, ignoring " + runnable);
        return;
      }
      tasks.offerFirst(runnable);
    }
  }

  public static void runAll() {
    List<Runnable> pending;
    synchronized (tasks) {
      // mark done before draining so a second pass is a no-op
      if (done) {
        return;
      }
      done = true;
      pending = new ArrayList<>(tasks);
      tasks.clear();
    }
    for (Runnable task : pending) {
      try {
        task.run();
      } catch (Throwable throwable) {
        // one failing task must not cancel the remaining shutdown tasks
        System.out.println("[Intave] Shutdown task " + task + " failed to complete");
        throwable.printStackTrace();
      }
    }
  }
}
