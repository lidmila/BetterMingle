import csv
import sys

INPUT = r"C:\Users\lidmi\Downloads\data_safety_export (1).csv"
OUTPUT = r"C:\Users\lidmi\Downloads\data_safety_export_filled.csv"

rows = []
with open(INPUT, "r", encoding="utf-8") as f:
    reader = csv.reader(f)
    for row in reader:
        rows.append(row)

header = rows[0]

def set_val(question_id, response_id, value):
    for row in rows[1:]:
        if row[0] == question_id and (response_id is None or row[1] == response_id):
            row[2] = value
            return True
    return False

# ============================================================
# SECTION 1: GENERAL QUESTIONS
# ============================================================

# App collects/shares personal data
set_val("PSL_DATA_COLLECTION_COLLECTS_PERSONAL_DATA", None, "TRUE")

# All data encrypted in transit (Firebase/HTTPS)
set_val("PSL_DATA_COLLECTION_ENCRYPTED_IN_TRANSIT", None, "TRUE")

# Account creation methods - Email+Password
set_val("PSL_SUPPORTED_ACCOUNT_CREATION_METHODS", "PSL_ACM_USER_ID_PASSWORD", "TRUE")
set_val("PSL_SUPPORTED_ACCOUNT_CREATION_METHODS", "PSL_ACM_USER_ID_OTHER_AUTH", "FALSE")
set_val("PSL_SUPPORTED_ACCOUNT_CREATION_METHODS", "PSL_ACM_USER_ID_PASSWORD_OTHER_AUTH", "FALSE")
set_val("PSL_SUPPORTED_ACCOUNT_CREATION_METHODS", "PSL_ACM_OAUTH", "TRUE")  # Google Sign-In
set_val("PSL_SUPPORTED_ACCOUNT_CREATION_METHODS", "PSL_ACM_OTHER", "FALSE")
set_val("PSL_SUPPORTED_ACCOUNT_CREATION_METHODS", "PSL_ACM_NONE", "FALSE")

# Specify account creation
set_val("PSL_ACM_SPECIFY", None, "")

# Account deletion URL
set_val("PSL_ACCOUNT_DELETION_URL", None, "https://www.codewhiskers.app/smazani-uctu")

# Support data deletion
set_val("PSL_SUPPORT_DATA_DELETION_BY_USER", "DATA_DELETION_YES", "TRUE")
set_val("PSL_SUPPORT_DATA_DELETION_BY_USER", "DATA_DELETION_NO", "FALSE")
set_val("PSL_SUPPORT_DATA_DELETION_BY_USER", "DATA_DELETION_NO_AUTO_DELETED", "FALSE")

# Data deletion URL
set_val("PSL_DATA_DELETION_URL", None, "https://www.codewhiskers.app/smazani-uctu")

# Family policy - DO NOT answer (not applicable)
# set_val("PSL_DATA_COLLECTION_COMPLIES_FAMILY_POLICY", None, ...)

# MASA validation
set_val("PSL_INDEPENDENTLY_VALIDATED", None, "FALSE")

# UPI badge
set_val("PSL_UPI_BADGE_OPT_IN", None, "FALSE")

# Outside app accounts - DO NOT answer (not applicable)
# set_val("PSL_HAS_OUTSIDE_APP_ACCOUNTS", None, ...)

# Outside app account types - DO NOT answer (not applicable)
# set_val("PSL_OUTSIDE_APP_ACCOUNT_TYPES", ...)
# set_val("PSL_OUTSIDE_APP_ACCOUNT_TYPE_SPECIFY", ...)

# ============================================================
# SECTION 2: DATA TYPES COLLECTED
# ============================================================

# Personal data
set_val("PSL_DATA_TYPES_PERSONAL", "PSL_NAME", "TRUE")           # Display name
set_val("PSL_DATA_TYPES_PERSONAL", "PSL_EMAIL", "TRUE")          # Email for auth
set_val("PSL_DATA_TYPES_PERSONAL", "PSL_USER_ACCOUNT", "TRUE")   # User IDs (Firebase UID)
set_val("PSL_DATA_TYPES_PERSONAL", "PSL_ADDRESS", "FALSE")       # No home address
set_val("PSL_DATA_TYPES_PERSONAL", "PSL_PHONE", "TRUE")          # Phone number in profile
set_val("PSL_DATA_TYPES_PERSONAL", "PSL_RACE_ETHNICITY", "FALSE")
set_val("PSL_DATA_TYPES_PERSONAL", "PSL_POLITICAL_RELIGIOUS", "FALSE")
set_val("PSL_DATA_TYPES_PERSONAL", "PSL_SEXUAL_ORIENTATION_GENDER_IDENTITY", "FALSE")
set_val("PSL_DATA_TYPES_PERSONAL", "PSL_OTHER_PERSONAL", "FALSE")

