package dsclient.models;

public class JobSubmission {
    public int submitTime;
    public int id;
    public int estimatedRuntime;
    public int coreCount;
    public int memory;
    public int disk;

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
}
