public class TestServlet extends com.microtomcat.servlet.HttpServlet {
    @Override
    protected void doGet(com.microtomcat.connector.Request request, 
                        com.microtomcat.connector.Response response) 
            throws com.microtomcat.servlet.ServletException, java.io.IOException {
        String content = "<html><body><h1>Hello from TestServlet in webroot!</h1></body></html>";
        response.sendServletResponse(content);
    }
}