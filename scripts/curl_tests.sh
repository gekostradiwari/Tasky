#!/usr/bin/env bash
set -euo pipefail

# Extended test suite (updated for Marshmallow validation & visibility endpoints)
# Planned additions:
# - Negative login cases: only email, only password, empty body
# - Validation errors: missing required field for project/task/register returning fields map
# - Task visibility: manager with non-existent project id returns count 0 (already partly covered)
# - Project visibility: dipendente with dept having no tasks returns count (already covered) but add missing id_progetto/id_dipartimento raw empty JSON tests
# - Optional: attempt creating task with invalid foreign key (id_progetto huge) expect 400/409 depending on DB constraint mapping
# NOTE: Some FK violation scenarios depend on actual DB state; if not deterministic they are skipped.

# Usage: API_PORT=5001 ./scripts/curl_tests.sh
BASE=${BASE:-http://127.0.0.1:${API_PORT:-5001}}

echo "Using base URL: $BASE"

RAND=$(date +%s)
DIP_EMAIL="dip_${RAND}@example.com"
MGR_EMAIL="mgr_${RAND}@example.com"
DIP2_EMAIL="dip2_${RAND}@example.com"

json_post() {
  # $1 = path, $2 = json body
  local path="${1:-}" payload="${2:-}"
  curl -s -w "\n%{http_code}" -X POST "$BASE$path" -H "Content-Type: application/json" -d "$payload"
}

extract_token() {
  local body="${1:-}"
  echo "$body" | tr -d '\n' | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p'
}

extract_number() {
  local body="${1:-}" key="${2:-}"
  [ -z "$key" ] && return 0
  echo "$body" | tr -d '\n' | sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p"
}

extract_string() {
  # $1 = body, $2 = key -> returns first string value for key
  local body="${1:-}" key="${2:-}"
  [ -z "$key" ] && return 0
  echo "$body" | tr -d '\n' | sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p"
}

handle_register() {
  # $1 = path, $2 = json body, $3 = friendly name
  resp=$(json_post "$1" "$2")
  echo "$resp"
  code=$(echo "$resp" | tail -n1)
  body=$(echo "$resp" | sed '$d')

  if [ "$code" -eq 409 ]; then
    echo "$3 already registered, attempting login to retrieve token..."
    # Choose correct email to login (fallback assumes known password)
    if [ "$3" = "Manager" ]; then
      login_email="$MGR_EMAIL"
    else
      login_email="$DIP_EMAIL"
    fi
    login_resp=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"email\":\"$login_email\",\"password\":\"pwd\"}")
    login_code=$(echo "$login_resp" | tail -n1)
    login_body=$(echo "$login_resp" | sed '$d')
    if [ "$login_code" -ne 200 ]; then
      echo "Login after duplicate failed (status $login_code)"; echo "$login_body"; exit 1
    fi
    token=$(extract_token "$login_body")
    echo "$3 token: $token"
    echo "$token"
    return 0
  fi

  if [ "$code" -ne 201 ] && [ "$code" -ne 409 ]; then
    echo "Register $3 failed (expected 201 or 409 duplicate, got $code)"; echo "$body"; exit 1
  fi

  token=$(extract_token "$body")
  echo "$3 token: $token"
  echo "$token"
}

# Register Dipendente
cat <<EOF

=== Register Dipendente ===
EOF
DIP_PAYLOAD="{\"email\":\"$DIP_EMAIL\",\"password\":\"pwd\",\"nome\":\"Nome\",\"cognome\":\"Cognome\",\"data_nascita\":\"1990-01-01\",\"sesso\":\"M\",\"numero_telefono\":\"+39 333 1111111\",\"Dipartimento_id_dipartimento\":1}"
DIP_ALL=$(handle_register "/api/register/dipendente" "$DIP_PAYLOAD" "Dipendente")
echo "$DIP_ALL"
DIP_TOKEN=$(echo "$DIP_ALL" | tail -n1)
# Trim CR/LF and other control characters from token
DIP_TOKEN=$(echo "$DIP_TOKEN" | tr -d '\r\n')

# If token empty, attempt credential login as a fallback
if [ -z "$DIP_TOKEN" ]; then
  echo "No token returned from register, attempting credential login..."
  login_resp=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"email\":\"$DIP_EMAIL\",\"password\":\"pwd\"}")
  login_code=$(echo "$login_resp" | tail -n1)
  login_body=$(echo "$login_resp" | sed '$d')
  if [ "$login_code" -ne 200 ]; then
    echo "Credential login failed (status $login_code)"; echo "$login_body"; exit 1
  fi
  DIP_TOKEN=$(extract_token "$login_body")
  DIP_TOKEN=$(echo "$DIP_TOKEN" | tr -d '\r\n')
fi

cat <<EOF

=== Login Dipendente with token ===
EOF
curl -s -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"token\":\"$DIP_TOKEN\"}" | cat

cat <<EOF

=== Login Dipendente with email/password ===
EOF
curl -s -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"email\":\"$DIP_EMAIL\",\"password\":\"pwd\"}" | cat

# Attempt duplicate registration for Dipendente (expect 409)
cat <<EOF

=== Re-register Dipendente (should be duplicate) ===
EOF
dup_resp=$(json_post "/api/register/dipendente" "$DIP_PAYLOAD")
echo "$dup_resp"
dup_code=$(echo "$dup_resp" | tail -n1)
dup_body=$(echo "$dup_resp" | sed '$d')
if [ "$dup_code" -ne 409 ]; then
  echo "Expected 409 on duplicate dipendente register but got $dup_code"; exit 1
fi

cat <<EOF

=== Login Dipendente after duplicate check (token) ===
EOF
curl -s -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"token\":\"$DIP_TOKEN\"}" | cat

cat <<EOF

=== Login Dipendente after duplicate check (email/password) ===
EOF
curl -s -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"email\":\"$DIP_EMAIL\",\"password\":\"pwd\"}" | cat

# Register Manager
cat <<EOF

=== Register Manager ===
EOF
MGR_PAYLOAD="{\"email\":\"$MGR_EMAIL\",\"password\":\"pwd\",\"nome\":\"Mario\",\"cognome\":\"Rossi\",\"data_nascita\":\"1980-01-01\",\"sesso\":\"M\",\"numero_telefono\":\"+39 333 2222222\",\"anni_lavorativi\":5,\"Dipartimento_id_dipartimento\":1}"
MGR_ALL=$(handle_register "/api/register/manager" "$MGR_PAYLOAD" "Manager")
echo "$MGR_ALL"
MGR_TOKEN=$(echo "$MGR_ALL" | tail -n1)
MGR_TOKEN=$(echo "$MGR_TOKEN" | tr -d '\r\n')
if [ -z "$MGR_TOKEN" ]; then
  echo "No token returned from manager register, attempting credential login..."
  login_resp=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"email\":\"$MGR_EMAIL\",\"password\":\"pwd\"}")
  login_code=$(echo "$login_resp" | tail -n1)
  login_body=$(echo "$login_resp" | sed '$d')
  if [ "$login_code" -ne 200 ]; then
    echo "Credential login failed (status $login_code)"; echo "$login_body"; exit 1
  fi
  MGR_TOKEN=$(extract_token "$login_body")
  MGR_TOKEN=$(echo "$MGR_TOKEN" | tr -d '\r\n')
