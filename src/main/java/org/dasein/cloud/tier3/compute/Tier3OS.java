package org.dasein.cloud.tier3.compute;

public class Tier3OS {
    public int id;
    public String name;
    public int maxCpu;
    public int maxMemory;

    public Tier3OS(int id, String name, int maxCpu, int maxMemory) {
        this.id = id;
        this.name = name;
        this.maxCpu = maxCpu;
        this.maxMemory = maxMemory;
    }
}
