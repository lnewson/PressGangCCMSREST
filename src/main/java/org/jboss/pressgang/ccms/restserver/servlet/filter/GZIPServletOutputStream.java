package org.jboss.pressgang.ccms.restserver.servlet.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * A wrapper class to allow using a {@link java.util.zip.GZIPOutputStream GZIPOutputStream} as a {@link javax.servlet.ServletOutputStream ServletOutputStream}.
 * <br /><br />
 * See: <a href="http://stackoverflow.com/questions/4755302/which-compression-is-gzip-the-most-popular-servlet-filter-would-you-suggest/11068672#11068672">which-compression-is-gzip-the-most-popular-servlet-filter-would-you-suggest</a>
 * and <a href="https://github.com/geoserver/geoserver/tree/master/src/main/src/main/java/org/geoserver/filters">https://github.com/geoserver/geoserver/</a>
 * for details on where this implementation came from.
 */
public class GZIPServletOutputStream extends ServletOutputStream {
    protected ByteArrayOutputStream baos = null;
    protected GZIPOutputStream GZIPStream = null;
    protected boolean closed = false;
    protected HttpServletResponse response = null;
    protected ServletOutputStream output = null;

    public GZIPServletOutputStream(HttpServletResponse response) throws IOException {
        super();
        closed = false;
        this.response = response;
        this.output = response.getOutputStream();
        baos = new ByteArrayOutputStream();
        GZIPStream = new GZIPOutputStream(baos);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException("This output stream has already been closed");
        }
        GZIPStream.finish();

        final byte[] bytes = baos.toByteArray();
        final String contentLength = Integer.toString(bytes.length);
        
        if (response.containsHeader("Content-Length")) {
            response.setHeader("Content-Length", contentLength);
        } else {
            response.addHeader("Content-Length", contentLength);
        }
         
        response.addHeader("Content-Encoding", "gzip");
        output.write(bytes);
        output.flush();
        output.close();
        closed = true;
    }

    @Override
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Cannot flush a closed output stream");
        }
        GZIPStream.flush();
    }

    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }
        GZIPStream.write((byte)b);
    }

    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }
        GZIPStream.write(b, off, len);
    }

    public boolean closed() {
        return (this.closed);
    }

    public void reset() {
    }
}