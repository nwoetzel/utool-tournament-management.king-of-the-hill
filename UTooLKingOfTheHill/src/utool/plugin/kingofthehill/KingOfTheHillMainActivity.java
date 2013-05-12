package utool.plugin.kingofthehill;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import utool.plugin.Player;
import utool.plugin.activity.AbstractPluginMainReference;
import utool.plugin.activity.PluginCommonActivityHelper;
import utool.plugin.kingofthehill.communications.IncomingCommandHandler;
import utool.plugin.kingofthehill.tournament.KingOfTheHillPlayerExtra;
import utool.plugin.kingofthehill.tournament.KingOfTheHillTournament;
import utool.plugin.kingofthehill.tournament.TournamentLogic;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Main activity for the King of the Hill plugin
 * @author Cory
 */
@SuppressLint("NewApi")
public class KingOfTheHillMainActivity extends AbstractPluginMainReference {
	
	/**
	 * Log tag
	 */
	private static final String LOG_TAG = "utool.plugin.kingofthehill.KingOfTheHillMainActivity";
	
	/**
	 * Tournament controller object
	 */
	private TournamentLogic tournament;
	
	/**
	 * Handler for updating the activity from other threads
	 */
	private final Handler updateHandler = new Handler();
	
	/**
	 * "Null" value for player moving
	 */
	private static final int PLAYER_TO_MOVE_NULL = -2;
	
	/**
	 * Temporary holding variable for moving players
	 */
	private int playerToMove = PLAYER_TO_MOVE_NULL;
	
	/**
	 * Infinity ring constant
	 */
	private static final String infinityRing = "\u221E";
	
	/**
	 * Runnable associated with updateHandler
	 */
	private final Runnable updateRunnable = new Runnable() {
        public void run() {
            //call the activity method that updates the UI
            updateActivity();
        }
	};
	
	/**
	 * Timer updater handler
	 */
	Handler timerHandler = new Handler();
	
	/**
	 * Timer updater runnable
	 */
	private final Runnable timerRunnable = new Runnable() {
		public void run() {
			TextView gameTimerView = (TextView) findViewById(R.id.gameTimerTextView);
			TextView roundTimerView = (TextView) findViewById(R.id.roundTimerTextView);
			
			int gameTimer = tournament.getRemainingGameTime();
			int roundTimer = tournament.getRemainingRoundTime();
			if (gameTimer == TournamentLogic.TIMER_NOT_SET){
				gameTimerView.setText(infinityRing);
			} else {
				gameTimerView.setText(secondsToHHmmss(gameTimer));
			}
			if (roundTimer == TournamentLogic.TIMER_NOT_SET){
				roundTimerView.setText(infinityRing);
			} else {
				roundTimerView.setText(secondsToHHmmss(roundTimer));
			}
			timerHandler.postDelayed(timerRunnable, 500);
		}
	};
	
	/**
	 * Convert seconds to HHmmss format
	 * @param seconds Seconds to convert
	 * @return A string in HH:mm:ss, mm:ss, or ss format
	 */
	private String secondsToHHmmss(int seconds){
		String time = "";
//		int hours = (int) TimeUnit.SECONDS.toHours(seconds);
//		seconds -= TimeUnit.HOURS.toSeconds(hours);
//        int minutes = (int) TimeUnit.SECONDS.toMinutes(seconds);
//        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
        //Note: The above code requires API level 9, we support as low as API level 8
		
		int hours = seconds/3600;
		seconds -= hours*3600;
		int minutes = seconds/60;
		seconds -= minutes*60;
		
		
		if (hours > 0){
			time += hours;
			if (minutes < 10){
				time += ":0" + minutes;
			} else {
				time += ":" + minutes;
			}
			if (seconds < 10){
				time += ":0" + seconds;
			} else {
				time += ":" + seconds;
			}
			
		} else if (minutes > 0){
			time += minutes;
			if (seconds < 10){
				time += ":0" + seconds;
			} else {
				time += ":" + seconds;
			}
		} else {
			time += seconds;
		}
		return time;
	}
	
