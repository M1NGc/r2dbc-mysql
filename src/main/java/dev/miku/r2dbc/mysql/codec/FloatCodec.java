/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.miku.r2dbc.mysql.codec;

import dev.miku.r2dbc.mysql.Parameter;
import dev.miku.r2dbc.mysql.ParameterWriter;
import dev.miku.r2dbc.mysql.constant.DataTypes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Codec for {@link float}.
 */
final class FloatCodec extends AbstractPrimitiveCodec<Float> {

    FloatCodec(ByteBufAllocator allocator) {
        super(allocator, Float.TYPE, Float.class);
    }

    @Override
    public Float decode(ByteBuf value, FieldInformation info, Class<?> target, boolean binary, CodecContext context) {
        if (binary && info.getType() == DataTypes.FLOAT) {
            return value.readFloatLE();
        }
        // otherwise encoded by text (must not be DOUBLE).
        return Float.parseFloat(value.toString(StandardCharsets.US_ASCII));
    }

    @Override
    public boolean canEncode(Object value) {
        return value instanceof Float;
    }

    @Override
    public Parameter encode(Object value, CodecContext context) {
        return new FloatParameter(allocator, (Float) value);
    }

    @Override
    protected boolean doCanDecode(FieldInformation info) {
        short type = info.getType();
        return DataTypes.FLOAT == type || (info.getSize() < 7 && TypePredicates.isDecimal(type));
    }

    private static final class FloatParameter extends AbstractParameter {

        private final ByteBufAllocator allocator;

        private final float value;

        private FloatParameter(ByteBufAllocator allocator, float value) {
            this.allocator = allocator;
            this.value = value;
        }

        @Override
        public Mono<ByteBuf> publishBinary() {
            return Mono.fromSupplier(() -> {
                ByteBuf buf = allocator.buffer(Float.BYTES);
                try {
                    return buf.writeFloatLE(value);
                } catch (Throwable e) {
                    buf.release();
                    throw e;
                }
            });
        }

        @Override
        public Mono<Void> publishText(ParameterWriter writer) {
            return Mono.fromRunnable(() -> writer.writeFloat(value));
        }

        @Override
        public short getType() {
            return DataTypes.FLOAT;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FloatParameter)) {
                return false;
            }

            FloatParameter that = (FloatParameter) o;

            return Float.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
            return (value != +0.0f ? Float.floatToIntBits(value) : 0);
        }
    }
}
