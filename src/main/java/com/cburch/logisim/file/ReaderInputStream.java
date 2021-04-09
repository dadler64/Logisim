/*
 * Copyright 2004-2005 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.cburch.logisim.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Adapts a <code>Reader</code> as an <code>InputStream</code>.
 * Adapted from <CODE>StringInputStream</CODE>.
 */
public class ReaderInputStream extends InputStream {

    /**
     * Source Reader
     */
    private Reader in;

    private String encoding = System.getProperty("file.encoding");

    private byte[] slack;

    private int begin;

    /**
     * Construct a <CODE>ReaderInputStream</CODE>
     * for the specified <CODE>Reader</CODE>.
     *
     * @param reader <CODE>Reader</CODE>.  Must not be <code>null</code>.
     */
    private ReaderInputStream(Reader reader) {
        in = reader;
    }

    /**
     * Construct a <CODE>ReaderInputStream</CODE>
     * for the specified <CODE>Reader</CODE>,
     * with the specified encoding.
     *
     * @param reader non-null <CODE>Reader</CODE>.
     * @param encoding non-null <CODE>String</CODE> encoding.
     */
    public ReaderInputStream(Reader reader, String encoding) {
        this(reader);
        if (encoding == null) {
            throw new IllegalArgumentException("encoding must not be null");
        } else {
            this.encoding = encoding;
        }
    }

    /**
     * Reads from the <CODE>Reader</CODE>, returning the same value.
     *
     * @return the value of the next character in the <CODE>Reader</CODE>.
     * @throws IOException if the original <code>Reader</code> fails to be read
     */
    @Override
    public synchronized int read() throws IOException {
        if (in == null) {
            throw new IOException("Stream Closed");
        }

        byte result;
        if (slack != null && begin < slack.length) {
            result = slack[begin];
            if (++begin == slack.length) {
                slack = null;
            }
        } else {
            byte[] buffer = new byte[1];
            if (read(buffer, 0, 1) <= 0) {
                result = -1;
            }
            result = buffer[0];
        }

        if (result < -1) {
            result += 256;
        }

        return result;
    }

    /**
     * Reads from the <code>Reader</code> into a byte array
     *
     * @param bytes the byte array to read into
     * @param offset the offset in the byte array
     * @param length the length in the byte array to fill
     * @return the actual number read into the byte array, -1 at
     * the end of the stream
     * @throws IOException if an error occurs
     */
    @Override
    public synchronized int read(byte[] bytes, int offset, int length)
        throws IOException {
        if (in == null) {
            throw new IOException("Stream Closed");
        }

        while (slack == null) {
            char[] buffer = new char[length]; // might read too much
            int n = in.read(buffer);
            if (n == -1) {
                return -1;
            }
            if (n > 0) {
                slack = new String(buffer, 0, n).getBytes(encoding);
                begin = 0;
            }
        }

        if (length > slack.length - begin) {
            length = slack.length - begin;
        }

        System.arraycopy(slack, begin, bytes, offset, length);

        if ((begin += length) >= slack.length) {
            slack = null;
        }

        return length;
    }

    /**
     * Marks the read limit of the StringReader.
     *
     * @param limit the maximum limit of bytes that can be read before the
     * mark position becomes invalid
     */
    @Override
    public synchronized void mark(final int limit) {
        try {
            in.mark(limit);
        } catch (IOException ioException) {
//            throw new RuntimeException(ioException.getMessage());
            ioException.printStackTrace();
        }
    }


    /**
     * @return the current number of bytes ready for reading
     * @throws IOException if an error occurs
     */
    @Override
    public synchronized int available() throws IOException {
        if (in == null) {
            throw new IOException("Stream Closed");
        }
        if (slack != null) {
            return slack.length - begin;
        }
        if (in.ready()) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * @return false - mark is not supported
     */
    @Override
    public boolean markSupported() {
        return false;   // would be imprecise
    }

    /**
     * Resets the StringReader.
     *
     * @throws IOException if the StringReader fails to be reset
     */
    @Override
    public synchronized void reset() throws IOException {
        if (in == null) {
            throw new IOException("Stream Closed");
        }
        slack = null;
        in.reset();
    }

    /**
     * Closes the StringReader.
     *
     * @throws IOException if the original StringReader fails to be closed
     */
    @Override
    public synchronized void close() throws IOException {
        if (in != null) {
            in.close();
            slack = null;
            in = null;
        }
    }
}
