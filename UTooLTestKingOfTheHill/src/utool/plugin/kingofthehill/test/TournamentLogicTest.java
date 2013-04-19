package utool.plugin.kingofthehill.test;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import utool.plugin.Player;
import utool.plugin.kingofthehill.communications.GameStateMessage;
import utool.plugin.kingofthehill.tournament.KingOfTheHillTournament;
import utool.plugin.kingofthehill.tournament.TournamentLogic;
import android.test.AndroidTestCase;

/**
 * Tests for KOTH
 * @author Cory
 *
 */
public class TournamentLogicTest extends AndroidTestCase {
	
	/**
	 * Tournament instance
	 */
	KingOfTheHillTournament tournament;
	
	/**
	 * Base list of players
	 */
	List<Player> players;
	
	@Override
	protected void setUp() throws Exception{
		super.setUp();
		players = new LinkedList<Player>();
		players.add(new Player(UUID.randomUUID(), "Player 1"));
		players.add(new Player(UUID.randomUUID(), "Player 2"));
		players.add(new Player(UUID.randomUUID(), "Player 3"));
		
		tournament = (KingOfTheHillTournament) TournamentLogic.getNewInstance(this.getContext(), 0, players, Player.HOST);
		
	}
	
	@Override
	protected void tearDown() throws Exception{
		super.tearDown();
		TournamentLogic.removeInstance(tournament.getTournamentId());
	}
	
	/**
	 * Test king
	 */
	public void testKing(){
		assertEquals(players.get(0), tournament.getKing());
	}
	
	/**
	 * Test challenger
	 */
	public void testChallenger(){
		assertEquals(players.get(1), tournament.popTopPlayer());
	}
	
	/**
	 * Test an entire game
	 * @throws Exception When something bad happens
	 */
	public void testTournament() throws Exception{
		//Player 1 (king) wins, Player 2 goes to bottom
		tournament.moveChallengerToEnd();
		assertEquals(players.get(0), tournament.getKing());
		assertEquals(players.get(1), tournament.getPlayers().get(tournament.getPlayers().size()-1));
		
		//Player 3 (challenger) wins, Player 1 goes to bottom
		tournament.moveKingToEnd();
		assertEquals(players.get(2), tournament.getKing());
		assertEquals(players.get(0), tournament.getPlayers().get(tournament.getPlayers().size()-1));
		
		//check player 1's score
		assertEquals(1, tournament.getPlayerExtra(players.get(0)).getWins());
		assertEquals(1, tournament.getPlayerExtra(players.get(0)).getLosses());
		
		//check player 2's score
		assertEquals(0, tournament.getPlayerExtra(players.get(1)).getWins());
		assertEquals(1, tournament.getPlayerExtra(players.get(1)).getLosses());
		
		//check player 3's score
		assertEquals(1, tournament.getPlayerExtra(players.get(2)).getWins());
		assertEquals(0, tournament.getPlayerExtra(players.get(2)).getLosses());
	}
	
	/**
	 * Test moving players
	 * @throws Exception When something bad happens
	 */
	public void testMovePlayers() throws Exception{
		//swap king (Player 1) and challenger (Player 2)
		tournament.movePlayer(TournamentLogic.KING_POSITION, 0);
		assertEquals(players.get(0), tournament.getPlayers().get(0));
		assertEquals(players.get(1), tournament.getKing());
		
		//swap challenger (Player 1) and king (Player 2)
		tournament.movePlayer(0, TournamentLogic.KING_POSITION);
		assertEquals(players.get(1), tournament.getPlayers().get(0));
		assertEquals(players.get(0), tournament.getKing());
		
		//swap Player 2 and Player 3
		tournament.movePlayer(0, 1);
		assertEquals(players.get(1), tournament.getPlayers().get(1));
		assertEquals(players.get(2), tournament.getPlayers().get(0));
		
		//swap back
		tournament.movePlayer(1, 0);
		assertEquals(players.get(1), tournament.getPlayers().get(0));
		assertEquals(players.get(2), tournament.getPlayers().get(1));
	}
	
	/**
	 * Test adding players
	 * @throws Exception When something bad happens
	 */
	public void testAddPlayers() throws Exception{
		List<Player> p = new LinkedList<Player>();
		p.add(new Player(UUID.randomUUID(), "Player 4"));
		p.add(new Player(UUID.randomUUID(), "Player 5"));
		tournament.addNewPlayersToBottom(p);
		assertEquals(p.get(0), tournament.getPlayers().get(tournament.getPlayers().size() - 2));
		assertEquals(p.get(1), tournament.getPlayers().get(tournament.getPlayers().size() - 1));
	}
	
	/**
	 * Test network messages
	 * @throws Exception When something bad happens
	 */
	public void testGameStateMessage() throws Exception{
		GameStateMessage m1 = new GameStateMessage(tournament.getKing(), tournament.getPlayers(), tournament.getPlayerExtras(), tournament.getRemainingGameTime(), tournament.getRemainingRoundTime());
		GameStateMessage m2 = new GameStateMessage(m1.getXml());
		assertEquals(m1.getKing(), m2.getKing());
		assertEquals(m1.getPlayerList(), m2.getPlayerList());
		assertEquals(m1.getGameTimeRemaining(), m2.getGameTimeRemaining());
		assertEquals(m1.getRoundTimeRemaining(), m2.getRoundTimeRemaining());
		assertEquals(m1.getPlayerExtras(), m2.getPlayerExtras());
	}
}
