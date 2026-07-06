#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generate and simulate test smart bracelets for the Smart Watch Monitoring System.

The script intentionally uses only Python standard library modules so it can run
on a server or a clean Windows workstation without installing packages.
"""

from __future__ import annotations

import argparse
import getpass
import http.cookiejar
import json
import math
import os
import queue
import random
import socket
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Dict, Iterable, List, Optional, Sequence, Tuple


DEFAULT_HOST = "8.156.83.206"
DEFAULT_API_PORT = 8080
DEFAULT_TCP_PORT = 9000
DEFAULT_COUNT = 10
DEFAULT_IMEI_START = 359999000000001
DEFAULT_PREFIX = "测试手环-"
DEFAULT_LATITUDE = 30.886874
DEFAULT_LONGITUDE = 103.594558
DEFAULT_RADIUS_METERS = 30.0
DEFAULT_INTERVAL_SECONDS = 30
DEFAULT_DURATION_SECONDS = 600
DEFAULT_USERNAME = "admin"
DEFAULT_DOWNLINK_DELAY_SECONDS = 1.5
DEFAULT_DOWNLINK_RETRIES = 3
DEFAULT_API_RETRY_DELAY_SECONDS = 2.0
DEFAULT_API_RETRIES = 5
DEFAULT_HEART_RATE = 78
DEFAULT_BLOOD_DIASTOLIC = 75
DEFAULT_BLOOD_SYSTOLIC = 122
DEFAULT_SPO2 = 98
DEFAULT_BODY_TEMP = 36.5
DEFAULT_WRIST_TEMP = 34.0
DEFAULT_BLUETOOTH_PAYLOAD = "TraxBean064|BF:0C:B8:3F:2F:37|-30&VG05|F0:49:32:83:F9:9E|-52"
DEFAULT_BLUETOOTH_GATEWAY = "28:05:31:16:02:04"
RANDOM_LOCATION_RADIUS_RANGE = (5.0, 120.0)
RANDOM_LOCATION_CENTER_JITTER_METERS = 80.0
RANDOM_INTERVAL_RANGE = (10, 120)
RANDOM_DURATION_RANGE = (120, 1800)
RANDOM_BATTERY_RANGE = (20, 100)
RANDOM_HEART_RATE_RANGE = (55, 115)
RANDOM_BLOOD_DIASTOLIC_RANGE = (60, 95)
RANDOM_BLOOD_PRESSURE_GAP_RANGE = (25, 65)
RANDOM_SPO2_RANGE = (92, 100)
RANDOM_BODY_TEMP_RANGE = (35.5, 38.2)
RANDOM_WRIST_TEMP_RANGE = (32.0, 36.0)
RANDOM_DOWNLINK_DELAY_RANGE = (1.0, 3.0)
RANDOM_DOWNLINK_RETRIES_RANGE = (2, 6)

SCENARIOS = ("basic", "location", "health", "alarm", "downlink", "all")
LOG_SINK = None


class ScriptError(Exception):
    """Raised for expected command failures with a clean user-facing message."""


class RateLimitError(ScriptError):
    """Raised when the server rejects a request because of rate limiting."""


def log(message: str) -> None:
    now = datetime.now().strftime("%H:%M:%S")
    line = f"[{now}] {message}"
    print(line, flush=True)
    if LOG_SINK is not None:
        try:
            LOG_SINK(line)
        except Exception:
            pass


def set_log_sink(sink: Any) -> None:
    global LOG_SINK
    LOG_SINK = sink


def normalize_base_url(host: str, api_port: int) -> str:
    if host.startswith("http://") or host.startswith("https://"):
        parsed = urllib.parse.urlparse(host)
        if parsed.netloc and parsed.port is not None:
            return host.rstrip("/")
        scheme = parsed.scheme
        hostname = parsed.hostname or parsed.netloc
        return f"{scheme}://{hostname}:{api_port}"
    return f"http://{host}:{api_port}"


def unwrap_response(value: Any) -> Any:
    """Support both raw controller responses and ApiResponse-like wrappers."""
    if isinstance(value, dict) and "data" in value and (
        "code" in value or "message" in value or "timestamp" in value
    ):
        return value.get("data")
    return value


def page_items(value: Any) -> List[Dict[str, Any]]:
    value = unwrap_response(value)
    if isinstance(value, dict):
        content = value.get("content")
        if isinstance(content, list):
            return [item for item in content if isinstance(item, dict)]
        data = value.get("data")
        if isinstance(data, dict) and isinstance(data.get("content"), list):
            return [item for item in data["content"] if isinstance(item, dict)]
    if isinstance(value, list):
        return [item for item in value if isinstance(item, dict)]
    return []


def extract_id(value: Any) -> Optional[int]:
    value = unwrap_response(value)
    if isinstance(value, int):
        return value
    if isinstance(value, str) and value.isdigit():
        return int(value)
    if isinstance(value, dict):
        raw = value.get("id")
        if isinstance(raw, int):
            return raw
        if isinstance(raw, str) and raw.isdigit():
            return int(raw)
    return None


class ApiClient:
    def __init__(
        self,
        host: str,
        api_port: int,
        timeout: float = 15.0,
        rate_limit_delay: float = DEFAULT_API_RETRY_DELAY_SECONDS,
        rate_limit_retries: int = DEFAULT_API_RETRIES,
    ) -> None:
        self.base_url = normalize_base_url(host, api_port)
        self.timeout = timeout
        self.rate_limit_delay = rate_limit_delay
        self.rate_limit_retries = rate_limit_retries
        self.cookie_jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookie_jar))

    def request(
        self,
        method: str,
        path: str,
        body: Optional[Dict[str, Any]] = None,
        query: Optional[Dict[str, Any]] = None,
        allow_404: bool = False,
    ) -> Any:
        url = self.base_url + path
        if query:
            clean_query = {k: v for k, v in query.items() if v is not None}
            url += "?" + urllib.parse.urlencode(clean_query)

        data = None
        headers = {"Accept": "application/json"}
        if body is not None:
            data = json.dumps(body, ensure_ascii=False).encode("utf-8")
            headers["Content-Type"] = "application/json; charset=utf-8"

        for attempt in range(self.rate_limit_retries + 1):
            req = urllib.request.Request(url, data=data, headers=headers, method=method.upper())
            try:
                with self.opener.open(req, timeout=self.timeout) as resp:
                    raw = resp.read()
                    if not raw:
                        return None
                    text = raw.decode("utf-8", errors="replace")
                    try:
                        return json.loads(text)
                    except json.JSONDecodeError:
                        return text
            except urllib.error.HTTPError as exc:
                if allow_404 and exc.code == 404:
                    return None
                text = exc.read().decode("utf-8", errors="replace")
                if exc.code == 429:
                    if attempt < self.rate_limit_retries:
                        wait_seconds = max(self.rate_limit_delay, 0.5) * (2 ** attempt)
                        log(f"API rate limited: {method.upper()} {path}; retrying in {wait_seconds:.1f}s")
                        time.sleep(wait_seconds)
                        continue
                    raise RateLimitError(f"HTTP 429 {method.upper()} {url}: {text}") from exc
                raise ScriptError(f"HTTP {exc.code} {method.upper()} {url}: {text}") from exc
            except urllib.error.URLError as exc:
                raise ScriptError(f"Failed to connect API {url}: {exc.reason}") from exc
        raise RateLimitError(f"HTTP 429 {method.upper()} {url}: retries exhausted")

    def login(self, username: str, password: str) -> None:
        self.request("POST", "/api/auth/login", {"username": username, "password": password})
        me = self.request("GET", "/api/auth/me")
        user = unwrap_response(me)
        if not isinstance(user, dict) or not user.get("username"):
            raise ScriptError("Login succeeded but /api/auth/me did not return a user.")
        log(f"API login ok: {user.get('username')}")

    def get_device_by_imei(self, imei: str) -> Optional[Dict[str, Any]]:
        encoded = urllib.parse.quote(imei, safe="")
        result = self.request("GET", f"/api/devices/by-imei/{encoded}", allow_404=True)
        result = unwrap_response(result)
        return result if isinstance(result, dict) else None

    def list_devices(self, search: Optional[str] = None, limit: int = 100, offset: int = 0) -> List[Dict[str, Any]]:
        return page_items(self.request("GET", "/api/devices", query={"limit": limit, "offset": offset, "search": search}))

    def create_device(self, imei: str, index: int) -> int:
        existing = self.get_device_by_imei(imei)
        if existing and extract_id(existing):
            device_id = extract_id(existing)
            log(f"Device exists: imei={imei}, id={device_id}")
            return int(device_id)

        body = {
            "imei": imei,
            "deviceModel": "TEST-IW",
            "mcc": "460",
            "mnc": "04",
            "apn": "CMNET",
            "iccid": f"89860458{index:012d}",
            "imsi": f"46004{index:010d}",
            "firmwareVersion": "test-v1",
            "hardwareVersion": "sim",
            "status": "inactive",
        }
        result = self.request("POST", "/api/devices", body)
        device_id = extract_id(result)
        if device_id is None:
            existing = self.get_device_by_imei(imei)
            device_id = extract_id(existing)
        if device_id is None:
            raise ScriptError(f"Could not determine created device id for {imei}: {result!r}")
        log(f"Created device: imei={imei}, id={device_id}")
        return int(device_id)

    def list_patients(self, search: Optional[str] = None, limit: int = 100, offset: int = 0) -> List[Dict[str, Any]]:
        return page_items(self.request("GET", "/api/patients", query={"limit": limit, "offset": offset, "search": search}))

    def find_patient_by_name(self, name: str) -> Optional[Dict[str, Any]]:
        for item in self.list_patients(search=name, limit=100):
            if item.get("name") == name:
                return item
        return None

    def create_patient(self, name: str, index: int) -> int:
        existing = self.find_patient_by_name(name)
        if existing and extract_id(existing):
            patient_id = int(extract_id(existing))
            log(f"Patient exists: name={name}, id={patient_id}")
            return patient_id

        body = {
            "name": name,
            "gender": "男" if index % 2 else "女",
            "age": 60 + (index % 20),
            "idCard": f"510923199001{index % 28 + 1:02d}{index % 10000:04d}",
            "ward": "测试病区",
            "bed": f"T{index:03d}",
            "phone": f"139{index % 100000000:08d}",
            "diagnosis": "测试数据",
            "status": "admitted",
        }
        result = self.request("POST", "/api/patients", body)
        patient_id = extract_id(result)
        if patient_id is None:
            raise ScriptError(f"Could not determine created patient id for {name}: {result!r}")
        log(f"Created patient: name={name}, id={patient_id}")
        return int(patient_id)

    def bind_patient_device(self, patient_id: int, device_id: int) -> None:
        self.request(
            "POST",
            "/api/patient-devices",
            {"patientId": patient_id, "deviceId": device_id, "relationship": "wearing"},
        )
        log(f"Bound patient/device: patientId={patient_id}, deviceId={device_id}")

    def list_bindings_by_patient(self, patient_id: int) -> List[Dict[str, Any]]:
        return page_items(self.request("GET", f"/api/patient-devices/by-patient/{patient_id}", allow_404=True))

    def list_bindings_by_device(self, device_id: int) -> List[Dict[str, Any]]:
        return page_items(self.request("GET", f"/api/patient-devices/by-device/{device_id}", allow_404=True))

    def unbind(self, patient_id: int, device_id: int) -> None:
        self.request(
            "DELETE",
            "/api/patient-devices",
            query={"patientId": patient_id, "deviceId": device_id},
            allow_404=True,
        )

    def delete_patient(self, patient_id: int) -> None:
        self.request("DELETE", f"/api/patients/{patient_id}", allow_404=True)

    def delete_device(self, device_id: int) -> None:
        self.request("DELETE", f"/api/devices/{device_id}", allow_404=True)

    def downlink(self, endpoint: str, imei: str, **query: Any) -> Any:
        query = {"imei": imei, **query}
        return self.request("POST", f"/api/downlink/{endpoint}", query=query)

    def downlink_body(self, endpoint: str, body: Dict[str, Any]) -> Any:
        return self.request("POST", f"/api/downlink/{endpoint}", body=body)


def imei_for_index(start: int, index: int) -> str:
    return str(start + index)


def random_point(lat: float, lng: float, radius_meters: float, rng: random.Random) -> Tuple[float, float]:
    angle = rng.uniform(0, math.tau)
    distance = radius_meters * math.sqrt(rng.random())
    delta_lat = (distance * math.cos(angle)) / 111_320.0
    lng_scale = max(math.cos(math.radians(lat)), 0.01)
    delta_lng = (distance * math.sin(angle)) / (111_320.0 * lng_scale)
    return lat + delta_lat, lng + delta_lng


def decimal_to_nmea(value: float, is_latitude: bool) -> str:
    direction = "N" if is_latitude and value >= 0 else "S" if is_latitude else "E" if value >= 0 else "W"
    abs_value = abs(value)
    degrees = int(abs_value)
    minutes = (abs_value - degrees) * 60.0
    if is_latitude:
        return f"{degrees:02d}{minutes:07.4f}{direction}"
    return f"{degrees:03d}{minutes:07.4f}{direction}"


def status_block(
    battery: int,
    gsm: int = 59,
    satellites: int = 0,
    reserve: int = 0,
    defense: int = 0,
    work_mode: int = 8,
) -> str:
    battery = max(0, min(int(battery), 100))
    return f"{gsm:03d}{satellites:03d}{battery:03d}{reserve:d}{defense:02d}{work_mode:02d}"


def utc_packet_date(now: Optional[datetime] = None) -> str:
    now = now or datetime.now(timezone.utc)
    return now.strftime("%y%m%d")


def utc_packet_time(now: Optional[datetime] = None) -> str:
    now = now or datetime.now(timezone.utc)
    return now.strftime("%H%M%S")


def wifi_payload(seed: int = 1) -> str:
    base = [
        ("AP1", "d2:82:3d:a7:e2:ce", -45),
        ("AP2", "e2:2e:0b:83:d5:5b", -55),
        ("AP3", "22:82:3d:a7:e2:b3", -62),
        ("AP4", "06:05:88:1e:6d:ba", -63),
        ("AP5", "52:74:8d:c7:73:a6", -64),
    ]
    offset = seed % 10
    return "&".join(f"{ssid}|{mac}|{rssi - offset}" for ssid, mac, rssi in base)


def build_ap00(imei: str, index: int = 1) -> str:
    iccid = f"89860458{index:012d}"
    imsi = f"46004{index:010d}"
    return f"IWAP00{imei},{iccid},{imsi}#"


def build_ap01_network_location(
    lat: float,
    lng: float,
    battery: int = 80,
    seed: int = 1,
    now: Optional[datetime] = None,
) -> str:
    now = now or datetime.now(timezone.utc)
    return (
        f"IWAP01{utc_packet_date(now)}"
        f"V0000.0000N00000.0000E"
        f"000.0{utc_packet_time(now)}000.00"
        f"{status_block(battery=battery, gsm=59, satellites=0, work_mode=8)},"
        f"460,04,32768,236380872,"
        f"{wifi_payload(seed)},"
        f"[{lat:.6f}@{lng:.6f}]#"
    )


def build_ap01_gps(
    lat: float,
    lng: float,
    battery: int = 80,
    speed: float = 0.0,
    course: float = 0.0,
    now: Optional[datetime] = None,
) -> str:
    now = now or datetime.now(timezone.utc)
    return (
        f"IWAP01{utc_packet_date(now)}"
        f"A{decimal_to_nmea(lat, True)}{decimal_to_nmea(lng, False)}"
        f"{speed:05.1f}{utc_packet_time(now)}{course:06.2f}"
        f"{status_block(battery=battery, gsm=60, satellites=9, work_mode=8)},"
        f"460,04,32768,236380872,"
        f"{wifi_payload(0)}#"
    )


def build_ap03(
    battery: int = 80,
    steps: int = 0,
    roll_count: int = 0,
    work_mode: int = 8,
    interval: int = DEFAULT_INTERVAL_SECONDS,
) -> str:
    return f"IWAP03,{status_block(battery=battery, gsm=59, satellites=0, work_mode=work_mode)},{steps},{roll_count},{work_mode},{interval}#"


def build_ap04(imei: str, battery: int = 15) -> str:
    return f"IWAP04{imei},{battery:03d},{datetime.now().strftime('%Y%m%d%H%M%S')}#"


def build_ap10(imei: str, lat: float, lng: float, battery: int = 70, alarm_type: str = "SOS") -> str:
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    return f"IWAP10{alarm_type},{imei},{timestamp},{lat:.6f},{lng:.6f},0,0,0,{battery:03d}#"


def build_apjk(health_type: int, value: str, now: Optional[datetime] = None) -> str:
    now = now or datetime.now()
    return f"IWAPJK,{now.strftime('%Y-%m-%d %H:%M:%S')},{health_type},{value}#"


def build_aptp(body_temp: float = 36.5, wrist_temp: float = 34.2) -> str:
    return f"IWAPTP,{body_temp:.1f},{wrist_temp:.1f}#"


def build_apwr(imei: str, worn: bool = True) -> str:
    timestamp_ms = int(time.time() * 1000)
    return f"IWAPWR,{imei},{1 if worn else 0},{timestamp_ms}#"


def build_apbl(
    imei: str,
    bluetooth_payload: str = DEFAULT_BLUETOOTH_PAYLOAD,
    gateway_mac: str = DEFAULT_BLUETOOTH_GATEWAY,
) -> str:
    timestamp_ms = int(time.time() * 1000)
    payload = bluetooth_payload.strip() or DEFAULT_BLUETOOTH_PAYLOAD
    gateway = gateway_mac.strip() or DEFAULT_BLUETOOTH_GATEWAY
    return f"IWAPBL,{imei},{payload},{gateway},{timestamp_ms}#"


def build_ap16_ack(seq: str) -> str:
    return f"IWAP16,{seq}#"


def split_frames(buffer: str) -> Tuple[List[str], str]:
    frames: List[str] = []
    while "#" in buffer:
        before, buffer = buffer.split("#", 1)
        candidate = before.strip()
        if candidate:
            frames.append(candidate + "#")
    return frames, buffer


@dataclass
class PreparedDevice:
    imei: str
    device_id: int
    patient_id: Optional[int] = None
    patient_name: Optional[str] = None


@dataclass
class HealthProfile:
    heart_rate: int = DEFAULT_HEART_RATE
    blood_diastolic: int = DEFAULT_BLOOD_DIASTOLIC
    blood_systolic: int = DEFAULT_BLOOD_SYSTOLIC
    spo2: int = DEFAULT_SPO2
    body_temp: float = DEFAULT_BODY_TEMP
    wrist_temp: float = DEFAULT_WRIST_TEMP
    worn: bool = True
    bluetooth_payload: str = DEFAULT_BLUETOOTH_PAYLOAD
    bluetooth_gateway: str = DEFAULT_BLUETOOTH_GATEWAY

    @property
    def blood_pressure_value(self) -> str:
        return f"{self.blood_diastolic}|{self.blood_systolic}"


class BraceletSimulator:
    def __init__(
        self,
        host: str,
        tcp_port: int,
        imei: str,
        index: int,
        scenario: str,
        center_lat: float,
        center_lng: float,
        radius_meters: float,
        interval: int,
        duration: int,
        battery: int,
        health: HealthProfile,
        seed: int,
        verbose: bool,
    ) -> None:
        self.host = host
        self.tcp_port = tcp_port
        self.imei = imei
        self.index = index
        self.scenario = scenario
        self.center_lat = center_lat
        self.center_lng = center_lng
        self.radius_meters = radius_meters
        self.interval = max(1, interval)
        self.duration = max(1, duration)
        self.battery = battery
        self.health = health
        self.verbose = verbose
        self.rng = random.Random(seed + index)
        self.sock: Optional[socket.socket] = None
        self.stop_event = threading.Event()
        self.received_frames: List[str] = []
        self.reader_thread: Optional[threading.Thread] = None

    def connect(self) -> None:
        self.sock = socket.create_connection((self.host, self.tcp_port), timeout=10)
        self.sock.settimeout(1.0)
        self.reader_thread = threading.Thread(target=self._reader_loop, name=f"reader-{self.imei}", daemon=True)
        self.reader_thread.start()
        self.send(build_ap00(self.imei, self.index))

    def close(self) -> None:
        self.stop_event.set()
        if self.sock:
            try:
                self.sock.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            try:
                self.sock.close()
            except OSError:
                pass

    def send(self, frame: str) -> None:
        if not frame.endswith("#"):
            raise ScriptError(f"Invalid frame without #: {frame}")
        if self.verbose:
            log(f"{self.imei} -> {frame}")
        if not self.sock:
            raise ScriptError("Socket is not connected.")
        self.sock.sendall((frame + "\n").encode("utf-8"))

    def _reader_loop(self) -> None:
        assert self.sock is not None
        buffer = ""
        while not self.stop_event.is_set():
            try:
                chunk = self.sock.recv(4096)
                if not chunk:
                    break
                buffer += chunk.decode("utf-8", errors="replace")
                frames, buffer = split_frames(buffer)
                for frame in frames:
                    self.received_frames.append(frame)
                    if self.verbose:
                        log(f"{self.imei} <- {frame}")
                    self._auto_reply_downlink(frame)
            except socket.timeout:
                continue
            except OSError:
                break

    def _auto_reply_downlink(self, frame: str) -> None:
        if frame.startswith("IWBP16,"):
            parts = frame[:-1].split(",")
            seq = parts[2] if len(parts) >= 3 else "000000"
            try:
                self.send(build_ap16_ack(seq))
                lat, lng = self.next_point()
                self.send(build_ap01_network_location(lat, lng, battery=self.battery, seed=self.index))
            except Exception as exc:  # pragma: no cover - best effort during socket teardown
                log(f"{self.imei} failed to auto reply BP16: {exc}")
        elif frame.startswith("IWBPXL,"):
            self._send_health_safe(2, str(self.health.heart_rate))
        elif frame.startswith("IWBPXY,"):
            self._send_health_safe(1, self.health.blood_pressure_value)
        elif frame.startswith("IWBPXZ,"):
            self._send_health_safe(4, str(self.health.spo2))
        elif frame.startswith("IWBPXX,"):
            try:
                self.send(build_aptp(self.health.body_temp, self.health.wrist_temp))
            except Exception as exc:  # pragma: no cover
                log(f"{self.imei} failed to auto reply BPXX: {exc}")

    def _send_health_safe(self, health_type: int, value: str) -> None:
        try:
            self.send(build_apjk(health_type, value))
        except Exception as exc:  # pragma: no cover
            log(f"{self.imei} failed to auto reply health command: {exc}")

    def next_point(self) -> Tuple[float, float]:
        return random_point(self.center_lat, self.center_lng, self.radius_meters, self.rng)

    def send_initial_frames(self) -> None:
        lat, lng = self.next_point()
        self.send(build_ap03(battery=self.battery, steps=1000 + self.index, interval=self.interval))
        if self.scenario in ("basic", "location", "all", "downlink"):
            self.send(build_ap01_network_location(lat, lng, battery=self.battery, seed=self.index))
            self.send(build_ap01_gps(lat, lng, battery=self.battery))
        if self.scenario in ("health", "all"):
            self.send_health_frames()
        if self.scenario in ("alarm", "all"):
            self.send(build_ap10(self.imei, lat, lng, battery=self.battery, alarm_type="SOS"))
            self.send(build_ap04(self.imei, battery=min(self.battery, 15)))

    def send_health_frames(self) -> None:
        self.send(build_apjk(2, str(self.health.heart_rate)))
        self.send(build_apjk(1, self.health.blood_pressure_value))
        self.send(build_apjk(4, str(self.health.spo2)))
        self.send(build_apjk(3, f"{self.health.body_temp:.1f}"))
        self.send(build_aptp(self.health.body_temp, self.health.wrist_temp))
        self.send(build_apwr(self.imei, worn=self.health.worn))
        self.send(build_apbl(self.imei, self.health.bluetooth_payload, self.health.bluetooth_gateway))

    def run(self) -> None:
        self.connect()
        log(f"Simulator online: imei={self.imei}, scenario={self.scenario}")
        try:
            time.sleep(0.5)
            self.send_initial_frames()
            start = time.monotonic()
            next_health = start + self.interval * 2
            while time.monotonic() - start < self.duration and not self.stop_event.is_set():
                if self.stop_event.wait(self.interval):
                    break
                elapsed = int(time.monotonic() - start)
                lat, lng = self.next_point()
                steps = 1000 + self.index + elapsed
                battery = max(1, self.battery - elapsed // 300)
                self.send(build_ap03(battery=battery, steps=steps, interval=self.interval))
                if self.scenario in ("basic", "location", "all", "downlink"):
                    self.send(build_ap01_network_location(lat, lng, battery=battery, seed=self.index))
                if self.scenario in ("health", "all") and time.monotonic() >= next_health:
                    self.send_health_frames()
                    next_health = time.monotonic() + self.interval * 2
        finally:
            self.close()
            log(f"Simulator stopped: imei={self.imei}, received={len(self.received_frames)}")


def get_password(args: argparse.Namespace, required: bool) -> Optional[str]:
    if getattr(args, "password", None):
        return args.password
    env_password = os.environ.get("SMART_PASSWORD")
    if env_password:
        return env_password
    if required:
        return getpass.getpass("API password: ")
    return None


def build_imeis(args: argparse.Namespace) -> List[str]:
    raw_imeis: List[str] = []
    for value in getattr(args, "imei", None) or []:
        raw_imeis.extend(part.strip() for part in value.split(",") if part.strip())
    if raw_imeis:
        return raw_imeis
    start = int(getattr(args, "imei_start", DEFAULT_IMEI_START))
    return [imei_for_index(start, i) for i in range(int(getattr(args, "count", DEFAULT_COUNT)))]


def parse_bool(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    text = str(value).strip().lower()
    return text not in {"0", "false", "no", "n", "off", "未佩戴"}


def health_profile_from_args(args: argparse.Namespace) -> HealthProfile:
    return HealthProfile(
        heart_rate=int(getattr(args, "heart_rate", DEFAULT_HEART_RATE)),
        blood_diastolic=int(getattr(args, "blood_diastolic", DEFAULT_BLOOD_DIASTOLIC)),
        blood_systolic=int(getattr(args, "blood_systolic", DEFAULT_BLOOD_SYSTOLIC)),
        spo2=int(getattr(args, "spo2", DEFAULT_SPO2)),
        body_temp=float(getattr(args, "body_temp", DEFAULT_BODY_TEMP)),
        wrist_temp=float(getattr(args, "wrist_temp", DEFAULT_WRIST_TEMP)),
        worn=parse_bool(getattr(args, "health_worn", True)),
        bluetooth_payload=str(getattr(args, "bluetooth_payload", DEFAULT_BLUETOOTH_PAYLOAD)),
        bluetooth_gateway=str(getattr(args, "bluetooth_gateway", DEFAULT_BLUETOOTH_GATEWAY)),
    )


def random_mac(rng: random.Random) -> str:
    return ":".join(f"{rng.randint(0, 255):02X}" for _ in range(6))


def random_bluetooth_payload(rng: random.Random) -> str:
    beacon_count = rng.randint(2, 4)
    names = ("TEST-BEACON-A", "TEST-BEACON-B", "WARD-GATE", "BED-SENSOR")
    beacons = []
    for index in range(beacon_count):
        name = names[index % len(names)]
        rssi = rng.randint(-88, -35)
        beacons.append(f"{name}|{random_mac(rng)}|{rssi}")
    return "&".join(beacons)


def random_parameter_values(
    scenario: str,
    rng: Optional[random.Random] = None,
    latitude: float = DEFAULT_LATITUDE,
    longitude: float = DEFAULT_LONGITUDE,
) -> Dict[str, Any]:
    rng = rng or random.Random()
    scenario = scenario if scenario in SCENARIOS else "all"
    location_fields = scenario in {"basic", "location", "alarm", "downlink", "all"}
    health_fields = scenario in {"health", "downlink", "all"}
    downlink_fields = scenario in {"downlink", "all"}

    values: Dict[str, Any] = {
        "interval": rng.randint(*RANDOM_INTERVAL_RANGE),
        "duration": rng.randint(*RANDOM_DURATION_RANGE),
        "battery": rng.randint(*RANDOM_BATTERY_RANGE),
    }

    if location_fields:
        lat, lng = random_point(latitude, longitude, RANDOM_LOCATION_CENTER_JITTER_METERS, rng)
        values.update(
            {
                "latitude": round(lat, 6),
                "longitude": round(lng, 6),
                "radius": round(rng.uniform(*RANDOM_LOCATION_RADIUS_RANGE), 1),
            }
        )

    if health_fields:
        diastolic = rng.randint(*RANDOM_BLOOD_DIASTOLIC_RANGE)
        systolic = max(95, min(160, diastolic + rng.randint(*RANDOM_BLOOD_PRESSURE_GAP_RANGE)))
        values.update(
            {
                "heart_rate": rng.randint(*RANDOM_HEART_RATE_RANGE),
                "blood_diastolic": diastolic,
                "blood_systolic": systolic,
                "spo2": rng.randint(*RANDOM_SPO2_RANGE),
                "body_temp": round(rng.uniform(*RANDOM_BODY_TEMP_RANGE), 1),
                "wrist_temp": round(rng.uniform(*RANDOM_WRIST_TEMP_RANGE), 1),
                "health_worn": rng.random() >= 0.08,
                "bluetooth_payload": random_bluetooth_payload(rng),
                "bluetooth_gateway": random_mac(rng),
            }
        )

    if downlink_fields:
        values.update(
            {
                "downlink_delay": round(rng.uniform(*RANDOM_DOWNLINK_DELAY_RANGE), 1),
                "downlink_retries": rng.randint(*RANDOM_DOWNLINK_RETRIES_RANGE),
            }
        )

    return values


def apply_random_parameter_values(args: argparse.Namespace) -> None:
    if not getattr(args, "random_params", False):
        return
    scenario = str(getattr(args, "scenario", getattr(args, "scenario_name", "all")))
    seed = getattr(args, "seed", None)
    rng = random.Random(seed if seed is not None else time.time_ns())
    values = random_parameter_values(
        scenario,
        rng=rng,
        latitude=float(getattr(args, "latitude", DEFAULT_LATITUDE)),
        longitude=float(getattr(args, "longitude", DEFAULT_LONGITUDE)),
    )
    for key, value in values.items():
        setattr(args, key, value)
    log(
        "Randomized parameters: "
        + ", ".join(f"{key}={value}" for key, value in sorted(values.items()) if key != "bluetooth_payload")
    )


def create_data(client: ApiClient, args: argparse.Namespace) -> List[PreparedDevice]:
    prepared: List[PreparedDevice] = []
    start = int(args.imei_start)
    for i in range(args.count):
        imei = imei_for_index(start, i)
        patient_name = f"{args.prefix}{i + 1:03d}"
        device_id = client.create_device(imei, i + 1)
        patient_id = client.create_patient(patient_name, i + 1)
        client.bind_patient_device(patient_id, device_id)
        prepared.append(PreparedDevice(imei=imei, device_id=device_id, patient_id=patient_id, patient_name=patient_name))
    return prepared


def run_simulators(args: argparse.Namespace, imeis: Sequence[str], client: Optional[ApiClient] = None) -> None:
    threads: List[threading.Thread] = []
    simulators: List[BraceletSimulator] = []
    scenario = args.scenario
    if scenario not in SCENARIOS:
        raise ScriptError(f"Unsupported scenario: {scenario}. Supported: {', '.join(SCENARIOS)}")
    health = health_profile_from_args(args)

    for index, imei in enumerate(imeis, start=1):
        simulator = BraceletSimulator(
            host=args.host,
            tcp_port=args.tcp_port,
            imei=imei,
            index=index,
            scenario=scenario,
            center_lat=args.latitude,
            center_lng=args.longitude,
            radius_meters=args.radius,
            interval=args.interval,
            duration=args.duration,
            battery=args.battery,
            health=health,
            seed=args.seed,
            verbose=args.verbose,
        )
        simulators.append(simulator)
        thread = threading.Thread(target=simulator.run, name=f"sim-{imei}", daemon=False)
        threads.append(thread)
        thread.start()
        time.sleep(args.connect_stagger)

    if scenario in ("downlink", "all") and client is not None:
        time.sleep(2.0)
        for imei in imeis:
            exercise_downlink(
                client,
                imei,
                delay=getattr(args, "downlink_delay", DEFAULT_DOWNLINK_DELAY_SECONDS),
                retries=getattr(args, "downlink_retries", DEFAULT_DOWNLINK_RETRIES),
            )

    try:
        for thread in threads:
            thread.join()
    except KeyboardInterrupt:
        log("Interrupted, stopping simulators...")
        for simulator in simulators:
            simulator.close()
        for thread in threads:
            thread.join(timeout=3)


def send_downlink_with_retry(
    client: ApiClient,
    endpoint: str,
    imei: str,
    delay: float,
    retries: int,
    **query: Any,
) -> bool:
    for attempt in range(retries + 1):
        try:
            client.downlink(endpoint, imei, **query)
            time.sleep(max(delay, 0.0))
            return True
        except RateLimitError as exc:
            if attempt >= retries:
                log(f"Rate limited, skipped {endpoint} for {imei}: {exc}")
                return False
            wait_seconds = max(delay, 1.0) * (2 ** attempt)
            log(f"Rate limited on {endpoint} for {imei}; retrying in {wait_seconds:.1f}s")
            time.sleep(wait_seconds)


def exercise_downlink(
    client: ApiClient,
    imei: str,
    delay: float = DEFAULT_DOWNLINK_DELAY_SECONDS,
    retries: int = DEFAULT_DOWNLINK_RETRIES,
) -> None:
    seq = datetime.now().strftime("%H%M%S")
    log(f"Testing downlink for imei={imei}, seq={seq}, delay={delay}s")
    commands = [
        ("bp16", {"seq": seq}),
        ("bp15", {"seq": seq, "interval": 60}),
        ("bp50", {"seq": seq}),
        ("bpxl", {"seq": seq}),
        ("bpxy", {"seq": seq}),
        ("bpxz", {"seq": seq}),
        ("bpxx", {"seq": seq}),
    ]
    for endpoint, query in commands:
        send_downlink_with_retry(client, endpoint, imei, delay, retries, **query)


def cleanup_data(client: ApiClient, args: argparse.Namespace) -> None:
    if not args.yes:
        raise ScriptError("cleanup requires --yes to avoid deleting real data.")

    start = int(args.imei_start)
    end = start + max(args.cleanup_range, args.count)
    deleted_bindings = 0
    deleted_patients = 0
    deleted_devices = 0

    patients = client.list_patients(search=args.prefix, limit=1000)
    for patient in patients:
        patient_id = extract_id(patient)
        name = str(patient.get("name", ""))
        if patient_id is None or not name.startswith(args.prefix):
            continue
        for binding in client.list_bindings_by_patient(int(patient_id)):
            device_id = extract_id({"id": binding.get("deviceId")})
            if device_id is not None:
                client.unbind(int(patient_id), int(device_id))
                deleted_bindings += 1
        client.delete_patient(int(patient_id))
        deleted_patients += 1

    devices = client.list_devices(search=str(start)[:6], limit=1000)
    for device in devices:
        device_id = extract_id(device)
        imei = str(device.get("imei", ""))
        if device_id is None or not imei.isdigit():
            continue
        imei_int = int(imei)
        if not (start <= imei_int < end):
            continue
        for binding in client.list_bindings_by_device(int(device_id)):
            patient_id = binding.get("patientId")
            if isinstance(patient_id, int):
                client.unbind(patient_id, int(device_id))
                deleted_bindings += 1
        client.delete_device(int(device_id))
        deleted_devices += 1

    log(
        "Cleanup complete: "
        f"bindings={deleted_bindings}, patients={deleted_patients}, devices={deleted_devices}"
    )


def dry_run(args: argparse.Namespace) -> None:
    imei = imei_for_index(args.imei_start, 0)
    health = health_profile_from_args(args)
    frames = [
        build_ap00(imei),
        build_ap01_network_location(args.latitude, args.longitude, battery=args.battery),
        build_ap01_gps(args.latitude, args.longitude, battery=args.battery),
        build_ap03(battery=args.battery, steps=1234, interval=args.interval),
        build_ap04(imei, battery=15),
        build_ap10(imei, args.latitude, args.longitude, battery=args.battery),
        build_apjk(2, str(health.heart_rate)),
        build_apjk(1, health.blood_pressure_value),
        build_apjk(4, str(health.spo2)),
        build_apjk(3, f"{health.body_temp:.1f}"),
        build_aptp(health.body_temp, health.wrist_temp),
        build_apwr(imei, health.worn),
        build_apbl(imei, health.bluetooth_payload, health.bluetooth_gateway),
    ]
    for frame in frames:
        if not frame.startswith("IW") or not frame.endswith("#"):
            raise ScriptError(f"Frame format invalid: {frame}")
    network_frame = frames[1]
    expected_fallback = f"[{args.latitude:.6f}@{args.longitude:.6f}]"
    if "V0000.0000N00000.0000E" not in network_frame or expected_fallback not in network_frame:
        raise ScriptError(f"Network location frame missing fallback coordinates: {network_frame}")
    heartbeat = frames[3]
    heartbeat_status = heartbeat.split(",")[1]
    battery_text = heartbeat_status[6:9]
    if len(battery_text) != 3 or int(battery_text) != args.battery:
        raise ScriptError(f"Heartbeat battery is not a 3-digit field: {heartbeat}")

    log("Dry-run frame validation passed.")
    if args.verbose:
        for frame in frames:
            print(frame)


def make_client_and_login(args: argparse.Namespace, required: bool = True) -> ApiClient:
    password = get_password(args, required=required)
    if password is None:
        raise ScriptError("API password is required for this mode.")
    client = ApiClient(args.host, args.api_port, timeout=args.api_timeout)
    client.login(args.username, password)
    return client


def run_gui() -> None:
    try:
        import tkinter as tk
        from tkinter import messagebox, ttk
    except ImportError as exc:
        raise ScriptError(f"tkinter is not available in this Python runtime: {exc}") from exc

    class BraceletGui:
        def __init__(self) -> None:
            self.root = tk.Tk()
            self.root.title("测试手环控制台")
            self.root.geometry("1180x760")
            self.root.minsize(1040, 680)

            self.log_queue: "queue.Queue[str]" = queue.Queue()
            self.worker: Optional[threading.Thread] = None
            self.running_simulators: List[BraceletSimulator] = []
            self.stop_requested = threading.Event()

            self.vars: Dict[str, Any] = {
                "host": tk.StringVar(value=DEFAULT_HOST),
                "api_port": tk.StringVar(value=str(DEFAULT_API_PORT)),
                "tcp_port": tk.StringVar(value=str(DEFAULT_TCP_PORT)),
                "username": tk.StringVar(value=DEFAULT_USERNAME),
                "password": tk.StringVar(value=os.environ.get("SMART_PASSWORD", "")),
                "count": tk.StringVar(value=str(DEFAULT_COUNT)),
                "imei_start": tk.StringVar(value=str(DEFAULT_IMEI_START)),
                "prefix": tk.StringVar(value=DEFAULT_PREFIX),
                "imeis": tk.StringVar(value=""),
                "scenario": tk.StringVar(value="all"),
                "latitude": tk.StringVar(value=str(DEFAULT_LATITUDE)),
                "longitude": tk.StringVar(value=str(DEFAULT_LONGITUDE)),
                "radius": tk.StringVar(value=str(DEFAULT_RADIUS_METERS)),
                "interval": tk.StringVar(value=str(DEFAULT_INTERVAL_SECONDS)),
                "duration": tk.StringVar(value=str(DEFAULT_DURATION_SECONDS)),
                "battery": tk.StringVar(value="80"),
                "heart_rate": tk.StringVar(value=str(DEFAULT_HEART_RATE)),
                "blood_diastolic": tk.StringVar(value=str(DEFAULT_BLOOD_DIASTOLIC)),
                "blood_systolic": tk.StringVar(value=str(DEFAULT_BLOOD_SYSTOLIC)),
                "spo2": tk.StringVar(value=str(DEFAULT_SPO2)),
                "body_temp": tk.StringVar(value=str(DEFAULT_BODY_TEMP)),
                "wrist_temp": tk.StringVar(value=str(DEFAULT_WRIST_TEMP)),
                "health_worn": tk.BooleanVar(value=True),
                "bluetooth_payload": tk.StringVar(value=DEFAULT_BLUETOOTH_PAYLOAD),
                "bluetooth_gateway": tk.StringVar(value=DEFAULT_BLUETOOTH_GATEWAY),
                "downlink_delay": tk.StringVar(value=str(DEFAULT_DOWNLINK_DELAY_SECONDS)),
                "downlink_retries": tk.StringVar(value=str(DEFAULT_DOWNLINK_RETRIES)),
                "verbose": tk.BooleanVar(value=False),
            }

            set_log_sink(self._enqueue_log)
            self._build_styles(ttk)
            self._build_layout(tk, ttk)
            self.root.protocol("WM_DELETE_WINDOW", self._on_close)
            self.root.after(100, self._drain_logs)

        def _build_styles(self, ttk: Any) -> None:
            style = ttk.Style(self.root)
            try:
                style.theme_use("clam")
            except tk.TclError:
                pass
            style.configure("TFrame", background="#f6f8fb")
            style.configure("Panel.TFrame", background="#ffffff", relief="solid", borderwidth=1)
            style.configure("Title.TLabel", background="#f6f8fb", foreground="#0f172a", font=("Microsoft YaHei UI", 17, "bold"))
            style.configure("Section.TLabel", background="#ffffff", foreground="#0f172a", font=("Microsoft YaHei UI", 11, "bold"))
            style.configure("Hint.TLabel", background="#ffffff", foreground="#64748b", font=("Microsoft YaHei UI", 9))
            style.configure("TLabel", background="#ffffff", foreground="#334155", font=("Microsoft YaHei UI", 9))
            style.configure("TButton", font=("Microsoft YaHei UI", 9), padding=(10, 6))
            style.configure("Primary.TButton", font=("Microsoft YaHei UI", 10, "bold"), padding=(12, 7))
            style.configure("Danger.TButton", foreground="#b91c1c", font=("Microsoft YaHei UI", 9, "bold"))

        def _build_layout(self, tk: Any, ttk: Any) -> None:
            self.root.configure(bg="#f6f8fb")
            outer = ttk.Frame(self.root, padding=18)
            outer.pack(fill="both", expand=True)

            header = ttk.Frame(outer)
            header.pack(fill="x", pady=(0, 14))
            ttk.Label(header, text="测试手环控制台", style="Title.TLabel").pack(side="left")
            ttk.Label(
                header,
                text="批量造数、协议模拟、下行测试、清理测试数据",
                background="#f6f8fb",
                foreground="#64748b",
                font=("Microsoft YaHei UI", 10),
            ).pack(side="left", padx=(14, 0), pady=(8, 0))

            body = ttk.Frame(outer)
            body.pack(fill="both", expand=True)
            body.columnconfigure(0, weight=0)
            body.columnconfigure(1, weight=1)
            body.rowconfigure(0, weight=1)

            left = ttk.Frame(body, style="Panel.TFrame", padding=16)
            left.grid(row=0, column=0, sticky="nsw", padx=(0, 14))
            right = ttk.Frame(body, style="Panel.TFrame", padding=16)
            right.grid(row=0, column=1, sticky="nsew")
            right.rowconfigure(2, weight=1)
            right.columnconfigure(0, weight=1)

            self._section(left, "连接配置", 0)
            self._field(left, "服务器", "host", 1)
            self._field(left, "API端口", "api_port", 2, width=12)
            self._field(left, "TCP端口", "tcp_port", 3, width=12)
            self._field(left, "用户名", "username", 4)
            self._field(left, "密码", "password", 5, show="*")

            self._section(left, "测试数据", 6)
            self._field(left, "数量", "count", 7, width=12)
            self._field(left, "IMEI起始", "imei_start", 8)
            self._field(left, "病人前缀", "prefix", 9)
            self._field(left, "指定IMEI", "imeis", 10)
            ttk.Label(left, text="多个 IMEI 用英文逗号分隔；留空则按起始值生成", style="Hint.TLabel").grid(
                row=11, column=0, columnspan=2, sticky="w", pady=(0, 10)
            )

            self._section(left, "模拟参数", 12)
            ttk.Label(left, text="场景").grid(row=13, column=0, sticky="w", pady=5)
            scenario_frame = ttk.Frame(left)
            scenario_frame.grid(row=13, column=1, sticky="ew", pady=5)
            for index, scenario in enumerate(SCENARIOS):
                ttk.Radiobutton(
                    scenario_frame,
                    text=scenario,
                    value=scenario,
                    variable=self.vars["scenario"],
                ).grid(row=index // 3, column=index % 3, sticky="w", padx=(0, 12), pady=2)
            ttk.Button(left, text="随机生成参数", command=self._randomize_parameters).grid(
                row=14, column=0, columnspan=2, sticky="ew", pady=(4, 8)
            )
            self.scenario_hint = ttk.Label(left, text="", style="Hint.TLabel", wraplength=260)
            self.scenario_hint.grid(row=15, column=0, columnspan=2, sticky="w", pady=(0, 6))
            self.parameter_fields = {
                "latitude": self._field(left, "纬度", "latitude", 16),
                "longitude": self._field(left, "经度", "longitude", 17),
                "radius": self._field(left, "半径(m)", "radius", 18, width=12),
                "interval": self._field(left, "上报间隔(s)", "interval", 19, width=12),
                "duration": self._field(left, "持续时间(s)", "duration", 20, width=12),
                "battery": self._field(left, "初始电量(%)", "battery", 21, width=12),
                "heart_rate": self._field(left, "心率", "heart_rate", 22, width=12),
                "blood_diastolic": self._field(left, "舒张压", "blood_diastolic", 23, width=12),
                "blood_systolic": self._field(left, "收缩压", "blood_systolic", 24, width=12),
                "spo2": self._field(left, "血氧(%)", "spo2", 25, width=12),
                "body_temp": self._field(left, "体温(℃)", "body_temp", 26, width=12),
                "wrist_temp": self._field(left, "腕温(℃)", "wrist_temp", 27, width=12),
                "health_worn": self._check_field(left, "佩戴状态", "已佩戴", "health_worn", 28),
                "bluetooth_payload": self._field(left, "蓝牙周边", "bluetooth_payload", 29),
                "bluetooth_gateway": self._field(left, "蓝牙网关MAC", "bluetooth_gateway", 30),
                "downlink_delay": self._field(left, "下行命令间隔(s)", "downlink_delay", 31, width=12),
                "downlink_retries": self._field(left, "429重试次数", "downlink_retries", 32, width=12),
            }
            ttk.Checkbutton(left, text="打印协议帧", variable=self.vars["verbose"]).grid(
                row=33, column=0, columnspan=2, sticky="w", pady=(8, 0)
            )
            self.vars["scenario"].trace_add("write", lambda *_: self._refresh_scenario_fields())
            self._refresh_scenario_fields()

            ttk.Label(right, text="操作", style="Section.TLabel").grid(row=0, column=0, sticky="w")
            buttons = ttk.Frame(right)
            buttons.grid(row=1, column=0, sticky="ew", pady=(12, 14))
            for i in range(4):
                buttons.columnconfigure(i, weight=1)

            self.buttons: List[Any] = []
            self._button(buttons, "Dry-run 验证", lambda: self._start_worker("dry-run"), 0, 0)
            self._button(buttons, "创建测试数据", lambda: self._start_worker("create"), 0, 1)
            self._button(buttons, "开始模拟", lambda: self._start_worker("simulate"), 0, 2, "Primary.TButton")
            self._button(buttons, "创建并模拟", lambda: self._start_worker("create-and-simulate"), 0, 3, "Primary.TButton")
            self._button(buttons, "运行场景", lambda: self._start_worker("scenario"), 1, 0)
            self._button(buttons, "清理测试数据", lambda: self._confirm_cleanup(), 1, 1, "Danger.TButton")
            self._button(buttons, "停止模拟", self._stop_simulation, 1, 2)
            self._button(buttons, "清空日志", self._clear_logs, 1, 3)

            ttk.Label(right, text="实时日志", style="Section.TLabel").grid(row=2, column=0, sticky="nw")
            log_frame = ttk.Frame(right)
            log_frame.grid(row=3, column=0, sticky="nsew", pady=(8, 0))
            log_frame.rowconfigure(0, weight=1)
            log_frame.columnconfigure(0, weight=1)
            self.log_text = tk.Text(
                log_frame,
                wrap="word",
                bg="#0f172a",
                fg="#dbeafe",
                insertbackground="#dbeafe",
                relief="flat",
                font=("Consolas", 10),
                padx=12,
                pady=10,
            )
            scrollbar = ttk.Scrollbar(log_frame, command=self.log_text.yview)
            self.log_text.configure(yscrollcommand=scrollbar.set)
            self.log_text.grid(row=0, column=0, sticky="nsew")
            scrollbar.grid(row=0, column=1, sticky="ns")

            self.status = ttk.Label(
                right,
                text="空闲",
                background="#ffffff",
                foreground="#2563eb",
                font=("Microsoft YaHei UI", 9, "bold"),
            )
            self.status.grid(row=4, column=0, sticky="w", pady=(10, 0))

        def _section(self, parent: Any, text: str, row: int) -> None:
            import tkinter.ttk as ttk

            ttk_label = ttk.Label(parent, text=text, style="Section.TLabel")
            ttk_label.grid(row=row, column=0, columnspan=2, sticky="w", pady=(12 if row else 0, 6))

        def _field(self, parent: Any, label: str, key: str, row: int, width: int = 24, show: Optional[str] = None) -> Tuple[Any, Any]:
            import tkinter.ttk as ttk

            label_widget = ttk.Label(parent, text=label)
            label_widget.grid(row=row, column=0, sticky="w", pady=5, padx=(0, 10))
            entry = ttk.Entry(parent, textvariable=self.vars[key], width=width, show=show)
            entry.grid(row=row, column=1, sticky="ew", pady=5)
            parent.columnconfigure(1, weight=1)
            return label_widget, entry

        def _check_field(self, parent: Any, label: str, text: str, key: str, row: int) -> Tuple[Any, Any]:
            import tkinter.ttk as ttk

            label_widget = ttk.Label(parent, text=label)
            label_widget.grid(row=row, column=0, sticky="w", pady=5, padx=(0, 10))
            check = ttk.Checkbutton(parent, text=text, variable=self.vars[key])
            check.grid(row=row, column=1, sticky="w", pady=5)
            parent.columnconfigure(1, weight=1)
            return label_widget, check

        def _refresh_scenario_fields(self) -> None:
            scenario = str(self.vars["scenario"].get())
            location_fields = {"latitude", "longitude", "radius"}
            health_fields = {
                "heart_rate",
                "blood_diastolic",
                "blood_systolic",
                "spo2",
                "body_temp",
                "wrist_temp",
                "health_worn",
                "bluetooth_payload",
                "bluetooth_gateway",
            }
            downlink_fields = {"downlink_delay", "downlink_retries"}
            visible = {"interval", "duration", "battery"}

            hints = {
                "basic": "基础场景：心跳 + 网络定位 + GPS定位。",
                "location": "定位场景：循环上报网络定位，主要使用经纬度和随机半径。",
                "health": "健康场景：只发送心率、血压、血氧、体温、佩戴和蓝牙数据；经纬度参数不参与。",
                "alarm": "告警场景：发送 SOS/低电量告警，告警包会使用定位参数。",
                "downlink": "下行场景：模拟设备在线并测试立即定位、定位间隔等下行命令。",
                "all": "全量场景：定位、健康、告警和下行命令都会参与。",
            }

            if scenario in {"basic", "location", "alarm", "downlink", "all"}:
                visible.update(location_fields)
            if scenario in {"health", "all"}:
                visible.update(health_fields)
            if scenario in {"downlink", "all"}:
                visible.update(downlink_fields)
                visible.update(health_fields)

            self.scenario_hint.configure(text=hints.get(scenario, ""))
            for key, widgets in self.parameter_fields.items():
                for widget in widgets:
                    if key in visible:
                        widget.grid()
                    else:
                        widget.grid_remove()

        def _button(self, parent: Any, text: str, command: Any, row: int, column: int, style: Optional[str] = None) -> None:
            import tkinter.ttk as ttk

            button = ttk.Button(parent, text=text, command=command, style=style)
            button.grid(row=row, column=column, sticky="ew", padx=5, pady=5)
            if text != "停止模拟":
                self.buttons.append(button)

        def _enqueue_log(self, line: str) -> None:
            self.log_queue.put(line)

        def _drain_logs(self) -> None:
            try:
                while True:
                    line = self.log_queue.get_nowait()
                    self.log_text.insert("end", line + "\n")
                    self.log_text.see("end")
            except queue.Empty:
                pass
            self.root.after(100, self._drain_logs)

        def _clear_logs(self) -> None:
            self.log_text.delete("1.0", "end")

        def _randomize_parameters(self) -> None:
            try:
                center_lat = float(str(self.vars["latitude"].get()).strip() or DEFAULT_LATITUDE)
                center_lng = float(str(self.vars["longitude"].get()).strip() or DEFAULT_LONGITUDE)
            except ValueError:
                center_lat = DEFAULT_LATITUDE
                center_lng = DEFAULT_LONGITUDE
            values = random_parameter_values(
                str(self.vars["scenario"].get()),
                rng=random.Random(time.time_ns()),
                latitude=center_lat,
                longitude=center_lng,
            )
            for key, value in values.items():
                if key in self.vars:
                    self.vars[key].set(value if isinstance(value, bool) else str(value))
            self._refresh_scenario_fields()
            log(
                "GUI randomized parameters: "
                + ", ".join(f"{key}={value}" for key, value in sorted(values.items()) if key != "bluetooth_payload")
            )

        def _set_busy(self, busy: bool, text: str = "") -> None:
            state = "disabled" if busy else "normal"
            for button in self.buttons:
                button.configure(state=state)
            self.status.configure(text=text or ("运行中" if busy else "空闲"))

        def _args(self) -> argparse.Namespace:
            def int_value(key: str, default: int) -> int:
                raw = str(self.vars[key].get()).strip()
                return default if not raw else int(raw)

            def float_value(key: str, default: float) -> float:
                raw = str(self.vars[key].get()).strip()
                return default if not raw else float(raw)

            raw_imeis = str(self.vars["imeis"].get()).strip()
            imei_values = [raw_imeis] if raw_imeis else None
            password = str(self.vars["password"].get())
            return argparse.Namespace(
                host=str(self.vars["host"].get()).strip(),
                api_port=int_value("api_port", DEFAULT_API_PORT),
                tcp_port=int_value("tcp_port", DEFAULT_TCP_PORT),
                username=str(self.vars["username"].get()).strip(),
                password=password if password else None,
                api_timeout=15.0,
                verbose=bool(self.vars["verbose"].get()),
                count=int_value("count", DEFAULT_COUNT),
                imei_start=int_value("imei_start", DEFAULT_IMEI_START),
                prefix=str(self.vars["prefix"].get()),
                imei=imei_values,
                scenario=str(self.vars["scenario"].get()),
                scenario_name=str(self.vars["scenario"].get()),
                latitude=float_value("latitude", DEFAULT_LATITUDE),
                longitude=float_value("longitude", DEFAULT_LONGITUDE),
                radius=float_value("radius", DEFAULT_RADIUS_METERS),
                interval=int_value("interval", DEFAULT_INTERVAL_SECONDS),
                duration=int_value("duration", DEFAULT_DURATION_SECONDS),
                battery=int_value("battery", 80),
                heart_rate=int_value("heart_rate", DEFAULT_HEART_RATE),
                blood_diastolic=int_value("blood_diastolic", DEFAULT_BLOOD_DIASTOLIC),
                blood_systolic=int_value("blood_systolic", DEFAULT_BLOOD_SYSTOLIC),
                spo2=int_value("spo2", DEFAULT_SPO2),
                body_temp=float_value("body_temp", DEFAULT_BODY_TEMP),
                wrist_temp=float_value("wrist_temp", DEFAULT_WRIST_TEMP),
                health_worn=bool(self.vars["health_worn"].get()),
                bluetooth_payload=str(self.vars["bluetooth_payload"].get()).strip() or DEFAULT_BLUETOOTH_PAYLOAD,
                bluetooth_gateway=str(self.vars["bluetooth_gateway"].get()).strip() or DEFAULT_BLUETOOTH_GATEWAY,
                downlink_delay=float_value("downlink_delay", DEFAULT_DOWNLINK_DELAY_SECONDS),
                downlink_retries=int_value("downlink_retries", DEFAULT_DOWNLINK_RETRIES),
                seed=20260605,
                connect_stagger=0.2,
                cleanup_range=10000,
                yes=True,
            )

        def _client(self, args: argparse.Namespace) -> ApiClient:
            password = args.password or os.environ.get("SMART_PASSWORD")
            if not password:
                raise ScriptError("请输入密码，或设置 SMART_PASSWORD 环境变量。")
            client = ApiClient(args.host, args.api_port, timeout=args.api_timeout)
            client.login(args.username, password)
            return client

        def _confirm_cleanup(self) -> None:
            import tkinter.messagebox as messagebox

            if messagebox.askyesno("确认清理", "只会清理测试前缀和测试 IMEI 范围的数据。确认继续？"):
                self._start_worker("cleanup")

        def _start_worker(self, command: str) -> None:
            if self.worker and self.worker.is_alive():
                messagebox.showwarning("正在运行", "当前已有任务在运行，请先停止或等待完成。")
                return
            try:
                args = self._args()
            except ValueError as exc:
                messagebox.showerror("参数错误", f"请检查数字输入：{exc}")
                return
            self.stop_requested.clear()
            self._set_busy(True, f"运行中：{command}")
            self.worker = threading.Thread(target=self._run_command, args=(command, args), daemon=True)
            self.worker.start()

        def _run_command(self, command: str, args: argparse.Namespace) -> None:
            try:
                log(f"GUI command started: {command}")
                if command == "dry-run":
                    dry_run(args)
                elif command == "create":
                    create_data(self._client(args), args)
                elif command == "cleanup":
                    cleanup_data(self._client(args), args)
                elif command == "simulate":
                    client = self._client(args) if args.scenario in ("downlink", "all") else None
                    self._run_gui_simulators(args, build_imeis(args), client)
                elif command == "create-and-simulate":
                    client = self._client(args)
                    prepared = create_data(client, args)
                    self._run_gui_simulators(args, [item.imei for item in prepared], client)
                elif command == "scenario":
                    args.scenario = args.scenario_name
                    client = self._client(args)
                    prepared = create_data(client, args)
                    self._run_gui_simulators(args, [item.imei for item in prepared], client)
                else:
                    raise ScriptError(f"Unsupported GUI command: {command}")
                log(f"GUI command finished: {command}")
            except Exception as exc:
                log(f"ERROR: {exc}")
            finally:
                self.running_simulators = []
                self.root.after(0, lambda: self._set_busy(False, "空闲"))

        def _run_gui_simulators(self, args: argparse.Namespace, imeis: Sequence[str], client: Optional[ApiClient]) -> None:
            threads: List[threading.Thread] = []
            simulators: List[BraceletSimulator] = []
            self.running_simulators = simulators
            health = health_profile_from_args(args)
            for index, imei in enumerate(imeis, start=1):
                if self.stop_requested.is_set():
                    break
                simulator = BraceletSimulator(
                    host=args.host,
                    tcp_port=args.tcp_port,
                    imei=imei,
                    index=index,
                    scenario=args.scenario,
                    center_lat=args.latitude,
                    center_lng=args.longitude,
                    radius_meters=args.radius,
                    interval=args.interval,
                    duration=args.duration,
                    battery=args.battery,
                    health=health,
                    seed=args.seed,
                    verbose=args.verbose,
                )
                simulators.append(simulator)
                thread = threading.Thread(target=simulator.run, name=f"gui-sim-{imei}", daemon=True)
                threads.append(thread)
                thread.start()
                time.sleep(args.connect_stagger)

            if args.scenario in ("downlink", "all") and client is not None and not self.stop_requested.is_set():
                time.sleep(2.0)
                for imei in imeis:
                    if self.stop_requested.is_set():
                        break
                    exercise_downlink(
                        client,
                        imei,
                        delay=args.downlink_delay,
                        retries=args.downlink_retries,
                    )

            while any(thread.is_alive() for thread in threads):
                if self.stop_requested.is_set():
                    for simulator in simulators:
                        simulator.close()
                for thread in threads:
                    thread.join(timeout=0.3)

        def _stop_simulation(self) -> None:
            self.stop_requested.set()
            for simulator in list(self.running_simulators):
                simulator.close()
            log("Stop requested.")

        def _on_close(self) -> None:
            self._stop_simulation()
            set_log_sink(None)
            self.root.destroy()

        def run(self) -> None:
            self.root.mainloop()

    BraceletGui().run()


def add_common_options(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--host", default=DEFAULT_HOST, help="API/TCP host or API base URL.")
    parser.add_argument("--api-port", type=int, default=DEFAULT_API_PORT, help="HTTP API port.")
    parser.add_argument("--tcp-port", type=int, default=DEFAULT_TCP_PORT, help="Bracelet TCP service port.")
    parser.add_argument("--username", default=DEFAULT_USERNAME, help="API username.")
    parser.add_argument("--password", help="API password. If omitted, SMART_PASSWORD or prompt is used.")
    parser.add_argument("--api-timeout", type=float, default=15.0, help="HTTP request timeout seconds.")
    parser.add_argument("--verbose", action="store_true", help="Print sent and received frames.")


def add_generation_options(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--count", type=int, default=DEFAULT_COUNT, help="Number of test bracelets.")
    parser.add_argument("--imei-start", type=int, default=DEFAULT_IMEI_START, help="First generated test IMEI.")
    parser.add_argument("--prefix", default=DEFAULT_PREFIX, help="Generated patient name prefix.")


def add_simulation_options(parser: argparse.ArgumentParser, include_scenario: bool = True) -> None:
    parser.add_argument("--imei", action="append", help="Existing IMEI. Can repeat or use comma-separated values.")
    if include_scenario:
        parser.add_argument("--scenario", choices=SCENARIOS, default="all", help="Simulation scenario.")
    parser.add_argument("--latitude", type=float, default=DEFAULT_LATITUDE, help="Center latitude.")
    parser.add_argument("--longitude", type=float, default=DEFAULT_LONGITUDE, help="Center longitude.")
    parser.add_argument("--radius", type=float, default=DEFAULT_RADIUS_METERS, help="Random location radius in meters.")
    parser.add_argument("--interval", type=int, default=DEFAULT_INTERVAL_SECONDS, help="Report interval seconds.")
    parser.add_argument("--duration", type=int, default=DEFAULT_DURATION_SECONDS, help="Simulation duration seconds.")
    parser.add_argument("--battery", type=int, default=80, help="Initial battery percentage.")
    parser.add_argument("--heart-rate", type=int, default=DEFAULT_HEART_RATE, help="APJK heart-rate value.")
    parser.add_argument("--blood-diastolic", type=int, default=DEFAULT_BLOOD_DIASTOLIC, help="APJK blood-pressure diastolic value.")
    parser.add_argument("--blood-systolic", type=int, default=DEFAULT_BLOOD_SYSTOLIC, help="APJK blood-pressure systolic value.")
    parser.add_argument("--spo2", type=int, default=DEFAULT_SPO2, help="APJK blood-oxygen value.")
    parser.add_argument("--body-temp", type=float, default=DEFAULT_BODY_TEMP, help="APTP body temperature.")
    parser.add_argument("--wrist-temp", type=float, default=DEFAULT_WRIST_TEMP, help="APTP wrist temperature.")
    parser.add_argument("--health-worn", choices=("yes", "no"), default="yes", help="APWR wearing state.")
    parser.add_argument("--bluetooth-payload", default=DEFAULT_BLUETOOTH_PAYLOAD, help="APBL bluetooth beacon payload.")
    parser.add_argument("--bluetooth-gateway", default=DEFAULT_BLUETOOTH_GATEWAY, help="APBL gateway MAC.")
    parser.add_argument("--random-params", action="store_true", help="Randomize scenario parameters within type-specific thresholds.")
    parser.add_argument("--downlink-delay", type=float, default=DEFAULT_DOWNLINK_DELAY_SECONDS, help="Delay between downlink API calls.")
    parser.add_argument("--downlink-retries", type=int, default=DEFAULT_DOWNLINK_RETRIES, help="Retry count for HTTP 429 downlink responses.")
    parser.add_argument("--seed", type=int, default=20260605, help="Random seed.")
    parser.add_argument("--connect-stagger", type=float, default=0.2, help="Seconds between simulator connections.")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Create and simulate IW protocol test bracelets.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    for name in ("create", "create-and-simulate"):
        sub = subparsers.add_parser(name, help=f"{name} test devices/patients.")
        add_common_options(sub)
        add_generation_options(sub)
        if name == "create-and-simulate":
            add_simulation_options(sub)

    simulate = subparsers.add_parser("simulate", help="Simulate existing or generated IMEIs.")
    add_common_options(simulate)
    add_generation_options(simulate)
    add_simulation_options(simulate)

    scenario = subparsers.add_parser("scenario", help="Create data and run one scenario.")
    scenario.add_argument("scenario_name", choices=SCENARIOS, help="Scenario to run.")
    add_common_options(scenario)
    add_generation_options(scenario)
    add_simulation_options(scenario, include_scenario=False)

    cleanup = subparsers.add_parser("cleanup", help="Delete generated test data.")
    add_common_options(cleanup)
    add_generation_options(cleanup)
    cleanup.add_argument("--cleanup-range", type=int, default=10000, help="IMEI range size eligible for cleanup.")
    cleanup.add_argument("--yes", action="store_true", help="Confirm cleanup.")

    dry = subparsers.add_parser("dry-run", help="Validate generated frames without network calls.")
    add_generation_options(dry)
    add_simulation_options(dry)
    dry.add_argument("--verbose", action="store_true", help="Print frames.")

    subparsers.add_parser("gui", help="Open the local desktop GUI.")

    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    try:
        if args.command == "gui":
            run_gui()
            return 0

        apply_random_parameter_values(args)

        if args.command == "dry-run":
            dry_run(args)
            return 0

        if args.command == "create":
            client = make_client_and_login(args)
            create_data(client, args)
            return 0

        if args.command == "cleanup":
            client = make_client_and_login(args)
            cleanup_data(client, args)
            return 0

        if args.command == "simulate":
            imeis = build_imeis(args)
            client = None
            if args.scenario in ("downlink", "all"):
                client = make_client_and_login(args)
            run_simulators(args, imeis, client=client)
            return 0

        if args.command == "create-and-simulate":
            client = make_client_and_login(args)
            prepared = create_data(client, args)
            run_simulators(args, [item.imei for item in prepared], client=client)
            return 0

        if args.command == "scenario":
            args.scenario = args.scenario_name
            client = make_client_and_login(args)
            prepared = create_data(client, args)
            run_simulators(args, [item.imei for item in prepared], client=client)
            return 0

        raise ScriptError(f"Unsupported command: {args.command}")
    except ScriptError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
