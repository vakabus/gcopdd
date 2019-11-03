package cz.cuni.mff.d3s.blood.report;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

public class NtarOutputStream implements Closeable {
    private static final int DEFLATE_BUFFER_INITIAL_CAPACITY = 8192;
    private final OutputStream os;

    public NtarOutputStream(OutputStream os) {
        this.os = os;
    }

    @Override
    public void close() throws IOException {
        os.close();
    }

    public void set(String entryName, byte[] entryContent) throws IOException {
        appendEntry(entryName, deflate(entryContent));
    }

    private void appendSizedBytes(int size, byte[] bytes) throws IOException {
        os.write(size >> 24);
        os.write(size >> 16);
        os.write(size >>  8);
        os.write(size >>  0);
        os.write(bytes, 0, size);
    }

    private void appendEntry(String entryName, Bytes compressedEntryContent) throws IOException {
        byte[] entryNameBytes = entryName.getBytes(StandardCharsets.US_ASCII);
        appendSizedBytes(entryNameBytes.length, entryNameBytes);
        appendSizedBytes(compressedEntryContent.length, compressedEntryContent.array);
    }

    private static Bytes deflate(byte[] uncompressed) {
        Bytes compressed = new Bytes(DEFLATE_BUFFER_INITIAL_CAPACITY);
        Deflater deflater = new Deflater();
        deflater.setInput(uncompressed);
        deflater.finish();
        for(;;) {
            compressed.length += deflater.deflate(compressed.array, compressed.length, compressed.remainingSpace());
            if(deflater.finished())
                break;
            compressed.resize(compressed.capacity * 2);
        }
        deflater.end();
        return compressed;
    }

    private static final class Bytes {
        int length;
        int capacity;
        byte[] array;

        Bytes(int capacity) {
            this.length = 0;
            this.capacity = capacity;
            this.array = new byte[capacity];
        }

        final void resize(int newCapacity) {
            byte[] newArray = new byte[newCapacity];
            System.arraycopy(array, 0, newArray, 0, length);
            capacity = newCapacity;
            array = newArray;
        }

        final int remainingSpace() {
            return capacity - length;
        }
    }
}
