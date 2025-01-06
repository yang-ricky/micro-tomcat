package com.microtomcat.processor;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.container.Engine;
import com.microtomcat.container.Host;
import com.microtomcat.context.Context;
import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.pipeline.Pipeline;
import com.microtomcat.pipeline.StandardPipeline;
import com.microtomcat.pipeline.valve.AccessLogValve;
import com.microtomcat.pipeline.valve.AuthenticatorValve;
import com.microtomcat.pipeline.valve.StandardValve;
import com.microtomcat.session.Session;
import com.microtomcat.session.SessionManager;
import com.microtomcat.session.distributed.DistributedSessionManager;
import com.microtomcat.session.distributed.InMemoryReplicatedSessionStore;

import javax.servlet.ServletException;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.container.Engine;
import com.microtomcat.container.Host;

public class Processor extends LifecycleBase {
    private final String webRoot;
    private final Engine engine;
    private final Pipeline pipeline;

    public Processor(String webRoot, Engine engine) {
        this.webRoot = webRoot;
        this.engine = engine;
        
        this.pipeline = new StandardPipeline();
        pipeline.addValve(new AccessLogValve());
        pipeline.addValve(new AuthenticatorValve());
        pipeline.setBasic(new StandardValve(webRoot));
    }

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing processor");
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting processor");
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping processor");
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying processor");
    }

    private void log(String message) {
        System.out.println("[Processor] " + message);
    }

    public void process(Socket socket) {
        try {
            Request request = new Request(socket.getInputStream(), null);
            Response response = new Response(socket.getOutputStream());
            
            try {
                request.parse();
                
                // 处理 /ping 请求
                if ("/ping".equals(request.getUri())) {
                    // 设置响应头和状态
                    response.setContentType("text/plain");
                    response.setContentLength(2);  // "OK" 的长度
                    response.setStatus(200);
                    
                    // 获取 writer 会自动写入响应头
                    PrintWriter writer = response.getWriter();
                    writer.write("OK");
                    writer.flush();
                    return;
                }
                
                if ("/_sessionReplication".equals(request.getUri())) {
                    handleSessionReplication(request, response);
                    return;
                }
                
                // 继续原有的处理逻辑
                engine.invoke(request, response);
                
            } catch (Exception e) {
                log("Error processing request: " + e.getMessage());
                try {
                    response.sendError(500, "Internal Server Error: " + e.getMessage());
                } catch (IOException ioe) {
                    log("Failed to send error response: " + ioe.getMessage());
                }
            } finally {
                try {
                    socket.close(); // 确保socket被关闭
                } catch (IOException e) {
                    log("Error closing socket: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            log("Error creating request/response: " + e.getMessage());
        }
    }

    private void handleSessionReplication(Request request, Response response) throws IOException {
        // 直接获取已解析的body
        String rawBody = request.getBody();
        if (rawBody == null || rawBody.isEmpty()) {
            response.sendError(400, "Bad Request: no body");
            return;
        }

        String[] lines = rawBody.split("\n", 2);
        if (lines.length < 2) {
            response.sendError(400, "Bad Request: invalid body format");
            return;
        }

        String actionLine = lines[0].trim();
        if (!actionLine.startsWith("ACTION=")) {
            response.sendError(400, "Bad Request: missing ACTION");
            return;
        }

        String action = actionLine.substring(7); // Remove "ACTION="
        
        // 使用默认上下文
        if (request.getContext() == null) {
            Host host = engine.getDefaultHostContainer();
            if (host != null) {
                Context defaultContext = host.getDefaultContext();
                if (defaultContext != null) {
                    request.setContext(defaultContext);
                }
            }
        }
        
        Context context = request.getContext();
        if (context == null) {
            response.sendError(500, "No context available");
            return;
        }

        try {
            SessionManager manager = context.getSessionManager();
            if (!(manager instanceof DistributedSessionManager)) {
                response.sendError(500, "Not a distributed session manager");
                return;
            }

            InMemoryReplicatedSessionStore sessionStore = 
                (InMemoryReplicatedSessionStore) ((DistributedSessionManager)manager).getSessionStore();

            if ("SAVE".equals(action)) {
                Session session = sessionStore.jsonToSession(lines[1]);
                if (session != null) {
                    sessionStore.saveSessionLocally(session);
                    response.sendError(200, "OK");
                } else {
                    response.sendError(400, "Invalid session data");
                }
            } else if ("DELETE".equals(action)) {
                String sessionId = lines[1].substring(10); // Remove "sessionId="
                sessionStore.deleteSessionLocally(sessionId);
                response.sendError(200, "OK");
            } else {
                response.sendError(400, "Invalid action");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Internal Server Error: " + e.getMessage());
        }
    }
} 