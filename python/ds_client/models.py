
from __future__ import annotations
from typing import TypeVar, Type, Mapping, Any

from dataclasses import dataclass

@dataclass
class ServerType:
    # system.xml

    type: str
    limit: int
    bootup_time: int
    rate: float
    core_count: int
    memory: int
    disk: int

    T = TypeVar('T', bound='ServerType')

    @classmethod
    def from_xml_elem_attr_dict(cls: Type[T], data: Mapping[str, Any]) -> T:
        return cls(
            type=data['type'],
            limit=int(data['limit']),
            bootup_time=int(data['bootupTime']),
            rate=float(data['rate']),
            core_count=int(data['coreCount']),
            memory=int(data['memory']),
            disk=int(data['disk']),
        )

@dataclass
class Resource:
    # RESC

    type: str
    id: int
    state: int
    available_time: int
    core_count: int
    memory: int
    disk: int

    T = TypeVar('T', bound='Resource')

    @classmethod
    def from_received_line(cls: Type[T], line: str) -> T:
        parts = line.split()
        return cls(
            type=parts[0],
            id=int(parts[1]),
            state=int(parts[2]),
            available_time=int(parts[3]),
            core_count=int(parts[4]),
            memory=int(parts[5]),
            disk=int(parts[6]),
        )

@dataclass
class JobSubmission:
    # JOBN

    submit_time: int
    id: int
    estimated_runtime: int
    core_count: int
    memory: int
    disk: int

    T = TypeVar('T', bound='JobSubmission')

    @classmethod
    def from_received_line(cls: Type[T], line: str) -> T:
        parts = line.split()
        if parts[0] != 'JOBN':
            raise ValueError(line)

        return cls(
            submit_time=int(parts[1]),
            id=int(parts[2]),
            estimated_runtime=int(parts[3]),
            core_count=int(parts[4]),
            memory=int(parts[5]),
            disk=int(parts[6]),
        )
