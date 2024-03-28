package com.provoly.transfo.runner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Processor send complete to downstream if when future is ready
 * And send error to downstream if future throw an exception
 *
 * @param <T>
 * @param <U>
 */
public class MultiUntilThreadQuitOp<T, U> extends AbstractMultiOperator<T, U> {

    private final Future<?> thread;

    public MultiUntilThreadQuitOp(Multi<? extends T> upstream, Future<?> thread) {
        super(upstream);
        this.thread = thread;
    }

    @Override
    public void subscribe(MultiSubscriber<? super U> downstream) {
        if (downstream == null) {
            throw new NullPointerException("Subscriber is `null`");
        }
        upstream.subscribe().withSubscriber(new MultiUntilThreadQuitOp.UntilQuitProcessor<T, U>(downstream, thread));
    }

    public static class UntilQuitProcessor<I, O> extends MultiOperatorProcessor<I, O> {

        public UntilQuitProcessor(MultiSubscriber downstream, Future<?> future) {
            super(downstream);
            new Thread(() -> waitProcessAndCancel(future), "wait-process-end").start();
        }

        private void waitProcessAndCancel(Future<?> future) {
            try {
                future.get();
                // If process is already finished, complete is sent to downstream even before
                // last messages from upstream are transfert to downstream.
                // Let time to kafkaCompanion to do its jobs.
                Thread.sleep(200);
                downstream.onComplete();
            } catch (InterruptedException | ExecutionException e) {
                downstream.onError(e);
                throw new IllegalStateException(e);
            }

        }

    }

}
