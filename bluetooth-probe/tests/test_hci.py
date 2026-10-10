import importlib.util
import json
import pathlib
import struct
import unittest

spec = importlib.util.spec_from_file_location("hci", pathlib.Path(__file__).parents[1]/"tools/analyze_hci.py")
hci = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hci)


def acl(data, boundary=2, direction=0, ms=100, handle=7):
    packet = b"\x02" + struct.pack("<HH", handle | (boundary << 12), len(data)) + data
    return json.dumps(dict(dev=0, dir=direction, ms=ms, hex=packet.hex()))


class FramingTests(unittest.TestCase):
    """Synthetic Bluetooth standards fixtures, not EcuTek protocol evidence."""
    def test_dynamic_psm_fragmentation_and_direction(self):
        for psm in (0x87, 0xb2):
            req = struct.pack("<BBHHHHHH", 0x14, 3, 10, psm, 0x41, 100, 64, 8)
            l2 = struct.pack("<HH", len(req), 5) + req
            result = hci.analyze([acl(l2[:6]), acl(l2[6:], boundary=1)])
            channel = result["le_coc_signalling"][0]
            self.assertEqual(channel["psm"], psm)
            self.assertEqual(channel["scid"], 0x41)
            self.assertEqual(channel["connected"], "UNPROVEN_REQUEST_ONLY")
            self.assertEqual(result["errors"], [])
            self.assertEqual(result["ecutek_rpm"], "UNAVAILABLE")

    def test_response_rejection_is_preserved(self):
        rsp = struct.pack("<BBHHHHHH", 0x15, 3, 10, 0, 0, 0, 0, 2)
        result = hci.analyze([acl(struct.pack("<HH", len(rsp), 5)+rsp, direction=1)])
        self.assertEqual(result["le_coc_signalling"][0]["result"], 2)
        self.assertEqual(result["evi_connected"], "UNKNOWN")

    def test_interleaved_handles_are_not_combined(self):
        data = struct.pack("<HH", 4, 0x42) + b"abcd"
        result = hci.analyze([acl(data[:5], handle=1), acl(data[:5], handle=2),
                              acl(data[5:], boundary=1, handle=1), acl(data[5:], boundary=1, handle=2)])
        self.assertEqual(len(result["generic_traffic"]), 2)

    def test_unrelated_acl_never_becomes_telemetry(self):
        data = struct.pack("<HH", 4, 0x41) + b"abcd"
        result = hci.analyze([acl(data, direction=-1), acl(data, direction=-1, ms=1100)])
        self.assertEqual(result["generic_traffic"][0]["bytes_per_sec"], 8)
        self.assertEqual(result["generic_traffic"][0]["direction"], -1)
        self.assertEqual(result["ecutek_boost_actual"], "UNAVAILABLE")

    def test_truncation_orphan_and_malformed_are_errors(self):
        malformed = ["not json", "[]", acl(b"abc", boundary=1), json.dumps(dict(dev=0,dir=1,ms=1,hex="020020ff00")),
                     json.dumps(dict(dev=0,dir=1,ms=1,hex="04",truncated=True))]
        result = hci.analyze(malformed)
        self.assertEqual(len(result["errors"]), 5)
        self.assertEqual(result["le_coc_signalling"], [])

    def test_incomplete_capture_retains_unknown(self):
        result = hci.analyze([acl(struct.pack("<HH", 20, 5)+b"ab")])
        self.assertEqual(result["incomplete_acl_streams"], 1)
        self.assertEqual(result["evi_discovered"], "UNKNOWN")

    def test_le_connection_and_disconnect_event(self):
        payload = bytes([1, 0])+struct.pack("<H", 7)+bytes([0, 1])+bytes.fromhex("010203040506")+b"\x00"*7
        event = bytes([4, 0x3e, len(payload)])+payload
        disc = bytes([4,5,4,0,7,0,0x13])
        rows = [json.dumps(dict(dev=0,dir=1,ms=100,hex=p.hex())) for p in (event, disc)]
        result = hci.analyze(rows)
        self.assertEqual(result["links"][0]["remote"], "06:05:04:03:02:01")
        self.assertEqual(result["links"][1]["reason"], 0x13)
        self.assertEqual(result["evi_connected"], "UNKNOWN")


if __name__ == "__main__":
    unittest.main()
