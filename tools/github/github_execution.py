#!/usr/bin/env python3
"""BurnMate GitHub execution visibility tooling."""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import config

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
SLICES_DIR = REPO_ROOT / "docs" / "slices"
INDEX_FILE = SLICES_DIR / "index.md"
ROADMAP_FILE = SLICES_DIR / "ROADMAP.md"


class ToolError(RuntimeError):
    """Base error with an attached process exit code."""

    exit_code = 2


class ValidationError(ToolError):
    """Raised when local data does not satisfy the requested operation."""

    exit_code = 1


class CommandError(ToolError):
    """Raised when a subprocess command fails."""

    def __init__(self, message: str, *, exit_code: int = 2) -> None:
        super().__init__(message)
        self.exit_code = exit_code


@dataclass
class RepoInfo:
    owner: str
    name: str
    default_branch: str
    url: str

    @property
    def full_name(self) -> str:
        return f"{self.owner}/{self.name}"


@dataclass
class PullRequestRecord:
    id: str
    number: int
    title: str
    body: str
    url: str
    head_ref_name: str
    state: str
    merged_at: str | None


@dataclass
class IssueRecord:
    id: str
    number: int
    title: str
    body: str
    url: str
    state: str
    labels: list[str]


@dataclass
class ProjectField:
    id: str
    name: str
    data_type: str
    options: dict[str, str] = field(default_factory=dict)


@dataclass
class ProjectItem:
    id: str
    content_id: str
    url: str
    field_values: dict[str, str | None] = field(default_factory=dict)


@dataclass
class ProjectInfo:
    id: str
    number: int
    title: str
    url: str
    fields: dict[str, ProjectField] = field(default_factory=dict)
    items_by_content_id: dict[str, ProjectItem] = field(default_factory=dict)
    items_by_url: dict[str, ProjectItem] = field(default_factory=dict)


@dataclass
class SliceMetadata:
    slice_id: str
    title: str
    current_state: str
    owner_role: str
    owner_role_project: str
    last_updated: str
    links: str
    branch_name: str | None
    blocking_issues: str
    notes: str
    folder: Path
    folder_relative: str
    docs: dict[str, Path]
    doc_relpaths: dict[str, str]
    roadmap_position: int | None
    roadmap_total: int | None
    roadmap_title: str | None
    prd_summary: str | None
    acceptance_criteria: list[str]
    acceptance_criteria_total: int
    inferred_labels: list[str]
    placeholder_fields: list[str]
    issue_title: str
    github_execution_state: str
    issue_should_be_closed: bool
    project_field_values: dict[str, str | None]
    pull_request: PullRequestRecord | None = None


class ShellRunner:
    """Thin subprocess wrapper to make command mocking easier in tests."""

    def run(
        self,
        args: list[str],
        *,
        input_text: str | None = None,
        check: bool = True,
        cwd: Path = REPO_ROOT,
    ) -> subprocess.CompletedProcess[str]:
        completed = subprocess.run(
            args,
            cwd=cwd,
            input=input_text,
            text=True,
            capture_output=True,
        )
        if check and completed.returncode != 0:
            stderr = completed.stderr.strip()
            stdout = completed.stdout.strip()
            detail = stderr or stdout or "command failed without output"
            raise CommandError(
                f"Command failed ({completed.returncode}): {' '.join(args)}\n{detail}",
                exit_code=2,
            )
        return completed


def print_info(message: str) -> None:
    print(message, file=sys.stderr)


def normalize_for_lookup(value: str) -> str:
    return re.sub(r"[^A-Z0-9]+", "", value.upper())


def is_placeholder(value: str | None) -> bool:
    if value is None:
        return True
    stripped = value.strip()
    if not stripped:
        return True
    if "<!--" in stripped:
        return True
    return stripped in {"—", "-"}


def clean_table_value(value: str | None) -> str:
    if value is None:
        return ""
    return value.strip().strip("`").strip()


def collapse_whitespace(value: str) -> str:
    return re.sub(r"\s+", " ", value.strip())


def markdown_section(text: str, heading: str) -> str | None:
    pattern = re.compile(
        rf"^## {re.escape(heading)}\s*\n(.*?)(?=^## |\Z)",
        re.MULTILINE | re.DOTALL,
    )
    match = pattern.search(text)
    return match.group(1).strip() if match else None


def repo_file_url(repo: RepoInfo, relpath: str) -> str:
    return f"{repo.url}/blob/{repo.default_branch}/{relpath}"


def parse_remote_url(remote_url: str) -> tuple[str, str] | None:
    patterns = [
        r"git@github\.com:(?P<owner>[^/]+)/(?P<repo>[^/]+?)(?:\.git)?$",
        r"https://github\.com/(?P<owner>[^/]+)/(?P<repo>[^/]+?)(?:\.git)?$",
        r"ssh://git@github\.com/(?P<owner>[^/]+)/(?P<repo>[^/]+?)(?:\.git)?$",
    ]
    for pattern in patterns:
        match = re.match(pattern, remote_url.strip())
        if match:
            return match.group("owner"), match.group("repo")
    return None


def ensure_dependencies() -> None:
    missing = [binary for binary in ("gh", "jq", "python3") if shutil.which(binary) is None]
    if missing:
        raise CommandError(
            f"Missing required dependencies: {', '.join(missing)}",
            exit_code=2,
        )


def detect_repo_info(runner: ShellRunner) -> RepoInfo:
    remote_owner: str | None = None
    remote_name: str | None = None

    remote = runner.run(["git", "remote", "get-url", "origin"], check=False)
    if remote.returncode == 0:
        parsed = parse_remote_url(remote.stdout.strip())
        if parsed:
            remote_owner, remote_name = parsed

    repo_view = runner.run(
        ["gh", "repo", "view", "--json", "nameWithOwner,defaultBranchRef,url"]
    )
    repo_json = json.loads(repo_view.stdout)
    name_with_owner = repo_json["nameWithOwner"]
    owner, name = name_with_owner.split("/", 1)
    if remote_owner and remote_name and (remote_owner != owner or remote_name != name):
        raise ValidationError(
            "git remote origin does not match gh repo view; refusing to guess repository identity."
        )
    return RepoInfo(
        owner=owner,
        name=name,
        default_branch=repo_json["defaultBranchRef"]["name"],
        url=repo_json["url"],
    )


def get_gh_auth_scopes(runner: ShellRunner) -> set[str]:
    status = runner.run(["gh", "auth", "status"], check=False)
    output = "\n".join(part for part in (status.stdout, status.stderr) if part)
    if status.returncode != 0:
        raise CommandError(
            "gh auth status failed. Authenticate with GitHub CLI before using BurnMate GitHub tooling.",
            exit_code=2,
        )

    match = re.search(r"Token scopes:\s*(.+)", output)
    if not match:
        return set()
    scopes_raw = match.group(1)
    return set(re.findall(r"[A-Za-z0-9:_-]+", scopes_raw))


def has_project_read_scope(scopes: set[str]) -> bool:
    return "project" in scopes or "read:project" in scopes


def require_project_scopes(runner: ShellRunner) -> None:
    scopes = get_gh_auth_scopes(runner)
    if "project" not in scopes:
        raise CommandError(
            "GitHub CLI token is missing required scope (project).\n"
            "Run: gh auth refresh -s project",
            exit_code=2,
        )


def run_preflight_validators(runner: ShellRunner) -> None:
    for relative_path in config.VALIDATOR_PATHS:
        validator = REPO_ROOT / relative_path
        runner.run(["python3", str(validator)])


