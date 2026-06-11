import { Star } from "lucide-react";
import clsx from "clsx";

export function RatingStars({
  value,
  onChange,
  size = "md",
}: {
  value: number;
  onChange?: (value: number) => void;
  size?: "sm" | "md";
}) {
  return (
    <div className="flex items-center gap-1" aria-label={`${value} trên 5 sao`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          disabled={!onChange}
          onClick={() => onChange?.(star)}
          className={clsx(
            "rounded transition",
            onChange && "hover:scale-110",
          )}
          aria-label={`Đánh giá ${star} sao`}
        >
          <Star
            className={clsx(
              size === "sm" ? "h-4 w-4" : "h-5 w-5",
              star <= value
                ? "fill-amber-400 text-amber-400"
                : "text-ink/20",
            )}
          />
        </button>
      ))}
    </div>
  );
}
