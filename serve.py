from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import os


ROOT = Path(__file__).resolve().parent
os.chdir(ROOT)


class LocalOnlyServer(ThreadingHTTPServer):
    allow_reuse_address = True


if __name__ == "__main__":
    address = ("127.0.0.1", 8765)
    print(f"Personal Finance Planner: http://{address[0]}:{address[1]}")
    print("Press Ctrl+C to stop.")
    LocalOnlyServer(address, SimpleHTTPRequestHandler).serve_forever()