# Financial data
set_val("PSL_DATA_TYPES_FINANCIAL", "PSL_CREDIT_DEBIT_BANK_ACCOUNT_NUMBER", "FALSE")
set_val("PSL_DATA_TYPES_FINANCIAL", "PSL_PURCHASE_HISTORY", "TRUE")   # In-app purchases (premium)
set_val("PSL_DATA_TYPES_FINANCIAL", "PSL_CREDIT_SCORE", "FALSE")
set_val("PSL_DATA_TYPES_FINANCIAL", "PSL_OTHER", "FALSE")

# Location
set_val("PSL_DATA_TYPES_LOCATION", "PSL_APPROX_LOCATION", "TRUE")   # Event/carpool locations via Places API
set_val("PSL_DATA_TYPES_LOCATION", "PSL_PRECISE_LOCATION", "FALSE")  # No GPS permission

# Search and browsing
set_val("PSL_DATA_TYPES_SEARCH_AND_BROWSING", "PSL_WEB_BROWSING_HISTORY", "FALSE")

# Messages
set_val("PSL_DATA_TYPES_EMAIL_AND_TEXT", "PSL_EMAILS", "FALSE")
set_val("PSL_DATA_TYPES_EMAIL_AND_TEXT", "PSL_SMS_CALL_LOG", "FALSE")
set_val("PSL_DATA_TYPES_EMAIL_AND_TEXT", "PSL_OTHER_MESSAGES", "TRUE")  # In-app chat

# Photos and videos
set_val("PSL_DATA_TYPES_PHOTOS_AND_VIDEOS", "PSL_PHOTOS", "TRUE")   # Avatars, event photos
set_val("PSL_DATA_TYPES_PHOTOS_AND_VIDEOS", "PSL_VIDEOS", "FALSE")

# Audio
set_val("PSL_DATA_TYPES_AUDIO", "PSL_AUDIO", "FALSE")
set_val("PSL_DATA_TYPES_AUDIO", "PSL_MUSIC", "FALSE")
set_val("PSL_DATA_TYPES_AUDIO", "PSL_OTHER_AUDIO", "FALSE")

# Health and fitness
set_val("PSL_DATA_TYPES_HEALTH_AND_FITNESS", "PSL_HEALTH", "FALSE")
set_val("PSL_DATA_TYPES_HEALTH_AND_FITNESS", "PSL_FITNESS", "FALSE")

# Contacts
set_val("PSL_DATA_TYPES_CONTACTS", "PSL_CONTACTS", "FALSE")

# Calendar
set_val("PSL_DATA_TYPES_CALENDAR", "PSL_CALENDAR", "FALSE")

# App performance
set_val("PSL_DATA_TYPES_APP_PERFORMANCE", "PSL_CRASH_LOGS", "TRUE")        # Firebase Crashlytics
set_val("PSL_DATA_TYPES_APP_PERFORMANCE", "PSL_PERFORMANCE_DIAGNOSTICS", "TRUE")  # Firebase/PostHog
set_val("PSL_DATA_TYPES_APP_PERFORMANCE", "PSL_OTHER_PERFORMANCE", "FALSE")

# Files and docs
set_val("PSL_DATA_TYPES_FILES_AND_DOCS", "PSL_FILES_AND_DOCS", "FALSE")

# App activity
set_val("PSL_DATA_TYPES_APP_ACTIVITY", "PSL_USER_INTERACTION", "TRUE")        # PostHog analytics
set_val("PSL_DATA_TYPES_APP_ACTIVITY", "PSL_IN_APP_SEARCH_HISTORY", "FALSE")
set_val("PSL_DATA_TYPES_APP_ACTIVITY", "PSL_APPS_ON_DEVICE", "FALSE")
set_val("PSL_DATA_TYPES_APP_ACTIVITY", "PSL_USER_GENERATED_CONTENT", "TRUE")  # Events, polls, expenses, ratings
set_val("PSL_DATA_TYPES_APP_ACTIVITY", "PSL_OTHER_APP_ACTIVITY", "FALSE")

# Device identifiers
set_val("PSL_DATA_TYPES_IDENTIFIERS", "PSL_DEVICE_ID", "TRUE")  # Firebase/AdMob device ID

# ============================================================
# SECTION 3: DATA USAGE RESPONSES per data type
# ============================================================

