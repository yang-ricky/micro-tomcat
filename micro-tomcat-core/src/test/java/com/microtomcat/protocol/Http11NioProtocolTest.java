package com.microtomcat.protocol;

import com.microtomcat.processor.Processor;
import com.microtomcat.processor.ProcessorPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class Http11NioProtocolTest {
    
    private Http11NioProtocol protocol;
    private static final int TEST_PORT = 8890;
    
    @Mock
    private ProcessorPool processorPool;
    
    @Mock
    private Processor processor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        protocol = new Http11NioProtocol();
        protocol.setPort(TEST_PORT);
        protocol.setProcessorPool(processorPool);
        
        when(processorPool.getProcessor(anyInt())).thenReturn(processor);
    }

    @After
    public void tearDown() throws Exception {
        protocol.stop();
    }

    @Test
    public void testIsNonBlockingIO() throws Exception {
        protocol.init();
        Thread serverThread = new Thread(() -> {
            try {
                protocol.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        
        Thread.sleep(1000); // 等待服务器启动
        
        try (SocketChannel client = SocketChannel.open()) {
            // 1. 验证是非阻塞模式
            client.configureBlocking(false);
            assertFalse("Channel should be in non-blocking mode", client.isBlocking());
            
            // 2. 验证连接是非阻塞的
            boolean connected = client.connect(new InetSocketAddress("localhost", TEST_PORT));
            if (!connected) {
                // 非阻塞模式下，connect 可能立即返回 false
                while (!client.finishConnect()) {
                    Thread.sleep(100);
                }
            }
            assertTrue("Connection should be established", client.isConnected());
            
            // 3. 验证写操作是非阻塞的
            ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
            String request = 
                "GET /test HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Connection: close\r\n" +
                "\r\n";
            writeBuffer.put(request.getBytes());
            writeBuffer.flip();
            
            long startTime = System.currentTimeMillis();
            while (writeBuffer.hasRemaining()) {
                client.write(writeBuffer);
            }
            long writeTime = System.currentTimeMillis() - startTime;
            assertTrue("Write operation should not block", writeTime < 100);
            
            // 4. 验证读操作是非阻塞的
            ByteBuffer readBuffer = ByteBuffer.allocate(1024);
            startTime = System.currentTimeMillis();
            int bytesRead = client.read(readBuffer);
            long readTime = System.currentTimeMillis() - startTime;
            assertTrue("Read operation should not block", readTime < 100);
            // 非阻塞模式下，如果没有数据可读，应该立即返回 0
            assertTrue("Read should return immediately if no data", bytesRead >= 0);
        }
    }

    @Test
    public void testConcurrentConnections() throws Exception {
        protocol.init();
        Thread serverThread = new Thread(() -> {
            try {
                protocol.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        
        Thread.sleep(1000);
        
        int numConnections = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numConnections);
        SocketChannel[] channels = new SocketChannel[numConnections];
        
        // 创建多个非阻塞连接
        for (int i = 0; i < numConnections; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    channels[index] = SocketChannel.open();
                    channels[index].configureBlocking(false);
                    channels[index].connect(new InetSocketAddress("localhost", TEST_PORT));
                    startLatch.await();
                    
                    // 等待连接完成
                    while (!channels[index].finishConnect()) {
                        Thread.yield();
                    }
                    
                    completionLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        startLatch.countDown();
        assertTrue("Concurrent connections failed to complete", 
            completionLatch.await(5, TimeUnit.SECONDS));
        
        // 清理连接
        for (SocketChannel channel : channels) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    @Test
    public void testPartialReads() throws Exception {
        protocol.init();
        protocol.start();
        
        try (SocketChannel client = SocketChannel.open()) {
            client.configureBlocking(false);
            client.connect(new InetSocketAddress("localhost", TEST_PORT));
            
            while (!client.finishConnect()) {
                Thread.yield();
            }
            
            // 准备一个大请求
            StringBuilder largeRequest = new StringBuilder();
            largeRequest.append("POST /test HTTP/1.1\r\n");
            largeRequest.append("Host: localhost\r\n");
            largeRequest.append("Content-Length: 1048576\r\n"); // 1MB
            largeRequest.append("\r\n");
            for (int i = 0; i < 1048576; i++) {
                largeRequest.append('X');
            }
            
            // 分多次写入
            ByteBuffer buffer = ByteBuffer.wrap(largeRequest.toString().getBytes());
            while (buffer.hasRemaining()) {
                int written = client.write(buffer);
                assertTrue("Write should be non-blocking", written >= 0);
                Thread.sleep(10); // 模拟网络延迟
            }
            
            // 验证服务器能够正确处理分片读取
            ByteBuffer readBuffer = ByteBuffer.allocate(1024);
            boolean responseReceived = false;
            long deadline = System.currentTimeMillis() + 5000;
            
            while (System.currentTimeMillis() < deadline && !responseReceived) {
                int bytesRead = client.read(readBuffer);
                if (bytesRead > 0) {
                    readBuffer.flip();
                    String response = new String(readBuffer.array(), 0, readBuffer.limit());
                    if (response.contains("HTTP/1.1 200")) {
                        responseReceived = true;
                    }
                    readBuffer.clear();
                }
                Thread.sleep(100);
            }
            
            assertTrue("Should receive response", responseReceived);
        }
    }
} 