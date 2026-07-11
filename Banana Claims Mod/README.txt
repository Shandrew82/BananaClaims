Banana Claims - Claim Lifecycle and Ownership Polish

Copy the src directory into the project root and replace matching files.
Two new domain files are included:
- ClaimRole.java
- ClaimMutationResult.java

This update centralizes member, subowner, leave, and ownership-transfer
mutations in ClaimManager so authorization, role invariants, persistence,
and claim change events happen together.

Build:
  .\gradlew.bat clean build
