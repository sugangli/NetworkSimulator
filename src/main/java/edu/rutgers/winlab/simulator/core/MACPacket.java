package edu.rutgers.winlab.simulator.core;

public class MACPacket implements ISerializable {
    public static final int MAC_PACKET_HEADER_SIZE = 9 * BYTE;
 
    private final Node _from;
    private final Node _to;
    private final ISerializable _payload;

    public MACPacket(Node from, Node to, ISerializable payload) {
        _from = from;
        _to = to;
        _payload = payload;
    }

    public Node getFrom() {
        return _from;
    }

    public Node getTo() {
        return _to;
    }

    public ISerializable getPayload() {
        return _payload;
    }

    @Override
    public int getSize() {

        return _payload.getSize() + MAC_PACKET_HEADER_SIZE;
    }

//    public String toString() {
//        return String.format("MAC:{From:%s, To:%s, Payload:%s}", From, To, Macpayload);
//    }

}