def gh_graphql(runner: ShellRunner, query: str) -> dict[str, Any]:
    completed = runner.run(["gh", "api", "graphql", "-f", f"query={query}"])
    payload = json.loads(completed.stdout)
    if "errors" in payload:
        messages = "; ".join(error.get("message", "unknown GraphQL error") for error in payload["errors"])
        raise CommandError(f"GitHub GraphQL query failed: {messages}", exit_code=2)
    return payload["data"]


def query_owner_node(runner: ShellRunner, owner: str) -> tuple[str, str]:
    owner_lookup = runner.run(["gh", "api", f"users/{owner}"])
    owner_payload = json.loads(owner_lookup.stdout)
    owner_type = owner_payload.get("type")
    if owner_type == "User":
        root_name = "user"
    elif owner_type == "Organization":
        root_name = "organization"
    else:
        raise CommandError(f"Unable to determine whether {owner} is a user or organization.", exit_code=2)

    query = f"""
    query {{
      ownerRoot: {root_name}(login: {json.dumps(owner)}) {{
        id
        login
      }}
    }}
    """
    data = gh_graphql(runner, query)
    owner_node = data.get("ownerRoot")
    if owner_node:
        return root_name, owner_node["id"]
    raise CommandError(f"Unable to resolve GitHub owner node for {owner}.", exit_code=2)


def parse_project_field_node(node: dict[str, Any]) -> ProjectField | None:
    typename = node.get("__typename")
    if typename not in {"ProjectV2Field", "ProjectV2SingleSelectField", "ProjectV2IterationField"}:
        return None
    options: dict[str, str] = {}
    for option in node.get("options", []) or []:
        options[option["name"]] = option["id"]
    return ProjectField(
        id=node["id"],
        name=node["name"],
        data_type=node["dataType"],
        options=options,
    )


def parse_project_item_field_value(node: dict[str, Any]) -> tuple[str, str | None] | None:
    field = node.get("field")
    if not field:
        return None
    field_name = field.get("name")
    if not field_name:
        return None
    typename = node.get("__typename")
    if typename == "ProjectV2ItemFieldSingleSelectValue":
        return field_name, node.get("name")
    if typename == "ProjectV2ItemFieldTextValue":
        return field_name, node.get("text")
    return None


def parse_project_snapshot(project_node: dict[str, Any]) -> ProjectInfo:
    fields: dict[str, ProjectField] = {}
    for field_node in project_node.get("fields", {}).get("nodes", []):
        parsed = parse_project_field_node(field_node)
        if parsed:
            fields[parsed.name] = parsed

    items_by_content_id: dict[str, ProjectItem] = {}
    items_by_url: dict[str, ProjectItem] = {}
    for item_node in project_node.get("items", {}).get("nodes", []):
        content = item_node.get("content")
        if not content or content.get("__typename") != "Issue":
            continue
        field_values: dict[str, str | None] = {}
        for field_value_node in item_node.get("fieldValues", {}).get("nodes", []):
            parsed_value = parse_project_item_field_value(field_value_node)
            if parsed_value:
                field_values[parsed_value[0]] = parsed_value[1]
        item = ProjectItem(
            id=item_node["id"],
            content_id=content["id"],
            url=content["url"],
            field_values=field_values,
        )
        items_by_content_id[item.content_id] = item
        items_by_url[item.url] = item

    return ProjectInfo(
        id=project_node["id"],
        number=project_node["number"],
        title=project_node["title"],
        url=project_node["url"],
        fields=fields,
        items_by_content_id=items_by_content_id,
        items_by_url=items_by_url,
    )


def get_project_by_title(
    runner: ShellRunner,
    owner: str,
    title: str,
) -> ProjectInfo | None:
    owner_type, _ = query_owner_node(runner, owner)
    query = f"""
    query {{
      ownerRoot: {owner_type}(login: {json.dumps(owner)}) {{
        projectsV2(first: 20, query: {json.dumps(title)}) {{
          nodes {{
            id
            number
            title
            url
            closed
            fields(first: 100) {{
              nodes {{
                __typename
                ... on ProjectV2Field {{
                  id
                  name
                  dataType
                }}
                ... on ProjectV2SingleSelectField {{
                  id
                  name
                  dataType
                  options {{
                    id
                    name
                  }}
                }}
                ... on ProjectV2IterationField {{
                  id
                  name
                  dataType
                }}
              }}
            }}
            items(first: 100) {{
              nodes {{
                id
                content {{
                  __typename
                  ... on Issue {{
                    id
                    number
                    url
                    title
                  }}
                }}
                fieldValues(first: 20) {{
                  nodes {{
                    __typename
                    ... on ProjectV2ItemFieldSingleSelectValue {{
                      name
                      optionId
                      field {{
                        __typename
                        ... on ProjectV2Field {{
                          id
                          name
                        }}
                        ... on ProjectV2SingleSelectField {{
                          id
                          name
                        }}
                        ... on ProjectV2IterationField {{
                          id
                          name
                        }}
                      }}
                    }}
                    ... on ProjectV2ItemFieldTextValue {{
                      text
                      field {{
                        __typename
                        ... on ProjectV2Field {{
                          id
                          name
                        }}
                        ... on ProjectV2SingleSelectField {{
                          id
                          name
                        }}
                        ... on ProjectV2IterationField {{
                          id
                          name
                        }}
                      }}
                    }}
                  }}
                }}
              }}
            }}
          }}
        }}
      }}
    }}
    """
    data = gh_graphql(runner, query)
    owner_node = data.get("ownerRoot")
    if not owner_node:
        return None

    candidates = [
        node
        for node in owner_node["projectsV2"]["nodes"]
        if node["title"] == title and not node.get("closed", False)
    ]
    if not candidates:
        return None
    candidates.sort(key=lambda node: node["number"])
    return parse_project_snapshot(candidates[0])


def create_project(runner: ShellRunner, owner: str, title: str) -> ProjectInfo:
    _, owner_id = query_owner_node(runner, owner)
    mutation = f"""
    mutation {{
      createProjectV2(input: {{
        ownerId: {json.dumps(owner_id)}
        title: {json.dumps(title)}
      }}) {{
        projectV2 {{
          id
          number
          title
          url
        }}
      }}
    }}
    """
    data = gh_graphql(runner, mutation)
    project_node = data["createProjectV2"]["projectV2"]
    return ProjectInfo(
        id=project_node["id"],
        number=project_node["number"],
        title=project_node["title"],
        url=project_node["url"],
    )


def delete_project_field(runner: ShellRunner, field_id: str) -> None:
    runner.run(["gh", "project", "field-delete", "--id", field_id])


def ensure_project_link(runner: ShellRunner, repo: RepoInfo, project: ProjectInfo) -> None:
    completed = runner.run(
        [
            "gh",
            "project",
            "link",
            str(project.number),
            "--owner",
            repo.owner,
            "--repo",
            repo.full_name,
        ],
        check=False,
    )
    output = "\n".join(part for part in (completed.stdout, completed.stderr) if part)
    if completed.returncode != 0 and "already linked" not in output.lower():
        raise CommandError(
            f"Unable to link project {project.number} to {repo.full_name}.\n{output.strip()}",
            exit_code=2,
        )


def ensure_labels(runner: ShellRunner, repo: RepoInfo) -> list[str]:
    created_or_updated: list[str] = []
    for label_name, metadata in config.MANAGED_LABELS.items():
        runner.run(
            [
                "gh",
                "label",
                "create",
                label_name,
                "--repo",
                repo.full_name,
                "--color",
                metadata["color"],
                "--description",
                metadata["description"],
                "--force",
            ]
        )
        created_or_updated.append(label_name)
    return created_or_updated


