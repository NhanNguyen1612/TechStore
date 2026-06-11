import { ArrowRight, LockKeyhole, Mail, UserRound } from "lucide-react";
import { type FormEvent, useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { clearAuthError, login, register } from "../store/authSlice";
import { useAppDispatch, useAppSelector } from "../store/hooks";

export function AuthPage({ mode }: { mode: "login" | "register" }) {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, loading, error } = useAppSelector((state) => state.auth);
  const [form, setForm] = useState({
    email: "",
    password: "",
    fullName: "",
    phone: "",
  });

  useEffect(() => {
    dispatch(clearAuthError());
  }, [dispatch, mode]);

  useEffect(() => {
    if (user) {
      const from =
        (location.state as { from?: { pathname?: string } } | null)?.from
          ?.pathname ?? (user.role === "ROLE_ADMIN" ? "/dashboard" : "/products");
      navigate(from, { replace: true });
    }
  }, [location.state, navigate, user]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (mode === "login") {
      await dispatch(login({ email: form.email, password: form.password }));
    } else {
      await dispatch(register(form));
    }
  };

  return (
    <section className="container-page grid min-h-[75vh] gap-10 py-12 lg:grid-cols-2 lg:items-center">
      <div className="hidden rounded-[2.5rem] bg-ink p-12 text-white lg:block">
        <span className="inline-flex rounded-full bg-lime px-4 py-2 text-sm font-bold text-ink">
          Tài khoản TechStore của bạn
        </span>
        <h1 className="mt-8 font-display text-5xl font-extrabold leading-tight">
          Một tài khoản cho mua sắm, đánh giá, thanh toán và hỗ trợ.
        </h1>
        <p className="mt-6 text-lg leading-8 text-white/60">
          Phiên đăng nhập luôn được bảo vệ và tự động làm mới mã truy cập.
        </p>
      </div>
      <form onSubmit={submit} className="card mx-auto w-full max-w-xl p-6 sm:p-10">
        <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
          {mode === "login" ? "Chào mừng trở lại" : "Tham gia TechStore"}
        </p>
        <h2 className="mt-3 font-display text-3xl font-extrabold">
          {mode === "login" ? "Đăng nhập để tiếp tục" : "Tạo tài khoản của bạn"}
        </h2>

        <div className="mt-8 grid gap-5">
          {mode === "register" && (
            <Field
              icon={UserRound}
              label="Họ và tên"
              value={form.fullName}
              onChange={(value) => setForm({ ...form, fullName: value })}
              required
            />
          )}
          <Field
            icon={Mail}
            label="Email"
            type="email"
            value={form.email}
            onChange={(value) => setForm({ ...form, email: value })}
            required
          />
          <Field
            icon={LockKeyhole}
            label="Mật khẩu"
            type="password"
            value={form.password}
            onChange={(value) => setForm({ ...form, password: value })}
            minLength={8}
            required
          />
          {mode === "register" && (
            <div>
              <label className="label">Số điện thoại</label>
              <input
                className="field"
                value={form.phone}
                onChange={(event) =>
                  setForm({ ...form, phone: event.target.value })
                }
                placeholder="+84912345678"
                pattern="^\+?[0-9]{8,15}$"
              />
            </div>
          )}
        </div>

        {error && (
          <p className="mt-5 rounded-2xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            {error}
          </p>
        )}

        <button disabled={loading} className="btn-primary mt-7 w-full">
          {loading
            ? "Vui lòng chờ..."
            : mode === "login"
              ? "Đăng nhập"
              : "Đăng ký"}
          <ArrowRight className="h-5 w-5" />
        </button>
        <p className="mt-6 text-center text-sm text-ink/55">
          {mode === "login" ? "Chưa có tài khoản TechStore?" : "Đã có tài khoản?"}{" "}
          <Link
            className="font-bold text-teal"
            to={mode === "login" ? "/register" : "/login"}
          >
            {mode === "login" ? "Tạo tài khoản" : "Đăng nhập"}
          </Link>
        </p>
      </form>
    </section>
  );
}

function Field({
  icon: Icon,
  label,
  value,
  onChange,
  type = "text",
  ...props
}: {
  icon: typeof Mail;
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
} & Omit<
  React.InputHTMLAttributes<HTMLInputElement>,
  "onChange" | "value" | "type"
>) {
  return (
    <div>
      <label className="label">{label}</label>
      <div className="relative">
        <Icon className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-ink/35" />
        <input
          {...props}
          type={type}
          className="field pl-12"
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
      </div>
    </div>
  );
}