fi

cat <<EOF

=== Crea Dipartimento (Manager) ===
EOF
DEPT_NAME="Dept_${RAND}"
DEPT_PAYLOAD="{\"token\":\"$MGR_TOKEN\",\"nome\":\"$DEPT_NAME\",\"numero_dipendenti\":0}"
dept_resp=$(json_post "/api/add/Department" "$DEPT_PAYLOAD")
echo "$dept_resp"
dept_code=$(echo "$dept_resp" | tail -n1)
dept_body=$(echo "$dept_resp" | sed '$d')
if [ "$dept_code" -ne 201 ]; then
  echo "Expected 201 creating department, got $dept_code"; echo "$dept_body"; exit 1
fi
dept_id=$(echo "$dept_body" | tr -d '\n' | sed -n 's/.*"id_dipartimento"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p')
echo "Created department id: $dept_id"

cat <<EOF

=== Crea Dipartimento duplicato (should be 409 or handled) ===
EOF
DEPT_DUP_PAYLOAD="{\"token\":\"$MGR_TOKEN\",\"id_dipartimento\":$dept_id,\"nome\":\"$DEPT_NAME\"}"
dup_dept_resp=$(json_post "/api/add/Department" "$DEPT_DUP_PAYLOAD")
echo "$dup_dept_resp"
dup_dept_code=$(echo "$dup_dept_resp" | tail -n1)
dup_dept_body=$(echo "$dup_dept_resp" | sed '$d')
# Accept either 201 (insert with same id allowed) or 409 on duplicate or 400 on integrity
if [ "$dup_dept_code" -ne 201 ] && [ "$dup_dept_code" -ne 409 ] && [ "$dup_dept_code" -ne 400 ]; then
  echo "Unexpected status for duplicate department insert: $dup_dept_code"; echo "$dup_dept_body"; exit 1
fi

cat <<EOF

=== Negative: Dipendente tenta creare Dipartimento (should 403) ===
EOF
bad_dept_payload="{\"token\":\"$DIP_TOKEN\",\"nome\":\"BadDept\"}"
bad_dept_resp=$(json_post "/api/add/Department" "$bad_dept_payload")
echo "$bad_dept_resp"
bad_dept_code=$(echo "$bad_dept_resp" | tail -n1)
if [ "$bad_dept_code" -ne 403 ]; then
  echo "Expected 403 when Dipendente attempts to create department, got $bad_dept_code"; exit 1
fi

cat <<EOF

=== Login Manager with token ===
EOF
curl -s -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"token\":\"$MGR_TOKEN\"}" | cat

cat <<EOF

=== Login Manager with email/password ===
EOF
curl -s -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"email\":\"$MGR_EMAIL\",\"password\":\"pwd\"}" | cat

cat <<EOF

=== Numero Dipendenti (manager scope) ===
EOF
num_dip_resp=$(json_post "/api/numeroDipendenti" "{\"token\":\"$MGR_TOKEN\",\"id_dipartimento\":1}")
echo "$num_dip_resp"
num_dip_code=$(echo "$num_dip_resp" | tail -n1)
num_dip_body=$(echo "$num_dip_resp" | sed '$d')
if [ "$num_dip_code" -ne 200 ]; then
  echo "Expected 200 for numeroDipendenti initial, got $num_dip_code"; exit 1
fi
num_count=$(extract_number "$num_dip_body" "n_dipendenti")
if [ -z "$num_count" ] || [ "$num_count" -lt 1 ]; then
  echo "Expected n_dipendenti >=1 after first dipendente, got $num_count"; exit 1
fi

cat <<EOF

=== Numero Dipendenti (dipendente token should 403) ===
EOF
num_dip_forbidden=$(json_post "/api/numeroDipendenti" "{\"token\":\"$DIP_TOKEN\",\"id_dipartimento\":1}")
echo "$num_dip_forbidden"
num_dip_forbidden_code=$(echo "$num_dip_forbidden" | tail -n1)
if [ "$num_dip_forbidden_code" -ne 403 ]; then
  echo "Expected 403 for numeroDipendenti with dipendente token, got $num_dip_forbidden_code"; exit 1
fi

cat <<EOF

=== Numero Dipendenti (missing id_dipartimento) ===
EOF
num_dip_missing=$(json_post "/api/numeroDipendenti" "{\"token\":\"$MGR_TOKEN\"}")
echo "$num_dip_missing"
num_dip_missing_code=$(echo "$num_dip_missing" | tail -n1)
if [ "$num_dip_missing_code" -ne 400 ]; then
  echo "Expected 400 for numeroDipendenti missing id_dipartimento, got $num_dip_missing_code"; exit 1
fi

cat <<EOF

All done.
EOF

cat <<EOF

=== Dipendenti per Dipartimento (manager scope) ===
EOF
dip_resp=$(json_post "/api/dipendenti/by-department" "{\"token\":\"$MGR_TOKEN\",\"id_dipartimento\":1}")
echo "$dip_resp"
dip_code=$(echo "$dip_resp" | tail -n1)
dip_body=$(echo "$dip_resp" | sed '$d')
if [ "$dip_code" -ne 200 ]; then
  echo "Expected 200 for dipendenti/by-department initial, got $dip_code"; echo "$dip_body"; exit 1
fi
dip_count=$(echo "$dip_body" | tr -d '\n' | sed -n 's/.*"count"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p')
if [ -z "$dip_count" ] || [ "$dip_count" -lt 1 ]; then
  echo "Expected count >=1 after creating dipendenti, got $dip_count"; exit 1
fi

cat <<EOF

=== Dipendenti per Dipartimento (missing id_dipartimento) ===
EOF
dip_missing=$(json_post "/api/dipendenti/by-department" "{\"token\":\"$MGR_TOKEN\"}")
echo "$dip_missing"
dip_missing_code=$(echo "$dip_missing" | tail -n1)
if [ "$dip_missing_code" -ne 400 ]; then
  echo "Expected 400 for dipendenti/by-department missing id_dipartimento, got $dip_missing_code"; exit 1
fi

cat <<EOF

=== Dipendenti per Dipartimento (dipendente token should 403) ===
EOF
dip_forbidden=$(json_post "/api/dipendenti/by-department" "{\"token\":\"$DIP_TOKEN\",\"id_dipartimento\":1}")
echo "$dip_forbidden"
dip_forbidden_code=$(echo "$dip_forbidden" | tail -n1)
if [ "$dip_forbidden_code" -ne 403 ]; then
  echo "Expected 403 for dipendenti/by-department with dipendente token, got $dip_forbidden_code"; exit 1
fi

cat <<EOF

=== Dipendenti per Dipartimento (manager other dept should 403) ===
EOF
dip_other=$(json_post "/api/dipendenti/by-department" "{\"token\":\"$MGR_TOKEN\",\"id_dipartimento\":999}")

cat <<EOF

