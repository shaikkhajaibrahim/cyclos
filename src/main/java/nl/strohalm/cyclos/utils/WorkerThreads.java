/*
   This file is part of Cyclos.

   Cyclos is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   Cyclos is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Cyclos; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 */
package nl.strohalm.cyclos.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An abstract implementation for a thread group of consumer threads
 * @author luis
 */
public abstract class WorkerThreads<T> {

    private final class WorkerThread extends Thread {
        private long    lastUsedAt;
        private boolean inProcess;

        @Override
        public void run() {
            while (true) {
                try {
                    while (true) {
                        // Block until something is available
                        final T object = queue.take();

                        inProcess = true;

                        // Update the last used counter
                        lastUsedAt = System.currentTimeMillis();

                        // Process the object
                        process(object);

                        inProcess = false;
                    }
                } catch (final InterruptedException e) {
                    return;
                } catch (final Exception e) {
                    try {
                        LOG.error("Error processing work by " + name, e);
                    } catch (final Exception e1) {
                        // Ignore
                    }
                    inProcess = false;
                }
            }
        }
    }

    private static final long  CHECK_INTERVAL = DateUtils.MILLIS_PER_MINUTE;
    private static final Log   LOG            = LogFactory.getLog(WorkerThreads.class);

    private String             name;
    private List<WorkerThread> threads;
    private int                maxThreads;
    private BlockingQueue<T>   queue          = new LinkedBlockingQueue<T>();
    private long               threadIndex;
    private Timer              cleanUpTimer;

    protected WorkerThreads(final String name, final int maxThreads) {
        this.name = name;
        this.maxThreads = maxThreads;
        threads = Collections.synchronizedList(new LinkedList<WorkerThread>());
        cleanUpTimer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                interruptOldThreads();
            }
        };
        cleanUpTimer.scheduleAtFixedRate(task, CHECK_INTERVAL, CHECK_INTERVAL);
    }

    /**
     * Enqueues the given object to be processed
     */
    public synchronized void enqueue(final T object) {
        if (maxThreads <= 0) {
            return;
        }
        queue.offer(object);
        final int queueSize = queue.size();
        final int threadsSize = threads.size();
        if (threadsSize < maxThreads && threadsSize < queueSize) {
            // Start another thread
            final WorkerThread thread = new WorkerThread();
            thread.setName("#" + (threadIndex++) + " " + name);
            threads.add(thread);
            thread.start();
        }
    }

    /**
     * Interrupts all threads
     */
    public synchronized void interrupt() {
        cleanUpTimer.cancel();
        for (final WorkerThread thread : threads) {
            thread.interrupt();
        }
        threads.clear();
    }

    /**
     * Should be implemented in order to do the actual work with the given object
     */
    protected abstract void process(T object);

    private synchronized void interruptOldThreads() {
        final long tolerance = System.currentTimeMillis() - CHECK_INTERVAL;
        for (final Iterator<WorkerThread> iterator = threads.iterator(); iterator.hasNext();) {
            final WorkerThread thread = iterator.next();
            if (thread.lastUsedAt < tolerance && !thread.inProcess) {
                thread.interrupt();
                iterator.remove();
            }
        }

    }

}
