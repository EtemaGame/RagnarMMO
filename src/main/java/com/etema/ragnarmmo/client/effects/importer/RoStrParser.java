package com.etema.ragnarmmo.client.effects.importer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class RoStrParser {

    public RoStrEffect parse(byte[] data) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            byte[] magic = new byte[4];
            input.readFully(magic);
            String magicString = new String(magic, StandardCharsets.US_ASCII);
            if (!"STRM".equals(magicString)) {
                throw new IOException("Invalid STR magic: " + magicString);
            }

            long version = Integer.toUnsignedLong(Integer.reverseBytes(input.readInt()));
            input.skipBytes(2);
            long fps = Integer.toUnsignedLong(Integer.reverseBytes(input.readInt()));
            long maxKey = Integer.toUnsignedLong(Integer.reverseBytes(input.readInt()));
            long layerCount = Integer.toUnsignedLong(Integer.reverseBytes(input.readInt()));
            input.skipBytes(16);

            List<RoStrEffect.Layer> layers = new ArrayList<>();
            for (int i = 0; i < layerCount; i++) {
                long textureCount = Integer.toUnsignedLong(Integer.reverseBytes(input.readInt()));
                List<String> textures = new ArrayList<>();
                for (int tex = 0; tex < textureCount; tex++) {
                    long nameLen = Integer.toUnsignedLong(Integer.reverseBytes(input.readInt()));
                    byte[] nameBytes = new byte[(int) nameLen];
                    input.readFully(nameBytes);
                    textures.add(new String(nameBytes, StandardCharsets.UTF_8));
                }

                long keyframeCount = Integer.toUnsignedLong(Integer.reverseBytes(input.readInt()));
                List<RoStrEffect.KeyFrame> keyframes = new ArrayList<>();
                for (int k = 0; k < keyframeCount; k++) {
                    keyframes.add(new RoStrEffect.KeyFrame(
                            Integer.toUnsignedLong(Integer.reverseBytes(input.readInt())),
                            Integer.toUnsignedLong(Integer.reverseBytes(input.readInt())),
                            new float[] { readFloatLE(input), readFloatLE(input) },
                            readFloatArray(input, 8),
                            readFloatArray(input, 8),
                            readFloatLE(input),
                            Integer.toUnsignedLong(Integer.reverseBytes(input.readInt())),
                            readFloatLE(input),
                            readFloatLE(input),
                            readFloatArray(input, 4),
                            Integer.toUnsignedLong(Integer.reverseBytes(input.readInt())),
                            Integer.toUnsignedLong(Integer.reverseBytes(input.readInt())),
                            Integer.toUnsignedLong(Integer.reverseBytes(input.readInt()))));
                }

                layers.add(new RoStrEffect.Layer(textures, keyframes));
            }

            return new RoStrEffect(version, fps, maxKey, layers);
        }
    }

    private float[] readFloatArray(DataInputStream input, int size) throws IOException {
        float[] values = new float[size];
        for (int i = 0; i < size; i++) {
            values[i] = readFloatLE(input);
        }
        return values;
    }

    private float readFloatLE(DataInputStream input) throws IOException {
        return Float.intBitsToFloat(Integer.reverseBytes(input.readInt()));
    }

    public record RoStrEffect(
            long version,
            long fps,
            long maxKey,
            List<Layer> layers) {

        public record Layer(
                List<String> textures,
                List<KeyFrame> keyframes) {
        }

        public record KeyFrame(
                long frame,
                long type,
                float[] position,
                float[] uv,
                float[] xy,
                float texId,
                long animType,
                float animDelay,
                float angle,
                float[] color,
                long srcAlpha,
                long destAlpha,
                long materialPreset) {
        }
    }
}
