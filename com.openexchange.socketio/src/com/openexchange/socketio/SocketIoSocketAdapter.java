
/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.socketio;

import static com.openexchange.socketio.SocketIoPacketUtils.isPacketDataValid;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.ExceptionUtils;
import com.openexchange.exception.OXException;
import com.openexchange.websockets.MessageTranscoder;
import com.openexchange.websockets.WebSocket;
import com.openexchange.websockets.WebSocketConnectException;
import com.openexchange.websockets.WebSocketListener;
import io.socket.engineio.server.EngineIoServer;
import io.socket.engineio.server.EngineIoSocket;
import io.socket.engineio.server.EngineIoWebSocket;
import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoServer;
import io.socket.socketio.server.SocketIoSocket;
import io.socket.socketio.server.parser.Parser;



/**
 * {@link SocketIoSocketAdapter}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.4
 */
public class SocketIoSocketAdapter extends EngineIoWebSocket implements WebSocketListener {

    private static final Logger LOG = LoggerFactory.getLogger(SocketIoSocketAdapter.class);

    private static final String DEFAULT_NAMESPACE = "/";

    /** The Engine.IO protocol version number of <code>"4"</code> */
    private static final int EIO_PROTOCOL_4 = 4;

    /** The Engine.IO protocol version number of <code>"3"</code> */
    private static final int EIO_PROTOCOL_3 = 3;

    /** The identifier for <code>"connection"</code> event */
    private static final String EVENT_CONNECTION = "connection";

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final EngineIoServer engineIoServer;
    private final SocketIoServer socketIoServer;

    private final AtomicBoolean open;
    private final AtomicReference<WebSocket> socketReference;

    /**
     * Initializes a new {@link SocketIoSocketAdapter}.
     *
     * @param engineIoServer The Engine.IO server instance
     * @param socketIoServer The Socket.IO server instance
     */
    public SocketIoSocketAdapter(EngineIoServer engineIoServer, SocketIoServer socketIoServer) {
        super();
        this.engineIoServer = engineIoServer;
        this.socketIoServer = socketIoServer;
        this.open = new AtomicBoolean(false);
        this.socketReference = new AtomicReference<>();
        LOG.trace("new SocketIOSocketAdapter()");
    }

    /* WebSocketListener */

    private static boolean isAppropriateWebSocket(WebSocket socket) {
        String path = socket.getPath();
        return (null != path && path.startsWith("/socket.io"));
    }

    @Override
    public void onWebSocketConnect(WebSocket socket) {
        LOG.trace("onWebSocketConnect(): {}", socket);
        if (!isAppropriateWebSocket(socket)) {
            LOG.debug("Inappropriate WebSocket on connect: {}", socket);
            return;
        }

        this.socketReference.set(socket);

        int engineIOProtocolVersion = getEngineIOProtocolFromQuery(socket);
        switch (engineIOProtocolVersion) {
            case EIO_PROTOCOL_3:
                // Handle EIO v3
                connectEioV3(socket);
                break;
            case EIO_PROTOCOL_4:
                // Handle EIO v4
                connectEioV4(socket);
                break;
            default:
                WebSocketConnectException wsce = new WebSocketConnectException(500, "Unsupported Engine.IO protocol version: " + engineIOProtocolVersion);
                LOG.error("Engine.IO WebSocket connect failed", wsce);
                throw wsce;
        }
    }

