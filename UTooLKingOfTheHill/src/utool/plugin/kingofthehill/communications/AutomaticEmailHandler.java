package utool.plugin.kingofthehill.communications;

import java.util.ArrayList;

import utool.plugin.email.GMailSender;
import utool.plugin.kingofthehill.tournament.TournamentLogic;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Manager for the automatic Email Handling
 * Sends emails from default email of msoetablet@gmail.com
 * @author waltzm
 * @author bryancl
 * @version 2/5/2013
 */
public class AutomaticEmailHandler 
{
	/**
	 * Holds the list of active subscribers to the tournament
	 */
	private ArrayList<String> subscribers;

	/**
	 * Holds the list of non-active subscribers in the tournament
	 */
	private ArrayList<String> possible_subscribers;

	/**
	 * Tournament id
	 */
	private long tid;
	
	/**
	 * Log tag to be used in this class
	 */
	private static String logtag = "AutomaticEmailHandler";

	/**
	 * Constructor for AutomaticEmailHandler. Links it to a tournament
	 * @param tournamentId id of the tournament
	 */
	public AutomaticEmailHandler(long tournamentId)
	{
		//initialize variables
		this.tid = tournamentId;
		subscribers = new ArrayList<String>();
		possible_subscribers = new ArrayList<String>();
	}

	/**
	 * Updates the subscriber of the tournament state
	 * @param address of the subscriber to update
	 */
	public void updateSubscriber(String address)
	{
		//send notification to subscriber of setup
		Log.d(logtag,"Updating "+address);
		new RetreiveFeedTask().execute(address);
	}

	/**
	 * Getter for list of subscribers
	 * @return cloned list of subscribers
	 */
	public ArrayList<String> getSubscribers()
	{
		//return clone
		ArrayList<String> subscriberst = new ArrayList<String>();
		for(int i=0;i<subscribers.size();i++)
		{
			subscriberst.add(subscribers.get(i));
		}
		return subscriberst;
	}

	/**
	 * Getter for the list of possible subscribers
	 * @return cloned list of subscribers
	 */
	public ArrayList<String> getPossibleSubscribers()
	{
		//return clone
		ArrayList<String> possible_subscriberst = new ArrayList<String>();
		for(int i=0;i<possible_subscribers.size();i++)
		{
			possible_subscriberst.add(possible_subscribers.get(i));
		}
		return possible_subscriberst;
	}

	/**
	 * Setter for subscribers. If a new subscriber is in the list an initial
	 * email will be sent to them with the current state of the tournament
	 * @param subs list of subscribers to add
	 */
	public void setSubscribers(ArrayList<String> subs)
	{
		Log.d(logtag,"subs: "+subs.toString());
		Log.d(logtag,"subscr: "+subscribers.toString());

		//go through calling update on new additions
		for(int i=0;i<subs.size();i++)
		{
			boolean isIn = false;
			for(int j=0;j<subscribers.size();j++)
			{
				if(subs.get(i).equals(subscribers.get(j)))
				{
					isIn = true;
				}
			}
			if(!isIn)
			{
				this.updateSubscriber(subs.get(i));
			}
		}

		//reset to passed in
		subscribers = new ArrayList<String>();
		for(int i=0;i<subs.size();i++)
		{
			subscribers.add(subs.get(i));
		}

	}
	
	/**
	 * Setter for the possible subscribers
	 * @param subs the possible subscribers to add
	 */
	public void setPossibleSubscribers(ArrayList<String> subs)
	{
		possible_subscribers = new ArrayList<String>();
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
		//send new email to each subscriber
		for(int i=0;i<this.subscribers.size();i++)
		{
			new RetreiveFeedTask().execute(subscribers.get(i));
		}
	}
	
	/**
	 * AsyncTask for actually sending emails.
	 * Sends from msoetablet@gmail.com
	 * @author waltzm
	 * @author bryancl
	 * @version 1/14/2013
	 */
	class RetreiveFeedTask extends AsyncTask<String, Void, String> {

		protected String doInBackground(String... urls) {

			try {   
				GMailSender sender = new GMailSender("msoetablet@gmail.com", "msoetablet");
				TournamentLogic t = TournamentLogic.getInstance(tid);
				String subject = "Tournament Status";
				String body = t.getTournamentData();

				sender.sendMail(subject, body, "msoetablet@gmail.com", urls[0]);   
				Log.d(logtag, "sent");
			} catch (Exception e) {   
				Log.e(logtag, "Error:"+e.getMessage());
			} 
			return null;
		}

		protected void onPostExecute(String feed) {
		}
	}

}
