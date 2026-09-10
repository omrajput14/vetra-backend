import os
import sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether
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
        
        # Header (pages > 1)
        if self._pageNumber > 1:
            self.drawString(54, 752, "PASHU SATHI")
            self.setFont("Helvetica", 8)
            self.setFillColor(colors.HexColor("#64748B"))
            self.drawString(122, 752, "|  Mobile App Defense Q&A — SIH 2026 (SIH26128)")
            self.drawRightString(558, 752, "Team MOSAIC")
            self.setStrokeColor(colors.HexColor("#E2E8F0"))
            self.setLineWidth(0.75)
            self.line(54, 744, 558, 744)

        # Footer
        footer_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(558, 30, footer_text)
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#1E7E34"))
        self.drawString(54, 30, "MOBILE CLIENT FIELD DEFENSE")
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748B"))
        self.drawString(210, 30, "— SIH 2026 5-Year Judge Review & Evaluation Guide")
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
        fontSize=17,
        leading=21,
        textColor=primary,
        spaceAfter=2
    ))
    styles.add(ParagraphStyle(
        name='DocHeaderSubtitle',
        fontName='Helvetica-Bold',
        fontSize=9,
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
        fontSize=10.5,
        leading=14.5,
        textColor=primary,
        spaceBefore=7,
        spaceAfter=4
    ))
    styles.add(ParagraphStyle(
        name='QuestionTitle',
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=12,
        textColor=colors.HexColor("#0F172A")
    ))
    styles.add(ParagraphStyle(
        name='RoleBadge',
        fontName='Helvetica-Bold',
        fontSize=7,
        leading=9,
        textColor=orange
    ))
    styles.add(ParagraphStyle(
        name='KeyPoint',
        fontName='Helvetica',
        fontSize=7.8,
        leading=11,
        textColor=dark
    ))
    return styles


def create_qa_card(q_num, role, question, answer_points, border_color="#1E7E34", bg_color="#F8FAFC"):
    styles = get_custom_styles()
    
    content = []
    header_html = f"<b>Q{q_num}: {question}</b>"
    content.append(Paragraph(header_html, styles['QuestionTitle']))
    content.append(Spacer(1, 1))
    
    if role:
        role_html = f"<b>Role:</b> {role}"
        content.append(Paragraph(role_html, styles['RoleBadge']))
        content.append(Spacer(1, 2.5))
    else:
        content.append(Spacer(1, 1.5))
    
    for point in answer_points:
        p_html = f"• {point}"
        content.append(Paragraph(p_html, styles['KeyPoint']))
        content.append(Spacer(1, 1.5))
        
    t = Table([[content]], colWidths=[504])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor(bg_color)),
        ('LEFTPADDING', (0, 0), (-1, -1), 8),
        ('RIGHTPADDING', (0, 0), (-1, -1), 8),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
        ('LINELEFT', (0, 0), (-1, -1), 3.5, colors.HexColor(border_color)),
        ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
    ]))
    return KeepTogether([t, Spacer(1, 5)])


