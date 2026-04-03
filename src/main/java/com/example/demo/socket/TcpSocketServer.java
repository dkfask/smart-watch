package com.example.demo.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TcpSocketServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(TcpSocketServer.class);

    @Value("${app.tcp.port:9090}")
    private int port;

    @Value("${app.tcp.max-clients:100}")
    private int maxClients;

    @Value("${app.tcp.timeout:300000}") // 5分钟超时
    private int timeout;

    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private Thread acceptThread;
    
    // 存储活跃的客户端连接
    private final Map<String, ClientConnection> clientConnections = new ConcurrentHashMap<>();
    
    // 客户端连接数计数器
    private final AtomicInteger clientCount = new AtomicInteger(0);
    
    // 消息队列，用于处理客户端消息
    private final BlockingQueue<ClientMessage> messageQueue = new LinkedBlockingQueue<>(10000);
    
    // 消息处理线程池
    private ExecutorService messageProcessorPool;

    // 客户端消息类
    private static class ClientMessage {
        private final String clientId;
        private final String message;
        private final ClientConnection connection;
        
        public ClientMessage(String clientId, String message, ClientConnection connection) {
            this.clientId = clientId;
            this.message = message;
            this.connection = connection;
        }
        
        public String getClientId() {
            return clientId;
        }
        
        public String getMessage() {
            return message;
        }
        
        public ClientConnection getConnection() {
            return connection;
        }
    }
    
    // 客户端连接类
    private static class ClientConnection {
        private final String clientId;
        private final Socket socket;
        private final BufferedReader in;
        private final BufferedWriter out;
        private volatile boolean connected = true;
        
        public ClientConnection(String clientId, Socket socket, BufferedReader in, BufferedWriter out) {
            this.clientId = clientId;
            this.socket = socket;
            this.in = in;
            this.out = out;
        }
        
        public String getClientId() {
            return clientId;
        }
        
        public Socket getSocket() {
            return socket;
        }
        
        public BufferedReader getIn() {
            return in;
        }
        
        public BufferedWriter getOut() {
            return out;
        }
        
        public boolean isConnected() {
            return connected && !socket.isClosed();
        }
        
        public void setConnected(boolean connected) {
            this.connected = connected;
        }
        
        public void close() {
            connected = false;
            try { in.close(); } catch (IOException ignored) {}
            try { out.close(); } catch (IOException ignored) {}
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    @Override
    public synchronized void start() {
        if (running) return;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
            serverSocket.setReuseAddress(true);

            // 固定大小的线程池，避免线程数过多
            clientPool = Executors.newFixedThreadPool(maxClients, r -> {
                Thread t = new Thread(r, "tcp-client-" + UUID.randomUUID());
                t.setDaemon(true);
                return t;
            });

            // 消息处理线程池
            messageProcessorPool = Executors.newFixedThreadPool(5, r -> {
                Thread t = new Thread(r, "tcp-message-processor-");
                t.setDaemon(true);
                return t;
            });

            // 启动消息处理线程
            for (int i = 0; i < 5; i++) {
                messageProcessorPool.submit(this::processMessages);
            }

            acceptThread = new Thread(this::acceptLoop, "tcp-acceptor");
            acceptThread.setDaemon(true);
            running = true;
            acceptThread.start();
            log.info("TCP socket server started on port {}, max clients: {}", getBoundPort(), maxClients);
        } catch (IOException e) {
            running = false;
            closeQuietly(serverSocket);
            log.error("Failed to start TCP server on port {}", port, e);
            throw new IllegalStateException("Failed to start TCP server", e);
        }
    }

    private void acceptLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                // 检查客户端连接数是否超过限制
                if (clientCount.get() >= maxClients) {
                    log.warn("Client connection limit reached: {}", maxClients);
                    // 短暂睡眠，避免忙等
                    Thread.sleep(100);
                    continue;
                }
                
                Socket client = serverSocket.accept();
                // 设置连接超时
                client.setSoTimeout(timeout);
                client.setTcpNoDelay(true); // 禁用Nagle算法，提高实时性
                
                String clientId = UUID.randomUUID().toString();
                log.info("Client connected: {}:{}, clientId: {}", 
                        client.getInetAddress().getHostAddress(), client.getPort(), clientId);
                
                // 增加客户端计数
                clientCount.incrementAndGet();
                
                clientPool.submit(() -> handleClient(client, clientId));
            } catch (SocketException se) {
                if (running) {
                    log.warn("Server socket exception: {}", se.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Accept failed", e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleClient(Socket client, String clientId) {
        ClientConnection connection = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
            
            // 创建客户端连接对象
            connection = new ClientConnection(clientId, client, in, out);
            // 存储客户端连接
            clientConnections.put(clientId, connection);
            
            // 发送欢迎消息
            writeLine(out, "Welcome to TCP server. Type 'bye' to exit.");

            String line;
            while (running && connection.isConnected() && (line = in.readLine()) != null) {
                String msg = line.trim();
                if (msg.equalsIgnoreCase("bye")) {
                    writeLine(out, "Goodbye!");
                    break;
                }
                
                // 将消息加入队列，异步处理
                boolean added = messageQueue.offer(new ClientMessage(clientId, msg, connection), 1, TimeUnit.SECONDS);
                if (!added) {
                    log.warn("Message queue full, dropping message from client: {}", clientId);
                    writeLine(out, "Error: Message queue full");
                }
            }
        } catch (IOException e) {
            log.warn("Client I/O error: {}, clientId: {}", e.toString(), clientId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 清理客户端连接
            if (connection != null) {
                connection.close();
            }
            clientConnections.remove(clientId);
            clientCount.decrementAndGet();
            log.info("Client disconnected, clientId: {}, current clients: {}", clientId, clientCount.get());
        }
    }

    private void writeLine(BufferedWriter out, String s) throws IOException {
        out.write(s);
        out.write("\r\n");
        out.flush();
    }
    
    /**
     * 处理消息队列中的消息
     */
    private void processMessages() {
        while (running) {
            try {
                ClientMessage message = messageQueue.take();
                if (message != null) {
                    processMessage(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing message", e);
            }
        }
    }
    
    /**
     * 处理单个消息
     */
    private void processMessage(ClientMessage message) {
        String clientId = message.getClientId();
        String msg = message.getMessage();
        ClientConnection connection = message.getConnection();
        
        if (connection != null && connection.isConnected()) {
            try {
                // 简单的回显功能
                writeLine(connection.getOut(), "echo: " + msg);
                log.debug("Processed message from client {}: {}", clientId, msg);
            } catch (IOException e) {
                log.warn("Error sending response to client {}: {}", clientId, e.getMessage());
                // 关闭连接
                connection.close();
                clientConnections.remove(clientId);
                clientCount.decrementAndGet();
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) return;
        running = false;
        
        // 关闭服务器Socket
        closeQuietly(serverSocket);
        
        // 等待接受线程结束
        if (acceptThread != null) {
            try { acceptThread.join(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        
        // 关闭所有客户端连接
        for (ClientConnection connection : clientConnections.values()) {
            try {
                connection.close();
            } catch (Exception ignored) {}
        }
        clientConnections.clear();
        clientCount.set(0);
        
        // 关闭客户端线程池
        if (clientPool != null) {
            clientPool.shutdownNow();
            try { clientPool.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        // 关闭消息处理线程池
        if (messageProcessorPool != null) {
            messageProcessorPool.shutdownNow();
            try { messageProcessorPool.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        log.info("TCP socket server stopped");
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public boolean isAutoStartup() { return true; }

    public int getBoundPort() {
        return (serverSocket != null && serverSocket.isBound()) ? serverSocket.getLocalPort() : -1;
    }
    
    /**
     * 获取当前客户端连接数
     */
    public int getClientCount() {
        return clientCount.get();
    }
    
    /**
     * 获取最大客户端连接数
     */
    public int getMaxClients() {
        return maxClients;
    }
    
    /**
     * 获取客户端连接列表
     */
    public Map<String, ClientConnection> getClientConnections() {
        return new ConcurrentHashMap<>(clientConnections);
    }
    
    /**
     * 向指定客户端发送消息
     */
    public boolean sendMessage(String clientId, String message) {
        ClientConnection connection = clientConnections.get(clientId);
        if (connection != null && connection.isConnected()) {
            try {
                writeLine(connection.getOut(), message);
                return true;
            } catch (IOException e) {
                log.warn("Error sending message to client {}: {}", clientId, e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    /**
     * 向所有客户端广播消息
     */
    public void broadcastMessage(String message) {
        for (ClientConnection connection : clientConnections.values()) {
            if (connection.isConnected()) {
                try {
                    writeLine(connection.getOut(), message);
                } catch (IOException e) {
                    log.warn("Error broadcasting message to client {}: {}", connection.getClientId(), e.getMessage());
                }
            }
        }
    }

    private void closeQuietly(ServerSocket s) {
        if (s != null && !s.isClosed()) {
            try { s.close(); } catch (IOException ignored) {}
        }
    }
    
    private void closeQuietly(Socket s) {
        if (s != null && !s.isClosed()) {
            try { s.close(); } catch (IOException ignored) {}
        }
    }
}
