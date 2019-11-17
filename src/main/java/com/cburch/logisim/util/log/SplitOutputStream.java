package com.cburch.logisim.util.log;

import java.io.IOException;
import java.io.OutputStream;

/**@author Zachary Shelton*/
public class SplitOutputStream extends OutputStream {

    private OutputStream outputStream;
    private OutputStream otherOutputStream;

    public SplitOutputStream(final OutputStream outputStream, final OutputStream otherOutputStream) {
        this.outputStream = outputStream;
        this.otherOutputStream = otherOutputStream;
    }

    @Override
    public void write(int b) throws IOException {
        this.outputStream.write(b);
        this.otherOutputStream.write(b);
    }

    @Override
    public void close() throws IOException{
        this.outputStream.close();
        this.otherOutputStream.close();
    }

    @Override
    public void flush() throws IOException {
        this.outputStream.flush();
        this.otherOutputStream.flush();
    }

    @Override
    public void write(final byte[] b) throws IOException {
        this.outputStream.write(b);
        this.otherOutputStream.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        this.outputStream.write(b, off, len);
        this.otherOutputStream.write(b, off, len);
    }
}