def ensure_project_fields(
    runner: ShellRunner,
    owner: str,
    project: ProjectInfo,
) -> tuple[ProjectInfo, list[str]]:
    created_fields: list[str] = []
    for field_spec in config.PROJECT_FIELDS:
        existing_field = project.fields.get(field_spec["name"])
        if existing_field is not None:
            if (
                field_spec["data_type"] == "SINGLE_SELECT"
                and sorted(existing_field.options) != sorted(field_spec["options"])
            ):
                delete_project_field(runner, existing_field.id)
            else:
                continue
        args = [
            "gh",
            "project",
            "field-create",
            str(project.number),
            "--owner",
            owner,
            "--name",
            field_spec["name"],
            "--data-type",
            field_spec["data_type"],
        ]
        if field_spec["data_type"] == "SINGLE_SELECT":
            args.extend(["--single-select-options", ",".join(field_spec["options"])])
        runner.run(args)
        created_fields.append(
            f"{field_spec['name']} (recreated)" if existing_field is not None else field_spec["name"]
        )
    refreshed = get_project_by_title(runner, owner, config.PROJECT_TITLE)
    if refreshed is None:
        raise CommandError("Project disappeared after field creation.", exit_code=2)
    return refreshed, created_fields


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def parse_index_entries() -> dict[str, dict[str, str]]:
    if not INDEX_FILE.exists():
        raise ValidationError(f"Missing slice index: {INDEX_FILE}")
    content = read_text(INDEX_FILE)
    entries: dict[str, dict[str, str]] = {}
    pattern = re.compile(
        r"^\|\s*(SLICE-\d{4})\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|$",
        re.MULTILINE,
    )
    for match in pattern.finditer(content):
        entries[match.group(1)] = {
            "name": clean_table_value(match.group(2)),
            "state": clean_table_value(match.group(3)),
            "owner_role": clean_table_value(match.group(4)),
            "folder": clean_table_value(match.group(5)),
            "last_updated": clean_table_value(match.group(6)),
        }
    return entries


def parse_roadmap_entries() -> dict[str, dict[str, Any]]:
    if not ROADMAP_FILE.exists():
        raise ValidationError(f"Missing roadmap file: {ROADMAP_FILE}")
    entries: dict[str, dict[str, Any]] = {}
    ordered_ids: list[str] = []
    for line in read_text(ROADMAP_FILE).splitlines():
        match = re.match(r"^(SLICE-\d{4})\s+[—-]\s+(.+?)(?:\s+✅)?$", line.strip())
        if not match:
            continue
        ordered_ids.append(match.group(1))
        entries[match.group(1)] = {
            "title": match.group(2).strip(),
        }
    total = len(ordered_ids)
    for position, slice_id in enumerate(ordered_ids, start=1):
        entries[slice_id]["position"] = position
        entries[slice_id]["total"] = total
    return entries


def parse_state_file(path: Path) -> dict[str, str]:
    fields: dict[str, str] = {}
    pattern = re.compile(r"^\|\s*\*\*(.+?)\*\*\s*\|\s*(.*?)\s*\|$", re.MULTILINE)
    for match in pattern.finditer(read_text(path)):
        fields[match.group(1)] = clean_table_value(match.group(2))
    return fields


def extract_prd_summary(path: Path) -> str | None:
    if not path.exists():
        return None
    section = markdown_section(read_text(path), "Problem Statement")
    if not section:
        return None
    for paragraph in section.split("\n\n"):
        paragraph = collapse_whitespace(paragraph)
        if paragraph and not paragraph.startswith("|"):
            return paragraph
    return None


def extract_acceptance_criteria(path: Path, *, limit: int | None = None) -> list[str]:
    if not path.exists():
        return []
    section = markdown_section(read_text(path), "Acceptance Criteria")
    if not section:
        return []
    criteria: list[str] = []
    for line in section.splitlines():
        if not line.startswith("| AC-"):
            continue
        columns = [column.strip() for column in line.strip("|").split("|")]
        if len(columns) >= 2:
            criteria.append(columns[1])
        if limit is not None and len(criteria) >= limit:
            break
    return criteria


def parse_branch_name(links_value: str) -> str | None:
    if is_placeholder(links_value):
        return None
    cleaned = links_value.strip().strip("`")
    if cleaned.lower() in {"n/a", "none"}:
        return None
    if cleaned.startswith("http://") or cleaned.startswith("https://"):
        return None
    return cleaned


def normalized_optional_text(value: str) -> str | None:
    if is_placeholder(value):
        return None
    cleaned = collapse_whitespace(value)
    if cleaned.lower().rstrip(".") in {"none", "n/a"}:
        return None
    return cleaned or None


def map_local_state_to_github_state(
    *,
    local_state: str,
    owner_role: str,
    pull_request: PullRequestRecord | None,
) -> str:
    if pull_request and pull_request.state == "MERGED":
        return "Live"
    if local_state == "MERGED":
        return "Live"
    if local_state == "AUDIT_APPROVED":
        return "Done"
    if owner_role in {"Planner", "Architect"}:
        return "Planning"
    if owner_role == "Engineer":
        return "Dev"
    if owner_role == "Reviewer":
        return "Review"
    if owner_role == "QA":
        return "QA"
    if owner_role == "Auditor":
        return "Audit"
    if local_state == "NOT_STARTED":
        return "Todo"
    if local_state in {"PRD_DEFINED", "HLD_DEFINED", "LLD_DEFINED"}:
        return "Planning"
    if local_state in {"CODE_IN_PROGRESS", "CODE_COMPLETE"}:
        return "Dev"
    if local_state in {"REVIEW_REQUIRED", "REVIEW_CHANGES", "REVIEW_APPROVED"}:
        return "Review"
    if local_state in {"QA_REQUIRED", "QA_CHANGES", "QA_APPROVED"}:
        return "QA"
    if local_state == "AUDIT_REQUIRED":
        return "Audit"
    return "Todo"


def should_close_issue_for_state(github_state: str) -> bool:
    return github_state in {"Todo", "Done", "Live"}


def normalize_state_input(value: str) -> str:
    canonical = config.STATE_ALIASES.get(normalize_for_lookup(value))
    if not canonical:
        raise ValidationError(f"Unsupported execution state: {value}")
    return canonical


def normalize_role_input(value: str) -> str:
    canonical = config.ROLE_ALIASES.get(normalize_for_lookup(value))
    if not canonical:
        raise ValidationError(f"Unsupported role owner: {value}")
    return canonical


def discover_slice_dirs() -> list[Path]:
    if not SLICES_DIR.exists():
        return []
    return sorted(path for path in SLICES_DIR.iterdir() if path.is_dir() and re.match(r"SLICE-\d{4}$", path.name))


def infer_labels(
    *,
    title: str,
    current_state: str,
    prd_summary: str | None,
    notes: str,
    blocking_issues: str,
    placeholder_fields: list[str],
    in_roadmap: bool,
) -> list[str]:
    labels = {"type:slice"}
    if in_roadmap:
        labels.add("roadmap")
    if current_state == "NOT_STARTED" or placeholder_fields:
        labels.add("needs-triage")
    if normalized_optional_text(blocking_issues):
        labels.add("blocked")

    haystack = " ".join([title, prd_summary or "", notes]).lower()
    if any(keyword in haystack for keyword in ("google", "auth", "login", "sign-in", "permissions", "fit")):
        labels.add("area:auth")
    if any(keyword in haystack for keyword in ("dashboard", "chart", "visual progress")):
        labels.add("area:dashboard")
    if any(keyword in haystack for keyword in ("ui", "compose", "screen", "navigation", "onboarding")):
        labels.add("area:ui")
    if any(keyword in haystack for keyword in ("engine", "domain", "persistence", "repository", "read model", "sync", "history")):
        labels.add("area:data")

    return sorted(label for label in labels if label in config.MANAGED_LABEL_NAMES)


