package de.jpx3.intave.packet.reader;

public final class BlockChangedAckReader extends AbstractPacketReader {
  public int sequenceNumber() {
    return packet().getIntegers().read(0);
  }
}
