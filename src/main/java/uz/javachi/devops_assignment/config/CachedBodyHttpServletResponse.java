package uz.javachi.devops_assignment.config;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {

    private ByteArrayOutputStream cachedOutputStream;
    private ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;
    private final HttpServletResponse response;

    public CachedBodyHttpServletResponse(HttpServletResponse response) {
        super(response);
        this.response = response;
        this.cachedOutputStream = new ByteArrayOutputStream();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (printWriter != null) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }

        if (servletOutputStream == null) {
            servletOutputStream = new CachedBodyServletOutputStream(cachedOutputStream, response.getOutputStream());
        }
        return servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (servletOutputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        }

        if (printWriter == null) {
            // Create a writer that writes to both cache and underlying response
            ServletOutputStream underlyingOutputStream = response.getOutputStream();
            CachedBodyServletOutputStream cachedStream = new CachedBodyServletOutputStream(cachedOutputStream, underlyingOutputStream);
            printWriter = new PrintWriter(new OutputStreamWriter(cachedStream, getCharacterEncoding()));
        }
        return printWriter;
    }

    public byte[] getCachedBody() {
        if (printWriter != null) {
            printWriter.flush();
        }
        return cachedOutputStream.toByteArray();
    }

    public String getCachedBodyAsString() {
        return new String(getCachedBody());
    }

    private static class CachedBodyServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream cacheBuffer;
        private final ServletOutputStream underlyingStream;

        public CachedBodyServletOutputStream(ByteArrayOutputStream cacheBuffer, ServletOutputStream underlyingStream) {
            this.cacheBuffer = cacheBuffer;
            this.underlyingStream = underlyingStream;
        }

        @Override
        public void write(int b) throws IOException {
            cacheBuffer.write(b);
            underlyingStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            cacheBuffer.write(b);
            underlyingStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            cacheBuffer.write(b, off, len);
            underlyingStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            underlyingStream.flush();
        }

        @Override
        public boolean isReady() {
            return underlyingStream.isReady();
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            underlyingStream.setWriteListener(listener);
        }
    }
}
