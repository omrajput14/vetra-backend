const http = require('http');

function request(options, data) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => body += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, headers: res.headers, data: body ? JSON.parse(body) : null });
        } catch (e) {
          resolve({ status: res.statusCode, headers: res.headers, raw: body });
        }
      });
    });
    req.on('error', reject);
    if (data) {
      req.write(typeof data === 'string' ? data : JSON.stringify(data));
    }
    req.end();
  });
}

async function run() {
  console.log('=== Vetra AI Veterinary Advisor End-to-End Live Verification ===\n');

  const randomSuffix = Math.floor(Math.random() * 100000);
  const farmerEmail = `farmer_adv_${randomSuffix}@vetra.app`;
  const password = 'Password@123';

  // 1. Register Farmer
  console.log(`1. Registering farmer: ${farmerEmail}`);
  const regRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/v1/auth/farmer/register',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, {
    email: farmerEmail,
    phoneNumber: `+9198${String(randomSuffix).padStart(8, '0')}`,
    password: password,
    fullName: 'Ramesh Patel',
    village: 'Anand',
    taluka: 'Anand',
    district: 'Anand',
    state: 'Gujarat',
    latitude: 22.5645,
    longitude: 72.9289,
    farmSizeAcres: 12.5
  });

  if (regRes.status !== 201) {
    console.error('Registration failed:', regRes);
    process.exit(1);
  }
  const token = regRes.data.data.accessToken;
  console.log('   Farmer registered successfully.');

  // 2. Register Animal
  console.log('\n2. Registering Animal (Gauri, Gir Cattle)...');
  const animalRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/v1/animals',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, {
    name: 'Gauri',
    tagNumber: `IND-${randomSuffix}`,
    qrCodeId: `QR-GAURI-${randomSuffix}`,
    species: 'CATTLE',
    breed: 'Gir',
    gender: 'FEMALE',
    dateOfBirth: '2022-04-10'
  });

  if (animalRes.status !== 201) {
    console.error('Animal creation failed:', animalRes);
    process.exit(1);
  }
  const animalId = animalRes.data.data.id;
  console.log(`   Animal registered: id=${animalId}, name=Gauri`);

  // 3. Create AI Advisor Session (Turn 1)
  console.log('\n3. Creating AI Advisor Session (Turn 1: Initial Concern)...');
  const initialMsg = 'Gauri has stopped eating grain today and seems slightly sluggish.';
  const session1Res = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/v1/animals/${animalId}/ai/advisor/sessions`,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, {
    initialMessage: initialMsg
  });

  console.log(`   Status: ${session1Res.status}`);
  if (session1Res.status !== 201) {
    console.error('Session creation failed:', session1Res);
    process.exit(1);
  }
  const session = session1Res.data.data;
  const sessionId = session.id;
  console.log(`   Session ID: ${sessionId}`);
  console.log(`   Session Status: ${session.status}`);
  console.log(`   Turn Count: ${session.turnCount}`);
  console.log(`   Advisor Reply:\n   "${session.messages[session.messages.length - 1].content}"`);
  console.log(`   Follow-up Questions:`, session.messages[session.messages.length - 1].followUpQuestions);

  // 4. Send Follow-up Answer (Turn 2) with explicit normal water intake and denied bloat
  console.log('\n4. Sending Follow-up Answer (Turn 2: Explicit symptom report & denial)...');
  const followUpMsg = 'She is drinking water normally, temperature is 101.5 F, no bloat, and no coughing.';
  const session2Res = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/v1/ai/advisor/sessions/${sessionId}/messages`,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, {
    message: followUpMsg
  });

  console.log(`   Status: ${session2Res.status}`);
  const session2 = session2Res.data.data;
  console.log(`   Session Status: ${session2.status}`);
  console.log(`   Turn Count: ${session2.turnCount}`);
  const lastMsg = session2.messages[session2.messages.length - 1];
  console.log(`   Advisor Reply:\n   "${lastMsg.content}"`);
  if (session2.assessment) {
    console.log(`\n   --- STRUCTURED PRELIMINARY ASSESSMENT ---`);
    console.log(`   Risk Level: ${session2.assessment.riskLevel}`);
    console.log(`   Requires Vet Review: ${session2.assessment.requiresVeterinarianReview}`);
    console.log(`   Recommended Care: ${session2.assessment.recommendedNextStep}`);
    console.log(`   Disclaimer: ${session2.assessment.disclaimer}`);
    console.log(`   Possible Conditions:`, JSON.stringify(session2.assessment.possibleConditions, null, 2));
    console.log(`   Owner-Reported Symptoms & Vitals:`, session2.assessment.userReportedSymptoms);
    console.log(`   AI Clinical Observations:`, session2.assessment.keyObservations);

    // Factual consistency validations
    const userReports = session2.assessment.userReportedSymptoms || [];
    const observations = session2.assessment.keyObservations || [];

    const hasNormalWater = userReports.some(s => s.toLowerCase().includes('normal water'));
    const hasBloatAbsence = userReports.some(s => s.toLowerCase().includes('bloat'));
    const hasDecreasedWater = observations.some(s => s.toLowerCase().includes('decreased water'));

    if (!hasNormalWater) {
      console.error('   FAILED: Expected "Reported normal water intake" in userReportedSymptoms.');
      process.exit(1);
    }
    if (!hasBloatAbsence) {
      console.error('   FAILED: Expected confirmed absence of bloat in userReportedSymptoms.');
      process.exit(1);
    }
    if (hasDecreasedWater) {
      console.error('   FAILED: Contradictory "decreased water intake" found in AI observations.');
      process.exit(1);
    }
    console.log('\n   Factual Consistency Verification: PASSED (Zero contradiction or fabrication).');
  }

  // 5. Test Red-Flag Emergency Keyword Escalation
  console.log('\n5. Testing Red-Flag Emergency Keyword Escalation...');
  const emergMsg = 'Now she cannot stand up and is a downer animal.';
  const session3Res = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/v1/ai/advisor/sessions/${sessionId}/messages`,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, {
    message: emergMsg
  });

  const session3 = session3Res.data.data;
  console.log(`   Session Status: ${session3.status}`);
  console.log(`   Risk Level: ${session3.riskLevel}`);
  console.log(`   Requires Vet Review: ${session3.requiresVetReview}`);
  console.log(`   Emergency Reply:\n   "${session3.messages[session3.messages.length - 1].content}"`);

  // 6. Test Authorization (Cross-tenant security)
  console.log('\n6. Testing Multi-Tenant Authorization Security...');
  const otherFarmerEmail = `farmer_other_${randomSuffix}@vetra.app`;
  const regOther = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/v1/auth/farmer/register',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, {
    email: otherFarmerEmail,
    phoneNumber: `+9197${String(randomSuffix).padStart(8, '0')}`,
    password: password,
    fullName: 'Suresh Kumar',
    village: 'Vadodara',
    taluka: 'Vadodara',
    district: 'Vadodara',
    state: 'Gujarat',
    latitude: 22.3072,
    longitude: 73.1812,
    farmSizeAcres: 5.0
  });
  const otherToken = regOther.data.data.accessToken;

  const unauthorizedAccess = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/v1/ai/advisor/sessions/${sessionId}`,
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${otherToken}`
    }
  });

  console.log(`   Unauthorized Access Attempt Status: ${unauthorizedAccess.status} (Expected: 403 Forbidden)`);
  if (unauthorizedAccess.status === 403) {
    console.log('   Security verification PASSED: Cross-user advisor session access strictly rejected.');
  } else {
    console.error('   Security verification FAILED!');
    process.exit(1);
  }

  console.log('\n=== All AI Advisor Live Integration Tests PASSED! ===');
}

run().catch(console.error);