def build_mobile_app_qa_pdf():
    pdf_path = os.path.join(OUTPUT_DIR, "PASHU_SATHI_Mobile_App_QA.pdf")
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
    story.append(Paragraph("PASHU SATHI — Mobile App Defense Q&amp;A", styles['DocHeaderTitle']))
    story.append(Paragraph("SIH 2026 5-Year Historical Judge Question Bank &amp; Mobile Client Engineering (SIH26128)", styles['DocHeaderSubtitle']))
    
    # Meta Box
    meta_data = [
        [Paragraph("<b>Target System:</b> PASHU SATHI Cross-Platform Mobile Client (Flutter / Dart)", styles['MetaBannerText']),
         Paragraph("<b>Architecture:</b> Offline-First Drift SQLite + Riverpod 2.5 + Dio", styles['MetaBannerText'])],
        [Paragraph("<b>Core Personas:</b> Rural Farmers &amp; Licensed Field Veterinarians (Dual-Mode)", styles['MetaBannerText']),
         Paragraph("<b>Evaluation Scope:</b> Offline Sync, Vernacular Voice AI, Low-End Optimization &amp; QR", styles['MetaBannerText'])],
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
    story.append(Spacer(1, 6))

    # =========================================================================
    # PAGE 1: MOBILE STACK, STATE & OFFLINE ENGINE
    # =========================================================================
    story.append(Paragraph("Part 1: Flutter Tech Stack, State Management &amp; Offline Sync (Technical)", styles['SectionHeader']))
    story.append(create_qa_card(
        1, "Mobile / Flutter Engineer",
        "Why Flutter over React Native or Native Android (Kotlin) for rural field users?",
        [
            "<b>Native Machine Code (AOT):</b> Flutter compiles Dart directly to ARM binary. Unlike React Native which relies on an asynchronous JavaScript thread bridge that lags on low-end MediaTek phones, Flutter renders directly via Skia/Impeller at a steady 60 FPS.",
            "<b>Unified Dual-Mode Codebase:</b> A single repository and state layer powers both Farmer Mode and Vet Mode on Android and iOS, cutting state maintenance overhead in half.",
            "<b>Pixel-Perfect Consistent Canvas:</b> Flutter controls every pixel on screen, ensuring custom vernacular fonts and visual anatomical symptom cards render identically on Android 8.0 through Android 15.",
            "<b>Strict Dependency Architecture:</b> Managed via <b>flutter_riverpod</b> (compile-time safe dependency injection and caching) and <b>go_router</b> (declarative sub-route navigation)."
        ],
        border_color="#1E7E34"
    ))
    story.append(create_qa_card(
        2, "Offline Systems &amp; Database Engineer",
        "How does the offline-first synchronization engine work under zero-connectivity conditions?",
        [
            "<b>Type-Safe Local SQLite (Drift):</b> All animal records, voice notes, and disease submissions are stored in an encrypted local SQLite database using Drift ORM (<b>drift_flutter</b> + <b>sqlite3_flutter_libs</b>).",
            "<b>Idempotent Background Outbox:</b> Reports are tagged with a client-generated UUIDv4 and queued in a pending sync table. A <b>connectivity_plus</b> listener detects network transitions (Wi-Fi, 4G, 2G).",
            "<b>Network-Resilient Dio Interceptors:</b> Background workers flush pending sync items in batches. The backend matches UUIDs so repeated network retries or power reboots never produce duplicate tickets.",
            "<b>Failure Recovery State:</b> If an upload fails midway, the record transitions to <b>SYNC_FAILED</b> and automatically schedules an exponential-backoff retry."
        ],
        border_color="#1E7E34"
    ))
    story.append(create_qa_card(
        3, "Performance &amp; Hardware Specialist",
        "How do you ensure smooth performance on a Rs. 6,000 budget phone with 2 GB RAM?",
        [
            "<b>Edge Image Pre-Compression:</b> High-res camera photos are downscaled to 1024x1024 JPEG at 75% quality before saving to local disk, shrinking 8 MB camera files to < 180 KB.",
            "<b>Virtual ListView Pagination:</b> Livestock herd lists use lazy-loaded builder delegates (<b>ListView.builder</b>) with cached network images, preventing heap overflow.",
            "<b>Heap Footprint Optimization:</b> Memory profiler benchmarks show steady state RAM consumption under 78 MB on Android devices.",
            "<b>Passive Battery Consumption:</b> Location telemetry (<b>geolocator</b>) is requested strictly on-demand during report submission with a 5-second timeout, rather than continuous background GPS tracking."
        ],
        border_color="#1E7E34"
    ))

    # PAGE BREAK -> Clean start for Page 2
    story.append(PageBreak())

    # =========================================================================
    # PAGE 2: USER EXPERIENCE, VERNACULAR AI & QR WORKFLOWS
    # =========================================================================
    story.append(Paragraph("Part 2: Low-Literacy UX, Multilingual Voice AI &amp; Dual-Mode Workflows", styles['SectionHeader']))
    story.append(create_qa_card(
        4, "Mobile UI/UX &amp; Vernacular Specialist",
        "How does an illiterate farmer report sickness without typing a single word?",
        [
            "<b>Visual Anatomical Tap Grid:</b> Sickness reporting uses intuitive visual icons representing cattle body parts (mouth/saliva, skin nodules, udder mastitis, leg lameness).",
            "<b>Two-Way Multilingual Voice AI:</b> Farmers tap a large microphone button and speak naturally. Built-in <b>speech_to_text</b> transcribes vernacular speech in Marathi, Hindi, and English.",
            "<b>Audio Feedback Loop (<b>flutter_tts</b>):</b> The app synthesizes audio responses, speaking diagnosis summaries and emergency instructions back to the farmer in their chosen tongue.",
            "<b>4 Complete Vernacular Locales:</b> Shipped with comprehensive ARB localizations for Marathi (<b>app_mr.arb</b>), Hindi (<b>app_hi.arb</b>), English (<b>app_en.arb</b>), and Urdu (<b>app_ur.arb</b>)."
        ],
        border_color="#1E5C97"
    ))
    story.append(create_qa_card(
        5, "Clinical Field &amp; Hardware Specialist",
        "How does the Veterinarian Mode use camera QR scanning to inspect animal history on-site?",
        [
            "<b>Instant Ear-Tag QR Recognition:</b> Uses <b>mobile_scanner</b> leveraging native MLKit hardware acceleration to decode 12-digit Pashu Aadhaar barcodes in < 200 ms.",
            "<b>Digital Animal Health Passport:</b> Scanning opens the animal's comprehensive medical dossier: lifetime vaccinations, deworming cycles, past AI scans, and milk yield trends.",
            "<b>Offline Passport Caching:</b> Previously synced herd records can be verified even when standing inside remote rural cattle sheds with zero cellular signal.",
            "<b>On-Site Triage &amp; Escalation:</b> Field vets can verify physical symptoms, update preliminary AI findings, order antibiotic therapy, or log mortality events instantly."
        ],
        border_color="#1E5C97"
    ))
    story.append(create_qa_card(
        6, "Mobile Security &amp; Auth Engineer",
        "How do you secure veterinarian credentials and protect farmer data on shared village phones?",
        [
            "<b>Hardware-Backed Key Storage:</b> JWT access and refresh tokens are encrypted using <b>flutter_secure_storage</b> backed by Android KeyStore and iOS Keychain.",
            "<b>VRC Statutory Gate:</b> Veterinarian registration enforces entering official Veterinary Council State Registration numbers, verified against the backend registry.",
            "<b>Role-Gated Routing (<b>go_router</b>):</b> Route guards dynamically redirect authenticated sessions to the appropriate interface (<b>/farmer/dashboard</b> vs <b>/vet/triage</b>).",
            "<b>No Plaintext PII Storage:</b> Contact numbers and farm geolocation are stored in private app sandbox databases inaccessible to other third-party apps."
        ],
        border_color="#1E5C97"
    ))

    # PAGE BREAK -> Clean start for Page 3
    story.append(PageBreak())

    # =========================================================================
    # PAGE 3: 5-YEAR SIH JURY QUESTIONS SPECIFIC TO MOBILE APPS
    # =========================================================================
    story.append(Paragraph("Part 3: 5-Year SIH Judge Grilling Scenarios on Mobile Deployment", styles['SectionHeader']))
    story.append(create_qa_card(
        7, "Presenter / App Demonstrator",
        "SIH Classic Question: 'Why create a native mobile app when you could just build a responsive web page (PWA)?'",
        [
            "<b>Hardware Camera Integration:</b> PWA camera access through mobile browsers is notoriously flaky on low-cost Android phones; native Flutter access gives instant autofocus and barcode scanning.",
            "<b>Reliable Background Synchronization:</b> PWAs cannot run reliable background sync workers when the browser tab is swiped away. Flutter background isolates sync data silently as soon as signal returns.",
            "<b>Local High-Performance Database:</b> Drift SQLite provides microsecond indexing and multi-table relational queries on 1,000+ cached animals, whereas browser IndexedDB faces strict quota wipes.",
            "<b>Offline Voice AI:</b> Native speech recognition modules interface directly with device audio drivers without requiring constant WebRTC network streaming."
        ],
        border_color="#C2410C"
    ))
    story.append(create_qa_card(
        8, "Offline Systems Engineer",
        "SIH Classic Question: 'What happens if two users submit conflicting reports for the same animal while offline?'",
        [
            "<b>Deterministic Timestamp &amp; State Precedence:</b> The backend employs optimistic locking (<b>@Version</b>) and timestamp ordering (<b>updatedAt</b>).",
            "<b>Role Hierarchy Rule:</b> Veterinarian submissions (<b>VETERINARIAN</b>) supersede preliminary farmer field reports (<b>FARMER</b>). A vet diagnosis automatically resolves conflicting farmer symptom flags.",
            "<b>Append-Only Health Log:</b> Clinical history records are append-only; past reports are never deleted, ensuring a continuous diagnostic audit trail."
        ],
        border_color="#C2410C"
    ))
    story.append(create_qa_card(
        9, "Presenter / App Demonstrator",
        "SIH Classic Question: 'How will farmers get this app? What is your distribution and onboarding strategy?'",
        [
            "<b>Pashu Sakhi Facilitator Network:</b> Deployed through rural women livestock workers (Pashu Sakhis) under the National Rural Livelihood Mission (NRLM) who carry smartphones to village doorsteps.",
            "<b>Ultra-Lightweight APK:</b> Stripped ProGuard ABI splits produce a download size under 16 MB, easily shareable village-to-village via Bluetooth or local Wi-Fi sharing apps.",
            "<b>Single-Screen OTP Registration:</b> Farmers register with just their mobile number and village name; no complex passwords or paperwork required."
        ],
        border_color="#C2410C"
    ))

    # Mobile App Architectural Summary Card
    summary_content = [
        Paragraph("<b>Executive Summary: The 3 Core Pillars of the PASHU SATHI Mobile Client</b>", styles['QuestionTitle']),
        Spacer(1, 2),
        Paragraph(
            "<b>1. Zero-Friction Farmer UX:</b> Multilingual voice AI (Marathi, Hindi, Urdu, English) and visual anatomical tap icons remove literacy barriers.<br/>"
            "<b>2. True Offline-First Architecture:</b> Drift SQLite local database and idempotent Dio sync workers ensure 100% functionality without cellular signal.<br/>"
            "<b>3. Clinical Field Tool for Vets:</b> Sub-second QR scanning unlocks the complete digital animal passport for rapid on-site diagnosis.",
            styles['KeyPoint']
        )
    ]
    summary_table = Table([[summary_content]], colWidths=[504])
    summary_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#EFF6FF")),
        ('LEFTPADDING', (0, 0), (-1, -1), 9),
        ('RIGHTPADDING', (0, 0), (-1, -1), 9),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
        ('LINELEFT', (0, 0), (-1, -1), 3.5, colors.HexColor("#1E5C97")),
        ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor("#BFDBFE")),
    ]))
    story.append(summary_table)

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Generated: {pdf_path}")


if __name__ == "__main__":
    build_mobile_app_qa_pdf()
