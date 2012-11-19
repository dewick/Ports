package net.robinjam.bukkit.ports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.robinjam.bukkit.ports.persistence.Port;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * 
 * @author robinjam
 */
public class TicketManager implements Runnable {

	Ports plugin;
	Map<Player, Port> tickets = new HashMap<Player, Port>();
	Map<World, Long> previousTimes = new HashMap<World, Long>();
	Set<Player> playersToRemove = new HashSet<Player>();

	public TicketManager(Ports plugin) {
		this.plugin = plugin;
	}

	public void addTicket(Player player, Port port) {
		tickets.put(player, port);
		playersToRemove.remove(player);
	}

	public void removeTicket(Player player) {
		playersToRemove.add(player);
	}

	public void run() {
		for (Player player : playersToRemove) {
			tickets.remove(player);
		}

		playersToRemove.clear();

		for (Port port : tickets.values()) {
			plugin.getDatabase().refresh(port);
		}

		Map<World, Long> newTimes = new HashMap<World, Long>();
		for (World world : Bukkit.getWorlds()) {
			newTimes.put(world, world.getFullTime());
			if (!previousTimes.containsKey(world))
				previousTimes.put(world, world.getFullTime());
		}

		Set<Port> ports = new HashSet<Port>(tickets.values());
		for (Port port : ports) {
			World world = Bukkit.getWorld(port.getWorld());

			boolean readyToDepart = false;
			for (long x = previousTimes.get(world); (x <= newTimes.get(world) + 16); x++) {
				if (x % port.getDepartureSchedule() == 0) {
					readyToDepart = true;
				}
			}

			for (Map.Entry<Player, Port> ticket : tickets.entrySet()) {
				if (port.equals(ticket.getValue())) {
					// If the port is ready to depart
					if (readyToDepart) {
						Player player = ticket.getKey();
						plugin.teleportPlayer(player, port);
					} else {
						Player player = ticket.getKey();
						long t = (port.getDepartureSchedule() - newTimes
								.get(world) % port.getDepartureSchedule());
						long d = t / 24000;
						long h = (t % 24000) / 1000;
						long m = ((t % 24000) % 1000) / 16;
						String nextDeparture = String.format("[%dd %dh %dm]",
								d, h, m);
						player.sendMessage(ChatColor.GREEN + ""
                                + port.getDescription()
                                + ChatColor.AQUA + " odleta za "
								+ nextDeparture);
					}
				}
			}
		}

		previousTimes = newTimes;
	}

}
