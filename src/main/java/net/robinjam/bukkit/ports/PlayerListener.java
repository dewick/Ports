package net.robinjam.bukkit.ports;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.robinjam.bukkit.ports.persistence.Port;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 
 * @author robinjam
 */
public class PlayerListener implements Listener {

	private Ports plugin;

	private Map<Player, Port> playerLocations = new HashMap<Player, Port>();

	public PlayerListener(Ports plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();

		if (player.isInsideVehicle())
			return;

		List<Port> ports = plugin.getDatabase().find(Port.class).where()
				.ieq("world", player.getWorld().getName()).findList();

		for (Port port : ports) {
			if (!port.contains(event.getFrom()) && port.contains(event.getTo())) {
				if (playerLocations.get(player) != port) {
					playerLocations.put(player, port);
					onPlayerEnterPort(player, port);
				}
			} else if (port.contains(event.getFrom())
					&& !port.contains(event.getTo())
					&& playerLocations.containsKey(player)) {
				playerLocations.remove(player);
				onPlayerLeavePort(player, port);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();
		plugin.getTicketManager().removeTicket(player);
	}

	private void onPlayerEnterPort(Player player, Port port) {
		if (port.getDestination() == null) {
			player.sendMessage(ChatColor.YELLOW + "This "
					+ port.getDescription() + " has no destination!");
		} else {
			if (port.getDepartureSchedule() > 0) {
				plugin.getTicketManager().addTicket(player, port);
				player.sendMessage(ChatColor.AQUA
                        + "Vitej! Tady mas jizdenku a pockej, nez "
                        + ChatColor.GREEN
						+ port.getDescription()
                        + ChatColor.AQUA + " odleti.");
			} else {
				plugin.teleportPlayer(player, port);
			}
		}
	}

	private void onPlayerLeavePort(Player player, Port port) {
		plugin.getTicketManager().removeTicket(player);
		player.sendMessage(ChatColor.AQUA + "Please come again soon!");
	}

}
