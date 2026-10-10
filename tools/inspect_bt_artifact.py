"""Offline leads from a legally supplied VR30 APK, jar or vendor ELF. No execution."""
import argparse, hashlib, json, pathlib, re, zipfile

TERMS = re.compile(rb"bluetooth|bluez|ygomi|connexis|l2cap|rfcomm|ecutek|gatt|hci|dbus|socket|virtual.?hdt|ivi", re.I)

def inspect(name, data):
    strings = re.findall(rb"[\x20-\x7e]{5,}", data)
    leads = sorted(set(s[:1000].decode("ascii") for s in strings if TERMS.search(s)))
    return {"name":name,"bytes":len(data),"sha256":hashlib.sha256(data).hexdigest(),
            "leads":leads[:2000],"truncated":len(leads)>2000}

def main():
    p=argparse.ArgumentParser();p.add_argument("artifact",type=pathlib.Path);p.add_argument("--out",type=pathlib.Path)
    args=p.parse_args();results=[]
    if zipfile.is_zipfile(args.artifact):
        with zipfile.ZipFile(args.artifact) as z:
            for item in z.infolist():
                if item.filename.endswith((".dex",".so",".xml")) and item.file_size<=64*1024*1024:
                    results.append(inspect(item.filename,z.read(item)))
    else:
        if args.artifact.stat().st_size>64*1024*1024:raise ValueError("64 MiB input limit")
        results.append(inspect(args.artifact.name,args.artifact.read_bytes()))
    data={"artifact_sha256":hashlib.sha256(args.artifact.read_bytes()).hexdigest(),
          "warning":"Strings are leads only. No inferred UUID, PSM, command, transport or telemetry.","entries":results}
    output=json.dumps(data,ensure_ascii=False,indent=2)
    if args.out:args.out.write_text(output,encoding="utf-8")
    else:print(output)

if __name__=="__main__":main()
