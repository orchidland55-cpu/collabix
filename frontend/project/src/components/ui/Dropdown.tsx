import { useEffect, useRef, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { cn } from '../../lib/cn';

export interface DropdownItem {
  id?: string | number;
  label?: string;
  icon?: ReactNode;
  onClick?: () => void;
  className?: string;
  danger?: boolean;
  disabled?: boolean;
  divider?: boolean;
}

export interface DropdownProps {
  trigger: ReactNode;
  items: DropdownItem[];
  align?: 'left' | 'right';
  className?: string;
}

export function Dropdown({ trigger, items, align = 'left', className }: DropdownProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const [menuPos, setMenuPos] = useState<{ top: number; left: number } | null>(null);

  useEffect(() => {
    if (!open || !ref.current) return;
    const updatePosition = () => {
      if (!ref.current) return;
      const rect = ref.current.getBoundingClientRect();
      const menuWidth = menuRef.current?.offsetWidth ?? 180;
      setMenuPos({
        top: rect.bottom + 4,
        left: align === 'right' ? rect.right - menuWidth : rect.left,
      });
    };
    updatePosition();
    window.addEventListener('scroll', updatePosition, true);
    window.addEventListener('resize', updatePosition);
    return () => {
      window.removeEventListener('scroll', updatePosition, true);
      window.removeEventListener('resize', updatePosition);
    };
  }, [open, align]);

  useEffect(() => {
    if (!open) return;
    const onClick = (e: MouseEvent) => {
      const target = e.target as Node;
      if (ref.current?.contains(target) || menuRef.current?.contains(target)) return;
      setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const visibleItems = items.filter((item) => item.divider || item.label);

  return (
    <div ref={ref} className={cn('relative inline-block', className)}>
      <div
        role="button"
        tabIndex={0}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={(e) => {
          e.stopPropagation();
          e.preventDefault();
          setOpen((o) => !o);
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.stopPropagation();
            e.preventDefault();
            setOpen((o) => !o);
          }
        }}
      >
        {trigger}
      </div>
      {open && menuPos && visibleItems.length > 0 && createPortal(
        <div
          ref={menuRef}
          className="fixed z-[200] min-w-[180px] rounded-lg border border-border-subtle bg-elevated p-1 shadow-cx-lg animate-scale-in"
          style={{ top: menuPos.top, left: menuPos.left }}
          role="menu"
          aria-orientation="vertical"
          onClick={(e) => e.stopPropagation()}
        >
          {visibleItems.map((item, i) =>
            item.divider ? (
              <div key={i} className="my-1 h-px bg-border-subtle" />
            ) : (
              <button
                key={item.id ?? i}
                type="button"
                role="menuitem"
                disabled={item.disabled}
                onClick={(e) => {
                  e.stopPropagation();
                  if (item.disabled) return;
                  item.onClick?.();
                  setOpen(false);
                }}
                className={cn(
                  'flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-body transition-colors text-left',
                  'disabled:opacity-50 disabled:pointer-events-none',
                  item.danger
                    ? 'text-danger-500 hover:bg-danger-50 dark:hover:bg-danger-500/10'
                    : 'text-text-secondary hover:bg-surface-2 hover:text-text-primary',
                  item.className,
                )}
              >
                {item.icon && <span className="shrink-0 [&>svg]:h-4 [&>svg]:w-4">{item.icon}</span>}
                {item.label ?? ''}
              </button>
            ),
          )}
        </div>,
        document.body,
      )}
    </div>
  );
}
