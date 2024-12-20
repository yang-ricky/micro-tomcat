package com.microtomcat.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NonBlockingHttpServer extends AbstractHttpServer {
    private final Selector selector;
    private final ServerSocketChannel serverChannel;

    public NonBlockingHttpServer(ServerConfig config) throws IOException {
        super(config);
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverChannel.socket().bind(new InetSocketAddress(config.getPort()));
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void start() throws IOException {
        log("Non-blocking server started on port: " + config.getPort());
        
        while (!Thread.currentThread().isInterrupted()) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                if (key.isAcceptable()) {
                    SocketChannel client = serverChannel.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                    log("New connection accepted from: " + client.getRemoteAddress());
                } else if (key.isReadable()) {
                    handleRead(key);
                }
            }
        }
    }

    @Override
    protected void stop() {
        try {
            selector.close();
            serverChannel.close();
        } catch (IOException e) {
            log("Error while stopping server: " + e.getMessage());
        }
    }
    
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        try {
            // 创建缓冲区读取数据
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = client.read(buffer);
            
            if (bytesRead == -1) {
                // 客户端关闭连接
                key.cancel();
                client.close();
                return;
            }
            
            // 处理HTTP请求
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String request = new String(data);
            
            // 发送响应
            String response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: 13\r\n" +
                            "\r\n" +
                            "Hello, World!";
            
            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
            client.write(responseBuffer);
            
            // 关闭连接
            key.cancel();
            client.close();
            
        } catch (IOException e) {
            log("Error processing request: " + e.getMessage());
            key.cancel();
            client.close();
        }
    }
} 