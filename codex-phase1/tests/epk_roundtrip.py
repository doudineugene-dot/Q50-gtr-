"""Verify reused EPK tooling with a disposable test key; never access OBU private keys."""
import importlib.util
from pathlib import Path
import tempfile
from argparse import Namespace
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization

spec = importlib.util.spec_from_file_location('epktool', Path(__file__).resolve().parents[2] / 'tools/epktool.py')
epk = importlib.util.module_from_spec(spec)
spec.loader.exec_module(epk)
with tempfile.TemporaryDirectory() as tmp:
    root = Path(tmp)
    key = rsa.generate_private_key(public_exponent=65537, key_size=1024)
    cert = root / 'test-public.pem'
    cert.write_bytes(key.public_key().public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo))
    payload = bytes(range(256)) * 17
    apk = root / 'test.apk'
    apk.write_bytes(payload)
    out = root / 'test.epk'
    epk.cmd_build(Namespace(cert=str(cert), inputs=[str(apk)], output=str(out), type=2, name='q50codex.apk'))
    data = out.read_bytes()
    envelope = epk.parse_envelope(data)
    from cryptography.hazmat.primitives.asymmetric import padding
    session = key.decrypt(envelope['key'], padding.PKCS1v15())
    blocks = list(epk.iter_blocks(data))
    assert len(blocks) == 1 and blocks[0][0] == 'q50codex.apk'
    assert epk.aes_decrypt(blocks[0][2], session) == payload
    assert envelope['version'] == 2 and envelope['key_size'] == 128
    print('PASS: RSA-1024 / AES-256-CBC EPK roundtrip recovers exact payload')
