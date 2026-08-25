import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';

interface PDFExportOptions {
  title: string;
  filename?: string;
  elementId?: string;
  content?: string;
  metadata?: {
    workspace?: string;
    department?: string;
    generatedDate?: string;
    reportType?: string;
  };
}

export async function exportReportToPDF(options: PDFExportOptions): Promise<void> {
  const { title, filename, elementId, content, metadata } = options;

  const pdf = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4',
  });

  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  const margin = 20;
  const contentWidth = pageWidth - 2 * margin;

  let yPos = margin;

  // Helper to add text with word wrapping
  const addText = (text: string, fontSize: number = 11, fontStyle: string = 'normal', color: [number, number, number] = [30, 30, 30], maxWidth: number = contentWidth) => {
    pdf.setFontSize(fontSize);
    pdf.setFont('helvetica', fontStyle);
    pdf.setTextColor(...color);
    const lines = pdf.splitTextToSize(text, maxWidth);
    lines.forEach((line: string) => {
      if (yPos > pageHeight - margin) {
        pdf.addPage();
        yPos = margin;
      }
      pdf.text(line, margin, yPos);
      yPos += fontSize * 0.5;
    });
    yPos += 2;
  };

  // Helper to add heading
  const addHeading = (text: string, level: 1 | 2 | 3 = 1) => {
    const sizes: Record<1 | 2 | 3, number> = { 1: 20, 2: 16, 3: 14 };
    const styles: Record<1 | 2 | 3, string> = { 1: 'bold', 2: 'bold', 3: 'bold' };
    yPos += 4;
    if (yPos > pageHeight - margin) {
      pdf.addPage();
      yPos = margin;
    }
    addText(text, sizes[level] || 14, styles[level] || 'bold', [20, 20, 20]);
  };

  // Title
  pdf.setFontSize(24);
  pdf.setFont('helvetica', 'bold');
  pdf.setTextColor(99, 102, 241); // accent color
  const titleLines = pdf.splitTextToSize(title, contentWidth);
  titleLines.forEach((line: string) => {
    pdf.text(line, margin, yPos);
    yPos += 12;
  });
  yPos += 4;

  // Metadata section
  if (metadata) {
    pdf.setDrawColor(200, 200, 200);
    pdf.setLineWidth(0.5);
    pdf.line(margin, yPos, pageWidth - margin, yPos);
    yPos += 6;

    const metaItems = [
      { label: 'Workspace', value: metadata.workspace },
      { label: 'Department', value: metadata.department },
      { label: 'Report Type', value: metadata.reportType },
      { label: 'Generated', value: metadata.generatedDate },
    ].filter((item) => item.value);

    metaItems.forEach((item) => {
      pdf.setFontSize(9);
      pdf.setFont('helvetica', 'bold');
      pdf.setTextColor(100, 100, 100);
      pdf.text(`${item.label}:`, margin, yPos);
      pdf.setFont('helvetica', 'normal');
      pdf.setTextColor(50, 50, 50);
      pdf.text(item.value || '', margin + 40, yPos);
      yPos += 5;
    });
    yPos += 6;

    pdf.setDrawColor(200, 200, 200);
    pdf.line(margin, yPos, pageWidth - margin, yPos);
    yPos += 10;
  }

  // Content
  if (content) {
    // Parse and render markdown-like content
    const sections = content.split(/^##\s+/m);
    if (sections.length > 1) {
      // First section might be empty or title
      sections.forEach((section, index) => {
        if (!section.trim()) return;
        const lines = section.split('\n');
        const heading = lines[0].trim();
        const body = lines.slice(1).join('\n').trim();

        if (index === 0 && heading === title.trim()) {
          // Skip duplicate title
          if (body) addText(body);
        } else {
          addHeading(heading, 2);
          if (body) addText(body);
        }
      });
    } else {
      // Simple content
      addText(content);
    }
  }

  // If elementId provided, capture the DOM element
  if (elementId) {
    const element = document.getElementById(elementId);
    if (element) {
      pdf.addPage();
      yPos = margin;
      addHeading('Report Content', 2);

      const canvas = await html2canvas(element, {
        scale: 2,
        useCORS: true,
        logging: false,
        backgroundColor: '#ffffff',
      });

      const imgData = canvas.toDataURL('image/png');
      const imgWidth = contentWidth;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;

      let remainingHeight = imgHeight;
      let sourceY = 0;

      while (remainingHeight > 0) {
        const pageSpace = pageHeight - yPos - margin;
        const drawHeight = Math.min(remainingHeight, pageSpace);

        if (drawHeight < 10) {
          pdf.addPage();
          yPos = margin;
          continue;
        }

        const sourceCanvas = document.createElement('canvas');
        sourceCanvas.width = canvas.width;
        sourceCanvas.height = Math.ceil((drawHeight * canvas.width) / imgWidth);
        const ctx = sourceCanvas.getContext('2d');
        if (ctx) {
          ctx.drawImage(
            canvas,
            0, sourceY, canvas.width, sourceCanvas.height,
            0, 0, sourceCanvas.width, sourceCanvas.height
          );
        }

        const pageImgData = sourceCanvas.toDataURL('image/png');
        pdf.addImage(pageImgData, 'PNG', margin, yPos, imgWidth, drawHeight);

        remainingHeight -= drawHeight;
        sourceY += sourceCanvas.height;
        yPos += drawHeight + 5;

        if (remainingHeight > 0) {
          pdf.addPage();
          yPos = margin;
        }
      }
    }
  }

  // Footer
  const totalPages = pdf.getNumberOfPages();
  for (let i = 1; i <= totalPages; i++) {
    pdf.setPage(i);
    pdf.setFontSize(8);
    pdf.setTextColor(150, 150, 150);
    pdf.text(
      `Collabix AI Report | Page ${i} of ${totalPages} | Generated ${new Date().toLocaleDateString()}`,
      pageWidth / 2,
      pageHeight - 10,
      { align: 'center' }
    );
  }

  // Save
  const finalFilename = filename || `${title.replace(/[^a-z0-9]/gi, '_').toLowerCase()}_${new Date().toISOString().split('T')[0]}.pdf`;
  pdf.save(finalFilename);
}

