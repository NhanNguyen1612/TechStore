/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#15231f",
        cream: "#f5f0e6",
        lime: "#d7f64b",
        coral: "#ff6b4a",
        teal: "#0f766e",
      },
      boxShadow: {
        soft: "0 18px 50px rgba(21, 35, 31, 0.10)",
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["Manrope", "Inter", "ui-sans-serif", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
