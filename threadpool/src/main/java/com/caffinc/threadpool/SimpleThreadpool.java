package com.caffinc.threadpool;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple Thread Pool
 *
 * @author Sriram
 */
public class SimpleThreadpool {
    private ConcurrentLinkedQueue<Runnable> runnables;
    private AtomicBoolean execute;
    private List<SimpleThreadpoolThread> threads;


    private class ExecutionException extends RuntimeException {
        public ExecutionException(Throwable cause) {
            super(cause);
        }
    }

    private class SimpleThreadpoolThread extends Thread {
        private AtomicBoolean execute;
        private ConcurrentLinkedQueue<Runnable> runnables;

        public SimpleThreadpoolThread(String name, AtomicBoolean execute, ConcurrentLinkedQueue<Runnable> runnables) {
            super(name);
            this.execute = execute;
            this.runnables = runnables;
        }

        @Override
        public void run() {
            try {
                while (execute.get() || !runnables.isEmpty()) {
                    Runnable runnable;
                    while ((runnable = runnables.poll()) != null) {
                        runnable.run();
                    }
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }
    }

    private SimpleThreadpool(int threadCount) {
        this.runnables = new ConcurrentLinkedQueue<>();
        this.execute = new AtomicBoolean(true);
        this.threads = new ArrayList<>();
        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            SimpleThreadpoolThread thread = new SimpleThreadpoolThread("SimpleThreadpoolThread" + threadIndex, this.execute, this.runnables);
            thread.start();
            this.threads.add(thread);
        }
    }

    public static SimpleThreadpool getInstance() {
        return getInstance(Runtime.getRuntime().availableProcessors());
    }

    public static SimpleThreadpool getInstance(int threadCount) {
        return new SimpleThreadpool(threadCount);
    }

    public void execute(Runnable runnable) {
        if (this.execute.get()) {
            runnables.add(runnable);
        } else {
            throw new IllegalStateException("Threadpool terminating, unable to execute runnable");
        }
    }

    public void awaitTermination(long timeout) throws TimeoutException {
        if (this.execute.get()) {
            throw new IllegalStateException("Threadpool not terminated before awaiting termination");
        }
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime <= timeout) {
            boolean flag = true;
            for (Thread thread : threads) {
                if (thread.isAlive()) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                return;
            }
        }
        throw new TimeoutException("Unable to terminate threadpool within the specified timeout (" + timeout + "ms)");
    }

    public void awaitTermination() throws TimeoutException {
        if (this.execute.get()) {
            throw new IllegalStateException("Threadpool not terminated before awaiting termination");
        }
        while (true) {
            boolean flag = true;
            for (Thread thread : threads) {
                if (thread.isAlive()) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                return;
            }
        }
    }

    public void terminate() {
        runnables.clear();
        stop();
    }

    public void stop() {
        execute.set(false);
    }
}
