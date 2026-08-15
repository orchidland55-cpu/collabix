import { useCallback, useId, useMemo, useState } from 'react';
import { cn } from '../../lib/cn';

export interface ActivityChartPoint {
  label: string;
  value: number;
}

interface AdminActivityLineChartProps {
  data: ActivityChartPoint[];
  height?: number;
  className?: string;
}

const STROKE = 'rgb(var(--accent-500))';
const GRID = 'rgb(var(--border-subtle) / 0.65)';
const AXIS = 'rgb(var(--text-tertiary))';

export function AdminActivityLineChart({ data, height = 240, className }: AdminActivityLineChartProps) {
  const gradId = useId();
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const chart = useMemo(() => {
    const padLeft = 8;
    const padRight = 4;
    const padTop = 8;
    const padBottom = 4;
    const w = 100;
    const h = height / 2;
    const innerW = w - padLeft - padRight;
    const innerH = h - padTop - padBottom;

    const max = Math.max(...data.map((d) => d.value), 1);
    const stepX = data.length > 1 ? innerW / (data.length - 1) : 0;

    const points = data.map((d, i) => {
      const x = padLeft + i * stepX;
      const y = padTop + innerH - (d.value / max) * innerH;
      return { x, y, ...d };
    });

    const linePath = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
    const areaPath = points.length
      ? `${linePath} L ${points[points.length - 1].x} ${padTop + innerH} L ${points[0].x} ${padTop + innerH} Z`
      : '';

    const yTicks = [0, Math.ceil(max / 2), max];

    return { w, h, padLeft, padTop, innerH, max, points, linePath, areaPath, yTicks };
  }, [data, height]);

  const handleMove = useCallback(
    (event: React.MouseEvent<SVGSVGElement>) => {
      const rect = event.currentTarget.getBoundingClientRect();
      const relativeX = ((event.clientX - rect.left) / rect.width) * chart.w;
      let closest = 0;
      let minDist = Number.POSITIVE_INFINITY;
      chart.points.forEach((p, i) => {
        const dist = Math.abs(p.x - relativeX);
        if (dist < minDist) {
          minDist = dist;
          closest = i;
        }
      });
      setHoveredIndex(closest);
    },
    [chart.points, chart.w],
  );

  if (!data.length) {
    return null;
  }

  const hovered = hoveredIndex != null ? chart.points[hoveredIndex] : null;

  return (
    <div className={cn('relative w-full', className)}>
      <svg
        role="img"
        aria-label={`Activity line chart with ${data.length} data points`}
        viewBox={`0 0 ${chart.w} ${chart.h}`}
        preserveAspectRatio="none"
        className="w-full touch-none"
        style={{ height }}
        onMouseMove={handleMove}
        onMouseLeave={() => setHoveredIndex(null)}
      >
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={STROKE} stopOpacity="0.22" />
            <stop offset="100%" stopColor={STROKE} stopOpacity="0" />
          </linearGradient>
        </defs>

        {chart.yTicks.map((tick) => {
          const y = chart.padTop + chart.innerH - (tick / chart.max) * chart.innerH;
          return (
            <g key={tick}>
              <line x1={chart.padLeft} y1={y} x2={chart.w - 4} y2={y} stroke={GRID} strokeWidth={0.25} />
              <text x={0} y={y + 0.8} fill={AXIS} fontSize={2.2} opacity={0.85}>
                {tick}
              </text>
            </g>
          );
        })}

        <path d={chart.areaPath} fill={`url(#${gradId})`} />
        <path
          d={chart.linePath}
          fill="none"
          stroke={STROKE}
          strokeWidth={0.75}
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {chart.points.map((p, i) => (
          <circle
            key={i}
            cx={p.x}
            cy={p.y}
            r={hoveredIndex === i ? 1.4 : 0.9}
            fill={STROKE}
            opacity={hoveredIndex == null || hoveredIndex === i ? 1 : 0.35}
          />
        ))}

        {hovered && (
          <line
            x1={hovered.x}
            y1={chart.padTop}
            x2={hovered.x}
            y2={chart.padTop + chart.innerH}
            stroke={STROKE}
            strokeWidth={0.35}
            strokeDasharray="1 1"
            opacity={0.5}
          />
        )}
      </svg>

      {hovered && (
        <div
          className="pointer-events-none absolute z-10 rounded-lg border border-border-subtle bg-surface px-2.5 py-1.5 shadow-md"
          style={{
            left: `${(hovered.x / chart.w) * 100}%`,
            top: `${(hovered.y / chart.h) * 100}%`,
            transform: 'translate(-50%, -120%)',
          }}
        >
          <p className="text-2xs text-text-tertiary">{hovered.label}</p>
          <p className="text-caption font-semibold text-text-primary">{hovered.value} activities</p>
        </div>
      )}

      <div className="mt-2 flex justify-between gap-1">
        {data.map((d, i) => (
          <span
            key={i}
            className={cn(
              'text-2xs truncate text-center',
              hoveredIndex === i ? 'text-text-primary font-medium' : 'text-text-tertiary',
            )}
            style={{ width: `${100 / data.length}%` }}
          >
            {d.label.length > 6 && data.length > 7 ? d.label.slice(0, 3) : d.label}
          </span>
        ))}
      </div>
    </div>
  );
}

