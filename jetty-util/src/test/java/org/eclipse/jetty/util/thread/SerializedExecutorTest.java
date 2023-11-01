//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.util.thread;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SerializedExecutorTest
{
    @Test
    public void test() throws Exception
    {
        int threads = 64;
        int loops = 1000;
        int depth = 100;

        AtomicInteger ran = new AtomicInteger();
        AtomicBoolean running = new AtomicBoolean();
        SerializedExecutor executor = new SerializedExecutor();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch stop = new CountDownLatch(threads);
        Random random = new Random();

        for (int t = threads; t-- > 0; )
        {
            new Thread(() ->
            {
                try
                {
                    start.await();

                    for (int l = loops; l-- > 0; )
                    {
                        final AtomicInteger d = new AtomicInteger(depth);
                        executor.execute(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                ran.incrementAndGet();
                                if (!running.compareAndSet(false, true))
                                    throw new IllegalStateException();
                                if (d.decrementAndGet() > 0)
                                    executor.execute(this);
                                if (!running.compareAndSet(true, false))
                                    throw new IllegalStateException();
                            }
                        });
                        Thread.sleep(random.nextInt(5));
                    }
                }
                catch (Throwable th)
                {
                    th.printStackTrace();
                }
                finally
                {
                    stop.countDown();
                }
            }).start();
        }

        start.countDown();
        assertTrue(stop.await(30, TimeUnit.SECONDS));
        assertThat(ran.get(), Matchers.is(threads * loops * depth));
    }
}
