package utool.plugin.kingofthehill.tournament;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import utool.plugin.IUTooLCore;
import utool.plugin.Player;
import utool.plugin.kingofthehill.KingOfTheHillMainActivity;
import utool.plugin.kingofthehill.R;
import utool.plugin.kingofthehill.communications.AutomaticEmailHandler;
import utool.plugin.kingofthehill.communications.IncomingCommandHandler;
import utool.plugin.kingofthehill.communications.OutgoingCommandHandler;

/**
 * Base class for tournament logic classes, as well as game instance handler and UI list adapter.
 * @author Cory
 *
 */
public abstract class TournamentLogic extends BaseAdapter {
	/**
	 * "Index" that the king is at in the player queue.
	 * Not really an index, but is used by almost all tournament logic as the king position value.
	 */
	public static final int KING_POSITION = -1;
	
	/**
	 * HashMap storing instances for all tournaments
	 */
	protected static HashMap<Long, TournamentLogic> tournamentInstances = new HashMap<Long, TournamentLogic>();

	/**
	 * The tournament id given by the core associated with this game instance.
	 */
	protected long tournamentId;

	/**
	 * The incoming command handler for this tournament
	 */
	protected IncomingCommandHandler incommingCommandHandler;

	/**
	 * The outgoing command handler for this tournament
	 */
	protected OutgoingCommandHandler outgoingCommandHandler;

	/**
	 * The list of players in the game, excluding the current king
	 */
	protected volatile LinkedList<Player> players;

	/**
	 * Mapping of players to extra player data
	 */
	protected HashMap<Player, KingOfTheHillPlayerExtra> playersExtraData = new HashMap<Player, KingOfTheHillPlayerExtra>();

	/**
	 * The current king of the game
	 */
	protected Player king;

	/**
	 * The current activity this game instance is associated with. Used for notifying on network communications.
	 */
	protected KingOfTheHillMainActivity activity = null;

	/**
	 * Android context
	 */
	protected Context context;

	/**
	 * The time the game timer was started, from System.nanoTime()
	 */
	protected long gameTimerStart;

	/**
	 * The user-configured game timer setting in seconds
	 */
	protected int gameTimerSetting = 0;

	/**
	 * The time the round timer was started, from System.nanoTime()
	 */
	protected long roundTimerStart;

	/**
	 * The user-configured round timer setting in seconds
	 */
	protected int roundTimerSetting = 0;
	
	/**
	 * The value returned by timer methods when the timer isn't configured
	 */
	public int TIMER_NOT_SET = -1;

	/**
	 * Sync lock object for player list
	 */
	protected Object playerListLock = new Object();
	
	/**
	 * Email handler for this tournament
	 */
	private AutomaticEmailHandler emailHandler;
	
	/**
	 * Basic abstract constructor
	 * @param tournamentId Core tournament id
	 * @param context Android context
	 */
	protected TournamentLogic(long tournamentId, Context context){
		this.tournamentId = tournamentId;
		this.context = context;
		incommingCommandHandler = new IncomingCommandHandler(this);
		outgoingCommandHandler = new OutgoingCommandHandler(this);
		emailHandler = new AutomaticEmailHandler(tournamentId);
	}
	
	/**
	 * Get the tournament's ID from the core
	 * @return Tournament ID
	 */
	public long getTournamentId(){
		return this.tournamentId;
	}

	/**
	 * Method for getting singleton instance of this TournamentLogic
	 * This method does not attempt to create the instance, so if it does not exist, this will return null
	 * @param tournamentId The tournament id of the single elimination instance to retrieve
	 * @return the singleton instance of this tournament or null if it has not been created yet
	 */
	public static TournamentLogic getInstance(long tournamentId){
		return tournamentInstances.get(tournamentId);
	}

