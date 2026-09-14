package de.jpx3.intave.check.movement.physics.recording;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Supplies the Bukkit effect registry used by PTR playback without keeping a fake server installed. */
public final class ReplayPotionRegistry {
  private static boolean initialized;

  public static synchronized void initialize() {
    if (initialized || Bukkit.getServer() != null) return;
    try {
      var serverField = Bukkit.class.getDeclaredField("server");
      serverField.setAccessible(true);
      Map<NamespacedKey, PotionEffectType> effects = new LinkedHashMap<>();
      var server = Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
        (proxy, method, args) -> {
          if (!method.getName().equals("getRegistry")) {
            throw new UnsupportedOperationException(method.getName());
          }
          boolean potionRegistry = args[0] == PotionEffectType.class;
          return Proxy.newProxyInstance(Registry.class.getClassLoader(), new Class<?>[]{Registry.class},
            (registry, registryMethod, registryArgs) -> switch (registryMethod.getName()) {
              case "getOrThrow" -> {
                if (!potionRegistry) throw new UnsupportedOperationException("Only potion effects are provided");
                var key = (NamespacedKey) registryArgs[0];
                yield effects.computeIfAbsent(key, value -> new RecordedEffectType(value, effects.size() + 1));
              }
              case "get" -> potionRegistry ? effects.get(registryArgs[0]) : null;
              case "iterator" -> effects.values().iterator();
              case "stream" -> effects.values().stream();
              default -> throw new UnsupportedOperationException(registryMethod.getName());
            });
        });
      serverField.set(null, server);
      try {
        Class.forName(PotionEffectType.class.getName(), true, PotionEffectType.class.getClassLoader());
        initialized = true;
      } finally {
        serverField.set(null, null);
      }
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Could not initialize replay potion effects", exception);
    }
  }

  private static final class RecordedEffectType extends PotionEffectType {
    private final NamespacedKey key;
    private final int id;

    RecordedEffectType(NamespacedKey key, int id) {
      this.key = key;
      this.id = id;
    }

    @Override public PotionEffect createEffect(int duration, int amplifier) { return new PotionEffect(this, duration, amplifier); }
    @Override public boolean isInstant() { return false; }
    @Override public PotionEffectTypeCategory getCategory() { return PotionEffectTypeCategory.NEUTRAL; }
    @Override public Color getColor() { return Color.WHITE; }
    @Override public NamespacedKey getKey() { return key; }
    @Override public NamespacedKey getKeyOrThrow() { return key; }
    @Override public NamespacedKey getKeyOrNull() { return key; }
    @Override public boolean isRegistered() { return true; }
    @Override public double getDurationModifier() { return 1.0D; }
    @Override public int getId() { return id; }
    @Override public String getName() { return key.getKey().toUpperCase(Locale.ROOT); }
    @Override public String getTranslationKey() { return "effect." + key.getNamespace() + "." + key.getKey(); }
  }
}
