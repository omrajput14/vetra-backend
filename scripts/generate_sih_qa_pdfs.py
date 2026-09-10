import os
import sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.pdfgen import canvas

OUTPUT_DIR = "/Users/0mrajput/Desktop/SIH/vetra-backend/sih-prep"
os.makedirs(OUTPUT_DIR, exist_ok=True)

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#475569"))
        
        # Header (only on pages > 1)
        if self._pageNumber > 1:
            self.drawString(54, 752, "PASHU SATHI")
            self.setFont("Helvetica", 8)
            self.setFillColor(colors.HexColor("#64748B"))
            self.drawString(122, 752, "|  SIH 2026 Internal Defense — Problem Statement SIH26128")
            self.drawRightString(558, 752, "Team MOSAIC")
            self.setStrokeColor(colors.HexColor("#E2E8F0"))
            self.setLineWidth(0.75)
            self.line(54, 744, 558, 744)

        # Footer
        footer_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(558, 30, footer_text)
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#1E7E34"))
        self.drawString(54, 30, "CONFIDENTIAL & PROPRIETARY")
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748B"))
        self.drawString(195, 30, "— Smart India Hackathon 2026 Internal Evaluation Sheet")
        self.setStrokeColor(colors.HexColor("#E2E8F0"))
        self.setLineWidth(0.75)
        self.line(54, 40, 558, 40)
        self.restoreState()


def get_custom_styles():
    styles = getSampleStyleSheet()
    
    primary = colors.HexColor("#0E1A2B")
    green = colors.HexColor("#1E7E34")
    orange = colors.HexColor("#C2410C")
    dark = colors.HexColor("#1E293B")
    
    styles.add(ParagraphStyle(
        name='DocHeaderTitle',
        fontName='Helvetica-Bold',
        fontSize=18,
        leading=22,
        textColor=primary,
        spaceAfter=2
    ))
    styles.add(ParagraphStyle(
        name='DocHeaderSubtitle',
        fontName='Helvetica-Bold',
        fontSize=9.5,
        leading=13,
        textColor=green,
        spaceAfter=10
    ))
    styles.add(ParagraphStyle(
        name='MetaBannerText',
        fontName='Helvetica',
        fontSize=8,
        leading=11,
        textColor=colors.HexColor("#334155")
    ))
    styles.add(ParagraphStyle(
        name='SectionHeader',
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=15,
        textColor=primary,
        spaceBefore=8,
        spaceAfter=5
    ))
    styles.add(ParagraphStyle(
        name='QuestionTitle',
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=12.5,
        textColor=colors.HexColor("#0F172A")
    ))
    styles.add(ParagraphStyle(
        name='RoleBadge',
        fontName='Helvetica-Bold',
        fontSize=7.5,
        leading=9.5,
        textColor=orange
    ))
    styles.add(ParagraphStyle(
        name='KeyPoint',
        fontName='Helvetica',
        fontSize=8,
        leading=11.5,
        textColor=dark
    ))
    return styles


def create_qa_card(q_num, role, question, answer_points, border_color="#1E7E34", bg_color="#F8FAFC"):
    styles = get_custom_styles()
    
    content = []
    header_html = f"<b>Q{q_num}: {question}</b>"
    content.append(Paragraph(header_html, styles['QuestionTitle']))
    content.append(Spacer(1, 1.5))
    
    if role:
        role_html = f"<b>Role:</b> {role}"
        content.append(Paragraph(role_html, styles['RoleBadge']))
        content.append(Spacer(1, 3))
    else:
        content.append(Spacer(1, 2))
    
    for point in answer_points:
        p_html = f"• {point}"
        content.append(Paragraph(p_html, styles['KeyPoint']))
        content.append(Spacer(1, 1.5))
        
    t = Table([[content]], colWidths=[504])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor(bg_color)),
        ('LEFTPADDING', (0, 0), (-1, -1), 9),
        ('RIGHTPADDING', (0, 0), (-1, -1), 9),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
        ('LINELEFT', (0, 0), (-1, -1), 3.5, colors.HexColor(border_color)),
        ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
    ]))
    return KeepTogether([t, Spacer(1, 6)])


