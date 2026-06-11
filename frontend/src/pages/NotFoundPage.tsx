import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="container-page py-24 text-center">
      <p className="font-display text-8xl font-black text-ink/10">404</p>
      <h1 className="mt-4 font-display text-3xl font-extrabold">
        Không tìm thấy trang
      </h1>
      <Link to="/" className="btn-primary mt-7">
        Về trang chủ
      </Link>
    </section>
  );
}
