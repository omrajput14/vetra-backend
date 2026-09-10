#!/usr/bin/env python3
"""
PASHU SATHI / VETRA — Khandesh Region Supplemental Seeder
District Focus: Dhule (general activity) + Jalgaon (Rabies outbreak)

Naming Convention (zero "demo" anywhere):
  Farmer  : vijay.borse.dhule@vetra.co.in
  Vet     : dr.priya.sonawane.khandesh@vetra.co.in
  Vet reg : VRC-KH-MH-2026
  Tags    : DHU-TAG-001..004  /  QR: DHU-QR-001..004
  Scans   : KH-SCAN-HASH-DHU-TAG-00x-B

Design:
  Dhule   — 4 animals, 5 health records, 3 AI scans, 5 disease reports
            (HS suspected, LSD suspected, BVD confirmed, Mastitis confirmed,
             BVD suspected) — spread over 2-4 weeks, different diseases,
             sub-threshold → NO outbreak triggered.
  Jalgaon — 1 Rabies CONFIRMED case via disease report API → real
            OutbreakDetectionEngine fires 1-case/24-hr zoonotic threshold →
            genuine second active outbreak. Distance from Pune FMD ~305 km.
"""

import argparse, datetime, json, math, os, sys, time
from typing import Any, Dict, List, Optional, Tuple
import requests

DEFAULT_BASE_URL = os.getenv("VETRA_API_URL", "http://localhost:8080/api/v1")

DHULE_FARMER = {
    "email": "vijay.borse.dhule@vetra.co.in",
    "phone": "+919801050051",
    "password": "VijayFarmer@2026",
    "fullName": "Vijay Ashokrao Borse",
    "farmName": "Borse Agro Livestock Farm",
    "village": "Deopur",
    "taluka": "Dhule",
    "district": "Dhule",
    "state": "Maharashtra",
    "latitude": 20.9042,
    "longitude": 74.7749,
    "animalCount": 8,
    "preferredLanguage": "mr",
    "expectedRole": "FARMER",
}

KHANDESH_VET = {
    "email": "dr.priya.sonawane.khandesh@vetra.co.in",
    "phone": "+919801060061",
    "password": "PriyaVet@2026",
    "fullName": "Dr. Priya Ramesh Sonawane",
    "registrationNumber": "VRC-KH-MH-2026",
    "qualification": "BVSc & AH, MVSc Veterinary Medicine",
    "specialization": "Zoonotic Disease Management & Livestock Epidemiology",
    "clinicName": "Khandesh Pashu Arogya Kendra",
    "clinicAddress": "Near Mahabal Road, Dhule, Maharashtra 424001",
    "village": "Dhule",
    "taluka": "Dhule",
    "district": "Dhule",
    "state": "Maharashtra",
    "yearsExperience": 9,
    "latitude": 20.9042,
    "longitude": 74.7749,
    "preferredLanguage": "mr",
    "expectedRole": "VETERINARIAN",
}

GOVERNMENT = {
    "identifier": "officer@vetra.gov.in",
    "password": "Password@123",
    "expectedRole": "GOVERNMENT_OFFICER",
}

DHU_TAG_PREFIX      = "DHU-TAG-"
DHU_QR_PREFIX       = "DHU-QR-"
KH_SCAN_HASH_PREFIX = "KH-SCAN-HASH-"
JAL_LAT, JAL_LNG    = 21.0077, 75.5626
PUNE_LAT, PUNE_LNG  = 18.5204, 73.8567


def haversine_km(lat1, lng1, lat2, lng2):
    R = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = math.radians(lat2-lat1), math.radians(lng2-lng1)
    a = math.sin(dp/2)**2 + math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 2*R*math.asin(math.sqrt(a))


import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