=== Dipendenti DATA per Dipartimento (manager scope) ===
EOF
dip_data_resp=$(json_post "/api/dipendenti/data/by-department" "{\"token\":\"$MGR_TOKEN\",\"id_dipartimento\":1}")
echo "$dip_data_resp"
dip_data_code=$(echo "$dip_data_resp" | tail -n1)
dip_data_body=$(echo "$dip_data_resp" | sed '$d')
if [ "$dip_data_code" -ne 200 ]; then
  echo "Expected 200 for dipendenti data initial, got $dip_data_code"; echo "$dip_data_body"; exit 1
fi
dip_data_count=$(echo "$dip_data_body" | tr -d '\n' | sed -n 's/.*"count"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p')
if [ -z "$dip_data_count" ] || [ "$dip_data_count" -lt 1 ]; then
  echo "Expected count >=1 for dipendenti data, got $dip_data_count"; exit 1
fi
echo "$dip_other"
dip_other_code=$(echo "$dip_other" | tail -n1)
if [ "$dip_other_code" -ne 403 ]; then
  echo "Expected 403 for manager querying other department, got $dip_other_code"; exit 1
fi



# Attempt duplicate registration for Manager (expect 409)
cat <<EOF

=== Re-register Manager (should be duplicate) ===
EOF
dup_mgr_resp=$(json_post "/api/register/manager" "$MGR_PAYLOAD")
echo "$dup_mgr_resp"
dup_mgr_code=$(echo "$dup_mgr_resp" | tail -n1)
dup_mgr_body=$(echo "$dup_mgr_resp" | sed '$d')
if [ "$dup_mgr_code" -ne 409 ]; then
  echo "Expected 409 on duplicate manager register but got $dup_mgr_code"; exit 1
fi

cat <<EOF

=== Login Manager after duplicate check (token) ===
EOF
curl -s -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"token\":\"$MGR_TOKEN\"}" | cat

cat <<EOF

=== Login Manager after duplicate check (email/password) ===
EOF
curl -s -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"email\":\"$MGR_EMAIL\",\"password\":\"pwd\"}" | cat

# Negative tests: invalid tokens and wrong credentials
cat <<EOF

=== Negative tests: invalid token / wrong credentials ===
EOF

# Invalid token (expect 403)
BAD_TOKEN="this_is_an_invalid_token_12345"
bad_resp=$(json_post "/api/login" "{\"token\":\"$BAD_TOKEN\"}")
echo "$bad_resp"
bad_code=$(echo "$bad_resp" | tail -n1)
if [ "$bad_code" -ne 403 ]; then
  echo "Expected 403 for invalid token but got $bad_code"; exit 1
fi

# Wrong credentials for Dipendente (expect 401)
bad_cred_resp=$(json_post "/api/login" "{\"email\":\"$DIP_EMAIL\",\"password\":\"wrongpwd\"}")
echo "$bad_cred_resp"
bad_cred_code=$(echo "$bad_cred_resp" | tail -n1)
if [ "$bad_cred_code" -ne 401 ]; then
  echo "Expected 401 for wrong credentials (Dipendente) but got $bad_cred_code"; exit 1
fi

# Wrong credentials for Manager (expect 401)
bad_mgr_resp=$(json_post "/api/login" "{\"email\":\"$MGR_EMAIL\",\"password\":\"wrongpwd\"}")
echo "$bad_mgr_resp"
bad_mgr_code=$(echo "$bad_mgr_resp" | tail -n1)
if [ "$bad_mgr_code" -ne 401 ]; then
  echo "Expected 401 for wrong credentials (Manager) but got $bad_mgr_code"; exit 1
fi

# Project tests: add a project letting DB auto-generate id, then attempt duplicate by reusing explicit id
# Requires manager token now because endpoint is protected by @manager_of_department
PROJ_ID=$((RAND + 1000))
PROJ_PAYLOAD_AUTO="{\"token\":\"$MGR_TOKEN\",\"descrizione\":\"Test project\",\"budget\":10000.50,\"nome\":\"TestProj\",\"data_inizio\":\"2025-01-01\",\"data_fine\":\"2025-12-31\",\"id_dipartimento\":1}"
PROJ_PAYLOAD_DUP="{\"token\":\"$MGR_TOKEN\",\"id_progetto\":$PROJ_ID,\"descrizione\":\"Test project\",\"budget\":10000.50,\"nome\":\"TestProj-Explicit\",\"data_inizio\":\"2025-01-01\",\"data_fine\":\"2025-12-31\",\"id_dipartimento\":1}"

cat <<EOF

=== Project creation: auto id ===
EOF
proj_resp=$(json_post "/api/add/Project" "$PROJ_PAYLOAD_AUTO")
echo "$proj_resp"
proj_code=$(echo "$proj_resp" | tail -n1)
proj_body=$(echo "$proj_resp" | sed '$d')

cat <<EOF

=== Projects in Progress (manager should succeed) ===
EOF
proj_in_prog_resp=$(json_post "/api/projects/in-progress" "{\"token\":\"$MGR_TOKEN\"}")
echo "$proj_in_prog_resp"
proj_in_prog_code=$(echo "$proj_in_prog_resp" | tail -n1)
proj_in_prog_body=$(echo "$proj_in_prog_resp" | sed '$d')
if [ "$proj_in_prog_code" -ne 200 ]; then
  echo "Expected 200 for projects/in-progress with manager token, got $proj_in_prog_code"; echo "$proj_in_prog_body"; exit 1
fi

cat <<EOF

=== Projects in Progress (dipendente should be forbidden 403) ===
EOF
proj_in_prog_forbid=$(json_post "/api/projects/in-progress" "{\"token\":\"$DIP_TOKEN\"}")
echo "$proj_in_prog_forbid"
proj_in_prog_forbid_code=$(echo "$proj_in_prog_forbid" | tail -n1)
if [ "$proj_in_prog_forbid_code" -ne 403 ]; then
  echo "Expected 403 for projects/in-progress with dipendente token, got $proj_in_prog_forbid_code"; exit 1
fi
if [ "$proj_code" -ne 201 ]; then
  echo "Project creation failed (expected 201, got $proj_code)"; echo "$proj_body"; exit 1
fi

# Extract id_progetto from response
proj_id_returned=$(echo "$proj_body" | sed -n 's/.*"id_progetto"[[:space:]]*:[[:space:]]*\([0-9]*\).*/\1/p')
if [ -z "$proj_id_returned" ]; then
  # nuovo schema: dentro data
  proj_id_returned=$(echo "$proj_body" | sed -n 's/.*"data"[^
]*"id_progetto"[[:space:]]*:[[:space:]]*\([0-9]*\).*/\1/p')
fi
echo "Returned project id: $proj_id_returned"
if [ -z "$proj_id_returned" ]; then
  echo "No id_progetto returned from project creation"; exit 1
fi

# Use returned id for subsequent project-scoped tests
USE_PROJ_ID=${proj_id_returned:-$PROJ_ID}

cat <<EOF

