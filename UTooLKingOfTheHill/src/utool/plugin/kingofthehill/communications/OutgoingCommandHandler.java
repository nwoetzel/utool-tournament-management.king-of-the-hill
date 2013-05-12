package utool.plugin.kingofthehill.communications;

import java.util.List;

import android.os.RemoteException;
import android.util.Log;

import utool.plugin.IUTooLCore;
import utool.plugin.Player;
import utool.plugin.kingofthehill.tournament.TournamentLogic;

/**
 * Outgoing command handler for KOTH
 * @author Cory
 *
 */
public class OutgoingCommandHandler {
	
	/**
	 * Tournament logic class using this handler
	 */
	TournamentLogic tournament;
	
	/**
	 * Service connection associated with this tournament
	 */
	IUTooLCore service;
	
	/**
	 * Constructor
	 * @param tournament The tournament
	 */
	public OutgoingCommandHandler(TournamentLogic tournament){
		this.tournament = tournament;
	}
	
	/**
	 * Constructor
	 * @param tournament The tournament
	 * @param service The mICore service from the main activity
	 */
	public OutgoingCommandHandler(TournamentLogic tournament, IUTooLCore service){
		this.tournament = tournament;
		this.service = service;
	}
	
	/**
	 * Send the game state
	 */
	public void sendGameState(){
		Player king = tournament.getKing();
		List<Player> players = tournament.getPlayers();
		
		GameStateMessage message = new GameStateMessage(king, players, tournament.getPlayerExtras(), tournament.getRemainingGameTime(), tournament.getRemainingRoundTime());
		message.setKingWins(tournament.getKingWinsStreakCount());
		String xml = message.getXml();

		try {
			if (service != null)
				service.send(xml);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Request the current game state from the host
	 */
	public void requestGameState(){
		GameStateMessage message = new GameStateMessage();
		String xml = message.getXml();
		
		try {
			if (service != null){
				Log.d("KOTH", "Requesting game state");
				service.send(xml);
			} else {
				Log.d("KOTH", "Error requesting game state, service is null");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
