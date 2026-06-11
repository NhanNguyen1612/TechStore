import { ChevronLeft, ChevronRight } from "lucide-react";

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="mt-10 flex items-center justify-center gap-3">
      <button
        className="btn-secondary !p-3"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
        aria-label="Trang trước"
      >
        <ChevronLeft className="h-5 w-5" />
      </button>
      <span className="rounded-full bg-white px-5 py-3 text-sm font-semibold shadow-sm">
        Trang {page + 1} trên {totalPages}
      </span>
      <button
        className="btn-secondary !p-3"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
        aria-label="Trang sau"
      >
        <ChevronRight className="h-5 w-5" />
      </button>
    </div>
  );
}
