#!/usr/bin/env bash
# validate_framework.sh
#
# Verifies that the contextualized framework files in _context/ are ready
# for Claude agent handoff. Run from the repository root.
#
#   bash _framework/validate_framework.sh
#
# Exits 0 if ready, non-zero with details if not.

set -euo pipefail


script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
cd "$repo_root"

context_dir="_context"

if [[ ! -d "$context_dir" ]]; then
  printf 'Missing required directory: %s\n' "$context_dir" >&2
  printf 'Run /start-working to bootstrap _context/ from _framework/.\n' >&2
  exit 1
fi

required_files=(
  "01_product_vision_and_scope.md"
  "02_domain_model_and_business_rules.md"
  "03_user_experience_and_use_cases.md"
  "04_solution_architecture.md"
  "05_engineering_quality_security_and_compliance.md"
  "06_integrations_data_flow_and_boundaries.md"
  "07_delivery_plan_and_milestones.md"
  "08_active_task_packet.md"
  "09_decision_log.md"
)

optional_files=(
  "10_non_functional_requirements.md"
  "11_visual_language_and_design_system.md"
  "12_environments_and_devops.md"
  "13_threat_model_and_data_classification.md"
  "14_agent_roles_and_handoffs.md"
  "15_glossary.md"
)

fail=0

for file in "${required_files[@]}"; do
  if [[ ! -f "$context_dir/$file" ]]; then
    printf 'Missing required file: %s/%s\n' "$context_dir" "$file" >&2
    fail=1
  fi
done

if [[ ! -f "CLAUDE.md" ]]; then
  printf 'Missing CLAUDE.md at repository root\n' >&2
  fail=1
fi

if (( fail )); then
  exit 1
fi

# Build the list of files to scan: required + any optional ones that exist.
scan_files=()
for file in "${required_files[@]}"; do
  scan_files+=("$context_dir/$file")
done
for file in "${optional_files[@]}"; do
  if [[ -f "$context_dir/$file" ]]; then
    scan_files+=("$context_dir/$file")
  fi
done

placeholder_hits="$(grep -En '\{\{(REQUIRED|OPTIONAL|EXAMPLE):' "${scan_files[@]}" || true)"
if [[ -n "$placeholder_hits" ]]; then
  printf 'Unresolved placeholders detected in _context/ files:\n' >&2
  printf '%s\n' "$placeholder_hits" >&2
  fail=1
fi

if ! grep -n '08_active_task_packet\.md' CLAUDE.md >/dev/null 2>&1; then
  printf 'CLAUDE.md must reference 08_active_task_packet.md as the active task contract\n' >&2
  fail=1
fi

if ! grep -n '01_product_vision_and_scope\.md' CLAUDE.md >/dev/null 2>&1 \
   || ! grep -n '09_decision_log\.md' CLAUDE.md >/dev/null 2>&1; then
  printf 'CLAUDE.md must route Claude through 01 through 09\n' >&2
  fail=1
fi

if ! grep -n 'Approved for apply' CLAUDE.md >/dev/null 2>&1; then
  printf 'CLAUDE.md must document the "Approved for apply" gate\n' >&2
  fail=1
fi

if ! grep -Fn '| Task status |' "$context_dir/08_active_task_packet.md" >/dev/null 2>&1; then
  printf 'Missing authoritative "Task status" row in %s/08_active_task_packet.md\n' "$context_dir" >&2
  fail=1
fi

active_task_count="$(grep -Fc '| Task status |' "$context_dir/08_active_task_packet.md" || true)"
if [[ "$active_task_count" != "1" ]]; then
  printf '%s/08_active_task_packet.md must contain exactly one active task (found %s)\n' \
    "$context_dir" "$active_task_count" >&2
  fail=1
fi

if (( fail )); then
  exit 1
fi

printf 'Framework ready for Claude handoff.\n'
