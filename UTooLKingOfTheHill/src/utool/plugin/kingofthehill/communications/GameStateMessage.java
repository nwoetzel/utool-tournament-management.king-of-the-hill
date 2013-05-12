package utool.plugin.kingofthehill.communications;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import utool.networking.XmlMessageTypeException;
import utool.networking.packet.IXmlMessage;
import utool.plugin.Player;
import utool.plugin.kingofthehill.tournament.KingOfTheHillPlayerExtra;
import android.util.SparseArray;
import android.util.Xml;

/**
 * This class handles encoding and decoding messages used for tournament synchronization
 * @author Cory
 *
 */
public class GameStateMessage implements IXmlMessage {
	
	/**
	 * XML root tag
	 */
	private static final String ROOT_TAG = "utool_kingofthehill";
	
	/**
	 * Message type attribute
	 */
	private static final String MESSAGE_TYPE_ATTRIB = "type";
	
	/**
	 * The round timer setting attribute tag
	 */
	private static final String ROUND_TIMER_REMAINING_ATTRIB = "roundTimerRemaining";
	
	/**
	 * The game timer setting attribute tag
	 */
	private static final String GAME_TIMER_REMAINING_SECONDS_ATTRIB = "gameTimerRemaining";
	
	/**
	 * XML current king tag
	 */
	private static final String KING_TAG = "king";
	
	/**
	 * XML current king wins tag
	 */
	private static final String KING_WINS_ATTRIB = "kingWins";
	
	/**
	 * XML player tag
	 */
	private static final String PLAYER_TAG = "player";
	
	/**
	 * XML player UUID attribute name
	 */
	private static final String PLAYER_UUID_ATTRIB = "player_uuid";
	
	/**
	 * XML player position attribute name
	 * The position in the queue the player is in
	 */
	private static final String PLAYER_POSITION_ATTRIB = "player_position";
	
	/**
	 * XML player wins attribute name
	 */
	private static final String PLAYER_WINS_ATTRIB = "player_wins";
	
	/**
	 * XML player losses attribute name
	 */
	private static final String PLAYER_LOSSES_ATTRIB = "player_losses";
	
	/**
	 * The players for this message
	 */
	private List<UUID> players = new LinkedList<UUID>();
	
	/**
	 * HashMap of player extra data
	 */
	private HashMap<UUID, KingOfTheHillPlayerExtra> playersExtra = new HashMap<UUID, KingOfTheHillPlayerExtra>(); 
	
	/**
	 * The current king of the tournament
	 */
	private UUID king;
	
	/**
	 * The current king's wins this round
	 */
	private int kingWins;
	
	/**
	 * The message type
	 */
	private MessageType messageType;
	
	/**
	 * The number of seconds remaining in the game
	 */
	private int gameTimerRemaining;
	
	/**
	 * The number of seconds remaining in the round
	 */
	private int roundTimerRemaining;
	
	/**
	 * Read a received PlayerRegisterMessage
	 * @param message The XML string of the message
	 * @throws XmlMessageTypeException On invalid message type
	 */
	public GameStateMessage(String message) throws XmlMessageTypeException{
		players = new LinkedList<UUID>();
		decodeMessage(message);
	}
	
	/**
	 * Get the king in this message
	 * @return Player object
	 */
	public UUID getKing(){
		return king;
	}
	
	/**
	 * Get all players in the message
	 * @return The list of players in the message
	 */
	public List<UUID> getPlayerList(){
		return players;
	}
	
	/**
	 * Get this message's type
	 * @return MessageType of the message
	 */
	public MessageType getMessageType(){
		return messageType;
	}
	
	/**
	 * Get the time remaining in the game
	 * @return Time in seconds
	 */
	public int getGameTimeRemaining(){
		return gameTimerRemaining;
	}
	
	/**
	 * Get the time remaining in the round
	 * @return Time in seconds
	 */
	public int getRoundTimeRemaining(){
		return roundTimerRemaining;
	}
	
	/**
	 * Get the king's wins this round
	 * @return King's wins
	 */
	public int getKingWins(){
		return kingWins;
	}
	
	/**
	 * Set the king's wins this round
	 * @param wins King's wins
	 */
	public void setKingWins(int wins){
		kingWins = wins;
	}
	
