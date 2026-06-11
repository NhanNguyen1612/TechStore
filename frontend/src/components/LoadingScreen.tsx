import { LoaderCircle } from "lucide-react";

export function LoadingScreen({ label = "Đang tải" }: { label?: string }) {
  return (
    <div className="flex min-h-[40vh] items-center justify-center">
      <div className="flex items-center gap-3 rounded-full bg-white px-5 py-3 text-sm font-semibold shadow-soft">
        <LoaderCircle className="h-5 w-5 animate-spin text-teal" />
        {label}
      </div>
    </div>
  );
}
