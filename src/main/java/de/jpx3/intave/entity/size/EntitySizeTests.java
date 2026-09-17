/*
 * Copyright 2026 Intave
 *
 * This software is licensed under the PolyForm Perimeter License 1.0.0.
 * You may use this software for any purpose, except for providing to
 * others any product that competes with the software.
 *
 * A copy of the license is available at:
 *   https://polyformproject.org/licenses/perimeter/1.0.0/
 */

package de.jpx3.intave.entity.size;

import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.klass.Lookup;
import de.jpx3.intave.entity.type.EntityTypeData;
import de.jpx3.intave.entity.type.EntityTypeDataAccessor;
import de.jpx3.intave.reflect.access.ReflectiveHandleAccess;
import de.jpx3.intave.test.IntegrationTests;
import de.jpx3.intave.test.Severity;
import de.jpx3.intave.test.Test;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Sheep;

public final class EntitySizeTests extends IntegrationTests {
  public EntitySizeTests() {
    super("ES");
  }

  @Test(testCode = "native-registry", severity = Severity.ERROR)
  public void testNativeEntityRegistry() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_3.below()) return;
    Object registry = Lookup.serverField("IRegistry", "ENTITY_TYPE").get(null);
    java.lang.reflect.Method getId = registry.getClass().getMethod("getId", Object.class);
    java.lang.reflect.Method getKey = registry.getClass().getMethod("getKey", Object.class);
    java.lang.reflect.Method getDimensions = Lookup.serverClass("EntityTypes").getMethod("getDimensions");
    int count = 0;
    for (Object type : (Iterable<?>) registry) {
      int id = (int) getId.invoke(registry, type);
      EntityTypeData entry = EntityTypeDataAccessor.resolveFromId(id, false);
      if (entry == null) fail("Missing native entity ID " + id);
      String key = getKey.invoke(registry, type).toString();
      if (entry.typeId() != id || !("minecraft:" + entry.name()).equals(key)) {
        fail("Incorrect native entity mapping at " + id + ": " + key);
      }
      Object size = getDimensions.invoke(type);
      float width = (float) size.getClass().getMethod("width").invoke(size);
      float height = (float) size.getClass().getMethod("height").invoke(size);
      if (entry.size().width() != width || entry.size().height() != height) {
        fail("Incorrect native dimensions for " + key);
      }
      count++;
    }
    if (EntityTypeDataAccessor.resolveFromId(count, false) != null) fail("Unexpected stored entity ID " + count);
  }

  @Test(
    severity = Severity.ERROR
  )
  public void testSheep() {
    World world = Bukkit.getWorlds().get(0);
    // spawn sheep
    Sheep sheep = world.spawn(new Location(world, 0,0,0), Sheep.class);
    Object handle = ReflectiveHandleAccess.handleOf(sheep);
    // get size
    Class<?> entityClass = handle.getClass();
    HitboxSize size = HitboxSizeAccess.dimensionsOfNMSEntityClass(entityClass);
    sheep.remove();
    if (size == null || Math.abs(size.width() - 0.9) > 0.01 || Math.abs(size.height() - 1.3) > 0.01) {
      fail("Failed to fetch sheep size, is " + size);
    }
  }
}