	/**
	 * Method for creating new singleton instance of this TournamentLogic.
	 * If an instance already exists with the given tournament id, the parameters will be ignored and that instance returned.
	 * @param context Android context
	 * @param tournamentId Core tournament id
	 * @param players List of players to add to the game
	 * @param permissionLevel Permission level the plugin was started with. If Player.HOST, this method creates a KingOfTheHillTournament. Otherwise, KingOfTheHillParticipantTournament
	 * @return New or existing instance of a tournament instance.
	 */
	public static TournamentLogic getNewInstance(Context context, long tournamentId, List<Player> players, int permissionLevel)
	{
		TournamentLogic tournament = tournamentInstances.get(tournamentId);
		LinkedList<Player> playersLinked = new LinkedList<Player>(players);

		if(tournament == null){
			//determine if the tournament should be participant or host
			if(permissionLevel == Player.HOST)
			{
				Player king = playersLinked.poll();
				tournament = new KingOfTheHillTournament(context, tournamentId, playersLinked, king);
			}
			else
			{
				//participant tournament class
				tournament = new KingOfTheHillParticipantTournament(tournamentId, context, playersLinked);
			}

			tournamentInstances.put(tournamentId, tournament);
		}
		return tournament;
	}

	/**
	 * Remove an instance from the tournament manager. Call this when finishing a tournament as part of the cleanup process.
	 * @param tournamentId Core tournament id
	 */
	public static void removeInstance(long tournamentId){
		tournamentInstances.remove(tournamentId);
	}

	/**
	 * Update the activity associated with this instance
	 * @param activity KingOfTheHillMainActivity
	 * @param service The mICore service connection from the main activity
	 */
	public void setMatchupActivity(KingOfTheHillMainActivity activity, IUTooLCore service){
		this.activity = activity;
		this.outgoingCommandHandler = new OutgoingCommandHandler(this, service);
	}

	/**
	 * Get all players in the game, excluding the king
	 * @return List of players
	 */
	public List<Player> getPlayers(){
		synchronized (playerListLock) {
			return new LinkedList<Player>(players);
		}
	}

	/**
	 * Get extra data associated with a player
	 * @param player The player to get the data for
	 * @return Extra data for the player
	 */
	public KingOfTheHillPlayerExtra getPlayerExtra(Player player){
		KingOfTheHillPlayerExtra extra =  playersExtraData.get(player);
		if (extra == null){
			extra = new KingOfTheHillPlayerExtra(player.getUUID());
			playersExtraData.put(player, extra);
		}
		return extra;
	}

	/**
	 * Get all player extra data
	 * @return Player extra data
	 */
	public HashMap<Player, KingOfTheHillPlayerExtra> getPlayerExtras(){
		return playersExtraData;
	}

	/**
	 * Get the current king of the game
	 * @return The king
	 */
	public Player getKing(){
		return king;
	}

	@Override
	public int getCount() {
		synchronized (playerListLock) {
			return players.size();
		}
	}

	@Override
	public Object getItem(int arg0) {
		return players.get(arg0);
	}

	@Override
	/**
	 * Does nothing, required by BaseAdapter
	 */
	public long getItemId(int arg0) {
		return 0;
	}

	/**
	 * Get the OutgoingCommandHandler for this tournament
	 * @return OutgoingCommandHandler
	 */
	public OutgoingCommandHandler getOutgoingCommandHandler(){
		return outgoingCommandHandler;
	}

	/**
	 * Get the IncomingCommandHandler for this tournament
	 * @return IncomingCommandHandler
	 */
	public IncomingCommandHandler getIncomingCommandHandler(){
		return incommingCommandHandler;
	}

	/**
	 * Set the current king of the tournament
	 * @param king The player UUID to set as the king
	 */
	public void setKing(UUID king){
		Player oldKing = this.king;
		if (this.king == null || !this.king.getUUID().equals(king)){
			for (Player p:players){
				if (p.getUUID().equals(king)){
					this.king = p;
					continue;
				}
			}
			if (oldKing != null){
				players.add(oldKing);
			}
		}
	}

	/**
	 * Get the current setting of the round timer in seconds.
	 * @return Round timer setting in second
	 */
	public int getRoundTimerSetting(){
		return roundTimerSetting;
	}