=== Update Project (change nome & descrizione) ===
EOF
upd_proj_payload="{\"token\":\"$MGR_TOKEN\",\"id_progetto\":$USE_PROJ_ID,\"id_dipartimento\":1,\"nome\":\"TestProjUpdated\",\"descrizione\":\"Descrizione aggiornata\"}"
upd_proj_resp=$(json_post "/api/update/Project" "$upd_proj_payload")
echo "$upd_proj_resp"
upd_proj_code=$(echo "$upd_proj_resp" | tail -n1)
upd_proj_body=$(echo "$upd_proj_resp" | sed '$d')
if [ "$upd_proj_code" -ne 200 ]; then
  echo "Expected 200 for update/Project, got $upd_proj_code"; echo "$upd_proj_body"; exit 1
fi

cat <<EOF

=== Delete Project (negative: non-existing id) ===
EOF
del_proj_neg_resp=$(json_post "/api/delete/Project" "{\"token\":\"$MGR_TOKEN\",\"id_progetto\":999999,\"id_dipartimento\":1}")
echo "$del_proj_neg_resp"
del_proj_neg_code=$(echo "$del_proj_neg_resp" | tail -n1)
if [ "$del_proj_neg_code" -ne 404 ]; then
  echo "Expected 404 deleting non-existing project, got $del_proj_neg_code"; exit 1
fi

cat <<EOF

=== Update Task (negative before creation) ===
EOF
upd_task_neg_resp=$(json_post "/api/update/Task" "{\"token\":\"$MGR_TOKEN\",\"id\":12345,\"id_progetto\":$USE_PROJ_ID,\"id_dipartimento\":1,\"stato\":\"Closed\"}")
echo "$upd_task_neg_resp"
upd_task_neg_code=$(echo "$upd_task_neg_resp" | tail -n1)
if [ "$upd_task_neg_code" -ne 404 ]; then
  echo "Expected 404 for updating non-existing task, got $upd_task_neg_code"; exit 1
fi

cat <<EOF

=== Projects by Dipendente (happy path) ===
EOF
proj_by_dip_resp=$(json_post "/api/projects/by-dipendente" "{\"email_dipendente\":\"$DIP_EMAIL\"}")
echo "$proj_by_dip_resp"
proj_by_dip_code=$(echo "$proj_by_dip_resp" | tail -n1)
proj_by_dip_body=$(echo "$proj_by_dip_resp" | sed '$d')
if [ "$proj_by_dip_code" -ne 200 ]; then
  echo "Expected 200 for projects/by-dipendente, got $proj_by_dip_code"; echo "$proj_by_dip_body"; exit 1
fi

cat <<EOF

=== Projects by Dipendente (missing email should 400) ===
EOF
proj_by_dip_missing=$(json_post "/api/projects/by-dipendente" "{}")
echo "$proj_by_dip_missing"
proj_by_dip_missing_code=$(echo "$proj_by_dip_missing" | tail -n1)
if [ "$proj_by_dip_missing_code" -ne 400 ]; then
  echo "Expected 400 for projects/by-dipendente missing email, got $proj_by_dip_missing_code"; exit 1
fi

cat <<EOF

=== Project Budget (happy path) ===
EOF
proj_budget_resp=$(json_post "/api/projects/budget" "{\"id_progetto\":$USE_PROJ_ID}")
echo "$proj_budget_resp"
proj_budget_code=$(echo "$proj_budget_resp" | tail -n1)
proj_budget_body=$(echo "$proj_budget_resp" | sed '$d')
if [ "$proj_budget_code" -ne 200 ]; then
  echo "Expected 200 for projects/budget, got $proj_budget_code"; echo "$proj_budget_body"; exit 1
fi
# Note: budget returned as number with 2 decimals

cat <<EOF

=== Project Budget (missing id_progetto should 400) ===
EOF
proj_budget_missing=$(json_post "/api/projects/budget" "{}")
echo "$proj_budget_missing"
proj_budget_missing_code=$(echo "$proj_budget_missing" | tail -n1)
if [ "$proj_budget_missing_code" -ne 400 ]; then
  echo "Expected 400 for projects/budget missing id_progetto, got $proj_budget_missing_code"; exit 1
fi

cat <<EOF

=== Dipendenti per Progetto (manager scope) ===
EOF
dip_proj_resp=$(json_post "/api/dipendenti/by-project" "{\"token\":\"$MGR_TOKEN\",\"id_progetto\":$USE_PROJ_ID,\"id_dipartimento\":1}")
echo "$dip_proj_resp"
dip_proj_code=$(echo "$dip_proj_resp" | tail -n1)
dip_proj_body=$(echo "$dip_proj_resp" | sed '$d')
if [ "$dip_proj_code" -ne 200 ]; then
  echo "Expected 200 for dipendenti/by-project initial, got $dip_proj_code"; echo "$dip_proj_body"; exit 1
fi
dip_proj_count=$(echo "$dip_proj_body" | tr -d '\n' | sed -n 's/.*"count"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p')
if [ -z "$dip_proj_count" ]; then
  echo "Expected count present for dipendenti/by-project, got empty"; exit 1
fi

cat <<EOF

=== Dipendenti per Progetto (missing id_progetto) ===
EOF
dip_proj_missing=$(json_post "/api/dipendenti/by-project" "{\"token\":\"$MGR_TOKEN\"}")
echo "$dip_proj_missing"
dip_proj_missing_code=$(echo "$dip_proj_missing" | tail -n1)
if [ "$dip_proj_missing_code" -ne 400 ]; then
  echo "Expected 400 for dipendenti/by-project missing id_progetto, got $dip_proj_missing_code"; exit 1
fi

cat <<EOF

=== Dipendenti per Progetto (dipendente token should 403) ===
EOF
dip_proj_forbidden=$(json_post "/api/dipendenti/by-project" "{\"token\":\"$DIP_TOKEN\",\"id_progetto\":$USE_PROJ_ID}")
echo "$dip_proj_forbidden"
dip_proj_forbidden_code=$(echo "$dip_proj_forbidden" | tail -n1)
if [ "$dip_proj_forbidden_code" -ne 403 ]; then
  echo "Expected 403 for dipendenti/by-project with dipendente token, got $dip_proj_forbidden_code"; exit 1
fi

cat <<EOF

=== Managers per Progetto (manager scope) ===
EOF
mgrs_resp=$(json_post "/api/managers/by-project" "{\"token\":\"$MGR_TOKEN\",\"id_progetto\":$USE_PROJ_ID,\"id_dipartimento\":1}")
echo "$mgrs_resp"
mgrs_code=$(echo "$mgrs_resp" | tail -n1)
mgrs_body=$(echo "$mgrs_resp" | sed '$d')
if [ "$mgrs_code" -ne 200 ]; then
  echo "Expected 200 for managers/by-project (manager), got $mgrs_code"; echo "$mgrs_body"; exit 1
fi

cat <<EOF

=== Managers per Progetto (dipendente scope) ===
EOF
mgrs_dip_resp=$(json_post "/api/managers/by-project" "{\"token\":\"$DIP_TOKEN\",\"id_progetto\":$USE_PROJ_ID}")
echo "$mgrs_dip_resp"
mgrs_dip_code=$(echo "$mgrs_dip_resp" | tail -n1)
if [ "$mgrs_dip_code" -ne 200 ] && [ "$mgrs_dip_code" -ne 403 ]; then
  echo "Expected 200 or 403 for managers/by-project with dipendente token, got $mgrs_dip_code"; exit 1
