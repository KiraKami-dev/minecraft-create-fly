package dev.ryanhcode.sable.mixinterface.tracking_points;

public interface LoginPointExtension {

    /**
     * Marks that the player was moved by a login point, so their saved vehicle must not be re-attached.
     */
    void sable$skipRootVehicle();
}
