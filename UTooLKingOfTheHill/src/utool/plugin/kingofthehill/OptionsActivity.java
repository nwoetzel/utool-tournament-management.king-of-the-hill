package utool.plugin.kingofthehill;

import utool.plugin.activity.AbstractPluginCommonReference;
import utool.plugin.kingofthehill.tournament.KingOfTheHillTournament;
import utool.plugin.kingofthehill.tournament.TournamentLogic;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Controller class for the Options activity
 * @author Cory
 *
 */
public class OptionsActivity extends AbstractPluginCommonReference {
	/**
	 * The tournament
	 */
	TournamentLogic tournament;
	
	/**
	 * The current game timer setting
	 */
	private int previousGameTimerSetting;
	/**
	 * The current round timer setting
	 */
	private int previousRoundTimerSetting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//remove title bar if under honeycomb
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		setContentView(R.layout.activity_options);
		
		tournament = TournamentLogic.getInstance(pluginHelper.getTournamentId());
		
		Button sendEmail = (Button)findViewById(R.id.options_send_email);
		Button configureEmail = (Button)findViewById(R.id.options_configure_email);
		
		sendEmail.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendEmail();
			}
		});
		
		configureEmail.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showEmailActivity();
			}
		});
		
		Button applyButton = (Button)findViewById(R.id.applyButton);
		applyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				applySettings();
			}
		});
		
		loadSettings();
	}
	
	/**
	 * Load settings to the UI
	 */
	private void loadSettings(){
		EditText roundTimerEditText = (EditText)findViewById(R.id.roundTimerEditText);
		EditText gameTimerEditText = (EditText)findViewById(R.id.gameTimerEditText);
		
		previousRoundTimerSetting = tournament.getRoundTimerSetting();
		previousGameTimerSetting = tournament.getGameTimerSetting();
		
		roundTimerEditText.setText(Integer.toString(previousRoundTimerSetting));
		gameTimerEditText.setText(Integer.toString(previousGameTimerSetting));
	}
	
	/**
	 * Apply settings to the tournament
	 */
	private void applySettings(){
		EditText roundTimerEditText = (EditText)findViewById(R.id.roundTimerEditText);
		EditText gameTimerEditText = (EditText)findViewById(R.id.gameTimerEditText);
		int roundTimerSetting = 0;
		int gameTimerSetting = 0;
		try {
			roundTimerSetting = Integer.parseInt(roundTimerEditText.getText().toString());
		} catch (Exception e){/*dodge the crash*/}
		
		try{
			gameTimerSetting = Integer.parseInt(gameTimerEditText.getText().toString());
		} catch (Exception e){/*dodge the crash*/}
		
		if (tournament instanceof KingOfTheHillTournament){
			if (roundTimerSetting != previousRoundTimerSetting || roundTimerSetting < 1){
				((KingOfTheHillTournament)tournament).setRoundTimerSetting(roundTimerSetting);
			}
			if (gameTimerSetting != previousGameTimerSetting || gameTimerSetting < 1){
				((KingOfTheHillTournament)tournament).setGameTimerSetting(gameTimerSetting);
			}
			((KingOfTheHillTournament)tournament).sendGameState();
		}
		finish();
	}
	
	/**
	 * Send out emails
	 */
	private void sendEmail(){
		TournamentLogic.getInstance(pluginHelper.getTournamentId()).getAutomaticMessageHandler().sendOutNotifications();
	}
	
	/**
	 * Show the email activity
	 */
	private void showEmailActivity(){
		Intent i = pluginHelper.getNewIntent(this, EmailActivity.class);
		startActivity(i);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_options, menu);
		return true;
	}

}