def build_slice_metadata(
    repo: RepoInfo,
    index_entries: dict[str, dict[str, str]],
    roadmap_entries: dict[str, dict[str, Any]],
    pull_requests: list[PullRequestRecord],
) -> list[SliceMetadata]:
    slices: list[SliceMetadata] = []

    pr_by_branch = {pr.head_ref_name: pr for pr in pull_requests if pr.head_ref_name}
    for slice_dir in discover_slice_dirs():
        slice_id = slice_dir.name
        state_path = slice_dir / "state.md"
        if not state_path.exists():
            raise ValidationError(f"Slice {slice_id} is missing state.md")
        state_fields = parse_state_file(state_path)
        index_entry = index_entries.get(slice_id, {})
        roadmap_entry = roadmap_entries.get(slice_id, {})

        title_candidates = [
            clean_table_value(state_fields.get("Name")),
            clean_table_value(index_entry.get("name")),
            clean_table_value(roadmap_entry.get("title")),
            slice_id,
        ]
        title = next((candidate for candidate in title_candidates if not is_placeholder(candidate)), slice_id)

        current_state = clean_table_value(state_fields.get("Current State")) or clean_table_value(index_entry.get("state"))
        if current_state not in config.LOCAL_EXECUTION_STATES:
            raise ValidationError(f"Slice {slice_id} has unsupported state {current_state!r}")

        owner_role = clean_table_value(state_fields.get("Owner Role")) or clean_table_value(index_entry.get("owner_role"))
        owner_role_project = owner_role if owner_role in config.ROLE_OWNER_VALUES else "Unassigned"
        last_updated = clean_table_value(state_fields.get("Last Updated")) or clean_table_value(index_entry.get("last_updated"))
        links_value = clean_table_value(state_fields.get("Links"))
        blocking_issues = clean_table_value(state_fields.get("Blocking Issues"))
        notes = clean_table_value(state_fields.get("Notes"))
        branch_name = parse_branch_name(links_value)

        docs: dict[str, Path] = {}
        doc_relpaths: dict[str, str] = {}
        for filename in config.DOC_FILE_ORDER:
            path = slice_dir / filename
            if path.exists():
                docs[filename] = path
                doc_relpaths[filename] = path.relative_to(REPO_ROOT).as_posix()

        prd_summary = extract_prd_summary(slice_dir / "prd.md")
        all_acceptance_criteria = extract_acceptance_criteria(slice_dir / "prd.md")
        acceptance_criteria = all_acceptance_criteria[:5]

        placeholder_fields = [
            field_name
            for field_name, field_value in (
                ("Name", state_fields.get("Name")),
                ("Owner Role", state_fields.get("Owner Role")),
                ("Last Updated", state_fields.get("Last Updated")),
            )
            if is_placeholder(field_value)
        ]

        inferred_labels = infer_labels(
            title=title,
            current_state=current_state,
            prd_summary=prd_summary,
            notes=notes,
            blocking_issues=blocking_issues,
            placeholder_fields=placeholder_fields,
            in_roadmap=slice_id in roadmap_entries,
        )

        pull_request = None
        if branch_name and branch_name in pr_by_branch:
            pull_request = pr_by_branch[branch_name]
        else:
            for pr in pull_requests:
                haystack = f"{pr.title} {pr.head_ref_name} {pr.body}".lower()
                if slice_id.lower() in haystack:
                    pull_request = pr
                    break

        github_execution_state = map_local_state_to_github_state(
            local_state=current_state,
            owner_role=owner_role_project,
            pull_request=pull_request,
        )

        project_field_values = {
            "Execution State": github_execution_state,
            "Role Owner": owner_role_project,
            "Priority": None,
            "Effort": None,
            "Sprint": None,
            "Target Release": None,
            "Epic": None,
            "Blocked Reason": normalized_optional_text(blocking_issues),
        }

        issue_title = f"{slice_id}: {title}"
        slices.append(
            SliceMetadata(
                slice_id=slice_id,
                title=title,
                current_state=current_state,
                owner_role=owner_role,
                owner_role_project=owner_role_project,
                last_updated=last_updated,
                links=links_value,
                branch_name=branch_name,
                blocking_issues=blocking_issues,
                notes=notes,
                folder=slice_dir,
                folder_relative=slice_dir.relative_to(REPO_ROOT).as_posix(),
                docs=docs,
                doc_relpaths=doc_relpaths,
                roadmap_position=roadmap_entry.get("position"),
                roadmap_total=roadmap_entry.get("total"),
                roadmap_title=roadmap_entry.get("title"),
                prd_summary=prd_summary,
                acceptance_criteria=acceptance_criteria,
                acceptance_criteria_total=len(all_acceptance_criteria),
                inferred_labels=inferred_labels,
                placeholder_fields=placeholder_fields,
                issue_title=issue_title,
                github_execution_state=github_execution_state,
                issue_should_be_closed=should_close_issue_for_state(github_execution_state),
                project_field_values=project_field_values,
                pull_request=pull_request,
            )
        )

    return slices


def load_pull_requests(runner: ShellRunner, repo: RepoInfo) -> list[PullRequestRecord]:
    completed = runner.run(
        [
            "gh",
            "pr",
            "list",
            "--repo",
            repo.full_name,
            "--state",
            "all",
            "--limit",
            "200",
            "--json",
            "id,number,title,body,url,headRefName,state,mergedAt",
        ]
    )
    payload = json.loads(completed.stdout)
    return [
        PullRequestRecord(
            id=item["id"],
            number=item["number"],
            title=item["title"],
            body=item.get("body") or "",
            url=item["url"],
            head_ref_name=item.get("headRefName") or "",
            state=item["state"],
            merged_at=item.get("mergedAt"),
        )
        for item in payload
    ]


def load_issues(runner: ShellRunner, repo: RepoInfo) -> list[IssueRecord]:
    completed = runner.run(
        [
            "gh",
            "issue",
            "list",
            "--repo",
            repo.full_name,
            "--state",
            "all",
            "--limit",
            "200",
            "--json",
            "id,number,title,body,url,state,labels",
        ]
    )
    payload = json.loads(completed.stdout)
    return [
        IssueRecord(
            id=item["id"],
            number=item["number"],
            title=item["title"],
            body=item.get("body") or "",
            url=item["url"],
            state=item["state"],
            labels=[label["name"] for label in item.get("labels", [])],
        )
        for item in payload
    ]


