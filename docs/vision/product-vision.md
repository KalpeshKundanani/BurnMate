# BurnMate – Product Vision

## 1. Introduction

BurnMate is a simple, motivation-driven weight management application designed to help users reach their target body weight by focusing on **cumulative calorie deficit**, referred to as **calorie debt**.

Most weight loss applications focus heavily on strict daily calorie counting, complex food databases, or rigid diet plans. BurnMate takes a different approach. Instead of overwhelming users with daily constraints, the app visualizes the **total calorie deficit required to reach a goal weight** and allows users to see how each day's actions affect that number.

The goal of BurnMate is to make weight management **numerical, transparent, and motivating**.

Users are not forced to follow rigid diets. Instead, they can see how their behavior changes their overall progress toward the target weight.

---

# 2. Core Concept

Weight loss is fundamentally driven by energy balance.

A commonly accepted approximation in nutrition science:

1 kilogram of body fat ≈ 7700 kcal

BurnMate uses this relationship to calculate the **total calorie deficit required to reach the user's goal weight**.

### Example

Current weight: 82 kg  
Goal weight: 72 kg  

Weight difference:

82 - 72 = 10 kg

Total calorie deficit required:

10 × 7700 = 77,000 kcal

BurnMate will display:

Remaining calorie debt: 77,000 kcal

Every day:

net_deficit = calories_burned - calories_consumed

Remaining debt is updated:

remaining_debt = previous_debt - net_deficit

The user’s mission is simply to **bring this number down to zero**.

---

# 3. Target Users

BurnMate is designed for individuals who want to manage their body weight using a **simple numerical model** rather than complicated diet systems.

Primary users include:

• Individuals trying to lose weight  
• Individuals trying to stay accountable to daily calorie habits  
• Users who enjoy progress tracking through numbers  
• People who prefer **minimalistic tracking instead of heavy food databases**

BurnMate especially appeals to users who:

• want clarity about how their daily actions affect long-term weight goals  
• prefer motivation through progress metrics  
• want an app that feels lightweight and simple

---

# 4. Product Philosophy

BurnMate follows several design principles.

### Simplicity First

The application should avoid unnecessary complexity.

Users should not need to log dozens of food items or manage complex nutrition breakdowns.

The focus is on **calorie balance and progress tracking**.

---

### Motivation Through Visibility

Users remain motivated when they can **see progress clearly**.

The application emphasizes:

• remaining calorie debt  
• progress trends over time  
• visual feedback for improvement or regression

---

### Minimal Friction

Users should be able to record daily actions quickly.

Logging should require **minimal steps**.

---

### Long-Term Thinking

Weight change occurs over weeks or months.

BurnMate emphasizes cumulative progress instead of daily perfection.

---

# 5. Core User Inputs

## Onboarding Inputs

When a user first installs BurnMate, they will provide:

• Gender  
• Height  
• Current weight  
• Goal weight  

These values allow the application to:

• calculate BMI
• determine healthy weight ranges
• compute calorie debt

---

## Daily Inputs

Users will record daily actions affecting energy balance.

### Food Intake

Users will manually enter calories consumed.

Example inputs:

Lunch: 650 kcal
Dinner: 900 kcal
Snacks: 300 kcal

Total intake is calculated for the day.

---

### Exercise / Activity

Users can manually enter calories burned through exercise.

Example:

Running: 350 kcal
Cycling: 200 kcal

---

### Steps

Steps will be automatically imported using **Google Fit**.

The application will estimate calories burned from steps.

---

# 6. Data Model Concept

Each day records:

date
calories_consumed
calories_burned_manual
calories_burned_steps
net_calorie_deficit
remaining_calorie_debt

Daily records accumulate to update the overall progress.

---

# 7. Core Experience Flow

### Step 1 — Authentication

User logs in using **Google account**.

---

### Step 2 — Permission Request

App requests permission for:

• Google Fit step data

---

### Step 3 — Onboarding

User enters:

• gender  
• height  
• current weight  
• goal weight  

BurnMate calculates:

total calorie debt

---

### Step 4 — Dashboard

User lands on the main dashboard.

The dashboard shows:

• remaining calorie debt (primary metric)
• progress trend chart
• today's activity summary

---

### Step 5 — Daily Logging

Users log:

• calories consumed  
• exercise calories burned  

Step data is imported automatically.

---

### Step 6 — Progress Visualization

A chart displays calorie debt trends over time.

Chart range:

-15 days history
+7 days projected

---

# 8. Dashboard Design Concept

The dashboard should highlight the most important metric.

### Primary Metric

Remaining Calorie Debt

This should appear prominently at the top.

---

### Progress Chart

Below the debt value:

A **scrollable line chart** visualizes:

• rising debt (bad days)  
• falling debt (good days)

This creates clear visual feedback.

---

### Supporting Metrics

Dashboard may show:

• today's intake
• today's calories burned
• net deficit for the day

---

# 9. Historical Navigation

Users should be able to:

• navigate to past dates  
• edit historical entries  

A **date picker** allows quick navigation across days.

---

# 10. Design Philosophy

BurnMate will use a **premium, modern visual style** inspired by minimalistic fintech applications.

Design characteristics:

• AMOLED dark theme  
• neon accent colors  
• subtle translucent glass UI elements  
• bold typography for key metrics

The UI should feel **clean, futuristic, and motivational**.

---

# 11. MVP Scope

The first version of BurnMate includes:

### Onboarding
• Google login  
• permissions  
• height / weight / goal input

---

### Core Engine
• calorie debt calculation  
• daily deficit calculation

---

### Tracking
• manual calorie intake logging  
• manual exercise logging  
• Google Fit step integration

---

### Dashboard
• remaining calorie debt display  
• progress chart  
• daily summary

---

### History
• date picker navigation  
• editable past records

---

# 12. Non-Goals (MVP)

The MVP will intentionally exclude:

• diet plans  
• food database search  
• macro tracking (protein/carbs/fat)  
• social features  
• coaching systems  
• wearable integrations beyond Google Fit  
• AI nutrition recommendations

These may be considered in future versions.

---

# 13. Success Criteria

BurnMate will be considered successful if users:

• clearly understand their remaining calorie debt  
• consistently log daily activities  
• stay motivated by watching progress trends  
• reach their target weight over time

The application should feel **simple, empowering, and transparent**.

---

# 14. Long-Term Vision

In future versions BurnMate may evolve to include:

• predictive weight trend modeling  
• calorie burn estimation improvements  
• wearable integrations  
• habit tracking  
• AI-assisted recommendations

However, the core concept of **calorie debt as the primary metric** will remain the central pillar of the product.

---

# 15. Guiding Principle

BurnMate should always answer one simple question for the user:

How far am I from my goal weight today?

And show how today's actions move that number.