# ==============================================================================
# PDF 1: GENERAL & SOLUTION Q&A (No "Team Lead", Distributed Team Roles)
# ==============================================================================
def build_general_qa_pdf():
    pdf_path = os.path.join(OUTPUT_DIR, "PASHU_SATHI_General_Solution_QA.pdf")
    doc = SimpleDocTemplate(
        pdf_path,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=46,
        bottomMargin=46
    )
    styles = get_custom_styles()
    story = []

    # Title & Metadata
    story.append(Paragraph("PASHU SATHI — General &amp; Solution Q&amp;A", styles['DocHeaderTitle']))
    story.append(Paragraph("Smart India Hackathon 2026 | Internal Evaluation Viva Guide (General Domain)", styles['DocHeaderSubtitle']))
    
    # Meta Box
    meta_data = [
        [Paragraph("<b>Problem Statement:</b> SIH26128 — Early Detection &amp; Prevention of Livestock Diseases", styles['MetaBannerText']),
         Paragraph("<b>Team:</b> MOSAIC (6 Members)", styles['MetaBannerText'])],
        [Paragraph("<b>Theme:</b> MedTech / BioTech / HealthTech", styles['MetaBannerText']),
         Paragraph("<b>Core Focus:</b> Problem Overview, Solution Loop, Farmer UX &amp; Impact", styles['MetaBannerText'])],
    ]
    meta_table = Table(meta_data, colWidths=[310, 194])
    meta_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#F1F5F9")),
        ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor("#CBD5E1")),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('LEFTPADDING', (0, 0), (-1, -1), 8),
        ('RIGHTPADDING', (0, 0), (-1, -1), 8),
    ]))
    story.append(meta_table)
    story.append(Spacer(1, 8))

    # SECTION 1: Problem & Adoption (Page 1)
    story.append(Paragraph("1. Core Problem &amp; Rural Field Constraints", styles['SectionHeader']))
    story.append(create_qa_card(
        1, "Presenter / Pitcher",
        "What exact gap in the current rural veterinary system does Pashu Sathi solve?",
        [
            "<b>Physical Distance Barrier:</b> The nearest government veterinary dispensary is 15+ km away from smallholder farmers, creating reporting delays of 3 to 7 days.",
            "<b>Silent Epidemiological Spread:</b> Highly contagious infections like FMD and Lumpy Skin Disease spread between herds before paper reports reach district headquarters.",
            "<b>Catastrophic Household Loss:</b> A single cattle death wipes out up to 50% of a marginal farmer's yearly household income (approx. Rs. 75,000 net loss)."
        ]
    ))
    story.append(create_qa_card(
        2, "Mobile App Dev / UI Specialist",
        "How will an illiterate rural farmer with no smartphone expertise use your app?",
        [
            "<b>Zero-Typing UI:</b> The interface eliminates text boxes; reporting is conducted through visual anatomical tap-icons (mouth, skin, udder, legs).",
            "<b>Multilingual Two-Way Voice AI:</b> Farmers simply press a prominent mic button and speak naturally in Marathi, Hindi, or English.",
            "<b>Passive Geo-Telemetry:</b> Exact GPS coordinates, date, and device timestamps are captured automatically in the background without user intervention."
        ]
    ))
    story.append(create_qa_card(
        3, "Product &amp; Systems Analyst",
        "Why won't farmers simply dial the existing 1962 veterinary toll-free helpline instead?",
        [
            "<b>Bandwidth Limits:</b> 1962 call centers suffer from busy lines, limited call operators, and verbal descriptions without objective visual imagery.",
            "<b>No Spatial Intelligence:</b> Phone calls do not generate structured spatial-temporal outbreak coordinates on an interactive GIS map for state epidemiologists.",
            "<b>Complementary Backend:</b> Pashu Sathi can directly serve as the digital intake and ticketing software for 1962 call-center dispatchers."
        ]
    ))

    # PAGE BREAK -> Clean start for Page 2
    story.append(PageBreak())

    # SECTION 2: Solution Loop & Government Integration (Page 2)
    story.append(Paragraph("2. Solution Workflow &amp; Government Scheme Integration", styles['SectionHeader']))
    story.append(create_qa_card(
        4, "Healthcare Domain Specialist",
        "Explain your 5-step rapid response loop in simple, non-technical terms.",
        [
            "<b>Step 1 (Farmer Reports):</b> Farmer submits voice note or photo in under 30 seconds (works fully offline).",
            "<b>Step 2 (Preliminary Triage):</b> System flags suspected symptoms and alerts the assigned local block veterinarian.",
            "<b>Step 3 (Clinical Verification):</b> Licensed field veterinarian visits, scans animal QR passport, and verifies diagnosis.",
            "<b>Step 4 (Automated Spatial Clustering):</b> If 3 confirmed cases cluster within 25 km inside 48 hours, an outbreak cluster triggers.",
            "<b>Step 5 (Containment Deployment):</b> District animal husbandry officers receive GIS alerts and deploy targeted ring-vaccination."
        ]
    ))
    story.append(create_qa_card(
        5, "Regulatory &amp; Policy Analyst",
        "How does Pashu Sathi align with government platforms like Bharat Pashudhan &amp; INAPH?",
        [
            "<b>Intake Layer vs. Static Registry:</b> Bharat Pashudhan and INAPH are static census and animal tracking registries. Pashu Sathi is the real-time active surveillance engine.",
            "<b>Ear-Tag UID Compatibility:</b> Every animal record in Pashu Sathi directly links to the national 12-digit Pashu Aadhaar ear-tag standard.",
            "<b>Scheme Acceleration:</b> Directly accelerates DAHD's National Animal Disease Control Programme (NADCP) and LHDCP targets to eradicate FMD and Brucellosis by 2030."
        ]
    ))

    # SECTION 3: Economic Impact & Rollout (Page 2)
    story.append(Paragraph("3. Economic Impact &amp; 30-Second Elevator Defense", styles['SectionHeader']))
    story.append(create_qa_card(
        6, "Impact &amp; Economic Analyst",
        "What is the tangible ROI or financial justification for state animal husbandry departments?",
        [
            "<b>Rs. 20,000 Crore Loss Mitigation:</b> FMD alone causes over Rs. 20,000 Crore in direct losses annually; early containment saves milk yield and draught power.",
            "<b>Targeted Ring Vaccination:</b> Instead of expensive, delayed statewide blanket vaccination, officers direct cold-chain logistics strictly to the 25–50 km buffer zone.",
            "<b>Public Health Safety:</b> Fast isolation of Rabies, Anthrax, and Brucellosis mitigates dangerous zoonotic transmission to rural human populations."
        ]
    ))

    # Elevator Pitch Box
    pitch_content = [
        Paragraph("<b>30-Second Closing Elevator Pitch for Judges:</b>", styles['QuestionTitle']),
        Spacer(1, 2),
        Paragraph(
            "<i>\"Pashu Sathi transforms rural livestock healthcare from slow, reactive paper reporting into instant, digital syndromic surveillance. "
            "By empowering low-literacy farmers with offline voice reporting, enforcing licensed veterinary verification, and providing state officers with "
            "real-time GIS containment maps, we stop epidemics at 3 cases instead of hundreds—protecting smallholder livelihoods and the nation's agricultural economy.\"</i>",
            styles['KeyPoint']
        )
    ]
    pitch_table = Table([[pitch_content]], colWidths=[504])
    pitch_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#EDF7F0")),
        ('LEFTPADDING', (0, 0), (-1, -1), 9),
        ('RIGHTPADDING', (0, 0), (-1, -1), 9),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
        ('LINELEFT', (0, 0), (-1, -1), 3.5, colors.HexColor("#1E7E34")),
        ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor("#BFE4C9")),
    ]))
    story.append(pitch_table)

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Generated: {pdf_path}")


