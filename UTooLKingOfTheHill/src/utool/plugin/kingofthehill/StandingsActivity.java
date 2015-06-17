package utool.plugin.kingofthehill;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import utool.plugin.Player;
import utool.plugin.activity.AbstractPluginCommonReference;
import utool.plugin.kingofthehill.tournament.KingOfTheHillPlayerExtra;
import utool.plugin.kingofthehill.tournament.TournamentLogic;

/**
 * Controller class for Standings activity
 * @author Cory
 *
 */
public class StandingsActivity extends AbstractPluginCommonReference {
	
	/**
	 * Tournament controller object
	 */
	private TournamentLogic tournament;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//remove title bar if under honeycomb
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		setContentView(R.layout.activity_standings);
		
		tournament = TournamentLogic.getInstance(pluginHelper.getTournamentId());
		
		RadioButton name = (RadioButton) findViewById(R.id.sortByNameRadioButton);
		name.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateList();
				
			}
		});
		
		RadioButton wins = (RadioButton) findViewById(R.id.sortByWinsRadioButton);
		wins.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateList();
				
			}
		});
		RadioButton losses = (RadioButton) findViewById(R.id.sortByLossesRadioButton);
		losses.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateList();
				
			}
		});
		
		updateList();
	}
	
	/**
	 * Update the list UI
	 */
	private void updateList(){
		List<Player> players = tournament.getPlayers();
		players.add(tournament.getKing());
		
		HashMap<Player, KingOfTheHillPlayerExtra> extras = tournament.getPlayerExtras();
		
		List<KingOfTheHillPlayerExtra> extras2 = new LinkedList<KingOfTheHillPlayerExtra>(extras.values());
		
		RadioButton wins = (RadioButton) findViewById(R.id.sortByWinsRadioButton);
		RadioButton losses = (RadioButton) findViewById(R.id.sortByLossesRadioButton);
		RadioButton name = (RadioButton) findViewById(R.id.sortByNameRadioButton);
		if (wins.isChecked()){
			Collections.sort(extras2);
		} else if (losses.isChecked()) {
			Collections.sort(extras2, new Comparator<KingOfTheHillPlayerExtra>() {
				@Override
				public int compare(KingOfTheHillPlayerExtra lhs, KingOfTheHillPlayerExtra rhs) {
					if (lhs.getLosses() > rhs.getLosses()){
						return -1;
					} else if (lhs.getLosses() < rhs.getLosses()){
						return 1;
					} else {
						return 0;
					}
				}
			});
		}
		
		List<Player> playersTemp = new LinkedList<Player>();
		if (!name.isChecked()){
			//sort players by extras
			for (KingOfTheHillPlayerExtra e : extras2){
				UUID uuid = e.getPlayerUUID();
				for (Player p : players){
					if (p.getUUID().equals(uuid)){
						playersTemp.add(p);
						break;
					}
				}
			}
		} else {
			//sort players by name
			playersTemp.addAll(players);
			Collections.sort(playersTemp, new Comparator<Player>() {
				@Override
				public int compare(Player lhs, Player rhs) {
					return lhs.getName().compareTo(rhs.getName());
				}
			});
		}
		
		StandingsList listAdapter = new StandingsList(playersTemp);
		
		ListView listView = (ListView)findViewById(R.id.playerStandingsListView);
		listView.setAdapter(listAdapter);
		
	}
	
	/**
	 * Adapter class for standings list
	 * @author Cory
	 *
	 */
	private class StandingsList extends BaseAdapter{
		/**
		 * List of players to display
		 */
		private List<Player> players;
		
		/**
		 * Constructor
		 * @param sortedPlayers List of presorted players to display
		 */
		public StandingsList(List<Player> sortedPlayers){
			this.players = sortedPlayers;
		}

		@Override
		public int getCount() {
			return players.size();
		}

		@Override
		public Object getItem(int position) {
			return players.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Player player = players.get(position);

			View row = convertView;
			if (row == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = vi.inflate(R.layout.list_player_item, parent, false);
			}

			//set the player name
			TextView profileName = (TextView)row.findViewById(R.id.playerName);
			profileName.setText(player.getName());
			if (player.getUUID().equals(tournament.getLocalPlayer())){
				profileName.setTextColor(Color.CYAN);
			} else {
				profileName.setTextColor(Color.WHITE);
			}

			//set the profile picture
			ImageView portrait = (ImageView)row.findViewById(R.id.playerPortrait);
			portrait.setImageBitmap(player.getPortrait());

			//set player wins/losses
			TextView standings = (TextView)row.findViewById(R.id.playerStanding);
			KingOfTheHillPlayerExtra extra = tournament.getPlayerExtra(player);
			if (extra.getWins() != -1 && extra.getLosses() != -1){
				standings.setText(extra.getWins() + "W " + extra.getLosses() + "L");
			} else {
				standings.setText("");
			}
			
			ImageView crown = (ImageView)row.findViewById(R.id.playerKingImage);
			if (tournament.getKing().equals(player)){
				crown.setImageResource(R.drawable.crown);
				row.setBackgroundResource(R.drawable.kingshape);
			} else if (tournament.getPlayers().get(0).equals(player)){
				crown.setImageResource(R.drawable.sword);
				row.setBackgroundResource(R.drawable.challengershape);
			} else {
				if (defaultRowBackground != null){
					defaultRowBackground = row.getBackground();
				}
				crown.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
				row.setBackgroundDrawable(defaultRowBackground);
			}
			return row;
		}
	}
	
	/**
	 * Holding variable for default row background drawable
	 */
	private Drawable defaultRowBackground;

}