fi

cat <<EOF

=== Dipendenti per Progetto (manager other dept should 403) ===
EOF
dip_proj_other=$(json_post "/api/dipendenti/by-project" "{\"token\":\"$MGR_TOKEN\",\"id_progetto\":$USE_PROJ_ID,\"id_dipartimento\":999}")
echo "$dip_proj_other"
dip_proj_other_code=$(echo "$dip_proj_other" | tail -n1)
if [ "$dip_proj_other_code" -ne 403 ]; then
  echo "Expected 403 for manager querying dipendenti by project in other dept, got $dip_proj_other_code"; exit 1
fi


cat <<EOF

=== Insert explicit id project (should succeed or conflict if id exists) ===
EOF
explicit_resp=$(json_post "/api/add/Project" "$PROJ_PAYLOAD_DUP")
echo "$explicit_resp"
explicit_code=$(echo "$explicit_resp" | tail -n1)
if [ "$explicit_code" -eq 409 ]; then
  echo "Explicit project id duplicate as expected (409)"
elif [ "$explicit_code" -eq 201 ]; then
  echo "Explicit project created (id provided)."
else
  echo "Unexpected status for explicit id project: $explicit_code"; exit 1
fi


cat <<EOF

=== Duplicate explicit project id (force same id again) ===
EOF
dup_again_resp=$(json_post "/api/add/Project" "$PROJ_PAYLOAD_DUP")
echo "$dup_again_resp"
dup_again_code=$(echo "$dup_again_resp" | tail -n1)
if [ "$dup_again_code" -ne 409 ]; then
  echo "Expected 409 inserting same explicit project id again, got $dup_again_code"; exit 1
fi
cat <<EOF

=== Cross-department project creation (mismatched department id) ===
EOF
# Use existing manager token but force a different department id (assuming manager dep is 1)
MISMATCH_PAYLOAD="{\"token\":\"$MGR_TOKEN\",\"descrizione\":\"Cross dept attempt\",\"budget\":500,\"nome\":\"CrossProj\",\"data_inizio\":\"2025-02-01\",\"data_fine\":\"2025-12-31\",\"id_dipartimento\":999}"
cross_resp=$(json_post "/api/add/Project" "$MISMATCH_PAYLOAD")
echo "$cross_resp"
cross_code=$(echo "$cross_resp" | tail -n1)
if [ "$cross_code" -ne 403 ]; then
  echo "Expected 403 for mismatched department attempt, got $cross_code"; exit 1
fi


cat <<EOF

=== Project creation with Dipendente token (should be 403) ===
EOF
bad_proj_payload="{\"token\":\"$DIP_TOKEN\",\"descrizione\":\"Bad attempt\",\"budget\":10,\"nome\":\"FailProj\",\"data_inizio\":\"2025-01-01\",\"data_fine\":\"2025-12-31\",\"id_dipartimento\":1}"
bad_proj_resp=$(json_post "/api/add/Project" "$bad_proj_payload")
echo "$bad_proj_resp"
bad_proj_code=$(echo "$bad_proj_resp" | tail -n1)
if [ "$bad_proj_code" -ne 403 ]; then
  echo "Expected 403 when Dipendente tries to create project, got $bad_proj_code"; exit 1
fi


# (Project visibility tests moved after first task creation to allow Dipendente filtering)

# Prepare a task linked to the explicit project (or fallback to returned id)
TASK_ID=$((RAND + 5000))
TASK_PAYLOAD_AUTO="{\"token\":\"$MGR_TOKEN\",\"stato\":\"Open\",\"descrizione\":\"Task di test\",\"data_inizio\":\"2025-03-01\",\"data_fine\":\"2025-03-31\",\"id_progetto\":$PROJ_ID,\"id_dipartimento\":1,\"email_dipendente\":\"$DIP_EMAIL\",\"email_manager\":\"$MGR_EMAIL\"}"

cat <<EOF

=== Task creation (auto id) ===
EOF
task_resp=$(json_post "/api/add/Task" "$TASK_PAYLOAD_AUTO")
echo "$task_resp"
task_code=$(echo "$task_resp" | tail -n1)
task_body=$(echo "$task_resp" | sed '$d')
if [ "$task_code" -ne 201 ]; then
  echo "Task creation failed (expected 201, got $task_code)"; echo "$task_body"; exit 1
fi
task_id_returned=$(extract_number "$task_body" "id_task")
echo "Returned task id: $task_id_returned"
if [ -z "$task_id_returned" ]; then
  echo "No id_task returned from task creation"; exit 1
fi

cat <<EOF

=== Update Task (change stato) ===
EOF
upd_task_payload="{\"token\":\"$MGR_TOKEN\",\"id\":$task_id_returned,\"id_progetto\":$PROJ_ID,\"id_dipartimento\":1,\"stato\":\"InProgress\"}"
upd_task_resp=$(json_post "/api/update/Task" "$upd_task_payload")
echo "$upd_task_resp"
upd_task_code=$(echo "$upd_task_resp" | tail -n1)
if [ "$upd_task_code" -ne 200 ]; then
  echo "Expected 200 for update/Task, got $upd_task_code"; exit 1
fi

# (Tasks visibility endpoint updated: now requires token and singular path /api/task/by-project; tests added later)

cat <<EOF

=== Negative: addTask with dipendente token (should 403) ===
EOF
task_bad_payload="{\"token\":\"$DIP_TOKEN\",\"stato\":\"Open\",\"descrizione\":\"Fail task\",\"data_inizio\":\"2025-03-01\",\"data_fine\":\"2025-03-31\",\"id_progetto\":$PROJ_ID,\"id_dipartimento\":1,\"email_dipendente\":\"$DIP_EMAIL\",\"email_manager\":\"$MGR_EMAIL\"}"
task_bad_resp=$(json_post "/api/add/Task" "$task_bad_payload")
echo "$task_bad_resp"
task_bad_code=$(echo "$task_bad_resp" | tail -n1)
if [ "$task_bad_code" -ne 403 ]; then
  echo "Expected 403 when Dipendente tries to create task, got $task_bad_code"; exit 1
fi

cat <<EOF

=== All extended tests completed successfully ===
EOF

# Placeholder for new unified visibility endpoint tests (to be appended):
# - /api/project/by-department
# - /api/task/by-project

cat <<EOF

=== Visibility: manager fetch projects (scope all) ===
EOF
mgr_proj_vis_resp=$(json_post "/api/project/by-department" "{\"token\":\"$MGR_TOKEN\",\"id_dipartimento\":1}")
echo "$mgr_proj_vis_resp"
mgr_proj_vis_code=$(echo "$mgr_proj_vis_resp" | tail -n1)
mgr_proj_vis_body=$(echo "$mgr_proj_vis_resp" | sed '$d')
if [ "$mgr_proj_vis_code" -ne 200 ]; then
  echo "Expected 200 for manager project visibility, got $mgr_proj_vis_code"; exit 1
