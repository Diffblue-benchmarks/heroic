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

package com.spotify.heroic.grammar;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.beans.ConstructorProperties;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Data
@EqualsAndHashCode(exclude = {"ctx"})
@JsonTypeName("instant")
@RequiredArgsConstructor
public class InstantExpression implements Expression {
    @Getter(AccessLevel.NONE)
    private final Context ctx;

    private final Instant instant;

    @ConstructorProperties({"instant"})
    public InstantExpression(final Instant instant) {
        this(Context.empty(), instant);
    }

    @Override
    public Expression add(final Expression other) {
        final long o = other.cast(InstantExpression.class).getInstant().toEpochMilli();
        final long m = instant.toEpochMilli() + o;
        return new InstantExpression(ctx.join(other.context()), Instant.ofEpochMilli(m));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Expression> T cast(Class<T> to) {
        if (to.isAssignableFrom(InstantExpression.class)) {
            return (T) this;
        }

        if (to.isAssignableFrom(IntegerExpression.class)) {
            return (T) new IntegerExpression(ctx, instant.toEpochMilli());
        }

        if (to.isAssignableFrom(DoubleExpression.class)) {
            return (T) new DoubleExpression(ctx, instant.toEpochMilli());
        }

        if (to.isAssignableFrom(DurationExpression.class)) {
            return (T) new DurationExpression(ctx, TimeUnit.MILLISECONDS, instant.toEpochMilli());
        }

        throw ctx.castError(this, to);
    }

    @Override
    public <R> R visit(final Visitor<R> visitor) {
        return visitor.visitInstant(this);
    }

    @Override
    public Context context() {
        return ctx;
    }

    @Override
    public String toString() {
        return String.format("{%s}", instant.toString());
    }
}