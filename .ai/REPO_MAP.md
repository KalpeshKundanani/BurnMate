# Repository Truth Map

## Purpose

This document records the repository structure that actually exists on disk at the time it was generated. It exists to prevent agents from referencing invented files, folders, validators, or artifacts.

## Agent Rule

Before executing any role or task:

1. Read `.ai/REPO_MAP.md`.
2. Confirm every referenced file or directory exists in the repository.
3. If any required path is missing, stop immediately and report the discrepancy.

Do not infer missing files from templates, examples, prior runs, chat history, or expected framework shape.

## Repository Structure

```text
/
|-- .ai/
|   |-- CONTEXT_CAPSULE.md
|   |-- ENTRYPOINT.md
|   |-- GO_NO_GO_RUBRICS.md
|   |-- OPERATING_PRINCIPLES.md
|   |-- ORCHESTRATION_PROTOCOL.md
|   |-- OUTPUT_FORMATS.md
|   |-- REPO_MAP.md
|   |-- ROLES.md
|   |-- ROLE_PROMPTS.md
|   `-- STATE_MACHINE.md
|-- .github/
|   |-- ISSUE_TEMPLATE/
|   |   |-- defect.md
|   |   `-- new-slice.md
|   |-- pull_request_template.md
|   `-- workflows/
|       `-- validators.yml
|-- composeApp/
|   |-- build.gradle.kts
|   `-- src/
|       |-- androidMain/
|       |   |-- AndroidManifest.xml
|       |   |-- kotlin/org/kalpeshbkundanani/burnmate/
|       |   |   |-- MainActivity.kt
|       |   |   `-- Platform.android.kt
|       |   `-- res/
|       |       |-- drawable/
|       |       |   `-- ic_launcher_background.xml
|       |       |-- drawable-v24/
|       |       |   `-- ic_launcher_foreground.xml
|       |       |-- mipmap-anydpi-v26/
|       |       |   |-- ic_launcher.xml
|       |       |   `-- ic_launcher_round.xml
|       |       |-- mipmap-hdpi/
|       |       |   |-- ic_launcher.png
|       |       |   `-- ic_launcher_round.png
|       |       |-- mipmap-mdpi/
|       |       |   |-- ic_launcher.png
|       |       |   `-- ic_launcher_round.png
|       |       |-- mipmap-xhdpi/
|       |       |   |-- ic_launcher.png
|       |       |   `-- ic_launcher_round.png
|       |       |-- mipmap-xxhdpi/
|       |       |   |-- ic_launcher.png
|       |       |   `-- ic_launcher_round.png
|       |       |-- mipmap-xxxhdpi/
|       |       |   |-- ic_launcher.png
|       |       |   `-- ic_launcher_round.png
|       |       `-- values/
|       |           `-- strings.xml
|       |-- commonMain/
|       |   |-- composeResources/
|       |   |   `-- drawable/
|       |   |       `-- compose-multiplatform.xml
|       |   `-- kotlin/org/kalpeshbkundanani/burnmate/
|       |       |-- App.kt
|       |       |-- Greeting.kt
|       |       |-- Platform.kt
|       |       `-- caloriedebt/
|       |           |-- domain/
|       |           |   |-- CalorieDebtCalculator.kt
|       |           |   |-- CalorieDebtValidator.kt
|       |           |   |-- DebtTrendClassifier.kt
|       |           |   |-- DefaultCalorieDebtCalculator.kt
|       |           |   |-- DefaultCalorieDebtValidator.kt
|       |           |   `-- DefaultDebtTrendClassifier.kt
|       |           `-- model/
|       |               |-- CalculationWindow.kt
|       |               |-- CalorieDebtDay.kt
|       |               |-- CalorieDebtError.kt
|       |               |-- CalorieDebtResult.kt
|       |               |-- CalorieDebtSeverity.kt
|       |               |-- CalorieDebtTrend.kt
|       |               `-- DailyCalorieEntry.kt
|       |       `-- profile/
|       |           |-- domain/
|       |           |   |-- BmiCalculator.kt
|       |           |   |-- DefaultBmiCalculator.kt
|       |           |   |-- DefaultHealthyGoalValidator.kt
|       |           |   |-- DefaultProfileMetricsValidator.kt
|       |           |   |-- DefaultUserProfileFactory.kt
|       |           |   |-- HealthyGoalValidator.kt
|       |           |   |-- ProfileMetricsValidator.kt
|       |           |   `-- UserProfileFactory.kt
|       |           `-- model/
|       |               |-- BmiCategory.kt
|       |               |-- BmiSnapshot.kt
|       |               |-- BodyMetrics.kt
|       |               |-- GoalValidationReason.kt
|       |               |-- GoalValidationResult.kt
|       |               |-- ProfileDomainError.kt
|       |               `-- UserProfileSummary.kt
|       |-- commonTest/
|       |   `-- kotlin/org/kalpeshbkundanani/burnmate/
|       |       |-- ComposeAppCommonTest.kt
|       |       |-- caloriedebt/
|       |           |-- DefaultCalorieDebtCalculatorTest.kt
|       |           `-- DefaultCalorieDebtValidatorTest.kt
|       |       `-- profile/
|       |           |-- DefaultBmiCalculatorTest.kt
|       |           |-- DefaultHealthyGoalValidatorTest.kt
|       |           `-- DefaultUserProfileFactoryTest.kt
|       `-- iosMain/
|           `-- kotlin/org/kalpeshbkundanani/burnmate/
|               |-- MainViewController.kt
|               `-- Platform.ios.kt
|-- docs/
|   |-- architecture/
|   |   |-- .gitkeep
|   |   |-- PHASE4_VALIDATORS.md
|   |   `-- technical-approach.md
|   |-- slices/
|   |   |-- PHASE3_COMPLIANCE_AUDIT.md
|   |   |-- PHASE4_AUDIT_REPORT.md
|   |   |-- README.md
|   |   |-- RETROFIT_AUDIT_REPORT.md
|   |   |-- ROADMAP.md
|   |   |-- index.md
|   |   |-- SLICE-0001/
|   |   |   |-- contract.md
|   |   |   `-- state.md
|   |   |-- SLICE-0002/
|   |   |   |-- audit-report.md
|   |   |   |-- contract.md
|   |   |   |-- hld.md
|   |   |   |-- lld.md
|   |   |   |-- prd.md
|   |   |   |-- qa.md
|   |   |   |-- review.md
|   |   |   `-- state.md
|   |   |-- SLICE-0003/
|   |   |   |-- contract.md
|   |   |   |-- hld.md
|   |   |   |-- lld.md
|   |   |   |-- prd.md
|   |   |   `-- state.md
|   |   `-- _templates/
|   |       |-- audit-report.md
|   |       |-- change-request.md
|   |       |-- contract-template.md
|   |       |-- hld.md
|   |       |-- lld.md
|   |       |-- prd.md
|   |       |-- qa.md
|   |       |-- review.md
|   |       |-- state.md
|   |       `-- test-plan.md
|   `-- vision/
|       |-- .gitkeep
|       `-- product-vision.md
|-- gradle/
|   |-- gradle-daemon-jvm.properties
|   |-- libs.versions.toml
|   `-- wrapper/
|       |-- gradle-wrapper.jar
|       `-- gradle-wrapper.properties
|-- iosApp/
|   |-- Configuration/
|   |   `-- Config.xcconfig
|   |-- iosApp/
|   |   |-- Assets.xcassets/
|   |   |   |-- AccentColor.colorset/Contents.json
|   |   |   |-- AppIcon.appiconset/
|   |   |   |   |-- app-icon-1024.png
|   |   |   |   `-- Contents.json
|   |   |   `-- Contents.json
|   |   |-- ContentView.swift
|   |   |-- Info.plist
|   |   |-- Preview Content/
|   |   |   `-- Preview Assets.xcassets/Contents.json
|   |   `-- iOSApp.swift
|   `-- iosApp.xcodeproj/
|       |-- project.pbxproj
|       |-- project.xcworkspace/
|       |   `-- contents.xcworkspacedata
|       `-- xcuserdata/
|           `-- kalpeshkundanani.xcuserdatad/
|               `-- xcschemes/
|                   |-- iosApp.xcscheme
|                   `-- xcschememanagement.plist
|-- scripts/
|   |-- .gitkeep
|   |-- README.md
|   |-- validate_all.sh
|   |-- validate_doc_freeze.py
|   |-- validate_pr_checklist.py
|   |-- validate_required_artifacts.py
|   |-- validate_slice_registry.py
|   |-- validate_state_machine_transitions.py
|   `-- tests/
|       |-- __init__.py
|       |-- test_validators.py
|       `-- fixtures/
|           |-- frozen_violation_slice/
|           |   |-- hld.md
|           |   |-- lld.md
|           |   |-- prd.md
|           |   `-- state.md
|           |-- invalid_state_slice/
|           |   `-- state.md
|           |-- missing_artifacts_slice/
|           |   |-- prd.md
|           |   `-- state.md
|           |-- registry_mismatch/
|           |   `-- index.md
|           `-- valid_slice/
|               |-- hld.md
|               |-- lld.md
|               |-- prd.md
|               `-- state.md
|-- .DS_Store
|-- .gitignore
|-- README.md
|-- build.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- local.properties
`-- settings.gradle.kts
```

## Notes

- This map intentionally lists only paths confirmed on disk.
- Generated directories such as `.gradle/`, `.kotlin/`, `build/`, and `composeApp/build/` exist locally but are not part of the framework source of truth.
- The canonical implementation path for `SLICE-0003` is `org.kalpeshbkundanani.burnmate.profile`, which maps to:
  - `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/profile`
  - `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/profile`
- The `profile/` directories are now present on disk under `composeApp/src/commonMain/...` and `composeApp/src/commonTest/...` as part of `SLICE-0003`.
- If the repository changes, update this file in the same change set that introduces or removes the affected paths.