    private void connectEioV4(WebSocket socket) {
        LOG.debug("EIO v4 connect of WebSocket: {}", socket);

        // Message transcoder for Engine.IO v4 protocol
        EioV4MessageTranscoder eioV4MessageTranscoder = new EioV4MessageTranscoder(socketIoServer, this);

        // Initialize "connection" listener for Engine.IO server
        Listener engineIoListener = new AbstractThreadAwareListener() {

            @Override
            protected void onAnotherThread(Object[] args) {
                LOG.debug("Cannot handle \"{}\" event on Engine.IO server: Another thread.", EVENT_CONNECTION);
            }

            @Override
            protected void onCreatorThread(Object[] args) {
                LOG.debug("Handling \"{}\" event on Engine.IO server", EVENT_CONNECTION);
                EngineIoSocket eSocket = (EngineIoSocket) args[0];
                eioV4MessageTranscoder.connectionInitialized(eSocket);
                LOG.debug("Assigned Engine.IO socket '{}' to WebSocket: {}", eSocket.getId(), socket);
            }
        };

        engineIoServer.on(EVENT_CONNECTION, engineIoListener);
        try {
            socket.setMessageTranscoder(eioV4MessageTranscoder);
            engineIoServer.handleWebSocket(this);
            open.set(true);
            LOG.debug("Initialized Engine.IO for WebSocket: {}", socket);
        } catch (Exception e) {
            WebSocketConnectException wsce = new WebSocketConnectException(500, "Engine.IO socket intitialization failed", e);
            LOG.error("Engine.IO socket intitialization failed", wsce);
            throw wsce;
        } finally {
            engineIoServer.off(EVENT_CONNECTION, engineIoListener);
        }
    }

    private void connectEioV3(WebSocket socket) {
        LOG.debug("EIO v3 connect of WebSocket: {}", socket);

        // Message transcoder for Engine.IO v3 protocol
        EioV3MessageTranscoder eioV3MessageTranscoder = new EioV3MessageTranscoder(this);

        // Initialize "connection" listener for Socket.IO default namespace
        Listener socketListener = new AbstractThreadAwareListener() {

            @Override
            protected void onAnotherThread(Object[] args) {
                LOG.debug("Cannot handle \"{}\" event on Socket.IO server: Another thread.", EVENT_CONNECTION);
            }

            @Override
            protected void onCreatorThread(Object[] args) {
                LOG.debug("Handling \"{}\" event on Socket.IO default namespace", EVENT_CONNECTION);
                SocketIoSocket sSocket = (SocketIoSocket) args[0];
                eioV3MessageTranscoder.connectionInitialized(sSocket);
                LOG.debug("Assigned Socket.IO socket '{}' to WebSocket: {}", sSocket.getId(), socket);
            }
        };

        SocketIoNamespace namespace = socketIoServer.namespace(DEFAULT_NAMESPACE);
        namespace.on(EVENT_CONNECTION, socketListener);
        try {
            socket.setMessageTranscoder(eioV3MessageTranscoder);
            engineIoServer.handleWebSocket(this);
            open.set(true);
            LOG.debug("Initialized Socket.IO for WebSocket: {}", socket);
        } catch (Exception e) {
            WebSocketConnectException wsce = new WebSocketConnectException(500, "Socket.IO socket intitialization failed", e);
            LOG.error("Socket.IO socket intitialization failed", wsce);
            throw wsce;
        } finally {
            namespace.off(EVENT_CONNECTION, socketListener);
        }
    }

    /**
     * Determines the number of the Engine.IO protocol version advertised by connect of given WebSocket instance.
     * <p>
     * The Engine.IO protocol version number is advertised by client through <code>"EIO"</code> query parameter. If query is missing or no
     * such URL parameter is present, the default Engine.IO protocol version number of <code>"3"</code> is assumed.
     *
     * @param socket The WebSocket instance that got connected
     * @return The Engine.IO protocol version number
     */
    private static int getEngineIOProtocolFromQuery(WebSocket socket) {
        Map<String, String> query = socket.getParameters();
        if (query == null) {
            // No query parameters available
            return EIO_PROTOCOL_3;
        }
        return "4".equals(query.get("EIO")) ? EIO_PROTOCOL_4 : EIO_PROTOCOL_3;
    }

    @Override
    public void onWebSocketClose(WebSocket socket) {
        LOG.trace("onWebSocketClose(): {}", socket);
        if (open.compareAndSet(true, false)) {
            emit("close");
        }
        socket.setMessageTranscoder(null);
        this.socketReference.set(null);
        LOG.debug("Shutdown Socket.IO for closed WebSocket: {}", socket);
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        // not called but handled by onInboundMessage()
        LOG.warn("onMessage() called even though a transcoder is set!");
    }

