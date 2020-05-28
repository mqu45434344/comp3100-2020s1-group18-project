
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from ..job_scheduler import JobScheduler

from ..job_dispatch_policy import JobDispatchPolicy
from ..models import JobSubmission
from ..enums import ServerState

class MinimalCost(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
        server_types = sorted(scheduler.servers, key=lambda srv: srv.rate)
        server_order = {srv.type: i for i, srv in enumerate(server_types)}

        while True:
            received = scheduler.inquire('REDY')
            if received == 'NONE':
                break

            job = JobSubmission.from_received_line(received)

            resources = scheduler.fetch_capable_resources(job)

            best = max(
                resources,
                key=lambda res: (
                    -server_order[res.type],
                    res.state == ServerState.IDLE,
                    res.state == ServerState.ACTIVE,
                ),
            )

            scheduler.schedule(job, best)
