package myz.Utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import myz.MyZ;
import myz.Support.Configuration;
import myz.Support.Messenger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SQLManager {

	private Connection sql;
	public final String hostname, database, username, password;
	public final int port;
	private boolean connected;
	private Map<String, Map<String, String>> cachedStringValues = new HashMap<String, Map<String, String>>();
	private Map<String, Map<String, Integer>> cachedIntegerValues = new HashMap<String, Map<String, Integer>>();
	private Map<String, Map<String, Boolean>> cachedBooleanValues = new HashMap<String, Map<String, Boolean>>();
	private Map<String, Map<String, Long>> cachedLongValues = new HashMap<String, Map<String, Long>>();
	private List<String> cachedKeyValues = new ArrayList<String>();

	/**
	 * A simple MySQL tool for ease of access.
	 * 
	 * @param hostname
	 *            Host
	 * @param port
	 *            Port
	 * @param database
	 *            Database
	 * @param username
	 *            Username
	 * @param password
	 *            Password
	 */
	public SQLManager(String hostname, int port, String database, String username, String password) {
		this.hostname = hostname;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
		Messenger.sendConsoleMessage(ChatColor.YELLOW + "Connecting to MySQL...");
		connect();
	}

	public void executeQuery(String query) throws SQLException {
		sql.createStatement().executeUpdate(query);
	}

	/**
	 * Establish connection with MySQL.
	 */
	public void connect() {
		String url = "jdbc:mysql://" + hostname + ":" + port + "/" + database;

		// Attempt to connect
		try {
			// Connection succeeded
			sql = DriverManager.getConnection(url, username, password);
			connected = true;
			Messenger.sendConsoleMessage(ChatColor.GREEN + "Connection successful.");
			setup();
		} catch (Exception e) {
			// Couldn't connect to the database
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to connect.");
		}
	}

	/**
	 * Disconnect from MySQL.
	 */
	public void disconnect() {
		if (sql != null && connected) {
			Messenger.sendConsoleMessage(ChatColor.YELLOW + "Disconnected from MySQL.");
			try {
				sql.close();
			} catch (SQLException e) {
				Messenger.sendConsoleMessage(ChatColor.RED + "Unable to close MySQL connection: " + e.getMessage());
			}
		}
	}

	/**
	 * @return true if connected to MySQL.
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Create tables.
	 */
	public void setup() {
		if (!isConnected())
			return;
		try {
			executeQuery("CREATE TABLE IF NOT EXISTS playerdata (username VARCHAR(17) PRIMARY KEY, player_kills SMALLINT UNSIGNED, zombie_kills SMALLINT UNSIGNED, pigman_kills SMALLINT UNSIGNED, giant_kills SMALLINT UNSIGNED, player_kills_life SMALLINT UNSIGNED, zombie_kills_life SMALLINT UNSIGNED, pigman_kills_life SMALLINT UNSIGNED, giant_kills_life SMALLINT UNSIGNED, plays SMALLINT UNSIGNED, deaths SMALLINT UNSIGNED, rank SMALLINT UNSIGNED, isBleeding TINYINT(1), isPoisoned TINYINT(1), wasNPCKilled TINYINT(1), timeOfKickban BIGINT(20), friends VARCHAR(255), heals_life SMALLINT UNSIGNED, thirst SMALLINT UNSIGNED)");
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL setup command: " + e.getMessage());
		}
	}

	/**
	 * Empty the given table.
	 * 
	 * @param table
	 *            The table name
	 * @return non null if deletion was successful
	 */
	public ResultSet emptyTable(String table) {
		return query("TRUNCATE TABLE " + table);
	}

	/**
	 * Add a player to the table if they're not currently in.
	 * 
	 * @param player
	 *            The user to add
	 */
	public void add(Player player) {
		if (!isIn(player.getName()))
			try {
				cachedKeyValues.add(player.getName());
				executeQuery("INSERT INTO playerdata (username, player_kills, zombie_kills, pigman_kills, giant_kills, player_kills_life, zombie_kills_life, pigman_kills_life, giant_kills_life, plays, rank, isBleeding, isPoisoned, wasNPCKilled, timeOfKickban, friends, heals_life, thirst) VALUES ('"
						+ player.getName()
						+ "', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 0,"
						+ Configuration.getMaxThirstLevel()
						+ ")");
			} catch (Exception e) {
				Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL add command: " + e.getMessage());
			}
	}

	/**
	 * Query a MySQL command
	 * 
	 * @param cmd
	 *            The command
	 * @return If the command executed properly
	 */
	private ResultSet query(String cmd) {
		if (!isConnected())
			return null;
		try {
			return sql.createStatement().executeQuery(cmd);
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL query command '" + cmd + "': " + e.getMessage());
			return null;
		}
	}

	/**
	 * Determine whether or not a given username is in the table.
	 * 
	 * @param name
	 *            The username to check
	 * @return true if the user is in the table.
	 */
	public boolean isIn(String name) {
		boolean isin = false;

		// Make sure our player is in the data cache.
		if (!cachedBooleanValues.containsKey(name)) {
			Map<String, Boolean> insert = new HashMap<String, Boolean>();
			insert.put("isin", false);
			cachedBooleanValues.put(name, insert);
		} else {
			// Our player was already in the data cache so let's see if the
			// player has the right key.
			Map<String, Boolean> received = new HashMap<String, Boolean>(cachedBooleanValues.get(name));
			if (!received.containsKey("isin")) {
				received.put("isin", false);
				cachedBooleanValues.put(name, received);
			} else {
				// The player had the key already so let's return it.
				return cachedBooleanValues.get(name).get("isin");
			}
		}

		if (!isConnected())
			return isin;
		try {
			isin = query("SELECT * FROM playerdata WHERE username = '" + name + "' LIMIT 1").next();
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL isin command: " + e.getMessage());
			isin = false;
		}

		// Update the keyset because we don't have the current value.
		Map<String, Boolean> received = new HashMap<String, Boolean>(cachedBooleanValues.get(name));
		received.put("isin", isin);
		cachedBooleanValues.put(name, received);

		return isin;
	}

	/**
	 * Get a list of all primary keys in the table.
	 * 
	 * @return A list of all the primary keys, an empty list if none found or
	 *         null if not connected.
	 */
	public List<String> getKeys() {
		// Return the cached values if we have them.
		if (!cachedKeyValues.isEmpty()) { return cachedKeyValues; }

		if (!isConnected())
			return null;
		List<String> list = new ArrayList<String>();
		try {
			ResultSet rs = query("SELECT * FROM playerdata WHERE username != 'null'");
			if (rs != null)
				while (rs.next())
					if (rs.getString("username") != null)
						list.add(rs.getString("username"));
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL getkeys command: " + e.getMessage());
		}

		// We didn't have any key values stored so let's add all of them.
		cachedKeyValues.addAll(list);
		return list;
	}

	/**
	 * Run a query to set data in MySQL.
	 * 
	 * @param name
	 *            The primary key.
	 * @param field
	 *            The field.
	 * @param value
	 *            The value.
	 * @param aSync
	 *            Whether or not to use an aSync thread to execute the command.
	 */
	public void set(final String name, final String field, final Object value, boolean aSync) {
		if (aSync) {
			MyZ.instance.getServer().getScheduler().runTaskLaterAsynchronously(MyZ.instance, new Runnable() {
				@Override
				public void run() {
					set(name, field, value, false);
				}
			}, 0L);
			return;
		}
		try {
			// Make sure we update our cached values when we set new ones. Do it
			// before we execute the query in case of async demands.
			doUpdateCache(name, field, value);
			executeQuery("UPDATE playerdata SET " + field + " = " + value + " WHERE username = '" + name + "' LIMIT 1");
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL set command for " + name + "." + field + ": "
					+ e.getMessage());
		}
	}

	/**
	 * Update the cache of data to ensure we're up to date when we set new data.
	 * 
	 * @param name
	 *            The primary key.
	 * @param field
	 *            The field.
	 * @param value
	 *            The value.
	 */
	private void doUpdateCache(String name, String field, Object value) {
		if (value instanceof Integer)
			// Make sure our player is in the data cache.
			if (!cachedIntegerValues.containsKey(name)) {
				Map<String, Integer> insert = new HashMap<String, Integer>();
				insert.put(field, (Integer) value);
				cachedIntegerValues.put(name, insert);
			} else {
				Map<String, Integer> received = new HashMap<String, Integer>(cachedIntegerValues.get(name));
				received.put(field, (Integer) value);
				cachedIntegerValues.put(name, received);
			}
		else if (value instanceof Long)
			// Make sure our player is in the data cache.
			if (!cachedLongValues.containsKey(name)) {
				Map<String, Long> insert = new HashMap<String, Long>();
				insert.put(field, (Long) value);
				cachedLongValues.put(name, insert);
			} else {
				Map<String, Long> received = new HashMap<String, Long>(cachedLongValues.get(name));
				received.put(field, (Long) value);
				cachedLongValues.put(name, received);
			}
		else if (value instanceof String)
			// Make sure our player is in the data cache.
			if (!cachedStringValues.containsKey(name)) {
				Map<String, String> insert = new HashMap<String, String>();
				insert.put(field, (String) value);
				cachedStringValues.put(name, insert);
			} else {
				Map<String, String> received = new HashMap<String, String>(cachedStringValues.get(name));
				received.put(field, (String) value);
				cachedStringValues.put(name, received);
			}
		else if (value instanceof Boolean)
			// Make sure our player is in the data cache.
			if (!cachedBooleanValues.containsKey(name)) {
				Map<String, Boolean> insert = new HashMap<String, Boolean>();
				insert.put(field, (Boolean) value);
				cachedBooleanValues.put(name, insert);
			} else {
				Map<String, Boolean> received = new HashMap<String, Boolean>(cachedBooleanValues.get(name));
				received.put(field, (Boolean) value);
				cachedBooleanValues.put(name, received);
			}
	}

	/**
	 * Get a piece of integer data out of the MySQL database.
	 * 
	 * @param name
	 *            The primary key
	 * @param field
	 *            The field
	 * @return The int received or 0 if nothing found
	 */
	public int getInt(String name, String field) {
		int getint = 0;

		// Make sure our player is in the data cache.
		if (!cachedIntegerValues.containsKey(name)) {
			Map<String, Integer> insert = new HashMap<String, Integer>();
			insert.put(field, 0);
			cachedIntegerValues.put(name, insert);
		} else {
			// Our player was already in the data cache so let's see if the
			// player has the right key.
			Map<String, Integer> received = new HashMap<String, Integer>(cachedIntegerValues.get(name));
			if (!received.containsKey(field)) {
				received.put(field, 0);
				cachedIntegerValues.put(name, received);
			} else {
				// The player had the key already so let's return it.
				return cachedIntegerValues.get(name).get(field);
			}
		}

		try {
			ResultSet rs = query("SELECT * FROM playerdata WHERE username = '" + name + "' LIMIT 1");
			if (rs.next())
				getint = rs.getInt(field);
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL getint command for " + name + "." + field + ": "
					+ e.getMessage());
		}

		// Update the keyset because we don't have the current value.
		Map<String, Integer> received = new HashMap<String, Integer>(cachedIntegerValues.get(name));
		received.put(field, getint);
		cachedIntegerValues.put(name, received);

		return getint;
	}

	/**
	 * Get a piece of boolean data out of the MySQL database.
	 * 
	 * @param name
	 *            The primary key
	 * @param field
	 *            The field
	 * @return The boolean received or false if nothing found
	 */
	public boolean getBoolean(String name, String field) {
		boolean getboolean = false;

		// Make sure our player is in the data cache.
		if (!cachedBooleanValues.containsKey(name)) {
			Map<String, Boolean> insert = new HashMap<String, Boolean>();
			insert.put(field, false);
			cachedBooleanValues.put(name, insert);
		} else {
			// Our player was already in the data cache so let's see if the
			// player has the right key.
			Map<String, Boolean> received = new HashMap<String, Boolean>(cachedBooleanValues.get(name));
			if (!received.containsKey(field)) {
				received.put(field, false);
				cachedBooleanValues.put(name, received);
			} else {
				// The player had the key already so let's return it.
				return cachedBooleanValues.get(name).get(field);
			}
		}

		try {
			ResultSet rs = query("SELECT * FROM playerdata WHERE username = '" + name + "' LIMIT 1");
			if (rs.next())
				getboolean = rs.getBoolean(field);
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL getboolean command for " + name + "." + field + ": "
					+ e.getMessage());
		}

		// Update the keyset because we don't have the current value.
		Map<String, Boolean> received = new HashMap<String, Boolean>(cachedBooleanValues.get(name));
		received.put(field, getboolean);
		cachedBooleanValues.put(name, received);

		return getboolean;
	}

	/**
	 * Get a piece of long data out of the MySQL database.
	 * 
	 * @param name
	 *            The primary key
	 * @param field
	 *            The field
	 * @return The long received or 0 if nothing found
	 */
	public long getLong(String name, String field) {
		long getlong = 0L;

		// Make sure our player is in the data cache.
		if (!cachedLongValues.containsKey(name)) {
			Map<String, Long> insert = new HashMap<String, Long>();
			insert.put(field, 0L);
			cachedLongValues.put(name, insert);
		} else {
			// Our player was already in the data cache so let's see if the
			// player has the right key.
			Map<String, Long> received = new HashMap<String, Long>(cachedLongValues.get(name));
			if (!received.containsKey(field)) {
				received.put(field, 0L);
				cachedLongValues.put(name, received);
			} else {
				// The player had the key already so let's return it.
				return cachedLongValues.get(name).get(field);
			}
		}

		try {
			ResultSet rs = query("SELECT * FROM playerdata WHERE username = '" + name + "' LIMIT 1");
			if (rs.next())
				getlong = rs.getLong(field);
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL getlong command for " + name + "." + field + ": "
					+ e.getMessage());
		}

		// Update the keyset because we don't have the current value.
		Map<String, Long> received = new HashMap<String, Long>(cachedLongValues.get(name));
		received.put(field, getlong);
		cachedLongValues.put(name, received);

		return getlong;
	}

	/**
	 * Get a piece of string data out of the MySQL database.
	 * 
	 * @param name
	 *            The primary key
	 * @param field
	 *            The field
	 * @return The string received or an empty string if nothing found
	 */
	public String getString(String name, String field) {
		String getstring = "";

		// Make sure our player is in the data cache.
		if (!cachedStringValues.containsKey(name)) {
			Map<String, String> insert = new HashMap<String, String>();
			insert.put(field, "");
			cachedStringValues.put(name, insert);
		} else {
			// Our player was already in the data cache so let's see if the
			// player has the right key.
			Map<String, String> received = new HashMap<String, String>(cachedStringValues.get(name));
			if (!received.containsKey(field)) {
				received.put(field, "");
				cachedStringValues.put(name, received);
			} else {
				// The player had the key already so let's return it.
				return cachedStringValues.get(name).get(field);
			}
		}

		try {
			ResultSet rs = query("SELECT * FROM playerdata WHERE username = '" + name + "' LIMIT 1");
			if (rs.next())
				getstring = rs.getString(field);
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL getstring commandfor " + name + "." + field + ": "
					+ e.getMessage());
		}

		// Update the keyset because we don't have the current value.
		Map<String, String> received = new HashMap<String, String>(cachedStringValues.get(name));
		received.put(field, getstring);
		cachedStringValues.put(name, received);

		return getstring;
	}

	/**
	 * Get a piece of stringlist data out of the MySQL database. Every element
	 * is separated by a comma (,)
	 * 
	 * @param name
	 *            The primary key
	 * @param field
	 *            The field
	 * @return The stringlist received or an empty stringlist if nothing found
	 */
	public List<String> getStringList(String name, String field) {
		List<String> returnList = new ArrayList<String>();
		String raw = "";

		// Make sure our player is in the data cache.
		if (!cachedStringValues.containsKey(name)) {
			Map<String, String> insert = new HashMap<String, String>();
			insert.put(field, "");
			cachedStringValues.put(name, insert);
		} else {
			// Our player was already in the data cache so let's see if the
			// player has the right key.
			Map<String, String> received = new HashMap<String, String>(cachedStringValues.get(name));
			if (!received.containsKey(field)) {
				received.put(field, "");
				cachedStringValues.put(name, received);
			} else {
				// The player had the key already so let's return it.
				for (String player : cachedStringValues.get(name).get(field).split(","))
					returnList.add(player);
				return returnList;
			}
		}

		try {
			ResultSet rs = query("SELECT * FROM playerdata WHERE username = '" + name + "' LIMIT 1");
			if (rs.next()) {
				raw = rs.getString(field);
				for (String player : raw.split(","))
					returnList.add(player);
			}
		} catch (Exception e) {
			Messenger.sendConsoleMessage(ChatColor.RED + "Unable to execute MySQL getstringlist command for" + name + "." + field + ": "
					+ e.getMessage());
		}

		// Update the keyset because we don't have the current value.
		Map<String, String> received = new HashMap<String, String>(cachedStringValues.get(name));
		received.put(field, raw);
		cachedStringValues.put(name, received);

		return returnList;
	}
}
