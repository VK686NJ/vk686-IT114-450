package Project.Common;


public class PrivateMessagePayload extends Payload {
    private long targetClientId;

    public long getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(long targetClientId) {
        this.targetClientId = targetClientId;
    }

    @Override
    public String toString() {
        return String.format("PrivateMessagePayload[%s] Client id [%s] Target id [%s] Message: [%s]",
                getPayloadType(), getClientId(), getTargetClientId(), getMessage());
    }
}