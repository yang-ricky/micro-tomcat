package com.microtomcat.protocol;

import com.microtomcat.processor.Processor;
import com.microtomcat.processor.ProcessorPool;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Http11NioProtocol extends AbstractProtocol {
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private ProcessorPool processorPool;
    private volatile boolean running;
    
    // HTTP/1.1 specific constants
    private static final String HTTP_11 = "HTTP/1.1";
    private static final int DEFAULT_MAX_HEADER_SIZE = 8 * 1024;
    private static final int DEFAULT_MAX_POST_SIZE = 2 * 1024 * 1024;
    private static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 20000; // 20s
    
    // 缓冲区大小
    private static final int BUFFER_SIZE = 8 * 1024;
    
    // 保存每个连接的状态
    private final Map<SocketChannel, ConnectionState> connections = new ConcurrentHashMap<>();
    
    private static final int INITIAL_BUFFER_SIZE = 8 * 1024;
    private static final int MAX_BUFFER_SIZE = 64 * 1024;
    private ExecutorService processorExecutor = 
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    private static class ConnectionState {
        final DynamicByteBuffer readBuffer;
        final DynamicByteBuffer writeBuffer;
        long lastActiveTime;
        State state;
        StringBuilder requestLine;
        Map<String, String> headers;
        boolean keepAlive;
        Processor processor;
        
        private static class DynamicByteBuffer {
            ByteBuffer buffer;
            final int maxSize;
            
            DynamicByteBuffer(int initialSize, int maxSize) {
                this.buffer = ByteBuffer.allocate(initialSize);
                this.maxSize = maxSize;
            }
            
            ByteBuffer getBuffer() {
                return buffer;
            }
            
            void ensureCapacity(int needed) {
                if (buffer.remaining() < needed) {
                    int newSize = Math.min(buffer.capacity() * 2, maxSize);
                    if (newSize < buffer.capacity() + needed) {
                        throw new BufferOverflowException();
                    }
                    ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }
        }
        
        ConnectionState(int initialSize, int maxSize) {
            this.readBuffer = new DynamicByteBuffer(initialSize, maxSize);
            this.writeBuffer = new DynamicByteBuffer(initialSize, maxSize);
            this.lastActiveTime = System.currentTimeMillis();
            this.state = State.READ_REQUEST_LINE;
            this.requestLine = new StringBuilder();
            this.headers = new HashMap<>();
            this.keepAlive = true;
        }
        
        enum State {
            READ_REQUEST_LINE,
            READ_HEADERS,
            READ_CHUNK_SIZE,
            READ_CHUNK_DATA,
            READ_CHUNK_END,
            PROCESS_REQUEST,
            WRITE_RESPONSE,
            WRITE_RESPONSE_BODY,
            COMPLETE
        }
    }
    
    @Override
    public void init() throws Exception {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void start() throws Exception {
        running = true;
        processorExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        // 启动清理线程
        startCleanupThread();
        
        while (running) {
            try {
                selector.select(1000);
                processSelectedKeys();
                processTimeouts();
            } catch (Exception e) {
                log("Error in event loop: " + e.getMessage());
            }
        }
    }
    
    private void processSelectedKeys() {
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            SelectionKey key = keys.next();
            keys.remove();
            
            try {
                if (!key.isValid()) continue;
                
                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            } catch (CancelledKeyException e) {
                closeConnection(key);
            } catch (Exception e) {
                log("Error processing key: " + e.getMessage());
                closeConnection(key);
            }
        }
    }
    
    // 处理耗时操作
    private void processRequest(SelectionKey key, ConnectionState state) {
        processorExecutor.submit(() -> {
            try {
                Processor processor = processorPool.getProcessor(5000);
                if (processor != null) {
                    try {
                        processor.processNio(key, state.writeBuffer.buffer);
                    } finally {
                        processorPool.releaseProcessor(processor);
                    }
                }
            } catch (Exception e) {
                log("Error in request processing: " + e.getMessage());
                closeConnection(key);
            }
        });
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        connections.put(client, new ConnectionState(INITIAL_BUFFER_SIZE, MAX_BUFFER_SIZE));
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ConnectionState state = connections.get(channel);
        if (state == null) {
            return;
        }

        ByteBuffer buffer = state.readBuffer.buffer;
        int read = channel.read(buffer);
        
        if (read == -1) {
            closeConnection(key);
            return;
        }

        buffer.flip();
        processBuffer(key, state);
        buffer.compact();
    }

    private void processBuffer(SelectionKey key, ConnectionState state) throws IOException {
        switch (state.state) {
            case READ_REQUEST_LINE:
                if (readLine(state.readBuffer.getBuffer(), state.requestLine)) {
                    state.state = ConnectionState.State.READ_HEADERS;
                }
                break;
                
            case READ_HEADERS:
                StringBuilder line = new StringBuilder();
                while (readLine(state.readBuffer.getBuffer(), line)) {
                    if (line.length() == 0) {
                        state.keepAlive = isKeepAlive(state.headers);
                        state.state = ConnectionState.State.PROCESS_REQUEST;
                        try {
                            state.processor = processorPool.getProcessor(5000);
                            key.interestOps(SelectionKey.OP_WRITE);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Interrupted while getting processor", e);
                        }
                        break;
                    }
                    parseHeader(line.toString(), state.headers);
                    line.setLength(0);
                }
                break;
                
            case PROCESS_REQUEST:
                if (state.processor != null) {
                    state.processor.processNio(key, state.writeBuffer.getBuffer());
                    state.state = ConnectionState.State.WRITE_RESPONSE;
                }
                break;
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ConnectionState state = connections.get(channel);
        if (state == null) {
            return;
        }

        ByteBuffer buffer = state.writeBuffer.buffer;
        buffer.flip();
        channel.write(buffer);
        
        if (!buffer.hasRemaining()) {
            buffer.compact();
            if (state.keepAlive) {
                state.state = ConnectionState.State.READ_REQUEST_LINE;
                key.interestOps(SelectionKey.OP_READ);
            } else {
                closeConnection(key);
            }
        }
    }

    private boolean readLine(ByteBuffer buffer, StringBuilder line) {
        while (buffer.hasRemaining()) {
            char c = (char) buffer.get();
            if (c == '\n') {
                if (line.length() > 0 && line.charAt(line.length() - 1) == '\r') {
                    line.setLength(line.length() - 1);
                }
                return true;
            }
            line.append(c);
        }
        return false;
    }

    private void parseHeader(String line, Map<String, String> headers) {
        int colonPos = line.indexOf(':');
        if (colonPos > 0) {
            String name = line.substring(0, colonPos).trim().toLowerCase();
            String value = line.substring(colonPos + 1).trim();
            headers.put(name, value);
        }
    }

    private boolean isKeepAlive(Map<String, String> headers) {
        String connection = headers.get("connection");
        if ("close".equalsIgnoreCase(connection)) {
            return false;
        }
        return true; // HTTP/1.1 默认 keep-alive
    }

    private void closeConnection(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        connections.remove(channel);
        try {
            channel.close();
        } catch (IOException e) {
            // 忽略关闭错误
        }
        key.cancel();
    }

    @Override
    public void stop() throws Exception {
        running = false;
        if (selector != null) {
            selector.close();
        }
        if (serverChannel != null) {
            serverChannel.close();
        }
    }

    @Override
    public void setProcessorPool(ProcessorPool processorPool) {
        this.processorPool = processorPool;
    }

    private void log(String message) {
        System.out.printf("[Http11NioProtocol] %s%n", message);
    }

    private void startCleanupThread() {
        Thread cleanup = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(1000);
                    long now = System.currentTimeMillis();
                    connections.forEach((channel, state) -> {
                        if (now - state.lastActiveTime > DEFAULT_KEEP_ALIVE_TIMEOUT) {
                            closeConnection(channel.keyFor(selector));
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanup.setDaemon(true);
        cleanup.start();
    }

    private void processTimeouts() {
        long now = System.currentTimeMillis();
        connections.forEach((channel, state) -> {
            if (now - state.lastActiveTime > DEFAULT_KEEP_ALIVE_TIMEOUT) {
                closeConnection(channel.keyFor(selector));
            }
        });
    }
} 