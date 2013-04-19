package utool.plugin.kingofthehill;

import java.util.ArrayList;
import java.util.StringTokenizer;

import utool.plugin.activity.AbstractPluginCommonReference;
import utool.plugin.kingofthehill.communications.AutomaticEmailHandler;
import utool.plugin.kingofthehill.tournament.TournamentLogic;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Controller for email options activity
 * @author Cory
 *
 */
public class EmailActivity extends AbstractPluginCommonReference {

	/**
	 * Holds the arrayAdapter
	 */
	private AdvancedOptionsAdapter ad;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_email);

		//setup adapter
		AutomaticEmailHandler a = TournamentLogic.getInstance(getTournamentId()).getAutomaticEmailHandler();
		ArrayList<String> emails = a.getSubscribers();
		int size = emails.size();
		emails.addAll(a.getPossibleSubscribers());

		ListView l = (ListView)findViewById(R.id.email_subscribers);
		ad=new AdvancedOptionsAdapter(this, R.id.email_subscribers, emails);
		l.setAdapter(ad);

		//load email addresses from preferences and add to list if unique
		SharedPreferences prefs = getSharedPreferences("utool.plugin.singleelimination", Context.MODE_PRIVATE);
		String em= prefs.getString("email_addresses", ""); 
		StringTokenizer e = new StringTokenizer(em, ",");
		while(e.hasMoreTokens())
		{
			addPossibleSubscriber(emails, e.nextToken());
		}

		ArrayList<Boolean> ton = new ArrayList<Boolean>();
		for(int i=0;i<emails.size();i++)
		{
			if(i<size)
			{
				ton.add(true);
			}
			else
			{
				ton.add(false);
			}
		}
		ad.turnedOn= ton ;
		ad.notifyDataSetChanged();

		//setup add button
		ImageButton plus = (ImageButton)findViewById(R.id.email_plus);
		plus.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) 
			{
				EditText ea = (EditText)findViewById(R.id.email_address);

				//add typed in email to list
				ad.add(ea.getText().toString());
				ad.notifyDataSetChanged();
				reloadUI();

			}

		});

		//setup save button
		Button save = (Button)findViewById(R.id.adv_save);
		save.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) 
			{
				//save settings to tournament's email object and exit
				AutomaticEmailHandler a = TournamentLogic.getInstance(getTournamentId()).getAutomaticEmailHandler();
				ArrayList<String> subs = new ArrayList<String>();
				ArrayList<String> psubs = new ArrayList<String>();
				ArrayList<String> emails = ad.addresses;
				ArrayList<Boolean> on = ad.turnedOn;
				for(int i=0;i<emails.size();i++)
				{
					if(on.get(i))
					{
						//add to subscriber since checked
						subs.add(emails.get(i));
					}
					else
					{
						//add to possible subscriber since unchecked
						psubs.add(emails.get(i));
					}
				}

				a.setSubscribers(subs);
				a.setPossibleSubscribers(psubs);

				String ems = "";
				for(int i=0;i<subs.size();i++)
				{
					ems+=subs.get(i)+",";
				}
				for(int i=0;i<psubs.size();i++)
				{
					ems+=psubs.get(i)+",";
				}

				//save list to preferences
				SharedPreferences prefs = getSharedPreferences("utool.plugin.kingofthehill", Context.MODE_PRIVATE);
				prefs.edit().putString("email_addresses", ems).commit();
				finish();
			}

		});
		reloadUI();
	}

	/**
	 * Adds nextToken to list if not already in emails
	 * @param emails list of addresses
	 * @param nextToken email to add if unique
	 */
	public void addPossibleSubscriber(ArrayList<String> emails, String nextToken) 
	{
		for(int i=0;i<emails.size();i++)
		{
			if(emails.get(i).equals(nextToken))
			{
				return;
			}
		}

		//not in list
		emails.add(nextToken);
	}

	/**
	 * Re-registers listview for the context menu
	 */
	private void reloadUI()
	{
		ListView l = (ListView)findViewById(R.id.email_subscribers);
		l.setOnCreateContextMenuListener(this);
		registerForContextMenu(l);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_options, menu);
		return true;
	}

	/**
	 * This class is responsible for setting up the list of players to display in the list view
	 * @author waltzm
	 * @version 12/11/2012
	 */
	private class AdvancedOptionsAdapter extends ArrayAdapter<String>{

		/**
		 * Holds the list of players
		 */
		private ArrayList<String> addresses;

		/**
		 * Holds whether addresses are subscribed
		 */
		private ArrayList<Boolean> turnedOn;

		/**
		 * Simple constructor to hide the annoying stuff
		 * @param context the application context
		 * @param textViewResourceId the list id
		 * @param addresses list of addresses
		 */
		public AdvancedOptionsAdapter(Context context, int textViewResourceId, ArrayList<String> addresses)
		{
			super(context, textViewResourceId, addresses);
			this.addresses = addresses;
			turnedOn = new ArrayList<Boolean>();
			for(int i=0;i<addresses.size();i++)
			{
				turnedOn.add(false);
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent){
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.list_email_item, parent, false);

			//add address
			TextView adr = (TextView)row.findViewById(R.id.adv_address);
			adr.setText(addresses.get(position));

			CheckBox box = (CheckBox)row.findViewById(R.id.checkBox1);
			box.setOnCheckedChangeListener(new OnCheckChangedListener_AdvancedOptions(position));
			if(turnedOn.get(position))
			{

				box.setChecked(true);
			}
			else
			{
				box.setChecked(false);
			}



			row.invalidate();
			return row;
		}

		@Override
		public void add(String item)
		{
			addresses.add(item);
			turnedOn.add(true);
		}

		/**
		 * Turns the email address at the position on or off
		 * @param position the position of the address
		 * @param state on or off
		 */
		public void setTurnedOn(int position, boolean state)
		{
			turnedOn.set(position,state);
		}

	}

	/**
	 * Custom listener to update the player based on if the check box is checked
	 * @author waltzm
	 *
	 */
	private class OnCheckChangedListener_AdvancedOptions implements OnCheckedChangeListener
	{
		/**
		 * Holds the position
		 */
		private int position;

		/**
		 * Constructor that accepts the position for the checkbox.
		 * @param position the position
		 */
		public OnCheckChangedListener_AdvancedOptions(int position)
		{
			this.position = position;
		}

		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
		{
			ad.setTurnedOn(position, isChecked);
		}

	}
}
