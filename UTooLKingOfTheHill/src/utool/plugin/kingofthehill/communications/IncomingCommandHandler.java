package utool.plugin.kingofthehill.communications;

import utool.networking.XmlMessageTypeException;
import utool.networking.packet.PlayerMessage;
import utool.networking.packet.PlayerMessage.MessageType;
import utool.networking.packet.PluginTerminationMessage;
import utool.plugin.kingofthehill.tournament.KingOfTheHillParticipantTournament;
import utool.plugin.kingofthehill.tournament.KingOfTheHillTournament;
import utool.plugin.kingofthehill.tournament.TournamentLogic;

/**
 * Incoming command handler for KOTH
 * @author Cory
 *
 */
public class IncomingCommandHandler {

	/**
	 * Tournament logic class this handler is associated with
	 */
	TournamentLogic tournament;

	/**
	 * Constructor
	 * @param tournament Tournament logic class this handler is associated with
	 */
	public IncomingCommandHandler(TournamentLogic tournament){
		this.tournament = tournament;
	}

	/**
	 * Handle a received message
	 * @param data The message string
	 */
	public void handleMessage(String data){
		try {
			GameStateMessage message = new GameStateMessage(data);
			switch (message.getMessageType()) {
				case GameState:
					if (tournament instanceof KingOfTheHillParticipantTournament){
						tournament.setPlayerExtras(message.getPlayerExtras());
						tournament.setKing(message.getKing());
						tournament.setPlayers(message.getPlayerList());
						tournament.setRemainingGameTime(message.getGameTimeRemaining());
						tournament.setRemainingRoundTime(message.getRoundTimeRemaining());
					}
					break;
				case RequestGameState:
					if (tournament instanceof KingOfTheHillTournament){
						tournament.getOutgoingCommandHandler().sendGameState();
					}
					break;
				default:
					break;
			}
			return;
		} catch (XmlMessageTypeException e) {
		}
		try {
			PlayerMessage message = new PlayerMessage(data);
			if (message.getMessageType() == MessageType.PlayerRegister){
				tournament.addNewPlayersToBottom(message.getPlayerList());
			}
			else if (message.getMessageType() == MessageType.PlayerList){
				tournament.updatePlayerList(message.getPlayerList());
				tournament.updateActivity();
			}
			return;
		} catch (XmlMessageTypeException e){
		}
		if (PluginTerminationMessage.isPluginTerminationMessage(data)){
			tournament.endTournament();
		}
	}
}