def render_managed_issue_section(repo: RepoInfo, slice_meta: SliceMetadata) -> str:
    docs_lines = [
        f"- [{relpath}]({repo_file_url(repo, relpath)})"
        for _, relpath in slice_meta.doc_relpaths.items()
    ]
    ac_lines = [
        f"{index}. {criterion}"
        for index, criterion in enumerate(slice_meta.acceptance_criteria, start=1)
    ]
    docs_block = "\n".join(docs_lines) if docs_lines else "- No slice artifacts discovered."
    ac_block = "\n".join(ac_lines) if ac_lines else "No acceptance criteria were safely inferred from `prd.md`."
    branch_line = f"- Branch: `{slice_meta.branch_name}`" if slice_meta.branch_name else "- Branch: Not discoverable"
    pr_line = (
        f"- Pull Request: [#{slice_meta.pull_request.number}]({slice_meta.pull_request.url}) ({slice_meta.pull_request.state})"
        if slice_meta.pull_request
        else "- Pull Request: Not discoverable"
    )
    roadmap_line = (
        f"{slice_meta.roadmap_position} of {slice_meta.roadmap_total}"
        if slice_meta.roadmap_position and slice_meta.roadmap_total
        else "Not in roadmap"
    )
    blocker_text = slice_meta.blocking_issues or "None"
    notes_text = slice_meta.notes or "None"
    summary_text = slice_meta.prd_summary or "No PRD problem statement summary was safely inferred."

    return "\n".join(
        [
            config.SLICE_MARKER_TEMPLATE.format(slice_id=slice_meta.slice_id),
            config.SYNC_START_MARKER,
            "## BurnMate Slice Sync",
            "",
            f"- Slice ID: `{slice_meta.slice_id}`",
            f"- Current State: `{slice_meta.current_state}`",
            f"- Role Owner: `{slice_meta.owner_role_project}`",
            f"- Last Updated: `{slice_meta.last_updated or 'Unknown'}`",
            f"- Roadmap Position: {roadmap_line}",
            f"- Slice Folder: [{slice_meta.folder_relative}]({repo_file_url(repo, slice_meta.folder_relative)})",
            "",
            "### Slice Docs",
            docs_block,
            "",
            "### Summary",
            summary_text,
            "",
            "### Acceptance Criteria Preview",
            f"Showing {len(slice_meta.acceptance_criteria)} of {slice_meta.acceptance_criteria_total} inferred criteria.",
            ac_block,
            "",
            "### Delivery Links",
            branch_line,
            pr_line,
            "",
            "### Local State Notes",
            f"- Blocking Issues: {blocker_text}",
            f"- Notes: {notes_text}",
            "",
            "Repo docs remain the source of truth for BurnMate slice execution details.",
            config.SYNC_END_MARKER,
        ]
    ).strip()


def merge_managed_issue_section(existing_body: str, managed_section: str) -> str:
    full_pattern = re.compile(
        r"<!-- burnmate:slice-id:[^>]+ -->\s*<!-- burnmate:slice-sync:start -->.*?<!-- burnmate:slice-sync:end -->\s*",
        re.DOTALL,
    )
    if full_pattern.search(existing_body):
        updated = full_pattern.sub(managed_section + "\n\n", existing_body, count=1)
        return updated.strip() + "\n"

    block_pattern = re.compile(
        r"<!-- burnmate:slice-sync:start -->.*?<!-- burnmate:slice-sync:end -->\s*",
        re.DOTALL,
    )
    if block_pattern.search(existing_body):
        updated = block_pattern.sub(managed_section + "\n\n", existing_body, count=1)
        return updated.strip() + "\n"

    if existing_body.strip():
        return f"{managed_section}\n\n{existing_body.strip()}\n"
    return managed_section + "\n"


def find_existing_issue(
    issues: list[IssueRecord],
    *,
    slice_id: str,
    issue_title: str,
) -> IssueRecord | None:
    marker = config.SLICE_MARKER_TEMPLATE.format(slice_id=slice_id)
    for issue in issues:
        if marker in issue.body:
            return issue
    for issue in issues:
        if issue.title == issue_title:
            return issue
    return None


def managed_label_delta(existing_labels: list[str], desired_labels: list[str]) -> tuple[list[str], list[str]]:
    existing_set = set(existing_labels)
    desired_set = set(desired_labels)
    add_labels = sorted(desired_set - existing_set)
    remove_labels = sorted((existing_set & config.MANAGED_LABEL_NAMES) - desired_set)
    return add_labels, remove_labels


def ensure_issue_state(
    runner: ShellRunner,
    repo: RepoInfo,
    issue: IssueRecord,
    *,
    should_be_closed: bool,
    dry_run: bool,
) -> IssueRecord:
    desired_state = "CLOSED" if should_be_closed else "OPEN"
    if issue.state == desired_state:
        return issue

    if dry_run:
        print_info(f"DRY-RUN set issue #{issue.number} state to {desired_state}")
        issue.state = desired_state
        return issue

    if should_be_closed:
        runner.run(
            [
                "gh",
                "issue",
                "close",
                str(issue.number),
                "--repo",
                repo.full_name,
                "--reason",
                "completed",
            ]
        )
    else:
        runner.run(
            [
                "gh",
                "issue",
                "reopen",
                str(issue.number),
                "--repo",
                repo.full_name,
            ]
        )
    issue.state = desired_state
    return issue


def write_tempfile(content: str) -> str:
    temporary = tempfile.NamedTemporaryFile("w", encoding="utf-8", delete=False)
    try:
        temporary.write(content)
        return temporary.name
    finally:
        temporary.close()


def create_issue(
    runner: ShellRunner,
    repo: RepoInfo,
    title: str,
    body: str,
    labels: list[str],
) -> str:
    body_path = write_tempfile(body)
    try:
        args = [
            "gh",
            "issue",
            "create",
            "--repo",
            repo.full_name,
            "--title",
            title,
            "--body-file",
            body_path,
        ]
        for label in labels:
            args.extend(["--label", label])
        completed = runner.run(args)
        issue_url = completed.stdout.strip().splitlines()[-1].strip()
        if not issue_url.startswith("https://github.com/"):
            raise CommandError(f"Unexpected gh issue create output: {completed.stdout.strip()}", exit_code=2)
        return issue_url
    finally:
        Path(body_path).unlink(missing_ok=True)


def fetch_issue_by_url(runner: ShellRunner, repo: RepoInfo, issue_url: str) -> IssueRecord:
    completed = runner.run(
        [
            "gh",
            "issue",
            "view",
            issue_url,
            "--repo",
            repo.full_name,
            "--json",
            "id,number,title,body,url,state,labels",
        ]
    )
    item = json.loads(completed.stdout)
    return IssueRecord(
        id=item["id"],
        number=item["number"],
        title=item["title"],
        body=item.get("body") or "",
        url=item["url"],
        state=item["state"],
        labels=[label["name"] for label in item.get("labels", [])],
    )


def edit_issue(
    runner: ShellRunner,
    repo: RepoInfo,
    issue_number: int,
    *,
    title: str,
    body: str,
    add_labels: list[str],
    remove_labels: list[str],
) -> None:
    body_path = write_tempfile(body)
    try:
        args = [
            "gh",
            "issue",
            "edit",
            str(issue_number),
            "--repo",
            repo.full_name,
            "--title",
            title,
            "--body-file",
            body_path,
        ]
        for label in add_labels:
            args.extend(["--add-label", label])
        for label in remove_labels:
            args.extend(["--remove-label", label])
        runner.run(args)
    finally:
        Path(body_path).unlink(missing_ok=True)


