
from __future__ import annotations
from typing import TYPE_CHECKING, Optional, List
if TYPE_CHECKING:
    from .job_dispatch_policy import JobDispatchPolicy
    from .models import JobSubmission

import socket
from getpass import getuser
from xml.etree import ElementTree

from .models import ServerType, Resource
from .scheduling_policies import AllToLargest


class JobScheduler:
    @staticmethod
    def parse_for_server_types(filename: str) -> List[ServerType]:
        tree = ElementTree.parse(filename)
        root = tree.getroot()
        servers = []
        for elem in root.findall('servers/server'):
            server = ServerType.from_xml_elem_attr_dict(elem.attrib)
            servers.append(server)
        return servers

    def __init__(self,
        address: str,
        port: int,
        dispatch_policy: Optional[JobDispatchPolicy] = None,
    ) -> None:
        self._sock = sock = socket.socket()
        sock.connect((address, port))
        sock.settimeout(2)

        if dispatch_policy is None:
            dispatch_policy = AllToLargest()
        self.dispatch_policy = dispatch_policy

        self.servers: List[ServerType] = []

        self.newlines = False

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

    def read_system_xml(self, filename: str = 'system.xml') -> None:
        self.servers.extend(self.parse_for_server_types(filename))

    def run(self) -> None:
        self.inquire('HELO')
        self.inquire('AUTH ' + getuser())
        self.read_system_xml()

        self.dispatch_policy.dispatch(self)

        self.inquire("QUIT")
        self.close()

    # High-level API methods #

    def schedule(self, job: JobSubmission, res: Resource) -> None:
        recieved = self.inquire(f"SCHD {job.id} {res.type} {res.id}")
        if recieved.startswith('ERR'):
            raise Exception(recieved)

    def fetch_capable_resources(self, job: JobSubmission) -> List[Resource]:
        self.inquire("RESC Capable {0.core_count} {0.memory} {0.disk}".format(job))
        resources = []
        while True:
            received = self.inquire('OK')
            if received == '.':
                break
            res = Resource.from_received_line(received)
            resources.append(res)
        return resources
