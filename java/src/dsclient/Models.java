package dsclient;

import org.w3c.dom.Element;

class ServerType {
    public String type;
    public int limit;
    public int bootupTime;
    public double rate;
    public int coreCount;
    public int memory;
    public int disk;

    public static ServerType fromElement(Element el) {
        return new ServerType(
            el.getAttribute("type"),
            Integer.parseInt(el.getAttribute("limit")),
            Integer.parseInt(el.getAttribute("bootupTime")),
            Double.parseDouble(el.getAttribute("rate")),
            Integer.parseInt(el.getAttribute("coreCount")),
            Integer.parseInt(el.getAttribute("memory")),
            Integer.parseInt(el.getAttribute("disk"))
        );
    }

    ServerType(
        String type,
        int limit,
        int bootupTime,
        double rate,
        int coreCount,
        int memory,
        int disk
    ) {
        this.type = type;
        this.limit = limit;
        this.bootupTime = bootupTime;
        this.rate = rate;
        this.coreCount = coreCount;
        this.memory = memory;
        this.disk = disk;
    }
}

class JobSubmission {
    public int submitTime;
    public int id;
    public int estimatedRuntime;
    public int coreCount;
    public int memory;
    public int disk;

    public static JobSubmission fromReceivedLine(String line) {
        String[] parts = line.split("\\s+");
        if (!parts[0].equals("JOBN")) {
            throw new IllegalArgumentException();
        }

        return new JobSubmission(
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3]),
            Integer.parseInt(parts[4]),
            Integer.parseInt(parts[5]),
            Integer.parseInt(parts[6])
        );
    }

    JobSubmission(
        int submitTime,
        int id,
        int estimatedRuntime,
        int coreCount,
        int memory,
        int disk
    ) {
        this.submitTime = submitTime;
        this.id = id;
        this.estimatedRuntime = estimatedRuntime;
        this.coreCount = coreCount;
        this.memory = memory;
        this.disk = disk;
    }
}

class Resource {
    public String type;
    public int id;
    public int state;
    public int availableTime;
    public int coreCount;
    public int memory;
    public int disk;

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
}
