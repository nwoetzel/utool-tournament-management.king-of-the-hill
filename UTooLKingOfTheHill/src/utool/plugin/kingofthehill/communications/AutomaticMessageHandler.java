package utool.plugin.kingofthehill.communications;

import java.util.ArrayList;
import java.util.List;

import utool.plugin.email.Contact;
import utool.plugin.email.GMailSender;
import utool.plugin.email.TextSender;
import utool.plugin.kingofthehill.tournament.TournamentLogic;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Manager for the automatic message Handling
 * Sends emails from default email of outgoingEmailAddress.
 * MUST SET CONTEXT BEFORE USING!!!
 * @author waltzm
 * @version 1/14/2013
 */
public class AutomaticMessageHandler 
{
	/**
	 * Outgoing email address to use to send the messages
	 */
	private static final String OUTGOING_EMAIL_ADDRESS = "utool.update@gmail.com";

	/**
	 * Outgoing email password to use
	 */
	private static final String OUTGOING_EMAIL_PASSWORD = "utooltourn1";
	/**
	 * Holds the list of active subscribers to the tournament
	 */
	private ArrayList<Contact> subscribers;

	/**
	 * Holds the list of non-active subscribers in the tournament
	 */
	private ArrayList<Contact> possible_subscribers;

	/**
	 * Tournament id
	 */
	private long tid;

	/**
	 * Log tag to be used in this class
	 */
	private static final String LOG_TAG = "AutomaticEmailHandler";


	/**
	 * Context of app
	 */
	private Context c;

	/**
	 * Constructor for AutomaticEmailHandler. Links it to a tournamemt
	 * @param tournamentId id of the tournament
	 */
	public AutomaticMessageHandler(long tournamentId)
	{
		//initialize variables
		this.tid = tournamentId;
		subscribers = new ArrayList<Contact>();
		possible_subscribers = new ArrayList<Contact>();
	}

	/**
	 * Updates the subscriber of the tournament state
	 * @param contact of the subscriber to update
	 */
	public void updateSubscriber(final Contact contact)
	{
		//send notification to subscriber of setup
		Log.d(LOG_TAG,"Updateing "+contact.getInfo());

		if(contact.getType()==Contact.EMAIL_ADDRESS)
		{
			new RetreiveFeedTask().execute(contact.getInfo());
		}
		else
		{
			this.sendSMS(contact.getInfo());
		}
	}

	/**
	 * Getter for list of subscribers
	 * @return cloned list of subscribers
	 */
	public ArrayList<Contact> getSubscribers()
	{
		//return clone
		ArrayList<Contact> subscriberst = new ArrayList<Contact>();
		for(int i=0;i<subscribers.size();i++)
		{
			subscriberst.add(subscribers.get(i));
		}
		return subscriberst;
	}

	/**
	 * setter for app context
	 * @param c the app context
	 */
	public void setContext(Context c)
	{
		this.c=c;
	}

	/**
	 * Getter for the list of possible subscribers
	 * @return cloned list of subscribers
	 */
	public ArrayList<Contact> getPossibleSubscribers()
	{
		//return clone
		ArrayList<Contact> possible_subscriberst = new ArrayList<Contact>();
		for(int i=0;i<possible_subscribers.size();i++)
		{
			possible_subscriberst.add(possible_subscribers.get(i));
		}
		return possible_subscriberst;
	}

	/**
	 * Setter for subscribers. If a new subscriber is in the list an initial
	 * email will be sent to them with the current state of the tournament
	 * Note: passed in list is cloned before getting set
	 * @param subs list of subscribers to add
	 */
	public void setSubscribers(List<Contact> subs)
	{
		//go through calling update on new additions
		for(int i=0;i<subs.size();i++)
		{
			boolean isIn = false;
			for(int j=0;j<subscribers.size();j++)
			{
				if(subs.get(i).equals(subscribers.get(j)))
				{
					isIn = true;
					break;
				}
			}
			if(!isIn)
			{
				this.updateSubscriber(subs.get(i));
			}
		}

		//reset to passed in
		subscribers = new ArrayList<Contact>();
		for(int i=0;i<subs.size();i++)
		{
			subscribers.add(subs.get(i));
		}

	}

	/**
	 * Setter for the possible subscribers
	 * @param subs the possible subscribers to add
	 */
	public void setPossibleSubscribers(List<Contact> subs)
	{
		possible_subscribers = new ArrayList<Contact>();
		for(int i=0;i<subs.size();i++)
		{
			possible_subscribers.add(subs.get(i));
		}
	}

	/**
	 * Sends out an email to all subscribers with current tournament state. This
	 * should be called every new round, and every time something matchup related
	 * is changed.
	 */
	public void sendOutNotifications()
	{
		//send notification to each subscriber
		for(int i=0;i<this.subscribers.size();i++)
		{
			//if email contact
			if(subscribers.get(i).getType() == Contact.EMAIL_ADDRESS)
			{
				//send new email to each email subscriber
				//create  duplicate final to call task on
				final String clone = subscribers.get(i).getInfo();

				new RetreiveFeedTask().execute(clone);
			}
			else
			{
				//phone number
				//send out a text to each text subscriber
				this.sendSMS(subscribers.get(i).getInfo());

			}
		}
	}

	/**
	 * ---sends an SMS message to another device---
	 * @param phoneNumber the phone number to send the message to
	 */
	public void sendSMS(String phoneNumber)
	{     
		TournamentLogic t = TournamentLogic.getInstance(tid);
		String message = t.getTournamentDataText();
		try
		{
			TextSender.sendSMS(phoneNumber,message,c);        
		}
		catch(Exception e)
		{
			Log.e("AutomaticTextHandler","Message Failed");
		}
	}


	/**
	// * AsyncTask for actually sending emails.
	// * Sends from Outgoing_Email_Address
	// * @author waltzm
	// * @author bryancl
	// * @version 4/21/2013
	// */
	class RetreiveFeedTask extends AsyncTask<String, Void, String> {

		protected String doInBackground(String... urls) {

			try {   
				GMailSender sender = new GMailSender(OUTGOING_EMAIL_ADDRESS, OUTGOING_EMAIL_PASSWORD);
				TournamentLogic t = TournamentLogic.getInstance(tid);
				String subject = "Tournament Status";
				String body = t.getTournamentDataHTML();
				sender.sendMail(subject, body, OUTGOING_EMAIL_ADDRESS, urls[0]);   
				Log.d("AutomaticEmailHandler", "sent");
			} catch (Exception e) {   
				Log.e("AutomaticEmailHandler", "Error:"+e.getMessage());
			} 
			return null;
		}

		protected void onPostExecute(String feed) {
		}
	}

}