fi
mgr_proj_scope=$(extract_string "$mgr_proj_vis_body" "scope")
if [ "$mgr_proj_scope" != "all" ]; then
  echo "Expected scope=all for manager, got $mgr_proj_scope"; exit 1
fi
mgr_proj_count=$(extract_number "$mgr_proj_vis_body" "count")
if [ -z "$mgr_proj_count" ]; then
  echo "Manager project visibility: count missing"; exit 1
fi

cat <<EOF

=== Visibility negative: manager missing token ===
EOF
missing_token_resp=$(json_post "/api/project/by-department" "{\"id_dipartimento\":1}")
echo "$missing_token_resp"
missing_token_code=$(echo "$missing_token_resp" | tail -n1)
if [ "$missing_token_code" -ne 400 ]; then
  echo "Expected 400 (schema validation) when token missing, got $missing_token_code"; exit 1
fi

cat <<EOF

=== Visibility negative: manager invalid token ===
EOF
invalid_token_resp=$(json_post "/api/project/by-department" "{\"token\":\"invalid_token_123\",\"id_dipartimento\":1}")
echo "$invalid_token_resp"
invalid_token_code=$(echo "$invalid_token_resp" | tail -n1)
if [ "$invalid_token_code" -ne 403 ]; then
  echo "Expected 403 for invalid token, got $invalid_token_code"; exit 1
fi

cat <<EOF

=== Visibility: dipendente fetch projects (scope own) ===
EOF
dip_proj_vis_resp=$(json_post "/api/project/by-department" "{\"token\":\"$DIP_TOKEN\",\"id_dipartimento\":1}")
echo "$dip_proj_vis_resp"
dip_proj_vis_code=$(echo "$dip_proj_vis_resp" | tail -n1)
dip_proj_vis_body=$(echo "$dip_proj_vis_resp" | sed '$d')
if [ "$dip_proj_vis_code" -ne 200 ]; then
  echo "Expected 200 for dipendente project visibility, got $dip_proj_vis_code"; exit 1
fi
dip_proj_scope=$(extract_string "$dip_proj_vis_body" "scope")
if [ "$dip_proj_scope" != "own" ]; then
  echo "Expected scope=own for dipendente, got $dip_proj_scope"; exit 1
fi
dip_proj_count=$(extract_number "$dip_proj_vis_body" "count")
if [ -z "$dip_proj_count" ]; then
  echo "Dipendente project visibility: count missing"; exit 1
fi

cat <<EOF

=== Visibility negative: project by department missing id_dipartimento ===
EOF
missing_dept_resp=$(json_post "/api/project/by-department" "{\"token\":\"$MGR_TOKEN\"}")
echo "$missing_dept_resp"
missing_dept_code=$(echo "$missing_dept_resp" | tail -n1)
if [ "$missing_dept_code" -ne 400 ]; then
  echo "Expected 400 for missing id_dipartimento, got $missing_dept_code"; exit 1
fi

cat <<EOF

=== Visibility: manager fetch tasks by project (scope all) ===
EOF
mgr_tasks_vis_resp=$(json_post "/api/task/by-project" "{\"token\":\"$MGR_TOKEN\",\"id_progetto\":$PROJ_ID,\"id_dipartimento\":1}")
echo "$mgr_tasks_vis_resp"
mgr_tasks_vis_code=$(echo "$mgr_tasks_vis_resp" | tail -n1)
mgr_tasks_vis_body=$(echo "$mgr_tasks_vis_resp" | sed '$d')
if [ "$mgr_tasks_vis_code" -ne 200 ]; then
  echo "Expected 200 for manager tasks visibility, got $mgr_tasks_vis_code"; exit 1
fi
mgr_tasks_scope=$(extract_string "$mgr_tasks_vis_body" "scope")
if [ "$mgr_tasks_scope" != "all" ]; then
  echo "Expected scope=all for manager tasks, got $mgr_tasks_scope"; exit 1
fi
mgr_tasks_count=$(extract_number "$mgr_tasks_vis_body" "count")
if [ -z "$mgr_tasks_count" ]; then
  echo "Manager tasks visibility: count missing"; exit 1
fi

cat <<EOF

=== Visibility negative: manager tasks missing id_dipartimento ===
EOF
mgr_tasks_missing_dept=$(json_post "/api/task/by-project" "{\"token\":\"$MGR_TOKEN\",\"id_progetto\":$PROJ_ID}")
echo "$mgr_tasks_missing_dept"
mgr_tasks_missing_dept_code=$(echo "$mgr_tasks_missing_dept" | tail -n1)
if [ "$mgr_tasks_missing_dept_code" -ne 400 ]; then
  echo "Expected 400 for missing id_dipartimento in tasks visibility, got $mgr_tasks_missing_dept_code"; exit 1
fi

cat <<EOF

=== Visibility: dipendente tasks by project (scope own) ===
EOF
dip_tasks_vis_resp=$(json_post "/api/task/by-project" "{\"token\":\"$DIP_TOKEN\",\"id_progetto\":$PROJ_ID}")
echo "$dip_tasks_vis_resp"
dip_tasks_vis_code=$(echo "$dip_tasks_vis_resp" | tail -n1)
dip_tasks_vis_body=$(echo "$dip_tasks_vis_resp" | sed '$d')
if [ "$dip_tasks_vis_code" -ne 200 ]; then
  echo "Expected 200 for dipendente tasks visibility, got $dip_tasks_vis_code"; exit 1
fi
dip_tasks_scope=$(extract_string "$dip_tasks_vis_body" "scope")
if [ "$dip_tasks_scope" != "own" ]; then
  echo "Expected scope=own for dipendente tasks, got $dip_tasks_scope"; exit 1
fi
dip_tasks_count=$(extract_number "$dip_tasks_vis_body" "count")
if [ -z "$dip_tasks_count" ]; then
  echo "Dip tasks visibility: count missing"; exit 1
fi

cat <<EOF

=== Register second Dipendente (for filtering) ===
EOF
DIP2_PAYLOAD="{\"email\":\"$DIP2_EMAIL\",\"password\":\"pwd\",\"nome\":\"Secondo\",\"cognome\":\"Utente\",\"data_nascita\":\"1992-02-02\",\"Dipartimento_id_dipartimento\":1}"
DIP2_ALL=$(handle_register "/api/register/dipendente" "$DIP2_PAYLOAD" "Dipendente2")
echo "$DIP2_ALL"
DIP2_TOKEN=$(echo "$DIP2_ALL" | tail -n1 | tr -d '\r\n')
if [ -z "$DIP2_TOKEN" ]; then
  echo "Second dipendente token missing"; exit 1
fi

cat <<EOF

