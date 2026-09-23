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

package de.jpx3.intave.check.combat.attackraytrace;

import com.comphenix.protocol.utility.MinecraftReflection;
import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import de.jpx3.intave.world.raytrace.Raytracing;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public final class AttackReach {
  private static final MinecraftVersion COMPONENT_VERSION = new MinecraftVersion("1.21.11");
  private static boolean warned;

  private final double maximum;
  private final double hitboxMargin;

  private AttackReach(double maximum, double hitboxMargin) {
    this.maximum = maximum;
    this.hitboxMargin = hitboxMargin;
  }

  public double maximum() {
    return maximum;
  }

  public double hitboxMargin() {
    return hitboxMargin;
  }

  public static AttackReach resolve(User user) {
    double normalReach = Raytracing.reachDistanceOf(user);
    if (user.meta().protocol().protocolVersion() < ProtocolMetadata.VER_1_21_11
      || COMPONENT_VERSION.below()) {
      return new AttackReach(normalReach, 0.0D);
    }

    ItemStack heldItem = user.meta().inventory().heldItem();
    if (heldItem == null || heldItem.getAmount() <= 0 || heldItem.getType() == Material.AIR) {
      return new AttackReach(normalReach, 0.0D);
    }

    try {
      Object nativeItem = MinecraftReflection.getMinecraftItemStack(heldItem);
      Object component = ComponentAccess.GET.invoke(nativeItem, ComponentAccess.ATTACK_RANGE);
      if (component == null) {
        return new AttackReach(normalReach, 0.0D);
      }
      boolean creative = user.meta().abilities().inGameMode(GameMode.CREATIVE);
      Method maximumMethod = creative ? ComponentAccess.MAX_CREATIVE_RANGE : ComponentAccess.MAX_RANGE;
      double maximum = ((Number) maximumMethod.invoke(component)).doubleValue();
      double margin = ((Number) ComponentAccess.HITBOX_MARGIN.invoke(component)).doubleValue();
      if (!Double.isFinite(maximum) || !Double.isFinite(margin)) {
        return null;
      }
      maximum = Math.max(0.0D, Math.min(64.0D, maximum));
      margin = Math.max(0.0D, Math.min(1.0D, margin));
      return new AttackReach(maximum + margin, margin);
    } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
      warnOnce(exception);
      return null;
    }
  }

  private static synchronized void warnOnce(Throwable exception) {
    if (!warned) {
      warned = true;
      IntaveLogger.logger().warn("Unable to read item attack range; skipping uncertain attack raytraces: " + exception);
    }
  }

  private static final class ComponentAccess {
    private static final Object ATTACK_RANGE;
    private static final Method GET;
    private static final Method MAX_RANGE;
    private static final Method MAX_CREATIVE_RANGE;
    private static final Method HITBOX_MARGIN;

    static {
      try {
        ClassLoader loader = MinecraftReflection.getItemStackClass().getClassLoader();
        Class<?> type = Class.forName("net.minecraft.core.component.DataComponentType", true, loader);
        Class<?> components = Class.forName("net.minecraft.core.component.DataComponents", true, loader);
        Class<?> range = Class.forName("net.minecraft.world.item.component.AttackRange", true, loader);
        ATTACK_RANGE = components.getField("ATTACK_RANGE").get(null);
        GET = MinecraftReflection.getItemStackClass().getMethod("get", type);
        MAX_RANGE = accessor(range, "maxReach", "maxRange");
        MAX_CREATIVE_RANGE = accessor(range, "maxCreativeReach", "maxCreativeRange");
        HITBOX_MARGIN = range.getMethod("hitboxMargin");
      } catch (ReflectiveOperationException exception) {
        throw new IllegalStateException("Unable to resolve attack-range item component", exception);
      }
    }

    private static Method accessor(Class<?> type, String currentName, String previousName)
      throws NoSuchMethodException {
      try {
        return type.getMethod(currentName);
      } catch (NoSuchMethodException ignored) {
        return type.getMethod(previousName);
      }
    }
  }
}