	/**
	 * Get the extra player data
	 * @return HashMap of data
	 */
	public HashMap<UUID, KingOfTheHillPlayerExtra> getPlayerExtras(){
		return playersExtra;
	}
	
	/**
	 * Constructor for the game state message
	 * @param king The current king
	 * @param players The current player queue
	 * @param playersExtra The extra data associated with all players
	 * @param gameTimerRemaining The number of seconds left in the game
	 * @param roundTimerRemaining The number of seconds left in the round
	 */
	public GameStateMessage(Player king, List<Player> players, HashMap<Player, KingOfTheHillPlayerExtra> playersExtra, int gameTimerRemaining, int roundTimerRemaining){
		this.messageType = MessageType.GameState;
		this.gameTimerRemaining = gameTimerRemaining;
		this.roundTimerRemaining = roundTimerRemaining;
		List<UUID> playersUUID = new LinkedList<UUID>();
		for (Player p : players){
			playersUUID.add(p.getUUID());
			KingOfTheHillPlayerExtra extra = playersExtra.get(p);
			if (extra != null){
				this.playersExtra.put(p.getUUID(), extra);
			}
		}
		
		this.players = playersUUID;
		if (king != null){
			this.king = king.getUUID();
		}
		KingOfTheHillPlayerExtra extra = playersExtra.get(king);
		if (extra != null){
			this.playersExtra.put(this.king, extra);
		}
	}
	
	/**
	 * Constructor for game state request message
	 */
	public GameStateMessage(){
		this.messageType = MessageType.RequestGameState;
	}
	
	@Override
	public String getXml(){
		String xml = "";
		try{
			XmlSerializer xmlSerializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();

			//Use writer as the output
			xmlSerializer.setOutput(writer);

			//Start the document
			xmlSerializer.startDocument("UTF-8", true);
			xmlSerializer.startTag("", ROOT_TAG);
			xmlSerializer.attribute("", MESSAGE_TYPE_ATTRIB, messageType.name());
			xmlSerializer.attribute("", GAME_TIMER_REMAINING_SECONDS_ATTRIB, Integer.toString(gameTimerRemaining));
			xmlSerializer.attribute("", ROUND_TIMER_REMAINING_ATTRIB, Integer.toString(roundTimerRemaining));

			//Write the king
			if (king != null){
				xmlSerializer.startTag("", KING_TAG);
				xmlSerializer.attribute("", PLAYER_UUID_ATTRIB, king.toString());
				xmlSerializer.attribute("", KING_WINS_ATTRIB, Integer.toString(kingWins));
				KingOfTheHillPlayerExtra extra = playersExtra.get(king);
				if (extra != null){
					xmlSerializer.attribute("", PLAYER_WINS_ATTRIB, Integer.toString(playersExtra.get(king).getWins()));
					xmlSerializer.attribute("", PLAYER_LOSSES_ATTRIB, Integer.toString(playersExtra.get(king).getLosses()));
				}
				xmlSerializer.endTag("", KING_TAG);
			}


			//Write the player(s)
			if (players != null){
				int position = 0;
				for (UUID player: players){
					xmlSerializer.startTag("", PLAYER_TAG);
					xmlSerializer.attribute("", PLAYER_UUID_ATTRIB, player.toString());
					xmlSerializer.attribute("", PLAYER_POSITION_ATTRIB, Integer.toString(position));
					KingOfTheHillPlayerExtra extra = playersExtra.get(player);
					if (extra != null){
						xmlSerializer.attribute("", PLAYER_WINS_ATTRIB, Integer.toString(playersExtra.get(player).getWins()));
						xmlSerializer.attribute("", PLAYER_LOSSES_ATTRIB, Integer.toString(playersExtra.get(player).getLosses()));
					}
					xmlSerializer.endTag("", PLAYER_TAG);
					position += 1;
				}
			}

			//End the document
			xmlSerializer.endTag("", ROOT_TAG);
			xmlSerializer.endDocument();
			xml = writer.toString();
		} catch (Exception e) {	}
		return xml;
	}
	
