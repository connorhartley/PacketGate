package eu.crushedpixel.sponge.packetgate.api.registry;

import com.google.common.base.Preconditions;
import eu.crushedpixel.sponge.packetgate.api.listener.PacketListener;
import eu.crushedpixel.sponge.packetgate.api.listener.PacketListener.ListenerPriority;
import eu.crushedpixel.sponge.packetgate.api.listener.PacketListener.PacketListenerData;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.Packet;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PacketGate extends ListenerOwner {

    private Set<PacketConnection> connections = new HashSet<>();

    public void registerConnection(PacketConnection connection) {
        // register all global packet listeners to the connection
        this.packetListeners.forEach((clazz, list) -> {
            list.forEach(data -> connection.register(data, clazz));
        });

        connections.add(connection);
    }

    public void unregisterConnection(PacketConnection connection) {
        connections.remove(connection);
    }

    public Optional<PacketConnection> connectionByPlayer(Player player) {
        return connections
                .stream()
                .filter(connection -> player.getUniqueId().equals(connection.getPlayerUUID()))
                .findFirst();
    }

    public void registerListener(PacketListener packetListener, ListenerPriority priority,
                                 Class... packetClasses) {
        registerListener(packetListener, priority, null, packetClasses);
    }

    public void registerListener(PacketListener packetListener, ListenerPriority priority,
                                 PacketConnection connection,
                                 Class... packetClasses) {
        List<Class> classes = new ArrayList<>();

        // if no classes are specified, apply the listener to all Minecraft packet classes
        if (packetClasses.length == 0) {
            for (EnumConnectionState state : EnumConnectionState.values()) {
                state.directionMaps.forEach((enumPacketDirection, integerClassBiMap) -> {
                    integerClassBiMap.forEach((id, clazz) -> {
                        classes.add(clazz);
                    });
                });
            }
        } else {
            // check if packet classes are valid
            for (Class clazz : packetClasses) {
                Preconditions.checkArgument(Packet.class.isAssignableFrom(clazz),
                        "Packet classes have to be subclasses of net.minecraft.network.Packet");

                classes.add(clazz);
            }
        }

        PacketListenerData packetListenerData = new PacketListenerData(packetListener, priority);

        Class[] array = classes.toArray(new Class[classes.size()]);

        if (connection != null) {
            connection.register(packetListenerData, array);
        } else {
            this.register(packetListenerData, array);
        }
    }

    public void unregisterListener(PacketListener packetListener) {
        unregister(packetListener);

        for (PacketConnection connection : connections) {
            connection.unregister(packetListener);
        }
    }

    @Override
    void register(PacketListenerData packetListenerData, Class... classes) {
        super.register(packetListenerData, classes);
        connections.forEach(packetConnection -> packetConnection.register(packetListenerData, classes));
    }

}
