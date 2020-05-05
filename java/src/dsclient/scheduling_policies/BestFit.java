package dsclient.scheduling_policies;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import dsclient.JobDispatchPolicy;
import dsclient.JobScheduler;
import dsclient.models.JobSubmission;
import dsclient.models.Resource;

public class BestFit implements JobDispatchPolicy {
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

            Resource best = sufficient.stream().min(
                Comparator.<Resource>comparingInt(res -> res.coreCount - job.coreCount)
                        .thenComparingInt(res -> res.availableTime)
            ).orElse(resources.get(0));

            scheduler.schedule(job, best);
        }
    }
}