def ensure_issue(
    runner: ShellRunner,
    repo: RepoInfo,
    slice_meta: SliceMetadata,
    existing_issue: IssueRecord | None,
    *,
    dry_run: bool,
) -> IssueRecord:
    managed_section = render_managed_issue_section(repo, slice_meta)
    merged_body = merge_managed_issue_section(existing_issue.body if existing_issue else "", managed_section)
    desired_labels = slice_meta.inferred_labels

    if existing_issue is None:
        if dry_run:
            print_info(f"DRY-RUN create issue {slice_meta.issue_title}")
            return IssueRecord(
                id=f"dry-run-{slice_meta.slice_id}",
                number=0,
                title=slice_meta.issue_title,
                body=merged_body,
                url=f"{repo.url}/issues/dry-run-{slice_meta.slice_id}",
                state="OPEN",
                labels=desired_labels,
            )
        created_url = create_issue(runner, repo, slice_meta.issue_title, merged_body, desired_labels)
        return fetch_issue_by_url(runner, repo, created_url)

    add_labels, remove_labels = managed_label_delta(existing_issue.labels, desired_labels)
    if (
        existing_issue.title == slice_meta.issue_title
        and existing_issue.body == merged_body
        and not add_labels
        and not remove_labels
    ):
        return existing_issue

    if dry_run:
        print_info(
            f"DRY-RUN update issue #{existing_issue.number} {slice_meta.slice_id} "
            f"(add_labels={add_labels}, remove_labels={remove_labels})"
        )
        return IssueRecord(
            id=existing_issue.id,
            number=existing_issue.number,
            title=slice_meta.issue_title,
            body=merged_body,
            url=existing_issue.url,
            state=existing_issue.state,
            labels=sorted((set(existing_issue.labels) - set(remove_labels)) | set(add_labels)),
        )

    edit_issue(
        runner,
        repo,
        existing_issue.number,
        title=slice_meta.issue_title,
        body=merged_body,
        add_labels=add_labels,
        remove_labels=remove_labels,
    )
    refreshed = load_issues(runner, repo)
    updated = find_existing_issue(refreshed, slice_id=slice_meta.slice_id, issue_title=slice_meta.issue_title)
    if updated is None:
        raise CommandError(f"Updated issue for {slice_meta.slice_id} but could not refetch it.", exit_code=2)
    return updated


def ensure_project_item(
    runner: ShellRunner,
    repo: RepoInfo,
    project: ProjectInfo,
    issue: IssueRecord,
    *,
    dry_run: bool,
) -> tuple[ProjectInfo, ProjectItem]:
    existing_item = project.items_by_content_id.get(issue.id) or project.items_by_url.get(issue.url)
    if existing_item:
        return project, existing_item
    if dry_run:
        print_info(f"DRY-RUN add issue #{issue.number} to project {project.title}")
        return project, ProjectItem(id=f"dry-run-{issue.id}", content_id=issue.id, url=issue.url)

    runner.run(
        [
            "gh",
            "project",
            "item-add",
            str(project.number),
            "--owner",
            repo.owner,
            "--url",
            issue.url,
        ]
    )
    refreshed = get_project_by_title(runner, repo.owner, project.title)
    if refreshed is None:
        raise CommandError("Project disappeared after adding an item.", exit_code=2)
    created_item = refreshed.items_by_content_id.get(issue.id) or refreshed.items_by_url.get(issue.url)
    if created_item is None:
        item_list = runner.run(
            [
                "gh",
                "project",
                "item-list",
                str(project.number),
                "--owner",
                repo.owner,
                "--limit",
                "200",
                "--format",
                "json",
            ]
        )
        items_payload = json.loads(item_list.stdout).get("items", [])
        fallback = next(
            (
                item
                for item in items_payload
                if (item.get("content") or {}).get("url") == issue.url
            ),
            None,
        )
        if fallback:
            created_item = ProjectItem(
                id=fallback["id"],
                content_id=issue.id,
                url=issue.url,
                field_values={},
            )
    if created_item is None:
        raise CommandError(f"Issue #{issue.number} was added to the project but item lookup failed.", exit_code=2)
    return refreshed, created_item


def update_project_field(
    runner: ShellRunner,
    project: ProjectInfo,
    item: ProjectItem,
    *,
    field_name: str,
    value: str | None,
    dry_run: bool,
) -> bool:
    field = project.fields.get(field_name)
    if field is None:
        raise ValidationError(
            f"Project field {field_name!r} does not exist in {project.title}. Run bootstrap first."
        )
    current = item.field_values.get(field_name)
    if field.data_type == "SINGLE_SELECT":
        if current == value:
            return False
        if value is None:
            args = [
                "gh",
                "project",
                "item-edit",
                "--id",
                item.id,
                "--project-id",
                project.id,
                "--field-id",
                field.id,
                "--clear",
            ]
        else:
            option_id = field.options.get(value)
            if option_id is None:
                raise ValidationError(f"Field {field_name!r} does not have an option named {value!r}")
            args = [
                "gh",
                "project",
                "item-edit",
                "--id",
                item.id,
                "--project-id",
                project.id,
                "--field-id",
                field.id,
                "--single-select-option-id",
                option_id,
            ]
    else:
        normalized_value = value or None
        if (current or None) == normalized_value:
            return False
        if normalized_value is None:
            args = [
                "gh",
                "project",
                "item-edit",
                "--id",
                item.id,
                "--project-id",
                project.id,
                "--field-id",
                field.id,
                "--clear",
            ]
        else:
            args = [
                "gh",
                "project",
                "item-edit",
                "--id",
                item.id,
                "--project-id",
                project.id,
                "--field-id",
                field.id,
                "--text",
                normalized_value,
            ]

    if dry_run:
        print_info(f"DRY-RUN set field {field_name}={value!r} for project item {item.id}")
        item.field_values[field_name] = value
        return True

    runner.run(args)
    item.field_values[field_name] = value
    return True


def sync_slice(
    runner: ShellRunner,
    repo: RepoInfo,
    project: ProjectInfo | None,
    slice_meta: SliceMetadata,
    issues: list[IssueRecord],
    *,
    dry_run: bool,
) -> tuple[ProjectInfo | None, IssueRecord]:
    existing_issue = find_existing_issue(issues, slice_id=slice_meta.slice_id, issue_title=slice_meta.issue_title)
    issue = ensure_issue(runner, repo, slice_meta, existing_issue, dry_run=dry_run)
    issue = ensure_issue_state(
        runner,
        repo,
        issue,
        should_be_closed=slice_meta.issue_should_be_closed,
        dry_run=dry_run,
    )
    if project is None:
        return None, issue

    project, item = ensure_project_item(runner, repo, project, issue, dry_run=dry_run)
    for field_name, value in slice_meta.project_field_values.items():
        update_project_field(runner, project, item, field_name=field_name, value=value, dry_run=dry_run)
    return project, issue


def ensure_comment_marker(
    runner: ShellRunner,
    repo: RepoInfo,
    issue_number: int,
    *,
    body: str,
    marker: str,
    dry_run: bool,
) -> None:
    comments_completed = runner.run(
        [
            "gh",
            "api",
            f"repos/{repo.full_name}/issues/{issue_number}/comments",
        ]
    )
    comments = json.loads(comments_completed.stdout)
    existing = next((comment for comment in comments if marker in (comment.get("body") or "")), None)

    if dry_run:
        action = "update" if existing else "create"
        print_info(f"DRY-RUN {action} PR link comment on issue #{issue_number}")
        return

    if existing:
        runner.run(
            [
                "gh",
                "api",
                "--method",
                "PATCH",
                f"repos/{repo.full_name}/issues/comments/{existing['id']}",
                "-f",
                f"body={body}",
            ]
        )
    else:
        runner.run(
            [
                "gh",
                "api",
                "--method",
                "POST",
                f"repos/{repo.full_name}/issues/{issue_number}/comments",
                "-f",
                f"body={body}",
            ]
        )


