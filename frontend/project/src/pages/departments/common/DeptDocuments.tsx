import { useState } from 'react';
import { Search, FileText, Download, Star, Loader2, AlertCircle } from 'lucide-react';
import { Card, CardBody } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Badge } from '../../../components/ui/Badge';
import { EmptyState } from '../../../components/ui/EmptyState';
import { useWorkspaceDocuments } from '../../../services/document-hooks';
import { downloadAuthenticatedFile } from '../../../lib/file-download';
import { getFileIcon, formatFileSize, formatDate } from '../../knowledge/types/document-types';

export function DeptDocuments({ wsId }: { wsId?: string; deptId?: string }) {
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState<string | null>(null);

  const { data, isLoading, isError, refetch } = useWorkspaceDocuments(wsId ?? '');

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-text-tertiary" />
      </div>
    );
  }

  if (isError) {
    return (
      <Card>
        <CardBody className="py-16 flex flex-col items-center gap-3">
          <AlertCircle className="h-8 w-8 text-danger-500" />
          <p className="text-body font-medium text-text-secondary">Failed to load documents</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>Retry</Button>
        </CardBody>
      </Card>
    );
  }

  const documents = data?.content ?? [];
  const categories = Array.from(new Set(documents.map((d) => d.category).filter(Boolean))) as string[];

  const filtered = documents.filter((d) => {
    if (search) {
      const q = search.toLowerCase();
      if (!d.title.toLowerCase().includes(q) && !(d.fileName ?? '').toLowerCase().includes(q)) return false;
    }
    if (category && d.category !== category) return false;
    return true;
  });

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <Input
          placeholder="Search documents..."
          leftIcon={<Search />}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          containerClassName="max-w-sm"
        />
        <Badge tone="neutral" variant="soft">{filtered.length} documents</Badge>
      </div>

      {categories.length > 0 && (
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setCategory(null)}
            className={`rounded-md px-3 py-1.5 text-caption font-medium transition-colors ${!category ? 'bg-accent-600 text-white' : 'text-text-secondary hover:bg-surface-2'}`}
          >
            All
          </button>
          {categories.map((c) => (
            <button
              key={c}
              onClick={() => setCategory(c)}
              className={`rounded-md px-3 py-1.5 text-caption font-medium transition-colors ${category === c ? 'bg-accent-600 text-white' : 'text-text-secondary hover:bg-surface-2'}`}
            >
              {c}
            </button>
          ))}
        </div>
      )}

      {filtered.length === 0 ? (
        <EmptyState
          icon={<FileText />}
          title={search || category ? 'No documents match your filters' : 'No documents yet'}
          description="Documents uploaded to this workspace will appear here."
        />
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((d) => (
            <Card key={d.id}>
              <CardBody className="flex flex-col gap-3">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent-50 text-accent-600 dark:bg-accent-100">
                      <FileText className="h-4 w-4" />
                    </span>
                    <div className="min-w-0">
                      <p className="text-body font-medium text-text-primary truncate">{d.title}</p>
                      <p className="text-2xs text-text-tertiary">{getFileIcon(d.mimeType).toUpperCase()} • {formatFileSize(d.fileSize)}</p>
                    </div>
                  </div>
                  <Star className="h-4 w-4 text-text-tertiary" />
                </div>
                <div className="flex items-center gap-2">
                  {d.category && <Badge tone="neutral" variant="soft">{d.category}</Badge>}
                  <Badge tone="info" variant="soft">v{d.version}</Badge>
                  <Badge tone={d.status === 'ACTIVE' ? 'success' : 'neutral'} variant="soft">{d.status}</Badge>
                </div>
                <div className="flex items-center justify-between border-t border-border-subtle pt-2">
                  <div className="flex items-center gap-2 text-2xs text-text-tertiary">
                    <span>{d.updatedBy}</span>
                    <span>• {formatDate(d.updatedAt)}</span>
                  </div>
                  <button
                    type="button"
                    className="flex h-7 w-7 items-center justify-center rounded-md text-text-tertiary hover:bg-surface-2 hover:text-text-primary transition-colors"
                    title="Download"
                    onClick={() => downloadAuthenticatedFile(`/workspaces/${wsId}/documents/${d.id}/download`, d.fileName ?? d.title).catch(() => {})}
                  >
                    <Download className="h-3.5 w-3.5" />
                  </button>
                </div>
              </CardBody>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
