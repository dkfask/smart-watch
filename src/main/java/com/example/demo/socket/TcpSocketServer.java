package com.example.demo.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class TcpSocketServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(TcpSocketServer.class);

    @Value("${app.tcp.port:9090}")
    private int port;

    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private Thread acceptThread;

    @Override
    public synchronized void start() {
        if (running) return;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("0.0.0.0", port));

            clientPool = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "tcp-client-" + UUID.randomUUID());
                t.setDaemon(true);
                return t;
            });

            acceptThread = new Thread(this::acceptLoop, "tcp-acceptor");
            acceptThread.setDaemon(true);
            running = true;
            acceptThread.start();
            log.info("TCP socket server started on port {}", getBoundPort());
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
                Socket client = serverSocket.accept();
                log.info("Client connected: {}:{}", client.getInetAddress().getHostAddress(), client.getPort());
                clientPool.submit(() -> handleClient(client));
            } catch (SocketException se) {
                if (running) {
                    log.warn("Server socket exception: {}", se.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Accept failed", e);
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

            writeLine(out, "Welcome to TCP server. Type 'bye' to exit.");

            String line;
            while ((line = in.readLine()) != null) {
                String msg = line.trim();
                if (msg.equalsIgnoreCase("bye")) {
                    writeLine(out, "Goodbye!");
                    break;
                }
                writeLine(out, "echo: " + msg);
            }
        } catch (IOException e) {
            log.warn("Client I/O error: {}", e.toString());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
            log.info("Client disconnected");
        }
    }

    private void writeLine(BufferedWriter out, String s) throws IOException {
        out.write(s);
        out.write("\r\n");
        out.flush();
    }

    @Override
    public synchronized void stop() {
        if (!running) return;
        running = false;
        closeQuietly(serverSocket);
        if (acceptThread != null) {
            try { acceptThread.join(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        if (clientPool != null) {
            clientPool.shutdownNow();
            try { clientPool.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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

    private void closeQuietly(ServerSocket s) {
        if (s != null && !s.isClosed()) {
            try { s.close(); } catch (IOException ignored) {}
        }
    }
}
