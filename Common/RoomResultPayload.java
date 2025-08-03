package Common;

import java.util.ArrayList;
import java.util.List;

public class RoomResultPayload extends Payload {
    private List<String> rooms = new ArrayList<String>();
    private List<User> players = new ArrayList<User>();

    public RoomResultPayload() {
        setPayloadType(PayloadType.ROOM_LIST);
    }

    public List<String> getRooms() {
        return rooms;
    }

      public List<User> getPlayers() {
        return players;
    }

    public void setPlayers(List<User> players) {
        this.players = players;
    }

    public void setRooms(List<String> rooms) {
        this.rooms = rooms;
    }

    @Override
    public String toString() {
        return super.toString() + "Rooms [" + String.join(",", rooms) + "]";
    }
}
// ... other imports and class definition


   