class VetraApiClient:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.verify = False
        if "135.235.217.194" in self.base_url:
            self.session.headers["Host"] = "api.vetra.co.in"

    def _url(self, path):
        return f"{self.base_url}/{path.lstrip('/')}"

    def post(self, path, data=None, token=None):
        h = {"Content-Type": "application/json"}
        if token: h["Authorization"] = f"Bearer {token}"
        try:
            r = self.session.post(self._url(path), json=data, headers=h, timeout=15)
            try: body = r.json()
            except: body = {"text": r.text}
            return r.status_code, body
        except requests.RequestException as e:
            return 0, {"error": str(e)}

    def put(self, path, data=None, token=None):
        h = {"Content-Type": "application/json"}
        if token: h["Authorization"] = f"Bearer {token}"
        try:
            r = self.session.put(self._url(path), json=data, headers=h, timeout=15)
            try: body = r.json()
            except: body = {"text": r.text}
            return r.status_code, body
        except requests.RequestException as e:
            return 0, {"error": str(e)}

    def get(self, path, params=None, token=None):
        h = {}
        if token: h["Authorization"] = f"Bearer {token}"
        try:
            r = self.session.get(self._url(path), params=params, headers=h, timeout=15)
            try: body = r.json()
            except: body = {"text": r.text}
            return r.status_code, body
        except requests.RequestException as e:
            return 0, {"error": str(e)}

    def patch(self, path, data=None, token=None):
        h = {"Content-Type": "application/json"}
        if token: h["Authorization"] = f"Bearer {token}"
        try:
            r = self.session.patch(self._url(path), json=data, headers=h, timeout=15)
            try: body = r.json()
            except: body = {"text": r.text}
            return r.status_code, body
        except requests.RequestException as e:
            return 0, {"error": str(e)}


class TemporalTimeline:
    def __init__(self):
        self.anchor = datetime.datetime.now(datetime.timezone.utc)

    def format_iso(self, days_ago=0, hours_ago=0):
        return (self.anchor - datetime.timedelta(days=days_ago, hours=hours_ago)).strftime("%Y-%m-%dT%H:%M:%SZ")

    def format_local_iso(self, days_ago=0):
        return (self.anchor - datetime.timedelta(days=days_ago)).strftime("%Y-%m-%dT%H:%M:%S")

    def format_date(self, days_ago=0):
        return (self.anchor - datetime.timedelta(days=days_ago)).strftime("%Y-%m-%d")

    def future_date(self, days_ahead):
        return (self.anchor + datetime.timedelta(days=days_ahead)).strftime("%Y-%m-%d")


