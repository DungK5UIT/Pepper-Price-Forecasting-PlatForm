import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin", "vietnamese"],
});

export const metadata: Metadata = {
  title: "Giá Tiêu Việt — Dự báo giá tiêu Việt Nam",
  description:
    "Nền tảng dự báo giá tiêu Việt Nam theo ngày, tuần, tháng từ chuỗi giá lịch sử, kèm khoảng dao động có thể xảy ra.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="vi" className={`${inter.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col bg-canvas text-ink">{children}</body>
    </html>
  );
}