	/**
	 * Get the nano time the round timer was started. Compare to System.nanoTime()
	 * @return Start time of the round
	 */
	public long getRoundTimerStartTime(){
		return roundTimerStart;
	}

	/**
	 * Get the elapsed round time in seconds.
	 * @return Current round time in seconds
	 */
	public int getElapsedRoundTime(){
		long time = System.nanoTime() - roundTimerStart;
		time /= 1000000000;
		return (int)time;
	}

	/**
	 * Get the time remaining in the round
	 * @return The remaining round time in seconds. -1 if unlimited
	 */
	public int getRemainingRoundTime(){
		if (roundTimerSetting == 0){
			return TIMER_NOT_SET;
		}
		int elapsed = getElapsedRoundTime();
		int remaining = roundTimerSetting - elapsed;
		if (remaining < 0){
			return 0;
		}
		return remaining;
	}

	/**
	 * Get the current setting of the game timer in seconds.
	 * @return Game timer setting in second
	 */
	public int getGameTimerSetting(){
		return gameTimerSetting;
	}

	/**
	 * Get the nano time the game timer was started. Compare to System.nanoTime()
	 * @return Start time of the game
	 */
	public long getGameTimerStartTime(){
		return gameTimerStart;
	}

	/**
	 * Start the round timer
	 */
	public void startRound(){
		roundTimerStart = System.nanoTime();
	}

	/**
	 * Start the game timer
	 */
	public void startGame(){
		gameTimerStart = System.nanoTime();
	}

	/**
	 * Get the elapsed game time in seconds.
	 * @return Current game time in seconds
	 */
	public int getElapsedGameTime(){
		long time = System.nanoTime() - gameTimerStart;
		time /= 1000000000;
		return (int)time;
	}

	/**
	 * Get the time remaining in the game
	 * @return The remaining game time in seconds. -1 if unlimited
	 */
	public int getRemainingGameTime(){
		if (gameTimerSetting == 0){
			return -1;
		}
		int elapsed = getElapsedGameTime();
		int remaining = gameTimerSetting - elapsed;
		if (remaining < 0) {
			return 0;
		}
		return remaining;
	}
	
	/**
	 * Set the game timer to a specific value
	 * @param timeRemaining The number of seconds to set the timer to
	 */
	public void setRemainingGameTime(int timeRemaining){
		gameTimerStart = System.nanoTime();
		gameTimerSetting = timeRemaining;
	}
	
	/**
	 * Set the round timer to a specific value
	 * @param timeRemaining The number of seconds to set the timer to
	 */
	public void setRemainingRoundTime(int timeRemaining){
		roundTimerStart = System.nanoTime();
		roundTimerSetting = timeRemaining;
	}

	/**
	 * Notify the main activity to update itself
	 */
	public void updateActivity(){
		activity.updateActivityExternal();
	}

	/**
	 * Update the game's player list
	 * @param players The complete player list
	 */
	public abstract void updatePlayerList(List<Player> players);
	
	/**
	 * Add players to the bottom of the game queue.
	 * @param players The players to add. Players already in the game are ignored.
	 */
	public abstract void addNewPlayersToBottom(List<Player> players);

	/**
	 * Set the players in the game queue
	 * @param players The ordered game queue player list
	 */
	public void setPlayers(List<UUID> players){
		synchronized (playerListLock) {
			LinkedList<Player> tempCurrent = new LinkedList<Player>(TournamentLogic.this.players);
			LinkedList<Player> tempSorted = new LinkedList<Player>();

			for (UUID player:players){
				for (int i = 0; i < tempCurrent.size(); i++){
					Player p = TournamentLogic.this.players.get(i);
					if (p.getUUID().equals(player)){
						tempSorted.add(p);
					}
				}
			}
			TournamentLogic.this.players = tempSorted;
		}
		notifyDataSetChanged();
		updateActivity();
	}

