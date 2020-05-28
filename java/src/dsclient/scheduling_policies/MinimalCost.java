package dsclient.scheduling_policies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dsclient.JobDispatchPolicy;
import dsclient.JobScheduler;
import dsclient.enums.ServerState;
import dsclient.models.JobSubmission;
import dsclient.models.Resource;
import dsclient.models.ServerType;

public class MinimalCost implements JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException {
        List<ServerType> serverTypes = new ArrayList<>(scheduler.servers);
        Collections.sort(serverTypes, Comparator.comparingDouble(srv -> srv.rate));
        Map<String, Integer> serverOrder = IntStream
                .range(0, serverTypes.size())
                .boxed()
                .collect(Collectors.<Integer, String, Integer>toMap(
                    i -> serverTypes.get(i).type,
                    i -> i
                )
        );

        while (true) {
            String received = scheduler.inquire("REDY");
            if (received.equals("NONE")) {
                break;
            }

            JobSubmission job = JobSubmission.fromReceivedLine(received);

            List<Resource> resources = scheduler.fetchCapableResources(job);

            Resource best = resources.stream().max(
                Comparator.<Resource>comparingInt(
                    res -> -serverOrder.get(res.type)
                ).thenComparingInt(
                    res -> (res.state == ServerState.IDLE) ? 1 : 0
                ).thenComparingInt(
                    res -> (res.state == ServerState.ACTIVE) ? 1 : 0
                )
            ).get();

            scheduler.schedule(job, best);
        }
    }
}
