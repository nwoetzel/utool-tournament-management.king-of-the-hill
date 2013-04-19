package utool.plugin.kingofthehill.tournament;

import java.util.UUID;

/**
 * Extra data class for players
 * @author Cory
 *
 */
public class KingOfTheHillPlayerExtra implements Comparable<KingOfTheHillPlayerExtra> {
	/**
	 * The number of games the player has won. -1 for unset
	 */
	private int wins = -1;
	/**
	 * The number of games the player has lost. -1 for unset
	 */
	private int losses = -1;
	/**
	 * The player's UUID
	 */
	private UUID playerUUID;
	
	/**
	 * Constructor for local tournaments
	 * @param playerUUID Player's UUID
	 */
	public KingOfTheHillPlayerExtra(UUID playerUUID){
		this.playerUUID = playerUUID;
	}
	
	/**
	 * Constructor for preset data
	 * @param playerUUID Player's UUID
	 * @param wins Player's wins
	 * @param losses Player's losses
	 */
	public KingOfTheHillPlayerExtra(UUID playerUUID, int wins, int losses){
		this.playerUUID = playerUUID;
		this.wins = wins;
		this.losses = losses;
	}
	
	/**
	 * Get wins
	 * @return wins
	 */
	public int getWins(){
		return wins;
	}
	
	/**
	 * Get losses
	 * @return losses
	 */
	public int getLosses(){
		return losses;
	}
	
	/**
	 * Get player's UUID
	 * @return UUID
	 */
	public UUID getPlayerUUID(){
		return playerUUID;
	}
	
	/**
	 * Get total number of games the player has been in
	 * @return wins + losses
	 */
	public int getGames(){
		return wins + losses;
	}
	
	/**
	 * Add 1 to wins
	 */
	public void addWin(){
		prepareWinLoss();
		wins += 1;
	}
	
	/**
	 * Add 1 to losses
	 */
	public void addLoss(){
		prepareWinLoss();
		losses += 1;
	}
	
	/**
	 * Prepare the data for adding wins and losses.
	 */
	private void prepareWinLoss(){
		if (wins == -1){
			wins = 0;
		}
		if (losses == -1){
			losses = 0;
		}
	}

	@Override
	public int compareTo(KingOfTheHillPlayerExtra another) {
		if (another.wins < this.wins){
			return -1;
		} else if (another.wins > this.wins) {
			return 1;
		} else {
			return 0;
		}
	}
}
