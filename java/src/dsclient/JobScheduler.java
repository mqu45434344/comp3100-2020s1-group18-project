package dsclient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import dsclient.models.ServerType;
import dsclient.scheduling_policies.AllToLargest;

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
    throws ParserConfigurationException, FileNotFoundException, SAXException, IOException
    {
        servers.addAll(parseForServerTypes(filename));
    }

    public void readSystemXml()
    throws ParserConfigurationException, FileNotFoundException, SAXException, IOException
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
