import javax.servlet.*;
import java.io.IOException;

public class CachedServlet implements Servlet {
    private ServletConfig config;

    public void init(ServletConfig config) throws ServletException {
        this.config = config;
    }

    public ServletConfig getServletConfig() {
        return config;
    }

    public void service(ServletRequest req, ServletResponse res) 
            throws ServletException, IOException {
        res.getWriter().write("Cached Response");
    }

    public String getServletInfo() {
        return "Cached Servlet";
    }

    public void destroy() {}
} 