

package utool.plugin.kingofthehill;

import java.util.ArrayList;
import java.util.List;
import utool.plugin.activity.AbstractPluginCommonActivity;
import utool.plugin.email.Contact;
import utool.plugin.email.ContactDAO;
import utool.plugin.kingofthehill.communications.AutomaticMessageHandler;
import utool.plugin.kingofthehill.tournament.TournamentLogic;
import android.app.Dialog;
import android.content.Context;
import android.database.SQLException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Activity for handling the setting up of subscriber emails.
 * @author waltzm
 * @version 4/20/2013
 */
public class EmailActivity extends AbstractPluginCommonActivity
{
	/**
	 * Holds the arrayAdapter
	 */
	private AdvancedOptionsAdapter ad;
	

	/**
	 * holds access to the database
	 */
	private ContactDAO dao;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		//remove title bar if under honeycomb
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		setContentView(R.layout.koth_options_email);

		//create dao
		dao = new ContactDAO(this);
		
		//setup adapter
		AutomaticMessageHandler a =TournamentLogic.getInstance(getTournamentId()).getAutomaticMessageHandler();
		a.setContext(this);
		ArrayList<Contact> contacts = a.getSubscribers();
		int size = contacts.size();
		contacts.addAll(a.getPossibleSubscribers());

		ListView l = (ListView)findViewById(R.id.email_subscribers);
		ad=new AdvancedOptionsAdapter(this, R.id.email_subscribers, contacts);
		l.setAdapter(ad);

		this.loadContactsDatabase(contacts);

