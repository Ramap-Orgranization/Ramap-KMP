type ReverseGeocodeRequest = {
  lat?: unknown
  lng?: unknown
}

type NaverReverseGeocodeResponse = {
  results?: Array<{
    region?: {
      area1?: { name?: string }
      area2?: { name?: string }
      area3?: { name?: string }
      area4?: { name?: string }
    }
    land?: {
      name?: string
      number1?: string
      number2?: string
    }
  }>
}

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return json({ message: "Method not allowed" }, 405, { Allow: "POST" })
  }

  const coordinates = await request.json().catch(() => null)
  if (!hasValidCoordinates(coordinates)) {
    return json({ message: "Invalid coordinates" }, 400)
  }

  const naverMapNcpKeyId = Deno.env.get("NAVER_MAP_NCP_KEY_ID")?.trim()
  const naverClientSecret = Deno.env.get("NAVER_CLIENT_SECRET")?.trim()
  if (
    naverMapNcpKeyId === undefined ||
    naverMapNcpKeyId === "" ||
    naverClientSecret === undefined ||
    naverClientSecret === ""
  ) {
    return json({ message: "Reverse geocoding is unavailable" }, 503)
  }

  const url = new URL("https://maps.apigw.ntruss.com/map-reversegeocode/v2/gc")
  url.searchParams.set("coords", `${coordinates.lng},${coordinates.lat}`)
  url.searchParams.set("sourcecrs", "EPSG:4326")
  url.searchParams.set("orders", "roadaddr,addr")
  url.searchParams.set("output", "json")

  const response = await fetch(url, {
    headers: {
      "x-ncp-apigw-api-key-id": naverMapNcpKeyId,
      "x-ncp-apigw-api-key": naverClientSecret,
    },
  })
  if (!response.ok) {
    return json({ message: "Reverse geocoding is unavailable" }, 502)
  }

  const body = (await response.json()) as NaverReverseGeocodeResponse
  return json({ address: parseAddress(body) })
})

function hasValidCoordinates(value: unknown): value is Required<ReverseGeocodeRequest> {
  if (typeof value !== "object" || value === null) return false

  const { lat, lng } = value as ReverseGeocodeRequest
  return (
    typeof lat === "number" &&
    Number.isFinite(lat) &&
    lat >= -90 &&
    lat <= 90 &&
    typeof lng === "number" &&
    Number.isFinite(lng) &&
    lng >= -180 &&
    lng <= 180
  )
}

function parseAddress(response: NaverReverseGeocodeResponse): string | null {
  const result = response.results?.[0]
  if (result === undefined) return null

  const regionNames = [
    result.region?.area1?.name,
    result.region?.area2?.name,
    result.region?.area3?.name,
    result.region?.area4?.name,
  ].filter(isNonBlank)
  const landNumber = [result.land?.number1, result.land?.number2].filter(isNonBlank).join("-")
  const address = [...regionNames, result.land?.name, landNumber].filter(isNonBlank).join(" ")

  return address === "" ? null : address
}

function isNonBlank(value: string | undefined): value is string {
  return value !== undefined && value.trim() !== ""
}

function json(
  body: Record<string, string | null>,
  status = 200,
  headers: HeadersInit = {},
): Response {
  return Response.json(body, {
    status,
    headers,
  })
}
