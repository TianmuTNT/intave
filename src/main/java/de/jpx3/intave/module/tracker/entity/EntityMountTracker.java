package de.jpx3.intave.module.tracker.entity;

import de.jpx3.intave.user.User;
import de.jpx3.intave.user.meta.ConnectionMetadata;
import de.jpx3.intave.user.meta.MovementMetadata;

import java.util.LinkedHashSet;
import java.util.Set;

/** Applies complete passenger updates in client acknowledgement order. */
final class EntityMountTracker {
  void passengers(User observer, int vehicleId, int[] passengers) {
    int[] snapshot = passengers.clone();
    observer.tickFeedback(() -> applyPassengers(observer, vehicleId, snapshot));
  }

  void attachment(User observer, int passengerId, int vehicleId) {
    observer.tickFeedback(() -> {
      if (vehicleId == -1) {
        detach(observer, passengerId);
      } else {
        Entity vehicle = observer.meta().connection().entityBy(vehicleId);
        if (present(vehicle)) attach(observer, vehicle, passengerId);
      }
    });
  }

  private void applyPassengers(User observer, int vehicleId, int[] passengers) {
    ConnectionMetadata connection = observer.meta().connection();
    Entity vehicle = connection.entityBy(vehicleId);
    // ClientPacketListener also ignores passenger updates for unknown vehicles.
    if (!present(vehicle)) return;

    Set<Integer> desired = new LinkedHashSet<>();
    for (int passengerId : passengers) {
      if (passengerId == observer.player().getEntityId() || present(connection.entityBy(passengerId))) {
        desired.add(passengerId);
      }
    }
    // This map includes the local player, who has no tracked passenger Entity.
    for (int passengerId : connection.sittingOn(vehicleId)) {
      if (!desired.contains(passengerId)) detach(observer, passengerId);
    }
    vehicle.clearPassengers();
    for (int passengerId : desired) attach(observer, vehicle, passengerId);
  }

  private void attach(User observer, Entity vehicle, int passengerId) {
    ConnectionMetadata connection = observer.meta().connection();
    Entity passenger = connection.entityBy(passengerId);
    boolean localPlayer = passengerId == observer.player().getEntityId();
    if (!localPlayer && !present(passenger)) return;

    if (present(passenger)) {
      Entity previous = passenger.vehicle();
      if (previous != null && previous != vehicle) previous.removePassenger(passenger);
      if (!vehicle.passengers().contains(passenger)) vehicle.addPassenger(passenger);
      passenger.mountToEntity(vehicle);
    }
    connection.noteMount(passengerId, vehicle.entityId());
    if (localPlayer) {
      MovementMetadata movement = observer.meta().movement();
      if (movement.vehicle() == vehicle) return;
      if (movement.isInVehicle()) movement.dismountRidingEntity("Vehicle switch", false);
      movement.setVehicle(vehicle);
    }
  }

  private void detach(User observer, int passengerId) {
    ConnectionMetadata connection = observer.meta().connection();
    Entity passenger = connection.entityBy(passengerId);
    if (present(passenger)) {
      Entity vehicle = passenger.vehicle();
      if (vehicle != null) vehicle.removePassenger(passenger);
      passenger.unmountFromEntity();
    }
    connection.noteDismount(passengerId);
    if (passengerId == observer.player().getEntityId()) {
      // The server has already changed the mount; teleporting here would undo it.
      observer.meta().movement().dismountRidingEntity("Dismount", false);
    }
  }

  private boolean present(Entity entity) {
    return entity != null && entity != Entity.destroyedEntity();
  }
}
