import type { SVGProps } from "react";
import type { WeatherCondition } from "@/lib/types";

export function TriangleUpIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width="10" height="10" viewBox="0 0 10 10" fill="none" {...props}>
      <path d="M5 1L9 8H1L5 1Z" fill="currentColor" />
    </svg>
  );
}

export function TriangleDownIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width="10" height="10" viewBox="0 0 10 10" fill="none" {...props}>
      <path d="M5 9L1 2H9L5 9Z" fill="currentColor" />
    </svg>
  );
}

export function ArrowRightIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" {...props}>
      <path
        d="M3 8H13M13 8L9 4M13 8L9 12"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function LeafIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" {...props}>
      <path
        d="M12 21C12 21 5 17 5 10C5 5 9 3 12 3C15 3 19 5 19 10C19 17 12 21 12 21Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path d="M12 21V8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

export function SunIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" {...props}>
      <circle cx="12" cy="12" r="5" stroke="currentColor" strokeWidth="1.6" />
      <path
        d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M19.1 4.9L17 7M7 17l-2.1 2.1"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
      />
    </svg>
  );
}

export function CloudIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" {...props}>
      <path
        d="M7 18h10a4 4 0 0 0 .4-7.98A5.5 5.5 0 0 0 7.1 9.2 4 4 0 0 0 7 18Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function CloudRainIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" {...props}>
      <path
        d="M7 15h10a4 4 0 0 0 .4-7.98A5.5 5.5 0 0 0 7.1 6.2 4 4 0 0 0 7 15Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      <path
        d="M8 18l-1 3M12 18l-1 3M16 18l-1 3"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
      />
    </svg>
  );
}

export function CloudSunIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" {...props}>
      <circle cx="8" cy="8" r="3.2" stroke="currentColor" strokeWidth="1.6" />
      <path
        d="M8 2.5v2M3 8H1M4.3 4.3l1.4 1.4"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinecap="round"
      />
      <path
        d="M9 18h9a3.5 3.5 0 0 0 .3-6.98A4.8 4.8 0 0 0 9.7 9.6 3.5 3.5 0 0 0 9 18Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function WindIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" {...props}>
      <path
        d="M3 8h11a2.5 2.5 0 1 0-2.5-2.5M3 12h15a2.5 2.5 0 1 1-2.5 2.5M3 16h9a2 2 0 1 1-2 2"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

const CONDITION_ICON: Record<WeatherCondition, (props: SVGProps<SVGSVGElement>) => React.JSX.Element> = {
  sun: SunIcon,
  cloud: CloudIcon,
  "cloud-sun": CloudSunIcon,
  "cloud-rain": CloudRainIcon,
  wind: WindIcon,
};

export function WeatherConditionIcon({
  condition,
  ...props
}: { condition: WeatherCondition } & SVGProps<SVGSVGElement>) {
  const Icon = CONDITION_ICON[condition];
  return <Icon {...props} />;
}
