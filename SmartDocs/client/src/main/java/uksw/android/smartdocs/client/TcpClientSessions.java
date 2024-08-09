package uksw.android.smartdocs.client;

import androidx.core.util.Consumer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpClientSessions implements AutoCloseable {
    public interface SessionHandler {
        void handleSession(TcpClient client) throws Exception;
    }
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Set<TcpClient> activeClients = Collections.synchronizedSet(new HashSet<>());
    private final Callable<TcpClient> clientFactory;

    public TcpClientSessions(Callable<TcpClient> clientFactory) {
        this.clientFactory = clientFactory;
    }

    public void session(SessionHandler handler, Consumer<Exception> errorListener) {
        threadPool.submit(() -> {
            TcpClient client = null;
            try {
                client = clientFactory.call();
                activeClients.add(client);
                handler.handleSession(client);
            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    errorListener.accept(e);
                }
            } finally {
                if (client != null) {
                    client.close();
                    activeClients.remove(client);
                }
            }
        });
    }

    @Override
    public void close() {
        threadPool.shutdownNow();
        for (TcpClient client : activeClients) {
            client.close();
        }
        activeClients.clear();
    }
}
