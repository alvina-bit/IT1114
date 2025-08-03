package Common;

import java.util.HashMap;

public enum Command {
    QUIT("quit"),
    DISCONNECT("disconnect"),
    LOGOUT("logout"),
    LOGOFF("logoff"),
    REVERSE("reverse"),
    CREATE_ROOM("createroom"),
    LEAVE_ROOM("leaveroom"),
    JOIN_ROOM("joinroom"),
    NAME("name"),
    LIST_USERS("users"),
    LIST_ROOMS("rooms"),
    MESSAGE("message"),
    SYNC_CLIENT("syncclient"),
    SYNC_READY("syncready"),
    RESET_READY("resetready"),
    PHASE("phase"),
    TURN("turn"),
    SYNC_TURN("syncturn"),
    RESET_TURN("resetturn"),
    EXAMPLE_TURN("exampleturn"),
    EXAMPLE_SYNC_TURN("examplesyncturn"),
    EXAMPLE_RESET_TURN("exampleresettturn"),
    EXAMPLE_READY("exampleready"),
    EXAMPLE_SYNC_READY("examplesyncready"),
    EXAMPLE_RESET_READY("exampleresetready"),
    EXAMPLE_PHASE("examplephase"),
    pick("pick"),
    READY("ready");

    private static final HashMap<String, Command> BY_COMMAND = new HashMap<>();
    static {
        for (Command e : values()) {
            BY_COMMAND.put(e.command, e);
        }
    }
    public final String command;

    private Command(String command) {
        this.command = command;
    }

    public static Command stringToCommand(String command) {
        return BY_COMMAND.get(command);
    }
}
