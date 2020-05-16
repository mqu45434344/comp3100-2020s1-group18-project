package dsclient.models;

import org.w3c.dom.Element;

public class ServerType {
    public String type;
    public int limit;
    public int bootupTime;
    public double rate;
    public int coreCount;
    public int memory;
    public int disk;

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
}
