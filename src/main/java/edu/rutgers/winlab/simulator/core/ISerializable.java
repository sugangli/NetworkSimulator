package edu.rutgers.winlab.simulator.core;

@FunctionalInterface
public interface ISerializable {
    public static final int BIT = 1;
    public static final int K_BIT = 1000 * BIT;
    public static final int M_BIT = 1000 * K_BIT;
    public static final int G_BIT = 1000 * M_BIT;

    public static final int BYTE = 8 * BIT;
    public static final int K_BYTE = 1000 * BYTE;
    public static final int M_BYTE = 1000 * K_BYTE;
    public static final int G_BYTE = 1000 * M_BYTE;
    
    public int getSize();

}