# Helper to set all usage responses for a data type
def set_collected_only(data_type, ephemeral="FALSE", user_control_required=True,
                       purposes=None, sharing_purposes=None):
    prefix = f"PSL_DATA_USAGE_RESPONSES:{data_type}"

    # Collection and sharing
    set_val(f"{prefix}:PSL_DATA_USAGE_COLLECTION_AND_SHARING", "PSL_DATA_USAGE_ONLY_COLLECTED", "TRUE")
    if sharing_purposes:
        set_val(f"{prefix}:PSL_DATA_USAGE_COLLECTION_AND_SHARING", "PSL_DATA_USAGE_ONLY_SHARED", "TRUE")
    else:
        set_val(f"{prefix}:PSL_DATA_USAGE_COLLECTION_AND_SHARING", "PSL_DATA_USAGE_ONLY_SHARED", "FALSE")

    # Ephemeral
    set_val(f"{prefix}:PSL_DATA_USAGE_EPHEMERAL", None, ephemeral)

    # User control
    if user_control_required:
        set_val(f"{prefix}:DATA_USAGE_USER_CONTROL", "PSL_DATA_USAGE_USER_CONTROL_OPTIONAL", "FALSE")
        set_val(f"{prefix}:DATA_USAGE_USER_CONTROL", "PSL_DATA_USAGE_USER_CONTROL_REQUIRED", "TRUE")
    else:
        set_val(f"{prefix}:DATA_USAGE_USER_CONTROL", "PSL_DATA_USAGE_USER_CONTROL_OPTIONAL", "TRUE")
        set_val(f"{prefix}:DATA_USAGE_USER_CONTROL", "PSL_DATA_USAGE_USER_CONTROL_REQUIRED", "FALSE")

    # Collection purposes
    all_purposes = ["PSL_APP_FUNCTIONALITY", "PSL_ANALYTICS", "PSL_DEVELOPER_COMMUNICATIONS",
                    "PSL_FRAUD_PREVENTION_SECURITY", "PSL_ADVERTISING", "PSL_PERSONALIZATION",
                    "PSL_ACCOUNT_MANAGEMENT"]
    if purposes is None:
        purposes = []
    for p in all_purposes:
        set_val(f"{prefix}:DATA_USAGE_COLLECTION_PURPOSE", p, "TRUE" if p in purposes else "FALSE")

    # Sharing purposes
    if sharing_purposes is None:
        sharing_purposes = []
    for p in all_purposes:
        set_val(f"{prefix}:DATA_USAGE_SHARING_PURPOSE", p, "TRUE" if p in sharing_purposes else "FALSE")


# --- NAME ---
set_collected_only("PSL_NAME", ephemeral="FALSE", user_control_required=True,
    purposes=["PSL_APP_FUNCTIONALITY", "PSL_ACCOUNT_MANAGEMENT", "PSL_PERSONALIZATION"],
    sharing_purposes=None)

# --- EMAIL ---
set_collected_only("PSL_EMAIL", ephemeral="FALSE", user_control_required=True,
    purposes=["PSL_APP_FUNCTIONALITY", "PSL_ACCOUNT_MANAGEMENT", "PSL_DEVELOPER_COMMUNICATIONS",
              "PSL_FRAUD_PREVENTION_SECURITY"],
    sharing_purposes=None)

# --- USER ACCOUNT (User IDs) ---
set_collected_only("PSL_USER_ACCOUNT", ephemeral="FALSE", user_control_required=True,
    purposes=["PSL_APP_FUNCTIONALITY", "PSL_ANALYTICS", "PSL_ACCOUNT_MANAGEMENT",
              "PSL_FRAUD_PREVENTION_SECURITY"],
    sharing_purposes=["PSL_ANALYTICS"])

# --- PHONE ---
set_collected_only("PSL_PHONE", ephemeral="FALSE", user_control_required=False,
    purposes=["PSL_APP_FUNCTIONALITY", "PSL_ACCOUNT_MANAGEMENT"],
    sharing_purposes=None)

# --- PURCHASE HISTORY ---
set_collected_only("PSL_PURCHASE_HISTORY", ephemeral="FALSE", user_control_required=True,
    purposes=["PSL_APP_FUNCTIONALITY", "PSL_ACCOUNT_MANAGEMENT"],
    sharing_purposes=None)

# --- APPROX LOCATION ---
set_collected_only("PSL_APPROX_LOCATION", ephemeral="FALSE", user_control_required=False,
    purposes=["PSL_APP_FUNCTIONALITY"],
    sharing_purposes=None)

# --- OTHER MESSAGES (in-app chat) ---
set_collected_only("PSL_OTHER_MESSAGES", ephemeral="FALSE", user_control_required=False,
    purposes=["PSL_APP_FUNCTIONALITY"],
    sharing_purposes=None)

