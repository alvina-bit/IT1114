package Server;

import Exceptions.*;

import Common.*;




import Common.Constants;
import Common.LoggerUtil;
import Common.Phase;
import Common.TimedEvent;
import Exceptions.NotReadyException;
import Exceptions.PhaseMismatchException;
import Exceptions.PlayerNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Comparator;

public class GameRoom extends BaseGameRoom {

    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    private TimedEvent turnTimer = null;

    private int round = 0;
    public GameRoom(String name) {
        super(name);
    }
    private List<ServerThread> getActivePlayers() {
    return clientsInRoom.values().stream()
            .filter(player -> !player.isEliminated()) .collect(Collectors.toList());
}

private void broadcastUserListUpdate() {
    // Build PlayerData list sorted by points descending, then name ascending
    List<PlayerData> playerDataList = clientsInRoom.values().stream()
        .map(client -> new PlayerData(
            client.getClientName(), 
            client.getClientId(),
            client.getPoints(),
            client.isEliminated(),
            !client.hasMadeChoice() // pending if not made choice
        ))
        .sorted(Comparator.comparingInt(PlayerData::getPoints).reversed()
                .thenComparing(PlayerData::getName))
        .collect(Collectors.toList());

    Payload payload = new Payload();
    payload.setPayloadType(PayloadType.USER_LIST_UPDATE);
    payload.setPlayerList(playerDataList);

    // Broadcast to all clients in this room
    broadcast(payload);
}

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerThread sp) {
        // sync GameRoom state to new client
        syncCurrentPhase(sp);
        syncReadyStatus(sp);
        syncTurnStatus(sp);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerThread sp) {
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + clientsInRoom.size());
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        }
    }

    // timer handlers
    private void startRoundTimer() {
        roundTimer = new TimedEvent(30, () -> onRoundEnd());
        roundTimer.setTickCallback((time) -> System.out.println("Round Time: " + time));
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    private void startTurnTimer() {
        turnTimer = new TimedEvent(30, () -> onTurnEnd());
        turnTimer.setTickCallback((time) -> System.out.println("Turn Time: " + time));
    }

    private void resetTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
    }
    // end timer handlers

    // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        changePhase(Phase.IN_PROGRESS);
        round = 0;
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

  @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        resetRoundTimer();
        resetTurnStatus();
        round++;
        clientsInRoom.values().forEach(player -> {
          
            if(!player.isEliminated()) {
                player.setChoice(null);
            }
        });
        changePhase(Phase.CHOOSING);
        relay(null, String.format("Round %d has started", round));
        startRoundTimer();
        LoggerUtil.INSTANCE.info("onRoundStart() end");
    }

    /** {@inheritDoc} */
    @Override
    protected void onTurnStart() {
        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();
        startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }

    // Note: logic between Turn Start and Turn End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onTurnEnd() {
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer(); // reset timer if turn ended without the time expiring

        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }

    // Note: logic between Round Start and Round End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */

 @Override
protected void onRoundEnd() {
    LoggerUtil.INSTANCE.info("onRoundEnd() start");

    resetRoundTimer(); // stop/reset the timer

    // Step 1: Mark non-responding players as eliminated
    for (ServerThread player : clientsInRoom.values()) {
        if (!player.isEliminated() && !player.hasMadeChoice()) {
            player.setEliminated(true);
            relay(null, "Player " + player.getClientName() + " did not pick and is now eliminated.");
        }
    }

    // Step 2: Get all non-eliminated players
    List<ServerThread> activePlayers = getActivePlayers();
    if (activePlayers.size() <= 1) {
        onSessionEnd(); // 0 or 1 players left
        return;
    }

    // Step 3: Round-robin battles
    for (int i = 0; i < activePlayers.size(); i++) {
        ServerThread attacker = activePlayers.get(i);
        ServerThread defender = activePlayers.get((i + 1) % activePlayers.size());

        String result = resolveBattle(attacker, defender); // you need to define this method

        switch (result) {
            case "attacker":
                attacker.addPoint();  // make sure these methods exist in ServerThread or user object
                defender.setEliminated(true);
                break;
            case "defender":
                defender.addPoint();
                attacker.setEliminated(true);
                break;
            case "tie":
                // no one eliminated
                break;
        }

        // Step 4: Sync scores & relay battle result
        broadcastScores();  // make sure this method exists and works
        relay(null, attacker.getClientName() + " (" + attacker.getChoice() + ") vs " +
                     defender.getClientName() + " (" + defender.getChoice() + ") → " + result);
    }

    // Step 5: Check again how many are left
    if (getActivePlayers().size() <= 1) {
        onSessionEnd();
    } else {
        round++;
        onRoundStart();
    }

    LoggerUtil.INSTANCE.info("onRoundEnd() end");
}

  @Override