		//create array of isChecked
		ArrayList<Boolean> ton = new ArrayList<Boolean>();
		for(int i=0;i<contacts.size();i++)
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
		View plus = findViewById(R.id.email_plus);
		plus.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) 
			{
				EditText ea = (EditText)findViewById(R.id.email_address);
				String contact = ea.getText().toString();

				//error checking
				if(contact.equals(""))
				{
					//misclick, do not save
					return;
				}


				Contact c;
				//if 9 numbers, then phone number
				try
				{
					//if contact can be parsed to number, is phone
					Long.parseLong(contact);
					c = new Contact(contact, Contact.PHONE_NUMBER);
					Log.d("OptionsEmailTab","Added a phone number");
				}
				catch(NumberFormatException e)
				{
					//else email
					c=new Contact(contact, Contact.EMAIL_ADDRESS);
					Log.d("OptionsEmailTab","Added an email address");
				}

				//clear editText
				ea.setText("");
				//add typed in contact to list
				ad.add(c);
				ad.notifyDataSetChanged();
				reloadUI();

			}

		});

		reloadUI();

	}

	@Override
	public void onPause()
	{
		super.onPause();

		//save settings to tournament's email object and exit
		AutomaticMessageHandler a =TournamentLogic.getInstance(getTournamentId()).getAutomaticMessageHandler();
		ArrayList<Contact> subs = new ArrayList<Contact>();
		ArrayList<Contact> psubs = new ArrayList<Contact>();
		ArrayList<Contact> emails = ad.addresses;
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

		//Log.e(logtag, subs);
		a.setSubscribers(subs);
		a.setPossibleSubscribers(psubs);

		this.saveContactsDatabase(emails);

	}

	/**
	 * Loads the contacts from db into contacts
	 * @param contacts the list of contacts
	 */
	private void loadContactsDatabase(ArrayList<Contact> contacts) 
	{
		try{
			//open connection
			dao.open();

			//load contacts from database and add to list if unique
			List<Contact> dbc= dao.getContactListArray();

			//only add unique ones
			for(int i=0;i<dbc.size();i++)
			{
				addPossibleSubscriber(contacts, dbc.get(i));
			}

		} catch (SQLException e){
			Log.e("SQLException", "Could not open or read from the database", e);
		} finally{
			try{
				//close connection
				dao.close();
			} catch (Exception e){
				//ignore, we wanted it closed... it closed
			}
		}



	}
	/**
	 * Saves the email list and the phone number list to db
	 * @param list the list of contacts
	 */
	private void saveContactsDatabase(List<Contact> list) 
	{
		try{
			//open connection
			dao.open();

			dao.putContactList(list);
		} catch(SQLException e){

		} finally {
			try{
				//close connection
				dao.close();
			} catch (Exception e){
				//ignore, we wanted it closed... it closed
			}
		}
	}
	
	/**
	 * Re-registers listview for the context menu
	 */
	private void reloadUI()
	{
		ListView l = (ListView)findViewById(R.id.email_subscribers);
		l.setOnCreateContextMenuListener(this);
		registerForContextMenu(l);

		//if none in list, hide the pictures
		if(ad.addresses.size()==0)
		{
			findViewById(R.id.imageView3).setVisibility(ImageView.INVISIBLE);
			findViewById(R.id.imageView1).setVisibility(ImageView.INVISIBLE);
		}
		else
		{
			findViewById(R.id.imageView3).setVisibility(ImageView.VISIBLE);
			findViewById(R.id.imageView1).setVisibility(ImageView.VISIBLE);
		}
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_email_options_context_menu, menu);
	}


	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.context_delete:
			ad.addresses.remove(info.position);
			ad.turnedOn.remove(info.position);
			ad.notifyDataSetChanged();
			reloadUI();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * Adds nextToken to list if not already in list of emails
	 * @param emails list of addresses
	 * @param nextToken email to add if unique
	 * @return true if added, false if not added
	 */
	public boolean addPossibleSubscriber(ArrayList<Contact> emails, Contact nextToken) 
	{
		for(int i=0;i<emails.size();i++)
		{
			if(emails.get(i).equals(nextToken))
			{
				return false;
			}
		}

		//not in list
		emails.add(nextToken);
		return true;
	}



	/**
	 * This class is responsible for setting up the list of players to display in the list view
	 * @author waltzm
	 * @version 12/11/2012
	 */
	private class AdvancedOptionsAdapter extends ArrayAdapter<Contact>{

		/**
		 * Holds the list of players
		 */
		private ArrayList<Contact> addresses;

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
		public AdvancedOptionsAdapter(Context context, int textViewResourceId, ArrayList<Contact> addresses)
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
			View row = inflater.inflate(R.layout.koth_options_email_row, parent, false);

			//add address
			TextView adr = (TextView)row.findViewById(R.id.adv_address);
			adr.setText(addresses.get(position).getInfo());
			RadioGroup rg = (RadioGroup)row.findViewById(R.id.options_radiogroup);
			rg.setOnCheckedChangeListener(new OnCheckChangedRadioButton(position));

			if(addresses.get(position).getType()==Contact.EMAIL_ADDRESS)
			{
				rg.check(R.id.email_radiobutton);
				Log.d("Email Tab", "selecting email");
			}
			else
			{
				rg.check(R.id.phone_radiobutton);
				Log.d("Email Tab", "selecting phone");
			}

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
		public void add(Contact item)
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

		/**
		 * CHange the type at position
		 * @param position the position in addresses
		 * @param type the type
		 */
		public void changeType(int position, int type) {
			addresses.get(position).setType(type);

		}

	}


	/**
	 * Custom listener to update the type of the item
	 * If checked the player is set to Moderator, otherwise it is set to Participant
	 * @author waltzm
	 *
	 */
	private class OnCheckChangedRadioButton implements RadioGroup.OnCheckedChangeListener
	{
		/**
		 * Holds the position
		 */
		private int position;

		/**
		 * Constructor that accepts the position for the checkbox.
		 * @param position the position
		 */
		public OnCheckChangedRadioButton(int position)
		{
			this.position = position;
		}

		@Override
		public void onCheckedChanged(RadioGroup rg, int  checkedId) 
		{
			if(checkedId == R.id.email_radiobutton)
			{
				ad.changeType(position, Contact.EMAIL_ADDRESS);
				ad.notifyDataSetChanged();
			}
			else
			{
				ad.changeType(position, Contact.PHONE_NUMBER);
				ad.notifyDataSetChanged();
			}
		}

	}

	/**
	 * Custom listener to update the player based on if the check box is checked
	 * If checked the player is set to Moderator, otherwise it is set to Participant
	 * @author waltzm
	 *
	 */
	private class OnCheckChangedListener_AdvancedOptions implements CompoundButton.OnCheckedChangeListener
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

	/**
	 * Displays the help messages for the user
	 */
	private void setupHelp() 
	{
		// Create and show the help dialog.
		final Dialog dialog = new Dialog(EmailActivity.this);
		dialog.setContentView(R.layout.koth_options_email_help);
		dialog.setTitle("UTooL King of the Hill Help");
		dialog.setCancelable(true);
		Button closeButton = (Button) dialog.findViewById(R.id.help_close_button);
		closeButton.setOnClickListener(new Button.OnClickListener() {      
			public void onClick(View view) { 
				dialog.dismiss();     
			}
		});
		dialog.show();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.help:
			this.setupHelp();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
