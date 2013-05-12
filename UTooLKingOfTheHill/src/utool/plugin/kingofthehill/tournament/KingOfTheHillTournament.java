package utool.plugin.kingofthehill.tournament;

import java.util.LinkedList;
import java.util.List;
import android.content.Context;
import utool.plugin.Player;

/**
 * This class controls the host-side logic for the King of the Hill plugin.
 * @author Cory
 *
 */
public class KingOfTheHillTournament extends TournamentLogic {

	/**
	 * Constructor
	 * @param context Android context.
	 * @param tournamentId Tournament ID from the core
	 * @param players List of players to start the game with.
	 * @param king The player to make the king.
	 */
	protected KingOfTheHillTournament(Context context, long tournamentId, LinkedList<Player> players, Player king){
		super(tournamentId, context);
		this.players = new LinkedList<Player>(players);
		this.king = king;
		if (this.players.contains(king)){
			this.players.remove(king);
		}
	}
	
	/**
	 * Restart the tournament with using the given players
	 * @param king The king to set
	 * @param players The list of players
	 */
	public void restartTournament(Player king, LinkedList<Player> players){
		this.king = king;
		this.players = new LinkedList<Player>(players);
		if (this.players.contains(king)){
			this.players.remove(king);
		}
		this.playersExtraData.clear();
		this.kingWins = 0;
		activity.updateActivityExternal();
		outgoingCommandHandler.sendGameState();
	}
	
	/**
	 * Pop the player at the top of the game queue. The player is removed from the queue after this method has been executed.
	 * @param enableAnimations Set whether to (re)enable slide animations with this call. Use false when calling as part of a move operation.
	 * @return The player at the top of the queue.
	 */
	public Player popTopPlayer(boolean enableAnimations){
		if (enableAnimations){
			setUseSlideAnimations(true);
		}
		Player p = players.poll();
		notifyDataSetChanged();
		return p;
	}
	
	/**
	 * Add a player to the bottom of the game queue. If the player is already a part of the game queue, this method does nothing.
	 * @param player The player to add.
	 */
	private void addPlayerToBottom(Player player){
		setUseSlideAnimations(true);
		if (!this.players.contains(player)){
			players.add(player);
			notifyDataSetChanged();
		}
	}
	
	/**
	 * Move the specified player to another location.
	 * @param player The player to move. Can be the king.
	 * @param destinationPosition The position to move the player to. If position is -1, this will move the player to the king position.
	 * Otherwise, this value is a 0-based index of the position in the player list.  
	 */
	public void movePlayer(Player player, int destinationPosition){
		//Block slide animations
		setUseSlideAnimations(false);
		
		if (player.equals(king)){
			Player newKing = popTopPlayer(false);
			setKing(newKing);
		}
		if (destinationPosition == KING_POSITION){
			//move current king to top of queue
			players.add(0, getKing());
			
			//set new king
			setKing(player);
			
			//remove player from queue
			players.remove(player);
			
		} else if (destinationPosition < players.size()) {
			//remove player from queue
			players.remove(player);
			//insert in new position
			players.add(destinationPosition, player);
		} else {
			//insert at end of queue
			players.remove(player);
			players.add(player);
		}
		notifyDataSetChanged();
		if (activity != null){
			activity.setKing(getKing());
		}
		
		outgoingCommandHandler.sendGameState();
	}
	
	/**
	 * Move the specified player to another location.
	 * @param currentPosition The index the player is currently at. King is at -1.
	 * @param destinationPosition The index to move the player to.
	 */
	public void movePlayer(int currentPosition, int destinationPosition){
		setUseSlideAnimations(false);
		Player player;
		if (currentPosition == KING_POSITION){
			player = getKing();
		} else {
			player = players.get(currentPosition);
		}
		movePlayer(player, destinationPosition);
		
		outgoingCommandHandler.sendGameState();
	}
	
	/**
	 * Add players to the bottom of the game queue.
	 * @param players The players to add. Players already in the game are ignored.
	 */
	public void addNewPlayersToBottom(List<Player> players){
		setUseSlideAnimations(true);
		for (Player p: players){
			if (king == null){
				setKing(p);
			}
			if (!this.players.contains(p) && !king.equals(p)){
				this.players.add(p);
			}
		}
		notifyDataSetChanged();
		
		//update game state
		outgoingCommandHandler.sendGameState();
	}
	
	/**
	 * Fully update the players in the game.  
	 * @param players The new list of players. Players not in this list are fully removed from the game.
	 */
	public void updatePlayerList(List<Player> players){
		setUseSlideAnimations(true);
		List<Player> removed = new LinkedList<Player>();
		//Add new players
		addNewPlayersToBottom(players);
		
		//Remove players not in the list
		for (Player p: this.players){
			if (!players.contains(p) && !king.equals(p)){
				removed.add(p);
			}
		}
		
		//Check if current king has been removed from the game
		if (!players.contains(king)){
			removed.add(king);
		}
		
		//Remove removed players from tournament
		for (Player p: removed){
			if (king != null && king.equals(p)){
				setKing(popTopPlayer(true));
			} else {
				this.players.remove(p);
			}
		}
		
		notifyDataSetChanged();
		
		//update game state
		outgoingCommandHandler.sendGameState();
	}
	
	/**
	 * Move the current king to the end of the game queue, and promote the challenger.
	 */
	public void moveKingToEnd(){
		setUseSlideAnimations(true);
		Player oldKing = getKing();
		addPlayerToBottom(oldKing);
		
		Player newKing = popTopPlayer(true);
		setKing(newKing);
		kingWins = 1;
		
		getPlayerExtra(oldKing).addLoss();
		getPlayerExtra(newKing).addWin();
		
		notifyDataSetChanged();
		startRound();
		outgoingCommandHandler.sendGameState();
	}
	
	/**
	 * Move the current challenger to the end of the game queue.
	 * @return The player who was moved.
	 */
	public Player moveChallengerToEnd(){
		setUseSlideAnimations(true);
		Player challenger = popTopPlayer(true);
		addPlayerToBottom(challenger);
		
		kingWins += 1;
		getPlayerExtra(getKing()).addWin();
		getPlayerExtra(challenger).addLoss();
		
		notifyDataSetChanged();
		startRound();
		outgoingCommandHandler.sendGameState();
		return challenger;
	}
	
	/**
	 * Set the king to the specified player, overriding the current king.
	 * This method does not move the current king to the game queue, or remove the given player from the game queue.
	 * @param player The player to set as king.
	 */
	private void setKing(Player player){
		this.king = player;
	}
	
	/**
	 * Configure the game timer
	 * @param seconds The number of seconds to make the game. Sets to TIMER_NOT_SET if < 1
	 */
	public void setGameTimerSetting(int seconds){
		if (seconds < 1){
			this.gameTimerSetting = TIMER_NOT_SET;
		} else {
			this.gameTimerSetting = seconds;
		}
		this.gameTimerStart = System.nanoTime();
	}
	
	/**
	 * Configure the round timer
	 * @param seconds The number of seconds to make the round. Sets to TIMER_NOT_SET if < 1
	 */
	public void setRoundTimerSetting(int seconds){
		if (seconds < 1){
			this.roundTimerSetting = TIMER_NOT_SET;
		} else {
			this.roundTimerSetting = seconds;
		}
		this.roundTimerStart = System.nanoTime();
	}
	
	/**
	 * Update clients with the most recent game state
	 */
	public void sendGameState(){
		outgoingCommandHandler.sendGameState();
	}
}
