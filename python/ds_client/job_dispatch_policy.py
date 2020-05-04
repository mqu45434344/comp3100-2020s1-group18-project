
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from .job_scheduler import JobScheduler

class JobDispatchPolicy:
    def dispatch(self, scheduler: JobScheduler) -> None:
        raise NotImplementedError