# --- PHOTOS ---
set_collected_only("PSL_PHOTOS", ephemeral="FALSE", user_control_required=False,
    purposes=["PSL_APP_FUNCTIONALITY", "PSL_PERSONALIZATION"],
    sharing_purposes=None)

# --- CRASH LOGS ---
set_collected_only("PSL_CRASH_LOGS", ephemeral="FALSE", user_control_required=True,
    purposes=["PSL_ANALYTICS"],
    sharing_purposes=["PSL_ANALYTICS"])

# --- PERFORMANCE DIAGNOSTICS ---
set_collected_only("PSL_PERFORMANCE_DIAGNOSTICS", ephemeral="FALSE", user_control_required=True,
    purposes=["PSL_ANALYTICS"],
    sharing_purposes=["PSL_ANALYTICS"])

# --- USER INTERACTION (app activity) ---
set_collected_only("PSL_USER_INTERACTION", ephemeral="FALSE", user_control_required=True,
    purposes=["PSL_ANALYTICS", "PSL_APP_FUNCTIONALITY"],
    sharing_purposes=["PSL_ANALYTICS"])

# --- USER GENERATED CONTENT ---
set_collected_only("PSL_USER_GENERATED_CONTENT", ephemeral="FALSE", user_control_required=False,
    purposes=["PSL_APP_FUNCTIONALITY"],
    sharing_purposes=None)

# --- DEVICE ID ---
set_collected_only("PSL_DEVICE_ID", ephemeral="FALSE", user_control_required=True,
    purposes=["PSL_ANALYTICS", "PSL_ADVERTISING", "PSL_FRAUD_PREVENTION_SECURITY"],
    sharing_purposes=["PSL_ANALYTICS", "PSL_ADVERTISING"])

# ============================================================
# SECTION 4: Set FALSE for all unused data type usage responses
# ============================================================

# Data types NOT collected - leave their usage responses EMPTY
# Google Play says: "you cannot answer questions" for non-collected types
# So we must NOT set any value for these prefixes.
not_collected_types = [
    "PSL_ADDRESS", "PSL_RACE_ETHNICITY", "PSL_POLITICAL_RELIGIOUS",
    "PSL_SEXUAL_ORIENTATION_GENDER_IDENTITY", "PSL_OTHER_PERSONAL",
    "PSL_CREDIT_DEBIT_BANK_ACCOUNT_NUMBER", "PSL_CREDIT_SCORE", "PSL_OTHER",
    "PSL_PRECISE_LOCATION", "PSL_WEB_BROWSING_HISTORY",
    "PSL_EMAILS", "PSL_SMS_CALL_LOG",
    "PSL_VIDEOS", "PSL_AUDIO", "PSL_MUSIC", "PSL_OTHER_AUDIO",
    "PSL_HEALTH", "PSL_FITNESS", "PSL_CONTACTS", "PSL_CALENDAR",
    "PSL_OTHER_PERFORMANCE", "PSL_FILES_AND_DOCS",
    "PSL_IN_APP_SEARCH_HISTORY", "PSL_APPS_ON_DEVICE", "PSL_OTHER_APP_ACTIVITY"
]

# Ensure these rows stay empty (clear any accidentally set values)
# Use ":dt:" to avoid prefix conflicts (e.g. PSL_OTHER matching PSL_OTHER_MESSAGES)
for dt in not_collected_types:
    prefix = f"PSL_DATA_USAGE_RESPONSES:{dt}:"
    for row in rows[1:]:
        if row[0].startswith(prefix):
            row[2] = ""

# Also clear optional/non-applicable questions
for row in rows[1:]:
    qid = row[0]
    if qid in ("PSL_DATA_COLLECTION_COMPLIES_FAMILY_POLICY", "PSL_HAS_OUTSIDE_APP_ACCOUNTS"):
        row[2] = ""
    if qid in ("PSL_OUTSIDE_APP_ACCOUNT_TYPES", "PSL_OUTSIDE_APP_ACCOUNT_TYPE_SPECIFY"):
        row[2] = ""

# ============================================================
# Write output
# ============================================================
with open(OUTPUT, "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f)
    for row in rows:
        writer.writerow(row)

print(f"Done! Written to: {OUTPUT}")
print(f"Total rows: {len(rows)}")

# Verify: count filled vs empty
filled = sum(1 for r in rows[1:] if r[2] != "")
empty = sum(1 for r in rows[1:] if r[2] == "")
print(f"Filled: {filled}, Empty: {empty}")
