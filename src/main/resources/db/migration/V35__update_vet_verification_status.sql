-- V35: Update only the demo veterinarian to VERIFIED status for clinical validation workflows
UPDATE vet_profiles 
SET verification_status = 'VERIFIED' 
WHERE user_id = (
    SELECT id FROM users WHERE email = 'demo.vet.pune@vetra.co.in'
);

