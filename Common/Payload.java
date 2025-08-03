package Common;

import java.io.Serializable;
import java.util.List;







public class Payload implements Serializable {
    private PayloadType payloadType;
    private long clientId;
    private String message;
    private String choice;
    private List<PlayerData> playerList;


    /**
     * @return the payloadType
     */
    public PayloadType getPayloadType() {
        return payloadType;
    }

    /**
     * @param payloadType the payloadType to set
     */
    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }

    /**
     * @return the clientId
     */
    public long getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
    public String getChoice() {
    return choice;
}

public void setChoice(String choice) {
    this.choice = choice;
}

/**
 * @return the playerList
 */
public List<PlayerData> getPlayerList() {
    return playerList;
}

/**
 * @param playerList the playerList to set
 */
public void setPlayerList(List<PlayerData> playerList) {
    this.playerList = playerList;
}


    @Override
    public String toString() {
        return String.format("Payload[%s] Client Id [%s] Message: [%s]", getPayloadType(), getClientId(), getMessage());
    }
}