    /* EngineIoWebSocket */

    @Override
    public Map<String, List<String>> getConnectionHeaders() {
        WebSocket socket = this.socketReference.get();
        if (socket == null) {
            throw new IllegalStateException("WebSocket instance not set");
        }

        Map<String, List<String>> headers = socket.getHeaders();
        return headers == null ? Collections.emptyMap() : headers;
    }

    @Override
    public Map<String, String> getQuery() {
        WebSocket socket = this.socketReference.get();
        if (socket == null) {
            throw new IllegalStateException("WebSocket instance not set");
        }

        Map<String, String> parameters = socket.getParameters();
        return parameters == null ? Collections.emptyMap() : parameters;
    }

    @Override
    public void write(String message) throws IOException {
        WebSocket socket = this.socketReference.get();
        if (socket == null) {
            throw new IOException("WebSocket is closed");
        }

        try {
            LOG.debug("Sending message: {}", message);
            socket.sendMessageRaw(message);
            debugSentEioMessage(message, socket);
        } catch (OXException e) {
            IOException ioe = ExceptionUtils.extractFrom(e, IOException.class);
            throw ioe != null ? ioe : new IOException(e.getSoleMessage(), e);
        }
    }

    private static void debugSentEioMessage(String message, WebSocket socket) {
        LOG.debug("Sent Engine.IO message for user {} in context {}: {}", Integer.valueOf(socket.getUserId()), Integer.valueOf(socket.getContextId()), message);
    }

    @Override
    public void write(byte[] message) throws IOException {
        WebSocket socket = this.socketReference.get();
        if (socket == null) {
            throw new IOException("WebSocket is closed");
        }

        throw new IOException("Support for byte messages is not implemented!");
    }

