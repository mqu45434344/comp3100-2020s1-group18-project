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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


class Server {
    public String type;
    public int limit;
    public int bootupTime;
    public double rate;
    public int coreCount;
    public int memory;
    public int disk;

    Server(
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
    public int jobId;
    public int estimatedRuntime;
    public int coreCount;
    public int memory;
    public int disk;

    public static JobSubmission fromReceivedLine(String line) {
        String[] parts = line.split("\\s+");
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
        int jobId,
        int estimatedRuntime,
        int coreCount,
        int memory,
        int disk
    ) {
        this.submitTime = submitTime;
        this.jobId = jobId;
        this.estimatedRuntime = estimatedRuntime;
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

            JobSubmission jobn = JobSubmission.fromReceivedLine(incoming);
            scheduler.inquire(String.format("SCHD %d %s 0", jobn.jobId, largestServerType));
        }
    }
}


public class JobScheduler {
    public JobDispatchPolicy dispatchPolicy;
    public List<Server> servers;

    private Socket sock;
    private OutputStream sockOutput;
    private InputStream sockInput;

    JobScheduler(String address, int port, JobDispatchPolicy dispatchPolicy) throws IOException {
        sock = new Socket();
        sock.connect(new InetSocketAddress(address, port), 1000);
        sockOutput = sock.getOutputStream();
        sockInput = sock.getInputStream();

        this.dispatchPolicy = dispatchPolicy;

        servers = new ArrayList<Server>();
    }

    public void close() throws IOException {
        sock.close();
    }

    public void send(String message) throws IOException {
        byte[] data = message.getBytes();
        sockOutput.write(data);
        sockOutput.flush();
    }

    public String receive() throws IOException {
        byte[] data = new byte[4096];
        int bytesRead = sockInput.read(data);
        return new String(data, 0, bytesRead, StandardCharsets.UTF_8);
    }

    public String inquire(String message) throws IOException {
        send(message);
        return receive();
    }

    public List<Server> parseForServers(String filename)
            throws ParserConfigurationException, FileNotFoundException, SAXException, IOException
    {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(filename);

        List<Server> servers = new ArrayList<>();
        Element element = (Element) document.getElementsByTagName("servers").item(0);
        NodeList serversNodeList = element.getElementsByTagName("server");
        for (int i = 0; i < serversNodeList.getLength(); i++) {
            Element serverElement = (Element) serversNodeList.item(i);
            servers.add(
                new Server(
                    serverElement.getAttribute("type"),
                    Integer.parseInt(serverElement.getAttribute("limit")),
                    Integer.parseInt(serverElement.getAttribute("bootupTime")),
                    Double.parseDouble(serverElement.getAttribute("rate")),
                    Integer.parseInt(serverElement.getAttribute("coreCount")),
                    Integer.parseInt(serverElement.getAttribute("memory")),
                    Integer.parseInt(serverElement.getAttribute("disk"))
                )
            );
        }
        return servers;
    }

    public void run()
            throws IOException, ParserConfigurationException, SAXException
    {
        inquire("HELO");
        inquire("AUTH " + System.getProperty("user.name"));

        servers.addAll(parseForServers("system.xml"));

        dispatchPolicy.dispatch(this);

        inquire("QUIT");
        close();
    }
}


class Main {
    public static void main(String[] args)
            throws IOException, ParserConfigurationException, SAXException
    {
        JobScheduler scheduler = new JobScheduler("127.0.0.1", 50000, new AllToLargest());
        scheduler.run();
    }
}
