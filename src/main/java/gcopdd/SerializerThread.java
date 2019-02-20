package gcopdd;

import com.esotericsoftware.kryo.Kryo;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SerializerThread extends Thread{

    public static ConcurrentLinkedQueue<Object> SERIALIZATION_QUEUE = new ConcurrentLinkedQueue<>();
    private static AtomicBoolean serializationThreadRunning = new AtomicBoolean(false);

    @Override
    public void run() {
        if (!serializationThreadRunning.compareAndSet(false, true))
            throw new RuntimeException("Multiple SerializerThreads can't be running at once!");

        Kryo kryo = new Kryo();
        //TODO initialization...

        while (true) {
            Object obj = SERIALIZATION_QUEUE.poll();
            //TODO ...
        }
    }
}
