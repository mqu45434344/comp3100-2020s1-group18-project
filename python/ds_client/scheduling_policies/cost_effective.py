
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from ..job_scheduler import JobScheduler

from ..job_dispatch_policy import JobDispatchPolicy
from ..models import JobSubmission
from ..enums import ServerState

class CostEffective(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
        while True:
            received = scheduler.inquire('REDY')
            if received == 'NONE':
                break

            job = JobSubmission.from_received_line(received)

            resources = scheduler.fetch_capable_resources(job)

            best = max(
                resources,
                key=lambda res: (
                    -(res.core_count - job.core_count),
                    res.state == ServerState.IDLE,
                    -res.available_time,
                ),
            )

            scheduler.schedule(job, best)