export function generateReportHTMLContent(reportData: {
  title: string;
  executiveSummary: string;
  majorHighlights?: string;
  businessHealth?: string;
  productivityReview?: string;
  criticalRisks?: string;
  achievements?: string;
  challenges?: string;
  recommendations?: string;
  strategicPriorities?: string;
  nextActions?: string;
  finalReport?: string;
  metadata?: {
    workspace?: string;
    department?: string;
    generatedDate?: string;
    reportType?: string;
  };
}): string {
  const { title, executiveSummary, majorHighlights, businessHealth, productivityReview, criticalRisks, achievements, challenges, recommendations, strategicPriorities, nextActions, finalReport, metadata } = reportData;

  let html = `<h1>${title}</h1>`;

  if (metadata) {
    html += '<div class="report-metadata">';
    if (metadata.workspace) html += `<p><strong>Workspace:</strong> ${metadata.workspace}</p>`;
    if (metadata.department) html += `<p><strong>Department:</strong> ${metadata.department}</p>`;
    if (metadata.reportType) html += `<p><strong>Report Type:</strong> ${metadata.reportType}</p>`;
    if (metadata.generatedDate) html += `<p><strong>Generated:</strong> ${metadata.generatedDate}</p>`;
    html += '</div><hr/>';
  }

  if (executiveSummary) html += `<h2>Executive Summary</h2><div>${executiveSummary}</div>`;
  if (majorHighlights) html += `<h2>Key Highlights</h2><div>${majorHighlights}</div>`;
  if (businessHealth) html += `<h2>Business Health Assessment</h2><div>${businessHealth}</div>`;
  if (productivityReview) html += `<h2>Productivity Review</h2><div>${productivityReview}</div>`;
  if (criticalRisks) html += `<h2>Critical Risks & Issues</h2><div>${criticalRisks}</div>`;
  if (achievements) html += `<h2>Achievements</h2><div>${achievements}</div>`;
  if (challenges) html += `<h2>Challenges</h2><div>${challenges}</div>`;
  if (recommendations) html += `<h2>Strategic Recommendations</h2><div>${recommendations}</div>`;
  if (strategicPriorities) html += `<h2>Strategic Priorities</h2><div>${strategicPriorities}</div>`;
  if (nextActions) html += `<h2>Next Actions</h2><div>${nextActions}</div>`;
  if (finalReport && finalReport !== executiveSummary) html += `<h2>Full Report</h2><div>${finalReport}</div>`;

  return html;
}