protected void onSessionEnd() {
    LoggerUtil.INSTANCE.info("onSessionEnd() start");

    // 1. Sort players by points descending
    List<ServerThread> sortedPlayers = new ArrayList<>(clientsInRoom.values());
    sortedPlayers.sort((p1, p2) -> Integer.compare(p2.getPoints(), p1.getPoints()));

    // 2. Build scoreboard message
    StringBuilder scoreboard = new StringBuilder();
    scoreboard.append("Game Over!\nFinal Scoreboard:\n");
    for (ServerThread player : sortedPlayers) {
        scoreboard.append(player.getClientName())
                  .append(" - Points: ")
                  .append(player.getPoints());
        if (player.isEliminated()) {
            scoreboard.append(" (eliminated)");
        }
        scoreboard.append("\n");
    }

    // 3. Send scoreboard to all players
    relay(null, scoreboard.toString().trim());

    // 4. Reset player session state (but NOT disconnect)
    for (ServerThread player : clientsInRoom.values()) {
        player.setEliminated(false);
        player.setReady(false);
        // You need to add setMadeChoice and setChoice methods if you want to track choices
        player.setChoice(null);
        player.setPoints(0); // Reset score — make sure ServerThread has points field & methods
    }

    // 5. Reset server-side game state
    resetReadyStatus();
    resetTurnStatus();
    changePhase(Phase.READY);

    LoggerUtil.INSTANCE.info("onSessionEnd() end");
}


    private void sendTurnStatus(ServerThread client, boolean tookTurn) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendTurnStatus(client.getClientId(), client.didTakeTurn());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    private void syncTurnStatus(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendTurnStatus(serverUser.getClientId(),
                        serverUser.didTakeTurn(), true);
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    // end send data to ServerThread(s)
    /**
 * Resolve the battle outcome between attacker and defender based on their choices.
 * @param attacker The attacking player
 * @param defender The defending player
 * @return "attacker" if attacker wins, "defender" if defender wins, "tie" otherwise
 */
protected String resolveBattle(ServerThread attacker, ServerThread defender) {
    String attackerChoice = attacker.getChoice();
    String defenderChoice = defender.getChoice();

    if (attackerChoice == null || defenderChoice == null) {
        return "tie"; // No choice means tie by default
    }

    // Example rules for Rock-Paper-Scissors:
    if (attackerChoice.equals(defenderChoice)) {
        return "tie";
    }
    switch (attackerChoice) {
        case "rock":
            return defenderChoice.equals("scissors") ? "attacker" : "defender";
        case "paper":
            return defenderChoice.equals("rock") ? "attacker" : "defender";
        case "scissors":
            return defenderChoice.equals("paper") ? "attacker" : "defender";
        default:
            return "tie";
    }
}

private void broadcastScores() {
    StringBuilder scoreMessage = new StringBuilder("Scores:\n");
    for (ServerThread player : clientsInRoom.values()) {
        scoreMessage.append(player.getClientName())  // or getName(), depending on your method
                    .append(": ")
                    .append(player.getPoints())
                    .append(player.isEliminated() ? " (eliminated)" : "")
                    .append("\n");
    }
    // Relay the scores to all clients in the room
    relay(null, scoreMessage.toString());
}

private void sendResetTurnStatus() {
    clientsInRoom.values().forEach(client -> client.sendResetTurnStatus());
}

    // misc methods
    private void resetTurnStatus() {
        clientsInRoom.values().forEach(sp -> {
            sp.setTookTurn(false);
        });
        sendResetTurnStatus();
    }

    private void checkAllTookTurn() {
        int numReady = clientsInRoom.values().stream()
                .filter(sp -> sp.isReady())
                .toList().size();
        int numTookTurn = clientsInRoom.values().stream()
                // ensure to verify the isReady part since it's against the original list
                .filter(sp -> sp.isReady() && sp.didTakeTurn())
                .toList().size();
        if (numReady == numTookTurn) {
            relay(null,
                    String.format("All players have taken their turn (%d/%d) ending the round", numTookTurn, numReady));
            onRoundEnd();
        }
    }

    // receive data from ServerThread (GameRoom specific)

    /**
     * Example turn action
     * 
     * @param currentUser
     */
    protected void handleTurnAction(ServerThread currentUser, String exampleText) {
        // check if the client is in the room
        try {
            checkPlayerInRoom(currentUser);
            checkCurrentPhase(currentUser, Phase.IN_PROGRESS);
            checkIsReady(currentUser);
            if (currentUser.didTakeTurn()) {
                currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You have already taken your turn this round");
                return;
            }
            currentUser.setTookTurn(true);
            sendTurnStatus(currentUser, currentUser.didTakeTurn());
            checkAllTookTurn();
        }
        catch(NotReadyException e){
            // The check method already informs the currentUser
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } 
        catch (PlayerNotFoundException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do the ready check");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (PhaseMismatchException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You can only take a turn during the IN_PROGRESS phase");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        }
    }

    // end receive data from ServerThread (GameRoom specific)
}