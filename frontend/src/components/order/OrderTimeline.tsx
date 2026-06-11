import { Check, Clock3, X } from "lucide-react";
import clsx from "clsx";
import { formatDate } from "../../lib/format";
import type { OrderTimelineEntry } from "../../types/api";

export function OrderTimeline({
  entries,
}: {
  entries: OrderTimelineEntry[];
}) {
  return (
    <ol className="grid gap-0">
      {entries.map((entry, index) => {
        const cancelled = entry.status === "CANCELLED";
        return (
          <li
            key={`${entry.status}-${entry.time}-${index}`}
            className="grid grid-cols-[36px_1fr] gap-3"
          >
            <div className="flex flex-col items-center">
              <span
                className={clsx(
                  "grid h-9 w-9 place-items-center rounded-full",
                  cancelled ? "bg-red-100 text-red-700" : "bg-lime text-ink",
                )}
              >
                {cancelled ? (
                  <X className="h-4 w-4" />
                ) : index === entries.length - 1 ? (
                  <Clock3 className="h-4 w-4" />
                ) : (
                  <Check className="h-4 w-4" />
                )}
              </span>
              {index < entries.length - 1 && (
                <span className="h-full min-h-10 w-px bg-ink/10" />
              )}
            </div>
            <div className="pb-6">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="font-bold">{entry.title}</p>
                <time className="text-xs text-ink/40">
                  {formatDate(entry.time)}
                </time>
              </div>
              <p className="mt-1 text-sm leading-6 text-ink/55">
                {entry.description}
              </p>
              {entry.note && entry.note !== entry.description && (
                <p className="mt-2 rounded-xl bg-cream px-3 py-2 text-xs text-ink/65">
                  {entry.note}
                </p>
              )}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
