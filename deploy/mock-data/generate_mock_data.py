#!/usr/bin/env python3
"""
Fills the six SwiftDrop databases with linked mock data by driving the real services, then moves every
timeline into the past so the data looks like a month of operation.

Why through the APIs: parcels, status history, outbox events, notifications, webhook deliveries,
dispatch assignments, proof photos and merchant orders are then written by the services' own code, so
every row passes their rules and the rows in different databases point at each other correctly.
SQL is only used where no API exists yet (the first staff roles, dispatch courier records) and for the time shift.

Needs the Docker Compose stack running on fresh, migrated databases (see README.md). Standard library only.

    python3 deploy/mock-data/generate_mock_data.py

All mock users share the password in MOCK_PASSWORD (a test value for local development only).
"""

import datetime as dt
import json
import random
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

MOCK_PASSWORD = "SwiftDrop@2026"
SEED = 20261114
DAYS_OF_HISTORY = 30
POSTGRES_CONTAINER = "swiftdrop-postgres"

AUTH = "http://localhost:8081"
PRICING = "http://localhost:8082"
PARCEL = "http://localhost:8083"
DISPATCH = "http://localhost:8084"
MERCHANT = "http://localhost:8085"

SL = dt.timezone(dt.timedelta(hours=5, minutes=30))  # Sri Lanka time, for working hours
NOW = dt.datetime.now(dt.timezone.utc).replace(microsecond=0)

rnd = random.Random(SEED)

# ---------------------------------------------------------------------------------------------------------------
# Reference data
# ---------------------------------------------------------------------------------------------------------------

FIRST = ["Nimal", "Kamal", "Sunil", "Chamari", "Dilani", "Kasun", "Tharindu", "Ishara", "Nadeesha", "Ruwan",
         "Sanduni", "Supuni", "Thisaru", "Sachini", "Malith", "Hashini", "Amaya", "Pradeep", "Lahiru", "Nipuni",
         "Dinuka", "Shehan", "Kavindi", "Ravindu", "Gayani", "Chathura", "Sahan", "Dulani", "Isuru", "Tharushi",
         "Mohamed", "Fathima", "Rizwan", "Ayesha", "Arun", "Priya", "Kumar", "Lakshmi", "Dimuth", "Anjali",
         "Sajith", "Hiruni", "Yasiru", "Methmi", "Oshada", "Senuri"]
LAST = ["Perera", "Fernando", "Silva", "de Silva", "Jayasinghe", "Wijesinghe", "Bandara", "Herath", "Gunasekara",
        "Rajapaksa", "Dissanayake", "Senanayake", "Amarasekara", "Wickramasinghe", "Ratnayake", "Karunaratne",
        "Weerasinghe", "Jayawardena", "Mendis", "Fonseka", "Rathnayake", "Peiris", "Cooray", "Abeysekera",
        "Nawaz", "Rahman", "Sivakumar", "Rajendran", "Liyanage", "Samarasinghe"]

# Streets per zone, so addresses match the zone they are booked for
STREETS = {
    "COLOMBO_CENTRAL": [("Galle Road", "Colombo 03"), ("Duplication Road", "Colombo 03"), ("York Street", "Colombo 01"),
                        ("Chatham Street", "Colombo 01"), ("Union Place", "Colombo 02"), ("Kollupitiya Road", "Colombo 03")],
    "COLOMBO_SOUTH": [("Havelock Road", "Colombo 05"), ("Galle Road", "Colombo 04"), ("Marine Drive", "Colombo 06"),
                      ("Thimbirigasyaya Road", "Colombo 05"), ("Ramakrishna Road", "Colombo 06"), ("Lauries Road", "Colombo 04")],
    "COLOMBO_NORTH": [("Flower Road", "Colombo 07"), ("Baseline Road", "Colombo 09"), ("Main Street", "Colombo 11"),
                      ("Ward Place", "Colombo 07"), ("Maradana Road", "Colombo 10"), ("Prince of Wales Avenue", "Colombo 14")],
    "KOTTE_RAJAGIRIYA": [("Parliament Road", "Rajagiriya"), ("Kotte Road", "Kotte"), ("Nawala Road", "Rajagiriya"),
                         ("Pita Kotte Road", "Kotte")],
    "NUGEGODA_MAHARAGAMA": [("Stanley Thilakaratne Mawatha", "Nugegoda"), ("High Level Road", "Maharagama"),
                            ("Station Road", "Nugegoda"), ("Old Kesbewa Road", "Nugegoda")],
    "DEHIWALA_MT_LAVINIA": [("Galle Road", "Dehiwala"), ("Hotel Road", "Mount Lavinia"), ("Hill Street", "Dehiwala"),
                            ("De Saram Road", "Mount Lavinia")],
    "KANDY": [("Peradeniya Road", "Kandy"), ("Dalada Veediya", "Kandy"), ("William Gopallawa Mawatha", "Kandy")],
    "GALLE": [("Wakwella Road", "Galle"), ("Church Street", "Galle Fort"), ("Matara Road", "Galle")],
    "NEGOMBO": [("Lewis Place", "Negombo"), ("Main Street", "Negombo"), ("Colombo Road", "Negombo")],
}
COLOMBO_AREA = [z for z in STREETS if z not in ("KANDY", "GALLE", "NEGOMBO")]

