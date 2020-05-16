
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from ..job_scheduler import JobScheduler

from ..job_dispatch_policy import JobDispatchPolicy
from ..models import JobSubmission
from ..enums import ServerState

class WorstFit(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
        while True:
            received = scheduler.inquire('REDY')
            if received == 'NONE':
                break

            job = JobSubmission.from_received_line(received)

            resources = scheduler.fetch_capable_resources(job)
            sufficient = [
                res for res in resources
                if (
                    res.core_count >= job.core_count
                    and res.disk >= job.disk
                    and res.memory >= job.memory
                )
            ]

            worst = None
            if sufficient:
                worst = max(
                    sufficient,
                    key=lambda res: (
                        res.state in {ServerState.IDLE, ServerState.ACTIVE},
                        res.core_count - job.core_count,
                    ),
                )
            else:
                worst = next(res for res in reversed(resources) if res.id == 0)

            scheduler.schedule(job, worst)
