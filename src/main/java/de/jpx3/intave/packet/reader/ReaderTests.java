package de.jpx3.intave.packet.reader;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.events.PacketEvent;
import de.jpx3.intave.module.dispatch.AttackDispatcher;
import com.comphenix.protocol.wrappers.Pair;
import de.jpx3.intave.adapter.ProtocolLibraryAdapter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.entity.size.HitboxSize;
import de.jpx3.intave.entity.type.EntityTypeData;
import de.jpx3.intave.module.tracker.entity.Entity;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.PositionAndRotation;
import de.jpx3.intave.packet.converter.PositionAndRotationConverter;
import de.jpx3.intave.version.ServerProtocolVersion;
import de.jpx3.intave.test.IntegrationTests;
import de.jpx3.intave.test.Severity;
import de.jpx3.intave.test.Test;

import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class ReaderTests extends IntegrationTests {
  private static final Set<PacketType> EXCLUDED_TYPES = new HashSet<>();
  static {
    EXCLUDED_TYPES.add(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
  }

  public ReaderTests() {
    super("PR");
  }

  @Test(testCode = "attachment-reader", severity = Severity.ERROR)
  public void testAttachmentReaderDistinguishesMountsFromLeashes() {
    boolean legacy = MinecraftVersions.VER1_9_0.below();
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ATTACH_ENTITY);
    int offset = legacy ? 1 : 0;
    if (legacy) packet.getIntegers().write(0, 0);
    packet.getIntegers().write(offset, 7).write(offset + 1, 10);
    try (AttachEntityReader reader = PacketReaders.readerOf(packet)) {
      assertEquals(7, reader.entityId());
      assertEquals(10, reader.vehicleId());
      assertEquals(legacy, reader.isMount());
      SubstitutionIterator<Integer> ids = reader.iterator();
      assertEquals(7, ids.next());
      ids.set(17);
      assertEquals(10, ids.next());
      ids.set(20);
      assertFalse(ids.hasNext());
      assertEquals(17, reader.entityId());
      assertEquals(20, reader.vehicleId());
      // A zero source ID on modern servers is still a leash, not a legacy mount flag.
      packet.getIntegers().write(0, legacy ? 1 : 0);
      assertFalse(reader.isMount());
    }
  }

  @Test(testCode = "mount-reader", severity = Severity.ERROR)
  public void testMountReaderRetainsAllPassengerIds() {
    if (MinecraftVersions.VER1_9_0.below()) return;
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.MOUNT);
    packet.getIntegers().write(0, 10);
    packet.getIntegerArrays().write(0, new int[]{7, 11});
    try (MountEntityReader reader = PacketReaders.readerOf(packet)) {
      assertEquals(10, reader.entityId());
      assertTrue(Arrays.equals(new int[]{7, 11}, reader.mounts()));
      packet.getIntegerArrays().write(0, new int[0]);
      assertEquals(0, reader.mounts().length);
    }
  }

  @Test(testCode = "vehicle-movement", severity = Severity.ERROR)
  public void testVehicleMovementReadWrite() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_2.below()) return;
    PacketContainer packet = vehiclePacket(1.25D, 64.5D, -2.75D, 90F, -30F);
    try (PlayerMoveReader reader = PacketReaders.readerOf(packet)) {
      assertTrue(reader.isVehicleMove());
      assertTrue(reader.hasMovement());
      assertTrue(reader.hasRotation());
      assertTrue(reader.onGround());
      assertMovement(reader, 1.25D, 64.5D, -2.75D, 90F, -30F);
      assertFalse(reader.anyNaNOrInfiniteValue());
      reader.setPositionX(-16.25D);
      reader.setPositionY(72.125D);
      reader.setPositionZ(33.75D);
      reader.setYaw(-125.5F);
      reader.setPitch(45.25F);
      reader.setOnGround(false);
      assertMovement(reader, -16.25D, 72.125D, 33.75D, -125.5F, 45.25F);
      assertFalse(reader.onGround());
      reader.setPosition(new Position(5.5D, -12.25D, 128.75D));
      assertMovement(reader, 5.5D, -12.25D, 128.75D, -125.5F, 45.25F);
    }
    // Reacquire the reader to catch writes that only changed a detached view.
    try (PlayerMoveReader reader = PacketReaders.readerOf(packet)) {
      assertMovement(reader, 5.5D, -12.25D, 128.75D, -125.5F, 45.25F);
      assertFalse(reader.onGround());
    }
  }

  @Test(testCode = "position-rotation-converter", severity = Severity.ERROR)
  public void testPositionAndRotationConversionAndReplacement() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_2.below()) return;
    PositionAndRotation initial = new PositionAndRotation(1.25D, 64.5D, -2.75D, 90F, -30F);
    PositionAndRotation changed = new PositionAndRotation(-0.0D, -12.25D, 128.75D, -125.5F, -0.0F);
    PacketContainer packet = vehiclePacket(initial.x(), initial.y(), initial.z(), initial.yaw(), initial.pitch());
    Object originalNative = MinecraftVersions.VER26_3.atOrAbove() ? packet.getModifier().read(0) : null;
    PositionAndRotation snapshot;
    try (PlayerMoveReader reader = PacketReaders.readerOf(packet)) {
      snapshot = reader.positionAndRotation();
      assertEquals(initial, snapshot);
      reader.setPositionAndRotation(changed);
      assertEquals(changed, reader.positionAndRotation());
      assertTrue(reader.onGround());
    }
    assertEquals(initial, snapshot);
    try (PlayerMoveReader reader = PacketReaders.readerOf(packet)) {
      assertEquals(changed, reader.positionAndRotation());
    }
    if (originalNative != null) {
      PositionAndRotationConverter converter = PositionAndRotationConverter.INSTANCE;
      assertEquals(initial, converter.getSpecific(originalNative));
      assertEquals(changed, converter.getSpecific(converter.getGeneric(changed)));
      // Verify the actual native accessor values, independently of the converter.
      Object replacement = packet.getModifier().read(0);
      assertEquals(changed.yaw(), converter.nativeType().getMethod("yRot").invoke(replacement));
      assertEquals(changed.pitch(), converter.nativeType().getMethod("xRot").invoke(replacement));
    }
  }

  @Test(testCode = "partial-player-movement", severity = Severity.ERROR)
  public void testPartialMovementDoesNotInventMissingComponents() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_2.below()) return;
    String prefix = "net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$";
    Object[] handles = {
      Class.forName(prefix + "Pos").getConstructor(double.class, double.class, double.class, boolean.class, boolean.class)
        .newInstance(1.25D, 64.5D, -2.75D, true, false),
      Class.forName(prefix + "Rot").getConstructor(float.class, float.class, boolean.class, boolean.class)
        .newInstance(90F, -30F, true, false),
      Class.forName(prefix + "StatusOnly").getConstructor(boolean.class, boolean.class).newInstance(true, false)
    };
    for (int i = 0; i < handles.length; i++) {
      // Exercise native decoding independently of packet-name registration.
      PlayerMoveReader acquired = new PlayerMoveReader();
      acquired.enter(PacketContainer.fromPacket(handles[i]));
      try (PlayerMoveReader reader = acquired) {
        assertNull(reader.positionAndRotation());
        assertEquals(i == 0, reader.hasMovement());
        assertEquals(i == 1, reader.hasRotation());
        boolean rejected = false;
        try {
          reader.setPositionAndRotation(new PositionAndRotation(8, 9, 10, 11, 12));
        } catch (IllegalStateException expected) {
          rejected = true;
        }
        assertTrue(rejected);
        if (i == 0) assertEquals(new Position(1.25D, 64.5D, -2.75D), reader.position());
        else assertNull(reader.position());
        if (i == 1) assertEquals(new de.jpx3.intave.share.Rotation(90F, -30F), reader.rotation());
        else assertNull(reader.rotation());
        assertFalse(reader.anyNaNOrInfiniteValue());
        assertTrue(reader.onGround());
      }
    }
  }

  @Test(testCode = "vehicle-invalid-movement", severity = Severity.ERROR)
  public void testVehicleMovementRejectsNonFiniteValues() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_2.below()) return;
    for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
      for (int field = 0; field < 5; field++) {
        PacketContainer packet = vehiclePacket(field == 0 ? invalid : 1.25D,
          field == 1 ? invalid : 64.5D, field == 2 ? invalid : -2.75D,
          field == 3 ? (float)invalid : 90F, field == 4 ? (float)invalid : -30F);
        try (PlayerMoveReader reader = PacketReaders.readerOf(packet)) {
          assertTrue(reader.anyNaNOrInfiniteValue());
        }
      }
    }
  }

  @Test(testCode = "player-movement", severity = Severity.ERROR)
  public void testPlayerMovementKeepsItsOwnLayout() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_2.below()) return;
    Object handle = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$PosRot")
      .getConstructor(double.class, double.class, double.class, float.class, float.class, boolean.class, boolean.class)
      .newInstance(1.25D, 64.5D, -2.75D, 90F, -30F, true, false);
    PacketContainer packet = new PacketContainer(PacketType.Play.Client.POSITION_LOOK, handle);
    try (PlayerMoveReader reader = PacketReaders.readerOf(packet)) {
      assertFalse(reader.isVehicleMove());
      assertTrue(reader.hasMovement());
      assertTrue(reader.hasRotation());
      assertTrue(reader.onGround());
      assertMovement(reader, 1.25D, 64.5D, -2.75D, 90F, -30F);
      reader.setPosition(new Position(-1D, 80D, 3D));
      reader.setYaw(15F);
      reader.setPitch(-60F);
      reader.setOnGround(false);
      assertMovement(reader, -1D, 80D, 3D, 15F, -60F);
      assertFalse(reader.onGround());
      assertFalse(reader.anyNaNOrInfiniteValue());
      PositionAndRotation combined = new PositionAndRotation(7, 8, 9, 10, 11);
      reader.setPositionAndRotation(combined);
      assertEquals(combined, reader.positionAndRotation());
    }
  }

  private void assertMovement(PlayerMoveReader reader, double x, double y, double z, float yaw, float pitch) {
    assertEquals(new PositionAndRotation(x, y, z, yaw, pitch), reader.positionAndRotation());
    assertEquals(x, reader.positionX());
    assertEquals(y, reader.positionY());
    assertEquals(z, reader.positionZ());
    assertEquals(new Position(x, y, z), reader.position());
    assertEquals(yaw, reader.yaw());
    assertEquals(pitch, reader.pitch());
    assertEquals(new de.jpx3.intave.share.Rotation(yaw, pitch), reader.rotation());
  }

  private PacketContainer vehiclePacket(double x, double y, double z, float yaw, float pitch)
    throws ReflectiveOperationException {
    Class<?> vector = Class.forName("net.minecraft.world.phys.Vec3");
    Object position = vector.getConstructor(double.class, double.class, double.class).newInstance(x, y, z);
    Class<?> packet = Class.forName("net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket");
    Object handle;
    if (MinecraftVersions.VER26_3.atOrAbove()) {
      Class<?> transform = Class.forName("net.minecraft.core.PositionAndRotation");
      Object movingTo = transform.getMethod("of", vector, float.class, float.class).invoke(null, position, yaw, pitch);
      handle = packet.getConstructor(transform, boolean.class).newInstance(movingTo, true);
    } else {
      handle = packet.getConstructor(vector, float.class, float.class, boolean.class).newInstance(position, yaw, pitch, true);
    }
    return new PacketContainer(PacketType.Play.Client.VEHICLE_MOVE, handle);
  }

  @Test(testCode = "native-protocol", severity = Severity.ERROR)
  public void testNativeProtocolMatchesSharedVersionResource() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_2.below()) return;
    Object version = Class.forName("net.minecraft.SharedConstants").getMethod("getCurrentVersion").invoke(null);
    int nativeProtocol = (int) Class.forName("net.minecraft.WorldVersion").getMethod("protocolVersion").invoke(version);
    assertEquals(nativeProtocol, ServerProtocolVersion.current());
  }

  @Test(testCode = "263-items", severity = Severity.ERROR)
  public void testItemStackPacketConversion() {
    if (MinecraftVersions.VER26_2.below()) return;
    ItemStack item = new ItemStack(Material.DIAMOND, 7);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName("Packet conversion test");
    meta.setLore(Arrays.asList("Keep metadata", "through conversion"));
    item.setItemMeta(meta);

    PacketContainer slot = new PacketContainer(PacketType.Play.Server.SET_SLOT);
    slot.getIntegers().write(0, 0).write(1, 4).write(2, 12);
    slot.getItemModifier().write(0, item);
    new AttackDispatcher().filterSharpness(PacketEvent.fromServer(this, slot, null));
    assertEquals(item, slot.getItemModifier().read(0));
    WindowSingleItemReader singleReader = new WindowSingleItemReader();
    singleReader.enter(slot);
    assertEquals(item, singleReader.itemMap().get(12));
    ItemStack copy = slot.getItemModifier().read(0).clone();
    copy.setAmount(3);
    slot.getItemModifier().write(0, copy);
    assertEquals(3, slot.getItemModifier().read(0).getAmount());
    assertEquals(7, item.getAmount());

    ItemStack empty = new ItemStack(Material.AIR);
    PacketContainer contents = new PacketContainer(PacketType.Play.Server.WINDOW_ITEMS);
    contents.getIntegers().write(0, 0).write(1, 4);
    contents.getItemListModifier().write(0, Arrays.asList(item, empty));
    contents.getItemModifier().write(0, copy);
    WindowBulkItemReader bulkReader = new WindowBulkItemReader();
    bulkReader.enter(contents);
    assertEquals(item, bulkReader.itemMap().get(0));
    assertEquals(Material.AIR, bulkReader.itemMap().get(1).getType());
    assertEquals(copy, bulkReader.carriedItem());

    PacketContainer equipment = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
    equipment.getSlotStackPairLists().write(0, Arrays.asList(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, item)));
    assertEquals(item, equipment.getSlotStackPairLists().read(0).get(0).getSecond());
    slot.getItemModifier().write(0, empty);
    assertEquals(Material.AIR, slot.getItemModifier().read(0).getType());
    slot.getItemModifier().write(0, null);
    assertEquals(Material.AIR, slot.getItemModifier().read(0).getType());
    ProtocolLibraryAdapter.prepareItemStackConversion();
    slot.getItemModifier().write(0, item);
    assertEquals(item, slot.getItemModifier().read(0));
  }

  @Test(testCode = "263-relative", severity = Severity.ERROR)
  public void testRelativeEntityMovement() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_2.below()) return;
    int protocol = ServerProtocolVersion.current();
    for (PacketType type : Arrays.asList(PacketType.Play.Server.REL_ENTITY_MOVE,
      PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, PacketType.Play.Server.ENTITY_LOOK)) {
      PacketContainer packet = linearMovePacket(type, (short)4096, (short)-2048, (short)1);
      boolean position = type != PacketType.Play.Server.ENTITY_LOOK;
      Entity entity = testEntity();
      entity.immediateEntityMovement(packet);
      entity.handleEntityMovement(null, packet, false);
      assertEquals(position ? 1.0D : 0.0D, entity.immediateServerPosition.getX());
      assertEquals(position ? -0.5D : 0.0D, entity.immediateServerPosition.getY());
      assertEquals(position ? 1.0D / 4096 : 0.0D, entity.immediateServerPosition.getZ());
      assertEquals(position ? 4096L : 0L, entity.serverPosX);
      EntityMovementReader.Delta update = EntityMovementReader.read(packet, protocol);
      assertEquals(position ? 4096L : 0L, update.x());
      assertTrue(packet.getBooleans().read(0));
      if (type != PacketType.Play.Server.REL_ENTITY_MOVE) {
        assertEquals((byte)64, packet.getBytes().read(0));
        assertEquals((byte)-32, packet.getBytes().read(1));
      }
      if (protocol >= 777 && position) {
        EntityMovementReader.writeLinear263(packet, Short.MIN_VALUE, (short)0, Short.MAX_VALUE);
        EntityMovementReader.Delta delta = EntityMovementReader.read(packet, protocol);
        assertEquals((long)Short.MIN_VALUE, delta.x());
        assertEquals((long)Short.MAX_VALUE, delta.z());
      }
    }
  }

  @Test(testCode = "263-stepped", severity = Severity.ERROR)
  public void testSteppedEntityMovement() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_3.below()) return;
    Class<?> stepType = Class.forName("net.minecraft.network.protocol.game.VecDelta$Stepped$DeltaStep");
    java.lang.reflect.Constructor<?> step = stepType.getConstructor(short.class, short.class, short.class, int.class);
    Object delta = Class.forName("net.minecraft.network.protocol.game.VecDelta$Stepped").getConstructor(List.class)
      .newInstance(Arrays.asList(step.newInstance((short)30000, (short)-1, (short)1, 2),
        step.newInstance((short)30000, (short)1, (short)-1, 4)));
    PacketContainer packet = movePacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, delta);
    EntityMovementReader.Delta decoded = EntityMovementReader.read(packet, ServerProtocolVersion.current());
    assertTrue(decoded.stepped());
    assertEquals(2, decoded.steps().size());
    assertEquals(2, decoded.steps().get(0).ticks);
    assertEquals(4, decoded.steps().get(1).ticks);
    assertEquals(60000L, decoded.x());
    assertEquals(0L, decoded.y());
    Entity entity = testEntity();
    entity.immediateEntityMovement(packet);
    entity.handleEntityMovement(null, packet, false);
    assertEquals(60000L, entity.serverPosX);
    assertEquals(60000D / 4096, entity.immediateServerPosition.getX());

  }

  @Test(testCode = "263-sync", severity = Severity.ERROR)
  public void testEntityPositionPathSync() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_3.below()) return;
    Class<?> vector = Class.forName("net.minecraft.world.phys.Vec3");
    Object end = vector.getConstructor(double.class, double.class, double.class).newInstance(1.25D, 64.125D, -2.5D);
    Object path = Class.forName("net.minecraft.world.entity.PositionPath$Linear").getConstructor(vector).newInstance(end);
    Object handle = Class.forName("net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket")
      .getConstructor(int.class, Class.forName("net.minecraft.world.entity.PositionPath"), float.class, float.class, boolean.class)
      .newInstance(7, path, 90F, -45F, true);
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_POSITION_SYNC, handle);
    Entity entity = testEntity();
    entity.immediateEntityPositionSync(packet);
    assertEquals(1.25D, entity.immediateServerPosition.getX());
    assertEquals(64.125D, entity.immediateServerPosition.getY());
    assertEquals(-2.5D, entity.immediateServerPosition.getZ());
    de.jpx3.intave.share.PositionMoveRotation sync =
      EntityMovementReader.readPositionSync(packet, ServerProtocolVersion.current());
    assertEquals(90F, sync.rotation().yaw());
    assertEquals(-45F, sync.rotation().pitch());
    assertEquals(64.125D, sync.position().getY());
  }

  private Entity testEntity() {
    return new Entity(7, new EntityTypeData("player", HitboxSize.of(0.6F, 1.8F), 159, true, 0), true);
  }

  private PacketContainer linearMovePacket(PacketType type, short x, short y, short z) throws ReflectiveOperationException {
    if (type == PacketType.Play.Server.ENTITY_LOOK) {
      Object handle = Class.forName("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$Rot")
        .getConstructor(int.class, byte.class, byte.class, boolean.class).newInstance(7, (byte)64, (byte)-32, true);
      return new PacketContainer(type, handle);
    }
    if (MinecraftVersions.VER26_3.atOrAbove()) {
      Object delta = Class.forName("net.minecraft.network.protocol.game.VecDelta$Linear")
        .getConstructor(short.class, short.class, short.class).newInstance(x, y, z);
      return movePacket(type, delta);
    }
    boolean rotation = type == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK;
    Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$" + (rotation ? "PosRot" : "Pos"));
    Object handle = rotation
      ? packetClass.getConstructor(int.class, short.class, short.class, short.class, byte.class, byte.class, boolean.class)
        .newInstance(7, x, y, z, (byte)64, (byte)-32, true)
      : packetClass.getConstructor(int.class, short.class, short.class, short.class, boolean.class).newInstance(7, x, y, z, true);
    return new PacketContainer(type, handle);
  }

  private PacketContainer movePacket(PacketType type, Object delta) throws ReflectiveOperationException {
    boolean rotation = type == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK;
    Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$" + (rotation ? "PosRot" : "Pos"));
    Class<?> deltaType = Class.forName("net.minecraft.network.protocol.game.VecDelta");
    Object handle = rotation
      ? packetClass.getConstructor(int.class, deltaType, byte.class, byte.class, boolean.class).newInstance(7, delta, (byte)64, (byte)-32, true)
      : packetClass.getConstructor(int.class, deltaType, boolean.class).newInstance(7, delta, true);
    return new PacketContainer(type, handle);
  }

  @Test(testCode = "native-dig", severity = Severity.ERROR)
  public void testNativeDigActions() throws ReflectiveOperationException {
    if (MinecraftVersions.VER26_2.below()) return;
    PacketContainer packet = new PacketContainer(PacketType.Play.Client.BLOCK_DIG);
    BlockPosition position = new BlockPosition(-17, 64, 33);
    packet.getBlockPositionModifier().write(0, position);
    Class<?> actionType = Class.forName("net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action");
    for (Object value : actionType.getEnumConstants()) {
      packet.getModifier().withType(actionType).write(0, value);
      String name = ((Enum<?>) value).name();
      if ("SWAP_ITEM_WITH_OFFHAND".equals(name)) name = "SWAP_HELD_ITEMS";
      EnumWrappers.PlayerDigType expected;
      try {
        expected = EnumWrappers.PlayerDigType.valueOf(name);
      } catch (IllegalArgumentException ignored) {
        expected = null;
      }
      try (BlockDigReader reader = PacketReaders.readerOf(packet)) {
        if (reader.action() != expected) fail("Incorrect digging action for " + name);
        assertEquals(position, reader.blockPosition());
        assertEquals(new de.jpx3.intave.share.BlockPosition(-17, 64, 33), reader.nativeBlockPosition());
      }
    }
  }

  @Test(
    testCode = "A",
    severity = Severity.ERROR
  )
  public void testRegisteredReadersAcquireAndReleasePackets() {
    int checkedReaders = 0;
    for (PacketType value : PacketType.values()) {
      if (PacketReaders.hasReader(value) && !EXCLUDED_TYPES.contains(value)) {
        PacketContainer packet;
	      try {
          packet = new PacketContainer(value);
        } catch (Throwable exception) {
          exception.printStackTrace();
          throw new IllegalStateException("Failed to create packet container for " + value);
        }
        // Empty packets are useful for lifecycle checks, not semantic getter tests.
        // Exercise decoded values with explicit populated fixtures above instead.
        AbstractPacketReader reader = PacketReaders.readerOf(packet);
        try (AbstractPacketReader acquired = reader) {
          assertSame(packet, acquired.packet());
        }
        assertNull(reader.packet());
        checkedReaders++;
      }
    }
    assertTrue(checkedReaders > 0);
  }
}
