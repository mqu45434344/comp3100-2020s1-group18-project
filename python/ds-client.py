#/usr/bin/env python3.7
from __future__ import annotations
from typing import TypeVar, Type, Optional, Mapping, Any, List

import sys
import socket
from getpass import getuser
from dataclasses import dataclass
from xml.etree import ElementTree as ET


@dataclass
class Server:
    # system.xml

    TServer = TypeVar('TServer', bound='Server')

    @classmethod
    def from_xml_elem_attr_dict(cls: Type[TServer], data: Mapping[str, Any]) -> TServer:
        return cls(
            type=data['type'],
            limit=int(data['limit']),
            bootup_time=int(data['bootupTime']),
            rate=float(data['rate']),
            core_count=int(data['coreCount']),
            memory=int(data['memory']),
            disk=int(data['disk']),
        )

    type: str
    limit: int
    bootup_time: int
    rate: float
    core_count: int
    memory: int
    disk: int

@dataclass
class JobSubmission:
    # JOBN

    TJobSubmission = TypeVar('TJobSubmission', bound='JobSubmission')

    @classmethod
    def from_received_line(cls: Type[TJobSubmission], line: str) -> TJobSubmission:
        parts = line.split()
        if parts[0] != 'JOBN':
            raise ValueError(line)

        return cls(
            submit_time=int(parts[1]),
            job_id=int(parts[2]),
            estimated_runtime=int(parts[3]),
            core_count=int(parts[4]),
            memory=int(parts[5]),
            disk=int(parts[6]),
        )

    submit_time: int
    job_id: int
    estimated_runtime: int
    core_count: int
    memory: int
    disk: int


class JobDispatchPolicy:
    def dispatch(self, scheduler: JobScheduler) -> None:
        raise NotImplementedError

class AllToLargest(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
        largest_server_type = max(
            scheduler.servers, key=lambda srv: srv.core_count
        ).type

        while True:
            incoming = scheduler.inquire('REDY')
            if incoming == 'NONE':
                break

            jobn = JobSubmission.from_received_line(incoming)
            scheduler.inquire(f"SCHD {jobn.job_id} {largest_server_type} 0")


class JobScheduler:
    @staticmethod
    def parse_for_servers(filename: str) -> List[Server]:
        tree = ET.parse(filename)
        root = tree.getroot()
        servers = []
        for elem in root.findall('servers/server'):
            server = Server.from_xml_elem_attr_dict(elem.attrib)
            servers.append(server)
        return servers

    def __init__(self,
        address: str,
        port: int,
        *,
        dispatch_policy: Optional[JobDispatchPolicy] = None,
    ) -> None:
        self._sock = sock = socket.socket()
        sock.connect((address, port))
        sock.settimeout(1)

        self.newlines = False

        if dispatch_policy is None:
            dispatch_policy = AllToLargest()
        self.dispatch_policy = dispatch_policy

        self.servers: List[Server] = []

    def close(self) -> None:
        self._sock.close()

    def send(self, message: str) -> None:
        if self.newlines:
            message += '\n'
        data = message.encode()
        self._sock.send(data)

    def receive(self) -> str:
        data = self._sock.recv(4096)
        if self.newlines:
            data = data[:-1]
        return data.decode()

    def inquire(self, message: str) -> str:
        self.send(message)
        return self.receive()

    def read_servers(self) -> None:
        self.servers.extend(self.parse_for_servers('system.xml'))

    def run(self) -> None:
        self.inquire('HELO')
        self.inquire('AUTH ' + getuser())
        self.read_servers()

        self.dispatch_policy.dispatch(self)

        self.inquire("QUIT")
        self.close()


if __name__ == '__main__':
    args = sys.argv[1:]
    args_set = set(args)

    scheduler = JobScheduler('127.0.0.1', 50000)
    scheduler.newlines = '-n' in args_set

    if not sys.flags.interactive:
        scheduler.run()
