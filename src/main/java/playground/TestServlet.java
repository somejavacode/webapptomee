package playground;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;

public class TestServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(TestServlet.class);


    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getRequestURI();
        String qs = req.getQueryString();
        if (qs != null) {
            url += "?" + qs;
        }
        String method = req.getMethod();

        LOG.info(method + " url=" + url);

        int sleep = saveGetInt(req.getParameter("s"));
        int burn = saveGetInt(req.getParameter("b"));
        int mem = saveGetInt(req.getParameter("m"));
        int up = saveGetInt(req.getParameter("u"));  // TODO: missing upload (large post)
        int down = saveGetInt(req.getParameter("d"));
        int randSeed = saveGetInt(req.getParameter("r"));
        int throttle = saveGetInt(req.getParameter("t"));

        boolean burnDouble = false;
        boolean throwEx = req.getParameter("ex") != null;

        testLimits(sleep, burn, burnDouble, mem, throwEx);

        String echo = req.getParameter("echo");
        if (echo != null) {
            resp.setCharacterEncoding("utf-8");
            resp.getOutputStream().print(echo);
            resp.getOutputStream().close();
        }
        else if (up > 0) {
            // String type = req.getHeader("Content-Type");  // don't care, assume "application/octet-stream"
            InputStream is = req.getInputStream();
            try {
                Random rand = randSeed == 0 ? new Random() : new Random(randSeed);
                int buffSize = 8192;
                byte[] upBuff = new byte[buffSize];
                int remaining = up;  // remaining bytes.
                while (remaining > 0) {
                    int bytes = is.read(upBuff);
                    if (bytes == -1) {
                        throw new RuntimeException("missing random upstream bytes: " + remaining);
                    }
                    byte[] randBuff = new byte[bytes];
                    rand.nextBytes(randBuff);  // no signature to fill part of byte array

                    byte[] usedBuff = upBuff;
                    if (bytes < buffSize) {
                        usedBuff = Arrays.copyOf(upBuff, bytes);
                    }
                    if (!Arrays.equals(usedBuff, randBuff)) {
                        throw new RuntimeException("invalid random upstream bytes.");
                    }
                    remaining -= buffSize;

                    if (throttle > 0) {
                        try {
                            Thread.sleep(throttle);
                        }
                        catch (InterruptedException e) {
                            LOG.warn("interrupted throttle sleep.");
                        }
                    }
                }
                int eof = is.read();  // check for EOF
                if (eof != -1) {
                    throw new RuntimeException("got surplus bytes.");
                }
            }
            finally {
                is.close();
            }
        }
        else if (down > 0) {
            // binary random response....
            resp.setHeader("Content-Type", "application/octet-stream");  // no alternative for these literals?
            // note about content size: it is calculated by tomcat for small sizes.
            // for large files (e.g. 20MB): "Transfer-Encoding: chunked"
            OutputStream os = resp.getOutputStream();
            Random rand = randSeed == 0 ? new Random() : new Random(randSeed);
            int buffSize = 8192;
            byte[] buff = new byte[buffSize];  // make this memory efficient
            int remaining = down;  // remaining bytes.
            while (remaining > buffSize) {
                rand.nextBytes(buff);
                os.write(buff);
                remaining -= buffSize;
                if (throttle > 0) {
                    try {
                        Thread.sleep(throttle);
                    }
                    catch (InterruptedException e) {
                        LOG.warn("interrupted throttle sleep.");
                    }
                }
            }
            byte[] lastBytes = new byte[remaining];
            rand.nextBytes(lastBytes);
            os.write(lastBytes);
            os.close();
        }

        String log = req.getParameter("log");
        if (log != null) {
            // test all that (not so) funny logging variants...

            LOG.debug("slfj4 debug " + log);
            LOG.info("slfj4 info " + log);
            LOG.warn("slfj4 warn " + log);
            // CHECKSTYLE:OFF
            System.out.println("system out " + log);
            System.err.println("system err " + log);
            // CHECKSTYLE:ON
            java.util.logging.Logger julLog = java.util.logging.Logger.getLogger("test logger");
            julLog.log(Level.FINE, "jul fine " + log);
            julLog.log(Level.INFO, "jul info " + log);
            julLog.log(Level.WARNING, "jul warn " + log);
        }

    }

    private int saveGetInt(String value) throws ServletException {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            LOG.error("parsing problem.", e);
            throw new ServletException(e);
        }
    }

    public void testLimits(int sleep, int burn, boolean burnDouble, int mem, boolean throwEx) throws RuntimeException {
        // keep bytes allocated during slow calls burnCpu() and sleep()
        byte[] bytes = allocateMemory(mem); // this memory is "locked" during sleep
        burnCpu(burn, false);
        sleep(sleep);
        throwException(throwEx);
        bytes = null;
    }

    private byte[] allocateMemory(final int memK) {
        if (memK != 0) {
            return new byte[memK * 1024]; // allocate memory as local variable
        }
        return null;
    }

    private void burnCpu(final int burn, boolean burnDouble) {
        if (burn != 0) {
            if (burnDouble) {
                fibonacciDouble(burn);
            }
            else {
                fibonacci(burn);
            }
        }
    }

    private void sleep(final int sleep) {
        if (sleep != 0) {
            try {
                Thread.sleep(sleep);
            }
            catch (InterruptedException ie) {
                LOG.warn("interrupted.", ie);
            }
        }
    }

    private void throwException(boolean throwEx) throws RuntimeException {
        if (throwEx) {
            throw new RuntimeException("TestServlet was here.");
        }
    }

    /**
     * fibonacci recursion to burn cpu
     */
    private long fibonacci(long n) {
        if (n <= 1) {
            return n;
        }
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    /**
     * fibonacci recursion to burn cpu
     */
    private double fibonacciDouble(double n) {
        if (n <= 1) {
            return n;
        }
        return fibonacciDouble(n - 1) + fibonacciDouble(n - 2);
    }

}