	/**
	 * Decode an XML message to this object
	 * @param xml string to parse
	 * @throws XmlMessageTypeException On invalid message type
	 */
	private void decodeMessage(String xml) throws XmlMessageTypeException {
		SparseArray<UUID> players = new SparseArray<UUID>();
		
		StringReader input = new java.io.StringReader(xml);
		try{
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(input);
			parser.nextTag();

			if (!parser.getName().equals(ROOT_TAG)){
				throw new XmlMessageTypeException("Not a game state message: " + parser.getName());
			}
			String messageType = parser.getAttributeValue("", MESSAGE_TYPE_ATTRIB);
			this.messageType = MessageType.valueOf(messageType);
			this.gameTimerRemaining = Integer.parseInt(parser.getAttributeValue("", GAME_TIMER_REMAINING_SECONDS_ATTRIB));
			this.roundTimerRemaining = Integer.parseInt(parser.getAttributeValue("", ROUND_TIMER_REMAINING_ATTRIB));

			int next = parser.next();
			UUID playerUUID = null;
			int position = 0;
			int wins = -1;
			int losses = -1;

			while (next != XmlPullParser.END_DOCUMENT) {
				String name = parser.getName();
				if (parser.getEventType() == XmlPullParser.START_TAG){
					if (name != null && name.equalsIgnoreCase(PLAYER_TAG)){
						//read a player
						for (int i = 0; i < parser.getAttributeCount(); i++){
							String attrib = parser.getAttributeName(i);
							if (attrib.equalsIgnoreCase(PLAYER_UUID_ATTRIB)){
								playerUUID = UUID.fromString(parser.getAttributeValue(i));
							} else if (attrib.equalsIgnoreCase(PLAYER_POSITION_ATTRIB)){
								position = Integer.valueOf(parser.getAttributeValue(i));
							} else if (attrib.equalsIgnoreCase(PLAYER_WINS_ATTRIB)){
								wins = Integer.valueOf(parser.getAttributeValue(i));
							} else if (attrib.equalsIgnoreCase(PLAYER_LOSSES_ATTRIB)){
								losses = Integer.valueOf(parser.getAttributeValue(i));
							}
						}
					} else if (name != null && name.equalsIgnoreCase(KING_TAG)){
						//read the king
						for (int i = 0; i < parser.getAttributeCount(); i++){
							String attrib = parser.getAttributeName(i);
							if (attrib.equalsIgnoreCase(PLAYER_UUID_ATTRIB)){
								king = UUID.fromString(parser.getAttributeValue(i));
							} else if (attrib.equalsIgnoreCase(PLAYER_WINS_ATTRIB)){
								wins = Integer.valueOf(parser.getAttributeValue(i));
							} else if (attrib.equalsIgnoreCase(PLAYER_LOSSES_ATTRIB)){
								losses = Integer.valueOf(parser.getAttributeValue(i));
							} else if (attrib.equalsIgnoreCase(KING_WINS_ATTRIB)){
								kingWins = Integer.valueOf(parser.getAttributeValue(i));
							}
						}
					}
				} else if (parser.getEventType() == XmlPullParser.END_TAG){
					if (name.equalsIgnoreCase(PLAYER_TAG)){
						players.put(position, playerUUID);
						if (wins != -1 && losses != -1){
							playersExtra.put(playerUUID, new KingOfTheHillPlayerExtra(playerUUID, wins, losses));
						}
					}
					else if (name.equalsIgnoreCase(KING_TAG)){
						if (wins != -1 && losses != -1){
							playersExtra.put(king, new KingOfTheHillPlayerExtra(king, wins, losses));
						}
					}
				}
				next = parser.next();
			}
			
			//add players to list in order
			for (int i = 0; i < players.size(); i++){
				UUID p = players.get(i);
				this.players.add(p);
			}

		} catch (XmlPullParserException e) {
			throw new XmlMessageTypeException("Not a player message");
		} catch (IOException e) {
		} finally {
			try {
				input.close();
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * Message type enum
	 *
	 */
	public enum MessageType{
		/**
		 * The message contains new game state
		 */
		GameState,
		/**
		 * The message is request for the current game state
		 */
		RequestGameState
	}

	@Override
	public boolean isOfMessageType(String xml) {
		StringReader input = new java.io.StringReader(xml);

		XmlPullParser parser = Xml.newPullParser();
		try {
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

			parser.setInput(input);
			parser.nextTag();

			if (parser.getName().equals(ROOT_TAG)){
				return true;
			}
		} catch (XmlPullParserException e) {
		} catch (IOException e) {
		}
		return false;
	}
}