def upsert_pr_reference(
    runner: ShellRunner,
    repo: RepoInfo,
    pr_identifier: str,
    *,
    issue_number: int,
    slice_id: str,
    dry_run: bool,
) -> None:
    completed = runner.run(
        [
            "gh",
            "pr",
            "view",
            pr_identifier,
            "--repo",
            repo.full_name,
            "--json",
            "body",
        ]
    )
    body = json.loads(completed.stdout).get("body") or ""
    if f"#{issue_number}" in body or config.PR_REF_START_MARKER in body:
        new_body = re.sub(
            rf"{re.escape(config.PR_REF_START_MARKER)}.*?{re.escape(config.PR_REF_END_MARKER)}",
            "\n".join(
                [
                    config.PR_REF_START_MARKER,
                    f"Refs #{issue_number} (`{slice_id}`)",
                    config.PR_REF_END_MARKER,
                ]
            ),
            body,
            flags=re.DOTALL,
        )
        if new_body == body and config.PR_REF_START_MARKER not in body:
            return
    else:
        block = "\n".join(
            [
                config.PR_REF_START_MARKER,
                f"Refs #{issue_number} (`{slice_id}`)",
                config.PR_REF_END_MARKER,
            ]
        )
        new_body = f"{body.rstrip()}\n\n{block}\n" if body.strip() else block + "\n"

    if dry_run:
        print_info(f"DRY-RUN update PR body for {pr_identifier} with issue #{issue_number}")
        return

    body_path = write_tempfile(new_body)
    try:
        runner.run(
            [
                "gh",
                "pr",
                "edit",
                pr_identifier,
                "--repo",
                repo.full_name,
                "--body-file",
                body_path,
            ]
        )
    finally:
        Path(body_path).unlink(missing_ok=True)


def resolve_pr_identifier(
    runner: ShellRunner,
    repo: RepoInfo,
    slice_meta: SliceMetadata,
    explicit_pr: str | None,
) -> tuple[str, PullRequestRecord]:
    pull_requests = load_pull_requests(runner, repo)
    if explicit_pr:
        if explicit_pr.isdigit():
            match = next((pr for pr in pull_requests if pr.number == int(explicit_pr)), None)
            if match:
                return str(match.number), match
        match = next((pr for pr in pull_requests if pr.url == explicit_pr), None)
        if match:
            return str(match.number), match
        raise ValidationError(f"Could not resolve pull request {explicit_pr!r}")

    if slice_meta.pull_request:
        return str(slice_meta.pull_request.number), slice_meta.pull_request

    current_branch = runner.run(["git", "branch", "--show-current"]).stdout.strip()
    if current_branch:
        match = next((pr for pr in pull_requests if pr.head_ref_name == current_branch), None)
        if match:
            return str(match.number), match

    raise ValidationError(
        f"Could not infer a pull request for {slice_meta.slice_id}; pass --pr explicitly."
    )


def ensure_local_status(slice_meta: SliceMetadata, expected_state: str) -> None:
    if slice_meta.github_execution_state != expected_state:
        raise ValidationError(
            f"{slice_meta.slice_id} maps to GitHub state {slice_meta.github_execution_state}, not {expected_state}. "
            "Update state.md/index.md first or use --allow-drift."
        )


def ensure_local_owner(slice_meta: SliceMetadata, expected_owner: str) -> None:
    normalized = slice_meta.owner_role_project
    if normalized != expected_owner:
        raise ValidationError(
            f"{slice_meta.slice_id} is owned by {normalized} in docs, not {expected_owner}. "
            "Update state.md/index.md first or use --allow-drift."
        )


def load_slice_lookup(repo: RepoInfo, runner: ShellRunner) -> dict[str, SliceMetadata]:
    index_entries = parse_index_entries()
    roadmap_entries = parse_roadmap_entries()
    pull_requests = load_pull_requests(runner, repo)
    return {
        slice_meta.slice_id: slice_meta
        for slice_meta in build_slice_metadata(repo, index_entries, roadmap_entries, pull_requests)
    }


def bootstrap_command(args: argparse.Namespace) -> int:
    runner = ShellRunner()
    ensure_dependencies()
    require_project_scopes(runner)
    repo = detect_repo_info(runner)

    project = get_project_by_title(runner, repo.owner, config.PROJECT_TITLE)
    created_project = False
    if project is None:
        project = create_project(runner, repo.owner, config.PROJECT_TITLE)
        created_project = True

    ensure_project_link(runner, repo, project)
    labels = ensure_labels(runner, repo)
    project, created_fields = ensure_project_fields(runner, repo.owner, project)

    print(f"Repository: {repo.full_name}")
    print(f"Project: {project.title} (#{project.number})")
    print(f"Project URL: {project.url}")
    print(f"Project Created: {'yes' if created_project else 'no'}")
    print(f"Labels Ensured: {', '.join(labels)}")
    print(f"Fields Created: {', '.join(created_fields) if created_fields else 'none'}")
    print("Views: manual setup required; see tools/github/README.md")
    print("Next: python3 tools/github/sync_slices_to_github.py --all")
    return 0


def sync_command(args: argparse.Namespace) -> int:
    runner = ShellRunner()
    ensure_dependencies()
    repo = detect_repo_info(runner)
    if not args.no_validate:
        run_preflight_validators(runner)

    require_project = not args.dry_run
    project: ProjectInfo | None = None
    if require_project:
        require_project_scopes(runner)
        project = get_project_by_title(runner, repo.owner, config.PROJECT_TITLE)
        if project is None:
            raise ValidationError(
                f"GitHub project {config.PROJECT_TITLE!r} was not found. Run bootstrap first."
            )
    else:
        try:
            if has_project_read_scope(get_gh_auth_scopes(runner)):
                project = get_project_by_title(runner, repo.owner, config.PROJECT_TITLE)
        except ToolError:
            project = None

    slice_lookup = load_slice_lookup(repo, runner)
    if args.slice_id:
        selected_ids = [args.slice_id]
    else:
        selected_ids = sorted(slice_lookup)

    issues = load_issues(runner, repo)
    for slice_id in selected_ids:
        slice_meta = slice_lookup.get(slice_id)
        if slice_meta is None:
            raise ValidationError(f"Unknown slice: {slice_id}")
        project, issue = sync_slice(runner, repo, project, slice_meta, issues, dry_run=args.dry_run)
        issues = [existing for existing in issues if existing.number != issue.number] + [issue]
        print_info(f"Synchronized {slice_id} -> {issue.title}")

    return 0


def set_status_command(args: argparse.Namespace) -> int:
    runner = ShellRunner()
    ensure_dependencies()
    repo = detect_repo_info(runner)
    if not args.no_validate:
        run_preflight_validators(runner)
    require_project_scopes(runner)

    canonical_state = normalize_state_input(args.state)
    owner_override = normalize_role_input(args.owner) if args.owner else None
    slice_lookup = load_slice_lookup(repo, runner)
    slice_meta = slice_lookup.get(args.slice_id)
    if slice_meta is None:
        raise ValidationError(f"Unknown slice: {args.slice_id}")
    if not args.allow_drift:
        ensure_local_status(slice_meta, canonical_state)
        if owner_override:
            ensure_local_owner(slice_meta, owner_override)

    issues = load_issues(runner, repo)
    issue = find_existing_issue(issues, slice_id=slice_meta.slice_id, issue_title=slice_meta.issue_title)
    if issue is None:
        raise ValidationError(f"No GitHub issue exists for {slice_meta.slice_id}. Run sync first.")
    project = get_project_by_title(runner, repo.owner, config.PROJECT_TITLE)
    if project is None:
        raise ValidationError(f"GitHub project {config.PROJECT_TITLE!r} was not found. Run bootstrap first.")

    project, item = ensure_project_item(runner, repo, project, issue, dry_run=args.dry_run)
    update_project_field(
        runner,
        project,
        item,
        field_name="Execution State",
        value=canonical_state,
        dry_run=args.dry_run,
    )
    issue = ensure_issue_state(
        runner,
        repo,
        issue,
        should_be_closed=should_close_issue_for_state(canonical_state),
        dry_run=args.dry_run,
    )
    if owner_override:
        update_project_field(
            runner,
            project,
            item,
            field_name="Role Owner",
            value=owner_override,
            dry_run=args.dry_run,
        )
    if args.comment:
        if args.dry_run:
            print_info(f"DRY-RUN comment on issue #{issue.number}: {args.comment}")
        else:
            runner.run(
                [
                    "gh",
                    "issue",
                    "comment",
                    str(issue.number),
                    "--repo",
                    repo.full_name,
                    "--body",
                    args.comment,
                ]
            )
    return 0


