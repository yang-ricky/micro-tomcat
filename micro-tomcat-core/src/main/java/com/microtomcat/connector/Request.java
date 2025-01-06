package com.microtomcat.connector;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.microtomcat.session.Session;
import com.microtomcat.session.SessionManager;
import com.microtomcat.context.Context;
import com.microtomcat.session.HttpSessionWrapper;

public class Request implements HttpServletRequest {
    private InputStream input;
    private String method;
    private String uri;
    private String protocol;
    private Session session;
    private final SessionManager sessionManager;
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
    private final Map<String, String> headers = new HashMap<>();
    private Context context;
    private String serverName;
    private final StringBuilder requestContent = new StringBuilder();
    private BufferedReader reader;
    private String body;
    private final Map<String, String[]> parameters = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private int serverPort = 8080;
    private String remoteAddr;
    private String scheme = "http";
    private java.util.Locale locale = java.util.Locale.getDefault();
    private String characterEncoding = "UTF-8";
    private long contentLength = -1;
    private final List<Cookie> cookies = new ArrayList<>();

    public Request(InputStream input, SessionManager sessionManager) {
        this.input = input;
        this.sessionManager = sessionManager;
        this.reader = new BufferedReader(new InputStreamReader(input));
    }

    public void parse() throws IOException {
        String requestLine = reader.readLine();
        if (requestLine != null) {
            String[] parts = requestLine.split(" ");
            if (parts.length == 3) {
                method = parts[0];
                uri = parts[1];
                protocol = parts[2];
            }
        }
        
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int colonPos = headerLine.indexOf(':');
            if (colonPos > 0) {
                String headerName = headerLine.substring(0, colonPos).trim();
                String headerValue = headerLine.substring(colonPos + 1).trim();
                headers.put(headerName, headerValue);
            }
        }

