
from enum import IntEnum

class ServerState(IntEnum):
    INACTIVE = 0
    BOOTING = 1
    IDLE = 2
    ACTIVE = 3
    UNAVAILABLE = 4

class JobState(IntEnum):
    SUBMITTED = 0
    WAITING = 1
    RUNNING = 2
    SUSPENDED = 3
    COMPLETED = 4
    FAILED = 5
    KILLED = 6
