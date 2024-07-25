package Project.Server;

import java.net.Socket;

import java.util.List;
import java.util.Objects;

import java.util.function.Consumer;

import Project.Common.PayloadType;
import Project.Common.PrivateMessagePayload;
import Project.Common.RollPayload;
import Project.Common.RoomResultsPayload;
import Project.Common.TextFX;
import Project.Common.Payload;
import Project.Common.ConnectionPayload;
import Project.Common.LoggerUtil;

/**
 * A server-side representation of a single client.
 * This class is more about the data and abstracted communication
 */
public class ServerThread extends BaseServerThread {
    public static final long DEFAULT_CLIENT_ID = -1;
    private Room currentRoom;
    private long clientId;
    private String clientName;
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready

    /**
     * Wraps the Socket connection and takes a Server reference and a callback
     * 
     * @param myClient
     * @param server
     * @param onInitializationComplete method to inform listener that this object is
     *                                 ready
     */
    protected ServerThread(
            Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        info("ServerThread created");
        // get communication channels to single client
        this.client = myClient;
        this.clientId = ServerThread.DEFAULT_CLIENT_ID;// this is updated later by the server
        this.onInitializationComplete = onInitializationComplete;

    }

    public void setClientName(String name) {
        if (name == null) {
            throw new NullPointerException("Client name can't be null");
        }
        this.clientName = name;
        onInitialized();
    }

    public String getClientName() {
        return clientName;
    }

    public long getClientId() {
        return this.clientId;
    }

    protected Room getCurrentRoom() {
        return this.currentRoom;
    }

    protected void setCurrentRoom(Room room) {
        if (room == null) {
            throw new NullPointerException("Room argument can't be null");
        }
        currentRoom = room;
    }

    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this); // Notify server that initialization is complete
    }

    @Override
    protected void info(String message) {
        LoggerUtil.INSTANCE.info(String.format("ServerThread[%s(%s)]: %s", getClientName(), getClientId(), message));
    }

    @Override
    protected void cleanup() {
        currentRoom = null;
        super.cleanup();
    }

    @Override
    protected void disconnect() {
        // sendDisconnect(clientId, clientName);
        super.disconnect();
    }

    // handle received message from the Client
    @Override
    protected void processPayload(Payload payload) {
        try {
            switch (payload.getPayloadType()) {
                case CLIENT_CONNECT:
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    setClientName(cp.getClientName());
                    break;
                case MESSAGE:
                    String formattedMessage = TextFX.formatText(payload.getMessage());
                    currentRoom.sendMessage(this, formattedMessage);
                    break;
                case ROOM_CREATE:
                    currentRoom.handleCreateRoom(this, payload.getMessage());
                    break;
                case ROOM_JOIN:
                    currentRoom.handleJoinRoom(this, payload.getMessage());
                    break;
                case ROOM_LIST:
                    currentRoom.handleListRooms(this, payload.getMessage());
                    break;
                // vk686 07/22/2024
                case ROLL:
                    handleRollPayload((RollPayload) payload);
                    break;
                case FLIP:
                    handleFlipPayload(payload);
                    break;
                case DISCONNECT:
                    currentRoom.disconnect(this);
                    break;
                case PRIVATE:
                    handlePrivateMessagePayload((PrivateMessagePayload) payload);
                    break;
                /*
                 * case MUTE:vk686 07/24/2024
                 * handleMutePayload(payload, true);
                 * break;
                 * case UNMUTE:
                 * handleMutePayload(payload, false);
                 * break;
                 */
                default:
                    break;
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Could not process Payload: " + payload, e);

        }
    }

    // vk686 07/22/2024
    private void handleRollPayload(RollPayload rollPayload) {
        int numberOfDice = rollPayload.getNumberOfDice();
        int sidesPerDie = rollPayload.getSidesPerDie();
        int rollResult = rollDice(numberOfDice, sidesPerDie);
        rollPayload.setRollResult(rollResult);
        String result;
        if (numberOfDice == 1) {
            result = String.format("%s rolled %d and got %d", getClientName(), sidesPerDie, rollResult);
        } else {
            result = String.format("%s rolled %dd%d and got %d", getClientName(), numberOfDice, sidesPerDie,
                    rollResult);
        }

        result = TextFX.formatText(String.format("_**<font color='green'>%s</font>**_", result));
        currentRoom.sendMessage(this, result);
    }

    private int rollDice(int numberOfDice, int sidesPerDie) {
        int total = 0;
        for (int i = 0; i < numberOfDice; i++) {
            total += (int) (Math.random() * sidesPerDie) + 1;
        }
        return total;
    }

    private void handleFlipPayload(Payload payload) {
        String result = Math.random() < 0.5 ? "heads" : "tails";
        String message = String.format("%s flipped a coin and got %s", getClientName(), result);

        message = TextFX.formatText(String.format("_**<font color='red'>%s</font>**_", message));
        currentRoom.sendMessage(this, message);
    }

    private void handlePrivateMessagePayload(PrivateMessagePayload payload) {
        // Sending the private message details to the Room class
        currentRoom.sendPrivateMessage(this, payload.getTargetClientId(), payload.getMessage());
    }
    // vk686 07/24/2024
    /*
     * private void handleMutePayload(Payload payload, boolean mute) {
     * long targetClientId = payload.getTargetClientId();
     * if (mute) {
     * mutedClientIds.add(targetClientId);
     * sendMessage("Muted user with ID " + targetClientId);
     * } else {
     * mutedClientIds.remove(targetClientId);
     * sendMessage("Unmuted user with ID " + targetClientId);
     * }
     * }
     * 
     * public boolean isMuted(long clientId) {
     * return mutedClientIds.contains(clientId);
     * }
     */

    // send methods to pass data back to the Client

    public boolean sendRooms(List<String> rooms) {
        RoomResultsPayload rrp = new RoomResultsPayload();
        rrp.setRooms(rooms);
        return send(rrp);
    }

    public boolean sendClientSync(long clientId, String clientName) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        cp.setConnect(true);
        cp.setPayloadType(PayloadType.SYNC_CLIENT);
        return send(cp);
    }

    /**
     * Overload of sendMessage used for server-side generated messages
     * 
     * @param message
     * @return @see {@link #send(Payload)}
     */
    public boolean sendMessage(String message) {
        return sendMessage(ServerThread.DEFAULT_CLIENT_ID, message);
    }

    /**
     * Sends a message with the author/source identifier
     * 
     * @param senderId
     * @param message
     * @return @see {@link #send(Payload)}
     */
    public boolean sendMessage(long senderId, String message) {
        Payload p = new Payload();
        p.setClientId(senderId);
        p.setMessage(message);
        p.setPayloadType(PayloadType.MESSAGE);
        return send(p);
    }

    /**
     * Tells the client information about a client joining/leaving a room
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @param room       the room
     * @param isJoin     true for join, false for leaivng
     * @return success of sending the payload
     */
    public boolean sendRoomAction(long clientId, String clientName, String room, boolean isJoin) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.ROOM_JOIN);
        cp.setConnect(isJoin); // <-- determine if join or leave
        cp.setMessage(room);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    /**
     * Tells the client information about a disconnect (similar to leaving a room)
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @return success of sending the payload
     */
    public boolean sendDisconnect(long clientId, String clientName) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.DISCONNECT);
        cp.setConnect(false);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    /**
     * Sends (and sets) this client their id (typically when they first connect)
     * 
     * @param clientId
     * @return success of sending the payload
     */
    public boolean sendClientId(long clientId) {
        this.clientId = clientId;
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.CLIENT_ID);
        cp.setConnect(true);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    // end send methods
}
