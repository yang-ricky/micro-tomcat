package com.microtomcat.processor;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.container.Engine;
import com.microtomcat.container.Host;
import com.microtomcat.container.Context;
import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.session.Session;
import com.microtomcat.session.SessionManager;
import com.microtomcat.session.distributed.DistributedSessionManager;
import com.microtomcat.session.distributed.InMemoryReplicatedSessionStore;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Processor extends LifecycleBase {
    private final String webRoot;
    private final Engine engine;
    private final SessionManager sessionManager;

    public Processor(String webRoot, Engine engine, SessionManager sessionManager) {
        this.webRoot = webRoot;
        this.engine = engine;
        this.sessionManager = sessionManager;
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
        Response response = null;
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            
            Request request = new Request(input, sessionManager);
            request.parse();
            
            response = new Response(output);
            
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
            
        } catch (IOException e) {
            log("Error processing request: " + e.getMessage());
            if (response != null) {
                try {
                    response.sendError(500, "Internal Server Error: " + e.getMessage());
                } catch (IOException ignored) {
                    // 忽略发送错误响应时的异常
                }
            }
        } finally {
            try {
                socket.close(); // 确保socket被关闭
            } catch (IOException e) {
                log("Error closing socket: " + e.getMessage());
            }
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

    public void processNio(SelectionKey key, ByteBuffer buffer) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Response response = new Response(new ChannelOutputStream(channel));
        
        try {
            Request request = new Request(new ByteBufferInputStream(buffer), sessionManager);
            request.parse();
            engine.invoke(request, response);
        } catch (Exception e) {
            log("Error processing NIO request: " + e.getMessage());
            response.sendError(500, "Internal Server Error: " + e.getMessage());
        }
    }

    private static class ByteBufferInputStream extends InputStream {
        private final ByteBuffer buffer;
        
        ByteBufferInputStream(ByteBuffer buffer) {
            this.buffer = buffer;
        }
        
        @Override
        public int read() {
            if (!buffer.hasRemaining()) {
                return -1;
            }
            return buffer.get() & 0xFF;
        }
    }

    private static class ChannelOutputStream extends OutputStream {
        private final SocketChannel channel;
        private final ByteBuffer buffer = ByteBuffer.allocate(8192);
        
        ChannelOutputStream(SocketChannel channel) {
            this.channel = channel;
        }
        
        @Override
        public void write(int b) throws IOException {
            if (!buffer.hasRemaining()) {
                flush();
            }
            buffer.put((byte) b);
        }
        
        @Override
        public void flush() throws IOException {
            buffer.flip();
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            buffer.clear();
        }
    }
} 