package dsclient.models;

public class Resource {
    public String type;
    public int id;
    public int state;
    public int availableTime;
    public int coreCount;
    public int memory;
    public int disk;

    Resource(
        String type,
        int id,
        int state,
        int availableTime,
        int coreCount,
        int memory,
        int disk
    ) {
        this.type = type;
        this.id = id;
        this.state = state;
        this.availableTime = availableTime;
        this.coreCount = coreCount;
        this.memory = memory;
        this.disk = disk;
    }

    public static Resource fromReceivedLine(String line) {
        String[] parts = line.split("\\s+");
        return new Resource(
            parts[0],
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3]),
            Integer.parseInt(parts[4]),
            Integer.parseInt(parts[5]),
            Integer.parseInt(parts[6])
        );
    }
}
