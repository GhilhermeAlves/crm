import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  const body = await request.json();

  const response = await fetch(
    `${process.env.NEXT_PUBLIC_AUTH_SERVICE_URL || "http://localhost:8082"}/internal/auth/login`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }
  );

  if (!response.ok) {
    return NextResponse.json(
      await response.json(),
      { status: response.status }
    );
  }

  return NextResponse.json(await response.json());
}
