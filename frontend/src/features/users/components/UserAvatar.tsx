"use client";

import { cn } from "@/lib/utils";

interface UserAvatarProps {
  firstName?: string;
  lastName?: string;
  avatarUrl?: string | null;
  size?: "sm" | "md" | "lg";
  className?: string;
}

const sizeClasses = {
  sm: "h-8 w-8 text-xs",
  md: "h-10 w-10 text-sm",
  lg: "h-16 w-16 text-lg",
};

function getInitials(firstName?: string, lastName?: string): string {
  const first = firstName?.charAt(0) || "";
  const last = lastName?.charAt(0) || "";
  return (first + last).toUpperCase();
}

function getAvatarColor(name: string): string {
  const colors = [
    "bg-blue-500",
    "bg-green-500",
    "bg-purple-500",
    "bg-pink-500",
    "bg-indigo-500",
    "bg-orange-500",
    "bg-teal-500",
    "bg-red-500",
  ];
  const index = name.charCodeAt(0) % colors.length;
  return colors[index];
}

export function UserAvatar({
  firstName,
  lastName,
  avatarUrl,
  size = "md",
  className,
}: UserAvatarProps) {
  const initials = getInitials(firstName, lastName);
  const name = `${firstName || ""} ${lastName || ""}`.trim();
  const colorClass = getAvatarColor(name || "U");

  if (avatarUrl) {
    return (
      <img
        src={avatarUrl}
        alt={name}
        className={cn("rounded-full object-cover", sizeClasses[size], className)}
      />
    );
  }

  return (
    <div
      className={cn(
        "flex items-center justify-center rounded-full font-medium text-white",
        sizeClasses[size],
        colorClass,
        className,
      )}
    >
      {initials}
    </div>
  );
}
