package com.microtomcat.protocol;

import com.microtomcat.processor.Processor;
import com.microtomcat.processor.ProcessorPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class Http11ProtocolTest {
    
    private Http11Protocol protocol;
    private static final int TEST_PORT = 8889;
    
    @Mock
    private ProcessorPool processorPool;
    
    @Mock
    private Processor processor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        protocol = new Http11Protocol();
        protocol.setPort(TEST_PORT);
        protocol.setProcessorPool(processorPool);
        
        when(processorPool.getProcessor(anyInt())).thenReturn(processor);
    }

    @After
    public void tearDown() throws Exception {
        protocol.stop();
    }

    @Test
    public void testBasicHttpRequest() throws Exception {
        // 启动协议
        CountDownLatch serverStarted = new CountDownLatch(1);
        Thread serverThread = new Thread(() -> {
            try {
                protocol.init();
                protocol.start();
                serverStarted.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        
        assertTrue("Server failed to start", serverStarted.await(5, TimeUnit.SECONDS));
        
        // 发送请求
        try (Socket client = new Socket("localhost", TEST_PORT)) {
            // 设置超时
            client.setSoTimeout(5000);
            
            // 发送 HTTP 请求
            try (OutputStream out = client.getOutputStream()) {
                String request = 
                    "GET /test HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
                out.write(request.getBytes());
                out.flush();
            }
            
            // 读取响应
            try (InputStream in = client.getInputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer);
                assertTrue("No response received", bytesRead > 0);
            }
        }
        
        // 验证处理器被调用
        verify(processor, timeout(1000)).process(any(Socket.class));
    }

    @Test
    public void testMaxConnections() throws Exception {
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
        
        // 创建超过最大连接数的连接
        int maxConnections = 10000;
        Socket[] sockets = new Socket[maxConnections + 1];
        int successfulConnections = 0;
        
        try {
            for (int i = 0; i <= maxConnections; i++) {
                try {
                    sockets[i] = new Socket("localhost", TEST_PORT);
                    successfulConnections++;
                } catch (IOException e) {
                    // 预期在达到最大连接数后会失败
                    break;
                }
            }
            
            assertTrue("Should accept up to max connections", 
                successfulConnections > 0 && successfulConnections <= maxConnections);
            
        } finally {
            // 清理连接
            for (Socket socket : sockets) {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    @Test
    public void testKeepAliveConnection() throws Exception {
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
        
        try (Socket client = new Socket("localhost", TEST_PORT)) {
            client.setSoTimeout(5000);
            
            // 发送两个 keep-alive 请求
            try (OutputStream out = client.getOutputStream()) {
                String request1 = 
                    "GET /test1 HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Connection: keep-alive\r\n" +
                    "\r\n";
                out.write(request1.getBytes());
                out.flush();
                
                // 读取第一个响应
                readResponse(client);
                
                String request2 = 
                    "GET /test2 HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
                out.write(request2.getBytes());
                out.flush();
                
                // 读取第二个响应
                readResponse(client);
            }
        }
        
        // 验证处理器被调用两次
        verify(processor, timeout(1000).times(2)).process(any(Socket.class));
    }

    @Test
    public void testServerBusy() throws Exception {
        // 模拟处理器池已满
        when(processorPool.getProcessor(anyInt())).thenReturn(null);
        
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
        
        try (Socket client = new Socket("localhost", TEST_PORT)) {
            client.setSoTimeout(5000);
            
            // 发送请求
            try (OutputStream out = client.getOutputStream()) {
                String request = 
                    "GET /test HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
                out.write(request.getBytes());
                out.flush();
            }
            
            // 读取响应，应该是 503 Service Unavailable
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(client.getInputStream()))) {
                String statusLine = reader.readLine();
                assertNotNull("No response received", statusLine);
                assertTrue("Expected 503 response", 
                    statusLine.contains("503") || statusLine.contains("Service Unavailable"));
            }
        }
    }

    @Test
    public void testIsBlockingIO() throws Exception {
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
        
        try (Socket client = new Socket("localhost", TEST_PORT)) {
            // 1. 验证 Socket 是阻塞模式
            assertTrue("Socket should be in blocking mode", 
                client.getChannel() == null || client.getChannel().isBlocking());
            
            // 2. 验证读取操作是阻塞的
            client.setSoTimeout(1000); // 设置超时以防止测试卡住
            try (InputStream in = client.getInputStream()) {
                // 尝试读取数据，应该阻塞直到超时
                long startTime = System.currentTimeMillis();
                try {
                    in.read();
                    fail("Should throw SocketTimeoutException");
                } catch (SocketTimeoutException e) {
                    // 计算实际阻塞时间
                    long blockTime = System.currentTimeMillis() - startTime;
                    // 验证确实阻塞了接近超时时间
                    assertTrue("Read operation should block for close to timeout period",
                        blockTime >= 900); // 允许100ms的误差
                }
            }
            
            // 3. 验证写操作是阻塞的
            try (OutputStream out = client.getOutputStream()) {
                byte[] largeData = new byte[1024 * 1024]; // 1MB 数据
                // 持续写入直到缓冲区满，应该阻塞
                long startTime = System.currentTimeMillis();
                try {
                    while (System.currentTimeMillis() - startTime < 1000) {
                        out.write(largeData);
                        out.flush();
                    }
                } catch (IOException e) {
                    // 期望在缓冲区满时发生阻塞或抛出异常
                    assertTrue("Write operation should block or throw exception",
                        e.getMessage().contains("broken pipe") || 
                        e.getMessage().contains("connection reset"));
                }
            }
        }
    }

    private void readResponse(Socket client) throws IOException {
        try (InputStream in = client.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                // 读取直到响应结束
                if (new String(buffer, 0, bytesRead).contains("\r\n\r\n")) {
                    break;
                }
            }
        }
    }
} 