# ==============================================================================
# PDF 2: TECHNICAL & ARCHITECTURE DEEP-DIVE Q&A (No "Team Lead", Engineering Roles)
# ==============================================================================
def build_technical_qa_pdf():
    pdf_path = os.path.join(OUTPUT_DIR, "PASHU_SATHI_Technical_Architecture_QA.pdf")
    doc = SimpleDocTemplate(
        pdf_path,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=46,
        bottomMargin=46
    )
    styles = get_custom_styles()
    story = []

    # Title & Metadata
    story.append(Paragraph("PASHU SATHI — Technical &amp; Architecture Q&amp;A", styles['DocHeaderTitle']))
    story.append(Paragraph("Smart India Hackathon 2026 | Internal Evaluation Viva Guide (Technical Domain)", styles['DocHeaderSubtitle']))
    
    # Meta Box
    meta_data = [
        [Paragraph("<b>Problem Statement:</b> SIH26128 — Early Detection &amp; Prevention of Livestock Diseases", styles['MetaBannerText']),
         Paragraph("<b>Core Stack:</b> Spring Boot 3, Java 21, PostGIS 3.5, Flutter", styles['MetaBannerText'])],
        [Paragraph("<b>Architecture:</b> 3-Tier Micro-Modular with PostGIS Clustering &amp; Redis 7", styles['MetaBannerText']),
         Paragraph("<b>Validation:</b> 68/68 Automated Tests Passing (100% Green)", styles['MetaBannerText'])],
    ]
    meta_table = Table(meta_data, colWidths=[310, 194])
    meta_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#F1F5F9")),
        ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor("#CBD5E1")),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('LEFTPADDING', (0, 0), (-1, -1), 8),
        ('RIGHTPADDING', (0, 0), (-1, -1), 8),
    ]))
    story.append(meta_table)
    story.append(Spacer(1, 8))

    # SECTION 1: Architecture & Concurrency (Page 1)
    story.append(Paragraph("1. System Architecture &amp; High-Throughput Engineering", styles['SectionHeader']))
    story.append(create_qa_card(
        1, "Backend / Cloud Engineer",
        "Walk us through your 3-Tier system topology and data flow.",
        [
            "<b>Client Tier:</b> Flutter cross-platform mobile app (dual-mode Farmer/Vet) + React 18 TypeScript GIS Government Command Dashboard.",
            "<b>Application Tier:</b> Spring Boot 3 on Java 21 with Project Loom Virtual Threads for high-concurrency non-blocking I/O.",
            "<b>Data &amp; Cache Tier:</b> PostgreSQL 17 + PostGIS 3.5 spatial extension for spatial indexing; Redis 7 for caching token sessions and weather metadata.",
            "<b>Cloud Infrastructure:</b> Hosted on an Azure Linux VM with Nginx reverse proxy, SSL termination, and Docker containerization."
        ],
        border_color="#0E1A2B"
    ))
    story.append(create_qa_card(
        2, "Backend / Database Engineer",
        "How do you prevent database lock contention when thousands of field reports arrive at once?",
        [
            "<b>Asynchronous Event Bus:</b> Disease intake endpoints return an immediate <b>201 Created</b> receipt; outbreak clustering logic executes asynchronously via Spring <b>@Async</b> threads.",
            "<b>Optimistic Locking (@Version):</b> Implemented on entity lifecycles to prevent stale state write-over without expensive pessimistic database row locks.",
            "<b>Spatial Query Offloading:</b> Heavy geospatial calculations are cached in Redis with a 30-minute TTL to keep PostGIS read load minimal."
        ],
        border_color="#0E1A2B"
    ))

    # SECTION 2: AI Pipeline & Clinical Governance (Page 1)
    story.append(Paragraph("2. AI Diagnostic Pipeline &amp; Clinical Governance", styles['SectionHeader']))
    story.append(create_qa_card(
        3, "AI / ML Engineer",
        "How does your Gemini AI vision pipeline work, and how do you prevent hallucinations?",
        [
            "<b>Multimodal Visual Assessment:</b> Lesion photos are processed via Google Gemini Multimodal Vision API with constrained JSON schema output.",
            "<b>Strict Diagnostic Guardrails:</b> Prompts instruct the model to return differential probabilities, clinical confidence (0.00–1.00), and triage urgency.",
            "<b>Pre-Ingestion Quality Filter:</b> Image classifiers reject blurry images, non-livestock subjects, and corrupt payloads before invoking the model.",
            "<b>Strict Status Isolation:</b> AI output is permanently tagged <b>SUSPECTED</b> in the database; it is strictly advisory and cannot trigger legal quarantine on its own."
        ],
        border_color="#0E1A2B"
    ))
    story.append(create_qa_card(
        4, "AI Governance / Clinical Validation Dev",
        "Explain the Human-in-the-Loop 'Vet Safety Gate'. Why is it legally necessary?",
        [
            "<b>Statutory Compliance:</b> Under Indian Veterinary Council regulations, only a registered Veterinary Practitioner (VRC) can legally confirm an infectious disease.",
            "<b>Two-Phase State Machine:</b> Farmer/AI reports enter as <b>PENDING_REVIEW</b>. A licensed vet must verify physical clinical symptoms or lab assays.",
            "<b>False Alarm Blocker:</b> Outbreak detection algorithms only ingest verified veterinary reports (<b>VETERINARIAN</b> or <b>LAB_CONFIRMED</b>), completely eliminating false panics."
        ],
        border_color="#0E1A2B"
    ))

    # PAGE BREAK -> Clean start for Page 2
    story.append(PageBreak())

    # SECTION 3: Spatial Engine & Risk Algorithm (Page 2)
    story.append(Paragraph("3. Geospatial Outbreak Engine &amp; Multi-Signal Risk Scoring", styles['SectionHeader']))
    story.append(create_qa_card(
        5, "Data / GIS Engineer",
        "How does your PostGIS spatial-temporal clustering algorithm detect outbreaks?",
        [
            "<b>Spatial Distance Indexing:</b> Uses PostGIS <b>ST_DWithin(geography, geography, 25000)</b> backed by GiST R-Tree indexing for sub-millisecond proximity queries.",
            "<b>Temporal Sliding Window:</b> Evaluates contributing reports within a rolling 48-to-72-hour window (<b>createdAt &gt;= NOW() - INTERVAL '48 HOURS'</b>).",
            "<b>Dynamic Centroid Calculation:</b> Outbreak centers are computed using <b>ST_Centroid(ST_Collect(location))</b> to update geographical cluster centers dynamically.",
            "<b>Zoonotic One-Case Trigger:</b> For lethal zoonoses (Rabies, Anthrax), the threshold drops to <b>1 confirmed case within 24 hours</b>, immediately creating a 50 km containment buffer."
        ],
        border_color="#0E1A2B"
    ))
    story.append(create_qa_card(
        6, "Spatial Algorithms Engineer",
        "Deconstruct the 4-Signal Composite Threat Score (0–100) used on your GIS map.",
        [
            "<b>Signal 1 — Sickness Severity (40%):</b> Ratio of confirmed vs. suspected cases, weighted by disease mortality and transmission R0.",
            "<b>Signal 2 — Live Weather Anomaly (20%):</b> Real-time temperature, relative humidity, and rainfall fetched from Open-Meteo API (favorable vector breeding ranges add score).",
            "<b>Signal 3 — Historical Baseline CV (20%):</b> Coefficient of variation comparing current incidence against 3-year seasonal historical baseline for that specific taluka.",
            "<b>Signal 4 — Vaccine Immunity Deficit (20%):</b> Estimated percentage of unimmunized susceptible livestock within the spatial perimeter."
        ],
        border_color="#0E1A2B"
    ))

    # SECTION 4: Offline Sync & Security (Page 2)
    story.append(Paragraph("4. Offline-First Sync &amp; Anti-Fraud Security", styles['SectionHeader']))
    story.append(create_qa_card(
        7, "Mobile / Flutter Dev",
        "How does the mobile app maintain full offline resilience in remote zero-network zones?",
        [
            "<b>Local Persistent SQLite / Hive Storage:</b> Reports, audio recordings, and compressed image blobs are stored in an encrypted local database when offline.",
            "<b>Idempotent Background Worker:</b> Flutter background work-manager monitors network state. On network reconnection, queued reports sync automatically.",
            "<b>UUID-Based Deduplication:</b> Client generates deterministic v4 UUIDs for reports; backend enforces unique constraints so repeated retries never create duplicate tickets."
        ],
        border_color="#0E1A2B"
    ))
    story.append(create_qa_card(
        8, "Full-Stack / Security Engineer",
        "How do you prevent malicious users from spoofing disease reports to crash livestock prices?",
        [
            "<b>JWT Role-Based Access Control (RBAC):</b> Strict role isolation between <b>FARMER</b>, <b>VETERINARIAN</b>, and <b>GOVERNMENT_OFFICER</b> enforced via Spring Security 6 filter chains.",
            "<b>Tiered Confidence Scoring:</b> Unverified farmer reports carry a confidence weight of 0.20; they cannot mathematically trigger an outbreak cluster without vet confirmation.",
            "<b>Geo-Spoofing Mitigations:</b> Mobile client validates cell tower triangulation against device GPS; rapid teleportation triggers automated fraud review flags."
        ],
        border_color="#0E1A2B"
    ))

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Generated: {pdf_path}")


if __name__ == "__main__":
    build_general_qa_pdf()
    build_technical_qa_pdf()
