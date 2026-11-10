package za.ac.skiet.vyou2.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.IOException;
import java.io.InputStream;

final class SizeLimitedRequestWrapper extends HttpServletRequestWrapper {

    private final long maxBytes;

    SizeLimitedRequestWrapper(HttpServletRequest request, long maxBytes) {
        super(request);
        this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        ServletInputStream delegate = super.getInputStream();
        return new ServletInputStream() {
            private long readSoFar;

            @Override
            public int read() throws IOException {
                int b = delegate.read();
                if (b != -1) {
                    checkLimit(1);
                }
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int n = delegate.read(b, off, len);
                if (n > 0) {
                    checkLimit(n);
                }
                return n;
            }

            private void checkLimit(int n) throws IOException {
                readSoFar += n;
                if (readSoFar > maxBytes) {
                    throw new IOException("Request body exceeds the maximum allowed size of " + maxBytes + " bytes");
                }
            }

            @Override
            public boolean isFinished() {
                return delegate.isFinished();
            }

            @Override
            public boolean isReady() {
                return delegate.isReady();
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                delegate.setReadListener(readListener);
            }
        };
    }
}
