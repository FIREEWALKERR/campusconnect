# Ten-minute demonstration

1. Start MySQL and run the app with the `dev` profile. Open the homepage and point out that its counts come from seeded database records.
2. Sign in as `student@campusconnect.test`. Open the profile, edit skills and academics, and upload a small genuine PDF resume. Download it to demonstrate access-controlled storage.
3. Open Opportunities. Use company/location/department filters and Eligible only. Open Associate Java Developer and submit an application. A second application is blocked; the original remains in Applications.
4. Sign out. Sign in as `recruiter@campusconnect.test`. Open Applications and filter for the new role. Open the applicant and move Applied → Under Review → Shortlisted.
5. Schedule a future technical interview. Return to the application, mark Selected, and create an offer with a future deadline and annual INR salary.
6. Sign in as the student. Show the interview and notifications. Open Offers and accept. Explain that decline is the alternative terminal response and only the student's own pending unexpired offers permit a response.
7. Sign in as administrator. Show Students, search/filter, profile edit, and the account-disable action. Show Companies → Pending and approve Orbit Analytics.
8. Show placement drives and academic-year settings. Create an announcement. Open Reports and filter by batch/company/year, then export CSV.
9. Open Audit history. Show the status change records on the application. Explain unique constraints and optimistic versions.
10. Sign in as coordinator. Show drive, interview, announcement and report access. Explain why student administration, company approvals and audit history are administrator-only.

## Negative demonstrations

- Lower a student's CGPA or choose an ineligible batch and inspect the eligibility explanation.
- Try to navigate to `/admin/students` as a student: access is denied.
- As one recruiter, request another recruiter's job detail: access is denied.
- Upload an executable renamed `.pdf`: content validation rejects it.
- Attempt to jump from Applied directly to Offered: the generic workflow service rejects it; normal UI never offers that transition.
- Try responding twice to an offer: only the first valid response is persisted.

Use fictional records. Demo actions modify the database; history and reports will reflect those actions. To repeat from the original seed, use a separate disposable database rather than erasing records you need.
