package dsclient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import dsclient.scheduling_policies.AllToLargest;
import dsclient.scheduling_policies.FirstFit;
import dsclient.scheduling_policies.BestFit;
import dsclient.scheduling_policies.WorstFit;
import dsclient.scheduling_policies.CostEffective;

class Main {
    public static void main(String[] args)
    throws IOException, ParserConfigurationException, SAXException
    {
        Set<String> argSet = Set.of(args);

        String algo = "al";
        if (argSet.contains("-a")) {
            int idx = Arrays.asList(args).indexOf("-a");
            try {
                algo = args[idx + 1];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new RuntimeException("a value must be specified for option -a");
            }
        }

        JobDispatchPolicy dispatchPolicy;
        switch (algo) {
            case "al":
                dispatchPolicy = new AllToLargest();
                break;
            case "ff":
                dispatchPolicy = new FirstFit();
                break;
            case "bf":
                dispatchPolicy = new BestFit();
                break;
            case "wf":
                dispatchPolicy = new WorstFit();
                break;
            case "ce":
                dispatchPolicy = new CostEffective();
                break;
            default:
                throw new RuntimeException("algorithm not supported");
        }

        JobScheduler scheduler = new JobScheduler("127.0.0.1", 50000, dispatchPolicy);
        scheduler.newlines = argSet.contains("-n");
        scheduler.run();
    }
}