	/**
	 * Temporary holding variable for the king
	 * Used to determine when to show animations
	 */
	private Player kingTemp;
	
	/**
	 * Request code for calling tournament configuration screen in core
	 */
	private static final int UTOOL_TOURNAMENT_CONFIG_REQUEST_CODE = 1;
	
	/**
	 * Data reception threads
	 */
	private static HashMap<Long, Thread> receiveThreads = new HashMap<Long, Thread>();
	
	/**
	 * Runnable for data reception thread
	 */
	private final Runnable receiveRunnable = new Runnable() 
	{
		public void run() 
		{
			try {
				Log.d("KOTH", "Receive thread running");
				while (true)
				{
					String msg = pluginHelper.mICore.receive();
					Log.d("KOTH", "Message received");
					if (msg.equals(PluginCommonActivityHelper.UTOOL_SOCKET_CLOSED_MESSAGE)){
						Log.d("KOTH", "Receive thread closing");
						return;
					}
					runOnUiThread(new MessageHandler(msg));
					
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * Runnable class for handling received messages on the UI thread.
	 * Handling these messages on a separate thread causes issues when updating the UI with the new data.
	 * @author Cory
	 *
	 */
	private class MessageHandler implements Runnable{
		/**
		 * The message received
		 */
		private String message;
		
		/**
		 * Constructor
		 * @param message The message received
		 */
		public MessageHandler(String message){
			this.message = message;
		}

		@Override
		public void run() {
			IncomingCommandHandler handler = tournament.getIncomingCommandHandler();
			handler.handleMessage(message);
		}
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//remove title bar if under honeycomb
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		setContentView(R.layout.activity_king_of_the_hill_main);
				
		
		//android.os.Debug.waitForDebugger();
		
		TextView king = (TextView)findViewById(R.id.playerName);
		king.setText("Please Wait...");
	}
	
	@Override 
	protected void onResume(){
		super.onResume();
		if (!isNewInstance() && pluginHelper.mICore != null){
			tournament = TournamentLogic.getInstance(pluginHelper.getTournamentId());
			updateActivity();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//use a different menu for participants
		if (pluginHelper.getPermissionLevel() == Player.HOST){
			getMenuInflater().inflate(R.menu.activity_king_of_the_hill_main, menu);
		} else {
			getMenuInflater().inflate(R.menu.activity_king_of_the_hill_main_participant, menu);
		}

		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent i = pluginHelper.getTournamentConfigurationIntent();
			startActivityForResult(i, UTOOL_TOURNAMENT_CONFIG_REQUEST_CODE);
			return true;
		case R.id.menu_options:
			Intent optionsIntent = pluginHelper.getNewIntent(this, OptionsActivity.class);
			startActivity(optionsIntent);
			return true;
		case R.id.menu_help:
			showHelp();
			return true;
		case R.id.menu_standings:
			Intent standingsIntent = pluginHelper.getNewIntent(this, StandingsActivity.class);
			startActivity(standingsIntent);
			return true;
		case R.id.menu_restart:
			promptRestartTournament();
			return true;
		case R.id.menu_disconnect:
			promptEndTournament();
			return true;
		case R.id.menu_terminate:
			promptEndTournament();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		pluginHelper.unbindService();
	}
	
	/**
	 * Display yes/no prompt for ending the tournament
	 */
	public void promptEndTournament(){
		//Construct the Yes/No prompt dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				endTournament();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		if (pluginHelper.getPermissionLevel() == Player.HOST){
			builder.setMessage("Are you sure you want to terminate this tournament?");
		} else {
			builder.setMessage("Are you sure you want to disconnect from this tournament?");
		}
		//Show the Yes/No prompt dialog
		builder.show();
	}
	
	/**
	 * Display yes/no prompt for restarting the tournament
	 */
	public void promptRestartTournament(){
		//Construct the Yes/No prompt dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				restartTournament();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setMessage("Are you sure you want to restart this tournament?");
		//Show the Yes/No prompt dialog
		builder.show();
	}
	
	/**
	 * End this tournament. Clients will just disconnect from the server.
	 */
	public void endTournament(){
		try {
			pluginHelper.mICore.close();
		} catch (Exception e) {
			//Catch all exceptions so the plugin doesn't crash
		}
		TournamentLogic.removeInstance(pluginHelper.getTournamentId());
		finish();
	}
	
	/**
	 * Restart this tournament.
	 */
	private void restartTournament(){
		LinkedList<Player> playerList;
		try {
			playerList = new LinkedList<Player>(pluginHelper.mICore.getPlayerList());
			Player king = playerList.poll();
			((KingOfTheHillTournament)tournament).restartTournament(king, playerList);
		} catch (RemoteException e) {
			//do nothing
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {     
		super.onActivityResult(requestCode, resultCode, data); 
		switch(requestCode) { 
			case (UTOOL_TOURNAMENT_CONFIG_REQUEST_CODE):{
				if (resultCode == Activity.RESULT_OK)
				{
					updatePlayerList();
				}
				break;
			}
		}
	}
	
	/**
	 * Update the player list based on the current core player list
	 */
	private void updatePlayerList(){
		try {
			if (!pluginHelper.mICore.isClient()){
				List<Player> playerList = pluginHelper.mICore.getPlayerList();
				tournament.updatePlayerList(playerList);
			}
			if (tournament.getKing() != null){
				setKing(tournament.getKing());
			}
		} catch (RemoteException e) {
		}	
	}
	
	/**
	 * Show the help dialog
	 */
	private void showHelp(){
		final Dialog dialog = new Dialog(KingOfTheHillMainActivity.this);
		dialog.setContentView(R.layout.activity_main_help);
		dialog.setTitle("King of the Hill Help");
		dialog.setCancelable(true);
		Button closeButton = (Button) dialog.findViewById(R.id.home_help_close_button);
		closeButton.setOnClickListener(new Button.OnClickListener() {      
			public void onClick(View view) { 
				dialog.dismiss();     
			}
		});
		dialog.show();
	}
	
	/**
	 * Show the set score dialog
	 */
	private void showSetScoreDialog(){
		final Dialog dialog = new Dialog(KingOfTheHillMainActivity.this);
		dialog.setContentView(R.layout.activity_set_scores);
		dialog.setTitle("Set Score");
		dialog.setCancelable(true);
		
		int playerIndex = playerToMove;
		KingOfTheHillPlayerExtra extra;
		if (playerIndex == TournamentLogic.KING_POSITION){
			extra = tournament.getPlayerExtra(tournament.getKing());
		} else {
			extra = tournament.getPlayerExtra(tournament.getPlayers().get(playerIndex));
		}
		//set wins/losses in view
		EditText winsField = (EditText)dialog.findViewById(R.id.playerWinsScoreField);
		EditText lossesField = (EditText)dialog.findViewById(R.id.playerLossesScoreField);
		int wins = extra.getWins();
		if (wins == -1){
			wins = 0;
		}
		int losses = extra.getLosses();
		if (losses == -1){
			losses = 0;
		}
		winsField.setText(Integer.toString(wins));
		lossesField.setText(Integer.toString(losses));
		
		//program +/- buttons
		Button winsPlus = (Button)dialog.findViewById(R.id.playerWinsPlusBtn);
		Button winsMinus = (Button)dialog.findViewById(R.id.playerWinsMinusBtn);
		Button lossesPlus = (Button)dialog.findViewById(R.id.playerLossesPlusBtn);
		Button lossesMinus = (Button)dialog.findViewById(R.id.playerLossesMinusBtn);
		
		winsPlus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText winsField = (EditText)dialog.findViewById(R.id.playerWinsScoreField);
				int current = Integer.parseInt(winsField.getText().toString());
				current += 1;
				winsField.setText(Integer.toString(current));
			}
		});
		
		winsMinus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText winsField = (EditText)dialog.findViewById(R.id.playerWinsScoreField);
				int current = Integer.parseInt(winsField.getText().toString());
				if (current > 0){
					current -= 1;
				}
				winsField.setText(Integer.toString(current));
			}
		});
		
		lossesPlus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText lossesField = (EditText)dialog.findViewById(R.id.playerLossesScoreField);
				int current = Integer.parseInt(lossesField.getText().toString());
				current += 1;
				lossesField.setText(Integer.toString(current));
			}
		});
		
		lossesMinus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText lossesField = (EditText)dialog.findViewById(R.id.playerLossesScoreField);
				int current = Integer.parseInt(lossesField.getText().toString());
				if (current > 0){
					current -= 1;
				}
				lossesField.setText(Integer.toString(current));
			}
		});
		
		Button setScoreButton = (Button) dialog.findViewById(R.id.saveBtn);
		setScoreButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText winsField = (EditText)dialog.findViewById(R.id.playerWinsScoreField);
				EditText lossesField = (EditText)dialog.findViewById(R.id.playerLossesScoreField);
				int wins = 0;
				int losses = 0;
				try{
					wins = Integer.parseInt(winsField.getText().toString());
				} catch (Exception e){/*Dodge the crash*/}
				
				try{
					losses = Integer.parseInt(lossesField.getText().toString());
				} catch (Exception e){/*Dodge the crash*/}
				
				KingOfTheHillPlayerExtra extra;
				if (playerToMove == TournamentLogic.KING_POSITION){
					extra = tournament.getPlayerExtra(tournament.getKing());
				} else {
					extra = tournament.getPlayerExtra(tournament.getPlayers().get(playerToMove));
				}
				KingOfTheHillPlayerExtra newExtra = new KingOfTheHillPlayerExtra(extra.getPlayerUUID(), wins, losses);
				tournament.setPlayerExtra(newExtra);
				playerToMove = PLAYER_TO_MOVE_NULL;
				updateActivity();
				
				dialog.dismiss();
			}
		});
		
		Button closeButton = (Button) dialog.findViewById(R.id.cancelBtn);
		closeButton.setOnClickListener(new Button.OnClickListener() {      
			public void onClick(View view) {
				playerToMove = PLAYER_TO_MOVE_NULL;
				dialog.dismiss();
			}
		});
		
		dialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				playerToMove = PLAYER_TO_MOVE_NULL;
			}
		});
		dialog.show();
	}

	@Override
	public void runOnServiceConnected() {
		try {			
			if (!pluginHelper.mICore.isClient()){
				//Host code
				List<Player> playerList = pluginHelper.mICore.getPlayerList();
				tournament = TournamentLogic.getNewInstance(this, pluginHelper.getTournamentId(), playerList, Player.HOST);
				tournament.setMatchupActivity(this, pluginHelper.mICore);
				tournament.setLocalPlayer(pluginHelper.getPid());
				
				updateActivity();
			} else {
				//Participant code
				List<Player> playerList = pluginHelper.mICore.getPlayerList();
				tournament = TournamentLogic.getNewInstance(this, pluginHelper.getTournamentId(), playerList, Player.PARTICIPANT);
				tournament.setMatchupActivity(this, pluginHelper.mICore);
				tournament.setLocalPlayer(pluginHelper.getPid());
				ListView playerListView = (ListView)findViewById(R.id.playersQueueListView);
				playerListView.setAdapter(this.tournament);
				tournament.getOutgoingCommandHandler().requestGameState();
			}
			
			Thread receiveThread = receiveThreads.get(pluginHelper.getTournamentId());
			if (isNewInstance() || receiveThread == null || (receiveThread != null && !receiveThread.isAlive())){
				Log.d("KOTH", "Creating receive thread");
				receiveThread = new Thread(receiveRunnable);
				receiveThread.start();
				receiveThreads.put(pluginHelper.getTournamentId(), receiveThread);
			} else {
				Log.d("KOTH", "Not creating receive thread: " + isNewInstance() + " " + receiveThread.toString());
			}
			timerRunnable.run();
		} catch (RemoteException e) {
		}
	}

	@Override
	public void runOnServiceDisconnected() {
		Log.e(LOG_TAG, "Service has unexpectedly disconnected");
		//If the service has disconnected, either the plugin is closing or the core has died. Do some cleanup.
		//TournamentLogic.removeInstance(pluginHelper.getTournamentId());
		endTournament();
	}
	
	/**
	 * Set a player as the king in the UI. Does not touch the tournament logic, and should only be called based on results from a logic class.
	 * @param player The new king
	 */
	public void setKing(Player player){
		ImageView portrait = (ImageView)findViewById(R.id.playerPortrait);
		ImageView crown = (ImageView)findViewById(R.id.playerKingImage);
		portrait.setImageBitmap(player.getPortrait());
		crown.setImageResource(R.drawable.crown);
		TextView name = (TextView)findViewById(R.id.playerName);
		name.setText(player.getName());
		if (player.getUUID().equals(tournament.getLocalPlayer())){
			name.setTextColor(Color.CYAN);
		} else {
			name.setTextColor(Color.WHITE);
		}
		
		//set player wins/losses
		TextView standings = (TextView)findViewById(R.id.playerStanding);
		KingOfTheHillPlayerExtra extra = tournament.getPlayerExtra(player);
		if (extra.getWins() != -1 && extra.getLosses() != -1){
			String standingsText = extra.getWins() + "W " + extra.getLosses() + "L " + tournament.getKingWinsStreakCount() + "WRnd";
			standings.setText(standingsText);
		} else {
			standings.setText("");
		}
		
		View kingItem = findViewById(R.id.listPlayerItemLayout);
		
		//Set king background
		kingItem.setBackgroundResource(R.drawable.kingshape);
		
		//On click listener for king view
		kingItem.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (tournament instanceof KingOfTheHillTournament && playerToMove != PLAYER_TO_MOVE_NULL){
					//move the selected player to this position
					((KingOfTheHillTournament)tournament).movePlayer(playerToMove, TournamentLogic.KING_POSITION);
					playerToMove = PLAYER_TO_MOVE_NULL;
				}
				else if (tournament instanceof KingOfTheHillTournament){
					moveChallengerToEnd();
				}
			}
		});
		
		//Long click listener for king view
		kingItem.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (tournament instanceof KingOfTheHillTournament && playerToMove == PLAYER_TO_MOVE_NULL){
					//prepare to move the player
					playerQueueListView_onItemLongClick(v, TournamentLogic.KING_POSITION);
				}
				
				return true;
			}
		});
		
		if (!tournament.getUseSlideAnimations()){
			//Players have been moved, so don't do slide animations
			kingItem.clearAnimation();
			kingTemp = player;
		}
		if ((kingTemp == null || !kingTemp.equals(player)) && tournament.getUseSlideAnimations()){
			kingTemp = player;
			Animation animation = AnimationUtils.loadAnimation(KingOfTheHillMainActivity.this, R.anim.anim_slide_up);
			kingItem.setAnimation(animation);
		}
	}
	
	/**
	 * Move the current king to the end of the queue and promote the challenger.
	 * This occurs when the king loses the match.
	 * Updates both the logic class and UI.
	 * Only call on the host.
	 */
	private void moveKingToEnd(){
		//play the animation
		Animation animation = AnimationUtils.loadAnimation(KingOfTheHillMainActivity.this, R.anim.anim_slide_out_left);
		View kingItem = findViewById(R.id.listPlayerItemLayout);
		kingItem.startAnimation(animation);

		//tell the logic class to update after the animation has been performed
		kingItem.postDelayed(new Runnable() {
			public void run() {
				KingOfTheHillTournament t = (KingOfTheHillTournament)tournament;
				t.moveKingToEnd();
				setKing(t.getKing());
			}
		}, animation.getDuration());
	}
	
	/**
	 * Move the current challenger to the end of the queue.
	 */
	public void moveChallengerToEnd(){
		//play the animation
		Animation animation = AnimationUtils.loadAnimation(KingOfTheHillMainActivity.this, R.anim.anim_slide_out_left);
		ListView playerListView = (ListView)findViewById(R.id.playersQueueListView);
		View view = playerListView.getChildAt(0);
		if (view == null){
			return;
		}
		view.startAnimation(animation);
		
		//tell the logic class to update after the animation has been performed.
		view.postDelayed(new Runnable() {
			public void run() {
				((KingOfTheHillTournament)tournament).moveChallengerToEnd();
				setKing(tournament.getKing());
			}
		}, animation.getDuration());
	}
	
	/**
	 * Notify the activity to update its views
	 */
	public void updateActivityExternal(){
		updateHandler.post(updateRunnable);
	}

	/**
	 * Update the activity's views
	 */
	private void updateActivity() {
		updatePlayerList();
		
		TextView tournamentNameTextViewLabel = (TextView)findViewById(R.id.tournamentNameTextViewLabel);
		try{
			tournamentNameTextViewLabel.setText(pluginHelper.mICore.getTournamentName());
		} catch (RemoteException e){
			//do nothing, shouldn't happen
		}
		
		
		ListView playerListView = (ListView)findViewById(R.id.playersQueueListView);
		playerListView.setAdapter(this.tournament);
		playerListView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				tournament.setScrollState(scrollState);
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				//do nothing
			}
		});
		//OnClick listener for player list view
		playerListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
				if (tournament instanceof KingOfTheHillTournament && playerToMove != PLAYER_TO_MOVE_NULL){
					//move the selected player to this position
					((KingOfTheHillTournament)tournament).movePlayer(playerToMove, position);
					playerToMove = PLAYER_TO_MOVE_NULL;
				} else if (tournament instanceof KingOfTheHillTournament && position == 0){
					moveKingToEnd();
				}
			}
		});
		//OnLongClick listener for player list view
		playerListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View v, int position, long arg3) {
				playerQueueListView_onItemLongClick(v, position);
				return true;
			}
		});
	}
	
	/**
	 * OnItemLongClickListener for players
	 * @param view View
	 * @param index Index
	 * @return Boolean
	 * @see OnItemLongClickListener
	 */
	private boolean playerQueueListView_onItemLongClick(View view, int index) {
		final String move = "Move Player";
		final String setScore = "Set Score";
		
		List<String> menuItems = new LinkedList<String>();
		//Details
		menuItems.add(move);
		menuItems.add(setScore);
		AlertDialog.Builder builder = new AlertDialog.Builder(KingOfTheHillMainActivity.this);
		
		String[] items = new String[0];
		items = menuItems.toArray(items);

		/**
		 * Inner class for handling the menu choice
		 */
		class dialogOnClick implements DialogInterface.OnClickListener {
			private String[] items;
			View player;
			int position;

			public dialogOnClick(String[] items, View player, int position){
				this.items = items;
				this.player = player;
				this.position = position;
			}

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String item = items[which];
				if (tournament instanceof KingOfTheHillTournament && playerToMove == PLAYER_TO_MOVE_NULL){
					if (item.equals(move)){
						//prepare to move the player
						playerToMove = position;
						Animation animation = AnimationUtils.loadAnimation(KingOfTheHillMainActivity.this, R.anim.anim_wiggle);
						player.startAnimation(animation);

					} else if (item.equals(setScore)){
						//trigger score dialog
						playerToMove = position;
						showSetScoreDialog();
					}
				}
			}

		}
		builder.setItems(items, new dialogOnClick(items, view, index));
		builder.show();
		return true;
	}

}