        if ("POST".equalsIgnoreCase(method)) {
            String contentLengthStr = headers.get("Content-Length");
            if (contentLengthStr != null) {
                int contentLength = Integer.parseInt(contentLengthStr);
                char[] bodyChars = new char[contentLength];
                int readCount = reader.read(bodyChars);
                if (readCount > 0) {
                    this.body = new String(bodyChars, 0, readCount);
                }
            }
        }
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        this.characterEncoding = env;
    }

    @Override
    public int getContentLength() {
        return (int) contentLength;
    }

    @Override
    public long getContentLengthLong() {
        return contentLength;
    }

    @Override
    public String getContentType() {
        return headers.get("Content-Type");
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {
            private final InputStream in = input;

            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("ReadListener not supported");
            }
        };
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getServerName() {
        return serverName != null ? serverName : "localhost";
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(input, getCharacterEncoding()));
        }
        return reader;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr != null ? remoteAddr : "127.0.0.1";
    }

    @Override
    public String getRemoteHost() {
        return getRemoteAddr();
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(Collections.singletonList(locale));
    }

    @Override
    public boolean isSecure() {
        return "https".equalsIgnoreCase(scheme);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        Context context = getContext();
        if (context != null) {
            return new RequestDispatcher() {
                @Override
                public void forward(ServletRequest request, ServletResponse response)
                        throws ServletException, IOException {
                    throw new UnsupportedOperationException("Forward not supported yet");
                }

                @Override
                public void include(ServletRequest request, ServletResponse response)
                        throws ServletException, IOException {
                    throw new UnsupportedOperationException("Include not supported yet");
                }
            };
        }
        return null;
    }

    @Override
    public String getRealPath(String path) {
        Context context = getContext();
        return context != null ? context.getRealPath(path) : null;
    }

    @Override
    public int getRemotePort() {
        return 0;  // 默认值
    }

    @Override
    public String getLocalName() {
        return "localhost";
    }

    @Override
    public String getLocalAddr() {
        return "127.0.0.1";
    }

    @Override
    public int getLocalPort() {
        return serverPort;
    }

    @Override
    public ServletContext getServletContext() {
        Context context = getContext();
        return context != null ? context.getServletContext() : null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException("Async not supported");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        throw new UnsupportedOperationException("Async not supported");
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException("Async not supported");
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    // 其他公共方法
    public String getUri() {
        return uri;
    }

    public String getMethod() {
        return method;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public String getRequestURI() {
        return uri;
    }

    public String getContextPath() {
        Context context = getContext();
        return context != null ? context.getName() : "";
    }

    public String getBody() {
        if (body == null) {
            try {
                StringBuilder content = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;
                BufferedReader bodyReader = getReader();
                while ((bytesRead = bodyReader.read(buffer)) != -1) {
                    content.append(buffer, 0, bytesRead);
                }
                body = content.toString();
            } catch (IOException e) {
                // 处理异常
                body = "";
            }
        }
        return body;
    }

    private HttpSession getSessionInternal(boolean create) {
        if (session != null) {
            return new HttpSessionWrapper(session);
        }
        
        String sessionId = headers.get(SESSION_COOKIE_NAME);
        if (sessionId != null) {
            session = sessionManager.getSession(sessionId);
            if (session != null) {
                return new HttpSessionWrapper(session);
            }
        }
        
        if (create) {
            session = sessionManager.createSession();
            return new HttpSessionWrapper(session);
        }
        
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return getSessionInternal(create);
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        throw new UnsupportedOperationException("Upgrade not supported");
    }

    @Override
    public String getServletPath() {
        String contextPath = getContextPath();
        if (uri != null && contextPath != null && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return "";
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        throw new UnsupportedOperationException("Multipart not supported yet");
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException("Multipart not supported yet");
    }

    @Override
    public void logout() throws ServletException {
        HttpSession session = getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    public void login(String username, String password) throws ServletException {
        throw new UnsupportedOperationException("Authentication not supported yet");
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws ServletException {
        throw new UnsupportedOperationException("Authentication not supported yet");
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        // 检查 URL 中是否包含会话 ID
        String requestURI = getRequestURI();
        if (requestURI != null) {
            int sessionIndex = requestURI.indexOf(";jsessionid=");
            if (sessionIndex != -1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        // 检查会话 ID 是否来自 Cookie
        Cookie[] cookies = getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        String sessionId = headers.get(SESSION_COOKIE_NAME);
        if (sessionId != null) {
            Session session = sessionManager.getSession(sessionId);
            return session != null && session.isValid();
        }
        return false;
    }

    @Override
    public String changeSessionId() {
        HttpSession session = getSession(false);
        if (session == null) {
            throw new IllegalStateException("No session exists");
        }
        
        // 创建新会话并复制属性
        String oldId = session.getId();
        Session newSession = sessionManager.createSession();
        
        // 复制旧会话的属性到新会话
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            newSession.setAttribute(name, session.getAttribute(name));
        }
        
        // 移除旧会话
        sessionManager.removeSession(oldId);
        
        return newSession.getId();
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        String serverName = getServerName();
        
        url.append(scheme).append("://").append(serverName);
        
        // 只有非默认端口才需要显式添加
        if ((scheme.equals("http") && port != 80)
            || (scheme.equals("https") && port != 443)) {
            url.append(':').append(port);
        }
        
        url.append(getRequestURI());
        
        return url;
    }

    private String generateNewSessionId() {
        Session session = sessionManager.createSession();
        return session.getId();
    }

    @Override
    public String getRequestedSessionId() {
        // 首先从 Cookie 中查找
        Cookie[] cookies = getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // 如果没有找到，从 URL 中查找
        String uri = getRequestURI();
        if (uri != null) {
            int sessionStart = uri.indexOf(";jsessionid=");
            if (sessionStart != -1) {
                int sessionEnd = uri.indexOf(';', sessionStart + 11);
                if (sessionEnd == -1) {
                    sessionEnd = uri.length();
                }
                return uri.substring(sessionStart + 11, sessionEnd);
            }
        }
        
        return null;
    }

    @Override
    public java.security.Principal getUserPrincipal() {
        // 由于我们还没有实现认证系统，暂时返回 null
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        // 由于我们还没有实现认证和角色系统，暂时返回 false
        return false;
    }

    @Override
    public String getRemoteUser() {
        // 从会话中获取用户信息
        HttpSession session = getSession(false);
        if (session != null) {
            Object user = session.getAttribute("user");
            if (user != null) {
                return user.toString();
            }
        }
        return null;
    }

    @Override
    public String getQueryString() {
        String uri = getRequestURI();
        int questionPos = uri.indexOf('?');
        if (questionPos != -1) {
            return uri.substring(questionPos + 1);
        }
        return null;
    }

    @Override
    public String getPathTranslated() {
        String pathInfo = getPathInfo();
        if (pathInfo == null) {
            return null;
        }
        
        // 将虚拟路径转换为文件系统路径
        return getServletContext().getRealPath(pathInfo);
    }

    @Override
    public String getPathInfo() {
        String servletPath = getServletPath();
        String requestURI = getRequestURI();
        
        if (servletPath == null || requestURI == null) {
            return null;
        }
        
        // 如果请求URI等于servlet路径，说明没有额外的路径信息
        if (requestURI.equals(servletPath)) {
            return null;
        }
        
        // 返回servlet路径之后的部分作为路径信息
        if (requestURI.startsWith(servletPath)) {
            return requestURI.substring(servletPath.length());
        }
        
        return null;
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Header '" + name + "' cannot be converted to int: " + value);
        }
    }

    @Override
    public java.util.Enumeration<String> getHeaderNames() {
        return java.util.Collections.enumeration(headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String value = headers.get(name.toLowerCase());
        return value != null ? 
            Collections.enumeration(Collections.singletonList(value)) : 
            Collections.emptyEnumeration();
    }

    public void addHeader(String headerLine) {
        int colonPos = headerLine.indexOf(':');
        if (colonPos > 0) {
            String headerName = headerLine.substring(0, colonPos).trim().toLowerCase();
            String headerValue = headerLine.substring(colonPos + 1).trim();
            headers.put(headerName, headerValue);
        }
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    @Override
    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1L;
        }
        try {
            return Date.parse(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot parse date header: " + value);
        }
    }

    @Override
    public Cookie[] getCookies() {
        return cookies.toArray(new Cookie[0]);
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    @Override
    public String getAuthType() {
        return null; // 暂不支持认证
    }
}
