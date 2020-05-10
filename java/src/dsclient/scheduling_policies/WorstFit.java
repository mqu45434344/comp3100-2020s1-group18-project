package dsclient.scheduling_policies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import dsclient.JobDispatchPolicy;
import dsclient.JobScheduler;
import dsclient.enums.ServerState;
import dsclient.models.JobSubmission;
import dsclient.models.Resource;

public class WorstFit implements JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException {
        while (true) {
            String received = scheduler.inquire("REDY");
            if (received.equals("NONE")) {
                break;
            }

            JobSubmission job = JobSubmission.fromReceivedLine(received);

            List<Resource> resources = scheduler.fetchCapableResources(job);
            List<Resource> sufficient = resources.stream().filter(
                res -> res.coreCount >= job.coreCount
                && res.disk >= job.disk
                && res.memory >= job.memory
            ).collect(Collectors.toList());

            Resource worst = null;
            if (sufficient.isEmpty()) {
                List<Resource> reversed = new ArrayList<>(resources);
                Collections.reverse(reversed);
                worst = reversed.stream().min(
                    Comparator.comparingInt(res -> res.id)
                ).get();
            } else {
                worst = sufficient.stream().max(
                    Comparator.<Resource>comparingInt(
                            res -> (
                                    res.state == ServerState.IDLE
                                    || res.state == ServerState.ACTIVE
                                ) ? 1 : 0
                            )
                            .thenComparingInt(res -> res.coreCount - job.coreCount)
                ).get();
            }

            scheduler.schedule(job, worst);
        }
    }
}
