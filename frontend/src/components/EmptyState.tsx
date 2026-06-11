import type { LucideIcon } from "lucide-react";

export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
}: {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="card flex flex-col items-center px-6 py-16 text-center">
      <div className="mb-5 rounded-full bg-cream p-5">
        <Icon className="h-8 w-8 text-teal" />
      </div>
      <h2 className="font-display text-xl font-bold">{title}</h2>
      <p className="mt-2 max-w-md text-sm leading-6 text-ink/60">
        {description}
      </p>
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}
