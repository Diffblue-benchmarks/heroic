/*
 * Copyright (c) 2015 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.heroic.async;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.Transform;

/**
 * XXX: consider replacing with RxJava at some point.
 *
 * @author udoprog
 * @param <T>
 */
public interface AsyncObserver<T> {
    AsyncFuture<Void> observe(final T value) throws Exception;

    void cancel() throws Exception;

    void fail(Throwable cause) throws Exception;

    void end() throws Exception;

    /**
     * Bind a given future as an observeable. It can also be considered as converting a future into
     * an observable.
     *
     * The end states of the observable will be passed on to the future.
     *
     * @param future
     * @param transform Transforms each observed action into a future. This is equivalent to the
     *            {@link #observe(Object)} method.
     * @return An observer bound to the given future.
     */
    static <T> AsyncObserver<T> bind(final ResolvableFuture<Void> future,
            final Transform<T, AsyncFuture<Void>> transform) {
        return new AsyncObserver<T>() {
            @Override
            public AsyncFuture<Void> observe(T value) throws Exception {
                return transform.transform(value);
            }

            @Override
            public void cancel() throws Exception {
                future.cancel();
            }

            @Override
            public void fail(Throwable cause) throws Exception {
                future.fail(cause);
            }

            @Override
            public void end() throws Exception {
                future.resolve(null);
            }
        };
    }

    /**
     * Bind a callback that will be fired if the given observer comes to an end-state.
     *
     * The callback will be called _before_ the nested observer's own callbacks will be fired.
     *
     * @param observer Observer to bind to.
     * @param callback Callback to fire.
     * @return A new observer that will call the given callback for any given end-state.
     */
    static <T> AsyncObserver<T> onFinished(final AsyncObserver<T> observer,
            final FutureFinished callback) {
        return new AsyncObserver<T>() {
            @Override
            public AsyncFuture<Void> observe(T value) throws Exception {
                return observer.observe(value);
            }

            @Override
            public void cancel() throws Exception {
                callAfter(() -> observer.cancel());
            }

            @Override
            public void fail(final Throwable cause) throws Exception {
                callAfter(() -> observer.fail(cause));
            }

            @Override
            public void end() throws Exception {
                callAfter(() -> observer.end());
            }

            private void callAfter(final FutureFinished original) throws Exception {
                Exception nested = null;

                try {
                    callback.finished();
                } catch (final Exception e) {
                    nested = e;
                }

                try {
                    original.finished();
                } catch (final Exception e) {
                    if (nested != null) {
                        e.addSuppressed(nested);
                    }

                    throw e;
                }

                if (nested != null) {
                    throw nested;
                }
            }
        };
    }
}
