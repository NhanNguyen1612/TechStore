import { LoaderCircle, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { api, apiData } from "../../lib/api";
import { adminStatusLabel } from "../../lib/constants";
import { getErrorMessage } from "../../lib/format";
import type { AdminPage } from "../../types/api";

export function useAdminPage<T>(endpoint: string) {
  const [data, setData] = useState<AdminPage<T> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [version, setVersion] = useState(0);

  const reload = useCallback(() => setVersion((value) => value + 1), []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    apiData<AdminPage<T>>(api.get(endpoint))
      .then((result) => {
        if (!active) return;
        setData(result);
        setError("");
      })
      .catch((requestError) => {
        if (active) setError(getErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [endpoint, version]);

  return { data, loading, error, setError, reload };
}

export function PanelHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow: string;
  title: string;
  description: string;
  actions?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
      <div>
        <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-teal">
          {eyebrow}
        </p>
        <h2 className="mt-2 font-display text-2xl font-extrabold">{title}</h2>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-ink/55">
          {description}
        </p>
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function Toolbar({ children }: { children: React.ReactNode }) {
  return (
    <div className="mt-6 flex flex-wrap items-end gap-3 rounded-3xl border border-black/5 bg-white/65 p-4">
      {children}
    </div>
  );
}

export function TableShell({
  children,
  loading,
  empty,
}: {
  children: React.ReactNode;
  loading: boolean;
  empty: boolean;
}) {
  return (
    <div className="card mt-5 overflow-hidden">
      {loading ? (
        <div className="grid min-h-64 place-items-center">
          <LoaderCircle className="h-8 w-8 animate-spin text-teal" />
        </div>
      ) : empty ? (
        <div className="grid min-h-64 place-items-center px-6 text-center">
          <div>
            <p className="font-display text-xl font-bold">Không có dữ liệu</p>
            <p className="mt-2 text-sm text-ink/45">
              Thử đổi bộ lọc hoặc tạo bản ghi mới.
            </p>
          </div>
        </div>
      ) : (
        <div className="overflow-x-auto">{children}</div>
      )}
    </div>
  );
}

export function AdminTable({
  headers,
  children,
}: {
  headers: string[];
  children: React.ReactNode;
}) {
  return (
    <table className="w-full min-w-[860px] text-left text-sm">
      <thead className="border-b border-ink/5 bg-cream/65 text-xs uppercase tracking-wider text-ink/45">
        <tr>
          {headers.map((header) => (
            <th key={header} className="px-5 py-4 font-bold">
              {header}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="divide-y divide-ink/5">{children}</tbody>
    </table>
  );
}

export function PageControls({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1 && totalElements === 0) return null;
  return (
    <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
      <p className="text-sm text-ink/45">{totalElements} bản ghi</p>
      <div className="flex items-center gap-2">
        <button
          className="btn-secondary !px-4 !py-2"
          disabled={page === 0}
          onClick={() => onChange(page - 1)}
        >
          Trước
        </button>
        <span className="px-3 text-sm font-semibold">
          {page + 1} / {Math.max(totalPages, 1)}
        </span>
        <button
          className="btn-secondary !px-4 !py-2"
          disabled={page >= totalPages - 1}
          onClick={() => onChange(page + 1)}
        >
          Sau
        </button>
      </div>
    </div>
  );
}

export function StatusBadge({
  value,
  tone,
}: {
  value: string;
  tone?: "green" | "red" | "amber" | "blue" | "neutral";
}) {
  const resolved =
    tone ??
    (["ACTIVE", "ENABLED", "PAID", "APPROVED", "COMPLETED", "DELIVERED"].includes(
      value,
    )
      ? "green"
      : ["CANCELLED", "FAILED", "HIDDEN", "DISABLED"].includes(value)
        ? "red"
        : ["PENDING", "PENDING_PAYMENT", "SHIPPING"].includes(value)
          ? "amber"
          : "neutral");
  const tones = {
    green: "bg-emerald-50 text-emerald-700",
    red: "bg-red-50 text-red-700",
    amber: "bg-amber-50 text-amber-700",
    blue: "bg-sky-50 text-sky-700",
    neutral: "bg-slate-100 text-slate-600",
  };
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-[11px] font-extrabold uppercase tracking-wider ${tones[resolved]}`}
    >
      {adminStatusLabel[value] ?? value.replaceAll("_", " ")}
    </span>
  );
}

export function ErrorBanner({
  error,
  onRetry,
}: {
  error: string;
  onRetry?: () => void;
}) {
  if (!error) return null;
  return (
    <div className="mt-5 flex items-center justify-between gap-4 rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">
      <span>{error}</span>
      {onRetry && (
        <button
          className="inline-flex items-center gap-2 font-bold"
          onClick={onRetry}
        >
          <RefreshCw className="h-4 w-4" />
          Thử lại
        </button>
      )}
    </div>
  );
}

export function FormCard({
  title,
  children,
  onClose,
}: {
  title: string;
  children: React.ReactNode;
  onClose: () => void;
}) {
  return (
    <div className="mt-5 rounded-3xl border border-teal/15 bg-white p-5 shadow-soft">
      <div className="mb-5 flex items-center justify-between gap-3">
        <h3 className="font-display text-lg font-extrabold">{title}</h3>
        <button className="text-sm font-bold text-ink/45" onClick={onClose}>
          Đóng
        </button>
      </div>
      {children}
    </div>
  );
}

export function FieldLabel({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block min-w-40 flex-1">
      <span className="label">{label}</span>
      {children}
    </label>
  );
}

export async function runMutation(
  request: Promise<unknown>,
  onSuccess: () => void,
  onError: (message: string) => void,
) {
  try {
    await request;
    onError("");
    onSuccess();
  } catch (error) {
    onError(getErrorMessage(error));
  }
}

export function buildQuery(values: Record<string, unknown>) {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      query.set(key, String(value));
    }
  });
  return query.toString();
}
