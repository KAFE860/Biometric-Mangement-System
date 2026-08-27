# Biometric Management project
System Overview
The application is a biometric attendance and shift management platform designed for the support staff at Pan-Atlantic University. It specifically targets the Security, Horticulture, Facility, Cafeteria, and Maintenance units to replace an inefficient manual tracking process. The full-stack solution is intended to improve accountability, streamline attendance monitoring, and automate shift scheduling through role-based access and fingerprint authentication.

Core Capabilities
User Registration & Security: The system captures comprehensive staff details, including biometric data (fingerprints) and passport photos (via webcam or local upload). Passwords are automatically generated, securely hashed using industry standards, and emailed to users.
Shift & Roster Management: Supervisors can bulk-upload and download unit rosters via CSV files. The system automatically distributes shift assignments to staff via email and handles the complete workflow for shift swap requests, updating the active roster automatically upon supervisor approval.
Biometric Attendance Tracking: The system records exact sign-in and sign-out times using fingerprint authentication, immediately emailing timestamp receipts to the respective staff member.
Automated Notifications: Triggers automated email alerts for account creation, password resets, roster publications, shift swap approvals, and daily sign-in/out events.
Reporting & Analytics: Generates real-time dashboards and comprehensive daily, weekly, and monthly attendance reports that explicitly highlight absences, late arrivals, and early departures.

Role-Based Access & Permissions
The system enforces strict access control across four distinct user tiers:
Non-Supervisory Staff: Can log in to view their personal records and assigned shifts, request shift swaps with colleagues within their specific unit, and track their own attendance.
Unit Supervisors: Have administrative control isolated to their specific unit. They can approve or decline shift swaps, manage rosters, and access real-time dashboards and filtered logs (by date or staff ID) for their team.
IT Administrators: Responsible for system-wide maintenance, full user account creation/registration, and overriding password resets.

Director of Services & Head of IT: Hold global oversight privileges. They can view, filter (by date, unit, or staff ID), and download system-wide sign-in logs, staff details, and rosters in CSV format for all departments.
