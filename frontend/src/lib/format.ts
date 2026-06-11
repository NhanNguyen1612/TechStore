export const formatCurrency = (value: number | string) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value));

export const formatDate = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(value))
    : "-";

export const getErrorMessage = (error: unknown) => {
  if (axiosError(error)) {
    const fieldErrors = Object.values(error.response?.data?.data ?? {}).filter(
      Boolean,
    );
    if (fieldErrors.length > 0) {
      return fieldErrors.join(". ");
    }
    const message = error.response?.data?.message || error.message;
    return message === "An unexpected error occurred"
      ? "Đã xảy ra lỗi không mong muốn"
      : message;
  }
  return error instanceof Error ? error.message : "Đã xảy ra lỗi";
};

const axiosError = (
  error: unknown,
): error is {
  message: string;
  response?: { data?: { message?: string; data?: Record<string, string> } };
} => typeof error === "object" && error !== null && "message" in error;
