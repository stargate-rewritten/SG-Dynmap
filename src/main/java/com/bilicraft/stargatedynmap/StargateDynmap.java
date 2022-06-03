package com.bilicraft.stargatedynmap;

import net.TheDgtl.Stargate.api.StargateAPI;
import net.TheDgtl.Stargate.event.StargateCreateEvent;
import net.TheDgtl.Stargate.event.StargateDestroyEvent;
import net.TheDgtl.Stargate.network.Network;
import net.TheDgtl.Stargate.network.portal.Portal;
import net.TheDgtl.Stargate.network.portal.RealPortal;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.GenericMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Feel free to change the package name and project license.
 */
public final class StargateDynmap extends JavaPlugin implements Listener {

    DynmapAPI dynmapAPI;
    StargateAPI stargateAPI;
    MarkerSet markerSet;
    MarkerIcon portalIcon;

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
        dynmapAPI = (DynmapAPI) Bukkit.getPluginManager().getPlugin("dynmap");

        //Get the Stargate API
        ServicesManager servicesManager = this.getServer().getServicesManager();
        RegisteredServiceProvider<StargateAPI> stargateProvider = servicesManager.getRegistration(StargateAPI.class);
        if (stargateProvider != null) {
            stargateAPI = stargateProvider.getProvider();
        } else {
            throw new IllegalStateException("Unable to hook into Stargate. Make sure the Stargate plugin is installed " +
                    "and enabled.");
        }
        markerSet = dynmapAPI.getMarkerAPI().createMarkerSet("stargate", "Stargate", null, false);
        portalIcon = dynmapAPI.getMarkerAPI().getMarkerIcon("portal");
        updatePortals();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        markerSet.deleteMarkerSet();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void portalEvent(StargateCreateEvent event) {
        if (!event.getDeny()) {
            updatePortals();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void portalEvent(StargateDestroyEvent event) {
        if (!event.getDeny()) {
            updatePortals();
        }
    }

    public List<Portal> getAllPortals() {
        List<Portal> portals = new ArrayList<>();
        Map<String, Network> networks = stargateAPI.getRegistry().getNetworkMap();
        for (Network network : networks.values()) {
            portals.addAll(network.getAllPortals());
        }
        return portals;
    }

    public void updatePortals() {
        List<Portal> portals = getAllPortals();
        // Delete all markers
        markerSet.getMarkers().forEach(GenericMarker::deleteMarker);
        portals.forEach(portal -> {
            try {
                Field hiddenField = portal.getClass().getDeclaredField("hidden");
                boolean hidden = (boolean) hiddenField.get(portal);
                if (hidden) {
                    return;
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
            Location location;
            if (!(portal instanceof RealPortal)) {
                return;
            }
            location = ((RealPortal) portal).getExit();
            String destinationName = portal.getDestinationName();
            // TODO: I18N
            if (StringUtils.isEmpty(destinationName)) {
                destinationName = "<Non-directional Stargate>";
            }
            Marker marker = markerSet.createMarker(null, portal.getName(), ((RealPortal) portal).getExit().getWorld().getName(), location.getX(), location.getY(), location.getZ(), portalIcon, false);
            String desc = "Name: " + portal.getName() + "<br />" +
                    "Network: " + portal.getNetwork() + "<br />" +
                    "Destination: " + destinationName + "<br />" +
                    "Owner: " + Bukkit.getOfflinePlayer(portal.getOwnerUUID()).getName() + "<br />";
            marker.setDescription(desc);
            marker.setLabel(portal.getName(), true);
            marker.setMarkerIcon(portalIcon);
        });
    }

}