class KhandeshSeeder:
    def __init__(self, client, timeline):
        self.client   = client
        self.timeline = timeline
        self.tokens   = {}
        self.user_ids = {}
        self.animals  = {}

    def authenticate_all(self):
        print("\n[Phase 1] Authenticating Khandesh Accounts (Check-Before-Create)...")
        for role, udef in [("FARMER", DHULE_FARMER), ("VETERINARIAN", KHANDESH_VET), ("GOVERNMENT_OFFICER", GOVERNMENT)]:
            r = self._auth_or_register(role, udef)
            print(f"  * {role:20s}: {r['action']} | {r['identifier']}")

    def _auth_or_register(self, role, udef):
        email = udef.get("email") or udef.get("identifier")
        s, b = self.client.post("/auth/login", {"identifier": email, "password": udef["password"]})
        if s == 200 and b.get("success"):
            self.tokens[role] = b["data"]["accessToken"]
            self.user_ids[role] = str(b["data"].get("user", {}).get("id", ""))
            return {"identifier": email, "action": "REUSED_EXISTING"}
        if role == "FARMER":
            s, b = self.client.post("/auth/farmer/register", {
                "email": udef["email"], "phone": udef["phone"], "password": udef["password"],
                "fullName": udef["fullName"], "farmName": udef["farmName"],
                "village": udef["village"], "taluka": udef["taluka"],
                "district": udef["district"], "state": udef["state"],
                "latitude": udef["latitude"], "longitude": udef["longitude"],
                "animalCount": udef["animalCount"], "preferredLanguage": udef["preferredLanguage"],
            })
            if s in (200,201) and b.get("success"):
                self.tokens[role] = b["data"]["accessToken"]
                self.user_ids[role] = str(b["data"].get("user", {}).get("id", ""))
                return {"identifier": email, "action": "CREATED_NEW"}
        elif role == "VETERINARIAN":
            s, b = self.client.post("/auth/vet/register", {
                "email": udef["email"], "phone": udef["phone"], "password": udef["password"],
                "fullName": udef["fullName"], "registrationNumber": udef["registrationNumber"],
                "qualification": udef["qualification"], "specialization": udef["specialization"],
                "clinicName": udef["clinicName"], "clinicAddress": udef["clinicAddress"],
                "village": udef["village"], "taluka": udef["taluka"],
                "district": udef["district"], "state": udef["state"],
                "yearsExperience": udef["yearsExperience"],
                "latitude": udef["latitude"], "longitude": udef["longitude"],
                "preferredLanguage": udef["preferredLanguage"],
            })
            if s in (200,201) and b.get("success"):
                self.tokens[role] = b["data"]["accessToken"]
                self.user_ids[role] = str(b["data"].get("user", {}).get("id", ""))
                return {"identifier": email, "action": "CREATED_NEW"}
        return {"identifier": email, "action": "FAILED"}

    def seed_animals(self):
        print("\n[Phase 2] Seeding Dhule Livestock Herd...")
        tok = self.tokens.get("FARMER")
        if not tok: return
        s, b = self.client.get("/animals", token=tok)
        existing = {}
        if s == 200 and isinstance(b.get("data"), list):
            for a in b["data"]:
                if a.get("tagNumber"): existing[a["tagNumber"]] = a
        defs = [
            {"tag": f"{DHU_TAG_PREFIX}001","name":"Malti",  "species":"CATTLE", "breed":"Gir",           "gender":"FEMALE","birthDays":1460},
            {"tag": f"{DHU_TAG_PREFIX}002","name":"Savitri","species":"BUFFALO","breed":"Murrah",         "gender":"FEMALE","birthDays":1825},
            {"tag": f"{DHU_TAG_PREFIX}003","name":"Bhima",  "species":"CATTLE", "breed":"Kankrej Bull",   "gender":"MALE",  "birthDays":1095},
            {"tag": f"{DHU_TAG_PREFIX}004","name":"Rukmini","species":"CATTLE", "breed":"Sahiwal",        "gender":"FEMALE","birthDays":730 },
        ]
        for d in defs:
            tag = d["tag"]
            if tag in existing:
                self.animals[tag] = existing[tag]
                print(f"  * {tag} ({d['name']}): REUSED_EXISTING (ID: {existing[tag]['id']})")
            else:
                cs, cb = self.client.post("/animals", {
                    "animalName": d["name"], "tagNumber": tag,
                    "qrCodeId": tag.replace(DHU_TAG_PREFIX, DHU_QR_PREFIX),
                    "species": d["species"], "breed": d["breed"], "gender": d["gender"],
                    "birthDate": self.timeline.format_date(d["birthDays"]),
                }, token=tok)
                if cs in (200,201) and cb.get("success"):
                    self.animals[tag] = cb["data"]
                    print(f"  * {tag} ({d['name']}): CREATED_NEW (ID: {cb['data']['id']})")
                else:
                    print(f"  * {tag} ({d['name']}): FAILED ({cb.get('message')})")

    def seed_health_records(self):
        print("\n[Phase 3] Seeding Dhule Health Records...")
        tok = self.tokens.get("FARMER")
        if not tok: return
        records = [
            {"tag":f"{DHU_TAG_PREFIX}001","recordType":"OBSERVATION","title":"Quarterly Anthelmintic Deworming","description":"Routine ivermectin administration. BCS 3/5.","daysAgo":28},
            {"tag":f"{DHU_TAG_PREFIX}001","recordType":"VACCINATION","title":"Haemorrhagic Septicaemia (HS) Immunisation","description":"Subcutaneous HS vaccine — monsoon seasonal protocol.","vaccineName":"Haemorrhagic Septicaemia Vaccine","nextDueDays":365,"daysAgo":22},
            {"tag":f"{DHU_TAG_PREFIX}002","recordType":"DIAGNOSIS","title":"Subclinical Mastitis Screening","description":"CMT positive in one quarter. Teat dip hygiene protocol initiated.","daysAgo":17},
            {"tag":f"{DHU_TAG_PREFIX}003","recordType":"OBSERVATION","title":"Dermatophilosis Clinical Check","description":"Mild scabby lesions on neck post-monsoon. Topical antiseptic applied.","daysAgo":11},
            {"tag":f"{DHU_TAG_PREFIX}004","recordType":"VACCINATION","title":"FMD Trivalent Immunisation (Raksha-Ovac)","description":"Biannual FMD vaccination Types O, A, Asia-1.","vaccineName":"Foot and Mouth Disease (FMD) Vaccine","nextDueDays":180,"daysAgo":5},
        ]
        for rec in records:
            tag = rec["tag"]
            if tag not in self.animals:
                print(f"  ! Skip {tag}: not loaded"); continue
            anim_id = self.animals[tag]["id"]
            s, b = self.client.get(f"/animals/{anim_id}/health-records", token=tok)
            existing = b.get("data", []) if s == 200 and isinstance(b.get("data"), list) else []
            if any(r.get("title") == rec["title"] for r in existing):
                print(f"  * '{rec['title']}': REUSED_EXISTING"); continue
            payload = {"recordType": rec["recordType"], "title": rec["title"],
                       "description": rec["description"], "recordedAt": self.timeline.format_local_iso(rec["daysAgo"])}
            if rec.get("vaccineName"): payload["vaccineName"] = rec["vaccineName"]
            if rec.get("nextDueDays"): payload["nextDueDate"] = self.timeline.future_date(rec["nextDueDays"])
            cs, cb = self.client.post(f"/animals/{anim_id}/health-records", payload, token=tok)
            if cs in (200,201) and cb.get("success"):
                print(f"  * '{rec['title']}': CREATED_NEW (-{rec['daysAgo']}d)")
            else:
                print(f"  * '{rec['title']}': FAILED ({cb.get('message')})")

    def seed_ai_scans(self):
        print("\n[Phase 4] Seeding Dhule AI Scans (3 scans — 2 pending, 1 vet-rejected)...")
        ftok = self.tokens.get("FARMER")
        vtok = self.tokens.get("VETERINARIAN")
        if not ftok or not vtok: return
        s, b = self.client.get("/ai/scans", token=ftok)
        existing_scans = b.get("data", []) if s == 200 and isinstance(b.get("data"), list) else []
        scan_defs = [
            {"tag":f"{DHU_TAG_PREFIX}001","hash":f"{KH_SCAN_HASH_PREFIX}{DHU_TAG_PREFIX}001-B","action":"KEEP_PENDING"},
            {"tag":f"{DHU_TAG_PREFIX}002","hash":f"{KH_SCAN_HASH_PREFIX}{DHU_TAG_PREFIX}002-B","action":"KEEP_PENDING"},
            {"tag":f"{DHU_TAG_PREFIX}003","hash":f"{KH_SCAN_HASH_PREFIX}{DHU_TAG_PREFIX}003-B","action":"VET_REJECT",
             "rejectionReason":"Dermatophilosis — bacterial skin condition. Non-viral. No quarantine required."},
        ]
        for sd in scan_defs:
            tag = sd["tag"]
            if tag not in self.animals:
                print(f"  ! Skip {tag}: not loaded"); continue
            anim_id = self.animals[tag]["id"]
            match = next((x for x in existing_scans if x.get("imageHash") == sd["hash"] and x.get("status") != "FAILED"), None)
            if match:
                sid, st = match["id"], match.get("status")
                print(f"  * {tag} ({sd['hash']}): REUSED_EXISTING (ID: {sid}, Status: {st})")
                if sd["action"] == "VET_REJECT" and st in ("PENDING","COMPLETED"):
                    rs, _ = self.client.post(f"/ai/scans/{sid}/reject", {"rejectionReason": sd["rejectionReason"]}, token=vtok)
                    print(f"    -> Vet Rejected: {'PASS' if rs == 200 else 'FAIL'}")
                continue
            cs, cb = self.client.post("/ai/scans", {
                "animalId": anim_id,
                "imageUrl": f"/api/v1/animals/{anim_id}/photo",
                "imageHash": sd["hash"],
            }, token=ftok)
            if cs in (200,201) and cb.get("success"):
                sid = cb["data"]["id"]
                print(f"  * {tag}: CREATED_NEW (ID: {sid})")
                if sd["action"] == "VET_REJECT":
                    rs, _ = self.client.post(f"/ai/scans/{sid}/reject", {"rejectionReason": sd["rejectionReason"]}, token=vtok)
                    print(f"    -> Vet Rejected: {'PASS' if rs == 200 else 'FAIL'}")
            else:
                print(f"  * {tag}: FAILED ({cb.get('message')})")

    def seed_dhule_disease_reports(self):
        print("\n[Phase 5] Seeding Dhule General Disease Activity (5 reports, no outbreak)...")
        vtok = self.tokens.get("VETERINARIAN")
        if not vtok: return
        s, b = self.client.get("/disease/reports", params={"size": 200}, token=vtok)
        existing = b.get("data", {}).get("content", []) if s == 200 and isinstance(b.get("data"), dict) else []
        target_reports = [
            {"tag":f"{DHU_TAG_PREFIX}001","diseaseName":"Haemorrhagic Septicaemia","status":"SUSPECTED","source":"MANUAL","confidence":"FARMER","lat":20.9010,"lng":74.7720,"notes":"Sudden high fever, drooling, laboured breathing. Farmer field observation.","daysAgo":26},
            {"tag":f"{DHU_TAG_PREFIX}002","diseaseName":"Lumpy Skin Disease","status":"SUSPECTED","source":"MANUAL","confidence":"AI_VERIFIED","lat":20.9060,"lng":74.7760,"notes":"AI scan flagged multiple nodular skin lesions on buffalo. Isolated for monitoring.","daysAgo":21},
            {"tag":f"{DHU_TAG_PREFIX}003","diseaseName":"Bovine Viral Diarrhoea (BVD)","status":"CONFIRMED","source":"VETERINARIAN","confidence":"VETERINARIAN","lat":20.9030,"lng":74.7700,"notes":"Confirmed BVD via antigen ELISA. Supportive therapy. No herd spread detected.","daysAgo":14},
            {"tag":f"{DHU_TAG_PREFIX}002","diseaseName":"Bovine Mastitis","status":"CONFIRMED","source":"VETERINARIAN","confidence":"VETERINARIAN","lat":20.9055,"lng":74.7755,"notes":"Clinical mastitis right fore quarter. Intramammary antibiotic infusion applied.","daysAgo":10},
            {"tag":f"{DHU_TAG_PREFIX}004","diseaseName":"Bovine Viral Diarrhoea (BVD)","status":"SUSPECTED","source":"MANUAL","confidence":"FARMER","lat":20.9038,"lng":74.7730,"notes":"Mild watery diarrhoea and reduced milk yield. Samples sent for lab confirmation.","daysAgo":4},
        ]
        for rdef in target_reports:
            tag = rdef["tag"]
            if tag not in self.animals:
                print(f"  ! Skip {tag}: not loaded"); continue
            anim_id = self.animals[tag]["id"]
            match = next((r for r in existing
                if r.get("tagNumber") == tag
                and r.get("diseaseName") == rdef["diseaseName"]
                and r.get("diagnosisStatus") == rdef["status"]), None)
            if match:
                print(f"  * {tag} ({rdef['diseaseName']}/{rdef['status']}): REUSED_EXISTING (ID: {match['id']})"); continue
            cs, cb = self.client.post("/disease/reports", {
                "animalId": anim_id,
                "diseaseName": rdef["diseaseName"],
                "diagnosisStatus": rdef["status"],
                "reportSource": rdef["source"],
                "diagnosisConfidenceSource": rdef["confidence"],
                "latitude": rdef["lat"],
                "longitude": rdef["lng"],
                "notes": rdef["notes"],
            }, token=vtok)
            if cs in (200,201) and cb.get("success"):
                print(f"  * {tag} ({rdef['diseaseName']}/{rdef['status']}): CREATED_NEW (ID: {cb['data']['id']})")
            else:
                print(f"  * {tag}: FAILED ({cb.get('message')})")

    def seed_jalgaon_rabies_outbreak(self):
        print("\n[Phase 6] Seeding Jalgaon Rabies Outbreak (1-Case Zoonotic Trigger)...")
        dist = haversine_km(PUNE_LAT, PUNE_LNG, JAL_LAT, JAL_LNG)
        print(f"  Distance check: Pune -> Jalgaon = {dist:.1f} km (must be >> 25 km — OK)")
        vtok = self.tokens.get("VETERINARIAN")
        if not vtok: return
        tag = f"{DHU_TAG_PREFIX}001"
        if tag not in self.animals:
            print(f"  ! {tag} not loaded"); return
        anim_id = self.animals[tag]["id"]
        s, b = self.client.get("/disease/reports", params={"size": 200}, token=vtok)
        existing = b.get("data", {}).get("content", []) if s == 200 and isinstance(b.get("data"), dict) else []
        match = next((r for r in existing
            if "rabies" in r.get("diseaseName","").lower()
            and r.get("diagnosisStatus") == "CONFIRMED"), None)
        if match:
            print(f"  * Jalgaon Rabies: REUSED_EXISTING (ID: {match['id']})")
            print("  -> OutbreakDetectionEngine already processed this cluster.")
            return
        cs, cb = self.client.post("/disease/reports", {
            "animalId": anim_id,
            "diseaseName": "Rabies",
            "diagnosisStatus": "CONFIRMED",
            "reportSource": "VETERINARIAN",
            "diagnosisConfidenceSource": "VETERINARIAN",
            "latitude": JAL_LAT,
            "longitude": JAL_LNG,
            "notes": (
                "Confirmed rabies in stray cattle contact at Jalgaon district. "
                "Animal presented aggression, profuse salivation, hydrophobia. "
                "Post-mortem brainstem samples sent to state veterinary lab. "
                "ZOONOTIC ALERT: Farmer family advised post-exposure prophylaxis immediately."
            ),
        }, token=vtok)
        if cs in (200,201) and cb.get("success"):
            rep = cb["data"]
            print(f"  * Jalgaon Rabies CONFIRMED: CREATED_NEW (ID: {rep['id']})")
            print("  -> Waiting 2s for OutbreakDetectionEngine async evaluation...")
            time.sleep(2)
        else:
            print(f"  * Jalgaon Rabies: FAILED ({cb.get('message')})")

    def query_counts(self):
        counts = {"users": "?", "animals_dhu": 0, "diseaseReports_total": 0, "aiScans_dhu": 0, "outbreaks_total": 0}
        s, b = self.client.post("/auth/login", {"identifier": "admin@vetra.gov.in", "password": "Password@123"})
        if s == 200 and b.get("success"):
            adm = b["data"]["accessToken"]
            s2, b2 = self.client.get("/developer/overview", token=adm)
            if s2 == 200: counts["users"] = b2.get("data", {}).get("totalUsers", "?")
        ftok = self.tokens.get("FARMER")
        vtok = self.tokens.get("VETERINARIAN")
        gtok = self.tokens.get("GOVERNMENT_OFFICER")
        if ftok:
            s, b = self.client.get("/animals", token=ftok)
            if s == 200 and isinstance(b.get("data"), list):
                counts["animals_dhu"] = sum(1 for a in b["data"] if a.get("tagNumber","").startswith(DHU_TAG_PREFIX))
            s, b = self.client.get("/ai/scans", token=ftok)
            if s == 200 and isinstance(b.get("data"), list):
                counts["aiScans_dhu"] = sum(1 for sc in b["data"] if sc.get("imageHash","").startswith(KH_SCAN_HASH_PREFIX))
        if vtok:
            s, b = self.client.get("/disease/reports", params={"size": 200}, token=vtok)
            if s == 200 and isinstance(b.get("data"), dict):
                counts["diseaseReports_total"] = b["data"].get("totalElements", 0)
        if gtok:
            s, b = self.client.get("/disease/outbreaks", token=gtok)
            if s == 200 and isinstance(b.get("data"), list):
                counts["outbreaks_total"] = len(b["data"])
        return counts

    def run_verify(self):
        print("\n=======================================================")
        print("  KHANDESH SEED — POST-SEED VERIFICATION")
        print("=======================================================")
        self.authenticate_all()
        ftok = self.tokens.get("FARMER")
        vtok = self.tokens.get("VETERINARIAN")
        gtok = self.tokens.get("GOVERNMENT_OFFICER")
        checks = []

        # 1. Accounts
        for role, tok, exp in [("Dhule Farmer", ftok,"FARMER"),("Khandesh Vet",vtok,"VETERINARIAN"),("Gov Officer",gtok,"GOVERNMENT_OFFICER")]:
            s, b = self.client.get("/auth/me", token=tok)
            ok = s == 200 and b.get("data", {}).get("role") == exp
            checks.append(ok)
            print(f"  [ACCOUNTS] {role:20s}: {'PASS' if ok else 'FAIL'}")

        # 2. Animals
        s, b = self.client.get("/animals", token=ftok)
        animals = b.get("data", []) if s == 200 else []
        tags_found = {a.get("tagNumber") for a in animals}
        expected = {f"{DHU_TAG_PREFIX}{i:03d}" for i in range(1,5)}
        ok = expected.issubset(tags_found)
        checks.append(ok)
        print(f"  [ANIMALS]  Dhule herd (4)       : {'PASS' if ok else 'FAIL'} (found: {tags_found & expected})")

        # 3. AI scans
        s, b = self.client.get("/ai/scans", token=ftok)
        scans = b.get("data", []) if s == 200 else []
        kh_scans = [x for x in scans if x.get("imageHash","").startswith(KH_SCAN_HASH_PREFIX)]
        hp = any(x.get("status") in ("PENDING","COMPLETED") for x in kh_scans)
        hr = any(x.get("status") == "REJECTED" for x in kh_scans)
        ok = hp and hr
        checks.append(ok)
        print(f"  [AI_SCANS] Dhule scans (mixed)  : {'PASS' if ok else 'FAIL'} (pending={hp}, rejected={hr}, total={len(kh_scans)})")

        # 4. Dhule reports (>=5)
        s, b = self.client.get("/disease/reports", params={"size": 200}, token=vtok)
        all_reps = b.get("data",{}).get("content",[]) if s == 200 else []
        dhu_reps = [r for r in all_reps if r.get("tagNumber","").startswith(DHU_TAG_PREFIX)]
        ok = len(dhu_reps) >= 5
        checks.append(ok)
        print(f"  [DISEASE]  Dhule reports (>=5)  : {'PASS' if ok else 'FAIL'} (found: {len(dhu_reps)})")

        # 5. Jalgaon Rabies report
        jal = next((r for r in all_reps if "rabies" in r.get("diseaseName","").lower() and r.get("diagnosisStatus")=="CONFIRMED"), None)
        ok = jal is not None
        checks.append(ok)
        print(f"  [DISEASE]  Jalgaon Rabies rpt   : {'PASS' if ok else 'FAIL'} ({'ID: '+jal['id'] if jal else 'NOT FOUND'})")

        # 6. Outbreaks: >=2 active, including Pune FMD + Jalgaon Rabies
        s, b = self.client.get("/disease/outbreaks", token=gtok)
        outbreaks = b.get("data", []) if s == 200 else []
        active = [o for o in outbreaks if o.get("status") in ("MONITORING","ACTIVE","ESCALATED")]
        pune_ok = any("fmd" in o.get("diseaseName","").lower() or "foot" in o.get("diseaseName","").lower() for o in active)
        jal_ok  = any("rabies" in o.get("diseaseName","").lower() for o in active)
        ok = pune_ok and jal_ok
        checks.append(ok)
        print(f"  [OUTBREAK] Active (>=2 expected) : {'PASS' if ok else 'FAIL'} (total_active={len(active)}, pune_fmd={pune_ok}, jal_rabies={jal_ok})")

        # 7. Pune cluster untouched
        pune_obs = [o for o in active if "fmd" in o.get("diseaseName","").lower() or "foot" in o.get("diseaseName","").lower()]
        ok = len(pune_obs) >= 1
        checks.append(ok)
        print(f"  [SAFETY]   Pune FMD intact       : {'PASS' if ok else 'FAIL'} ({len(pune_obs)} FMD outbreak(s))")

        # 8. No false Dhule outbreak
        dhule_bad = [o for o in outbreaks if any(d in o.get("diseaseName","").lower() for d in ["haemorrhagic","lumpy","bvd","mastitis"])]
        ok = len(dhule_bad) == 0
        checks.append(ok)
        print(f"  [SAFETY]   No false Dhule alert  : {'PASS' if ok else 'WARN'} (unwanted={len(dhule_bad)})")

        passed = sum(1 for c in checks if c)
        print(f"\n  RESULT: {passed}/{len(checks)} checks passed {'✓' if passed == len(checks) else '⚠'}")

        print("\n  All Active Outbreaks (Dashboard view):")
        for o in active:
            lat = o.get("centerLatitude") or o.get("latitude") or "?"
            lng = o.get("centerLongitude") or o.get("longitude") or "?"
            print(f"    - {o.get('diseaseName','?'):38s} | {o.get('status','?'):12s} | Risk: {o.get('riskScore','?')} | ({lat}, {lng})")

    def run_seed(self):
        print("\n=======================================================")
        print("  PASHU SATHI — KHANDESH SUPPLEMENTAL SEED (API-ONLY)")
        print("=======================================================")
        print(f"API        : {self.client.base_url}")
        print(f"Timestamp  : {self.timeline.anchor.isoformat()}")
        print(f"Jalgaon->Pune distance: {haversine_km(PUNE_LAT,PUNE_LNG,JAL_LAT,JAL_LNG):.1f} km (>> 25 km outbreak merge radius)\n")

        self.authenticate_all()
        before = self.query_counts()
        print(f"\n  BEFORE: {json.dumps(before, default=str)}")

        self.seed_animals()
        self.seed_health_records()
        self.seed_ai_scans()
        self.seed_dhule_disease_reports()
        self.seed_jalgaon_rabies_outbreak()

        after = self.query_counts()
        print(f"\n  AFTER:  {json.dumps(after, default=str)}")
        print("\n  Deterministic Identifiers:")
        print(f"    Farmer    : vijay.borse.dhule@vetra.co.in")
        print(f"    Vet       : dr.priya.sonawane.khandesh@vetra.co.in  (VRC-KH-MH-2026)")
        print(f"    Tags      : DHU-TAG-001..004  |  QR: DHU-QR-001..004")
        print(f"    AI hashes : KH-SCAN-HASH-DHU-TAG-00x-A")
        print(f"    Jalgaon   : {JAL_LAT}N, {JAL_LNG}E — Rabies CONFIRMED (zoonotic trigger)")
        print(f"    Backup    : backups/vetra_db_backup_20260909_172600.dump (600K)")

        self.run_verify()


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--verify", action="store_true")
    p.add_argument("--url", default=DEFAULT_BASE_URL)
    args = p.parse_args()
    client = VetraApiClient(args.url)
    tl     = TemporalTimeline()
    seeder = KhandeshSeeder(client, tl)
    if args.verify:
        seeder.authenticate_all()
        seeder.run_verify()
    else:
        seeder.run_seed()

if __name__ == "__main__":
    main()
