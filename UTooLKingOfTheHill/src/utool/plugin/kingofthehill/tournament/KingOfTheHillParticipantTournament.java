package utool.plugin.kingofthehill.tournament;

import java.util.LinkedList;
import java.util.List;
import utool.plugin.Player;
import android.content.Context;

/**
 * This class controls the client-side logic for the King of the Hill plugin.
 * @author Cory
 *
 */
public class KingOfTheHillParticipantTournament extends TournamentLogic {

	/**
	 * Constructor
	 * @param tournamentId Tournament id from the core
	 * @param context Android context
	 * @param players Players in the game queue
	 */
	protected KingOfTheHillParticipantTournament(long tournamentId, Context context, List<Player> players) {
		super(tournamentId, context);
		
		updatePlayerList(players);
	}

	@Override
	public void updatePlayerList(List<Player> players) {
		synchronized (playerListLock) {
			this.players = new LinkedList<Player>(players);
			if (this.players.contains(king)){
				this.players.remove(king);
			}
		}
		if (activity != null)
			notifyDataSetChanged();
	}

	@Override
	public void addNewPlayersToBottom(List<Player> players) {
		// do nothing
		
	}

}