SIZES = [("SMALL", 0.2, 5.0, 45), ("MEDIUM", 2.0, 10.0, 30), ("LARGE", 5.0, 20.0, 18), ("EXTRA_LARGE", 10.0, 35.0, 7)]
SPEEDS = [("STANDARD", 60), ("EXPRESS", 25), ("NEXT_DAY", 15)]

MERCHANTS = [
    ("Kandy Spice House", "Food & groceries", "KANDY", "APPROVED"),
    ("Colombo Threads", "Clothing", "COLOMBO_NORTH", "APPROVED"),
    ("TechLanka Gadgets", "Electronics", "COLOMBO_CENTRAL", "APPROVED"),
    ("Ceylon Tea Corner", "Food & groceries", "NUGEGODA_MAHARAGAMA", "APPROVED"),
    ("BookNook Lanka", "Books", "COLOMBO_SOUTH", "APPROVED"),
    ("Galle Fort Crafts", "Handicrafts", "GALLE", "APPROVED"),
    ("Negombo Fresh Fish", "Food & groceries", "NEGOMBO", "PENDING"),
    ("QuickFix Mobile Parts", "Electronics", "KOTTE_RAJAGIRIYA", "REJECTED"),
]

FAILURE_REASONS = ["Recipient not available after two attempts", "Address could not be found",
                   "Recipient refused the parcel", "Recipient phone unreachable", "Business closed at delivery time"]
DELIVERY_NOTES = [None, None, None, "Left with the security guard", "Handed to a family member",
                  "Delivered at the front desk", "Customer collected at the gate"]

# A 1x1 PNG, used as the proof-of-delivery photo
PROOF_PNG = bytes.fromhex("89504e470d0a1a0a0000000d4948445200000001000000010806000000"
                          "1f15c4890000000d49444154789c6360f8cfc0000003010100c9fe92ef0000000049454e44ae426082")


# ---------------------------------------------------------------------------------------------------------------
# HTTP and SQL helpers
# ---------------------------------------------------------------------------------------------------------------

class ApiError(Exception):
    pass