export interface DonutSegment {
  label: string;
  value: number;
  color: string;
  percentage: number;
}

interface AdminProjectDonutChartProps {
  segments: DonutSegment[];
  legendSegments?: DonutSegment[];
  size?: number;
  className?: string;
}

export function AdminProjectDonutChart({
  segments,
  legendSegments,
  size = 168,
  className,
}: AdminProjectDonutChartProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const legend = legendSegments ?? segments;
  const total = legend.reduce((sum, s) => sum + s.value, 0);
  const outerRadius = 40;
  const innerRadius = 24;
  const cx = 50;
  const cy = 50;

  const arcs = useMemo(() => {
    if (total === 0) return [];
    let cumulative = 0;
    return segments.map((segment) => {
      const startAngle = (cumulative / total) * 2 * Math.PI - Math.PI / 2;
      cumulative += segment.value;
      const endAngle = (cumulative / total) * 2 * Math.PI - Math.PI / 2;
      return { ...segment, startAngle, endAngle };
    });
  }, [segments, total]);

  const describeArc = (startAngle: number, endAngle: number) => {
    const x1o = cx + outerRadius * Math.cos(startAngle);
    const y1o = cy + outerRadius * Math.sin(startAngle);
    const x2o = cx + outerRadius * Math.cos(endAngle);
    const y2o = cy + outerRadius * Math.sin(endAngle);
    const x1i = cx + innerRadius * Math.cos(endAngle);
    const y1i = cy + innerRadius * Math.sin(endAngle);
    const x2i = cx + innerRadius * Math.cos(startAngle);
    const y2i = cy + innerRadius * Math.sin(startAngle);
    const largeArc = endAngle - startAngle > Math.PI ? 1 : 0;
    return `M ${x1o} ${y1o} A ${outerRadius} ${outerRadius} 0 ${largeArc} 1 ${x2o} ${y2o} L ${x1i} ${y1i} A ${innerRadius} ${innerRadius} 0 ${largeArc} 0 ${x2i} ${y2i} Z`;
  };

  return (
    <div className={cn('flex flex-col gap-5 sm:flex-row sm:items-center', className)}>
      <div className="relative shrink-0 mx-auto sm:mx-0">
        <svg
          role="img"
          aria-label="Project status donut chart"
          viewBox="0 0 100 100"
          style={{ width: size, height: size }}
          className="shrink-0"
        >
          {total === 0 ? (
            <circle cx={cx} cy={cy} r={outerRadius} fill="none" stroke={GRID} strokeWidth={8} />
          ) : (
            arcs.map((arc, i) => (
              <path
                key={arc.label}
                d={describeArc(arc.startAngle, arc.endAngle)}
                fill={arc.color}
                opacity={hoveredIndex == null || hoveredIndex === i ? 0.92 : 0.45}
                stroke="rgb(var(--bg-elevated))"
                strokeWidth={0.6}
                onMouseEnter={() => setHoveredIndex(i)}
                onMouseLeave={() => setHoveredIndex(null)}
              />
            ))
          )}
        </svg>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-lg font-bold text-text-primary leading-none">{total}</span>
          <span className="text-2xs text-text-tertiary mt-0.5">projects</span>
        </div>
        {hoveredIndex != null && arcs[hoveredIndex] && (
          <div className="absolute -top-2 left-1/2 -translate-x-1/2 -translate-y-full rounded-lg border border-border-subtle bg-surface px-2.5 py-1.5 shadow-md whitespace-nowrap">
            <p className="text-caption font-medium text-text-primary">{arcs[hoveredIndex].label}</p>
            <p className="text-2xs text-text-tertiary">
              {arcs[hoveredIndex].value} · {arcs[hoveredIndex].percentage}%
            </p>
          </div>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-2.5 min-w-0">
        {legend.map((segment) => (
          <div key={segment.label} className="flex items-center gap-2.5">
            <span
              className="h-2.5 w-2.5 rounded-full shrink-0"
              style={{ backgroundColor: segment.color }}
            />
            <span className="text-caption text-text-secondary truncate flex-1">{segment.label}</span>
            <span className="text-caption font-semibold text-text-primary tabular-nums">{segment.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
