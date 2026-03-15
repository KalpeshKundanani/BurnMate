# LLD: SLICE-XXXX — <!-- Slice Name -->

**Author:** <!-- Architect agent/person -->
**Date:** <!-- YYYY-MM-DD -->
**HLD Reference:** `docs/slices/SLICE-XXXX/hld.md`
**PRD Reference:** `docs/slices/SLICE-XXXX/prd.md`

---

## Interfaces / APIs

<!-- Every public function, endpoint, or method the Engineer must implement. -->

### <!-- Endpoint or Module Name -->

```
Method: POST
Path:   /api/v1/example
Auth:   Bearer token

Request Body:
{
  "field_a": string (required),
  "field_b": integer (optional, default: 0)
}

Response 200:
{
  "id": string,
  "status": "created"
}

Response 400:
{
  "error": "validation_error",
  "details": [...]
}
```

### <!-- Function Signature (if internal module) -->

```python
def process_item(item_id: str, options: ProcessOptions) -> ProcessResult:
    """
    Processes a single item according to business rules.
    Raises: ItemNotFoundError, ValidationError
    """
```

## Data Models

<!-- Every model the Engineer must create or modify. -->

```python
@dataclass
class ExampleModel:
    id: str
    name: str
    status: Literal["active", "inactive"]
    created_at: datetime
```

## Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| `field_a` | Non-empty string, max 255 chars | `INVALID_FIELD_A` |
| `field_b` | Integer >= 0 | `INVALID_FIELD_B` |

## Error Handling Contracts

| Error Condition | HTTP Status | Error Code | Recovery |
|---|---|---|---|
| Missing required field | 400 | `VALIDATION_ERROR` | Return all violations |
| Resource not found | 404 | `NOT_FOUND` | — |
| Downstream service timeout | 502 | `UPSTREAM_TIMEOUT` | Retry up to 3x with exponential backoff |

## Algorithms

<!-- For any non-trivial logic, describe the algorithm deterministically. -->

```
1. Receive input
2. Validate against schema
3. Check preconditions (state == expected)
4. Execute transformation
5. Persist result
6. Return response
```

## Persistence Schema Changes

<!-- If this slice modifies the database schema. Remove if N/A. -->

```sql
ALTER TABLE examples ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active';
CREATE INDEX idx_examples_status ON examples(status);
```

## External Integration Contracts

<!-- Stub definitions for any external service calls. -->

| Service | Endpoint | Method | Contract |
|---|---|---|---|
| <!-- e.g., Email Service --> | `/send` | POST | `{ to: string, subject: string, body: string }` |

## Unit Test Cases

<!-- Every test the Engineer must write. Be specific enough that tests are deterministic. -->

| ID | Test Case | Input | Expected Output |
|---|---|---|---|
| T-01 | Valid input creates resource | `{ field_a: "test", field_b: 5 }` | 200, resource created |
| T-02 | Missing required field returns 400 | `{ field_b: 5 }` | 400, `VALIDATION_ERROR` |
| T-03 | Negative field_b rejected | `{ field_a: "test", field_b: -1 }` | 400, `INVALID_FIELD_B` |

## Definition of Done — CODE_COMPLETE Checklist

The Engineer must satisfy ALL of the following before transitioning to `CODE_COMPLETE`:

- [ ] All interfaces/APIs above are implemented
- [ ] All data models are created
- [ ] All validation rules are enforced
- [ ] All error handling contracts are implemented
- [ ] All unit test cases above are written and passing
- [ ] No TODO/FIXME/HACK comments remain in slice code
- [ ] Code compiles/lints without errors
- [ ] No features beyond this LLD are implemented