=== Create task for second Dipendente ===
EOF
TASK2_PAYLOAD_AUTO="{\"token\":\"$MGR_TOKEN\",\"stato\":\"Open\",\"descrizione\":\"Task dip2\",\"data_inizio\":\"2025-04-01\",\"data_fine\":\"2025-04-30\",\"id_progetto\":$PROJ_ID,\"id_dipartimento\":1,\"email_dipendente\":\"$DIP2_EMAIL\",\"email_manager\":\"$MGR_EMAIL\"}"
task2_resp=$(json_post "/api/add/Task" "$TASK2_PAYLOAD_AUTO")
echo "$task2_resp"
task2_code=$(echo "$task2_resp" | tail -n1)
if [ "$task2_code" -ne 201 ]; then
  echo "Second task creation failed (expected 201, got $task2_code)"; exit 1
fi

cat <<EOF

=== Visibility: manager tasks after second task (should increase count) ===
EOF
mgr_tasks_vis_resp2=$(json_post "/api/task/by-project" "{\"token\":\"$MGR_TOKEN\",\"id_progetto\":$PROJ_ID,\"id_dipartimento\":1}")
echo "$mgr_tasks_vis_resp2"
mgr_tasks_vis_code2=$(echo "$mgr_tasks_vis_resp2" | tail -n1)
mgr_tasks_vis_body2=$(echo "$mgr_tasks_vis_resp2" | sed '$d')
if [ "$mgr_tasks_vis_code2" -ne 200 ]; then
  echo "Expected 200 after second manager tasks visibility, got $mgr_tasks_vis_code2"; exit 1
fi
mgr_tasks_count2=$(extract_number "$mgr_tasks_vis_body2" "count")
if [ -z "$mgr_tasks_count2" ]; then
  echo "Missing updated manager tasks count"; exit 1
fi
if [ "$mgr_tasks_count2" -lt 2 ]; then
  echo "Expected at least 2 tasks after adding second, got $mgr_tasks_count2"; exit 1
fi

cat <<EOF

=== Visibility: first dipendente tasks remain isolated (still own only) ===
EOF
dip_tasks_vis_resp2=$(json_post "/api/task/by-project" "{\"token\":\"$DIP_TOKEN\",\"id_progetto\":$PROJ_ID}")
echo "$dip_tasks_vis_resp2"
dip_tasks_vis_code2=$(echo "$dip_tasks_vis_resp2" | tail -n1)
dip_tasks_vis_body2=$(echo "$dip_tasks_vis_resp2" | sed '$d')
if [ "$dip_tasks_vis_code2" -ne 200 ]; then
  echo "Expected 200 for first dipendente tasks post second insertion, got $dip_tasks_vis_code2"; exit 1
fi
dip_tasks_count2=$(extract_number "$dip_tasks_vis_body2" "count")
if [ "$dip_tasks_count2" -ne "$dip_tasks_count" ]; then
  echo "First dipendente task count changed unexpectedly: before=$dip_tasks_count after=$dip_tasks_count2"; exit 1
fi

cat <<EOF

=== Visibility: second dipendente tasks (own) ===
EOF
dip2_tasks_vis_resp=$(json_post "/api/task/by-project" "{\"token\":\"$DIP2_TOKEN\",\"id_progetto\":$PROJ_ID}")
echo "$dip2_tasks_vis_resp"
dip2_tasks_vis_code=$(echo "$dip2_tasks_vis_resp" | tail -n1)
dip2_tasks_vis_body=$(echo "$dip2_tasks_vis_resp" | sed '$d')
if [ "$dip2_tasks_vis_code" -ne 200 ]; then
  echo "Expected 200 for second dipendente tasks visibility, got $dip2_tasks_vis_code"; exit 1
fi
dip2_scope=$(extract_string "$dip2_tasks_vis_body" "scope")
if [ "$dip2_scope" != "own" ]; then
  echo "Expected scope=own for second dipendente, got $dip2_scope"; exit 1
fi
dip2_tasks_count=$(extract_number "$dip2_tasks_vis_body" "count")
if [ -z "$dip2_tasks_count" ] || [ "$dip2_tasks_count" -ne 1 ]; then
  echo "Expected exactly 1 task for second dipendente, got $dip2_tasks_count"; exit 1
fi

cat <<EOF

=== Delete Task (happy path after visibility tests) ===
EOF
del_task_resp=$(json_post "/api/delete/Task" "{\"token\":\"$MGR_TOKEN\",\"id\":$task_id_returned,\"id_progetto\":$PROJ_ID,\"id_dipartimento\":1}")
echo "$del_task_resp"
del_task_code=$(echo "$del_task_resp" | tail -n1)
if [ "$del_task_code" -ne 200 ]; then
  echo "Expected 200 for delete/Task, got $del_task_code"; exit 1
fi

cat <<EOF

=== Delete Task (negative again) ===
EOF
del_task_neg_again=$(json_post "/api/delete/Task" "{\"token\":\"$MGR_TOKEN\",\"id\":$task_id_returned,\"id_progetto\":$PROJ_ID,\"id_dipartimento\":1}")
echo "$del_task_neg_again"
del_task_neg_again_code=$(echo "$del_task_neg_again" | tail -n1)
if [ "$del_task_neg_again_code" -ne 404 ]; then
  echo "Expected 404 for deleting already deleted task, got $del_task_neg_again_code"; exit 1
fi

cat <<EOF

=== Numero Dipendenti after second registration (should reflect +1) ===
EOF
num_dip_after_second=$(json_post "/api/numeroDipendenti" "{\"token\":\"$MGR_TOKEN\",\"id_dipartimento\":1}")
echo "$num_dip_after_second"
num_dip_after_second_code=$(echo "$num_dip_after_second" | tail -n1)
num_dip_after_second_body=$(echo "$num_dip_after_second" | sed '$d')
if [ "$num_dip_after_second_code" -ne 200 ]; then
  echo "Expected 200 for numeroDipendenti after second dipendente, got $num_dip_after_second_code"; exit 1
fi
num_count_after_second=$(extract_number "$num_dip_after_second_body" "n_dipendenti")
if [ -z "$num_count_after_second" ] || [ "$num_count_after_second" -lt 2 ]; then
  echo "Expected n_dipendenti >=2 after second dipendente, got $num_count_after_second"; exit 1
fi

cat <<EOF

=== Numero Dipendenti wrong department (manager) ===
EOF
num_dip_wrong_dept=$(json_post "/api/numeroDipendenti" "{\"token\":\"$MGR_TOKEN\",\"id_dipartimento\":999999}")
echo "$num_dip_wrong_dept"
num_dip_wrong_dept_code=$(echo "$num_dip_wrong_dept" | tail -n1)
if [ "$num_dip_wrong_dept_code" -ne 403 ]; then
  echo "Expected 403 for numeroDipendenti wrong department, got $num_dip_wrong_dept_code"; exit 1
fi

cat <<EOF

=== Visibility negative: tasks by project missing id_progetto ===
EOF
missing_proj_resp=$(json_post "/api/task/by-project" "{\"token\":\"$DIP_TOKEN\"}")
echo "$missing_proj_resp"
missing_proj_code=$(echo "$missing_proj_resp" | tail -n1)
if [ "$missing_proj_code" -ne 400 ]; then
  echo "Expected 400 for missing id_progetto, got $missing_proj_code"; exit 1
fi

cat <<EOF

=== Unified visibility endpoint tests completed ===
EOF

cat <<EOF

