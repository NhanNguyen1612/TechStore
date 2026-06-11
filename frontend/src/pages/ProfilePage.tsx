import { Camera, KeyRound, Save } from "lucide-react";
import { type FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { roleLabel } from "../lib/constants";
import {
  changePassword,
  updateProfile,
  uploadAvatar,
} from "../store/authSlice";
import { useAppDispatch, useAppSelector } from "../store/hooks";

export function ProfilePage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { user, loading, error } = useAppSelector((state) => state.auth);
  const [profile, setProfile] = useState({ fullName: "", phone: "" });
  const [password, setPassword] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (user) {
      setProfile({ fullName: user.fullName, phone: user.phone ?? "" });
    }
  }, [user]);

  const saveProfile = async (event: FormEvent) => {
    event.preventDefault();
    const action = await dispatch(updateProfile(profile));
    if (updateProfile.fulfilled.match(action)) {
      setMessage("Cập nhật hồ sơ thành công.");
    }
  };

  const savePassword = async (event: FormEvent) => {
    event.preventDefault();
    const action = await dispatch(changePassword(password));
    if (changePassword.fulfilled.match(action)) {
      navigate("/login", { replace: true });
    }
  };

  if (!user) return null;

  return (
    <section className="container-page py-10 sm:py-14">
      <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
        Cài đặt tài khoản
      </p>
      <h1 className="section-title mt-2">Hồ sơ</h1>

      <div className="mt-8 grid gap-8 lg:grid-cols-[320px_1fr]">
        <aside className="card h-fit p-6 text-center">
          <div className="relative mx-auto h-32 w-32">
            {user.avatarUrl ? (
              <img
                src={user.avatarUrl}
                alt=""
                className="h-full w-full rounded-full object-cover"
              />
            ) : (
              <div className="grid h-full w-full place-items-center rounded-full bg-lime font-display text-4xl font-black">
                {user.fullName.charAt(0)}
              </div>
            )}
            <label className="absolute bottom-0 right-0 cursor-pointer rounded-full bg-ink p-3 text-white shadow-lg">
              <Camera className="h-5 w-5" />
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) dispatch(uploadAvatar(file));
                }}
              />
            </label>
          </div>
          <h2 className="mt-5 font-display text-xl font-bold">
            {user.fullName}
          </h2>
          <p className="mt-1 text-sm text-ink/45">{user.email}</p>
          <span className="mt-4 inline-flex rounded-full bg-cream px-3 py-1.5 text-xs font-bold">
            {roleLabel[user.role]}
          </span>
        </aside>

        <div className="grid gap-6">
          <form onSubmit={saveProfile} className="card p-6 sm:p-8">
            <h2 className="font-display text-xl font-bold">
              Thông tin cá nhân
            </h2>
            <div className="mt-6 grid gap-5 sm:grid-cols-2">
              <div>
                <label className="label">Họ và tên</label>
                <input
                  className="field"
                  value={profile.fullName}
                  minLength={2}
                  maxLength={100}
                  required
                  onChange={(event) =>
                    setProfile({ ...profile, fullName: event.target.value })
                  }
                />
              </div>
              <div>
                <label className="label">Số điện thoại</label>
                <input
                  className="field"
                  value={profile.phone}
                  pattern="^\+?[0-9]{8,15}$"
                  onChange={(event) =>
                    setProfile({ ...profile, phone: event.target.value })
                  }
                />
              </div>
            </div>
            {message && (
              <p className="mt-4 text-sm font-semibold text-teal">{message}</p>
            )}
            <button disabled={loading} className="btn-primary mt-6">
              <Save className="h-5 w-5" />
              Lưu hồ sơ
            </button>
          </form>

          <form onSubmit={savePassword} className="card p-6 sm:p-8">
            <h2 className="font-display text-xl font-bold">Đổi mật khẩu</h2>
            <div className="mt-6 grid gap-5">
              <input
                className="field"
                type="password"
                placeholder="Mật khẩu hiện tại"
                required
                value={password.currentPassword}
                onChange={(event) =>
                  setPassword({
                    ...password,
                    currentPassword: event.target.value,
                  })
                }
              />
              <div className="grid gap-5 sm:grid-cols-2">
                <input
                  className="field"
                  type="password"
                  placeholder="Mật khẩu mới"
                  minLength={8}
                  required
                  value={password.newPassword}
                  onChange={(event) =>
                    setPassword({
                      ...password,
                      newPassword: event.target.value,
                    })
                  }
                />
                <input
                  className="field"
                  type="password"
                  placeholder="Xác nhận mật khẩu mới"
                  minLength={8}
                  required
                  value={password.confirmPassword}
                  onChange={(event) =>
                    setPassword({
                      ...password,
                      confirmPassword: event.target.value,
                    })
                  }
                />
              </div>
            </div>
            {error && <p className="mt-4 text-sm text-red-700">{error}</p>}
            <button disabled={loading} className="btn-secondary mt-6">
              <KeyRound className="h-5 w-5" />
              Cập nhật mật khẩu
            </button>
          </form>
        </div>
      </div>
    </section>
  );
}
