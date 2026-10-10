"""Read GitHub Actions results using existing git authentication; never print credentials."""
import argparse, json, pathlib, subprocess, urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
REPO = "doudineugene-dot/Q50-gtr-"

def credential():
    p = subprocess.run(["git", "-c", "safe.directory=" + ROOT.as_posix(), "credential", "fill"],
        input="protocol=https\nhost=github.com\n\n", text=True, capture_output=True, check=True)
    values = dict(line.split("=", 1) for line in p.stdout.splitlines() if "=" in line)
    return values["password"]

def request(path, token):
    req = urllib.request.Request("https://api.github.com/repos/" + REPO + path,
        headers={"Accept": "application/vnd.github+json", "User-Agent": "Q50-recon"})
    # GitHub redirects logs/artifacts to signed storage URLs; never forward the token.
    req.add_unredirected_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req, timeout=60) as response:
        return response.read()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["runs", "jobs", "artifacts", "download", "logs"])
    parser.add_argument("--id", type=int)
    args = parser.parse_args(); token = credential()
    if args.mode == "runs":
        data = json.loads(request("/actions/workflows/codex-phase1.yml/runs?branch=codex%2Fphase1-from-zero&per_page=5", token))
        for run in data["workflow_runs"]:
            print(json.dumps({k:run[k] for k in ("id", "head_sha", "status", "conclusion", "html_url")}, ensure_ascii=False))
    elif args.mode in ("jobs", "artifacts"):
        data = json.loads(request(f"/actions/runs/{args.id}/{args.mode}", token))
        for item in data[args.mode]:
            keys = ("id", "name", "status", "conclusion", "steps") if args.mode == "jobs" else ("id", "name", "size_in_bytes", "expired")
            print(json.dumps({k:item.get(k) for k in keys}, ensure_ascii=False))
    else:
        suffix = f"/actions/artifacts/{args.id}/zip" if args.mode == "download" else f"/actions/jobs/{args.id}/logs"
        out = ROOT / "artifact"; out.mkdir(exist_ok=True)
        target = out / (f"github-{args.id}.zip" if args.mode == "download" else f"job-{args.id}.txt")
        target.write_bytes(request(suffix, token)); print(target)

if __name__ == "__main__":
    main()
