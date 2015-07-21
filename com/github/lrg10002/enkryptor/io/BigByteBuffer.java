package com.github.lrg10002.enkryptor.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BigByteBuffer {

    private List<ByteBuffer> buffs = new ArrayList<>();

    public void add(ByteBuffer b) {
        buffs.add(b);
    }

    public void addAll(ByteBuffer... buffs) {
        for (ByteBuffer buff : buffs) {
            add(buff);
        }
    }

    public ByteBuffer makeBigBuffer() {
        int capacity = 0;
        for (ByteBuffer bb : buffs) {
            capacity += bb.capacity();
        }

        ByteBuffer bigbigbuffer = ByteBuffer.allocateDirect(capacity);
        for (ByteBuffer bb : buffs) {
            bigbigbuffer = bigbigbuffer.put(bb);
        }

        return bigbigbuffer;
    }

    public ByteBuffer makeBigBufferWithLength() {
        int capacity = 0;
        for (ByteBuffer bb : buffs) {
            capacity += bb.capacity();
        }

        ByteBuffer bigbigbuffer = ByteBuffer.allocateDirect(capacity + Integer.BYTES);
        bigbigbuffer = bigbigbuffer.putInt(capacity);
        for (ByteBuffer bb : buffs) {
            bigbigbuffer = bigbigbuffer.put(bb);
        }

        return bigbigbuffer;
    }
}
