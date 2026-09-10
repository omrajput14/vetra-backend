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
            self.drawString(122, 752, "|  Government Command Dashboard Q&A — SIH 2026 (SIH26128)")
            self.drawRightString(558, 752, "Team MOSAIC")
            self.setStrokeColor(colors.HexColor("#E2E8F0"))
            self.setLineWidth(0.75)
            self.line(54, 744, 558, 744)

        # Footer
        footer_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(558, 30, footer_text)
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#1E7E34"))
        self.drawString(54, 30, "GOVERNMENT OPERATIONAL DEFENSE")
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748B"))
        self.drawString(225, 30, "— SIH 2026 5-Year Judge Review & Evaluation Guide")
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


def create_qa_card(q_num, role, question, answer_points, border_color="#1E5C97", bg_color="#F8FAFC"):
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


def build_gov_dashboard_qa_pdf():
    pdf_path = os.path.join(OUTPUT_DIR, "PASHU_SATHI_Gov_Dashboard_QA.pdf")
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
    story.append(Paragraph("PASHU SATHI — Government Command Dashboard Q&amp;A", styles['DocHeaderTitle']))
    story.append(Paragraph("SIH 2026 5-Year Historical Judge Question Bank &amp; Technical Defense (SIH26128)", styles['DocHeaderSubtitle']))
    
    # Meta Box
    meta_data = [
        [Paragraph("<b>Target System:</b> PASHU SATHI Government Command Center (React 18 + PostGIS GIS)", styles['MetaBannerText']),
         Paragraph("<b>Deployment:</b> Live Vercel Production + Azure REST API", styles['MetaBannerText'])],
        [Paragraph("<b>Primary Users:</b> State Epidemiologists, District Magistrates &amp; Veterinary Officers", styles['MetaBannerText']),
         Paragraph("<b>Evaluation Scope:</b> UI/UX, Spatial Engine, Actionability, Security &amp; GIGW", styles['MetaBannerText'])],
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
    # PAGE 1: TECHNICAL ARCHITECTURE & GIS STACK
    # =========================================================================
    story.append(Paragraph("Part 1: Front-End Architecture, GIS Engine &amp; Performance (Technical)", styles['SectionHeader']))
    story.append(create_qa_card(
        1, "Frontend &amp; GIS Engineer",
        "Explain the dashboard tech stack. Why choose React + Vite over Angular or Next.js for a government tool?",
        [
            "<b>Modern Component Core:</b> Built with React 18, TypeScript, and Vite for instant HMR development and minimal production bundle size (< 240 KB gzipped).",
            "<b>Tailwind CSS &amp; Tactical Tokens:</b> Uses a strict, accessible design token system (Navy, Forest Green, 4-tier risk tokens) tailored for high readability in harsh daylight or emergency control rooms.",
            "<b>Vite Static Build Advantage:</b> Unlike heavy SSR frameworks like Next.js that require node servers, Vite outputs purely static optimized assets deployable to edge CDNs (Vercel / S3) with zero cold-start latency.",
            "<b>Testing Rigor:</b> Backed by 95 unit and integration tests using Vitest and React Testing Library (100% green)."
        ],
        border_color="#1E5C97"
    ))
    story.append(create_qa_card(
        2, "GIS &amp; Mapping Specialist",
        "How is the spatial map implemented? Why Leaflet instead of heavy Google Maps API?",
        [
            "<b>Open-Source PostGIS Compliance:</b> Built with Leaflet.js natively consuming RFC 7946 GeoJSON layers directly emitted by PostgreSQL 17 / PostGIS 3.5.",
            "<b>Zero Commercial API Tax:</b> Google Maps incurs high per-load API costs and strict quota cutoffs. Leaflet pairs with CARTO Voyager / MapTiler vector tiles at near-zero operating cost.",
            "<b>Multi-Layer Vector Overlay:</b> Renders 394 administrative boundaries (1 State, 36 Districts, 357 Talukas), dynamic 25–50 km buffer perimeters, and kernel density estimation (KDE) heatmaps.",
            "<b>Dynamic Camera Auto-Fly:</b> Selecting any administrative scope (e.g. Pune, Khandesh, Baramati) executes programmatic viewport bounding-box fly animations."
        ],
        border_color="#1E5C97"
    ))
    story.append(create_qa_card(
        3, "Performance &amp; Optimization Engineer",
        "How do you render 357 taluka boundary polygons without lagging on low-spec government PCs?",
        [
            "<b>Zoom-Dependent Layer Toggling:</b> Taluka boundary GeoJSON is conditionally mounted only when current zoom level reaches >= 9 (`TALUKA_AUTO_ZOOM_THRESHOLD`), preventing DOM bloat at statewide view.",
            "<b>TopoJSON / Simplified Coordinate Precision:</b> Administrative boundary geometry coordinates are pre-simplified to 4 decimal places, reducing polygon vertex weight by over 65%.",
            "<b>R-Tree Spatial Caching:</b> Layers are managed in dedicated Leaflet `LayerGroup` instances that clear and remount only on scope change, eliminating memory leaks."
        ],
        border_color="#1E5C97"
    ))

    # PAGE BREAK -> Clean start for Page 2
    story.append(PageBreak())

    # =========================================================================
    # PAGE 2: OPERATIONAL ACTIONS & GOVERNMENT WORKFLOW
    # =========================================================================
    story.append(Paragraph("Part 2: Executive Actionability &amp; Real-Time Telemetry (Technical + Operations)", styles['SectionHeader']))
    story.append(create_qa_card(
        4, "Systems Architect",
        "Is this dashboard just read-only analytics, or can an officer execute real statutory actions?",
        [
            "<b>Statutory Bio-Containment Directives:</b> Officers can open the Outbreak Intelligence modal and execute legally binding containment orders (setting movement bans, market closures, and buffer radiuses).",
            "<b>Collector Escalation Channel:</b> In the Alerts Priority Queue, officers can acknowledge alerts or escalate critical zoonotic outbreaks (Rabies/Anthrax) directly to the District Collector and Disaster Authority.",
            "<b>Vaccine Campaign Launchpad:</b> In Vaccination Intelligence, officers analyze pathogen immunity deficits and launch cold-chain ring-vaccination campaigns targeted strictly at at-risk talukas.",
            "<b>Offline SitRep CSV Export:</b> One-click Situation Report CSV download formats field deployment rosters for Rapid Response Teams during rural power/connectivity outages."
        ],
        border_color="#1E7E34"
    ))
    story.append(create_qa_card(
        5, "Data &amp; API Engineer",
        "How does 'Live Telemetry' update without crashing the database under 1,000 concurrent state officers?",
        [
            "<b>Stateless REST Architecture:</b> All dashboard components query stateless `/api/v1` endpoints protected by Spring Security 6 JWT Bearer filters.",
            "<b>Redis 7 Micro-Caching:</b> Outbreak clusters and composite risk decompositions are cached in Redis with a 30-minute TTL; identical officer requests hit RAM cache rather than disk SQL queries.",
            "<b>Manual 'Sync Telemetry' Pulse:</b> Eliminates unthrottled WebSockets battery/server drain; users receive automated status polling with explicit user-triggered refresh capabilities."
        ],
        border_color="#1E7E34"
    ))
    story.append(create_qa_card(
        6, "Public Administration &amp; Policy Analyst",
        "How does administrative scoping work? Can a Dhule District Officer see Pune data?",
        [
            "<b>Cascading Scope Filter Engine:</b> Implemented in `scopeFilter.ts` with explicit geographical bounding coordinates, radius math, and district keyword matching across all 7 operational screens.",
            "<b>Granular Multi-Tier Scopes:</b> Supports `Statewide (Maharashtra)`, `Pune District`, `Baramati Taluka`, `Dhule District`, `Jalgaon District`.",
            "<b>Role-Based Spatial Containment:</b> When an officer logs in with district credentials, the scope selector locks to their assigned jurisdiction, preventing unauthorized cross-district data leakage."
        ],
        border_color="#1E7E34"
    ))

    # PAGE BREAK -> Clean start for Page 3
    story.append(PageBreak())

    # =========================================================================
    # PAGE 3: HISTORICAL SIH JUDGE QUESTIONS & DEFENSE MATRIX
    # =========================================================================
    story.append(Paragraph("Part 3: 5-Year SIH Judge Grilling Scenarios &amp; Proven Defenses (Non-Technical)", styles['SectionHeader']))
    story.append(create_qa_card(
        7, "UI/UX &amp; Accessibility Specialist",
        "SIH Classic Question: 'How does your dashboard comply with GIGW (Guidelines for Indian Government Websites) and accessibility?'",
        [
            "<b>High-Contrast Semantic Palettes:</b> Designed to meet WCAG 2.1 Level AA color contrast ratios (Navy `#0E1A2B` on white `#FFFFFF` achieves 14.8:1, well above the 4.5:1 requirement).",
            "<b>Color-Blind Safe Symbolism:</b> Never relies on color alone. Confirmed cases are marked with solid squares (■); suspected cases with hollow diamonds (◇); risk tags include text badges.",
            "<b>Accessible Tabular Fallback:</b> Every map visual is backed by an accessible, screen-reader-friendly data table (`OutbreakAccessibleListView.tsx`) for keyboard navigability.",
            "<b>Standard Gov Typography:</b> Utilizes clean, official sans-serif fonts with JetBrains Mono for coordinates and alphanumeric ear-tag IDs."
        ],
        border_color="#C2410C"
    ))
    story.append(create_qa_card(
        8, "Healthcare Domain Specialist",
        "SIH Classic Question: 'What prevents a corrupt or panicked officer from shutting down a cattle market on a false AI rumor?'",
        [
            "<b>Two-Man Rule Protocol:</b> AI-screened animals remain strictly labeled as `SUSPECTED` and cannot legally trigger bio-containment directives on the dashboard.",
            "<b>Veterinary Council Authentication:</b> Containment orders require entering a verified Council Registration Number (VRC) or verified PCR laboratory confirmation from an accredited state LIMS lab.",
            "<b>Immutable Access Audit Trail:</b> All actions (directive deployments, alert acknowledgements, exports) are permanently recorded in the immutable audit ledger with user ID, IP, and timestamp."
        ],
        border_color="#C2410C"
    ))
    story.append(create_qa_card(
        9, "Presenter / Pitcher",
        "SIH Classic Question: 'Government already has INAPH and Bharat Pashudhan. Why would the Animal Husbandry Ministry deploy this?'",
        [
            "<b>Retrospective vs. Tactical:</b> Bharat Pashudhan is a digital census register updated weeks after disease events. Pashu Sathi is a tactical emergency dispatch room.",
            "<b>Pre-Configured SOP Library:</b> Built-in Protocols Reference section houses standard operating procedures (SOPs) for FMD, LSD, Anthrax, and Rabies matching National Disaster Guidelines.",
            "<b>Plug-and-Play Integration:</b> Feeds into existing state infrastructure via open REST APIs without demanding a replacement of state servers or ear-tag hardware."
        ],
        border_color="#C2410C"
    ))

    # Executive Summary Card at the bottom of Page 3
    summary_content = [
        Paragraph("<b>Executive Summary: The 3-Pillars of the PASHU SATHI Government Command Center</b>", styles['QuestionTitle']),
        Spacer(1, 2),
        Paragraph(
            "<b>1. Spatial Reality (GIS):</b> Real-time PostGIS clustering compresses outbreak discovery from 14 days of paper reporting down to minutes.<br/>"
            "<b>2. Statutory Safety:</b> Mandatory licensed veterinary gates eliminate false alarms and panic shutdowns while adhering strictly to Indian veterinary law.<br/>"
            "<b>3. Direct Actionability:</b> Equips district magistrates with instant bio-containment deployment, cold-chain vaccination dispatch, and offline SitRep rosters.",
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
    build_gov_dashboard_qa_pdf()
