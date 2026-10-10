"""Offline Bluetooth framing analysis, never an EcuTek telemetry decoder.

Input: hci-packets.jsonl from the native RAW observer. Standard HCI/ACL/L2CAP
fields only (Bluetooth Core Vol 3 Part A, Vol 4 Part E). No guessed PSM/UUID.
Direction 1 = received by controller, 0 = sent; -1 remains UNKNOWN.
"""
import argparse
import collections
import json
import struct


class Analyzer:
    def __init__(self):
        self.fragments = {}
        self.links = []
        self.channels = []
        self.errors = []
        self.traffic = collections.defaultdict(lambda: [0, 0, None, None])

    def packet(self, row):
        try:
            p = bytes.fromhex(row["hex"])
            dev, direction, ms = int(row["dev"]), int(row["dir"]), int(row["ms"])
        except (ValueError, KeyError, TypeError) as e:
            self.errors.append("invalid record: " + str(e))
            return
        if row.get("truncated") or not p:
            self.errors.append("truncated/empty HCI packet")
            return
        if p[0] == 4:
            if len(p) < 3 or len(p) != p[2] + 3:
                self.errors.append("invalid HCI event length")
                return
            if p[1] == 0x3e and len(p) >= 22 and p[3] == 1:
                self.links.append(dict(dev=dev, ms=ms, kind="LE_CONNECTION_COMPLETE",
                    status=p[4], handle=int.from_bytes(p[5:7], "little"),
                    remote=":".join(f"{b:02X}" for b in p[9:15][::-1]),
                    address_type=p[8], evi_attribution="UNKNOWN"))
            elif p[1] == 5 and len(p) == 7:
                self.links.append(dict(dev=dev, ms=ms, kind="DISCONNECTION_COMPLETE",
                    status=p[3], handle=int.from_bytes(p[4:6], "little"), reason=p[6]))
            return
        if p[0] != 2:
            return
        if len(p) < 5:
            self.errors.append("short ACL header")
            return
        hf, size = struct.unpack_from("<HH", p, 1)
        if len(p) != size + 5:
            self.errors.append("invalid ACL length")
            return
        handle, boundary = hf & 0xfff, (hf >> 12) & 3
        key = dev, direction, handle
        if boundary in (0, 2):
            if key in self.fragments:
                self.errors.append("incomplete ACL replaced by new start")
            self.fragments[key] = bytearray(p[5:])
        elif boundary == 1 and key in self.fragments:
            self.fragments[key].extend(p[5:])
        else:
            self.errors.append("orphan/unsupported ACL fragment")
            return
        data = self.fragments[key]
        if len(data) < 4:
            return
        length, cid = struct.unpack_from("<HH", data)
        if length > 65531 or len(data) > length + 4:
            self.errors.append("invalid L2CAP size")
            del self.fragments[key]
            return
        if len(data) != length + 4:
            return
        del self.fragments[key]
        stats = self.traffic[key + (cid,)]
        stats[0] += length
        stats[1] += 1
        if stats[2] is None:
            stats[2] = ms
        stats[3] = ms
        if cid != 5:  # LE signalling fixed CID, not a vendor/EcuTek assumption.
            return
        offset = 4
        while offset < len(data):
            if len(data) - offset < 4:
                self.errors.append("short LE signalling header")
                break
            code, ident, n = struct.unpack_from("<BBH", data, offset)
            offset += 4
            if n > len(data) - offset:
                self.errors.append("invalid LE signalling command length")
                break
            payload = data[offset:offset+n]
            offset += n
            base = dict(dev=dev, direction=direction, handle=handle, ms=ms,
                        identifier=ident, evi_attribution="UNKNOWN")
            if code == 0x14 and n == 10:
                psm, scid, mtu, mps, credits = struct.unpack("<HHHHH", payload)
                self.channels.append(dict(base, kind="LE_CREDIT_CONNECTION_REQUEST",
                    psm=psm, scid=scid, mtu=mtu, mps=mps, credits=credits,
                    connected="UNPROVEN_REQUEST_ONLY"))
            elif code == 0x15 and n == 10:
                dcid, mtu, mps, credits, result = struct.unpack("<HHHHH", payload)
                self.channels.append(dict(base, kind="LE_CREDIT_CONNECTION_RESPONSE",
                    dcid=dcid, mtu=mtu, mps=mps, credits=credits, result=result))

    def report(self):
        traffic = []
        for (dev, direction, handle, cid), (size, count, first, last) in sorted(self.traffic.items()):
            elapsed = (last-first)/1000
            traffic.append(dict(dev=dev, direction=direction, handle=handle, cid=cid,
                l2cap_payload_bytes=size, l2cap_pdus=count, first_ms=first, last_ms=last,
                bytes_per_sec=size/elapsed if elapsed > 0 else None,
                rate_window="first to last captured PDU, not entire run",
                evi_attribution="UNKNOWN"))
        return dict(
            kind="PASSIVE_BLUETOOTH_FRAMING_ONLY", evi_discovered="UNKNOWN", evi_connected="UNKNOWN",
            ecutek_rpm="UNAVAILABLE", ecutek_boost_actual="UNAVAILABLE",
            links=self.links, le_coc_signalling=self.channels, generic_traffic=traffic,
            incomplete_acl_streams=len(self.fragments), errors=self.errors)


def analyze(lines):
    analyzer = Analyzer()
    for index, line in enumerate(lines, 1):
        try:
            row = json.loads(line)
            if not isinstance(row, dict):
                raise ValueError("record must be an object")
            analyzer.packet(row)
        except (ValueError, TypeError) as e:
            analyzer.errors.append(f"line {index}: {e}")
    return analyzer.report()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture")
    args = parser.parse_args()
    with open(args.capture, encoding="utf-8") as stream:
        print(json.dumps(analyze(stream), ensure_ascii=False, indent=2))
