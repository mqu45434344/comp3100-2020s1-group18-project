package dsclient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


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


interface JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException;
}

class AllToLargest implements JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException {
        String largestServerType = Collections.max(
            scheduler.servers, Comparator.comparing(srv -> srv.coreCount)
        ).type;

        while (true) {
            String incoming = scheduler.inquire("REDY");
            if (incoming.equals("NONE")) {
                break;
            }

            JobSubmission job = JobSubmission.fromReceivedLine(incoming);
            scheduler.inquire(String.format("SCHD %d %s 0", job.id, largestServerType));
        }
    }
}

class FirstAvailableWithSufficientResources implements JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException {
        String largestServerType = Collections.max(
            scheduler.servers, Comparator.comparing(srv -> srv.coreCount)
        ).type;

        while (true) {
            String incoming = scheduler.inquire("REDY");
            if (incoming.equals("NONE")) {
                break;
            }

            JobSubmission job = JobSubmission.fromReceivedLine(incoming);

            scheduler.inquire(String.format("RESC Avail %d %d %d", job.coreCount, job.memory, job.disk));
            incoming = scheduler.inquire("OK");

            if (incoming.equals(".")) {
                // No server available with sufficient resources.
                // Fall back to the largest server type with ID 0.
                scheduler.inquire(String.format("SCHD %d %s 0", job.id, largestServerType));
            } else {
                Resource firstAvailable = Resource.fromReceivedLine(incoming);

                while (!incoming.equals(".")) {
                    incoming = scheduler.inquire("OK");
                }
                scheduler.inquire(String.format("SCHD %d %s %d", job.id, firstAvailable.type, firstAvailable.id));
            }
        }
    }
}


public class JobScheduler {
    public JobDispatchPolicy dispatchPolicy;
    public boolean newlines = false;
    public List<ServerType> servers = new ArrayList<>();

    private Socket sock;
    private OutputStream sockOutput;
    private InputStream sockInput;

    JobScheduler(String address, int port, JobDispatchPolicy dispatchPolicy) throws IOException {
        sock = new Socket();
        sock.connect(new InetSocketAddress(address, port), 1000);
        sockOutput = sock.getOutputStream();
        sockInput = sock.getInputStream();

        this.dispatchPolicy = dispatchPolicy;
    }

    JobScheduler(String address, int port) throws IOException {
        this(address, port, new AllToLargest());
    }

    public static List<ServerType> parseForServerTypes(String filename)
            throws ParserConfigurationException, FileNotFoundException, SAXException, IOException
    {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(filename);

        List<ServerType> servers = new ArrayList<>();
        Element element = (Element) document.getElementsByTagName("servers").item(0);
        NodeList serversNodeList = element.getElementsByTagName("server");
        for (int i = 0; i < serversNodeList.getLength(); i++) {
            Element serverElement = (Element) serversNodeList.item(i);
            servers.add(ServerType.fromElement(serverElement));
        }
        return servers;
    }

    public void close() throws IOException {
        sock.close();
    }

    public void send(String message) throws IOException {
        if (newlines) {
            message += "\n";
        }
        byte[] data = message.getBytes();
        sockOutput.write(data);
        sockOutput.flush();
    }

    public String receive() throws IOException {
        byte[] data = new byte[4096];
        int length = sockInput.read(data);
        if (newlines) {
            length -= 1;
        }
        return new String(data, 0, length, StandardCharsets.UTF_8);
    }

    public String inquire(String message) throws IOException {
        send(message);
        return receive();
    }

    public void readSystemXml(String filename)
            throws ParserConfigurationException, FileNotFoundException,
                SAXException, IOException
    {
        servers.addAll(parseForServerTypes(filename));
    }

    public void readSystemXml()
            throws ParserConfigurationException, FileNotFoundException,
                SAXException, IOException
    {
        readSystemXml("system.xml");
    }

    public void run()
            throws IOException, ParserConfigurationException, SAXException
    {
        inquire("HELO");
        inquire("AUTH " + System.getProperty("user.name"));
        readSystemXml();

        dispatchPolicy.dispatch(this);

        inquire("QUIT");
        close();
    }
}


class Main {
    public static void main(String[] args)
            throws IOException, ParserConfigurationException, SAXException
    {
        Set<String> argSet = Set.of(args);

        JobScheduler scheduler = new JobScheduler("127.0.0.1", 50000, new FirstAvailableWithSufficientResources());
        scheduler.newlines = argSet.contains("-n");
        scheduler.run();
    }
}
