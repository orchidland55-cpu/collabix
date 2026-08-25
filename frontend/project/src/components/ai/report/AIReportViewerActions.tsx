import { Download, Copy, Printer, Share2, Heart, RefreshCw, MessageSquare, FileText, Loader2 } from 'lucide-react';
import { useState } from 'react';
import { cn } from '../../../lib/cn';
import { Button } from '../../ui/Button';
import { IconButton } from '../../ui/IconButton';
import { useToast } from '../../../components/ui/Toast';
import { exportReportToPDF } from '../../../lib/pdf-export';
import type { ReportingResponse } from '../../../services/reporting-ai-service';

interface AIReportViewerActionsProps {
  favorite: boolean;
  onToggleFavorite: () => void;
  reportData?: ReportingResponse;
}

export function AIReportViewerActions({ favorite, onToggleFavorite, reportData }: AIReportViewerActionsProps) {
  const [copied, setCopied] = useState(false);
  const [exporting, setExporting] = useState(false);
  const { toast } = useToast();

  function handleCopy() {
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  async function handleExportPDF() {
    if (!reportData) {
      toast({ title: 'No report data available', tone: 'error' });
      return;
    }

    setExporting(true);
    try {
      await exportReportToPDF({
        title: reportData.title,
        metadata: {
          workspace: 'Workspace',
          department: reportData.departmentId,
          generatedDate: reportData.generationDate,
          reportType: reportData.reportType,
        },
        content: reportData.finalReport || reportData.executiveSummary,
      });
      toast({ title: 'PDF exported successfully', tone: 'success' });
    } catch (error) {
      console.error('PDF export failed:', error);
      toast({ title: 'Failed to export PDF', tone: 'error' });
    } finally {
      setExporting(false);
    }
  }

  return (
    <div className="sticky bottom-0 z-10 rounded-xl border border-border-subtle bg-elevated dark:bg-surface shadow-cx-lg p-3">
      <div className="flex items-center justify-between gap-2 flex-wrap">
        <div className="flex items-center gap-1.5">
          <Button
            size="sm"
            variant="primary"
            leftIcon={exporting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download />}
            onClick={handleExportPDF}
            disabled={exporting}
          >
            {exporting ? 'Exporting...' : 'Export PDF'}
          </Button>
          <IconButton size="sm" label={copied ? 'Copied' : 'Copy'} variant="ghost" onClick={handleCopy}>
            <Copy className={cn('h-4 w-4', copied && 'text-success-500')} />
          </IconButton>
          <IconButton size="sm" label="Print" variant="ghost" onClick={() => window.print()}>
            <Printer className="h-4 w-4" />
          </IconButton>
          <IconButton size="sm" label="Share" variant="ghost" onClick={() => {}}>
            <Share2 className="h-4 w-4" />
          </IconButton>
          <IconButton size="sm" label={favorite ? 'Remove from favorites' : 'Add to favorites'} variant="ghost" onClick={onToggleFavorite}>
            <Heart className={cn('h-4 w-4', favorite && 'fill-danger-500 text-danger-500')} />
          </IconButton>
        </div>
        <div className="flex items-center gap-1.5">
          <Button size="sm" variant="secondary" leftIcon={<RefreshCw />}>Regenerate</Button>
          <Button size="sm" variant="secondary" leftIcon={<MessageSquare />}>Continue</Button>
          <Button size="sm" variant="ghost" leftIcon={<FileText />}>Source Data</Button>
        </div>
      </div>
    </div>
  );
}