    @Override
    public void close() {
        WebSocket socket = this.socketReference.get();
        if (socket != null) {
            LOG.trace("close(): {}", socket);
            LOG.debug("Shutdown Socket.IO due to server-side disconnect attempt for WebSocket: {}", socket);
            socket.setMessageTranscoder(null);
            this.socketReference.set(null);
            socket.close();
        } else {
            LOG.trace("close(): null");
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private abstract static class AbstractEngineIoMessageTranscoder<SOCKET> implements MessageTranscoder {

        /** The Engine.IO WebSocket to emit "message" events to */
        protected final EngineIoWebSocket engineIoWebSocket;

        /** The protocol version */
        protected final Integer version;

        private final AtomicReference<SOCKET> socketReference;
        private final CountDownLatch latch;

        /**
         * Initializes a new {@link AbstractEngineIoMessageTranscoder}.
         *
         * @param engineIoWebSocket The Engine.IO WebSocket to emit "message" events to
         * @param version The version of the Engine.IO protocol supported by this transcoder
         */
        AbstractEngineIoMessageTranscoder(EngineIoWebSocket engineIoWebSocket, int version) {
            super();
            this.engineIoWebSocket = engineIoWebSocket;
            this.version = Integer.valueOf(version);
            this.socketReference = new AtomicReference<>();
            this.latch = new CountDownLatch(1);
        }

        /**
         * Signals given initialized socket that is now ready to use.
         *
         * @param socket The initialized socket
         */
        void connectionInitialized(SOCKET socket) {
            socketReference.set(socket);
            latch.countDown();
        }

        /**
         * Awaits presence of initialized socket.
         *
         * @return The initialized socket
         * @throws InterruptedException If awaiting presence gets interrupted
         */
        protected SOCKET awaitSocket() throws InterruptedException {
            SOCKET socket = socketReference.get();
            if (socket != null) {
                return socket;
            }

            latch.await();
            return socketReference.get();
        }

        /**
         * Emits the given incoming message to the Engine.IO WebSocket.
         *
         * @param message The message
         * @param socket The socket for which the message has been received
         */
        protected void emitIncominMessage(String message, SOCKET socket) {
            engineIoWebSocket.emit("message", message);
        }

        @Override
        public String onInboundMessage(WebSocket socket, String message) {
            LOG.trace("Engine.IO v{} onInboundMessage(): {}", version, socket);
            LOG.debug("Engine.IO v{} Received message: {}", version, message);
            debugReceivedEioMessage(message, socket);

            try {
                // Await socket
                SOCKET awaitSocket = awaitSocket();
                emitIncominMessage(message, awaitSocket);
                return null;
            } catch (InterruptedException e) {
                // Keep interrupted state
                Thread.currentThread().interrupt();
                LOG.error("Cannot handle message since Engine.IO v{} initialization has been interrupted: {}", version, message, e);
                return message;
            } catch (Exception e) {
                LOG.error("Cannot handle Engine.IO v{} message: {}", version, message, e);
                return message;
            }
        }

        private static void debugReceivedEioMessage(String message, WebSocket socket) {
            LOG.debug("Received Engine.IO message for user {} in context {}: {}", Integer.valueOf(socket.getUserId()), Integer.valueOf(socket.getContextId()), message);
        }

        /**
         * Parses given outbound message into its event name and arguments components.
         *
         * @param message The message to parse
         * @return The event name and arguments
         */
        protected static Optional<NameAndArgs> parseOutboundMessage(String message) {
            try {
                // Expect content to be JSON-formatted text; e.g. {"name":"ox:mail:new", "args":[...], "namespace":"/"}
                JSONObject jEvent = JSONServices.parseObject(message);

                // Check advertised namespace
                String namespace = jEvent.optString("namespace", DEFAULT_NAMESPACE);
                if (!DEFAULT_NAMESPACE.equals(namespace)) {
                    LOG.error("Socket.IO message for namespace '{}' cannot be sent. Only '{}' is supported.", namespace, DEFAULT_NAMESPACE);
                    return Optional.empty();
                }

                // Get name & args
                String name = jEvent.getString("name");
                JSONArray jArgs = jEvent.getJSONArray("args");

                // Create args
                int argsLen = jArgs.length();
                Object[] args = new Object[argsLen];
                for (int i = 0; i < argsLen; i++) {
                    args[i] = jArgs.get(i);
                }

                return Optional.of(new NameAndArgs(name, args));
            } catch (JSONException e) {
                LOG.error("Invalid message to send: {}", message, e);
                return Optional.empty();
            }
        }

        /**
         * Sends the given message as Socket.IO packet to remote client. Returns message as-is in case given message has an illegal format.
         *
         * @param message The message to send to remote client
         * @param socketIoSocket The Socket.IO socket
         * @return <code>null</code> if successfully sent to remote client; otherwise message as-is if message has an illegal format
         */
        protected static String sendSocketIoPacketElseReturnMessage(String message, SocketIoSocket socketIoSocket) {
            Optional<NameAndArgs> optNaa = parseOutboundMessage(message);
            if (optNaa.isEmpty()) {
                return message;
            }

            // Send data to remote client
            NameAndArgs nameAndArgs = optNaa.get();
            socketIoSocket.send(nameAndArgs.name(), nameAndArgs.args());
            return null;
        }
    }

    private static class EioV4MessageTranscoder extends AbstractEngineIoMessageTranscoder<EngineIoSocket> {

        /**
         * The Engine.IO packet type for <code>MESSAGE</code>.
         * <p>
         * See <code>io.socket.engineio.server.parser.Parser.PACKETS_REVERSE</code>
         */
        private static final int ENGINEIO_PACKET_TYPE_MESSAGE = 4;

        /**
         * The Socket.IO packet type for <code>CONNECT</code>.
         * <p>
         * See <code>io.socket.socketio.server.parser.Parser.CONNECT</code>
         */
        private static final int SOCKETIO_PACKET_TYPE_CONNECT = Parser.CONNECT;

        private final SocketIoServer socketIoServer;
        private final AtomicReference<SocketIoSocket> socketIoSocketReference;
        private final String expectedConnectMessage;

        /**
         * Initializes a new {@link EioV4MessageTranscoder}.
         *
         * @param socketIoServer The Socket.IO server instance needed to determine associated Socket.IO socket
         * @param engineIoWebSocket The Engine.IO WebSocket to emit "message" events to
         */
        EioV4MessageTranscoder(SocketIoServer socketIoServer, EngineIoWebSocket engineIoWebSocket) {
            super(engineIoWebSocket, 4);
            this.socketIoServer = socketIoServer;
            socketIoSocketReference = new AtomicReference<>();
            expectedConnectMessage = new StringBuilder(2).append(ENGINEIO_PACKET_TYPE_MESSAGE).append(SOCKETIO_PACKET_TYPE_CONNECT).toString();
        }

        @Override
        public String getId() {
            return "engine.io-v4";
        }

        @Override
        protected void emitIncominMessage(String message, EngineIoSocket socket) {
            if (!expectedConnectMessage.equals(message)) {
                // Regular "emit"
                engineIoWebSocket.emit("message", message);
                return;
            }

            synchronized (this) {
                // Received Socket.IO CONNECT packet. Check if Socket.IO socket has already been assigned.
                if (socketIoSocketReference.get() != null) {
                    // Received CONNECT packet, but Socket.IO socket already assigned
                    throw new IllegalStateException("Received Socket.IO CONNECT packet, but Socket.IO socket already assigned");
                }

                // Initialize "connection" listener for Socket.IO default namespace
                Listener socketListener = new AbstractThreadAwareListener() {

                    @Override
                    protected void onAnotherThread(Object[] args) {
                        LOG.debug("Cannot handle \"{}\" event on Socket.IO server: Another thread.", EVENT_CONNECTION);
                    }

                    @Override
                    protected void onCreatorThread(Object[] args) {
                        LOG.debug("Handling \"{}\" event on Socket.IO default namespace", EVENT_CONNECTION);
                        SocketIoSocket sSocket = (SocketIoSocket) args[0];
                        socketIoSocketReference.set(sSocket);
                        LOG.debug("Assigned Socket.IO socket '{}' to Engine.IO socket '{}'", sSocket.getId(), socket.getId());
                    }
                };

                SocketIoNamespace namespace = socketIoServer.namespace(DEFAULT_NAMESPACE);
                namespace.on(EVENT_CONNECTION, socketListener);
                try {
                    engineIoWebSocket.emit("message", message);
                } finally {
                    namespace.off(EVENT_CONNECTION, socketListener);
                }
            }
        }

        @Override
        public String onOutboundMessage(WebSocket socket, String message) {
            LOG.trace("Engine.IO v{} onOutboundMessage(): {}", version, socket);
            LOG.debug("Engine.IO v{} Send message: {}", version, message);

            SocketIoSocket socketIoSocket = socketIoSocketReference.get();
            if (socketIoSocket != null) {
                return sendSocketIoPacketElseReturnMessage(message, socketIoSocket);
            }

            // Socket.IO socket instance not yet available. Manually build appropriate Socket.IO packet and send it.
            try {
                // Await Engine.IO socket
                EngineIoSocket engineIoSocket = awaitSocket();
                return sendSocketIoPacketElseReturnMessage(message, engineIoSocket);
            } catch (InterruptedException e) {
                // Keep interrupted state
                Thread.currentThread().interrupt();
                LOG.error("Cannot send message since Socket.IO socket initialization has been interrupted: {}", message, e);
                return message;
            }
        }

        /**
         * Sends the given message as Socket.IO packet to remote client. Returns message as-is in case given message has an illegal format.
         *
         * @param message The message to send to remote client
         * @param engineIoSocket The Engine.IO socket
         * @return <code>null</code> if successfully sent to remote client; otherwise message as-is if message has an illegal format
         */
        private static String sendSocketIoPacketElseReturnMessage(String message, EngineIoSocket engineIoSocket) {
            Optional<NameAndArgs> optNaa = parseOutboundMessage(message);
            if (optNaa.isEmpty()) {
                return message;
            }

            // Create packet's JSON data
            JSONArray jPacketData;
            {
                NameAndArgs nameAndArgs = optNaa.get();
                Object[] args = nameAndArgs.args();
                int argsLen = args.length;
                jPacketData = new JSONArray(argsLen + 1);
                jPacketData.put(nameAndArgs.name());
                for (int i = 0; i < argsLen; i++) {
                    jPacketData.put(args[i]);
                }
            }

            if (!isPacketDataValid(jPacketData)) {
                LOG.error("args contain invalid data type.");
                return message;
            }

            // Create text representation for Socket.IO packet
            StringBuilder str = new StringBuilder().append(Parser.EVENT).append(jPacketData);

            // Create Engine.IO packet
            io.socket.engineio.server.parser.Packet<Object> engineIoPacket = new io.socket.engineio.server.parser.Packet<>(io.socket.engineio.server.parser.Packet.MESSAGE);
            engineIoPacket.data = str.toString();

            // Send data to remote client
            engineIoSocket.send(engineIoPacket);
            return null;
        }
    }

    private static class EioV3MessageTranscoder extends AbstractEngineIoMessageTranscoder<SocketIoSocket> {

        /**
         * Initializes a new {@link EioV3MessageTranscoder}.
         *
         * @param engineIoWebSocket The Engine.IO WebSocket to emit "message" events to
         */
        EioV3MessageTranscoder(EngineIoWebSocket engineIoWebSocket) {
            super(engineIoWebSocket, 3);
        }

        @Override
        public String getId() {
            return "engine.io-v3";
        }

        @Override
        public String onOutboundMessage(WebSocket socket, String message) {
            LOG.trace("Engine.IO v{} onOutboundMessage(): {}", version, socket);
            LOG.debug("Engine.IO v{} Send message: {}", version, message);

            try {
                // Await Socket.IO socket
                SocketIoSocket socketIoSocket = awaitSocket();
                return sendSocketIoPacketElseReturnMessage(message, socketIoSocket);
            } catch (InterruptedException e) {
                // Keep interrupted state
                Thread.currentThread().interrupt();
                LOG.error("Cannot send message since Socket.IO socket initialization has been interrupted: {}", message, e);
                return message;
            }
        }
    }

    private static record NameAndArgs(String name, Object[] args) {

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = prime * result + Arrays.deepHashCode(args);
            result = prime * result + Objects.hash(name);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NameAndArgs other = (NameAndArgs) obj;
            return Arrays.deepEquals(args, other.args) && Objects.equals(name, other.name);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            if (name != null) {
                builder.append("name=").append(name).append(", ");
            }
            if (args != null) {
                builder.append("args=").append(Arrays.toString(args));
            }
            builder.append(']');
            return builder.toString();
        }
    }

    /** A listener that pays respect to creating/adding thread */
    private abstract static class AbstractThreadAwareListener implements Listener {

        /** The thread that created (& added) this listener */
        private final Thread creator;

        /**
         * Initializes a new {@link AbstractThreadAwareListener}.
         */
        protected AbstractThreadAwareListener() {
            super();
            creator = Thread.currentThread();
        }

        @Override
        public final void call(Object... args) {
            if (creator == Thread.currentThread()) {
                onCreatorThread(args);
            } else {
                onAnotherThread(args);
            }
        }

        /**
         * Invoked when calling thread is equal to creator thread.
         *
         * @param args The passed event arguments
         */
        protected abstract void onCreatorThread(Object[] args);

        /**
         * Invoked when calling thread is <b>NOT</b> equal to creator thread.
         *
         * @param args The passed event arguments
         */
        protected abstract void onAnotherThread(Object[] args);

    }

}