def set_owner_command(args: argparse.Namespace) -> int:
    runner = ShellRunner()
    ensure_dependencies()
    repo = detect_repo_info(runner)
    if not args.no_validate:
        run_preflight_validators(runner)
    require_project_scopes(runner)

    canonical_owner = normalize_role_input(args.owner)
    slice_lookup = load_slice_lookup(repo, runner)
    slice_meta = slice_lookup.get(args.slice_id)
    if slice_meta is None:
        raise ValidationError(f"Unknown slice: {args.slice_id}")
    if not args.allow_drift:
        ensure_local_owner(slice_meta, canonical_owner)

    issues = load_issues(runner, repo)
    issue = find_existing_issue(issues, slice_id=slice_meta.slice_id, issue_title=slice_meta.issue_title)
    if issue is None:
        raise ValidationError(f"No GitHub issue exists for {slice_meta.slice_id}. Run sync first.")
    project = get_project_by_title(runner, repo.owner, config.PROJECT_TITLE)
    if project is None:
        raise ValidationError(f"GitHub project {config.PROJECT_TITLE!r} was not found. Run bootstrap first.")

    project, item = ensure_project_item(runner, repo, project, issue, dry_run=args.dry_run)
    update_project_field(
        runner,
        project,
        item,
        field_name="Role Owner",
        value=canonical_owner,
        dry_run=args.dry_run,
    )
    return 0


def attach_pr_command(args: argparse.Namespace) -> int:
    runner = ShellRunner()
    ensure_dependencies()
    repo = detect_repo_info(runner)
    if not args.no_validate:
        run_preflight_validators(runner)

    slice_lookup = load_slice_lookup(repo, runner)
    slice_meta = slice_lookup.get(args.slice_id)
    if slice_meta is None:
        raise ValidationError(f"Unknown slice: {args.slice_id}")

    issues = load_issues(runner, repo)
    issue = find_existing_issue(issues, slice_id=slice_meta.slice_id, issue_title=slice_meta.issue_title)
    if issue is None:
        raise ValidationError(f"No GitHub issue exists for {slice_meta.slice_id}. Run sync first.")

    pr_identifier, pull_request = resolve_pr_identifier(runner, repo, slice_meta, args.pr)
    comment_body = "\n".join(
        [
            config.PR_COMMENT_MARKER_TEMPLATE.format(slice_id=slice_meta.slice_id),
            f"Linked PR: [#{pull_request.number}]({pull_request.url})",
        ]
    )
    ensure_comment_marker(
        runner,
        repo,
        issue.number,
        body=comment_body,
        marker=config.PR_COMMENT_MARKER_TEMPLATE.format(slice_id=slice_meta.slice_id),
        dry_run=args.dry_run,
    )
    upsert_pr_reference(
        runner,
        repo,
        pr_identifier,
        issue_number=issue.number,
        slice_id=slice_meta.slice_id,
        dry_run=args.dry_run,
    )

    if args.set_status:
        status_args = argparse.Namespace(
            slice_id=args.slice_id,
            state=args.set_status,
            owner=args.owner,
            comment=args.comment,
            dry_run=args.dry_run,
            allow_drift=args.allow_drift,
            no_validate=True,
        )
        return set_status_command(status_args)

    if args.comment:
        if args.dry_run:
            print_info(f"DRY-RUN comment on issue #{issue.number}: {args.comment}")
        else:
            runner.run(
                [
                    "gh",
                    "issue",
                    "comment",
                    str(issue.number),
                    "--repo",
                    repo.full_name,
                    "--body",
                    args.comment,
                ]
            )
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="BurnMate GitHub execution tooling")
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("bootstrap", help="Create the execution project, fields, and labels.")

    sync_parser = subparsers.add_parser("sync", help="Sync slice docs into GitHub issues and project fields.")
    sync_scope = sync_parser.add_mutually_exclusive_group(required=True)
    sync_scope.add_argument("--all", action="store_true", help="Sync all discovered slices.")
    sync_scope.add_argument("--slice", dest="slice_id", help="Sync a single slice by ID.")
    sync_parser.add_argument("--dry-run", action="store_true", help="Show what would change without mutating GitHub.")
    sync_parser.add_argument("--no-validate", action="store_true", help="Skip local registry/state-machine validators.")

    status_parser = subparsers.add_parser("set-status", help="Set Execution State and optional Role Owner.")
    status_parser.add_argument("slice_id", help="Slice identifier, for example SLICE-0008.")
    status_parser.add_argument("state", help="Execution state or friendly alias.")
    status_parser.add_argument("--owner", help="Optional role owner to set at the same time.")
    status_parser.add_argument("--comment", help="Optional issue comment to leave after the state change.")
    status_parser.add_argument("--dry-run", action="store_true", help="Show what would change without mutating GitHub.")
    status_parser.add_argument("--allow-drift", action="store_true", help="Skip local state/owner match checks.")
    status_parser.add_argument("--no-validate", action="store_true", help="Skip local registry/state-machine validators.")

    owner_parser = subparsers.add_parser("set-owner", help="Set Role Owner only.")
    owner_parser.add_argument("slice_id", help="Slice identifier, for example SLICE-0008.")
    owner_parser.add_argument("owner", help="Role owner or friendly alias.")
    owner_parser.add_argument("--dry-run", action="store_true", help="Show what would change without mutating GitHub.")
    owner_parser.add_argument("--allow-drift", action="store_true", help="Skip local owner match checks.")
    owner_parser.add_argument("--no-validate", action="store_true", help="Skip local registry/state-machine validators.")

    attach_parser = subparsers.add_parser("attach-pr", help="Link a PR to a slice issue and optional status transition.")
    attach_parser.add_argument("slice_id", help="Slice identifier, for example SLICE-0008.")
    attach_parser.add_argument("--pr", help="PR number or full URL. If omitted, infer from slice metadata/current branch.")
    attach_parser.add_argument("--comment", help="Optional issue comment to leave after linking.")
    attach_parser.add_argument("--set-status", help="Optional follow-on execution state.")
    attach_parser.add_argument("--owner", help="Optional follow-on role owner when --set-status is used.")
    attach_parser.add_argument("--dry-run", action="store_true", help="Show what would change without mutating GitHub.")
    attach_parser.add_argument("--allow-drift", action="store_true", help="Skip local state/owner match checks.")
    attach_parser.add_argument("--no-validate", action="store_true", help="Skip local registry/state-machine validators.")

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        if args.command == "bootstrap":
            return bootstrap_command(args)
        if args.command == "sync":
            if args.all:
                args.slice_id = None
            return sync_command(args)
        if args.command == "set-status":
            return set_status_command(args)
        if args.command == "set-owner":
            return set_owner_command(args)
        if args.command == "attach-pr":
            return attach_pr_command(args)
        raise CommandError(f"Unsupported command: {args.command}", exit_code=2)
    except ToolError as error:
        print(error, file=sys.stderr)
        return error.exit_code


if __name__ == "__main__":
    raise SystemExit(main())
