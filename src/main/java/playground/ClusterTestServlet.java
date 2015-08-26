package playground;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;

/**
 * testing cluster replication
 */
public class ClusterTestServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        StringBuffer response = new StringBuffer(150);
        response = response.append("nodeId: ").append(System.getProperty("cluster.node")).append("\n");
        response = response.append("server: ").append(req.getServerName()).append(":").append(req.getServerPort()).append("\n");

        HttpSession session = req.getSession(true);

        if (req.getParameter("counter") != null) {

            String attributeName = "counter";

            Integer counter = (Integer) session.getAttribute(attributeName);

            if (counter == null) {
                response = response.append("counter reset\n");
                counter = 0;
            }
            else {
                response = response.append("counter NOT NULL :)\n");
            }

            response = response.append("current value of counter is ").append(counter).append("\n");
            counter = counter + 1;
            response = response.append("counter is now").append(counter).append("\n");
            session.setAttribute(attributeName, counter);

            resp.getOutputStream().print(response.toString());
            resp.getOutputStream().close();

        }
        if (req.getParameter("counterHolder") != null) {
            String attributeName = "counterHolder";

            CounterHolder holder = (CounterHolder) session.getAttribute(attributeName);
            if (holder == null) {
                // create and set...
                holder = new CounterHolder();
                session.setAttribute(attributeName, holder);
            }
            else {
                holder.count();
                if (req.getParameter("counterHolderSet") != null) {
                    session.setAttribute(attributeName, holder);
                }
            }
            response = response.append("counter is now ").append(holder.getCounter()).append("\n");
            resp.getOutputStream().print(response.toString());
            resp.getOutputStream().close();
        }
    }

    private class CounterHolder implements Serializable {
        private int counter;

        private CounterHolder() {
            counter = 0;
        }

        private int getCounter() {
            return counter;
        }

        private void setCounter(int counter) {
            this.counter = counter;
        }

        private void count() {
            counter++;
        }
    }

}