def call(method, url, body=None, token=None, headers=None, multipart=None):
    hdrs = {"Accept": "application/json"}
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        hdrs["Content-Type"] = "application/json"
    if multipart is not None:
        boundary = uuid.uuid4().hex
        parts = []
        for name, value in multipart.items():
            if value is None:
                continue
            if isinstance(value, bytes):
                parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="proof.png"\r\n'
                             f"Content-Type: image/png\r\n\r\n".encode() + value + b"\r\n")
            else:
                parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode())
        data = b"".join(parts) + f"--{boundary}--\r\n".encode()
        hdrs["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    if token:
        hdrs["Authorization"] = f"Bearer {token}"
    hdrs.update(headers or {})
    req = urllib.request.Request(url, data=data, method=method, headers=hdrs)
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                raw = resp.read()
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as e:
            detail = e.read().decode(errors="replace")[:400]
            if e.code in (429, 502, 503) and attempt < 4:
                time.sleep(1 + attempt)
                continue
            raise ApiError(f"{method} {url} -> {e.code}: {detail}") from None
    raise ApiError(f"{method} {url} failed")


def sql(db, statement):
    """Runs SQL in one database and returns rows as lists of strings (tab separated output)."""
    out = subprocess.run(["docker", "exec", "-i", POSTGRES_CONTAINER, "psql", "-U", "postgres", "-d", db,
                          "-v", "ON_ERROR_STOP=1", "-q", "-tA", "-F", "\t"],
                         input=statement, capture_output=True, text=True)
    if out.returncode != 0:
        raise RuntimeError(f"SQL failed in {db}: {out.stderr.strip()}")
    return [line.split("\t") for line in out.stdout.splitlines() if line]


def q(value):
    return "NULL" if value is None else "'" + str(value).replace("'", "''") + "'"


def ts(value):
    return "NULL" if value is None else f"'{value.isoformat()}'::timestamptz"


def log(message):
    print(message, flush=True)


# ---------------------------------------------------------------------------------------------------------------
# People
# ---------------------------------------------------------------------------------------------------------------

used_names, used_mobiles = set(), set()


def person(taken_first_names=None):
    """A new full name; with taken_first_names, also a first name not in that set (which it then joins)."""
    while True:
        first = rnd.choice(FIRST)
        if taken_first_names is not None and first in taken_first_names:
            continue
        name = f"{first} {rnd.choice(LAST)}"
        if name not in used_names:
            used_names.add(name)
            if taken_first_names is not None:
                taken_first_names.add(first)
            return name


def mobile():
    while True:
        number = "07" + rnd.choice("0124678") + "".join(rnd.choice("0123456789") for _ in range(7))
        if number not in used_mobiles:
            used_mobiles.add(number)
            return number


def email_for(name, domain="example.com"):
    return name.lower().replace(" ", ".").replace("..", ".") + "@" + domain


class User:
    def __init__(self, role, name, email, mobile_no):
        self.role, self.name, self.email, self.mobile = role, name, email, mobile_no
        self.id = None
        self.token = None

    def signup(self):
        resp = call("POST", f"{AUTH}/api/v1/auth/signup", {"fullName": self.name, "email": self.email,
                                                           "mobileNumber": self.mobile, "password": MOCK_PASSWORD})
        self.id = resp["user"]["id"]
        self.token = resp["accessToken"]

    def login(self):
        resp = call("POST", f"{AUTH}/api/v1/auth/login", {"emailOrMobile": self.email, "password": MOCK_PASSWORD})
        self.token = resp["accessToken"]
        return self


def make_users():
    users = []
    users += [User("ADMIN", n, e, mobile()) for n, e in [("Sachintha Dilshan", "admin@swiftdrop.example.com"),
                                                        ("Ops Admin", "ops.admin@swiftdrop.example.com")]]
    for i in range(3):
        n = person()
        users.append(User("DISPATCHER", n, f"dispatcher{i + 1}@swiftdrop.example.com", mobile()))
    # Dispatch screens show couriers by first name, so no two couriers share one
    courier_first_names = set()
    for i in range(10):
        n = person(courier_first_names)
        users.append(User("COURIER", n, f"courier{i + 1:02d}@swiftdrop.example.com", mobile()))
    for _ in MERCHANTS:
        n = person()
        users.append(User("MERCHANT", n, email_for(n, "merchant.example.com"), mobile()))
    users.append(User("CUSTOMER", "Demo Customer", "customer@example.com", mobile()))
    for _ in range(39):
        n = person()
        users.append(User("CUSTOMER", n, email_for(n), mobile()))
    return users


def setup_roles(users):
    """Staff roles have no API yet (GET/PATCH /admin/users is not built), so set them in auth_db."""
    by_role = {}
    for u in users:
        by_role.setdefault(u.role, []).append(u)
    admin = by_role["ADMIN"][0]
    stmts = []
    for u in users:
        if u.role in ("CUSTOMER", "MERCHANT"):  # merchants get their role when an admin approves them
            continue
        stmts.append(f"DELETE FROM user_roles WHERE user_id = {q(u.id)};")
        status = "ACTIVE"
        stmts.append(f"INSERT INTO user_roles (user_id, role_id, assignment_status, reviewed_at, reviewed_by) "
                     f"SELECT {q(u.id)}, id, '{status}', now(), {q(admin.id) if u is not admin else 'NULL'} "
                     f"FROM roles WHERE name = '{u.role}';")
    stmts.append("UPDATE users SET email_verified = true;")
    sql("auth_db", "BEGIN;\n" + "\n".join(stmts) + "\nCOMMIT;")
    # dispatch-service has no courier registration yet: its couriers table just links to auth users
    rows = []
    for i, c in enumerate(by_role["COURIER"]):
        status = "OFFLINE" if i == 9 else "AVAILABLE"
        rows.append(f"({q(c.id)}, '{status}')")
    sql("dispatch_db", "INSERT INTO couriers (user_id, status) VALUES " + ", ".join(rows) + ";")
    courier_ids = dict((r[1], r[0]) for r in sql("dispatch_db", "SELECT id, user_id FROM couriers"))
    for c in by_role["COURIER"]:
        c.courier_id = courier_ids[c.id]
    return by_role


# ---------------------------------------------------------------------------------------------------------------
# Promo codes (DROP100 comes from pricing-service's own seed migration)

PROMOS = [  # code, discount, valid from (days from now), valid to (days from now or None), active
    ("WELCOME50", 50, -90, None, True),
    ("AVURUDU2026", 150, -215, -190, True),   # Sinhala and Tamil New Year, expired
    ("XMAS250", 250, 30, 45, True),           # not started yet
    ("STAFFTEST", 500, -60, None, False),     # switched off
]


def setup_promos(by_role):
    admin = by_role["ADMIN"][0]
    for code, amount, start, end, active in PROMOS:
        body = {"code": code, "discountAmount": amount, "validFrom": iso(NOW + dt.timedelta(days=start)),
                "validTo": iso(NOW + dt.timedelta(days=end)) if end is not None else None, "active": active}
        call("POST", f"{PRICING}/api/v1/admin/promo-codes", body, token=admin.token)
    stmts = [f"UPDATE promo_codes SET created_at = {ts(NOW + dt.timedelta(days=start - 7))}, updated_at = NULL "
             f"WHERE code = {q(code)};" for code, _, start, _, _ in PROMOS]
    sql("pricing_db", "\n".join(stmts))


def iso(value):
    return value.isoformat().replace("+00:00", "Z")


# ---------------------------------------------------------------------------------------------------------------
# Merchants
# ---------------------------------------------------------------------------------------------------------------

def setup_merchants(by_role):
    admin = by_role["ADMIN"][0]
    merchants = []
    for (business, category, zone, status), m in zip(MERCHANTS, by_role["MERCHANT"]):
        street, district = rnd.choice(STREETS[zone])
        address = f"{rnd.randint(1, 250)} {street}, {district}"
        reg = call("POST", f"{MERCHANT}/api/v1/merchants/register",
                   {"businessName": business, "businessRegNo": f"PV{rnd.randint(100000, 999999)}",
                    "contactName": m.name, "email": m.email, "phone": m.mobile, "address": address}, token=m.token)
        merchant = {"user": m, "id": reg["id"], "zone": zone, "address": address, "status": status, "api_key": None}
        if status == "APPROVED":
            call("PATCH", f"{MERCHANT}/api/v1/admin/merchants/{reg['id']}/approve", token=admin.token)
            m.login()  # approval granted the MERCHANT role; a new token carries it
            key = call("POST", f"{MERCHANT}/api/v1/merchants/me/api-keys", {"name": "Online store"}, token=m.token)
            merchant["api_key"] = key.get("rawApiKey")
            if not merchant["api_key"]:
                raise RuntimeError(f"No API key in response fields {sorted(key)}")
            if rnd.random() < 0.3:
                call("POST", f"{MERCHANT}/api/v1/merchants/me/api-keys", {"name": "Warehouse system"}, token=m.token)
            slug = business.lower().replace(" ", "-").replace("&", "and")
            call("PUT", f"{MERCHANT}/api/v1/merchants/me/webhook",
                 {"webhookUrl": f"https://{slug}.example.com/swiftdrop/webhook", "rotateSecret": False}, token=m.token)
        elif status == "REJECTED":
            call("PATCH", f"{MERCHANT}/api/v1/admin/merchants/{reg['id']}/reject",
                 {"reason": "Business registration certificate could not be verified"}, token=admin.token)
        merchants.append(merchant)
    return merchants


# ---------------------------------------------------------------------------------------------------------------
# Parcels and their planned timelines
# ---------------------------------------------------------------------------------------------------------------

def working_time(day, earliest_hour=8, latest_hour=19):
    local = dt.datetime.combine(day, dt.time(rnd.randint(earliest_hour, latest_hour - 1), rnd.randint(0, 59)), SL)
    return local.astimezone(dt.timezone.utc)


def next_working_morning(after):
    local = after.astimezone(SL)
    day = local.date() + dt.timedelta(days=1)
    return dt.datetime.combine(day, dt.time(rnd.randint(8, 10), rnd.randint(0, 59)), SL).astimezone(dt.timezone.utc)


def plan_timeline(created, speed, will_fail):
    """Planned time of each step; only the steps before now happen."""
    steps = [("CREATED", created)]
    t = created + dt.timedelta(minutes=rnd.randint(15, 120))
    steps.append(("ASSIGNED", t))
    t += dt.timedelta(minutes=rnd.randint(30, 180))
    steps.append(("PICKED_UP", t))
    t += dt.timedelta(minutes=rnd.randint(30, 150))
    steps.append(("IN_TRANSIT", t))
    if speed == "EXPRESS" and t.astimezone(SL).hour < 16:
        t += dt.timedelta(minutes=rnd.randint(20, 90))
    else:
        t = next_working_morning(t)
    steps.append(("OUT_FOR_DELIVERY", t))
    t += dt.timedelta(minutes=rnd.randint(30, 240))
    steps.append(("FAILED" if will_fail else "DELIVERED", t))
    return [(s, when) for s, when in steps if when < NOW - dt.timedelta(minutes=5)]


def weighted(options):
    total = sum(w for *_, w in options)
    pick = rnd.uniform(0, total)
    for option in options:
        pick -= option[-1]
        if pick <= 0:
            return option
    return options[-1]


def parcel_shape():
    size, low, high, _ = weighted(SIZES)
    weight = round(rnd.uniform(low * 0.5 if size == "SMALL" else low, high), 2)
    speed = weighted(SPEEDS)[0]
    return size, max(weight, 0.1), speed


def address(zone):
    street, district = rnd.choice(STREETS[zone])
    return f"{rnd.randint(1, 350)} {street}, {district}"


def creation_times(count):
    """Spread over the last DAYS_OF_HISTORY days in working hours, a little busier recently."""
    times = []
    for _ in range(count):
        days_ago = int(DAYS_OF_HISTORY * (rnd.random() ** 1.3))
        day = (NOW.astimezone(SL) - dt.timedelta(days=days_ago)).date()
        when = working_time(day)
        if when >= NOW - dt.timedelta(minutes=20):
            when = NOW - dt.timedelta(minutes=rnd.randint(20, 240))
        times.append(when)
    return sorted(times)


def create_parcels(by_role, merchants, customer_count, merchant_count):
    parcels = []
    customers = by_role["CUSTOMER"]
    demo = customers[0]
    for created in creation_times(customer_count):
        owner = demo if rnd.random() < 0.12 else rnd.choice(customers)
        pickup = rnd.choice(COLOMBO_AREA) if rnd.random() < 0.85 else rnd.choice(list(STREETS))
        dropoff = rnd.choice(COLOMBO_AREA) if rnd.random() < 0.8 else rnd.choice(["KANDY", "GALLE", "NEGOMBO"])
        size, weight, speed = parcel_shape()
        body = {"pickupAddress": address(pickup), "pickupZone": pickup, "dropoffAddress": address(dropoff),
                "dropoffZone": dropoff, "recipientName": person(), "recipientPhone": mobile(), "size": size,
                "weightKg": weight, "speed": speed}
        if rnd.random() < 0.15:
            body["promoCode"] = rnd.choice(["DROP100", "WELCOME50"])
        resp = call("POST", f"{PARCEL}/api/v1/parcels", body, token=owner.token)
        parcels.append({"id": resp["id"], "tracking": resp["trackingNo"], "speed": speed, "created": created,
                        "merchant": None})
    approved = [m for m in merchants if m["status"] == "APPROVED"]
    for i, created in enumerate(creation_times(merchant_count)):
        m = approved[i % len(approved)]
        dropoff = rnd.choice(COLOMBO_AREA) if rnd.random() < 0.75 else rnd.choice(["KANDY", "GALLE", "NEGOMBO"])
        size, weight, speed = parcel_shape()
        recipient = person()
        body = {"recipientName": recipient, "recipientPhone": mobile(), "recipientEmail": email_for(recipient),
                "pickupAddress": m["address"], "pickupZone": m["zone"], "dropoffAddress": address(dropoff),
                "dropoffZone": dropoff, "weightKg": weight, "size": size, "speed": speed,
                "idempotencyKey": f"order-{uuid.UUID(int=rnd.getrandbits(128))}"}
        resp = call("POST", f"{MERCHANT}/api/v1/merchant-api/orders", body, headers={"X-API-Key": m["api_key"]})
        parcel_id = resp.get("parcelId") or resp.get("id")
        tracking = resp.get("trackingNo")
        parcels.append({"id": parcel_id, "tracking": tracking, "speed": speed, "created": created,
                        "merchant": m["id"]})
    return parcels


def progress_parcels(by_role, parcels):
    dispatchers = by_role["DISPATCHER"]
    couriers = [c for c in by_role["COURIER"]][:9]  # the 10th courier is OFFLINE
    for n, p in enumerate(parcels, 1):
        will_fail = rnd.random() < 0.08
        p["timeline"] = plan_timeline(p["created"], p["speed"], will_fail)
        steps = [s for s, _ in p["timeline"]]
        if "ASSIGNED" not in steps:
            continue
        dispatcher = rnd.choice(dispatchers)
        courier = rnd.choice(couriers)
        a = call("POST", f"{DISPATCH}/api/v1/dispatch/assignments",
                 {"parcelId": p["id"], "courierId": courier.courier_id}, token=dispatcher.token)
        p["assignment"] = a["id"]
        for status in steps[2:]:
            form = {"status": status}
            if status == "DELIVERED":
                form["recipientName"] = person() if rnd.random() < 0.3 else None
                form["note"] = rnd.choice(DELIVERY_NOTES)
                if rnd.random() < 0.6:
                    form["photo"] = PROOF_PNG
            if status == "FAILED":
                form["failureReason"] = rnd.choice(FAILURE_REASONS)
            call("POST", f"{DISPATCH}/api/v1/courier/deliveries/{a['id']}/status", token=courier.token,
                 multipart=form)
        if n % 25 == 0:
            log(f"  progressed {n}/{len(parcels)} parcels")


# ---------------------------------------------------------------------------------------------------------------
# Moving timelines into the past
# ---------------------------------------------------------------------------------------------------------------

def wait_for_events(expected_parcels):
    """Waits until parcel-service has sent every outbox event and notification-service has handled them."""
    for _ in range(120):
        pending = int(sql("parcel_db", "SELECT count(*) FROM outbox_events WHERE status <> 'SENT'")[0][0])
        sent = int(sql("parcel_db", "SELECT count(*) FROM outbox_events")[0][0])
        handled = int(sql("notification_db", "SELECT count(DISTINCT event_id) FROM processed_events")[0][0])
        if pending == 0 and handled >= sent:
            break
        time.sleep(2)
    else:
        raise RuntimeError(f"Events not processed: {pending} pending in the outbox, {handled}/{sent} handled")
    # Webhook deliveries to the (unreachable) mock merchant URLs are retried after 10 s, 1 min and 5 min, each
    # attempt adding a row. Wait until every event has a final row, or later retries would add rows dated today.
    log("  waiting for webhook retries to finish (about 7 minutes)...")
    for _ in range(600):
        open_events = int(sql("notification_db",
                              "SELECT count(*) FROM (SELECT event_id FROM webhook_deliveries GROUP BY event_id "
                              "HAVING bool_and(status NOT IN ('SENT', 'PARKED'))) open")[0][0])
        if open_events == 0:
            time.sleep(15)  # a last in-flight attempt may still be writing
            if int(sql("notification_db", "SELECT count(*) FROM (SELECT event_id FROM webhook_deliveries GROUP BY "
                                          "event_id HAVING bool_and(status NOT IN ('SENT', 'PARKED'))) open")[0][0]) == 0:
                return sent
        time.sleep(5)
    raise RuntimeError(f"{open_events} webhook events still being retried")


def shift(db, table, columns, where, delta_seconds):
    sets = ", ".join(f"{c} = {c} + make_interval(secs => {delta_seconds})" for c in columns)
    return f"UPDATE {table} SET {sets} WHERE {where};"


def backdate(by_role, merchants, parcels):
    events = sql("parcel_db", "SELECT event_id, aggregate_id, payload, extract(epoch FROM created_at) "
                              "FROM outbox_events ORDER BY seq")
    by_parcel = {}
    for event_id, parcel_id, payload, created_epoch in events:
        status = json.loads(payload)["toStatus"]
        by_parcel.setdefault(parcel_id, {})[status] = (event_id, float(created_epoch), payload)
    assignments = dict((r[0], float(r[1])) for r in sql("dispatch_db",
                                                         "SELECT parcel_id, extract(epoch FROM assigned_at) FROM assignments"))

    parcel_sql, dispatch_sql, merchant_sql, notif_sql = [], [], [], []
    for p in parcels:
        planned = dict(p["timeline"])
        actual = by_parcel.get(p["id"], {})
        last_delta = None
        for status, (event_id, created_epoch, payload) in actual.items():
            target = planned[status]
            delta = round(target.timestamp() - created_epoch, 3)
            if status == "CREATED":
                parcel_sql.append(shift("parcel_db", "parcels", ["created_at"], f"id = {q(p['id'])}", delta))
                merchant_sql.append(shift("merchant_db", "merchant_orders", ["created_at"],
                                          f"parcel_id = {q(p['id'])}", delta))
            parcel_sql.append(shift("parcel_db", "parcel_status_history", ["created_at", "updated_at"],
                                    f"parcel_id = {q(p['id'])} AND status = {q(status)}", delta))
            new_payload = json.loads(payload)
            new_payload["occurredAt"] = target.isoformat().replace("+00:00", "Z")
            parcel_sql.append(f"UPDATE outbox_events SET payload = {q(json.dumps(new_payload, separators=(',', ':')))} "
                              f"WHERE event_id = {q(event_id)};")
            parcel_sql.append(shift("parcel_db", "outbox_events", ["created_at", "sent_at", "next_attempt_at", "updated_at"],
                                    f"event_id = {q(event_id)}", delta))
            parcel_sql.append(shift("parcel_db", "processed_events", ["processed_at", "created_at", "updated_at"],
                                    f"event_id = {q(event_id)}", delta))
            for table, cols in (("notifications", ["created_at", "updated_at"]),
                                ("processed_events", ["processed_at", "created_at"]),
                                ("webhook_deliveries", ["created_at", "updated_at"])):
                notif_sql.append(shift("notification_db", table, cols, f"event_id = {q(event_id)}", delta))
            if status == "DELIVERED":
                dispatch_sql.append(shift("dispatch_db", "delivery_proofs", ["uploaded_at", "created_at", "updated_at"],
                                          f"assignment_id IN (SELECT id FROM assignments WHERE parcel_id = {q(p['id'])})",
                                          delta))
            last_delta = delta
        if last_delta is not None:
            parcel_sql.append(shift("parcel_db", "parcels", ["updated_at"], f"id = {q(p['id'])}", last_delta))
            merchant_sql.append(shift("merchant_db", "merchant_orders", ["updated_at"],
                                      f"parcel_id = {q(p['id'])}", last_delta))
            dispatch_sql.append(shift("dispatch_db", "assignments", ["updated_at"], f"parcel_id = {q(p['id'])}",
                                      last_delta))
        if p["id"] in assignments:
            delta = round(planned["ASSIGNED"].timestamp() - assignments[p["id"]], 3)
            dispatch_sql.append(shift("dispatch_db", "assignments", ["assigned_at", "created_at"],
                                      f"parcel_id = {q(p['id'])}", delta))

    # Accounts existed before the first parcel
    first_parcel = min(p["created"] for p in parcels)
    auth_sql = []
    for u in by_role_users(by_role):
        if u.role in ("ADMIN",):
            when = first_parcel - dt.timedelta(days=rnd.randint(110, 130))
        elif u.role in ("DISPATCHER", "COURIER"):
            when = first_parcel - dt.timedelta(days=rnd.randint(40, 100))
        elif u.role == "MERCHANT":
            when = first_parcel - dt.timedelta(days=rnd.randint(25, 60))
        else:
            when = first_parcel - dt.timedelta(days=rnd.randint(1, 80))
        when = working_time(when.astimezone(SL).date())
        u.created = when
        auth_sql.append(f"UPDATE users SET created_at = {ts(when)}, updated_at = {ts(when)} WHERE id = {q(u.id)};")
        auth_sql.append(f"UPDATE user_roles SET assigned_at = {ts(when)}, reviewed_at = "
                        f"CASE WHEN reviewed_at IS NULL THEN NULL ELSE {ts(when + dt.timedelta(hours=rnd.randint(2, 30)))} END "
                        f"WHERE user_id = {q(u.id)};")
    auth_sql.append("DELETE FROM refresh_tokens;")  # sessions from the generator run; sign in again
    for m in merchants:
        when = m["user"].created + dt.timedelta(hours=rnd.randint(1, 6))
        reviewed = when + dt.timedelta(hours=rnd.randint(3, 48))
        upd = ts(reviewed) if m["status"] != "PENDING" else "NULL"
        merchant_sql.append(f"UPDATE merchants SET created_at = {ts(when)}, updated_at = {upd} WHERE id = {q(m['id'])};")
        merchant_sql.append(f"UPDATE merchant_api_keys SET created_at = {ts(reviewed + dt.timedelta(hours=1))}, "
                            f"updated_at = NULL WHERE merchant_id = {q(m['id'])};")
        merchant_sql.append(f"UPDATE merchant_webhook_endpoints SET created_at = {ts(reviewed + dt.timedelta(hours=2))}, "
                            f"updated_at = NULL WHERE merchant_id = {q(m['id'])};")
    courier_sql = [f"UPDATE couriers SET created_at = {ts(c.created)}, updated_at = NULL WHERE user_id = {q(c.id)};"
                   for c in by_role["COURIER"]]

    # One row per courier and Sri Lanka day, from the assignments (dispatch's reports read assignments)
    stats_sql = """
        DELETE FROM daily_stats;
        INSERT INTO daily_stats (date, courier_id, assigned_count, completed_count, failed_count, created_at, updated_at)
        SELECT (assigned_at AT TIME ZONE 'Asia/Colombo')::date, courier_id, count(*),
               count(*) FILTER (WHERE status = 'COMPLETED'), count(*) FILTER (WHERE status = 'FAILED'),
               max(assigned_at), max(coalesce(updated_at, assigned_at))
        FROM assignments GROUP BY 1, 2;"""

    for db, stmts in (("parcel_db", parcel_sql), ("dispatch_db", dispatch_sql + courier_sql + [stats_sql]),
                      ("merchant_db", merchant_sql), ("notification_db", notif_sql), ("auth_db", auth_sql)):
        sql(db, "BEGIN;\n" + "\n".join(stmts) + "\nCOMMIT;")
        log(f"  {db}: {len(stmts)} updates")


def by_role_users(by_role):
    return [u for users in by_role.values() for u in users]


# ---------------------------------------------------------------------------------------------------------------

def main():
    for db in ("auth_db", "parcel_db", "dispatch_db", "merchant_db", "notification_db"):
        tables = sql(db, "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'")[0][0]
        if int(tables) < 2:
            sys.exit(f"{db} has no tables yet: start the services once so Flyway creates them.")
    if int(sql("auth_db", "SELECT count(*) FROM users")[0][0]) > 0:
        sys.exit("auth_db already has users. Run this on fresh databases (see README.md).")

    log("Creating users...")
    users = make_users()
    for u in users:
        u.signup()
    by_role = setup_roles(users)
    for u in users:
        u.login()  # new tokens carry the new roles
    log(f"  {len(users)} users")

    log("Adding promo codes...")
    setup_promos(by_role)

    log("Registering merchants...")
    merchants = setup_merchants(by_role)

    log("Booking parcels...")
    parcels = create_parcels(by_role, merchants, customer_count=170, merchant_count=80)
    log(f"  {len(parcels)} parcels")

    log("Assigning and delivering...")
    for u in by_role["DISPATCHER"] + by_role["COURIER"]:
        u.login()
    progress_parcels(by_role, parcels)

    log("Waiting for events and notifications...")
    log(f"  {wait_for_events(len(parcels))} events processed")

    log("Moving timelines into the past...")
    backdate(by_role, merchants, parcels)
    log("Done.")


if __name__ == "__main__":
    main()