=== Visibility negative: manager wrong department (projects) ===
EOF
mgr_wrong_dept_proj=$(json_post "/api/project/by-department" "{\"token\":\"$MGR_TOKEN\",\"id_dipartimento\":999999}")
echo "$mgr_wrong_dept_proj"
mgr_wrong_dept_proj_code=$(echo "$mgr_wrong_dept_proj" | tail -n1)
if [ "$mgr_wrong_dept_proj_code" -ne 403 ]; then
  echo "Expected 403 for manager wrong department on projects, got $mgr_wrong_dept_proj_code"; exit 1
fi

cat <<EOF

=== Visibility negative: manager wrong department (tasks) ===
EOF
mgr_wrong_dept_tasks=$(json_post "/api/task/by-project" "{\"token\":\"$MGR_TOKEN\",\"id_progetto\":$PROJ_ID,\"id_dipartimento\":999999}")
echo "$mgr_wrong_dept_tasks"
mgr_wrong_dept_tasks_code=$(echo "$mgr_wrong_dept_tasks" | tail -n1)
if [ "$mgr_wrong_dept_tasks_code" -ne 403 ]; then
  echo "Expected 403 for manager wrong department on tasks, got $mgr_wrong_dept_tasks_code"; exit 1
fi

cat <<EOF

=== Visibility empty result: dipendente other department ===
EOF
dip_other_dept=$(json_post "/api/project/by-department" "{\"token\":\"$DIP_TOKEN\",\"id_dipartimento\":999999}")
echo "$dip_other_dept"
dip_other_dept_code=$(echo "$dip_other_dept" | tail -n1)
dip_other_dept_body=$(echo "$dip_other_dept" | sed '$d')
if [ "$dip_other_dept_code" -ne 200 ]; then
  echo "Expected 200 for dipendente other department (empty allowed), got $dip_other_dept_code"; exit 1
fi
dip_other_dept_count=$(extract_number "$dip_other_dept_body" "count")
if [ -z "$dip_other_dept_count" ] || [ "$dip_other_dept_count" -ne 0 ]; then
  echo "Expected count 0 for dipendente other dept, got $dip_other_dept_count"; exit 1
fi

cat <<EOF

=== Visibility empty result: dipendente tasks non-existing project ===
EOF
NON_EXIST_PROJ=$((PROJ_ID + 987654))
dip_no_tasks=$(json_post "/api/task/by-project" "{\"token\":\"$DIP_TOKEN\",\"id_progetto\":$NON_EXIST_PROJ}")
echo "$dip_no_tasks"
dip_no_tasks_code=$(echo "$dip_no_tasks" | tail -n1)
dip_no_tasks_body=$(echo "$dip_no_tasks" | sed '$d')
if [ "$dip_no_tasks_code" -ne 200 ]; then
  echo "Expected 200 for dipendente tasks non-existing project (empty), got $dip_no_tasks_code"; exit 1
fi
dip_no_tasks_count=$(extract_number "$dip_no_tasks_body" "count")
if [ -z "$dip_no_tasks_count" ] || [ "$dip_no_tasks_count" -ne 0 ]; then
  echo "Expected 0 tasks for non-existing project, got $dip_no_tasks_count"; exit 1
fi

cat <<EOF

=== Additional negative & empty visibility tests completed ===
EOF


cat <<EOF

=== Validation: missing required field in project creation (expect 400 with fields) ===
EOF
missing_field_proj=$(json_post "/api/add/Project" "{\"token\":\"$MGR_TOKEN\",\"descrizione\":\"No name\",\"budget\":100,\"data_inizio\":\"2025-01-01\",\"data_fine\":\"2025-12-31\",\"id_dipartimento\":1}")
echo "$missing_field_proj"
missing_field_proj_code=$(echo "$missing_field_proj" | tail -n1)
if [ "$missing_field_proj_code" -ne 400 ]; then
  echo "Expected 400 for missing 'nome' field, got $missing_field_proj_code"; exit 1
fi

cat <<EOF

=== Validation: missing token for task creation (expect 401) ===
EOF
missing_token_task=$(json_post "/api/add/Task" "{\"stato\":\"Open\",\"descrizione\":\"No token\",\"data_inizio\":\"2025-05-01\",\"data_fine\":\"2025-05-31\",\"id_progetto\":$PROJ_ID,\"id_dipartimento\":1,\"email_dipendente\":\"$DIP_EMAIL\",\"email_manager\":\"$MGR_EMAIL\"}")
echo "$missing_token_task"
missing_token_task_code=$(echo "$missing_token_task" | tail -n1)
if [ "$missing_token_task_code" -ne 401 ]; then
  echo "Expected 401 for missing token in task creation, got $missing_token_task_code"; exit 1
fi

cat <<EOF

=== Validation: login with neither token nor credentials (expect 400) ===
EOF
login_empty=$(json_post "/api/login" "{}")
echo "$login_empty"
login_empty_code=$(echo "$login_empty" | tail -n1)
if [ "$login_empty_code" -ne 400 ]; then
  echo "Expected 400 for empty login payload, got $login_empty_code"; exit 1
fi

cat <<EOF

=== Validation: login with only email (expect 400) ===
EOF
login_only_email=$(json_post "/api/login" "{\"email\":\"$DIP_EMAIL\"}")
echo "$login_only_email"
login_only_email_code=$(echo "$login_only_email" | tail -n1)
if [ "$login_only_email_code" -ne 400 ]; then
  echo "Expected 400 for login with only email, got $login_only_email_code"; exit 1
fi

cat <<EOF

=== Validation: login with only password (expect 400) ===
EOF
login_only_pwd=$(json_post "/api/login" "{\"password\":\"pwd\"}")
echo "$login_only_pwd"
login_only_pwd_code=$(echo "$login_only_pwd" | tail -n1)
if [ "$login_only_pwd_code" -ne 400 ]; then
  echo "Expected 400 for login with only password, got $login_only_pwd_code"; exit 1
fi

cat <<EOF

=== Validation/Integrity: task creation with non-existent project (expect 400 or 400/409 mapped) ===
EOF
NON_EXIST_PROJ_FOR_TASK=$((PROJ_ID + 424242))
task_fk_fail=$(json_post "/api/add/Task" "{\"token\":\"$MGR_TOKEN\",\"stato\":\"Open\",\"descrizione\":\"Bad FK\",\"data_inizio\":\"2025-06-01\",\"data_fine\":\"2025-06-30\",\"id_progetto\":$NON_EXIST_PROJ_FOR_TASK,\"id_dipartimento\":1,\"email_dipendente\":\"$DIP_EMAIL\",\"email_manager\":\"$MGR_EMAIL\"}")
echo "$task_fk_fail"
task_fk_fail_code=$(echo "$task_fk_fail" | tail -n1)
if [ "$task_fk_fail_code" -ne 400 ] && [ "$task_fk_fail_code" -ne 409 ]; then
  echo "Expected 400 or 409 for task with non-existent project FK, got $task_fk_fail_code"; exit 1
fi

cat <<EOF

=== Extended Marshmallow + integrity negative tests completed ===
EOF



