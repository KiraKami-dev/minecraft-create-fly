package dev.simulated_team.simulated.content.blocks.handle;

import dev.ryanhcode.sable.veillite.api.network.VeilPacketManager;
import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.network.packets.handle.ClientboundPlayersHoldingHandlePacket;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class ServerHandleHoldingHandler {
	public static Object2IntMap<UUID> holdingPlayers = new Object2IntOpenHashMap<>();

	public static int ticks;

	public static void startHolding(final Player player) {
		final int count = holdingPlayers.size();
		if (holdingPlayers.put(player.getUUID(), 20) <= 0) {
			SimStats.INTERACT_WITH_HANDLE.awardTo(player);
		}

		if (holdingPlayers.size() != count)
			sync(player.level().getServer());
	}

	public static void stopHolding(final Player player) {
		if (holdingPlayers.removeInt(player.getUUID()) != 0)
			sync(player.level().getServer());
	}

	// (not called upstream either)
	public static void tick(final MinecraftServer server) {
		ticks++;

		final int before = holdingPlayers.size();

		final ObjectIterator<Object2IntMap.Entry<UUID>> iterator = holdingPlayers.object2IntEntrySet().iterator();
		while (iterator.hasNext()) {
			final Map.Entry<UUID, Integer> entry = iterator.next();
			final int newTTL = entry.getValue() - 1;
			if (newTTL <= 0) {
				iterator.remove();
			} else {
				entry.setValue(newTTL);
			}
		}

		final int after = holdingPlayers.size();

		if (ticks % 10 != 0 && before == after)
			return;

		sync(server);

	}

	/**
	 * Sends the players holding handles to all clients (was CatnipServices.NETWORK.sendToAllClients)
	 */
	public static void sync(@Nullable final MinecraftServer server) {
		if (server == null)
			return;
		VeilPacketManager.all(server).sendPacket(new ClientboundPlayersHoldingHandlePacket(new HashSet<>(holdingPlayers.keySet())));
	}
}
