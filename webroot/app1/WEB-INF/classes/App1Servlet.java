public class App1Servlet extends com.microtomcat.servlet.HttpServlet {
    @Override
    public void service(com.microtomcat.connector.Request request, 
                       com.microtomcat.connector.Response response) 
            throws com.microtomcat.servlet.ServletException, java.io.IOException {
        String content = "<html><body><h1>Hello from 不一样App1Servlet in app1/WEB-INF/classes!</h1></body></html>";
        response.sendServletResponse(content);
    }
}