	/**
	 * Set the player extra data. For use by network message handlers
	 * @param extras HashMap of player extra data
	 */
	public void setPlayerExtras(HashMap<UUID, KingOfTheHillPlayerExtra> extras){
		for (UUID uuid: extras.keySet()){
			if (king != null && king.getUUID().equals(uuid)){
				this.playersExtraData.put(king, extras.get(uuid));
			} else {
				for (Player p : players){
					if (p.getUUID().equals(uuid)){
						this.playersExtraData.put(p, extras.get(uuid));
					}
				}
			}

		}
	}
	
	/**
	 * Set player's extra data
	 * @param extra KingOfTheHillPlayerExtra
	 */
	public void setPlayerExtra(KingOfTheHillPlayerExtra extra){
		if (king != null && king.getUUID().equals(extra.getPlayerUUID())){
			this.playersExtraData.put(king, extra);
		} else {
			for (Player p : players){
				if (p.getUUID().equals(extra.getPlayerUUID())){
					this.playersExtraData.put(p, extra);
				}
			}
		}
	}
	
	/**
	 * Get the current tournament data as text
	 * @return Tournament data
	 */
	public String getTournamentData(){
		StringBuilder s = new StringBuilder();
		s.append("Current King: ");
		s.append(king.getName());
		s.append("\n\t");
		s.append("Wins: ");
		s.append(getPlayerExtra(king).getWins());
		s.append("\n\tLosses: ");
		s.append(getPlayerExtra(king).getLosses());
		
		for (Player p : players){
			s.append("\n\n");
			s.append(p.getName());
			s.append("\n\t");
			s.append("Wins: ");
			s.append(getPlayerExtra(p).getWins());
			s.append("\n\tLosses: ");
			s.append(getPlayerExtra(p).getLosses());
		}
		return s.toString();
	}
	
	/**
	 * End the tournament through the main activity.
	 */
	public void endTournament(){
		activity.endTournament();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		synchronized (playerListLock) {
			Player player = players.get(position);

			View row = convertView;
			if (row == null) {
				LayoutInflater vi = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = vi.inflate(R.layout.list_player_item, parent, false);
			}

			//set the player name
			TextView profileName = (TextView)row.findViewById(R.id.playerName);
			profileName.setText(player.getName());

			//set the profile picture
			ImageView portrait = (ImageView)row.findViewById(R.id.playerPortrait);
			portrait.setImageBitmap(player.getPortrait());

			//set player wins/losses
			TextView standings = (TextView)row.findViewById(R.id.playerStanding);
			KingOfTheHillPlayerExtra extra = getPlayerExtra(player);
			if (extra.getWins() != -1 && extra.getLosses() != -1){
				standings.setText(extra.getWins() + "W " + extra.getLosses() + "L");
			} else {
				standings.setText("");
			}
			
			//set background if challenger
			ImageView crown = (ImageView)row.findViewById(R.id.playerKingImage);
			if (position == 0){
				row.setBackgroundResource(R.drawable.challengershape);
				crown.setImageResource(R.drawable.sword);
			} else {
				if (defaultRowBackground != null){
					defaultRowBackground = row.getBackground();
				}
				crown.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
				row.setBackgroundDrawable(defaultRowBackground);
			}

			//set the slide up animation on this row
			if (scrollState == 0){
				Animation animation = AnimationUtils.loadAnimation(context, R.anim.anim_slide_up);
				row.setAnimation(animation);
			}
			
			return row;

		}
	}
	
	/**
	 * Holding variable for default row background drawable
	 */
	private Drawable defaultRowBackground;
	
	/**
	 * The current scroll state of the listview
	 */
	private int scrollState = 0;
	
	/**
	 * Set the current scroll state of the listview
	 * @param scrollState The scroll state
	 */
	public void setScrollState(int scrollState){
		this.scrollState = scrollState;
	}

	/**
	 * Get this tournament's email handler
	 * @return AutomaticEmailHandler
	 */
	public AutomaticEmailHandler getAutomaticEmailHandler() {
		return emailHandler;
	}
}
